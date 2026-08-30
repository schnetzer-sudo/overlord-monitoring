-- Messung M105 d - Der Schreibpfad MIT Index
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
-- Dieselben vier Schritte, dieselben zwei Fenster, gegen dieselbe Probetabelle, die jetzt den Index (process_id, stunde) traegt.
--
-- Benutzer: monitor_write
-- Z1: alle Zeitpunkte als Literal, nie NOW().

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, VERSION() AS version, NOW() AS serverzeit;

SET max_statement_time = 60;
SET profiling = 1;
SET profiling_history_size = 300;

SELECT '=== Zustand der Probetabelle ===' AS marke;
SHOW INDEX FROM overlord_monitor.message_rollup_probe_lauf;
SELECT TABLE_ROWS, DATA_LENGTH, INDEX_LENGTH FROM information_schema.TABLES
 WHERE TABLE_SCHEMA='overlord_monitor' AND TABLE_NAME='message_rollup_probe_lauf';

SELECT '########## delta-dichteste-stunde ##########' AS marke;
SELECT 'delta-dichteste-stunde' AS fall,
       (SELECT COUNT(*) FROM overlord_monitor.message_rollup
         WHERE stunde >= '2025-12-07 17:00:00' AND stunde < '2025-12-07 18:00:00') AS stundenzeilen,
       (SELECT COUNT(*) FROM overlord_monitor.message_rollup_tag
         WHERE tag >= '2025-12-07' AND tag <= '2025-12-07')  AS tageszeilen;
SELECT '=== delta-dichteste-stunde - Plan der Tagesableitung (L15) ===' AS marke;
EXPLAIN SELECT date(stunde), process_id, message_status, SUM(anzahl) FROM overlord_monitor.message_rollup_probe_lauf
 WHERE stunde >= '2025-12-07 17:00:00' AND stunde < '2025-12-07 18:00:00'
 GROUP BY date(stunde), process_id, message_status;
SELECT '=== delta-dichteste-stunde - Aufwaermlauf und fuenf (L7) ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
DELETE FROM overlord_monitor.message_rollup_probe_lauf WHERE stunde >= '2025-12-07 17:00:00' AND stunde < '2025-12-07 18:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2025-12-07 17:00:00' AND stunde < '2025-12-07 18:00:00';
DELETE FROM overlord_monitor.message_rollup_tag_probe_lauf WHERE tag >= '2025-12-07' AND tag <= '2025-12-07';
INSERT INTO overlord_monitor.message_rollup_tag_probe_lauf (tag, process_id, message_status, anzahl)
SELECT date(stunde), process_id, message_status, SUM(anzahl) FROM overlord_monitor.message_rollup_probe_lauf
 WHERE stunde >= '2025-12-07 17:00:00' AND stunde < '2025-12-07 18:00:00'
 GROUP BY date(stunde), process_id, message_status;
DELETE FROM overlord_monitor.message_rollup_probe_lauf WHERE stunde >= '2025-12-07 17:00:00' AND stunde < '2025-12-07 18:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2025-12-07 17:00:00' AND stunde < '2025-12-07 18:00:00';
DELETE FROM overlord_monitor.message_rollup_tag_probe_lauf WHERE tag >= '2025-12-07' AND tag <= '2025-12-07';
INSERT INTO overlord_monitor.message_rollup_tag_probe_lauf (tag, process_id, message_status, anzahl)
SELECT date(stunde), process_id, message_status, SUM(anzahl) FROM overlord_monitor.message_rollup_probe_lauf
 WHERE stunde >= '2025-12-07 17:00:00' AND stunde < '2025-12-07 18:00:00'
 GROUP BY date(stunde), process_id, message_status;
DELETE FROM overlord_monitor.message_rollup_probe_lauf WHERE stunde >= '2025-12-07 17:00:00' AND stunde < '2025-12-07 18:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2025-12-07 17:00:00' AND stunde < '2025-12-07 18:00:00';
DELETE FROM overlord_monitor.message_rollup_tag_probe_lauf WHERE tag >= '2025-12-07' AND tag <= '2025-12-07';
INSERT INTO overlord_monitor.message_rollup_tag_probe_lauf (tag, process_id, message_status, anzahl)
SELECT date(stunde), process_id, message_status, SUM(anzahl) FROM overlord_monitor.message_rollup_probe_lauf
 WHERE stunde >= '2025-12-07 17:00:00' AND stunde < '2025-12-07 18:00:00'
 GROUP BY date(stunde), process_id, message_status;
DELETE FROM overlord_monitor.message_rollup_probe_lauf WHERE stunde >= '2025-12-07 17:00:00' AND stunde < '2025-12-07 18:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2025-12-07 17:00:00' AND stunde < '2025-12-07 18:00:00';
DELETE FROM overlord_monitor.message_rollup_tag_probe_lauf WHERE tag >= '2025-12-07' AND tag <= '2025-12-07';
INSERT INTO overlord_monitor.message_rollup_tag_probe_lauf (tag, process_id, message_status, anzahl)
SELECT date(stunde), process_id, message_status, SUM(anzahl) FROM overlord_monitor.message_rollup_probe_lauf
 WHERE stunde >= '2025-12-07 17:00:00' AND stunde < '2025-12-07 18:00:00'
 GROUP BY date(stunde), process_id, message_status;
DELETE FROM overlord_monitor.message_rollup_probe_lauf WHERE stunde >= '2025-12-07 17:00:00' AND stunde < '2025-12-07 18:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2025-12-07 17:00:00' AND stunde < '2025-12-07 18:00:00';
DELETE FROM overlord_monitor.message_rollup_tag_probe_lauf WHERE tag >= '2025-12-07' AND tag <= '2025-12-07';
INSERT INTO overlord_monitor.message_rollup_tag_probe_lauf (tag, process_id, message_status, anzahl)
SELECT date(stunde), process_id, message_status, SUM(anzahl) FROM overlord_monitor.message_rollup_probe_lauf
 WHERE stunde >= '2025-12-07 17:00:00' AND stunde < '2025-12-07 18:00:00'
 GROUP BY date(stunde), process_id, message_status;
DELETE FROM overlord_monitor.message_rollup_probe_lauf WHERE stunde >= '2025-12-07 17:00:00' AND stunde < '2025-12-07 18:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2025-12-07 17:00:00' AND stunde < '2025-12-07 18:00:00';
DELETE FROM overlord_monitor.message_rollup_tag_probe_lauf WHERE tag >= '2025-12-07' AND tag <= '2025-12-07';
INSERT INTO overlord_monitor.message_rollup_tag_probe_lauf (tag, process_id, message_status, anzahl)
SELECT date(stunde), process_id, message_status, SUM(anzahl) FROM overlord_monitor.message_rollup_probe_lauf
 WHERE stunde >= '2025-12-07 17:00:00' AND stunde < '2025-12-07 18:00:00'
 GROUP BY date(stunde), process_id, message_status;
SELECT 'delta-dichteste-stunde' AS fall,
       CASE (rn - 1) % 4 WHEN 0 THEN '1 DELETE stunde'
                         WHEN 1 THEN '2 INSERT stunde'
                         WHEN 2 THEN '3 DELETE tag'
                         ELSE        '4 INSERT tag (liest overlord_monitor.message_rollup_probe_lauf)' END AS schritt,
       COUNT(*)                          AS laeufe,
       MAX(CASE WHEN lauf = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN lauf > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN lauf > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT rn, CEIL(rn / 4) AS lauf, ms FROM (
        SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
               ROUND(SUM(DURATION) * 1000, 3) AS ms
        FROM information_schema.PROFILING
        WHERE QUERY_ID > @basis + 1
        GROUP BY QUERY_ID) x) y
GROUP BY schritt ORDER BY schritt;

SELECT '########## monatsscheibe-2025-07 ##########' AS marke;
SELECT 'monatsscheibe-2025-07' AS fall,
       (SELECT COUNT(*) FROM overlord_monitor.message_rollup
         WHERE stunde >= '2025-07-01 00:00:00' AND stunde < '2025-08-01 00:00:00') AS stundenzeilen,
       (SELECT COUNT(*) FROM overlord_monitor.message_rollup_tag
         WHERE tag >= '2025-07-01' AND tag <= '2025-07-31')  AS tageszeilen;
SELECT '=== monatsscheibe-2025-07 - Plan der Tagesableitung (L15) ===' AS marke;
EXPLAIN SELECT date(stunde), process_id, message_status, SUM(anzahl) FROM overlord_monitor.message_rollup_probe_lauf
 WHERE stunde >= '2025-07-01 00:00:00' AND stunde < '2025-08-01 00:00:00'
 GROUP BY date(stunde), process_id, message_status;
SELECT '=== monatsscheibe-2025-07 - Aufwaermlauf und fuenf (L7) ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
DELETE FROM overlord_monitor.message_rollup_probe_lauf WHERE stunde >= '2025-07-01 00:00:00' AND stunde < '2025-08-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2025-07-01 00:00:00' AND stunde < '2025-08-01 00:00:00';
DELETE FROM overlord_monitor.message_rollup_tag_probe_lauf WHERE tag >= '2025-07-01' AND tag <= '2025-07-31';
INSERT INTO overlord_monitor.message_rollup_tag_probe_lauf (tag, process_id, message_status, anzahl)
SELECT date(stunde), process_id, message_status, SUM(anzahl) FROM overlord_monitor.message_rollup_probe_lauf
 WHERE stunde >= '2025-07-01 00:00:00' AND stunde < '2025-08-01 00:00:00'
 GROUP BY date(stunde), process_id, message_status;
DELETE FROM overlord_monitor.message_rollup_probe_lauf WHERE stunde >= '2025-07-01 00:00:00' AND stunde < '2025-08-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2025-07-01 00:00:00' AND stunde < '2025-08-01 00:00:00';
DELETE FROM overlord_monitor.message_rollup_tag_probe_lauf WHERE tag >= '2025-07-01' AND tag <= '2025-07-31';
INSERT INTO overlord_monitor.message_rollup_tag_probe_lauf (tag, process_id, message_status, anzahl)
SELECT date(stunde), process_id, message_status, SUM(anzahl) FROM overlord_monitor.message_rollup_probe_lauf
 WHERE stunde >= '2025-07-01 00:00:00' AND stunde < '2025-08-01 00:00:00'
 GROUP BY date(stunde), process_id, message_status;
DELETE FROM overlord_monitor.message_rollup_probe_lauf WHERE stunde >= '2025-07-01 00:00:00' AND stunde < '2025-08-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2025-07-01 00:00:00' AND stunde < '2025-08-01 00:00:00';
DELETE FROM overlord_monitor.message_rollup_tag_probe_lauf WHERE tag >= '2025-07-01' AND tag <= '2025-07-31';
INSERT INTO overlord_monitor.message_rollup_tag_probe_lauf (tag, process_id, message_status, anzahl)
SELECT date(stunde), process_id, message_status, SUM(anzahl) FROM overlord_monitor.message_rollup_probe_lauf
 WHERE stunde >= '2025-07-01 00:00:00' AND stunde < '2025-08-01 00:00:00'
 GROUP BY date(stunde), process_id, message_status;
DELETE FROM overlord_monitor.message_rollup_probe_lauf WHERE stunde >= '2025-07-01 00:00:00' AND stunde < '2025-08-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2025-07-01 00:00:00' AND stunde < '2025-08-01 00:00:00';
DELETE FROM overlord_monitor.message_rollup_tag_probe_lauf WHERE tag >= '2025-07-01' AND tag <= '2025-07-31';
INSERT INTO overlord_monitor.message_rollup_tag_probe_lauf (tag, process_id, message_status, anzahl)
SELECT date(stunde), process_id, message_status, SUM(anzahl) FROM overlord_monitor.message_rollup_probe_lauf
 WHERE stunde >= '2025-07-01 00:00:00' AND stunde < '2025-08-01 00:00:00'
 GROUP BY date(stunde), process_id, message_status;
DELETE FROM overlord_monitor.message_rollup_probe_lauf WHERE stunde >= '2025-07-01 00:00:00' AND stunde < '2025-08-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2025-07-01 00:00:00' AND stunde < '2025-08-01 00:00:00';
DELETE FROM overlord_monitor.message_rollup_tag_probe_lauf WHERE tag >= '2025-07-01' AND tag <= '2025-07-31';
INSERT INTO overlord_monitor.message_rollup_tag_probe_lauf (tag, process_id, message_status, anzahl)
SELECT date(stunde), process_id, message_status, SUM(anzahl) FROM overlord_monitor.message_rollup_probe_lauf
 WHERE stunde >= '2025-07-01 00:00:00' AND stunde < '2025-08-01 00:00:00'
 GROUP BY date(stunde), process_id, message_status;
DELETE FROM overlord_monitor.message_rollup_probe_lauf WHERE stunde >= '2025-07-01 00:00:00' AND stunde < '2025-08-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2025-07-01 00:00:00' AND stunde < '2025-08-01 00:00:00';
DELETE FROM overlord_monitor.message_rollup_tag_probe_lauf WHERE tag >= '2025-07-01' AND tag <= '2025-07-31';
INSERT INTO overlord_monitor.message_rollup_tag_probe_lauf (tag, process_id, message_status, anzahl)
SELECT date(stunde), process_id, message_status, SUM(anzahl) FROM overlord_monitor.message_rollup_probe_lauf
 WHERE stunde >= '2025-07-01 00:00:00' AND stunde < '2025-08-01 00:00:00'
 GROUP BY date(stunde), process_id, message_status;
SELECT 'monatsscheibe-2025-07' AS fall,
       CASE (rn - 1) % 4 WHEN 0 THEN '1 DELETE stunde'
                         WHEN 1 THEN '2 INSERT stunde'
                         WHEN 2 THEN '3 DELETE tag'
                         ELSE        '4 INSERT tag (liest overlord_monitor.message_rollup_probe_lauf)' END AS schritt,
       COUNT(*)                          AS laeufe,
       MAX(CASE WHEN lauf = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN lauf > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN lauf > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT rn, CEIL(rn / 4) AS lauf, ms FROM (
        SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
               ROUND(SUM(DURATION) * 1000, 3) AS ms
        FROM information_schema.PROFILING
        WHERE QUERY_ID > @basis + 1
        GROUP BY QUERY_ID) x) y
GROUP BY schritt ORDER BY schritt;

SELECT '=== Der Volllauf: alle 23 Scheiben, Stundenebene ===' AS marke;
SELECT NOW(3) AS volllauf_beginn;
DELETE FROM overlord_monitor.message_rollup_probe_lauf WHERE stunde >= '2024-10-01 00:00:00' AND stunde < '2024-11-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2024-10-01 00:00:00' AND stunde < '2024-11-01 00:00:00';
DELETE FROM overlord_monitor.message_rollup_probe_lauf WHERE stunde >= '2024-11-01 00:00:00' AND stunde < '2024-12-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2024-11-01 00:00:00' AND stunde < '2024-12-01 00:00:00';
DELETE FROM overlord_monitor.message_rollup_probe_lauf WHERE stunde >= '2024-12-01 00:00:00' AND stunde < '2025-01-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2024-12-01 00:00:00' AND stunde < '2025-01-01 00:00:00';
DELETE FROM overlord_monitor.message_rollup_probe_lauf WHERE stunde >= '2025-01-01 00:00:00' AND stunde < '2025-02-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2025-01-01 00:00:00' AND stunde < '2025-02-01 00:00:00';
DELETE FROM overlord_monitor.message_rollup_probe_lauf WHERE stunde >= '2025-02-01 00:00:00' AND stunde < '2025-03-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2025-02-01 00:00:00' AND stunde < '2025-03-01 00:00:00';
DELETE FROM overlord_monitor.message_rollup_probe_lauf WHERE stunde >= '2025-03-01 00:00:00' AND stunde < '2025-04-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2025-03-01 00:00:00' AND stunde < '2025-04-01 00:00:00';
DELETE FROM overlord_monitor.message_rollup_probe_lauf WHERE stunde >= '2025-04-01 00:00:00' AND stunde < '2025-05-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2025-04-01 00:00:00' AND stunde < '2025-05-01 00:00:00';
DELETE FROM overlord_monitor.message_rollup_probe_lauf WHERE stunde >= '2025-05-01 00:00:00' AND stunde < '2025-06-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2025-05-01 00:00:00' AND stunde < '2025-06-01 00:00:00';
DELETE FROM overlord_monitor.message_rollup_probe_lauf WHERE stunde >= '2025-06-01 00:00:00' AND stunde < '2025-07-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2025-06-01 00:00:00' AND stunde < '2025-07-01 00:00:00';
DELETE FROM overlord_monitor.message_rollup_probe_lauf WHERE stunde >= '2025-07-01 00:00:00' AND stunde < '2025-08-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2025-07-01 00:00:00' AND stunde < '2025-08-01 00:00:00';
DELETE FROM overlord_monitor.message_rollup_probe_lauf WHERE stunde >= '2025-08-01 00:00:00' AND stunde < '2025-09-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2025-08-01 00:00:00' AND stunde < '2025-09-01 00:00:00';
DELETE FROM overlord_monitor.message_rollup_probe_lauf WHERE stunde >= '2025-09-01 00:00:00' AND stunde < '2025-10-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2025-09-01 00:00:00' AND stunde < '2025-10-01 00:00:00';
DELETE FROM overlord_monitor.message_rollup_probe_lauf WHERE stunde >= '2025-10-01 00:00:00' AND stunde < '2025-11-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2025-10-01 00:00:00' AND stunde < '2025-11-01 00:00:00';
DELETE FROM overlord_monitor.message_rollup_probe_lauf WHERE stunde >= '2025-11-01 00:00:00' AND stunde < '2025-12-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2025-11-01 00:00:00' AND stunde < '2025-12-01 00:00:00';
DELETE FROM overlord_monitor.message_rollup_probe_lauf WHERE stunde >= '2025-12-01 00:00:00' AND stunde < '2026-01-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2025-12-01 00:00:00' AND stunde < '2026-01-01 00:00:00';
DELETE FROM overlord_monitor.message_rollup_probe_lauf WHERE stunde >= '2026-01-01 00:00:00' AND stunde < '2026-02-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2026-01-01 00:00:00' AND stunde < '2026-02-01 00:00:00';
DELETE FROM overlord_monitor.message_rollup_probe_lauf WHERE stunde >= '2026-02-01 00:00:00' AND stunde < '2026-03-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2026-02-01 00:00:00' AND stunde < '2026-03-01 00:00:00';
DELETE FROM overlord_monitor.message_rollup_probe_lauf WHERE stunde >= '2026-03-01 00:00:00' AND stunde < '2026-04-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2026-03-01 00:00:00' AND stunde < '2026-04-01 00:00:00';
DELETE FROM overlord_monitor.message_rollup_probe_lauf WHERE stunde >= '2026-04-01 00:00:00' AND stunde < '2026-05-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2026-04-01 00:00:00' AND stunde < '2026-05-01 00:00:00';
DELETE FROM overlord_monitor.message_rollup_probe_lauf WHERE stunde >= '2026-05-01 00:00:00' AND stunde < '2026-06-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2026-05-01 00:00:00' AND stunde < '2026-06-01 00:00:00';
DELETE FROM overlord_monitor.message_rollup_probe_lauf WHERE stunde >= '2026-06-01 00:00:00' AND stunde < '2026-07-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2026-06-01 00:00:00' AND stunde < '2026-07-01 00:00:00';
DELETE FROM overlord_monitor.message_rollup_probe_lauf WHERE stunde >= '2026-07-01 00:00:00' AND stunde < '2026-08-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2026-07-01 00:00:00' AND stunde < '2026-08-01 00:00:00';
DELETE FROM overlord_monitor.message_rollup_probe_lauf WHERE stunde >= '2026-08-01 00:00:00' AND stunde < '2026-09-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2026-08-01 00:00:00' AND stunde < '2026-09-01 00:00:00';
SELECT NOW(3) AS volllauf_ende;
SELECT COUNT(*) AS probe_stunden_nach_volllauf FROM overlord_monitor.message_rollup_probe_lauf;

SELECT '=== 99 Abschluss ===' AS marke;
SELECT @@global.read_only AS read_only_am_ende, NOW() AS serverzeit_ende;
