-- Messrunde M86-M92 — Sitzung 6: M90 "Ueberfaellig" live, mit Mandantenfilter und Fenster
--
-- Die Abfrage, die E-c A verlangt. Sie ist eine ANDERE als die in docs/message-status.md
-- gemessene: dort fehlten Mandantenfilter und Zeitfenster.
--
-- Z1: Der Anker steht als Literal im Statement — '2026-07-08 17:21:10', der Wert aus V5
--     (MAX(Message.MessageLastUpdate), zugleich der Anker der Anwendungsuhr im Profil dev).
--     Kein NOW().
-- V4: die Spaltennamen der Mandantenkette sind gegen information_schema erhoben, nicht
--     aus einer Projektdatei uebernommen.
-- L7: NEXANS und SUTTONS.
-- L9: Fassung 3 laeuft ohne Zeitfenster ueber den Gesamtbestand. Begruendung: Sie IST die
--     zweite Zahl an der Kachel aus 10b ("insgesamt"), die der Auftrag ausdruecklich
--     erhoben haben will. Kein Vorbild fuer einen Listen-Endpunkt.
--
-- Drei Fassungen je Mandant:
--   1  Fenster D  — was die Kachel nach E-h zeigt   (48 h bis zum Anker)
--   2  Fenster B  — ein Monat
--   3  Fenster G  — was "insgesamt" waere
--
-- Ausschliesslich SELECT / SET / EXPLAIN. (Regel S1)

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;

SET max_statement_time = 60;
SET profiling_history_size = 100;
SET profiling = 1;

SELECT '=== NEX-D-0 ===' AS marke;
SELECT COUNT(*) AS ueberfaellig
FROM GlassfishDB.Message m
WHERE m.MessageStatus IN ('SUSPENDED','RUNNING')
  AND m.MessageTimeout > 0
  AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND < '2026-07-08 17:21:10'
  AND m.MessageLastUpdate >= '2026-07-06 17:21:10'
  AND m.MessageLastUpdate <  '2026-07-08 17:21:10'
  AND EXISTS (
        SELECT 1
        FROM GlassfishDB.Process        p
        JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
        JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
        WHERE p.ProcessID = m.ProcessID
          AND pm.MandantID = 'NEXANS'
      );

SELECT '=== NEX-D-1 ===' AS marke;
SELECT COUNT(*) AS ueberfaellig
FROM GlassfishDB.Message m
WHERE m.MessageStatus IN ('SUSPENDED','RUNNING')
  AND m.MessageTimeout > 0
  AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND < '2026-07-08 17:21:10'
  AND m.MessageLastUpdate >= '2026-07-06 17:21:10'
  AND m.MessageLastUpdate <  '2026-07-08 17:21:10'
  AND EXISTS (
        SELECT 1
        FROM GlassfishDB.Process        p
        JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
        JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
        WHERE p.ProcessID = m.ProcessID
          AND pm.MandantID = 'NEXANS'
      );

SELECT '=== NEX-D-2 ===' AS marke;
SELECT COUNT(*) AS ueberfaellig
FROM GlassfishDB.Message m
WHERE m.MessageStatus IN ('SUSPENDED','RUNNING')
  AND m.MessageTimeout > 0
  AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND < '2026-07-08 17:21:10'
  AND m.MessageLastUpdate >= '2026-07-06 17:21:10'
  AND m.MessageLastUpdate <  '2026-07-08 17:21:10'
  AND EXISTS (
        SELECT 1
        FROM GlassfishDB.Process        p
        JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
        JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
        WHERE p.ProcessID = m.ProcessID
          AND pm.MandantID = 'NEXANS'
      );

SELECT '=== NEX-D-3 ===' AS marke;
SELECT COUNT(*) AS ueberfaellig
FROM GlassfishDB.Message m
WHERE m.MessageStatus IN ('SUSPENDED','RUNNING')
  AND m.MessageTimeout > 0
  AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND < '2026-07-08 17:21:10'
  AND m.MessageLastUpdate >= '2026-07-06 17:21:10'
  AND m.MessageLastUpdate <  '2026-07-08 17:21:10'
  AND EXISTS (
        SELECT 1
        FROM GlassfishDB.Process        p
        JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
        JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
        WHERE p.ProcessID = m.ProcessID
          AND pm.MandantID = 'NEXANS'
      );

SELECT '=== NEX-D-4 ===' AS marke;
SELECT COUNT(*) AS ueberfaellig
FROM GlassfishDB.Message m
WHERE m.MessageStatus IN ('SUSPENDED','RUNNING')
  AND m.MessageTimeout > 0
  AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND < '2026-07-08 17:21:10'
  AND m.MessageLastUpdate >= '2026-07-06 17:21:10'
  AND m.MessageLastUpdate <  '2026-07-08 17:21:10'
  AND EXISTS (
        SELECT 1
        FROM GlassfishDB.Process        p
        JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
        JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
        WHERE p.ProcessID = m.ProcessID
          AND pm.MandantID = 'NEXANS'
      );

SELECT '=== NEX-D-5 ===' AS marke;
SELECT COUNT(*) AS ueberfaellig
FROM GlassfishDB.Message m
WHERE m.MessageStatus IN ('SUSPENDED','RUNNING')
  AND m.MessageTimeout > 0
  AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND < '2026-07-08 17:21:10'
  AND m.MessageLastUpdate >= '2026-07-06 17:21:10'
  AND m.MessageLastUpdate <  '2026-07-08 17:21:10'
  AND EXISTS (
        SELECT 1
        FROM GlassfishDB.Process        p
        JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
        JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
        WHERE p.ProcessID = m.ProcessID
          AND pm.MandantID = 'NEXANS'
      );

SELECT '=== SUT-D-0 ===' AS marke;
SELECT COUNT(*) AS ueberfaellig
FROM GlassfishDB.Message m
WHERE m.MessageStatus IN ('SUSPENDED','RUNNING')
  AND m.MessageTimeout > 0
  AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND < '2026-07-08 17:21:10'
  AND m.MessageLastUpdate >= '2026-07-06 17:21:10'
  AND m.MessageLastUpdate <  '2026-07-08 17:21:10'
  AND EXISTS (
        SELECT 1
        FROM GlassfishDB.Process        p
        JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
        JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
        WHERE p.ProcessID = m.ProcessID
          AND pm.MandantID = 'SUTTONS'
      );

SELECT '=== SUT-D-1 ===' AS marke;
SELECT COUNT(*) AS ueberfaellig
FROM GlassfishDB.Message m
WHERE m.MessageStatus IN ('SUSPENDED','RUNNING')
  AND m.MessageTimeout > 0
  AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND < '2026-07-08 17:21:10'
  AND m.MessageLastUpdate >= '2026-07-06 17:21:10'
  AND m.MessageLastUpdate <  '2026-07-08 17:21:10'
  AND EXISTS (
        SELECT 1
        FROM GlassfishDB.Process        p
        JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
        JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
        WHERE p.ProcessID = m.ProcessID
          AND pm.MandantID = 'SUTTONS'
      );

SELECT '=== SUT-D-2 ===' AS marke;
SELECT COUNT(*) AS ueberfaellig
FROM GlassfishDB.Message m
WHERE m.MessageStatus IN ('SUSPENDED','RUNNING')
  AND m.MessageTimeout > 0
  AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND < '2026-07-08 17:21:10'
  AND m.MessageLastUpdate >= '2026-07-06 17:21:10'
  AND m.MessageLastUpdate <  '2026-07-08 17:21:10'
  AND EXISTS (
        SELECT 1
        FROM GlassfishDB.Process        p
        JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
        JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
        WHERE p.ProcessID = m.ProcessID
          AND pm.MandantID = 'SUTTONS'
      );

SELECT '=== SUT-D-3 ===' AS marke;
SELECT COUNT(*) AS ueberfaellig
FROM GlassfishDB.Message m
WHERE m.MessageStatus IN ('SUSPENDED','RUNNING')
  AND m.MessageTimeout > 0
  AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND < '2026-07-08 17:21:10'
  AND m.MessageLastUpdate >= '2026-07-06 17:21:10'
  AND m.MessageLastUpdate <  '2026-07-08 17:21:10'
  AND EXISTS (
        SELECT 1
        FROM GlassfishDB.Process        p
        JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
        JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
        WHERE p.ProcessID = m.ProcessID
          AND pm.MandantID = 'SUTTONS'
      );

SELECT '=== SUT-D-4 ===' AS marke;
SELECT COUNT(*) AS ueberfaellig
FROM GlassfishDB.Message m
WHERE m.MessageStatus IN ('SUSPENDED','RUNNING')
  AND m.MessageTimeout > 0
  AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND < '2026-07-08 17:21:10'
  AND m.MessageLastUpdate >= '2026-07-06 17:21:10'
  AND m.MessageLastUpdate <  '2026-07-08 17:21:10'
  AND EXISTS (
        SELECT 1
        FROM GlassfishDB.Process        p
        JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
        JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
        WHERE p.ProcessID = m.ProcessID
          AND pm.MandantID = 'SUTTONS'
      );

SELECT '=== SUT-D-5 ===' AS marke;
SELECT COUNT(*) AS ueberfaellig
FROM GlassfishDB.Message m
WHERE m.MessageStatus IN ('SUSPENDED','RUNNING')
  AND m.MessageTimeout > 0
  AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND < '2026-07-08 17:21:10'
  AND m.MessageLastUpdate >= '2026-07-06 17:21:10'
  AND m.MessageLastUpdate <  '2026-07-08 17:21:10'
  AND EXISTS (
        SELECT 1
        FROM GlassfishDB.Process        p
        JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
        JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
        WHERE p.ProcessID = m.ProcessID
          AND pm.MandantID = 'SUTTONS'
      );

SELECT '=== NEX-B-0 ===' AS marke;
SELECT COUNT(*) AS ueberfaellig
FROM GlassfishDB.Message m
WHERE m.MessageStatus IN ('SUSPENDED','RUNNING')
  AND m.MessageTimeout > 0
  AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND < '2026-07-08 17:21:10'
  AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  AND EXISTS (
        SELECT 1
        FROM GlassfishDB.Process        p
        JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
        JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
        WHERE p.ProcessID = m.ProcessID
          AND pm.MandantID = 'NEXANS'
      );

SELECT '=== NEX-B-1 ===' AS marke;
SELECT COUNT(*) AS ueberfaellig
FROM GlassfishDB.Message m
WHERE m.MessageStatus IN ('SUSPENDED','RUNNING')
  AND m.MessageTimeout > 0
  AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND < '2026-07-08 17:21:10'
  AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  AND EXISTS (
        SELECT 1
        FROM GlassfishDB.Process        p
        JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
        JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
        WHERE p.ProcessID = m.ProcessID
          AND pm.MandantID = 'NEXANS'
      );

SELECT '=== NEX-B-2 ===' AS marke;
SELECT COUNT(*) AS ueberfaellig
FROM GlassfishDB.Message m
WHERE m.MessageStatus IN ('SUSPENDED','RUNNING')
  AND m.MessageTimeout > 0
  AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND < '2026-07-08 17:21:10'
  AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  AND EXISTS (
        SELECT 1
        FROM GlassfishDB.Process        p
        JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
        JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
        WHERE p.ProcessID = m.ProcessID
          AND pm.MandantID = 'NEXANS'
      );

SELECT '=== NEX-B-3 ===' AS marke;
SELECT COUNT(*) AS ueberfaellig
FROM GlassfishDB.Message m
WHERE m.MessageStatus IN ('SUSPENDED','RUNNING')
  AND m.MessageTimeout > 0
  AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND < '2026-07-08 17:21:10'
  AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  AND EXISTS (
        SELECT 1
        FROM GlassfishDB.Process        p
        JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
        JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
        WHERE p.ProcessID = m.ProcessID
          AND pm.MandantID = 'NEXANS'
      );

SELECT '=== NEX-B-4 ===' AS marke;
SELECT COUNT(*) AS ueberfaellig
FROM GlassfishDB.Message m
WHERE m.MessageStatus IN ('SUSPENDED','RUNNING')
  AND m.MessageTimeout > 0
  AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND < '2026-07-08 17:21:10'
  AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  AND EXISTS (
        SELECT 1
        FROM GlassfishDB.Process        p
        JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
        JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
        WHERE p.ProcessID = m.ProcessID
          AND pm.MandantID = 'NEXANS'
      );

SELECT '=== NEX-B-5 ===' AS marke;
SELECT COUNT(*) AS ueberfaellig
FROM GlassfishDB.Message m
WHERE m.MessageStatus IN ('SUSPENDED','RUNNING')
  AND m.MessageTimeout > 0
  AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND < '2026-07-08 17:21:10'
  AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  AND EXISTS (
        SELECT 1
        FROM GlassfishDB.Process        p
        JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
        JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
        WHERE p.ProcessID = m.ProcessID
          AND pm.MandantID = 'NEXANS'
      );

SELECT '=== SUT-B-0 ===' AS marke;
SELECT COUNT(*) AS ueberfaellig
FROM GlassfishDB.Message m
WHERE m.MessageStatus IN ('SUSPENDED','RUNNING')
  AND m.MessageTimeout > 0
  AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND < '2026-07-08 17:21:10'
  AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  AND EXISTS (
        SELECT 1
        FROM GlassfishDB.Process        p
        JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
        JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
        WHERE p.ProcessID = m.ProcessID
          AND pm.MandantID = 'SUTTONS'
      );

SELECT '=== SUT-B-1 ===' AS marke;
SELECT COUNT(*) AS ueberfaellig
FROM GlassfishDB.Message m
WHERE m.MessageStatus IN ('SUSPENDED','RUNNING')
  AND m.MessageTimeout > 0
  AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND < '2026-07-08 17:21:10'
  AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  AND EXISTS (
        SELECT 1
        FROM GlassfishDB.Process        p
        JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
        JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
        WHERE p.ProcessID = m.ProcessID
          AND pm.MandantID = 'SUTTONS'
      );

SELECT '=== SUT-B-2 ===' AS marke;
SELECT COUNT(*) AS ueberfaellig
FROM GlassfishDB.Message m
WHERE m.MessageStatus IN ('SUSPENDED','RUNNING')
  AND m.MessageTimeout > 0
  AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND < '2026-07-08 17:21:10'
  AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  AND EXISTS (
        SELECT 1
        FROM GlassfishDB.Process        p
        JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
        JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
        WHERE p.ProcessID = m.ProcessID
          AND pm.MandantID = 'SUTTONS'
      );

SELECT '=== SUT-B-3 ===' AS marke;
SELECT COUNT(*) AS ueberfaellig
FROM GlassfishDB.Message m
WHERE m.MessageStatus IN ('SUSPENDED','RUNNING')
  AND m.MessageTimeout > 0
  AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND < '2026-07-08 17:21:10'
  AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  AND EXISTS (
        SELECT 1
        FROM GlassfishDB.Process        p
        JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
        JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
        WHERE p.ProcessID = m.ProcessID
          AND pm.MandantID = 'SUTTONS'
      );

SELECT '=== SUT-B-4 ===' AS marke;
SELECT COUNT(*) AS ueberfaellig
FROM GlassfishDB.Message m
WHERE m.MessageStatus IN ('SUSPENDED','RUNNING')
  AND m.MessageTimeout > 0
  AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND < '2026-07-08 17:21:10'
  AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  AND EXISTS (
        SELECT 1
        FROM GlassfishDB.Process        p
        JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
        JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
        WHERE p.ProcessID = m.ProcessID
          AND pm.MandantID = 'SUTTONS'
      );

SELECT '=== SUT-B-5 ===' AS marke;
SELECT COUNT(*) AS ueberfaellig
FROM GlassfishDB.Message m
WHERE m.MessageStatus IN ('SUSPENDED','RUNNING')
  AND m.MessageTimeout > 0
  AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND < '2026-07-08 17:21:10'
  AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  AND EXISTS (
        SELECT 1
        FROM GlassfishDB.Process        p
        JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
        JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
        WHERE p.ProcessID = m.ProcessID
          AND pm.MandantID = 'SUTTONS'
      );

SELECT '=== NEX-G-0 ===' AS marke;
SELECT COUNT(*) AS ueberfaellig
FROM GlassfishDB.Message m
WHERE m.MessageStatus IN ('SUSPENDED','RUNNING')
  AND m.MessageTimeout > 0
  AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND < '2026-07-08 17:21:10'
  AND EXISTS (
        SELECT 1
        FROM GlassfishDB.Process        p
        JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
        JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
        WHERE p.ProcessID = m.ProcessID
          AND pm.MandantID = 'NEXANS'
      );

SELECT '=== NEX-G-1 ===' AS marke;
SELECT COUNT(*) AS ueberfaellig
FROM GlassfishDB.Message m
WHERE m.MessageStatus IN ('SUSPENDED','RUNNING')
  AND m.MessageTimeout > 0
  AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND < '2026-07-08 17:21:10'
  AND EXISTS (
        SELECT 1
        FROM GlassfishDB.Process        p
        JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
        JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
        WHERE p.ProcessID = m.ProcessID
          AND pm.MandantID = 'NEXANS'
      );

SELECT '=== NEX-G-2 ===' AS marke;
SELECT COUNT(*) AS ueberfaellig
FROM GlassfishDB.Message m
WHERE m.MessageStatus IN ('SUSPENDED','RUNNING')
  AND m.MessageTimeout > 0
  AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND < '2026-07-08 17:21:10'
  AND EXISTS (
        SELECT 1
        FROM GlassfishDB.Process        p
        JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
        JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
        WHERE p.ProcessID = m.ProcessID
          AND pm.MandantID = 'NEXANS'
      );

SELECT '=== NEX-G-3 ===' AS marke;
SELECT COUNT(*) AS ueberfaellig
FROM GlassfishDB.Message m
WHERE m.MessageStatus IN ('SUSPENDED','RUNNING')
  AND m.MessageTimeout > 0
  AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND < '2026-07-08 17:21:10'
  AND EXISTS (
        SELECT 1
        FROM GlassfishDB.Process        p
        JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
        JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
        WHERE p.ProcessID = m.ProcessID
          AND pm.MandantID = 'NEXANS'
      );

SELECT '=== NEX-G-4 ===' AS marke;
SELECT COUNT(*) AS ueberfaellig
FROM GlassfishDB.Message m
WHERE m.MessageStatus IN ('SUSPENDED','RUNNING')
  AND m.MessageTimeout > 0
  AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND < '2026-07-08 17:21:10'
  AND EXISTS (
        SELECT 1
        FROM GlassfishDB.Process        p
        JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
        JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
        WHERE p.ProcessID = m.ProcessID
          AND pm.MandantID = 'NEXANS'
      );

SELECT '=== NEX-G-5 ===' AS marke;
SELECT COUNT(*) AS ueberfaellig
FROM GlassfishDB.Message m
WHERE m.MessageStatus IN ('SUSPENDED','RUNNING')
  AND m.MessageTimeout > 0
  AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND < '2026-07-08 17:21:10'
  AND EXISTS (
        SELECT 1
        FROM GlassfishDB.Process        p
        JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
        JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
        WHERE p.ProcessID = m.ProcessID
          AND pm.MandantID = 'NEXANS'
      );

SELECT '=== SUT-G-0 ===' AS marke;
SELECT COUNT(*) AS ueberfaellig
FROM GlassfishDB.Message m
WHERE m.MessageStatus IN ('SUSPENDED','RUNNING')
  AND m.MessageTimeout > 0
  AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND < '2026-07-08 17:21:10'
  AND EXISTS (
        SELECT 1
        FROM GlassfishDB.Process        p
        JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
        JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
        WHERE p.ProcessID = m.ProcessID
          AND pm.MandantID = 'SUTTONS'
      );

SELECT '=== SUT-G-1 ===' AS marke;
SELECT COUNT(*) AS ueberfaellig
FROM GlassfishDB.Message m
WHERE m.MessageStatus IN ('SUSPENDED','RUNNING')
  AND m.MessageTimeout > 0
  AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND < '2026-07-08 17:21:10'
  AND EXISTS (
        SELECT 1
        FROM GlassfishDB.Process        p
        JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
        JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
        WHERE p.ProcessID = m.ProcessID
          AND pm.MandantID = 'SUTTONS'
      );

SELECT '=== SUT-G-2 ===' AS marke;
SELECT COUNT(*) AS ueberfaellig
FROM GlassfishDB.Message m
WHERE m.MessageStatus IN ('SUSPENDED','RUNNING')
  AND m.MessageTimeout > 0
  AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND < '2026-07-08 17:21:10'
  AND EXISTS (
        SELECT 1
        FROM GlassfishDB.Process        p
        JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
        JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
        WHERE p.ProcessID = m.ProcessID
          AND pm.MandantID = 'SUTTONS'
      );

SELECT '=== SUT-G-3 ===' AS marke;
SELECT COUNT(*) AS ueberfaellig
FROM GlassfishDB.Message m
WHERE m.MessageStatus IN ('SUSPENDED','RUNNING')
  AND m.MessageTimeout > 0
  AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND < '2026-07-08 17:21:10'
  AND EXISTS (
        SELECT 1
        FROM GlassfishDB.Process        p
        JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
        JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
        WHERE p.ProcessID = m.ProcessID
          AND pm.MandantID = 'SUTTONS'
      );

SELECT '=== SUT-G-4 ===' AS marke;
SELECT COUNT(*) AS ueberfaellig
FROM GlassfishDB.Message m
WHERE m.MessageStatus IN ('SUSPENDED','RUNNING')
  AND m.MessageTimeout > 0
  AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND < '2026-07-08 17:21:10'
  AND EXISTS (
        SELECT 1
        FROM GlassfishDB.Process        p
        JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
        JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
        WHERE p.ProcessID = m.ProcessID
          AND pm.MandantID = 'SUTTONS'
      );

SELECT '=== SUT-G-5 ===' AS marke;
SELECT COUNT(*) AS ueberfaellig
FROM GlassfishDB.Message m
WHERE m.MessageStatus IN ('SUSPENDED','RUNNING')
  AND m.MessageTimeout > 0
  AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND < '2026-07-08 17:21:10'
  AND EXISTS (
        SELECT 1
        FROM GlassfishDB.Process        p
        JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
        JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
        WHERE p.ProcessID = m.ProcessID
          AND pm.MandantID = 'SUTTONS'
      );

-- ===== L15 =====

SELECT '=== EXPLAIN NEX-D ===' AS marke;
EXPLAIN
SELECT COUNT(*) AS ueberfaellig
FROM GlassfishDB.Message m
WHERE m.MessageStatus IN ('SUSPENDED','RUNNING')
  AND m.MessageTimeout > 0
  AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND < '2026-07-08 17:21:10'
  AND m.MessageLastUpdate >= '2026-07-06 17:21:10'
  AND m.MessageLastUpdate <  '2026-07-08 17:21:10'
  AND EXISTS (
        SELECT 1
        FROM GlassfishDB.Process        p
        JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
        JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
        WHERE p.ProcessID = m.ProcessID
          AND pm.MandantID = 'NEXANS'
      );

SELECT '=== EXPLAIN SUT-D ===' AS marke;
EXPLAIN
SELECT COUNT(*) AS ueberfaellig
FROM GlassfishDB.Message m
WHERE m.MessageStatus IN ('SUSPENDED','RUNNING')
  AND m.MessageTimeout > 0
  AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND < '2026-07-08 17:21:10'
  AND m.MessageLastUpdate >= '2026-07-06 17:21:10'
  AND m.MessageLastUpdate <  '2026-07-08 17:21:10'
  AND EXISTS (
        SELECT 1
        FROM GlassfishDB.Process        p
        JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
        JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
        WHERE p.ProcessID = m.ProcessID
          AND pm.MandantID = 'SUTTONS'
      );

SELECT '=== EXPLAIN NEX-B ===' AS marke;
EXPLAIN
SELECT COUNT(*) AS ueberfaellig
FROM GlassfishDB.Message m
WHERE m.MessageStatus IN ('SUSPENDED','RUNNING')
  AND m.MessageTimeout > 0
  AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND < '2026-07-08 17:21:10'
  AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  AND EXISTS (
        SELECT 1
        FROM GlassfishDB.Process        p
        JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
        JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
        WHERE p.ProcessID = m.ProcessID
          AND pm.MandantID = 'NEXANS'
      );

SELECT '=== EXPLAIN SUT-B ===' AS marke;
EXPLAIN
SELECT COUNT(*) AS ueberfaellig
FROM GlassfishDB.Message m
WHERE m.MessageStatus IN ('SUSPENDED','RUNNING')
  AND m.MessageTimeout > 0
  AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND < '2026-07-08 17:21:10'
  AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  AND EXISTS (
        SELECT 1
        FROM GlassfishDB.Process        p
        JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
        JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
        WHERE p.ProcessID = m.ProcessID
          AND pm.MandantID = 'SUTTONS'
      );

SELECT '=== EXPLAIN NEX-G ===' AS marke;
EXPLAIN
SELECT COUNT(*) AS ueberfaellig
FROM GlassfishDB.Message m
WHERE m.MessageStatus IN ('SUSPENDED','RUNNING')
  AND m.MessageTimeout > 0
  AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND < '2026-07-08 17:21:10'
  AND EXISTS (
        SELECT 1
        FROM GlassfishDB.Process        p
        JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
        JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
        WHERE p.ProcessID = m.ProcessID
          AND pm.MandantID = 'NEXANS'
      );

SELECT '=== EXPLAIN SUT-G ===' AS marke;
EXPLAIN
SELECT COUNT(*) AS ueberfaellig
FROM GlassfishDB.Message m
WHERE m.MessageStatus IN ('SUSPENDED','RUNNING')
  AND m.MessageTimeout > 0
  AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND < '2026-07-08 17:21:10'
  AND EXISTS (
        SELECT 1
        FROM GlassfishDB.Process        p
        JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
        JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
        WHERE p.ProcessID = m.ProcessID
          AND pm.MandantID = 'SUTTONS'
      );

SELECT '=== M90-Z Die reinen Zahlen nebeneinander ===' AS marke;
SELECT 'offen gesamt (ohne Mandant, ohne Fenster)' AS groesse, COUNT(*) AS zeilen
FROM GlassfishDB.Message WHERE MessageStatus IN ('SUSPENDED','RUNNING')
UNION ALL SELECT 'davon MessageTimeout > 0', COUNT(*)
FROM GlassfishDB.Message WHERE MessageStatus IN ('SUSPENDED','RUNNING') AND MessageTimeout > 0
UNION ALL SELECT 'davon ueberfaellig gegen den Anker', COUNT(*)
FROM GlassfishDB.Message WHERE MessageStatus IN ('SUSPENDED','RUNNING') AND MessageTimeout > 0
  AND MessageLastUpdate + INTERVAL MessageTimeout SECOND < '2026-07-08 17:21:10'
UNION ALL SELECT 'RUNNING (Gegenprobe, muss 0 sein)', COUNT(*)
FROM GlassfishDB.Message WHERE MessageStatus = 'RUNNING'
UNION ALL SELECT 'MessageTimeout IS NULL (Gegenprobe)', COUNT(*)
FROM GlassfishDB.Message WHERE MessageTimeout IS NULL;

SELECT '=== M90-Z2 Die offenen Zeilen: wann enden sie, wem gehoeren sie? ===' AS marke;
SELECT pm.MandantID AS mandant, COUNT(*) AS offene_zeilen,
       MIN(m.MessageLastUpdate) AS frueheste, MAX(m.MessageLastUpdate) AS spaeteste
FROM GlassfishDB.Message m
JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE m.MessageStatus IN ('SUSPENDED','RUNNING')
GROUP BY pm.MandantID ORDER BY offene_zeilen DESC;

SELECT '=== 98 SHOW PROFILES ===' AS marke;
SHOW PROFILES;
SELECT '=== 99 Abschluss ===' AS marke;
SELECT @@global.read_only AS read_only_ende, NOW() AS serverzeit_ende;
