-- Messrunde M86-M92 — Sitzung 1: Rahmen, Nachweis Testkopie, Vorbedingungen V1 bis V5
-- Auftrag:  "Messrunde vor Schritt 10a", Stand 24.08.2026
-- Ergebnis: docs/messungen-schritt10.md
--
-- Ausschliesslich SELECT / SET / SHOW. (Regel S1)
-- G1: keine Zugangsdaten, kein Hostname, kein Partnername, keine ProcessID in dieser Datei.

-- ── Nachweis: Ziel ist die Testkopie. Muss 1 liefern, sonst Abbruch. ──────────
SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, VERSION() AS version, NOW() AS serverzeit;

SELECT '=== 01 Benutzer ===' AS marke;
SELECT USER() AS benutzer, CURRENT_USER() AS aufgeloest;

SELECT '=== 02 Rahmenwerte ===' AS marke;
SELECT @@div_precision_increment   AS dpi,
       @@session.sql_mode          AS sql_mode,
       @@profiling_history_size    AS profiling_history_size_vorgabe,
       @@max_statement_time        AS max_statement_time_vorgabe,
       @@innodb_buffer_pool_size   AS buffer_pool_size,
       @@time_zone                 AS time_zone,
       @@system_time_zone          AS system_time_zone,
       @@global.character_set_server AS cs_server,
       @@global.collation_server   AS coll_server;

-- ── V1 — Ist die Testkopie seit dem 24.08.2026 unveraendert? ─────────────────
SELECT '=== V1 Tabellengroessen (gegen messungen-schritt7.md §0) ===' AS marke;
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

SELECT '=== V1b Zeilenzahlen gezaehlt (nicht geschaetzt) ===' AS marke;
SELECT (SELECT COUNT(*) FROM GlassfishDB.Message)         AS message_zeilen,
       (SELECT COUNT(*) FROM GlassfishDB.MessageAction)   AS messageaction_zeilen,
       (SELECT COUNT(*) FROM GlassfishDB.MessageBAM)      AS messagebam_zeilen;

-- ── V2 — Stand des Prozess-Katalogs ─────────────────────────────────────────
SELECT '=== V2 process_catalog Spalten ===' AS marke;
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT, EXTRA
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'overlord_monitor' AND TABLE_NAME = 'process_catalog'
ORDER BY ORDINAL_POSITION;

SELECT '=== V2b process_catalog Stand ===' AS marke;
SELECT COUNT(*)                                                            AS zeilen,
       COUNT(partner)                                                      AS partner_nicht_null,
       SUM(partner IS NOT NULL AND partner <> '')                          AS partner_nicht_leer,
       SUM(pflegestatus = 'GEPFLEGT')                                      AS gepflegt,
       SUM(pflegestatus = 'OFFEN')                                         AS offen,
       SUM(pflegestatus = 'GEPFLEGT' AND (partner IS NULL OR partner = '')) AS gepflegt_ohne_partner,
       COUNT(richtung)                                                     AS richtung_nicht_null,
       SUM(richtung = 'EINGEHEND')                                         AS eingehend,
       SUM(richtung = 'AUSGEHEND')                                         AS ausgehend,
       COUNT(DISTINCT partner)                                             AS verschiedene_partner
FROM overlord_monitor.process_catalog;

SELECT '=== V2c process_catalog: Pflegestatus x Herkunft ===' AS marke;
SELECT pflegestatus, vorschlag_herkunft, COUNT(*) AS anzahl
FROM overlord_monitor.process_catalog
GROUP BY pflegestatus, vorschlag_herkunft
ORDER BY pflegestatus, vorschlag_herkunft;

-- ── V3 — Welche Indizes traegt Message wirklich? ────────────────────────────
SELECT '=== V3 SHOW INDEX FROM Message ===' AS marke;
SHOW INDEX FROM GlassfishDB.Message;

SELECT '=== V3b Indizes verdichtet ===' AS marke;
SELECT INDEX_NAME,
       NON_UNIQUE,
       GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ', ') AS spalten,
       MAX(CARDINALITY) AS max_kardinalitaet,
       INDEX_TYPE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'GlassfishDB' AND TABLE_NAME = 'Message'
GROUP BY INDEX_NAME, NON_UNIQUE, INDEX_TYPE
ORDER BY INDEX_NAME;

-- ── V4 — Wie heissen die Spalten der Mandantenkette wirklich? ───────────────
SELECT '=== V4 Spalten Process / Project / ProjectMandant ===' AS marke;
SELECT TABLE_NAME, ORDINAL_POSITION, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE,
       COLUMN_KEY, COLUMN_DEFAULT, EXTRA, COLLATION_NAME
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'GlassfishDB'
  AND TABLE_NAME IN ('Process','Project','ProjectMandant')
ORDER BY TABLE_NAME, ORDINAL_POSITION;

SELECT '=== V4b Indizes der Mandantenkette ===' AS marke;
SELECT TABLE_NAME, INDEX_NAME, NON_UNIQUE,
       GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ', ') AS spalten
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'GlassfishDB'
  AND TABLE_NAME IN ('Process','Project','ProjectMandant')
GROUP BY TABLE_NAME, INDEX_NAME, NON_UNIQUE
ORDER BY TABLE_NAME, INDEX_NAME;

SELECT '=== V4c Bezugsgroessen der Kette ===' AS marke;
SELECT (SELECT COUNT(*) FROM GlassfishDB.Process)                        AS prozesse,
       (SELECT COUNT(*) FROM GlassfishDB.Project)                        AS projekte,
       (SELECT COUNT(*) FROM GlassfishDB.ProjectMandant)                 AS projektmandant,
       (SELECT COUNT(DISTINCT MandantID) FROM GlassfishDB.ProjectMandant) AS mandanten;

-- ── V5 — Ankerzeitpunkt "jetzt" ────────────────────────────────────────────
SELECT '=== V5 Anker der Anwendungsuhr (dev) ===' AS marke;
SELECT MAX(MessageLastUpdate) AS anker_v5,
       MIN(MessageLastUpdate) AS bestand_beginn
FROM GlassfishDB.Message;

SELECT '=== 99 Abschluss ===' AS marke;
SELECT @@global.read_only AS read_only_ende, NOW() AS serverzeit_ende;
