-- Messung M191 -- Sitzung 1: die Statuszaehlung ueber den ganzen Bestand
-- Auftrag:  "EERP_FAILED als Fehlerstatus einordnen", 13.10.2026
-- Ergebnis: docs/message-status.md, Abschnitt "EERP_FAILED"
--
-- Ausschliesslich SELECT / SET. (Regel S1) Benutzer: der Lese-Benutzer.
-- G1: keine Zugangsdaten, kein Hostname. Die Ausgabe traegt nur Statuswerte und
--     Zahlen, keine Prozess- oder Partnerkennung.
-- T1: Es wird nichts zugesichert; gezaehlt wird.
--
-- Block 01 ist die Zaehlung des Auftrags im Wortlaut. Block 02 ist die
-- Gegenprobe dazu: Die Spalte sortiert mit utf8mb4_general_ci (PAD SPACE), und
-- GROUP BY MessageStatus legt damit Schreibweisen und angehaengte Leerzeichen
-- zusammen. Ein Wert wie 'eerp_failed' oder 'EERP_FAILED ' erschiene in 01 nicht
-- als eigene Zeile. Block 02 gruppiert deshalb bytegenau ueber HEX().

SELECT '=== 00 read_only, Serverzeit, Datenstand (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit, VERSION() AS version,
       @@max_statement_time AS grenze_vorher;

SET max_statement_time = 120;

SELECT MIN(MessageLastUpdate) AS fruehester, MAX(MessageLastUpdate) AS datenstand
FROM `GlassfishDB`.`Message`;

SELECT '=== 01 Die Zaehlung des Auftrags, im Wortlaut ===' AS marke;
SELECT MessageStatus, COUNT(*) FROM GlassfishDB.Message GROUP BY MessageStatus;

SELECT '=== 01a Summe und Zahl der Gruppen aus 01, zaehlend ===' AS marke;
SELECT COUNT(*) AS gruppen, SUM(n) AS zeilen
FROM (SELECT MessageStatus, COUNT(*) AS n FROM `GlassfishDB`.`Message` GROUP BY MessageStatus) g;

SELECT '=== 02 Gegenprobe bytegenau: GROUP BY HEX(MessageStatus) ===' AS marke;
SELECT MIN(MessageStatus) AS status, HEX(MessageStatus) AS hex_wert,
       CHAR_LENGTH(MIN(MessageStatus)) AS zeichen, COUNT(*) AS anzahl
FROM `GlassfishDB`.`Message`
GROUP BY HEX(MessageStatus)
ORDER BY anzahl DESC, hex_wert;

SELECT '=== 02a Summe und Zahl der Gruppen aus 02, zaehlend ===' AS marke;
SELECT COUNT(*) AS gruppen, SUM(n) AS zeilen
FROM (SELECT HEX(MessageStatus) AS h, COUNT(*) AS n FROM `GlassfishDB`.`Message`
      GROUP BY HEX(MessageStatus)) g;

SELECT '=== 03 Gezielt: EERP_FAILED unter der Spaltensortierung, und jeder Wert mit FAIL oder EERP ===' AS marke;
SELECT COUNT(*) AS eerp_failed_gleich
FROM `GlassfishDB`.`Message` WHERE MessageStatus = 'EERP_FAILED';
SELECT MessageStatus, HEX(MessageStatus) AS hex_wert, COUNT(*) AS anzahl
FROM `GlassfishDB`.`Message`
WHERE MessageStatus LIKE '%FAIL%' OR MessageStatus LIKE 'EERP%'
GROUP BY HEX(MessageStatus), MessageStatus
ORDER BY MessageStatus;

SELECT '=== 99 Ende ===' AS marke;
