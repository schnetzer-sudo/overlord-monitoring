-- M158 Diagnose: was aendert FORCE INDEX - und M160 Gegenprobe der Laengenmessung
-- ACHTUNG: FORCE INDEX ist hier DIAGNOSE und ausdruecklich keine Bauempfehlung.
SELECT @@global.read_only AS globalReadOnly;
SET max_statement_time = 180;
SET profiling = 1;
SET profiling_history_size = 100;

SELECT '--- M160-G Gegenprobe: greift CHAR_LENGTH > 50 ueberhaupt? Alle Namen, 30 Tage, NEXANS ---' AS marke;
SELECT COUNT(*) AS zeilen,
       SUM(CHAR_LENGTH(p.MessagePropertyValue) > 50) AS laengerAls50,
       SUM(CHAR_LENGTH(p.MessagePropertyValue) > 200) AS laengerAls200,
       MAX(CHAR_LENGTH(p.MessagePropertyValue)) AS maxLaenge,
       COUNT(DISTINCT p.MessagePropertyName) AS verschiedeneNamen
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.MessageProperty p ON p.MessageID = m.MessageID
 WHERE m.MessageLastUpdate >= '2025-12-29 00:00:00'
   AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
   AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                 JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS');

SELECT '--- M158-D1 Message.GUID mit FORCE INDEX (MessagePropertyNameValueIDX) ---' AS marke;

SELECT QUOTE(v) INTO @w FROM (
  SELECT p.MessagePropertyValue AS v, COUNT(*) AS c
    FROM GlassfishDB.Message m
    JOIN GlassfishDB.MessageProperty p
      ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.GUID'
   WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
     AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
     AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                   JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                  WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
   GROUP BY p.MessagePropertyValue ORDER BY c DESC, v ASC LIMIT 1) x;
SET @q = CONCAT('SELECT COUNT(*) AS treffer FROM GlassfishDB.MessageProperty FORCE INDEX (MessagePropertyNameValueIDX) ',
                'WHERE MessagePropertyName = ''Message.GUID'' AND MessagePropertyValue = ', @w);
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
PREPARE qp FROM @q;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp;
DEALLOCATE PREPARE qp;
SELECT QUERY_ID, ROUND(SUM(DURATION) * 1000, 3) AS ms
  FROM information_schema.PROFILING WHERE QUERY_ID > @basis + 1 GROUP BY QUERY_ID ORDER BY QUERY_ID;

SELECT '--- M158-D2 Service.Type mit FORCE INDEX (MessagePropertyNameValueIDX), Aufwaermlauf plus zwei ---' AS marke;

SELECT QUOTE(v) INTO @w FROM (
  SELECT p.MessagePropertyValue AS v, COUNT(*) AS c
    FROM GlassfishDB.Message m
    JOIN GlassfishDB.MessageProperty p
      ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Service.Type'
   WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
     AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
     AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                   JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                  WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
   GROUP BY p.MessagePropertyValue ORDER BY c DESC, v ASC LIMIT 1) x;
SET @q = CONCAT('SELECT COUNT(*) AS treffer FROM GlassfishDB.MessageProperty FORCE INDEX (MessagePropertyNameValueIDX) ',
                'WHERE MessagePropertyName = ''Service.Type'' AND MessagePropertyValue = ', @w);
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
PREPARE qp FROM @q;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE qp; EXECUTE qp; EXECUTE qp;
DEALLOCATE PREPARE qp;
SELECT QUERY_ID, ROUND(SUM(DURATION) * 1000, 3) AS ms
  FROM information_schema.PROFILING WHERE QUERY_ID > @basis + 1 GROUP BY QUERY_ID ORDER BY QUERY_ID;
