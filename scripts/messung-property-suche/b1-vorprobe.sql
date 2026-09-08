-- L4-Pfad-Runde - Sitzung B1: Vorprobe. Welche Materialisierungsform traegt Fassung B?
-- BG (GROUP BY MessageID) gegen BL (ORDER BY ... LIMIT 1e9), NEXANS / Message.SNDPRN, 24 h und 30 T.
-- Dazu die Probe, ob ANALYZE <select> (r_rows je Tabelle) ueber PREPARE laeuft - fuer M170.
-- Grenze 10 s = Lese-Pool. Lauf mit --force. G1: kein Wert im Skript, keiner in der Ausgabe.
SELECT @@global.read_only AS globalReadOnly;
SET max_statement_time = 10;
SET profiling = 1;
SET profiling_history_size = 100;
SELECT '=== B1 / NEXANS / Message.SNDPRN / 24h / BG ===' AS marke;
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
 '               GROUP BY m.MessageID) AS f ',
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
PREPARE qp FROM @q;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp;
DEALLOCATE PREPARE qp;
SELECT COUNT(*) AS anzahlLaeufe, ROUND(MIN(ms), 3) AS besteMsLauf2bis6, ROUND(MAX(ms), 3) AS schlechtesteMs
  FROM (SELECT QUERY_ID, SUM(DURATION) * 1000 AS ms FROM information_schema.PROFILING
         WHERE QUERY_ID > @basis + 2 AND QUERY_ID <= @basis + 7 GROUP BY QUERY_ID) l;
SELECT QUERY_ID - @basis - 1 AS lauf, ROUND(SUM(DURATION) * 1000, 3) AS ms
  FROM information_schema.PROFILING WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 7
 GROUP BY QUERY_ID ORDER BY QUERY_ID;

SELECT '=== B1 / NEXANS / Message.SNDPRN / 30T / BG ===' AS marke;
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
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
SET @a = CONCAT('ANALYZE ', @q);
PREPARE ap FROM @a; EXECUTE ap; DEALLOCATE PREPARE ap;
PREPARE qp FROM @q;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp;
DEALLOCATE PREPARE qp;
SELECT COUNT(*) AS anzahlLaeufe, ROUND(MIN(ms), 3) AS besteMsLauf2bis6, ROUND(MAX(ms), 3) AS schlechtesteMs
  FROM (SELECT QUERY_ID, SUM(DURATION) * 1000 AS ms FROM information_schema.PROFILING
         WHERE QUERY_ID > @basis + 2 AND QUERY_ID <= @basis + 7 GROUP BY QUERY_ID) l;
SELECT QUERY_ID - @basis - 1 AS lauf, ROUND(SUM(DURATION) * 1000, 3) AS ms
  FROM information_schema.PROFILING WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 7
 GROUP BY QUERY_ID ORDER BY QUERY_ID;

SELECT '=== B1 / NEXANS / Message.SNDPRN / 24h / BL ===' AS marke;
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
PREPARE qp FROM @q;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp;
DEALLOCATE PREPARE qp;
SELECT COUNT(*) AS anzahlLaeufe, ROUND(MIN(ms), 3) AS besteMsLauf2bis6, ROUND(MAX(ms), 3) AS schlechtesteMs
  FROM (SELECT QUERY_ID, SUM(DURATION) * 1000 AS ms FROM information_schema.PROFILING
         WHERE QUERY_ID > @basis + 2 AND QUERY_ID <= @basis + 7 GROUP BY QUERY_ID) l;
SELECT QUERY_ID - @basis - 1 AS lauf, ROUND(SUM(DURATION) * 1000, 3) AS ms
  FROM information_schema.PROFILING WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 7
 GROUP BY QUERY_ID ORDER BY QUERY_ID;

SELECT '=== B1 / NEXANS / Message.SNDPRN / 30T / BL ===' AS marke;
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
PREPARE qp FROM @q;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp;
DEALLOCATE PREPARE qp;
SELECT COUNT(*) AS anzahlLaeufe, ROUND(MIN(ms), 3) AS besteMsLauf2bis6, ROUND(MAX(ms), 3) AS schlechtesteMs
  FROM (SELECT QUERY_ID, SUM(DURATION) * 1000 AS ms FROM information_schema.PROFILING
         WHERE QUERY_ID > @basis + 2 AND QUERY_ID <= @basis + 7 GROUP BY QUERY_ID) l;
SELECT QUERY_ID - @basis - 1 AS lauf, ROUND(SUM(DURATION) * 1000, 3) AS ms
  FROM information_schema.PROFILING WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 7
 GROUP BY QUERY_ID ORDER BY QUERY_ID;

SELECT NOW() AS serverzeitEnde;
