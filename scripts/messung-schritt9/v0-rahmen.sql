-- V0 — Rahmen und Nachweis Testkopie (erstes und letztes Statement der Runde)
SELECT @@global.read_only AS read_only, VERSION() AS version, NOW() AS serverzeit,
       @@div_precision_increment AS dpi, @@session.sql_mode AS sql_mode;
SELECT USER() AS benutzer, CURRENT_USER() AS aufgeloest;
SELECT MAX(MessageLastUpdate) AS datenstand, COUNT(*) AS nachrichten FROM Message;
SELECT (SELECT COUNT(*) FROM Process) AS prozesse,
       (SELECT COUNT(*) FROM Project) AS projekte,
       (SELECT COUNT(*) FROM ProjectMandant) AS projektmandant,
       (SELECT COUNT(*) FROM SOS) AS sos,
       (SELECT COUNT(DISTINCT MandantID) FROM ProjectMandant) AS mandanten;
