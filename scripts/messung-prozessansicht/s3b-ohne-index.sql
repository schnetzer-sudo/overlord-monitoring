-- Messung M115: WAS DER INDEX AUS V11 DER GEWAEHLTEN FASSUNG BRINGT
-- Auftrag:  "Schritt 10c-1: Prozessansicht, Backend", Stand 01.09.2026, Teil 2
-- Ergebnis: docs/process-view.md
--
-- ERZEUGT von scripts/messung-prozessansicht/erzeuge-s3b.py.
-- Ausschliesslich SELECT / SET / EXPLAIN. (Regel S1)
--
-- Der Index wird NICHT geloescht. Die Tabelle ist geteilt, und die
-- Fensterverengung der Nachrichtenliste haengt an ihm (docs/rollup.md §9b).
-- IGNORE INDEX beantwortet dieselbe Frage, ohne etwas anzufassen.

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;

SET max_statement_time = 60;
SET profiling = 1;
SET profiling_history_size = 1000;

SELECT '=== M115-mit-NEXANS · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT '=== M115-mit-NEXANS · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT 'M115-mit-NEXANS' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M115-ohne-NEXANS · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r IGNORE INDEX (message_rollup_prozess_idx)
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT '=== M115-ohne-NEXANS · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r IGNORE INDEX (message_rollup_prozess_idx)
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r IGNORE INDEX (message_rollup_prozess_idx)
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r IGNORE INDEX (message_rollup_prozess_idx)
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r IGNORE INDEX (message_rollup_prozess_idx)
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r IGNORE INDEX (message_rollup_prozess_idx)
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r IGNORE INDEX (message_rollup_prozess_idx)
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT 'M115-ohne-NEXANS' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M115-mit-VOTG · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT '=== M115-mit-VOTG · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT 'M115-mit-VOTG' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M115-ohne-VOTG · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r IGNORE INDEX (message_rollup_prozess_idx)
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT '=== M115-ohne-VOTG · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r IGNORE INDEX (message_rollup_prozess_idx)
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r IGNORE INDEX (message_rollup_prozess_idx)
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r IGNORE INDEX (message_rollup_prozess_idx)
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r IGNORE INDEX (message_rollup_prozess_idx)
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r IGNORE INDEX (message_rollup_prozess_idx)
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r IGNORE INDEX (message_rollup_prozess_idx)
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT 'M115-ohne-VOTG' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M115-mit-IBIS · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT '=== M115-mit-IBIS · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT 'M115-mit-IBIS' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M115-ohne-IBIS · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r IGNORE INDEX (message_rollup_prozess_idx)
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT '=== M115-ohne-IBIS · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r IGNORE INDEX (message_rollup_prozess_idx)
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r IGNORE INDEX (message_rollup_prozess_idx)
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r IGNORE INDEX (message_rollup_prozess_idx)
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r IGNORE INDEX (message_rollup_prozess_idx)
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r IGNORE INDEX (message_rollup_prozess_idx)
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r IGNORE INDEX (message_rollup_prozess_idx)
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT 'M115-ohne-IBIS' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M115-mit-SUTTONS · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT '=== M115-mit-SUTTONS · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT 'M115-mit-SUTTONS' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M115-ohne-SUTTONS · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r IGNORE INDEX (message_rollup_prozess_idx)
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT '=== M115-ohne-SUTTONS · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r IGNORE INDEX (message_rollup_prozess_idx)
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r IGNORE INDEX (message_rollup_prozess_idx)
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r IGNORE INDEX (message_rollup_prozess_idx)
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r IGNORE INDEX (message_rollup_prozess_idx)
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r IGNORE INDEX (message_rollup_prozess_idx)
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r IGNORE INDEX (message_rollup_prozess_idx)
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT 'M115-ohne-SUTTONS' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== 99 fertig ===' AS marke;
