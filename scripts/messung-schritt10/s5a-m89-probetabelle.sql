-- Messrunde M86-M92 — Sitzung 5a: M89, die Probetabelle anlegen und fuellen
--
-- HIER STEHT DIE EINZIGE SCHREIBENDE HANDLUNG DIESER RUNDE.
-- Freigegeben durch den Auftrag, Abschnitt M89, woertlich:
--   "Freigegeben: genau eine Tabelle overlord_monitor.message_rollup_probe, angelegt mit
--    dem Schreibbenutzer, befuellt aus der Aggregation von M87 Variante 1, gemessen,
--    am Ende der Runde geloescht."
--
-- GESPERRT und hier nicht getan: jede Flyway-Migration, jede Aenderung an
-- process_catalog, JEDER Schreibzugriff auf GlassfishDB, jede zweite Tabelle,
-- jedes Belassen der Probetabelle ueber das Rundenende hinaus.
--
-- Benutzer: monitor_write (ALL PRIVILEGES nur auf overlord_monitor, SELECT auf GlassfishDB).
-- Auf GlassfishDB wird ausschliesslich GELESEN — die INSERT-Ziele liegen alle in
-- overlord_monitor.
--
-- WARUM IN MONATSSCHEIBEN. Ein einziges INSERT ... SELECT ueber den Gesamtbestand liefe
-- gegen `max_statement_time = 60` — M87-5 hat die Grenze mit einer aehnlich grossen
-- Aggregation bereits gerissen (60,219 s). Der Rahmen verbietet, die Grenze hochzusetzen.
-- Gefuellt wird deshalb in 22 Monatsscheiben, genau wie der Rueckwaertslauf aus M92 es
-- taete. Die Scheibengrenzen stehen als Literal im Statement (Regel Z1).
--
-- Der Schluessel der Probetabelle ist (Stunde, ProcessID, MessageStatus) — E-a plus E-g,
-- die gebaute Fassung. Mandant, Partner und Richtung stehen NICHT in der Zeile; sie
-- werden zur Lesezeit gejoint. Genau das misst Sitzung 5b.

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;
SELECT USER() AS benutzer, CURRENT_USER() AS aufgeloest;

SET max_statement_time = 60;
SET profiling_history_size = 100;
SET profiling = 1;

SELECT '=== 5a-1 Vorher: gibt es die Tabelle schon? (muss leer sein) ===' AS marke;
SELECT COUNT(*) AS tabelle_vorhanden
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'overlord_monitor' AND TABLE_NAME = 'message_rollup_probe';

SELECT '=== 5a-2 Bestand von overlord_monitor VOR der Anlage ===' AS marke;
SELECT TABLE_NAME FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'overlord_monitor' ORDER BY TABLE_NAME;

SELECT '=== 5a-3 Anlegen ===' AS marke;
CREATE TABLE overlord_monitor.message_rollup_probe (
  stunde         DATETIME    NOT NULL,
  process_id     VARCHAR(36) NOT NULL,
  message_status VARCHAR(30) NOT NULL,
  anzahl         INT         NOT NULL,
  PRIMARY KEY (stunde, process_id, message_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

SELECT '=== 5a-4 Fuellen, 22 Monatsscheiben ===' AS marke;

SELECT '--- Scheibe 2024-10 ---' AS marke;
INSERT INTO overlord_monitor.message_rollup_probe (stunde, process_id, message_status, anzahl)
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00'), ProcessID, MessageStatus, COUNT(*)
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2024-10-01 00:00:00' AND MessageLastUpdate < '2024-11-01 00:00:00'
GROUP BY 1, 2, 3;

SELECT '--- Scheibe 2024-11 ---' AS marke;
INSERT INTO overlord_monitor.message_rollup_probe (stunde, process_id, message_status, anzahl)
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00'), ProcessID, MessageStatus, COUNT(*)
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2024-11-01 00:00:00' AND MessageLastUpdate < '2024-12-01 00:00:00'
GROUP BY 1, 2, 3;

SELECT '--- Scheibe 2024-12 ---' AS marke;
INSERT INTO overlord_monitor.message_rollup_probe (stunde, process_id, message_status, anzahl)
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00'), ProcessID, MessageStatus, COUNT(*)
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2024-12-01 00:00:00' AND MessageLastUpdate < '2025-01-01 00:00:00'
GROUP BY 1, 2, 3;

SELECT '--- Scheibe 2025-01 ---' AS marke;
INSERT INTO overlord_monitor.message_rollup_probe (stunde, process_id, message_status, anzahl)
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00'), ProcessID, MessageStatus, COUNT(*)
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2025-01-01 00:00:00' AND MessageLastUpdate < '2025-02-01 00:00:00'
GROUP BY 1, 2, 3;

SELECT '--- Scheibe 2025-02 ---' AS marke;
INSERT INTO overlord_monitor.message_rollup_probe (stunde, process_id, message_status, anzahl)
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00'), ProcessID, MessageStatus, COUNT(*)
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2025-02-01 00:00:00' AND MessageLastUpdate < '2025-03-01 00:00:00'
GROUP BY 1, 2, 3;

SELECT '--- Scheibe 2025-03 ---' AS marke;
INSERT INTO overlord_monitor.message_rollup_probe (stunde, process_id, message_status, anzahl)
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00'), ProcessID, MessageStatus, COUNT(*)
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2025-03-01 00:00:00' AND MessageLastUpdate < '2025-04-01 00:00:00'
GROUP BY 1, 2, 3;

SELECT '--- Scheibe 2025-04 ---' AS marke;
INSERT INTO overlord_monitor.message_rollup_probe (stunde, process_id, message_status, anzahl)
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00'), ProcessID, MessageStatus, COUNT(*)
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2025-04-01 00:00:00' AND MessageLastUpdate < '2025-05-01 00:00:00'
GROUP BY 1, 2, 3;

SELECT '--- Scheibe 2025-05 ---' AS marke;
INSERT INTO overlord_monitor.message_rollup_probe (stunde, process_id, message_status, anzahl)
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00'), ProcessID, MessageStatus, COUNT(*)
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2025-05-01 00:00:00' AND MessageLastUpdate < '2025-06-01 00:00:00'
GROUP BY 1, 2, 3;

SELECT '--- Scheibe 2025-06 ---' AS marke;
INSERT INTO overlord_monitor.message_rollup_probe (stunde, process_id, message_status, anzahl)
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00'), ProcessID, MessageStatus, COUNT(*)
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2025-06-01 00:00:00' AND MessageLastUpdate < '2025-07-01 00:00:00'
GROUP BY 1, 2, 3;

SELECT '--- Scheibe 2025-07 ---' AS marke;
INSERT INTO overlord_monitor.message_rollup_probe (stunde, process_id, message_status, anzahl)
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00'), ProcessID, MessageStatus, COUNT(*)
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2025-07-01 00:00:00' AND MessageLastUpdate < '2025-08-01 00:00:00'
GROUP BY 1, 2, 3;

SELECT '--- Scheibe 2025-08 ---' AS marke;
INSERT INTO overlord_monitor.message_rollup_probe (stunde, process_id, message_status, anzahl)
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00'), ProcessID, MessageStatus, COUNT(*)
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2025-08-01 00:00:00' AND MessageLastUpdate < '2025-09-01 00:00:00'
GROUP BY 1, 2, 3;

SELECT '--- Scheibe 2025-09 ---' AS marke;
INSERT INTO overlord_monitor.message_rollup_probe (stunde, process_id, message_status, anzahl)
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00'), ProcessID, MessageStatus, COUNT(*)
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2025-09-01 00:00:00' AND MessageLastUpdate < '2025-10-01 00:00:00'
GROUP BY 1, 2, 3;

SELECT '--- Scheibe 2025-10 ---' AS marke;
INSERT INTO overlord_monitor.message_rollup_probe (stunde, process_id, message_status, anzahl)
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00'), ProcessID, MessageStatus, COUNT(*)
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2025-10-01 00:00:00' AND MessageLastUpdate < '2025-11-01 00:00:00'
GROUP BY 1, 2, 3;

SELECT '--- Scheibe 2025-11 ---' AS marke;
INSERT INTO overlord_monitor.message_rollup_probe (stunde, process_id, message_status, anzahl)
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00'), ProcessID, MessageStatus, COUNT(*)
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2025-11-01 00:00:00' AND MessageLastUpdate < '2025-12-01 00:00:00'
GROUP BY 1, 2, 3;

SELECT '--- Scheibe 2025-12 ---' AS marke;
INSERT INTO overlord_monitor.message_rollup_probe (stunde, process_id, message_status, anzahl)
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00'), ProcessID, MessageStatus, COUNT(*)
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2025-12-01 00:00:00' AND MessageLastUpdate < '2026-01-01 00:00:00'
GROUP BY 1, 2, 3;

SELECT '--- Scheibe 2026-01 ---' AS marke;
INSERT INTO overlord_monitor.message_rollup_probe (stunde, process_id, message_status, anzahl)
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00'), ProcessID, MessageStatus, COUNT(*)
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2026-01-01 00:00:00' AND MessageLastUpdate < '2026-02-01 00:00:00'
GROUP BY 1, 2, 3;

SELECT '--- Scheibe 2026-02 ---' AS marke;
INSERT INTO overlord_monitor.message_rollup_probe (stunde, process_id, message_status, anzahl)
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00'), ProcessID, MessageStatus, COUNT(*)
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2026-02-01 00:00:00' AND MessageLastUpdate < '2026-03-01 00:00:00'
GROUP BY 1, 2, 3;

SELECT '--- Scheibe 2026-03 ---' AS marke;
INSERT INTO overlord_monitor.message_rollup_probe (stunde, process_id, message_status, anzahl)
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00'), ProcessID, MessageStatus, COUNT(*)
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2026-03-01 00:00:00' AND MessageLastUpdate < '2026-04-01 00:00:00'
GROUP BY 1, 2, 3;

SELECT '--- Scheibe 2026-04 ---' AS marke;
INSERT INTO overlord_monitor.message_rollup_probe (stunde, process_id, message_status, anzahl)
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00'), ProcessID, MessageStatus, COUNT(*)
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2026-04-01 00:00:00' AND MessageLastUpdate < '2026-05-01 00:00:00'
GROUP BY 1, 2, 3;

SELECT '--- Scheibe 2026-05 ---' AS marke;
INSERT INTO overlord_monitor.message_rollup_probe (stunde, process_id, message_status, anzahl)
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00'), ProcessID, MessageStatus, COUNT(*)
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2026-05-01 00:00:00' AND MessageLastUpdate < '2026-06-01 00:00:00'
GROUP BY 1, 2, 3;

SELECT '--- Scheibe 2026-06 ---' AS marke;
INSERT INTO overlord_monitor.message_rollup_probe (stunde, process_id, message_status, anzahl)
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00'), ProcessID, MessageStatus, COUNT(*)
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2026-06-01 00:00:00' AND MessageLastUpdate < '2026-07-01 00:00:00'
GROUP BY 1, 2, 3;

SELECT '--- Scheibe 2026-07 ---' AS marke;
INSERT INTO overlord_monitor.message_rollup_probe (stunde, process_id, message_status, anzahl)
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00'), ProcessID, MessageStatus, COUNT(*)
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2026-07-01 00:00:00' AND MessageLastUpdate < '2026-08-01 00:00:00'
GROUP BY 1, 2, 3;


SELECT '=== 5a-5 Kontrolle: Zeilenzahl muss 335.610 sein (M87 Variante 1) ===' AS marke;
SELECT COUNT(*)          AS zeilen_probe,
       SUM(anzahl)       AS summe_anzahl,
       MIN(stunde)       AS frueheste_stunde,
       MAX(stunde)       AS spaeteste_stunde,
       COUNT(DISTINCT process_id)     AS prozesse,
       COUNT(DISTINCT message_status) AS status
FROM overlord_monitor.message_rollup_probe;

SELECT '=== 5a-6 Groesse der Probetabelle ===' AS marke;
SELECT TABLE_NAME, TABLE_ROWS, DATA_LENGTH, INDEX_LENGTH,
       ROUND((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024, 2) AS mib
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'overlord_monitor' AND TABLE_NAME = 'message_rollup_probe';

SELECT '=== 5a-7 Gegenprobe: GlassfishDB ist unberuehrt ===' AS marke;
SELECT COUNT(*) AS zeilen_message, MAX(MessageLastUpdate) AS datenstand
FROM GlassfishDB.Message;

SELECT '=== 98 SHOW PROFILES ===' AS marke;
SHOW PROFILES;

SELECT '=== 99 Abschluss ===' AS marke;
SELECT @@global.read_only AS read_only_ende, NOW() AS serverzeit_ende;
