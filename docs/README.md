# docs/

Hier steht, **was** gebaut ist und **warum**. Wie gebaut wird, steht in
[`../DEVELOPMENT_GUIDELINES.md`](../DEVELOPMENT_GUIDELINES.md).

**Ab 1.0.0 beschreibt diese Datei den aktuellen Stand**, ebenso wie
[`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md): Eine Änderung ersetzt den Text; Grund und
Entscheidung stehen im Commit und in [`../CHANGELOG.md`](../CHANGELOG.md) (Entscheidung 9 vom
16.10.2026). Feature- und Messdateien behalten ihre datierten Korrekturblöcke. Die Fassungen beider
Dateien bis 1.0.0 liegen unverändert im [Archiv](#archiv).

## Wie dieses Verzeichnis funktioniert

**Jedes Feature bekommt eine eigene Datei.** Keine Sammeldatei, keine „sonstiges.md“.

| Anlass | Was zu tun ist |
|---|---|
| Neues Feature | **neue Datei** anlegen und unten im Verzeichnis eintragen |
| Geändertes Feature | die **vorhandene Datei aktualisieren**, nicht eine zweite anlegen |
| Entferntes Feature | Datei löschen, Eintrag entfernen |

**Ein Schritt gilt erst als fertig, wenn die Dokumentation steht.** Code ohne zugehörige
Dokumentation ist unfertig, auch wenn er läuft. Die Datei entsteht **im selben Commit** wie das
Feature. Neue offene Punkte kommen im selben Commit ins Register
[`offene-punkte.md`](offene-punkte.md).

### Was in eine Feature-Datei gehört

Kurz, konkret, aus der Sicht von jemandem, der das Feature später ändern muss:

1. **Zweck** — welche fachliche Frage beantwortet das Feature? Ein bis zwei Sätze.
2. **Endpunkte** — Pfad, Parameter, Antwortform, Fehlerfälle.
3. **Datenquelle** — welche Tabellen, welche Joins, welcher Index; bei jeder Abfrage das
   `EXPLAIN`-Ergebnis und die gemessene Laufzeit gegen die Testkopie (Regel L7).
4. **Entscheidungen** — was bewusst *nicht* gemacht wurde und warum.
5. **Regelbezug** — welche Regeln aus Abschnitt 4 der Richtlinien das Feature berührt und wie sie
   umgesetzt sind.
6. **Offene Punkte** — was bekannt fehlt, mit Nummer, und dieselbe Zeile im Register.

### Was nicht hineingehört — und was ausdrücklich doch

Geschützt sind **Nutzdaten und Namen Dritter**:

- echte Belegnummern und `MessagePropertyValue`-Inhalte
- **Partner**namen
- Zugangsdaten, Passwörter, Passwort-Hashes, Zertifikate
- Hostnamen und Adressfragmente interner Server, auch wenn sie in einem Dienstnamen stecken

**Nicht** geschützt ist das **Konfigurationsvokabular des Altsystems**: Bausteinmarken
(`NXS_FILE_CONVERT`, `FTPSender`), Prozess-, Projekt- und Ablaufnamen, Statuswerte,
Mandantenkürzel. Die zehn Mandanten stehen mit Firmennamen in
[`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §3.2. **Der Trennstrich läuft zwischen Nutzdaten
und Konfiguration, nicht zwischen „intern“ und „extern“.** Enthält eine Marke oder ein Ablaufname
den Namen eines **Partners**, wird dieser Bestandteil maskiert (`<Kunde>`).

### Nummern

Wie heute vergeben wird. Vor jeder Vergabe wird die höchste vergebene Nummer per `grep` über `docs/`
ermittelt; die jüngsten Dateien führen dazu eine Tabelle „Nummernvergabe“.

| Nummer | Kreis | Seit |
|---|---|---|
| **E‑&lt;n&gt;** — Entscheidung | **projektweit fortlaufend**, geschrieben mit geschütztem Bindestrich (`E‑243`) | 24.08.2026 mit den Buchstaben `E‑a` bis `E‑z`, numerisch ab `E‑27` am 01.09.2026 ([`testfestigkeit.md`](testfestigkeit.md) §10.6) |
| **M&lt;n&gt;** — Messung | **projektweit fortlaufend** | 10.08.2026 ([`messungen-schritt6.md`](messungen-schritt6.md)) |
| **Offener Punkt** | **projektweit fortlaufend**, Verzeichnis und nächste freie Nummer im Register [`offene-punkte.md`](offene-punkte.md) | ab Nummer 33 ([`messungen-schritt9.md`](messungen-schritt9.md)) |

- **Ältere, je Datei vergebene Kennungen bleiben gültig, mit ihrer Fundstelle:** Entscheidungen
  `E1`, `E14` … ohne Bindestrich zählen je Feature-Datei; dasselbe `E14` in zwei Dateien ist kein
  Fehler. Sie werden **nie ohne ihre Datei** genannt („`prozess-katalog.md` E14“). Ebenso zählen
  offene Punkte unter 33 und unnummerierte Listen je Datei; sie stehen im Register als lokale Punkte
  und bekommen eine projektweite Nummer erst, wenn jemand sie wieder aufgreift.
- **`E<n>` in Messdateien** (`E1` bis `E7`) heißt dort *ergänzende Messung*; die Bezeichnung ist
  seit dem 13.08.2026 nicht mehr in Gebrauch.
- **Auf dem Zweig `assistent` beginnen alle projektweit gezählten Nummern bei 1000**
  ([`../DEVELOPMENT_GUIDELINES.md`](../DEVELOPMENT_GUIDELINES.md) §12).

### Sichtprüfungen

Die offenen Sichtprüfungen stehen in **einer** Tabelle, im Register Teil 2, als Punkte 234 bis 246:
**dreizehn**, alle offen. Zehn betreffen das schmale Fenster, eine das Eingabegerät (klebende
Spalte), eine Zustände, die die Testkopie nicht zeigt (Eigenschaften an der Zeitleiste), und eine den
Rückweg aus der Datei im Browser. Die Beschreibung steht jeweils in der Feature-Datei.

Auf der Linie `assistent` stehen neue Sichtprüfungen als gewöhnliche Punkte in Teil 1 des Registers:
**1018 bis 1022** für Chatbox und Karte, mit eingeschaltetem Assistenten
([`assistent-frontend.md`](assistent-frontend.md) §11), und **1028 bis 1032** für den Block „Ihre
Aufträge“ ([`assistent-frontend.md`](assistent-frontend.md) §12.12).

---

## Verzeichnis

### Grundlagen

| Datei | Inhalt |
|---|---|
| [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) | **Die verbindliche Wahrheit.** Zweck, Rollen, Quellsystem, eigenes Schema, fachliche Definitionen, Architektur, Sicherheit, Leistungsregeln mit ihren benannten Ausnahmen, Funktionsumfang 1.0, Annahmen und Glossar. |
| [`ROADMAP.md`](ROADMAP.md) | Planung ab 1.0.0: die Linie 1.x mit Fixes aus dem Register, die Linie `assistent` mit A1 bis A4 und weitere Ausbaustufen. Dazu die Vorlage für einen Arbeitsauftrag. |
| [`offene-punkte.md`](offene-punkte.md) | Register aller offenen Punkte: je Punkt eine Zeile mit Kurztext, Fundstelle, Art und Stand. Die Beschreibung bleibt in der Feature-Datei; hier steht die nächste freie Nummer. |
| [`betrieb.md`](betrieb.md) | Was das Repository über den Betrieb belegt: Bausteine, Profile, Umgebungsvariablen, Flyway, Rollup-Job, Ablagenprüfung, Release und Rollback. Dazu die offenen Fragen zum Produktionsbetrieb. |
| [`IMPLEMENTIERUNGSPLAN_MVP.md`](IMPLEMENTIERUNGSPLAN_MVP.md) | Die Schritte 1 bis 11 bis Version 1.0.0, je mit Ziel, Abgrenzung und Abnahmekriterium. Abgeschlossen am 16.10.2026, wird nicht mehr fortgeschrieben. |
| [`datenmodell.md`](datenmodell.md) | Nachschlagewerk für jede Abfrage gegen `GlassfishDB`: Hierarchie, Mandantenkette, Kerntabellen und Indizes, Fallstricke, Datenbank-Events, Verkettung und Größenordnung. |
| [`datenzugriff.md`](datenzugriff.md) | Wie das Backend liest und schreibt: zwei DataSources und DSLContexts, ein Transaktionsmanager, dreischichtiger Schreibschutz, Flyway. Dazu Zeitquellen, Fehlerformat, jOOQ-Codegenerierung und die zwei Teststufen. |
| [`mandantentrennung.md`](mandantentrennung.md) | `MandantContext`, Berechtigung als Mandantenmenge, die benannten Ausnahmen von M1 und M2 und ihre Prüfung per ArchUnit. Enthält die Vorlage für den Mandanten-Isolationstest. |
| [`message-status.md`](message-status.md) | Der Statuskatalog von `Message.MessageStatus`, seine Einordnung über `MessageStatusClassifier` und die eine Fehlerbedingung. Dazu Endstatus, Fehler ohne `ERROR_`-Präfix und die Sicherung gegen neue Werte. |
| [`testfestigkeit.md`](testfestigkeit.md) | Woran die Aussage eines Tests hängen darf und woran nicht: keine Wanduhrzeit, keine veränderlichen Daten der Testkopie. Dazu die Mutationsprüfung der Frontend-Tests, die Quelltext lesen. |
| [`visuelles-konzept.md`](visuelles-konzept.md) | Warum die Oberfläche so aussieht: Leitsatz, Farbrollen mit fachlicher Bindung und gemessenen Kontrasten, Schrift, Dichte, Breite, kleiner Bildschirm. |
| [`frontend-grundlagen.md`](frontend-grundlagen.md) | Der Unterbau der Oberfläche: Weiterleitung statt CORS, Routensperre, Ablauf nach dem Anmelden, Sprachdateien, Zwischenspeicher, Fehlertypen. Dazu der feste Rahmen und Farbe in Diagrammen. |
| [`START-LOKAL.md`](START-LOKAL.md) | Was lokal anders ist als in der Produktion, damit niemand einem Scheinfehler nachjagt: Rohdaten nur zum Teil abrufbar, langsame Schreibvorgänge der Testkopie, leere Monate, abgeschaltete Ablagenprüfung. |
| [`annahmen-korrekturen.md`](annahmen-korrekturen.md) | Datiertes Protokoll, was Erhebungen gegenüber der Projektbeschreibung bestätigt oder verändert haben und wo das Projekt geirrt hat. |

### Features

| Datei | Inhalt |
|---|---|
| [`authentifizierung.md`](authentifizierung.md) | Anmeldung, Sperre und Auskunftsdisziplin, Sitzung und Cookie je Profil, Passwortwechsel und Bootstrap des ersten Kontos. |
| [`nachrichtenliste.md`](nachrichtenliste.md) | `GET /api/nachrichten` und die Liste: Pflicht-Zeitfenster, Cursor, Filter, Freitextsuche, Fensterverengung über den Rollup. Dazu die Oberfläche mit Spalten, Filtern in der URL und Blättern. |
| [`prozessauswahl.md`](prozessauswahl.md) | `GET /api/prozesse`: die Prozesse des Mandanten als Auswahl für den Prozessfilter. |
| [`nachrichtendetail.md`](nachrichtendetail.md) | Die Detail-Endpunkte mit Schrittfolge, Eigenschaften und offenem Zustand einer Nachricht. Dazu Panel und eigene Route mit Zeitleiste, Ketten- und BAM-Block und dem Rückweg aus Datei, Suche und Prozessansicht. |
| [`verkettung.md`](verkettung.md) | Die Ketten-Endpunkte: Aufteilung und Zusammenführung als zwei Beziehungen, nach oben vollständig, nach unten mit Cursor. Dazu der Kettenblock im Detail. |
| [`bam-werte.md`](bam-werte.md) | Die Belegdaten am Nachrichtendetail: `GET /api/nachrichten/{id}/bam` mit wahrer Zahl je Typ und höchstens 20 Werten je Gruppe. |
| [`bam-sollaengen.md`](bam-sollaengen.md) | Die kuratierte Tabelle `bam_sollaenge`: auf welche Länge die Belegsuche je Mandant und Typ auffüllt und wann sie mit führendem Leerzeichen sucht. |
| [`bam-suche.md`](bam-suche.md) | Die Belegsuche `GET /api/bam/suche`: exakt mit sichtbarer Normalisierung, 30-Tage-Fenster, mehrere Begriffe, Präfixsuche als Rückfall. Dazu Suchfeld, Marken, Trefferliste und Leerzustand. |
| [`property-suche.md`](property-suche.md) | Suche über technische Werte per `feld=<name>:<wert>`, die benannte Ausnahme von Regel 4, dazu `GET /api/bam/suchfelder`. Dazu die Oberfläche und der Absprung in den Prozessbaum. |
| [`rohdaten.md`](rohdaten.md) | Die Vorgabe zum Rohdatenzugriff: alle Dateien und Protokolle ansehen und herunterladen, Beschnitt bei Protokollen. Hält die Entscheidungen fest, aus denen Backend und Oberfläche folgen. |
| [`rohdaten-backend.md`](rohdaten-backend.md) | Die Endpunkte unter `…/{messageId}/dateien` und die serverseitig hergeleitete Artefaktkennung. Dazu der Abrufweg zur Ablage, Kodierung je Datei, Zustände und Protokollierung. |
| [`rohdaten-frontend.md`](rohdaten-frontend.md) | Die Dateiziele an der Zeitleiste und die Route `/nachrichten/{id}/dateien/{artefaktId}` mit Anzeige, Zuständen und Download. |
| [`dateiansicht-darstellung.md`](dateiansicht-darstellung.md) | Die Auswahl „Darstellung“ in der Dateiansicht (EDIFACT, ANSI X12, VDA, IDoc, XML, JSON, Hex). Formatiert wird nur die Struktur, nie der Inhalt. |
| [`prozess-katalog.md`](prozess-katalog.md) | Die Vorgabe zum Prozess-Katalog: jeder Prozess bekommt Partner und Richtung, Unzugeordnetes heißt „nicht zugeordnet“. Entscheidungen zu Heuristik, Pflege und Massenzuordnung. |
| [`prozess-katalog-backend.md`](prozess-katalog-backend.md) | Tabelle `process_catalog`, die Partnerheuristik und die Endpunkte unter `/api/katalog`, einschließlich Lauf und Vorschlagsübernahme. |
| [`prozess-katalog-frontend.md`](prozess-katalog-frontend.md) | Der Administrationsbereich und die Katalogpflege: Pflegeliste mit Filtern, Bearbeitung in der Zeile, Lauf, Massenzuordnung und Vorschlagsübernahme. |
| [`benutzerverwaltung.md`](benutzerverwaltung.md) | Die Vorgabe zur Benutzerverwaltung: Konten, Mandantenmenge, Sperre und Deaktivierung mit Sitzungsentzug, kein Löschen. |
| [`benutzerverwaltung-backend.md`](benutzerverwaltung-backend.md) | Die Migrationen `V7` und `V13`, die Endpunkte unter `/api/admin/users` und der Sitzungsentzug. Dazu Selbstschutz des Admins und Rollengrenze. |
| [`benutzerverwaltung-frontend.md`](benutzerverwaltung-frontend.md) | Die Oberfläche unter `/administration/benutzer`: Liste, Formular unter der Zeile, Mandantenmenge, Anlegen, Baumgliederung und Rollenauswahl. |
| [`rollup.md`](rollup.md) | Die Rollup-Tabellen je Stunde, Tag und Monat samt `rollup_lauf` und der Job mit Delta- und Volllauf. Quelle aller Kennzahlen und der Fensterverengung. |
| [`dashboard.md`](dashboard.md) | Der Endpunkt der Übersicht, `GET /api/dashboard`: Verlauf, Kacheln, Verteilung, „Zuletzt aufgefallen“, Stand und Plattform-Block. Mit Statements, Ausnahmen von Regel 2 und Tests. |
| [`dashboard-frontend.md`](dashboard-frontend.md) | Die Oberfläche der Übersicht auf `/`: Zeitraum, Verlauf, Kacheln, Verteilung, „Zuletzt aufgefallen“, Stand und Plattform-Block. |
| [`dienste.md`](dienste.md) | Der Plattform-Block: eine Lampe je Dienst mit Zeitgrenze und eine Kachel für die Ablagen, für jeden Mandanten gleich. Nennt nur `ServiceID`-Kennungen. |
| [`process-view.md`](process-view.md) | Die Prozessansicht: `GET /api/prozesse/baum` mit Baum nach Partner → Richtung → Prozess oder nach Projekt, Kennzahlen je Prozess und Zeitfenster. Dazu Baum, Übertragungsliste und Panel. |
| [`live-rest.md`](live-rest.md) | Der Baustein, der Prozessbaum und Übersicht den Verkehr seit dem letzten Rollup-Lauf dazurechnet, mit Entscheidungsregel und Hinweis in der Oberfläche. |
| [`fehler-live.md`](fehler-live.md) | Die Einordnung `FEHLER` in Übersicht und Prozessbaum live aus `Message`, weil ein Fehlerstatus durch Nachverarbeitung nicht endgültig ist. |
| [`neu-laden.md`](neu-laden.md) | Der Knopf „Neu laden“ in Übersicht, Nachrichten und Prozessansicht und die automatische Aktualisierung der Nachrichten. |
| [`dichte-umschalter.md`](dichte-umschalter.md) | Die Anzeigegröße in vier Stufen über die Wurzelschriftgröße, gespeichert per Cookie. |
| [`dunkelmodus.md`](dunkelmodus.md) | Die Werte des dunklen Farbblocks mit per Test gehaltenen Kontrasten und der Umschalter hell, dunkel oder System ohne Aufblitzen. |
| [`spaltenwahl.md`](spaltenwahl.md) | Tabellen wählen ihre Spalten nach der Breite ihres Containers, mit Schwellen aus gemessenen Mindestbreiten. |
| [`tasks.md`](tasks.md) | Aufträge über den Assistenten, Auftragsmodell (Linie `assistent`, A1): Tabellen `task`, `task_type`, `task_type_mandant`, Auftragstypen als Daten samt Anleitung zum Anlegen per SQL, der Typ `WERK_ABLADESTELLE`, Prüfung, Parametertext, Einordnung und die Endpunkte unter `/api/tasks`, seit A4 auch das Löschen offener Aufträge. |
| [`assistent.md`](assistent.md) | Der Assistent im Backend (Linie `assistent`, A2): Ablauf von der Nachricht bis zur Karte, Datenfluss zum Anbieter, die anbieterneutrale Schnittstelle und der Adapter für Anthropic, Werkzeuge, Endpunkte unter `/api/assistent` und `POST /api/tasks/pruefung`, Grenzen, Testsatz. |
| [`assistent-frontend.md`](assistent-frontend.md) | Der Assistent im Frontend (Linie `assistent`, A3 und A4): Symbol in der Kopfleiste, Chatbox als Overlay, Bestätigungskarte mit Prüfung und Anlegen, Verhalten bei Fehlern, Barrierefreiheit und die Sichtprüfungen mit eingeschaltetem Assistenten; dazu der Block „Ihre Aufträge“ in der Übersicht mit den Ständen eines Auftrags und dem Löschen offener Aufträge (§12). |
| [`task-schnittstelle.md`](task-schnittstelle.md) | Der Vertrag für die EDI-Plattform, für sich lesbar: die vier Statements zum Lesen, Abholen und Zurückmelden, was der Monitor an der Tabelle tut (Anlegen, Löschen offener Aufträge), Parametertext, Rückgabe, Fehlertext und das Beispiel `WERK_ABLADESTELLE`. |

### Erhebungen

Messungen vor und während eines Schritts. Nicht jede M-Nummer steht hier: Feature-Dateien wie
`process-view.md`, `dunkelmodus.md`, `dashboard.md`, `rollup.md`, `live-rest.md`, `fehler-live.md`
und `message-status.md` tragen eigene Messungen.

| Datei | Inhalt |
|---|---|
| [`messungen-schritt4.md`](messungen-schritt4.md) | M0 bis M13 vor der Nachrichtenliste: Datenstand, Spalten und Indizes, Mandantenkette, BAM-Konfiguration, Einheit von `MessageTimeout`, Anker der Dev-Uhr; dazu die Statements L1 bis L15. |
| [`messungen-schritt5.md`](messungen-schritt5.md) | M14 bis M22 und M29 vor dem Nachrichtendetail: `MessageAction`, `MessageProperty`, `Service`, lesbare Schrittnamen und Wartezustand. |
| [`messungen-schritt6.md`](messungen-schritt6.md) | M23 bis M28, M30, M31 und die ergänzenden Messungen E1 bis E5 vor der Verkettung. |
| [`messungen-schritt7.md`](messungen-schritt7.md) | M32 bis M51, E6 und E7 zur Belegsuche: Index und Trefferzahlen, Zeitfenster, Normalisierung, Sollänge, Präfixsuche und das Komma im Wert. |
| [`messungen-schritt8-auftrag.md`](messungen-schritt8-auftrag.md) | Der Auftrag zur Messrunde vor Schritt 8: welche Messungen freigegeben und welche gesperrt waren. |
| [`messungen-schritt8.md`](messungen-schritt8.md) | M52 bis M73 vor den Rohdaten: Ablagen, Artefakt-Inventur, Abrufweg über die SOAP-Ablage, Größen, Kodierung und Beschnitt. |
| [`messungen-schritt9.md`](messungen-schritt9.md) | M74a bis M85 und M93 zu Prozess-Katalog und Benutzerverwaltung: Prozessbestand, Partner und Richtung in den Namen, Bestandslauf, Sitzungen je Konto. |
| [`messungen-schritt10.md`](messungen-schritt10.md) | M86 bis M92 vor dem Rollup: Nachlauf von `MessageLastUpdate`, Zeilenzahl und Kosten, Leseabfrage der Übersicht, Rückwärtslauf. |
| [`messungen-schritt10b.md`](messungen-schritt10b.md) | M94 bis M98 vor der Übersicht und M142 bis M145 zu den Kacheln *Läuft* und *Wartend*. |
| [`messungen-liste-verengung.md`](messungen-liste-verengung.md) | M99 bis M104: ob ein über den Rollup verengtes Fenster die Liste zur zeitgetriebenen Form bringt, und was die Vorabfrage kostet. |
| [`messungen-property-suche.md`](messungen-property-suche.md) | M153 bis M171 vor der Property-Suche: Gestalt und Deckung der Suchfeldkonfiguration, Plan des Wertprädikats und der Weg über Regel 4. |
| [`messungen-sichtprobe-schmal.md`](messungen-sichtprobe-schmal.md) | M176: die Anwendung am schmalen Fenster, gemessen im DOM; Grundlage von `spaltenwahl.md`. |
| [`messungen-suche-plan.md`](messungen-suche-plan.md) | M193 bis M197 zur Freitextsuche: der Produktionsbefund zum Plan, die statistikunabhängige Fassung und das Diagnoseskript. |

### Archiv

Das Archiv hält die Fassungen von Projektbeschreibung und README bis 1.0.0 unverändert fest, samt
allen Korrekturkästen; sie gelten nicht mehr.

| Datei | Inhalt |
|---|---|
| [`archiv/PROJEKTBESCHREIBUNG_bis-1.0.0.md`](archiv/PROJEKTBESCHREIBUNG_bis-1.0.0.md) | Die Projektbeschreibung bis 1.0.0, byte-gleich mit einer Kopfzeile. |
| [`archiv/README_bis-1.0.0.md`](archiv/README_bis-1.0.0.md) | Diese Datei bis 1.0.0, byte-gleich mit einer Kopfzeile; mit der früheren Tabelle „Offene Sichtprüfungen“. |
| [`archiv/konsolidierung-1.0.0.md`](archiv/konsolidierung-1.0.0.md) | Je Abschnitt und Korrekturkasten der alten Projektbeschreibung: wo die Aussage heute steht oder warum sie entfallen ist. |
