# docs/

Hier steht, **was** gebaut wird und **warum**. Wie gebaut wird, steht in
[`../DEVELOPMENT_GUIDELINES.md`](../DEVELOPMENT_GUIDELINES.md).

## Wie dieses Verzeichnis funktioniert

**Jedes Feature bekommt eine eigene Datei.** Keine Sammeldatei, keine „sonstiges.md".

| Anlass | Was zu tun ist |
|---|---|
| Neues Feature | **Neue Datei** anlegen und unten im Verzeichnis eintragen |
| Geändertes Feature | Die **vorhandene Datei aktualisieren** — nicht eine zweite anlegen |
| Entferntes Feature | Datei löschen, Eintrag entfernen |

**Ein Schritt gilt erst als fertig, wenn die Dokumentation steht.** Code ohne zugehörige
Dokumentation ist unfertig, auch wenn er läuft. Die Datei entsteht **im selben Commit** wie das
Feature, nicht später.

## Was in eine Feature-Datei gehört

Kurz, konkret, aus der Sicht von jemandem, der das Feature später ändern muss:

1. **Zweck** — welche fachliche Frage beantwortet das Feature? Ein bis zwei Sätze.
2. **Endpunkte** — Pfad, Parameter, Antwortform, Fehlerfälle.
3. **Datenquelle** — welche Tabellen, welche Joins, welcher Index. Bei jeder Abfrage: das
   `EXPLAIN`-Ergebnis und die gemessene Laufzeit gegen die Testkopie (Regel L7).
4. **Entscheidungen** — was wurde bewusst *nicht* gemacht und warum. Das ist der Teil, der
   sich sonst nicht rekonstruieren lässt.
5. **Regelbezug** — welche der unverhandelbaren Regeln aus Abschnitt 4 der Richtlinien betrifft
   das Feature, und wie ist sie umgesetzt.
6. **Offene Punkte** — was bekannt fehlt. Lieber notiert als vergessen.

**Nicht** hineingehören: Zugangsdaten, Hostnamen, Produktionsdaten, echte Belegnummern,
`MessagePropertyValue`-Inhalte. Beispiele werden anonymisiert.

## Verzeichnis

### Grundlagen — immer gültig

| Datei | Inhalt |
|---|---|
| [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) | **Die verbindliche Wahrheit.** Zweck, Rollen, Quellsystem, fachliche Definitionen, Architektur, Sicherheit, Leistungsregeln, MVP-Umfang, Annahmen |
| [`IMPLEMENTIERUNGSPLAN_MVP.md`](IMPLEMENTIERUNGSPLAN_MVP.md) | Die zehn Schritte bis zum MVP, mit Abgrenzung und Abnahmekriterium je Schritt |
| [`datenmodell.md`](datenmodell.md) | **Nachschlagewerk für jede Abfrage.** Kerntabellen, Mandantenkette, Indizes, dokumentierte Fallstricke |

### Features

Angelegt:

| Datei | Aus Schritt | Inhalt |
|---|---|---|
| [`datenzugriff.md`](datenzugriff.md) | Schritt 2 | Zwei DataSources, zwei DSLContexts, ein Transaktionsmanager, dreischichtiger Schreibschutz, Flyway, Zeitquellen, Fehlerformat, jOOQ-Codegenerierung, Tests |
| [`message-status.md`](message-status.md) | Schritt 2 | Statuskatalog, `MessageStatusClassifier`, die eine Fehlerbedingung (vorgezogen aus Schritt 3) |
| [`annahmen-korrekturen.md`](annahmen-korrekturen.md) | Schritt 2 | Was die Erhebungen gegenüber der Projektbeschreibung verändert/bestätigt haben |
| [`authentifizierung.md`](authentifizierung.md) | Schritt 3, Teil 1 | Anmeldung, Sperre und Auskunftsdisziplin, Sitzung und Cookie je Profil, Passwortwechsel, Bootstrap, Nutzer anlegen — und warum die Altnutzer bewusst **nicht** übernommen werden |
| [`mandantentrennung.md`](mandantentrennung.md) | Schritt 3, Teil 1 | `MandantContext`, Berechtigung als Menge statt Rolle, die genau zwei Endpunkt-Ausnahmen, die ArchUnit-Regel, die Vorlage für den Isolationstest |
| [`visuelles-konzept.md`](visuelles-konzept.md) | Schritt 3, Teil 2 | Farbrollen samt fachlicher Bindung der Statusfarben, die vier Stufen des Akzents `#b9c022` mit gemessenen Kontrastwerten, „Status nie allein über Farbe", Typografie, Dichte nach Zeigergerät, volle Fensterbreite und die eine Ausnahme davon, Verhalten am Handy — und wie man das Konzept in einer Datei ändert |
| [`frontend-grundlagen.md`](frontend-grundlagen.md) | Schritt 3, Teil 2 | Rewrite statt CORS, warum die Routensperre kein Schutz ist, Ablauf nach dem Anmelden, Aufbau der Sprachdateien, Regeln für den Zwischenspeicher, Zuordnung von `type` zu Übersetzung, Scroll-Architektur des Rahmens |
| [`nachrichtenliste.md`](nachrichtenliste.md) | Schritt 4 | Listen-Endpunkt mit Pflicht-Zeitfenster, Cursor-Paginierung in der gemessenen ODER-Form, Filter über `MessageStatusKind`, Freitext gegen die Stammdaten samt der gemessenen Fenstergrenze von 30 Tagen (aufhebbar bis 90), die Mandantenkette als `EXISTS` statt über die View — und warum |
| [`prozessauswahl.md`](prozessauswahl.md) | Schritt 4 | `GET /api/prozesse` — die Liste, aus der der Prozessfilter wählt. Stammdaten ohne Zeitfenster und ohne Cursor, die Mandantenkette hier als Join statt als `EXISTS`, und warum die Gegenprobe des Isolationstests bei einem Endpunkt ohne Parameter anders aussieht |

Die folgenden Dateien entstehen laut Plan:

| Datei | Entsteht in | Inhalt |
|---|---|---|
| `nachrichtendetail.md` | Schritt 5 | Detailansicht, Prozessschritte, Eigenschaften |
| `prozessschritte-uebersetzung.md` | Schritt 5 | Zuordnungstabelle `SOSActionServiceProperties` → Klartext |
| `verkettung.md` | Schritt 6 | Auflösung über Split, Merge und Quittung |
| `bam-suche.md` | Schritt 7 | Suche über `MessageBAM`, Limit und Mindestlänge |
| `rohdaten-download.md` | Schritt 8 | Filestore-Auflösung, Proxy, Protokollierung |
| `prozess-katalog.md` | Schritt 9 | `process_catalog`, Heuristik-Import, Massenzuordnung |
| `benutzerverwaltung.md` | Schritt 9 | Anlegen, Sperren, Rolle, Passwort zurücksetzen |
| `rollup.md` | Schritt 10 | `message_rollup`, stündlicher Job, Rückwärtslauf |
| `dashboard.md` | Schritt 10 | Kennzahlen, die drei Problemkategorien |
| `process-view.md` | Schritt 10 | Prozessansicht, gruppiert nach kuratiertem Partner |

Diese Tabelle ist eine Vorschau, keine Zusage über Dateinamen. Wer einen Schritt umsetzt, trägt
die tatsächlich entstandenen Dateien hier ein.

### Erhebungen

Einmalig erhobene Fakten über das Quellsystem, die sonst niemand mehr nachvollziehen kann:

| Datei | Entsteht in | Inhalt |
|---|---|---|
| [`message-status.md`](message-status.md) | **Schritt 2** (vorgezogen) | Ergebnis von `SELECT DISTINCT MessageStatus` — widerlegt Annahme A6. Ersetzt das ursprünglich für Schritt 3 geplante `message-status-werte.md` |
| [`messungen-schritt4.md`](messungen-schritt4.md) | **vor Schritt 4** | M0 bis M7 gegen die Testkopie (01.08.2026): Datenstand, Spalten und Indizes, `MessageTimeout`, Nachrichten je Mandant, View gegen `EXISTS`, `SOSID`, `SPLITTED`/`MERGED`, BAM-Konfiguration — je mit Statement, Ergebnis, `EXPLAIN` und Laufzeit. Dazu die Auffälligkeiten und die daraus **offenen Fragen**. Reine Erhebung, keine Entscheidungen. Nachgetragen: M8/M9 (die beiden Fragen, die sie entscheiden), L1 bis L13 zu den Statements des Listen-Endpunkts — darunter M10 und L11, die beide vorgesehenen Umbauten des Freitextfilters widerlegen, und **L13**, aus dem seine Fenstergrenze folgt. Zur Nachbesserung (06.08.2026): **M11** BAM-Abdeckung je Typ (Vorarbeit für Schritt 7), **M12** Zwischenschritte je Mandant, **M13** `SOSAction` — die Messung, die entscheidet, ob der aktuelle Schritt anzeigbar ist |
