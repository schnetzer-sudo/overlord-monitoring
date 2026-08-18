-- Messrunde vor Schritt 8 — Teil B, Beschaffung der Stichprobe
-- Auftrag: docs/messungen-schritt8-auftrag.md, Fassung 3 vom 17.08.2026
--
-- Diese Datei erzeugt KEINE Messung. Sie beschafft die Adressen und die
-- Stichprobenliste fuer Teil B. Ihre Ausgabe ist Stufe-1-Material und geht
-- ausschliesslich in das Arbeitsverzeichnis, niemals in die Ergebnisdatei.
--
-- Die Stichprobenliste traegt Ablagenkennung und UUID GETRENNT (Auftrag §6):
-- Ohne die Kennung laesst sich die Holregel "immer bei der Ablage, die der
-- Verweis nennt" nicht einhalten.
-- Ausschliesslich SELECT / SET. (Regel S1)

SELECT @@global.read_only AS global_read_only;
SET SESSION max_statement_time = 60;

-- (1) Die beiden Adressen
SELECT ServiceID, ServiceConnectString
FROM Service
WHERE ServiceID IN ('FILESTOREPROD09', 'FILESTOREPROD10')
ORDER BY ServiceID;

-- (2) Stichprobe fuer M59/M68: je Ablage die zehn Verweise mit der kleinsten
--     UUID aus Fenster A, getrennt nach Kennung und UUID. Deterministisch.
SELECT SUBSTRING_INDEX(mp.MessagePropertyValue, '|', 1)  AS ablage,
       SUBSTRING_INDEX(mp.MessagePropertyValue, '|', -1) AS uuid,
       mp.MessagePropertyName                            AS familie
FROM Message m
JOIN MessageProperty mp ON mp.MessageID = m.MessageID
WHERE m.MessageLastUpdate >= '2025-12-29 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  AND SUBSTRING_INDEX(mp.MessagePropertyValue, '|', 1) = 'FILESTOREPROD09'
  AND (mp.MessagePropertyName LIKE '%.Payload.GUID'
    OR mp.MessagePropertyName LIKE '%.Log.GUID')
ORDER BY uuid
LIMIT 10;

SELECT SUBSTRING_INDEX(mp.MessagePropertyValue, '|', 1)  AS ablage,
       SUBSTRING_INDEX(mp.MessagePropertyValue, '|', -1) AS uuid,
       mp.MessagePropertyName                            AS familie
FROM Message m
JOIN MessageProperty mp ON mp.MessageID = m.MessageID
WHERE m.MessageLastUpdate >= '2025-12-29 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  AND SUBSTRING_INDEX(mp.MessagePropertyValue, '|', 1) = 'FILESTOREPROD10'
  AND (mp.MessagePropertyName LIKE '%.Payload.GUID'
    OR mp.MessagePropertyName LIKE '%.Log.GUID')
ORDER BY uuid
LIMIT 10;
