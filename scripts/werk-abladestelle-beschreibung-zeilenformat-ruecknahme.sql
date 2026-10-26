-- ═══════════════════════════════════════════════════════════════════════════════
-- WERK_ABLADESTELLE: Zeilenformat in der Beschreibung zurücknehmen
-- ═══════════════════════════════════════════════════════════════════════════════
--
-- Die Rücknahme zu scripts/werk-abladestelle-beschreibung-zeilenformat.sql
-- (docs/tasks.md §4.3, E-1155). Danach steht wieder die Beschreibung nach V102: ohne
-- den Absatz zum Zeilenformat, und bei einer Anpassung mit nur einem Wert fragt der
-- Assistent wieder nach, ob der andere entfernt werden soll — auch dann, wenn der
-- Kunde ihn ausdrücklich leer gelassen hat. Wirkt ab dem nächsten eröffneten
-- Gespräch, ohne Neustart.
--
-- IDEMPOTENT: Ein zweiter Lauf ändert nichts, auch nicht geaendert_am.
--
-- Was es NICHT tut: die Struktur, den Namen, die Freischaltung oder angelegte Aufträge
-- ändern.
--
-- ── Ausführen ────────────────────────────────────────────────────────────────
--
-- Wie das Skript selbst: MySQL Workbench, Schreibbenutzer (monitor_write),
-- Vorgabeschema = das eigene Schema, Query > Execute (All or Selection) to Text.
-- Die Vorgabe „bei Fehler anhalten“ nicht ändern. Auf dem Server:
-- mysql … overlord_monitor < scripts/werk-abladestelle-beschreibung-zeilenformat-ruecknahme.sql
--
-- Erwartet: drei Ergebnismengen — „vorher“, „nachher“, „fertig“. In „nachher“ steht
-- alt 1 und zeichen 1666.

SET SESSION max_statement_time = 10;
SET NAMES utf8mb4;

-- Die Beschreibung nach V102, die wieder gelten soll.
SET @alt = 'Die EDI-Plattform erzeugt aus eingehenden Partnernachrichten IDOC-Nachrichten für das Kundensystem und braucht dafür eine SNDPRN. Ist sie nicht fest hinterlegt, ermittelt die Plattform sie über die Werk-/Abladestellentabelle des Kunden. Mit diesem Auftrag legt der Kunde Einträge dieser Tabelle an, passt sie an oder löscht sie.

Ein Eintrag besteht aus einem Schlüssel und zwei Werten. Der Schlüssel besteht aus Werk und Abladestelle, wahlweise mit der Lieferantennummer davor; die Lieferantennummer gehört nur dazu, wenn der Kunde sie für diesen Eintrag nennt. Die Werte sind SNDPRN und SNDPRN_4908 (für VDA-4908-Prozesse), jeweils sechsstellig numerisch.

Neuanlage: Schlüssel und mindestens einer der beiden Werte. Ist der Schlüssel schon vorhanden, bleibt der Eintrag unverändert, und die Rückmeldung lautet „Schlüssel bereits vorhanden“.
Anpassung: der Schlüssel eines vorhandenen Eintrags und die neuen Werte. Ein leer gelassener Wert wird entfernt, mindestens einer muss gesetzt bleiben. Nenne dem Kunden vor dem Anlegen jeden Wert, der dadurch entfernt wird, denn er sieht die bisherigen Werte nicht. Nennt der Kunde bei einer Anpassung nur einen der beiden Werte, frage nach, ob der andere entfernt werden soll oder welchen Wert er behalten soll.
Löschung: nur der Schlüssel.

Ist unklar, ob ein Eintrag neu angelegt oder geändert werden soll, schlage die Neuanlage vor. Gibt es den Schlüssel schon, bleibt der Eintrag unverändert. Eine Anpassung schlägst du nur vor, wenn der Kunde ausdrücklich etwas ändern will.

Ein Auftrag enthält eine Aktion für bis zu 20 Einträge. Ob ein Schlüssel existiert, weiß der Assistent nicht; das zeigt erst die Ausführung.';

-- Die Beschreibung mit dem Zeilenformat, die zurückgenommen wird.
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

-- Wache 3: Die Beschreibung ist die neue oder schon wieder die alte, zeichengleich.
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

UPDATE task_type
SET beschreibung = @alt
WHERE task_typ = 'WERK_ABLADESTELLE'
  AND CAST(beschreibung AS BINARY) = CAST(@neu AS BINARY);

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
