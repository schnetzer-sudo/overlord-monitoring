-- Messrunde M94-M98 — Sitzung 6a: Vorprobe zu M97
-- Frage:   Wie viele ueberfaellige Zeilen traegt das Fenster ueberhaupt, und
--          reicht das fuer eine zweite Seite (Cursor bei Limit 50)?
-- Ergebnis: docs/messungen-schritt10b.md
--
-- Ausschliesslich SELECT / SET. (Regel S1)
-- G1: keine MessageID, keine ProcessID in der Ausgabe — nur Anzahlen.
--
-- Z1: `jetzt` ist der Anker der Anwendungsuhr, 2025-12-30 04:09:47 (V5), als
--     Literal. Die Fenster sind die aufgeloesten `zeitraum`-Werte des Endpunkts:
--       24h  ->  2025-12-29 04:09:47 .. 2025-12-30 04:09:47
--       30d  ->  2025-11-30 04:09:47 .. 2025-12-30 04:09:47
--     Beide Grenzen einschliesslich — so setzt sie NachrichtenRepository (.ge/.le).
--
-- Das Praedikat ist MessageStatusClassifier.istUeberfaellig, nach SQL uebersetzt:
--   nicht Endstatus  ->  MessageStatus IN ('SUSPENDED','RUNNING')
--   Frist vorhanden  ->  MessageTimeout IS NOT NULL AND MessageTimeout > 0
--   Frist abgelaufen ->  MessageLastUpdate + INTERVAL MessageTimeout SECOND < jetzt

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;

SET max_statement_time = 60;
SET profiling = 1;
SET profiling_history_size = 100;

SELECT '=== 6a-1 Offene und ueberfaellige Zeilen je Mandant, 30-Tage-Fenster ===' AS marke;
SELECT pm.MandantID AS mandant,
       COUNT(*)     AS zeilen_im_fenster,
       SUM(m.MessageStatus IN ('SUSPENDED','RUNNING')) AS offen,
       SUM(m.MessageStatus IN ('SUSPENDED','RUNNING')
           AND m.MessageTimeout IS NOT NULL AND m.MessageTimeout > 0
           AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND
               < '2025-12-30 04:09:47')                AS ueberfaellig
FROM GlassfishDB.Message m
JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE m.MessageLastUpdate >= '2025-11-30 04:09:47'
  AND m.MessageLastUpdate <= '2025-12-30 04:09:47'
GROUP BY pm.MandantID
ORDER BY zeilen_im_fenster DESC;

SELECT '=== 6a-2 Dasselbe im 24-Stunden-Fenster ===' AS marke;
SELECT pm.MandantID AS mandant,
       COUNT(*)     AS zeilen_im_fenster,
       SUM(m.MessageStatus IN ('SUSPENDED','RUNNING')) AS offen,
       SUM(m.MessageStatus IN ('SUSPENDED','RUNNING')
           AND m.MessageTimeout IS NOT NULL AND m.MessageTimeout > 0
           AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND
               < '2025-12-30 04:09:47')                AS ueberfaellig
FROM GlassfishDB.Message m
JOIN GlassfishDB.Process        p  ON p.ProcessID  = m.ProcessID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE m.MessageLastUpdate >= '2025-12-29 04:09:47'
  AND m.MessageLastUpdate <= '2025-12-30 04:09:47'
GROUP BY pm.MandantID
ORDER BY zeilen_im_fenster DESC;

SELECT '=== 6a-3 Die Nachmessung, die die Korrekturrunde offen gelassen hat ===' AS marke;
-- messungen-schritt10.md, Kasten bei V5: Die 538 aus M90 sind am falschen Anker
-- gerechnet. Wie viele davon am GELTENDEN Anker ueberfaellig sind, war offen.
-- Ohne Zeitfenster (L9): Gefragt ist der Gesamtbestand, ein Fenster schnitte
-- genau die Zeilen weg, um die es geht. Der Zugriff laeuft ueber
-- MessageStatusIDX auf 539 von 3,34 Mio. Zeilen — kein voller Durchlauf.
-- Kein Vorbild fuer Anwendungscode.
SELECT COUNT(*) AS offen_gesamtbestand,
       SUM(MessageTimeout IS NOT NULL AND MessageTimeout > 0) AS davon_mit_frist,
       SUM(MessageTimeout IS NOT NULL AND MessageTimeout > 0
           AND MessageLastUpdate + INTERVAL MessageTimeout SECOND
               < '2025-12-30 04:09:47')                AS ueberfaellig_am_anker_v5b,
       SUM(MessageTimeout IS NOT NULL AND MessageTimeout > 0
           AND MessageLastUpdate + INTERVAL MessageTimeout SECOND
               < '2026-07-08 17:21:10')                AS ueberfaellig_am_alten_anker,
       MIN(MessageLastUpdate) AS aelteste_offene,
       MAX(MessageLastUpdate) AS juengste_offene,
       MIN(MessageTimeout)    AS kleinste_frist,
       MAX(MessageTimeout)    AS groesste_frist
FROM GlassfishDB.Message
WHERE MessageStatus IN ('SUSPENDED','RUNNING');

SELECT '=== 6a-4 Fristen der offenen Zeilen, gruppiert ===' AS marke;
SELECT MessageTimeout AS frist_sekunden, COUNT(*) AS zeilen
FROM GlassfishDB.Message
WHERE MessageStatus IN ('SUSPENDED','RUNNING')
GROUP BY MessageTimeout
ORDER BY zeilen DESC;

SELECT '=== 99 Laufzeiten ===' AS marke;
SHOW PROFILES;

SELECT '=== 99 read_only am Ende der Sitzung ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;
