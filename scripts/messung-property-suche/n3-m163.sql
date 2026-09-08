-- M163 — Werteverteilung der drei neuen Namen. Statement aus M157 (s11), unveraendert.
-- Fenster B, je Mandant. Regel G1: KEIN MessagePropertyValue in der Ausgabe, nur Zaehler.
-- Erhebung: max_statement_time = 180 wie s11 der Hauptrunde (Abweichung von §4, gemeldet).
SELECT @@global.read_only AS globalReadOnly;
SET max_statement_time = 180;
SET profiling = 1;
SET profiling_history_size = 100;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;

SELECT '--- M163-1 EXPLAIN der Form (NEXANS / Message.SNDPRN) ---' AS marke;
EXPLAIN
SELECT COUNT(*) AS verschiedeneWerte, MAX(c) AS zeilenHaeufigsterWert,
       SUM(c) AS zeilenGesamt, SUM(c = 1) AS werteMitGenauEinerZeile
  FROM (SELECT COUNT(*) AS c
          FROM GlassfishDB.Message m
          JOIN GlassfishDB.MessageProperty p
            ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.SNDPRN'
         WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
           AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
           AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                         JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                        WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
         GROUP BY p.MessagePropertyValue) g;

SELECT '--- M163-2 NEXANS ---' AS marke;

SELECT 'NEXANS / Message.DestinationFilename' AS fall,
       COUNT(*) AS verschiedeneWerte, MAX(c) AS zeilenHaeufigsterWert,
       SUM(c) AS zeilenGesamt, SUM(c = 1) AS werteMitGenauEinerZeile
  FROM (SELECT COUNT(*) AS c
          FROM GlassfishDB.Message m
          JOIN GlassfishDB.MessageProperty p
            ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.DestinationFilename'
         WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
           AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
           AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                         JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                        WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
         GROUP BY p.MessagePropertyValue) g;

SELECT 'NEXANS / Message.SNDPRN' AS fall,
       COUNT(*) AS verschiedeneWerte, MAX(c) AS zeilenHaeufigsterWert,
       SUM(c) AS zeilenGesamt, SUM(c = 1) AS werteMitGenauEinerZeile
  FROM (SELECT COUNT(*) AS c
          FROM GlassfishDB.Message m
          JOIN GlassfishDB.MessageProperty p
            ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.SNDPRN'
         WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
           AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
           AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                         JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                        WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
         GROUP BY p.MessagePropertyValue) g;

SELECT 'NEXANS / Message.VFN' AS fall,
       COUNT(*) AS verschiedeneWerte, MAX(c) AS zeilenHaeufigsterWert,
       SUM(c) AS zeilenGesamt, SUM(c = 1) AS werteMitGenauEinerZeile
  FROM (SELECT COUNT(*) AS c
          FROM GlassfishDB.Message m
          JOIN GlassfishDB.MessageProperty p
            ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.VFN'
         WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
           AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
           AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                         JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                        WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
         GROUP BY p.MessagePropertyValue) g;

SELECT '--- M163-3 SUTTONS ---' AS marke;

SELECT 'SUTTONS / Message.DestinationFilename' AS fall,
       COUNT(*) AS verschiedeneWerte, MAX(c) AS zeilenHaeufigsterWert,
       SUM(c) AS zeilenGesamt, SUM(c = 1) AS werteMitGenauEinerZeile
  FROM (SELECT COUNT(*) AS c
          FROM GlassfishDB.Message m
          JOIN GlassfishDB.MessageProperty p
            ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.DestinationFilename'
         WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
           AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
           AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                         JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                        WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'SUTTONS')
         GROUP BY p.MessagePropertyValue) g;

SELECT 'SUTTONS / Message.SNDPRN' AS fall,
       COUNT(*) AS verschiedeneWerte, MAX(c) AS zeilenHaeufigsterWert,
       SUM(c) AS zeilenGesamt, SUM(c = 1) AS werteMitGenauEinerZeile
  FROM (SELECT COUNT(*) AS c
          FROM GlassfishDB.Message m
          JOIN GlassfishDB.MessageProperty p
            ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.SNDPRN'
         WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
           AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
           AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                         JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                        WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'SUTTONS')
         GROUP BY p.MessagePropertyValue) g;

SELECT 'SUTTONS / Message.VFN' AS fall,
       COUNT(*) AS verschiedeneWerte, MAX(c) AS zeilenHaeufigsterWert,
       SUM(c) AS zeilenGesamt, SUM(c = 1) AS werteMitGenauEinerZeile
  FROM (SELECT COUNT(*) AS c
          FROM GlassfishDB.Message m
          JOIN GlassfishDB.MessageProperty p
            ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.VFN'
         WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
           AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
           AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                         JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                        WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'SUTTONS')
         GROUP BY p.MessagePropertyValue) g;

SELECT '--- Laufzeiten (Einzellauf, Erhebung) ---' AS marke;
SELECT QUERY_ID, ROUND(SUM(DURATION), 3) AS sekunden
  FROM information_schema.PROFILING WHERE QUERY_ID > @basis + 1 GROUP BY QUERY_ID ORDER BY QUERY_ID;
