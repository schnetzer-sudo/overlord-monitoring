package de.kraftwerkone.overlord.monitor.common;

import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.MESSAGE;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.PROCESS;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.PROJECTMANDANT;

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
 * <b>Die Live-Lesung der Fehler</b> — je Stundeneimer, Prozess und Rohstatus, wie viele Nachrichten
 * des Mandanten im Fenster <b>jetzt</b> die Fehlerbedingung erfuellen ({@code docs/fehler-live.md}
 * §4).
 *
 * <h2>Die vierte benannte Ausnahme von Regel L2</h2>
 *
 * <p>{@link #ausDerQuelle} zaehlt live ueber {@code GlassfishDB.Message}. Eingetragen und
 * begruendet in {@code docs/PROJEKTBESCHREIBUNG.md} §8, Regel 2: <b>Ein Fehlerstatus ist nicht
 * endgueltig</b> — eine Nachverarbeitung setzt die Nachricht auf {@code RUNNING} und bucht sie in
 * die aktuelle Stunde um —, und der Delta-Lauf entfernt einen solchen Abgang aus einem alten Eimer
 * nicht; bis zum Volllauf zaehlte die Uebersicht ihn weiter als Fehler (Entscheidung E-208, Weg B).
 * Die Messung steht in {@code docs/fehler-live.md} §8 (M188).
 *
 * <h2>Warum das billig ist, obwohl das Fenster ein Jahr sein kann</h2>
 *
 * <p>Die Bedingung ist {@link MessageStatusClassifier#fehlerBedingung} — <b>gerufen, nicht
 * nachgebaut</b> —, und sie ergibt zwei Indexbereiche auf {@code MessageStatusIDX}. Der Aufwand
 * haengt damit an der Zahl der <b>Fehlerzeilen im Bestand</b> und nicht an der Breite des Fensters,
 * wie bei Block 6 des Dashboards (M108, M146). <b>Kein Indexhinweis</b>: Ohne {@code ORDER BY} und
 * ohne Deckelung gibt es keine Sortierung, fuer die der Zeitindex verlockend waere; ob der
 * Optimierer das so sieht, belegt der {@code EXPLAIN} (M188, {@code DashboardPlanDbIT}) und nicht
 * diese Zeile.
 *
 * <h2>Was im Statement steht, und was nicht</h2>
 *
 * <ul>
 *   <li>Die Stundenbildung ist {@link Stundeneimer#ausdruck} — dieselbe wie im Rollup-Job, damit
 *       eine Zeile genau in den Eimer faellt, in dem der Rollup sie fuehrt.
 *   <li>{@code GROUP BY} ueber den <b>vollen Ausdruck</b> und nicht ueber einen Alias (Befund 11).
 *   <li>Um {@code MessageLastUpdate} steht in der Bedingung <b>keine Funktion</b>; der Bereich ist
 *       halboffen wie die Eimer.
 *   <li>Die Mandantenkette ist ein {@code EXISTS} und kein Join, wie in {@link LiveRestRepository}:
 *       {@code ProjectMandant} ist n:m, ein Join vervielfachte die Zahl.
 * </ul>
 *
 * <p>Gelesen wird ueber {@code glassfishDsl}, den Lese-Pool, nur lesend (Regel S1).
 */
@Repository
public class FehlerLiveRepository {

  /** Eigener Alias fuer die Kette — Message wird in derselben Abfrage ohne Alias gelesen. */
  private static final Process KETTE_PROCESS = PROCESS.as("fehler_process");

  /** Die Stundenbildung des Rollups, aus {@code common} (E-180). */
  private static final Field<String> STUNDE = Stundeneimer.ausdruck(MESSAGE.MESSAGELASTUPDATE);

  private final DSLContext glassfishDsl;
  private final MessageStatusClassifier statusClassifier;

  public FehlerLiveRepository(
      @Qualifier("glassfishDsl") DSLContext glassfishDsl,
      MessageStatusClassifier statusClassifier) {
    this.glassfishDsl = glassfishDsl;
    this.statusClassifier = statusClassifier;
  }

  /**
   * Die Fehler im Fenster, wie die Quelle sie <b>jetzt</b> fuehrt: je {@code (stunde, ProcessID,
   * MessageStatus)} eine Anzahl, ohne Sortierung — die Verbraucher summieren in ihre eigenen Eimer.
   *
   * @param mandant Regel M2 — erster Pflichtparameter, im Statement und nicht dahinter (M3)
   * @param von einschliessend, auf einer vollen Stunde
   * @param bis ausschliessend, auf einer vollen Stunde
   */
  public List<FehlerLiveZeile> ausDerQuelle(
      MandantContext mandant, LocalDateTime von, LocalDateTime bis) {
    return glassfishDsl
        .select(STUNDE, MESSAGE.PROCESSID, MESSAGE.MESSAGESTATUS, DSL.count())
        .from(MESSAGE)
        .where(statusClassifier.fehlerBedingung(MESSAGE.MESSAGESTATUS))
        .and(MESSAGE.MESSAGELASTUPDATE.ge(von))
        .and(MESSAGE.MESSAGELASTUPDATE.lt(bis))
        .and(mandantenkette(mandant, MESSAGE.PROCESSID))
        .groupBy(STUNDE, MESSAGE.PROCESSID, MESSAGE.MESSAGESTATUS)
        .fetch(
            satz ->
                new FehlerLiveZeile(
                    Stundeneimer.lies(satz.value1()), satz.value2(), satz.value3(), satz.value4()));
  }

  /**
   * <b>Regel M3, als Bestandteil des Statements und nicht als nachgelagerte Pruefung.</b> Dieselbe
   * Form wie in {@link LiveRestRepository} — jede lesende Klasse schreibt ihre Kette selbst, mit
   * eigenem Alias.
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
