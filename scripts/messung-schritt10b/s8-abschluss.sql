-- Messrunde M94-M98 — Sitzung 8: Wiederherstellung, Nachweis, Abschluss
-- Auftrag:  "Messrunde vor Schritt 10b", Stand 27.08.2026
-- Ergebnis: docs/messungen-schritt10b.md
--
-- ⚠ DIESE SITZUNG SCHREIBT. Geschrieben wird AUSSCHLIESSLICH in overlord_monitor:
--     * fehlende Zeilen im M96-Fenster aus der Sicherung zurueckschreiben
--       (falls welche fehlen — der Volllauf aus Sitzung 5b hat den Bereich
--       bereits korrekt neu gerechnet),
--     * die Sicherungstabelle loeschen.
--   GlassfishDB wird nur gelesen. (Regel S1)
--
-- Die Wiederherstellung wird Zeichen fuer Zeichen nachgewiesen: Zeilenzahl,
-- Summe und eine reihenfolgeunabhaengige Inhaltspruefsumme werden gegen die
-- Werte aus Sitzung 5a gehalten.
--
-- Sollwerte aus Sitzung 5a (27.08.2026):
--     Fenster            929 Zeilen · 12.004 · Pruefsumme 382024424
--     ganze Tabelle  335.610 Zeilen · 3.341.519 · Pruefsumme 4256889028
--     ausserhalb     334.681 Zeilen · 3.329.515 · Pruefsumme 3951018540
--     rollup_lauf          3 Zeilen · Kennungen 25, 27, 29

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, VERSION() AS version, NOW() AS serverzeit;

SELECT '=== 01 Benutzer ===' AS marke;
SELECT USER() AS benutzer, CURRENT_USER() AS aufgeloest;

SET max_statement_time = 60;
SET profiling = 1;
SET profiling_history_size = 100;

SELECT '=== 8-1 Stand NACH M96, vor jeder Wiederherstellung ===' AS marke;
SELECT COUNT(*)    AS zeilen,
       SUM(anzahl) AS summe_anzahl,
       BIT_XOR(CRC32(CONCAT_WS('|', stunde, process_id, message_status, anzahl)))
                   AS inhalt_pruefsumme
FROM overlord_monitor.message_rollup
WHERE stunde >= '2025-12-28 05:00:00' AND stunde < '2025-12-30 05:00:00';

SELECT '=== 8-2 Abweichung gegen die Sicherung, beide Richtungen ===' AS marke;
SELECT (SELECT COUNT(*) FROM overlord_monitor.message_rollup_m96_sicherung s
         LEFT JOIN overlord_monitor.message_rollup r
                ON r.stunde = s.stunde AND r.process_id = s.process_id
               AND r.message_status = s.message_status
        WHERE r.stunde IS NULL OR r.anzahl <> s.anzahl)   AS fehlt_oder_weicht_ab,
       (SELECT COUNT(*) FROM overlord_monitor.message_rollup r
         LEFT JOIN overlord_monitor.message_rollup_m96_sicherung s
                ON s.stunde = r.stunde AND s.process_id = r.process_id
               AND s.message_status = r.message_status
        WHERE r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
          AND s.stunde IS NULL)                           AS zuviel_im_fenster;

SELECT '=== 8-3 Wiederherstellung aus der Sicherung ===' AS marke;
-- Das mehrtabellige DELETE mit Alias braucht eine Vorgabedatenbank; ohne sie
-- bricht der Client mit ERROR 1046 (3D000) ab. Aufgefallen beim ersten Lauf
-- dieser Sitzung (Abweichung A5).
USE overlord_monitor;
-- Idempotent und in einem Zug: was fehlt, kommt zurueck; was abweicht, wird
-- ueberschrieben; was zu viel ist, faellt vorher weg. Steht nach 8-2 ueberall
-- eine 0, aendert dieser Block keine einzige Zeile — und genau das ist der
-- erwartete Fall, weil der Volllauf aus Sitzung 5b den Bereich korrekt
-- neu gerechnet hat.
DELETE r FROM overlord_monitor.message_rollup r
LEFT JOIN overlord_monitor.message_rollup_m96_sicherung s
       ON s.stunde = r.stunde AND s.process_id = r.process_id
      AND s.message_status = r.message_status
WHERE r.stunde >= '2025-12-28 05:00:00' AND r.stunde < '2025-12-30 05:00:00'
  AND s.stunde IS NULL;

INSERT INTO overlord_monitor.message_rollup (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl
FROM overlord_monitor.message_rollup_m96_sicherung
ON DUPLICATE KEY UPDATE anzahl = VALUES(anzahl);

SELECT '=== 8-4 Nachweis: Fenster gegen Sitzung 5a ===' AS marke;
SELECT COUNT(*)    AS zeilen,
       SUM(anzahl) AS summe_anzahl,
       BIT_XOR(CRC32(CONCAT_WS('|', stunde, process_id, message_status, anzahl)))
                   AS inhalt_pruefsumme,
       COUNT(*) = 929                AS zeilen_stimmen,
       SUM(anzahl) = 12004           AS summe_stimmt,
       BIT_XOR(CRC32(CONCAT_WS('|', stunde, process_id, message_status, anzahl)))
         = 382024424                 AS pruefsumme_stimmt
FROM overlord_monitor.message_rollup
WHERE stunde >= '2025-12-28 05:00:00' AND stunde < '2025-12-30 05:00:00';

SELECT '=== 8-5 Nachweis: ausserhalb des Fensters unberuehrt ===' AS marke;
SELECT COUNT(*)    AS zeilen,
       SUM(anzahl) AS summe_anzahl,
       BIT_XOR(CRC32(CONCAT_WS('|', stunde, process_id, message_status, anzahl)))
                   AS inhalt_pruefsumme,
       BIT_XOR(CRC32(CONCAT_WS('|', stunde, process_id, message_status, anzahl)))
         = 3951018540                AS pruefsumme_stimmt
FROM overlord_monitor.message_rollup
WHERE stunde < '2025-12-28 05:00:00' OR stunde >= '2025-12-30 05:00:00';

SELECT '=== 8-6 Nachweis: ganze Tabelle gegen Sitzung 5a ===' AS marke;
SELECT COUNT(*)    AS zeilen,
       SUM(anzahl) AS summe_anzahl,
       BIT_XOR(CRC32(CONCAT_WS('|', stunde, process_id, message_status, anzahl)))
                   AS inhalt_pruefsumme,
       COUNT(*) = 335610             AS zeilen_stimmen,
       SUM(anzahl) = 3341519         AS summe_stimmt,
       BIT_XOR(CRC32(CONCAT_WS('|', stunde, process_id, message_status, anzahl)))
         = 4256889028                AS pruefsumme_stimmt
FROM overlord_monitor.message_rollup;

SELECT '=== 8-7 Nachweis: rollup_lauf abgeraeumt ===' AS marke;
SELECT COUNT(*) AS zeilen, GROUP_CONCAT(id ORDER BY id) AS alle_ids,
       GROUP_CONCAT(id ORDER BY id) = '25,27,29' AS unveraendert
FROM overlord_monitor.rollup_lauf;

SELECT '=== 8-8 Sicherungstabelle loeschen ===' AS marke;
DROP TABLE overlord_monitor.message_rollup_m96_sicherung;

SELECT '=== 8-9 Nachweis: Sicherungstabelle ist weg ===' AS marke;
SELECT COUNT(*) AS gefundene_tabellen
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'overlord_monitor'
  AND TABLE_NAME = 'message_rollup_m96_sicherung';

SELECT '=== 8-10 Alle Tabellen in overlord_monitor ===' AS marke;
SELECT TABLE_NAME, TABLE_ROWS,
       ROUND((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024, 2) AS gesamt_mib
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'overlord_monitor'
ORDER BY TABLE_NAME;

SELECT '=== 8-11 Gegenprobe: GlassfishDB unberuehrt ===' AS marke;
SELECT COUNT(*) AS message_zeilen, MAX(MessageLastUpdate) AS datenstand
FROM GlassfishDB.Message;

SELECT '=== 8-12 Gegenprobe: Tabellengroessen in GlassfishDB wie in V1 ===' AS marke;
SELECT TABLE_NAME, DATA_LENGTH, INDEX_LENGTH, UPDATE_TIME
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'GlassfishDB'
  AND TABLE_NAME IN ('Message','MessageAction','MessageBAM','MessageProperty')
ORDER BY TABLE_NAME;

SELECT '=== 99 Laufzeiten ===' AS marke;
SHOW PROFILES;

SELECT '=== 99 read_only am Ende der Runde ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;
