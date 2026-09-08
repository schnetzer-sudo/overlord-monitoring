-- M163 Ergaenzung — Rangverteilung (nur Raenge und Zaehler, G1) und die 90-Tage-Gegenprobe
-- zur Kategoriefrage, NEXANS. Wie M157-3: waechst die Zahl der Werte mit dem Fenster oder nicht?
-- Erhebung: max_statement_time = 180 (Abweichung 2).
SELECT @@global.read_only AS globalReadOnly;
SET max_statement_time = 180;
SET profiling = 1;
SET profiling_history_size = 100;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;

SELECT '--- M163-4 Rang 1 bis 5 je Name, NEXANS, 30 Tage (Zeilen je Rang, nie der Wert) ---' AS marke;

SELECT 'Message.DestinationFilename' AS name, rang, zeilen
  FROM (SELECT ROW_NUMBER() OVER (ORDER BY c DESC, v ASC) AS rang, c AS zeilen
          FROM (SELECT p.MessagePropertyValue AS v, COUNT(*) AS c
                  FROM GlassfishDB.Message m
                  JOIN GlassfishDB.MessageProperty p
                    ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.DestinationFilename'
                 WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
                   AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
                   AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                                 JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                                WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
                 GROUP BY p.MessagePropertyValue) g) r
 WHERE rang <= 5 ORDER BY rang;

SELECT 'Message.SNDPRN' AS name, rang, zeilen
  FROM (SELECT ROW_NUMBER() OVER (ORDER BY c DESC, v ASC) AS rang, c AS zeilen
          FROM (SELECT p.MessagePropertyValue AS v, COUNT(*) AS c
                  FROM GlassfishDB.Message m
                  JOIN GlassfishDB.MessageProperty p
                    ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.SNDPRN'
                 WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
                   AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
                   AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                                 JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                                WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
                 GROUP BY p.MessagePropertyValue) g) r
 WHERE rang <= 5 ORDER BY rang;

SELECT 'Message.VFN' AS name, rang, zeilen
  FROM (SELECT ROW_NUMBER() OVER (ORDER BY c DESC, v ASC) AS rang, c AS zeilen
          FROM (SELECT p.MessagePropertyValue AS v, COUNT(*) AS c
                  FROM GlassfishDB.Message m
                  JOIN GlassfishDB.MessageProperty p
                    ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.VFN'
                 WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
                   AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
                   AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                                 JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                                WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
                 GROUP BY p.MessagePropertyValue) g) r
 WHERE rang <= 5 ORDER BY rang;

SELECT '--- M163-5 Werte ueber der Deckelung (mehr als 51 Zeilen) je Name, NEXANS, 30 Tage ---' AS marke;

SELECT 'Message.DestinationFilename' AS name,
       SUM(c > 51) AS werteUeber51, SUM(CASE WHEN c > 51 THEN c END) AS zeilenInWertenUeber51,
       SUM(c BETWEEN 2 AND 51) AS werteVon2Bis51, SUM(c = 1) AS werteMitEinerZeile
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

SELECT 'Message.SNDPRN' AS name,
       SUM(c > 51) AS werteUeber51, SUM(CASE WHEN c > 51 THEN c END) AS zeilenInWertenUeber51,
       SUM(c BETWEEN 2 AND 51) AS werteVon2Bis51, SUM(c = 1) AS werteMitEinerZeile
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

SELECT 'Message.VFN' AS name,
       SUM(c > 51) AS werteUeber51, SUM(CASE WHEN c > 51 THEN c END) AS zeilenInWertenUeber51,
       SUM(c BETWEEN 2 AND 51) AS werteVon2Bis51, SUM(c = 1) AS werteMitEinerZeile
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

SELECT '--- M163-6 90 Tage (2025-10-01 bis 2025-12-30), NEXANS — wie M157-3 ---' AS marke;

SELECT 'Message.DestinationFilename' AS name,
       COUNT(*) AS verschiedeneWerte, MAX(c) AS zeilenHaeufigsterWert, SUM(c) AS zeilenGesamt, SUM(c = 1) AS werteMitGenauEinerZeile
  FROM (SELECT COUNT(*) AS c
          FROM GlassfishDB.Message m
          JOIN GlassfishDB.MessageProperty p
            ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.DestinationFilename'
         WHERE m.MessageLastUpdate >= '2025-10-01 00:00:00'
           AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
           AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                         JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                        WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
         GROUP BY p.MessagePropertyValue) g;

SELECT 'Message.SNDPRN' AS name,
       COUNT(*) AS verschiedeneWerte, MAX(c) AS zeilenHaeufigsterWert, SUM(c) AS zeilenGesamt, SUM(c = 1) AS werteMitGenauEinerZeile
  FROM (SELECT COUNT(*) AS c
          FROM GlassfishDB.Message m
          JOIN GlassfishDB.MessageProperty p
            ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.SNDPRN'
         WHERE m.MessageLastUpdate >= '2025-10-01 00:00:00'
           AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
           AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                         JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                        WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
         GROUP BY p.MessagePropertyValue) g;

SELECT 'Message.VFN' AS name,
       COUNT(*) AS verschiedeneWerte, MAX(c) AS zeilenHaeufigsterWert, SUM(c) AS zeilenGesamt, SUM(c = 1) AS werteMitGenauEinerZeile
  FROM (SELECT COUNT(*) AS c
          FROM GlassfishDB.Message m
          JOIN GlassfishDB.MessageProperty p
            ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.VFN'
         WHERE m.MessageLastUpdate >= '2025-10-01 00:00:00'
           AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
           AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                         JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                        WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
         GROUP BY p.MessagePropertyValue) g;

SELECT '--- Laufzeiten (Einzellauf, Erhebung) ---' AS marke;
SELECT QUERY_ID, ROUND(SUM(DURATION), 3) AS sekunden
  FROM information_schema.PROFILING WHERE QUERY_ID > @basis + 1 GROUP BY QUERY_ID ORDER BY QUERY_ID;
