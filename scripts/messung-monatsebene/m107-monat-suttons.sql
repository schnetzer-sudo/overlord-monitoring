-- Messung M107: DIE MONATSEBENE UEBER ZWOELF MONATE · SUTTONS
-- Auftrag:  "Drei Sicherheitstests und die Monatsebene", Stand 31.08.2026, Teil B.4
-- Ergebnis: docs/rollup.md §9d
--
-- Ausschliesslich SELECT / SET / EXPLAIN. (Regel S1)
--
-- Gemessen wird, was die Zwoelf-Monats-Ansicht ueber die neue MONATSEBENE kostet --
-- Verlauf und Verteilung --, und daneben, in DERSELBEN Sitzung, dieselben zwei Faelle
-- ueber die Tagesebene. Nur so ist der Vergleich so genau, wie er sein muss: Die
-- 767,128 ms und 908,539 ms aus §9a stammen aus einer anderen Sitzung an einem
-- anderen Tag.
--
-- Die Abfragen sind die aus §9a, Zeichen fuer Zeichen -- bis auf die Tabelle und den
-- Eimerausdruck.
--
-- DER EIMERAUSDRUCK FAELLT AUF DER MONATSEBENE WEG, und das ist kein Schummeln,
-- sondern der Punkt: `monat` IST der Eimer. Damit die Ersparnis trotzdem der
-- Zeilenzahl zugeschrieben werden kann und nicht dem weggefallenen DATE_FORMAT,
-- wird der Verlauf ZWEIMAL gemessen -- einmal mit `r.monat` und einmal mit
-- DATE_FORMAT(r.monat, '%Y-%m-01'). Steht dazwischen kein Unterschied, ist der
-- Ausdruck folgenlos und die Ersparnis kommt aus den gelesenen Zeilen.
--
-- G1: Der Verlauf gibt Eimer, Rohstatus und Anzahlen aus -- keine Kennung, keinen
--     Partnernamen. Die Verteilung haelt den Partnernamen in einer inneren Abfrage;
--     nach aussen dringen nur Anzahlen.
-- Z1: Alle Fenstergrenzen als Literal, hergeleitet aus dem Anker 2025-12-30 04:09:47.
--
-- Laufzeiten aus `information_schema.PROFILING`; `anzahl_laeufe` muss 6 sein.

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;

SET max_statement_time = 60;
SET profiling = 1;
SET profiling_history_size = 200;


-- == Vorbedingung: Alle DREI Ebenen sagen dasselbe ==========================
-- Ohne diese Probe misst der Rest nur, dass drei verschiedene Zahlen verschieden
-- schnell entstehen.
SELECT '=== Gleichheitsprobe SUTTONS: Stunde gegen Tag gegen Monat ===' AS marke;
SELECT s.eimer,
       s.nachrichten AS aus_stunden,
       t.nachrichten AS aus_tagen,
       m.nachrichten AS aus_monaten,
       (s.nachrichten = t.nachrichten AND t.nachrichten = m.nachrichten) AS gleich
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
JOIN (
  SELECT DATE_FORMAT(r.monat, '%Y-%m-01') AS eimer, SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup_monat r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  WHERE pm.MandantID = 'SUTTONS'
    AND r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
  GROUP BY DATE_FORMAT(r.monat, '%Y-%m-01')) m ON m.eimer = s.eimer
ORDER BY s.eimer;

-- Der JOIN oben verschweigt einen Eimer, den eine Ebene gar nicht hat. Diese Probe
-- nicht: Sie zaehlt Eimer und Summen je Ebene einzeln.
SELECT '=== Gleichheitsprobe SUTTONS: Eimer und Summen je Ebene ===' AS marke;
SELECT 'stunde' AS ebene, COUNT(*) AS eimer, SUM(nachrichten) AS nachrichten FROM (
  SELECT DATE_FORMAT(r.stunde, '%Y-%m-01') AS e, SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
  JOIN GlassfishDB.Project pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  WHERE pm.MandantID = 'SUTTONS' AND r.stunde >= '2025-01-01 00:00:00' AND r.stunde < '2026-01-01 00:00:00'
  GROUP BY DATE_FORMAT(r.stunde, '%Y-%m-01')) x
UNION ALL
SELECT 'tag', COUNT(*), SUM(nachrichten) FROM (
  SELECT DATE_FORMAT(r.tag, '%Y-%m-01') AS e, SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup_tag r
  JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
  JOIN GlassfishDB.Project pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  WHERE pm.MandantID = 'SUTTONS' AND r.tag >= '2025-01-01' AND r.tag < '2026-01-01'
  GROUP BY DATE_FORMAT(r.tag, '%Y-%m-01')) y
UNION ALL
SELECT 'monat', COUNT(*), SUM(nachrichten) FROM (
  SELECT DATE_FORMAT(r.monat, '%Y-%m-01') AS e, SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup_monat r
  JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
  JOIN GlassfishDB.Project pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  WHERE pm.MandantID = 'SUTTONS' AND r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
  GROUP BY DATE_FORMAT(r.monat, '%Y-%m-01')) z;

SELECT '=== Gleichheitsprobe SUTTONS: Zeilen, die die Ebenen lesen ===' AS marke;
SELECT (SELECT COUNT(*) FROM overlord_monitor.message_rollup
         WHERE stunde >= '2025-01-01 00:00:00' AND stunde < '2026-01-01 00:00:00') AS stundenzeilen_12monate,
       (SELECT COUNT(*) FROM overlord_monitor.message_rollup_tag
         WHERE tag >= '2025-01-01' AND tag < '2026-01-01') AS tageszeilen_12monate,
       (SELECT COUNT(*) FROM overlord_monitor.message_rollup_monat
         WHERE monat >= '2025-01-01' AND monat < '2026-01-01') AS monatszeilen_12monate;

-- ==========================================================================
-- verlauf-monat-12monate-SUTTONS
-- ==========================================================================
SELECT '=== verlauf-monat-12monate-SUTTONS · Plan (L15) ===' AS marke;
EXPLAIN SELECT r.monat AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.monat >= '2025-01-01'
  AND r.monat <  '2026-01-01'
GROUP BY r.monat, r.message_status
ORDER BY r.monat, r.message_status;
SELECT '=== verlauf-monat-12monate-SUTTONS · Aufwaermlauf + fuenf ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT r.monat AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.monat >= '2025-01-01'
  AND r.monat <  '2026-01-01'
GROUP BY r.monat, r.message_status
ORDER BY r.monat, r.message_status;
SELECT r.monat AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.monat >= '2025-01-01'
  AND r.monat <  '2026-01-01'
GROUP BY r.monat, r.message_status
ORDER BY r.monat, r.message_status;
SELECT r.monat AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.monat >= '2025-01-01'
  AND r.monat <  '2026-01-01'
GROUP BY r.monat, r.message_status
ORDER BY r.monat, r.message_status;
SELECT r.monat AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.monat >= '2025-01-01'
  AND r.monat <  '2026-01-01'
GROUP BY r.monat, r.message_status
ORDER BY r.monat, r.message_status;
SELECT r.monat AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.monat >= '2025-01-01'
  AND r.monat <  '2026-01-01'
GROUP BY r.monat, r.message_status
ORDER BY r.monat, r.message_status;
SELECT r.monat AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.monat >= '2025-01-01'
  AND r.monat <  '2026-01-01'
GROUP BY r.monat, r.message_status
ORDER BY r.monat, r.message_status;
SELECT 'verlauf-monat-12monate-SUTTONS' AS fall,
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
-- verlauf-monat-dateformat-12monate-SUTTONS
-- ==========================================================================
SELECT '=== verlauf-monat-dateformat-12monate-SUTTONS · Plan (L15) ===' AS marke;
EXPLAIN SELECT DATE_FORMAT(r.monat, '%Y-%m-01') AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.monat >= '2025-01-01'
  AND r.monat <  '2026-01-01'
GROUP BY DATE_FORMAT(r.monat, '%Y-%m-01'), r.message_status
ORDER BY DATE_FORMAT(r.monat, '%Y-%m-01'), r.message_status;
SELECT '=== verlauf-monat-dateformat-12monate-SUTTONS · Aufwaermlauf + fuenf ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT DATE_FORMAT(r.monat, '%Y-%m-01') AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.monat >= '2025-01-01'
  AND r.monat <  '2026-01-01'
GROUP BY DATE_FORMAT(r.monat, '%Y-%m-01'), r.message_status
ORDER BY DATE_FORMAT(r.monat, '%Y-%m-01'), r.message_status;
SELECT DATE_FORMAT(r.monat, '%Y-%m-01') AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.monat >= '2025-01-01'
  AND r.monat <  '2026-01-01'
GROUP BY DATE_FORMAT(r.monat, '%Y-%m-01'), r.message_status
ORDER BY DATE_FORMAT(r.monat, '%Y-%m-01'), r.message_status;
SELECT DATE_FORMAT(r.monat, '%Y-%m-01') AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.monat >= '2025-01-01'
  AND r.monat <  '2026-01-01'
GROUP BY DATE_FORMAT(r.monat, '%Y-%m-01'), r.message_status
ORDER BY DATE_FORMAT(r.monat, '%Y-%m-01'), r.message_status;
SELECT DATE_FORMAT(r.monat, '%Y-%m-01') AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.monat >= '2025-01-01'
  AND r.monat <  '2026-01-01'
GROUP BY DATE_FORMAT(r.monat, '%Y-%m-01'), r.message_status
ORDER BY DATE_FORMAT(r.monat, '%Y-%m-01'), r.message_status;
SELECT DATE_FORMAT(r.monat, '%Y-%m-01') AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.monat >= '2025-01-01'
  AND r.monat <  '2026-01-01'
GROUP BY DATE_FORMAT(r.monat, '%Y-%m-01'), r.message_status
ORDER BY DATE_FORMAT(r.monat, '%Y-%m-01'), r.message_status;
SELECT DATE_FORMAT(r.monat, '%Y-%m-01') AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_monat r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'SUTTONS'
  AND r.monat >= '2025-01-01'
  AND r.monat <  '2026-01-01'
GROUP BY DATE_FORMAT(r.monat, '%Y-%m-01'), r.message_status
ORDER BY DATE_FORMAT(r.monat, '%Y-%m-01'), r.message_status;
SELECT 'verlauf-monat-dateformat-12monate-SUTTONS' AS fall,
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
-- verteilung-monat-12monate-SUTTONS
-- ==========================================================================
SELECT '=== verteilung-monat-12monate-SUTTONS · Plan (L15) ===' AS marke;
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
  FROM overlord_monitor.message_rollup_monat r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
  AND r.monat >= '2025-01-01'
  AND r.monat <  '2026-01-01'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END) t;
SELECT '=== verteilung-monat-12monate-SUTTONS · Aufwaermlauf + fuenf ===' AS marke;
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
  FROM overlord_monitor.message_rollup_monat r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
  AND r.monat >= '2025-01-01'
  AND r.monat <  '2026-01-01'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END) t;
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
  FROM overlord_monitor.message_rollup_monat r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
  AND r.monat >= '2025-01-01'
  AND r.monat <  '2026-01-01'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END) t;
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
  FROM overlord_monitor.message_rollup_monat r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
  AND r.monat >= '2025-01-01'
  AND r.monat <  '2026-01-01'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END) t;
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
  FROM overlord_monitor.message_rollup_monat r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
  AND r.monat >= '2025-01-01'
  AND r.monat <  '2026-01-01'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END) t;
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
  FROM overlord_monitor.message_rollup_monat r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
  AND r.monat >= '2025-01-01'
  AND r.monat <  '2026-01-01'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END) t;
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
  FROM overlord_monitor.message_rollup_monat r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
  AND r.monat >= '2025-01-01'
  AND r.monat <  '2026-01-01'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END) t;
SELECT 'verteilung-monat-12monate-SUTTONS' AS fall,
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
              ELSE c.partner END) t;
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
              ELSE c.partner END) t;
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
              ELSE c.partner END) t;
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
              ELSE c.partner END) t;
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
              ELSE c.partner END) t;
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
              ELSE c.partner END) t;
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
              ELSE c.partner END) t;
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

SELECT '=== 99 read_only (letzte Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;
