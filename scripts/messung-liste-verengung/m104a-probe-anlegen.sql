-- Messung M104 a - Probetabelle anlegen und befuellen
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
SELECT TABLE_NAME FROM information_schema.TABLES
 WHERE TABLE_SCHEMA = 'overlord_monitor' ORDER BY TABLE_NAME;
SELECT COUNT(*) AS probetabelle_existiert_schon FROM information_schema.TABLES
 WHERE TABLE_SCHEMA = 'overlord_monitor' AND TABLE_NAME = 'message_rollup_probe_idx';

SET max_statement_time = 60;

CREATE TABLE overlord_monitor.message_rollup_probe_idx (
  stunde         DATETIME    NOT NULL,
  process_id     VARCHAR(36) NOT NULL,
  message_status VARCHAR(30) NOT NULL,
  anzahl         INT         NOT NULL,
  PRIMARY KEY (stunde, process_id, message_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Zeichengleich zu message_rollup: derselbe Schluessel, dieselbe Sortierung, KEIN Sekundaerindex.
-- Der kommt erst in m104c dazu, damit beide Messungen gegen DIESELBE Tabelle laufen und der
-- Unterschied allein am Index haengt.

SELECT '=== 01 Befuellen, in Monatsscheiben (Muster M89) ===' AS marke;
INSERT INTO overlord_monitor.message_rollup_probe_idx (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2024-10-01 00:00:00' AND stunde < '2024-11-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_idx (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2024-11-01 00:00:00' AND stunde < '2024-12-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_idx (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2024-12-01 00:00:00' AND stunde < '2025-01-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_idx (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2025-01-01 00:00:00' AND stunde < '2025-02-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_idx (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2025-02-01 00:00:00' AND stunde < '2025-03-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_idx (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2025-03-01 00:00:00' AND stunde < '2025-04-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_idx (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2025-04-01 00:00:00' AND stunde < '2025-05-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_idx (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2025-05-01 00:00:00' AND stunde < '2025-06-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_idx (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2025-06-01 00:00:00' AND stunde < '2025-07-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_idx (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2025-07-01 00:00:00' AND stunde < '2025-08-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_idx (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2025-08-01 00:00:00' AND stunde < '2025-09-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_idx (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2025-09-01 00:00:00' AND stunde < '2025-10-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_idx (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2025-10-01 00:00:00' AND stunde < '2025-11-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_idx (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2025-11-01 00:00:00' AND stunde < '2025-12-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_idx (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2025-12-01 00:00:00' AND stunde < '2026-01-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_idx (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2026-01-01 00:00:00' AND stunde < '2026-02-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_idx (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2026-02-01 00:00:00' AND stunde < '2026-03-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_idx (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2026-03-01 00:00:00' AND stunde < '2026-04-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_idx (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2026-04-01 00:00:00' AND stunde < '2026-05-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_idx (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2026-05-01 00:00:00' AND stunde < '2026-06-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_idx (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2026-06-01 00:00:00' AND stunde < '2026-07-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_idx (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2026-07-01 00:00:00' AND stunde < '2026-08-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_idx (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2026-08-01 00:00:00' AND stunde < '2026-09-01 00:00:00';

SELECT '=== 02 Kontrolle: deckungsgleich mit der Quelle? ===' AS marke;
SELECT (SELECT COUNT(*)    FROM overlord_monitor.message_rollup)  AS quelle_zeilen,
       (SELECT COUNT(*)    FROM overlord_monitor.message_rollup_probe_idx)                             AS probe_zeilen,
       (SELECT SUM(anzahl) FROM overlord_monitor.message_rollup)  AS quelle_summe,
       (SELECT SUM(anzahl) FROM overlord_monitor.message_rollup_probe_idx)                             AS probe_summe;
SELECT COUNT(*) AS zeilen_die_sich_unterscheiden FROM (
  (SELECT * FROM overlord_monitor.message_rollup EXCEPT SELECT * FROM overlord_monitor.message_rollup_probe_idx)
  UNION ALL
  (SELECT * FROM overlord_monitor.message_rollup_probe_idx EXCEPT SELECT * FROM overlord_monitor.message_rollup)) d;
SELECT TABLE_NAME, TABLE_ROWS, DATA_LENGTH, INDEX_LENGTH FROM information_schema.TABLES
 WHERE TABLE_SCHEMA = 'overlord_monitor'
   AND TABLE_NAME IN ('message_rollup', 'message_rollup_probe_idx');
SELECT @@global.read_only AS read_only_am_ende, NOW() AS serverzeit_ende;
