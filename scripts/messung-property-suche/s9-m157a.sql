-- M157a: Zeilen je Name einzeln, Gesamtbestand, ref statt range
SELECT @@global.read_only AS globalReadOnly;
SET max_statement_time = 180;
SET profiling = 1;
SET profiling_history_size = 100;

SELECT '--- EXPLAIN ein Name, ref ---' AS marke;
EXPLAIN SELECT COUNT(*) FROM GlassfishDB.MessageProperty p
 WHERE p.MessagePropertyName = 'Message.ReceiverID';

SELECT '--- Message.ReceiverID, Gesamtbestand ---' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT COUNT(*) AS zeilen FROM GlassfishDB.MessageProperty p
 WHERE p.MessagePropertyName = 'Message.ReceiverID';
SELECT QUERY_ID, ROUND(SUM(DURATION), 3) AS sekunden
  FROM information_schema.PROFILING WHERE QUERY_ID > @basis + 1 GROUP BY QUERY_ID;
