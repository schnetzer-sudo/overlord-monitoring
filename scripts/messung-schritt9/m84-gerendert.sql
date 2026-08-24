-- M84 — Der GERENDERTE Text des Bestandslaufs (E14), Fassung A.
-- Rein lesend: nur SELECT, SET, EXPLAIN, SHOW. Kein INSERT/UPDATE/DDL.
SET SESSION max_statement_time = 60;
SET SESSION profiling_history_size = 100;
SET SESSION profiling = 1;

SELECT '=== RAHMEN ===' AS marke;
SELECT @@global.read_only AS global_read_only, VERSION() AS version, NOW() AS serverzeit;
SELECT COUNT(*) AS message_zeilen, MAX(MessageLastUpdate) AS datenstand FROM GlassfishDB.Message;

SET @mandant = 'NEXANS';

SELECT '=== EXPLAIN NEXANS (gerendert) ===' AS marke;
EXPLAIN
select `GlassfishDB`.`Process`.`ProcessID`, exists (select 1 as `one` from `GlassfishDB`.`Message` where `GlassfishDB`.`Message`.`ProcessID` = `GlassfishDB`.`Process`.`ProcessID`) from `GlassfishDB`.`ProjectMandant` join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProjectID` = `GlassfishDB`.`ProjectMandant`.`ProjectID` where `GlassfishDB`.`ProjectMandant`.`MandantID` = @mandant;

SELECT '=== LAEUFE NEXANS ===' AS marke;
select `GlassfishDB`.`Process`.`ProcessID`, exists (select 1 as `one` from `GlassfishDB`.`Message` where `GlassfishDB`.`Message`.`ProcessID` = `GlassfishDB`.`Process`.`ProcessID`) from `GlassfishDB`.`ProjectMandant` join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProjectID` = `GlassfishDB`.`ProjectMandant`.`ProjectID` where `GlassfishDB`.`ProjectMandant`.`MandantID` = @mandant;
select `GlassfishDB`.`Process`.`ProcessID`, exists (select 1 as `one` from `GlassfishDB`.`Message` where `GlassfishDB`.`Message`.`ProcessID` = `GlassfishDB`.`Process`.`ProcessID`) from `GlassfishDB`.`ProjectMandant` join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProjectID` = `GlassfishDB`.`ProjectMandant`.`ProjectID` where `GlassfishDB`.`ProjectMandant`.`MandantID` = @mandant;
select `GlassfishDB`.`Process`.`ProcessID`, exists (select 1 as `one` from `GlassfishDB`.`Message` where `GlassfishDB`.`Message`.`ProcessID` = `GlassfishDB`.`Process`.`ProcessID`) from `GlassfishDB`.`ProjectMandant` join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProjectID` = `GlassfishDB`.`ProjectMandant`.`ProjectID` where `GlassfishDB`.`ProjectMandant`.`MandantID` = @mandant;
select `GlassfishDB`.`Process`.`ProcessID`, exists (select 1 as `one` from `GlassfishDB`.`Message` where `GlassfishDB`.`Message`.`ProcessID` = `GlassfishDB`.`Process`.`ProcessID`) from `GlassfishDB`.`ProjectMandant` join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProjectID` = `GlassfishDB`.`ProjectMandant`.`ProjectID` where `GlassfishDB`.`ProjectMandant`.`MandantID` = @mandant;
select `GlassfishDB`.`Process`.`ProcessID`, exists (select 1 as `one` from `GlassfishDB`.`Message` where `GlassfishDB`.`Message`.`ProcessID` = `GlassfishDB`.`Process`.`ProcessID`) from `GlassfishDB`.`ProjectMandant` join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProjectID` = `GlassfishDB`.`ProjectMandant`.`ProjectID` where `GlassfishDB`.`ProjectMandant`.`MandantID` = @mandant;

SET @mandant = 'SUTTONS';

SELECT '=== EXPLAIN SUTTONS (gerendert) ===' AS marke;
EXPLAIN
select `GlassfishDB`.`Process`.`ProcessID`, exists (select 1 as `one` from `GlassfishDB`.`Message` where `GlassfishDB`.`Message`.`ProcessID` = `GlassfishDB`.`Process`.`ProcessID`) from `GlassfishDB`.`ProjectMandant` join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProjectID` = `GlassfishDB`.`ProjectMandant`.`ProjectID` where `GlassfishDB`.`ProjectMandant`.`MandantID` = @mandant;

SELECT '=== LAEUFE SUTTONS ===' AS marke;
select `GlassfishDB`.`Process`.`ProcessID`, exists (select 1 as `one` from `GlassfishDB`.`Message` where `GlassfishDB`.`Message`.`ProcessID` = `GlassfishDB`.`Process`.`ProcessID`) from `GlassfishDB`.`ProjectMandant` join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProjectID` = `GlassfishDB`.`ProjectMandant`.`ProjectID` where `GlassfishDB`.`ProjectMandant`.`MandantID` = @mandant;
select `GlassfishDB`.`Process`.`ProcessID`, exists (select 1 as `one` from `GlassfishDB`.`Message` where `GlassfishDB`.`Message`.`ProcessID` = `GlassfishDB`.`Process`.`ProcessID`) from `GlassfishDB`.`ProjectMandant` join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProjectID` = `GlassfishDB`.`ProjectMandant`.`ProjectID` where `GlassfishDB`.`ProjectMandant`.`MandantID` = @mandant;
select `GlassfishDB`.`Process`.`ProcessID`, exists (select 1 as `one` from `GlassfishDB`.`Message` where `GlassfishDB`.`Message`.`ProcessID` = `GlassfishDB`.`Process`.`ProcessID`) from `GlassfishDB`.`ProjectMandant` join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProjectID` = `GlassfishDB`.`ProjectMandant`.`ProjectID` where `GlassfishDB`.`ProjectMandant`.`MandantID` = @mandant;
select `GlassfishDB`.`Process`.`ProcessID`, exists (select 1 as `one` from `GlassfishDB`.`Message` where `GlassfishDB`.`Message`.`ProcessID` = `GlassfishDB`.`Process`.`ProcessID`) from `GlassfishDB`.`ProjectMandant` join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProjectID` = `GlassfishDB`.`ProjectMandant`.`ProjectID` where `GlassfishDB`.`ProjectMandant`.`MandantID` = @mandant;
select `GlassfishDB`.`Process`.`ProcessID`, exists (select 1 as `one` from `GlassfishDB`.`Message` where `GlassfishDB`.`Message`.`ProcessID` = `GlassfishDB`.`Process`.`ProcessID`) from `GlassfishDB`.`ProjectMandant` join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProjectID` = `GlassfishDB`.`ProjectMandant`.`ProjectID` where `GlassfishDB`.`ProjectMandant`.`MandantID` = @mandant;

SET SESSION profiling = 0;
SELECT '=== PROFILES ===' AS marke;
SHOW PROFILES;
SELECT '=== RAHMEN ENDE ===' AS marke;
SELECT @@global.read_only AS global_read_only;
