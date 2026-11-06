-- Messung M1011: die eigenen Zeilen wegraeumen, ueber ihre task_id
-- Auftrag:  "Ersteller am Auftrag in Ihre Auftraege", 06.11.2026
-- Ergebnis: docs/tasks.md §9.10
--
-- Nicht ueber das Praefix von angelegt_von: Ein Teil der Zeilen traegt den Benutzernamen eines echten
-- Kontos. Danach gleicht Anzahl und Pruefsumme dem Stand "00 Vorher" aus m1011-anlegen.sql.
--
-- Benutzer: monitor_write

USE overlord_monitor;

SELECT '=== 00 Vorher ===' AS marke;
SELECT COUNT(*) AS eigene_zeilen FROM task WHERE task_id LIKE '00000000-1011-%';

SELECT '=== 01 Loeschen ===' AS marke;
DELETE FROM task WHERE task_id LIKE '00000000-1011-%';
SELECT ROW_COUNT() AS geloescht;

SELECT '=== 02 Nachher ===' AS marke;
SELECT COUNT(*) AS eigene_zeilen FROM task
 WHERE task_id LIKE '00000000-1011-%' OR mandant_id LIKE 'it-m1011-%';
SELECT COUNT(*) AS task_gesamt,
       BIT_XOR(CRC32(CONCAT_WS('|', task_id, mandant_id, status, angelegt_von))) AS pruefsumme
  FROM task;
