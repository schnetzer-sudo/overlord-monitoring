-- Messrunde M83 — V0: Rahmen, Nachweis Testkopie, Datenstand (erstes Statement der Runde)
-- Auftrag:  "Messrunde M83 — die Bestandsabfrage vor E14", Fassung 1, 21.08.2026
-- Ergebnis: docs/messungen-schritt9.md, Nachtrag vom 21.08.2026 — M83
--
-- Ausschliesslich SELECT / SET / EXPLAIN / SHOW. (Regel S1)
-- G1: keine Zugangsdaten, kein Hostname, keine UUID, kein Partnername in dieser Datei.

-- V2 — Ziel ist die Testkopie. Muss 1 liefern, sonst Abbruch der Runde.
SELECT @@global.read_only AS read_only, VERSION() AS version, NOW() AS serverzeit;

SELECT USER() AS benutzer, CURRENT_USER() AS aufgeloest;

-- V3 — Datenstand gegenpruefen, nicht abschreiben.
-- Erwartet: datenstand = 2026-07-08 17:21:10, nachrichten = 3341519
SELECT MAX(MessageLastUpdate) AS datenstand, COUNT(*) AS nachrichten
FROM GlassfishDB.Message;

-- Bezugsgroessen der Runde
SELECT (SELECT COUNT(*) FROM GlassfishDB.Process)        AS prozesse,
       (SELECT COUNT(*) FROM GlassfishDB.Project)        AS projekte,
       (SELECT COUNT(*) FROM GlassfishDB.ProjectMandant) AS projektmandant,
       (SELECT COUNT(DISTINCT MandantID) FROM GlassfishDB.ProjectMandant) AS mandanten;

-- Rahmenwerte, die den Vergleich mit frueheren Runden tragen
SELECT @@div_precision_increment AS dpi,
       @@session.sql_mode        AS sql_mode,
       @@profiling_history_size  AS profiling_history_size_vorgabe,
       @@max_statement_time      AS max_statement_time_vorgabe,
       @@innodb_buffer_pool_size AS buffer_pool_size;
