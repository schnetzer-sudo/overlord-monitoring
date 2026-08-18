-- M66 - deterministische Auswahl der Stichprobe
-- Auftrag Fassung 3, M66; Ergaenzung 5 vom 17.08.2026 (25 statt 50 je Scheibe)
--
-- Fuenf Zeitscheiben wie im Auftrag. Je Scheibe die 25 AELTESTEN Nachrichten
-- (ORDER BY MessageLastUpdate, MessageID) - deterministisch und billig, weil
-- die Sortierung der Indexordnung folgt. Je Nachricht Message.Payload.GUID und
-- alle *.Log.GUID.
--
-- Leere Scheiben liefern schlicht keine Zeile. Das ist ein Befund, kein Fehler.
--
-- Diese Datei enthaelt KEINE Kennung, KEINEN Verweis und KEINE Adresse.
-- Alles wird in der Sitzung hergeleitet. (Regel G1)
--
-- Ausschliesslich SELECT / SET. (Regel S1)

SELECT @@global.read_only AS global_read_only;
SET SESSION max_statement_time = 60;

SELECT '2024-10' AS scheibe,
       SUBSTRING_INDEX(mp.MessagePropertyValue, '|', 1) AS ablage,
       mp.MessagePropertyName                           AS familie,
       mp.MessagePropertyValue                          AS verweis,
       s.ServiceConnectString                           AS adresse
FROM (SELECT m.MessageID FROM Message m
      WHERE m.MessageLastUpdate >= '2024-10-01 00:00:00' AND m.MessageLastUpdate < '2024-11-01 00:00:00'
      ORDER BY m.MessageLastUpdate, m.MessageID LIMIT 25) sub
JOIN MessageProperty mp ON mp.MessageID = sub.MessageID
JOIN Service s ON s.ServiceID = SUBSTRING_INDEX(mp.MessagePropertyValue, '|', 1)
WHERE mp.MessagePropertyName = 'Message.Payload.GUID'
   OR mp.MessagePropertyName LIKE '%.Log.GUID'

UNION ALL

SELECT '2025-04',
       SUBSTRING_INDEX(mp.MessagePropertyValue, '|', 1),
       mp.MessagePropertyName, mp.MessagePropertyValue, s.ServiceConnectString
FROM (SELECT m.MessageID FROM Message m
      WHERE m.MessageLastUpdate >= '2025-04-01 00:00:00' AND m.MessageLastUpdate < '2025-05-01 00:00:00'
      ORDER BY m.MessageLastUpdate, m.MessageID LIMIT 25) sub
JOIN MessageProperty mp ON mp.MessageID = sub.MessageID
JOIN Service s ON s.ServiceID = SUBSTRING_INDEX(mp.MessagePropertyValue, '|', 1)
WHERE mp.MessagePropertyName = 'Message.Payload.GUID'
   OR mp.MessagePropertyName LIKE '%.Log.GUID'

UNION ALL

SELECT '2025-12',
       SUBSTRING_INDEX(mp.MessagePropertyValue, '|', 1),
       mp.MessagePropertyName, mp.MessagePropertyValue, s.ServiceConnectString
FROM (SELECT m.MessageID FROM Message m
      WHERE m.MessageLastUpdate >= '2025-12-01 00:00:00' AND m.MessageLastUpdate < '2026-01-01 00:00:00'
      ORDER BY m.MessageLastUpdate, m.MessageID LIMIT 25) sub
JOIN MessageProperty mp ON mp.MessageID = sub.MessageID
JOIN Service s ON s.ServiceID = SUBSTRING_INDEX(mp.MessagePropertyValue, '|', 1)
WHERE mp.MessagePropertyName = 'Message.Payload.GUID'
   OR mp.MessagePropertyName LIKE '%.Log.GUID'

UNION ALL

SELECT '2026-05',
       SUBSTRING_INDEX(mp.MessagePropertyValue, '|', 1),
       mp.MessagePropertyName, mp.MessagePropertyValue, s.ServiceConnectString
FROM (SELECT m.MessageID FROM Message m
      WHERE m.MessageLastUpdate >= '2026-05-01 00:00:00' AND m.MessageLastUpdate < '2026-06-01 00:00:00'
      ORDER BY m.MessageLastUpdate, m.MessageID LIMIT 25) sub
JOIN MessageProperty mp ON mp.MessageID = sub.MessageID
JOIN Service s ON s.ServiceID = SUBSTRING_INDEX(mp.MessagePropertyValue, '|', 1)
WHERE mp.MessagePropertyName = 'Message.Payload.GUID'
   OR mp.MessagePropertyName LIKE '%.Log.GUID'

UNION ALL

SELECT '2026-07-01..08',
       SUBSTRING_INDEX(mp.MessagePropertyValue, '|', 1),
       mp.MessagePropertyName, mp.MessagePropertyValue, s.ServiceConnectString
FROM (SELECT m.MessageID FROM Message m
      WHERE m.MessageLastUpdate >= '2026-07-01 00:00:00' AND m.MessageLastUpdate < '2026-07-08 00:00:00'
      ORDER BY m.MessageLastUpdate, m.MessageID LIMIT 25) sub
JOIN MessageProperty mp ON mp.MessageID = sub.MessageID
JOIN Service s ON s.ServiceID = SUBSTRING_INDEX(mp.MessagePropertyValue, '|', 1)
WHERE mp.MessagePropertyName = 'Message.Payload.GUID'
   OR mp.MessagePropertyName LIKE '%.Log.GUID';
