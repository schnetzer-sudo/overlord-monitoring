-- Messrunde M86-M92 — Sitzung 3b: M87-5, die Gegenprobe ueber Jahresscheiben
--
-- WARUM DIESE SITZUNG UEBERHAUPT LAEUFT. M87-5 (alle vier Varianten in EINEM Durchlauf,
-- als Gegenprobe zu den vier Einzelabfragen) hat in Sitzung 3 nach 60,219 s die Grenze
-- `max_statement_time = 60` gerissen. Der Rahmen (§1) sagt dazu: "Wird sie erreicht, ist
-- das der Befund — nicht hochsetzen. Die Messung wird dann ueber Jahresscheiben gefahren
-- und summiert, mit Vermerk." Genau das geschieht hier.
--
-- WARUM DAS SUMMIEREN EXAKT IST UND KEINE NAEHERUNG. In allen vier Varianten steht die
-- Zeit als erstes Glied des Schluessels (Stunde, Tag, Monat). Eine Kombination kann
-- deshalb nicht ueber zwei Jahresscheiben hinweg existieren: die Scheibe ist aus dem
-- Schluessel selbst ableitbar. Die Teilmengen sind disjunkt, die Summe der
-- Verschiedenheiten ist die Verschiedenheit der Vereinigung. Das gilt NICHT fuer
-- verschiedene_prozesse — diese Spalte wird deshalb nicht summiert, sondern nur
-- je Scheibe ausgewiesen.
--
-- Ausschliesslich SELECT / SET. (Regel S1)   Z1: alle Grenzen als Literal.
-- Bestand: 2024-10-01 02:00:28 bis 2026-07-08 17:21:10 (V5).

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;

SET max_statement_time = 60;
SET profiling_history_size = 100;
SET profiling = 1;

SELECT '=== M87-5a Scheibe 2024 (2024-10-01 .. 2025-01-01) ===' AS marke;
SELECT '2024' AS scheibe,
       COUNT(*) AS zeilen,
       COUNT(DISTINCT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00'), ProcessID, MessageStatus) AS v1,
       COUNT(DISTINCT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00'), ProcessID,
         CASE
           WHEN MessageStatus = 'FINISHED'                             THEN 'ABGESCHLOSSEN'
           WHEN MessageStatus IN ('EERP_RECEIVED','COMMIT_RECEIVED')   THEN 'QUITTIERT'
           WHEN MessageStatus LIKE 'ERROR!_%' ESCAPE '!'
                OR MessageStatus = 'COMMIT_REJECTED'                   THEN 'FEHLER'
           WHEN MessageStatus = 'SPLITTED'                             THEN 'AUFGETEILT'
           WHEN MessageStatus = 'MERGED'                               THEN 'ZUSAMMENGEFUEHRT'
           WHEN MessageStatus = 'SUSPENDED'                            THEN 'WARTEND'
           WHEN MessageStatus = 'RUNNING'                              THEN 'LAEUFT'
           ELSE 'UNGEKLAERT'
         END)                                                                              AS v2,
       COUNT(DISTINCT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d'), ProcessID, MessageStatus) AS v3,
       COUNT(DISTINCT DATE_FORMAT(MessageLastUpdate, '%Y-%m'), ProcessID, MessageStatus)    AS v4,
       COUNT(DISTINCT ProcessID)                                                            AS prozesse_scheibe
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2024-10-01 00:00:00' AND MessageLastUpdate < '2025-01-01 00:00:00';

SELECT '=== M87-5b Scheibe 2025 (2025-01-01 .. 2026-01-01) ===' AS marke;
SELECT '2025' AS scheibe,
       COUNT(*) AS zeilen,
       COUNT(DISTINCT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00'), ProcessID, MessageStatus) AS v1,
       COUNT(DISTINCT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00'), ProcessID,
         CASE
           WHEN MessageStatus = 'FINISHED'                             THEN 'ABGESCHLOSSEN'
           WHEN MessageStatus IN ('EERP_RECEIVED','COMMIT_RECEIVED')   THEN 'QUITTIERT'
           WHEN MessageStatus LIKE 'ERROR!_%' ESCAPE '!'
                OR MessageStatus = 'COMMIT_REJECTED'                   THEN 'FEHLER'
           WHEN MessageStatus = 'SPLITTED'                             THEN 'AUFGETEILT'
           WHEN MessageStatus = 'MERGED'                               THEN 'ZUSAMMENGEFUEHRT'
           WHEN MessageStatus = 'SUSPENDED'                            THEN 'WARTEND'
           WHEN MessageStatus = 'RUNNING'                              THEN 'LAEUFT'
           ELSE 'UNGEKLAERT'
         END)                                                                              AS v2,
       COUNT(DISTINCT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d'), ProcessID, MessageStatus) AS v3,
       COUNT(DISTINCT DATE_FORMAT(MessageLastUpdate, '%Y-%m'), ProcessID, MessageStatus)    AS v4,
       COUNT(DISTINCT ProcessID)                                                            AS prozesse_scheibe
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2025-01-01 00:00:00' AND MessageLastUpdate < '2026-01-01 00:00:00';

SELECT '=== M87-5c Scheibe 2026 (2026-01-01 .. 2026-08-01) ===' AS marke;
SELECT '2026' AS scheibe,
       COUNT(*) AS zeilen,
       COUNT(DISTINCT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00'), ProcessID, MessageStatus) AS v1,
       COUNT(DISTINCT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00'), ProcessID,
         CASE
           WHEN MessageStatus = 'FINISHED'                             THEN 'ABGESCHLOSSEN'
           WHEN MessageStatus IN ('EERP_RECEIVED','COMMIT_RECEIVED')   THEN 'QUITTIERT'
           WHEN MessageStatus LIKE 'ERROR!_%' ESCAPE '!'
                OR MessageStatus = 'COMMIT_REJECTED'                   THEN 'FEHLER'
           WHEN MessageStatus = 'SPLITTED'                             THEN 'AUFGETEILT'
           WHEN MessageStatus = 'MERGED'                               THEN 'ZUSAMMENGEFUEHRT'
           WHEN MessageStatus = 'SUSPENDED'                            THEN 'WARTEND'
           WHEN MessageStatus = 'RUNNING'                              THEN 'LAEUFT'
           ELSE 'UNGEKLAERT'
         END)                                                                              AS v2,
       COUNT(DISTINCT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d'), ProcessID, MessageStatus) AS v3,
       COUNT(DISTINCT DATE_FORMAT(MessageLastUpdate, '%Y-%m'), ProcessID, MessageStatus)    AS v4,
       COUNT(DISTINCT ProcessID)                                                            AS prozesse_scheibe
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2026-01-01 00:00:00' AND MessageLastUpdate < '2026-08-01 00:00:00';

SELECT '=== M87-5d Kontrolle: deckt die Dreiteilung den ganzen Bestand? ===' AS marke;
SELECT COUNT(*) AS zeilen_ausserhalb_der_drei_scheiben
FROM GlassfishDB.Message
WHERE MessageLastUpdate <  '2024-10-01 00:00:00'
   OR MessageLastUpdate >= '2026-08-01 00:00:00';

SELECT '=== 98 SHOW PROFILES ===' AS marke;
SHOW PROFILES;

SELECT '=== 99 Abschluss ===' AS marke;
SELECT @@global.read_only AS read_only_ende, NOW() AS serverzeit_ende;
