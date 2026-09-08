-- M165 Diagnose — was aendert FORCE INDEX? In beide Richtungen: der Fall, in dem der Optimizer den
-- reinen Wertindex gewaehlt hat (SNDPRN, haeufigster), erzwungen auf MessagePropertyNameValueIDX;
-- und der Fall, in dem er den Namensindex gewaehlt hat (VFN, haeufigster), erzwungen auf
-- MessagePropertyValueIDX. ACHTUNG: FORCE INDEX ist DIAGNOSE und ausdruecklich keine Bauempfehlung.
-- G1 wie n5. Grenze 10 s. Lauf mit --force.
SELECT @@global.read_only AS globalReadOnly;
SET max_statement_time = 10;
SET profiling = 1;
SET profiling_history_size = 100;

SELECT '=== Message.SNDPRN / haeufigster Wert / FORCE INDEX (MessagePropertyNameValueIDX) ===' AS marke;
SELECT QUOTE(v) INTO @w FROM (
  SELECT p.MessagePropertyValue AS v, COUNT(*) AS c
    FROM GlassfishDB.Message m
    JOIN GlassfishDB.MessageProperty p
      ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.SNDPRN'
   WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
     AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
     AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                   JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                  WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
   GROUP BY p.MessagePropertyValue ORDER BY c DESC, v ASC LIMIT 1) x;
SELECT CHAR_LENGTH(@w) - 2 AS laengeDesPruefwertsMindestens;
SET @q = CONCAT('SELECT COUNT(*) AS treffer FROM GlassfishDB.MessageProperty FORCE INDEX (MessagePropertyNameValueIDX) ',
                'WHERE MessagePropertyName = ''Message.SNDPRN'' AND MessagePropertyValue = ', @w);
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
PREPARE qp FROM @q;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp;
DEALLOCATE PREPARE qp;
SELECT QUERY_ID - @basis - 1 AS lauf, ROUND(SUM(DURATION) * 1000, 3) AS ms
  FROM information_schema.PROFILING WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 7
 GROUP BY QUERY_ID ORDER BY QUERY_ID;

SELECT '=== Message.VFN / haeufigster Wert / FORCE INDEX (MessagePropertyValueIDX) ===' AS marke;
SELECT QUOTE(v) INTO @w FROM (
  SELECT p.MessagePropertyValue AS v, COUNT(*) AS c
    FROM GlassfishDB.Message m
    JOIN GlassfishDB.MessageProperty p
      ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.VFN'
   WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
     AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
     AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                   JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                  WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
   GROUP BY p.MessagePropertyValue ORDER BY c DESC, v ASC LIMIT 1) x;
SELECT CHAR_LENGTH(@w) - 2 AS laengeDesPruefwertsMindestens;
SET @q = CONCAT('SELECT COUNT(*) AS treffer FROM GlassfishDB.MessageProperty FORCE INDEX (MessagePropertyValueIDX) ',
                'WHERE MessagePropertyName = ''Message.VFN'' AND MessagePropertyValue = ', @w);
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
PREPARE qp FROM @q;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp;
DEALLOCATE PREPARE qp;
SELECT QUERY_ID - @basis - 1 AS lauf, ROUND(SUM(DURATION) * 1000, 3) AS ms
  FROM information_schema.PROFILING WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 7
 GROUP BY QUERY_ID ORDER BY QUERY_ID;
