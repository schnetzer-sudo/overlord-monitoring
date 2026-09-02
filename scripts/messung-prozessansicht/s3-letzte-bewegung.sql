-- Messung M114: DIE STICHWAHL FUER „LETZTE BEWEGUNG"
-- Auftrag:  "Schritt 10c-1: Prozessansicht, Backend", Stand 01.09.2026, Teil 2 und 4
-- Ergebnis: docs/process-view.md
--
-- ERZEUGT von scripts/messung-prozessansicht/erzeuge-s3.py.
-- Ausschliesslich SELECT / SET / EXPLAIN. (Regel S1)
--
--   D  korrelierte Unterabfrage als ORDER BY … LIMIT 1 statt MAX()
--   E  Ableitung, auf die Prozesse des Mandanten eingeschraenkt
--   G  Ableitung ueber den ganzen Bestand, mit Katalog — der Kandidat
--   F  wie G, aber IGNORE INDEX (message_rollup_prozess_idx)
--
-- F IST DIE EIGENTLICHE FRAGE DES AUFTRAGS. Teil 2 fragt, was ein voller
-- Durchlauf ueber 335.610 Zeilen kostete, wenn es den Index aus V11 nicht
-- gaebe. Der Index wird dafuer NICHT geloescht — die Tabelle ist geteilt, und
-- die Fensterverengung der Nachrichtenliste haengt an ihm (docs/rollup.md §9b).
-- IGNORE INDEX beantwortet dieselbe Frage, ohne etwas anzufassen.

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;

SET max_statement_time = 60;
SET profiling = 1;
SET profiling_history_size = 1000;

SELECT '=== M114-D-NEXANS · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT '=== M114-D-NEXANS · sechs Laeufe ===' AS marke;
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
SELECT 'M114-D-NEXANS' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M114-E-NEXANS · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           WHERE EXISTS (SELECT 1 FROM GlassfishDB.Process p2
                         JOIN GlassfishDB.ProjectMandant pm2
                              ON pm2.ProjectID = p2.ProjectID
                         WHERE p2.ProcessID = rr.process_id
                           AND pm2.MandantID = 'NEXANS')
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT '=== M114-E-NEXANS · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           WHERE EXISTS (SELECT 1 FROM GlassfishDB.Process p2
                         JOIN GlassfishDB.ProjectMandant pm2
                              ON pm2.ProjectID = p2.ProjectID
                         WHERE p2.ProcessID = rr.process_id
                           AND pm2.MandantID = 'NEXANS')
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           WHERE EXISTS (SELECT 1 FROM GlassfishDB.Process p2
                         JOIN GlassfishDB.ProjectMandant pm2
                              ON pm2.ProjectID = p2.ProjectID
                         WHERE p2.ProcessID = rr.process_id
                           AND pm2.MandantID = 'NEXANS')
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           WHERE EXISTS (SELECT 1 FROM GlassfishDB.Process p2
                         JOIN GlassfishDB.ProjectMandant pm2
                              ON pm2.ProjectID = p2.ProjectID
                         WHERE p2.ProcessID = rr.process_id
                           AND pm2.MandantID = 'NEXANS')
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           WHERE EXISTS (SELECT 1 FROM GlassfishDB.Process p2
                         JOIN GlassfishDB.ProjectMandant pm2
                              ON pm2.ProjectID = p2.ProjectID
                         WHERE p2.ProcessID = rr.process_id
                           AND pm2.MandantID = 'NEXANS')
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           WHERE EXISTS (SELECT 1 FROM GlassfishDB.Process p2
                         JOIN GlassfishDB.ProjectMandant pm2
                              ON pm2.ProjectID = p2.ProjectID
                         WHERE p2.ProcessID = rr.process_id
                           AND pm2.MandantID = 'NEXANS')
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           WHERE EXISTS (SELECT 1 FROM GlassfishDB.Process p2
                         JOIN GlassfishDB.ProjectMandant pm2
                              ON pm2.ProjectID = p2.ProjectID
                         WHERE p2.ProcessID = rr.process_id
                           AND pm2.MandantID = 'NEXANS')
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT 'M114-E-NEXANS' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M114-G-NEXANS · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT '=== M114-G-NEXANS · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT 'M114-G-NEXANS' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M114-F-NEXANS · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr IGNORE INDEX (message_rollup_prozess_idx)
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT '=== M114-F-NEXANS · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr IGNORE INDEX (message_rollup_prozess_idx)
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr IGNORE INDEX (message_rollup_prozess_idx)
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr IGNORE INDEX (message_rollup_prozess_idx)
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr IGNORE INDEX (message_rollup_prozess_idx)
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr IGNORE INDEX (message_rollup_prozess_idx)
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr IGNORE INDEX (message_rollup_prozess_idx)
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT 'M114-F-NEXANS' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M114-D-VOTG · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT '=== M114-D-VOTG · sechs Laeufe ===' AS marke;
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
SELECT 'M114-D-VOTG' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M114-E-VOTG · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           WHERE EXISTS (SELECT 1 FROM GlassfishDB.Process p2
                         JOIN GlassfishDB.ProjectMandant pm2
                              ON pm2.ProjectID = p2.ProjectID
                         WHERE p2.ProcessID = rr.process_id
                           AND pm2.MandantID = 'VOTG')
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT '=== M114-E-VOTG · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           WHERE EXISTS (SELECT 1 FROM GlassfishDB.Process p2
                         JOIN GlassfishDB.ProjectMandant pm2
                              ON pm2.ProjectID = p2.ProjectID
                         WHERE p2.ProcessID = rr.process_id
                           AND pm2.MandantID = 'VOTG')
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           WHERE EXISTS (SELECT 1 FROM GlassfishDB.Process p2
                         JOIN GlassfishDB.ProjectMandant pm2
                              ON pm2.ProjectID = p2.ProjectID
                         WHERE p2.ProcessID = rr.process_id
                           AND pm2.MandantID = 'VOTG')
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           WHERE EXISTS (SELECT 1 FROM GlassfishDB.Process p2
                         JOIN GlassfishDB.ProjectMandant pm2
                              ON pm2.ProjectID = p2.ProjectID
                         WHERE p2.ProcessID = rr.process_id
                           AND pm2.MandantID = 'VOTG')
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           WHERE EXISTS (SELECT 1 FROM GlassfishDB.Process p2
                         JOIN GlassfishDB.ProjectMandant pm2
                              ON pm2.ProjectID = p2.ProjectID
                         WHERE p2.ProcessID = rr.process_id
                           AND pm2.MandantID = 'VOTG')
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           WHERE EXISTS (SELECT 1 FROM GlassfishDB.Process p2
                         JOIN GlassfishDB.ProjectMandant pm2
                              ON pm2.ProjectID = p2.ProjectID
                         WHERE p2.ProcessID = rr.process_id
                           AND pm2.MandantID = 'VOTG')
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           WHERE EXISTS (SELECT 1 FROM GlassfishDB.Process p2
                         JOIN GlassfishDB.ProjectMandant pm2
                              ON pm2.ProjectID = p2.ProjectID
                         WHERE p2.ProcessID = rr.process_id
                           AND pm2.MandantID = 'VOTG')
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT 'M114-E-VOTG' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M114-G-VOTG · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT '=== M114-G-VOTG · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT 'M114-G-VOTG' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M114-F-VOTG · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr IGNORE INDEX (message_rollup_prozess_idx)
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT '=== M114-F-VOTG · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr IGNORE INDEX (message_rollup_prozess_idx)
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr IGNORE INDEX (message_rollup_prozess_idx)
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr IGNORE INDEX (message_rollup_prozess_idx)
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr IGNORE INDEX (message_rollup_prozess_idx)
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr IGNORE INDEX (message_rollup_prozess_idx)
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr IGNORE INDEX (message_rollup_prozess_idx)
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT 'M114-F-VOTG' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M114-D-IBIS · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT '=== M114-D-IBIS · sechs Laeufe ===' AS marke;
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
SELECT 'M114-D-IBIS' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M114-E-IBIS · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           WHERE EXISTS (SELECT 1 FROM GlassfishDB.Process p2
                         JOIN GlassfishDB.ProjectMandant pm2
                              ON pm2.ProjectID = p2.ProjectID
                         WHERE p2.ProcessID = rr.process_id
                           AND pm2.MandantID = 'IBIS')
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT '=== M114-E-IBIS · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           WHERE EXISTS (SELECT 1 FROM GlassfishDB.Process p2
                         JOIN GlassfishDB.ProjectMandant pm2
                              ON pm2.ProjectID = p2.ProjectID
                         WHERE p2.ProcessID = rr.process_id
                           AND pm2.MandantID = 'IBIS')
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           WHERE EXISTS (SELECT 1 FROM GlassfishDB.Process p2
                         JOIN GlassfishDB.ProjectMandant pm2
                              ON pm2.ProjectID = p2.ProjectID
                         WHERE p2.ProcessID = rr.process_id
                           AND pm2.MandantID = 'IBIS')
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           WHERE EXISTS (SELECT 1 FROM GlassfishDB.Process p2
                         JOIN GlassfishDB.ProjectMandant pm2
                              ON pm2.ProjectID = p2.ProjectID
                         WHERE p2.ProcessID = rr.process_id
                           AND pm2.MandantID = 'IBIS')
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           WHERE EXISTS (SELECT 1 FROM GlassfishDB.Process p2
                         JOIN GlassfishDB.ProjectMandant pm2
                              ON pm2.ProjectID = p2.ProjectID
                         WHERE p2.ProcessID = rr.process_id
                           AND pm2.MandantID = 'IBIS')
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           WHERE EXISTS (SELECT 1 FROM GlassfishDB.Process p2
                         JOIN GlassfishDB.ProjectMandant pm2
                              ON pm2.ProjectID = p2.ProjectID
                         WHERE p2.ProcessID = rr.process_id
                           AND pm2.MandantID = 'IBIS')
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           WHERE EXISTS (SELECT 1 FROM GlassfishDB.Process p2
                         JOIN GlassfishDB.ProjectMandant pm2
                              ON pm2.ProjectID = p2.ProjectID
                         WHERE p2.ProcessID = rr.process_id
                           AND pm2.MandantID = 'IBIS')
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT 'M114-E-IBIS' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M114-G-IBIS · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT '=== M114-G-IBIS · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT 'M114-G-IBIS' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M114-F-IBIS · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr IGNORE INDEX (message_rollup_prozess_idx)
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT '=== M114-F-IBIS · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr IGNORE INDEX (message_rollup_prozess_idx)
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr IGNORE INDEX (message_rollup_prozess_idx)
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr IGNORE INDEX (message_rollup_prozess_idx)
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr IGNORE INDEX (message_rollup_prozess_idx)
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr IGNORE INDEX (message_rollup_prozess_idx)
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr IGNORE INDEX (message_rollup_prozess_idx)
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT 'M114-F-IBIS' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M114-D-SUTTONS · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT r.stunde FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID
        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT '=== M114-D-SUTTONS · sechs Laeufe ===' AS marke;
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
SELECT 'M114-D-SUTTONS' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M114-E-SUTTONS · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           WHERE EXISTS (SELECT 1 FROM GlassfishDB.Process p2
                         JOIN GlassfishDB.ProjectMandant pm2
                              ON pm2.ProjectID = p2.ProjectID
                         WHERE p2.ProcessID = rr.process_id
                           AND pm2.MandantID = 'SUTTONS')
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT '=== M114-E-SUTTONS · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           WHERE EXISTS (SELECT 1 FROM GlassfishDB.Process p2
                         JOIN GlassfishDB.ProjectMandant pm2
                              ON pm2.ProjectID = p2.ProjectID
                         WHERE p2.ProcessID = rr.process_id
                           AND pm2.MandantID = 'SUTTONS')
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           WHERE EXISTS (SELECT 1 FROM GlassfishDB.Process p2
                         JOIN GlassfishDB.ProjectMandant pm2
                              ON pm2.ProjectID = p2.ProjectID
                         WHERE p2.ProcessID = rr.process_id
                           AND pm2.MandantID = 'SUTTONS')
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           WHERE EXISTS (SELECT 1 FROM GlassfishDB.Process p2
                         JOIN GlassfishDB.ProjectMandant pm2
                              ON pm2.ProjectID = p2.ProjectID
                         WHERE p2.ProcessID = rr.process_id
                           AND pm2.MandantID = 'SUTTONS')
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           WHERE EXISTS (SELECT 1 FROM GlassfishDB.Process p2
                         JOIN GlassfishDB.ProjectMandant pm2
                              ON pm2.ProjectID = p2.ProjectID
                         WHERE p2.ProcessID = rr.process_id
                           AND pm2.MandantID = 'SUTTONS')
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           WHERE EXISTS (SELECT 1 FROM GlassfishDB.Process p2
                         JOIN GlassfishDB.ProjectMandant pm2
                              ON pm2.ProjectID = p2.ProjectID
                         WHERE p2.ProcessID = rr.process_id
                           AND pm2.MandantID = 'SUTTONS')
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           WHERE EXISTS (SELECT 1 FROM GlassfishDB.Process p2
                         JOIN GlassfishDB.ProjectMandant pm2
                              ON pm2.ProjectID = p2.ProjectID
                         WHERE p2.ProcessID = rr.process_id
                           AND pm2.MandantID = 'SUTTONS')
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT 'M114-E-SUTTONS' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M114-G-SUTTONS · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT '=== M114-G-SUTTONS · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT 'M114-G-SUTTONS' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M114-F-SUTTONS · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr IGNORE INDEX (message_rollup_prozess_idx)
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT '=== M114-F-SUTTONS · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr IGNORE INDEX (message_rollup_prozess_idx)
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr IGNORE INDEX (message_rollup_prozess_idx)
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr IGNORE INDEX (message_rollup_prozess_idx)
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr IGNORE INDEX (message_rollup_prozess_idx)
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr IGNORE INDEX (message_rollup_prozess_idx)
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup rr IGNORE INDEX (message_rollup_prozess_idx)
           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT 'M114-F-SUTTONS' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

-- Gegenprobe: Liefern alle vier Fassungen dieselben Werte? Verglichen wird die
-- Zahl der Prozesse, die Zahl mit gesetzter letzter Bewegung und deren Maximum.
SELECT '=== M114-Z Gleichheitsprobe der Fassungen, NEXANS ===' AS marke;
SELECT 'D' AS fassung, COUNT(*) AS prozesse, COUNT(letzte_bewegung) AS mit_bewegung,
       MAX(letzte_bewegung) AS juengste
FROM (SELECT p.ProcessID,
             (SELECT r.stunde FROM overlord_monitor.message_rollup r
              WHERE r.process_id = p.ProcessID
              ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
      FROM GlassfishDB.Process p
      JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
      WHERE pm.MandantID = 'NEXANS') x
UNION ALL
SELECT 'G', COUNT(*), COUNT(letzte_bewegung), MAX(letzte_bewegung)
FROM (SELECT p.ProcessID, r.letzte_bewegung
      FROM GlassfishDB.Process p
      JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
      LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
                 FROM overlord_monitor.message_rollup rr
                 GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
      WHERE pm.MandantID = 'NEXANS') y;

-- Die Summenprobe aus M113, ohne LATERAL (MariaDB 10.6 kennt es nicht).
SELECT '=== M113-Z Summenprobe EXISTS gegen JOIN, 12 Monate ===' AS marke;
SELECT m.mandant,
       (SELECT SUM(r.anzahl) FROM overlord_monitor.message_rollup_monat r
        WHERE r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
          AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
                      JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
                      WHERE p.ProcessID = r.process_id
                        AND pm.MandantID = m.mandant))            AS aus_exists,
       (SELECT SUM(r.anzahl) FROM overlord_monitor.message_rollup_monat r
        JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
        JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
        WHERE pm.MandantID = m.mandant
          AND r.monat >= '2025-01-01' AND r.monat < '2026-01-01') AS aus_join
FROM (SELECT 'NEXANS' AS mandant UNION ALL SELECT 'VOTG' UNION ALL
      SELECT 'IBIS'   UNION ALL SELECT 'SUTTONS') m;

SELECT '=== 99 fertig ===' AS marke;
