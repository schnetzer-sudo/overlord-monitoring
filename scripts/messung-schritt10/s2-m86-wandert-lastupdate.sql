-- Messrunde M86-M92 — Sitzung 2: M86 "Wandert MessageLastUpdate?"
-- Ausschliesslich SELECT / SET / EXPLAIN. (Regel S1)
-- Z1: jeder Zeitpunkt steht als Literal im Statement. Kein NOW() in einer Messabfrage.
-- Fenster A (M17, woertlich): >= '2025-12-29 00:00:00' AND < '2025-12-30 00:00:00'  (n = 6.249)
-- Fenster B (M17, woertlich): >= '2025-11-30 00:00:00' AND < '2025-12-30 00:00:00'  (n = 214.330)
-- G1: keine ProcessID, keine MessageID, kein Partnername in dieser Datei.

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;

SET max_statement_time = 60;
SET profiling_history_size = 100;
SET profiling = 1;
SELECT '=== 01 Grenzen gesetzt ===' AS marke;
SELECT @@max_statement_time AS mst, @@profiling AS prof, @@profiling_history_size AS hist;

-- ===== M86 (a) — Die Spaltendefinition von Message, vollstaendig ============
SELECT '=== M86a Spalten von Message ===' AS marke;
SELECT ORDINAL_POSITION, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT,
       EXTRA, COLUMN_KEY, COLLATION_NAME
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'GlassfishDB' AND TABLE_NAME = 'Message'
ORDER BY ORDINAL_POSITION;

SELECT '=== M86a2 Nur Spalten mit EXTRA (Nachschrift-Automatik?) ===' AS marke;
SELECT COLUMN_NAME, COLUMN_TYPE, EXTRA
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'GlassfishDB' AND TABLE_NAME = 'Message'
  AND EXTRA <> ''
ORDER BY ORDINAL_POSITION;

SELECT '=== M86a3 Spalten von MessageAction (Hilfsgroesse, Lehre aus M14) ===' AS marke;
SELECT ORDINAL_POSITION, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT, EXTRA, COLUMN_KEY
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'GlassfishDB' AND TABLE_NAME = 'MessageAction'
ORDER BY ORDINAL_POSITION;

-- ===== Gegenprobe der Fenstergroessen gegen M17 =============================
SELECT '=== M86-0 Fenstergroessen gegen M17 ===' AS marke;
SELECT 'A' AS fenster, COUNT(*) AS nachrichten FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2025-12-29 00:00:00' AND MessageLastUpdate < '2025-12-30 00:00:00'
UNION ALL
SELECT 'B', COUNT(*) FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2025-11-30 00:00:00' AND MessageLastUpdate < '2025-12-30 00:00:00';

-- ===== M86 (b) — Der Wirkungsbeleg, Fenster B ==============================
SELECT '=== M86b EXPLAIN (Fenster B) ===' AS marke;
EXPLAIN
SELECT m.MessageStatus AS status,
       COUNT(*) AS nachrichten,
       SUM(a.letzte_aktion IS NOT NULL AND a.letzte_aktion > '1971-01-01 00:00:00') AS mit_aktionsende,
       SUM(a.letzte_aktion IS NOT NULL AND a.letzte_aktion > '1971-01-01 00:00:00'
           AND m.MessageLastUpdate > a.letzte_aktion) AS lastupdate_nach_aktion
FROM GlassfishDB.Message m
LEFT JOIN (
    SELECT ma.MessageID, MAX(ma.MessageActionEnd) AS letzte_aktion
    FROM GlassfishDB.MessageAction ma
    JOIN GlassfishDB.Message mw
      ON mw.MessageID = ma.MessageID
     AND mw.MessageLastUpdate >= '2025-11-30 00:00:00'
     AND mw.MessageLastUpdate <  '2025-12-30 00:00:00'
    GROUP BY ma.MessageID
) a ON a.MessageID = m.MessageID
WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
GROUP BY m.MessageStatus;

SELECT '=== M86b Anteile je Status (Fenster B) ===' AS marke;
SELECT m.MessageStatus AS status,
       COUNT(*) AS nachrichten,
       SUM(a.letzte_aktion IS NOT NULL AND a.letzte_aktion > '1971-01-01 00:00:00') AS mit_aktionsende,
       SUM(a.letzte_aktion IS NOT NULL AND a.letzte_aktion > '1971-01-01 00:00:00'
           AND m.MessageLastUpdate > a.letzte_aktion) AS lastupdate_nach_aktion,
       SUM(a.letzte_aktion IS NOT NULL AND a.letzte_aktion > '1971-01-01 00:00:00'
           AND m.MessageLastUpdate = a.letzte_aktion) AS lastupdate_gleich_aktion,
       SUM(a.letzte_aktion IS NOT NULL AND a.letzte_aktion > '1971-01-01 00:00:00'
           AND m.MessageLastUpdate < a.letzte_aktion) AS lastupdate_vor_aktion,
       ROUND(100.0 * SUM(a.letzte_aktion IS NOT NULL AND a.letzte_aktion > '1971-01-01 00:00:00'
           AND m.MessageLastUpdate > a.letzte_aktion) / COUNT(*), 4) AS prozent_nachschrift
FROM GlassfishDB.Message m
LEFT JOIN (
    SELECT ma.MessageID, MAX(ma.MessageActionEnd) AS letzte_aktion
    FROM GlassfishDB.MessageAction ma
    JOIN GlassfishDB.Message mw
      ON mw.MessageID = ma.MessageID
     AND mw.MessageLastUpdate >= '2025-11-30 00:00:00'
     AND mw.MessageLastUpdate <  '2025-12-30 00:00:00'
    GROUP BY ma.MessageID
) a ON a.MessageID = m.MessageID
WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
GROUP BY m.MessageStatus
ORDER BY nachrichten DESC;

SELECT '=== M86b Differenzverteilung in Sekunden, Eimer (Fenster B) ===' AS marke;
SELECT status,
       COUNT(*)                                   AS mit_differenz,
       MIN(d)                                     AS min_sek,
       MAX(d)                                     AS max_sek,
       ROUND(AVG(d), 3)                           AS mittel_sek,
       SUM(d <= 0)                                AS e_kleiner_gleich_0,
       SUM(d BETWEEN 1 AND 60)                    AS e_1_60,
       SUM(d BETWEEN 61 AND 3600)                 AS e_61_3600,
       SUM(d BETWEEN 3601 AND 86400)              AS e_1h_24h,
       SUM(d > 86400)                             AS e_ueber_24h
FROM (
  SELECT m.MessageStatus AS status,
         TIMESTAMPDIFF(SECOND, a.letzte_aktion, m.MessageLastUpdate) AS d
  FROM GlassfishDB.Message m
  JOIN (
      SELECT ma.MessageID, MAX(ma.MessageActionEnd) AS letzte_aktion
      FROM GlassfishDB.MessageAction ma
      JOIN GlassfishDB.Message mw
        ON mw.MessageID = ma.MessageID
       AND mw.MessageLastUpdate >= '2025-11-30 00:00:00'
       AND mw.MessageLastUpdate <  '2025-12-30 00:00:00'
      GROUP BY ma.MessageID
  ) a ON a.MessageID = m.MessageID
  WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
    AND a.letzte_aktion > '1971-01-01 00:00:00'
) t
GROUP BY status
ORDER BY mit_differenz DESC;

SELECT '=== M86b Perzentile der Differenz (Fenster B) ===' AS marke;
SELECT DISTINCT status,
       PERCENTILE_DISC(0.00) WITHIN GROUP (ORDER BY d) OVER (PARTITION BY status) AS p_min,
       PERCENTILE_DISC(0.50) WITHIN GROUP (ORDER BY d) OVER (PARTITION BY status) AS p50,
       PERCENTILE_DISC(0.90) WITHIN GROUP (ORDER BY d) OVER (PARTITION BY status) AS p90,
       PERCENTILE_DISC(0.99) WITHIN GROUP (ORDER BY d) OVER (PARTITION BY status) AS p99,
       PERCENTILE_DISC(1.00) WITHIN GROUP (ORDER BY d) OVER (PARTITION BY status) AS p_max
FROM (
  SELECT m.MessageStatus AS status,
         TIMESTAMPDIFF(SECOND, a.letzte_aktion, m.MessageLastUpdate) AS d
  FROM GlassfishDB.Message m
  JOIN (
      SELECT ma.MessageID, MAX(ma.MessageActionEnd) AS letzte_aktion
      FROM GlassfishDB.MessageAction ma
      JOIN GlassfishDB.Message mw
        ON mw.MessageID = ma.MessageID
       AND mw.MessageLastUpdate >= '2025-11-30 00:00:00'
       AND mw.MessageLastUpdate <  '2025-12-30 00:00:00'
      GROUP BY ma.MessageID
  ) a ON a.MessageID = m.MessageID
  WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
    AND a.letzte_aktion > '1971-01-01 00:00:00'
) t;

-- ===== M86 (c) — Die Gegenprobe, Fenster A =================================
SELECT '=== M86c EXPLAIN (Fenster A) ===' AS marke;
EXPLAIN
SELECT m.MessageStatus AS status, COUNT(*) AS nachrichten
FROM GlassfishDB.Message m
LEFT JOIN (
    SELECT ma.MessageID, MAX(ma.MessageActionEnd) AS letzte_aktion
    FROM GlassfishDB.MessageAction ma
    JOIN GlassfishDB.Message mw
      ON mw.MessageID = ma.MessageID
     AND mw.MessageLastUpdate >= '2025-12-29 00:00:00'
     AND mw.MessageLastUpdate <  '2025-12-30 00:00:00'
    GROUP BY ma.MessageID
) a ON a.MessageID = m.MessageID
WHERE m.MessageLastUpdate >= '2025-12-29 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
GROUP BY m.MessageStatus;

SELECT '=== M86c Anteile je Status (Fenster A) ===' AS marke;
SELECT m.MessageStatus AS status,
       COUNT(*) AS nachrichten,
       SUM(a.letzte_aktion IS NOT NULL AND a.letzte_aktion > '1971-01-01 00:00:00') AS mit_aktionsende,
       SUM(a.letzte_aktion IS NOT NULL AND a.letzte_aktion > '1971-01-01 00:00:00'
           AND m.MessageLastUpdate > a.letzte_aktion) AS lastupdate_nach_aktion,
       SUM(a.letzte_aktion IS NOT NULL AND a.letzte_aktion > '1971-01-01 00:00:00'
           AND m.MessageLastUpdate = a.letzte_aktion) AS lastupdate_gleich_aktion,
       SUM(a.letzte_aktion IS NOT NULL AND a.letzte_aktion > '1971-01-01 00:00:00'
           AND m.MessageLastUpdate < a.letzte_aktion) AS lastupdate_vor_aktion,
       ROUND(100.0 * SUM(a.letzte_aktion IS NOT NULL AND a.letzte_aktion > '1971-01-01 00:00:00'
           AND m.MessageLastUpdate > a.letzte_aktion) / COUNT(*), 4) AS prozent_nachschrift
FROM GlassfishDB.Message m
LEFT JOIN (
    SELECT ma.MessageID, MAX(ma.MessageActionEnd) AS letzte_aktion
    FROM GlassfishDB.MessageAction ma
    JOIN GlassfishDB.Message mw
      ON mw.MessageID = ma.MessageID
     AND mw.MessageLastUpdate >= '2025-12-29 00:00:00'
     AND mw.MessageLastUpdate <  '2025-12-30 00:00:00'
    GROUP BY ma.MessageID
) a ON a.MessageID = m.MessageID
WHERE m.MessageLastUpdate >= '2025-12-29 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
GROUP BY m.MessageStatus
ORDER BY nachrichten DESC;

SELECT '=== M86c Differenzverteilung in Sekunden, Eimer (Fenster A) ===' AS marke;
SELECT status,
       COUNT(*)                      AS mit_differenz,
       MIN(d)                        AS min_sek,
       MAX(d)                        AS max_sek,
       ROUND(AVG(d), 3)              AS mittel_sek,
       SUM(d <= 0)                   AS e_kleiner_gleich_0,
       SUM(d BETWEEN 1 AND 60)       AS e_1_60,
       SUM(d BETWEEN 61 AND 3600)    AS e_61_3600,
       SUM(d BETWEEN 3601 AND 86400) AS e_1h_24h,
       SUM(d > 86400)                AS e_ueber_24h
FROM (
  SELECT m.MessageStatus AS status,
         TIMESTAMPDIFF(SECOND, a.letzte_aktion, m.MessageLastUpdate) AS d
  FROM GlassfishDB.Message m
  JOIN (
      SELECT ma.MessageID, MAX(ma.MessageActionEnd) AS letzte_aktion
      FROM GlassfishDB.MessageAction ma
      JOIN GlassfishDB.Message mw
        ON mw.MessageID = ma.MessageID
       AND mw.MessageLastUpdate >= '2025-12-29 00:00:00'
       AND mw.MessageLastUpdate <  '2025-12-30 00:00:00'
      GROUP BY ma.MessageID
  ) a ON a.MessageID = m.MessageID
  WHERE m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
    AND a.letzte_aktion > '1971-01-01 00:00:00'
) t
GROUP BY status
ORDER BY mit_differenz DESC;

SELECT '=== M86c Perzentile der Differenz (Fenster A) ===' AS marke;
SELECT DISTINCT status,
       PERCENTILE_DISC(0.00) WITHIN GROUP (ORDER BY d) OVER (PARTITION BY status) AS p_min,
       PERCENTILE_DISC(0.50) WITHIN GROUP (ORDER BY d) OVER (PARTITION BY status) AS p50,
       PERCENTILE_DISC(0.90) WITHIN GROUP (ORDER BY d) OVER (PARTITION BY status) AS p90,
       PERCENTILE_DISC(0.99) WITHIN GROUP (ORDER BY d) OVER (PARTITION BY status) AS p99,
       PERCENTILE_DISC(1.00) WITHIN GROUP (ORDER BY d) OVER (PARTITION BY status) AS p_max
FROM (
  SELECT m.MessageStatus AS status,
         TIMESTAMPDIFF(SECOND, a.letzte_aktion, m.MessageLastUpdate) AS d
  FROM GlassfishDB.Message m
  JOIN (
      SELECT ma.MessageID, MAX(ma.MessageActionEnd) AS letzte_aktion
      FROM GlassfishDB.MessageAction ma
      JOIN GlassfishDB.Message mw
        ON mw.MessageID = ma.MessageID
       AND mw.MessageLastUpdate >= '2025-12-29 00:00:00'
       AND mw.MessageLastUpdate <  '2025-12-30 00:00:00'
      GROUP BY ma.MessageID
  ) a ON a.MessageID = m.MessageID
  WHERE m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
    AND a.letzte_aktion > '1971-01-01 00:00:00'
) t;

SELECT '=== 98 SHOW PROFILES ===' AS marke;
SHOW PROFILES;

SELECT '=== 99 Abschluss ===' AS marke;
SELECT @@global.read_only AS read_only_ende, NOW() AS serverzeit_ende;
