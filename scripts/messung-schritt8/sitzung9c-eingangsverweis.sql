-- Messrunde M73 — Sitzung 9c (Mehrfachtreffer Fenster A, danach Fenster B)
-- Auftrag: Prompt M73 vom 19.08.2026, §4 ("Fenster B nur bei Auffaelligkeit")
-- Ergebnis: docs/messungen-schritt8.md, Abschnitt M73
--
-- Die Auffaelligkeit, die Fenster B ausloest, liegt vor: In Fenster A traegt
-- KEINER der vorformulierten Ausgaenge die geforderte Mehrheit von 95 %
-- (Sender 70,1 %, Converter 29,9 %, Reader 0,016 %). Nach §3 ist das
-- Ausgang G. Ob es dabei bleibt oder ob eine Regel dahinter steht, die die
-- Tabelle nicht kennt, ist am groesseren Fenster zu pruefen.
--
-- Ausschliesslich SELECT / SET / EXPLAIN / SHOW. (Regel S1)
-- G1: keine MessageID, keine UUID, keine Ablagenkennung, kein Wert.
-- L4: Einstieg ueber Message.MessageID aus dem Zeitfenster.
-- L9: jedes Statement traegt ein Fenster.

SELECT @@global.read_only AS global_read_only_beginn;
SELECT NOW() AS serverzeit;
SET SESSION max_statement_time = 60;
SET profiling = 1;
SET profiling_history_size = 100;

SELECT 'S2-4 Mehrfachtreffer im Einzelnen, Fenster A' AS marke;

-- ═══ Fenster A — die 45 Mehrfachtreffer aufgeschluesselt ═══════════════════
-- Ausgewiesen wird die KOMBINATION der Stellen (Name und Schritt), an denen
-- derselbe Verweis haengt — keine Nachricht, kein Wert. (G1)
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
)
SELECT t.kombination, t.stellen, COUNT(*) AS nachrichten
FROM (
  SELECT e.MessageID,
         COUNT(*) AS stellen,
         GROUP_CONCAT(CONCAT(z.MessagePropertyName, '@', z.MessageActionID)
                      ORDER BY z.MessageActionID, z.MessagePropertyName
                      SEPARATOR ' + ') AS kombination
  FROM eingang e
  JOIN zeilen z ON z.MessageID = e.MessageID
               AND z.MessagePropertyName <> 'Message.Payload.GUID'
               AND z.wert = e.wert
  GROUP BY e.MessageID
  HAVING COUNT(*) >= 2
) t
GROUP BY t.kombination, t.stellen
ORDER BY nachrichten DESC, t.kombination;

SELECT 'S2-5B EXPLAIN Fenster B' AS marke;

-- ═══ Fenster B — EXPLAIN vor dem ersten Statement ══════════════════════════
EXPLAIN
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
)
SELECT z.MessagePropertyName, z.MessageActionID,
       COUNT(*)             AS zeilen,
       SUM(z.wert = e.wert) AS gleich
FROM eingang e
JOIN zeilen z ON z.MessageID = e.MessageID
             AND z.MessagePropertyName <> 'Message.Payload.GUID'
GROUP BY z.MessagePropertyName, z.MessageActionID
ORDER BY z.MessagePropertyName, z.MessageActionID;

SELECT 'S2-6B Kennzahlen und Lage des Treffers, Fenster B' AS marke;

-- ═══ Fenster B (1) — die Kennzahlen, verdichtet ════════════════════════════
-- LEFT JOIN, damit eine Nachricht ohne weitere Artefaktzeile nicht still
-- herausfaellt. `nachrichten_gesamt` ist die Zahl der Nachrichten des
-- Fensters mit einem Message.Payload.GUID.
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
         COALESCE(SUM(z.wert = e.wert), 0) AS treffer,
         MAX(CASE WHEN z.wert = e.wert THEN z.MessageActionID END) AS treffer_schritt,
         MAX(CASE WHEN z.MessagePropertyName LIKE '%.Payload.GUID'
                  THEN z.MessageActionID END)                      AS max_payload_schritt,
         MAX(z.MessagePropertyName LIKE '%Reader.%' AND z.MessageActionID = 0) AS hat_reader0,
         MAX(z.MessagePropertyName LIKE '%Sender.%')                           AS hat_sender,
         MAX(CASE WHEN z.wert = e.wert THEN
               CASE WHEN z.MessagePropertyName LIKE '%Sender.%'       THEN 'Sender'
                    WHEN z.MessagePropertyName LIKE 'Converter.%'     THEN 'Converter'
                    WHEN z.MessagePropertyName LIKE '%Reader.%'       THEN 'Reader'
                    WHEN z.MessagePropertyName LIKE 'DataWarehouse.%' THEN 'DataWarehouse'
                    ELSE 'sonstige' END END)                         AS treffer_klasse
  FROM eingang e
  LEFT JOIN zeilen z ON z.MessageID = e.MessageID
                    AND z.MessagePropertyName <> 'Message.Payload.GUID'
  GROUP BY e.MessageID
)
SELECT COUNT(*)                        AS nachrichten_gesamt,
       SUM(treffer >= 1)               AS mit_treffer,
       SUM(treffer = 0)                AS ohne_treffer,
       SUM(treffer >= 2)               AS mehrfachtreffer,
       SUM(treffer >= 3)               AS dreifachtreffer,
       SUM(treffer >= 1 AND treffer_schritt =  max_payload_schritt) AS auf_hoechstem_nutzdatenschritt,
       SUM(treffer >= 1 AND treffer_schritt <> max_payload_schritt) AS darunter,
       SUM(treffer_klasse = 'Sender')        AS klasse_sender,
       SUM(treffer_klasse = 'Converter')     AS klasse_converter,
       SUM(treffer_klasse = 'Reader')        AS klasse_reader,
       SUM(treffer_klasse = 'DataWarehouse') AS klasse_datawarehouse,
       SUM(hat_reader0 = 1)                  AS mit_reader_auf_schritt0,
       SUM(hat_sender  = 1)                  AS mit_sendedienst
FROM je_nachricht;

SELECT 'S2-7B je Name und Schritt, Fenster B' AS marke;

-- ═══ Fenster B (2) — je (Name, MessageActionID) ════════════════════════════
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
)
SELECT z.MessagePropertyName, z.MessageActionID,
       COUNT(*)                    AS zeilen,
       SUM(z.wert = e.wert)        AS gleich_kollation,
       SUM(BINARY z.wert = e.wert) AS gleich_binaer
FROM eingang e
JOIN zeilen z ON z.MessageID = e.MessageID
             AND z.MessagePropertyName <> 'Message.Payload.GUID'
GROUP BY z.MessagePropertyName, z.MessageActionID
HAVING gleich_kollation > 0
ORDER BY gleich_kollation DESC, z.MessagePropertyName, z.MessageActionID;

SHOW PROFILES;

SELECT @@global.read_only AS global_read_only_ende, NOW() AS serverzeit_ende;
