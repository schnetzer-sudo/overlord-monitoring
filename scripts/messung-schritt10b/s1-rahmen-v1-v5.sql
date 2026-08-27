-- Messrunde M94-M98 — Sitzung 1: Rahmen, Nachweis Testkopie, Vorbedingungen V1 bis V5
-- Auftrag:  "Messrunde vor Schritt 10b", Stand 27.08.2026
-- Ergebnis: docs/messungen-schritt10b.md
--
-- Ausschliesslich SELECT / SET / SHOW. (Regel S1)
-- G1: keine Zugangsdaten, kein Hostname, kein Partnername, keine ProcessID in dieser Datei.

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, VERSION() AS version, NOW() AS serverzeit;

SELECT '=== 01 Benutzer ===' AS marke;
SELECT USER() AS benutzer, CURRENT_USER() AS aufgeloest;

SELECT '=== 02 Rahmenwerte ===' AS marke;
SELECT @@div_precision_increment     AS dpi,
       @@session.sql_mode            AS sql_mode,
       @@profiling_history_size      AS profiling_history_size_vorgabe,
       @@max_statement_time          AS max_statement_time_vorgabe,
       @@innodb_buffer_pool_size     AS buffer_pool_size,
       @@time_zone                   AS time_zone,
       @@system_time_zone            AS system_time_zone,
       @@global.character_set_server AS cs_server,
       @@global.collation_server     AS coll_server;

SET max_statement_time = 60;
SET profiling = 1;
SET profiling_history_size = 100;

-- ── V1 — Ist die Testkopie seit dem 26.08.2026 unveraendert? ─────────────────
SELECT '=== V1 Tabellengroessen (gegen messungen-schritt10.md V1) ===' AS marke;
SELECT TABLE_NAME,
       ENGINE,
       TABLE_ROWS,
       DATA_LENGTH,
       INDEX_LENGTH,
       DATA_LENGTH + INDEX_LENGTH AS gesamt_bytes,
       ROUND((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024, 1) AS gesamt_mib,
       DATA_FREE,
       AVG_ROW_LENGTH,
       UPDATE_TIME,
       CREATE_TIME,
       TABLE_COLLATION
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'GlassfishDB'
  AND TABLE_NAME IN ('Message','MessageAction','MessageBAM','MessageProperty')
ORDER BY TABLE_NAME;

SELECT '=== V1b Zeilenzahl Message, gezaehlt (nicht geschaetzt) ===' AS marke;
-- L9: Bestandskontrolle, keine Messung. Ohne Zeitfenster, weil genau der
-- Gesamtbestand die Bezugsgroesse ist. Kein Vorbild fuer Anwendungscode.
SELECT COUNT(*) AS message_zeilen FROM GlassfishDB.Message;

-- ── V3 — Wie steht der Katalog heute? ───────────────────────────────────────
-- Bezugsgroesse fuer M95 und M98. Die Zahlen aus M91 und M93-0 sind ueberholt.
SELECT '=== V3a process_catalog Gesamtstand ===' AS marke;
SELECT COUNT(*)                                                  AS zeilen,
       SUM(pflegestatus = 'GEPFLEGT')                            AS gepflegt,
       SUM(pflegestatus = 'OFFEN')                               AS offen,
       SUM(partner IS NOT NULL AND partner <> '')                AS mit_partner,
       SUM(pflegestatus = 'GEPFLEGT'
           AND partner IS NOT NULL AND partner <> '')            AS gepflegt_mit_partner,
       SUM(richtung IS NOT NULL AND richtung <> '')              AS mit_richtung,
       SUM(pflegestatus = 'GEPFLEGT'
           AND richtung IS NOT NULL AND richtung <> '')           AS gepflegt_mit_richtung,
       COUNT(DISTINCT partner)                                   AS versch_partner
FROM overlord_monitor.process_catalog;

SELECT '=== V3b process_catalog je Mandant ===' AS marke;
SELECT pm.MandantID                                                AS mandant,
       COUNT(*)                                                    AS prozesse,
       SUM(c.process_id IS NOT NULL)                               AS mit_katalogzeile,
       SUM(c.pflegestatus = 'GEPFLEGT')                            AS gepflegt,
       SUM(c.pflegestatus = 'GEPFLEGT'
           AND c.partner IS NOT NULL AND c.partner <> '')          AS gepflegt_mit_partner,
       SUM(c.pflegestatus = 'GEPFLEGT'
           AND c.richtung IS NOT NULL AND c.richtung <> '')        AS gepflegt_mit_richtung,
       SUM(c.pflegestatus = 'OFFEN')                               AS offen,
       COUNT(DISTINCT CASE WHEN c.pflegestatus = 'GEPFLEGT'
                            AND c.partner IS NOT NULL
                            AND c.partner <> '' THEN c.partner END) AS versch_partner
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
GROUP BY pm.MandantID
ORDER BY prozesse DESC;

SELECT '=== V3c Richtungswerte im Katalog ===' AS marke;
SELECT COALESCE(richtung, '(NULL)') AS richtung, COUNT(*) AS zeilen
FROM overlord_monitor.process_catalog
GROUP BY COALESCE(richtung, '(NULL)')
ORDER BY zeilen DESC;

-- ── V4 — Ist message_rollup gefuellt und wie? ───────────────────────────────
SELECT '=== V4a message_rollup Stand ===' AS marke;
SELECT COUNT(*)                     AS zeilen,
       SUM(anzahl)                  AS summe_anzahl,
       COUNT(DISTINCT process_id)   AS versch_prozesse,
       COUNT(DISTINCT message_status) AS versch_status,
       MIN(stunde)                  AS fruehste_stunde,
       MAX(stunde)                  AS spaeteste_stunde
FROM overlord_monitor.message_rollup;

SELECT '=== V4b message_rollup Groesse ===' AS marke;
SELECT TABLE_NAME, TABLE_ROWS, DATA_LENGTH, INDEX_LENGTH,
       ROUND((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024, 2) AS gesamt_mib,
       TABLE_COLLATION
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'overlord_monitor'
  AND TABLE_NAME IN ('message_rollup','rollup_lauf');

SELECT '=== V4c rollup_lauf, die letzten zehn Zeilen ===' AS marke;
SELECT id, art, fenster_von, fenster_bis, gestartet_am, beendet_am,
       zeilen_geschrieben,
       CASE WHEN fehler IS NULL THEN 'nein' ELSE 'ja' END AS mit_fehler,
       TIMESTAMPDIFF(MICROSECOND, gestartet_am, beendet_am) / 1000 AS dauer_ms
FROM overlord_monitor.rollup_lauf
ORDER BY id DESC
LIMIT 10;

SELECT '=== V4d Wasserstand (wie RollupSchreibRepository.wasserstand) ===' AS marke;
SELECT MAX(fenster_bis) AS wasserstand
FROM overlord_monitor.rollup_lauf
WHERE beendet_am IS NOT NULL AND fehler IS NULL;

-- ── V5 — Der Ankerzeitpunkt ─────────────────────────────────────────────────
-- Woertlich die Abfrage aus docs/datenzugriff.md §6. Einzige Aenderung: das
-- vorangestellte USE, damit die unqualifizierten Tabellennamen aufloesen.
SELECT '=== V5 Anker: juengster Tag mit mindestens drei Mandanten ===' AS marke;
USE GlassfishDB;
select max(m.MessageLastUpdate)
from Message m
join Process p on p.ProcessID = m.ProcessID
join ProjectMandant pm on pm.ProjectID = p.ProjectID
group by date(m.MessageLastUpdate)
having count(distinct pm.MandantID) >= 3
order by date(m.MessageLastUpdate) desc
limit 1;

SELECT '=== V5b Gegenprobe: MAX und MIN des Bestands ===' AS marke;
-- L9: Bestandskontrolle ueber Indexspitzen, kein Zeilendurchlauf. Kein Vorbild.
SELECT MAX(MessageLastUpdate) AS max_lastupdate,
       MIN(MessageLastUpdate) AS min_lastupdate
FROM GlassfishDB.Message;

SELECT '=== 99 Laufzeiten ===' AS marke;
SHOW PROFILES;

SELECT '=== 99 read_only am Ende der Sitzung ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;
