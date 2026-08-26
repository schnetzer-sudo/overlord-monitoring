-- Messrunde M86-M92 — Sitzung 5c: M89, zwei Nachmessungen
--
-- WARUM ES DIESE SITZUNG GIBT. Sitzung 5b hat die Abfrage gefahren, die der Auftrag
-- woertlich beschreibt: Rollup, Mandantenkette, LEFT JOIN auf process_catalog, gruppiert
-- nach Stunde und Rohstatus. Im EXPLAIN kommt process_catalog **kein einziges Mal** vor.
-- MariaDB entfernt den LEFT JOIN vollstaendig: keine Spalte von c wird verwendet, und der
-- Join laeuft auf den Primaerschluessel, kann die Zeilenmenge also nicht vervielfachen.
-- Die Abfrage aus 5b misst den Katalog-Join deshalb NICHT — sie misst seine Abwesenheit.
--
-- Variante B holt ihn zurueck, indem Partner und Richtung tatsaechlich in die Gruppierung
-- eingehen. Das ist zugleich naeher an dem, was 10b braucht.
-- G1: Der Partnername verlaesst die innere Abfrage nicht. Nach aussen dringen nur die
--     Anzahl der Partner-Eimer, die Richtung und "zugeordnet ja/nein" (E-i).
--
-- Dazu Fenster B (ein Monat) in Variante A — das Dashboard bietet mehr als 48 Stunden an,
-- und das 500-ms-Budget gilt fuer die ganze Landingpage, nicht fuer ihr kuerzestes Fenster.
--
-- Benutzer: monitor_read. Ausschliesslich SELECT / SET / EXPLAIN. (Regel S1)
-- Z1: alle Grenzen als Literal.   L7: NEXANS und SUTTONS.

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;

SET max_statement_time = 60;
SET profiling_history_size = 100;
SET profiling = 1;

SELECT '=== BNEX-D2-0 Aufwaermlauf ===' AS marke;
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
    AND r.stunde >= '2025-12-28 00:00:00'
    AND r.stunde <  '2025-12-30 00:00:00'
  GROUP BY r.stunde, r.message_status, richtung, zugeordnet, partner
) t
GROUP BY t.stunde, t.message_status, t.richtung, t.zugeordnet
ORDER BY t.stunde, t.message_status;

SELECT '=== BNEX-D2-1 ===' AS marke;
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
    AND r.stunde >= '2025-12-28 00:00:00'
    AND r.stunde <  '2025-12-30 00:00:00'
  GROUP BY r.stunde, r.message_status, richtung, zugeordnet, partner
) t
GROUP BY t.stunde, t.message_status, t.richtung, t.zugeordnet
ORDER BY t.stunde, t.message_status;

SELECT '=== BNEX-D2-2 ===' AS marke;
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
    AND r.stunde >= '2025-12-28 00:00:00'
    AND r.stunde <  '2025-12-30 00:00:00'
  GROUP BY r.stunde, r.message_status, richtung, zugeordnet, partner
) t
GROUP BY t.stunde, t.message_status, t.richtung, t.zugeordnet
ORDER BY t.stunde, t.message_status;

SELECT '=== BNEX-D2-3 ===' AS marke;
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
    AND r.stunde >= '2025-12-28 00:00:00'
    AND r.stunde <  '2025-12-30 00:00:00'
  GROUP BY r.stunde, r.message_status, richtung, zugeordnet, partner
) t
GROUP BY t.stunde, t.message_status, t.richtung, t.zugeordnet
ORDER BY t.stunde, t.message_status;

SELECT '=== BNEX-D2-4 ===' AS marke;
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
    AND r.stunde >= '2025-12-28 00:00:00'
    AND r.stunde <  '2025-12-30 00:00:00'
  GROUP BY r.stunde, r.message_status, richtung, zugeordnet, partner
) t
GROUP BY t.stunde, t.message_status, t.richtung, t.zugeordnet
ORDER BY t.stunde, t.message_status;

SELECT '=== BNEX-D2-5 ===' AS marke;
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
    AND r.stunde >= '2025-12-28 00:00:00'
    AND r.stunde <  '2025-12-30 00:00:00'
  GROUP BY r.stunde, r.message_status, richtung, zugeordnet, partner
) t
GROUP BY t.stunde, t.message_status, t.richtung, t.zugeordnet
ORDER BY t.stunde, t.message_status;

SELECT '=== BSUT-D2-0 Aufwaermlauf ===' AS marke;
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
    AND r.stunde >= '2025-12-28 00:00:00'
    AND r.stunde <  '2025-12-30 00:00:00'
  GROUP BY r.stunde, r.message_status, richtung, zugeordnet, partner
) t
GROUP BY t.stunde, t.message_status, t.richtung, t.zugeordnet
ORDER BY t.stunde, t.message_status;

SELECT '=== BSUT-D2-1 ===' AS marke;
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
    AND r.stunde >= '2025-12-28 00:00:00'
    AND r.stunde <  '2025-12-30 00:00:00'
  GROUP BY r.stunde, r.message_status, richtung, zugeordnet, partner
) t
GROUP BY t.stunde, t.message_status, t.richtung, t.zugeordnet
ORDER BY t.stunde, t.message_status;

SELECT '=== BSUT-D2-2 ===' AS marke;
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
    AND r.stunde >= '2025-12-28 00:00:00'
    AND r.stunde <  '2025-12-30 00:00:00'
  GROUP BY r.stunde, r.message_status, richtung, zugeordnet, partner
) t
GROUP BY t.stunde, t.message_status, t.richtung, t.zugeordnet
ORDER BY t.stunde, t.message_status;

SELECT '=== BSUT-D2-3 ===' AS marke;
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
    AND r.stunde >= '2025-12-28 00:00:00'
    AND r.stunde <  '2025-12-30 00:00:00'
  GROUP BY r.stunde, r.message_status, richtung, zugeordnet, partner
) t
GROUP BY t.stunde, t.message_status, t.richtung, t.zugeordnet
ORDER BY t.stunde, t.message_status;

SELECT '=== BSUT-D2-4 ===' AS marke;
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
    AND r.stunde >= '2025-12-28 00:00:00'
    AND r.stunde <  '2025-12-30 00:00:00'
  GROUP BY r.stunde, r.message_status, richtung, zugeordnet, partner
) t
GROUP BY t.stunde, t.message_status, t.richtung, t.zugeordnet
ORDER BY t.stunde, t.message_status;

SELECT '=== BSUT-D2-5 ===' AS marke;
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
    AND r.stunde >= '2025-12-28 00:00:00'
    AND r.stunde <  '2025-12-30 00:00:00'
  GROUP BY r.stunde, r.message_status, richtung, zugeordnet, partner
) t
GROUP BY t.stunde, t.message_status, t.richtung, t.zugeordnet
ORDER BY t.stunde, t.message_status;

SELECT '=== ANEX-B-0 Aufwaermlauf ===' AS marke;
SELECT r.stunde, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_probe r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2025-11-30 00:00:00'
  AND r.stunde <  '2025-12-30 00:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;

SELECT '=== ANEX-B-1 ===' AS marke;
SELECT r.stunde, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_probe r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2025-11-30 00:00:00'
  AND r.stunde <  '2025-12-30 00:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;

SELECT '=== ANEX-B-2 ===' AS marke;
SELECT r.stunde, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_probe r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2025-11-30 00:00:00'
  AND r.stunde <  '2025-12-30 00:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;

SELECT '=== ANEX-B-3 ===' AS marke;
SELECT r.stunde, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_probe r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2025-11-30 00:00:00'
  AND r.stunde <  '2025-12-30 00:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;

SELECT '=== ANEX-B-4 ===' AS marke;
SELECT r.stunde, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_probe r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2025-11-30 00:00:00'
  AND r.stunde <  '2025-12-30 00:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;

SELECT '=== ANEX-B-5 ===' AS marke;
SELECT r.stunde, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_probe r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2025-11-30 00:00:00'
  AND r.stunde <  '2025-12-30 00:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;

SELECT '=== ASUT-B-0 Aufwaermlauf ===' AS marke;
SELECT r.stunde, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_probe r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-11-30 00:00:00'
  AND r.stunde <  '2025-12-30 00:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;

SELECT '=== ASUT-B-1 ===' AS marke;
SELECT r.stunde, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_probe r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-11-30 00:00:00'
  AND r.stunde <  '2025-12-30 00:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;

SELECT '=== ASUT-B-2 ===' AS marke;
SELECT r.stunde, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_probe r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-11-30 00:00:00'
  AND r.stunde <  '2025-12-30 00:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;

SELECT '=== ASUT-B-3 ===' AS marke;
SELECT r.stunde, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_probe r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-11-30 00:00:00'
  AND r.stunde <  '2025-12-30 00:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;

SELECT '=== ASUT-B-4 ===' AS marke;
SELECT r.stunde, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_probe r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-11-30 00:00:00'
  AND r.stunde <  '2025-12-30 00:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;

SELECT '=== ASUT-B-5 ===' AS marke;
SELECT r.stunde, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_probe r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-11-30 00:00:00'
  AND r.stunde <  '2025-12-30 00:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;

-- ===== L15 =====

SELECT '=== EXPLAIN BNEX-D2 ===' AS marke;
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
    AND r.stunde >= '2025-12-28 00:00:00'
    AND r.stunde <  '2025-12-30 00:00:00'
  GROUP BY r.stunde, r.message_status, richtung, zugeordnet, partner
) t
GROUP BY t.stunde, t.message_status, t.richtung, t.zugeordnet
ORDER BY t.stunde, t.message_status;

SELECT '=== EXPLAIN BSUT-D2 ===' AS marke;
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
    AND r.stunde >= '2025-12-28 00:00:00'
    AND r.stunde <  '2025-12-30 00:00:00'
  GROUP BY r.stunde, r.message_status, richtung, zugeordnet, partner
) t
GROUP BY t.stunde, t.message_status, t.richtung, t.zugeordnet
ORDER BY t.stunde, t.message_status;

SELECT '=== EXPLAIN ANEX-B ===' AS marke;
EXPLAIN
SELECT r.stunde, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_probe r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2025-11-30 00:00:00'
  AND r.stunde <  '2025-12-30 00:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;

SELECT '=== EXPLAIN ASUT-B ===' AS marke;
EXPLAIN
SELECT r.stunde, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_probe r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-11-30 00:00:00'
  AND r.stunde <  '2025-12-30 00:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;

SELECT '=== 98 SHOW PROFILES ===' AS marke;
SHOW PROFILES;

SELECT '=== 99 Abschluss ===' AS marke;
SELECT @@global.read_only AS read_only_ende, NOW() AS serverzeit_ende;
