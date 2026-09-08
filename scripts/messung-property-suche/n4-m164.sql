-- M164 — Laengenverteilung der drei neuen Namen, NEXANS, Fenster B. Nur Laengen und Zaehler (G1).
-- SUTTONS entfaellt: keine Zeile im Fenster (M162-5).
-- Zusatz fuer M167: Wie breit ist der Praefix-Vorfilter wirklich? (Werte je 50-Zeichen-Praefix)
-- Erhebung: max_statement_time = 180 (Abweichung 2). Lauf mit --force.
SELECT @@global.read_only AS globalReadOnly;
SET max_statement_time = 180;
SET profiling = 1;
SET profiling_history_size = 100;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;

SELECT '--- M164-1 Laengen je Name: Minimum, Maximum, Mittel, Median, Anteil ueber 50 ---' AS marke;

SELECT 'Message.DestinationFilename' AS name, COUNT(*) AS zeilen, SUM(l IS NULL) AS wertNull,
       MIN(l) AS minLaenge, MAX(l) AS maxLaenge, ROUND(AVG(l), 2) AS mittelLaenge, MAX(med) AS median,
       SUM(l > 50) AS laengerAls50, ROUND(100.0 * SUM(l > 50) / COUNT(*), 3) AS anteilUeber50,
       SUM(l > 100) AS laengerAls100
  FROM (SELECT CHAR_LENGTH(p.MessagePropertyValue) AS l,
               MEDIAN(CHAR_LENGTH(p.MessagePropertyValue)) OVER () AS med
          FROM GlassfishDB.Message m
          JOIN GlassfishDB.MessageProperty p
            ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.DestinationFilename'
         WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
           AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
           AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                         JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                        WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')) t;

SELECT 'Message.SNDPRN' AS name, COUNT(*) AS zeilen, SUM(l IS NULL) AS wertNull,
       MIN(l) AS minLaenge, MAX(l) AS maxLaenge, ROUND(AVG(l), 2) AS mittelLaenge, MAX(med) AS median,
       SUM(l > 50) AS laengerAls50, ROUND(100.0 * SUM(l > 50) / COUNT(*), 3) AS anteilUeber50,
       SUM(l > 100) AS laengerAls100
  FROM (SELECT CHAR_LENGTH(p.MessagePropertyValue) AS l,
               MEDIAN(CHAR_LENGTH(p.MessagePropertyValue)) OVER () AS med
          FROM GlassfishDB.Message m
          JOIN GlassfishDB.MessageProperty p
            ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.SNDPRN'
         WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
           AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
           AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                         JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                        WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')) t;

SELECT 'Message.VFN' AS name, COUNT(*) AS zeilen, SUM(l IS NULL) AS wertNull,
       MIN(l) AS minLaenge, MAX(l) AS maxLaenge, ROUND(AVG(l), 2) AS mittelLaenge, MAX(med) AS median,
       SUM(l > 50) AS laengerAls50, ROUND(100.0 * SUM(l > 50) / COUNT(*), 3) AS anteilUeber50,
       SUM(l > 100) AS laengerAls100
  FROM (SELECT CHAR_LENGTH(p.MessagePropertyValue) AS l,
               MEDIAN(CHAR_LENGTH(p.MessagePropertyValue)) OVER () AS med
          FROM GlassfishDB.Message m
          JOIN GlassfishDB.MessageProperty p
            ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.VFN'
         WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
           AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
           AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                         JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                        WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')) t;

SELECT '--- M164-2 Laengenklassen Message.DestinationFilename (Zeilen und verschiedene Werte je Klasse) ---' AS marke;
SELECT CASE WHEN l <= 50 THEN 'bis 50' WHEN l <= 60 THEN '51 bis 60' WHEN l <= 70 THEN '61 bis 70' ELSE 'ueber 70' END AS klasse,
       COUNT(*) AS zeilen, COUNT(DISTINCT v) AS werte
  FROM (SELECT p.MessagePropertyValue AS v, CHAR_LENGTH(p.MessagePropertyValue) AS l
          FROM GlassfishDB.Message m
          JOIN GlassfishDB.MessageProperty p
            ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.DestinationFilename'
         WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
           AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
           AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                         JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                        WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')) t
 GROUP BY klasse ORDER BY MIN(l);

SELECT '--- M164-3 Histogramm je Laenge, Message.DestinationFilename ---' AS marke;
SELECT CHAR_LENGTH(p.MessagePropertyValue) AS laenge, COUNT(*) AS zeilen, COUNT(DISTINCT p.MessagePropertyValue) AS werte
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.MessageProperty p
    ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.DestinationFilename'
 WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
   AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
   AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                 JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
 GROUP BY laenge ORDER BY laenge;

SELECT '--- M164-4 Breite des Praefix-Vorfilters: Werte je 50-Zeichen-Praefix, alle Zeilen des Namens ---' AS marke;
SELECT 'Message.DestinationFilename' AS name,
       COUNT(*) AS praefixe, SUM(werte) AS werte, SUM(zeilen) AS zeilen,
       SUM(werte > 1) AS praefixeMitMehrerenWerten, SUM(CASE WHEN werte > 1 THEN zeilen END) AS zeilenUnterSolchenPraefixen,
       MAX(werte) AS maxWerteJePraefix, MAX(zeilen) AS maxZeilenJePraefix
  FROM (SELECT LEFT(p.MessagePropertyValue, 50) AS pf, COUNT(DISTINCT p.MessagePropertyValue) AS werte, COUNT(*) AS zeilen
          FROM GlassfishDB.Message m
          JOIN GlassfishDB.MessageProperty p
            ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.DestinationFilename'
         WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
           AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
           AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                         JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                        WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
         GROUP BY pf) g;

SELECT '--- M164-5 dieselbe Breite, nur Werte ueber 50 Zeichen ---' AS marke;
SELECT 'Message.DestinationFilename, nur > 50' AS name,
       COUNT(*) AS praefixe, SUM(werte) AS werte, SUM(zeilen) AS zeilen,
       SUM(werte > 1) AS praefixeMitMehrerenWerten, SUM(CASE WHEN werte > 1 THEN zeilen END) AS zeilenUnterSolchenPraefixen,
       MAX(werte) AS maxWerteJePraefix, MAX(zeilen) AS maxZeilenJePraefix
  FROM (SELECT LEFT(p.MessagePropertyValue, 50) AS pf, COUNT(DISTINCT p.MessagePropertyValue) AS werte, COUNT(*) AS zeilen
          FROM GlassfishDB.Message m
          JOIN GlassfishDB.MessageProperty p
            ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.DestinationFilename'
         WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
           AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
           AND CHAR_LENGTH(p.MessagePropertyValue) > 50
           AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                         JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                        WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
         GROUP BY pf) g;

SELECT '--- Laufzeiten (Einzellauf, Erhebung) ---' AS marke;
SELECT QUERY_ID, ROUND(SUM(DURATION), 3) AS sekunden
  FROM information_schema.PROFILING WHERE QUERY_ID > @basis + 1 GROUP BY QUERY_ID ORDER BY QUERY_ID;
