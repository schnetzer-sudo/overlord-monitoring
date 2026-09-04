-- Messrunde 10b-4 — Sitzung 2: M144, die Erscheinungsbedingung je Mandant
-- Auftrag:  "Schritt 10b-4 — Ueberfaellig widerlegen, Laeuft und Wartend bauen", 03.09.2026
-- Ergebnis: docs/messungen-schritt10b.md, Abschnitt "Messrunde 10b-4"
--
-- Ausschliesslich SELECT / SET. (Regel S1)
-- G1: keine Zugangsdaten, kein Hostname, keine ProcessID, keine MessageID.
--     Mandanten-IDs sind Codes und stehen in PROJEKTBESCHREIBUNG.md Paragraf 3.2.
--
-- Z1: Der Anker der Anwendungsuhr steht als Literal: 2025-12-30 04:09:47 (M9).

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;

SET max_statement_time = 60;
SET profiling = 1;
SET profiling_history_size = 200;

-- ── M144 (a) — Die Erscheinungsbedingung, fuer alle zehn Mandanten ──────────
-- Dieselbe Kette wie im Anwendungscode: SOSAction -> SOS -> Process ->
-- ProjectMandant, als EXISTS (ProjectMandant ist n:m). Project steht nicht in
-- der Kette (Befund 48).
--
-- Hier je Mandant EINE Zeile, damit auch die `false` protokolliert sind: Das
-- Ergebnis je Mandant ist die Zahl, die vorher niemand hatte.
SELECT '=== M144a Erscheinungsbedingung SUSPEND und WAITUNTIL, alle zehn ===' AS marke;
SELECT md.MandantID AS mandant,
       EXISTS (SELECT 1
               FROM GlassfishDB.SOSAction sa
               JOIN GlassfishDB.SOS s ON s.SOSID = sa.SOSID
               WHERE sa.SOSActionServiceProperties LIKE '%SUSPEND%'
                 AND EXISTS (SELECT 1
                             FROM GlassfishDB.Process p
                             JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
                             WHERE p.ProcessID = s.ProcessID
                               AND pm.MandantID = md.MandantID)) AS hat_suspend_baustein,
       EXISTS (SELECT 1
               FROM GlassfishDB.SOSAction sa
               JOIN GlassfishDB.SOS s ON s.SOSID = sa.SOSID
               WHERE sa.SOSActionServiceProperties LIKE '%WAITUNTIL%'
                 AND EXISTS (SELECT 1
                             FROM GlassfishDB.Process p
                             JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
                             WHERE p.ProcessID = s.ProcessID
                               AND pm.MandantID = md.MandantID)) AS hat_waituntil_baustein
FROM GlassfishDB.Mandant md
ORDER BY md.MandantID;

SELECT '=== M144a-2 Wie viele SOSAction-Zeilen mit SUSPEND je Mandant? (Gegenprobe zur 0/1) ===' AS marke;
SELECT pm.MandantID AS mandant,
       COUNT(*)                                                   AS sosaction_mit_suspend,
       COUNT(DISTINCT s.SOSID)                                    AS verschiedene_ablaeufe,
       COUNT(DISTINCT s.ProcessID)                                AS verschiedene_prozesse
FROM GlassfishDB.SOSAction sa
JOIN GlassfishDB.SOS s            ON s.SOSID     = sa.SOSID
JOIN GlassfishDB.Process p        ON p.ProcessID = s.ProcessID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE sa.SOSActionServiceProperties LIKE '%SUSPEND%'
GROUP BY pm.MandantID
ORDER BY pm.MandantID;

SELECT '=== M144a-3 Dasselbe fuer WAITUNTIL ===' AS marke;
SELECT pm.MandantID AS mandant,
       COUNT(*)                    AS sosaction_mit_waituntil,
       COUNT(DISTINCT s.SOSID)     AS verschiedene_ablaeufe,
       COUNT(DISTINCT s.ProcessID) AS verschiedene_prozesse
FROM GlassfishDB.SOSAction sa
JOIN GlassfishDB.SOS s            ON s.SOSID     = sa.SOSID
JOIN GlassfishDB.Process p        ON p.ProcessID = s.ProcessID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE sa.SOSActionServiceProperties LIKE '%WAITUNTIL%'
GROUP BY pm.MandantID
ORDER BY pm.MandantID;

-- ── M144 (b) — Die Aufteilung der 538 SUSPENDED auf die Mandanten ───────────
-- Der Auftrag fuehrt sie als "nicht erhoben". M90 hat sie erhoben; hier wird
-- nachgemessen, weil M90 gegen den falschen Anker gerechnet hat (dashboard.md
-- Paragraf 8). Auf die Zuordnung wirkt der Anker nicht -- nachgemessen wird
-- trotzdem, damit die Zahl in dieser Runde belegt ist und nicht uebernommen.
--
-- Ohne Zeitfenster, und das ist gedeckt (Regel L9): Gefragt ist der ganze
-- Bestand eines Status, und der Statusfilter traegt einen Index.
SELECT '=== M144b Alle offenen Zeilen (SUSPENDED/RUNNING) je Mandant, Gesamtbestand ===' AS marke;
SELECT pm.MandantID       AS mandant,
       m.MessageStatus    AS status,
       COUNT(*)           AS zeilen,
       MIN(m.MessageLastUpdate) AS aelteste,
       MAX(m.MessageLastUpdate) AS juengste,
       TIMESTAMPDIFF(SECOND, MIN(m.MessageLastUpdate), '2025-12-30 04:09:47') AS aeltestes_alter_s
FROM GlassfishDB.Message m
JOIN GlassfishDB.Process p         ON p.ProcessID  = m.ProcessID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE m.MessageStatus IN ('SUSPENDED','RUNNING')
GROUP BY pm.MandantID, m.MessageStatus
ORDER BY pm.MandantID, m.MessageStatus;

SELECT '=== M144b-2 Gegenprobe ohne Mandantenkette: die Gesamtzahl ===' AS marke;
SELECT m.MessageStatus AS status,
       COUNT(*)        AS zeilen,
       MIN(m.MessageLastUpdate) AS aelteste,
       MAX(m.MessageLastUpdate) AS juengste
FROM GlassfishDB.Message m
WHERE m.MessageStatus IN ('SUSPENDED','RUNNING')
GROUP BY m.MessageStatus;

-- ── M144 (c) — Die offene Pruefung aus Paragraf 2, gegen die TESTKOPIE ──────
-- Sie gehoert gegen die PRODUKTION und steht dort als offene Pruefung. Hier
-- laeuft sie einmal gegen die Testkopie, damit belegt ist, was sie DORT sagt --
-- und damit sichtbar ist, dass sie dort nichts entscheidet.
SELECT '=== M144c Die offene Pruefung aus Paragraf 2, gegen die Testkopie ===' AS marke;
SELECT MessageStatus, COUNT(*) AS zeilen,
       MIN(MessageLastUpdate) AS aelteste,
       MAX(TIMESTAMPDIFF(SECOND, MessageLastUpdate, '2025-12-30 04:09:47')) AS aeltestes_alter_sekunden,
       ROUND(MAX(TIMESTAMPDIFF(SECOND, MessageLastUpdate, '2025-12-30 04:09:47')) / 86400, 2) AS aeltestes_alter_tage
FROM GlassfishDB.Message
WHERE MessageStatus IN ('SUSPENDED','RUNNING')
GROUP BY MessageStatus;

-- ── M144 (d) — Wieviel Ueberhang traegt Message.MessageTimeout bei den 538? ─
-- Fuer die Kachel Wartend faellt fristSekunden weg. Die Zahl steht hier, damit
-- die Streichung belegt ist und nicht behauptet.
SELECT '=== M144d MessageTimeout der offenen Zeilen ===' AS marke;
SELECT MessageStatus,
       MessageTimeout,
       COUNT(*) AS zeilen
FROM GlassfishDB.Message
WHERE MessageStatus IN ('SUSPENDED','RUNNING')
GROUP BY MessageStatus, MessageTimeout
ORDER BY MessageStatus, MessageTimeout;

SELECT '=== M144 Profil ===' AS marke;
SET profiling = 0;
SHOW PROFILES;
