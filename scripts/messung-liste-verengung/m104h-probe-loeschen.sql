-- Messung M104 h - Die Probetabelle des zweiten Durchgangs loeschen, mit Nachweis
-- Auftrag:  "Fensterverengung fuer die Nachrichtenliste", Stand 27.08.2026, Nachtrag zu Punkt 73
-- Ergebnis: docs/messungen-liste-verengung.md
--
-- ⚠ Fortsetzung der benannten Ausnahme aus m104a: Es wird EINE Probetabelle in
--   'overlord_monitor' angelegt, befuellt, gemessen und wieder geloescht - Bauform M89.
--   Die echte Tabelle message_rollup wird nicht angefasst; auf GlassfishDB nur gelesen. (S1)
--
-- Benutzer: monitor_write

SELECT '=== 00 Vorher ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;
SELECT COUNT(*) AS probe_zeilen FROM overlord_monitor.message_rollup_probe_idx;

SELECT '=== 01 Loeschen ===' AS marke;
DROP TABLE overlord_monitor.message_rollup_probe_idx;

SELECT '=== 02 Nachweis ===' AS marke;
SELECT COUNT(*) AS eintraege_fuer_die_probetabelle FROM information_schema.TABLES
 WHERE TABLE_SCHEMA='overlord_monitor' AND TABLE_NAME='message_rollup_probe_idx';
SELECT TABLE_NAME FROM information_schema.TABLES
 WHERE TABLE_SCHEMA='overlord_monitor' ORDER BY TABLE_NAME;

SELECT '=== 03 Die echte Tabelle ist unberuehrt ===' AS marke;
SELECT COUNT(*) AS rollup_zeilen, SUM(anzahl) AS rollup_summe FROM overlord_monitor.message_rollup;
SHOW INDEX FROM overlord_monitor.message_rollup;
SELECT TABLE_NAME, DATA_LENGTH, INDEX_LENGTH FROM information_schema.TABLES
 WHERE TABLE_SCHEMA='overlord_monitor' AND TABLE_NAME IN ('message_rollup','message_rollup_tag');

SELECT '=== 04 GlassfishDB ist unberuehrt ===' AS marke;
SELECT COUNT(*) AS message_zeilen, MAX(MessageLastUpdate) AS datenstand FROM GlassfishDB.Message;
SELECT @@global.read_only AS read_only_am_ende, NOW() AS serverzeit_ende;
