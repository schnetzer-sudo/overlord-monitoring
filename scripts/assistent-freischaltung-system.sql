-- ═══════════════════════════════════════════════════════════════════════════════
-- Assistent 1.1.0: WERK_ABLADESTELLE ausschließlich für SYSTEM freischalten
-- ═══════════════════════════════════════════════════════════════════════════════
--
-- Zielzustand (docs/tasks.md §3.9, E-1125): Der Typ WERK_ABLADESTELLE ist definiert
-- und ausschließlich für den Mandanten SYSTEM freigeschaltet. Den Typ legen die
-- Migrationen V100 bis V102 an, V103 nimmt die Freischaltungen für NEXANS und IBIS
-- aus V100 zurück. Dieses Skript schaltet SYSTEM frei; SYSTEM sieht nur ADMIN.
--
-- IDEMPOTENT: Ein zweiter Lauf ändert nichts.
-- RÜCKNAHME: scripts/assistent-freischaltung-system-ruecknahme.sql
--
-- Was es NICHT tut: einen Kundenmandanten freischalten, den Typ ändern, den
-- Assistenten einschalten. Eingeschaltet wird er über OVERLORD_ASSISTENT_AKTIV und
-- OVERLORD_ASSISTENT_API_KEY (docs/betrieb.md §3); ohne beides wirkt die
-- Freischaltung nicht. Weitere Mandanten schaltet ein INSERT frei
-- (docs/tasks.md §3.9) — nicht dieses Skript: Steht für den Typ schon ein anderer
-- Mandant, bricht es ab (Wache 3).
--
-- ── Wann ─────────────────────────────────────────────────────────────────────
--
-- Nach dem ersten Start von 1.1.0: Erst dann hat Flyway V100 bis V103 angewandt.
-- Vorher bricht Wache 2 ab, und es ist nichts geschehen.
--
-- ── Ausführen ────────────────────────────────────────────────────────────────
--
-- MySQL Workbench, Verbindung mit dem Schreibbenutzer (monitor_write),
-- Vorgabeschema = das eigene Schema (OVERLORD_DB_MONITOR_SCHEMA; auf der Testkopie
-- overlord_monitor). Query > Execute (All or Selection) to Text, Reiter
-- „Query Output“. Die Vorgabe „bei Fehler anhalten“ nicht ändern: Die Wachen
-- brechen mit Fehler 1231 ab, wenn etwas nicht stimmt, und danach läuft nichts mehr.
--
-- Erwartet: drei Ergebnismengen — „vorher“, „nachher“, „fertig“. Nach dem ersten
-- Lauf steht in „nachher“ freischaltungen 1, system 1, andere 0, typ_aktiv 1.

SET SESSION max_statement_time = 10;

-- Wache 1: Das Vorgabeschema ist das eigene Schema.
SET SESSION sql_mode = IF(
  (SELECT COUNT(*) FROM information_schema.TABLES
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'task_type_mandant') = 1,
  @@session.sql_mode,
  'BITTE_DAS_EIGENE_SCHEMA_ALS_VORGABE_WAEHLEN');

-- Wache 2: 1.1.0 ist gestartet — V103 angewandt, der Typ definiert.
SET SESSION sql_mode = IF(
  (SELECT COUNT(*) FROM flyway_schema_history WHERE version = '103' AND success = 1) = 1
    AND (SELECT COUNT(*) FROM task_type WHERE task_typ = 'WERK_ABLADESTELLE') = 1,
  @@session.sql_mode,
  'BITTE_ERST_1_1_0_STARTEN');

-- Wache 3: Für den Typ ist kein anderer Mandant freigeschaltet. Nach V103 ist das so;
-- steht doch einer da, ist der Typ schon weiter ausgerollt, und dieses Skript passt
-- nicht mehr.
SET SESSION sql_mode = IF(
  (SELECT COUNT(*) FROM task_type_mandant
   WHERE task_typ = 'WERK_ABLADESTELLE' AND mandant_id <> 'SYSTEM') = 0,
  @@session.sql_mode,
  'ANDERE_MANDANTEN_FREIGESCHALTET_SKRIPT_PASST_NICHT');

SELECT 'vorher' AS stand,
       COUNT(tm.mandant_id) AS freischaltungen,
       COALESCE(SUM(tm.mandant_id = 'SYSTEM'), 0) AS system,
       COALESCE(SUM(tm.mandant_id <> 'SYSTEM'), 0) AS andere,
       (SELECT aktiv FROM task_type WHERE task_typ = 'WERK_ABLADESTELLE') AS typ_aktiv
FROM task_type_mandant tm
WHERE tm.task_typ = 'WERK_ABLADESTELLE';

-- Die Freischaltung. Steht sie schon, bleibt die Zeile, wie sie ist.
INSERT INTO task_type_mandant (task_typ, mandant_id)
VALUES ('WERK_ABLADESTELLE', 'SYSTEM')
ON DUPLICATE KEY UPDATE mandant_id = mandant_id;

SELECT 'nachher' AS stand,
       COUNT(tm.mandant_id) AS freischaltungen,
       COALESCE(SUM(tm.mandant_id = 'SYSTEM'), 0) AS system,
       COALESCE(SUM(tm.mandant_id <> 'SYSTEM'), 0) AS andere,
       (SELECT aktiv FROM task_type WHERE task_typ = 'WERK_ABLADESTELLE') AS typ_aktiv
FROM task_type_mandant tm
WHERE tm.task_typ = 'WERK_ABLADESTELLE';

SELECT 'fertig' AS stand;
