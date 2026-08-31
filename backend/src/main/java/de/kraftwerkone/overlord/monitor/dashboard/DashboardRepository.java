package de.kraftwerkone.overlord.monitor.dashboard;

import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.MESSAGE;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.PROCESS;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.PROJECTMANDANT;
import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.MESSAGE_ROLLUP;
import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.MESSAGE_ROLLUP_MONAT;
import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.MESSAGE_ROLLUP_TAG;
import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.PROCESS_CATALOG;

import de.kraftwerkone.overlord.monitor.common.MessageStatusClassifier;
import de.kraftwerkone.overlord.monitor.common.Pflegestatus;
import de.kraftwerkone.overlord.monitor.common.Zeitfenster;
import de.kraftwerkone.overlord.monitor.jooq.glassfish.tables.Process;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.math.BigDecimal;
import java.sql.SQLTimeoutException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.OptionalLong;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
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
      return OptionalLong.of(
          glassfishDsl.fetchCount(
              MESSAGE,
              statusClassifier
                  .ueberfaelligBedingung(
                      MESSAGE.MESSAGESTATUS,
                      MESSAGE.MESSAGELASTUPDATE,
                      MESSAGE.MESSAGETIMEOUT,
                      jetzt)
                  .and(zusatz)
                  .and(mandantenkette(mandant, MESSAGE.PROCESSID))));
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
      Table<?> tabelle, Field<String> prozess, Field<Integer> anzahl, Condition bereich) {}

  private static Ebene ebene(Dashboardzeitraum zeitraum, Zeitfenster fenster) {
    return switch (zeitraum) {
      case STUNDEN_48 ->
          new Ebene(
              MESSAGE_ROLLUP,
              MESSAGE_ROLLUP.PROCESS_ID,
              MESSAGE_ROLLUP.ANZAHL,
              MESSAGE_ROLLUP.STUNDE.ge(fenster.von()).and(MESSAGE_ROLLUP.STUNDE.lt(fenster.bis())));
      case TAGE_30 ->
          new Ebene(
              MESSAGE_ROLLUP_TAG,
              MESSAGE_ROLLUP_TAG.PROCESS_ID,
              MESSAGE_ROLLUP_TAG.ANZAHL,
              MESSAGE_ROLLUP_TAG
                  .TAG
                  .ge(fenster.von().toLocalDate())
                  .and(MESSAGE_ROLLUP_TAG.TAG.lt(fenster.bis().toLocalDate())));
      case MONATE_12 ->
          new Ebene(
              MESSAGE_ROLLUP_MONAT,
              MESSAGE_ROLLUP_MONAT.PROCESS_ID,
              MESSAGE_ROLLUP_MONAT.ANZAHL,
              MESSAGE_ROLLUP_MONAT
                  .MONAT
                  .ge(fenster.von().toLocalDate())
                  .and(MESSAGE_ROLLUP_MONAT.MONAT.lt(fenster.bis().toLocalDate())));
    };
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
