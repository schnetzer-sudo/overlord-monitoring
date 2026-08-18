-- Die Rotationsgrenze - ab wann nennen die Verweise 09/10 statt 07/08?
--
-- Nachtrag zu M53 und M66. Keine eigene M-Nummer: Die Zahl faellt aus den
-- Stichproben dieses Laufs ab und beantwortet keine eigene Frage, sondern
-- praezisiert eine bereits gemessene (M53 Befund 1, M66 Befund 4).
--
-- Verfahren: je Monat die ersten 200 Nachrichten, dazu die Ablage ihrer
-- Message.Payload.GUID. Billig, weil die Sortierung der Indexordnung folgt und
-- je Monat nur 200 Zeilen materialisiert werden.
--
-- MariaDB 10.6 kennt kein LATERAL; deshalb je Monat ein eigener Block.
--
-- Diese Datei enthaelt KEINE Kennung und KEINEN Verweis. (Regel G1)
-- Ausschliesslich SELECT / SET. (Regel S1)

SELECT @@global.read_only AS global_read_only;
SET SESSION max_statement_time = 60;

SELECT '2025-05' AS monat, SUBSTRING_INDEX(mp.MessagePropertyValue,'|',1) AS ablage, COUNT(*) AS n
FROM (SELECT MessageID FROM Message WHERE MessageLastUpdate >= '2025-05-01' AND MessageLastUpdate < '2025-06-01' ORDER BY MessageLastUpdate, MessageID LIMIT 200) s
JOIN MessageProperty mp ON mp.MessageID = s.MessageID AND mp.MessagePropertyName = 'Message.Payload.GUID' GROUP BY 1,2
UNION ALL
SELECT '2025-06', SUBSTRING_INDEX(mp.MessagePropertyValue,'|',1), COUNT(*)
FROM (SELECT MessageID FROM Message WHERE MessageLastUpdate >= '2025-06-01' AND MessageLastUpdate < '2025-07-01' ORDER BY MessageLastUpdate, MessageID LIMIT 200) s
JOIN MessageProperty mp ON mp.MessageID = s.MessageID AND mp.MessagePropertyName = 'Message.Payload.GUID' GROUP BY 1,2
UNION ALL
SELECT '2025-07', SUBSTRING_INDEX(mp.MessagePropertyValue,'|',1), COUNT(*)
FROM (SELECT MessageID FROM Message WHERE MessageLastUpdate >= '2025-07-01' AND MessageLastUpdate < '2025-08-01' ORDER BY MessageLastUpdate, MessageID LIMIT 200) s
JOIN MessageProperty mp ON mp.MessageID = s.MessageID AND mp.MessagePropertyName = 'Message.Payload.GUID' GROUP BY 1,2
UNION ALL
SELECT '2025-08', SUBSTRING_INDEX(mp.MessagePropertyValue,'|',1), COUNT(*)
FROM (SELECT MessageID FROM Message WHERE MessageLastUpdate >= '2025-08-01' AND MessageLastUpdate < '2025-09-01' ORDER BY MessageLastUpdate, MessageID LIMIT 200) s
JOIN MessageProperty mp ON mp.MessageID = s.MessageID AND mp.MessagePropertyName = 'Message.Payload.GUID' GROUP BY 1,2
UNION ALL
SELECT '2025-09', SUBSTRING_INDEX(mp.MessagePropertyValue,'|',1), COUNT(*)
FROM (SELECT MessageID FROM Message WHERE MessageLastUpdate >= '2025-09-01' AND MessageLastUpdate < '2025-10-01' ORDER BY MessageLastUpdate, MessageID LIMIT 200) s
JOIN MessageProperty mp ON mp.MessageID = s.MessageID AND mp.MessagePropertyName = 'Message.Payload.GUID' GROUP BY 1,2
UNION ALL
SELECT '2025-10', SUBSTRING_INDEX(mp.MessagePropertyValue,'|',1), COUNT(*)
FROM (SELECT MessageID FROM Message WHERE MessageLastUpdate >= '2025-10-01' AND MessageLastUpdate < '2025-11-01' ORDER BY MessageLastUpdate, MessageID LIMIT 200) s
JOIN MessageProperty mp ON mp.MessageID = s.MessageID AND mp.MessagePropertyName = 'Message.Payload.GUID' GROUP BY 1,2
UNION ALL
SELECT '2025-11', SUBSTRING_INDEX(mp.MessagePropertyValue,'|',1), COUNT(*)
FROM (SELECT MessageID FROM Message WHERE MessageLastUpdate >= '2025-11-01' AND MessageLastUpdate < '2025-12-01' ORDER BY MessageLastUpdate, MessageID LIMIT 200) s
JOIN MessageProperty mp ON mp.MessageID = s.MessageID AND mp.MessagePropertyName = 'Message.Payload.GUID' GROUP BY 1,2
ORDER BY 1, 2;
