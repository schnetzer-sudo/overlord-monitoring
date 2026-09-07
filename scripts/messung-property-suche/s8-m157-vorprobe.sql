-- Vorprobe zu M157: Zeilen je Typ-1-Name ueber den Gesamtbestand
SELECT @@global.read_only AS globalReadOnly;
SET max_statement_time = 120;
SET profiling = 1;
SET profiling_history_size = 100;

SELECT '--- EXPLAIN Zeilen je Name ---' AS marke;
EXPLAIN SELECT p.MessagePropertyName, COUNT(*) AS zeilen
  FROM GlassfishDB.MessageProperty p
 WHERE p.MessagePropertyName IN ('Converter.TransactionID','Message.GUID',
                                 'Message.ReceiverID','Service.Type')
 GROUP BY p.MessagePropertyName;

SELECT '--- Zeilen je Name, Gesamtbestand ---' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT p.MessagePropertyName, COUNT(*) AS zeilen,
       SUM(p.MessagePropertyValue IS NULL) AS wertNull
  FROM GlassfishDB.MessageProperty p
 WHERE p.MessagePropertyName IN ('Converter.TransactionID','Message.GUID',
                                 'Message.ReceiverID','Service.Type')
 GROUP BY p.MessagePropertyName;
SELECT QUERY_ID, ROUND(SUM(DURATION) * 1000, 3) AS ms
  FROM information_schema.PROFILING WHERE QUERY_ID > @basis + 1 GROUP BY QUERY_ID;
