-- M167 — Was die Nachpruefung auf der Zeile kostet. Voraussetzung aus M164 erfuellt: acht Werte von
-- Message.DestinationFilename ueberschreiten in Fenster B (NEXANS) die 50 Zeichen, jeder mit genau
-- einer Zeile. Vergleichbare Trefferzahl ist damit 1: ein Wert ueber dem Praefix gegen einen Wert,
-- der hineinpasst — einmal mit genau 50 Zeichen (Praefixgrenze), einmal kurz.
-- Gemessen: EXPLAIN, beste von fuenf nach Aufwaermlauf, und die Handler-Zaehler EINES Laufs
-- (gelesene Indexeintraege, ICP-Versuche und -Treffer, Zeilenzugriffe) — gemessen, nicht geschaetzt.
-- Dazu die Breite des Indexbereichs ueber die GANZE Tabelle (LIKE 'praefix%', maskiert), denn der
-- Index kennt kein Fenster.
-- G1: kein Wert im Skript, keiner in der Ausgabe — nur Laengen, Zaehler, Laufzeiten.
-- Grenze 10 s = Lese-Pool. Lauf mit --force.
SELECT @@global.read_only AS globalReadOnly;
SET max_statement_time = 10;
SET profiling = 1;
SET profiling_history_size = 100;

SELECT '--- M167-0 Auswahl der drei Pruefwerte (deterministisch, nur Laenge und Zeilenzahl ausgegeben) ---' AS marke;

-- (L) laengster Wert ueber 50 Zeichen, bei Gleichstand der lexikografisch erste
SELECT v, c INTO @rawL, @cL FROM (
  SELECT p.MessagePropertyValue AS v, COUNT(*) AS c
    FROM GlassfishDB.Message m
    JOIN GlassfishDB.MessageProperty p
      ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.DestinationFilename'
   WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
     AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
     AND CHAR_LENGTH(p.MessagePropertyValue) > 50
     AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                   JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                  WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
   GROUP BY p.MessagePropertyValue
   ORDER BY CHAR_LENGTH(v) DESC, c DESC, v ASC LIMIT 1) x;

-- (G) Wert mit genau 50 Zeichen und derselben Zeilenzahl wie (L), lexikografisch der erste
SELECT v INTO @rawG FROM (
  SELECT p.MessagePropertyValue AS v, COUNT(*) AS c
    FROM GlassfishDB.Message m
    JOIN GlassfishDB.MessageProperty p
      ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.DestinationFilename'
   WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
     AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
     AND CHAR_LENGTH(p.MessagePropertyValue) = 50
     AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                   JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                  WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
   GROUP BY p.MessagePropertyValue
   HAVING c = @cL
   ORDER BY v ASC LIMIT 1) x;

-- (K) kurzer Wert (hoechstens 10 Zeichen) mit derselben Zeilenzahl wie (L), lexikografisch der erste
SELECT v INTO @rawK FROM (
  SELECT p.MessagePropertyValue AS v, COUNT(*) AS c
    FROM GlassfishDB.Message m
    JOIN GlassfishDB.MessageProperty p
      ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.DestinationFilename'
   WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
     AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
     AND CHAR_LENGTH(p.MessagePropertyValue) <= 10
     AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                   JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                  WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
   GROUP BY p.MessagePropertyValue
   HAVING c = @cL
   ORDER BY v ASC LIMIT 1) x;

SELECT CHAR_LENGTH(@rawL) AS laengeL, @cL AS zeilenLImFenster,
       CHAR_LENGTH(@rawG) AS laengeG, CHAR_LENGTH(@rawK) AS laengeK;

SET @wL = QUOTE(@rawL); SET @wG = QUOTE(@rawG); SET @wK = QUOTE(@rawK);

SELECT '--- M167-1 Breite des Indexbereichs ueber die ganze Tabelle: Zeilen, die den 50-Zeichen-Praefix teilen (LIKE, % und _ maskiert) ---' AS marke;
SET @pfL = LEFT(@rawL, 50);
SET @likeL = QUOTE(CONCAT(REPLACE(REPLACE(REPLACE(@pfL, '\\', '\\\\'), '%', '\\%'), '_', '\\_'), '%'));
SET @qp = CONCAT('SELECT COUNT(*) AS zeilenImPraefixbereichL, SUM(MessagePropertyValue = ', @wL, ') AS davonExaktL ',
                 'FROM GlassfishDB.MessageProperty WHERE MessagePropertyName = ''Message.DestinationFilename'' ',
                 'AND MessagePropertyValue LIKE ', @likeL);
SET @ep = CONCAT('EXPLAIN ', @qp);
PREPARE e FROM @ep; EXECUTE e; DEALLOCATE PREPARE e;
PREPARE q FROM @qp; EXECUTE q; DEALLOCATE PREPARE q;

-- Gegenprobe, dass die Maskierung greift: derselbe LIKE-Ausdruck fuer (G), erwartet mindestens die exakten Treffer
SET @pfG = LEFT(@rawG, 50);
SET @likeG = QUOTE(CONCAT(REPLACE(REPLACE(REPLACE(@pfG, '\\', '\\\\'), '%', '\\%'), '_', '\\_'), '%'));
SET @qp = CONCAT('SELECT COUNT(*) AS zeilenImPraefixbereichG, SUM(MessagePropertyValue = ', @wG, ') AS davonExaktG ',
                 'FROM GlassfishDB.MessageProperty WHERE MessagePropertyName = ''Message.DestinationFilename'' ',
                 'AND MessagePropertyValue LIKE ', @likeG);
PREPARE q FROM @qp; EXECUTE q; DEALLOCATE PREPARE q;

SELECT '--- M167-2 Wertpraedikat ohne Join (Form M158/M165) fuer L, G, K: EXPLAIN, Handler-Zaehler eines Laufs, beste von fuenf ---' AS marke;

SELECT '=== (L) ueber dem Praefix ===' AS marke;
SET @q = CONCAT('SELECT COUNT(*) AS treffer FROM GlassfishDB.MessageProperty ',
                'WHERE MessagePropertyName = ''Message.DestinationFilename'' AND MessagePropertyValue = ', @wL);
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
PREPARE qp FROM @q;
EXECUTE qp;
SELECT VARIABLE_NAME, VARIABLE_VALUE INTO @n1, @v1 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY';
SELECT VARIABLE_VALUE INTO @v2 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT';
SELECT VARIABLE_VALUE INTO @v3 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS';
SELECT VARIABLE_VALUE INTO @v4 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_MATCH';
SELECT VARIABLE_VALUE INTO @v5 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT';
EXECUTE qp;
SELECT 'L' AS fall,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY') - @v1 AS readKey,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT') - @v2 AS readNext,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS') - @v3 AS icpAttempts,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_MATCH') - @v4 AS icpMatch,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT') - @v5 AS readRndNext;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp;
DEALLOCATE PREPARE qp;
SELECT COUNT(*) AS anzahlLaeufe, ROUND(MIN(ms), 3) AS besteMs, ROUND(MAX(ms), 3) AS schlechtesteMs
  FROM (SELECT QUERY_ID, SUM(DURATION) * 1000 AS ms FROM information_schema.PROFILING
         WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 7 GROUP BY QUERY_ID) l;

SELECT '=== (G) genau 50 Zeichen ===' AS marke;
SET @q = CONCAT('SELECT COUNT(*) AS treffer FROM GlassfishDB.MessageProperty ',
                'WHERE MessagePropertyName = ''Message.DestinationFilename'' AND MessagePropertyValue = ', @wG);
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
PREPARE qp FROM @q;
EXECUTE qp;
SELECT VARIABLE_VALUE INTO @v1 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY';
SELECT VARIABLE_VALUE INTO @v2 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT';
SELECT VARIABLE_VALUE INTO @v3 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS';
SELECT VARIABLE_VALUE INTO @v4 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_MATCH';
SELECT VARIABLE_VALUE INTO @v5 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT';
EXECUTE qp;
SELECT 'G' AS fall,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY') - @v1 AS readKey,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT') - @v2 AS readNext,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS') - @v3 AS icpAttempts,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_MATCH') - @v4 AS icpMatch,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT') - @v5 AS readRndNext;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp;
DEALLOCATE PREPARE qp;
SELECT COUNT(*) AS anzahlLaeufe, ROUND(MIN(ms), 3) AS besteMs, ROUND(MAX(ms), 3) AS schlechtesteMs
  FROM (SELECT QUERY_ID, SUM(DURATION) * 1000 AS ms FROM information_schema.PROFILING
         WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 7 GROUP BY QUERY_ID) l;

SELECT '=== (K) kurz ===' AS marke;
SET @q = CONCAT('SELECT COUNT(*) AS treffer FROM GlassfishDB.MessageProperty ',
                'WHERE MessagePropertyName = ''Message.DestinationFilename'' AND MessagePropertyValue = ', @wK);
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
PREPARE qp FROM @q;
EXECUTE qp;
SELECT VARIABLE_VALUE INTO @v1 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY';
SELECT VARIABLE_VALUE INTO @v2 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT';
SELECT VARIABLE_VALUE INTO @v3 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS';
SELECT VARIABLE_VALUE INTO @v4 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_MATCH';
SELECT VARIABLE_VALUE INTO @v5 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT';
EXECUTE qp;
SELECT 'K' AS fall,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY') - @v1 AS readKey,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT') - @v2 AS readNext,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS') - @v3 AS icpAttempts,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_MATCH') - @v4 AS icpMatch,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT') - @v5 AS readRndNext;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp; EXECUTE qp;
DEALLOCATE PREPARE qp;
SELECT COUNT(*) AS anzahlLaeufe, ROUND(MIN(ms), 3) AS besteMs, ROUND(MAX(ms), 3) AS schlechtesteMs
  FROM (SELECT QUERY_ID, SUM(DURATION) * 1000 AS ms FROM information_schema.PROFILING
         WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 7 GROUP BY QUERY_ID) l;

SELECT '--- M167-3 Eichung der Zaehler: dieselben fuenf Zaehler um eine leere Abfrage (SELECT 1) herum ---' AS marke;
SELECT VARIABLE_VALUE INTO @v1 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY';
SELECT VARIABLE_VALUE INTO @v2 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT';
SELECT VARIABLE_VALUE INTO @v3 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS';
SELECT VARIABLE_VALUE INTO @v4 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_MATCH';
SELECT VARIABLE_VALUE INTO @v5 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT';
SELECT 1;
SELECT 'leer' AS fall,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY') - @v1 AS readKey,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT') - @v2 AS readNext,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS') - @v3 AS icpAttempts,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_MATCH') - @v4 AS icpMatch,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT') - @v5 AS readRndNext;

SELECT '--- M167-4 Der ganze Weg (Form M159/M166) fuer L und G, 30 Tage, NEXANS ---' AS marke;
SELECT '=== (L) ganzer Weg ===' AS marke;
SET @q = CONCAT(
 'SELECT COUNT(*) AS zeilen, MAX(CHAR_LENGTH(p2.ProcessName)) AS maxPnLaenge, ',
 '       MAX(CHAR_LENGTH(prj.ProjectName)) AS maxPjLaenge ',
 'FROM (SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID ',
 '        FROM GlassfishDB.MessageProperty mp ',
 '        JOIN GlassfishDB.Message m ON m.MessageID = mp.MessageID ',
 '       WHERE mp.MessagePropertyName = ''Message.DestinationFilename'' AND mp.MessagePropertyValue = ', @wL, ' ',
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

SELECT '=== (G) ganzer Weg ===' AS marke;
SET @q = CONCAT(
 'SELECT COUNT(*) AS zeilen, MAX(CHAR_LENGTH(p2.ProcessName)) AS maxPnLaenge, ',
 '       MAX(CHAR_LENGTH(prj.ProjectName)) AS maxPjLaenge ',
 'FROM (SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID ',
 '        FROM GlassfishDB.MessageProperty mp ',
 '        JOIN GlassfishDB.Message m ON m.MessageID = mp.MessageID ',
 '       WHERE mp.MessagePropertyName = ''Message.DestinationFilename'' AND mp.MessagePropertyValue = ', @wG, ' ',
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
