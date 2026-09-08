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

SELECT '=== EAV / NEXANS / Converter.TransactionID / 24h (Boesfall: haeufigster Wert im 30-Tage-Fenster) ===' AS marke;
SET max_statement_time = 60;
SELECT QUOTE(v) INTO @w FROM (
  SELECT p.MessagePropertyValue AS v, COUNT(*) AS c
    FROM GlassfishDB.Message m
    JOIN GlassfishDB.MessageProperty p
      ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Converter.TransactionID'
   WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00' AND m.MessageLastUpdate < '2025-12-30 00:00:00'
     AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
   GROUP BY p.MessagePropertyValue ORDER BY c DESC, v ASC LIMIT 1) x;
SELECT CHAR_LENGTH(@w) - 2 AS laengeDesPruefwerts_w;
SET max_statement_time = 10;
SET @q = CONCAT(
 'SELECT COUNT(*) AS zeilen, MAX(CHAR_LENGTH(ProcessName)) AS maxPnLaenge FROM (select ',
 '`treffer`.`MessageID`, `treffer`.`MessageLastUpdate`, `treffer`.`MessageStatus`, `treffer`.`ProcessID`, ',
 '`GlassfishDB`.`Process`.`ProcessName`, `GlassfishDB`.`Project`.`ProjectName`, ',
 '`GlassfishDB`.`SOS`.`SOSName`, `GlassfishDB`.`SOSAction`.`SOSActionName`, `treffer`.`Source`, ',
 '`treffer`.`SourceMessageID`, `treffer`.`TargetMessageID`, `treffer`.`Target` from (select ',
 '`GlassfishDB`.`Message`.`MessageID`, `GlassfishDB`.`Message`.`MessageLastUpdate`, ',
 '`GlassfishDB`.`Message`.`MessageStatus`, `GlassfishDB`.`Message`.`ProcessID`, ',
 '`GlassfishDB`.`Message`.`SOSID`, `GlassfishDB`.`Message`.`SOSActionID`, `GlassfishDB`.`Message`.`Source`, ',
 '`GlassfishDB`.`Message`.`SourceMessageID`, `GlassfishDB`.`Message`.`TargetMessageID`, ',
 '`GlassfishDB`.`Message`.`Target` from `GlassfishDB`.`MessageProperty` as `mp1` join ',
 '`GlassfishDB`.`Message` on `GlassfishDB`.`Message`.`MessageID` = `mp1`.`MessageID` where ',
 '(`mp1`.`MessagePropertyName` = ''Converter.TransactionID'' and `mp1`.`MessagePropertyValue` = ', @w, ' and ',
 '`GlassfishDB`.`Message`.`MessageLastUpdate` >= ''2025-12-29 00:00:00'' and ',
 '`GlassfishDB`.`Message`.`MessageLastUpdate` <= ''2025-12-30 00:00:00'' and exists (select 1 as `one` from ',
 '`GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on ',
 '`GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where ',
 '(`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and ',
 '`GlassfishDB`.`ProjectMandant`.`MandantID` = ''NEXANS''))) group by `GlassfishDB`.`Message`.`MessageID` ',
 'order by `GlassfishDB`.`Message`.`MessageLastUpdate` desc, `GlassfishDB`.`Message`.`MessageID` desc fetch ',
 'next 51 rows only) as `treffer` left outer join `GlassfishDB`.`Process` on ',
 '`GlassfishDB`.`Process`.`ProcessID` = `treffer`.`ProcessID` left outer join `GlassfishDB`.`Project` on ',
 '`GlassfishDB`.`Project`.`ProjectID` = `GlassfishDB`.`Process`.`ProjectID` left outer join ',
 '`GlassfishDB`.`SOS` on `GlassfishDB`.`SOS`.`SOSID` = `treffer`.`SOSID` left outer join ',
 '`GlassfishDB`.`SOSAction` on (`GlassfishDB`.`SOSAction`.`SOSID` = `treffer`.`SOSID` and ',
 '`GlassfishDB`.`SOSAction`.`SOSActionID` = `treffer`.`SOSActionID`) order by `treffer`.`MessageLastUpdate` ',
 'desc, `treffer`.`MessageID` desc) AS gebaut ');
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
PREPARE qp FROM @q;
SELECT VARIABLE_VALUE INTO @h1 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY';
SELECT VARIABLE_VALUE INTO @h2 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT';
SELECT VARIABLE_VALUE INTO @h3 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT';
SELECT VARIABLE_VALUE INTO @h4 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE';
SELECT VARIABLE_VALUE INTO @h5 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS';
SELECT VARIABLE_VALUE INTO @h6 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND';
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d1;
SELECT (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY') - @h1 AS read_key,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT') - @h2 AS read_next,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT') - @h3 AS read_rnd_next,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE') - @h4 AS tmp_write,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS') - @h5 AS icp_attempts,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND') - @h6 AS read_rnd;
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
SELECT ROUND(@d1/1000,3) AS wanduhrLauf1, ROUND(@d2/1000,3) AS l2, ROUND(@d3/1000,3) AS l3, ROUND(@d4/1000,3) AS l4,
       ROUND(@d5/1000,3) AS l5, ROUND(@d6/1000,3) AS l6, ROUND(LEAST(@d2,@d3,@d4,@d5,@d6)/1000,3) AS wanduhrBesteVon5;
SELECT ROUND(@p1,3) AS profilLauf1, ROUND(@p2,3) AS p2, ROUND(@p3,3) AS p3, ROUND(@p4,3) AS p4,
       ROUND(@p5,3) AS p5, ROUND(@p6,3) AS p6, ROUND(LEAST(@p2,@p3,@p4,@p5,@p6),3) AS profilBesteVon5;

SELECT '=== EAV / NEXANS / Converter.TransactionID / 30T (Boesfall: haeufigster Wert im 30-Tage-Fenster) ===' AS marke;
SET max_statement_time = 60;
SELECT QUOTE(v) INTO @w FROM (
  SELECT p.MessagePropertyValue AS v, COUNT(*) AS c
    FROM GlassfishDB.Message m
    JOIN GlassfishDB.MessageProperty p
      ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Converter.TransactionID'
   WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00' AND m.MessageLastUpdate < '2025-12-30 00:00:00'
     AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
   GROUP BY p.MessagePropertyValue ORDER BY c DESC, v ASC LIMIT 1) x;
SELECT CHAR_LENGTH(@w) - 2 AS laengeDesPruefwerts_w;
SET max_statement_time = 10;
SET @q = CONCAT(
 'SELECT COUNT(*) AS zeilen, MAX(CHAR_LENGTH(ProcessName)) AS maxPnLaenge FROM (select ',
 '`treffer`.`MessageID`, `treffer`.`MessageLastUpdate`, `treffer`.`MessageStatus`, `treffer`.`ProcessID`, ',
 '`GlassfishDB`.`Process`.`ProcessName`, `GlassfishDB`.`Project`.`ProjectName`, ',
 '`GlassfishDB`.`SOS`.`SOSName`, `GlassfishDB`.`SOSAction`.`SOSActionName`, `treffer`.`Source`, ',
 '`treffer`.`SourceMessageID`, `treffer`.`TargetMessageID`, `treffer`.`Target` from (select ',
 '`GlassfishDB`.`Message`.`MessageID`, `GlassfishDB`.`Message`.`MessageLastUpdate`, ',
 '`GlassfishDB`.`Message`.`MessageStatus`, `GlassfishDB`.`Message`.`ProcessID`, ',
 '`GlassfishDB`.`Message`.`SOSID`, `GlassfishDB`.`Message`.`SOSActionID`, `GlassfishDB`.`Message`.`Source`, ',
 '`GlassfishDB`.`Message`.`SourceMessageID`, `GlassfishDB`.`Message`.`TargetMessageID`, ',
 '`GlassfishDB`.`Message`.`Target` from `GlassfishDB`.`MessageProperty` as `mp1` join ',
 '`GlassfishDB`.`Message` on `GlassfishDB`.`Message`.`MessageID` = `mp1`.`MessageID` where ',
 '(`mp1`.`MessagePropertyName` = ''Converter.TransactionID'' and `mp1`.`MessagePropertyValue` = ', @w, ' and ',
 '`GlassfishDB`.`Message`.`MessageLastUpdate` >= ''2025-11-30 00:00:00'' and ',
 '`GlassfishDB`.`Message`.`MessageLastUpdate` <= ''2025-12-30 00:00:00'' and exists (select 1 as `one` from ',
 '`GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on ',
 '`GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where ',
 '(`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and ',
 '`GlassfishDB`.`ProjectMandant`.`MandantID` = ''NEXANS''))) group by `GlassfishDB`.`Message`.`MessageID` ',
 'order by `GlassfishDB`.`Message`.`MessageLastUpdate` desc, `GlassfishDB`.`Message`.`MessageID` desc fetch ',
 'next 51 rows only) as `treffer` left outer join `GlassfishDB`.`Process` on ',
 '`GlassfishDB`.`Process`.`ProcessID` = `treffer`.`ProcessID` left outer join `GlassfishDB`.`Project` on ',
 '`GlassfishDB`.`Project`.`ProjectID` = `GlassfishDB`.`Process`.`ProjectID` left outer join ',
 '`GlassfishDB`.`SOS` on `GlassfishDB`.`SOS`.`SOSID` = `treffer`.`SOSID` left outer join ',
 '`GlassfishDB`.`SOSAction` on (`GlassfishDB`.`SOSAction`.`SOSID` = `treffer`.`SOSID` and ',
 '`GlassfishDB`.`SOSAction`.`SOSActionID` = `treffer`.`SOSActionID`) order by `treffer`.`MessageLastUpdate` ',
 'desc, `treffer`.`MessageID` desc) AS gebaut ');
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
PREPARE qp FROM @q;
SELECT VARIABLE_VALUE INTO @h1 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY';
SELECT VARIABLE_VALUE INTO @h2 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT';
SELECT VARIABLE_VALUE INTO @h3 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT';
SELECT VARIABLE_VALUE INTO @h4 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE';
SELECT VARIABLE_VALUE INTO @h5 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS';
SELECT VARIABLE_VALUE INTO @h6 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND';
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d1;
SELECT (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY') - @h1 AS read_key,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT') - @h2 AS read_next,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT') - @h3 AS read_rnd_next,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE') - @h4 AS tmp_write,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS') - @h5 AS icp_attempts,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND') - @h6 AS read_rnd;
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
SELECT ROUND(@d1/1000,3) AS wanduhrLauf1, ROUND(@d2/1000,3) AS l2, ROUND(@d3/1000,3) AS l3, ROUND(@d4/1000,3) AS l4,
       ROUND(@d5/1000,3) AS l5, ROUND(@d6/1000,3) AS l6, ROUND(LEAST(@d2,@d3,@d4,@d5,@d6)/1000,3) AS wanduhrBesteVon5;
SELECT ROUND(@p1,3) AS profilLauf1, ROUND(@p2,3) AS p2, ROUND(@p3,3) AS p3, ROUND(@p4,3) AS p4,
       ROUND(@p5,3) AS p5, ROUND(@p6,3) AS p6, ROUND(LEAST(@p2,@p3,@p4,@p5,@p6),3) AS profilBesteVon5;

SELECT '=== EAV / NEXANS / Converter.TransactionID / 1J (Boesfall: haeufigster Wert im 30-Tage-Fenster) ===' AS marke;
SET max_statement_time = 60;
SELECT QUOTE(v) INTO @w FROM (
  SELECT p.MessagePropertyValue AS v, COUNT(*) AS c
    FROM GlassfishDB.Message m
    JOIN GlassfishDB.MessageProperty p
      ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Converter.TransactionID'
   WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00' AND m.MessageLastUpdate < '2025-12-30 00:00:00'
     AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
   GROUP BY p.MessagePropertyValue ORDER BY c DESC, v ASC LIMIT 1) x;
SELECT CHAR_LENGTH(@w) - 2 AS laengeDesPruefwerts_w;
SET max_statement_time = 10;
SET @q = CONCAT(
 'SELECT COUNT(*) AS zeilen, MAX(CHAR_LENGTH(ProcessName)) AS maxPnLaenge FROM (select ',
 '`treffer`.`MessageID`, `treffer`.`MessageLastUpdate`, `treffer`.`MessageStatus`, `treffer`.`ProcessID`, ',
 '`GlassfishDB`.`Process`.`ProcessName`, `GlassfishDB`.`Project`.`ProjectName`, ',
 '`GlassfishDB`.`SOS`.`SOSName`, `GlassfishDB`.`SOSAction`.`SOSActionName`, `treffer`.`Source`, ',
 '`treffer`.`SourceMessageID`, `treffer`.`TargetMessageID`, `treffer`.`Target` from (select ',
 '`GlassfishDB`.`Message`.`MessageID`, `GlassfishDB`.`Message`.`MessageLastUpdate`, ',
 '`GlassfishDB`.`Message`.`MessageStatus`, `GlassfishDB`.`Message`.`ProcessID`, ',
 '`GlassfishDB`.`Message`.`SOSID`, `GlassfishDB`.`Message`.`SOSActionID`, `GlassfishDB`.`Message`.`Source`, ',
 '`GlassfishDB`.`Message`.`SourceMessageID`, `GlassfishDB`.`Message`.`TargetMessageID`, ',
 '`GlassfishDB`.`Message`.`Target` from `GlassfishDB`.`MessageProperty` as `mp1` join ',
 '`GlassfishDB`.`Message` on `GlassfishDB`.`Message`.`MessageID` = `mp1`.`MessageID` where ',
 '(`mp1`.`MessagePropertyName` = ''Converter.TransactionID'' and `mp1`.`MessagePropertyValue` = ', @w, ' and ',
 '`GlassfishDB`.`Message`.`MessageLastUpdate` >= ''2024-12-30 00:00:00'' and ',
 '`GlassfishDB`.`Message`.`MessageLastUpdate` <= ''2025-12-30 00:00:00'' and exists (select 1 as `one` from ',
 '`GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on ',
 '`GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where ',
 '(`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and ',
 '`GlassfishDB`.`ProjectMandant`.`MandantID` = ''NEXANS''))) group by `GlassfishDB`.`Message`.`MessageID` ',
 'order by `GlassfishDB`.`Message`.`MessageLastUpdate` desc, `GlassfishDB`.`Message`.`MessageID` desc fetch ',
 'next 51 rows only) as `treffer` left outer join `GlassfishDB`.`Process` on ',
 '`GlassfishDB`.`Process`.`ProcessID` = `treffer`.`ProcessID` left outer join `GlassfishDB`.`Project` on ',
 '`GlassfishDB`.`Project`.`ProjectID` = `GlassfishDB`.`Process`.`ProjectID` left outer join ',
 '`GlassfishDB`.`SOS` on `GlassfishDB`.`SOS`.`SOSID` = `treffer`.`SOSID` left outer join ',
 '`GlassfishDB`.`SOSAction` on (`GlassfishDB`.`SOSAction`.`SOSID` = `treffer`.`SOSID` and ',
 '`GlassfishDB`.`SOSAction`.`SOSActionID` = `treffer`.`SOSActionID`) order by `treffer`.`MessageLastUpdate` ',
 'desc, `treffer`.`MessageID` desc) AS gebaut ');
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
PREPARE qp FROM @q;
SELECT VARIABLE_VALUE INTO @h1 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY';
SELECT VARIABLE_VALUE INTO @h2 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT';
SELECT VARIABLE_VALUE INTO @h3 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT';
SELECT VARIABLE_VALUE INTO @h4 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE';
SELECT VARIABLE_VALUE INTO @h5 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS';
SELECT VARIABLE_VALUE INTO @h6 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND';
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d1;
SELECT (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY') - @h1 AS read_key,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT') - @h2 AS read_next,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT') - @h3 AS read_rnd_next,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE') - @h4 AS tmp_write,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS') - @h5 AS icp_attempts,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND') - @h6 AS read_rnd;
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
SELECT ROUND(@d1/1000,3) AS wanduhrLauf1, ROUND(@d2/1000,3) AS l2, ROUND(@d3/1000,3) AS l3, ROUND(@d4/1000,3) AS l4,
       ROUND(@d5/1000,3) AS l5, ROUND(@d6/1000,3) AS l6, ROUND(LEAST(@d2,@d3,@d4,@d5,@d6)/1000,3) AS wanduhrBesteVon5;
SELECT ROUND(@p1,3) AS profilLauf1, ROUND(@p2,3) AS p2, ROUND(@p3,3) AS p3, ROUND(@p4,3) AS p4,
       ROUND(@p5,3) AS p5, ROUND(@p6,3) AS p6, ROUND(LEAST(@p2,@p3,@p4,@p5,@p6),3) AS profilBesteVon5;

SELECT '=== EAV / NEXANS / Message.DestinationFilename / 24h (Boesfall: haeufigster Wert im 30-Tage-Fenster) ===' AS marke;
SET max_statement_time = 60;
SELECT QUOTE(v) INTO @w FROM (
  SELECT p.MessagePropertyValue AS v, COUNT(*) AS c
    FROM GlassfishDB.Message m
    JOIN GlassfishDB.MessageProperty p
      ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.DestinationFilename'
   WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00' AND m.MessageLastUpdate < '2025-12-30 00:00:00'
     AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
   GROUP BY p.MessagePropertyValue ORDER BY c DESC, v ASC LIMIT 1) x;
SELECT CHAR_LENGTH(@w) - 2 AS laengeDesPruefwerts_w;
SET max_statement_time = 10;
SET @q = CONCAT(
 'SELECT COUNT(*) AS zeilen, MAX(CHAR_LENGTH(ProcessName)) AS maxPnLaenge FROM (select ',
 '`treffer`.`MessageID`, `treffer`.`MessageLastUpdate`, `treffer`.`MessageStatus`, `treffer`.`ProcessID`, ',
 '`GlassfishDB`.`Process`.`ProcessName`, `GlassfishDB`.`Project`.`ProjectName`, ',
 '`GlassfishDB`.`SOS`.`SOSName`, `GlassfishDB`.`SOSAction`.`SOSActionName`, `treffer`.`Source`, ',
 '`treffer`.`SourceMessageID`, `treffer`.`TargetMessageID`, `treffer`.`Target` from (select ',
 '`GlassfishDB`.`Message`.`MessageID`, `GlassfishDB`.`Message`.`MessageLastUpdate`, ',
 '`GlassfishDB`.`Message`.`MessageStatus`, `GlassfishDB`.`Message`.`ProcessID`, ',
 '`GlassfishDB`.`Message`.`SOSID`, `GlassfishDB`.`Message`.`SOSActionID`, `GlassfishDB`.`Message`.`Source`, ',
 '`GlassfishDB`.`Message`.`SourceMessageID`, `GlassfishDB`.`Message`.`TargetMessageID`, ',
 '`GlassfishDB`.`Message`.`Target` from `GlassfishDB`.`MessageProperty` as `mp1` join ',
 '`GlassfishDB`.`Message` on `GlassfishDB`.`Message`.`MessageID` = `mp1`.`MessageID` where ',
 '(`mp1`.`MessagePropertyName` = ''Message.DestinationFilename'' and `mp1`.`MessagePropertyValue` = ', @w, ' ',
 'and `GlassfishDB`.`Message`.`MessageLastUpdate` >= ''2025-12-29 00:00:00'' and ',
 '`GlassfishDB`.`Message`.`MessageLastUpdate` <= ''2025-12-30 00:00:00'' and exists (select 1 as `one` from ',
 '`GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on ',
 '`GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where ',
 '(`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and ',
 '`GlassfishDB`.`ProjectMandant`.`MandantID` = ''NEXANS''))) group by `GlassfishDB`.`Message`.`MessageID` ',
 'order by `GlassfishDB`.`Message`.`MessageLastUpdate` desc, `GlassfishDB`.`Message`.`MessageID` desc fetch ',
 'next 51 rows only) as `treffer` left outer join `GlassfishDB`.`Process` on ',
 '`GlassfishDB`.`Process`.`ProcessID` = `treffer`.`ProcessID` left outer join `GlassfishDB`.`Project` on ',
 '`GlassfishDB`.`Project`.`ProjectID` = `GlassfishDB`.`Process`.`ProjectID` left outer join ',
 '`GlassfishDB`.`SOS` on `GlassfishDB`.`SOS`.`SOSID` = `treffer`.`SOSID` left outer join ',
 '`GlassfishDB`.`SOSAction` on (`GlassfishDB`.`SOSAction`.`SOSID` = `treffer`.`SOSID` and ',
 '`GlassfishDB`.`SOSAction`.`SOSActionID` = `treffer`.`SOSActionID`) order by `treffer`.`MessageLastUpdate` ',
 'desc, `treffer`.`MessageID` desc) AS gebaut ');
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
PREPARE qp FROM @q;
SELECT VARIABLE_VALUE INTO @h1 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY';
SELECT VARIABLE_VALUE INTO @h2 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT';
SELECT VARIABLE_VALUE INTO @h3 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT';
SELECT VARIABLE_VALUE INTO @h4 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE';
SELECT VARIABLE_VALUE INTO @h5 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS';
SELECT VARIABLE_VALUE INTO @h6 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND';
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d1;
SELECT (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY') - @h1 AS read_key,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT') - @h2 AS read_next,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT') - @h3 AS read_rnd_next,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE') - @h4 AS tmp_write,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS') - @h5 AS icp_attempts,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND') - @h6 AS read_rnd;
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
SELECT ROUND(@d1/1000,3) AS wanduhrLauf1, ROUND(@d2/1000,3) AS l2, ROUND(@d3/1000,3) AS l3, ROUND(@d4/1000,3) AS l4,
       ROUND(@d5/1000,3) AS l5, ROUND(@d6/1000,3) AS l6, ROUND(LEAST(@d2,@d3,@d4,@d5,@d6)/1000,3) AS wanduhrBesteVon5;
SELECT ROUND(@p1,3) AS profilLauf1, ROUND(@p2,3) AS p2, ROUND(@p3,3) AS p3, ROUND(@p4,3) AS p4,
       ROUND(@p5,3) AS p5, ROUND(@p6,3) AS p6, ROUND(LEAST(@p2,@p3,@p4,@p5,@p6),3) AS profilBesteVon5;

SELECT '=== EAV / NEXANS / Message.DestinationFilename / 30T (Boesfall: haeufigster Wert im 30-Tage-Fenster) ===' AS marke;
SET max_statement_time = 60;
SELECT QUOTE(v) INTO @w FROM (
  SELECT p.MessagePropertyValue AS v, COUNT(*) AS c
    FROM GlassfishDB.Message m
    JOIN GlassfishDB.MessageProperty p
      ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.DestinationFilename'
   WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00' AND m.MessageLastUpdate < '2025-12-30 00:00:00'
     AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
   GROUP BY p.MessagePropertyValue ORDER BY c DESC, v ASC LIMIT 1) x;
SELECT CHAR_LENGTH(@w) - 2 AS laengeDesPruefwerts_w;
SET max_statement_time = 10;
SET @q = CONCAT(
 'SELECT COUNT(*) AS zeilen, MAX(CHAR_LENGTH(ProcessName)) AS maxPnLaenge FROM (select ',
 '`treffer`.`MessageID`, `treffer`.`MessageLastUpdate`, `treffer`.`MessageStatus`, `treffer`.`ProcessID`, ',
 '`GlassfishDB`.`Process`.`ProcessName`, `GlassfishDB`.`Project`.`ProjectName`, ',
 '`GlassfishDB`.`SOS`.`SOSName`, `GlassfishDB`.`SOSAction`.`SOSActionName`, `treffer`.`Source`, ',
 '`treffer`.`SourceMessageID`, `treffer`.`TargetMessageID`, `treffer`.`Target` from (select ',
 '`GlassfishDB`.`Message`.`MessageID`, `GlassfishDB`.`Message`.`MessageLastUpdate`, ',
 '`GlassfishDB`.`Message`.`MessageStatus`, `GlassfishDB`.`Message`.`ProcessID`, ',
 '`GlassfishDB`.`Message`.`SOSID`, `GlassfishDB`.`Message`.`SOSActionID`, `GlassfishDB`.`Message`.`Source`, ',
 '`GlassfishDB`.`Message`.`SourceMessageID`, `GlassfishDB`.`Message`.`TargetMessageID`, ',
 '`GlassfishDB`.`Message`.`Target` from `GlassfishDB`.`MessageProperty` as `mp1` join ',
 '`GlassfishDB`.`Message` on `GlassfishDB`.`Message`.`MessageID` = `mp1`.`MessageID` where ',
 '(`mp1`.`MessagePropertyName` = ''Message.DestinationFilename'' and `mp1`.`MessagePropertyValue` = ', @w, ' ',
 'and `GlassfishDB`.`Message`.`MessageLastUpdate` >= ''2025-11-30 00:00:00'' and ',
 '`GlassfishDB`.`Message`.`MessageLastUpdate` <= ''2025-12-30 00:00:00'' and exists (select 1 as `one` from ',
 '`GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on ',
 '`GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where ',
 '(`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and ',
 '`GlassfishDB`.`ProjectMandant`.`MandantID` = ''NEXANS''))) group by `GlassfishDB`.`Message`.`MessageID` ',
 'order by `GlassfishDB`.`Message`.`MessageLastUpdate` desc, `GlassfishDB`.`Message`.`MessageID` desc fetch ',
 'next 51 rows only) as `treffer` left outer join `GlassfishDB`.`Process` on ',
 '`GlassfishDB`.`Process`.`ProcessID` = `treffer`.`ProcessID` left outer join `GlassfishDB`.`Project` on ',
 '`GlassfishDB`.`Project`.`ProjectID` = `GlassfishDB`.`Process`.`ProjectID` left outer join ',
 '`GlassfishDB`.`SOS` on `GlassfishDB`.`SOS`.`SOSID` = `treffer`.`SOSID` left outer join ',
 '`GlassfishDB`.`SOSAction` on (`GlassfishDB`.`SOSAction`.`SOSID` = `treffer`.`SOSID` and ',
 '`GlassfishDB`.`SOSAction`.`SOSActionID` = `treffer`.`SOSActionID`) order by `treffer`.`MessageLastUpdate` ',
 'desc, `treffer`.`MessageID` desc) AS gebaut ');
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
PREPARE qp FROM @q;
SELECT VARIABLE_VALUE INTO @h1 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY';
SELECT VARIABLE_VALUE INTO @h2 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT';
SELECT VARIABLE_VALUE INTO @h3 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT';
SELECT VARIABLE_VALUE INTO @h4 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE';
SELECT VARIABLE_VALUE INTO @h5 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS';
SELECT VARIABLE_VALUE INTO @h6 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND';
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d1;
SELECT (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY') - @h1 AS read_key,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT') - @h2 AS read_next,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT') - @h3 AS read_rnd_next,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE') - @h4 AS tmp_write,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS') - @h5 AS icp_attempts,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND') - @h6 AS read_rnd;
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
SELECT ROUND(@d1/1000,3) AS wanduhrLauf1, ROUND(@d2/1000,3) AS l2, ROUND(@d3/1000,3) AS l3, ROUND(@d4/1000,3) AS l4,
       ROUND(@d5/1000,3) AS l5, ROUND(@d6/1000,3) AS l6, ROUND(LEAST(@d2,@d3,@d4,@d5,@d6)/1000,3) AS wanduhrBesteVon5;
SELECT ROUND(@p1,3) AS profilLauf1, ROUND(@p2,3) AS p2, ROUND(@p3,3) AS p3, ROUND(@p4,3) AS p4,
       ROUND(@p5,3) AS p5, ROUND(@p6,3) AS p6, ROUND(LEAST(@p2,@p3,@p4,@p5,@p6),3) AS profilBesteVon5;

SELECT '=== EAV / NEXANS / Message.DestinationFilename / 1J (Boesfall: haeufigster Wert im 30-Tage-Fenster) ===' AS marke;
SET max_statement_time = 60;
SELECT QUOTE(v) INTO @w FROM (
  SELECT p.MessagePropertyValue AS v, COUNT(*) AS c
    FROM GlassfishDB.Message m
    JOIN GlassfishDB.MessageProperty p
      ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.DestinationFilename'
   WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00' AND m.MessageLastUpdate < '2025-12-30 00:00:00'
     AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
   GROUP BY p.MessagePropertyValue ORDER BY c DESC, v ASC LIMIT 1) x;
SELECT CHAR_LENGTH(@w) - 2 AS laengeDesPruefwerts_w;
SET max_statement_time = 10;
SET @q = CONCAT(
 'SELECT COUNT(*) AS zeilen, MAX(CHAR_LENGTH(ProcessName)) AS maxPnLaenge FROM (select ',
 '`treffer`.`MessageID`, `treffer`.`MessageLastUpdate`, `treffer`.`MessageStatus`, `treffer`.`ProcessID`, ',
 '`GlassfishDB`.`Process`.`ProcessName`, `GlassfishDB`.`Project`.`ProjectName`, ',
 '`GlassfishDB`.`SOS`.`SOSName`, `GlassfishDB`.`SOSAction`.`SOSActionName`, `treffer`.`Source`, ',
 '`treffer`.`SourceMessageID`, `treffer`.`TargetMessageID`, `treffer`.`Target` from (select ',
 '`GlassfishDB`.`Message`.`MessageID`, `GlassfishDB`.`Message`.`MessageLastUpdate`, ',
 '`GlassfishDB`.`Message`.`MessageStatus`, `GlassfishDB`.`Message`.`ProcessID`, ',
 '`GlassfishDB`.`Message`.`SOSID`, `GlassfishDB`.`Message`.`SOSActionID`, `GlassfishDB`.`Message`.`Source`, ',
 '`GlassfishDB`.`Message`.`SourceMessageID`, `GlassfishDB`.`Message`.`TargetMessageID`, ',
 '`GlassfishDB`.`Message`.`Target` from `GlassfishDB`.`MessageProperty` as `mp1` join ',
 '`GlassfishDB`.`Message` on `GlassfishDB`.`Message`.`MessageID` = `mp1`.`MessageID` where ',
 '(`mp1`.`MessagePropertyName` = ''Message.DestinationFilename'' and `mp1`.`MessagePropertyValue` = ', @w, ' ',
 'and `GlassfishDB`.`Message`.`MessageLastUpdate` >= ''2024-12-30 00:00:00'' and ',
 '`GlassfishDB`.`Message`.`MessageLastUpdate` <= ''2025-12-30 00:00:00'' and exists (select 1 as `one` from ',
 '`GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on ',
 '`GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where ',
 '(`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and ',
 '`GlassfishDB`.`ProjectMandant`.`MandantID` = ''NEXANS''))) group by `GlassfishDB`.`Message`.`MessageID` ',
 'order by `GlassfishDB`.`Message`.`MessageLastUpdate` desc, `GlassfishDB`.`Message`.`MessageID` desc fetch ',
 'next 51 rows only) as `treffer` left outer join `GlassfishDB`.`Process` on ',
 '`GlassfishDB`.`Process`.`ProcessID` = `treffer`.`ProcessID` left outer join `GlassfishDB`.`Project` on ',
 '`GlassfishDB`.`Project`.`ProjectID` = `GlassfishDB`.`Process`.`ProjectID` left outer join ',
 '`GlassfishDB`.`SOS` on `GlassfishDB`.`SOS`.`SOSID` = `treffer`.`SOSID` left outer join ',
 '`GlassfishDB`.`SOSAction` on (`GlassfishDB`.`SOSAction`.`SOSID` = `treffer`.`SOSID` and ',
 '`GlassfishDB`.`SOSAction`.`SOSActionID` = `treffer`.`SOSActionID`) order by `treffer`.`MessageLastUpdate` ',
 'desc, `treffer`.`MessageID` desc) AS gebaut ');
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
PREPARE qp FROM @q;
SELECT VARIABLE_VALUE INTO @h1 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY';
SELECT VARIABLE_VALUE INTO @h2 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT';
SELECT VARIABLE_VALUE INTO @h3 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT';
SELECT VARIABLE_VALUE INTO @h4 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE';
SELECT VARIABLE_VALUE INTO @h5 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS';
SELECT VARIABLE_VALUE INTO @h6 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND';
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d1;
SELECT (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY') - @h1 AS read_key,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT') - @h2 AS read_next,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT') - @h3 AS read_rnd_next,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE') - @h4 AS tmp_write,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS') - @h5 AS icp_attempts,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND') - @h6 AS read_rnd;
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
SELECT ROUND(@d1/1000,3) AS wanduhrLauf1, ROUND(@d2/1000,3) AS l2, ROUND(@d3/1000,3) AS l3, ROUND(@d4/1000,3) AS l4,
       ROUND(@d5/1000,3) AS l5, ROUND(@d6/1000,3) AS l6, ROUND(LEAST(@d2,@d3,@d4,@d5,@d6)/1000,3) AS wanduhrBesteVon5;
SELECT ROUND(@p1,3) AS profilLauf1, ROUND(@p2,3) AS p2, ROUND(@p3,3) AS p3, ROUND(@p4,3) AS p4,
       ROUND(@p5,3) AS p5, ROUND(@p6,3) AS p6, ROUND(LEAST(@p2,@p3,@p4,@p5,@p6),3) AS profilBesteVon5;

SELECT '=== EAV / NEXANS / Message.GUID / 24h (Boesfall: haeufigster Wert im 30-Tage-Fenster) ===' AS marke;
SET max_statement_time = 60;
SELECT QUOTE(v) INTO @w FROM (
  SELECT p.MessagePropertyValue AS v, COUNT(*) AS c
    FROM GlassfishDB.Message m
    JOIN GlassfishDB.MessageProperty p
      ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.GUID'
   WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00' AND m.MessageLastUpdate < '2025-12-30 00:00:00'
     AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
   GROUP BY p.MessagePropertyValue ORDER BY c DESC, v ASC LIMIT 1) x;
SELECT CHAR_LENGTH(@w) - 2 AS laengeDesPruefwerts_w;
SET max_statement_time = 10;
SET @q = CONCAT(
 'SELECT COUNT(*) AS zeilen, MAX(CHAR_LENGTH(ProcessName)) AS maxPnLaenge FROM (select ',
 '`treffer`.`MessageID`, `treffer`.`MessageLastUpdate`, `treffer`.`MessageStatus`, `treffer`.`ProcessID`, ',
 '`GlassfishDB`.`Process`.`ProcessName`, `GlassfishDB`.`Project`.`ProjectName`, ',
 '`GlassfishDB`.`SOS`.`SOSName`, `GlassfishDB`.`SOSAction`.`SOSActionName`, `treffer`.`Source`, ',
 '`treffer`.`SourceMessageID`, `treffer`.`TargetMessageID`, `treffer`.`Target` from (select ',
 '`GlassfishDB`.`Message`.`MessageID`, `GlassfishDB`.`Message`.`MessageLastUpdate`, ',
 '`GlassfishDB`.`Message`.`MessageStatus`, `GlassfishDB`.`Message`.`ProcessID`, ',
 '`GlassfishDB`.`Message`.`SOSID`, `GlassfishDB`.`Message`.`SOSActionID`, `GlassfishDB`.`Message`.`Source`, ',
 '`GlassfishDB`.`Message`.`SourceMessageID`, `GlassfishDB`.`Message`.`TargetMessageID`, ',
 '`GlassfishDB`.`Message`.`Target` from `GlassfishDB`.`MessageProperty` as `mp1` join ',
 '`GlassfishDB`.`Message` on `GlassfishDB`.`Message`.`MessageID` = `mp1`.`MessageID` where ',
 '(`mp1`.`MessagePropertyName` = ''Message.GUID'' and `mp1`.`MessagePropertyValue` = ', @w, ' and ',
 '`GlassfishDB`.`Message`.`MessageLastUpdate` >= ''2025-12-29 00:00:00'' and ',
 '`GlassfishDB`.`Message`.`MessageLastUpdate` <= ''2025-12-30 00:00:00'' and exists (select 1 as `one` from ',
 '`GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on ',
 '`GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where ',
 '(`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and ',
 '`GlassfishDB`.`ProjectMandant`.`MandantID` = ''NEXANS''))) group by `GlassfishDB`.`Message`.`MessageID` ',
 'order by `GlassfishDB`.`Message`.`MessageLastUpdate` desc, `GlassfishDB`.`Message`.`MessageID` desc fetch ',
 'next 51 rows only) as `treffer` left outer join `GlassfishDB`.`Process` on ',
 '`GlassfishDB`.`Process`.`ProcessID` = `treffer`.`ProcessID` left outer join `GlassfishDB`.`Project` on ',
 '`GlassfishDB`.`Project`.`ProjectID` = `GlassfishDB`.`Process`.`ProjectID` left outer join ',
 '`GlassfishDB`.`SOS` on `GlassfishDB`.`SOS`.`SOSID` = `treffer`.`SOSID` left outer join ',
 '`GlassfishDB`.`SOSAction` on (`GlassfishDB`.`SOSAction`.`SOSID` = `treffer`.`SOSID` and ',
 '`GlassfishDB`.`SOSAction`.`SOSActionID` = `treffer`.`SOSActionID`) order by `treffer`.`MessageLastUpdate` ',
 'desc, `treffer`.`MessageID` desc) AS gebaut ');
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
PREPARE qp FROM @q;
SELECT VARIABLE_VALUE INTO @h1 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY';
SELECT VARIABLE_VALUE INTO @h2 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT';
SELECT VARIABLE_VALUE INTO @h3 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT';
SELECT VARIABLE_VALUE INTO @h4 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE';
SELECT VARIABLE_VALUE INTO @h5 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS';
SELECT VARIABLE_VALUE INTO @h6 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND';
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d1;
SELECT (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY') - @h1 AS read_key,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT') - @h2 AS read_next,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT') - @h3 AS read_rnd_next,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE') - @h4 AS tmp_write,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS') - @h5 AS icp_attempts,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND') - @h6 AS read_rnd;
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
SELECT ROUND(@d1/1000,3) AS wanduhrLauf1, ROUND(@d2/1000,3) AS l2, ROUND(@d3/1000,3) AS l3, ROUND(@d4/1000,3) AS l4,
       ROUND(@d5/1000,3) AS l5, ROUND(@d6/1000,3) AS l6, ROUND(LEAST(@d2,@d3,@d4,@d5,@d6)/1000,3) AS wanduhrBesteVon5;
SELECT ROUND(@p1,3) AS profilLauf1, ROUND(@p2,3) AS p2, ROUND(@p3,3) AS p3, ROUND(@p4,3) AS p4,
       ROUND(@p5,3) AS p5, ROUND(@p6,3) AS p6, ROUND(LEAST(@p2,@p3,@p4,@p5,@p6),3) AS profilBesteVon5;

SELECT '=== EAV / NEXANS / Message.GUID / 30T (Boesfall: haeufigster Wert im 30-Tage-Fenster) ===' AS marke;
SET max_statement_time = 60;
SELECT QUOTE(v) INTO @w FROM (
  SELECT p.MessagePropertyValue AS v, COUNT(*) AS c
    FROM GlassfishDB.Message m
    JOIN GlassfishDB.MessageProperty p
      ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.GUID'
   WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00' AND m.MessageLastUpdate < '2025-12-30 00:00:00'
     AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
   GROUP BY p.MessagePropertyValue ORDER BY c DESC, v ASC LIMIT 1) x;
SELECT CHAR_LENGTH(@w) - 2 AS laengeDesPruefwerts_w;
SET max_statement_time = 10;
SET @q = CONCAT(
 'SELECT COUNT(*) AS zeilen, MAX(CHAR_LENGTH(ProcessName)) AS maxPnLaenge FROM (select ',
 '`treffer`.`MessageID`, `treffer`.`MessageLastUpdate`, `treffer`.`MessageStatus`, `treffer`.`ProcessID`, ',
 '`GlassfishDB`.`Process`.`ProcessName`, `GlassfishDB`.`Project`.`ProjectName`, ',
 '`GlassfishDB`.`SOS`.`SOSName`, `GlassfishDB`.`SOSAction`.`SOSActionName`, `treffer`.`Source`, ',
 '`treffer`.`SourceMessageID`, `treffer`.`TargetMessageID`, `treffer`.`Target` from (select ',
 '`GlassfishDB`.`Message`.`MessageID`, `GlassfishDB`.`Message`.`MessageLastUpdate`, ',
 '`GlassfishDB`.`Message`.`MessageStatus`, `GlassfishDB`.`Message`.`ProcessID`, ',
 '`GlassfishDB`.`Message`.`SOSID`, `GlassfishDB`.`Message`.`SOSActionID`, `GlassfishDB`.`Message`.`Source`, ',
 '`GlassfishDB`.`Message`.`SourceMessageID`, `GlassfishDB`.`Message`.`TargetMessageID`, ',
 '`GlassfishDB`.`Message`.`Target` from `GlassfishDB`.`MessageProperty` as `mp1` join ',
 '`GlassfishDB`.`Message` on `GlassfishDB`.`Message`.`MessageID` = `mp1`.`MessageID` where ',
 '(`mp1`.`MessagePropertyName` = ''Message.GUID'' and `mp1`.`MessagePropertyValue` = ', @w, ' and ',
 '`GlassfishDB`.`Message`.`MessageLastUpdate` >= ''2025-11-30 00:00:00'' and ',
 '`GlassfishDB`.`Message`.`MessageLastUpdate` <= ''2025-12-30 00:00:00'' and exists (select 1 as `one` from ',
 '`GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on ',
 '`GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where ',
 '(`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and ',
 '`GlassfishDB`.`ProjectMandant`.`MandantID` = ''NEXANS''))) group by `GlassfishDB`.`Message`.`MessageID` ',
 'order by `GlassfishDB`.`Message`.`MessageLastUpdate` desc, `GlassfishDB`.`Message`.`MessageID` desc fetch ',
 'next 51 rows only) as `treffer` left outer join `GlassfishDB`.`Process` on ',
 '`GlassfishDB`.`Process`.`ProcessID` = `treffer`.`ProcessID` left outer join `GlassfishDB`.`Project` on ',
 '`GlassfishDB`.`Project`.`ProjectID` = `GlassfishDB`.`Process`.`ProjectID` left outer join ',
 '`GlassfishDB`.`SOS` on `GlassfishDB`.`SOS`.`SOSID` = `treffer`.`SOSID` left outer join ',
 '`GlassfishDB`.`SOSAction` on (`GlassfishDB`.`SOSAction`.`SOSID` = `treffer`.`SOSID` and ',
 '`GlassfishDB`.`SOSAction`.`SOSActionID` = `treffer`.`SOSActionID`) order by `treffer`.`MessageLastUpdate` ',
 'desc, `treffer`.`MessageID` desc) AS gebaut ');
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
PREPARE qp FROM @q;
SELECT VARIABLE_VALUE INTO @h1 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY';
SELECT VARIABLE_VALUE INTO @h2 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT';
SELECT VARIABLE_VALUE INTO @h3 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT';
SELECT VARIABLE_VALUE INTO @h4 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE';
SELECT VARIABLE_VALUE INTO @h5 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS';
SELECT VARIABLE_VALUE INTO @h6 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND';
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d1;
SELECT (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY') - @h1 AS read_key,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT') - @h2 AS read_next,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT') - @h3 AS read_rnd_next,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE') - @h4 AS tmp_write,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS') - @h5 AS icp_attempts,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND') - @h6 AS read_rnd;
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
SELECT ROUND(@d1/1000,3) AS wanduhrLauf1, ROUND(@d2/1000,3) AS l2, ROUND(@d3/1000,3) AS l3, ROUND(@d4/1000,3) AS l4,
       ROUND(@d5/1000,3) AS l5, ROUND(@d6/1000,3) AS l6, ROUND(LEAST(@d2,@d3,@d4,@d5,@d6)/1000,3) AS wanduhrBesteVon5;
SELECT ROUND(@p1,3) AS profilLauf1, ROUND(@p2,3) AS p2, ROUND(@p3,3) AS p3, ROUND(@p4,3) AS p4,
       ROUND(@p5,3) AS p5, ROUND(@p6,3) AS p6, ROUND(LEAST(@p2,@p3,@p4,@p5,@p6),3) AS profilBesteVon5;

SELECT '=== EAV / NEXANS / Message.GUID / 1J (Boesfall: haeufigster Wert im 30-Tage-Fenster) ===' AS marke;
SET max_statement_time = 60;
SELECT QUOTE(v) INTO @w FROM (
  SELECT p.MessagePropertyValue AS v, COUNT(*) AS c
    FROM GlassfishDB.Message m
    JOIN GlassfishDB.MessageProperty p
      ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.GUID'
   WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00' AND m.MessageLastUpdate < '2025-12-30 00:00:00'
     AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
   GROUP BY p.MessagePropertyValue ORDER BY c DESC, v ASC LIMIT 1) x;
SELECT CHAR_LENGTH(@w) - 2 AS laengeDesPruefwerts_w;
SET max_statement_time = 10;
SET @q = CONCAT(
 'SELECT COUNT(*) AS zeilen, MAX(CHAR_LENGTH(ProcessName)) AS maxPnLaenge FROM (select ',
 '`treffer`.`MessageID`, `treffer`.`MessageLastUpdate`, `treffer`.`MessageStatus`, `treffer`.`ProcessID`, ',
 '`GlassfishDB`.`Process`.`ProcessName`, `GlassfishDB`.`Project`.`ProjectName`, ',
 '`GlassfishDB`.`SOS`.`SOSName`, `GlassfishDB`.`SOSAction`.`SOSActionName`, `treffer`.`Source`, ',
 '`treffer`.`SourceMessageID`, `treffer`.`TargetMessageID`, `treffer`.`Target` from (select ',
 '`GlassfishDB`.`Message`.`MessageID`, `GlassfishDB`.`Message`.`MessageLastUpdate`, ',
 '`GlassfishDB`.`Message`.`MessageStatus`, `GlassfishDB`.`Message`.`ProcessID`, ',
 '`GlassfishDB`.`Message`.`SOSID`, `GlassfishDB`.`Message`.`SOSActionID`, `GlassfishDB`.`Message`.`Source`, ',
 '`GlassfishDB`.`Message`.`SourceMessageID`, `GlassfishDB`.`Message`.`TargetMessageID`, ',
 '`GlassfishDB`.`Message`.`Target` from `GlassfishDB`.`MessageProperty` as `mp1` join ',
 '`GlassfishDB`.`Message` on `GlassfishDB`.`Message`.`MessageID` = `mp1`.`MessageID` where ',
 '(`mp1`.`MessagePropertyName` = ''Message.GUID'' and `mp1`.`MessagePropertyValue` = ', @w, ' and ',
 '`GlassfishDB`.`Message`.`MessageLastUpdate` >= ''2024-12-30 00:00:00'' and ',
 '`GlassfishDB`.`Message`.`MessageLastUpdate` <= ''2025-12-30 00:00:00'' and exists (select 1 as `one` from ',
 '`GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on ',
 '`GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where ',
 '(`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and ',
 '`GlassfishDB`.`ProjectMandant`.`MandantID` = ''NEXANS''))) group by `GlassfishDB`.`Message`.`MessageID` ',
 'order by `GlassfishDB`.`Message`.`MessageLastUpdate` desc, `GlassfishDB`.`Message`.`MessageID` desc fetch ',
 'next 51 rows only) as `treffer` left outer join `GlassfishDB`.`Process` on ',
 '`GlassfishDB`.`Process`.`ProcessID` = `treffer`.`ProcessID` left outer join `GlassfishDB`.`Project` on ',
 '`GlassfishDB`.`Project`.`ProjectID` = `GlassfishDB`.`Process`.`ProjectID` left outer join ',
 '`GlassfishDB`.`SOS` on `GlassfishDB`.`SOS`.`SOSID` = `treffer`.`SOSID` left outer join ',
 '`GlassfishDB`.`SOSAction` on (`GlassfishDB`.`SOSAction`.`SOSID` = `treffer`.`SOSID` and ',
 '`GlassfishDB`.`SOSAction`.`SOSActionID` = `treffer`.`SOSActionID`) order by `treffer`.`MessageLastUpdate` ',
 'desc, `treffer`.`MessageID` desc) AS gebaut ');
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
PREPARE qp FROM @q;
SELECT VARIABLE_VALUE INTO @h1 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY';
SELECT VARIABLE_VALUE INTO @h2 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT';
SELECT VARIABLE_VALUE INTO @h3 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT';
SELECT VARIABLE_VALUE INTO @h4 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE';
SELECT VARIABLE_VALUE INTO @h5 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS';
SELECT VARIABLE_VALUE INTO @h6 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND';
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d1;
SELECT (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY') - @h1 AS read_key,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT') - @h2 AS read_next,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT') - @h3 AS read_rnd_next,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE') - @h4 AS tmp_write,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS') - @h5 AS icp_attempts,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND') - @h6 AS read_rnd;
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
SELECT ROUND(@d1/1000,3) AS wanduhrLauf1, ROUND(@d2/1000,3) AS l2, ROUND(@d3/1000,3) AS l3, ROUND(@d4/1000,3) AS l4,
       ROUND(@d5/1000,3) AS l5, ROUND(@d6/1000,3) AS l6, ROUND(LEAST(@d2,@d3,@d4,@d5,@d6)/1000,3) AS wanduhrBesteVon5;
SELECT ROUND(@p1,3) AS profilLauf1, ROUND(@p2,3) AS p2, ROUND(@p3,3) AS p3, ROUND(@p4,3) AS p4,
       ROUND(@p5,3) AS p5, ROUND(@p6,3) AS p6, ROUND(LEAST(@p2,@p3,@p4,@p5,@p6),3) AS profilBesteVon5;

SELECT '=== EAV / NEXANS / Message.ReceiverID / 24h (Boesfall: haeufigster Wert im 30-Tage-Fenster) ===' AS marke;
SET max_statement_time = 60;
SELECT QUOTE(v) INTO @w FROM (
  SELECT p.MessagePropertyValue AS v, COUNT(*) AS c
    FROM GlassfishDB.Message m
    JOIN GlassfishDB.MessageProperty p
      ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.ReceiverID'
   WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00' AND m.MessageLastUpdate < '2025-12-30 00:00:00'
     AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
   GROUP BY p.MessagePropertyValue ORDER BY c DESC, v ASC LIMIT 1) x;
SELECT CHAR_LENGTH(@w) - 2 AS laengeDesPruefwerts_w;
SET max_statement_time = 10;
SET @q = CONCAT(
 'SELECT COUNT(*) AS zeilen, MAX(CHAR_LENGTH(ProcessName)) AS maxPnLaenge FROM (select ',
 '`treffer`.`MessageID`, `treffer`.`MessageLastUpdate`, `treffer`.`MessageStatus`, `treffer`.`ProcessID`, ',
 '`GlassfishDB`.`Process`.`ProcessName`, `GlassfishDB`.`Project`.`ProjectName`, ',
 '`GlassfishDB`.`SOS`.`SOSName`, `GlassfishDB`.`SOSAction`.`SOSActionName`, `treffer`.`Source`, ',
 '`treffer`.`SourceMessageID`, `treffer`.`TargetMessageID`, `treffer`.`Target` from (select ',
 '`GlassfishDB`.`Message`.`MessageID`, `GlassfishDB`.`Message`.`MessageLastUpdate`, ',
 '`GlassfishDB`.`Message`.`MessageStatus`, `GlassfishDB`.`Message`.`ProcessID`, ',
 '`GlassfishDB`.`Message`.`SOSID`, `GlassfishDB`.`Message`.`SOSActionID`, `GlassfishDB`.`Message`.`Source`, ',
 '`GlassfishDB`.`Message`.`SourceMessageID`, `GlassfishDB`.`Message`.`TargetMessageID`, ',
 '`GlassfishDB`.`Message`.`Target` from `GlassfishDB`.`MessageProperty` as `mp1` join ',
 '`GlassfishDB`.`Message` on `GlassfishDB`.`Message`.`MessageID` = `mp1`.`MessageID` where ',
 '(`mp1`.`MessagePropertyName` = ''Message.ReceiverID'' and `mp1`.`MessagePropertyValue` = ', @w, ' and ',
 '`GlassfishDB`.`Message`.`MessageLastUpdate` >= ''2025-12-29 00:00:00'' and ',
 '`GlassfishDB`.`Message`.`MessageLastUpdate` <= ''2025-12-30 00:00:00'' and exists (select 1 as `one` from ',
 '`GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on ',
 '`GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where ',
 '(`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and ',
 '`GlassfishDB`.`ProjectMandant`.`MandantID` = ''NEXANS''))) group by `GlassfishDB`.`Message`.`MessageID` ',
 'order by `GlassfishDB`.`Message`.`MessageLastUpdate` desc, `GlassfishDB`.`Message`.`MessageID` desc fetch ',
 'next 51 rows only) as `treffer` left outer join `GlassfishDB`.`Process` on ',
 '`GlassfishDB`.`Process`.`ProcessID` = `treffer`.`ProcessID` left outer join `GlassfishDB`.`Project` on ',
 '`GlassfishDB`.`Project`.`ProjectID` = `GlassfishDB`.`Process`.`ProjectID` left outer join ',
 '`GlassfishDB`.`SOS` on `GlassfishDB`.`SOS`.`SOSID` = `treffer`.`SOSID` left outer join ',
 '`GlassfishDB`.`SOSAction` on (`GlassfishDB`.`SOSAction`.`SOSID` = `treffer`.`SOSID` and ',
 '`GlassfishDB`.`SOSAction`.`SOSActionID` = `treffer`.`SOSActionID`) order by `treffer`.`MessageLastUpdate` ',
 'desc, `treffer`.`MessageID` desc) AS gebaut ');
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
PREPARE qp FROM @q;
SELECT VARIABLE_VALUE INTO @h1 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY';
SELECT VARIABLE_VALUE INTO @h2 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT';
SELECT VARIABLE_VALUE INTO @h3 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT';
SELECT VARIABLE_VALUE INTO @h4 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE';
SELECT VARIABLE_VALUE INTO @h5 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS';
SELECT VARIABLE_VALUE INTO @h6 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND';
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d1;
SELECT (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY') - @h1 AS read_key,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT') - @h2 AS read_next,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT') - @h3 AS read_rnd_next,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE') - @h4 AS tmp_write,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS') - @h5 AS icp_attempts,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND') - @h6 AS read_rnd;
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
SELECT ROUND(@d1/1000,3) AS wanduhrLauf1, ROUND(@d2/1000,3) AS l2, ROUND(@d3/1000,3) AS l3, ROUND(@d4/1000,3) AS l4,
       ROUND(@d5/1000,3) AS l5, ROUND(@d6/1000,3) AS l6, ROUND(LEAST(@d2,@d3,@d4,@d5,@d6)/1000,3) AS wanduhrBesteVon5;
SELECT ROUND(@p1,3) AS profilLauf1, ROUND(@p2,3) AS p2, ROUND(@p3,3) AS p3, ROUND(@p4,3) AS p4,
       ROUND(@p5,3) AS p5, ROUND(@p6,3) AS p6, ROUND(LEAST(@p2,@p3,@p4,@p5,@p6),3) AS profilBesteVon5;

SELECT '=== EAV / NEXANS / Message.ReceiverID / 30T (Boesfall: haeufigster Wert im 30-Tage-Fenster) ===' AS marke;
SET max_statement_time = 60;
SELECT QUOTE(v) INTO @w FROM (
  SELECT p.MessagePropertyValue AS v, COUNT(*) AS c
    FROM GlassfishDB.Message m
    JOIN GlassfishDB.MessageProperty p
      ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.ReceiverID'
   WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00' AND m.MessageLastUpdate < '2025-12-30 00:00:00'
     AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
   GROUP BY p.MessagePropertyValue ORDER BY c DESC, v ASC LIMIT 1) x;
SELECT CHAR_LENGTH(@w) - 2 AS laengeDesPruefwerts_w;
SET max_statement_time = 10;
SET @q = CONCAT(
 'SELECT COUNT(*) AS zeilen, MAX(CHAR_LENGTH(ProcessName)) AS maxPnLaenge FROM (select ',
 '`treffer`.`MessageID`, `treffer`.`MessageLastUpdate`, `treffer`.`MessageStatus`, `treffer`.`ProcessID`, ',
 '`GlassfishDB`.`Process`.`ProcessName`, `GlassfishDB`.`Project`.`ProjectName`, ',
 '`GlassfishDB`.`SOS`.`SOSName`, `GlassfishDB`.`SOSAction`.`SOSActionName`, `treffer`.`Source`, ',
 '`treffer`.`SourceMessageID`, `treffer`.`TargetMessageID`, `treffer`.`Target` from (select ',
 '`GlassfishDB`.`Message`.`MessageID`, `GlassfishDB`.`Message`.`MessageLastUpdate`, ',
 '`GlassfishDB`.`Message`.`MessageStatus`, `GlassfishDB`.`Message`.`ProcessID`, ',
 '`GlassfishDB`.`Message`.`SOSID`, `GlassfishDB`.`Message`.`SOSActionID`, `GlassfishDB`.`Message`.`Source`, ',
 '`GlassfishDB`.`Message`.`SourceMessageID`, `GlassfishDB`.`Message`.`TargetMessageID`, ',
 '`GlassfishDB`.`Message`.`Target` from `GlassfishDB`.`MessageProperty` as `mp1` join ',
 '`GlassfishDB`.`Message` on `GlassfishDB`.`Message`.`MessageID` = `mp1`.`MessageID` where ',
 '(`mp1`.`MessagePropertyName` = ''Message.ReceiverID'' and `mp1`.`MessagePropertyValue` = ', @w, ' and ',
 '`GlassfishDB`.`Message`.`MessageLastUpdate` >= ''2025-11-30 00:00:00'' and ',
 '`GlassfishDB`.`Message`.`MessageLastUpdate` <= ''2025-12-30 00:00:00'' and exists (select 1 as `one` from ',
 '`GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on ',
 '`GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where ',
 '(`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and ',
 '`GlassfishDB`.`ProjectMandant`.`MandantID` = ''NEXANS''))) group by `GlassfishDB`.`Message`.`MessageID` ',
 'order by `GlassfishDB`.`Message`.`MessageLastUpdate` desc, `GlassfishDB`.`Message`.`MessageID` desc fetch ',
 'next 51 rows only) as `treffer` left outer join `GlassfishDB`.`Process` on ',
 '`GlassfishDB`.`Process`.`ProcessID` = `treffer`.`ProcessID` left outer join `GlassfishDB`.`Project` on ',
 '`GlassfishDB`.`Project`.`ProjectID` = `GlassfishDB`.`Process`.`ProjectID` left outer join ',
 '`GlassfishDB`.`SOS` on `GlassfishDB`.`SOS`.`SOSID` = `treffer`.`SOSID` left outer join ',
 '`GlassfishDB`.`SOSAction` on (`GlassfishDB`.`SOSAction`.`SOSID` = `treffer`.`SOSID` and ',
 '`GlassfishDB`.`SOSAction`.`SOSActionID` = `treffer`.`SOSActionID`) order by `treffer`.`MessageLastUpdate` ',
 'desc, `treffer`.`MessageID` desc) AS gebaut ');
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
PREPARE qp FROM @q;
SELECT VARIABLE_VALUE INTO @h1 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY';
SELECT VARIABLE_VALUE INTO @h2 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT';
SELECT VARIABLE_VALUE INTO @h3 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT';
SELECT VARIABLE_VALUE INTO @h4 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE';
SELECT VARIABLE_VALUE INTO @h5 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS';
SELECT VARIABLE_VALUE INTO @h6 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND';
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d1;
SELECT (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY') - @h1 AS read_key,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT') - @h2 AS read_next,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT') - @h3 AS read_rnd_next,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE') - @h4 AS tmp_write,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS') - @h5 AS icp_attempts,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND') - @h6 AS read_rnd;
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
SELECT ROUND(@d1/1000,3) AS wanduhrLauf1, ROUND(@d2/1000,3) AS l2, ROUND(@d3/1000,3) AS l3, ROUND(@d4/1000,3) AS l4,
       ROUND(@d5/1000,3) AS l5, ROUND(@d6/1000,3) AS l6, ROUND(LEAST(@d2,@d3,@d4,@d5,@d6)/1000,3) AS wanduhrBesteVon5;
SELECT ROUND(@p1,3) AS profilLauf1, ROUND(@p2,3) AS p2, ROUND(@p3,3) AS p3, ROUND(@p4,3) AS p4,
       ROUND(@p5,3) AS p5, ROUND(@p6,3) AS p6, ROUND(LEAST(@p2,@p3,@p4,@p5,@p6),3) AS profilBesteVon5;

SELECT '=== EAV / NEXANS / Message.ReceiverID / 1J (Boesfall: haeufigster Wert im 30-Tage-Fenster) ===' AS marke;
SET max_statement_time = 60;
SELECT QUOTE(v) INTO @w FROM (
  SELECT p.MessagePropertyValue AS v, COUNT(*) AS c
    FROM GlassfishDB.Message m
    JOIN GlassfishDB.MessageProperty p
      ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.ReceiverID'
   WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00' AND m.MessageLastUpdate < '2025-12-30 00:00:00'
     AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
   GROUP BY p.MessagePropertyValue ORDER BY c DESC, v ASC LIMIT 1) x;
SELECT CHAR_LENGTH(@w) - 2 AS laengeDesPruefwerts_w;
SET max_statement_time = 10;
SET @q = CONCAT(
 'SELECT COUNT(*) AS zeilen, MAX(CHAR_LENGTH(ProcessName)) AS maxPnLaenge FROM (select ',
 '`treffer`.`MessageID`, `treffer`.`MessageLastUpdate`, `treffer`.`MessageStatus`, `treffer`.`ProcessID`, ',
 '`GlassfishDB`.`Process`.`ProcessName`, `GlassfishDB`.`Project`.`ProjectName`, ',
 '`GlassfishDB`.`SOS`.`SOSName`, `GlassfishDB`.`SOSAction`.`SOSActionName`, `treffer`.`Source`, ',
 '`treffer`.`SourceMessageID`, `treffer`.`TargetMessageID`, `treffer`.`Target` from (select ',
 '`GlassfishDB`.`Message`.`MessageID`, `GlassfishDB`.`Message`.`MessageLastUpdate`, ',
 '`GlassfishDB`.`Message`.`MessageStatus`, `GlassfishDB`.`Message`.`ProcessID`, ',
 '`GlassfishDB`.`Message`.`SOSID`, `GlassfishDB`.`Message`.`SOSActionID`, `GlassfishDB`.`Message`.`Source`, ',
 '`GlassfishDB`.`Message`.`SourceMessageID`, `GlassfishDB`.`Message`.`TargetMessageID`, ',
 '`GlassfishDB`.`Message`.`Target` from `GlassfishDB`.`MessageProperty` as `mp1` join ',
 '`GlassfishDB`.`Message` on `GlassfishDB`.`Message`.`MessageID` = `mp1`.`MessageID` where ',
 '(`mp1`.`MessagePropertyName` = ''Message.ReceiverID'' and `mp1`.`MessagePropertyValue` = ', @w, ' and ',
 '`GlassfishDB`.`Message`.`MessageLastUpdate` >= ''2024-12-30 00:00:00'' and ',
 '`GlassfishDB`.`Message`.`MessageLastUpdate` <= ''2025-12-30 00:00:00'' and exists (select 1 as `one` from ',
 '`GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on ',
 '`GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where ',
 '(`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and ',
 '`GlassfishDB`.`ProjectMandant`.`MandantID` = ''NEXANS''))) group by `GlassfishDB`.`Message`.`MessageID` ',
 'order by `GlassfishDB`.`Message`.`MessageLastUpdate` desc, `GlassfishDB`.`Message`.`MessageID` desc fetch ',
 'next 51 rows only) as `treffer` left outer join `GlassfishDB`.`Process` on ',
 '`GlassfishDB`.`Process`.`ProcessID` = `treffer`.`ProcessID` left outer join `GlassfishDB`.`Project` on ',
 '`GlassfishDB`.`Project`.`ProjectID` = `GlassfishDB`.`Process`.`ProjectID` left outer join ',
 '`GlassfishDB`.`SOS` on `GlassfishDB`.`SOS`.`SOSID` = `treffer`.`SOSID` left outer join ',
 '`GlassfishDB`.`SOSAction` on (`GlassfishDB`.`SOSAction`.`SOSID` = `treffer`.`SOSID` and ',
 '`GlassfishDB`.`SOSAction`.`SOSActionID` = `treffer`.`SOSActionID`) order by `treffer`.`MessageLastUpdate` ',
 'desc, `treffer`.`MessageID` desc) AS gebaut ');
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
PREPARE qp FROM @q;
SELECT VARIABLE_VALUE INTO @h1 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY';
SELECT VARIABLE_VALUE INTO @h2 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT';
SELECT VARIABLE_VALUE INTO @h3 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT';
SELECT VARIABLE_VALUE INTO @h4 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE';
SELECT VARIABLE_VALUE INTO @h5 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS';
SELECT VARIABLE_VALUE INTO @h6 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND';
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d1;
SELECT (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY') - @h1 AS read_key,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT') - @h2 AS read_next,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT') - @h3 AS read_rnd_next,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE') - @h4 AS tmp_write,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS') - @h5 AS icp_attempts,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND') - @h6 AS read_rnd;
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
SELECT ROUND(@d1/1000,3) AS wanduhrLauf1, ROUND(@d2/1000,3) AS l2, ROUND(@d3/1000,3) AS l3, ROUND(@d4/1000,3) AS l4,
       ROUND(@d5/1000,3) AS l5, ROUND(@d6/1000,3) AS l6, ROUND(LEAST(@d2,@d3,@d4,@d5,@d6)/1000,3) AS wanduhrBesteVon5;
SELECT ROUND(@p1,3) AS profilLauf1, ROUND(@p2,3) AS p2, ROUND(@p3,3) AS p3, ROUND(@p4,3) AS p4,
       ROUND(@p5,3) AS p5, ROUND(@p6,3) AS p6, ROUND(LEAST(@p2,@p3,@p4,@p5,@p6),3) AS profilBesteVon5;

SELECT '=== EAV / NEXANS / Message.SNDPRN / 24h (Boesfall: haeufigster Wert im 30-Tage-Fenster) ===' AS marke;
SET max_statement_time = 60;
SELECT QUOTE(v) INTO @w FROM (
  SELECT p.MessagePropertyValue AS v, COUNT(*) AS c
    FROM GlassfishDB.Message m
    JOIN GlassfishDB.MessageProperty p
      ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.SNDPRN'
   WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00' AND m.MessageLastUpdate < '2025-12-30 00:00:00'
     AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
   GROUP BY p.MessagePropertyValue ORDER BY c DESC, v ASC LIMIT 1) x;
SELECT CHAR_LENGTH(@w) - 2 AS laengeDesPruefwerts_w;
SET max_statement_time = 10;
SET @q = CONCAT(
 'SELECT COUNT(*) AS zeilen, MAX(CHAR_LENGTH(ProcessName)) AS maxPnLaenge FROM (select ',
 '`treffer`.`MessageID`, `treffer`.`MessageLastUpdate`, `treffer`.`MessageStatus`, `treffer`.`ProcessID`, ',
 '`GlassfishDB`.`Process`.`ProcessName`, `GlassfishDB`.`Project`.`ProjectName`, ',
 '`GlassfishDB`.`SOS`.`SOSName`, `GlassfishDB`.`SOSAction`.`SOSActionName`, `treffer`.`Source`, ',
 '`treffer`.`SourceMessageID`, `treffer`.`TargetMessageID`, `treffer`.`Target` from (select ',
 '`GlassfishDB`.`Message`.`MessageID`, `GlassfishDB`.`Message`.`MessageLastUpdate`, ',
 '`GlassfishDB`.`Message`.`MessageStatus`, `GlassfishDB`.`Message`.`ProcessID`, ',
 '`GlassfishDB`.`Message`.`SOSID`, `GlassfishDB`.`Message`.`SOSActionID`, `GlassfishDB`.`Message`.`Source`, ',
 '`GlassfishDB`.`Message`.`SourceMessageID`, `GlassfishDB`.`Message`.`TargetMessageID`, ',
 '`GlassfishDB`.`Message`.`Target` from `GlassfishDB`.`MessageProperty` as `mp1` join ',
 '`GlassfishDB`.`Message` on `GlassfishDB`.`Message`.`MessageID` = `mp1`.`MessageID` where ',
 '(`mp1`.`MessagePropertyName` = ''Message.SNDPRN'' and `mp1`.`MessagePropertyValue` = ', @w, ' and ',
 '`GlassfishDB`.`Message`.`MessageLastUpdate` >= ''2025-12-29 00:00:00'' and ',
 '`GlassfishDB`.`Message`.`MessageLastUpdate` <= ''2025-12-30 00:00:00'' and exists (select 1 as `one` from ',
 '`GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on ',
 '`GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where ',
 '(`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and ',
 '`GlassfishDB`.`ProjectMandant`.`MandantID` = ''NEXANS''))) group by `GlassfishDB`.`Message`.`MessageID` ',
 'order by `GlassfishDB`.`Message`.`MessageLastUpdate` desc, `GlassfishDB`.`Message`.`MessageID` desc fetch ',
 'next 51 rows only) as `treffer` left outer join `GlassfishDB`.`Process` on ',
 '`GlassfishDB`.`Process`.`ProcessID` = `treffer`.`ProcessID` left outer join `GlassfishDB`.`Project` on ',
 '`GlassfishDB`.`Project`.`ProjectID` = `GlassfishDB`.`Process`.`ProjectID` left outer join ',
 '`GlassfishDB`.`SOS` on `GlassfishDB`.`SOS`.`SOSID` = `treffer`.`SOSID` left outer join ',
 '`GlassfishDB`.`SOSAction` on (`GlassfishDB`.`SOSAction`.`SOSID` = `treffer`.`SOSID` and ',
 '`GlassfishDB`.`SOSAction`.`SOSActionID` = `treffer`.`SOSActionID`) order by `treffer`.`MessageLastUpdate` ',
 'desc, `treffer`.`MessageID` desc) AS gebaut ');
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
PREPARE qp FROM @q;
SELECT VARIABLE_VALUE INTO @h1 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY';
SELECT VARIABLE_VALUE INTO @h2 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT';
SELECT VARIABLE_VALUE INTO @h3 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT';
SELECT VARIABLE_VALUE INTO @h4 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE';
SELECT VARIABLE_VALUE INTO @h5 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS';
SELECT VARIABLE_VALUE INTO @h6 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND';
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d1;
SELECT (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY') - @h1 AS read_key,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT') - @h2 AS read_next,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT') - @h3 AS read_rnd_next,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE') - @h4 AS tmp_write,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS') - @h5 AS icp_attempts,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND') - @h6 AS read_rnd;
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
SELECT ROUND(@d1/1000,3) AS wanduhrLauf1, ROUND(@d2/1000,3) AS l2, ROUND(@d3/1000,3) AS l3, ROUND(@d4/1000,3) AS l4,
       ROUND(@d5/1000,3) AS l5, ROUND(@d6/1000,3) AS l6, ROUND(LEAST(@d2,@d3,@d4,@d5,@d6)/1000,3) AS wanduhrBesteVon5;
SELECT ROUND(@p1,3) AS profilLauf1, ROUND(@p2,3) AS p2, ROUND(@p3,3) AS p3, ROUND(@p4,3) AS p4,
       ROUND(@p5,3) AS p5, ROUND(@p6,3) AS p6, ROUND(LEAST(@p2,@p3,@p4,@p5,@p6),3) AS profilBesteVon5;

SELECT '=== EAV / NEXANS / Message.SNDPRN / 30T (Boesfall: haeufigster Wert im 30-Tage-Fenster) ===' AS marke;
SET max_statement_time = 60;
SELECT QUOTE(v) INTO @w FROM (
  SELECT p.MessagePropertyValue AS v, COUNT(*) AS c
    FROM GlassfishDB.Message m
    JOIN GlassfishDB.MessageProperty p
      ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.SNDPRN'
   WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00' AND m.MessageLastUpdate < '2025-12-30 00:00:00'
     AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
   GROUP BY p.MessagePropertyValue ORDER BY c DESC, v ASC LIMIT 1) x;
SELECT CHAR_LENGTH(@w) - 2 AS laengeDesPruefwerts_w;
SET max_statement_time = 10;
SET @q = CONCAT(
 'SELECT COUNT(*) AS zeilen, MAX(CHAR_LENGTH(ProcessName)) AS maxPnLaenge FROM (select ',
 '`treffer`.`MessageID`, `treffer`.`MessageLastUpdate`, `treffer`.`MessageStatus`, `treffer`.`ProcessID`, ',
 '`GlassfishDB`.`Process`.`ProcessName`, `GlassfishDB`.`Project`.`ProjectName`, ',
 '`GlassfishDB`.`SOS`.`SOSName`, `GlassfishDB`.`SOSAction`.`SOSActionName`, `treffer`.`Source`, ',
 '`treffer`.`SourceMessageID`, `treffer`.`TargetMessageID`, `treffer`.`Target` from (select ',
 '`GlassfishDB`.`Message`.`MessageID`, `GlassfishDB`.`Message`.`MessageLastUpdate`, ',
 '`GlassfishDB`.`Message`.`MessageStatus`, `GlassfishDB`.`Message`.`ProcessID`, ',
 '`GlassfishDB`.`Message`.`SOSID`, `GlassfishDB`.`Message`.`SOSActionID`, `GlassfishDB`.`Message`.`Source`, ',
 '`GlassfishDB`.`Message`.`SourceMessageID`, `GlassfishDB`.`Message`.`TargetMessageID`, ',
 '`GlassfishDB`.`Message`.`Target` from `GlassfishDB`.`MessageProperty` as `mp1` join ',
 '`GlassfishDB`.`Message` on `GlassfishDB`.`Message`.`MessageID` = `mp1`.`MessageID` where ',
 '(`mp1`.`MessagePropertyName` = ''Message.SNDPRN'' and `mp1`.`MessagePropertyValue` = ', @w, ' and ',
 '`GlassfishDB`.`Message`.`MessageLastUpdate` >= ''2025-11-30 00:00:00'' and ',
 '`GlassfishDB`.`Message`.`MessageLastUpdate` <= ''2025-12-30 00:00:00'' and exists (select 1 as `one` from ',
 '`GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on ',
 '`GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where ',
 '(`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and ',
 '`GlassfishDB`.`ProjectMandant`.`MandantID` = ''NEXANS''))) group by `GlassfishDB`.`Message`.`MessageID` ',
 'order by `GlassfishDB`.`Message`.`MessageLastUpdate` desc, `GlassfishDB`.`Message`.`MessageID` desc fetch ',
 'next 51 rows only) as `treffer` left outer join `GlassfishDB`.`Process` on ',
 '`GlassfishDB`.`Process`.`ProcessID` = `treffer`.`ProcessID` left outer join `GlassfishDB`.`Project` on ',
 '`GlassfishDB`.`Project`.`ProjectID` = `GlassfishDB`.`Process`.`ProjectID` left outer join ',
 '`GlassfishDB`.`SOS` on `GlassfishDB`.`SOS`.`SOSID` = `treffer`.`SOSID` left outer join ',
 '`GlassfishDB`.`SOSAction` on (`GlassfishDB`.`SOSAction`.`SOSID` = `treffer`.`SOSID` and ',
 '`GlassfishDB`.`SOSAction`.`SOSActionID` = `treffer`.`SOSActionID`) order by `treffer`.`MessageLastUpdate` ',
 'desc, `treffer`.`MessageID` desc) AS gebaut ');
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
PREPARE qp FROM @q;
SELECT VARIABLE_VALUE INTO @h1 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY';
SELECT VARIABLE_VALUE INTO @h2 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT';
SELECT VARIABLE_VALUE INTO @h3 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT';
SELECT VARIABLE_VALUE INTO @h4 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE';
SELECT VARIABLE_VALUE INTO @h5 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS';
SELECT VARIABLE_VALUE INTO @h6 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND';
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d1;
SELECT (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY') - @h1 AS read_key,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT') - @h2 AS read_next,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT') - @h3 AS read_rnd_next,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE') - @h4 AS tmp_write,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS') - @h5 AS icp_attempts,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND') - @h6 AS read_rnd;
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
SELECT ROUND(@d1/1000,3) AS wanduhrLauf1, ROUND(@d2/1000,3) AS l2, ROUND(@d3/1000,3) AS l3, ROUND(@d4/1000,3) AS l4,
       ROUND(@d5/1000,3) AS l5, ROUND(@d6/1000,3) AS l6, ROUND(LEAST(@d2,@d3,@d4,@d5,@d6)/1000,3) AS wanduhrBesteVon5;
SELECT ROUND(@p1,3) AS profilLauf1, ROUND(@p2,3) AS p2, ROUND(@p3,3) AS p3, ROUND(@p4,3) AS p4,
       ROUND(@p5,3) AS p5, ROUND(@p6,3) AS p6, ROUND(LEAST(@p2,@p3,@p4,@p5,@p6),3) AS profilBesteVon5;

SELECT '=== EAV / NEXANS / Message.SNDPRN / 1J (Boesfall: haeufigster Wert im 30-Tage-Fenster) ===' AS marke;
SET max_statement_time = 60;
SELECT QUOTE(v) INTO @w FROM (
  SELECT p.MessagePropertyValue AS v, COUNT(*) AS c
    FROM GlassfishDB.Message m
    JOIN GlassfishDB.MessageProperty p
      ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.SNDPRN'
   WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00' AND m.MessageLastUpdate < '2025-12-30 00:00:00'
     AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
   GROUP BY p.MessagePropertyValue ORDER BY c DESC, v ASC LIMIT 1) x;
SELECT CHAR_LENGTH(@w) - 2 AS laengeDesPruefwerts_w;
SET max_statement_time = 10;
SET @q = CONCAT(
 'SELECT COUNT(*) AS zeilen, MAX(CHAR_LENGTH(ProcessName)) AS maxPnLaenge FROM (select ',
 '`treffer`.`MessageID`, `treffer`.`MessageLastUpdate`, `treffer`.`MessageStatus`, `treffer`.`ProcessID`, ',
 '`GlassfishDB`.`Process`.`ProcessName`, `GlassfishDB`.`Project`.`ProjectName`, ',
 '`GlassfishDB`.`SOS`.`SOSName`, `GlassfishDB`.`SOSAction`.`SOSActionName`, `treffer`.`Source`, ',
 '`treffer`.`SourceMessageID`, `treffer`.`TargetMessageID`, `treffer`.`Target` from (select ',
 '`GlassfishDB`.`Message`.`MessageID`, `GlassfishDB`.`Message`.`MessageLastUpdate`, ',
 '`GlassfishDB`.`Message`.`MessageStatus`, `GlassfishDB`.`Message`.`ProcessID`, ',
 '`GlassfishDB`.`Message`.`SOSID`, `GlassfishDB`.`Message`.`SOSActionID`, `GlassfishDB`.`Message`.`Source`, ',
 '`GlassfishDB`.`Message`.`SourceMessageID`, `GlassfishDB`.`Message`.`TargetMessageID`, ',
 '`GlassfishDB`.`Message`.`Target` from `GlassfishDB`.`MessageProperty` as `mp1` join ',
 '`GlassfishDB`.`Message` on `GlassfishDB`.`Message`.`MessageID` = `mp1`.`MessageID` where ',
 '(`mp1`.`MessagePropertyName` = ''Message.SNDPRN'' and `mp1`.`MessagePropertyValue` = ', @w, ' and ',
 '`GlassfishDB`.`Message`.`MessageLastUpdate` >= ''2024-12-30 00:00:00'' and ',
 '`GlassfishDB`.`Message`.`MessageLastUpdate` <= ''2025-12-30 00:00:00'' and exists (select 1 as `one` from ',
 '`GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on ',
 '`GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where ',
 '(`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and ',
 '`GlassfishDB`.`ProjectMandant`.`MandantID` = ''NEXANS''))) group by `GlassfishDB`.`Message`.`MessageID` ',
 'order by `GlassfishDB`.`Message`.`MessageLastUpdate` desc, `GlassfishDB`.`Message`.`MessageID` desc fetch ',
 'next 51 rows only) as `treffer` left outer join `GlassfishDB`.`Process` on ',
 '`GlassfishDB`.`Process`.`ProcessID` = `treffer`.`ProcessID` left outer join `GlassfishDB`.`Project` on ',
 '`GlassfishDB`.`Project`.`ProjectID` = `GlassfishDB`.`Process`.`ProjectID` left outer join ',
 '`GlassfishDB`.`SOS` on `GlassfishDB`.`SOS`.`SOSID` = `treffer`.`SOSID` left outer join ',
 '`GlassfishDB`.`SOSAction` on (`GlassfishDB`.`SOSAction`.`SOSID` = `treffer`.`SOSID` and ',
 '`GlassfishDB`.`SOSAction`.`SOSActionID` = `treffer`.`SOSActionID`) order by `treffer`.`MessageLastUpdate` ',
 'desc, `treffer`.`MessageID` desc) AS gebaut ');
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
PREPARE qp FROM @q;
SELECT VARIABLE_VALUE INTO @h1 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY';
SELECT VARIABLE_VALUE INTO @h2 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT';
SELECT VARIABLE_VALUE INTO @h3 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT';
SELECT VARIABLE_VALUE INTO @h4 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE';
SELECT VARIABLE_VALUE INTO @h5 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS';
SELECT VARIABLE_VALUE INTO @h6 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND';
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d1;
SELECT (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY') - @h1 AS read_key,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT') - @h2 AS read_next,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT') - @h3 AS read_rnd_next,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE') - @h4 AS tmp_write,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS') - @h5 AS icp_attempts,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND') - @h6 AS read_rnd;
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
SELECT ROUND(@d1/1000,3) AS wanduhrLauf1, ROUND(@d2/1000,3) AS l2, ROUND(@d3/1000,3) AS l3, ROUND(@d4/1000,3) AS l4,
       ROUND(@d5/1000,3) AS l5, ROUND(@d6/1000,3) AS l6, ROUND(LEAST(@d2,@d3,@d4,@d5,@d6)/1000,3) AS wanduhrBesteVon5;
SELECT ROUND(@p1,3) AS profilLauf1, ROUND(@p2,3) AS p2, ROUND(@p3,3) AS p3, ROUND(@p4,3) AS p4,
       ROUND(@p5,3) AS p5, ROUND(@p6,3) AS p6, ROUND(LEAST(@p2,@p3,@p4,@p5,@p6),3) AS profilBesteVon5;

SELECT '=== EAV / NEXANS / Message.VFN / 24h (Boesfall: haeufigster Wert im 30-Tage-Fenster) ===' AS marke;
SET max_statement_time = 60;
SELECT QUOTE(v) INTO @w FROM (
  SELECT p.MessagePropertyValue AS v, COUNT(*) AS c
    FROM GlassfishDB.Message m
    JOIN GlassfishDB.MessageProperty p
      ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.VFN'
   WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00' AND m.MessageLastUpdate < '2025-12-30 00:00:00'
     AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
   GROUP BY p.MessagePropertyValue ORDER BY c DESC, v ASC LIMIT 1) x;
SELECT CHAR_LENGTH(@w) - 2 AS laengeDesPruefwerts_w;
SET max_statement_time = 10;
SET @q = CONCAT(
 'SELECT COUNT(*) AS zeilen, MAX(CHAR_LENGTH(ProcessName)) AS maxPnLaenge FROM (select ',
 '`treffer`.`MessageID`, `treffer`.`MessageLastUpdate`, `treffer`.`MessageStatus`, `treffer`.`ProcessID`, ',
 '`GlassfishDB`.`Process`.`ProcessName`, `GlassfishDB`.`Project`.`ProjectName`, ',
 '`GlassfishDB`.`SOS`.`SOSName`, `GlassfishDB`.`SOSAction`.`SOSActionName`, `treffer`.`Source`, ',
 '`treffer`.`SourceMessageID`, `treffer`.`TargetMessageID`, `treffer`.`Target` from (select ',
 '`GlassfishDB`.`Message`.`MessageID`, `GlassfishDB`.`Message`.`MessageLastUpdate`, ',
 '`GlassfishDB`.`Message`.`MessageStatus`, `GlassfishDB`.`Message`.`ProcessID`, ',
 '`GlassfishDB`.`Message`.`SOSID`, `GlassfishDB`.`Message`.`SOSActionID`, `GlassfishDB`.`Message`.`Source`, ',
 '`GlassfishDB`.`Message`.`SourceMessageID`, `GlassfishDB`.`Message`.`TargetMessageID`, ',
 '`GlassfishDB`.`Message`.`Target` from `GlassfishDB`.`MessageProperty` as `mp1` join ',
 '`GlassfishDB`.`Message` on `GlassfishDB`.`Message`.`MessageID` = `mp1`.`MessageID` where ',
 '(`mp1`.`MessagePropertyName` = ''Message.VFN'' and `mp1`.`MessagePropertyValue` = ', @w, ' and ',
 '`GlassfishDB`.`Message`.`MessageLastUpdate` >= ''2025-12-29 00:00:00'' and ',
 '`GlassfishDB`.`Message`.`MessageLastUpdate` <= ''2025-12-30 00:00:00'' and exists (select 1 as `one` from ',
 '`GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on ',
 '`GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where ',
 '(`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and ',
 '`GlassfishDB`.`ProjectMandant`.`MandantID` = ''NEXANS''))) group by `GlassfishDB`.`Message`.`MessageID` ',
 'order by `GlassfishDB`.`Message`.`MessageLastUpdate` desc, `GlassfishDB`.`Message`.`MessageID` desc fetch ',
 'next 51 rows only) as `treffer` left outer join `GlassfishDB`.`Process` on ',
 '`GlassfishDB`.`Process`.`ProcessID` = `treffer`.`ProcessID` left outer join `GlassfishDB`.`Project` on ',
 '`GlassfishDB`.`Project`.`ProjectID` = `GlassfishDB`.`Process`.`ProjectID` left outer join ',
 '`GlassfishDB`.`SOS` on `GlassfishDB`.`SOS`.`SOSID` = `treffer`.`SOSID` left outer join ',
 '`GlassfishDB`.`SOSAction` on (`GlassfishDB`.`SOSAction`.`SOSID` = `treffer`.`SOSID` and ',
 '`GlassfishDB`.`SOSAction`.`SOSActionID` = `treffer`.`SOSActionID`) order by `treffer`.`MessageLastUpdate` ',
 'desc, `treffer`.`MessageID` desc) AS gebaut ');
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
PREPARE qp FROM @q;
SELECT VARIABLE_VALUE INTO @h1 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY';
SELECT VARIABLE_VALUE INTO @h2 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT';
SELECT VARIABLE_VALUE INTO @h3 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT';
SELECT VARIABLE_VALUE INTO @h4 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE';
SELECT VARIABLE_VALUE INTO @h5 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS';
SELECT VARIABLE_VALUE INTO @h6 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND';
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d1;
SELECT (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY') - @h1 AS read_key,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT') - @h2 AS read_next,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT') - @h3 AS read_rnd_next,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE') - @h4 AS tmp_write,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS') - @h5 AS icp_attempts,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND') - @h6 AS read_rnd;
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
SELECT ROUND(@d1/1000,3) AS wanduhrLauf1, ROUND(@d2/1000,3) AS l2, ROUND(@d3/1000,3) AS l3, ROUND(@d4/1000,3) AS l4,
       ROUND(@d5/1000,3) AS l5, ROUND(@d6/1000,3) AS l6, ROUND(LEAST(@d2,@d3,@d4,@d5,@d6)/1000,3) AS wanduhrBesteVon5;
SELECT ROUND(@p1,3) AS profilLauf1, ROUND(@p2,3) AS p2, ROUND(@p3,3) AS p3, ROUND(@p4,3) AS p4,
       ROUND(@p5,3) AS p5, ROUND(@p6,3) AS p6, ROUND(LEAST(@p2,@p3,@p4,@p5,@p6),3) AS profilBesteVon5;

SELECT '=== EAV / NEXANS / Message.VFN / 30T (Boesfall: haeufigster Wert im 30-Tage-Fenster) ===' AS marke;
SET max_statement_time = 60;
SELECT QUOTE(v) INTO @w FROM (
  SELECT p.MessagePropertyValue AS v, COUNT(*) AS c
    FROM GlassfishDB.Message m
    JOIN GlassfishDB.MessageProperty p
      ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.VFN'
   WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00' AND m.MessageLastUpdate < '2025-12-30 00:00:00'
     AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
   GROUP BY p.MessagePropertyValue ORDER BY c DESC, v ASC LIMIT 1) x;
SELECT CHAR_LENGTH(@w) - 2 AS laengeDesPruefwerts_w;
SET max_statement_time = 10;
SET @q = CONCAT(
 'SELECT COUNT(*) AS zeilen, MAX(CHAR_LENGTH(ProcessName)) AS maxPnLaenge FROM (select ',
 '`treffer`.`MessageID`, `treffer`.`MessageLastUpdate`, `treffer`.`MessageStatus`, `treffer`.`ProcessID`, ',
 '`GlassfishDB`.`Process`.`ProcessName`, `GlassfishDB`.`Project`.`ProjectName`, ',
 '`GlassfishDB`.`SOS`.`SOSName`, `GlassfishDB`.`SOSAction`.`SOSActionName`, `treffer`.`Source`, ',
 '`treffer`.`SourceMessageID`, `treffer`.`TargetMessageID`, `treffer`.`Target` from (select ',
 '`GlassfishDB`.`Message`.`MessageID`, `GlassfishDB`.`Message`.`MessageLastUpdate`, ',
 '`GlassfishDB`.`Message`.`MessageStatus`, `GlassfishDB`.`Message`.`ProcessID`, ',
 '`GlassfishDB`.`Message`.`SOSID`, `GlassfishDB`.`Message`.`SOSActionID`, `GlassfishDB`.`Message`.`Source`, ',
 '`GlassfishDB`.`Message`.`SourceMessageID`, `GlassfishDB`.`Message`.`TargetMessageID`, ',
 '`GlassfishDB`.`Message`.`Target` from `GlassfishDB`.`MessageProperty` as `mp1` join ',
 '`GlassfishDB`.`Message` on `GlassfishDB`.`Message`.`MessageID` = `mp1`.`MessageID` where ',
 '(`mp1`.`MessagePropertyName` = ''Message.VFN'' and `mp1`.`MessagePropertyValue` = ', @w, ' and ',
 '`GlassfishDB`.`Message`.`MessageLastUpdate` >= ''2025-11-30 00:00:00'' and ',
 '`GlassfishDB`.`Message`.`MessageLastUpdate` <= ''2025-12-30 00:00:00'' and exists (select 1 as `one` from ',
 '`GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on ',
 '`GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where ',
 '(`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and ',
 '`GlassfishDB`.`ProjectMandant`.`MandantID` = ''NEXANS''))) group by `GlassfishDB`.`Message`.`MessageID` ',
 'order by `GlassfishDB`.`Message`.`MessageLastUpdate` desc, `GlassfishDB`.`Message`.`MessageID` desc fetch ',
 'next 51 rows only) as `treffer` left outer join `GlassfishDB`.`Process` on ',
 '`GlassfishDB`.`Process`.`ProcessID` = `treffer`.`ProcessID` left outer join `GlassfishDB`.`Project` on ',
 '`GlassfishDB`.`Project`.`ProjectID` = `GlassfishDB`.`Process`.`ProjectID` left outer join ',
 '`GlassfishDB`.`SOS` on `GlassfishDB`.`SOS`.`SOSID` = `treffer`.`SOSID` left outer join ',
 '`GlassfishDB`.`SOSAction` on (`GlassfishDB`.`SOSAction`.`SOSID` = `treffer`.`SOSID` and ',
 '`GlassfishDB`.`SOSAction`.`SOSActionID` = `treffer`.`SOSActionID`) order by `treffer`.`MessageLastUpdate` ',
 'desc, `treffer`.`MessageID` desc) AS gebaut ');
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
PREPARE qp FROM @q;
SELECT VARIABLE_VALUE INTO @h1 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY';
SELECT VARIABLE_VALUE INTO @h2 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT';
SELECT VARIABLE_VALUE INTO @h3 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT';
SELECT VARIABLE_VALUE INTO @h4 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE';
SELECT VARIABLE_VALUE INTO @h5 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS';
SELECT VARIABLE_VALUE INTO @h6 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND';
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d1;
SELECT (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY') - @h1 AS read_key,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT') - @h2 AS read_next,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT') - @h3 AS read_rnd_next,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE') - @h4 AS tmp_write,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS') - @h5 AS icp_attempts,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND') - @h6 AS read_rnd;
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
SELECT ROUND(@d1/1000,3) AS wanduhrLauf1, ROUND(@d2/1000,3) AS l2, ROUND(@d3/1000,3) AS l3, ROUND(@d4/1000,3) AS l4,
       ROUND(@d5/1000,3) AS l5, ROUND(@d6/1000,3) AS l6, ROUND(LEAST(@d2,@d3,@d4,@d5,@d6)/1000,3) AS wanduhrBesteVon5;
SELECT ROUND(@p1,3) AS profilLauf1, ROUND(@p2,3) AS p2, ROUND(@p3,3) AS p3, ROUND(@p4,3) AS p4,
       ROUND(@p5,3) AS p5, ROUND(@p6,3) AS p6, ROUND(LEAST(@p2,@p3,@p4,@p5,@p6),3) AS profilBesteVon5;

SELECT '=== EAV / NEXANS / Message.VFN / 1J (Boesfall: haeufigster Wert im 30-Tage-Fenster) ===' AS marke;
SET max_statement_time = 60;
SELECT QUOTE(v) INTO @w FROM (
  SELECT p.MessagePropertyValue AS v, COUNT(*) AS c
    FROM GlassfishDB.Message m
    JOIN GlassfishDB.MessageProperty p
      ON p.MessageID = m.MessageID AND p.MessagePropertyName = 'Message.VFN'
   WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00' AND m.MessageLastUpdate < '2025-12-30 00:00:00'
     AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
   GROUP BY p.MessagePropertyValue ORDER BY c DESC, v ASC LIMIT 1) x;
SELECT CHAR_LENGTH(@w) - 2 AS laengeDesPruefwerts_w;
SET max_statement_time = 10;
SET @q = CONCAT(
 'SELECT COUNT(*) AS zeilen, MAX(CHAR_LENGTH(ProcessName)) AS maxPnLaenge FROM (select ',
 '`treffer`.`MessageID`, `treffer`.`MessageLastUpdate`, `treffer`.`MessageStatus`, `treffer`.`ProcessID`, ',
 '`GlassfishDB`.`Process`.`ProcessName`, `GlassfishDB`.`Project`.`ProjectName`, ',
 '`GlassfishDB`.`SOS`.`SOSName`, `GlassfishDB`.`SOSAction`.`SOSActionName`, `treffer`.`Source`, ',
 '`treffer`.`SourceMessageID`, `treffer`.`TargetMessageID`, `treffer`.`Target` from (select ',
 '`GlassfishDB`.`Message`.`MessageID`, `GlassfishDB`.`Message`.`MessageLastUpdate`, ',
 '`GlassfishDB`.`Message`.`MessageStatus`, `GlassfishDB`.`Message`.`ProcessID`, ',
 '`GlassfishDB`.`Message`.`SOSID`, `GlassfishDB`.`Message`.`SOSActionID`, `GlassfishDB`.`Message`.`Source`, ',
 '`GlassfishDB`.`Message`.`SourceMessageID`, `GlassfishDB`.`Message`.`TargetMessageID`, ',
 '`GlassfishDB`.`Message`.`Target` from `GlassfishDB`.`MessageProperty` as `mp1` join ',
 '`GlassfishDB`.`Message` on `GlassfishDB`.`Message`.`MessageID` = `mp1`.`MessageID` where ',
 '(`mp1`.`MessagePropertyName` = ''Message.VFN'' and `mp1`.`MessagePropertyValue` = ', @w, ' and ',
 '`GlassfishDB`.`Message`.`MessageLastUpdate` >= ''2024-12-30 00:00:00'' and ',
 '`GlassfishDB`.`Message`.`MessageLastUpdate` <= ''2025-12-30 00:00:00'' and exists (select 1 as `one` from ',
 '`GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on ',
 '`GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where ',
 '(`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and ',
 '`GlassfishDB`.`ProjectMandant`.`MandantID` = ''NEXANS''))) group by `GlassfishDB`.`Message`.`MessageID` ',
 'order by `GlassfishDB`.`Message`.`MessageLastUpdate` desc, `GlassfishDB`.`Message`.`MessageID` desc fetch ',
 'next 51 rows only) as `treffer` left outer join `GlassfishDB`.`Process` on ',
 '`GlassfishDB`.`Process`.`ProcessID` = `treffer`.`ProcessID` left outer join `GlassfishDB`.`Project` on ',
 '`GlassfishDB`.`Project`.`ProjectID` = `GlassfishDB`.`Process`.`ProjectID` left outer join ',
 '`GlassfishDB`.`SOS` on `GlassfishDB`.`SOS`.`SOSID` = `treffer`.`SOSID` left outer join ',
 '`GlassfishDB`.`SOSAction` on (`GlassfishDB`.`SOSAction`.`SOSID` = `treffer`.`SOSID` and ',
 '`GlassfishDB`.`SOSAction`.`SOSActionID` = `treffer`.`SOSActionID`) order by `treffer`.`MessageLastUpdate` ',
 'desc, `treffer`.`MessageID` desc) AS gebaut ');
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
PREPARE qp FROM @q;
SELECT VARIABLE_VALUE INTO @h1 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY';
SELECT VARIABLE_VALUE INTO @h2 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT';
SELECT VARIABLE_VALUE INTO @h3 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT';
SELECT VARIABLE_VALUE INTO @h4 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE';
SELECT VARIABLE_VALUE INTO @h5 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS';
SELECT VARIABLE_VALUE INTO @h6 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND';
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d1;
SELECT (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY') - @h1 AS read_key,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT') - @h2 AS read_next,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT') - @h3 AS read_rnd_next,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE') - @h4 AS tmp_write,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS') - @h5 AS icp_attempts,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND') - @h6 AS read_rnd;
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
SELECT ROUND(@d1/1000,3) AS wanduhrLauf1, ROUND(@d2/1000,3) AS l2, ROUND(@d3/1000,3) AS l3, ROUND(@d4/1000,3) AS l4,
       ROUND(@d5/1000,3) AS l5, ROUND(@d6/1000,3) AS l6, ROUND(LEAST(@d2,@d3,@d4,@d5,@d6)/1000,3) AS wanduhrBesteVon5;
SELECT ROUND(@p1,3) AS profilLauf1, ROUND(@p2,3) AS p2, ROUND(@p3,3) AS p3, ROUND(@p4,3) AS p4,
       ROUND(@p5,3) AS p5, ROUND(@p6,3) AS p6, ROUND(LEAST(@p2,@p3,@p4,@p5,@p6),3) AS profilBesteVon5;
