-- Messung M83-2 — Fassung B (lebende Prozesse, Komplement im Dienst), Mandant NEXANS
-- Auftrag:  "Messrunde M83 — die Bestandsabfrage vor E14", Fassung 1, 21.08.2026
-- Ergebnis: docs/messungen-schritt9.md, Nachtrag vom 21.08.2026 — M83
--
-- Ausschliesslich SELECT / SET / EXPLAIN / SHOW. (Regel S1)
-- Kein STRAIGHT_JOIN, auch nicht probeweise (M42, M47).
-- G1: kein Partnername, keine ProjectID, keine ProcessID in dieser Datei. MandantID ist
--     der Mandant selbst und wird vom Auftrag (L7) ausdruecklich benannt.
--
-- REIHENFOLGE IST TEIL DER MESSUNG: Lauf 1 ist der erste Zugriff DIESER Sitzung, aber
-- nicht der erste der Runde — die vorher gelaufenen Sitzungen haben den Puffer bereits
-- angewaermt. Der Wert liegt damit noch weiter vom echten Kaltlauf entfernt als der aus
-- der ersten Sitzung und wird so ausgewiesen. Das EXPLAIN steht am ENDE der Sitzung.

SET profiling = 1;
SET profiling_history_size = 100;
SET max_statement_time = 60;
SET @mandant = 'NEXANS';

SELECT 'B1-erstlauf' AS marke;
SELECT DISTINCT m.ProcessID
FROM GlassfishDB.Message        m
JOIN GlassfishDB.Process        p  ON p.ProcessID = m.ProcessID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = @mandant;

SELECT 'B2-wiederholung' AS marke;
SELECT DISTINCT m.ProcessID
FROM GlassfishDB.Message        m
JOIN GlassfishDB.Process        p  ON p.ProcessID = m.ProcessID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = @mandant;

SELECT 'B3-wiederholung' AS marke;
SELECT DISTINCT m.ProcessID
FROM GlassfishDB.Message        m
JOIN GlassfishDB.Process        p  ON p.ProcessID = m.ProcessID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = @mandant;

SELECT 'B4-wiederholung' AS marke;
SELECT DISTINCT m.ProcessID
FROM GlassfishDB.Message        m
JOIN GlassfishDB.Process        p  ON p.ProcessID = m.ProcessID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = @mandant;

SELECT 'B5-wiederholung' AS marke;
SELECT DISTINCT m.ProcessID
FROM GlassfishDB.Message        m
JOIN GlassfishDB.Process        p  ON p.ProcessID = m.ProcessID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = @mandant;

SELECT 'B6-wiederholung' AS marke;
SELECT DISTINCT m.ProcessID
FROM GlassfishDB.Message        m
JOIN GlassfishDB.Process        p  ON p.ProcessID = m.ProcessID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = @mandant;

-- L15 — EXPLAIN zu dieser Fassung.
SELECT 'B-explain' AS marke;
EXPLAIN
SELECT DISTINCT m.ProcessID
FROM GlassfishDB.Message        m
JOIN GlassfishDB.Process        p  ON p.ProcessID = m.ProcessID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = @mandant;

SHOW PROFILES;
