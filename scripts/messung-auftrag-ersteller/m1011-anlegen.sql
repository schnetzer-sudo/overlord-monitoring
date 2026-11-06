-- Messung M1011: eigene Zeilen anlegen
-- Auftrag:  "Ersteller am Auftrag in Ihre Auftraege", 06.11.2026
-- Ergebnis: docs/tasks.md §9.10
--
-- ⚠ Diese Sitzung schreibt in 'overlord_monitor.task' - und NUR dort, NUR auf der Testkopie. Der
--   Auftrag verlangt es: Auf der Testkopie ist 'task' fast leer, und der Plan des Joins zeigt sich
--   erst mit Zeilen. Die Loeschung ist in m1011-aufraeumen.sql nachgewiesen.
--
-- Die Zeilen liegen unter zwei KUENSTLICHEN Mandanten (mandant_id ohne Fremdschluessel): 'it-m1011-gross'
-- mit 400 Auftraegen und 'it-m1011-klein' mit 3 (Regel L7: neben dem grossen immer ein kleiner). So
-- sieht sie kein echter Mandant, auch nicht waehrend der Messung. Alle tragen ERLEDIGT: Die Plattform
-- liest nur OFFEN.
--
-- Je vierte Zeile traegt den Benutzernamen eines echten lokalen ADMIN-Kontos, je vierte den eines
-- MANDANT-Kontos, der Rest 'it-m1011-<n>' ohne Konto. Die beiden Namen leitet die Sitzung selbst her
-- und gibt sie nicht aus (Regel G1). Deshalb raeumt m1011-aufraeumen.sql ueber die task_id auf und
-- nicht ueber das Praefix.
--
-- Benutzer: monitor_write

USE overlord_monitor;

SELECT '=== 00 Vorher ===' AS marke;
SELECT @@global.read_only AS read_only;
SELECT COUNT(*) AS task_gesamt,
       BIT_XOR(CRC32(CONCAT_WS('|', task_id, mandant_id, status, angelegt_von))) AS pruefsumme
  FROM task;
SELECT COUNT(*) AS kollisionen FROM task
 WHERE task_id LIKE '00000000-1011-%' OR mandant_id LIKE 'it-m1011-%';

SET @admin = (SELECT username FROM app_user WHERE role = 'ADMIN' ORDER BY id LIMIT 1);
SET @mandant = (SELECT username FROM app_user WHERE role = 'MANDANT' ORDER BY id LIMIT 1);
SET @definition = (SELECT CONCAT('{"name":', JSON_QUOTE(name), ',"struktur":', struktur, '}')
                     FROM task_type WHERE task_typ = 'WERK_ABLADESTELLE');
SELECT @admin IS NOT NULL AS admin_da, @mandant IS NOT NULL AS mandant_da,
       JSON_VALID(@definition) AS definition_gueltig, LENGTH(@definition) AS definition_bytes;

SELECT '=== 01 Anlegen ===' AS marke;
INSERT INTO task (task_id, mandant_id, task_typ, definition, aktion, eintraege, anfrage_id, parameter,
                  status, rueckgabe, eingestellt_am, abgeholt_am, ausgefuehrt_am, angelegt_von)
WITH RECURSIVE zahl (n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM zahl WHERE n < 403)
SELECT CONCAT('00000000-1011-4000-8000-', LPAD(n, 12, '0')),
       IF(n <= 400, 'it-m1011-gross', 'it-m1011-klein'),
       'WERK_ABLADESTELLE',
       @definition,
       'Neuanlage',
       '[{"werk":"72332","abladestelle":"DDB0","sndprn":"120764"}]',
       CONCAT('00000000-1011-4000-9000-', LPAD(n, 12, '0')),
       CONCAT('Aktion=Neuanlage', CHAR(10), '72332/DDB0,120764,'),
       'ERLEDIGT',
       'Ok',
       UTC_TIMESTAMP(3) - INTERVAL n MINUTE,
       UTC_TIMESTAMP(3) - INTERVAL n MINUTE,
       UTC_TIMESTAMP(3) - INTERVAL n MINUTE,
       CASE n % 4
         WHEN 0 THEN @admin
         WHEN 1 THEN @mandant
         ELSE CONCAT('it-m1011-', n)
       END
  FROM zahl;

SELECT '=== 02 Nachher ===' AS marke;
SELECT mandant_id, COUNT(*) AS zeilen,
       SUM(angelegt_von = @admin) AS konto_admin,
       SUM(angelegt_von = @mandant) AS konto_mandant,
       SUM(angelegt_von LIKE 'it-m1011-%') AS ohne_konto
  FROM task WHERE mandant_id LIKE 'it-m1011-%' GROUP BY mandant_id;
