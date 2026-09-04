-- Messrunde 10c — M146: „Zuletzt aufgefallen" je Prozess statt je Nachricht
-- Mandant:  NEXANS
-- Auftrag:  „Nachbesserung Uebersicht", 04.09.2026, Teil 3
-- Erzeugt von scripts/messung-schritt10c-verdichtung/erzeuge-s2.py
--
-- Ausschliesslich SELECT / SET / EXPLAIN. (Regel S1)
-- G1: keine Zugangsdaten, kein Hostname. Mandanten-IDs sind Codes und stehen in
--     PROJEKTBESCHREIBUNG.md Paragraf 3.2.
-- Z1: Anker 2025-12-30 04:09:47 (M9); die drei Fenster sind eimerausgerichtet
--     wie Rollupzeitraum.fenster().
--
-- BAUFORM WIE M108: ein Aufwaermlauf, dann die beste von fuenf. BEIDE Fassungen
-- tragen denselben Indexhinweis, damit der Vergleich die GRUPPIERUNG misst und
-- nicht den Hinweis.

SELECT '=== 00 read_only ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;
SET max_statement_time = 60;
SET profiling = 1;
SET profiling_history_size = 100;


SELECT '--- NEXANS 48H alt (6 Laeufe) ---' AS marke;
SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID, s.SOSName
FROM GlassfishDB.Message m IGNORE INDEX FOR ORDER BY (MessageLastUpdateIDX)
LEFT JOIN GlassfishDB.SOS s ON s.SOSID = m.SOSID
  WHERE (m.MessageStatus LIKE 'ERROR\_%' ESCAPE '\\'
         OR m.MessageStatus = 'COMMIT_REJECTED')
  AND m.MessageLastUpdate >= '2025-12-28 09:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 09:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process dp
              JOIN GlassfishDB.ProjectMandant dpm ON dpm.ProjectID = dp.ProjectID
              WHERE dp.ProcessID = m.ProcessID AND dpm.MandantID = 'NEXANS')
ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
LIMIT 10;
SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID, s.SOSName
FROM GlassfishDB.Message m IGNORE INDEX FOR ORDER BY (MessageLastUpdateIDX)
LEFT JOIN GlassfishDB.SOS s ON s.SOSID = m.SOSID
  WHERE (m.MessageStatus LIKE 'ERROR\_%' ESCAPE '\\'
         OR m.MessageStatus = 'COMMIT_REJECTED')
  AND m.MessageLastUpdate >= '2025-12-28 09:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 09:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process dp
              JOIN GlassfishDB.ProjectMandant dpm ON dpm.ProjectID = dp.ProjectID
              WHERE dp.ProcessID = m.ProcessID AND dpm.MandantID = 'NEXANS')
ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
LIMIT 10;
SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID, s.SOSName
FROM GlassfishDB.Message m IGNORE INDEX FOR ORDER BY (MessageLastUpdateIDX)
LEFT JOIN GlassfishDB.SOS s ON s.SOSID = m.SOSID
  WHERE (m.MessageStatus LIKE 'ERROR\_%' ESCAPE '\\'
         OR m.MessageStatus = 'COMMIT_REJECTED')
  AND m.MessageLastUpdate >= '2025-12-28 09:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 09:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process dp
              JOIN GlassfishDB.ProjectMandant dpm ON dpm.ProjectID = dp.ProjectID
              WHERE dp.ProcessID = m.ProcessID AND dpm.MandantID = 'NEXANS')
ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
LIMIT 10;
SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID, s.SOSName
FROM GlassfishDB.Message m IGNORE INDEX FOR ORDER BY (MessageLastUpdateIDX)
LEFT JOIN GlassfishDB.SOS s ON s.SOSID = m.SOSID
  WHERE (m.MessageStatus LIKE 'ERROR\_%' ESCAPE '\\'
         OR m.MessageStatus = 'COMMIT_REJECTED')
  AND m.MessageLastUpdate >= '2025-12-28 09:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 09:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process dp
              JOIN GlassfishDB.ProjectMandant dpm ON dpm.ProjectID = dp.ProjectID
              WHERE dp.ProcessID = m.ProcessID AND dpm.MandantID = 'NEXANS')
ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
LIMIT 10;
SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID, s.SOSName
FROM GlassfishDB.Message m IGNORE INDEX FOR ORDER BY (MessageLastUpdateIDX)
LEFT JOIN GlassfishDB.SOS s ON s.SOSID = m.SOSID
  WHERE (m.MessageStatus LIKE 'ERROR\_%' ESCAPE '\\'
         OR m.MessageStatus = 'COMMIT_REJECTED')
  AND m.MessageLastUpdate >= '2025-12-28 09:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 09:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process dp
              JOIN GlassfishDB.ProjectMandant dpm ON dpm.ProjectID = dp.ProjectID
              WHERE dp.ProcessID = m.ProcessID AND dpm.MandantID = 'NEXANS')
ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
LIMIT 10;
SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID, s.SOSName
FROM GlassfishDB.Message m IGNORE INDEX FOR ORDER BY (MessageLastUpdateIDX)
LEFT JOIN GlassfishDB.SOS s ON s.SOSID = m.SOSID
  WHERE (m.MessageStatus LIKE 'ERROR\_%' ESCAPE '\\'
         OR m.MessageStatus = 'COMMIT_REJECTED')
  AND m.MessageLastUpdate >= '2025-12-28 09:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 09:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process dp
              JOIN GlassfishDB.ProjectMandant dpm ON dpm.ProjectID = dp.ProjectID
              WHERE dp.ProcessID = m.ProcessID AND dpm.MandantID = 'NEXANS')
ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
LIMIT 10;

SELECT '--- NEXANS 48H neu (6 Laeufe) ---' AS marke;
SELECT m.ProcessID, p.ProcessName, COUNT(*), MAX(m.MessageLastUpdate)
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
SELECT m.ProcessID, p.ProcessName, COUNT(*), MAX(m.MessageLastUpdate)
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
SELECT m.ProcessID, p.ProcessName, COUNT(*), MAX(m.MessageLastUpdate)
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
SELECT m.ProcessID, p.ProcessName, COUNT(*), MAX(m.MessageLastUpdate)
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
SELECT m.ProcessID, p.ProcessName, COUNT(*), MAX(m.MessageLastUpdate)
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
SELECT m.ProcessID, p.ProcessName, COUNT(*), MAX(m.MessageLastUpdate)
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

SELECT '--- NEXANS 30T alt (6 Laeufe) ---' AS marke;
SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID, s.SOSName
FROM GlassfishDB.Message m IGNORE INDEX FOR ORDER BY (MessageLastUpdateIDX)
LEFT JOIN GlassfishDB.SOS s ON s.SOSID = m.SOSID
  WHERE (m.MessageStatus LIKE 'ERROR\_%' ESCAPE '\\'
         OR m.MessageStatus = 'COMMIT_REJECTED')
  AND m.MessageLastUpdate >= '2025-12-01 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-31 00:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process dp
              JOIN GlassfishDB.ProjectMandant dpm ON dpm.ProjectID = dp.ProjectID
              WHERE dp.ProcessID = m.ProcessID AND dpm.MandantID = 'NEXANS')
ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
LIMIT 10;
SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID, s.SOSName
FROM GlassfishDB.Message m IGNORE INDEX FOR ORDER BY (MessageLastUpdateIDX)
LEFT JOIN GlassfishDB.SOS s ON s.SOSID = m.SOSID
  WHERE (m.MessageStatus LIKE 'ERROR\_%' ESCAPE '\\'
         OR m.MessageStatus = 'COMMIT_REJECTED')
  AND m.MessageLastUpdate >= '2025-12-01 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-31 00:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process dp
              JOIN GlassfishDB.ProjectMandant dpm ON dpm.ProjectID = dp.ProjectID
              WHERE dp.ProcessID = m.ProcessID AND dpm.MandantID = 'NEXANS')
ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
LIMIT 10;
SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID, s.SOSName
FROM GlassfishDB.Message m IGNORE INDEX FOR ORDER BY (MessageLastUpdateIDX)
LEFT JOIN GlassfishDB.SOS s ON s.SOSID = m.SOSID
  WHERE (m.MessageStatus LIKE 'ERROR\_%' ESCAPE '\\'
         OR m.MessageStatus = 'COMMIT_REJECTED')
  AND m.MessageLastUpdate >= '2025-12-01 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-31 00:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process dp
              JOIN GlassfishDB.ProjectMandant dpm ON dpm.ProjectID = dp.ProjectID
              WHERE dp.ProcessID = m.ProcessID AND dpm.MandantID = 'NEXANS')
ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
LIMIT 10;
SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID, s.SOSName
FROM GlassfishDB.Message m IGNORE INDEX FOR ORDER BY (MessageLastUpdateIDX)
LEFT JOIN GlassfishDB.SOS s ON s.SOSID = m.SOSID
  WHERE (m.MessageStatus LIKE 'ERROR\_%' ESCAPE '\\'
         OR m.MessageStatus = 'COMMIT_REJECTED')
  AND m.MessageLastUpdate >= '2025-12-01 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-31 00:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process dp
              JOIN GlassfishDB.ProjectMandant dpm ON dpm.ProjectID = dp.ProjectID
              WHERE dp.ProcessID = m.ProcessID AND dpm.MandantID = 'NEXANS')
ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
LIMIT 10;
SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID, s.SOSName
FROM GlassfishDB.Message m IGNORE INDEX FOR ORDER BY (MessageLastUpdateIDX)
LEFT JOIN GlassfishDB.SOS s ON s.SOSID = m.SOSID
  WHERE (m.MessageStatus LIKE 'ERROR\_%' ESCAPE '\\'
         OR m.MessageStatus = 'COMMIT_REJECTED')
  AND m.MessageLastUpdate >= '2025-12-01 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-31 00:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process dp
              JOIN GlassfishDB.ProjectMandant dpm ON dpm.ProjectID = dp.ProjectID
              WHERE dp.ProcessID = m.ProcessID AND dpm.MandantID = 'NEXANS')
ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
LIMIT 10;
SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID, s.SOSName
FROM GlassfishDB.Message m IGNORE INDEX FOR ORDER BY (MessageLastUpdateIDX)
LEFT JOIN GlassfishDB.SOS s ON s.SOSID = m.SOSID
  WHERE (m.MessageStatus LIKE 'ERROR\_%' ESCAPE '\\'
         OR m.MessageStatus = 'COMMIT_REJECTED')
  AND m.MessageLastUpdate >= '2025-12-01 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-31 00:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process dp
              JOIN GlassfishDB.ProjectMandant dpm ON dpm.ProjectID = dp.ProjectID
              WHERE dp.ProcessID = m.ProcessID AND dpm.MandantID = 'NEXANS')
ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
LIMIT 10;

SELECT '--- NEXANS 30T neu (6 Laeufe) ---' AS marke;
SELECT m.ProcessID, p.ProcessName, COUNT(*), MAX(m.MessageLastUpdate)
FROM GlassfishDB.Message m IGNORE INDEX FOR ORDER BY (MessageLastUpdateIDX)
LEFT JOIN GlassfishDB.Process p ON p.ProcessID = m.ProcessID
  WHERE (m.MessageStatus LIKE 'ERROR\_%' ESCAPE '\\'
         OR m.MessageStatus = 'COMMIT_REJECTED')
  AND m.MessageLastUpdate >= '2025-12-01 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-31 00:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process dp
              JOIN GlassfishDB.ProjectMandant dpm ON dpm.ProjectID = dp.ProjectID
              WHERE dp.ProcessID = m.ProcessID AND dpm.MandantID = 'NEXANS')
GROUP BY m.ProcessID, p.ProcessName
ORDER BY MAX(m.MessageLastUpdate) DESC, m.ProcessID DESC
LIMIT 10;
SELECT m.ProcessID, p.ProcessName, COUNT(*), MAX(m.MessageLastUpdate)
FROM GlassfishDB.Message m IGNORE INDEX FOR ORDER BY (MessageLastUpdateIDX)
LEFT JOIN GlassfishDB.Process p ON p.ProcessID = m.ProcessID
  WHERE (m.MessageStatus LIKE 'ERROR\_%' ESCAPE '\\'
         OR m.MessageStatus = 'COMMIT_REJECTED')
  AND m.MessageLastUpdate >= '2025-12-01 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-31 00:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process dp
              JOIN GlassfishDB.ProjectMandant dpm ON dpm.ProjectID = dp.ProjectID
              WHERE dp.ProcessID = m.ProcessID AND dpm.MandantID = 'NEXANS')
GROUP BY m.ProcessID, p.ProcessName
ORDER BY MAX(m.MessageLastUpdate) DESC, m.ProcessID DESC
LIMIT 10;
SELECT m.ProcessID, p.ProcessName, COUNT(*), MAX(m.MessageLastUpdate)
FROM GlassfishDB.Message m IGNORE INDEX FOR ORDER BY (MessageLastUpdateIDX)
LEFT JOIN GlassfishDB.Process p ON p.ProcessID = m.ProcessID
  WHERE (m.MessageStatus LIKE 'ERROR\_%' ESCAPE '\\'
         OR m.MessageStatus = 'COMMIT_REJECTED')
  AND m.MessageLastUpdate >= '2025-12-01 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-31 00:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process dp
              JOIN GlassfishDB.ProjectMandant dpm ON dpm.ProjectID = dp.ProjectID
              WHERE dp.ProcessID = m.ProcessID AND dpm.MandantID = 'NEXANS')
GROUP BY m.ProcessID, p.ProcessName
ORDER BY MAX(m.MessageLastUpdate) DESC, m.ProcessID DESC
LIMIT 10;
SELECT m.ProcessID, p.ProcessName, COUNT(*), MAX(m.MessageLastUpdate)
FROM GlassfishDB.Message m IGNORE INDEX FOR ORDER BY (MessageLastUpdateIDX)
LEFT JOIN GlassfishDB.Process p ON p.ProcessID = m.ProcessID
  WHERE (m.MessageStatus LIKE 'ERROR\_%' ESCAPE '\\'
         OR m.MessageStatus = 'COMMIT_REJECTED')
  AND m.MessageLastUpdate >= '2025-12-01 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-31 00:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process dp
              JOIN GlassfishDB.ProjectMandant dpm ON dpm.ProjectID = dp.ProjectID
              WHERE dp.ProcessID = m.ProcessID AND dpm.MandantID = 'NEXANS')
GROUP BY m.ProcessID, p.ProcessName
ORDER BY MAX(m.MessageLastUpdate) DESC, m.ProcessID DESC
LIMIT 10;
SELECT m.ProcessID, p.ProcessName, COUNT(*), MAX(m.MessageLastUpdate)
FROM GlassfishDB.Message m IGNORE INDEX FOR ORDER BY (MessageLastUpdateIDX)
LEFT JOIN GlassfishDB.Process p ON p.ProcessID = m.ProcessID
  WHERE (m.MessageStatus LIKE 'ERROR\_%' ESCAPE '\\'
         OR m.MessageStatus = 'COMMIT_REJECTED')
  AND m.MessageLastUpdate >= '2025-12-01 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-31 00:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process dp
              JOIN GlassfishDB.ProjectMandant dpm ON dpm.ProjectID = dp.ProjectID
              WHERE dp.ProcessID = m.ProcessID AND dpm.MandantID = 'NEXANS')
GROUP BY m.ProcessID, p.ProcessName
ORDER BY MAX(m.MessageLastUpdate) DESC, m.ProcessID DESC
LIMIT 10;
SELECT m.ProcessID, p.ProcessName, COUNT(*), MAX(m.MessageLastUpdate)
FROM GlassfishDB.Message m IGNORE INDEX FOR ORDER BY (MessageLastUpdateIDX)
LEFT JOIN GlassfishDB.Process p ON p.ProcessID = m.ProcessID
  WHERE (m.MessageStatus LIKE 'ERROR\_%' ESCAPE '\\'
         OR m.MessageStatus = 'COMMIT_REJECTED')
  AND m.MessageLastUpdate >= '2025-12-01 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-31 00:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process dp
              JOIN GlassfishDB.ProjectMandant dpm ON dpm.ProjectID = dp.ProjectID
              WHERE dp.ProcessID = m.ProcessID AND dpm.MandantID = 'NEXANS')
GROUP BY m.ProcessID, p.ProcessName
ORDER BY MAX(m.MessageLastUpdate) DESC, m.ProcessID DESC
LIMIT 10;

SELECT '--- NEXANS 12M alt (6 Laeufe) ---' AS marke;
SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID, s.SOSName
FROM GlassfishDB.Message m IGNORE INDEX FOR ORDER BY (MessageLastUpdateIDX)
LEFT JOIN GlassfishDB.SOS s ON s.SOSID = m.SOSID
  WHERE (m.MessageStatus LIKE 'ERROR\_%' ESCAPE '\\'
         OR m.MessageStatus = 'COMMIT_REJECTED')
  AND m.MessageLastUpdate >= '2025-01-01 00:00:00'
  AND m.MessageLastUpdate <  '2026-01-01 00:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process dp
              JOIN GlassfishDB.ProjectMandant dpm ON dpm.ProjectID = dp.ProjectID
              WHERE dp.ProcessID = m.ProcessID AND dpm.MandantID = 'NEXANS')
ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
LIMIT 10;
SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID, s.SOSName
FROM GlassfishDB.Message m IGNORE INDEX FOR ORDER BY (MessageLastUpdateIDX)
LEFT JOIN GlassfishDB.SOS s ON s.SOSID = m.SOSID
  WHERE (m.MessageStatus LIKE 'ERROR\_%' ESCAPE '\\'
         OR m.MessageStatus = 'COMMIT_REJECTED')
  AND m.MessageLastUpdate >= '2025-01-01 00:00:00'
  AND m.MessageLastUpdate <  '2026-01-01 00:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process dp
              JOIN GlassfishDB.ProjectMandant dpm ON dpm.ProjectID = dp.ProjectID
              WHERE dp.ProcessID = m.ProcessID AND dpm.MandantID = 'NEXANS')
ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
LIMIT 10;
SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID, s.SOSName
FROM GlassfishDB.Message m IGNORE INDEX FOR ORDER BY (MessageLastUpdateIDX)
LEFT JOIN GlassfishDB.SOS s ON s.SOSID = m.SOSID
  WHERE (m.MessageStatus LIKE 'ERROR\_%' ESCAPE '\\'
         OR m.MessageStatus = 'COMMIT_REJECTED')
  AND m.MessageLastUpdate >= '2025-01-01 00:00:00'
  AND m.MessageLastUpdate <  '2026-01-01 00:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process dp
              JOIN GlassfishDB.ProjectMandant dpm ON dpm.ProjectID = dp.ProjectID
              WHERE dp.ProcessID = m.ProcessID AND dpm.MandantID = 'NEXANS')
ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
LIMIT 10;
SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID, s.SOSName
FROM GlassfishDB.Message m IGNORE INDEX FOR ORDER BY (MessageLastUpdateIDX)
LEFT JOIN GlassfishDB.SOS s ON s.SOSID = m.SOSID
  WHERE (m.MessageStatus LIKE 'ERROR\_%' ESCAPE '\\'
         OR m.MessageStatus = 'COMMIT_REJECTED')
  AND m.MessageLastUpdate >= '2025-01-01 00:00:00'
  AND m.MessageLastUpdate <  '2026-01-01 00:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process dp
              JOIN GlassfishDB.ProjectMandant dpm ON dpm.ProjectID = dp.ProjectID
              WHERE dp.ProcessID = m.ProcessID AND dpm.MandantID = 'NEXANS')
ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
LIMIT 10;
SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID, s.SOSName
FROM GlassfishDB.Message m IGNORE INDEX FOR ORDER BY (MessageLastUpdateIDX)
LEFT JOIN GlassfishDB.SOS s ON s.SOSID = m.SOSID
  WHERE (m.MessageStatus LIKE 'ERROR\_%' ESCAPE '\\'
         OR m.MessageStatus = 'COMMIT_REJECTED')
  AND m.MessageLastUpdate >= '2025-01-01 00:00:00'
  AND m.MessageLastUpdate <  '2026-01-01 00:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process dp
              JOIN GlassfishDB.ProjectMandant dpm ON dpm.ProjectID = dp.ProjectID
              WHERE dp.ProcessID = m.ProcessID AND dpm.MandantID = 'NEXANS')
ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
LIMIT 10;
SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID, s.SOSName
FROM GlassfishDB.Message m IGNORE INDEX FOR ORDER BY (MessageLastUpdateIDX)
LEFT JOIN GlassfishDB.SOS s ON s.SOSID = m.SOSID
  WHERE (m.MessageStatus LIKE 'ERROR\_%' ESCAPE '\\'
         OR m.MessageStatus = 'COMMIT_REJECTED')
  AND m.MessageLastUpdate >= '2025-01-01 00:00:00'
  AND m.MessageLastUpdate <  '2026-01-01 00:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process dp
              JOIN GlassfishDB.ProjectMandant dpm ON dpm.ProjectID = dp.ProjectID
              WHERE dp.ProcessID = m.ProcessID AND dpm.MandantID = 'NEXANS')
ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
LIMIT 10;

SELECT '--- NEXANS 12M neu (6 Laeufe) ---' AS marke;
SELECT m.ProcessID, p.ProcessName, COUNT(*), MAX(m.MessageLastUpdate)
FROM GlassfishDB.Message m IGNORE INDEX FOR ORDER BY (MessageLastUpdateIDX)
LEFT JOIN GlassfishDB.Process p ON p.ProcessID = m.ProcessID
  WHERE (m.MessageStatus LIKE 'ERROR\_%' ESCAPE '\\'
         OR m.MessageStatus = 'COMMIT_REJECTED')
  AND m.MessageLastUpdate >= '2025-01-01 00:00:00'
  AND m.MessageLastUpdate <  '2026-01-01 00:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process dp
              JOIN GlassfishDB.ProjectMandant dpm ON dpm.ProjectID = dp.ProjectID
              WHERE dp.ProcessID = m.ProcessID AND dpm.MandantID = 'NEXANS')
GROUP BY m.ProcessID, p.ProcessName
ORDER BY MAX(m.MessageLastUpdate) DESC, m.ProcessID DESC
LIMIT 10;
SELECT m.ProcessID, p.ProcessName, COUNT(*), MAX(m.MessageLastUpdate)
FROM GlassfishDB.Message m IGNORE INDEX FOR ORDER BY (MessageLastUpdateIDX)
LEFT JOIN GlassfishDB.Process p ON p.ProcessID = m.ProcessID
  WHERE (m.MessageStatus LIKE 'ERROR\_%' ESCAPE '\\'
         OR m.MessageStatus = 'COMMIT_REJECTED')
  AND m.MessageLastUpdate >= '2025-01-01 00:00:00'
  AND m.MessageLastUpdate <  '2026-01-01 00:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process dp
              JOIN GlassfishDB.ProjectMandant dpm ON dpm.ProjectID = dp.ProjectID
              WHERE dp.ProcessID = m.ProcessID AND dpm.MandantID = 'NEXANS')
GROUP BY m.ProcessID, p.ProcessName
ORDER BY MAX(m.MessageLastUpdate) DESC, m.ProcessID DESC
LIMIT 10;
SELECT m.ProcessID, p.ProcessName, COUNT(*), MAX(m.MessageLastUpdate)
FROM GlassfishDB.Message m IGNORE INDEX FOR ORDER BY (MessageLastUpdateIDX)
LEFT JOIN GlassfishDB.Process p ON p.ProcessID = m.ProcessID
  WHERE (m.MessageStatus LIKE 'ERROR\_%' ESCAPE '\\'
         OR m.MessageStatus = 'COMMIT_REJECTED')
  AND m.MessageLastUpdate >= '2025-01-01 00:00:00'
  AND m.MessageLastUpdate <  '2026-01-01 00:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process dp
              JOIN GlassfishDB.ProjectMandant dpm ON dpm.ProjectID = dp.ProjectID
              WHERE dp.ProcessID = m.ProcessID AND dpm.MandantID = 'NEXANS')
GROUP BY m.ProcessID, p.ProcessName
ORDER BY MAX(m.MessageLastUpdate) DESC, m.ProcessID DESC
LIMIT 10;
SELECT m.ProcessID, p.ProcessName, COUNT(*), MAX(m.MessageLastUpdate)
FROM GlassfishDB.Message m IGNORE INDEX FOR ORDER BY (MessageLastUpdateIDX)
LEFT JOIN GlassfishDB.Process p ON p.ProcessID = m.ProcessID
  WHERE (m.MessageStatus LIKE 'ERROR\_%' ESCAPE '\\'
         OR m.MessageStatus = 'COMMIT_REJECTED')
  AND m.MessageLastUpdate >= '2025-01-01 00:00:00'
  AND m.MessageLastUpdate <  '2026-01-01 00:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process dp
              JOIN GlassfishDB.ProjectMandant dpm ON dpm.ProjectID = dp.ProjectID
              WHERE dp.ProcessID = m.ProcessID AND dpm.MandantID = 'NEXANS')
GROUP BY m.ProcessID, p.ProcessName
ORDER BY MAX(m.MessageLastUpdate) DESC, m.ProcessID DESC
LIMIT 10;
SELECT m.ProcessID, p.ProcessName, COUNT(*), MAX(m.MessageLastUpdate)
FROM GlassfishDB.Message m IGNORE INDEX FOR ORDER BY (MessageLastUpdateIDX)
LEFT JOIN GlassfishDB.Process p ON p.ProcessID = m.ProcessID
  WHERE (m.MessageStatus LIKE 'ERROR\_%' ESCAPE '\\'
         OR m.MessageStatus = 'COMMIT_REJECTED')
  AND m.MessageLastUpdate >= '2025-01-01 00:00:00'
  AND m.MessageLastUpdate <  '2026-01-01 00:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process dp
              JOIN GlassfishDB.ProjectMandant dpm ON dpm.ProjectID = dp.ProjectID
              WHERE dp.ProcessID = m.ProcessID AND dpm.MandantID = 'NEXANS')
GROUP BY m.ProcessID, p.ProcessName
ORDER BY MAX(m.MessageLastUpdate) DESC, m.ProcessID DESC
LIMIT 10;
SELECT m.ProcessID, p.ProcessName, COUNT(*), MAX(m.MessageLastUpdate)
FROM GlassfishDB.Message m IGNORE INDEX FOR ORDER BY (MessageLastUpdateIDX)
LEFT JOIN GlassfishDB.Process p ON p.ProcessID = m.ProcessID
  WHERE (m.MessageStatus LIKE 'ERROR\_%' ESCAPE '\\'
         OR m.MessageStatus = 'COMMIT_REJECTED')
  AND m.MessageLastUpdate >= '2025-01-01 00:00:00'
  AND m.MessageLastUpdate <  '2026-01-01 00:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process dp
              JOIN GlassfishDB.ProjectMandant dpm ON dpm.ProjectID = dp.ProjectID
              WHERE dp.ProcessID = m.ProcessID AND dpm.MandantID = 'NEXANS')
GROUP BY m.ProcessID, p.ProcessName
ORDER BY MAX(m.MessageLastUpdate) DESC, m.ProcessID DESC
LIMIT 10;

SELECT '=== EXPLAIN verdichtet, NEXANS 12M ===' AS marke;
EXPLAIN SELECT m.ProcessID, p.ProcessName, COUNT(*), MAX(m.MessageLastUpdate)
FROM GlassfishDB.Message m IGNORE INDEX FOR ORDER BY (MessageLastUpdateIDX)
LEFT JOIN GlassfishDB.Process p ON p.ProcessID = m.ProcessID
  WHERE (m.MessageStatus LIKE 'ERROR\_%' ESCAPE '\\'
         OR m.MessageStatus = 'COMMIT_REJECTED')
  AND m.MessageLastUpdate >= '2025-01-01 00:00:00'
  AND m.MessageLastUpdate <  '2026-01-01 00:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process dp
              JOIN GlassfishDB.ProjectMandant dpm ON dpm.ProjectID = dp.ProjectID
              WHERE dp.ProcessID = m.ProcessID AND dpm.MandantID = 'NEXANS')
GROUP BY m.ProcessID, p.ProcessName
ORDER BY MAX(m.MessageLastUpdate) DESC, m.ProcessID DESC
LIMIT 10;

SELECT '=== EXPLAIN alt, NEXANS 12M ===' AS marke;
EXPLAIN SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID, s.SOSName
FROM GlassfishDB.Message m IGNORE INDEX FOR ORDER BY (MessageLastUpdateIDX)
LEFT JOIN GlassfishDB.SOS s ON s.SOSID = m.SOSID
  WHERE (m.MessageStatus LIKE 'ERROR\_%' ESCAPE '\\'
         OR m.MessageStatus = 'COMMIT_REJECTED')
  AND m.MessageLastUpdate >= '2025-01-01 00:00:00'
  AND m.MessageLastUpdate <  '2026-01-01 00:00:00'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process dp
              JOIN GlassfishDB.ProjectMandant dpm ON dpm.ProjectID = dp.ProjectID
              WHERE dp.ProcessID = m.ProcessID AND dpm.MandantID = 'NEXANS')
ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
LIMIT 10;

SELECT '=== Profil ===' AS marke;
SHOW PROFILES;
