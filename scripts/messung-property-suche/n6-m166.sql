-- M166 — der ganze Weg. Statement aus M159 (s14), unveraendert: Wertpraedikat + Verdichtung auf
-- MessageID + Join auf Message + Mandantenkette + Zeitfenster, Bauform aus bam-suche.md §4
-- (erst deckeln, dann beschriften; Laengen statt Namen in der Ausgabe, G1).
-- Namen: die drei neuen und Converter.TransactionID (Luecke 6). Fenster 24 h und 30 Tage.
-- Pruefwert je Fall: der haeufigste Wert des 30-Tage-Fensters desselben Mandanten (Boesfall).
-- SUTTONS nur fuer Converter.TransactionID — die drei neuen Namen haben dort keine Zeile (M162-5).
-- Grenze 10 s = Lese-Pool; ein Abbruch ist das Ergebnis. Lauf mit --force.
SELECT @@global.read_only AS globalReadOnly;
SET max_statement_time = 10;
SET profiling = 1;
SET profiling_history_size = 100;

SELECT '=== NEXANS / Message.DestinationFilename / 24h (Boesfall: haeufigster Wert im 30-Tage-Fenster) ===' AS marke;
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
 'FROM (SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID ',
 '        FROM GlassfishDB.MessageProperty mp ',
 '        JOIN GlassfishDB.Message m ON m.MessageID = mp.MessageID ',
 '       WHERE mp.MessagePropertyName = ''Message.DestinationFilename'' AND mp.MessagePropertyValue = ', @w, ' ',
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
SELECT COUNT(*) AS anzahlLaeufe, ROUND(MIN(ms), 3) AS besteMs, ROUND(MAX(ms), 3) AS schlechtesteMs
  FROM (SELECT QUERY_ID, SUM(DURATION) * 1000 AS ms FROM information_schema.PROFILING
         WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 7 GROUP BY QUERY_ID) l;
SELECT QUERY_ID - @basis - 1 AS lauf, ROUND(SUM(DURATION) * 1000, 3) AS ms
  FROM information_schema.PROFILING WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 7
 GROUP BY QUERY_ID ORDER BY QUERY_ID;

SELECT '=== NEXANS / Message.DestinationFilename / 30T (Boesfall: haeufigster Wert im 30-Tage-Fenster) ===' AS marke;
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
 'FROM (SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID ',
 '        FROM GlassfishDB.MessageProperty mp ',
 '        JOIN GlassfishDB.Message m ON m.MessageID = mp.MessageID ',
 '       WHERE mp.MessagePropertyName = ''Message.DestinationFilename'' AND mp.MessagePropertyValue = ', @w, ' ',
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
SELECT COUNT(*) AS anzahlLaeufe, ROUND(MIN(ms), 3) AS besteMs, ROUND(MAX(ms), 3) AS schlechtesteMs
  FROM (SELECT QUERY_ID, SUM(DURATION) * 1000 AS ms FROM information_schema.PROFILING
         WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 7 GROUP BY QUERY_ID) l;
SELECT QUERY_ID - @basis - 1 AS lauf, ROUND(SUM(DURATION) * 1000, 3) AS ms
  FROM information_schema.PROFILING WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 7
 GROUP BY QUERY_ID ORDER BY QUERY_ID;

SELECT '=== NEXANS / Message.SNDPRN / 24h (Boesfall: haeufigster Wert im 30-Tage-Fenster) ===' AS marke;
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
SELECT COUNT(*) AS anzahlLaeufe, ROUND(MIN(ms), 3) AS besteMs, ROUND(MAX(ms), 3) AS schlechtesteMs
  FROM (SELECT QUERY_ID, SUM(DURATION) * 1000 AS ms FROM information_schema.PROFILING
         WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 7 GROUP BY QUERY_ID) l;
SELECT QUERY_ID - @basis - 1 AS lauf, ROUND(SUM(DURATION) * 1000, 3) AS ms
  FROM information_schema.PROFILING WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 7
 GROUP BY QUERY_ID ORDER BY QUERY_ID;

SELECT '=== NEXANS / Message.SNDPRN / 30T (Boesfall: haeufigster Wert im 30-Tage-Fenster) ===' AS marke;
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
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
PREPARE qp FROM @q;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp;
DEALLOCATE PREPARE qp;
SELECT COUNT(*) AS anzahlLaeufe, ROUND(MIN(ms), 3) AS besteMs, ROUND(MAX(ms), 3) AS schlechtesteMs
  FROM (SELECT QUERY_ID, SUM(DURATION) * 1000 AS ms FROM information_schema.PROFILING
         WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 7 GROUP BY QUERY_ID) l;
SELECT QUERY_ID - @basis - 1 AS lauf, ROUND(SUM(DURATION) * 1000, 3) AS ms
  FROM information_schema.PROFILING WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 7
 GROUP BY QUERY_ID ORDER BY QUERY_ID;

SELECT '=== NEXANS / Message.VFN / 24h (Boesfall: haeufigster Wert im 30-Tage-Fenster) ===' AS marke;
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
 'FROM (SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID ',
 '        FROM GlassfishDB.MessageProperty mp ',
 '        JOIN GlassfishDB.Message m ON m.MessageID = mp.MessageID ',
 '       WHERE mp.MessagePropertyName = ''Message.VFN'' AND mp.MessagePropertyValue = ', @w, ' ',
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
SELECT COUNT(*) AS anzahlLaeufe, ROUND(MIN(ms), 3) AS besteMs, ROUND(MAX(ms), 3) AS schlechtesteMs
  FROM (SELECT QUERY_ID, SUM(DURATION) * 1000 AS ms FROM information_schema.PROFILING
         WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 7 GROUP BY QUERY_ID) l;
SELECT QUERY_ID - @basis - 1 AS lauf, ROUND(SUM(DURATION) * 1000, 3) AS ms
  FROM information_schema.PROFILING WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 7
 GROUP BY QUERY_ID ORDER BY QUERY_ID;

SELECT '=== NEXANS / Message.VFN / 30T (Boesfall: haeufigster Wert im 30-Tage-Fenster) ===' AS marke;
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
 'FROM (SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID ',
 '        FROM GlassfishDB.MessageProperty mp ',
 '        JOIN GlassfishDB.Message m ON m.MessageID = mp.MessageID ',
 '       WHERE mp.MessagePropertyName = ''Message.VFN'' AND mp.MessagePropertyValue = ', @w, ' ',
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
SELECT COUNT(*) AS anzahlLaeufe, ROUND(MIN(ms), 3) AS besteMs, ROUND(MAX(ms), 3) AS schlechtesteMs
  FROM (SELECT QUERY_ID, SUM(DURATION) * 1000 AS ms FROM information_schema.PROFILING
         WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 7 GROUP BY QUERY_ID) l;
SELECT QUERY_ID - @basis - 1 AS lauf, ROUND(SUM(DURATION) * 1000, 3) AS ms
  FROM information_schema.PROFILING WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 7
 GROUP BY QUERY_ID ORDER BY QUERY_ID;

SELECT '=== NEXANS / Converter.TransactionID / 24h (Boesfall: haeufigster Wert im 30-Tage-Fenster) ===' AS marke;
SELECT QUOTE(v) INTO @w FROM (
  SELECT p.MessagePropertyValue AS v, COUNT(*) AS c
    FROM GlassfishDB.Message m
    JOIN GlassfishDB.MessageProperty p
      ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Converter.TransactionID'
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
 '       WHERE mp.MessagePropertyName = ''Converter.TransactionID'' AND mp.MessagePropertyValue = ', @w, ' ',
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
SELECT COUNT(*) AS anzahlLaeufe, ROUND(MIN(ms), 3) AS besteMs, ROUND(MAX(ms), 3) AS schlechtesteMs
  FROM (SELECT QUERY_ID, SUM(DURATION) * 1000 AS ms FROM information_schema.PROFILING
         WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 7 GROUP BY QUERY_ID) l;
SELECT QUERY_ID - @basis - 1 AS lauf, ROUND(SUM(DURATION) * 1000, 3) AS ms
  FROM information_schema.PROFILING WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 7
 GROUP BY QUERY_ID ORDER BY QUERY_ID;

SELECT '=== NEXANS / Converter.TransactionID / 30T (Boesfall: haeufigster Wert im 30-Tage-Fenster) ===' AS marke;
SELECT QUOTE(v) INTO @w FROM (
  SELECT p.MessagePropertyValue AS v, COUNT(*) AS c
    FROM GlassfishDB.Message m
    JOIN GlassfishDB.MessageProperty p
      ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Converter.TransactionID'
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
 '       WHERE mp.MessagePropertyName = ''Converter.TransactionID'' AND mp.MessagePropertyValue = ', @w, ' ',
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
SELECT COUNT(*) AS anzahlLaeufe, ROUND(MIN(ms), 3) AS besteMs, ROUND(MAX(ms), 3) AS schlechtesteMs
  FROM (SELECT QUERY_ID, SUM(DURATION) * 1000 AS ms FROM information_schema.PROFILING
         WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 7 GROUP BY QUERY_ID) l;
SELECT QUERY_ID - @basis - 1 AS lauf, ROUND(SUM(DURATION) * 1000, 3) AS ms
  FROM information_schema.PROFILING WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 7
 GROUP BY QUERY_ID ORDER BY QUERY_ID;

SELECT '=== SUTTONS / Converter.TransactionID / 24h (Boesfall: haeufigster Wert im 30-Tage-Fenster) ===' AS marke;
SELECT QUOTE(v) INTO @w FROM (
  SELECT p.MessagePropertyValue AS v, COUNT(*) AS c
    FROM GlassfishDB.Message m
    JOIN GlassfishDB.MessageProperty p
      ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Converter.TransactionID'
   WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
     AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
     AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                   JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                  WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'SUTTONS')
   GROUP BY p.MessagePropertyValue ORDER BY c DESC, v ASC LIMIT 1) x;
SELECT CHAR_LENGTH(@w) - 2 AS laengeDesPruefwertsMindestens;

SET @q = CONCAT(
 'SELECT COUNT(*) AS zeilen, MAX(CHAR_LENGTH(p2.ProcessName)) AS maxPnLaenge, ',
 '       MAX(CHAR_LENGTH(prj.ProjectName)) AS maxPjLaenge ',
 'FROM (SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID ',
 '        FROM GlassfishDB.MessageProperty mp ',
 '        JOIN GlassfishDB.Message m ON m.MessageID = mp.MessageID ',
 '       WHERE mp.MessagePropertyName = ''Converter.TransactionID'' AND mp.MessagePropertyValue = ', @w, ' ',
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
SELECT COUNT(*) AS anzahlLaeufe, ROUND(MIN(ms), 3) AS besteMs, ROUND(MAX(ms), 3) AS schlechtesteMs
  FROM (SELECT QUERY_ID, SUM(DURATION) * 1000 AS ms FROM information_schema.PROFILING
         WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 7 GROUP BY QUERY_ID) l;
SELECT QUERY_ID - @basis - 1 AS lauf, ROUND(SUM(DURATION) * 1000, 3) AS ms
  FROM information_schema.PROFILING WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 7
 GROUP BY QUERY_ID ORDER BY QUERY_ID;

SELECT '=== SUTTONS / Converter.TransactionID / 30T (Boesfall: haeufigster Wert im 30-Tage-Fenster) ===' AS marke;
SELECT QUOTE(v) INTO @w FROM (
  SELECT p.MessagePropertyValue AS v, COUNT(*) AS c
    FROM GlassfishDB.Message m
    JOIN GlassfishDB.MessageProperty p
      ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Converter.TransactionID'
   WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
     AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
     AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                   JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                  WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'SUTTONS')
   GROUP BY p.MessagePropertyValue ORDER BY c DESC, v ASC LIMIT 1) x;
SELECT CHAR_LENGTH(@w) - 2 AS laengeDesPruefwertsMindestens;

SET @q = CONCAT(
 'SELECT COUNT(*) AS zeilen, MAX(CHAR_LENGTH(p2.ProcessName)) AS maxPnLaenge, ',
 '       MAX(CHAR_LENGTH(prj.ProjectName)) AS maxPjLaenge ',
 'FROM (SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID ',
 '        FROM GlassfishDB.MessageProperty mp ',
 '        JOIN GlassfishDB.Message m ON m.MessageID = mp.MessageID ',
 '       WHERE mp.MessagePropertyName = ''Converter.TransactionID'' AND mp.MessagePropertyValue = ', @w, ' ',
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
SELECT COUNT(*) AS anzahlLaeufe, ROUND(MIN(ms), 3) AS besteMs, ROUND(MAX(ms), 3) AS schlechtesteMs
  FROM (SELECT QUERY_ID, SUM(DURATION) * 1000 AS ms FROM information_schema.PROFILING
         WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 7 GROUP BY QUERY_ID) l;
SELECT QUERY_ID - @basis - 1 AS lauf, ROUND(SUM(DURATION) * 1000, 3) AS ms
  FROM information_schema.PROFILING WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 7
 GROUP BY QUERY_ID ORDER BY QUERY_ID;
