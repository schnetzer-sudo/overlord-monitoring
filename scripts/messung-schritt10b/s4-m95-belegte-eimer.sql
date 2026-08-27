-- Messrunde M94-M98 — Sitzung 4: M95, Anteil belegter Eimer je Mandant und Paar
-- Auftrag:  "Messrunde vor Schritt 10b", Stand 27.08.2026
-- Ergebnis: docs/messungen-schritt10b.md
--
-- Ausschliesslich SELECT / SET. (Regel S1)
-- G1: keine ProcessID, kein Partnername. MandantID ist Konfigurationsvokabular
--     und steht seit M3 in jeder Messdatei im Klartext.
--
-- Kein EXPLAIN: Das ist eine Verteilungsfrage, keine Leistungsfrage (Auftrag).
--     Die Laufzeiten stehen trotzdem im Profil.
--
-- Ueber ALLE zehn Mandanten in EINER Abfrage je Paar, gruppiert nach MandantID:
--     Zehn einzelne Abfragen laesen denselben Rollupbereich zehnmal. L7 verlangt
--     zwei Mandanten fuer Abfragen, die in Anwendungscode muenden — diese hier
--     tut es nicht, sie ist die Erhebung selbst.
-- Mandanten ohne eine einzige Rollupzeile im Fenster erscheinen NICHT in der
--     Gruppierung. Sie sind der Befund und stehen unter V3b mit ihren Prozessen.

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;

SET max_statement_time = 60;
SET profiling = 1;
SET profiling_history_size = 100;


-- ══ M95 · 48 h / Stunde · 48 Eimer im Fenster ═══════════════════
SELECT '=== M95 P1 Gegenprobe: Rollupbereich ohne Mandantenjoin ===' AS marke;
SELECT COUNT(*) AS rollupzeilen_gesamt, SUM(anzahl) AS nachrichten_gesamt,
       COUNT(DISTINCT process_id) AS versch_prozesse_gesamt
FROM overlord_monitor.message_rollup
WHERE stunde >= '2025-12-28 05:00:00' AND stunde < '2025-12-30 05:00:00';

SELECT '=== M95 P1 Ergebnis je Mandant (48 Eimer moeglich) ===' AS marke;
SELECT pm.MandantID                  AS mandant,
       COUNT(DISTINCT r.stunde) AS belegte_eimer,
       COUNT(*)                    AS rollupzeilen,
       SUM(r.anzahl)               AS nachrichten,
       COUNT(DISTINCT r.process_id) AS versch_prozesse
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE r.stunde >= '2025-12-28 05:00:00'
  AND r.stunde <  '2025-12-30 05:00:00'
GROUP BY pm.MandantID
ORDER BY nachrichten DESC;

SELECT '=== M95 P1 Laufzeit: Aufwaermlauf + fuenf ===' AS marke;
SELECT pm.MandantID                  AS mandant,
       COUNT(DISTINCT r.stunde) AS belegte_eimer,
       COUNT(*)                    AS rollupzeilen,
       SUM(r.anzahl)               AS nachrichten,
       COUNT(DISTINCT r.process_id) AS versch_prozesse
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE r.stunde >= '2025-12-28 05:00:00'
  AND r.stunde <  '2025-12-30 05:00:00'
GROUP BY pm.MandantID
ORDER BY nachrichten DESC;
SELECT pm.MandantID                  AS mandant,
       COUNT(DISTINCT r.stunde) AS belegte_eimer,
       COUNT(*)                    AS rollupzeilen,
       SUM(r.anzahl)               AS nachrichten,
       COUNT(DISTINCT r.process_id) AS versch_prozesse
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE r.stunde >= '2025-12-28 05:00:00'
  AND r.stunde <  '2025-12-30 05:00:00'
GROUP BY pm.MandantID
ORDER BY nachrichten DESC;
SELECT pm.MandantID                  AS mandant,
       COUNT(DISTINCT r.stunde) AS belegte_eimer,
       COUNT(*)                    AS rollupzeilen,
       SUM(r.anzahl)               AS nachrichten,
       COUNT(DISTINCT r.process_id) AS versch_prozesse
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE r.stunde >= '2025-12-28 05:00:00'
  AND r.stunde <  '2025-12-30 05:00:00'
GROUP BY pm.MandantID
ORDER BY nachrichten DESC;
SELECT pm.MandantID                  AS mandant,
       COUNT(DISTINCT r.stunde) AS belegte_eimer,
       COUNT(*)                    AS rollupzeilen,
       SUM(r.anzahl)               AS nachrichten,
       COUNT(DISTINCT r.process_id) AS versch_prozesse
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE r.stunde >= '2025-12-28 05:00:00'
  AND r.stunde <  '2025-12-30 05:00:00'
GROUP BY pm.MandantID
ORDER BY nachrichten DESC;
SELECT pm.MandantID                  AS mandant,
       COUNT(DISTINCT r.stunde) AS belegte_eimer,
       COUNT(*)                    AS rollupzeilen,
       SUM(r.anzahl)               AS nachrichten,
       COUNT(DISTINCT r.process_id) AS versch_prozesse
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE r.stunde >= '2025-12-28 05:00:00'
  AND r.stunde <  '2025-12-30 05:00:00'
GROUP BY pm.MandantID
ORDER BY nachrichten DESC;
SELECT pm.MandantID                  AS mandant,
       COUNT(DISTINCT r.stunde) AS belegte_eimer,
       COUNT(*)                    AS rollupzeilen,
       SUM(r.anzahl)               AS nachrichten,
       COUNT(DISTINCT r.process_id) AS versch_prozesse
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE r.stunde >= '2025-12-28 05:00:00'
  AND r.stunde <  '2025-12-30 05:00:00'
GROUP BY pm.MandantID
ORDER BY nachrichten DESC;

-- ══ M95 · 30 Tage / Tag · 30 Eimer im Fenster ═══════════════════
SELECT '=== M95 P2 Gegenprobe: Rollupbereich ohne Mandantenjoin ===' AS marke;
SELECT COUNT(*) AS rollupzeilen_gesamt, SUM(anzahl) AS nachrichten_gesamt,
       COUNT(DISTINCT process_id) AS versch_prozesse_gesamt
FROM overlord_monitor.message_rollup
WHERE stunde >= '2025-12-01 00:00:00' AND stunde < '2025-12-31 00:00:00';

SELECT '=== M95 P2 Ergebnis je Mandant (30 Eimer moeglich) ===' AS marke;
SELECT pm.MandantID                  AS mandant,
       COUNT(DISTINCT DATE(r.stunde)) AS belegte_eimer,
       COUNT(*)                    AS rollupzeilen,
       SUM(r.anzahl)               AS nachrichten,
       COUNT(DISTINCT r.process_id) AS versch_prozesse
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE r.stunde >= '2025-12-01 00:00:00'
  AND r.stunde <  '2025-12-31 00:00:00'
GROUP BY pm.MandantID
ORDER BY nachrichten DESC;

SELECT '=== M95 P2 Laufzeit: Aufwaermlauf + fuenf ===' AS marke;
SELECT pm.MandantID                  AS mandant,
       COUNT(DISTINCT DATE(r.stunde)) AS belegte_eimer,
       COUNT(*)                    AS rollupzeilen,
       SUM(r.anzahl)               AS nachrichten,
       COUNT(DISTINCT r.process_id) AS versch_prozesse
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE r.stunde >= '2025-12-01 00:00:00'
  AND r.stunde <  '2025-12-31 00:00:00'
GROUP BY pm.MandantID
ORDER BY nachrichten DESC;
SELECT pm.MandantID                  AS mandant,
       COUNT(DISTINCT DATE(r.stunde)) AS belegte_eimer,
       COUNT(*)                    AS rollupzeilen,
       SUM(r.anzahl)               AS nachrichten,
       COUNT(DISTINCT r.process_id) AS versch_prozesse
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE r.stunde >= '2025-12-01 00:00:00'
  AND r.stunde <  '2025-12-31 00:00:00'
GROUP BY pm.MandantID
ORDER BY nachrichten DESC;
SELECT pm.MandantID                  AS mandant,
       COUNT(DISTINCT DATE(r.stunde)) AS belegte_eimer,
       COUNT(*)                    AS rollupzeilen,
       SUM(r.anzahl)               AS nachrichten,
       COUNT(DISTINCT r.process_id) AS versch_prozesse
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE r.stunde >= '2025-12-01 00:00:00'
  AND r.stunde <  '2025-12-31 00:00:00'
GROUP BY pm.MandantID
ORDER BY nachrichten DESC;
SELECT pm.MandantID                  AS mandant,
       COUNT(DISTINCT DATE(r.stunde)) AS belegte_eimer,
       COUNT(*)                    AS rollupzeilen,
       SUM(r.anzahl)               AS nachrichten,
       COUNT(DISTINCT r.process_id) AS versch_prozesse
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE r.stunde >= '2025-12-01 00:00:00'
  AND r.stunde <  '2025-12-31 00:00:00'
GROUP BY pm.MandantID
ORDER BY nachrichten DESC;
SELECT pm.MandantID                  AS mandant,
       COUNT(DISTINCT DATE(r.stunde)) AS belegte_eimer,
       COUNT(*)                    AS rollupzeilen,
       SUM(r.anzahl)               AS nachrichten,
       COUNT(DISTINCT r.process_id) AS versch_prozesse
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE r.stunde >= '2025-12-01 00:00:00'
  AND r.stunde <  '2025-12-31 00:00:00'
GROUP BY pm.MandantID
ORDER BY nachrichten DESC;
SELECT pm.MandantID                  AS mandant,
       COUNT(DISTINCT DATE(r.stunde)) AS belegte_eimer,
       COUNT(*)                    AS rollupzeilen,
       SUM(r.anzahl)               AS nachrichten,
       COUNT(DISTINCT r.process_id) AS versch_prozesse
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE r.stunde >= '2025-12-01 00:00:00'
  AND r.stunde <  '2025-12-31 00:00:00'
GROUP BY pm.MandantID
ORDER BY nachrichten DESC;

-- ══ M95 · 12 Monate / Monat · 12 Eimer im Fenster ═══════════════════
SELECT '=== M95 P3 Gegenprobe: Rollupbereich ohne Mandantenjoin ===' AS marke;
SELECT COUNT(*) AS rollupzeilen_gesamt, SUM(anzahl) AS nachrichten_gesamt,
       COUNT(DISTINCT process_id) AS versch_prozesse_gesamt
FROM overlord_monitor.message_rollup
WHERE stunde >= '2025-01-01 00:00:00' AND stunde < '2026-01-01 00:00:00';

SELECT '=== M95 P3 Ergebnis je Mandant (12 Eimer moeglich) ===' AS marke;
SELECT pm.MandantID                  AS mandant,
       COUNT(DISTINCT DATE_FORMAT(r.stunde, '%Y-%m-01')) AS belegte_eimer,
       COUNT(*)                    AS rollupzeilen,
       SUM(r.anzahl)               AS nachrichten,
       COUNT(DISTINCT r.process_id) AS versch_prozesse
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE r.stunde >= '2025-01-01 00:00:00'
  AND r.stunde <  '2026-01-01 00:00:00'
GROUP BY pm.MandantID
ORDER BY nachrichten DESC;

SELECT '=== M95 P3 Laufzeit: Aufwaermlauf + fuenf ===' AS marke;
SELECT pm.MandantID                  AS mandant,
       COUNT(DISTINCT DATE_FORMAT(r.stunde, '%Y-%m-01')) AS belegte_eimer,
       COUNT(*)                    AS rollupzeilen,
       SUM(r.anzahl)               AS nachrichten,
       COUNT(DISTINCT r.process_id) AS versch_prozesse
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE r.stunde >= '2025-01-01 00:00:00'
  AND r.stunde <  '2026-01-01 00:00:00'
GROUP BY pm.MandantID
ORDER BY nachrichten DESC;
SELECT pm.MandantID                  AS mandant,
       COUNT(DISTINCT DATE_FORMAT(r.stunde, '%Y-%m-01')) AS belegte_eimer,
       COUNT(*)                    AS rollupzeilen,
       SUM(r.anzahl)               AS nachrichten,
       COUNT(DISTINCT r.process_id) AS versch_prozesse
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE r.stunde >= '2025-01-01 00:00:00'
  AND r.stunde <  '2026-01-01 00:00:00'
GROUP BY pm.MandantID
ORDER BY nachrichten DESC;
SELECT pm.MandantID                  AS mandant,
       COUNT(DISTINCT DATE_FORMAT(r.stunde, '%Y-%m-01')) AS belegte_eimer,
       COUNT(*)                    AS rollupzeilen,
       SUM(r.anzahl)               AS nachrichten,
       COUNT(DISTINCT r.process_id) AS versch_prozesse
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE r.stunde >= '2025-01-01 00:00:00'
  AND r.stunde <  '2026-01-01 00:00:00'
GROUP BY pm.MandantID
ORDER BY nachrichten DESC;
SELECT pm.MandantID                  AS mandant,
       COUNT(DISTINCT DATE_FORMAT(r.stunde, '%Y-%m-01')) AS belegte_eimer,
       COUNT(*)                    AS rollupzeilen,
       SUM(r.anzahl)               AS nachrichten,
       COUNT(DISTINCT r.process_id) AS versch_prozesse
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE r.stunde >= '2025-01-01 00:00:00'
  AND r.stunde <  '2026-01-01 00:00:00'
GROUP BY pm.MandantID
ORDER BY nachrichten DESC;
SELECT pm.MandantID                  AS mandant,
       COUNT(DISTINCT DATE_FORMAT(r.stunde, '%Y-%m-01')) AS belegte_eimer,
       COUNT(*)                    AS rollupzeilen,
       SUM(r.anzahl)               AS nachrichten,
       COUNT(DISTINCT r.process_id) AS versch_prozesse
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE r.stunde >= '2025-01-01 00:00:00'
  AND r.stunde <  '2026-01-01 00:00:00'
GROUP BY pm.MandantID
ORDER BY nachrichten DESC;
SELECT pm.MandantID                  AS mandant,
       COUNT(DISTINCT DATE_FORMAT(r.stunde, '%Y-%m-01')) AS belegte_eimer,
       COUNT(*)                    AS rollupzeilen,
       SUM(r.anzahl)               AS nachrichten,
       COUNT(DISTINCT r.process_id) AS versch_prozesse
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE r.stunde >= '2025-01-01 00:00:00'
  AND r.stunde <  '2026-01-01 00:00:00'
GROUP BY pm.MandantID
ORDER BY nachrichten DESC;

SELECT '=== 99 Laufzeiten ===' AS marke;
SHOW PROFILES;

SELECT '=== 99 read_only am Ende der Sitzung ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;
