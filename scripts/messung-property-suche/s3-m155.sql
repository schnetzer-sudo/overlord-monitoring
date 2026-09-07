-- M155 — Gegenprobe der Typ-Lesart: kommt jeder Name in MessageProperty vor?
-- Als EXISTS-Probe je Name gegen MessagePropertyNameIDX, NICHT als SELECT DISTINCT.
SELECT @@global.read_only AS globalReadOnly;
SET max_statement_time = 60;
SET profiling = 1;
SET profiling_history_size = 100;

SELECT '--- M155-1 EXPLAIN der EXISTS-Probe ---' AS marke;
EXPLAIN
SELECT e.MessagePropertyName, e.MessagePropertyType,
       EXISTS (SELECT 1 FROM GlassfishDB.MessageProperty p
                WHERE p.MessagePropertyName = e.MessagePropertyName) AS kommtVor
  FROM GlassfishDB.MessagePropertySearchListEntry e;

SELECT '--- M155-2 EXPLAIN FORMAT=JSON, Indexname ---' AS marke;
EXPLAIN FORMAT=JSON
SELECT e.MessagePropertyName,
       EXISTS (SELECT 1 FROM GlassfishDB.MessageProperty p
                WHERE p.MessagePropertyName = e.MessagePropertyName) AS kommtVor
  FROM GlassfishDB.MessagePropertySearchListEntry e;

SELECT '--- M155-3 Ergebnis der Probe ---' AS marke;
SELECT e.MessagePropertyType AS typ, e.MessagePropertyName, e.MandantID,
       EXISTS (SELECT 1 FROM GlassfishDB.MessageProperty p
                WHERE p.MessagePropertyName = e.MessagePropertyName) AS kommtInMessagePropertyVor
  FROM GlassfishDB.MessagePropertySearchListEntry e
 ORDER BY e.MessagePropertyType, e.MessagePropertyName;

SELECT '--- M155-4 Laufzeit: Aufwaermlauf plus fuenf ---' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT COUNT(*) FROM (SELECT e.MessagePropertyName, EXISTS (SELECT 1 FROM GlassfishDB.MessageProperty p WHERE p.MessagePropertyName = e.MessagePropertyName) AS k FROM GlassfishDB.MessagePropertySearchListEntry e) x;
SELECT COUNT(*) FROM (SELECT e.MessagePropertyName, EXISTS (SELECT 1 FROM GlassfishDB.MessageProperty p WHERE p.MessagePropertyName = e.MessagePropertyName) AS k FROM GlassfishDB.MessagePropertySearchListEntry e) x;
SELECT COUNT(*) FROM (SELECT e.MessagePropertyName, EXISTS (SELECT 1 FROM GlassfishDB.MessageProperty p WHERE p.MessagePropertyName = e.MessagePropertyName) AS k FROM GlassfishDB.MessagePropertySearchListEntry e) x;
SELECT COUNT(*) FROM (SELECT e.MessagePropertyName, EXISTS (SELECT 1 FROM GlassfishDB.MessageProperty p WHERE p.MessagePropertyName = e.MessagePropertyName) AS k FROM GlassfishDB.MessagePropertySearchListEntry e) x;
SELECT COUNT(*) FROM (SELECT e.MessagePropertyName, EXISTS (SELECT 1 FROM GlassfishDB.MessageProperty p WHERE p.MessagePropertyName = e.MessagePropertyName) AS k FROM GlassfishDB.MessagePropertySearchListEntry e) x;
SELECT COUNT(*) FROM (SELECT e.MessagePropertyName, EXISTS (SELECT 1 FROM GlassfishDB.MessageProperty p WHERE p.MessagePropertyName = e.MessagePropertyName) AS k FROM GlassfishDB.MessagePropertySearchListEntry e) x;
SELECT COUNT(*) AS anzahlLaeufe, MIN(d) AS besteSekunden, MAX(d) AS schlechtesteSekunden
  FROM (SELECT QUERY_ID, SUM(DURATION) AS d FROM information_schema.PROFILING
         WHERE QUERY_ID > @basis + 1 GROUP BY QUERY_ID) g;

SELECT '--- M155-5 Vermutete Quellspalten der Typ-0-Namen: existieren die Spalten? ---' AS marke;
SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = 'GlassfishDB'
   AND TABLE_NAME IN ('Message','Process','SOS','SOSAction','Project')
   AND COLUMN_NAME IN ('MessageID','SourceMessageID','TargetMessageID','ProcessID',
                       'ProcessName','SOSID','SOSName','MessageStatus','Status')
 ORDER BY TABLE_NAME, COLUMN_NAME;
