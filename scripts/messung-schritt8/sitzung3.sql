-- Messrunde vor Schritt 8 — Sitzung 3
-- Auftrag: docs/messungen-schritt8-auftrag.md, Fassung 3 vom 17.08.2026
-- Inhalt: M53 Fenster B (der teure Lauf, eigene Sitzung nach Sitzungsplan)
-- Fenster B: 2025-11-30 00:00:00 .. 2025-12-30 00:00:00
--            woertlich aus messungen-schritt5.md Zeile 73
-- Ausschliesslich SELECT / SET / SHOW. (Regel S1)

SELECT @@global.read_only AS global_read_only;
SELECT NOW() AS serverzeit;
SET SESSION max_statement_time = 60;
SET profiling = 1;
SET profiling_history_size = 100;

-- Aufwaermlauf (kostenrelevante Messung, Auftrag: beste von den Wiederholungen)
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
WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  AND (mp.MessagePropertyName LIKE '%.Payload.GUID'
    OR mp.MessagePropertyName LIKE '%.Log.GUID')
GROUP BY pm.MandantID, filestore_kennung
ORDER BY pm.MandantID, verweise DESC;

-- Messlauf 1
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
WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  AND (mp.MessagePropertyName LIKE '%.Payload.GUID'
    OR mp.MessagePropertyName LIKE '%.Log.GUID')
GROUP BY pm.MandantID, filestore_kennung
ORDER BY pm.MandantID, verweise DESC;

-- Messlauf 2
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
WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  AND (mp.MessagePropertyName LIKE '%.Payload.GUID'
    OR mp.MessagePropertyName LIKE '%.Log.GUID')
GROUP BY pm.MandantID, filestore_kennung
ORDER BY pm.MandantID, verweise DESC;

SHOW PROFILES;
