-- Vorprobe zu M156: Groesse des 30-Tage-Fensters je Mandant, Rollenverteilung
-- Fenster: 2025-11-30 00:00:00 (einschliesslich) bis 2025-12-30 00:00:00 (ausschliesslich)
SELECT @@global.read_only AS globalReadOnly;
SET max_statement_time = 60;

SELECT '--- Fenstergroesse und Rollen, NEXANS ---' AS marke;
SELECT COUNT(*) AS nachrichten,
       SUM(m.Source IS NOT NULL AND m.Source <> '') AS wurzeln,
       SUM(m.SourceMessageID IS NOT NULL AND m.SourceMessageID <> '') AS kinder
  FROM GlassfishDB.Message m
 WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
   AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
   AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                 JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS');

SELECT '--- Fenstergroesse und Rollen, SUTTONS ---' AS marke;
SELECT COUNT(*) AS nachrichten,
       SUM(m.Source IS NOT NULL AND m.Source <> '') AS wurzeln,
       SUM(m.SourceMessageID IS NOT NULL AND m.SourceMessageID <> '') AS kinder
  FROM GlassfishDB.Message m
 WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
   AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
   AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                 JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'SUTTONS');

SELECT '--- Gegenprobe: Fenster ueber alle Mandanten ---' AS marke;
SELECT COUNT(*) AS nachrichtenGesamt
  FROM GlassfishDB.Message m
 WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
   AND m.MessageLastUpdate <  '2025-12-30 00:00:00';

SELECT '--- Spalten Source/Target in Message ---' AS marke;
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = 'GlassfishDB' AND TABLE_NAME = 'Message'
   AND COLUMN_NAME IN ('Source','Target','SourceMessageID','TargetMessageID');
