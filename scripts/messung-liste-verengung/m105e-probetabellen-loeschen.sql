-- Messung M105 e - Die beiden Probetabellen loeschen, mit Nachweis
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
-- Die Runde hinterlaesst nichts.
--
-- Benutzer: monitor_write
-- Z1: alle Zeitpunkte als Literal, nie NOW().

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, VERSION() AS version, NOW() AS serverzeit;

SELECT COUNT(*) AS probe_stunden FROM overlord_monitor.message_rollup_probe_lauf;
SELECT COUNT(*) AS probe_tage FROM overlord_monitor.message_rollup_tag_probe_lauf;

SELECT '=== Loeschen ===' AS marke;
DROP TABLE overlord_monitor.message_rollup_probe_lauf;
DROP TABLE overlord_monitor.message_rollup_tag_probe_lauf;

SELECT '=== Nachweis ===' AS marke;
SELECT COUNT(*) AS probetabellen_uebrig FROM information_schema.TABLES
 WHERE TABLE_SCHEMA='overlord_monitor' AND TABLE_NAME LIKE '%probe%';
SELECT TABLE_NAME FROM information_schema.TABLES
 WHERE TABLE_SCHEMA='overlord_monitor' ORDER BY TABLE_NAME;

SELECT '=== Die echten Tabellen sind unberuehrt ===' AS marke;
SELECT (SELECT COUNT(*)    FROM overlord_monitor.message_rollup)     AS rollup_zeilen,
       (SELECT SUM(anzahl) FROM overlord_monitor.message_rollup)     AS rollup_summe,
       (SELECT COUNT(*)    FROM overlord_monitor.message_rollup_tag) AS tag_zeilen,
       (SELECT SUM(anzahl) FROM overlord_monitor.message_rollup_tag) AS tag_summe;
SELECT COUNT(*) AS sekundaerindizes_auf_message_rollup FROM information_schema.STATISTICS
 WHERE TABLE_SCHEMA='overlord_monitor' AND TABLE_NAME='message_rollup' AND INDEX_NAME <> 'PRIMARY';
SELECT @@global.read_only AS read_only_am_ende, NOW() AS serverzeit_ende;
