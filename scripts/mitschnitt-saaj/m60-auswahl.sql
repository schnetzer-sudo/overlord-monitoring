-- M60 und M61 - Groessenverteilung und Kodierung, gemeinsame Stichprobe
--
-- Auftrag M60: mindestens 200 Artefakte, gleichmaessig ueber die in M54
-- gefundenen Namen, beide Familien, mehrere Mandanten und Service.Type.
-- M61 nutzt dieselbe Stichprobe.
--
-- Fenster A (2025-12-29), weil es 32 der 34 Namen traegt und mit 44.329 Zeilen
-- billig ist; Fenster B kostet fuer dieselbe Aussage ein Vielfaches. Fenster A
-- liegt im erreichbaren Zeitraum (FILESTOREPROD09/10, siehe M53).
--
-- Je Name bis zu 7 Verweise -> rund 220 Artefakte.
--
-- zusatz traegt FileReader.FileProperty.Size, wo es die Nachricht hat. Damit
-- laesst sich pruefen, ob jene Spalte Bytes zaehlt (offener Punkt 6).
--
-- Gezogen werden AUSSCHLIESSLICH Verweise auf FILESTOREPROD09/10.
--
-- Diese Datei enthaelt KEINE Kennung, KEINEN Verweis, KEINE Adresse. (Regel G1)
-- Ausschliesslich SELECT / SET. (Regel S1)

SELECT @@global.read_only AS global_read_only;
SET SESSION max_statement_time = 60;

WITH v AS (
  SELECT mp.MessageID,
         mp.MessagePropertyName AS familie,
         mp.MessagePropertyValue AS verweis,
         SUBSTRING_INDEX(mp.MessagePropertyValue, '|', 1) AS ablage
  FROM Message m
  JOIN MessageProperty mp ON mp.MessageID = m.MessageID
  WHERE m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
    AND (mp.MessagePropertyName LIKE '%.Payload.GUID'
      OR mp.MessagePropertyName LIKE '%.Log.GUID')
)
SELECT 'UEBERGANGEN' AS hinweis,
       SUM(ablage NOT IN ('FILESTOREPROD09','FILESTOREPROD10')) AS uebergangen,
       COUNT(*) AS verweise_gesamt
FROM v;

WITH v AS (
  SELECT mp.MessageID,
         mp.MessagePropertyName AS familie,
         mp.MessagePropertyValue AS verweis,
         SUBSTRING_INDEX(mp.MessagePropertyValue, '|', 1) AS ablage,
         ROW_NUMBER() OVER (PARTITION BY mp.MessagePropertyName
                            ORDER BY SUBSTRING_INDEX(mp.MessagePropertyValue, '|', -1)) AS rn
  FROM Message m
  JOIN MessageProperty mp ON mp.MessageID = m.MessageID
  WHERE m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
    AND (mp.MessagePropertyName LIKE '%.Payload.GUID'
      OR mp.MessagePropertyName LIKE '%.Log.GUID')
    AND SUBSTRING_INDEX(mp.MessagePropertyValue, '|', 1)
          IN ('FILESTOREPROD09','FILESTOREPROD10')
)
SELECT 'M60' AS label, v.ablage, v.familie, v.verweis,
       s.ServiceConnectString AS adresse,
       COALESCE(gr.MessagePropertyValue, '') AS zusatz
FROM v
JOIN Service s ON s.ServiceID = v.ablage
LEFT JOIN MessageProperty gr
       ON gr.MessageID = v.MessageID
      AND gr.MessagePropertyName = 'FileReader.FileProperty.Size'
      AND v.familie = 'FileReader.Payload.GUID'
WHERE v.rn <= 7
ORDER BY v.familie, v.rn;
