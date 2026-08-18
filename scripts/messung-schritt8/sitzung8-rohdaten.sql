-- Messung nach Regel L7 fuer die Statements des Rohdatenzugriffs (Schritt 8).
-- Erhoben am 18.08.2026. Ergebnis in docs/rohdaten-backend.md.
--
-- Warum diese Messung noetig ist: M58 hat drei Statements gemessen, die den
-- Mandantenfilter als JOIN ueber Process/ProjectMandant tragen. Der gebaute
-- Code verwendet stattdessen die im Projekt eingefuehrte EXISTS-Form (Regel M3,
-- Begruendung in NachrichtendetailRepository). Damit weicht der Zugriffspfad
-- moeglicherweise ab, und Regel L7 verlangt eine neue Messung. Hinzu kommen
-- zwei Statements, die M58 gar nicht kennt: die Aufloesung MIT Mandantenkette
-- und der Originaldateiname.
--
-- Die vier Statements sind WOERTLICH die von jOOQ gerenderten. Sie sind aus
-- dem laufenden Code abgegriffen; nur die Werte fuer MessageID und MandantID
-- sind durch Sitzungsvariablen ersetzt, weil in dieser Datei nach G1 keine
-- MessageID stehen darf.
--
-- Ausschliesslich SELECT / SET / EXPLAIN / SHOW. (Regel S1)
--
-- G1: keine MessageID, kein ServiceConnectString, kein Dateiname, kein Wert.
-- Jeder Pruefwert wird in der Sitzung selbst deterministisch hergeleitet.
-- Die Laufzeitmessungen laufen gegen eine ZAEHLENDE HUELLE um das echte
-- Statement, weil die echten Statements MessagePropertyValue und
-- ServiceConnectString liefern und diese Werte nicht auf den Bildschirm
-- duerfen. Der EXPLAIN laeuft gegen das ROHE Statement, unveraendert.
-- Dieselbe Abweichung wie bei M58.

SELECT @@global.read_only AS global_read_only_beginn;
SELECT NOW() AS serverzeit;
SET SESSION max_statement_time = 60;
SET profiling = 1;
SET profiling_history_size = 200;

-- ═══ Auswahl der Pruefnachricht ════════════════════════════════════════════
-- Wie bei M58: je Mandant die Nachricht aus Fenster A mit den MEISTEN
-- Artefaktverweisen (der teuerste Fall, L7), Gleichstand nach kleinster
-- MessageID. Zwei Mandanten als Kontrolle nach L15.

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

-- Eine Ablagenkennung, die zu diesen Nachrichten gehoert — hergeleitet, nicht
-- eingetragen. Nur die Kennung, nie der ServiceConnectString.
SET @ablage = (
  SELECT SUBSTRING_INDEX(mp.MessagePropertyValue, '|', 1)
  FROM MessageProperty mp
  WHERE mp.MessageID = @mid_nexans
    AND (mp.MessagePropertyName LIKE '%.Payload.GUID'
      OR mp.MessagePropertyName LIKE '%.Log.GUID')
  ORDER BY mp.MessageActionID, mp.MessagePropertyName
  LIMIT 1);

-- Der Schritt, an dem der erste Originaldateiname haengt.
SET @schritt = (
  SELECT MIN(mp.MessageActionID)
  FROM MessageProperty mp
  WHERE mp.MessageID = @mid_nexans
    AND mp.MessagePropertyName = 'FileReader.FileProperty.OriginalFilename');

-- Kontrolle: Zahlen, keine Kennungen.
SELECT (SELECT COUNT(*) FROM MessageProperty mp WHERE mp.MessageID = @mid_nexans
          AND (mp.MessagePropertyName LIKE '%.Payload.GUID'
            OR mp.MessagePropertyName LIKE '%.Log.GUID')) AS artefakte_nexans,
       (SELECT COUNT(*) FROM MessageProperty mp WHERE mp.MessageID = @mid_ibisgus
          AND (mp.MessagePropertyName LIKE '%.Payload.GUID'
            OR mp.MessagePropertyName LIKE '%.Log.GUID')) AS artefakte_ibisgus,
       CHAR_LENGTH(@ablage) AS ablagenkennung_zeichen,
       @schritt             AS schritt_mit_originalname;

-- ═══ (1) Artefaktliste — EXPLAIN gegen das ROHE Statement ══════════════════
EXPLAIN
select `GlassfishDB`.`MessageProperty`.`MessagePropertyName`, `GlassfishDB`.`MessageProperty`.`MessageActionID`, `GlassfishDB`.`MessageProperty`.`MessagePropertyValue` from `GlassfishDB`.`Message` join `GlassfishDB`.`MessageProperty` on `GlassfishDB`.`MessageProperty`.`MessageID` = `GlassfishDB`.`Message`.`MessageID` where (`GlassfishDB`.`Message`.`MessageID` = @mid_nexans and (`GlassfishDB`.`MessageProperty`.`MessagePropertyName` like '%.Payload.GUID' or `GlassfishDB`.`MessageProperty`.`MessagePropertyName` like '%.Log.GUID') and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) order by `GlassfishDB`.`MessageProperty`.`MessageActionID` asc, `GlassfishDB`.`MessageProperty`.`MessagePropertyName` asc;

-- ═══ (2) Existenznachweis — EXPLAIN ════════════════════════════════════════
EXPLAIN
select exists (select 1 as `one` from `GlassfishDB`.`Message` where (`GlassfishDB`.`Message`.`MessageID` = @mid_nexans and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS')))) AS vorhanden;

-- ═══ (3) Aufloesung der Ablagenkennung MIT Mandantenkette — EXPLAIN ════════
EXPLAIN
select `GlassfishDB`.`Service`.`ServiceConnectString` from `GlassfishDB`.`Service` where (`GlassfishDB`.`Service`.`ServiceID` = @ablage and exists (select 1 as `one` from `GlassfishDB`.`Message` where (`GlassfishDB`.`Message`.`MessageID` = @mid_nexans and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS')))));

-- ═══ (4) Originaldateiname — EXPLAIN ═══════════════════════════════════════
EXPLAIN
select `GlassfishDB`.`MessageProperty`.`MessagePropertyValue` from `GlassfishDB`.`Message` join `GlassfishDB`.`MessageProperty` on `GlassfishDB`.`MessageProperty`.`MessageID` = `GlassfishDB`.`Message`.`MessageID` where (`GlassfishDB`.`Message`.`MessageID` = @mid_nexans and `GlassfishDB`.`MessageProperty`.`MessagePropertyName` = 'FileReader.FileProperty.OriginalFilename' and `GlassfishDB`.`MessageProperty`.`MessageActionID` = @schritt and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS')));

-- ═══ Laufzeiten — Aufwaermlauf plus fuenf Messlaeufe je Abfrage ════════════
-- (1) NEXANS — zaehlende Huelle
SELECT COUNT(*) AS n, SUM(CHAR_LENGTH(v)) AS s FROM ( select `GlassfishDB`.`MessageProperty`.`MessagePropertyValue` AS v from `GlassfishDB`.`Message` join `GlassfishDB`.`MessageProperty` on `GlassfishDB`.`MessageProperty`.`MessageID` = `GlassfishDB`.`Message`.`MessageID` where (`GlassfishDB`.`Message`.`MessageID` = @mid_nexans and (`GlassfishDB`.`MessageProperty`.`MessagePropertyName` like '%.Payload.GUID' or `GlassfishDB`.`MessageProperty`.`MessagePropertyName` like '%.Log.GUID') and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) order by `GlassfishDB`.`MessageProperty`.`MessageActionID` asc, `GlassfishDB`.`MessageProperty`.`MessagePropertyName` asc ) x;
SELECT COUNT(*) AS n, SUM(CHAR_LENGTH(v)) AS s FROM ( select `GlassfishDB`.`MessageProperty`.`MessagePropertyValue` AS v from `GlassfishDB`.`Message` join `GlassfishDB`.`MessageProperty` on `GlassfishDB`.`MessageProperty`.`MessageID` = `GlassfishDB`.`Message`.`MessageID` where (`GlassfishDB`.`Message`.`MessageID` = @mid_nexans and (`GlassfishDB`.`MessageProperty`.`MessagePropertyName` like '%.Payload.GUID' or `GlassfishDB`.`MessageProperty`.`MessagePropertyName` like '%.Log.GUID') and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) order by `GlassfishDB`.`MessageProperty`.`MessageActionID` asc, `GlassfishDB`.`MessageProperty`.`MessagePropertyName` asc ) x;
SELECT COUNT(*) AS n, SUM(CHAR_LENGTH(v)) AS s FROM ( select `GlassfishDB`.`MessageProperty`.`MessagePropertyValue` AS v from `GlassfishDB`.`Message` join `GlassfishDB`.`MessageProperty` on `GlassfishDB`.`MessageProperty`.`MessageID` = `GlassfishDB`.`Message`.`MessageID` where (`GlassfishDB`.`Message`.`MessageID` = @mid_nexans and (`GlassfishDB`.`MessageProperty`.`MessagePropertyName` like '%.Payload.GUID' or `GlassfishDB`.`MessageProperty`.`MessagePropertyName` like '%.Log.GUID') and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) order by `GlassfishDB`.`MessageProperty`.`MessageActionID` asc, `GlassfishDB`.`MessageProperty`.`MessagePropertyName` asc ) x;
SELECT COUNT(*) AS n, SUM(CHAR_LENGTH(v)) AS s FROM ( select `GlassfishDB`.`MessageProperty`.`MessagePropertyValue` AS v from `GlassfishDB`.`Message` join `GlassfishDB`.`MessageProperty` on `GlassfishDB`.`MessageProperty`.`MessageID` = `GlassfishDB`.`Message`.`MessageID` where (`GlassfishDB`.`Message`.`MessageID` = @mid_nexans and (`GlassfishDB`.`MessageProperty`.`MessagePropertyName` like '%.Payload.GUID' or `GlassfishDB`.`MessageProperty`.`MessagePropertyName` like '%.Log.GUID') and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) order by `GlassfishDB`.`MessageProperty`.`MessageActionID` asc, `GlassfishDB`.`MessageProperty`.`MessagePropertyName` asc ) x;
SELECT COUNT(*) AS n, SUM(CHAR_LENGTH(v)) AS s FROM ( select `GlassfishDB`.`MessageProperty`.`MessagePropertyValue` AS v from `GlassfishDB`.`Message` join `GlassfishDB`.`MessageProperty` on `GlassfishDB`.`MessageProperty`.`MessageID` = `GlassfishDB`.`Message`.`MessageID` where (`GlassfishDB`.`Message`.`MessageID` = @mid_nexans and (`GlassfishDB`.`MessageProperty`.`MessagePropertyName` like '%.Payload.GUID' or `GlassfishDB`.`MessageProperty`.`MessagePropertyName` like '%.Log.GUID') and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) order by `GlassfishDB`.`MessageProperty`.`MessageActionID` asc, `GlassfishDB`.`MessageProperty`.`MessagePropertyName` asc ) x;
SELECT COUNT(*) AS n, SUM(CHAR_LENGTH(v)) AS s FROM ( select `GlassfishDB`.`MessageProperty`.`MessagePropertyValue` AS v from `GlassfishDB`.`Message` join `GlassfishDB`.`MessageProperty` on `GlassfishDB`.`MessageProperty`.`MessageID` = `GlassfishDB`.`Message`.`MessageID` where (`GlassfishDB`.`Message`.`MessageID` = @mid_nexans and (`GlassfishDB`.`MessageProperty`.`MessagePropertyName` like '%.Payload.GUID' or `GlassfishDB`.`MessageProperty`.`MessagePropertyName` like '%.Log.GUID') and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) order by `GlassfishDB`.`MessageProperty`.`MessageActionID` asc, `GlassfishDB`.`MessageProperty`.`MessagePropertyName` asc ) x;

-- (1) IBISGUS — Kontrolle nach L15
SELECT COUNT(*) AS n, SUM(CHAR_LENGTH(v)) AS s FROM ( select `GlassfishDB`.`MessageProperty`.`MessagePropertyValue` AS v from `GlassfishDB`.`Message` join `GlassfishDB`.`MessageProperty` on `GlassfishDB`.`MessageProperty`.`MessageID` = `GlassfishDB`.`Message`.`MessageID` where (`GlassfishDB`.`Message`.`MessageID` = @mid_ibisgus and (`GlassfishDB`.`MessageProperty`.`MessagePropertyName` like '%.Payload.GUID' or `GlassfishDB`.`MessageProperty`.`MessagePropertyName` like '%.Log.GUID') and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'IBISGUS'))) order by `GlassfishDB`.`MessageProperty`.`MessageActionID` asc, `GlassfishDB`.`MessageProperty`.`MessagePropertyName` asc ) x;
SELECT COUNT(*) AS n, SUM(CHAR_LENGTH(v)) AS s FROM ( select `GlassfishDB`.`MessageProperty`.`MessagePropertyValue` AS v from `GlassfishDB`.`Message` join `GlassfishDB`.`MessageProperty` on `GlassfishDB`.`MessageProperty`.`MessageID` = `GlassfishDB`.`Message`.`MessageID` where (`GlassfishDB`.`Message`.`MessageID` = @mid_ibisgus and (`GlassfishDB`.`MessageProperty`.`MessagePropertyName` like '%.Payload.GUID' or `GlassfishDB`.`MessageProperty`.`MessagePropertyName` like '%.Log.GUID') and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'IBISGUS'))) order by `GlassfishDB`.`MessageProperty`.`MessageActionID` asc, `GlassfishDB`.`MessageProperty`.`MessagePropertyName` asc ) x;
SELECT COUNT(*) AS n, SUM(CHAR_LENGTH(v)) AS s FROM ( select `GlassfishDB`.`MessageProperty`.`MessagePropertyValue` AS v from `GlassfishDB`.`Message` join `GlassfishDB`.`MessageProperty` on `GlassfishDB`.`MessageProperty`.`MessageID` = `GlassfishDB`.`Message`.`MessageID` where (`GlassfishDB`.`Message`.`MessageID` = @mid_ibisgus and (`GlassfishDB`.`MessageProperty`.`MessagePropertyName` like '%.Payload.GUID' or `GlassfishDB`.`MessageProperty`.`MessagePropertyName` like '%.Log.GUID') and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'IBISGUS'))) order by `GlassfishDB`.`MessageProperty`.`MessageActionID` asc, `GlassfishDB`.`MessageProperty`.`MessagePropertyName` asc ) x;
SELECT COUNT(*) AS n, SUM(CHAR_LENGTH(v)) AS s FROM ( select `GlassfishDB`.`MessageProperty`.`MessagePropertyValue` AS v from `GlassfishDB`.`Message` join `GlassfishDB`.`MessageProperty` on `GlassfishDB`.`MessageProperty`.`MessageID` = `GlassfishDB`.`Message`.`MessageID` where (`GlassfishDB`.`Message`.`MessageID` = @mid_ibisgus and (`GlassfishDB`.`MessageProperty`.`MessagePropertyName` like '%.Payload.GUID' or `GlassfishDB`.`MessageProperty`.`MessagePropertyName` like '%.Log.GUID') and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'IBISGUS'))) order by `GlassfishDB`.`MessageProperty`.`MessageActionID` asc, `GlassfishDB`.`MessageProperty`.`MessagePropertyName` asc ) x;
SELECT COUNT(*) AS n, SUM(CHAR_LENGTH(v)) AS s FROM ( select `GlassfishDB`.`MessageProperty`.`MessagePropertyValue` AS v from `GlassfishDB`.`Message` join `GlassfishDB`.`MessageProperty` on `GlassfishDB`.`MessageProperty`.`MessageID` = `GlassfishDB`.`Message`.`MessageID` where (`GlassfishDB`.`Message`.`MessageID` = @mid_ibisgus and (`GlassfishDB`.`MessageProperty`.`MessagePropertyName` like '%.Payload.GUID' or `GlassfishDB`.`MessageProperty`.`MessagePropertyName` like '%.Log.GUID') and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'IBISGUS'))) order by `GlassfishDB`.`MessageProperty`.`MessageActionID` asc, `GlassfishDB`.`MessageProperty`.`MessagePropertyName` asc ) x;
SELECT COUNT(*) AS n, SUM(CHAR_LENGTH(v)) AS s FROM ( select `GlassfishDB`.`MessageProperty`.`MessagePropertyValue` AS v from `GlassfishDB`.`Message` join `GlassfishDB`.`MessageProperty` on `GlassfishDB`.`MessageProperty`.`MessageID` = `GlassfishDB`.`Message`.`MessageID` where (`GlassfishDB`.`Message`.`MessageID` = @mid_ibisgus and (`GlassfishDB`.`MessageProperty`.`MessagePropertyName` like '%.Payload.GUID' or `GlassfishDB`.`MessageProperty`.`MessagePropertyName` like '%.Log.GUID') and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'IBISGUS'))) order by `GlassfishDB`.`MessageProperty`.`MessageActionID` asc, `GlassfishDB`.`MessageProperty`.`MessagePropertyName` asc ) x;

-- (2) Existenznachweis — NEXANS
SELECT (select exists (select 1 as `one` from `GlassfishDB`.`Message` where (`GlassfishDB`.`Message`.`MessageID` = @mid_nexans and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))))) AS vorhanden;
SELECT (select exists (select 1 as `one` from `GlassfishDB`.`Message` where (`GlassfishDB`.`Message`.`MessageID` = @mid_nexans and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))))) AS vorhanden;
SELECT (select exists (select 1 as `one` from `GlassfishDB`.`Message` where (`GlassfishDB`.`Message`.`MessageID` = @mid_nexans and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))))) AS vorhanden;
SELECT (select exists (select 1 as `one` from `GlassfishDB`.`Message` where (`GlassfishDB`.`Message`.`MessageID` = @mid_nexans and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))))) AS vorhanden;
SELECT (select exists (select 1 as `one` from `GlassfishDB`.`Message` where (`GlassfishDB`.`Message`.`MessageID` = @mid_nexans and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))))) AS vorhanden;
SELECT (select exists (select 1 as `one` from `GlassfishDB`.`Message` where (`GlassfishDB`.`Message`.`MessageID` = @mid_nexans and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))))) AS vorhanden;

-- (3) Aufloesung MIT Mandantenkette — zaehlende Huelle, der Wert bleibt verdeckt
SELECT COUNT(*) AS n, SUM(CHAR_LENGTH(v)) AS s FROM ( select `GlassfishDB`.`Service`.`ServiceConnectString` AS v from `GlassfishDB`.`Service` where (`GlassfishDB`.`Service`.`ServiceID` = @ablage and exists (select 1 as `one` from `GlassfishDB`.`Message` where (`GlassfishDB`.`Message`.`MessageID` = @mid_nexans and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))))) ) x;
SELECT COUNT(*) AS n, SUM(CHAR_LENGTH(v)) AS s FROM ( select `GlassfishDB`.`Service`.`ServiceConnectString` AS v from `GlassfishDB`.`Service` where (`GlassfishDB`.`Service`.`ServiceID` = @ablage and exists (select 1 as `one` from `GlassfishDB`.`Message` where (`GlassfishDB`.`Message`.`MessageID` = @mid_nexans and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))))) ) x;
SELECT COUNT(*) AS n, SUM(CHAR_LENGTH(v)) AS s FROM ( select `GlassfishDB`.`Service`.`ServiceConnectString` AS v from `GlassfishDB`.`Service` where (`GlassfishDB`.`Service`.`ServiceID` = @ablage and exists (select 1 as `one` from `GlassfishDB`.`Message` where (`GlassfishDB`.`Message`.`MessageID` = @mid_nexans and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))))) ) x;
SELECT COUNT(*) AS n, SUM(CHAR_LENGTH(v)) AS s FROM ( select `GlassfishDB`.`Service`.`ServiceConnectString` AS v from `GlassfishDB`.`Service` where (`GlassfishDB`.`Service`.`ServiceID` = @ablage and exists (select 1 as `one` from `GlassfishDB`.`Message` where (`GlassfishDB`.`Message`.`MessageID` = @mid_nexans and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))))) ) x;
SELECT COUNT(*) AS n, SUM(CHAR_LENGTH(v)) AS s FROM ( select `GlassfishDB`.`Service`.`ServiceConnectString` AS v from `GlassfishDB`.`Service` where (`GlassfishDB`.`Service`.`ServiceID` = @ablage and exists (select 1 as `one` from `GlassfishDB`.`Message` where (`GlassfishDB`.`Message`.`MessageID` = @mid_nexans and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))))) ) x;
SELECT COUNT(*) AS n, SUM(CHAR_LENGTH(v)) AS s FROM ( select `GlassfishDB`.`Service`.`ServiceConnectString` AS v from `GlassfishDB`.`Service` where (`GlassfishDB`.`Service`.`ServiceID` = @ablage and exists (select 1 as `one` from `GlassfishDB`.`Message` where (`GlassfishDB`.`Message`.`MessageID` = @mid_nexans and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))))) ) x;

-- (4) Originaldateiname — zaehlende Huelle, der Name bleibt verdeckt
SELECT COUNT(*) AS n, SUM(CHAR_LENGTH(v)) AS s FROM ( select `GlassfishDB`.`MessageProperty`.`MessagePropertyValue` AS v from `GlassfishDB`.`Message` join `GlassfishDB`.`MessageProperty` on `GlassfishDB`.`MessageProperty`.`MessageID` = `GlassfishDB`.`Message`.`MessageID` where (`GlassfishDB`.`Message`.`MessageID` = @mid_nexans and `GlassfishDB`.`MessageProperty`.`MessagePropertyName` = 'FileReader.FileProperty.OriginalFilename' and `GlassfishDB`.`MessageProperty`.`MessageActionID` = @schritt and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) ) x;
SELECT COUNT(*) AS n, SUM(CHAR_LENGTH(v)) AS s FROM ( select `GlassfishDB`.`MessageProperty`.`MessagePropertyValue` AS v from `GlassfishDB`.`Message` join `GlassfishDB`.`MessageProperty` on `GlassfishDB`.`MessageProperty`.`MessageID` = `GlassfishDB`.`Message`.`MessageID` where (`GlassfishDB`.`Message`.`MessageID` = @mid_nexans and `GlassfishDB`.`MessageProperty`.`MessagePropertyName` = 'FileReader.FileProperty.OriginalFilename' and `GlassfishDB`.`MessageProperty`.`MessageActionID` = @schritt and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) ) x;
SELECT COUNT(*) AS n, SUM(CHAR_LENGTH(v)) AS s FROM ( select `GlassfishDB`.`MessageProperty`.`MessagePropertyValue` AS v from `GlassfishDB`.`Message` join `GlassfishDB`.`MessageProperty` on `GlassfishDB`.`MessageProperty`.`MessageID` = `GlassfishDB`.`Message`.`MessageID` where (`GlassfishDB`.`Message`.`MessageID` = @mid_nexans and `GlassfishDB`.`MessageProperty`.`MessagePropertyName` = 'FileReader.FileProperty.OriginalFilename' and `GlassfishDB`.`MessageProperty`.`MessageActionID` = @schritt and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) ) x;
SELECT COUNT(*) AS n, SUM(CHAR_LENGTH(v)) AS s FROM ( select `GlassfishDB`.`MessageProperty`.`MessagePropertyValue` AS v from `GlassfishDB`.`Message` join `GlassfishDB`.`MessageProperty` on `GlassfishDB`.`MessageProperty`.`MessageID` = `GlassfishDB`.`Message`.`MessageID` where (`GlassfishDB`.`Message`.`MessageID` = @mid_nexans and `GlassfishDB`.`MessageProperty`.`MessagePropertyName` = 'FileReader.FileProperty.OriginalFilename' and `GlassfishDB`.`MessageProperty`.`MessageActionID` = @schritt and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) ) x;
SELECT COUNT(*) AS n, SUM(CHAR_LENGTH(v)) AS s FROM ( select `GlassfishDB`.`MessageProperty`.`MessagePropertyValue` AS v from `GlassfishDB`.`Message` join `GlassfishDB`.`MessageProperty` on `GlassfishDB`.`MessageProperty`.`MessageID` = `GlassfishDB`.`Message`.`MessageID` where (`GlassfishDB`.`Message`.`MessageID` = @mid_nexans and `GlassfishDB`.`MessageProperty`.`MessagePropertyName` = 'FileReader.FileProperty.OriginalFilename' and `GlassfishDB`.`MessageProperty`.`MessageActionID` = @schritt and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) ) x;
SELECT COUNT(*) AS n, SUM(CHAR_LENGTH(v)) AS s FROM ( select `GlassfishDB`.`MessageProperty`.`MessagePropertyValue` AS v from `GlassfishDB`.`Message` join `GlassfishDB`.`MessageProperty` on `GlassfishDB`.`MessageProperty`.`MessageID` = `GlassfishDB`.`Message`.`MessageID` where (`GlassfishDB`.`Message`.`MessageID` = @mid_nexans and `GlassfishDB`.`MessageProperty`.`MessagePropertyName` = 'FileReader.FileProperty.OriginalFilename' and `GlassfishDB`.`MessageProperty`.`MessageActionID` = @schritt and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process` join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID` where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))) ) x;

SHOW PROFILES;

-- ═══ Abschlussnachweis ══════════════════════════════════════════════════════
SELECT @@global.read_only AS global_read_only_ende,
       @@read_only        AS read_only_ende,
       NOW()              AS serverzeit_ende;
