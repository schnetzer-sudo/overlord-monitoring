-- Sitzung 0 — Rahmen der Messrunde Property-Suche
SELECT @@global.read_only AS globalReadOnly;
SELECT VERSION() AS serverVersion, NOW() AS serverzeitBeginn, CURRENT_USER() AS benutzer;
SELECT @@session.sql_mode AS sqlMode;
SELECT @@div_precision_increment AS divPrecisionIncrement;
SELECT @@max_statement_time AS maxStatementTimeVorgabe;
SELECT @@innodb_buffer_pool_size AS bufferPool;
SELECT @@profiling_history_size AS profilingHistoryVorgabe;
SELECT MIN(MessageLastUpdate) AS aeltester, MAX(MessageLastUpdate) AS neuester
  FROM GlassfishDB.Message;
