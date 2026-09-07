-- M155 Nachtrag: Laufzeit je Lauf einzeln (Aufwaermlauf getrennt ausweisbar)
-- und die Statistikgrundlage des Optimizers fuer MessageProperty
SELECT @@global.read_only AS globalReadOnly;
SET max_statement_time = 60;
SET profiling = 1;
SET profiling_history_size = 100;

SELECT '--- Statistikgrundlage MessageProperty ---' AS marke;
SELECT TABLE_ROWS AS schaetzungTableRows, DATA_LENGTH, INDEX_LENGTH
  FROM information_schema.TABLES
 WHERE TABLE_SCHEMA = 'GlassfishDB' AND TABLE_NAME = 'MessageProperty';
SELECT INDEX_NAME, SEQ_IN_INDEX, COLUMN_NAME, CARDINALITY, SUB_PART
  FROM information_schema.STATISTICS
 WHERE TABLE_SCHEMA = 'GlassfishDB' AND TABLE_NAME = 'MessageProperty'
 ORDER BY INDEX_NAME, SEQ_IN_INDEX;

SELECT '--- M155-4b Laufzeit je Lauf ---' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT COUNT(*) FROM (SELECT e.MessagePropertyName, EXISTS (SELECT 1 FROM GlassfishDB.MessageProperty p WHERE p.MessagePropertyName = e.MessagePropertyName) AS k FROM GlassfishDB.MessagePropertySearchListEntry e) x;
SELECT COUNT(*) FROM (SELECT e.MessagePropertyName, EXISTS (SELECT 1 FROM GlassfishDB.MessageProperty p WHERE p.MessagePropertyName = e.MessagePropertyName) AS k FROM GlassfishDB.MessagePropertySearchListEntry e) x;
SELECT COUNT(*) FROM (SELECT e.MessagePropertyName, EXISTS (SELECT 1 FROM GlassfishDB.MessageProperty p WHERE p.MessagePropertyName = e.MessagePropertyName) AS k FROM GlassfishDB.MessagePropertySearchListEntry e) x;
SELECT COUNT(*) FROM (SELECT e.MessagePropertyName, EXISTS (SELECT 1 FROM GlassfishDB.MessageProperty p WHERE p.MessagePropertyName = e.MessagePropertyName) AS k FROM GlassfishDB.MessagePropertySearchListEntry e) x;
SELECT COUNT(*) FROM (SELECT e.MessagePropertyName, EXISTS (SELECT 1 FROM GlassfishDB.MessageProperty p WHERE p.MessagePropertyName = e.MessagePropertyName) AS k FROM GlassfishDB.MessagePropertySearchListEntry e) x;
SELECT COUNT(*) FROM (SELECT e.MessagePropertyName, EXISTS (SELECT 1 FROM GlassfishDB.MessageProperty p WHERE p.MessagePropertyName = e.MessagePropertyName) AS k FROM GlassfishDB.MessagePropertySearchListEntry e) x;
SELECT QUERY_ID, ROUND(SUM(DURATION) * 1000, 3) AS ms
  FROM information_schema.PROFILING
 WHERE QUERY_ID > @basis + 1
 GROUP BY QUERY_ID ORDER BY QUERY_ID;
