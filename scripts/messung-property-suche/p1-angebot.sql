-- Property-Suche, Bau vom 08.09.2026 — Messung der gebauten Statements (Regel L7).
-- Die Statements sind von jOOQ gerendert (MockConnection) und Zeichen fuer Zeichen die des Codes;
-- sie stehen in einer aeusseren Huelle SELECT COUNT(*) ... FROM (<Statement>) — damit kein Prozess-
-- oder Projektname in die Rohausgabe geraet (G1). Pruefwerte je Sitzung hergeleitet, nie im Skript.
-- Wanduhr (SYSDATE(6)) neben dem Profil (E-107), Handler-Zaehler des ersten Laufs, EXPLAIN je Fall.
-- Grenze 10 s = Lese-Pool; ein Abbruch ist das Ergebnis. Lauf mit --force.
SELECT @@global.read_only AS globalReadOnly;
SELECT NOW() AS serverzeitBeginn;
SET profiling = 1;
SET profiling_history_size = 100;
SELECT SYSDATE(6) INTO @t0; SELECT 1; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @eich;
SELECT ROUND(@eich/1000,3) AS eichungLeeresStatementMs;

SELECT '=== Angebot / NEXANS ===' AS marke;
SET max_statement_time = 10;
SET @q = CONCAT(
 'SELECT COUNT(*) AS zeilen FROM (select ',
 '`GlassfishDB`.`MessagePropertySearchListEntry`.`MessagePropertyName`, ',
 '`GlassfishDB`.`MessagePropertySearchListEntry`.`MessagePropertyType` from ',
 '`GlassfishDB`.`MessagePropertySearchListEntry` where ',
 '(`GlassfishDB`.`MessagePropertySearchListEntry`.`MandantID` is null or ',
 '`GlassfishDB`.`MessagePropertySearchListEntry`.`MandantID` = ''NEXANS'') order by ',
 '`GlassfishDB`.`MessagePropertySearchListEntry`.`MessagePropertyName` asc) AS gebaut ');
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
PREPARE qp FROM @q;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d1;
SELECT SUM(DURATION)*1000 INTO @p1 FROM information_schema.PROFILING WHERE QUERY_ID = @basis + 3;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d2;
SELECT SUM(DURATION)*1000 INTO @p2 FROM information_schema.PROFILING WHERE QUERY_ID = @basis + 3;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d3;
SELECT SUM(DURATION)*1000 INTO @p3 FROM information_schema.PROFILING WHERE QUERY_ID = @basis + 3;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d4;
SELECT SUM(DURATION)*1000 INTO @p4 FROM information_schema.PROFILING WHERE QUERY_ID = @basis + 3;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d5;
SELECT SUM(DURATION)*1000 INTO @p5 FROM information_schema.PROFILING WHERE QUERY_ID = @basis + 3;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d6;
SELECT SUM(DURATION)*1000 INTO @p6 FROM information_schema.PROFILING WHERE QUERY_ID = @basis + 3;
DEALLOCATE PREPARE qp;
SELECT ROUND(@d1/1000,3) AS wanduhrLauf1, ROUND(LEAST(@d2,@d3,@d4,@d5,@d6)/1000,3) AS wanduhrBesteVon5;
SELECT ROUND(@p1,3) AS profilLauf1, ROUND(LEAST(@p2,@p3,@p4,@p5,@p6),3) AS profilBesteVon5;

SELECT '=== Angebot / SUTTONS ===' AS marke;
SET max_statement_time = 10;
SET @q = CONCAT(
 'SELECT COUNT(*) AS zeilen FROM (select ',
 '`GlassfishDB`.`MessagePropertySearchListEntry`.`MessagePropertyName`, ',
 '`GlassfishDB`.`MessagePropertySearchListEntry`.`MessagePropertyType` from ',
 '`GlassfishDB`.`MessagePropertySearchListEntry` where ',
 '(`GlassfishDB`.`MessagePropertySearchListEntry`.`MandantID` is null or ',
 '`GlassfishDB`.`MessagePropertySearchListEntry`.`MandantID` = ''SUTTONS'') order by ',
 '`GlassfishDB`.`MessagePropertySearchListEntry`.`MessagePropertyName` asc) AS gebaut ');
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
PREPARE qp FROM @q;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d1;
SELECT SUM(DURATION)*1000 INTO @p1 FROM information_schema.PROFILING WHERE QUERY_ID = @basis + 3;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d2;
SELECT SUM(DURATION)*1000 INTO @p2 FROM information_schema.PROFILING WHERE QUERY_ID = @basis + 3;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d3;
SELECT SUM(DURATION)*1000 INTO @p3 FROM information_schema.PROFILING WHERE QUERY_ID = @basis + 3;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d4;
SELECT SUM(DURATION)*1000 INTO @p4 FROM information_schema.PROFILING WHERE QUERY_ID = @basis + 3;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d5;
SELECT SUM(DURATION)*1000 INTO @p5 FROM information_schema.PROFILING WHERE QUERY_ID = @basis + 3;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d6;
SELECT SUM(DURATION)*1000 INTO @p6 FROM information_schema.PROFILING WHERE QUERY_ID = @basis + 3;
DEALLOCATE PREPARE qp;
SELECT ROUND(@d1/1000,3) AS wanduhrLauf1, ROUND(LEAST(@d2,@d3,@d4,@d5,@d6)/1000,3) AS wanduhrBesteVon5;
SELECT ROUND(@p1,3) AS profilLauf1, ROUND(LEAST(@p2,@p3,@p4,@p5,@p6),3) AS profilBesteVon5;

SELECT '=== Angebot / WOC ===' AS marke;
SET max_statement_time = 10;
SET @q = CONCAT(
 'SELECT COUNT(*) AS zeilen FROM (select ',
 '`GlassfishDB`.`MessagePropertySearchListEntry`.`MessagePropertyName`, ',
 '`GlassfishDB`.`MessagePropertySearchListEntry`.`MessagePropertyType` from ',
 '`GlassfishDB`.`MessagePropertySearchListEntry` where ',
 '(`GlassfishDB`.`MessagePropertySearchListEntry`.`MandantID` is null or ',
 '`GlassfishDB`.`MessagePropertySearchListEntry`.`MandantID` = ''WOC'') order by ',
 '`GlassfishDB`.`MessagePropertySearchListEntry`.`MessagePropertyName` asc) AS gebaut ');
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
PREPARE qp FROM @q;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d1;
SELECT SUM(DURATION)*1000 INTO @p1 FROM information_schema.PROFILING WHERE QUERY_ID = @basis + 3;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d2;
SELECT SUM(DURATION)*1000 INTO @p2 FROM information_schema.PROFILING WHERE QUERY_ID = @basis + 3;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d3;
SELECT SUM(DURATION)*1000 INTO @p3 FROM information_schema.PROFILING WHERE QUERY_ID = @basis + 3;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d4;
SELECT SUM(DURATION)*1000 INTO @p4 FROM information_schema.PROFILING WHERE QUERY_ID = @basis + 3;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d5;
SELECT SUM(DURATION)*1000 INTO @p5 FROM information_schema.PROFILING WHERE QUERY_ID = @basis + 3;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d6;
SELECT SUM(DURATION)*1000 INTO @p6 FROM information_schema.PROFILING WHERE QUERY_ID = @basis + 3;
DEALLOCATE PREPARE qp;
SELECT ROUND(@d1/1000,3) AS wanduhrLauf1, ROUND(LEAST(@d2,@d3,@d4,@d5,@d6)/1000,3) AS wanduhrBesteVon5;
SELECT ROUND(@p1,3) AS profilLauf1, ROUND(LEAST(@p2,@p3,@p4,@p5,@p6),3) AS profilBesteVon5;
