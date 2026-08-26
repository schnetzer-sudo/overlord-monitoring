-- Messrunde M86-M92 — Sitzung 6e: M91, eine Zahl, die zweimal gleich herauskam
--
-- In Sitzung 6c ist der groesste kuratierte Partner von NEXANS in Fenster B mit 105.654
-- Nachrichten (58,6149 %) herausgekommen. In Sitzung 6d ist der Eimer "Richtung nicht
-- zugeordnet" mit EXAKT derselben Zahl herausgekommen — 105.654, 58,6149 %.
--
-- Zwei voneinander unabhaengige Gruppierungen, dieselbe Zahl. Das ist entweder Zufall
-- oder es heisst: Genau die Prozesse, die den groessten Partner tragen, sind die, denen
-- die Richtung fehlt. Bevor dieser Satz in die Ergebnisdatei geht, wird er geprueft.
--
-- Ausschliesslich SELECT / SET. (Regel S1)   Z1: alle Grenzen als Literal.
-- G1: keine Partnernamen, keine ProcessID — nur Anzahlen.

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;

SET max_statement_time = 60;
SET profiling_history_size = 100;
SET profiling = 1;

SELECT '=== 6e-1 NEXANS Fenster B: Partner x Richtung, gekreuzt ===' AS marke;
SELECT CASE WHEN c.process_id IS NULL                THEN 'ohne Katalogzeile'
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 'OFFEN'
            WHEN c.partner IS NULL OR c.partner = '' THEN 'GEPFLEGT ohne Partner'
            ELSE 'GEPFLEGT mit Partner' END          AS partnerlage,
       CASE WHEN c.process_id IS NULL                THEN 'ohne Katalogzeile'
            WHEN c.richtung IS NULL                  THEN 'ohne Richtung'
            ELSE c.richtung END                      AS richtungslage,
       COUNT(*)                                      AS nachrichten,
       COUNT(DISTINCT m.ProcessID)                   AS prozesse,
       ROUND(100.0 * COUNT(*) / SUM(COUNT(*)) OVER (), 4) AS anteil_prozent
FROM GlassfishDB.Message m
JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
WHERE pm.MandantID = 'NEXANS'
  AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
GROUP BY partnerlage, richtungslage
ORDER BY nachrichten DESC;

SELECT '=== 6e-2 Die elf NEXANS-Katalogzeilen ohne Richtung: wie viele Nachrichten? ===' AS marke;
SELECT COUNT(*)                    AS katalogzeilen_ohne_richtung,
       SUM(c.partner IS NOT NULL AND c.partner <> '') AS davon_mit_partner,
       COUNT(DISTINCT CASE WHEN c.partner IS NOT NULL AND c.partner <> ''
                           THEN c.partner END)        AS verschiedene_partner
FROM overlord_monitor.process_catalog c
JOIN GlassfishDB.Process        p  ON p.ProcessID  = c.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'NEXANS' AND c.richtung IS NULL;

SELECT '=== 6e-3 Gegenprobe ueber den Gesamtbestand (Fenster G, L9 als Bezugsgroesse) ===' AS marke;
SELECT CASE WHEN c.richtung IS NULL THEN 'ohne Richtung' ELSE 'mit Richtung' END AS lage,
       COUNT(*)                    AS nachrichten,
       COUNT(DISTINCT m.ProcessID) AS prozesse,
       ROUND(100.0 * COUNT(*) / SUM(COUNT(*)) OVER (), 4) AS anteil_prozent
FROM GlassfishDB.Message m
JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
JOIN overlord_monitor.process_catalog c ON c.process_id = m.ProcessID
WHERE pm.MandantID = 'NEXANS'
GROUP BY lage;

SELECT '=== 98 SHOW PROFILES ===' AS marke;
SHOW PROFILES;
SELECT '=== 99 Abschluss ===' AS marke;
SELECT @@global.read_only AS read_only_ende, NOW() AS serverzeit_ende;
