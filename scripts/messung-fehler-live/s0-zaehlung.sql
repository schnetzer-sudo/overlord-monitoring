-- Messung M188 -- Sitzung 0: die Zaehlung fuer Tor 1
-- Auftrag:  "Fehler live, Teil A (Baustein und Uebersicht)", Stand 18.09.2026
-- Vorregistrierung und Ergebnis: docs/fehler-live.md Paragraf 8
--
-- ERZEUGT von scripts/messung-fehler-live/erzeuge.py. Die Statementtexte sind aus
-- dem Code gerendert; eingesetzt sind nur Fenster und Mandant.
--
-- Ausschliesslich SELECT / SET / EXPLAIN. (Regel S1) Benutzer: der Lese-Benutzer.
-- G1: keine Zugangsdaten, kein Hostname. Mandanten-IDs sind Codes. Die
--     Rohausgabe traegt Prozesskennungen und bleibt deshalb ausserhalb des
--     Repositorys (ergebnis/*.txt); eingecheckt wird das gefilterte Protokoll.
-- L9: SET max_statement_time = 60 fuer diese Sitzung.
-- T1: Zeiten werden ausgegeben, nichts wird zugesichert.

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit, VERSION() AS version,
       @@max_statement_time AS grenze_vorher;

SET max_statement_time = 60;
SET profiling = 1;
SET profiling_history_size = 100;

SELECT '=== 01 Die Zaehlung: Fehlerzeilen und Nachrichten je Mandant, ganzer Bestand ===' AS marke;
SELECT md.MandantID AS mandant,
       (SELECT COUNT(*) FROM `GlassfishDB`.`Message` m
         WHERE (m.MessageStatus LIKE 'ERROR\\_%' ESCAPE '\\' OR m.MessageStatus = 'COMMIT_REJECTED')
           AND EXISTS (SELECT 1 FROM `GlassfishDB`.`Process` p
                         JOIN `GlassfishDB`.`ProjectMandant` pm ON pm.ProjectID = p.ProjectID
                        WHERE p.ProcessID = m.ProcessID AND pm.MandantID = md.MandantID))
         AS fehlerzeilen,
       (SELECT COALESCE(SUM(r.anzahl), 0) FROM `overlord_monitor`.`message_rollup` r
         WHERE EXISTS (SELECT 1 FROM `GlassfishDB`.`Process` p
                         JOIN `GlassfishDB`.`ProjectMandant` pm ON pm.ProjectID = p.ProjectID
                        WHERE p.ProcessID = r.process_id AND pm.MandantID = md.MandantID))
         AS nachrichten
FROM `GlassfishDB`.`Mandant` md
ORDER BY fehlerzeilen, nachrichten DESC, md.MandantID;

SELECT '=== 01a Die Deckung: message_rollup gegen Message, ganzer Bestand ===' AS marke;
SELECT (SELECT SUM(anzahl) FROM `overlord_monitor`.`message_rollup`) AS rollup_gesamt,
       (SELECT COUNT(*) FROM `GlassfishDB`.`Message`) AS message_gesamt;

SELECT '=== 01b Die Fehlerzeilen im ganzen Bestand, Rohwert x Einordnung ===' AS marke;
SELECT m.MessageStatus AS rohwert, COUNT(*) AS anzahl
FROM `GlassfishDB`.`Message` m
WHERE m.MessageStatus LIKE 'ERROR\\_%' ESCAPE '\\' OR m.MessageStatus = 'COMMIT_REJECTED'
GROUP BY m.MessageStatus ORDER BY m.MessageStatus;
