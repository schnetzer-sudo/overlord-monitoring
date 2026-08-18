-- Messrunde vor Schritt 8 — Sitzung 2
-- Auftrag: docs/messungen-schritt8-auftrag.md, Fassung 3 vom 17.08.2026
-- Inhalt: M53 Fenster A und Fenster C
-- Fenster A: 2025-12-29 00:00:00 .. 2025-12-30 00:00:00 (Auftrag §1)
-- Fenster C: 2024-10-01 00:00:00 .. 2024-10-02 00:00:00 (Auftrag §1)
-- Ausschliesslich SELECT / SET / SHOW. (Regel S1)

SELECT @@global.read_only AS global_read_only;
SELECT NOW() AS serverzeit;
SET SESSION max_statement_time = 60;
SET profiling = 1;
SET profiling_history_size = 100;

-- ─── M53 — Fenster A ────────────────────────────────────────────────────────
SELECT pm.MandantID,
       SUBSTRING_INDEX(mp.MessagePropertyValue, '|', 1) AS filestore_kennung,
       COUNT(*)                                AS verweise,
       COUNT(DISTINCT mp.MessageID)            AS nachrichten,
       COUNT(DISTINCT mp.MessagePropertyName)  AS namen,
       SUM(s.ServiceID IS NULL)                AS nicht_aufloesbar
FROM Message m
JOIN Process p          ON p.ProcessID = m.ProcessID
JOIN ProjectMandant pm  ON pm.ProjectID = p.ProjectID
JOIN MessageProperty mp ON mp.MessageID = m.MessageID
LEFT JOIN Service s
       ON s.ServiceID = SUBSTRING_INDEX(mp.MessagePropertyValue, '|', 1)
WHERE m.MessageLastUpdate >= '2025-12-29 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  AND (mp.MessagePropertyName LIKE '%.Payload.GUID'
    OR mp.MessagePropertyName LIKE '%.Log.GUID')
GROUP BY pm.MandantID, filestore_kennung
ORDER BY pm.MandantID, verweise DESC;

-- ─── M53 — Fenster C ────────────────────────────────────────────────────────
SELECT pm.MandantID,
       SUBSTRING_INDEX(mp.MessagePropertyValue, '|', 1) AS filestore_kennung,
       COUNT(*)                                AS verweise,
       COUNT(DISTINCT mp.MessageID)            AS nachrichten,
       COUNT(DISTINCT mp.MessagePropertyName)  AS namen,
       SUM(s.ServiceID IS NULL)                AS nicht_aufloesbar
FROM Message m
JOIN Process p          ON p.ProcessID = m.ProcessID
JOIN ProjectMandant pm  ON pm.ProjectID = p.ProjectID
JOIN MessageProperty mp ON mp.MessageID = m.MessageID
LEFT JOIN Service s
       ON s.ServiceID = SUBSTRING_INDEX(mp.MessagePropertyValue, '|', 1)
WHERE m.MessageLastUpdate >= '2024-10-01 00:00:00'
  AND m.MessageLastUpdate <  '2024-10-02 00:00:00'
  AND (mp.MessagePropertyName LIKE '%.Payload.GUID'
    OR mp.MessagePropertyName LIKE '%.Log.GUID')
GROUP BY pm.MandantID, filestore_kennung
ORDER BY pm.MandantID, verweise DESC;

SHOW PROFILES;
