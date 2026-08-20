-- M74b — Wie viele Prozesse tragen ueberhaupt Nachrichten?
SET profiling = 1;
SET profiling_history_size = 100;

SELECT 'A: voller durchlauf, kennzahlen' AS marke;
SELECT COUNT(*) AS prozesse_mit_nachrichten, SUM(n) AS nachrichten_gesamt,
       MIN(n) AS min_je_prozess, MAX(n) AS max_je_prozess
FROM (SELECT ProcessID, COUNT(*) AS n FROM Message GROUP BY ProcessID) t;

SELECT 'B: prozesse ohne nachricht (EXISTS ueber Index)' AS marke;
SELECT COUNT(*) AS prozesse_ohne_nachricht
FROM Process p WHERE NOT EXISTS (SELECT 1 FROM Message m WHERE m.ProcessID = p.ProcessID);

SELECT 'C: je mandant' AS marke;
SELECT pm.MandantID, COUNT(*) AS prozesse,
       SUM(EXISTS (SELECT 1 FROM Message m WHERE m.ProcessID = p.ProcessID)) AS mit_nachrichten,
       SUM(NOT EXISTS (SELECT 1 FROM Message m WHERE m.ProcessID = p.ProcessID)) AS ohne_nachrichten,
       ROUND(100.0 * SUM(NOT EXISTS (SELECT 1 FROM Message m WHERE m.ProcessID = p.ProcessID)) / COUNT(*), 2) AS anteil_ohne_prozent
FROM Process p JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID
GROUP BY pm.MandantID ORDER BY prozesse DESC;

SELECT 'D: tragen die 13 herrenlosen Prozesse Nachrichten? (A8-Probe)' AS marke;
SELECT COUNT(*) AS herrenlose_prozesse,
       SUM(EXISTS (SELECT 1 FROM Message m WHERE m.ProcessID = p.ProcessID)) AS davon_mit_nachrichten
FROM Process p LEFT JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.ProjectID IS NULL;

SELECT 'E: Message-ProcessIDs ohne Process-Zeile (Verweistreue)' AS marke;
SELECT COUNT(*) AS prozesskennungen_in_message, SUM(p.ProcessID IS NULL) AS ohne_process_zeile
FROM (SELECT DISTINCT ProcessID FROM Message) m
LEFT JOIN Process p ON p.ProcessID = m.ProcessID;

SHOW PROFILES;
