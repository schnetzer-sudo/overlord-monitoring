-- Messung A.1 vor Schritt 10b-1 — Sitzung 7: Fassung 1 — Prozessliste des Mandanten vorab aufgeloest, dann ProcessID IN (…) · NEXANS
-- Auftrag:  "Schritt 10b-1 — Tagesebene, Listen-Fix und der Parameter `ueberfaellig`",
--           Stand 27.08.2026, Teil A.1
-- Ergebnis: docs/nachrichtenliste.md §5a
--
-- Ausschliesslich SELECT / SET / EXPLAIN / PREPARE. (Regel S1)
--
-- G1 — dieselbe Bauform wie M97 (docs/messungen-schritt10b.md), und aus demselben Grund:
--   Die echte Listenabfrage liefert MessageID, ProcessID, ProcessName und ProjectName.
--   Sie darf deshalb NICHT ausgegeben werden.
--     * Der PLAN (L15) wird am NACKTEN, echten Statement erhoben — EXPLAIN fuehrt es
--       nicht aus und liefert keine einzige Datenzeile.
--     * Die LAUFZEIT (L7) wird an derselben Abfrage in einer aggregierenden Huelle
--       gemessen, die JEDE gejointe Spalte anfasst — sonst optimierte MariaDB die
--       LEFT JOIN weg (Befund 9 der Vorrunde).
--     * Beide Plaene stehen nebeneinander; weichen sie ab, ist die Messung die der
--       Huelle und nicht die des Endpunkts.
--
-- Z1: `jetzt` ist der Anker der Anwendungsuhr 2025-12-30 04:09:47 (V5 der Vorrunde) und steht
--     als Literal. Das Fenster ist `zeitraum=30d`, aufgeloest gegen diesen Anker; beide
--     Grenzen einschliesslich, wie NachrichtenRepository sie setzt (.ge/.le).
--     Limit 51 = 50 + 1, wie das Repository liest.
--
-- Cursor und Prozessliste stehen als Sitzungsvariablen und nicht als Literal — G1:
--     Weder MessageID noch ProcessID duerfen in dieser Datei oder in der Ausgabe stehen.
--     Die Form im Statement ist dieselbe, die jOOQ bindet (Parameter, kein Literal).
--     Hergeleitet wird der Cursor deterministisch als 50. Zeile der ersten Seite; das
--     OFFSET dabei ist Herleitung und kein Blaettern (Regel L3 gilt dem Endpunkt).
--
-- Laufzeiten kommen aus `information_schema.PROFILING` und nicht aus `SHOW PROFILES`:
--     Letzteres gaebe den Statementtext aus, und der traegt in Fassung 1 die ganze
--     Prozessliste. Die Zuordnung laeuft ueber @basis; `anzahl_laeufe` muss in jedem
--     Block **6** sein, sonst ist die Zuordnung verrutscht und der Wert unbrauchbar.
--
-- STRAIGHT_JOIN kommt in keiner Fassung vor (M42: Faktor 219 bis 1094).
-- ANALYZE TABLE kommt nicht vor: `monitor_read` hat auf GlassfishDB nur SELECT.

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;

SET max_statement_time = 60;
SET profiling = 1;
SET profiling_history_size = 100;



-- ── Cursor herleiten: 50. Zeile der ersten Seite, je Fall ───────────────────
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
  AND m.MessageStatus IN ('SUSPENDED','RUNNING')
  AND m.MessageTimeout IS NOT NULL
  AND m.MessageTimeout > 0
  AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND < '2025-12-30 04:09:47'
ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
LIMIT 49, 1;

-- Hat die ueberfaellige Liste keine 50 Zeilen, faellt ihr Cursor auf den der
-- ungefilterten zurueck (Abweichung A8 der Vorrunde, hier uebernommen).
SET @u_ts = COALESCE(@u_ts, @c_ts), @u_id = COALESCE(@u_id, @c_id);

SELECT '=== Cursor gesetzt (ohne Kennungen) ===' AS marke;
SELECT @c_ts AS cursor_liste_zeitpunkt,
       @c_id IS NOT NULL AS cursor_liste_vorhanden,
       @u_ts AS cursor_ueberfaellig_zeitpunkt,
       @u_id IS NOT NULL AS cursor_ueberfaellig_vorhanden,
       (@u_id <=> @c_id) AS ueberfaellig_nutzt_denselben_cursor;



-- ── Die Prozessliste des Mandanten, Fassung 1 ───────────────────────────────
-- G1: Die Liste bleibt in einer Sitzungsvariablen; ausgegeben wird nur ihre Groesse.
SET SESSION group_concat_max_len = 8000000;

SELECT '=== F1 · Aufloesung der Prozessliste · Aufwaermlauf + fuenf ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SELECT GROUP_CONCAT(QUOTE(p.ProcessID)) INTO @pids
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'NEXANS';
SELECT GROUP_CONCAT(QUOTE(p.ProcessID)) INTO @pids
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'NEXANS';
SELECT GROUP_CONCAT(QUOTE(p.ProcessID)) INTO @pids
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'NEXANS';
SELECT GROUP_CONCAT(QUOTE(p.ProcessID)) INTO @pids
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'NEXANS';
SELECT GROUP_CONCAT(QUOTE(p.ProcessID)) INTO @pids
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'NEXANS';
SELECT GROUP_CONCAT(QUOTE(p.ProcessID)) INTO @pids
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'NEXANS';
SELECT 'F1-aufloesung-prozessliste' AS fall,
       COUNT(*)                                       AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)              AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)              AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

SELECT '=== F1 · Groesse der Prozessliste (ohne Kennungen) ===' AS marke;
SELECT (LENGTH(@pids) - LENGTH(REPLACE(@pids, ',', ''))) + 1 AS bindeplaetze,
       LENGTH(@pids) AS zeichen;

SELECT '=== F1 · Plan der Aufloesung (L15) ===' AS marke;
EXPLAIN SELECT p.ProcessID
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'NEXANS';


-- ==========================================================================
-- F1-referenz-ohneCursor · NEXANS
-- ==========================================================================
SET @s_nackt = CONCAT('EXPLAIN
  SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID,
         p.ProcessName, pr.ProjectName, s.SOSName, sa.SOSActionName
  FROM GlassfishDB.Message m
  LEFT JOIN GlassfishDB.Process   p  ON p.ProcessID  = m.ProcessID
  LEFT JOIN GlassfishDB.Project   pr ON pr.ProjectID = p.ProjectID
  LEFT JOIN GlassfishDB.SOS       s  ON s.SOSID      = m.SOSID
  LEFT JOIN GlassfishDB.SOSAction sa ON sa.SOSID     = m.SOSID
                                    AND sa.SOSActionID = m.SOSActionID
  WHERE m.MessageLastUpdate >= ''2025-11-30 04:09:47''
    AND m.MessageLastUpdate <= ''2025-12-30 04:09:47''
    AND m.ProcessID IN (', @pids, ')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51');
SET @s_huelle_plan = CONCAT('EXPLAIN SELECT COUNT(*)                         AS zeilen,
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
  SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID,
         p.ProcessName, pr.ProjectName, s.SOSName, sa.SOSActionName
  FROM GlassfishDB.Message m
  LEFT JOIN GlassfishDB.Process   p  ON p.ProcessID  = m.ProcessID
  LEFT JOIN GlassfishDB.Project   pr ON pr.ProjectID = p.ProjectID
  LEFT JOIN GlassfishDB.SOS       s  ON s.SOSID      = m.SOSID
  LEFT JOIN GlassfishDB.SOSAction sa ON sa.SOSID     = m.SOSID
                                    AND sa.SOSActionID = m.SOSActionID
  WHERE m.MessageLastUpdate >= ''2025-11-30 04:09:47''
    AND m.MessageLastUpdate <= ''2025-12-30 04:09:47''
    AND m.ProcessID IN (', @pids, ')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
SET @s_huelle = CONCAT('SELECT COUNT(*)                         AS zeilen,
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
  SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID,
         p.ProcessName, pr.ProjectName, s.SOSName, sa.SOSActionName
  FROM GlassfishDB.Message m
  LEFT JOIN GlassfishDB.Process   p  ON p.ProcessID  = m.ProcessID
  LEFT JOIN GlassfishDB.Project   pr ON pr.ProjectID = p.ProjectID
  LEFT JOIN GlassfishDB.SOS       s  ON s.SOSID      = m.SOSID
  LEFT JOIN GlassfishDB.SOSAction sa ON sa.SOSID     = m.SOSID
                                    AND sa.SOSActionID = m.SOSActionID
  WHERE m.MessageLastUpdate >= ''2025-11-30 04:09:47''
    AND m.MessageLastUpdate <= ''2025-12-30 04:09:47''
    AND m.ProcessID IN (', @pids, ')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
PREPARE p_nackt_F1_referenz_ohneCursor      FROM @s_nackt;
PREPARE p_huelleplan_F1_referenz_ohneCursor FROM @s_huelle_plan;
PREPARE p_huelle_F1_referenz_ohneCursor     FROM @s_huelle;

SELECT '=== F1-referenz-ohneCursor · Plan des NACKTEN Statements (L15) ===' AS marke;
EXECUTE p_nackt_F1_referenz_ohneCursor;
SELECT '=== F1-referenz-ohneCursor · Plan der gemessenen Huelle ===' AS marke;
EXECUTE p_huelleplan_F1_referenz_ohneCursor;

SELECT '=== F1-referenz-ohneCursor · Ergebnis + Aufwaermlauf + fuenf ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_huelle_F1_referenz_ohneCursor;
EXECUTE p_huelle_F1_referenz_ohneCursor;
EXECUTE p_huelle_F1_referenz_ohneCursor;
EXECUTE p_huelle_F1_referenz_ohneCursor;
EXECUTE p_huelle_F1_referenz_ohneCursor;
EXECUTE p_huelle_F1_referenz_ohneCursor;
SELECT 'F1-referenz-ohneCursor' AS fall,
       COUNT(*)                                       AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)              AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)              AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

DEALLOCATE PREPARE p_nackt_F1_referenz_ohneCursor;
DEALLOCATE PREPARE p_huelleplan_F1_referenz_ohneCursor;
DEALLOCATE PREPARE p_huelle_F1_referenz_ohneCursor;

-- ==========================================================================
-- F1-referenz-mitCursor · NEXANS
-- ==========================================================================
SET @s_nackt = CONCAT('EXPLAIN
  SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID,
         p.ProcessName, pr.ProjectName, s.SOSName, sa.SOSActionName
  FROM GlassfishDB.Message m
  LEFT JOIN GlassfishDB.Process   p  ON p.ProcessID  = m.ProcessID
  LEFT JOIN GlassfishDB.Project   pr ON pr.ProjectID = p.ProjectID
  LEFT JOIN GlassfishDB.SOS       s  ON s.SOSID      = m.SOSID
  LEFT JOIN GlassfishDB.SOSAction sa ON sa.SOSID     = m.SOSID
                                    AND sa.SOSActionID = m.SOSActionID
  WHERE m.MessageLastUpdate >= ''2025-11-30 04:09:47''
    AND m.MessageLastUpdate <= ''2025-12-30 04:09:47''
    AND m.ProcessID IN (', @pids, ')
    AND (m.MessageLastUpdate < @c_ts
         OR (m.MessageLastUpdate = @c_ts AND m.MessageID < @c_id))
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51');
SET @s_huelle_plan = CONCAT('EXPLAIN SELECT COUNT(*)                         AS zeilen,
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
  SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID,
         p.ProcessName, pr.ProjectName, s.SOSName, sa.SOSActionName
  FROM GlassfishDB.Message m
  LEFT JOIN GlassfishDB.Process   p  ON p.ProcessID  = m.ProcessID
  LEFT JOIN GlassfishDB.Project   pr ON pr.ProjectID = p.ProjectID
  LEFT JOIN GlassfishDB.SOS       s  ON s.SOSID      = m.SOSID
  LEFT JOIN GlassfishDB.SOSAction sa ON sa.SOSID     = m.SOSID
                                    AND sa.SOSActionID = m.SOSActionID
  WHERE m.MessageLastUpdate >= ''2025-11-30 04:09:47''
    AND m.MessageLastUpdate <= ''2025-12-30 04:09:47''
    AND m.ProcessID IN (', @pids, ')
    AND (m.MessageLastUpdate < @c_ts
         OR (m.MessageLastUpdate = @c_ts AND m.MessageID < @c_id))
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
SET @s_huelle = CONCAT('SELECT COUNT(*)                         AS zeilen,
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
  SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID,
         p.ProcessName, pr.ProjectName, s.SOSName, sa.SOSActionName
  FROM GlassfishDB.Message m
  LEFT JOIN GlassfishDB.Process   p  ON p.ProcessID  = m.ProcessID
  LEFT JOIN GlassfishDB.Project   pr ON pr.ProjectID = p.ProjectID
  LEFT JOIN GlassfishDB.SOS       s  ON s.SOSID      = m.SOSID
  LEFT JOIN GlassfishDB.SOSAction sa ON sa.SOSID     = m.SOSID
                                    AND sa.SOSActionID = m.SOSActionID
  WHERE m.MessageLastUpdate >= ''2025-11-30 04:09:47''
    AND m.MessageLastUpdate <= ''2025-12-30 04:09:47''
    AND m.ProcessID IN (', @pids, ')
    AND (m.MessageLastUpdate < @c_ts
         OR (m.MessageLastUpdate = @c_ts AND m.MessageID < @c_id))
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
PREPARE p_nackt_F1_referenz_mitCursor      FROM @s_nackt;
PREPARE p_huelleplan_F1_referenz_mitCursor FROM @s_huelle_plan;
PREPARE p_huelle_F1_referenz_mitCursor     FROM @s_huelle;

SELECT '=== F1-referenz-mitCursor · Plan des NACKTEN Statements (L15) ===' AS marke;
EXECUTE p_nackt_F1_referenz_mitCursor;
SELECT '=== F1-referenz-mitCursor · Plan der gemessenen Huelle ===' AS marke;
EXECUTE p_huelleplan_F1_referenz_mitCursor;

SELECT '=== F1-referenz-mitCursor · Ergebnis + Aufwaermlauf + fuenf ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_huelle_F1_referenz_mitCursor;
EXECUTE p_huelle_F1_referenz_mitCursor;
EXECUTE p_huelle_F1_referenz_mitCursor;
EXECUTE p_huelle_F1_referenz_mitCursor;
EXECUTE p_huelle_F1_referenz_mitCursor;
EXECUTE p_huelle_F1_referenz_mitCursor;
SELECT 'F1-referenz-mitCursor' AS fall,
       COUNT(*)                                       AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)              AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)              AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

DEALLOCATE PREPARE p_nackt_F1_referenz_mitCursor;
DEALLOCATE PREPARE p_huelleplan_F1_referenz_mitCursor;
DEALLOCATE PREPARE p_huelle_F1_referenz_mitCursor;

-- ==========================================================================
-- F1-ueberfaellig-ohneCursor · NEXANS
-- ==========================================================================
SET @s_nackt = CONCAT('EXPLAIN
  SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID,
         p.ProcessName, pr.ProjectName, s.SOSName, sa.SOSActionName
  FROM GlassfishDB.Message m
  LEFT JOIN GlassfishDB.Process   p  ON p.ProcessID  = m.ProcessID
  LEFT JOIN GlassfishDB.Project   pr ON pr.ProjectID = p.ProjectID
  LEFT JOIN GlassfishDB.SOS       s  ON s.SOSID      = m.SOSID
  LEFT JOIN GlassfishDB.SOSAction sa ON sa.SOSID     = m.SOSID
                                    AND sa.SOSActionID = m.SOSActionID
  WHERE m.MessageLastUpdate >= ''2025-11-30 04:09:47''
    AND m.MessageLastUpdate <= ''2025-12-30 04:09:47''
    AND m.ProcessID IN (', @pids, ')
    AND m.MessageStatus IN (''SUSPENDED'',''RUNNING'')
    AND m.MessageTimeout IS NOT NULL
    AND m.MessageTimeout > 0
    AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND < ''2025-12-30 04:09:47''
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51');
SET @s_huelle_plan = CONCAT('EXPLAIN SELECT COUNT(*)                         AS zeilen,
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
  SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID,
         p.ProcessName, pr.ProjectName, s.SOSName, sa.SOSActionName
  FROM GlassfishDB.Message m
  LEFT JOIN GlassfishDB.Process   p  ON p.ProcessID  = m.ProcessID
  LEFT JOIN GlassfishDB.Project   pr ON pr.ProjectID = p.ProjectID
  LEFT JOIN GlassfishDB.SOS       s  ON s.SOSID      = m.SOSID
  LEFT JOIN GlassfishDB.SOSAction sa ON sa.SOSID     = m.SOSID
                                    AND sa.SOSActionID = m.SOSActionID
  WHERE m.MessageLastUpdate >= ''2025-11-30 04:09:47''
    AND m.MessageLastUpdate <= ''2025-12-30 04:09:47''
    AND m.ProcessID IN (', @pids, ')
    AND m.MessageStatus IN (''SUSPENDED'',''RUNNING'')
    AND m.MessageTimeout IS NOT NULL
    AND m.MessageTimeout > 0
    AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND < ''2025-12-30 04:09:47''
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
SET @s_huelle = CONCAT('SELECT COUNT(*)                         AS zeilen,
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
  SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID,
         p.ProcessName, pr.ProjectName, s.SOSName, sa.SOSActionName
  FROM GlassfishDB.Message m
  LEFT JOIN GlassfishDB.Process   p  ON p.ProcessID  = m.ProcessID
  LEFT JOIN GlassfishDB.Project   pr ON pr.ProjectID = p.ProjectID
  LEFT JOIN GlassfishDB.SOS       s  ON s.SOSID      = m.SOSID
  LEFT JOIN GlassfishDB.SOSAction sa ON sa.SOSID     = m.SOSID
                                    AND sa.SOSActionID = m.SOSActionID
  WHERE m.MessageLastUpdate >= ''2025-11-30 04:09:47''
    AND m.MessageLastUpdate <= ''2025-12-30 04:09:47''
    AND m.ProcessID IN (', @pids, ')
    AND m.MessageStatus IN (''SUSPENDED'',''RUNNING'')
    AND m.MessageTimeout IS NOT NULL
    AND m.MessageTimeout > 0
    AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND < ''2025-12-30 04:09:47''
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
PREPARE p_nackt_F1_ueberfaellig_ohneCursor      FROM @s_nackt;
PREPARE p_huelleplan_F1_ueberfaellig_ohneCursor FROM @s_huelle_plan;
PREPARE p_huelle_F1_ueberfaellig_ohneCursor     FROM @s_huelle;

SELECT '=== F1-ueberfaellig-ohneCursor · Plan des NACKTEN Statements (L15) ===' AS marke;
EXECUTE p_nackt_F1_ueberfaellig_ohneCursor;
SELECT '=== F1-ueberfaellig-ohneCursor · Plan der gemessenen Huelle ===' AS marke;
EXECUTE p_huelleplan_F1_ueberfaellig_ohneCursor;

SELECT '=== F1-ueberfaellig-ohneCursor · Ergebnis + Aufwaermlauf + fuenf ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_huelle_F1_ueberfaellig_ohneCursor;
EXECUTE p_huelle_F1_ueberfaellig_ohneCursor;
EXECUTE p_huelle_F1_ueberfaellig_ohneCursor;
EXECUTE p_huelle_F1_ueberfaellig_ohneCursor;
EXECUTE p_huelle_F1_ueberfaellig_ohneCursor;
EXECUTE p_huelle_F1_ueberfaellig_ohneCursor;
SELECT 'F1-ueberfaellig-ohneCursor' AS fall,
       COUNT(*)                                       AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)              AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)              AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

DEALLOCATE PREPARE p_nackt_F1_ueberfaellig_ohneCursor;
DEALLOCATE PREPARE p_huelleplan_F1_ueberfaellig_ohneCursor;
DEALLOCATE PREPARE p_huelle_F1_ueberfaellig_ohneCursor;

-- ==========================================================================
-- F1-ueberfaellig-mitCursor · NEXANS
-- ==========================================================================
SET @s_nackt = CONCAT('EXPLAIN
  SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID,
         p.ProcessName, pr.ProjectName, s.SOSName, sa.SOSActionName
  FROM GlassfishDB.Message m
  LEFT JOIN GlassfishDB.Process   p  ON p.ProcessID  = m.ProcessID
  LEFT JOIN GlassfishDB.Project   pr ON pr.ProjectID = p.ProjectID
  LEFT JOIN GlassfishDB.SOS       s  ON s.SOSID      = m.SOSID
  LEFT JOIN GlassfishDB.SOSAction sa ON sa.SOSID     = m.SOSID
                                    AND sa.SOSActionID = m.SOSActionID
  WHERE m.MessageLastUpdate >= ''2025-11-30 04:09:47''
    AND m.MessageLastUpdate <= ''2025-12-30 04:09:47''
    AND m.ProcessID IN (', @pids, ')
    AND m.MessageStatus IN (''SUSPENDED'',''RUNNING'')
    AND m.MessageTimeout IS NOT NULL
    AND m.MessageTimeout > 0
    AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND < ''2025-12-30 04:09:47''
    AND (m.MessageLastUpdate < @u_ts
         OR (m.MessageLastUpdate = @u_ts AND m.MessageID < @u_id))
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51');
SET @s_huelle_plan = CONCAT('EXPLAIN SELECT COUNT(*)                         AS zeilen,
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
  SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID,
         p.ProcessName, pr.ProjectName, s.SOSName, sa.SOSActionName
  FROM GlassfishDB.Message m
  LEFT JOIN GlassfishDB.Process   p  ON p.ProcessID  = m.ProcessID
  LEFT JOIN GlassfishDB.Project   pr ON pr.ProjectID = p.ProjectID
  LEFT JOIN GlassfishDB.SOS       s  ON s.SOSID      = m.SOSID
  LEFT JOIN GlassfishDB.SOSAction sa ON sa.SOSID     = m.SOSID
                                    AND sa.SOSActionID = m.SOSActionID
  WHERE m.MessageLastUpdate >= ''2025-11-30 04:09:47''
    AND m.MessageLastUpdate <= ''2025-12-30 04:09:47''
    AND m.ProcessID IN (', @pids, ')
    AND m.MessageStatus IN (''SUSPENDED'',''RUNNING'')
    AND m.MessageTimeout IS NOT NULL
    AND m.MessageTimeout > 0
    AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND < ''2025-12-30 04:09:47''
    AND (m.MessageLastUpdate < @u_ts
         OR (m.MessageLastUpdate = @u_ts AND m.MessageID < @u_id))
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
SET @s_huelle = CONCAT('SELECT COUNT(*)                         AS zeilen,
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
  SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID,
         p.ProcessName, pr.ProjectName, s.SOSName, sa.SOSActionName
  FROM GlassfishDB.Message m
  LEFT JOIN GlassfishDB.Process   p  ON p.ProcessID  = m.ProcessID
  LEFT JOIN GlassfishDB.Project   pr ON pr.ProjectID = p.ProjectID
  LEFT JOIN GlassfishDB.SOS       s  ON s.SOSID      = m.SOSID
  LEFT JOIN GlassfishDB.SOSAction sa ON sa.SOSID     = m.SOSID
                                    AND sa.SOSActionID = m.SOSActionID
  WHERE m.MessageLastUpdate >= ''2025-11-30 04:09:47''
    AND m.MessageLastUpdate <= ''2025-12-30 04:09:47''
    AND m.ProcessID IN (', @pids, ')
    AND m.MessageStatus IN (''SUSPENDED'',''RUNNING'')
    AND m.MessageTimeout IS NOT NULL
    AND m.MessageTimeout > 0
    AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND < ''2025-12-30 04:09:47''
    AND (m.MessageLastUpdate < @u_ts
         OR (m.MessageLastUpdate = @u_ts AND m.MessageID < @u_id))
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
PREPARE p_nackt_F1_ueberfaellig_mitCursor      FROM @s_nackt;
PREPARE p_huelleplan_F1_ueberfaellig_mitCursor FROM @s_huelle_plan;
PREPARE p_huelle_F1_ueberfaellig_mitCursor     FROM @s_huelle;

SELECT '=== F1-ueberfaellig-mitCursor · Plan des NACKTEN Statements (L15) ===' AS marke;
EXECUTE p_nackt_F1_ueberfaellig_mitCursor;
SELECT '=== F1-ueberfaellig-mitCursor · Plan der gemessenen Huelle ===' AS marke;
EXECUTE p_huelleplan_F1_ueberfaellig_mitCursor;

SELECT '=== F1-ueberfaellig-mitCursor · Ergebnis + Aufwaermlauf + fuenf ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_huelle_F1_ueberfaellig_mitCursor;
EXECUTE p_huelle_F1_ueberfaellig_mitCursor;
EXECUTE p_huelle_F1_ueberfaellig_mitCursor;
EXECUTE p_huelle_F1_ueberfaellig_mitCursor;
EXECUTE p_huelle_F1_ueberfaellig_mitCursor;
EXECUTE p_huelle_F1_ueberfaellig_mitCursor;
SELECT 'F1-ueberfaellig-mitCursor' AS fall,
       COUNT(*)                                       AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)              AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)              AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

DEALLOCATE PREPARE p_nackt_F1_ueberfaellig_mitCursor;
DEALLOCATE PREPARE p_huelleplan_F1_ueberfaellig_mitCursor;
DEALLOCATE PREPARE p_huelle_F1_ueberfaellig_mitCursor;

SELECT '=== Abschluss: read_only ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;
