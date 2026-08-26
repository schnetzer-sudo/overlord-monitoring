-- Messrunde M86-M92 — Sitzung 7b: M92, was kostet der Rueckwaertslauf?
--
-- Gemessen wird die Aggregation aus M88, unveraendert, ueber Monatsscheiben.
-- Kein STRAIGHT_JOIN, in keiner Fassung (M42).
--
-- VIER SCHEIBEN:
--   B     Fenster B, woertlich aus M17          2025-11-30 .. 2025-12-30   214.330 Zeilen
--   OKT   "der duenne Anfang des Bestands"      2024-10-01 .. 2024-11-01   241.203 Zeilen
--   MAX   die groesste Monatsscheibe (M92-2)    2025-07-01 .. 2025-08-01   248.320 Zeilen
--   LEER  eine der fuenf leeren Scheiben        2026-01-01 .. 2026-02-01         0 Zeilen
--
-- ZU "OKT": Der Auftrag nennt Oktober 2024 "eine Scheibe aus dem duennen Anfang des
-- Bestands". Sitzung 7a widerlegt das — Oktober 2024 traegt 241.203 Zeilen und ist die
-- DRITTGROESSTE der 22 Scheiben. Die Scheibe wird trotzdem gefahren, weil der Auftrag sie
-- verlangt; der Befund steht unter L10 in der Ergebnisdatei.
--
-- ZU "LEER": Sie steht nicht im Auftrag. Sie ist dazugekommen, weil M92 ausdruecklich
-- verlangt, dass die fuenf leeren Monate "als leere Scheiben auftauchen" und ein
-- Rueckwaertslauf ueber sie nicht stolpern darf. Was eine leere Scheibe KOSTET, gehoert
-- zur Bauform dazu.
--
-- Je Scheibe ein Aufwaermlauf, dann fuenf Laeufe. Beste von fuenf wird ausgewiesen,
-- der Aufwaermlauf getrennt. EXPLAIN am Ende der Sitzung.
--
-- Ausschliesslich SELECT / SET / EXPLAIN. (Regel S1)   Z1: alle Grenzen als Literal.
-- G1: Die Abfrage liefert ProcessID. Die Rohausgabe liegt unter ergebnis/ und ist ueber
--     .gitignore ausgeschlossen; in der Ergebnisdatei stehen nur Anzahlen und Laufzeiten.

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;

SET max_statement_time = 60;
SET profiling_history_size = 100;
SET profiling = 1;

SELECT '=== B-0 Aufwaermlauf ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde,
       ProcessID,
       MessageStatus,
       COUNT(*) AS anzahl
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2025-11-30 00:00:00'
  AND MessageLastUpdate <  '2025-12-30 00:00:00'
GROUP BY stunde, ProcessID, MessageStatus;

SELECT '=== B-1 ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde,
       ProcessID,
       MessageStatus,
       COUNT(*) AS anzahl
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2025-11-30 00:00:00'
  AND MessageLastUpdate <  '2025-12-30 00:00:00'
GROUP BY stunde, ProcessID, MessageStatus;

SELECT '=== B-2 ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde,
       ProcessID,
       MessageStatus,
       COUNT(*) AS anzahl
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2025-11-30 00:00:00'
  AND MessageLastUpdate <  '2025-12-30 00:00:00'
GROUP BY stunde, ProcessID, MessageStatus;

SELECT '=== B-3 ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde,
       ProcessID,
       MessageStatus,
       COUNT(*) AS anzahl
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2025-11-30 00:00:00'
  AND MessageLastUpdate <  '2025-12-30 00:00:00'
GROUP BY stunde, ProcessID, MessageStatus;

SELECT '=== B-4 ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde,
       ProcessID,
       MessageStatus,
       COUNT(*) AS anzahl
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2025-11-30 00:00:00'
  AND MessageLastUpdate <  '2025-12-30 00:00:00'
GROUP BY stunde, ProcessID, MessageStatus;

SELECT '=== B-5 ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde,
       ProcessID,
       MessageStatus,
       COUNT(*) AS anzahl
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2025-11-30 00:00:00'
  AND MessageLastUpdate <  '2025-12-30 00:00:00'
GROUP BY stunde, ProcessID, MessageStatus;

SELECT '=== OKT-0 Aufwaermlauf ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde,
       ProcessID,
       MessageStatus,
       COUNT(*) AS anzahl
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2024-10-01 00:00:00'
  AND MessageLastUpdate <  '2024-11-01 00:00:00'
GROUP BY stunde, ProcessID, MessageStatus;

SELECT '=== OKT-1 ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde,
       ProcessID,
       MessageStatus,
       COUNT(*) AS anzahl
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2024-10-01 00:00:00'
  AND MessageLastUpdate <  '2024-11-01 00:00:00'
GROUP BY stunde, ProcessID, MessageStatus;

SELECT '=== OKT-2 ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde,
       ProcessID,
       MessageStatus,
       COUNT(*) AS anzahl
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2024-10-01 00:00:00'
  AND MessageLastUpdate <  '2024-11-01 00:00:00'
GROUP BY stunde, ProcessID, MessageStatus;

SELECT '=== OKT-3 ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde,
       ProcessID,
       MessageStatus,
       COUNT(*) AS anzahl
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2024-10-01 00:00:00'
  AND MessageLastUpdate <  '2024-11-01 00:00:00'
GROUP BY stunde, ProcessID, MessageStatus;

SELECT '=== OKT-4 ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde,
       ProcessID,
       MessageStatus,
       COUNT(*) AS anzahl
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2024-10-01 00:00:00'
  AND MessageLastUpdate <  '2024-11-01 00:00:00'
GROUP BY stunde, ProcessID, MessageStatus;

SELECT '=== OKT-5 ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde,
       ProcessID,
       MessageStatus,
       COUNT(*) AS anzahl
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2024-10-01 00:00:00'
  AND MessageLastUpdate <  '2024-11-01 00:00:00'
GROUP BY stunde, ProcessID, MessageStatus;

SELECT '=== MAX-0 Aufwaermlauf ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde,
       ProcessID,
       MessageStatus,
       COUNT(*) AS anzahl
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2025-07-01 00:00:00'
  AND MessageLastUpdate <  '2025-08-01 00:00:00'
GROUP BY stunde, ProcessID, MessageStatus;

SELECT '=== MAX-1 ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde,
       ProcessID,
       MessageStatus,
       COUNT(*) AS anzahl
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2025-07-01 00:00:00'
  AND MessageLastUpdate <  '2025-08-01 00:00:00'
GROUP BY stunde, ProcessID, MessageStatus;

SELECT '=== MAX-2 ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde,
       ProcessID,
       MessageStatus,
       COUNT(*) AS anzahl
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2025-07-01 00:00:00'
  AND MessageLastUpdate <  '2025-08-01 00:00:00'
GROUP BY stunde, ProcessID, MessageStatus;

SELECT '=== MAX-3 ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde,
       ProcessID,
       MessageStatus,
       COUNT(*) AS anzahl
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2025-07-01 00:00:00'
  AND MessageLastUpdate <  '2025-08-01 00:00:00'
GROUP BY stunde, ProcessID, MessageStatus;

SELECT '=== MAX-4 ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde,
       ProcessID,
       MessageStatus,
       COUNT(*) AS anzahl
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2025-07-01 00:00:00'
  AND MessageLastUpdate <  '2025-08-01 00:00:00'
GROUP BY stunde, ProcessID, MessageStatus;

SELECT '=== MAX-5 ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde,
       ProcessID,
       MessageStatus,
       COUNT(*) AS anzahl
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2025-07-01 00:00:00'
  AND MessageLastUpdate <  '2025-08-01 00:00:00'
GROUP BY stunde, ProcessID, MessageStatus;

SELECT '=== LEER-0 Aufwaermlauf ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde,
       ProcessID,
       MessageStatus,
       COUNT(*) AS anzahl
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2026-01-01 00:00:00'
  AND MessageLastUpdate <  '2026-02-01 00:00:00'
GROUP BY stunde, ProcessID, MessageStatus;

SELECT '=== LEER-1 ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde,
       ProcessID,
       MessageStatus,
       COUNT(*) AS anzahl
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2026-01-01 00:00:00'
  AND MessageLastUpdate <  '2026-02-01 00:00:00'
GROUP BY stunde, ProcessID, MessageStatus;

SELECT '=== LEER-2 ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde,
       ProcessID,
       MessageStatus,
       COUNT(*) AS anzahl
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2026-01-01 00:00:00'
  AND MessageLastUpdate <  '2026-02-01 00:00:00'
GROUP BY stunde, ProcessID, MessageStatus;

SELECT '=== LEER-3 ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde,
       ProcessID,
       MessageStatus,
       COUNT(*) AS anzahl
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2026-01-01 00:00:00'
  AND MessageLastUpdate <  '2026-02-01 00:00:00'
GROUP BY stunde, ProcessID, MessageStatus;

SELECT '=== LEER-4 ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde,
       ProcessID,
       MessageStatus,
       COUNT(*) AS anzahl
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2026-01-01 00:00:00'
  AND MessageLastUpdate <  '2026-02-01 00:00:00'
GROUP BY stunde, ProcessID, MessageStatus;

SELECT '=== LEER-5 ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde,
       ProcessID,
       MessageStatus,
       COUNT(*) AS anzahl
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2026-01-01 00:00:00'
  AND MessageLastUpdate <  '2026-02-01 00:00:00'
GROUP BY stunde, ProcessID, MessageStatus;

-- ===== L15 =====

SELECT '=== EXPLAIN B ===' AS marke;
EXPLAIN
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde,
       ProcessID,
       MessageStatus,
       COUNT(*) AS anzahl
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2025-11-30 00:00:00'
  AND MessageLastUpdate <  '2025-12-30 00:00:00'
GROUP BY stunde, ProcessID, MessageStatus;

SELECT '=== EXPLAIN OKT ===' AS marke;
EXPLAIN
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde,
       ProcessID,
       MessageStatus,
       COUNT(*) AS anzahl
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2024-10-01 00:00:00'
  AND MessageLastUpdate <  '2024-11-01 00:00:00'
GROUP BY stunde, ProcessID, MessageStatus;

SELECT '=== EXPLAIN MAX ===' AS marke;
EXPLAIN
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde,
       ProcessID,
       MessageStatus,
       COUNT(*) AS anzahl
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2025-07-01 00:00:00'
  AND MessageLastUpdate <  '2025-08-01 00:00:00'
GROUP BY stunde, ProcessID, MessageStatus;

SELECT '=== EXPLAIN LEER ===' AS marke;
EXPLAIN
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde,
       ProcessID,
       MessageStatus,
       COUNT(*) AS anzahl
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2026-01-01 00:00:00'
  AND MessageLastUpdate <  '2026-02-01 00:00:00'
GROUP BY stunde, ProcessID, MessageStatus;

SELECT '=== M92-4 Ergebnisgroessen je Scheibe ===' AS marke;
SELECT 'B   2025-11-30..12-30' AS scheibe, COUNT(*) AS zeilen,
       COUNT(DISTINCT DATE_FORMAT(MessageLastUpdate,'%Y-%m-%d %H'), ProcessID, MessageStatus) AS rollupzeilen
FROM GlassfishDB.Message WHERE MessageLastUpdate >= '2025-11-30 00:00:00' AND MessageLastUpdate < '2025-12-30 00:00:00'
UNION ALL SELECT 'OKT 2024-10', COUNT(*), COUNT(DISTINCT DATE_FORMAT(MessageLastUpdate,'%Y-%m-%d %H'), ProcessID, MessageStatus)
FROM GlassfishDB.Message WHERE MessageLastUpdate >= '2024-10-01 00:00:00' AND MessageLastUpdate < '2024-11-01 00:00:00'
UNION ALL SELECT 'MAX 2025-07', COUNT(*), COUNT(DISTINCT DATE_FORMAT(MessageLastUpdate,'%Y-%m-%d %H'), ProcessID, MessageStatus)
FROM GlassfishDB.Message WHERE MessageLastUpdate >= '2025-07-01 00:00:00' AND MessageLastUpdate < '2025-08-01 00:00:00'
UNION ALL SELECT 'LEER 2026-01', COUNT(*), COUNT(DISTINCT DATE_FORMAT(MessageLastUpdate,'%Y-%m-%d %H'), ProcessID, MessageStatus)
FROM GlassfishDB.Message WHERE MessageLastUpdate >= '2026-01-01 00:00:00' AND MessageLastUpdate < '2026-02-01 00:00:00';

SELECT '=== 98 SHOW PROFILES ===' AS marke;
SHOW PROFILES;
SELECT '=== 99 Abschluss ===' AS marke;
SELECT @@global.read_only AS read_only_ende, NOW() AS serverzeit_ende;
