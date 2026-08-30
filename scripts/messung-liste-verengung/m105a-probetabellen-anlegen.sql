-- Messung M105 a - Zwei Probetabellen fuer den Schreibpfad des Rollup-Laufs
-- Auftrag:  "Fensterverengung bauen - mit Index auf message_rollup", Stand 27.08.2026, Teil A
-- Ergebnis: docs/rollup.md §9c
--
-- ⚠ DIE BENANNTE AUSNAHME VOM SATZ "ES WIRD NUR GELESEN"
--
--   Diese Sitzung schreibt in 'overlord_monitor' - und nur dort. Sie legt ZWEI Probetabellen an
--   (Stunden- und Tagesebene), befuellt sie, misst gegen sie und loescht sie wieder. Die Loeschung
--   ist in m105e nachgewiesen. Bauform M89 / M104, aus demselben Grund und mit derselben
--   Rechenschaft. Die echten Tabellen message_rollup und message_rollup_tag werden nur GELESEN.
--   Auf GlassfishDB wird ueberhaupt nicht zugegriffen. (Regel S1)
--
-- Teil A misst, was ein Index (process_id, stunde) den beiden Laeufen kostet. Der Index wirkt auf
-- das SCHREIBEN, nicht auf die Aggregation ueber Message - die Bezugswerte 84,999 ms und
-- 2.575,006 ms aus rollup.md §9 messen die Aggregation und koennen sich gar nicht aendern.
-- Gemessen wird deshalb der Schreibpfad, wortgleich zu RollupSchreibRepository.ersetzeFenster:
--   1 DELETE auf der Stundenebene   2 INSERT auf der Stundenebene
--   3 DELETE auf der Tagesebene     4 INSERT auf der Tagesebene - und der LIEST die Stundenebene
-- Schritt 4 ist der eigentliche Verdacht: Ein neuer Index koennte seinen Plan kippen.
--
-- Benutzer: monitor_write
-- Z1: alle Zeitpunkte als Literal, nie NOW().

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, VERSION() AS version, NOW() AS serverzeit;

SELECT '=== 01 Vorher ===' AS marke;
SELECT TABLE_NAME FROM information_schema.TABLES
 WHERE TABLE_SCHEMA='overlord_monitor' ORDER BY TABLE_NAME;

SET max_statement_time = 60;

CREATE TABLE overlord_monitor.message_rollup_probe_lauf (
  stunde         DATETIME    NOT NULL,
  process_id     VARCHAR(36) NOT NULL,
  message_status VARCHAR(30) NOT NULL,
  anzahl         INT         NOT NULL,
  PRIMARY KEY (stunde, process_id, message_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE overlord_monitor.message_rollup_tag_probe_lauf (
  tag            DATE        NOT NULL,
  process_id     VARCHAR(36) NOT NULL,
  message_status VARCHAR(30) NOT NULL,
  anzahl         INT         NOT NULL,
  PRIMARY KEY (tag, process_id, message_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

SELECT '=== 02 Befuellen, in Monatsscheiben (Muster M89) ===' AS marke;
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2024-10-01 00:00:00' AND stunde < '2024-11-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2024-11-01 00:00:00' AND stunde < '2024-12-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2024-12-01 00:00:00' AND stunde < '2025-01-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2025-01-01 00:00:00' AND stunde < '2025-02-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2025-02-01 00:00:00' AND stunde < '2025-03-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2025-03-01 00:00:00' AND stunde < '2025-04-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2025-04-01 00:00:00' AND stunde < '2025-05-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2025-05-01 00:00:00' AND stunde < '2025-06-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2025-06-01 00:00:00' AND stunde < '2025-07-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2025-07-01 00:00:00' AND stunde < '2025-08-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2025-08-01 00:00:00' AND stunde < '2025-09-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2025-09-01 00:00:00' AND stunde < '2025-10-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2025-10-01 00:00:00' AND stunde < '2025-11-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2025-11-01 00:00:00' AND stunde < '2025-12-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2025-12-01 00:00:00' AND stunde < '2026-01-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2026-01-01 00:00:00' AND stunde < '2026-02-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2026-02-01 00:00:00' AND stunde < '2026-03-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2026-03-01 00:00:00' AND stunde < '2026-04-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2026-04-01 00:00:00' AND stunde < '2026-05-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2026-05-01 00:00:00' AND stunde < '2026-06-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2026-06-01 00:00:00' AND stunde < '2026-07-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2026-07-01 00:00:00' AND stunde < '2026-08-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_probe_lauf (stunde, process_id, message_status, anzahl)
SELECT stunde, process_id, message_status, anzahl FROM overlord_monitor.message_rollup
 WHERE stunde >= '2026-08-01 00:00:00' AND stunde < '2026-09-01 00:00:00';
INSERT INTO overlord_monitor.message_rollup_tag_probe_lauf (tag, process_id, message_status, anzahl)
SELECT tag, process_id, message_status, anzahl FROM overlord_monitor.message_rollup_tag;

SELECT '=== 03 Kontrolle: deckungsgleich? ===' AS marke;
SELECT (SELECT COUNT(*) FROM overlord_monitor.message_rollup)     AS quelle_stunden,
       (SELECT COUNT(*) FROM overlord_monitor.message_rollup_probe_lauf)                                AS probe_stunden,
       (SELECT COUNT(*) FROM overlord_monitor.message_rollup_tag) AS quelle_tage,
       (SELECT COUNT(*) FROM overlord_monitor.message_rollup_tag_probe_lauf)                                AS probe_tage;
SELECT COUNT(*) AS abweichende_stundenzeilen FROM (
  (SELECT * FROM overlord_monitor.message_rollup EXCEPT SELECT * FROM overlord_monitor.message_rollup_probe_lauf)
  UNION ALL
  (SELECT * FROM overlord_monitor.message_rollup_probe_lauf EXCEPT SELECT * FROM overlord_monitor.message_rollup)) d;
SELECT @@global.read_only AS read_only_am_ende, NOW() AS serverzeit_ende;
