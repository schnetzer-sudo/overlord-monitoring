# Overlord Monitoring — Projektbeschreibung

Stand: 01.08.2026 · Diese Datei ist der verbindliche Kontext für alle Arbeiten am Projekt.
Bei Widersprüchen zwischen dieser Datei und einer Annahme im Code gilt diese Datei.

> **Ausnahme, 20.08.2026 — Indizes.** Für die Indexlisten dieser Datei gilt der Satz oben **nicht**.
> Dort entscheidet die Datenbank. Grund: §3.2 führte achtzehn Tage lang drei Indizes auf `Message`,
> während acht existierten und die Messung seit dem 01.08.2026 vorlag — und die falsche Liste ist
> in einen Arbeitsauftrag eingegangen. Ein Test gegen `information_schema.STATISTICS` bewacht die
> Listen künftig **in beide Richtungen**: Er wird auch bei undokumentierten Indizes rot, denn genau
> das war der eingetretene Fall. **Der Test ist am 20.08.2026 entschieden und noch nicht gebaut**
> (E37); bis dahin ist diese Ausnahme eine Absichtserklärung und keine Zusage.

**Korrektur 01.08.2026.** Annahme A1 (Abschnitt 11) nannte „Daten bis Ende 2025" und widersprach
damit dem Datenstand in Abschnitt 8. Messung M0 gegen die Testkopie entscheidet zugunsten von
Abschnitt 8 (**08.07.2026**); A1 ist entsprechend korrigiert und Abschnitt 8 um die Bestätigung
ergänzt. Alle Zahlen dazu in [`messungen-schritt4.md`](messungen-schritt4.md). Sonst nichts geändert.

**Revision 28.07.2026.** Schritt 2 ist umgesetzt und über die CI bestätigt. Geändert haben sich
die Rollenbeschreibung (Abschnitt 2), der Schreibbenutzer (Abschnitt 3.0), die Mandantenliste
(Abschnitt 3.2), die Authentifizierung samt Wegfall der Nutzermigration (Abschnitt 7) sowie die
Annahmen A2, A8 und die neue A11 (Abschnitt 11).

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
| `MANDANT` | Kundenmitarbeiter (extern) | Ausschließlich Daten der ihm zugeordneten Mandanten |
| `ADMIN` | Interne EDI-Betreuung | Alle Mandanten, zusätzlich Benutzer- und Katalogpflege |

Primäre Zielgruppe ist die Rolle `MANDANT`. Externe Nutzer bedeuten: Die Mandantentrennung ist
eine Sicherheitsanforderung, keine Komfortfunktion.

**ADMIN sieht alle Mandanten, aber nacheinander — nicht gleichzeitig** (Entscheidung 28.07.2026).
Es ist immer genau ein Mandant aktiv, für jede Rolle. Der Unterschied zwischen den Rollen liegt
allein darin, welche Mandanten wählbar sind. Damit gibt es keinen Codepfad ohne Mandantenfilter,
die Repository-Signaturen sind für beide Rollen identisch, und der Isolationstest gilt für ADMIN
unverändert. Der Preis: Ansichten sind für ADMIN nur eingeschränkt teilbar, weil der aktive Mandant
in der Session steht und nicht in der URL. Deshalb gehört er sichtbar in die Kopfzeile.

Berechtigung wird als **Menge** geprüft, nicht als Rolle: *Wer für einen Mandanten berechtigt ist,
darf zu ihm wechseln.* ADMIN ist für alle berechtigt und damit kein Sonderfall, sondern der Nutzer
mit der größten Menge.

Annahme A2 (ein Nutzer gehört zu genau einem Mandanten) gilt weiterhin als Ausgangslage, steht
aber erkennbar unter Druck: `NEXANS` und `NXHBE` sind derselbe Konzern, `IBIS` und `IBISGUS`
dieselbe Firma mit anderem Zuschnitt. Das Datenmodell ist über `app_user_mandant` für n:m
vorbereitet, und die mengenbasierte Prüfung fängt den Fall ohne Zusatzbau ab.

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
| `monitor_write` | `SELECT` | `ALL PRIVILEGES` |

Kein Benutzer besitzt irgendein Schreibrecht auf `GlassfishDB`. Host, Benutzernamen und Passwörter
stehen ausschließlich in Umgebungsvariablen, niemals in einer versionierten Datei.

Der Schreibbenutzer hieß ursprünglich `monitor_root`. Umbenannt am 28.07.2026, weil der Name
Allmacht suggerierte, obwohl er nur auf `overlord_monitor` schreiben darf — der nächste Kollege
liest den Namen und nicht die Dokumentation.

MariaDB 10.6 hat seinen Wartungszeitraum im Juli 2026 erreicht. Die Instanz gehört uns nicht, aber
wir lesen dauerhaft darauf — siehe Annahme A10.

**Der Server bietet keine TLS-Verschlüsselung an** (festgestellt 28.07.2026). Der JDBC-Treiber
stört sich nicht daran und verbindet unverschlüsselt, neuere Kommandozeilen-Clients brechen mit
„SSL is required, but the server does not support it" ab und brauchen `--skip-ssl`. Fachlich heißt
das: Der gesamte Verkehr zwischen Anwendung und Datenbank läuft im Klartext über das Netz — nicht
die Passwörter, die überträgt MariaDB im Frage-Antwort-Verfahren, aber jede Nachricht, jeder
BAM-Wert, jede Belegnummer. Siehe Annahme A11.

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
- `MessageTimeout` ist eine **Dauer in Sekunden**, kein Zeitpunkt. Der Timeout-Zeitpunkt wird im
  Backend berechnet. *Korrigiert 01.08.2026:* Hier stand „Dauer in Minuten". Messung M8 widerlegt
  das — `SOSActionTimeout = 1800` steht 37.120-mal neben dem Ablaufschritt `WAIT|30M`, und 1800
  Sekunden sind exakt 30 Minuten. Die Größenordnung ändert sich um Faktor 60; Belegkette in
  [`messungen-schritt4.md`](messungen-schritt4.md) M8.

  > **Präzisiert 03.09.2026: `MessageTimeout` ist der Parameter eines Wächters und keine
  > Anzeigegröße.** Er sagt, wann das Altsystem eine Nachricht in `RUNNING` automatisch auf
  > `ERROR_TIMEOUT` setzt — und **er gilt nur für `RUNNING`.** Auf `SUSPENDED` wird er nicht
  > angewendet; solche Nachrichten warten absichtlich und werden nie automatisch beendet.
  >
  > **Folge im Code:** Das Nachrichtendetail liefert `fristSekunden` bei `WARTEND` als `null`
  > (E‑76) — ein Feld, das eine Frist nennt, die niemand durchsetzt, ist eine falsche Auskunft.
  > `MessageStatusClassifier.TIMEOUT_EINHEIT` trägt die Einheit weiterhin, hat aber seit E‑71
  > **keinen Verbraucher mehr im Anwendungscode**.
  >
  > **Herkunft:** fachliche Auskunft des Auftraggebers vom 03.09.2026. **Nicht gemessen.**
  > Die Testkopie kann sie nicht belegen: `RUNNING` kommt dort null Mal vor, und die 538
  > `SUSPENDED` sind der Bestand *eines* Status in *einer* Gestalt. **Gegen die Produktion zu
  > prüfen** mit der Abfrage in [`message-status.md`](message-status.md), Abschnitt „Die offene
  > Prüfung".
- Nutzbare Indizes — `Message` trägt **acht**:

| Index | Spalten | eindeutig |
|---|---|---|
| `PRIMARY` | `MessageID` | ja |
| `MessageLastUpdateIDX` | `MessageLastUpdate` | nein |
| `MessageLastUpdateProcessMessageIDX` | `MessageLastUpdate, ProcessID, MessageID` | nein |
| `MessageStatusIDX` | `MessageStatus` | nein |
| `Message_ProcessFK` | `ProcessID` | nein |
| `ProejctIDIDX` *(sic)* | `ProcessID` | nein |
| `SourceMessageIDIDX` | `SourceMessageID` | nein |
| `TargetMessageIDIDX` | `TargetMessageID` | nein |

Der Indexname `ProejctIDIDX` ist im Altsystem so geschrieben — Buchstabendreher inbegriffen — und
er steht auf `ProcessID`, nicht auf `ProjectID`. Bestand, kein Tippfehler dieser Datei.

> **Korrigiert 20.08.2026.** Hier standen **drei** Indizes, es sind **acht** — darunter **zwei
> eigenständige auf `ProcessID`** (`Message_ProcessFK` und `ProejctIDIDX`).
>
> Die Messung lag seit **M1 vom 01.08.2026** vor: [`messungen-schritt4.md`](messungen-schritt4.md)
> M1 trägt die Überschrift „Spaltennamen **und Indizes**“ und führt alle acht. **Diese Datei hat
> sie nie übernommen.** Die Liste hier war nie gegen `information_schema` erhoben, sondern
> übernommen; erhoben ist sie in [`messungen-schritt9.md`](messungen-schritt9.md), V2.
>
> **Folge:** Die Nachtragsmessung zu Schritt 9b trug am 20.08.2026 ein Abbruchkriterium, das auf
> dieser überholten Liste beruhte — „es gibt keinen eigenständigen Index auf `ProcessID`“. Es gibt
> zwei. **Die falsche Liste hat einen Arbeitsauftrag falsch gemacht**, und das ist der Grund, warum
> sie hier benannt und nicht stillschweigend ersetzt wird.
>
> Ein Test gegen `information_schema.STATISTICS` bewacht die Liste künftig (E37). Siehe dazu die
> **Ausnahme für Indizes** in der Präambel dieser Datei.

**`MessageAction`** — die einzelnen Prozessschritte einer Nachricht.
PK `(MessageID, MessageActionID)` · **`SOSID`** varchar(36) NOT NULL · **`SOSActionID`** smallint(6)
NOT NULL · `MessageActionStart` / `MessageActionEnd` · `ServiceID` ·
`SOSActionServiceProperties` mediumtext · `SOSActionTimeout`

*Korrigiert 07.08.2026:* Hier standen sieben Spalten, die Tabelle hat **neun** — `SOSID` und
`SOSActionID` fehlten. Diese Liste war nie gegen `information_schema` erhoben, sondern übernommen;
gemessen wurde sie erst in [`messungen-schritt5.md`](messungen-schritt5.md) M14. Die beiden Spalten
tragen die Auflösung des Schrittnamens: Der Join lautet
`SOSAction ON (MessageAction.SOSID, MessageAction.SOSActionID)` und **niemals** über
`Message.SOSID` — M15 belegt das mit 100 % Übereinstimmung des ausgeführten mit dem geplanten
Baustein gegen 96,17 % bei der Fassung über `Message`. Details in
[`datenmodell.md`](datenmodell.md) §3.

`SOSActionServiceProperties` enthält die ausgeführten Bausteine als pipe-getrennte Liste, etwa
`NXS_FILE_CONVERT|E2A|UNWRAP` oder `NXS_MERGE|KE_OSTROV_734973|WAIT|30M|30406_..._MRG`.
Diese Rohwerte werden dem Nutzer **nicht** angezeigt, sondern in lesbare Schritte übersetzt — der
Klartext ist `SOSAction.SOSActionName`, nicht eine handgepflegte Zuordnungstabelle.

**`MessageProperty`** — Schlüssel/Wert-Paare je Nachricht (EAV).
PK `(MessageID, MessagePropertyName, MessageActionID)` · `MessagePropertyValue` mediumtext

**Gezählt am 12.08.2026: 75.571.462 Zeilen**, 61,0 GB. Rund **23** Zeilen je Nachricht
(75.571.462 / 3.341.519 = 22,62) und durchschnittlich **808 Byte** je Zeile. Belegt in
[`messungen-schritt7.md`](messungen-schritt7.md) M44.

**Diese eine Tabelle ist 82 Prozent der Datenbank** — die Bytegröße ist hier die maßgebliche
Kennzahl, nicht die Zeilenzahl. *Dieser Satz rechnet mit **Bytes** und ist von jeder Korrektur der
Zeilenzahl unberührt; er ist beim Nachziehen nicht zu ändern.*

*Korrigiert 12.08.2026:* Hier stand „**Gemessen** am 27.07.2026: 46.964.279 Zeilen, 61 GB. Rund
**vierzehn** Zeilen je Nachricht und durchschnittlich **1,3 Kilobyte** je Zeile." Gemessen war davon
nur die Bytegröße. Die Zeilenzahl war die Schätzung `information_schema.TABLE_ROWS` und lag
**60,9 % zu niedrig**; die beiden Kennzahlen dahinter sind aus ihr gerechnet und ändern sich
entsprechend mit. **Die 61 GB bleiben** — sie stammen aus belegten Seiten und sind über fünf
Messtage byteidentisch. Vollständig im Kasten in Abschnitt 8.

> **Die „rund vierzehn" waren nie ein Zeitraumeffekt.** `datenmodell.md` §3 und
> [`annahmen-korrekturen.md`](annahmen-korrekturen.md) erklärten die Lücke zwischen den vierzehn hier
> und den in `messungen-schritt5.md` M17 gemessenen **22,57** im dichten Bestand seit dem 07.08.2026
> mit **verschiedenen Nennern**. Die Erklärung war plausibel und ist mit M44 gegenstandslos: Aus der
> gezählten Zahl folgen **22,62** über den Gesamtbestand, und das liegt zwischen den beiden direkt
> gemessenen Werten 22,57 (Tag) und 22,88 (Monat). Es gab keinen Dichteeffekt, es gab eine falsche
> Zahl — und eine plausible Erklärung, die sie fünf Tage lang zugedeckt hat.

**Zugriff ausschließlich über `MessageID`.** Niemals filtern, gruppieren oder sortieren über
`MessagePropertyValue` — die Indizes darauf sind Präfix-Indizes über 50 Zeichen und für
Aggregation ungeeignet. Jede Abfrage, die anders als über `MessageID` einsteigt, wälzt ein
Vielfaches dessen um, was `Message` insgesamt groß ist.

Bekannte Namen: `Message.GUID`, `Message.SendingPartner`, `Message.SNDPRN`, `Message.VFN`,
`Message.SOS`, `Message.SplitCount`, `Message.Payload.GUID` (Format `<Ablagenkennung>|<UUID>`,
etwa `FILESTOREPROD09|d95499ff-...`), `Message.InterchangeNumber`,
`Message.CommitInterchangeNumber`, `Message.SourceMessageID` — **und, ergänzt am 08.09.2026,
`Message.DestinationFilename`.**

> **Ergänzt 08.09.2026 — der elfte Name, und woher er kommt.** `Message.DestinationFilename` ist
> seit dem Handabgleich der Konfigurationstabelle `MessagePropertySearchListEntry` mit der
> Produktion am 08.09.2026 als Suchfeld für `NEXANS` konfiguriert (M161) und in M162 bis M167
> gemessen ([`messungen-property-suche.md`](messungen-property-suche.md), Nachtrag; offener Punkt
> 151 dort). Gemessen ist: In Fenster B trägt er **20.765** Zeilen auf 20.765 Nachrichten mit
> 3 bis 73 Zeichen Länge (M56); bei `NEXANS` deckt er **14,518 %** der Wurzeln (M162), 27 Werte
> tragen 80,35 % seiner Zeilen (M163). **Was der Name fachlich bedeutet, steht in keiner
> Projektdatei** — er wird angeboten, weil er konfiguriert ist, und nicht, weil jemand ihn
> versteht (E‑105). Dieselbe Ergänzung steht in [`datenmodell.md`](datenmodell.md) §3.

> **Gekennzeichnet 19.08.2026 — `Message.Payload.GUID` ist nicht die eingegangene Datei.** Der Name
> legt genau das nahe: `Message.` plus `Payload` liest sich wie „die Nutzdatei *der* Nachricht".
> **Gemessen ist das Gegenteil** — er trägt in **6.249 von 6.249** Nachrichten (Fenster A) und
> **214.330 von 214.330** (Fenster B) denselben Verweis wie die Nutzdatenzeile mit dem **höchsten
> `MessageActionID`** derselben Nachricht, ohne einen Gegenfall
> ([`messungen-schritt8.md`](messungen-schritt8.md) M73, Befund 2).
>
> *Ausdrücklich nicht behauptet:* dass „höchster `MessageActionID`" gleichbedeutend mit „zeitlich
> zuletzt" ist. Gemessen ist die Schrittnummer, nicht die Uhr.
>
> Der Zusatz steht hier, weil dies die einzige lebende Stelle dieser Datei ist, an der der Name
> überhaupt noch vorkommt — und weil genau der Fehlschluss aus ihm am 18.08.2026 in zwei
> Feature-Dateien als Tatsache gelandet ist. **Der Rohdatenzugriff führt nicht über diesen Namen**
> (§7); die Artefaktliste führt ihn seit dem 19.08.2026 nicht mehr.
>
> Die Formatangabe ist zugleich präzisiert: Hier stand bis heute „**Format
> `FILESTOREPROD09|<uuid>`**" — das ist eine *konkrete* Ablagenkennung als Format ausgegeben,
> während §7 mehrere Ablagen führt und Kreuzabrufe ausdrücklich scheitern (M68).

**`MessageBAM`** — fachliche Suchschlüssel (Business Activity Monitoring).
PK `(MessageID, MessageBAMType, MessageBAMValue)` · `MessageBAMValue` varchar(70), eigener Index

Das ist die zentrale Suchdimension für Fachanwender: Lieferschein-Nr., Bestellnummer,
Transport-Nummer, Charge, Werk, Materialnummer und so weiter. `MessageBAMType` verweist auf
`MessageBAMType.MessageBAMTypeDescription`.

**Gezählt am 11.08.2026: 15.406.350 Zeilen**, 7,1 GB, rund **fünf** Einträge je Nachricht
(15.406.350 / 3.341.519 = 4,61). Belegt in [`messungen-schritt7.md`](messungen-schritt7.md) M33‑0.

*Korrigiert 12.08.2026:* Hier stand „Gemessen am 27.07.2026: 10.859.666 Zeilen, 7,1 GB, rund **drei**
Einträge je Nachricht". Die Zeilenzahl war nie gemessen, sondern die Stichprobenschätzung
`information_schema.TABLE_ROWS` — und sie lag **41,9 % zu niedrig**. **Die 7,1 GB bleiben:** Sie
stammen aus belegten Seiten und nicht aus einer Stichprobe. Vollständig im Kasten in Abschnitt 8.

**`MessageBAM` hat keinen Zeitstempel.** Das Pflicht-Zeitfenster aus Abschnitt 8 kann deshalb erst
**nach** dem Join auf `Message` greifen, also nach dem teuren Teil. Ein `LIMIT` vor dem Join hilft
nicht, weil es die falschen Zeilen abschneidet: die ersten nach Indexreihenfolge, nicht die
neuesten. Bei häufigen Werten — die Abladestelle `050` ist ein solcher — liefert der Index auf
`MessageBAMValue` potenziell hunderttausende Treffer. Siehe Annahme A9.

**`MessageBAMMandant`** — steuert je Mandant, welche BAM-Typen sichtbar sind und in welcher
Reihenfolge (`MessageBAMTypeSortIndex`). Diese Konfiguration wird für die Spaltenauswahl und die
Suchfelder übernommen, nicht neu erfunden.

**`Process` / `Project` / `Mandant` / `ProjectMandant`** — die Hierarchie. Reine Stammdaten.

Die zehn Mandanten, erhoben am 28.07.2026. Die `MandantID` ist ein lesbarer Code, keine UUID:

| ID | Name | |
|---|---|---|
| `EDITIONLINGERI` | Edition Lingerie GmbH | |
| `IBIS` | IBIS GmbH | gehört mit `IBISGUS` zusammen |
| `IBISGUS` | IBIS GmbH mit GUS WW | |
| `NEXANS` | Nexans autoelectric GmbH | trägt praktisch das gesamte Nachrichtenaufkommen |
| `NXHBE` | Nexans Belgien | gehört mit `NEXANS` zusammen |
| `SUTTONS` | Suttons Group | |
| `SYSTEM` | Systemmandant | **technisch, kein Kunde** |
| `VOTG` | VOTG Tanktainer GmbH | |
| `WOC` | Without Contract | **technisch, kein Kunde** — Auffangbecken für Verkehr ohne hinterlegten Vertrag |
| `ZAST` | Zast GmbH | |

`SYSTEM` und `WOC` gehören nicht in eine Auswahl, die wie eine Kundenliste aussieht. `WOC` ist
vermutlich das Gegenstück zum Auffangprozess `00001_Undefined` und braucht im Dashboard eine
gesonderte Behandlung, sonst steht ein Auffangbecken zwischen echten Kunden.

> **Entschieden am 24.08.2026 (E‑f), eingetragen am 27.08.2026: keine gesonderte Behandlung.**
> `SYSTEM` und `WOC` werden **behandelt wie jeder andere Mandant** — keine Aussiebung aus der
> Auswahl, kein Sonderpfad im Dashboard, keine eigene Darstellung.
>
> **Die Begründung ist eine über die Zielgruppe und nicht über die Daten.** Beide sind technische
> Mandanten, denen **nie ein externes Konto zugeordnet wird**. Wer sie überhaupt zu sehen bekommt,
> ist damit ausschließlich `ADMIN` — die interne EDI-Betreuung, die weiß, was ein Auffangbecken ist.
> Ein Sonderpfad hätte eine Zielgruppe von einer Person, und er hätte dieselbe Fläche ein zweites
> Mal zu pflegen. Der Absatz oben löst ein Problem, das die Rollenaufteilung schon gelöst hat.
>
> **Konsequent ist das auch anderswo:** Die Mandantenauswahl siebt beide seit Schritt 3 nicht aus,
> und die Benutzerverwaltung tut es aus demselben Grund nicht
> ([`benutzerverwaltung-frontend.md`](benutzerverwaltung-frontend.md)).
>
> **Die Zahlen, die daneben gehören** — M91 vom 26.08.2026
> ([`messungen-schritt10.md`](messungen-schritt10.md)): `WOC` trägt **2.529** Nachrichten auf
> **zwei** Prozessen, von denen einer **81,77 %** hält; `SYSTEM` trägt **151** auf **einem**
> Prozess, also 0,0045 % des Bestands. E‑f bleibt davon unberührt — es ist eine Entscheidung über
> **Gleichbehandlung, nicht über Inhalt**. Aber wer 10b an diesen beiden Mandanten abnimmt, sieht
> ein fast leeres Dashboard, und das ist dann kein Fehler. Vollständig als offener Punkt 47 in
> [`messungen-schritt10.md`](messungen-schritt10.md), zusammen mit `NXHBE` (neun Nachrichten) und
> `EDITIONLINGERI` (keine einzige).
>
> **Der Absatz oben bleibt stehen.** Er war die Frage, die zu E‑f geführt hat, und ohne ihn wäre
> nicht mehr erkennbar, dass die Gleichbehandlung eine Entscheidung ist und keine Unterlassung.

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
| `MatchInterchange` | ⚠️ **„stündlich" — ungedeckt** (M31‑3, 11.08.2026) | ordnet COMMITs über Interchange-Nummern zu, setzt `MessageStatus = 'COMMIT_RECEIVED'` und `SourceMessageID` |
| `SetTargetFlag` | — | setzt `Target`-Flag |
| `MoveDTNA997` | stündlich | verschiebt Nachrichten zwischen zwei Prozessen (kundenspezifisch) |

Konsequenz: Die COMMIT-Zuordnung ist bis zu eine Stunde verzögert. Das muss in der Oberfläche
kommuniziert werden, sonst wirkt eine korrekt übertragene Nachricht wie unquittiert. ⚠️ **Die
Angabe „eine Stunde" hängt am Takt und ist damit ebenso ungedeckt** — siehe den Kasten darunter.

> ### ⚠️ Der Takt von `MatchInterchange` ist ungedeckt *(gekennzeichnet am 11.08.2026)*
>
> **Bis heute stand „stündlich" in dieser Datei als Tatsache — und diese Datei ist die verbindliche
> Wahrheit dieses Projekts.** Eine Quelle für die Angabe ist nirgends verzeichnet. M31‑3
> ([`messungen-schritt6.md`](messungen-schritt6.md)) hat versucht, sie von der Testkopie aus zu
> belegen. **Ergebnis: nicht belegt.**
>
> | Weg | Ergebnis |
> |---|---|
> | `information_schema.EVENTS` | **leer** — nachweislich ein Rechteartefakt und kein Befund über den Bestand |
> | `SHOW EVENTS FROM GlassfishDB` | `ERROR 1044 … Access denied for user 'monitor_read'` — **die Verweigerung ist die bessere Auskunft:** Sie belegt, dass die leere Antwort oben nichts über die Existenz von Events sagt |
> | `@@global.event_scheduler` | **`ON`** — Events *können* auf dieser Instanz laufen, wir sehen sie nur nicht |
> | Wirkung: `MessageLastUpdate` über die Minute der Stunde (`COMMIT_RECEIVED`, n = 12.654, 408 Tage) | **jede** der 60 Minuten belegt, kein Wert hebt sich ab. Bei einem stündlichen Event stünden rund 12.000 der 12.654 Zeilen auf einer oder zwei Minuten |
> | volumengleicher Ballungsvergleich über alle Status | `COMMIT_RECEIVED` **0,597** gegen `MERGED` **0,601** — ein Status, den **kein** Event schreibt. Die Ballung ist die des EDI-Verkehrs und nicht die einer Uhr |
>
> > **Belegvermerk** (Regel L10).
> > *Gemessen:* die Verteilung von `MessageLastUpdate` über die Minute der Stunde, für
> > `COMMIT_RECEIVED` über den ganzen Bestand und für sechs Kontrollstatus bei gleichem Aufkommen.
> > *Behauptet wird:* dass der Takt **von hier aus nicht sichtbar** ist.
> > **Die Lücke, und sie ist grundsätzlich:** `MessageLastUpdate` ist der Zeitpunkt der **letzten**
> > Änderung, nicht der des Event-Schreibzugriffs. Schreibt danach noch etwas auf die Zeile,
> > verwischt die Ballung. **Die Messung kann den Takt belegen, wenn sie ihn zeigt — aber nicht
> > ausschließen, wenn sie ihn nicht zeigt.** Sie zeigt ihn nicht. Der Satz „`MatchInterchange`
> > läuft nicht stündlich" ist damit **nicht** gemessen und wird hier auch nicht behauptet.
>
> **Was daraus folgt.** „Stündlich" wird **nicht gestrichen** — es kann stimmen, und die Messung
> kann es nicht widerlegen. Es ist als **ungedeckt gekennzeichnet**: Die Quelle liegt außerhalb der
> Testkopie, beim Altsystem, dem das Event gehört, und dort ist sie zu holen.
>
> **Die Konsequenz oben steht auf demselben Boden.** Dass die COMMIT-Zuordnung verzögert ist, ist
> unstrittig; **wie lange**, ist es nicht. Ein Satz in der Oberfläche wie „kann bis zu eine Stunde
> dauern" wäre nach Regel Q4 geraten und ist deshalb **nicht gebaut** — er steht als offener Punkt
> in [`verkettung.md`](verkettung.md) §11.
>
> **Warum das nicht als Randnotiz endet:** In der verbindlichen Datei stand ein Satz, der eine
> Angabe ohne Herkunft als Tatsache ausgibt. Das ist der wichtigere Befund von M31‑3 — wichtiger
> als die Frage, wie lange das Event nachläuft.

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
| `MERGED` | 607.277 | **zusammengeführt** (`ZUSAMMENGEFUEHRT`), Verkettung beachten | neutral |
| `SPLITTED` | 310.263 | **aufgeteilt** (`AUFGETEILT`), Verkettung beachten | neutral |
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

> **Ergänzt 03.09.2026: Auf `RUNNING` wirkt ein Wächter des Altsystems.** Eine Nachricht, die
> länger als `MessageTimeout` in diesem Status steht, wird automatisch auf `ERROR_TIMEOUT` gesetzt.
> Der Status ist damit nicht nur flüchtig, sondern **nach oben begrenzt** — und das ist der Grund,
> warum die Problemkategorie *Überfällig* mit E‑71 gefallen ist (§4.2, Punkt 2).
>
> **Herkunft:** fachliche Auskunft des Auftraggebers vom 03.09.2026. **Nicht gemessen.**
> Die Testkopie kann sie nicht belegen: `RUNNING` kommt dort null Mal vor, und die 538
> `SUSPENDED` sind der Bestand *eines* Status in *einer* Gestalt. **Gegen die Produktion zu
> prüfen** mit der Abfrage in [`message-status.md`](message-status.md), Abschnitt „Die offene
> Prüfung".

**`CHECKED`, `CKECKED` und `COMMIT_SENT` gelten als bekannt, aber fachlich ungeklärt.** Sie werden
neutral behandelt und mit Rohwert plus dem Hinweis "Bedeutung nicht verifiziert" angezeigt. Es
wird nichts geraten.

**`SPLITTED` und `MERGED` sind zwei Einordnungen und nicht eine** (entschieden am 11.08.2026,
Schritt 6, Teil 2a). Bis dahin trugen beide dieselbe: `ZWISCHENSCHRITT`. Technisch waren sie
austauschbar, fachlich bedeuten sie Gegenteiliges — **aus eins wurde viel** gegen **aus viel wurde
eins**. Die Verkettung macht genau diesen Unterschied sichtbar (§4.3): `MERGED` ist der *Eingang*
einer Zusammenführung, `SPLITTED` die *Wurzel* einer Aufteilung. Ein gemeinsamer Eimer verschluckte
ihn. **An der Überfälligkeitsrechnung ändert das nichts** — beide bleiben Endstatus (§4.2).

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

   > ### ⚠️ Widerlegt und aus dem MVP genommen *(Entscheidung E‑71 vom 03.09.2026)*
   >
   > **Der Text oben bleibt vollständig stehen.** Er ist die Vorarbeit, gegen die die Widerlegung
   > zu lesen ist — und er ist der Grund, warum die Kategorie überhaupt gebaut worden ist. **Gebaut
   > ist davon nichts mehr:** `MessageStatusClassifier.istUeberfaellig`, `timeoutZeitpunkt` und
   > `ueberfaelligBedingung` sind entfallen, der Listenparameter `ueberfaellig` ebenso, das Feld
   > `ueberfaellig` im Nachrichtendetail ebenso, und das Dashboard zeigt statt der Kachel
   > *Überfällig* die beiden Kacheln *Läuft* und *Wartend*.
   >
   > **Die Regel, die sie widerlegt** — fachliche Auskunft des Auftraggebers vom 03.09.2026:
   >
   > 1. Eine Nachricht, die länger als `MessageTimeout` in `RUNNING` steht, wird vom Altsystem
   >    automatisch auf `ERROR_TIMEOUT` gesetzt.
   > 2. `SUSPENDED`-Nachrichten warten **absichtlich** — auf einen Folgeprozess, etwa auf den
   >    Versand zu einem bestimmten Zeitpunkt. Sie werden nie automatisch beendet und sind **nicht
   >    überfällig**.
   > 3. In der Praxis liegen sie höchstens **rund eine Woche**.
   >
   > **Herkunft:** fachliche Auskunft des Auftraggebers vom 03.09.2026. **Nicht gemessen.**
   > Die Testkopie kann sie nicht belegen: `RUNNING` kommt dort null Mal vor, und die 538
   > `SUSPENDED` sind der Bestand *eines* Status in *einer* Gestalt. **Gegen die Produktion zu
   > prüfen** mit der Abfrage in [`message-status.md`](message-status.md), Abschnitt „Die offene
   > Prüfung".
   >
   > **Was daraus folgt.** Offen sind genau zwei Statuswerte, und beide fallen weg: `SUSPENDED`
   > wartet absichtlich und ist damit ein **Fehlalarm per Definition**; `RUNNING` steht über der
   > Frist nur im Spalt zwischen Fristablauf und dem Zuschlagen des Wächters — danach heißt es
   > `ERROR_TIMEOUT` und gehört in die **Fehlerkachel**.
   >
   > | | |
   > |---|---:|
   > | von `istUeberfaellig` im Gesamtbestand markiert | **538** |
   > | davon `SUSPENDED` | **538** |
   > | davon `RUNNING` | **0** |
   > | **wahre Treffer** | **0** |
   >
   > **538 Fehlalarme und kein einziger Treffer** — nicht die Mehrheit, alle. Die Rechnung, die das
   > Werkzeug anstellte, stellt das Altsystem bereits an; sein Ergebnis heißt `ERROR_TIMEOUT` und
   > steht bei uns namentlich aufgeschlüsselt in der Fehlerkachel. **Eine zweite Fassung derselben
   > Auskunft, mit anderer Uhr und ohne Schreibrecht, ist keine zusätzliche Aussage.**
   >
   > **Was an ihre Stelle tritt.** Zwei Kacheln, die keine Frist behaupten, sondern einen Zustand
   > zählen: *Läuft* (`RUNNING`) und *Wartend* (`SUSPENDED`), je mit dem Alter der ältesten Zeile
   > ([`dashboard.md`](dashboard.md) §5). **Sie beantworten die Frage nicht, die *Überfällig*
   > beantworten sollte** — *hängt hier etwas zu lange* —, und das ist benannt: Eine Schwelle dafür
   > steht nirgends in den Daten, und sie zu erfinden verbietet Regel Q4. Offener Punkt **130**.
   >
   > **Die Überschrift „Die drei Problemkategorien" bleibt**, nach demselben Muster wie bei E‑d.
   > Fachlich sind es drei; **im MVP ist es seit heute eine.**

   > **Diese Kategorie entsteht an genau einer Stelle im Code:**
   > `common/MessageStatusClassifier.istUeberfaellig(status, messageLastUpdate, messageTimeout,
   > jetzt)` *(seit 10.08.2026 in Gebrauch, Schritt 5)*. Sie ist von außen aufrufbar, damit das
   > Dashboard sie **ruft** statt sie dort nachzubauen — dieselbe Bauform wie die Statuseinordnung
   > selbst. Sie prüft beide Bedingungen zusammen und benutzt für die erste `istEndstatus`, dem
   > [`message-status.md`](message-status.md) diese Methode ausdrücklich zuweist.
   >
   > `jetzt` zieht der Aufrufer aus der **Anwendungsuhr** (Regel Z1) — niemals aus der Systemuhr und
   > niemals aus dem Browser. Erste Verwendung und vollständige Begründung in
   > [`nachrichtendetail.md`](nachrichtendetail.md) §3a.
   >
   > **In der Anzeige trägt „überfällig" keine Farbe.** Rot gehört ausschließlich der Kategorie
   > *Fehler*; würden beide rot, verschmölzen sie in der Wahrnehmung, obwohl der Code sie trennt.
   > Eine eigene Farbrolle ist offen und in [`visuelles-konzept.md`](visuelles-konzept.md) §7 einer
   > späteren Entscheidung vorbehalten.
   >
   > **Berichtigt 27.08.2026 — der Verweis geht in den falschen Abschnitt, und der Termin ist
   > inzwischen benannt.** Der richtige Abschnitt ist **§7a („Offene Punkte")**; §7 heißt „Was
   > bewusst fehlt" und führt die Farbrolle nur als Aufzählungszeile. Die ausgeschriebene Fassung —
   > Rahmen (orange, Ton höchstens 85), Umfang (drei Werte), nachzurechnender Textkontrast — steht
   > in §7a. **Und „einer späteren Entscheidung vorbehalten" ist zu unbestimmt geworden:** §7a nennt
   > den Zeitpunkt seit dem 10.08.2026 selbst — **spätestens beim Dashboard und nicht später**, denn
   > dort stehen die Problemkategorien nebeneinander, und eine ohne Farbe neben einer roten ist
   > keine neutrale Darstellung, sondern eine leisere. Das Dashboard ist nach der Teilung vom
   > 24.08.2026 **Schritt 10b**. Der Satz oben bleibt stehen; er ist der Grund, warum die
   > Entscheidung überhaupt aufgeschoben werden konnte.
3. **Unquittiert** — ausgehende Nachricht ohne zugeordnete Empfangsbestätigung.
   **`COMMIT_REJECTED` gehört ausdrücklich nicht hierher.** Das ist eine Quittung, nur eine
   negative. Sonst erscheint derselbe Beleg in zwei Kacheln und die Zahlen wirken erfunden.

   > ### ⚠️ Nicht im MVP *(Entscheidung E‑d vom 24.08.2026, eingetragen am 27.08.2026)*
   >
   > **„Unquittiert" ist aus dem MVP genommen.** Die Kategorie wird in Schritt 10b nicht gebaut,
   > nicht gezählt und nicht angezeigt.
   >
   > **Zwei Gründe, und der zweite ist der härtere.**
   >
   > 1. **Sie hat in keiner Datei eine operative Definition.** Die drei Zeilen oben sagen, was
   >    *nicht* dazugehört (`COMMIT_REJECTED`), und nennen keine Bedingung, die sich in ein
   >    Statement schreiben ließe: kein Statuswert, keine Frist, keine Methode. Die beiden anderen
   >    Kategorien haben je eine benannte Stelle im Code — `MessageStatusClassifier.fehlerBedingung`
   >    und `…istUeberfaellig`. Diese hat keine. Eine Kachel danach zu bauen hieße, die Definition
   >    beim Bauen zu erfinden, und das verbietet Regel Q4.
   > 2. **Sie hängt am Takt von `MatchInterchange`** — dem Event, das die COMMIT-Zuordnung
   >    nachträgt. Dessen Takt ist seit **M31‑3 (11.08.2026) ungedeckt**, siehe den Kasten in
   >    Abschnitt 3.3. Eine Nachricht sieht so lange unquittiert aus, wie das Event noch nicht
   >    gelaufen ist. Ohne den Takt ist nicht sagbar, ab wann „noch keine Quittung" etwas anderes
   >    bedeutet als „das Event ist noch nicht durch" — und eine Kachel, die Verzögerung als Problem
   >    ausweist, ist schlimmer als keine.
   >
   > **Gemessen ist dazu nichts, und das ist Absicht.** Die Messrunde vom 26.08.2026 führt es
   > ausdrücklich unter „Was diese Runde nicht zeigt", Punkt 5: *„Eine Messung ohne Definition wäre
   > geraten."*
   >
   > **Der Text oben bleibt vollständig stehen** — er ist die Vorarbeit für den Tag, an dem die
   > Kategorie gebaut wird, und die Abgrenzung gegen `COMMIT_REJECTED` gilt unverändert. **Auch die
   > Überschrift „Die drei Problemkategorien" bleibt.** Im MVP sind es zwei; drei bleiben es
   > fachlich. Eine Überschrift auf „die zwei" zu ändern verlöre genau die Auskunft, dass eine
   > dritte existiert und wo sie beschrieben ist.
   >
   > **Für die Farbrolle heißt das:** Die Aufschiebung in
   > [`visuelles-konzept.md`](visuelles-konzept.md) §7a betrifft in 10b nur noch *Überfällig*.
   > „Unquittiert" braucht bis auf Weiteres keine.

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

**Entscheidung: Es wird nicht geparst, es wird kuratiert.** Partner und Richtung sind Felder im
Prozess-Katalog (Abschnitt 5). Eine Heuristik befüllt vor, die Wahrheit
steht im Katalog. Nicht zugeordnete Prozesse erscheinen in Auswertungen sichtbar als
"nicht zugeordnet" — niemals als geratener Wert.

*Korrigiert 20.08.2026:* Hier standen **vier** Felder — „Partner, Standort, Richtung und Belegart“.
Standort und Belegart entfallen; im MVP liest sie nichts. Abnehmer sind allein die Verteilung nach
Partner, die nach Partner und Richtung, und die nach Partner gruppierte Prozessansicht
(Abschnitt 9).

Sonderfall: `00001_Undefined` ist ein Auffangprozess und wird in der Oberfläche gesondert behandelt.

---

## 5. Eigenes Schema `overlord_monitor`

Liegt auf derselben MariaDB-Instanz, wird per Flyway verwaltet. Eigener DB-Benutzer mit
Schreibrechten **ausschließlich** auf dieses Schema.

| Tabelle | Zweck |
|---|---|
| `app_user` | Benutzer, Passwort-Hash, Mandant, Rolle, Sperrzustand |
| `app_user_mandant` | n:m-Zuordnung (vorbereitet, MVP nutzt 1:1) |
| `process_catalog` | je `ProcessID`: Partner, Richtung, Pflegestatus |
| `message_rollup` | stündliche Aggregate je Mandant, Prozess, Partner, Richtung, Status |
| `audit_log` | Anmeldungen, Fehlversuche, Passwortänderungen, Katalogänderungen |
| `saved_view` | gespeicherte Filter je Nutzer |

*Korrigiert 20.08.2026:* Die Tabelle `partner` war hier geführt und entfällt. Ohne eigene
Pflegeoberfläche und ohne Attribute über den Namen hinaus liefert sie nichts, was
`SELECT DISTINCT` über die Katalogzeilen des aktiven Mandanten nicht auch liefert. Die
Auswahlliste wird abgeleitet. `process_catalog` führte in derselben Zeile „Standort“ und
„Belegart“ — beide entfallen, Begründung in Abschnitt 4.4.

> ### ⚠️ Der Schlüssel von `message_rollup` ist ein anderer *(berichtigt am 27.08.2026)*
>
> **Die Zeile oben ist überholt.** Sie führt `message_rollup` als „stündliche Aggregate je
> **Mandant, Prozess, Partner, Richtung, Status**". Entscheidung **E‑a** der Sparringsrunde vom
> **24.08.2026** hebt das auf: Der Schlüssel ist **`(stunde, process_id, message_status)`**.
> **Mandant, Partner und Richtung stehen nicht in der Zeile.** Sie kommen zur **Lesezeit** dazu —
> der Mandant über `Process → ProjectMandant`, Partner und Richtung über `process_catalog`.
>
> **Der Grund ist nicht Sparsamkeit, sondern Rückwirkung.** Stünde der kuratierte Partner in der
> Rollup-Zeile, wäre er dort eingefroren: Jede Katalogkorrektur wirkte erst ab der nächsten
> Aggregation, und die Vergangenheit bliebe für immer falsch beschriftet. Gejoint wirkt dieselbe
> Korrektur rückwirkend über den ganzen Bestand. Bei einem Katalog, der zu sieben von zehn
> Mandanten heute noch keine einzige gepflegte Zeile trägt (M91), ist das der Unterschied zwischen
> „später richtig" und „später richtig, auch für gestern".
>
> **Gemessen — M89, 26.08.2026** ([`messungen-schritt10.md`](messungen-schritt10.md)): Die
> Leseabfrage des Dashboards kostet im Standardfenster (48 Stunden, `NEXANS`) **11,299 ms**, und
> zwar **mit** dem Katalog-Join. Das sind **2,3 %** des 500‑ms‑Budgets aus Abschnitt 8; keine der
> zehn gemessenen Fassungen kommt über 400 ms. Ein Mandant im Schlüssel hätte etwas gespart und
> sich trotzdem nicht gelohnt.
>
> **Gebaut ist es seit dem 26.08.2026 so** ([`rollup.md`](rollup.md) §2). **Die Zeile oben bleibt
> stehen**, weil sonst nicht mehr erkennbar wäre, dass die verbindliche Datei den Rollup einen Tag
> lang falsch beschrieben hat — und zwar während 10a bereits nach E‑a gebaut wurde. Der Widerspruch
> war seit dem 26.08.2026 als offener Punkt 51 in [`rollup.md`](rollup.md) §13 notiert; **die
> Ausführung ist diese Korrekturrunde.** Dieselbe falsche Angabe steht im
> [`IMPLEMENTIERUNGSPLAN_MVP.md`](IMPLEMENTIERUNGSPLAN_MVP.md), Schritt 10 — dort ebenso berichtigt.

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
├─ payload/     Rohdaten und Protokolle: Artefaktliste, Anzeige, Download
├─ catalog/     process_catalog
├─ rollup/      per Profil separat startbar
├─ dashboard/
├─ admin/
└─ jooq/        generiert, nicht handgepflegt
```

Fachpakete kennen einander nicht. Gemeinsames liegt in `common`, nicht in einem Nachbarmodul.

**Korrektur 19.08.2026 zur Beschreibung von `payload`.** Hier stand wortgleich
`├─ payload/     Download-Proxy`. Das beschrieb weder die **Protokolle** noch die **Anzeige**, und ein
Proxy ist es nicht: Die Ablage spricht SOAP und liefert ein ZIP, das Backend liest, entpackt und gibt
neu aus ([`rohdaten.md`](rohdaten.md) §4). Berichtigt ist allein die **Beschreibung**.

> **Der Paketname `payload` bleibt unverändert.** Dass er nicht mehr passt, ist bekannt und als
> offener Punkt geführt — [`rohdaten-backend.md`](rohdaten-backend.md) §11, Punkt 2, mit dem Vorschlag
> `rohdaten`. Eine Umbenennung ist Code und berührt zusätzlich diesen Abschnitt,
> [`IMPLEMENTIERUNGSPLAN_MVP.md`](IMPLEMENTIERUNGSPLAN_MVP.md) und `PaketstrukturTest`; sie gehört
> nicht in denselben Diff wie eine Dokumentationskorrektur. Der Punkt bleibt **offen**.

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
mit `HttpOnly`, `SameSite=Lax` und `Secure`. Kein JWT — bei externen Nutzern wiegt sofortige
Rücknehmbarkeit schwerer als Zustandslosigkeit.

`Secure` ist **per Profil schaltbar** und im Profil `dev` aus. Ohne diese Schaltbarkeit
funktioniert die lokale Entwicklung über `http://localhost` nicht. Ein Test belegt, dass das
Attribut außerhalb von `dev` gesetzt ist.

Sperre nach fünf Fehlversuchen für 15 Minuten, zusätzlich Begrenzung pro IP. Dabei gilt:

- Die **Nutzersperre ist persistent** in `app_user`. Die **IP-Begrenzung liegt ausschließlich im
  Arbeitsspeicher** und schreibt niemals in die Datenbank — sonst wäre der Schutzmechanismus selbst
  der Angriffsvektor, weil jeder Bot Schreiblast erzeugt.
- Fehlermeldungen bleiben unspezifisch. **Die gleichlautende Meldung allein reicht aber nicht:**
  Bei unbekanntem Benutzernamen wird trotzdem ein BCrypt-Vergleich gegen einen festen Dummy-Hash
  gerechnet. Ohne das antwortet der unbekannte Fall in zwei statt zweihundert Millisekunden und
  Benutzernamen lassen sich über die Laufzeit durchprobieren.
- **Eine Ausnahme von der Unspezifik:** War das Passwort korrekt und das Konto gesperrt oder
  deaktiviert, darf das benannt werden. Wer das Passwort kennt, erfährt nichts Neues — wer es nicht
  kennt, bekommt weiter die unspezifische Meldung. Sonst rennt ein berechtigter Nutzer fünfzehn
  Minuten gegen eine Wand, ohne den Grund zu erfahren.

Sicherheitsrelevante Zeit — Sperrfristen, Sitzungsablauf — rechnet mit der **Systemuhr**, niemals
mit dem `Clock`-Bean aus Abschnitt 8.

**Es findet keine Migration der Alt-Benutzertabelle statt** (Entscheidung 28.07.2026). Ursprünglich
war vorgesehen, Benutzername, Mandant und Rolle zu übernehmen. Dagegen sprechen drei Dinge: Die
Tabelle `GlassfishDB.User` bleibt dauerhaft lesbar, eine Übernahme wäre also nur ein vorgezogener
`SELECT` und keine einmalige Gelegenheit. Alle 36 Konten wären gesperrt und ohne Passwort gestartet
und hätten einzeln freigeschaltet werden müssen — derselbe Aufwand wie neu anlegen, nur mit toten
Zeilen als Zwischenschritt. Und die Rolle `Admin` bedeutet dort etwas anderes als bei uns: Im
Altsystem trägt jeder Nutzer eine `MandantID`, auch die zehn Admins, die Rolle war also innerhalb
eines Mandanten gedacht. Eine wörtliche Übernahme hätte zehn überwiegend externe Konten quer über
genau die Grenze gehoben, die dieses Projekt schützt.

Konten entstehen stattdessen einzeln und bewusst. Das erste Admin-Konto legt ein Startvorgang im
Profil `bootstrap` aus Umgebungsvariablen an — nur, wenn noch kein ADMIN existiert, und mit
Zwang zur Passwortänderung, sodass die Variable lediglich ein Einmalpasswort transportiert. Die
Klartext-Passwörter des Altsystems werden nicht gelesen und gelten als kompromittiert.

Jedes neu angelegte Konto startet mit Änderungszwang. Solange dieser gesetzt ist, lehnt jeder
Endpunkt außer Abmelden, Selbstauskunft und Passwortänderung ab. **Ohne diesen Änderungspfad wäre
kein Konto nutzbar** — Annahme A3 schließt eine Selbstbedienung per E-Mail aus.

### Mandantentrennung — die wichtigste Regel des Projekts

**Kein Endpunkt nimmt eine Mandanten-ID entgegen.** Der Mandant wird ausschließlich aus der
Session gelesen.

**Genau drei Ausnahmen**, alle namentlich geführt in `docs/mandantentrennung.md`:
`POST /api/auth/mandant` (Wechsel, geprüft gegen die zulässige Menge), `POST /api/admin/users`
(Anlegen eines Kontos, nur ADMIN) und `PUT /api/admin/users/{id}/tenants` (Pflege der
Mandantenmenge eines Kontos, nur ADMIN). **Alle drei definieren eine Berechtigung, statt einen
Datenausschnitt abzufragen** — das ist das Merkmal, an dem eine Ausnahme zulässig wird. Taucht dort
jemals eine vierte auf, ist das ein Signal und keine Kleinigkeit.

*Korrigiert 20.08.2026:* Hier stand „**Genau zwei Ausnahmen**“ und „Die erste fragt einen
Datenausschnitt, die zweite definiert ein Konto“ — letzteres traf schon auf die erste nicht zu, die
ebenfalls nichts abfragt, sondern aus der zulässigen Menge auswählt. Die dritte Ausnahme kommt mit
Schritt 9a hinzu; `POST /api/admin/users` bleibt unverändert bei **einem** Mandanten.

Beim Wechsel liefert ein **existierender, aber nicht zulässiger** Mandant dieselbe Antwort wie eine
erfundene ID: `404`. Unterschieden sie sich, ließe sich die Mandantenliste abfragen.

Der `MandantContext` trägt immer **genau eine** Mandanten-ID, für beide Rollen. Jede
Repository-Methode, die auf das Quellschema zugreift, bekommt ihn als ersten Pflichtparameter. Es
gibt keine Überladung ohne ihn, und eine ArchUnit-Regel prüft das, statt es nur aufzuschreiben. Der Filter über `ProjectMandant` ist Bestandteil jedes Statements, nicht
nachgelagerte Prüfung. Wer eine fremde `MessageID` errät, bekommt null Zeilen — weil die Zeile für
ihn nie existiert hat.

Pro Endpunkt existiert ein automatisierter Test, der mit Mandant A abfragt und nachweist, dass
Daten von Mandant B unerreichbar sind. Ein neuer Endpunkt ohne diesen Test wird nicht gemergt.

### Rohdatenzugriff

Nutzer sehen zu einer Nachricht **alle zugehörigen Dateien** — die eingegangene Nutzdatei, die
umgewandelten Fassungen und die Protokolle der einzelnen Schritte —, können sie **im Browser ansehen**
und herunterladen. Das ist deutlich sensibler als die Metadaten: Dort stehen Preise, Mengen und
Kundendaten.

**Die Anzeige ist der Regelfall, nicht der Download**, und sie ist **Rohtext**: die Datei als Text,
unverändert, in Festbreitenschrift, als Textknoten gerendert und niemals als HTML. Die
**aufbereitete** Anzeige — EDIFACT, VDA und IDOC in Segmente zerlegt — bleibt ausgeschlossen
(Abschnitt 9).

**Jeder Abruf läuft immer über das Backend, niemals als direkter Link in den Browser.**
Der Filestore kennt unsere Nutzer nicht und kann die Mandantenprüfung nicht leisten. Ein
durchgereichter Link wäre ein unkontrollierter, per Copy-Paste teilbarer Zugang. Kein Endpunkt nimmt
einen Verweis, eine Ablagenkennung oder eine GUID entgegen; die Kennung eines Artefakts ist
`<MessageActionID>-<MessagePropertyName>` und der Verweis wird serverseitig hergeleitet.

Ablauf: Die Verweise stehen als `<Dienst>.Payload.GUID` und `<Dienst>.Log.GUID` in `MessageProperty`
und haben das Format `<Ablagenkennung>|<UUID>`, etwa `FILESTOREPROD09|d95499ff-...`. Die Kennung vor
der Pipe **ist** eine `Service.ServiceID` und wird über den Primärschlüssel zur konkreten Ablage
aufgelöst (Servicetyp "TOMCAT Filestore"). Das Backend prüft zuerst die Mandantenzugehörigkeit der
Nachricht — im Statement, nicht nachgelagert —, ruft dann die Datei ab und **liest, entpackt und gibt
sie neu aus**: Die Ablage spricht ausschließlich **SOAP** (Operation `RETRIEVE`, ohne WSDL, ohne
Authentifizierung) und liefert die Datei als **ZIP-Anhang**. Ein Bytestrom, der bloß durchgereicht
wird, ist es also nicht.

**Die Ablagen sind keine Spiegel.** Löst eine Kennung nicht auf oder antwortet ihr Knoten nicht, ist
das ein benannter Fehlerzustand — **kein Rückfall auf eine andere Ablage**. Gemessen scheitern 20 von
20 Kreuzabrufen, während dieselben Verweise am eigenen Knoten gelingen
([`rohdaten.md`](rohdaten.md) §2.2, M68).

Regeln:
- Ausschließlich `Content-Disposition: attachment`, **niemals inline rendern**. Eine EDI-Datei kann
  gültiges HTML oder SVG enthalten — inline wäre das eine von außen befüllbare
  Cross-Site-Scripting-Lücke.
- `Content-Type: application/octet-stream`, kein Erraten des Typs. Der **Anzeigepfad** liefert
  dagegen JSON beziehungsweise `text/plain` mit `nosniff` — niemals einen Bytestrom mit ratbarem Typ.
- **Der Beschnitt bei Protokollen hängt an der Rolle, nicht an einem Flag.** `MANDANT` sieht bei
  einem Protokoll nur den Bereich zwischen den Marken, pfadmaskiert; `ADMIN` sieht die vollständige,
  unmaskierte Datei ([`rohdaten.md`](rohdaten.md) §3, Entscheidung 3, und §6).
- **Gleichlauf von Anzeige und Download** (Entscheidung 9): Beide gehen durch **dieselbe**
  Aufbereitung, einen Codepfad. Was `MANDANT` sieht, bekommt er auch als Datei — bei einem Protokoll
  also die beschnittene und maskierte Fassung.
- **Drei Ereignisarten im `audit_log`, nicht eine:** Artefakt angesehen · Artefakt heruntergeladen ·
  Abruf fehlgeschlagen. Jede mit Nutzer, Nachricht, Zeitpunkt und IP, und **jede mit der Fassung** —
  beschnitten oder vollständig — beziehungsweise mit dem Zustand, an dem der Abruf scheiterte.
  **Die Fassung ist der Punkt:** Ein Eintrag, der beschnitten und vollständig nicht unterscheidet,
  ist bei einer Rückfrage wertlos, und die Rückfrage ist der Grund, warum es das Protokoll gibt.
- Alle Mandantennutzer sind berechtigt. Eine zweite Berechtigungsstufe für Dateien gibt es nicht.

> **Das Flag ist heute ein totes Feld** *(vermerkt 19.08.2026)*. `app_user.download_allowed` ist
> angelegt, wird bis in die Sitzung getragen und von `GET /api/auth/me` ausgegeben — **geprüft wird es
> nirgends.** Das ist kein Widerspruch zu diesem Abschnitt, der es ausdrücklich nur als *modelliert*
> führt, wohl aber zu einer Erwartung: [`rohdaten.md`](rohdaten.md) §3, Entscheidung 2 schließt eine
> **zweite Berechtigungsstufe** aus, und eine Prüfung einzubauen hieße, genau sie zu errichten.
> **Zu entscheiden: fällt Entscheidung 2, oder fällt das Flag?** Vollständig geführt in
> [`rohdaten-backend.md`](rohdaten-backend.md) §11, Punkt 8, und
> [`rohdaten-frontend.md`](rohdaten-frontend.md) §11, Punkt 8. **Hier ist nichts entschieden.**

*Korrigiert 20.08.2026:* Hier stand, die Berechtigung werde als Flag an `app_user` modelliert
(Standardwert: erlaubt), „damit ein späterer Entzug keine Migration erfordert“. Das Flag
`download_allowed` war seit Schritt 3 tot — von keinem Endpunkt geprüft, in `GET /api/auth/me` aber
ausgegeben. Es fällt per Migration. **Die Begründung wird damit ausdrücklich umgestoßen:** Ein
späterer Entzug erfordert jetzt eine Migration. Bewusst in Kauf genommen, weil niemand danach
gefragt hat. Damit ist die Frage aus dem Kasten darüber entschieden — **es fällt das Flag, nicht
Entscheidung 2** (E20; [`rohdaten.md`](rohdaten.md) §3, Entscheidung 2 gilt unverändert).

**Korrektur 19.08.2026 zu Abschnitt 7, „Rohdatenzugriff".** Der Abschnitt beschrieb durchgehend einen
**Download-Proxy** und war seit dem **14.08.2026** überholt: [`rohdaten.md`](rohdaten.md) §1 führt seit
diesem Tag die **Anzeige als Regelfall**, und genau so ist gebaut worden
([`rohdaten-backend.md`](rohdaten-backend.md), [`rohdaten-frontend.md`](rohdaten-frontend.md), beide
18.08.2026).

**Der alte Abschnitt, wortgleich wie er bis zum 19.08.2026 hier stand:**

```
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
```

**Was fünf Aussagen unberührt lässt.** „Niemals als direkter Link in den Browser", ausschließlich
`Content-Disposition: attachment`, „niemals inline rendern", `Content-Type:
application/octet-stream` und die Protokollpflicht stehen unverändert — sie sind so gebaut und in
diesem Abschnitt nur ergänzt, nicht angetastet. Ebenso unverändert bleibt die Zeile zum
Berechtigungsflag; sie ist um den Vermerk oben ergänzt.

**Was geändert ist, und warum:** „als Proxy" und „holt dann die Datei und streamt sie durch" —
die Ablage spricht SOAP und liefert ein ZIP, das Backend liest, entpackt und gibt neu aus
([`rohdaten.md`](rohdaten.md) §2.3 und §4). Hinzugekommen sind die **Anzeige als Regelfall**, die
**Protokolle** neben den Nutzdaten, das **Verbot des Rückfalls** zwischen den Ablagen, der
**rollenabhängige Beschnitt**, der **Gleichlauf** und die **drei Ereignisarten** statt der einen
Download-Zeile.

> **Nachgetragen 19.08.2026 — die schwerste Änderung fehlte in beiden Listen, und die Auslassung ist
> der Befund.**
>
> Die beiden Absätze darüber teilen jede Aussage des zitierten Altabschnitts in „unberührt" oder
> „geändert" ein. **Eine fehlt in beiden:** dass der Einstieg in den Rohdatenzugriff nicht mehr
> `Message.Payload.GUID` ist. Der Altabschnitt sagt „**Ablauf: `Message.Payload.GUID` hat das
> Format `<FilestoreID>|<UUID>` …**"; die geltende Fassung sagt seit dem 19.08.2026 „Die Verweise
> stehen als `<Dienst>.Payload.GUID` und `<Dienst>.Log.GUID` in `MessageProperty`". Das ist an
> jenem Tag **still** geschehen — und genau das darf in dieser Datei nicht passieren.
>
> **1. Was falsch war.** `Message.Payload.GUID` war nie ein tauglicher Einstieg, und zwar aus einem
> Grund, der erst am Abend desselben Tages gemessen wurde: Der Name benennt **kein eigenes
> Artefakt**. Er trägt in **6.249 von 6.249** Nachrichten (Fenster A) und **214.330 von 214.330**
> (Fenster B) denselben Verweis wie die Nutzdatenzeile mit dem **höchsten `MessageActionID`**
> derselben Nachricht — kein Gegenfall — [`messungen-schritt8.md`](messungen-schritt8.md) M73,
> Befund 2. Der Satz „Nutzer dürfen die **ursprüngliche** EDI-Datei herunterladen" im Altabschnitt
> beschreibt damit ebenfalls die falsche Datei.
>
> **2. Woher der Fehler kam.** Aus dem Namen. `Message.` plus `Payload` liest sich wie „die
> Nutzdatei *der* Nachricht", und niemand hat gefragt, worauf der Verweis zeigt. Dieselbe Ableitung
> ist am 18.08.2026 als Tatsache in [`rohdaten.md`](rohdaten.md) §5 und
> [`rohdaten-frontend.md`](rohdaten-frontend.md) §2 gelandet; beide Stellen tragen seit dem
> 19.08.2026 ihren Korrekturkasten.
>
> **3. Und das ist der eigentliche Befund: Diese Datei ist laut ihrer eigenen Präambel die Instanz,
> die Widersprüche entscheidet.** Genau deshalb wiegt sie hier schwerer als anderswo. Steht in ihr
> eine Aussage ohne Herkunft, schlägt sie jede korrigierte Datei — es ist der **dritte** Fall
> dieser Art nach den beiden in Abschnitt 8 (den 5.000 Nachrichten pro Tag am 07.08.2026 und der
> `information_schema`-Zeilenzahl am 12.08.2026). Alle drei haben dieselbe Gestalt: eine Zahl oder
> ein Name, der plausibel aussah und nie belegt worden ist.
>
> **4. Was unberührt bleibt — und das ist der größere Teil.** Der Weg über das Backend, die
> Auflösung der Kennung über `Service`, das Verbot des direkten Links, `attachment`, `nosniff`,
> `application/octet-stream`, die Protokollpflicht, der Beschnitt, der Gleichlauf, die Ablagen als
> Nicht-Spiegel. Es fällt **ein Name** weg, kein Mechanismus. Die Kennung eines Artefakts bleibt
> `<MessageActionID>-<MessagePropertyName>`.
>
> **5. Was daraus *nicht* folgt.** „Höchster `MessageActionID`" ist die gemessene Größe. „Zuletzt
> erzeugt" wäre eine Deutung darüber, dass die Schrittnummer die Ausführungsreihenfolge ist — sie
> ist plausibel und **nicht gemessen** und steht deshalb in keiner Beschriftung, keinem Feldnamen
> und keinem Kommentar. Genau diese Art naheliegender Deutung ist gerade eingestürzt.
>
> **Kein stilles Überschreiben.** Der zitierte Altabschnitt bleibt Zeichen für Zeichen stehen; die
> Erhebung im Volltext steht in [`messungen-schritt8.md`](messungen-schritt8.md) unter M73.

---

## 8. Verbindliche Leistungsregeln

**Mengengerüst — Zeilenzahlen gezählt (Stand 12.08.2026), Bytegrößen erhoben am 27.07.2026**
(ersetzt die frühere Schätzung von 10.000 bis 100.000 Nachrichten pro Tag und 36 Millionen Zeilen).

**Die Spalte `Herkunft` gehört zur Tabelle und nicht zur Fußnote.** Sie sagt für jede Zeile, ob die
Zahl gezählt oder geschätzt ist; bis zum 12.08.2026 stand über dieser Tabelle „Gemessenes
Mengengerüst", und von sieben Zeilen war **eine** gemessen. Siehe den Kasten darunter.

| Tabelle | Zeilen | Herkunft der Zeilenzahl | Größe |
|---|---|---|---|
| `MessageProperty` | **75.571.462** | **gezählt** (M44, 12.08.2026) | 61,0 GB |
| `MessageBAM` | **15.406.350** | **gezählt** (M33‑0, 11.08.2026) | 7,1 GB |
| `MessageAction` | **10.308.590** | **gezählt** (M14, 07.08.2026) | 3,0 GB |
| `Message` | 3.341.519 | **gezählt** (M0, 01.08.2026) | 2,9 GB |
| `Process` | **1.503** | **gezählt** (M10, 01.08.2026; bestätigt M44) | — |
| `Project` | **140** | **gezählt** (28.07.2026; bestätigt M44) | — |
| `User` | 36 | **gezählt** (M44, 12.08.2026) | — |

**Die Bytegrößen sind durchgängig erhoben und nicht geschätzt.** `DATA_LENGTH` und `INDEX_LENGTH`
stammen aus den belegten Seiten und nicht aus einer Stichprobe. Sie sind über fünf Messtage
(27.07., 07.08., 10.08., 11.08., 12.08.2026) **byteidentisch** geblieben; die Kontrolle steht in
[`messungen-schritt7.md`](messungen-schritt7.md) §0 und M44‑0.

Rund **7.300 Nachrichten pro Tag** im dichten Bestand. Die Aufbewahrung beträgt **22 Monate** —
ältester Datensatz 01.10.2024 —, nicht ein Jahr. Es wird auf der Produktionsdatenbank gelesen.

> **Korrigiert 07.08.2026.** Hier stand bis heute **„rund 5.000 Nachrichten pro Tag"**. Das
> beschreibt einen Durchschnitt, den es an keinem einzigen Tag gab: Er entsteht, wenn man den
> gesamten Zeitraum **einschließlich der fünfmonatigen Datenlücke** durch die Tage teilt. Gemessen
> sind **3.336.386 Zeilen über 456 Tage** (01.10.2024 bis 30.12.2025), also rund 7.300 je Tag —
> [`messungen-schritt4.md`](messungen-schritt4.md), Auffälligkeit A.
>
> **Diese Datei ist laut ihrer eigenen Präambel die Instanz, die Widersprüche entscheidet.** Genau
> deshalb wiegt die falsche Zahl hier schwerer als anderswo: [`annahmen-korrekturen.md`](annahmen-korrekturen.md)
> führt die Korrektur seit dem **01.08.2026**, [`datenmodell.md`](datenmodell.md) §8 ist am
> **07.08.2026** nachgezogen worden — und solange sie hier stand, schlug sie beide. Sie wird
> benannt und nicht stillschweigend ersetzt, damit nachvollziehbar bleibt, dass die verbindliche
> Datei sechs Tage lang der korrigierten hinterherlief.
>
> Auch die Tabelle darüber ist inzwischen an einer Stelle überholt: `MessageAction` ist mit
> **10.308.590** Zeilen **gezählt** worden (M14), die 10.215.743 waren die Schätzung aus
> `information_schema`. Bemerkenswert ist die Richtung — bei `Message` **über**schätzt sie um
> 6,5 %, bei `MessageAction` **unter**schätzt sie um 0,9 %. „Veraltet" heißt also nicht „zu hoch",
> sondern nur „unzuverlässig", und eine `rows`-Angabe im `EXPLAIN` trägt kein Vorzeichen, auf das
> man sich verlassen könnte.

> **Korrigiert 12.08.2026 — nicht eine Zeile, sondern die Überschrift.**
>
> **1. Was falsch war.** Diese Tabelle führte `MessageBAM` mit **10.859.666** und `MessageProperty`
> mit **46.964.279** Zeilen, beide unter der Überschrift „Gemessenes Mengengerüst". Gezählt sind
> **15.406.350** und **75.571.462** — **41,9 %** und **60,9 %** zu niedrig. Dazu `Process` mit 1.490
> statt **1.503** und `Project` mit 142 statt **140**. §3.2 nannte daraus abgeleitet „rund drei
> Einträge je Nachricht" (tatsächlich **4,61**), „rund vierzehn Zeilen je Nachricht" (tatsächlich
> **22,62**) und „1,3 Kilobyte je Zeile" (tatsächlich **808 Byte**).
>
> **2. Woher der Fehler kam.** Aus `information_schema.TABLE_ROWS` — einer Stichprobenschätzung, die
> als Messung ausgegeben wurde. Für `MessageBAM` war das kein Zufall: Regel L8 führte sie bis zum
> 11.08.2026 als **nicht erhoben**; gezählt worden ist sie erst, als der erste MVP-Schritt sie über
> den Wert statt über die `MessageID` anfasste ([`messungen-schritt7.md`](messungen-schritt7.md)
> M33‑0). Bei `MessageProperty` hatte M14 den Zähllauf am 07.08.2026 ausdrücklich abgelehnt — „die
> Schätzung genügt" —, und genau diese Schätzung lag am weitesten daneben (M44).
>
> **3. Und das ist der eigentliche Befund: falsch war die Überschrift, nicht die Zeile.** Von den
> **sieben** Zeilen dieser Tabelle war genau **eine** gezählt (`Message`, M0). Die übrigen sechs
> stammten aus `information_schema`, und **fünf davon waren falsch — in beide Richtungen und von
> 0,9 % bis 60,9 %**:
>
> | Tabelle | hier geführt | gezählt | Abweichung der geführten Zahl | Messung |
> |---|---:|---:|---|---|
> | `MessageProperty` | 46.964.279 | **75.571.462** | **60,9 % zu niedrig** | M44, 12.08.2026 |
> | `MessageBAM` | 10.859.666 | **15.406.350** | **41,9 % zu niedrig** | M33‑0, 11.08.2026 |
> | `Project` | 142 | **140** | 1,4 % zu hoch | 28.07.2026, bestätigt M44 |
> | `MessageAction` | 10.215.743 | **10.308.590** | 0,9 % zu niedrig | M14, 07.08.2026 |
> | `Process` | 1.490 | **1.503** | 0,9 % zu niedrig | M10, 01.08.2026, bestätigt M44 |
> | `User` | 36 | **36** | keine | M44, 12.08.2026 |
> | `Message` | 3.341.519 | 3.341.519 | — die geführte Zahl **war** die gezählte; `information_schema` nannte 3.560.486 und lag 6,5 % über der Wahrheit | M0, 01.08.2026 |
>
> Der Satz, dass eine `rows`-Angabe **kein Vorzeichen** trägt, steht im Kasten darüber seit dem
> 07.08.2026 für `MessageAction`. Er bekommt hier seinen dritten und vierten Beleg — und mit 60,9 %
> ist die Schätzung bei der größten Tabelle nicht mehr ungenau, sondern **unbrauchbar**. *Zur
> Prozentrechnung: Bezugsgröße ist die hier geführte Zahl. Für `Message` ist es umgekehrt die
> gezählte, weil dort nicht die Datei danebenlag, sondern `information_schema`.*
>
> **4. Was unberührt bleibt — und das ist der größere Teil.** Die **Bytegrößen** stammen aus
> belegten Seiten und nicht aus einer Stichprobe; sie sind über fünf Messtage byteidentisch
> geblieben. Damit stehen unverändert: **Regel L4** (`MessageProperty` nur über `MessageID`) — die
> Zählung macht sie sogar **strenger**, weil ein Fehlzugriff 75,6 statt 47 Millionen Zeilen umwälzt;
> die **82 Prozent** (sie rechnen mit Bytes); **Regel L5** (BAM-Suche mit Limit und Mindestlänge),
> durch M33 erstmals gemessen unterlegt — Maximum 234.159 Treffer auf einen Wert; **Annahme A9**,
> durch M33/M34 erledigt; und **alle sieben Leistungsregeln** darunter. Keine davon ändert sich.
>
> **Kein stilles Überschreiben.** Die alten Zahlen stehen oben in diesem Kasten und bleiben dort.
> Die Erhebung im Volltext steht in [`messungen-schritt7.md`](messungen-schritt7.md) M33‑0 und M44,
> die Einordnung in [`annahmen-korrekturen.md`](annahmen-korrekturen.md).

> ### ⚠️ Die Aufbewahrung von 22 Monaten ist ungedeckt *(gekennzeichnet am 27.08.2026)*
>
> **Der Satz „Die Aufbewahrung beträgt 22 Monate — ältester Datensatz 01.10.2024" beschreibt die
> Testkopie und nicht die Produktion.** Die Zahl ist aus `MIN`/`MAX` über
> `Message.MessageLastUpdate` **der Kopie** hergeleitet; sie sagt, wie weit diese Kopie
> zurückreicht, und nicht, wie lange produktiv aufbewahrt wird. Eine Aussage des Betreibers steht
> dahinter nicht.
>
> | Weg | Ergebnis |
> |---|---|
> | `MIN`/`MAX` über die Testkopie (V5 der Messrunde, 26.08.2026) | `2024-10-01 02:00:28` bis `2026-07-08 17:21:10` — **21,2 Monate**, darin eine fünfmonatige Lücke und ein praktisch leeres Jahr 2026 |
> | [`rohdaten.md`](rohdaten.md) §12 | produktiv **rund 18 Monate**, danach ein Archivsystem — **eine Auskunft, keine Messung** |
> | Nachfrage beim Betreiber des Altsystems | **nicht erfolgt** |
>
> > **Belegvermerk** (Regel L10).
> > *Gemessen ist:* die Ausdehnung der **Testkopie**.
> > *Behauptet wird* in dem Satz oben: die Aufbewahrungsdauer der **Produktion**.
> > **Die Lücke ist grundsätzlich:** Aus der Ausdehnung einer Kopie folgt nichts über die
> > Löschregel des Originals. Eine Kopie kann jünger sein als die Aufbewahrung, älter als sie —
> > wenn sie vor der letzten Löschung gezogen wurde —, oder zufällig gleich lang. **Von hier aus
> > ist der Unterschied nicht messbar**, weil auf der Testkopie nichts entfernt wird.
>
> **Die Zahl wird nicht ersetzt.** Weder 18 noch 22 sind belegt; 18 wäre nur eine ungedeckte Angabe
> gegen eine andere getauscht. Sie wird **gekennzeichnet** — dieselbe Bauform wie beim Takt von
> `MatchInterchange` in Abschnitt 3.3, und aus demselben Grund: In der verbindlichen Datei steht
> eine Angabe ohne Herkunft als Tatsache. **Die Quelle liegt beim Betreiber des Altsystems, und
> dort ist sie zu holen.**
>
> **Was daran hängt, und es ist mehr als eine Fußnote.** Offener Punkt 54 in
> [`rollup.md`](rollup.md) §13 beschreibt den Fall, dass das Altsystem alte Nachrichten entfernt:
> Dann bleiben Rollup-Zeilen unterhalb des neuen Bestandsanfangs für immer stehen, und
> `SUM(anzahl)` läuft gegen die Zeilenzahl von `Message` auseinander. **Ob dieser Fall eintritt,
> hängt genau an der hier ungedeckten Angabe.** M92 rechnet deshalb ausdrücklich mit der Testkopie
> und rechnet nichts hoch.

Die Zeilenzahl war nie die richtige Kennzahl. `MessageProperty` belegt 61 GB und ist damit 82
Prozent der Datenbank; dort entscheidet die Bytegröße.

1. **Jeder Listen-Endpunkt hat ein Pflicht-Zeitfenster.** Standard 24 Stunden, Maximum ein Jahr.
   Ohne Zeitfenster keine Abfrage.

   > **Benannt am 27.08.2026 — die Spannung zwischen dieser Regel und der Aufbewahrung.** Oben
   > steht ein Bestand von **22 Monaten**, hier ein Höchstfenster von **einem Jahr**. Daraus folgt:
   > **Rund zehn Monate des Bestands sind über einen Listen-Endpunkt nicht erreichbar** — nicht in
   > einer Abfrage, und auch nicht in mehreren, weil das Fenster immer am gewählten Ende hängt.
   > Über einen Cursor lässt sich nach hinten weiterblättern, aber nur innerhalb desselben Fensters.
   >
   > **Das ist kein Fehler, und die Regel wird nicht gelockert.** Das Werkzeug beantwortet die
   > Frage „wo ist mein Beleg", und die stellt niemand für einen Beleg von vor achtzehn Monaten.
   > Ein Jahr ist bereits großzügig gegen den Leitsatz aus Abschnitt 1.
   >
   > **Benannt gehört es trotzdem**, aus zwei Gründen. Erstens sucht sonst irgendwann jemand den
   > Fehler in der Abfrage, wenn eine Nachricht von vor 18 Monaten „nicht existiert". Zweitens ist
   > die Zahl **22 selbst ungedeckt** — siehe den Kasten oben. Verkürzt sich die produktive
   > Aufbewahrung auf die dort genannten rund 18 Monate, schrumpft die unerreichbare Spanne auf
   > sechs; verlängert sie sich, wächst sie. **Die Spannung ist damit nicht bezifferbar, nur
   > benannt.** Erhoben als Nebenbefund der Messrunde vom 26.08.2026
   > ([`messungen-schritt10.md`](messungen-schritt10.md), Abschnitt „Korrekturen").
   >
   > **Und das MVP hat keinen zweiten Weg dorthin.** Die BAM-Suche ist ebenfalls an diese Regel
   > gebunden — Vorgabe 30 Tage statt 24 Stunden, Maximum unverändert ein Jahr
   > ([`bam-suche.md`](bam-suche.md) §2). Über eine bekannte `MessageID` ist eine Nachricht zwar
   > ohne Fenster erreichbar, aber nur, wenn man sie schon hat. **Wer die alten Monate braucht,
   > braucht das Archivsystem aus [`rohdaten.md`](rohdaten.md) §12 — und das ist kein Bau dieses
   > Projekts.**

   > ### ⚠️ Das Jahresmaximum ist für den größten Mandanten möglicherweise nicht baubar *(gekennzeichnet am 08.09.2026)*
   >
   > **Die Regel bleibt, wie sie ist — Maximum ein Jahr. Gekennzeichnet wird, dass dieses Maximum
   > für den Zugriff über den Zeitbereich in `Message` gemessen nicht mehr trägt, sobald der
   > Bereich groß genug ist.** Der Optimizer gibt `MessageLastUpdateIDX` zwischen 30 und 90 Tagen
   > auf und liest `Message` als Vollscan — und das betrifft **jedes** Statement, das über den
   > Zeitbereich in diese Tabelle einsteigt, nicht nur die Property-Suche, an der es aufgefallen
   > ist.
   >
   > | Fenster (`NEXANS`, Ende 30.12.2025) | geschätzte Zeilen des Bereichs | Zugriff auf `Message` | Messung |
   > |---|---:|---|---|
   > | 30 Tage | 409.756 (gelesen 214.330) | `range` über `MessageLastUpdateIDX` | M168, M170 |
   > | 90 Tage | **1.476.166** — 41 % der geschätzten Tabelle (gelesen 680.872) | **`ALL` über 3.560.486 Zeilen**, `Using filesort`; in den zehn Sekunden 3.383.893 Zeilen sequenziell gelesen, dann Abbruch | M168, M171 |
   > | ein Jahr | 1.780.243 | `range` über `MessageLastUpdateIDX` — **in einem anders gebauten Statement**, der BAM-Präfixsuche | M50 |
   >
   > **Die Schwelle ist nicht erhoben, und sie ist nicht einmal eine Zahl.** Über 90 Tage kippt
   > der Plan in allen zehn Messfällen der L4‑Pfad-Runde (M168: drei Namen in zwei Fassungen,
   > M171: vier Formen); über ein Jahr hält M50 den Zeitindex in einem anderen Statement. Ob ein
   > Statement kippt, hängt an der Kostenrechnung des Optimizers für **dieses** Statement — und
   > die rechnet mit Bereichsschätzungen, die um Faktor 1,9 bis 2,2 zu hoch liegen (M170, M171).
   > **Wo zwischen 30 und 90 Tagen die Grenze liegt, ist für kein Statement gemessen** — offener
   > Punkt 159 in [`messungen-property-suche.md`](messungen-property-suche.md). Erzwungen —
   > `STRAIGHT_JOIN`, `FORCE INDEX` — hält der Zeitindex über 90 Tage, und das Statement bricht
   > trotzdem ab, weil hinter 621.284 bis 660.928 gelesenen Indexeinträgen die Nachschlagezugriffe
   > nicht mehr durchkommen (M171).
   >
   > > **Belegvermerk** (Regel L10).
   > > *Gemessen ist:* Plan und Laufzeit von zehn Statementformen über 90 Tage bei `NEXANS`, alle
   > > mit Vollscan oder Abbruch; und der Plan eines anderen Statements über ein Jahr, mit
   > > gehaltenem Zeitindex (M50).
   > > *Behauptet wird:* dass das Jahresmaximum für den größten Mandanten **möglicherweise** nicht
   > > baubar ist.
   > > **Die Lücke:** „Möglicherweise" ist das Wort. Ob ein konkretes Statement über ein Jahr
   > > antwortet, sagt nur seine eigene Messung — die BAM-Suche antwortet exakt über ein Jahr
   > > (8.939,7 ms, M47) und bricht präfixweise ab (M50); für die Property-Suche steht die
   > > Jahresmessung in [`property-suche.md`](property-suche.md).
   >
   > **Die Zahl wird nicht ersetzt.** Ein Jahr bleibt das Maximum, weil kein Statement gemessen
   > ist, das ein kleineres rechtfertigt, und weil die Regel jeden Listen-Endpunkt bindet — nicht
   > nur den, an dem der Befund entstand. Sie wird **gekennzeichnet**, dieselbe Bauform wie bei der
   > Aufbewahrung oben: Wer einen Listen-Endpunkt baut, der über den Zeitbereich in `Message`
   > einsteigt, misst ihn über ein Jahr gegen `NEXANS` und nicht nur über die Vorgabe (Regel L7)
   > — und wer jenseits von 30 Tagen einen Vollscan im `EXPLAIN` sieht, hat diesen Befund gefunden
   > und keinen neuen.
2. **Keine Live-Aggregation über `Message`.** Dashboard-Kennzahlen kommen ausschließlich aus
   `message_rollup`. Ein stündlicher Job schreibt inkrementell fort.
   *Begründung korrigiert 27.07.2026:* Bei 3,3 Millionen Zeilen und 2,9 GB wäre eine
   Live-Aggregation technisch nicht unmöglich — das Altsystem macht sie tagesweise. Der Rollup
   existiert, damit das Dashboard unter einer halben Sekunde lädt und die Produktionsdatenbank
   nichts davon merkt, nicht weil eine Live-Abfrage scheitern würde. Eine Regel, die mit einer
   falschen Zahl begründet ist, wird beim ersten Zweifel gekippt.

   > ### Die erste benannte Ausnahme dieser Regel: „Überfällig" *(E‑c, 24.08.2026, eingetragen am 27.08.2026)*
   >
   > **Das Wort „ausschließlich" bleibt stehen, und daneben steht diese Ausnahme.** Die Kachel
   > *Überfällig* kommt aus einer **Live-Abfrage** über `Message`, nicht aus `message_rollup`.
   >
   > **Der Grund ist, dass die Kategorie an `jetzt` hängt.** „Überfällig" heißt nach Abschnitt 4.2:
   > nicht in einem Endstatus **und** `MessageLastUpdate + MessageTimeout` liegt in der
   > Vergangenheit. Der zweite Teil bewegt sich mit der Uhr. Ein gespeicherter Zähler wäre in der
   > Sekunde nach dem Rollup-Lauf falsch — und zwar in beide Richtungen: Zeilen laufen ohne jede
   > Datenänderung in die Kategorie hinein, und eine Nachricht, die fertig wird, fällt heraus, ohne
   > dass eine Aggregation davon erführe. Das ist kein Genauigkeitsproblem, das man mit einem
   > kürzeren Takt löste; es ist eine Kennzahl, die es in gespeicherter Form nicht gibt.
   >
   > **Gemessen — M90, 26.08.2026** ([`messungen-schritt10.md`](messungen-schritt10.md)):
   >
   > | Fassung | Laufzeit |
   > |---|---:|
   > | Standardfenster (48 h, `NEXANS`, mit Mandantenfilter) | **2,275 ms** |
   > | „insgesamt", ohne jedes Zeitfenster | **4,275 ms** |
   > | teuerste gemessene Fassung überhaupt | 5,371 ms |
   >
   > Die Abfrage liest **539 Indexsätze** über `MessageStatusIDX` und schlägt für jeden einmal in
   > der Mandantenkette nach. Sie skaliert mit der Zahl der **offenen** Zeilen, nicht mit dem
   > Bestand — deshalb kostet die Fassung *ohne* Zeitfenster nur zwei Millisekunden mehr. **Das ist
   > keine Live-Aggregation über `Message` im Sinne dieser Regel**, sondern ein Indexzugriff mit
   > Nachschlagen. Zusammen mit der Rollup-Leseabfrage aus M89 steht die Landingpage bei
   > **17,86 ms** von 500.
   >
   > **Eine benannte Ausnahme ist etwas anderes als eine aufgeweichte Regel.** Sie ist einzeln
   > begründet, einzeln gemessen und einzeln gezählt — genau wie die Ausnahmen von M1 und M2, die
   > dieses Projekt in [`mandantentrennung.md`](mandantentrennung.md) namentlich führt. **Taucht
   > hier jemals eine zweite auf, ist das ein Signal und keine Kleinigkeit.** Wer eine zweite
   > Kennzahl live rechnen will, trägt sie hier ein und begründet sie — und misst sie, denn der
   > Vorbehalt aus M89 gilt: Beim Monatsfenster kostet die Rollup-Leseabfrage bereits 237,67 ms,
   > und dort dürfen die Kacheln nicht zusätzlich live rechnen.

   > ### ⚠️ Die Ausnahme ist am 03.09.2026 gefallen — und es sind jetzt zwei andere *(E‑71, E‑72, E‑73)*
   >
   > **Der Kasten oben bleibt vollständig stehen.** Er ist die Begründung, an der sich ablesen
   > lässt, wofür eine benannte Ausnahme überhaupt gut ist — und sein letzter Satz ist genau der,
   > der hier eingelöst wird.
   >
   > **Die Ausnahme *Überfällig* ist entfallen**, weil die Problemkategorie widerlegt ist (§4.2,
   > Punkt 2). **An ihre Stelle treten zwei:**
   >
   > | Ausnahme | Was sie live liest | Warum sie nicht aus dem Rollup kommen kann |
   > |---|---|---|
   > | **Kachel *Läuft*** (E‑72) | `COUNT(*)` und `MIN(MessageLastUpdate)` über `MessageStatus = 'RUNNING'` | — |
   > | **Kachel *Wartend*** (E‑73) | dasselbe über `SUSPENDED` | — |
   >
   > **Der Grund ist ein anderer als bei *Überfällig*, und das ist der Punkt.** Nicht mehr, dass
   > die Kennzahl an einer **Frist** hängt, die zwischen zwei Läufen abläuft — sondern dass sie an
   > einem **flüchtigen Status** hängt. `message_rollup` trägt **keine Statushistorie**: Der
   > nächtliche Volllauf rechnet jeden Eimer aus dem *heutigen* Zustand jeder Nachricht neu, und
   > eine Nachricht, die im März `SUSPENDED` war und im April fertig wurde, hinterlässt im März
   > nichts. *„Wie viele warten gerade"* ist dort nicht beantwortbar
   > ([`rollup.md`](rollup.md) §7a).
   >
   > **Gemessen — M143, 03.09.2026** ([`messungen-schritt10b.md`](messungen-schritt10b.md)), an
   > dem, was der Code schickt, ein Aufwärmlauf und dann die beste von fünf:
   >
   > | Statement | `NEXANS` | `SUTTONS` |
   > |---|---:|---:|
   > | Kachel *Läuft* | **1,09–1,25 ms** | **0,94–1,04 ms** |
   > | Kachel *Wartend* | **4,40–4,66 ms** | **3,34–3,55 ms** |
   > | Erscheinungsbedingung *Wartend* | **1,24–1,40 ms** | **1,40–1,42 ms** |
   >
   > Alle drei steigen über einen Index ein und lesen keine Tabelle voll. **Zusammen kosten sie
   > weniger als die eine Ausnahme, die sie ersetzen** (9,7 bis 11,3 ms in M108).
   >
   > **Die dritte Abfrage ist ausdrücklich keine dritte Ausnahme.** Die Erscheinungsbedingung der
   > Kachel *Wartend* liest **Stammdaten** (`SOSAction`, 3.944 Zeilen, 2,0 MiB) und nicht `Message`
   > — sie ist damit dieselbe Art Zugriff wie die Mandantenkette, die in jedem Statement dieses
   > Dashboards steht. **Sie aggregiert nicht.**
   >
   > **Die Zählung bleibt damit bei zwei, und der Satz oben gilt unverändert:** Wer eine dritte
   > Kennzahl live rechnen will, trägt sie hier ein, begründet sie und misst sie.
3. **Keine `OFFSET`-Paginierung.** Cursor-basiert über `(MessageLastUpdate, MessageID)`.
4. **`MessageProperty` nur über `MessageID`.** Nie filtern, gruppieren oder sortieren über den Wert.

   > ### Die erste benannte Ausnahme dieser Regel: der Sucheinstieg über Name und Wert *(E‑102, Sparringsrunde vor dem 07.09.2026; bestätigt durch M168 bis M171 am 08.09.2026; eingetragen am 08.09.2026)*
   >
   > **Der Satz oben bleibt Zeichen für Zeichen stehen, und daneben steht diese Ausnahme.** Die
   > Property-Suche ([`property-suche.md`](property-suche.md)) steigt in `MessageProperty` über
   > **Name und Wert** ein — `MessagePropertyName = ? AND MessagePropertyValue = ?` — und nicht
   > über die `MessageID`. **Das ist die Ausnahme, und sie ist die ganze Ausnahme:** Für jeden
   > anderen Zugriff auf diese Tabelle — Detail, Rohdaten, Verkettung, jede Aggregation — gilt
   > die Regel unverändert. Gefiltert wird ausschließlich mit `=`; gruppiert und sortiert wird
   > über den Wert auch in der Suche nie.
   >
   > **Der Grund ist, dass der L4‑konforme Pfad die Suche nicht trägt — und das ist in vier
   > Fassungen erzwungen und gemessen, nicht angenommen.** Über die Fenstermenge einsteigen und
   > `MessageProperty` über den Primärschlüssel erreichen — genau das, was die Regel vorschreibt
   > — ist strukturell (materialisierte Fenstermenge, bindende Unterabfrage je Zeile) und per
   > Hinweis (`STRAIGHT_JOIN`, `FORCE INDEX (PRIMARY)`) erzwungen worden, gegen `NEXANS`, je Name
   > mit dem häufigsten Wert seines 30‑Tage-Fensters
   > ([`messungen-property-suche.md`](messungen-property-suche.md), L4‑Pfad-Runde):
   >
   > | Fenster | L4‑Pfad, erzwungen | freier Pfad über den Wertindex (Fassung A) |
   > |---|---:|---:|
   > | 24 Stunden | **96 bis 97 ms** (M168) | 91 bis 93 ms (M166) — dort wählt der Optimizer den L4‑Pfad von selbst |
   > | 30 Tage | **3.186 bis 8.195 ms** (M171; Fassung B und C in M168 4.347 bis 8.129 ms) | **789 bis 1.862 ms** (M166) |
   > | 90 Tage | **⛔ Abbruch an der 10‑s‑Grenze des Lese-Pools, in jeder Form** — M168: 42 von 42 Ausführungen, M171: 4 von 4 Formen | `Message.GUID` 0,946 ms, `Message.ReceiverID` 1.276 ms (M159); für die drei Namen des Nachtrags nicht gemessen |
   >
   > Über 30 Tage unterbietet **keine** erzwungene Form den Maßstab der ausgelieferten BAM-Suche
   > (1.655,8 ms, M47), und keine unterbietet den freien Pfad. **Über 90 Tage ist die Ausnahme
   > nicht die billigere, sondern die einzige antwortende Form** — dort gibt der Optimizer den
   > Zeitindex auf `Message` auf und liest die Tabelle voll (Kennzeichnung an Leistungsregel 1).
   >
   > **Warum sie nicht durch ein Tagesfenster zu umgehen ist.** Über 24 Stunden hält die Regel —
   > aber **als Wahl des Optimizers, nicht als Garantie**: Die materialisierte Fassung B bricht in
   > **14 von 18** gemessenen Kombinationen aus dem L4‑Pfad aus und führt mit `MessageProperty`
   > über einen Wertindex (M170), und die Tabellenstatistik, an der diese Wahl hängt, liegt um
   > **37,9 %** daneben — 46.964.279 geschätzt gegen 75.571.462 gezählt (M155). Ein Tagesfenster
   > machte die Suche außerdem für den Nutzer wertlos, der eine Nummer hat und kein Datum —
   > dieselbe Lage wie bei der BAM-Suche ([`bam-suche.md`](bam-suche.md) §2).
   >
   > **Was sie kostet.** Die Regel ist nicht mehr absolut. Der Wertindex ist ein Präfixindex über
   > 50 Zeichen; beim häufigsten Wert eines Namens liest die Suche über ihn bis zu 124.715
   > Indexeinträge (`Message.VFN`, M165) und kostet über 30 Tage bis zu 1.862 ms (M166). Das ist
   > der Preis, und er steht in [`property-suche.md`](property-suche.md) neben jedem Statement,
   > das ihn zahlt.
   >
   > **Eine benannte Ausnahme ist etwas anderes als eine aufgeweichte Regel.** Sie ist einzeln
   > begründet, einzeln gemessen und einzeln gezählt — genau wie die Ausnahmen von M1 und M2, die
   > dieses Projekt in [`mandantentrennung.md`](mandantentrennung.md) namentlich führt, und wie
   > die beiden Ausnahmen von Leistungsregel 2 darüber. **Taucht hier jemals eine zweite auf, ist
   > das ein Signal und keine Kleinigkeit.** Wer `MessageProperty` ein zweites Mal anders als über
   > die `MessageID` anfassen will, trägt es hier ein und begründet es — und misst es, denn der
   > Vorbehalt aus M157 gilt: Schon das Zählen eines einzigen Namens über den Gesamtbestand kostet
   > 125,527 s, das 12,6‑Fache der Poolgrenze.
5. **BAM-Suche mit hartem Limit und Mindestlänge** des Suchbegriffs. BAM-Werte wie `050` kommen
   millionenfach vor.
6. **Der Rollup-Job läuft gedrosselt.** Er teilt sich die Instanz mit der Produktion.

   > **Verortet am 27.08.2026 — die Regel wird nicht gestrichen, sie bekommt ihre Stelle.**
   > Gemessen in M88 und M92 (26.08.2026) und an der gebauten Fassung nachgemessen
   > ([`rollup.md`](rollup.md) §9): **Der Rollup-Job hat zwei Laufarten, und die Regel trifft nur
   > eine davon.**
   >
   > | Laufart | Kosten | Anteil an der Instanz |
   > |---|---:|---|
   > | **stündlicher Delta-Lauf**, dichteste Stunde des ganzen Bestands (8.630 Zeilen) | **88,167 ms** | **0,0024 %** einer Stunde |
   > | stündlicher Delta-Lauf, letzte Stunde des Bestands (285 Zeilen) | 3,190 ms | 0,00009 % |
   > | **nächtlicher Volllauf**, gebaut, ohne Drosselung (335.610 Zeilen) | **45,772 s** | — |
   > | derselbe Volllauf mit `scheiben-pause: 1s` | 69,186 s | 51,2 % länger |
   >
   > **Beim Delta-Lauf ist die Regel Zeremonie.** Ein Statement, das in einer Stunde 88 Millisekunden
   > der Instanz belegt, ist nicht drosselbar — es ist bereits vorbei, bevor eine Drosselung greifen
   > könnte, und jede Pause dahinter verlängerte nur die Wanduhr, ohne Last zu verteilen. Der
   > Delta-Lauf fährt deshalb ungedrosselt und in **einem** Statement.
   >
   > **Beim Volllauf greift sie.** Er liest den gesamten Bestand ohne Zeitfenster; der Gesamtbereich
   > in einem Statement reißt die Zeitgrenze `max_statement_time = 10` des Lese-Pools. Er fährt
   > deshalb in **Monatsscheiben**, mit einer konfigurierbaren Pause **zwischen** den Scheiben —
   > dort, und nur dort, ist die Drosselung eine Stellschraube. Sie kostet auf der Testkopie 23,4
   > Sekunden zusätzlich, und genau dafür ist sie da.
   >
   > **Warum das hier steht, obwohl es eine Kleinigkeit zu sein scheint.** Eine Regel, die überall
   > zitiert wird, wirkt nirgends: Solange „gedrosselt" für beide Laufarten galt, war sie an der
   > einen unerfüllbar und an der anderen selbstverständlich — und in beiden Fällen kein Maßstab,
   > an dem sich eine Umsetzung prüfen ließe. **Verortet ist sie prüfbar.**
   >
   > **Was daran ungemessen bleibt** (offener Punkt 50 in [`rollup.md`](rollup.md) §13): *wie viel*
   > Last die Produktionsinstanz nachts verträgt, und ob `03:00` die richtige Stunde ist. Beides ist
   > nicht erhoben, beides ist konfigurierbar, damit eine Korrektur keine Codeänderung kostet. Und
   > alle Zahlen oben stammen von der **Testkopie**; M92 rechnet ausdrücklich nicht hoch.
7. **Jede neue Abfrage wird vor dem Merge gegen die Testkopie gemessen** (`EXPLAIN` plus
   Laufzeit). Kein Statement geht ungeprüft in Produktion.

### Umgebungen

| Umgebung | Inhalt | Verwendung |
|---|---|---|
| Testkopie (interner Host, MariaDB 10.6) | Vollkopie der Produktion, Datenstand **08.07.2026** | Entwicklung, Tests, Messung von Abfrageplänen |
| Produktion | Live | Laufzeitdatenquelle der Anwendung |

**Bestätigt am 01.08.2026** durch Messung M0: `MIN(MessageLastUpdate) = 2024-10-01 02:00:28`,
`MAX = 2026-07-08 17:21:10`, 3.341.519 Zeilen — unverändert gegenüber dem 28.07.2026, die Testkopie
ist seither nicht neu befüllt worden. Dieser Abschnitt ist damit die maßgebliche Stelle für den
Datenstand; die abweichende Angabe „Daten bis Ende 2025" bei Annahme A1 in Abschnitt 11 war falsch
und ist dort korrigiert. **Wie sich die 3,34 Millionen Zeilen über diesen Zeitraum verteilen, ist
nicht gleichmäßig** — die gemessene Verteilung steht in
[`messungen-schritt4.md`](messungen-schritt4.md) und ist vor der Festlegung relativer Zeitfenster in
Schritt 4 zu lesen.

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
- Rohdaten und Protokolle: alle Dateien einer Nachricht — Nutzdaten **und** Protokolle — als
  **Rohtext ansehbar** und herunterladbar, immer über das Backend, mit Protokollierung
- Administration: Benutzerverwaltung, Prozess-Katalog mit Massenzuordnung nach Projekt

**Nicht enthalten**

- Service- und Heartbeat-Überwachung (`Service.ServiceStatus`) — Betriebssicht, nicht Kundensicht
- **Aufbereitete** Anzeige der Rohdaten im Browser (EDIFACT/VDA/IDOC in Segmente zerlegt) — gebaut
  ist die **Rohtextanzeige**: die Datei als Text, unverändert, in Festbreitenschrift
- Benachrichtigungen und Alarmierung
- Chatbot

**Korrektur 19.08.2026 zu Abschnitt 9.** Zwei Zeilen dieser Listen beschrieben Schritt 8 als reinen
Download und waren seit dem **14.08.2026** überholt.

> **Wortgleich, wie sie bis heute hier standen:**
>
> > - Download der EDI-Rohdatei über den Backend-Proxy, mit Protokollierung
> >
> > - Formatierte Anzeige der Rohdaten im Browser (EDIFACT/VDA/IDOC aufbereitet) — **nur Download**

**Der Ausschluss bleibt bestehen, und die Klammer bleibt richtig.** Gefallen ist allein der Zusatz
„nur Download": Ausgeschlossen ist die **aufbereitete** Anzeige, nicht die Anzeige überhaupt.
[`rohdaten.md`](rohdaten.md) §1 führt seit dem 14.08.2026 die **Rohtextanzeige als Regelfall** und
grenzt sie ausdrücklich gegen diesen Abschnitt ab; gebaut ist es so
([`rohdaten-frontend.md`](rohdaten-frontend.md) §4). Bemerkenswert dazu, und der Grund ist offen: Das
Altwerkzeug **hat** die aufbereitete Anzeige — vier Formate stehen in der Auswahl, alle vier sind
abgeschaltet, der Umformatierungscode existiert ([`rohdaten.md`](rohdaten.md) §13, Punkt 3). In der
Zeile unter „Enthalten" ist außerdem der **Proxy** gefallen und sind die **Protokolle** hinzugekommen
— die Begründung dafür steht bei der Korrektur zu Abschnitt 7.

**Korrektur 27.08.2026 zu Abschnitt 9 — „die drei Problemkategorien" sind im MVP zwei.**

> **Wortgleich, wie sie oben steht und stehen bleibt:**
>
> > - Dashboard: Volumen im Zeitverlauf, die drei Problemkategorien, Verteilung nach Partner

**Es sind zwei: *Fehler* und *Überfällig*.** *Unquittiert* ist mit Entscheidung **E‑d** vom
24.08.2026 aus dem MVP genommen; die vollständige Begründung steht im Kasten bei Abschnitt 4.2
Punkt 3 — keine operative Definition, und die Kategorie hängt am ungedeckten Takt von
`MatchInterchange`. **Die Beschreibung in Abschnitt 4.2 bleibt vollständig stehen**, und ihre
Überschrift bleibt „Die drei Problemkategorien": Fachlich sind es drei, gebaut werden zwei.

> ### ⚠️ Seit dem 03.09.2026 ist es **eine** *(E‑71)*
>
> **Der Absatz oben bleibt stehen und war am Tag seiner Niederschrift richtig.** *Überfällig* ist
> mit Entscheidung **E‑71** widerlegt und aus dem MVP genommen; die vollständige Begründung samt
> Herkunftsvermerk steht im Kasten bei Abschnitt 4.2, Punkt 2.
>
> **Im MVP bleibt *Fehler*.** Regel Q3 — „die Problemkategorien bleiben getrennt" — ist damit
> **einstellig** und in der Sache nicht mehr prüfbar: Es gibt nichts mehr, wovon zu trennen wäre.
> **Die Regel wird trotzdem nicht gestrichen.** Sie gilt fachlich für drei Kategorien und wird an
> dem Tag wieder wirksam, an dem eine zweite gebaut wird.
>
> **Was das Dashboard aus Schritt 10b stattdessen baut:** eine Problemkachel (*Fehler*) und zwei
> **Zustandskacheln** (*Läuft*, *Wartend*). Zustandskacheln sind keine Problemkategorien — sie
> behaupten kein Problem, sie zählen einen Zustand. **Genau deshalb tragen sie auch keine
> Problemfarbe** ([`visuelles-konzept.md`](visuelles-konzept.md) §7a).

> **Ein Befund der Messrunde gehört dazu, und er verschiebt die Korrektur.** Der Auftrag der
> Messrunde vom 26.08.2026 ging davon aus, in Abschnitt 9 stehe das Wort „Unquittiert" und sei zu
> streichen. **Es steht hier nicht — nirgends.** Abschnitt 9 nennt die Kategorie überhaupt nicht,
> sondern ausschließlich die Sammelformulierung „die drei Problemkategorien"
> ([`messungen-schritt10.md`](messungen-schritt10.md), „Was diese Runde nicht zeigt", Punkt 5).
>
> **Das ist mehr als eine Formalie.** Eine Zeile, die eine Zahl nennt statt einer Aufzählung, ist
> beim Nachschlagen nicht auffindbar: Wer nach „Unquittiert" sucht, um zu prüfen, wo die Kategorie
> zugesagt ist, findet diesen Abschnitt nicht — und dieser Abschnitt ist die Stelle, die den
> MVP-Umfang festlegt. **Die Zahl „drei" war hier die einzige Zusage**, und sie stand ohne den
> Begriff, der sie prüfbar gemacht hätte.

**Was das für die Umsetzung heißt:** Das Dashboard aus Schritt 10b baut **zwei** Kacheln.
*Überfällig* kommt dabei nicht aus dem Rollup, sondern aus einer Live-Abfrage — die erste benannte
Ausnahme von Leistungsregel 2, siehe Abschnitt 8. *(Überholt seit dem 03.09.2026: siehe den Kasten
darüber. Gebaut sind eine Problemkachel und zwei Zustandskacheln; die Live-Abfragen sind jetzt
zwei und heißen* Läuft *und* Wartend*.)* Der [`IMPLEMENTIERUNGSPLAN_MVP.md`](IMPLEMENTIERUNGSPLAN_MVP.md)
trägt dieselbe Angabe („die drei getrennten Problemkategorien") und ist dort ebenso berichtigt.

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
| ~~A1~~ | **Geklärt.** Testkopie der Produktion vorhanden, zur Laufzeit wird auf der Produktion gelesen. **Korrigiert 01.08.2026:** Hier stand „Daten bis Ende 2025" — das widersprach Abschnitt 8, der **08.07.2026** nennt. Messung M0 gibt Abschnitt 8 recht: `MAX(Message.MessageLastUpdate) = 2026-07-08 17:21:10`. Die falsche Angabe stammte aus der Zeit vor der Erhebung vom 27.07.2026 und ist beim Nachziehen von Abschnitt 8 übersehen worden. Zur Verteilung innerhalb dieses Zeitraums siehe [`messungen-schritt4.md`](messungen-schritt4.md) | — |
| ~~A2~~ | **Geklärt 20.08.2026.** Schritt 9a pflegt die n:m-Zuordnung über `app_user_mandant`. Die Annahme „ein Mandant je Nutzer“ ist damit keine Ausgangslage mehr, sondern erledigt. `NEXANS`/`NXHBE` und `IBIS`/`IBISGUS` sind je dasselbe Haus; die Berechtigung wurde schon immer als **Menge** geprüft, es fehlte allein die Pflege. *Korrigiert 20.08.2026:* In der Spalte „Annahme“ stand „Ein Nutzer gehört zu genau einem Mandanten“, im Risiko „**Unter Druck.** `NEXANS`/`NXHBE` und `IBIS`/`IBISGUS` sind jeweils dasselbe Haus. `app_user_mandant` ist n:m vorbereitet, und der Wechsel prüft die Menge statt der Rolle — der Fall ist damit ohne Zusatzbau abgedeckt“ | — |
| A3 | Kein SMTP-Relay verfügbar, Passwort-Reset erfolgt durch Admin | Selbstbedienung fehlt, später nachrüstbar |
| ~~A4~~ | **Geklärt.** Rohdatenzugriff über Filestore-Links ist gewünscht und für alle Mandantennutzer freigegeben | — |
| ~~A5~~ | **Geklärt.** Eigenständiger Betrieb möglich, GlassFish nicht vorgeschrieben (Instanz wäre Version 6/7) | — |
| ~~A6~~ | **Widerlegt 27.07.2026.** `ERROR_*` ist **nicht** die vollständige Fehlerdefinition: `COMMIT_REJECTED` ist ein Fehler ohne Präfix. Siehe 4.1 | — |
| ~~A7~~ | **Bestätigt 27.07.2026.** Rund 1.500 Prozesse. *Korrigiert 12.08.2026:* Hier stand **1.490**; das war die `information_schema`-Schätzung. **Gezählt sind 1.503** — am 01.08.2026 ([`messungen-schritt4.md`](messungen-schritt4.md) M10) und erneut in M44. An der Annahme selbst ändert das nichts, sie ist weiterhin bestätigt | — |
| ~~A8~~ | **Geklärt 28.07.2026, bewusst akzeptiert.** **140** Projekte stehen 134 Zeilen in `ProjectMandant` gegenüber — sechs Projekte ohne Mandanten, und sie tragen zusammen **null** Nachrichten ([`annahmen-korrekturen.md`](annahmen-korrekturen.md)). *Korrigiert 12.08.2026:* Hier standen **142**; das war die `information_schema`-Schätzung. Gezählt sind 140, am 28.07.2026 und erneut in M44 am 12.08.2026. Nachrichten in Projekten ohne Zuordnung sind im Werkzeug für niemanden sichtbar, auch nicht für ADMIN. Es wird **kein** Sonderpfad und kein Pseudo-Mandant gebaut. Wichtig, dass das dokumentiert bleibt: Wird eine solche Nachricht gesucht, findet sie niemand, und ohne diesen Eintrag wüsste auch niemand warum | — |
| A9 | Die BAM-Suche lässt sich mit Zeitfenster und hartem Limit ausreichend begrenzen | `MessageBAM` hat keinen Zeitstempel, das Fenster greift erst nach dem Join. Rückfalloption: eigener BAM-Index in `overlord_monitor`, vom Rollup-Job mitgeführt |
| A10 | MariaDB 10.6 bleibt für die Laufzeit des Projekts in Betrieb | Version hat im Juli 2026 den Wartungszeitraum erreicht. Ein Upgrade auf 11.x ändert Standard-Sortierungen — deshalb steht die Sortierung in jeder Migration explizit |
| A11 | Die unverschlüsselte Verbindung zur Datenbank ist tragbar | **Offen.** Auf der Testkopie ist kein TLS eingerichtet. Tragbar, wenn Anwendungsserver und Datenbank im selben Rechenzentrum am selben Switch stehen; nicht tragbar über ein Firmennetz mit WLAN oder Standortkopplung. Vor dem Produktivbetrieb mit `SHOW VARIABLES LIKE 'have_ssl'` gegen die Produktion zu prüfen |

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