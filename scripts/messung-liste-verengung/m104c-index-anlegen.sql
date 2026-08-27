-- Messung M104 c - Den Sekundaerindex auf der Probetabelle anlegen
-- Auftrag:  "Fensterverengung fuer die Nachrichtenliste", Stand 27.08.2026, Nachtrag zu Punkt 73
-- Ergebnis: docs/messungen-liste-verengung.md
--
-- ⚠ DIE EINE BENANNTE AUSNAHME VOM SATZ "DIESE RUNDE SCHREIBT NIRGENDS"
--
--   Diese Sitzung schreibt in 'overlord_monitor' - und NUR dort. Sie legt EINE Probetabelle an,
--   befuellt sie aus message_rollup, misst gegen sie und loescht sie am Ende wieder. Die Loeschung
--   ist in m104e-probe-loeschen.sql nachgewiesen. Dieselbe Bauform wie M89 (messungen-schritt10.md
--   Z. 1098 ff.), aus demselben Grund und mit derselben Rechenschaft.
--
--   Die echte Tabelle message_rollup wird NICHT angefasst - weder ihre Daten noch ihr Schema.
--   Auf GlassfishDB wird ausschliesslich gelesen; monitor_write hat dort nur SELECT. (Regel S1)
--
--   Warum ueberhaupt: M99 zeigt, dass die duennen Mandanten 13 bis 18 ms fuer die Vorabfrage
--   zahlen, weil der Nachweis "weniger als 51 Zeilen" alle 21.274 Rollupzeilen des Fensters lesen
--   muss - message_rollup traegt keinen Sekundaerindex (rollup.md §2, ausdrueckliche Entscheidung).
--   Genau daran ist das Tor gescheitert. Ob ein Index (process_id, stunde) es oeffnen wuerde, ist
--   die offene Frage 73.
--
-- Benutzer: monitor_write

SELECT '=== 00 Vorher ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;
SELECT TABLE_ROWS, DATA_LENGTH, INDEX_LENGTH FROM information_schema.TABLES
 WHERE TABLE_SCHEMA='overlord_monitor' AND TABLE_NAME='message_rollup_probe_idx';

SET max_statement_time = 60;

SELECT '=== 01 Index anlegen: (process_id, stunde) ===' AS marke;
SELECT NOW(3) AS begonnen;
ALTER TABLE overlord_monitor.message_rollup_probe_idx ADD INDEX probe_prozess_stunde_idx (process_id, stunde);
SELECT NOW(3) AS beendet;

SELECT '=== 02 Nachher ===' AS marke;
SHOW INDEX FROM overlord_monitor.message_rollup_probe_idx;
SELECT TABLE_ROWS, DATA_LENGTH, INDEX_LENGTH FROM information_schema.TABLES
 WHERE TABLE_SCHEMA='overlord_monitor' AND TABLE_NAME='message_rollup_probe_idx';
SELECT @@global.read_only AS read_only_am_ende, NOW() AS serverzeit_ende;
