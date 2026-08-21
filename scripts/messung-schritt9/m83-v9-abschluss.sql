-- Messrunde M83 — V9: Abschluss. Nachweis Testkopie erneut, Datenstand erneut (V2/V3).
-- Zusaetzlich die Kennzahlen, die die Aussage zum Kaltlauf (M83-4) tragen.
--
-- Ausschliesslich SELECT / SET. (Regel S1)

SET max_statement_time = 60;

-- V2, zweite Ablesung: muss weiterhin 1 liefern.
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;

-- V3, zweite Ablesung: Datenstand darf sich waehrend der Runde nicht bewegt haben.
SELECT MAX(MessageLastUpdate) AS datenstand, COUNT(*) AS nachrichten
FROM GlassfishDB.Message;

-- Warum "erster Lauf der Sitzung" nur eine UNTERE Schranke des Kaltfalls ist:
-- der Puffer ist um ein Vielfaches groesser als die ganze Tabelle.
SELECT ROUND(@@innodb_buffer_pool_size / 1024 / 1024, 1) AS puffer_mib,
       ROUND((SELECT (DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024
              FROM information_schema.TABLES
              WHERE TABLE_SCHEMA = 'GlassfishDB' AND TABLE_NAME = 'Message'), 1) AS message_mib,
       ROUND(@@innodb_buffer_pool_size /
             (SELECT DATA_LENGTH + INDEX_LENGTH FROM information_schema.TABLES
              WHERE TABLE_SCHEMA = 'GlassfishDB' AND TABLE_NAME = 'Message'), 2) AS puffer_je_tabelle;

-- Wie viel wurde in dieser Runde ueberhaupt noch physisch gelesen?
-- Ein sehr kleines Verhaeltnis belegt: es lag alles im Puffer.
SELECT
  MAX(CASE WHEN VARIABLE_NAME = 'INNODB_BUFFER_POOL_READ_REQUESTS' THEN VARIABLE_VALUE END) AS leseanfragen_logisch,
  MAX(CASE WHEN VARIABLE_NAME = 'INNODB_BUFFER_POOL_READS'         THEN VARIABLE_VALUE END) AS lesevorgaenge_physisch,
  MAX(CASE WHEN VARIABLE_NAME = 'UPTIME'                           THEN VARIABLE_VALUE END) AS uptime_s
FROM information_schema.GLOBAL_STATUS
WHERE VARIABLE_NAME IN ('INNODB_BUFFER_POOL_READ_REQUESTS','INNODB_BUFFER_POOL_READS','UPTIME');

-- Groesse der beiden ProcessID-Indizes, falls lesbar (mysql-Schema ist meist gesperrt).
SELECT index_name, stat_name, stat_value, stat_description
FROM mysql.innodb_index_stats
WHERE database_name = 'GlassfishDB' AND table_name = 'Message'
  AND index_name IN ('ProejctIDIDX','Message_ProcessFK') AND stat_name IN ('size','n_leaf_pages')
ORDER BY index_name, stat_name;
