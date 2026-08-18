-- Messrunde vor Schritt 8 — Sitzung 4
-- Auftrag: docs/messungen-schritt8-auftrag.md, Fassung 3 vom 17.08.2026
-- Inhalt: M54 (a) und (b), Fenster A und B
-- Fenster A: 2025-12-29 00:00:00 .. 2025-12-30 00:00:00
-- Fenster B: 2025-11-30 00:00:00 .. 2025-12-30 00:00:00
-- Ausschliesslich SELECT / SET / SHOW. (Regel S1)

SELECT @@global.read_only AS global_read_only;
SELECT NOW() AS serverzeit;
SET SESSION max_statement_time = 60;
SET profiling = 1;
SET profiling_history_size = 100;

-- ─── M54 (a) — Namensmuster, Fenster A ──────────────────────────────────────
SELECT mp.MessagePropertyName,
       COUNT(*)                                     AS zeilen,
       COUNT(DISTINCT mp.MessageID)                 AS nachrichten,
       COUNT(DISTINCT mp.MessagePropertyValue)      AS verschiedene,
       MIN(CHAR_LENGTH(mp.MessagePropertyValue))    AS kuerzester,
       MAX(CHAR_LENGTH(mp.MessagePropertyValue))    AS laengster,
       SUM(mp.MessagePropertyValue LIKE '%|%')      AS mit_pipe
FROM Message m
JOIN MessageProperty mp ON mp.MessageID = m.MessageID
WHERE m.MessageLastUpdate >= '2025-12-29 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  AND (mp.MessagePropertyName LIKE '%.Payload.GUID'
    OR mp.MessagePropertyName LIKE '%.Log.GUID')
GROUP BY mp.MessagePropertyName
ORDER BY zeilen DESC;

-- ─── M54 (b) — Gestalt, Fenster A ───────────────────────────────────────────
SELECT mp.MessagePropertyName,
       COUNT(*) AS zeilen, COUNT(DISTINCT mp.MessageID) AS nachrichten,
       MIN(CHAR_LENGTH(mp.MessagePropertyValue)) AS kuerzester,
       MAX(CHAR_LENGTH(mp.MessagePropertyValue)) AS laengster
FROM Message m
JOIN MessageProperty mp ON mp.MessageID = m.MessageID
WHERE m.MessageLastUpdate >= '2025-12-29 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  AND mp.MessagePropertyValue REGEXP
      '^[^|]+\\|[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
  AND mp.MessagePropertyName NOT LIKE '%.Payload.GUID'
  AND mp.MessagePropertyName NOT LIKE '%.Log.GUID'
GROUP BY mp.MessagePropertyName
ORDER BY zeilen DESC;

-- ─── M54 (a) — Fenster B, Aufwaermlauf ──────────────────────────────────────
SELECT mp.MessagePropertyName,
       COUNT(*)                                     AS zeilen,
       COUNT(DISTINCT mp.MessageID)                 AS nachrichten,
       COUNT(DISTINCT mp.MessagePropertyValue)      AS verschiedene,
       MIN(CHAR_LENGTH(mp.MessagePropertyValue))    AS kuerzester,
       MAX(CHAR_LENGTH(mp.MessagePropertyValue))    AS laengster,
       SUM(mp.MessagePropertyValue LIKE '%|%')      AS mit_pipe
FROM Message m
JOIN MessageProperty mp ON mp.MessageID = m.MessageID
WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  AND (mp.MessagePropertyName LIKE '%.Payload.GUID'
    OR mp.MessagePropertyName LIKE '%.Log.GUID')
GROUP BY mp.MessagePropertyName
ORDER BY zeilen DESC;

-- ─── M54 (a) — Fenster B, Messlauf 1 ────────────────────────────────────────
SELECT mp.MessagePropertyName,
       COUNT(*)                                     AS zeilen,
       COUNT(DISTINCT mp.MessageID)                 AS nachrichten,
       COUNT(DISTINCT mp.MessagePropertyValue)      AS verschiedene,
       MIN(CHAR_LENGTH(mp.MessagePropertyValue))    AS kuerzester,
       MAX(CHAR_LENGTH(mp.MessagePropertyValue))    AS laengster,
       SUM(mp.MessagePropertyValue LIKE '%|%')      AS mit_pipe
FROM Message m
JOIN MessageProperty mp ON mp.MessageID = m.MessageID
WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  AND (mp.MessagePropertyName LIKE '%.Payload.GUID'
    OR mp.MessagePropertyName LIKE '%.Log.GUID')
GROUP BY mp.MessagePropertyName
ORDER BY zeilen DESC;

-- ─── M54 (b) — Fenster B, Aufwaermlauf ──────────────────────────────────────
SELECT mp.MessagePropertyName,
       COUNT(*) AS zeilen, COUNT(DISTINCT mp.MessageID) AS nachrichten,
       MIN(CHAR_LENGTH(mp.MessagePropertyValue)) AS kuerzester,
       MAX(CHAR_LENGTH(mp.MessagePropertyValue)) AS laengster
FROM Message m
JOIN MessageProperty mp ON mp.MessageID = m.MessageID
WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  AND mp.MessagePropertyValue REGEXP
      '^[^|]+\\|[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
  AND mp.MessagePropertyName NOT LIKE '%.Payload.GUID'
  AND mp.MessagePropertyName NOT LIKE '%.Log.GUID'
GROUP BY mp.MessagePropertyName
ORDER BY zeilen DESC;

-- ─── M54 (b) — Fenster B, Messlauf 1 ────────────────────────────────────────
SELECT mp.MessagePropertyName,
       COUNT(*) AS zeilen, COUNT(DISTINCT mp.MessageID) AS nachrichten,
       MIN(CHAR_LENGTH(mp.MessagePropertyValue)) AS kuerzester,
       MAX(CHAR_LENGTH(mp.MessagePropertyValue)) AS laengster
FROM Message m
JOIN MessageProperty mp ON mp.MessageID = m.MessageID
WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  AND mp.MessagePropertyValue REGEXP
      '^[^|]+\\|[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
  AND mp.MessagePropertyName NOT LIKE '%.Payload.GUID'
  AND mp.MessagePropertyName NOT LIKE '%.Log.GUID'
GROUP BY mp.MessagePropertyName
ORDER BY zeilen DESC;

SHOW PROFILES;
