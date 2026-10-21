-- ═══════════════════════════════════════════════════════════════════════════════
-- Assistent 1.1.0: Freischaltung von WERK_ABLADESTELLE für SYSTEM zurücknehmen
-- ═══════════════════════════════════════════════════════════════════════════════
--
-- Die Rücknahme zu scripts/assistent-freischaltung-system.sql (docs/tasks.md §3.9).
-- Danach ist der Assistent für SYSTEM beim nächsten Zugriff nicht mehr verfügbar:
-- kein Symbol, Gespräche und Anlegen antworten 404. Ohne Neustart.
--
-- IDEMPOTENT: Ein zweiter Lauf ändert nichts.
--
-- Was es NICHT tut: Aufträge löschen, den Typ ändern, andere Mandanten anfassen.
-- Offene Aufträge von SYSTEM bleiben in task, und die Plattform holt sie weiter ab;
-- wer das nicht will, löscht sie vorher unter „Ihre Aufträge“ (docs/tasks.md §6.6).
-- „Ihre Aufträge“ zeigt sie auch nach der Rücknahme.
--
-- ── Ausführen ────────────────────────────────────────────────────────────────
--
-- Wie das Freischalten: MySQL Workbench, Schreibbenutzer (monitor_write),
-- Vorgabeschema = das eigene Schema, Query > Execute (All or Selection) to Text.
-- Die Vorgabe „bei Fehler anhalten“ nicht ändern.
--
-- Erwartet: drei Ergebnismengen — „vorher“, „nachher“, „fertig“. In „nachher“
-- steht system 0.

SET SESSION max_statement_time = 10;

-- Wache: Das Vorgabeschema ist das eigene Schema.
SET SESSION sql_mode = IF(
  (SELECT COUNT(*) FROM information_schema.TABLES
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'task_type_mandant') = 1,
  @@session.sql_mode,
  'BITTE_DAS_EIGENE_SCHEMA_ALS_VORGABE_WAEHLEN');

SELECT 'vorher' AS stand,
       COUNT(tm.mandant_id) AS freischaltungen,
       COALESCE(SUM(tm.mandant_id = 'SYSTEM'), 0) AS system,
       COALESCE(SUM(tm.mandant_id <> 'SYSTEM'), 0) AS andere
FROM task_type_mandant tm
WHERE tm.task_typ = 'WERK_ABLADESTELLE';

DELETE FROM task_type_mandant
WHERE task_typ = 'WERK_ABLADESTELLE' AND mandant_id = 'SYSTEM';

SELECT 'nachher' AS stand,
       COUNT(tm.mandant_id) AS freischaltungen,
       COALESCE(SUM(tm.mandant_id = 'SYSTEM'), 0) AS system,
       COALESCE(SUM(tm.mandant_id <> 'SYSTEM'), 0) AS andere
FROM task_type_mandant tm
WHERE tm.task_typ = 'WERK_ABLADESTELLE';

SELECT 'fertig' AS stand;
