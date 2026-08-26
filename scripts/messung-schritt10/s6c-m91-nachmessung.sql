-- Messrunde M86-M92 — Sitzung 6c: M91, Nachmessung nach einem Fehler in Sitzung 6b
--
-- WARUM ES DIESE SITZUNG GIBT. In Sitzung 6b hiess der Ausdrucksalias `partner`. MariaDB
-- loest GROUP BY zuerst gegen TABELLENSPALTEN auf und erst dann gegen Ausdrucksaliasse —
-- und process_catalog traegt eine Spalte, die ebenfalls `partner` heisst. `GROUP BY nz,
-- partner` hat deshalb nach c.partner gruppiert und nicht nach dem CASE. Folge: Der Eimer
-- `nicht zugeordnet` zerfiel bei VOTG in acht Zeilen, weil die dortigen OFFEN-Zeilen
-- verschiedene REGEL_A-Vorschlaege tragen. Die Anteilszahlen aus 6b waren davon NICHT
-- betroffen (sie zaehlen ueber nz), die Verteilungstabelle schon.
--
-- Hier heisst der Alias `partner_kuratiert`. Zusaetzlich erhoben: die Katalogabdeckung je
-- Mandant — sie ist die Bezugsgroesse, ohne die M91 nicht zu lesen ist.
--
-- G1 unveraendert scharf: kein Partnername verlaesst die innere Abfrage.
-- Ausschliesslich SELECT / SET / EXPLAIN. (Regel S1)   Z1: alle Grenzen als Literal.

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;

SET max_statement_time = 60;
SET profiling_history_size = 100;
SET profiling = 1;

SELECT '=== M91-0 Katalogabdeckung je Mandant (Bezugsgroesse) ===' AS marke;
SELECT pm.MandantID AS mandant,
       COUNT(*)                                                              AS prozesse,
       SUM(c.process_id IS NOT NULL)                                         AS mit_katalogzeile,
       SUM(c.pflegestatus = 'GEPFLEGT')                                      AS gepflegt,
       SUM(c.pflegestatus = 'GEPFLEGT' AND c.partner IS NOT NULL AND c.partner <> '') AS gepflegt_mit_partner,
       SUM(c.pflegestatus = 'OFFEN')                                         AS offen,
       COUNT(DISTINCT CASE WHEN c.pflegestatus = 'GEPFLEGT' AND c.partner IS NOT NULL
                             AND c.partner <> '' THEN c.partner END)         AS verschiedene_partner
FROM GlassfishDB.ProjectMandant pm
JOIN GlassfishDB.Project pr ON pr.ProjectID = pm.ProjectID
JOIN GlassfishDB.Process p  ON p.ProjectID  = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
GROUP BY pm.MandantID
ORDER BY prozesse DESC;

SELECT '=== M91-0b Prozesse ohne Projekt (V4: Process.ProjectID ist NULL-bar) ===' AS marke;
SELECT COUNT(*) AS prozesse_ohne_projekt FROM GlassfishDB.Process WHERE ProjectID IS NULL;

SELECT '=== M91-0c Prozesse, deren Projekt keine Mandantenzeile hat ===' AS marke;
SELECT COUNT(*) AS prozesse_ohne_mandant
FROM GlassfishDB.Process p
LEFT JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.ProjectID IS NULL;

SELECT '=== M91c NEXANS Fenster A — 1 Verteilung (korrigiert) ===' AS marke;
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
              ELSE c.partner END AS partner_kuratiert,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'NEXANS'
    AND m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner_kuratiert
) t
ORDER BY nz DESC, anzahl DESC
LIMIT 16;

SELECT '=== M91c NEXANS Fenster A — 2 Anteile (korrigiert) ===' AS marke;
SELECT (SELECT SUM(anzahl) FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner_kuratiert,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'NEXANS'
    AND m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner_kuratiert
) g)              AS nachrichten_gesamt,
       (SELECT SUM(anzahl) FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner_kuratiert,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'NEXANS'
    AND m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner_kuratiert
) g WHERE nz = 1) AS nicht_zugeordnet,
       (SELECT COUNT(*)    FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner_kuratiert,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'NEXANS'
    AND m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner_kuratiert
) g WHERE nz = 1) AS gruppen_nicht_zugeordnet,
       (SELECT COUNT(*)    FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner_kuratiert,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'NEXANS'
    AND m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner_kuratiert
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
              ELSE c.partner END AS partner_kuratiert,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'NEXANS'
    AND m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner_kuratiert
  ) t WHERE nz = 0
) r;

SELECT '=== M91c NEXANS Fenster B — 1 Verteilung (korrigiert) ===' AS marke;
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
              ELSE c.partner END AS partner_kuratiert,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'NEXANS'
    AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner_kuratiert
) t
ORDER BY nz DESC, anzahl DESC
LIMIT 16;

SELECT '=== M91c NEXANS Fenster B — 2 Anteile (korrigiert) ===' AS marke;
SELECT (SELECT SUM(anzahl) FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner_kuratiert,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'NEXANS'
    AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner_kuratiert
) g)              AS nachrichten_gesamt,
       (SELECT SUM(anzahl) FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner_kuratiert,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'NEXANS'
    AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner_kuratiert
) g WHERE nz = 1) AS nicht_zugeordnet,
       (SELECT COUNT(*)    FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner_kuratiert,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'NEXANS'
    AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner_kuratiert
) g WHERE nz = 1) AS gruppen_nicht_zugeordnet,
       (SELECT COUNT(*)    FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner_kuratiert,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'NEXANS'
    AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner_kuratiert
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
              ELSE c.partner END AS partner_kuratiert,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'NEXANS'
    AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner_kuratiert
  ) t WHERE nz = 0
) r;

SELECT '=== M91c SUTTONS Fenster A — 1 Verteilung (korrigiert) ===' AS marke;
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
              ELSE c.partner END AS partner_kuratiert,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'SUTTONS'
    AND m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner_kuratiert
) t
ORDER BY nz DESC, anzahl DESC
LIMIT 16;

SELECT '=== M91c SUTTONS Fenster A — 2 Anteile (korrigiert) ===' AS marke;
SELECT (SELECT SUM(anzahl) FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner_kuratiert,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'SUTTONS'
    AND m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner_kuratiert
) g)              AS nachrichten_gesamt,
       (SELECT SUM(anzahl) FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner_kuratiert,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'SUTTONS'
    AND m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner_kuratiert
) g WHERE nz = 1) AS nicht_zugeordnet,
       (SELECT COUNT(*)    FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner_kuratiert,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'SUTTONS'
    AND m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner_kuratiert
) g WHERE nz = 1) AS gruppen_nicht_zugeordnet,
       (SELECT COUNT(*)    FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner_kuratiert,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'SUTTONS'
    AND m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner_kuratiert
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
              ELSE c.partner END AS partner_kuratiert,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'SUTTONS'
    AND m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner_kuratiert
  ) t WHERE nz = 0
) r;

SELECT '=== M91c SUTTONS Fenster B — 1 Verteilung (korrigiert) ===' AS marke;
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
              ELSE c.partner END AS partner_kuratiert,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'SUTTONS'
    AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner_kuratiert
) t
ORDER BY nz DESC, anzahl DESC
LIMIT 16;

SELECT '=== M91c SUTTONS Fenster B — 2 Anteile (korrigiert) ===' AS marke;
SELECT (SELECT SUM(anzahl) FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner_kuratiert,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'SUTTONS'
    AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner_kuratiert
) g)              AS nachrichten_gesamt,
       (SELECT SUM(anzahl) FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner_kuratiert,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'SUTTONS'
    AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner_kuratiert
) g WHERE nz = 1) AS nicht_zugeordnet,
       (SELECT COUNT(*)    FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner_kuratiert,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'SUTTONS'
    AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner_kuratiert
) g WHERE nz = 1) AS gruppen_nicht_zugeordnet,
       (SELECT COUNT(*)    FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner_kuratiert,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'SUTTONS'
    AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner_kuratiert
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
              ELSE c.partner END AS partner_kuratiert,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'SUTTONS'
    AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner_kuratiert
  ) t WHERE nz = 0
) r;

SELECT '=== M91c VOTG Fenster A — 1 Verteilung (korrigiert) ===' AS marke;
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
              ELSE c.partner END AS partner_kuratiert,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'VOTG'
    AND m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner_kuratiert
) t
ORDER BY nz DESC, anzahl DESC
LIMIT 16;

SELECT '=== M91c VOTG Fenster A — 2 Anteile (korrigiert) ===' AS marke;
SELECT (SELECT SUM(anzahl) FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner_kuratiert,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'VOTG'
    AND m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner_kuratiert
) g)              AS nachrichten_gesamt,
       (SELECT SUM(anzahl) FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner_kuratiert,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'VOTG'
    AND m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner_kuratiert
) g WHERE nz = 1) AS nicht_zugeordnet,
       (SELECT COUNT(*)    FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner_kuratiert,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'VOTG'
    AND m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner_kuratiert
) g WHERE nz = 1) AS gruppen_nicht_zugeordnet,
       (SELECT COUNT(*)    FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner_kuratiert,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'VOTG'
    AND m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner_kuratiert
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
              ELSE c.partner END AS partner_kuratiert,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'VOTG'
    AND m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner_kuratiert
  ) t WHERE nz = 0
) r;

SELECT '=== M91c VOTG Fenster B — 1 Verteilung (korrigiert) ===' AS marke;
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
              ELSE c.partner END AS partner_kuratiert,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'VOTG'
    AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner_kuratiert
) t
ORDER BY nz DESC, anzahl DESC
LIMIT 16;

SELECT '=== M91c VOTG Fenster B — 2 Anteile (korrigiert) ===' AS marke;
SELECT (SELECT SUM(anzahl) FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner_kuratiert,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'VOTG'
    AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner_kuratiert
) g)              AS nachrichten_gesamt,
       (SELECT SUM(anzahl) FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner_kuratiert,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'VOTG'
    AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner_kuratiert
) g WHERE nz = 1) AS nicht_zugeordnet,
       (SELECT COUNT(*)    FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner_kuratiert,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'VOTG'
    AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner_kuratiert
) g WHERE nz = 1) AS gruppen_nicht_zugeordnet,
       (SELECT COUNT(*)    FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN 1
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 1
              WHEN c.partner IS NULL OR c.partner = '' THEN 1
              ELSE 0 END AS nz,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner_kuratiert,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'VOTG'
    AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner_kuratiert
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
              ELSE c.partner END AS partner_kuratiert,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'VOTG'
    AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner_kuratiert
  ) t WHERE nz = 0
) r;

SELECT '=== EXPLAIN M91c-1 NEXANS Fenster B ===' AS marke;
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
              ELSE c.partner END AS partner_kuratiert,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'NEXANS'
    AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY nz, partner_kuratiert
) t
ORDER BY nz DESC, anzahl DESC
LIMIT 16;

SELECT '=== 98 SHOW PROFILES ===' AS marke;
SHOW PROFILES;
SELECT '=== 99 Abschluss ===' AS marke;
SELECT @@global.read_only AS read_only_ende, NOW() AS serverzeit_ende;
