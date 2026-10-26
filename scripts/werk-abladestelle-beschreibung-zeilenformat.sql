-- ═══════════════════════════════════════════════════════════════════════════════
-- WERK_ABLADESTELLE: Zeilenformat in der Beschreibung
-- ═══════════════════════════════════════════════════════════════════════════════
--
-- Zielzustand (docs/tasks.md §4.3, E-1155): Die Beschreibung des Typs nennt das
-- Zeilenformat, in dem Kunden einen Eintrag oft schreiben (Schlüssel, Komma, SNDPRN,
-- Komma, SNDPRN_4908), und sagt, dass ein Feld, das nach einem Komma leer bleibt,
-- ausdrücklich leer ist. Gegenüber dem Stand nach V102 ändern sich zwei Stellen:
--
--   1. Nach dem Absatz „Ein Eintrag besteht aus einem Schlüssel und zwei Werten. …“
--      steht ein neuer Absatz zum Zeilenformat, mit einem Beispiel.
--   2. Der Satz zur Anpassung mit nur einem Wert: Nachgefragt wird nur noch, wenn der
--      Kunde einen der beiden Werte gar nicht nennt. Ein ausdrücklich leeres Feld ist
--      genannt; dann sagt der Assistent vor dem Anlegen, dass der Wert entfernt wird.
--
-- Das Skript setzt die ganze Beschreibung, aber nur, wenn sie zeichengleich dem Stand
-- nach V102 entspricht oder schon dem neuen. Steht dort etwas Drittes, bricht es ab.
-- Beide Fassungen stehen unten vollständig, damit sie sich mit der Testressource
-- (backend/src/test/resources/task/werk-abladestelle-beschreibung.txt) vergleichen
-- lassen.
--
-- IDEMPOTENT: Ein zweiter Lauf ändert nichts, auch nicht geaendert_am.
-- RÜCKNAHME: scripts/werk-abladestelle-beschreibung-zeilenformat-ruecknahme.sql
--
-- Was es NICHT tut: die Struktur, den Namen, die Freischaltung oder angelegte Aufträge
-- ändern. Jeder Auftrag trägt die Kopie der Definition, mit der er angelegt wurde
-- (E-1023); die Beschreibung gehört nicht einmal dazu.
--
-- ── Wann ─────────────────────────────────────────────────────────────────────
--
-- An keine Version gebunden (E-1156): Die Beschreibung ist Text für das Modell, kein
-- Code wertet sie aus. Das Skript kann vor oder nach einem Deployment laufen. Es wirkt
-- ab dem nächsten eröffneten Gespräch; ein offenes Gespräch behält seine Anweisung bis
-- zu seinem Ende (E-1045).
--
-- ── Ausführen ────────────────────────────────────────────────────────────────
--
-- MySQL Workbench, Verbindung mit dem Schreibbenutzer (monitor_write),
-- Vorgabeschema = das eigene Schema (OVERLORD_DB_MONITOR_SCHEMA; auf der Testkopie
-- overlord_monitor). Query > Execute (All or Selection) to Text, Reiter
-- „Query Output“. Die Vorgabe „bei Fehler anhalten“ nicht ändern: Die Wachen
-- brechen mit Fehler 1231 ab, wenn etwas nicht stimmt, und danach läuft nichts mehr.
-- Auf dem Server nach der Deployment-Routine (docs/betrieb.md §7a F):
-- mysql … overlord_monitor < scripts/werk-abladestelle-beschreibung-zeilenformat.sql
--
-- Die Texte enthalten Umlaute und typografische Anführungszeichen; SET NAMES legt
-- den Zeichensatz der Verbindung auf utf8mb4 fest, gleich was der Client mitbringt.
-- Käme ein Text trotzdem verfälscht an, erkennt Wache 3 die gespeicherte Beschreibung
-- nicht wieder und bricht ab, bevor etwas geschrieben wird.
--
-- Erwartet: drei Ergebnismengen — „vorher“, „nachher“, „fertig“. In „nachher“ steht
-- neu 1 und zeichen 2272.

SET SESSION max_statement_time = 10;
SET NAMES utf8mb4;

-- Die Beschreibung nach V102, wie sie bis zu diesem Skript galt.
SET @alt = 'Die EDI-Plattform erzeugt aus eingehenden Partnernachrichten IDOC-Nachrichten für das Kundensystem und braucht dafür eine SNDPRN. Ist sie nicht fest hinterlegt, ermittelt die Plattform sie über die Werk-/Abladestellentabelle des Kunden. Mit diesem Auftrag legt der Kunde Einträge dieser Tabelle an, passt sie an oder löscht sie.

Ein Eintrag besteht aus einem Schlüssel und zwei Werten. Der Schlüssel besteht aus Werk und Abladestelle, wahlweise mit der Lieferantennummer davor; die Lieferantennummer gehört nur dazu, wenn der Kunde sie für diesen Eintrag nennt. Die Werte sind SNDPRN und SNDPRN_4908 (für VDA-4908-Prozesse), jeweils sechsstellig numerisch.

Neuanlage: Schlüssel und mindestens einer der beiden Werte. Ist der Schlüssel schon vorhanden, bleibt der Eintrag unverändert, und die Rückmeldung lautet „Schlüssel bereits vorhanden“.
Anpassung: der Schlüssel eines vorhandenen Eintrags und die neuen Werte. Ein leer gelassener Wert wird entfernt, mindestens einer muss gesetzt bleiben. Nenne dem Kunden vor dem Anlegen jeden Wert, der dadurch entfernt wird, denn er sieht die bisherigen Werte nicht. Nennt der Kunde bei einer Anpassung nur einen der beiden Werte, frage nach, ob der andere entfernt werden soll oder welchen Wert er behalten soll.
Löschung: nur der Schlüssel.

Ist unklar, ob ein Eintrag neu angelegt oder geändert werden soll, schlage die Neuanlage vor. Gibt es den Schlüssel schon, bleibt der Eintrag unverändert. Eine Anpassung schlägst du nur vor, wenn der Kunde ausdrücklich etwas ändern will.

Ein Auftrag enthält eine Aktion für bis zu 20 Einträge. Ob ein Schlüssel existiert, weiß der Assistent nicht; das zeigt erst die Ausführung.';

-- Die Beschreibung mit dem Zeilenformat.
SET @neu = 'Die EDI-Plattform erzeugt aus eingehenden Partnernachrichten IDOC-Nachrichten für das Kundensystem und braucht dafür eine SNDPRN. Ist sie nicht fest hinterlegt, ermittelt die Plattform sie über die Werk-/Abladestellentabelle des Kunden. Mit diesem Auftrag legt der Kunde Einträge dieser Tabelle an, passt sie an oder löscht sie.

Ein Eintrag besteht aus einem Schlüssel und zwei Werten. Der Schlüssel besteht aus Werk und Abladestelle, wahlweise mit der Lieferantennummer davor; die Lieferantennummer gehört nur dazu, wenn der Kunde sie für diesen Eintrag nennt. Die Werte sind SNDPRN und SNDPRN_4908 (für VDA-4908-Prozesse), jeweils sechsstellig numerisch.

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

-- Wache 2: Der Typ ist da.
SET SESSION sql_mode = IF(
  (SELECT COUNT(*) FROM task_type WHERE task_typ = 'WERK_ABLADESTELLE') = 1,
  @@session.sql_mode,
  'TYP_FEHLT_SKRIPT_PASST_NICHT');

-- Wache 3: Die Beschreibung ist die alte oder schon die neue, zeichengleich. Steht dort
-- etwas Drittes, hat jemand sie anders geändert, und dieses Skript passt nicht.
SET SESSION sql_mode = IF(
  (SELECT COUNT(*) FROM task_type
   WHERE task_typ = 'WERK_ABLADESTELLE'
     AND CAST(beschreibung AS BINARY) IN (CAST(@alt AS BINARY), CAST(@neu AS BINARY))) = 1,
  @@session.sql_mode,
  'BESCHREIBUNG_UNERWARTET_SKRIPT_PASST_NICHT');

SELECT 'vorher' AS stand,
       CAST(beschreibung AS BINARY) = CAST(@alt AS BINARY) AS alt,
       CAST(beschreibung AS BINARY) = CAST(@neu AS BINARY) AS neu,
       CHAR_LENGTH(beschreibung) AS zeichen,
       geaendert_am
FROM task_type
WHERE task_typ = 'WERK_ABLADESTELLE';

-- Die Änderung. Steht schon die neue Beschreibung da, trifft die Bedingung keine Zeile,
-- und geaendert_am bleibt.
UPDATE task_type
SET beschreibung = @neu
WHERE task_typ = 'WERK_ABLADESTELLE'
  AND CAST(beschreibung AS BINARY) = CAST(@alt AS BINARY);

SELECT 'nachher' AS stand,
       CAST(beschreibung AS BINARY) = CAST(@alt AS BINARY) AS alt,
       CAST(beschreibung AS BINARY) = CAST(@neu AS BINARY) AS neu,
       CHAR_LENGTH(beschreibung) AS zeichen,
       geaendert_am
FROM task_type
WHERE task_typ = 'WERK_ABLADESTELLE';

SET @alt = NULL;
SET @neu = NULL;

SELECT 'fertig' AS stand;
