# Prozess-Katalog — Backend

Stand: 21.08.2026 · Schritt 9b, Teil Backend
Fachliche Grundlage: [`prozess-katalog.md`](prozess-katalog.md) (E1–E21)
Messgrundlage: [`messungen-schritt9.md`](messungen-schritt9.md), M74 bis M79 · Nachträge **M80** und
**M83** (Bestandsabfrage zu E14)

**Kein Frontend.** Dieser Schritt liefert die Migration, die Heuristik, den Datenzugriff und fünf
Endpunkte. Die Oberfläche ist ein eigener Auftrag.

---

## 1. Was entstanden ist

| | |
|---|---|
| Migration | `V6__process_catalog.sql`, dazu `V8__bestandsspalte.sql` (21.08.2026, E14) |
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
`message_rollup`), keine Ableitung der Richtung aus dem `SOSName` (§9), kein dritter Pflegestatus
(§9).

> **Berichtigt 21.08.2026.** Hier stand zusätzlich *„keine gespeicherte Spalte ‚trägt Nachrichten'
> (§9)"*. Das gilt nicht mehr: **E14** kehrt die Verwerfung um, `V8__bestandsspalte.sql` legt
> `traegt_nachrichten` und `bestand_geprueft_am` an, und der Bestandslauf füllt sie (§3.5). Der alte
> Wortlaut ist in [`messungen-schritt9.md`](messungen-schritt9.md) unter „Die zwei Sätze, die E14
> umkehrt" festgehalten.
>
> **Was weiter nicht entsteht, ist die Auswertung daneben:** „seit wann kam von diesem Partner
> nichts" bleibt Schritt 10 und bleibt Sache von `message_rollup`. E14 beantwortet **ob**, nicht
> **seit wann**.

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
| `traegt_nachrichten` | `BOOLEAN` | **drei Zustände:** NULL = noch nie geprüft, `false` = geprüft und tot, `true` = geprüft und lebend (E14, `V8`) |
| `bestand_geprueft_am` | `DATETIME(3)` | UTC, NULL erlaubt — der Zeitpunkt des letzten Bestandslaufs über diese Zeile (E14, `V8`) |

Zeichensatz und Sortierung stehen explizit (`utf8mb4` / `utf8mb4_general_ci`), kein
`utf8mb4_bin` — hier steht nichts Tokenartiges. Kein Fremdschlüssel über die Schemagrenze; verwaiste
Einträge sind erwünscht. Keine Sekundärindizes und keine Vorbelegung. Begründungen stehen in der
Migration selbst und in [`datenzugriff.md`](datenzugriff.md) §5.

**`geaendert_am` ist `DATETIME(3)` und nicht `timestamp`** — siehe §9, Abweichung 1.
`bestand_geprueft_am` folgt derselben Wahl.

**Die beiden letzten Spalten sind nicht kuratiert, sondern beobachtet** (E14). Sie tragen deshalb
**keinen** Standardwert und **keinen** Index: `NULL` muss von `false` unterscheidbar bleiben (E20),
und gefiltert wird im Browser und nicht in der Datenbank. Der Bestandslauf steht in §3.5.

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

### 3.5 Der Bestandslauf *(21.08.2026, E14–E16)*

**Ein Knopf, drei Schritte.** `POST /api/katalog/vorschlagen` fährt seit dem 21.08.2026 nicht mehr
nur die Heuristik, sondern erhebt anschließend den Bestand.

| | Was er tut | Wen er anfasst |
|---|---|---|
| **1** | fehlende Katalogzeilen anlegen | Prozesse ohne Zeile |
| **2** | Partner und Richtung vorschlagen | **nur `OFFEN`** — `GEPFLEGT` bleibt unberührt (E13) |
| **3** | Bestand erheben | **alle** Zeilen des Mandanten, **auch `GEPFLEGT`** (E15) |

**Schritt 2 und Schritt 3 sind verschieden vorsichtig, und das ist keine Nachlässigkeit.** E13
schützt **Kuratierung**, nicht **Beobachtung**: Partner, Richtung, Pflegestatus und Herkunft hat ein
Mensch entschieden; `traegt_nachrichten` sagt, was die Datenbank sagt. Eine Beobachtung, die für
gepflegte Zeilen stehenbliebe, wäre nach dem ersten Lauf falsch — und gerade dort ist sie wertvoll:
„kuratiert **und** ohne Verkehr" ist die Aussage, auf die Schritt 10 aufsetzt.

**Im Code sind es deshalb zwei getrennte Methoden mit sprechenden Namen** —
`speichereVorschlaege` und `speichereBestandsflags` — und kein Schalter an einer. Als Parameter wäre
E15 beim Lesen nicht zu erkennen und würde beim nächsten Anfassen „repariert".

**Schritt 3 läuft *nach* Schritt 1 und 2.** Danach trägt jeder Prozess des Mandanten eine
Katalogzeile, und das `UPDATE` findet sie alle. Andersherum gingen die eben angelegten Zeilen leer
aus und stünden bis zum nächsten Knopfdruck auf „noch nie geprüft".

#### Das Statement — Fassung A aus M83‑1

```sql
SELECT p.ProcessID,
       EXISTS (SELECT 1 FROM GlassfishDB.Message m WHERE m.ProcessID = p.ProcessID) AS traegt
FROM GlassfishDB.ProjectMandant pm
JOIN GlassfishDB.Process     p  ON p.ProjectID = pm.ProjectID
WHERE pm.MandantID = ?;
```

**Die Form ist gemessen und nicht frei gewählt.** Die naheliegende Alternative — `SELECT DISTINCT
m.ProcessID FROM Message …` und das Komplement im Dienst bilden — kostet **4.797 ms** statt 23 und
damit **Faktor 216,5** (M83‑2). Der Grund ist keine Feinheit des Plans, sondern die Bezugsgröße:
**Fassung A zahlt je Prozess, Fassung B je Nachricht.** Die Prozesszahl steht still, die
Nachrichtenzahl wächst — Fassung A wird nicht teurer, Fassung B schon.

Sie liefert `true` **und** `false`, nicht nur die lebenden Prozesse: Der Lauf schreibt beides, und
eine Fassung, die nur die lebenden nennt, verlöre den Unterschied zwischen „geprüft und tot" und
„nie geprüft".

Gelesen wird über den **Lese-Pool** (`glassfishDsl`), geschrieben über den **Schreib-Pool** — wie
überall in dieser Klasse. Kein `STRAIGHT_JOIN`, auch nicht als Reparatur (M42).

#### Die drei Zustände der Spalte

| Wert | Bedeutung |
|---|---|
| `NULL` | **noch nie geprüft** — für diese Zeile hat nie ein Bestandslauf stattgefunden |
| `false` | geprüft, es hängt **keine** Nachricht daran |
| `true` | geprüft, es hängen Nachrichten daran |

`NULL` wird **nicht** auf `false` abgebildet, anders als bei `pflegestatus` (→ `OFFEN`) und
`vorschlag_herkunft` (→ `KEINE`). Dort gibt es einen sinnvollen Ersatzwert, hier nicht: Der Filter
aus E20 muss ungeprüfte Zeilen **zeigen**, sonst verschwindet eine nie gemessene Zeile aus **beiden**
Filterstellungen. `KatalogzeileResponse.traegtNachrichten` ist deshalb `Boolean` und nicht `boolean`.

#### Das Schreiben ist ein `UPDATE` und bewusst **kein** Upsert

Es ist der einzige Schreibweg des Katalogs, der das ist — und der einzige, der auf gepflegte Zeilen
geht. Ein `INSERT … ON DUPLICATE KEY UPDATE` müsste die `NOT NULL`-Spalten mitliefern
(`pflegestatus`, `vorschlag_herkunft`, `geaendert_am`, `geaendert_von`) und überschriebe damit genau
die Kuratierung, die E15 unangetastet lässt. Das `UPDATE` fasst **ausschließlich die zwei
Beobachtungsspalten** an.

**`geaendert_am` und `geaendert_von` bleiben stehen.** Der Bestandslauf ist keine Änderung an der
Zeile im Sinne der Kuratierung; er trägt seinen eigenen Zeitstempel in `bestand_geprueft_am`. Zöge
er `geaendert_am` mit, sähe nach jedem Knopfdruck jede Zeile des Mandanten frisch bearbeitet aus.

**Die Uhr ist `systemClock`**, wie bei `geaendert_am` und beim `audit_log` (Regel A5) — **nicht** die
Anwendungsuhr. Sie ist im Profil `dev` um den Rückstand der Testkopie zurückversetzt; ein
Prüfzeitpunkt Wochen in der Vergangenheit wäre keiner, und zwei Zeitstempel derselben Zeile lägen
Wochen auseinander. Regel Z1 verbietet den direkten `now()`-Aufruf und schreibt **keine** der beiden
Uhren vor — sie verlangt für Protokollzeit ausdrücklich `systemClock`.

#### Die Antwort nennt zwei Zahlen

`VorschlagslaufResponse` trägt zusätzlich `bestandGeprueft` und `ohneNachrichten`. **Ohne sie ist
ein reihenweise wirkungsloser Lauf von einem erfolgreichen nicht zu unterscheiden** — dieselbe Falle
wie bei der Zahl verworfener Sitzungen in Schritt 9a. Die beiden Zahlen sind mit `angelegt`,
`aufgefrischt` und `unberuehrt` **nicht** zu verrechnen: Sie zählen einen anderen Schritt über
dieselbe Menge.

`00001_Undefined` wird dabei markiert und zählt mit (E16). Ein Filter auf `^0+_` gäbe es nicht — er
fängt vier reguläre Prozesse mit, darunter einen mit 1.602 Nachrichten (M78).

---

## 4. Die Endpunkte

Pfade deutsch, Antwortfelder camelCase, Fehlertexte deutsch.

### `GET /api/katalog/prozesse`

Die Pflegeliste des aktiven Mandanten. Parameter `nurOffene` (Vorgabe `false`).

Je Zeile zehn Felder: `processId`, `projectId`, `projectName`, `processName`, `partner`,
`richtung`, `pflegestatus`, `vorschlagHerkunft`, `traegtNachrichten`, `bestandGeprueftAm`.

- `traegtNachrichten` ist **nullable** und trägt drei Zustände (E14, §3.5): `null` = noch nie
  geprüft, `false` = geprüft und ohne Verkehr, `true` = geprüft und mit. `null` wird **nicht** auf
  `false` abgebildet.
- `bestandGeprueftAm` ist UTC und `null`, solange kein Bestandslauf über die Zeile ging. Die
  Oberfläche leitet daraus das Alter der Erhebung für die ganze Liste ab; **ein zusätzliches Feld
  im Umschlag gibt es dafür bewusst nicht.**

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

Heuristik-Lauf **und Bestandslauf** für den aktiven Mandanten (E13, E14), leerer Körper.
Antwort: `angelegt`, `aufgefrischt`, `unberuehrt`, `regelA`, `regelB`, `keine`,
`bestandGeprueft`, `ohneNachrichten`.

| | |
|---|---|
| legt an | fehlende Zeilen |
| frischt auf | Zeilen mit `OFFEN`, **auch wenn sie schon einen Vorschlag tragen** |
| rührt nie an | Zeilen mit `GEPFLEGT` — **soweit es die Kuratierung angeht** |
| erhebt den Bestand auf | **allen** Zeilen des Mandanten, auch den gepflegten (E15, §3.5) |

Die ersten drei Zahlen ergeben zusammen die Zahl der Prozesse des Mandanten. Die drei danach zählen
nur die **geschriebenen** Zeilen nach der Herkunft ihres Partnervorschlags; unberührte Zeilen tragen
ihre eigene, ältere Herkunft.

**`bestandGeprueft` und `ohneNachrichten` sind mit den übrigen nicht zu verrechnen** — sie zählen
den dritten Schritt über dieselbe Menge. `bestandGeprueft` entspricht der Zahl der Prozesse des
Mandanten; `ohneNachrichten` ist die Teilmenge ohne jede Nachricht im Bestand. Ohne die beiden wäre
ein reihenweise wirkungsloser Lauf von einem erfolgreichen nicht zu unterscheiden.

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
| `ProzessKatalogStatementsTest` | Unit, jOOQ-Attrappe | **16** |
| `ProzessKatalogDbIT` | `@Tag("db")` | **17** |
| `ProzessKatalogIsolationDbIT` | `@Tag("db")` | 13 |
| `HeuristikBestandDbIT` | `@Tag("db")`, **schreibt nichts** | 2 |
| `BestandslaufDbIT` | `@Tag("db")`, **schreibt nichts** | **4** |

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

**`BestandslaufDbIT`** ist das Gegenstück zu `HeuristikBestandDbIT` für E14: Er liest die
Bestandsabfrage gegen **zwei** Mandanten (Regel L7) und hält sie gegen die Gegenprobe aus M83‑5 —
`NEXANS` **733 / 516 / 217**, `SUTTONS` **17 / 17 / 0**. Er schreibt nichts.

**`SUTTONS` ist dabei die schärfere Probe, obwohl es der kleinere Bestand ist.** Dort **muss** die
Menge der toten Prozesse leer bleiben. Eine Fassung, die versehentlich Zeilen erzeugt — ein Join zu
viel, ein `LEFT` statt eines inneren —, fällt genau dort auf und bei `NEXANS` nicht, wo 217 tote
Prozesse ohnehin erwartet werden. Dazu die Doublettenprobe (`COUNT` gegen `COUNT(DISTINCT …)`, wie
M83‑5 sie gefahren hat) und die Mandantentrennung.

**Vier neue Fälle in `ProzessKatalogDbIT`** decken E14 und E15 am laufenden Endpunkt ab: `null` vor
dem ersten Lauf und auch nach einer Zuordnung (Kuratierung ist keine Erhebung); der Lauf setzt das
Flag und meldet beide Zahlen; **eine `GEPFLEGT`-Zeile bekommt das Flag, während `unberuehrt = 1`
bleibt und Partner, Richtung und Pflegestatus stehenbleiben** — E13 und E15 in einem Test; und eine
Zuordnung nach dem Lauf trägt den erhobenen Bestand weiter, weil die Antwort gebaut und nicht
nachgelesen wird.

> **Eine bestehende Zusicherung ist dabei geschärft worden, nicht aufgeweicht.**
> `schreibende_statements_sind_upserts` verlangte von **jedem** Schreiben ein `INSERT … ON DUPLICATE
> KEY UPDATE`. Der Bestandslauf ist bewusst keins (§3.5). Der Test verlangt den Upsert jetzt von den
> **drei kuratierenden** Schreibwegen und vom vierten ausdrücklich das Gegenteil — samt Nachweis,
> dass er als eigener Schreibweg überhaupt auftaucht. Eine Ausnahme, die nur durch Weglassen
> entstünde, wäre beim nächsten Umbau wieder da.

**Aufgeräumt wird über `geaendert_von`** mit dem Testpräfix `it-`. Eine von Hand kuratierte Zeile
überlebt jeden Testlauf.

> **Eine Grenze dieser Aufräumregel, die E15 sichtbar macht.** Der Bestandslauf schreibt auf **alle**
> Zeilen des Mandanten — auch auf solche, die ein Mensch angelegt hat und die das Präfix nicht
> tragen. Ein Testlauf hinterlässt dort also einen aufgefrischten `traegt_nachrichten`-Wert und
> einen neuen `bestand_geprueft_am`. **Das ist unschädlich und ausdrücklich in Kauf genommen:** Der
> Wert ist eine Beobachtung und keine Kuratierung, er ist reproduzierbar, und der nächste Lauf
> schriebe ihn ohnehin. Die kuratierten Felder bleiben unberührt — genau das prüft der E15-Fall.

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

### 8.1 Der Bestandslauf — **M83** und **M84** *(21./24.08.2026)*

Zwei Runden, und die zweite ist keine Wiederholung:

| | Was sie misst | Wann |
|---|---|---|
| **M83** | die **beauftragten** Fassungen A und B, von Hand getippt — die Entscheidungsgrundlage für die Bauform | 21.08.2026, **vor** dem Bau |
| **M84** | den **gerenderten** Text aus `findeBestandsflags` — der Nachweis am gebauten Code | 24.08.2026, **nach** dem Bau |

**M83 hat die zweite Runde selbst verlangt:** *„Der gemessene Text ist nicht gerendert … Sobald er
existiert, verlangt L7 eine neue Messung des gerenderten Textes — jOOQ qualifiziert mit
`GlassfishDB.` und rendert `EXISTS` anders, als man es von Hand tippt."* **Regel L7 ist deshalb
nicht schon mit M83 erfüllt gewesen, sondern erst mit M84.**

| Statement | Mandant | erster Lauf | beste von fünf |
|---|---|---:|---:|
| **Bestandsabfrage, gerendert** (M84) | `NEXANS` (733) | 24,510 ms | **23,153 ms** |
| **Bestandsabfrage, gerendert** (M84) | `SUTTONS` (17) | 1,320 ms | **1,170 ms** |
| dieselbe, von Hand getippt (M83) | `NEXANS` | 32,144 ms | 22,154 ms |
| dieselbe, von Hand getippt (M83) | `SUTTONS` | 2,536 ms | 1,109 ms |
| verworfene Fassung B (M83) | `NEXANS` | 4.840,413 ms | 4.797,038 ms |

**Der Plan ist Zeile für Zeile derselbe wie in M83‑1** — Einstieg `ProjectMandant`, `ref` über
`ProjectMandant_Mandant_idx`, `Process` über `Process_ProjectFK`, die Unterabfrage als `DEPENDENT
SUBQUERY` mit `ref` auf `ProejctIDIDX`, **dreimal `Using index`**, `key_len` 146/147. Der Optimierer
sieht dieselbe Abfrage; die Unterschiede im Text (volle Qualifizierung statt Aliase, Backticks,
``select 1 as `one` ``, kein Spaltenalias) sind **nicht planbestimmend**.

**Der gerenderte Text ist rund 5 % teurer — und diese 5 % sind nicht gedeutet.** Sie liegen in der
Spanne, in der auf dieser Instanz auch fünf Läufe desselben Statements streuen (hier 23,153 bis
24,510 ms, also 5,9 %). Für die Entscheidung ist es gleichgültig: **0,23 % der Laufzeitgrenze des
Lese-Pools** (`max_statement_time = 10`).

**Fassung B ist um Faktor 216,5 teurer, und der Grund ist die Bezugsgröße** — A zahlt je Prozess,
B je Nachricht. Die Prozesszahl steht still, die Nachrichtenzahl wächst.

**Nicht gemessen ist auch hier das Schreiben:** das `UPDATE` über alle Zeilen des Mandanten läuft
über den Schreib-Pool und ist nicht `EXPLAIN`-bar. Es gilt dieselbe Schranke wie oben.

**Und für beide Runden gilt der Vorbehalt vom 21.08.2026**
([`annahmen-korrekturen.md`](annahmen-korrekturen.md)): Laufzeiten von der Testkopie sind für die
Produktion eine **optimistische** Schranke — der Puffer fasst `Message` neunfach, die Trefferquote
steht bei 99,975 %, und außer uns belastet die Instanz niemand.

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

### Zum Nachtrag vom 21.08.2026 (E14–E16)

**8. `bestand_geprueft_am` kommt aus `systemClock`, nicht aus der Anwendungsuhr.** Der Bauauftrag
verlangt ausdrücklich die **Anwendungsuhr** und beruft sich dabei auf Regel Z1. **Z1 trägt das
nicht:** Sie verbietet den direkten `now()`-Aufruf und führt *beide* Uhren auf — für
sicherheitsrelevante Zeit **und Protokollzeit** verlangt sie ausdrücklich `systemClock`
(`DEVELOPMENT_GUIDELINES.md` §4.5). Sie entscheidet die Frage also nicht gegen den Bau, sondern für
ihn.

Dazu kommt ein Argument aus der Zeile selbst: `geaendert_am` steht seit `V6` daneben und kommt aus
`systemClock` (Regel A5, dieselbe Wahl wie beim `audit_log`). Gemischt lägen **zwei Zeitstempel
derselben Zeile im Profil `dev` Wochen auseinander**, weil die Anwendungsuhr um den Rückstand der
Testkopie zurückversetzt ist — ein Prüfzeitpunkt in der Vergangenheit wäre keiner. **Gemeldet und
nicht stillschweigend aufgelöst.**

**9. Regel L7 war mit M83 *nicht* erfüllt — M84 ist nachgeholt worden.** Der Auftrag führt unter
V3: *„M83 ist die Messung zu diesem Bau. Regel 7 … ist damit erfüllt, solange das gerenderte SQL dem
gemessenen Text entspricht."* **M83 selbst sagt das Gegenteil:** Der dort gemessene Text ist von
Hand getippt, Anwendungscode existierte nicht, und *„sobald er existiert, verlangt L7 eine neue
Messung des gerenderten Textes"*.

Das gerenderte SQL ist protokolliert und gegen den gemessenen Text gehalten worden. **Alle drei im
Auftrag genannten Meldekriterien sind eingehalten** — Einstiegstabelle `ProjectMandant`, `EXISTS`
statt `IN`, kein zusätzlicher Join. Die Unterschiede sind genau die, die M83 vorhergesagt hat: volle
Qualifizierung statt Aliase, Backticks, ``select 1 as `one` ``, kein Spaltenalias `AS traegt`, und
ein Bindeplatz `?` statt der Sitzungsvariablen `@mandant`. **Keiner davon ist planbestimmend** —
`M84` weist denselben `EXPLAIN` nach, Zeile für Zeile (§8.1). Es war damit **kein** Meldefall im
Sinne des Auftrags, wohl aber eine offene Messpflicht; sie ist geschlossen.

**10. Der Bestandslauf ist der einzige Schreibweg ohne Upsert.** Das ist keine Abweichung vom
Auftrag, sondern von einer bestehenden Zusicherung dieses Backends, und steht deshalb hier: Der
Invariantentest verlangte von **jedem** Schreiben ein `INSERT … ON DUPLICATE KEY UPDATE`. Er ist
geschärft worden statt aufgeweicht (§7).

**11. Der Auftrag nennt `V8` ohne Dateinamen; gebaut ist `V8__bestandsspalte.sql`.** Erwähnt, weil
Flyway-Migrationen nach dem ersten Lauf eingefroren sind — der Name ist ab jetzt nicht mehr
änderbar, ohne den Anwendungsstart zu brechen.

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
6. **Das Schreiben des Bestandslaufs ist nicht gemessen.** Das `UPDATE` läuft über den Schreib-Pool
   und ist nicht `EXPLAIN`-bar; belegt ist nur die Schranke aus M80 (1.490 Zeilen über zehn
   Transaktionen in unter zehn Sekunden), und die ist eine **Beobachtung des Bauablaufs, keine
   Messung**. Es ist jetzt ein Stapel über bis zu 733 Zeilen je Knopfdruck — und die Testkopie
   braucht laut Erfahrung 10 bis 25 s je `COMMIT`. **Vor der Produktion zu erheben.**
7. **Der Lauf hat kein Zeitlimit und keinen Fortschritt.** Er fährt drei Schritte hintereinander in
   **einer** Transaktion; die Oberfläche bekommt erst am Ende eine Antwort. Bei `NEXANS` sind das
   733 Zeilen — bislang unauffällig, aber ungemessen (Punkt 6).
8. **`bestandGeprueft` zählt Anweisungen, nicht geänderte Zeilen.** MariaDB meldet für ein `UPDATE`
   ohne Wertänderung **null** betroffene Zeilen. Gezählt wird deshalb jede Anweisung des Stapels,
   die nicht fehlgeschlagen ist — sonst meldete der zweite Lauf in Folge `0`. Die Zahl beantwortet
   damit „wie viele Zeilen hat der Lauf angefasst" und **nicht** „wie viele haben sich geändert".
   Wer Letzteres braucht, braucht eine andere Zählung.
9. **Der Filter aus E20 ist clientseitig und damit hier nicht gebaut.** Das Backend liefert die drei
   Zustände; wer sie filtert, ist die Oberfläche. Vermerkt, damit niemand später einen
   Serverparameter dafür sucht.
