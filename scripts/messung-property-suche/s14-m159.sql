-- M159: der ganze Weg - Wertpraedikat + Verdichtung auf MessageID + Join auf Message
-- + Mandantenkette + Zeitfenster, in der Bauform aus bam-suche.md §4.
-- Grenze = 10 s, die Grenze des Lese-Pools der Anwendung (datenzugriff.md §1).
-- Ein Abbruch ist das Ergebnis. Lauf mit --force, damit die Sitzung weiterlaeuft.
-- Regel G1: kein Wert, keine MessageID, kein Prozess- oder Projektname in der Ausgabe.
SELECT @@global.read_only AS globalReadOnly;
SET max_statement_time = 10;
SET profiling = 1;
SET profiling_history_size = 100;

SELECT '=== NEXANS / Message.GUID / 24h (Boesfall: haeufigster Wert im 30-Tage-Fenster) ===' AS marke;
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

SET @q = CONCAT(
 'SELECT COUNT(*) AS zeilen, MAX(CHAR_LENGTH(p2.ProcessName)) AS maxPnLaenge, ',
 '       MAX(CHAR_LENGTH(prj.ProjectName)) AS maxPjLaenge ',
 'FROM (SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID ',
 '        FROM GlassfishDB.MessageProperty mp ',
 '        JOIN GlassfishDB.Message m ON m.MessageID = mp.MessageID ',
 '       WHERE mp.MessagePropertyName = ''Message.GUID'' AND mp.MessagePropertyValue = ', @w, ' ',
 '         AND m.MessageLastUpdate >= ''2025-12-29 00:00:00'' AND m.MessageLastUpdate < ''2025-12-30 00:00:00'' ',
 '         AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr ',
 '                       JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID ',
 '                      WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = ''NEXANS'') ',
 '       GROUP BY m.MessageID ',
 '       ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC LIMIT 51) AS treffer ',
 'LEFT JOIN GlassfishDB.Process p2 ON p2.ProcessID = treffer.ProcessID ',
 'LEFT JOIN GlassfishDB.Project prj ON prj.ProjectID = p2.ProjectID');
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
PREPARE qp FROM @q;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp;
DEALLOCATE PREPARE qp;
SELECT QUERY_ID, ROUND(SUM(DURATION) * 1000, 3) AS ms
  FROM information_schema.PROFILING WHERE QUERY_ID > @basis + 1
 GROUP BY QUERY_ID ORDER BY QUERY_ID;

SELECT '=== NEXANS / Message.GUID / 30T (Boesfall: haeufigster Wert im 30-Tage-Fenster) ===' AS marke;
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

SET @q = CONCAT(
 'SELECT COUNT(*) AS zeilen, MAX(CHAR_LENGTH(p2.ProcessName)) AS maxPnLaenge, ',
 '       MAX(CHAR_LENGTH(prj.ProjectName)) AS maxPjLaenge ',
 'FROM (SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID ',
 '        FROM GlassfishDB.MessageProperty mp ',
 '        JOIN GlassfishDB.Message m ON m.MessageID = mp.MessageID ',
 '       WHERE mp.MessagePropertyName = ''Message.GUID'' AND mp.MessagePropertyValue = ', @w, ' ',
 '         AND m.MessageLastUpdate >= ''2025-11-30 00:00:00'' AND m.MessageLastUpdate < ''2025-12-30 00:00:00'' ',
 '         AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr ',
 '                       JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID ',
 '                      WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = ''NEXANS'') ',
 '       GROUP BY m.MessageID ',
 '       ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC LIMIT 51) AS treffer ',
 'LEFT JOIN GlassfishDB.Process p2 ON p2.ProcessID = treffer.ProcessID ',
 'LEFT JOIN GlassfishDB.Project prj ON prj.ProjectID = p2.ProjectID');
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
PREPARE qp FROM @q;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp;
DEALLOCATE PREPARE qp;
SELECT QUERY_ID, ROUND(SUM(DURATION) * 1000, 3) AS ms
  FROM information_schema.PROFILING WHERE QUERY_ID > @basis + 1
 GROUP BY QUERY_ID ORDER BY QUERY_ID;

SELECT '=== NEXANS / Message.GUID / 90T (Boesfall: haeufigster Wert im 30-Tage-Fenster) ===' AS marke;
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

SET @q = CONCAT(
 'SELECT COUNT(*) AS zeilen, MAX(CHAR_LENGTH(p2.ProcessName)) AS maxPnLaenge, ',
 '       MAX(CHAR_LENGTH(prj.ProjectName)) AS maxPjLaenge ',
 'FROM (SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID ',
 '        FROM GlassfishDB.MessageProperty mp ',
 '        JOIN GlassfishDB.Message m ON m.MessageID = mp.MessageID ',
 '       WHERE mp.MessagePropertyName = ''Message.GUID'' AND mp.MessagePropertyValue = ', @w, ' ',
 '         AND m.MessageLastUpdate >= ''2025-10-01 00:00:00'' AND m.MessageLastUpdate < ''2025-12-30 00:00:00'' ',
 '         AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr ',
 '                       JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID ',
 '                      WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = ''NEXANS'') ',
 '       GROUP BY m.MessageID ',
 '       ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC LIMIT 51) AS treffer ',
 'LEFT JOIN GlassfishDB.Process p2 ON p2.ProcessID = treffer.ProcessID ',
 'LEFT JOIN GlassfishDB.Project prj ON prj.ProjectID = p2.ProjectID');
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
PREPARE qp FROM @q;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp;
DEALLOCATE PREPARE qp;
SELECT QUERY_ID, ROUND(SUM(DURATION) * 1000, 3) AS ms
  FROM information_schema.PROFILING WHERE QUERY_ID > @basis + 1
 GROUP BY QUERY_ID ORDER BY QUERY_ID;

SELECT '=== NEXANS / Message.ReceiverID / 24h (Boesfall: haeufigster Wert im 30-Tage-Fenster) ===' AS marke;
SELECT QUOTE(v) INTO @w FROM (
  SELECT p.MessagePropertyValue AS v, COUNT(*) AS c
    FROM GlassfishDB.Message m
    JOIN GlassfishDB.MessageProperty p
      ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.ReceiverID'
   WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
     AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
     AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                   JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                  WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
   GROUP BY p.MessagePropertyValue ORDER BY c DESC, v ASC LIMIT 1) x;

SET @q = CONCAT(
 'SELECT COUNT(*) AS zeilen, MAX(CHAR_LENGTH(p2.ProcessName)) AS maxPnLaenge, ',
 '       MAX(CHAR_LENGTH(prj.ProjectName)) AS maxPjLaenge ',
 'FROM (SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID ',
 '        FROM GlassfishDB.MessageProperty mp ',
 '        JOIN GlassfishDB.Message m ON m.MessageID = mp.MessageID ',
 '       WHERE mp.MessagePropertyName = ''Message.ReceiverID'' AND mp.MessagePropertyValue = ', @w, ' ',
 '         AND m.MessageLastUpdate >= ''2025-12-29 00:00:00'' AND m.MessageLastUpdate < ''2025-12-30 00:00:00'' ',
 '         AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr ',
 '                       JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID ',
 '                      WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = ''NEXANS'') ',
 '       GROUP BY m.MessageID ',
 '       ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC LIMIT 51) AS treffer ',
 'LEFT JOIN GlassfishDB.Process p2 ON p2.ProcessID = treffer.ProcessID ',
 'LEFT JOIN GlassfishDB.Project prj ON prj.ProjectID = p2.ProjectID');
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
PREPARE qp FROM @q;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp;
DEALLOCATE PREPARE qp;
SELECT QUERY_ID, ROUND(SUM(DURATION) * 1000, 3) AS ms
  FROM information_schema.PROFILING WHERE QUERY_ID > @basis + 1
 GROUP BY QUERY_ID ORDER BY QUERY_ID;

SELECT '=== NEXANS / Message.ReceiverID / 30T (Boesfall: haeufigster Wert im 30-Tage-Fenster) ===' AS marke;
SELECT QUOTE(v) INTO @w FROM (
  SELECT p.MessagePropertyValue AS v, COUNT(*) AS c
    FROM GlassfishDB.Message m
    JOIN GlassfishDB.MessageProperty p
      ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.ReceiverID'
   WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
     AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
     AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                   JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                  WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
   GROUP BY p.MessagePropertyValue ORDER BY c DESC, v ASC LIMIT 1) x;

SET @q = CONCAT(
 'SELECT COUNT(*) AS zeilen, MAX(CHAR_LENGTH(p2.ProcessName)) AS maxPnLaenge, ',
 '       MAX(CHAR_LENGTH(prj.ProjectName)) AS maxPjLaenge ',
 'FROM (SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID ',
 '        FROM GlassfishDB.MessageProperty mp ',
 '        JOIN GlassfishDB.Message m ON m.MessageID = mp.MessageID ',
 '       WHERE mp.MessagePropertyName = ''Message.ReceiverID'' AND mp.MessagePropertyValue = ', @w, ' ',
 '         AND m.MessageLastUpdate >= ''2025-11-30 00:00:00'' AND m.MessageLastUpdate < ''2025-12-30 00:00:00'' ',
 '         AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr ',
 '                       JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID ',
 '                      WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = ''NEXANS'') ',
 '       GROUP BY m.MessageID ',
 '       ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC LIMIT 51) AS treffer ',
 'LEFT JOIN GlassfishDB.Process p2 ON p2.ProcessID = treffer.ProcessID ',
 'LEFT JOIN GlassfishDB.Project prj ON prj.ProjectID = p2.ProjectID');
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
PREPARE qp FROM @q;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp;
DEALLOCATE PREPARE qp;
SELECT QUERY_ID, ROUND(SUM(DURATION) * 1000, 3) AS ms
  FROM information_schema.PROFILING WHERE QUERY_ID > @basis + 1
 GROUP BY QUERY_ID ORDER BY QUERY_ID;

SELECT '=== NEXANS / Message.ReceiverID / 90T (Boesfall: haeufigster Wert im 30-Tage-Fenster) ===' AS marke;
SELECT QUOTE(v) INTO @w FROM (
  SELECT p.MessagePropertyValue AS v, COUNT(*) AS c
    FROM GlassfishDB.Message m
    JOIN GlassfishDB.MessageProperty p
      ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.ReceiverID'
   WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
     AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
     AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                   JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                  WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
   GROUP BY p.MessagePropertyValue ORDER BY c DESC, v ASC LIMIT 1) x;

SET @q = CONCAT(
 'SELECT COUNT(*) AS zeilen, MAX(CHAR_LENGTH(p2.ProcessName)) AS maxPnLaenge, ',
 '       MAX(CHAR_LENGTH(prj.ProjectName)) AS maxPjLaenge ',
 'FROM (SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID ',
 '        FROM GlassfishDB.MessageProperty mp ',
 '        JOIN GlassfishDB.Message m ON m.MessageID = mp.MessageID ',
 '       WHERE mp.MessagePropertyName = ''Message.ReceiverID'' AND mp.MessagePropertyValue = ', @w, ' ',
 '         AND m.MessageLastUpdate >= ''2025-10-01 00:00:00'' AND m.MessageLastUpdate < ''2025-12-30 00:00:00'' ',
 '         AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr ',
 '                       JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID ',
 '                      WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = ''NEXANS'') ',
 '       GROUP BY m.MessageID ',
 '       ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC LIMIT 51) AS treffer ',
 'LEFT JOIN GlassfishDB.Process p2 ON p2.ProcessID = treffer.ProcessID ',
 'LEFT JOIN GlassfishDB.Project prj ON prj.ProjectID = p2.ProjectID');
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
PREPARE qp FROM @q;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp;
DEALLOCATE PREPARE qp;
SELECT QUERY_ID, ROUND(SUM(DURATION) * 1000, 3) AS ms
  FROM information_schema.PROFILING WHERE QUERY_ID > @basis + 1
 GROUP BY QUERY_ID ORDER BY QUERY_ID;

SELECT '=== NEXANS / Service.Type / 24h (Boesfall: haeufigster Wert im 30-Tage-Fenster) ===' AS marke;
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

SET @q = CONCAT(
 'SELECT COUNT(*) AS zeilen, MAX(CHAR_LENGTH(p2.ProcessName)) AS maxPnLaenge, ',
 '       MAX(CHAR_LENGTH(prj.ProjectName)) AS maxPjLaenge ',
 'FROM (SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID ',
 '        FROM GlassfishDB.MessageProperty mp ',
 '        JOIN GlassfishDB.Message m ON m.MessageID = mp.MessageID ',
 '       WHERE mp.MessagePropertyName = ''Service.Type'' AND mp.MessagePropertyValue = ', @w, ' ',
 '         AND m.MessageLastUpdate >= ''2025-12-29 00:00:00'' AND m.MessageLastUpdate < ''2025-12-30 00:00:00'' ',
 '         AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr ',
 '                       JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID ',
 '                      WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = ''NEXANS'') ',
 '       GROUP BY m.MessageID ',
 '       ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC LIMIT 51) AS treffer ',
 'LEFT JOIN GlassfishDB.Process p2 ON p2.ProcessID = treffer.ProcessID ',
 'LEFT JOIN GlassfishDB.Project prj ON prj.ProjectID = p2.ProjectID');
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
PREPARE qp FROM @q;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp;
DEALLOCATE PREPARE qp;
SELECT QUERY_ID, ROUND(SUM(DURATION) * 1000, 3) AS ms
  FROM information_schema.PROFILING WHERE QUERY_ID > @basis + 1
 GROUP BY QUERY_ID ORDER BY QUERY_ID;

SELECT '=== NEXANS / Service.Type / 30T (Boesfall: haeufigster Wert im 30-Tage-Fenster) ===' AS marke;
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

SET @q = CONCAT(
 'SELECT COUNT(*) AS zeilen, MAX(CHAR_LENGTH(p2.ProcessName)) AS maxPnLaenge, ',
 '       MAX(CHAR_LENGTH(prj.ProjectName)) AS maxPjLaenge ',
 'FROM (SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID ',
 '        FROM GlassfishDB.MessageProperty mp ',
 '        JOIN GlassfishDB.Message m ON m.MessageID = mp.MessageID ',
 '       WHERE mp.MessagePropertyName = ''Service.Type'' AND mp.MessagePropertyValue = ', @w, ' ',
 '         AND m.MessageLastUpdate >= ''2025-11-30 00:00:00'' AND m.MessageLastUpdate < ''2025-12-30 00:00:00'' ',
 '         AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr ',
 '                       JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID ',
 '                      WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = ''NEXANS'') ',
 '       GROUP BY m.MessageID ',
 '       ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC LIMIT 51) AS treffer ',
 'LEFT JOIN GlassfishDB.Process p2 ON p2.ProcessID = treffer.ProcessID ',
 'LEFT JOIN GlassfishDB.Project prj ON prj.ProjectID = p2.ProjectID');
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
PREPARE qp FROM @q;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp;
DEALLOCATE PREPARE qp;
SELECT QUERY_ID, ROUND(SUM(DURATION) * 1000, 3) AS ms
  FROM information_schema.PROFILING WHERE QUERY_ID > @basis + 1
 GROUP BY QUERY_ID ORDER BY QUERY_ID;

SELECT '=== NEXANS / Service.Type / 90T (Boesfall: haeufigster Wert im 30-Tage-Fenster) ===' AS marke;
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

SET @q = CONCAT(
 'SELECT COUNT(*) AS zeilen, MAX(CHAR_LENGTH(p2.ProcessName)) AS maxPnLaenge, ',
 '       MAX(CHAR_LENGTH(prj.ProjectName)) AS maxPjLaenge ',
 'FROM (SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID ',
 '        FROM GlassfishDB.MessageProperty mp ',
 '        JOIN GlassfishDB.Message m ON m.MessageID = mp.MessageID ',
 '       WHERE mp.MessagePropertyName = ''Service.Type'' AND mp.MessagePropertyValue = ', @w, ' ',
 '         AND m.MessageLastUpdate >= ''2025-10-01 00:00:00'' AND m.MessageLastUpdate < ''2025-12-30 00:00:00'' ',
 '         AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr ',
 '                       JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID ',
 '                      WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = ''NEXANS'') ',
 '       GROUP BY m.MessageID ',
 '       ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC LIMIT 51) AS treffer ',
 'LEFT JOIN GlassfishDB.Process p2 ON p2.ProcessID = treffer.ProcessID ',
 'LEFT JOIN GlassfishDB.Project prj ON prj.ProjectID = p2.ProjectID');
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
PREPARE qp FROM @q;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp;
DEALLOCATE PREPARE qp;
SELECT QUERY_ID, ROUND(SUM(DURATION) * 1000, 3) AS ms
  FROM information_schema.PROFILING WHERE QUERY_ID > @basis + 1
 GROUP BY QUERY_ID ORDER BY QUERY_ID;

SELECT '=== SUTTONS / Message.GUID / 24h (Boesfall: haeufigster Wert im 30-Tage-Fenster) ===' AS marke;
SELECT QUOTE(v) INTO @w FROM (
  SELECT p.MessagePropertyValue AS v, COUNT(*) AS c
    FROM GlassfishDB.Message m
    JOIN GlassfishDB.MessageProperty p
      ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.GUID'
   WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
     AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
     AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                   JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                  WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'SUTTONS')
   GROUP BY p.MessagePropertyValue ORDER BY c DESC, v ASC LIMIT 1) x;

SET @q = CONCAT(
 'SELECT COUNT(*) AS zeilen, MAX(CHAR_LENGTH(p2.ProcessName)) AS maxPnLaenge, ',
 '       MAX(CHAR_LENGTH(prj.ProjectName)) AS maxPjLaenge ',
 'FROM (SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID ',
 '        FROM GlassfishDB.MessageProperty mp ',
 '        JOIN GlassfishDB.Message m ON m.MessageID = mp.MessageID ',
 '       WHERE mp.MessagePropertyName = ''Message.GUID'' AND mp.MessagePropertyValue = ', @w, ' ',
 '         AND m.MessageLastUpdate >= ''2025-12-29 00:00:00'' AND m.MessageLastUpdate < ''2025-12-30 00:00:00'' ',
 '         AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr ',
 '                       JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID ',
 '                      WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = ''SUTTONS'') ',
 '       GROUP BY m.MessageID ',
 '       ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC LIMIT 51) AS treffer ',
 'LEFT JOIN GlassfishDB.Process p2 ON p2.ProcessID = treffer.ProcessID ',
 'LEFT JOIN GlassfishDB.Project prj ON prj.ProjectID = p2.ProjectID');
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
PREPARE qp FROM @q;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp;
DEALLOCATE PREPARE qp;
SELECT QUERY_ID, ROUND(SUM(DURATION) * 1000, 3) AS ms
  FROM information_schema.PROFILING WHERE QUERY_ID > @basis + 1
 GROUP BY QUERY_ID ORDER BY QUERY_ID;

SELECT '=== SUTTONS / Message.GUID / 30T (Boesfall: haeufigster Wert im 30-Tage-Fenster) ===' AS marke;
SELECT QUOTE(v) INTO @w FROM (
  SELECT p.MessagePropertyValue AS v, COUNT(*) AS c
    FROM GlassfishDB.Message m
    JOIN GlassfishDB.MessageProperty p
      ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.GUID'
   WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
     AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
     AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                   JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                  WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'SUTTONS')
   GROUP BY p.MessagePropertyValue ORDER BY c DESC, v ASC LIMIT 1) x;

SET @q = CONCAT(
 'SELECT COUNT(*) AS zeilen, MAX(CHAR_LENGTH(p2.ProcessName)) AS maxPnLaenge, ',
 '       MAX(CHAR_LENGTH(prj.ProjectName)) AS maxPjLaenge ',
 'FROM (SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID ',
 '        FROM GlassfishDB.MessageProperty mp ',
 '        JOIN GlassfishDB.Message m ON m.MessageID = mp.MessageID ',
 '       WHERE mp.MessagePropertyName = ''Message.GUID'' AND mp.MessagePropertyValue = ', @w, ' ',
 '         AND m.MessageLastUpdate >= ''2025-11-30 00:00:00'' AND m.MessageLastUpdate < ''2025-12-30 00:00:00'' ',
 '         AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr ',
 '                       JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID ',
 '                      WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = ''SUTTONS'') ',
 '       GROUP BY m.MessageID ',
 '       ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC LIMIT 51) AS treffer ',
 'LEFT JOIN GlassfishDB.Process p2 ON p2.ProcessID = treffer.ProcessID ',
 'LEFT JOIN GlassfishDB.Project prj ON prj.ProjectID = p2.ProjectID');
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
PREPARE qp FROM @q;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp;
DEALLOCATE PREPARE qp;
SELECT QUERY_ID, ROUND(SUM(DURATION) * 1000, 3) AS ms
  FROM information_schema.PROFILING WHERE QUERY_ID > @basis + 1
 GROUP BY QUERY_ID ORDER BY QUERY_ID;

SELECT '=== SUTTONS / Message.GUID / 90T (Boesfall: haeufigster Wert im 30-Tage-Fenster) ===' AS marke;
SELECT QUOTE(v) INTO @w FROM (
  SELECT p.MessagePropertyValue AS v, COUNT(*) AS c
    FROM GlassfishDB.Message m
    JOIN GlassfishDB.MessageProperty p
      ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.GUID'
   WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
     AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
     AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                   JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                  WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'SUTTONS')
   GROUP BY p.MessagePropertyValue ORDER BY c DESC, v ASC LIMIT 1) x;

SET @q = CONCAT(
 'SELECT COUNT(*) AS zeilen, MAX(CHAR_LENGTH(p2.ProcessName)) AS maxPnLaenge, ',
 '       MAX(CHAR_LENGTH(prj.ProjectName)) AS maxPjLaenge ',
 'FROM (SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID ',
 '        FROM GlassfishDB.MessageProperty mp ',
 '        JOIN GlassfishDB.Message m ON m.MessageID = mp.MessageID ',
 '       WHERE mp.MessagePropertyName = ''Message.GUID'' AND mp.MessagePropertyValue = ', @w, ' ',
 '         AND m.MessageLastUpdate >= ''2025-10-01 00:00:00'' AND m.MessageLastUpdate < ''2025-12-30 00:00:00'' ',
 '         AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr ',
 '                       JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID ',
 '                      WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = ''SUTTONS'') ',
 '       GROUP BY m.MessageID ',
 '       ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC LIMIT 51) AS treffer ',
 'LEFT JOIN GlassfishDB.Process p2 ON p2.ProcessID = treffer.ProcessID ',
 'LEFT JOIN GlassfishDB.Project prj ON prj.ProjectID = p2.ProjectID');
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
PREPARE qp FROM @q;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp;
DEALLOCATE PREPARE qp;
SELECT QUERY_ID, ROUND(SUM(DURATION) * 1000, 3) AS ms
  FROM information_schema.PROFILING WHERE QUERY_ID > @basis + 1
 GROUP BY QUERY_ID ORDER BY QUERY_ID;

SELECT '=== SUTTONS / Service.Type / 24h (Boesfall: haeufigster Wert im 30-Tage-Fenster) ===' AS marke;
SELECT QUOTE(v) INTO @w FROM (
  SELECT p.MessagePropertyValue AS v, COUNT(*) AS c
    FROM GlassfishDB.Message m
    JOIN GlassfishDB.MessageProperty p
      ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Service.Type'
   WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
     AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
     AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                   JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                  WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'SUTTONS')
   GROUP BY p.MessagePropertyValue ORDER BY c DESC, v ASC LIMIT 1) x;

SET @q = CONCAT(
 'SELECT COUNT(*) AS zeilen, MAX(CHAR_LENGTH(p2.ProcessName)) AS maxPnLaenge, ',
 '       MAX(CHAR_LENGTH(prj.ProjectName)) AS maxPjLaenge ',
 'FROM (SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID ',
 '        FROM GlassfishDB.MessageProperty mp ',
 '        JOIN GlassfishDB.Message m ON m.MessageID = mp.MessageID ',
 '       WHERE mp.MessagePropertyName = ''Service.Type'' AND mp.MessagePropertyValue = ', @w, ' ',
 '         AND m.MessageLastUpdate >= ''2025-12-29 00:00:00'' AND m.MessageLastUpdate < ''2025-12-30 00:00:00'' ',
 '         AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr ',
 '                       JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID ',
 '                      WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = ''SUTTONS'') ',
 '       GROUP BY m.MessageID ',
 '       ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC LIMIT 51) AS treffer ',
 'LEFT JOIN GlassfishDB.Process p2 ON p2.ProcessID = treffer.ProcessID ',
 'LEFT JOIN GlassfishDB.Project prj ON prj.ProjectID = p2.ProjectID');
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
PREPARE qp FROM @q;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp;
DEALLOCATE PREPARE qp;
SELECT QUERY_ID, ROUND(SUM(DURATION) * 1000, 3) AS ms
  FROM information_schema.PROFILING WHERE QUERY_ID > @basis + 1
 GROUP BY QUERY_ID ORDER BY QUERY_ID;

SELECT '=== SUTTONS / Service.Type / 30T (Boesfall: haeufigster Wert im 30-Tage-Fenster) ===' AS marke;
SELECT QUOTE(v) INTO @w FROM (
  SELECT p.MessagePropertyValue AS v, COUNT(*) AS c
    FROM GlassfishDB.Message m
    JOIN GlassfishDB.MessageProperty p
      ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Service.Type'
   WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
     AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
     AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                   JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                  WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'SUTTONS')
   GROUP BY p.MessagePropertyValue ORDER BY c DESC, v ASC LIMIT 1) x;

SET @q = CONCAT(
 'SELECT COUNT(*) AS zeilen, MAX(CHAR_LENGTH(p2.ProcessName)) AS maxPnLaenge, ',
 '       MAX(CHAR_LENGTH(prj.ProjectName)) AS maxPjLaenge ',
 'FROM (SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID ',
 '        FROM GlassfishDB.MessageProperty mp ',
 '        JOIN GlassfishDB.Message m ON m.MessageID = mp.MessageID ',
 '       WHERE mp.MessagePropertyName = ''Service.Type'' AND mp.MessagePropertyValue = ', @w, ' ',
 '         AND m.MessageLastUpdate >= ''2025-11-30 00:00:00'' AND m.MessageLastUpdate < ''2025-12-30 00:00:00'' ',
 '         AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr ',
 '                       JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID ',
 '                      WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = ''SUTTONS'') ',
 '       GROUP BY m.MessageID ',
 '       ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC LIMIT 51) AS treffer ',
 'LEFT JOIN GlassfishDB.Process p2 ON p2.ProcessID = treffer.ProcessID ',
 'LEFT JOIN GlassfishDB.Project prj ON prj.ProjectID = p2.ProjectID');
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
PREPARE qp FROM @q;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp;
DEALLOCATE PREPARE qp;
SELECT QUERY_ID, ROUND(SUM(DURATION) * 1000, 3) AS ms
  FROM information_schema.PROFILING WHERE QUERY_ID > @basis + 1
 GROUP BY QUERY_ID ORDER BY QUERY_ID;

SELECT '=== SUTTONS / Service.Type / 90T (Boesfall: haeufigster Wert im 30-Tage-Fenster) ===' AS marke;
SELECT QUOTE(v) INTO @w FROM (
  SELECT p.MessagePropertyValue AS v, COUNT(*) AS c
    FROM GlassfishDB.Message m
    JOIN GlassfishDB.MessageProperty p
      ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Service.Type'
   WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
     AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
     AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                   JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                  WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'SUTTONS')
   GROUP BY p.MessagePropertyValue ORDER BY c DESC, v ASC LIMIT 1) x;

SET @q = CONCAT(
 'SELECT COUNT(*) AS zeilen, MAX(CHAR_LENGTH(p2.ProcessName)) AS maxPnLaenge, ',
 '       MAX(CHAR_LENGTH(prj.ProjectName)) AS maxPjLaenge ',
 'FROM (SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID ',
 '        FROM GlassfishDB.MessageProperty mp ',
 '        JOIN GlassfishDB.Message m ON m.MessageID = mp.MessageID ',
 '       WHERE mp.MessagePropertyName = ''Service.Type'' AND mp.MessagePropertyValue = ', @w, ' ',
 '         AND m.MessageLastUpdate >= ''2025-10-01 00:00:00'' AND m.MessageLastUpdate < ''2025-12-30 00:00:00'' ',
 '         AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr ',
 '                       JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID ',
 '                      WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = ''SUTTONS'') ',
 '       GROUP BY m.MessageID ',
 '       ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC LIMIT 51) AS treffer ',
 'LEFT JOIN GlassfishDB.Process p2 ON p2.ProcessID = treffer.ProcessID ',
 'LEFT JOIN GlassfishDB.Project prj ON prj.ProjectID = p2.ProjectID');
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
PREPARE qp FROM @q;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp;
DEALLOCATE PREPARE qp;
SELECT QUERY_ID, ROUND(SUM(DURATION) * 1000, 3) AS ms
  FROM information_schema.PROFILING WHERE QUERY_ID > @basis + 1
 GROUP BY QUERY_ID ORDER BY QUERY_ID;
