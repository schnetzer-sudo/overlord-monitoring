-- Messrunde 10c — Sitzung 1: M146, „Zuletzt aufgefallen" je Prozess statt je Nachricht
-- Auftrag:  „Nachbesserung Uebersicht: Farbe, Kurve, Verdichtung", 04.09.2026, Teil 3
-- Ergebnis: docs/dashboard.md Paragraf 8, docs/messungen-schritt10c.md
--
-- Ausschliesslich SELECT / SET / EXPLAIN. (Regel S1)
-- G1: keine Zugangsdaten, kein Hostname, keine ProcessID, keine MessageID.
--     Mandanten-IDs sind Codes und stehen in PROJEKTBESCHREIBUNG.md Paragraf 3.2.
--
-- Z1: Der Anker der Anwendungsuhr steht als Literal: 2025-12-30 04:09:47 (M9).
--     Die drei Fenster sind eimerausgerichtet wie Rollupzeitraum.fenster():
--       48H   2025-12-28 09:00:00  ..  2025-12-30 09:00:00
--       30T   2025-12-01 00:00:00  ..  2025-12-31 00:00:00
--       12M   2025-01-01 00:00:00  ..  2026-01-01 00:00:00
--
-- DIE FRAGE DIESER RUNDE: Der Block gruppiert seit heute nach Prozess. Damit
-- faellt der Grund weg, aus dem `IGNORE INDEX FOR ORDER BY` in M108 gewirkt hat
-- — dort verbot der Hinweis, den Zeitindex ZUR SORTIERUNG zu nehmen, und erst
-- das drehte den Plan auf MessageStatusIDX. Ueber einer Gruppierung gibt es
-- keine freie Sortierung mehr; der Optimierer kann den Zeitindex trotzdem fuer
-- den BEREICH waehlen. Gemessen werden deshalb vier Fassungen.
--
-- VERGLEICHSMASS ist M108/M145: konstante 22 bis 26 ms ueber alle Mandanten und
-- Fensterbreiten. Reisst eine Fassung die Groessenordnung, wird berichtet und
-- nicht geheilt.

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit, @@max_statement_time AS grenze;

SET max_statement_time = 60;
SET profiling = 1;
SET profiling_history_size = 400;

-- ── Vorprobe: wie viele Zeilen und wie viele PROZESSE stecken dahinter ──────
-- Ohne diese Zahl ist keine Laufzeit zu deuten: Der Aufwand der Gruppierung
-- haengt an der Zahl der auffaelligen Zeilen, die Zahl der Ausgabezeilen an der
-- Zahl der betroffenen Prozesse. Beide stehen vorher nirgends.
SELECT '=== M146-0 Vorprobe: Fehlerzeilen und betroffene Prozesse je Mandant ===' AS marke;
SELECT md.MandantID AS mandant,
       COUNT(*) AS fehlerzeilen_gesamtbestand,
       COUNT(DISTINCT m.ProcessID) AS betroffene_prozesse
FROM GlassfishDB.Mandant md
JOIN GlassfishDB.ProjectMandant pm ON pm.MandantID = md.MandantID
JOIN GlassfishDB.Process p ON p.ProjectID = pm.ProjectID
JOIN GlassfishDB.Message m ON m.ProcessID = p.ProcessID
WHERE (m.MessageStatus LIKE 'ERROR\_%' ESCAPE '\\'
       OR m.MessageStatus = 'COMMIT_REJECTED')
  AND md.MandantID IN ('NEXANS', 'SUTTONS', 'VOTG')
GROUP BY md.MandantID
ORDER BY md.MandantID;

-- ── Die vier Fassungen ──────────────────────────────────────────────────────
-- V1  ohne Hinweis
-- V2  IGNORE INDEX FOR ORDER BY (MessageLastUpdateIDX)   — der Hinweis von M108
-- V3  IGNORE INDEX (MessageLastUpdateIDX)                — das volle Verbot
-- V4  FORCE INDEX (MessageStatusIDX)                     — der staerkste Eingriff
--
-- Die Mandantenkette ist ein EXISTS und kein JOIN (ProjectMandant ist n:m; ein
-- JOIN vervielfachte die Zeilen und damit COUNT(*)). `Project` steht nicht in
-- der Kette — Befund 48.

SELECT '=== M146-1 NEXANS 48H, vier Fassungen ===' AS marke;

SELECT 'V1 ohne Hinweis' AS fassung;
SELECT m.ProcessID AS prozess, p.ProcessName AS name,
       COUNT(*) AS anzahl, MAX(m.MessageLastUpdate) AS zuletzt
FROM GlassfishDB.Message m
LEFT JOIN GlassfishDB.Process p ON p.ProcessID = m.ProcessID
WHERE (m.MessageStatus LIKE 'ERROR\_%' ESCAPE '\\'
       OR m.MessageStatus = 'COMMIT_REJECTED')
  AND m.MessageLastUpdate >= '2025-12-28 09:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 09:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process dp
              JOIN GlassfishDB.ProjectMandant dpm ON dpm.ProjectID = dp.ProjectID
              WHERE dp.ProcessID = m.ProcessID AND dpm.MandantID = 'NEXANS')
GROUP BY m.ProcessID, p.ProcessName
ORDER BY MAX(m.MessageLastUpdate) DESC, m.ProcessID DESC
LIMIT 10;

SELECT 'V2 IGNORE INDEX FOR ORDER BY' AS fassung;
SELECT m.ProcessID AS prozess, p.ProcessName AS name,
       COUNT(*) AS anzahl, MAX(m.MessageLastUpdate) AS zuletzt
FROM GlassfishDB.Message m IGNORE INDEX FOR ORDER BY (MessageLastUpdateIDX)
LEFT JOIN GlassfishDB.Process p ON p.ProcessID = m.ProcessID
WHERE (m.MessageStatus LIKE 'ERROR\_%' ESCAPE '\\'
       OR m.MessageStatus = 'COMMIT_REJECTED')
  AND m.MessageLastUpdate >= '2025-12-28 09:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 09:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process dp
              JOIN GlassfishDB.ProjectMandant dpm ON dpm.ProjectID = dp.ProjectID
              WHERE dp.ProcessID = m.ProcessID AND dpm.MandantID = 'NEXANS')
GROUP BY m.ProcessID, p.ProcessName
ORDER BY MAX(m.MessageLastUpdate) DESC, m.ProcessID DESC
LIMIT 10;

SELECT 'V3 IGNORE INDEX (voll)' AS fassung;
SELECT m.ProcessID AS prozess, p.ProcessName AS name,
       COUNT(*) AS anzahl, MAX(m.MessageLastUpdate) AS zuletzt
FROM GlassfishDB.Message m IGNORE INDEX (MessageLastUpdateIDX)
LEFT JOIN GlassfishDB.Process p ON p.ProcessID = m.ProcessID
WHERE (m.MessageStatus LIKE 'ERROR\_%' ESCAPE '\\'
       OR m.MessageStatus = 'COMMIT_REJECTED')
  AND m.MessageLastUpdate >= '2025-12-28 09:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 09:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process dp
              JOIN GlassfishDB.ProjectMandant dpm ON dpm.ProjectID = dp.ProjectID
              WHERE dp.ProcessID = m.ProcessID AND dpm.MandantID = 'NEXANS')
GROUP BY m.ProcessID, p.ProcessName
ORDER BY MAX(m.MessageLastUpdate) DESC, m.ProcessID DESC
LIMIT 10;

SELECT 'V4 FORCE INDEX (MessageStatusIDX)' AS fassung;
SELECT m.ProcessID AS prozess, p.ProcessName AS name,
       COUNT(*) AS anzahl, MAX(m.MessageLastUpdate) AS zuletzt
FROM GlassfishDB.Message m FORCE INDEX (MessageStatusIDX)
LEFT JOIN GlassfishDB.Process p ON p.ProcessID = m.ProcessID
WHERE (m.MessageStatus LIKE 'ERROR\_%' ESCAPE '\\'
       OR m.MessageStatus = 'COMMIT_REJECTED')
  AND m.MessageLastUpdate >= '2025-12-28 09:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 09:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process dp
              JOIN GlassfishDB.ProjectMandant dpm ON dpm.ProjectID = dp.ProjectID
              WHERE dp.ProcessID = m.ProcessID AND dpm.MandantID = 'NEXANS')
GROUP BY m.ProcessID, p.ProcessName
ORDER BY MAX(m.MessageLastUpdate) DESC, m.ProcessID DESC
LIMIT 10;

SELECT '=== M146-1 EXPLAIN je Fassung, NEXANS 48H ===' AS marke;

EXPLAIN SELECT m.ProcessID, p.ProcessName, COUNT(*), MAX(m.MessageLastUpdate)
FROM GlassfishDB.Message m
LEFT JOIN GlassfishDB.Process p ON p.ProcessID = m.ProcessID
WHERE (m.MessageStatus LIKE 'ERROR\_%' ESCAPE '\\' OR m.MessageStatus = 'COMMIT_REJECTED')
  AND m.MessageLastUpdate >= '2025-12-28 09:00:00' AND m.MessageLastUpdate < '2025-12-30 09:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process dp
              JOIN GlassfishDB.ProjectMandant dpm ON dpm.ProjectID = dp.ProjectID
              WHERE dp.ProcessID = m.ProcessID AND dpm.MandantID = 'NEXANS')
GROUP BY m.ProcessID, p.ProcessName
ORDER BY MAX(m.MessageLastUpdate) DESC, m.ProcessID DESC LIMIT 10;

EXPLAIN SELECT m.ProcessID, p.ProcessName, COUNT(*), MAX(m.MessageLastUpdate)
FROM GlassfishDB.Message m IGNORE INDEX FOR ORDER BY (MessageLastUpdateIDX)
LEFT JOIN GlassfishDB.Process p ON p.ProcessID = m.ProcessID
WHERE (m.MessageStatus LIKE 'ERROR\_%' ESCAPE '\\' OR m.MessageStatus = 'COMMIT_REJECTED')
  AND m.MessageLastUpdate >= '2025-12-28 09:00:00' AND m.MessageLastUpdate < '2025-12-30 09:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process dp
              JOIN GlassfishDB.ProjectMandant dpm ON dpm.ProjectID = dp.ProjectID
              WHERE dp.ProcessID = m.ProcessID AND dpm.MandantID = 'NEXANS')
GROUP BY m.ProcessID, p.ProcessName
ORDER BY MAX(m.MessageLastUpdate) DESC, m.ProcessID DESC LIMIT 10;

EXPLAIN SELECT m.ProcessID, p.ProcessName, COUNT(*), MAX(m.MessageLastUpdate)
FROM GlassfishDB.Message m IGNORE INDEX (MessageLastUpdateIDX)
LEFT JOIN GlassfishDB.Process p ON p.ProcessID = m.ProcessID
WHERE (m.MessageStatus LIKE 'ERROR\_%' ESCAPE '\\' OR m.MessageStatus = 'COMMIT_REJECTED')
  AND m.MessageLastUpdate >= '2025-12-28 09:00:00' AND m.MessageLastUpdate < '2025-12-30 09:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process dp
              JOIN GlassfishDB.ProjectMandant dpm ON dpm.ProjectID = dp.ProjectID
              WHERE dp.ProcessID = m.ProcessID AND dpm.MandantID = 'NEXANS')
GROUP BY m.ProcessID, p.ProcessName
ORDER BY MAX(m.MessageLastUpdate) DESC, m.ProcessID DESC LIMIT 10;

EXPLAIN SELECT m.ProcessID, p.ProcessName, COUNT(*), MAX(m.MessageLastUpdate)
FROM GlassfishDB.Message m FORCE INDEX (MessageStatusIDX)
LEFT JOIN GlassfishDB.Process p ON p.ProcessID = m.ProcessID
WHERE (m.MessageStatus LIKE 'ERROR\_%' ESCAPE '\\' OR m.MessageStatus = 'COMMIT_REJECTED')
  AND m.MessageLastUpdate >= '2025-12-28 09:00:00' AND m.MessageLastUpdate < '2025-12-30 09:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process dp
              JOIN GlassfishDB.ProjectMandant dpm ON dpm.ProjectID = dp.ProjectID
              WHERE dp.ProcessID = m.ProcessID AND dpm.MandantID = 'NEXANS')
GROUP BY m.ProcessID, p.ProcessName
ORDER BY MAX(m.MessageLastUpdate) DESC, m.ProcessID DESC LIMIT 10;

SELECT '=== M146-1 Profil ===' AS marke;
SHOW PROFILES;
