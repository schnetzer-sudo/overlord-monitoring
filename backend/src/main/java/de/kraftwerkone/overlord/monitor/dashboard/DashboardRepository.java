package de.kraftwerkone.overlord.monitor.dashboard;

import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.PROCESS;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.PROJECTMANDANT;
import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.MESSAGE_ROLLUP;
import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.MESSAGE_ROLLUP_MONAT;
import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.MESSAGE_ROLLUP_TAG;

import de.kraftwerkone.overlord.monitor.common.Zeitfenster;
import de.kraftwerkone.overlord.monitor.jooq.glassfish.tables.Process;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.time.LocalDate;
import java.util.List;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;
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

  private final DSLContext glassfishDsl;

  DashboardRepository(@Qualifier("glassfishDsl") DSLContext glassfishDsl) {
    this.glassfishDsl = glassfishDsl;
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
