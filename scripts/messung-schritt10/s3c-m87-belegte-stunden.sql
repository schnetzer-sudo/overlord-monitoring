-- Messrunde M86-M92 — Sitzung 3c: M87, zwei Zahlen, die in M87-5 mit abgebrochen sind
--
-- M87-5 sollte neben den vier Varianten auch
--   COUNT(DISTINCT stunde, ProcessID)  — belegte Stunde-Prozess-Paare
--   COUNT(DISTINCT stunde)             — belegte Stundeneimer
-- liefern. Das Statement ist an max_statement_time = 60 abgebrochen (60,219 s), und die
-- Jahresscheiben aus Sitzung 3b haben diese beiden Spalten nicht gefuehrt. Sie fehlten
-- damit; hier werden sie nachgeholt.
--
-- WARUM NICHT SUMMIERBAR WIE DIE VIER VARIANTEN: Bei COUNT(DISTINCT stunde, ProcessID)
-- gilt dasselbe Argument wie dort — die Stunde steht im Schluessel, die Jahresscheiben sind
-- disjunkt, die Summe ist exakt. Bei COUNT(DISTINCT ProcessID) waere sie es NICHT (derselbe
-- Prozess kommt in mehreren Jahren vor); diese Spalte wird deshalb nicht summiert, sondern
-- steht schon aus M87-0 mit 738 fest.
--
-- L9: voller Durchlauf ueber Message, je Jahresscheibe. Begruendung wie in M87 — die
-- Mengengeruest-Frage der Tabelle, die es nicht gibt. Kein Vorbild fuer Anwendungscode.
-- Ausschliesslich SELECT / SET. (Regel S1)   Z1: alle Grenzen als Literal.

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;

SET max_statement_time = 60;
SET profiling_history_size = 100;
SET profiling = 1;

SELECT '=== M87-7a Scheibe 2024 ===' AS marke;
SELECT '2024' AS scheibe,
       COUNT(DISTINCT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00'), ProcessID) AS stunde_prozess,
       COUNT(DISTINCT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00'))            AS belegte_stunden
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2024-10-01 00:00:00' AND MessageLastUpdate < '2025-01-01 00:00:00';

SELECT '=== M87-7b Scheibe 2025 ===' AS marke;
SELECT '2025' AS scheibe,
       COUNT(DISTINCT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00'), ProcessID) AS stunde_prozess,
       COUNT(DISTINCT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00'))            AS belegte_stunden
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2025-01-01 00:00:00' AND MessageLastUpdate < '2026-01-01 00:00:00';

SELECT '=== M87-7c Scheibe 2026 ===' AS marke;
SELECT '2026' AS scheibe,
       COUNT(DISTINCT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00'), ProcessID) AS stunde_prozess,
       COUNT(DISTINCT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00'))            AS belegte_stunden
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2026-01-01 00:00:00' AND MessageLastUpdate < '2026-08-01 00:00:00';

SELECT '=== 98 SHOW PROFILES ===' AS marke;
SHOW PROFILES;
SELECT '=== 99 Abschluss ===' AS marke;
SELECT @@global.read_only AS read_only_ende, NOW() AS serverzeit_ende;
