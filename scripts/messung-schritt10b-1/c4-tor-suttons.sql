-- Messung zu Schritt 10b-1, Teil C.4: DAS TOR · SUTTONS
-- Auftrag:  "Schritt 10b-1", Stand 27.08.2026, Teil C.4
-- Ergebnis: docs/rollup.md §9a
--
-- Ausschliesslich SELECT / SET / EXPLAIN. (Regel S1)
--
-- Gemessen werden Verlauf und Verteilung ueber die neue TAGESEBENE, fuer 12 Monate
-- und fuer 30 Tage — und daneben, in derselben Sitzung, dieselben vier Faelle ueber
-- die STUNDENEBENE. Nur so ist der Vergleich so genau, wie er sein muss: Die Werte
-- aus M94 stammen aus einer anderen Sitzung an einem anderen Tag.
--
-- Die Abfragen sind die aus M94, Zeichen fuer Zeichen — bis auf die Tabelle und den
-- Eimerausdruck. Der Eimer steht in SELECT, GROUP BY und ORDER BY jeweils als VOLLER
-- Ausdruck und nie ueber den Alias (Befund 11 der Vorrunde: Hiesse ein Alias wie eine
-- Tabellenspalte, baende MariaDB still an die Spalte).
--
-- G1: Der Verlauf gibt Eimer, Rohstatus und Anzahlen aus — keine Kennung, keinen
--     Partnernamen. Er wird deshalb NACKT gemessen, wie in M94. Die Verteilung haelt
--     den Partnernamen in einer inneren Abfrage; nach aussen dringen nur Anzahlen.
-- Z1: Alle Fenstergrenzen als Literal, hergeleitet aus dem Anker 2025-12-30 04:09:47.
--
-- Laufzeiten aus `information_schema.PROFILING`; `anzahl_laeufe` muss 6 sein.

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;

SET max_statement_time = 60;
SET profiling = 1;
SET profiling_history_size = 200;



-- ── Vorbedingung: Beide Ebenen sagen dasselbe ───────────────────────────────
-- Ohne diese Probe misst der Rest nur, dass zwei verschiedene Zahlen verschieden
-- schnell entstehen.
SELECT '=== Gleichheitsprobe SUTTONS: Stundenebene gegen Tagesebene ===' AS marke;
SELECT s.eimer, s.nachrichten AS aus_stunden, t.nachrichten AS aus_tagen,
       (s.nachrichten = t.nachrichten) AS gleich
FROM (
  SELECT DATE_FORMAT(r.stunde, '%Y-%m-01') AS eimer, SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  WHERE pm.MandantID = 'SUTTONS'
    AND r.stunde >= '2025-01-01 00:00:00' AND r.stunde < '2026-01-01 00:00:00'
  GROUP BY DATE_FORMAT(r.stunde, '%Y-%m-01')) s
JOIN (
  SELECT DATE_FORMAT(r.tag, '%Y-%m-01') AS eimer, SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup_tag r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  WHERE pm.MandantID = 'SUTTONS'
    AND r.tag >= '2025-01-01' AND r.tag < '2026-01-01'
  GROUP BY DATE_FORMAT(r.tag, '%Y-%m-01')) t ON t.eimer = s.eimer
ORDER BY s.eimer;

SELECT '=== Gleichheitsprobe SUTTONS: Zeilen, die die Ebenen lesen ===' AS marke;
SELECT (SELECT COUNT(*) FROM overlord_monitor.message_rollup
         WHERE stunde >= '2025-01-01 00:00:00' AND stunde < '2026-01-01 00:00:00')
         AS stundenzeilen_12monate,
       (SELECT COUNT(*) FROM overlord_monitor.message_rollup_tag
         WHERE tag >= '2025-01-01' AND tag < '2026-01-01') AS tageszeilen_12monate,
       (SELECT COUNT(*) FROM overlord_monitor.message_rollup
         WHERE stunde >= '2025-12-01 00:00:00' AND stunde < '2025-12-31 00:00:00')
         AS stundenzeilen_30tage,
       (SELECT COUNT(*) FROM overlord_monitor.message_rollup_tag
         WHERE tag >= '2025-12-01' AND tag < '2025-12-31') AS tageszeilen_30tage;


-- ==========================================================================
-- verlauf-stunde-12monate-SUTTONS
-- ==========================================================================
SELECT '=== verlauf-stunde-12monate-SUTTONS · Plan (L15) ===' AS marke;
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
SELECT '=== verlauf-stunde-12monate-SUTTONS · Aufwaermlauf + fuenf ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
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
SELECT 'verlauf-stunde-12monate-SUTTONS' AS fall,
       COUNT(*)                                       AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)              AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)              AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;


-- ==========================================================================
-- verlauf-tag-12monate-SUTTONS
-- ==========================================================================
SELECT '=== verlauf-tag-12monate-SUTTONS · Plan (L15) ===' AS marke;
EXPLAIN SELECT DATE_FORMAT(r.tag, '%Y-%m-01') AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.tag >= '2025-01-01'
  AND r.tag <  '2026-01-01'
GROUP BY DATE_FORMAT(r.tag, '%Y-%m-01'), r.message_status
ORDER BY DATE_FORMAT(r.tag, '%Y-%m-01'), r.message_status;
SELECT '=== verlauf-tag-12monate-SUTTONS · Aufwaermlauf + fuenf ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT DATE_FORMAT(r.tag, '%Y-%m-01') AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.tag >= '2025-01-01'
  AND r.tag <  '2026-01-01'
GROUP BY DATE_FORMAT(r.tag, '%Y-%m-01'), r.message_status
ORDER BY DATE_FORMAT(r.tag, '%Y-%m-01'), r.message_status;
SELECT DATE_FORMAT(r.tag, '%Y-%m-01') AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.tag >= '2025-01-01'
  AND r.tag <  '2026-01-01'
GROUP BY DATE_FORMAT(r.tag, '%Y-%m-01'), r.message_status
ORDER BY DATE_FORMAT(r.tag, '%Y-%m-01'), r.message_status;
SELECT DATE_FORMAT(r.tag, '%Y-%m-01') AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.tag >= '2025-01-01'
  AND r.tag <  '2026-01-01'
GROUP BY DATE_FORMAT(r.tag, '%Y-%m-01'), r.message_status
ORDER BY DATE_FORMAT(r.tag, '%Y-%m-01'), r.message_status;
SELECT DATE_FORMAT(r.tag, '%Y-%m-01') AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.tag >= '2025-01-01'
  AND r.tag <  '2026-01-01'
GROUP BY DATE_FORMAT(r.tag, '%Y-%m-01'), r.message_status
ORDER BY DATE_FORMAT(r.tag, '%Y-%m-01'), r.message_status;
SELECT DATE_FORMAT(r.tag, '%Y-%m-01') AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.tag >= '2025-01-01'
  AND r.tag <  '2026-01-01'
GROUP BY DATE_FORMAT(r.tag, '%Y-%m-01'), r.message_status
ORDER BY DATE_FORMAT(r.tag, '%Y-%m-01'), r.message_status;
SELECT DATE_FORMAT(r.tag, '%Y-%m-01') AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.tag >= '2025-01-01'
  AND r.tag <  '2026-01-01'
GROUP BY DATE_FORMAT(r.tag, '%Y-%m-01'), r.message_status
ORDER BY DATE_FORMAT(r.tag, '%Y-%m-01'), r.message_status;
SELECT 'verlauf-tag-12monate-SUTTONS' AS fall,
       COUNT(*)                                       AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)              AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)              AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;


-- ==========================================================================
-- verlauf-stunde-30tage-SUTTONS
-- ==========================================================================
SELECT '=== verlauf-stunde-30tage-SUTTONS · Plan (L15) ===' AS marke;
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
SELECT '=== verlauf-stunde-30tage-SUTTONS · Aufwaermlauf + fuenf ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
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
SELECT 'verlauf-stunde-30tage-SUTTONS' AS fall,
       COUNT(*)                                       AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)              AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)              AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;


-- ==========================================================================
-- verlauf-tag-30tage-SUTTONS
-- ==========================================================================
SELECT '=== verlauf-tag-30tage-SUTTONS · Plan (L15) ===' AS marke;
EXPLAIN SELECT r.tag AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.tag >= '2025-12-01'
  AND r.tag <  '2025-12-31'
GROUP BY r.tag, r.message_status
ORDER BY r.tag, r.message_status;
SELECT '=== verlauf-tag-30tage-SUTTONS · Aufwaermlauf + fuenf ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT r.tag AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.tag >= '2025-12-01'
  AND r.tag <  '2025-12-31'
GROUP BY r.tag, r.message_status
ORDER BY r.tag, r.message_status;
SELECT r.tag AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.tag >= '2025-12-01'
  AND r.tag <  '2025-12-31'
GROUP BY r.tag, r.message_status
ORDER BY r.tag, r.message_status;
SELECT r.tag AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.tag >= '2025-12-01'
  AND r.tag <  '2025-12-31'
GROUP BY r.tag, r.message_status
ORDER BY r.tag, r.message_status;
SELECT r.tag AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.tag >= '2025-12-01'
  AND r.tag <  '2025-12-31'
GROUP BY r.tag, r.message_status
ORDER BY r.tag, r.message_status;
SELECT r.tag AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.tag >= '2025-12-01'
  AND r.tag <  '2025-12-31'
GROUP BY r.tag, r.message_status
ORDER BY r.tag, r.message_status;
SELECT r.tag AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_tag r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.tag >= '2025-12-01'
  AND r.tag <  '2025-12-31'
GROUP BY r.tag, r.message_status
ORDER BY r.tag, r.message_status;
SELECT 'verlauf-tag-30tage-SUTTONS' AS fall,
       COUNT(*)                                       AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)              AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)              AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;


-- ==========================================================================
-- verteilung-stunde-12monate-SUTTONS
-- ==========================================================================
SELECT '=== verteilung-stunde-12monate-SUTTONS · Plan (L15) ===' AS marke;
EXPLAIN SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-01-01 00:00:00'
  AND r.stunde <  '2026-01-01 00:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT '=== verteilung-stunde-12monate-SUTTONS · Aufwaermlauf + fuenf ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-01-01 00:00:00'
  AND r.stunde <  '2026-01-01 00:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-01-01 00:00:00'
  AND r.stunde <  '2026-01-01 00:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-01-01 00:00:00'
  AND r.stunde <  '2026-01-01 00:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-01-01 00:00:00'
  AND r.stunde <  '2026-01-01 00:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-01-01 00:00:00'
  AND r.stunde <  '2026-01-01 00:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-01-01 00:00:00'
  AND r.stunde <  '2026-01-01 00:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT 'verteilung-stunde-12monate-SUTTONS' AS fall,
       COUNT(*)                                       AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)              AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)              AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;


-- ==========================================================================
-- verteilung-tag-12monate-SUTTONS
-- ==========================================================================
SELECT '=== verteilung-tag-12monate-SUTTONS · Plan (L15) ===' AS marke;
EXPLAIN SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup_tag r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
  AND r.tag >= '2025-01-01'
  AND r.tag <  '2026-01-01'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT '=== verteilung-tag-12monate-SUTTONS · Aufwaermlauf + fuenf ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup_tag r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
  AND r.tag >= '2025-01-01'
  AND r.tag <  '2026-01-01'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup_tag r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
  AND r.tag >= '2025-01-01'
  AND r.tag <  '2026-01-01'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup_tag r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
  AND r.tag >= '2025-01-01'
  AND r.tag <  '2026-01-01'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup_tag r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
  AND r.tag >= '2025-01-01'
  AND r.tag <  '2026-01-01'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup_tag r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
  AND r.tag >= '2025-01-01'
  AND r.tag <  '2026-01-01'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup_tag r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
  AND r.tag >= '2025-01-01'
  AND r.tag <  '2026-01-01'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT 'verteilung-tag-12monate-SUTTONS' AS fall,
       COUNT(*)                                       AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)              AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)              AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;


-- ==========================================================================
-- verteilung-stunde-30tage-SUTTONS
-- ==========================================================================
SELECT '=== verteilung-stunde-30tage-SUTTONS · Plan (L15) ===' AS marke;
EXPLAIN SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-12-01 00:00:00'
  AND r.stunde <  '2025-12-31 00:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT '=== verteilung-stunde-30tage-SUTTONS · Aufwaermlauf + fuenf ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-12-01 00:00:00'
  AND r.stunde <  '2025-12-31 00:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-12-01 00:00:00'
  AND r.stunde <  '2025-12-31 00:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-12-01 00:00:00'
  AND r.stunde <  '2025-12-31 00:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-12-01 00:00:00'
  AND r.stunde <  '2025-12-31 00:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-12-01 00:00:00'
  AND r.stunde <  '2025-12-31 00:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-12-01 00:00:00'
  AND r.stunde <  '2025-12-31 00:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT 'verteilung-stunde-30tage-SUTTONS' AS fall,
       COUNT(*)                                       AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)              AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)              AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;


-- ==========================================================================
-- verteilung-tag-30tage-SUTTONS
-- ==========================================================================
SELECT '=== verteilung-tag-30tage-SUTTONS · Plan (L15) ===' AS marke;
EXPLAIN SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup_tag r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
  AND r.tag >= '2025-12-01'
  AND r.tag <  '2025-12-31'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT '=== verteilung-tag-30tage-SUTTONS · Aufwaermlauf + fuenf ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup_tag r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
  AND r.tag >= '2025-12-01'
  AND r.tag <  '2025-12-31'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup_tag r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
  AND r.tag >= '2025-12-01'
  AND r.tag <  '2025-12-31'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup_tag r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
  AND r.tag >= '2025-12-01'
  AND r.tag <  '2025-12-31'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup_tag r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
  AND r.tag >= '2025-12-01'
  AND r.tag <  '2025-12-31'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup_tag r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
  AND r.tag >= '2025-12-01'
  AND r.tag <  '2025-12-31'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup_tag r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
  AND r.tag >= '2025-12-01'
  AND r.tag <  '2025-12-31'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT 'verteilung-tag-30tage-SUTTONS' AS fall,
       COUNT(*)                                       AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)              AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)              AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;


SELECT '=== Abschluss: read_only ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;
