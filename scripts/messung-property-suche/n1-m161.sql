-- M161 — Ist-Stand der Konfigurationstabelle nach dem Handabgleich vom 08.09.2026
-- Feldnamen sind Konfiguration und duerfen stehen (G1). Kein MessagePropertyValue in der Ausgabe.
SELECT @@global.read_only AS globalReadOnly;
SET max_statement_time = 60;

SELECT '--- M161-1 Zeilenzahl gezaehlt (COUNT(*), nie TABLE_ROWS) und verschiedene Namen ---' AS marke;
SELECT COUNT(*) AS zeilen, COUNT(DISTINCT MessagePropertyName) AS verschiedeneNamen
  FROM GlassfishDB.MessagePropertySearchListEntry;

SELECT '--- M161-2 Kreuztabelle Typ x Mandant, NULL als eigene Zeile ---' AS marke;
SELECT MessagePropertyType, MandantID, COUNT(*) AS zeilen
  FROM GlassfishDB.MessagePropertySearchListEntry
 GROUP BY MessagePropertyType, MandantID
 ORDER BY MessagePropertyType, MandantID;

SELECT '--- M161-3 vollstaendige Auflistung, alle Zeilen ---' AS marke;
SELECT MessagePropertyName, MandantID, MessagePropertyType
  FROM GlassfishDB.MessagePropertySearchListEntry
 ORDER BY MessagePropertyType, MessagePropertyName;

SELECT '--- M161-4 Verbleib von Service.Type ---' AS marke;
SELECT COUNT(*) AS zeilenServiceType
  FROM GlassfishDB.MessagePropertySearchListEntry
 WHERE MessagePropertyName = 'Service.Type';

SELECT '--- M161-5 Gegenprobe: NULL wirklich NULL, nicht leerer String ---' AS marke;
SELECT SUM(MandantID IS NULL) AS mandantNull,
       SUM(MandantID = '') AS mandantLeer,
       SUM(MessagePropertyType IS NULL) AS typNull
  FROM GlassfishDB.MessagePropertySearchListEntry;

SELECT '--- M161-6 Tabellenstatus (CREATE_TIME, UPDATE_TIME als Hinweis auf den Handabgleich) ---' AS marke;
SELECT CREATE_TIME, UPDATE_TIME, TABLE_ROWS AS schaetzungNichtVerwendet
  FROM information_schema.TABLES
 WHERE TABLE_SCHEMA = 'GlassfishDB' AND TABLE_NAME = 'MessagePropertySearchListEntry';

SELECT '--- M161-7 Gegenprobe der Typ-Lesart (M155-Form): kommen die drei neuen Namen in MessageProperty vor? ---' AS marke;
EXPLAIN SELECT 1 FROM GlassfishDB.MessageProperty WHERE MessagePropertyName = 'Message.DestinationFilename' LIMIT 1;
SELECT 'Message.DestinationFilename' AS name,
       (SELECT COUNT(*) FROM (SELECT 1 FROM GlassfishDB.MessageProperty
                               WHERE MessagePropertyName = 'Message.DestinationFilename' LIMIT 1) t) AS kommtVor;
SELECT 'Message.SNDPRN' AS name,
       (SELECT COUNT(*) FROM (SELECT 1 FROM GlassfishDB.MessageProperty
                               WHERE MessagePropertyName = 'Message.SNDPRN' LIMIT 1) t) AS kommtVor;
SELECT 'Message.VFN' AS name,
       (SELECT COUNT(*) FROM (SELECT 1 FROM GlassfishDB.MessageProperty
                               WHERE MessagePropertyName = 'Message.VFN' LIMIT 1) t) AS kommtVor;
