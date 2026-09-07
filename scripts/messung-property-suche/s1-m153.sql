-- M153 — Was die Tabelle MessagePropertySearchListEntry ist
SELECT @@global.read_only AS globalReadOnly;
SET max_statement_time = 60;

SELECT '--- M153-1 Existenz und Zeilenzahl (COUNT(*), nie TABLE_ROWS) ---' AS marke;
SELECT TABLE_NAME, ENGINE, TABLE_COLLATION, CREATE_TIME
  FROM information_schema.TABLES
 WHERE TABLE_SCHEMA = 'GlassfishDB' AND TABLE_NAME = 'MessagePropertySearchListEntry';

SELECT '--- M153-2 SHOW CREATE TABLE ---' AS marke;
SHOW CREATE TABLE GlassfishDB.MessagePropertySearchListEntry;

SELECT '--- M153-3 information_schema.COLUMNS ---' AS marke;
SELECT ORDINAL_POSITION AS pos, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_KEY,
       COLUMN_DEFAULT, COLLATION_NAME
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = 'GlassfishDB' AND TABLE_NAME = 'MessagePropertySearchListEntry'
 ORDER BY ORDINAL_POSITION;

SELECT '--- M153-4 information_schema.STATISTICS ---' AS marke;
SELECT INDEX_NAME, SEQ_IN_INDEX, COLUMN_NAME, NON_UNIQUE, CARDINALITY, SUB_PART, NULLABLE, INDEX_TYPE
  FROM information_schema.STATISTICS
 WHERE TABLE_SCHEMA = 'GlassfishDB' AND TABLE_NAME = 'MessagePropertySearchListEntry'
 ORDER BY INDEX_NAME, SEQ_IN_INDEX;

SELECT '--- M153-5 Zeilenzahl gezaehlt ---' AS marke;
SELECT COUNT(*) AS zeilenGezaehlt FROM GlassfishDB.MessagePropertySearchListEntry;

SELECT '--- M153-6 Gegenprobe: TABLE_ROWS als Schaetzung, ausdruecklich nicht verwendet ---' AS marke;
SELECT TABLE_ROWS AS schaetzungNichtVerwendet, DATA_LENGTH, INDEX_LENGTH
  FROM information_schema.TABLES
 WHERE TABLE_SCHEMA = 'GlassfishDB' AND TABLE_NAME = 'MessagePropertySearchListEntry';
