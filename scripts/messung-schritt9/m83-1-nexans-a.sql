-- Messung M83-1 — Fassung A (EXISTS je Prozess), Mandant NEXANS
-- Auftrag:  "Messrunde M83 — die Bestandsabfrage vor E14", Fassung 1, 21.08.2026
-- Ergebnis: docs/messungen-schritt9.md, Nachtrag vom 21.08.2026 — M83
--
-- Ausschliesslich SELECT / SET / EXPLAIN / SHOW. (Regel S1)
-- Kein STRAIGHT_JOIN, auch nicht probeweise (M42, M47).
-- G1: kein Partnername, keine ProjectID, keine ProcessID in dieser Datei. MandantID ist
--     der Mandant selbst und wird vom Auftrag (L7) ausdruecklich benannt.
--
-- REIHENFOLGE IST TEIL DER MESSUNG: Lauf 1 ist der erste Zugriff dieser Sitzung. Er wird
-- nach M83-4 getrennt ausgewiesen und ist die UNTERE Schranke des Kaltfalls, nicht dessen
-- Obergrenze. Die Laeufe 2 bis 6 tragen die "beste von fuenf". Das EXPLAIN steht am ENDE
-- der Sitzung, damit Lauf 1 wirklich der erste Zugriff ist.

SET profiling = 1;
SET profiling_history_size = 100;
SET max_statement_time = 60;
SET @mandant = 'NEXANS';

SELECT 'A1-erstlauf' AS marke;
SELECT p.ProcessID,
       EXISTS (SELECT 1 FROM GlassfishDB.Message m WHERE m.ProcessID = p.ProcessID) AS traegt
FROM GlassfishDB.ProjectMandant pm
JOIN GlassfishDB.Process     p  ON p.ProjectID = pm.ProjectID
WHERE pm.MandantID = @mandant;

SELECT 'A2-wiederholung' AS marke;
SELECT p.ProcessID,
       EXISTS (SELECT 1 FROM GlassfishDB.Message m WHERE m.ProcessID = p.ProcessID) AS traegt
FROM GlassfishDB.ProjectMandant pm
JOIN GlassfishDB.Process     p  ON p.ProjectID = pm.ProjectID
WHERE pm.MandantID = @mandant;

SELECT 'A3-wiederholung' AS marke;
SELECT p.ProcessID,
       EXISTS (SELECT 1 FROM GlassfishDB.Message m WHERE m.ProcessID = p.ProcessID) AS traegt
FROM GlassfishDB.ProjectMandant pm
JOIN GlassfishDB.Process     p  ON p.ProjectID = pm.ProjectID
WHERE pm.MandantID = @mandant;

SELECT 'A4-wiederholung' AS marke;
SELECT p.ProcessID,
       EXISTS (SELECT 1 FROM GlassfishDB.Message m WHERE m.ProcessID = p.ProcessID) AS traegt
FROM GlassfishDB.ProjectMandant pm
JOIN GlassfishDB.Process     p  ON p.ProjectID = pm.ProjectID
WHERE pm.MandantID = @mandant;

SELECT 'A5-wiederholung' AS marke;
SELECT p.ProcessID,
       EXISTS (SELECT 1 FROM GlassfishDB.Message m WHERE m.ProcessID = p.ProcessID) AS traegt
FROM GlassfishDB.ProjectMandant pm
JOIN GlassfishDB.Process     p  ON p.ProjectID = pm.ProjectID
WHERE pm.MandantID = @mandant;

SELECT 'A6-wiederholung' AS marke;
SELECT p.ProcessID,
       EXISTS (SELECT 1 FROM GlassfishDB.Message m WHERE m.ProcessID = p.ProcessID) AS traegt
FROM GlassfishDB.ProjectMandant pm
JOIN GlassfishDB.Process     p  ON p.ProjectID = pm.ProjectID
WHERE pm.MandantID = @mandant;

-- L15 — EXPLAIN zu dieser Fassung.
SELECT 'A-explain' AS marke;
EXPLAIN
SELECT p.ProcessID,
       EXISTS (SELECT 1 FROM GlassfishDB.Message m WHERE m.ProcessID = p.ProcessID) AS traegt
FROM GlassfishDB.ProjectMandant pm
JOIN GlassfishDB.Process     p  ON p.ProjectID = pm.ProjectID
WHERE pm.MandantID = @mandant;

SHOW PROFILES;
