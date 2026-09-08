-- L4-Pfad-Runde - Sitzung B1b: Nachpruefung der Vorprobe. Die GROUP-BY-Form lief ueber 30 Tage
-- laut Profil in 6 ms, obwohl ANALYZE 102.284 gelesene Indexeintraege zeigt. Geprueft werden
-- Wanduhr (SYSDATE(6) um EXECUTE), Handler-Zaehler und Query-Cache je Form, dazu Fassung A als
-- Eichung. NEXANS / Message.SNDPRN / 30 T. Grenze 10 s. --force.
SELECT @@global.read_only AS globalReadOnly;
SET max_statement_time = 10;
SET profiling = 1;
SET profiling_history_size = 100;
SELECT @@have_query_cache AS haveQueryCache, @@query_cache_type AS queryCacheType,
       @@query_cache_size AS queryCacheSize;
SELECT '=== Eichung: SELECT 1 ===' AS marke;
SET @q = 'SELECT 1';
SELECT VARIABLE_VALUE INTO @h1 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY';
SELECT VARIABLE_VALUE INTO @h2 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT';
SELECT VARIABLE_VALUE INTO @h3 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT';
SELECT VARIABLE_VALUE INTO @h4 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE';
SELECT VARIABLE_VALUE INTO @h5 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS';
SELECT VARIABLE_VALUE INTO @h6 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND';
SELECT VARIABLE_VALUE INTO @qc FROM information_schema.GLOBAL_STATUS WHERE VARIABLE_NAME = 'QCACHE_HITS';
SELECT MAX(QUERY_ID) INTO @basisH FROM information_schema.PROFILING;
PREPARE qp FROM @q;
SELECT SYSDATE(6) INTO @t0;
EXECUTE qp;
SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @dt;
DEALLOCATE PREPARE qp;
SELECT 'leer' AS fall, ROUND(@dt / 1000, 3) AS wandMs,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY') - @h1 AS readKey,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT') - @h2 AS readNext,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT') - @h3 AS readRndNext,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE') - @h4 AS tmpWrite,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS') - @h5 AS icpAttempts,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND') - @h6 AS readRnd,
       (SELECT VARIABLE_VALUE FROM information_schema.GLOBAL_STATUS WHERE VARIABLE_NAME = 'QCACHE_HITS') - @qc AS qcacheHitsGlobal;
SELECT QUERY_ID - @basisH AS lfd, ROUND(SUM(DURATION) * 1000, 3) AS profilMs
  FROM information_schema.PROFILING WHERE QUERY_ID > @basisH + 1 AND QUERY_ID <= @basisH + 4
 GROUP BY QUERY_ID ORDER BY QUERY_ID;
SELECT '=== B1b / NEXANS / Message.SNDPRN / 30T / BG ===' AS marke;
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
SET @q = CONCAT(
 'SELECT COUNT(*) AS zeilen, MAX(CHAR_LENGTH(p2.ProcessName)) AS maxPnLaenge, ',
 '       MAX(CHAR_LENGTH(prj.ProjectName)) AS maxPjLaenge ',
 'FROM (SELECT f.MessageID, f.MessageLastUpdate, f.MessageStatus, f.ProcessID ',
 '        FROM (SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID ',
 '                FROM GlassfishDB.Message m ',
 '               WHERE m.MessageLastUpdate >= ''2025-11-30 00:00:00'' AND m.MessageLastUpdate < ''2025-12-30 00:00:00'' ',
 '                 AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr ',
 '                               JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID ',
 '                              WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = ''NEXANS'') ',
 '               GROUP BY m.MessageID) AS f ',
 '        JOIN GlassfishDB.MessageProperty mp ',
 '          ON mp.MessageID = f.MessageID ',
 '         AND mp.MessagePropertyName = ''Message.SNDPRN'' AND mp.MessagePropertyValue = ', @w, ' ',
 '       GROUP BY f.MessageID ',
 '       ORDER BY f.MessageLastUpdate DESC, f.MessageID DESC LIMIT 51) AS treffer ',
 'LEFT JOIN GlassfishDB.Process p2 ON p2.ProcessID = treffer.ProcessID ',
 'LEFT JOIN GlassfishDB.Project prj ON prj.ProjectID = p2.ProjectID');
SELECT VARIABLE_VALUE INTO @h1 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY';
SELECT VARIABLE_VALUE INTO @h2 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT';
SELECT VARIABLE_VALUE INTO @h3 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT';
SELECT VARIABLE_VALUE INTO @h4 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE';
SELECT VARIABLE_VALUE INTO @h5 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS';
SELECT VARIABLE_VALUE INTO @h6 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND';
SELECT VARIABLE_VALUE INTO @qc FROM information_schema.GLOBAL_STATUS WHERE VARIABLE_NAME = 'QCACHE_HITS';
SELECT MAX(QUERY_ID) INTO @basisH FROM information_schema.PROFILING;
PREPARE qp FROM @q;
SELECT SYSDATE(6) INTO @t0;
EXECUTE qp;
SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @dt;
DEALLOCATE PREPARE qp;
SELECT 'BG Lauf 1' AS fall, ROUND(@dt / 1000, 3) AS wandMs,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY') - @h1 AS readKey,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT') - @h2 AS readNext,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT') - @h3 AS readRndNext,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE') - @h4 AS tmpWrite,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS') - @h5 AS icpAttempts,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND') - @h6 AS readRnd,
       (SELECT VARIABLE_VALUE FROM information_schema.GLOBAL_STATUS WHERE VARIABLE_NAME = 'QCACHE_HITS') - @qc AS qcacheHitsGlobal;
SELECT QUERY_ID - @basisH AS lfd, ROUND(SUM(DURATION) * 1000, 3) AS profilMs
  FROM information_schema.PROFILING WHERE QUERY_ID > @basisH + 1 AND QUERY_ID <= @basisH + 4
 GROUP BY QUERY_ID ORDER BY QUERY_ID;
SELECT VARIABLE_VALUE INTO @h1 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY';
SELECT VARIABLE_VALUE INTO @h2 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT';
SELECT VARIABLE_VALUE INTO @h3 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT';
SELECT VARIABLE_VALUE INTO @h4 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE';
SELECT VARIABLE_VALUE INTO @h5 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS';
SELECT VARIABLE_VALUE INTO @h6 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND';
SELECT VARIABLE_VALUE INTO @qc FROM information_schema.GLOBAL_STATUS WHERE VARIABLE_NAME = 'QCACHE_HITS';
SELECT MAX(QUERY_ID) INTO @basisH FROM information_schema.PROFILING;
PREPARE qp FROM @q;
SELECT SYSDATE(6) INTO @t0;
EXECUTE qp;
SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @dt;
DEALLOCATE PREPARE qp;
SELECT 'BG Lauf 2' AS fall, ROUND(@dt / 1000, 3) AS wandMs,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY') - @h1 AS readKey,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT') - @h2 AS readNext,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT') - @h3 AS readRndNext,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE') - @h4 AS tmpWrite,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS') - @h5 AS icpAttempts,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND') - @h6 AS readRnd,
       (SELECT VARIABLE_VALUE FROM information_schema.GLOBAL_STATUS WHERE VARIABLE_NAME = 'QCACHE_HITS') - @qc AS qcacheHitsGlobal;
SELECT QUERY_ID - @basisH AS lfd, ROUND(SUM(DURATION) * 1000, 3) AS profilMs
  FROM information_schema.PROFILING WHERE QUERY_ID > @basisH + 1 AND QUERY_ID <= @basisH + 4
 GROUP BY QUERY_ID ORDER BY QUERY_ID;
SELECT '=== B1b / NEXANS / Message.SNDPRN / 30T / BL ===' AS marke;
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
SET @q = CONCAT(
 'SELECT COUNT(*) AS zeilen, MAX(CHAR_LENGTH(p2.ProcessName)) AS maxPnLaenge, ',
 '       MAX(CHAR_LENGTH(prj.ProjectName)) AS maxPjLaenge ',
 'FROM (SELECT f.MessageID, f.MessageLastUpdate, f.MessageStatus, f.ProcessID ',
 '        FROM (SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID ',
 '                FROM GlassfishDB.Message m ',
 '               WHERE m.MessageLastUpdate >= ''2025-11-30 00:00:00'' AND m.MessageLastUpdate < ''2025-12-30 00:00:00'' ',
 '                 AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr ',
 '                               JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID ',
 '                              WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = ''NEXANS'') ',
 '               ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC LIMIT 1000000000) AS f ',
 '        JOIN GlassfishDB.MessageProperty mp ',
 '          ON mp.MessageID = f.MessageID ',
 '         AND mp.MessagePropertyName = ''Message.SNDPRN'' AND mp.MessagePropertyValue = ', @w, ' ',
 '       GROUP BY f.MessageID ',
 '       ORDER BY f.MessageLastUpdate DESC, f.MessageID DESC LIMIT 51) AS treffer ',
 'LEFT JOIN GlassfishDB.Process p2 ON p2.ProcessID = treffer.ProcessID ',
 'LEFT JOIN GlassfishDB.Project prj ON prj.ProjectID = p2.ProjectID');
SELECT VARIABLE_VALUE INTO @h1 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY';
SELECT VARIABLE_VALUE INTO @h2 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT';
SELECT VARIABLE_VALUE INTO @h3 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT';
SELECT VARIABLE_VALUE INTO @h4 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE';
SELECT VARIABLE_VALUE INTO @h5 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS';
SELECT VARIABLE_VALUE INTO @h6 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND';
SELECT VARIABLE_VALUE INTO @qc FROM information_schema.GLOBAL_STATUS WHERE VARIABLE_NAME = 'QCACHE_HITS';
SELECT MAX(QUERY_ID) INTO @basisH FROM information_schema.PROFILING;
PREPARE qp FROM @q;
SELECT SYSDATE(6) INTO @t0;
EXECUTE qp;
SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @dt;
DEALLOCATE PREPARE qp;
SELECT 'BL Lauf 1' AS fall, ROUND(@dt / 1000, 3) AS wandMs,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY') - @h1 AS readKey,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT') - @h2 AS readNext,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT') - @h3 AS readRndNext,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE') - @h4 AS tmpWrite,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS') - @h5 AS icpAttempts,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND') - @h6 AS readRnd,
       (SELECT VARIABLE_VALUE FROM information_schema.GLOBAL_STATUS WHERE VARIABLE_NAME = 'QCACHE_HITS') - @qc AS qcacheHitsGlobal;
SELECT QUERY_ID - @basisH AS lfd, ROUND(SUM(DURATION) * 1000, 3) AS profilMs
  FROM information_schema.PROFILING WHERE QUERY_ID > @basisH + 1 AND QUERY_ID <= @basisH + 4
 GROUP BY QUERY_ID ORDER BY QUERY_ID;
SELECT VARIABLE_VALUE INTO @h1 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY';
SELECT VARIABLE_VALUE INTO @h2 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT';
SELECT VARIABLE_VALUE INTO @h3 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT';
SELECT VARIABLE_VALUE INTO @h4 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE';
SELECT VARIABLE_VALUE INTO @h5 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS';
SELECT VARIABLE_VALUE INTO @h6 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND';
SELECT VARIABLE_VALUE INTO @qc FROM information_schema.GLOBAL_STATUS WHERE VARIABLE_NAME = 'QCACHE_HITS';
SELECT MAX(QUERY_ID) INTO @basisH FROM information_schema.PROFILING;
PREPARE qp FROM @q;
SELECT SYSDATE(6) INTO @t0;
EXECUTE qp;
SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @dt;
DEALLOCATE PREPARE qp;
SELECT 'BL Lauf 2' AS fall, ROUND(@dt / 1000, 3) AS wandMs,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY') - @h1 AS readKey,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT') - @h2 AS readNext,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT') - @h3 AS readRndNext,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE') - @h4 AS tmpWrite,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS') - @h5 AS icpAttempts,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND') - @h6 AS readRnd,
       (SELECT VARIABLE_VALUE FROM information_schema.GLOBAL_STATUS WHERE VARIABLE_NAME = 'QCACHE_HITS') - @qc AS qcacheHitsGlobal;
SELECT QUERY_ID - @basisH AS lfd, ROUND(SUM(DURATION) * 1000, 3) AS profilMs
  FROM information_schema.PROFILING WHERE QUERY_ID > @basisH + 1 AND QUERY_ID <= @basisH + 4
 GROUP BY QUERY_ID ORDER BY QUERY_ID;
SELECT '=== B1b / NEXANS / Message.SNDPRN / 30T / A ===' AS marke;
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
SET @q = CONCAT(
 'SELECT COUNT(*) AS zeilen, MAX(CHAR_LENGTH(p2.ProcessName)) AS maxPnLaenge, ',
 '       MAX(CHAR_LENGTH(prj.ProjectName)) AS maxPjLaenge ',
 'FROM (SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID ',
 '        FROM GlassfishDB.MessageProperty mp ',
 '        JOIN GlassfishDB.Message m ON m.MessageID = mp.MessageID ',
 '       WHERE mp.MessagePropertyName = ''Message.SNDPRN'' AND mp.MessagePropertyValue = ', @w, ' ',
 '         AND m.MessageLastUpdate >= ''2025-11-30 00:00:00'' AND m.MessageLastUpdate < ''2025-12-30 00:00:00'' ',
 '         AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr ',
 '                       JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID ',
 '                      WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = ''NEXANS'') ',
 '       GROUP BY m.MessageID ',
 '       ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC LIMIT 51) AS treffer ',
 'LEFT JOIN GlassfishDB.Process p2 ON p2.ProcessID = treffer.ProcessID ',
 'LEFT JOIN GlassfishDB.Project prj ON prj.ProjectID = p2.ProjectID');
SELECT VARIABLE_VALUE INTO @h1 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY';
SELECT VARIABLE_VALUE INTO @h2 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT';
SELECT VARIABLE_VALUE INTO @h3 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT';
SELECT VARIABLE_VALUE INTO @h4 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE';
SELECT VARIABLE_VALUE INTO @h5 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS';
SELECT VARIABLE_VALUE INTO @h6 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND';
SELECT VARIABLE_VALUE INTO @qc FROM information_schema.GLOBAL_STATUS WHERE VARIABLE_NAME = 'QCACHE_HITS';
SELECT MAX(QUERY_ID) INTO @basisH FROM information_schema.PROFILING;
PREPARE qp FROM @q;
SELECT SYSDATE(6) INTO @t0;
EXECUTE qp;
SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @dt;
DEALLOCATE PREPARE qp;
SELECT 'A Lauf 1' AS fall, ROUND(@dt / 1000, 3) AS wandMs,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY') - @h1 AS readKey,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT') - @h2 AS readNext,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT') - @h3 AS readRndNext,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE') - @h4 AS tmpWrite,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS') - @h5 AS icpAttempts,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND') - @h6 AS readRnd,
       (SELECT VARIABLE_VALUE FROM information_schema.GLOBAL_STATUS WHERE VARIABLE_NAME = 'QCACHE_HITS') - @qc AS qcacheHitsGlobal;
SELECT QUERY_ID - @basisH AS lfd, ROUND(SUM(DURATION) * 1000, 3) AS profilMs
  FROM information_schema.PROFILING WHERE QUERY_ID > @basisH + 1 AND QUERY_ID <= @basisH + 4
 GROUP BY QUERY_ID ORDER BY QUERY_ID;
SELECT VARIABLE_VALUE INTO @h1 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY';
SELECT VARIABLE_VALUE INTO @h2 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT';
SELECT VARIABLE_VALUE INTO @h3 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT';
SELECT VARIABLE_VALUE INTO @h4 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE';
SELECT VARIABLE_VALUE INTO @h5 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS';
SELECT VARIABLE_VALUE INTO @h6 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND';
SELECT VARIABLE_VALUE INTO @qc FROM information_schema.GLOBAL_STATUS WHERE VARIABLE_NAME = 'QCACHE_HITS';
SELECT MAX(QUERY_ID) INTO @basisH FROM information_schema.PROFILING;
PREPARE qp FROM @q;
SELECT SYSDATE(6) INTO @t0;
EXECUTE qp;
SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @dt;
DEALLOCATE PREPARE qp;
SELECT 'A Lauf 2' AS fall, ROUND(@dt / 1000, 3) AS wandMs,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY') - @h1 AS readKey,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT') - @h2 AS readNext,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT') - @h3 AS readRndNext,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE') - @h4 AS tmpWrite,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS') - @h5 AS icpAttempts,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND') - @h6 AS readRnd,
       (SELECT VARIABLE_VALUE FROM information_schema.GLOBAL_STATUS WHERE VARIABLE_NAME = 'QCACHE_HITS') - @qc AS qcacheHitsGlobal;
SELECT QUERY_ID - @basisH AS lfd, ROUND(SUM(DURATION) * 1000, 3) AS profilMs
  FROM information_schema.PROFILING WHERE QUERY_ID > @basisH + 1 AND QUERY_ID <= @basisH + 4
 GROUP BY QUERY_ID ORDER BY QUERY_ID;
SELECT NOW() AS serverzeitEnde;
