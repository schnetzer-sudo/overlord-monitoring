-- ═══════════════════════════════════════════════════════════════════════════════
-- WERK_ABLADESTELLE: Leerzeichen im Schlüssel zulassen
-- ═══════════════════════════════════════════════════════════════════════════════
--
-- Zielzustand (docs/tasks.md §4.3, E-1136): Die drei Schlüsselteile
-- (Lieferantennummer, Werk, Abladestelle) dürfen Leerzeichen (U+0020) enthalten,
-- auch mehrere hintereinander. Jedes andere Leerzeichen der Unicode-Kategorie Z
-- (U+00A0, U+202F, U+3000 …) bleibt unzulässig, ebenso / und ,. Tabulator und
-- Zeilenumbruch lehnt der Zeichenschutz des Backends ab (docs/tasks.md §3.4).
-- Das neue Muster je Teil:
--
--   ^(?:[^/,\p{Z}]| ){1,35}$
--
-- Leerzeichen am Rand entfernt das Backend vor der Prüfung; das Muster sieht den
-- gekürzten Wert. Den Backslash baut CHAR(92) — so hängt das Skript nicht daran,
-- ob der Client NO_BACKSLASH_ESCAPES gesetzt hat, und JSON_SET maskiert ihn im JSON.
--
-- IDEMPOTENT: Ein zweiter Lauf ändert nichts, auch nicht geaendert_am.
-- RÜCKNAHME: scripts/werk-abladestelle-schluessel-leerzeichen-ruecknahme.sql
--
-- Was es NICHT tut: die SNDPRN-Felder, die Beschreibung, die Freischaltung oder
-- angelegte Aufträge ändern. Jeder Auftrag trägt die Kopie der Definition, mit der
-- er angelegt wurde (E-1023).
--
-- ── Wann ─────────────────────────────────────────────────────────────────────
--
-- Erst NACH dem Start einer Version, deren Backend Schlüsselteile am Rand kürzt
-- (CHANGELOG.md: „Leerzeichen im Schlüssel“, E-1136). 1.1.0 kürzt nicht: Mit dem
-- neuen Muster nähme sie Leerzeichen am Rand mit in Auftrag und Parametertext. Das
-- Skript kann das nicht selbst prüfen, die Version hinterlässt keine Spur in der
-- Datenbank.
--
-- ── Ausführen ────────────────────────────────────────────────────────────────
--
-- MySQL Workbench, Verbindung mit dem Schreibbenutzer (monitor_write),
-- Vorgabeschema = das eigene Schema (OVERLORD_DB_MONITOR_SCHEMA; auf der Testkopie
-- overlord_monitor). Query > Execute (All or Selection) to Text, Reiter
-- „Query Output“. Die Vorgabe „bei Fehler anhalten“ nicht ändern: Die Wachen
-- brechen mit Fehler 1231 ab, wenn etwas nicht stimmt, und danach läuft nichts mehr.
-- Auf dem Server nach der Deployment-Routine (docs/betrieb.md §7a C):
-- mysql … overlord_monitor < scripts/werk-abladestelle-schluessel-leerzeichen.sql
--
-- Erwartet: drei Ergebnismengen — „vorher“, „nachher“, „fertig“. In „nachher“
-- steht neu 3, und die drei Muster lauten wie oben.

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

-- Wache 3: Jedes der drei Muster ist das alte oder schon das neue. Steht dort etwas
-- Drittes, hat jemand die Definition anders geändert, und dieses Skript passt nicht.
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
       (CAST(JSON_UNQUOTE(JSON_EXTRACT(struktur, '$.felder[0].muster')) AS BINARY) = CAST(@neu AS BINARY))
       + (CAST(JSON_UNQUOTE(JSON_EXTRACT(struktur, '$.felder[1].muster')) AS BINARY) = CAST(@neu AS BINARY))
       + (CAST(JSON_UNQUOTE(JSON_EXTRACT(struktur, '$.felder[2].muster')) AS BINARY) = CAST(@neu AS BINARY))
         AS neu,
       geaendert_am
FROM task_type
WHERE task_typ = 'WERK_ABLADESTELLE';

-- Die Änderung. Stehen alle drei schon auf dem neuen Muster, trifft die Bedingung
-- keine Zeile, und geaendert_am bleibt.
UPDATE task_type
SET struktur = JSON_SET(struktur,
                        '$.felder[0].muster', @neu,
                        '$.felder[1].muster', @neu,
                        '$.felder[2].muster', @neu)
WHERE task_typ = 'WERK_ABLADESTELLE'
  AND NOT (CAST(JSON_UNQUOTE(JSON_EXTRACT(struktur, '$.felder[0].muster')) AS BINARY) = CAST(@neu AS BINARY)
       AND CAST(JSON_UNQUOTE(JSON_EXTRACT(struktur, '$.felder[1].muster')) AS BINARY) = CAST(@neu AS BINARY)
       AND CAST(JSON_UNQUOTE(JSON_EXTRACT(struktur, '$.felder[2].muster')) AS BINARY) = CAST(@neu AS BINARY));

SELECT 'nachher' AS stand,
       JSON_UNQUOTE(JSON_EXTRACT(struktur, '$.felder[0].muster')) AS lieferantennummer,
       JSON_UNQUOTE(JSON_EXTRACT(struktur, '$.felder[1].muster')) AS werk,
       JSON_UNQUOTE(JSON_EXTRACT(struktur, '$.felder[2].muster')) AS abladestelle,
       (CAST(JSON_UNQUOTE(JSON_EXTRACT(struktur, '$.felder[0].muster')) AS BINARY) = CAST(@neu AS BINARY))
       + (CAST(JSON_UNQUOTE(JSON_EXTRACT(struktur, '$.felder[1].muster')) AS BINARY) = CAST(@neu AS BINARY))
       + (CAST(JSON_UNQUOTE(JSON_EXTRACT(struktur, '$.felder[2].muster')) AS BINARY) = CAST(@neu AS BINARY))
         AS neu,
       geaendert_am
FROM task_type
WHERE task_typ = 'WERK_ABLADESTELLE';

SELECT 'fertig' AS stand;
