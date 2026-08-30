-- Messung M105 c - Den Index auf der Stunden-Probetabelle anlegen
-- Auftrag:  "Fensterverengung bauen - mit Index auf message_rollup", Stand 27.08.2026, Teil A
-- Ergebnis: docs/rollup.md §9c
--
-- ⚠ DIE BENANNTE AUSNAHME VOM SATZ "ES WIRD NUR GELESEN"
--
--   Diese Sitzung schreibt in 'overlord_monitor' - und nur dort. Sie legt ZWEI Probetabellen an
--   (Stunden- und Tagesebene), befuellt sie, misst gegen sie und loescht sie wieder. Die Loeschung
--   ist in m105e nachgewiesen. Bauform M89 / M104, aus demselben Grund und mit derselben
--   Rechenschaft. Die echten Tabellen message_rollup und message_rollup_tag werden nur GELESEN.
--   Auf GlassfishDB wird ueberhaupt nicht zugegriffen. (Regel S1)
--
-- Zwischen den beiden Messungen. Beide laufen damit gegen DIESELBE Tabelle.
--
-- Benutzer: monitor_write
-- Z1: alle Zeitpunkte als Literal, nie NOW().

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, VERSION() AS version, NOW() AS serverzeit;

SET max_statement_time = 60;
SELECT TABLE_ROWS, DATA_LENGTH, INDEX_LENGTH FROM information_schema.TABLES
 WHERE TABLE_SCHEMA='overlord_monitor' AND TABLE_NAME='message_rollup_probe_lauf';
SELECT NOW(3) AS begonnen;
ALTER TABLE overlord_monitor.message_rollup_probe_lauf ADD INDEX message_rollup_prozess_idx (process_id, stunde);
SELECT NOW(3) AS beendet;
SHOW INDEX FROM overlord_monitor.message_rollup_probe_lauf;
SELECT TABLE_ROWS, DATA_LENGTH, INDEX_LENGTH FROM information_schema.TABLES
 WHERE TABLE_SCHEMA='overlord_monitor' AND TABLE_NAME='message_rollup_probe_lauf';
SELECT @@global.read_only AS read_only_am_ende, NOW() AS serverzeit_ende;
