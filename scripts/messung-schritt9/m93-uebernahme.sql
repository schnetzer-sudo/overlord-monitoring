-- M93 — Die Lesung der Vorschlagsuebernahme (E22), GERENDERTER Text.
--
-- Rein lesend: nur SELECT, SET, EXPLAIN, SHOW. Kein INSERT/UPDATE/DDL.
-- Der Text stammt aus ProzessKatalogRepository.findeUebernehmbareVorschlaege,
-- protokolliert gegen eine jOOQ-Attrappe (MockConnection). Einzige Abweichung
-- zum laufenden Code: Die vier Bindeplaetze `?` stehen hier als Literale bzw.
-- als Sitzungsvariable @mandant — dieselbe Abweichung wie in M83 und M84.
SET SESSION max_statement_time = 60;
SET SESSION profiling_history_size = 100;
SET SESSION profiling = 1;

SELECT '=== V0 RAHMEN ===' AS marke;
SELECT @@global.read_only AS global_read_only, VERSION() AS version, NOW() AS serverzeit;
SELECT @@div_precision_increment AS div_precision_increment,
       @@session.sql_mode        AS sql_mode,
       @@max_statement_time      AS max_statement_time_vorgabe,
       @@innodb_buffer_pool_size AS innodb_buffer_pool_size;
SELECT COUNT(*) AS message_zeilen, MAX(MessageLastUpdate) AS datenstand FROM GlassfishDB.Message;

-- ─────────────────────────────────────────────────────────────────────────────
-- M93-0 — Der Katalogstand, bevor irgendetwas gemessen wird.
--
-- Er entscheidet, ob die vorregistrierten Erwartungen ueberhaupt erreichbar
-- sind: Die Bedingung aus E22 verlangt OFFEN, und der Lauf ruehrt gepflegte
-- Zeilen nie an (E13).
-- ─────────────────────────────────────────────────────────────────────────────
SELECT '=== M93-0 KATALOGSTAND JE MANDANT ===' AS marke;
SELECT pm.MandantID                                     AS mandant,
       COUNT(*)                                         AS katalogzeilen,
       SUM(pc.pflegestatus = 'OFFEN')                   AS offen,
       SUM(pc.pflegestatus = 'GEPFLEGT')                AS gepflegt,
       SUM(pc.vorschlag_herkunft = 'REGEL_A')           AS regel_a_gesamt,
       SUM(pc.vorschlag_herkunft = 'REGEL_B')           AS regel_b_gesamt,
       COUNT(DISTINCT pc.geaendert_von)                 AS anfasser,
       MAX(pc.geaendert_am)                             AS zuletzt_geaendert
  FROM overlord_monitor.process_catalog pc
  JOIN GlassfishDB.Process p         ON p.ProcessID  = pc.process_id
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
 GROUP BY pm.MandantID
 ORDER BY katalogzeilen DESC;

SELECT '=== M93-0b PROZESSZAHL JE MANDANT (Nenner) ===' AS marke;
SELECT pm.MandantID AS mandant, COUNT(*) AS prozesse
  FROM GlassfishDB.ProjectMandant pm
  JOIN GlassfishDB.Process p ON p.ProjectID = pm.ProjectID
 WHERE pm.MandantID IN ('NEXANS', 'IBISGUS', 'VOTG', 'IBIS')
 GROUP BY pm.MandantID
 ORDER BY prozesse DESC;

-- ─────────────────────────────────────────────────────────────────────────────
-- M93-1 — NEXANS (733 Prozesse, groesster Bestand). Der Auftragsmandant.
-- ─────────────────────────────────────────────────────────────────────────────
SET @mandant = 'NEXANS';

SELECT '=== M93-1 EXPLAIN NEXANS ===' AS marke;
EXPLAIN
select `GlassfishDB`.`Process`.`ProcessID`, `overlord_monitor`.`process_catalog`.`vorschlag_herkunft` from `GlassfishDB`.`ProjectMandant` join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProjectID` = `GlassfishDB`.`ProjectMandant`.`ProjectID` join `overlord_monitor`.`process_catalog` on `overlord_monitor`.`process_catalog`.`process_id` = `GlassfishDB`.`Process`.`ProcessID` where (`GlassfishDB`.`ProjectMandant`.`MandantID` = @mandant and `overlord_monitor`.`process_catalog`.`pflegestatus` = 'OFFEN' and `overlord_monitor`.`process_catalog`.`vorschlag_herkunft` in ('REGEL_A', 'REGEL_B')) order by `GlassfishDB`.`Process`.`ProcessID` asc;

SELECT '=== M93-1 LAEUFE NEXANS (5, sequenziell) ===' AS marke;
select `GlassfishDB`.`Process`.`ProcessID`, `overlord_monitor`.`process_catalog`.`vorschlag_herkunft` from `GlassfishDB`.`ProjectMandant` join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProjectID` = `GlassfishDB`.`ProjectMandant`.`ProjectID` join `overlord_monitor`.`process_catalog` on `overlord_monitor`.`process_catalog`.`process_id` = `GlassfishDB`.`Process`.`ProcessID` where (`GlassfishDB`.`ProjectMandant`.`MandantID` = @mandant and `overlord_monitor`.`process_catalog`.`pflegestatus` = 'OFFEN' and `overlord_monitor`.`process_catalog`.`vorschlag_herkunft` in ('REGEL_A', 'REGEL_B')) order by `GlassfishDB`.`Process`.`ProcessID` asc;
select `GlassfishDB`.`Process`.`ProcessID`, `overlord_monitor`.`process_catalog`.`vorschlag_herkunft` from `GlassfishDB`.`ProjectMandant` join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProjectID` = `GlassfishDB`.`ProjectMandant`.`ProjectID` join `overlord_monitor`.`process_catalog` on `overlord_monitor`.`process_catalog`.`process_id` = `GlassfishDB`.`Process`.`ProcessID` where (`GlassfishDB`.`ProjectMandant`.`MandantID` = @mandant and `overlord_monitor`.`process_catalog`.`pflegestatus` = 'OFFEN' and `overlord_monitor`.`process_catalog`.`vorschlag_herkunft` in ('REGEL_A', 'REGEL_B')) order by `GlassfishDB`.`Process`.`ProcessID` asc;
select `GlassfishDB`.`Process`.`ProcessID`, `overlord_monitor`.`process_catalog`.`vorschlag_herkunft` from `GlassfishDB`.`ProjectMandant` join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProjectID` = `GlassfishDB`.`ProjectMandant`.`ProjectID` join `overlord_monitor`.`process_catalog` on `overlord_monitor`.`process_catalog`.`process_id` = `GlassfishDB`.`Process`.`ProcessID` where (`GlassfishDB`.`ProjectMandant`.`MandantID` = @mandant and `overlord_monitor`.`process_catalog`.`pflegestatus` = 'OFFEN' and `overlord_monitor`.`process_catalog`.`vorschlag_herkunft` in ('REGEL_A', 'REGEL_B')) order by `GlassfishDB`.`Process`.`ProcessID` asc;
select `GlassfishDB`.`Process`.`ProcessID`, `overlord_monitor`.`process_catalog`.`vorschlag_herkunft` from `GlassfishDB`.`ProjectMandant` join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProjectID` = `GlassfishDB`.`ProjectMandant`.`ProjectID` join `overlord_monitor`.`process_catalog` on `overlord_monitor`.`process_catalog`.`process_id` = `GlassfishDB`.`Process`.`ProcessID` where (`GlassfishDB`.`ProjectMandant`.`MandantID` = @mandant and `overlord_monitor`.`process_catalog`.`pflegestatus` = 'OFFEN' and `overlord_monitor`.`process_catalog`.`vorschlag_herkunft` in ('REGEL_A', 'REGEL_B')) order by `GlassfishDB`.`Process`.`ProcessID` asc;
select `GlassfishDB`.`Process`.`ProcessID`, `overlord_monitor`.`process_catalog`.`vorschlag_herkunft` from `GlassfishDB`.`ProjectMandant` join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProjectID` = `GlassfishDB`.`ProjectMandant`.`ProjectID` join `overlord_monitor`.`process_catalog` on `overlord_monitor`.`process_catalog`.`process_id` = `GlassfishDB`.`Process`.`ProcessID` where (`GlassfishDB`.`ProjectMandant`.`MandantID` = @mandant and `overlord_monitor`.`process_catalog`.`pflegestatus` = 'OFFEN' and `overlord_monitor`.`process_catalog`.`vorschlag_herkunft` in ('REGEL_A', 'REGEL_B')) order by `GlassfishDB`.`Process`.`ProcessID` asc;

-- ─────────────────────────────────────────────────────────────────────────────
-- M93-2 — IBISGUS (89 Prozesse, kleiner Bestand). Der zweite Auftragsmandant,
-- Regel L7.
-- ─────────────────────────────────────────────────────────────────────────────
SET @mandant = 'IBISGUS';

SELECT '=== M93-2 EXPLAIN IBISGUS ===' AS marke;
EXPLAIN
select `GlassfishDB`.`Process`.`ProcessID`, `overlord_monitor`.`process_catalog`.`vorschlag_herkunft` from `GlassfishDB`.`ProjectMandant` join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProjectID` = `GlassfishDB`.`ProjectMandant`.`ProjectID` join `overlord_monitor`.`process_catalog` on `overlord_monitor`.`process_catalog`.`process_id` = `GlassfishDB`.`Process`.`ProcessID` where (`GlassfishDB`.`ProjectMandant`.`MandantID` = @mandant and `overlord_monitor`.`process_catalog`.`pflegestatus` = 'OFFEN' and `overlord_monitor`.`process_catalog`.`vorschlag_herkunft` in ('REGEL_A', 'REGEL_B')) order by `GlassfishDB`.`Process`.`ProcessID` asc;

SELECT '=== M93-2 LAEUFE IBISGUS (5, sequenziell) ===' AS marke;
select `GlassfishDB`.`Process`.`ProcessID`, `overlord_monitor`.`process_catalog`.`vorschlag_herkunft` from `GlassfishDB`.`ProjectMandant` join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProjectID` = `GlassfishDB`.`ProjectMandant`.`ProjectID` join `overlord_monitor`.`process_catalog` on `overlord_monitor`.`process_catalog`.`process_id` = `GlassfishDB`.`Process`.`ProcessID` where (`GlassfishDB`.`ProjectMandant`.`MandantID` = @mandant and `overlord_monitor`.`process_catalog`.`pflegestatus` = 'OFFEN' and `overlord_monitor`.`process_catalog`.`vorschlag_herkunft` in ('REGEL_A', 'REGEL_B')) order by `GlassfishDB`.`Process`.`ProcessID` asc;
select `GlassfishDB`.`Process`.`ProcessID`, `overlord_monitor`.`process_catalog`.`vorschlag_herkunft` from `GlassfishDB`.`ProjectMandant` join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProjectID` = `GlassfishDB`.`ProjectMandant`.`ProjectID` join `overlord_monitor`.`process_catalog` on `overlord_monitor`.`process_catalog`.`process_id` = `GlassfishDB`.`Process`.`ProcessID` where (`GlassfishDB`.`ProjectMandant`.`MandantID` = @mandant and `overlord_monitor`.`process_catalog`.`pflegestatus` = 'OFFEN' and `overlord_monitor`.`process_catalog`.`vorschlag_herkunft` in ('REGEL_A', 'REGEL_B')) order by `GlassfishDB`.`Process`.`ProcessID` asc;
select `GlassfishDB`.`Process`.`ProcessID`, `overlord_monitor`.`process_catalog`.`vorschlag_herkunft` from `GlassfishDB`.`ProjectMandant` join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProjectID` = `GlassfishDB`.`ProjectMandant`.`ProjectID` join `overlord_monitor`.`process_catalog` on `overlord_monitor`.`process_catalog`.`process_id` = `GlassfishDB`.`Process`.`ProcessID` where (`GlassfishDB`.`ProjectMandant`.`MandantID` = @mandant and `overlord_monitor`.`process_catalog`.`pflegestatus` = 'OFFEN' and `overlord_monitor`.`process_catalog`.`vorschlag_herkunft` in ('REGEL_A', 'REGEL_B')) order by `GlassfishDB`.`Process`.`ProcessID` asc;
select `GlassfishDB`.`Process`.`ProcessID`, `overlord_monitor`.`process_catalog`.`vorschlag_herkunft` from `GlassfishDB`.`ProjectMandant` join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProjectID` = `GlassfishDB`.`ProjectMandant`.`ProjectID` join `overlord_monitor`.`process_catalog` on `overlord_monitor`.`process_catalog`.`process_id` = `GlassfishDB`.`Process`.`ProcessID` where (`GlassfishDB`.`ProjectMandant`.`MandantID` = @mandant and `overlord_monitor`.`process_catalog`.`pflegestatus` = 'OFFEN' and `overlord_monitor`.`process_catalog`.`vorschlag_herkunft` in ('REGEL_A', 'REGEL_B')) order by `GlassfishDB`.`Process`.`ProcessID` asc;
select `GlassfishDB`.`Process`.`ProcessID`, `overlord_monitor`.`process_catalog`.`vorschlag_herkunft` from `GlassfishDB`.`ProjectMandant` join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProjectID` = `GlassfishDB`.`ProjectMandant`.`ProjectID` join `overlord_monitor`.`process_catalog` on `overlord_monitor`.`process_catalog`.`process_id` = `GlassfishDB`.`Process`.`ProcessID` where (`GlassfishDB`.`ProjectMandant`.`MandantID` = @mandant and `overlord_monitor`.`process_catalog`.`pflegestatus` = 'OFFEN' and `overlord_monitor`.`process_catalog`.`vorschlag_herkunft` in ('REGEL_A', 'REGEL_B')) order by `GlassfishDB`.`Process`.`ProcessID` asc;

-- ─────────────────────────────────────────────────────────────────────────────
-- M93-3 — VOTG (390 Prozesse). Der Mandant, bei dem die Bedingung im aktuellen
-- Katalogstand wirklich greift: 390 offene Zeilen, 378 davon aus Regel A.
-- ─────────────────────────────────────────────────────────────────────────────
SET @mandant = 'VOTG';

SELECT '=== M93-3 EXPLAIN VOTG ===' AS marke;
EXPLAIN
select `GlassfishDB`.`Process`.`ProcessID`, `overlord_monitor`.`process_catalog`.`vorschlag_herkunft` from `GlassfishDB`.`ProjectMandant` join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProjectID` = `GlassfishDB`.`ProjectMandant`.`ProjectID` join `overlord_monitor`.`process_catalog` on `overlord_monitor`.`process_catalog`.`process_id` = `GlassfishDB`.`Process`.`ProcessID` where (`GlassfishDB`.`ProjectMandant`.`MandantID` = @mandant and `overlord_monitor`.`process_catalog`.`pflegestatus` = 'OFFEN' and `overlord_monitor`.`process_catalog`.`vorschlag_herkunft` in ('REGEL_A', 'REGEL_B')) order by `GlassfishDB`.`Process`.`ProcessID` asc;

SELECT '=== M93-3 LAEUFE VOTG (5, sequenziell) ===' AS marke;
select `GlassfishDB`.`Process`.`ProcessID`, `overlord_monitor`.`process_catalog`.`vorschlag_herkunft` from `GlassfishDB`.`ProjectMandant` join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProjectID` = `GlassfishDB`.`ProjectMandant`.`ProjectID` join `overlord_monitor`.`process_catalog` on `overlord_monitor`.`process_catalog`.`process_id` = `GlassfishDB`.`Process`.`ProcessID` where (`GlassfishDB`.`ProjectMandant`.`MandantID` = @mandant and `overlord_monitor`.`process_catalog`.`pflegestatus` = 'OFFEN' and `overlord_monitor`.`process_catalog`.`vorschlag_herkunft` in ('REGEL_A', 'REGEL_B')) order by `GlassfishDB`.`Process`.`ProcessID` asc;
select `GlassfishDB`.`Process`.`ProcessID`, `overlord_monitor`.`process_catalog`.`vorschlag_herkunft` from `GlassfishDB`.`ProjectMandant` join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProjectID` = `GlassfishDB`.`ProjectMandant`.`ProjectID` join `overlord_monitor`.`process_catalog` on `overlord_monitor`.`process_catalog`.`process_id` = `GlassfishDB`.`Process`.`ProcessID` where (`GlassfishDB`.`ProjectMandant`.`MandantID` = @mandant and `overlord_monitor`.`process_catalog`.`pflegestatus` = 'OFFEN' and `overlord_monitor`.`process_catalog`.`vorschlag_herkunft` in ('REGEL_A', 'REGEL_B')) order by `GlassfishDB`.`Process`.`ProcessID` asc;
select `GlassfishDB`.`Process`.`ProcessID`, `overlord_monitor`.`process_catalog`.`vorschlag_herkunft` from `GlassfishDB`.`ProjectMandant` join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProjectID` = `GlassfishDB`.`ProjectMandant`.`ProjectID` join `overlord_monitor`.`process_catalog` on `overlord_monitor`.`process_catalog`.`process_id` = `GlassfishDB`.`Process`.`ProcessID` where (`GlassfishDB`.`ProjectMandant`.`MandantID` = @mandant and `overlord_monitor`.`process_catalog`.`pflegestatus` = 'OFFEN' and `overlord_monitor`.`process_catalog`.`vorschlag_herkunft` in ('REGEL_A', 'REGEL_B')) order by `GlassfishDB`.`Process`.`ProcessID` asc;
select `GlassfishDB`.`Process`.`ProcessID`, `overlord_monitor`.`process_catalog`.`vorschlag_herkunft` from `GlassfishDB`.`ProjectMandant` join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProjectID` = `GlassfishDB`.`ProjectMandant`.`ProjectID` join `overlord_monitor`.`process_catalog` on `overlord_monitor`.`process_catalog`.`process_id` = `GlassfishDB`.`Process`.`ProcessID` where (`GlassfishDB`.`ProjectMandant`.`MandantID` = @mandant and `overlord_monitor`.`process_catalog`.`pflegestatus` = 'OFFEN' and `overlord_monitor`.`process_catalog`.`vorschlag_herkunft` in ('REGEL_A', 'REGEL_B')) order by `GlassfishDB`.`Process`.`ProcessID` asc;
select `GlassfishDB`.`Process`.`ProcessID`, `overlord_monitor`.`process_catalog`.`vorschlag_herkunft` from `GlassfishDB`.`ProjectMandant` join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProjectID` = `GlassfishDB`.`ProjectMandant`.`ProjectID` join `overlord_monitor`.`process_catalog` on `overlord_monitor`.`process_catalog`.`process_id` = `GlassfishDB`.`Process`.`ProcessID` where (`GlassfishDB`.`ProjectMandant`.`MandantID` = @mandant and `overlord_monitor`.`process_catalog`.`pflegestatus` = 'OFFEN' and `overlord_monitor`.`process_catalog`.`vorschlag_herkunft` in ('REGEL_A', 'REGEL_B')) order by `GlassfishDB`.`Process`.`ProcessID` asc;

-- ─────────────────────────────────────────────────────────────────────────────
-- M93-4 — IBIS (192 Prozesse). Der Mandant mit GEMISCHTEM Stand: 169 offene und
-- 23 gepflegte Zeilen, 171 davon aus Regel B. Hier muss E22 die gepflegten
-- ausschliessen.
-- ─────────────────────────────────────────────────────────────────────────────
SET @mandant = 'IBIS';

SELECT '=== M93-4 EXPLAIN IBIS ===' AS marke;
EXPLAIN
select `GlassfishDB`.`Process`.`ProcessID`, `overlord_monitor`.`process_catalog`.`vorschlag_herkunft` from `GlassfishDB`.`ProjectMandant` join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProjectID` = `GlassfishDB`.`ProjectMandant`.`ProjectID` join `overlord_monitor`.`process_catalog` on `overlord_monitor`.`process_catalog`.`process_id` = `GlassfishDB`.`Process`.`ProcessID` where (`GlassfishDB`.`ProjectMandant`.`MandantID` = @mandant and `overlord_monitor`.`process_catalog`.`pflegestatus` = 'OFFEN' and `overlord_monitor`.`process_catalog`.`vorschlag_herkunft` in ('REGEL_A', 'REGEL_B')) order by `GlassfishDB`.`Process`.`ProcessID` asc;

SELECT '=== M93-4 LAEUFE IBIS (5, sequenziell) ===' AS marke;
select `GlassfishDB`.`Process`.`ProcessID`, `overlord_monitor`.`process_catalog`.`vorschlag_herkunft` from `GlassfishDB`.`ProjectMandant` join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProjectID` = `GlassfishDB`.`ProjectMandant`.`ProjectID` join `overlord_monitor`.`process_catalog` on `overlord_monitor`.`process_catalog`.`process_id` = `GlassfishDB`.`Process`.`ProcessID` where (`GlassfishDB`.`ProjectMandant`.`MandantID` = @mandant and `overlord_monitor`.`process_catalog`.`pflegestatus` = 'OFFEN' and `overlord_monitor`.`process_catalog`.`vorschlag_herkunft` in ('REGEL_A', 'REGEL_B')) order by `GlassfishDB`.`Process`.`ProcessID` asc;
select `GlassfishDB`.`Process`.`ProcessID`, `overlord_monitor`.`process_catalog`.`vorschlag_herkunft` from `GlassfishDB`.`ProjectMandant` join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProjectID` = `GlassfishDB`.`ProjectMandant`.`ProjectID` join `overlord_monitor`.`process_catalog` on `overlord_monitor`.`process_catalog`.`process_id` = `GlassfishDB`.`Process`.`ProcessID` where (`GlassfishDB`.`ProjectMandant`.`MandantID` = @mandant and `overlord_monitor`.`process_catalog`.`pflegestatus` = 'OFFEN' and `overlord_monitor`.`process_catalog`.`vorschlag_herkunft` in ('REGEL_A', 'REGEL_B')) order by `GlassfishDB`.`Process`.`ProcessID` asc;
select `GlassfishDB`.`Process`.`ProcessID`, `overlord_monitor`.`process_catalog`.`vorschlag_herkunft` from `GlassfishDB`.`ProjectMandant` join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProjectID` = `GlassfishDB`.`ProjectMandant`.`ProjectID` join `overlord_monitor`.`process_catalog` on `overlord_monitor`.`process_catalog`.`process_id` = `GlassfishDB`.`Process`.`ProcessID` where (`GlassfishDB`.`ProjectMandant`.`MandantID` = @mandant and `overlord_monitor`.`process_catalog`.`pflegestatus` = 'OFFEN' and `overlord_monitor`.`process_catalog`.`vorschlag_herkunft` in ('REGEL_A', 'REGEL_B')) order by `GlassfishDB`.`Process`.`ProcessID` asc;
select `GlassfishDB`.`Process`.`ProcessID`, `overlord_monitor`.`process_catalog`.`vorschlag_herkunft` from `GlassfishDB`.`ProjectMandant` join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProjectID` = `GlassfishDB`.`ProjectMandant`.`ProjectID` join `overlord_monitor`.`process_catalog` on `overlord_monitor`.`process_catalog`.`process_id` = `GlassfishDB`.`Process`.`ProcessID` where (`GlassfishDB`.`ProjectMandant`.`MandantID` = @mandant and `overlord_monitor`.`process_catalog`.`pflegestatus` = 'OFFEN' and `overlord_monitor`.`process_catalog`.`vorschlag_herkunft` in ('REGEL_A', 'REGEL_B')) order by `GlassfishDB`.`Process`.`ProcessID` asc;
select `GlassfishDB`.`Process`.`ProcessID`, `overlord_monitor`.`process_catalog`.`vorschlag_herkunft` from `GlassfishDB`.`ProjectMandant` join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProjectID` = `GlassfishDB`.`ProjectMandant`.`ProjectID` join `overlord_monitor`.`process_catalog` on `overlord_monitor`.`process_catalog`.`process_id` = `GlassfishDB`.`Process`.`ProcessID` where (`GlassfishDB`.`ProjectMandant`.`MandantID` = @mandant and `overlord_monitor`.`process_catalog`.`pflegestatus` = 'OFFEN' and `overlord_monitor`.`process_catalog`.`vorschlag_herkunft` in ('REGEL_A', 'REGEL_B')) order by `GlassfishDB`.`Process`.`ProcessID` asc;

-- ─────────────────────────────────────────────────────────────────────────────
-- M93-5 — Die Zahlen, die der Endpunkt melden wird: betroffen, regelA, regelB.
-- Aufbau wie im Dienst: dieselbe Bedingung, nur aggregiert statt aufgezaehlt.
-- ─────────────────────────────────────────────────────────────────────────────
SELECT '=== M93-5 BETROFFEN JE MANDANT ===' AS marke;
SELECT pm.MandantID                                 AS mandant,
       COUNT(*)                                     AS betroffen,
       SUM(pc.vorschlag_herkunft = 'REGEL_A')       AS regel_a,
       SUM(pc.vorschlag_herkunft = 'REGEL_B')       AS regel_b,
       COUNT(DISTINCT p.ProcessID)                  AS verschiedene_prozesse
  FROM GlassfishDB.ProjectMandant pm
  JOIN GlassfishDB.Process p                  ON p.ProjectID  = pm.ProjectID
  JOIN overlord_monitor.process_catalog pc    ON pc.process_id = p.ProcessID
 WHERE pc.pflegestatus = 'OFFEN'
   AND pc.vorschlag_herkunft IN ('REGEL_A', 'REGEL_B')
 GROUP BY pm.MandantID
 ORDER BY betroffen DESC;

-- Gegenprobe zur Einebnung: Ein LEFT JOIN duerfte HIER dieselbe Zahl liefern —
-- die NULL-Herkunft faellt an der IN-Bedingung ohnehin heraus. Der INNER JOIN
-- ist die ehrlichere Schreibweise und nicht die engere Bedingung.
SELECT '=== M93-5b GEGENPROBE LEFT JOIN ===' AS marke;
SELECT pm.MandantID AS mandant, COUNT(*) AS betroffen_mit_left_join
  FROM GlassfishDB.ProjectMandant pm
  JOIN GlassfishDB.Process p                       ON p.ProjectID  = pm.ProjectID
  LEFT JOIN overlord_monitor.process_catalog pc    ON pc.process_id = p.ProcessID
 WHERE pc.pflegestatus = 'OFFEN'
   AND pc.vorschlag_herkunft IN ('REGEL_A', 'REGEL_B')
 GROUP BY pm.MandantID
 ORDER BY betroffen_mit_left_join DESC;

-- Und die Zahl, die E22 ausdruecklich NICHT mitnimmt: offene Zeilen mit
-- Herkunft KEINE, die trotzdem eine Richtung tragen.
SELECT '=== M93-5c OFFEN, KEINE HERKUNFT, MIT RICHTUNG ===' AS marke;
SELECT pm.MandantID AS mandant,
       COUNT(*)     AS offen_ohne_partnervorschlag,
       SUM(pc.richtung IS NOT NULL) AS davon_mit_richtung
  FROM GlassfishDB.ProjectMandant pm
  JOIN GlassfishDB.Process p               ON p.ProjectID   = pm.ProjectID
  JOIN overlord_monitor.process_catalog pc ON pc.process_id = p.ProcessID
 WHERE pc.pflegestatus = 'OFFEN'
   AND pc.vorschlag_herkunft = 'KEINE'
 GROUP BY pm.MandantID
 ORDER BY offen_ohne_partnervorschlag DESC;

SET SESSION profiling = 0;
SELECT '=== PROFILES ===' AS marke;
SHOW PROFILES;

SELECT '=== V9 ABSCHLUSS ===' AS marke;
SELECT @@global.read_only AS global_read_only, NOW() AS serverzeit_ende;
