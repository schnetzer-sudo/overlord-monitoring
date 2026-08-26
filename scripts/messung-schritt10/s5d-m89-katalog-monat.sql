-- Messrunde M86-M92 — Sitzung 5d: M89, Variante B ueber Fenster B (ein Monat)
--
-- Die Luecke, die 5c offenlaesst: Variante A ueber Fenster B kostet 148,840 ms (NEXANS)
-- und liegt damit unmittelbar an der 150-ms-Schwelle der vorregistrierten Deutung.
-- Variante B kostet in Fenster D2 den Faktor 1,66. Ob 148,840 x 1,66 zutrifft, wird hier
-- GEMESSEN und nicht hochgerechnet — eine Hochrechnung waere genau die Sorte Zahl, die
-- Regel L10 trennen soll.
--
-- Benutzer: monitor_read. Ausschliesslich SELECT / SET / EXPLAIN. (Regel S1)
-- Fenster B (M17, woertlich): >= '2025-11-30 00:00:00' AND < '2025-12-30 00:00:00'

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;

SET max_statement_time = 60;
SET profiling_history_size = 100;
SET profiling = 1;

SELECT '=== BNEX-B-0 ===' AS marke;
SELECT t.stunde, t.message_status, t.richtung, t.zugeordnet,
       COUNT(*) AS partner_eimer, SUM(t.anzahl) AS nachrichten
FROM (
  SELECT r.stunde,
         r.message_status,
         COALESCE(c.richtung, 'nicht zugeordnet') AS richtung,
         CASE WHEN c.process_id IS NULL                     THEN 'nein'
              WHEN c.pflegestatus <> 'GEPFLEGT'             THEN 'nein'
              WHEN c.partner IS NULL OR c.partner = ''      THEN 'nein'
              ELSE 'ja' END                                 AS zugeordnet,
         CASE WHEN c.process_id IS NULL                     THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'             THEN NULL
              WHEN c.partner IS NULL OR c.partner = ''      THEN NULL
              ELSE c.partner END                            AS partner,
         SUM(r.anzahl) AS anzahl
  FROM overlord_monitor.message_rollup_probe r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'NEXANS'
    AND r.stunde >= '2025-11-30 00:00:00'
    AND r.stunde <  '2025-12-30 00:00:00'
  GROUP BY r.stunde, r.message_status, richtung, zugeordnet, partner
) t
GROUP BY t.stunde, t.message_status, t.richtung, t.zugeordnet
ORDER BY t.stunde, t.message_status;

SELECT '=== BNEX-B-1 ===' AS marke;
SELECT t.stunde, t.message_status, t.richtung, t.zugeordnet,
       COUNT(*) AS partner_eimer, SUM(t.anzahl) AS nachrichten
FROM (
  SELECT r.stunde,
         r.message_status,
         COALESCE(c.richtung, 'nicht zugeordnet') AS richtung,
         CASE WHEN c.process_id IS NULL                     THEN 'nein'
              WHEN c.pflegestatus <> 'GEPFLEGT'             THEN 'nein'
              WHEN c.partner IS NULL OR c.partner = ''      THEN 'nein'
              ELSE 'ja' END                                 AS zugeordnet,
         CASE WHEN c.process_id IS NULL                     THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'             THEN NULL
              WHEN c.partner IS NULL OR c.partner = ''      THEN NULL
              ELSE c.partner END                            AS partner,
         SUM(r.anzahl) AS anzahl
  FROM overlord_monitor.message_rollup_probe r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'NEXANS'
    AND r.stunde >= '2025-11-30 00:00:00'
    AND r.stunde <  '2025-12-30 00:00:00'
  GROUP BY r.stunde, r.message_status, richtung, zugeordnet, partner
) t
GROUP BY t.stunde, t.message_status, t.richtung, t.zugeordnet
ORDER BY t.stunde, t.message_status;

SELECT '=== BNEX-B-2 ===' AS marke;
SELECT t.stunde, t.message_status, t.richtung, t.zugeordnet,
       COUNT(*) AS partner_eimer, SUM(t.anzahl) AS nachrichten
FROM (
  SELECT r.stunde,
         r.message_status,
         COALESCE(c.richtung, 'nicht zugeordnet') AS richtung,
         CASE WHEN c.process_id IS NULL                     THEN 'nein'
              WHEN c.pflegestatus <> 'GEPFLEGT'             THEN 'nein'
              WHEN c.partner IS NULL OR c.partner = ''      THEN 'nein'
              ELSE 'ja' END                                 AS zugeordnet,
         CASE WHEN c.process_id IS NULL                     THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'             THEN NULL
              WHEN c.partner IS NULL OR c.partner = ''      THEN NULL
              ELSE c.partner END                            AS partner,
         SUM(r.anzahl) AS anzahl
  FROM overlord_monitor.message_rollup_probe r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'NEXANS'
    AND r.stunde >= '2025-11-30 00:00:00'
    AND r.stunde <  '2025-12-30 00:00:00'
  GROUP BY r.stunde, r.message_status, richtung, zugeordnet, partner
) t
GROUP BY t.stunde, t.message_status, t.richtung, t.zugeordnet
ORDER BY t.stunde, t.message_status;

SELECT '=== BNEX-B-3 ===' AS marke;
SELECT t.stunde, t.message_status, t.richtung, t.zugeordnet,
       COUNT(*) AS partner_eimer, SUM(t.anzahl) AS nachrichten
FROM (
  SELECT r.stunde,
         r.message_status,
         COALESCE(c.richtung, 'nicht zugeordnet') AS richtung,
         CASE WHEN c.process_id IS NULL                     THEN 'nein'
              WHEN c.pflegestatus <> 'GEPFLEGT'             THEN 'nein'
              WHEN c.partner IS NULL OR c.partner = ''      THEN 'nein'
              ELSE 'ja' END                                 AS zugeordnet,
         CASE WHEN c.process_id IS NULL                     THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'             THEN NULL
              WHEN c.partner IS NULL OR c.partner = ''      THEN NULL
              ELSE c.partner END                            AS partner,
         SUM(r.anzahl) AS anzahl
  FROM overlord_monitor.message_rollup_probe r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'NEXANS'
    AND r.stunde >= '2025-11-30 00:00:00'
    AND r.stunde <  '2025-12-30 00:00:00'
  GROUP BY r.stunde, r.message_status, richtung, zugeordnet, partner
) t
GROUP BY t.stunde, t.message_status, t.richtung, t.zugeordnet
ORDER BY t.stunde, t.message_status;

SELECT '=== BNEX-B-4 ===' AS marke;
SELECT t.stunde, t.message_status, t.richtung, t.zugeordnet,
       COUNT(*) AS partner_eimer, SUM(t.anzahl) AS nachrichten
FROM (
  SELECT r.stunde,
         r.message_status,
         COALESCE(c.richtung, 'nicht zugeordnet') AS richtung,
         CASE WHEN c.process_id IS NULL                     THEN 'nein'
              WHEN c.pflegestatus <> 'GEPFLEGT'             THEN 'nein'
              WHEN c.partner IS NULL OR c.partner = ''      THEN 'nein'
              ELSE 'ja' END                                 AS zugeordnet,
         CASE WHEN c.process_id IS NULL                     THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'             THEN NULL
              WHEN c.partner IS NULL OR c.partner = ''      THEN NULL
              ELSE c.partner END                            AS partner,
         SUM(r.anzahl) AS anzahl
  FROM overlord_monitor.message_rollup_probe r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'NEXANS'
    AND r.stunde >= '2025-11-30 00:00:00'
    AND r.stunde <  '2025-12-30 00:00:00'
  GROUP BY r.stunde, r.message_status, richtung, zugeordnet, partner
) t
GROUP BY t.stunde, t.message_status, t.richtung, t.zugeordnet
ORDER BY t.stunde, t.message_status;

SELECT '=== BNEX-B-5 ===' AS marke;
SELECT t.stunde, t.message_status, t.richtung, t.zugeordnet,
       COUNT(*) AS partner_eimer, SUM(t.anzahl) AS nachrichten
FROM (
  SELECT r.stunde,
         r.message_status,
         COALESCE(c.richtung, 'nicht zugeordnet') AS richtung,
         CASE WHEN c.process_id IS NULL                     THEN 'nein'
              WHEN c.pflegestatus <> 'GEPFLEGT'             THEN 'nein'
              WHEN c.partner IS NULL OR c.partner = ''      THEN 'nein'
              ELSE 'ja' END                                 AS zugeordnet,
         CASE WHEN c.process_id IS NULL                     THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'             THEN NULL
              WHEN c.partner IS NULL OR c.partner = ''      THEN NULL
              ELSE c.partner END                            AS partner,
         SUM(r.anzahl) AS anzahl
  FROM overlord_monitor.message_rollup_probe r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'NEXANS'
    AND r.stunde >= '2025-11-30 00:00:00'
    AND r.stunde <  '2025-12-30 00:00:00'
  GROUP BY r.stunde, r.message_status, richtung, zugeordnet, partner
) t
GROUP BY t.stunde, t.message_status, t.richtung, t.zugeordnet
ORDER BY t.stunde, t.message_status;

SELECT '=== BSUT-B-0 ===' AS marke;
SELECT t.stunde, t.message_status, t.richtung, t.zugeordnet,
       COUNT(*) AS partner_eimer, SUM(t.anzahl) AS nachrichten
FROM (
  SELECT r.stunde,
         r.message_status,
         COALESCE(c.richtung, 'nicht zugeordnet') AS richtung,
         CASE WHEN c.process_id IS NULL                     THEN 'nein'
              WHEN c.pflegestatus <> 'GEPFLEGT'             THEN 'nein'
              WHEN c.partner IS NULL OR c.partner = ''      THEN 'nein'
              ELSE 'ja' END                                 AS zugeordnet,
         CASE WHEN c.process_id IS NULL                     THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'             THEN NULL
              WHEN c.partner IS NULL OR c.partner = ''      THEN NULL
              ELSE c.partner END                            AS partner,
         SUM(r.anzahl) AS anzahl
  FROM overlord_monitor.message_rollup_probe r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
    AND r.stunde >= '2025-11-30 00:00:00'
    AND r.stunde <  '2025-12-30 00:00:00'
  GROUP BY r.stunde, r.message_status, richtung, zugeordnet, partner
) t
GROUP BY t.stunde, t.message_status, t.richtung, t.zugeordnet
ORDER BY t.stunde, t.message_status;

SELECT '=== BSUT-B-1 ===' AS marke;
SELECT t.stunde, t.message_status, t.richtung, t.zugeordnet,
       COUNT(*) AS partner_eimer, SUM(t.anzahl) AS nachrichten
FROM (
  SELECT r.stunde,
         r.message_status,
         COALESCE(c.richtung, 'nicht zugeordnet') AS richtung,
         CASE WHEN c.process_id IS NULL                     THEN 'nein'
              WHEN c.pflegestatus <> 'GEPFLEGT'             THEN 'nein'
              WHEN c.partner IS NULL OR c.partner = ''      THEN 'nein'
              ELSE 'ja' END                                 AS zugeordnet,
         CASE WHEN c.process_id IS NULL                     THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'             THEN NULL
              WHEN c.partner IS NULL OR c.partner = ''      THEN NULL
              ELSE c.partner END                            AS partner,
         SUM(r.anzahl) AS anzahl
  FROM overlord_monitor.message_rollup_probe r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
    AND r.stunde >= '2025-11-30 00:00:00'
    AND r.stunde <  '2025-12-30 00:00:00'
  GROUP BY r.stunde, r.message_status, richtung, zugeordnet, partner
) t
GROUP BY t.stunde, t.message_status, t.richtung, t.zugeordnet
ORDER BY t.stunde, t.message_status;

SELECT '=== BSUT-B-2 ===' AS marke;
SELECT t.stunde, t.message_status, t.richtung, t.zugeordnet,
       COUNT(*) AS partner_eimer, SUM(t.anzahl) AS nachrichten
FROM (
  SELECT r.stunde,
         r.message_status,
         COALESCE(c.richtung, 'nicht zugeordnet') AS richtung,
         CASE WHEN c.process_id IS NULL                     THEN 'nein'
              WHEN c.pflegestatus <> 'GEPFLEGT'             THEN 'nein'
              WHEN c.partner IS NULL OR c.partner = ''      THEN 'nein'
              ELSE 'ja' END                                 AS zugeordnet,
         CASE WHEN c.process_id IS NULL                     THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'             THEN NULL
              WHEN c.partner IS NULL OR c.partner = ''      THEN NULL
              ELSE c.partner END                            AS partner,
         SUM(r.anzahl) AS anzahl
  FROM overlord_monitor.message_rollup_probe r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
    AND r.stunde >= '2025-11-30 00:00:00'
    AND r.stunde <  '2025-12-30 00:00:00'
  GROUP BY r.stunde, r.message_status, richtung, zugeordnet, partner
) t
GROUP BY t.stunde, t.message_status, t.richtung, t.zugeordnet
ORDER BY t.stunde, t.message_status;

SELECT '=== BSUT-B-3 ===' AS marke;
SELECT t.stunde, t.message_status, t.richtung, t.zugeordnet,
       COUNT(*) AS partner_eimer, SUM(t.anzahl) AS nachrichten
FROM (
  SELECT r.stunde,
         r.message_status,
         COALESCE(c.richtung, 'nicht zugeordnet') AS richtung,
         CASE WHEN c.process_id IS NULL                     THEN 'nein'
              WHEN c.pflegestatus <> 'GEPFLEGT'             THEN 'nein'
              WHEN c.partner IS NULL OR c.partner = ''      THEN 'nein'
              ELSE 'ja' END                                 AS zugeordnet,
         CASE WHEN c.process_id IS NULL                     THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'             THEN NULL
              WHEN c.partner IS NULL OR c.partner = ''      THEN NULL
              ELSE c.partner END                            AS partner,
         SUM(r.anzahl) AS anzahl
  FROM overlord_monitor.message_rollup_probe r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
    AND r.stunde >= '2025-11-30 00:00:00'
    AND r.stunde <  '2025-12-30 00:00:00'
  GROUP BY r.stunde, r.message_status, richtung, zugeordnet, partner
) t
GROUP BY t.stunde, t.message_status, t.richtung, t.zugeordnet
ORDER BY t.stunde, t.message_status;

SELECT '=== BSUT-B-4 ===' AS marke;
SELECT t.stunde, t.message_status, t.richtung, t.zugeordnet,
       COUNT(*) AS partner_eimer, SUM(t.anzahl) AS nachrichten
FROM (
  SELECT r.stunde,
         r.message_status,
         COALESCE(c.richtung, 'nicht zugeordnet') AS richtung,
         CASE WHEN c.process_id IS NULL                     THEN 'nein'
              WHEN c.pflegestatus <> 'GEPFLEGT'             THEN 'nein'
              WHEN c.partner IS NULL OR c.partner = ''      THEN 'nein'
              ELSE 'ja' END                                 AS zugeordnet,
         CASE WHEN c.process_id IS NULL                     THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'             THEN NULL
              WHEN c.partner IS NULL OR c.partner = ''      THEN NULL
              ELSE c.partner END                            AS partner,
         SUM(r.anzahl) AS anzahl
  FROM overlord_monitor.message_rollup_probe r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
    AND r.stunde >= '2025-11-30 00:00:00'
    AND r.stunde <  '2025-12-30 00:00:00'
  GROUP BY r.stunde, r.message_status, richtung, zugeordnet, partner
) t
GROUP BY t.stunde, t.message_status, t.richtung, t.zugeordnet
ORDER BY t.stunde, t.message_status;

SELECT '=== BSUT-B-5 ===' AS marke;
SELECT t.stunde, t.message_status, t.richtung, t.zugeordnet,
       COUNT(*) AS partner_eimer, SUM(t.anzahl) AS nachrichten
FROM (
  SELECT r.stunde,
         r.message_status,
         COALESCE(c.richtung, 'nicht zugeordnet') AS richtung,
         CASE WHEN c.process_id IS NULL                     THEN 'nein'
              WHEN c.pflegestatus <> 'GEPFLEGT'             THEN 'nein'
              WHEN c.partner IS NULL OR c.partner = ''      THEN 'nein'
              ELSE 'ja' END                                 AS zugeordnet,
         CASE WHEN c.process_id IS NULL                     THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'             THEN NULL
              WHEN c.partner IS NULL OR c.partner = ''      THEN NULL
              ELSE c.partner END                            AS partner,
         SUM(r.anzahl) AS anzahl
  FROM overlord_monitor.message_rollup_probe r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
    AND r.stunde >= '2025-11-30 00:00:00'
    AND r.stunde <  '2025-12-30 00:00:00'
  GROUP BY r.stunde, r.message_status, richtung, zugeordnet, partner
) t
GROUP BY t.stunde, t.message_status, t.richtung, t.zugeordnet
ORDER BY t.stunde, t.message_status;

SELECT '=== EXPLAIN BNEX-B ===' AS marke;
EXPLAIN
SELECT t.stunde, t.message_status, t.richtung, t.zugeordnet,
       COUNT(*) AS partner_eimer, SUM(t.anzahl) AS nachrichten
FROM (
  SELECT r.stunde,
         r.message_status,
         COALESCE(c.richtung, 'nicht zugeordnet') AS richtung,
         CASE WHEN c.process_id IS NULL                     THEN 'nein'
              WHEN c.pflegestatus <> 'GEPFLEGT'             THEN 'nein'
              WHEN c.partner IS NULL OR c.partner = ''      THEN 'nein'
              ELSE 'ja' END                                 AS zugeordnet,
         CASE WHEN c.process_id IS NULL                     THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'             THEN NULL
              WHEN c.partner IS NULL OR c.partner = ''      THEN NULL
              ELSE c.partner END                            AS partner,
         SUM(r.anzahl) AS anzahl
  FROM overlord_monitor.message_rollup_probe r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'NEXANS'
    AND r.stunde >= '2025-11-30 00:00:00'
    AND r.stunde <  '2025-12-30 00:00:00'
  GROUP BY r.stunde, r.message_status, richtung, zugeordnet, partner
) t
GROUP BY t.stunde, t.message_status, t.richtung, t.zugeordnet
ORDER BY t.stunde, t.message_status;

SELECT '=== EXPLAIN BSUT-B ===' AS marke;
EXPLAIN
SELECT t.stunde, t.message_status, t.richtung, t.zugeordnet,
       COUNT(*) AS partner_eimer, SUM(t.anzahl) AS nachrichten
FROM (
  SELECT r.stunde,
         r.message_status,
         COALESCE(c.richtung, 'nicht zugeordnet') AS richtung,
         CASE WHEN c.process_id IS NULL                     THEN 'nein'
              WHEN c.pflegestatus <> 'GEPFLEGT'             THEN 'nein'
              WHEN c.partner IS NULL OR c.partner = ''      THEN 'nein'
              ELSE 'ja' END                                 AS zugeordnet,
         CASE WHEN c.process_id IS NULL                     THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'             THEN NULL
              WHEN c.partner IS NULL OR c.partner = ''      THEN NULL
              ELSE c.partner END                            AS partner,
         SUM(r.anzahl) AS anzahl
  FROM overlord_monitor.message_rollup_probe r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
    AND r.stunde >= '2025-11-30 00:00:00'
    AND r.stunde <  '2025-12-30 00:00:00'
  GROUP BY r.stunde, r.message_status, richtung, zugeordnet, partner
) t
GROUP BY t.stunde, t.message_status, t.richtung, t.zugeordnet
ORDER BY t.stunde, t.message_status;

SELECT '=== Ergebnisgroessen ===' AS marke;
SELECT 'NEXANS' AS mandant, COUNT(*) AS rollupzeilen_im_fenster, SUM(r.anzahl) AS nachrichten
FROM overlord_monitor.message_rollup_probe r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.Project pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'NEXANS' AND r.stunde >= '2025-11-30 00:00:00' AND r.stunde < '2025-12-30 00:00:00'
UNION ALL
SELECT 'SUTTONS', COUNT(*), SUM(r.anzahl)
FROM overlord_monitor.message_rollup_probe r
JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
JOIN GlassfishDB.Project pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS' AND r.stunde >= '2025-11-30 00:00:00' AND r.stunde < '2025-12-30 00:00:00'
UNION ALL
SELECT 'alle', COUNT(*), SUM(anzahl) FROM overlord_monitor.message_rollup_probe
WHERE stunde >= '2025-11-30 00:00:00' AND stunde < '2025-12-30 00:00:00';

SELECT '=== 98 SHOW PROFILES ===' AS marke;
SHOW PROFILES;
SELECT '=== 99 Abschluss ===' AS marke;
SELECT @@global.read_only AS read_only_ende, NOW() AS serverzeit_ende;
