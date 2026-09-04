package de.kraftwerkone.overlord.monitor.dashboard;

import de.kraftwerkone.overlord.monitor.common.MessageStatusClassifier;
import de.kraftwerkone.overlord.monitor.common.MessageStatusKind;
import de.kraftwerkone.overlord.monitor.common.Rollupzeitraum;
import de.kraftwerkone.overlord.monitor.common.Zeitfenster;
import de.kraftwerkone.overlord.monitor.common.Zeitpunkte;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
 * Aufruf, der eine Eimergrenze streift, zwei verschiedene Fenster.
 */
@Service
public class DashboardService {

  private final DashboardRepository dashboardRepository;
  private final MessageStatusClassifier statusClassifier;
  private final Clock anwendungsuhr;

  DashboardService(
      DashboardRepository dashboardRepository,
      MessageStatusClassifier statusClassifier,
      Clock anwendungsuhr) {
    this.dashboardRepository = dashboardRepository;
    this.statusClassifier = statusClassifier;
    this.anwendungsuhr = anwendungsuhr;
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
   * @param mandant Regel M2 — erster Pflichtparameter, und er kommt aus der Sitzung (Regel M1)
   * @param gewaehlt das Paar aus der URL, oder {@code null}
   * @param sicht wonach der Verteilungsblock gruppiert
   */
  public DashboardResponse landingpage(
      MandantContext mandant, Rollupzeitraum gewaehlt, Verteilungssicht sicht) {
    LocalDateTime jetzt = LocalDateTime.now(anwendungsuhr);
    Rollupzeitraum zeitraum = gewaehlt == null ? standardfenster(mandant, jetzt) : gewaehlt;
    Zeitfenster fenster = zeitraum.fenster(jetzt);

    List<Rollupsumme> summen = dashboardRepository.verlauf(mandant, zeitraum, fenster);
    List<Verteilungssumme> verteilt =
        dashboardRepository.verteilung(mandant, zeitraum, fenster, sicht);
    OffeneKachelResponse laeuft = offeneKachel(mandant, MessageStatusKind.LAEUFT, jetzt);
    // Die Erscheinungsbedingung wird bei JEDEM Aufruf mitgelesen und nicht bedingt: Sonst haenge
    // die Zahl der Statements am Mandanten, und DashboardStatementsTest waere nicht mehr
    // deterministisch. Entschieden wird erst hier, ob die Kachel in die Antwort kommt.
    boolean zeigeWartend = dashboardRepository.hatWartendeAblaeufe(mandant);
    OffeneKachelResponse wartend = offeneKachel(mandant, MessageStatusKind.WARTEND, jetzt);
    List<Auffaelligkeitszeile> aufgefallen =
        dashboardRepository.zuletztAufgefallen(mandant, fenster, AUFFAELLIG_HOECHSTENS);

    KachelnResponse kacheln = kacheln(summen, laeuft, zeigeWartend ? wartend : null);
    return new DashboardResponse(
        zeitraum.code(),
        fensterAntwort(fenster),
        kacheln.nachrichten() == 0,
        verlauf(summen),
        kacheln,
        verteilung(verteilt, sicht),
        zuletztAufgefallen(aufgefallen),
        stand());
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
   * Block 5: Top 10, dann die beiden Restzeilen — <b>und die stehen immer unten</b>.
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
  private VerteilungResponse verteilung(List<Verteilungssumme> summen, Verteilungssicht sicht) {
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
    return new VerteilungResponse(sicht, zeilen);
  }

  /**
   * Block 6: die Einordnung entsteht aus dem Rohwert und nicht aus einer zweiten Spalte.
   *
   * <p><b>Die Kategorie ist seit dem 03.09.2026 nicht mehr abzuleiten, sondern festzustellen.</b>
   * Bis dahin filterte die Abfrage mit {@code fehlerBedingung OR ueberfaelligBedingung}, und was
   * kein Fehler war, war ueberfaellig. Die Ueberfaelligkeitshaelfte ist mit E‑71 entfallen; der
   * Block liest nur noch die Fehlerbedingung, und {@link Auffaelligkeit} traegt nur noch einen
   * Wert.
   *
   * <p><b>Die Einordnung wird trotzdem weiterhin gerufen</b> und nicht durch ein Literal ersetzt:
   * Sie steht als eigenes Feld in der Antwort ({@code statusKind}) und sagt dort etwas, das die
   * Kategorie nicht sagt — welcher Rohwert es genau ist. <b>Nur die Kategorie ist konstant, nicht
   * die Einordnung.</b>
   */
  private List<AuffaelligeNachrichtResponse> zuletztAufgefallen(List<Auffaelligkeitszeile> zeilen) {
    return zeilen.stream()
        .map(
            zeile ->
                new AuffaelligeNachrichtResponse(
                    zeile.messageId(),
                    Zeitpunkte.nachUtc(zeile.zeitpunkt(), anwendungsuhr.getZone()),
                    zeile.messageStatus(),
                    statusClassifier.einordnung(zeile.messageStatus()),
                    Auffaelligkeit.FEHLER,
                    zeile.processId(),
                    zeile.sosName()))
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

  /** Der Abstand in ganzen Sekunden, {@code null} bei fehlendem Wert und bei negativem Abstand. */
  private static Long alterSekunden(LocalDateTime aelteste, LocalDateTime jetzt) {
    if (aelteste == null) {
      return null;
    }
    long sekunden = Duration.between(aelteste, jetzt).toSeconds();
    return sekunden < 0 ? null : sekunden;
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
