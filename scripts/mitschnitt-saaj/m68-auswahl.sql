-- M68 - Sind die Ablagen austauschbar? (Kreuzabruf)
--
-- Auftrag M68: Zehn Verweise, die FILESTOREPROD09 nennen, werden ZUSAETZLICH
-- bei FILESTOREPROD10 abgerufen, und zehn Verweise auf FILESTOREPROD10
-- zusaetzlich bei FILESTOREPROD09.
--
-- DIES IST DIE EINZIGE VORGESEHENE AUSNAHME VON DER HOLREGEL. Sie ist im
-- Auftrag als eigene Messung benannt und kein Rueckfallmechanismus.
--
-- Zusaetzlich zur Gegenprobe dieselben zwanzig Verweise am EIGENEN Knoten
-- (zusatz = GERADE). Ohne sie ist ein Fehlschlag des Kreuzabrufs nicht deutbar:
-- Er koennte "nicht gespiegelt" heissen oder "Datei gibt es gar nicht".
--
-- Diese Datei enthaelt KEINE Kennung, KEINEN Verweis, KEINE Adresse. (Regel G1)
-- Ausschliesslich SELECT / SET. (Regel S1)

SELECT @@global.read_only AS global_read_only;
SET SESSION max_statement_time = 60;

WITH v AS (
  SELECT mp.MessagePropertyValue AS verweis,
         mp.MessagePropertyName  AS familie,
         SUBSTRING_INDEX(mp.MessagePropertyValue, '|', 1) AS ablage,
         ROW_NUMBER() OVER (PARTITION BY SUBSTRING_INDEX(mp.MessagePropertyValue, '|', 1)
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
-- (1) Kreuzabruf: Adresse der ANDEREN Ablage
SELECT 'M68-kreuz' AS label, v.ablage, v.familie, v.verweis,
       s.ServiceConnectString AS adresse, 'KREUZ' AS zusatz
FROM v
JOIN Service s
  ON s.ServiceID = CASE v.ablage WHEN 'FILESTOREPROD09' THEN 'FILESTOREPROD10'
                                 ELSE 'FILESTOREPROD09' END
WHERE v.rn <= 10

UNION ALL

-- (2) Gegenprobe: dieselben Verweise am eigenen Knoten
SELECT 'M68-gerade', v.ablage, v.familie, v.verweis,
       s.ServiceConnectString, 'GERADE'
FROM v
JOIN Service s ON s.ServiceID = v.ablage
WHERE v.rn <= 10
ORDER BY 6, 2;
