-- Messung M189 -- Sitzung 3: die Zaehlung fuer Tor 1
-- Auftrag:  "Fehler live, Teil B (Prozessbaum)", Stand 18.09.2026
-- Vorregistrierung und Ergebnis: docs/fehler-live.md Paragraf 5b
--
-- ERZEUGT von scripts/messung-fehler-live/erzeuge_m189.py. Die Lesung ist aus dem Code
-- gerendert; eingesetzt sind nur Fenster und Mandant.
--
-- Ausschliesslich SELECT / SET / EXPLAIN. (Regel S1) Benutzer: der Lese-Benutzer.
-- G1: keine Zugangsdaten, kein Hostname. Mandanten-IDs sind Codes. Die
--     Rohausgabe traegt Prozesskennungen und bleibt deshalb ausserhalb des
--     Repositorys (ergebnis/s[0-9].txt); eingecheckt wird das gefilterte Protokoll.
-- L9: SET max_statement_time = 60 fuer diese Sitzung.
-- T1: Zeiten werden ausgegeben, nichts wird zugesichert.

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit, VERSION() AS version,
       @@max_statement_time AS grenze_vorher;

SET max_statement_time = 60;
SET profiling = 1;
SET profiling_history_size = 100;

SELECT '=== 01 Die Zaehlung: das Jahresfenster mit den meisten NEXANS-Fehlern ===' AS marke;
WITH je_stunde AS (
  SELECT CAST(DATE_FORMAT(m.MessageLastUpdate, '%Y-%m-%d %H:00:00') AS DATETIME) AS stunde,
         COUNT(*) AS fehler
  FROM `GlassfishDB`.`Message` m
  WHERE (m.MessageStatus LIKE 'ERROR\\_%' ESCAPE '\\' OR m.MessageStatus = 'COMMIT_REJECTED')
    AND EXISTS (SELECT 1 FROM `GlassfishDB`.`Process` p
                  JOIN `GlassfishDB`.`ProjectMandant` pm ON pm.ProjectID = p.ProjectID
                 WHERE p.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
  GROUP BY CAST(DATE_FORMAT(m.MessageLastUpdate, '%Y-%m-%d %H:00:00') AS DATETIME)
)
SELECT a.stunde AS von, a.stunde + INTERVAL 1 YEAR AS bis_ausschliessend,
       SUM(b.fehler) AS fehler_im_fenster, COUNT(*) AS fehlerstunden_im_fenster
FROM je_stunde a
JOIN je_stunde b ON b.stunde >= a.stunde AND b.stunde < a.stunde + INTERVAL 1 YEAR
GROUP BY a.stunde
ORDER BY fehler_im_fenster DESC, a.stunde ASC
LIMIT 5;

SELECT '=== 01a Eichung der Zaehlung: zwei Zahlen aus M188 ===' AS marke;
SELECT 'M189-ZAEHLUNG-EICHUNG' AS fall,
       COUNT(*) AS fehler_nexans_ganzer_bestand,
       SUM(m.MessageLastUpdate >= '2023-11-01 00:00:00' AND m.MessageLastUpdate < '2024-11-01 00:00:00')
         AS fehler_bis_2024_11_01,
       COUNT(DISTINCT DATE_FORMAT(m.MessageLastUpdate, '%Y-%m-%d %H:00:00')) AS fehlerstunden
FROM `GlassfishDB`.`Message` m
WHERE (m.MessageStatus LIKE 'ERROR\\_%' ESCAPE '\\' OR m.MessageStatus = 'COMMIT_REJECTED')
  AND EXISTS (SELECT 1 FROM `GlassfishDB`.`Process` p
                JOIN `GlassfishDB`.`ProjectMandant` pm ON pm.ProjectID = p.ProjectID
               WHERE p.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS');
