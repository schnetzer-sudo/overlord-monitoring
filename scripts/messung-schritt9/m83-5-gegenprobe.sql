-- Messung M83-5 — Gegenprobe: liefern beide Fassungen dieselbe Menge wie M74b?
-- Auftrag:  "Messrunde M83 — die Bestandsabfrage vor E14", Fassung 1, 21.08.2026
-- Anlass:   Die gezaehlten Ergebniszeilen aus M83-1/M83-2 sind fuer NEXANS 734 bzw. 517.
--           M74b (B) fuehrt 733 bzw. 516 bei gleichem "ohne" = 217. Der Auftrag sagt:
--           "Eine Abweichung ist ein Fehler in der Abfrage und nicht in M74b." Also wird
--           hier die Abfrage zerlegt, nicht M74b angezweifelt.
-- Ergebnis: docs/messungen-schritt9.md, Nachtrag vom 21.08.2026 — M83, Abschnitt M83-5
--
-- Ausschliesslich SELECT / SET. (Regel S1)
-- G1: Ausgabe sind ausschliesslich ZAHLEN. Keine ProcessID, keine ProjectID, kein
--     Partnername verlaesst diese Sitzung.

SET max_statement_time = 60;

-- 1. Zeilen und verschiedene Prozesse ueber den Weg der Fassung A (ProjectMandant -> Process).
--    Weichen die beiden Spalten voneinander ab, erzeugt der Join Doubletten.
SELECT pm.MandantID AS mandant,
       COUNT(*)                    AS zeilen_fassung_a,
       COUNT(DISTINCT p.ProcessID) AS verschiedene_prozesse
FROM GlassfishDB.ProjectMandant pm
JOIN GlassfishDB.Process p ON p.ProjectID = pm.ProjectID
WHERE pm.MandantID IN ('NEXANS','SUTTONS')
GROUP BY pm.MandantID ORDER BY pm.MandantID;

-- 2. Derselbe Bestand ueber den Umweg Project. Faellt hier eine Zeile weg, ist die
--    Projektzeile in ProjectMandant verwaist und M74b hat ueber Project gezaehlt.
SELECT pm.MandantID AS mandant,
       COUNT(*)                    AS zeilen_ueber_project,
       COUNT(DISTINCT p.ProcessID) AS verschiedene_prozesse
FROM GlassfishDB.ProjectMandant pm
JOIN GlassfishDB.Project pr ON pr.ProjectID = pm.ProjectID
JOIN GlassfishDB.Process p  ON p.ProjectID  = pr.ProjectID
WHERE pm.MandantID IN ('NEXANS','SUTTONS')
GROUP BY pm.MandantID ORDER BY pm.MandantID;

-- 3. Verwaiste Projektzuordnungen: ProjectMandant-Zeilen ohne Project-Zeile.
SELECT pm.MandantID AS mandant, COUNT(*) AS verwaiste_projektzuordnungen
FROM GlassfishDB.ProjectMandant pm
LEFT JOIN GlassfishDB.Project pr ON pr.ProjectID = pm.ProjectID
WHERE pr.ProjectID IS NULL
GROUP BY pm.MandantID ORDER BY pm.MandantID;

-- 4. Die drei Zahlen der Gegenprobe je Mandant, in der Form der M74b-Tabelle (B).
SELECT pm.MandantID AS mandant,
       COUNT(*) AS prozesse,
       SUM(EXISTS (SELECT 1 FROM GlassfishDB.Message m WHERE m.ProcessID = p.ProcessID)) AS mit_nachrichten,
       SUM(NOT EXISTS (SELECT 1 FROM GlassfishDB.Message m WHERE m.ProcessID = p.ProcessID)) AS ohne
FROM GlassfishDB.ProjectMandant pm
JOIN GlassfishDB.Process p ON p.ProjectID = pm.ProjectID
GROUP BY pm.MandantID ORDER BY pm.MandantID;

-- 5. Gesamtbild ohne Mandantenfilter — die Ebene, auf der M74b gemessen hat.
SELECT COUNT(*) AS prozesse_gesamt,
       SUM(EXISTS (SELECT 1 FROM GlassfishDB.Message m WHERE m.ProcessID = p.ProcessID)) AS mit_nachrichten,
       SUM(NOT EXISTS (SELECT 1 FROM GlassfishDB.Message m WHERE m.ProcessID = p.ProcessID)) AS ohne
FROM GlassfishDB.Process p;

-- 6. Wie viele Prozesse haengen an gar keinem Mandanten, wie viele an mehr als einem?
--    Ein Prozess an zwei Mandanten erscheint in der je-Mandant-Summe zweimal.
SELECT zuordnungen, COUNT(*) AS prozesse
FROM (SELECT p.ProcessID,
             (SELECT COUNT(*) FROM GlassfishDB.ProjectMandant pm
               WHERE pm.ProjectID = p.ProjectID) AS zuordnungen
      FROM GlassfishDB.Process p) t
GROUP BY zuordnungen ORDER BY zuordnungen;

-- 7. Prozesse mit NULL-ProjectID (haengen an keinem Projekt und damit an keinem Mandanten).
SELECT SUM(ProjectID IS NULL) AS prozesse_ohne_projekt, COUNT(*) AS prozesse_gesamt
FROM GlassfishDB.Process;
