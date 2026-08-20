-- M75 — Gestalt der Prozess- und Projektnamen je Mandant
SET profiling = 1; SET profiling_history_size = 100;

SELECT 'A: unterstriche in ProcessID je mandant' AS marke;
SELECT pm.MandantID,
       CHAR_LENGTH(p.ProcessID) - CHAR_LENGTH(REPLACE(p.ProcessID, '_', '')) AS unterstriche,
       COUNT(*) AS prozesse
FROM Process p JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID
GROUP BY pm.MandantID, unterstriche ORDER BY pm.MandantID, unterstriche;

SELECT 'B: nummernpraefix in ProcessID je mandant' AS marke;
SELECT pm.MandantID,
       SUM(p.ProcessID REGEXP '^[0-9]+_')     AS mit_nummernpraefix,
       SUM(p.ProcessID NOT REGEXP '^[0-9]+_') AS ohne_nummernpraefix,
       COUNT(*)                               AS prozesse,
       ROUND(100.0 * SUM(p.ProcessID REGEXP '^[0-9]+_') / COUNT(*), 2) AS anteil_prozent
FROM Process p JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID
GROUP BY pm.MandantID ORDER BY prozesse DESC;

-- ERWEITERUNG (nicht im Auftrag): Process.ProcessName existiert und ist eine zweite lesbare Quelle
SELECT 'C: dieselbe Frage fuer ProcessName' AS marke;
SELECT pm.MandantID,
       SUM(p.ProcessName REGEXP '^[0-9]+_')  AS name_mit_nummernpraefix,
       SUM(p.ProcessName REGEXP '_')         AS name_mit_unterstrich,
       SUM(p.ProcessName = p.ProcessID)      AS name_gleich_id,
       COUNT(*)                              AS prozesse
FROM Process p JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID
GROUP BY pm.MandantID ORDER BY prozesse DESC;

SELECT 'D: unterstriche in ProjectID je mandant' AS marke;
SELECT pm.MandantID,
       CHAR_LENGTH(prj.ProjectID) - CHAR_LENGTH(REPLACE(prj.ProjectID, '_', '')) AS unterstriche,
       COUNT(*) AS projekte
FROM Project prj JOIN ProjectMandant pm ON pm.ProjectID = prj.ProjectID
GROUP BY pm.MandantID, unterstriche ORDER BY pm.MandantID, unterstriche;

SHOW PROFILES;
