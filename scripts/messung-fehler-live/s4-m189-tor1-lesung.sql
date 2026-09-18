-- Messung M189 -- Sitzung 4: Tor 1, die Lesung im Jahresfenster aus Sitzung 3
-- Auftrag:  "Fehler live, Teil B (Prozessbaum)", Stand 18.09.2026
-- Vorregistrierung und Ergebnis: docs/fehler-live.md Paragraf 5b
--
-- ERZEUGT von scripts/messung-fehler-live/erzeuge_m189.py. Die Lesung ist aus dem Code
-- gerendert; eingesetzt sind nur Fenster und Mandant.
--
-- Ausschliesslich SELECT / SET / EXPLAIN. (Regel S1) Benutzer: der Lese-Benutzer.
-- G1: keine Zugangsdaten, kein Hostname. Mandanten-IDs sind Codes. Die
--     Rohausgabe traegt Prozesskennungen und bleibt deshalb ausserhalb des
--     Repositorys (ergebnis/s[0-9].txt); eingecheckt wird das gefilterte Protokoll.
-- L9: SET max_statement_time = 60 fuer diese Sitzung.
-- T1: Zeiten werden ausgegeben, nichts wird zugesichert.

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit, VERSION() AS version,
       @@max_statement_time AS grenze_vorher;

SET max_statement_time = 60;
SET profiling = 1;
SET profiling_history_size = 100;

SELECT '=== 01 Die Zaehlung: das Jahresfenster mit den meisten NEXANS-Fehlern ===' AS marke;
WITH je_stunde AS (
  SELECT CAST(DATE_FORMAT(m.MessageLastUpdate, '%Y-%m-%d %H:00:00') AS DATETIME) AS stunde,
         COUNT(*) AS fehler
  FROM `GlassfishDB`.`Message` m
  WHERE (m.MessageStatus LIKE 'ERROR\\_%' ESCAPE '\\' OR m.MessageStatus = 'COMMIT_REJECTED')
    AND EXISTS (SELECT 1 FROM `GlassfishDB`.`Process` p
                  JOIN `GlassfishDB`.`ProjectMandant` pm ON pm.ProjectID = p.ProjectID
                 WHERE p.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
  GROUP BY CAST(DATE_FORMAT(m.MessageLastUpdate, '%Y-%m-%d %H:00:00') AS DATETIME)
)
SELECT a.stunde AS von, a.stunde + INTERVAL 1 YEAR AS bis_ausschliessend,
       SUM(b.fehler) AS fehler_im_fenster, COUNT(*) AS fehlerstunden_im_fenster
FROM je_stunde a
JOIN je_stunde b ON b.stunde >= a.stunde AND b.stunde < a.stunde + INTERVAL 1 YEAR
GROUP BY a.stunde
ORDER BY fehler_im_fenster DESC, a.stunde ASC
LIMIT 5;

SELECT '=== 01a Eichung der Zaehlung: zwei Zahlen aus M188 ===' AS marke;
SELECT 'M189-ZAEHLUNG-EICHUNG' AS fall,
       COUNT(*) AS fehler_nexans_ganzer_bestand,
       SUM(m.MessageLastUpdate >= '2023-11-01 00:00:00' AND m.MessageLastUpdate < '2024-11-01 00:00:00')
         AS fehler_bis_2024_11_01,
       COUNT(DISTINCT DATE_FORMAT(m.MessageLastUpdate, '%Y-%m-%d %H:00:00')) AS fehlerstunden
FROM `GlassfishDB`.`Message` m
WHERE (m.MessageStatus LIKE 'ERROR\\_%' ESCAPE '\\' OR m.MessageStatus = 'COMMIT_REJECTED')
  AND EXISTS (SELECT 1 FROM `GlassfishDB`.`Process` p
                JOIN `GlassfishDB`.`ProjectMandant` pm ON pm.ProjectID = p.ProjectID
               WHERE p.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS');

SELECT '=== 00a Eichung: Wanduhr um ein leeres Statement ===' AS marke;
SET @t0 = SYSDATE(6); SELECT 1; SET @e1 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6); SELECT 1; SET @e2 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6); SELECT 1; SET @e3 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SELECT 'M189-EICHUNG-WAND' AS fall, ROUND(@e1 / 1000, 3) AS ms1, ROUND(@e2 / 1000, 3) AS ms2,
       ROUND(@e3 / 1000, 3) AS ms3;

SELECT '=== 00b Eichung: Handler-Zaehler um ein leeres Statement ===' AS marke;
SELECT SUM(IF(VARIABLE_NAME = 'HANDLER_READ_KEY', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_NEXT', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_PREV', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_FIRST', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_RND', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_RND_NEXT', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_ICP_MATCH', VARIABLE_VALUE, 0))
INTO @handler_read_key_0, @handler_read_next_0, @handler_read_prev_0, @handler_read_first_0, @handler_read_rnd_0, @handler_read_rnd_next_0, @handler_icp_attempts_0, @handler_icp_match_0
FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME IN ('HANDLER_READ_KEY', 'HANDLER_READ_NEXT', 'HANDLER_READ_PREV', 'HANDLER_READ_FIRST', 'HANDLER_READ_RND', 'HANDLER_READ_RND_NEXT', 'HANDLER_ICP_ATTEMPTS', 'HANDLER_ICP_MATCH');
SELECT 1;
SELECT SUM(IF(VARIABLE_NAME = 'HANDLER_READ_KEY', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_NEXT', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_PREV', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_FIRST', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_RND', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_RND_NEXT', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_ICP_MATCH', VARIABLE_VALUE, 0))
INTO @handler_read_key_1, @handler_read_next_1, @handler_read_prev_1, @handler_read_first_1, @handler_read_rnd_1, @handler_read_rnd_next_1, @handler_icp_attempts_1, @handler_icp_match_1
FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME IN ('HANDLER_READ_KEY', 'HANDLER_READ_NEXT', 'HANDLER_READ_PREV', 'HANDLER_READ_FIRST', 'HANDLER_READ_RND', 'HANDLER_READ_RND_NEXT', 'HANDLER_ICP_ATTEMPTS', 'HANDLER_ICP_MATCH');
SELECT 'M189-EICHUNG-ZAEHLER' AS fall,
       @handler_read_key_1 - @handler_read_key_0 AS read_key,
       @handler_read_next_1 - @handler_read_next_0 AS read_next,
       @handler_read_prev_1 - @handler_read_prev_0 AS read_prev,
       @handler_read_first_1 - @handler_read_first_0 AS read_first,
       @handler_read_rnd_1 - @handler_read_rnd_0 AS read_rnd,
       @handler_read_rnd_next_1 - @handler_read_rnd_next_0 AS read_rnd_next,
       @handler_icp_attempts_1 - @handler_icp_attempts_0 AS icp_attempts,
       @handler_icp_match_1 - @handler_icp_match_0 AS icp_match;

SELECT '=== 02 Tor 1: das Fenster aus Sitzung 3, 2024-10-04 11:00:00.0 bis 2025-10-04 11:00:00.0 ===' AS marke;

SELECT '=== M189-1-NEXANS-JAHRESFENSTER · Plan (Regel L15) ===' AS marke;
EXPLAIN select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2024-10-04 11:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-10-04 11:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SELECT '=== M189-1-NEXANS-JAHRESFENSTER · Handler-Zaehler, ein Lauf ===' AS marke;
SELECT SUM(IF(VARIABLE_NAME = 'HANDLER_READ_KEY', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_NEXT', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_PREV', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_FIRST', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_RND', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_RND_NEXT', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_ICP_MATCH', VARIABLE_VALUE, 0))
INTO @handler_read_key_0, @handler_read_next_0, @handler_read_prev_0, @handler_read_first_0, @handler_read_rnd_0, @handler_read_rnd_next_0, @handler_icp_attempts_0, @handler_icp_match_0
FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME IN ('HANDLER_READ_KEY', 'HANDLER_READ_NEXT', 'HANDLER_READ_PREV', 'HANDLER_READ_FIRST', 'HANDLER_READ_RND', 'HANDLER_READ_RND_NEXT', 'HANDLER_ICP_ATTEMPTS', 'HANDLER_ICP_MATCH');
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2024-10-04 11:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-10-04 11:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SELECT SUM(IF(VARIABLE_NAME = 'HANDLER_READ_KEY', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_NEXT', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_PREV', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_FIRST', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_RND', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_RND_NEXT', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_ICP_MATCH', VARIABLE_VALUE, 0))
INTO @handler_read_key_1, @handler_read_next_1, @handler_read_prev_1, @handler_read_first_1, @handler_read_rnd_1, @handler_read_rnd_next_1, @handler_icp_attempts_1, @handler_icp_match_1
FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME IN ('HANDLER_READ_KEY', 'HANDLER_READ_NEXT', 'HANDLER_READ_PREV', 'HANDLER_READ_FIRST', 'HANDLER_READ_RND', 'HANDLER_READ_RND_NEXT', 'HANDLER_ICP_ATTEMPTS', 'HANDLER_ICP_MATCH');
SELECT 'M189-1-NEXANS-JAHRESFENSTER-ZAEHLER' AS fall,
       @handler_read_key_1 - @handler_read_key_0 AS read_key,
       @handler_read_next_1 - @handler_read_next_0 AS read_next,
       @handler_read_prev_1 - @handler_read_prev_0 AS read_prev,
       @handler_read_first_1 - @handler_read_first_0 AS read_first,
       @handler_read_rnd_1 - @handler_read_rnd_0 AS read_rnd,
       @handler_read_rnd_next_1 - @handler_read_rnd_next_0 AS read_rnd_next,
       @handler_icp_attempts_1 - @handler_icp_attempts_0 AS icp_attempts,
       @handler_icp_match_1 - @handler_icp_match_0 AS icp_match;
SELECT '=== M189-1-NEXANS-JAHRESFENSTER · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2024-10-04 11:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-10-04 11:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w1 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2024-10-04 11:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-10-04 11:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w2 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2024-10-04 11:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-10-04 11:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w3 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2024-10-04 11:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-10-04 11:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w4 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2024-10-04 11:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-10-04 11:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w5 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2024-10-04 11:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-10-04 11:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w6 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SELECT 'M189-1-NEXANS-JAHRESFENSTER' AS fall,
       COUNT(*)                          AS laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS profil_aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS profil_beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS profil_alle_fuenf,
       ROUND(@w1 / 1000, 3)              AS wand_aufwaermlauf,
       ROUND(LEAST(@w2, @w3, @w4, @w5, @w6) / 1000, 3) AS wand_beste_von_fuenf,
       CONCAT_WS('/', ROUND(@w2 / 1000, 3), ROUND(@w3 / 1000, 3), ROUND(@w4 / 1000, 3), ROUND(@w5 / 1000, 3), ROUND(@w6 / 1000, 3))       AS wand_alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 19
        AND (QUERY_ID - @basis) % 3 = 0
      GROUP BY QUERY_ID) t;

SELECT '=== 03 Zusaetzlich: die Fenster der Tore 2 bis 4 ===' AS marke;

SELECT '=== M189-Z-NEXANS-T2-typisch-48H · Plan (Regel L15) ===' AS marke;
EXPLAIN select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-11-07 17:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-11-09 17:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SELECT '=== M189-Z-NEXANS-T2-typisch-48H · Handler-Zaehler, ein Lauf ===' AS marke;
SELECT SUM(IF(VARIABLE_NAME = 'HANDLER_READ_KEY', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_NEXT', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_PREV', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_FIRST', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_RND', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_RND_NEXT', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_ICP_MATCH', VARIABLE_VALUE, 0))
INTO @handler_read_key_0, @handler_read_next_0, @handler_read_prev_0, @handler_read_first_0, @handler_read_rnd_0, @handler_read_rnd_next_0, @handler_icp_attempts_0, @handler_icp_match_0
FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME IN ('HANDLER_READ_KEY', 'HANDLER_READ_NEXT', 'HANDLER_READ_PREV', 'HANDLER_READ_FIRST', 'HANDLER_READ_RND', 'HANDLER_READ_RND_NEXT', 'HANDLER_ICP_ATTEMPTS', 'HANDLER_ICP_MATCH');
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-11-07 17:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-11-09 17:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SELECT SUM(IF(VARIABLE_NAME = 'HANDLER_READ_KEY', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_NEXT', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_PREV', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_FIRST', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_RND', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_RND_NEXT', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_ICP_MATCH', VARIABLE_VALUE, 0))
INTO @handler_read_key_1, @handler_read_next_1, @handler_read_prev_1, @handler_read_first_1, @handler_read_rnd_1, @handler_read_rnd_next_1, @handler_icp_attempts_1, @handler_icp_match_1
FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME IN ('HANDLER_READ_KEY', 'HANDLER_READ_NEXT', 'HANDLER_READ_PREV', 'HANDLER_READ_FIRST', 'HANDLER_READ_RND', 'HANDLER_READ_RND_NEXT', 'HANDLER_ICP_ATTEMPTS', 'HANDLER_ICP_MATCH');
SELECT 'M189-Z-NEXANS-T2-typisch-48H-ZAEHLER' AS fall,
       @handler_read_key_1 - @handler_read_key_0 AS read_key,
       @handler_read_next_1 - @handler_read_next_0 AS read_next,
       @handler_read_prev_1 - @handler_read_prev_0 AS read_prev,
       @handler_read_first_1 - @handler_read_first_0 AS read_first,
       @handler_read_rnd_1 - @handler_read_rnd_0 AS read_rnd,
       @handler_read_rnd_next_1 - @handler_read_rnd_next_0 AS read_rnd_next,
       @handler_icp_attempts_1 - @handler_icp_attempts_0 AS icp_attempts,
       @handler_icp_match_1 - @handler_icp_match_0 AS icp_match;
SELECT '=== M189-Z-NEXANS-T2-typisch-48H · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-11-07 17:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-11-09 17:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w1 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-11-07 17:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-11-09 17:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w2 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-11-07 17:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-11-09 17:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w3 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-11-07 17:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-11-09 17:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w4 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-11-07 17:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-11-09 17:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w5 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-11-07 17:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-11-09 17:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w6 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SELECT 'M189-Z-NEXANS-T2-typisch-48H' AS fall,
       COUNT(*)                          AS laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS profil_aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS profil_beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS profil_alle_fuenf,
       ROUND(@w1 / 1000, 3)              AS wand_aufwaermlauf,
       ROUND(LEAST(@w2, @w3, @w4, @w5, @w6) / 1000, 3) AS wand_beste_von_fuenf,
       CONCAT_WS('/', ROUND(@w2 / 1000, 3), ROUND(@w3 / 1000, 3), ROUND(@w4 / 1000, 3), ROUND(@w5 / 1000, 3), ROUND(@w6 / 1000, 3))       AS wand_alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 19
        AND (QUERY_ID - @basis) % 3 = 0
      GROUP BY QUERY_ID) t;

SELECT '=== M189-Z-NEXANS-T2-typisch-12M · Plan (Regel L15) ===' AS marke;
EXPLAIN select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2024-12-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SELECT '=== M189-Z-NEXANS-T2-typisch-12M · Handler-Zaehler, ein Lauf ===' AS marke;
SELECT SUM(IF(VARIABLE_NAME = 'HANDLER_READ_KEY', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_NEXT', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_PREV', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_FIRST', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_RND', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_RND_NEXT', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_ICP_MATCH', VARIABLE_VALUE, 0))
INTO @handler_read_key_0, @handler_read_next_0, @handler_read_prev_0, @handler_read_first_0, @handler_read_rnd_0, @handler_read_rnd_next_0, @handler_icp_attempts_0, @handler_icp_match_0
FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME IN ('HANDLER_READ_KEY', 'HANDLER_READ_NEXT', 'HANDLER_READ_PREV', 'HANDLER_READ_FIRST', 'HANDLER_READ_RND', 'HANDLER_READ_RND_NEXT', 'HANDLER_ICP_ATTEMPTS', 'HANDLER_ICP_MATCH');
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2024-12-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SELECT SUM(IF(VARIABLE_NAME = 'HANDLER_READ_KEY', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_NEXT', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_PREV', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_FIRST', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_RND', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_RND_NEXT', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_ICP_MATCH', VARIABLE_VALUE, 0))
INTO @handler_read_key_1, @handler_read_next_1, @handler_read_prev_1, @handler_read_first_1, @handler_read_rnd_1, @handler_read_rnd_next_1, @handler_icp_attempts_1, @handler_icp_match_1
FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME IN ('HANDLER_READ_KEY', 'HANDLER_READ_NEXT', 'HANDLER_READ_PREV', 'HANDLER_READ_FIRST', 'HANDLER_READ_RND', 'HANDLER_READ_RND_NEXT', 'HANDLER_ICP_ATTEMPTS', 'HANDLER_ICP_MATCH');
SELECT 'M189-Z-NEXANS-T2-typisch-12M-ZAEHLER' AS fall,
       @handler_read_key_1 - @handler_read_key_0 AS read_key,
       @handler_read_next_1 - @handler_read_next_0 AS read_next,
       @handler_read_prev_1 - @handler_read_prev_0 AS read_prev,
       @handler_read_first_1 - @handler_read_first_0 AS read_first,
       @handler_read_rnd_1 - @handler_read_rnd_0 AS read_rnd,
       @handler_read_rnd_next_1 - @handler_read_rnd_next_0 AS read_rnd_next,
       @handler_icp_attempts_1 - @handler_icp_attempts_0 AS icp_attempts,
       @handler_icp_match_1 - @handler_icp_match_0 AS icp_match;
SELECT '=== M189-Z-NEXANS-T2-typisch-12M · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2024-12-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w1 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2024-12-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w2 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2024-12-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w3 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2024-12-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w4 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2024-12-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w5 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2024-12-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w6 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SELECT 'M189-Z-NEXANS-T2-typisch-12M' AS fall,
       COUNT(*)                          AS laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS profil_aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS profil_beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS profil_alle_fuenf,
       ROUND(@w1 / 1000, 3)              AS wand_aufwaermlauf,
       ROUND(LEAST(@w2, @w3, @w4, @w5, @w6) / 1000, 3) AS wand_beste_von_fuenf,
       CONCAT_WS('/', ROUND(@w2 / 1000, 3), ROUND(@w3 / 1000, 3), ROUND(@w4 / 1000, 3), ROUND(@w5 / 1000, 3), ROUND(@w6 / 1000, 3))       AS wand_alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 19
        AND (QUERY_ID - @basis) % 3 = 0
      GROUP BY QUERY_ID) t;

SELECT '=== M189-Z-NEXANS-T3-dicht-48H · Plan (Regel L15) ===' AS marke;
EXPLAIN select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2024-10-07 22:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2024-10-09 22:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SELECT '=== M189-Z-NEXANS-T3-dicht-48H · Handler-Zaehler, ein Lauf ===' AS marke;
SELECT SUM(IF(VARIABLE_NAME = 'HANDLER_READ_KEY', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_NEXT', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_PREV', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_FIRST', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_RND', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_RND_NEXT', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_ICP_MATCH', VARIABLE_VALUE, 0))
INTO @handler_read_key_0, @handler_read_next_0, @handler_read_prev_0, @handler_read_first_0, @handler_read_rnd_0, @handler_read_rnd_next_0, @handler_icp_attempts_0, @handler_icp_match_0
FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME IN ('HANDLER_READ_KEY', 'HANDLER_READ_NEXT', 'HANDLER_READ_PREV', 'HANDLER_READ_FIRST', 'HANDLER_READ_RND', 'HANDLER_READ_RND_NEXT', 'HANDLER_ICP_ATTEMPTS', 'HANDLER_ICP_MATCH');
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2024-10-07 22:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2024-10-09 22:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SELECT SUM(IF(VARIABLE_NAME = 'HANDLER_READ_KEY', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_NEXT', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_PREV', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_FIRST', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_RND', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_RND_NEXT', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_ICP_MATCH', VARIABLE_VALUE, 0))
INTO @handler_read_key_1, @handler_read_next_1, @handler_read_prev_1, @handler_read_first_1, @handler_read_rnd_1, @handler_read_rnd_next_1, @handler_icp_attempts_1, @handler_icp_match_1
FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME IN ('HANDLER_READ_KEY', 'HANDLER_READ_NEXT', 'HANDLER_READ_PREV', 'HANDLER_READ_FIRST', 'HANDLER_READ_RND', 'HANDLER_READ_RND_NEXT', 'HANDLER_ICP_ATTEMPTS', 'HANDLER_ICP_MATCH');
SELECT 'M189-Z-NEXANS-T3-dicht-48H-ZAEHLER' AS fall,
       @handler_read_key_1 - @handler_read_key_0 AS read_key,
       @handler_read_next_1 - @handler_read_next_0 AS read_next,
       @handler_read_prev_1 - @handler_read_prev_0 AS read_prev,
       @handler_read_first_1 - @handler_read_first_0 AS read_first,
       @handler_read_rnd_1 - @handler_read_rnd_0 AS read_rnd,
       @handler_read_rnd_next_1 - @handler_read_rnd_next_0 AS read_rnd_next,
       @handler_icp_attempts_1 - @handler_icp_attempts_0 AS icp_attempts,
       @handler_icp_match_1 - @handler_icp_match_0 AS icp_match;
SELECT '=== M189-Z-NEXANS-T3-dicht-48H · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2024-10-07 22:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2024-10-09 22:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w1 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2024-10-07 22:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2024-10-09 22:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w2 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2024-10-07 22:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2024-10-09 22:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w3 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2024-10-07 22:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2024-10-09 22:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w4 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2024-10-07 22:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2024-10-09 22:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w5 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2024-10-07 22:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2024-10-09 22:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w6 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SELECT 'M189-Z-NEXANS-T3-dicht-48H' AS fall,
       COUNT(*)                          AS laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS profil_aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS profil_beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS profil_alle_fuenf,
       ROUND(@w1 / 1000, 3)              AS wand_aufwaermlauf,
       ROUND(LEAST(@w2, @w3, @w4, @w5, @w6) / 1000, 3) AS wand_beste_von_fuenf,
       CONCAT_WS('/', ROUND(@w2 / 1000, 3), ROUND(@w3 / 1000, 3), ROUND(@w4 / 1000, 3), ROUND(@w5 / 1000, 3), ROUND(@w6 / 1000, 3))       AS wand_alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 19
        AND (QUERY_ID - @basis) % 3 = 0
      GROUP BY QUERY_ID) t;

SELECT '=== M189-Z-NEXANS-T3-dicht-12M · Plan (Regel L15) ===' AS marke;
EXPLAIN select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2023-11-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2024-11-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SELECT '=== M189-Z-NEXANS-T3-dicht-12M · Handler-Zaehler, ein Lauf ===' AS marke;
SELECT SUM(IF(VARIABLE_NAME = 'HANDLER_READ_KEY', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_NEXT', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_PREV', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_FIRST', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_RND', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_RND_NEXT', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_ICP_MATCH', VARIABLE_VALUE, 0))
INTO @handler_read_key_0, @handler_read_next_0, @handler_read_prev_0, @handler_read_first_0, @handler_read_rnd_0, @handler_read_rnd_next_0, @handler_icp_attempts_0, @handler_icp_match_0
FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME IN ('HANDLER_READ_KEY', 'HANDLER_READ_NEXT', 'HANDLER_READ_PREV', 'HANDLER_READ_FIRST', 'HANDLER_READ_RND', 'HANDLER_READ_RND_NEXT', 'HANDLER_ICP_ATTEMPTS', 'HANDLER_ICP_MATCH');
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2023-11-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2024-11-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SELECT SUM(IF(VARIABLE_NAME = 'HANDLER_READ_KEY', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_NEXT', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_PREV', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_FIRST', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_RND', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_RND_NEXT', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_ICP_MATCH', VARIABLE_VALUE, 0))
INTO @handler_read_key_1, @handler_read_next_1, @handler_read_prev_1, @handler_read_first_1, @handler_read_rnd_1, @handler_read_rnd_next_1, @handler_icp_attempts_1, @handler_icp_match_1
FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME IN ('HANDLER_READ_KEY', 'HANDLER_READ_NEXT', 'HANDLER_READ_PREV', 'HANDLER_READ_FIRST', 'HANDLER_READ_RND', 'HANDLER_READ_RND_NEXT', 'HANDLER_ICP_ATTEMPTS', 'HANDLER_ICP_MATCH');
SELECT 'M189-Z-NEXANS-T3-dicht-12M-ZAEHLER' AS fall,
       @handler_read_key_1 - @handler_read_key_0 AS read_key,
       @handler_read_next_1 - @handler_read_next_0 AS read_next,
       @handler_read_prev_1 - @handler_read_prev_0 AS read_prev,
       @handler_read_first_1 - @handler_read_first_0 AS read_first,
       @handler_read_rnd_1 - @handler_read_rnd_0 AS read_rnd,
       @handler_read_rnd_next_1 - @handler_read_rnd_next_0 AS read_rnd_next,
       @handler_icp_attempts_1 - @handler_icp_attempts_0 AS icp_attempts,
       @handler_icp_match_1 - @handler_icp_match_0 AS icp_match;
SELECT '=== M189-Z-NEXANS-T3-dicht-12M · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2023-11-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2024-11-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w1 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2023-11-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2024-11-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w2 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2023-11-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2024-11-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w3 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2023-11-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2024-11-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w4 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2023-11-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2024-11-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w5 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2023-11-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2024-11-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w6 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SELECT 'M189-Z-NEXANS-T3-dicht-12M' AS fall,
       COUNT(*)                          AS laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS profil_aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS profil_beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS profil_alle_fuenf,
       ROUND(@w1 / 1000, 3)              AS wand_aufwaermlauf,
       ROUND(LEAST(@w2, @w3, @w4, @w5, @w6) / 1000, 3) AS wand_beste_von_fuenf,
       CONCAT_WS('/', ROUND(@w2 / 1000, 3), ROUND(@w3 / 1000, 3), ROUND(@w4 / 1000, 3), ROUND(@w5 / 1000, 3), ROUND(@w6 / 1000, 3))       AS wand_alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 19
        AND (QUERY_ID - @basis) % 3 = 0
      GROUP BY QUERY_ID) t;

SELECT '=== M189-Z-NEXANS-T4-jahr · Plan (Regel L15) ===' AS marke;
EXPLAIN select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-01-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2026-01-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SELECT '=== M189-Z-NEXANS-T4-jahr · Handler-Zaehler, ein Lauf ===' AS marke;
SELECT SUM(IF(VARIABLE_NAME = 'HANDLER_READ_KEY', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_NEXT', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_PREV', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_FIRST', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_RND', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_RND_NEXT', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_ICP_MATCH', VARIABLE_VALUE, 0))
INTO @handler_read_key_0, @handler_read_next_0, @handler_read_prev_0, @handler_read_first_0, @handler_read_rnd_0, @handler_read_rnd_next_0, @handler_icp_attempts_0, @handler_icp_match_0
FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME IN ('HANDLER_READ_KEY', 'HANDLER_READ_NEXT', 'HANDLER_READ_PREV', 'HANDLER_READ_FIRST', 'HANDLER_READ_RND', 'HANDLER_READ_RND_NEXT', 'HANDLER_ICP_ATTEMPTS', 'HANDLER_ICP_MATCH');
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-01-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2026-01-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SELECT SUM(IF(VARIABLE_NAME = 'HANDLER_READ_KEY', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_NEXT', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_PREV', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_FIRST', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_RND', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_RND_NEXT', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_ICP_MATCH', VARIABLE_VALUE, 0))
INTO @handler_read_key_1, @handler_read_next_1, @handler_read_prev_1, @handler_read_first_1, @handler_read_rnd_1, @handler_read_rnd_next_1, @handler_icp_attempts_1, @handler_icp_match_1
FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME IN ('HANDLER_READ_KEY', 'HANDLER_READ_NEXT', 'HANDLER_READ_PREV', 'HANDLER_READ_FIRST', 'HANDLER_READ_RND', 'HANDLER_READ_RND_NEXT', 'HANDLER_ICP_ATTEMPTS', 'HANDLER_ICP_MATCH');
SELECT 'M189-Z-NEXANS-T4-jahr-ZAEHLER' AS fall,
       @handler_read_key_1 - @handler_read_key_0 AS read_key,
       @handler_read_next_1 - @handler_read_next_0 AS read_next,
       @handler_read_prev_1 - @handler_read_prev_0 AS read_prev,
       @handler_read_first_1 - @handler_read_first_0 AS read_first,
       @handler_read_rnd_1 - @handler_read_rnd_0 AS read_rnd,
       @handler_read_rnd_next_1 - @handler_read_rnd_next_0 AS read_rnd_next,
       @handler_icp_attempts_1 - @handler_icp_attempts_0 AS icp_attempts,
       @handler_icp_match_1 - @handler_icp_match_0 AS icp_match;
SELECT '=== M189-Z-NEXANS-T4-jahr · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-01-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2026-01-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w1 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-01-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2026-01-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w2 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-01-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2026-01-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w3 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-01-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2026-01-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w4 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-01-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2026-01-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w5 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-01-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2026-01-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w6 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SELECT 'M189-Z-NEXANS-T4-jahr' AS fall,
       COUNT(*)                          AS laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS profil_aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS profil_beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS profil_alle_fuenf,
       ROUND(@w1 / 1000, 3)              AS wand_aufwaermlauf,
       ROUND(LEAST(@w2, @w3, @w4, @w5, @w6) / 1000, 3) AS wand_beste_von_fuenf,
       CONCAT_WS('/', ROUND(@w2 / 1000, 3), ROUND(@w3 / 1000, 3), ROUND(@w4 / 1000, 3), ROUND(@w5 / 1000, 3), ROUND(@w6 / 1000, 3))       AS wand_alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 19
        AND (QUERY_ID - @basis) % 3 = 0
      GROUP BY QUERY_ID) t;

SELECT '=== M189-Z-NEXANS-T4-boesfall · Plan (Regel L15) ===' AS marke;
EXPLAIN select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2024-12-30 14:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-30 03:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SELECT '=== M189-Z-NEXANS-T4-boesfall · Handler-Zaehler, ein Lauf ===' AS marke;
SELECT SUM(IF(VARIABLE_NAME = 'HANDLER_READ_KEY', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_NEXT', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_PREV', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_FIRST', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_RND', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_RND_NEXT', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_ICP_MATCH', VARIABLE_VALUE, 0))
INTO @handler_read_key_0, @handler_read_next_0, @handler_read_prev_0, @handler_read_first_0, @handler_read_rnd_0, @handler_read_rnd_next_0, @handler_icp_attempts_0, @handler_icp_match_0
FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME IN ('HANDLER_READ_KEY', 'HANDLER_READ_NEXT', 'HANDLER_READ_PREV', 'HANDLER_READ_FIRST', 'HANDLER_READ_RND', 'HANDLER_READ_RND_NEXT', 'HANDLER_ICP_ATTEMPTS', 'HANDLER_ICP_MATCH');
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2024-12-30 14:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-30 03:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SELECT SUM(IF(VARIABLE_NAME = 'HANDLER_READ_KEY', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_NEXT', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_PREV', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_FIRST', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_RND', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_RND_NEXT', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_ICP_MATCH', VARIABLE_VALUE, 0))
INTO @handler_read_key_1, @handler_read_next_1, @handler_read_prev_1, @handler_read_first_1, @handler_read_rnd_1, @handler_read_rnd_next_1, @handler_icp_attempts_1, @handler_icp_match_1
FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME IN ('HANDLER_READ_KEY', 'HANDLER_READ_NEXT', 'HANDLER_READ_PREV', 'HANDLER_READ_FIRST', 'HANDLER_READ_RND', 'HANDLER_READ_RND_NEXT', 'HANDLER_ICP_ATTEMPTS', 'HANDLER_ICP_MATCH');
SELECT 'M189-Z-NEXANS-T4-boesfall-ZAEHLER' AS fall,
       @handler_read_key_1 - @handler_read_key_0 AS read_key,
       @handler_read_next_1 - @handler_read_next_0 AS read_next,
       @handler_read_prev_1 - @handler_read_prev_0 AS read_prev,
       @handler_read_first_1 - @handler_read_first_0 AS read_first,
       @handler_read_rnd_1 - @handler_read_rnd_0 AS read_rnd,
       @handler_read_rnd_next_1 - @handler_read_rnd_next_0 AS read_rnd_next,
       @handler_icp_attempts_1 - @handler_icp_attempts_0 AS icp_attempts,
       @handler_icp_match_1 - @handler_icp_match_0 AS icp_match;
SELECT '=== M189-Z-NEXANS-T4-boesfall · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2024-12-30 14:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-30 03:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w1 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2024-12-30 14:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-30 03:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w2 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2024-12-30 14:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-30 03:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w3 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2024-12-30 14:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-30 03:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w4 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2024-12-30 14:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-30 03:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w5 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2024-12-30 14:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-30 03:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w6 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SELECT 'M189-Z-NEXANS-T4-boesfall' AS fall,
       COUNT(*)                          AS laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS profil_aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS profil_beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS profil_alle_fuenf,
       ROUND(@w1 / 1000, 3)              AS wand_aufwaermlauf,
       ROUND(LEAST(@w2, @w3, @w4, @w5, @w6) / 1000, 3) AS wand_beste_von_fuenf,
       CONCAT_WS('/', ROUND(@w2 / 1000, 3), ROUND(@w3 / 1000, 3), ROUND(@w4 / 1000, 3), ROUND(@w5 / 1000, 3), ROUND(@w6 / 1000, 3))       AS wand_alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 19
        AND (QUERY_ID - @basis) % 3 = 0
      GROUP BY QUERY_ID) t;

SELECT '=== M189-Z-SUTTONS-T2-typisch-48H · Plan (Regel L15) ===' AS marke;
EXPLAIN select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-06-07 21:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-06-09 21:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SELECT '=== M189-Z-SUTTONS-T2-typisch-48H · Handler-Zaehler, ein Lauf ===' AS marke;
SELECT SUM(IF(VARIABLE_NAME = 'HANDLER_READ_KEY', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_NEXT', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_PREV', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_FIRST', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_RND', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_RND_NEXT', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_ICP_MATCH', VARIABLE_VALUE, 0))
INTO @handler_read_key_0, @handler_read_next_0, @handler_read_prev_0, @handler_read_first_0, @handler_read_rnd_0, @handler_read_rnd_next_0, @handler_icp_attempts_0, @handler_icp_match_0
FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME IN ('HANDLER_READ_KEY', 'HANDLER_READ_NEXT', 'HANDLER_READ_PREV', 'HANDLER_READ_FIRST', 'HANDLER_READ_RND', 'HANDLER_READ_RND_NEXT', 'HANDLER_ICP_ATTEMPTS', 'HANDLER_ICP_MATCH');
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-06-07 21:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-06-09 21:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SELECT SUM(IF(VARIABLE_NAME = 'HANDLER_READ_KEY', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_NEXT', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_PREV', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_FIRST', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_RND', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_RND_NEXT', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_ICP_MATCH', VARIABLE_VALUE, 0))
INTO @handler_read_key_1, @handler_read_next_1, @handler_read_prev_1, @handler_read_first_1, @handler_read_rnd_1, @handler_read_rnd_next_1, @handler_icp_attempts_1, @handler_icp_match_1
FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME IN ('HANDLER_READ_KEY', 'HANDLER_READ_NEXT', 'HANDLER_READ_PREV', 'HANDLER_READ_FIRST', 'HANDLER_READ_RND', 'HANDLER_READ_RND_NEXT', 'HANDLER_ICP_ATTEMPTS', 'HANDLER_ICP_MATCH');
SELECT 'M189-Z-SUTTONS-T2-typisch-48H-ZAEHLER' AS fall,
       @handler_read_key_1 - @handler_read_key_0 AS read_key,
       @handler_read_next_1 - @handler_read_next_0 AS read_next,
       @handler_read_prev_1 - @handler_read_prev_0 AS read_prev,
       @handler_read_first_1 - @handler_read_first_0 AS read_first,
       @handler_read_rnd_1 - @handler_read_rnd_0 AS read_rnd,
       @handler_read_rnd_next_1 - @handler_read_rnd_next_0 AS read_rnd_next,
       @handler_icp_attempts_1 - @handler_icp_attempts_0 AS icp_attempts,
       @handler_icp_match_1 - @handler_icp_match_0 AS icp_match;
SELECT '=== M189-Z-SUTTONS-T2-typisch-48H · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-06-07 21:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-06-09 21:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w1 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-06-07 21:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-06-09 21:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w2 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-06-07 21:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-06-09 21:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w3 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-06-07 21:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-06-09 21:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w4 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-06-07 21:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-06-09 21:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w5 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-06-07 21:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-06-09 21:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w6 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SELECT 'M189-Z-SUTTONS-T2-typisch-48H' AS fall,
       COUNT(*)                          AS laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS profil_aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS profil_beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS profil_alle_fuenf,
       ROUND(@w1 / 1000, 3)              AS wand_aufwaermlauf,
       ROUND(LEAST(@w2, @w3, @w4, @w5, @w6) / 1000, 3) AS wand_beste_von_fuenf,
       CONCAT_WS('/', ROUND(@w2 / 1000, 3), ROUND(@w3 / 1000, 3), ROUND(@w4 / 1000, 3), ROUND(@w5 / 1000, 3), ROUND(@w6 / 1000, 3))       AS wand_alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 19
        AND (QUERY_ID - @basis) % 3 = 0
      GROUP BY QUERY_ID) t;

SELECT '=== M189-Z-SUTTONS-T2T3-12M · Plan (Regel L15) ===' AS marke;
EXPLAIN select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2024-07-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-07-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SELECT '=== M189-Z-SUTTONS-T2T3-12M · Handler-Zaehler, ein Lauf ===' AS marke;
SELECT SUM(IF(VARIABLE_NAME = 'HANDLER_READ_KEY', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_NEXT', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_PREV', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_FIRST', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_RND', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_RND_NEXT', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_ICP_MATCH', VARIABLE_VALUE, 0))
INTO @handler_read_key_0, @handler_read_next_0, @handler_read_prev_0, @handler_read_first_0, @handler_read_rnd_0, @handler_read_rnd_next_0, @handler_icp_attempts_0, @handler_icp_match_0
FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME IN ('HANDLER_READ_KEY', 'HANDLER_READ_NEXT', 'HANDLER_READ_PREV', 'HANDLER_READ_FIRST', 'HANDLER_READ_RND', 'HANDLER_READ_RND_NEXT', 'HANDLER_ICP_ATTEMPTS', 'HANDLER_ICP_MATCH');
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2024-07-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-07-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SELECT SUM(IF(VARIABLE_NAME = 'HANDLER_READ_KEY', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_NEXT', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_PREV', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_FIRST', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_RND', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_RND_NEXT', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_ICP_MATCH', VARIABLE_VALUE, 0))
INTO @handler_read_key_1, @handler_read_next_1, @handler_read_prev_1, @handler_read_first_1, @handler_read_rnd_1, @handler_read_rnd_next_1, @handler_icp_attempts_1, @handler_icp_match_1
FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME IN ('HANDLER_READ_KEY', 'HANDLER_READ_NEXT', 'HANDLER_READ_PREV', 'HANDLER_READ_FIRST', 'HANDLER_READ_RND', 'HANDLER_READ_RND_NEXT', 'HANDLER_ICP_ATTEMPTS', 'HANDLER_ICP_MATCH');
SELECT 'M189-Z-SUTTONS-T2T3-12M-ZAEHLER' AS fall,
       @handler_read_key_1 - @handler_read_key_0 AS read_key,
       @handler_read_next_1 - @handler_read_next_0 AS read_next,
       @handler_read_prev_1 - @handler_read_prev_0 AS read_prev,
       @handler_read_first_1 - @handler_read_first_0 AS read_first,
       @handler_read_rnd_1 - @handler_read_rnd_0 AS read_rnd,
       @handler_read_rnd_next_1 - @handler_read_rnd_next_0 AS read_rnd_next,
       @handler_icp_attempts_1 - @handler_icp_attempts_0 AS icp_attempts,
       @handler_icp_match_1 - @handler_icp_match_0 AS icp_match;
SELECT '=== M189-Z-SUTTONS-T2T3-12M · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2024-07-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-07-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w1 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2024-07-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-07-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w2 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2024-07-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-07-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w3 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2024-07-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-07-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w4 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2024-07-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-07-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w5 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2024-07-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-07-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w6 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SELECT 'M189-Z-SUTTONS-T2T3-12M' AS fall,
       COUNT(*)                          AS laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS profil_aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS profil_beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS profil_alle_fuenf,
       ROUND(@w1 / 1000, 3)              AS wand_aufwaermlauf,
       ROUND(LEAST(@w2, @w3, @w4, @w5, @w6) / 1000, 3) AS wand_beste_von_fuenf,
       CONCAT_WS('/', ROUND(@w2 / 1000, 3), ROUND(@w3 / 1000, 3), ROUND(@w4 / 1000, 3), ROUND(@w5 / 1000, 3), ROUND(@w6 / 1000, 3))       AS wand_alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 19
        AND (QUERY_ID - @basis) % 3 = 0
      GROUP BY QUERY_ID) t;

SELECT '=== M189-Z-SUTTONS-T3-dicht-48H · Plan (Regel L15) ===' AS marke;
EXPLAIN select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-06-10 11:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-06-12 11:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SELECT '=== M189-Z-SUTTONS-T3-dicht-48H · Handler-Zaehler, ein Lauf ===' AS marke;
SELECT SUM(IF(VARIABLE_NAME = 'HANDLER_READ_KEY', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_NEXT', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_PREV', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_FIRST', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_RND', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_RND_NEXT', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_ICP_MATCH', VARIABLE_VALUE, 0))
INTO @handler_read_key_0, @handler_read_next_0, @handler_read_prev_0, @handler_read_first_0, @handler_read_rnd_0, @handler_read_rnd_next_0, @handler_icp_attempts_0, @handler_icp_match_0
FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME IN ('HANDLER_READ_KEY', 'HANDLER_READ_NEXT', 'HANDLER_READ_PREV', 'HANDLER_READ_FIRST', 'HANDLER_READ_RND', 'HANDLER_READ_RND_NEXT', 'HANDLER_ICP_ATTEMPTS', 'HANDLER_ICP_MATCH');
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-06-10 11:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-06-12 11:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SELECT SUM(IF(VARIABLE_NAME = 'HANDLER_READ_KEY', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_NEXT', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_PREV', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_FIRST', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_RND', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_RND_NEXT', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_ICP_MATCH', VARIABLE_VALUE, 0))
INTO @handler_read_key_1, @handler_read_next_1, @handler_read_prev_1, @handler_read_first_1, @handler_read_rnd_1, @handler_read_rnd_next_1, @handler_icp_attempts_1, @handler_icp_match_1
FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME IN ('HANDLER_READ_KEY', 'HANDLER_READ_NEXT', 'HANDLER_READ_PREV', 'HANDLER_READ_FIRST', 'HANDLER_READ_RND', 'HANDLER_READ_RND_NEXT', 'HANDLER_ICP_ATTEMPTS', 'HANDLER_ICP_MATCH');
SELECT 'M189-Z-SUTTONS-T3-dicht-48H-ZAEHLER' AS fall,
       @handler_read_key_1 - @handler_read_key_0 AS read_key,
       @handler_read_next_1 - @handler_read_next_0 AS read_next,
       @handler_read_prev_1 - @handler_read_prev_0 AS read_prev,
       @handler_read_first_1 - @handler_read_first_0 AS read_first,
       @handler_read_rnd_1 - @handler_read_rnd_0 AS read_rnd,
       @handler_read_rnd_next_1 - @handler_read_rnd_next_0 AS read_rnd_next,
       @handler_icp_attempts_1 - @handler_icp_attempts_0 AS icp_attempts,
       @handler_icp_match_1 - @handler_icp_match_0 AS icp_match;
SELECT '=== M189-Z-SUTTONS-T3-dicht-48H · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-06-10 11:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-06-12 11:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w1 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-06-10 11:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-06-12 11:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w2 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-06-10 11:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-06-12 11:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w3 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-06-10 11:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-06-12 11:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w4 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-06-10 11:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-06-12 11:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w5 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-06-10 11:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-06-12 11:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w6 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SELECT 'M189-Z-SUTTONS-T3-dicht-48H' AS fall,
       COUNT(*)                          AS laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS profil_aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS profil_beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS profil_alle_fuenf,
       ROUND(@w1 / 1000, 3)              AS wand_aufwaermlauf,
       ROUND(LEAST(@w2, @w3, @w4, @w5, @w6) / 1000, 3) AS wand_beste_von_fuenf,
       CONCAT_WS('/', ROUND(@w2 / 1000, 3), ROUND(@w3 / 1000, 3), ROUND(@w4 / 1000, 3), ROUND(@w5 / 1000, 3), ROUND(@w6 / 1000, 3))       AS wand_alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 19
        AND (QUERY_ID - @basis) % 3 = 0
      GROUP BY QUERY_ID) t;

SELECT '=== M189-Z-SUTTONS-T4-jahr · Plan (Regel L15) ===' AS marke;
EXPLAIN select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-01-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2026-01-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SELECT '=== M189-Z-SUTTONS-T4-jahr · Handler-Zaehler, ein Lauf ===' AS marke;
SELECT SUM(IF(VARIABLE_NAME = 'HANDLER_READ_KEY', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_NEXT', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_PREV', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_FIRST', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_RND', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_RND_NEXT', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_ICP_MATCH', VARIABLE_VALUE, 0))
INTO @handler_read_key_0, @handler_read_next_0, @handler_read_prev_0, @handler_read_first_0, @handler_read_rnd_0, @handler_read_rnd_next_0, @handler_icp_attempts_0, @handler_icp_match_0
FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME IN ('HANDLER_READ_KEY', 'HANDLER_READ_NEXT', 'HANDLER_READ_PREV', 'HANDLER_READ_FIRST', 'HANDLER_READ_RND', 'HANDLER_READ_RND_NEXT', 'HANDLER_ICP_ATTEMPTS', 'HANDLER_ICP_MATCH');
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-01-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2026-01-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SELECT SUM(IF(VARIABLE_NAME = 'HANDLER_READ_KEY', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_NEXT', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_PREV', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_FIRST', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_RND', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_RND_NEXT', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_ICP_MATCH', VARIABLE_VALUE, 0))
INTO @handler_read_key_1, @handler_read_next_1, @handler_read_prev_1, @handler_read_first_1, @handler_read_rnd_1, @handler_read_rnd_next_1, @handler_icp_attempts_1, @handler_icp_match_1
FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME IN ('HANDLER_READ_KEY', 'HANDLER_READ_NEXT', 'HANDLER_READ_PREV', 'HANDLER_READ_FIRST', 'HANDLER_READ_RND', 'HANDLER_READ_RND_NEXT', 'HANDLER_ICP_ATTEMPTS', 'HANDLER_ICP_MATCH');
SELECT 'M189-Z-SUTTONS-T4-jahr-ZAEHLER' AS fall,
       @handler_read_key_1 - @handler_read_key_0 AS read_key,
       @handler_read_next_1 - @handler_read_next_0 AS read_next,
       @handler_read_prev_1 - @handler_read_prev_0 AS read_prev,
       @handler_read_first_1 - @handler_read_first_0 AS read_first,
       @handler_read_rnd_1 - @handler_read_rnd_0 AS read_rnd,
       @handler_read_rnd_next_1 - @handler_read_rnd_next_0 AS read_rnd_next,
       @handler_icp_attempts_1 - @handler_icp_attempts_0 AS icp_attempts,
       @handler_icp_match_1 - @handler_icp_match_0 AS icp_match;
SELECT '=== M189-Z-SUTTONS-T4-jahr · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-01-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2026-01-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w1 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-01-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2026-01-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w2 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-01-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2026-01-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w3 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-01-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2026-01-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w4 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-01-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2026-01-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w5 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-01-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2026-01-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w6 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SELECT 'M189-Z-SUTTONS-T4-jahr' AS fall,
       COUNT(*)                          AS laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS profil_aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS profil_beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS profil_alle_fuenf,
       ROUND(@w1 / 1000, 3)              AS wand_aufwaermlauf,
       ROUND(LEAST(@w2, @w3, @w4, @w5, @w6) / 1000, 3) AS wand_beste_von_fuenf,
       CONCAT_WS('/', ROUND(@w2 / 1000, 3), ROUND(@w3 / 1000, 3), ROUND(@w4 / 1000, 3), ROUND(@w5 / 1000, 3), ROUND(@w6 / 1000, 3))       AS wand_alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 19
        AND (QUERY_ID - @basis) % 3 = 0
      GROUP BY QUERY_ID) t;

SELECT '=== M189-Z-SUTTONS-T4-boesfall · Plan (Regel L15) ===' AS marke;
EXPLAIN select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2024-12-30 14:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-30 03:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SELECT '=== M189-Z-SUTTONS-T4-boesfall · Handler-Zaehler, ein Lauf ===' AS marke;
SELECT SUM(IF(VARIABLE_NAME = 'HANDLER_READ_KEY', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_NEXT', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_PREV', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_FIRST', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_RND', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_RND_NEXT', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_ICP_MATCH', VARIABLE_VALUE, 0))
INTO @handler_read_key_0, @handler_read_next_0, @handler_read_prev_0, @handler_read_first_0, @handler_read_rnd_0, @handler_read_rnd_next_0, @handler_icp_attempts_0, @handler_icp_match_0
FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME IN ('HANDLER_READ_KEY', 'HANDLER_READ_NEXT', 'HANDLER_READ_PREV', 'HANDLER_READ_FIRST', 'HANDLER_READ_RND', 'HANDLER_READ_RND_NEXT', 'HANDLER_ICP_ATTEMPTS', 'HANDLER_ICP_MATCH');
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2024-12-30 14:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-30 03:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SELECT SUM(IF(VARIABLE_NAME = 'HANDLER_READ_KEY', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_NEXT', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_PREV', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_FIRST', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_RND', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_READ_RND_NEXT', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_ICP_ATTEMPTS', VARIABLE_VALUE, 0)),
       SUM(IF(VARIABLE_NAME = 'HANDLER_ICP_MATCH', VARIABLE_VALUE, 0))
INTO @handler_read_key_1, @handler_read_next_1, @handler_read_prev_1, @handler_read_first_1, @handler_read_rnd_1, @handler_read_rnd_next_1, @handler_icp_attempts_1, @handler_icp_match_1
FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME IN ('HANDLER_READ_KEY', 'HANDLER_READ_NEXT', 'HANDLER_READ_PREV', 'HANDLER_READ_FIRST', 'HANDLER_READ_RND', 'HANDLER_READ_RND_NEXT', 'HANDLER_ICP_ATTEMPTS', 'HANDLER_ICP_MATCH');
SELECT 'M189-Z-SUTTONS-T4-boesfall-ZAEHLER' AS fall,
       @handler_read_key_1 - @handler_read_key_0 AS read_key,
       @handler_read_next_1 - @handler_read_next_0 AS read_next,
       @handler_read_prev_1 - @handler_read_prev_0 AS read_prev,
       @handler_read_first_1 - @handler_read_first_0 AS read_first,
       @handler_read_rnd_1 - @handler_read_rnd_0 AS read_rnd,
       @handler_read_rnd_next_1 - @handler_read_rnd_next_0 AS read_rnd_next,
       @handler_icp_attempts_1 - @handler_icp_attempts_0 AS icp_attempts,
       @handler_icp_match_1 - @handler_icp_match_0 AS icp_match;
SELECT '=== M189-Z-SUTTONS-T4-boesfall · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2024-12-30 14:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-30 03:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w1 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2024-12-30 14:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-30 03:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w2 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2024-12-30 14:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-30 03:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w3 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2024-12-30 14:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-30 03:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w4 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2024-12-30 14:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-30 03:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w5 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2024-12-30 14:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-30 03:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w6 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SELECT 'M189-Z-SUTTONS-T4-boesfall' AS fall,
       COUNT(*)                          AS laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS profil_aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS profil_beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS profil_alle_fuenf,
       ROUND(@w1 / 1000, 3)              AS wand_aufwaermlauf,
       ROUND(LEAST(@w2, @w3, @w4, @w5, @w6) / 1000, 3) AS wand_beste_von_fuenf,
       CONCAT_WS('/', ROUND(@w2 / 1000, 3), ROUND(@w3 / 1000, 3), ROUND(@w4 / 1000, 3), ROUND(@w5 / 1000, 3), ROUND(@w6 / 1000, 3))       AS wand_alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + 19
        AND (QUERY_ID - @basis) % 3 = 0
      GROUP BY QUERY_ID) t;
