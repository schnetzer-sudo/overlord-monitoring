-- Messung M100 - Die Verengung unter dem Prozessfilter - NEXANS
-- Auftrag:  "Fensterverengung fuer die Nachrichtenliste", Stand 27.08.2026
-- Ergebnis: docs/messungen-liste-verengung.md
--
-- M100 - Der Prozessfilter, der 7,46-s-Fall. Dieselben zwei Prozessmengen wie in 10b-1
-- (a1-gegenprobe-prozessfilter-nexans.sql): die drei mit den MEISTEN und die drei mit den
-- WENIGSTEN Nachrichten im Fenster, deterministisch aus message_rollup hergeleitet (L2).
-- Die Verengung fragt den Rollup nur nach DIESEN Prozessen - er ist nach process_id
-- geschluesselt und weiss genau, welche Stunden sie tragen.
--
-- Ausschliesslich SELECT / SET / EXPLAIN / PREPARE / DEALLOCATE. (Regel S1)
--
-- G1 - dieselbe Bauform wie 10b-1 (docs/nachrichtenliste.md 5a), aus demselben Grund:
--   Die Listenabfrage liefert MessageID, ProcessID, ProcessName und ProjectName; sie darf
--   deshalb NICHT ausgegeben werden.
--     * Der PLAN (L15) wird am NACKTEN Statement erhoben - EXPLAIN fuehrt es nicht aus.
--     * Die LAUFZEIT (L7) wird an derselben Abfrage in einer aggregierenden Huelle gemessen,
--       die JEDE gejointe Spalte anfasst - sonst optimiert MariaDB die LEFT JOIN weg.
--   Die Prozesskennungen des Mandanten stehen in @proz und weder in dieser Datei noch in
--   der Ausgabe. Sie muessen als LITERAL ins Statement (nicht als Sitzungsvariable), sonst
--   fuehrt MariaDB keine Bereichsanalyse aus - der Fehler, an dem die erste Fassung der
--   Prozessfilter-Gegenprobe in 10b-1 gescheitert ist. Daher CONCAT + PREPARE.
--
-- Z1: 'jetzt' ist der Anker der Anwendungsuhr 2025-12-30 04:09:47 (V5 der Vorrunde) und steht als
--     Literal. Fenster = zeitraum=30d dagegen, beide Grenzen einschliesslich (.ge/.le),
--     LIMIT 51 = 50 + 1, wie NachrichtenRepository liest.
--
-- Die Stundeneimer des Rollups sind halboffen: [stunde, stunde+1h). Fuer die kumulierte
-- Summe zaehlen nur VOLLSTAENDIG im Fenster liegende Stunden (2025-11-30 05:00:00
-- bis 2025-12-30 03:00:00); die beiden angebrochenen Randstunden gehen mit 0 ein.
-- Damit ist die gezaehlte Menge stets eine UNTERSCHRANKE der wirklichen - die Verengung
-- kann nie zu weit gehen und nie eine Zeile verlieren. Fuer den Nullfall zaehlt dagegen
-- der ganze ueberlappende Bereich, sonst waere die Aussage 'null Zeilen' nicht gedeckt.
--
-- STRAIGHT_JOIN kommt in keiner Fassung vor (M42). ANALYZE TABLE kommt nicht vor (S1).

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, VERSION() AS version, NOW() AS serverzeit;
SELECT COUNT(*) AS message_zeilen, MAX(MessageLastUpdate) AS datenstand
FROM GlassfishDB.Message;
SELECT (SELECT COUNT(*)     FROM overlord_monitor.message_rollup) AS rollup_zeilen,
       (SELECT SUM(anzahl)  FROM overlord_monitor.message_rollup) AS rollup_summe,
       (SELECT MAX(fenster_bis) FROM overlord_monitor.rollup_lauf
         WHERE beendet_am IS NOT NULL AND fehler IS NULL)         AS wasserstand;

SET max_statement_time = 60;
SET profiling = 1;
SET profiling_history_size = 400;
SET SESSION group_concat_max_len = 1048576;

-- Die beiden Prozessmengen, deterministisch und wortgleich zu 10b-1 hergeleitet (Regel L2).
-- Die Kennungen bleiben in den Variablen; weder Datei noch Ausgabe zeigen sie. (G1)
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

SELECT '=== Die beiden Prozessmengen (ohne Kennungen) ===' AS marke;
SELECT (@proz_gross IS NOT NULL) AS gross_vorhanden,
       LENGTH(@proz_gross) - LENGTH(REPLACE(@proz_gross, ',', '')) + 1 AS gross_anzahl,
       (@proz_klein IS NOT NULL) AS klein_vorhanden,
       LENGTH(@proz_klein) - LENGTH(REPLACE(@proz_klein, ',', '')) + 1 AS klein_anzahl,
       (@proz_gross <=> @proz_klein) AS gleiche_wahl;

SELECT '########## NEXANS - drei grosse Prozesse ##########' AS marke;
SET @proz = @proz_gross;

SELECT '=== F0-NEXANS-30d-prozess-gross - Plan (L15) ===' AS marke;
SET @s = CONCAT('EXPLAIN ', CONCAT('SELECT COUNT(*)                         AS zeilen,
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
) t'));
PREPARE p_F0_NEXANS_30d_prozess_gross_pl FROM @s;
EXECUTE p_F0_NEXANS_30d_prozess_gross_pl;
DEALLOCATE PREPARE p_F0_NEXANS_30d_prozess_gross_pl;
SELECT '=== F0-NEXANS-30d-prozess-gross - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = CONCAT('SELECT COUNT(*)                         AS zeilen,
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
PREPARE p_F0_NEXANS_30d_prozess_gross FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_F0_NEXANS_30d_prozess_gross;
EXECUTE p_F0_NEXANS_30d_prozess_gross;
EXECUTE p_F0_NEXANS_30d_prozess_gross;
EXECUTE p_F0_NEXANS_30d_prozess_gross;
EXECUTE p_F0_NEXANS_30d_prozess_gross;
EXECUTE p_F0_NEXANS_30d_prozess_gross;
SELECT 'F0-NEXANS-30d-prozess-gross' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_F0_NEXANS_30d_prozess_gross;

SELECT '=== VA-NEXANS-prozess-gross-1h - Plan (L15) ===' AS marke;
SET @s = CONCAT('EXPLAIN ', CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-12-30 03:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g'));
PREPARE p_VA_NEXANS_prozess_gross_1h_pl FROM @s;
EXECUTE p_VA_NEXANS_prozess_gross_1h_pl;
DEALLOCATE PREPARE p_VA_NEXANS_prozess_gross_1h_pl;
SELECT '=== VA-NEXANS-prozess-gross-1h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-12-30 03:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_VA_NEXANS_prozess_gross_1h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VA_NEXANS_prozess_gross_1h;
EXECUTE p_VA_NEXANS_prozess_gross_1h;
EXECUTE p_VA_NEXANS_prozess_gross_1h;
EXECUTE p_VA_NEXANS_prozess_gross_1h;
EXECUTE p_VA_NEXANS_prozess_gross_1h;
EXECUTE p_VA_NEXANS_prozess_gross_1h;
SELECT 'VA-NEXANS-prozess-gross-1h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VA_NEXANS_prozess_gross_1h;

SELECT '=== VA-NEXANS-prozess-gross-24h - Plan (L15) ===' AS marke;
SET @s = CONCAT('EXPLAIN ', CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-12-29 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g'));
PREPARE p_VA_NEXANS_prozess_gross_24h_pl FROM @s;
EXECUTE p_VA_NEXANS_prozess_gross_24h_pl;
DEALLOCATE PREPARE p_VA_NEXANS_prozess_gross_24h_pl;
SELECT '=== VA-NEXANS-prozess-gross-24h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-12-29 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_VA_NEXANS_prozess_gross_24h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VA_NEXANS_prozess_gross_24h;
EXECUTE p_VA_NEXANS_prozess_gross_24h;
EXECUTE p_VA_NEXANS_prozess_gross_24h;
EXECUTE p_VA_NEXANS_prozess_gross_24h;
EXECUTE p_VA_NEXANS_prozess_gross_24h;
EXECUTE p_VA_NEXANS_prozess_gross_24h;
SELECT 'VA-NEXANS-prozess-gross-24h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VA_NEXANS_prozess_gross_24h;

SELECT '=== VA-NEXANS-prozess-gross-7d - Plan (L15) ===' AS marke;
SET @s = CONCAT('EXPLAIN ', CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-12-23 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g'));
PREPARE p_VA_NEXANS_prozess_gross_7d_pl FROM @s;
EXECUTE p_VA_NEXANS_prozess_gross_7d_pl;
DEALLOCATE PREPARE p_VA_NEXANS_prozess_gross_7d_pl;
SELECT '=== VA-NEXANS-prozess-gross-7d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-12-23 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_VA_NEXANS_prozess_gross_7d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VA_NEXANS_prozess_gross_7d;
EXECUTE p_VA_NEXANS_prozess_gross_7d;
EXECUTE p_VA_NEXANS_prozess_gross_7d;
EXECUTE p_VA_NEXANS_prozess_gross_7d;
EXECUTE p_VA_NEXANS_prozess_gross_7d;
EXECUTE p_VA_NEXANS_prozess_gross_7d;
SELECT 'VA-NEXANS-prozess-gross-7d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VA_NEXANS_prozess_gross_7d;

SELECT '=== VA-NEXANS-prozess-gross-30d - Plan (L15) ===' AS marke;
SET @s = CONCAT('EXPLAIN ', CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g'));
PREPARE p_VA_NEXANS_prozess_gross_30d_pl FROM @s;
EXECUTE p_VA_NEXANS_prozess_gross_30d_pl;
DEALLOCATE PREPARE p_VA_NEXANS_prozess_gross_30d_pl;
SELECT '=== VA-NEXANS-prozess-gross-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_VA_NEXANS_prozess_gross_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VA_NEXANS_prozess_gross_30d;
EXECUTE p_VA_NEXANS_prozess_gross_30d;
EXECUTE p_VA_NEXANS_prozess_gross_30d;
EXECUTE p_VA_NEXANS_prozess_gross_30d;
EXECUTE p_VA_NEXANS_prozess_gross_30d;
EXECUTE p_VA_NEXANS_prozess_gross_30d;
SELECT 'VA-NEXANS-prozess-gross-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VA_NEXANS_prozess_gross_30d;

SELECT '=== NEXANS-prozess-gross - die Verengung ausgerechnet ===' AS marke;
SET @zeilen = NULL, @ug = NULL;
SET @s = CONCAT('SELECT SUM(g.anzahl), MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) INTO @zeilen, @ug
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_rechne FROM @s;
EXECUTE p_rechne;
DEALLOCATE PREPARE p_rechne;
SET @ug_wirksam = GREATEST('2025-11-30 04:09:47', COALESCE(@ug, '2025-11-30 04:09:47'));
SELECT 'NEXANS-prozess-gross' AS fall,
       @zeilen                                    AS zeilen_im_fenster,
       @ug                                        AS untergrenze_roh,
       @ug_wirksam                                AS untergrenze,
       TIMESTAMPDIFF(HOUR, @ug_wirksam, '2025-12-30 04:09:47')   AS fenster_stunden,
       (@zeilen IS NULL OR @zeilen = 0)           AS nullfall,
       (@ug IS NULL)                              AS keine_verengung;

SELECT '=== VERENGT-NEXANS-30d-prozess-gross - Plan (L15) ===' AS marke;
SET @s = CONCAT('EXPLAIN ', CONCAT('SELECT COUNT(*)                         AS zeilen,
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
  WHERE m.MessageLastUpdate >= ''', @ug_wirksam, '''
    AND m.MessageLastUpdate <= ''2025-12-30 04:09:47''
    AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''NEXANS'')
    AND m.ProcessID IN (', @proz, ')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t'));
PREPARE p_VERENGT_NEXANS_30d_prozess_gross_pl FROM @s;
EXECUTE p_VERENGT_NEXANS_30d_prozess_gross_pl;
DEALLOCATE PREPARE p_VERENGT_NEXANS_30d_prozess_gross_pl;
SELECT '=== VERENGT-NEXANS-30d-prozess-gross - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = CONCAT('SELECT COUNT(*)                         AS zeilen,
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
  WHERE m.MessageLastUpdate >= ''', @ug_wirksam, '''
    AND m.MessageLastUpdate <= ''2025-12-30 04:09:47''
    AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''NEXANS'')
    AND m.ProcessID IN (', @proz, ')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
PREPARE p_VERENGT_NEXANS_30d_prozess_gross FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VERENGT_NEXANS_30d_prozess_gross;
EXECUTE p_VERENGT_NEXANS_30d_prozess_gross;
EXECUTE p_VERENGT_NEXANS_30d_prozess_gross;
EXECUTE p_VERENGT_NEXANS_30d_prozess_gross;
EXECUTE p_VERENGT_NEXANS_30d_prozess_gross;
EXECUTE p_VERENGT_NEXANS_30d_prozess_gross;
SELECT 'VERENGT-NEXANS-30d-prozess-gross' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VERENGT_NEXANS_30d_prozess_gross;

SELECT '########## NEXANS - drei kleine Prozesse ##########' AS marke;
SET @proz = @proz_klein;

SELECT '=== F0-NEXANS-30d-prozess-klein - Plan (L15) ===' AS marke;
SET @s = CONCAT('EXPLAIN ', CONCAT('SELECT COUNT(*)                         AS zeilen,
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
) t'));
PREPARE p_F0_NEXANS_30d_prozess_klein_pl FROM @s;
EXECUTE p_F0_NEXANS_30d_prozess_klein_pl;
DEALLOCATE PREPARE p_F0_NEXANS_30d_prozess_klein_pl;
SELECT '=== F0-NEXANS-30d-prozess-klein - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = CONCAT('SELECT COUNT(*)                         AS zeilen,
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
PREPARE p_F0_NEXANS_30d_prozess_klein FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_F0_NEXANS_30d_prozess_klein;
EXECUTE p_F0_NEXANS_30d_prozess_klein;
EXECUTE p_F0_NEXANS_30d_prozess_klein;
EXECUTE p_F0_NEXANS_30d_prozess_klein;
EXECUTE p_F0_NEXANS_30d_prozess_klein;
EXECUTE p_F0_NEXANS_30d_prozess_klein;
SELECT 'F0-NEXANS-30d-prozess-klein' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_F0_NEXANS_30d_prozess_klein;

SELECT '=== VA-NEXANS-prozess-klein-1h - Plan (L15) ===' AS marke;
SET @s = CONCAT('EXPLAIN ', CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-12-30 03:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g'));
PREPARE p_VA_NEXANS_prozess_klein_1h_pl FROM @s;
EXECUTE p_VA_NEXANS_prozess_klein_1h_pl;
DEALLOCATE PREPARE p_VA_NEXANS_prozess_klein_1h_pl;
SELECT '=== VA-NEXANS-prozess-klein-1h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-12-30 03:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_VA_NEXANS_prozess_klein_1h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VA_NEXANS_prozess_klein_1h;
EXECUTE p_VA_NEXANS_prozess_klein_1h;
EXECUTE p_VA_NEXANS_prozess_klein_1h;
EXECUTE p_VA_NEXANS_prozess_klein_1h;
EXECUTE p_VA_NEXANS_prozess_klein_1h;
EXECUTE p_VA_NEXANS_prozess_klein_1h;
SELECT 'VA-NEXANS-prozess-klein-1h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VA_NEXANS_prozess_klein_1h;

SELECT '=== VA-NEXANS-prozess-klein-24h - Plan (L15) ===' AS marke;
SET @s = CONCAT('EXPLAIN ', CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-12-29 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g'));
PREPARE p_VA_NEXANS_prozess_klein_24h_pl FROM @s;
EXECUTE p_VA_NEXANS_prozess_klein_24h_pl;
DEALLOCATE PREPARE p_VA_NEXANS_prozess_klein_24h_pl;
SELECT '=== VA-NEXANS-prozess-klein-24h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-12-29 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_VA_NEXANS_prozess_klein_24h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VA_NEXANS_prozess_klein_24h;
EXECUTE p_VA_NEXANS_prozess_klein_24h;
EXECUTE p_VA_NEXANS_prozess_klein_24h;
EXECUTE p_VA_NEXANS_prozess_klein_24h;
EXECUTE p_VA_NEXANS_prozess_klein_24h;
EXECUTE p_VA_NEXANS_prozess_klein_24h;
SELECT 'VA-NEXANS-prozess-klein-24h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VA_NEXANS_prozess_klein_24h;

SELECT '=== VA-NEXANS-prozess-klein-7d - Plan (L15) ===' AS marke;
SET @s = CONCAT('EXPLAIN ', CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-12-23 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g'));
PREPARE p_VA_NEXANS_prozess_klein_7d_pl FROM @s;
EXECUTE p_VA_NEXANS_prozess_klein_7d_pl;
DEALLOCATE PREPARE p_VA_NEXANS_prozess_klein_7d_pl;
SELECT '=== VA-NEXANS-prozess-klein-7d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-12-23 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_VA_NEXANS_prozess_klein_7d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VA_NEXANS_prozess_klein_7d;
EXECUTE p_VA_NEXANS_prozess_klein_7d;
EXECUTE p_VA_NEXANS_prozess_klein_7d;
EXECUTE p_VA_NEXANS_prozess_klein_7d;
EXECUTE p_VA_NEXANS_prozess_klein_7d;
EXECUTE p_VA_NEXANS_prozess_klein_7d;
SELECT 'VA-NEXANS-prozess-klein-7d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VA_NEXANS_prozess_klein_7d;

SELECT '=== VA-NEXANS-prozess-klein-30d - Plan (L15) ===' AS marke;
SET @s = CONCAT('EXPLAIN ', CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g'));
PREPARE p_VA_NEXANS_prozess_klein_30d_pl FROM @s;
EXECUTE p_VA_NEXANS_prozess_klein_30d_pl;
DEALLOCATE PREPARE p_VA_NEXANS_prozess_klein_30d_pl;
SELECT '=== VA-NEXANS-prozess-klein-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_VA_NEXANS_prozess_klein_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VA_NEXANS_prozess_klein_30d;
EXECUTE p_VA_NEXANS_prozess_klein_30d;
EXECUTE p_VA_NEXANS_prozess_klein_30d;
EXECUTE p_VA_NEXANS_prozess_klein_30d;
EXECUTE p_VA_NEXANS_prozess_klein_30d;
EXECUTE p_VA_NEXANS_prozess_klein_30d;
SELECT 'VA-NEXANS-prozess-klein-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VA_NEXANS_prozess_klein_30d;

SELECT '=== NEXANS-prozess-klein - die Verengung ausgerechnet ===' AS marke;
SET @zeilen = NULL, @ug = NULL;
SET @s = CONCAT('SELECT SUM(g.anzahl), MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) INTO @zeilen, @ug
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_rechne FROM @s;
EXECUTE p_rechne;
DEALLOCATE PREPARE p_rechne;
SET @ug_wirksam = GREATEST('2025-11-30 04:09:47', COALESCE(@ug, '2025-11-30 04:09:47'));
SELECT 'NEXANS-prozess-klein' AS fall,
       @zeilen                                    AS zeilen_im_fenster,
       @ug                                        AS untergrenze_roh,
       @ug_wirksam                                AS untergrenze,
       TIMESTAMPDIFF(HOUR, @ug_wirksam, '2025-12-30 04:09:47')   AS fenster_stunden,
       (@zeilen IS NULL OR @zeilen = 0)           AS nullfall,
       (@ug IS NULL)                              AS keine_verengung;

SELECT '=== VERENGT-NEXANS-30d-prozess-klein - Plan (L15) ===' AS marke;
SET @s = CONCAT('EXPLAIN ', CONCAT('SELECT COUNT(*)                         AS zeilen,
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
  WHERE m.MessageLastUpdate >= ''', @ug_wirksam, '''
    AND m.MessageLastUpdate <= ''2025-12-30 04:09:47''
    AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''NEXANS'')
    AND m.ProcessID IN (', @proz, ')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t'));
PREPARE p_VERENGT_NEXANS_30d_prozess_klein_pl FROM @s;
EXECUTE p_VERENGT_NEXANS_30d_prozess_klein_pl;
DEALLOCATE PREPARE p_VERENGT_NEXANS_30d_prozess_klein_pl;
SELECT '=== VERENGT-NEXANS-30d-prozess-klein - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = CONCAT('SELECT COUNT(*)                         AS zeilen,
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
  WHERE m.MessageLastUpdate >= ''', @ug_wirksam, '''
    AND m.MessageLastUpdate <= ''2025-12-30 04:09:47''
    AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''NEXANS'')
    AND m.ProcessID IN (', @proz, ')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
PREPARE p_VERENGT_NEXANS_30d_prozess_klein FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VERENGT_NEXANS_30d_prozess_klein;
EXECUTE p_VERENGT_NEXANS_30d_prozess_klein;
EXECUTE p_VERENGT_NEXANS_30d_prozess_klein;
EXECUTE p_VERENGT_NEXANS_30d_prozess_klein;
EXECUTE p_VERENGT_NEXANS_30d_prozess_klein;
EXECUTE p_VERENGT_NEXANS_30d_prozess_klein;
SELECT 'VERENGT-NEXANS-30d-prozess-klein' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VERENGT_NEXANS_30d_prozess_klein;

SELECT '=== 99 Abschluss ===' AS marke;
SELECT @@global.read_only AS read_only_am_ende, NOW() AS serverzeit_ende;
