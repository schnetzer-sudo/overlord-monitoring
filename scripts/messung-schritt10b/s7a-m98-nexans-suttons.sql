-- Messrunde M94-M98 — Sitzung 7a: M98, Top-10 mit zwei Restzeilen (NEXANS und SUTTONS)
-- Auftrag:  "Messrunde vor Schritt 10b", Stand 27.08.2026
-- Ergebnis: docs/messungen-schritt10b.md
--
-- Ausschliesslich SELECT / SET / EXPLAIN. (Regel S1)
--
-- G1: Der Partnername verlaesst die innere Abfrage NICHT. Nach aussen dringt
--     `Partner <Rang>` — die Maske entsteht ueber ROW_NUMBER() im Statement
--     selbst, nicht durch Abschreiben. Die Zuordnung zu echten Namen ist
--     nirgends festgehalten. Die Richtung (EINGEHEND/AUSGEHEND) ist
--     Konfigurationsvokabular und darf im Klartext stehen.
--
-- Z1: Fenster ist das Paar `30 Tage / Tag`, hergeleitet aus dem Anker
--     2025-12-30 04:09:47 (V5): 2025-12-01 00:00:00 bis 2025-12-31 00:00:00.
--     Die Verteilung gruppiert ueber das ganze Fenster, nicht je Tag.
--
-- E-i: `nicht zugeordnet` fasst drei Ursachen zusammen — keine Katalogzeile,
--     pflegestatus <> GEPFLEGT, gepflegt mit leerem Partner. Es ist keine
--     Rangposition und faellt deshalb nie in `Uebrige` (M91, Punkt 39).
--
-- Befund 11 vorgebeugt: kein Alias heisst wie eine Tabellenspalte.
--
-- @@div_precision_increment steht auf 4; Anteile sind deshalb als
--     100.0000 * SUM(...) / SUM(...) gerechnet und nicht als AVG.

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;

SET max_statement_time = 60;
SET profiling = 1;
SET profiling_history_size = 100;


-- ══ M98 · NEXANS · 30 Tage ══════════════════════════════════════════
SELECT '=== M98 NEXANS Plan Partnergruppen ===' AS marke;
EXPLAIN SELECT x.gruppe,
       COUNT(*)                                                  AS partner,
       SUM(x.nachrichten)                                        AS nachrichten,
       ROUND(100.0000 * SUM(x.nachrichten)
             / SUM(SUM(x.nachrichten)) OVER (), 4)               AS anteil_prozent
FROM (
  SELECT g.nachrichten,
         CASE WHEN g.schluessel IS NULL THEN 'nicht zugeordnet'
              WHEN ROW_NUMBER() OVER (ORDER BY (g.schluessel IS NULL),
                                               g.nachrichten DESC) <= 10 THEN 'Top 10'
              ELSE 'Uebrige' END AS gruppe
  FROM (
    SELECT CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS schluessel,
           SUM(r.anzahl) AS nachrichten
    FROM overlord_monitor.message_rollup r
    JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
    JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
    JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
    LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
    WHERE pm.MandantID = 'NEXANS'
      AND r.stunde >= '2025-12-01 00:00:00'
      AND r.stunde <  '2025-12-31 00:00:00'
    GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END
  ) g
) x
GROUP BY x.gruppe
ORDER BY nachrichten DESC;

SELECT '=== M98 NEXANS Rangliste, maskiert (Top 10 plus zwei) ===' AS marke;
SELECT CASE WHEN g.schluessel IS NULL THEN 'nicht zugeordnet'
            ELSE CONCAT('Partner ', ROW_NUMBER() OVER (
                 ORDER BY (g.schluessel IS NULL), g.nachrichten DESC)) END AS bezeichnung,
       g.nachrichten,
       ROUND(100.0000 * g.nachrichten / SUM(g.nachrichten) OVER (), 4) AS anteil_prozent
FROM (
    SELECT CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS schluessel,
           SUM(r.anzahl) AS nachrichten
    FROM overlord_monitor.message_rollup r
    JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
    JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
    JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
    LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
    WHERE pm.MandantID = 'NEXANS'
      AND r.stunde >= '2025-12-01 00:00:00'
      AND r.stunde <  '2025-12-31 00:00:00'
    GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END
) g
ORDER BY (g.schluessel IS NULL), g.nachrichten DESC
LIMIT 13;

SELECT '=== M98 NEXANS Top 10 / Uebrige / nicht zugeordnet ===' AS marke;
SELECT x.gruppe,
       COUNT(*)                                                  AS partner,
       SUM(x.nachrichten)                                        AS nachrichten,
       ROUND(100.0000 * SUM(x.nachrichten)
             / SUM(SUM(x.nachrichten)) OVER (), 4)               AS anteil_prozent
FROM (
  SELECT g.nachrichten,
         CASE WHEN g.schluessel IS NULL THEN 'nicht zugeordnet'
              WHEN ROW_NUMBER() OVER (ORDER BY (g.schluessel IS NULL),
                                               g.nachrichten DESC) <= 10 THEN 'Top 10'
              ELSE 'Uebrige' END AS gruppe
  FROM (
    SELECT CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS schluessel,
           SUM(r.anzahl) AS nachrichten
    FROM overlord_monitor.message_rollup r
    JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
    JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
    JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
    LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
    WHERE pm.MandantID = 'NEXANS'
      AND r.stunde >= '2025-12-01 00:00:00'
      AND r.stunde <  '2025-12-31 00:00:00'
    GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END
  ) g
) x
GROUP BY x.gruppe
ORDER BY nachrichten DESC;

SELECT '=== M98 NEXANS Laufzeit Partnergruppen: Aufwaermlauf + fuenf ===' AS marke;
SELECT x.gruppe,
       COUNT(*)                                                  AS partner,
       SUM(x.nachrichten)                                        AS nachrichten,
       ROUND(100.0000 * SUM(x.nachrichten)
             / SUM(SUM(x.nachrichten)) OVER (), 4)               AS anteil_prozent
FROM (
  SELECT g.nachrichten,
         CASE WHEN g.schluessel IS NULL THEN 'nicht zugeordnet'
              WHEN ROW_NUMBER() OVER (ORDER BY (g.schluessel IS NULL),
                                               g.nachrichten DESC) <= 10 THEN 'Top 10'
              ELSE 'Uebrige' END AS gruppe
  FROM (
    SELECT CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS schluessel,
           SUM(r.anzahl) AS nachrichten
    FROM overlord_monitor.message_rollup r
    JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
    JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
    JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
    LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
    WHERE pm.MandantID = 'NEXANS'
      AND r.stunde >= '2025-12-01 00:00:00'
      AND r.stunde <  '2025-12-31 00:00:00'
    GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END
  ) g
) x
GROUP BY x.gruppe
ORDER BY nachrichten DESC;
SELECT x.gruppe,
       COUNT(*)                                                  AS partner,
       SUM(x.nachrichten)                                        AS nachrichten,
       ROUND(100.0000 * SUM(x.nachrichten)
             / SUM(SUM(x.nachrichten)) OVER (), 4)               AS anteil_prozent
FROM (
  SELECT g.nachrichten,
         CASE WHEN g.schluessel IS NULL THEN 'nicht zugeordnet'
              WHEN ROW_NUMBER() OVER (ORDER BY (g.schluessel IS NULL),
                                               g.nachrichten DESC) <= 10 THEN 'Top 10'
              ELSE 'Uebrige' END AS gruppe
  FROM (
    SELECT CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS schluessel,
           SUM(r.anzahl) AS nachrichten
    FROM overlord_monitor.message_rollup r
    JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
    JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
    JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
    LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
    WHERE pm.MandantID = 'NEXANS'
      AND r.stunde >= '2025-12-01 00:00:00'
      AND r.stunde <  '2025-12-31 00:00:00'
    GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END
  ) g
) x
GROUP BY x.gruppe
ORDER BY nachrichten DESC;
SELECT x.gruppe,
       COUNT(*)                                                  AS partner,
       SUM(x.nachrichten)                                        AS nachrichten,
       ROUND(100.0000 * SUM(x.nachrichten)
             / SUM(SUM(x.nachrichten)) OVER (), 4)               AS anteil_prozent
FROM (
  SELECT g.nachrichten,
         CASE WHEN g.schluessel IS NULL THEN 'nicht zugeordnet'
              WHEN ROW_NUMBER() OVER (ORDER BY (g.schluessel IS NULL),
                                               g.nachrichten DESC) <= 10 THEN 'Top 10'
              ELSE 'Uebrige' END AS gruppe
  FROM (
    SELECT CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS schluessel,
           SUM(r.anzahl) AS nachrichten
    FROM overlord_monitor.message_rollup r
    JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
    JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
    JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
    LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
    WHERE pm.MandantID = 'NEXANS'
      AND r.stunde >= '2025-12-01 00:00:00'
      AND r.stunde <  '2025-12-31 00:00:00'
    GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END
  ) g
) x
GROUP BY x.gruppe
ORDER BY nachrichten DESC;
SELECT x.gruppe,
       COUNT(*)                                                  AS partner,
       SUM(x.nachrichten)                                        AS nachrichten,
       ROUND(100.0000 * SUM(x.nachrichten)
             / SUM(SUM(x.nachrichten)) OVER (), 4)               AS anteil_prozent
FROM (
  SELECT g.nachrichten,
         CASE WHEN g.schluessel IS NULL THEN 'nicht zugeordnet'
              WHEN ROW_NUMBER() OVER (ORDER BY (g.schluessel IS NULL),
                                               g.nachrichten DESC) <= 10 THEN 'Top 10'
              ELSE 'Uebrige' END AS gruppe
  FROM (
    SELECT CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS schluessel,
           SUM(r.anzahl) AS nachrichten
    FROM overlord_monitor.message_rollup r
    JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
    JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
    JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
    LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
    WHERE pm.MandantID = 'NEXANS'
      AND r.stunde >= '2025-12-01 00:00:00'
      AND r.stunde <  '2025-12-31 00:00:00'
    GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END
  ) g
) x
GROUP BY x.gruppe
ORDER BY nachrichten DESC;
SELECT x.gruppe,
       COUNT(*)                                                  AS partner,
       SUM(x.nachrichten)                                        AS nachrichten,
       ROUND(100.0000 * SUM(x.nachrichten)
             / SUM(SUM(x.nachrichten)) OVER (), 4)               AS anteil_prozent
FROM (
  SELECT g.nachrichten,
         CASE WHEN g.schluessel IS NULL THEN 'nicht zugeordnet'
              WHEN ROW_NUMBER() OVER (ORDER BY (g.schluessel IS NULL),
                                               g.nachrichten DESC) <= 10 THEN 'Top 10'
              ELSE 'Uebrige' END AS gruppe
  FROM (
    SELECT CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS schluessel,
           SUM(r.anzahl) AS nachrichten
    FROM overlord_monitor.message_rollup r
    JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
    JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
    JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
    LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
    WHERE pm.MandantID = 'NEXANS'
      AND r.stunde >= '2025-12-01 00:00:00'
      AND r.stunde <  '2025-12-31 00:00:00'
    GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END
  ) g
) x
GROUP BY x.gruppe
ORDER BY nachrichten DESC;
SELECT x.gruppe,
       COUNT(*)                                                  AS partner,
       SUM(x.nachrichten)                                        AS nachrichten,
       ROUND(100.0000 * SUM(x.nachrichten)
             / SUM(SUM(x.nachrichten)) OVER (), 4)               AS anteil_prozent
FROM (
  SELECT g.nachrichten,
         CASE WHEN g.schluessel IS NULL THEN 'nicht zugeordnet'
              WHEN ROW_NUMBER() OVER (ORDER BY (g.schluessel IS NULL),
                                               g.nachrichten DESC) <= 10 THEN 'Top 10'
              ELSE 'Uebrige' END AS gruppe
  FROM (
    SELECT CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS schluessel,
           SUM(r.anzahl) AS nachrichten
    FROM overlord_monitor.message_rollup r
    JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
    JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
    JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
    LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
    WHERE pm.MandantID = 'NEXANS'
      AND r.stunde >= '2025-12-01 00:00:00'
      AND r.stunde <  '2025-12-31 00:00:00'
    GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END
  ) g
) x
GROUP BY x.gruppe
ORDER BY nachrichten DESC;

SELECT '=== M98 NEXANS Plan Richtung ===' AS marke;
EXPLAIN SELECT COALESCE(g.schluessel, 'nicht zugeordnet') AS richtung_wert,
       g.nachrichten,
       ROUND(100.0000 * g.nachrichten / SUM(g.nachrichten) OVER (), 4) AS anteil_prozent
FROM (
    SELECT CASE WHEN c.process_id IS NULL                  THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'          THEN NULL
              WHEN c.richtung IS NULL OR c.richtung = '' THEN NULL
              ELSE c.richtung END AS schluessel,
           SUM(r.anzahl) AS nachrichten
    FROM overlord_monitor.message_rollup r
    JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
    JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
    JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
    LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
    WHERE pm.MandantID = 'NEXANS'
      AND r.stunde >= '2025-12-01 00:00:00'
      AND r.stunde <  '2025-12-31 00:00:00'
    GROUP BY CASE WHEN c.process_id IS NULL                  THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'          THEN NULL
              WHEN c.richtung IS NULL OR c.richtung = '' THEN NULL
              ELSE c.richtung END
) g
ORDER BY g.nachrichten DESC;

SELECT '=== M98 NEXANS Verteilung nach Richtung ===' AS marke;
SELECT COALESCE(g.schluessel, 'nicht zugeordnet') AS richtung_wert,
       g.nachrichten,
       ROUND(100.0000 * g.nachrichten / SUM(g.nachrichten) OVER (), 4) AS anteil_prozent
FROM (
    SELECT CASE WHEN c.process_id IS NULL                  THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'          THEN NULL
              WHEN c.richtung IS NULL OR c.richtung = '' THEN NULL
              ELSE c.richtung END AS schluessel,
           SUM(r.anzahl) AS nachrichten
    FROM overlord_monitor.message_rollup r
    JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
    JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
    JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
    LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
    WHERE pm.MandantID = 'NEXANS'
      AND r.stunde >= '2025-12-01 00:00:00'
      AND r.stunde <  '2025-12-31 00:00:00'
    GROUP BY CASE WHEN c.process_id IS NULL                  THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'          THEN NULL
              WHEN c.richtung IS NULL OR c.richtung = '' THEN NULL
              ELSE c.richtung END
) g
ORDER BY g.nachrichten DESC;

SELECT '=== M98 NEXANS Laufzeit Richtung: Aufwaermlauf + fuenf ===' AS marke;
SELECT COALESCE(g.schluessel, 'nicht zugeordnet') AS richtung_wert,
       g.nachrichten,
       ROUND(100.0000 * g.nachrichten / SUM(g.nachrichten) OVER (), 4) AS anteil_prozent
FROM (
    SELECT CASE WHEN c.process_id IS NULL                  THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'          THEN NULL
              WHEN c.richtung IS NULL OR c.richtung = '' THEN NULL
              ELSE c.richtung END AS schluessel,
           SUM(r.anzahl) AS nachrichten
    FROM overlord_monitor.message_rollup r
    JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
    JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
    JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
    LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
    WHERE pm.MandantID = 'NEXANS'
      AND r.stunde >= '2025-12-01 00:00:00'
      AND r.stunde <  '2025-12-31 00:00:00'
    GROUP BY CASE WHEN c.process_id IS NULL                  THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'          THEN NULL
              WHEN c.richtung IS NULL OR c.richtung = '' THEN NULL
              ELSE c.richtung END
) g
ORDER BY g.nachrichten DESC;
SELECT COALESCE(g.schluessel, 'nicht zugeordnet') AS richtung_wert,
       g.nachrichten,
       ROUND(100.0000 * g.nachrichten / SUM(g.nachrichten) OVER (), 4) AS anteil_prozent
FROM (
    SELECT CASE WHEN c.process_id IS NULL                  THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'          THEN NULL
              WHEN c.richtung IS NULL OR c.richtung = '' THEN NULL
              ELSE c.richtung END AS schluessel,
           SUM(r.anzahl) AS nachrichten
    FROM overlord_monitor.message_rollup r
    JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
    JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
    JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
    LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
    WHERE pm.MandantID = 'NEXANS'
      AND r.stunde >= '2025-12-01 00:00:00'
      AND r.stunde <  '2025-12-31 00:00:00'
    GROUP BY CASE WHEN c.process_id IS NULL                  THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'          THEN NULL
              WHEN c.richtung IS NULL OR c.richtung = '' THEN NULL
              ELSE c.richtung END
) g
ORDER BY g.nachrichten DESC;
SELECT COALESCE(g.schluessel, 'nicht zugeordnet') AS richtung_wert,
       g.nachrichten,
       ROUND(100.0000 * g.nachrichten / SUM(g.nachrichten) OVER (), 4) AS anteil_prozent
FROM (
    SELECT CASE WHEN c.process_id IS NULL                  THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'          THEN NULL
              WHEN c.richtung IS NULL OR c.richtung = '' THEN NULL
              ELSE c.richtung END AS schluessel,
           SUM(r.anzahl) AS nachrichten
    FROM overlord_monitor.message_rollup r
    JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
    JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
    JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
    LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
    WHERE pm.MandantID = 'NEXANS'
      AND r.stunde >= '2025-12-01 00:00:00'
      AND r.stunde <  '2025-12-31 00:00:00'
    GROUP BY CASE WHEN c.process_id IS NULL                  THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'          THEN NULL
              WHEN c.richtung IS NULL OR c.richtung = '' THEN NULL
              ELSE c.richtung END
) g
ORDER BY g.nachrichten DESC;
SELECT COALESCE(g.schluessel, 'nicht zugeordnet') AS richtung_wert,
       g.nachrichten,
       ROUND(100.0000 * g.nachrichten / SUM(g.nachrichten) OVER (), 4) AS anteil_prozent
FROM (
    SELECT CASE WHEN c.process_id IS NULL                  THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'          THEN NULL
              WHEN c.richtung IS NULL OR c.richtung = '' THEN NULL
              ELSE c.richtung END AS schluessel,
           SUM(r.anzahl) AS nachrichten
    FROM overlord_monitor.message_rollup r
    JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
    JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
    JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
    LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
    WHERE pm.MandantID = 'NEXANS'
      AND r.stunde >= '2025-12-01 00:00:00'
      AND r.stunde <  '2025-12-31 00:00:00'
    GROUP BY CASE WHEN c.process_id IS NULL                  THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'          THEN NULL
              WHEN c.richtung IS NULL OR c.richtung = '' THEN NULL
              ELSE c.richtung END
) g
ORDER BY g.nachrichten DESC;
SELECT COALESCE(g.schluessel, 'nicht zugeordnet') AS richtung_wert,
       g.nachrichten,
       ROUND(100.0000 * g.nachrichten / SUM(g.nachrichten) OVER (), 4) AS anteil_prozent
FROM (
    SELECT CASE WHEN c.process_id IS NULL                  THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'          THEN NULL
              WHEN c.richtung IS NULL OR c.richtung = '' THEN NULL
              ELSE c.richtung END AS schluessel,
           SUM(r.anzahl) AS nachrichten
    FROM overlord_monitor.message_rollup r
    JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
    JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
    JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
    LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
    WHERE pm.MandantID = 'NEXANS'
      AND r.stunde >= '2025-12-01 00:00:00'
      AND r.stunde <  '2025-12-31 00:00:00'
    GROUP BY CASE WHEN c.process_id IS NULL                  THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'          THEN NULL
              WHEN c.richtung IS NULL OR c.richtung = '' THEN NULL
              ELSE c.richtung END
) g
ORDER BY g.nachrichten DESC;
SELECT COALESCE(g.schluessel, 'nicht zugeordnet') AS richtung_wert,
       g.nachrichten,
       ROUND(100.0000 * g.nachrichten / SUM(g.nachrichten) OVER (), 4) AS anteil_prozent
FROM (
    SELECT CASE WHEN c.process_id IS NULL                  THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'          THEN NULL
              WHEN c.richtung IS NULL OR c.richtung = '' THEN NULL
              ELSE c.richtung END AS schluessel,
           SUM(r.anzahl) AS nachrichten
    FROM overlord_monitor.message_rollup r
    JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
    JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
    JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
    LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
    WHERE pm.MandantID = 'NEXANS'
      AND r.stunde >= '2025-12-01 00:00:00'
      AND r.stunde <  '2025-12-31 00:00:00'
    GROUP BY CASE WHEN c.process_id IS NULL                  THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'          THEN NULL
              WHEN c.richtung IS NULL OR c.richtung = '' THEN NULL
              ELSE c.richtung END
) g
ORDER BY g.nachrichten DESC;

-- ══ M98 · SUTTONS · 30 Tage ══════════════════════════════════════════
SELECT '=== M98 SUTTONS Plan Partnergruppen ===' AS marke;
EXPLAIN SELECT x.gruppe,
       COUNT(*)                                                  AS partner,
       SUM(x.nachrichten)                                        AS nachrichten,
       ROUND(100.0000 * SUM(x.nachrichten)
             / SUM(SUM(x.nachrichten)) OVER (), 4)               AS anteil_prozent
FROM (
  SELECT g.nachrichten,
         CASE WHEN g.schluessel IS NULL THEN 'nicht zugeordnet'
              WHEN ROW_NUMBER() OVER (ORDER BY (g.schluessel IS NULL),
                                               g.nachrichten DESC) <= 10 THEN 'Top 10'
              ELSE 'Uebrige' END AS gruppe
  FROM (
    SELECT CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS schluessel,
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
  ) g
) x
GROUP BY x.gruppe
ORDER BY nachrichten DESC;

SELECT '=== M98 SUTTONS Rangliste, maskiert (Top 10 plus zwei) ===' AS marke;
SELECT CASE WHEN g.schluessel IS NULL THEN 'nicht zugeordnet'
            ELSE CONCAT('Partner ', ROW_NUMBER() OVER (
                 ORDER BY (g.schluessel IS NULL), g.nachrichten DESC)) END AS bezeichnung,
       g.nachrichten,
       ROUND(100.0000 * g.nachrichten / SUM(g.nachrichten) OVER (), 4) AS anteil_prozent
FROM (
    SELECT CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS schluessel,
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
) g
ORDER BY (g.schluessel IS NULL), g.nachrichten DESC
LIMIT 13;

SELECT '=== M98 SUTTONS Top 10 / Uebrige / nicht zugeordnet ===' AS marke;
SELECT x.gruppe,
       COUNT(*)                                                  AS partner,
       SUM(x.nachrichten)                                        AS nachrichten,
       ROUND(100.0000 * SUM(x.nachrichten)
             / SUM(SUM(x.nachrichten)) OVER (), 4)               AS anteil_prozent
FROM (
  SELECT g.nachrichten,
         CASE WHEN g.schluessel IS NULL THEN 'nicht zugeordnet'
              WHEN ROW_NUMBER() OVER (ORDER BY (g.schluessel IS NULL),
                                               g.nachrichten DESC) <= 10 THEN 'Top 10'
              ELSE 'Uebrige' END AS gruppe
  FROM (
    SELECT CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS schluessel,
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
  ) g
) x
GROUP BY x.gruppe
ORDER BY nachrichten DESC;

SELECT '=== M98 SUTTONS Laufzeit Partnergruppen: Aufwaermlauf + fuenf ===' AS marke;
SELECT x.gruppe,
       COUNT(*)                                                  AS partner,
       SUM(x.nachrichten)                                        AS nachrichten,
       ROUND(100.0000 * SUM(x.nachrichten)
             / SUM(SUM(x.nachrichten)) OVER (), 4)               AS anteil_prozent
FROM (
  SELECT g.nachrichten,
         CASE WHEN g.schluessel IS NULL THEN 'nicht zugeordnet'
              WHEN ROW_NUMBER() OVER (ORDER BY (g.schluessel IS NULL),
                                               g.nachrichten DESC) <= 10 THEN 'Top 10'
              ELSE 'Uebrige' END AS gruppe
  FROM (
    SELECT CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS schluessel,
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
  ) g
) x
GROUP BY x.gruppe
ORDER BY nachrichten DESC;
SELECT x.gruppe,
       COUNT(*)                                                  AS partner,
       SUM(x.nachrichten)                                        AS nachrichten,
       ROUND(100.0000 * SUM(x.nachrichten)
             / SUM(SUM(x.nachrichten)) OVER (), 4)               AS anteil_prozent
FROM (
  SELECT g.nachrichten,
         CASE WHEN g.schluessel IS NULL THEN 'nicht zugeordnet'
              WHEN ROW_NUMBER() OVER (ORDER BY (g.schluessel IS NULL),
                                               g.nachrichten DESC) <= 10 THEN 'Top 10'
              ELSE 'Uebrige' END AS gruppe
  FROM (
    SELECT CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS schluessel,
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
  ) g
) x
GROUP BY x.gruppe
ORDER BY nachrichten DESC;
SELECT x.gruppe,
       COUNT(*)                                                  AS partner,
       SUM(x.nachrichten)                                        AS nachrichten,
       ROUND(100.0000 * SUM(x.nachrichten)
             / SUM(SUM(x.nachrichten)) OVER (), 4)               AS anteil_prozent
FROM (
  SELECT g.nachrichten,
         CASE WHEN g.schluessel IS NULL THEN 'nicht zugeordnet'
              WHEN ROW_NUMBER() OVER (ORDER BY (g.schluessel IS NULL),
                                               g.nachrichten DESC) <= 10 THEN 'Top 10'
              ELSE 'Uebrige' END AS gruppe
  FROM (
    SELECT CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS schluessel,
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
  ) g
) x
GROUP BY x.gruppe
ORDER BY nachrichten DESC;
SELECT x.gruppe,
       COUNT(*)                                                  AS partner,
       SUM(x.nachrichten)                                        AS nachrichten,
       ROUND(100.0000 * SUM(x.nachrichten)
             / SUM(SUM(x.nachrichten)) OVER (), 4)               AS anteil_prozent
FROM (
  SELECT g.nachrichten,
         CASE WHEN g.schluessel IS NULL THEN 'nicht zugeordnet'
              WHEN ROW_NUMBER() OVER (ORDER BY (g.schluessel IS NULL),
                                               g.nachrichten DESC) <= 10 THEN 'Top 10'
              ELSE 'Uebrige' END AS gruppe
  FROM (
    SELECT CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS schluessel,
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
  ) g
) x
GROUP BY x.gruppe
ORDER BY nachrichten DESC;
SELECT x.gruppe,
       COUNT(*)                                                  AS partner,
       SUM(x.nachrichten)                                        AS nachrichten,
       ROUND(100.0000 * SUM(x.nachrichten)
             / SUM(SUM(x.nachrichten)) OVER (), 4)               AS anteil_prozent
FROM (
  SELECT g.nachrichten,
         CASE WHEN g.schluessel IS NULL THEN 'nicht zugeordnet'
              WHEN ROW_NUMBER() OVER (ORDER BY (g.schluessel IS NULL),
                                               g.nachrichten DESC) <= 10 THEN 'Top 10'
              ELSE 'Uebrige' END AS gruppe
  FROM (
    SELECT CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS schluessel,
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
  ) g
) x
GROUP BY x.gruppe
ORDER BY nachrichten DESC;
SELECT x.gruppe,
       COUNT(*)                                                  AS partner,
       SUM(x.nachrichten)                                        AS nachrichten,
       ROUND(100.0000 * SUM(x.nachrichten)
             / SUM(SUM(x.nachrichten)) OVER (), 4)               AS anteil_prozent
FROM (
  SELECT g.nachrichten,
         CASE WHEN g.schluessel IS NULL THEN 'nicht zugeordnet'
              WHEN ROW_NUMBER() OVER (ORDER BY (g.schluessel IS NULL),
                                               g.nachrichten DESC) <= 10 THEN 'Top 10'
              ELSE 'Uebrige' END AS gruppe
  FROM (
    SELECT CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS schluessel,
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
  ) g
) x
GROUP BY x.gruppe
ORDER BY nachrichten DESC;

SELECT '=== M98 SUTTONS Plan Richtung ===' AS marke;
EXPLAIN SELECT COALESCE(g.schluessel, 'nicht zugeordnet') AS richtung_wert,
       g.nachrichten,
       ROUND(100.0000 * g.nachrichten / SUM(g.nachrichten) OVER (), 4) AS anteil_prozent
FROM (
    SELECT CASE WHEN c.process_id IS NULL                  THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'          THEN NULL
              WHEN c.richtung IS NULL OR c.richtung = '' THEN NULL
              ELSE c.richtung END AS schluessel,
           SUM(r.anzahl) AS nachrichten
    FROM overlord_monitor.message_rollup r
    JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
    JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
    JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
    LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
    WHERE pm.MandantID = 'SUTTONS'
      AND r.stunde >= '2025-12-01 00:00:00'
      AND r.stunde <  '2025-12-31 00:00:00'
    GROUP BY CASE WHEN c.process_id IS NULL                  THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'          THEN NULL
              WHEN c.richtung IS NULL OR c.richtung = '' THEN NULL
              ELSE c.richtung END
) g
ORDER BY g.nachrichten DESC;

SELECT '=== M98 SUTTONS Verteilung nach Richtung ===' AS marke;
SELECT COALESCE(g.schluessel, 'nicht zugeordnet') AS richtung_wert,
       g.nachrichten,
       ROUND(100.0000 * g.nachrichten / SUM(g.nachrichten) OVER (), 4) AS anteil_prozent
FROM (
    SELECT CASE WHEN c.process_id IS NULL                  THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'          THEN NULL
              WHEN c.richtung IS NULL OR c.richtung = '' THEN NULL
              ELSE c.richtung END AS schluessel,
           SUM(r.anzahl) AS nachrichten
    FROM overlord_monitor.message_rollup r
    JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
    JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
    JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
    LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
    WHERE pm.MandantID = 'SUTTONS'
      AND r.stunde >= '2025-12-01 00:00:00'
      AND r.stunde <  '2025-12-31 00:00:00'
    GROUP BY CASE WHEN c.process_id IS NULL                  THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'          THEN NULL
              WHEN c.richtung IS NULL OR c.richtung = '' THEN NULL
              ELSE c.richtung END
) g
ORDER BY g.nachrichten DESC;

SELECT '=== M98 SUTTONS Laufzeit Richtung: Aufwaermlauf + fuenf ===' AS marke;
SELECT COALESCE(g.schluessel, 'nicht zugeordnet') AS richtung_wert,
       g.nachrichten,
       ROUND(100.0000 * g.nachrichten / SUM(g.nachrichten) OVER (), 4) AS anteil_prozent
FROM (
    SELECT CASE WHEN c.process_id IS NULL                  THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'          THEN NULL
              WHEN c.richtung IS NULL OR c.richtung = '' THEN NULL
              ELSE c.richtung END AS schluessel,
           SUM(r.anzahl) AS nachrichten
    FROM overlord_monitor.message_rollup r
    JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
    JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
    JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
    LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
    WHERE pm.MandantID = 'SUTTONS'
      AND r.stunde >= '2025-12-01 00:00:00'
      AND r.stunde <  '2025-12-31 00:00:00'
    GROUP BY CASE WHEN c.process_id IS NULL                  THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'          THEN NULL
              WHEN c.richtung IS NULL OR c.richtung = '' THEN NULL
              ELSE c.richtung END
) g
ORDER BY g.nachrichten DESC;
SELECT COALESCE(g.schluessel, 'nicht zugeordnet') AS richtung_wert,
       g.nachrichten,
       ROUND(100.0000 * g.nachrichten / SUM(g.nachrichten) OVER (), 4) AS anteil_prozent
FROM (
    SELECT CASE WHEN c.process_id IS NULL                  THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'          THEN NULL
              WHEN c.richtung IS NULL OR c.richtung = '' THEN NULL
              ELSE c.richtung END AS schluessel,
           SUM(r.anzahl) AS nachrichten
    FROM overlord_monitor.message_rollup r
    JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
    JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
    JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
    LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
    WHERE pm.MandantID = 'SUTTONS'
      AND r.stunde >= '2025-12-01 00:00:00'
      AND r.stunde <  '2025-12-31 00:00:00'
    GROUP BY CASE WHEN c.process_id IS NULL                  THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'          THEN NULL
              WHEN c.richtung IS NULL OR c.richtung = '' THEN NULL
              ELSE c.richtung END
) g
ORDER BY g.nachrichten DESC;
SELECT COALESCE(g.schluessel, 'nicht zugeordnet') AS richtung_wert,
       g.nachrichten,
       ROUND(100.0000 * g.nachrichten / SUM(g.nachrichten) OVER (), 4) AS anteil_prozent
FROM (
    SELECT CASE WHEN c.process_id IS NULL                  THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'          THEN NULL
              WHEN c.richtung IS NULL OR c.richtung = '' THEN NULL
              ELSE c.richtung END AS schluessel,
           SUM(r.anzahl) AS nachrichten
    FROM overlord_monitor.message_rollup r
    JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
    JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
    JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
    LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
    WHERE pm.MandantID = 'SUTTONS'
      AND r.stunde >= '2025-12-01 00:00:00'
      AND r.stunde <  '2025-12-31 00:00:00'
    GROUP BY CASE WHEN c.process_id IS NULL                  THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'          THEN NULL
              WHEN c.richtung IS NULL OR c.richtung = '' THEN NULL
              ELSE c.richtung END
) g
ORDER BY g.nachrichten DESC;
SELECT COALESCE(g.schluessel, 'nicht zugeordnet') AS richtung_wert,
       g.nachrichten,
       ROUND(100.0000 * g.nachrichten / SUM(g.nachrichten) OVER (), 4) AS anteil_prozent
FROM (
    SELECT CASE WHEN c.process_id IS NULL                  THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'          THEN NULL
              WHEN c.richtung IS NULL OR c.richtung = '' THEN NULL
              ELSE c.richtung END AS schluessel,
           SUM(r.anzahl) AS nachrichten
    FROM overlord_monitor.message_rollup r
    JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
    JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
    JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
    LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
    WHERE pm.MandantID = 'SUTTONS'
      AND r.stunde >= '2025-12-01 00:00:00'
      AND r.stunde <  '2025-12-31 00:00:00'
    GROUP BY CASE WHEN c.process_id IS NULL                  THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'          THEN NULL
              WHEN c.richtung IS NULL OR c.richtung = '' THEN NULL
              ELSE c.richtung END
) g
ORDER BY g.nachrichten DESC;
SELECT COALESCE(g.schluessel, 'nicht zugeordnet') AS richtung_wert,
       g.nachrichten,
       ROUND(100.0000 * g.nachrichten / SUM(g.nachrichten) OVER (), 4) AS anteil_prozent
FROM (
    SELECT CASE WHEN c.process_id IS NULL                  THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'          THEN NULL
              WHEN c.richtung IS NULL OR c.richtung = '' THEN NULL
              ELSE c.richtung END AS schluessel,
           SUM(r.anzahl) AS nachrichten
    FROM overlord_monitor.message_rollup r
    JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
    JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
    JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
    LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
    WHERE pm.MandantID = 'SUTTONS'
      AND r.stunde >= '2025-12-01 00:00:00'
      AND r.stunde <  '2025-12-31 00:00:00'
    GROUP BY CASE WHEN c.process_id IS NULL                  THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'          THEN NULL
              WHEN c.richtung IS NULL OR c.richtung = '' THEN NULL
              ELSE c.richtung END
) g
ORDER BY g.nachrichten DESC;
SELECT COALESCE(g.schluessel, 'nicht zugeordnet') AS richtung_wert,
       g.nachrichten,
       ROUND(100.0000 * g.nachrichten / SUM(g.nachrichten) OVER (), 4) AS anteil_prozent
FROM (
    SELECT CASE WHEN c.process_id IS NULL                  THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'          THEN NULL
              WHEN c.richtung IS NULL OR c.richtung = '' THEN NULL
              ELSE c.richtung END AS schluessel,
           SUM(r.anzahl) AS nachrichten
    FROM overlord_monitor.message_rollup r
    JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
    JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
    JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
    LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
    WHERE pm.MandantID = 'SUTTONS'
      AND r.stunde >= '2025-12-01 00:00:00'
      AND r.stunde <  '2025-12-31 00:00:00'
    GROUP BY CASE WHEN c.process_id IS NULL                  THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'          THEN NULL
              WHEN c.richtung IS NULL OR c.richtung = '' THEN NULL
              ELSE c.richtung END
) g
ORDER BY g.nachrichten DESC;

SELECT '=== 99 Laufzeiten ===' AS marke;
SHOW PROFILES;

SELECT '=== 99 read_only am Ende der Sitzung ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;
