-- Messrunde M86-M92 — Sitzung 8: Probetabelle loeschen, Loeschung nachweisen, Abschluss
--
-- Der Auftrag (§M89) sagt woertlich: "gemessen, am Ende der Runde geloescht. Die Loeschung
-- wird in der Ergebnisdatei nachgewiesen." Und unter GESPERRT: "jedes Belassen der
-- Probetabelle ueber das Rundenende hinaus."
--
-- Benutzer: monitor_write. Der EINZIGE schreibende Zugriff dieser Sitzung ist das DROP
-- auf overlord_monitor.message_rollup_probe. Auf GlassfishDB wird nur gelesen.
--
-- Der Nachweis ist dreiteilig und steht bewusst VOR und NACH dem DROP:
--   1. vorher:  die Tabelle ist da, mit ihrer Zeilenzahl
--   2. DROP
--   3. nachher: information_schema kennt sie nicht mehr, und overlord_monitor traegt
--               wieder genau die neun Tabellen aus Sitzung 5a-2
--   4. GlassfishDB ist unveraendert, und @@global.read_only steht weiterhin auf 1

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, VERSION() AS version, NOW() AS serverzeit;
SELECT USER() AS benutzer, CURRENT_USER() AS aufgeloest;

SELECT '=== 8-1 VORHER: die Probetabelle steht noch ===' AS marke;
SELECT COUNT(*) AS zeilen_probetabelle FROM overlord_monitor.message_rollup_probe;
SELECT TABLE_NAME, TABLE_ROWS, DATA_LENGTH, INDEX_LENGTH
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'overlord_monitor' AND TABLE_NAME = 'message_rollup_probe';

SELECT '=== 8-2 DROP ===' AS marke;
DROP TABLE overlord_monitor.message_rollup_probe;

SELECT '=== 8-3 NACHHER: information_schema kennt sie nicht mehr (muss 0 sein) ===' AS marke;
SELECT COUNT(*) AS tabelle_noch_vorhanden
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'overlord_monitor' AND TABLE_NAME = 'message_rollup_probe';

SELECT '=== 8-4 Bestand von overlord_monitor NACH dem Loeschen ===' AS marke;
SELECT TABLE_NAME FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'overlord_monitor' ORDER BY TABLE_NAME;

SELECT '=== 8-5 process_catalog ist unveraendert (V2 gegengeprueft) ===' AS marke;
SELECT COUNT(*)                        AS zeilen,
       SUM(pflegestatus = 'GEPFLEGT')  AS gepflegt,
       SUM(pflegestatus = 'OFFEN')     AS offen,
       COUNT(partner)                  AS partner_nicht_null,
       COUNT(richtung)                 AS richtung_nicht_null
FROM overlord_monitor.process_catalog;

SELECT '=== 8-6 GlassfishDB ist unberuehrt ===' AS marke;
SELECT COUNT(*) AS zeilen_message, MAX(MessageLastUpdate) AS datenstand
FROM GlassfishDB.Message;

SELECT '=== 8-7 Tabellengroessen gegen V1 (byteidentisch?) ===' AS marke;
SELECT TABLE_NAME, DATA_LENGTH, INDEX_LENGTH
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'GlassfishDB'
  AND TABLE_NAME IN ('Message','MessageAction','MessageBAM','MessageProperty')
ORDER BY TABLE_NAME;

SELECT '=== 99 Abschluss der Runde ===' AS marke;
SELECT @@global.read_only AS read_only_ende, NOW() AS serverzeit_ende;
