-- M74a — Prozessbestand je Mandant (beantwortet V3)
SET profiling = 1;
SET profiling_history_size = 100;

SELECT 'aufwaermlauf' AS marke;
SELECT pm.MandantID, COUNT(DISTINCT p.ProcessID) AS prozesse, COUNT(DISTINCT p.ProjectID) AS projekte
FROM Process p JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID
GROUP BY pm.MandantID ORDER BY prozesse DESC;

SELECT 'gemessen' AS marke;
SELECT pm.MandantID, COUNT(DISTINCT p.ProcessID) AS prozesse, COUNT(DISTINCT p.ProjectID) AS projekte
FROM Process p JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID
GROUP BY pm.MandantID ORDER BY prozesse DESC;

SELECT 'gegenprobe' AS marke;
SELECT COUNT(*) AS prozesse_ohne_mandant
FROM Process p LEFT JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.ProjectID IS NULL;

-- Summenprobe: traegt ProcessID allein den Katalogschluessel?
SELECT 'summenprobe' AS marke;
SELECT COUNT(*) AS zeilen_gesamt, COUNT(DISTINCT p.ProcessID) AS prozesse_verschieden
FROM Process p JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID;

-- Projekte, die an mehr als einem Mandanten haengen
SELECT 'mehrfachprojekte' AS marke;
SELECT COUNT(*) AS projekte_mit_mehreren_mandanten FROM (
  SELECT ProjectID FROM ProjectMandant GROUP BY ProjectID HAVING COUNT(*) > 1
) t;

-- Projekte ohne jeden Mandanten
SELECT 'projekte_ohne_mandant' AS marke;
SELECT COUNT(*) AS projekte_ohne_mandant
FROM Project prj LEFT JOIN ProjectMandant pm ON pm.ProjectID = prj.ProjectID
WHERE pm.ProjectID IS NULL;

SHOW PROFILES;
