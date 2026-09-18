package de.kraftwerkone.overlord.monitor.dashboard;

import de.kraftwerkone.overlord.monitor.common.FehlerLiveErgebnis;
import de.kraftwerkone.overlord.monitor.common.FehlerLiveErsatz;
import de.kraftwerkone.overlord.monitor.common.FehlerLiveResponse;
import de.kraftwerkone.overlord.monitor.common.FehlerLiveService;
import de.kraftwerkone.overlord.monitor.common.LiveRestErgebnis;
import de.kraftwerkone.overlord.monitor.common.LiveRestResponse;
import de.kraftwerkone.overlord.monitor.common.LiveRestService;
import de.kraftwerkone.overlord.monitor.common.MandantContext;
import de.kraftwerkone.overlord.monitor.common.MessageStatusClassifier;
import de.kraftwerkone.overlord.monitor.common.MessageStatusKind;
import de.kraftwerkone.overlord.monitor.common.Rollupzeitraum;
import de.kraftwerkone.overlord.monitor.common.Zeitfenster;
import de.kraftwerkone.overlord.monitor.common.Zeitpunkte;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.springframework.stereotype.Service;

/**
 * Setzt die Landingpage zusammen — <b>aus einem Lesevorgang je Block und nicht aus einem je
 * Kennzahl</b>.
 *
 * <h2>Die Einordnung entsteht hier und wird gerufen, nicht nachgebaut</h2>
 *
 * <p>Im Rollup steht der <b>Rohwert</b> (Entscheidung E-g). Die Kategorie bildet {@link
 * MessageStatusClassifier#einordnung(String)} — dieselbe Stelle, die Liste, Detailansicht und
 * spaeter der Chatbot benutzen. <b>Ein zweites {@code switch} ueber Statuswoerter darf in diesem
 * Paket nicht entstehen:</b> Es liefe beim naechsten neuen Statuswert von der Liste weg, und der
 * Unterschied fiele erst auf, wenn jemand zwei Zahlen nebeneinanderlegt.
 *
 * <p>Dasselbe gilt fuer die <b>Fehlerart</b>: {@link MessageStatusClassifier#fehlerart(String)},
 * gerufen und nicht nachgebaut.
 *
 * <h2>Ein Uhrenschlag je Anfrage</h2>
 *
 * <p>{@code jetzt} kommt genau einmal aus der <b>Anwendungsuhr</b> (Regel Z1) und wird danach
 * herumgereicht. Zwei Schlaege innerhalb derselben Anfrage waeren zwei Stichtage — und bei einem
 * Aufruf, der eine Eimergrenze streift, zwei verschiedene Fenster. <b>Auch der Live-Rest bekommt
 * diesen Schlag</b> — sein Live-Bereich endet an derselben Stundengrenze wie das Fenster.
 *
 * <h2>Der Live-Rest der laufenden Stunde (Teil B, E-190, E-191)</h2>
 *
 * <p>Seit dem 17.09.2026 ruft die Seite denselben Baustein wie der Prozessbaum ({@code
 * common/LiveRestService}, {@code docs/live-rest.md}) und ordnet die Korrektur ihren eigenen Eimern
 * zu ({@link Liveverrechnung}): Verlauf und beide Rollup-Kacheln rechnen mit den korrigierten
 * Zeilen, Block 5 mit der Korrektur je Schluessel — dafuer liest das Repository den Katalog fuer
 * die betroffenen Prozesse nach, <b>nur wenn es Korrekturzeilen gibt</b>. Die <b>Belegungsprobe</b>
 * des Standardfensters bleibt ohne Korrektur: Sie entscheidet ueber das Paar, bevor der Live-Rest
 * gelesen ist, und ein Paar, das ohne den angebrochenen Eimer nicht traegt, traegt mit ihm nicht
 * besser. <i>Laeuft</i>, <i>Wartend</i> und <i>Zuletzt aufgefallen</i> lesen ohnehin live. Der
 * Block <i>Stand</i> bleibt, was er war.
 *
 * <h2>Die Fehler kommen live (E-208, {@code docs/fehler-live.md})</h2>
 *
 * <p>Seit dem 18.09.2026 kommt die Einordnung {@code FEHLER} nicht mehr aus dem Rollup, sondern aus
 * einer Live-Lesung ueber {@code Message} ({@code common/FehlerLiveService}) — ein Fehlerstatus ist
 * durch Nachverarbeitung nicht endgueltig, und der Delta-Lauf entfernt einen Abgang aus einem alten
 * Eimer nicht. <b>Nach der Verrechnung des Live-Rests</b> fallen die Fehlerzeilen aus den Zeilen
 * der Bloecke 1 bis 3, und die Zeilen der Lesung kommen, auf den Eimer des Paares gehoben, hinzu
 * ({@code common/FehlerLiveErsatz}); Kachel <i>Fehler</i> und Fehlerarten entstehen danach wie
 * bisher. <b>Die Lesung laeuft vor Block 5</b>, denn dessen zwei Statements haengen an ihrem
 * Zustand: angewandt ohne die Fehler des Rollups, und die Fehlerzeilen gehen wie die
 * Korrekturzeilen ueber die Katalog-Nachlesung; ausgesetzt im heutigen Wortlaut. Alle anderen
 * Einordnungen, <i>Laeuft</i>, <i>Wartend</i>, Block 6, <i>Stand</i>, <i>Plattform</i> und die
 * Belegungsprobe bleiben, was sie waren.
 */
@Service
public class DashboardService {

  private final DashboardRepository dashboardRepository;
  private final MessageStatusClassifier statusClassifier;
  private final Clock anwendungsuhr;
  private final DienstLeseRepository dienstLeseRepository;
  private final DienstStatusClassifier dienstClassifier;

  /**
   * Die Ablagenpruefung — <b>und sie ist leer, wenn sie abgeschaltet ist</b>.
   *
   * <p>{@code overlord.ablagenpruefung.aktiv} entscheidet, ob es die Bean ueberhaupt gibt (dieselbe
   * Bauform wie {@code rollup/RollupPlaner}). Ein {@link Optional} im Konstruktor ist damit kein
   * Vorbehalt gegen eine vielleicht fehlende Abhaengigkeit, sondern der <b>Zustand selbst</b>: leer
   * heisst abgeschaltet, und die Kachel sagt das mit benanntem Grund.
   */
  private final Optional<Ablagenpruefung> ablagenpruefung;

  /** Der Baustein aus {@code common} — derselbe, den der Prozessbaum ruft (E-179 bis E-185). */
  private final LiveRestService liveRestService;

  /** Die Fehler live, aus {@code common} (E-208) — mit Teil B ruft ihn auch der Prozessbaum. */
  private final FehlerLiveService fehlerLiveService;

  DashboardService(
      DashboardRepository dashboardRepository,
      MessageStatusClassifier statusClassifier,
      Clock anwendungsuhr,
      DienstLeseRepository dienstLeseRepository,
      DienstStatusClassifier dienstClassifier,
      Optional<Ablagenpruefung> ablagenpruefung,
      LiveRestService liveRestService,
      FehlerLiveService fehlerLiveService) {
    this.dashboardRepository = dashboardRepository;
    this.statusClassifier = statusClassifier;
    this.anwendungsuhr = anwendungsuhr;
    this.dienstLeseRepository = dienstLeseRepository;
    this.dienstClassifier = dienstClassifier;
    this.ablagenpruefung = ablagenpruefung;
    this.liveRestService = liveRestService;
    this.fehlerLiveService = fehlerLiveService;
  }

  /**
   * Wie viele benannte Werte der Verteilungsblock einzeln zeigt.
   *
   * <p><b>Zehn, und die Zahl ist gewaehlt und nicht gemessen.</b> Sie ist die Grenze, an der die
   * Restzeile entsteht; M98 hat sie fuer vier Mandanten durchgerechnet (Top 10 traegt bei {@code
   * NEXANS} 76,4 %, bei {@code VOTG} 76,5 %, bei {@code IBIS} 72,1 %). Sie steht hier als Konstante
   * und nicht in der Konfiguration: Ein Schalter dafuer waere eine Gestaltungsentscheidung, die
   * niemand getroffen hat.
   */
  static final int VERTEILUNG_TOP = 10;

  /**
   * Wie viele Zeilen „Zuletzt aufgefallen" zeigt.
   *
   * <p><b>Zehn, gewaehlt und nicht gemessen</b> — es ist eine kurze Liste auf einer Landingpage und
   * keine Seite. Wer mehr will, klickt in die Nachrichtenliste; dort gibt es Cursor, Filter und
   * Sortierung. Auch diese Zahl steht bewusst nicht in der Konfiguration.
   */
  static final int AUFFAELLIG_HOECHSTENS = 10;

  /**
   * Die zweite Bedingung des Standardfensters: Mindestens ein Eimer muss <b>mehr als</b> so viele
   * Nachrichten tragen.
   *
   * <h2>Sie ist in der Praxis wirkungslos, und das ist bekannt</h2>
   *
   * <p>Sie sollte {@code WOC} fangen: 29 von 30 Tagen belegt bei 117 Nachrichten (M95), also knapp
   * vier am Tag — ein Diagramm mit Punkten, das nichts zeigt. <b>Weil EDI-Verkehr stossweise ist,
   * liegt aber mit hoher Wahrscheinlichkeit ein Tag ueber fuenf, und {@code WOC} besteht die
   * Bedingung.</b>
   *
   * <p><b>Das ist gerechnet und nicht gemessen</b> — M108 misst {@code NEXANS} und {@code SUTTONS},
   * nicht {@code WOC}. Damit <i>kein</i> Tag ueber fuenf laege, muessten sich 117 Nachrichten fast
   * gleichmaessig auf 29 Tage verteilen, und genau das tun sie nicht.
   *
   * <p><b>Der Auftraggeber hat das am 31.08.2026 in Kenntnis dieser Folge so entschieden.</b> Sie
   * steht deshalb als <i>bekannte Grenze</i> in {@code docs/dashboard.md} und wird nicht
   * nachgebessert: Eine Schwelle, die {@code WOC} sicher faengt, faenge auch Mandanten mit echtem,
   * aber duennem Verkehr — und die haetten dann kein Diagramm, obwohl es etwas zu sehen gaebe.
   */
  static final long EIMER_MINDESTENS = 5;

  /**
   * Die ganze Landingpage fuer den aktiven Mandanten.
   *
   * <h2>Die Verteilung kommt in beiden Sichten — zwei Statements derselben Gestalt</h2>
   *
   * <p>Seit dem 16.09.2026 laeuft das Verteilungsstatement <b>je Sicht einmal</b>, und zwar
   * unveraendert: {@code CASE} als ein Ausdruck ohne Alias, {@code LEFT JOIN} auf den Katalog,
   * Mandantenkette als {@code EXISTS}. <b>Zusammengelegt ueber beide Katalogspalten wird bewusst
   * nicht</b> — ein Statement mit zwei {@code CASE}-Ausdruecken im {@code GROUP BY} lieferte eine
   * Zeile je vorkommender Kombination aus Partner und Richtung, die Summen je Sicht entstuenden
   * erst in Java, und es waere eine neue Abfrage mit eigener Messung statt der gemessenen. Der
   * Preis ist ein zweiter Bereichszugriff auf dieselbe Rollup-Ebene; die Vorregistrierung und das
   * Ergebnis stehen in {@code docs/dashboard.md} §8.
   *
   * @param mandant Regel M2 — erster Pflichtparameter, und er kommt aus der Sitzung (Regel M1)
   * @param gewaehlt das Paar aus der URL, oder {@code null}
   */
  public DashboardResponse landingpage(MandantContext mandant, Rollupzeitraum gewaehlt) {
    LocalDateTime jetzt = LocalDateTime.now(anwendungsuhr);
    Rollupzeitraum zeitraum = gewaehlt == null ? standardfenster(mandant, jetzt) : gewaehlt;
    Zeitfenster fenster = zeitraum.fenster(jetzt);

    List<Rollupsumme> ausDemRollup = dashboardRepository.verlauf(mandant, zeitraum, fenster);
    // Fehler live liest VOR Block 5: Dessen zwei Statements haengen am Zustand der Lesung (E-208)
    // — dasselbe Fenster aus demselben Uhrenschlag wie Rollup und Live-Rest.
    FehlerLiveErgebnis fehlerLive =
        fehlerLiveService.ermittle(mandant, fenster.von(), fenster.bis());
    List<Verteilungssumme> nachPartner =
        verteilungssummen(mandant, zeitraum, fenster, Verteilungssicht.PARTNER, fehlerLive);
    List<Verteilungssumme> nachRichtung =
        verteilungssummen(mandant, zeitraum, fenster, Verteilungssicht.RICHTUNG, fehlerLive);
    OffeneKachelResponse laeuft = offeneKachel(mandant, MessageStatusKind.LAEUFT, jetzt);
    // Die Erscheinungsbedingung wird bei JEDEM Aufruf mitgelesen und nicht bedingt: Sonst haenge
    // die Zahl der Statements am Mandanten, und DashboardStatementsTest waere nicht mehr
    // deterministisch. Entschieden wird erst hier, ob die Kachel in die Antwort kommt.
    boolean zeigeWartend = dashboardRepository.hatWartendeAblaeufe(mandant);
    OffeneKachelResponse wartend = offeneKachel(mandant, MessageStatusKind.WARTEND, jetzt);
    List<AuffaelligerProzess> aufgefallen =
        dashboardRepository.zuletztAufgefallen(mandant, fenster, AUFFAELLIG_HOECHSTENS);
    StandResponse stand = stand();
    PlattformResponse plattform = plattform(jetzt);

    // Der Live-Rest kommt nach den Statements der Seite und vor dem Zusammensetzen: erst der
    // Wasserstand, dann bei ANGEWANDT die zwei Live-Lesungen — und die Katalog-Nachlesung nur,
    // wenn Block 5 etwas zuzurechnen hat. DashboardStatementsTest benennt alle einzeln.
    LiveRestErgebnis liveRest = liveRestService.ermittle(mandant, jetzt);
    Liveverrechnung verrechnung = Liveverrechnung.im(zeitraum, fenster, liveRest.korrektur());
    // Der Ersatz NACH der Verrechnung des Live-Rests (E-190, E-208): Die Fehlerzeilen fallen
    // heraus, die der Lesung kommen auf dem Eimer des Paares hinzu. Ausgesetzt bleibt alles.
    List<Rollupsumme> summen =
        FehlerLiveErsatz.ersetze(
            fehlerLive.zustand(),
            verrechnung.verlauf(ausDemRollup),
            Liveverrechnung.gehoben(zeitraum, fenster, fehlerLive.zeilen()),
            Rollupsumme::messageStatus,
            statusClassifier);
    Liveverrechnung zuBlock5 =
        Liveverrechnung.fuerDieVerteilung(
            zeitraum, fenster, liveRest.korrektur(), fehlerLive, statusClassifier);
    Map<String, String> partnerJeProzess = new LinkedHashMap<>();
    Map<String, String> richtungJeProzess = new LinkedHashMap<>();
    if (!zuBlock5.leer()) {
      for (Katalogzuordnungszeile zeile :
          dashboardRepository.katalogzuordnung(mandant, zuBlock5.betroffeneProzesse())) {
        partnerJeProzess.put(zeile.processId(), zeile.partner());
        richtungJeProzess.put(zeile.processId(), zeile.richtung());
      }
    }

    KachelnResponse kacheln = kacheln(summen, laeuft, zeigeWartend ? wartend : null);
    return new DashboardResponse(
        zeitraum.code(),
        fensterAntwort(fenster),
        kacheln.nachrichten() == 0,
        verlauf(summen),
        kacheln,
        new VerteilungResponse(
            verteilung(zuBlock5.verteilung(nachPartner, partnerJeProzess)),
            verteilung(zuBlock5.verteilung(nachRichtung, richtungJeProzess))),
        zuletztAufgefallen(aufgefallen),
        stand,
        LiveRestResponse.aus(liveRest.entscheidung(), anwendungsuhr.getZone()),
        FehlerLiveResponse.aus(fehlerLive),
        plattform);
  }

  /**
   * Block 5, eine Sicht, <b>als Statement</b>: bei angewandtem Fehler live ohne die Fehler des
   * Rollups — die kommen aus der Lesung ueber die Nachlesung —, sonst im heutigen Wortlaut. So
   * zaehlen Kachel und Sichten in beiden Zustaenden dieselbe Zahl.
   */
  private List<Verteilungssumme> verteilungssummen(
      MandantContext mandant,
      Rollupzeitraum zeitraum,
      Zeitfenster fenster,
      Verteilungssicht sicht,
      FehlerLiveErgebnis fehlerLive) {
    return fehlerLive.angewandt()
        ? dashboardRepository.verteilungOhneFehler(mandant, zeitraum, fenster, sicht)
        : dashboardRepository.verteilung(mandant, zeitraum, fenster, sicht);
  }

  /**
   * <b>Block 8 — der plattformweite Teil</b> (Schritt 10d, E‑116).
   *
   * <p>Er haengt an keinem Mandanten und kostet trotzdem nur <b>ein</b> Statement: die Lampen. Die
   * Ablagenkachel kommt aus dem Speicher — <b>die Pruefung liegt ausserhalb der Anfrage</b>
   * (E‑120), denn ein Abruf gegen eine abgeschaltete Ablage dauert allein rund 2,7 Sekunden (M174)
   * und das Budget der Seite liegt bei 500 ms.
   *
   * <p><b>Er steht im selben Aufruf und wird nicht nachgeladen</b> — dieselbe Begruendung wie fuer
   * die uebrigen sieben Bloecke.
   */
  private PlattformResponse plattform(LocalDateTime jetzt) {
    List<DienstResponse> dienste =
        dienstLeseRepository.dienste().stream()
            .map(
                zeile ->
                    new DienstResponse(
                        zeile.serviceId(),
                        dienstClassifier.einordnung(zeile.rohwert()),
                        zeile.rohwert(),
                        zeile.stand() == null
                            ? null
                            : Zeitpunkte.nachUtc(zeile.stand(), anwendungsuhr.getZone()),
                        Alter.sekunden(zeile.stand(), jetzt)))
            .toList();

    AblagenResponse ablagen =
        ablagenpruefung
            .map(
                pruefung ->
                    Ablagenkachel.aus(
                        pruefung.letzterStand().orElse(null),
                        pruefung.takt(),
                        jetzt,
                        anwendungsuhr.getZone()))
            .orElseGet(Ablagenkachel::abgeschaltet);

    return new PlattformResponse(dienste, ablagen);
  }

  /**
   * <b>D.3: Das Standardfenster richtet sich nach dem Mandanten.</b>
   *
   * <p>Genommen wird das <b>erste</b> Paar der Reihe 48 h → 30 Tage → 12 Monate, das <b>beide</b>
   * Bedingungen erfuellt: mindestens die Haelfte der Eimer belegt <b>und</b> mindestens ein Eimer
   * mit mehr als {@value #EIMER_MINDESTENS} Nachrichten.
   *
   * <p><b>Die Schwelle von 50 % ist aus M95 abgeleitet</b>, nicht gewaehlt: {@code NEXANS}, {@code
   * SUTTONS} und {@code VOTG} liegen bei allen drei Paaren auf 100 %; {@code IBIS} und {@code
   * IBISGUS} fallen bei 48 Stunden auf 37,50 % und 27,08 %. Der Sprung liegt also nicht zwischen
   * gross und klein, sondern beim <b>verstreutesten</b> Verkehr — und genau den soll die Ansicht
   * nicht als Diagramm mit Luecken zeigen.
   *
   * <p><b>Gesucht wird der Reihe nach und nicht in einem Statement.</b> Der Normalfall — ein
   * Mandant mit Verkehr — ist nach der ersten, kleinsten Abfrage entschieden; nur wer bei 48
   * Stunden durchfaellt, kostet eine zweite. Drei Belegungsproben auf einmal kosteten <b>immer</b>
   * auch die teuerste, und die liest die Monatsebene.
   *
   * <p><b>Erfuellt keines der drei beide Bedingungen, greift der Leerzustand</b> — und der Endpunkt
   * nennt dann trotzdem ein Paar, naemlich das erste der Reihe. Die Oberflaeche braucht eines: Ohne
   * gewaehltes Paar gaebe es nichts hervorzuheben und nichts in die URL zu schreiben. Ein Mandant
   * ohne Daten sieht damit dieselbe Auswahl wie jeder andere und darf durchschalten; er findet
   * ueberall denselben Satz.
   */
  private Rollupzeitraum standardfenster(MandantContext mandant, LocalDateTime jetzt) {
    for (Rollupzeitraum kandidat : Rollupzeitraum.reihe()) {
      Belegung belegung = dashboardRepository.belegung(mandant, kandidat, kandidat.fenster(jetzt));
      boolean genugEimer = belegung.belegteEimer() * 2 >= kandidat.eimer();
      boolean genugVerkehr = belegung.groessterEimer() > EIMER_MINDESTENS;
      if (genugEimer && genugVerkehr) {
        return kandidat;
      }
    }
    return Rollupzeitraum.reihe().getFirst();
  }

  /** Block 7 — {@code null}, solange es keinen abgeschlossenen, fehlerfreien Lauf gibt. */
  private StandResponse stand() {
    return dashboardRepository
        .letzterLauf()
        .map(
            zeile ->
                new StandResponse(
                    zeile.beendetAm() == null ? null : zeile.beendetAm().toInstant(ZoneOffset.UTC),
                    zeile.art()))
        .orElse(null);
  }

  /**
   * Block 5, <b>eine Sicht</b>: Top 10, dann die beiden Restzeilen — <b>und die stehen immer
   * unten</b>. Gerufen wird sie je Sicht einmal, mit denselben Regeln fuer beide.
   *
   * <p>Die Abfrage liefert die benannten Werte bereits absteigend und „nicht zugeordnet" am Ende.
   * <b>Sortiert wird hier trotzdem noch einmal</b>, und zwar mit dem Wert als zweitem Schluessel:
   * Bei Gleichstand haengt die Reihenfolge sonst davon ab, in welcher Reihenfolge die Datenbank die
   * Gruppen zurueckgibt, und der Block spraenge zwischen zwei Aufrufen.
   *
   * <p><b>„Übrige" fehlt, wenn es keinen Rang 11 gibt</b> — eine Null ist dort reines Rangartefakt.
   * <b>„Nicht zugeordnet" erscheint immer, auch bei null:</b> Das ist eine Aussage ueber den
   * Katalog, und ohne sie waere <i>vollstaendig gepflegt</i> nicht von <i>diese Ansicht zeigt das
   * nicht</i> zu unterscheiden.
   */
  private VerteilungszeilenResponse verteilung(List<Verteilungssumme> summen) {
    long nichtZugeordnet = 0;
    List<Verteilungssumme> benannt = new ArrayList<>();
    for (Verteilungssumme summe : summen) {
      if (summe.schluessel() == null) {
        nichtZugeordnet += summe.anzahl();
      } else {
        benannt.add(summe);
      }
    }
    benannt.sort(
        Comparator.comparingLong(Verteilungssumme::anzahl)
            .reversed()
            .thenComparing(Verteilungssumme::schluessel));

    List<VerteilungszeileResponse> zeilen = new ArrayList<>();
    for (Verteilungssumme summe : benannt.subList(0, Math.min(VERTEILUNG_TOP, benannt.size()))) {
      zeilen.add(VerteilungszeileResponse.wert(summe.schluessel(), summe.anzahl()));
    }
    if (benannt.size() > VERTEILUNG_TOP) {
      List<Verteilungssumme> rest = benannt.subList(VERTEILUNG_TOP, benannt.size());
      zeilen.add(
          VerteilungszeileResponse.uebrige(
              rest.size(), rest.stream().mapToLong(Verteilungssumme::anzahl).sum()));
    }
    zeilen.add(VerteilungszeileResponse.nichtZugeordnet(nichtZugeordnet));
    return new VerteilungszeilenResponse(zeilen);
  }

  /**
   * Block 6: aus Zeilen des Repositorys werden Zeilen der Antwort — <b>eine je Prozess</b>.
   *
   * <h2>⚠️ Hier stand ein Aufruf des {@code MessageStatusClassifier}, und er ist entfallen</h2>
   *
   * <p>Bis zum 04.09.2026 trug jede Zeile einen <b>Rohstatus</b> und die daraus gebildete
   * Einordnung ({@code statusKind}). Beides gehoerte zu einer einzelnen Nachricht. <b>Eine Zeile,
   * die einen ganzen Prozess zusammenfasst, hat keinen Rohstatus</b> — sie kann zwanzig
   * verschiedene enthalten. Der Aufruf ist deshalb nicht weggespart, sondern gegenstandslos
   * geworden; die Bedingung, <i>welche</i> Zeilen ueberhaupt auffaellig sind, ruft weiterhin
   * derselbe Klassifizierer im Repository.
   *
   * <p><b>Die Kategorie bleibt und bleibt konstant.</b> Regel Q3 verlangt, dass Problemkategorien
   * getrennt und nie zu „Problem" zusammengefasst werden; heute ist es eine Aufzaehlung mit einem
   * Wert (offener Punkt 133). Kommt je eine zweite zurueck, steht in der Antwort ihr Platz — und
   * dann ist die Gruppierung nach Prozess <b>je Kategorie</b> zu fuehren und nicht darueber hinweg.
   * Das steht hier als Warnung und nicht als Bau: Es gibt keine zweite Kategorie.
   */
  private List<AuffaelligerProzessResponse> zuletztAufgefallen(List<AuffaelligerProzess> zeilen) {
    return zeilen.stream()
        .map(
            zeile ->
                new AuffaelligerProzessResponse(
                    zeile.processId(),
                    zeile.processName(),
                    zeile.anzahl(),
                    Zeitpunkte.nachUtc(zeile.zuletzt(), anwendungsuhr.getZone()),
                    Auffaelligkeit.FEHLER))
        .toList();
  }

  private FensterResponse fensterAntwort(Zeitfenster fenster) {
    return new FensterResponse(
        Zeitpunkte.nachUtc(fenster.von(), anwendungsuhr.getZone()),
        Zeitpunkte.nachUtc(fenster.bis(), anwendungsuhr.getZone()));
  }

  /**
   * Block 1: je Eimer die Aufschluesselung nach Einordnung.
   *
   * <p><b>Ein {@link TreeMap} und kein {@code HashMap}:</b> Die Eimer kommen zwar sortiert aus der
   * Abfrage, aber die Reihenfolge der Antwort darf nicht davon abhaengen, dass jemand das {@code
   * ORDER BY} stehen laesst.
   *
   * <p><b>Nur Einordnungen, die vorkommen.</b> Alle acht je Eimer waeren bei 48 Stunden 384
   * Eintraege, die meisten null. Die Reihenfolge ist die der Aufzaehlung ({@link EnumMap}) und
   * damit ueber alle Eimer dieselbe — eine Oberflaeche, die Farben nach Position vergibt, bekommt
   * sonst in jedem Balken eine andere.
   */
  private List<VerlaufspunktResponse> verlauf(List<Rollupsumme> summen) {
    Map<LocalDateTime, Map<MessageStatusKind, Long>> jeEimer = new TreeMap<>();
    for (Rollupsumme summe : summen) {
      jeEimer
          .computeIfAbsent(summe.eimer(), eimer -> new EnumMap<>(MessageStatusKind.class))
          .merge(statusClassifier.einordnung(summe.messageStatus()), summe.anzahl(), Long::sum);
    }

    List<VerlaufspunktResponse> punkte = new ArrayList<>(jeEimer.size());
    for (Map.Entry<LocalDateTime, Map<MessageStatusKind, Long>> eintrag : jeEimer.entrySet()) {
      List<EinordnungszahlResponse> einordnungen =
          eintrag.getValue().entrySet().stream()
              .map(je -> new EinordnungszahlResponse(je.getKey(), je.getValue()))
              .toList();
      long gesamt = einordnungen.stream().mapToLong(EinordnungszahlResponse::anzahl).sum();
      punkte.add(
          new VerlaufspunktResponse(
              Zeitpunkte.nachUtc(eintrag.getKey(), anwendungsuhr.getZone()), gesamt, einordnungen));
    }
    return punkte;
  }

  /**
   * Bloecke 2 und 3 — <b>aus denselben Zeilen wie der Verlauf</b>, ohne einen zweiten Lesevorgang.
   *
   * <p>Die Fehlerarten kommen aus dem Rohwert und nicht aus der Einordnung: Die Einordnung sagt
   * <i>Fehler</i>, der Rohwert sagt <i>welcher</i>.
   */
  /**
   * Bloecke 4 und 4a — <b>eine Kachel je offenem Zustand, zwei Werte aus einem Statement</b>.
   *
   * <p>Bricht die Live-Abfrage an der Zeitgrenze ab, ist <i>diese</i> Kachel „nicht ermittelbar" —
   * die andere bleibt davon unberuehrt. Das ist der Unterschied zur alten Ueberfaellig-Kachel, wo
   * zwei Zahlen <i>derselben</i> Kachel zusammenfielen: Dort waren „im Zeitraum" und „insgesamt"
   * ein Paar, das man nebeneinander liest. <i>Laeuft</i> und <i>Wartend</i> sind zwei verschiedene
   * Auskuenfte und keine Rechnung.
   *
   * <h2>{@code aeltesteSekunden} entsteht hier und nicht im Repository</h2>
   *
   * <p>Das Repository liefert den rohen Zeitpunkt; die Dauer wird gegen die <b>Anwendungsuhr</b>
   * gerechnet (Regel Z1) — <b>ein Repository liest keine Uhr</b>. {@code jetzt} ist derselbe
   * Uhrenschlag, den auch das Fenster benutzt.
   *
   * <p><b>{@code null} bei {@code anzahl = 0}</b>: Ohne Zeile gibt es kein Alter. Und {@code null}
   * bei einem negativen Abstand — dieselbe Regel wie in {@code docs/nachrichtendetail.md} §3a:
   * „wartet seit minus drei Sekunden" ist schlechter als gar keine Angabe. Vorgekommen ist das
   * nicht; die Anwendungsuhr laeuft vorwaerts.
   */
  private OffeneKachelResponse offeneKachel(
      MandantContext mandant, MessageStatusKind einordnung, LocalDateTime jetzt) {
    return dashboardRepository
        .offeneNachrichten(mandant, einordnung)
        .map(
            stand ->
                OffeneKachelResponse.von(stand.anzahl(), alterSekunden(stand.aelteste(), jetzt)))
        .orElseGet(OffeneKachelResponse::nichtErmittelbar);
  }

  /**
   * Der Abstand in ganzen Sekunden, {@code null} bei fehlendem Wert und bei negativem Abstand.
   *
   * <p>Die Rechnung steht seit Schritt 10d in {@link Alter} — dieselbe Regel traegt jetzt auch das
   * Alter der Dienstlampen und des Pruefzeitpunkts der Ablagenkachel. <b>Vier Stellen waeren vier
   * Gelegenheiten, sie unterschiedlich zu machen.</b>
   */
  private static Long alterSekunden(LocalDateTime aelteste, LocalDateTime jetzt) {
    return Alter.sekunden(aelteste, jetzt);
  }

  private KachelnResponse kacheln(
      List<Rollupsumme> summen, OffeneKachelResponse laeuft, OffeneKachelResponse wartend) {
    long nachrichten = 0;
    long fehler = 0;
    Map<String, Long> jeRohwert = new LinkedHashMap<>();
    for (Rollupsumme summe : summen) {
      nachrichten += summe.anzahl();
      if (statusClassifier.einordnung(summe.messageStatus()) == MessageStatusKind.FEHLER) {
        fehler += summe.anzahl();
        jeRohwert.merge(summe.messageStatus(), summe.anzahl(), Long::sum);
      }
    }

    List<FehlerartResponse> arten =
        jeRohwert.entrySet().stream()
            .map(
                eintrag ->
                    new FehlerartResponse(
                        eintrag.getKey(),
                        statusClassifier.fehlerart(eintrag.getKey()),
                        eintrag.getValue()))
            .sorted(
                Comparator.comparingLong(FehlerartResponse::anzahl)
                    .reversed()
                    .thenComparing(
                        FehlerartResponse::rohwert,
                        Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();

    return new KachelnResponse(
        nachrichten, new FehlerkachelResponse(fehler, arten), laeuft, wartend);
  }
}
