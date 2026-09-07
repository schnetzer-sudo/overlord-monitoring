-- M157-1: Zeilen je Typ-1-Name, Gesamtbestand, je ein ref-Zugriff
SELECT @@global.read_only AS globalReadOnly;
SET max_statement_time = 180;
SET profiling = 1;
SET profiling_history_size = 100;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;

SELECT 'Converter.TransactionID' AS name, COUNT(*) AS zeilen
  FROM GlassfishDB.MessageProperty WHERE MessagePropertyName = 'Converter.TransactionID';
SELECT 'Message.GUID' AS name, COUNT(*) AS zeilen
  FROM GlassfishDB.MessageProperty WHERE MessagePropertyName = 'Message.GUID';
SELECT 'Message.ReceiverID' AS name, COUNT(*) AS zeilen
  FROM GlassfishDB.MessageProperty WHERE MessagePropertyName = 'Message.ReceiverID';
SELECT 'Service.Type' AS name, COUNT(*) AS zeilen
  FROM GlassfishDB.MessageProperty WHERE MessagePropertyName = 'Service.Type';

SELECT '--- Laufzeiten je Zaehlung ---' AS marke;
SELECT QUERY_ID, ROUND(SUM(DURATION), 3) AS sekunden
  FROM information_schema.PROFILING WHERE QUERY_ID > @basis + 1 GROUP BY QUERY_ID ORDER BY QUERY_ID;
