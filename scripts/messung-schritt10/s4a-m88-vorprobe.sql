-- Messrunde M86-M92 — Sitzung 4a: Vorprobe zu M88
--
-- WOZU. M88 verlangt drei Fenster: "eine Stunde", Fenster D (48 h) und Fenster A (ein Tag).
-- WELCHE Stunde, sagt der Auftrag nicht. Sie wird hier hergeleitet statt geraten:
-- gemessen wird die dichteste Stunde des Fensters A und, als Gegenstueck, die letzte
-- Stunde des Bestands. Erst danach steht das Literal fuer Sitzung 4b fest (Regel Z1 —
-- der Zeitpunkt gehoert im Klartext ins Statement, also muss er vorher bekannt sein).
--
-- Zugleich beantwortet diese Vorprobe die Frage, die Befund 7 aufwirft: Wie viel steht
-- ueberhaupt in Fenster D? Fenster D ist das Standardfenster des Dashboards und endet am
-- Anker aus V5 — in einem Jahr, das nur 5.133 Zeilen traegt.
--
-- Ausschliesslich SELECT / SET. (Regel S1)   Z1: alle Grenzen als Literal.
-- Fenster A: >= '2025-12-29 00:00:00' AND < '2025-12-30 00:00:00'
-- Fenster D: >= '2026-07-06 17:21:10' AND < '2026-07-08 17:21:10'  (48 h, Ende = Anker V5)

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;

SET max_statement_time = 60;
SET profiling_history_size = 100;
SET profiling = 1;

SELECT '=== 4a-1 Fenster D: was steht ueberhaupt darin? ===' AS marke;
SELECT COUNT(*)                      AS zeilen_fenster_d,
       COUNT(DISTINCT ProcessID)     AS prozesse,
       COUNT(DISTINCT MessageStatus) AS status,
       MIN(MessageLastUpdate)        AS frueheste,
       MAX(MessageLastUpdate)        AS spaeteste
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2026-07-06 17:21:10'
  AND MessageLastUpdate <  '2026-07-08 17:21:10';

SELECT '=== 4a-2 Die letzten 30 Tage des Bestands, je Tag ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d') AS tag,
       COUNT(*) AS zeilen,
       COUNT(DISTINCT ProcessID) AS prozesse
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2026-06-08 00:00:00'
  AND MessageLastUpdate <  '2026-07-09 00:00:00'
GROUP BY tag
ORDER BY tag;

SELECT '=== 4a-3 Fenster A: Verteilung ueber die 24 Stunden ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde,
       COUNT(*) AS zeilen,
       COUNT(DISTINCT ProcessID) AS prozesse,
       COUNT(DISTINCT ProcessID, MessageStatus) AS rollupzeilen
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2025-12-29 00:00:00'
  AND MessageLastUpdate <  '2025-12-30 00:00:00'
GROUP BY stunde
ORDER BY zeilen DESC;

SELECT '=== 4a-4 Die dichteste Stunde des ganzen Bestands (Bezugsgroesse, L9) ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde,
       COUNT(*) AS zeilen
FROM GlassfishDB.Message
GROUP BY stunde
ORDER BY zeilen DESC
LIMIT 5;

SELECT '=== 98 SHOW PROFILES ===' AS marke;
SHOW PROFILES;

SELECT '=== 99 Abschluss ===' AS marke;
SELECT @@global.read_only AS read_only_ende, NOW() AS serverzeit_ende;
