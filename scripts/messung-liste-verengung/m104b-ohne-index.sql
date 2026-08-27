-- Messung M104 b - Die Vorabfrage gegen die Probetabelle OHNE Sekundaerindex
-- Auftrag:  "Fensterverengung fuer die Nachrichtenliste", Stand 27.08.2026
-- Ergebnis: docs/messungen-liste-verengung.md
--
-- M104 b - Dieselben Stufen und dieselben zehn Mandanten wie M99, aber gegen die Probetabelle
-- overlord_monitor.message_rollup_probe_idx,
-- die zu diesem Zeitpunkt KEINEN Sekundaerindex traegt - der Kontrollwert.
-- Beide Messungen laufen gegen DIESELBE Tabelle; der Unterschied haengt allein am Index.
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
SELECT TABLE_ROWS, DATA_LENGTH, INDEX_LENGTH FROM information_schema.TABLES
 WHERE TABLE_SCHEMA='overlord_monitor' AND TABLE_NAME='message_rollup_probe_idx';

SELECT '########## NEXANS ##########' AS marke;
SELECT GROUP_CONCAT(QUOTE(ProcessID)) INTO @proz
FROM (SELECT DISTINCT p.ProcessID
      FROM GlassfishDB.Process p
      JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
      WHERE pm.MandantID = 'NEXANS') x;
SELECT 'NEXANS' AS mandant,
       (@proz IS NOT NULL) AS prozesse_vorhanden,
       LENGTH(@proz) - LENGTH(REPLACE(@proz, ',', '')) + 1 AS prozesse_anzahl;
SELECT '=== POHNE-lit-NEXANS-1h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-30 03:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_POHNE_lit_NEXANS_1h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_POHNE_lit_NEXANS_1h;
EXECUTE p_POHNE_lit_NEXANS_1h;
EXECUTE p_POHNE_lit_NEXANS_1h;
EXECUTE p_POHNE_lit_NEXANS_1h;
EXECUTE p_POHNE_lit_NEXANS_1h;
EXECUTE p_POHNE_lit_NEXANS_1h;
SELECT 'POHNE-lit-NEXANS-1h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_POHNE_lit_NEXANS_1h;
SELECT '=== POHNE-lit-NEXANS-24h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-29 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_POHNE_lit_NEXANS_24h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_POHNE_lit_NEXANS_24h;
EXECUTE p_POHNE_lit_NEXANS_24h;
EXECUTE p_POHNE_lit_NEXANS_24h;
EXECUTE p_POHNE_lit_NEXANS_24h;
EXECUTE p_POHNE_lit_NEXANS_24h;
EXECUTE p_POHNE_lit_NEXANS_24h;
SELECT 'POHNE-lit-NEXANS-24h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_POHNE_lit_NEXANS_24h;
SELECT '=== POHNE-lit-NEXANS-7d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-23 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_POHNE_lit_NEXANS_7d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_POHNE_lit_NEXANS_7d;
EXECUTE p_POHNE_lit_NEXANS_7d;
EXECUTE p_POHNE_lit_NEXANS_7d;
EXECUTE p_POHNE_lit_NEXANS_7d;
EXECUTE p_POHNE_lit_NEXANS_7d;
EXECUTE p_POHNE_lit_NEXANS_7d;
SELECT 'POHNE-lit-NEXANS-7d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_POHNE_lit_NEXANS_7d;

SELECT '=== POHNE-lit-NEXANS-30d - Plan (L15) ===' AS marke;
SET @s = CONCAT('EXPLAIN ', CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g'));
PREPARE p_POHNE_lit_NEXANS_30d_pl FROM @s;
EXECUTE p_POHNE_lit_NEXANS_30d_pl;
DEALLOCATE PREPARE p_POHNE_lit_NEXANS_30d_pl;
SELECT '=== POHNE-lit-NEXANS-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_POHNE_lit_NEXANS_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_POHNE_lit_NEXANS_30d;
EXECUTE p_POHNE_lit_NEXANS_30d;
EXECUTE p_POHNE_lit_NEXANS_30d;
EXECUTE p_POHNE_lit_NEXANS_30d;
EXECUTE p_POHNE_lit_NEXANS_30d;
EXECUTE p_POHNE_lit_NEXANS_30d;
SELECT 'POHNE-lit-NEXANS-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_POHNE_lit_NEXANS_30d;

SELECT '=== POHNE-ex-NEXANS-30d - Plan (L15) ===' AS marke;
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
PREPARE p_POHNE_ex_NEXANS_30d_pl FROM @s;
EXECUTE p_POHNE_ex_NEXANS_30d_pl;
DEALLOCATE PREPARE p_POHNE_ex_NEXANS_30d_pl;
SELECT '=== POHNE-ex-NEXANS-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
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
PREPARE p_POHNE_ex_NEXANS_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_POHNE_ex_NEXANS_30d;
EXECUTE p_POHNE_ex_NEXANS_30d;
EXECUTE p_POHNE_ex_NEXANS_30d;
EXECUTE p_POHNE_ex_NEXANS_30d;
EXECUTE p_POHNE_ex_NEXANS_30d;
EXECUTE p_POHNE_ex_NEXANS_30d;
SELECT 'POHNE-ex-NEXANS-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_POHNE_ex_NEXANS_30d;

SELECT '########## SUTTONS ##########' AS marke;
SELECT GROUP_CONCAT(QUOTE(ProcessID)) INTO @proz
FROM (SELECT DISTINCT p.ProcessID
      FROM GlassfishDB.Process p
      JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
      WHERE pm.MandantID = 'SUTTONS') x;
SELECT 'SUTTONS' AS mandant,
       (@proz IS NOT NULL) AS prozesse_vorhanden,
       LENGTH(@proz) - LENGTH(REPLACE(@proz, ',', '')) + 1 AS prozesse_anzahl;
SELECT '=== POHNE-lit-SUTTONS-1h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-30 03:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_POHNE_lit_SUTTONS_1h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_POHNE_lit_SUTTONS_1h;
EXECUTE p_POHNE_lit_SUTTONS_1h;
EXECUTE p_POHNE_lit_SUTTONS_1h;
EXECUTE p_POHNE_lit_SUTTONS_1h;
EXECUTE p_POHNE_lit_SUTTONS_1h;
EXECUTE p_POHNE_lit_SUTTONS_1h;
SELECT 'POHNE-lit-SUTTONS-1h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_POHNE_lit_SUTTONS_1h;
SELECT '=== POHNE-lit-SUTTONS-24h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-29 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_POHNE_lit_SUTTONS_24h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_POHNE_lit_SUTTONS_24h;
EXECUTE p_POHNE_lit_SUTTONS_24h;
EXECUTE p_POHNE_lit_SUTTONS_24h;
EXECUTE p_POHNE_lit_SUTTONS_24h;
EXECUTE p_POHNE_lit_SUTTONS_24h;
EXECUTE p_POHNE_lit_SUTTONS_24h;
SELECT 'POHNE-lit-SUTTONS-24h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_POHNE_lit_SUTTONS_24h;
SELECT '=== POHNE-lit-SUTTONS-7d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-23 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_POHNE_lit_SUTTONS_7d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_POHNE_lit_SUTTONS_7d;
EXECUTE p_POHNE_lit_SUTTONS_7d;
EXECUTE p_POHNE_lit_SUTTONS_7d;
EXECUTE p_POHNE_lit_SUTTONS_7d;
EXECUTE p_POHNE_lit_SUTTONS_7d;
EXECUTE p_POHNE_lit_SUTTONS_7d;
SELECT 'POHNE-lit-SUTTONS-7d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_POHNE_lit_SUTTONS_7d;

SELECT '=== POHNE-lit-SUTTONS-30d - Plan (L15) ===' AS marke;
SET @s = CONCAT('EXPLAIN ', CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g'));
PREPARE p_POHNE_lit_SUTTONS_30d_pl FROM @s;
EXECUTE p_POHNE_lit_SUTTONS_30d_pl;
DEALLOCATE PREPARE p_POHNE_lit_SUTTONS_30d_pl;
SELECT '=== POHNE-lit-SUTTONS-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_POHNE_lit_SUTTONS_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_POHNE_lit_SUTTONS_30d;
EXECUTE p_POHNE_lit_SUTTONS_30d;
EXECUTE p_POHNE_lit_SUTTONS_30d;
EXECUTE p_POHNE_lit_SUTTONS_30d;
EXECUTE p_POHNE_lit_SUTTONS_30d;
EXECUTE p_POHNE_lit_SUTTONS_30d;
SELECT 'POHNE-lit-SUTTONS-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_POHNE_lit_SUTTONS_30d;

SELECT '=== POHNE-ex-SUTTONS-30d - Plan (L15) ===' AS marke;
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
PREPARE p_POHNE_ex_SUTTONS_30d_pl FROM @s;
EXECUTE p_POHNE_ex_SUTTONS_30d_pl;
DEALLOCATE PREPARE p_POHNE_ex_SUTTONS_30d_pl;
SELECT '=== POHNE-ex-SUTTONS-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
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
PREPARE p_POHNE_ex_SUTTONS_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_POHNE_ex_SUTTONS_30d;
EXECUTE p_POHNE_ex_SUTTONS_30d;
EXECUTE p_POHNE_ex_SUTTONS_30d;
EXECUTE p_POHNE_ex_SUTTONS_30d;
EXECUTE p_POHNE_ex_SUTTONS_30d;
EXECUTE p_POHNE_ex_SUTTONS_30d;
SELECT 'POHNE-ex-SUTTONS-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_POHNE_ex_SUTTONS_30d;

SELECT '########## VOTG ##########' AS marke;
SELECT GROUP_CONCAT(QUOTE(ProcessID)) INTO @proz
FROM (SELECT DISTINCT p.ProcessID
      FROM GlassfishDB.Process p
      JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
      WHERE pm.MandantID = 'VOTG') x;
SELECT 'VOTG' AS mandant,
       (@proz IS NOT NULL) AS prozesse_vorhanden,
       LENGTH(@proz) - LENGTH(REPLACE(@proz, ',', '')) + 1 AS prozesse_anzahl;
SELECT '=== POHNE-lit-VOTG-1h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-30 03:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_POHNE_lit_VOTG_1h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_POHNE_lit_VOTG_1h;
EXECUTE p_POHNE_lit_VOTG_1h;
EXECUTE p_POHNE_lit_VOTG_1h;
EXECUTE p_POHNE_lit_VOTG_1h;
EXECUTE p_POHNE_lit_VOTG_1h;
EXECUTE p_POHNE_lit_VOTG_1h;
SELECT 'POHNE-lit-VOTG-1h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_POHNE_lit_VOTG_1h;
SELECT '=== POHNE-lit-VOTG-24h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-29 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_POHNE_lit_VOTG_24h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_POHNE_lit_VOTG_24h;
EXECUTE p_POHNE_lit_VOTG_24h;
EXECUTE p_POHNE_lit_VOTG_24h;
EXECUTE p_POHNE_lit_VOTG_24h;
EXECUTE p_POHNE_lit_VOTG_24h;
EXECUTE p_POHNE_lit_VOTG_24h;
SELECT 'POHNE-lit-VOTG-24h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_POHNE_lit_VOTG_24h;
SELECT '=== POHNE-lit-VOTG-7d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-23 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_POHNE_lit_VOTG_7d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_POHNE_lit_VOTG_7d;
EXECUTE p_POHNE_lit_VOTG_7d;
EXECUTE p_POHNE_lit_VOTG_7d;
EXECUTE p_POHNE_lit_VOTG_7d;
EXECUTE p_POHNE_lit_VOTG_7d;
EXECUTE p_POHNE_lit_VOTG_7d;
SELECT 'POHNE-lit-VOTG-7d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_POHNE_lit_VOTG_7d;

SELECT '=== POHNE-lit-VOTG-30d - Plan (L15) ===' AS marke;
SET @s = CONCAT('EXPLAIN ', CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g'));
PREPARE p_POHNE_lit_VOTG_30d_pl FROM @s;
EXECUTE p_POHNE_lit_VOTG_30d_pl;
DEALLOCATE PREPARE p_POHNE_lit_VOTG_30d_pl;
SELECT '=== POHNE-lit-VOTG-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_POHNE_lit_VOTG_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_POHNE_lit_VOTG_30d;
EXECUTE p_POHNE_lit_VOTG_30d;
EXECUTE p_POHNE_lit_VOTG_30d;
EXECUTE p_POHNE_lit_VOTG_30d;
EXECUTE p_POHNE_lit_VOTG_30d;
EXECUTE p_POHNE_lit_VOTG_30d;
SELECT 'POHNE-lit-VOTG-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_POHNE_lit_VOTG_30d;

SELECT '=== POHNE-ex-VOTG-30d - Plan (L15) ===' AS marke;
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
PREPARE p_POHNE_ex_VOTG_30d_pl FROM @s;
EXECUTE p_POHNE_ex_VOTG_30d_pl;
DEALLOCATE PREPARE p_POHNE_ex_VOTG_30d_pl;
SELECT '=== POHNE-ex-VOTG-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
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
PREPARE p_POHNE_ex_VOTG_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_POHNE_ex_VOTG_30d;
EXECUTE p_POHNE_ex_VOTG_30d;
EXECUTE p_POHNE_ex_VOTG_30d;
EXECUTE p_POHNE_ex_VOTG_30d;
EXECUTE p_POHNE_ex_VOTG_30d;
EXECUTE p_POHNE_ex_VOTG_30d;
SELECT 'POHNE-ex-VOTG-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_POHNE_ex_VOTG_30d;

SELECT '########## IBIS ##########' AS marke;
SELECT GROUP_CONCAT(QUOTE(ProcessID)) INTO @proz
FROM (SELECT DISTINCT p.ProcessID
      FROM GlassfishDB.Process p
      JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
      WHERE pm.MandantID = 'IBIS') x;
SELECT 'IBIS' AS mandant,
       (@proz IS NOT NULL) AS prozesse_vorhanden,
       LENGTH(@proz) - LENGTH(REPLACE(@proz, ',', '')) + 1 AS prozesse_anzahl;
SELECT '=== POHNE-lit-IBIS-1h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-30 03:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_POHNE_lit_IBIS_1h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_POHNE_lit_IBIS_1h;
EXECUTE p_POHNE_lit_IBIS_1h;
EXECUTE p_POHNE_lit_IBIS_1h;
EXECUTE p_POHNE_lit_IBIS_1h;
EXECUTE p_POHNE_lit_IBIS_1h;
EXECUTE p_POHNE_lit_IBIS_1h;
SELECT 'POHNE-lit-IBIS-1h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_POHNE_lit_IBIS_1h;
SELECT '=== POHNE-lit-IBIS-24h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-29 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_POHNE_lit_IBIS_24h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_POHNE_lit_IBIS_24h;
EXECUTE p_POHNE_lit_IBIS_24h;
EXECUTE p_POHNE_lit_IBIS_24h;
EXECUTE p_POHNE_lit_IBIS_24h;
EXECUTE p_POHNE_lit_IBIS_24h;
EXECUTE p_POHNE_lit_IBIS_24h;
SELECT 'POHNE-lit-IBIS-24h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_POHNE_lit_IBIS_24h;
SELECT '=== POHNE-lit-IBIS-7d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-23 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_POHNE_lit_IBIS_7d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_POHNE_lit_IBIS_7d;
EXECUTE p_POHNE_lit_IBIS_7d;
EXECUTE p_POHNE_lit_IBIS_7d;
EXECUTE p_POHNE_lit_IBIS_7d;
EXECUTE p_POHNE_lit_IBIS_7d;
EXECUTE p_POHNE_lit_IBIS_7d;
SELECT 'POHNE-lit-IBIS-7d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_POHNE_lit_IBIS_7d;

SELECT '=== POHNE-lit-IBIS-30d - Plan (L15) ===' AS marke;
SET @s = CONCAT('EXPLAIN ', CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g'));
PREPARE p_POHNE_lit_IBIS_30d_pl FROM @s;
EXECUTE p_POHNE_lit_IBIS_30d_pl;
DEALLOCATE PREPARE p_POHNE_lit_IBIS_30d_pl;
SELECT '=== POHNE-lit-IBIS-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_POHNE_lit_IBIS_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_POHNE_lit_IBIS_30d;
EXECUTE p_POHNE_lit_IBIS_30d;
EXECUTE p_POHNE_lit_IBIS_30d;
EXECUTE p_POHNE_lit_IBIS_30d;
EXECUTE p_POHNE_lit_IBIS_30d;
EXECUTE p_POHNE_lit_IBIS_30d;
SELECT 'POHNE-lit-IBIS-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_POHNE_lit_IBIS_30d;

SELECT '=== POHNE-ex-IBIS-30d - Plan (L15) ===' AS marke;
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
PREPARE p_POHNE_ex_IBIS_30d_pl FROM @s;
EXECUTE p_POHNE_ex_IBIS_30d_pl;
DEALLOCATE PREPARE p_POHNE_ex_IBIS_30d_pl;
SELECT '=== POHNE-ex-IBIS-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
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
PREPARE p_POHNE_ex_IBIS_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_POHNE_ex_IBIS_30d;
EXECUTE p_POHNE_ex_IBIS_30d;
EXECUTE p_POHNE_ex_IBIS_30d;
EXECUTE p_POHNE_ex_IBIS_30d;
EXECUTE p_POHNE_ex_IBIS_30d;
EXECUTE p_POHNE_ex_IBIS_30d;
SELECT 'POHNE-ex-IBIS-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_POHNE_ex_IBIS_30d;

SELECT '########## IBISGUS ##########' AS marke;
SELECT GROUP_CONCAT(QUOTE(ProcessID)) INTO @proz
FROM (SELECT DISTINCT p.ProcessID
      FROM GlassfishDB.Process p
      JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
      WHERE pm.MandantID = 'IBISGUS') x;
SELECT 'IBISGUS' AS mandant,
       (@proz IS NOT NULL) AS prozesse_vorhanden,
       LENGTH(@proz) - LENGTH(REPLACE(@proz, ',', '')) + 1 AS prozesse_anzahl;
SELECT '=== POHNE-lit-IBISGUS-1h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-30 03:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_POHNE_lit_IBISGUS_1h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_POHNE_lit_IBISGUS_1h;
EXECUTE p_POHNE_lit_IBISGUS_1h;
EXECUTE p_POHNE_lit_IBISGUS_1h;
EXECUTE p_POHNE_lit_IBISGUS_1h;
EXECUTE p_POHNE_lit_IBISGUS_1h;
EXECUTE p_POHNE_lit_IBISGUS_1h;
SELECT 'POHNE-lit-IBISGUS-1h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_POHNE_lit_IBISGUS_1h;
SELECT '=== POHNE-lit-IBISGUS-24h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-29 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_POHNE_lit_IBISGUS_24h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_POHNE_lit_IBISGUS_24h;
EXECUTE p_POHNE_lit_IBISGUS_24h;
EXECUTE p_POHNE_lit_IBISGUS_24h;
EXECUTE p_POHNE_lit_IBISGUS_24h;
EXECUTE p_POHNE_lit_IBISGUS_24h;
EXECUTE p_POHNE_lit_IBISGUS_24h;
SELECT 'POHNE-lit-IBISGUS-24h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_POHNE_lit_IBISGUS_24h;
SELECT '=== POHNE-lit-IBISGUS-7d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-23 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_POHNE_lit_IBISGUS_7d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_POHNE_lit_IBISGUS_7d;
EXECUTE p_POHNE_lit_IBISGUS_7d;
EXECUTE p_POHNE_lit_IBISGUS_7d;
EXECUTE p_POHNE_lit_IBISGUS_7d;
EXECUTE p_POHNE_lit_IBISGUS_7d;
EXECUTE p_POHNE_lit_IBISGUS_7d;
SELECT 'POHNE-lit-IBISGUS-7d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_POHNE_lit_IBISGUS_7d;

SELECT '=== POHNE-lit-IBISGUS-30d - Plan (L15) ===' AS marke;
SET @s = CONCAT('EXPLAIN ', CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g'));
PREPARE p_POHNE_lit_IBISGUS_30d_pl FROM @s;
EXECUTE p_POHNE_lit_IBISGUS_30d_pl;
DEALLOCATE PREPARE p_POHNE_lit_IBISGUS_30d_pl;
SELECT '=== POHNE-lit-IBISGUS-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_POHNE_lit_IBISGUS_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_POHNE_lit_IBISGUS_30d;
EXECUTE p_POHNE_lit_IBISGUS_30d;
EXECUTE p_POHNE_lit_IBISGUS_30d;
EXECUTE p_POHNE_lit_IBISGUS_30d;
EXECUTE p_POHNE_lit_IBISGUS_30d;
EXECUTE p_POHNE_lit_IBISGUS_30d;
SELECT 'POHNE-lit-IBISGUS-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_POHNE_lit_IBISGUS_30d;

SELECT '=== POHNE-ex-IBISGUS-30d - Plan (L15) ===' AS marke;
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
PREPARE p_POHNE_ex_IBISGUS_30d_pl FROM @s;
EXECUTE p_POHNE_ex_IBISGUS_30d_pl;
DEALLOCATE PREPARE p_POHNE_ex_IBISGUS_30d_pl;
SELECT '=== POHNE-ex-IBISGUS-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
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
PREPARE p_POHNE_ex_IBISGUS_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_POHNE_ex_IBISGUS_30d;
EXECUTE p_POHNE_ex_IBISGUS_30d;
EXECUTE p_POHNE_ex_IBISGUS_30d;
EXECUTE p_POHNE_ex_IBISGUS_30d;
EXECUTE p_POHNE_ex_IBISGUS_30d;
EXECUTE p_POHNE_ex_IBISGUS_30d;
SELECT 'POHNE-ex-IBISGUS-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_POHNE_ex_IBISGUS_30d;

SELECT '########## ZAST ##########' AS marke;
SELECT GROUP_CONCAT(QUOTE(ProcessID)) INTO @proz
FROM (SELECT DISTINCT p.ProcessID
      FROM GlassfishDB.Process p
      JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
      WHERE pm.MandantID = 'ZAST') x;
SELECT 'ZAST' AS mandant,
       (@proz IS NOT NULL) AS prozesse_vorhanden,
       LENGTH(@proz) - LENGTH(REPLACE(@proz, ',', '')) + 1 AS prozesse_anzahl;
SELECT '=== POHNE-lit-ZAST-1h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-30 03:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_POHNE_lit_ZAST_1h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_POHNE_lit_ZAST_1h;
EXECUTE p_POHNE_lit_ZAST_1h;
EXECUTE p_POHNE_lit_ZAST_1h;
EXECUTE p_POHNE_lit_ZAST_1h;
EXECUTE p_POHNE_lit_ZAST_1h;
EXECUTE p_POHNE_lit_ZAST_1h;
SELECT 'POHNE-lit-ZAST-1h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_POHNE_lit_ZAST_1h;
SELECT '=== POHNE-lit-ZAST-24h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-29 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_POHNE_lit_ZAST_24h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_POHNE_lit_ZAST_24h;
EXECUTE p_POHNE_lit_ZAST_24h;
EXECUTE p_POHNE_lit_ZAST_24h;
EXECUTE p_POHNE_lit_ZAST_24h;
EXECUTE p_POHNE_lit_ZAST_24h;
EXECUTE p_POHNE_lit_ZAST_24h;
SELECT 'POHNE-lit-ZAST-24h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_POHNE_lit_ZAST_24h;
SELECT '=== POHNE-lit-ZAST-7d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-23 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_POHNE_lit_ZAST_7d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_POHNE_lit_ZAST_7d;
EXECUTE p_POHNE_lit_ZAST_7d;
EXECUTE p_POHNE_lit_ZAST_7d;
EXECUTE p_POHNE_lit_ZAST_7d;
EXECUTE p_POHNE_lit_ZAST_7d;
EXECUTE p_POHNE_lit_ZAST_7d;
SELECT 'POHNE-lit-ZAST-7d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_POHNE_lit_ZAST_7d;

SELECT '=== POHNE-lit-ZAST-30d - Plan (L15) ===' AS marke;
SET @s = CONCAT('EXPLAIN ', CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g'));
PREPARE p_POHNE_lit_ZAST_30d_pl FROM @s;
EXECUTE p_POHNE_lit_ZAST_30d_pl;
DEALLOCATE PREPARE p_POHNE_lit_ZAST_30d_pl;
SELECT '=== POHNE-lit-ZAST-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_POHNE_lit_ZAST_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_POHNE_lit_ZAST_30d;
EXECUTE p_POHNE_lit_ZAST_30d;
EXECUTE p_POHNE_lit_ZAST_30d;
EXECUTE p_POHNE_lit_ZAST_30d;
EXECUTE p_POHNE_lit_ZAST_30d;
EXECUTE p_POHNE_lit_ZAST_30d;
SELECT 'POHNE-lit-ZAST-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_POHNE_lit_ZAST_30d;

SELECT '=== POHNE-ex-ZAST-30d - Plan (L15) ===' AS marke;
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
PREPARE p_POHNE_ex_ZAST_30d_pl FROM @s;
EXECUTE p_POHNE_ex_ZAST_30d_pl;
DEALLOCATE PREPARE p_POHNE_ex_ZAST_30d_pl;
SELECT '=== POHNE-ex-ZAST-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
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
PREPARE p_POHNE_ex_ZAST_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_POHNE_ex_ZAST_30d;
EXECUTE p_POHNE_ex_ZAST_30d;
EXECUTE p_POHNE_ex_ZAST_30d;
EXECUTE p_POHNE_ex_ZAST_30d;
EXECUTE p_POHNE_ex_ZAST_30d;
EXECUTE p_POHNE_ex_ZAST_30d;
SELECT 'POHNE-ex-ZAST-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_POHNE_ex_ZAST_30d;

SELECT '########## WOC ##########' AS marke;
SELECT GROUP_CONCAT(QUOTE(ProcessID)) INTO @proz
FROM (SELECT DISTINCT p.ProcessID
      FROM GlassfishDB.Process p
      JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
      WHERE pm.MandantID = 'WOC') x;
SELECT 'WOC' AS mandant,
       (@proz IS NOT NULL) AS prozesse_vorhanden,
       LENGTH(@proz) - LENGTH(REPLACE(@proz, ',', '')) + 1 AS prozesse_anzahl;
SELECT '=== POHNE-lit-WOC-1h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-30 03:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_POHNE_lit_WOC_1h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_POHNE_lit_WOC_1h;
EXECUTE p_POHNE_lit_WOC_1h;
EXECUTE p_POHNE_lit_WOC_1h;
EXECUTE p_POHNE_lit_WOC_1h;
EXECUTE p_POHNE_lit_WOC_1h;
EXECUTE p_POHNE_lit_WOC_1h;
SELECT 'POHNE-lit-WOC-1h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_POHNE_lit_WOC_1h;
SELECT '=== POHNE-lit-WOC-24h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-29 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_POHNE_lit_WOC_24h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_POHNE_lit_WOC_24h;
EXECUTE p_POHNE_lit_WOC_24h;
EXECUTE p_POHNE_lit_WOC_24h;
EXECUTE p_POHNE_lit_WOC_24h;
EXECUTE p_POHNE_lit_WOC_24h;
EXECUTE p_POHNE_lit_WOC_24h;
SELECT 'POHNE-lit-WOC-24h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_POHNE_lit_WOC_24h;
SELECT '=== POHNE-lit-WOC-7d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-23 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_POHNE_lit_WOC_7d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_POHNE_lit_WOC_7d;
EXECUTE p_POHNE_lit_WOC_7d;
EXECUTE p_POHNE_lit_WOC_7d;
EXECUTE p_POHNE_lit_WOC_7d;
EXECUTE p_POHNE_lit_WOC_7d;
EXECUTE p_POHNE_lit_WOC_7d;
SELECT 'POHNE-lit-WOC-7d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_POHNE_lit_WOC_7d;

SELECT '=== POHNE-lit-WOC-30d - Plan (L15) ===' AS marke;
SET @s = CONCAT('EXPLAIN ', CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g'));
PREPARE p_POHNE_lit_WOC_30d_pl FROM @s;
EXECUTE p_POHNE_lit_WOC_30d_pl;
DEALLOCATE PREPARE p_POHNE_lit_WOC_30d_pl;
SELECT '=== POHNE-lit-WOC-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_POHNE_lit_WOC_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_POHNE_lit_WOC_30d;
EXECUTE p_POHNE_lit_WOC_30d;
EXECUTE p_POHNE_lit_WOC_30d;
EXECUTE p_POHNE_lit_WOC_30d;
EXECUTE p_POHNE_lit_WOC_30d;
EXECUTE p_POHNE_lit_WOC_30d;
SELECT 'POHNE-lit-WOC-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_POHNE_lit_WOC_30d;

SELECT '=== POHNE-ex-WOC-30d - Plan (L15) ===' AS marke;
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
PREPARE p_POHNE_ex_WOC_30d_pl FROM @s;
EXECUTE p_POHNE_ex_WOC_30d_pl;
DEALLOCATE PREPARE p_POHNE_ex_WOC_30d_pl;
SELECT '=== POHNE-ex-WOC-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
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
PREPARE p_POHNE_ex_WOC_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_POHNE_ex_WOC_30d;
EXECUTE p_POHNE_ex_WOC_30d;
EXECUTE p_POHNE_ex_WOC_30d;
EXECUTE p_POHNE_ex_WOC_30d;
EXECUTE p_POHNE_ex_WOC_30d;
EXECUTE p_POHNE_ex_WOC_30d;
SELECT 'POHNE-ex-WOC-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_POHNE_ex_WOC_30d;

SELECT '########## SYSTEM ##########' AS marke;
SELECT GROUP_CONCAT(QUOTE(ProcessID)) INTO @proz
FROM (SELECT DISTINCT p.ProcessID
      FROM GlassfishDB.Process p
      JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
      WHERE pm.MandantID = 'SYSTEM') x;
SELECT 'SYSTEM' AS mandant,
       (@proz IS NOT NULL) AS prozesse_vorhanden,
       LENGTH(@proz) - LENGTH(REPLACE(@proz, ',', '')) + 1 AS prozesse_anzahl;
SELECT '=== POHNE-lit-SYSTEM-1h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-30 03:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_POHNE_lit_SYSTEM_1h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_POHNE_lit_SYSTEM_1h;
EXECUTE p_POHNE_lit_SYSTEM_1h;
EXECUTE p_POHNE_lit_SYSTEM_1h;
EXECUTE p_POHNE_lit_SYSTEM_1h;
EXECUTE p_POHNE_lit_SYSTEM_1h;
EXECUTE p_POHNE_lit_SYSTEM_1h;
SELECT 'POHNE-lit-SYSTEM-1h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_POHNE_lit_SYSTEM_1h;
SELECT '=== POHNE-lit-SYSTEM-24h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-29 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_POHNE_lit_SYSTEM_24h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_POHNE_lit_SYSTEM_24h;
EXECUTE p_POHNE_lit_SYSTEM_24h;
EXECUTE p_POHNE_lit_SYSTEM_24h;
EXECUTE p_POHNE_lit_SYSTEM_24h;
EXECUTE p_POHNE_lit_SYSTEM_24h;
EXECUTE p_POHNE_lit_SYSTEM_24h;
SELECT 'POHNE-lit-SYSTEM-24h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_POHNE_lit_SYSTEM_24h;
SELECT '=== POHNE-lit-SYSTEM-7d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-23 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_POHNE_lit_SYSTEM_7d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_POHNE_lit_SYSTEM_7d;
EXECUTE p_POHNE_lit_SYSTEM_7d;
EXECUTE p_POHNE_lit_SYSTEM_7d;
EXECUTE p_POHNE_lit_SYSTEM_7d;
EXECUTE p_POHNE_lit_SYSTEM_7d;
EXECUTE p_POHNE_lit_SYSTEM_7d;
SELECT 'POHNE-lit-SYSTEM-7d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_POHNE_lit_SYSTEM_7d;

SELECT '=== POHNE-lit-SYSTEM-30d - Plan (L15) ===' AS marke;
SET @s = CONCAT('EXPLAIN ', CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g'));
PREPARE p_POHNE_lit_SYSTEM_30d_pl FROM @s;
EXECUTE p_POHNE_lit_SYSTEM_30d_pl;
DEALLOCATE PREPARE p_POHNE_lit_SYSTEM_30d_pl;
SELECT '=== POHNE-lit-SYSTEM-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_POHNE_lit_SYSTEM_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_POHNE_lit_SYSTEM_30d;
EXECUTE p_POHNE_lit_SYSTEM_30d;
EXECUTE p_POHNE_lit_SYSTEM_30d;
EXECUTE p_POHNE_lit_SYSTEM_30d;
EXECUTE p_POHNE_lit_SYSTEM_30d;
EXECUTE p_POHNE_lit_SYSTEM_30d;
SELECT 'POHNE-lit-SYSTEM-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_POHNE_lit_SYSTEM_30d;

SELECT '=== POHNE-ex-SYSTEM-30d - Plan (L15) ===' AS marke;
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
PREPARE p_POHNE_ex_SYSTEM_30d_pl FROM @s;
EXECUTE p_POHNE_ex_SYSTEM_30d_pl;
DEALLOCATE PREPARE p_POHNE_ex_SYSTEM_30d_pl;
SELECT '=== POHNE-ex-SYSTEM-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
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
PREPARE p_POHNE_ex_SYSTEM_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_POHNE_ex_SYSTEM_30d;
EXECUTE p_POHNE_ex_SYSTEM_30d;
EXECUTE p_POHNE_ex_SYSTEM_30d;
EXECUTE p_POHNE_ex_SYSTEM_30d;
EXECUTE p_POHNE_ex_SYSTEM_30d;
EXECUTE p_POHNE_ex_SYSTEM_30d;
SELECT 'POHNE-ex-SYSTEM-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_POHNE_ex_SYSTEM_30d;

SELECT '########## NXHBE ##########' AS marke;
SELECT GROUP_CONCAT(QUOTE(ProcessID)) INTO @proz
FROM (SELECT DISTINCT p.ProcessID
      FROM GlassfishDB.Process p
      JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
      WHERE pm.MandantID = 'NXHBE') x;
SELECT 'NXHBE' AS mandant,
       (@proz IS NOT NULL) AS prozesse_vorhanden,
       LENGTH(@proz) - LENGTH(REPLACE(@proz, ',', '')) + 1 AS prozesse_anzahl;
SELECT '=== POHNE-lit-NXHBE-1h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-30 03:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_POHNE_lit_NXHBE_1h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_POHNE_lit_NXHBE_1h;
EXECUTE p_POHNE_lit_NXHBE_1h;
EXECUTE p_POHNE_lit_NXHBE_1h;
EXECUTE p_POHNE_lit_NXHBE_1h;
EXECUTE p_POHNE_lit_NXHBE_1h;
EXECUTE p_POHNE_lit_NXHBE_1h;
SELECT 'POHNE-lit-NXHBE-1h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_POHNE_lit_NXHBE_1h;
SELECT '=== POHNE-lit-NXHBE-24h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-29 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_POHNE_lit_NXHBE_24h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_POHNE_lit_NXHBE_24h;
EXECUTE p_POHNE_lit_NXHBE_24h;
EXECUTE p_POHNE_lit_NXHBE_24h;
EXECUTE p_POHNE_lit_NXHBE_24h;
EXECUTE p_POHNE_lit_NXHBE_24h;
EXECUTE p_POHNE_lit_NXHBE_24h;
SELECT 'POHNE-lit-NXHBE-24h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_POHNE_lit_NXHBE_24h;
SELECT '=== POHNE-lit-NXHBE-7d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-23 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_POHNE_lit_NXHBE_7d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_POHNE_lit_NXHBE_7d;
EXECUTE p_POHNE_lit_NXHBE_7d;
EXECUTE p_POHNE_lit_NXHBE_7d;
EXECUTE p_POHNE_lit_NXHBE_7d;
EXECUTE p_POHNE_lit_NXHBE_7d;
EXECUTE p_POHNE_lit_NXHBE_7d;
SELECT 'POHNE-lit-NXHBE-7d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_POHNE_lit_NXHBE_7d;

SELECT '=== POHNE-lit-NXHBE-30d - Plan (L15) ===' AS marke;
SET @s = CONCAT('EXPLAIN ', CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g'));
PREPARE p_POHNE_lit_NXHBE_30d_pl FROM @s;
EXECUTE p_POHNE_lit_NXHBE_30d_pl;
DEALLOCATE PREPARE p_POHNE_lit_NXHBE_30d_pl;
SELECT '=== POHNE-lit-NXHBE-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_POHNE_lit_NXHBE_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_POHNE_lit_NXHBE_30d;
EXECUTE p_POHNE_lit_NXHBE_30d;
EXECUTE p_POHNE_lit_NXHBE_30d;
EXECUTE p_POHNE_lit_NXHBE_30d;
EXECUTE p_POHNE_lit_NXHBE_30d;
EXECUTE p_POHNE_lit_NXHBE_30d;
SELECT 'POHNE-lit-NXHBE-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_POHNE_lit_NXHBE_30d;

SELECT '=== POHNE-ex-NXHBE-30d - Plan (L15) ===' AS marke;
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
PREPARE p_POHNE_ex_NXHBE_30d_pl FROM @s;
EXECUTE p_POHNE_ex_NXHBE_30d_pl;
DEALLOCATE PREPARE p_POHNE_ex_NXHBE_30d_pl;
SELECT '=== POHNE-ex-NXHBE-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
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
PREPARE p_POHNE_ex_NXHBE_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_POHNE_ex_NXHBE_30d;
EXECUTE p_POHNE_ex_NXHBE_30d;
EXECUTE p_POHNE_ex_NXHBE_30d;
EXECUTE p_POHNE_ex_NXHBE_30d;
EXECUTE p_POHNE_ex_NXHBE_30d;
EXECUTE p_POHNE_ex_NXHBE_30d;
SELECT 'POHNE-ex-NXHBE-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_POHNE_ex_NXHBE_30d;

SELECT '########## EDITIONLINGERI ##########' AS marke;
SELECT GROUP_CONCAT(QUOTE(ProcessID)) INTO @proz
FROM (SELECT DISTINCT p.ProcessID
      FROM GlassfishDB.Process p
      JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
      WHERE pm.MandantID = 'EDITIONLINGERI') x;
SELECT 'EDITIONLINGERI' AS mandant,
       (@proz IS NOT NULL) AS prozesse_vorhanden,
       LENGTH(@proz) - LENGTH(REPLACE(@proz, ',', '')) + 1 AS prozesse_anzahl;
SELECT '=== POHNE-lit-EDITIONLINGERI-1h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-30 03:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_POHNE_lit_EDITIONLINGERI_1h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_POHNE_lit_EDITIONLINGERI_1h;
EXECUTE p_POHNE_lit_EDITIONLINGERI_1h;
EXECUTE p_POHNE_lit_EDITIONLINGERI_1h;
EXECUTE p_POHNE_lit_EDITIONLINGERI_1h;
EXECUTE p_POHNE_lit_EDITIONLINGERI_1h;
EXECUTE p_POHNE_lit_EDITIONLINGERI_1h;
SELECT 'POHNE-lit-EDITIONLINGERI-1h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_POHNE_lit_EDITIONLINGERI_1h;
SELECT '=== POHNE-lit-EDITIONLINGERI-24h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-29 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_POHNE_lit_EDITIONLINGERI_24h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_POHNE_lit_EDITIONLINGERI_24h;
EXECUTE p_POHNE_lit_EDITIONLINGERI_24h;
EXECUTE p_POHNE_lit_EDITIONLINGERI_24h;
EXECUTE p_POHNE_lit_EDITIONLINGERI_24h;
EXECUTE p_POHNE_lit_EDITIONLINGERI_24h;
EXECUTE p_POHNE_lit_EDITIONLINGERI_24h;
SELECT 'POHNE-lit-EDITIONLINGERI-24h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_POHNE_lit_EDITIONLINGERI_24h;
SELECT '=== POHNE-lit-EDITIONLINGERI-7d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-12-23 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_POHNE_lit_EDITIONLINGERI_7d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_POHNE_lit_EDITIONLINGERI_7d;
EXECUTE p_POHNE_lit_EDITIONLINGERI_7d;
EXECUTE p_POHNE_lit_EDITIONLINGERI_7d;
EXECUTE p_POHNE_lit_EDITIONLINGERI_7d;
EXECUTE p_POHNE_lit_EDITIONLINGERI_7d;
EXECUTE p_POHNE_lit_EDITIONLINGERI_7d;
SELECT 'POHNE-lit-EDITIONLINGERI-7d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_POHNE_lit_EDITIONLINGERI_7d;

SELECT '=== POHNE-lit-EDITIONLINGERI-30d - Plan (L15) ===' AS marke;
SET @s = CONCAT('EXPLAIN ', CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g'));
PREPARE p_POHNE_lit_EDITIONLINGERI_30d_pl FROM @s;
EXECUTE p_POHNE_lit_EDITIONLINGERI_30d_pl;
DEALLOCATE PREPARE p_POHNE_lit_EDITIONLINGERI_30d_pl;
SELECT '=== POHNE-lit-EDITIONLINGERI-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = CONCAT('SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup_probe_idx r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND r.process_id IN (', @proz, ')
              GROUP BY r.stunde ) s ) g');
PREPARE p_POHNE_lit_EDITIONLINGERI_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_POHNE_lit_EDITIONLINGERI_30d;
EXECUTE p_POHNE_lit_EDITIONLINGERI_30d;
EXECUTE p_POHNE_lit_EDITIONLINGERI_30d;
EXECUTE p_POHNE_lit_EDITIONLINGERI_30d;
EXECUTE p_POHNE_lit_EDITIONLINGERI_30d;
EXECUTE p_POHNE_lit_EDITIONLINGERI_30d;
SELECT 'POHNE-lit-EDITIONLINGERI-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_POHNE_lit_EDITIONLINGERI_30d;

SELECT '=== POHNE-ex-EDITIONLINGERI-30d - Plan (L15) ===' AS marke;
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
PREPARE p_POHNE_ex_EDITIONLINGERI_30d_pl FROM @s;
EXECUTE p_POHNE_ex_EDITIONLINGERI_30d_pl;
DEALLOCATE PREPARE p_POHNE_ex_EDITIONLINGERI_30d_pl;
SELECT '=== POHNE-ex-EDITIONLINGERI-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
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
PREPARE p_POHNE_ex_EDITIONLINGERI_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_POHNE_ex_EDITIONLINGERI_30d;
EXECUTE p_POHNE_ex_EDITIONLINGERI_30d;
EXECUTE p_POHNE_ex_EDITIONLINGERI_30d;
EXECUTE p_POHNE_ex_EDITIONLINGERI_30d;
EXECUTE p_POHNE_ex_EDITIONLINGERI_30d;
EXECUTE p_POHNE_ex_EDITIONLINGERI_30d;
SELECT 'POHNE-ex-EDITIONLINGERI-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_POHNE_ex_EDITIONLINGERI_30d;

SELECT '=== 99 Abschluss ===' AS marke;
SELECT @@global.read_only AS read_only_am_ende, NOW() AS serverzeit_ende;
