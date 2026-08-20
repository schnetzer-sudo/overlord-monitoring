SET profiling = 1; SET profiling_history_size = 100;

SELECT 'M77 F: widersprechen sich mehrere SOS eines Prozesses? (Q4)' AS marke;
SELECT pm.MandantID,
       COUNT(*) AS prozesse_mit_richtungshinweis,
       SUM(eing > 0 AND ausg > 0) AS widerspruechlich,
       SUM(eing > 0 AND ausg = 0) AS eindeutig_eingehend,
       SUM(eing = 0 AND ausg > 0) AS eindeutig_ausgehend
FROM (
  SELECT s.ProcessID,
         SUM(s.SOSName LIKE 'Eingehend%') AS eing,
         SUM(s.SOSName LIKE 'Ausgehend%') AS ausg
  FROM SOS s GROUP BY s.ProcessID
) t
JOIN Process p ON p.ProcessID = t.ProcessID
JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE t.eing > 0 OR t.ausg > 0
GROUP BY pm.MandantID ORDER BY prozesse_mit_richtungshinweis DESC;

SELECT 'M77 G: Deckung je Mandant — Prozesse mit Richtungshinweis' AS marke;
SELECT pm.MandantID, COUNT(*) AS prozesse,
       SUM(EXISTS (SELECT 1 FROM SOS s WHERE s.ProcessID = p.ProcessID
                   AND (s.SOSName LIKE 'Eingehend%' OR s.SOSName LIKE 'Ausgehend%'))) AS mit_hinweis
FROM Process p JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID
GROUP BY pm.MandantID ORDER BY prozesse DESC;

SELECT 'M78 A: Auffangprozesse' AS marke;
SELECT pm.MandantID, p.ProcessID, p.ProjectID
FROM Process p JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE p.ProcessID REGEXP 'ndefined' OR p.ProcessID REGEXP '^0+[_]'
ORDER BY pm.MandantID, p.ProcessID;

SELECT 'M78 B: Nachrichten auf diesen Auffangprozessen' AS marke;
SELECT pm.MandantID, p.ProcessID,
       (SELECT COUNT(*) FROM Message m WHERE m.ProcessID = p.ProcessID) AS nachrichten
FROM Process p JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE p.ProcessID REGEXP 'ndefined' OR p.ProcessID REGEXP '^0+[_]'
ORDER BY nachrichten DESC;

SELECT 'M78 C: weiter gefasst — Sonstige/Undefined/Router in ProcessID oder ProjectID' AS marke;
SELECT pm.MandantID, COUNT(*) AS treffer
FROM Process p JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE p.ProcessID REGEXP 'ndefined|onstige|Router|nkonfiguriert'
   OR p.ProjectID REGEXP 'ndefined|onstige|Router|nkonfiguriert'
GROUP BY pm.MandantID ORDER BY treffer DESC;
SHOW PROFILES;
