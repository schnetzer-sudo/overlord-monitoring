-- M66 (2) - die nachzuholende juengste Zeitscheibe
--
-- Die Scheibe "letzte Woche vor 2026-07-08" aus M66 war leer: Der Bestand endet
-- am 2026-07-08 selbst (285 Nachrichten zwischen 17:16:26 und 17:21:10). Diese
-- Messung legt die Scheibe auf den TATSAECHLICH letzten Tag mit Daten.
--
-- Umfang 50 Nachrichten - der Auftragsumfang. Die Kuerzung aus Ergaenzung 5 gilt
-- nach Auskunft des Auftraggebers vom 17.08.2026 nicht mehr.
--
-- Gezogen werden AUSSCHLIESSLICH Verweise auf FILESTOREPROD09/10. Alle uebrigen
-- Ablagen sind abgeschaltet; ein Fehlversuch gegen sie waere Rauschen und saehe
-- in der Tabelle wie ein Befund aus. Die Zahl der uebergangenen Verweise wird
-- ausgewiesen.
--
-- Diese Datei enthaelt KEINE Kennung, KEINEN Verweis, KEINE Adresse. (Regel G1)
-- Ausschliesslich SELECT / SET. (Regel S1)

SELECT @@global.read_only AS global_read_only;
SET SESSION max_statement_time = 60;

-- (1) uebergangene Verweise
WITH auswahl AS (
  SELECT m.MessageID FROM Message m
  WHERE m.MessageLastUpdate >= '2026-07-08 00:00:00'
    AND m.MessageLastUpdate <  '2026-07-09 00:00:00'
  ORDER BY m.MessageLastUpdate, m.MessageID LIMIT 50
)
SELECT 'UEBERGANGEN' AS hinweis,
       SUM(SUBSTRING_INDEX(mp.MessagePropertyValue, '|', 1)
             NOT IN ('FILESTOREPROD09','FILESTOREPROD10')) AS uebergangen,
       COUNT(*) AS verweise_gesamt
FROM auswahl a
JOIN MessageProperty mp ON mp.MessageID = a.MessageID
WHERE mp.MessagePropertyName = 'Message.Payload.GUID'
   OR mp.MessagePropertyName LIKE '%.Log.GUID';

-- (2) die Stichprobe
WITH auswahl AS (
  SELECT m.MessageID FROM Message m
  WHERE m.MessageLastUpdate >= '2026-07-08 00:00:00'
    AND m.MessageLastUpdate <  '2026-07-09 00:00:00'
  ORDER BY m.MessageLastUpdate, m.MessageID LIMIT 50
)
SELECT '2026-07-08'                                       AS label,
       SUBSTRING_INDEX(mp.MessagePropertyValue, '|', 1)   AS ablage,
       mp.MessagePropertyName                             AS familie,
       mp.MessagePropertyValue                            AS verweis,
       s.ServiceConnectString                             AS adresse,
       ''                                                 AS zusatz
FROM auswahl a
JOIN MessageProperty mp ON mp.MessageID = a.MessageID
JOIN Service s ON s.ServiceID = SUBSTRING_INDEX(mp.MessagePropertyValue, '|', 1)
WHERE (mp.MessagePropertyName = 'Message.Payload.GUID'
    OR mp.MessagePropertyName LIKE '%.Log.GUID')
  AND SUBSTRING_INDEX(mp.MessagePropertyValue, '|', 1)
        IN ('FILESTOREPROD09','FILESTOREPROD10');
