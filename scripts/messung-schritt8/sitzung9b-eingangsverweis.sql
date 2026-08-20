-- Messrunde M73 — Sitzung 9b (Stufen 2, 3 und 4)
-- Auftrag: Prompt M73 vom 19.08.2026, im Rahmen von
--          docs/messungen-schritt8-auftrag.md, Fassung 3, Teil A (Rahmen §1)
-- Ergebnis: docs/messungen-schritt8.md, Abschnitt M73
--
-- Laeuft erst, nachdem der Plan aus Sitzung 9 (Stufe 1) geprueft ist:
-- `mp` ueber PRIMARY, `m` ueber MessageLastUpdateIDX, kein Zugriff ueber
-- MessagePropertyValueIDX oder MessagePropertyNameValueIDX, kein Vollzugriff
-- auf MessageProperty. (Auftrag §4)
--
-- Ausschliesslich SELECT / SET / EXPLAIN / SHOW. (Regel S1)
-- G1: keine MessageID, keine UUID, keine Ablagenkennung, kein Wert.
-- L4: Einstieg ueber Message.MessageID aus Fenster A; der Wertvergleich
--     geschieht auf der gelesenen Menge einer einzelnen Nachricht.
-- L9: jedes Statement traegt Fenster A.

SELECT @@global.read_only AS global_read_only_beginn;
SELECT NOW() AS serverzeit;
SET SESSION max_statement_time = 60;
SET profiling = 1;
SET profiling_history_size = 100;

SELECT 'S2-0 EXPLAIN Fenster A vollstaendig' AS marke;

-- ═══ Stufe 2 (0) — EXPLAIN, vor dem ersten Statement des vollen Fensters ═══
EXPLAIN
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
SELECT z.MessagePropertyName, z.MessageActionID,
       COUNT(*)                    AS zeilen,
       SUM(z.wert = e.wert)        AS gleich_kollation,
       SUM(BINARY z.wert = e.wert) AS gleich_binaer,
       COUNT(DISTINCT CASE WHEN z.wert = e.wert THEN z.MessageID END) AS nachrichten
FROM eingang e
JOIN zeilen z ON z.MessageID = e.MessageID
             AND z.MessagePropertyName <> 'Message.Payload.GUID'
GROUP BY z.MessagePropertyName, z.MessageActionID
ORDER BY z.MessagePropertyName, z.MessageActionID;

SELECT 'S2-1 je Name und Schritt, Fenster A' AS marke;

-- ═══ Stufe 2 (1) — je (Name, MessageActionID), Fenster A vollstaendig ══════
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
SELECT z.MessagePropertyName, z.MessageActionID,
       COUNT(*)                    AS zeilen,
       SUM(z.wert = e.wert)        AS gleich_kollation,
       SUM(BINARY z.wert = e.wert) AS gleich_binaer,
       COUNT(DISTINCT CASE WHEN z.wert = e.wert THEN z.MessageID END) AS nachrichten
FROM eingang e
JOIN zeilen z ON z.MessageID = e.MessageID
             AND z.MessagePropertyName <> 'Message.Payload.GUID'
GROUP BY z.MessagePropertyName, z.MessageActionID
ORDER BY z.MessagePropertyName, z.MessageActionID;

SELECT 'S2-2 Kennzahlen und Verteilung der Trefferzahl' AS marke;

-- ═══ Stufe 2 (2) — die Kennzahlen aus §5 ═══════════════════════════════════
-- nachrichten_gesamt = Summe ueber alle Zeilen dieser Ausgabe
-- ohne_treffer       = Zeile geteilte_zeilen = 0
-- mit_treffer        = Summe der Zeilen geteilte_zeilen >= 1
-- mehrfachtreffer    = Summe der Zeilen geteilte_zeilen >= 2
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

SELECT 'S2-3 Zusatz: Lage des Treffers im Ablauf' AS marke;

-- ═══ Stufe 2 (3) — ZUSATZ zu §5, begruendet ════════════════════════════════
-- Stufe 1 hat gezeigt, dass die Treffer auf MEHRERE Familien fallen
-- (FTPSender, Converter, AS2Sender). Ohne diese Auszaehlung liesse sich
-- "es gibt keine eine Regel" (Ausgang G) nicht von "es gibt eine Regel, die
-- die vorregistrierte Tabelle nicht kennt" unterscheiden. Gemessen wird,
-- ob die treffende Zeile die Nutzdatenzeile mit dem HOECHSTEN
-- MessageActionID der Nachricht ist.
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
         SUM(z.wert = e.wert) AS treffer,
         MAX(CASE WHEN z.wert = e.wert THEN z.MessageActionID END) AS treffer_schritt,
         MAX(CASE WHEN z.MessagePropertyName LIKE '%.Payload.GUID'
                  THEN z.MessageActionID END)                      AS max_payload_schritt
  FROM eingang e
  JOIN zeilen z ON z.MessageID = e.MessageID
               AND z.MessagePropertyName <> 'Message.Payload.GUID'
  GROUP BY e.MessageID
)
SELECT treffer,
       CASE WHEN treffer = 0 THEN NULL
            WHEN treffer_schritt = max_payload_schritt THEN 1 ELSE 0 END
         AS auf_hoechstem_nutzdatenschritt,
       COUNT(*) AS nachrichten
FROM je_nachricht
GROUP BY 1, 2
ORDER BY 1, 2;

SELECT 'S3-1 Richtungsprobe' AS marke;

-- ═══ Stufe 3 — die Richtungsprobe ══════════════════════════════════════════
-- Die Richtung liegt nicht als Datum vor (PROJEKTBESCHREIBUNG.md §4.4). Sie
-- wird genaehert: traegt die Nachricht eine %Reader.%-Familie auf Schritt 0,
-- und traegt sie eine %Sender.%-Familie?
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
         MAX(z.MessagePropertyName LIKE '%Reader.%' AND z.MessageActionID = 0) AS hat_reader0,
         MAX(z.MessagePropertyName LIKE '%Sender.%')                           AS hat_sender,
         SUM(z.wert = e.wert)                                                  AS treffer,
         MAX(CASE WHEN z.wert = e.wert THEN
               CASE WHEN z.MessagePropertyName LIKE '%Sender.%'        THEN 'Sender'
                    WHEN z.MessagePropertyName LIKE 'Converter.%'      THEN 'Converter'
                    WHEN z.MessagePropertyName LIKE '%Reader.%'        THEN 'Reader'
                    WHEN z.MessagePropertyName LIKE 'DataWarehouse.%'  THEN 'DataWarehouse'
                    ELSE 'sonstige' END END)                                   AS treffer_klasse
  FROM eingang e
  JOIN zeilen z ON z.MessageID = e.MessageID
               AND z.MessagePropertyName <> 'Message.Payload.GUID'
  GROUP BY e.MessageID
)
SELECT hat_reader0, hat_sender,
       COALESCE(treffer_klasse, 'ohne Treffer') AS treffer_klasse,
       COUNT(*) AS nachrichten
FROM je_nachricht
GROUP BY hat_reader0, hat_sender, COALESCE(treffer_klasse, 'ohne Treffer')
ORDER BY hat_reader0, hat_sender, nachrichten DESC;

SELECT 'S4-1 Mandantenprobe je Name und Schritt' AS marke;

-- ═══ Stufe 4 — die Mandantenprobe (L7, L15) ════════════════════════════════
-- NEXANS und der kleine Mandant IBISGUS (hergeleitet in M53).
WITH zeilen AS (
  SELECT pm.MandantID, mp.MessageID, mp.MessagePropertyName, mp.MessageActionID,
         mp.MessagePropertyValue AS wert
  FROM Message m
  JOIN Process p          ON p.ProcessID  = m.ProcessID
  JOIN ProjectMandant pm  ON pm.ProjectID = p.ProjectID
  JOIN MessageProperty mp ON mp.MessageID = m.MessageID
  WHERE m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
    AND pm.MandantID IN ('NEXANS', 'IBISGUS')
    AND (mp.MessagePropertyName LIKE '%.Payload.GUID'
      OR mp.MessagePropertyName LIKE '%.Log.GUID')
),
eingang AS (
  SELECT MandantID, MessageID, wert FROM zeilen
  WHERE MessagePropertyName = 'Message.Payload.GUID'
)
SELECT e.MandantID, z.MessagePropertyName, z.MessageActionID,
       COUNT(*)             AS zeilen,
       SUM(z.wert = e.wert) AS gleich
FROM eingang e
JOIN zeilen z ON z.MessageID = e.MessageID
             AND z.MessagePropertyName <> 'Message.Payload.GUID'
GROUP BY e.MandantID, z.MessagePropertyName, z.MessageActionID
HAVING gleich > 0
ORDER BY e.MandantID, gleich DESC, z.MessagePropertyName, z.MessageActionID;

SELECT 'S4-2 Mandantenprobe: Kennzahlen' AS marke;

WITH zeilen AS (
  SELECT pm.MandantID, mp.MessageID, mp.MessagePropertyName, mp.MessageActionID,
         mp.MessagePropertyValue AS wert
  FROM Message m
  JOIN Process p          ON p.ProcessID  = m.ProcessID
  JOIN ProjectMandant pm  ON pm.ProjectID = p.ProjectID
  JOIN MessageProperty mp ON mp.MessageID = m.MessageID
  WHERE m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
    AND pm.MandantID IN ('NEXANS', 'IBISGUS')
    AND (mp.MessagePropertyName LIKE '%.Payload.GUID'
      OR mp.MessagePropertyName LIKE '%.Log.GUID')
),
eingang AS (
  SELECT MandantID, MessageID, wert FROM zeilen
  WHERE MessagePropertyName = 'Message.Payload.GUID'
),
je_nachricht AS (
  SELECT e.MandantID, e.MessageID,
         SUM(z.wert = e.wert) AS treffer,
         MAX(CASE WHEN z.wert = e.wert THEN z.MessageActionID END) AS treffer_schritt,
         MAX(CASE WHEN z.MessagePropertyName LIKE '%.Payload.GUID'
                  THEN z.MessageActionID END)                      AS max_payload_schritt,
         MAX(CASE WHEN z.wert = e.wert THEN
               CASE WHEN z.MessagePropertyName LIKE '%Sender.%'       THEN 'Sender'
                    WHEN z.MessagePropertyName LIKE 'Converter.%'     THEN 'Converter'
                    WHEN z.MessagePropertyName LIKE '%Reader.%'       THEN 'Reader'
                    WHEN z.MessagePropertyName LIKE 'DataWarehouse.%' THEN 'DataWarehouse'
                    ELSE 'sonstige' END END)                         AS treffer_klasse
  FROM eingang e
  JOIN zeilen z ON z.MessageID = e.MessageID
               AND z.MessagePropertyName <> 'Message.Payload.GUID'
  GROUP BY e.MandantID, e.MessageID
)
SELECT MandantID,
       COUNT(*)                                            AS nachrichten_gesamt,
       SUM(treffer >= 1)                                   AS mit_treffer,
       SUM(treffer = 0)                                    AS ohne_treffer,
       SUM(treffer >= 2)                                   AS mehrfachtreffer,
       SUM(treffer_klasse = 'Sender')                      AS treffer_sender,
       SUM(treffer_klasse = 'Converter')                   AS treffer_converter,
       SUM(treffer_klasse = 'Reader')                      AS treffer_reader,
       SUM(treffer >= 1 AND treffer_schritt = max_payload_schritt) AS auf_hoechstem_nutzdatenschritt
FROM je_nachricht
GROUP BY MandantID
ORDER BY nachrichten_gesamt DESC;

SHOW PROFILES;

SELECT @@global.read_only AS global_read_only_ende, NOW() AS serverzeit_ende;
