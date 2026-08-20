-- Messung M80, Nachlauf — der Umfang der Antworten, den M80 misst.
-- Ausschliesslich SELECT. (Regel S1) G1: nur Anzahlen, kein einziger Partnername.
SELECT 'M80b Umfang' AS marke;
SELECT pm.MandantID AS mandant,
       COUNT(*)                                   AS prozesse,
       COUNT(DISTINCT c.partner)                  AS verschiedene_partner,
       SUM(c.partner IS NOT NULL)                 AS zeilen_mit_partner,
       SUM(c.richtung IS NOT NULL)                AS zeilen_mit_richtung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
GROUP BY pm.MandantID
ORDER BY prozesse DESC;
