-- Messrunde M86-M92 — Sitzung 4b: M88 "Was kostet der stuendliche Delta-Lauf?"
--
-- Die gemessene Abfrage ist die aus dem Auftrag, unveraendert:
--
--   SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde,
--          ProcessID, MessageStatus, COUNT(*) AS anzahl
--   FROM Message
--   WHERE MessageLastUpdate >= ? AND MessageLastUpdate < ?
--   GROUP BY stunde, ProcessID, MessageStatus;
--
-- AUSDRUECKLICH NICHT: STRAIGHT_JOIN, in keiner Fassung (M42, Faktor 219-1094x).
--
-- SECHS FENSTER statt der drei aus dem Auftrag. Begruendung, und sie ist keine
-- Ausweitung um ihrer selbst willen:
--   Sitzung 4a hat gezeigt, dass Fenster D **256 Zeilen auf zwei Prozessen** traegt,
--   alle innerhalb von vier Minuten. Eine Laufzeit gegen Fenster D misst deshalb einen
--   leeren Indexbereich und nicht den Delta-Lauf. Damit die Frage des Auftrags trotzdem
--   beantwortet wird, laufen daneben die dichten Entsprechungen:
--     H1  dichteste Stunde des GANZEN Bestands   2025-12-07 17:00  (8.630 Zeilen)
--     H2  dichteste Stunde des Fensters A        2025-12-29 22:00  (2.883 Zeilen)
--     H3  letzte Stunde des Bestands             2026-07-08 17:00  (285 Zeilen)
--     D   Fenster D, 48 h bis zum Anker V5                         (256 Zeilen)
--     D2  dichtes 48-h-Fenster als Gegenstueck   2025-12-28..30    (~ 10.000 Zeilen)
--     A   Fenster A, ein Tag                     2025-12-29        (6.249 Zeilen)
--   H1, H2, H3 und A sind die drei vom Auftrag verlangten Fenster plus zwei Stunden,
--   die den Bereich zwischen leer und dicht aufspannen. D2 ist das, was Fenster D
--   waere, wenn der Bestand dort nicht aufgehoert haette.
--
-- Je Fenster: ein Aufwaermlauf, dann fuenf Laeufe. Ausgewiesen wird die beste von fuenf
-- und der Aufwaermlauf getrennt. EXPLAIN steht am ENDE, damit der Aufwaermlauf wirklich
-- der erste Zugriff auf das jeweilige Fenster ist.
--
-- Ausschliesslich SELECT / SET / EXPLAIN. (Regel S1)   Z1: alle Grenzen als Literal.
-- G1: die Abfrage liefert ProcessID — sie steht in der Rohausgabe unter ergebnis/ und
--     ist ueber .gitignore ausgeschlossen. In der Ergebnisdatei stehen nur Anzahlen.

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;

SET max_statement_time = 60;
SET profiling_history_size = 100;
SET profiling = 1;

-- ===== H1 — dichteste Stunde des Bestands: 2025-12-07 17:00 ================
SELECT '=== H1-0 Aufwaermlauf ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde, ProcessID, MessageStatus, COUNT(*) AS anzahl
FROM GlassfishDB.Message WHERE MessageLastUpdate >= '2025-12-07 17:00:00' AND MessageLastUpdate < '2025-12-07 18:00:00'
GROUP BY stunde, ProcessID, MessageStatus;
SELECT '=== H1-1 ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde, ProcessID, MessageStatus, COUNT(*) AS anzahl
FROM GlassfishDB.Message WHERE MessageLastUpdate >= '2025-12-07 17:00:00' AND MessageLastUpdate < '2025-12-07 18:00:00'
GROUP BY stunde, ProcessID, MessageStatus;
SELECT '=== H1-2 ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde, ProcessID, MessageStatus, COUNT(*) AS anzahl
FROM GlassfishDB.Message WHERE MessageLastUpdate >= '2025-12-07 17:00:00' AND MessageLastUpdate < '2025-12-07 18:00:00'
GROUP BY stunde, ProcessID, MessageStatus;
SELECT '=== H1-3 ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde, ProcessID, MessageStatus, COUNT(*) AS anzahl
FROM GlassfishDB.Message WHERE MessageLastUpdate >= '2025-12-07 17:00:00' AND MessageLastUpdate < '2025-12-07 18:00:00'
GROUP BY stunde, ProcessID, MessageStatus;
SELECT '=== H1-4 ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde, ProcessID, MessageStatus, COUNT(*) AS anzahl
FROM GlassfishDB.Message WHERE MessageLastUpdate >= '2025-12-07 17:00:00' AND MessageLastUpdate < '2025-12-07 18:00:00'
GROUP BY stunde, ProcessID, MessageStatus;
SELECT '=== H1-5 ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde, ProcessID, MessageStatus, COUNT(*) AS anzahl
FROM GlassfishDB.Message WHERE MessageLastUpdate >= '2025-12-07 17:00:00' AND MessageLastUpdate < '2025-12-07 18:00:00'
GROUP BY stunde, ProcessID, MessageStatus;

-- ===== H2 — dichteste Stunde des Fensters A: 2025-12-29 22:00 ==============
SELECT '=== H2-0 Aufwaermlauf ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde, ProcessID, MessageStatus, COUNT(*) AS anzahl
FROM GlassfishDB.Message WHERE MessageLastUpdate >= '2025-12-29 22:00:00' AND MessageLastUpdate < '2025-12-29 23:00:00'
GROUP BY stunde, ProcessID, MessageStatus;
SELECT '=== H2-1 ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde, ProcessID, MessageStatus, COUNT(*) AS anzahl
FROM GlassfishDB.Message WHERE MessageLastUpdate >= '2025-12-29 22:00:00' AND MessageLastUpdate < '2025-12-29 23:00:00'
GROUP BY stunde, ProcessID, MessageStatus;
SELECT '=== H2-2 ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde, ProcessID, MessageStatus, COUNT(*) AS anzahl
FROM GlassfishDB.Message WHERE MessageLastUpdate >= '2025-12-29 22:00:00' AND MessageLastUpdate < '2025-12-29 23:00:00'
GROUP BY stunde, ProcessID, MessageStatus;
SELECT '=== H2-3 ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde, ProcessID, MessageStatus, COUNT(*) AS anzahl
FROM GlassfishDB.Message WHERE MessageLastUpdate >= '2025-12-29 22:00:00' AND MessageLastUpdate < '2025-12-29 23:00:00'
GROUP BY stunde, ProcessID, MessageStatus;
SELECT '=== H2-4 ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde, ProcessID, MessageStatus, COUNT(*) AS anzahl
FROM GlassfishDB.Message WHERE MessageLastUpdate >= '2025-12-29 22:00:00' AND MessageLastUpdate < '2025-12-29 23:00:00'
GROUP BY stunde, ProcessID, MessageStatus;
SELECT '=== H2-5 ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde, ProcessID, MessageStatus, COUNT(*) AS anzahl
FROM GlassfishDB.Message WHERE MessageLastUpdate >= '2025-12-29 22:00:00' AND MessageLastUpdate < '2025-12-29 23:00:00'
GROUP BY stunde, ProcessID, MessageStatus;

-- ===== H3 — letzte Stunde des Bestands: 2026-07-08 17:00 ==================
SELECT '=== H3-0 Aufwaermlauf ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde, ProcessID, MessageStatus, COUNT(*) AS anzahl
FROM GlassfishDB.Message WHERE MessageLastUpdate >= '2026-07-08 17:00:00' AND MessageLastUpdate < '2026-07-08 18:00:00'
GROUP BY stunde, ProcessID, MessageStatus;
SELECT '=== H3-1 ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde, ProcessID, MessageStatus, COUNT(*) AS anzahl
FROM GlassfishDB.Message WHERE MessageLastUpdate >= '2026-07-08 17:00:00' AND MessageLastUpdate < '2026-07-08 18:00:00'
GROUP BY stunde, ProcessID, MessageStatus;
SELECT '=== H3-2 ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde, ProcessID, MessageStatus, COUNT(*) AS anzahl
FROM GlassfishDB.Message WHERE MessageLastUpdate >= '2026-07-08 17:00:00' AND MessageLastUpdate < '2026-07-08 18:00:00'
GROUP BY stunde, ProcessID, MessageStatus;
SELECT '=== H3-3 ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde, ProcessID, MessageStatus, COUNT(*) AS anzahl
FROM GlassfishDB.Message WHERE MessageLastUpdate >= '2026-07-08 17:00:00' AND MessageLastUpdate < '2026-07-08 18:00:00'
GROUP BY stunde, ProcessID, MessageStatus;
SELECT '=== H3-4 ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde, ProcessID, MessageStatus, COUNT(*) AS anzahl
FROM GlassfishDB.Message WHERE MessageLastUpdate >= '2026-07-08 17:00:00' AND MessageLastUpdate < '2026-07-08 18:00:00'
GROUP BY stunde, ProcessID, MessageStatus;
SELECT '=== H3-5 ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde, ProcessID, MessageStatus, COUNT(*) AS anzahl
FROM GlassfishDB.Message WHERE MessageLastUpdate >= '2026-07-08 17:00:00' AND MessageLastUpdate < '2026-07-08 18:00:00'
GROUP BY stunde, ProcessID, MessageStatus;

-- ===== D — Fenster D: 48 h bis zum Anker aus V5 ===========================
SELECT '=== D-0 Aufwaermlauf ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde, ProcessID, MessageStatus, COUNT(*) AS anzahl
FROM GlassfishDB.Message WHERE MessageLastUpdate >= '2026-07-06 17:21:10' AND MessageLastUpdate < '2026-07-08 17:21:10'
GROUP BY stunde, ProcessID, MessageStatus;
SELECT '=== D-1 ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde, ProcessID, MessageStatus, COUNT(*) AS anzahl
FROM GlassfishDB.Message WHERE MessageLastUpdate >= '2026-07-06 17:21:10' AND MessageLastUpdate < '2026-07-08 17:21:10'
GROUP BY stunde, ProcessID, MessageStatus;
SELECT '=== D-2 ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde, ProcessID, MessageStatus, COUNT(*) AS anzahl
FROM GlassfishDB.Message WHERE MessageLastUpdate >= '2026-07-06 17:21:10' AND MessageLastUpdate < '2026-07-08 17:21:10'
GROUP BY stunde, ProcessID, MessageStatus;
SELECT '=== D-3 ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde, ProcessID, MessageStatus, COUNT(*) AS anzahl
FROM GlassfishDB.Message WHERE MessageLastUpdate >= '2026-07-06 17:21:10' AND MessageLastUpdate < '2026-07-08 17:21:10'
GROUP BY stunde, ProcessID, MessageStatus;
SELECT '=== D-4 ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde, ProcessID, MessageStatus, COUNT(*) AS anzahl
FROM GlassfishDB.Message WHERE MessageLastUpdate >= '2026-07-06 17:21:10' AND MessageLastUpdate < '2026-07-08 17:21:10'
GROUP BY stunde, ProcessID, MessageStatus;
SELECT '=== D-5 ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde, ProcessID, MessageStatus, COUNT(*) AS anzahl
FROM GlassfishDB.Message WHERE MessageLastUpdate >= '2026-07-06 17:21:10' AND MessageLastUpdate < '2026-07-08 17:21:10'
GROUP BY stunde, ProcessID, MessageStatus;

-- ===== D2 — dichtes 48-h-Fenster als Gegenstueck zu D =====================
SELECT '=== D2-0 Aufwaermlauf ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde, ProcessID, MessageStatus, COUNT(*) AS anzahl
FROM GlassfishDB.Message WHERE MessageLastUpdate >= '2025-12-28 00:00:00' AND MessageLastUpdate < '2025-12-30 00:00:00'
GROUP BY stunde, ProcessID, MessageStatus;
SELECT '=== D2-1 ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde, ProcessID, MessageStatus, COUNT(*) AS anzahl
FROM GlassfishDB.Message WHERE MessageLastUpdate >= '2025-12-28 00:00:00' AND MessageLastUpdate < '2025-12-30 00:00:00'
GROUP BY stunde, ProcessID, MessageStatus;
SELECT '=== D2-2 ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde, ProcessID, MessageStatus, COUNT(*) AS anzahl
FROM GlassfishDB.Message WHERE MessageLastUpdate >= '2025-12-28 00:00:00' AND MessageLastUpdate < '2025-12-30 00:00:00'
GROUP BY stunde, ProcessID, MessageStatus;
SELECT '=== D2-3 ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde, ProcessID, MessageStatus, COUNT(*) AS anzahl
FROM GlassfishDB.Message WHERE MessageLastUpdate >= '2025-12-28 00:00:00' AND MessageLastUpdate < '2025-12-30 00:00:00'
GROUP BY stunde, ProcessID, MessageStatus;
SELECT '=== D2-4 ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde, ProcessID, MessageStatus, COUNT(*) AS anzahl
FROM GlassfishDB.Message WHERE MessageLastUpdate >= '2025-12-28 00:00:00' AND MessageLastUpdate < '2025-12-30 00:00:00'
GROUP BY stunde, ProcessID, MessageStatus;
SELECT '=== D2-5 ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde, ProcessID, MessageStatus, COUNT(*) AS anzahl
FROM GlassfishDB.Message WHERE MessageLastUpdate >= '2025-12-28 00:00:00' AND MessageLastUpdate < '2025-12-30 00:00:00'
GROUP BY stunde, ProcessID, MessageStatus;

-- ===== A — Fenster A: ein Tag ============================================
SELECT '=== A-0 Aufwaermlauf ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde, ProcessID, MessageStatus, COUNT(*) AS anzahl
FROM GlassfishDB.Message WHERE MessageLastUpdate >= '2025-12-29 00:00:00' AND MessageLastUpdate < '2025-12-30 00:00:00'
GROUP BY stunde, ProcessID, MessageStatus;
SELECT '=== A-1 ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde, ProcessID, MessageStatus, COUNT(*) AS anzahl
FROM GlassfishDB.Message WHERE MessageLastUpdate >= '2025-12-29 00:00:00' AND MessageLastUpdate < '2025-12-30 00:00:00'
GROUP BY stunde, ProcessID, MessageStatus;
SELECT '=== A-2 ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde, ProcessID, MessageStatus, COUNT(*) AS anzahl
FROM GlassfishDB.Message WHERE MessageLastUpdate >= '2025-12-29 00:00:00' AND MessageLastUpdate < '2025-12-30 00:00:00'
GROUP BY stunde, ProcessID, MessageStatus;
SELECT '=== A-3 ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde, ProcessID, MessageStatus, COUNT(*) AS anzahl
FROM GlassfishDB.Message WHERE MessageLastUpdate >= '2025-12-29 00:00:00' AND MessageLastUpdate < '2025-12-30 00:00:00'
GROUP BY stunde, ProcessID, MessageStatus;
SELECT '=== A-4 ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde, ProcessID, MessageStatus, COUNT(*) AS anzahl
FROM GlassfishDB.Message WHERE MessageLastUpdate >= '2025-12-29 00:00:00' AND MessageLastUpdate < '2025-12-30 00:00:00'
GROUP BY stunde, ProcessID, MessageStatus;
SELECT '=== A-5 ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde, ProcessID, MessageStatus, COUNT(*) AS anzahl
FROM GlassfishDB.Message WHERE MessageLastUpdate >= '2025-12-29 00:00:00' AND MessageLastUpdate < '2025-12-30 00:00:00'
GROUP BY stunde, ProcessID, MessageStatus;

-- ===== L15 — die Plaene, am Ende der Sitzung =============================
SELECT '=== EXPLAIN H1 (dichteste Stunde) ===' AS marke;
EXPLAIN SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde, ProcessID, MessageStatus, COUNT(*) AS anzahl
FROM GlassfishDB.Message WHERE MessageLastUpdate >= '2025-12-07 17:00:00' AND MessageLastUpdate < '2025-12-07 18:00:00'
GROUP BY stunde, ProcessID, MessageStatus;

SELECT '=== EXPLAIN H3 (letzte Stunde) ===' AS marke;
EXPLAIN SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde, ProcessID, MessageStatus, COUNT(*) AS anzahl
FROM GlassfishDB.Message WHERE MessageLastUpdate >= '2026-07-08 17:00:00' AND MessageLastUpdate < '2026-07-08 18:00:00'
GROUP BY stunde, ProcessID, MessageStatus;

SELECT '=== EXPLAIN D (Fenster D) ===' AS marke;
EXPLAIN SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde, ProcessID, MessageStatus, COUNT(*) AS anzahl
FROM GlassfishDB.Message WHERE MessageLastUpdate >= '2026-07-06 17:21:10' AND MessageLastUpdate < '2026-07-08 17:21:10'
GROUP BY stunde, ProcessID, MessageStatus;

SELECT '=== EXPLAIN D2 (dichte 48 h) ===' AS marke;
EXPLAIN SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde, ProcessID, MessageStatus, COUNT(*) AS anzahl
FROM GlassfishDB.Message WHERE MessageLastUpdate >= '2025-12-28 00:00:00' AND MessageLastUpdate < '2025-12-30 00:00:00'
GROUP BY stunde, ProcessID, MessageStatus;

SELECT '=== EXPLAIN A (Fenster A) ===' AS marke;
EXPLAIN SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde, ProcessID, MessageStatus, COUNT(*) AS anzahl
FROM GlassfishDB.Message WHERE MessageLastUpdate >= '2025-12-29 00:00:00' AND MessageLastUpdate < '2025-12-30 00:00:00'
GROUP BY stunde, ProcessID, MessageStatus;

SELECT '=== EXPLAIN FORMAT=JSON H1 (welcher Index, wie weit?) ===' AS marke;
EXPLAIN FORMAT=JSON SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde, ProcessID, MessageStatus, COUNT(*) AS anzahl
FROM GlassfishDB.Message WHERE MessageLastUpdate >= '2025-12-07 17:00:00' AND MessageLastUpdate < '2025-12-07 18:00:00'
GROUP BY stunde, ProcessID, MessageStatus;

-- Ergebnisgroessen je Fenster, damit die Laufzeiten eine Bezugsgroesse haben.
SELECT '=== Bezugsgroessen der sechs Fenster ===' AS marke;
SELECT 'H1 2025-12-07 17:00' AS fenster, COUNT(*) AS zeilen,
       COUNT(DISTINCT ProcessID, MessageStatus) AS ergebniszeilen
FROM GlassfishDB.Message WHERE MessageLastUpdate >= '2025-12-07 17:00:00' AND MessageLastUpdate < '2025-12-07 18:00:00'
UNION ALL SELECT 'H2 2025-12-29 22:00', COUNT(*), COUNT(DISTINCT ProcessID, MessageStatus)
FROM GlassfishDB.Message WHERE MessageLastUpdate >= '2025-12-29 22:00:00' AND MessageLastUpdate < '2025-12-29 23:00:00'
UNION ALL SELECT 'H3 2026-07-08 17:00', COUNT(*), COUNT(DISTINCT ProcessID, MessageStatus)
FROM GlassfishDB.Message WHERE MessageLastUpdate >= '2026-07-08 17:00:00' AND MessageLastUpdate < '2026-07-08 18:00:00'
UNION ALL SELECT 'D  Fenster D 48h', COUNT(*), COUNT(DISTINCT DATE_FORMAT(MessageLastUpdate,'%Y-%m-%d %H'), ProcessID, MessageStatus)
FROM GlassfishDB.Message WHERE MessageLastUpdate >= '2026-07-06 17:21:10' AND MessageLastUpdate < '2026-07-08 17:21:10'
UNION ALL SELECT 'D2 dichte 48h', COUNT(*), COUNT(DISTINCT DATE_FORMAT(MessageLastUpdate,'%Y-%m-%d %H'), ProcessID, MessageStatus)
FROM GlassfishDB.Message WHERE MessageLastUpdate >= '2025-12-28 00:00:00' AND MessageLastUpdate < '2025-12-30 00:00:00'
UNION ALL SELECT 'A  Fenster A 1 Tag', COUNT(*), COUNT(DISTINCT DATE_FORMAT(MessageLastUpdate,'%Y-%m-%d %H'), ProcessID, MessageStatus)
FROM GlassfishDB.Message WHERE MessageLastUpdate >= '2025-12-29 00:00:00' AND MessageLastUpdate < '2025-12-30 00:00:00';

SELECT '=== 98 SHOW PROFILES ===' AS marke;
SHOW PROFILES;

SELECT '=== 99 Abschluss ===' AS marke;
SELECT @@global.read_only AS read_only_ende, NOW() AS serverzeit_ende;
