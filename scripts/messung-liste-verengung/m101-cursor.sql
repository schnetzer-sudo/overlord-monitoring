-- Messung M101 - Die Verengung unter dem Cursor - SUTTONS, NEXANS
-- Auftrag:  "Fensterverengung fuer die Nachrichtenliste", Stand 27.08.2026
-- Ergebnis: docs/messungen-liste-verengung.md
--
-- M101 - Der Cursor. Zwei aufeinanderfolgende Seiten fuer SUTTONS und NEXANS, jede mit
-- EIGENER Verengung: die zweite Seite verengt gegen den Cursor-Zeitpunkt, nicht gegen das
-- Fensterende. Zu belegen ist die RICHTIGKEIT, nicht die Laufzeit - die verengte Fassung
-- muss zeichengleich dieselben Zeilen in derselben Reihenfolge liefern wie die unverengte.
-- Verglichen wird ueber MD5 ueber die geordneten MessageID; die Kennungen selbst bleiben
-- damit aus der Ausgabe. (G1)
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

SELECT '=== SUTTONS-seite1 - die Verengung ausgerechnet ===' AS marke;
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
SELECT 'SUTTONS-seite1' AS fall,
       @zeilen                                    AS zeilen_im_fenster,
       @ug                                        AS untergrenze_roh,
       @ug_wirksam                                AS untergrenze,
       TIMESTAMPDIFF(HOUR, @ug_wirksam, '2025-12-30 04:09:47')   AS fenster_stunden,
       (@zeilen IS NULL OR @zeilen = 0)           AS nullfall,
       (@ug IS NULL)                              AS keine_verengung;

SELECT '=== SUTTONS - Seite 1: unverengt gegen verengt ===' AS marke;
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
SET @s = 'SELECT ''SUTTONS-seite1-unverengt'' AS fall, COUNT(*) AS zeilen,
       MD5(GROUP_CONCAT(t.MessageID ORDER BY t.MessageLastUpdate DESC, t.MessageID DESC SEPARATOR ''|'')) AS pruefsumme,
       MIN(t.MessageLastUpdate) AS aelteste,
       MAX(t.MessageLastUpdate) AS juengste
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
PREPARE p_ps FROM @s;
EXECUTE p_ps;
DEALLOCATE PREPARE p_ps;
SET @s = CONCAT('SELECT ''SUTTONS-seite1-verengt'' AS fall, COUNT(*) AS zeilen,
       MD5(GROUP_CONCAT(t.MessageID ORDER BY t.MessageLastUpdate DESC, t.MessageID DESC SEPARATOR ''|'')) AS pruefsumme,
       MIN(t.MessageLastUpdate) AS aelteste,
       MAX(t.MessageLastUpdate) AS juengste
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
PREPARE p_ps FROM @s;
EXECUTE p_ps;
DEALLOCATE PREPARE p_ps;
SELECT 'SUTTONS-seite1' AS fall, (@h_unv <=> @h_ver) AS gleiche_zeilen_gleiche_reihenfolge;
SELECT '=== VERENGT-SUTTONS-seite1 - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
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
PREPARE p_VERENGT_SUTTONS_seite1 FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VERENGT_SUTTONS_seite1;
EXECUTE p_VERENGT_SUTTONS_seite1;
EXECUTE p_VERENGT_SUTTONS_seite1;
EXECUTE p_VERENGT_SUTTONS_seite1;
EXECUTE p_VERENGT_SUTTONS_seite1;
EXECUTE p_VERENGT_SUTTONS_seite1;
SELECT 'VERENGT-SUTTONS-seite1' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VERENGT_SUTTONS_seite1;
SET @ug_seite1 = @ug_wirksam;

SELECT '=== SUTTONS - Cursor herleiten (50. Zeile der ersten Seite) ===' AS marke;
SELECT m.MessageLastUpdate, m.MessageID INTO @c_ts, @c_id
FROM GlassfishDB.Message m
WHERE m.MessageLastUpdate >= '2025-11-30 04:09:47'
  AND m.MessageLastUpdate <= '2025-12-30 04:09:47'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
              WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = 'SUTTONS')
ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
LIMIT 49, 1;
SELECT 'SUTTONS' AS mandant, @c_ts AS cursor_zeitpunkt, (@c_id IS NOT NULL) AS cursor_vorhanden;

SET @bis = @c_ts;
SET @h_all_bis  = DATE_FORMAT(@c_ts, '%Y-%m-%d %H:00:00');
SET @h_voll_bis = DATE_FORMAT(@c_ts, '%Y-%m-%d %H:00:00') - INTERVAL 1 HOUR;
SELECT 'SUTTONS' AS mandant, @bis AS bis_seite2, @h_all_bis AS letzte_ueberlappende_stunde,
       @h_voll_bis AS letzte_volle_stunde;

SELECT '=== SUTTONS-seite2 - die Verengung ausgerechnet ===' AS marke;
SET @zeilen = NULL, @ug = NULL;
SET @s = CONCAT('SELECT SUM(g.anzahl), MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) INTO @zeilen, @ug
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''', @h_voll_bis, ''') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''', @h_all_bis, '''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_rechne FROM @s;
EXECUTE p_rechne;
DEALLOCATE PREPARE p_rechne;
SET @ug_wirksam = GREATEST('2025-11-30 04:09:47', COALESCE(@ug, '2025-11-30 04:09:47'));
SELECT 'SUTTONS-seite2' AS fall,
       @zeilen                                    AS zeilen_im_fenster,
       @ug                                        AS untergrenze_roh,
       @ug_wirksam                                AS untergrenze,
       TIMESTAMPDIFF(HOUR, @ug_wirksam, @bis)   AS fenster_stunden,
       (@zeilen IS NULL OR @zeilen = 0)           AS nullfall,
       (@ug IS NULL)                              AS keine_verengung;

SELECT '=== SUTTONS - Seite 2: unverengt gegen verengt ===' AS marke;
SET @s = CONCAT('SELECT MD5(GROUP_CONCAT(t.MessageID ORDER BY t.MessageLastUpdate DESC, t.MessageID DESC SEPARATOR ''|''))
INTO @h_unv
FROM (
  SELECT m.MessageID, m.MessageLastUpdate
  FROM GlassfishDB.Message m
  WHERE m.MessageLastUpdate >= ''2025-11-30 04:09:47''
    AND m.MessageLastUpdate <= ''2025-12-30 04:09:47''
    AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''SUTTONS'')
    AND (m.MessageLastUpdate < ''', @c_ts, '''
         OR (m.MessageLastUpdate = ''', @c_ts, ''' AND m.MessageID < ''', @c_id, '''))
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
PREPARE p_psi FROM @s;
EXECUTE p_psi;
DEALLOCATE PREPARE p_psi;
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
    AND (m.MessageLastUpdate < ''', @c_ts, '''
         OR (m.MessageLastUpdate = ''', @c_ts, ''' AND m.MessageID < ''', @c_id, '''))
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
PREPARE p_psi FROM @s;
EXECUTE p_psi;
DEALLOCATE PREPARE p_psi;
SET @s = CONCAT('SELECT ''SUTTONS-seite2-unverengt'' AS fall, COUNT(*) AS zeilen,
       MD5(GROUP_CONCAT(t.MessageID ORDER BY t.MessageLastUpdate DESC, t.MessageID DESC SEPARATOR ''|'')) AS pruefsumme,
       MIN(t.MessageLastUpdate) AS aelteste,
       MAX(t.MessageLastUpdate) AS juengste
FROM (
  SELECT m.MessageID, m.MessageLastUpdate
  FROM GlassfishDB.Message m
  WHERE m.MessageLastUpdate >= ''2025-11-30 04:09:47''
    AND m.MessageLastUpdate <= ''2025-12-30 04:09:47''
    AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''SUTTONS'')
    AND (m.MessageLastUpdate < ''', @c_ts, '''
         OR (m.MessageLastUpdate = ''', @c_ts, ''' AND m.MessageID < ''', @c_id, '''))
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
PREPARE p_ps FROM @s;
EXECUTE p_ps;
DEALLOCATE PREPARE p_ps;
SET @s = CONCAT('SELECT ''SUTTONS-seite2-verengt'' AS fall, COUNT(*) AS zeilen,
       MD5(GROUP_CONCAT(t.MessageID ORDER BY t.MessageLastUpdate DESC, t.MessageID DESC SEPARATOR ''|'')) AS pruefsumme,
       MIN(t.MessageLastUpdate) AS aelteste,
       MAX(t.MessageLastUpdate) AS juengste
FROM (
  SELECT m.MessageID, m.MessageLastUpdate
  FROM GlassfishDB.Message m
  WHERE m.MessageLastUpdate >= ''', @ug_wirksam, '''
    AND m.MessageLastUpdate <= ''2025-12-30 04:09:47''
    AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''SUTTONS'')
    AND (m.MessageLastUpdate < ''', @c_ts, '''
         OR (m.MessageLastUpdate = ''', @c_ts, ''' AND m.MessageID < ''', @c_id, '''))
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
PREPARE p_ps FROM @s;
EXECUTE p_ps;
DEALLOCATE PREPARE p_ps;
SELECT 'SUTTONS-seite2' AS fall, (@h_unv <=> @h_ver) AS gleiche_zeilen_gleiche_reihenfolge;
SELECT '=== VERENGT-SUTTONS-seite2 - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
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
    AND (m.MessageLastUpdate < ''', @c_ts, '''
         OR (m.MessageLastUpdate = ''', @c_ts, ''' AND m.MessageID < ''', @c_id, '''))
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
PREPARE p_VERENGT_SUTTONS_seite2 FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VERENGT_SUTTONS_seite2;
EXECUTE p_VERENGT_SUTTONS_seite2;
EXECUTE p_VERENGT_SUTTONS_seite2;
EXECUTE p_VERENGT_SUTTONS_seite2;
EXECUTE p_VERENGT_SUTTONS_seite2;
EXECUTE p_VERENGT_SUTTONS_seite2;
SELECT 'VERENGT-SUTTONS-seite2' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VERENGT_SUTTONS_seite2;

SELECT '=== SUTTONS - beide Seiten zusammen ===' AS marke;
SET @s = CONCAT('SELECT ''SUTTONS-beide-seiten-verengt'' AS fall,
       COUNT(*)                    AS gelesene_zeilen,
       COUNT(DISTINCT u.MessageID) AS verschiedene_zeilen,
       COUNT(*) - COUNT(DISTINCT u.MessageID) AS ueberschneidung,
       MIN(u.MessageLastUpdate)    AS aelteste,
       MAX(u.MessageLastUpdate)    AS juengste
FROM ( (
  SELECT m.MessageID, m.MessageLastUpdate
  FROM GlassfishDB.Message m
  WHERE m.MessageLastUpdate >= ''', @ug_seite1, '''
    AND m.MessageLastUpdate <= ''2025-12-30 04:09:47''
    AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''SUTTONS'')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) UNION ALL (
  SELECT m.MessageID, m.MessageLastUpdate
  FROM GlassfishDB.Message m
  WHERE m.MessageLastUpdate >= ''', @ug_wirksam, '''
    AND m.MessageLastUpdate <= ''2025-12-30 04:09:47''
    AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''SUTTONS'')
    AND (m.MessageLastUpdate < ''', @c_ts, '''
         OR (m.MessageLastUpdate = ''', @c_ts, ''' AND m.MessageID < ''', @c_id, '''))
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) ) u');
PREPARE p_beide FROM @s;
EXECUTE p_beide;
DEALLOCATE PREPARE p_beide;
SELECT '=== SUTTONS - beide Seiten, unverengt (die Wahrheit) ===' AS marke;
SET @s = CONCAT('SELECT ''SUTTONS-beide-seiten-unverengt'' AS fall,
       COUNT(*)                    AS gelesene_zeilen,
       COUNT(DISTINCT u.MessageID) AS verschiedene_zeilen,
       COUNT(*) - COUNT(DISTINCT u.MessageID) AS ueberschneidung,
       MIN(u.MessageLastUpdate)    AS aelteste,
       MAX(u.MessageLastUpdate)    AS juengste
FROM ( (
  SELECT m.MessageID, m.MessageLastUpdate
  FROM GlassfishDB.Message m
  WHERE m.MessageLastUpdate >= ''2025-11-30 04:09:47''
    AND m.MessageLastUpdate <= ''2025-12-30 04:09:47''
    AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''SUTTONS'')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) UNION ALL (
  SELECT m.MessageID, m.MessageLastUpdate
  FROM GlassfishDB.Message m
  WHERE m.MessageLastUpdate >= ''2025-11-30 04:09:47''
    AND m.MessageLastUpdate <= ''2025-12-30 04:09:47''
    AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''SUTTONS'')
    AND (m.MessageLastUpdate < ''', @c_ts, '''
         OR (m.MessageLastUpdate = ''', @c_ts, ''' AND m.MessageID < ''', @c_id, '''))
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) ) u');
PREPARE p_beideu FROM @s;
EXECUTE p_beideu;
DEALLOCATE PREPARE p_beideu;

SELECT '########## NEXANS ##########' AS marke;
SELECT GROUP_CONCAT(QUOTE(ProcessID)) INTO @proz
FROM (SELECT DISTINCT p.ProcessID
      FROM GlassfishDB.Process p
      JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
      WHERE pm.MandantID = 'NEXANS') x;
SELECT 'NEXANS' AS mandant,
       (@proz IS NOT NULL) AS prozesse_vorhanden,
       LENGTH(@proz) - LENGTH(REPLACE(@proz, ',', '')) + 1 AS prozesse_anzahl;

SELECT '=== NEXANS-seite1 - die Verengung ausgerechnet ===' AS marke;
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
SELECT 'NEXANS-seite1' AS fall,
       @zeilen                                    AS zeilen_im_fenster,
       @ug                                        AS untergrenze_roh,
       @ug_wirksam                                AS untergrenze,
       TIMESTAMPDIFF(HOUR, @ug_wirksam, '2025-12-30 04:09:47')   AS fenster_stunden,
       (@zeilen IS NULL OR @zeilen = 0)           AS nullfall,
       (@ug IS NULL)                              AS keine_verengung;

SELECT '=== NEXANS - Seite 1: unverengt gegen verengt ===' AS marke;
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
SET @s = 'SELECT ''NEXANS-seite1-unverengt'' AS fall, COUNT(*) AS zeilen,
       MD5(GROUP_CONCAT(t.MessageID ORDER BY t.MessageLastUpdate DESC, t.MessageID DESC SEPARATOR ''|'')) AS pruefsumme,
       MIN(t.MessageLastUpdate) AS aelteste,
       MAX(t.MessageLastUpdate) AS juengste
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
PREPARE p_ps FROM @s;
EXECUTE p_ps;
DEALLOCATE PREPARE p_ps;
SET @s = CONCAT('SELECT ''NEXANS-seite1-verengt'' AS fall, COUNT(*) AS zeilen,
       MD5(GROUP_CONCAT(t.MessageID ORDER BY t.MessageLastUpdate DESC, t.MessageID DESC SEPARATOR ''|'')) AS pruefsumme,
       MIN(t.MessageLastUpdate) AS aelteste,
       MAX(t.MessageLastUpdate) AS juengste
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
PREPARE p_ps FROM @s;
EXECUTE p_ps;
DEALLOCATE PREPARE p_ps;
SELECT 'NEXANS-seite1' AS fall, (@h_unv <=> @h_ver) AS gleiche_zeilen_gleiche_reihenfolge;
SELECT '=== VERENGT-NEXANS-seite1 - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
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
PREPARE p_VERENGT_NEXANS_seite1 FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VERENGT_NEXANS_seite1;
EXECUTE p_VERENGT_NEXANS_seite1;
EXECUTE p_VERENGT_NEXANS_seite1;
EXECUTE p_VERENGT_NEXANS_seite1;
EXECUTE p_VERENGT_NEXANS_seite1;
EXECUTE p_VERENGT_NEXANS_seite1;
SELECT 'VERENGT-NEXANS-seite1' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VERENGT_NEXANS_seite1;
SET @ug_seite1 = @ug_wirksam;

SELECT '=== NEXANS - Cursor herleiten (50. Zeile der ersten Seite) ===' AS marke;
SELECT m.MessageLastUpdate, m.MessageID INTO @c_ts, @c_id
FROM GlassfishDB.Message m
WHERE m.MessageLastUpdate >= '2025-11-30 04:09:47'
  AND m.MessageLastUpdate <= '2025-12-30 04:09:47'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
              WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
LIMIT 49, 1;
SELECT 'NEXANS' AS mandant, @c_ts AS cursor_zeitpunkt, (@c_id IS NOT NULL) AS cursor_vorhanden;

SET @bis = @c_ts;
SET @h_all_bis  = DATE_FORMAT(@c_ts, '%Y-%m-%d %H:00:00');
SET @h_voll_bis = DATE_FORMAT(@c_ts, '%Y-%m-%d %H:00:00') - INTERVAL 1 HOUR;
SELECT 'NEXANS' AS mandant, @bis AS bis_seite2, @h_all_bis AS letzte_ueberlappende_stunde,
       @h_voll_bis AS letzte_volle_stunde;

SELECT '=== NEXANS-seite2 - die Verengung ausgerechnet ===' AS marke;
SET @zeilen = NULL, @ug = NULL;
SET @s = CONCAT('SELECT SUM(g.anzahl), MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) INTO @zeilen, @ug
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''', @h_voll_bis, ''') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''', @h_all_bis, '''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_rechne FROM @s;
EXECUTE p_rechne;
DEALLOCATE PREPARE p_rechne;
SET @ug_wirksam = GREATEST('2025-11-30 04:09:47', COALESCE(@ug, '2025-11-30 04:09:47'));
SELECT 'NEXANS-seite2' AS fall,
       @zeilen                                    AS zeilen_im_fenster,
       @ug                                        AS untergrenze_roh,
       @ug_wirksam                                AS untergrenze,
       TIMESTAMPDIFF(HOUR, @ug_wirksam, @bis)   AS fenster_stunden,
       (@zeilen IS NULL OR @zeilen = 0)           AS nullfall,
       (@ug IS NULL)                              AS keine_verengung;

SELECT '=== NEXANS - Seite 2: unverengt gegen verengt ===' AS marke;
SET @s = CONCAT('SELECT MD5(GROUP_CONCAT(t.MessageID ORDER BY t.MessageLastUpdate DESC, t.MessageID DESC SEPARATOR ''|''))
INTO @h_unv
FROM (
  SELECT m.MessageID, m.MessageLastUpdate
  FROM GlassfishDB.Message m
  WHERE m.MessageLastUpdate >= ''2025-11-30 04:09:47''
    AND m.MessageLastUpdate <= ''2025-12-30 04:09:47''
    AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''NEXANS'')
    AND (m.MessageLastUpdate < ''', @c_ts, '''
         OR (m.MessageLastUpdate = ''', @c_ts, ''' AND m.MessageID < ''', @c_id, '''))
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
PREPARE p_psi FROM @s;
EXECUTE p_psi;
DEALLOCATE PREPARE p_psi;
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
    AND (m.MessageLastUpdate < ''', @c_ts, '''
         OR (m.MessageLastUpdate = ''', @c_ts, ''' AND m.MessageID < ''', @c_id, '''))
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
PREPARE p_psi FROM @s;
EXECUTE p_psi;
DEALLOCATE PREPARE p_psi;
SET @s = CONCAT('SELECT ''NEXANS-seite2-unverengt'' AS fall, COUNT(*) AS zeilen,
       MD5(GROUP_CONCAT(t.MessageID ORDER BY t.MessageLastUpdate DESC, t.MessageID DESC SEPARATOR ''|'')) AS pruefsumme,
       MIN(t.MessageLastUpdate) AS aelteste,
       MAX(t.MessageLastUpdate) AS juengste
FROM (
  SELECT m.MessageID, m.MessageLastUpdate
  FROM GlassfishDB.Message m
  WHERE m.MessageLastUpdate >= ''2025-11-30 04:09:47''
    AND m.MessageLastUpdate <= ''2025-12-30 04:09:47''
    AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''NEXANS'')
    AND (m.MessageLastUpdate < ''', @c_ts, '''
         OR (m.MessageLastUpdate = ''', @c_ts, ''' AND m.MessageID < ''', @c_id, '''))
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
PREPARE p_ps FROM @s;
EXECUTE p_ps;
DEALLOCATE PREPARE p_ps;
SET @s = CONCAT('SELECT ''NEXANS-seite2-verengt'' AS fall, COUNT(*) AS zeilen,
       MD5(GROUP_CONCAT(t.MessageID ORDER BY t.MessageLastUpdate DESC, t.MessageID DESC SEPARATOR ''|'')) AS pruefsumme,
       MIN(t.MessageLastUpdate) AS aelteste,
       MAX(t.MessageLastUpdate) AS juengste
FROM (
  SELECT m.MessageID, m.MessageLastUpdate
  FROM GlassfishDB.Message m
  WHERE m.MessageLastUpdate >= ''', @ug_wirksam, '''
    AND m.MessageLastUpdate <= ''2025-12-30 04:09:47''
    AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''NEXANS'')
    AND (m.MessageLastUpdate < ''', @c_ts, '''
         OR (m.MessageLastUpdate = ''', @c_ts, ''' AND m.MessageID < ''', @c_id, '''))
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
PREPARE p_ps FROM @s;
EXECUTE p_ps;
DEALLOCATE PREPARE p_ps;
SELECT 'NEXANS-seite2' AS fall, (@h_unv <=> @h_ver) AS gleiche_zeilen_gleiche_reihenfolge;
SELECT '=== VERENGT-NEXANS-seite2 - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
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
    AND (m.MessageLastUpdate < ''', @c_ts, '''
         OR (m.MessageLastUpdate = ''', @c_ts, ''' AND m.MessageID < ''', @c_id, '''))
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
PREPARE p_VERENGT_NEXANS_seite2 FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VERENGT_NEXANS_seite2;
EXECUTE p_VERENGT_NEXANS_seite2;
EXECUTE p_VERENGT_NEXANS_seite2;
EXECUTE p_VERENGT_NEXANS_seite2;
EXECUTE p_VERENGT_NEXANS_seite2;
EXECUTE p_VERENGT_NEXANS_seite2;
SELECT 'VERENGT-NEXANS-seite2' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VERENGT_NEXANS_seite2;

SELECT '=== NEXANS - beide Seiten zusammen ===' AS marke;
SET @s = CONCAT('SELECT ''NEXANS-beide-seiten-verengt'' AS fall,
       COUNT(*)                    AS gelesene_zeilen,
       COUNT(DISTINCT u.MessageID) AS verschiedene_zeilen,
       COUNT(*) - COUNT(DISTINCT u.MessageID) AS ueberschneidung,
       MIN(u.MessageLastUpdate)    AS aelteste,
       MAX(u.MessageLastUpdate)    AS juengste
FROM ( (
  SELECT m.MessageID, m.MessageLastUpdate
  FROM GlassfishDB.Message m
  WHERE m.MessageLastUpdate >= ''', @ug_seite1, '''
    AND m.MessageLastUpdate <= ''2025-12-30 04:09:47''
    AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''NEXANS'')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) UNION ALL (
  SELECT m.MessageID, m.MessageLastUpdate
  FROM GlassfishDB.Message m
  WHERE m.MessageLastUpdate >= ''', @ug_wirksam, '''
    AND m.MessageLastUpdate <= ''2025-12-30 04:09:47''
    AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''NEXANS'')
    AND (m.MessageLastUpdate < ''', @c_ts, '''
         OR (m.MessageLastUpdate = ''', @c_ts, ''' AND m.MessageID < ''', @c_id, '''))
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) ) u');
PREPARE p_beide FROM @s;
EXECUTE p_beide;
DEALLOCATE PREPARE p_beide;
SELECT '=== NEXANS - beide Seiten, unverengt (die Wahrheit) ===' AS marke;
SET @s = CONCAT('SELECT ''NEXANS-beide-seiten-unverengt'' AS fall,
       COUNT(*)                    AS gelesene_zeilen,
       COUNT(DISTINCT u.MessageID) AS verschiedene_zeilen,
       COUNT(*) - COUNT(DISTINCT u.MessageID) AS ueberschneidung,
       MIN(u.MessageLastUpdate)    AS aelteste,
       MAX(u.MessageLastUpdate)    AS juengste
FROM ( (
  SELECT m.MessageID, m.MessageLastUpdate
  FROM GlassfishDB.Message m
  WHERE m.MessageLastUpdate >= ''2025-11-30 04:09:47''
    AND m.MessageLastUpdate <= ''2025-12-30 04:09:47''
    AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''NEXANS'')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) UNION ALL (
  SELECT m.MessageID, m.MessageLastUpdate
  FROM GlassfishDB.Message m
  WHERE m.MessageLastUpdate >= ''2025-11-30 04:09:47''
    AND m.MessageLastUpdate <= ''2025-12-30 04:09:47''
    AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''NEXANS'')
    AND (m.MessageLastUpdate < ''', @c_ts, '''
         OR (m.MessageLastUpdate = ''', @c_ts, ''' AND m.MessageID < ''', @c_id, '''))
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) ) u');
PREPARE p_beideu FROM @s;
EXECUTE p_beideu;
DEALLOCATE PREPARE p_beideu;

SELECT '=== 99 Abschluss ===' AS marke;
SELECT @@global.read_only AS read_only_am_ende, NOW() AS serverzeit_ende;
