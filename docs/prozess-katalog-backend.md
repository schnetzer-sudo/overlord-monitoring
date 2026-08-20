# Prozess-Katalog — Backend

Stand: 20.08.2026 · Schritt 9b, Teil Backend
Fachliche Grundlage: [`prozess-katalog.md`](prozess-katalog.md) (E1–E13)
Messgrundlage: [`messungen-schritt9.md`](messungen-schritt9.md), M74 bis M79 · Nachtrag **M80**

**Kein Frontend.** Dieser Schritt liefert die Migration, die Heuristik, den Datenzugriff und fünf
Endpunkte. Die Oberfläche ist ein eigener Auftrag.

---

## 1. Was entstanden ist

| | |
|---|---|
| Migration | `V6__process_catalog.sql` |
| Heuristik | `catalog/Partnerheuristik` — reine Funktionen, ohne Spring, ohne Datenbank |
| Datenzugriff | `catalog/ProzessKatalogRepository` — zwei `DSLContext` |
| Fachlogik | `catalog/ProzessKatalogService` |
| Endpunkte | `catalog/ProzessKatalogController` — fünf, alle unter `/api/katalog` |
| Rollengrenze | eine Zeile in `config/SecurityConfig` |

Dazu die Aufzählungstypen `Pflegestatus`, `Richtung`, `VorschlagHerkunft`, `Zuordnungsfeld`,
`Massenmodus` und die Antwortsätze `KatalogzeileResponse`, `MassenzuordnungResponse`,
`VorschlagslaufResponse`.

**Was ausdrücklich nicht entstanden ist:** keine Tabelle `partner` (E2), keine Felder Standort und
Belegart (E1), keine Auswertung „seit wann kam nichts" (§8 der Festlegung — das ist Schritt 10 aus
`message_rollup`), keine Ableitung der Richtung aus dem `SOSName` (§9), keine gespeicherte Spalte
„trägt Nachrichten" (§9), kein dritter Pflegestatus (§9).

---

## 2. Die Tabelle

`overlord_monitor.process_catalog`, eine Zeile je `ProcessID`, Primärschlüssel `process_id` allein.

| Spalte | Typ | |
|---|---|---|
| `process_id` | `varchar(36)` | Primärschlüssel |
| `partner` | `varchar(100)` | NULL erlaubt — **ein leerer Partner ist ein gültiger gepflegter Zustand** (E4) |
| `richtung` | `varchar(20)` | `EINGEHEND` / `AUSGEHEND` / NULL |
| `pflegestatus` | `varchar(20)` | `OFFEN` / `GEPFLEGT`, NOT NULL |
| `vorschlag_herkunft` | `varchar(20)` | `REGEL_A` / `REGEL_B` / `KEINE`, NOT NULL |
| `geaendert_am` | `DATETIME(3)` | UTC, NOT NULL |
| `geaendert_von` | `varchar(100)` | Benutzername, NOT NULL |

Zeichensatz und Sortierung stehen explizit (`utf8mb4` / `utf8mb4_general_ci`), kein
`utf8mb4_bin` — hier steht nichts Tokenartiges. Kein Fremdschlüssel über die Schemagrenze; verwaiste
Einträge sind erwünscht. Keine Sekundärindizes und keine Vorbelegung. Begründungen stehen in der
Migration selbst und in [`datenzugriff.md`](datenzugriff.md) §5.

**`geaendert_am` ist `DATETIME(3)` und nicht `timestamp`** — siehe §9, Abweichung 1.

**`vorschlag_herkunft` beschreibt den Partner, nicht die Richtung.** Eine Zeile darf `KEINE` tragen
und trotzdem eine Richtung haben; das ist bei 224 `NEXANS`-Prozessen der Regelfall. Die Zählweise
folgt damit §3.5 der Festlegung, wo Regel A 887, Regel B 281 und „ohne Vorschlag" 322 Prozesse
zählt — allesamt Partnerzahlen.

---

## 3. Die Heuristik

`Partnerheuristik` ist eine Klasse ohne Zustand, ohne Spring-Annotation und ohne Datenbankzugriff.
Eingabe sind drei Zeichenketten (`mandantId`, `processId`, `projectId`), Ausgabe ist ein
`Partnervorschlag` — notfalls `Partnervorschlag.KEINER`.

Der Grund für diesen Schnitt steht in §10 der Festlegung: Die Regeln stehen unter offenen Punkten
und werden sich ändern. Eine Regeländerung darf keinen Service anfassen und keine Verbindung
brauchen.

### 3.1 Regel A — Präfix und Position

Führender `^[0-9]+_`-Präfix, dann ist **Token 2 der `ProcessID`** der Partnerkandidat.
Sie feuert bei **genau zwei oder drei** Unterstrichen, gezählt über die volle `ProcessID`
einschließlich des Präfix-Trenners. `SUTTONS` ist namentlich ausgenommen.

**Zwei Lesarten mussten entschieden werden, und beide sind an den Zahlen entschieden statt geraten.**

**(a) Token 2 wovon?** „Führenden Präfix abschneiden, Token 2 ist der Partnerkandidat" lässt sich
als Token 2 des *Rests* lesen. Die abgedruckte Gestalt entscheidet: `40000_AMG_LAB_VDA` hat den
Partner `AMG`. Token 2 des Rests wäre `LAB`. Gemeint ist Token 2 der **vollen** `ProcessID`, und das
Abschneiden ist die Bedingung, nicht der Schritt davor.

**(b) Ist der Nummernpräfix Bedingung oder nur Namensgeber?** Der Auftrag nennt als
Feuerbedingung nur die Unterstrichzahl. Dann aber träfe die Regel `NXHBE` (14 von 17 Prozessen mit
genau zwei Unterstrichen) und drei der vier `WOC`-Prozesse — beide führt §3.5 vollständig unter
„ohne Vorschlag", und beide haben laut M75 **keinen** Präfix. Mit dem Präfix als Bedingung geht die
Rechnung dagegen genau auf:

| Mandant | Unterstriche 2 | Unterstriche 3 | Regel A | §3.5 |
|---|---:|---:|---:|---:|
| `NEXANS` | 9 | 500 | **509** | 509 |
| `VOTG` | 118 | 260 | **378** | 378 |
| | | | **887** | **887** |

Und die Gegenprobe über die 322 ohne Vorschlag: `NEXANS` 6 (ein Unterstrich) + 168 (vier) + 50
(fünf) = **224**, `VOTG` 4 + 8 = **12** — beide Zahlen stehen so in §3.5. Damit ist die Lesart
belegt und nicht gewählt.

### 3.2 Regel B — CamelCase mit Richtungsanker

An `Eingehend` oder `Ausgehend` geteilt: was dahinter steht, ist der Partner, das Ankerwort selbst
ist die Richtung. Fehlt der Anker, gibt es **keinen** Vorschlag.

Der Anker gilt als **Wort**, nicht als Teilzeichenkette. Die Vorgabe sagt „an Großbuchstaben
zerlegen" — und genau das steht im Code: Ein Großbuchstabe *ist* in CamelCase ein Wortanfang, davor
braucht es keine weitere Bedingung. Gebraucht wird nur die Grenze **dahinter**: Kein Kleinbuchstabe
darf folgen, sonst wäre `LagerEingehendeMeldung` ein Treffer, obwohl das Folgezeichen noch zum
selben Wort gehört und alles „dahinter" geraten wäre. Die Prüfung ist fallunterscheidend; eine
durchgängig großgeschriebene Schreibweise ist ein anderer Fall.

> **Das ist die Fassung nach der Messung, nicht vor ihr.** Die erste Fassung verlangte zusätzlich
> einen Kleinbuchstaben *vor* dem Anker. Das war eine Erfindung und kostete **26** Prozesse —
> `IBIS` 20 und `IBISGUS` 6 tragen unmittelbar davor ein zweibuchstabiges Kürzel in Großschreibung.
> Gefunden hat es der Regressionstest gegen den Bestand (§7), und die Diagnose war eindeutig: Von
> den verfehlten Namen trugen **alle** den Anker in exakter CamelCase-Schreibweise, **keiner** in
> einer anderen. Es war die Wortgrenze und nichts sonst. Vollständig in
> [`messungen-schritt9.md`](messungen-schritt9.md) M80‑6.

**Auch hier eine entschiedene Lesart:** „Für Prozessnamen ohne Trennzeichen" ist als *Zweck*
gelesen und nicht als Bedingung — der Anker entscheidet. Im Bestand ist die Unterscheidung
**folgenlos**: Die sechs Prozesse mit Trennzeichen bei `IBIS`/`IBISGUS` tragen ohnehin keinen Anker.
Die Lesart bleibt trotzdem die des Dokuments, weil eine Bedingung, die nichts ausschließt, keine
Bedingung ist.

Ein führender Trenner am Partnerkandidaten wird entfernt: Ein Partner `_BAYER` wäre eine Behauptung
über Zeichensetzung, keine über den Namen. Steht hinter dem Anker gar nichts, gibt es eine Richtung
und **keinen** Partner — die Herkunft ist dann `KEINE`.

### 3.3 Die Richtung aus dem Projektnamen

Derselbe Anker, auf die Projektkennung angewandt (`300_KundenEingehend`,
`OrdersVerarbeitungEingehendNL`). Er wirkt **zusätzlich** zu beiden Regeln: Ein Prozess, für den
Regel A den Partner liefert, bekommt die Richtung von dort; ein Prozess ohne jeden Partnervorschlag
kann trotzdem eine Richtung tragen.

Die Prozesskennung schlägt die Projektkennung: Wo Regel B eine Richtung liefert, gilt sie — der
nähere Name gewinnt.

**Der `SOSName` wird nicht angefasst.**

### 3.4 Was die Heuristik nicht tut

Sie schlägt vor und entscheidet nicht. Jede geschriebene Zeile trägt `OFFEN`. Wo eine Regel nicht
sicher trifft, liefert sie **nichts** — nicht einen wahrscheinlichen Wert. `KEINE` heißt „geprüft,
nichts abgeleitet" und nicht „noch nicht gelaufen". Der Typ erzwingt das: Ein `Partnervorschlag` mit
Partner und Herkunft `KEINE` lässt sich gar nicht bauen.

---

## 4. Die Endpunkte

Pfade deutsch, Antwortfelder camelCase, Fehlertexte deutsch.

### `GET /api/katalog/prozesse`

Die Pflegeliste des aktiven Mandanten. Parameter `nurOffene` (Vorgabe `false`).

Je Zeile acht Felder: `processId`, `projectId`, `projectName`, `processName`, `partner`,
`richtung`, `pflegestatus`, `vorschlagHerkunft`.

- **Alle** Prozesse des Mandanten, auch die ohne Nachrichten (E5) und auch die ohne Katalogzeile —
  `process_catalog` hängt als `LEFT JOIN` daran.
- Prozesse ohne Katalogzeile erscheinen mit `pflegestatus = OFFEN`, `vorschlagHerkunft = KEINE` und
  leeren Feldern. Die Umsetzung des `NULL` passiert im Repository, damit kein Aufrufer die Frage
  anders beantwortet.
- Sortiert nach **`ProjectID`, dann `ProcessID`** (E6 in der eindeutigen Fassung vom 20.08.2026) —
  beides Schlüssel und damit stabil. Siehe §9, Abweichung 2.
- **Keine Paginierung** (E8), kein Zeitfenster. Die Fortschrittszahl bekommt keine eigene Abfrage;
  die volle Liste kommt zurück und wird vorne gezählt.
- `nurOffene=true` blendet gepflegte Zeilen aus. Prozesse **ohne** Katalogzeile bleiben drin — sonst
  verschwänden ausgerechnet die, die noch nie jemand angesehen hat.

### `GET /api/katalog/partner`

`SELECT DISTINCT partner` über die Katalogzeilen **des aktiven Mandanten**, leere Werte ausgelassen,
alphabetisch. Antwort ist eine Liste von Zeichenketten. **Keine Tabelle `partner`** (E2).

Der Mandantenbezug entsteht im Join über `ProjectMandant` und nicht in einer Spalte (E3): `BAYER`
bei `VOTG` und `BAYER` bei `SUTTONS` sind zwei Werte.

Ein leerer Partner ist ein gültiger *Pflegezustand*, aber kein wählbarer *Wert* — deshalb bleibt er
aus der Liste.

### `PUT /api/katalog/prozesse/{processId}`

Körper: `{"partner": …, "richtung": …}`, beide Felder dürfen fehlen oder `null` sein.
Setzt beide Felder und setzt `pflegestatus = GEPFLEGT`. Antwort ist die geänderte Zeile.

- **Ein leerer Partner ist gültig** und bedeutet „hingesehen, es gibt nichts" (E4). Ein Partner aus
  Leerzeichen wird zu leer — ein gefülltes Feld ohne Inhalt wäre ein dritter Zustand durch die
  Hintertür.
- Ein Partner über 100 Zeichen ist `400` (`partner-zu-lang`), eine unbekannte Richtung ist `400`
  (`richtung-unbekannt`). Ein unbekannter Wert fällt **nicht** stillschweigend auf leer zurück.
- `processId` außerhalb des aktiven Mandanten → `404`, nicht `403`, und nicht von einer erfundenen
  Kennung zu unterscheiden.
- Eintrag ins `audit_log` (`KATALOG_GEAENDERT`).
- `vorschlag_herkunft` bleibt beim Aktualisieren stehen: Sie beschreibt, welche Regelfassung diese
  Zeile einmal vorgeschlagen hat, und wird durch eine Kuratierung nicht falsch, sondern historisch.

### `POST /api/katalog/massenzuordnung`

Körper: `{"projectId": …, "feld": "PARTNER"|"RICHTUNG", "wert": …, "modus": "VORSCHAU"|"AUSFUEHREN"}`.
Setzt **ein** Feld für alle Prozesse eines Projekts (E11). Ohne `modus` gilt `VORSCHAU` — wer den
Modus vergisst, verändert nichts.

Antwort: `modus`, `projectId`, `feld`, `wert`, `betroffen`, `davonGepflegt`.

- **Beide Modi teilen ein Statement.** `speichereFeld` ruft dieselbe Methode
  `findeProjektbestand(mandant, projectId)`, die auch die Vorschau nennt, und schreibt genau auf die
  Zeilen, die sie liefert. Ein Attrappentest vergleicht die beiden gerenderten Statements Zeichen
  für Zeichen.
- Die Vorschau nennt die betroffenen Zeilen **und** davon die bereits gepflegten — das ist die Zahl,
  die verloren geht.
- **Sie überschreibt gepflegte Zeilen** (E12). Das ist gewollt: Der Schutzmodus „nur offene Zeilen"
  machte genau die Korrektur unmöglich, für die man sie braucht.
- Das jeweils andere Feld bleibt unangetastet.
- Projekt außerhalb des aktiven Mandanten → `404`, ununterscheidbar von einem erfundenen. Die
  Existenzprüfung läuft über `ProjectMandant` und nicht über die Zeilenzahl — ein Projekt **ohne**
  Prozesse gäbe sonst dieselbe leere Antwort wie ein fremdes, und das ist der Unterschied zwischen
  „nichts zu tun" und `404`.
- Nur `AUSFUEHREN` schreibt einen `audit_log`-Eintrag.

Größtes Projekt im Bestand: 226 Prozesse (`VOTG`, `110_VTG_SalesInvoice`), 161 bei `NEXANS`.

### `POST /api/katalog/vorschlagen`

Heuristik-Lauf für den aktiven Mandanten (E13), leerer Körper.
Antwort: `angelegt`, `aufgefrischt`, `unberuehrt`, `regelA`, `regelB`, `keine`.

| | |
|---|---|
| legt an | fehlende Zeilen |
| frischt auf | Zeilen mit `OFFEN`, **auch wenn sie schon einen Vorschlag tragen** |
| rührt nie an | Zeilen mit `GEPFLEGT` |

Die ersten drei Zahlen ergeben zusammen die Zahl der Prozesse des Mandanten. Die letzten drei zählen
nur die **geschriebenen** Zeilen nach der Herkunft ihres Partnervorschlags; unberührte Zeilen tragen
ihre eigene, ältere Herkunft.

**Nicht beim Anwendungsstart.** Kein `ApplicationRunner`, kein `@PostConstruct`, kein Scheduler.
Ein `audit_log`-Eintrag hält jeden Lauf mit seinen Zahlen fest.

Für die Unterscheidung „anlegen" gegen „auffrischen" gibt es eine eigene Lesung
(`findeBestand`): Die Pflegeliste ebnet den Unterschied bewusst ein, weil ein Prozess ohne
Katalogzeile für den Nutzer genau `OFFEN` ist — der Lauf muss ihn aber auseinanderhalten.

---

## 5. Mandantentrennung und Rollengrenze

Der Katalog erbt M1 bis M4 unverändert (§7 der Festlegung).

- **Kein Endpunkt nimmt eine Mandanten-ID entgegen** (M1). Auch der Heuristik-Lauf läuft für den
  aktiven Mandanten und nicht für alle.
- **`MandantContext` ist erster Pflichtparameter jeder Repository-Methode** (M2) — auch der rein
  schreibenden. ArchUnit erzwingt das klassenweit, sobald eine Klasse `jooq.glassfish` berührt, und
  hier ist das kein Formalismus: `process_catalog` trägt keine Mandantenspalte, eine Schreibmethode
  ohne Mandantennachweis änderte deshalb ohne Weiteres fremde Kuratierung.
- **Der Filter über `ProjectMandant` ist Bestandteil jedes lesenden Statements** (M3). Wer eine
  fremde `ProcessID` errät, bekommt null Zeilen. Die schreibenden Statements setzen ihn nicht noch
  einmal, sondern schreiben ausschließlich auf Zeilen, die eine mandantengefilterte Lesung ergeben
  hat.
- **Ein Isolationstest je Endpunkt** (M4), fünf an der Zahl.

**Zusätzlich eine Rollengrenze.** `/api/katalog/**` verlangt `ADMIN`:

```java
.requestMatchers("/api/katalog/**")
.hasRole("ADMIN")
```

Sie steht in `SecurityConfig` und nicht als Annotation am Controller — dieselbe Entscheidung wie bei
`AdminUserController`: Die Regel soll an einer Stelle stehen. Siehe §9, Abweichung 3.

Weil ein Administrator für **alle** Mandanten berechtigt ist, hat er keinen automatisch gewählten
Mandanten und bekommt ohne ausdrückliche Wahl `403`. Das ist geprüft.

---

## 6. Zwei Fallen, die zugeschnappt sind

Beide sind von Tests gefunden worden, nicht beim Lesen.

**1. Der Bindezähler des Stapels.** Die Massenzuordnung und der Heuristik-Lauf schreiben über
`DSLContext.batch(…)`. In der Vorlage standen zwei Statuswerte als **feste** Werte
(`Pflegestatus.GEPFLEGT.name()`), und genau die sind ebenfalls Bindeplätze — die Bindeliste war um
zwei Werte zu kurz. jOOQ behandelt das **nicht** als Fehler: Es schreibt eine Zeile ins Protokoll
(`Batch bind value set 0 has 4 values when 6 values were expected`) und füllt den Rest mit `null`.
Jede Zeile wäre um Spalten verschoben geschrieben worden. Behoben ist es, indem jede Zeile der
Vorlage ein `null`-Platzhalter ist und alle Werte in derselben Reihenfolge gebunden werden;
`ProzessKatalogStatementsTest.stapelsaetze_binden_vollstaendig` zählt die `?` im gerenderten Text
gegen jeden Bindesatz und fängt einen Rückfall.

**2. `@Transactional` bindet nur den Schreib-Kontext.** Beschrieben in
[`datenzugriff.md`](datenzugriff.md) §5 beim Abschnitt zu `V6`.

---

## 7. Tests

| Datei | Art | Fälle |
|---|---|---|
| `PartnerheuristikTest` | Unit, ohne Datenbank | 22 |
| `ProzessKatalogStatementsTest` | Unit, jOOQ-Attrappe | 14 |
| `ProzessKatalogDbIT` | `@Tag("db")` | 13 |
| `ProzessKatalogIsolationDbIT` | `@Tag("db")` | 13 |
| `HeuristikBestandDbIT` | `@Tag("db")`, **schreibt nichts** | 2 |

**`HeuristikBestandDbIT` ist der Test, der sich bezahlt gemacht hat, bevor er einmal grün war.** Er
lässt die Heuristik über den echten Prozessbestand jedes Mandanten laufen und vergleicht die
Trefferzahlen mit §3.5. Beim ersten Lauf war er rot — und der Befund war nicht die Zahl, sondern
eine erfundene Bedingung im eigenen Code (§3.2). Er zählt **je Mandant** und nicht nur die Summe:
Die beiden gefundenen Abweichungen unterscheiden sich in der Summe um **eins** und wären an ihr
unsichtbar geblieben. Und er berichtet alle zehn Mandanten in einem Durchgang, statt beim ersten
Unterschied abzubrechen — beim Bau war genau die vollständige Liste der Befund.

**`PartnerheuristikTest`** legt den Schwerpunkt auf die Negativfälle: vier und fünf Unterstriche,
ein Unterstrich, fehlender Nummernpräfix, `SUTTONS`, fehlender Anker, Anker am Wortende, ein Anker
ohne Wortgrenze, leere und fehlende Eingabe. Eine Heuristik, die zu viel liefert, ist schlimmer als
eine, die zu wenig liefert.

**`ProzessKatalogStatementsTest`** prüft den gerenderten Text: Mandantenfilter in jedem lesenden
Statement, beide Schemata voll qualifiziert, `LEFT JOIN` auf `process_catalog`, keine Paginierung,
kein Zugriff auf `Message`, jedes Schreiben ein Upsert, kein Schreibzugriff berührt `GlassfishDB`,
die Massenzuordnung setzt genau ein Feld — und Vorschau und Ausführung fahren denselben Text.
Weil jOOQ Werte bindet statt sie einzusetzen, prüft er die Bindewerte daneben; eine reine
Textprüfung auf `'OFFEN'` ginge still ins Leere.

**`ProzessKatalogDbIT`** fährt auf `SUTTONS` — bewusst der kleinste Bestand: 17 Prozesse, ein
Projekt. Die Fragen hängen an der Logik und nicht an der Menge, und jeder Schreibvorgang ist ein
Commit auf einer gemeinsam genutzten Datenbank. Dass die Heuristik dort **nichts** liefert, ist ein
Vorteil: Jeder Partner in diesen Tests ist von Hand gesetzt und keiner aus einer Regel.

**`ProzessKatalogIsolationDbIT`** fährt `VOTG` gegen `SUTTONS` mit zwei Administratoren, die ihren
Mandanten ausdrücklich wählen. Je Endpunkt ein Nachweis, dazu die Rollengrenze (ein
`MANDANT`-Nutzer bekommt auf allen fünf Endpunkten `403`, erreicht seine eigene Nachrichtenfläche
aber sehr wohl), der Administrator ohne Mandantenwahl, und Regel M1 in zwei Formen — als
Abfrageparameter und als untergeschobenes Feld im Anfragekörper.

**Aufgeräumt wird über `geaendert_von`** mit dem Testpräfix `it-`. Eine von Hand kuratierte Zeile
überlebt jeden Testlauf.

---

## 8. Messung

Vollständig als Nachtrag **M80** in [`messungen-schritt9.md`](messungen-schritt9.md), gefahren am
20.08.2026 gegen die Testkopie mit **gefülltem** Katalog (1.490 Zeilen über den echten Codepfad;
nach der Messung wieder gelöscht). Das Kurzergebnis:

| Statement | Mandant | Laufzeit (beste von fünf) |
|---|---|---:|
| **Pflegeliste** | `NEXANS` (733 Zeilen) | **8,000 ms** |
| **Pflegeliste** | `SUTTONS` (17 Zeilen) | **0,812 ms** |
| dieselbe ohne Katalog-Join | `NEXANS` | 4,764 ms |
| Pflegeliste `nurOffene` | `NEXANS` | 8,006 ms |
| Partnerliste | `NEXANS` / `SUTTONS` | 4,084 / 0,681 ms |
| Projektbestand (Massenzuordnung, 226 Prozesse) | `VOTG` | 1,688 ms |

**`process_catalog` hängt als `eq_ref` auf `PRIMARY` am Plan** — der bestmögliche Zugriff, bei
beiden Mandanten identisch, Einstiegstabelle unverändert `ProjectMandant`. Regel L15 hat nicht
zugeschlagen, kein `STRAIGHT_JOIN`.

**Der Join kostet +3,24 ms, also 68 %.** Das ist harmlos, weil die Ausgangszahl klein ist — aber es
ist mehr, als „ein `eq_ref` kostet nichts" vermuten lässt, und deshalb steht die Zahl hier und nicht
nur im Messdokument. Keine Paginierung nötig, E8 bleibt.

**Der Vorbehalt an genau diese Zahl gehört danebengestellt:** Die **+68 % sind keine
Produktionszahl** — ein Verhältnis zweier kleiner Zahlen, beste von fünf, auf der Testkopie ohne
Nebenlast. Am **Umfang** liegt das nicht: Der Katalog stand bei der Messung mit **1.490 Zeilen** auf
voller Größe, eine je erreichbarem Prozess. Nachgemessen wird nicht; wird die Aussage später
gebraucht, wird sie gegen die Produktion neu erhoben.

**Eine Stelle ist seit der Messung nicht mehr deckungsgleich:** Gemessen ist `ORDER BY ProjectName,
ProcessName`, ausgeliefert wird `ORDER BY ProjectID, ProcessID` (§9, Abweichung 2). Derselbe
Sortierschritt über dieselbe Zeilenmenge — aber nicht nachgemessen.

**Zwei Zahlen sind ausdrücklich *nicht* gemessen:** was der Filter `nurOffene` im Betrieb spart (im
Messzustand stand alles auf `OFFEN`, er sparte nichts — gemessen ist seine obere Schranke), und der
Schreibweg, der über den Schreib-Pool läuft und nicht `EXPLAIN`-bar ist. Belegt ist dort nur, dass
1.490 Zeilen über zehn Transaktionen in unter zehn Sekunden entstehen.

---

## 9. Abweichungen von der Festlegung und vom Auftrag

Jede Stelle, an der der Bau von `prozess-katalog.md` oder vom Auftragstext abweicht, mit Grund.

**1. `geaendert_am` ist `DATETIME(3)`, nicht `timestamp`.** Der Auftrag nennt in der Spaltentabelle
`timestamp`. Der Hausstil verbietet `TIMESTAMP` ausdrücklich — wegen Zeitzonen und der 2038-Grenze
steht in `V1` und `V2` `DATETIME(3)` in UTC, mit Begründung im Migrationstext. Eine `TIMESTAMP`-
Spalte wäre die erste des Schemas gewesen.

**2. ~~Sortiert wird nach `ProjectName`, nicht nach `ProjectID`.~~ — zurückgenommen am
20.08.2026.** Der Auftrag schreibt „sortiert nach Projekt, dann Prozesskennung" und beruft sich
dabei auf E6. E6 sagte „Projekt **und Name**", und M79 hatte die Stelle bereits benannt: *„Die im
Auftrag abgedruckte Abfrage sortiert dagegen nach `p.ProjectID, p.ProcessID` — das ist **nicht**
die beschlossene Sortierung. Gemessen ist die beschlossene."* Gebaut war daraufhin
`ORDER BY ProjectName, ProcessName`.

**Zurückgenommen ist die Abweichung nicht, weil ihre Auflösung falsch war, sondern weil E6
mehrdeutig formuliert war.** „Projekt und Name" lässt beide Lesarten zu. Am 20.08.2026 ist E6
eindeutig gemacht worden — `ProjectID`, dann `ProcessID` —, und damit fällt die Abweichung weg:
Beides sind Schlüssel und damit stabil, während `ProcessName` über den Bestand nicht eindeutig ist
und eine Liste ohne Paginierung nach ihm keine feste Reihenfolge hätte. Gebaut ist seither
`ORDER BY Process.ProjectID, Process.ProcessID` — die Projektkennung aus `Process` und nicht aus
dem `LEFT JOIN` auf `Project`, weil sie dort nie `null` ist und weil sie es ist, die in der Antwort
steht.

**Übrig bleibt ein Vorbehalt an die Messung und keiner am Bau:** L12 und M80‑1 haben
`ORDER BY ProjectName, ProcessName` gefahren. Der ausgelieferte Text ist an dieser einen Stelle
nicht mehr der gemessene (§8). Die Nummer 2 bleibt vergeben, damit Verweise auf sie nicht ins Leere
zeigen; es stehen damit **sechs** Abweichungen und eine zurückgenommene.

**3. Die Antwort trägt ein achtes Feld: `projectName`.** Der Auftrag zählt sieben Felder auf und
nennt `projectId`, nicht `projectName`. **Das Feld bleibt, seine Begründung ändert sich mit
Abweichung 2: Es dient der Anzeige und nicht dem Nachvollziehen der Reihenfolge.** Sortiert wird
seit dem 20.08.2026 nach Kennungen; lesbar ist die Zeile trotzdem erst mit dem Namen, denn
`ProjectName` und `ProjectID` sind verschiedene Zeichenketten (M75, `ProjectID` trägt bei 67 von
140 Projekten einen Unterstrich, `ProjectName` bei keinem). Das Feld ist additiv; die sieben
beauftragten sind unverändert da.

**4. `/api/katalog/**` verlangt die Rolle `ADMIN`.** Der Auftrag nennt keine Rolle. Ohne eine Regel
fiele der Katalog unter `anyRequest().authenticated()`, und jeder angemeldete Nutzer könnte die
Kuratierung seines Mandanten ändern. `IMPLEMENTIERUNGSPLAN_MVP.md` führt die Katalogpflege im
Administrationsbereich („nur für die Rolle `ADMIN` sichtbar"), `PROJEKTBESCHREIBUNG.md` §9 ebenso.
**Vom Auftraggeber am 20.08.2026 ausdrücklich entschieden**, nicht angenommen.

**5. Protokolliert werden alle drei schreibenden Vorgänge, nicht nur die einzelne Zuordnung.** Der
Auftrag verlangt einen `audit_log`-Eintrag nur bei `PUT`. Eine Massenzuordnung überschreibt bis zu
226 gepflegte Zeilen und ein Heuristik-Lauf berührt jede offene Zeile eines Mandanten; wären die
beiden nicht im Protokoll, wäre ausgerechnet die folgenreichste Änderung die unsichtbarste.
`AuditEventType.KATALOG_GEAENDERT` existiert seit Schritt 2, es kostet keine Migration.

**6. Zwei Lesarten der Regeln sind entschieden worden**, beide an den Zahlen aus §3.5 belegt und in
§3.1 und §3.2 ausgeschrieben: der Nummernpräfix als Bedingung von Regel A, und „ohne Trennzeichen"
als Zweckbeschreibung statt als Bedingung von Regel B. Ohne die erste bekämen 14 von 17
`NXHBE`-Prozessen einen Vorschlag, den §3.5 nicht kennt.

**6a. Die Zahlen aus §3.5 sind nicht erreichbar, und das ist gemessen** (M80‑6). Regel A trifft die
Projektion auf den Prozess genau. Regel B liefert **280 statt 281** — sechs `IBIS`/`IBISGUS`-Prozesse
tragen das Ankerwort überhaupt nicht und sind für **keine** Fassung der Regel erreichbar, dafür
trifft sie fünfmal bei `EDITIONLINGERI`, wo §3.5 sie nicht erwartet. Die Festlegung trägt seit dem
20.08.2026 einen datierten Korrekturkasten dazu; der alte Wortlaut steht unverändert daneben.

**6b. Regel B ist nach der ersten Messung korrigiert worden.** Die zuerst gebaute Fassung verlangte
vor dem Anker einen Kleinbuchstaben — eine Bedingung, die in der Vorgabe nicht steht. Sie kostete 26
Prozesse. Das ist die einzige Stelle, an der der Bau von seiner *eigenen* ersten Fassung abweicht,
und sie steht hier, weil eine stillschweigend behobene Erfindung dasselbe verschweigt wie eine
stillschweigende Abweichung.

**7. `SicherheitsTestbasis` hat einen `PUT`-Helfer bekommen.** Bis zum 20.08.2026 gab es im gesamten
Backend kein einziges `PUT`; ohne den Helfer wäre der Endpunkt nicht prüfbar gewesen. Er setzt den
CSRF-Kopf wie `sende(…)`.

---

## 10. Offene Punkte

1. **Drei der vier offenen Punkte aus [`prozess-katalog.md`](prozess-katalog.md) §10 bleiben offen,
   der vierte ist für vier seiner fünf Mandanten beantwortet.**
   - *Offen:* `ZAST` (35) und `NXHBE` (17) haben kein Verfahren für den **Partner**; ob `WOC` (4)
     und `SYSTEM` (4) überhaupt kuratiert werden sollen, ist nicht entschieden; und Regel A bei
     **zwei** Unterstrichen ist für die neun `NEXANS`-Prozesse dieser Gestalt weiterhin nicht
     geprüft. Der Bau nimmt keine dieser Entscheidungen vorweg.
   - *Beantwortet (M80‑7):* Der Anker in **Projektnamen** wirkt bei `NXHBE` für **alle 17**
     Prozesse und bei `ZAST`, `WOC` und `SYSTEM` **gar nicht**. **Offen bleibt dort allein
     `EDITIONLINGERI`:** Seine fünf Zeilen mit Richtung sind genau die fünf, die Regel B über den
     **Prozess**namen trifft — was seine Projektnamen tragen, ist an dieser Zählung nicht
     ablesbar.
   - *Und einer ist kleiner geworden:* `EDITIONLINGERI` steht nicht mehr vollständig ohne
     Verfahren — Regel B trifft dort bei fünf von neun Prozessen.
2. **Der Änderungsvermerk kennt kein „von wem angelegt".** `geaendert_von` trägt nach einem
   Heuristik-Lauf den Namen dessen, der den Knopf gedrückt hat — nicht den einer Kuratierung. Wer
   die beiden unterscheiden will, liest das `audit_log`; an der Zeile selbst steht nur der letzte
   Anfasser. Das ist bewusst so und hier vermerkt, damit niemand die Spalte später als Kuratierungs-
   nachweis liest.
3. **Es gibt keinen Sekundärindex auf `pflegestatus`.** Der Filter `nurOffene` arbeitet innerhalb
   der Prozessmenge eines Mandanten (höchstens 733 Zeilen) und wird vom Join getrieben. Bei einem
   deutlich größeren Bestand wäre die Frage neu zu stellen.
4. **Die Sonderbehandlung von `00001_Undefined` ist nicht gebaut.** Sie steht im Plan unter
   Schritt 9b, gehört aber laut M78 in die Oberfläche — und der Filter dafür darf nicht `^0+_` sein,
   sonst behandelt er vier reguläre Prozesse als Auffangbecken.
5. **Eine Sichtprüfung im Browser steht aus**, wie bei jedem Backend-Teil. Sie ist erst mit der
   Oberfläche möglich.
