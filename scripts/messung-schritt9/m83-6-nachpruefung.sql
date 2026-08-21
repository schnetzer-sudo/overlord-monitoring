-- Messung M83-6 — Nachpruefung zweier Behauptungen des Auftrags selbst (Regel L10)
-- Auftrag:  "Messrunde M83 — die Bestandsabfrage vor E14", Fassung 1, 21.08.2026
-- Anlass:   M83-2 begruendet die Kosten der Fassung B mit dem Satz "bei NEXANS ist das der
--           Bestand, in dem ein einzelner Prozess 1.472.788 Zeilen haelt". Die ZAHL ist in
--           M74b belegt, die ZUORDNUNG zu NEXANS steht dort nirgends. Sie wird hier gemessen.
-- Ergebnis: docs/messungen-schritt9.md, Nachtrag vom 21.08.2026 — M83, Abschnitt M83-6
--
-- Ausschliesslich SELECT / SET. (Regel S1)
-- L9 — Vollzugriff auf Message mit Begruendung: Die Frage ist nur ueber eine vollstaendige
--      Gruppierung je ProcessID zu beantworten; ein Zeitfenster wuerde sie verfaelschen.
--      Einmalige Messung, kein Anwendungscode.
-- G1: Ausgabe sind Mandantenkuerzel und Zahlen. Keine ProcessID, keine ProjectID, kein
--     Partnername. Mandantenkuerzel sind nach annahmen-korrekturen.md nicht geschuetzt.

SET profiling = 1;
SET profiling_history_size = 100;
SET max_statement_time = 60;

-- 1. Zu welchem Mandanten gehoert der groesste Prozess?
SELECT pm.MandantID AS mandant, t.n AS nachrichten,
       ROUND(100.0 * t.n / 3341519, 2) AS anteil_prozent
FROM (SELECT ProcessID, COUNT(*) AS n
      FROM GlassfishDB.Message
      GROUP BY ProcessID ORDER BY n DESC LIMIT 1) t
JOIN GlassfishDB.Process        p  ON p.ProcessID = t.ProcessID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID;

-- 2. Nachrichten je Mandant — die Groesse, an der die Kosten der Fassung B haengen.
SELECT pm.MandantID AS mandant,
       COUNT(*)                    AS nachrichten,
       COUNT(DISTINCT m.ProcessID) AS prozesse_mit_nachrichten
FROM GlassfishDB.Message        m
JOIN GlassfishDB.Process        p  ON p.ProcessID = m.ProcessID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
GROUP BY pm.MandantID ORDER BY nachrichten DESC;

SHOW PROFILES;
