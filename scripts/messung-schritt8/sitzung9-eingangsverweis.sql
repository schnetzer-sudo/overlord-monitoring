-- Messrunde M73 — Sitzung 9, Stufe 1 (Stichprobe und Planpruefung)
-- Auftrag: Prompt M73 vom 19.08.2026, im Rahmen von
--          docs/messungen-schritt8-auftrag.md, Fassung 3, Teil A (Rahmen §1)
-- Frage:   Traegt Message.Payload.GUID denselben Verweis wie ein anderes
--          Artefakt derselben Nachricht — und wenn ja, welches?
-- Ergebnis: docs/messungen-schritt8.md, Abschnitt M73
--
-- Ausschliesslich SELECT / SET / EXPLAIN / SHOW. (Regel S1)
--
-- G1: In dieser Datei steht keine MessageID, keine UUID, keine
-- Ablagenkennung und kein Wert. Ausgewiesen wird ausschliesslich, OB zwei
-- Verweise uebereinstimmen — niemals der Verweis selbst. Die Stichprobe wird
-- in der Sitzung selbst deterministisch hergeleitet (ORDER BY MessageID
-- LIMIT 200), damit kein Pruefwert das Skript beruehrt.
--
-- L4: Der Einstieg laeuft ueber Message.MessageID aus dem Zeitfenster. Der
-- Wertvergleich geschieht auf der bereits gelesenen, kleinen Menge einer
-- einzelnen Nachricht (CTE `zeilen`) und niemals als Einstieg ueber
-- MessagePropertyValue. Die beiden Praefix-Indizes ueber den Wert
-- (MessagePropertyValueIDX, MessagePropertyNameValueIDX) duerfen im EXPLAIN
-- nicht auftauchen; taeten sie es, wird abgebrochen und berichtet.
--
-- L9: Jedes Statement gegen Message/MessageProperty traegt Fenster A.

SELECT @@global.read_only AS global_read_only_beginn;
SELECT VERSION() AS version, NOW() AS serverzeit;
SET SESSION max_statement_time = 60;
SET profiling = 1;
SET profiling_history_size = 100;

SELECT 'S1-1 Umfang der Stichprobe' AS marke;

-- ═══ Stufe 1 (1) — Umfang der Stichprobe ═══════════════════════════════════
WITH stichprobe AS (
  SELECT m.MessageID
  FROM Message m
  WHERE m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  ORDER BY m.MessageID
  LIMIT 200
),
zeilen AS (
  SELECT mp.MessageID, mp.MessagePropertyName, mp.MessageActionID,
         mp.MessagePropertyValue AS wert
  FROM stichprobe s
  JOIN MessageProperty mp ON mp.MessageID = s.MessageID
  WHERE mp.MessagePropertyName LIKE '%.Payload.GUID'
     OR mp.MessagePropertyName LIKE '%.Log.GUID'
)
SELECT COUNT(DISTINCT MessageID) AS nachrichten,
       COUNT(*)                  AS artefaktzeilen,
       SUM(MessagePropertyName = 'Message.Payload.GUID') AS eingangszeilen,
       COUNT(DISTINCT CASE WHEN MessagePropertyName = 'Message.Payload.GUID'
                           THEN MessageID END)           AS nachrichten_mit_eingang,
       MIN(CHAR_LENGTH(wert)) AS kuerzester,
       MAX(CHAR_LENGTH(wert)) AS laengster
FROM zeilen;

SELECT 'S1-2 EXPLAIN der Vergleichsabfrage' AS marke;

-- ═══ Stufe 1 (2) — EXPLAIN, vor dem Statement ══════════════════════════════
EXPLAIN
WITH stichprobe AS (
  SELECT m.MessageID
  FROM Message m
  WHERE m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  ORDER BY m.MessageID
  LIMIT 200
),
zeilen AS (
  SELECT mp.MessageID, mp.MessagePropertyName, mp.MessageActionID,
         mp.MessagePropertyValue AS wert
  FROM stichprobe s
  JOIN MessageProperty mp ON mp.MessageID = s.MessageID
  WHERE mp.MessagePropertyName LIKE '%.Payload.GUID'
     OR mp.MessagePropertyName LIKE '%.Log.GUID'
),
eingang AS (
  SELECT MessageID, wert FROM zeilen
  WHERE MessagePropertyName = 'Message.Payload.GUID'
)
SELECT z.MessagePropertyName, z.MessageActionID,
       COUNT(*)                            AS zeilen,
       SUM(z.wert = e.wert)                AS gleich_kollation,
       SUM(BINARY z.wert = e.wert)         AS gleich_binaer
FROM eingang e
JOIN zeilen z ON z.MessageID = e.MessageID
             AND z.MessagePropertyName <> 'Message.Payload.GUID'
GROUP BY z.MessagePropertyName, z.MessageActionID
ORDER BY z.MessagePropertyName, z.MessageActionID;

SELECT 'S1-3 Vergleich je Name und Schritt' AS marke;

-- ═══ Stufe 1 (3) — der Vergleich, je Name und Schritt ══════════════════════
-- `zeilen`  = alle Artefaktzeilen der 200 Nachrichten
-- `eingang` = die Message.Payload.GUID-Zeile derselben Nachricht
-- Ausgewiesen wird, in wie vielen Zeilen der Wert mit dem des Eingangs
-- uebereinstimmt — der Wert selbst erscheint nirgends. (G1)
WITH stichprobe AS (
  SELECT m.MessageID
  FROM Message m
  WHERE m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  ORDER BY m.MessageID
  LIMIT 200
),
zeilen AS (
  SELECT mp.MessageID, mp.MessagePropertyName, mp.MessageActionID,
         mp.MessagePropertyValue AS wert
  FROM stichprobe s
  JOIN MessageProperty mp ON mp.MessageID = s.MessageID
  WHERE mp.MessagePropertyName LIKE '%.Payload.GUID'
     OR mp.MessagePropertyName LIKE '%.Log.GUID'
),
eingang AS (
  SELECT MessageID, wert FROM zeilen
  WHERE MessagePropertyName = 'Message.Payload.GUID'
)
SELECT z.MessagePropertyName, z.MessageActionID,
       COUNT(*)                            AS zeilen,
       SUM(z.wert = e.wert)                AS gleich_kollation,
       SUM(BINARY z.wert = e.wert)         AS gleich_binaer
FROM eingang e
JOIN zeilen z ON z.MessageID = e.MessageID
             AND z.MessagePropertyName <> 'Message.Payload.GUID'
GROUP BY z.MessagePropertyName, z.MessageActionID
ORDER BY z.MessagePropertyName, z.MessageActionID;

SELECT 'S1-4 Verteilung der Trefferzahl' AS marke;

-- ═══ Stufe 1 (4) — wie viele andere Zeilen teilen den Verweis? ═════════════
WITH stichprobe AS (
  SELECT m.MessageID
  FROM Message m
  WHERE m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  ORDER BY m.MessageID
  LIMIT 200
),
zeilen AS (
  SELECT mp.MessageID, mp.MessagePropertyName, mp.MessageActionID,
         mp.MessagePropertyValue AS wert
  FROM stichprobe s
  JOIN MessageProperty mp ON mp.MessageID = s.MessageID
  WHERE mp.MessagePropertyName LIKE '%.Payload.GUID'
     OR mp.MessagePropertyName LIKE '%.Log.GUID'
),
eingang AS (
  SELECT MessageID, wert FROM zeilen
  WHERE MessagePropertyName = 'Message.Payload.GUID'
)
SELECT t.geteilt AS geteilte_zeilen, COUNT(*) AS nachrichten
FROM (
  SELECT e.MessageID,
         SUM(CASE WHEN z.MessageID IS NOT NULL AND z.wert = e.wert THEN 1 ELSE 0 END) AS geteilt
  FROM eingang e
  LEFT JOIN zeilen z ON z.MessageID = e.MessageID
                    AND z.MessagePropertyName <> 'Message.Payload.GUID'
  GROUP BY e.MessageID
) t
GROUP BY t.geteilt
ORDER BY t.geteilt;

SHOW PROFILES;

SELECT @@global.read_only AS global_read_only_ende, NOW() AS serverzeit_ende;
