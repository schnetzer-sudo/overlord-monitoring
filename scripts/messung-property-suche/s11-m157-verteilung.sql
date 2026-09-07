-- M157-2: Werteverteilung je Typ-1-Name, 30-Tage-Fenster bis 2025-12-30
-- Regel G1: KEIN MessagePropertyValue in der Ausgabe, ausschliesslich Raenge und Zaehler.
SELECT @@global.read_only AS globalReadOnly;
SET max_statement_time = 180;
SET profiling = 1;
SET profiling_history_size = 100;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;

SELECT 'NEXANS / Converter.TransactionID' AS fall,
       COUNT(*) AS verschiedeneWerte, MAX(c) AS zeilenHaeufigsterWert,
       SUM(c) AS zeilenGesamt, SUM(c = 1) AS werteMitGenauEinerZeile
  FROM (SELECT COUNT(*) AS c
          FROM GlassfishDB.Message m
          JOIN GlassfishDB.MessageProperty p
            ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Converter.TransactionID'
         WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
           AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
           AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                         JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                        WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
         GROUP BY p.MessagePropertyValue) g;

SELECT 'NEXANS / Message.GUID' AS fall,
       COUNT(*) AS verschiedeneWerte, MAX(c) AS zeilenHaeufigsterWert,
       SUM(c) AS zeilenGesamt, SUM(c = 1) AS werteMitGenauEinerZeile
  FROM (SELECT COUNT(*) AS c
          FROM GlassfishDB.Message m
          JOIN GlassfishDB.MessageProperty p
            ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.GUID'
         WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
           AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
           AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                         JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                        WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
         GROUP BY p.MessagePropertyValue) g;

SELECT 'NEXANS / Message.ReceiverID' AS fall,
       COUNT(*) AS verschiedeneWerte, MAX(c) AS zeilenHaeufigsterWert,
       SUM(c) AS zeilenGesamt, SUM(c = 1) AS werteMitGenauEinerZeile
  FROM (SELECT COUNT(*) AS c
          FROM GlassfishDB.Message m
          JOIN GlassfishDB.MessageProperty p
            ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.ReceiverID'
         WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
           AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
           AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                         JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                        WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
         GROUP BY p.MessagePropertyValue) g;

SELECT 'NEXANS / Service.Type' AS fall,
       COUNT(*) AS verschiedeneWerte, MAX(c) AS zeilenHaeufigsterWert,
       SUM(c) AS zeilenGesamt, SUM(c = 1) AS werteMitGenauEinerZeile
  FROM (SELECT COUNT(*) AS c
          FROM GlassfishDB.Message m
          JOIN GlassfishDB.MessageProperty p
            ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Service.Type'
         WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
           AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
           AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                         JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                        WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
         GROUP BY p.MessagePropertyValue) g;

SELECT 'SUTTONS / Converter.TransactionID' AS fall,
       COUNT(*) AS verschiedeneWerte, MAX(c) AS zeilenHaeufigsterWert,
       SUM(c) AS zeilenGesamt, SUM(c = 1) AS werteMitGenauEinerZeile
  FROM (SELECT COUNT(*) AS c
          FROM GlassfishDB.Message m
          JOIN GlassfishDB.MessageProperty p
            ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Converter.TransactionID'
         WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
           AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
           AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                         JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                        WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'SUTTONS')
         GROUP BY p.MessagePropertyValue) g;

SELECT 'SUTTONS / Message.GUID' AS fall,
       COUNT(*) AS verschiedeneWerte, MAX(c) AS zeilenHaeufigsterWert,
       SUM(c) AS zeilenGesamt, SUM(c = 1) AS werteMitGenauEinerZeile
  FROM (SELECT COUNT(*) AS c
          FROM GlassfishDB.Message m
          JOIN GlassfishDB.MessageProperty p
            ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.GUID'
         WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
           AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
           AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                         JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                        WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'SUTTONS')
         GROUP BY p.MessagePropertyValue) g;

SELECT 'SUTTONS / Message.ReceiverID' AS fall,
       COUNT(*) AS verschiedeneWerte, MAX(c) AS zeilenHaeufigsterWert,
       SUM(c) AS zeilenGesamt, SUM(c = 1) AS werteMitGenauEinerZeile
  FROM (SELECT COUNT(*) AS c
          FROM GlassfishDB.Message m
          JOIN GlassfishDB.MessageProperty p
            ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.ReceiverID'
         WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
           AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
           AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                         JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                        WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'SUTTONS')
         GROUP BY p.MessagePropertyValue) g;

SELECT 'SUTTONS / Service.Type' AS fall,
       COUNT(*) AS verschiedeneWerte, MAX(c) AS zeilenHaeufigsterWert,
       SUM(c) AS zeilenGesamt, SUM(c = 1) AS werteMitGenauEinerZeile
  FROM (SELECT COUNT(*) AS c
          FROM GlassfishDB.Message m
          JOIN GlassfishDB.MessageProperty p
            ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Service.Type'
         WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
           AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
           AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                         JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                        WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'SUTTONS')
         GROUP BY p.MessagePropertyValue) g;

SELECT '--- Laufzeiten ---' AS marke;
SELECT QUERY_ID, ROUND(SUM(DURATION), 3) AS sekunden
  FROM information_schema.PROFILING WHERE QUERY_ID > @basis + 1 GROUP BY QUERY_ID ORDER BY QUERY_ID;
