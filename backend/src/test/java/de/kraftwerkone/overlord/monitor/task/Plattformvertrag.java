package de.kraftwerkone.overlord.monitor.task;

import java.util.List;

/**
 * Die vier Statements der EDI-Plattform, <b>woertlich</b> wie im Auftrag an die
 * Plattform-Entwicklung vom 16.10.2026 und in {@code docs/task-schnittstelle.md} §3. {@code
 * PlattformvertragTest} haelt die Gleichheit mit der Datei, {@code AuftraegeDbIT} fuehrt sie aus.
 * Aendert sich eins, ist das eine Aenderung des Vertrags und gehoert in die Rueckmeldung.
 *
 * <p>Ohne das abschliessende Semikolon der Datei; Bindeparameter als {@code ?} in der Reihenfolge
 * des Textes.
 */
final class Plattformvertrag {

  static final String OFFENE_LESEN =
      """
      SELECT task_id, mandant_id, task_typ, aktion, parameter
      FROM overlord_monitor.task
      WHERE status = 'OFFEN'
      ORDER BY eingestellt_am, task_id""";

  /** Bindeparameter: {@code task_id}. */
  static final String ABHOLEN =
      """
      UPDATE overlord_monitor.task
      SET status = 'IN_AUSFUEHRUNG', abgeholt_am = UTC_TIMESTAMP(3)
      WHERE task_id = ? AND status = 'OFFEN'""";

  /** Bindeparameter: {@code rueckgabe}, {@code task_id}. */
  static final String ERLEDIGT_MELDEN =
      """
      UPDATE overlord_monitor.task
      SET status = 'ERLEDIGT', rueckgabe = ?, ausgefuehrt_am = UTC_TIMESTAMP(3)
      WHERE task_id = ? AND status = 'IN_AUSFUEHRUNG'""";

  /** Bindeparameter: {@code fehlercode}, {@code fehlertext}, {@code task_id}. */
  static final String FEHLGESCHLAGEN_MELDEN =
      """
      UPDATE overlord_monitor.task
      SET status = 'FEHLGESCHLAGEN', fehlercode = ?, fehlertext = ?,
          ausgefuehrt_am = UTC_TIMESTAMP(3)
      WHERE task_id = ? AND status = 'IN_AUSFUEHRUNG'""";

  static final List<String> ALLE =
      List.of(OFFENE_LESEN, ABHOLEN, ERLEDIGT_MELDEN, FEHLGESCHLAGEN_MELDEN);

  private Plattformvertrag() {}
}
