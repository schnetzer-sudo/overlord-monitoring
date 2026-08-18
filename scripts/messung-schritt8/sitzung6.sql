-- Messrunde vor Schritt 8 — Sitzung 6 (Schlusssitzung Teil A)
-- Auftrag: docs/messungen-schritt8-auftrag.md, Fassung 3 vom 17.08.2026
-- Inhalt: M57, M58, M64a, Abschluss-read_only
-- Ausschliesslich SELECT / SET / EXPLAIN / SHOW. (Regel S1)
--
-- G1: In dieser Datei steht keine MessageID und kein Wert. Jeder Pruefwert
-- wird in der Sitzung selbst ueber eine deterministische Auswahlabfrage
-- hergeleitet und in eine Sitzungsvariable gelegt.
--
-- ABWEICHUNG bei M58: Die Laufzeitmessung laeuft gegen eine zaehlende Huelle
-- um das echte Statement, weil das echte Statement MessagePropertyValue
-- liefert und diese Werte nicht auf den Bildschirm duerfen (G1). Der EXPLAIN
-- laeuft gegen das ROHE Statement, unveraendert.

SELECT @@global.read_only AS global_read_only_beginn;
SELECT NOW() AS serverzeit;
SET SESSION max_statement_time = 60;
SET profiling = 1;
SET profiling_history_size = 100;

-- ═══ M57 — An welchem Schritt haengt welches Artefakt? (Fenster A) ══════════
-- Join auf SOSAction ueber BEIDE Spalten von MessageAction, niemals ueber
-- Message.SOSID (M15: 100 % gegen 96,17 %).
SELECT mp.MessagePropertyName, mp.MessageActionID,
       COUNT(*) AS zeilen,
       SUM(ma.MessageID IS NULL) AS ohne_schrittzeile,
       COUNT(DISTINCT sa.SOSActionName) AS verschiedene_schrittnamen,
       SUM(sa.SOSID IS NULL) AS ohne_schrittnamen
FROM Message m
JOIN MessageProperty mp ON mp.MessageID = m.MessageID
LEFT JOIN MessageAction ma ON ma.MessageID = mp.MessageID
                          AND ma.MessageActionID = mp.MessageActionID
LEFT JOIN SOSAction sa ON sa.SOSID = ma.SOSID AND sa.SOSActionID = ma.SOSActionID
WHERE m.MessageLastUpdate >= '2025-12-29 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  AND (mp.MessagePropertyName LIKE '%.Payload.GUID'
    OR mp.MessagePropertyName LIKE '%.Log.GUID')
GROUP BY mp.MessagePropertyName, mp.MessageActionID
ORDER BY mp.MessagePropertyName, mp.MessageActionID;

-- ═══ M58 — Kostet das etwas? (Regel L7) ════════════════════════════════════
-- Auswahl der Pruefnachrichten: je Mandant die Nachricht aus Fenster A mit den
-- MEISTEN Artefaktverweisen (der teuerste Fall, L7), Gleichstand nach
-- kleinster MessageID. Deterministisch, ohne Literal.

SET @mid_nexans = (
  SELECT mp.MessageID
  FROM Message m
  JOIN Process p         ON p.ProcessID = m.ProcessID
  JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID
  JOIN MessageProperty mp ON mp.MessageID = m.MessageID
  WHERE m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
    AND pm.MandantID = 'NEXANS'
    AND (mp.MessagePropertyName LIKE '%.Payload.GUID'
      OR mp.MessagePropertyName LIKE '%.Log.GUID')
  GROUP BY mp.MessageID
  ORDER BY COUNT(*) DESC, mp.MessageID ASC
  LIMIT 1);

SET @mid_ibisgus = (
  SELECT mp.MessageID
  FROM Message m
  JOIN Process p         ON p.ProcessID = m.ProcessID
  JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID
  JOIN MessageProperty mp ON mp.MessageID = m.MessageID
  WHERE m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
    AND pm.MandantID = 'IBISGUS'
    AND (mp.MessagePropertyName LIKE '%.Payload.GUID'
      OR mp.MessagePropertyName LIKE '%.Log.GUID')
  GROUP BY mp.MessageID
  ORDER BY COUNT(*) DESC, mp.MessageID ASC
  LIMIT 1);

-- Kontrolle: wie viele Artefakte tragen die beiden gewaehlten Nachrichten?
-- (Zahl, keine Kennung.)
SELECT (SELECT COUNT(*) FROM MessageProperty mp WHERE mp.MessageID = @mid_nexans
          AND (mp.MessagePropertyName LIKE '%.Payload.GUID'
            OR mp.MessagePropertyName LIKE '%.Log.GUID')) AS artefakte_nexans,
       (SELECT COUNT(*) FROM MessageProperty mp WHERE mp.MessageID = @mid_ibisgus
          AND (mp.MessagePropertyName LIKE '%.Payload.GUID'
            OR mp.MessagePropertyName LIKE '%.Log.GUID')) AS artefakte_ibisgus;

-- ─── M58 (1) — Artefaktliste einer Nachricht, EXPLAIN gegen das ROHE Statement
EXPLAIN
SELECT mp.MessagePropertyName, mp.MessagePropertyValue, mp.MessageActionID
FROM MessageProperty mp
JOIN Message m         ON m.MessageID = mp.MessageID
JOIN Process p         ON p.ProcessID = m.ProcessID
JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE mp.MessageID = @mid_nexans
  AND pm.MandantID = 'NEXANS'
  AND (mp.MessagePropertyName LIKE '%.Payload.GUID'
    OR mp.MessagePropertyName LIKE '%.Log.GUID');

-- ─── M58 (2) — Aufloesung einer Kennung, EXPLAIN gegen das ROHE Statement
EXPLAIN
SELECT ServiceID, ServiceConnectString, ServiceStatus
FROM Service WHERE ServiceID = 'FILESTOREPROD09';

-- ─── M58 (3) — Existenznachweis der Nachricht fuer den Mandanten
EXPLAIN
SELECT 1
FROM Message m
JOIN Process p         ON p.ProcessID = m.ProcessID
JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE m.MessageID = @mid_nexans AND pm.MandantID = 'NEXANS'
LIMIT 1;

-- ─── M58 Laufzeiten: Aufwaermlauf + fuenf Messlaeufe je Abfrage ─────────────
-- (1) NEXANS
SELECT COUNT(*) AS n, SUM(CHAR_LENGTH(v)) AS s FROM ( SELECT mp.MessagePropertyValue AS v FROM MessageProperty mp JOIN Message m ON m.MessageID = mp.MessageID JOIN Process p ON p.ProcessID = m.ProcessID JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID WHERE mp.MessageID = @mid_nexans AND pm.MandantID = 'NEXANS' AND (mp.MessagePropertyName LIKE '%.Payload.GUID' OR mp.MessagePropertyName LIKE '%.Log.GUID') ) x;
SELECT COUNT(*) AS n, SUM(CHAR_LENGTH(v)) AS s FROM ( SELECT mp.MessagePropertyValue AS v FROM MessageProperty mp JOIN Message m ON m.MessageID = mp.MessageID JOIN Process p ON p.ProcessID = m.ProcessID JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID WHERE mp.MessageID = @mid_nexans AND pm.MandantID = 'NEXANS' AND (mp.MessagePropertyName LIKE '%.Payload.GUID' OR mp.MessagePropertyName LIKE '%.Log.GUID') ) x;
SELECT COUNT(*) AS n, SUM(CHAR_LENGTH(v)) AS s FROM ( SELECT mp.MessagePropertyValue AS v FROM MessageProperty mp JOIN Message m ON m.MessageID = mp.MessageID JOIN Process p ON p.ProcessID = m.ProcessID JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID WHERE mp.MessageID = @mid_nexans AND pm.MandantID = 'NEXANS' AND (mp.MessagePropertyName LIKE '%.Payload.GUID' OR mp.MessagePropertyName LIKE '%.Log.GUID') ) x;
SELECT COUNT(*) AS n, SUM(CHAR_LENGTH(v)) AS s FROM ( SELECT mp.MessagePropertyValue AS v FROM MessageProperty mp JOIN Message m ON m.MessageID = mp.MessageID JOIN Process p ON p.ProcessID = m.ProcessID JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID WHERE mp.MessageID = @mid_nexans AND pm.MandantID = 'NEXANS' AND (mp.MessagePropertyName LIKE '%.Payload.GUID' OR mp.MessagePropertyName LIKE '%.Log.GUID') ) x;
SELECT COUNT(*) AS n, SUM(CHAR_LENGTH(v)) AS s FROM ( SELECT mp.MessagePropertyValue AS v FROM MessageProperty mp JOIN Message m ON m.MessageID = mp.MessageID JOIN Process p ON p.ProcessID = m.ProcessID JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID WHERE mp.MessageID = @mid_nexans AND pm.MandantID = 'NEXANS' AND (mp.MessagePropertyName LIKE '%.Payload.GUID' OR mp.MessagePropertyName LIKE '%.Log.GUID') ) x;
SELECT COUNT(*) AS n, SUM(CHAR_LENGTH(v)) AS s FROM ( SELECT mp.MessagePropertyValue AS v FROM MessageProperty mp JOIN Message m ON m.MessageID = mp.MessageID JOIN Process p ON p.ProcessID = m.ProcessID JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID WHERE mp.MessageID = @mid_nexans AND pm.MandantID = 'NEXANS' AND (mp.MessagePropertyName LIKE '%.Payload.GUID' OR mp.MessagePropertyName LIKE '%.Log.GUID') ) x;

-- (1) IBISGUS
SELECT COUNT(*) AS n, SUM(CHAR_LENGTH(v)) AS s FROM ( SELECT mp.MessagePropertyValue AS v FROM MessageProperty mp JOIN Message m ON m.MessageID = mp.MessageID JOIN Process p ON p.ProcessID = m.ProcessID JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID WHERE mp.MessageID = @mid_ibisgus AND pm.MandantID = 'IBISGUS' AND (mp.MessagePropertyName LIKE '%.Payload.GUID' OR mp.MessagePropertyName LIKE '%.Log.GUID') ) x;
SELECT COUNT(*) AS n, SUM(CHAR_LENGTH(v)) AS s FROM ( SELECT mp.MessagePropertyValue AS v FROM MessageProperty mp JOIN Message m ON m.MessageID = mp.MessageID JOIN Process p ON p.ProcessID = m.ProcessID JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID WHERE mp.MessageID = @mid_ibisgus AND pm.MandantID = 'IBISGUS' AND (mp.MessagePropertyName LIKE '%.Payload.GUID' OR mp.MessagePropertyName LIKE '%.Log.GUID') ) x;
SELECT COUNT(*) AS n, SUM(CHAR_LENGTH(v)) AS s FROM ( SELECT mp.MessagePropertyValue AS v FROM MessageProperty mp JOIN Message m ON m.MessageID = mp.MessageID JOIN Process p ON p.ProcessID = m.ProcessID JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID WHERE mp.MessageID = @mid_ibisgus AND pm.MandantID = 'IBISGUS' AND (mp.MessagePropertyName LIKE '%.Payload.GUID' OR mp.MessagePropertyName LIKE '%.Log.GUID') ) x;
SELECT COUNT(*) AS n, SUM(CHAR_LENGTH(v)) AS s FROM ( SELECT mp.MessagePropertyValue AS v FROM MessageProperty mp JOIN Message m ON m.MessageID = mp.MessageID JOIN Process p ON p.ProcessID = m.ProcessID JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID WHERE mp.MessageID = @mid_ibisgus AND pm.MandantID = 'IBISGUS' AND (mp.MessagePropertyName LIKE '%.Payload.GUID' OR mp.MessagePropertyName LIKE '%.Log.GUID') ) x;
SELECT COUNT(*) AS n, SUM(CHAR_LENGTH(v)) AS s FROM ( SELECT mp.MessagePropertyValue AS v FROM MessageProperty mp JOIN Message m ON m.MessageID = mp.MessageID JOIN Process p ON p.ProcessID = m.ProcessID JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID WHERE mp.MessageID = @mid_ibisgus AND pm.MandantID = 'IBISGUS' AND (mp.MessagePropertyName LIKE '%.Payload.GUID' OR mp.MessagePropertyName LIKE '%.Log.GUID') ) x;
SELECT COUNT(*) AS n, SUM(CHAR_LENGTH(v)) AS s FROM ( SELECT mp.MessagePropertyValue AS v FROM MessageProperty mp JOIN Message m ON m.MessageID = mp.MessageID JOIN Process p ON p.ProcessID = m.ProcessID JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID WHERE mp.MessageID = @mid_ibisgus AND pm.MandantID = 'IBISGUS' AND (mp.MessagePropertyName LIKE '%.Payload.GUID' OR mp.MessagePropertyName LIKE '%.Log.GUID') ) x;

-- (2) Aufloesung einer Kennung — mandantenunabhaengig, deshalb einmal sechs Laeufe
SELECT COUNT(*) AS n, SUM(CHAR_LENGTH(ServiceConnectString)) AS s FROM Service WHERE ServiceID = 'FILESTOREPROD09';
SELECT COUNT(*) AS n, SUM(CHAR_LENGTH(ServiceConnectString)) AS s FROM Service WHERE ServiceID = 'FILESTOREPROD09';
SELECT COUNT(*) AS n, SUM(CHAR_LENGTH(ServiceConnectString)) AS s FROM Service WHERE ServiceID = 'FILESTOREPROD09';
SELECT COUNT(*) AS n, SUM(CHAR_LENGTH(ServiceConnectString)) AS s FROM Service WHERE ServiceID = 'FILESTOREPROD09';
SELECT COUNT(*) AS n, SUM(CHAR_LENGTH(ServiceConnectString)) AS s FROM Service WHERE ServiceID = 'FILESTOREPROD09';
SELECT COUNT(*) AS n, SUM(CHAR_LENGTH(ServiceConnectString)) AS s FROM Service WHERE ServiceID = 'FILESTOREPROD09';

-- (3) Existenznachweis — NEXANS
SELECT COUNT(*) AS n FROM ( SELECT 1 FROM Message m JOIN Process p ON p.ProcessID = m.ProcessID JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID WHERE m.MessageID = @mid_nexans AND pm.MandantID = 'NEXANS' LIMIT 1 ) x;
SELECT COUNT(*) AS n FROM ( SELECT 1 FROM Message m JOIN Process p ON p.ProcessID = m.ProcessID JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID WHERE m.MessageID = @mid_nexans AND pm.MandantID = 'NEXANS' LIMIT 1 ) x;
SELECT COUNT(*) AS n FROM ( SELECT 1 FROM Message m JOIN Process p ON p.ProcessID = m.ProcessID JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID WHERE m.MessageID = @mid_nexans AND pm.MandantID = 'NEXANS' LIMIT 1 ) x;
SELECT COUNT(*) AS n FROM ( SELECT 1 FROM Message m JOIN Process p ON p.ProcessID = m.ProcessID JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID WHERE m.MessageID = @mid_nexans AND pm.MandantID = 'NEXANS' LIMIT 1 ) x;
SELECT COUNT(*) AS n FROM ( SELECT 1 FROM Message m JOIN Process p ON p.ProcessID = m.ProcessID JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID WHERE m.MessageID = @mid_nexans AND pm.MandantID = 'NEXANS' LIMIT 1 ) x;
SELECT COUNT(*) AS n FROM ( SELECT 1 FROM Message m JOIN Process p ON p.ProcessID = m.ProcessID JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID WHERE m.MessageID = @mid_nexans AND pm.MandantID = 'NEXANS' LIMIT 1 ) x;

-- (3) Existenznachweis — IBISGUS
SELECT COUNT(*) AS n FROM ( SELECT 1 FROM Message m JOIN Process p ON p.ProcessID = m.ProcessID JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID WHERE m.MessageID = @mid_ibisgus AND pm.MandantID = 'IBISGUS' LIMIT 1 ) x;
SELECT COUNT(*) AS n FROM ( SELECT 1 FROM Message m JOIN Process p ON p.ProcessID = m.ProcessID JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID WHERE m.MessageID = @mid_ibisgus AND pm.MandantID = 'IBISGUS' LIMIT 1 ) x;
SELECT COUNT(*) AS n FROM ( SELECT 1 FROM Message m JOIN Process p ON p.ProcessID = m.ProcessID JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID WHERE m.MessageID = @mid_ibisgus AND pm.MandantID = 'IBISGUS' LIMIT 1 ) x;
SELECT COUNT(*) AS n FROM ( SELECT 1 FROM Message m JOIN Process p ON p.ProcessID = m.ProcessID JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID WHERE m.MessageID = @mid_ibisgus AND pm.MandantID = 'IBISGUS' LIMIT 1 ) x;
SELECT COUNT(*) AS n FROM ( SELECT 1 FROM Message m JOIN Process p ON p.ProcessID = m.ProcessID JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID WHERE m.MessageID = @mid_ibisgus AND pm.MandantID = 'IBISGUS' LIMIT 1 ) x;
SELECT COUNT(*) AS n FROM ( SELECT 1 FROM Message m JOIN Process p ON p.ProcessID = m.ProcessID JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID WHERE m.MessageID = @mid_ibisgus AND pm.MandantID = 'IBISGUS' LIMIT 1 ) x;

-- ═══ M64a — Kandidatenauswahl fuer die Fehlerprotokolle (Fenster B) ═════════
-- Ohne Fenster wird ausdruecklich NICHT gefahren (Auftrag).
SELECT m.MessageStatus, COUNT(DISTINCT m.MessageID) AS nachrichten,
       COUNT(DISTINCT CASE WHEN mp.MessagePropertyName LIKE '%.Log.GUID'
                           THEN mp.MessageID END) AS mit_protokoll,
       COUNT(DISTINCT mp.MessagePropertyName)     AS familien
FROM Message m
LEFT JOIN MessageProperty mp ON mp.MessageID = m.MessageID
     AND mp.MessagePropertyName LIKE '%.Log.GUID'
WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  AND (m.MessageStatus LIKE 'ERROR\_%' ESCAPE '\\'
    OR m.MessageStatus = 'COMMIT_REJECTED')
GROUP BY m.MessageStatus ORDER BY nachrichten DESC;

SHOW PROFILES;

-- ═══ Abschlussnachweis ══════════════════════════════════════════════════════
SELECT @@global.read_only AS global_read_only_ende,
       @@read_only        AS read_only_ende,
       NOW()              AS serverzeit_ende;
