-- L4-Pfad-Runde 08.09.2026 - Sitzung B0: Rahmen (wie n0-rahmen.sql, gleiche Reihenfolge),
-- dazu die Optimizer-Schalter und Temp-Tabellen-Grenzen, an denen eine Materialisierung haengt,
-- und die Nenner: Nachrichten je Mandant und Fenster (Erhebung, Grenze 60 s - gemeldet).
SELECT @@global.read_only AS globalReadOnly;
SELECT VERSION() AS serverVersion, NOW() AS serverzeitBeginn, CURRENT_USER() AS benutzer;
SELECT @@session.sql_mode AS sqlMode;
SELECT @@div_precision_increment AS divPrecisionIncrement;
SELECT @@max_statement_time AS maxStatementTimeVorgabe;
SELECT @@innodb_buffer_pool_size AS bufferPool;
SELECT @@profiling_history_size AS profilingHistoryVorgabe;
SELECT MIN(MessageLastUpdate) AS aeltester, MAX(MessageLastUpdate) AS neuester
  FROM GlassfishDB.Message;
SELECT @@optimizer_switch AS optimizerSwitch;
SELECT @@tmp_table_size AS tmpTableSize, @@max_heap_table_size AS maxHeapTableSize,
       @@tmp_memory_table_size AS tmpMemoryTableSize, @@tmp_disk_table_size AS tmpDiskTableSize;
SELECT @@optimizer_use_condition_selectivity AS condSelectivity, @@use_stat_tables AS useStatTables;

SELECT '--- B0-1 Nenner: Nachrichten je Mandant und Fenster (Erhebung, 60 s) ---' AS marke;
SET max_statement_time = 60;
SELECT 'NEXANS' AS mandant, '24h' AS fenster, COUNT(*) AS nachrichten
  FROM GlassfishDB.Message m
 WHERE m.MessageLastUpdate >= '2025-12-29 00:00:00' AND m.MessageLastUpdate < '2025-12-30 00:00:00'
   AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                 JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS');
SELECT 'NEXANS' AS mandant, '30T' AS fenster, COUNT(*) AS nachrichten
  FROM GlassfishDB.Message m
 WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00' AND m.MessageLastUpdate < '2025-12-30 00:00:00'
   AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                 JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS');
SELECT 'NEXANS' AS mandant, '90T' AS fenster, COUNT(*) AS nachrichten
  FROM GlassfishDB.Message m
 WHERE m.MessageLastUpdate >= '2025-10-01 00:00:00' AND m.MessageLastUpdate < '2025-12-30 00:00:00'
   AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                 JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS');
SELECT 'SUTTONS' AS mandant, '24h' AS fenster, COUNT(*) AS nachrichten
  FROM GlassfishDB.Message m
 WHERE m.MessageLastUpdate >= '2025-12-29 00:00:00' AND m.MessageLastUpdate < '2025-12-30 00:00:00'
   AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                 JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'SUTTONS');
SELECT 'SUTTONS' AS mandant, '30T' AS fenster, COUNT(*) AS nachrichten
  FROM GlassfishDB.Message m
 WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00' AND m.MessageLastUpdate < '2025-12-30 00:00:00'
   AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                 JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'SUTTONS');
SELECT 'SUTTONS' AS mandant, '90T' AS fenster, COUNT(*) AS nachrichten
  FROM GlassfishDB.Message m
 WHERE m.MessageLastUpdate >= '2025-10-01 00:00:00' AND m.MessageLastUpdate < '2025-12-30 00:00:00'
   AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                 JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'SUTTONS');
SELECT NOW() AS serverzeitEnde;
