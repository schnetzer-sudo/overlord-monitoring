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

### Was nicht hineingehört — und was ausdrücklich doch

Geschützt sind **Nutzdaten und Namen Dritter**:

- echte Belegnummern und `MessagePropertyValue`-Inhalte
- **Partner**namen (`KE_OSTROV`, `DAS_DRAEXLMAIER` und dergleichen)
- Zugangsdaten, Passwörter, Passwort-Hashes, Zertifikate
- Hostnamen und Adressfragmente interner Server — auch dann, wenn sie in einem Dienstnamen stecken
  (`MULTI Service (<Knoten>) Produktion`)

**Nicht** geschützt ist das **Konfigurationsvokabular des Altsystems**: Bausteinmarken
(`NXS_FILE_CONVERT`, `FTPSender`), Prozess-, Projekt- und Ablaufnamen, Statuswerte, Mandantenkürzel.
Die zehn Mandanten stehen mit vollem Firmennamen in [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md)
§3.2; sie andernorts zu schwärzen, hilft niemandem und macht die Messdokumente unlesbar.

Enthält eine Marke oder ein Ablaufname den Namen eines **Partners** — nicht eines Mandanten —, wird
dieser Bestandteil maskiert (`<Kunde>`), wie in
[`messungen-schritt4.md`](messungen-schritt4.md) M13 geschehen.

> **Geschärft am 07.08.2026.** Hier stand nur „Zugangsdaten, Hostnamen, Produktionsdaten, echte
> Belegnummern, `MessagePropertyValue`-Inhalte. Beispiele werden anonymisiert." Das hat eine konkrete
> Frage nicht beantwortet: ob Bausteinmarken wie `NXS_FILE_CONVERT` als „Produktionsdaten" gelten,
> weil `NXS` ein Mandantenkürzel ist. Sie stand als offene Frage 12 in
> [`messungen-schritt5.md`](messungen-schritt5.md) und ist mit dieser Fassung entschieden: **Der
> Trennstrich läuft zwischen Nutzdaten und Konfiguration, nicht zwischen „intern" und „extern".**

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
| [`nachrichtendetail.md`](nachrichtendetail.md) | Schritt 5 (Teil 1 §1–§9, Teil 2 §10) | Die beiden Detail-Endpunkte: Kopf, Schrittfolge und kuratierte Eigenschaften in einem Aufruf, alle technischen Eigenschaften auf Abruf. Die **dreistufige Namensauflösung** (`DIREKT` → `HERGELEITET` → `ROHWERT`) samt der Eindeutigkeitsbedingung, die sie vor dem Raten bewahrt — sie hebt den Anteil benannter Schritte von 71,5 auf **99,7 Prozent**. Der **offene Zustand** (`LAEUFT_AUF`/`WARTET_VOR`/`OHNE_SCHRITT`/`KEINER`), die kuratierte Auswahl mit Befüllungsquote je Name **und je Mandant**, die harte Byte-Kappung der Werte, `EXPLAIN` und Laufzeit aller sechs Statements gegen drei Bezugsgestalten — und die bewussten Nicht-Entscheidungen (keine Zuordnungstabelle, kein Gerüst, keine Obergrenze) mit den Zahlen dahinter. **§10 ist die Oberfläche:** zwei Einhängepunkte (`?nachricht=` und `/nachrichten/<id>`) mit **einer** Komponente und ohne abfangende Routen, die auf **die Nachricht normierte** Zeitleiste samt Lückenzeile ab einer Minute, die Herkunft des Schrittnamens im Tooltip, die Eigenschaften erst beim Aufklappen — und der für „gibt es nicht" und „gehört einem anderen Mandanten" **identische** Fehlertext, der stattdessen den Mandanten in der Kopfzeile nennt |

Die folgenden Dateien entstehen laut Plan:

| Datei | Entsteht in | Inhalt |
|---|---|---|
| ~~`prozessschritte-uebersetzung.md`~~ | ~~Schritt 5~~ | **Entsteht nicht.** Angekündigt war eine Zuordnungstabelle `SOSActionServiceProperties` → Klartext. Sie ist durch [M19](messungen-schritt5.md#m19--wie-groß-müsste-die-zuordnungstabelle-sein) überholt: Über den ganzen dichten Monat stehen hinter 103.402 namenlosen Schritten **vier** Marken, **drei davon sind bereits lesbarer Klartext** (`EERP received`, `Message has been sent`, `EERP pending`). Zu übersetzen bliebe genau eine — `FTPSender` —, und ausgerechnet die löst anderswo auf **25 verschiedene** `SOSActionName` auf; eine Tabellenzeile müsste diese 25 auf einen Sammelbegriff eindampfen und wäre damit nicht die fehlende Übersetzung, sondern eine gröbere. An ihre Stelle tritt die zweite Stufe der Namensauflösung in [`nachrichtendetail.md`](nachrichtendetail.md) §2, die den **echten** Namen liefert und keine gepflegte Zeile braucht. Die Zeile bleibt hier stehen, statt still zu verschwinden — wer die Vorschau in einem älteren Stand liest, soll hier finden, warum die Datei fehlt |
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
| [`messungen-schritt5.md`](messungen-schritt5.md) | **vor Schritt 5** | M14 bis M17 gegen die Testkopie (07.08.2026), vor dem Bau des Nachrichtendetails: **M14** Spalten, Indizes und Größe von `MessageAction`, `MessageProperty` und `Service` — findet die undokumentierten Spalten `MessageAction.SOSID` und `MessageAction.SOSActionID` und belegt die Präfix-Indizes über 50 Zeichen. **M15** entscheidet, ob die Zeitleiste Klartext trägt: der Join über `(MessageAction.SOSID, MessageAction.SOSActionID)` ist mit **null Abweichungen** beim Vergleich des ausgeführten mit dem geplanten Baustein belegt, 71 bis 78 % der echten Schritte bekommen einen Namen — **die im Plantext beauftragte Zuordnungstabelle entfällt**. **M16** Gestalt der Schrittfolge: höchstens sieben Aktionen, also keine Obergrenze nötig; eine wartende Nachricht steht **zwischen** zwei Schritten, und der Hänger ist an `MessageActionEnd IS NULL` erkennbar (95 Fälle im Gesamtbestand). **M17** `MessageProperty`: 101 Namen statt der zehn dokumentierten, rund 22,6 statt zehn Zeilen je Nachricht, **595 Byte je Nachricht** statt der befürchteten Megabyte — und die Eigenschaften verteilen sich über die Schritte, getrennt in `Message.*` (Metadaten auf Schritt `0`) und `<Dienst>.*` (Protokoll am jeweiligen Schritt). Je mit Statement, Ergebnis, `EXPLAIN` und Laufzeit; dazu die Widersprüche zur Dokumentation und die daraus **offenen Fragen**. **Nachtrag vom 07.08.2026 (S1, M18 bis M22)** — diese Abschnitte ziehen ihre Schlussfolgerung ausdrücklich: **S1** legt das Kriterium des Metadaten-Schritts fest, **M18** zeigt, dass die Namenslücke nicht gestreut ist, sondern auf drei Kennungen sitzt, **M19** beziffert eine Zuordnungstabelle auf **eine Zeile für 90 %** und findet einen zweiten Weg ohne jede gepflegte Zeile, **M20** widerlegt die Vermutung zu den 43,9 % aus M13 und benennt die wahre Ursache (die Ablaufdefinition nummeriert nicht lückenlos), **M21** misst, wie oft ein Gerüst aus geplanten Schritten trägt, **M22** trennt den wiederkehrenden Status von dem einen Vorgang mit offenen Schritten |
| [`messungen-schritt4.md`](messungen-schritt4.md) | **vor Schritt 4** | M0 bis M7 gegen die Testkopie (01.08.2026): Datenstand, Spalten und Indizes, `MessageTimeout`, Nachrichten je Mandant, View gegen `EXISTS`, `SOSID`, `SPLITTED`/`MERGED`, BAM-Konfiguration — je mit Statement, Ergebnis, `EXPLAIN` und Laufzeit. Dazu die Auffälligkeiten und die daraus **offenen Fragen**. Reine Erhebung, keine Entscheidungen. Nachgetragen: M8/M9 (die beiden Fragen, die sie entscheiden), L1 bis L13 zu den Statements des Listen-Endpunkts — darunter M10 und L11, die beide vorgesehenen Umbauten des Freitextfilters widerlegen, und **L13**, aus dem seine Fenstergrenze folgt. Zur Nachbesserung (06.08.2026): **M11** BAM-Abdeckung je Typ (Vorarbeit für Schritt 7), **M12** Zwischenschritte je Mandant, **M13** `SOSAction` — die Messung, die entscheidet, ob der aktuelle Schritt anzeigbar ist |
