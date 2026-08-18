-- Messrunde vor Schritt 8 — Sitzung 5
-- Auftrag: docs/messungen-schritt8-auftrag.md, Fassung 3 vom 17.08.2026
-- Inhalt: M55 (a)(b) und M56 (a)(b)(c), Fenster A und B
-- Ausschliesslich SELECT / SET / SHOW. (Regel S1)
--
-- ABWEICHUNG bei M56 (c), begruendet in der Ergebnisdatei:
-- Der beauftragte Filter ^[a-z0-9]{1,5}$ laesst reine Ziffernfolgen durch.
-- Genau davor warnt die Begruendung des Filters im Auftrag selbst ("darin kann
-- eine Belegnummer stecken"). Die gefahrene Fassung verlangt zusaetzlich einen
-- Buchstaben an erster Stelle und zaehlt alles Uebrige als <ziffernfolge>.

SELECT @@global.read_only AS global_read_only;
SELECT NOW() AS serverzeit;
SET SESSION max_statement_time = 60;
SET profiling = 1;
SET profiling_history_size = 100;

-- ═══ FENSTER A ══════════════════════════════════════════════════════════════

-- ─── M55 (a) — Verteilung, Fenster A ────────────────────────────────────────
SELECT verweise, COUNT(*) AS nachrichten FROM (
  SELECT m.MessageID, COUNT(mp.MessagePropertyName) AS verweise
  FROM Message m
  LEFT JOIN MessageProperty mp ON mp.MessageID = m.MessageID
       AND (mp.MessagePropertyName LIKE '%.Payload.GUID'
         OR mp.MessagePropertyName LIKE '%.Log.GUID')
  WHERE m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY m.MessageID
) x GROUP BY verweise ORDER BY verweise;

-- ─── M55 (b) — je Mandant, Fenster A ────────────────────────────────────────
SELECT pm.MandantID,
       COUNT(DISTINCT m.MessageID)                          AS nachrichten,
       SUM(mp.MessagePropertyName LIKE '%.Payload.GUID')    AS nutzdaten_verweise,
       SUM(mp.MessagePropertyName LIKE '%.Log.GUID')        AS protokoll_verweise,
       COUNT(DISTINCT CASE WHEN mp.MessagePropertyName LIKE '%.Log.GUID'
                           THEN mp.MessageID END)           AS nachrichten_mit_protokoll
FROM Message m
JOIN Process p         ON p.ProcessID = m.ProcessID
JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN MessageProperty mp ON mp.MessageID = m.MessageID
     AND (mp.MessagePropertyName LIKE '%.Payload.GUID'
       OR mp.MessagePropertyName LIKE '%.Log.GUID')
WHERE m.MessageLastUpdate >= '2025-12-29 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
GROUP BY pm.MandantID
ORDER BY nachrichten DESC;

-- ─── M56 (a) — welche Namen es gibt, Fenster A ──────────────────────────────
SELECT mp.MessagePropertyName,
       COUNT(*) AS zeilen, COUNT(DISTINCT mp.MessageID) AS nachrichten,
       MIN(CHAR_LENGTH(mp.MessagePropertyValue)) AS kuerzester,
       MAX(CHAR_LENGTH(mp.MessagePropertyValue)) AS laengster,
       SUM(mp.MessagePropertyValue REGEXP '^[0-9]+$') AS nur_ziffern
FROM Message m
JOIN MessageProperty mp ON mp.MessageID = m.MessageID
WHERE m.MessageLastUpdate >= '2025-12-29 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  AND (mp.MessagePropertyName LIKE '%Size%'
    OR mp.MessagePropertyName LIKE '%Filename%'
    OR mp.MessagePropertyName LIKE '%FileProperty%')
GROUP BY mp.MessagePropertyName ORDER BY zeilen DESC;

-- ─── M56 (b) — Groessenordnung, Fenster A ───────────────────────────────────
SELECT COUNT(*) AS zeilen,
       MIN(CAST(mp.MessagePropertyValue AS UNSIGNED))  AS kleinster,
       AVG(CAST(mp.MessagePropertyValue AS UNSIGNED))  AS mittel,
       MAX(CAST(mp.MessagePropertyValue AS UNSIGNED))  AS groesster,
       SUM(CAST(mp.MessagePropertyValue AS UNSIGNED) >    65536) AS ueber_64_kib,
       SUM(CAST(mp.MessagePropertyValue AS UNSIGNED) >  1048576) AS ueber_1_mib,
       SUM(CAST(mp.MessagePropertyValue AS UNSIGNED) > 10485760) AS ueber_10_mib
FROM Message m
JOIN MessageProperty mp ON mp.MessageID = m.MessageID
WHERE m.MessageLastUpdate >= '2025-12-29 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  AND mp.MessagePropertyName = 'FileReader.FileProperty.Size';

-- ─── M56 (c) — Endungen, Fenster A (verschaerfte Klassifikation) ────────────
SELECT CASE WHEN endung REGEXP '^[a-z][a-z0-9]{0,4}$' THEN endung
            WHEN endung REGEXP '^[0-9]+$'             THEN '<ziffernfolge>'
            ELSE '<sonstige>' END AS endung,
       COUNT(*) AS zeilen
FROM (
  SELECT LOWER(SUBSTRING_INDEX(mp.MessagePropertyValue, '.', -1)) AS endung
  FROM Message m
  JOIN MessageProperty mp ON mp.MessageID = m.MessageID
  WHERE m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
    AND mp.MessagePropertyName = 'FileReader.FileProperty.OriginalFilename'
    AND mp.MessagePropertyValue LIKE '%.%'
) x GROUP BY 1 ORDER BY zeilen DESC;

-- ═══ FENSTER B ══════════════════════════════════════════════════════════════

-- ─── M55 (a) — Verteilung, Fenster B ────────────────────────────────────────
SELECT verweise, COUNT(*) AS nachrichten FROM (
  SELECT m.MessageID, COUNT(mp.MessagePropertyName) AS verweise
  FROM Message m
  LEFT JOIN MessageProperty mp ON mp.MessageID = m.MessageID
       AND (mp.MessagePropertyName LIKE '%.Payload.GUID'
         OR mp.MessagePropertyName LIKE '%.Log.GUID')
  WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY m.MessageID
) x GROUP BY verweise ORDER BY verweise;

-- ─── M55 (b) — je Mandant, Fenster B ────────────────────────────────────────
SELECT pm.MandantID,
       COUNT(DISTINCT m.MessageID)                          AS nachrichten,
       SUM(mp.MessagePropertyName LIKE '%.Payload.GUID')    AS nutzdaten_verweise,
       SUM(mp.MessagePropertyName LIKE '%.Log.GUID')        AS protokoll_verweise,
       COUNT(DISTINCT CASE WHEN mp.MessagePropertyName LIKE '%.Log.GUID'
                           THEN mp.MessageID END)           AS nachrichten_mit_protokoll
FROM Message m
JOIN Process p         ON p.ProcessID = m.ProcessID
JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN MessageProperty mp ON mp.MessageID = m.MessageID
     AND (mp.MessagePropertyName LIKE '%.Payload.GUID'
       OR mp.MessagePropertyName LIKE '%.Log.GUID')
WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
GROUP BY pm.MandantID
ORDER BY nachrichten DESC;

-- ─── M56 (a) — welche Namen es gibt, Fenster B ──────────────────────────────
SELECT mp.MessagePropertyName,
       COUNT(*) AS zeilen, COUNT(DISTINCT mp.MessageID) AS nachrichten,
       MIN(CHAR_LENGTH(mp.MessagePropertyValue)) AS kuerzester,
       MAX(CHAR_LENGTH(mp.MessagePropertyValue)) AS laengster,
       SUM(mp.MessagePropertyValue REGEXP '^[0-9]+$') AS nur_ziffern
FROM Message m
JOIN MessageProperty mp ON mp.MessageID = m.MessageID
WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  AND (mp.MessagePropertyName LIKE '%Size%'
    OR mp.MessagePropertyName LIKE '%Filename%'
    OR mp.MessagePropertyName LIKE '%FileProperty%')
GROUP BY mp.MessagePropertyName ORDER BY zeilen DESC;

-- ─── M56 (b) — Groessenordnung, Fenster B ───────────────────────────────────
SELECT COUNT(*) AS zeilen,
       MIN(CAST(mp.MessagePropertyValue AS UNSIGNED))  AS kleinster,
       AVG(CAST(mp.MessagePropertyValue AS UNSIGNED))  AS mittel,
       MAX(CAST(mp.MessagePropertyValue AS UNSIGNED))  AS groesster,
       SUM(CAST(mp.MessagePropertyValue AS UNSIGNED) >    65536) AS ueber_64_kib,
       SUM(CAST(mp.MessagePropertyValue AS UNSIGNED) >  1048576) AS ueber_1_mib,
       SUM(CAST(mp.MessagePropertyValue AS UNSIGNED) > 10485760) AS ueber_10_mib
FROM Message m
JOIN MessageProperty mp ON mp.MessageID = m.MessageID
WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  AND mp.MessagePropertyName = 'FileReader.FileProperty.Size';

-- ─── M56 (c) — Endungen, Fenster B (verschaerfte Klassifikation) ────────────
SELECT CASE WHEN endung REGEXP '^[a-z][a-z0-9]{0,4}$' THEN endung
            WHEN endung REGEXP '^[0-9]+$'             THEN '<ziffernfolge>'
            ELSE '<sonstige>' END AS endung,
       COUNT(*) AS zeilen
FROM (
  SELECT LOWER(SUBSTRING_INDEX(mp.MessagePropertyValue, '.', -1)) AS endung
  FROM Message m
  JOIN MessageProperty mp ON mp.MessageID = m.MessageID
  WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
    AND mp.MessagePropertyName = 'FileReader.FileProperty.OriginalFilename'
    AND mp.MessagePropertyValue LIKE '%.%'
) x GROUP BY 1 ORDER BY zeilen DESC;

SHOW PROFILES;
