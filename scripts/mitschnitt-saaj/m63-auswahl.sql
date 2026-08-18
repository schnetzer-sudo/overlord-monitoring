-- M63, M65 und M67 - gemeinsame Stichprobe ueber die Protokollfamilien
--
-- Auftrag M63: mindestens 30 Protokolle je in M54 gefundener *.Log.GUID-Familie,
-- sofern vorhanden. M65 und M67 nutzen dieselbe Stichprobe.
--
-- Fenster B (2025-11-30 bis 2025-12-30), weil Fenster A vier Familien mit
-- weniger als 30 Zeilen traegt (HTTPReader 1, SSHReader 5, OFTP2Sender 6,
-- OFTPSender 20). In Fenster B erreichen 15 der 16 Familien die 30; DBReader
-- hat dort 4 - das ist der im Auftrag mitgedachte Fall "sofern vorhanden".
--
-- Gezogen werden AUSSCHLIESSLICH Verweise auf FILESTOREPROD09/10.
--
-- Diese Datei enthaelt KEINE Kennung, KEINEN Verweis, KEINE Adresse. (Regel G1)
-- Ausschliesslich SELECT / SET. (Regel S1)

SELECT @@global.read_only AS global_read_only;
SET SESSION max_statement_time = 60;

WITH v AS (
  SELECT mp.MessagePropertyName AS familie,
         mp.MessagePropertyValue AS verweis,
         SUBSTRING_INDEX(mp.MessagePropertyValue, '|', 1) AS ablage
  FROM Message m
  JOIN MessageProperty mp ON mp.MessageID = m.MessageID
  WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
    AND mp.MessagePropertyName LIKE '%.Log.GUID'
)
SELECT 'UEBERGANGEN' AS hinweis,
       SUM(ablage NOT IN ('FILESTOREPROD09','FILESTOREPROD10')) AS uebergangen,
       COUNT(*) AS verweise_gesamt
FROM v;

WITH v AS (
  SELECT mp.MessagePropertyName AS familie,
         mp.MessagePropertyValue AS verweis,
         SUBSTRING_INDEX(mp.MessagePropertyValue, '|', 1) AS ablage,
         ROW_NUMBER() OVER (PARTITION BY mp.MessagePropertyName
                            ORDER BY SUBSTRING_INDEX(mp.MessagePropertyValue, '|', -1)) AS rn
  FROM Message m
  JOIN MessageProperty mp ON mp.MessageID = m.MessageID
  WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
    AND mp.MessagePropertyName LIKE '%.Log.GUID'
    AND SUBSTRING_INDEX(mp.MessagePropertyValue, '|', 1)
          IN ('FILESTOREPROD09','FILESTOREPROD10')
)
SELECT 'M63' AS label, v.ablage, v.familie, v.verweis,
       s.ServiceConnectString AS adresse, '' AS zusatz
FROM v
JOIN Service s ON s.ServiceID = v.ablage
WHERE v.rn <= 30
ORDER BY v.familie, v.rn;
