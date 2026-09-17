package de.kraftwerkone.overlord.monitor.common;

import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.MESSAGE;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.PROCESS;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.PROJECTMANDANT;
import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.MESSAGE_ROLLUP;

import de.kraftwerkone.overlord.monitor.jooq.glassfish.tables.Process;
import java.time.LocalDateTime;
import java.util.List;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

/**
 * <b>Die beiden Lesungen des Live-Bereichs</b> — was der Rollup ueber ihn sagt und was die Quelle
 * ueber ihn sagt —, beide mit Mandantenkette im Statement ({@code docs/live-rest.md} §7).
 *
 * <h2>Die dritte benannte Ausnahme von Regel L2</h2>
 *
 * <p>{@link #ausDerQuelle} zaehlt live ueber {@code GlassfishDB.Message}. Sie ist in {@code
 * docs/PROJEKTBESCHREIBUNG.md} §8, Regel 2, als dritte benannte Ausnahme eingetragen und
 * begruendet: <b>Die Zahl haengt am Takt des Laufs</b> — nicht an einer Frist (wie
 * <i>Ueberfaellig</i>) und nicht an einem fluechtigen Status (wie <i>Laeuft</i> und
 * <i>Wartend</i>). Was zwischen dem letzten Delta-Lauf und jetzt passiert ist, steht in keiner
 * Rolluptabelle, und die Liste daneben zeigt es bereits. Der Bereich ist auf hoechstens vier
 * Stundeneimer begrenzt ({@link LiveRest#OBERGRENZE_STUNDEN}); die Messung steht in {@code
 * docs/live-rest.md} §8 (M185).
 *
 * <h2>Zwei Statements, kein {@code UNION ALL}</h2>
 *
 * <p>Am Plan entschieden ({@code docs/live-rest.md} §8): Beide Teile steigen ueber verschiedene
 * Indizes in verschiedene Tabellen ein, und in einer Vereinigung materialisierte der Optimierer die
 * Ableitung, um die Kette einmal aussen zu pruefen — fuer den kleinen Rollup-Teil ein Umweg, fuer
 * den Message-Teil kein Gewinn. Getrennt hat jeder Teil seinen eigenen Plan, und jeder ist einzeln
 * im Statementstest benannt.
 *
 * <h2>Die Mandantenkette ist ein {@code EXISTS} und kein Join</h2>
 *
 * <p>In beiden Teilen wird summiert beziehungsweise gezaehlt. {@code ProjectMandant} ist n:m; ein
 * Join vervielfachte Zeilen und damit die Zahl, und der Fehler waere still ({@code
 * docs/process-view.md} §6). Um {@code MessageLastUpdate} steht keine Funktion — der Bereich ist
 * ein Indexbereich ueber {@code MessageLastUpdateIDX}, wie beim Rollup-Job (M88).
 *
 * <p>Gelesen wird ueber {@code glassfishDsl}, den Lese-Pool — beide Schemata in einer Verbindung,
 * nur lesend (Regel S1). Die Stundenbildung ist {@link Stundeneimer#ausdruck} — dieselbe wie im
 * Rollup-Job, gerufen und nicht nachgebaut.
 */
@Repository
public class LiveRestRepository {

  /** Eigener Alias fuer die Kette — Message wird in derselben Abfrage ohne Alias gelesen. */
  private static final Process KETTE_PROCESS = PROCESS.as("live_process");

  /** Die Stundenbildung des Rollups, aus {@code common} (E-180). */
  private static final Field<String> STUNDE = Stundeneimer.ausdruck(MESSAGE.MESSAGELASTUPDATE);

  private final DSLContext glassfishDsl;

  public LiveRestRepository(@Qualifier("glassfishDsl") DSLContext glassfishDsl) {
    this.glassfishDsl = glassfishDsl;
  }

  /**
   * Was der Rollup ueber den Live-Bereich sagt: seine Zeilen je {@code (stunde, process_id,
   * message_status)} — die Zeilen, die ein Verbraucher <b>abzieht</b>, weil {@link #ausDerQuelle}
   * denselben Bereich vollstaendig zaehlt.
   *
   * <p>Ohne {@code GROUP BY}: Der Primaerschluessel macht jede Zeile eindeutig. Ohne {@code ORDER
   * BY}: Die Verrechnung braucht keine Reihenfolge.
   *
   * @param mandant Regel M2 — erster Pflichtparameter, im Statement und nicht dahinter (M3)
   * @param von G, einschliessend
   * @param bis der Anfang der Stunde nach jetzt, ausschliessend
   */
  public List<LiveRestZeile> ausDemRollup(
      MandantContext mandant, LocalDateTime von, LocalDateTime bis) {
    return glassfishDsl
        .select(
            MESSAGE_ROLLUP.STUNDE,
            MESSAGE_ROLLUP.PROCESS_ID,
            MESSAGE_ROLLUP.MESSAGE_STATUS,
            MESSAGE_ROLLUP.ANZAHL)
        .from(MESSAGE_ROLLUP)
        .where(MESSAGE_ROLLUP.STUNDE.ge(von))
        .and(MESSAGE_ROLLUP.STUNDE.lt(bis))
        .and(mandantenkette(mandant, MESSAGE_ROLLUP.PROCESS_ID))
        .fetch(
            satz -> new LiveRestZeile(satz.value1(), satz.value2(), satz.value3(), satz.value4()));
  }

  /**
   * Was die Quelle ueber den Live-Bereich sagt: dieselbe Aggregation wie der Rollup-Job ({@code
   * docs/rollup.md} §7), <b>mit</b> Mandantenkette — je Stundeneimer, Prozess und Rohstatus eine
   * Anzahl.
   *
   * <p>Der Bereich liegt ueber {@code MessageLastUpdate}, wie beim Rollup: Das ist die Spalte, nach
   * der {@code message_rollup} gruppiert ist, und nur so heben sich minus und plus je Eimer auf.
   *
   * @param mandant Regel M2
   * @param von G, einschliessend
   * @param bis der Anfang der Stunde nach jetzt, ausschliessend
   */
  public List<LiveRestZeile> ausDerQuelle(
      MandantContext mandant, LocalDateTime von, LocalDateTime bis) {
    return glassfishDsl
        .select(STUNDE, MESSAGE.PROCESSID, MESSAGE.MESSAGESTATUS, DSL.count())
        .from(MESSAGE)
        .where(MESSAGE.MESSAGELASTUPDATE.ge(von))
        .and(MESSAGE.MESSAGELASTUPDATE.lt(bis))
        .and(mandantenkette(mandant, MESSAGE.PROCESSID))
        .groupBy(STUNDE, MESSAGE.PROCESSID, MESSAGE.MESSAGESTATUS)
        .fetch(
            satz ->
                new LiveRestZeile(
                    Stundeneimer.lies(satz.value1()), satz.value2(), satz.value3(), satz.value4()));
  }

  /**
   * <b>Regel M3, als Bestandteil des Statements und nicht als nachgelagerte Pruefung.</b> Dieselbe
   * Form wie in {@code ProzessbaumRepository} und {@code DashboardRepository}.
   *
   * @param prozessSpalte {@code process_id} des Rollups oder {@code ProcessID} der Quelle
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
