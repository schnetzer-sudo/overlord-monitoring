-- Property-Suche, Teil 2 (08.09.2026) — Message.ProcessName ueber die Stammdaten (E-109), Regel L7.
-- Aufruf ueber p5a-nexans.sql bzw. p5b-suttons.sql, die vorher @mandant setzen.
-- Die beiden Statements sind von jOOQ gerendert (MockConnection, STATIC_STATEMENT) und Zeichen fuer
-- Zeichen die des Codes; der Kern steht in der Huelle SELECT COUNT(*) ... FROM (<Statement>), damit
-- kein Prozess- oder Projektname in die Rohausgabe geraet (G1). Pruefwert je Sitzung hergeleitet.
-- Wanduhr (SYSDATE(6)) neben dem Profil (E-107), Handler-Zaehler des ersten Laufs, EXPLAIN je Fall.
-- Grenze 10 s = Lese-Pool fuer die gemessenen Statements; 60 s fuer Herleitung und Gleichheitsprobe.
SELECT @@global.read_only AS globalReadOnly;
SELECT NOW() AS serverzeitBeginn;
SELECT @mandant AS mandant;
SET @m = QUOTE(@mandant);
SET profiling = 1;
SET profiling_history_size = 100;
SELECT SYSDATE(6) INTO @t0; SELECT 1; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @eich;
SELECT ROUND(@eich/1000,3) AS eichungLeeresStatementMs;

-- ─── Pruefwert: der haeufigste Prozessname im 30-Tage-Fenster des Mandanten (wie p3) ────────────
SELECT '=== Pruefwert / haeufigster ProcessName im 30-Tage-Fenster ===' AS marke;
SET max_statement_time = 60;
SELECT QUOTE(pn.ProcessName) INTO @w FROM (
  SELECT m.ProcessID AS s, COUNT(*) AS c FROM GlassfishDB.Message m
   WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00' AND m.MessageLastUpdate < '2025-12-30 00:00:00' AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = @mandant)
   GROUP BY m.ProcessID ORDER BY c DESC, s ASC LIMIT 1) x
  JOIN GlassfishDB.Process pn ON pn.ProcessID = x.s;
SELECT CHAR_LENGTH(@w) - 2 AS laengeDesPruefwerts_w;

-- ─── A: die Aufloesung — das erste Statement der neuen Form ─────────────────────────────────────
SELECT '=== A / Aufloesung ProcessName -> ProcessID (fensterunabhaengig) ===' AS marke;
SET max_statement_time = 10;
SET @a = CONCAT('select distinct `GlassfishDB`.`Process`.`ProcessID` from `GlassfishDB`.`Process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `GlassfishDB`.`Process`.`ProjectID` where (`GlassfishDB`.`ProjectMandant`.`MandantID` = ', @m, ' and `GlassfishDB`.`Process`.`ProcessName` = ', @w, ') order by `GlassfishDB`.`Process`.`ProcessID`');
SET @q = CONCAT('SELECT COUNT(*) AS kennungen, MAX(CHAR_LENGTH(ProcessID)) AS maxIdLaenge FROM (', @a, ') AS gebaut');
SET @e = CONCAT('EXPLAIN ', @a);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
PREPARE qp FROM @q;
SELECT VARIABLE_VALUE INTO @h1 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY';
SELECT VARIABLE_VALUE INTO @h2 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT';
SELECT VARIABLE_VALUE INTO @h3 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT';
SELECT VARIABLE_VALUE INTO @h4 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE';
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d1;
SELECT (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY') - @h1 AS read_key,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT') - @h2 AS read_next,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT') - @h3 AS read_rnd_next,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE') - @h4 AS tmp_write;
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

-- Die Kennungen, so wie der Code sie in den Kern einsetzt — als Literale (G1: nur die Zahl wird gezeigt).
-- Das INTO steht IM vorbereiteten Text: EXECUTE … INTO kennt MariaDB nicht (erster Lauf, 1064).
SET @ids_q = CONCAT('SELECT GROUP_CONCAT(QUOTE(ProcessID) ORDER BY ProcessID SEPARATOR '', '') INTO @ids FROM (', @a, ') AS gebaut');
PREPARE ip FROM @ids_q; EXECUTE ip; DEALLOCATE PREPARE ip;
SET @ids_n = CONCAT('SELECT COUNT(*) INTO @n FROM (', @a, ') AS gebaut');
PREPARE np FROM @ids_n; EXECUTE np; DEALLOCATE PREPARE np;
SELECT @n AS kennungenZumNamen, CHAR_LENGTH(@ids) AS laengeDerKennungsliste;

-- ─── B: der Kern der neuen Form ueber 30 Tage ───────────────────────────────────────────────────
SELECT '=== B / Kern neu / ProcessID IN (…) / 30T ===' AS marke;
SET max_statement_time = 10;
SET @q = CONCAT('SELECT COUNT(*) AS zeilen, MAX(CHAR_LENGTH(ProcessName)) AS maxPnLaenge FROM (select `treffer`.`MessageID`, `treffer`.`MessageLastUpdate`, `treffer`.`MessageStatus`, `treffer`.`ProcessID`, `GlassfishDB`.`Process`.`ProcessName`, `GlassfishDB`.`Project`.`ProjectName`, `GlassfishDB`.`SOS`.`SOSName`, `GlassfishDB`.`SOSAction`.`SOSActionName`, `treffer`.`Source`, `treffer`.`SourceMessageID`, `treffer`.`TargetMessageID`, `treffer`.`Target` from (select `GlassfishDB`.`Message`.`MessageID`, `GlassfishDB`.`Message`.`MessageLastUpdate`, `GlassfishDB`.`Message`.`MessageStatus`, `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`SOSID`, `GlassfishDB`.`Message`.`SOSActionID`, `GlassfishDB`.`Message`.`Source`, `GlassfishDB`.`Message`.`SourceMessageID`, `GlassfishDB`.`Message`.`TargetMessageID`, `GlassfishDB`.`Message`.`Target` from `GlassfishDB`.`Message` where (`GlassfishDB`.`Message`.`ProcessID` in (', @ids, ') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= ''2025-11-30 00:00:00'' and `GlassfishDB`.`Message`.`MessageLastUpdate` <= ''2025-12-30 00:00:00'' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = ', @m, '))) group by `GlassfishDB`.`Message`.`MessageID` order by `GlassfishDB`.`Message`.`MessageLastUpdate` desc, `GlassfishDB`.`Message`.`MessageID` desc fetch next 51 rows only) as `treffer` left outer join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProcessID` = `treffer`.`ProcessID` left outer join `GlassfishDB`.`Project` on `GlassfishDB`.`Project`.`ProjectID` = `GlassfishDB`.`Process`.`ProjectID` left outer join `GlassfishDB`.`SOS` on `GlassfishDB`.`SOS`.`SOSID` = `treffer`.`SOSID` left outer join `GlassfishDB`.`SOSAction` on (`GlassfishDB`.`SOSAction`.`SOSID` = `treffer`.`SOSID` and `GlassfishDB`.`SOSAction`.`SOSActionID` = `treffer`.`SOSActionID`) order by `treffer`.`MessageLastUpdate` desc, `treffer`.`MessageID` desc) AS gebaut');
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
PREPARE qp FROM @q;
SELECT VARIABLE_VALUE INTO @h1 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY';
SELECT VARIABLE_VALUE INTO @h2 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT';
SELECT VARIABLE_VALUE INTO @h3 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT';
SELECT VARIABLE_VALUE INTO @h4 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE';
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d1;
SELECT (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY') - @h1 AS read_key,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT') - @h2 AS read_next,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT') - @h3 AS read_rnd_next,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE') - @h4 AS tmp_write;
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

-- ─── C: der Kern der neuen Form ueber ein Jahr ──────────────────────────────────────────────────
SELECT '=== C / Kern neu / ProcessID IN (…) / 1J ===' AS marke;
SET max_statement_time = 10;
SET @q = CONCAT('SELECT COUNT(*) AS zeilen, MAX(CHAR_LENGTH(ProcessName)) AS maxPnLaenge FROM (select `treffer`.`MessageID`, `treffer`.`MessageLastUpdate`, `treffer`.`MessageStatus`, `treffer`.`ProcessID`, `GlassfishDB`.`Process`.`ProcessName`, `GlassfishDB`.`Project`.`ProjectName`, `GlassfishDB`.`SOS`.`SOSName`, `GlassfishDB`.`SOSAction`.`SOSActionName`, `treffer`.`Source`, `treffer`.`SourceMessageID`, `treffer`.`TargetMessageID`, `treffer`.`Target` from (select `GlassfishDB`.`Message`.`MessageID`, `GlassfishDB`.`Message`.`MessageLastUpdate`, `GlassfishDB`.`Message`.`MessageStatus`, `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`SOSID`, `GlassfishDB`.`Message`.`SOSActionID`, `GlassfishDB`.`Message`.`Source`, `GlassfishDB`.`Message`.`SourceMessageID`, `GlassfishDB`.`Message`.`TargetMessageID`, `GlassfishDB`.`Message`.`Target` from `GlassfishDB`.`Message` where (`GlassfishDB`.`Message`.`ProcessID` in (', @ids, ') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= ''2024-12-30 00:00:00'' and `GlassfishDB`.`Message`.`MessageLastUpdate` <= ''2025-12-30 00:00:00'' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = ', @m, '))) group by `GlassfishDB`.`Message`.`MessageID` order by `GlassfishDB`.`Message`.`MessageLastUpdate` desc, `GlassfishDB`.`Message`.`MessageID` desc fetch next 51 rows only) as `treffer` left outer join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProcessID` = `treffer`.`ProcessID` left outer join `GlassfishDB`.`Project` on `GlassfishDB`.`Project`.`ProjectID` = `GlassfishDB`.`Process`.`ProjectID` left outer join `GlassfishDB`.`SOS` on `GlassfishDB`.`SOS`.`SOSID` = `treffer`.`SOSID` left outer join `GlassfishDB`.`SOSAction` on (`GlassfishDB`.`SOSAction`.`SOSID` = `treffer`.`SOSID` and `GlassfishDB`.`SOSAction`.`SOSActionID` = `treffer`.`SOSActionID`) order by `treffer`.`MessageLastUpdate` desc, `treffer`.`MessageID` desc) AS gebaut');
SET @e = CONCAT('EXPLAIN ', @q);
PREPARE ep FROM @e; EXECUTE ep; DEALLOCATE PREPARE ep;
PREPARE qp FROM @q;
SELECT VARIABLE_VALUE INTO @h1 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY';
SELECT VARIABLE_VALUE INTO @h2 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT';
SELECT VARIABLE_VALUE INTO @h3 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT';
SELECT VARIABLE_VALUE INTO @h4 FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE';
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6)) INTO @d1;
SELECT (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_KEY') - @h1 AS read_key,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_NEXT') - @h2 AS read_next,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_READ_RND_NEXT') - @h3 AS read_rnd_next,
       (SELECT VARIABLE_VALUE FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME = 'HANDLER_TMP_WRITE') - @h4 AS tmp_write;
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

-- ─── D: die Gleichheitsprobe ueber 30 Tage — alte Form (Join) gegen neue Form (IN) ─────────────
-- Verglichen wird die Treffermenge als Zahl und als reihenfolgeunabhaengige Pruefsumme der Kennungen,
-- (1) gedeckelt wie im Code (51 Zeilen) und (2) ungedeckelt ueber das ganze Fenster.
SELECT '=== D / Gleichheitsprobe 30T: alt (Join feld_process) gegen neu (ProcessID IN) ===' AS marke;
SET max_statement_time = 60;
SET @kern_alt = CONCAT('select `GlassfishDB`.`Message`.`MessageID` from `GlassfishDB`.`Message` join `GlassfishDB`.`Process` as `feld_process` on `feld_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` where (`feld_process`.`ProcessName` = ', @w, ' and `GlassfishDB`.`Message`.`MessageLastUpdate` >= ''2025-11-30 00:00:00'' and `GlassfishDB`.`Message`.`MessageLastUpdate` <= ''2025-12-30 00:00:00'' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = ', @m, '))) group by `GlassfishDB`.`Message`.`MessageID` order by `GlassfishDB`.`Message`.`MessageLastUpdate` desc, `GlassfishDB`.`Message`.`MessageID` desc');
SET @kern_neu = CONCAT('select `GlassfishDB`.`Message`.`MessageID` from `GlassfishDB`.`Message` where (`GlassfishDB`.`Message`.`ProcessID` in (', @ids, ') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= ''2025-11-30 00:00:00'' and `GlassfishDB`.`Message`.`MessageLastUpdate` <= ''2025-12-30 00:00:00'' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = ', @m, '))) group by `GlassfishDB`.`Message`.`MessageID` order by `GlassfishDB`.`Message`.`MessageLastUpdate` desc, `GlassfishDB`.`Message`.`MessageID` desc');
SET @q = CONCAT('SELECT ''alt gedeckelt'' AS form, COUNT(*) AS zeilen, BIT_XOR(CRC32(MessageID)) AS pruefsumme, MIN(MessageID) AS erste, MAX(MessageID) AS letzte FROM (', @kern_alt, ' fetch next 51 rows only) AS t');
PREPARE p FROM @q; SELECT SYSDATE(6) INTO @t0; EXECUTE p; SELECT ROUND(TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6))/1000,3) AS wanduhrMs; DEALLOCATE PREPARE p;
SET @q = CONCAT('SELECT ''neu gedeckelt'' AS form, COUNT(*) AS zeilen, BIT_XOR(CRC32(MessageID)) AS pruefsumme, MIN(MessageID) AS erste, MAX(MessageID) AS letzte FROM (', @kern_neu, ' fetch next 51 rows only) AS t');
PREPARE p FROM @q; SELECT SYSDATE(6) INTO @t0; EXECUTE p; SELECT ROUND(TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6))/1000,3) AS wanduhrMs; DEALLOCATE PREPARE p;
SET @q = CONCAT('SELECT ''alt voll'' AS form, COUNT(*) AS zeilen, BIT_XOR(CRC32(MessageID)) AS pruefsumme, MIN(MessageID) AS erste, MAX(MessageID) AS letzte FROM (', @kern_alt, ') AS t');
PREPARE p FROM @q; SELECT SYSDATE(6) INTO @t0; EXECUTE p; SELECT ROUND(TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6))/1000,3) AS wanduhrMs; DEALLOCATE PREPARE p;
SET @q = CONCAT('SELECT ''neu voll'' AS form, COUNT(*) AS zeilen, BIT_XOR(CRC32(MessageID)) AS pruefsumme, MIN(MessageID) AS erste, MAX(MessageID) AS letzte FROM (', @kern_neu, ') AS t');
PREPARE p FROM @q; SELECT SYSDATE(6) INTO @t0; EXECUTE p; SELECT ROUND(TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6))/1000,3) AS wanduhrMs; DEALLOCATE PREPARE p;

SELECT NOW() AS serverzeitEnde;
