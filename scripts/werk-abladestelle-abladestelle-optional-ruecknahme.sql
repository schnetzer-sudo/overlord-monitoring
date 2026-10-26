-- ═══════════════════════════════════════════════════════════════════════════════
-- WERK_ABLADESTELLE: Abladestelle optional zurücknehmen
-- ═══════════════════════════════════════════════════════════════════════════════
--
-- Die Rücknahme zu scripts/werk-abladestelle-abladestelle-optional.sql
-- (docs/tasks.md §4.3, E-1164). Danach ist die Abladestelle wieder Pflicht, und die
-- Beschreibung ist wieder die nach dem Skript vom 26.10.2026: „Der Schlüssel besteht aus
-- Werk und Abladestelle, wahlweise mit der Lieferantennummer davor; …“.
--
-- Die Struktur ist danach Zeichen für Zeichen die von vorher: "pflicht": true steht wieder
-- an seiner Stelle zwischen "label" und "muster". Dafür setzt die Rücknahme das ganze Feld
-- abladestelle neu, aus seinen eigenen Werten, und Wache 3 verlangt, dass es genau name,
-- label, muster und beispiel trägt; ein Feld mit mehr ginge sonst verloren. Das Muster
-- übernimmt sie, wie es dasteht, auch wenn das Skript vom 23.10.2026 zurückgenommen ist.
--
-- Wirkt beim nächsten Zugriff, ohne Neustart. Ein Eintrag ohne Abladestelle fällt danach
-- wieder mit PFLICHT_FEHLT.
--
-- IDEMPOTENT: Ein zweiter Lauf ändert nichts, auch nicht geaendert_am.
--
-- Was es NICHT tut: angelegte Aufträge ändern. Ein Auftrag, der ohne Abladestelle angelegt
-- wurde, behält seinen Schlüssel und seine Kopie der Definition (E-1023); ist er noch
-- offen, holt die Plattform ihn ab wie jeden anderen.
--
-- ── Wann ─────────────────────────────────────────────────────────────────────
--
-- Vor einem Rollback auf eine Version, die optionale Schlüsselteile nur vorn zulässt
-- (docs/betrieb.md §7a D): Sonst liest sie die Definition als ungültig, und der Assistent
-- verschwindet.
--
-- ── Ausführen ────────────────────────────────────────────────────────────────
--
-- Wie das Skript selbst: MySQL Workbench, Schreibbenutzer (monitor_write),
-- Vorgabeschema = das eigene Schema, Query > Execute (All or Selection) to Text.
-- Die Vorgabe „bei Fehler anhalten“ nicht ändern.
--
-- Erwartet: drei Ergebnismengen — „vorher“, „nachher“, „fertig“. In „nachher“ steht
-- abladestelle_pflicht true, alt 2 und zeichen 2272.

SET SESSION max_statement_time = 10;
SET NAMES utf8mb4;

-- Die Beschreibung nach dem Skript vom 26.10.2026, wie sie bis zum Skript galt.
SET @alt = 'Die EDI-Plattform erzeugt aus eingehenden Partnernachrichten IDOC-Nachrichten für das Kundensystem und braucht dafür eine SNDPRN. Ist sie nicht fest hinterlegt, ermittelt die Plattform sie über die Werk-/Abladestellentabelle des Kunden. Mit diesem Auftrag legt der Kunde Einträge dieser Tabelle an, passt sie an oder löscht sie.

Ein Eintrag besteht aus einem Schlüssel und zwei Werten. Der Schlüssel besteht aus Werk und Abladestelle, wahlweise mit der Lieferantennummer davor; die Lieferantennummer gehört nur dazu, wenn der Kunde sie für diesen Eintrag nennt. Die Werte sind SNDPRN und SNDPRN_4908 (für VDA-4908-Prozesse), jeweils sechsstellig numerisch.

Kunden schreiben einen Eintrag oft als Zeile: Schlüssel, Komma, SNDPRN, Komma, SNDPRN_4908. Im Schlüssel trennt „/“ die Teile, das erste Komma beendet den Schlüssel. Beispiel: „0101159903/40NP/123,120764,“ heißt Lieferantennummer 0101159903, Werk 40NP, Abladestelle 123, SNDPRN 120764 und SNDPRN_4908 leer. Ein Feld, das nach einem Komma leer bleibt, ist ausdrücklich leer. Einen Schlüsselteil zerlegst du nie weiter, auch wenn er wie 40NP Ziffern und Buchstaben mischt.

Neuanlage: Schlüssel und mindestens einer der beiden Werte. Ist der Schlüssel schon vorhanden, bleibt der Eintrag unverändert, und die Rückmeldung lautet „Schlüssel bereits vorhanden“.
Anpassung: der Schlüssel eines vorhandenen Eintrags und die neuen Werte. Ein leer gelassener Wert wird entfernt, mindestens einer muss gesetzt bleiben. Nenne dem Kunden vor dem Anlegen jeden Wert, der dadurch entfernt wird, denn er sieht die bisherigen Werte nicht. Nennt der Kunde bei einer Anpassung einen der beiden Werte gar nicht, frage nach, ob er entfernt werden soll oder welchen Wert er behalten soll. Ein ausdrücklich leeres Feld ist genannt: Dann fragst du nicht nach, sondern sagst ihm vor dem Anlegen, dass dieser Wert entfernt wird.
Löschung: nur der Schlüssel.

Ist unklar, ob ein Eintrag neu angelegt oder geändert werden soll, schlage die Neuanlage vor. Gibt es den Schlüssel schon, bleibt der Eintrag unverändert. Eine Anpassung schlägst du nur vor, wenn der Kunde ausdrücklich etwas ändern will.

Ein Auftrag enthält eine Aktion für bis zu 20 Einträge. Ob ein Schlüssel existiert, weiß der Assistent nicht; das zeigt erst die Ausführung.';

-- Die Beschreibung mit optionaler Abladestelle.
SET @neu = 'Die EDI-Plattform erzeugt aus eingehenden Partnernachrichten IDOC-Nachrichten für das Kundensystem und braucht dafür eine SNDPRN. Ist sie nicht fest hinterlegt, ermittelt die Plattform sie über die Werk-/Abladestellentabelle des Kunden. Mit diesem Auftrag legt der Kunde Einträge dieser Tabelle an, passt sie an oder löscht sie.

Ein Eintrag besteht aus einem Schlüssel und zwei Werten. Der Schlüssel besteht aus dem Werk, wahlweise mit der Lieferantennummer davor und der Abladestelle dahinter; Lieferantennummer und Abladestelle gehören nur dazu, wenn der Kunde sie für diesen Eintrag nennt. Die Werte sind SNDPRN und SNDPRN_4908 (für VDA-4908-Prozesse), jeweils sechsstellig numerisch.

Kunden schreiben einen Eintrag oft als Zeile: Schlüssel, Komma, SNDPRN, Komma, SNDPRN_4908. Im Schlüssel trennt „/“ die Teile, das erste Komma beendet den Schlüssel. Beispiel: „0101159903/40NP/123,120764,“ heißt Lieferantennummer 0101159903, Werk 40NP, Abladestelle 123, SNDPRN 120764 und SNDPRN_4908 leer. Ein Feld, das nach einem Komma leer bleibt, ist ausdrücklich leer. Einen Schlüsselteil zerlegst du nie weiter, auch wenn er wie 40NP Ziffern und Buchstaben mischt.

Neuanlage: Schlüssel und mindestens einer der beiden Werte. Ist der Schlüssel schon vorhanden, bleibt der Eintrag unverändert, und die Rückmeldung lautet „Schlüssel bereits vorhanden“.
Anpassung: der Schlüssel eines vorhandenen Eintrags und die neuen Werte. Ein leer gelassener Wert wird entfernt, mindestens einer muss gesetzt bleiben. Nenne dem Kunden vor dem Anlegen jeden Wert, der dadurch entfernt wird, denn er sieht die bisherigen Werte nicht. Nennt der Kunde bei einer Anpassung einen der beiden Werte gar nicht, frage nach, ob er entfernt werden soll oder welchen Wert er behalten soll. Ein ausdrücklich leeres Feld ist genannt: Dann fragst du nicht nach, sondern sagst ihm vor dem Anlegen, dass dieser Wert entfernt wird.
Löschung: nur der Schlüssel.

Ist unklar, ob ein Eintrag neu angelegt oder geändert werden soll, schlage die Neuanlage vor. Gibt es den Schlüssel schon, bleibt der Eintrag unverändert. Eine Anpassung schlägst du nur vor, wenn der Kunde ausdrücklich etwas ändern will.

Ein Auftrag enthält eine Aktion für bis zu 20 Einträge. Ob ein Schlüssel existiert, weiß der Assistent nicht; das zeigt erst die Ausführung.';

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

-- Wache 3: An der Abladestelle steht schon wieder "pflicht": true, oder sie trägt genau
-- name, label, muster und beispiel; und die Beschreibung ist die neue oder schon wieder
-- die alte.
SET SESSION sql_mode = IF(
  (SELECT COUNT(*) FROM task_type
   WHERE task_typ = 'WERK_ABLADESTELLE'
     AND (JSON_CONTAINS(struktur, 'true', '$.felder[2].pflicht') = 1
          OR (JSON_LENGTH(struktur, '$.felder[2]') = 4
              AND JSON_CONTAINS_PATH(struktur, 'all', '$.felder[2].name', '$.felder[2].label',
                                     '$.felder[2].muster', '$.felder[2].beispiel') = 1))
     AND CAST(beschreibung AS BINARY) IN (CAST(@alt AS BINARY), CAST(@neu AS BINARY))) = 1,
  @@session.sql_mode,
  'DEFINITION_UNERWARTET_SKRIPT_PASST_NICHT');

SELECT 'vorher' AS stand,
       JSON_EXTRACT(struktur, '$.felder[2].pflicht') AS abladestelle_pflicht,
       JSON_CONTAINS(struktur, 'true', '$.felder[2].pflicht')
       + (CAST(beschreibung AS BINARY) = CAST(@alt AS BINARY)) AS alt,
       CHAR_LENGTH(beschreibung) AS zeichen,
       geaendert_am
FROM task_type
WHERE task_typ = 'WERK_ABLADESTELLE';

-- Die Änderung, in einer Anweisung. JSON_OBJECT legt die Schlüssel in dieser Reihenfolge
-- ab, so wie sie vor dem Skript standen.
UPDATE task_type
SET struktur = IF(JSON_CONTAINS(struktur, 'true', '$.felder[2].pflicht') = 1,
                  struktur,
                  JSON_SET(struktur, '$.felder[2]',
                           JSON_OBJECT('name', JSON_VALUE(struktur, '$.felder[2].name'),
                                       'label', JSON_VALUE(struktur, '$.felder[2].label'),
                                       'pflicht', TRUE,
                                       'muster', JSON_VALUE(struktur, '$.felder[2].muster'),
                                       'beispiel', JSON_VALUE(struktur, '$.felder[2].beispiel')))),
    beschreibung = @alt
WHERE task_typ = 'WERK_ABLADESTELLE'
  AND (JSON_CONTAINS(struktur, 'true', '$.felder[2].pflicht') = 0
       OR CAST(beschreibung AS BINARY) <> CAST(@alt AS BINARY));

SELECT 'nachher' AS stand,
       JSON_EXTRACT(struktur, '$.felder[2].pflicht') AS abladestelle_pflicht,
       JSON_CONTAINS(struktur, 'true', '$.felder[2].pflicht')
       + (CAST(beschreibung AS BINARY) = CAST(@alt AS BINARY)) AS alt,
       CHAR_LENGTH(beschreibung) AS zeichen,
       geaendert_am
FROM task_type
WHERE task_typ = 'WERK_ABLADESTELLE';

SELECT 'fertig' AS stand;
