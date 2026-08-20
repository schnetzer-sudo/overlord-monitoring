-- Messrunde M73 — Sitzung 9d (Erreichbarkeit und Gegenprobe zur Richtung)
-- Auftrag: Prompt M73 vom 19.08.2026, §3 Zeile A: "Die Datei bleibt ueber den
--          Sendeschritt erreichbar — das ist zu belegen und nicht anzunehmen."
-- Ergebnis: docs/messungen-schritt8.md, Abschnitt M73
--
-- (1) Auf welchem Schritt liegt die treffende Zeile? Nur Zeilen ab Schritt 1
--     stehen in der Zeitleiste (das Backend nimmt Schritt 0 aus schritte[]
--     aus, nachrichtendetail.md §4). Eine Nachricht, deren einzige Entsprechung
--     auf Schritt 0 liegt, waere nach dem Entfernen des Eingangsverweises
--     nicht mehr erreichbar — genau der Datenverlust, den §3 ausschliesst.
-- (2) Gegenprobe zur Richtungsnaeherung aus Stufe 3: Fenster A hat 950
--     Nachrichten ohne %Reader.%-Familie auf Schritt 0, und M54 zaehlt
--     genau 950 DataWarehouse-Nachrichten. Wenn beide Mengen deckungsgleich
--     sind, trennt die Naeherung keine Richtungen.
--
-- Ausschliesslich SELECT / SET / SHOW. (Regel S1)
-- G1: keine MessageID, keine UUID, keine Ablagenkennung, kein Wert.
-- L4/L9 wie in Sitzung 9 und 9b.

SELECT @@global.read_only AS global_read_only_beginn;
SELECT NOW() AS serverzeit;
SET SESSION max_statement_time = 60;
SET profiling = 1;
SET profiling_history_size = 100;

SELECT 'S5-1A Lage des Treffers im Schrittraum, Fenster A' AS marke;

WITH zeilen AS (
  SELECT mp.MessageID, mp.MessagePropertyName, mp.MessageActionID,
         mp.MessagePropertyValue AS wert
  FROM Message m
  JOIN MessageProperty mp ON mp.MessageID = m.MessageID
  WHERE m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
    AND (mp.MessagePropertyName LIKE '%.Payload.GUID'
      OR mp.MessagePropertyName LIKE '%.Log.GUID')
),
eingang AS (
  SELECT MessageID, wert FROM zeilen
  WHERE MessagePropertyName = 'Message.Payload.GUID'
),
je_nachricht AS (
  SELECT e.MessageID,
         COALESCE(SUM(z.wert = e.wert), 0)                          AS treffer,
         MIN(CASE WHEN z.wert = e.wert THEN z.MessageActionID END)  AS min_treffer_schritt,
         MAX(CASE WHEN z.wert = e.wert THEN z.MessageActionID END)  AS max_treffer_schritt
  FROM eingang e
  LEFT JOIN zeilen z ON z.MessageID = e.MessageID
                    AND z.MessagePropertyName <> 'Message.Payload.GUID'
  GROUP BY e.MessageID
)
SELECT COUNT(*)                                     AS nachrichten_gesamt,
       SUM(treffer = 0)                             AS ohne_treffer,
       SUM(treffer >= 1 AND max_treffer_schritt >= 1) AS erreichbar_ueber_die_zeitleiste,
       SUM(treffer >= 1 AND max_treffer_schritt =  0) AS nur_auf_schritt_0,
       SUM(treffer >= 1 AND min_treffer_schritt =  0) AS mindestens_eine_stelle_auf_schritt_0
FROM je_nachricht;

SELECT 'S5-2A Gegenprobe: Reader auf Schritt 0 gegen DataWarehouse, Fenster A' AS marke;

WITH zeilen AS (
  SELECT mp.MessageID, mp.MessagePropertyName, mp.MessageActionID
  FROM Message m
  JOIN MessageProperty mp ON mp.MessageID = m.MessageID
  WHERE m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
    AND (mp.MessagePropertyName LIKE '%.Payload.GUID'
      OR mp.MessagePropertyName LIKE '%.Log.GUID')
)
SELECT hat_reader0, hat_datawarehouse, COUNT(*) AS nachrichten
FROM (
  SELECT MessageID,
         MAX(MessagePropertyName LIKE '%Reader.%' AND MessageActionID = 0) AS hat_reader0,
         MAX(MessagePropertyName LIKE 'DataWarehouse.%')                   AS hat_datawarehouse
  FROM zeilen
  GROUP BY MessageID
) t
GROUP BY hat_reader0, hat_datawarehouse
ORDER BY hat_reader0, hat_datawarehouse;

SELECT 'S5-3B Lage des Treffers im Schrittraum, Fenster B' AS marke;

WITH zeilen AS (
  SELECT mp.MessageID, mp.MessagePropertyName, mp.MessageActionID,
         mp.MessagePropertyValue AS wert
  FROM Message m
  JOIN MessageProperty mp ON mp.MessageID = m.MessageID
  WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
    AND (mp.MessagePropertyName LIKE '%.Payload.GUID'
      OR mp.MessagePropertyName LIKE '%.Log.GUID')
),
eingang AS (
  SELECT MessageID, wert FROM zeilen
  WHERE MessagePropertyName = 'Message.Payload.GUID'
),
je_nachricht AS (
  SELECT e.MessageID,
         COALESCE(SUM(z.wert = e.wert), 0)                          AS treffer,
         MIN(CASE WHEN z.wert = e.wert THEN z.MessageActionID END)  AS min_treffer_schritt,
         MAX(CASE WHEN z.wert = e.wert THEN z.MessageActionID END)  AS max_treffer_schritt
  FROM eingang e
  LEFT JOIN zeilen z ON z.MessageID = e.MessageID
                    AND z.MessagePropertyName <> 'Message.Payload.GUID'
  GROUP BY e.MessageID
)
SELECT COUNT(*)                                     AS nachrichten_gesamt,
       SUM(treffer = 0)                             AS ohne_treffer,
       SUM(treffer >= 1 AND max_treffer_schritt >= 1) AS erreichbar_ueber_die_zeitleiste,
       SUM(treffer >= 1 AND max_treffer_schritt =  0) AS nur_auf_schritt_0,
       SUM(treffer >= 1 AND min_treffer_schritt =  0) AS mindestens_eine_stelle_auf_schritt_0
FROM je_nachricht;

SHOW PROFILES;

SELECT @@global.read_only AS global_read_only_ende, NOW() AS serverzeit_ende;
