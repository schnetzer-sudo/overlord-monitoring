-- M162 — Deckung der drei neuen Namen. Statement aus M156 (Fassung B), unveraendert bis auf die
-- Namensliste und die Anteile, die hier im Statement gerechnet werden (Nenner aus M162-0/-3).
-- Fenster B: 2025-11-30 bis 2025-12-30, je Mandant, getrennt nach Kettenstellung.
-- Erhebung: max_statement_time = 180 wie s11/s12 der Hauptrunde (Abweichung von §4, gemeldet).
SELECT @@global.read_only AS globalReadOnly;
SET max_statement_time = 180;
SET profiling = 1;
SET profiling_history_size = 100;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;

SELECT '--- M162-0 Nenner NEXANS: Nachrichten, Wurzeln, Kinder in Fenster B (Rollen wie M28-1/M156) ---' AS marke;
SELECT COUNT(*), SUM(m.Source = 1), SUM(m.SourceMessageID IS NOT NULL AND m.SourceMessageID <> '')
  INTO @nN, @wN, @kN
  FROM GlassfishDB.Message m
 WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
   AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
   AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                 JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS');
SELECT @nN AS nachrichten, @wN AS wurzeln, @kN AS kinder;

SELECT '--- M162-1 EXPLAIN NEXANS ---' AS marke;
EXPLAIN
SELECT p.MessagePropertyName,
       COUNT(*) AS zeilen,
       COUNT(DISTINCT m.MessageID) AS belegt,
       COUNT(DISTINCT CASE WHEN m.Source = 1 THEN m.MessageID END) AS belegtWurzel,
       COUNT(DISTINCT CASE WHEN m.SourceMessageID IS NOT NULL AND m.SourceMessageID <> ''
                           THEN m.MessageID END) AS belegtKind
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.MessageProperty p
    ON p.MessageID = m.MessageID
   AND p.MessagePropertyName IN ('Message.DestinationFilename','Message.SNDPRN','Message.VFN')
 WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
   AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
   AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                 JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
 GROUP BY p.MessagePropertyName;

SELECT '--- M162-2 NEXANS ---' AS marke;
SELECT p.MessagePropertyName,
       COUNT(*) AS zeilen,
       COUNT(DISTINCT m.MessageID) AS belegt,
       ROUND(100.0 * COUNT(DISTINCT m.MessageID) / @nN, 3) AS anteil,
       COUNT(DISTINCT CASE WHEN m.Source = 1 THEN m.MessageID END) AS belegtWurzel,
       ROUND(100.0 * COUNT(DISTINCT CASE WHEN m.Source = 1 THEN m.MessageID END) / @wN, 3) AS jeWurzel,
       COUNT(DISTINCT CASE WHEN m.SourceMessageID IS NOT NULL AND m.SourceMessageID <> ''
                           THEN m.MessageID END) AS belegtKind,
       ROUND(100.0 * COUNT(DISTINCT CASE WHEN m.SourceMessageID IS NOT NULL AND m.SourceMessageID <> ''
                           THEN m.MessageID END) / @kN, 3) AS jeKind
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.MessageProperty p
    ON p.MessageID = m.MessageID
   AND p.MessagePropertyName IN ('Message.DestinationFilename','Message.SNDPRN','Message.VFN')
 WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
   AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
   AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                 JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
 GROUP BY p.MessagePropertyName;

SELECT '--- M162-3 Nenner SUTTONS ---' AS marke;
SELECT COUNT(*), SUM(m.Source = 1), SUM(m.SourceMessageID IS NOT NULL AND m.SourceMessageID <> '')
  INTO @nS, @wS, @kS
  FROM GlassfishDB.Message m
 WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
   AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
   AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                 JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'SUTTONS');
SELECT @nS AS nachrichten, @wS AS wurzeln, @kS AS kinder;

SELECT '--- M162-4 EXPLAIN SUTTONS ---' AS marke;
EXPLAIN
SELECT p.MessagePropertyName,
       COUNT(*) AS zeilen,
       COUNT(DISTINCT m.MessageID) AS belegt,
       COUNT(DISTINCT CASE WHEN m.Source = 1 THEN m.MessageID END) AS belegtWurzel,
       COUNT(DISTINCT CASE WHEN m.SourceMessageID IS NOT NULL AND m.SourceMessageID <> ''
                           THEN m.MessageID END) AS belegtKind
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.MessageProperty p
    ON p.MessageID = m.MessageID
   AND p.MessagePropertyName IN ('Message.DestinationFilename','Message.SNDPRN','Message.VFN')
 WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
   AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
   AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                 JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'SUTTONS')
 GROUP BY p.MessagePropertyName;

SELECT '--- M162-5 SUTTONS (fehlende Zeile = 0) ---' AS marke;
SELECT p.MessagePropertyName,
       COUNT(*) AS zeilen,
       COUNT(DISTINCT m.MessageID) AS belegt,
       ROUND(100.0 * COUNT(DISTINCT m.MessageID) / @nS, 3) AS anteil,
       COUNT(DISTINCT CASE WHEN m.Source = 1 THEN m.MessageID END) AS belegtWurzel,
       ROUND(100.0 * COUNT(DISTINCT CASE WHEN m.Source = 1 THEN m.MessageID END) / @wS, 3) AS jeWurzel,
       COUNT(DISTINCT CASE WHEN m.SourceMessageID IS NOT NULL AND m.SourceMessageID <> ''
                           THEN m.MessageID END) AS belegtKind,
       ROUND(100.0 * COUNT(DISTINCT CASE WHEN m.SourceMessageID IS NOT NULL AND m.SourceMessageID <> ''
                           THEN m.MessageID END) / @kS, 3) AS jeKind
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.MessageProperty p
    ON p.MessageID = m.MessageID
   AND p.MessagePropertyName IN ('Message.DestinationFilename','Message.SNDPRN','Message.VFN')
 WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
   AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
   AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                 JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = 'SUTTONS')
 GROUP BY p.MessagePropertyName;

SELECT '--- Laufzeiten je Statement der Sitzung (Einzellauf, Erhebung) ---' AS marke;
SELECT QUERY_ID, ROUND(SUM(DURATION), 3) AS sekunden
  FROM information_schema.PROFILING WHERE QUERY_ID > @basis + 1 GROUP BY QUERY_ID ORDER BY QUERY_ID;
