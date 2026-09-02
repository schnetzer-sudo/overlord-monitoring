-- Messung M112 und M113: DIE ZWEI STATEMENTS DER PROZESSANSICHT, IN FASSUNGEN
-- Auftrag:  "Schritt 10c-1: Prozessansicht, Backend", Stand 01.09.2026, Teil 4
-- Ergebnis: docs/process-view.md
--
-- ERZEUGT von scripts/messung-prozessansicht/erzeuge-s2.py — nicht von Hand
-- aendern, sondern den Erzeuger aendern und neu erzeugen.
--
-- Ausschliesslich SELECT / SET / EXPLAIN. (Regel S1)
--
-- M112  „letzte Bewegung" — MAX(stunde) je Prozess, FENSTERUNABHAENGIG. Drei
--       Fassungen: korrelierte Unterabfrage (A), Ableitung ueber den ganzen
--       Bestand (B), und A mit dem Katalog daneben (C, der Kandidat).
-- M113  die Kennzahlen im Fenster, je Ebene und je Fassung der Mandantenkette.
--
-- Z1: Alle Fenstergrenzen sind Literale, hergeleitet aus dem Dev-Anker 2025-12-30 04:09:47.
-- G1: Ausgegeben werden Kennungen und Zahlen des Konfigurationsvokabulars,
--     keine Partnernamen — die Auswertung unten zaehlt nur.
--
-- Die Zeitgrenze steht auf 60 s und nicht auf den 10 s des Lese-Pools: Eine
-- Fassung, die reisst, soll ihre Laufzeit noch nennen koennen. Wo eine Fassung
-- ueber 10 s liegt, ist sie fuer die Anwendung unbrauchbar — das steht dann im
-- Befund und nicht in einem abgebrochenen Lauf.

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;

SET max_statement_time = 60;
SET profiling = 1;
SET profiling_history_size = 1000;

-- ####################  M112 — letzte Bewegung  ####################

SELECT '=== M112-A-NEXANS · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT p.ProcessID, p.ProcessName,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT '=== M112-A-NEXANS · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT p.ProcessID, p.ProcessName,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT 'M112-A-NEXANS' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M112-B-NEXANS · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT p.ProcessID, p.ProcessName, r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN (SELECT process_id, MAX(stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup
           GROUP BY process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT '=== M112-B-NEXANS · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT p.ProcessID, p.ProcessName, r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN (SELECT process_id, MAX(stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup
           GROUP BY process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN (SELECT process_id, MAX(stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup
           GROUP BY process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN (SELECT process_id, MAX(stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup
           GROUP BY process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN (SELECT process_id, MAX(stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup
           GROUP BY process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN (SELECT process_id, MAX(stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup
           GROUP BY process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN (SELECT process_id, MAX(stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup
           GROUP BY process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT 'M112-B-NEXANS' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M112-C-NEXANS · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT '=== M112-C-NEXANS · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'NEXANS'
ORDER BY p.ProcessName;
SELECT 'M112-C-NEXANS' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M112-A-VOTG · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT p.ProcessID, p.ProcessName,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT '=== M112-A-VOTG · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT p.ProcessID, p.ProcessName,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT 'M112-A-VOTG' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M112-B-VOTG · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT p.ProcessID, p.ProcessName, r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN (SELECT process_id, MAX(stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup
           GROUP BY process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT '=== M112-B-VOTG · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT p.ProcessID, p.ProcessName, r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN (SELECT process_id, MAX(stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup
           GROUP BY process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN (SELECT process_id, MAX(stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup
           GROUP BY process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN (SELECT process_id, MAX(stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup
           GROUP BY process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN (SELECT process_id, MAX(stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup
           GROUP BY process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN (SELECT process_id, MAX(stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup
           GROUP BY process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN (SELECT process_id, MAX(stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup
           GROUP BY process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT 'M112-B-VOTG' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M112-C-VOTG · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT '=== M112-C-VOTG · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'VOTG'
ORDER BY p.ProcessName;
SELECT 'M112-C-VOTG' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M112-A-IBIS · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT p.ProcessID, p.ProcessName,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT '=== M112-A-IBIS · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT p.ProcessID, p.ProcessName,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT 'M112-A-IBIS' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M112-B-IBIS · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT p.ProcessID, p.ProcessName, r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN (SELECT process_id, MAX(stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup
           GROUP BY process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT '=== M112-B-IBIS · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT p.ProcessID, p.ProcessName, r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN (SELECT process_id, MAX(stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup
           GROUP BY process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN (SELECT process_id, MAX(stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup
           GROUP BY process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN (SELECT process_id, MAX(stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup
           GROUP BY process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN (SELECT process_id, MAX(stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup
           GROUP BY process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN (SELECT process_id, MAX(stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup
           GROUP BY process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN (SELECT process_id, MAX(stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup
           GROUP BY process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT 'M112-B-IBIS' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M112-C-IBIS · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT '=== M112-C-IBIS · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'IBIS'
ORDER BY p.ProcessName;
SELECT 'M112-C-IBIS' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M112-A-SUTTONS · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT p.ProcessID, p.ProcessName,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT '=== M112-A-SUTTONS · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT p.ProcessID, p.ProcessName,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT 'M112-A-SUTTONS' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M112-B-SUTTONS · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT p.ProcessID, p.ProcessName, r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN (SELECT process_id, MAX(stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup
           GROUP BY process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT '=== M112-B-SUTTONS · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT p.ProcessID, p.ProcessName, r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN (SELECT process_id, MAX(stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup
           GROUP BY process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN (SELECT process_id, MAX(stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup
           GROUP BY process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN (SELECT process_id, MAX(stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup
           GROUP BY process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN (SELECT process_id, MAX(stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup
           GROUP BY process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN (SELECT process_id, MAX(stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup
           GROUP BY process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, r.letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN (SELECT process_id, MAX(stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup
           GROUP BY process_id) r ON r.process_id = p.ProcessID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT 'M112-B-SUTTONS' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M112-C-SUTTONS · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT '=== M112-C-SUTTONS · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,
       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r
        WHERE r.process_id = p.ProcessID) AS letzte_bewegung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
WHERE pm.MandantID = 'SUTTONS'
ORDER BY p.ProcessName;
SELECT 'M112-C-SUTTONS' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

-- ####################  M113 — Kennzahlen im Fenster  ####################

SELECT '=== M113-EXISTS-NEXANS-48H · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
WHERE r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'NEXANS')
GROUP BY r.process_id, r.message_status;
SELECT '=== M113-EXISTS-NEXANS-48H · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
WHERE r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'NEXANS')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
WHERE r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'NEXANS')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
WHERE r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'NEXANS')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
WHERE r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'NEXANS')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
WHERE r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'NEXANS')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
WHERE r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'NEXANS')
GROUP BY r.process_id, r.message_status;
SELECT 'M113-EXISTS-NEXANS-48H' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M113-JOIN-NEXANS-48H · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
GROUP BY r.process_id, r.message_status;
SELECT '=== M113-JOIN-NEXANS-48H · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
GROUP BY r.process_id, r.message_status;
SELECT 'M113-JOIN-NEXANS-48H' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M113-EXISTS-NEXANS-30T · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
WHERE r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'NEXANS')
GROUP BY r.process_id, r.message_status;
SELECT '=== M113-EXISTS-NEXANS-30T · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
WHERE r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'NEXANS')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
WHERE r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'NEXANS')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
WHERE r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'NEXANS')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
WHERE r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'NEXANS')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
WHERE r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'NEXANS')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
WHERE r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'NEXANS')
GROUP BY r.process_id, r.message_status;
SELECT 'M113-EXISTS-NEXANS-30T' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M113-JOIN-NEXANS-30T · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'NEXANS'
  AND r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
GROUP BY r.process_id, r.message_status;
SELECT '=== M113-JOIN-NEXANS-30T · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'NEXANS'
  AND r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'NEXANS'
  AND r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'NEXANS'
  AND r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'NEXANS'
  AND r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'NEXANS'
  AND r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'NEXANS'
  AND r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
GROUP BY r.process_id, r.message_status;
SELECT 'M113-JOIN-NEXANS-30T' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M113-EXISTS-NEXANS-12M · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
WHERE r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'NEXANS')
GROUP BY r.process_id, r.message_status;
SELECT '=== M113-EXISTS-NEXANS-12M · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
WHERE r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'NEXANS')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
WHERE r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'NEXANS')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
WHERE r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'NEXANS')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
WHERE r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'NEXANS')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
WHERE r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'NEXANS')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
WHERE r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'NEXANS')
GROUP BY r.process_id, r.message_status;
SELECT 'M113-EXISTS-NEXANS-12M' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M113-JOIN-NEXANS-12M · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'NEXANS'
  AND r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
GROUP BY r.process_id, r.message_status;
SELECT '=== M113-JOIN-NEXANS-12M · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'NEXANS'
  AND r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'NEXANS'
  AND r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'NEXANS'
  AND r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'NEXANS'
  AND r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'NEXANS'
  AND r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'NEXANS'
  AND r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
GROUP BY r.process_id, r.message_status;
SELECT 'M113-JOIN-NEXANS-12M' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M113-EXISTS-VOTG-48H · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
WHERE r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'VOTG')
GROUP BY r.process_id, r.message_status;
SELECT '=== M113-EXISTS-VOTG-48H · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
WHERE r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'VOTG')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
WHERE r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'VOTG')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
WHERE r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'VOTG')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
WHERE r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'VOTG')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
WHERE r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'VOTG')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
WHERE r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'VOTG')
GROUP BY r.process_id, r.message_status;
SELECT 'M113-EXISTS-VOTG-48H' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M113-JOIN-VOTG-48H · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'VOTG'
  AND r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
GROUP BY r.process_id, r.message_status;
SELECT '=== M113-JOIN-VOTG-48H · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'VOTG'
  AND r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'VOTG'
  AND r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'VOTG'
  AND r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'VOTG'
  AND r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'VOTG'
  AND r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'VOTG'
  AND r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
GROUP BY r.process_id, r.message_status;
SELECT 'M113-JOIN-VOTG-48H' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M113-EXISTS-VOTG-30T · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
WHERE r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'VOTG')
GROUP BY r.process_id, r.message_status;
SELECT '=== M113-EXISTS-VOTG-30T · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
WHERE r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'VOTG')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
WHERE r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'VOTG')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
WHERE r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'VOTG')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
WHERE r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'VOTG')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
WHERE r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'VOTG')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
WHERE r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'VOTG')
GROUP BY r.process_id, r.message_status;
SELECT 'M113-EXISTS-VOTG-30T' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M113-JOIN-VOTG-30T · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'VOTG'
  AND r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
GROUP BY r.process_id, r.message_status;
SELECT '=== M113-JOIN-VOTG-30T · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'VOTG'
  AND r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'VOTG'
  AND r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'VOTG'
  AND r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'VOTG'
  AND r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'VOTG'
  AND r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'VOTG'
  AND r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
GROUP BY r.process_id, r.message_status;
SELECT 'M113-JOIN-VOTG-30T' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M113-EXISTS-VOTG-12M · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
WHERE r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'VOTG')
GROUP BY r.process_id, r.message_status;
SELECT '=== M113-EXISTS-VOTG-12M · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
WHERE r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'VOTG')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
WHERE r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'VOTG')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
WHERE r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'VOTG')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
WHERE r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'VOTG')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
WHERE r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'VOTG')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
WHERE r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'VOTG')
GROUP BY r.process_id, r.message_status;
SELECT 'M113-EXISTS-VOTG-12M' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M113-JOIN-VOTG-12M · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'VOTG'
  AND r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
GROUP BY r.process_id, r.message_status;
SELECT '=== M113-JOIN-VOTG-12M · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'VOTG'
  AND r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'VOTG'
  AND r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'VOTG'
  AND r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'VOTG'
  AND r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'VOTG'
  AND r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'VOTG'
  AND r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
GROUP BY r.process_id, r.message_status;
SELECT 'M113-JOIN-VOTG-12M' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M113-EXISTS-IBIS-48H · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
WHERE r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'IBIS')
GROUP BY r.process_id, r.message_status;
SELECT '=== M113-EXISTS-IBIS-48H · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
WHERE r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'IBIS')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
WHERE r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'IBIS')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
WHERE r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'IBIS')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
WHERE r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'IBIS')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
WHERE r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'IBIS')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
WHERE r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'IBIS')
GROUP BY r.process_id, r.message_status;
SELECT 'M113-EXISTS-IBIS-48H' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M113-JOIN-IBIS-48H · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'IBIS'
  AND r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
GROUP BY r.process_id, r.message_status;
SELECT '=== M113-JOIN-IBIS-48H · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'IBIS'
  AND r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'IBIS'
  AND r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'IBIS'
  AND r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'IBIS'
  AND r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'IBIS'
  AND r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'IBIS'
  AND r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
GROUP BY r.process_id, r.message_status;
SELECT 'M113-JOIN-IBIS-48H' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M113-EXISTS-IBIS-30T · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
WHERE r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'IBIS')
GROUP BY r.process_id, r.message_status;
SELECT '=== M113-EXISTS-IBIS-30T · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
WHERE r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'IBIS')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
WHERE r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'IBIS')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
WHERE r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'IBIS')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
WHERE r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'IBIS')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
WHERE r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'IBIS')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
WHERE r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'IBIS')
GROUP BY r.process_id, r.message_status;
SELECT 'M113-EXISTS-IBIS-30T' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M113-JOIN-IBIS-30T · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'IBIS'
  AND r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
GROUP BY r.process_id, r.message_status;
SELECT '=== M113-JOIN-IBIS-30T · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'IBIS'
  AND r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'IBIS'
  AND r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'IBIS'
  AND r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'IBIS'
  AND r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'IBIS'
  AND r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'IBIS'
  AND r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
GROUP BY r.process_id, r.message_status;
SELECT 'M113-JOIN-IBIS-30T' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M113-EXISTS-IBIS-12M · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
WHERE r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'IBIS')
GROUP BY r.process_id, r.message_status;
SELECT '=== M113-EXISTS-IBIS-12M · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
WHERE r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'IBIS')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
WHERE r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'IBIS')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
WHERE r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'IBIS')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
WHERE r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'IBIS')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
WHERE r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'IBIS')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
WHERE r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'IBIS')
GROUP BY r.process_id, r.message_status;
SELECT 'M113-EXISTS-IBIS-12M' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M113-JOIN-IBIS-12M · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'IBIS'
  AND r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
GROUP BY r.process_id, r.message_status;
SELECT '=== M113-JOIN-IBIS-12M · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'IBIS'
  AND r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'IBIS'
  AND r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'IBIS'
  AND r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'IBIS'
  AND r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'IBIS'
  AND r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'IBIS'
  AND r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
GROUP BY r.process_id, r.message_status;
SELECT 'M113-JOIN-IBIS-12M' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M113-EXISTS-SUTTONS-48H · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
WHERE r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'SUTTONS')
GROUP BY r.process_id, r.message_status;
SELECT '=== M113-EXISTS-SUTTONS-48H · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
WHERE r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'SUTTONS')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
WHERE r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'SUTTONS')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
WHERE r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'SUTTONS')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
WHERE r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'SUTTONS')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
WHERE r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'SUTTONS')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
WHERE r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'SUTTONS')
GROUP BY r.process_id, r.message_status;
SELECT 'M113-EXISTS-SUTTONS-48H' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M113-JOIN-SUTTONS-48H · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
GROUP BY r.process_id, r.message_status;
SELECT '=== M113-JOIN-SUTTONS-48H · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
GROUP BY r.process_id, r.message_status;
SELECT 'M113-JOIN-SUTTONS-48H' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M113-EXISTS-SUTTONS-30T · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
WHERE r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'SUTTONS')
GROUP BY r.process_id, r.message_status;
SELECT '=== M113-EXISTS-SUTTONS-30T · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
WHERE r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'SUTTONS')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
WHERE r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'SUTTONS')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
WHERE r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'SUTTONS')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
WHERE r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'SUTTONS')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
WHERE r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'SUTTONS')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
WHERE r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'SUTTONS')
GROUP BY r.process_id, r.message_status;
SELECT 'M113-EXISTS-SUTTONS-30T' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M113-JOIN-SUTTONS-30T · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
GROUP BY r.process_id, r.message_status;
SELECT '=== M113-JOIN-SUTTONS-30T · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.tag >= '2025-12-01' AND r.tag < '2025-12-31'
GROUP BY r.process_id, r.message_status;
SELECT 'M113-JOIN-SUTTONS-30T' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M113-EXISTS-SUTTONS-12M · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
WHERE r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'SUTTONS')
GROUP BY r.process_id, r.message_status;
SELECT '=== M113-EXISTS-SUTTONS-12M · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
WHERE r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'SUTTONS')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
WHERE r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'SUTTONS')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
WHERE r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'SUTTONS')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
WHERE r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'SUTTONS')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
WHERE r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'SUTTONS')
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
WHERE r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = r.process_id AND pm.MandantID = 'SUTTONS')
GROUP BY r.process_id, r.message_status;
SELECT 'M113-EXISTS-SUTTONS-12M' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== M113-JOIN-SUTTONS-12M · Plan (Regel L15) ===' AS marke;
EXPLAIN SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
GROUP BY r.process_id, r.message_status;
SELECT '=== M113-JOIN-SUTTONS-12M · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
GROUP BY r.process_id, r.message_status;
SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
GROUP BY r.process_id, r.message_status;
SELECT 'M113-JOIN-SUTTONS-12M' AS fall,
       COUNT(*)                          AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

-- ####################  Die Gegenprobe zur Vervielfachung  ####################
-- Sagt der JOIN dieselbe Summe wie das EXISTS? Auf DIESEM Bestand ja, weil
-- kein Projekt an mehreren Mandanten haengt (M74a). Die Probe steht hier,
-- damit die Zahl dieses Bestands nicht mit einer Zusicherung des Schemas
-- verwechselt wird.
SELECT '=== M113-Z Summenprobe EXISTS gegen JOIN, 12 Monate ===' AS marke;
SELECT m.mandant, e.summe AS aus_exists, j.summe AS aus_join, (e.summe = j.summe) AS gleich
FROM (SELECT 'NEXANS' AS mandant UNION ALL SELECT 'VOTG' UNION ALL
      SELECT 'IBIS'   UNION ALL SELECT 'SUTTONS') m
JOIN LATERAL (SELECT SUM(r.anzahl) AS summe
              FROM overlord_monitor.message_rollup_monat r
              WHERE r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
                            WHERE p.ProcessID = r.process_id
                              AND pm.MandantID = m.mandant)) e ON TRUE
JOIN LATERAL (SELECT SUM(r.anzahl) AS summe
              FROM overlord_monitor.message_rollup_monat r
              JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE pm.MandantID = m.mandant
                AND r.monat >= '2025-01-01' AND r.monat < '2026-01-01') j ON TRUE;

SELECT '=== 99 fertig ===' AS marke;
