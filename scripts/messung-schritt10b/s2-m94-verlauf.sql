-- Messrunde M94-M98 — Sitzung 2: M94 Verlauf, drei Paare, zwei Mandanten (L7)
-- Auftrag:  "Messrunde vor Schritt 10b", Stand 27.08.2026
-- Ergebnis: docs/messungen-schritt10b.md
--
-- Ausschliesslich SELECT / SET / EXPLAIN. (Regel S1)
-- G1: keine ProcessID, kein ProcessName, kein Partnername in dieser Datei.
--
-- Z1: Jeder Zeitpunkt steht als Literal. Sie sind aus dem Anker der
--     Anwendungsuhr hergeleitet (V5, docs/datenzugriff.md §6):
--         Anker                       2025-12-30 04:09:47
--         48 h / Stunde   48 Eimer    2025-12-28 05:00:00  bis  2025-12-30 05:00:00
--         30 Tage / Tag   30 Eimer    2025-12-01 00:00:00  bis  2025-12-31 00:00:00
--         12 Monate/Monat 12 Eimer    2025-01-01 00:00:00  bis  2026-01-01 00:00:00
--     Die obere Grenze ist jeweils der Anfang des naechsten Eimers, wie in
--     RollupFenster.ausgedehnt — der angebrochene Eimer gehoert dazu.
--
-- Die Verlaufsabfrage braucht process_catalog NICHT: die Kurve kennt keinen Partner.
-- GROUP BY steht mit dem vollen Ausdruck und nicht ueber den Alias (Befund 11).

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;

SET max_statement_time = 60;
SET profiling = 1;
SET profiling_history_size = 100;


-- ══ M94 Verlauf · 48 h / Stunde · NEXANS ══════════════════════════════════
SELECT '=== M94-V P1-NEXANS Plan ===' AS marke;
EXPLAIN SELECT r.stunde AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2025-12-28 05:00:00'
  AND r.stunde <  '2025-12-30 05:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;

SELECT '=== M94-V P1-NEXANS gelesene Rollupzeilen, gezaehlt ===' AS marke;
SELECT COUNT(*) AS rollupzeilen, SUM(r.anzahl) AS nachrichten,
       COUNT(DISTINCT r.process_id) AS versch_prozesse,
       COUNT(DISTINCT r.stunde) AS belegte_eimer
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2025-12-28 05:00:00'
  AND r.stunde <  '2025-12-30 05:00:00';

SELECT '=== M94-V P1-NEXANS Ergebnis (erste Zeilen) ===' AS marke;
SELECT r.stunde AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2025-12-28 05:00:00'
  AND r.stunde <  '2025-12-30 05:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status
LIMIT 5;

SELECT '=== M94-V P1-NEXANS Laufzeit: Aufwaermlauf + fuenf ===' AS marke;
SELECT r.stunde AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2025-12-28 05:00:00'
  AND r.stunde <  '2025-12-30 05:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;
SELECT r.stunde AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2025-12-28 05:00:00'
  AND r.stunde <  '2025-12-30 05:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;
SELECT r.stunde AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2025-12-28 05:00:00'
  AND r.stunde <  '2025-12-30 05:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;
SELECT r.stunde AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2025-12-28 05:00:00'
  AND r.stunde <  '2025-12-30 05:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;
SELECT r.stunde AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2025-12-28 05:00:00'
  AND r.stunde <  '2025-12-30 05:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;
SELECT r.stunde AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2025-12-28 05:00:00'
  AND r.stunde <  '2025-12-30 05:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;

-- ══ M94 Verlauf · 30 Tage / Tag · NEXANS ══════════════════════════════════
SELECT '=== M94-V P2-NEXANS Plan ===' AS marke;
EXPLAIN SELECT DATE(r.stunde) AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2025-12-01 00:00:00'
  AND r.stunde <  '2025-12-31 00:00:00'
GROUP BY DATE(r.stunde), r.message_status
ORDER BY DATE(r.stunde), r.message_status;

SELECT '=== M94-V P2-NEXANS gelesene Rollupzeilen, gezaehlt ===' AS marke;
SELECT COUNT(*) AS rollupzeilen, SUM(r.anzahl) AS nachrichten,
       COUNT(DISTINCT r.process_id) AS versch_prozesse,
       COUNT(DISTINCT DATE(r.stunde)) AS belegte_eimer
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2025-12-01 00:00:00'
  AND r.stunde <  '2025-12-31 00:00:00';

SELECT '=== M94-V P2-NEXANS Ergebnis (erste Zeilen) ===' AS marke;
SELECT DATE(r.stunde) AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2025-12-01 00:00:00'
  AND r.stunde <  '2025-12-31 00:00:00'
GROUP BY DATE(r.stunde), r.message_status
ORDER BY DATE(r.stunde), r.message_status
LIMIT 5;

SELECT '=== M94-V P2-NEXANS Laufzeit: Aufwaermlauf + fuenf ===' AS marke;
SELECT DATE(r.stunde) AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2025-12-01 00:00:00'
  AND r.stunde <  '2025-12-31 00:00:00'
GROUP BY DATE(r.stunde), r.message_status
ORDER BY DATE(r.stunde), r.message_status;
SELECT DATE(r.stunde) AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2025-12-01 00:00:00'
  AND r.stunde <  '2025-12-31 00:00:00'
GROUP BY DATE(r.stunde), r.message_status
ORDER BY DATE(r.stunde), r.message_status;
SELECT DATE(r.stunde) AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2025-12-01 00:00:00'
  AND r.stunde <  '2025-12-31 00:00:00'
GROUP BY DATE(r.stunde), r.message_status
ORDER BY DATE(r.stunde), r.message_status;
SELECT DATE(r.stunde) AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2025-12-01 00:00:00'
  AND r.stunde <  '2025-12-31 00:00:00'
GROUP BY DATE(r.stunde), r.message_status
ORDER BY DATE(r.stunde), r.message_status;
SELECT DATE(r.stunde) AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2025-12-01 00:00:00'
  AND r.stunde <  '2025-12-31 00:00:00'
GROUP BY DATE(r.stunde), r.message_status
ORDER BY DATE(r.stunde), r.message_status;
SELECT DATE(r.stunde) AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2025-12-01 00:00:00'
  AND r.stunde <  '2025-12-31 00:00:00'
GROUP BY DATE(r.stunde), r.message_status
ORDER BY DATE(r.stunde), r.message_status;

-- ══ M94 Verlauf · 12 Monate / Monat · NEXANS ══════════════════════════════════
SELECT '=== M94-V P3-NEXANS Plan ===' AS marke;
EXPLAIN SELECT DATE_FORMAT(r.stunde, '%Y-%m-01') AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2025-01-01 00:00:00'
  AND r.stunde <  '2026-01-01 00:00:00'
GROUP BY DATE_FORMAT(r.stunde, '%Y-%m-01'), r.message_status
ORDER BY DATE_FORMAT(r.stunde, '%Y-%m-01'), r.message_status;

SELECT '=== M94-V P3-NEXANS gelesene Rollupzeilen, gezaehlt ===' AS marke;
SELECT COUNT(*) AS rollupzeilen, SUM(r.anzahl) AS nachrichten,
       COUNT(DISTINCT r.process_id) AS versch_prozesse,
       COUNT(DISTINCT DATE_FORMAT(r.stunde, '%Y-%m-01')) AS belegte_eimer
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2025-01-01 00:00:00'
  AND r.stunde <  '2026-01-01 00:00:00';

SELECT '=== M94-V P3-NEXANS Ergebnis (erste Zeilen) ===' AS marke;
SELECT DATE_FORMAT(r.stunde, '%Y-%m-01') AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2025-01-01 00:00:00'
  AND r.stunde <  '2026-01-01 00:00:00'
GROUP BY DATE_FORMAT(r.stunde, '%Y-%m-01'), r.message_status
ORDER BY DATE_FORMAT(r.stunde, '%Y-%m-01'), r.message_status
LIMIT 5;

SELECT '=== M94-V P3-NEXANS Laufzeit: Aufwaermlauf + fuenf ===' AS marke;
SELECT DATE_FORMAT(r.stunde, '%Y-%m-01') AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2025-01-01 00:00:00'
  AND r.stunde <  '2026-01-01 00:00:00'
GROUP BY DATE_FORMAT(r.stunde, '%Y-%m-01'), r.message_status
ORDER BY DATE_FORMAT(r.stunde, '%Y-%m-01'), r.message_status;
SELECT DATE_FORMAT(r.stunde, '%Y-%m-01') AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2025-01-01 00:00:00'
  AND r.stunde <  '2026-01-01 00:00:00'
GROUP BY DATE_FORMAT(r.stunde, '%Y-%m-01'), r.message_status
ORDER BY DATE_FORMAT(r.stunde, '%Y-%m-01'), r.message_status;
SELECT DATE_FORMAT(r.stunde, '%Y-%m-01') AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2025-01-01 00:00:00'
  AND r.stunde <  '2026-01-01 00:00:00'
GROUP BY DATE_FORMAT(r.stunde, '%Y-%m-01'), r.message_status
ORDER BY DATE_FORMAT(r.stunde, '%Y-%m-01'), r.message_status;
SELECT DATE_FORMAT(r.stunde, '%Y-%m-01') AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2025-01-01 00:00:00'
  AND r.stunde <  '2026-01-01 00:00:00'
GROUP BY DATE_FORMAT(r.stunde, '%Y-%m-01'), r.message_status
ORDER BY DATE_FORMAT(r.stunde, '%Y-%m-01'), r.message_status;
SELECT DATE_FORMAT(r.stunde, '%Y-%m-01') AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2025-01-01 00:00:00'
  AND r.stunde <  '2026-01-01 00:00:00'
GROUP BY DATE_FORMAT(r.stunde, '%Y-%m-01'), r.message_status
ORDER BY DATE_FORMAT(r.stunde, '%Y-%m-01'), r.message_status;
SELECT DATE_FORMAT(r.stunde, '%Y-%m-01') AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2025-01-01 00:00:00'
  AND r.stunde <  '2026-01-01 00:00:00'
GROUP BY DATE_FORMAT(r.stunde, '%Y-%m-01'), r.message_status
ORDER BY DATE_FORMAT(r.stunde, '%Y-%m-01'), r.message_status;

-- ══ M94 Verlauf · 48 h / Stunde · SUTTONS ══════════════════════════════════
SELECT '=== M94-V P1-SUTTONS Plan ===' AS marke;
EXPLAIN SELECT r.stunde AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-12-28 05:00:00'
  AND r.stunde <  '2025-12-30 05:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;

SELECT '=== M94-V P1-SUTTONS gelesene Rollupzeilen, gezaehlt ===' AS marke;
SELECT COUNT(*) AS rollupzeilen, SUM(r.anzahl) AS nachrichten,
       COUNT(DISTINCT r.process_id) AS versch_prozesse,
       COUNT(DISTINCT r.stunde) AS belegte_eimer
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-12-28 05:00:00'
  AND r.stunde <  '2025-12-30 05:00:00';

SELECT '=== M94-V P1-SUTTONS Ergebnis (erste Zeilen) ===' AS marke;
SELECT r.stunde AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-12-28 05:00:00'
  AND r.stunde <  '2025-12-30 05:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status
LIMIT 5;

SELECT '=== M94-V P1-SUTTONS Laufzeit: Aufwaermlauf + fuenf ===' AS marke;
SELECT r.stunde AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-12-28 05:00:00'
  AND r.stunde <  '2025-12-30 05:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;
SELECT r.stunde AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-12-28 05:00:00'
  AND r.stunde <  '2025-12-30 05:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;
SELECT r.stunde AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-12-28 05:00:00'
  AND r.stunde <  '2025-12-30 05:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;
SELECT r.stunde AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-12-28 05:00:00'
  AND r.stunde <  '2025-12-30 05:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;
SELECT r.stunde AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-12-28 05:00:00'
  AND r.stunde <  '2025-12-30 05:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;
SELECT r.stunde AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-12-28 05:00:00'
  AND r.stunde <  '2025-12-30 05:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;

-- ══ M94 Verlauf · 30 Tage / Tag · SUTTONS ══════════════════════════════════
SELECT '=== M94-V P2-SUTTONS Plan ===' AS marke;
EXPLAIN SELECT DATE(r.stunde) AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-12-01 00:00:00'
  AND r.stunde <  '2025-12-31 00:00:00'
GROUP BY DATE(r.stunde), r.message_status
ORDER BY DATE(r.stunde), r.message_status;

SELECT '=== M94-V P2-SUTTONS gelesene Rollupzeilen, gezaehlt ===' AS marke;
SELECT COUNT(*) AS rollupzeilen, SUM(r.anzahl) AS nachrichten,
       COUNT(DISTINCT r.process_id) AS versch_prozesse,
       COUNT(DISTINCT DATE(r.stunde)) AS belegte_eimer
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-12-01 00:00:00'
  AND r.stunde <  '2025-12-31 00:00:00';

SELECT '=== M94-V P2-SUTTONS Ergebnis (erste Zeilen) ===' AS marke;
SELECT DATE(r.stunde) AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-12-01 00:00:00'
  AND r.stunde <  '2025-12-31 00:00:00'
GROUP BY DATE(r.stunde), r.message_status
ORDER BY DATE(r.stunde), r.message_status
LIMIT 5;

SELECT '=== M94-V P2-SUTTONS Laufzeit: Aufwaermlauf + fuenf ===' AS marke;
SELECT DATE(r.stunde) AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-12-01 00:00:00'
  AND r.stunde <  '2025-12-31 00:00:00'
GROUP BY DATE(r.stunde), r.message_status
ORDER BY DATE(r.stunde), r.message_status;
SELECT DATE(r.stunde) AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-12-01 00:00:00'
  AND r.stunde <  '2025-12-31 00:00:00'
GROUP BY DATE(r.stunde), r.message_status
ORDER BY DATE(r.stunde), r.message_status;
SELECT DATE(r.stunde) AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-12-01 00:00:00'
  AND r.stunde <  '2025-12-31 00:00:00'
GROUP BY DATE(r.stunde), r.message_status
ORDER BY DATE(r.stunde), r.message_status;
SELECT DATE(r.stunde) AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-12-01 00:00:00'
  AND r.stunde <  '2025-12-31 00:00:00'
GROUP BY DATE(r.stunde), r.message_status
ORDER BY DATE(r.stunde), r.message_status;
SELECT DATE(r.stunde) AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-12-01 00:00:00'
  AND r.stunde <  '2025-12-31 00:00:00'
GROUP BY DATE(r.stunde), r.message_status
ORDER BY DATE(r.stunde), r.message_status;
SELECT DATE(r.stunde) AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-12-01 00:00:00'
  AND r.stunde <  '2025-12-31 00:00:00'
GROUP BY DATE(r.stunde), r.message_status
ORDER BY DATE(r.stunde), r.message_status;

-- ══ M94 Verlauf · 12 Monate / Monat · SUTTONS ══════════════════════════════════
SELECT '=== M94-V P3-SUTTONS Plan ===' AS marke;
EXPLAIN SELECT DATE_FORMAT(r.stunde, '%Y-%m-01') AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-01-01 00:00:00'
  AND r.stunde <  '2026-01-01 00:00:00'
GROUP BY DATE_FORMAT(r.stunde, '%Y-%m-01'), r.message_status
ORDER BY DATE_FORMAT(r.stunde, '%Y-%m-01'), r.message_status;

SELECT '=== M94-V P3-SUTTONS gelesene Rollupzeilen, gezaehlt ===' AS marke;
SELECT COUNT(*) AS rollupzeilen, SUM(r.anzahl) AS nachrichten,
       COUNT(DISTINCT r.process_id) AS versch_prozesse,
       COUNT(DISTINCT DATE_FORMAT(r.stunde, '%Y-%m-01')) AS belegte_eimer
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-01-01 00:00:00'
  AND r.stunde <  '2026-01-01 00:00:00';

SELECT '=== M94-V P3-SUTTONS Ergebnis (erste Zeilen) ===' AS marke;
SELECT DATE_FORMAT(r.stunde, '%Y-%m-01') AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-01-01 00:00:00'
  AND r.stunde <  '2026-01-01 00:00:00'
GROUP BY DATE_FORMAT(r.stunde, '%Y-%m-01'), r.message_status
ORDER BY DATE_FORMAT(r.stunde, '%Y-%m-01'), r.message_status
LIMIT 5;

SELECT '=== M94-V P3-SUTTONS Laufzeit: Aufwaermlauf + fuenf ===' AS marke;
SELECT DATE_FORMAT(r.stunde, '%Y-%m-01') AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-01-01 00:00:00'
  AND r.stunde <  '2026-01-01 00:00:00'
GROUP BY DATE_FORMAT(r.stunde, '%Y-%m-01'), r.message_status
ORDER BY DATE_FORMAT(r.stunde, '%Y-%m-01'), r.message_status;
SELECT DATE_FORMAT(r.stunde, '%Y-%m-01') AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-01-01 00:00:00'
  AND r.stunde <  '2026-01-01 00:00:00'
GROUP BY DATE_FORMAT(r.stunde, '%Y-%m-01'), r.message_status
ORDER BY DATE_FORMAT(r.stunde, '%Y-%m-01'), r.message_status;
SELECT DATE_FORMAT(r.stunde, '%Y-%m-01') AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-01-01 00:00:00'
  AND r.stunde <  '2026-01-01 00:00:00'
GROUP BY DATE_FORMAT(r.stunde, '%Y-%m-01'), r.message_status
ORDER BY DATE_FORMAT(r.stunde, '%Y-%m-01'), r.message_status;
SELECT DATE_FORMAT(r.stunde, '%Y-%m-01') AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-01-01 00:00:00'
  AND r.stunde <  '2026-01-01 00:00:00'
GROUP BY DATE_FORMAT(r.stunde, '%Y-%m-01'), r.message_status
ORDER BY DATE_FORMAT(r.stunde, '%Y-%m-01'), r.message_status;
SELECT DATE_FORMAT(r.stunde, '%Y-%m-01') AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-01-01 00:00:00'
  AND r.stunde <  '2026-01-01 00:00:00'
GROUP BY DATE_FORMAT(r.stunde, '%Y-%m-01'), r.message_status
ORDER BY DATE_FORMAT(r.stunde, '%Y-%m-01'), r.message_status;
SELECT DATE_FORMAT(r.stunde, '%Y-%m-01') AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-01-01 00:00:00'
  AND r.stunde <  '2026-01-01 00:00:00'
GROUP BY DATE_FORMAT(r.stunde, '%Y-%m-01'), r.message_status
ORDER BY DATE_FORMAT(r.stunde, '%Y-%m-01'), r.message_status;

SELECT '=== 99 Laufzeiten ===' AS marke;
SHOW PROFILES;

SELECT '=== 99 read_only am Ende der Sitzung ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;
