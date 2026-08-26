-- Messrunde M86-M92 — Sitzung 5b: M89, die Leseabfrage des Dashboards
--
-- Benutzer: monitor_read. Ausschliesslich SELECT / SET / EXPLAIN. (Regel S1)
-- Gemessen wird gegen die Probetabelle aus Sitzung 5a (335.610 Zeilen, 18,59 MiB).
--
-- Die Abfrage ist die des Auftrags, woertlich: "Rollup ueber Fenster D, gejoint auf die
-- Mandantenkette (Process -> Project -> ProjectMandant) und auf process_catalog fuer
-- Partner und Richtung, gruppiert nach Stunde und Rohstatus."
--
-- LEFT JOIN auf process_catalog und nicht JOIN: der Katalog deckt nur 1.160 der 1.503
-- Prozesse (V2). Ein innerer Join verloere 343 Prozesse stillschweigend.
--
-- ZWEI FENSTER je Mandant, aus demselben Grund wie in M88:
--   D   Fenster D, 48 h bis zum Anker aus V5 — traegt 256 Nachrichten auf zwei Prozessen
--   D2  dichte 48 h, 2025-12-28 bis 2025-12-30 — traegt 12.332 Nachrichten
-- Ohne D2 waere M89 die Messung eines leeren Fensters.
--
-- Z1: alle Grenzen als Literal.   L7: NEXANS und SUTTONS.
-- G1: die Abfrage gibt Stunde, Rohstatus und Anzahl aus — keine ProcessID, keinen Partner.

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;
SELECT USER() AS benutzer;

SET max_statement_time = 60;
SET profiling_history_size = 100;
SET profiling = 1;

SELECT '=== NEXANS-D-0 Aufwaermlauf ===' AS marke;
SELECT r.stunde, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_probe r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2026-07-06 17:00:00'
  AND r.stunde <  '2026-07-08 18:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;

SELECT '=== NEXANS-D-1 ===' AS marke;
SELECT r.stunde, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_probe r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2026-07-06 17:00:00'
  AND r.stunde <  '2026-07-08 18:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;

SELECT '=== NEXANS-D-2 ===' AS marke;
SELECT r.stunde, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_probe r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2026-07-06 17:00:00'
  AND r.stunde <  '2026-07-08 18:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;

SELECT '=== NEXANS-D-3 ===' AS marke;
SELECT r.stunde, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_probe r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2026-07-06 17:00:00'
  AND r.stunde <  '2026-07-08 18:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;

SELECT '=== NEXANS-D-4 ===' AS marke;
SELECT r.stunde, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_probe r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2026-07-06 17:00:00'
  AND r.stunde <  '2026-07-08 18:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;

SELECT '=== NEXANS-D-5 ===' AS marke;
SELECT r.stunde, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_probe r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2026-07-06 17:00:00'
  AND r.stunde <  '2026-07-08 18:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;

SELECT '=== SUTTONS-D-0 Aufwaermlauf ===' AS marke;
SELECT r.stunde, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_probe r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2026-07-06 17:00:00'
  AND r.stunde <  '2026-07-08 18:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;

SELECT '=== SUTTONS-D-1 ===' AS marke;
SELECT r.stunde, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_probe r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2026-07-06 17:00:00'
  AND r.stunde <  '2026-07-08 18:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;

SELECT '=== SUTTONS-D-2 ===' AS marke;
SELECT r.stunde, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_probe r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2026-07-06 17:00:00'
  AND r.stunde <  '2026-07-08 18:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;

SELECT '=== SUTTONS-D-3 ===' AS marke;
SELECT r.stunde, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_probe r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2026-07-06 17:00:00'
  AND r.stunde <  '2026-07-08 18:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;

SELECT '=== SUTTONS-D-4 ===' AS marke;
SELECT r.stunde, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_probe r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2026-07-06 17:00:00'
  AND r.stunde <  '2026-07-08 18:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;

SELECT '=== SUTTONS-D-5 ===' AS marke;
SELECT r.stunde, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_probe r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2026-07-06 17:00:00'
  AND r.stunde <  '2026-07-08 18:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;

SELECT '=== NEXANS-D2-0 Aufwaermlauf ===' AS marke;
SELECT r.stunde, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_probe r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2025-12-28 00:00:00'
  AND r.stunde <  '2025-12-30 00:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;

SELECT '=== NEXANS-D2-1 ===' AS marke;
SELECT r.stunde, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_probe r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2025-12-28 00:00:00'
  AND r.stunde <  '2025-12-30 00:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;

SELECT '=== NEXANS-D2-2 ===' AS marke;
SELECT r.stunde, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_probe r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2025-12-28 00:00:00'
  AND r.stunde <  '2025-12-30 00:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;

SELECT '=== NEXANS-D2-3 ===' AS marke;
SELECT r.stunde, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_probe r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2025-12-28 00:00:00'
  AND r.stunde <  '2025-12-30 00:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;

SELECT '=== NEXANS-D2-4 ===' AS marke;
SELECT r.stunde, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_probe r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2025-12-28 00:00:00'
  AND r.stunde <  '2025-12-30 00:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;

SELECT '=== NEXANS-D2-5 ===' AS marke;
SELECT r.stunde, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_probe r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2025-12-28 00:00:00'
  AND r.stunde <  '2025-12-30 00:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;

SELECT '=== SUTTONS-D2-0 Aufwaermlauf ===' AS marke;
SELECT r.stunde, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_probe r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-12-28 00:00:00'
  AND r.stunde <  '2025-12-30 00:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;

SELECT '=== SUTTONS-D2-1 ===' AS marke;
SELECT r.stunde, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_probe r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-12-28 00:00:00'
  AND r.stunde <  '2025-12-30 00:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;

SELECT '=== SUTTONS-D2-2 ===' AS marke;
SELECT r.stunde, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_probe r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-12-28 00:00:00'
  AND r.stunde <  '2025-12-30 00:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;

SELECT '=== SUTTONS-D2-3 ===' AS marke;
SELECT r.stunde, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_probe r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-12-28 00:00:00'
  AND r.stunde <  '2025-12-30 00:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;

SELECT '=== SUTTONS-D2-4 ===' AS marke;
SELECT r.stunde, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_probe r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-12-28 00:00:00'
  AND r.stunde <  '2025-12-30 00:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;

SELECT '=== SUTTONS-D2-5 ===' AS marke;
SELECT r.stunde, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_probe r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-12-28 00:00:00'
  AND r.stunde <  '2025-12-30 00:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;

-- ===== L15 — die Plaene, am Ende der Sitzung =====

SELECT '=== EXPLAIN NEXANS-D ===' AS marke;
EXPLAIN
SELECT r.stunde, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_probe r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2026-07-06 17:00:00'
  AND r.stunde <  '2026-07-08 18:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;

SELECT '=== EXPLAIN SUTTONS-D ===' AS marke;
EXPLAIN
SELECT r.stunde, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_probe r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2026-07-06 17:00:00'
  AND r.stunde <  '2026-07-08 18:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;

SELECT '=== EXPLAIN NEXANS-D2 ===' AS marke;
EXPLAIN
SELECT r.stunde, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_probe r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2025-12-28 00:00:00'
  AND r.stunde <  '2025-12-30 00:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;

SELECT '=== EXPLAIN SUTTONS-D2 ===' AS marke;
EXPLAIN
SELECT r.stunde, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_probe r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-12-28 00:00:00'
  AND r.stunde <  '2025-12-30 00:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;

SELECT '=== EXPLAIN FORMAT=JSON NEXANS-D2 (Einstiegstabelle im Volltext) ===' AS marke;
EXPLAIN FORMAT=JSON
SELECT r.stunde, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_probe r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2025-12-28 00:00:00'
  AND r.stunde <  '2025-12-30 00:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;

SELECT '=== Bezugsgroessen: was liefert die Abfrage je Fall? ===' AS marke;
SELECT 'NEXANS-D' AS fall, COUNT(*) AS ergebniszeilen, SUM(anzahl) AS nachrichten FROM (
  SELECT r.stunde, r.message_status, SUM(r.anzahl) AS anzahl
  FROM overlord_monitor.message_rollup_probe r
  JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
  JOIN GlassfishDB.Project pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'NEXANS' AND r.stunde >= '2026-07-06 17:00:00' AND r.stunde < '2026-07-08 18:00:00'
  GROUP BY r.stunde, r.message_status) t
UNION ALL
SELECT 'SUTTONS-D', COUNT(*), SUM(anzahl) FROM (
  SELECT r.stunde, r.message_status, SUM(r.anzahl) AS anzahl
  FROM overlord_monitor.message_rollup_probe r
  JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
  JOIN GlassfishDB.Project pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS' AND r.stunde >= '2026-07-06 17:00:00' AND r.stunde < '2026-07-08 18:00:00'
  GROUP BY r.stunde, r.message_status) t
UNION ALL
SELECT 'NEXANS-D2', COUNT(*), SUM(anzahl) FROM (
  SELECT r.stunde, r.message_status, SUM(r.anzahl) AS anzahl
  FROM overlord_monitor.message_rollup_probe r
  JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
  JOIN GlassfishDB.Project pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'NEXANS' AND r.stunde >= '2025-12-28 00:00:00' AND r.stunde < '2025-12-30 00:00:00'
  GROUP BY r.stunde, r.message_status) t
UNION ALL
SELECT 'SUTTONS-D2', COUNT(*), SUM(anzahl) FROM (
  SELECT r.stunde, r.message_status, SUM(r.anzahl) AS anzahl
  FROM overlord_monitor.message_rollup_probe r
  JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
  JOIN GlassfishDB.Project pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS' AND r.stunde >= '2025-12-28 00:00:00' AND r.stunde < '2025-12-30 00:00:00'
  GROUP BY r.stunde, r.message_status) t;

SELECT '=== 98 SHOW PROFILES ===' AS marke;
SHOW PROFILES;

SELECT '=== 99 Abschluss ===' AS marke;
SELECT @@global.read_only AS read_only_ende, NOW() AS serverzeit_ende;
