-- ═══════════════════════════════════════════════════════════════════════════════
-- WERK_ABLADESTELLE: Leerzeichen im Schlüssel zurücknehmen
-- ═══════════════════════════════════════════════════════════════════════════════
--
-- Die Rücknahme zu scripts/werk-abladestelle-schluessel-leerzeichen.sql
-- (docs/tasks.md §4.3, E-1136). Danach tragen die drei Schlüsselteile wieder das
-- Muster aus V100:
--
--   ^[^ /,]{1,35}$
--
-- Ein Leerzeichen im Innern eines Schlüsselteils fällt dann wieder über das Muster
-- (FORMAT). Leerzeichen am Rand kürzt das Backend weiter; das ist Code, kein Wert
-- der Definition. Wirkt beim nächsten Zugriff, ohne Neustart.
--
-- IDEMPOTENT: Ein zweiter Lauf ändert nichts, auch nicht geaendert_am.
--
-- Was es NICHT tut: angelegte Aufträge ändern. Ein Auftrag, der mit Leerzeichen im
-- Schlüssel angelegt wurde, behält sie und seine Kopie der Definition (E-1023); ist
-- er noch offen, holt die Plattform ihn ab wie jeden anderen.
--
-- ── Ausführen ────────────────────────────────────────────────────────────────
--
-- Wie das Skript selbst: MySQL Workbench, Schreibbenutzer (monitor_write),
-- Vorgabeschema = das eigene Schema, Query > Execute (All or Selection) to Text.
-- Die Vorgabe „bei Fehler anhalten“ nicht ändern.
--
-- Erwartet: drei Ergebnismengen — „vorher“, „nachher“, „fertig“. In „nachher“
-- steht alt 3, und die drei Muster lauten wie oben.

SET SESSION max_statement_time = 10;

SET @alt = '^[^ /,]{1,35}$';
SET @neu = CONCAT('^(?:[^/,', CHAR(92 USING utf8mb4), 'p{Z}]| ){1,35}$');

-- Wache 1: Das Vorgabeschema ist das eigene Schema.
SET SESSION sql_mode = IF(
  (SELECT COUNT(*) FROM information_schema.TABLES
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'task_type') = 1,
  @@session.sql_mode,
  'BITTE_DAS_EIGENE_SCHEMA_ALS_VORGABE_WAEHLEN');

-- Wache 2: Der Typ ist da, und die ersten drei Felder sind die drei Schlüsselteile.
SET SESSION sql_mode = IF(
  (SELECT COUNT(*) FROM task_type
   WHERE task_typ = 'WERK_ABLADESTELLE'
     AND JSON_UNQUOTE(JSON_EXTRACT(struktur, '$.felder[0].name')) = 'lieferantennummer'
     AND JSON_UNQUOTE(JSON_EXTRACT(struktur, '$.felder[1].name')) = 'werk'
     AND JSON_UNQUOTE(JSON_EXTRACT(struktur, '$.felder[2].name')) = 'abladestelle') = 1,
  @@session.sql_mode,
  'TYP_FEHLT_ODER_FELDER_ANDERS_SKRIPT_PASST_NICHT');

-- Wache 3: Jedes der drei Muster ist das neue oder schon wieder das alte.
SET SESSION sql_mode = IF(
  (SELECT COUNT(*) FROM task_type
   WHERE task_typ = 'WERK_ABLADESTELLE'
     AND CAST(JSON_UNQUOTE(JSON_EXTRACT(struktur, '$.felder[0].muster')) AS BINARY)
         IN (CAST(@alt AS BINARY), CAST(@neu AS BINARY))
     AND CAST(JSON_UNQUOTE(JSON_EXTRACT(struktur, '$.felder[1].muster')) AS BINARY)
         IN (CAST(@alt AS BINARY), CAST(@neu AS BINARY))
     AND CAST(JSON_UNQUOTE(JSON_EXTRACT(struktur, '$.felder[2].muster')) AS BINARY)
         IN (CAST(@alt AS BINARY), CAST(@neu AS BINARY))) = 1,
  @@session.sql_mode,
  'MUSTER_UNERWARTET_SKRIPT_PASST_NICHT');

SELECT 'vorher' AS stand,
       JSON_UNQUOTE(JSON_EXTRACT(struktur, '$.felder[0].muster')) AS lieferantennummer,
       JSON_UNQUOTE(JSON_EXTRACT(struktur, '$.felder[1].muster')) AS werk,
       JSON_UNQUOTE(JSON_EXTRACT(struktur, '$.felder[2].muster')) AS abladestelle,
       (CAST(JSON_UNQUOTE(JSON_EXTRACT(struktur, '$.felder[0].muster')) AS BINARY) = CAST(@alt AS BINARY))
       + (CAST(JSON_UNQUOTE(JSON_EXTRACT(struktur, '$.felder[1].muster')) AS BINARY) = CAST(@alt AS BINARY))
       + (CAST(JSON_UNQUOTE(JSON_EXTRACT(struktur, '$.felder[2].muster')) AS BINARY) = CAST(@alt AS BINARY))
         AS alt,
       geaendert_am
FROM task_type
WHERE task_typ = 'WERK_ABLADESTELLE';

UPDATE task_type
SET struktur = JSON_SET(struktur,
                        '$.felder[0].muster', @alt,
                        '$.felder[1].muster', @alt,
                        '$.felder[2].muster', @alt)
WHERE task_typ = 'WERK_ABLADESTELLE'
  AND NOT (CAST(JSON_UNQUOTE(JSON_EXTRACT(struktur, '$.felder[0].muster')) AS BINARY) = CAST(@alt AS BINARY)
       AND CAST(JSON_UNQUOTE(JSON_EXTRACT(struktur, '$.felder[1].muster')) AS BINARY) = CAST(@alt AS BINARY)
       AND CAST(JSON_UNQUOTE(JSON_EXTRACT(struktur, '$.felder[2].muster')) AS BINARY) = CAST(@alt AS BINARY));

SELECT 'nachher' AS stand,
       JSON_UNQUOTE(JSON_EXTRACT(struktur, '$.felder[0].muster')) AS lieferantennummer,
       JSON_UNQUOTE(JSON_EXTRACT(struktur, '$.felder[1].muster')) AS werk,
       JSON_UNQUOTE(JSON_EXTRACT(struktur, '$.felder[2].muster')) AS abladestelle,
       (CAST(JSON_UNQUOTE(JSON_EXTRACT(struktur, '$.felder[0].muster')) AS BINARY) = CAST(@alt AS BINARY))
       + (CAST(JSON_UNQUOTE(JSON_EXTRACT(struktur, '$.felder[1].muster')) AS BINARY) = CAST(@alt AS BINARY))
       + (CAST(JSON_UNQUOTE(JSON_EXTRACT(struktur, '$.felder[2].muster')) AS BINARY) = CAST(@alt AS BINARY))
         AS alt,
       geaendert_am
FROM task_type
WHERE task_typ = 'WERK_ABLADESTELLE';

SELECT 'fertig' AS stand;
