-- Messung M188 -- Sitzung 1: Tor 1, die Lesung
-- Auftrag:  "Fehler live, Teil A (Baustein und Uebersicht)", Stand 18.09.2026
-- Vorregistrierung und Ergebnis: docs/fehler-live.md Paragraf 8
--
-- ERZEUGT von scripts/messung-fehler-live/erzeuge.py. Die Statementtexte sind aus
-- dem Code gerendert; eingesetzt sind nur Fenster und Mandant.
--
-- Ausschliesslich SELECT / SET / EXPLAIN. (Regel S1) Benutzer: der Lese-Benutzer.
-- G1: keine Zugangsdaten, kein Hostname. Mandanten-IDs sind Codes. Die
--     Rohausgabe traegt Prozesskennungen und bleibt deshalb ausserhalb des
--     Repositorys (ergebnis/*.txt); eingecheckt wird das gefilterte Protokoll.
-- L9: SET max_statement_time = 60 fuer diese Sitzung.
-- T1: Zeiten werden ausgegeben, nichts wird zugesichert.

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit, VERSION() AS version,
       @@max_statement_time AS grenze_vorher;

SET max_statement_time = 60;
SET profiling = 1;
SET profiling_history_size = 100;

SELECT '=== 00a Eichung: Wanduhr um ein leeres Statement ===' AS marke;
SET @t0 = SYSDATE(6); SELECT 1; SET @e1 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6); SELECT 1; SET @e2 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6); SELECT 1; SET @e3 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SELECT 'M188-EICHUNG-WAND' AS fall, ROUND(@e1 / 1000, 3) AS ms1, ROUND(@e2 / 1000, 3) AS ms2,
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
SELECT 'M188-EICHUNG-ZAEHLER' AS fall,
       @handler_read_key_1 - @handler_read_key_0 AS read_key,
       @handler_read_next_1 - @handler_read_next_0 AS read_next,
       @handler_read_prev_1 - @handler_read_prev_0 AS read_prev,
       @handler_read_first_1 - @handler_read_first_0 AS read_first,
       @handler_read_rnd_1 - @handler_read_rnd_0 AS read_rnd,
       @handler_read_rnd_next_1 - @handler_read_rnd_next_0 AS read_rnd_next,
       @handler_icp_attempts_1 - @handler_icp_attempts_0 AS icp_attempts,
       @handler_icp_match_1 - @handler_icp_match_0 AS icp_match;

SELECT '=== 01 Die Zaehlung: Fehlerzeilen und Nachrichten je Mandant, ganzer Bestand ===' AS marke;
SELECT md.MandantID AS mandant,
       (SELECT COUNT(*) FROM `GlassfishDB`.`Message` m
         WHERE (m.MessageStatus LIKE 'ERROR\\_%' ESCAPE '\\' OR m.MessageStatus = 'COMMIT_REJECTED')
           AND EXISTS (SELECT 1 FROM `GlassfishDB`.`Process` p
                         JOIN `GlassfishDB`.`ProjectMandant` pm ON pm.ProjectID = p.ProjectID
                        WHERE p.ProcessID = m.ProcessID AND pm.MandantID = md.MandantID))
         AS fehlerzeilen,
       (SELECT COALESCE(SUM(r.anzahl), 0) FROM `overlord_monitor`.`message_rollup` r
         WHERE EXISTS (SELECT 1 FROM `GlassfishDB`.`Process` p
                         JOIN `GlassfishDB`.`ProjectMandant` pm ON pm.ProjectID = p.ProjectID
                        WHERE p.ProcessID = r.process_id AND pm.MandantID = md.MandantID))
         AS nachrichten
FROM `GlassfishDB`.`Mandant` md
ORDER BY fehlerzeilen, nachrichten DESC, md.MandantID;

SELECT '=== 01a Die Deckung: message_rollup gegen Message, ganzer Bestand ===' AS marke;
SELECT (SELECT SUM(anzahl) FROM `overlord_monitor`.`message_rollup`) AS rollup_gesamt,
       (SELECT COUNT(*) FROM `GlassfishDB`.`Message`) AS message_gesamt;

SELECT '=== 01b Die Fehlerzeilen im ganzen Bestand, Rohwert x Einordnung ===' AS marke;
SELECT m.MessageStatus AS rohwert, COUNT(*) AS anzahl
FROM `GlassfishDB`.`Message` m
WHERE m.MessageStatus LIKE 'ERROR\\_%' ESCAPE '\\' OR m.MessageStatus = 'COMMIT_REJECTED'
GROUP BY m.MessageStatus ORDER BY m.MessageStatus;

SELECT '=== M188-1-NEXANS-48H · Plan (Regel L15) ===' AS marke;
EXPLAIN select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-28 05:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-30 05:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SELECT '=== M188-1-NEXANS-48H · Handler-Zaehler, ein Lauf ===' AS marke;
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
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-28 05:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-30 05:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
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
SELECT 'M188-1-NEXANS-48H-ZAEHLER' AS fall,
       @handler_read_key_1 - @handler_read_key_0 AS read_key,
       @handler_read_next_1 - @handler_read_next_0 AS read_next,
       @handler_read_prev_1 - @handler_read_prev_0 AS read_prev,
       @handler_read_first_1 - @handler_read_first_0 AS read_first,
       @handler_read_rnd_1 - @handler_read_rnd_0 AS read_rnd,
       @handler_read_rnd_next_1 - @handler_read_rnd_next_0 AS read_rnd_next,
       @handler_icp_attempts_1 - @handler_icp_attempts_0 AS icp_attempts,
       @handler_icp_match_1 - @handler_icp_match_0 AS icp_match;
SELECT '=== M188-1-NEXANS-48H · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-28 05:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-30 05:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w1 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-28 05:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-30 05:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w2 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-28 05:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-30 05:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w3 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-28 05:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-30 05:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w4 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-28 05:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-30 05:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w5 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-28 05:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-30 05:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w6 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SELECT 'M188-1-NEXANS-48H' AS fall,
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

SELECT '=== M188-1-NEXANS-30T · Plan (Regel L15) ===' AS marke;
EXPLAIN select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-31 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SELECT '=== M188-1-NEXANS-30T · Handler-Zaehler, ein Lauf ===' AS marke;
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
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-31 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
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
SELECT 'M188-1-NEXANS-30T-ZAEHLER' AS fall,
       @handler_read_key_1 - @handler_read_key_0 AS read_key,
       @handler_read_next_1 - @handler_read_next_0 AS read_next,
       @handler_read_prev_1 - @handler_read_prev_0 AS read_prev,
       @handler_read_first_1 - @handler_read_first_0 AS read_first,
       @handler_read_rnd_1 - @handler_read_rnd_0 AS read_rnd,
       @handler_read_rnd_next_1 - @handler_read_rnd_next_0 AS read_rnd_next,
       @handler_icp_attempts_1 - @handler_icp_attempts_0 AS icp_attempts,
       @handler_icp_match_1 - @handler_icp_match_0 AS icp_match;
SELECT '=== M188-1-NEXANS-30T · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-31 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w1 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-31 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w2 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-31 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w3 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-31 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w4 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-31 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w5 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-31 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w6 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SELECT 'M188-1-NEXANS-30T' AS fall,
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

SELECT '=== M188-1-NEXANS-12M · Plan (Regel L15) ===' AS marke;
EXPLAIN select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-01-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2026-01-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SELECT '=== M188-1-NEXANS-12M · Handler-Zaehler, ein Lauf ===' AS marke;
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
SELECT 'M188-1-NEXANS-12M-ZAEHLER' AS fall,
       @handler_read_key_1 - @handler_read_key_0 AS read_key,
       @handler_read_next_1 - @handler_read_next_0 AS read_next,
       @handler_read_prev_1 - @handler_read_prev_0 AS read_prev,
       @handler_read_first_1 - @handler_read_first_0 AS read_first,
       @handler_read_rnd_1 - @handler_read_rnd_0 AS read_rnd,
       @handler_read_rnd_next_1 - @handler_read_rnd_next_0 AS read_rnd_next,
       @handler_icp_attempts_1 - @handler_icp_attempts_0 AS icp_attempts,
       @handler_icp_match_1 - @handler_icp_match_0 AS icp_match;
SELECT '=== M188-1-NEXANS-12M · sechs Laeufe ===' AS marke;
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
SELECT 'M188-1-NEXANS-12M' AS fall,
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

SELECT '=== M188-1-SUTTONS-48H · Plan (Regel L15) ===' AS marke;
EXPLAIN select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-28 05:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-30 05:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SELECT '=== M188-1-SUTTONS-48H · Handler-Zaehler, ein Lauf ===' AS marke;
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
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-28 05:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-30 05:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
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
SELECT 'M188-1-SUTTONS-48H-ZAEHLER' AS fall,
       @handler_read_key_1 - @handler_read_key_0 AS read_key,
       @handler_read_next_1 - @handler_read_next_0 AS read_next,
       @handler_read_prev_1 - @handler_read_prev_0 AS read_prev,
       @handler_read_first_1 - @handler_read_first_0 AS read_first,
       @handler_read_rnd_1 - @handler_read_rnd_0 AS read_rnd,
       @handler_read_rnd_next_1 - @handler_read_rnd_next_0 AS read_rnd_next,
       @handler_icp_attempts_1 - @handler_icp_attempts_0 AS icp_attempts,
       @handler_icp_match_1 - @handler_icp_match_0 AS icp_match;
SELECT '=== M188-1-SUTTONS-48H · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-28 05:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-30 05:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w1 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-28 05:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-30 05:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w2 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-28 05:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-30 05:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w3 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-28 05:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-30 05:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w4 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-28 05:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-30 05:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w5 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-28 05:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-30 05:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w6 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SELECT 'M188-1-SUTTONS-48H' AS fall,
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

SELECT '=== M188-1-SUTTONS-30T · Plan (Regel L15) ===' AS marke;
EXPLAIN select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-31 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SELECT '=== M188-1-SUTTONS-30T · Handler-Zaehler, ein Lauf ===' AS marke;
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
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-31 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
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
SELECT 'M188-1-SUTTONS-30T-ZAEHLER' AS fall,
       @handler_read_key_1 - @handler_read_key_0 AS read_key,
       @handler_read_next_1 - @handler_read_next_0 AS read_next,
       @handler_read_prev_1 - @handler_read_prev_0 AS read_prev,
       @handler_read_first_1 - @handler_read_first_0 AS read_first,
       @handler_read_rnd_1 - @handler_read_rnd_0 AS read_rnd,
       @handler_read_rnd_next_1 - @handler_read_rnd_next_0 AS read_rnd_next,
       @handler_icp_attempts_1 - @handler_icp_attempts_0 AS icp_attempts,
       @handler_icp_match_1 - @handler_icp_match_0 AS icp_match;
SELECT '=== M188-1-SUTTONS-30T · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-31 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w1 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-31 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w2 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-31 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w3 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-31 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w4 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-31 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w5 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-31 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w6 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SELECT 'M188-1-SUTTONS-30T' AS fall,
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

SELECT '=== M188-1-SUTTONS-12M · Plan (Regel L15) ===' AS marke;
EXPLAIN select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-01-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2026-01-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'SUTTONS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SELECT '=== M188-1-SUTTONS-12M · Handler-Zaehler, ein Lauf ===' AS marke;
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
SELECT 'M188-1-SUTTONS-12M-ZAEHLER' AS fall,
       @handler_read_key_1 - @handler_read_key_0 AS read_key,
       @handler_read_next_1 - @handler_read_next_0 AS read_next,
       @handler_read_prev_1 - @handler_read_prev_0 AS read_prev,
       @handler_read_first_1 - @handler_read_first_0 AS read_first,
       @handler_read_rnd_1 - @handler_read_rnd_0 AS read_rnd,
       @handler_read_rnd_next_1 - @handler_read_rnd_next_0 AS read_rnd_next,
       @handler_icp_attempts_1 - @handler_icp_attempts_0 AS icp_attempts,
       @handler_icp_match_1 - @handler_icp_match_0 AS icp_match;
SELECT '=== M188-1-SUTTONS-12M · sechs Laeufe ===' AS marke;
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
SELECT 'M188-1-SUTTONS-12M' AS fall,
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

SELECT '=== M188-1-VOTG-48H · Plan (Regel L15) ===' AS marke;
EXPLAIN select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-28 05:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-30 05:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'VOTG'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SELECT '=== M188-1-VOTG-48H · Handler-Zaehler, ein Lauf ===' AS marke;
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
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-28 05:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-30 05:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'VOTG'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
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
SELECT 'M188-1-VOTG-48H-ZAEHLER' AS fall,
       @handler_read_key_1 - @handler_read_key_0 AS read_key,
       @handler_read_next_1 - @handler_read_next_0 AS read_next,
       @handler_read_prev_1 - @handler_read_prev_0 AS read_prev,
       @handler_read_first_1 - @handler_read_first_0 AS read_first,
       @handler_read_rnd_1 - @handler_read_rnd_0 AS read_rnd,
       @handler_read_rnd_next_1 - @handler_read_rnd_next_0 AS read_rnd_next,
       @handler_icp_attempts_1 - @handler_icp_attempts_0 AS icp_attempts,
       @handler_icp_match_1 - @handler_icp_match_0 AS icp_match;
SELECT '=== M188-1-VOTG-48H · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-28 05:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-30 05:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'VOTG'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w1 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-28 05:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-30 05:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'VOTG'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w2 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-28 05:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-30 05:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'VOTG'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w3 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-28 05:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-30 05:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'VOTG'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w4 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-28 05:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-30 05:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'VOTG'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w5 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-28 05:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-30 05:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'VOTG'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w6 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SELECT 'M188-1-VOTG-48H' AS fall,
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

SELECT '=== M188-1-VOTG-30T · Plan (Regel L15) ===' AS marke;
EXPLAIN select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-31 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'VOTG'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SELECT '=== M188-1-VOTG-30T · Handler-Zaehler, ein Lauf ===' AS marke;
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
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-31 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'VOTG'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
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
SELECT 'M188-1-VOTG-30T-ZAEHLER' AS fall,
       @handler_read_key_1 - @handler_read_key_0 AS read_key,
       @handler_read_next_1 - @handler_read_next_0 AS read_next,
       @handler_read_prev_1 - @handler_read_prev_0 AS read_prev,
       @handler_read_first_1 - @handler_read_first_0 AS read_first,
       @handler_read_rnd_1 - @handler_read_rnd_0 AS read_rnd,
       @handler_read_rnd_next_1 - @handler_read_rnd_next_0 AS read_rnd_next,
       @handler_icp_attempts_1 - @handler_icp_attempts_0 AS icp_attempts,
       @handler_icp_match_1 - @handler_icp_match_0 AS icp_match;
SELECT '=== M188-1-VOTG-30T · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-31 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'VOTG'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w1 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-31 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'VOTG'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w2 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-31 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'VOTG'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w3 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-31 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'VOTG'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w4 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-31 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'VOTG'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w5 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-31 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'VOTG'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w6 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SELECT 'M188-1-VOTG-30T' AS fall,
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

SELECT '=== M188-1-VOTG-12M · Plan (Regel L15) ===' AS marke;
EXPLAIN select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-01-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2026-01-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'VOTG'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SELECT '=== M188-1-VOTG-12M · Handler-Zaehler, ein Lauf ===' AS marke;
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
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-01-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2026-01-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'VOTG'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
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
SELECT 'M188-1-VOTG-12M-ZAEHLER' AS fall,
       @handler_read_key_1 - @handler_read_key_0 AS read_key,
       @handler_read_next_1 - @handler_read_next_0 AS read_next,
       @handler_read_prev_1 - @handler_read_prev_0 AS read_prev,
       @handler_read_first_1 - @handler_read_first_0 AS read_first,
       @handler_read_rnd_1 - @handler_read_rnd_0 AS read_rnd,
       @handler_read_rnd_next_1 - @handler_read_rnd_next_0 AS read_rnd_next,
       @handler_icp_attempts_1 - @handler_icp_attempts_0 AS icp_attempts,
       @handler_icp_match_1 - @handler_icp_match_0 AS icp_match;
SELECT '=== M188-1-VOTG-12M · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-01-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2026-01-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'VOTG'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w1 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-01-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2026-01-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'VOTG'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w2 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-01-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2026-01-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'VOTG'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w3 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-01-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2026-01-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'VOTG'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w4 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-01-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2026-01-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'VOTG'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w5 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-01-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2026-01-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'VOTG'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w6 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SELECT 'M188-1-VOTG-12M' AS fall,
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

SELECT '=== M188-1-IBIS-48H · Plan (Regel L15) ===' AS marke;
EXPLAIN select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-28 05:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-30 05:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'IBIS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SELECT '=== M188-1-IBIS-48H · Handler-Zaehler, ein Lauf ===' AS marke;
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
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-28 05:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-30 05:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'IBIS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
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
SELECT 'M188-1-IBIS-48H-ZAEHLER' AS fall,
       @handler_read_key_1 - @handler_read_key_0 AS read_key,
       @handler_read_next_1 - @handler_read_next_0 AS read_next,
       @handler_read_prev_1 - @handler_read_prev_0 AS read_prev,
       @handler_read_first_1 - @handler_read_first_0 AS read_first,
       @handler_read_rnd_1 - @handler_read_rnd_0 AS read_rnd,
       @handler_read_rnd_next_1 - @handler_read_rnd_next_0 AS read_rnd_next,
       @handler_icp_attempts_1 - @handler_icp_attempts_0 AS icp_attempts,
       @handler_icp_match_1 - @handler_icp_match_0 AS icp_match;
SELECT '=== M188-1-IBIS-48H · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-28 05:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-30 05:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'IBIS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w1 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-28 05:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-30 05:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'IBIS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w2 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-28 05:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-30 05:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'IBIS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w3 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-28 05:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-30 05:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'IBIS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w4 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-28 05:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-30 05:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'IBIS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w5 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-28 05:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-30 05:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'IBIS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w6 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SELECT 'M188-1-IBIS-48H' AS fall,
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

SELECT '=== M188-1-IBIS-30T · Plan (Regel L15) ===' AS marke;
EXPLAIN select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-31 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'IBIS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SELECT '=== M188-1-IBIS-30T · Handler-Zaehler, ein Lauf ===' AS marke;
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
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-31 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'IBIS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
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
SELECT 'M188-1-IBIS-30T-ZAEHLER' AS fall,
       @handler_read_key_1 - @handler_read_key_0 AS read_key,
       @handler_read_next_1 - @handler_read_next_0 AS read_next,
       @handler_read_prev_1 - @handler_read_prev_0 AS read_prev,
       @handler_read_first_1 - @handler_read_first_0 AS read_first,
       @handler_read_rnd_1 - @handler_read_rnd_0 AS read_rnd,
       @handler_read_rnd_next_1 - @handler_read_rnd_next_0 AS read_rnd_next,
       @handler_icp_attempts_1 - @handler_icp_attempts_0 AS icp_attempts,
       @handler_icp_match_1 - @handler_icp_match_0 AS icp_match;
SELECT '=== M188-1-IBIS-30T · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-31 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'IBIS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w1 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-31 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'IBIS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w2 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-31 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'IBIS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w3 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-31 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'IBIS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w4 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-31 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'IBIS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w5 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-12-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-31 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'IBIS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w6 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SELECT 'M188-1-IBIS-30T' AS fall,
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

SELECT '=== M188-1-IBIS-12M · Plan (Regel L15) ===' AS marke;
EXPLAIN select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-01-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2026-01-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'IBIS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SELECT '=== M188-1-IBIS-12M · Handler-Zaehler, ein Lauf ===' AS marke;
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
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-01-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2026-01-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'IBIS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
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
SELECT 'M188-1-IBIS-12M-ZAEHLER' AS fall,
       @handler_read_key_1 - @handler_read_key_0 AS read_key,
       @handler_read_next_1 - @handler_read_next_0 AS read_next,
       @handler_read_prev_1 - @handler_read_prev_0 AS read_prev,
       @handler_read_first_1 - @handler_read_first_0 AS read_first,
       @handler_read_rnd_1 - @handler_read_rnd_0 AS read_rnd,
       @handler_read_rnd_next_1 - @handler_read_rnd_next_0 AS read_rnd_next,
       @handler_icp_attempts_1 - @handler_icp_attempts_0 AS icp_attempts,
       @handler_icp_match_1 - @handler_icp_match_0 AS icp_match;
SELECT '=== M188-1-IBIS-12M · sechs Laeufe ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-01-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2026-01-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'IBIS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w1 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-01-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2026-01-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'IBIS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w2 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-01-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2026-01-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'IBIS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w3 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-01-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2026-01-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'IBIS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w4 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-01-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2026-01-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'IBIS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w5 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6);
select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*) from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED') and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '2025-01-01 00:00:00.0' and `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2026-01-01 00:00:00.0' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'IBIS'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`;
SET @w6 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SELECT 'M188-1-IBIS-12M' AS fall,
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
