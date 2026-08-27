-- Messung M104 f - Probetabelle mit Index anlegen (zweiter Durchgang)
-- Auftrag:  "Fensterverengung fuer die Nachrichtenliste", Stand 27.08.2026, Nachtrag zu Punkt 73
-- Ergebnis: docs/messungen-liste-verengung.md
--
-- ⚠ Fortsetzung der benannten Ausnahme aus m104a: Es wird EINE Probetabelle in
--   'overlord_monitor' angelegt, befuellt, gemessen und wieder geloescht - Bauform M89.
--   Die echte Tabelle message_rollup wird nicht angefasst; auf GlassfishDB nur gelesen. (S1)
--
-- Der erste Durchgang (m104b/d) hat die LITERALfassung ueber alle vier Stufen gemessen und die
-- EXISTS-Fassung nur ueber die 30-Tage-Stufe. Damit fehlt genau die Zelle, an der die Antwort
-- haengt: Was kostet die EXISTS-Fassung auf den ENGEN Stufen, wenn der Index da ist? Ohne sie
-- laesst sich nicht sagen, ob eine einzige gebaute Fassung alle zehn Mandanten traegt.
--
-- Benutzer: monitor_write

SELECT '=== 00 Vorher ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;
SELECT COUNT(*) AS probetabelle_existiert_schon FROM information_schema.TABLES
 WHERE TABLE_SCHEMA='overlord_monitor' AND TABLE_NAME='message_rollup_probe_idx';

SET max_statement_time = 60;

CREATE TABLE overlord_monitor.message_rollup_probe_idx (
  stunde         DATETIME    NOT NULL,
  process_id     VARCHAR(36) NOT NULL,
  message_status VARCHAR(30) NOT NULL,
  anzahl         INT         NOT NULL,
  PRIMARY KEY (stunde, process_id, message_status),
  INDEX probe_prozess_stunde_idx (process_id, stunde)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

SELECT '=== 01 Befuellen, in Monatsscheiben ===' AS marke;
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

SELECT '=== 02 Kontrolle ===' AS marke;
SELECT (SELECT COUNT(*) FROM overlord_monitor.message_rollup) AS quelle_zeilen,
       (SELECT COUNT(*) FROM overlord_monitor.message_rollup_probe_idx)                            AS probe_zeilen;
SELECT COUNT(*) AS zeilen_die_sich_unterscheiden FROM (
  (SELECT * FROM overlord_monitor.message_rollup EXCEPT SELECT * FROM overlord_monitor.message_rollup_probe_idx)
  UNION ALL
  (SELECT * FROM overlord_monitor.message_rollup_probe_idx EXCEPT SELECT * FROM overlord_monitor.message_rollup)) d;
SHOW INDEX FROM overlord_monitor.message_rollup_probe_idx;
SELECT TABLE_ROWS, DATA_LENGTH, INDEX_LENGTH FROM information_schema.TABLES
 WHERE TABLE_SCHEMA='overlord_monitor' AND TABLE_NAME='message_rollup_probe_idx';
SELECT @@global.read_only AS read_only_am_ende, NOW() AS serverzeit_ende;
