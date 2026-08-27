-- Messung A.1 vor Schritt 10b-1 — Gegenprobe 5: der Prozessfilter · NEXANS
-- Auftrag:  "Schritt 10b-1", Stand 27.08.2026, Teil A.1
-- Ergebnis: docs/nachrichtenliste.md §5a
--
-- Ausschliesslich SELECT / SET / EXPLAIN / PREPARE. (Regel S1)
--
-- WARUM DIESE SITZUNG DIE VORIGE ERSETZT: In `a1-gegenprobe-filter-*.sql` stand der
-- Prozessfilter als `m.ProcessID IN (@p1, @p2, @p3)`. **Eine Sitzungsvariable taugt
-- dafuer nicht:** MariaDB fuehrt darauf keine Bereichsanalyse aus, `ProejctIDIDX` blieb
-- in `possible_keys` und wurde nie als Bereich genutzt. Gemessen war damit nicht der
-- Prozessfilter, sondern seine Abwesenheit. Hier stehen die Kennungen als **Literale**
-- im Statement — dieselbe Form, die der Treiber auf die Leitung legt.
--
-- G1: Die Kennungen entstehen zur Laufzeit in @proz_* und stehen weder in dieser Datei
--     noch in der Ausgabe. `SHOW PROFILES` wird deshalb nicht verwendet (es gaebe den
--     Statementtext aus); die Laufzeiten kommen aus `information_schema.PROFILING`.
-- Z1: Anker 2025-12-30 04:09:47 als Literal.

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;

SET max_statement_time = 60;
SET profiling = 1;
SET profiling_history_size = 100;
SET SESSION group_concat_max_len = 8000000;

-- ── Zwei Prozesswahlen, deterministisch ────────────────────────────────────
--   `gross` — die drei Prozesse mit den meisten Nachrichten im Fenster.
--             Der guenstige Fall: die Seite ist sofort voll.
--   `klein` — die drei mit den wenigsten (aber mehr als null).
--             Der teure Fall: MariaDB muss durch das ganze Fenster.
-- Gelesen aus `message_rollup`, nicht aus `Message` (Regel L2).
SELECT GROUP_CONCAT(QUOTE(process_id)) INTO @proz_gross FROM (
  SELECT r.process_id FROM overlord_monitor.message_rollup r
    JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
    JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
   WHERE pm.MandantID = 'NEXANS'
     AND r.stunde >= '2025-11-30 04:09:47' AND r.stunde < '2025-12-30 04:09:47'
   GROUP BY r.process_id ORDER BY SUM(r.anzahl) DESC, r.process_id LIMIT 3) g;

SELECT GROUP_CONCAT(QUOTE(process_id)) INTO @proz_klein FROM (
  SELECT r.process_id FROM overlord_monitor.message_rollup r
    JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
    JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
   WHERE pm.MandantID = 'NEXANS'
     AND r.stunde >= '2025-11-30 04:09:47' AND r.stunde < '2025-12-30 04:09:47'
   GROUP BY r.process_id ORDER BY SUM(r.anzahl) ASC, r.process_id LIMIT 3) k;

SELECT '=== Prozesswahl gesetzt (ohne Kennungen) ===' AS marke;
SELECT (LENGTH(@proz_gross) - LENGTH(REPLACE(@proz_gross, ',', ''))) + 1 AS gross_anzahl,
       (LENGTH(@proz_klein) - LENGTH(REPLACE(@proz_klein, ',', ''))) + 1 AS klein_anzahl,
       (@proz_gross <=> @proz_klein) AS gleiche_wahl;


-- ==========================================================================
-- F0-NEXANS-30d-prozess-gross
-- ==========================================================================
SET @proz = @proz_gross;
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
    AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''NEXANS'')
    AND m.ProcessID IN (', @proz, ')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51');
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
    AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''NEXANS'')
    AND m.ProcessID IN (', @proz, ')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
PREPARE p_n_F0_NEXANS_30d_prozess_gross FROM @s_nackt;
PREPARE p_h_F0_NEXANS_30d_prozess_gross FROM @s_huelle;
SELECT '=== F0-NEXANS-30d-prozess-gross · Plan (L15) ===' AS marke;
EXECUTE p_n_F0_NEXANS_30d_prozess_gross;
SELECT '=== F0-NEXANS-30d-prozess-gross · Ergebnis + Aufwaermlauf + fuenf ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_h_F0_NEXANS_30d_prozess_gross;
EXECUTE p_h_F0_NEXANS_30d_prozess_gross;
EXECUTE p_h_F0_NEXANS_30d_prozess_gross;
EXECUTE p_h_F0_NEXANS_30d_prozess_gross;
EXECUTE p_h_F0_NEXANS_30d_prozess_gross;
EXECUTE p_h_F0_NEXANS_30d_prozess_gross;
SELECT 'F0-NEXANS-30d-prozess-gross' AS fall,
       COUNT(*)                                       AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)              AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)              AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

DEALLOCATE PREPARE p_n_F0_NEXANS_30d_prozess_gross;
DEALLOCATE PREPARE p_h_F0_NEXANS_30d_prozess_gross;

-- ==========================================================================
-- F4-NEXANS-30d-prozess-gross
-- ==========================================================================
SET @proz = @proz_gross;
SET @s_nackt = CONCAT('EXPLAIN
  SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID,
         p.ProcessName, pr.ProjectName, s.SOSName, sa.SOSActionName
  FROM GlassfishDB.Message m IGNORE INDEX FOR JOIN (ProejctIDIDX, Message_ProcessFK)
  LEFT JOIN GlassfishDB.Process   p  ON p.ProcessID  = m.ProcessID
  LEFT JOIN GlassfishDB.Project   pr ON pr.ProjectID = p.ProjectID
  LEFT JOIN GlassfishDB.SOS       s  ON s.SOSID      = m.SOSID
  LEFT JOIN GlassfishDB.SOSAction sa ON sa.SOSID     = m.SOSID
                                    AND sa.SOSActionID = m.SOSActionID
  WHERE m.MessageLastUpdate >= ''2025-11-30 04:09:47''
    AND m.MessageLastUpdate <= ''2025-12-30 04:09:47''
    AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''NEXANS'')
    AND m.ProcessID IN (', @proz, ')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51');
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
  FROM GlassfishDB.Message m IGNORE INDEX FOR JOIN (ProejctIDIDX, Message_ProcessFK)
  LEFT JOIN GlassfishDB.Process   p  ON p.ProcessID  = m.ProcessID
  LEFT JOIN GlassfishDB.Project   pr ON pr.ProjectID = p.ProjectID
  LEFT JOIN GlassfishDB.SOS       s  ON s.SOSID      = m.SOSID
  LEFT JOIN GlassfishDB.SOSAction sa ON sa.SOSID     = m.SOSID
                                    AND sa.SOSActionID = m.SOSActionID
  WHERE m.MessageLastUpdate >= ''2025-11-30 04:09:47''
    AND m.MessageLastUpdate <= ''2025-12-30 04:09:47''
    AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''NEXANS'')
    AND m.ProcessID IN (', @proz, ')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
PREPARE p_n_F4_NEXANS_30d_prozess_gross FROM @s_nackt;
PREPARE p_h_F4_NEXANS_30d_prozess_gross FROM @s_huelle;
SELECT '=== F4-NEXANS-30d-prozess-gross · Plan (L15) ===' AS marke;
EXECUTE p_n_F4_NEXANS_30d_prozess_gross;
SELECT '=== F4-NEXANS-30d-prozess-gross · Ergebnis + Aufwaermlauf + fuenf ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_h_F4_NEXANS_30d_prozess_gross;
EXECUTE p_h_F4_NEXANS_30d_prozess_gross;
EXECUTE p_h_F4_NEXANS_30d_prozess_gross;
EXECUTE p_h_F4_NEXANS_30d_prozess_gross;
EXECUTE p_h_F4_NEXANS_30d_prozess_gross;
EXECUTE p_h_F4_NEXANS_30d_prozess_gross;
SELECT 'F4-NEXANS-30d-prozess-gross' AS fall,
       COUNT(*)                                       AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)              AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)              AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

DEALLOCATE PREPARE p_n_F4_NEXANS_30d_prozess_gross;
DEALLOCATE PREPARE p_h_F4_NEXANS_30d_prozess_gross;

-- ==========================================================================
-- F0-NEXANS-30d-prozess-klein
-- ==========================================================================
SET @proz = @proz_klein;
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
    AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''NEXANS'')
    AND m.ProcessID IN (', @proz, ')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51');
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
    AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''NEXANS'')
    AND m.ProcessID IN (', @proz, ')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
PREPARE p_n_F0_NEXANS_30d_prozess_klein FROM @s_nackt;
PREPARE p_h_F0_NEXANS_30d_prozess_klein FROM @s_huelle;
SELECT '=== F0-NEXANS-30d-prozess-klein · Plan (L15) ===' AS marke;
EXECUTE p_n_F0_NEXANS_30d_prozess_klein;
SELECT '=== F0-NEXANS-30d-prozess-klein · Ergebnis + Aufwaermlauf + fuenf ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_h_F0_NEXANS_30d_prozess_klein;
EXECUTE p_h_F0_NEXANS_30d_prozess_klein;
EXECUTE p_h_F0_NEXANS_30d_prozess_klein;
EXECUTE p_h_F0_NEXANS_30d_prozess_klein;
EXECUTE p_h_F0_NEXANS_30d_prozess_klein;
EXECUTE p_h_F0_NEXANS_30d_prozess_klein;
SELECT 'F0-NEXANS-30d-prozess-klein' AS fall,
       COUNT(*)                                       AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)              AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)              AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

DEALLOCATE PREPARE p_n_F0_NEXANS_30d_prozess_klein;
DEALLOCATE PREPARE p_h_F0_NEXANS_30d_prozess_klein;

-- ==========================================================================
-- F4-NEXANS-30d-prozess-klein
-- ==========================================================================
SET @proz = @proz_klein;
SET @s_nackt = CONCAT('EXPLAIN
  SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID,
         p.ProcessName, pr.ProjectName, s.SOSName, sa.SOSActionName
  FROM GlassfishDB.Message m IGNORE INDEX FOR JOIN (ProejctIDIDX, Message_ProcessFK)
  LEFT JOIN GlassfishDB.Process   p  ON p.ProcessID  = m.ProcessID
  LEFT JOIN GlassfishDB.Project   pr ON pr.ProjectID = p.ProjectID
  LEFT JOIN GlassfishDB.SOS       s  ON s.SOSID      = m.SOSID
  LEFT JOIN GlassfishDB.SOSAction sa ON sa.SOSID     = m.SOSID
                                    AND sa.SOSActionID = m.SOSActionID
  WHERE m.MessageLastUpdate >= ''2025-11-30 04:09:47''
    AND m.MessageLastUpdate <= ''2025-12-30 04:09:47''
    AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''NEXANS'')
    AND m.ProcessID IN (', @proz, ')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51');
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
  FROM GlassfishDB.Message m IGNORE INDEX FOR JOIN (ProejctIDIDX, Message_ProcessFK)
  LEFT JOIN GlassfishDB.Process   p  ON p.ProcessID  = m.ProcessID
  LEFT JOIN GlassfishDB.Project   pr ON pr.ProjectID = p.ProjectID
  LEFT JOIN GlassfishDB.SOS       s  ON s.SOSID      = m.SOSID
  LEFT JOIN GlassfishDB.SOSAction sa ON sa.SOSID     = m.SOSID
                                    AND sa.SOSActionID = m.SOSActionID
  WHERE m.MessageLastUpdate >= ''2025-11-30 04:09:47''
    AND m.MessageLastUpdate <= ''2025-12-30 04:09:47''
    AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''NEXANS'')
    AND m.ProcessID IN (', @proz, ')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
PREPARE p_n_F4_NEXANS_30d_prozess_klein FROM @s_nackt;
PREPARE p_h_F4_NEXANS_30d_prozess_klein FROM @s_huelle;
SELECT '=== F4-NEXANS-30d-prozess-klein · Plan (L15) ===' AS marke;
EXECUTE p_n_F4_NEXANS_30d_prozess_klein;
SELECT '=== F4-NEXANS-30d-prozess-klein · Ergebnis + Aufwaermlauf + fuenf ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_h_F4_NEXANS_30d_prozess_klein;
EXECUTE p_h_F4_NEXANS_30d_prozess_klein;
EXECUTE p_h_F4_NEXANS_30d_prozess_klein;
EXECUTE p_h_F4_NEXANS_30d_prozess_klein;
EXECUTE p_h_F4_NEXANS_30d_prozess_klein;
EXECUTE p_h_F4_NEXANS_30d_prozess_klein;
SELECT 'F4-NEXANS-30d-prozess-klein' AS fall,
       COUNT(*)                                       AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)              AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)              AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;

DEALLOCATE PREPARE p_n_F4_NEXANS_30d_prozess_klein;
DEALLOCATE PREPARE p_h_F4_NEXANS_30d_prozess_klein;

SELECT '=== Abschluss: read_only ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;
