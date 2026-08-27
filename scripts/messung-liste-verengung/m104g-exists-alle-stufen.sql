-- Messung M104 g - Die EXISTS-Fassung ueber alle Stufen, gegen die indizierte Probe
-- Auftrag:  "Fensterverengung fuer die Nachrichtenliste", Stand 27.08.2026
-- Ergebnis: docs/messungen-liste-verengung.md
--
-- M104 g - Die fehlende Zelle: EXISTS-Kette ueber alle vier Stufen gegen overlord_monitor.message_rollup_probe_idx,
-- die den Index (process_id, stunde) traegt. Die EXISTS-Fassung braucht keine Prozessliste
-- und damit weder deren Beschaffung noch das Parsen eines 19-KB-Statements - der Aufschlag,
-- an dem die Literalfassung bei NEXANS haengt (Nachtrag m99b). Zusammen mit m99c (dieselbe
-- Fassung, dieselben Stufen, aber gegen die Tabelle OHNE Index) ergibt das die vollstaendige
-- Matrix.
-- Benutzer: monitor_read, ausschliesslich lesend.
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

SELECT '=== Zustand der Probetabelle ===' AS marke;
SHOW INDEX FROM overlord_monitor.message_rollup_probe_idx;

SELECT '########## NEXANS ##########' AS marke;

SELECT '=== PX-NEXANS-1h - Plan (L15) ===' AS marke;
SET @s = CONCAT('EXPLAIN ', 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-30 03:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''NEXANS'')
              GROUP BY r.stunde ) s ) g');
PREPARE p_PX_NEXANS_1h_pl FROM @s;
EXECUTE p_PX_NEXANS_1h_pl;
DEALLOCATE PREPARE p_PX_NEXANS_1h_pl;
SELECT '=== PX-NEXANS-1h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-30 03:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''NEXANS'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_PX_NEXANS_1h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_PX_NEXANS_1h;
EXECUTE p_PX_NEXANS_1h;
EXECUTE p_PX_NEXANS_1h;
EXECUTE p_PX_NEXANS_1h;
EXECUTE p_PX_NEXANS_1h;
EXECUTE p_PX_NEXANS_1h;
SELECT 'PX-NEXANS-1h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_PX_NEXANS_1h;
SELECT '=== PX-NEXANS-24h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-29 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''NEXANS'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_PX_NEXANS_24h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_PX_NEXANS_24h;
EXECUTE p_PX_NEXANS_24h;
EXECUTE p_PX_NEXANS_24h;
EXECUTE p_PX_NEXANS_24h;
EXECUTE p_PX_NEXANS_24h;
EXECUTE p_PX_NEXANS_24h;
SELECT 'PX-NEXANS-24h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_PX_NEXANS_24h;
SELECT '=== PX-NEXANS-7d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-23 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''NEXANS'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_PX_NEXANS_7d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_PX_NEXANS_7d;
EXECUTE p_PX_NEXANS_7d;
EXECUTE p_PX_NEXANS_7d;
EXECUTE p_PX_NEXANS_7d;
EXECUTE p_PX_NEXANS_7d;
EXECUTE p_PX_NEXANS_7d;
SELECT 'PX-NEXANS-7d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_PX_NEXANS_7d;

SELECT '=== PX-NEXANS-30d - Plan (L15) ===' AS marke;
SET @s = CONCAT('EXPLAIN ', 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''NEXANS'')
              GROUP BY r.stunde ) s ) g');
PREPARE p_PX_NEXANS_30d_pl FROM @s;
EXECUTE p_PX_NEXANS_30d_pl;
DEALLOCATE PREPARE p_PX_NEXANS_30d_pl;
SELECT '=== PX-NEXANS-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''NEXANS'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_PX_NEXANS_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_PX_NEXANS_30d;
EXECUTE p_PX_NEXANS_30d;
EXECUTE p_PX_NEXANS_30d;
EXECUTE p_PX_NEXANS_30d;
EXECUTE p_PX_NEXANS_30d;
EXECUTE p_PX_NEXANS_30d;
SELECT 'PX-NEXANS-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_PX_NEXANS_30d;

SELECT '########## SUTTONS ##########' AS marke;

SELECT '=== PX-SUTTONS-1h - Plan (L15) ===' AS marke;
SET @s = CONCAT('EXPLAIN ', 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-30 03:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''SUTTONS'')
              GROUP BY r.stunde ) s ) g');
PREPARE p_PX_SUTTONS_1h_pl FROM @s;
EXECUTE p_PX_SUTTONS_1h_pl;
DEALLOCATE PREPARE p_PX_SUTTONS_1h_pl;
SELECT '=== PX-SUTTONS-1h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-30 03:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''SUTTONS'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_PX_SUTTONS_1h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_PX_SUTTONS_1h;
EXECUTE p_PX_SUTTONS_1h;
EXECUTE p_PX_SUTTONS_1h;
EXECUTE p_PX_SUTTONS_1h;
EXECUTE p_PX_SUTTONS_1h;
EXECUTE p_PX_SUTTONS_1h;
SELECT 'PX-SUTTONS-1h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_PX_SUTTONS_1h;
SELECT '=== PX-SUTTONS-24h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-29 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''SUTTONS'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_PX_SUTTONS_24h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_PX_SUTTONS_24h;
EXECUTE p_PX_SUTTONS_24h;
EXECUTE p_PX_SUTTONS_24h;
EXECUTE p_PX_SUTTONS_24h;
EXECUTE p_PX_SUTTONS_24h;
EXECUTE p_PX_SUTTONS_24h;
SELECT 'PX-SUTTONS-24h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_PX_SUTTONS_24h;
SELECT '=== PX-SUTTONS-7d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-23 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''SUTTONS'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_PX_SUTTONS_7d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_PX_SUTTONS_7d;
EXECUTE p_PX_SUTTONS_7d;
EXECUTE p_PX_SUTTONS_7d;
EXECUTE p_PX_SUTTONS_7d;
EXECUTE p_PX_SUTTONS_7d;
EXECUTE p_PX_SUTTONS_7d;
SELECT 'PX-SUTTONS-7d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_PX_SUTTONS_7d;

SELECT '=== PX-SUTTONS-30d - Plan (L15) ===' AS marke;
SET @s = CONCAT('EXPLAIN ', 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''SUTTONS'')
              GROUP BY r.stunde ) s ) g');
PREPARE p_PX_SUTTONS_30d_pl FROM @s;
EXECUTE p_PX_SUTTONS_30d_pl;
DEALLOCATE PREPARE p_PX_SUTTONS_30d_pl;
SELECT '=== PX-SUTTONS-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''SUTTONS'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_PX_SUTTONS_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_PX_SUTTONS_30d;
EXECUTE p_PX_SUTTONS_30d;
EXECUTE p_PX_SUTTONS_30d;
EXECUTE p_PX_SUTTONS_30d;
EXECUTE p_PX_SUTTONS_30d;
EXECUTE p_PX_SUTTONS_30d;
SELECT 'PX-SUTTONS-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_PX_SUTTONS_30d;

SELECT '########## VOTG ##########' AS marke;

SELECT '=== PX-VOTG-1h - Plan (L15) ===' AS marke;
SET @s = CONCAT('EXPLAIN ', 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-30 03:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''VOTG'')
              GROUP BY r.stunde ) s ) g');
PREPARE p_PX_VOTG_1h_pl FROM @s;
EXECUTE p_PX_VOTG_1h_pl;
DEALLOCATE PREPARE p_PX_VOTG_1h_pl;
SELECT '=== PX-VOTG-1h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-30 03:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''VOTG'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_PX_VOTG_1h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_PX_VOTG_1h;
EXECUTE p_PX_VOTG_1h;
EXECUTE p_PX_VOTG_1h;
EXECUTE p_PX_VOTG_1h;
EXECUTE p_PX_VOTG_1h;
EXECUTE p_PX_VOTG_1h;
SELECT 'PX-VOTG-1h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_PX_VOTG_1h;
SELECT '=== PX-VOTG-24h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-29 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''VOTG'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_PX_VOTG_24h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_PX_VOTG_24h;
EXECUTE p_PX_VOTG_24h;
EXECUTE p_PX_VOTG_24h;
EXECUTE p_PX_VOTG_24h;
EXECUTE p_PX_VOTG_24h;
EXECUTE p_PX_VOTG_24h;
SELECT 'PX-VOTG-24h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_PX_VOTG_24h;
SELECT '=== PX-VOTG-7d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-23 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''VOTG'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_PX_VOTG_7d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_PX_VOTG_7d;
EXECUTE p_PX_VOTG_7d;
EXECUTE p_PX_VOTG_7d;
EXECUTE p_PX_VOTG_7d;
EXECUTE p_PX_VOTG_7d;
EXECUTE p_PX_VOTG_7d;
SELECT 'PX-VOTG-7d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_PX_VOTG_7d;

SELECT '=== PX-VOTG-30d - Plan (L15) ===' AS marke;
SET @s = CONCAT('EXPLAIN ', 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''VOTG'')
              GROUP BY r.stunde ) s ) g');
PREPARE p_PX_VOTG_30d_pl FROM @s;
EXECUTE p_PX_VOTG_30d_pl;
DEALLOCATE PREPARE p_PX_VOTG_30d_pl;
SELECT '=== PX-VOTG-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''VOTG'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_PX_VOTG_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_PX_VOTG_30d;
EXECUTE p_PX_VOTG_30d;
EXECUTE p_PX_VOTG_30d;
EXECUTE p_PX_VOTG_30d;
EXECUTE p_PX_VOTG_30d;
EXECUTE p_PX_VOTG_30d;
SELECT 'PX-VOTG-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_PX_VOTG_30d;

SELECT '########## IBIS ##########' AS marke;

SELECT '=== PX-IBIS-1h - Plan (L15) ===' AS marke;
SET @s = CONCAT('EXPLAIN ', 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-30 03:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''IBIS'')
              GROUP BY r.stunde ) s ) g');
PREPARE p_PX_IBIS_1h_pl FROM @s;
EXECUTE p_PX_IBIS_1h_pl;
DEALLOCATE PREPARE p_PX_IBIS_1h_pl;
SELECT '=== PX-IBIS-1h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-30 03:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''IBIS'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_PX_IBIS_1h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_PX_IBIS_1h;
EXECUTE p_PX_IBIS_1h;
EXECUTE p_PX_IBIS_1h;
EXECUTE p_PX_IBIS_1h;
EXECUTE p_PX_IBIS_1h;
EXECUTE p_PX_IBIS_1h;
SELECT 'PX-IBIS-1h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_PX_IBIS_1h;
SELECT '=== PX-IBIS-24h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-29 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''IBIS'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_PX_IBIS_24h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_PX_IBIS_24h;
EXECUTE p_PX_IBIS_24h;
EXECUTE p_PX_IBIS_24h;
EXECUTE p_PX_IBIS_24h;
EXECUTE p_PX_IBIS_24h;
EXECUTE p_PX_IBIS_24h;
SELECT 'PX-IBIS-24h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_PX_IBIS_24h;
SELECT '=== PX-IBIS-7d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-23 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''IBIS'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_PX_IBIS_7d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_PX_IBIS_7d;
EXECUTE p_PX_IBIS_7d;
EXECUTE p_PX_IBIS_7d;
EXECUTE p_PX_IBIS_7d;
EXECUTE p_PX_IBIS_7d;
EXECUTE p_PX_IBIS_7d;
SELECT 'PX-IBIS-7d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_PX_IBIS_7d;

SELECT '=== PX-IBIS-30d - Plan (L15) ===' AS marke;
SET @s = CONCAT('EXPLAIN ', 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''IBIS'')
              GROUP BY r.stunde ) s ) g');
PREPARE p_PX_IBIS_30d_pl FROM @s;
EXECUTE p_PX_IBIS_30d_pl;
DEALLOCATE PREPARE p_PX_IBIS_30d_pl;
SELECT '=== PX-IBIS-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''IBIS'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_PX_IBIS_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_PX_IBIS_30d;
EXECUTE p_PX_IBIS_30d;
EXECUTE p_PX_IBIS_30d;
EXECUTE p_PX_IBIS_30d;
EXECUTE p_PX_IBIS_30d;
EXECUTE p_PX_IBIS_30d;
SELECT 'PX-IBIS-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_PX_IBIS_30d;

SELECT '########## IBISGUS ##########' AS marke;

SELECT '=== PX-IBISGUS-1h - Plan (L15) ===' AS marke;
SET @s = CONCAT('EXPLAIN ', 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-30 03:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''IBISGUS'')
              GROUP BY r.stunde ) s ) g');
PREPARE p_PX_IBISGUS_1h_pl FROM @s;
EXECUTE p_PX_IBISGUS_1h_pl;
DEALLOCATE PREPARE p_PX_IBISGUS_1h_pl;
SELECT '=== PX-IBISGUS-1h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-30 03:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''IBISGUS'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_PX_IBISGUS_1h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_PX_IBISGUS_1h;
EXECUTE p_PX_IBISGUS_1h;
EXECUTE p_PX_IBISGUS_1h;
EXECUTE p_PX_IBISGUS_1h;
EXECUTE p_PX_IBISGUS_1h;
EXECUTE p_PX_IBISGUS_1h;
SELECT 'PX-IBISGUS-1h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_PX_IBISGUS_1h;
SELECT '=== PX-IBISGUS-24h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-29 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''IBISGUS'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_PX_IBISGUS_24h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_PX_IBISGUS_24h;
EXECUTE p_PX_IBISGUS_24h;
EXECUTE p_PX_IBISGUS_24h;
EXECUTE p_PX_IBISGUS_24h;
EXECUTE p_PX_IBISGUS_24h;
EXECUTE p_PX_IBISGUS_24h;
SELECT 'PX-IBISGUS-24h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_PX_IBISGUS_24h;
SELECT '=== PX-IBISGUS-7d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-23 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''IBISGUS'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_PX_IBISGUS_7d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_PX_IBISGUS_7d;
EXECUTE p_PX_IBISGUS_7d;
EXECUTE p_PX_IBISGUS_7d;
EXECUTE p_PX_IBISGUS_7d;
EXECUTE p_PX_IBISGUS_7d;
EXECUTE p_PX_IBISGUS_7d;
SELECT 'PX-IBISGUS-7d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_PX_IBISGUS_7d;

SELECT '=== PX-IBISGUS-30d - Plan (L15) ===' AS marke;
SET @s = CONCAT('EXPLAIN ', 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''IBISGUS'')
              GROUP BY r.stunde ) s ) g');
PREPARE p_PX_IBISGUS_30d_pl FROM @s;
EXECUTE p_PX_IBISGUS_30d_pl;
DEALLOCATE PREPARE p_PX_IBISGUS_30d_pl;
SELECT '=== PX-IBISGUS-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''IBISGUS'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_PX_IBISGUS_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_PX_IBISGUS_30d;
EXECUTE p_PX_IBISGUS_30d;
EXECUTE p_PX_IBISGUS_30d;
EXECUTE p_PX_IBISGUS_30d;
EXECUTE p_PX_IBISGUS_30d;
EXECUTE p_PX_IBISGUS_30d;
SELECT 'PX-IBISGUS-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_PX_IBISGUS_30d;

SELECT '########## ZAST ##########' AS marke;

SELECT '=== PX-ZAST-1h - Plan (L15) ===' AS marke;
SET @s = CONCAT('EXPLAIN ', 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-30 03:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''ZAST'')
              GROUP BY r.stunde ) s ) g');
PREPARE p_PX_ZAST_1h_pl FROM @s;
EXECUTE p_PX_ZAST_1h_pl;
DEALLOCATE PREPARE p_PX_ZAST_1h_pl;
SELECT '=== PX-ZAST-1h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-30 03:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''ZAST'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_PX_ZAST_1h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_PX_ZAST_1h;
EXECUTE p_PX_ZAST_1h;
EXECUTE p_PX_ZAST_1h;
EXECUTE p_PX_ZAST_1h;
EXECUTE p_PX_ZAST_1h;
EXECUTE p_PX_ZAST_1h;
SELECT 'PX-ZAST-1h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_PX_ZAST_1h;
SELECT '=== PX-ZAST-24h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-29 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''ZAST'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_PX_ZAST_24h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_PX_ZAST_24h;
EXECUTE p_PX_ZAST_24h;
EXECUTE p_PX_ZAST_24h;
EXECUTE p_PX_ZAST_24h;
EXECUTE p_PX_ZAST_24h;
EXECUTE p_PX_ZAST_24h;
SELECT 'PX-ZAST-24h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_PX_ZAST_24h;
SELECT '=== PX-ZAST-7d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-23 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''ZAST'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_PX_ZAST_7d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_PX_ZAST_7d;
EXECUTE p_PX_ZAST_7d;
EXECUTE p_PX_ZAST_7d;
EXECUTE p_PX_ZAST_7d;
EXECUTE p_PX_ZAST_7d;
EXECUTE p_PX_ZAST_7d;
SELECT 'PX-ZAST-7d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_PX_ZAST_7d;

SELECT '=== PX-ZAST-30d - Plan (L15) ===' AS marke;
SET @s = CONCAT('EXPLAIN ', 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''ZAST'')
              GROUP BY r.stunde ) s ) g');
PREPARE p_PX_ZAST_30d_pl FROM @s;
EXECUTE p_PX_ZAST_30d_pl;
DEALLOCATE PREPARE p_PX_ZAST_30d_pl;
SELECT '=== PX-ZAST-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''ZAST'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_PX_ZAST_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_PX_ZAST_30d;
EXECUTE p_PX_ZAST_30d;
EXECUTE p_PX_ZAST_30d;
EXECUTE p_PX_ZAST_30d;
EXECUTE p_PX_ZAST_30d;
EXECUTE p_PX_ZAST_30d;
SELECT 'PX-ZAST-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_PX_ZAST_30d;

SELECT '########## WOC ##########' AS marke;

SELECT '=== PX-WOC-1h - Plan (L15) ===' AS marke;
SET @s = CONCAT('EXPLAIN ', 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-30 03:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''WOC'')
              GROUP BY r.stunde ) s ) g');
PREPARE p_PX_WOC_1h_pl FROM @s;
EXECUTE p_PX_WOC_1h_pl;
DEALLOCATE PREPARE p_PX_WOC_1h_pl;
SELECT '=== PX-WOC-1h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-30 03:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''WOC'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_PX_WOC_1h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_PX_WOC_1h;
EXECUTE p_PX_WOC_1h;
EXECUTE p_PX_WOC_1h;
EXECUTE p_PX_WOC_1h;
EXECUTE p_PX_WOC_1h;
EXECUTE p_PX_WOC_1h;
SELECT 'PX-WOC-1h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_PX_WOC_1h;
SELECT '=== PX-WOC-24h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-29 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''WOC'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_PX_WOC_24h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_PX_WOC_24h;
EXECUTE p_PX_WOC_24h;
EXECUTE p_PX_WOC_24h;
EXECUTE p_PX_WOC_24h;
EXECUTE p_PX_WOC_24h;
EXECUTE p_PX_WOC_24h;
SELECT 'PX-WOC-24h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_PX_WOC_24h;
SELECT '=== PX-WOC-7d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-23 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''WOC'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_PX_WOC_7d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_PX_WOC_7d;
EXECUTE p_PX_WOC_7d;
EXECUTE p_PX_WOC_7d;
EXECUTE p_PX_WOC_7d;
EXECUTE p_PX_WOC_7d;
EXECUTE p_PX_WOC_7d;
SELECT 'PX-WOC-7d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_PX_WOC_7d;

SELECT '=== PX-WOC-30d - Plan (L15) ===' AS marke;
SET @s = CONCAT('EXPLAIN ', 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''WOC'')
              GROUP BY r.stunde ) s ) g');
PREPARE p_PX_WOC_30d_pl FROM @s;
EXECUTE p_PX_WOC_30d_pl;
DEALLOCATE PREPARE p_PX_WOC_30d_pl;
SELECT '=== PX-WOC-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''WOC'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_PX_WOC_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_PX_WOC_30d;
EXECUTE p_PX_WOC_30d;
EXECUTE p_PX_WOC_30d;
EXECUTE p_PX_WOC_30d;
EXECUTE p_PX_WOC_30d;
EXECUTE p_PX_WOC_30d;
SELECT 'PX-WOC-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_PX_WOC_30d;

SELECT '########## SYSTEM ##########' AS marke;

SELECT '=== PX-SYSTEM-1h - Plan (L15) ===' AS marke;
SET @s = CONCAT('EXPLAIN ', 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-30 03:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''SYSTEM'')
              GROUP BY r.stunde ) s ) g');
PREPARE p_PX_SYSTEM_1h_pl FROM @s;
EXECUTE p_PX_SYSTEM_1h_pl;
DEALLOCATE PREPARE p_PX_SYSTEM_1h_pl;
SELECT '=== PX-SYSTEM-1h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-30 03:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''SYSTEM'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_PX_SYSTEM_1h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_PX_SYSTEM_1h;
EXECUTE p_PX_SYSTEM_1h;
EXECUTE p_PX_SYSTEM_1h;
EXECUTE p_PX_SYSTEM_1h;
EXECUTE p_PX_SYSTEM_1h;
EXECUTE p_PX_SYSTEM_1h;
SELECT 'PX-SYSTEM-1h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_PX_SYSTEM_1h;
SELECT '=== PX-SYSTEM-24h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-29 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''SYSTEM'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_PX_SYSTEM_24h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_PX_SYSTEM_24h;
EXECUTE p_PX_SYSTEM_24h;
EXECUTE p_PX_SYSTEM_24h;
EXECUTE p_PX_SYSTEM_24h;
EXECUTE p_PX_SYSTEM_24h;
EXECUTE p_PX_SYSTEM_24h;
SELECT 'PX-SYSTEM-24h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_PX_SYSTEM_24h;
SELECT '=== PX-SYSTEM-7d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-23 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''SYSTEM'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_PX_SYSTEM_7d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_PX_SYSTEM_7d;
EXECUTE p_PX_SYSTEM_7d;
EXECUTE p_PX_SYSTEM_7d;
EXECUTE p_PX_SYSTEM_7d;
EXECUTE p_PX_SYSTEM_7d;
EXECUTE p_PX_SYSTEM_7d;
SELECT 'PX-SYSTEM-7d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_PX_SYSTEM_7d;

SELECT '=== PX-SYSTEM-30d - Plan (L15) ===' AS marke;
SET @s = CONCAT('EXPLAIN ', 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''SYSTEM'')
              GROUP BY r.stunde ) s ) g');
PREPARE p_PX_SYSTEM_30d_pl FROM @s;
EXECUTE p_PX_SYSTEM_30d_pl;
DEALLOCATE PREPARE p_PX_SYSTEM_30d_pl;
SELECT '=== PX-SYSTEM-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''SYSTEM'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_PX_SYSTEM_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_PX_SYSTEM_30d;
EXECUTE p_PX_SYSTEM_30d;
EXECUTE p_PX_SYSTEM_30d;
EXECUTE p_PX_SYSTEM_30d;
EXECUTE p_PX_SYSTEM_30d;
EXECUTE p_PX_SYSTEM_30d;
SELECT 'PX-SYSTEM-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_PX_SYSTEM_30d;

SELECT '########## NXHBE ##########' AS marke;

SELECT '=== PX-NXHBE-1h - Plan (L15) ===' AS marke;
SET @s = CONCAT('EXPLAIN ', 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-30 03:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''NXHBE'')
              GROUP BY r.stunde ) s ) g');
PREPARE p_PX_NXHBE_1h_pl FROM @s;
EXECUTE p_PX_NXHBE_1h_pl;
DEALLOCATE PREPARE p_PX_NXHBE_1h_pl;
SELECT '=== PX-NXHBE-1h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-30 03:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''NXHBE'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_PX_NXHBE_1h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_PX_NXHBE_1h;
EXECUTE p_PX_NXHBE_1h;
EXECUTE p_PX_NXHBE_1h;
EXECUTE p_PX_NXHBE_1h;
EXECUTE p_PX_NXHBE_1h;
EXECUTE p_PX_NXHBE_1h;
SELECT 'PX-NXHBE-1h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_PX_NXHBE_1h;
SELECT '=== PX-NXHBE-24h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-29 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''NXHBE'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_PX_NXHBE_24h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_PX_NXHBE_24h;
EXECUTE p_PX_NXHBE_24h;
EXECUTE p_PX_NXHBE_24h;
EXECUTE p_PX_NXHBE_24h;
EXECUTE p_PX_NXHBE_24h;
EXECUTE p_PX_NXHBE_24h;
SELECT 'PX-NXHBE-24h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_PX_NXHBE_24h;
SELECT '=== PX-NXHBE-7d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-23 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''NXHBE'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_PX_NXHBE_7d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_PX_NXHBE_7d;
EXECUTE p_PX_NXHBE_7d;
EXECUTE p_PX_NXHBE_7d;
EXECUTE p_PX_NXHBE_7d;
EXECUTE p_PX_NXHBE_7d;
EXECUTE p_PX_NXHBE_7d;
SELECT 'PX-NXHBE-7d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_PX_NXHBE_7d;

SELECT '=== PX-NXHBE-30d - Plan (L15) ===' AS marke;
SET @s = CONCAT('EXPLAIN ', 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''NXHBE'')
              GROUP BY r.stunde ) s ) g');
PREPARE p_PX_NXHBE_30d_pl FROM @s;
EXECUTE p_PX_NXHBE_30d_pl;
DEALLOCATE PREPARE p_PX_NXHBE_30d_pl;
SELECT '=== PX-NXHBE-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''NXHBE'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_PX_NXHBE_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_PX_NXHBE_30d;
EXECUTE p_PX_NXHBE_30d;
EXECUTE p_PX_NXHBE_30d;
EXECUTE p_PX_NXHBE_30d;
EXECUTE p_PX_NXHBE_30d;
EXECUTE p_PX_NXHBE_30d;
SELECT 'PX-NXHBE-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_PX_NXHBE_30d;

SELECT '########## EDITIONLINGERI ##########' AS marke;

SELECT '=== PX-EDITIONLINGERI-1h - Plan (L15) ===' AS marke;
SET @s = CONCAT('EXPLAIN ', 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-30 03:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''EDITIONLINGERI'')
              GROUP BY r.stunde ) s ) g');
PREPARE p_PX_EDITIONLINGERI_1h_pl FROM @s;
EXECUTE p_PX_EDITIONLINGERI_1h_pl;
DEALLOCATE PREPARE p_PX_EDITIONLINGERI_1h_pl;
SELECT '=== PX-EDITIONLINGERI-1h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-30 03:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''EDITIONLINGERI'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_PX_EDITIONLINGERI_1h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_PX_EDITIONLINGERI_1h;
EXECUTE p_PX_EDITIONLINGERI_1h;
EXECUTE p_PX_EDITIONLINGERI_1h;
EXECUTE p_PX_EDITIONLINGERI_1h;
EXECUTE p_PX_EDITIONLINGERI_1h;
EXECUTE p_PX_EDITIONLINGERI_1h;
SELECT 'PX-EDITIONLINGERI-1h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_PX_EDITIONLINGERI_1h;
SELECT '=== PX-EDITIONLINGERI-24h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-29 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''EDITIONLINGERI'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_PX_EDITIONLINGERI_24h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_PX_EDITIONLINGERI_24h;
EXECUTE p_PX_EDITIONLINGERI_24h;
EXECUTE p_PX_EDITIONLINGERI_24h;
EXECUTE p_PX_EDITIONLINGERI_24h;
EXECUTE p_PX_EDITIONLINGERI_24h;
EXECUTE p_PX_EDITIONLINGERI_24h;
SELECT 'PX-EDITIONLINGERI-24h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_PX_EDITIONLINGERI_24h;
SELECT '=== PX-EDITIONLINGERI-7d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-23 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''EDITIONLINGERI'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_PX_EDITIONLINGERI_7d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_PX_EDITIONLINGERI_7d;
EXECUTE p_PX_EDITIONLINGERI_7d;
EXECUTE p_PX_EDITIONLINGERI_7d;
EXECUTE p_PX_EDITIONLINGERI_7d;
EXECUTE p_PX_EDITIONLINGERI_7d;
EXECUTE p_PX_EDITIONLINGERI_7d;
SELECT 'PX-EDITIONLINGERI-7d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_PX_EDITIONLINGERI_7d;

SELECT '=== PX-EDITIONLINGERI-30d - Plan (L15) ===' AS marke;
SET @s = CONCAT('EXPLAIN ', 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''EDITIONLINGERI'')
              GROUP BY r.stunde ) s ) g');
PREPARE p_PX_EDITIONLINGERI_30d_pl FROM @s;
EXECUTE p_PX_EDITIONLINGERI_30d_pl;
DEALLOCATE PREPARE p_PX_EDITIONLINGERI_30d_pl;
SELECT '=== PX-EDITIONLINGERI-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''EDITIONLINGERI'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_PX_EDITIONLINGERI_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_PX_EDITIONLINGERI_30d;
EXECUTE p_PX_EDITIONLINGERI_30d;
EXECUTE p_PX_EDITIONLINGERI_30d;
EXECUTE p_PX_EDITIONLINGERI_30d;
EXECUTE p_PX_EDITIONLINGERI_30d;
EXECUTE p_PX_EDITIONLINGERI_30d;
SELECT 'PX-EDITIONLINGERI-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_PX_EDITIONLINGERI_30d;

SELECT '=== 99 Abschluss ===' AS marke;
SELECT @@global.read_only AS read_only_am_ende, NOW() AS serverzeit_ende;
