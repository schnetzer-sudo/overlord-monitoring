package de.kraftwerkone.overlord.monitor.catalog;

import de.kraftwerkone.overlord.monitor.common.Baumfenster;
import de.kraftwerkone.overlord.monitor.common.Baumgliederung;
import de.kraftwerkone.overlord.monitor.common.FehlerLiveErgebnis;
import de.kraftwerkone.overlord.monitor.common.FehlerLiveErsatz;
import de.kraftwerkone.overlord.monitor.common.FehlerLiveResponse;
import de.kraftwerkone.overlord.monitor.common.FehlerLiveService;
import de.kraftwerkone.overlord.monitor.common.FehlerLiveZeile;
import de.kraftwerkone.overlord.monitor.common.Katalogzuordnung;
import de.kraftwerkone.overlord.monitor.common.LiveRestErgebnis;
import de.kraftwerkone.overlord.monitor.common.LiveRestKorrektur;
import de.kraftwerkone.overlord.monitor.common.LiveRestResponse;
import de.kraftwerkone.overlord.monitor.common.LiveRestService;
import de.kraftwerkone.overlord.monitor.common.LiveRestZeile;
import de.kraftwerkone.overlord.monitor.common.MandantContext;
import de.kraftwerkone.overlord.monitor.common.MessageStatusClassifier;
import de.kraftwerkone.overlord.monitor.common.MessageStatusKind;
import de.kraftwerkone.overlord.monitor.common.Rollupzeitraum;
import de.kraftwerkone.overlord.monitor.common.Zeitfenster;
import de.kraftwerkone.overlord.monitor.common.Zeitpunkte;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import org.springframework.stereotype.Service;

/**
 * Setzt den Prozessbaum zusammen — <b>aus zwei Lesevorgaengen, und die Gliederung entsteht hier und
 * nicht in SQL</b>.
 *
 * <h2>Warum die Gruppierung in Java steht</h2>
 *
 * <p>Der Baum ist eine <b>Schachtelung</b> und keine Gruppierung: Partner → Richtung → Prozess, mit
 * Summen auf jeder Ebene. In SQL waeren das entweder drei Abfragen mit drei Bereichszugriffen auf
 * dieselben Rollupzeilen oder ein {@code ROLLUP}, dessen Zwischenzeilen der Aufrufer erst wieder
 * auseinandersortieren muesste. Hier stehen die Zeilen ohnehin schon im Speicher — die Schachtelung
 * kostet einen Durchlauf ueber hoechstens 733 Zeilen.
 *
 * <h2>Zwei Gliederungen, ein Aufbau <i>(seit 15.09.2026)</i></h2>
 *
 * <p>Neben Partner → Richtung → Prozess gibt es {@link Baumgliederung#PROJEKT}: Projektbeschreibung
 * → Prozess (E-139). <b>Beide lesen dieselben zwei Statements</b> — das Geruest traegt die Spalten
 * beider Gliederungen —, und beide entstehen in derselben Schleife ({@link #knoten}) aus einer
 * Liste von {@link Gruppierung Ebenen}. Ein zweiter Aufbau fuer den Projektbaum waere eine zweite
 * Stelle fuer Summen, Reihenfolge und Schreibweisen, und die liefe beim naechsten Feld auseinander.
 * Gliedern ist damit der einzige Unterschied, und er steht in {@link #gruppierungen}.
 *
 * <h2>Ein Uhrenschlag je Anfrage</h2>
 *
 * <p>{@code jetzt} kommt genau einmal aus der <b>Anwendungsuhr</b> (Regel Z1) und wird danach
 * herumgereicht — an das Zeitfenster <b>und</b> an die Stilleschwelle. Zwei Schlaege waeren zwei
 * Stichtage, und ein Prozess koennte im selben Aufruf im Fenster liegen und trotzdem gegen einen
 * anderen Stichtag als still gelten.
 *
 * <h2>Der Live-Rest wird verrechnet <i>(seit 17.09.2026)</i></h2>
 *
 * <p>Die Kennzahlen kommen aus dem Rollup, und der Rollup rechnet den angebrochenen Eimer nur
 * einmal pro Stunde. Der <b>Live-Rest</b> ({@link LiveRestService}, {@code docs/live-rest.md})
 * liefert je {@code (stunde, process_id, message_status)} eine vorzeichenbehaftete Korrektur fuer
 * den Bereich seit dem letzten Lauf; hier werden davon nur die Stunden verrechnet, die <b>im
 * Fenster</b> liegen, und die Rohwerte wie alle anderen ueber den Klassifizierer eingeordnet. Kein
 * Endwert wird negativ. Die <b>letzte Bewegung</b> ist das Maximum aus dem Wert nach E-34 und der
 * juengsten Live-Stunde des Prozesses — unabhaengig vom Fenster (E-35): Ein Prozess mit Verkehr in
 * der laufenden Stunde steht nie als still oder nie da.
 *
 * <h2>Die Fehler kommen live <i>(seit 18.09.2026, E-208, E-213)</i></h2>
 *
 * <p>Ein Fehlerstatus ist durch Nachverarbeitung nicht endgueltig, und der Delta-Lauf entfernt
 * einen Abgang aus einem alten Eimer nicht. Der Baum ruft deshalb <b>denselben Baustein wie die
 * Uebersicht</b> ({@link FehlerLiveService}, {@code docs/fehler-live.md} §5b): Nachdem der
 * Live-Rest verrechnet ist, fallen die Zeilen je Prozess und Rohstatus heraus, die der
 * Klassifizierer als {@code FEHLER} einordnet, und die Zeilen der Lesung kommen hinzu ({@link
 * FehlerLiveErsatz}). <i>Nachrichten</i> und <i>Fehler</i> entstehen danach wie bisher, die Klemme
 * bleibt, wo sie war. Die Lesung ist das <b>letzte</b> Statement eines Aufrufs; faellt sie aus,
 * rechnet der Baum wie vorher und sagt es im Block {@code fehlerLive}. Letzte Bewegung und Zustand
 * beruehrt sie nicht.
 *
 * <h2>Die Einordnung wird gerufen, nicht nachgebaut</h2>
 *
 * <p>Im Rollup steht der <b>Rohwert</b> (Entscheidung E-g). Ob eine Zeile Fehler ist, entscheidet
 * {@link MessageStatusClassifier#einordnung(String)} — dieselbe Stelle, die Liste, Dashboard und
 * spaeter der Chatbot benutzen. <b>Ein zweites {@code switch} ueber Statuswoerter darf in diesem
 * Paket nicht entstehen:</b> Es liefe beim naechsten neuen Statuswert von der Liste weg, und der
 * Unterschied fiele erst auf, wenn jemand zwei Zahlen nebeneinanderlegt.
 */
@Service
public class ProzessbaumService {

  /**
   * <b>Ab wann ein Prozess als still gilt — die fachliche Festlegung dieses Schritts.</b>
   *
   * <p>Drei Monate, und die Zahl steht hier als benannter Wert und nicht als Zahl in einem
   * Statement: Sie ist eine fachliche Festlegung und wird sich aendern.
   *
   * <p><b>Sie gilt unabhaengig vom gewaehlten Zeitfenster.</b> Waehlt jemand 48 Stunden, heisst
   * „still" trotzdem <i>seit drei Monaten nichts</i>. Eine fensterabhaengige Schwelle markierte bei
   * 48 Stunden den Normalfall — bei {@code NEXANS} traegt in einem 48-Stunden-Fenster nur ein
   * Bruchteil der 733 Prozesse Verkehr, und 733 Markierungen sind keine Markierung mehr.
   *
   * <p><b>Warum nicht kuerzer:</b> Gemessen am Stichtag der Testkopie sind bei drei Monaten 28 von
   * 733 Prozessen ({@code NEXANS}) still — 3,8 %. Das ist eine Menge, die jemand durchsieht.
   *
   * <p><b>Warum ein {@link Period} und keine {@code Duration}:</b> Drei Monate sind keine feste
   * Zahl von Tagen. {@code Duration.ofDays(90)} waere eine Naeherung mit einem Fehler von bis zu
   * zwei Tagen, und die Schwelle traegt eine fachliche Aussage und keine Rechnung.
   */
  static final Period STILLE_SCHWELLE = Period.ofMonths(3);

  /**
   * Das Paar, das ohne Parameter gilt.
   *
   * <p><b>Der Endpunkt waehlt es nicht selbst</b>, anders als das Dashboard. Dessen Belegungsprobe
   * gibt es hier bewusst nicht, und der Grund ist die Bauform der Ansicht: Das Dashboard zeigt ein
   * <b>Diagramm</b>, und ein Diagramm mit ueberwiegend leeren Eimern zeigt nichts. Der Baum zeigt
   * <b>alle Prozesse des Mandanten</b>, unabhaengig vom Fenster — er ist auch bei einem leeren
   * Fenster vollstaendig, nur stehen dann ueberall Nullen. Eine Probe, die ein Fenster sucht, in
   * dem etwas passiert ist, loeste hier ein Problem, das die Ansicht nicht hat, und kostete dafuer
   * eine bis drei zusaetzliche Abfragen je Aufruf.
   */
  static final Rollupzeitraum VORGABE = Rollupzeitraum.STUNDEN_48;

  /**
   * <b>Alphabetisch nach dem hochgestellten Schluessel, {@code null} ans Ende</b> — die Ordnung
   * jeder Gruppenebene ausser der Richtung.
   *
   * <p>Fuer den Partner ist das E-39, fuer das Projekt E-142. <b>Die Reihenfolge des Altwerkzeugs
   * wird dabei bewusst nicht nachgebildet</b>: Dort folgt die Prozessebene innerhalb eines Partners
   * offenbar der Kennung. Ein Baum ist zum Finden da, und gefunden wird ueber den Namen.
   *
   * <p><b>Ueber den Schluessel und nicht ueber den Rohwert:</b> Sonst stuende eine
   * kleingeschriebene Schreibweise hinter allen grossgeschriebenen, weil Kleinbuchstaben in der
   * natuerlichen Ordnung hinter den Grossbuchstaben liegen.
   */
  private static final Comparator<String> ALPHABETISCH =
      Comparator.nullsLast(Comparator.naturalOrder());

  /**
   * Partner. <b>Gruppiert wird ueber {@link Katalogzuordnung#gruppenschluessel(String, String)}</b>
   * und nicht ueber den Rohwert. Daran haengen <b>zwei</b> Entscheidungen, und beide sind mehr als
   * eine Feinheit:
   *
   * <ol>
   *   <li><b>Entscheidung E-i:</b> Ein <i>Regelvorschlag</i> der Heuristik steht mit {@code
   *       pflegestatus = OFFEN} in derselben Spalte wie eine kuratierte Zuordnung. Gruppierte der
   *       Baum darueber, stuenden Prozesse unter einem Partner, den niemand bestaetigt hat — und
   *       der Nutzer saehe der Gruppe nicht an, dass sie geraten ist (Regel Q4).
   *   <li><b>Die Gleichheit kommt von der Spalte und nicht von Java</b> (M117): {@code
   *       utf8mb4_general_ci} macht zwei Schreibweisen zu <i>einem</i> Wert. Ohne den
   *       hochgestellten Schluessel zeigte der Baum bei {@code NEXANS} einen Partner zweimal, mit
   *       geteilten Zahlen — und widerspraeche damit der Verteilung des Dashboards, die in SQL
   *       gruppiert.
   * </ol>
   *
   * <p><b>Angezeigt wird trotzdem der Rohwert</b>, nicht der hochgestellte Schluessel: Der Katalog
   * ist die Wahrheit, und was dort steht, wird nicht umgeschrieben (Regel Q4).
   */
  private static final Gruppierung NACH_PARTNER =
      new Gruppierung(
          Baumebene.PARTNER,
          zeile -> Katalogzuordnung.gruppenschluessel(zeile.pflegestatus(), zeile.partner()),
          zeile -> Katalogzuordnung.schluessel(zeile.pflegestatus(), zeile.partner()),
          ALPHABETISCH);

  /**
   * Richtung. <b>Nur die Richtungen, die vorkommen</b> — kein leerer Ast. Die Reihenfolge ist die
   * der Aufzaehlung {@link Richtung} und danach „nicht ermittelt"; sie ist damit ueber alle Partner
   * dieselbe, und eine Oberflaeche, die Symbole nach Position vergibt, bekaeme sonst unter jedem
   * Partner eine andere.
   */
  private static final Gruppierung NACH_RICHTUNG =
      new Gruppierung(
          Baumebene.RICHTUNG,
          zeile -> Katalogzuordnung.gruppenschluessel(zeile.pflegestatus(), zeile.richtung()),
          zeile -> Katalogzuordnung.schluessel(zeile.pflegestatus(), zeile.richtung()),
          Comparator.<String>comparingInt(ProzessbaumService::richtungsrang)
              .thenComparing(ALPHABETISCH));

  /**
   * Projekt (E-139). <b>Gruppiert wird ueber den Text {@code ProjectDescription} und nicht ueber
   * {@code ProjectID}</b> (E-141): Mehrere Projekte mit derselben Beschreibung sind fuer den Nutzer
   * ein Knoten; nach Kennung gruppiert stuende dieselbe Beschriftung mehrmals untereinander.
   *
   * <p><b>Damit ist der Schluessel wieder ein freier Text in {@code utf8mb4_general_ci}, und E-41
   * gilt unveraendert</b>: hochgestellt ueber {@link #hochgestellt}, angezeigt in der zuerst
   * angetroffenen Schreibweise ueber {@link #merkeAnzeige} — derselbe Weg wie beim Partner, nicht
   * ein zweiter.
   *
   * <p><b>Kein Knoten „nicht zugeordnet" und keine Rueckfallregel</b> (E-144). Jeder Prozess hat
   * aus dem Schema heraus genau ein Projekt. Eine leere Beschreibung gibt es nach Auskunft des
   * Auftraggebers nicht; kaeme sie doch, stuende sie als eigener Knoten mit ihrem Rohwert da — sie
   * wird weder umbenannt noch auf einen anderen Wert gezogen.
   */
  private static final Gruppierung NACH_PROJEKT =
      new Gruppierung(
          Baumebene.PROJEKT,
          zeile -> hochgestellt(zeile.projectDescription()),
          Prozessgeruestzeile::projectDescription,
          ALPHABETISCH);

  private final ProzessbaumRepository prozessbaumRepository;
  private final MessageStatusClassifier statusClassifier;
  private final Clock anwendungsuhr;
  private final LiveRestService liveRestService;
  private final FehlerLiveService fehlerLiveService;

  ProzessbaumService(
      ProzessbaumRepository prozessbaumRepository,
      MessageStatusClassifier statusClassifier,
      Clock anwendungsuhr,
      LiveRestService liveRestService,
      FehlerLiveService fehlerLiveService) {
    this.prozessbaumRepository = prozessbaumRepository;
    this.statusClassifier = statusClassifier;
    this.anwendungsuhr = anwendungsuhr;
    this.liveRestService = liveRestService;
    this.fehlerLiveService = fehlerLiveService;
  }

  /**
   * Der ganze Baum fuer den aktiven Mandanten, in der verlangten Gliederung.
   *
   * <p><b>Ein Paar oder ein freies Fenster — der Weg ist derselbe.</b> Beides ist ein {@link
   * Baumfenster}, beides liefert Segmente, und das Repository rendert fuer ein Segment den Text der
   * Paare und fuer mehrere die Vereinigung. Der Dienst rechnet hier nichts nach, rundet nichts und
   * kennt keine Ebene; er reicht die Segmente durch und nennt in der Antwort den Code des Fensters
   * — {@code 48H}, {@code 30T}, {@code 12M} oder {@code FREI}.
   *
   * <p><b>Die Gliederung aendert keinen Zugriff</b> <i>(seit 15.09.2026)</i>: dieselben zwei
   * Statements, dieselben Zeilen, dieselbe Kopfzahl. Sie entscheidet allein, wie die Zeilen hier
   * geschachtelt werden — und damit auch, dass beide Baeume dieselben Blaetter tragen.
   *
   * <p><b>Die Reihenfolge der Statements</b> <i>(seit 18.09.2026)</i>: Geruest, Kennzahlen,
   * Wasserstand, bei angewandtem Live-Rest dessen zwei Lesungen — und <b>zuletzt die
   * Fehlerlesung</b> ueber das Fenster der Antwort. So bleiben die Stellen der Statements davor,
   * wie sie waren; anders als in der Uebersicht haengt hier kein Statement am Zustand der Lesung.
   *
   * @param mandant Regel M2 — erster Pflichtparameter, und er kommt aus der Sitzung (Regel M1)
   * @param gewaehlt das Fenster aus der URL, oder {@code null} fuer {@link #VORGABE}
   * @param gliederung die aktive Gliederung — <b>nie {@code null}</b>. Fehlt sie in der URL, setzt
   *     der Controller die Vorgabe des angemeldeten Kontos ein; der Dienst kennt kein Konto (E-143)
   */
  public ProzessbaumResponse baum(
      MandantContext mandant, Baumfenster gewaehlt, Baumgliederung gliederung) {
    Objects.requireNonNull(gliederung, "Ohne Gliederung gibt es keinen Baum");
    LocalDateTime jetzt = LocalDateTime.now(anwendungsuhr);
    Baumfenster baumfenster = gewaehlt == null ? Baumfenster.paar(VORGABE) : gewaehlt;
    Zeitfenster fenster = baumfenster.fenster(jetzt);

    List<Prozessgeruestzeile> geruest = prozessbaumRepository.geruest(mandant);
    List<Prozesskennzahlzeile> kennzahlen =
        prozessbaumRepository.kennzahlen(mandant, baumfenster.segmente(jetzt));
    // Derselbe Uhrenschlag wie fuer das Fenster: Der Live-Bereich endet in derselben Stunde.
    LiveRestErgebnis liveRest = liveRestService.ermittle(mandant, jetzt);
    // Die Fehlerlesung als letztes Statement, ueber das Fenster der Antwort aus demselben
    // Uhrenschlag — bei FREI mit dem ausschliessenden Ende (E-213).
    FehlerLiveErgebnis fehlerLive =
        fehlerLiveService.ermittle(mandant, fenster.von(), fenster.bis());
    Map<String, Kennzahl> jeProzess =
        kennzahlenJeProzess(kennzahlen, liveRest.korrektur(), fehlerLive, fenster);
    Map<String, LocalDateTime> juengsteLive = liveRest.korrektur().juengsteStundeJeProzess();
    List<Gruppierung> gruppierungen = gruppierungen(gliederung);

    return new ProzessbaumResponse(
        baumfenster.code(),
        gliederung,
        new ZeitfensterResponse(
            Zeitpunkte.nachUtc(fenster.von(), anwendungsuhr.getZone()),
            Zeitpunkte.nachUtc(fenster.bis(), anwendungsuhr.getZone())),
        (int) STILLE_SCHWELLE.toTotalMonths(),
        LiveRestResponse.aus(liveRest.entscheidung(), anwendungsuhr.getZone()),
        FehlerLiveResponse.aus(fehlerLive),
        gesamt(geruest, jeProzess, juengsteLive, jetzt),
        ebenen(gruppierungen),
        knoten(geruest, gruppierungen, jeProzess, juengsteLive, jetzt));
  }

  /**
   * Verdichtet die Zeilen der Kennzahlenabfrage auf eine Zahl je Prozess — und verrechnet den
   * Live-Rest, soweit er im Fenster liegt.
   *
   * <p>Aus <i>Prozess und Rohstatus</i> wird hier <i>Prozess</i>: Was Fehler ist, entscheidet der
   * Klassifizierer am Rohwert — <b>ein Lesevorgang, zwei Zahlen</b>. Die Korrekturzeilen gehen mit
   * ihrem Vorzeichen denselben Weg; ein Statuswechsel innerhalb eines Eimers laesst so die
   * Nachrichtenzahl gleich und aendert die Fehlerzahl.
   *
   * <p><b>Nur die Stunden, die im Fenster liegen</b> — {@code von} einschliessend, {@code bis}
   * ausschliessend, wie die Eimer des Rollups. Ein Fenster, das vor G endet, bekommt damit keine
   * Korrektur; die letzte Bewegung bekommt sie trotzdem ({@link #letzteBewegung}).
   *
   * <p><b>Kein negativer Endwert.</b> Minus und plus kommen aus zwei Lesungen desselben Bestands
   * und heben sich auf; laeuft der Bestand zwischen beiden weg, wird auf null geklemmt statt eine
   * negative Zahl anzuzeigen.
   *
   * <p><b>Fehler live</b> <i>(seit 18.09.2026, E-213)</i> sitzt zwischen beidem: <b>nach</b> der
   * Verrechnung des Live-Rests — die Korrektur traegt ihre eigenen Fehlerzeilen, und ein Fehler der
   * laufenden Stunde zaehlte sonst zweimal (E-210) — und <b>vor</b> der Bildung der zwei Zahlen,
   * denn nur die Zeilen tragen den Rohstatus. Dafuer stehen die Zeilen hier zuerst je <i>(Prozess,
   * Rohstatus)</i> ueber das Fenster summiert; die Zeilen der Lesung werden auf dieselbe Gestalt
   * gehoben. Ausgesetzt bleibt alles, wie es war. Die Klemme bleibt, wo sie war: je Prozess, nach
   * dem Ersatz.
   */
  private Map<String, Kennzahl> kennzahlenJeProzess(
      List<Prozesskennzahlzeile> zeilen,
      LiveRestKorrektur korrektur,
      FehlerLiveErgebnis fehlerLive,
      Zeitfenster fenster) {
    Map<Prozessstatus, Long> jeProzessUndStatus = new LinkedHashMap<>();
    for (Prozesskennzahlzeile zeile : zeilen) {
      jeProzessUndStatus.merge(
          new Prozessstatus(zeile.processId(), zeile.messageStatus()), zeile.anzahl(), Long::sum);
    }
    for (LiveRestZeile zeile : korrektur.zeilen()) {
      if (imFenster(zeile.stunde(), fenster)) {
        jeProzessUndStatus.merge(
            new Prozessstatus(zeile.processId(), zeile.messageStatus()), zeile.anzahl(), Long::sum);
      }
    }
    List<Prozesskennzahlzeile> ersetzt =
        FehlerLiveErsatz.ersetze(
            fehlerLive.zustand(),
            zeilen(jeProzessUndStatus),
            gehoben(fehlerLive.zeilen(), fenster),
            Prozesskennzahlzeile::messageStatus,
            statusClassifier);

    Map<String, Kennzahl> jeProzess = new HashMap<>();
    for (Prozesskennzahlzeile zeile : ersetzt) {
      jeProzess.merge(
          zeile.processId(), kennzahl(zeile.messageStatus(), zeile.anzahl()), Kennzahl::plus);
    }
    jeProzess.replaceAll((prozess, kennzahl) -> kennzahl.geklemmt());
    return jeProzess;
  }

  /**
   * <b>Die Zeilen der Fehlerlesung in der Gestalt der Kennzahlzeilen</b>: je <i>(Prozess,
   * Rohstatus)</i> ueber die Stunden des Fensters summiert — {@code von} einschliessend, {@code
   * bis} ausschliessend, wie die Eimer und wie in der Uebersicht. Die Lesung liest ohnehin nur das
   * Fenster; die Pruefung haelt die Haltung an einer Stelle mit der Korrektur des Live-Rests.
   */
  private static List<Prozesskennzahlzeile> gehoben(
      List<FehlerLiveZeile> fehlerzeilen, Zeitfenster fenster) {
    Map<Prozessstatus, Long> summe = new LinkedHashMap<>();
    for (FehlerLiveZeile zeile : fehlerzeilen) {
      if (imFenster(zeile.stunde(), fenster)) {
        summe.merge(
            new Prozessstatus(zeile.processId(), zeile.messageStatus()), zeile.anzahl(), Long::sum);
      }
    }
    return zeilen(summe);
  }

  private static List<Prozesskennzahlzeile> zeilen(Map<Prozessstatus, Long> jeProzessUndStatus) {
    List<Prozesskennzahlzeile> zeilen = new ArrayList<>(jeProzessUndStatus.size());
    jeProzessUndStatus.forEach(
        (schluessel, anzahl) ->
            zeilen.add(
                new Prozesskennzahlzeile(
                    schluessel.processId(), schluessel.messageStatus(), anzahl)));
    return zeilen;
  }

  /** {@code von} einschliessend, {@code bis} ausschliessend — wie die Eimer des Rollups. */
  private static boolean imFenster(LocalDateTime stunde, Zeitfenster fenster) {
    return !stunde.isBefore(fenster.von()) && stunde.isBefore(fenster.bis());
  }

  /**
   * Der Schluessel, ueber den der Ersatz tauscht: Prozess und <b>Rohstatus</b>, nicht die
   * Einordnung (E-g).
   */
  private record Prozessstatus(String processId, String messageStatus) {}

  private Kennzahl kennzahl(String messageStatus, long anzahl) {
    boolean fehler = statusClassifier.einordnung(messageStatus) == MessageStatusKind.FEHLER;
    return new Kennzahl(anzahl, fehler ? anzahl : 0);
  }

  /**
   * <b>Die letzte Bewegung: das Maximum aus dem Wert nach E-34 und der juengsten Live-Stunde</b> —
   * unabhaengig vom gewaehlten Fenster (E-35). Der Rollup kennt die laufende Stunde nur bis zum
   * Laufzeitpunkt; was die Quelle seither zaehlt, ist die juengere Bewegung.
   */
  private static LocalDateTime letzteBewegung(
      Prozessgeruestzeile zeile, Map<String, LocalDateTime> juengsteLive) {
    LocalDateTime ausDemRollup = zeile.letzteBewegung();
    LocalDateTime live = juengsteLive.get(zeile.processId());
    if (live == null) {
      return ausDemRollup;
    }
    return ausDemRollup == null || live.isAfter(ausDemRollup) ? live : ausDemRollup;
  }

  /**
   * Eine Ebene der Gliederung: <b>wonach eine Zeile gruppiert wird, wie die Gruppe heisst und in
   * welcher Reihenfolge die Gruppen stehen</b>.
   *
   * @param ebene der Name, wie er in {@code ebenen} der Antwort steht
   * @param schluessel der Gruppenschluessel — hochgestellt, damit Java so gruppiert, wie die Spalte
   *     vergleicht (E-41); {@code null} fuer die eine Gruppe ohne Wert
   * @param anzeige der Rohwert, der als {@code name} angezeigt wird
   * @param reihenfolge die Ordnung der Gruppenschluessel; sie muss {@code null} vertragen
   */
  private record Gruppierung(
      Baumebene ebene,
      Function<Prozessgeruestzeile, String> schluessel,
      Function<Prozessgeruestzeile, String> anzeige,
      Comparator<String> reihenfolge) {}

  /**
   * Die Gruppenebenen einer Gliederung, von aussen nach innen. Unter der letzten stehen die
   * Blaetter.
   *
   * <p><b>Vollstaendiges {@code switch} ohne {@code default}</b>: Eine dritte Gliederung ist ein
   * Compilerfehler und keine stille Voreinstellung.
   */
  private static List<Gruppierung> gruppierungen(Baumgliederung gliederung) {
    return switch (gliederung) {
      case PARTNER -> List.of(NACH_PARTNER, NACH_RICHTUNG);
      case PROJEKT -> List.of(NACH_PROJEKT);
    };
  }

  /** Die Ebenennamen der Antwort — die Gruppenebenen und darunter immer {@code PROZESS}. */
  private static List<Baumebene> ebenen(List<Gruppierung> gruppierungen) {
    List<Baumebene> ebenen = new ArrayList<>(gruppierungen.size() + 1);
    for (Gruppierung gruppierung : gruppierungen) {
      ebenen.add(gruppierung.ebene());
    }
    ebenen.add(Baumebene.PROZESS);
    return List.copyOf(ebenen);
  }

  /**
   * Eine Ebene des Baums, aufgebaut in zwei Schritten: erst schachteln, dann sortieren — <b>fuer
   * jede Ebene jeder Gliederung dieselbe Schleife</b>.
   *
   * <p>Geschachtelt wird ueber den <b>Gruppenschluessel</b>, angezeigt wird der <b>Rohwert</b>. Die
   * beiden fallen auseinander, sobald derselbe Wert in zwei Schreibweisen steht — genau der Fall,
   * den M117 bei {@code NEXANS} gefunden hat. Fuer die Datenbank ist das ein Wert ({@code
   * utf8mb4_general_ci}), fuer {@code String.equals} waeren es zwei.
   *
   * <p><b>Die Summen entstehen von den Blaettern nach oben</b>, auf jeder Ebene aus ihren Kindern.
   * Der Aufrufer soll nichts zusammenrechnen muessen (Richtlinie §5.1).
   *
   * <p><b>Die Blaetter behalten die Reihenfolge der Abfrage</b> — dort steht {@code ORDER BY
   * ProcessName}, in beiden Gliederungen. Die Einfuegereihenfolge der Schachtelung erhaelt sie.
   */
  private List<BaumknotenResponse> knoten(
      List<Prozessgeruestzeile> zeilen,
      List<Gruppierung> gruppierungen,
      Map<String, Kennzahl> jeProzess,
      Map<String, LocalDateTime> juengsteLive,
      LocalDateTime jetzt) {
    if (gruppierungen.isEmpty()) {
      List<BaumknotenResponse> blaetter = new ArrayList<>(zeilen.size());
      for (Prozessgeruestzeile zeile : zeilen) {
        blaetter.add(blatt(zeile, jeProzess, juengsteLive, jetzt));
      }
      return List.copyOf(blaetter);
    }

    Gruppierung ebene = gruppierungen.getFirst();
    List<Gruppierung> darunter = gruppierungen.subList(1, gruppierungen.size());

    Map<String, List<Prozessgeruestzeile>> geschachtelt = new LinkedHashMap<>();
    Map<String, String> anzeige = new HashMap<>();
    for (Prozessgeruestzeile zeile : zeilen) {
      String schluessel = ebene.schluessel().apply(zeile);
      merkeAnzeige(anzeige, schluessel, ebene.anzeige().apply(zeile));
      geschachtelt.computeIfAbsent(schluessel, unbenutzt -> new ArrayList<>()).add(zeile);
    }

    List<GruppenknotenResponse> gruppen = new ArrayList<>(geschachtelt.size());
    for (Map.Entry<String, List<Prozessgeruestzeile>> eintrag : geschachtelt.entrySet()) {
      List<BaumknotenResponse> kinder =
          knoten(eintrag.getValue(), darunter, jeProzess, juengsteLive, jetzt);
      gruppen.add(
          new GruppenknotenResponse(
              eintrag.getKey(),
              anzeige.get(eintrag.getKey()),
              anzahlProzesse(kinder),
              kinder.stream().mapToLong(BaumknotenResponse::nachrichten).sum(),
              kinder.stream().mapToLong(BaumknotenResponse::fehler).sum(),
              kinder));
    }
    gruppen.sort(Comparator.comparing(GruppenknotenResponse::schluessel, ebene.reihenfolge()));
    return List.<BaumknotenResponse>copyOf(gruppen);
  }

  /** Wie viele Blaetter unter diesen Kindern haengen — eine Gruppe zaehlt ihre, ein Blatt sich. */
  private static int anzahlProzesse(List<BaumknotenResponse> kinder) {
    int anzahl = 0;
    for (BaumknotenResponse kind : kinder) {
      anzahl +=
          switch (kind) {
            case GruppenknotenResponse gruppe -> gruppe.anzahlProzesse();
            case ProzessknotenResponse blatt -> 1;
          };
    }
    return anzahl;
  }

  /**
   * Haelt zu einem Gruppenschluessel die <b>zuerst angetroffene</b> Schreibweise fest.
   *
   * <p><b>Die erste und nicht irgendeine.</b> Die Blaetter kommen nach {@code ProcessName} sortiert
   * aus der Abfrage; „die erste" ist damit ueber zwei Aufrufe dieselbe. Waehlte der Dienst
   * stattdessen die zuletzt gesehene, spraenge die Beschriftung, sobald der Katalog eine Zeile
   * dazubekommt.
   *
   * <p>Welche der beiden Schreibweisen die richtige ist, entscheidet <b>niemand hier</b> — das ist
   * eine Katalogfrage und steht als offener Punkt in {@code docs/process-view.md} §10.
   */
  private static void merkeAnzeige(Map<String, String> anzeige, String schluessel, String rohwert) {
    if (schluessel != null) {
      anzeige.putIfAbsent(schluessel, rohwert);
    }
  }

  /**
   * Ein freier Text als Gruppenschluessel — <b>dieselbe Gleichheit wie {@link
   * Katalogzuordnung#gruppenschluessel(String, String)}</b>, fuer eine Spalte ohne Pflegestatus.
   *
   * <p>{@link Locale#ROOT}, damit die Umwandlung nicht an der Systemsprache haengt. Die Naeherung
   * an {@code utf8mb4_general_ci} ist dieselbe und hat dieselbe Grenze (Akzente, offener Punkt
   * 105).
   */
  private static String hochgestellt(String rohwert) {
    return rohwert == null ? null : rohwert.toUpperCase(Locale.ROOT);
  }

  /**
   * Der Rang einer Richtung in der Anzeige.
   *
   * <p>Drei Faelle, und der mittlere ist der Grund fuer diese Methode: Ein <b>gepflegter, aber
   * unbekannter</b> Wert — den die Spalte zulaesst, weil sie {@code varchar} ist und die Whitelist
   * im Code steht — faellt hinter die bekannten und <b>vor</b> „nicht ermittelt". Er verschwindet
   * damit nicht und wird auch nicht mit dem leeren Feld verwechselt.
   *
   * @param richtung der <b>Gruppenschluessel</b> und nicht der Rohwert. Damit ordnet sich ein
   *     kleingeschriebenes {@code eingehend} im Katalog richtig ein, statt als „unbekannt" zu
   *     gelten — dieselbe Gleichheit, die die Datenbank auf der Spalte anwendet
   */
  private static int richtungsrang(String richtung) {
    if (richtung == null) {
      return Richtung.values().length + 1;
    }
    for (Richtung bekannt : Richtung.values()) {
      if (bekannt.name().equals(richtung)) {
        return bekannt.ordinal();
      }
    }
    return Richtung.values().length;
  }

  private ProzessknotenResponse blatt(
      Prozessgeruestzeile zeile,
      Map<String, Kennzahl> jeProzess,
      Map<String, LocalDateTime> juengsteLive,
      LocalDateTime jetzt) {
    Kennzahl kennzahl = jeProzess.getOrDefault(zeile.processId(), Kennzahl.LEER);
    LocalDateTime letzteBewegung = letzteBewegung(zeile, juengsteLive);
    return new ProzessknotenResponse(
        zeile.processId(),
        zeile.processName(),
        zeile.processId(),
        kennzahl.nachrichten(),
        kennzahl.fehler(),
        Zeitpunkte.nachUtc(letzteBewegung, anwendungsuhr.getZone()),
        zustand(letzteBewegung, jetzt));
  }

  /**
   * <b>Die drei Zustaende, abgeleitet aus einem Wert.</b>
   *
   * <p>Die Reihenfolge der Pruefung ist die Festlegung: {@code NIE} vor {@code STILL} vor {@code
   * BEWEGT}. Ein Prozess, der nie etwas trug, ist <b>nicht still — er war nie laut</b>, und die
   * Handlung daraus ist eine andere.
   *
   * <p><b>{@code isBefore} und nicht {@code isEqual}-tolerant:</b> Genau auf der Schwelle gilt ein
   * Prozess als bewegt. Die Grenze muss irgendwo liegen; sie zugunsten von „bewegt" zu ziehen
   * heisst, im Zweifel <b>nicht</b> zu markieren — eine Markierung, die auch im Grenzfall
   * erscheint, wird schneller uebersehen als eine, die es nicht tut.
   */
  static Prozesszustand zustand(LocalDateTime letzteBewegung, LocalDateTime jetzt) {
    if (letzteBewegung == null) {
      return Prozesszustand.NIE;
    }
    return letzteBewegung.isBefore(jetzt.minus(STILLE_SCHWELLE))
        ? Prozesszustand.STILL
        : Prozesszustand.BEWEGT;
  }

  private BaumsummeResponse gesamt(
      List<Prozessgeruestzeile> geruest,
      Map<String, Kennzahl> jeProzess,
      Map<String, LocalDateTime> juengsteLive,
      LocalDateTime jetzt) {
    int bewegt = 0;
    int still = 0;
    int nie = 0;
    long nachrichten = 0;
    long fehler = 0;
    for (Prozessgeruestzeile zeile : geruest) {
      switch (zustand(letzteBewegung(zeile, juengsteLive), jetzt)) {
        case BEWEGT -> bewegt++;
        case STILL -> still++;
        case NIE -> nie++;
      }
      Kennzahl kennzahl = jeProzess.getOrDefault(zeile.processId(), Kennzahl.LEER);
      nachrichten += kennzahl.nachrichten();
      fehler += kennzahl.fehler();
    }
    return new BaumsummeResponse(geruest.size(), bewegt, still, nie, nachrichten, fehler);
  }

  /**
   * Die beiden Zahlen eines Prozesses im Fenster, waehrend die Rohstatuszeilen verdichtet werden.
   */
  private record Kennzahl(long nachrichten, long fehler) {

    private static final Kennzahl LEER = new Kennzahl(0, 0);

    Kennzahl plus(Kennzahl weitere) {
      return new Kennzahl(nachrichten + weitere.nachrichten, fehler + weitere.fehler);
    }

    /** Kein negativer Endwert — beide Zahlen einzeln auf null geklemmt. */
    Kennzahl geklemmt() {
      return new Kennzahl(Math.max(0, nachrichten), Math.max(0, fehler));
    }
  }
}
