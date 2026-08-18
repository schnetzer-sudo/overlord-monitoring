-- M64 - Markenbilanz bei Fehlernachrichten
--
-- Fortsetzung von M64a: dort sind in Fenster B elf Fehlernachrichten gefunden
-- worden (COMMIT_REJECTED 5, ERROR_TIMEOUT 3, ERROR_DUPLICATE 3), alle mit
-- Protokoll. Hier werden deren Protokolle geholt.
--
-- Fehlerpruefung ueber ERROR\_% ESCAPE plus COMMIT_REJECTED, wie in M64a und
-- wie es Regel 9 des Projekts verlangt - das naive LIKE 'ERROR_%' wuerde den
-- Unterstrich als Platzhalter lesen.
--
-- Gezogen werden AUSSCHLIESSLICH Verweise auf FILESTOREPROD09/10. Bleiben
-- weniger als drei uebrig, ist die Messung nicht aussagekraeftig - und DAS ist
-- der Befund, kein Grund zum Auffuellen anderswo.
--
-- Diese Datei enthaelt KEINE Kennung, KEINEN Verweis, KEINE Adresse. (Regel G1)
-- Ausschliesslich SELECT / SET. (Regel S1)

SELECT @@global.read_only AS global_read_only;
SET SESSION max_statement_time = 60;

WITH v AS (
  SELECT mp.MessagePropertyValue AS verweis,
         SUBSTRING_INDEX(mp.MessagePropertyValue, '|', 1) AS ablage
  FROM Message m
  JOIN MessageProperty mp ON mp.MessageID = m.MessageID
  WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
    AND (m.MessageStatus LIKE 'ERROR\_%' ESCAPE '\\'
      OR m.MessageStatus = 'COMMIT_REJECTED')
    AND mp.MessagePropertyName LIKE '%.Log.GUID'
)
SELECT 'UEBERGANGEN' AS hinweis,
       SUM(ablage NOT IN ('FILESTOREPROD09','FILESTOREPROD10')) AS uebergangen,
       COUNT(*) AS verweise_gesamt
FROM v;

SELECT CONCAT('M64-', m.MessageStatus)                     AS label,
       SUBSTRING_INDEX(mp.MessagePropertyValue, '|', 1)    AS ablage,
       mp.MessagePropertyName                              AS familie,
       mp.MessagePropertyValue                             AS verweis,
       s.ServiceConnectString                              AS adresse,
       ''                                                  AS zusatz
FROM Message m
JOIN MessageProperty mp ON mp.MessageID = m.MessageID
JOIN Service s ON s.ServiceID = SUBSTRING_INDEX(mp.MessagePropertyValue, '|', 1)
WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  AND (m.MessageStatus LIKE 'ERROR\_%' ESCAPE '\\'
    OR m.MessageStatus = 'COMMIT_REJECTED')
  AND mp.MessagePropertyName LIKE '%.Log.GUID'
  AND SUBSTRING_INDEX(mp.MessagePropertyValue, '|', 1)
        IN ('FILESTOREPROD09','FILESTOREPROD10')
ORDER BY m.MessageStatus, mp.MessagePropertyName;
