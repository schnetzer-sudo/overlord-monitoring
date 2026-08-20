-- M76 Nachlauf — Laufzeit der Projektliste, nachgeholt (erste Sitzung ohne SHOW PROFILES abgebrochen)
-- sowie M77 H: SOS je Prozess, eingeschraenkt auf richtungstragende Prozesse
SET profiling = 1; SET profiling_history_size = 100;

SELECT 'aufwaermlauf' AS marke;
SELECT pm.MandantID, prj.ProjectID, COUNT(p.ProcessID) AS prozesse
FROM Project prj JOIN ProjectMandant pm ON pm.ProjectID = prj.ProjectID
LEFT JOIN Process p ON p.ProjectID = prj.ProjectID
GROUP BY pm.MandantID, prj.ProjectID ORDER BY pm.MandantID, prozesse DESC;

SELECT 'gemessen' AS marke;
SELECT pm.MandantID, prj.ProjectID, COUNT(p.ProcessID) AS prozesse
FROM Project prj JOIN ProjectMandant pm ON pm.ProjectID = prj.ProjectID
LEFT JOIN Process p ON p.ProjectID = prj.ProjectID
GROUP BY pm.MandantID, prj.ProjectID ORDER BY pm.MandantID, prozesse DESC;

SELECT 'M77 H: SOS je Prozess bei VOTG, und bei den richtungstragenden' AS marke;
SELECT SUM(t.anzahl_sos > 1)                                   AS votg_prozesse_mit_mehr_als_einem_sos,
       SUM(t.anzahl_sos > 1 AND t.richtung > 0)                AS davon_richtungstragend,
       SUM(t.richtung > 0)                                     AS richtungstragende_prozesse
FROM (SELECT s.ProcessID, COUNT(*) AS anzahl_sos,
             SUM(s.SOSName LIKE 'Eingehend%' OR s.SOSName LIKE 'Ausgehend%') AS richtung
      FROM SOS s GROUP BY s.ProcessID) t
JOIN Process p ON p.ProcessID = t.ProcessID
JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'VOTG';
SHOW PROFILES;
