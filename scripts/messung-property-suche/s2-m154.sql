-- M154 — Verteilung ueber Typ und Mandant, einschliesslich NULL, ueber alle zehn Mandanten
SELECT @@global.read_only AS globalReadOnly;
SET max_statement_time = 60;

SELECT '--- M154-1 Zeilen je MessagePropertyType, NULL als eigene Zeile ---' AS marke;
SELECT MessagePropertyType, COUNT(*) AS zeilen
  FROM GlassfishDB.MessagePropertySearchListEntry
 GROUP BY MessagePropertyType
 ORDER BY MessagePropertyType;

SELECT '--- M154-2 Zeilen je MandantID, NULL als eigene Zeile ---' AS marke;
SELECT MandantID, COUNT(*) AS zeilen
  FROM GlassfishDB.MessagePropertySearchListEntry
 GROUP BY MandantID
 ORDER BY MandantID;

SELECT '--- M154-3 Kreuztabelle Typ x Mandant ---' AS marke;
SELECT MessagePropertyType, MandantID, COUNT(*) AS zeilen
  FROM GlassfishDB.MessagePropertySearchListEntry
 GROUP BY MessagePropertyType, MandantID
 ORDER BY MessagePropertyType, MandantID;

SELECT '--- M154-4 vollstaendige Auflistung (12 Zeilen, Feldnamen sind Konfiguration, G1) ---' AS marke;
SELECT MessagePropertyName, MandantID, MessagePropertyType
  FROM GlassfishDB.MessagePropertySearchListEntry
 ORDER BY MessagePropertyType, MessagePropertyName;

SELECT '--- M154-5 Gegenprobe: kommt ein Name mehrfach vor? (PK sagt nein) ---' AS marke;
SELECT COUNT(*) AS zeilen, COUNT(DISTINCT MessagePropertyName) AS verschiedeneNamen
  FROM GlassfishDB.MessagePropertySearchListEntry;

SELECT '--- M154-6 Gegenprobe: NULL wirklich vorhanden, nicht leerer String ---' AS marke;
SELECT SUM(MandantID IS NULL) AS mandantNull,
       SUM(MandantID = '') AS mandantLeer,
       SUM(MessagePropertyType IS NULL) AS typNull
  FROM GlassfishDB.MessagePropertySearchListEntry;
