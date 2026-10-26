-- ═══════════════════════════════════════════════════════════════════════════════
-- WERK_ABLADESTELLE: Abladestelle optional
-- ═══════════════════════════════════════════════════════════════════════════════
--
-- Zielzustand (docs/tasks.md §4.3, E-1164 bis E-1176): Der Schlüssel besteht aus dem
-- Werk, wahlweise mit der Lieferantennummer davor und der Abladestelle dahinter. Ohne
-- Abladestelle steht der Schrägstrich am Ende, wie in der Tabelle der Plattform: Die
-- vier Formen sind Lieferantennummer/Werk/Abladestelle, Werk/Abladestelle,
-- Lieferantennummer/Werk/ und Werk/ (E-1172). Gegenüber dem Stand nach den Skripten vom
-- 23. und 26.10.2026 ändern sich drei Stellen:
--
--   1. Struktur: Das Feld abladestelle ($.felder[2]) verliert "pflicht": true. Sonst
--      bleibt die Struktur Zeichen für Zeichen, wie sie ist; das Werk bleibt Pflicht.
--   2. Beschreibung: Der Satz
--        „Der Schlüssel besteht aus Werk und Abladestelle, wahlweise mit der
--        Lieferantennummer davor; die Lieferantennummer gehört nur dazu, wenn der Kunde
--        sie für diesen Eintrag nennt.“
--      wird zu
--        „Der Schlüssel besteht aus dem Werk, wahlweise mit der Lieferantennummer davor
--        und der Abladestelle dahinter; Lieferantennummer und Abladestelle gehören nur
--        dazu, wenn der Kunde sie für diesen Eintrag nennt.“
--   3. Beschreibung, Absatz zum Zeilenformat: Nach dem Beispiel
--      „0101159903/40NP/123,120764,“ steht ein zweites (E-1176):
--        „0101159903/40NP/,120764,“ heißt Lieferantennummer 0101159903, Werk 40NP,
--        keine Abladestelle, SNDPRN 120764 und SNDPRN_4908 leer.
--
-- Die Beschreibung setzt das Skript ganz, aber nur, wenn sie zeichengleich dem Stand nach
-- dem Skript vom 26.10.2026 entspricht oder schon dem neuen (wie E-1162). Die beiden
-- Fassungen unterscheiden sich nur an den Stellen 2 und 3; sie stehen unten vollständig,
-- damit sie sich mit der Testressource
-- (backend/src/test/resources/task/werk-abladestelle-beschreibung.txt) vergleichen lassen.
-- Die erste Fassung dieses Skripts (Commit cae4fbf, nie ausgerollt) hatte Stelle 3 nicht;
-- ihre Beschreibung erkennt Wache 3 nicht. Dort zuerst deren Rücknahme fahren.
--
-- IDEMPOTENT: Ein zweiter Lauf ändert nichts, auch nicht geaendert_am.
-- RÜCKNAHME: scripts/werk-abladestelle-abladestelle-optional-ruecknahme.sql
--
-- Was es NICHT tut: die Muster, die übrigen Felder, den Namen, aktiv, die Freischaltung
-- oder angelegte Aufträge ändern. Jeder Auftrag trägt die Kopie der Definition, mit der
-- er angelegt wurde (E-1023).
--
-- ── Wann ─────────────────────────────────────────────────────────────────────
--
-- Erst NACH dem Start einer Version, deren Backend optionale Schlüsselteile an jeder
-- Stelle zulässt (CHANGELOG.md: „Schlüsselteile frei wählbar“, E-1164). Eine ältere
-- Version liest die Definition danach als ungültig („nur der erste Teil des Schlüssels
-- darf optional sein“): Sie bietet den Typ nicht mehr an, der Assistent verschwindet für
-- jeden freigeschalteten Mandanten, und Anlegen antwortet 404. Das Skript kann das nicht
-- selbst prüfen, die Version hinterlässt keine Spur in der Datenbank.
--
-- Vorher muss das Skript vom 26.10.2026 gelaufen sein
-- (scripts/werk-abladestelle-beschreibung-zeilenformat.sql): Steht noch die Beschreibung
-- von davor, erkennt Wache 3 sie nicht und bricht ab, ohne etwas zu schreiben.
--
-- ── Ausführen ────────────────────────────────────────────────────────────────
--
-- MySQL Workbench, Verbindung mit dem Schreibbenutzer (monitor_write),
-- Vorgabeschema = das eigene Schema (OVERLORD_DB_MONITOR_SCHEMA; auf der Testkopie
-- overlord_monitor). Query > Execute (All or Selection) to Text, Reiter
-- „Query Output“. Die Vorgabe „bei Fehler anhalten“ nicht ändern: Die Wachen
-- brechen mit Fehler 1231 ab, wenn etwas nicht stimmt, und danach läuft nichts mehr.
-- Auf dem Server nach der Deployment-Routine (docs/betrieb.md §7a G):
-- mysql … overlord_monitor < scripts/werk-abladestelle-abladestelle-optional.sql
--
-- Die Texte enthalten Umlaute und typografische Anführungszeichen; SET NAMES legt
-- den Zeichensatz der Verbindung auf utf8mb4 fest, gleich was der Client mitbringt.
-- Käme ein Text trotzdem verfälscht an, erkennt Wache 3 die gespeicherte Beschreibung
-- nicht wieder und bricht ab, bevor etwas geschrieben wird.
--
-- Erwartet: drei Ergebnismengen — „vorher“, „nachher“, „fertig“. In „nachher“ steht
-- abladestelle_pflicht NULL, werk_pflicht 1, neu 2 und zeichen 2433.

SET SESSION max_statement_time = 10;
SET NAMES utf8mb4;

-- Die Beschreibung nach dem Skript vom 26.10.2026, wie sie bis zu diesem Skript galt.
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

Kunden schreiben einen Eintrag oft als Zeile: Schlüssel, Komma, SNDPRN, Komma, SNDPRN_4908. Im Schlüssel trennt „/“ die Teile, das erste Komma beendet den Schlüssel. Beispiel: „0101159903/40NP/123,120764,“ heißt Lieferantennummer 0101159903, Werk 40NP, Abladestelle 123, SNDPRN 120764 und SNDPRN_4908 leer. „0101159903/40NP/,120764,“ heißt Lieferantennummer 0101159903, Werk 40NP, keine Abladestelle, SNDPRN 120764 und SNDPRN_4908 leer. Ein Feld, das nach einem Komma leer bleibt, ist ausdrücklich leer. Einen Schlüsselteil zerlegst du nie weiter, auch wenn er wie 40NP Ziffern und Buchstaben mischt.

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

-- Wache 2: Der Typ ist da, die ersten drei Felder sind die drei Schlüsselteile, und das
-- Werk ist Pflicht. Ohne Pflicht am Werk wäre nach diesem Skript kein Teil des Schlüssels
-- mehr Pflicht, und die Definition wäre ungültig.
SET SESSION sql_mode = IF(
  (SELECT COUNT(*) FROM task_type
   WHERE task_typ = 'WERK_ABLADESTELLE'
     AND JSON_UNQUOTE(JSON_EXTRACT(struktur, '$.felder[0].name')) = 'lieferantennummer'
     AND JSON_UNQUOTE(JSON_EXTRACT(struktur, '$.felder[1].name')) = 'werk'
     AND JSON_UNQUOTE(JSON_EXTRACT(struktur, '$.felder[2].name')) = 'abladestelle'
     AND JSON_CONTAINS(struktur, 'true', '$.felder[1].pflicht') = 1) = 1,
  @@session.sql_mode,
  'TYP_FEHLT_ODER_FELDER_ANDERS_SKRIPT_PASST_NICHT');

-- Wache 3: An der Abladestelle steht "pflicht": true oder schon kein "pflicht", und die
-- Beschreibung ist die alte oder schon die neue. Steht dort etwas Drittes, hat jemand die
-- Definition anders geändert, und dieses Skript passt nicht.
SET SESSION sql_mode = IF(
  (SELECT COUNT(*) FROM task_type
   WHERE task_typ = 'WERK_ABLADESTELLE'
     AND (JSON_CONTAINS(struktur, 'true', '$.felder[2].pflicht') = 1
          OR JSON_CONTAINS_PATH(struktur, 'one', '$.felder[2].pflicht') = 0)
     AND CAST(beschreibung AS BINARY) IN (CAST(@alt AS BINARY), CAST(@neu AS BINARY))) = 1,
  @@session.sql_mode,
  'DEFINITION_UNERWARTET_SKRIPT_PASST_NICHT');

SELECT 'vorher' AS stand,
       JSON_EXTRACT(struktur, '$.felder[2].pflicht') AS abladestelle_pflicht,
       JSON_CONTAINS(struktur, 'true', '$.felder[1].pflicht') AS werk_pflicht,
       (JSON_CONTAINS_PATH(struktur, 'one', '$.felder[2].pflicht') = 0)
       + (CAST(beschreibung AS BINARY) = CAST(@neu AS BINARY)) AS neu,
       CHAR_LENGTH(beschreibung) AS zeichen,
       geaendert_am
FROM task_type
WHERE task_typ = 'WERK_ABLADESTELLE';

-- Die Änderung, in einer Anweisung, damit geaendert_am sich höchstens einmal ändert. Ist
-- beides schon neu, trifft die Bedingung keine Zeile, und geaendert_am bleibt.
UPDATE task_type
SET struktur = IF(JSON_CONTAINS_PATH(struktur, 'one', '$.felder[2].pflicht') = 1,
                  JSON_REMOVE(struktur, '$.felder[2].pflicht'),
                  struktur),
    beschreibung = @neu
WHERE task_typ = 'WERK_ABLADESTELLE'
  AND (JSON_CONTAINS_PATH(struktur, 'one', '$.felder[2].pflicht') = 1
       OR CAST(beschreibung AS BINARY) <> CAST(@neu AS BINARY));

SELECT 'nachher' AS stand,
       JSON_EXTRACT(struktur, '$.felder[2].pflicht') AS abladestelle_pflicht,
       JSON_CONTAINS(struktur, 'true', '$.felder[1].pflicht') AS werk_pflicht,
       (JSON_CONTAINS_PATH(struktur, 'one', '$.felder[2].pflicht') = 0)
       + (CAST(beschreibung AS BINARY) = CAST(@neu AS BINARY)) AS neu,
       CHAR_LENGTH(beschreibung) AS zeichen,
       geaendert_am
FROM task_type
WHERE task_typ = 'WERK_ABLADESTELLE';

SELECT 'fertig' AS stand;
