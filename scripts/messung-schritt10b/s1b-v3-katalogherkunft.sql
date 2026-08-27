-- Messrunde M94-M98 — Sitzung 1b: Nachtrag zu V3, Herkunft der Katalogzeilen
-- Grund: V3 hat 1.457 gepflegte Zeilen gegen 770 in M91 gefunden. Ob das
--        Kuratierung oder ein Bestandslauf mit uebernommenen Vorschlaegen ist,
--        entscheidet, wie M95 und M98 zu lesen sind.
--
-- Ausschliesslich SELECT / SET. (Regel S1)
-- G1: keine Partnernamen, keine ProcessID, keine Benutzernamen im Klartext.

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;

SET max_statement_time = 60;
SET profiling = 1;
SET profiling_history_size = 100;

SELECT '=== V3d Spalten von process_catalog ===' AS marke;
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT, EXTRA
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'overlord_monitor' AND TABLE_NAME = 'process_catalog'
ORDER BY ORDINAL_POSITION;

SELECT '=== V3e pflegestatus x vorschlag_herkunft ===' AS marke;
SELECT pflegestatus,
       COALESCE(vorschlag_herkunft, '(NULL)') AS vorschlag_herkunft,
       COUNT(*)                               AS zeilen,
       SUM(partner IS NOT NULL AND partner <> '')   AS mit_partner,
       SUM(richtung IS NOT NULL AND richtung <> '') AS mit_richtung
FROM overlord_monitor.process_catalog
GROUP BY pflegestatus, COALESCE(vorschlag_herkunft, '(NULL)')
ORDER BY zeilen DESC;

SELECT '=== V3f Wer hat zuletzt geaendert, und wann (ohne Namen) ===' AS marke;
SELECT COUNT(DISTINCT geaendert_von) AS versch_bearbeiter,
       MIN(geaendert_am)             AS aelteste_aenderung,
       MAX(geaendert_am)             AS juengste_aenderung,
       SUM(geaendert_von LIKE 'it-%') AS von_testkennung,
       SUM(geaendert_von LIKE 'system%' OR geaendert_von LIKE 'bestandslauf%') AS von_bestandslauf
FROM overlord_monitor.process_catalog;

SELECT '=== V3g Aenderungen je Tag ===' AS marke;
SELECT DATE(geaendert_am) AS tag, COUNT(*) AS zeilen
FROM overlord_monitor.process_catalog
GROUP BY DATE(geaendert_am)
ORDER BY tag;

SELECT '=== 99 Laufzeiten ===' AS marke;
SHOW PROFILES;

SELECT '=== 99 read_only am Ende der Sitzung ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;
