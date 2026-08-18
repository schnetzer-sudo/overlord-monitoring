-- M66 (2) - wo endet der Bestand tatsaechlich?
--
-- Die jueng<ste Scheibe aus M66 ("letzte Woche vor 2026-07-08") war leer. Diese
-- Abfrage sucht die tatsaechlich letzten Tage mit Daten, damit die Scheibe
-- darauf gelegt werden kann. Dazu die Monatsverteilung 2026, um die Luecke aus
-- Offenem Punkt 18 gegen PROJEKTBESCHREIBUNG.md §8 zu pruefen.
--
-- Regel L9: beide Statements tragen kein Zeitfenster im Sinne der Regel, laufen
-- aber ausschliesslich ueber Message und sind auf 2026 eingegrenzt.
--
-- Ausschliesslich SELECT / SET. (Regel S1)

SELECT @@global.read_only AS global_read_only;
SET SESSION max_statement_time = 60;

-- (1) Monate 2026
SELECT DATE_FORMAT(m.MessageLastUpdate, '%Y-%m') AS monat, COUNT(*) AS nachrichten
FROM Message m
WHERE m.MessageLastUpdate >= '2026-01-01 00:00:00'
GROUP BY monat ORDER BY monat;

-- (2) die letzten Tage mit Daten
SELECT DATE(m.MessageLastUpdate) AS tag, COUNT(*) AS nachrichten,
       MIN(m.MessageLastUpdate) AS erster, MAX(m.MessageLastUpdate) AS letzter
FROM Message m
WHERE m.MessageLastUpdate >= '2026-06-01 00:00:00'
GROUP BY tag ORDER BY tag DESC LIMIT 15;
