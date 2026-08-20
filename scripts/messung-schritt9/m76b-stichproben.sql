SET profiling = 1; SET profiling_history_size = 100;
SELECT 'A: VOTG vollstaendig' AS marke;
SELECT prj.ProjectID, COUNT(p.ProcessID) AS prozesse
FROM Project prj JOIN ProjectMandant pm ON pm.ProjectID = prj.ProjectID
LEFT JOIN Process p ON p.ProjectID = prj.ProjectID
WHERE pm.MandantID = 'VOTG' GROUP BY prj.ProjectID ORDER BY prozesse DESC;

SELECT 'B: NEXANS — 15 Prozesskennungen' AS marke;
SELECT p.ProjectID, p.ProcessID FROM Process p JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'NEXANS' ORDER BY p.ProjectID, p.ProcessID LIMIT 15;

SELECT 'C: VOTG — 15 Prozesskennungen' AS marke;
SELECT p.ProjectID, p.ProcessID FROM Process p JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'VOTG' ORDER BY p.ProjectID, p.ProcessID LIMIT 15;

SELECT 'D: VOTG — 15 aus dem grossen Projekt 110_VTG_SalesInvoice' AS marke;
SELECT p.ProcessID FROM Process p WHERE p.ProjectID = '110_VTG_SalesInvoice' ORDER BY p.ProcessID LIMIT 15;

SELECT 'E: SUTTONS — alle 17' AS marke;
SELECT p.ProjectID, p.ProcessID FROM Process p JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'SUTTONS' ORDER BY p.ProcessID;

SELECT 'F: IBIS — 15 Prozesskennungen' AS marke;
SELECT p.ProjectID, p.ProcessID FROM Process p JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'IBIS' ORDER BY p.ProjectID, p.ProcessID LIMIT 15;
SHOW PROFILES;
