-- Messrunde M86-M92 — Sitzung 6b: M91 "Traegt eine Verteilung nach Partner ueberhaupt?"
--
-- Offener Punkt 39 aus messungen-schritt9.md: Ein einziger Prozess haelt 44,08 % aller
-- Nachrichten. Die Frage ist, ob das nach Kuratierung noch so aussieht.
--
-- G1 — HIER GILT G1 SCHAERFER ALS SONST, und das ist der Kern der Darstellung:
--   Es wird NIE ein Partnername ausgegeben. Die Verteilung erscheint als 'Partner 1',
--   'Partner 2', ... — die Rangfolge entsteht ueber ROW_NUMBER() im Server, der Name
--   verlaesst die innere Abfrage nicht. Die Zuordnung von Rang zu echtem Namen wird
--   NIRGENDS festgehalten, auch nicht in der Rohausgabe unter ergebnis/.
--   Die Richtung (EINGEHEND / AUSGEHEND) ist Konfigurationsvokabular und darf stehen.
--
-- E-i, woertlich umgesetzt: 'nicht zugeordnet' fasst zusammen
--   (a) Prozesse ohne Katalogzeile — 343 der 1.503 Prozesse haben keine (V2)
--   (b) pflegestatus = 'OFFEN'     — auch wenn REGEL_A einen Vorschlag hinterlegt hat
--   (c) GEPFLEGT mit leerem Partner
-- Daneben, NUR ZUR KENNTNIS, steht die Aufteilung dieser drei (Abfrage 3). E-i ist
-- entschieden; die Zahl sagt lediglich, wie gross der Unterschied gewesen waere.
--
-- Katalogstand aus V2 als Bezugsgroesse: 1.160 Zeilen, 770 GEPFLEGT, davon 554 mit
-- Partner, 321 verschiedene Partner, 343 Prozesse ohne Katalogzeile.
--
-- Mandanten: NEXANS, SUTTONS, VOTG.   Fenster A und B.
-- Ausschliesslich SELECT / SET / EXPLAIN. (Regel S1)   Z1: alle Grenzen als Literal.

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;

SET max_statement_time = 60;
SET profiling_history_size = 100;
SET profiling = 1;

SELECT '=== M91 NEXANS Fenster A — 1 Verteilung nach Partner (maskiert) ===' AS marke;
SELECT CASE WHEN nz = 1 THEN 'nicht zugeordnet'
            ELSE CONCAT('Partner ', ROW_NUMBER() OVER (PARTITION BY nz ORDER BY anzahl DESC)) END AS bezeichnung,
       anzahl,
       ROUND(100.0 * anzahl / SUM(anzahl) OVER (), 4) AS anteil_prozent
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'NEXANS'
    AND m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner
) t
ORDER BY nz, anzahl DESC
LIMIT 16;

SELECT '=== M91 NEXANS Fenster A — 2 Anteile ===' AS marke;
SELECT (SELECT SUM(anzahl) FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'NEXANS'
    AND m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner
) g)              AS nachrichten_gesamt,
       (SELECT SUM(anzahl) FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'NEXANS'
    AND m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner
) g WHERE nz = 1) AS nicht_zugeordnet,
       (SELECT COUNT(*)    FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'NEXANS'
    AND m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner
) g WHERE nz = 0) AS verschiedene_partner,
       MAX(CASE WHEN rang = 1  THEN kum END) AS top1,
       MAX(CASE WHEN rang = 3  THEN kum END) AS top3,
       MAX(CASE WHEN rang = 10 THEN kum END) AS top10
FROM (
  SELECT anzahl,
         ROW_NUMBER() OVER (ORDER BY anzahl DESC) AS rang,
         SUM(anzahl) OVER (ORDER BY anzahl DESC ROWS UNBOUNDED PRECEDING) AS kum
  FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'NEXANS'
    AND m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner
  ) t WHERE nz = 0
) r;

SELECT '=== M91 NEXANS Fenster A — 3 Aufteilung von nicht zugeordnet ===' AS marke;
SELECT CASE WHEN c.process_id IS NULL                THEN 'a) keine Katalogzeile'
            WHEN c.pflegestatus = 'OFFEN'            THEN 'b) OFFEN'
            WHEN c.partner IS NULL OR c.partner = '' THEN 'c) GEPFLEGT ohne Partner'
            ELSE 'd) zugeordnet' END AS herkunft,
       COUNT(*) AS nachrichten,
       COUNT(DISTINCT m.ProcessID) AS prozesse
FROM GlassfishDB.Message m
JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
WHERE pm.MandantID = 'NEXANS'
  AND m.MessageLastUpdate >= '2025-12-29 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
GROUP BY herkunft ORDER BY herkunft;

SELECT '=== M91 NEXANS Fenster A — 4 Verteilung nach Richtung ===' AS marke;
SELECT CASE WHEN c.process_id IS NULL     THEN 'nicht zugeordnet'
            WHEN c.pflegestatus = 'OFFEN' THEN 'nicht zugeordnet'
            WHEN c.richtung IS NULL       THEN 'nicht zugeordnet'
            ELSE c.richtung END AS richtung,
       COUNT(*) AS nachrichten,
       ROUND(100.0 * COUNT(*) / SUM(COUNT(*)) OVER (), 4) AS anteil_prozent
FROM GlassfishDB.Message m
JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
WHERE pm.MandantID = 'NEXANS'
  AND m.MessageLastUpdate >= '2025-12-29 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
GROUP BY richtung ORDER BY nachrichten DESC;

SELECT '=== M91 NEXANS Fenster B — 1 Verteilung nach Partner (maskiert) ===' AS marke;
SELECT CASE WHEN nz = 1 THEN 'nicht zugeordnet'
            ELSE CONCAT('Partner ', ROW_NUMBER() OVER (PARTITION BY nz ORDER BY anzahl DESC)) END AS bezeichnung,
       anzahl,
       ROUND(100.0 * anzahl / SUM(anzahl) OVER (), 4) AS anteil_prozent
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'NEXANS'
    AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner
) t
ORDER BY nz, anzahl DESC
LIMIT 16;

SELECT '=== M91 NEXANS Fenster B — 2 Anteile ===' AS marke;
SELECT (SELECT SUM(anzahl) FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'NEXANS'
    AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner
) g)              AS nachrichten_gesamt,
       (SELECT SUM(anzahl) FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'NEXANS'
    AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner
) g WHERE nz = 1) AS nicht_zugeordnet,
       (SELECT COUNT(*)    FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'NEXANS'
    AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner
) g WHERE nz = 0) AS verschiedene_partner,
       MAX(CASE WHEN rang = 1  THEN kum END) AS top1,
       MAX(CASE WHEN rang = 3  THEN kum END) AS top3,
       MAX(CASE WHEN rang = 10 THEN kum END) AS top10
FROM (
  SELECT anzahl,
         ROW_NUMBER() OVER (ORDER BY anzahl DESC) AS rang,
         SUM(anzahl) OVER (ORDER BY anzahl DESC ROWS UNBOUNDED PRECEDING) AS kum
  FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'NEXANS'
    AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner
  ) t WHERE nz = 0
) r;

SELECT '=== M91 NEXANS Fenster B — 3 Aufteilung von nicht zugeordnet ===' AS marke;
SELECT CASE WHEN c.process_id IS NULL                THEN 'a) keine Katalogzeile'
            WHEN c.pflegestatus = 'OFFEN'            THEN 'b) OFFEN'
            WHEN c.partner IS NULL OR c.partner = '' THEN 'c) GEPFLEGT ohne Partner'
            ELSE 'd) zugeordnet' END AS herkunft,
       COUNT(*) AS nachrichten,
       COUNT(DISTINCT m.ProcessID) AS prozesse
FROM GlassfishDB.Message m
JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
WHERE pm.MandantID = 'NEXANS'
  AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
GROUP BY herkunft ORDER BY herkunft;

SELECT '=== M91 NEXANS Fenster B — 4 Verteilung nach Richtung ===' AS marke;
SELECT CASE WHEN c.process_id IS NULL     THEN 'nicht zugeordnet'
            WHEN c.pflegestatus = 'OFFEN' THEN 'nicht zugeordnet'
            WHEN c.richtung IS NULL       THEN 'nicht zugeordnet'
            ELSE c.richtung END AS richtung,
       COUNT(*) AS nachrichten,
       ROUND(100.0 * COUNT(*) / SUM(COUNT(*)) OVER (), 4) AS anteil_prozent
FROM GlassfishDB.Message m
JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
WHERE pm.MandantID = 'NEXANS'
  AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
GROUP BY richtung ORDER BY nachrichten DESC;

SELECT '=== M91 SUTTONS Fenster A — 1 Verteilung nach Partner (maskiert) ===' AS marke;
SELECT CASE WHEN nz = 1 THEN 'nicht zugeordnet'
            ELSE CONCAT('Partner ', ROW_NUMBER() OVER (PARTITION BY nz ORDER BY anzahl DESC)) END AS bezeichnung,
       anzahl,
       ROUND(100.0 * anzahl / SUM(anzahl) OVER (), 4) AS anteil_prozent
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'SUTTONS'
    AND m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner
) t
ORDER BY nz, anzahl DESC
LIMIT 16;

SELECT '=== M91 SUTTONS Fenster A — 2 Anteile ===' AS marke;
SELECT (SELECT SUM(anzahl) FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'SUTTONS'
    AND m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner
) g)              AS nachrichten_gesamt,
       (SELECT SUM(anzahl) FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'SUTTONS'
    AND m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner
) g WHERE nz = 1) AS nicht_zugeordnet,
       (SELECT COUNT(*)    FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'SUTTONS'
    AND m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner
) g WHERE nz = 0) AS verschiedene_partner,
       MAX(CASE WHEN rang = 1  THEN kum END) AS top1,
       MAX(CASE WHEN rang = 3  THEN kum END) AS top3,
       MAX(CASE WHEN rang = 10 THEN kum END) AS top10
FROM (
  SELECT anzahl,
         ROW_NUMBER() OVER (ORDER BY anzahl DESC) AS rang,
         SUM(anzahl) OVER (ORDER BY anzahl DESC ROWS UNBOUNDED PRECEDING) AS kum
  FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'SUTTONS'
    AND m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner
  ) t WHERE nz = 0
) r;

SELECT '=== M91 SUTTONS Fenster A — 3 Aufteilung von nicht zugeordnet ===' AS marke;
SELECT CASE WHEN c.process_id IS NULL                THEN 'a) keine Katalogzeile'
            WHEN c.pflegestatus = 'OFFEN'            THEN 'b) OFFEN'
            WHEN c.partner IS NULL OR c.partner = '' THEN 'c) GEPFLEGT ohne Partner'
            ELSE 'd) zugeordnet' END AS herkunft,
       COUNT(*) AS nachrichten,
       COUNT(DISTINCT m.ProcessID) AS prozesse
FROM GlassfishDB.Message m
JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
WHERE pm.MandantID = 'SUTTONS'
  AND m.MessageLastUpdate >= '2025-12-29 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
GROUP BY herkunft ORDER BY herkunft;

SELECT '=== M91 SUTTONS Fenster A — 4 Verteilung nach Richtung ===' AS marke;
SELECT CASE WHEN c.process_id IS NULL     THEN 'nicht zugeordnet'
            WHEN c.pflegestatus = 'OFFEN' THEN 'nicht zugeordnet'
            WHEN c.richtung IS NULL       THEN 'nicht zugeordnet'
            ELSE c.richtung END AS richtung,
       COUNT(*) AS nachrichten,
       ROUND(100.0 * COUNT(*) / SUM(COUNT(*)) OVER (), 4) AS anteil_prozent
FROM GlassfishDB.Message m
JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
WHERE pm.MandantID = 'SUTTONS'
  AND m.MessageLastUpdate >= '2025-12-29 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
GROUP BY richtung ORDER BY nachrichten DESC;

SELECT '=== M91 SUTTONS Fenster B — 1 Verteilung nach Partner (maskiert) ===' AS marke;
SELECT CASE WHEN nz = 1 THEN 'nicht zugeordnet'
            ELSE CONCAT('Partner ', ROW_NUMBER() OVER (PARTITION BY nz ORDER BY anzahl DESC)) END AS bezeichnung,
       anzahl,
       ROUND(100.0 * anzahl / SUM(anzahl) OVER (), 4) AS anteil_prozent
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'SUTTONS'
    AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner
) t
ORDER BY nz, anzahl DESC
LIMIT 16;

SELECT '=== M91 SUTTONS Fenster B — 2 Anteile ===' AS marke;
SELECT (SELECT SUM(anzahl) FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'SUTTONS'
    AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner
) g)              AS nachrichten_gesamt,
       (SELECT SUM(anzahl) FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'SUTTONS'
    AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner
) g WHERE nz = 1) AS nicht_zugeordnet,
       (SELECT COUNT(*)    FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'SUTTONS'
    AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner
) g WHERE nz = 0) AS verschiedene_partner,
       MAX(CASE WHEN rang = 1  THEN kum END) AS top1,
       MAX(CASE WHEN rang = 3  THEN kum END) AS top3,
       MAX(CASE WHEN rang = 10 THEN kum END) AS top10
FROM (
  SELECT anzahl,
         ROW_NUMBER() OVER (ORDER BY anzahl DESC) AS rang,
         SUM(anzahl) OVER (ORDER BY anzahl DESC ROWS UNBOUNDED PRECEDING) AS kum
  FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'SUTTONS'
    AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner
  ) t WHERE nz = 0
) r;

SELECT '=== M91 SUTTONS Fenster B — 3 Aufteilung von nicht zugeordnet ===' AS marke;
SELECT CASE WHEN c.process_id IS NULL                THEN 'a) keine Katalogzeile'
            WHEN c.pflegestatus = 'OFFEN'            THEN 'b) OFFEN'
            WHEN c.partner IS NULL OR c.partner = '' THEN 'c) GEPFLEGT ohne Partner'
            ELSE 'd) zugeordnet' END AS herkunft,
       COUNT(*) AS nachrichten,
       COUNT(DISTINCT m.ProcessID) AS prozesse
FROM GlassfishDB.Message m
JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
WHERE pm.MandantID = 'SUTTONS'
  AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
GROUP BY herkunft ORDER BY herkunft;

SELECT '=== M91 SUTTONS Fenster B — 4 Verteilung nach Richtung ===' AS marke;
SELECT CASE WHEN c.process_id IS NULL     THEN 'nicht zugeordnet'
            WHEN c.pflegestatus = 'OFFEN' THEN 'nicht zugeordnet'
            WHEN c.richtung IS NULL       THEN 'nicht zugeordnet'
            ELSE c.richtung END AS richtung,
       COUNT(*) AS nachrichten,
       ROUND(100.0 * COUNT(*) / SUM(COUNT(*)) OVER (), 4) AS anteil_prozent
FROM GlassfishDB.Message m
JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
WHERE pm.MandantID = 'SUTTONS'
  AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
GROUP BY richtung ORDER BY nachrichten DESC;

SELECT '=== M91 VOTG Fenster A — 1 Verteilung nach Partner (maskiert) ===' AS marke;
SELECT CASE WHEN nz = 1 THEN 'nicht zugeordnet'
            ELSE CONCAT('Partner ', ROW_NUMBER() OVER (PARTITION BY nz ORDER BY anzahl DESC)) END AS bezeichnung,
       anzahl,
       ROUND(100.0 * anzahl / SUM(anzahl) OVER (), 4) AS anteil_prozent
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'VOTG'
    AND m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner
) t
ORDER BY nz, anzahl DESC
LIMIT 16;

SELECT '=== M91 VOTG Fenster A — 2 Anteile ===' AS marke;
SELECT (SELECT SUM(anzahl) FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'VOTG'
    AND m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner
) g)              AS nachrichten_gesamt,
       (SELECT SUM(anzahl) FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'VOTG'
    AND m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner
) g WHERE nz = 1) AS nicht_zugeordnet,
       (SELECT COUNT(*)    FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'VOTG'
    AND m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner
) g WHERE nz = 0) AS verschiedene_partner,
       MAX(CASE WHEN rang = 1  THEN kum END) AS top1,
       MAX(CASE WHEN rang = 3  THEN kum END) AS top3,
       MAX(CASE WHEN rang = 10 THEN kum END) AS top10
FROM (
  SELECT anzahl,
         ROW_NUMBER() OVER (ORDER BY anzahl DESC) AS rang,
         SUM(anzahl) OVER (ORDER BY anzahl DESC ROWS UNBOUNDED PRECEDING) AS kum
  FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'VOTG'
    AND m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner
  ) t WHERE nz = 0
) r;

SELECT '=== M91 VOTG Fenster A — 3 Aufteilung von nicht zugeordnet ===' AS marke;
SELECT CASE WHEN c.process_id IS NULL                THEN 'a) keine Katalogzeile'
            WHEN c.pflegestatus = 'OFFEN'            THEN 'b) OFFEN'
            WHEN c.partner IS NULL OR c.partner = '' THEN 'c) GEPFLEGT ohne Partner'
            ELSE 'd) zugeordnet' END AS herkunft,
       COUNT(*) AS nachrichten,
       COUNT(DISTINCT m.ProcessID) AS prozesse
FROM GlassfishDB.Message m
JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
WHERE pm.MandantID = 'VOTG'
  AND m.MessageLastUpdate >= '2025-12-29 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
GROUP BY herkunft ORDER BY herkunft;

SELECT '=== M91 VOTG Fenster A — 4 Verteilung nach Richtung ===' AS marke;
SELECT CASE WHEN c.process_id IS NULL     THEN 'nicht zugeordnet'
            WHEN c.pflegestatus = 'OFFEN' THEN 'nicht zugeordnet'
            WHEN c.richtung IS NULL       THEN 'nicht zugeordnet'
            ELSE c.richtung END AS richtung,
       COUNT(*) AS nachrichten,
       ROUND(100.0 * COUNT(*) / SUM(COUNT(*)) OVER (), 4) AS anteil_prozent
FROM GlassfishDB.Message m
JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
WHERE pm.MandantID = 'VOTG'
  AND m.MessageLastUpdate >= '2025-12-29 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
GROUP BY richtung ORDER BY nachrichten DESC;

SELECT '=== M91 VOTG Fenster B — 1 Verteilung nach Partner (maskiert) ===' AS marke;
SELECT CASE WHEN nz = 1 THEN 'nicht zugeordnet'
            ELSE CONCAT('Partner ', ROW_NUMBER() OVER (PARTITION BY nz ORDER BY anzahl DESC)) END AS bezeichnung,
       anzahl,
       ROUND(100.0 * anzahl / SUM(anzahl) OVER (), 4) AS anteil_prozent
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'VOTG'
    AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner
) t
ORDER BY nz, anzahl DESC
LIMIT 16;

SELECT '=== M91 VOTG Fenster B — 2 Anteile ===' AS marke;
SELECT (SELECT SUM(anzahl) FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'VOTG'
    AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner
) g)              AS nachrichten_gesamt,
       (SELECT SUM(anzahl) FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'VOTG'
    AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner
) g WHERE nz = 1) AS nicht_zugeordnet,
       (SELECT COUNT(*)    FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'VOTG'
    AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner
) g WHERE nz = 0) AS verschiedene_partner,
       MAX(CASE WHEN rang = 1  THEN kum END) AS top1,
       MAX(CASE WHEN rang = 3  THEN kum END) AS top3,
       MAX(CASE WHEN rang = 10 THEN kum END) AS top10
FROM (
  SELECT anzahl,
         ROW_NUMBER() OVER (ORDER BY anzahl DESC) AS rang,
         SUM(anzahl) OVER (ORDER BY anzahl DESC ROWS UNBOUNDED PRECEDING) AS kum
  FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'VOTG'
    AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner
  ) t WHERE nz = 0
) r;

SELECT '=== M91 VOTG Fenster B — 3 Aufteilung von nicht zugeordnet ===' AS marke;
SELECT CASE WHEN c.process_id IS NULL                THEN 'a) keine Katalogzeile'
            WHEN c.pflegestatus = 'OFFEN'            THEN 'b) OFFEN'
            WHEN c.partner IS NULL OR c.partner = '' THEN 'c) GEPFLEGT ohne Partner'
            ELSE 'd) zugeordnet' END AS herkunft,
       COUNT(*) AS nachrichten,
       COUNT(DISTINCT m.ProcessID) AS prozesse
FROM GlassfishDB.Message m
JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
WHERE pm.MandantID = 'VOTG'
  AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
GROUP BY herkunft ORDER BY herkunft;

SELECT '=== M91 VOTG Fenster B — 4 Verteilung nach Richtung ===' AS marke;
SELECT CASE WHEN c.process_id IS NULL     THEN 'nicht zugeordnet'
            WHEN c.pflegestatus = 'OFFEN' THEN 'nicht zugeordnet'
            WHEN c.richtung IS NULL       THEN 'nicht zugeordnet'
            ELSE c.richtung END AS richtung,
       COUNT(*) AS nachrichten,
       ROUND(100.0 * COUNT(*) / SUM(COUNT(*)) OVER (), 4) AS anteil_prozent
FROM GlassfishDB.Message m
JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
WHERE pm.MandantID = 'VOTG'
  AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
GROUP BY richtung ORDER BY nachrichten DESC;

-- ===== L15 — je Abfrageform ein Plan, an NEXANS/Fenster B =====

SELECT '=== EXPLAIN M91-1 Verteilung ===' AS marke;
EXPLAIN
SELECT CASE WHEN nz = 1 THEN 'nicht zugeordnet'
            ELSE CONCAT('Partner ', ROW_NUMBER() OVER (PARTITION BY nz ORDER BY anzahl DESC)) END AS bezeichnung,
       anzahl,
       ROUND(100.0 * anzahl / SUM(anzahl) OVER (), 4) AS anteil_prozent
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'NEXANS'
    AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner
) t
ORDER BY nz, anzahl DESC
LIMIT 16;

SELECT '=== EXPLAIN M91-3 Aufteilung ===' AS marke;
EXPLAIN
SELECT CASE WHEN c.process_id IS NULL                THEN 'a) keine Katalogzeile'
            WHEN c.pflegestatus = 'OFFEN'            THEN 'b) OFFEN'
            WHEN c.partner IS NULL OR c.partner = '' THEN 'c) GEPFLEGT ohne Partner'
            ELSE 'd) zugeordnet' END AS herkunft,
       COUNT(*) AS nachrichten,
       COUNT(DISTINCT m.ProcessID) AS prozesse
FROM GlassfishDB.Message m
JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
WHERE pm.MandantID = 'NEXANS'
  AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
GROUP BY herkunft ORDER BY herkunft;

SELECT '=== EXPLAIN M91-4 Richtung ===' AS marke;
EXPLAIN
SELECT CASE WHEN c.process_id IS NULL     THEN 'nicht zugeordnet'
            WHEN c.pflegestatus = 'OFFEN' THEN 'nicht zugeordnet'
            WHEN c.richtung IS NULL       THEN 'nicht zugeordnet'
            ELSE c.richtung END AS richtung,
       COUNT(*) AS nachrichten,
       ROUND(100.0 * COUNT(*) / SUM(COUNT(*)) OVER (), 4) AS anteil_prozent
FROM GlassfishDB.Message m
JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
WHERE pm.MandantID = 'NEXANS'
  AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
GROUP BY richtung ORDER BY nachrichten DESC;

SELECT '=== EXPLAIN M91-1 Gegenprobe SUTTONS Fenster A ===' AS marke;
EXPLAIN
SELECT CASE WHEN nz = 1 THEN 'nicht zugeordnet'
            ELSE CONCAT('Partner ', ROW_NUMBER() OVER (PARTITION BY nz ORDER BY anzahl DESC)) END AS bezeichnung,
       anzahl,
       ROUND(100.0 * anzahl / SUM(anzahl) OVER (), 4) AS anteil_prozent
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'SUTTONS'
    AND m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner
) t
ORDER BY nz, anzahl DESC
LIMIT 16;

SELECT '=== 98 SHOW PROFILES ===' AS marke;
SHOW PROFILES;
SELECT '=== 99 Abschluss ===' AS marke;
SELECT @@global.read_only AS read_only_ende, NOW() AS serverzeit_ende;
