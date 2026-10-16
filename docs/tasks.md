# Aufträge über den Assistenten — Auftragsmodell (A1)

*Angelegt am 16.10.2026 mit Teilschritt A1 der Linie `assistent`* (Zweig
`assistent-a1-auftragsmodell`, [`ROADMAP.md`](ROADMAP.md) §2). Der Vertrag für die EDI-Plattform
steht für sich lesbar in [`task-schnittstelle.md`](task-schnittstelle.md).

A1 baut das Fundament: drei Tabellen in `overlord_monitor`, die Typdefinition samt Prüfung, den
ersten Typ `WERK_ABLADESTELLE` als Datenzeile, vier Endpunkte und den Plattformvertrag. **Keine
Oberfläche, kein Sprachmodell, keine Ausführung.** Assistent, Chatbox und die Übersicht folgen mit
A2 bis A4 (§8).

---

## 1. Zweck und Ablauf

Kunden beauftragen wiederkehrende Konfigurationsänderungen an der EDI-Plattform selbst, ohne
Umweg über die Betreuung. Der Ablauf, wenn A1 bis A4 stehen:

1. **Assistent.** Der Kunde öffnet den Assistenten (Chat) in der Kopfleiste. Er ist der
   **einzige** Eingang; es gibt keinen Menüpunkt und keine Formularseite (E‑1001). Der Assistent
   sammelt nur Werte, er beurteilt sie nicht (§7).
2. **Karte.** Der Assistent zeigt die gesammelten Werte auf einer Bestätigungskarte. Erst der Klick
   des Kunden schickt `POST /api/tasks` ab — nie das Modell.
3. **`task`.** Das Backend prüft die Werte gegen die Definition des Typs, baut daraus den
   Parametertext und legt den Auftrag mit Status `OFFEN` in `overlord_monitor.task` an. Es gibt
   keinen Freigabeschritt (E‑1002).
4. **Plattform.** Die EDI-Plattform holt offene Aufträge ab (`IN_AUSFUEHRUNG`), führt sie aus und
   meldet zurück (`ERLEDIGT` mit einer Rückgabezeile je Eintrag, oder `FEHLGESCHLAGEN`).
5. **Rückmeldung.** Der Monitor liest die Rückgabe und ordnet den Auftrag ein — Erledigt,
   Teilweise erledigt, Fehlgeschlagen —, an genau einer Stelle (`TaskStatusClassifier`, §5.5).
6. **Übersicht.** Der Kunde sieht den Stand unter „Ihre Aufträge“ (A4).

**Auftragstypen sind Daten** (E‑1018). Ein Typ ist eine Zeile in `task_type`: eine Beschreibung in
Fließtext für den Assistenten und eine Struktur (JSON), aus der das Backend Prüfung, Parametertext
und später die Werkzeuge des Modells baut. Ein neuer Typ, der mit den vorhandenen Regelbausteinen
auskommt, braucht keinen Code und kein Deployment (§3).

---

## 2. Entscheidungen

Herkunft: **A** = vom Auftraggeber am 15. oder 16.10.2026 entschieden. **V** = Vorschlag vom 15.
oder 16.10.2026, ohne Einspruch übernommen. Die E‑Nummer ist `E‑1000` plus die Nummer der
Entscheidung im Auftrag zu A1; `E‑1000` selbst ist nicht vergeben. Die Nummern beginnen bei 1000,
weil auf der Linie `assistent` alle projektweit gezählten Nummern dort beginnen
([`DEVELOPMENT_GUIDELINES.md`](../DEVELOPMENT_GUIDELINES.md) §12).

### Ablauf und Plattform

| E | Entscheidung | Herkunft |
|---|---|---|
| **E‑1001** | Aufträge entstehen ausschließlich über den Assistenten. Kein Menüpunkt, keine Formularseite. | A |
| **E‑1002** | Kein Freigabeschritt. Die Bestätigung des Kunden legt den Auftrag direkt mit Status `OFFEN` an. | A |
| **E‑1003** | Die Auftragstabelle liegt in `overlord_monitor`. | A |
| **E‑1004** | Die Plattform holt Aufträge ab und meldet zurück, mit dem Benutzer `monitor_write`. **Folge:** Sie hat damit alle Rechte auf `overlord_monitor`, einschließlich `app_user` und der Sitzungen. Bewusst so entschieden: kein neuer Benutzer, kein `GRANT`. | A |
| **E‑1005** | Ein Auftrag trägt genau eine Aktion aus den Aktionen seines Typs und eine Liste von Einträgen. | V |
| **E‑1006** | Den Parametertext baut ausschließlich das Backend aus geprüften Einzelwerten. Kein Endpunkt nimmt Parametertext entgegen. | V |
| **E‑1009** | Gesamtstand: **Erledigt** (alle Ok), **Teilweise erledigt** (mindestens einer nicht Ok), **Fehlgeschlagen** (Auftrag nicht verarbeitbar, mit Fehlercode). „Teilweise“ wird **nicht gespeichert**, sondern im Monitor aus der Rückgabe abgeleitet, an genau einer Stelle. Die Plattform setzt nur `ERLEDIGT` oder `FEHLGESCHLAGEN`. | A (Regel), V (Ableitung im Monitor) |
| **E‑1010** | Die Plattform setzt beim Abholen `IN_AUSFUEHRUNG`, mit einem bedingten Update, das nur bei Status `OFFEN` greift. Das verhindert doppelte Ausführung. | V |
| **E‑1011** | Alle Zeitstempel eines Auftrags in UTC, gesetzt in der Datenbank über `UTC_TIMESTAMP(3)`, nie aus Java. | V |
| **E‑1016** | Klarstellung zu [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §8: Die Regeln 1 bis 6 sind mit der Last auf `GlassfishDB` begründet und gelten für Lesezugriffe dort. Tabellen in `overlord_monitor`, die nicht aus `GlassfishDB` abgeleitet sind, folgen ihnen nicht. Regel 7 gilt weiter. | V |

### Typdefinitionen

| E | Entscheidung | Herkunft |
|---|---|---|
| **E‑1018** | Auftragstypen sind Daten: eine Zeile in `task_type` mit Kennung, Name, Beschreibung (Fließtext für den Assistenten) und Struktur (JSON). Im Hauptcode kennt keine Stelle einen bestimmten Typ. | A |
| **E‑1019** | Neue und geänderte Definitionen kommen direkt per SQL in die Produktion, ohne Deployment und ohne Neustart. Das Backend prüft jede Definition beim Laden. Eine ungültige wird nicht angeboten und mit Grund als `WARN` ins Log geschrieben. | A |
| **E‑1020** | Ein Typ wird einem Mandanten angeboten, wenn er aktiv, gültig und für ihn freigeschaltet ist. Sonst existiert er für diesen Mandanten nicht: `404` beim Anlegen, nicht in der Typliste. | V |
| **E‑1021** | Die Struktur kennt einen festen Satz von Regelbausteinen (§3.3). Ein neuer Baustein ist Code und steht danach allen Typen zur Verfügung. Keine Ausdrucks- oder Skriptsprache in der Definition. | V |
| **E‑1022** | Unabhängig von der Definition sind in jedem Wert verboten: Zeilenumbrüche, Steuerzeichen und jedes Zeichen, das in der Zeilenvorlage oder als Schlüsseltrenner wörtlich vorkommt. So kann kein Wert den Parametertext zerlegen, auch wenn ein Muster in der Definition fehlt. | V |
| **E‑1023** | Jeder Auftrag speichert eine Kopie von Name und Struktur, mit denen er angelegt wurde, und seine Einträge strukturiert. Anzeige und Einordnung lesen die Kopie, nie die aktuelle Zeile. Eine Änderung an der Definition wirkt nur auf neue Aufträge. | V |
| **E‑1024** | Beschreibung und Struktur sind vertrauenswürdige Eingaben der Betreuung. Was der Kunde schreibt, ist es nicht. | V |

### Der erste Typ `WERK_ABLADESTELLE`

Diese Regeln stehen als Werte in seiner Definition, nicht im Code (§4).

| E | Entscheidung | Herkunft |
|---|---|---|
| **E‑1007** | Anpassung: Ein leeres SNDPRN-Feld bedeutet „Wert wird entfernt“. Mindestens einer der beiden Werte muss gesetzt bleiben. Die Bedeutung steht in Beschreibung und Plattformvertrag; der Monitor prüft nur die Regel „mindestens eins“. | A |
| **E‑1008** | Neuanlage mit vorhandenem Schlüssel: Eintrag bleibt unverändert, Rückgabe `Schlüssel bereits vorhanden`. Das ist ein dritter Rückgabewert über Anhang A hinaus. | A |
| **E‑1012** | Schlüsselteile: 1 bis 35 Zeichen, kein Leerraum, kein `/`, kein `,`. Groß- und Kleinschreibung bleiben wie eingegeben. Die 35 sind eine technische Schranke, keine fachliche Regel. | V |
| **E‑1013** | SNDPRN und SNDPRN_4908: leer oder genau sechs Ziffern. | A (Anhang A) |
| **E‑1014** | Doppelte Schlüssel innerhalb eines Auftrags werden abgelehnt. | V |
| **E‑1015** | Höchstens 50 Einträge je Auftrag. | A (16.10.2026) |
| **E‑1017** | Lesarten in Anhang A: „Anpasung“ ist „Anpassung“. „Löschen eines oder mehrerer neuer Einträge“ heißt „vorhandener“. | V |

### Rückfragen zu A1 (16.10.2026)

Drei Regeln der Richtlinien widersprachen dem Auftrag oder ließen ihn offen. Der Auftraggeber hat
am 16.10.2026 entschieden:

| E | Entscheidung | Herkunft |
|---|---|---|
| **E‑1025** | Die Pfade heißen `/api/tasks` und `/api/tasks/typen`, die Antwortfelder sind deutsch. Das weicht von [`DEVELOPMENT_GUIDELINES.md`](../DEVELOPMENT_GUIDELINES.md) §5.2 ab (deutsche Pfadsegmente, englische Felder): Tabelle und Plattformvertrag heißen `task`, das Vorbild für einen englischen Pfad ist `/api/admin/users`; deutsche Felder sind die gelebte Praxis des Projekts. | A |
| **E‑1026** | Regel 7 gilt vollständig: Unter M1000 stehen je Abfrage `EXPLAIN` **und** die Laufzeit aus dem Profil, aus einer Sitzung mit wenigen Läufen — kein Lastgerüst, keine Wiederholungsreihen. | A |
| **E‑1027** | Die Klarstellung aus E‑1016 steht nicht nur in der Projektbeschreibung, sondern mit einem Satz auch bei L1 und L3 in [`DEVELOPMENT_GUIDELINES.md`](../DEVELOPMENT_GUIDELINES.md) §4.4 und bei Regel 4 und 5 in [`CLAUDE.md`](../CLAUDE.md). Sonst hielte ein späterer Schritt `GET /api/tasks` ohne Zeitfenster für einen Regelverstoß. | A |

---

## 3. Auftragstypen definieren

*Geschrieben für die Betreuung (Lukas), die neue Typen per SQL anlegt.* Ein Typ, der mit den
Regelbausteinen unten auskommt, braucht keinen Code, kein Deployment und keinen Neustart (E‑1019).

### 3.1 Die Tabellen

**`task_type`** — eine Zeile je Typ.

| Spalte | Typ | Bedeutung |
|---|---|---|
| `task_typ` | `VARCHAR(64)`, Primärschlüssel | Kennung, zum Beispiel `WERK_ABLADESTELLE`. Steht im Auftrag und bestimmt den Ablauf der Plattform. Die Spalte vergleicht ohne Rücksicht auf Groß- und Kleinschreibung; das Backend vergleicht die Kennung in der Anfrage **genau** |
| `name` | `VARCHAR(200)` NOT NULL | Anzeigename, nicht leer |
| `beschreibung` | `TEXT` NOT NULL | Fließtext für den Assistenten: was der Auftrag bewirkt, was er nicht weiß, was er dem Kunden vor dem Anlegen sagen muss |
| `struktur` | JSON NOT NULL, von MariaDB per `JSON_VALID` geprüft | Aufbau nach §3.2 |
| `aktiv` | `BOOLEAN` NOT NULL, Vorgabe `FALSE` | eine neue Zeile ist erst aktiv, wenn sie ausdrücklich gesetzt wird |
| `geaendert_am` | `DATETIME(3)` NOT NULL, Vorgabe und `ON UPDATE` `CURRENT_TIMESTAMP(3)` | nur zur Erkennung von Änderungen (§3.6), wird nicht angezeigt |

**`task_type_mandant`** — die Freischaltung. Eine Zeile `(task_typ, mandant_id)` heißt: Dieser
Mandant bekommt den Typ angeboten, sofern er aktiv und gültig ist. Primärschlüssel über beide
Spalten, Fremdschlüssel auf `task_type` mit `ON DELETE CASCADE` — wer einen Typ löscht, löscht seine
Freischaltungen mit. Die Mandantenkennung ist die aus `GlassfishDB.Mandant`, ohne Fremdschlüssel
über die Schemagrenze.

### 3.2 Die Struktur

Ein JSON-Objekt. **Pflicht** sind `aktionen`, `maxEintraege`, `felder`, `zeile` und `rueckgaben`;
**optional** `schluessel` und `regeln`. Ohne `schluessel` darf keine Vorlage `{schluessel}`
verwenden. **Unbekannte Schlüssel machen die Definition ungültig** — auf jeder Ebene, auch in einem
Feld, einer Regel oder in `rueckgaben`. Ein Tippfehler (`pflichT`, `muser`) fällt so auf, statt still
wirkungslos zu bleiben.

| Schlüssel | Bedeutung |
|---|---|
| `aktionen` | Liste der Aktionen in Klartext, 1 bis 10, eindeutig, je 1 bis 64 Zeichen, ohne Zeilenumbruch und Steuerzeichen. Sie stehen so in der Kopfzeile des Parametertexts |
| `maxEintraege` | Höchstzahl Einträge je Auftrag, ganze Zahl von 1 bis 500 |
| `felder` | Liste der Felder, mindestens eines. Je Feld: `name` (Pflicht, `[a-z][a-z0-9]*`, eindeutig, nicht `schluessel`), `label` (Pflicht, nicht leer — so nennt der Klartext einer Prüfmeldung das Feld), `pflicht` (`true` oder `false`, Vorgabe `false`; gilt bei den Aktionen, bei denen das Feld vorkommt), `muster` (regulärer Ausdruck nach Java, den der **ganze** Wert erfüllen muss, optional), `nurBei` (Liste von Aktionen, optional, nicht leer; außerhalb davon ist das Feld verboten), `beispiel` und `hinweis` (optional, Text, für den Assistenten) |
| `schluessel` | `felder` (Liste von Feldnamen in Reihenfolge, mindestens einer, ohne `nurBei`) und `trenner` (genau ein Zeichen, kein Zeilenumbruch, kein Steuerzeichen). **Nur der erste Teil darf ein Feld ohne `pflicht` sein**, und mindestens ein Teil muss `pflicht` sein. Fehlt der Wert des ersten Teils, entfällt er mit seinem Trenner |
| `regeln` | Liste aus dem festen Satz in §3.3 |
| `zeile` | je Aktion genau eine Vorlage für die Zeile eines Eintrags, keine für eine unbekannte Aktion. Platzhalter `{feldname}` und `{schluessel}`; ein fehlender Wert wird zu leerem Text. Außerhalb eines Platzhalters keine geschweifte Klammer, kein Zeilenumbruch, kein Steuerzeichen |
| `rueckgaben` | `ok` (der Wert, der Erfolg heißt; Pflicht, nicht leer) und `weitere` (Liste bekannter anderer Werte, optional). Kein Wert mit Leerraum am Rand oder mit Zeilenumbruch — die Rückgabe wird zeilenweise und ohne Leerraum am Rand verglichen (§5.4) |

**Beispiel** — eine Struktur ohne fachliche Bedeutung, nur zur Form:

```json
{
  "aktionen": ["Anlage", "Entfernen"],
  "maxEintraege": 20,
  "felder": [
    { "name": "bereich", "label": "Bereich", "muster": "^[A-Z]{2}$", "beispiel": "NO" },
    { "name": "nummer", "label": "Nummer", "pflicht": true, "muster": "^[0-9]{1,10}$" },
    { "name": "wert", "label": "Wert", "nurBei": ["Anlage"], "pflicht": true,
      "hinweis": "Der Wert, der unter der Nummer hinterlegt wird." }
  ],
  "schluessel": { "felder": ["bereich", "nummer"], "trenner": "-" },
  "regeln": [ { "art": "eindeutig", "felder": ["schluessel"] } ],
  "zeile": { "Anlage": "{schluessel};{wert}", "Entfernen": "{schluessel}" },
  "rueckgaben": { "ok": "OK", "weitere": ["UNBEKANNT"] }
}
```

Daraus entsteht für eine Anlage mit zwei Einträgen der Parametertext

```
Aktion=Anlage
NO-17;abc
42;xyz
```

— im zweiten Eintrag fehlt der optionale erste Schlüsselteil, er entfällt mit seinem Trenner. In
jedem Wert verboten sind dabei `-` (Trenner) und `;` (steht wörtlich in einer Vorlage), §3.4.

### 3.3 Die Regelbausteine

Der feste Satz (E‑1021). Jede Regel ist ein Objekt mit `art`:

| `art` | Schlüssel | Wirkung | Fehlercode |
|---|---|---|---|
| `mindestensEins` | `felder` (Liste von Feldnamen, mindestens einer), `bei` (Liste von Aktionen, optional) | In jedem Eintrag hat mindestens eines der Felder einen Wert. Mit `bei` nur bei diesen Aktionen, sonst bei allen | `MINDESTENS_EINS` |
| `eindeutig` | `felder` (Liste von Feldnamen, mindestens einer; `schluessel` ist als Name zulässig, wenn die Struktur einen Schlüssel hat) | Keine zwei Einträge eines Auftrags tragen dieselbe Kombination dieser Werte. Verglichen wird genau, mit Groß- und Kleinschreibung. Einträge, bei denen alle diese Werte fehlen, zählen nicht | `DOPPELT` |

Ein neuer Baustein ist Code in `AuftragsPruefung` und `DefinitionsPruefung` und steht danach allen
Typen zur Verfügung. Eine Ausdrucks- oder Skriptsprache gibt es bewusst nicht.

### 3.4 Die automatische Zeichenprüfung

Unabhängig von jedem `muster` lehnt das Backend in **jedem** Wert ab (E‑1022, Fehlercode
`UNZULAESSIGES_ZEICHEN`):

- Zeilenumbrüche: LF, CR, `U+0085`, `U+2028`, `U+2029`;
- Steuerzeichen (Unicode-Kategorie Cc, `U+0000` bis `U+001F` und `U+007F` bis `U+009F`), also auch
  den Tabulator;
- jedes Zeichen, das nach Entfernen der Platzhalter in **irgendeiner** Zeilenvorlage des Typs steht;
- den Schlüsseltrenner.

Bei `WERK_ABLADESTELLE` sind das außer Umbrüchen und Steuerzeichen `,` und `/`. Ein Wert kann den
Parametertext damit nicht in zusätzliche Zeilen oder Spalten zerlegen, auch wenn die Definition ein
Muster vergisst. Das `muster` bleibt trotzdem die fachliche Prüfung: Ein Leerzeichen etwa ist kein
Steuerzeichen und fällt nur über das Muster (`FORMAT`).

### 3.5 Muster ohne Backslash

**Schreibe Muster ohne Backslash:** `[0-9]` statt `\d`, ein Leerzeichen statt `\s`. MariaDB liest in
einem SQL-Text den Backslash als Escapezeichen: Aus `\\d` im JSON würde beim Einfügen `\d`, das ist
kein gültiges JSON, und die Prüfung `JSON_VALID` wiese die Zeile ab. Tabulator und Zeilenumbruch
muss ein Muster nicht abwehren, das tut die Zeichenprüfung (§3.4).

Muster sind reguläre Ausdrücke nach `java.util.regex.Pattern` und müssen auf den **ganzen** Wert
passen (`matches`); `^` und `$` schaden nicht und machen das sichtbar.

### 3.6 Was beim Laden geprüft wird — und wo ein Fehler sichtbar wird

Das Backend liest die Definitionen bei jedem Zugriff aus der Datenbank, prüft eine Definition aber
nur einmal je `(task_typ, geaendert_am)` und hält das Ergebnis im Speicher. **Eine geänderte, neue
oder gelöschte Zeile wirkt beim nächsten Zugriff**, ohne Neustart. Gelesen werden je Zugriff nur die
Typen, die für den aktiven Mandanten freigeschaltet und aktiv sind (§5.6).

Geprüft wird beim Laden, jeweils mit Grund:

1. Die Struktur ist ein JSON-Objekt; der Name ist nicht leer.
2. Die Pflichtschlüssel sind da; es gibt keinen unbekannten Schlüssel, auf keiner Ebene.
3. `aktionen`: 1 bis 10, eindeutig, je 1 bis 64 Zeichen, ohne Zeilenumbruch und Steuerzeichen.
4. `maxEintraege`: ganze Zahl von 1 bis 500.
5. `felder`: mindestens eines; je Feld `name` nach `[a-z][a-z0-9]*`, eindeutig, nicht
   `schluessel`; `label` nicht leer; `pflicht` ein Wahrheitswert; jedes `muster` übersetzbar;
   `nurBei` nicht leer und nur aus bekannten Aktionen; `beispiel` und `hinweis` Text.
6. `schluessel`: Felder bekannt, eindeutig und ohne `nurBei`; `trenner` genau ein Zeichen, kein
   Umbruch, kein Steuerzeichen; ab dem zweiten Teil `pflicht`; mindestens ein Teil `pflicht`.
7. `regeln`: jede `art` aus dem festen Satz; jeder Feldname bekannt (`schluessel` nur bei
   `eindeutig` und nur mit Schlüssel); jede Aktion in `bei` bekannt.
8. `zeile`: für jede Aktion genau eine Vorlage, keine für eine unbekannte Aktion; jeder Platzhalter
   ein bekanntes Feld oder `{schluessel}` (nur mit Schlüssel); keine einzelne geschweifte Klammer,
   kein Umbruch, kein Steuerzeichen.
9. `rueckgaben`: `ok` nicht leer, ohne Leerraum am Rand und Umbruch, und nicht in `weitere`; jeder
   Wert in `weitere` ebenso.

**Eine ungültige Definition wird nicht angeboten** und steht mit allen Gründen als `WARN` im Log der
Anwendung — **einmal je Fassung**, nicht bei jedem Zugriff. Sie erscheint weder in der Typliste noch
lässt sie sich anlegen (`404`, E‑1020). **Sichtbar ist das nur im Log** (Punkt 1005); geprüft wird
eine Definition erst, wenn ein Mandant, für den sie freigeschaltet und aktiv ist, das erste Mal
zugreift.

### 3.7 Ein neuer Typ, Schritt für Schritt

**Die Reihenfolge ist Pflicht: erst den Ablauf in Lobster bauen, dann aktivieren und
freischalten.** Ein aktiver, freigeschalteter Typ ohne Ablauf auf der Plattform führt zu Aufträgen,
die niemand abholt. Die Plattform bekommt die Definition, bevor der Typ aktiviert wird
([`task-schnittstelle.md`](task-schnittstelle.md) §7).

```sql
-- 1. Typ anlegen, inaktiv (aktiv hat die Vorgabe FALSE). Muster ohne Backslash.
INSERT INTO overlord_monitor.task_type (task_typ, name, beschreibung, struktur)
VALUES (
  'BEISPIEL',
  'Beispiel ohne fachliche Bedeutung',
  'Was der Auftrag bewirkt und was der Assistent dem Kunden vor dem Anlegen sagen muss.',
  '{
    "aktionen": ["Anlage"],
    "maxEintraege": 20,
    "felder": [
      { "name": "nummer", "label": "Nummer", "pflicht": true, "muster": "^[0-9]{1,10}$" }
    ],
    "zeile": { "Anlage": "{nummer}" },
    "rueckgaben": { "ok": "OK" }
  }'
);

-- 2. Fuer einen Mandanten freischalten. Noch wirkt nichts: Der Typ ist inaktiv.
INSERT INTO overlord_monitor.task_type_mandant (task_typ, mandant_id) VALUES ('BEISPIEL', 'NEXANS');

-- 3. Erst wenn der Ablauf in Lobster steht: aktivieren.
UPDATE overlord_monitor.task_type SET aktiv = TRUE WHERE task_typ = 'BEISPIEL';
```

Danach in der Typliste des Mandanten nachsehen (`GET /api/tasks/typen`). Fehlt der Typ dort, steht
der Grund im Log (§3.6). Zurücknehmen: `aktiv = FALSE` — der Typ verschwindet beim nächsten Zugriff
aus dem Angebot; angelegte Aufträge bleiben unberührt.

### 3.8 Eine Änderung wirkt nur auf neue Aufträge

Jeder Auftrag trägt eine Kopie von Name und Struktur, mit der er angelegt wurde (E‑1023). Anzeige
und Einordnung lesen die Kopie. Wer `rueckgaben` oder eine Vorlage ändert, ändert also nichts an
Aufträgen, die schon in `task` stehen — auch nicht an solchen, die die Plattform noch nicht
abgeholt hat: Deren Parametertext ist fertig gebaut.

---

## 4. Der erste Typ `WERK_ABLADESTELLE`

### 4.1 Quelle: die Taskdefinition des Auftraggebers (15.10.2026), wortgleich

```
Taskname :
Anpassen der Werk-/Abladestellentabelle

Aktionen :
Anlegen eines oder mehrerer neuer Einträge, Anpassen eines oder mehrerer vorhandener Einträge, Löschen eines oder mehrerer neuer Einträge

Beschreibung :
Die EDI Plattform erzeugt aus den eingehenden Partnernachrichten IDOC Nachrichten für das Kundensystem. Dazu wird eine SNDPRN benötigt. Dieses ist ein sechsstelliger, numerischer Wert. Ist dieser nicht als Fixwert hinterlegt muss er über eine Kombination aus der Lieferantenummer, dem Werk und der Abladestelle, die aus der Eingangsnachricht gelesen werden, ermittelt werden. Ist die Lieferantennummer nicht ermittelbar, kann alternativ auch nur mit der Kombination aus dem Werk und der Abladestelle gearbeitet werden.
Ein Werk-/Abladestellentabelleneintrag besteht aus drei Teilen.
Dem Schlüssel, der SNDPRN, sowie der SNDPRN_4908 für die VDA4908 Prozesse.
Der Schlüssel setzt sich aus zwei (Werk und Abladestelle) oder drei Werten zusammen (Lieferantennummer und Werk und Abladestelle).
Die SNDPRN ist ein sechsstelliger, numerischer Wert und optional.
Die SNDPRN_4908 ist ein sechsstelliger, numerischer Wert und optional.
Mindestens einer der beiden SNDPRN Werte muss angegeben werden.
Der Schlüssel ist verpflichtend.
Die Übergabe mehrerer Listeneinträge zur Neuanlage soll möglich sein.
Die Übergabe mehrerer Listeneinträge zur Löschung soll möglich sein, dabei reicht es den Schlüssel des Eintrags anzugeben.
Die Übergabe mehrerer Listeneinträge zur Anpassung soll möglich sein, dabei reicht es den Schlüssel des Eintrags anzugeben, der angepasst werden soll, sowie die neu zu setzenden Werte für SNDPRN und SNDPRN_4908.
Die vom Bediener im Dialog ermittelten Werte sollen wie folgt in der Tasktabelle als Parameter für die Auftragsausführung hinterlegt werden :
Aktion=Neuanlage oder Löschung oder Anpassung
Liste der einzelnen Aktionen

Beispiel : Neuanlage
Aktion=Neuanlage
72332/DDB0,120764,
72261/DDR0,120764,
010/18VZE,120055,100050
0101159903/22/001N0,123085,

Beispiel : Löschung
Aktion=Löschung
010/18VZE
0101159903/22/001N0

Beispiel : Anpassung
Aktion=Anpasung
010/18VZE,123456,123456
0101159903/22/001N0,234567,

Mandanten :
NEXANS|IBIS

Task eingestellt am :
Zeitpunkt, zu dem der Task zur Verfügung gestellt wurde

Task ausgeführt am :
Zeitpunkt, zu dem der Task zurückgemeldet wurde

Rückgabewerte :
Zu jedem Listeneintrag wird eine Rückmeldung gegeben. Folgende Werte sind möglich : Ok, Schlüssel nicht gefunden
Beispiel für eine Übergabeliste mit drei Einträgen :
Ok
Schlüssel nicht gefunden
Ok
```

### 4.2 Lesarten und Regeln

- **Lesarten (E‑1017):** „Anpasung“ im Beispiel ist „Anpassung“. „Löschen eines oder mehrerer
  *neuer* Einträge“ heißt „*vorhandener*“.
- **Anpassung, leeres SNDPRN-Feld (E‑1007):** heißt „Wert wird entfernt“. Mindestens einer der
  beiden Werte muss gesetzt bleiben. Die Bedeutung steht in der Beschreibung und im
  Plattformvertrag; der Monitor prüft nur „mindestens eins“.
- **Neuanlage mit vorhandenem Schlüssel (E‑1008):** Eintrag bleibt unverändert, Rückgabe
  `Schlüssel bereits vorhanden` — ein dritter Rückgabewert über die Quelle hinaus.
- **Schlüsselteile (E‑1012):** 1 bis 35 Zeichen, kein Leerraum, kein `/`, kein `,`; Groß- und
  Kleinschreibung bleiben wie eingegeben. Die 35 sind eine technische Schranke (Punkt 1002).
- **SNDPRN und SNDPRN_4908 (E‑1013):** leer oder genau sechs Ziffern.
- **Doppelte Schlüssel (E‑1014)** innerhalb eines Auftrags werden abgelehnt.
- **Höchstens 50 Einträge (E‑1015).**
- **Mandanten:** freigeschaltet für `NEXANS` und `IBIS`, wie in der Quelle.

### 4.3 Die Definition, wie in der Migration `V100`

Name: **Anpassen der Werk-/Abladestellentabelle**, `aktiv = TRUE`.

```json
{
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
}
```

Die Beschreibung (Fließtext für den Assistenten), wortgleich aus dem Auftrag an die
Plattform-Entwicklung vom 16.10.2026:

```
Die EDI-Plattform erzeugt aus eingehenden Partnernachrichten IDOC-Nachrichten für das Kundensystem und braucht dafür eine SNDPRN. Ist sie nicht fest hinterlegt, ermittelt die Plattform sie über die Werk-/Abladestellentabelle des Kunden. Mit diesem Auftrag legt der Kunde Einträge dieser Tabelle an, passt sie an oder löscht sie.

Ein Eintrag besteht aus einem Schlüssel und zwei Werten. Der Schlüssel besteht aus Werk und Abladestelle, wahlweise mit der Lieferantennummer davor; die Lieferantennummer gehört nur dazu, wenn der Kunde sie für diesen Eintrag nennt. Die Werte sind SNDPRN und SNDPRN_4908 (für VDA-4908-Prozesse), jeweils sechsstellig numerisch.

Neuanlage: Schlüssel und mindestens einer der beiden Werte. Ist der Schlüssel schon vorhanden, bleibt der Eintrag unverändert, und die Rückmeldung lautet „Schlüssel bereits vorhanden“.
Anpassung: der Schlüssel eines vorhandenen Eintrags und die neuen Werte. Ein leer gelassener Wert wird entfernt, mindestens einer muss gesetzt bleiben. Nenne dem Kunden vor dem Anlegen jeden Wert, der dadurch entfernt wird, denn er sieht die bisherigen Werte nicht.
Löschung: nur der Schlüssel.

Ein Auftrag enthält eine Aktion für bis zu 50 Einträge. Ob ein Schlüssel existiert, weiß der Assistent nicht; das zeigt erst die Ausführung.
```

**Woran die Regeln hängen:** Der Schlüssel aus zwei oder drei Teilen ist `schluessel` mit der
optionalen `lieferantennummer` vorn. „Löschung: nur der Schlüssel“ ist `nurBei` an beiden
SNDPRN-Feldern — eine Löschung mit SNDPRN wird mit `FELD_NICHT_ERLAUBT` abgelehnt. „Mindestens
einer der beiden Werte“ ist `mindestensEins` bei Neuanlage und Anpassung. Keine Zeile Code nennt den
Typ (E‑1018); ein Test hält das fest (§9).

---

## 5. Datenmodell, Parametertext, Einordnung

### 5.1 Die Migration `V100__task.sql`

Die erste Migration der Linie `assistent` (Migrationen ab `V100`,
[`DEVELOPMENT_GUIDELINES.md`](../DEVELOPMENT_GUIDELINES.md) §12). Zeichensatz `utf8mb4` und Sortierung
`utf8mb4_general_ci` stehen explizit an jeder Tabelle; die Kennungen `task_id` und `anfrage_id`
tragen `utf8mb4_bin` ([`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §5). Kein Fremdschlüssel
nach `GlassfishDB`. Die JSON-Spalten sind `LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin` mit
`CHECK (JSON_VALID(…))` — das ist genau das, was MariaDB aus dem Typ `JSON` macht, hier aber mit
ausgeschriebener Sortierung. Die Migration legt `WERK_ABLADESTELLE` an (§4.3) und schaltet ihn für
`NEXANS` und `IBIS` frei.

**`task`**

| Spalte | Typ | Bemerkung |
|---|---|---|
| `task_id` | `CHAR(36)` `utf8mb4_bin`, Primärschlüssel | UUID, vom Backend erzeugt |
| `mandant_id` | `VARCHAR(36)` NOT NULL | aus der Sitzung |
| `task_typ` | `VARCHAR(64)` NOT NULL | ohne Fremdschlüssel: Der Auftrag trägt seine Kopie (E‑1023) |
| `definition` | JSON NOT NULL | Kopie zum Anlagezeitpunkt: `{"name": …, "struktur": …}`, die Struktur inhaltlich unverändert aus `task_type` (dieselbe Fassung, die beim Anlegen geprüft wurde) |
| `aktion` | `VARCHAR(64)` NOT NULL | eine der Aktionen der Kopie |
| `eintraege` | JSON NOT NULL | die geprüften Einträge in Eingabereihenfolge, je Eintrag ein Objekt Feldname → Wert, nur Felder mit Wert, in der Reihenfolge der Definition |
| `anfrage_id` | `CHAR(36)` `utf8mb4_bin` NOT NULL | vom Client je Absenden erzeugt, für Wiederholungen; klein geschrieben gespeichert |
| `parameter` | `TEXT` NOT NULL | Parametertext (§5.2) |
| `status` | `VARCHAR(30)` NOT NULL | `OFFEN`, `IN_AUSFUEHRUNG`, `ERLEDIGT`, `FEHLGESCHLAGEN` |
| `rueckgabe` | `TEXT` NULL | von der Plattform, eine Zeile je Eintrag |
| `fehlercode` | `VARCHAR(64)` NULL | nur bei `FEHLGESCHLAGEN` |
| `fehlertext` | `VARCHAR(1000)` NULL | Klartext, wird dem Kunden gezeigt |
| `eingestellt_am` | `DATETIME(3)` NOT NULL | UTC, im `INSERT` über `UTC_TIMESTAMP(3)` (E‑1011) |
| `abgeholt_am` | `DATETIME(3)` NULL | UTC, setzt die Plattform |
| `ausgefuehrt_am` | `DATETIME(3)` NULL | UTC, setzt die Plattform bei der Rückmeldung |
| `angelegt_von` | `VARCHAR(190)` NOT NULL | Benutzername des Anlegenden, wie `audit_log.actor_username`, ohne Fremdschlüssel |

**`angelegt_von` ist `VARCHAR(190)`** — dieselbe Breite wie `audit_log.actor_username`, nach dessen
Muster die Spalte gebaut ist. Der Auftrag an die Plattform vom 16.10.2026 nannte vorläufig
`VARCHAR(100)`; die Plattform liest die Spalte nicht.

Indizes:

| Index | Spalten | Zweck |
|---|---|---|
| `PRIMARY` | `task_id` | Einzelabruf, bedingte Updates der Plattform |
| `task_mandant_idx` | `mandant_id, eingestellt_am` | Liste des Mandanten, neueste zuerst |
| `task_status_idx` | `status, eingestellt_am` | Abfrage der offenen Aufträge durch die Plattform |
| `uq_task_anfrage` (eindeutig) | `mandant_id, anfrage_id` | Wiederholung mit derselben `anfrageId` |

`task_type` hat nur `PRIMARY (task_typ)`, `task_type_mandant` nur `PRIMARY (task_typ, mandant_id)`;
der Fremdschlüssel `fk_task_type_mandant_typ` nutzt den Primärschlüssel und braucht keinen eigenen
Index. Gehalten von `IndexbestandDbIT` über die Sollliste.

### 5.2 Parametertext (exakt)

Erste Zeile `Aktion=<Aktion>`, dann je Eintrag die Zeile nach der Vorlage seiner Aktion, in
Eingabereihenfolge. `{schluessel}` sind die Schlüsselteile mit Wert, verbunden mit dem Trenner;
`{feldname}` ist der Wert oder leerer Text. Ersetzt wird in einem Durchgang — ein Wert, der wie ein
Platzhalter aussieht, wird nicht noch einmal ersetzt. Zeilenende LF, **kein** abschließendes LF.
Kodierung UTF-8.

Mit `WERK_ABLADESTELLE` entstehen aus den entsprechenden Eingaben exakt die drei Texte der Quelle
(mit der Lesart „Anpassung“):

```
Aktion=Neuanlage
72332/DDB0,120764,
72261/DDR0,120764,
010/18VZE,120055,100050
0101159903/22/001N0,123085,
```

```
Aktion=Löschung
010/18VZE
0101159903/22/001N0
```

```
Aktion=Anpassung
010/18VZE,123456,123456
0101159903/22/001N0,234567,
```

**Die Spalte ist `TEXT`, höchstens 65.535 Bytes.** Bei `WERK_ABLADESTELLE` liegt der längste
mögliche Text bei rund 6 KB. Ein Typ ohne Muster mit vielen Einträgen könnte die Grenze reißen; das
Backend lehnt einen solchen Auftrag deshalb mit `ZU_LANG` ab, statt an der Datenbank zu scheitern.

### 5.3 Prüfung eines Auftrags

In dieser Reihenfolge, **alle Fehler gesammelt** statt beim ersten abzubrechen:

1. Die Aktion ist eine der Aktionen der Definition (`AKTION_UNBEKANNT`).
2. Die Zahl der Einträge liegt zwischen 1 und `maxEintraege` (`ZU_WENIGE_EINTRAEGE`,
   `ZU_VIELE_EINTRAEGE`).
3. Je Eintrag: nur bekannte Feldnamen (`FELD_UNBEKANNT`); ein Feld außerhalb seiner
   `nurBei`-Aktionen trägt keinen Wert (`FELD_NICHT_ERLAUBT`); jedes `pflicht`-Feld hat bei den
   Aktionen, bei denen es vorkommt, einen Wert (`PFLICHT_FEHLT`); jeder Wert passt auf sein `muster`
   (`FORMAT`); kein Wert enthält ein Zeichen nach §3.4 (`UNZULAESSIGES_ZEICHEN`).
4. Die Regeln der Definition (`MINDESTENS_EINS`, `DOPPELT`).

`null` und leerer Text heißen gleichermaßen „fehlend“. **Sonst wird kein Wert verändert**, auch nicht
um Leerraum gekürzt: `" 72332"` ist ein Wert mit Leerzeichen und fällt über das Muster. Ist die
Aktion unbekannt, entfallen die Prüfungen, die an der Aktion hängen (`nurBei`, `pflicht`, Regeln
mit `bei`); alle übrigen laufen. Ein Wert, der zugleich das Muster verfehlt und ein verbotenes
Zeichen trägt, bekommt beide Meldungen. `DOPPELT` steht am späteren der beiden Einträge.

Jeder Fehler trägt den Index des Eintrags (ab 0, `null` für den Auftrag als Ganzes), das Feld
(`null`, wenn die Meldung kein einzelnes Feld betrifft), einen stabilen Code und einen deutschen
Klartext, der das Feld mit seinem `label` nennt und den Eintrag ab 1 zählt
(„Eintrag 3, SNDPRN: …“).

### 5.4 Rückgabe lesen

An LF trennen; ein abschließendes CR je Zeile entfernen; Leerraum am Rand entfernen. Endet die
Rückgabe mit einem Zeilenende, entsteht dadurch keine zusätzliche leere Zeile. Jede Zeile wird
gegen die **Kopie** der Definition im Auftrag gehalten:

| Zeile | Ergebnis |
|---|---|
| gleich `rueckgaben.ok` | `OK` |
| in `rueckgaben.weitere` | `NICHT_OK` |
| jeder andere Wert | `UNBEKANNT` — zählt als nicht Ok |

Verglichen wird genau, mit Groß- und Kleinschreibung. **Der Rohwert wird immer mitgeliefert und nie
umgedeutet.** Eine Rückgabe, die fehlt (`NULL`) oder nur aus Leerraum besteht, gilt als fehlend.

### 5.5 Einordnung (`TaskStatusClassifier`)

Die eine Stelle, an der aus gespeichertem Status und Rückgabe der Gesamtstand wird (E‑1009).

| gespeicherter Status | Rückgabe | Einordnung |
|---|---|---|
| `OFFEN` | beliebig | `OFFEN` |
| `IN_AUSFUEHRUNG` | beliebig | `IN_AUSFUEHRUNG` |
| `ERLEDIGT` | Zeilenzahl gleich Eintragszahl, alle `OK` | `ERLEDIGT` |
| `ERLEDIGT` | Zeilenzahl gleich Eintragszahl, mindestens eine nicht `OK` | `TEILWEISE_ERLEDIGT` |
| `ERLEDIGT` | fehlt, oder Zeilenzahl passt nicht zur Eintragszahl | `UNSTIMMIG` |
| `FEHLGESCHLAGEN` | beliebig | `FEHLGESCHLAGEN` |
| anderer Wert | beliebig | `UNBEKANNT`, der Rohwert steht in `status` |

`TEILWEISE_ERLEDIGT` gilt auch, wenn **kein** Eintrag Ok ist — Regel E‑1009 unterscheidet das
nicht; wie die Anzeige damit umgeht, ist offen (Punkt 1000).

### 5.6 Der Typkatalog

`TypKatalog` liefert die einem Mandanten angebotenen Typen: aktiv, für ihn freigeschaltet (beides
im Statement) und gültig (beim Laden geprüft). Er liest bei jedem Zugriff **ein** Statement — die
freigeschalteten aktiven Zeilen samt Struktur — und prüft nur Zeilen, deren `(task_typ,
geaendert_am)` er noch nicht kennt. Die Prüfung samt `WARN` läuft damit einmal je Fassung und nicht
bei jedem Aufruf; eine Änderung per SQL ändert `geaendert_am` (`ON UPDATE`) und wirkt beim nächsten
Zugriff. Gemessen in M1000 (§9).

> **Ergänzt 16.10.2026 (A2).** `TypKatalog`, `TypDefinition`, `DefinitionsPruefung`,
> `AuftragsPruefung`, `Prueffehler` und `Fehlercode` liegen seit A2 in `common.auftrag`, weil der
> Assistent sie ebenfalls braucht (E‑1037, [`assistent.md`](assistent.md) §4.1). Das Lesen der
> Typen heißt seitdem `AuftragstypRepository.angeboteneTypen` statt
> `AuftragRepository.angeboteneTypen`; Statement und Verhalten sind unverändert. Wo diese Datei
> die alten Orte nennt (§9.2, §9.3, §10), gilt das neue.

**Warum kein Zwischenspeicher mit Ablaufzeit:** Er hielte eine deaktivierte Definition bis zum
Ablauf im Angebot. Das eine Statement liest zwei Tabellen mit einer Handvoll Zeilen; was es kostet,
steht in M1000.

---

## 6. Endpunkte

Mandant ausschließlich aus der Sitzung. **Keiner der vier Endpunkte nimmt eine Mandanten-ID
entgegen**; es entsteht keine neue Ausnahme von M1 ([`mandantentrennung.md`](mandantentrennung.md)
§3). Ein untergeschobenes Feld `mandantId` im Anfragekörper wird ignoriert. Rollen `MANDANT` und
`ADMIN`, jeweils für den aktiven Mandanten; ohne aktiven Mandanten `403 kein-mandant-gewaehlt`. Der
Änderungszwang beim Passwort sperrt die Endpunkte wie alle übrigen. Pfade nach E‑1025.

Kein Endpunkt hat ein Zeitfenster und keiner blättert — das deckt E‑1016: `task` ist nicht aus
`GlassfishDB` abgeleitet, die Regeln 1 bis 6 gelten dort nicht.

### 6.1 `GET /api/tasks/typen`

Die dem aktiven Mandanten angebotenen Typen (E‑1020), nach Kennung sortiert. Kein angebotener Typ:
`200` mit leerer Liste. Grundlage für A2 und A3.

```json
[
  {
    "typ": "WERK_ABLADESTELLE",
    "name": "Anpassen der Werk-/Abladestellentabelle",
    "beschreibung": "Die EDI-Plattform erzeugt …",
    "aktionen": ["Neuanlage", "Anpassung", "Löschung"],
    "maxEintraege": 50,
    "schluessel": { "felder": ["lieferantennummer", "werk", "abladestelle"], "trenner": "/" },
    "felder": [
      { "name": "sndprn", "label": "SNDPRN", "pflicht": false,
        "nurBei": ["Neuanlage", "Anpassung"], "muster": "^[0-9]{6}$",
        "beispiel": "120764", "hinweis": "Bei Anpassung heißt leer: der Wert wird entfernt." }
    ]
  }
]
```

`schluessel` ist `null`, wenn der Typ keinen hat; `nurBei`, `muster`, `beispiel` und `hinweis` sind
`null`, wo die Definition sie nicht setzt. `regeln`, `zeile` und `rueckgaben` gehen nicht hinaus:
Den Parametertext baut das Backend (E‑1006), und die Regeln prüft es selbst — der Assistent erfährt
sie aus der Beschreibung und aus den Fehlercodes (§7).

### 6.2 `POST /api/tasks`

```json
{
  "anfrageId": "8f0c2a4e-1d3b-4c5a-9e7f-0a1b2c3d4e5f",
  "typ": "WERK_ABLADESTELLE",
  "aktion": "Neuanlage",
  "eintraege": [
    { "werk": "72332", "abladestelle": "DDB0", "sndprn": "120764" }
  ]
}
```

| Fall | Antwort |
|---|---|
| angelegt | **`201`** mit dem Auftrag (§6.5) und `Location: /api/tasks/{taskId}`. Ein Ereignis `AUFTRAG_ANGELEGT` im `audit_log` mit Typ, Aktion, Zahl der Einträge und Task-ID — nicht mit dem Parametertext |
| gleiche `anfrageId` im selben Mandanten noch einmal | **`200`** mit dem vorhandenen Auftrag. Kein zweiter Auftrag, kein zweites Ereignis — auch wenn der übrige Rumpf diesmal anders aussieht |
| `anfrageId` fehlt oder ist keine UUID | `400`, Fehlerliste mit `feld: "anfrageId"` und Code `PFLICHT_FEHLT` bzw. `FORMAT` |
| Typ unbekannt, inaktiv, ungültig **oder** für den aktiven Mandanten nicht freigeschaltet | **`404`**, in allen Fällen gleich: derselbe Rumpf, dasselbe Statement. Sonst ließe sich die Freischaltung anderer Mandanten abfragen |
| Prüffehler (§5.3) | `400` |

Die Reihenfolge ist: `anfrageId` prüfen, nach einem vorhandenen Auftrag mit dieser `anfrageId`
suchen, Typ auflösen, Einträge prüfen, anlegen. Kommen zwei gleiche Anfragen gleichzeitig, verliert
die zweite am eindeutigen Index `uq_task_anfrage` und antwortet mit `200` und dem Auftrag der
ersten.

**Das Fehlerformat** ist das vorhandene ([`datenzugriff.md`](datenzugriff.md) §8): Problemtyp
`eingabe-ungueltig`, Status `400`, wie bei der Feldvalidierung; die Liste `errors` trägt je Fehler
`index`, `feld`, `code` und `meldung`.

```json
{
  "type": "https://overlord.kraftwerkone.de/probleme/eingabe-ungueltig",
  "title": "Auftrag nicht angenommen",
  "status": 400,
  "detail": "Bitte prüfe die genannten Angaben.",
  "errors": [
    { "index": 0, "feld": "sndprn", "code": "FORMAT",
      "meldung": "Eintrag 1, SNDPRN: Der Wert „12076“ hat nicht die erwartete Form." }
  ],
  "traceId": "…"
}
```

**Die Fehlercodes:** `AKTION_UNBEKANNT`, `ZU_WENIGE_EINTRAEGE`, `ZU_VIELE_EINTRAEGE`,
`FELD_UNBEKANNT`, `FELD_NICHT_ERLAUBT`, `PFLICHT_FEHLT`, `FORMAT`, `UNZULAESSIGES_ZEICHEN`,
`MINDESTENS_EINS`, `DOPPELT`, dazu `ZU_LANG` (§5.2). Sie sind stabil; der Klartext darf sich
ändern.

### 6.3 `GET /api/tasks`

Alle Aufträge des aktiven Mandanten, die noch laufen (`OFFEN`, `IN_AUSFUEHRUNG`), **unabhängig vom
Alter**, dazu alle übrigen, deren `COALESCE(ausgefuehrt_am, eingestellt_am)` in den letzten 7 Tagen
liegt (Datenbankzeit, `UTC_TIMESTAMP(3)`). Neueste zuerst nach `eingestellt_am`, höchstens 50. Kein
Zeitfenster-Parameter, keine Paginierung (E‑1016). Welche Status als laufend gelten, steht nur im
`TaskStatusClassifier`; das Statement übernimmt die Menge von dort.

### 6.4 `GET /api/tasks/{taskId}`

Ein Auftrag. Fremde oder unbekannte Kennung: `404`, ununterscheidbar — **ein** Statement mit
Kennung und Mandant im `WHERE`, keine Existenzprüfung davor
([`mandantentrennung.md`](mandantentrennung.md) §5).

### 6.5 Die Antwort

Liste und Einzelabruf liefern dieselbe Form, `POST` ebenso:

| Feld | Inhalt |
|---|---|
| `taskId` | UUID |
| `typ` | Kennung des Typs |
| `name` | Name aus der Kopie |
| `aktion` | die Aktion |
| `status` | gespeicherter Status, roh |
| `einordnung` | `OFFEN`, `IN_AUSFUEHRUNG`, `ERLEDIGT`, `TEILWEISE_ERLEDIGT`, `UNSTIMMIG`, `FEHLGESCHLAGEN` oder `UNBEKANNT` (§5.5) |
| `eingestelltAm`, `abgeholtAm`, `ausgefuehrtAm` | ISO 8601 mit `Z`, `null` wo nicht gesetzt |
| `fehlercode`, `fehlertext` | von der Plattform, sonst `null` |
| `eintraege` | je Eintrag `werte` (Feldname → Wert, nur Felder mit Wert), `schluessel` (Text nach der Kopie, `null` ohne Schlüssel), `rueckgabe` (Rohwert der zugehörigen Zeile oder `null`) und `ergebnis` (`OK`, `NICHT_OK`, `UNBEKANNT` oder `null`) |

Rückgabe und Ergebnis eines Eintrags gehören zur Zeile mit demselben Index; gibt es diese Zeile
nicht, sind beide `null`.

---

## 7. Hinweise für A2

> **Erledigt mit A2 (16.10.2026).** Umgesetzt in [`assistent.md`](assistent.md): Werkzeuge aus der
> Struktur ohne Mandantenparameter (E‑1034), Rückfragen aus den Fehlercodes (E‑1033), Anlegen nur
> per Klick, Datenschutz als Punkt 1009 (E‑1029, E‑1030). Typdefinition, Typkatalog und Prüfung
> liegen seitdem in `common.auftrag` (E‑1037); neu ist `POST /api/tasks/pruefung` (E‑1042,
> [`assistent.md`](assistent.md) §6.5).

Damit sie nicht verloren gehen:

- **Die Werkzeuge des Modells entstehen aus der Struktur des Typs** (Aktionen, Felder, Beispiele),
  die Beschreibung geht in die Anweisungen des Modells. **Kein Werkzeug hat einen
  Mandantenparameter.**
- **Die Fehlercodes aus §6.2 sind die Grundlage der Rückfragen**; das Modell beurteilt Werte nicht
  selbst.
- **Bei `WERK_ABLADESTELLE` nennt die Karte jedes leere SNDPRN-Feld einer Anpassung ausdrücklich
  als „wird entfernt“** (E‑1007), weil der Kunde den bisherigen Wert nicht sehen kann. Das gehört
  als Hinweis in die Beschreibung des Typs, nicht in den Code — es steht dort bereits.
- **Den Auftrag legt der Klick auf der Karte über `POST /api/tasks` an, nie das Modell.** Die
  `anfrageId` entsteht je Absenden der Karte, nicht je Gespräch.
- Vor A2 ist Datenschutz und Auftragsverarbeitung mit dem Modellanbieter zu klären (Punkt 1008).

---

## 8. Folgeschritte

| Teil | Inhalt |
|---|---|
| **A2** | Assistent im Backend: Anbindung an das Sprachmodell, Werkzeuge aus der Typdefinition — erledigt, [`assistent.md`](assistent.md) |
| **A3** | Chatbox in der Kopfleiste und Bestätigungskarte |
| **A4** | Bereich „Ihre Aufträge“ in der Übersicht, über `GET /api/tasks` |

---

## 9. Messungen und Tests

### 9.1 Tests ohne Datenbank

| Test | Was er hält |
|---|---|
| `DefinitionsPruefungTest` | `WERK_ABLADESTELLE` und die Gattung sind gültig; je Prüfregel beim Laden (§3.6) mindestens ein ungültiger Fall — unbekannter Schlüssel auf jeder Ebene, fehlender Pflichtschlüssel, nicht übersetzbares Muster, Platzhalter ohne Feld, Aktion ohne Zeilenvorlage, optionaler Schlüsselteil nicht vorn, `maxEintraege` außerhalb der Grenzen, `rueckgaben.ok` fehlt oder steht in `weitere`; alle Gründe kommen zurück |
| `AuftragsPruefungTest` | jeder Fehlercode aus §5.3 mindestens einmal; 35 und 36 Zeichen, fünf und sieben Ziffern, leere Liste, 50 und 51 Einträge, Schlüssel doppelt, Löschung mit SNDPRN, Anpassung mit beiden Werten leer, Leerzeichen (`FORMAT`) und Tabulator (`UNZULAESSIGES_ZEICHEN`) im Schlüsselteil; der Zeichenschutz an einem Feld ohne Muster (Trenner, Komma bzw. Zeichen jeder Vorlage, LF, CR, Steuerzeichen, `U+0085`, `U+2028`); die Gattung |
| `ParametertextTest` | die drei Texte aus §5.2 zeichengenau, Zeilenende, die Gattung, Ersetzen in einem Durchgang |
| `RueckgabeTest` | CRLF, abschließendes LF, Leerraum, unbekannter Wert, leere Zeile, fehlende Rückgabe |
| `TaskStatusClassifierTest` | jede Zeile der Tabelle in §5.5, dazu falsche Zeilenzahl |
| `TypUnabhaengigkeitTest` | `WERK_ABLADESTELLE` steht im Hauptcode (`src/main`, auch generiert und Ressourcen) nirgends außer in `V100` — mit Gegenprobe, dass die Suche ihn dort findet (E‑1018) |
| `PlattformvertragTest` | die vier Statements in [`task-schnittstelle.md`](task-schnittstelle.md) §3 sind zeichengleich mit denen, die `AuftraegeDbIT` ausführt |
| `PaketstrukturTest` | das Paket `task` ist als Fachpaket geführt und kennt kein anderes Fachpaket |

Die **Gattung** ist eine zweite Definition, die nur im Test besteht (`Testdefinitionen`): andere
Aktionen, Felder, Trenner (`-`), Vorlagen mit `;` und `|`, eine Regel `eindeutig` über ein Feld
statt über den Schlüssel, andere Rückgabewerte (`JA`, `NEIN`, `SPAETER`). Prüfung und
Parametertext arbeiten für sie richtig, ohne dass im Hauptcode etwas für sie steht.

### 9.2 Tests mit Datenbank (`@Tag("db")`)

| Test | Was er hält |
|---|---|
| `AuftraegeDbIT` | Anlegen ergibt eine Zeile `OFFEN` mit `eingestellt_am`, Kopie der Definition, geordneten Einträgen, Parametertext, `angelegt_von` und genau einem Ereignis ohne Parametertext; Wiederholung mit derselben `anfrageId` (auch groß geschrieben, mit anderem Rumpf) gibt `200` und keinen zweiten Auftrag; Prüffehler im Fehlerformat; **simulierte Plattform** mit den vier Statements aus dem Vertrag: Erledigt, Teilweise erledigt (CRLF), Unstimmig, Fehlgeschlagen, doppeltes Abholen, Zurückmelden ohne Abholen; Zeitstempel nur auf vorhanden und Reihenfolge (T1) |
| `AuftraegeIsolationDbIT` | die vier Endpunkte: `NEXANS` gegen `IBIS`, `SUTTONS` ohne Freischaltung (`404` beim Anlegen, ununterscheidbar vom erfundenen Typ in Rumpf **und** Statements; leere Typliste), fremder Auftrag weder in der Liste noch einzeln (`404` gleich einer erfundenen Kennung, ein Statement), untergeschobenes `mandantId`, fremde `anfrageId`, ADMIN je aktivem Mandanten, Änderungszwang |
| `TypdefinitionDbIT` | ein eigener Testtyp per SQL, ohne Neustart: inaktiv `404`, aktiviert in der Typliste, Änderung an `maxEintraege` wirkt beim nächsten Anlegen, ungültige Struktur nimmt ihn aus dem Angebot, ein Auftrag von vorher liest seine Kopie; `WERK_ABLADESTELLE` bleibt unberührt |
| `TaskMigrationDbIT` | `JSON_VALID(struktur)` ist wahr; die Muster, in der Datenbank per `JSON_EXTRACT` herausgeholt, sind zeichengleich mit dem Auftrag; Struktur, Name und Beschreibung wie geschrieben |
| `IndexbestandDbIT` | die drei Tabellen und ihre Indizes in der Sollliste, in beide Richtungen |

**Verletzungsprobe je Mandantenfilter (16.10.2026).** Jeder Filter in `AuftragRepository` ist
einzeln entfernt worden, danach lief `AuftraegeIsolationDbIT`; die Datei ist jedes Mal byte-gleich
zurückgespielt.

| Entfernt | Rot |
|---|---|
| `mandant_id` in `angeboteneTypen` | `typliste`, `anlegen_ohne_freischaltung`, `admin_sieht_nur_den_aktiven_mandanten` |
| `mandant_id` in `finde` | `fremder_auftrag_unerreichbar`, `admin_sieht_nur_den_aktiven_mandanten` |
| `mandant_id` in `findeNachAnfrage` | `fremde_anfrage_id` |
| `mandant_id` in `findeAktuelle` | `fremder_auftrag_unerreichbar`, `mandant_id_im_rumpf_wird_ignoriert`, `admin_sieht_nur_den_aktiven_mandanten` |
| `legeAn` schreibt fest `IBIS` statt des Session-Mandanten | `mandant_id_im_rumpf_wird_ignoriert`, `fremde_anfrage_id`, `fremder_auftrag_unerreichbar`, `admin_sieht_nur_den_aktiven_mandanten` |

Jeder Filter hat also mindestens einen Test, der sein Fehlen bemerkt. `aenderungszwang_sperrt`
bleibt bei allen Eingriffen grün: Er prüft keine Mandantentrennung, sondern dass der
Änderungszwang beim Passwort auch diese Endpunkte sperrt. Eine erste Probe, die nur die Filter in
`angeboteneTypen` und `finde` gemeinsam entfernte, ließ deshalb drei Tests grün: Die beiden anderen
prüfen Filter, die sie nicht angefasst hatte.

Aufgeräumt wird, was das Präfix `it-` trägt: Aufträge über `angelegt_von`, Testtypen über
`task_typ` (Regel T2). Die Zeile `WERK_ABLADESTELLE` fasst kein Test an.

### 9.3 Messung M1000 (Regel 7)

*Gemessen am 16.10.2026 gegen die Testkopie (MariaDB 10.6.22), eine Sitzung mit dem Workbench-Client,
nach Regel 7 mit `EXPLAIN` und Laufzeit (E‑1026): je Statement fünf Läufe, Dauer aus `SHOW PROFILES`
und Wanduhr über `SYSDATE(6)` vor und nach den fünf Läufen. Kein Lastgerüst.* Die Statements sind
die von jOOQ gerenderten (über eine Wegwerfklasse gegen eine `MockConnection` mit eingesetzten
Werten erhoben), die der Plattform wörtlich aus [`task-schnittstelle.md`](task-schnittstelle.md).

**Bestand bei der Messung:** `task_type` 1 Zeile, `task_type_mandant` 2, `task` 0 (die Tests räumen
ihre Aufträge weg). Die Pläne zeigen deshalb, **welchen Index der Optimizer nimmt**, nicht, wie er
unter Last läuft; Laufzeiten unter einer Millisekunde sind hier die Kosten des Rundlaufs.

| # | Statement | `EXPLAIN` | Profil (min–max) | Wanduhr je Lauf |
|---|---|---|---|---|
| L1 | Typen samt Freischaltung, `NEXANS` (`AuftragRepository.angeboteneTypen`) | `task_type`: `type=index`, `key=PRIMARY`, `rows=1`, `Using where` · `task_type_mandant`: `type=eq_ref`, `key=PRIMARY`, `ref=task_type.task_typ,const`, `Using index` | 0,552–0,668 ms | 1,233 ms |
| L1b | dasselbe, `SUTTONS` (keine Freischaltung) | wie L1 | 0,495–0,522 ms | 0,937 ms |
| L2 | Liste, `NEXANS` (`findeAktuelle`, `ORDER BY eingestellt_am DESC, task_id DESC`, 50) | `type=ref`, `key=task_mandant_idx`, `ref=const`, `rows=1`, `Using where` — kein `filesort` | 0,545–0,598 ms | 0,977 ms |
| L3 | Einzelabruf (`finde`, Kennung und Mandant) | `Impossible WHERE noticed after reading const tables` — Zugriff über `PRIMARY` als Konstante, die Kennung gibt es nicht | 0,406–0,498 ms | 0,802 ms |
| L4 | Wiederholung (`findeNachAnfrage`, Mandant und `anfrage_id`) | wie L3, über `uq_task_anfrage` | 0,419–0,445 ms | 0,787 ms |
| P1 | Plattform: offene lesen | `type=ref`, `key=task_status_idx`, `ref=const`, `rows=1`, `Using index condition; Using where` — kein `filesort` | 0,439–0,481 ms | 0,878 ms |
| P2 | Plattform: abholen (bedingtes `UPDATE`) | `type=range`, `key=PRIMARY`, `rows=1`, `Using where` | — (nur `EXPLAIN`) | — |
| P3 | Plattform: zurückmelden (`ERLEDIGT`; `FEHLGESCHLAGEN` hat dieselbe Bedingung) | wie P2 | — (nur `EXPLAIN`) | — |

**Was das trägt.** L2 und P1 lesen über den Index, der für sie angelegt ist, und sortieren ohne
`filesort`: `task_mandant_idx (mandant_id, eingestellt_am)` liefert die Liste rückwärts in
Reihenfolge, `task_status_idx (status, eingestellt_am)` samt dem angehängten Primärschlüssel die
offenen Aufträge in der Reihenfolge des Vertrags. Die beiden bedingten `UPDATE`s treffen genau eine
Zeile über `PRIMARY`; das `status` in der Bedingung wird an der Zeile geprüft. L1 beginnt bei
`task_type` und schlägt die Freischaltung über ihren vollen Primärschlüssel nach — bei einer
Handvoll Typen ist das der billigste Weg; mit Hunderten von Typen läse L1 jeden einmal.

**Was es nicht trägt.** Wie sich L2 verhält, wenn ein Mandant Tausende Aufträge hat, ist auf der
leeren Tabelle nicht zu sehen; `task` wird nie gelöscht. P2 und P3 sind nicht ausgeführt, weil die
Plattform sie ausführt und die Testkopie geteilt ist; `AuftraegeDbIT` führt sie aus, misst aber
nicht (T1).

### 9.4 Gegenprobe gegen `main` (16.10.2026)

Nachdem `V100` auf der Testkopie angewandt war: `main` (`1194079`) in einem eigenen Worktree im
Profil `dev` gebaut, mit Codegen und mit allen Tests unter `@Tag("db")` (`./mvnw verify`).
Flyway validiert 14 Migrationen und meldet „Schema is up to date“ — `V100` liegt für `main` in der
Zukunft und wird ignoriert. Der Codegen schließt `task`, `task_type` und `task_type_mandant` aus;
`src/main/generated-java` bleibt unverändert, der Arbeitsbaum danach sauber. **1.040 Tests ohne und
456 mit Datenbank, alle grün.** Auf `main` wurde nichts geändert; der Worktree ist entfernt.

---

## 10. Regelbezug

| Regel | Umsetzung |
|---|---|
| **M1** Kein Endpunkt nimmt eine Mandanten-ID entgegen | keiner der vier; keine neue Ausnahme |
| **M2** Mandant als erster Pflichtparameter | jede Methode von `AuftragRepository`, auch das Laden der Typen |
| **M3** Filter im Statement | Mandant im `WHERE` jedes Statements; die Freischaltung als Join im Statement des Typkatalogs |
| **M4** Isolationstest je Endpunkt | `AuftraegeIsolationDbIT` |
| **Regel 3** (kein Schreibzugriff auf `GlassfishDB`) | das Paket `task` liest und schreibt ausschließlich `overlord_monitor` |
| **Regeln 1 bis 6** | gelten hier nicht (E‑1016) |
| **Regel 7** | M1000 (E‑1026) |
| **Z1** | keine Uhr in Java; alle Zeitstempel aus der Datenbank (E‑1011) |

---

## 11. Offene Punkte

Nummern ab 1000 (Linie `assistent`); dieselben Zeilen stehen im Register
[`offene-punkte.md`](offene-punkte.md).

| Nr. | Punkt | Art |
|---|---|---|
| **1000** | Regel E‑1009 nennt einen Auftrag „Teilweise erledigt“, auch wenn **kein** Eintrag Ok ist, also nichts geändert wurde. Die Anzeige dafür ist offen. | Entscheidung offen |
| **1001** | Aufträge, die in `IN_AUSFUEHRUNG` hängen bleiben (die Plattform bricht ab), haben keine Frist und keinen Rückweg. | Entscheidung offen |
| **1002** | Die Schranke von 35 Zeichen je Schlüsselteil (E‑1012) ist zu bestätigen. | Entscheidung offen |
| **1003** | Ob die Plattform UTF-8 (ö in „Löschung“) und LF als Zeilenende sicher verarbeitet. | Sichtprüfung oder Messung offen |
| **1004** | Der Monitor kann die Werk-/Abladestellentabelle der Plattform nicht lesen. Ob ein Schlüssel existiert, zeigt sich erst bei der Ausführung. | Verbesserung |
| **1005** | Ungültige Definitionen sind nur im Log sichtbar — und erst, wenn ein freigeschalteter Mandant nach der Aktivierung zugreift. | Verbesserung |
| **1006** | Die Kopfzeile `Aktion=<Aktion>` ist fest; ein Typ mit anderer Kopfzeile braucht Code. | Verbesserung |
| **1007** | Nur der erste Schlüsselteil kann optional sein. | Verbesserung |
| **1008** | Datenschutz und Auftragsverarbeitung mit dem Modellanbieter, vor A2. **Erledigt am 16.10.2026:** beantwortet mit E‑1029 und E‑1030 (gebaut, aber abgeschaltet), weitergeführt als Punkt 1009 ([`assistent.md`](assistent.md) §11). | Entscheidung offen |
