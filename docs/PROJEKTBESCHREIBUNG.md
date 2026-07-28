# Overlord Monitoring — Projektbeschreibung

Stand: 27.07.2026 · Diese Datei ist der verbindliche Kontext für alle Arbeiten am Projekt.
Bei Widersprüchen zwischen dieser Datei und einer Annahme im Code gilt diese Datei.

**Revision 27.07.2026.** Erhebung gegen die Testkopie vor Schritt 2. Geändert haben sich das
Mengengerüst (Abschnitt 3.2 und 8), die Statusdefinition (Abschnitt 4.1 und 4.2), der Datenstand
der Testkopie (Abschnitt 8) sowie die Annahmen A6 und A7 (Abschnitt 11). Die betroffenen Stellen
sind mit dem Datum gekennzeichnet.

---

## 1. Zweck

Ein neues Monitoring-Werkzeug für die EDI-Integrationsplattform **Overlord**. Es liest aus der
bestehenden MariaDB (`GlassfishDB`) und stellt Mandanten den Zustand ihrer EDI-Übertragungen dar.

Das bestehende Werkzeug (ExtJS 4.1, Servlet-basiert) wird **nicht nachgebaut**. Es dient nur als
Referenz dafür, welche Daten fachlich gebraucht werden. Die Bedienlogik wird neu gedacht.

### Leitsatz

Der typische Nutzer ist kein EDI-Spezialist. Er sucht einen Beleg und will wissen, wo dieser steht.
Jede Entscheidung im Zweifel zugunsten dieser Frage treffen — nicht zugunsten technischer
Vollständigkeit. Interne IDs, Statuscodes und Servicenamen sind Beiwerk, keine Hauptinformation.

---

## 2. Nutzer und Rollen

| Rolle | Wer | Sieht |
|---|---|---|
| `MANDANT` | Kundenmitarbeiter (extern) | Ausschließlich Daten des eigenen Mandanten |
| `ADMIN` | Interne EDI-Betreuung | Alle Mandanten, zusätzlich Benutzer- und Katalogpflege |

Primäre Zielgruppe ist die Rolle `MANDANT`. Externe Nutzer bedeuten: Die Mandantentrennung ist
eine Sicherheitsanforderung, keine Komfortfunktion.

Aktuelle Annahme: Ein Nutzer gehört zu genau einem Mandanten (so ist es im Altsystem modelliert).
Das Datenmodell wird trotzdem für n:m vorbereitet, damit ein späterer Wechsel keine Migration erzwingt.

---

## 3. Quellsystem `GlassfishDB`

**Zugriff ausschließlich lesend.** Es wird niemals in dieses Schema geschrieben. Der DB-Benutzer
für dieses Schema besitzt ausschließlich `SELECT`.

### 3.0 Verbindung und Rechte (Stand 27.07.2026)

Server MariaDB 10.6.22. Beide Schemata liegen auf derselben Instanz, beide mit `utf8mb4` und
`utf8mb4_general_ci`. Alle Schlüsselspalten (`MandantID`, `ProjectID`, `ProcessID`, `MessageID`)
sind `varchar(36)`.

| Benutzer | `GlassfishDB` | `overlord_monitor` |
|---|---|---|
| `monitor_read` | `SELECT` | `SELECT` |
| `monitor_root` | `SELECT` | `ALL PRIVILEGES` |

Kein Benutzer besitzt irgendein Schreibrecht auf `GlassfishDB`. Host, Benutzernamen und Passwörter
stehen ausschließlich in Umgebungsvariablen, niemals in einer versionierten Datei.

MariaDB 10.6 hat seinen Wartungszeitraum im Juli 2026 erreicht. Die Instanz gehört uns nicht, aber
wir lesen dauerhaft darauf — siehe Annahme A10.

### 3.1 Hierarchie

```
Mandant ──< ProjectMandant >── Project ──< Process ──< SOS ──< SOSAction
                                            │           │
                                            └──< Message ──< MessageAction ──< MessageProperty
                                                    │
                                                    └──< MessageBAM
```

Die Zuordnung einer Nachricht zu einem Mandanten läuft über vier Joins und ist in der vorhandenen
View `MessageMandantID` gekapselt:

```
Message → Process → Project → ProjectMandant → Mandant
```

`ProjectMandant` ist eine n:m-Beziehung. Ein Projekt kann mehreren Mandanten zugeordnet sein.

### 3.2 Kerntabellen

**`Message`** — eine Nachricht/Übertragung.
`MessageID` varchar(36) PK · `ProcessID` FK · `SOSID` · `SOSActionID` · `MessageStatus` varchar(30) ·
`MessageLastUpdate` timestamp · `MessageTimeout` smallint · `SourceMessageID` · `TargetMessageID` ·
`Source` bit · `Target` bit

Wichtig:
- Es gibt **kein Anlagedatum**. `MessageLastUpdate` ist der Zeitpunkt der letzten Änderung.
  Der fachliche Start ist `MIN(MessageAction.MessageActionStart)`.
- `MessageTimeout` ist eine **Dauer in Minuten**, kein Zeitpunkt. Der Timeout-Zeitpunkt wird im
  Backend berechnet.
- Nutzbare Indizes: `MessageLastUpdateIDX`, `MessageStatusIDX` und der zusammengesetzte
  `MessageLastUpdateProcessMessageIDX (MessageLastUpdate, ProcessID, MessageID)`.

**`MessageAction`** — die einzelnen Prozessschritte einer Nachricht.
PK `(MessageID, MessageActionID)` · `MessageActionStart` / `MessageActionEnd` · `ServiceID` ·
`SOSActionServiceProperties` mediumtext · `SOSActionTimeout`

`SOSActionServiceProperties` enthält die ausgeführten Bausteine als pipe-getrennte Liste, etwa
`NXS_FILE_CONVERT|E2A|UNWRAP` oder `NXS_MERGE|KE_OSTROV_734973|WAIT|30M|30406_..._MRG`.
Diese Rohwerte werden dem Nutzer **nicht** angezeigt, sondern in lesbare Schritte übersetzt.

**`MessageProperty`** — Schlüssel/Wert-Paare je Nachricht (EAV).
PK `(MessageID, MessagePropertyName, MessageActionID)` · `MessagePropertyValue` mediumtext

**Gemessen am 27.07.2026: 46.964.279 Zeilen, 61 GB.** Rund vierzehn Zeilen je Nachricht und
durchschnittlich 1,3 Kilobyte je Zeile. Diese eine Tabelle ist 82 Prozent der Datenbank — die
Bytegröße ist hier die maßgebliche Kennzahl, nicht die Zeilenzahl.

**Zugriff ausschließlich über `MessageID`.** Niemals filtern, gruppieren oder sortieren über
`MessagePropertyValue` — die Indizes darauf sind Präfix-Indizes über 50 Zeichen und für
Aggregation ungeeignet. Jede Abfrage, die anders als über `MessageID` einsteigt, wälzt ein
Vielfaches dessen um, was `Message` insgesamt groß ist.

Bekannte Namen: `Message.GUID`, `Message.SendingPartner`, `Message.SNDPRN`, `Message.VFN`,
`Message.SOS`, `Message.SplitCount`, `Message.Payload.GUID` (Format `FILESTOREPROD09|<uuid>`),
`Message.InterchangeNumber`, `Message.CommitInterchangeNumber`, `Message.SourceMessageID`.

**`MessageBAM`** — fachliche Suchschlüssel (Business Activity Monitoring).
PK `(MessageID, MessageBAMType, MessageBAMValue)` · `MessageBAMValue` varchar(70), eigener Index

Das ist die zentrale Suchdimension für Fachanwender: Lieferschein-Nr., Bestellnummer,
Transport-Nummer, Charge, Werk, Materialnummer und so weiter. `MessageBAMType` verweist auf
`MessageBAMType.MessageBAMTypeDescription`.

Gemessen am 27.07.2026: 10.859.666 Zeilen, 7,1 GB, rund drei Einträge je Nachricht.

**`MessageBAM` hat keinen Zeitstempel.** Das Pflicht-Zeitfenster aus Abschnitt 8 kann deshalb erst
**nach** dem Join auf `Message` greifen, also nach dem teuren Teil. Ein `LIMIT` vor dem Join hilft
nicht, weil es die falschen Zeilen abschneidet: die ersten nach Indexreihenfolge, nicht die
neuesten. Bei häufigen Werten — die Abladestelle `050` ist ein solcher — liefert der Index auf
`MessageBAMValue` potenziell hunderttausende Treffer. Siehe Annahme A9.

**`MessageBAMMandant`** — steuert je Mandant, welche BAM-Typen sichtbar sind und in welcher
Reihenfolge (`MessageBAMTypeSortIndex`). Diese Konfiguration wird für die Spaltenauswahl und die
Suchfelder übernommen, nicht neu erfunden.

**`Process` / `Project` / `Mandant` / `ProjectMandant`** — die Hierarchie. Reine Stammdaten.

**`SOS` / `SOSAction`** — "Sequence of Services", der konkrete, aus Bausteinen zusammengesetzte
Ablauf. `SOSName` ist bereits in Klartext gepflegt ("Lieferabruf von AMG (VDA)", "Eingehender
IFTMIN BAYER") und wird als Anzeigename verwendet. Das Verhältnis Process zu SOS ist meist 1:1,
gelegentlich 1:n (Varianten wie `_OUT`, `_MAIL`).

**`User`** — Alt-Benutzertabelle. `UserPassword` varchar(20) im **Klartext**. Wird nicht
weiterverwendet, siehe Abschnitt 7.

**`MessageStatisticHistory`** und View `MessageStatistic` — bestehende Aggregation. Der Schlüssel
`Period` ist ein zusammengesetzter String der Form `<MandantID>&&<YYYYMMDDHH>&&HOUR` bzw. `&&DAY`
oder `&&MONTH`. Liefert nur Anzahlen, keine Aufschlüsselung nach Status oder Partner.

**Stand 27.07.2026: Die Tabelle enthält 42 Zeilen.** Sie ist als Datenquelle unbrauchbar, und die
View `MessageStatistic` ist keine Abkürzung für unser Dashboard. `message_rollup` wird von Grund
auf selbst gefüllt.

### 3.3 Datenbank-Events (laufen weiter, gehören uns nicht)

| Event | Takt | Wirkung |
|---|---|---|
| `CreateMessageStatisticHistory` | täglich | füllt `MessageStatisticHistory` |
| `MatchInterchange` | stündlich | ordnet COMMITs über Interchange-Nummern zu, setzt `MessageStatus = 'COMMIT_RECEIVED'` und `SourceMessageID` |
| `SetTargetFlag` | — | setzt `Target`-Flag |
| `MoveDTNA997` | stündlich | verschiebt Nachrichten zwischen zwei Prozessen (kundenspezifisch) |

Konsequenz: Die COMMIT-Zuordnung ist bis zu eine Stunde verzögert. Das muss in der Oberfläche
kommuniziert werden, sonst wirkt eine korrekt übertragene Nachricht wie unquittiert.

---

## 4. Fachliche Definitionen

### 4.1 Nachrichtenstatus

**`MessageStatus` ist freier Text, kein Aufzählungstyp.** In den Produktivdaten steht zweimal
`CKECKED` — ein Tippfehler, der nie korrigiert wurde. Unbekannte Werte müssen deshalb immer als
Rohwert durchgereicht werden und dürfen nie stillschweigend in einen bekannten Eimer fallen.

Vollständige Erhebung der Testkopie seit 01.01.2025 (Stand 27.07.2026):

| Status | Anzahl | Einordnung | Anzeige |
|---|---|---|---|
| `FINISHED` | 1.663.884 | abgeschlossen | grün |
| `MERGED` | 607.277 | Zwischenschritt, Verkettung beachten | neutral |
| `SPLITTED` | 310.263 | Zwischenschritt, Verkettung beachten | neutral |
| `EERP_RECEIVED` | 116.828 | Empfangsbestätigung liegt vor | grün |
| `COMMIT_RECEIVED` | 10.126 | Empfangsbestätigung liegt vor | grün |
| `COMMIT_SENT` | 979 | ungeklärt | neutral |
| `ERROR_DUPLICATE` | 659 | **Fehler** | rot |
| `SUSPENDED` | 538 | wartet, z. B. auf Zusammenführung. **Kein Fehler.** | neutral |
| `CHECKED` | 257 | ungeklärt | neutral |
| `COMMIT_REJECTED` | 111 | **Fehler** — Partner hat abgelehnt | rot |
| `ERROR_TIMEOUT` | 52 | **Fehler** | rot |
| `CKECKED` | 2 | ungeklärt (Tippfehler) | neutral |
| `RUNNING` | 0 | läuft gerade | neutral |

**`COMMIT_REJECTED` zählt als Fehler, obwohl der Wert nicht mit `ERROR_` beginnt.** Der Partner
hat die Übertragung abgelehnt, der Beleg ist nicht angekommen. Für den Nutzer ist das dieselbe
Frage wie bei einem `ERROR_`-Status.

**`RUNNING` kommt in der Testkopie null Mal vor, in der Produktion aber sehr wohl.** Der Status ist
flüchtig und existiert nur, solange eine Nachricht tatsächlich in Arbeit ist. Zwei Folgen: Die
Problemkategorie "Überfällig" ist gegen die Testkopie nicht prüfbar, und "läuft noch" darf niemals
als `MessageStatus = 'RUNNING'` definiert werden — die Kategorie wäre sonst konstruktionsbedingt
leer.

**`CHECKED`, `CKECKED` und `COMMIT_SENT` gelten als bekannt, aber fachlich ungeklärt.** Sie werden
neutral behandelt und mit Rohwert plus dem Hinweis "Bedeutung nicht verifiziert" angezeigt. Es
wird nichts geraten.

**Die Fehlerabfrage.** In SQL ist `_` ein Platzhalter für ein beliebiges Zeichen, `LIKE 'ERROR_%'`
trifft also auch `ERRORX...`. Verwendet wird:

```sql
MessageStatus LIKE 'ERROR\_%' ESCAPE '\' OR MessageStatus = 'COMMIT_REJECTED'
```

**Nicht** `LEFT(MessageStatus, 6) = 'ERROR_'`. Diese Form kann `MessageStatusIDX` nicht nutzen und
erzwingt in Kombination mit der Oder-Bedingung einen vollen Durchlauf über 2,9 GB. Die
`LIKE`-Fassung ergibt zwei Indexbereiche, die MariaDB zusammenführen kann.

**Die Einordnung entsteht an genau einer Stelle im Code** (`MessageStatusClassifier` in `common`)
und wird von Liste, Dashboard, Rollup und später vom Chatbot verwendet. Sie wird nirgends
nachgebaut, sonst driftet sie über die Ausbaustufen auseinander.

**Sicherung gegen neue Statuswerte:** Ein automatisierter Test vergleicht
`SELECT DISTINCT MessageStatus` gegen die dokumentierte Liste und wird rot, sobald im Altsystem
ein unbekannter Wert auftaucht. Nur deshalb ist es vertretbar, unbekannte Werte neutral zu
behandeln.

### 4.2 Die drei Problemkategorien

Diese drei sind bewusst getrennt und dürfen nie zu "Fehler" zusammengefasst werden:

1. **Fehler** — `MessageStatus` beginnt mit `ERROR_` **oder** ist `COMMIT_REJECTED`.
   Die Fehlerart ist bei `ERROR_*` der Namensteil dahinter, bei `COMMIT_REJECTED` der feste Text
   "Vom Partner abgelehnt".
2. **Überfällig** — die Nachricht ist **nicht in einem Endstatus** und
   `MessageLastUpdate + MessageTimeout` liegt in der Vergangenheit. Nicht über
   `MessageStatus = 'RUNNING'` definieren, siehe 4.1.
3. **Unquittiert** — ausgehende Nachricht ohne zugeordnete Empfangsbestätigung.
   **`COMMIT_REJECTED` gehört ausdrücklich nicht hierher.** Das ist eine Quittung, nur eine
   negative. Sonst erscheint derselbe Beleg in zwei Kacheln und die Zahlen wirken erfunden.

### 4.3 Verkettung (Lineage)

Eine Nachricht ist selten allein. Über `SourceMessageID`, `TargetMessageID` sowie die Flags
`Source` und `Target` entsteht eine Kette: eine eingehende Sammelnachricht wird gesplittet, die
Teile werden verarbeitet, mehrere werden zusammengeführt, am Ende kommt eine Quittung zurück.

Für den Nutzer ist genau das die Antwort auf "wo ist mein Lieferschein". Die Verkettung ist
deshalb MVP-Bestandteil, nicht Ausbaustufe.

### 4.4 Partner, Richtung, Belegart

**Diese vier Angaben sind in `GlassfishDB` nicht als Daten vorhanden.** Sie stecken in
Namenskonventionen, und die Konventionen unterscheiden sich je Mandant:

- NEXANS: Projekt trägt Richtung und Kategorie (`300_KundenEingehend`), Prozess trägt Partner und
  Belegart (`40000_AMG_LAB_VDA`)
- VTG / Suttons: Projekt trägt Geschäftsbereich und Partner (`100_VTG_BAYER`,
  `100_SUTTONS_BAYER`), die Richtung steht nur im `SOSName`
- Partnernamen sind nicht token-sauber: `KE_OSTROV`, `DAS_DRAEXLMAIER`, `TYCO_AMP`,
  `DELFINGEN_DE_HA` bestehen aus mehreren Teilen
- Granularität ist eine fachliche Frage: `BASF`, `BASFANTWERPEN`, `BASFPOLY`, `NONBASF`

**Entscheidung: Es wird nicht geparst, es wird kuratiert.** Partner, Standort, Richtung und
Belegart sind Felder im Prozess-Katalog (Abschnitt 5). Eine Heuristik befüllt vor, die Wahrheit
steht im Katalog. Nicht zugeordnete Prozesse erscheinen in Auswertungen sichtbar als
"nicht zugeordnet" — niemals als geratener Wert.

Sonderfall: `00001_Undefined` ist ein Auffangprozess und wird in der Oberfläche gesondert behandelt.

---

## 5. Eigenes Schema `overlord_monitor`

Liegt auf derselben MariaDB-Instanz, wird per Flyway verwaltet. Eigener DB-Benutzer mit
Schreibrechten **ausschließlich** auf dieses Schema.

| Tabelle | Zweck |
|---|---|
| `app_user` | Benutzer, Passwort-Hash, Mandant, Rolle, Sperrzustand |
| `app_user_mandant` | n:m-Zuordnung (vorbereitet, MVP nutzt 1:1) |
| `process_catalog` | je `ProcessID`: Partner, Standort, Richtung, Belegart, Pflegestatus |
| `partner` | kuratierte Partnerstammdaten |
| `message_rollup` | stündliche Aggregate je Mandant, Prozess, Partner, Richtung, Status |
| `audit_log` | Anmeldungen, Fehlversuche, Passwortänderungen, Katalogänderungen |
| `saved_view` | gespeicherte Filter je Nutzer |

### Zeichensatz und Sortierung — verbindlich

Das Schema und **jede einzelne Tabelle** bekommen `utf8mb4` und `utf8mb4_general_ci`
**explizit** in der Migration, nie geerbt. `GlassfishDB` verwendet dieselbe Sortierung. Ohne
explizite Angabe bricht jeder schemaübergreifende Join, sobald der Server-Default sich ändert oder
die Instanz auf MariaDB 11 gehoben wird — dort sind die `uca1400`-Sortierungen Standard. Der
Fehlerfall ist entweder "Illegal mix of collations" oder, schlimmer, ein Join, der läuft und dabei
den Index ignoriert.

`utf8mb4_general_ci` vergleicht **ohne Rücksicht auf Groß- und Kleinschreibung**. Für
`app_user.username` ist das erwünscht — der eindeutige Index verhindert damit von selbst, dass
"Lukas" und "lukas" zwei Konten werden. Für alles Tokenartige ist es das Gegenteil:
**Session-IDs, Rücksetz-Token und vergleichbare Spalten bekommen `COLLATE utf8mb4_bin`.**

---

## 6. Technische Architektur

### Backend
Spring Boot 4.1, Java 21, jOOQ, Flyway (nur für `overlord_monitor`), Spring Security.

**Zwei DataSources, aber eine Rechteregel.** Beide Datenbankbenutzer erhalten `SELECT` auf beide
Schemata, damit schemaübergreifende Joins in einer Abfrage möglich sind (`GlassfishDB.Message`
gejoint auf `overlord_monitor.process_catalog`). Nur einer der beiden besitzt zusätzlich
Schreibrechte, und ausschließlich auf `overlord_monitor`.

**Kein Datenbankbenutzer dieser Anwendung besitzt irgendein Schreibrecht auf `GlassfishDB`.**
Daran hängt die Garantie, nicht an der Trennung der Verbindungen.

Der Schreibschutz hat drei Schichten, die **nicht gleichwertig** sind:

1. **Die DB-Rechte sind die Wahrheit.** Nur sie tragen die Zusage.
2. **Der `readOnly`-Pool ist Zusatz, kein Nachweis.** Beim MariaDB-Treiber ist nicht verlässlich,
   was `setReadOnly` serverseitig bewirkt. Nicht als Garantie dokumentieren.
3. **Ein jOOQ-`ExecuteListener`** auf dem Lese-Kontext weist alles ab, was nicht
   `ExecuteType.READ` ist, und fängt den Fehler damit im Code statt im Netz.

**Keine der beiden DataSources ist `@Primary`.** Ein vergessener Qualifier soll beim Start eine
`NoUniqueBeanDefinitionException` auslösen und nicht still den falschen Pool verdrahten — ein
Startfehler ist besser als ein Laufzeitfehler in einem selten begangenen Codepfad. Flyway und die
Session werden über `@FlywayDataSource` beziehungsweise `@SpringSessionDataSource` gebunden.

**Genau ein Transaktionsmanager**, gebunden an `overlord_monitor` und als `@Primary` markiert.
Damit bedeutet `@Transactional` im gesamten Projekt eindeutig "schreibt ins eigene Schema". Folge,
die man kennen muss: Ein Lesezugriff innerhalb einer `@Transactional`-Methode läuft auf einer
anderen Verbindung und ist nicht Teil dieser Transaktion.

**Die Regel für die beiden DSLContexts** lautet nicht "`monitor`-Klassen gehören zum
Schreib-Kontext", sondern:

> Der Lese-DSLContext darf **beide** Schemata lesen. Der Schreib-DSLContext darf ausschließlich
> `overlord_monitor` und ausschließlich dort schreiben.

Schemaübergreifende Abfragen laufen zwingend über eine einzige Verbindung, also über den
Lese-Pool. Deshalb kein `defaultSchema` in der jOOQ-Konfiguration — Schemanamen werden immer voll
qualifiziert gerendert.

**Der Lese-Pool ist klein und hat eine Laufzeitgrenze** (`SET SESSION max_statement_time`). Zur
Laufzeit wird auf der Produktion gelesen; die Leistungsregeln aus Abschnitt 8 schützen vor
Abfragen, die wir absichtlich schreiben, nicht vor der einen Filterkombination, die niemand
gemessen hat. Ein Statement, das nach zehn Sekunden stirbt, ist ein Fehler in der Oberfläche. Eines,
das nicht stirbt, ist ein Vorfall in der EDI-Plattform.

**Zeitzonen.** Zeitstempel aus `GlassfishDB` werden als Wanduhrzeit des Servers behandelt und
nirgends konvertiert; die JDBC-URL bekommt keine Zeitzonenparameter. Im eigenen Schema wird UTC
gespeichert. Die Bruchstelle ist bewusst und dokumentiert — die Alternative, alles auf UTC zu
ziehen, scheitert daran, dass wir `GlassfishDB` nicht anfassen dürfen.

**Keine Fremdschlüssel über Schemagrenzen.** `process_catalog` verweist auf die `ProcessID` als
Zeichenkette. Wird ein Prozess im Altsystem gelöscht, bleibt die Katalogzeile bestehen — sonst
verschwände rückwirkend die Partnerzuordnung aller historischen Nachrichten. Verwaiste Einträge
sind hier erwünscht.

Kein JPA. Das Quellschema gehört uns nicht, ist lesegetrieben und teilweise EAV — ORM-Mapping wäre
hier ein Nachteil. jOOQ generiert typsichere Zugriffe aus dem Schema und zahlt sich beim späteren
Chatbot zusätzlich aus.

Begründung Spring Boot statt Quarkus: Das Projekt wird schrittweise mit Claude Code umgesetzt. Für
Spring Boot existiert deutlich mehr Trainingsmaterial, der generierte Code ist verlässlicher.

**Basispaket: `de.kraftwerkone.overlord.monitor`.** Reverse-Domain zu `overlord.kraftwerkone.de`.
`overlord.monitor` statt `overlordmonitor`, damit das Werkzeug im Namensraum sichtbar vom
beobachteten System getrennt bleibt. Maven: `groupId = de.kraftwerkone`,
`artifactId = overlord-monitor`. Die Hauptklasse `OverlordMonitorApplication` liegt direkt
im Basispaket, damit der Component-Scan die gesamte Anwendung erfasst.

Paketstruktur — fachlich geschnitten, nicht nach Schichten, passend zur Regel "pro Ansicht ein
eigenes Modul":

```
de.kraftwerkone.overlord.monitor
├─ config/      DataSources, jOOQ, Security, Flyway
├─ common/      Fehlerformat, Cursor-Paginierung, Filterabstraktion, TimeProvider
├─ security/    MandantContext, Session, Anmeldesperre
├─ audit/
├─ message/     Liste, Detail, Verkettung
├─ bam/
├─ payload/     Download-Proxy
├─ catalog/     process_catalog, partner
├─ rollup/      per Profil separat startbar
├─ dashboard/
├─ admin/
└─ jooq/        generiert, nicht handgepflegt
```

Fachpakete kennen einander nicht. Gemeinsames liegt in `common`, nicht in einem Nachbarmodul.

**Zwei getrennte Zielpakete für die jOOQ-Codegenerierung:**

```
de.kraftwerkone.overlord.monitor.jooq.glassfish   ← Quellschema, ausschließlich lesend
de.kraftwerkone.overlord.monitor.jooq.monitor     ← eigenes Schema, schreibend
```

Das ist Schutz, keine Kosmetik. Bei einem schemaübergreifenden Join ist am Import sofort
erkennbar, ob gerade eine Tabelle angefasst wird, auf die niemand schreiben darf. Gleichnamige
Tabellen in beiden Schemata kollidieren nicht.

**Die Codegenerierung läuft bei jedem lokalen Build gegen die Testkopie, die erzeugten Quellen
werden aber eingecheckt** (`src/main/generated-java`). Damit bricht der Build am selben Tag, an
dem jemand am Altschema etwas ändert — und die CI, die das interne Netz nicht erreicht, baut
trotzdem. Sie überspringt den Codegen über `-Djooq.codegen.skip=true`. Tests, die eine Datenbank
brauchen, tragen `@Tag("db")` und werden dort ausgeschlossen.

Der Rollup-Job bekommt trotz separater Startbarkeit kein eigenes Wurzelpaket. Die Trennung läuft
über das Spring-Profil, nicht über den Namensraum.

### Frontend
Next.js 16 (App Router), shadcn/ui, TanStack Query, TanStack Table, Recharts, nuqs.

Next.js dient als BFF für das Cookie-Handling und trifft **keine** Berechtigungsentscheidungen.
Rollenabhängige Navigation ist reine Bequemlichkeit; verbindlich prüft immer das Backend.

Das Frontend hält **keine eigenen Daten** und hat keine Datenbank. Es spricht ausschließlich über
HTTP mit dem Backend.

Filterzustand liegt in der URL (nuqs), damit Ansichten teilbar sind.

### Erweiterbarkeit
Neue Ansichten müssen ohne Umbau ergänzbar sein. Deshalb: eine gemeinsame Filter- und
Paginierungsabstraktion, ein Layout-Shell mit datengetriebener Navigation, und pro Ansicht ein
eigenes Modul in Backend wie Frontend.

### Betrieb

Backend als eigenständiges JAR mit eingebettetem Server, betrieben als systemd-Dienst oder im
Container. Frontend als Node-Prozess daneben. Davor ein Reverse Proxy, der TLS beendet und beide
unter einer Domain zusammenführt.

Bewusst **nicht** im vorhandenen GlassFish: Ein Monitoring-Werkzeug soll unabhängig von dem
laufen, was es beobachtet. Teilte es sich JVM und Applikationsserver mit dem Altsystem, nähme ein
Speicherproblem dort das Werkzeug mit — genau dann, wenn es gebraucht wird.

Eine Rückfalloption auf GlassFish gibt es nicht: Spring Boot 4 setzt Jakarta EE 11 voraus, die
vorhandene Instanz ist GlassFish 6 oder 7 und damit Jakarta EE 9 beziehungsweise 10. Ein
WAR-Deployment dorthin ist ausgeschlossen. Der Rollup-Job wird trotzdem per Profil separat
startbar geschnitten, weil das den Betrieb entzerrt.

**Versionsstand, geprüft am 24.07.2026.** Spring Boot 4.1 ist aktuell; der 3.x-Zweig hat mit
3.5.16 seinen letzten Patch erhalten und bekommt keine Sicherheitsfixes mehr. Next.js 16 ist
Active LTS bis Oktober 2027, Version 15 läuft im Oktober 2026 aus. Spring Boot 4 bringt Spring
Framework 7, Spring Security 7 und Jackson 3 — generierter Code enthält häufig noch Muster der
Vorgängergeneration, das ist bei jedem Schritt zu prüfen.

---

## 7. Sicherheit

### Authentifizierung
Eigene Benutzerverwaltung. BCrypt mit Kostenfaktor 12. Serverseitige Session, Session-ID im Cookie
mit `HttpOnly`, `Secure`, `SameSite=Lax`. Kein JWT — bei externen Nutzern wiegt sofortige
Rücknehmbarkeit schwerer als Zustandslosigkeit.

Sperre nach fünf Fehlversuchen für 15 Minuten, zusätzlich Begrenzung pro IP. Fehlermeldungen
bleiben unspezifisch, damit sich keine Benutzernamen durchprobieren lassen.

Migration: Benutzername, Mandant und Rolle werden aus der Alt-`User`-Tabelle übernommen. Die
Klartext-Passwörter werden **nicht** übernommen und gelten als kompromittiert. Alle migrierten
Konten starten gesperrt mit Zwang zur Neuvergabe.

### Mandantentrennung — die wichtigste Regel des Projekts

**Kein Endpunkt nimmt jemals eine Mandanten-ID entgegen.** Der Mandant wird ausschließlich aus der
Session gelesen.

Jede Repository-Methode bekommt den Mandanten als ersten Pflichtparameter. Es gibt keine
Überladung ohne ihn. Der Filter über `ProjectMandant` ist Bestandteil jedes Statements, nicht
nachgelagerte Prüfung. Wer eine fremde `MessageID` errät, bekommt null Zeilen — weil die Zeile für
ihn nie existiert hat.

Pro Endpunkt existiert ein automatisierter Test, der mit Mandant A abfragt und nachweist, dass
Daten von Mandant B unerreichbar sind. Ein neuer Endpunkt ohne diesen Test wird nicht gemergt.

### Rohdatenzugriff

Nutzer dürfen die ursprüngliche EDI-Datei herunterladen. Das ist deutlich sensibler als die
Metadaten: Dort stehen Preise, Mengen und Kundendaten.

**Der Download läuft immer über das Backend als Proxy, niemals als direkter Link in den Browser.**
Der Filestore kennt unsere Nutzer nicht und kann die Mandantenprüfung nicht leisten. Ein
durchgereichter Link wäre ein unkontrollierter, per Copy-Paste teilbarer Zugang.

Ablauf: `Message.Payload.GUID` hat das Format `<FilestoreID>|<UUID>`, etwa
`FILESTOREPROD09|d95499ff-...`. Der vordere Teil wird über die `Service`-Tabelle und deren
`ServiceConnectString` zum konkreten Filestore aufgelöst (Servicetyp "TOMCAT Filestore"). Das
Backend prüft zuerst die Mandantenzugehörigkeit der Nachricht, holt dann die Datei und streamt sie
durch.

Regeln:
- Ausschließlich `Content-Disposition: attachment`, **niemals inline rendern**. Eine EDI-Datei kann
  gültiges HTML oder SVG enthalten — inline wäre das eine von außen befüllbare
  Cross-Site-Scripting-Lücke.
- `Content-Type: application/octet-stream`, kein Erraten des Typs.
- Jeder Download wird mit Nutzer, Nachricht, Zeitpunkt und IP im `audit_log` protokolliert.
- Alle Mandantennutzer sind berechtigt. Trotzdem wird die Berechtigung als Flag an `app_user`
  modelliert (Standardwert: erlaubt), damit ein späterer Entzug keine Migration erfordert.

---

## 8. Verbindliche Leistungsregeln

**Gemessenes Mengengerüst, 27.07.2026** (ersetzt die frühere Schätzung von 10.000 bis 100.000
Nachrichten pro Tag und 36 Millionen Zeilen):

| Tabelle | Zeilen | Größe |
|---|---|---|
| `MessageProperty` | 46.964.279 | 61,0 GB |
| `MessageBAM` | 10.859.666 | 7,1 GB |
| `MessageAction` | 10.215.743 | 3,0 GB |
| `Message` | 3.341.519 | 2,9 GB |
| `Process` | 1.490 | — |
| `Project` | 142 | — |
| `User` | 36 | — |

Rund 5.000 Nachrichten pro Tag. Die Aufbewahrung beträgt **22 Monate** — ältester Datensatz
01.10.2024 —, nicht ein Jahr. Es wird auf der Produktionsdatenbank gelesen.

Die Zeilenzahl war nie die richtige Kennzahl. `MessageProperty` belegt 61 GB und ist damit 82
Prozent der Datenbank; dort entscheidet die Bytegröße.

1. **Jeder Listen-Endpunkt hat ein Pflicht-Zeitfenster.** Standard 24 Stunden, Maximum ein Jahr.
   Ohne Zeitfenster keine Abfrage.
2. **Keine Live-Aggregation über `Message`.** Dashboard-Kennzahlen kommen ausschließlich aus
   `message_rollup`. Ein stündlicher Job schreibt inkrementell fort.
   *Begründung korrigiert 27.07.2026:* Bei 3,3 Millionen Zeilen und 2,9 GB wäre eine
   Live-Aggregation technisch nicht unmöglich — das Altsystem macht sie tagesweise. Der Rollup
   existiert, damit das Dashboard unter einer halben Sekunde lädt und die Produktionsdatenbank
   nichts davon merkt, nicht weil eine Live-Abfrage scheitern würde. Eine Regel, die mit einer
   falschen Zahl begründet ist, wird beim ersten Zweifel gekippt.
3. **Keine `OFFSET`-Paginierung.** Cursor-basiert über `(MessageLastUpdate, MessageID)`.
4. **`MessageProperty` nur über `MessageID`.** Nie filtern, gruppieren oder sortieren über den Wert.
5. **BAM-Suche mit hartem Limit und Mindestlänge** des Suchbegriffs. BAM-Werte wie `050` kommen
   millionenfach vor.
6. **Der Rollup-Job läuft gedrosselt.** Er teilt sich die Instanz mit der Produktion.
7. **Jede neue Abfrage wird vor dem Merge gegen die Testkopie gemessen** (`EXPLAIN` plus
   Laufzeit). Kein Statement geht ungeprüft in Produktion.

### Umgebungen

| Umgebung | Inhalt | Verwendung |
|---|---|---|
| Testkopie (interner Host, MariaDB 10.6) | Vollkopie der Produktion, Datenstand **08.07.2026** | Entwicklung, Tests, Messung von Abfrageplänen |
| Produktion | Live | Laufzeitdatenquelle der Anwendung |

Eine laufend aktualisierte Replica existiert nicht. Zur Laufzeit wird auf der Produktion gelesen —
daher sind die Regeln 1 bis 6 verbindlich und nicht verhandelbar.

Die Codegenerierung läuft gegen die Testkopie, betrieben wird gegen die Produktion. Ein Test
gleicht die Spalten des generierten Modells gegen `information_schema` der jeweils verbundenen
Datenbank ab und fängt das Auseinanderlaufen.

**Referenzzeitpunkt.** Die Testkopie liegt hinter der realen Uhrzeit zurück — am 27.07.2026 waren
es 19 Tage. Ein Standard-Zeitfenster von 24 Stunden liefert dort null Zeilen und lässt korrekte
Anwendungsteile kaputt aussehen. Der Rückstand wächst täglich und springt bei jeder Neubefüllung,
er wird deshalb ermittelt und nicht eingetragen.

Umsetzung: `LocalDateTime.now()` wird **nirgends** direkt aufgerufen. Stattdessen ein Bean vom Typ
`java.time.Clock` — in Produktion die Systemuhr, im Dev-Profil ein `Clock.offset(...)`, dessen
Versatz beim Start aus `MAX(Message.MessageLastUpdate)` berechnet wird. Die Zeit **läuft weiter**
statt einzufrieren, sonst verhalten sich relative Zeitfenster und Timeout-Berechnungen anders als
in Produktion. Eine ArchUnit-Regel hält das Verbot von `now()` fest, statt es nur aufzuschreiben.

**Ausnahme:** Sicherheitsrelevante Zeit — Sitzungsablauf, Sperrfristen — nutzt niemals diesen
Clock, sondern immer die Systemuhr.

---

## 9. MVP-Umfang

**Enthalten**

- Anmeldung mit Mandantentrennung
- BAM-Suche als Einstiegspunkt ("wo ist mein Lieferschein?")
- Message View: Liste mit Zeitfenster und Filtern, Detailansicht mit Prozessschritten in Klartext
- Verkettung über Split, Merge und Quittung
- Process View: gruppiert nach kuratiertem Partner, Projekt als Filter
- Dashboard: Volumen im Zeitverlauf, die drei Problemkategorien, Verteilung nach Partner
- Download der EDI-Rohdatei über den Backend-Proxy, mit Protokollierung
- Administration: Benutzerverwaltung, Prozess-Katalog mit Massenzuordnung nach Projekt

**Nicht enthalten**

- Service- und Heartbeat-Überwachung (`Service.ServiceStatus`) — Betriebssicht, nicht Kundensicht
- Formatierte Anzeige der Rohdaten im Browser (EDIFACT/VDA/IDOC aufbereitet) — nur Download
- Benachrichtigungen und Alarmierung
- Chatbot

## 10. Geplante Ausbaustufen

1. **Chatbot.** Fragen wie "gab es heute Fehler bei der Übertragung an Partner XY?".
   **Kein rohes Text-to-SQL.** Bei EAV plus Namenskonventionen erfindet jedes Modell plausible
   Zahlen. Stattdessen Werkzeugaufrufe auf dieselben Endpunkte, die auch das Frontend nutzt. Das
   ist der Grund, die API von Anfang an kennzahlenorientiert statt tabellenorientiert zu schneiden.
2. **Alarmierung** bei Fehlern und Überfälligkeit.
3. **Service-Überwachung** für die interne Betreuung.
4. **SOS-Baukasten** — Nutzer stellen sich Abläufe aus `SOSAction`-Bausteinen selbst zusammen.
   Achtung: Das macht aus dem Werkzeug ein Konfigurationssystem mit Schreibzugriff auf das
   Altschema. Ein solches Schreibmodul wird ein eigener, separat berechtigter Baustein — der
   bestehende Lesepfad wird dafür nicht aufgeweicht.

---

## 11. Dokumentierte Annahmen und Risiken

| # | Annahme | Risiko wenn falsch |
|---|---|---|
| ~~A1~~ | **Geklärt.** Testkopie der Produktion vorhanden (Daten bis Ende 2025), zur Laufzeit wird auf der Produktion gelesen | — |
| A2 | Ein Nutzer gehört zu genau einem Mandanten | `app_user_mandant` ist vorbereitet, Aufwand gering |
| A3 | Kein SMTP-Relay verfügbar, Passwort-Reset erfolgt durch Admin | Selbstbedienung fehlt, später nachrüstbar |
| ~~A4~~ | **Geklärt.** Rohdatenzugriff über Filestore-Links ist gewünscht und für alle Mandantennutzer freigegeben | — |
| ~~A5~~ | **Geklärt.** Eigenständiger Betrieb möglich, GlassFish nicht vorgeschrieben (Instanz wäre Version 6/7) | — |
| ~~A6~~ | **Widerlegt 27.07.2026.** `ERROR_*` ist **nicht** die vollständige Fehlerdefinition: `COMMIT_REJECTED` ist ein Fehler ohne Präfix. Siehe 4.1 | — |
| ~~A7~~ | **Bestätigt 27.07.2026.** 1.490 Prozesse | — |
| A8 | Jedes Projekt ist mindestens einem Mandanten zugeordnet | **Offen.** 142 Projekte stehen 134 Zeilen in `ProjectMandant` gegenüber. Nachrichten in Projekten ohne Zuordnung wären für niemanden sichtbar, auch nicht für Admins. Vor Schritt 4 zu klären |
| A9 | Die BAM-Suche lässt sich mit Zeitfenster und hartem Limit ausreichend begrenzen | `MessageBAM` hat keinen Zeitstempel, das Fenster greift erst nach dem Join. Rückfalloption: eigener BAM-Index in `overlord_monitor`, vom Rollup-Job mitgeführt |
| A10 | MariaDB 10.6 bleibt für die Laufzeit des Projekts in Betrieb | Version hat im Juli 2026 den Wartungszeitraum erreicht. Ein Upgrade auf 11.x ändert Standard-Sortierungen — deshalb steht die Sortierung in jeder Migration explizit |

---

## 12. Glossar

| Begriff | Bedeutung |
|---|---|
| **Mandant** | Kunde bzw. Geschäftsbereich, die Sicherheitsgrenze des Systems |
| **Projekt** | Gruppierung von Prozessen; Bedeutung je Mandant unterschiedlich |
| **Prozess** | Ein EDI-Ablauf, meist Partner plus Belegart |
| **SOS** | Sequence of Services — der konkrete, aus Bausteinen gebaute Ablauf |
| **SOSAction** | Ein einzelner Baustein eines SOS |
| **Message** | Eine Übertragung, der zentrale Datensatz |
| **MessageAction** | Ein ausgeführter Schritt einer Übertragung |
| **BAM** | Business Activity Monitoring — fachliche Suchschlüssel wie Lieferschein-Nr. |
| **Interchange** | EDIFACT-Austauschnummer, Grundlage der COMMIT-Zuordnung |
| **COMMIT / EERP** | Empfangsbestätigung des Partners |