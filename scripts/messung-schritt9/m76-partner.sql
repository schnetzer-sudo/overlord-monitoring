-- M76 — Traegt der Partner im Projekt oder im Prozess? (Anschauungsmaterial)
SET profiling = 1; SET profiling_history_size = 100;

SELECT 'A: projekte mit prozesszahl je mandant' AS marke;
SELECT pm.MandantID, prj.ProjectID, COUNT(p.ProcessID) AS prozesse
FROM Project prj JOIN ProjectMandant pm ON pm.ProjectID = prj.ProjectID
LEFT JOIN Process p ON p.ProjectID = prj.ProjectID
GROUP BY pm.MandantID, prj.ProjectID
ORDER BY pm.MandantID, prozesse DESC;
SHOW PROFILES;
