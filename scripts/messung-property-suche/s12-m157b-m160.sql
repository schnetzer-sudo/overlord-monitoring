-- M157-3 (90 Tage, Service.Type) und M160 (Wertlaengen ueber die Praefixgrenze)
-- Regel G1: kein MessagePropertyValue in der Ausgabe.
SELECT @@global.read_only AS globalReadOnly;
SET max_statement_time = 180;
SET profiling = 1;
SET profiling_history_size = 100;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;

SELECT '--- M157-3 Service.Type, NEXANS, 90 Tage bis 2025-12-30 ---' AS marke;
SELECT COUNT(*) AS verschiedeneWerte, MAX(c) AS zeilenHaeufigsterWert, SUM(c) AS zeilenGesamt
  FROM (SELECT COUNT(*) AS c
          FROM GlassfishDB.Message m
          JOIN GlassfishDB.MessageProperty p
            ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Service.Type'
         WHERE m.MessageLastUpdate >= '2025-10-01 00:00:00'
           AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
           AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                         JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                        WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
         GROUP BY p.MessagePropertyValue) g;

SELECT '--- M160 Wertlaengen je Name, NEXANS, 30 Tage ---' AS marke;

SELECT 'Converter.TransactionID' AS name,
       COUNT(*) AS zeilen,
       SUM(CHAR_LENGTH(p.MessagePropertyValue) > 50) AS laengerAls50,
       SUM(p.MessagePropertyValue IS NULL) AS wertNull,
       MIN(CHAR_LENGTH(p.MessagePropertyValue)) AS minLaenge,
       MAX(CHAR_LENGTH(p.MessagePropertyValue)) AS maxLaenge,
       ROUND(AVG(CHAR_LENGTH(p.MessagePropertyValue)), 2) AS mittelLaenge
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.MessageProperty p
    ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Converter.TransactionID'
 WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
   AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
   AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                 JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS');

SELECT 'Message.GUID' AS name,
       COUNT(*) AS zeilen,
       SUM(CHAR_LENGTH(p.MessagePropertyValue) > 50) AS laengerAls50,
       SUM(p.MessagePropertyValue IS NULL) AS wertNull,
       MIN(CHAR_LENGTH(p.MessagePropertyValue)) AS minLaenge,
       MAX(CHAR_LENGTH(p.MessagePropertyValue)) AS maxLaenge,
       ROUND(AVG(CHAR_LENGTH(p.MessagePropertyValue)), 2) AS mittelLaenge
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.MessageProperty p
    ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.GUID'
 WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
   AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
   AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                 JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS');

SELECT 'Message.ReceiverID' AS name,
       COUNT(*) AS zeilen,
       SUM(CHAR_LENGTH(p.MessagePropertyValue) > 50) AS laengerAls50,
       SUM(p.MessagePropertyValue IS NULL) AS wertNull,
       MIN(CHAR_LENGTH(p.MessagePropertyValue)) AS minLaenge,
       MAX(CHAR_LENGTH(p.MessagePropertyValue)) AS maxLaenge,
       ROUND(AVG(CHAR_LENGTH(p.MessagePropertyValue)), 2) AS mittelLaenge
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.MessageProperty p
    ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.ReceiverID'
 WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
   AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
   AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                 JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS');

SELECT 'Service.Type' AS name,
       COUNT(*) AS zeilen,
       SUM(CHAR_LENGTH(p.MessagePropertyValue) > 50) AS laengerAls50,
       SUM(p.MessagePropertyValue IS NULL) AS wertNull,
       MIN(CHAR_LENGTH(p.MessagePropertyValue)) AS minLaenge,
       MAX(CHAR_LENGTH(p.MessagePropertyValue)) AS maxLaenge,
       ROUND(AVG(CHAR_LENGTH(p.MessagePropertyValue)), 2) AS mittelLaenge
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.MessageProperty p
    ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Service.Type'
 WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
   AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
   AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                 JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS');

SELECT '--- Laufzeiten ---' AS marke;
SELECT QUERY_ID, ROUND(SUM(DURATION), 3) AS sekunden
  FROM information_schema.PROFILING WHERE QUERY_ID > @basis + 1 GROUP BY QUERY_ID ORDER BY QUERY_ID;
