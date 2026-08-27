-- Messung M103 - Rueckfallpfade und der Statusfilter - NEXANS, SUTTONS
-- Auftrag:  "Fensterverengung fuer die Nachrichtenliste", Stand 27.08.2026
-- Ergebnis: docs/messungen-liste-verengung.md
--
-- M103 - Wo die Verengung nicht greift, und wo der Rollup mitreden kann.
--
-- ueberfaellig haengt an MessageTimeout und an 'jetzt'. Beides steht nicht im Rollup, und
-- es laesst sich auch nicht daraus herleiten - die Verengung entfaellt dort vollstaendig.
-- Zu belegen ist, dass der Rueckfallpfad die heutigen Werte trifft und nicht schlechter ist.
--
-- Der STATUSFILTER dagegen steht im Rollup: message_status traegt den Rohwert (E-g), und
-- MessageStatusClassifier.fehlerBedingung nimmt ein beliebiges Feld - also auch das des
-- Rollups. Gemessen wird verengt MIT Status im Rollup gegen verengt OHNE. Mit Status ist die
-- Verengung schwaecher (die Untergrenze rutscht weiter zurueck), aber die Quellabfrage muss
-- weniger Zeilen verwerfen. Was ueberwiegt, entscheidet die Messung.
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

SELECT '########## NEXANS ##########' AS marke;
SELECT GROUP_CONCAT(QUOTE(ProcessID)) INTO @proz
FROM (SELECT DISTINCT p.ProcessID
      FROM GlassfishDB.Process p
      JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
      WHERE pm.MandantID = 'NEXANS') x;
SELECT 'NEXANS' AS mandant,
       (@proz IS NOT NULL) AS prozesse_vorhanden,
       LENGTH(@proz) - LENGTH(REPLACE(@proz, ',', '')) + 1 AS prozesse_anzahl;

SELECT '########## NEXANS - ueberfaellig (Rueckfallpfad) ##########' AS marke;
SELECT 'NEXANS' AS mandant,
       'die Verengung wird hier gar nicht erst betreten' AS vermerk,
       'MessageTimeout und jetzt stehen nicht im Rollup' AS grund;

SELECT '=== RUECKFALL-NEXANS-30d-ueberfaellig - Plan (L15) ===' AS marke;
SET @s = CONCAT('EXPLAIN ', 'SELECT COUNT(*)                         AS zeilen,
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
    AND m.MessageStatus IN (''SUSPENDED'',''RUNNING'')
    AND m.MessageTimeout IS NOT NULL
    AND m.MessageTimeout > 0
    AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND < ''2025-12-30 04:09:47''
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
PREPARE p_RUECKFALL_NEXANS_30d_ueberfaellig_pl FROM @s;
EXECUTE p_RUECKFALL_NEXANS_30d_ueberfaellig_pl;
DEALLOCATE PREPARE p_RUECKFALL_NEXANS_30d_ueberfaellig_pl;
SELECT '=== RUECKFALL-NEXANS-30d-ueberfaellig - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT COUNT(*)                         AS zeilen,
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
    AND m.MessageStatus IN (''SUSPENDED'',''RUNNING'')
    AND m.MessageTimeout IS NOT NULL
    AND m.MessageTimeout > 0
    AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND < ''2025-12-30 04:09:47''
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t';
PREPARE p_RUECKFALL_NEXANS_30d_ueberfaellig FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_RUECKFALL_NEXANS_30d_ueberfaellig;
EXECUTE p_RUECKFALL_NEXANS_30d_ueberfaellig;
EXECUTE p_RUECKFALL_NEXANS_30d_ueberfaellig;
EXECUTE p_RUECKFALL_NEXANS_30d_ueberfaellig;
EXECUTE p_RUECKFALL_NEXANS_30d_ueberfaellig;
EXECUTE p_RUECKFALL_NEXANS_30d_ueberfaellig;
SELECT 'RUECKFALL-NEXANS-30d-ueberfaellig' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_RUECKFALL_NEXANS_30d_ueberfaellig;

SELECT '########## NEXANS - Statusfilter FEHLER ##########' AS marke;

SELECT '=== F0-NEXANS-30d-fehler - Plan (L15) ===' AS marke;
SET @s = CONCAT('EXPLAIN ', 'SELECT COUNT(*)                         AS zeilen,
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
    AND (m.MessageStatus LIKE ''ERROR!_%'' ESCAPE ''!''
         OR m.MessageStatus = ''COMMIT_REJECTED'')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
PREPARE p_F0_NEXANS_30d_fehler_pl FROM @s;
EXECUTE p_F0_NEXANS_30d_fehler_pl;
DEALLOCATE PREPARE p_F0_NEXANS_30d_fehler_pl;
SELECT '=== F0-NEXANS-30d-fehler - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT COUNT(*)                         AS zeilen,
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
    AND (m.MessageStatus LIKE ''ERROR!_%'' ESCAPE ''!''
         OR m.MessageStatus = ''COMMIT_REJECTED'')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t';
PREPARE p_F0_NEXANS_30d_fehler FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_F0_NEXANS_30d_fehler;
EXECUTE p_F0_NEXANS_30d_fehler;
EXECUTE p_F0_NEXANS_30d_fehler;
EXECUTE p_F0_NEXANS_30d_fehler;
EXECUTE p_F0_NEXANS_30d_fehler;
EXECUTE p_F0_NEXANS_30d_fehler;
SELECT 'F0-NEXANS-30d-fehler' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_F0_NEXANS_30d_fehler;

SELECT '=== VA-NEXANS-fehler-ohneStatus-1h - Plan (L15) ===' AS marke;
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
PREPARE p_VA_NEXANS_fehler_ohneStatus_1h_pl FROM @s;
EXECUTE p_VA_NEXANS_fehler_ohneStatus_1h_pl;
DEALLOCATE PREPARE p_VA_NEXANS_fehler_ohneStatus_1h_pl;
SELECT '=== VA-NEXANS-fehler-ohneStatus-1h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
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
PREPARE p_VA_NEXANS_fehler_ohneStatus_1h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VA_NEXANS_fehler_ohneStatus_1h;
EXECUTE p_VA_NEXANS_fehler_ohneStatus_1h;
EXECUTE p_VA_NEXANS_fehler_ohneStatus_1h;
EXECUTE p_VA_NEXANS_fehler_ohneStatus_1h;
EXECUTE p_VA_NEXANS_fehler_ohneStatus_1h;
EXECUTE p_VA_NEXANS_fehler_ohneStatus_1h;
SELECT 'VA-NEXANS-fehler-ohneStatus-1h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VA_NEXANS_fehler_ohneStatus_1h;

SELECT '=== VA-NEXANS-fehler-ohneStatus-24h - Plan (L15) ===' AS marke;
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
PREPARE p_VA_NEXANS_fehler_ohneStatus_24h_pl FROM @s;
EXECUTE p_VA_NEXANS_fehler_ohneStatus_24h_pl;
DEALLOCATE PREPARE p_VA_NEXANS_fehler_ohneStatus_24h_pl;
SELECT '=== VA-NEXANS-fehler-ohneStatus-24h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
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
PREPARE p_VA_NEXANS_fehler_ohneStatus_24h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VA_NEXANS_fehler_ohneStatus_24h;
EXECUTE p_VA_NEXANS_fehler_ohneStatus_24h;
EXECUTE p_VA_NEXANS_fehler_ohneStatus_24h;
EXECUTE p_VA_NEXANS_fehler_ohneStatus_24h;
EXECUTE p_VA_NEXANS_fehler_ohneStatus_24h;
EXECUTE p_VA_NEXANS_fehler_ohneStatus_24h;
SELECT 'VA-NEXANS-fehler-ohneStatus-24h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VA_NEXANS_fehler_ohneStatus_24h;

SELECT '=== VA-NEXANS-fehler-ohneStatus-7d - Plan (L15) ===' AS marke;
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
PREPARE p_VA_NEXANS_fehler_ohneStatus_7d_pl FROM @s;
EXECUTE p_VA_NEXANS_fehler_ohneStatus_7d_pl;
DEALLOCATE PREPARE p_VA_NEXANS_fehler_ohneStatus_7d_pl;
SELECT '=== VA-NEXANS-fehler-ohneStatus-7d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
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
PREPARE p_VA_NEXANS_fehler_ohneStatus_7d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VA_NEXANS_fehler_ohneStatus_7d;
EXECUTE p_VA_NEXANS_fehler_ohneStatus_7d;
EXECUTE p_VA_NEXANS_fehler_ohneStatus_7d;
EXECUTE p_VA_NEXANS_fehler_ohneStatus_7d;
EXECUTE p_VA_NEXANS_fehler_ohneStatus_7d;
EXECUTE p_VA_NEXANS_fehler_ohneStatus_7d;
SELECT 'VA-NEXANS-fehler-ohneStatus-7d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VA_NEXANS_fehler_ohneStatus_7d;

SELECT '=== VA-NEXANS-fehler-ohneStatus-30d - Plan (L15) ===' AS marke;
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
PREPARE p_VA_NEXANS_fehler_ohneStatus_30d_pl FROM @s;
EXECUTE p_VA_NEXANS_fehler_ohneStatus_30d_pl;
DEALLOCATE PREPARE p_VA_NEXANS_fehler_ohneStatus_30d_pl;
SELECT '=== VA-NEXANS-fehler-ohneStatus-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
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
PREPARE p_VA_NEXANS_fehler_ohneStatus_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VA_NEXANS_fehler_ohneStatus_30d;
EXECUTE p_VA_NEXANS_fehler_ohneStatus_30d;
EXECUTE p_VA_NEXANS_fehler_ohneStatus_30d;
EXECUTE p_VA_NEXANS_fehler_ohneStatus_30d;
EXECUTE p_VA_NEXANS_fehler_ohneStatus_30d;
EXECUTE p_VA_NEXANS_fehler_ohneStatus_30d;
SELECT 'VA-NEXANS-fehler-ohneStatus-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VA_NEXANS_fehler_ohneStatus_30d;

SELECT '=== NEXANS-fehler-ohneStatus - die Verengung ausgerechnet ===' AS marke;
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
SELECT 'NEXANS-fehler-ohneStatus' AS fall,
       @zeilen                                    AS zeilen_im_fenster,
       @ug                                        AS untergrenze_roh,
       @ug_wirksam                                AS untergrenze,
       TIMESTAMPDIFF(HOUR, @ug_wirksam, '2025-12-30 04:09:47')   AS fenster_stunden,
       (@zeilen IS NULL OR @zeilen = 0)           AS nullfall,
       (@ug IS NULL)                              AS keine_verengung;

SELECT '=== VERENGT-NEXANS-30d-fehler-ohneStatus - Plan (L15) ===' AS marke;
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
    AND (m.MessageStatus LIKE ''ERROR!_%'' ESCAPE ''!''
         OR m.MessageStatus = ''COMMIT_REJECTED'')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t'));
PREPARE p_VERENGT_NEXANS_30d_fehler_ohneStatus_pl FROM @s;
EXECUTE p_VERENGT_NEXANS_30d_fehler_ohneStatus_pl;
DEALLOCATE PREPARE p_VERENGT_NEXANS_30d_fehler_ohneStatus_pl;
SELECT '=== VERENGT-NEXANS-30d-fehler-ohneStatus - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
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
    AND (m.MessageStatus LIKE ''ERROR!_%'' ESCAPE ''!''
         OR m.MessageStatus = ''COMMIT_REJECTED'')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
PREPARE p_VERENGT_NEXANS_30d_fehler_ohneStatus FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VERENGT_NEXANS_30d_fehler_ohneStatus;
EXECUTE p_VERENGT_NEXANS_30d_fehler_ohneStatus;
EXECUTE p_VERENGT_NEXANS_30d_fehler_ohneStatus;
EXECUTE p_VERENGT_NEXANS_30d_fehler_ohneStatus;
EXECUTE p_VERENGT_NEXANS_30d_fehler_ohneStatus;
EXECUTE p_VERENGT_NEXANS_30d_fehler_ohneStatus;
SELECT 'VERENGT-NEXANS-30d-fehler-ohneStatus' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VERENGT_NEXANS_30d_fehler_ohneStatus;

SELECT '=== VA-NEXANS-fehler-mitStatus-1h - Plan (L15) ===' AS marke;
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
                AND (r.message_status LIKE ''ERROR!_%'' ESCAPE ''!''
                     OR r.message_status = ''COMMIT_REJECTED'')
              GROUP BY r.stunde ) s ) g'));
PREPARE p_VA_NEXANS_fehler_mitStatus_1h_pl FROM @s;
EXECUTE p_VA_NEXANS_fehler_mitStatus_1h_pl;
DEALLOCATE PREPARE p_VA_NEXANS_fehler_mitStatus_1h_pl;
SELECT '=== VA-NEXANS-fehler-mitStatus-1h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
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
                AND (r.message_status LIKE ''ERROR!_%'' ESCAPE ''!''
                     OR r.message_status = ''COMMIT_REJECTED'')
              GROUP BY r.stunde ) s ) g');
PREPARE p_VA_NEXANS_fehler_mitStatus_1h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VA_NEXANS_fehler_mitStatus_1h;
EXECUTE p_VA_NEXANS_fehler_mitStatus_1h;
EXECUTE p_VA_NEXANS_fehler_mitStatus_1h;
EXECUTE p_VA_NEXANS_fehler_mitStatus_1h;
EXECUTE p_VA_NEXANS_fehler_mitStatus_1h;
EXECUTE p_VA_NEXANS_fehler_mitStatus_1h;
SELECT 'VA-NEXANS-fehler-mitStatus-1h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VA_NEXANS_fehler_mitStatus_1h;

SELECT '=== VA-NEXANS-fehler-mitStatus-24h - Plan (L15) ===' AS marke;
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
                AND (r.message_status LIKE ''ERROR!_%'' ESCAPE ''!''
                     OR r.message_status = ''COMMIT_REJECTED'')
              GROUP BY r.stunde ) s ) g'));
PREPARE p_VA_NEXANS_fehler_mitStatus_24h_pl FROM @s;
EXECUTE p_VA_NEXANS_fehler_mitStatus_24h_pl;
DEALLOCATE PREPARE p_VA_NEXANS_fehler_mitStatus_24h_pl;
SELECT '=== VA-NEXANS-fehler-mitStatus-24h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
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
                AND (r.message_status LIKE ''ERROR!_%'' ESCAPE ''!''
                     OR r.message_status = ''COMMIT_REJECTED'')
              GROUP BY r.stunde ) s ) g');
PREPARE p_VA_NEXANS_fehler_mitStatus_24h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VA_NEXANS_fehler_mitStatus_24h;
EXECUTE p_VA_NEXANS_fehler_mitStatus_24h;
EXECUTE p_VA_NEXANS_fehler_mitStatus_24h;
EXECUTE p_VA_NEXANS_fehler_mitStatus_24h;
EXECUTE p_VA_NEXANS_fehler_mitStatus_24h;
EXECUTE p_VA_NEXANS_fehler_mitStatus_24h;
SELECT 'VA-NEXANS-fehler-mitStatus-24h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VA_NEXANS_fehler_mitStatus_24h;

SELECT '=== VA-NEXANS-fehler-mitStatus-7d - Plan (L15) ===' AS marke;
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
                AND (r.message_status LIKE ''ERROR!_%'' ESCAPE ''!''
                     OR r.message_status = ''COMMIT_REJECTED'')
              GROUP BY r.stunde ) s ) g'));
PREPARE p_VA_NEXANS_fehler_mitStatus_7d_pl FROM @s;
EXECUTE p_VA_NEXANS_fehler_mitStatus_7d_pl;
DEALLOCATE PREPARE p_VA_NEXANS_fehler_mitStatus_7d_pl;
SELECT '=== VA-NEXANS-fehler-mitStatus-7d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
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
                AND (r.message_status LIKE ''ERROR!_%'' ESCAPE ''!''
                     OR r.message_status = ''COMMIT_REJECTED'')
              GROUP BY r.stunde ) s ) g');
PREPARE p_VA_NEXANS_fehler_mitStatus_7d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VA_NEXANS_fehler_mitStatus_7d;
EXECUTE p_VA_NEXANS_fehler_mitStatus_7d;
EXECUTE p_VA_NEXANS_fehler_mitStatus_7d;
EXECUTE p_VA_NEXANS_fehler_mitStatus_7d;
EXECUTE p_VA_NEXANS_fehler_mitStatus_7d;
EXECUTE p_VA_NEXANS_fehler_mitStatus_7d;
SELECT 'VA-NEXANS-fehler-mitStatus-7d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VA_NEXANS_fehler_mitStatus_7d;

SELECT '=== VA-NEXANS-fehler-mitStatus-30d - Plan (L15) ===' AS marke;
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
                AND (r.message_status LIKE ''ERROR!_%'' ESCAPE ''!''
                     OR r.message_status = ''COMMIT_REJECTED'')
              GROUP BY r.stunde ) s ) g'));
PREPARE p_VA_NEXANS_fehler_mitStatus_30d_pl FROM @s;
EXECUTE p_VA_NEXANS_fehler_mitStatus_30d_pl;
DEALLOCATE PREPARE p_VA_NEXANS_fehler_mitStatus_30d_pl;
SELECT '=== VA-NEXANS-fehler-mitStatus-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
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
                AND (r.message_status LIKE ''ERROR!_%'' ESCAPE ''!''
                     OR r.message_status = ''COMMIT_REJECTED'')
              GROUP BY r.stunde ) s ) g');
PREPARE p_VA_NEXANS_fehler_mitStatus_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VA_NEXANS_fehler_mitStatus_30d;
EXECUTE p_VA_NEXANS_fehler_mitStatus_30d;
EXECUTE p_VA_NEXANS_fehler_mitStatus_30d;
EXECUTE p_VA_NEXANS_fehler_mitStatus_30d;
EXECUTE p_VA_NEXANS_fehler_mitStatus_30d;
EXECUTE p_VA_NEXANS_fehler_mitStatus_30d;
SELECT 'VA-NEXANS-fehler-mitStatus-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VA_NEXANS_fehler_mitStatus_30d;

SELECT '=== NEXANS-fehler-mitStatus - die Verengung ausgerechnet ===' AS marke;
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
                AND (r.message_status LIKE ''ERROR!_%'' ESCAPE ''!''
                     OR r.message_status = ''COMMIT_REJECTED'')
              GROUP BY r.stunde ) s ) g');
PREPARE p_rechne FROM @s;
EXECUTE p_rechne;
DEALLOCATE PREPARE p_rechne;
SET @ug_wirksam = GREATEST('2025-11-30 04:09:47', COALESCE(@ug, '2025-11-30 04:09:47'));
SELECT 'NEXANS-fehler-mitStatus' AS fall,
       @zeilen                                    AS zeilen_im_fenster,
       @ug                                        AS untergrenze_roh,
       @ug_wirksam                                AS untergrenze,
       TIMESTAMPDIFF(HOUR, @ug_wirksam, '2025-12-30 04:09:47')   AS fenster_stunden,
       (@zeilen IS NULL OR @zeilen = 0)           AS nullfall,
       (@ug IS NULL)                              AS keine_verengung;

SELECT '=== VERENGT-NEXANS-30d-fehler-mitStatus - Plan (L15) ===' AS marke;
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
    AND (m.MessageStatus LIKE ''ERROR!_%'' ESCAPE ''!''
         OR m.MessageStatus = ''COMMIT_REJECTED'')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t'));
PREPARE p_VERENGT_NEXANS_30d_fehler_mitStatus_pl FROM @s;
EXECUTE p_VERENGT_NEXANS_30d_fehler_mitStatus_pl;
DEALLOCATE PREPARE p_VERENGT_NEXANS_30d_fehler_mitStatus_pl;
SELECT '=== VERENGT-NEXANS-30d-fehler-mitStatus - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
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
    AND (m.MessageStatus LIKE ''ERROR!_%'' ESCAPE ''!''
         OR m.MessageStatus = ''COMMIT_REJECTED'')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
PREPARE p_VERENGT_NEXANS_30d_fehler_mitStatus FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VERENGT_NEXANS_30d_fehler_mitStatus;
EXECUTE p_VERENGT_NEXANS_30d_fehler_mitStatus;
EXECUTE p_VERENGT_NEXANS_30d_fehler_mitStatus;
EXECUTE p_VERENGT_NEXANS_30d_fehler_mitStatus;
EXECUTE p_VERENGT_NEXANS_30d_fehler_mitStatus;
EXECUTE p_VERENGT_NEXANS_30d_fehler_mitStatus;
SELECT 'VERENGT-NEXANS-30d-fehler-mitStatus' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VERENGT_NEXANS_30d_fehler_mitStatus;

SELECT '########## SUTTONS ##########' AS marke;
SELECT GROUP_CONCAT(QUOTE(ProcessID)) INTO @proz
FROM (SELECT DISTINCT p.ProcessID
      FROM GlassfishDB.Process p
      JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
      WHERE pm.MandantID = 'SUTTONS') x;
SELECT 'SUTTONS' AS mandant,
       (@proz IS NOT NULL) AS prozesse_vorhanden,
       LENGTH(@proz) - LENGTH(REPLACE(@proz, ',', '')) + 1 AS prozesse_anzahl;

SELECT '########## SUTTONS - ueberfaellig (Rueckfallpfad) ##########' AS marke;
SELECT 'SUTTONS' AS mandant,
       'die Verengung wird hier gar nicht erst betreten' AS vermerk,
       'MessageTimeout und jetzt stehen nicht im Rollup' AS grund;

SELECT '=== RUECKFALL-SUTTONS-30d-ueberfaellig - Plan (L15) ===' AS marke;
SET @s = CONCAT('EXPLAIN ', 'SELECT COUNT(*)                         AS zeilen,
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
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''SUTTONS'')
    AND m.MessageStatus IN (''SUSPENDED'',''RUNNING'')
    AND m.MessageTimeout IS NOT NULL
    AND m.MessageTimeout > 0
    AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND < ''2025-12-30 04:09:47''
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
PREPARE p_RUECKFALL_SUTTONS_30d_ueberfaellig_pl FROM @s;
EXECUTE p_RUECKFALL_SUTTONS_30d_ueberfaellig_pl;
DEALLOCATE PREPARE p_RUECKFALL_SUTTONS_30d_ueberfaellig_pl;
SELECT '=== RUECKFALL-SUTTONS-30d-ueberfaellig - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT COUNT(*)                         AS zeilen,
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
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''SUTTONS'')
    AND m.MessageStatus IN (''SUSPENDED'',''RUNNING'')
    AND m.MessageTimeout IS NOT NULL
    AND m.MessageTimeout > 0
    AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND < ''2025-12-30 04:09:47''
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t';
PREPARE p_RUECKFALL_SUTTONS_30d_ueberfaellig FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_RUECKFALL_SUTTONS_30d_ueberfaellig;
EXECUTE p_RUECKFALL_SUTTONS_30d_ueberfaellig;
EXECUTE p_RUECKFALL_SUTTONS_30d_ueberfaellig;
EXECUTE p_RUECKFALL_SUTTONS_30d_ueberfaellig;
EXECUTE p_RUECKFALL_SUTTONS_30d_ueberfaellig;
EXECUTE p_RUECKFALL_SUTTONS_30d_ueberfaellig;
SELECT 'RUECKFALL-SUTTONS-30d-ueberfaellig' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_RUECKFALL_SUTTONS_30d_ueberfaellig;

SELECT '########## SUTTONS - Statusfilter FEHLER ##########' AS marke;

SELECT '=== F0-SUTTONS-30d-fehler - Plan (L15) ===' AS marke;
SET @s = CONCAT('EXPLAIN ', 'SELECT COUNT(*)                         AS zeilen,
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
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''SUTTONS'')
    AND (m.MessageStatus LIKE ''ERROR!_%'' ESCAPE ''!''
         OR m.MessageStatus = ''COMMIT_REJECTED'')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
PREPARE p_F0_SUTTONS_30d_fehler_pl FROM @s;
EXECUTE p_F0_SUTTONS_30d_fehler_pl;
DEALLOCATE PREPARE p_F0_SUTTONS_30d_fehler_pl;
SELECT '=== F0-SUTTONS-30d-fehler - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT COUNT(*)                         AS zeilen,
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
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''SUTTONS'')
    AND (m.MessageStatus LIKE ''ERROR!_%'' ESCAPE ''!''
         OR m.MessageStatus = ''COMMIT_REJECTED'')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t';
PREPARE p_F0_SUTTONS_30d_fehler FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_F0_SUTTONS_30d_fehler;
EXECUTE p_F0_SUTTONS_30d_fehler;
EXECUTE p_F0_SUTTONS_30d_fehler;
EXECUTE p_F0_SUTTONS_30d_fehler;
EXECUTE p_F0_SUTTONS_30d_fehler;
EXECUTE p_F0_SUTTONS_30d_fehler;
SELECT 'F0-SUTTONS-30d-fehler' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_F0_SUTTONS_30d_fehler;

SELECT '=== VA-SUTTONS-fehler-ohneStatus-1h - Plan (L15) ===' AS marke;
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
PREPARE p_VA_SUTTONS_fehler_ohneStatus_1h_pl FROM @s;
EXECUTE p_VA_SUTTONS_fehler_ohneStatus_1h_pl;
DEALLOCATE PREPARE p_VA_SUTTONS_fehler_ohneStatus_1h_pl;
SELECT '=== VA-SUTTONS-fehler-ohneStatus-1h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
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
PREPARE p_VA_SUTTONS_fehler_ohneStatus_1h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VA_SUTTONS_fehler_ohneStatus_1h;
EXECUTE p_VA_SUTTONS_fehler_ohneStatus_1h;
EXECUTE p_VA_SUTTONS_fehler_ohneStatus_1h;
EXECUTE p_VA_SUTTONS_fehler_ohneStatus_1h;
EXECUTE p_VA_SUTTONS_fehler_ohneStatus_1h;
EXECUTE p_VA_SUTTONS_fehler_ohneStatus_1h;
SELECT 'VA-SUTTONS-fehler-ohneStatus-1h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VA_SUTTONS_fehler_ohneStatus_1h;

SELECT '=== VA-SUTTONS-fehler-ohneStatus-24h - Plan (L15) ===' AS marke;
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
PREPARE p_VA_SUTTONS_fehler_ohneStatus_24h_pl FROM @s;
EXECUTE p_VA_SUTTONS_fehler_ohneStatus_24h_pl;
DEALLOCATE PREPARE p_VA_SUTTONS_fehler_ohneStatus_24h_pl;
SELECT '=== VA-SUTTONS-fehler-ohneStatus-24h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
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
PREPARE p_VA_SUTTONS_fehler_ohneStatus_24h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VA_SUTTONS_fehler_ohneStatus_24h;
EXECUTE p_VA_SUTTONS_fehler_ohneStatus_24h;
EXECUTE p_VA_SUTTONS_fehler_ohneStatus_24h;
EXECUTE p_VA_SUTTONS_fehler_ohneStatus_24h;
EXECUTE p_VA_SUTTONS_fehler_ohneStatus_24h;
EXECUTE p_VA_SUTTONS_fehler_ohneStatus_24h;
SELECT 'VA-SUTTONS-fehler-ohneStatus-24h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VA_SUTTONS_fehler_ohneStatus_24h;

SELECT '=== VA-SUTTONS-fehler-ohneStatus-7d - Plan (L15) ===' AS marke;
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
PREPARE p_VA_SUTTONS_fehler_ohneStatus_7d_pl FROM @s;
EXECUTE p_VA_SUTTONS_fehler_ohneStatus_7d_pl;
DEALLOCATE PREPARE p_VA_SUTTONS_fehler_ohneStatus_7d_pl;
SELECT '=== VA-SUTTONS-fehler-ohneStatus-7d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
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
PREPARE p_VA_SUTTONS_fehler_ohneStatus_7d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VA_SUTTONS_fehler_ohneStatus_7d;
EXECUTE p_VA_SUTTONS_fehler_ohneStatus_7d;
EXECUTE p_VA_SUTTONS_fehler_ohneStatus_7d;
EXECUTE p_VA_SUTTONS_fehler_ohneStatus_7d;
EXECUTE p_VA_SUTTONS_fehler_ohneStatus_7d;
EXECUTE p_VA_SUTTONS_fehler_ohneStatus_7d;
SELECT 'VA-SUTTONS-fehler-ohneStatus-7d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VA_SUTTONS_fehler_ohneStatus_7d;

SELECT '=== VA-SUTTONS-fehler-ohneStatus-30d - Plan (L15) ===' AS marke;
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
PREPARE p_VA_SUTTONS_fehler_ohneStatus_30d_pl FROM @s;
EXECUTE p_VA_SUTTONS_fehler_ohneStatus_30d_pl;
DEALLOCATE PREPARE p_VA_SUTTONS_fehler_ohneStatus_30d_pl;
SELECT '=== VA-SUTTONS-fehler-ohneStatus-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
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
PREPARE p_VA_SUTTONS_fehler_ohneStatus_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VA_SUTTONS_fehler_ohneStatus_30d;
EXECUTE p_VA_SUTTONS_fehler_ohneStatus_30d;
EXECUTE p_VA_SUTTONS_fehler_ohneStatus_30d;
EXECUTE p_VA_SUTTONS_fehler_ohneStatus_30d;
EXECUTE p_VA_SUTTONS_fehler_ohneStatus_30d;
EXECUTE p_VA_SUTTONS_fehler_ohneStatus_30d;
SELECT 'VA-SUTTONS-fehler-ohneStatus-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VA_SUTTONS_fehler_ohneStatus_30d;

SELECT '=== SUTTONS-fehler-ohneStatus - die Verengung ausgerechnet ===' AS marke;
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
SELECT 'SUTTONS-fehler-ohneStatus' AS fall,
       @zeilen                                    AS zeilen_im_fenster,
       @ug                                        AS untergrenze_roh,
       @ug_wirksam                                AS untergrenze,
       TIMESTAMPDIFF(HOUR, @ug_wirksam, '2025-12-30 04:09:47')   AS fenster_stunden,
       (@zeilen IS NULL OR @zeilen = 0)           AS nullfall,
       (@ug IS NULL)                              AS keine_verengung;

SELECT '=== VERENGT-SUTTONS-30d-fehler-ohneStatus - Plan (L15) ===' AS marke;
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
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''SUTTONS'')
    AND (m.MessageStatus LIKE ''ERROR!_%'' ESCAPE ''!''
         OR m.MessageStatus = ''COMMIT_REJECTED'')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t'));
PREPARE p_VERENGT_SUTTONS_30d_fehler_ohneStatus_pl FROM @s;
EXECUTE p_VERENGT_SUTTONS_30d_fehler_ohneStatus_pl;
DEALLOCATE PREPARE p_VERENGT_SUTTONS_30d_fehler_ohneStatus_pl;
SELECT '=== VERENGT-SUTTONS-30d-fehler-ohneStatus - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
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
    AND (m.MessageStatus LIKE ''ERROR!_%'' ESCAPE ''!''
         OR m.MessageStatus = ''COMMIT_REJECTED'')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
PREPARE p_VERENGT_SUTTONS_30d_fehler_ohneStatus FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VERENGT_SUTTONS_30d_fehler_ohneStatus;
EXECUTE p_VERENGT_SUTTONS_30d_fehler_ohneStatus;
EXECUTE p_VERENGT_SUTTONS_30d_fehler_ohneStatus;
EXECUTE p_VERENGT_SUTTONS_30d_fehler_ohneStatus;
EXECUTE p_VERENGT_SUTTONS_30d_fehler_ohneStatus;
EXECUTE p_VERENGT_SUTTONS_30d_fehler_ohneStatus;
SELECT 'VERENGT-SUTTONS-30d-fehler-ohneStatus' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VERENGT_SUTTONS_30d_fehler_ohneStatus;

SELECT '=== VA-SUTTONS-fehler-mitStatus-1h - Plan (L15) ===' AS marke;
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
                AND (r.message_status LIKE ''ERROR!_%'' ESCAPE ''!''
                     OR r.message_status = ''COMMIT_REJECTED'')
              GROUP BY r.stunde ) s ) g'));
PREPARE p_VA_SUTTONS_fehler_mitStatus_1h_pl FROM @s;
EXECUTE p_VA_SUTTONS_fehler_mitStatus_1h_pl;
DEALLOCATE PREPARE p_VA_SUTTONS_fehler_mitStatus_1h_pl;
SELECT '=== VA-SUTTONS-fehler-mitStatus-1h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
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
                AND (r.message_status LIKE ''ERROR!_%'' ESCAPE ''!''
                     OR r.message_status = ''COMMIT_REJECTED'')
              GROUP BY r.stunde ) s ) g');
PREPARE p_VA_SUTTONS_fehler_mitStatus_1h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VA_SUTTONS_fehler_mitStatus_1h;
EXECUTE p_VA_SUTTONS_fehler_mitStatus_1h;
EXECUTE p_VA_SUTTONS_fehler_mitStatus_1h;
EXECUTE p_VA_SUTTONS_fehler_mitStatus_1h;
EXECUTE p_VA_SUTTONS_fehler_mitStatus_1h;
EXECUTE p_VA_SUTTONS_fehler_mitStatus_1h;
SELECT 'VA-SUTTONS-fehler-mitStatus-1h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VA_SUTTONS_fehler_mitStatus_1h;

SELECT '=== VA-SUTTONS-fehler-mitStatus-24h - Plan (L15) ===' AS marke;
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
                AND (r.message_status LIKE ''ERROR!_%'' ESCAPE ''!''
                     OR r.message_status = ''COMMIT_REJECTED'')
              GROUP BY r.stunde ) s ) g'));
PREPARE p_VA_SUTTONS_fehler_mitStatus_24h_pl FROM @s;
EXECUTE p_VA_SUTTONS_fehler_mitStatus_24h_pl;
DEALLOCATE PREPARE p_VA_SUTTONS_fehler_mitStatus_24h_pl;
SELECT '=== VA-SUTTONS-fehler-mitStatus-24h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
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
                AND (r.message_status LIKE ''ERROR!_%'' ESCAPE ''!''
                     OR r.message_status = ''COMMIT_REJECTED'')
              GROUP BY r.stunde ) s ) g');
PREPARE p_VA_SUTTONS_fehler_mitStatus_24h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VA_SUTTONS_fehler_mitStatus_24h;
EXECUTE p_VA_SUTTONS_fehler_mitStatus_24h;
EXECUTE p_VA_SUTTONS_fehler_mitStatus_24h;
EXECUTE p_VA_SUTTONS_fehler_mitStatus_24h;
EXECUTE p_VA_SUTTONS_fehler_mitStatus_24h;
EXECUTE p_VA_SUTTONS_fehler_mitStatus_24h;
SELECT 'VA-SUTTONS-fehler-mitStatus-24h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VA_SUTTONS_fehler_mitStatus_24h;

SELECT '=== VA-SUTTONS-fehler-mitStatus-7d - Plan (L15) ===' AS marke;
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
                AND (r.message_status LIKE ''ERROR!_%'' ESCAPE ''!''
                     OR r.message_status = ''COMMIT_REJECTED'')
              GROUP BY r.stunde ) s ) g'));
PREPARE p_VA_SUTTONS_fehler_mitStatus_7d_pl FROM @s;
EXECUTE p_VA_SUTTONS_fehler_mitStatus_7d_pl;
DEALLOCATE PREPARE p_VA_SUTTONS_fehler_mitStatus_7d_pl;
SELECT '=== VA-SUTTONS-fehler-mitStatus-7d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
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
                AND (r.message_status LIKE ''ERROR!_%'' ESCAPE ''!''
                     OR r.message_status = ''COMMIT_REJECTED'')
              GROUP BY r.stunde ) s ) g');
PREPARE p_VA_SUTTONS_fehler_mitStatus_7d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VA_SUTTONS_fehler_mitStatus_7d;
EXECUTE p_VA_SUTTONS_fehler_mitStatus_7d;
EXECUTE p_VA_SUTTONS_fehler_mitStatus_7d;
EXECUTE p_VA_SUTTONS_fehler_mitStatus_7d;
EXECUTE p_VA_SUTTONS_fehler_mitStatus_7d;
EXECUTE p_VA_SUTTONS_fehler_mitStatus_7d;
SELECT 'VA-SUTTONS-fehler-mitStatus-7d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VA_SUTTONS_fehler_mitStatus_7d;

SELECT '=== VA-SUTTONS-fehler-mitStatus-30d - Plan (L15) ===' AS marke;
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
                AND (r.message_status LIKE ''ERROR!_%'' ESCAPE ''!''
                     OR r.message_status = ''COMMIT_REJECTED'')
              GROUP BY r.stunde ) s ) g'));
PREPARE p_VA_SUTTONS_fehler_mitStatus_30d_pl FROM @s;
EXECUTE p_VA_SUTTONS_fehler_mitStatus_30d_pl;
DEALLOCATE PREPARE p_VA_SUTTONS_fehler_mitStatus_30d_pl;
SELECT '=== VA-SUTTONS-fehler-mitStatus-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
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
                AND (r.message_status LIKE ''ERROR!_%'' ESCAPE ''!''
                     OR r.message_status = ''COMMIT_REJECTED'')
              GROUP BY r.stunde ) s ) g');
PREPARE p_VA_SUTTONS_fehler_mitStatus_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VA_SUTTONS_fehler_mitStatus_30d;
EXECUTE p_VA_SUTTONS_fehler_mitStatus_30d;
EXECUTE p_VA_SUTTONS_fehler_mitStatus_30d;
EXECUTE p_VA_SUTTONS_fehler_mitStatus_30d;
EXECUTE p_VA_SUTTONS_fehler_mitStatus_30d;
EXECUTE p_VA_SUTTONS_fehler_mitStatus_30d;
SELECT 'VA-SUTTONS-fehler-mitStatus-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VA_SUTTONS_fehler_mitStatus_30d;

SELECT '=== SUTTONS-fehler-mitStatus - die Verengung ausgerechnet ===' AS marke;
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
                AND (r.message_status LIKE ''ERROR!_%'' ESCAPE ''!''
                     OR r.message_status = ''COMMIT_REJECTED'')
              GROUP BY r.stunde ) s ) g');
PREPARE p_rechne FROM @s;
EXECUTE p_rechne;
DEALLOCATE PREPARE p_rechne;
SET @ug_wirksam = GREATEST('2025-11-30 04:09:47', COALESCE(@ug, '2025-11-30 04:09:47'));
SELECT 'SUTTONS-fehler-mitStatus' AS fall,
       @zeilen                                    AS zeilen_im_fenster,
       @ug                                        AS untergrenze_roh,
       @ug_wirksam                                AS untergrenze,
       TIMESTAMPDIFF(HOUR, @ug_wirksam, '2025-12-30 04:09:47')   AS fenster_stunden,
       (@zeilen IS NULL OR @zeilen = 0)           AS nullfall,
       (@ug IS NULL)                              AS keine_verengung;

SELECT '=== VERENGT-SUTTONS-30d-fehler-mitStatus - Plan (L15) ===' AS marke;
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
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''SUTTONS'')
    AND (m.MessageStatus LIKE ''ERROR!_%'' ESCAPE ''!''
         OR m.MessageStatus = ''COMMIT_REJECTED'')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t'));
PREPARE p_VERENGT_SUTTONS_30d_fehler_mitStatus_pl FROM @s;
EXECUTE p_VERENGT_SUTTONS_30d_fehler_mitStatus_pl;
DEALLOCATE PREPARE p_VERENGT_SUTTONS_30d_fehler_mitStatus_pl;
SELECT '=== VERENGT-SUTTONS-30d-fehler-mitStatus - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
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
    AND (m.MessageStatus LIKE ''ERROR!_%'' ESCAPE ''!''
         OR m.MessageStatus = ''COMMIT_REJECTED'')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
PREPARE p_VERENGT_SUTTONS_30d_fehler_mitStatus FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VERENGT_SUTTONS_30d_fehler_mitStatus;
EXECUTE p_VERENGT_SUTTONS_30d_fehler_mitStatus;
EXECUTE p_VERENGT_SUTTONS_30d_fehler_mitStatus;
EXECUTE p_VERENGT_SUTTONS_30d_fehler_mitStatus;
EXECUTE p_VERENGT_SUTTONS_30d_fehler_mitStatus;
EXECUTE p_VERENGT_SUTTONS_30d_fehler_mitStatus;
SELECT 'VERENGT-SUTTONS-30d-fehler-mitStatus' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VERENGT_SUTTONS_30d_fehler_mitStatus;

SELECT '=== 99 Abschluss ===' AS marke;
SELECT @@global.read_only AS read_only_am_ende, NOW() AS serverzeit_ende;
