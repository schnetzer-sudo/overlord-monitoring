-- Messrunde M94-M98 — Sitzung 5a: M96, Sicherung vor dem Nachspielen
-- Auftrag:  "Messrunde vor Schritt 10b", Stand 27.08.2026
-- Ergebnis: docs/messungen-schritt10b.md
--
-- ⚠ DIESE SITZUNG SCHREIBT — und sie ist die eine benannte Ausnahme der Runde.
--   Geschrieben wird AUSSCHLIESSLICH in overlord_monitor:
--     * message_rollup_m96_sicherung wird angelegt und gefuellt.
--   GlassfishDB wird nur gelesen. (Regel S1)
--   Benutzer: monitor_write. Auf GlassfishDB hat er nur SELECT.
--
-- Das Fenster ist deterministisch: die 48 Stunden vor dem Anker der
-- Anwendungsuhr (V5, 2025-12-30 04:09:47), auf ganze Stunden ausgedehnt wie
-- RollupFenster es tut — obere Grenze ist der Anfang der naechsten Stunde:
--     von  2025-12-28 05:00:00  (einschliesslich)
--     bis  2025-12-30 05:00:00  (ausschliesslich)
-- Dasselbe Fenster wie Paar P1 in M94 und M95.
--
-- G1: keine ProcessID in der Ausgabe. Der Inhalt wird ueber eine
--     reihenfolgeunabhaengige Pruefsumme verglichen, nicht Zeile fuer Zeile.

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, VERSION() AS version, NOW() AS serverzeit;

SELECT '=== 01 Benutzer ===' AS marke;
SELECT USER() AS benutzer, CURRENT_USER() AS aufgeloest;

SET max_statement_time = 60;
SET profiling = 1;
SET profiling_history_size = 100;

SELECT '=== 5a-1 Stand VOR dem Nachspielen, Fenster ===' AS marke;
SELECT COUNT(*)      AS zeilen,
       SUM(anzahl)   AS summe_anzahl,
       COUNT(DISTINCT process_id)     AS versch_prozesse,
       COUNT(DISTINCT message_status) AS versch_status,
       MIN(stunde)   AS fruehste_stunde,
       MAX(stunde)   AS spaeteste_stunde,
       BIT_XOR(CRC32(CONCAT_WS('|', stunde, process_id, message_status, anzahl)))
                     AS inhalt_pruefsumme
FROM overlord_monitor.message_rollup
WHERE stunde >= '2025-12-28 05:00:00' AND stunde < '2025-12-30 05:00:00';

SELECT '=== 5a-2 Stand VOR dem Nachspielen, ganze Tabelle ===' AS marke;
SELECT COUNT(*)    AS zeilen,
       SUM(anzahl) AS summe_anzahl,
       BIT_XOR(CRC32(CONCAT_WS('|', stunde, process_id, message_status, anzahl)))
                   AS inhalt_pruefsumme
FROM overlord_monitor.message_rollup;

SELECT '=== 5a-3 Stand VOR dem Nachspielen, ausserhalb des Fensters ===' AS marke;
-- Diese Zahl muss die Runde unberuehrt lassen. Sie ist der Zeuge dafuer, dass
-- der Nachspiellauf nur das Fenster angefasst hat.
SELECT COUNT(*)    AS zeilen,
       SUM(anzahl) AS summe_anzahl,
       BIT_XOR(CRC32(CONCAT_WS('|', stunde, process_id, message_status, anzahl)))
                   AS inhalt_pruefsumme
FROM overlord_monitor.message_rollup
WHERE stunde < '2025-12-28 05:00:00' OR stunde >= '2025-12-30 05:00:00';

SELECT '=== 5a-4 rollup_lauf VOR dem Nachspielen ===' AS marke;
SELECT COUNT(*) AS zeilen, MIN(id) AS kleinste_id, MAX(id) AS groesste_id,
       GROUP_CONCAT(id ORDER BY id) AS alle_ids
FROM overlord_monitor.rollup_lauf;

SELECT '=== 5a-5 Sicherungstabelle anlegen ===' AS marke;
DROP TABLE IF EXISTS overlord_monitor.message_rollup_m96_sicherung;
CREATE TABLE overlord_monitor.message_rollup_m96_sicherung (
  stunde         DATETIME    NOT NULL,
  process_id     VARCHAR(36) NOT NULL,
  message_status VARCHAR(30) NOT NULL,
  anzahl         INT         NOT NULL,
  PRIMARY KEY (stunde, process_id, message_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO overlord_monitor.message_rollup_m96_sicherung
       (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl
FROM overlord_monitor.message_rollup
WHERE stunde >= '2025-12-28 05:00:00' AND stunde < '2025-12-30 05:00:00';

SELECT '=== 5a-6 Sicherung gegen das Original gehalten ===' AS marke;
SELECT COUNT(*)    AS zeilen,
       SUM(anzahl) AS summe_anzahl,
       BIT_XOR(CRC32(CONCAT_WS('|', stunde, process_id, message_status, anzahl)))
                   AS inhalt_pruefsumme
FROM overlord_monitor.message_rollup_m96_sicherung;

SELECT '=== 5a-7 Die direkte Zahl aus Message ueber dasselbe Fenster ===' AS marke;
-- Der Bezugswert fuer Schritt 6 der Messung. Nur SELECT auf GlassfishDB.
SELECT COUNT(*)                     AS nachrichten_direkt,
       COUNT(DISTINCT ProcessID)    AS versch_prozesse,
       COUNT(DISTINCT MessageStatus) AS versch_status
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2025-12-28 05:00:00'
  AND MessageLastUpdate <  '2025-12-30 05:00:00';

SELECT '=== 5a-8 Gegenprobe: GlassfishDB unberuehrt ===' AS marke;
SELECT COUNT(*) AS message_zeilen, MAX(MessageLastUpdate) AS datenstand
FROM GlassfishDB.Message;

SELECT '=== 99 Laufzeiten ===' AS marke;
SHOW PROFILES;

SELECT '=== 99 read_only am Ende der Sitzung ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;
