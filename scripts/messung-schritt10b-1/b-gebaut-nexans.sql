-- Messung zu Schritt 10b-1, Teil B: der GEBAUTE Parameter `ueberfaellig` · NEXANS
-- Auftrag:  "Schritt 10b-1", Stand 27.08.2026, Teil B
-- Ergebnis: docs/nachrichtenliste.md §5b
--
-- Ausschliesslich SELECT / SET. (Regel S1)
--
-- **Die Statements sind nicht abgeschrieben.** Sie sind die von jOOQ gerenderten —
-- aus `NachrichtenRepository.finde` gegen eine Attrappe abgegriffen und hier nur in die
-- Messhuelle gepackt. Regel L7 verlangt die Messung DER Abfrage, nicht einer aehnlichen;
-- `NachrichtenStatementsTest` haelt denselben Text maschinell fest.
--
-- Zwei Unterschiede zu M97, beide ohne Wirkung auf den Plan:
--   * jOOQ schreibt `date_add(x, INTERVAL y SECOND)`, M97 `x + INTERVAL y SECOND`.
--     MariaDB bildet beides auf dieselbe Funktion ab.
--   * jOOQ schreibt `FETCH NEXT 51 ROWS ONLY`, M97 `LIMIT 51`. Dasselbe.
--
-- G1: Cursorzeitpunkt und -kennung stehen als Sitzungsvariablen und werden deterministisch
--     als 50. Zeile der ersten Seite hergeleitet; das OFFSET dabei ist Herleitung und kein
--     Blaettern. Laufzeiten aus `information_schema.PROFILING`, nicht aus SHOW PROFILES.
-- Z1: Anker 2025-12-30 04:09:47 als Literal — genau der, den die Anwendungsuhr im Profil dev liefert.

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;

SET max_statement_time = 60;
SET profiling = 1;
SET profiling_history_size = 100;

-- ── Cursor herleiten ────────────────────────────────────────────────────────
SET @c_ts = NULL, @c_id = NULL, @u_ts = NULL, @u_id = NULL;

SELECT m.MessageLastUpdate, m.MessageID INTO @c_ts, @c_id
FROM GlassfishDB.Message m
WHERE m.MessageLastUpdate >= '2025-11-30 04:09:47'
  AND m.MessageLastUpdate <= '2025-12-30 04:09:47'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
              WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
LIMIT 49, 1;

SELECT m.MessageLastUpdate, m.MessageID INTO @u_ts, @u_id
FROM GlassfishDB.Message m
WHERE m.MessageLastUpdate >= '2025-11-30 04:09:47'
  AND m.MessageLastUpdate <= '2025-12-30 04:09:47'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
              WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
  AND m.MessageStatus IN ('RUNNING','SUSPENDED')
  AND m.MessageTimeout IS NOT NULL AND m.MessageTimeout > 0
  AND DATE_ADD(m.MessageLastUpdate, INTERVAL m.MessageTimeout SECOND) < '2025-12-30 04:09:47'
ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
LIMIT 49, 1;

-- Hat die ueberfaellige Liste keine 50 Zeilen, faellt ihr Cursor auf den der
-- ungefilterten zurueck (bei SUTTONS ist das der Fall — er hat keine einzige).
SET @u_ts = COALESCE(@u_ts, @c_ts), @u_id = COALESCE(@u_id, @c_id);

SELECT '=== Cursor gesetzt (ohne Kennungen) ===' AS marke;
SELECT @c_ts AS cursor_liste_zeitpunkt, @c_id IS NOT NULL AS cursor_liste_vorhanden,
       @u_ts AS cursor_ueberfaellig_zeitpunkt, @u_id IS NOT NULL AS cursor_u_vorhanden,
       (@u_id <=> @c_id) AS ueberfaellig_nutzt_denselben_cursor;


-- ==========================================================================
-- GEBAUT-NEXANS-referenz-ohneCursor
-- ==========================================================================
SELECT '=== GEBAUT-NEXANS-referenz-ohneCursor · Plan des gerenderten Statements (L15) ===' AS marke;
EXPLAIN select `GlassfishDB`.`Message`.`MessageID`, `GlassfishDB`.`Message`.`MessageLastUpdate`, `GlassfishDB`.`Message`.`MessageStatus`, `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Process`.`ProcessName`, `GlassfishDB`.`Project`.`ProjectName`, `GlassfishDB`.`SOS`.`SOSName`, `GlassfishDB`.`SOSAction`.`SOSActionName` from `GlassfishDB`.`Message` left outer join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` left outer join `GlassfishDB`.`Project` on `GlassfishDB`.`Project`.`ProjectID` = `GlassfishDB`.`Process`.`ProjectID` left outer join `GlassfishDB`.`SOS` on `GlassfishDB`.`SOS`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` left outer join `GlassfishDB`.`SOSAction` on (`GlassfishDB`.`SOSAction`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` and `GlassfishDB`.`SOSAction`.`SOSActionID` = `GlassfishDB`.`Message`.`SOSActionID`) where (`GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-11-30 04:09:47.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` <= timestamp '2025-12-30 04:09:47.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) order by `GlassfishDB`.`Message`.`MessageLastUpdate` desc, `GlassfishDB`.`Message`.`MessageID` desc fetch next 51 rows only;
SELECT '=== GEBAUT-NEXANS-referenz-ohneCursor · Ergebnis + Aufwaermlauf + fuenf ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT COUNT(*)                         AS zeilen,
       SUM(t.MessageID IS NOT NULL)     AS mit_id,
       SUM(t.ProcessID IS NOT NULL)     AS mit_prozess,
       SUM(t.ProcessName IS NOT NULL)   AS mit_prozessname,
       SUM(t.ProjectName IS NOT NULL)   AS mit_projektname,
       SUM(t.SOSName IS NOT NULL)       AS mit_ablaufname,
       SUM(t.SOSActionName IS NOT NULL) AS mit_schritt,
       COUNT(DISTINCT t.MessageStatus)  AS versch_status,
       MIN(t.MessageLastUpdate)         AS aelteste,
       MAX(t.MessageLastUpdate)         AS juengste
FROM (
select `GlassfishDB`.`Message`.`MessageID`, `GlassfishDB`.`Message`.`MessageLastUpdate`, `GlassfishDB`.`Message`.`MessageStatus`, `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Process`.`ProcessName`, `GlassfishDB`.`Project`.`ProjectName`, `GlassfishDB`.`SOS`.`SOSName`, `GlassfishDB`.`SOSAction`.`SOSActionName` from `GlassfishDB`.`Message` left outer join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` left outer join `GlassfishDB`.`Project` on `GlassfishDB`.`Project`.`ProjectID` = `GlassfishDB`.`Process`.`ProjectID` left outer join `GlassfishDB`.`SOS` on `GlassfishDB`.`SOS`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` left outer join `GlassfishDB`.`SOSAction` on (`GlassfishDB`.`SOSAction`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` and `GlassfishDB`.`SOSAction`.`SOSActionID` = `GlassfishDB`.`Message`.`SOSActionID`) where (`GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-11-30 04:09:47.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` <= timestamp '2025-12-30 04:09:47.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) order by `GlassfishDB`.`Message`.`MessageLastUpdate` desc, `GlassfishDB`.`Message`.`MessageID` desc fetch next 51 rows only
) t;
SELECT COUNT(*)                         AS zeilen,
       SUM(t.MessageID IS NOT NULL)     AS mit_id,
       SUM(t.ProcessID IS NOT NULL)     AS mit_prozess,
       SUM(t.ProcessName IS NOT NULL)   AS mit_prozessname,
       SUM(t.ProjectName IS NOT NULL)   AS mit_projektname,
       SUM(t.SOSName IS NOT NULL)       AS mit_ablaufname,
       SUM(t.SOSActionName IS NOT NULL) AS mit_schritt,
       COUNT(DISTINCT t.MessageStatus)  AS versch_status,
       MIN(t.MessageLastUpdate)         AS aelteste,
       MAX(t.MessageLastUpdate)         AS juengste
FROM (
select `GlassfishDB`.`Message`.`MessageID`, `GlassfishDB`.`Message`.`MessageLastUpdate`, `GlassfishDB`.`Message`.`MessageStatus`, `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Process`.`ProcessName`, `GlassfishDB`.`Project`.`ProjectName`, `GlassfishDB`.`SOS`.`SOSName`, `GlassfishDB`.`SOSAction`.`SOSActionName` from `GlassfishDB`.`Message` left outer join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` left outer join `GlassfishDB`.`Project` on `GlassfishDB`.`Project`.`ProjectID` = `GlassfishDB`.`Process`.`ProjectID` left outer join `GlassfishDB`.`SOS` on `GlassfishDB`.`SOS`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` left outer join `GlassfishDB`.`SOSAction` on (`GlassfishDB`.`SOSAction`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` and `GlassfishDB`.`SOSAction`.`SOSActionID` = `GlassfishDB`.`Message`.`SOSActionID`) where (`GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-11-30 04:09:47.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` <= timestamp '2025-12-30 04:09:47.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) order by `GlassfishDB`.`Message`.`MessageLastUpdate` desc, `GlassfishDB`.`Message`.`MessageID` desc fetch next 51 rows only
) t;
SELECT COUNT(*)                         AS zeilen,
       SUM(t.MessageID IS NOT NULL)     AS mit_id,
       SUM(t.ProcessID IS NOT NULL)     AS mit_prozess,
       SUM(t.ProcessName IS NOT NULL)   AS mit_prozessname,
       SUM(t.ProjectName IS NOT NULL)   AS mit_projektname,
       SUM(t.SOSName IS NOT NULL)       AS mit_ablaufname,
       SUM(t.SOSActionName IS NOT NULL) AS mit_schritt,
       COUNT(DISTINCT t.MessageStatus)  AS versch_status,
       MIN(t.MessageLastUpdate)         AS aelteste,
       MAX(t.MessageLastUpdate)         AS juengste
FROM (
select `GlassfishDB`.`Message`.`MessageID`, `GlassfishDB`.`Message`.`MessageLastUpdate`, `GlassfishDB`.`Message`.`MessageStatus`, `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Process`.`ProcessName`, `GlassfishDB`.`Project`.`ProjectName`, `GlassfishDB`.`SOS`.`SOSName`, `GlassfishDB`.`SOSAction`.`SOSActionName` from `GlassfishDB`.`Message` left outer join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` left outer join `GlassfishDB`.`Project` on `GlassfishDB`.`Project`.`ProjectID` = `GlassfishDB`.`Process`.`ProjectID` left outer join `GlassfishDB`.`SOS` on `GlassfishDB`.`SOS`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` left outer join `GlassfishDB`.`SOSAction` on (`GlassfishDB`.`SOSAction`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` and `GlassfishDB`.`SOSAction`.`SOSActionID` = `GlassfishDB`.`Message`.`SOSActionID`) where (`GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-11-30 04:09:47.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` <= timestamp '2025-12-30 04:09:47.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) order by `GlassfishDB`.`Message`.`MessageLastUpdate` desc, `GlassfishDB`.`Message`.`MessageID` desc fetch next 51 rows only
) t;
SELECT COUNT(*)                         AS zeilen,
       SUM(t.MessageID IS NOT NULL)     AS mit_id,
       SUM(t.ProcessID IS NOT NULL)     AS mit_prozess,
       SUM(t.ProcessName IS NOT NULL)   AS mit_prozessname,
       SUM(t.ProjectName IS NOT NULL)   AS mit_projektname,
       SUM(t.SOSName IS NOT NULL)       AS mit_ablaufname,
       SUM(t.SOSActionName IS NOT NULL) AS mit_schritt,
       COUNT(DISTINCT t.MessageStatus)  AS versch_status,
       MIN(t.MessageLastUpdate)         AS aelteste,
       MAX(t.MessageLastUpdate)         AS juengste
FROM (
select `GlassfishDB`.`Message`.`MessageID`, `GlassfishDB`.`Message`.`MessageLastUpdate`, `GlassfishDB`.`Message`.`MessageStatus`, `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Process`.`ProcessName`, `GlassfishDB`.`Project`.`ProjectName`, `GlassfishDB`.`SOS`.`SOSName`, `GlassfishDB`.`SOSAction`.`SOSActionName` from `GlassfishDB`.`Message` left outer join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` left outer join `GlassfishDB`.`Project` on `GlassfishDB`.`Project`.`ProjectID` = `GlassfishDB`.`Process`.`ProjectID` left outer join `GlassfishDB`.`SOS` on `GlassfishDB`.`SOS`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` left outer join `GlassfishDB`.`SOSAction` on (`GlassfishDB`.`SOSAction`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` and `GlassfishDB`.`SOSAction`.`SOSActionID` = `GlassfishDB`.`Message`.`SOSActionID`) where (`GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-11-30 04:09:47.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` <= timestamp '2025-12-30 04:09:47.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) order by `GlassfishDB`.`Message`.`MessageLastUpdate` desc, `GlassfishDB`.`Message`.`MessageID` desc fetch next 51 rows only
) t;
SELECT COUNT(*)                         AS zeilen,
       SUM(t.MessageID IS NOT NULL)     AS mit_id,
       SUM(t.ProcessID IS NOT NULL)     AS mit_prozess,
       SUM(t.ProcessName IS NOT NULL)   AS mit_prozessname,
       SUM(t.ProjectName IS NOT NULL)   AS mit_projektname,
       SUM(t.SOSName IS NOT NULL)       AS mit_ablaufname,
       SUM(t.SOSActionName IS NOT NULL) AS mit_schritt,
       COUNT(DISTINCT t.MessageStatus)  AS versch_status,
       MIN(t.MessageLastUpdate)         AS aelteste,
       MAX(t.MessageLastUpdate)         AS juengste
FROM (
select `GlassfishDB`.`Message`.`MessageID`, `GlassfishDB`.`Message`.`MessageLastUpdate`, `GlassfishDB`.`Message`.`MessageStatus`, `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Process`.`ProcessName`, `GlassfishDB`.`Project`.`ProjectName`, `GlassfishDB`.`SOS`.`SOSName`, `GlassfishDB`.`SOSAction`.`SOSActionName` from `GlassfishDB`.`Message` left outer join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` left outer join `GlassfishDB`.`Project` on `GlassfishDB`.`Project`.`ProjectID` = `GlassfishDB`.`Process`.`ProjectID` left outer join `GlassfishDB`.`SOS` on `GlassfishDB`.`SOS`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` left outer join `GlassfishDB`.`SOSAction` on (`GlassfishDB`.`SOSAction`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` and `GlassfishDB`.`SOSAction`.`SOSActionID` = `GlassfishDB`.`Message`.`SOSActionID`) where (`GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-11-30 04:09:47.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` <= timestamp '2025-12-30 04:09:47.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) order by `GlassfishDB`.`Message`.`MessageLastUpdate` desc, `GlassfishDB`.`Message`.`MessageID` desc fetch next 51 rows only
) t;
SELECT COUNT(*)                         AS zeilen,
       SUM(t.MessageID IS NOT NULL)     AS mit_id,
       SUM(t.ProcessID IS NOT NULL)     AS mit_prozess,
       SUM(t.ProcessName IS NOT NULL)   AS mit_prozessname,
       SUM(t.ProjectName IS NOT NULL)   AS mit_projektname,
       SUM(t.SOSName IS NOT NULL)       AS mit_ablaufname,
       SUM(t.SOSActionName IS NOT NULL) AS mit_schritt,
       COUNT(DISTINCT t.MessageStatus)  AS versch_status,
       MIN(t.MessageLastUpdate)         AS aelteste,
       MAX(t.MessageLastUpdate)         AS juengste
FROM (
select `GlassfishDB`.`Message`.`MessageID`, `GlassfishDB`.`Message`.`MessageLastUpdate`, `GlassfishDB`.`Message`.`MessageStatus`, `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Process`.`ProcessName`, `GlassfishDB`.`Project`.`ProjectName`, `GlassfishDB`.`SOS`.`SOSName`, `GlassfishDB`.`SOSAction`.`SOSActionName` from `GlassfishDB`.`Message` left outer join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` left outer join `GlassfishDB`.`Project` on `GlassfishDB`.`Project`.`ProjectID` = `GlassfishDB`.`Process`.`ProjectID` left outer join `GlassfishDB`.`SOS` on `GlassfishDB`.`SOS`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` left outer join `GlassfishDB`.`SOSAction` on (`GlassfishDB`.`SOSAction`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` and `GlassfishDB`.`SOSAction`.`SOSActionID` = `GlassfishDB`.`Message`.`SOSActionID`) where (`GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-11-30 04:09:47.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` <= timestamp '2025-12-30 04:09:47.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) order by `GlassfishDB`.`Message`.`MessageLastUpdate` desc, `GlassfishDB`.`Message`.`MessageID` desc fetch next 51 rows only
) t;
SELECT 'GEBAUT-NEXANS-referenz-ohneCursor' AS fall,
       COUNT(*)                                       AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)              AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)              AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;


-- ==========================================================================
-- GEBAUT-NEXANS-referenz-mitCursor
-- ==========================================================================
SELECT '=== GEBAUT-NEXANS-referenz-mitCursor · Plan des gerenderten Statements (L15) ===' AS marke;
EXPLAIN select `GlassfishDB`.`Message`.`MessageID`, `GlassfishDB`.`Message`.`MessageLastUpdate`, `GlassfishDB`.`Message`.`MessageStatus`, `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Process`.`ProcessName`, `GlassfishDB`.`Project`.`ProjectName`, `GlassfishDB`.`SOS`.`SOSName`, `GlassfishDB`.`SOSAction`.`SOSActionName` from `GlassfishDB`.`Message` left outer join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` left outer join `GlassfishDB`.`Project` on `GlassfishDB`.`Project`.`ProjectID` = `GlassfishDB`.`Process`.`ProjectID` left outer join `GlassfishDB`.`SOS` on `GlassfishDB`.`SOS`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` left outer join `GlassfishDB`.`SOSAction` on (`GlassfishDB`.`SOSAction`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` and `GlassfishDB`.`SOSAction`.`SOSActionID` = `GlassfishDB`.`Message`.`SOSActionID`) where (`GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-11-30 04:09:47.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` <= timestamp '2025-12-30 04:09:47.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS')) and (`GlassfishDB`.`Message`.`MessageLastUpdate` < @c_ts or (`GlassfishDB`.`Message`.`MessageLastUpdate` = @c_ts and `GlassfishDB`.`Message`.`MessageID` < @c_id))) order by `GlassfishDB`.`Message`.`MessageLastUpdate` desc, `GlassfishDB`.`Message`.`MessageID` desc fetch next 51 rows only;
SELECT '=== GEBAUT-NEXANS-referenz-mitCursor · Ergebnis + Aufwaermlauf + fuenf ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT COUNT(*)                         AS zeilen,
       SUM(t.MessageID IS NOT NULL)     AS mit_id,
       SUM(t.ProcessID IS NOT NULL)     AS mit_prozess,
       SUM(t.ProcessName IS NOT NULL)   AS mit_prozessname,
       SUM(t.ProjectName IS NOT NULL)   AS mit_projektname,
       SUM(t.SOSName IS NOT NULL)       AS mit_ablaufname,
       SUM(t.SOSActionName IS NOT NULL) AS mit_schritt,
       COUNT(DISTINCT t.MessageStatus)  AS versch_status,
       MIN(t.MessageLastUpdate)         AS aelteste,
       MAX(t.MessageLastUpdate)         AS juengste
FROM (
select `GlassfishDB`.`Message`.`MessageID`, `GlassfishDB`.`Message`.`MessageLastUpdate`, `GlassfishDB`.`Message`.`MessageStatus`, `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Process`.`ProcessName`, `GlassfishDB`.`Project`.`ProjectName`, `GlassfishDB`.`SOS`.`SOSName`, `GlassfishDB`.`SOSAction`.`SOSActionName` from `GlassfishDB`.`Message` left outer join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` left outer join `GlassfishDB`.`Project` on `GlassfishDB`.`Project`.`ProjectID` = `GlassfishDB`.`Process`.`ProjectID` left outer join `GlassfishDB`.`SOS` on `GlassfishDB`.`SOS`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` left outer join `GlassfishDB`.`SOSAction` on (`GlassfishDB`.`SOSAction`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` and `GlassfishDB`.`SOSAction`.`SOSActionID` = `GlassfishDB`.`Message`.`SOSActionID`) where (`GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-11-30 04:09:47.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` <= timestamp '2025-12-30 04:09:47.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS')) and (`GlassfishDB`.`Message`.`MessageLastUpdate` < @c_ts or (`GlassfishDB`.`Message`.`MessageLastUpdate` = @c_ts and `GlassfishDB`.`Message`.`MessageID` < @c_id))) order by `GlassfishDB`.`Message`.`MessageLastUpdate` desc, `GlassfishDB`.`Message`.`MessageID` desc fetch next 51 rows only
) t;
SELECT COUNT(*)                         AS zeilen,
       SUM(t.MessageID IS NOT NULL)     AS mit_id,
       SUM(t.ProcessID IS NOT NULL)     AS mit_prozess,
       SUM(t.ProcessName IS NOT NULL)   AS mit_prozessname,
       SUM(t.ProjectName IS NOT NULL)   AS mit_projektname,
       SUM(t.SOSName IS NOT NULL)       AS mit_ablaufname,
       SUM(t.SOSActionName IS NOT NULL) AS mit_schritt,
       COUNT(DISTINCT t.MessageStatus)  AS versch_status,
       MIN(t.MessageLastUpdate)         AS aelteste,
       MAX(t.MessageLastUpdate)         AS juengste
FROM (
select `GlassfishDB`.`Message`.`MessageID`, `GlassfishDB`.`Message`.`MessageLastUpdate`, `GlassfishDB`.`Message`.`MessageStatus`, `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Process`.`ProcessName`, `GlassfishDB`.`Project`.`ProjectName`, `GlassfishDB`.`SOS`.`SOSName`, `GlassfishDB`.`SOSAction`.`SOSActionName` from `GlassfishDB`.`Message` left outer join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` left outer join `GlassfishDB`.`Project` on `GlassfishDB`.`Project`.`ProjectID` = `GlassfishDB`.`Process`.`ProjectID` left outer join `GlassfishDB`.`SOS` on `GlassfishDB`.`SOS`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` left outer join `GlassfishDB`.`SOSAction` on (`GlassfishDB`.`SOSAction`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` and `GlassfishDB`.`SOSAction`.`SOSActionID` = `GlassfishDB`.`Message`.`SOSActionID`) where (`GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-11-30 04:09:47.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` <= timestamp '2025-12-30 04:09:47.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS')) and (`GlassfishDB`.`Message`.`MessageLastUpdate` < @c_ts or (`GlassfishDB`.`Message`.`MessageLastUpdate` = @c_ts and `GlassfishDB`.`Message`.`MessageID` < @c_id))) order by `GlassfishDB`.`Message`.`MessageLastUpdate` desc, `GlassfishDB`.`Message`.`MessageID` desc fetch next 51 rows only
) t;
SELECT COUNT(*)                         AS zeilen,
       SUM(t.MessageID IS NOT NULL)     AS mit_id,
       SUM(t.ProcessID IS NOT NULL)     AS mit_prozess,
       SUM(t.ProcessName IS NOT NULL)   AS mit_prozessname,
       SUM(t.ProjectName IS NOT NULL)   AS mit_projektname,
       SUM(t.SOSName IS NOT NULL)       AS mit_ablaufname,
       SUM(t.SOSActionName IS NOT NULL) AS mit_schritt,
       COUNT(DISTINCT t.MessageStatus)  AS versch_status,
       MIN(t.MessageLastUpdate)         AS aelteste,
       MAX(t.MessageLastUpdate)         AS juengste
FROM (
select `GlassfishDB`.`Message`.`MessageID`, `GlassfishDB`.`Message`.`MessageLastUpdate`, `GlassfishDB`.`Message`.`MessageStatus`, `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Process`.`ProcessName`, `GlassfishDB`.`Project`.`ProjectName`, `GlassfishDB`.`SOS`.`SOSName`, `GlassfishDB`.`SOSAction`.`SOSActionName` from `GlassfishDB`.`Message` left outer join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` left outer join `GlassfishDB`.`Project` on `GlassfishDB`.`Project`.`ProjectID` = `GlassfishDB`.`Process`.`ProjectID` left outer join `GlassfishDB`.`SOS` on `GlassfishDB`.`SOS`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` left outer join `GlassfishDB`.`SOSAction` on (`GlassfishDB`.`SOSAction`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` and `GlassfishDB`.`SOSAction`.`SOSActionID` = `GlassfishDB`.`Message`.`SOSActionID`) where (`GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-11-30 04:09:47.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` <= timestamp '2025-12-30 04:09:47.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS')) and (`GlassfishDB`.`Message`.`MessageLastUpdate` < @c_ts or (`GlassfishDB`.`Message`.`MessageLastUpdate` = @c_ts and `GlassfishDB`.`Message`.`MessageID` < @c_id))) order by `GlassfishDB`.`Message`.`MessageLastUpdate` desc, `GlassfishDB`.`Message`.`MessageID` desc fetch next 51 rows only
) t;
SELECT COUNT(*)                         AS zeilen,
       SUM(t.MessageID IS NOT NULL)     AS mit_id,
       SUM(t.ProcessID IS NOT NULL)     AS mit_prozess,
       SUM(t.ProcessName IS NOT NULL)   AS mit_prozessname,
       SUM(t.ProjectName IS NOT NULL)   AS mit_projektname,
       SUM(t.SOSName IS NOT NULL)       AS mit_ablaufname,
       SUM(t.SOSActionName IS NOT NULL) AS mit_schritt,
       COUNT(DISTINCT t.MessageStatus)  AS versch_status,
       MIN(t.MessageLastUpdate)         AS aelteste,
       MAX(t.MessageLastUpdate)         AS juengste
FROM (
select `GlassfishDB`.`Message`.`MessageID`, `GlassfishDB`.`Message`.`MessageLastUpdate`, `GlassfishDB`.`Message`.`MessageStatus`, `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Process`.`ProcessName`, `GlassfishDB`.`Project`.`ProjectName`, `GlassfishDB`.`SOS`.`SOSName`, `GlassfishDB`.`SOSAction`.`SOSActionName` from `GlassfishDB`.`Message` left outer join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` left outer join `GlassfishDB`.`Project` on `GlassfishDB`.`Project`.`ProjectID` = `GlassfishDB`.`Process`.`ProjectID` left outer join `GlassfishDB`.`SOS` on `GlassfishDB`.`SOS`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` left outer join `GlassfishDB`.`SOSAction` on (`GlassfishDB`.`SOSAction`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` and `GlassfishDB`.`SOSAction`.`SOSActionID` = `GlassfishDB`.`Message`.`SOSActionID`) where (`GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-11-30 04:09:47.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` <= timestamp '2025-12-30 04:09:47.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS')) and (`GlassfishDB`.`Message`.`MessageLastUpdate` < @c_ts or (`GlassfishDB`.`Message`.`MessageLastUpdate` = @c_ts and `GlassfishDB`.`Message`.`MessageID` < @c_id))) order by `GlassfishDB`.`Message`.`MessageLastUpdate` desc, `GlassfishDB`.`Message`.`MessageID` desc fetch next 51 rows only
) t;
SELECT COUNT(*)                         AS zeilen,
       SUM(t.MessageID IS NOT NULL)     AS mit_id,
       SUM(t.ProcessID IS NOT NULL)     AS mit_prozess,
       SUM(t.ProcessName IS NOT NULL)   AS mit_prozessname,
       SUM(t.ProjectName IS NOT NULL)   AS mit_projektname,
       SUM(t.SOSName IS NOT NULL)       AS mit_ablaufname,
       SUM(t.SOSActionName IS NOT NULL) AS mit_schritt,
       COUNT(DISTINCT t.MessageStatus)  AS versch_status,
       MIN(t.MessageLastUpdate)         AS aelteste,
       MAX(t.MessageLastUpdate)         AS juengste
FROM (
select `GlassfishDB`.`Message`.`MessageID`, `GlassfishDB`.`Message`.`MessageLastUpdate`, `GlassfishDB`.`Message`.`MessageStatus`, `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Process`.`ProcessName`, `GlassfishDB`.`Project`.`ProjectName`, `GlassfishDB`.`SOS`.`SOSName`, `GlassfishDB`.`SOSAction`.`SOSActionName` from `GlassfishDB`.`Message` left outer join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` left outer join `GlassfishDB`.`Project` on `GlassfishDB`.`Project`.`ProjectID` = `GlassfishDB`.`Process`.`ProjectID` left outer join `GlassfishDB`.`SOS` on `GlassfishDB`.`SOS`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` left outer join `GlassfishDB`.`SOSAction` on (`GlassfishDB`.`SOSAction`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` and `GlassfishDB`.`SOSAction`.`SOSActionID` = `GlassfishDB`.`Message`.`SOSActionID`) where (`GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-11-30 04:09:47.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` <= timestamp '2025-12-30 04:09:47.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS')) and (`GlassfishDB`.`Message`.`MessageLastUpdate` < @c_ts or (`GlassfishDB`.`Message`.`MessageLastUpdate` = @c_ts and `GlassfishDB`.`Message`.`MessageID` < @c_id))) order by `GlassfishDB`.`Message`.`MessageLastUpdate` desc, `GlassfishDB`.`Message`.`MessageID` desc fetch next 51 rows only
) t;
SELECT COUNT(*)                         AS zeilen,
       SUM(t.MessageID IS NOT NULL)     AS mit_id,
       SUM(t.ProcessID IS NOT NULL)     AS mit_prozess,
       SUM(t.ProcessName IS NOT NULL)   AS mit_prozessname,
       SUM(t.ProjectName IS NOT NULL)   AS mit_projektname,
       SUM(t.SOSName IS NOT NULL)       AS mit_ablaufname,
       SUM(t.SOSActionName IS NOT NULL) AS mit_schritt,
       COUNT(DISTINCT t.MessageStatus)  AS versch_status,
       MIN(t.MessageLastUpdate)         AS aelteste,
       MAX(t.MessageLastUpdate)         AS juengste
FROM (
select `GlassfishDB`.`Message`.`MessageID`, `GlassfishDB`.`Message`.`MessageLastUpdate`, `GlassfishDB`.`Message`.`MessageStatus`, `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Process`.`ProcessName`, `GlassfishDB`.`Project`.`ProjectName`, `GlassfishDB`.`SOS`.`SOSName`, `GlassfishDB`.`SOSAction`.`SOSActionName` from `GlassfishDB`.`Message` left outer join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` left outer join `GlassfishDB`.`Project` on `GlassfishDB`.`Project`.`ProjectID` = `GlassfishDB`.`Process`.`ProjectID` left outer join `GlassfishDB`.`SOS` on `GlassfishDB`.`SOS`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` left outer join `GlassfishDB`.`SOSAction` on (`GlassfishDB`.`SOSAction`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` and `GlassfishDB`.`SOSAction`.`SOSActionID` = `GlassfishDB`.`Message`.`SOSActionID`) where (`GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-11-30 04:09:47.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` <= timestamp '2025-12-30 04:09:47.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS')) and (`GlassfishDB`.`Message`.`MessageLastUpdate` < @c_ts or (`GlassfishDB`.`Message`.`MessageLastUpdate` = @c_ts and `GlassfishDB`.`Message`.`MessageID` < @c_id))) order by `GlassfishDB`.`Message`.`MessageLastUpdate` desc, `GlassfishDB`.`Message`.`MessageID` desc fetch next 51 rows only
) t;
SELECT 'GEBAUT-NEXANS-referenz-mitCursor' AS fall,
       COUNT(*)                                       AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)              AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)              AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;


-- ==========================================================================
-- GEBAUT-NEXANS-ueberfaellig-ohneCursor
-- ==========================================================================
SELECT '=== GEBAUT-NEXANS-ueberfaellig-ohneCursor · Plan des gerenderten Statements (L15) ===' AS marke;
EXPLAIN select `GlassfishDB`.`Message`.`MessageID`, `GlassfishDB`.`Message`.`MessageLastUpdate`, `GlassfishDB`.`Message`.`MessageStatus`, `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Process`.`ProcessName`, `GlassfishDB`.`Project`.`ProjectName`, `GlassfishDB`.`SOS`.`SOSName`, `GlassfishDB`.`SOSAction`.`SOSActionName` from `GlassfishDB`.`Message` left outer join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` left outer join `GlassfishDB`.`Project` on `GlassfishDB`.`Project`.`ProjectID` = `GlassfishDB`.`Process`.`ProjectID` left outer join `GlassfishDB`.`SOS` on `GlassfishDB`.`SOS`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` left outer join `GlassfishDB`.`SOSAction` on (`GlassfishDB`.`SOSAction`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` and `GlassfishDB`.`SOSAction`.`SOSActionID` = `GlassfishDB`.`Message`.`SOSActionID`) where (`GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-11-30 04:09:47.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` <= timestamp '2025-12-30 04:09:47.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS')) and `GlassfishDB`.`Message`.`MessageStatus` in ('RUNNING', 'SUSPENDED') and `GlassfishDB`.`Message`.`MessageTimeout` is not null and `GlassfishDB`.`Message`.`MessageTimeout` > 0 and date_add(`GlassfishDB`.`Message`.`MessageLastUpdate`, interval `GlassfishDB`.`Message`.`MessageTimeout` second) < timestamp '2025-12-30 04:09:47.0') order by `GlassfishDB`.`Message`.`MessageLastUpdate` desc, `GlassfishDB`.`Message`.`MessageID` desc fetch next 51 rows only;
SELECT '=== GEBAUT-NEXANS-ueberfaellig-ohneCursor · Ergebnis + Aufwaermlauf + fuenf ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT COUNT(*)                         AS zeilen,
       SUM(t.MessageID IS NOT NULL)     AS mit_id,
       SUM(t.ProcessID IS NOT NULL)     AS mit_prozess,
       SUM(t.ProcessName IS NOT NULL)   AS mit_prozessname,
       SUM(t.ProjectName IS NOT NULL)   AS mit_projektname,
       SUM(t.SOSName IS NOT NULL)       AS mit_ablaufname,
       SUM(t.SOSActionName IS NOT NULL) AS mit_schritt,
       COUNT(DISTINCT t.MessageStatus)  AS versch_status,
       MIN(t.MessageLastUpdate)         AS aelteste,
       MAX(t.MessageLastUpdate)         AS juengste
FROM (
select `GlassfishDB`.`Message`.`MessageID`, `GlassfishDB`.`Message`.`MessageLastUpdate`, `GlassfishDB`.`Message`.`MessageStatus`, `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Process`.`ProcessName`, `GlassfishDB`.`Project`.`ProjectName`, `GlassfishDB`.`SOS`.`SOSName`, `GlassfishDB`.`SOSAction`.`SOSActionName` from `GlassfishDB`.`Message` left outer join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` left outer join `GlassfishDB`.`Project` on `GlassfishDB`.`Project`.`ProjectID` = `GlassfishDB`.`Process`.`ProjectID` left outer join `GlassfishDB`.`SOS` on `GlassfishDB`.`SOS`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` left outer join `GlassfishDB`.`SOSAction` on (`GlassfishDB`.`SOSAction`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` and `GlassfishDB`.`SOSAction`.`SOSActionID` = `GlassfishDB`.`Message`.`SOSActionID`) where (`GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-11-30 04:09:47.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` <= timestamp '2025-12-30 04:09:47.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS')) and `GlassfishDB`.`Message`.`MessageStatus` in ('RUNNING', 'SUSPENDED') and `GlassfishDB`.`Message`.`MessageTimeout` is not null and `GlassfishDB`.`Message`.`MessageTimeout` > 0 and date_add(`GlassfishDB`.`Message`.`MessageLastUpdate`, interval `GlassfishDB`.`Message`.`MessageTimeout` second) < timestamp '2025-12-30 04:09:47.0') order by `GlassfishDB`.`Message`.`MessageLastUpdate` desc, `GlassfishDB`.`Message`.`MessageID` desc fetch next 51 rows only
) t;
SELECT COUNT(*)                         AS zeilen,
       SUM(t.MessageID IS NOT NULL)     AS mit_id,
       SUM(t.ProcessID IS NOT NULL)     AS mit_prozess,
       SUM(t.ProcessName IS NOT NULL)   AS mit_prozessname,
       SUM(t.ProjectName IS NOT NULL)   AS mit_projektname,
       SUM(t.SOSName IS NOT NULL)       AS mit_ablaufname,
       SUM(t.SOSActionName IS NOT NULL) AS mit_schritt,
       COUNT(DISTINCT t.MessageStatus)  AS versch_status,
       MIN(t.MessageLastUpdate)         AS aelteste,
       MAX(t.MessageLastUpdate)         AS juengste
FROM (
select `GlassfishDB`.`Message`.`MessageID`, `GlassfishDB`.`Message`.`MessageLastUpdate`, `GlassfishDB`.`Message`.`MessageStatus`, `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Process`.`ProcessName`, `GlassfishDB`.`Project`.`ProjectName`, `GlassfishDB`.`SOS`.`SOSName`, `GlassfishDB`.`SOSAction`.`SOSActionName` from `GlassfishDB`.`Message` left outer join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` left outer join `GlassfishDB`.`Project` on `GlassfishDB`.`Project`.`ProjectID` = `GlassfishDB`.`Process`.`ProjectID` left outer join `GlassfishDB`.`SOS` on `GlassfishDB`.`SOS`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` left outer join `GlassfishDB`.`SOSAction` on (`GlassfishDB`.`SOSAction`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` and `GlassfishDB`.`SOSAction`.`SOSActionID` = `GlassfishDB`.`Message`.`SOSActionID`) where (`GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-11-30 04:09:47.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` <= timestamp '2025-12-30 04:09:47.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS')) and `GlassfishDB`.`Message`.`MessageStatus` in ('RUNNING', 'SUSPENDED') and `GlassfishDB`.`Message`.`MessageTimeout` is not null and `GlassfishDB`.`Message`.`MessageTimeout` > 0 and date_add(`GlassfishDB`.`Message`.`MessageLastUpdate`, interval `GlassfishDB`.`Message`.`MessageTimeout` second) < timestamp '2025-12-30 04:09:47.0') order by `GlassfishDB`.`Message`.`MessageLastUpdate` desc, `GlassfishDB`.`Message`.`MessageID` desc fetch next 51 rows only
) t;
SELECT COUNT(*)                         AS zeilen,
       SUM(t.MessageID IS NOT NULL)     AS mit_id,
       SUM(t.ProcessID IS NOT NULL)     AS mit_prozess,
       SUM(t.ProcessName IS NOT NULL)   AS mit_prozessname,
       SUM(t.ProjectName IS NOT NULL)   AS mit_projektname,
       SUM(t.SOSName IS NOT NULL)       AS mit_ablaufname,
       SUM(t.SOSActionName IS NOT NULL) AS mit_schritt,
       COUNT(DISTINCT t.MessageStatus)  AS versch_status,
       MIN(t.MessageLastUpdate)         AS aelteste,
       MAX(t.MessageLastUpdate)         AS juengste
FROM (
select `GlassfishDB`.`Message`.`MessageID`, `GlassfishDB`.`Message`.`MessageLastUpdate`, `GlassfishDB`.`Message`.`MessageStatus`, `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Process`.`ProcessName`, `GlassfishDB`.`Project`.`ProjectName`, `GlassfishDB`.`SOS`.`SOSName`, `GlassfishDB`.`SOSAction`.`SOSActionName` from `GlassfishDB`.`Message` left outer join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` left outer join `GlassfishDB`.`Project` on `GlassfishDB`.`Project`.`ProjectID` = `GlassfishDB`.`Process`.`ProjectID` left outer join `GlassfishDB`.`SOS` on `GlassfishDB`.`SOS`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` left outer join `GlassfishDB`.`SOSAction` on (`GlassfishDB`.`SOSAction`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` and `GlassfishDB`.`SOSAction`.`SOSActionID` = `GlassfishDB`.`Message`.`SOSActionID`) where (`GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-11-30 04:09:47.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` <= timestamp '2025-12-30 04:09:47.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS')) and `GlassfishDB`.`Message`.`MessageStatus` in ('RUNNING', 'SUSPENDED') and `GlassfishDB`.`Message`.`MessageTimeout` is not null and `GlassfishDB`.`Message`.`MessageTimeout` > 0 and date_add(`GlassfishDB`.`Message`.`MessageLastUpdate`, interval `GlassfishDB`.`Message`.`MessageTimeout` second) < timestamp '2025-12-30 04:09:47.0') order by `GlassfishDB`.`Message`.`MessageLastUpdate` desc, `GlassfishDB`.`Message`.`MessageID` desc fetch next 51 rows only
) t;
SELECT COUNT(*)                         AS zeilen,
       SUM(t.MessageID IS NOT NULL)     AS mit_id,
       SUM(t.ProcessID IS NOT NULL)     AS mit_prozess,
       SUM(t.ProcessName IS NOT NULL)   AS mit_prozessname,
       SUM(t.ProjectName IS NOT NULL)   AS mit_projektname,
       SUM(t.SOSName IS NOT NULL)       AS mit_ablaufname,
       SUM(t.SOSActionName IS NOT NULL) AS mit_schritt,
       COUNT(DISTINCT t.MessageStatus)  AS versch_status,
       MIN(t.MessageLastUpdate)         AS aelteste,
       MAX(t.MessageLastUpdate)         AS juengste
FROM (
select `GlassfishDB`.`Message`.`MessageID`, `GlassfishDB`.`Message`.`MessageLastUpdate`, `GlassfishDB`.`Message`.`MessageStatus`, `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Process`.`ProcessName`, `GlassfishDB`.`Project`.`ProjectName`, `GlassfishDB`.`SOS`.`SOSName`, `GlassfishDB`.`SOSAction`.`SOSActionName` from `GlassfishDB`.`Message` left outer join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` left outer join `GlassfishDB`.`Project` on `GlassfishDB`.`Project`.`ProjectID` = `GlassfishDB`.`Process`.`ProjectID` left outer join `GlassfishDB`.`SOS` on `GlassfishDB`.`SOS`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` left outer join `GlassfishDB`.`SOSAction` on (`GlassfishDB`.`SOSAction`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` and `GlassfishDB`.`SOSAction`.`SOSActionID` = `GlassfishDB`.`Message`.`SOSActionID`) where (`GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-11-30 04:09:47.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` <= timestamp '2025-12-30 04:09:47.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS')) and `GlassfishDB`.`Message`.`MessageStatus` in ('RUNNING', 'SUSPENDED') and `GlassfishDB`.`Message`.`MessageTimeout` is not null and `GlassfishDB`.`Message`.`MessageTimeout` > 0 and date_add(`GlassfishDB`.`Message`.`MessageLastUpdate`, interval `GlassfishDB`.`Message`.`MessageTimeout` second) < timestamp '2025-12-30 04:09:47.0') order by `GlassfishDB`.`Message`.`MessageLastUpdate` desc, `GlassfishDB`.`Message`.`MessageID` desc fetch next 51 rows only
) t;
SELECT COUNT(*)                         AS zeilen,
       SUM(t.MessageID IS NOT NULL)     AS mit_id,
       SUM(t.ProcessID IS NOT NULL)     AS mit_prozess,
       SUM(t.ProcessName IS NOT NULL)   AS mit_prozessname,
       SUM(t.ProjectName IS NOT NULL)   AS mit_projektname,
       SUM(t.SOSName IS NOT NULL)       AS mit_ablaufname,
       SUM(t.SOSActionName IS NOT NULL) AS mit_schritt,
       COUNT(DISTINCT t.MessageStatus)  AS versch_status,
       MIN(t.MessageLastUpdate)         AS aelteste,
       MAX(t.MessageLastUpdate)         AS juengste
FROM (
select `GlassfishDB`.`Message`.`MessageID`, `GlassfishDB`.`Message`.`MessageLastUpdate`, `GlassfishDB`.`Message`.`MessageStatus`, `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Process`.`ProcessName`, `GlassfishDB`.`Project`.`ProjectName`, `GlassfishDB`.`SOS`.`SOSName`, `GlassfishDB`.`SOSAction`.`SOSActionName` from `GlassfishDB`.`Message` left outer join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` left outer join `GlassfishDB`.`Project` on `GlassfishDB`.`Project`.`ProjectID` = `GlassfishDB`.`Process`.`ProjectID` left outer join `GlassfishDB`.`SOS` on `GlassfishDB`.`SOS`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` left outer join `GlassfishDB`.`SOSAction` on (`GlassfishDB`.`SOSAction`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` and `GlassfishDB`.`SOSAction`.`SOSActionID` = `GlassfishDB`.`Message`.`SOSActionID`) where (`GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-11-30 04:09:47.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` <= timestamp '2025-12-30 04:09:47.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS')) and `GlassfishDB`.`Message`.`MessageStatus` in ('RUNNING', 'SUSPENDED') and `GlassfishDB`.`Message`.`MessageTimeout` is not null and `GlassfishDB`.`Message`.`MessageTimeout` > 0 and date_add(`GlassfishDB`.`Message`.`MessageLastUpdate`, interval `GlassfishDB`.`Message`.`MessageTimeout` second) < timestamp '2025-12-30 04:09:47.0') order by `GlassfishDB`.`Message`.`MessageLastUpdate` desc, `GlassfishDB`.`Message`.`MessageID` desc fetch next 51 rows only
) t;
SELECT COUNT(*)                         AS zeilen,
       SUM(t.MessageID IS NOT NULL)     AS mit_id,
       SUM(t.ProcessID IS NOT NULL)     AS mit_prozess,
       SUM(t.ProcessName IS NOT NULL)   AS mit_prozessname,
       SUM(t.ProjectName IS NOT NULL)   AS mit_projektname,
       SUM(t.SOSName IS NOT NULL)       AS mit_ablaufname,
       SUM(t.SOSActionName IS NOT NULL) AS mit_schritt,
       COUNT(DISTINCT t.MessageStatus)  AS versch_status,
       MIN(t.MessageLastUpdate)         AS aelteste,
       MAX(t.MessageLastUpdate)         AS juengste
FROM (
select `GlassfishDB`.`Message`.`MessageID`, `GlassfishDB`.`Message`.`MessageLastUpdate`, `GlassfishDB`.`Message`.`MessageStatus`, `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Process`.`ProcessName`, `GlassfishDB`.`Project`.`ProjectName`, `GlassfishDB`.`SOS`.`SOSName`, `GlassfishDB`.`SOSAction`.`SOSActionName` from `GlassfishDB`.`Message` left outer join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` left outer join `GlassfishDB`.`Project` on `GlassfishDB`.`Project`.`ProjectID` = `GlassfishDB`.`Process`.`ProjectID` left outer join `GlassfishDB`.`SOS` on `GlassfishDB`.`SOS`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` left outer join `GlassfishDB`.`SOSAction` on (`GlassfishDB`.`SOSAction`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` and `GlassfishDB`.`SOSAction`.`SOSActionID` = `GlassfishDB`.`Message`.`SOSActionID`) where (`GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-11-30 04:09:47.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` <= timestamp '2025-12-30 04:09:47.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS')) and `GlassfishDB`.`Message`.`MessageStatus` in ('RUNNING', 'SUSPENDED') and `GlassfishDB`.`Message`.`MessageTimeout` is not null and `GlassfishDB`.`Message`.`MessageTimeout` > 0 and date_add(`GlassfishDB`.`Message`.`MessageLastUpdate`, interval `GlassfishDB`.`Message`.`MessageTimeout` second) < timestamp '2025-12-30 04:09:47.0') order by `GlassfishDB`.`Message`.`MessageLastUpdate` desc, `GlassfishDB`.`Message`.`MessageID` desc fetch next 51 rows only
) t;
SELECT 'GEBAUT-NEXANS-ueberfaellig-ohneCursor' AS fall,
       COUNT(*)                                       AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)              AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)              AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;


-- ==========================================================================
-- GEBAUT-NEXANS-ueberfaellig-mitCursor
-- ==========================================================================
SELECT '=== GEBAUT-NEXANS-ueberfaellig-mitCursor · Plan des gerenderten Statements (L15) ===' AS marke;
EXPLAIN select `GlassfishDB`.`Message`.`MessageID`, `GlassfishDB`.`Message`.`MessageLastUpdate`, `GlassfishDB`.`Message`.`MessageStatus`, `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Process`.`ProcessName`, `GlassfishDB`.`Project`.`ProjectName`, `GlassfishDB`.`SOS`.`SOSName`, `GlassfishDB`.`SOSAction`.`SOSActionName` from `GlassfishDB`.`Message` left outer join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` left outer join `GlassfishDB`.`Project` on `GlassfishDB`.`Project`.`ProjectID` = `GlassfishDB`.`Process`.`ProjectID` left outer join `GlassfishDB`.`SOS` on `GlassfishDB`.`SOS`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` left outer join `GlassfishDB`.`SOSAction` on (`GlassfishDB`.`SOSAction`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` and `GlassfishDB`.`SOSAction`.`SOSActionID` = `GlassfishDB`.`Message`.`SOSActionID`) where (`GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-11-30 04:09:47.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` <= timestamp '2025-12-30 04:09:47.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS')) and `GlassfishDB`.`Message`.`MessageStatus` in ('RUNNING', 'SUSPENDED') and `GlassfishDB`.`Message`.`MessageTimeout` is not null and `GlassfishDB`.`Message`.`MessageTimeout` > 0 and date_add(`GlassfishDB`.`Message`.`MessageLastUpdate`, interval `GlassfishDB`.`Message`.`MessageTimeout` second) < timestamp '2025-12-30 04:09:47.0' and (`GlassfishDB`.`Message`.`MessageLastUpdate` < @u_ts or (`GlassfishDB`.`Message`.`MessageLastUpdate` = @u_ts and `GlassfishDB`.`Message`.`MessageID` < @u_id))) order by `GlassfishDB`.`Message`.`MessageLastUpdate` desc, `GlassfishDB`.`Message`.`MessageID` desc fetch next 51 rows only;
SELECT '=== GEBAUT-NEXANS-ueberfaellig-mitCursor · Ergebnis + Aufwaermlauf + fuenf ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT COUNT(*)                         AS zeilen,
       SUM(t.MessageID IS NOT NULL)     AS mit_id,
       SUM(t.ProcessID IS NOT NULL)     AS mit_prozess,
       SUM(t.ProcessName IS NOT NULL)   AS mit_prozessname,
       SUM(t.ProjectName IS NOT NULL)   AS mit_projektname,
       SUM(t.SOSName IS NOT NULL)       AS mit_ablaufname,
       SUM(t.SOSActionName IS NOT NULL) AS mit_schritt,
       COUNT(DISTINCT t.MessageStatus)  AS versch_status,
       MIN(t.MessageLastUpdate)         AS aelteste,
       MAX(t.MessageLastUpdate)         AS juengste
FROM (
select `GlassfishDB`.`Message`.`MessageID`, `GlassfishDB`.`Message`.`MessageLastUpdate`, `GlassfishDB`.`Message`.`MessageStatus`, `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Process`.`ProcessName`, `GlassfishDB`.`Project`.`ProjectName`, `GlassfishDB`.`SOS`.`SOSName`, `GlassfishDB`.`SOSAction`.`SOSActionName` from `GlassfishDB`.`Message` left outer join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` left outer join `GlassfishDB`.`Project` on `GlassfishDB`.`Project`.`ProjectID` = `GlassfishDB`.`Process`.`ProjectID` left outer join `GlassfishDB`.`SOS` on `GlassfishDB`.`SOS`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` left outer join `GlassfishDB`.`SOSAction` on (`GlassfishDB`.`SOSAction`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` and `GlassfishDB`.`SOSAction`.`SOSActionID` = `GlassfishDB`.`Message`.`SOSActionID`) where (`GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-11-30 04:09:47.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` <= timestamp '2025-12-30 04:09:47.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS')) and `GlassfishDB`.`Message`.`MessageStatus` in ('RUNNING', 'SUSPENDED') and `GlassfishDB`.`Message`.`MessageTimeout` is not null and `GlassfishDB`.`Message`.`MessageTimeout` > 0 and date_add(`GlassfishDB`.`Message`.`MessageLastUpdate`, interval `GlassfishDB`.`Message`.`MessageTimeout` second) < timestamp '2025-12-30 04:09:47.0' and (`GlassfishDB`.`Message`.`MessageLastUpdate` < @u_ts or (`GlassfishDB`.`Message`.`MessageLastUpdate` = @u_ts and `GlassfishDB`.`Message`.`MessageID` < @u_id))) order by `GlassfishDB`.`Message`.`MessageLastUpdate` desc, `GlassfishDB`.`Message`.`MessageID` desc fetch next 51 rows only
) t;
SELECT COUNT(*)                         AS zeilen,
       SUM(t.MessageID IS NOT NULL)     AS mit_id,
       SUM(t.ProcessID IS NOT NULL)     AS mit_prozess,
       SUM(t.ProcessName IS NOT NULL)   AS mit_prozessname,
       SUM(t.ProjectName IS NOT NULL)   AS mit_projektname,
       SUM(t.SOSName IS NOT NULL)       AS mit_ablaufname,
       SUM(t.SOSActionName IS NOT NULL) AS mit_schritt,
       COUNT(DISTINCT t.MessageStatus)  AS versch_status,
       MIN(t.MessageLastUpdate)         AS aelteste,
       MAX(t.MessageLastUpdate)         AS juengste
FROM (
select `GlassfishDB`.`Message`.`MessageID`, `GlassfishDB`.`Message`.`MessageLastUpdate`, `GlassfishDB`.`Message`.`MessageStatus`, `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Process`.`ProcessName`, `GlassfishDB`.`Project`.`ProjectName`, `GlassfishDB`.`SOS`.`SOSName`, `GlassfishDB`.`SOSAction`.`SOSActionName` from `GlassfishDB`.`Message` left outer join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` left outer join `GlassfishDB`.`Project` on `GlassfishDB`.`Project`.`ProjectID` = `GlassfishDB`.`Process`.`ProjectID` left outer join `GlassfishDB`.`SOS` on `GlassfishDB`.`SOS`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` left outer join `GlassfishDB`.`SOSAction` on (`GlassfishDB`.`SOSAction`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` and `GlassfishDB`.`SOSAction`.`SOSActionID` = `GlassfishDB`.`Message`.`SOSActionID`) where (`GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-11-30 04:09:47.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` <= timestamp '2025-12-30 04:09:47.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS')) and `GlassfishDB`.`Message`.`MessageStatus` in ('RUNNING', 'SUSPENDED') and `GlassfishDB`.`Message`.`MessageTimeout` is not null and `GlassfishDB`.`Message`.`MessageTimeout` > 0 and date_add(`GlassfishDB`.`Message`.`MessageLastUpdate`, interval `GlassfishDB`.`Message`.`MessageTimeout` second) < timestamp '2025-12-30 04:09:47.0' and (`GlassfishDB`.`Message`.`MessageLastUpdate` < @u_ts or (`GlassfishDB`.`Message`.`MessageLastUpdate` = @u_ts and `GlassfishDB`.`Message`.`MessageID` < @u_id))) order by `GlassfishDB`.`Message`.`MessageLastUpdate` desc, `GlassfishDB`.`Message`.`MessageID` desc fetch next 51 rows only
) t;
SELECT COUNT(*)                         AS zeilen,
       SUM(t.MessageID IS NOT NULL)     AS mit_id,
       SUM(t.ProcessID IS NOT NULL)     AS mit_prozess,
       SUM(t.ProcessName IS NOT NULL)   AS mit_prozessname,
       SUM(t.ProjectName IS NOT NULL)   AS mit_projektname,
       SUM(t.SOSName IS NOT NULL)       AS mit_ablaufname,
       SUM(t.SOSActionName IS NOT NULL) AS mit_schritt,
       COUNT(DISTINCT t.MessageStatus)  AS versch_status,
       MIN(t.MessageLastUpdate)         AS aelteste,
       MAX(t.MessageLastUpdate)         AS juengste
FROM (
select `GlassfishDB`.`Message`.`MessageID`, `GlassfishDB`.`Message`.`MessageLastUpdate`, `GlassfishDB`.`Message`.`MessageStatus`, `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Process`.`ProcessName`, `GlassfishDB`.`Project`.`ProjectName`, `GlassfishDB`.`SOS`.`SOSName`, `GlassfishDB`.`SOSAction`.`SOSActionName` from `GlassfishDB`.`Message` left outer join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` left outer join `GlassfishDB`.`Project` on `GlassfishDB`.`Project`.`ProjectID` = `GlassfishDB`.`Process`.`ProjectID` left outer join `GlassfishDB`.`SOS` on `GlassfishDB`.`SOS`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` left outer join `GlassfishDB`.`SOSAction` on (`GlassfishDB`.`SOSAction`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` and `GlassfishDB`.`SOSAction`.`SOSActionID` = `GlassfishDB`.`Message`.`SOSActionID`) where (`GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-11-30 04:09:47.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` <= timestamp '2025-12-30 04:09:47.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS')) and `GlassfishDB`.`Message`.`MessageStatus` in ('RUNNING', 'SUSPENDED') and `GlassfishDB`.`Message`.`MessageTimeout` is not null and `GlassfishDB`.`Message`.`MessageTimeout` > 0 and date_add(`GlassfishDB`.`Message`.`MessageLastUpdate`, interval `GlassfishDB`.`Message`.`MessageTimeout` second) < timestamp '2025-12-30 04:09:47.0' and (`GlassfishDB`.`Message`.`MessageLastUpdate` < @u_ts or (`GlassfishDB`.`Message`.`MessageLastUpdate` = @u_ts and `GlassfishDB`.`Message`.`MessageID` < @u_id))) order by `GlassfishDB`.`Message`.`MessageLastUpdate` desc, `GlassfishDB`.`Message`.`MessageID` desc fetch next 51 rows only
) t;
SELECT COUNT(*)                         AS zeilen,
       SUM(t.MessageID IS NOT NULL)     AS mit_id,
       SUM(t.ProcessID IS NOT NULL)     AS mit_prozess,
       SUM(t.ProcessName IS NOT NULL)   AS mit_prozessname,
       SUM(t.ProjectName IS NOT NULL)   AS mit_projektname,
       SUM(t.SOSName IS NOT NULL)       AS mit_ablaufname,
       SUM(t.SOSActionName IS NOT NULL) AS mit_schritt,
       COUNT(DISTINCT t.MessageStatus)  AS versch_status,
       MIN(t.MessageLastUpdate)         AS aelteste,
       MAX(t.MessageLastUpdate)         AS juengste
FROM (
select `GlassfishDB`.`Message`.`MessageID`, `GlassfishDB`.`Message`.`MessageLastUpdate`, `GlassfishDB`.`Message`.`MessageStatus`, `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Process`.`ProcessName`, `GlassfishDB`.`Project`.`ProjectName`, `GlassfishDB`.`SOS`.`SOSName`, `GlassfishDB`.`SOSAction`.`SOSActionName` from `GlassfishDB`.`Message` left outer join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` left outer join `GlassfishDB`.`Project` on `GlassfishDB`.`Project`.`ProjectID` = `GlassfishDB`.`Process`.`ProjectID` left outer join `GlassfishDB`.`SOS` on `GlassfishDB`.`SOS`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` left outer join `GlassfishDB`.`SOSAction` on (`GlassfishDB`.`SOSAction`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` and `GlassfishDB`.`SOSAction`.`SOSActionID` = `GlassfishDB`.`Message`.`SOSActionID`) where (`GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-11-30 04:09:47.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` <= timestamp '2025-12-30 04:09:47.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS')) and `GlassfishDB`.`Message`.`MessageStatus` in ('RUNNING', 'SUSPENDED') and `GlassfishDB`.`Message`.`MessageTimeout` is not null and `GlassfishDB`.`Message`.`MessageTimeout` > 0 and date_add(`GlassfishDB`.`Message`.`MessageLastUpdate`, interval `GlassfishDB`.`Message`.`MessageTimeout` second) < timestamp '2025-12-30 04:09:47.0' and (`GlassfishDB`.`Message`.`MessageLastUpdate` < @u_ts or (`GlassfishDB`.`Message`.`MessageLastUpdate` = @u_ts and `GlassfishDB`.`Message`.`MessageID` < @u_id))) order by `GlassfishDB`.`Message`.`MessageLastUpdate` desc, `GlassfishDB`.`Message`.`MessageID` desc fetch next 51 rows only
) t;
SELECT COUNT(*)                         AS zeilen,
       SUM(t.MessageID IS NOT NULL)     AS mit_id,
       SUM(t.ProcessID IS NOT NULL)     AS mit_prozess,
       SUM(t.ProcessName IS NOT NULL)   AS mit_prozessname,
       SUM(t.ProjectName IS NOT NULL)   AS mit_projektname,
       SUM(t.SOSName IS NOT NULL)       AS mit_ablaufname,
       SUM(t.SOSActionName IS NOT NULL) AS mit_schritt,
       COUNT(DISTINCT t.MessageStatus)  AS versch_status,
       MIN(t.MessageLastUpdate)         AS aelteste,
       MAX(t.MessageLastUpdate)         AS juengste
FROM (
select `GlassfishDB`.`Message`.`MessageID`, `GlassfishDB`.`Message`.`MessageLastUpdate`, `GlassfishDB`.`Message`.`MessageStatus`, `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Process`.`ProcessName`, `GlassfishDB`.`Project`.`ProjectName`, `GlassfishDB`.`SOS`.`SOSName`, `GlassfishDB`.`SOSAction`.`SOSActionName` from `GlassfishDB`.`Message` left outer join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` left outer join `GlassfishDB`.`Project` on `GlassfishDB`.`Project`.`ProjectID` = `GlassfishDB`.`Process`.`ProjectID` left outer join `GlassfishDB`.`SOS` on `GlassfishDB`.`SOS`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` left outer join `GlassfishDB`.`SOSAction` on (`GlassfishDB`.`SOSAction`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` and `GlassfishDB`.`SOSAction`.`SOSActionID` = `GlassfishDB`.`Message`.`SOSActionID`) where (`GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-11-30 04:09:47.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` <= timestamp '2025-12-30 04:09:47.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS')) and `GlassfishDB`.`Message`.`MessageStatus` in ('RUNNING', 'SUSPENDED') and `GlassfishDB`.`Message`.`MessageTimeout` is not null and `GlassfishDB`.`Message`.`MessageTimeout` > 0 and date_add(`GlassfishDB`.`Message`.`MessageLastUpdate`, interval `GlassfishDB`.`Message`.`MessageTimeout` second) < timestamp '2025-12-30 04:09:47.0' and (`GlassfishDB`.`Message`.`MessageLastUpdate` < @u_ts or (`GlassfishDB`.`Message`.`MessageLastUpdate` = @u_ts and `GlassfishDB`.`Message`.`MessageID` < @u_id))) order by `GlassfishDB`.`Message`.`MessageLastUpdate` desc, `GlassfishDB`.`Message`.`MessageID` desc fetch next 51 rows only
) t;
SELECT COUNT(*)                         AS zeilen,
       SUM(t.MessageID IS NOT NULL)     AS mit_id,
       SUM(t.ProcessID IS NOT NULL)     AS mit_prozess,
       SUM(t.ProcessName IS NOT NULL)   AS mit_prozessname,
       SUM(t.ProjectName IS NOT NULL)   AS mit_projektname,
       SUM(t.SOSName IS NOT NULL)       AS mit_ablaufname,
       SUM(t.SOSActionName IS NOT NULL) AS mit_schritt,
       COUNT(DISTINCT t.MessageStatus)  AS versch_status,
       MIN(t.MessageLastUpdate)         AS aelteste,
       MAX(t.MessageLastUpdate)         AS juengste
FROM (
select `GlassfishDB`.`Message`.`MessageID`, `GlassfishDB`.`Message`.`MessageLastUpdate`, `GlassfishDB`.`Message`.`MessageStatus`, `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Process`.`ProcessName`, `GlassfishDB`.`Project`.`ProjectName`, `GlassfishDB`.`SOS`.`SOSName`, `GlassfishDB`.`SOSAction`.`SOSActionName` from `GlassfishDB`.`Message` left outer join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` left outer join `GlassfishDB`.`Project` on `GlassfishDB`.`Project`.`ProjectID` = `GlassfishDB`.`Process`.`ProjectID` left outer join `GlassfishDB`.`SOS` on `GlassfishDB`.`SOS`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` left outer join `GlassfishDB`.`SOSAction` on (`GlassfishDB`.`SOSAction`.`SOSID` = `GlassfishDB`.`Message`.`SOSID` and `GlassfishDB`.`SOSAction`.`SOSActionID` = `GlassfishDB`.`Message`.`SOSActionID`) where (`GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-11-30 04:09:47.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` <= timestamp '2025-12-30 04:09:47.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS')) and `GlassfishDB`.`Message`.`MessageStatus` in ('RUNNING', 'SUSPENDED') and `GlassfishDB`.`Message`.`MessageTimeout` is not null and `GlassfishDB`.`Message`.`MessageTimeout` > 0 and date_add(`GlassfishDB`.`Message`.`MessageLastUpdate`, interval `GlassfishDB`.`Message`.`MessageTimeout` second) < timestamp '2025-12-30 04:09:47.0' and (`GlassfishDB`.`Message`.`MessageLastUpdate` < @u_ts or (`GlassfishDB`.`Message`.`MessageLastUpdate` = @u_ts and `GlassfishDB`.`Message`.`MessageID` < @u_id))) order by `GlassfishDB`.`Message`.`MessageLastUpdate` desc, `GlassfishDB`.`Message`.`MessageID` desc fetch next 51 rows only
) t;
SELECT 'GEBAUT-NEXANS-ueberfaellig-mitCursor' AS fall,
       COUNT(*)                                       AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)              AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)              AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;


SELECT '=== Abschluss: read_only ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;
