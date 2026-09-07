-- M156 Fassung B — ein Join statt vier EXISTS. Erst SUTTONS (klein), dann NEXANS.
SELECT @@global.read_only AS globalReadOnly;
SET max_statement_time = 60;

SELECT '--- M156-3 EXPLAIN Fassung B ---' AS marke;
EXPLAIN
SELECT p.MessagePropertyName,
       COUNT(DISTINCT m.MessageID) AS belegt,
       COUNT(DISTINCT CASE WHEN m.Source = 1 THEN m.MessageID END) AS belegtWurzel,
       COUNT(DISTINCT CASE WHEN m.SourceMessageID IS NOT NULL AND m.SourceMessageID <> ''
                           THEN m.MessageID END) AS belegtKind
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.MessageProperty p
    ON p.MessageID = m.MessageID
   AND p.MessagePropertyName IN ('Converter.TransactionID','Message.GUID',
                                 'Message.ReceiverID','Service.Type')
 WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
   AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
   AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                 JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'SUTTONS')
 GROUP BY p.MessagePropertyName;

SELECT '--- M156-4 SUTTONS ---' AS marke;
SELECT p.MessagePropertyName,
       COUNT(DISTINCT m.MessageID) AS belegt,
       COUNT(DISTINCT CASE WHEN m.Source = 1 THEN m.MessageID END) AS belegtWurzel,
       COUNT(DISTINCT CASE WHEN m.SourceMessageID IS NOT NULL AND m.SourceMessageID <> ''
                           THEN m.MessageID END) AS belegtKind
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.MessageProperty p
    ON p.MessageID = m.MessageID
   AND p.MessagePropertyName IN ('Converter.TransactionID','Message.GUID',
                                 'Message.ReceiverID','Service.Type')
 WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
   AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
   AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                 JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'SUTTONS')
 GROUP BY p.MessagePropertyName;
