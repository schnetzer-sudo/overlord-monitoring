package de.kraftwerkone.overlord.monitor.dashboard;

import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.MESSAGE;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.PROCESS;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.PROJECTMANDANT;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.SOS;
import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.MESSAGE_ROLLUP;
import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.MESSAGE_ROLLUP_MONAT;
import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.MESSAGE_ROLLUP_TAG;
import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.PROCESS_CATALOG;
import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.ROLLUP_LAUF;

import de.kraftwerkone.overlord.monitor.common.MessageStatusClassifier;
import de.kraftwerkone.overlord.monitor.common.Pflegestatus;
import de.kraftwerkone.overlord.monitor.common.Zeitfenster;
import de.kraftwerkone.overlord.monitor.jooq.glassfish.tables.Process;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.math.BigDecimal;
import java.sql.SQLTimeoutException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record2;
import org.jooq.Table;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

/**
 * Der Datenzugriff des Dashboards — <b>lesend, ueber {@code glassfishDsl}, schemauebergreifend</b>.
 *
 * <h2>Warum der Lese-Kontext und nicht {@code monitorDsl}</h2>
 *
 * <p>Die Aufteilung der beiden {@code DSLContext} ist <b>lesen gegen schreiben</b> und nicht
 * <i>Quellschema gegen eigenes Schema</i> ({@code config/JooqConfig}): Der Lese-Kontext darf beide
 * Schemata lesen, der Schreib-Kontext darf ausschliesslich {@code overlord_monitor}. Jede Abfrage
 * hier joint {@code overlord_monitor.message_rollup*} gegen {@code GlassfishDB.Process} und braucht
 * dafuer <b>eine einzige Verbindung</b> — also den Lese-Pool. Der {@code ReadOnlyExecuteListener}
 * haengt daran und weist jeden Schreibversuch ab.
 *
 * <h2>Die Mandantenkette ist ein {@code EXISTS} und darf kein {@code JOIN} sein</h2>
 *
 * <p>{@code ProjectMandant} ist <b>n:m</b>. Ein {@code JOIN} vervielfachte jede Rollupzeile, sobald
 * ein Projekt an mehreren Mandanten haengt — und damit <b>die Summe</b>. Der Fehler waere still:
 * Die Zahlen saehen plausibel aus und waeren zu hoch. Dieselbe Begruendung steht in {@code
 * docs/rollup.md} §9b und in {@code message/VerengungRepository}.
 *
 * <p><b>{@code Project} steht nicht in der Kette.</b> {@code Process → ProjectMandant} ueber {@code
 * Process.ProjectID} liefert dieselbe Menge; der Umweg ueber {@code Project} ist entbehrlich
 * (Befund 48). Der ganze Anwendungscode nimmt die kurze Form — die Messskripte nehmen die lange,
 * und der Unterschied ist in {@code docs/dashboard.md} vermerkt.
 *
 * <h2>Drei Ebenen, drei Statements — und das ist Absicht</h2>
 *
 * <p>Jedes Zeitraumpaar liest seine eigene Tabelle ({@link Dashboardzeitraum}). Die Ebenen liessen
 * sich mit einem {@code CAST} auf einen gemeinsamen Schluesseltyp zusammenfassen; das kostete eine
 * Funktion um die Schluesselspalte und damit den Bereichszugriff. <b>Gemessen ist, was der Code
 * schickt</b> (Regel L7) — und was er schickt, sind drei Statements ohne Funktion um den
 * Schluessel, genau wie M94 und M107 sie gemessen haben.
 *
 * <p>Tages- und Monatsebene haben <b>dieselbe Form</b> ({@code DATE} als Schluessel) und teilen
 * sich deshalb eine Methode. Nur die Stundenebene ist eigen, weil ihr Schluessel ein {@code
 * DATETIME} ist.
 */
@Repository
public class DashboardRepository {

  /**
   * Der Alias fuer die Mandantenkette. Er ist noetig, sobald dieselbe Abfrage {@code Process} auch
   * ausserhalb des {@code EXISTS} anfasst — und er steht auch dort, wo sie es heute nicht tut,
   * damit der naechste Join nicht stillschweigend auf die falsche Tabelle zeigt.
   */
  private static final Process KETTE_PROCESS = PROCESS.as("dashboard_process");

  /**
   * Der Zeitindex auf {@code Message} — <b>der einzige Indexhinweis dieses Projekts</b>, und er
   * verbietet genau eines: ihn <i>zur Sortierung</i> zu verwenden ({@link #auffaellige}).
   *
   * <p><b>Warum ueberhaupt ein Hinweis, wo {@code STRAIGHT_JOIN} ausgeschlossen ist.</b> Das eine
   * ist ein Verbot der Join-Reihenfolge und war in M42 um Faktor 219 bis 1094 schlechter; dies hier
   * nimmt dem Optimierer <b>eine einzige Moeglichkeit</b> und laesst ihm die Wahl des
   * Zugriffspfads. Er ist ausserdem gemessen, und zwar in drei Fassungen (M108): ohne Hinweis, mit
   * {@code IGNORE INDEX FOR ORDER BY} und mit {@code FORCE INDEX (MessageStatusIDX)}.
   *
   * <p><b>Das Ergebnis in einer Zeile:</b> ohne Hinweis 1,6 ms im besten und <b>2.174 ms</b> im
   * schlechtesten gemessenen Fall — bei {@code VOTG} ueber zwoelf Monate lief das Statement in die
   * Zeitgrenze des Lese-Pools und endete mit {@code 500}. Mit Hinweis <b>22 bis 25 ms, ueber alle
   * drei gemessenen Mandanten und alle drei Fensterbreiten</b>. {@code IGNORE} und {@code FORCE}
   * sind dabei gleich schnell; genommen ist der schwaechere Eingriff.
   *
   * <p><b>Der Preis ist benannt:</b> Im guten Fall — ein Mandant mit Fehlern am Fensterrand —
   * kostet der Hinweis das Sieben- bis Fuenfzehnfache. <b>Getauscht wird Schwankung gegen
   * Verlaesslichkeit:</b> konstante 24 ms bei einem Budget von 500 ms gegen einen Wert, der
   * zwischen 1,6 ms und einem Abbruch liegt, je nachdem, ob der Mandant gerade Fehler hat.
   */
  private static final String ZEITINDEX = "MessageLastUpdateIDX";

  private static final Logger log = LoggerFactory.getLogger(DashboardRepository.class);

  private final DSLContext glassfishDsl;
  private final MessageStatusClassifier statusClassifier;

  DashboardRepository(
      @Qualifier("glassfishDsl") DSLContext glassfishDsl,
      MessageStatusClassifier statusClassifier) {
    this.glassfishDsl = glassfishDsl;
    this.statusClassifier = statusClassifier;
  }

  /**
   * <b>Block 1 bis 3 in einem Lesevorgang</b>: der Verlauf je Eimer und Rohstatus.
   *
   * <p>Die Kachel <i>Nachrichten</i> ist die Summe ueber alles, was hier zurueckkommt, und die
   * Kachel <i>Fehler</i> samt ihrer Aufschluesselung entsteht aus demselben Rohwert. <b>Kein
   * zweiter Lesevorgang</b> — das ist der Grund, warum diese Methode nach Rohstatus gruppiert und
   * nicht schon nach Einordnung: Die Einordnung ist eine Regel, die sich aendern kann, der Rohwert
   * ist eine Tatsache (Entscheidung E-g).
   *
   * @param mandant der aktive Mandant — Regel M2, und er steht im Statement und nicht dahinter
   *     (Regel M3)
   * @param zeitraum bestimmt, welche der drei Ebenen gelesen wird
   * @param fenster {@code von} einschliesslich, {@code bis} ausschliessend, beide auf einer
   *     Eimergrenze
   */
  public List<Rollupsumme> verlauf(
      MandantContext mandant, Dashboardzeitraum zeitraum, Zeitfenster fenster) {
    return switch (zeitraum) {
      case STUNDEN_48 -> verlaufDerStundenebene(mandant, fenster);
      case TAGE_30 ->
          verlaufDerTagesform(
              MESSAGE_ROLLUP_TAG,
              MESSAGE_ROLLUP_TAG.TAG,
              MESSAGE_ROLLUP_TAG.PROCESS_ID,
              MESSAGE_ROLLUP_TAG.MESSAGE_STATUS,
              MESSAGE_ROLLUP_TAG.ANZAHL,
              mandant,
              fenster);
      case MONATE_12 ->
          verlaufDerTagesform(
              MESSAGE_ROLLUP_MONAT,
              MESSAGE_ROLLUP_MONAT.MONAT,
              MESSAGE_ROLLUP_MONAT.PROCESS_ID,
              MESSAGE_ROLLUP_MONAT.MESSAGE_STATUS,
              MESSAGE_ROLLUP_MONAT.ANZAHL,
              mandant,
              fenster);
    };
  }

  private List<Rollupsumme> verlaufDerStundenebene(MandantContext mandant, Zeitfenster fenster) {
    return glassfishDsl
        .select(
            MESSAGE_ROLLUP.STUNDE, MESSAGE_ROLLUP.MESSAGE_STATUS, DSL.sum(MESSAGE_ROLLUP.ANZAHL))
        .from(MESSAGE_ROLLUP)
        .where(MESSAGE_ROLLUP.STUNDE.ge(fenster.von()))
        .and(MESSAGE_ROLLUP.STUNDE.lt(fenster.bis()))
        .and(mandantenkette(mandant, MESSAGE_ROLLUP.PROCESS_ID))
        .groupBy(MESSAGE_ROLLUP.STUNDE, MESSAGE_ROLLUP.MESSAGE_STATUS)
        .orderBy(MESSAGE_ROLLUP.STUNDE, MESSAGE_ROLLUP.MESSAGE_STATUS)
        .fetch(satz -> new Rollupsumme(satz.value1(), satz.value2(), satz.value3().longValue()));
  }

  /**
   * Tages- und Monatsebene in einer Methode — sie haben dieselbe Form, nur eine andere Tabelle und
   * eine andere Schluesselspalte.
   *
   * <p>Der Eimer wird auf {@code LocalDateTime} gehoben, <b>in Java und nicht in SQL</b>: Ein
   * {@code CAST} um die Schluesselspalte stuende im {@code GROUP BY} und kostete den
   * Bereichszugriff.
   */
  private List<Rollupsumme> verlaufDerTagesform(
      Table<?> tabelle,
      Field<LocalDate> eimer,
      Field<String> prozess,
      Field<String> status,
      Field<Integer> anzahl,
      MandantContext mandant,
      Zeitfenster fenster) {
    return glassfishDsl
        .select(eimer, status, DSL.sum(anzahl))
        .from(tabelle)
        .where(eimer.ge(fenster.von().toLocalDate()))
        .and(eimer.lt(fenster.bis().toLocalDate()))
        .and(mandantenkette(mandant, prozess))
        .groupBy(eimer, status)
        .orderBy(eimer, status)
        .fetch(
            satz ->
                new Rollupsumme(
                    satz.value1().atStartOfDay(), satz.value2(), satz.value3().longValue()));
  }

  /**
   * <b>Block 5</b>: die Verteilung nach Partner oder Richtung, ein Eimer je kuratiertem Wert.
   *
   * <p><b>Beide Sichten laufen ueber dasselbe Statement</b>, nur mit einer anderen Katalogspalte im
   * Ausdruck. Die Eimerbreite des Paares spielt keine Rolle — gruppiert wird ueber das ganze
   * Fenster —, die Fensterbreite schon: Der Bereichszugriff liest dieselben Rollupzeilen wie der
   * Verlauf.
   *
   * <p><b>{@code LEFT JOIN} und nicht {@code JOIN}.</b> {@code WOC} hat keine einzige Katalogzeile;
   * ein innerer Join verloere seine vier Prozesse stillschweigend — und damit ausgerechnet die
   * Zeilen, die als „nicht zugeordnet" erscheinen muessten.
   *
   * <p><b>Der {@code CASE} steht als <i>ein</i> Ausdruck in {@code SELECT}, {@code GROUP BY} und
   * {@code ORDER BY}</b>, und das ist kein Stil, sondern Befund 11 der Vorrunde: MariaDB loest
   * {@code GROUP BY} zuerst gegen <b>Tabellenspalten</b> auf und erst danach gegen
   * Ausdrucksaliasse. Hiesse der Alias {@code partner}, gruppierte die Datenbank still nach {@code
   * c.partner} statt nach dem Ausdruck — bei {@code NEXANS} und {@code SUTTONS} faellt das nicht
   * auf, bei {@code VOTG} zerfiel der Eimer in acht Zeilen. Hier gibt es deshalb <b>gar keinen
   * Alias</b>: dasselbe {@link Field}-Objekt an allen drei Stellen.
   *
   * <p><b>{@code ORDER BY (schluessel IS NULL), summe DESC}</b> — „nicht zugeordnet" ist keine
   * Rangposition und faellt nie in „Übrige". Die Raenge 1…k gehoeren damit lueckenlos den benannten
   * Werten (M91, offener Punkt 39).
   */
  public List<Verteilungssumme> verteilung(
      MandantContext mandant,
      Dashboardzeitraum zeitraum,
      Zeitfenster fenster,
      Verteilungssicht sicht) {
    Ebene ebene = ebene(zeitraum, fenster);
    Field<String> katalogwert =
        sicht == Verteilungssicht.PARTNER ? PROCESS_CATALOG.PARTNER : PROCESS_CATALOG.RICHTUNG;
    Field<String> schluessel = DSL.when(zugeordnet(katalogwert), katalogwert);
    Field<BigDecimal> summe = DSL.sum(ebene.anzahl());

    return glassfishDsl
        .select(schluessel, summe)
        .from(ebene.tabelle())
        .leftJoin(PROCESS_CATALOG)
        .on(PROCESS_CATALOG.PROCESS_ID.eq(ebene.prozess()))
        .where(ebene.bereich())
        .and(mandantenkette(mandant, ebene.prozess()))
        .groupBy(schluessel)
        .orderBy(DSL.field(schluessel.isNull()), summe.desc())
        .fetch(satz -> new Verteilungssumme(satz.value1(), satz.value2().longValue()));
  }

  /**
   * <b>Block 4, erste Zahl</b>: die ueberfaelligen Nachrichten <b>im Fenster</b> — dieselbe Zahl,
   * die der Klick in die Liste liefert (Entscheidung E-h).
   *
   * <p>Gemessen in M90 mit <b>2,275 ms</b> ({@code NEXANS}, 48 Stunden).
   */
  public OptionalLong ueberfaelligImFenster(
      MandantContext mandant, Zeitfenster fenster, LocalDateTime jetzt) {
    return zaehleUeberfaellig(
        mandant,
        jetzt,
        MESSAGE
            .MESSAGELASTUPDATE
            .ge(fenster.von())
            .and(MESSAGE.MESSAGELASTUPDATE.lt(fenster.bis())));
  }

  /**
   * <b>Block 4, zweite Zahl</b>: die ueberfaelligen Nachrichten <b>insgesamt</b>, ohne Zeitfenster.
   *
   * <p><b>Ohne Fenster, und das ist gedeckt (Regel L9).</b> Gefragt ist genau, was
   * <i>ausserhalb</i> des gezeigten Zeitraums haengt — ein Fenster schnitte die Zeilen weg, um die
   * es geht. Es ist ausserdem keine Aggregation ueber einen Bereich, sondern eine Zaehlung ueber
   * die 539 Indexsaetze, auf die {@code MessageStatusIDX} herunterfuehrt.
   *
   * <p><b>Sie ist die billigere der beiden Zahlen</b>: 4,275 ms ohne jedes Zeitfenster gegen 5,127
   * ms mit einem Monatsfenster (M90, Befund 14). Das Zeitfenster verengt nichts, es kostet nur.
   * <b>Wer die zweite Zahl aus Kostengruenden weglassen wollte, haette kein Kostenargument.</b>
   */
  public OptionalLong ueberfaelligInsgesamt(MandantContext mandant, LocalDateTime jetzt) {
    return zaehleUeberfaellig(mandant, jetzt, DSL.noCondition());
  }

  /**
   * <b>Die erste benannte Ausnahme von Leistungsregel L2</b> — die einzige Stelle des Dashboards,
   * die zur Laufzeit ueber {@code Message} aggregiert statt aus {@code message_rollup} zu lesen.
   *
   * <p>Der Grund ist fachlich: <i>Ueberfaellig</i> haengt an einer Frist, die zwischen zwei
   * Rollup-Laeufen ablaeuft. Eine Kachel, die den Ablauf einer Frist erst nach dem naechsten
   * Nachtlauf zeigt, zeigt ihn zu spaet. Begruendet und gemessen in {@code PROJEKTBESCHREIBUNG.md}
   * §8 (E-c) und M90.
   *
   * <p><b>Die Bedingung wird gerufen, nicht nachgebaut</b>: {@code
   * MessageStatusClassifier.ueberfaelligBedingung}. Sie ist das SQL-Gegenstueck zu {@code
   * istUeberfaellig} und steht mit ihm zusammen an einer Stelle; {@code jetzt} zieht der Aufrufer
   * aus der <b>Anwendungsuhr</b> (Regel Z1).
   *
   * <h2>Warum diese eine Abfrage einen Abbruch verkraften muss</h2>
   *
   * <p>Sie ist der einzige Teil der Antwort, der auf der Produktion <b>live</b> liest — dort raeumt
   * {@code max_statement_time} nach zehn Sekunden ab. Der Rest kommt aus unserer eigenen Tabelle.
   * <b>Stirbt sie, darf nicht die ganze Seite sterben</b>; sie liefert dann {@link
   * OptionalLong#empty()}, und die Kachel sagt „nicht ermittelbar".
   *
   * <p><b>Gefangen wird genau eine Ausnahme und nicht pauschal alles</b> — dieselbe Unterscheidung
   * wie in {@code NachrichtenRepository.anDerZeitgrenze}: MariaDB meldet Fehler {@code 1969} mit
   * SQLState {@code 70100}, der Treiber macht daraus eine {@link SQLTimeoutException}, und jOOQ
   * verpackt sie. Ein Syntaxfehler, eine abgerissene Verbindung oder ein fehlendes Recht kommen
   * ebenfalls als {@link DataAccessException} an und bleiben, was sie sind: technische Fehler mit
   * {@code 500}. <b>Ein pauschales {@code catch} machte aus jedem Bruch ein „nicht ermittelbar" —
   * und damit aus einem Befund eine Beruhigung.</b>
   */
  private OptionalLong zaehleUeberfaellig(
      MandantContext mandant, LocalDateTime jetzt, Condition zusatz) {
    try {
      Long gezaehlt =
          glassfishDsl
              .select(DSL.count())
              .from(MESSAGE)
              .where(
                  statusClassifier.ueberfaelligBedingung(
                      MESSAGE.MESSAGESTATUS,
                      MESSAGE.MESSAGELASTUPDATE,
                      MESSAGE.MESSAGETIMEOUT,
                      jetzt))
              .and(zusatz)
              .and(mandantenkette(mandant, MESSAGE.PROCESSID))
              .fetchOne(0, Long.class);
      return OptionalLong.of(gezaehlt == null ? 0L : gezaehlt);
    } catch (DataAccessException fehler) {
      if (fehler.getCause(SQLTimeoutException.class) == null) {
        throw fehler;
      }
      log.warn(
          "Die Live-Abfrage der Kachel Ueberfaellig ist an der Zeitgrenze abgebrochen. Die uebrigen"
              + " Bloecke kommen aus message_rollup und sind davon unberuehrt.",
          fehler);
      return OptionalLong.empty();
    }
  }

  /**
   * <b>Block 6</b>: die auffaelligen Nachrichten des Fensters, neueste zuerst — Fehler <b>und</b>
   * Ueberfaellige in <b>einer</b> Abfrage.
   *
   * <h2>Warum das nicht ueber das Listen-Repository laeuft, obwohl es dieselbe Frage ist</h2>
   *
   * <p>Zwei Gruende, und der zweite waere allein schon entscheidend.
   *
   * <ol>
   *   <li><b>Die Liste kann diese Frage gar nicht beantworten.</b> Dort sind {@code status=FEHLER}
   *       und {@code ueberfaellig=true} ausdruecklich <b>unvereinbar</b> und ergeben {@code 400}:
   *       Ueberfaellig setzt {@code WARTEND} oder {@code LAEUFT} voraus, Fehler ist ein Endstatus.
   *       Ueber die Liste braeuchte dieser Block <b>zwei</b> Aufrufe und ein Zusammenfuehren samt
   *       Neusortierung im Speicher — und der Endpunkt haette zwei Zugriffe statt einem.
   *   <li><b>Fachpakete kennen einander nicht.</b> {@code dashboard} darf nicht aus {@code message}
   *       importieren ({@code PaketstrukturTest.fachpakete_kennen_einander_nicht}); braucht ein
   *       zweites Fachpaket einen Typ, wandert der Typ nach {@code common}. <b>Genau das ist hier
   *       geschehen:</b> Was geteilt gehoert, ist nicht das Repository, sondern die
   *       <i>Bedingung</i> — und die steht in {@code common/MessageStatusClassifier} und wird von
   *       beiden Seiten gerufen. Die Mandantenkette schreibt ohnehin jedes Fachpaket selbst; sie
   *       kann nicht nach {@code common} wandern, weil dort keine {@code jooq.glassfish}-Typen
   *       stehen duerfen.
   * </ol>
   *
   * <p><b>Wiederverwendet ist damit das, was driften koennte</b> — die Fehlerbedingung und die
   * Ueberfaelligkeitsbedingung. Nachgebaut ist nichts.
   *
   * <p><b>Die Fensterverengung faellt hier nicht weg, sie greift ohnehin nicht:</b> Sie ist fuer
   * {@code ueberfaellig} abgeschaltet ({@code Abfragemerkmal.UEBERFAELLIG}), weil der Rollup keine
   * Frist kennt.
   *
   * <h2>Zwei Statements und ein Indexhinweis — gemessen und nicht gewaehlt (M108, 31.08.2026)</h2>
   *
   * <p>Der erste Bau stellte beide Merkmale mit {@code OR} in <b>ein</b> Statement. Er lieferte das
   * Richtige und war falsch gebaut, und der Plan sagt warum: Mit dem {@code OR} steigt MariaDB
   * ueber <b>{@code MessageLastUpdateIDX}</b> ein und liest den <i>ganzen Zeitbereich</i> — 23.126
   * Zeilen bei 48 Stunden, 209.408 bei dreissig Tagen, <b>2,7 Millionen bei zwoelf Monaten</b> —
   * und wertet fuer jede die Mandantenkette aus.
   *
   * <p><b>Der Grund ist die Deckelung.</b> {@code ORDER BY … LIMIT 10} ist nur billig, wenn die
   * zehn Zeilen frueh gefunden werden. Ein Mandant <i>ohne</i> Fehler im Fenster zwingt die
   * Datenbank, den ganzen Bereich zu durchsuchen, bevor sie „nichts" sagen darf — <b>gerade der
   * gute Fall ist der teure</b>.
   *
   * <p><b>Je Merkmal ein Statement genuegte nicht.</b> Auch die getrennte Fehlerabfrage stieg
   * weiterhin ueber den Zeitindex ein — er liefert die Sortierung gratis, und das ist dem
   * Optimierer mehr wert als der kleinere Bereich. Erst {@link #ZEITINDEX} als {@code IGNORE INDEX
   * FOR ORDER BY} dreht den Plan um; dort steht die Messung.
   *
   * <p><b>Beides zusammen macht den Aufwand von der Fensterbreite unabhaengig:</b> Beide
   * Bedingungen sind ueber {@code MessageStatusIDX} sehr selektiv — 822 Fehlerzeilen und 538 offene
   * im <i>gesamten</i> Bestand —, und der Aufwand haengt danach an der Zahl der <b>auffaelligen</b>
   * Zeilen statt an der Breite des Fensters.
   *
   * @param hoechstens wie viele Zeilen zurueckkommen — die Landingpage zeigt eine kurze Liste und
   *     keine Seite
   */
  public List<Auffaelligkeitszeile> zuletztAufgefallen(
      MandantContext mandant, Zeitfenster fenster, LocalDateTime jetzt, int hoechstens) {
    List<Auffaelligkeitszeile> zusammen = new ArrayList<>();
    zusammen.addAll(
        auffaellige(
            mandant, fenster, statusClassifier.fehlerBedingung(MESSAGE.MESSAGESTATUS), hoechstens));
    zusammen.addAll(
        auffaellige(
            mandant,
            fenster,
            statusClassifier.ueberfaelligBedingung(
                MESSAGE.MESSAGESTATUS, MESSAGE.MESSAGELASTUPDATE, MESSAGE.MESSAGETIMEOUT, jetzt),
            hoechstens));
    // Aus zweimal zehn neuesten Zeilen sind die zehn neuesten dieselben wie aus einer
    // gemeinsamen Abfrage: Die beiden Mengen sind disjunkt (Fehler ist Endstatus, ueberfaellig
    // setzt das Gegenteil voraus), und keine Zeile kann durch die Deckelung der anderen Haelfte
    // verlorengehen.
    zusammen.sort(
        Comparator.comparing(Auffaelligkeitszeile::zeitpunkt)
            .thenComparing(Auffaelligkeitszeile::messageId)
            .reversed());
    return List.copyOf(zusammen.subList(0, Math.min(hoechstens, zusammen.size())));
  }

  /**
   * Die eine Haelfte von Block 6 — <b>eine Bedingung, ein Statement, ein Indexbereich</b>.
   *
   * <p>Der zweite Sortierschluessel ist die Kennung: Zwei Nachrichten derselben Sekunde haetten
   * sonst keine feste Reihenfolge, und der Block spraenge zwischen zwei Aufrufen.
   *
   * <p>Zum Indexhinweis siehe {@link #ZEITINDEX} — dort steht, was er kostet und was er spart.
   */
  private List<Auffaelligkeitszeile> auffaellige(
      MandantContext mandant, Zeitfenster fenster, Condition merkmal, int hoechstens) {
    return glassfishDsl
        .select(
            MESSAGE.MESSAGEID,
            MESSAGE.MESSAGELASTUPDATE,
            MESSAGE.MESSAGESTATUS,
            MESSAGE.PROCESSID,
            SOS.SOSNAME)
        .from(MESSAGE.ignoreIndexForOrderBy(ZEITINDEX))
        .leftJoin(SOS)
        .on(SOS.SOSID.eq(MESSAGE.SOSID))
        .where(merkmal)
        .and(MESSAGE.MESSAGELASTUPDATE.ge(fenster.von()))
        .and(MESSAGE.MESSAGELASTUPDATE.lt(fenster.bis()))
        .and(mandantenkette(mandant, MESSAGE.PROCESSID))
        .orderBy(MESSAGE.MESSAGELASTUPDATE.desc(), MESSAGE.MESSAGEID.desc())
        .limit(hoechstens)
        .fetch(
            satz ->
                new Auffaelligkeitszeile(
                    satz.value1(), satz.value2(), satz.value3(), satz.value4(), satz.value5()));
  }

  /**
   * <b>Entscheidung E-i, als ein Ausdruck.</b> Zugeordnet ist ein Prozess nur, wenn alle drei
   * Bedingungen halten; faellt eine, ist der Wert „nicht zugeordnet".
   *
   * <ol>
   *   <li>Es gibt eine Katalogzeile ({@code LEFT JOIN} traf).
   *   <li>Sie ist {@code GEPFLEGT} — ein offener Regelvorschlag ist eine Vermutung und keine
   *       Zuordnung.
   *   <li>Das Feld ist gefuellt — „gepflegt mit leerem Partner" heisst <i>hingesehen, es gibt
   *       keinen</i> (E4) und faellt fachlich mit „nicht zugeordnet" zusammen.
   * </ol>
   *
   * <p>Der Riegel unter Punkt 2 steht auch in der Richtungssicht. <b>Heute ist er dort
   * folgenlos</b> — nach der Kuratierung tragen alle Zeilen mit Richtung {@code GEPFLEGT} —, aber
   * die Regel ist E-i und nicht der Zufall dieses Katalogstands.
   */
  private static Condition zugeordnet(Field<String> katalogwert) {
    return PROCESS_CATALOG
        .PROCESS_ID
        .isNotNull()
        .and(PROCESS_CATALOG.PFLEGESTATUS.eq(Pflegestatus.GEPFLEGT.name()))
        .and(katalogwert.isNotNull())
        .and(katalogwert.ne(""));
  }

  /**
   * Die Spalten und der Bereich der Ebene, die zu einem Zeitraumpaar gehoert.
   *
   * <p>Sie steht hier, damit die Abfragen, die den Eimer <b>nicht</b> in der Ausgabe brauchen —
   * Verteilung und Belegungsprobe —, nicht dreimal geschrieben werden muessen. Der Verlauf braucht
   * ihn und hat deshalb zwei eigene Fassungen.
   */
  private record Ebene(
      Table<?> tabelle,
      Field<?> eimer,
      Field<String> prozess,
      Field<Integer> anzahl,
      Condition bereich) {}

  private static Ebene ebene(Dashboardzeitraum zeitraum, Zeitfenster fenster) {
    return switch (zeitraum) {
      case STUNDEN_48 ->
          new Ebene(
              MESSAGE_ROLLUP,
              MESSAGE_ROLLUP.STUNDE,
              MESSAGE_ROLLUP.PROCESS_ID,
              MESSAGE_ROLLUP.ANZAHL,
              MESSAGE_ROLLUP.STUNDE.ge(fenster.von()).and(MESSAGE_ROLLUP.STUNDE.lt(fenster.bis())));
      case TAGE_30 ->
          new Ebene(
              MESSAGE_ROLLUP_TAG,
              MESSAGE_ROLLUP_TAG.TAG,
              MESSAGE_ROLLUP_TAG.PROCESS_ID,
              MESSAGE_ROLLUP_TAG.ANZAHL,
              MESSAGE_ROLLUP_TAG
                  .TAG
                  .ge(fenster.von().toLocalDate())
                  .and(MESSAGE_ROLLUP_TAG.TAG.lt(fenster.bis().toLocalDate())));
      case MONATE_12 ->
          new Ebene(
              MESSAGE_ROLLUP_MONAT,
              MESSAGE_ROLLUP_MONAT.MONAT,
              MESSAGE_ROLLUP_MONAT.PROCESS_ID,
              MESSAGE_ROLLUP_MONAT.ANZAHL,
              MESSAGE_ROLLUP_MONAT
                  .MONAT
                  .ge(fenster.von().toLocalDate())
                  .and(MESSAGE_ROLLUP_MONAT.MONAT.lt(fenster.bis().toLocalDate())));
    };
  }

  /**
   * <b>Die Belegungsprobe</b> — Grundlage der Wahl des Standardfensters (D.3).
   *
   * <p>Zwei Zahlen aus <b>einem</b> Statement: wie viele Eimer des Paares ueberhaupt belegt sind,
   * und wie gross der groesste ist. Innen wird je Eimer summiert, aussen gezaehlt und das Maximum
   * genommen.
   *
   * <p><b>Sie liest dieselbe Tabelle wie der Verlauf und denselben Bereich</b> — der teure Teil ist
   * der Bereichszugriff, und der ist derselbe. Sie ist deshalb keine zweite Art von Frage, sondern
   * dieselbe Frage mit einer anderen Verdichtung.
   *
   * @return leere Eimer und leerer Bestand ergeben {@code new Belegung(0, 0)} — ein Mandant ohne
   *     eine einzige Rollupzeile im Fenster erscheint in der Gruppierung gar nicht
   */
  public Belegung belegung(
      MandantContext mandant, Dashboardzeitraum zeitraum, Zeitfenster fenster) {
    Ebene ebene = ebene(zeitraum, fenster);
    Field<BigDecimal> summe = DSL.sum(ebene.anzahl());
    Table<?> jeEimer =
        glassfishDsl
            .select(ebene.eimer(), summe.as("nachrichten"))
            .from(ebene.tabelle())
            .where(ebene.bereich())
            .and(mandantenkette(mandant, ebene.prozess()))
            .groupBy(ebene.eimer())
            .asTable("belegte_eimer");

    Record2<Integer, BigDecimal> satz =
        glassfishDsl
            .select(DSL.count(), DSL.max(jeEimer.field("nachrichten", BigDecimal.class)))
            .from(jeEimer)
            .fetchOne();
    if (satz == null || satz.value2() == null) {
      return new Belegung(0, 0);
    }
    return new Belegung(satz.value1(), satz.value2().longValue());
  }

  /**
   * <b>Block 7</b>: der letzte abgeschlossene, fehlerfreie Rollup-Lauf.
   *
   * <p><b>Dieselbe Bedingung wie der Wasserstand</b> ({@code
   * RollupSchreibRepository.wasserstand()}): {@code beendet_am IS NOT NULL AND fehler IS NULL}. Ein
   * abgebrochener Lauf hat nichts fortgeschrieben, und ein abgeschlossener mit Fehler ist nicht
   * verlaesslich gerechnet — beide taugen nicht als Aktualitaetsangabe. <b>Waere die Bedingung hier
   * eine andere, zeigte die Seite einen Stand an, den der Job selbst nicht anerkennt.</b>
   *
   * <p><b>Paketprivat, und das ist kein Versehen.</b> {@code rollup_lauf} traegt keinen Mandanten;
   * ein {@code MandantContext} als erster Parameter waere ein Schein-Kontext — ein Parameter, der
   * entgegengenommen und nicht verwendet wird, sieht von aussen wie Mandantentrennung aus. Regel M2
   * greift nur fuer oeffentliche Methoden, und paketprivat ist hier der saubere Ausweg statt einer
   * dritten benannten Ausnahme.
   *
   * @return leer, wenn es noch keinen abgeschlossenen, fehlerfreien Lauf gibt
   */
  Optional<Standzeile> letzterLauf() {
    return glassfishDsl
        .select(ROLLUP_LAUF.ART, ROLLUP_LAUF.BEENDET_AM)
        .from(ROLLUP_LAUF)
        .where(ROLLUP_LAUF.BEENDET_AM.isNotNull())
        .and(ROLLUP_LAUF.FEHLER.isNull())
        .orderBy(ROLLUP_LAUF.BEENDET_AM.desc())
        .limit(1)
        .fetchOptional(satz -> new Standzeile(satz.value1(), satz.value2()));
  }

  /**
   * <b>Regel M3, als Bestandteil des Statements und nicht als nachgelagerte Pruefung.</b>
   *
   * <p>{@code EXISTS} und nicht {@code JOIN}: {@code ProjectMandant} ist n:m, ein Join
   * vervielfachte die Rollupzeilen und damit die Summe. Seit {@code V11} ist die {@code
   * EXISTS}-Fassung auf der Stundenebene ausserdem die schnellere — Index und Abfragefassung sind
   * <b>eine</b> Entscheidung und nicht zwei (M104).
   *
   * @param prozessSpalte die {@code process_id} der jeweiligen Rollup-Ebene
   */
  private static Condition mandantenkette(MandantContext mandant, Field<String> prozessSpalte) {
    return DSL.exists(
        DSL.selectOne()
            .from(KETTE_PROCESS)
            .join(PROJECTMANDANT)
            .on(PROJECTMANDANT.PROJECTID.eq(KETTE_PROCESS.PROJECTID))
            .where(KETTE_PROCESS.PROCESSID.eq(prozessSpalte))
            .and(PROJECTMANDANT.MANDANTID.eq(mandant.mandantId())));
  }
}
