-- L4-Pfad-Runde - Sitzung B2: M168 - was Fassung B kostet, und daneben Fassung C (Ergaenzung 2).
-- Drei Namen x drei Fenster x zwei Fassungen, NEXANS. Pruefwert je Fall der haeufigste Wert des
-- 30-Tage-Fensters (Boesfall, wie M166). Je Fall: EXPLAIN, ANALYZE (ein Lauf, geschaetzt gegen
-- gelesen - M170), Aufwaermlauf mit Handler-Zaehlern, fuenf Laeufe mit Wanduhr, Profil daneben.
-- Grenze 10 s = Lese-Pool; ein Abbruch ist das Ergebnis. --force. G1: kein Wert im Skript.
SELECT @@global.read_only AS globalReadOnly;
SET max_statement_time = 10;
SET profiling = 1;
SET profiling_history_size = 100;
SELECT '=== M168 / NEXANS / Message.SNDPRN / 24h / B materialisiert ===' AS marke;
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
 '               WHERE m.MessageLastUpdate >= ''2025-12-29 00:00:00'' AND m.MessageLastUpdate < ''2025-12-30 00:00:00'' ',
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
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
SET @a = CONCAT('ANALYZE ', @q);
PREPARE ap FROM @a; EXECUTE ap; DEALLOCATE PREPARE ap;
SELECT VARIABLE_VALUE INTO @h1 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY';
SELECT VARIABLE_VALUE INTO @h2 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT';
SELECT VARIABLE_VALUE INTO @h3 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT';
SELECT VARIABLE_VALUE INTO @h4 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE';
SELECT VARIABLE_VALUE INTO @h5 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS';
SELECT VARIABLE_VALUE INTO @h6 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND';
PREPARE qp FROM @q;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d1;
SELECT (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY') - @h1 AS readKey,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT') - @h2 AS readNext,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT') - @h3 AS readRndNext,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE') - @h4 AS tmpWrite,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS') - @h5 AS icpAttempts,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND') - @h6 AS readRnd;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d2;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d3;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d4;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d5;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d6;
DEALLOCATE PREPARE qp;
SELECT ROUND(LEAST(@d2, @d3, @d4, @d5, @d6) / 1000, 3) AS besteWandMsLauf2bis6,
       ROUND(GREATEST(@d2, @d3, @d4, @d5, @d6) / 1000, 3) AS schlechtesteWandMs,
       ROUND(@d1 / 1000, 3) AS aufwaermlaufWandMs;
SELECT ROUND(@d2 / 1000, 3) AS l2, ROUND(@d3 / 1000, 3) AS l3, ROUND(@d4 / 1000, 3) AS l4,
       ROUND(@d5 / 1000, 3) AS l5, ROUND(@d6 / 1000, 3) AS l6;
SELECT COUNT(*) AS profilLaeufe, ROUND(MIN(ms), 3) AS besteProfilMs, ROUND(MAX(ms), 3) AS schlechtesteProfilMs
  FROM (SELECT QUERY_ID, SUM(DURATION) * 1000 AS ms FROM information_schema.PROFILING
         WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 16 AND (QUERY_ID - @basis) MOD 3 = 0
         GROUP BY QUERY_ID) l;

SELECT '=== M168 / NEXANS / Message.SNDPRN / 24h / C bindend ===' AS marke;
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
 '               WHERE m.MessageLastUpdate >= ''2025-12-29 00:00:00'' AND m.MessageLastUpdate < ''2025-12-30 00:00:00'' ',
 '                 AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr ',
 '                               JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID ',
 '                              WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = ''NEXANS'') ',
 '               ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC LIMIT 1000000000) AS f ',
 '       WHERE 1 = (SELECT 1 FROM GlassfishDB.MessageProperty mp ',
 '                   WHERE mp.MessageID = f.MessageID ',
 '                     AND mp.MessagePropertyName = ''Message.SNDPRN'' AND mp.MessagePropertyValue = ', @w, ' LIMIT 1) ',
 '       ORDER BY f.MessageLastUpdate DESC, f.MessageID DESC LIMIT 51) AS treffer ',
 'LEFT JOIN GlassfishDB.Process p2 ON p2.ProcessID = treffer.ProcessID ',
 'LEFT JOIN GlassfishDB.Project prj ON prj.ProjectID = p2.ProjectID');
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
SET @a = CONCAT('ANALYZE ', @q);
PREPARE ap FROM @a; EXECUTE ap; DEALLOCATE PREPARE ap;
SELECT VARIABLE_VALUE INTO @h1 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY';
SELECT VARIABLE_VALUE INTO @h2 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT';
SELECT VARIABLE_VALUE INTO @h3 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT';
SELECT VARIABLE_VALUE INTO @h4 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE';
SELECT VARIABLE_VALUE INTO @h5 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS';
SELECT VARIABLE_VALUE INTO @h6 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND';
PREPARE qp FROM @q;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d1;
SELECT (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY') - @h1 AS readKey,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT') - @h2 AS readNext,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT') - @h3 AS readRndNext,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE') - @h4 AS tmpWrite,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS') - @h5 AS icpAttempts,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND') - @h6 AS readRnd;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d2;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d3;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d4;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d5;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d6;
DEALLOCATE PREPARE qp;
SELECT ROUND(LEAST(@d2, @d3, @d4, @d5, @d6) / 1000, 3) AS besteWandMsLauf2bis6,
       ROUND(GREATEST(@d2, @d3, @d4, @d5, @d6) / 1000, 3) AS schlechtesteWandMs,
       ROUND(@d1 / 1000, 3) AS aufwaermlaufWandMs;
SELECT ROUND(@d2 / 1000, 3) AS l2, ROUND(@d3 / 1000, 3) AS l3, ROUND(@d4 / 1000, 3) AS l4,
       ROUND(@d5 / 1000, 3) AS l5, ROUND(@d6 / 1000, 3) AS l6;
SELECT COUNT(*) AS profilLaeufe, ROUND(MIN(ms), 3) AS besteProfilMs, ROUND(MAX(ms), 3) AS schlechtesteProfilMs
  FROM (SELECT QUERY_ID, SUM(DURATION) * 1000 AS ms FROM information_schema.PROFILING
         WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 16 AND (QUERY_ID - @basis) MOD 3 = 0
         GROUP BY QUERY_ID) l;

SELECT '=== M168 / NEXANS / Message.SNDPRN / 30T / B materialisiert ===' AS marke;
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
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
SET @a = CONCAT('ANALYZE ', @q);
PREPARE ap FROM @a; EXECUTE ap; DEALLOCATE PREPARE ap;
SELECT VARIABLE_VALUE INTO @h1 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY';
SELECT VARIABLE_VALUE INTO @h2 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT';
SELECT VARIABLE_VALUE INTO @h3 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT';
SELECT VARIABLE_VALUE INTO @h4 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE';
SELECT VARIABLE_VALUE INTO @h5 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS';
SELECT VARIABLE_VALUE INTO @h6 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND';
PREPARE qp FROM @q;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d1;
SELECT (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY') - @h1 AS readKey,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT') - @h2 AS readNext,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT') - @h3 AS readRndNext,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE') - @h4 AS tmpWrite,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS') - @h5 AS icpAttempts,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND') - @h6 AS readRnd;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d2;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d3;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d4;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d5;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d6;
DEALLOCATE PREPARE qp;
SELECT ROUND(LEAST(@d2, @d3, @d4, @d5, @d6) / 1000, 3) AS besteWandMsLauf2bis6,
       ROUND(GREATEST(@d2, @d3, @d4, @d5, @d6) / 1000, 3) AS schlechtesteWandMs,
       ROUND(@d1 / 1000, 3) AS aufwaermlaufWandMs;
SELECT ROUND(@d2 / 1000, 3) AS l2, ROUND(@d3 / 1000, 3) AS l3, ROUND(@d4 / 1000, 3) AS l4,
       ROUND(@d5 / 1000, 3) AS l5, ROUND(@d6 / 1000, 3) AS l6;
SELECT COUNT(*) AS profilLaeufe, ROUND(MIN(ms), 3) AS besteProfilMs, ROUND(MAX(ms), 3) AS schlechtesteProfilMs
  FROM (SELECT QUERY_ID, SUM(DURATION) * 1000 AS ms FROM information_schema.PROFILING
         WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 16 AND (QUERY_ID - @basis) MOD 3 = 0
         GROUP BY QUERY_ID) l;

SELECT '=== M168 / NEXANS / Message.SNDPRN / 30T / C bindend ===' AS marke;
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
 '       WHERE 1 = (SELECT 1 FROM GlassfishDB.MessageProperty mp ',
 '                   WHERE mp.MessageID = f.MessageID ',
 '                     AND mp.MessagePropertyName = ''Message.SNDPRN'' AND mp.MessagePropertyValue = ', @w, ' LIMIT 1) ',
 '       ORDER BY f.MessageLastUpdate DESC, f.MessageID DESC LIMIT 51) AS treffer ',
 'LEFT JOIN GlassfishDB.Process p2 ON p2.ProcessID = treffer.ProcessID ',
 'LEFT JOIN GlassfishDB.Project prj ON prj.ProjectID = p2.ProjectID');
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
SET @a = CONCAT('ANALYZE ', @q);
PREPARE ap FROM @a; EXECUTE ap; DEALLOCATE PREPARE ap;
SELECT VARIABLE_VALUE INTO @h1 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY';
SELECT VARIABLE_VALUE INTO @h2 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT';
SELECT VARIABLE_VALUE INTO @h3 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT';
SELECT VARIABLE_VALUE INTO @h4 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE';
SELECT VARIABLE_VALUE INTO @h5 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS';
SELECT VARIABLE_VALUE INTO @h6 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND';
PREPARE qp FROM @q;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d1;
SELECT (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY') - @h1 AS readKey,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT') - @h2 AS readNext,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT') - @h3 AS readRndNext,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE') - @h4 AS tmpWrite,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS') - @h5 AS icpAttempts,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND') - @h6 AS readRnd;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d2;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d3;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d4;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d5;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d6;
DEALLOCATE PREPARE qp;
SELECT ROUND(LEAST(@d2, @d3, @d4, @d5, @d6) / 1000, 3) AS besteWandMsLauf2bis6,
       ROUND(GREATEST(@d2, @d3, @d4, @d5, @d6) / 1000, 3) AS schlechtesteWandMs,
       ROUND(@d1 / 1000, 3) AS aufwaermlaufWandMs;
SELECT ROUND(@d2 / 1000, 3) AS l2, ROUND(@d3 / 1000, 3) AS l3, ROUND(@d4 / 1000, 3) AS l4,
       ROUND(@d5 / 1000, 3) AS l5, ROUND(@d6 / 1000, 3) AS l6;
SELECT COUNT(*) AS profilLaeufe, ROUND(MIN(ms), 3) AS besteProfilMs, ROUND(MAX(ms), 3) AS schlechtesteProfilMs
  FROM (SELECT QUERY_ID, SUM(DURATION) * 1000 AS ms FROM information_schema.PROFILING
         WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 16 AND (QUERY_ID - @basis) MOD 3 = 0
         GROUP BY QUERY_ID) l;

SELECT '=== M168 / NEXANS / Message.SNDPRN / 90T / B materialisiert ===' AS marke;
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
 '               WHERE m.MessageLastUpdate >= ''2025-10-01 00:00:00'' AND m.MessageLastUpdate < ''2025-12-30 00:00:00'' ',
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
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
SET @a = CONCAT('ANALYZE ', @q);
PREPARE ap FROM @a; EXECUTE ap; DEALLOCATE PREPARE ap;
SELECT VARIABLE_VALUE INTO @h1 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY';
SELECT VARIABLE_VALUE INTO @h2 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT';
SELECT VARIABLE_VALUE INTO @h3 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT';
SELECT VARIABLE_VALUE INTO @h4 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE';
SELECT VARIABLE_VALUE INTO @h5 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS';
SELECT VARIABLE_VALUE INTO @h6 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND';
PREPARE qp FROM @q;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d1;
SELECT (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY') - @h1 AS readKey,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT') - @h2 AS readNext,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT') - @h3 AS readRndNext,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE') - @h4 AS tmpWrite,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS') - @h5 AS icpAttempts,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND') - @h6 AS readRnd;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d2;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d3;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d4;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d5;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d6;
DEALLOCATE PREPARE qp;
SELECT ROUND(LEAST(@d2, @d3, @d4, @d5, @d6) / 1000, 3) AS besteWandMsLauf2bis6,
       ROUND(GREATEST(@d2, @d3, @d4, @d5, @d6) / 1000, 3) AS schlechtesteWandMs,
       ROUND(@d1 / 1000, 3) AS aufwaermlaufWandMs;
SELECT ROUND(@d2 / 1000, 3) AS l2, ROUND(@d3 / 1000, 3) AS l3, ROUND(@d4 / 1000, 3) AS l4,
       ROUND(@d5 / 1000, 3) AS l5, ROUND(@d6 / 1000, 3) AS l6;
SELECT COUNT(*) AS profilLaeufe, ROUND(MIN(ms), 3) AS besteProfilMs, ROUND(MAX(ms), 3) AS schlechtesteProfilMs
  FROM (SELECT QUERY_ID, SUM(DURATION) * 1000 AS ms FROM information_schema.PROFILING
         WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 16 AND (QUERY_ID - @basis) MOD 3 = 0
         GROUP BY QUERY_ID) l;

SELECT '=== M168 / NEXANS / Message.SNDPRN / 90T / C bindend ===' AS marke;
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
 '               WHERE m.MessageLastUpdate >= ''2025-10-01 00:00:00'' AND m.MessageLastUpdate < ''2025-12-30 00:00:00'' ',
 '                 AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr ',
 '                               JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID ',
 '                              WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = ''NEXANS'') ',
 '               ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC LIMIT 1000000000) AS f ',
 '       WHERE 1 = (SELECT 1 FROM GlassfishDB.MessageProperty mp ',
 '                   WHERE mp.MessageID = f.MessageID ',
 '                     AND mp.MessagePropertyName = ''Message.SNDPRN'' AND mp.MessagePropertyValue = ', @w, ' LIMIT 1) ',
 '       ORDER BY f.MessageLastUpdate DESC, f.MessageID DESC LIMIT 51) AS treffer ',
 'LEFT JOIN GlassfishDB.Process p2 ON p2.ProcessID = treffer.ProcessID ',
 'LEFT JOIN GlassfishDB.Project prj ON prj.ProjectID = p2.ProjectID');
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
SET @a = CONCAT('ANALYZE ', @q);
PREPARE ap FROM @a; EXECUTE ap; DEALLOCATE PREPARE ap;
SELECT VARIABLE_VALUE INTO @h1 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY';
SELECT VARIABLE_VALUE INTO @h2 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT';
SELECT VARIABLE_VALUE INTO @h3 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT';
SELECT VARIABLE_VALUE INTO @h4 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE';
SELECT VARIABLE_VALUE INTO @h5 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS';
SELECT VARIABLE_VALUE INTO @h6 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND';
PREPARE qp FROM @q;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d1;
SELECT (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY') - @h1 AS readKey,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT') - @h2 AS readNext,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT') - @h3 AS readRndNext,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE') - @h4 AS tmpWrite,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS') - @h5 AS icpAttempts,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND') - @h6 AS readRnd;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d2;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d3;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d4;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d5;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d6;
DEALLOCATE PREPARE qp;
SELECT ROUND(LEAST(@d2, @d3, @d4, @d5, @d6) / 1000, 3) AS besteWandMsLauf2bis6,
       ROUND(GREATEST(@d2, @d3, @d4, @d5, @d6) / 1000, 3) AS schlechtesteWandMs,
       ROUND(@d1 / 1000, 3) AS aufwaermlaufWandMs;
SELECT ROUND(@d2 / 1000, 3) AS l2, ROUND(@d3 / 1000, 3) AS l3, ROUND(@d4 / 1000, 3) AS l4,
       ROUND(@d5 / 1000, 3) AS l5, ROUND(@d6 / 1000, 3) AS l6;
SELECT COUNT(*) AS profilLaeufe, ROUND(MIN(ms), 3) AS besteProfilMs, ROUND(MAX(ms), 3) AS schlechtesteProfilMs
  FROM (SELECT QUERY_ID, SUM(DURATION) * 1000 AS ms FROM information_schema.PROFILING
         WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 16 AND (QUERY_ID - @basis) MOD 3 = 0
         GROUP BY QUERY_ID) l;

SELECT '=== M168 / NEXANS / Message.VFN / 24h / B materialisiert ===' AS marke;
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
SET @q = CONCAT(
 'SELECT COUNT(*) AS zeilen, MAX(CHAR_LENGTH(p2.ProcessName)) AS maxPnLaenge, ',
 '       MAX(CHAR_LENGTH(prj.ProjectName)) AS maxPjLaenge ',
 'FROM (SELECT f.MessageID, f.MessageLastUpdate, f.MessageStatus, f.ProcessID ',
 '        FROM (SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID ',
 '                FROM GlassfishDB.Message m ',
 '               WHERE m.MessageLastUpdate >= ''2025-12-29 00:00:00'' AND m.MessageLastUpdate < ''2025-12-30 00:00:00'' ',
 '                 AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr ',
 '                               JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID ',
 '                              WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = ''NEXANS'') ',
 '               ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC LIMIT 1000000000) AS f ',
 '        JOIN GlassfishDB.MessageProperty mp ',
 '          ON mp.MessageID = f.MessageID ',
 '         AND mp.MessagePropertyName = ''Message.VFN'' AND mp.MessagePropertyValue = ', @w, ' ',
 '       GROUP BY f.MessageID ',
 '       ORDER BY f.MessageLastUpdate DESC, f.MessageID DESC LIMIT 51) AS treffer ',
 'LEFT JOIN GlassfishDB.Process p2 ON p2.ProcessID = treffer.ProcessID ',
 'LEFT JOIN GlassfishDB.Project prj ON prj.ProjectID = p2.ProjectID');
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
SET @a = CONCAT('ANALYZE ', @q);
PREPARE ap FROM @a; EXECUTE ap; DEALLOCATE PREPARE ap;
SELECT VARIABLE_VALUE INTO @h1 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY';
SELECT VARIABLE_VALUE INTO @h2 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT';
SELECT VARIABLE_VALUE INTO @h3 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT';
SELECT VARIABLE_VALUE INTO @h4 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE';
SELECT VARIABLE_VALUE INTO @h5 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS';
SELECT VARIABLE_VALUE INTO @h6 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND';
PREPARE qp FROM @q;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d1;
SELECT (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY') - @h1 AS readKey,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT') - @h2 AS readNext,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT') - @h3 AS readRndNext,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE') - @h4 AS tmpWrite,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS') - @h5 AS icpAttempts,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND') - @h6 AS readRnd;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d2;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d3;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d4;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d5;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d6;
DEALLOCATE PREPARE qp;
SELECT ROUND(LEAST(@d2, @d3, @d4, @d5, @d6) / 1000, 3) AS besteWandMsLauf2bis6,
       ROUND(GREATEST(@d2, @d3, @d4, @d5, @d6) / 1000, 3) AS schlechtesteWandMs,
       ROUND(@d1 / 1000, 3) AS aufwaermlaufWandMs;
SELECT ROUND(@d2 / 1000, 3) AS l2, ROUND(@d3 / 1000, 3) AS l3, ROUND(@d4 / 1000, 3) AS l4,
       ROUND(@d5 / 1000, 3) AS l5, ROUND(@d6 / 1000, 3) AS l6;
SELECT COUNT(*) AS profilLaeufe, ROUND(MIN(ms), 3) AS besteProfilMs, ROUND(MAX(ms), 3) AS schlechtesteProfilMs
  FROM (SELECT QUERY_ID, SUM(DURATION) * 1000 AS ms FROM information_schema.PROFILING
         WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 16 AND (QUERY_ID - @basis) MOD 3 = 0
         GROUP BY QUERY_ID) l;

SELECT '=== M168 / NEXANS / Message.VFN / 24h / C bindend ===' AS marke;
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
SET @q = CONCAT(
 'SELECT COUNT(*) AS zeilen, MAX(CHAR_LENGTH(p2.ProcessName)) AS maxPnLaenge, ',
 '       MAX(CHAR_LENGTH(prj.ProjectName)) AS maxPjLaenge ',
 'FROM (SELECT f.MessageID, f.MessageLastUpdate, f.MessageStatus, f.ProcessID ',
 '        FROM (SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID ',
 '                FROM GlassfishDB.Message m ',
 '               WHERE m.MessageLastUpdate >= ''2025-12-29 00:00:00'' AND m.MessageLastUpdate < ''2025-12-30 00:00:00'' ',
 '                 AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr ',
 '                               JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID ',
 '                              WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = ''NEXANS'') ',
 '               ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC LIMIT 1000000000) AS f ',
 '       WHERE 1 = (SELECT 1 FROM GlassfishDB.MessageProperty mp ',
 '                   WHERE mp.MessageID = f.MessageID ',
 '                     AND mp.MessagePropertyName = ''Message.VFN'' AND mp.MessagePropertyValue = ', @w, ' LIMIT 1) ',
 '       ORDER BY f.MessageLastUpdate DESC, f.MessageID DESC LIMIT 51) AS treffer ',
 'LEFT JOIN GlassfishDB.Process p2 ON p2.ProcessID = treffer.ProcessID ',
 'LEFT JOIN GlassfishDB.Project prj ON prj.ProjectID = p2.ProjectID');
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
SET @a = CONCAT('ANALYZE ', @q);
PREPARE ap FROM @a; EXECUTE ap; DEALLOCATE PREPARE ap;
SELECT VARIABLE_VALUE INTO @h1 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY';
SELECT VARIABLE_VALUE INTO @h2 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT';
SELECT VARIABLE_VALUE INTO @h3 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT';
SELECT VARIABLE_VALUE INTO @h4 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE';
SELECT VARIABLE_VALUE INTO @h5 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS';
SELECT VARIABLE_VALUE INTO @h6 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND';
PREPARE qp FROM @q;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d1;
SELECT (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY') - @h1 AS readKey,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT') - @h2 AS readNext,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT') - @h3 AS readRndNext,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE') - @h4 AS tmpWrite,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS') - @h5 AS icpAttempts,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND') - @h6 AS readRnd;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d2;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d3;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d4;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d5;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d6;
DEALLOCATE PREPARE qp;
SELECT ROUND(LEAST(@d2, @d3, @d4, @d5, @d6) / 1000, 3) AS besteWandMsLauf2bis6,
       ROUND(GREATEST(@d2, @d3, @d4, @d5, @d6) / 1000, 3) AS schlechtesteWandMs,
       ROUND(@d1 / 1000, 3) AS aufwaermlaufWandMs;
SELECT ROUND(@d2 / 1000, 3) AS l2, ROUND(@d3 / 1000, 3) AS l3, ROUND(@d4 / 1000, 3) AS l4,
       ROUND(@d5 / 1000, 3) AS l5, ROUND(@d6 / 1000, 3) AS l6;
SELECT COUNT(*) AS profilLaeufe, ROUND(MIN(ms), 3) AS besteProfilMs, ROUND(MAX(ms), 3) AS schlechtesteProfilMs
  FROM (SELECT QUERY_ID, SUM(DURATION) * 1000 AS ms FROM information_schema.PROFILING
         WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 16 AND (QUERY_ID - @basis) MOD 3 = 0
         GROUP BY QUERY_ID) l;

SELECT '=== M168 / NEXANS / Message.VFN / 30T / B materialisiert ===' AS marke;
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
 '         AND mp.MessagePropertyName = ''Message.VFN'' AND mp.MessagePropertyValue = ', @w, ' ',
 '       GROUP BY f.MessageID ',
 '       ORDER BY f.MessageLastUpdate DESC, f.MessageID DESC LIMIT 51) AS treffer ',
 'LEFT JOIN GlassfishDB.Process p2 ON p2.ProcessID = treffer.ProcessID ',
 'LEFT JOIN GlassfishDB.Project prj ON prj.ProjectID = p2.ProjectID');
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
SET @a = CONCAT('ANALYZE ', @q);
PREPARE ap FROM @a; EXECUTE ap; DEALLOCATE PREPARE ap;
SELECT VARIABLE_VALUE INTO @h1 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY';
SELECT VARIABLE_VALUE INTO @h2 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT';
SELECT VARIABLE_VALUE INTO @h3 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT';
SELECT VARIABLE_VALUE INTO @h4 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE';
SELECT VARIABLE_VALUE INTO @h5 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS';
SELECT VARIABLE_VALUE INTO @h6 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND';
PREPARE qp FROM @q;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d1;
SELECT (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY') - @h1 AS readKey,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT') - @h2 AS readNext,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT') - @h3 AS readRndNext,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE') - @h4 AS tmpWrite,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS') - @h5 AS icpAttempts,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND') - @h6 AS readRnd;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d2;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d3;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d4;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d5;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d6;
DEALLOCATE PREPARE qp;
SELECT ROUND(LEAST(@d2, @d3, @d4, @d5, @d6) / 1000, 3) AS besteWandMsLauf2bis6,
       ROUND(GREATEST(@d2, @d3, @d4, @d5, @d6) / 1000, 3) AS schlechtesteWandMs,
       ROUND(@d1 / 1000, 3) AS aufwaermlaufWandMs;
SELECT ROUND(@d2 / 1000, 3) AS l2, ROUND(@d3 / 1000, 3) AS l3, ROUND(@d4 / 1000, 3) AS l4,
       ROUND(@d5 / 1000, 3) AS l5, ROUND(@d6 / 1000, 3) AS l6;
SELECT COUNT(*) AS profilLaeufe, ROUND(MIN(ms), 3) AS besteProfilMs, ROUND(MAX(ms), 3) AS schlechtesteProfilMs
  FROM (SELECT QUERY_ID, SUM(DURATION) * 1000 AS ms FROM information_schema.PROFILING
         WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 16 AND (QUERY_ID - @basis) MOD 3 = 0
         GROUP BY QUERY_ID) l;

SELECT '=== M168 / NEXANS / Message.VFN / 30T / C bindend ===' AS marke;
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
 '       WHERE 1 = (SELECT 1 FROM GlassfishDB.MessageProperty mp ',
 '                   WHERE mp.MessageID = f.MessageID ',
 '                     AND mp.MessagePropertyName = ''Message.VFN'' AND mp.MessagePropertyValue = ', @w, ' LIMIT 1) ',
 '       ORDER BY f.MessageLastUpdate DESC, f.MessageID DESC LIMIT 51) AS treffer ',
 'LEFT JOIN GlassfishDB.Process p2 ON p2.ProcessID = treffer.ProcessID ',
 'LEFT JOIN GlassfishDB.Project prj ON prj.ProjectID = p2.ProjectID');
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
SET @a = CONCAT('ANALYZE ', @q);
PREPARE ap FROM @a; EXECUTE ap; DEALLOCATE PREPARE ap;
SELECT VARIABLE_VALUE INTO @h1 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY';
SELECT VARIABLE_VALUE INTO @h2 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT';
SELECT VARIABLE_VALUE INTO @h3 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT';
SELECT VARIABLE_VALUE INTO @h4 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE';
SELECT VARIABLE_VALUE INTO @h5 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS';
SELECT VARIABLE_VALUE INTO @h6 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND';
PREPARE qp FROM @q;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d1;
SELECT (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY') - @h1 AS readKey,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT') - @h2 AS readNext,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT') - @h3 AS readRndNext,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE') - @h4 AS tmpWrite,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS') - @h5 AS icpAttempts,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND') - @h6 AS readRnd;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d2;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d3;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d4;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d5;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d6;
DEALLOCATE PREPARE qp;
SELECT ROUND(LEAST(@d2, @d3, @d4, @d5, @d6) / 1000, 3) AS besteWandMsLauf2bis6,
       ROUND(GREATEST(@d2, @d3, @d4, @d5, @d6) / 1000, 3) AS schlechtesteWandMs,
       ROUND(@d1 / 1000, 3) AS aufwaermlaufWandMs;
SELECT ROUND(@d2 / 1000, 3) AS l2, ROUND(@d3 / 1000, 3) AS l3, ROUND(@d4 / 1000, 3) AS l4,
       ROUND(@d5 / 1000, 3) AS l5, ROUND(@d6 / 1000, 3) AS l6;
SELECT COUNT(*) AS profilLaeufe, ROUND(MIN(ms), 3) AS besteProfilMs, ROUND(MAX(ms), 3) AS schlechtesteProfilMs
  FROM (SELECT QUERY_ID, SUM(DURATION) * 1000 AS ms FROM information_schema.PROFILING
         WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 16 AND (QUERY_ID - @basis) MOD 3 = 0
         GROUP BY QUERY_ID) l;

SELECT '=== M168 / NEXANS / Message.VFN / 90T / B materialisiert ===' AS marke;
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
SET @q = CONCAT(
 'SELECT COUNT(*) AS zeilen, MAX(CHAR_LENGTH(p2.ProcessName)) AS maxPnLaenge, ',
 '       MAX(CHAR_LENGTH(prj.ProjectName)) AS maxPjLaenge ',
 'FROM (SELECT f.MessageID, f.MessageLastUpdate, f.MessageStatus, f.ProcessID ',
 '        FROM (SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID ',
 '                FROM GlassfishDB.Message m ',
 '               WHERE m.MessageLastUpdate >= ''2025-10-01 00:00:00'' AND m.MessageLastUpdate < ''2025-12-30 00:00:00'' ',
 '                 AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr ',
 '                               JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID ',
 '                              WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = ''NEXANS'') ',
 '               ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC LIMIT 1000000000) AS f ',
 '        JOIN GlassfishDB.MessageProperty mp ',
 '          ON mp.MessageID = f.MessageID ',
 '         AND mp.MessagePropertyName = ''Message.VFN'' AND mp.MessagePropertyValue = ', @w, ' ',
 '       GROUP BY f.MessageID ',
 '       ORDER BY f.MessageLastUpdate DESC, f.MessageID DESC LIMIT 51) AS treffer ',
 'LEFT JOIN GlassfishDB.Process p2 ON p2.ProcessID = treffer.ProcessID ',
 'LEFT JOIN GlassfishDB.Project prj ON prj.ProjectID = p2.ProjectID');
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
SET @a = CONCAT('ANALYZE ', @q);
PREPARE ap FROM @a; EXECUTE ap; DEALLOCATE PREPARE ap;
SELECT VARIABLE_VALUE INTO @h1 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY';
SELECT VARIABLE_VALUE INTO @h2 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT';
SELECT VARIABLE_VALUE INTO @h3 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT';
SELECT VARIABLE_VALUE INTO @h4 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE';
SELECT VARIABLE_VALUE INTO @h5 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS';
SELECT VARIABLE_VALUE INTO @h6 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND';
PREPARE qp FROM @q;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d1;
SELECT (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY') - @h1 AS readKey,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT') - @h2 AS readNext,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT') - @h3 AS readRndNext,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE') - @h4 AS tmpWrite,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS') - @h5 AS icpAttempts,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND') - @h6 AS readRnd;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d2;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d3;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d4;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d5;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d6;
DEALLOCATE PREPARE qp;
SELECT ROUND(LEAST(@d2, @d3, @d4, @d5, @d6) / 1000, 3) AS besteWandMsLauf2bis6,
       ROUND(GREATEST(@d2, @d3, @d4, @d5, @d6) / 1000, 3) AS schlechtesteWandMs,
       ROUND(@d1 / 1000, 3) AS aufwaermlaufWandMs;
SELECT ROUND(@d2 / 1000, 3) AS l2, ROUND(@d3 / 1000, 3) AS l3, ROUND(@d4 / 1000, 3) AS l4,
       ROUND(@d5 / 1000, 3) AS l5, ROUND(@d6 / 1000, 3) AS l6;
SELECT COUNT(*) AS profilLaeufe, ROUND(MIN(ms), 3) AS besteProfilMs, ROUND(MAX(ms), 3) AS schlechtesteProfilMs
  FROM (SELECT QUERY_ID, SUM(DURATION) * 1000 AS ms FROM information_schema.PROFILING
         WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 16 AND (QUERY_ID - @basis) MOD 3 = 0
         GROUP BY QUERY_ID) l;

SELECT '=== M168 / NEXANS / Message.VFN / 90T / C bindend ===' AS marke;
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
SET @q = CONCAT(
 'SELECT COUNT(*) AS zeilen, MAX(CHAR_LENGTH(p2.ProcessName)) AS maxPnLaenge, ',
 '       MAX(CHAR_LENGTH(prj.ProjectName)) AS maxPjLaenge ',
 'FROM (SELECT f.MessageID, f.MessageLastUpdate, f.MessageStatus, f.ProcessID ',
 '        FROM (SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID ',
 '                FROM GlassfishDB.Message m ',
 '               WHERE m.MessageLastUpdate >= ''2025-10-01 00:00:00'' AND m.MessageLastUpdate < ''2025-12-30 00:00:00'' ',
 '                 AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr ',
 '                               JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID ',
 '                              WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = ''NEXANS'') ',
 '               ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC LIMIT 1000000000) AS f ',
 '       WHERE 1 = (SELECT 1 FROM GlassfishDB.MessageProperty mp ',
 '                   WHERE mp.MessageID = f.MessageID ',
 '                     AND mp.MessagePropertyName = ''Message.VFN'' AND mp.MessagePropertyValue = ', @w, ' LIMIT 1) ',
 '       ORDER BY f.MessageLastUpdate DESC, f.MessageID DESC LIMIT 51) AS treffer ',
 'LEFT JOIN GlassfishDB.Process p2 ON p2.ProcessID = treffer.ProcessID ',
 'LEFT JOIN GlassfishDB.Project prj ON prj.ProjectID = p2.ProjectID');
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
SET @a = CONCAT('ANALYZE ', @q);
PREPARE ap FROM @a; EXECUTE ap; DEALLOCATE PREPARE ap;
SELECT VARIABLE_VALUE INTO @h1 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY';
SELECT VARIABLE_VALUE INTO @h2 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT';
SELECT VARIABLE_VALUE INTO @h3 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT';
SELECT VARIABLE_VALUE INTO @h4 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE';
SELECT VARIABLE_VALUE INTO @h5 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS';
SELECT VARIABLE_VALUE INTO @h6 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND';
PREPARE qp FROM @q;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d1;
SELECT (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY') - @h1 AS readKey,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT') - @h2 AS readNext,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT') - @h3 AS readRndNext,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE') - @h4 AS tmpWrite,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS') - @h5 AS icpAttempts,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND') - @h6 AS readRnd;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d2;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d3;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d4;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d5;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d6;
DEALLOCATE PREPARE qp;
SELECT ROUND(LEAST(@d2, @d3, @d4, @d5, @d6) / 1000, 3) AS besteWandMsLauf2bis6,
       ROUND(GREATEST(@d2, @d3, @d4, @d5, @d6) / 1000, 3) AS schlechtesteWandMs,
       ROUND(@d1 / 1000, 3) AS aufwaermlaufWandMs;
SELECT ROUND(@d2 / 1000, 3) AS l2, ROUND(@d3 / 1000, 3) AS l3, ROUND(@d4 / 1000, 3) AS l4,
       ROUND(@d5 / 1000, 3) AS l5, ROUND(@d6 / 1000, 3) AS l6;
SELECT COUNT(*) AS profilLaeufe, ROUND(MIN(ms), 3) AS besteProfilMs, ROUND(MAX(ms), 3) AS schlechtesteProfilMs
  FROM (SELECT QUERY_ID, SUM(DURATION) * 1000 AS ms FROM information_schema.PROFILING
         WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 16 AND (QUERY_ID - @basis) MOD 3 = 0
         GROUP BY QUERY_ID) l;

SELECT '=== M168 / NEXANS / Message.DestinationFilename / 24h / B materialisiert ===' AS marke;
SELECT QUOTE(v) INTO @w FROM (
  SELECT p.MessagePropertyValue AS v, COUNT(*) AS c
    FROM GlassfishDB.Message m
    JOIN GlassfishDB.MessageProperty p
      ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.DestinationFilename'
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
 '               WHERE m.MessageLastUpdate >= ''2025-12-29 00:00:00'' AND m.MessageLastUpdate < ''2025-12-30 00:00:00'' ',
 '                 AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr ',
 '                               JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID ',
 '                              WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = ''NEXANS'') ',
 '               ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC LIMIT 1000000000) AS f ',
 '        JOIN GlassfishDB.MessageProperty mp ',
 '          ON mp.MessageID = f.MessageID ',
 '         AND mp.MessagePropertyName = ''Message.DestinationFilename'' AND mp.MessagePropertyValue = ', @w, ' ',
 '       GROUP BY f.MessageID ',
 '       ORDER BY f.MessageLastUpdate DESC, f.MessageID DESC LIMIT 51) AS treffer ',
 'LEFT JOIN GlassfishDB.Process p2 ON p2.ProcessID = treffer.ProcessID ',
 'LEFT JOIN GlassfishDB.Project prj ON prj.ProjectID = p2.ProjectID');
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
SET @a = CONCAT('ANALYZE ', @q);
PREPARE ap FROM @a; EXECUTE ap; DEALLOCATE PREPARE ap;
SELECT VARIABLE_VALUE INTO @h1 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY';
SELECT VARIABLE_VALUE INTO @h2 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT';
SELECT VARIABLE_VALUE INTO @h3 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT';
SELECT VARIABLE_VALUE INTO @h4 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE';
SELECT VARIABLE_VALUE INTO @h5 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS';
SELECT VARIABLE_VALUE INTO @h6 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND';
PREPARE qp FROM @q;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d1;
SELECT (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY') - @h1 AS readKey,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT') - @h2 AS readNext,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT') - @h3 AS readRndNext,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE') - @h4 AS tmpWrite,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS') - @h5 AS icpAttempts,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND') - @h6 AS readRnd;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d2;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d3;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d4;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d5;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d6;
DEALLOCATE PREPARE qp;
SELECT ROUND(LEAST(@d2, @d3, @d4, @d5, @d6) / 1000, 3) AS besteWandMsLauf2bis6,
       ROUND(GREATEST(@d2, @d3, @d4, @d5, @d6) / 1000, 3) AS schlechtesteWandMs,
       ROUND(@d1 / 1000, 3) AS aufwaermlaufWandMs;
SELECT ROUND(@d2 / 1000, 3) AS l2, ROUND(@d3 / 1000, 3) AS l3, ROUND(@d4 / 1000, 3) AS l4,
       ROUND(@d5 / 1000, 3) AS l5, ROUND(@d6 / 1000, 3) AS l6;
SELECT COUNT(*) AS profilLaeufe, ROUND(MIN(ms), 3) AS besteProfilMs, ROUND(MAX(ms), 3) AS schlechtesteProfilMs
  FROM (SELECT QUERY_ID, SUM(DURATION) * 1000 AS ms FROM information_schema.PROFILING
         WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 16 AND (QUERY_ID - @basis) MOD 3 = 0
         GROUP BY QUERY_ID) l;

SELECT '=== M168 / NEXANS / Message.DestinationFilename / 24h / C bindend ===' AS marke;
SELECT QUOTE(v) INTO @w FROM (
  SELECT p.MessagePropertyValue AS v, COUNT(*) AS c
    FROM GlassfishDB.Message m
    JOIN GlassfishDB.MessageProperty p
      ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.DestinationFilename'
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
 '               WHERE m.MessageLastUpdate >= ''2025-12-29 00:00:00'' AND m.MessageLastUpdate < ''2025-12-30 00:00:00'' ',
 '                 AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr ',
 '                               JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID ',
 '                              WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = ''NEXANS'') ',
 '               ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC LIMIT 1000000000) AS f ',
 '       WHERE 1 = (SELECT 1 FROM GlassfishDB.MessageProperty mp ',
 '                   WHERE mp.MessageID = f.MessageID ',
 '                     AND mp.MessagePropertyName = ''Message.DestinationFilename'' AND mp.MessagePropertyValue = ', @w, ' LIMIT 1) ',
 '       ORDER BY f.MessageLastUpdate DESC, f.MessageID DESC LIMIT 51) AS treffer ',
 'LEFT JOIN GlassfishDB.Process p2 ON p2.ProcessID = treffer.ProcessID ',
 'LEFT JOIN GlassfishDB.Project prj ON prj.ProjectID = p2.ProjectID');
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
SET @a = CONCAT('ANALYZE ', @q);
PREPARE ap FROM @a; EXECUTE ap; DEALLOCATE PREPARE ap;
SELECT VARIABLE_VALUE INTO @h1 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY';
SELECT VARIABLE_VALUE INTO @h2 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT';
SELECT VARIABLE_VALUE INTO @h3 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT';
SELECT VARIABLE_VALUE INTO @h4 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE';
SELECT VARIABLE_VALUE INTO @h5 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS';
SELECT VARIABLE_VALUE INTO @h6 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND';
PREPARE qp FROM @q;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d1;
SELECT (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY') - @h1 AS readKey,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT') - @h2 AS readNext,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT') - @h3 AS readRndNext,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE') - @h4 AS tmpWrite,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS') - @h5 AS icpAttempts,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND') - @h6 AS readRnd;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d2;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d3;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d4;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d5;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d6;
DEALLOCATE PREPARE qp;
SELECT ROUND(LEAST(@d2, @d3, @d4, @d5, @d6) / 1000, 3) AS besteWandMsLauf2bis6,
       ROUND(GREATEST(@d2, @d3, @d4, @d5, @d6) / 1000, 3) AS schlechtesteWandMs,
       ROUND(@d1 / 1000, 3) AS aufwaermlaufWandMs;
SELECT ROUND(@d2 / 1000, 3) AS l2, ROUND(@d3 / 1000, 3) AS l3, ROUND(@d4 / 1000, 3) AS l4,
       ROUND(@d5 / 1000, 3) AS l5, ROUND(@d6 / 1000, 3) AS l6;
SELECT COUNT(*) AS profilLaeufe, ROUND(MIN(ms), 3) AS besteProfilMs, ROUND(MAX(ms), 3) AS schlechtesteProfilMs
  FROM (SELECT QUERY_ID, SUM(DURATION) * 1000 AS ms FROM information_schema.PROFILING
         WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 16 AND (QUERY_ID - @basis) MOD 3 = 0
         GROUP BY QUERY_ID) l;

SELECT '=== M168 / NEXANS / Message.DestinationFilename / 30T / B materialisiert ===' AS marke;
SELECT QUOTE(v) INTO @w FROM (
  SELECT p.MessagePropertyValue AS v, COUNT(*) AS c
    FROM GlassfishDB.Message m
    JOIN GlassfishDB.MessageProperty p
      ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.DestinationFilename'
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
 '         AND mp.MessagePropertyName = ''Message.DestinationFilename'' AND mp.MessagePropertyValue = ', @w, ' ',
 '       GROUP BY f.MessageID ',
 '       ORDER BY f.MessageLastUpdate DESC, f.MessageID DESC LIMIT 51) AS treffer ',
 'LEFT JOIN GlassfishDB.Process p2 ON p2.ProcessID = treffer.ProcessID ',
 'LEFT JOIN GlassfishDB.Project prj ON prj.ProjectID = p2.ProjectID');
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
SET @a = CONCAT('ANALYZE ', @q);
PREPARE ap FROM @a; EXECUTE ap; DEALLOCATE PREPARE ap;
SELECT VARIABLE_VALUE INTO @h1 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY';
SELECT VARIABLE_VALUE INTO @h2 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT';
SELECT VARIABLE_VALUE INTO @h3 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT';
SELECT VARIABLE_VALUE INTO @h4 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE';
SELECT VARIABLE_VALUE INTO @h5 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS';
SELECT VARIABLE_VALUE INTO @h6 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND';
PREPARE qp FROM @q;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d1;
SELECT (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY') - @h1 AS readKey,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT') - @h2 AS readNext,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT') - @h3 AS readRndNext,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE') - @h4 AS tmpWrite,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS') - @h5 AS icpAttempts,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND') - @h6 AS readRnd;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d2;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d3;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d4;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d5;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d6;
DEALLOCATE PREPARE qp;
SELECT ROUND(LEAST(@d2, @d3, @d4, @d5, @d6) / 1000, 3) AS besteWandMsLauf2bis6,
       ROUND(GREATEST(@d2, @d3, @d4, @d5, @d6) / 1000, 3) AS schlechtesteWandMs,
       ROUND(@d1 / 1000, 3) AS aufwaermlaufWandMs;
SELECT ROUND(@d2 / 1000, 3) AS l2, ROUND(@d3 / 1000, 3) AS l3, ROUND(@d4 / 1000, 3) AS l4,
       ROUND(@d5 / 1000, 3) AS l5, ROUND(@d6 / 1000, 3) AS l6;
SELECT COUNT(*) AS profilLaeufe, ROUND(MIN(ms), 3) AS besteProfilMs, ROUND(MAX(ms), 3) AS schlechtesteProfilMs
  FROM (SELECT QUERY_ID, SUM(DURATION) * 1000 AS ms FROM information_schema.PROFILING
         WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 16 AND (QUERY_ID - @basis) MOD 3 = 0
         GROUP BY QUERY_ID) l;

SELECT '=== M168 / NEXANS / Message.DestinationFilename / 30T / C bindend ===' AS marke;
SELECT QUOTE(v) INTO @w FROM (
  SELECT p.MessagePropertyValue AS v, COUNT(*) AS c
    FROM GlassfishDB.Message m
    JOIN GlassfishDB.MessageProperty p
      ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.DestinationFilename'
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
 '       WHERE 1 = (SELECT 1 FROM GlassfishDB.MessageProperty mp ',
 '                   WHERE mp.MessageID = f.MessageID ',
 '                     AND mp.MessagePropertyName = ''Message.DestinationFilename'' AND mp.MessagePropertyValue = ', @w, ' LIMIT 1) ',
 '       ORDER BY f.MessageLastUpdate DESC, f.MessageID DESC LIMIT 51) AS treffer ',
 'LEFT JOIN GlassfishDB.Process p2 ON p2.ProcessID = treffer.ProcessID ',
 'LEFT JOIN GlassfishDB.Project prj ON prj.ProjectID = p2.ProjectID');
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
SET @a = CONCAT('ANALYZE ', @q);
PREPARE ap FROM @a; EXECUTE ap; DEALLOCATE PREPARE ap;
SELECT VARIABLE_VALUE INTO @h1 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY';
SELECT VARIABLE_VALUE INTO @h2 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT';
SELECT VARIABLE_VALUE INTO @h3 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT';
SELECT VARIABLE_VALUE INTO @h4 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE';
SELECT VARIABLE_VALUE INTO @h5 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS';
SELECT VARIABLE_VALUE INTO @h6 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND';
PREPARE qp FROM @q;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d1;
SELECT (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY') - @h1 AS readKey,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT') - @h2 AS readNext,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT') - @h3 AS readRndNext,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE') - @h4 AS tmpWrite,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS') - @h5 AS icpAttempts,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND') - @h6 AS readRnd;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d2;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d3;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d4;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d5;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d6;
DEALLOCATE PREPARE qp;
SELECT ROUND(LEAST(@d2, @d3, @d4, @d5, @d6) / 1000, 3) AS besteWandMsLauf2bis6,
       ROUND(GREATEST(@d2, @d3, @d4, @d5, @d6) / 1000, 3) AS schlechtesteWandMs,
       ROUND(@d1 / 1000, 3) AS aufwaermlaufWandMs;
SELECT ROUND(@d2 / 1000, 3) AS l2, ROUND(@d3 / 1000, 3) AS l3, ROUND(@d4 / 1000, 3) AS l4,
       ROUND(@d5 / 1000, 3) AS l5, ROUND(@d6 / 1000, 3) AS l6;
SELECT COUNT(*) AS profilLaeufe, ROUND(MIN(ms), 3) AS besteProfilMs, ROUND(MAX(ms), 3) AS schlechtesteProfilMs
  FROM (SELECT QUERY_ID, SUM(DURATION) * 1000 AS ms FROM information_schema.PROFILING
         WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 16 AND (QUERY_ID - @basis) MOD 3 = 0
         GROUP BY QUERY_ID) l;

SELECT '=== M168 / NEXANS / Message.DestinationFilename / 90T / B materialisiert ===' AS marke;
SELECT QUOTE(v) INTO @w FROM (
  SELECT p.MessagePropertyValue AS v, COUNT(*) AS c
    FROM GlassfishDB.Message m
    JOIN GlassfishDB.MessageProperty p
      ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.DestinationFilename'
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
 '               WHERE m.MessageLastUpdate >= ''2025-10-01 00:00:00'' AND m.MessageLastUpdate < ''2025-12-30 00:00:00'' ',
 '                 AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr ',
 '                               JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID ',
 '                              WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = ''NEXANS'') ',
 '               ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC LIMIT 1000000000) AS f ',
 '        JOIN GlassfishDB.MessageProperty mp ',
 '          ON mp.MessageID = f.MessageID ',
 '         AND mp.MessagePropertyName = ''Message.DestinationFilename'' AND mp.MessagePropertyValue = ', @w, ' ',
 '       GROUP BY f.MessageID ',
 '       ORDER BY f.MessageLastUpdate DESC, f.MessageID DESC LIMIT 51) AS treffer ',
 'LEFT JOIN GlassfishDB.Process p2 ON p2.ProcessID = treffer.ProcessID ',
 'LEFT JOIN GlassfishDB.Project prj ON prj.ProjectID = p2.ProjectID');
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
SET @a = CONCAT('ANALYZE ', @q);
PREPARE ap FROM @a; EXECUTE ap; DEALLOCATE PREPARE ap;
SELECT VARIABLE_VALUE INTO @h1 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY';
SELECT VARIABLE_VALUE INTO @h2 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT';
SELECT VARIABLE_VALUE INTO @h3 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT';
SELECT VARIABLE_VALUE INTO @h4 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE';
SELECT VARIABLE_VALUE INTO @h5 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS';
SELECT VARIABLE_VALUE INTO @h6 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND';
PREPARE qp FROM @q;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d1;
SELECT (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY') - @h1 AS readKey,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT') - @h2 AS readNext,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT') - @h3 AS readRndNext,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE') - @h4 AS tmpWrite,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS') - @h5 AS icpAttempts,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND') - @h6 AS readRnd;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d2;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d3;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d4;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d5;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d6;
DEALLOCATE PREPARE qp;
SELECT ROUND(LEAST(@d2, @d3, @d4, @d5, @d6) / 1000, 3) AS besteWandMsLauf2bis6,
       ROUND(GREATEST(@d2, @d3, @d4, @d5, @d6) / 1000, 3) AS schlechtesteWandMs,
       ROUND(@d1 / 1000, 3) AS aufwaermlaufWandMs;
SELECT ROUND(@d2 / 1000, 3) AS l2, ROUND(@d3 / 1000, 3) AS l3, ROUND(@d4 / 1000, 3) AS l4,
       ROUND(@d5 / 1000, 3) AS l5, ROUND(@d6 / 1000, 3) AS l6;
SELECT COUNT(*) AS profilLaeufe, ROUND(MIN(ms), 3) AS besteProfilMs, ROUND(MAX(ms), 3) AS schlechtesteProfilMs
  FROM (SELECT QUERY_ID, SUM(DURATION) * 1000 AS ms FROM information_schema.PROFILING
         WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 16 AND (QUERY_ID - @basis) MOD 3 = 0
         GROUP BY QUERY_ID) l;

SELECT '=== M168 / NEXANS / Message.DestinationFilename / 90T / C bindend ===' AS marke;
SELECT QUOTE(v) INTO @w FROM (
  SELECT p.MessagePropertyValue AS v, COUNT(*) AS c
    FROM GlassfishDB.Message m
    JOIN GlassfishDB.MessageProperty p
      ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.DestinationFilename'
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
 '               WHERE m.MessageLastUpdate >= ''2025-10-01 00:00:00'' AND m.MessageLastUpdate < ''2025-12-30 00:00:00'' ',
 '                 AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr ',
 '                               JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID ',
 '                              WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = ''NEXANS'') ',
 '               ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC LIMIT 1000000000) AS f ',
 '       WHERE 1 = (SELECT 1 FROM GlassfishDB.MessageProperty mp ',
 '                   WHERE mp.MessageID = f.MessageID ',
 '                     AND mp.MessagePropertyName = ''Message.DestinationFilename'' AND mp.MessagePropertyValue = ', @w, ' LIMIT 1) ',
 '       ORDER BY f.MessageLastUpdate DESC, f.MessageID DESC LIMIT 51) AS treffer ',
 'LEFT JOIN GlassfishDB.Process p2 ON p2.ProcessID = treffer.ProcessID ',
 'LEFT JOIN GlassfishDB.Project prj ON prj.ProjectID = p2.ProjectID');
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
SET @a = CONCAT('ANALYZE ', @q);
PREPARE ap FROM @a; EXECUTE ap; DEALLOCATE PREPARE ap;
SELECT VARIABLE_VALUE INTO @h1 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY';
SELECT VARIABLE_VALUE INTO @h2 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT';
SELECT VARIABLE_VALUE INTO @h3 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT';
SELECT VARIABLE_VALUE INTO @h4 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE';
SELECT VARIABLE_VALUE INTO @h5 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS';
SELECT VARIABLE_VALUE INTO @h6 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND';
PREPARE qp FROM @q;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d1;
SELECT (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY') - @h1 AS readKey,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT') - @h2 AS readNext,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT') - @h3 AS readRndNext,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE') - @h4 AS tmpWrite,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS') - @h5 AS icpAttempts,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND') - @h6 AS readRnd;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d2;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d3;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d4;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d5;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d6;
DEALLOCATE PREPARE qp;
SELECT ROUND(LEAST(@d2, @d3, @d4, @d5, @d6) / 1000, 3) AS besteWandMsLauf2bis6,
       ROUND(GREATEST(@d2, @d3, @d4, @d5, @d6) / 1000, 3) AS schlechtesteWandMs,
       ROUND(@d1 / 1000, 3) AS aufwaermlaufWandMs;
SELECT ROUND(@d2 / 1000, 3) AS l2, ROUND(@d3 / 1000, 3) AS l3, ROUND(@d4 / 1000, 3) AS l4,
       ROUND(@d5 / 1000, 3) AS l5, ROUND(@d6 / 1000, 3) AS l6;
SELECT COUNT(*) AS profilLaeufe, ROUND(MIN(ms), 3) AS besteProfilMs, ROUND(MAX(ms), 3) AS schlechtesteProfilMs
  FROM (SELECT QUERY_ID, SUM(DURATION) * 1000 AS ms FROM information_schema.PROFILING
         WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 16 AND (QUERY_ID - @basis) MOD 3 = 0
         GROUP BY QUERY_ID) l;

SELECT NOW() AS serverzeitEnde;
