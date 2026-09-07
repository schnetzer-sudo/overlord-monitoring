-- M156 — Deckung je Typ-1-Name, NEXANS, Fenster 30 Tage bis 2025-12-30
-- getrennt nach Stellung in der Kette (Rollendefinition wie M28-1/M28-2)
SELECT @@global.read_only AS globalReadOnly;
SET max_statement_time = 60;
SET profiling = 1;
SET profiling_history_size = 100;

SELECT '--- M156-0 Gegenprobe der Rollendefinition: bit(1) gegen <> Leerstring ---' AS marke;
SELECT SUM(m.Source = 1) AS wurzelnGleichEins,
       SUM(m.Source IS NOT NULL AND m.Source <> '') AS wurzelnUngleichLeer,
       SUM(m.Target = 1) AS mergeErgebnisse
  FROM GlassfishDB.Message m
 WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
   AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
   AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                 JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS');

SELECT '--- M156-1 EXPLAIN ---' AS marke;
EXPLAIN
SELECT COUNT(*) AS nachrichten
  FROM (SELECT (m.Source = 1) AS istWurzel,
               (m.SourceMessageID IS NOT NULL AND m.SourceMessageID <> '') AS istKind,
               EXISTS (SELECT 1 FROM GlassfishDB.MessageProperty p
                        WHERE p.MessageID = m.MessageID
                          AND p.MessagePropertyName = 'Converter.TransactionID') AS hatA,
               EXISTS (SELECT 1 FROM GlassfishDB.MessageProperty p
                        WHERE p.MessageID = m.MessageID
                          AND p.MessagePropertyName = 'Message.GUID') AS hatB,
               EXISTS (SELECT 1 FROM GlassfishDB.MessageProperty p
                        WHERE p.MessageID = m.MessageID
                          AND p.MessagePropertyName = 'Message.ReceiverID') AS hatC,
               EXISTS (SELECT 1 FROM GlassfishDB.MessageProperty p
                        WHERE p.MessageID = m.MessageID
                          AND p.MessagePropertyName = 'Service.Type') AS hatD
          FROM GlassfishDB.Message m
         WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
           AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
           AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                         JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                        WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')) x;

SELECT '--- M156-2 Ergebnis NEXANS ---' AS marke;
SELECT COUNT(*) AS nachrichten, SUM(istWurzel) AS wurzeln, SUM(istKind) AS kinder,
       SUM(hatA) AS a_ges, SUM(hatA AND istWurzel) AS a_wur, SUM(hatA AND istKind) AS a_kind,
       SUM(hatB) AS b_ges, SUM(hatB AND istWurzel) AS b_wur, SUM(hatB AND istKind) AS b_kind,
       SUM(hatC) AS c_ges, SUM(hatC AND istWurzel) AS c_wur, SUM(hatC AND istKind) AS c_kind,
       SUM(hatD) AS d_ges, SUM(hatD AND istWurzel) AS d_wur, SUM(hatD AND istKind) AS d_kind
  FROM (SELECT (m.Source = 1) AS istWurzel,
               (m.SourceMessageID IS NOT NULL AND m.SourceMessageID <> '') AS istKind,
               EXISTS (SELECT 1 FROM GlassfishDB.MessageProperty p
                        WHERE p.MessageID = m.MessageID
                          AND p.MessagePropertyName = 'Converter.TransactionID') AS hatA,
               EXISTS (SELECT 1 FROM GlassfishDB.MessageProperty p
                        WHERE p.MessageID = m.MessageID
                          AND p.MessagePropertyName = 'Message.GUID') AS hatB,
               EXISTS (SELECT 1 FROM GlassfishDB.MessageProperty p
                        WHERE p.MessageID = m.MessageID
                          AND p.MessagePropertyName = 'Message.ReceiverID') AS hatC,
               EXISTS (SELECT 1 FROM GlassfishDB.MessageProperty p
                        WHERE p.MessageID = m.MessageID
                          AND p.MessagePropertyName = 'Service.Type') AS hatD
          FROM GlassfishDB.Message m
         WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
           AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
           AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                         JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                        WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')) x;
