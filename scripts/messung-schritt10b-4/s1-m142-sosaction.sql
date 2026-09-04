-- Messrunde 10b-4 — Sitzung 1: M142, traegt SOSAction das Wort und was kostet es?
-- Auftrag:  "Schritt 10b-4 — Ueberfaellig widerlegen, Laeuft und Wartend bauen", 03.09.2026
-- Ergebnis: docs/messungen-schritt10b.md, Abschnitt "Messrunde 10b-4"
--
-- Ausschliesslich SELECT / SET / EXPLAIN. (Regel S1)
-- G1: keine Zugangsdaten, kein Hostname, keine ProcessID und keine MessageID in
--     der Ausgabe -- nur Anzahlen, Namen von Stammdatenbausteinen und Plaene.
--
-- Z1: Der Anker der Anwendungsuhr steht als Literal, nicht als NOW():
--     2025-12-30 04:09:47 (M9).
--
-- ABBRUCHKRITERIUM: Kostet die Erscheinungsbedingung mehr als 200 ms, ist
-- Paragraf 3.5 nicht so zu bauen und der Schritt haelt an.

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, VERSION() AS version, NOW() AS serverzeit;

SELECT '=== 01 Benutzer ===' AS marke;
SELECT USER() AS benutzer, CURRENT_USER() AS aufgeloest;

SELECT '=== 02 Rahmenwerte ===' AS marke;
SELECT @@div_precision_increment AS dpi,
       @@max_statement_time      AS max_statement_time_vorgabe,
       @@time_zone               AS time_zone,
       @@system_time_zone        AS system_time_zone;

SET max_statement_time = 60;
SET profiling = 1;
SET profiling_history_size = 200;

-- ── M142 (a) — Groesse von SOSAction und SOS ────────────────────────────────
SELECT '=== M142a Groesse der Stammdatentabellen (information_schema) ===' AS marke;
SELECT TABLE_NAME,
       ENGINE,
       TABLE_ROWS AS geschaetzt,
       DATA_LENGTH,
       INDEX_LENGTH,
       DATA_LENGTH + INDEX_LENGTH AS gesamt_bytes,
       AVG_ROW_LENGTH,
       TABLE_COLLATION
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'GlassfishDB'
  AND TABLE_NAME IN ('SOSAction','SOS','Process','Project','ProjectMandant')
ORDER BY TABLE_NAME;

SELECT '=== M142a-2 Zeilenzahl GEZAEHLT, nicht geschaetzt ===' AS marke;
-- information_schema.TABLE_ROWS ist eine Stichprobenschaetzung und lag in diesem
-- Projekt schon um 60,9 Prozent daneben (M44). Deshalb gezaehlt.
SELECT (SELECT COUNT(*) FROM GlassfishDB.SOSAction)      AS sosaction_zeilen,
       (SELECT COUNT(*) FROM GlassfishDB.SOS)            AS sos_zeilen,
       (SELECT COUNT(*) FROM GlassfishDB.Process)        AS process_zeilen,
       (SELECT COUNT(*) FROM GlassfishDB.ProjectMandant) AS projectmandant_zeilen;

SELECT '=== M142a-3 Traegt SOS wirklich eine ProcessID? (Beziehung vor dem Bau pruefen) ===' AS marke;
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_KEY
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'GlassfishDB' AND TABLE_NAME = 'SOS'
ORDER BY ORDINAL_POSITION;

SELECT '=== M142a-4 Wie viele SOS haengen an einem Prozess, wie viele nicht? ===' AS marke;
SELECT COUNT(*)                          AS sos_gesamt,
       SUM(ProcessID IS NULL)            AS ohne_processid,
       SUM(ProcessID IS NOT NULL)        AS mit_processid,
       COUNT(DISTINCT ProcessID)         AS verschiedene_prozesse
FROM GlassfishDB.SOS;

SELECT '=== M142a-5 Indizes auf SOSAction und SOS ===' AS marke;
SELECT TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX, COLUMN_NAME, NON_UNIQUE, CARDINALITY
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'GlassfishDB' AND TABLE_NAME IN ('SOSAction','SOS')
ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX;

-- ── M142 (b) — Kosten von LIKE ueber SOSActionServiceProperties ─────────────
SELECT '=== M142b Wie viele SOSAction-Zeilen tragen SUSPEND bzw. WAITUNTIL? ===' AS marke;
SELECT COUNT(*)                                              AS zeilen,
       SUM(SOSActionServiceProperties LIKE '%SUSPEND%')      AS mit_suspend,
       SUM(SOSActionServiceProperties LIKE '%WAITUNTIL%')    AS mit_waituntil,
       SUM(SOSActionServiceProperties LIKE '%SUSPEND%'
           AND SOSActionServiceProperties LIKE '%WAITUNTIL%') AS mit_beiden,
       SUM(SOSActionServiceProperties IS NULL)               AS ohne_bausteine,
       COUNT(DISTINCT SOSID)                                 AS verschiedene_ablaeufe
FROM GlassfishDB.SOSAction;

-- Aufwaermlauf und dann fuenf Laeufe je Fassung. Die Zeiten stehen im Profil.
SELECT '=== M142b-2 Aufwaermlauf ===' AS marke;
SELECT COUNT(*) AS mit_suspend FROM GlassfishDB.SOSAction WHERE SOSActionServiceProperties LIKE '%SUSPEND%';

SELECT '=== M142b-3 SUSPEND, fuenf Laeufe ===' AS marke;
SELECT COUNT(*) AS l1 FROM GlassfishDB.SOSAction WHERE SOSActionServiceProperties LIKE '%SUSPEND%';
SELECT COUNT(*) AS l2 FROM GlassfishDB.SOSAction WHERE SOSActionServiceProperties LIKE '%SUSPEND%';
SELECT COUNT(*) AS l3 FROM GlassfishDB.SOSAction WHERE SOSActionServiceProperties LIKE '%SUSPEND%';
SELECT COUNT(*) AS l4 FROM GlassfishDB.SOSAction WHERE SOSActionServiceProperties LIKE '%SUSPEND%';
SELECT COUNT(*) AS l5 FROM GlassfishDB.SOSAction WHERE SOSActionServiceProperties LIKE '%SUSPEND%';

SELECT '=== M142b-4 WAITUNTIL, fuenf Laeufe ===' AS marke;
SELECT COUNT(*) AS l1 FROM GlassfishDB.SOSAction WHERE SOSActionServiceProperties LIKE '%WAITUNTIL%';
SELECT COUNT(*) AS l2 FROM GlassfishDB.SOSAction WHERE SOSActionServiceProperties LIKE '%WAITUNTIL%';
SELECT COUNT(*) AS l3 FROM GlassfishDB.SOSAction WHERE SOSActionServiceProperties LIKE '%WAITUNTIL%';
SELECT COUNT(*) AS l4 FROM GlassfishDB.SOSAction WHERE SOSActionServiceProperties LIKE '%WAITUNTIL%';
SELECT COUNT(*) AS l5 FROM GlassfishDB.SOSAction WHERE SOSActionServiceProperties LIKE '%WAITUNTIL%';

-- ── M142 (c) — Traegt der GEPLANTE Baustein der 538 das Wort? ───────────────
-- M29 (4) hat SUSPEND und WAITUNTIL bei allen 538 in
-- MessageAction.SOSActionServiceProperties gefunden -- im AUSGEFUEHRTEN Baustein.
-- Hier wird SOSAction gelesen, also der GEPLANTE. Die Uebertragung haengt nach
-- M29 (4) an EINER SOSAction-Zeile: eine SOSActionID, ein Ablauf ueber alle 538.
SELECT '=== M142c Der geplante Baustein der 538 wartenden Nachrichten ===' AS marke;
SELECT COUNT(*)                                     AS nachrichten,
       COUNT(DISTINCT m.SOSID)                      AS verschiedene_ablaeufe,
       COUNT(DISTINCT m.SOSActionID)                AS verschiedene_sosactionid,
       SUM(sa.SOSID IS NOT NULL)                    AS verweis_loest_auf,
       SUM(sa.SOSActionServiceProperties LIKE '%SUSPEND%')   AS geplant_mit_suspend,
       SUM(sa.SOSActionServiceProperties LIKE '%WAITUNTIL%') AS geplant_mit_waituntil
FROM GlassfishDB.Message m
LEFT JOIN GlassfishDB.SOSAction sa
       ON sa.SOSID = m.SOSID AND sa.SOSActionID = m.SOSActionID
WHERE m.MessageStatus = 'SUSPENDED';

SELECT '=== M142c-2 Derselbe Baustein, ueber den zuletzt AUSGEFUEHRTEN Schritt (Gegenprobe zu M29) ===' AS marke;
-- Nicht ueber Message.SOSActionID, sondern ueber MessageAction -- der Join, den
-- M15 mit 100 Prozent Uebereinstimmung belegt: (MessageAction.SOSID, .SOSActionID).
SELECT COUNT(*)                                                  AS zeilen,
       SUM(ma.SOSActionServiceProperties LIKE '%SUSPEND%')        AS ausgefuehrt_suspend,
       SUM(sa.SOSActionServiceProperties LIKE '%SUSPEND%')        AS geplant_suspend,
       SUM((ma.SOSActionServiceProperties LIKE '%SUSPEND%')
        <> (sa.SOSActionServiceProperties LIKE '%SUSPEND%'))      AS abweichungen
FROM GlassfishDB.Message m
JOIN GlassfishDB.MessageAction ma ON ma.MessageID = m.MessageID AND ma.SOSActionID <> 0
JOIN GlassfishDB.SOSAction sa
     ON sa.SOSID = ma.SOSID AND sa.SOSActionID = ma.SOSActionID
WHERE m.MessageStatus = 'SUSPENDED';

SELECT '=== M142c-3 Die Bausteinliste des geplanten Schritts, maskiert bis auf die Marken ===' AS marke;
-- G1: der Rohwert kann Partnernamen tragen. Ausgegeben werden nur die beiden
-- Marken und die Laenge, nicht der Wert.
SELECT sa.SOSActionName                                   AS baustein_name,
       LENGTH(sa.SOSActionServiceProperties)              AS laenge,
       sa.SOSActionServiceProperties LIKE '%SUSPEND%'     AS hat_suspend,
       sa.SOSActionServiceProperties LIKE '%WAITUNTIL%'   AS hat_waituntil,
       sa.SOSActionTimeout                                AS schrittfrist,
       COUNT(*)                                           AS nachrichten
FROM GlassfishDB.Message m
JOIN GlassfishDB.SOSAction sa
     ON sa.SOSID = m.SOSID AND sa.SOSActionID = m.SOSActionID
WHERE m.MessageStatus = 'SUSPENDED'
GROUP BY sa.SOSActionName, laenge, hat_suspend, hat_waituntil, sa.SOSActionTimeout;

-- ── M142 (d) — Die Erscheinungsbedingung aus Paragraf 3.5, gerendert ────────
-- Die Kette ist die des Anwendungscodes: SOSAction -> SOS -> Process ->
-- ProjectMandant, als EXISTS und nicht als Join (ProjectMandant ist n:m).
-- Project steht nicht in der Kette (Befund 48).
SELECT '=== M142d Plan der Erscheinungsbedingung, NEXANS ===' AS marke;
EXPLAIN
SELECT EXISTS (
  SELECT 1
  FROM GlassfishDB.SOSAction sa
  JOIN GlassfishDB.SOS s ON s.SOSID = sa.SOSID
  WHERE sa.SOSActionServiceProperties LIKE '%SUSPEND%'
    AND EXISTS (
          SELECT 1
          FROM GlassfishDB.Process p
          JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
          WHERE p.ProcessID = s.ProcessID
            AND pm.MandantID = 'NEXANS')
) AS hat_wartende_ablaeufe;

SELECT '=== M142d-2 Plan der Erscheinungsbedingung, SUTTONS ===' AS marke;
EXPLAIN
SELECT EXISTS (
  SELECT 1
  FROM GlassfishDB.SOSAction sa
  JOIN GlassfishDB.SOS s ON s.SOSID = sa.SOSID
  WHERE sa.SOSActionServiceProperties LIKE '%SUSPEND%'
    AND EXISTS (
          SELECT 1
          FROM GlassfishDB.Process p
          JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
          WHERE p.ProcessID = s.ProcessID
            AND pm.MandantID = 'SUTTONS')
) AS hat_wartende_ablaeufe;

SELECT '=== M142d-3 Aufwaermlauf NEXANS ===' AS marke;
SELECT EXISTS (SELECT 1 FROM GlassfishDB.SOSAction sa JOIN GlassfishDB.SOS s ON s.SOSID = sa.SOSID
  WHERE sa.SOSActionServiceProperties LIKE '%SUSPEND%'
    AND EXISTS (SELECT 1 FROM GlassfishDB.Process p JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
                WHERE p.ProcessID = s.ProcessID AND pm.MandantID = 'NEXANS')) AS w;

SELECT '=== M142d-4 NEXANS, fuenf Laeufe ===' AS marke;
SELECT EXISTS (SELECT 1 FROM GlassfishDB.SOSAction sa JOIN GlassfishDB.SOS s ON s.SOSID = sa.SOSID WHERE sa.SOSActionServiceProperties LIKE '%SUSPEND%' AND EXISTS (SELECT 1 FROM GlassfishDB.Process p JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID WHERE p.ProcessID = s.ProcessID AND pm.MandantID = 'NEXANS')) AS l1;
SELECT EXISTS (SELECT 1 FROM GlassfishDB.SOSAction sa JOIN GlassfishDB.SOS s ON s.SOSID = sa.SOSID WHERE sa.SOSActionServiceProperties LIKE '%SUSPEND%' AND EXISTS (SELECT 1 FROM GlassfishDB.Process p JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID WHERE p.ProcessID = s.ProcessID AND pm.MandantID = 'NEXANS')) AS l2;
SELECT EXISTS (SELECT 1 FROM GlassfishDB.SOSAction sa JOIN GlassfishDB.SOS s ON s.SOSID = sa.SOSID WHERE sa.SOSActionServiceProperties LIKE '%SUSPEND%' AND EXISTS (SELECT 1 FROM GlassfishDB.Process p JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID WHERE p.ProcessID = s.ProcessID AND pm.MandantID = 'NEXANS')) AS l3;
SELECT EXISTS (SELECT 1 FROM GlassfishDB.SOSAction sa JOIN GlassfishDB.SOS s ON s.SOSID = sa.SOSID WHERE sa.SOSActionServiceProperties LIKE '%SUSPEND%' AND EXISTS (SELECT 1 FROM GlassfishDB.Process p JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID WHERE p.ProcessID = s.ProcessID AND pm.MandantID = 'NEXANS')) AS l4;
SELECT EXISTS (SELECT 1 FROM GlassfishDB.SOSAction sa JOIN GlassfishDB.SOS s ON s.SOSID = sa.SOSID WHERE sa.SOSActionServiceProperties LIKE '%SUSPEND%' AND EXISTS (SELECT 1 FROM GlassfishDB.Process p JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID WHERE p.ProcessID = s.ProcessID AND pm.MandantID = 'NEXANS')) AS l5;

SELECT '=== M142d-5 Aufwaermlauf SUTTONS ===' AS marke;
SELECT EXISTS (SELECT 1 FROM GlassfishDB.SOSAction sa JOIN GlassfishDB.SOS s ON s.SOSID = sa.SOSID WHERE sa.SOSActionServiceProperties LIKE '%SUSPEND%' AND EXISTS (SELECT 1 FROM GlassfishDB.Process p JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID WHERE p.ProcessID = s.ProcessID AND pm.MandantID = 'SUTTONS')) AS w;

SELECT '=== M142d-6 SUTTONS, fuenf Laeufe ===' AS marke;
SELECT EXISTS (SELECT 1 FROM GlassfishDB.SOSAction sa JOIN GlassfishDB.SOS s ON s.SOSID = sa.SOSID WHERE sa.SOSActionServiceProperties LIKE '%SUSPEND%' AND EXISTS (SELECT 1 FROM GlassfishDB.Process p JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID WHERE p.ProcessID = s.ProcessID AND pm.MandantID = 'SUTTONS')) AS l1;
SELECT EXISTS (SELECT 1 FROM GlassfishDB.SOSAction sa JOIN GlassfishDB.SOS s ON s.SOSID = sa.SOSID WHERE sa.SOSActionServiceProperties LIKE '%SUSPEND%' AND EXISTS (SELECT 1 FROM GlassfishDB.Process p JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID WHERE p.ProcessID = s.ProcessID AND pm.MandantID = 'SUTTONS')) AS l2;
SELECT EXISTS (SELECT 1 FROM GlassfishDB.SOSAction sa JOIN GlassfishDB.SOS s ON s.SOSID = sa.SOSID WHERE sa.SOSActionServiceProperties LIKE '%SUSPEND%' AND EXISTS (SELECT 1 FROM GlassfishDB.Process p JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID WHERE p.ProcessID = s.ProcessID AND pm.MandantID = 'SUTTONS')) AS l3;
SELECT EXISTS (SELECT 1 FROM GlassfishDB.SOSAction sa JOIN GlassfishDB.SOS s ON s.SOSID = sa.SOSID WHERE sa.SOSActionServiceProperties LIKE '%SUSPEND%' AND EXISTS (SELECT 1 FROM GlassfishDB.Process p JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID WHERE p.ProcessID = s.ProcessID AND pm.MandantID = 'SUTTONS')) AS l4;
SELECT EXISTS (SELECT 1 FROM GlassfishDB.SOSAction sa JOIN GlassfishDB.SOS s ON s.SOSID = sa.SOSID WHERE sa.SOSActionServiceProperties LIKE '%SUSPEND%' AND EXISTS (SELECT 1 FROM GlassfishDB.Process p JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID WHERE p.ProcessID = s.ProcessID AND pm.MandantID = 'SUTTONS')) AS l5;

-- ── Das Profil ─────────────────────────────────────────────────────────────
SELECT '=== M142 Profil: alle Laufzeiten dieser Sitzung ===' AS marke;
SET profiling = 0;
SHOW PROFILES;
