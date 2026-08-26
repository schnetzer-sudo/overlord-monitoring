-- Messrunde M86-M92 — Sitzung 6d: M91, die Verteilung nach Richtung (korrigiert)
--
-- Dieselbe Falle wie in Sitzung 6b: `process_catalog` traegt eine Spalte `richtung`, und
-- MariaDB loest GROUP BY zuerst gegen Tabellenspalten auf. Der Alias heisst hier deshalb
-- `richtung_kuratiert`. Bei NEXANS war das Ergebnis aus 6b zufaellig richtig (dort ist
-- kein Prozess OFFEN); belegt ist das erst mit dieser Sitzung.
--
-- Ausschliesslich SELECT / SET / EXPLAIN. (Regel S1)   Z1: alle Grenzen als Literal.
-- G1: EINGEHEND / AUSGEHEND ist Konfigurationsvokabular und darf stehen.

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;

SET max_statement_time = 60;
SET profiling_history_size = 100;
SET profiling = 1;

SELECT '=== M91-R0 Richtung im Katalog je Mandant (Bezugsgroesse) ===' AS marke;
SELECT pm.MandantID AS mandant,
       COUNT(*)                                       AS prozesse,
       SUM(c.richtung = 'EINGEHEND')                  AS eingehend,
       SUM(c.richtung = 'AUSGEHEND')                  AS ausgehend,
       SUM(c.process_id IS NOT NULL AND c.richtung IS NULL) AS katalogzeile_ohne_richtung,
       SUM(c.process_id IS NULL)                      AS ohne_katalogzeile
FROM GlassfishDB.ProjectMandant pm
JOIN GlassfishDB.Project pr ON pr.ProjectID = pm.ProjectID
JOIN GlassfishDB.Process p  ON p.ProjectID  = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
GROUP BY pm.MandantID
ORDER BY prozesse DESC;

SELECT '=== M91-R1 NEXANS Fenster A ===' AS marke;
SELECT richtung_kuratiert, SUM(anzahl) AS nachrichten,
       ROUND(100.0 * SUM(anzahl) / SUM(SUM(anzahl)) OVER (), 4) AS anteil_prozent
FROM (
  SELECT CASE WHEN c.process_id IS NULL         THEN 'nicht zugeordnet'
              WHEN c.pflegestatus = 'OFFEN'     THEN 'nicht zugeordnet'
              WHEN c.richtung IS NULL           THEN 'nicht zugeordnet'
              ELSE c.richtung END AS richtung_kuratiert,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'NEXANS'
    AND m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY richtung_kuratiert
) t GROUP BY richtung_kuratiert ORDER BY nachrichten DESC;

SELECT '=== M91-R2 NEXANS Fenster B ===' AS marke;
SELECT richtung_kuratiert, SUM(anzahl) AS nachrichten,
       ROUND(100.0 * SUM(anzahl) / SUM(SUM(anzahl)) OVER (), 4) AS anteil_prozent
FROM (
  SELECT CASE WHEN c.process_id IS NULL         THEN 'nicht zugeordnet'
              WHEN c.pflegestatus = 'OFFEN'     THEN 'nicht zugeordnet'
              WHEN c.richtung IS NULL           THEN 'nicht zugeordnet'
              ELSE c.richtung END AS richtung_kuratiert,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'NEXANS'
    AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY richtung_kuratiert
) t GROUP BY richtung_kuratiert ORDER BY nachrichten DESC;

SELECT '=== M91-R3 SUTTONS Fenster B ===' AS marke;
SELECT richtung_kuratiert, SUM(anzahl) AS nachrichten,
       ROUND(100.0 * SUM(anzahl) / SUM(SUM(anzahl)) OVER (), 4) AS anteil_prozent
FROM (
  SELECT CASE WHEN c.process_id IS NULL         THEN 'nicht zugeordnet'
              WHEN c.pflegestatus = 'OFFEN'     THEN 'nicht zugeordnet'
              WHEN c.richtung IS NULL           THEN 'nicht zugeordnet'
              ELSE c.richtung END AS richtung_kuratiert,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'SUTTONS'
    AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY richtung_kuratiert
) t GROUP BY richtung_kuratiert ORDER BY nachrichten DESC;

SELECT '=== M91-R4 VOTG Fenster B ===' AS marke;
SELECT richtung_kuratiert, SUM(anzahl) AS nachrichten,
       ROUND(100.0 * SUM(anzahl) / SUM(SUM(anzahl)) OVER (), 4) AS anteil_prozent
FROM (
  SELECT CASE WHEN c.process_id IS NULL         THEN 'nicht zugeordnet'
              WHEN c.pflegestatus = 'OFFEN'     THEN 'nicht zugeordnet'
              WHEN c.richtung IS NULL           THEN 'nicht zugeordnet'
              ELSE c.richtung END AS richtung_kuratiert,
         COUNT(*) AS anzahl
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
  WHERE pm.MandantID = 'VOTG'
    AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY richtung_kuratiert
) t GROUP BY richtung_kuratiert ORDER BY nachrichten DESC;

SELECT '=== M91-R5 Der groesste Prozess je Mandant, gegen offenen Punkt 39 (L9) ===' AS marke;
-- L9: voller Durchlauf ueber Message. Begruendung: Offener Punkt 39 behauptet "ein Prozess
-- haelt 44,08 % aller Nachrichten". Diese Zahl ist die Bezugsgroesse, gegen die M91 zu lesen
-- ist — sie ist ohne vollen Durchlauf nicht zu bekommen. Kein Vorbild fuer Anwendungscode.
SELECT mandant,
       COUNT(*)                                            AS prozesse_mit_nachrichten,
       MAX(je_prozess)                                     AS groesster_prozess,
       SUM(je_prozess)                                     AS nachrichten_gesamt,
       ROUND(100.0 * MAX(je_prozess) / SUM(je_prozess), 4) AS anteil_im_mandanten,
       ROUND(100.0 * MAX(je_prozess) / 3341519, 4)         AS anteil_am_bestand
FROM (
  SELECT pm.MandantID AS mandant, m.ProcessID AS pid, COUNT(*) AS je_prozess
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  GROUP BY pm.MandantID, m.ProcessID
) t
GROUP BY mandant
ORDER BY nachrichten_gesamt DESC;

SELECT '=== 98 SHOW PROFILES ===' AS marke;
SHOW PROFILES;
SELECT '=== 99 Abschluss ===' AS marke;
SELECT @@global.read_only AS read_only_ende, NOW() AS serverzeit_ende;
