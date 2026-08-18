-- M71 — deterministische Auswahl des EINEN Verweises
-- Auftrag: docs/messungen-schritt8-auftrag.md, Fassung 3, Ergaenzung 17.08.2026
--
-- Diese Datei enthaelt KEINE Kennung, KEINEN Wert und KEINE Adresse. Beides
-- wird in der Sitzung selbst hergeleitet — dieselbe Form wie die
-- Sitzungsskripte von Teil A. (Regel G1)
--
-- Gewaehlt ist der erste Verweis aus Fenster A, der FILESTOREPROD09 nennt,
-- sortiert nach der GUID: dieselbe Zeile, die schon die Stichprobe von
-- M59 (2) angefuehrt hat. Deterministisch und ohne Literal.
--
-- Ausschliesslich SELECT / SET. (Regel S1)

SELECT @@global.read_only AS global_read_only;
SET SESSION max_statement_time = 60;

SELECT mp.MessagePropertyValue  AS verweis,
       s.ServiceConnectString   AS adresse
FROM Message m
JOIN MessageProperty mp ON mp.MessageID = m.MessageID
JOIN Service s
  ON s.ServiceID = SUBSTRING_INDEX(mp.MessagePropertyValue, '|', 1)
WHERE m.MessageLastUpdate >= '2025-12-29 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  AND SUBSTRING_INDEX(mp.MessagePropertyValue, '|', 1) = 'FILESTOREPROD09'
  AND mp.MessagePropertyName = 'FileReader.Payload.GUID'
ORDER BY SUBSTRING_INDEX(mp.MessagePropertyValue, '|', -1)
LIMIT 1;
