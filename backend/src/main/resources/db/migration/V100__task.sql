-- Overlord Monitoring — Auftraege ueber den Assistenten: Typen, Freischaltung,
-- Auftraege (Linie assistent, Teilschritt A1). Beschrieben in docs/tasks.md,
-- der Vertrag fuer die EDI-Plattform in docs/task-schnittstelle.md.
--
-- DIE ERSTE MIGRATION DER LINIE assistent. Sie beginnt bei V100, damit keine
-- Versionsnummer mit einem Fix auf main kollidiert (DEVELOPMENT_GUIDELINES.md
-- §12). main und assistent teilen sich die Testkopie; main ignoriert diese
-- Migration dort (ignore-migration-patterns *:future) und schliesst die drei
-- Tabellen aus dem Codegen und der Indexpruefung aus.
--
-- WOZU. Kunden beauftragen wiederkehrende Konfigurationsaenderungen an der
-- EDI-Plattform selbst. Eingang ist ausschliesslich der Assistent; erst die
-- Bestaetigung des Kunden legt eine Zeile in task an (E-1001, E-1002). Die
-- Plattform holt offene Auftraege mit monitor_write ab und meldet je Eintrag
-- zurueck (E-1004).
--
-- AUFTRAGSTYPEN SIND DATEN (E-1018). Ein Typ ist eine Zeile in task_type: eine
-- Beschreibung fuer den Assistenten und eine Struktur (JSON), aus der das Backend
-- Pruefung und Parametertext baut. Neue Typen kommen per SQL, ohne Deployment
-- (E-1019). Diese Migration legt nur den ersten an.
--
-- ZEICHENSATZ UND SORTIERUNG stehen an jeder Tabelle (PROJEKTBESCHREIBUNG.md §5).
-- utf8mb4_bin tragen die Kennungen task_id und anfrage_id und die JSON-Spalten.
--
-- JSON-SPALTEN: LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin mit einer
-- Spaltenpruefung CHECK (JSON_VALID(...)). Das ist genau das, was MariaDB aus dem
-- Typ JSON macht — hier mit ausgeschriebener Sortierung, damit sie nicht geerbt
-- ist. Die Pruefung steht an der Spalte und nicht an der Tabelle, in derselben
-- Form, die auch der Typ JSON erzeugt.
--
-- KEIN Fremdschluessel nach GlassfishDB: mandant_id zeigt fachlich auf
-- GlassfishDB.Mandant.MandantID (varchar(36)), als Zeichenkette wie in
-- app_user_mandant.
--
-- KEINE DOWN-MIGRATION, wie ueberall in diesem Projekt.

CREATE TABLE task_type (
  -- Die Kennung, zum Beispiel WERK_ABLADESTELLE. Sie steht im Auftrag und
  -- bestimmt den Ablauf der Plattform.
  task_typ     VARCHAR(64)  NOT NULL,
  -- Anzeigename.
  name         VARCHAR(200) NOT NULL,
  -- Fliesstext fuer den Assistenten. Vertrauenswuerdige Eingabe der Betreuung
  -- (E-1024), nicht des Kunden.
  beschreibung TEXT         NOT NULL,
  -- Aufbau in docs/tasks.md §3.2. Muster darin OHNE Backslash: MariaDB liest ihn
  -- in einem SQL-Text als Escapezeichen, aus \\d im JSON wuerde \d, und
  -- JSON_VALID wiese die Zeile ab (docs/tasks.md §3.5).
  struktur     LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL
               CHECK (JSON_VALID(struktur)),
  -- Eine neue Zeile ist erst aktiv, wenn sie ausdruecklich gesetzt wird: Ein Typ
  -- ohne Ablauf auf der Plattform erzeugte Auftraege, die niemand abholt.
  aktiv        BOOLEAN      NOT NULL DEFAULT FALSE,
  -- Nur zur Erkennung von Aenderungen: Das Backend prueft eine Definition einmal
  -- je (task_typ, geaendert_am) und haelt das Ergebnis (docs/tasks.md §3.6). Von
  -- der Datenbank gesetzt, weil Definitionen per SQL kommen und niemand daran
  -- denken soll, die Spalte mitzuschreiben. Wird nicht angezeigt; die Zeitzone
  -- ist deshalb ohne Belang.
  geaendert_am DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
               ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (task_typ)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Die Freischaltung: Eine Zeile heisst, dieser Mandant bekommt den Typ angeboten,
-- sofern er aktiv und gueltig ist (E-1020). Wer einen Typ loescht, loescht seine
-- Freischaltungen mit.
CREATE TABLE task_type_mandant (
  task_typ   VARCHAR(64) NOT NULL,
  mandant_id VARCHAR(36) NOT NULL,
  PRIMARY KEY (task_typ, mandant_id),
  CONSTRAINT fk_task_type_mandant_typ
    FOREIGN KEY (task_typ) REFERENCES task_type (task_typ) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE task (
  -- UUID, vom Backend erzeugt.
  task_id        CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  -- Aus der Sitzung, nie aus der Anfrage (Regel M1). Bestimmt, wessen
  -- Konfiguration die Plattform aendert.
  mandant_id     VARCHAR(36)   NOT NULL,
  -- Ohne Fremdschluessel auf task_type: Der Auftrag traegt seine Kopie der
  -- Definition (E-1023), eine geaenderte oder geloeschte Typzeile beruehrt ihn
  -- nicht.
  task_typ       VARCHAR(64)   NOT NULL,
  -- Kopie von Name und Struktur zum Anlagezeitpunkt: {"name": ..., "struktur": ...}.
  -- Anzeige und Einordnung lesen sie, nie die aktuelle Typzeile (E-1023).
  definition     LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL
                 CHECK (JSON_VALID(definition)),
  aktion         VARCHAR(64)   NOT NULL,
  -- Die geprueften Eintraege in Eingabereihenfolge, je Eintrag Feldname -> Wert.
  eintraege      LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL
                 CHECK (JSON_VALID(eintraege)),
  -- Vom Client je Absenden erzeugt. Eine Wiederholung mit derselben Kennung
  -- legt keinen zweiten Auftrag an (eindeutiger Index unten).
  anfrage_id     CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  -- Der Parametertext, ausschliesslich vom Backend aus geprueften Einzelwerten
  -- gebaut (E-1006). TEXT, hoechstens 65.535 Bytes; das Backend lehnt laengere
  -- Auftraege vorher ab.
  parameter      TEXT          NOT NULL,
  -- OFFEN, IN_AUSFUEHRUNG, ERLEDIGT, FEHLGESCHLAGEN. VARCHAR und kein ENUM, wie
  -- process_catalog.pflegestatus. "Teilweise erledigt" wird nicht gespeichert,
  -- der Monitor leitet es aus der Rueckgabe ab (E-1009).
  status         VARCHAR(30)   NOT NULL,
  -- Von der Plattform: eine Zeile je Eintrag.
  rueckgabe      TEXT          NULL,
  -- Nur bei FEHLGESCHLAGEN. Den fehlertext sieht der Kunde.
  fehlercode     VARCHAR(64)   NULL,
  fehlertext     VARCHAR(1000) NULL,
  -- Alle Zeitstempel in UTC, gesetzt in der Datenbank ueber UTC_TIMESTAMP(3),
  -- nie aus Java (E-1011): eingestellt_am im INSERT des Backends, die beiden
  -- anderen von der Plattform. Deshalb kein DEFAULT.
  eingestellt_am DATETIME(3)   NOT NULL,
  abgeholt_am    DATETIME(3)   NULL,
  ausgefuehrt_am DATETIME(3)   NULL,
  -- Benutzername des Anlegenden, nach dem Muster von audit_log.actor_username
  -- und in dessen Breite, ohne Fremdschluessel: Ein deaktiviertes Konto soll
  -- seine Auftraege nicht mitnehmen.
  angelegt_von   VARCHAR(190)  NOT NULL,
  PRIMARY KEY (task_id),
  -- Die Liste des Mandanten, neueste zuerst (GET /api/tasks).
  KEY task_mandant_idx (mandant_id, eingestellt_am),
  -- Die Abfrage der offenen Auftraege durch die Plattform.
  KEY task_status_idx (status, eingestellt_am),
  -- Wiederholung desselben Absendens im selben Mandanten.
  UNIQUE KEY uq_task_anfrage (mandant_id, anfrage_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Der erste Typ (docs/tasks.md §4). Name aus Anhang A des Auftraggebers vom
-- 15.10.2026, Beschreibung wortgleich aus dem Auftrag an die Plattform-
-- Entwicklung vom 16.10.2026, Struktur wie im Auftrag zu A1. Aktiv, weil der
-- Ablauf auf der Plattform zugesagt ist.
INSERT INTO task_type (task_typ, name, beschreibung, struktur, aktiv) VALUES (
  'WERK_ABLADESTELLE',
  'Anpassen der Werk-/Abladestellentabelle',
  'Die EDI-Plattform erzeugt aus eingehenden Partnernachrichten IDOC-Nachrichten für das Kundensystem und braucht dafür eine SNDPRN. Ist sie nicht fest hinterlegt, ermittelt die Plattform sie über die Werk-/Abladestellentabelle des Kunden. Mit diesem Auftrag legt der Kunde Einträge dieser Tabelle an, passt sie an oder löscht sie.

Ein Eintrag besteht aus einem Schlüssel und zwei Werten. Der Schlüssel besteht aus Werk und Abladestelle, wahlweise mit der Lieferantennummer davor; die Lieferantennummer gehört nur dazu, wenn der Kunde sie für diesen Eintrag nennt. Die Werte sind SNDPRN und SNDPRN_4908 (für VDA-4908-Prozesse), jeweils sechsstellig numerisch.

Neuanlage: Schlüssel und mindestens einer der beiden Werte. Ist der Schlüssel schon vorhanden, bleibt der Eintrag unverändert, und die Rückmeldung lautet „Schlüssel bereits vorhanden“.
Anpassung: der Schlüssel eines vorhandenen Eintrags und die neuen Werte. Ein leer gelassener Wert wird entfernt, mindestens einer muss gesetzt bleiben. Nenne dem Kunden vor dem Anlegen jeden Wert, der dadurch entfernt wird, denn er sieht die bisherigen Werte nicht.
Löschung: nur der Schlüssel.

Ein Auftrag enthält eine Aktion für bis zu 50 Einträge. Ob ein Schlüssel existiert, weiß der Assistent nicht; das zeigt erst die Ausführung.',
  '{
  "aktionen": ["Neuanlage", "Anpassung", "Löschung"],
  "maxEintraege": 50,
  "felder": [
    { "name": "lieferantennummer", "label": "Lieferantennummer",
      "muster": "^[^ /,]{1,35}$", "beispiel": "0101159903" },
    { "name": "werk", "label": "Werk", "pflicht": true,
      "muster": "^[^ /,]{1,35}$", "beispiel": "72332" },
    { "name": "abladestelle", "label": "Abladestelle", "pflicht": true,
      "muster": "^[^ /,]{1,35}$", "beispiel": "DDB0" },
    { "name": "sndprn", "label": "SNDPRN",
      "muster": "^[0-9]{6}$", "nurBei": ["Neuanlage", "Anpassung"], "beispiel": "120764",
      "hinweis": "Bei Anpassung heißt leer: der Wert wird entfernt." },
    { "name": "sndprn4908", "label": "SNDPRN_4908",
      "muster": "^[0-9]{6}$", "nurBei": ["Neuanlage", "Anpassung"], "beispiel": "100050",
      "hinweis": "Für VDA-4908-Prozesse. Bei Anpassung heißt leer: der Wert wird entfernt." }
  ],
  "schluessel": { "felder": ["lieferantennummer", "werk", "abladestelle"], "trenner": "/" },
  "regeln": [
    { "art": "mindestensEins", "felder": ["sndprn", "sndprn4908"], "bei": ["Neuanlage", "Anpassung"] },
    { "art": "eindeutig", "felder": ["schluessel"] }
  ],
  "zeile": {
    "Neuanlage": "{schluessel},{sndprn},{sndprn4908}",
    "Anpassung": "{schluessel},{sndprn},{sndprn4908}",
    "Löschung": "{schluessel}"
  },
  "rueckgaben": { "ok": "Ok", "weitere": ["Schlüssel nicht gefunden", "Schlüssel bereits vorhanden"] }
}',
  TRUE
);

-- Freigeschaltet fuer die beiden Mandanten aus Anhang A ("Mandanten : NEXANS|IBIS").
INSERT INTO task_type_mandant (task_typ, mandant_id) VALUES
  ('WERK_ABLADESTELLE', 'NEXANS'),
  ('WERK_ABLADESTELLE', 'IBIS');
