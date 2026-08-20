-- M77 — Steckt die Richtung im SOSName?
SET profiling = 1; SET profiling_history_size = 100;

SELECT 'A: erstes Wort des SOSName je Mandant' AS marke;
SELECT pm.MandantID, SUBSTRING_INDEX(s.SOSName, ' ', 1) AS erstes_wort, COUNT(*) AS sos
FROM SOS s JOIN Process p ON p.ProcessID = s.ProcessID
JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID
GROUP BY pm.MandantID, erstes_wort ORDER BY pm.MandantID, sos DESC;

SELECT 'B: wie viele SOS je Prozess?' AS marke;
SELECT anzahl_sos, COUNT(*) AS prozesse
FROM (SELECT s.ProcessID, COUNT(*) AS anzahl_sos FROM SOS s GROUP BY s.ProcessID) t
GROUP BY anzahl_sos ORDER BY anzahl_sos;

SELECT 'C: traegt der SOSName ueberhaupt ein Leerzeichen?' AS marke;
SELECT pm.MandantID, COUNT(*) AS sos,
       SUM(s.SOSName LIKE '% %') AS mit_leerzeichen,
       SUM(s.SOSName IS NULL)    AS ohne_namen
FROM SOS s JOIN Process p ON p.ProcessID = s.ProcessID
JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID
GROUP BY pm.MandantID ORDER BY sos DESC;

SELECT 'D: Prozesse ganz ohne SOS-Zeile' AS marke;
SELECT COUNT(*) AS prozesse_ohne_sos FROM Process p
WHERE NOT EXISTS (SELECT 1 FROM SOS s WHERE s.ProcessID = p.ProcessID);

SELECT 'E: richtungstragende Woerter, gemessen statt geraten' AS marke;
SELECT pm.MandantID,
       SUM(s.SOSName LIKE 'Eingehend%' OR s.SOSName LIKE 'Eingang%')  AS eingehend,
       SUM(s.SOSName LIKE 'Ausgehend%' OR s.SOSName LIKE 'Ausgang%')  AS ausgehend,
       COUNT(*) AS sos
FROM SOS s JOIN Process p ON p.ProcessID = s.ProcessID
JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID
GROUP BY pm.MandantID ORDER BY sos DESC;
SHOW PROFILES;
