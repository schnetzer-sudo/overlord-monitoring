-- M66 - Groesse der fuenf Zeitscheiben, vor dem Abruf
-- Auftrag Fassung 3, M66; Ergaenzung 5 vom 17.08.2026 (25 statt 50 je Scheibe)
--
-- Diese Abfrage ruft nichts ab. Sie sagt nur, wie viele Nachrichten je Scheibe
-- ueberhaupt vorhanden sind - damit eine leere Scheibe als Befund erkennbar
-- ist und nicht als Fehler des Abrufs.
--
-- Ausschliesslich SELECT / SET. (Regel S1)

SELECT @@global.read_only AS global_read_only;
SET SESSION max_statement_time = 60;

SELECT '2024-10' AS scheibe, COUNT(*) AS nachrichten
FROM Message m
WHERE m.MessageLastUpdate >= '2024-10-01 00:00:00' AND m.MessageLastUpdate < '2024-11-01 00:00:00'
UNION ALL
SELECT '2025-04', COUNT(*)
FROM Message m
WHERE m.MessageLastUpdate >= '2025-04-01 00:00:00' AND m.MessageLastUpdate < '2025-05-01 00:00:00'
UNION ALL
SELECT '2025-12', COUNT(*)
FROM Message m
WHERE m.MessageLastUpdate >= '2025-12-01 00:00:00' AND m.MessageLastUpdate < '2026-01-01 00:00:00'
UNION ALL
SELECT '2026-05', COUNT(*)
FROM Message m
WHERE m.MessageLastUpdate >= '2026-05-01 00:00:00' AND m.MessageLastUpdate < '2026-06-01 00:00:00'
UNION ALL
SELECT '2026-07-01..08', COUNT(*)
FROM Message m
WHERE m.MessageLastUpdate >= '2026-07-01 00:00:00' AND m.MessageLastUpdate < '2026-07-08 00:00:00';
