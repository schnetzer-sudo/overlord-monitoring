-- Messung M106 - Abschlussmessung des gebauten Wegs - NEXANS, SUTTONS, VOTG, IBIS, IBISGUS
-- Auftrag:  "Fensterverengung fuer die Nachrichtenliste", Stand 27.08.2026
-- Ergebnis: docs/messungen-liste-verengung.md
--
-- M106 - Die Abschlussmessung des gebauten Wegs (Teil C.5). Gemessen wird, was der Code schickt:
-- die Wasserstandsabfrage, die gestuften Vorabfragen und die Quellabfrage ueber das verengte
-- Fenster - alle drei aus VerengungRepository bzw. NachrichtenRepository GERENDERT und von dort
-- abgeschrieben, nicht nachgebaut (Regel L7). Die Tabelle traegt jetzt den Index aus V11.
--
-- Alle ZEHN Mandanten, 30 Tage und 24 Stunden, dazu der Prozessfilter bei NEXANS. Nicht zwei -
-- genau daran ist Fassung F6 in 10b-1 gescheitert.
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

SELECT '=== Der Index aus V11 ist da? ===' AS marke;
SHOW INDEX FROM overlord_monitor.message_rollup;

SELECT '=== W-wasserstand - Plan (L15) ===' AS marke;
SET @s = CONCAT('EXPLAIN ', 'SELECT MAX(fenster_bis) FROM overlord_monitor.rollup_lauf
 WHERE beendet_am IS NOT NULL AND fehler IS NULL');
PREPARE p_W_wasserstand_pl FROM @s;
EXECUTE p_W_wasserstand_pl;
DEALLOCATE PREPARE p_W_wasserstand_pl;
SELECT '=== W-wasserstand - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT MAX(fenster_bis) FROM overlord_monitor.rollup_lauf
 WHERE beendet_am IS NOT NULL AND fehler IS NULL';
PREPARE p_W_wasserstand FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_W_wasserstand;
EXECUTE p_W_wasserstand;
EXECUTE p_W_wasserstand;
EXECUTE p_W_wasserstand;
EXECUTE p_W_wasserstand;
EXECUTE p_W_wasserstand;
SELECT 'W-wasserstand' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_W_wasserstand;

SELECT '########## NEXANS ##########' AS marke;

SELECT '=== F0-NEXANS-30d - Plan (L15) ===' AS marke;
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
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
PREPARE p_F0_NEXANS_30d_pl FROM @s;
EXECUTE p_F0_NEXANS_30d_pl;
DEALLOCATE PREPARE p_F0_NEXANS_30d_pl;
SELECT '=== F0-NEXANS-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
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
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t';
PREPARE p_F0_NEXANS_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_F0_NEXANS_30d;
EXECUTE p_F0_NEXANS_30d;
EXECUTE p_F0_NEXANS_30d;
EXECUTE p_F0_NEXANS_30d;
EXECUTE p_F0_NEXANS_30d;
EXECUTE p_F0_NEXANS_30d;
SELECT 'F0-NEXANS-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_F0_NEXANS_30d;
SELECT '=== VA-NEXANS-30d-1h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(kumuliert.anzahl), MAX(CASE WHEN kumuliert.kumuliert >= 51 THEN kumuliert.stunde END)
FROM (SELECT je_stunde.stunde, je_stunde.anzahl,
             SUM(CASE WHEN je_stunde.voll THEN je_stunde.anzahl ELSE 0 END)
                 OVER (ORDER BY je_stunde.stunde DESC) AS kumuliert
      FROM (SELECT r.stunde AS stunde, SUM(r.anzahl) AS anzahl,
                   (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
            FROM overlord_monitor.message_rollup r
            WHERE r.stunde >= ''2025-12-30 03:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
              AND r.stunde < ''2026-08-27 15:00:00''
              AND EXISTS (SELECT 1 FROM GlassfishDB.Process verengung_process
                          JOIN GlassfishDB.ProjectMandant
                            ON GlassfishDB.ProjectMandant.ProjectID = verengung_process.ProjectID
                          WHERE verengung_process.ProcessID = r.process_id
                            AND GlassfishDB.ProjectMandant.MandantID = ''NEXANS'')
            GROUP BY r.stunde) AS je_stunde) AS kumuliert';
PREPARE p_VA_NEXANS_30d_1h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VA_NEXANS_30d_1h;
EXECUTE p_VA_NEXANS_30d_1h;
EXECUTE p_VA_NEXANS_30d_1h;
EXECUTE p_VA_NEXANS_30d_1h;
EXECUTE p_VA_NEXANS_30d_1h;
EXECUTE p_VA_NEXANS_30d_1h;
SELECT 'VA-NEXANS-30d-1h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VA_NEXANS_30d_1h;
SELECT '=== VA-NEXANS-30d-24h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(kumuliert.anzahl), MAX(CASE WHEN kumuliert.kumuliert >= 51 THEN kumuliert.stunde END)
FROM (SELECT je_stunde.stunde, je_stunde.anzahl,
             SUM(CASE WHEN je_stunde.voll THEN je_stunde.anzahl ELSE 0 END)
                 OVER (ORDER BY je_stunde.stunde DESC) AS kumuliert
      FROM (SELECT r.stunde AS stunde, SUM(r.anzahl) AS anzahl,
                   (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
            FROM overlord_monitor.message_rollup r
            WHERE r.stunde >= ''2025-12-29 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
              AND r.stunde < ''2026-08-27 15:00:00''
              AND EXISTS (SELECT 1 FROM GlassfishDB.Process verengung_process
                          JOIN GlassfishDB.ProjectMandant
                            ON GlassfishDB.ProjectMandant.ProjectID = verengung_process.ProjectID
                          WHERE verengung_process.ProcessID = r.process_id
                            AND GlassfishDB.ProjectMandant.MandantID = ''NEXANS'')
            GROUP BY r.stunde) AS je_stunde) AS kumuliert';
PREPARE p_VA_NEXANS_30d_24h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VA_NEXANS_30d_24h;
EXECUTE p_VA_NEXANS_30d_24h;
EXECUTE p_VA_NEXANS_30d_24h;
EXECUTE p_VA_NEXANS_30d_24h;
EXECUTE p_VA_NEXANS_30d_24h;
EXECUTE p_VA_NEXANS_30d_24h;
SELECT 'VA-NEXANS-30d-24h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VA_NEXANS_30d_24h;

SELECT '=== VA-NEXANS-30d-30d - Plan (L15) ===' AS marke;
SET @s = CONCAT('EXPLAIN ', 'SELECT SUM(kumuliert.anzahl), MAX(CASE WHEN kumuliert.kumuliert >= 51 THEN kumuliert.stunde END)
FROM (SELECT je_stunde.stunde, je_stunde.anzahl,
             SUM(CASE WHEN je_stunde.voll THEN je_stunde.anzahl ELSE 0 END)
                 OVER (ORDER BY je_stunde.stunde DESC) AS kumuliert
      FROM (SELECT r.stunde AS stunde, SUM(r.anzahl) AS anzahl,
                   (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
            FROM overlord_monitor.message_rollup r
            WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
              AND r.stunde < ''2026-08-27 15:00:00''
              AND EXISTS (SELECT 1 FROM GlassfishDB.Process verengung_process
                          JOIN GlassfishDB.ProjectMandant
                            ON GlassfishDB.ProjectMandant.ProjectID = verengung_process.ProjectID
                          WHERE verengung_process.ProcessID = r.process_id
                            AND GlassfishDB.ProjectMandant.MandantID = ''NEXANS'')
            GROUP BY r.stunde) AS je_stunde) AS kumuliert');
PREPARE p_VA_NEXANS_30d_30d_pl FROM @s;
EXECUTE p_VA_NEXANS_30d_30d_pl;
DEALLOCATE PREPARE p_VA_NEXANS_30d_30d_pl;
SELECT '=== VA-NEXANS-30d-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(kumuliert.anzahl), MAX(CASE WHEN kumuliert.kumuliert >= 51 THEN kumuliert.stunde END)
FROM (SELECT je_stunde.stunde, je_stunde.anzahl,
             SUM(CASE WHEN je_stunde.voll THEN je_stunde.anzahl ELSE 0 END)
                 OVER (ORDER BY je_stunde.stunde DESC) AS kumuliert
      FROM (SELECT r.stunde AS stunde, SUM(r.anzahl) AS anzahl,
                   (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
            FROM overlord_monitor.message_rollup r
            WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
              AND r.stunde < ''2026-08-27 15:00:00''
              AND EXISTS (SELECT 1 FROM GlassfishDB.Process verengung_process
                          JOIN GlassfishDB.ProjectMandant
                            ON GlassfishDB.ProjectMandant.ProjectID = verengung_process.ProjectID
                          WHERE verengung_process.ProcessID = r.process_id
                            AND GlassfishDB.ProjectMandant.MandantID = ''NEXANS'')
            GROUP BY r.stunde) AS je_stunde) AS kumuliert';
PREPARE p_VA_NEXANS_30d_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VA_NEXANS_30d_30d;
EXECUTE p_VA_NEXANS_30d_30d;
EXECUTE p_VA_NEXANS_30d_30d;
EXECUTE p_VA_NEXANS_30d_30d;
EXECUTE p_VA_NEXANS_30d_30d;
EXECUTE p_VA_NEXANS_30d_30d;
SELECT 'VA-NEXANS-30d-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VA_NEXANS_30d_30d;

SELECT '=== NEXANS-30d - die Verengung ausgerechnet ===' AS marke;
SET @zeilen = NULL, @ug = NULL;
SET @s = 'SELECT SUM(kumuliert.anzahl), MAX(CASE WHEN kumuliert.kumuliert >= 51 THEN kumuliert.stunde END) INTO @zeilen, @ug
FROM (SELECT je_stunde.stunde, je_stunde.anzahl,
             SUM(CASE WHEN je_stunde.voll THEN je_stunde.anzahl ELSE 0 END)
                 OVER (ORDER BY je_stunde.stunde DESC) AS kumuliert
      FROM (SELECT r.stunde AS stunde, SUM(r.anzahl) AS anzahl,
                   (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
            FROM overlord_monitor.message_rollup r
            WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
              AND r.stunde < ''2026-08-27 15:00:00''
              AND EXISTS (SELECT 1 FROM GlassfishDB.Process verengung_process
                          JOIN GlassfishDB.ProjectMandant
                            ON GlassfishDB.ProjectMandant.ProjectID = verengung_process.ProjectID
                          WHERE verengung_process.ProcessID = r.process_id
                            AND GlassfishDB.ProjectMandant.MandantID = ''NEXANS'')
            GROUP BY r.stunde) AS je_stunde) AS kumuliert';
PREPARE p_rechne FROM @s;
EXECUTE p_rechne;
DEALLOCATE PREPARE p_rechne;
SET @ug_wirksam = GREATEST('2025-11-30 04:09:47', COALESCE(@ug, '2025-11-30 04:09:47'));
SELECT 'NEXANS-30d' AS fall, @zeilen AS zeilen_im_fenster, @ug AS untergrenze_roh,
       @ug_wirksam AS untergrenze,
       TIMESTAMPDIFF(HOUR, @ug_wirksam, '2025-12-30 04:09:47') AS fenster_stunden,
       (@zeilen IS NULL OR @zeilen = 0) AS nullfall, (@ug IS NULL) AS keine_verengung;

SELECT '=== VERENGT-NEXANS-30d - Plan (L15) ===' AS marke;
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
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t'));
PREPARE p_VERENGT_NEXANS_30d_pl FROM @s;
EXECUTE p_VERENGT_NEXANS_30d_pl;
DEALLOCATE PREPARE p_VERENGT_NEXANS_30d_pl;
SELECT '=== VERENGT-NEXANS-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
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
PREPARE p_VERENGT_NEXANS_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VERENGT_NEXANS_30d;
EXECUTE p_VERENGT_NEXANS_30d;
EXECUTE p_VERENGT_NEXANS_30d;
EXECUTE p_VERENGT_NEXANS_30d;
EXECUTE p_VERENGT_NEXANS_30d;
EXECUTE p_VERENGT_NEXANS_30d;
SELECT 'VERENGT-NEXANS-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VERENGT_NEXANS_30d;

SELECT '=== F0-NEXANS-24h - Plan (L15) ===' AS marke;
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
  WHERE m.MessageLastUpdate >= ''2025-12-29 04:09:47''
    AND m.MessageLastUpdate <= ''2025-12-30 04:09:47''
    AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''NEXANS'')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
PREPARE p_F0_NEXANS_24h_pl FROM @s;
EXECUTE p_F0_NEXANS_24h_pl;
DEALLOCATE PREPARE p_F0_NEXANS_24h_pl;
SELECT '=== F0-NEXANS-24h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
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
  WHERE m.MessageLastUpdate >= ''2025-12-29 04:09:47''
    AND m.MessageLastUpdate <= ''2025-12-30 04:09:47''
    AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''NEXANS'')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t';
PREPARE p_F0_NEXANS_24h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_F0_NEXANS_24h;
EXECUTE p_F0_NEXANS_24h;
EXECUTE p_F0_NEXANS_24h;
EXECUTE p_F0_NEXANS_24h;
EXECUTE p_F0_NEXANS_24h;
EXECUTE p_F0_NEXANS_24h;
SELECT 'F0-NEXANS-24h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_F0_NEXANS_24h;
SELECT '=== VA-NEXANS-24h-1h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(kumuliert.anzahl), MAX(CASE WHEN kumuliert.kumuliert >= 51 THEN kumuliert.stunde END)
FROM (SELECT je_stunde.stunde, je_stunde.anzahl,
             SUM(CASE WHEN je_stunde.voll THEN je_stunde.anzahl ELSE 0 END)
                 OVER (ORDER BY je_stunde.stunde DESC) AS kumuliert
      FROM (SELECT r.stunde AS stunde, SUM(r.anzahl) AS anzahl,
                   (r.stunde >= ''2025-12-29 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
            FROM overlord_monitor.message_rollup r
            WHERE r.stunde >= ''2025-12-30 03:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
              AND r.stunde < ''2026-08-27 15:00:00''
              AND EXISTS (SELECT 1 FROM GlassfishDB.Process verengung_process
                          JOIN GlassfishDB.ProjectMandant
                            ON GlassfishDB.ProjectMandant.ProjectID = verengung_process.ProjectID
                          WHERE verengung_process.ProcessID = r.process_id
                            AND GlassfishDB.ProjectMandant.MandantID = ''NEXANS'')
            GROUP BY r.stunde) AS je_stunde) AS kumuliert';
PREPARE p_VA_NEXANS_24h_1h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VA_NEXANS_24h_1h;
EXECUTE p_VA_NEXANS_24h_1h;
EXECUTE p_VA_NEXANS_24h_1h;
EXECUTE p_VA_NEXANS_24h_1h;
EXECUTE p_VA_NEXANS_24h_1h;
EXECUTE p_VA_NEXANS_24h_1h;
SELECT 'VA-NEXANS-24h-1h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VA_NEXANS_24h_1h;
SELECT '=== VA-NEXANS-24h-24h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(kumuliert.anzahl), MAX(CASE WHEN kumuliert.kumuliert >= 51 THEN kumuliert.stunde END)
FROM (SELECT je_stunde.stunde, je_stunde.anzahl,
             SUM(CASE WHEN je_stunde.voll THEN je_stunde.anzahl ELSE 0 END)
                 OVER (ORDER BY je_stunde.stunde DESC) AS kumuliert
      FROM (SELECT r.stunde AS stunde, SUM(r.anzahl) AS anzahl,
                   (r.stunde >= ''2025-12-29 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
            FROM overlord_monitor.message_rollup r
            WHERE r.stunde >= ''2025-12-29 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
              AND r.stunde < ''2026-08-27 15:00:00''
              AND EXISTS (SELECT 1 FROM GlassfishDB.Process verengung_process
                          JOIN GlassfishDB.ProjectMandant
                            ON GlassfishDB.ProjectMandant.ProjectID = verengung_process.ProjectID
                          WHERE verengung_process.ProcessID = r.process_id
                            AND GlassfishDB.ProjectMandant.MandantID = ''NEXANS'')
            GROUP BY r.stunde) AS je_stunde) AS kumuliert';
PREPARE p_VA_NEXANS_24h_24h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VA_NEXANS_24h_24h;
EXECUTE p_VA_NEXANS_24h_24h;
EXECUTE p_VA_NEXANS_24h_24h;
EXECUTE p_VA_NEXANS_24h_24h;
EXECUTE p_VA_NEXANS_24h_24h;
EXECUTE p_VA_NEXANS_24h_24h;
SELECT 'VA-NEXANS-24h-24h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VA_NEXANS_24h_24h;

SELECT '########## SUTTONS ##########' AS marke;

SELECT '=== F0-SUTTONS-30d - Plan (L15) ===' AS marke;
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
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
PREPARE p_F0_SUTTONS_30d_pl FROM @s;
EXECUTE p_F0_SUTTONS_30d_pl;
DEALLOCATE PREPARE p_F0_SUTTONS_30d_pl;
SELECT '=== F0-SUTTONS-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
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
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t';
PREPARE p_F0_SUTTONS_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_F0_SUTTONS_30d;
EXECUTE p_F0_SUTTONS_30d;
EXECUTE p_F0_SUTTONS_30d;
EXECUTE p_F0_SUTTONS_30d;
EXECUTE p_F0_SUTTONS_30d;
EXECUTE p_F0_SUTTONS_30d;
SELECT 'F0-SUTTONS-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_F0_SUTTONS_30d;
SELECT '=== VA-SUTTONS-30d-1h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(kumuliert.anzahl), MAX(CASE WHEN kumuliert.kumuliert >= 51 THEN kumuliert.stunde END)
FROM (SELECT je_stunde.stunde, je_stunde.anzahl,
             SUM(CASE WHEN je_stunde.voll THEN je_stunde.anzahl ELSE 0 END)
                 OVER (ORDER BY je_stunde.stunde DESC) AS kumuliert
      FROM (SELECT r.stunde AS stunde, SUM(r.anzahl) AS anzahl,
                   (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
            FROM overlord_monitor.message_rollup r
            WHERE r.stunde >= ''2025-12-30 03:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
              AND r.stunde < ''2026-08-27 15:00:00''
              AND EXISTS (SELECT 1 FROM GlassfishDB.Process verengung_process
                          JOIN GlassfishDB.ProjectMandant
                            ON GlassfishDB.ProjectMandant.ProjectID = verengung_process.ProjectID
                          WHERE verengung_process.ProcessID = r.process_id
                            AND GlassfishDB.ProjectMandant.MandantID = ''SUTTONS'')
            GROUP BY r.stunde) AS je_stunde) AS kumuliert';
PREPARE p_VA_SUTTONS_30d_1h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VA_SUTTONS_30d_1h;
EXECUTE p_VA_SUTTONS_30d_1h;
EXECUTE p_VA_SUTTONS_30d_1h;
EXECUTE p_VA_SUTTONS_30d_1h;
EXECUTE p_VA_SUTTONS_30d_1h;
EXECUTE p_VA_SUTTONS_30d_1h;
SELECT 'VA-SUTTONS-30d-1h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VA_SUTTONS_30d_1h;
SELECT '=== VA-SUTTONS-30d-24h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(kumuliert.anzahl), MAX(CASE WHEN kumuliert.kumuliert >= 51 THEN kumuliert.stunde END)
FROM (SELECT je_stunde.stunde, je_stunde.anzahl,
             SUM(CASE WHEN je_stunde.voll THEN je_stunde.anzahl ELSE 0 END)
                 OVER (ORDER BY je_stunde.stunde DESC) AS kumuliert
      FROM (SELECT r.stunde AS stunde, SUM(r.anzahl) AS anzahl,
                   (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
            FROM overlord_monitor.message_rollup r
            WHERE r.stunde >= ''2025-12-29 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
              AND r.stunde < ''2026-08-27 15:00:00''
              AND EXISTS (SELECT 1 FROM GlassfishDB.Process verengung_process
                          JOIN GlassfishDB.ProjectMandant
                            ON GlassfishDB.ProjectMandant.ProjectID = verengung_process.ProjectID
                          WHERE verengung_process.ProcessID = r.process_id
                            AND GlassfishDB.ProjectMandant.MandantID = ''SUTTONS'')
            GROUP BY r.stunde) AS je_stunde) AS kumuliert';
PREPARE p_VA_SUTTONS_30d_24h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VA_SUTTONS_30d_24h;
EXECUTE p_VA_SUTTONS_30d_24h;
EXECUTE p_VA_SUTTONS_30d_24h;
EXECUTE p_VA_SUTTONS_30d_24h;
EXECUTE p_VA_SUTTONS_30d_24h;
EXECUTE p_VA_SUTTONS_30d_24h;
SELECT 'VA-SUTTONS-30d-24h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VA_SUTTONS_30d_24h;

SELECT '=== VA-SUTTONS-30d-30d - Plan (L15) ===' AS marke;
SET @s = CONCAT('EXPLAIN ', 'SELECT SUM(kumuliert.anzahl), MAX(CASE WHEN kumuliert.kumuliert >= 51 THEN kumuliert.stunde END)
FROM (SELECT je_stunde.stunde, je_stunde.anzahl,
             SUM(CASE WHEN je_stunde.voll THEN je_stunde.anzahl ELSE 0 END)
                 OVER (ORDER BY je_stunde.stunde DESC) AS kumuliert
      FROM (SELECT r.stunde AS stunde, SUM(r.anzahl) AS anzahl,
                   (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
            FROM overlord_monitor.message_rollup r
            WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
              AND r.stunde < ''2026-08-27 15:00:00''
              AND EXISTS (SELECT 1 FROM GlassfishDB.Process verengung_process
                          JOIN GlassfishDB.ProjectMandant
                            ON GlassfishDB.ProjectMandant.ProjectID = verengung_process.ProjectID
                          WHERE verengung_process.ProcessID = r.process_id
                            AND GlassfishDB.ProjectMandant.MandantID = ''SUTTONS'')
            GROUP BY r.stunde) AS je_stunde) AS kumuliert');
PREPARE p_VA_SUTTONS_30d_30d_pl FROM @s;
EXECUTE p_VA_SUTTONS_30d_30d_pl;
DEALLOCATE PREPARE p_VA_SUTTONS_30d_30d_pl;
SELECT '=== VA-SUTTONS-30d-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(kumuliert.anzahl), MAX(CASE WHEN kumuliert.kumuliert >= 51 THEN kumuliert.stunde END)
FROM (SELECT je_stunde.stunde, je_stunde.anzahl,
             SUM(CASE WHEN je_stunde.voll THEN je_stunde.anzahl ELSE 0 END)
                 OVER (ORDER BY je_stunde.stunde DESC) AS kumuliert
      FROM (SELECT r.stunde AS stunde, SUM(r.anzahl) AS anzahl,
                   (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
            FROM overlord_monitor.message_rollup r
            WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
              AND r.stunde < ''2026-08-27 15:00:00''
              AND EXISTS (SELECT 1 FROM GlassfishDB.Process verengung_process
                          JOIN GlassfishDB.ProjectMandant
                            ON GlassfishDB.ProjectMandant.ProjectID = verengung_process.ProjectID
                          WHERE verengung_process.ProcessID = r.process_id
                            AND GlassfishDB.ProjectMandant.MandantID = ''SUTTONS'')
            GROUP BY r.stunde) AS je_stunde) AS kumuliert';
PREPARE p_VA_SUTTONS_30d_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VA_SUTTONS_30d_30d;
EXECUTE p_VA_SUTTONS_30d_30d;
EXECUTE p_VA_SUTTONS_30d_30d;
EXECUTE p_VA_SUTTONS_30d_30d;
EXECUTE p_VA_SUTTONS_30d_30d;
EXECUTE p_VA_SUTTONS_30d_30d;
SELECT 'VA-SUTTONS-30d-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VA_SUTTONS_30d_30d;

SELECT '=== SUTTONS-30d - die Verengung ausgerechnet ===' AS marke;
SET @zeilen = NULL, @ug = NULL;
SET @s = 'SELECT SUM(kumuliert.anzahl), MAX(CASE WHEN kumuliert.kumuliert >= 51 THEN kumuliert.stunde END) INTO @zeilen, @ug
FROM (SELECT je_stunde.stunde, je_stunde.anzahl,
             SUM(CASE WHEN je_stunde.voll THEN je_stunde.anzahl ELSE 0 END)
                 OVER (ORDER BY je_stunde.stunde DESC) AS kumuliert
      FROM (SELECT r.stunde AS stunde, SUM(r.anzahl) AS anzahl,
                   (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
            FROM overlord_monitor.message_rollup r
            WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
              AND r.stunde < ''2026-08-27 15:00:00''
              AND EXISTS (SELECT 1 FROM GlassfishDB.Process verengung_process
                          JOIN GlassfishDB.ProjectMandant
                            ON GlassfishDB.ProjectMandant.ProjectID = verengung_process.ProjectID
                          WHERE verengung_process.ProcessID = r.process_id
                            AND GlassfishDB.ProjectMandant.MandantID = ''SUTTONS'')
            GROUP BY r.stunde) AS je_stunde) AS kumuliert';
PREPARE p_rechne FROM @s;
EXECUTE p_rechne;
DEALLOCATE PREPARE p_rechne;
SET @ug_wirksam = GREATEST('2025-11-30 04:09:47', COALESCE(@ug, '2025-11-30 04:09:47'));
SELECT 'SUTTONS-30d' AS fall, @zeilen AS zeilen_im_fenster, @ug AS untergrenze_roh,
       @ug_wirksam AS untergrenze,
       TIMESTAMPDIFF(HOUR, @ug_wirksam, '2025-12-30 04:09:47') AS fenster_stunden,
       (@zeilen IS NULL OR @zeilen = 0) AS nullfall, (@ug IS NULL) AS keine_verengung;

SELECT '=== VERENGT-SUTTONS-30d - Plan (L15) ===' AS marke;
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
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t'));
PREPARE p_VERENGT_SUTTONS_30d_pl FROM @s;
EXECUTE p_VERENGT_SUTTONS_30d_pl;
DEALLOCATE PREPARE p_VERENGT_SUTTONS_30d_pl;
SELECT '=== VERENGT-SUTTONS-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
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
PREPARE p_VERENGT_SUTTONS_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VERENGT_SUTTONS_30d;
EXECUTE p_VERENGT_SUTTONS_30d;
EXECUTE p_VERENGT_SUTTONS_30d;
EXECUTE p_VERENGT_SUTTONS_30d;
EXECUTE p_VERENGT_SUTTONS_30d;
EXECUTE p_VERENGT_SUTTONS_30d;
SELECT 'VERENGT-SUTTONS-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VERENGT_SUTTONS_30d;

SELECT '=== F0-SUTTONS-24h - Plan (L15) ===' AS marke;
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
  WHERE m.MessageLastUpdate >= ''2025-12-29 04:09:47''
    AND m.MessageLastUpdate <= ''2025-12-30 04:09:47''
    AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''SUTTONS'')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
PREPARE p_F0_SUTTONS_24h_pl FROM @s;
EXECUTE p_F0_SUTTONS_24h_pl;
DEALLOCATE PREPARE p_F0_SUTTONS_24h_pl;
SELECT '=== F0-SUTTONS-24h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
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
  WHERE m.MessageLastUpdate >= ''2025-12-29 04:09:47''
    AND m.MessageLastUpdate <= ''2025-12-30 04:09:47''
    AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''SUTTONS'')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t';
PREPARE p_F0_SUTTONS_24h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_F0_SUTTONS_24h;
EXECUTE p_F0_SUTTONS_24h;
EXECUTE p_F0_SUTTONS_24h;
EXECUTE p_F0_SUTTONS_24h;
EXECUTE p_F0_SUTTONS_24h;
EXECUTE p_F0_SUTTONS_24h;
SELECT 'F0-SUTTONS-24h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_F0_SUTTONS_24h;
SELECT '=== VA-SUTTONS-24h-1h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(kumuliert.anzahl), MAX(CASE WHEN kumuliert.kumuliert >= 51 THEN kumuliert.stunde END)
FROM (SELECT je_stunde.stunde, je_stunde.anzahl,
             SUM(CASE WHEN je_stunde.voll THEN je_stunde.anzahl ELSE 0 END)
                 OVER (ORDER BY je_stunde.stunde DESC) AS kumuliert
      FROM (SELECT r.stunde AS stunde, SUM(r.anzahl) AS anzahl,
                   (r.stunde >= ''2025-12-29 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
            FROM overlord_monitor.message_rollup r
            WHERE r.stunde >= ''2025-12-30 03:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
              AND r.stunde < ''2026-08-27 15:00:00''
              AND EXISTS (SELECT 1 FROM GlassfishDB.Process verengung_process
                          JOIN GlassfishDB.ProjectMandant
                            ON GlassfishDB.ProjectMandant.ProjectID = verengung_process.ProjectID
                          WHERE verengung_process.ProcessID = r.process_id
                            AND GlassfishDB.ProjectMandant.MandantID = ''SUTTONS'')
            GROUP BY r.stunde) AS je_stunde) AS kumuliert';
PREPARE p_VA_SUTTONS_24h_1h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VA_SUTTONS_24h_1h;
EXECUTE p_VA_SUTTONS_24h_1h;
EXECUTE p_VA_SUTTONS_24h_1h;
EXECUTE p_VA_SUTTONS_24h_1h;
EXECUTE p_VA_SUTTONS_24h_1h;
EXECUTE p_VA_SUTTONS_24h_1h;
SELECT 'VA-SUTTONS-24h-1h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VA_SUTTONS_24h_1h;
SELECT '=== VA-SUTTONS-24h-24h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(kumuliert.anzahl), MAX(CASE WHEN kumuliert.kumuliert >= 51 THEN kumuliert.stunde END)
FROM (SELECT je_stunde.stunde, je_stunde.anzahl,
             SUM(CASE WHEN je_stunde.voll THEN je_stunde.anzahl ELSE 0 END)
                 OVER (ORDER BY je_stunde.stunde DESC) AS kumuliert
      FROM (SELECT r.stunde AS stunde, SUM(r.anzahl) AS anzahl,
                   (r.stunde >= ''2025-12-29 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
            FROM overlord_monitor.message_rollup r
            WHERE r.stunde >= ''2025-12-29 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
              AND r.stunde < ''2026-08-27 15:00:00''
              AND EXISTS (SELECT 1 FROM GlassfishDB.Process verengung_process
                          JOIN GlassfishDB.ProjectMandant
                            ON GlassfishDB.ProjectMandant.ProjectID = verengung_process.ProjectID
                          WHERE verengung_process.ProcessID = r.process_id
                            AND GlassfishDB.ProjectMandant.MandantID = ''SUTTONS'')
            GROUP BY r.stunde) AS je_stunde) AS kumuliert';
PREPARE p_VA_SUTTONS_24h_24h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VA_SUTTONS_24h_24h;
EXECUTE p_VA_SUTTONS_24h_24h;
EXECUTE p_VA_SUTTONS_24h_24h;
EXECUTE p_VA_SUTTONS_24h_24h;
EXECUTE p_VA_SUTTONS_24h_24h;
EXECUTE p_VA_SUTTONS_24h_24h;
SELECT 'VA-SUTTONS-24h-24h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VA_SUTTONS_24h_24h;

SELECT '########## VOTG ##########' AS marke;

SELECT '=== F0-VOTG-30d - Plan (L15) ===' AS marke;
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
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''VOTG'')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
PREPARE p_F0_VOTG_30d_pl FROM @s;
EXECUTE p_F0_VOTG_30d_pl;
DEALLOCATE PREPARE p_F0_VOTG_30d_pl;
SELECT '=== F0-VOTG-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
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
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''VOTG'')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t';
PREPARE p_F0_VOTG_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_F0_VOTG_30d;
EXECUTE p_F0_VOTG_30d;
EXECUTE p_F0_VOTG_30d;
EXECUTE p_F0_VOTG_30d;
EXECUTE p_F0_VOTG_30d;
EXECUTE p_F0_VOTG_30d;
SELECT 'F0-VOTG-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_F0_VOTG_30d;
SELECT '=== VA-VOTG-30d-1h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(kumuliert.anzahl), MAX(CASE WHEN kumuliert.kumuliert >= 51 THEN kumuliert.stunde END)
FROM (SELECT je_stunde.stunde, je_stunde.anzahl,
             SUM(CASE WHEN je_stunde.voll THEN je_stunde.anzahl ELSE 0 END)
                 OVER (ORDER BY je_stunde.stunde DESC) AS kumuliert
      FROM (SELECT r.stunde AS stunde, SUM(r.anzahl) AS anzahl,
                   (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
            FROM overlord_monitor.message_rollup r
            WHERE r.stunde >= ''2025-12-30 03:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
              AND r.stunde < ''2026-08-27 15:00:00''
              AND EXISTS (SELECT 1 FROM GlassfishDB.Process verengung_process
                          JOIN GlassfishDB.ProjectMandant
                            ON GlassfishDB.ProjectMandant.ProjectID = verengung_process.ProjectID
                          WHERE verengung_process.ProcessID = r.process_id
                            AND GlassfishDB.ProjectMandant.MandantID = ''VOTG'')
            GROUP BY r.stunde) AS je_stunde) AS kumuliert';
PREPARE p_VA_VOTG_30d_1h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VA_VOTG_30d_1h;
EXECUTE p_VA_VOTG_30d_1h;
EXECUTE p_VA_VOTG_30d_1h;
EXECUTE p_VA_VOTG_30d_1h;
EXECUTE p_VA_VOTG_30d_1h;
EXECUTE p_VA_VOTG_30d_1h;
SELECT 'VA-VOTG-30d-1h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VA_VOTG_30d_1h;
SELECT '=== VA-VOTG-30d-24h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(kumuliert.anzahl), MAX(CASE WHEN kumuliert.kumuliert >= 51 THEN kumuliert.stunde END)
FROM (SELECT je_stunde.stunde, je_stunde.anzahl,
             SUM(CASE WHEN je_stunde.voll THEN je_stunde.anzahl ELSE 0 END)
                 OVER (ORDER BY je_stunde.stunde DESC) AS kumuliert
      FROM (SELECT r.stunde AS stunde, SUM(r.anzahl) AS anzahl,
                   (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
            FROM overlord_monitor.message_rollup r
            WHERE r.stunde >= ''2025-12-29 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
              AND r.stunde < ''2026-08-27 15:00:00''
              AND EXISTS (SELECT 1 FROM GlassfishDB.Process verengung_process
                          JOIN GlassfishDB.ProjectMandant
                            ON GlassfishDB.ProjectMandant.ProjectID = verengung_process.ProjectID
                          WHERE verengung_process.ProcessID = r.process_id
                            AND GlassfishDB.ProjectMandant.MandantID = ''VOTG'')
            GROUP BY r.stunde) AS je_stunde) AS kumuliert';
PREPARE p_VA_VOTG_30d_24h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VA_VOTG_30d_24h;
EXECUTE p_VA_VOTG_30d_24h;
EXECUTE p_VA_VOTG_30d_24h;
EXECUTE p_VA_VOTG_30d_24h;
EXECUTE p_VA_VOTG_30d_24h;
EXECUTE p_VA_VOTG_30d_24h;
SELECT 'VA-VOTG-30d-24h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VA_VOTG_30d_24h;

SELECT '=== VA-VOTG-30d-30d - Plan (L15) ===' AS marke;
SET @s = CONCAT('EXPLAIN ', 'SELECT SUM(kumuliert.anzahl), MAX(CASE WHEN kumuliert.kumuliert >= 51 THEN kumuliert.stunde END)
FROM (SELECT je_stunde.stunde, je_stunde.anzahl,
             SUM(CASE WHEN je_stunde.voll THEN je_stunde.anzahl ELSE 0 END)
                 OVER (ORDER BY je_stunde.stunde DESC) AS kumuliert
      FROM (SELECT r.stunde AS stunde, SUM(r.anzahl) AS anzahl,
                   (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
            FROM overlord_monitor.message_rollup r
            WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
              AND r.stunde < ''2026-08-27 15:00:00''
              AND EXISTS (SELECT 1 FROM GlassfishDB.Process verengung_process
                          JOIN GlassfishDB.ProjectMandant
                            ON GlassfishDB.ProjectMandant.ProjectID = verengung_process.ProjectID
                          WHERE verengung_process.ProcessID = r.process_id
                            AND GlassfishDB.ProjectMandant.MandantID = ''VOTG'')
            GROUP BY r.stunde) AS je_stunde) AS kumuliert');
PREPARE p_VA_VOTG_30d_30d_pl FROM @s;
EXECUTE p_VA_VOTG_30d_30d_pl;
DEALLOCATE PREPARE p_VA_VOTG_30d_30d_pl;
SELECT '=== VA-VOTG-30d-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(kumuliert.anzahl), MAX(CASE WHEN kumuliert.kumuliert >= 51 THEN kumuliert.stunde END)
FROM (SELECT je_stunde.stunde, je_stunde.anzahl,
             SUM(CASE WHEN je_stunde.voll THEN je_stunde.anzahl ELSE 0 END)
                 OVER (ORDER BY je_stunde.stunde DESC) AS kumuliert
      FROM (SELECT r.stunde AS stunde, SUM(r.anzahl) AS anzahl,
                   (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
            FROM overlord_monitor.message_rollup r
            WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
              AND r.stunde < ''2026-08-27 15:00:00''
              AND EXISTS (SELECT 1 FROM GlassfishDB.Process verengung_process
                          JOIN GlassfishDB.ProjectMandant
                            ON GlassfishDB.ProjectMandant.ProjectID = verengung_process.ProjectID
                          WHERE verengung_process.ProcessID = r.process_id
                            AND GlassfishDB.ProjectMandant.MandantID = ''VOTG'')
            GROUP BY r.stunde) AS je_stunde) AS kumuliert';
PREPARE p_VA_VOTG_30d_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VA_VOTG_30d_30d;
EXECUTE p_VA_VOTG_30d_30d;
EXECUTE p_VA_VOTG_30d_30d;
EXECUTE p_VA_VOTG_30d_30d;
EXECUTE p_VA_VOTG_30d_30d;
EXECUTE p_VA_VOTG_30d_30d;
SELECT 'VA-VOTG-30d-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VA_VOTG_30d_30d;

SELECT '=== VOTG-30d - die Verengung ausgerechnet ===' AS marke;
SET @zeilen = NULL, @ug = NULL;
SET @s = 'SELECT SUM(kumuliert.anzahl), MAX(CASE WHEN kumuliert.kumuliert >= 51 THEN kumuliert.stunde END) INTO @zeilen, @ug
FROM (SELECT je_stunde.stunde, je_stunde.anzahl,
             SUM(CASE WHEN je_stunde.voll THEN je_stunde.anzahl ELSE 0 END)
                 OVER (ORDER BY je_stunde.stunde DESC) AS kumuliert
      FROM (SELECT r.stunde AS stunde, SUM(r.anzahl) AS anzahl,
                   (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
            FROM overlord_monitor.message_rollup r
            WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
              AND r.stunde < ''2026-08-27 15:00:00''
              AND EXISTS (SELECT 1 FROM GlassfishDB.Process verengung_process
                          JOIN GlassfishDB.ProjectMandant
                            ON GlassfishDB.ProjectMandant.ProjectID = verengung_process.ProjectID
                          WHERE verengung_process.ProcessID = r.process_id
                            AND GlassfishDB.ProjectMandant.MandantID = ''VOTG'')
            GROUP BY r.stunde) AS je_stunde) AS kumuliert';
PREPARE p_rechne FROM @s;
EXECUTE p_rechne;
DEALLOCATE PREPARE p_rechne;
SET @ug_wirksam = GREATEST('2025-11-30 04:09:47', COALESCE(@ug, '2025-11-30 04:09:47'));
SELECT 'VOTG-30d' AS fall, @zeilen AS zeilen_im_fenster, @ug AS untergrenze_roh,
       @ug_wirksam AS untergrenze,
       TIMESTAMPDIFF(HOUR, @ug_wirksam, '2025-12-30 04:09:47') AS fenster_stunden,
       (@zeilen IS NULL OR @zeilen = 0) AS nullfall, (@ug IS NULL) AS keine_verengung;

SELECT '=== VERENGT-VOTG-30d - Plan (L15) ===' AS marke;
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
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''VOTG'')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t'));
PREPARE p_VERENGT_VOTG_30d_pl FROM @s;
EXECUTE p_VERENGT_VOTG_30d_pl;
DEALLOCATE PREPARE p_VERENGT_VOTG_30d_pl;
SELECT '=== VERENGT-VOTG-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
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
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''VOTG'')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
PREPARE p_VERENGT_VOTG_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VERENGT_VOTG_30d;
EXECUTE p_VERENGT_VOTG_30d;
EXECUTE p_VERENGT_VOTG_30d;
EXECUTE p_VERENGT_VOTG_30d;
EXECUTE p_VERENGT_VOTG_30d;
EXECUTE p_VERENGT_VOTG_30d;
SELECT 'VERENGT-VOTG-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VERENGT_VOTG_30d;

SELECT '=== F0-VOTG-24h - Plan (L15) ===' AS marke;
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
  WHERE m.MessageLastUpdate >= ''2025-12-29 04:09:47''
    AND m.MessageLastUpdate <= ''2025-12-30 04:09:47''
    AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''VOTG'')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
PREPARE p_F0_VOTG_24h_pl FROM @s;
EXECUTE p_F0_VOTG_24h_pl;
DEALLOCATE PREPARE p_F0_VOTG_24h_pl;
SELECT '=== F0-VOTG-24h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
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
  WHERE m.MessageLastUpdate >= ''2025-12-29 04:09:47''
    AND m.MessageLastUpdate <= ''2025-12-30 04:09:47''
    AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''VOTG'')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t';
PREPARE p_F0_VOTG_24h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_F0_VOTG_24h;
EXECUTE p_F0_VOTG_24h;
EXECUTE p_F0_VOTG_24h;
EXECUTE p_F0_VOTG_24h;
EXECUTE p_F0_VOTG_24h;
EXECUTE p_F0_VOTG_24h;
SELECT 'F0-VOTG-24h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_F0_VOTG_24h;
SELECT '=== VA-VOTG-24h-1h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(kumuliert.anzahl), MAX(CASE WHEN kumuliert.kumuliert >= 51 THEN kumuliert.stunde END)
FROM (SELECT je_stunde.stunde, je_stunde.anzahl,
             SUM(CASE WHEN je_stunde.voll THEN je_stunde.anzahl ELSE 0 END)
                 OVER (ORDER BY je_stunde.stunde DESC) AS kumuliert
      FROM (SELECT r.stunde AS stunde, SUM(r.anzahl) AS anzahl,
                   (r.stunde >= ''2025-12-29 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
            FROM overlord_monitor.message_rollup r
            WHERE r.stunde >= ''2025-12-30 03:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
              AND r.stunde < ''2026-08-27 15:00:00''
              AND EXISTS (SELECT 1 FROM GlassfishDB.Process verengung_process
                          JOIN GlassfishDB.ProjectMandant
                            ON GlassfishDB.ProjectMandant.ProjectID = verengung_process.ProjectID
                          WHERE verengung_process.ProcessID = r.process_id
                            AND GlassfishDB.ProjectMandant.MandantID = ''VOTG'')
            GROUP BY r.stunde) AS je_stunde) AS kumuliert';
PREPARE p_VA_VOTG_24h_1h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VA_VOTG_24h_1h;
EXECUTE p_VA_VOTG_24h_1h;
EXECUTE p_VA_VOTG_24h_1h;
EXECUTE p_VA_VOTG_24h_1h;
EXECUTE p_VA_VOTG_24h_1h;
EXECUTE p_VA_VOTG_24h_1h;
SELECT 'VA-VOTG-24h-1h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VA_VOTG_24h_1h;
SELECT '=== VA-VOTG-24h-24h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(kumuliert.anzahl), MAX(CASE WHEN kumuliert.kumuliert >= 51 THEN kumuliert.stunde END)
FROM (SELECT je_stunde.stunde, je_stunde.anzahl,
             SUM(CASE WHEN je_stunde.voll THEN je_stunde.anzahl ELSE 0 END)
                 OVER (ORDER BY je_stunde.stunde DESC) AS kumuliert
      FROM (SELECT r.stunde AS stunde, SUM(r.anzahl) AS anzahl,
                   (r.stunde >= ''2025-12-29 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
            FROM overlord_monitor.message_rollup r
            WHERE r.stunde >= ''2025-12-29 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
              AND r.stunde < ''2026-08-27 15:00:00''
              AND EXISTS (SELECT 1 FROM GlassfishDB.Process verengung_process
                          JOIN GlassfishDB.ProjectMandant
                            ON GlassfishDB.ProjectMandant.ProjectID = verengung_process.ProjectID
                          WHERE verengung_process.ProcessID = r.process_id
                            AND GlassfishDB.ProjectMandant.MandantID = ''VOTG'')
            GROUP BY r.stunde) AS je_stunde) AS kumuliert';
PREPARE p_VA_VOTG_24h_24h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VA_VOTG_24h_24h;
EXECUTE p_VA_VOTG_24h_24h;
EXECUTE p_VA_VOTG_24h_24h;
EXECUTE p_VA_VOTG_24h_24h;
EXECUTE p_VA_VOTG_24h_24h;
EXECUTE p_VA_VOTG_24h_24h;
SELECT 'VA-VOTG-24h-24h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VA_VOTG_24h_24h;

SELECT '########## IBIS ##########' AS marke;

SELECT '=== F0-IBIS-30d - Plan (L15) ===' AS marke;
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
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''IBIS'')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
PREPARE p_F0_IBIS_30d_pl FROM @s;
EXECUTE p_F0_IBIS_30d_pl;
DEALLOCATE PREPARE p_F0_IBIS_30d_pl;
SELECT '=== F0-IBIS-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
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
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''IBIS'')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t';
PREPARE p_F0_IBIS_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_F0_IBIS_30d;
EXECUTE p_F0_IBIS_30d;
EXECUTE p_F0_IBIS_30d;
EXECUTE p_F0_IBIS_30d;
EXECUTE p_F0_IBIS_30d;
EXECUTE p_F0_IBIS_30d;
SELECT 'F0-IBIS-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_F0_IBIS_30d;
SELECT '=== VA-IBIS-30d-1h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(kumuliert.anzahl), MAX(CASE WHEN kumuliert.kumuliert >= 51 THEN kumuliert.stunde END)
FROM (SELECT je_stunde.stunde, je_stunde.anzahl,
             SUM(CASE WHEN je_stunde.voll THEN je_stunde.anzahl ELSE 0 END)
                 OVER (ORDER BY je_stunde.stunde DESC) AS kumuliert
      FROM (SELECT r.stunde AS stunde, SUM(r.anzahl) AS anzahl,
                   (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
            FROM overlord_monitor.message_rollup r
            WHERE r.stunde >= ''2025-12-30 03:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
              AND r.stunde < ''2026-08-27 15:00:00''
              AND EXISTS (SELECT 1 FROM GlassfishDB.Process verengung_process
                          JOIN GlassfishDB.ProjectMandant
                            ON GlassfishDB.ProjectMandant.ProjectID = verengung_process.ProjectID
                          WHERE verengung_process.ProcessID = r.process_id
                            AND GlassfishDB.ProjectMandant.MandantID = ''IBIS'')
            GROUP BY r.stunde) AS je_stunde) AS kumuliert';
PREPARE p_VA_IBIS_30d_1h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VA_IBIS_30d_1h;
EXECUTE p_VA_IBIS_30d_1h;
EXECUTE p_VA_IBIS_30d_1h;
EXECUTE p_VA_IBIS_30d_1h;
EXECUTE p_VA_IBIS_30d_1h;
EXECUTE p_VA_IBIS_30d_1h;
SELECT 'VA-IBIS-30d-1h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VA_IBIS_30d_1h;
SELECT '=== VA-IBIS-30d-24h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(kumuliert.anzahl), MAX(CASE WHEN kumuliert.kumuliert >= 51 THEN kumuliert.stunde END)
FROM (SELECT je_stunde.stunde, je_stunde.anzahl,
             SUM(CASE WHEN je_stunde.voll THEN je_stunde.anzahl ELSE 0 END)
                 OVER (ORDER BY je_stunde.stunde DESC) AS kumuliert
      FROM (SELECT r.stunde AS stunde, SUM(r.anzahl) AS anzahl,
                   (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
            FROM overlord_monitor.message_rollup r
            WHERE r.stunde >= ''2025-12-29 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
              AND r.stunde < ''2026-08-27 15:00:00''
              AND EXISTS (SELECT 1 FROM GlassfishDB.Process verengung_process
                          JOIN GlassfishDB.ProjectMandant
                            ON GlassfishDB.ProjectMandant.ProjectID = verengung_process.ProjectID
                          WHERE verengung_process.ProcessID = r.process_id
                            AND GlassfishDB.ProjectMandant.MandantID = ''IBIS'')
            GROUP BY r.stunde) AS je_stunde) AS kumuliert';
PREPARE p_VA_IBIS_30d_24h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VA_IBIS_30d_24h;
EXECUTE p_VA_IBIS_30d_24h;
EXECUTE p_VA_IBIS_30d_24h;
EXECUTE p_VA_IBIS_30d_24h;
EXECUTE p_VA_IBIS_30d_24h;
EXECUTE p_VA_IBIS_30d_24h;
SELECT 'VA-IBIS-30d-24h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VA_IBIS_30d_24h;

SELECT '=== VA-IBIS-30d-30d - Plan (L15) ===' AS marke;
SET @s = CONCAT('EXPLAIN ', 'SELECT SUM(kumuliert.anzahl), MAX(CASE WHEN kumuliert.kumuliert >= 51 THEN kumuliert.stunde END)
FROM (SELECT je_stunde.stunde, je_stunde.anzahl,
             SUM(CASE WHEN je_stunde.voll THEN je_stunde.anzahl ELSE 0 END)
                 OVER (ORDER BY je_stunde.stunde DESC) AS kumuliert
      FROM (SELECT r.stunde AS stunde, SUM(r.anzahl) AS anzahl,
                   (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
            FROM overlord_monitor.message_rollup r
            WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
              AND r.stunde < ''2026-08-27 15:00:00''
              AND EXISTS (SELECT 1 FROM GlassfishDB.Process verengung_process
                          JOIN GlassfishDB.ProjectMandant
                            ON GlassfishDB.ProjectMandant.ProjectID = verengung_process.ProjectID
                          WHERE verengung_process.ProcessID = r.process_id
                            AND GlassfishDB.ProjectMandant.MandantID = ''IBIS'')
            GROUP BY r.stunde) AS je_stunde) AS kumuliert');
PREPARE p_VA_IBIS_30d_30d_pl FROM @s;
EXECUTE p_VA_IBIS_30d_30d_pl;
DEALLOCATE PREPARE p_VA_IBIS_30d_30d_pl;
SELECT '=== VA-IBIS-30d-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(kumuliert.anzahl), MAX(CASE WHEN kumuliert.kumuliert >= 51 THEN kumuliert.stunde END)
FROM (SELECT je_stunde.stunde, je_stunde.anzahl,
             SUM(CASE WHEN je_stunde.voll THEN je_stunde.anzahl ELSE 0 END)
                 OVER (ORDER BY je_stunde.stunde DESC) AS kumuliert
      FROM (SELECT r.stunde AS stunde, SUM(r.anzahl) AS anzahl,
                   (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
            FROM overlord_monitor.message_rollup r
            WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
              AND r.stunde < ''2026-08-27 15:00:00''
              AND EXISTS (SELECT 1 FROM GlassfishDB.Process verengung_process
                          JOIN GlassfishDB.ProjectMandant
                            ON GlassfishDB.ProjectMandant.ProjectID = verengung_process.ProjectID
                          WHERE verengung_process.ProcessID = r.process_id
                            AND GlassfishDB.ProjectMandant.MandantID = ''IBIS'')
            GROUP BY r.stunde) AS je_stunde) AS kumuliert';
PREPARE p_VA_IBIS_30d_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VA_IBIS_30d_30d;
EXECUTE p_VA_IBIS_30d_30d;
EXECUTE p_VA_IBIS_30d_30d;
EXECUTE p_VA_IBIS_30d_30d;
EXECUTE p_VA_IBIS_30d_30d;
EXECUTE p_VA_IBIS_30d_30d;
SELECT 'VA-IBIS-30d-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VA_IBIS_30d_30d;

SELECT '=== IBIS-30d - die Verengung ausgerechnet ===' AS marke;
SET @zeilen = NULL, @ug = NULL;
SET @s = 'SELECT SUM(kumuliert.anzahl), MAX(CASE WHEN kumuliert.kumuliert >= 51 THEN kumuliert.stunde END) INTO @zeilen, @ug
FROM (SELECT je_stunde.stunde, je_stunde.anzahl,
             SUM(CASE WHEN je_stunde.voll THEN je_stunde.anzahl ELSE 0 END)
                 OVER (ORDER BY je_stunde.stunde DESC) AS kumuliert
      FROM (SELECT r.stunde AS stunde, SUM(r.anzahl) AS anzahl,
                   (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
            FROM overlord_monitor.message_rollup r
            WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
              AND r.stunde < ''2026-08-27 15:00:00''
              AND EXISTS (SELECT 1 FROM GlassfishDB.Process verengung_process
                          JOIN GlassfishDB.ProjectMandant
                            ON GlassfishDB.ProjectMandant.ProjectID = verengung_process.ProjectID
                          WHERE verengung_process.ProcessID = r.process_id
                            AND GlassfishDB.ProjectMandant.MandantID = ''IBIS'')
            GROUP BY r.stunde) AS je_stunde) AS kumuliert';
PREPARE p_rechne FROM @s;
EXECUTE p_rechne;
DEALLOCATE PREPARE p_rechne;
SET @ug_wirksam = GREATEST('2025-11-30 04:09:47', COALESCE(@ug, '2025-11-30 04:09:47'));
SELECT 'IBIS-30d' AS fall, @zeilen AS zeilen_im_fenster, @ug AS untergrenze_roh,
       @ug_wirksam AS untergrenze,
       TIMESTAMPDIFF(HOUR, @ug_wirksam, '2025-12-30 04:09:47') AS fenster_stunden,
       (@zeilen IS NULL OR @zeilen = 0) AS nullfall, (@ug IS NULL) AS keine_verengung;

SELECT '=== VERENGT-IBIS-30d - Plan (L15) ===' AS marke;
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
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''IBIS'')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t'));
PREPARE p_VERENGT_IBIS_30d_pl FROM @s;
EXECUTE p_VERENGT_IBIS_30d_pl;
DEALLOCATE PREPARE p_VERENGT_IBIS_30d_pl;
SELECT '=== VERENGT-IBIS-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
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
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''IBIS'')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
PREPARE p_VERENGT_IBIS_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VERENGT_IBIS_30d;
EXECUTE p_VERENGT_IBIS_30d;
EXECUTE p_VERENGT_IBIS_30d;
EXECUTE p_VERENGT_IBIS_30d;
EXECUTE p_VERENGT_IBIS_30d;
EXECUTE p_VERENGT_IBIS_30d;
SELECT 'VERENGT-IBIS-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VERENGT_IBIS_30d;

SELECT '=== F0-IBIS-24h - Plan (L15) ===' AS marke;
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
  WHERE m.MessageLastUpdate >= ''2025-12-29 04:09:47''
    AND m.MessageLastUpdate <= ''2025-12-30 04:09:47''
    AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''IBIS'')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
PREPARE p_F0_IBIS_24h_pl FROM @s;
EXECUTE p_F0_IBIS_24h_pl;
DEALLOCATE PREPARE p_F0_IBIS_24h_pl;
SELECT '=== F0-IBIS-24h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
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
  WHERE m.MessageLastUpdate >= ''2025-12-29 04:09:47''
    AND m.MessageLastUpdate <= ''2025-12-30 04:09:47''
    AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''IBIS'')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t';
PREPARE p_F0_IBIS_24h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_F0_IBIS_24h;
EXECUTE p_F0_IBIS_24h;
EXECUTE p_F0_IBIS_24h;
EXECUTE p_F0_IBIS_24h;
EXECUTE p_F0_IBIS_24h;
EXECUTE p_F0_IBIS_24h;
SELECT 'F0-IBIS-24h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_F0_IBIS_24h;
SELECT '=== VA-IBIS-24h-1h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(kumuliert.anzahl), MAX(CASE WHEN kumuliert.kumuliert >= 51 THEN kumuliert.stunde END)
FROM (SELECT je_stunde.stunde, je_stunde.anzahl,
             SUM(CASE WHEN je_stunde.voll THEN je_stunde.anzahl ELSE 0 END)
                 OVER (ORDER BY je_stunde.stunde DESC) AS kumuliert
      FROM (SELECT r.stunde AS stunde, SUM(r.anzahl) AS anzahl,
                   (r.stunde >= ''2025-12-29 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
            FROM overlord_monitor.message_rollup r
            WHERE r.stunde >= ''2025-12-30 03:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
              AND r.stunde < ''2026-08-27 15:00:00''
              AND EXISTS (SELECT 1 FROM GlassfishDB.Process verengung_process
                          JOIN GlassfishDB.ProjectMandant
                            ON GlassfishDB.ProjectMandant.ProjectID = verengung_process.ProjectID
                          WHERE verengung_process.ProcessID = r.process_id
                            AND GlassfishDB.ProjectMandant.MandantID = ''IBIS'')
            GROUP BY r.stunde) AS je_stunde) AS kumuliert';
PREPARE p_VA_IBIS_24h_1h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VA_IBIS_24h_1h;
EXECUTE p_VA_IBIS_24h_1h;
EXECUTE p_VA_IBIS_24h_1h;
EXECUTE p_VA_IBIS_24h_1h;
EXECUTE p_VA_IBIS_24h_1h;
EXECUTE p_VA_IBIS_24h_1h;
SELECT 'VA-IBIS-24h-1h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VA_IBIS_24h_1h;
SELECT '=== VA-IBIS-24h-24h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(kumuliert.anzahl), MAX(CASE WHEN kumuliert.kumuliert >= 51 THEN kumuliert.stunde END)
FROM (SELECT je_stunde.stunde, je_stunde.anzahl,
             SUM(CASE WHEN je_stunde.voll THEN je_stunde.anzahl ELSE 0 END)
                 OVER (ORDER BY je_stunde.stunde DESC) AS kumuliert
      FROM (SELECT r.stunde AS stunde, SUM(r.anzahl) AS anzahl,
                   (r.stunde >= ''2025-12-29 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
            FROM overlord_monitor.message_rollup r
            WHERE r.stunde >= ''2025-12-29 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
              AND r.stunde < ''2026-08-27 15:00:00''
              AND EXISTS (SELECT 1 FROM GlassfishDB.Process verengung_process
                          JOIN GlassfishDB.ProjectMandant
                            ON GlassfishDB.ProjectMandant.ProjectID = verengung_process.ProjectID
                          WHERE verengung_process.ProcessID = r.process_id
                            AND GlassfishDB.ProjectMandant.MandantID = ''IBIS'')
            GROUP BY r.stunde) AS je_stunde) AS kumuliert';
PREPARE p_VA_IBIS_24h_24h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VA_IBIS_24h_24h;
EXECUTE p_VA_IBIS_24h_24h;
EXECUTE p_VA_IBIS_24h_24h;
EXECUTE p_VA_IBIS_24h_24h;
EXECUTE p_VA_IBIS_24h_24h;
EXECUTE p_VA_IBIS_24h_24h;
SELECT 'VA-IBIS-24h-24h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VA_IBIS_24h_24h;

SELECT '########## IBISGUS ##########' AS marke;

SELECT '=== F0-IBISGUS-30d - Plan (L15) ===' AS marke;
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
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''IBISGUS'')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
PREPARE p_F0_IBISGUS_30d_pl FROM @s;
EXECUTE p_F0_IBISGUS_30d_pl;
DEALLOCATE PREPARE p_F0_IBISGUS_30d_pl;
SELECT '=== F0-IBISGUS-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
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
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''IBISGUS'')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t';
PREPARE p_F0_IBISGUS_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_F0_IBISGUS_30d;
EXECUTE p_F0_IBISGUS_30d;
EXECUTE p_F0_IBISGUS_30d;
EXECUTE p_F0_IBISGUS_30d;
EXECUTE p_F0_IBISGUS_30d;
EXECUTE p_F0_IBISGUS_30d;
SELECT 'F0-IBISGUS-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_F0_IBISGUS_30d;
SELECT '=== VA-IBISGUS-30d-1h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(kumuliert.anzahl), MAX(CASE WHEN kumuliert.kumuliert >= 51 THEN kumuliert.stunde END)
FROM (SELECT je_stunde.stunde, je_stunde.anzahl,
             SUM(CASE WHEN je_stunde.voll THEN je_stunde.anzahl ELSE 0 END)
                 OVER (ORDER BY je_stunde.stunde DESC) AS kumuliert
      FROM (SELECT r.stunde AS stunde, SUM(r.anzahl) AS anzahl,
                   (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
            FROM overlord_monitor.message_rollup r
            WHERE r.stunde >= ''2025-12-30 03:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
              AND r.stunde < ''2026-08-27 15:00:00''
              AND EXISTS (SELECT 1 FROM GlassfishDB.Process verengung_process
                          JOIN GlassfishDB.ProjectMandant
                            ON GlassfishDB.ProjectMandant.ProjectID = verengung_process.ProjectID
                          WHERE verengung_process.ProcessID = r.process_id
                            AND GlassfishDB.ProjectMandant.MandantID = ''IBISGUS'')
            GROUP BY r.stunde) AS je_stunde) AS kumuliert';
PREPARE p_VA_IBISGUS_30d_1h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VA_IBISGUS_30d_1h;
EXECUTE p_VA_IBISGUS_30d_1h;
EXECUTE p_VA_IBISGUS_30d_1h;
EXECUTE p_VA_IBISGUS_30d_1h;
EXECUTE p_VA_IBISGUS_30d_1h;
EXECUTE p_VA_IBISGUS_30d_1h;
SELECT 'VA-IBISGUS-30d-1h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VA_IBISGUS_30d_1h;
SELECT '=== VA-IBISGUS-30d-24h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(kumuliert.anzahl), MAX(CASE WHEN kumuliert.kumuliert >= 51 THEN kumuliert.stunde END)
FROM (SELECT je_stunde.stunde, je_stunde.anzahl,
             SUM(CASE WHEN je_stunde.voll THEN je_stunde.anzahl ELSE 0 END)
                 OVER (ORDER BY je_stunde.stunde DESC) AS kumuliert
      FROM (SELECT r.stunde AS stunde, SUM(r.anzahl) AS anzahl,
                   (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
            FROM overlord_monitor.message_rollup r
            WHERE r.stunde >= ''2025-12-29 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
              AND r.stunde < ''2026-08-27 15:00:00''
              AND EXISTS (SELECT 1 FROM GlassfishDB.Process verengung_process
                          JOIN GlassfishDB.ProjectMandant
                            ON GlassfishDB.ProjectMandant.ProjectID = verengung_process.ProjectID
                          WHERE verengung_process.ProcessID = r.process_id
                            AND GlassfishDB.ProjectMandant.MandantID = ''IBISGUS'')
            GROUP BY r.stunde) AS je_stunde) AS kumuliert';
PREPARE p_VA_IBISGUS_30d_24h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VA_IBISGUS_30d_24h;
EXECUTE p_VA_IBISGUS_30d_24h;
EXECUTE p_VA_IBISGUS_30d_24h;
EXECUTE p_VA_IBISGUS_30d_24h;
EXECUTE p_VA_IBISGUS_30d_24h;
EXECUTE p_VA_IBISGUS_30d_24h;
SELECT 'VA-IBISGUS-30d-24h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VA_IBISGUS_30d_24h;

SELECT '=== VA-IBISGUS-30d-30d - Plan (L15) ===' AS marke;
SET @s = CONCAT('EXPLAIN ', 'SELECT SUM(kumuliert.anzahl), MAX(CASE WHEN kumuliert.kumuliert >= 51 THEN kumuliert.stunde END)
FROM (SELECT je_stunde.stunde, je_stunde.anzahl,
             SUM(CASE WHEN je_stunde.voll THEN je_stunde.anzahl ELSE 0 END)
                 OVER (ORDER BY je_stunde.stunde DESC) AS kumuliert
      FROM (SELECT r.stunde AS stunde, SUM(r.anzahl) AS anzahl,
                   (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
            FROM overlord_monitor.message_rollup r
            WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
              AND r.stunde < ''2026-08-27 15:00:00''
              AND EXISTS (SELECT 1 FROM GlassfishDB.Process verengung_process
                          JOIN GlassfishDB.ProjectMandant
                            ON GlassfishDB.ProjectMandant.ProjectID = verengung_process.ProjectID
                          WHERE verengung_process.ProcessID = r.process_id
                            AND GlassfishDB.ProjectMandant.MandantID = ''IBISGUS'')
            GROUP BY r.stunde) AS je_stunde) AS kumuliert');
PREPARE p_VA_IBISGUS_30d_30d_pl FROM @s;
EXECUTE p_VA_IBISGUS_30d_30d_pl;
DEALLOCATE PREPARE p_VA_IBISGUS_30d_30d_pl;
SELECT '=== VA-IBISGUS-30d-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(kumuliert.anzahl), MAX(CASE WHEN kumuliert.kumuliert >= 51 THEN kumuliert.stunde END)
FROM (SELECT je_stunde.stunde, je_stunde.anzahl,
             SUM(CASE WHEN je_stunde.voll THEN je_stunde.anzahl ELSE 0 END)
                 OVER (ORDER BY je_stunde.stunde DESC) AS kumuliert
      FROM (SELECT r.stunde AS stunde, SUM(r.anzahl) AS anzahl,
                   (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
            FROM overlord_monitor.message_rollup r
            WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
              AND r.stunde < ''2026-08-27 15:00:00''
              AND EXISTS (SELECT 1 FROM GlassfishDB.Process verengung_process
                          JOIN GlassfishDB.ProjectMandant
                            ON GlassfishDB.ProjectMandant.ProjectID = verengung_process.ProjectID
                          WHERE verengung_process.ProcessID = r.process_id
                            AND GlassfishDB.ProjectMandant.MandantID = ''IBISGUS'')
            GROUP BY r.stunde) AS je_stunde) AS kumuliert';
PREPARE p_VA_IBISGUS_30d_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VA_IBISGUS_30d_30d;
EXECUTE p_VA_IBISGUS_30d_30d;
EXECUTE p_VA_IBISGUS_30d_30d;
EXECUTE p_VA_IBISGUS_30d_30d;
EXECUTE p_VA_IBISGUS_30d_30d;
EXECUTE p_VA_IBISGUS_30d_30d;
SELECT 'VA-IBISGUS-30d-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VA_IBISGUS_30d_30d;

SELECT '=== IBISGUS-30d - die Verengung ausgerechnet ===' AS marke;
SET @zeilen = NULL, @ug = NULL;
SET @s = 'SELECT SUM(kumuliert.anzahl), MAX(CASE WHEN kumuliert.kumuliert >= 51 THEN kumuliert.stunde END) INTO @zeilen, @ug
FROM (SELECT je_stunde.stunde, je_stunde.anzahl,
             SUM(CASE WHEN je_stunde.voll THEN je_stunde.anzahl ELSE 0 END)
                 OVER (ORDER BY je_stunde.stunde DESC) AS kumuliert
      FROM (SELECT r.stunde AS stunde, SUM(r.anzahl) AS anzahl,
                   (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
            FROM overlord_monitor.message_rollup r
            WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
              AND r.stunde < ''2026-08-27 15:00:00''
              AND EXISTS (SELECT 1 FROM GlassfishDB.Process verengung_process
                          JOIN GlassfishDB.ProjectMandant
                            ON GlassfishDB.ProjectMandant.ProjectID = verengung_process.ProjectID
                          WHERE verengung_process.ProcessID = r.process_id
                            AND GlassfishDB.ProjectMandant.MandantID = ''IBISGUS'')
            GROUP BY r.stunde) AS je_stunde) AS kumuliert';
PREPARE p_rechne FROM @s;
EXECUTE p_rechne;
DEALLOCATE PREPARE p_rechne;
SET @ug_wirksam = GREATEST('2025-11-30 04:09:47', COALESCE(@ug, '2025-11-30 04:09:47'));
SELECT 'IBISGUS-30d' AS fall, @zeilen AS zeilen_im_fenster, @ug AS untergrenze_roh,
       @ug_wirksam AS untergrenze,
       TIMESTAMPDIFF(HOUR, @ug_wirksam, '2025-12-30 04:09:47') AS fenster_stunden,
       (@zeilen IS NULL OR @zeilen = 0) AS nullfall, (@ug IS NULL) AS keine_verengung;

SELECT '=== VERENGT-IBISGUS-30d - Plan (L15) ===' AS marke;
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
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''IBISGUS'')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t'));
PREPARE p_VERENGT_IBISGUS_30d_pl FROM @s;
EXECUTE p_VERENGT_IBISGUS_30d_pl;
DEALLOCATE PREPARE p_VERENGT_IBISGUS_30d_pl;
SELECT '=== VERENGT-IBISGUS-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
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
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''IBISGUS'')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
PREPARE p_VERENGT_IBISGUS_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VERENGT_IBISGUS_30d;
EXECUTE p_VERENGT_IBISGUS_30d;
EXECUTE p_VERENGT_IBISGUS_30d;
EXECUTE p_VERENGT_IBISGUS_30d;
EXECUTE p_VERENGT_IBISGUS_30d;
EXECUTE p_VERENGT_IBISGUS_30d;
SELECT 'VERENGT-IBISGUS-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VERENGT_IBISGUS_30d;

SELECT '=== F0-IBISGUS-24h - Plan (L15) ===' AS marke;
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
  WHERE m.MessageLastUpdate >= ''2025-12-29 04:09:47''
    AND m.MessageLastUpdate <= ''2025-12-30 04:09:47''
    AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''IBISGUS'')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t');
PREPARE p_F0_IBISGUS_24h_pl FROM @s;
EXECUTE p_F0_IBISGUS_24h_pl;
DEALLOCATE PREPARE p_F0_IBISGUS_24h_pl;
SELECT '=== F0-IBISGUS-24h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
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
  WHERE m.MessageLastUpdate >= ''2025-12-29 04:09:47''
    AND m.MessageLastUpdate <= ''2025-12-30 04:09:47''
    AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ''IBISGUS'')
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  LIMIT 51
) t';
PREPARE p_F0_IBISGUS_24h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_F0_IBISGUS_24h;
EXECUTE p_F0_IBISGUS_24h;
EXECUTE p_F0_IBISGUS_24h;
EXECUTE p_F0_IBISGUS_24h;
EXECUTE p_F0_IBISGUS_24h;
EXECUTE p_F0_IBISGUS_24h;
SELECT 'F0-IBISGUS-24h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_F0_IBISGUS_24h;
SELECT '=== VA-IBISGUS-24h-1h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(kumuliert.anzahl), MAX(CASE WHEN kumuliert.kumuliert >= 51 THEN kumuliert.stunde END)
FROM (SELECT je_stunde.stunde, je_stunde.anzahl,
             SUM(CASE WHEN je_stunde.voll THEN je_stunde.anzahl ELSE 0 END)
                 OVER (ORDER BY je_stunde.stunde DESC) AS kumuliert
      FROM (SELECT r.stunde AS stunde, SUM(r.anzahl) AS anzahl,
                   (r.stunde >= ''2025-12-29 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
            FROM overlord_monitor.message_rollup r
            WHERE r.stunde >= ''2025-12-30 03:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
              AND r.stunde < ''2026-08-27 15:00:00''
              AND EXISTS (SELECT 1 FROM GlassfishDB.Process verengung_process
                          JOIN GlassfishDB.ProjectMandant
                            ON GlassfishDB.ProjectMandant.ProjectID = verengung_process.ProjectID
                          WHERE verengung_process.ProcessID = r.process_id
                            AND GlassfishDB.ProjectMandant.MandantID = ''IBISGUS'')
            GROUP BY r.stunde) AS je_stunde) AS kumuliert';
PREPARE p_VA_IBISGUS_24h_1h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VA_IBISGUS_24h_1h;
EXECUTE p_VA_IBISGUS_24h_1h;
EXECUTE p_VA_IBISGUS_24h_1h;
EXECUTE p_VA_IBISGUS_24h_1h;
EXECUTE p_VA_IBISGUS_24h_1h;
EXECUTE p_VA_IBISGUS_24h_1h;
SELECT 'VA-IBISGUS-24h-1h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VA_IBISGUS_24h_1h;
SELECT '=== VA-IBISGUS-24h-24h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(kumuliert.anzahl), MAX(CASE WHEN kumuliert.kumuliert >= 51 THEN kumuliert.stunde END)
FROM (SELECT je_stunde.stunde, je_stunde.anzahl,
             SUM(CASE WHEN je_stunde.voll THEN je_stunde.anzahl ELSE 0 END)
                 OVER (ORDER BY je_stunde.stunde DESC) AS kumuliert
      FROM (SELECT r.stunde AS stunde, SUM(r.anzahl) AS anzahl,
                   (r.stunde >= ''2025-12-29 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
            FROM overlord_monitor.message_rollup r
            WHERE r.stunde >= ''2025-12-29 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
              AND r.stunde < ''2026-08-27 15:00:00''
              AND EXISTS (SELECT 1 FROM GlassfishDB.Process verengung_process
                          JOIN GlassfishDB.ProjectMandant
                            ON GlassfishDB.ProjectMandant.ProjectID = verengung_process.ProjectID
                          WHERE verengung_process.ProcessID = r.process_id
                            AND GlassfishDB.ProjectMandant.MandantID = ''IBISGUS'')
            GROUP BY r.stunde) AS je_stunde) AS kumuliert';
PREPARE p_VA_IBISGUS_24h_24h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VA_IBISGUS_24h_24h;
EXECUTE p_VA_IBISGUS_24h_24h;
EXECUTE p_VA_IBISGUS_24h_24h;
EXECUTE p_VA_IBISGUS_24h_24h;
EXECUTE p_VA_IBISGUS_24h_24h;
EXECUTE p_VA_IBISGUS_24h_24h;
SELECT 'VA-IBISGUS-24h-24h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VA_IBISGUS_24h_24h;

SELECT '=== 99 Abschluss ===' AS marke;
SELECT @@global.read_only AS read_only_am_ende, NOW() AS serverzeit_ende;
