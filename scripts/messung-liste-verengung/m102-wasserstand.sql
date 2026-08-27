-- Messung M102 - Die Wasserstandsgrenze - SUTTONS, NEXANS
-- Auftrag:  "Fensterverengung fuer die Nachrichtenliste", Stand 27.08.2026
-- Ergebnis: docs/messungen-liste-verengung.md
--
-- M102 - Die Wasserstandsgrenze. Der Rollup weiss nur bis MAX(fenster_bis) aus rollup_lauf
-- Bescheid; oberhalb wird UNVERENGT gefragt. Umgesetzt ist das nicht als zweite Abfrage,
-- sondern als Null-Wertung: Stunden ab dem Wasserstand gehen mit 0 in die kumulierte Summe
-- ein. Damit bleibt die gezaehlte Menge eine Unterschranke - die Verengung kann nie ueber
-- den Wasserstand hinausgehen und nie eine Zeile verlieren.
--
-- Der wirkliche Wasserstand der Testkopie liegt bei 2026-08-27 15:00:00 und damit weit ueber
-- dem Anker; das ganze Fenster liegt darunter. Zurueckgesetzt wird er deshalb NICHT in
-- rollup_lauf - dort wird nicht geschrieben -, sondern als Literal in der Vorabfrage.
-- Zwei Faelle: W mitten im Fenster, und W unterhalb von 'von' - dann ist NICHTS bekannt
-- und die Verengung muss vollstaendig entfallen.
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

SELECT '########## SUTTONS ##########' AS marke;
SELECT GROUP_CONCAT(QUOTE(ProcessID)) INTO @proz
FROM (SELECT DISTINCT p.ProcessID
      FROM GlassfishDB.Process p
      JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
      WHERE pm.MandantID = 'SUTTONS') x;
SELECT 'SUTTONS' AS mandant,
       (@proz IS NOT NULL) AS prozesse_vorhanden,
       LENGTH(@proz) - LENGTH(REPLACE(@proz, ',', '')) + 1 AS prozesse_anzahl;

SELECT '=== SUTTONS - unverengt, die Wahrheit ===' AS marke;
SET @s = 'SELECT MD5(GROUP_CONCAT(t.MessageID ORDER BY t.MessageLastUpdate DESC, t.MessageID DESC SEPARATOR ''|''))
INTO @h_unv
FROM (
  SELECT m.MessageID, m.MessageLastUpdate
  FROM GlassfishDB.Message m
  WHERE m.MessageLastUpdate >= ''2025-11-30 04:09:47''
    AND m.MessageLastUpdate <= ''2025-12-30 04:09:47''
    AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''SUTTONS'')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t';
PREPARE p_psi FROM @s;
EXECUTE p_psi;
DEALLOCATE PREPARE p_psi;
SELECT 'SUTTONS-unverengt' AS fall, @h_unv AS pruefsumme;

SELECT '########## SUTTONS - W-mitten-im-fenster (W = 2025-12-20 00:00:00) ##########' AS marke;

SELECT '=== SUTTONS-W-mitten-im-fenster - die Verengung ausgerechnet ===' AS marke;
SET @zeilen = NULL, @ug = NULL;
SET @s = CONCAT('SELECT SUM(g.anzahl), MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) INTO @zeilen, @ug
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'' AND r.stunde < ''2025-12-20 00:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_rechne FROM @s;
EXECUTE p_rechne;
DEALLOCATE PREPARE p_rechne;
SET @ug_wirksam = GREATEST('2025-11-30 04:09:47', COALESCE(@ug, '2025-11-30 04:09:47'));
SELECT 'SUTTONS-W-mitten-im-fenster' AS fall,
       @zeilen                                    AS zeilen_im_fenster,
       @ug                                        AS untergrenze_roh,
       @ug_wirksam                                AS untergrenze,
       TIMESTAMPDIFF(HOUR, @ug_wirksam, '2025-12-30 04:09:47')   AS fenster_stunden,
       (@zeilen IS NULL OR @zeilen = 0)           AS nullfall,
       (@ug IS NULL)                              AS keine_verengung;
SELECT 'SUTTONS-W-mitten-im-fenster' AS fall,
       '2025-12-20 00:00:00' AS wasserstand,
       (@ug IS NULL) AS verengung_entfaellt,
       (@ug_wirksam < '2025-12-20 00:00:00' OR @ug IS NULL) AS nie_ueber_den_wasserstand,
       ('2025-12-30 04:09:47' < '2025-12-20 00:00:00') AS nullfall_aussagbar;
SET @s = CONCAT('SELECT MD5(GROUP_CONCAT(t.MessageID ORDER BY t.MessageLastUpdate DESC, t.MessageID DESC SEPARATOR ''|''))
INTO @h_ver
FROM (
  SELECT m.MessageID, m.MessageLastUpdate
  FROM GlassfishDB.Message m
  WHERE m.MessageLastUpdate >= ''', @ug_wirksam, '''
    AND m.MessageLastUpdate <= ''2025-12-30 04:09:47''
    AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''SUTTONS'')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
PREPARE p_psi FROM @s;
EXECUTE p_psi;
DEALLOCATE PREPARE p_psi;
SELECT 'SUTTONS-W-mitten-im-fenster' AS fall, @h_unv AS unverengt, @h_ver AS verengt,
       (@h_unv <=> @h_ver) AS gleiche_zeilen;
SELECT '=== W-SUTTONS-W-mitten-im-fenster - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
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
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''SUTTONS'')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
PREPARE p_W_SUTTONS_W_mitten_im_fenster FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_W_SUTTONS_W_mitten_im_fenster;
EXECUTE p_W_SUTTONS_W_mitten_im_fenster;
EXECUTE p_W_SUTTONS_W_mitten_im_fenster;
EXECUTE p_W_SUTTONS_W_mitten_im_fenster;
EXECUTE p_W_SUTTONS_W_mitten_im_fenster;
EXECUTE p_W_SUTTONS_W_mitten_im_fenster;
SELECT 'W-SUTTONS-W-mitten-im-fenster' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_W_SUTTONS_W_mitten_im_fenster;

SELECT '########## SUTTONS - W-unter-dem-fenster (W = 2025-11-01 00:00:00) ##########' AS marke;

SELECT '=== SUTTONS-W-unter-dem-fenster - die Verengung ausgerechnet ===' AS marke;
SET @zeilen = NULL, @ug = NULL;
SET @s = CONCAT('SELECT SUM(g.anzahl), MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) INTO @zeilen, @ug
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'' AND r.stunde < ''2025-11-01 00:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_rechne FROM @s;
EXECUTE p_rechne;
DEALLOCATE PREPARE p_rechne;
SET @ug_wirksam = GREATEST('2025-11-30 04:09:47', COALESCE(@ug, '2025-11-30 04:09:47'));
SELECT 'SUTTONS-W-unter-dem-fenster' AS fall,
       @zeilen                                    AS zeilen_im_fenster,
       @ug                                        AS untergrenze_roh,
       @ug_wirksam                                AS untergrenze,
       TIMESTAMPDIFF(HOUR, @ug_wirksam, '2025-12-30 04:09:47')   AS fenster_stunden,
       (@zeilen IS NULL OR @zeilen = 0)           AS nullfall,
       (@ug IS NULL)                              AS keine_verengung;
SELECT 'SUTTONS-W-unter-dem-fenster' AS fall,
       '2025-11-01 00:00:00' AS wasserstand,
       (@ug IS NULL) AS verengung_entfaellt,
       (@ug_wirksam < '2025-11-01 00:00:00' OR @ug IS NULL) AS nie_ueber_den_wasserstand,
       ('2025-12-30 04:09:47' < '2025-11-01 00:00:00') AS nullfall_aussagbar;
SET @s = CONCAT('SELECT MD5(GROUP_CONCAT(t.MessageID ORDER BY t.MessageLastUpdate DESC, t.MessageID DESC SEPARATOR ''|''))
INTO @h_ver
FROM (
  SELECT m.MessageID, m.MessageLastUpdate
  FROM GlassfishDB.Message m
  WHERE m.MessageLastUpdate >= ''', @ug_wirksam, '''
    AND m.MessageLastUpdate <= ''2025-12-30 04:09:47''
    AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''SUTTONS'')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
PREPARE p_psi FROM @s;
EXECUTE p_psi;
DEALLOCATE PREPARE p_psi;
SELECT 'SUTTONS-W-unter-dem-fenster' AS fall, @h_unv AS unverengt, @h_ver AS verengt,
       (@h_unv <=> @h_ver) AS gleiche_zeilen;
SELECT '=== W-SUTTONS-W-unter-dem-fenster - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
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
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''SUTTONS'')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
PREPARE p_W_SUTTONS_W_unter_dem_fenster FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_W_SUTTONS_W_unter_dem_fenster;
EXECUTE p_W_SUTTONS_W_unter_dem_fenster;
EXECUTE p_W_SUTTONS_W_unter_dem_fenster;
EXECUTE p_W_SUTTONS_W_unter_dem_fenster;
EXECUTE p_W_SUTTONS_W_unter_dem_fenster;
EXECUTE p_W_SUTTONS_W_unter_dem_fenster;
SELECT 'W-SUTTONS-W-unter-dem-fenster' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_W_SUTTONS_W_unter_dem_fenster;

SELECT '########## SUTTONS - W-ueber-dem-fenster (W = 2026-08-27 15:00:00) ##########' AS marke;

SELECT '=== SUTTONS-W-ueber-dem-fenster - die Verengung ausgerechnet ===' AS marke;
SET @zeilen = NULL, @ug = NULL;
SET @s = CONCAT('SELECT SUM(g.anzahl), MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) INTO @zeilen, @ug
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'' AND r.stunde < ''2026-08-27 15:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_rechne FROM @s;
EXECUTE p_rechne;
DEALLOCATE PREPARE p_rechne;
SET @ug_wirksam = GREATEST('2025-11-30 04:09:47', COALESCE(@ug, '2025-11-30 04:09:47'));
SELECT 'SUTTONS-W-ueber-dem-fenster' AS fall,
       @zeilen                                    AS zeilen_im_fenster,
       @ug                                        AS untergrenze_roh,
       @ug_wirksam                                AS untergrenze,
       TIMESTAMPDIFF(HOUR, @ug_wirksam, '2025-12-30 04:09:47')   AS fenster_stunden,
       (@zeilen IS NULL OR @zeilen = 0)           AS nullfall,
       (@ug IS NULL)                              AS keine_verengung;
SELECT 'SUTTONS-W-ueber-dem-fenster' AS fall,
       '2026-08-27 15:00:00' AS wasserstand,
       (@ug IS NULL) AS verengung_entfaellt,
       (@ug_wirksam < '2026-08-27 15:00:00' OR @ug IS NULL) AS nie_ueber_den_wasserstand,
       ('2025-12-30 04:09:47' < '2026-08-27 15:00:00') AS nullfall_aussagbar;
SET @s = CONCAT('SELECT MD5(GROUP_CONCAT(t.MessageID ORDER BY t.MessageLastUpdate DESC, t.MessageID DESC SEPARATOR ''|''))
INTO @h_ver
FROM (
  SELECT m.MessageID, m.MessageLastUpdate
  FROM GlassfishDB.Message m
  WHERE m.MessageLastUpdate >= ''', @ug_wirksam, '''
    AND m.MessageLastUpdate <= ''2025-12-30 04:09:47''
    AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''SUTTONS'')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
PREPARE p_psi FROM @s;
EXECUTE p_psi;
DEALLOCATE PREPARE p_psi;
SELECT 'SUTTONS-W-ueber-dem-fenster' AS fall, @h_unv AS unverengt, @h_ver AS verengt,
       (@h_unv <=> @h_ver) AS gleiche_zeilen;
SELECT '=== W-SUTTONS-W-ueber-dem-fenster - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
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
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''SUTTONS'')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
PREPARE p_W_SUTTONS_W_ueber_dem_fenster FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_W_SUTTONS_W_ueber_dem_fenster;
EXECUTE p_W_SUTTONS_W_ueber_dem_fenster;
EXECUTE p_W_SUTTONS_W_ueber_dem_fenster;
EXECUTE p_W_SUTTONS_W_ueber_dem_fenster;
EXECUTE p_W_SUTTONS_W_ueber_dem_fenster;
EXECUTE p_W_SUTTONS_W_ueber_dem_fenster;
SELECT 'W-SUTTONS-W-ueber-dem-fenster' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_W_SUTTONS_W_ueber_dem_fenster;

SELECT '########## NEXANS ##########' AS marke;
SELECT GROUP_CONCAT(QUOTE(ProcessID)) INTO @proz
FROM (SELECT DISTINCT p.ProcessID
      FROM GlassfishDB.Process p
      JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
      WHERE pm.MandantID = 'NEXANS') x;
SELECT 'NEXANS' AS mandant,
       (@proz IS NOT NULL) AS prozesse_vorhanden,
       LENGTH(@proz) - LENGTH(REPLACE(@proz, ',', '')) + 1 AS prozesse_anzahl;

SELECT '=== NEXANS - unverengt, die Wahrheit ===' AS marke;
SET @s = 'SELECT MD5(GROUP_CONCAT(t.MessageID ORDER BY t.MessageLastUpdate DESC, t.MessageID DESC SEPARATOR ''|''))
INTO @h_unv
FROM (
  SELECT m.MessageID, m.MessageLastUpdate
  FROM GlassfishDB.Message m
  WHERE m.MessageLastUpdate >= ''2025-11-30 04:09:47''
    AND m.MessageLastUpdate <= ''2025-12-30 04:09:47''
    AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''NEXANS'')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t';
PREPARE p_psi FROM @s;
EXECUTE p_psi;
DEALLOCATE PREPARE p_psi;
SELECT 'NEXANS-unverengt' AS fall, @h_unv AS pruefsumme;

SELECT '########## NEXANS - W-mitten-im-fenster (W = 2025-12-20 00:00:00) ##########' AS marke;

SELECT '=== NEXANS-W-mitten-im-fenster - die Verengung ausgerechnet ===' AS marke;
SET @zeilen = NULL, @ug = NULL;
SET @s = CONCAT('SELECT SUM(g.anzahl), MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) INTO @zeilen, @ug
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'' AND r.stunde < ''2025-12-20 00:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_rechne FROM @s;
EXECUTE p_rechne;
DEALLOCATE PREPARE p_rechne;
SET @ug_wirksam = GREATEST('2025-11-30 04:09:47', COALESCE(@ug, '2025-11-30 04:09:47'));
SELECT 'NEXANS-W-mitten-im-fenster' AS fall,
       @zeilen                                    AS zeilen_im_fenster,
       @ug                                        AS untergrenze_roh,
       @ug_wirksam                                AS untergrenze,
       TIMESTAMPDIFF(HOUR, @ug_wirksam, '2025-12-30 04:09:47')   AS fenster_stunden,
       (@zeilen IS NULL OR @zeilen = 0)           AS nullfall,
       (@ug IS NULL)                              AS keine_verengung;
SELECT 'NEXANS-W-mitten-im-fenster' AS fall,
       '2025-12-20 00:00:00' AS wasserstand,
       (@ug IS NULL) AS verengung_entfaellt,
       (@ug_wirksam < '2025-12-20 00:00:00' OR @ug IS NULL) AS nie_ueber_den_wasserstand,
       ('2025-12-30 04:09:47' < '2025-12-20 00:00:00') AS nullfall_aussagbar;
SET @s = CONCAT('SELECT MD5(GROUP_CONCAT(t.MessageID ORDER BY t.MessageLastUpdate DESC, t.MessageID DESC SEPARATOR ''|''))
INTO @h_ver
FROM (
  SELECT m.MessageID, m.MessageLastUpdate
  FROM GlassfishDB.Message m
  WHERE m.MessageLastUpdate >= ''', @ug_wirksam, '''
    AND m.MessageLastUpdate <= ''2025-12-30 04:09:47''
    AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''NEXANS'')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
PREPARE p_psi FROM @s;
EXECUTE p_psi;
DEALLOCATE PREPARE p_psi;
SELECT 'NEXANS-W-mitten-im-fenster' AS fall, @h_unv AS unverengt, @h_ver AS verengt,
       (@h_unv <=> @h_ver) AS gleiche_zeilen;
SELECT '=== W-NEXANS-W-mitten-im-fenster - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
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
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
PREPARE p_W_NEXANS_W_mitten_im_fenster FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_W_NEXANS_W_mitten_im_fenster;
EXECUTE p_W_NEXANS_W_mitten_im_fenster;
EXECUTE p_W_NEXANS_W_mitten_im_fenster;
EXECUTE p_W_NEXANS_W_mitten_im_fenster;
EXECUTE p_W_NEXANS_W_mitten_im_fenster;
EXECUTE p_W_NEXANS_W_mitten_im_fenster;
SELECT 'W-NEXANS-W-mitten-im-fenster' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_W_NEXANS_W_mitten_im_fenster;

SELECT '########## NEXANS - W-unter-dem-fenster (W = 2025-11-01 00:00:00) ##########' AS marke;

SELECT '=== NEXANS-W-unter-dem-fenster - die Verengung ausgerechnet ===' AS marke;
SET @zeilen = NULL, @ug = NULL;
SET @s = CONCAT('SELECT SUM(g.anzahl), MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) INTO @zeilen, @ug
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'' AND r.stunde < ''2025-11-01 00:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_rechne FROM @s;
EXECUTE p_rechne;
DEALLOCATE PREPARE p_rechne;
SET @ug_wirksam = GREATEST('2025-11-30 04:09:47', COALESCE(@ug, '2025-11-30 04:09:47'));
SELECT 'NEXANS-W-unter-dem-fenster' AS fall,
       @zeilen                                    AS zeilen_im_fenster,
       @ug                                        AS untergrenze_roh,
       @ug_wirksam                                AS untergrenze,
       TIMESTAMPDIFF(HOUR, @ug_wirksam, '2025-12-30 04:09:47')   AS fenster_stunden,
       (@zeilen IS NULL OR @zeilen = 0)           AS nullfall,
       (@ug IS NULL)                              AS keine_verengung;
SELECT 'NEXANS-W-unter-dem-fenster' AS fall,
       '2025-11-01 00:00:00' AS wasserstand,
       (@ug IS NULL) AS verengung_entfaellt,
       (@ug_wirksam < '2025-11-01 00:00:00' OR @ug IS NULL) AS nie_ueber_den_wasserstand,
       ('2025-12-30 04:09:47' < '2025-11-01 00:00:00') AS nullfall_aussagbar;
SET @s = CONCAT('SELECT MD5(GROUP_CONCAT(t.MessageID ORDER BY t.MessageLastUpdate DESC, t.MessageID DESC SEPARATOR ''|''))
INTO @h_ver
FROM (
  SELECT m.MessageID, m.MessageLastUpdate
  FROM GlassfishDB.Message m
  WHERE m.MessageLastUpdate >= ''', @ug_wirksam, '''
    AND m.MessageLastUpdate <= ''2025-12-30 04:09:47''
    AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''NEXANS'')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
PREPARE p_psi FROM @s;
EXECUTE p_psi;
DEALLOCATE PREPARE p_psi;
SELECT 'NEXANS-W-unter-dem-fenster' AS fall, @h_unv AS unverengt, @h_ver AS verengt,
       (@h_unv <=> @h_ver) AS gleiche_zeilen;
SELECT '=== W-NEXANS-W-unter-dem-fenster - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
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
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
PREPARE p_W_NEXANS_W_unter_dem_fenster FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_W_NEXANS_W_unter_dem_fenster;
EXECUTE p_W_NEXANS_W_unter_dem_fenster;
EXECUTE p_W_NEXANS_W_unter_dem_fenster;
EXECUTE p_W_NEXANS_W_unter_dem_fenster;
EXECUTE p_W_NEXANS_W_unter_dem_fenster;
EXECUTE p_W_NEXANS_W_unter_dem_fenster;
SELECT 'W-NEXANS-W-unter-dem-fenster' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_W_NEXANS_W_unter_dem_fenster;

SELECT '########## NEXANS - W-ueber-dem-fenster (W = 2026-08-27 15:00:00) ##########' AS marke;

SELECT '=== NEXANS-W-ueber-dem-fenster - die Verengung ausgerechnet ===' AS marke;
SET @zeilen = NULL, @ug = NULL;
SET @s = CONCAT('SELECT SUM(g.anzahl), MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) INTO @zeilen, @ug
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'' AND r.stunde < ''2026-08-27 15:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_rechne FROM @s;
EXECUTE p_rechne;
DEALLOCATE PREPARE p_rechne;
SET @ug_wirksam = GREATEST('2025-11-30 04:09:47', COALESCE(@ug, '2025-11-30 04:09:47'));
SELECT 'NEXANS-W-ueber-dem-fenster' AS fall,
       @zeilen                                    AS zeilen_im_fenster,
       @ug                                        AS untergrenze_roh,
       @ug_wirksam                                AS untergrenze,
       TIMESTAMPDIFF(HOUR, @ug_wirksam, '2025-12-30 04:09:47')   AS fenster_stunden,
       (@zeilen IS NULL OR @zeilen = 0)           AS nullfall,
       (@ug IS NULL)                              AS keine_verengung;
SELECT 'NEXANS-W-ueber-dem-fenster' AS fall,
       '2026-08-27 15:00:00' AS wasserstand,
       (@ug IS NULL) AS verengung_entfaellt,
       (@ug_wirksam < '2026-08-27 15:00:00' OR @ug IS NULL) AS nie_ueber_den_wasserstand,
       ('2025-12-30 04:09:47' < '2026-08-27 15:00:00') AS nullfall_aussagbar;
SET @s = CONCAT('SELECT MD5(GROUP_CONCAT(t.MessageID ORDER BY t.MessageLastUpdate DESC, t.MessageID DESC SEPARATOR ''|''))
INTO @h_ver
FROM (
  SELECT m.MessageID, m.MessageLastUpdate
  FROM GlassfishDB.Message m
  WHERE m.MessageLastUpdate >= ''', @ug_wirksam, '''
    AND m.MessageLastUpdate <= ''2025-12-30 04:09:47''
    AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''NEXANS'')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
PREPARE p_psi FROM @s;
EXECUTE p_psi;
DEALLOCATE PREPARE p_psi;
SELECT 'NEXANS-W-ueber-dem-fenster' AS fall, @h_unv AS unverengt, @h_ver AS verengt,
       (@h_unv <=> @h_ver) AS gleiche_zeilen;
SELECT '=== W-NEXANS-W-ueber-dem-fenster - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
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
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
PREPARE p_W_NEXANS_W_ueber_dem_fenster FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_W_NEXANS_W_ueber_dem_fenster;
EXECUTE p_W_NEXANS_W_ueber_dem_fenster;
EXECUTE p_W_NEXANS_W_ueber_dem_fenster;
EXECUTE p_W_NEXANS_W_ueber_dem_fenster;
EXECUTE p_W_NEXANS_W_ueber_dem_fenster;
EXECUTE p_W_NEXANS_W_ueber_dem_fenster;
SELECT 'W-NEXANS-W-ueber-dem-fenster' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_W_NEXANS_W_ueber_dem_fenster;

SELECT '=== 99 Abschluss ===' AS marke;
SELECT @@global.read_only AS read_only_am_ende, NOW() AS serverzeit_ende;
