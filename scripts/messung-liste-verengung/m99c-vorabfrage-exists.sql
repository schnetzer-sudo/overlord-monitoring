-- Messung M99 (Nachtrag 2) - Die Vorabfrage als EXISTS-Kette
-- Auftrag:  "Fensterverengung fuer die Nachrichtenliste", Stand 27.08.2026
-- Ergebnis: docs/messungen-liste-verengung.md
--
-- M99 (Nachtrag 2) - Die Vorabfrage als EXISTS-Kette, ueber alle vier Stufen und alle zehn
-- Mandanten. Der Nachtrag 1 (m99b) hat gezeigt: die Literalliste kostet bei engem Bereich
-- mehr als sie spart (2,198 ms gegen 0,993 ms bei einer Stunde) und muss zusaetzlich erst
-- beschafft werden (2,037 ms bei NEXANS). Ihr Vorteil liegt allein beim weiten Bereich.
-- Da die gestufte Vorabfrage die meisten Mandanten auf der 1- oder 24-Stunden-Stufe
-- aufloest, ist die EXISTS-Kette die naheliegende Fassung - sie braucht keine Liste.
-- Gemessen wird hier NUR die Vorabfrage; die Quellabfrage ist von der Fassung unberuehrt,
-- weil beide dieselbe Untergrenze liefern (in M99 fuer alle zehn Mandanten belegt).
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
SELECT '=== VAE-NEXANS-1h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-12-30 03:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''NEXANS'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_VAE_NEXANS_1h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VAE_NEXANS_1h;
EXECUTE p_VAE_NEXANS_1h;
EXECUTE p_VAE_NEXANS_1h;
EXECUTE p_VAE_NEXANS_1h;
EXECUTE p_VAE_NEXANS_1h;
EXECUTE p_VAE_NEXANS_1h;
SELECT 'VAE-NEXANS-1h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VAE_NEXANS_1h;
SELECT '=== VAE-NEXANS-24h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-12-29 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''NEXANS'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_VAE_NEXANS_24h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VAE_NEXANS_24h;
EXECUTE p_VAE_NEXANS_24h;
EXECUTE p_VAE_NEXANS_24h;
EXECUTE p_VAE_NEXANS_24h;
EXECUTE p_VAE_NEXANS_24h;
EXECUTE p_VAE_NEXANS_24h;
SELECT 'VAE-NEXANS-24h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VAE_NEXANS_24h;
SELECT '=== VAE-NEXANS-7d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-12-23 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''NEXANS'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_VAE_NEXANS_7d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VAE_NEXANS_7d;
EXECUTE p_VAE_NEXANS_7d;
EXECUTE p_VAE_NEXANS_7d;
EXECUTE p_VAE_NEXANS_7d;
EXECUTE p_VAE_NEXANS_7d;
EXECUTE p_VAE_NEXANS_7d;
SELECT 'VAE-NEXANS-7d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VAE_NEXANS_7d;
SELECT '=== VAE-NEXANS-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''NEXANS'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_VAE_NEXANS_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VAE_NEXANS_30d;
EXECUTE p_VAE_NEXANS_30d;
EXECUTE p_VAE_NEXANS_30d;
EXECUTE p_VAE_NEXANS_30d;
EXECUTE p_VAE_NEXANS_30d;
EXECUTE p_VAE_NEXANS_30d;
SELECT 'VAE-NEXANS-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VAE_NEXANS_30d;

SELECT '########## SUTTONS ##########' AS marke;
SELECT '=== VAE-SUTTONS-1h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-12-30 03:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''SUTTONS'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_VAE_SUTTONS_1h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VAE_SUTTONS_1h;
EXECUTE p_VAE_SUTTONS_1h;
EXECUTE p_VAE_SUTTONS_1h;
EXECUTE p_VAE_SUTTONS_1h;
EXECUTE p_VAE_SUTTONS_1h;
EXECUTE p_VAE_SUTTONS_1h;
SELECT 'VAE-SUTTONS-1h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VAE_SUTTONS_1h;
SELECT '=== VAE-SUTTONS-24h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-12-29 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''SUTTONS'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_VAE_SUTTONS_24h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VAE_SUTTONS_24h;
EXECUTE p_VAE_SUTTONS_24h;
EXECUTE p_VAE_SUTTONS_24h;
EXECUTE p_VAE_SUTTONS_24h;
EXECUTE p_VAE_SUTTONS_24h;
EXECUTE p_VAE_SUTTONS_24h;
SELECT 'VAE-SUTTONS-24h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VAE_SUTTONS_24h;
SELECT '=== VAE-SUTTONS-7d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-12-23 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''SUTTONS'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_VAE_SUTTONS_7d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VAE_SUTTONS_7d;
EXECUTE p_VAE_SUTTONS_7d;
EXECUTE p_VAE_SUTTONS_7d;
EXECUTE p_VAE_SUTTONS_7d;
EXECUTE p_VAE_SUTTONS_7d;
EXECUTE p_VAE_SUTTONS_7d;
SELECT 'VAE-SUTTONS-7d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VAE_SUTTONS_7d;
SELECT '=== VAE-SUTTONS-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''SUTTONS'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_VAE_SUTTONS_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VAE_SUTTONS_30d;
EXECUTE p_VAE_SUTTONS_30d;
EXECUTE p_VAE_SUTTONS_30d;
EXECUTE p_VAE_SUTTONS_30d;
EXECUTE p_VAE_SUTTONS_30d;
EXECUTE p_VAE_SUTTONS_30d;
SELECT 'VAE-SUTTONS-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VAE_SUTTONS_30d;

SELECT '########## VOTG ##########' AS marke;
SELECT '=== VAE-VOTG-1h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-12-30 03:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''VOTG'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_VAE_VOTG_1h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VAE_VOTG_1h;
EXECUTE p_VAE_VOTG_1h;
EXECUTE p_VAE_VOTG_1h;
EXECUTE p_VAE_VOTG_1h;
EXECUTE p_VAE_VOTG_1h;
EXECUTE p_VAE_VOTG_1h;
SELECT 'VAE-VOTG-1h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VAE_VOTG_1h;
SELECT '=== VAE-VOTG-24h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-12-29 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''VOTG'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_VAE_VOTG_24h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VAE_VOTG_24h;
EXECUTE p_VAE_VOTG_24h;
EXECUTE p_VAE_VOTG_24h;
EXECUTE p_VAE_VOTG_24h;
EXECUTE p_VAE_VOTG_24h;
EXECUTE p_VAE_VOTG_24h;
SELECT 'VAE-VOTG-24h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VAE_VOTG_24h;
SELECT '=== VAE-VOTG-7d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-12-23 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''VOTG'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_VAE_VOTG_7d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VAE_VOTG_7d;
EXECUTE p_VAE_VOTG_7d;
EXECUTE p_VAE_VOTG_7d;
EXECUTE p_VAE_VOTG_7d;
EXECUTE p_VAE_VOTG_7d;
EXECUTE p_VAE_VOTG_7d;
SELECT 'VAE-VOTG-7d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VAE_VOTG_7d;
SELECT '=== VAE-VOTG-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''VOTG'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_VAE_VOTG_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VAE_VOTG_30d;
EXECUTE p_VAE_VOTG_30d;
EXECUTE p_VAE_VOTG_30d;
EXECUTE p_VAE_VOTG_30d;
EXECUTE p_VAE_VOTG_30d;
EXECUTE p_VAE_VOTG_30d;
SELECT 'VAE-VOTG-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VAE_VOTG_30d;

SELECT '########## IBIS ##########' AS marke;
SELECT '=== VAE-IBIS-1h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-12-30 03:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''IBIS'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_VAE_IBIS_1h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VAE_IBIS_1h;
EXECUTE p_VAE_IBIS_1h;
EXECUTE p_VAE_IBIS_1h;
EXECUTE p_VAE_IBIS_1h;
EXECUTE p_VAE_IBIS_1h;
EXECUTE p_VAE_IBIS_1h;
SELECT 'VAE-IBIS-1h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VAE_IBIS_1h;
SELECT '=== VAE-IBIS-24h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-12-29 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''IBIS'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_VAE_IBIS_24h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VAE_IBIS_24h;
EXECUTE p_VAE_IBIS_24h;
EXECUTE p_VAE_IBIS_24h;
EXECUTE p_VAE_IBIS_24h;
EXECUTE p_VAE_IBIS_24h;
EXECUTE p_VAE_IBIS_24h;
SELECT 'VAE-IBIS-24h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VAE_IBIS_24h;
SELECT '=== VAE-IBIS-7d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-12-23 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''IBIS'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_VAE_IBIS_7d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VAE_IBIS_7d;
EXECUTE p_VAE_IBIS_7d;
EXECUTE p_VAE_IBIS_7d;
EXECUTE p_VAE_IBIS_7d;
EXECUTE p_VAE_IBIS_7d;
EXECUTE p_VAE_IBIS_7d;
SELECT 'VAE-IBIS-7d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VAE_IBIS_7d;
SELECT '=== VAE-IBIS-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''IBIS'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_VAE_IBIS_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VAE_IBIS_30d;
EXECUTE p_VAE_IBIS_30d;
EXECUTE p_VAE_IBIS_30d;
EXECUTE p_VAE_IBIS_30d;
EXECUTE p_VAE_IBIS_30d;
EXECUTE p_VAE_IBIS_30d;
SELECT 'VAE-IBIS-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VAE_IBIS_30d;

SELECT '########## IBISGUS ##########' AS marke;
SELECT '=== VAE-IBISGUS-1h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-12-30 03:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''IBISGUS'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_VAE_IBISGUS_1h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VAE_IBISGUS_1h;
EXECUTE p_VAE_IBISGUS_1h;
EXECUTE p_VAE_IBISGUS_1h;
EXECUTE p_VAE_IBISGUS_1h;
EXECUTE p_VAE_IBISGUS_1h;
EXECUTE p_VAE_IBISGUS_1h;
SELECT 'VAE-IBISGUS-1h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VAE_IBISGUS_1h;
SELECT '=== VAE-IBISGUS-24h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-12-29 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''IBISGUS'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_VAE_IBISGUS_24h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VAE_IBISGUS_24h;
EXECUTE p_VAE_IBISGUS_24h;
EXECUTE p_VAE_IBISGUS_24h;
EXECUTE p_VAE_IBISGUS_24h;
EXECUTE p_VAE_IBISGUS_24h;
EXECUTE p_VAE_IBISGUS_24h;
SELECT 'VAE-IBISGUS-24h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VAE_IBISGUS_24h;
SELECT '=== VAE-IBISGUS-7d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-12-23 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''IBISGUS'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_VAE_IBISGUS_7d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VAE_IBISGUS_7d;
EXECUTE p_VAE_IBISGUS_7d;
EXECUTE p_VAE_IBISGUS_7d;
EXECUTE p_VAE_IBISGUS_7d;
EXECUTE p_VAE_IBISGUS_7d;
EXECUTE p_VAE_IBISGUS_7d;
SELECT 'VAE-IBISGUS-7d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VAE_IBISGUS_7d;
SELECT '=== VAE-IBISGUS-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''IBISGUS'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_VAE_IBISGUS_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VAE_IBISGUS_30d;
EXECUTE p_VAE_IBISGUS_30d;
EXECUTE p_VAE_IBISGUS_30d;
EXECUTE p_VAE_IBISGUS_30d;
EXECUTE p_VAE_IBISGUS_30d;
EXECUTE p_VAE_IBISGUS_30d;
SELECT 'VAE-IBISGUS-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VAE_IBISGUS_30d;

SELECT '########## ZAST ##########' AS marke;
SELECT '=== VAE-ZAST-1h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-12-30 03:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''ZAST'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_VAE_ZAST_1h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VAE_ZAST_1h;
EXECUTE p_VAE_ZAST_1h;
EXECUTE p_VAE_ZAST_1h;
EXECUTE p_VAE_ZAST_1h;
EXECUTE p_VAE_ZAST_1h;
EXECUTE p_VAE_ZAST_1h;
SELECT 'VAE-ZAST-1h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VAE_ZAST_1h;
SELECT '=== VAE-ZAST-24h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-12-29 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''ZAST'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_VAE_ZAST_24h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VAE_ZAST_24h;
EXECUTE p_VAE_ZAST_24h;
EXECUTE p_VAE_ZAST_24h;
EXECUTE p_VAE_ZAST_24h;
EXECUTE p_VAE_ZAST_24h;
EXECUTE p_VAE_ZAST_24h;
SELECT 'VAE-ZAST-24h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VAE_ZAST_24h;
SELECT '=== VAE-ZAST-7d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-12-23 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''ZAST'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_VAE_ZAST_7d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VAE_ZAST_7d;
EXECUTE p_VAE_ZAST_7d;
EXECUTE p_VAE_ZAST_7d;
EXECUTE p_VAE_ZAST_7d;
EXECUTE p_VAE_ZAST_7d;
EXECUTE p_VAE_ZAST_7d;
SELECT 'VAE-ZAST-7d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VAE_ZAST_7d;
SELECT '=== VAE-ZAST-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''ZAST'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_VAE_ZAST_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VAE_ZAST_30d;
EXECUTE p_VAE_ZAST_30d;
EXECUTE p_VAE_ZAST_30d;
EXECUTE p_VAE_ZAST_30d;
EXECUTE p_VAE_ZAST_30d;
EXECUTE p_VAE_ZAST_30d;
SELECT 'VAE-ZAST-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VAE_ZAST_30d;

SELECT '########## WOC ##########' AS marke;
SELECT '=== VAE-WOC-1h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-12-30 03:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''WOC'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_VAE_WOC_1h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VAE_WOC_1h;
EXECUTE p_VAE_WOC_1h;
EXECUTE p_VAE_WOC_1h;
EXECUTE p_VAE_WOC_1h;
EXECUTE p_VAE_WOC_1h;
EXECUTE p_VAE_WOC_1h;
SELECT 'VAE-WOC-1h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VAE_WOC_1h;
SELECT '=== VAE-WOC-24h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-12-29 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''WOC'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_VAE_WOC_24h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VAE_WOC_24h;
EXECUTE p_VAE_WOC_24h;
EXECUTE p_VAE_WOC_24h;
EXECUTE p_VAE_WOC_24h;
EXECUTE p_VAE_WOC_24h;
EXECUTE p_VAE_WOC_24h;
SELECT 'VAE-WOC-24h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VAE_WOC_24h;
SELECT '=== VAE-WOC-7d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-12-23 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''WOC'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_VAE_WOC_7d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VAE_WOC_7d;
EXECUTE p_VAE_WOC_7d;
EXECUTE p_VAE_WOC_7d;
EXECUTE p_VAE_WOC_7d;
EXECUTE p_VAE_WOC_7d;
EXECUTE p_VAE_WOC_7d;
SELECT 'VAE-WOC-7d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VAE_WOC_7d;
SELECT '=== VAE-WOC-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''WOC'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_VAE_WOC_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VAE_WOC_30d;
EXECUTE p_VAE_WOC_30d;
EXECUTE p_VAE_WOC_30d;
EXECUTE p_VAE_WOC_30d;
EXECUTE p_VAE_WOC_30d;
EXECUTE p_VAE_WOC_30d;
SELECT 'VAE-WOC-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VAE_WOC_30d;

SELECT '########## SYSTEM ##########' AS marke;
SELECT '=== VAE-SYSTEM-1h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-12-30 03:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''SYSTEM'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_VAE_SYSTEM_1h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VAE_SYSTEM_1h;
EXECUTE p_VAE_SYSTEM_1h;
EXECUTE p_VAE_SYSTEM_1h;
EXECUTE p_VAE_SYSTEM_1h;
EXECUTE p_VAE_SYSTEM_1h;
EXECUTE p_VAE_SYSTEM_1h;
SELECT 'VAE-SYSTEM-1h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VAE_SYSTEM_1h;
SELECT '=== VAE-SYSTEM-24h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-12-29 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''SYSTEM'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_VAE_SYSTEM_24h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VAE_SYSTEM_24h;
EXECUTE p_VAE_SYSTEM_24h;
EXECUTE p_VAE_SYSTEM_24h;
EXECUTE p_VAE_SYSTEM_24h;
EXECUTE p_VAE_SYSTEM_24h;
EXECUTE p_VAE_SYSTEM_24h;
SELECT 'VAE-SYSTEM-24h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VAE_SYSTEM_24h;
SELECT '=== VAE-SYSTEM-7d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-12-23 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''SYSTEM'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_VAE_SYSTEM_7d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VAE_SYSTEM_7d;
EXECUTE p_VAE_SYSTEM_7d;
EXECUTE p_VAE_SYSTEM_7d;
EXECUTE p_VAE_SYSTEM_7d;
EXECUTE p_VAE_SYSTEM_7d;
EXECUTE p_VAE_SYSTEM_7d;
SELECT 'VAE-SYSTEM-7d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VAE_SYSTEM_7d;
SELECT '=== VAE-SYSTEM-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''SYSTEM'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_VAE_SYSTEM_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VAE_SYSTEM_30d;
EXECUTE p_VAE_SYSTEM_30d;
EXECUTE p_VAE_SYSTEM_30d;
EXECUTE p_VAE_SYSTEM_30d;
EXECUTE p_VAE_SYSTEM_30d;
EXECUTE p_VAE_SYSTEM_30d;
SELECT 'VAE-SYSTEM-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VAE_SYSTEM_30d;

SELECT '########## NXHBE ##########' AS marke;
SELECT '=== VAE-NXHBE-1h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-12-30 03:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''NXHBE'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_VAE_NXHBE_1h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VAE_NXHBE_1h;
EXECUTE p_VAE_NXHBE_1h;
EXECUTE p_VAE_NXHBE_1h;
EXECUTE p_VAE_NXHBE_1h;
EXECUTE p_VAE_NXHBE_1h;
EXECUTE p_VAE_NXHBE_1h;
SELECT 'VAE-NXHBE-1h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VAE_NXHBE_1h;
SELECT '=== VAE-NXHBE-24h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-12-29 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''NXHBE'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_VAE_NXHBE_24h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VAE_NXHBE_24h;
EXECUTE p_VAE_NXHBE_24h;
EXECUTE p_VAE_NXHBE_24h;
EXECUTE p_VAE_NXHBE_24h;
EXECUTE p_VAE_NXHBE_24h;
EXECUTE p_VAE_NXHBE_24h;
SELECT 'VAE-NXHBE-24h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VAE_NXHBE_24h;
SELECT '=== VAE-NXHBE-7d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-12-23 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''NXHBE'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_VAE_NXHBE_7d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VAE_NXHBE_7d;
EXECUTE p_VAE_NXHBE_7d;
EXECUTE p_VAE_NXHBE_7d;
EXECUTE p_VAE_NXHBE_7d;
EXECUTE p_VAE_NXHBE_7d;
EXECUTE p_VAE_NXHBE_7d;
SELECT 'VAE-NXHBE-7d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VAE_NXHBE_7d;
SELECT '=== VAE-NXHBE-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''NXHBE'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_VAE_NXHBE_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VAE_NXHBE_30d;
EXECUTE p_VAE_NXHBE_30d;
EXECUTE p_VAE_NXHBE_30d;
EXECUTE p_VAE_NXHBE_30d;
EXECUTE p_VAE_NXHBE_30d;
EXECUTE p_VAE_NXHBE_30d;
SELECT 'VAE-NXHBE-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VAE_NXHBE_30d;

SELECT '########## EDITIONLINGERI ##########' AS marke;
SELECT '=== VAE-EDITIONLINGERI-1h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-12-30 03:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''EDITIONLINGERI'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_VAE_EDITIONLINGERI_1h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VAE_EDITIONLINGERI_1h;
EXECUTE p_VAE_EDITIONLINGERI_1h;
EXECUTE p_VAE_EDITIONLINGERI_1h;
EXECUTE p_VAE_EDITIONLINGERI_1h;
EXECUTE p_VAE_EDITIONLINGERI_1h;
EXECUTE p_VAE_EDITIONLINGERI_1h;
SELECT 'VAE-EDITIONLINGERI-1h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VAE_EDITIONLINGERI_1h;
SELECT '=== VAE-EDITIONLINGERI-24h - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-12-29 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''EDITIONLINGERI'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_VAE_EDITIONLINGERI_24h FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VAE_EDITIONLINGERI_24h;
EXECUTE p_VAE_EDITIONLINGERI_24h;
EXECUTE p_VAE_EDITIONLINGERI_24h;
EXECUTE p_VAE_EDITIONLINGERI_24h;
EXECUTE p_VAE_EDITIONLINGERI_24h;
EXECUTE p_VAE_EDITIONLINGERI_24h;
SELECT 'VAE-EDITIONLINGERI-24h' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VAE_EDITIONLINGERI_24h;
SELECT '=== VAE-EDITIONLINGERI-7d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-12-23 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''EDITIONLINGERI'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_VAE_EDITIONLINGERI_7d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VAE_EDITIONLINGERI_7d;
EXECUTE p_VAE_EDITIONLINGERI_7d;
EXECUTE p_VAE_EDITIONLINGERI_7d;
EXECUTE p_VAE_EDITIONLINGERI_7d;
EXECUTE p_VAE_EDITIONLINGERI_7d;
EXECUTE p_VAE_EDITIONLINGERI_7d;
SELECT 'VAE-EDITIONLINGERI-7d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VAE_EDITIONLINGERI_7d;
SELECT '=== VAE-EDITIONLINGERI-30d - Ergebnis + Aufwaermlauf + fuenf (L7) ===' AS marke;
SET @s = 'SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= ''2025-11-30 04:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                            WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''EDITIONLINGERI'')
              GROUP BY r.stunde ) s ) g';
PREPARE p_VAE_EDITIONLINGERI_30d FROM @s;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE p_VAE_EDITIONLINGERI_30d;
EXECUTE p_VAE_EDITIONLINGERI_30d;
EXECUTE p_VAE_EDITIONLINGERI_30d;
EXECUTE p_VAE_EDITIONLINGERI_30d;
EXECUTE p_VAE_EDITIONLINGERI_30d;
EXECUTE p_VAE_EDITIONLINGERI_30d;
SELECT 'VAE-EDITIONLINGERI-30d' AS fall,
       COUNT(*)                                               AS anzahl_laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END)                      AS aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END)                      AS beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t;
DEALLOCATE PREPARE p_VAE_EDITIONLINGERI_30d;

SELECT '=== 99 Abschluss ===' AS marke;
SELECT @@global.read_only AS read_only_am_ende, NOW() AS serverzeit_ende;
