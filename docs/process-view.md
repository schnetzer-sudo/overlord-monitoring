# Prozessansicht — der Baum

Entstanden in zwei Schritten am selben Tag, dem 02.09.2026:

| Teil | Auftrag | Steht in |
|---|---|---|
| **10c‑1** | „Schritt 10c‑1: Prozessansicht, Backend", Stand 01.09.2026 | §1 bis §14 |
| **10c‑2** | „Schritt 10c‑2: Prozessansicht, Oberfläche", Stand 02.09.2026 | §15 bis §20 |
| **10c‑4b** | „Prozessansicht — freies Zeitfenster, Bau", Stand 07.09.2026 | §37 bis §44 (auf §30 bis §36, der Messrunde 10c‑4a) |

**Der erste Teil war ausdrücklich ohne Oberfläche**, und der Grund steht in §9: Wie groß der Baum je
Mandant tatsächlich ist, entscheidet über Vorklappen, Ladeverhalten und Virtualisierung. Der zweite
Teil ist gegen diese Zahlen entworfen und hat **kein Feld am Backend ergänzt**.

---

## Nummernvergabe

| | |
|---|---|
| **Messungen** | `grep -rnoE '\bM1(09\|1[0-7])\b' docs/ scripts/ *.md` → **kein Treffer** außerhalb der eigenen Skripte. **M109 bis M117** sind hier vergeben. Gegenprobe `\bM10[5-8]\b` → Treffer in fünf Dateien, der Ausdruck greift |
| **Entscheidungen** | **E‑32 bis E‑44**, numerisch fortgesetzt nach Punkt 99. Höchste vergebene ist **E‑31** ([`testfestigkeit.md`](testfestigkeit.md) §10.6, dort E‑27 bis E‑31); `grep -rnoE 'E‑(3[2-9]\|4[0-4])'` → kein Treffer |
| **Offene Punkte** | ab **105**. Höchster vergebener Stand ist **104** ([`dichte-umschalter.md`](dichte-umschalter.md) §9) |

> ### ⚠️ Zur E‑Nummer, und die Falle ist echt
>
> Es laufen **drei** E‑Reihen nebeneinander, und zwei davon sehen gleich aus:
>
> - `E1`, `E2`, … — **je Feature-Datei**, so festgelegt in [`README.md`](README.md) („Die
>   Nummernkreise", 21.08.2026). [`prozess-katalog.md`](prozess-katalog.md) E4 ist eine davon.
> - `E‑a` … `E‑z` → `E‑27`, `E‑28`, … — **projektweit**, mit Bindestrich. Sie trägt
>   [`rollup.md`](rollup.md) E‑a und E‑g, [`dashboard.md`](dashboard.md) E‑c und E‑i. **Diese Datei
>   setzt sie fort.**
> - `E37` und dergleichen **ohne** Bindestrich in [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md)
>   und [`IMPLEMENTIERUNGSPLAN_MVP.md`](IMPLEMENTIERUNGSPLAN_MVP.md) — eine dritte Reihe, die dem
>   Plan gehört. **`E37` dort und `E‑37` hier sind zwei verschiedene Dinge.**
>
> Das Register kennt die Bindestrich-Reihe bis heute nicht; das ist offener Punkt **104** und wird
> hier nicht geschlossen. Es steht hier, damit die nächste Runde nicht dieselbe halbe Stunde
> verbringt.

---

## 1. Der Endpunkt

```
GET /api/prozesse/baum?zeitraum={48H|30T|12M}
GET /api/prozesse/baum?von=<ISO,UTC>&bis=<ISO,UTC>
```

| Parameter | Werte | Vorgabe |
|---|---|---|
| `zeitraum` | `48H`, `30T`, `12M` | **`48H`** (E‑38) — die Antwort nennt das gewählte Paar |
| `von`, `bis` *(seit 07.09.2026, §37 ff.)* | ISO‑Zeitpunkte, UTC, beide auf einer **vollen Stunde** | keine — das freie Fenster wird absolut eingegeben und gegen keine Uhr aufgelöst |

> ### Ergänzt am 07.09.2026 — der zweite Modus (10c‑4b, E‑94)
>
> **Die beiden Modi schließen einander aus.** `zeitraum` und `von`/`bis` zugleich sind `400`
> `zeitfenster-mehrdeutig` und **keine stille Vorrangregel** — dieselbe Festlegung wie in
> [`nachrichtenliste.md`](nachrichtenliste.md) §2 und mit derselben Begründung.
>
> **`bis` ist in der Anfrage einschließend, `fenster.bis` in der Antwort ausschließend.** In der
> Anfrage ist `bis` die **letzte enthaltene Stunde** — beidseitig geschlossen, genau wie `von`/`bis`
> der Nachrichtenliste; wer `bis = 30.12. 23:00` einträgt, bekommt den Eimer 23:00–24:00 mit. In der
> Antwort bleibt `fenster.bis` **ausschließend**, wie seit 10c‑1. Das Backend rechnet
> `bisAusschliessend = bis + 1 Stunde`, serverseitig und in der Zone der Anwendungsuhr — nicht im
> Browser, weil eine Stunde am Umstellungstag keine Stunde ist. Der Grund für die Asymmetrie: Jede
> Seite hält die Konvention ihrer Nachbarn. `von = bis` ist erlaubt und heißt *ein Stundeneimer*.
>
> Die Antwort nennt dann `"zeitraum": "FREI"` — und **`FREI` verrät keine Ebene**. Das Fenster wird
> in Segmente über bis zu drei Rollup-Ebenen zerlegt (§39); die Oberfläche braucht das Feld nur, um
> zu wissen, welcher Knopf hervorgehoben ist. Vollständig in §37 bis §44.

**Kein Mandantenparameter, in keiner Form** (Regel M1). Der Mandant kommt aus der Sitzung, über
`SitzungsVerwaltung` und `MandantService`, der den Sitzungswert gegen die zulässige Menge prüft,
statt ihm zu glauben. Ein `?mandant=…` ist kein Fehler, sondern wirkungslos —
`ProzessbaumIsolationDbIT` hält das fest. **Dieser Endpunkt ist keine neue benannte Ausnahme**; die
drei, die es gibt, stehen in [`mandantentrennung.md`](mandantentrennung.md) §3.

**Kein Eintrag in `SecurityConfig`, und das ist richtig.** Der Pfad fällt unter
`anyRequest().authenticated()`. Eingetragen wird dort nur, wer eine **Rollengrenze** braucht — die
Katalogpflege unter `/api/katalog/**` etwa, weil sie `ADMIN` verlangt.

### E‑32 — Der Pfad ist ein Unterpfad von `/api/prozesse` und kein zweiter Wurzelpfad

Die **Menge** ist dieselbe wie bei `GET /api/prozesse` ([`prozessauswahl.md`](prozessauswahl.md)):
alle Prozesse des aktiven Mandanten. Was sich unterscheidet, ist die **Form** — dort eine flache
Auswahlliste mit drei Feldern, hier ein Baum mit Kennzahlen. Ein zweiter Wurzelpfad behauptete eine
zweite Ressource, wo es eine zweite Sicht auf dieselbe gibt. Dass beide dieselbe Menge beschreiben,
ist zugesichert und nicht behauptet (`ProzessbaumIsolationDbIT.baum_und_auswahl_decken_sich`).

### Die Antwort

```jsonc
{
  "zeitraum": "48H",                        // "48H" | "30T" | "12M" | "FREI" (seit 07.09.2026), immer gesetzt
  "fenster": { "von": "…Z", "bis": "…Z" },  // die gelesenen Grenzen, UTC, bis ausschliessend — auch bei FREI
  "stilleSchwelleMonate": 3,                // die fachliche Festlegung, mitgeliefert (E-37)
  "gesamt": { "anzahlProzesse": 733, "bewegt": 488, "still": 28, "nie": 217,
              "nachrichten": 9950, "fehler": 50 },
  "partner": [
    { "partner": "…", "anzahlProzesse": 12, "nachrichten": 8608, "fehler": 2,
      "richtungen": [
        { "richtung": "EINGEHEND", "anzahlProzesse": 7, "nachrichten": 5100, "fehler": 2,
          "prozesse": [
            { "processId": "…", "processName": "40000_AMG_LAB_VDA",
              "nachrichten": 812, "fehler": 0,
              "letzteBewegung": "…Z", "zustand": "BEWEGT" }
          ] }
      ] },
    { "partner": null, "anzahlProzesse": 216, … }   // „nicht zugeordnet", immer am Ende
  ]
}
```

**Die Feldnamen sind deutsch, die Quellsystembegriffe bleiben, wie sie sind.** Richtlinie §5.2
schreibt „JSON-Felder camelCase, **englisch**"; das Dashboard hat sich schon anders entschieden
(`zeitraum`, `fenster`, `kacheln`, `verteilung`, `zuletztAufgefallen`), und die Prozessauswahl führt
`processId`, `processName`, `projectName` als Namen des Quellsystems. **Hier gilt die gelebte
Konvention der Nachbarn und nicht die geschriebene** — zwei Endpunkte derselben Ansicht, die
verschieden benannt sind, kosten mehr als eine Regelabweichung. *Gemessen war: die Feldnamen der
Nachbarendpunkte, nachgesehen am Code. Behauptet wird: dass die Regel §5.2 im Projekt nicht mehr
gilt — das ist ein Befund für eine eigene Runde und steht als offener Punkt 111.*

### E‑33 — Ein Aufruf, eine Antwort. Kein Knoten wird nachgeladen

Beim größten gemessenen Mandanten sind das 154 Partner, 290 Richtungsgruppen und 733 Blätter
(M117). Ein Nachladen je Partner wären 154 Anfragen mit je einer Sitzungsprüfung und je einem
Verbindungsgriff; auf der Testkopie schreibt außerdem **jede** Anfrage die Sitzung fort
([`messungen-liste-verengung.md`](messungen-liste-verengung.md)). Dieselbe Überlegung wie beim
Dashboard, und dieselbe Zahl an Statements zum Vergleich: dort sieben je Seite, hier **zwei**.

### Fehlerfälle

| `type` | Status | Wann |
|---|---|---|
| `nicht-angemeldet` | 401 | keine Sitzung |
| `kein-mandant-gewaehlt` | 403 | angemeldet, aber kein aktiver Mandant |
| `zeitraum-unbekannt` | 400 | `zeitraum` ist kein Code der drei Paare |

Mehr gibt es nicht — es gibt genau einen Parameter, der falsch sein kann.

> ### Ergänzt am 07.09.2026 — sechs weitere, alle am freien Fenster (§38)
>
> Der Satz darüber gilt für den ersten Modus weiter. Mit `von`/`bis` kommen dazu, in der
> Reihenfolge der Prüfung: `zeitpunkt-ungueltig` (nicht als ISO‑Zeitpunkt lesbar),
> `zeitfenster-mehrdeutig` (`zeitraum` **und** `von`/`bis`), `zeitfenster-unvollstaendig` (nur
> einer der beiden), **`zeitfenster-zu-genau`** (nicht auf einer vollen Stunde der Anwendungszone —
> abgewiesen, nicht gerundet, E‑95), `zeitfenster-ungueltig` (`von` hinter `bis`) und
> `zeitfenster-zu-gross` (mehr als ein Kalenderjahr, gerechnet mit dem ausschließenden Ende). Alle
> `400`. **Die Codes sind aus der Nachrichtenliste übernommen**, bis auf den einen neuen. Ein
> Fenster in der Zukunft ist kein Fehler — es liefert Nullen.

---

## 2. Die Gliederung: Partner → Richtung → Prozess

Drei Ebenen. Unter einem Partner erscheinen **nur die Richtungen, die tatsächlich vorkommen** — kein
leerer Ast.

**Beide kuratierten Felder können ungepflegt sein, und das sind zwei verschiedene Fälle:**

| Fall | Wohin |
|---|---|
| Partner ungepflegt | eigene Gruppe **auf oberster Ebene**, am Ende. Wird nicht auf die Partner verteilt |
| Partner gepflegt, Richtung ungepflegt | eigene Gruppe **unter diesem Partner**, neben eingehend und ausgehend |

**Was „zugeordnet" heißt, entscheidet Entscheidung E‑i** aus [`dashboard.md`](dashboard.md) §4 und
nicht diese Datei: Es gibt eine Katalogzeile, sie trägt `GEPFLEGT`, und das Feld ist gefüllt. Ein
offener Regelvorschlag der Heuristik ist eine **Vermutung** und keine Zuordnung; gruppierte der Baum
darüber, stünden Prozesse unter einem Partner, den niemand bestätigt hat (Regel Q4).

**Die Regel steht seit dem 02.09.2026 in `common/Katalogzuordnung`** — in beiden Sprachen
nebeneinander (E‑44). Die Verteilung des Dashboards braucht sie in SQL, der Baum in Java; zweimal
geschrieben liefe sie beim nächsten Katalogzustand auseinander, und die beiden Ansichten zeigten
dieselbe Zahl verschieden.

### E‑40 — Ein leeres Richtungsfeld heißt „nicht ermittelt", nie „gibt es nicht"

Für die Richtung bedeutet ein leeres Feld etwas anderes als für den Partner. E4 im Katalog liest
„gepflegt und leer" als *hingesehen, es gibt nichts* — bei einer Übertragungsrichtung gibt es das
nicht, jede Übertragung hat eine.

**Die Antwort unterscheidet die beiden Fälle nicht.** *Gemessen war:* Der Katalog trägt zwei Felder,
`richtung` und `pflegestatus`, und beide Fälle — „offen, leer" und „gepflegt, leer" — ergeben
denselben Knoten. *Behauptet wird:* dass die Unterscheidung fachlich gebraucht würde. **Das ist
nicht gemessen**, und deshalb entsteht hier kein drittes Katalogfeld: Der Auftrag verbietet es
ausdrücklich, und ohne einen Nutzer, der die Unterscheidung braucht, wäre es eine Spalte auf
Verdacht. Der Punkt steht offen als **110**.

> **Auf der Testkopie ist der Fall heute nicht theoretisch.** Bei `VOTG` trägt **keine einzige** der
> 390 Katalogzeilen eine Richtung (M110) — die mittlere Ebene besteht dort aus 133 Knoten „nicht
> ermittelt" und sagt nichts. Das ist offener Punkt **108** und eine Gestaltungsfrage für 10c‑2, kein
> Fehler dieses Endpunkts.

### E‑39 — Partner alphabetisch, nicht nach Volumen

**Ein Baum ist zum *Finden* da.** Wer wissen will, ob von einem bestimmten Partner etwas kam, sucht
dessen Namen. Die Rangfolge nach Volumen beantwortet eine andere Frage, und die beantwortet der
Verteilungsblock des Dashboards. Bei `NEXANS` stehen 154 Partner nebeneinander — in einer
Volumenordnung wäre der gesuchte nirgends.

„Nicht zugeordnet" steht **am Ende** und ist keine Rangposition — dieselbe Festlegung wie im
Verteilungsblock.

Die Richtungen stehen in der Reihenfolge der Aufzählung `Richtung` (eingehend, ausgehend), danach
ein gepflegter, aber unbekannter Wert, zuletzt „nicht ermittelt". **Über alle Partner dieselbe** —
eine Oberfläche, die Symbole nach Position vergibt, bekäme sonst unter jedem Partner eine andere.

Die Blätter behalten die Reihenfolge der Abfrage: `ORDER BY ProcessName`.

### E‑41 — Die Gleichheit kommt von der Spalte und nicht von Java

> ### ⚠️ Der Befund, der beim Abnehmen der Baumgröße aufgefallen ist (M117)
>
> Der Endpunkt zählte bei `NEXANS` **155** Partner und **291** Richtungsgruppen. Die SQL-Erhebung
> derselben Daten (M110) zählte **154** und **290**.
>
> **Die Ursache ist die Sortierung der Spalte.** `process_catalog.partner` trägt
> `utf8mb4_general_ci`; für die Datenbank sind zwei Schreibweisen desselben Namens **ein** Wert, für
> `String.equals` sind es zwei. Nachgewiesen an der Quelle:
>
> | | `NEXANS`, gepflegte Partner |
> |---|---:|
> | `COUNT(DISTINCT partner)` | **154** |
> | `COUNT(DISTINCT BINARY partner)` | **155** |
>
> Genau **ein** Partner steht im Katalog in zwei Schreibweisen. Die Folge war sichtbar und falsch:
> derselbe Partner zweimal im Baum, mit geteilten Zahlen — und ein Widerspruch zur Verteilung des
> Dashboards, die in SQL gruppiert und deshalb 154 zeigt.
>
> **Zwei Ansichten derselben Daten dürfen sich nicht widersprechen.** Welche Werte gleich sind,
> entscheidet die Sortierung der Spalte und nicht das Gruppierungsverfahren des Aufrufers.
> `Katalogzuordnung.gruppenschluessel` stellt den Wert deshalb hoch (`Locale.ROOT`); **angezeigt wird
> weiterhin der Rohwert**, und zwar die zuerst angetroffene Schreibweise — der Katalog ist die
> Wahrheit und wird nicht umgeschrieben (Q4).
>
> **Die Näherung ist benannt und nicht verschwiegen.** `utf8mb4_general_ci` ignoriert *mehr* als die
> Groß- und Kleinschreibung: Es behandelt auch `a` und `ä` als gleich, `toUpperCase` tut das nicht.
> Zwei Werte, die sich nur in einem Akzent unterscheiden, blieben hier zwei Gruppen und wären für die
> Datenbank eine. Der exakte Weg wäre `WEIGHT_STRING` — der Sortierungsschlüssel selbst. **Er ist
> nicht gebaut und nicht gemessen**; das ist offener Punkt **105**.
>
> **Dass ein Partner in zwei Schreibweisen im Katalog steht, ist eine Kuratierungsfrage und kein
> Codefehler.** Sie ist offener Punkt **106**.

---

## 3. Die Kennzahlen je Prozess

| Kennzahl | Quelle | Fenster |
|---|---|---|
| **Nachrichten** im Zeitraum | die Rollup-Ebene des Paares | ja |
| **Fehler** im Zeitraum | dieselbe Zeile, über die Statuseinordnung | ja |
| **letzte Bewegung** | `message_rollup` je `process_id` | **nein** |

**Nachrichten und Fehler entstehen aus einem Lesevorgang.** Gruppiert wird nach dem **Rohstatus**
und nicht schon nach der Einordnung — Entscheidung E‑g aus [`rollup.md`](rollup.md) §2: Die
Einordnung ist eine Regel, die sich ändern kann, der Rohwert ist eine Tatsache. Was Fehler ist,
entscheidet `MessageStatusClassifier.einordnung(String)` beim Lesen, **gerufen und nicht
nachgebaut**. Ein unbekannter Wert mit `ERROR_`-Präfix fällt damit auch hier nach `FEHLER`, ohne
dass jemand eine zweite Liste pflegen müsste.

**Null ist eine Aussage und kein fehlender Wert.** Ein Prozess steht im Baum, weil er dem Mandanten
gehört, nicht weil er Verkehr hatte — genau der ist der interessante Fall, wenn jemand wissen will,
warum nichts ankommt ([`prozessauswahl.md`](prozessauswahl.md) §3).

**Die Summen stehen auf jeder Ebene.** Der Aufrufer soll nichts zusammenrechnen müssen (Richtlinie
§5.1).

### E‑34 — „Letzte Bewegung" als `ORDER BY … LIMIT 1` und nicht als `MAX()`

Beide fragen denselben Wert. **Sie kosten nicht dasselbe**, und der Unterschied ist gemessen:

| Fassung | `NEXANS` (733) | `VOTG` (390) | `IBIS` (192) | `SUTTONS` (17) |
|---|---:|---:|---:|---:|
| `MAX()` als korrelierte Unterabfrage (M112‑A) | 16,089 ms | 2,364 ms | 5,002 ms | **32,577 ms** |
| **`ORDER BY … DESC` mit Deckelung** (M114‑D, gebaut) | **3,417 ms** | **2,513 ms** | **2,524 ms** | **1,460 ms** |

**Der Plan sieht in beiden Fällen gleich aus** — `DEPENDENT SUBQUERY … ref
message_rollup_prozess_idx … Using index` —, und genau das ist die Falle: Mit `MAX()` liest MariaDB
je Prozess den **ganzen** Indexbereich und nimmt davon das Maximum; die MIN/MAX-Optimierung greift
in einer abhängigen Unterabfrage nicht. Mit der Deckelung steht der gesuchte Wert am Anfang des
Bereichs, und der Zugriff endet nach einer Zeile.

**Der Ausreißer bei `SUTTONS` ist der Beleg dafür, dass es nicht an der Prozesszahl hängt:** 17
Prozesse und die *höchste* Laufzeit aller vier Mandanten. Der Aufwand der `MAX()`-Fassung hängt an
der Zahl der **Eimer je Prozess**, und die ist bei einem kleinen Mandanten mit viel Verkehr hoch.

**Der Unterschied steht nur im Statementtext.** Deshalb hält ihn `ProzessbaumStatementsTest`
wörtlich fest — ein Plantest fände ihn nicht.

> **Zwei weitere Fassungen sind gemessen und verworfen** (M114): eine Ableitung, die nur die Prozesse
> des Mandanten gruppiert (`E`, 98 bis 398 ms — die Mandantenkette steht dann zweimal im Statement
> und wird zweimal ausgewertet), und eine Ableitung über den ganzen Bestand (`G`, 8 bis 17 ms). `G`
> ist die zweitbeste und hat einen eigenen Reiz: Ihre Laufzeit hängt **nicht** am Mandanten, weil sie
> immer alle 738 Prozesse gruppiert. Genommen ist trotzdem `D` — sie ist in jedem gemessenen Fall
> schneller, und ihr Aufwand wächst mit dem, was der Nutzer sieht.

---

## 4. Die drei Zustände und die Drei-Monats-Schwelle

| Zustand | Bedeutung |
|---|---|
| **bewegt** | hat innerhalb der Schwelle Nachrichten getragen |
| **still** | trug schon einmal etwas, aber seit mehr als der Schwelle nichts |
| **nie** | hat noch nie etwas getragen |

### E‑35 — Alle drei hängen an *einem* Wert, und der ist fensterunabhängig

Der Wert ist die **letzte Bewegung**. Die Reihenfolge der Prüfung ist die Festlegung: `NIE` vor
`STILL` vor `BEWEGT`.

**Der Auftrag beschreibt `BEWEGT` als „hat im Zeitraum Nachrichten getragen".** Wörtlich genommen
wären die drei Werte weder disjunkt noch vollständig:

- Ein Prozess mit Verkehr vor fünf Monaten wäre in einem Zwölf-Monats-Fenster **beides** — bewegt
  (Volumen im Fenster) und still (seit über drei Monaten nichts).
- Ein Prozess, dessen letzte Bewegung zehn Tage zurückliegt, wäre in einem 48-Stunden-Fenster
  **keines von dreien**.

**Aufgelöst ist das über die Reihenfolge**, und sie folgt dem tragenden Satz des Auftrags: *„Stille
Prozesse werden markiert."* Markiert werden `NIE` und `STILL`; `BEWEGT` ist der unmarkierte
Normalfall und damit genau das, was übrig bleibt. Die Zahl *Nachrichten im Zeitraum* steht daneben
und beantwortet die fensterabhängige Frage — sie ist eine Kennzahl und keine Markierung.

*Gemessen war: die Formulierung des Auftrags. Behauptet wird: dass „markiert" die tragende Lesart
ist. Das ist eine Auslegung und keine Messung* — sie ist hier begründet, damit sie widersprochen
werden kann.

**Die drei Zähler sind disjunkt und vollständig**: Ihre Summe ist `anzahlProzesse`. Zwei Tests halten
das fest, einer ohne Datenbank und einer am Endpunkt.

### E‑37 — Die Schwelle steht als benannter Wert und in der Antwort

`ProzessbaumService.STILLE_SCHWELLE = Period.ofMonths(3)` — **nicht** als `90` in einem Statement.
Sie ist eine fachliche Festlegung und wird sich ändern.

**Ein `Period` und keine `Duration`:** Drei Monate sind keine feste Zahl von Tagen.
`Duration.ofDays(90)` wäre eine Näherung mit einem Fehler von bis zu zwei Tagen, und die Schwelle
trägt eine fachliche Aussage und keine Rechnung.

**Sie steht als `stilleSchwelleMonate` in der Antwort.** Die Oberfläche muss „seit über drei
Monaten" formulieren können, ohne die Drei selbst zu kennen — sonst stünde dieselbe Festlegung an
zwei Orten und driftete.

**Sie gilt unabhängig vom gewählten Zeitfenster.** Wählt jemand 48 Stunden, heißt „still" trotzdem
*seit drei Monaten nichts*. Eine fensterabhängige Schwelle markierte bei 48 Stunden den Normalfall.
`ProzessbaumServiceTest.zustand_ist_fensterunabhaengig` prüft es über alle drei Paare — ohne diesen
Test wäre die Festlegung eine Absichtserklärung.

**Warum drei Monate tragen:** Am Stichtag der Testkopie sind bei `NEXANS` **28 von 733** Prozessen
still — 3,8 % (M111). Das ist eine Menge, die jemand durchsieht.

### E‑36 — „nie" kommt aus dem Rollup und nicht aus `traegt_nachrichten`

`process_catalog` führt seit `V8` eine Spalte für denselben Sachverhalt (E14 in
[`prozess-katalog.md`](prozess-katalog.md)): *ja / nein / noch nie geprüft*, gefüllt vom
Bestandslauf. **Der Baum benutzt sie nicht**, und das ist gemessen entschieden:

| | |
|---|---|
| **Widersprüche zwischen beiden Quellen** (M111‑2) | **0** — über alle Mandanten mit Katalogzeilen, in beide Richtungen: kein Prozess mit `traegt_nachrichten = true` ohne Rollupzeile, keiner mit `false` und Rollupzeile |
| **Wo sie auseinandergehen** | Bei `SUTTONS` (17 Prozesse) und `WOC` (4) steht die Katalogspalte auf **`NULL` — noch nie geprüft**, weil beide gar keine Katalogzeile tragen. Der Rollup hat für dieselben Prozesse eine **eindeutige** Antwort: `SUTTONS` 0 mal „nie", `WOC` 2 von 4 |

**Der Katalog kennt einen vierten Zustand, den der Baum nicht abbilden könnte.** „Noch nie geprüft"
ist weder bewegt noch still noch nie — es ist die Aussage, dass niemand nachgesehen hat. Der Rollup
kennt diesen Zustand nicht: Er ist über den Gesamtbestand gerechnet und beantwortet die Frage für
jeden Prozess.

**Dazu kommt die Einheitlichkeit der Quelle.** Jede andere Zahl des Baums kommt aus dem Rollup;
käme „nie" aus dem Katalog, stünden zwei Zahlen nebeneinander, die aus zwei Läufen mit zwei
verschiedenen Datumsständen stammen. Die Spalte `bestand_geprueft_am` sagt, wie alt die eine ist —
und genau das müsste die Oberfläche dann erklären.

**Der Preis ist benannt:** Der Baum sagt „nie" auch dort, wo der Rollup eine Lücke hätte. Er hat
heute keine (Summenprobe M109‑4: alle drei Ebenen tragen 3.341.519, dieselbe Zahl wie `Message`),
und der Wasserstand in `rollup_lauf` ist die Stelle, an der eine Lücke sichtbar würde.

---

## 5. Überfälligkeit steht nicht im Baum (E‑43)

[`dashboard.md`](dashboard.md) §5 führt sie als **erste benannte Ausnahme von Regel L2**: Sie hängt
an `MessageLastUpdate + MessageTimeout` gegen `jetzt` und ist im Rollup nicht abbildbar.

**Je Prozess wäre das eine Live-Aggregation über `Message` mit `GROUP BY process_id` über bis zu 733
Prozesse** — eine zweite und deutlich größere Ausnahme, für eine Zahl, die in der Übertragungsliste
rechts ohnehin steht.

**Der Auftrag lässt die Tür offen** („Falls du das für falsch hältst, miss es und berichte") und
weist darauf hin, dass die Menge klein ist: 538 Überfällige insgesamt am Messtag. **Sie ist nicht
gemessen worden**, und das ist die Antwort, nicht ein Versäumnis:

1. **Die Entscheidung gehört nicht in diesen Schritt.** Der Auftrag sagt das selbst. Eine Messung,
   die eine Entscheidung vorbereitet, die niemand treffen soll, ist Arbeit ohne Abnehmer.
2. **Die Zahl der Überfälligen ist nicht der Kostentreiber.** Die 538 sind das *Ergebnis*; die
   Kosten hängen daran, wie MariaDB dorthin kommt. [`dashboard.md`](dashboard.md) §7a hat für
   dieselbe Tabelle gemessen, was passiert, wenn der Optimierer den falschen Index nimmt: 2.174 ms
   bei `SUTTONS` und ein `500` an der Zeitgrenze bei `VOTG`. Eine Gruppierung je Prozess ist eine
   **andere** Abfrageform als die dortige Zählung, und ihre Kosten sind aus jener Messung nicht
   ableitbar.
3. **Der Endpunkt hätte dann drei Statements statt zwei**, und das dritte wäre das einzige, das auf
   der Produktion live liest — mit allem, was daran hängt: der Teilerfolg-Mechanismus, das Feld
   „nicht ermittelbar", die Unterscheidung zwischen Zeitgrenze und echtem Fehler.

**`ProzessbaumStatementsTest.keine_live_aggregation` und `ProzessbaumPlanDbIT.kein_plan_enthaelt_message`
halten es fest** — kein Statement dieser Ansicht fasst `Message` an, und kein Plan nennt die Tabelle.
Der Punkt bleibt offen als **112**, mit dem, was zu messen wäre, wenn ihn jemand aufmacht.

---

## 6. Die zwei Statements

### E‑42 — Zwei, und sie beantworten zwei verschiedene Fragen über zwei verschiedene Mengen

| | Menge | Fenster | Mandantenkette |
|---|---|---|---|
| **Gerüst** | **alle** Prozesse des Mandanten | nein | **Join** |
| **Kennzahlen** | nur, was im Fenster liegt | ja | **`EXISTS`** |

In *einem* Statement wäre die zweite Menge ein `LEFT JOIN` auf eine Aggregation und damit ein
zweiter Zugriffspfad im selben Plan; getrennt hat jede ihren eigenen, und jeder ist gemessen.

### Die Mandantenkette steht zweimal verschieden da — und das ist kein Versehen

- **Im Gerüst** ist `ProjectMandant` der *selektivste* Teil der Bedingung und soll den Zugriff
  treiben. Vervielfachen kann er nichts: Der Primärschlüssel ist `(ProjectID, MandantID)`, und mit
  `MandantID = ?` bleibt je Projekt höchstens eine Zeile übrig. Das gilt **aus dem Schema heraus**
  und nicht erst aus den Daten. Aggregiert wird außerdem nichts — eine Zeile zu viel wäre sichtbar
  und keine stille Verfälschung. Dieselbe Form wie in [`prozessauswahl.md`](prozessauswahl.md) §4.
- **In den Kennzahlen** wird **summiert**. `ProjectMandant` ist n:m; ein Join vervielfachte jede
  Rollupzeile, sobald ein Projekt an mehreren Mandanten hängt — und damit **die Summe**. Der Fehler
  wäre still: Die Zahlen sähen plausibel aus und wären zu hoch.

**Gemessen ist der Unterschied trotzdem, weil der Auftrag es verlangt** (M113): Über vier Mandanten
und alle drei Fensterbreiten liegen Join und `EXISTS` innerhalb des Rauschens — 6,216 bis 77,184 ms
die eine Fassung, 6,737 bis 77,172 ms die andere, mit wechselndem Vorzeichen. **Die Wahl fällt damit
an der Richtigkeit und nicht am Tempo.**

**Die Summenprobe dazu** (M113‑Z, Zwölf-Monats-Fenster): `EXISTS` und Join liefern auf diesem Bestand
**dieselbe** Summe — `NEXANS` 2.308.005, `VOTG` 113.291, `IBIS` 58.672, `SUTTONS` 196.536. *Gemessen
war: dass auf diesem Bestand kein Projekt an mehreren Mandanten hängt (M74a: 0). Behauptet wird: dass
ein Join gefährlich ist — das folgt aus dem **Schema** und nicht aus dieser Zahl.* Die Probe zeigt
gerade, dass der Fehler heute unsichtbar wäre.

### Was der Code schickt

Beide Statements stehen **wörtlich** in `ProzessbaumStatementsTest`. Was in §7 gemessen wird, ist aus
demselben Code gefallen — über eine jOOQ-Attrappe mit `StatementType.STATIC_STATEMENT`, nicht
abgetippt. Vier Eigenschaften hängen am Text und nirgends sonst:

1. Die letzte Bewegung kommt über die Deckelung und **nicht** über `MAX()`.
2. Die Mandantenkette ist im Gerüst ein Join und in den Kennzahlen ein `EXISTS`.
3. Um die Schlüsselspalte der Rollup-Ebene steht **keine Funktion** — sonst fällt der
   Bereichszugriff weg, und die Abfrage wäre langsamer, ohne falsch zu sein.
4. Jedes Paar liest **seine** Ebene und keine andere.

> ### ⚠️ Korrektur vom 07.09.2026 — Eigenschaft 4 ist ersetzt, nicht gestrichen (E‑97, §40)
>
> Der Satz darüber bleibt stehen; für die drei Paare sagt die neue Fassung dasselbe. Seit dem
> freien Zeitfenster liest **ein** Kennzahlenstatement bis zu drei Ebenen — in einer Ableitung mit
> `UNION ALL`, je Ebene ein Zweig —, und die Eigenschaft lautet seither: **Jedes *Segment* liest
> seine Ebene und keine andere, und es steht keine Ebene im Text, die kein Segment trägt.** Die
> Eigenschaften 1 bis 3 gelten unverändert, die dritte für jeden Zweig einzeln. **E‑42 fällt nicht:**
> Es bleiben zwei Statements je Aufruf, auch im freien Fenster.

**`process_catalog` hängt als `LEFT JOIN` dran.** `SUTTONS` und `WOC` haben keine einzige
Katalogzeile (M110); ein innerer Join verlöre ihre 17 bzw. 4 Prozesse stillschweigend — und damit
ausgerechnet die, die vollständig unter „nicht zugeordnet" erscheinen müssten.

**Die Kennzahlenabfrage hat kein `ORDER BY`.** Sortiert wird der Baum, und das passiert im Dienst.

---

## 7. Der Index — festgestellt, nicht geglaubt (Teil 2 des Auftrags)

Der Auftrag vermutet, dass `message_rollup` den Index für `MAX(stunde) GROUP BY process_id`
möglicherweise nicht trägt, und verweist auf zwei einander widersprechende Stellen in
[`rollup.md`](rollup.md). **Gefragt ist `information_schema`, nicht das Dokument.**

### M109 — Was heute tatsächlich da ist

| Tabelle | Index | Spalten |
|---|---|---|
| `message_rollup` | `PRIMARY` | `stunde, process_id, message_status` |
| `message_rollup` | **`message_rollup_prozess_idx`** | **`process_id, stunde`** — BTREE, nicht eindeutig |
| `message_rollup_tag` | `PRIMARY` | `tag, process_id, message_status` |
| `message_rollup_monat` | `PRIMARY` | `monat, process_id, message_status` |
| `process_catalog` | `PRIMARY` | `process_id` |

**Der Index existiert seit `V11__message_rollup_prozess_index.sql`** (30.08.2026) und ist für die
Fensterverengung der Nachrichtenliste gebaut worden ([`rollup.md`](rollup.md) §9b). Die
Prozessansicht ist sein **zweiter** Verbraucher und hat ihn vorgefunden. **Es entsteht deshalb keine
neue Migration** — Teil 2 des Auftrags ist ohne Commit erledigt.

**Platz, an der gebauten Tabelle gemessen:**

| Tabelle | Daten | Index |
|---|---:|---:|
| `message_rollup` | 22.577.152 B (21,53 MiB) | **19.480.576 B (18,58 MiB)** |
| `message_rollup_tag` | 9.994.240 B (9,53 MiB) | 0 |
| `message_rollup_monat` | 1.589.248 B (1,52 MiB) | 0 |
| `process_catalog` | 262.144 B | 0 |

> **Befund, klein aber datiert.** [`rollup.md`](rollup.md) §2 nennt für den Index **16,6 MiB**, an
> einer Probetabelle gemessen (M105). An der gebauten Tabelle sind es **18,58 MiB** — **+11,9 %**.
> *Gemessen war: `information_schema.TABLES.INDEX_LENGTH` am 02.09.2026. Behauptet wird in §2: der
> Platzbedarf des Index schlechthin.* Die Abweichung ändert an keiner Entscheidung etwas; sie steht
> hier, weil eine Zahl aus einer Probetabelle keine Zahl aus der Tabelle ist.

**Zeilen, gezählt statt geschätzt:** 335.610 / 123.049 / 11.957, Katalog **1.469**. Alle drei Ebenen
tragen `SUM(anzahl) = 3.341.519` — die Summenprobe hält. 738 verschiedene `process_id` stehen
überhaupt in der Stundenebene; der früheste Eimer ist `2024‑10‑01 02:00`, der jüngste
`2026‑07‑08 17:00`.

### M115 — Was der Index der gebauten Fassung bringt

Der Index wird für die Messung **nicht gelöscht**: Die Tabelle ist geteilt, und die Fensterverengung
der Nachrichtenliste hängt an ihm. `IGNORE INDEX` beantwortet dieselbe Frage, ohne etwas anzufassen.

| | `NEXANS` | `VOTG` | `IBIS` | `SUTTONS` |
|---|---:|---:|---:|---:|
| mit Index | 3,520 ms | 2,598 ms | 2,461 ms | 1,575 ms |
| **ohne Index** | **2.858,169 ms** | **6.082,889 ms** | **2.811,051 ms** | 11,701 ms |
| Faktor | 812 | **2.341** | 1.142 | 7,4 |

**Der Auftrag fragt, ob ein voller Durchlauf über 335.610 Zeilen vertretbar wäre. Er ist es nicht.**
Bei `VOTG` liegt die Fassung ohne Index bei **6,1 Sekunden** — unter der Zeitgrenze des Lese-Pools
(10 s), aber beim Zwölffachen des 500‑ms‑Budgets für eine ganze Seite. Und der teuerste Fall ist
nicht der größte Mandant: `VOTG` mit 390 Prozessen kostet mehr als `NEXANS` mit 733.
**Cross-Mandanten-Instabilität, wie der Auftrag sie erwartet hat.**

> **Die Gegenprobe an der zweitbesten Fassung** (M114‑F, Ableitung über den Gesamtbestand): ohne
> Index **985 bis 1.004 ms** über alle vier Mandanten, mit Index 8 bis 17 ms. Der Plan zeigt dort,
> was passiert — `DERIVED … index PRIMARY … rows 329979 … Using temporary; Using filesort`, also
> genau der volle Durchlauf, nach dem der Auftrag fragt.

### M105 ist geprüft und hält

`V11` hält fest, dass der Index den Plan der Tagesebenen-Ableitung hätte kippen können und es nicht
tut. **Dieser Schritt legt keinen Index an und ändert keinen** — die Prüfung von damals bleibt
gültig, und es gibt nichts neu zu messen.

---

## 8. Die Messung der gebauten Statements (Regel L7)

Gegen die Testkopie, Profil `dev`, Anker `2025-12-30 04:09:47`. Serverseitig über `SET profiling`,
beste von fünf nach einem Aufwärmlauf. Skripte und Protokolle in
`scripts/messung-prozessansicht/`.

**Vier Mandanten**, drei Größenordnungen wie von L7 verlangt, und der vierte aus einem Grund:

| Mandant | Prozesse | warum in der Liste |
|---|---:|---|
| `NEXANS` | 733 | der größte |
| `VOTG` | 390 | mittlerer — und der **teuerste** Fall ohne Index (M115) |
| `IBIS` | 192 | mittlerer — und der **breiteste** Baum (§9) |
| `SUTTONS` | 17 | der kleine — und der Ausreißer in M112 |

### M116 — beste von fünf, in Millisekunden

| Mandant | Gerüst | Kennzahlen 48 h | 30 Tage | 12 Monate | **Summe 48 h** | **30 T** | **12 M** |
|---|---:|---:|---:|---:|---:|---:|---:|
| `NEXANS` | 3,438 | 6,781 | 53,163 | 75,743 | **10,219** | **56,601** | **79,181** |
| `VOTG` | 2,690 | 6,791 | 32,980 | 42,037 | **9,481** | **35,670** | **44,727** |
| `IBIS` | 2,703 | 6,216 | 38,032 | 47,444 | **8,919** | **40,735** | **50,147** |
| `SUTTONS` | 1,621 | 6,886 | 30,717 | 39,206 | **8,507** | **32,338** | **40,827** |

**Der schlechteste gemessene Fall ist `NEXANS` über zwölf Monate mit 79,181 ms** — 15,8 % des
500‑ms‑Budgets, das die Abnahme für eine ganze Seite setzt, und Faktor 126 zur Zeitgrenze des
Lese-Pools.

**Was zu beobachten ist, und es ist die brauchbarere Zahl als die Summe:** Das Gerüst wächst mit der
Prozesszahl des Mandanten (1,6 bis 3,4 ms), die Kennzahlen wachsen mit der **Fensterbreite** und
kaum mit dem Mandanten (6,2 bis 6,9 ms bei 48 Stunden, 39 bis 76 ms bei zwölf Monaten). Der teure
Teil ist der Bereichszugriff auf die Rolluptabelle, und den zahlt jeder Mandant gleich — er liest
denselben Bereich und wirft danach weg, was ihm nicht gehört.

> ### Ergänzt am 07.09.2026 — der Satz gilt für die drei Paare, und nur für sie (M147, §31)
>
> Der Absatz darüber bleibt stehen; für `48H`, `30T` und `12M` beschreibt er die gemessene Lage.
> **Über weitere Fenster auf der Stundenebene gilt er nicht:** Ab einer mandantenabhängigen Spanne —
> bei `NEXANS` zwischen 7 und 30 Tagen, bei `VOTG` und `IBIS` zwischen 30 und 90 — steigt der
> Optimierer über `ProjectMandant` und `message_rollup_prozess_idx` ein und liest **nur die Zeilen
> des Mandanten**. Dann zahlt nicht jeder Mandant gleich: über 365 Tage `NEXANS` 832,090 ms, `IBIS`
> 194,514 ms. Der Preis hängt an den Zeilen, die der gewählte Plan liest — und welcher das ist, ist
> eine Eigenschaft der Spanne und des Mandanten, nicht der Tabelle.

### Die Pläne (Regel L15)

**Gerüst**, identisch über alle vier Mandanten bis auf die Zeilenschätzung:

| id | select_type | table | type | key | Extra |
|---|---|---|---|---|---|
| 1 | PRIMARY | `ProjectMandant` | `ref` | `ProjectMandant_Mandant_idx` | `Using where; Using index; Using temporary; Using filesort` |
| 1 | PRIMARY | `Process` | `ref` | `Process_ProjectFK` | — |
| 1 | PRIMARY | `process_catalog` | `eq_ref` | `PRIMARY` | — |
| 2 | DEPENDENT SUBQUERY | `message_rollup` | `ref` | **`message_rollup_prozess_idx`** | `Using where; Using index` |

`Using temporary; Using filesort` ist **kein Befund**: Sortiert werden ein paar hundert Zeilen, und
für `ORDER BY ProcessName` gibt es keinen Index, der das bediente — dieselbe Lage wie bei der
Prozessauswahl ([`prozessauswahl.md`](prozessauswahl.md) §6).

**Die Zeilenschätzung der Unterabfrage ist `453` und damit irreführend.** Sie ist die Schätzung des
*Bereichs*; die Deckelung nimmt davon **eine** Zeile. Das ist der Grund, warum der Plan die
Entscheidung aus E‑34 nicht sichtbar macht und `ProzessbaumStatementsTest` sie am Text hält.

**Kennzahlen**, `NEXANS` über zwölf Monate:

| id | table | type | key | key_len | rows | Extra |
|---|---|---|---|---|---|---|
| 1 | `message_rollup_monat` | `range` | `PRIMARY` | 3 | 5.961 | `Using where; Using temporary; Using filesort` |
| 1 | `baum_process` | `eq_ref` | `PRIMARY` | 146 | 1 | `Using where` |
| 1 | `ProjectMandant` | `eq_ref` | `PRIMARY` | 292 | 1 | `Using where; Using index` |

> **Bei `SUTTONS` dreht der Optimierer die Reihenfolge um** und steigt über `ProjectMandant` ein; die
> Rolluptabelle wird dann über einen `BNL`-Join erreicht, bleibt aber `range` über `PRIMARY`. **Beide
> Pläne sind richtig**, und beide sind gemessen schnell (39,206 gegen 75,743 ms). `ProzessbaumPlanDbIT`
> schreibt deshalb **die Zugriffsart** fest und **nicht die Reihenfolge** — dieselbe Überlegung wie in
> `DashboardPlanDbIT` und `NachrichtenPlanDbIT`.

---

## 9. Die Baumgröße je Mandant — was 10c‑2 braucht

**Das ist der eigentliche Ertrag dieser Runde.** Ein Baum mit vier Partnern und einer mit
hundertvierundfünfzig sind zwei verschiedene Ansichten.

### M117 — am gebauten Endpunkt abgenommen

| Mandant | Prozesse | Partner | ohne Partner | Richtungsgruppen | davon „nicht ermittelt" | bewegt | still | nie | Rumpf |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| `NEXANS` | 733 | 154 | 1 Gruppe | 290 | 1 | 488 | 28 | 217 | **153.885 B** (150,3 KiB) |
| `VOTG` | 390 | 132 | 1 Gruppe | 133 | **133** | 36 | 4 | 350 | 85.055 B (83,1 KiB) |
| `IBIS` | 192 | 79 | — | 139 | 1 | 105 | 8 | 79 | 50.570 B (49,4 KiB) |
| `SUTTONS` | 17 | 0 | 1 Gruppe | 1 | 1 | 17 | 0 | 0 | 3.333 B (3,3 KiB) |

### M110 — dieselben Zahlen über alle zehn Mandanten, aus der Quelle

| Mandant | Prozesse | Katalogzeilen | gepflegt | Partner | ohne Partner | eingehend | ausgehend | ohne Richtung |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| `NEXANS` | 733 | 733 | 733 | 154 | 216 | 384 | 338 | 11 |
| `VOTG` | 390 | 390 | 378 | 132 | 12 | **0** | **0** | 390 |
| `IBIS` | 192 | 192 | 192 | 79 | 0 | 76 | 112 | 4 |
| `IBISGUS` | 89 | 89 | 89 | 38 | 0 | 34 | 54 | 1 |
| `ZAST` | 35 | 35 | 35 | 34 | 0 | 2 | 33 | 0 |
| `SUTTONS` | 17 | **0** | — | 0 | 17 | 0 | 0 | 17 |
| `NXHBE` | 17 | 17 | 17 | 3 | 0 | 13 | 4 | 0 |
| `EDITIONLINGERI` | 9 | 9 | 9 | 3 | 0 | 5 | 4 | 0 |
| `WOC` | 4 | **0** | — | 0 | 4 | 0 | 0 | 4 |
| `SYSTEM` | 4 | 4 | 4 | 3 | 0 | 1 | 0 | 3 |

**Äste je Mandant** (Partner × Richtung), aus derselben Erhebung:

| Mandant | Gruppen | Prozesse | größte Gruppe | Schnitt |
|---|---:|---:|---:|---:|
| `NEXANS` | 290 | 733 | **108** | 2,53 |
| `VOTG` | 133 | 390 | 17 | 2,93 |
| `IBIS` | **139** | 192 | **4** | **1,38** |
| `IBISGUS` | 66 | 89 | 3 | 1,35 |
| `ZAST` | 34 | 35 | 2 | 1,03 |
| `NXHBE` | 5 | 17 | 7 | 3,40 |
| `EDITIONLINGERI` | 5 | 9 | 4 | 1,80 |
| `SYSTEM` | 3 | 4 | 2 | 1,33 |
| `SUTTONS` | 1 | 17 | 17 | 17,00 |
| `WOC` | 1 | 4 | 4 | 4,00 |

**Wie viele Richtungsgruppen unter einem Partner hängen:** bei `NEXANS` tragen 134 von 154 Partnern
**zwei** und 20 nur eine; bei `IBIS` 60 von 79 zwei; bei `VOTG` **alle 132 genau eine** — nämlich
„nicht ermittelt".

### Die vier Zahlen, an denen 10c‑2 hängt

1. **`NEXANS` ist der Fall, für den entworfen werden muss:** 154 Partner, 290 Gruppen, 733 Blätter,
   eine Gruppe mit **108** Prozessen. Vorklappen scheidet damit aus; die oberste Ebene passt nicht
   auf einen Bildschirm.
2. **`IBIS` ist der Gegenfall und der unbequemere:** 139 Gruppen auf 192 Prozesse, größte Gruppe
   **vier**. Der Baum ist dort fast so breit wie tief — er *ordnet* kaum noch, er *verlängert*. Ob
   eine Baumdarstellung dafür die richtige Bauform ist, ist eine Gestaltungsfrage und steht als
   offener Punkt **107**.
3. **`VOTG` hat eine mittlere Ebene ohne Aussage:** 133 Knoten, alle „nicht ermittelt". Offener
   Punkt **108**.
4. **Der Rumpf ist bei `NEXANS` 150,3 KiB.** Das ist für eine Antwort viel und für einen Download
   wenig. **Nicht gemessen ist, was das Rendern kostet** — das ist 10c‑2 und steht als offener Punkt
   **109**.

### M111 — die Zustandsverteilung, über alle zehn Mandanten

Stichtag ist der Anker; die Schwelle sind drei Monate, also `2025-09-30 04:09:47`.

| Mandant | Prozesse | bewegt | still | nie | Anteil „nie" |
|---|---:|---:|---:|---:|---:|
| `NEXANS` | 733 | 488 | 28 | 217 | 29,6 % |
| `VOTG` | 390 | 36 | 4 | **350** | **89,7 %** |
| `IBIS` | 192 | 105 | 8 | 79 | 41,1 % |
| `IBISGUS` | 89 | 21 | 2 | 66 | 74,2 % |
| `ZAST` | 35 | 23 | 1 | 11 | 31,4 % |
| `SUTTONS` | 17 | 17 | 0 | 0 | 0 % |
| `NXHBE` | 17 | **0** | 2 | 15 | 88,2 % |
| `EDITIONLINGERI` | 9 | 0 | 0 | 9 | 100 % |
| `WOC` | 4 | 2 | 0 | 2 | 50 % |
| `SYSTEM` | 4 | 1 | 0 | 3 | 75 % |

**„Still" ist bei jedem Mandanten die kleinste der drei Mengen** — 0 bis 28 Prozesse. Gemessen in
Anteilen: bei den vier großen Mandanten 1,0 bis 4,2 %, bei `NXHBE` mit **2 von 17** aber 11,8 %.
**Das ist die Zahl, die die Drei-Monats-Schwelle rechtfertigt:** Eine Markierung, die 28 von 733
Zeilen trifft, ist eine Markierung. Eine kürzere Schwelle träfe den Normalfall.

**„Nie" ist dagegen bei fünf von zehn Mandanten die *größte* Menge** und bei `WOC` gleichauf mit
„bewegt"; bei `EDITIONLINGERI` ist es die einzige. Das deckt sich mit M74b aus Schritt 9 (765 von 1.503 Prozessen tragen im Bestand keine
Nachricht) und mit `V8`, und es ist die Zahl, die die Trennung von „still" und „nie" trägt: Wären
beide ein Zustand, bestünde der Baum bei `VOTG` zu 90 % aus Markierungen.

> ### ⚠️ Berichtigung zu einer Zahl aus dem Auftrag
>
> Der Auftrag begründet die Ablehnung einer Projektebene damit, „nicht zugeordnet" verteilte sich
> über alle Projekte — *„bei NEXANS sind das **390 von 733** Prozessen"*.
>
> **Gemessen sind 216 von 733** (M110): So viele Prozesse von `NEXANS` tragen keinen zugeordneten
> Partner. **390** ist die Prozesszahl von `VOTG`.
>
> *Gemessen war: 216 Prozesse ohne zugeordneten Partner bei `NEXANS`, am 02.09.2026. Behauptet wird
> im Auftrag: 390.* **Die Entscheidung ändert sich dadurch nicht** — 216 von 733 sind 29,5 % und
> begründen die Ablehnung der Projektebene genauso gut wie 53 %. Die Zahl steht hier, weil eine
> übernommene Zahl keine gemessene ist (Regel L10).

---

## 10. Die Entscheidungen dieser Runde

| | |
|---|---|
| **E‑32** | Der Endpunkt ist `GET /api/prozesse/baum` — ein Unterpfad und kein zweiter Wurzelpfad (§1) |
| **E‑33** | Der Baum steht vollständig in einer Antwort; kein Knoten wird nachgeladen (§1) |
| **E‑34** | „Letzte Bewegung" als `ORDER BY … DESC` mit Deckelung, nicht als `MAX()` — gemessen, Faktor 22 im schlechtesten Fall (§3) |
| **E‑35** | Die drei Zustände hängen allein an der letzten Bewegung und sind **fensterunabhängig**; `NIE` vor `STILL` vor `BEWEGT` (§4) |
| **E‑36** | „nie" kommt aus dem Rollup und nicht aus `process_catalog.traegt_nachrichten` — gemessen: 0 Widersprüche, aber der Katalog kennt einen vierten Zustand, den der Baum nicht abbilden könnte (§4) |
| **E‑37** | Die Drei-Monats-Schwelle steht als benannter `Period` an einer Stelle und **in der Antwort** (§4) |
| **E‑38** | Kein Standardfenster nach Belegung wie im Dashboard; die Vorgabe ist `48H`. Der Baum ist auch bei leerem Fenster vollständig — eine Probe löste ein Problem, das diese Ansicht nicht hat (§1) |
| **E‑39** | Partner alphabetisch und nicht nach Volumen; „nicht zugeordnet" am Ende (§2) |
| **E‑40** | „Gepflegt und leer" und „offen" fallen bei der Richtung in **denselben** Knoten. Es entsteht **kein** drittes Katalogfeld (§2) |
| **E‑41** | Gruppiert wird über den hochgestellten Schlüssel — die Gleichheit kommt von der Spaltensortierung. Angezeigt wird der Rohwert (§2) |
| **E‑42** | Zwei Statements; die Mandantenkette steht im Gerüst als Join und in den Kennzahlen als `EXISTS` (§6) |
| **E‑43** | Überfälligkeit steht **nicht** im Baum (§5) |
| **E‑44** | `Rollupzeitraum` (vormals `Dashboardzeitraum`) und `Katalogzuordnung` wandern nach `common`, weil ein zweites Fachpaket sie braucht |

### Die Entscheidungen der Oberfläche (10c‑2)

| | |
|---|---|
| **E‑45** | Eine Richtungsebene, die nur **einen** Knoten trüge, wird übersprungen; die Richtung wandert als Zeichen in die Prozesszeile (§17) |
| **E‑46** | Das Überspringen geschieht in der **Oberfläche**, nicht im Endpunkt — dessen Antwort bleibt vollständig (§17) |
| **E‑47** | Die Ansicht liegt in `features/nachrichten`; der gemeinsame Teil wandert nach `lib/rollupzeitraum.ts` und `components/zeitraum-umschalter.tsx` — nicht ins Nachbarfeature (§15) |
| **E‑48** | Der Schalter „nur mit Verkehr im Zeitraum" steht **in der URL**, das Eingrenzungsfeld nicht: Der eine lässt weg, das andere ist eine Eingabe (§15) |
| **E‑49** | Die Baumspalte ist **26 rem** ab `xl` und darunter ein Anteil von 40 % — gemessen an drei Zahlen, nicht gewählt (§16, M118) |
| **E‑50** | Die Übertragungsliste bekommt **das Fenster aus der Antwort des Baums** (`von`/`bis`) und keinen eigenen Zeitraum — beide lesen dieselbe Spalte (§18) |
| **E‑51** | Die Baumzeile hält `--dichte-beruehrung` und nicht `--dichte-zeile`; die Folge ist, dass der Dichteumschalter im Baum fast nichts bewirkt (§16) |
| **E‑52** | Die Zeile bekommt ihren vorgelesenen Namen aus **einem `aria-label`** und nicht aus `sr-only`-Spannen neben jeder Zahl (§17) |
| **E‑53** | Beim Öffnen des Panels weicht die **Liste**, der Baum bleibt. Im Auftrag hieß sie `E‑44`; die Nummer war seit 10c‑1 vergeben (§18) |

---

## 11. Tests

| Test | Was er hält |
|---|---|
| `ProzessbaumServiceTest` | ohne Datenbank: Schachtelung, die drei Zustände, die Schwelle samt Grenzfall, die Summen je Ebene, die Reihenfolge, die Schreibweisenkollision. **Alle Prüfwerte erfunden** (T2) |
| `ProzessbaumStatementsTest` | die **gerenderten** Statements, wörtlich. Die vier Eigenschaften aus §6, dazu: genau zwei Statements je Aufruf und **kein** `Message` in irgendeinem davon |
| `ProzessbaumPlanDbIT` | Treiberindex und Zugriffsart über vier Mandanten und drei Paare — nicht die Laufzeit (Regel T1). **Die tragende Zusicherung:** die letzte Bewegung läuft über `message_rollup_prozess_idx` und **nicht** über `PRIMARY` |
| `ProzessbaumIsolationDbIT` | Regel M4, zehn Fälle |
| `MessungM117DbIT` | die Baumgröße je Mandant am Endpunkt. Zugesichert wird nur, dass die Antwort in sich stimmig ist; die Zahlen des Bestands werden **ausgegeben und nicht behauptet** (T2) |

### Die Verletzungsprobe — ausgeführt, nicht angenommen

Die Abnahme verlangt den Nachweis, dass der Isolationstest rot wird, wenn der Mandantenfilter fällt.
Gefahren am 02.09.2026, ein Filter je Durchgang, der Arbeitsbaum vorher committet:

| entfernt | Ergebnis |
|---|---|
| Mandantenfilter des **Gerüsts** | **5 von 9 Fällen rot**, darunter der Nachweis über den Partnernamen |
| Mandantenkette der **Kennzahlen** | **alle 9 grün** |

> ### ⚠️ Der zweite Durchgang war nicht das erwartete Ergebnis, und er hat den Test geändert
>
> **Der Grund ist die Bauform und kein Loch im Test.** Der Dienst hängt die Kennzahlen über die
> `ProcessID` an die Blätter des Gerüsts, und die sind mandantengefiltert. Eine fremde Kennzahlzeile
> fällt beim Zusammensetzen lautlos heraus und erreicht den Rumpf nie. **Für die Sicherheit ist das
> gut** — die Trennung hängt an zwei unabhängigen Riegeln —, **für den Test ist es das Gegenteil**:
> Ein Leck im zweiten Statement bliebe unsichtbar, bis jemand die Bauform des Dienstes ändert.
>
> **Regel M3 verlangt den Filter *im* Statement und nicht dahinter.** Ein Test, der nur den Rumpf
> ansieht, wäre mit einer nachgelagerten Prüfung zufrieden — also mit genau dem, was M3 verbietet.
>
> Der Test hat deshalb eine zehnte Zusicherung bekommen, und sie greift eine Ebene tiefer: am
> Repository, über zwölf Monate. Sie ist gegengeprüft — **mit entfernter Kette wird sie rot**.
>
> **Die Klassendokumentation hat vorher das Gegenteil behauptet** — dass die Summenprobe die zweite
> Kette absichere. Der Satz ist berichtigt und nicht stillschweigend gelöscht.

### Was `ProzessbaumIsolationDbIT` über die Prozessauswahl hinaus prüft

Der Baum trägt **mehr** als die Auswahl, und beides sind eigene Leckwege:

- **Partnernamen sind mandantengebunden** ([`prozess-katalog.md`](prozess-katalog.md) E3) und stehen
  nur hier. Geprüft wird gegen `VOTG` und **nicht** gegen `SUTTONS`: `SUTTONS` trägt keine
  Katalogzeile, gegen ihn liefe die Zusicherung leer. Die Verletzungsprobe hat gezeigt, dass sie
  nicht leer läuft — sie war einer der fünf roten Fälle.
- **Die Kennzahlen** kommen aus einer zweiten Abfrage mit einer zweiten Mandantenkette, siehe oben.

Dazu: beide Richtungen, `ADMIN` ohne Mandant bekommt `403` und nach dem Wechsel exakt das, was ein
`NEXANS`-Nutzer sieht, ein `?mandant=` ist wirkungslos, Baum und Prozessauswahl decken dieselbe Menge
(Regel M5), und **über alle drei Zeiträume steht derselbe Baum** — der Zeitraum ändert die Zahlen,
nie den Umfang.

---

## 12. Regelbezug

| Regel | Umsetzung |
|---|---|
| **M1** kein Endpunkt nimmt eine Mandanten-ID | ein Parameter, und der ist der Zeitraum; die Ausnahmenliste bleibt bei drei |
| **M2** Mandant als erster Pflichtparameter | `ProzessbaumRepository.geruest(MandantContext)` und `.kennzahlen(MandantContext, …)`; ArchUnit prüft es |
| **M3** Filter im Statement | Join im Gerüst, `EXISTS` in den Kennzahlen — beide Bestandteil der Bedingung, beide gegengeprüft |
| **M4** Isolationstest je Endpunkt | `ProzessbaumIsolationDbIT`, zehn Fälle, Verletzungsprobe gefahren |
| **M5** Trennung gilt auch quer | Baum und Prozessauswahl decken dieselbe Menge |
| **L1** Pflicht-Zeitfenster | jede Kennzahl steht in einem der drei Paare; ohne Angabe gilt `48H`. Das **Gerüst** hat keines, und das ist die Anwendung von L1 und nicht ihre Umgehung: Es liest Stammdaten, dieselbe Begründung wie [`prozessauswahl.md`](prozessauswahl.md) §3 |
| **L2** keine Live-Aggregation über `Message` | zwei Tests halten es fest; die Überfälligkeit bleibt draußen (§5) |
| **L3** keine `OFFSET`-Paginierung | es gibt keine Paginierung — Stammdaten, §3 der Prozessauswahl |
| **L7** jede Abfrage gemessen | §7 und §8, vier Mandanten, drei Fensterbreiten |
| **L10** Belegvermerk je Befundsatz | die vier ⚠️-Kästen und die kursiven Vermerke |
| **L15** Pläne festgehalten | §8, dazu `ProzessbaumPlanDbIT` als Wächter |
| **Q3** die Problemkategorien bleiben getrennt | *Fehler* im Baum, *Überfällig* nicht — und nie zusammengefasst |
| **Q4** nicht zugeordnet heißt nicht zugeordnet | `partner` und `richtung` bleiben `null`; der Text gehört in die Sprachdateien |
| **T1** kein Test behauptet Wanduhrzeit | `ProzessbaumPlanDbIT` prüft Index und Zugriffsart; `MessungM117DbIT` gibt Zahlen aus und sichert sie nicht zu |
| **T2** kein Test hängt an veränderlichen Daten | die Diensttests bauen sich jede Zeile selbst; die Datenbanktests vergleichen zwei Antworten miteinander |
| **Z1** kein direkter `now()`-Aufruf | ein Uhrenschlag je Anfrage, aus der Anwendungsuhr, weitergereicht an Fenster **und** Schwelle |

---

## 13. Offene Punkte

| | |
|---|---|
| **105** | **`toUpperCase` ist eine Näherung an `utf8mb4_general_ci`.** Die Sortierung ignoriert auch Akzente; zwei Werte, die sich nur darin unterscheiden, blieben im Baum zwei Gruppen und wären für die Datenbank eine. Der exakte Weg wäre `WEIGHT_STRING` in SQL — **ungemessen**, und ein MariaDB-eigener Funktionsaufruf im Antwortpfad will eigens gemessen sein (§2) |
| **106** | **Ein Partner steht bei `NEXANS` im Katalog in zwei Schreibweisen.** Das ist eine Kuratierungsfrage und kein Codefehler; welche der beiden richtig ist, entscheidet niemand im Code. Der Baum zeigt seit E‑41 einen Knoten und die zuerst angetroffene Schreibweise (§2) |
| ~~**107**~~ | ~~**Bei `IBIS` ordnet der Baum kaum noch:** 139 Gruppen auf 192 Prozesse, größte Gruppe vier.~~ **Beantwortet am 02.09.2026, und die Antwort ist eine Zahl und keine Umgestaltung:** E‑45 nimmt `IBIS` 19 der 139 Gruppen; es bleiben 120 Richtungsknoten auf 192 Prozesse, also **391 Zeilen für 192 Blätter** (§17). Der Baum verlängert dort weiterhin mehr, als er ordnet. **Ob das den Nutzer stört, ist nicht geprüft** — dafür fehlt die Sichtprüfung, siehe **115**. Eine zweite Bauform ist bewusst nicht entstanden: Sie wäre eine zweite Ansicht für denselben Endpunkt |
| ~~**108**~~ | ~~**`VOTG` trägt keine einzige kuratierte Richtung.**~~ **Geschlossen am 02.09.2026 durch E‑45:** Die Ebene wird übersprungen, wo sie nur einen Knoten trüge. Am gerenderten Baum ausgezählt: `VOTG` hat **null** Richtungsknoten statt 133, und keine einzige leere Ebene bleibt stehen (§17). Die Richtung ist nicht verschwunden — sie steht als Zeichen in jeder der 390 Prozesszeilen |
| ~~**109**~~ | ~~**Der Rumpf ist bei `NEXANS` 150,3 KiB.**~~ **Geschlossen am 02.09.2026.** Erst zur Hälfte (§16, M120): Auswerten und Layouten des Baums kosten im Browser **9,2 ms zugeklappt** und 84,7 ms vollständig aufgeklappt; `JSON.parse` 0,42 ms, die Eingrenzung 0,07 ms je Tastendruck, das Scrollen 0,3 ms über dreißig Sprünge. Die fehlende Hälfte steht seit der Sichtprüfung da (§21, M123): **über die Leitung sind es 14,5 KiB**, der Endpunkt antwortet in 60 ms, und **React braucht 98 ms** von der fertigen Antwort bis zum Baum im DOM. Alle 425 Gruppen aufzuklappen kostet 454 ms |
| **110** | **Die Antwort unterscheidet bei der Richtung nicht zwischen „gepflegt und leer" und „offen".** Dass die Unterscheidung gebraucht würde, ist **nicht gemessen**; ein drittes Katalogfeld entsteht deshalb nicht (§2) |
| **111** | **Die Feldnamen der Antwort sind deutsch, Richtlinie §5.2 schreibt englisch.** Das Dashboard hat sich schon anders entschieden; diese Datei folgt den Nachbarn. Ob die Regel nachzuziehen oder das Projekt zurückzudrehen ist, ist eine eigene Runde (§1) |
| **112** | **Überfälligkeit je Prozess ist nicht gemessen.** Wer den Punkt aufmacht, misst nicht die Zahl der Überfälligen, sondern den **Plan** einer Gruppierung je Prozess — und stellt daneben, was der Endpunkt dann an Bauform mitbekäme: ein drittes Statement, das als einziges live liest, samt Teilerfolg-Mechanismus (§5) |
| **113** | **Die Ebenenzuordnung Paar → Rolluptabelle steht zweimal im Code** — in `DashboardRepository` und in `ProzessbaumRepository`. Sie ließe sich nicht nach `common` heben, ohne die generierten Tabellen dorthin mitzunehmen. Das vollständige `switch` ohne `default` macht die Doppelung compilergesichert, aber sie bleibt eine Doppelung |

### Aus Teil 2, der Oberfläche (02.09.2026)

| | |
|---|---|
| **114** | **Bei 1280 px steht die Übertragungsliste neben dem Baum nicht vollständig.** Ihre Tabelle braucht gemessene **744 px**; darunter fällt die **Ablaufspalte auf 0 px** und die Tabelle scrollt waagerecht in ihrem eigenen Container (M119, §18). **Es liegt nicht an der Breite des Baums** — dafür dürfte er höchstens 16 rem messen, und dort bricht mehr als die Hälfte aller Prozessnamen um. Wer den Punkt aufmacht, entscheidet über die **Spaltenbreiten der Nachrichtenliste** und nicht über diese Ansicht. ⚠️ **Verschärft am 02.09.2026 durch die Sichtprüfung** (§21, M126): Die 0 px breite Spalte beschneidet ihre Beschriftung nicht — bei 1280 px steht „Ablauf" **über „Projekt" gedruckt**, und der Tabellenkopf ist an der Breite unlesbar, für die E‑53 entworfen wurde. **Fortgeschrieben am 02.09.2026 mit E‑57** (§27, M129): Der Befund **wandert mit** — die Liste steht künftig neben dem Panel statt neben dem Baum, und der Klumpen steht dort genauso. Er wird dabei **nicht schlimmer, und das ist gerechnet und gemessen**: Bis `2xl` ersetzt eine 26‑rem-Spalte die andere, die Liste bekommt neben dem Panel **auf den Pixel dieselbe Breite** wie neben dem Baum (gemessen 585 px bei 1280, 745 px bei 1440 — in beiden Zuständen gleich). Erst ab `2xl` wächst das Panel auf 30 rem, und dort verliert die Liste **64 px** (777 statt 841 bei 1536, 1.161 statt 1.225 bei 1920) — die Ablaufspalte fällt von 95 auf 31 px beziehungsweise von 479 auf 415 px, bleibt aber sichtbar. **Nicht in dieser Runde zu lösen**; wer den Punkt aufmacht, entscheidet weiterhin über die Spaltenbreiten der Nachrichtenliste |
| ~~**115**~~ | ~~**Es hat keine Sichtprüfung im Browser gegeben.**~~ **Nachgeholt am 02.09.2026, §21.** Die Anmeldung stand später am selben Tag zur Verfügung; jeder einzelne der hier aufgezählten Punkte ist abgearbeitet: die drei Breitenzustände (M124), die sichtbaren Zeilen je Dichtestufe (M121 — **19/19/18/16** gegen 28/26/24/19 in der Liste), die Renderzeit einschließlich React (M123), der tiefe Link, die Tastatur von Hand, die Ungleichförmigkeit aus E‑45 (135 Partner mit Ebene, 20 ohne, alle zwanzig namentlich), und die zwei Verhaltensregeln aus §19 — **der Fokus unter `md` landet auf „Zurück zum Baum", und der Baum springt bei geöffnetem Panel nicht.** Was die Sichtprüfung **neu** gefunden hat, steht als **118**, **119** und in der Verschärfung von **114** |
| **116** | **`features/nachrichten` trägt drei Ansichten, und sein Name sagt das nicht** — Liste, Belegsuche, Prozessansicht (E‑47). Ob das Verzeichnis anders heißen soll, ist eine Umbenennung und keine Umstellung; sie beträfe rund vierzig Importpfade und keinen Nutzer |
| ~~**117**~~ | ~~**Der Dichteumschalter bewegt im Baum fast nichts** (E‑51): Die Zeile hält `--dichte-beruehrung`, und das Token ist aus der Skalierung heraus — 44 px in `xs`, `s` und `m`, 49,5 px in `l`.~~ **Geschlossen am 02.09.2026 durch E‑54** (§24): Die Baumzeile trägt `--dichte-bedienzeile` — am Zeigergerät die Zeilenhöhe, am Berührungsgerät weiterhin die Mindestfläche. Nachgemessen (M127): **26 / 24 / 22 / 19** sichtbare Zeilen statt 19/19/18/16, also **sieben Zeilen über die Skala** statt drei; `xs` und `s` unterscheiden sich wieder. Am Berührungsgerät sind es unverändert 44 / 44 / 44 / 49,5 px. **Die Prozessauswahl ist mitgezogen** — dieselbe Wahl, dieselbe Begründung. **Offener Punkt 96 bleibt offen:** `--dichte-beruehrung` selbst ist unberührt |

### Aus der Sichtprüfung (02.09.2026)

| | |
|---|---|
| **118** | ⚠️ **Wer weit unten im Baum auswählt, bekommt das Ergebnis außerhalb des Bildes** (§21, M125). Baum und Liste teilen den **einen** Scrollbereich des Anwendungsrahmens ([`frontend-grundlagen.md`](frontend-grundlagen.md) §7); bei 155 Partnern steht die Überschrift der rechten Spalte dann bis zu **6.143 px** über dem Sichtfenster, und rechts bleibt eine leere Fläche. Betroffen ist alles unterhalb der ersten 18 bis 19 Zeilen, bei `NEXANS` also gut 130 der 155 Partner. **Zwei Auswege, beide Entscheidungen über §7 und nicht über diese Datei:** die rechte Spalte kleben lassen (`sticky`, wie der Baumkopf schon) — das widerspricht §7 nicht —, oder dem Baum einen eigenen Scrollbereich geben — das widerspricht ihm ausdrücklich |
| **119** | **Der Rückweg unter `md` verliert den Fokus** (§21, Befund 3). „Zurück zum Baum" verschwindet mit dem Kopf der rechten Spalte und nimmt den Fokus auf `document.body` mit; der nächste Tabulator beginnt wieder oben am Anwendungsrahmen. Der Weg **hin** ist eigens dagegen gebaut (§15) — die Gegenrichtung ist es nicht. Der Fokus gehörte auf die zuletzt gewählte Zeile im Baum, der ja wieder sichtbar ist |

### Aus den Korrekturen 10c‑3 (02.09.2026)

| | |
|---|---|
| **120** | **Die Zahl „noch nie" aus der Kopfzeile ist im Baum nicht mehr einzeln auffindbar** (E‑56, §17). Der Baum zeigt den Zustand nicht mehr an der Zeile; wer wissen will, **welche** der 217 Prozesse bei `NEXANS` nie etwas getragen haben, hat dafür den Schalter „Nur mit Verkehr im Zeitraum" — er blendet genau die Gegenmenge aus, und was stehenbleibt, ist die Menge „nie" plus die stillen. **Dass das ausreicht, ist eine Auslegung und keine Messung**: Es ist ein Weg über zwei Schritte statt einer Angabe in der Zeile, und ein Nutzer ist dazu nicht befragt worden. Wer den Punkt aufmacht, entscheidet zwischen „Zeile trägt es wieder" (dann samt Dämpfung, §3) und „die Kopfzeile bekommt einen Filter je Zustand" |
| **121** | **Unter `xl` aktualisiert die verdeckte Liste weiter, und ihr Schalter steckt im verdeckten Bereich** (§18, entfallene Sonderregel). Ab `xl` steht die Liste seit E‑57 neben dem Panel und ist bedienbar; darunter weicht sie, und dann läuft ihr Intervall für eine Liste, die niemand sieht — abschalten kann der Nutzer sie nicht. **Das ist nicht neu und nicht auf diese Ansicht beschränkt:** Die Nachrichtenliste trägt denselben Fall seit Schritt 5, mit derselben Ursache und ohne Gegenmittel. Zwei Auswege, und beide sind Entscheidungen über die **geteilte** Liste: den Aktualisierungsschalter aus dem Blätterblock in den Kopf der Ansicht heben (dann ist er immer erreichbar), oder die Sichtbarkeit an einen `IntersectionObserver` hängen (dann ist es kein zweiter Umbruchpunkt, sondern eine Messung). **Die Vorgabe ist aus** — der Fall tritt nur ein, wenn ein Nutzer die Aktualisierung selbst eingeschaltet hat |

---

## 14. Was **dieser Schritt** nicht zeigt

*Der Stand von 10c‑1. Was davon 10c‑2 eingelöst hat, ist gekennzeichnet; die Sätze bleiben stehen,
damit erkennbar bleibt, was am 02.09.2026 noch offen war.*

1. ~~**Keine Oberfläche.**~~ ✔ **Eingelöst in 10c‑2** — §15 bis §20. Die Zahlen aus §9 waren die
   Vorlage und haben getragen: Vorklappen scheidet aus, ein Aufruf genügt, Virtualisieren ist
   gemessen unnötig.
2. ~~**Keine Sichtprüfung im Browser.**~~ ✔ **Nachgeholt am 02.09.2026** — §21. Beim Schreiben
   dieser Zeile fehlte der Anmeldezugang; er stand später am selben Tag zur Verfügung. Der Endpunkt
   ist über `MessungM117DbIT` und die Isolationstests abgenommen, die Ansicht über ihre Tests, über
   Messungen an der gebauten Komponente **und jetzt am laufenden System** (M121 bis M126).
3. **Keine Messung gegen die Produktion.** Alle Zahlen stammen von der Testkopie mit 3,34 Millionen
   Zeilen in `Message` und 335.610 im Rollup. Die Projektbeschreibung rechnet mit bis zu 36
   Millionen; die Rolluptabelle wächst mit, und die Kennzahlenabfrage liest sie im Bereich.
   **Für die Oberfläche gilt dasselbe an einer anderen Stelle:** Die Baumgröße wächst mit dem
   Katalog, nicht mit der Nachrichtenmenge — 733 Blätter sind der heutige Höchstwert.
4. ~~**Kein Filter, keine Suche im Baum.**~~ ✔ **Eingelöst in 10c‑2** (§17): örtlich, ohne
   Entprellung, gemessene 0,07 ms je Tastendruck — dieselbe Abwägung wie in
   [`prozessauswahl.md`](prozessauswahl.md) §9 und mit demselben Ergebnis.
5. ~~**Kein Einstieg aus dem Baum in die Liste.**~~ ✔ **Eingelöst in 10c‑2** (§18): Der vorhandene
   Prozessfilter ist verdrahtet und nicht nachgebaut, und das Zeitfenster kommt aus der Antwort des
   Baums (E‑50).

---

# Die Oberfläche (Schritt 10c‑2, 02.09.2026)

Entsteht in **Schritt 10c‑2**. Auftrag: „Schritt 10c‑2: Prozessansicht, Oberfläche", Stand
02.09.2026. Alles ab hier beschreibt die Ansicht; §1 bis §14 bleiben unverändert und beschreiben
den Endpunkt darunter.

**Kein Feld ist am Backend ergänzt worden.** Was hier steht, ist ausschließlich Darstellung dessen,
was §1 bis §9 liefert.

## Nummernvergabe (Teil 2)

| | |
|---|---|
| **Messungen** | **M118 bis M120**. `grep -rnoE '\bM1(1[89]\|20)\b' docs/ scripts/ *.md` → kein Treffer außerhalb dieser Datei; höchste vergebene war **M117** (§9). **Die Sichtprüfung hat M121 bis M126 belegt** (§21) |
| **Entscheidungen** | **E‑45 bis E‑53**. `E‑45` und `E‑46` sind im Auftrag vergeben, `E‑47` bis `E‑53` hier; höchste vergebene war **E‑44** (§10). ⚠️ Der Auftrag führt die Panel-Entscheidung ebenfalls als `E‑44` — die Nummer war belegt, sie heißt hier **E‑53** (§18). **Die Sichtprüfung hat keine neue vergeben** — sie prüft nach, sie entscheidet nicht |
| **Offene Punkte** | ab **114**. Höchster vergebener Stand ist **113** (§13). **Die Sichtprüfung hat 118 und 119 belegt** |

---

## 15. Route, Aufteilung und Zustand

### Die Route gab es schon

`/prozesse` steht seit Schritt 3 in `lib/routen.ts` und in `lib/navigation.ts`; dort saß bis heute
ein Platzhalter mit dem Kommentar *„Die nach kuratiertem Partner gruppierte Ansicht entsteht in
Schritt 10."* **Zu tun war nicht, eine Route anzulegen, sondern eine zu füllen** — dieselbe Lage wie
beim Dashboard auf `/` (Entscheidung E‑r).

### E‑47 — Die Ansicht liegt in `features/nachrichten`, der gemeinsame Teil wandert nach `lib` und `components`

[`frontend-grundlagen.md`](frontend-grundlagen.md) §8 hat für diesen Tag vorgesorgt: *„Kommt in
Schritt 10 eine eigene Prozessansicht, wandert der gemeinsame Teil nach `components/` oder `lib/` —
nicht ins Nachbarfeature."* **Genau das ist geschehen**, und zwar zweimal:

| Was | Von | Nach | Warum |
|---|---|---|---|
| die drei Rollup-Paare `48H`/`30T`/`12M` | `features/dashboard/api.ts` | `lib/rollupzeitraum.ts` | beide Ansichten brauchen dieselbe Menge — **dieselbe Bewegung wie im Backend am selben Tag** (E‑44: `Dashboardzeitraum` → `common/Rollupzeitraum`), und der Typ heißt seither in beiden Hälften gleich |
| der Zeitraumumschalter | `features/dashboard/components/` | `components/zeitraum-umschalter.tsx` | dieselben drei Schaltflächen, dieselbe Regel „hervorgehoben ist, was gilt" |
| die Beschriftungen dazu | `texte.dashboard.zeitraum` | `texte.zeitraum` | ein Baustein in `components/` liest keinen Textblock eines Features |

**Der Baum selbst bleibt trotzdem in `features/nachrichten`, und das ist die Entscheidung.** Was die
Prozessansicht mit der Nachrichtenliste teilt, ist nicht *ein Fetch*: Ihre rechte Spalte **ist** die
Nachrichtenliste, und das Panel darüber **ist** das Nachrichtendetail. Beide nach `components/` zu
heben hieße, den halben Feature-Inhalt in die Naht zu schieben, die dort für Rahmen, Kopfzeile und
Zustände gedacht ist.

**Der Präzedenzfall steht daneben:** Die Belegsuche ist seit Schritt 7 eine eigene Route (`/suche`)
mit eigener Trefferliste und liegt aus demselben Grund in diesem Feature — sie hängt das vorhandene
Panel ein ([`bam-suche.md`](bam-suche.md) §11.4). Die Prozessansicht ist der **dritte Einstieg in
dieselbe Menge** und nicht eine zweite Menge.

*Gemessen war: dass heute kein Feature aus einem Nachbarfeature importiert (`grep` über
`src/features`, kein Treffer). Behauptet wird: dass ein Umbau der Liste nach `components/` teurer
wäre als der Name dieses Verzeichnisses.* **Der Preis ist benannt:** `features/nachrichten` trägt
damit drei Ansichten, und sein Name sagt das nicht — offener Punkt **116**.

### Die drei Breitenzustände

An den **vorhandenen** Schwellen. `md` (768 px) ist der Umbruchpunkt des Projekts
([`visuelles-konzept.md`](visuelles-konzept.md) §6), `xl` (1280 px) der, an dem das
Nachrichtenpanel neben die Liste tritt ([`nachrichtendetail.md`](nachrichtendetail.md) §10.7).
**Kein neuer entsteht.**

| Breite | Aufteilung |
|---|---|
| ab `xl` | Baum links in **fester** Breite (`--dichte-baumspalte`), Liste rechts im Rest |
| `md` bis `xl` | beide nebeneinander, der Baum mit **40 % Anteil** statt fester Breite |
| unter `md` | **eine** Spalte: Baum → Liste → Panel, mit „Zurück zum Baum" und dem Zurück des Browsers |

**Warum unter `xl` ein Anteil und keine feste Breite.** Bei 768 px bleiben nach Navigationsspalte
und Innenabstand rund 520 px Inhalt. Eine feste Baumspalte von 26 rem (416 px) ließe der Liste gut
hundert — die Aufteilung wäre dem Namen nach zweispaltig und der Sache nach keine. Mit 40 % wächst
der Baum mit dem Fenster und übernimmt bei 1280 px fast genau seine feste Breite: **412 px gegen
416 px**, der Sprung an der Schwelle ist vier Pixel breit.

**Unter `md` wird ausgeblendet, nicht ausgehängt** (`display: none`) — dieselbe Bauform wie in der
Nachrichtenliste. Der Baum behält seinen Aufklappzustand und die Liste ihre Seitenposition; wer
zurückgeht, findet beides wieder, ohne dass eine zweite Abfrage auf die Produktionsdatenbank geht.

**Umgesetzt über Klassen und nicht über eine Abfrage der Fensterbreite in JavaScript.** Die wäre ein
zweiter Umbruchpunkt neben dem der Ansicht, und zwei laufen auseinander — dieselbe Festlegung wie
beim Ansichtsumschalter des Nachrichtendetails.

### Der Zustand in der URL

```
/prozesse?zeitraum=30T&nurMitVerkehr=true&prozess=<ProcessID>&nachricht=<MessageID>&sortierung=aelteste
```

| Parameter | Verlauf | Warum |
|---|---|---|
| `zeitraum` | `replace` | wie im Dashboard: **nur die ausdrückliche Wahl**, nie das vom Endpunkt gewählte Paar (E‑n) |
| `prozess` | **`push`** | er *öffnet* etwas: unter `md` tritt die Liste an die Stelle des Baums, und das Zurück des Browsers ist dort der Weg heraus |
| `nachricht` | **`push`** | dasselbe eine Ebene tiefer; derselbe Parametername wie in Liste und Belegsuche (`lib/routen.ts` `NACHRICHT_PARAMETER`) |
| `nurMitVerkehr` | `replace` | siehe E‑48 |
| `sortierung` | `replace` | die Übertragungsliste behält ihren Sortierumschalter, und der beschreibt den gezeigten Ausschnitt |

**Was ausdrücklich *nicht* in der URL steht**, und beides ist die Prüfung aus
[`frontend-grundlagen.md`](frontend-grundlagen.md) §8:

- **Der aufgeklappte Partner.** Er ergibt sich aus dem gewählten Prozess (`pfadZuProzess`). Zwei
  Zustände für dieselbe Sache liefen auseinander, und ein Link mit `prozess=…` und einem
  widersprechenden Aufklappzustand wäre nicht mehr zu deuten.
- **Der Cursor der Liste.** Aus demselben Grund wie dort: Ein Link auf Seite sieben eines Fensters
  zeigte beim Empfänger auf andere Zeilen.

### E‑48 — Der Schalter steht in der URL, das Eingrenzungsfeld nicht

Beides schränkt ein, und trotzdem gehört nur eines hinein. Die Regel dafür steht seit dem
11.08.2026 fest: *Was ausgeblendet ist, muss man teilen können* — und sie hat einen Anlassfall,
`zwischenschritte` in der Nachrichtenliste.

| | in der URL | warum |
|---|---|---|
| **„Nur mit Verkehr im Zeitraum"** | **ja** | Er **lässt weg**. Wer bei `VOTG` einen Link weitergibt, in dem 354 von 390 Prozessen fehlen, muss das mitgeben — sonst sieht der Empfänger einen anderen Baum und weiß nicht, warum |
| **Das Eingrenzungsfeld** | **nein** | Es ist eine **Eingabe**, durch die man beim Tippen hindurchläuft — dieselbe Bauform wie das Eingrenzungsfeld der Prozessauswahl, das seit Schritt 4 im Komponentenzustand liegt |

**Ohne `clearOnDefault: false`, und das ist geprüft und nicht übersehen.** Die Vorgabe des Schalters
ist `false` und lässt nichts weg; ein Standardwert, der etwas *zulässt*, gehört nicht in die URL —
genau wie `langeSuche` und `ueberfaellig` in `filter.ts`.

**Die Vorgabe ist aus.** Bei `VOTG` sind 89,7 % der Prozesse „nie" (M111); ohne den Schalter ist der
Baum dort fast vollständig gedämpft. **Mit Vorgabe *an* wäre dagegen genau der Prozess unauffindbar,
den jemand sucht, *weil* er nichts trägt** — und das ist der Fall, für den es diese Ansicht gibt
([`prozessauswahl.md`](prozessauswahl.md) §3).

---

## 16. Die Baumspalte — gemessen, nicht gewählt

### M118 — wie viele Namen umbrechen, bei welcher Breite, in welcher Stufe

**Gemessen an der gebauten Komponente.** Der Baum ist über `react-dom/server` mit den echten Daten
der Testkopie zu HTML gerendert und in die laufende Anwendung eingesetzt; damit gelten die echte
Schrift (Geist über `next/font`), die echten Tokens aus `globals.css` und die echten Klassen der
Zeile. Kopfloses Chrome über das DevTools-Protokoll, Fenstermaße über
`Emulation.setDeviceMetricsOverride` — dieselbe Strecke wie in
[`dichte-umschalter.md`](dichte-umschalter.md) §5.

Gezählt ist ein **Blatt, dessen Prozessname mehr als eine Zeile braucht** (Höhe der Namensspanne
gegen die gemessene `line-height`).

| Breite (Stufe `xs`) | `NEXANS` (733) | `VOTG` (390) | `IBIS` (192) | `SUTTONS` (17) | zusammen (1.332) |
|---:|---:|---:|---:|---:|---:|
| 18 rem | 396 | 366 | 189 | 16 | 967 (72,6 %) |
| 20 rem | 200 | 304 | 151 | 10 | 665 (49,9 %) |
| 22 rem | 78 | 225 | 88 | 4 | 395 (29,7 %) |
| 24 rem | 38 | 145 | 39 | 2 | 224 (16,8 %) |
| **26 rem** | **17** | **99** | **16** | **0** | **132 (9,9 %)** |
| 28 rem | 7 | 33 | 6 | 0 | 46 (3,5 %) |
| 30 rem | 1 | 16 | 3 | 0 | 20 (1,5 %) |
| 32 rem | 1 | 0 | 2 | 0 | 3 (0,2 %) |
| 34 rem | 0 | 0 | 0 | 0 | 0 |

> ### ⚠️ Der Befund, der die Tabelle kurz macht: **die Dichtestufe ändert nichts**
>
> Die Messung lief über alle vier Stufen. **Die Zahlen sind in `xs`, `s`, `m` und `l` identisch** —
> mit genau einer Abweichung: `IBIS` bei 22 rem, 88 Umbrüche in `xs` gegen 89 in den drei anderen.
>
> **Der Grund ist keine Eigenheit dieser Ansicht, sondern die Definition von `rem`.** Spaltenbreite
> und Schriftgröße liegen beide in `rem` und skalieren mit derselben Zahl; das Verhältnis von Text
> zu Kasten bleibt. Die Stufe ändert die **Pixel**, nicht die **Umbrüche**.
>
> Daraus folgt für jeden, der die Zahl anfasst: **Eine Breite in `rem` gilt für alle vier Stufen
> zugleich.** Sie in einer Stufe zu prüfen genügt — und sie in einer Stufe zu verbessern, verbessert
> sie in allen.

### Warum `VOTG` und nicht `NEXANS` der schwierige Fall ist

Bei 26 rem brechen 2,3 % der `NEXANS`-Namen um und 25,4 % der `VOTG`-Namen. **Das liegt nicht an der
Einrückung** — bei `VOTG` fällt die Richtungsebene überall weg (E‑45), die Blätter stehen also eine
Ebene **weiter links** und haben 1,25 rem mehr Platz. Es liegt an den Namen:

| | längster | Median | mit Leerzeichen |
|---|---:|---:|---:|
| `NEXANS` | 58 Zeichen | **21** | 315 von 733 (43 %) |
| `VOTG` | 54 | **34** | 386 von 390 (99 %) |
| `IBIS` | 56 | 30 | 192 von 192 (100 %) |
| `SUTTONS` | 43 | 31 | 17 von 17 (100 %) |

**Der Median entscheidet und nicht das Maximum.** `NEXANS` führt technische Kurznamen
(`40000_AMG_LAB_VDA`), `VOTG` und `IBIS` führen Sätze (*„Ausgehender Anhang Rechnung Integra
Petrochemicals Ltd"*). Die 58 Zeichen aus [`prozessauswahl.md`](prozessauswahl.md) §7a sind
bestätigt und **für diese Frage die falsche Zahl**.

### Die Gesamthöhe des offenen Baums, Stufe `m`

| Breite | `NEXANS` | `VOTG` | `IBIS` |
|---:|---:|---:|---:|
| 18 rem | 60.704 px | 36.348 px | 22.134 px |
| 22 rem | 55.570 | 32.788 | 19.900 |
| **26 rem** | **54.626** | **30.126** | **18.684** |
| 30 rem | 54.394 | 28.320 | 18.458 |
| 34 rem | 54.382 | 27.968 | 18.422 |

Bei `NEXANS` liegt die Höhe ab 26 rem innerhalb von **0,4 %** ihres Grenzwerts; jeder weitere
Zentimeter Breite kauft dort nichts mehr. Bei `VOTG` sind es 7,7 % — dort trägt Breite länger.

### E‑49 — 26 rem, und die drei Zahlen, die sie tragen

1. **Bei `NEXANS` brechen 17 von 733 Namen um (2,3 %)**, und die Höhe ist praktisch am Grenzwert.
   `NEXANS` ist der Fall, für den zu entwerfen ist (§9).
2. **Es ist der größte Wert, bei dem die Übertragungsliste daneben bei 1440 px noch vollständig
   steht** — die Tabelle braucht gemessene **744 px** (M119, §18).
3. **Es ist die Breite, die das Nachrichtenpanel ab `xl` ohnehin hat.** Eine Zahl statt zwei.

**Was gegen 30 rem spricht, ist allein Punkt 2.** Der Baum wäre dort besser (1 von 733 statt 17),
und die Liste bei 1920 px merkte nichts davon. Sie merkte es bei **1440 px**, und das ist eine
verbreitete Bildschirmbreite: 1440 − 26 rem lässt der Liste gerade genug, 1440 − 30 rem nicht mehr.

> **Belegvermerk (Regel L10).** *Gemessen:* die Umbruchzahlen, die Gesamthöhen und die 744 px
> Mindestbreite der Tabelle. *Gerechnet:* welche Fensterbreite daraus folgt — 26 rem (416 px) plus
> 16 px Abstand plus 744 px sind 1.176 px Inhalt; dazu 40 px Innenabstand, 208 px Navigationsspalte,
> 1 px Trennlinie und rund 15 px Bildlaufleiste ergeben **1.440 px**. Die 15 px stammen aus der
> Messung in [`nachrichtendetail.md`](nachrichtendetail.md) §10.7 (1.697 px gemessen gegen 1.712 px
> gerechnet bei 1920) und sind **nicht** für diese Ansicht neu erhoben. ~~*Nicht gemessen:* die
> Schwelle selbst am laufenden System.~~ **Nachgemessen am 02.09.2026** (§21, M124): Bei 1.440 px
> steht die Liste vollständig (Kasten 758 px, Tabelle 758 px, kein Querlauf), bei 1.280 px nicht
> (598 gegen 744). **Die gerechnete Schwelle trägt** — mit der Einschränkung, dass die Ablaufspalte
> bei 1.440 px nur noch 14 px misst; „vollständig" heißt hier „ohne Querlauf", nicht „gut lesbar".

### E‑51 — Die Baumzeile hält die Berührungsfläche, nicht die Tabellenzeilenhöhe

`--dichte-beruehrung` (`max(2.75rem, 44px)`) und nicht `--dichte-zeile` (2,25 rem). **Der Baum ist
die Bedienfläche dieser Ansicht** — jede Zeile ist ein Ziel, das geklappt oder gewählt wird —,
während eine Tabellenzeile eine Zeile Daten ist. Dieselbe Wahl trifft die Prozessauswahl seit
Schritt 4.

**Der Preis ist gemessen und er ist die interessanteste Zahl dieser Runde:**

| Stufe | Wurzelschrift | Partnerzeile | Blatt, kürzeste | Blatt, Schnitt (`NEXANS`, 26 rem) | Blatt, längste |
|---|---:|---:|---:|---:|---:|
| `xs` | 14 px | **44 px** | 44 px | 46,5 px | 70 px |
| `s` | 15 px | **44 px** | 44 px | 47,6 px | 75 px |
| `m` | 16 px | **44 px** | 44 px | 49,0 px | 80 px |
| `l` | 18 px | **50 px** | 50 px | 55,4 px | 90 px |

**Der Dichteumschalter bewegt im Baum fast nichts**, und das ist kein Fehler, sondern die Folge
einer bewussten Festlegung: `--dichte-beruehrung` ist aus der Skalierung **heraus**
([`visuelles-konzept.md`](visuelles-konzept.md) §5), damit der Satz „wird nirgends unterschritten"
wahr bleibt. In `xs`, `s` und `m` greift überall der Boden von 44 px; erst `l` rechnet darüber
hinaus.

**Zum Vergleich, und der Vergleich ist der Auftrag:** In der Nachrichtenliste bringt der Umschalter
**28 / 26 / 24 / 19** sichtbare Zeilen ([`dichte-umschalter.md`](dichte-umschalter.md) §5.3). Im
Baum sind es bei zugeklappten Partnern **rechnerisch 22,7 / 22,7 / 22,7 / 20,2 Zeilen je 1.000 px** —
drei gleiche Stufen und eine, die weniger zeigt.

> **Das ist ein Befund und keine Empfehlung.** Ob der Baum stattdessen `--dichte-zeile` tragen
> sollte, ist eine Entscheidung über die **Berührungsfläche** und nicht über den Baum: Sie beträfe
> die Prozessauswahl genauso, und sie hängt an offenem Punkt 96
> ([`dichte-umschalter.md`](dichte-umschalter.md) §5.4), wo bereits steht, was das Token heute trägt
> und was nicht. Geführt als offener Punkt **117**.

> ### ⚠️ Korrektur vom 02.09.2026 — **E‑51 ist durch E‑54 ersetzt** (§24)
>
> Der Befund ist entschieden worden, und zwar gegen E‑51: Die Baumzeile trägt seither
> **`--dichte-bedienzeile`** — am Zeigergerät die Zeilenhöhe, am Berührungsgerät weiterhin die
> Mindestfläche. Die Tabelle oben und die Zahlen darunter beschreiben damit den Zustand **bis** zum
> 02.09.2026; sie bleiben stehen, weil die neue Entscheidung nur mit ihnen zu lesen ist.
>
> Nachgemessen als **M127** (§24): **26 / 24 / 22 / 19** sichtbare Zeilen gegen die 19/19/18/16
> hier — und am Berührungsgerät unverändert **44 / 44 / 44 / 49,5 px**. **Offener Punkt 117 ist
> damit geschlossen, offener Punkt 96 ausdrücklich nicht:** `--dichte-beruehrung` selbst ist
> unberührt geblieben.

---

## 17. Der Baum

### Die Bauform: `role="tree"`, flach im DOM

`role="tree"` am Behälter, `role="treeitem"` je Zeile, dazu `aria-level`, `aria-posinset`,
`aria-setsize`, `aria-expanded` an Gruppen und `aria-selected` an jeder Zeile.

**Gerendert wird die Liste der sichtbaren Zeilen und kein geschachtelter Baum.** Die Tiefe steht in
`aria-level` — genau die Form, die die ARIA-Spezifikation für einen *flattened tree* vorsieht. Zwei
Gründe:

1. **Die Tastaturbedienung wird zu „eine Zeile weiter".** Pfeil auf und ab bewegen sich über die
   *sichtbaren* Knoten; auf einer flachen Liste ist das ein Index, auf einem geschachtelten Baum ein
   Durchlauf.
2. **Der DOM bleibt flach.** Bei `NEXANS` vollständig aufgeklappt sind es 1.158 Zeilen; eine
   Schachtelung legte 425 zusätzliche Gruppenelemente darum.

**Kein Accordion-Baustein**, und die Prüfung, die der Auftrag verlangt, fällt aus zwei Gründen gegen
ihn aus. **Die Bauform**: Ein Accordion ist eine Folge unabhängiger Abschnitte, ein Baum eine
Hierarchie mit `aria-level`; die Tastaturbedienung ist eine andere. **Die Menge**: 155
Partnerknoten, 270 Richtungsknoten, 733 Blätter — jeder Abschnitt eines Accordions brächte eine
eigene Zustandsverwaltung mit.

> **Zur Zählweise, einmal für diesen ganzen Abschnitt.** §9 zählt **154 Partner** und **290
> Gruppen** — die kuratierten Werte, aus der Quelle erhoben (M117). §17 zählt **Knoten**: 155, denn
> „nicht zugeordnet" ist im Baum ein Partnerknoten wie jeder andere, und 270, weil E‑45 zwanzig
> Richtungsgruppen keine eigene Zeile mehr gibt. **Beide Zählungen sind richtig und zählen
> Verschiedenes.** Die 155 aus §2 ist etwas Drittes: die *falsche* Zahl der Schreibweisenkollision,
> die E‑41 behoben hat.

### Ein Tabstopp, nicht 1.158

Roving `tabindex`: Genau eine Zeile trägt `tabIndex={0}`, alle anderen `-1`. Wer aus dem
Eingrenzungsfeld heraus tabbt, landet im Baum und nicht in seiner ersten von tausend Zeilen.

**Der Tabstopp liegt auf der gewählten Zeile, wenn es eine gibt** — wer über einen tiefen Link
kommt, tabbt an seiner Stelle weiter und nicht am Anfang.

### Die Tastatur

| Taste | Wirkung |
|---|---|
| ↓ / ↑ | eine sichtbare Zeile weiter, an den Enden **stehen bleiben** statt umzuspringen |
| Pos1 / Ende | erste / letzte sichtbare Zeile |
| → | zugeklappt: **aufklappen**. Offen: zum ersten Kind. Auf einem Blatt: nichts |
| ← | offen: **zuklappen**. Sonst: zum Elternknoten |
| Eingabe / Leertaste | Blatt: **auswählen**. Gruppe: umklappen |

**Rechts klappt auf und springt nicht zugleich.** Zwei Tastendrücke, wie im WAI‑ARIA-Muster: Ein
Sprung in einem Zug übersprünge die Rückmeldung, dass überhaupt etwas aufgegangen ist.

**Die Regel steht als reine Funktion** (`prozessbaum.ts` `tastenbefehl`) und wird als solche geprüft
— nicht über einen gerenderten Baum. Die Komponente führt nur aus.

**Unterdrückt wird die Voreinstellung für jede Taste, die der Baum an sich zieht** — auch dort, wo
sie nichts bewirkt: `ArrowDown` auf der letzten Zeile darf die Seite nicht scrollen.

### Startzustand: alles zugeklappt

154 Partner passen nicht auf einen Bildschirm (§9); Vorklappen scheidet aus. **Der Aufklappzustand
ist abgeleitet und wird nicht nachgeführt:** Offen ist ein Knoten, wenn der Nutzer ihn umgeschaltet
hat — hat er das nicht, entscheidet der Pfad zum gewählten Prozess. Damit öffnet ein tiefer Link den
Baum an der richtigen Stelle, **ohne** dass ein Effekt Zustand nachträgt (`setState` im Effekt ist
im Projekt verboten, und ein Effekt liefe hier ohnehin erst nach dem ersten Malen).

**Ein tiefer Link zeigt die Stelle und nicht nur den offenen Ast.** `scrollIntoView` mit
`block: "nearest"` — es scrollt **nur, wenn nötig**: Wer im Baum weiterklickt, sieht seine Zeile
ohnehin, und ein Sprung bei jedem Klick wäre eine Bewegung, die niemand angefordert hat
([`visuelles-konzept.md`](visuelles-konzept.md) §7). **Kein Fokussprung** — der nähme dem Nutzer die
Stelle, an der er gerade war.

> **Und gar kein Sprung, solange ein Panel offen ist.** Baum und rechte Spalte sitzen im *einen*
> Scrollbereich; ein Sprung tief in den Baum nähme das Panel daneben mit nach oben aus dem Bild.
> Bei `NEXANS` liegt eine Zeile in der Mitte des zugeklappten Baums rund 3.400 px unten — das Panel
> stünde dann oberhalb des Sichtfensters. **Wer einen Link auf eine Nachricht öffnet, will zuerst
> den Beleg sehen**; der Baum steht offen an der richtigen Stelle und wird nur nicht angesprungen.
> Gefunden in der Gegenprüfung, §19.

**Eine Modifiertaste gehört dem Browser.** Der Handler steigt bei `Alt`, `Strg`, `Meta` und
`Umschalt` aus, **bevor** er die Voreinstellung unterdrückt. Sonst verschlucht der Baum `Alt+←` —
ausgerechnet die Taste, für die `prozess` und `nachricht` mit `history: "push"` überhaupt erst
Verlaufseinträge anlegen. Das WAI‑ARIA-Beispiel für `treeview` steigt an derselben Stelle aus.

**Weicht der Baum, geht der Fokus mit.** Unter `md` verschwindet die Baumspalte, sobald ein Prozess
gewählt ist — und mit ihr die Zeile, die gerade den Fokus trägt; er fiele sonst an `document.body`,
und der nächste Tabulator begänne wieder am Anwendungsrahmen. Der Fokus geht deshalb auf „Zurück zum
Baum", die Schaltfläche an genau der Stelle, an der der Baum eben war. **Ohne Abfrage der
Fensterbreite in JavaScript:** Die Schaltfläche trägt `md:hidden`, und `focus()` tut auf einem
`display: none`-Element nichts — ab `md` bleibt der Fokus damit von selbst dort, wo der Nutzer ihn
hatte. **Nicht beim ersten Rendern**: Ein tiefer Link ist keine Handlung des Nutzers.

### E‑45 in Zahlen — was das Überspringen tatsächlich tut

Am gerenderten Baum ausgezählt, mit den echten Daten:

| Mandant | Zeilen offen | Partnerknoten | Richtungsknoten | Blätter Ebene 2 | Blätter Ebene 3 |
|---|---:|---:|---:|---:|---:|
| `NEXANS` | 1.158 | 155 | **270** | 31 | 702 |
| `VOTG` | 523 | 133 | **0** | 390 | 0 |
| `IBIS` | 391 | 79 | **120** | 24 | 168 |
| `SUTTONS` | 18 | 1 | **0** | 17 | 0 |

**Bei `VOTG` verschwinden 133 Knoten**, die alle „nicht ermittelt" geheißen hätten — der Baum wird
von 656 auf 523 Zeilen kürzer, und keine einzige leere Richtungsebene bleibt stehen. Das ist offener
Punkt **108**, und er ist damit beantwortet.

> ### ⚠️ Die Zahlen dieses Abschnitts sind mit E‑58 überholt *(03.09.2026, §29)*
>
> Sie zählen den Zustand, in dem **jede** einzelne Richtung die Ebene verlor. Seit E‑58 verliert sie
> die Ebene nur noch, wenn sie `null` ist. **Nachgezählt am gerenderten Baum** (M130, §29):
>
> | | Zeilen offen | Richtungsknoten | Blätter auf Ebene 2 |
> |---|---:|---:|---:|
> | `NEXANS` | 1.177 *(1.158)* | **289** *(270)* | **11** *(31)* |
> | `VOTG` | 523 *(523)* | **0** *(0)* | 390 *(390)* |
> | `IBIS` | 409 *(391)* | **138** *(120)* | **4** *(24)* |
> | `SUTTONS` | 18 *(18)* | 0 *(0)* | 17 *(17)* |
>
> Der Baum wächst um **19 Zeilen bei `NEXANS`** und 18 bei `IBIS`; `VOTG` und `SUTTONS` ändern sich
> nicht. Von den 31 Blättern, die bei `NEXANS` eine Ebene weiter links standen, bleiben **elf** —
> die von `SONDERPROZESS`, dessen einzige Richtung `null` ist. **Die Ungleichförmigkeit ist damit
> nicht mehr sichtbar**, denn wo eine Zeile weiter links steht, trägt sie auch keine Richtung.

**Bei `NEXANS` erweist sich E‑45 als die kleinere Wirkung, die sie ist:** 20 von 155 Partnerknoten
verlieren die Ebene, 31 von 733 Blättern rücken herauf. **Die Ungleichförmigkeit ist damit sichtbar
und gering** — 4,2 % der Blätter stehen eine Ebene weiter links als ihre Nachbarn. *Gemessen war die
Zahl; behauptet wird, dass ein Nutzer daran keinen Anstoß nimmt — das ist eine Auslegung und keine
Messung.* **Angesehen ist sie seit dem 02.09.2026** (§21): Die beiden Sorten sind im Bild ohne Klick
zu unterscheiden — die eine Zeile trägt ein Aufklappzeichen, die andere das Richtungszeichen. **Ein
Nutzer ist dafür nicht befragt worden**; die Auslegung bleibt eine.

**`SUTTONS` ist der Fall, an dem die Regel nicht scheitern darf**: kein Katalog, 17 Prozesse, alle
ohne Partner. Der Baum zeigt **einen** Knoten „nicht zugeordnet" und darunter 17 Blätter, jedes mit
dem Zeichen für „nicht ermittelt". **Die Ansicht ist dort nicht leer.**

### Das Zeichen am Zeilenanfang — eine Stelle, drei Bedeutungen

| Zeile | Zeichen |
|---|---|
| Gruppe (Partner, Richtung) | das Aufklappzeichen |
| Blatt, dessen Richtungsebene wegfiel | **die Richtung** — `↙` eingehend, `↗` ausgehend, gestrichelter Kreis „nicht ermittelt" |
| Blatt unter einer stehenden Richtungsebene | leer — die Angabe stünde sonst zweimal übereinander |

**„Nicht ermittelt" ist ein eigenes Zeichen und nicht die Abwesenheit eines Zeichens.** §3 des
visuellen Konzepts gilt: eine leere Stelle sagt nichts. Deshalb der gestrichelte Kreis und nicht
nichts — bei `VOTG` trägt ihn jede der 390 Zeilen.

> ### ⚠️ Korrektur vom 02.09.2026 — **E‑55: an dieser Stelle steht das Wort** (§25)
>
> Die Tabelle darüber beschreibt den Zustand **bis** zum 02.09.2026. Sie bleibt stehen, damit
> erkennbar bleibt, wovon abgewichen wird. Seither gilt:
>
> | Zeile | neu |
> |---|---|
> | Gruppe (Partner, Richtung) | das Aufklappzeichen — **unverändert** |
> | Blatt ohne Richtungsebene, Richtung **bekannt** | **das Wort** („Eingehend" / „Ausgehend"), gedämpft, vor dem Prozessnamen |
> | Blatt ohne Richtungsebene, Richtung **nicht ermittelt** | **nichts** — der gestrichelte Kreis entfällt |
> | Blatt unter einer stehenden Richtungsebene | leer — **unverändert** |
>
> **Der Verzicht ist der Kern, und er ist bewusst gegen
> [`visuelles-konzept.md`](visuelles-konzept.md) §3 getroffen** („eine leere Stelle sagt nichts").
> Die Begründung: Die Angabe steht im Katalog, und ein Zeichen an *jeder* Zeile eines Mandanten
> ohne kuratierte Richtung — bei `VOTG` alle 390 — sagt dort nichts, was der Nutzer nicht schon
> weiß. Ein Zeichen, das nie fehlt, unterscheidet nichts.
>
> **Der Wortlaut kommt aus der Textquelle** (`texte.prozesse.richtung`), nicht aus der Komponente,
> und er steht in beiden Sprachen. **Die Eingrenzung greift weiterhin nicht auf ihn zu** — gefiltert
> wird über die Werte der Antwort, sonst fände dieselbe Eingabe je nach Sprache Verschiedenes
> (Regel Q4). Die Regel steht als reine Funktion `richtungswort` in `prozessbaum.ts`.
>
> **Nebenbei berichtigt:** Die Zeichenfassung ließ einen *gepflegten, aber unbekannten* vierten
> Katalogwert in denselben gestrichelten Kreis fallen wie `null` — im Bild war er damit von „nicht
> ermittelt" nicht zu unterscheiden, obwohl der `title` ihn nannte. Als Wort steht er da, wie er im
> Katalog steht.
>
> **Was es kostet, ist gemessen** (M128, §25) — und es kostet weniger als gedacht: Bei 26 rem
> brechen über alle vier Mandanten **91 von 1.332** Namen um gegen 132 vorher. Das Wort ersetzt ein
> Zeichen von 14 px samt 6 px Abstand; wo kein Wort tritt (`VOTG`, `SUTTONS`), wird die Zeile um
> 20 px breiter.

> ### ⚠️ Korrektur vom 03.09.2026 — **E‑55 hat einen Tag gehalten und ist durch E‑58 ersetzt** (§29)
>
> **Der Auftraggeber hat das Bild angesehen, und der Einwand steht in einem Satz:** Bei `ACOME`
> stand „Eingehend" als **Zeile**, beim Nachbarn `ADIENT` dasselbe Wort als **Vorsatz** in der
> Prozesszeile — zwei Schreibweisen für denselben Sachverhalt, direkt untereinander.
>
> ```
> ACOME                     20        ACOME                     20
>   > Eingehend              5          > Eingehend              5
>   > Ausgehend             15          > Ausgehend             15
> ADIENT                     0        ADIENT                     0
>   Eingehend Adient …       0          > Eingehend              0
>                                           Adient …             0
>        vorher (E‑55)                        jetzt (E‑58)
> ```
>
> **Der Vorsatz ist wieder weg, und die Richtung steht überall als Ebene** — auch dort, wo der
> Partner nur eine hat. Die Tabelle im Block darüber gilt damit nicht mehr; sie beschreibt einen
> Zustand, der einen Tag bestanden hat, und bleibt stehen, weil E‑58 nur mit ihr zu lesen ist.
>
> **Was bleibt, ist der Fall, für den E‑45 gebaut war:** Ist die Richtung `null`, fällt die Ebene
> weiter weg — ein Knoten „nicht ermittelt" über einem einzigen Kind ordnet nichts und schreibt
> nichts hin. Bei `VOTG` sind das weiterhin **null** Richtungsknoten bei 133 Partnern; offener
> Punkt 108 bleibt geschlossen. **Und dort steht dann auch kein Ersatz** — weder Zeichen noch
> Wort —, denn es gäbe nichts zu schreiben. Der Verzicht gegen
> [`visuelles-konzept.md`](visuelles-konzept.md) §3 bleibt damit bestehen und wird kleiner: Er
> betrifft nur noch die Zeilen, an denen die Angabe **fehlt**, und keine, an denen sie vorhanden
> wäre.

### Die drei Zustände in der Zeile

| Zustand | Darstellung | warum |
|---|---|---|
| **bewegt** | nichts | der unmarkierte Normalfall (E‑35) |
| **still** | eine **Marke** in gedämpfter Fläche: „seit über 3 Monaten nichts" | ein **Vorfall** — es gab eine Beziehung, und sie ist verstummt. 0 bis 28 je Mandant (M111): eine Menge, die jemand durchsieht |
| **nie** | die Zeile **gedämpft**, dazu das Wort „noch nie" in gedämpfter Schrift | eine **Katalogfrage** und kein Vorfall. Bei fünf von zehn Mandanten die größte Menge |

**Keine eigene Farbrolle.** `--ueberfaellig` gehört der Kategorie *Überfällig* und darf nicht für
einen zweiten Sachverhalt stehen; „still" nimmt deshalb die Marken-Gestalt aus
`components/marke.tsx` — gedämpfte Fläche, kleiner Radius, kein Rahmen, keine Statusfarbe.

> ### Warum „nie" trotzdem ein Wort bekommt, obwohl es „nicht markiert" sein soll
>
> **Weil eine Dämpfung allein eine Farbaussage ist.** §3 des Konzepts: *Status wird nie allein über
> Farbe ausgedrückt.* Wäre „nie" nur eine hellere Zeile, unterschiede sich ein Prozess, der **noch
> nie** etwas getragen hat, von einem, der im gewählten Fenster zufällig **null** Nachrichten hatte,
> allein durch seine Helligkeit — und beide zeigen dieselbe `0`.
>
> **Das Wort ist trotzdem keine Markierung.** Keine Marke, keine Farbrolle, kein Zeichen: nur ein
> Wort in gedämpfter Schrift. Der Unterschied zu „still" ist sichtbar und beabsichtigt.
>
> **Die Schwelle steht nicht im Code.** Sie kommt als `stilleSchwelleMonate` aus der Antwort
> (E‑37); die Oberfläche beschriftet damit und rechnet nichts nach. Eine andere Antwort ergibt einen
> anderen Text, ohne Codeänderung — `tests/prozessbaum.test.ts` hält genau das fest.

> ### ⚠️ Korrektur vom 02.09.2026 — **E‑56: „nie" wird in der Zeile nicht mehr gekennzeichnet**
>
> Der Kasten darüber bleibt stehen: Seine Logik ist richtig, **solange** „nie" überhaupt in der
> Zeile steht. Genau das ist entschieden worden — es steht nicht mehr darin. Von den drei Zuständen
> sind sichtbar **zwei**:
>
> | Zustand | neu |
> |---|---|
> | **bewegt** | nichts — unverändert |
> | **still** | die Marke „seit über 3 Monaten nichts" — **unverändert** |
> | **nie** | **nichts**: weder das Wort noch die Dämpfung |
>
> **Beides zusammen, und das ist der Kern.** Bliebe die Dämpfung ohne das Wort stehen, wäre der
> Zustand allein über Helligkeit ausgedrückt — genau der Fall, den der Kasten darüber beschreibt
> und den [`visuelles-konzept.md`](visuelles-konzept.md) §3 verbietet. Der Kasten ist also nicht
> widerlegt, sondern gegenstandslos geworden.
>
> **Der Grund ist die Sache selbst und nicht die Menge:** Der Katalog führt denselben Sachverhalt
> (E‑36). Ein Prozess, über den noch nie etwas gelaufen ist, ist ein **Katalogeintrag ohne Verkehr**
> und kein Vorfall; ein neu angelegter Partner sähe in der Zeile sonst aus wie ein Fehlerfall.
> „still" bleibt, weil es das Gegenteil ist — es gab eine Beziehung, und sie ist verstummt.
>
> **Auch im `aria-label`** (E‑52): Sichtbare und vorgelesene Fassung dürfen nicht auseinanderlaufen,
> und `tests/prozessbaum.test.ts` hält beides fest.
>
> **Was bleibt:** die Zählung in der Kopfzeile („… — 23 bewegt, 1 still, 11 noch nie"), der Schalter
> „Nur mit Verkehr im Zeitraum" samt E‑48 und das Backend vollständig — E‑35 und E‑36 gelten
> unverändert, `zustand` wird weiter geliefert.
>
> ⚠️ **Der Preis ist benannt:** Die Zahl aus der Kopfzeile ist im Baum nicht mehr **einzeln**
> auffindbar. Der Weg dorthin ist der Schalter; dass er ausreicht, ist eine Auslegung und keine
> Messung — offener Punkt **120**.

### Die Zahlen je Knoten

**Nachrichten und Fehler — nicht Überfällig** (E‑43): Die Kategorie steht nicht im Baum, und die
Zahl steht in der Übertragungsliste rechts ohnehin.

- **Die Nachrichtenzahl steht immer da, auch als `0`.** Null ist eine Aussage und kein fehlender
  Wert (§3).
- **Die Fehlerzahl nur, wenn es welche gibt**, mit Zeichen und in der Fehlerfarbe. Eine rote `0` auf
  1.158 Zeilen wäre ein Flächenteppich, und die Farbe verlöre genau das, wofür sie da ist.

Die Farbe kommt aus `lib/status-farbe.ts` (`statusVordergrund`), nicht aus der Komponente — die
Datei hat dafür eine vierte Fassung bekommen: **nur der Vordergrund**, ohne Fläche und ohne Kontur.
Eine Plakette je Zeile wäre bei dieser Menge eine Fläche und keine Auszeichnung.

### E‑52 — Ein `aria-label` je Zeile statt vieler `sr-only`-Spannen

Sichtbar trägt eine Zeile **zwei** Zahlen ohne Beschriftung: die Nachrichten und, wenn es welche
gibt, die Fehler. Vorgelesen wäre das „ACME 8.608 2" — zwei Zahlen ohne Aussage.

Stattdessen trägt jede Zeile einen zusammengesetzten Namen: *„ACME, Partner, Prozesse: 12,
Nachrichten: 8.608, Fehler: 2"*. **Er nennt eine Angabe mehr als die Zeile zeigt** — die Zahl der
Prozesse unter einem Knoten, die ein sehender Nutzer durch Aufklappen erfährt und ein hörender
sonst gar nicht. **Die Zusammensetzung ist eine Entscheidung** — welche Angabe in welcher
Reihenfolge — und steht als reine Funktion in `prozessbaum.ts`.

**Der Zahlenblock der Zeile ist dabei `aria-hidden`**, damit die Zahlen nicht ein zweites Mal
unbeschriftet danebenstehen.

> ### Warum nicht `sr-only` neben jede Zahl
>
> Weil es der naheliegende Weg ist und der teurere. Zwei Spannen je Zeile sind bei 1.158 Zeilen über
> zweitausend zusätzliche Knoten, **und jeder davon ist `position: absolute`** — also ein Kandidat
> für genau den Befund aus [`frontend-grundlagen.md`](frontend-grundlagen.md) §7, bei dem vier
> verirrte `sr-only`-Elemente 1.354 px Scrollfläche am Dokument erzeugt haben.
>
> Der zweite Grund wiegt schwerer: Ein Name, der aus dem Textinhalt zusammengelesen wird, ist die
> Reihenfolge des Markups — und die ist keine Entscheidung, die irgendwo geprüft würde. Als
> `aria-label` ist sie eine reine Funktion mit einem Test.

**„Prozesse: 12" und nicht „12 Prozesse":** Diese Anwendung kennt keine Pluralregeln
(`i18n/index.ts` `einsetzen`), und „1 Prozesse" wäre der Preis dafür.

### Die Eingrenzung

**Örtlich und nicht serverseitig.** Die Antwort liegt vollständig vor (E‑33); ein Serverparameter
brächte genau die Fallstricke mit, die den Freitextfilter der Liste teuer machen
([`prozessauswahl.md`](prozessauswahl.md) §9). **Deshalb auch keine Entprellung** — es geht keine
Anfrage hinaus, und was 0,07 ms kostet, braucht keine.

**Sie filtert Partner *und* Prozessnamen, und ein Partner bleibt stehen, dessen Kind trifft.**
Trifft der **Partner** selbst, bleiben **alle** seine Prozesse stehen: Wer nach einem Partner sucht,
will dessen Prozesse sehen und nicht die Teilmenge, deren Namen zufällig denselben Text tragen.

**Sie greift nicht auf die Richtung zu** und nicht auf die Wörter „nicht zugeordnet" oder „nicht
ermittelt": Beides sind Texte der *Oberfläche* und stünden in zwei Sprachen verschieden da.
Gefiltert wird über die Werte, die die Antwort trägt (Regel Q4).

> **Die Zahlen eines eingegrenzten Knotens sind die Summe seiner sichtbaren Blätter** — und nicht
> die Zahl, die die Antwort für den ganzen Knoten nennt. Sonst stünde über zwei Zeilen „12
> Prozesse". **Die Rechnung ist exakt und keine Näherung**: Alle drei Kennzahlen sind über die
> Blätter additiv, und der Dienst bildet sie genauso. Ohne Eingrenzung fällt sie deshalb mit den
> gelieferten Werten zusammen, und `tests/prozessbaum.test.ts` hält genau das fest — die Probe wäre
> rot, wenn das Backend seine Summen je änderte.

**Der Kopf der Baumspalte klebt** (`sticky`). Bei 154 Partnern und aufgeklappt 1.158 Zeilen wäre ein
Eingrenzungsfeld, das mit dem Baum nach oben wandert, nach zwei Bildschirmen nicht mehr erreichbar.
`sticky` braucht dafür **keinen** eigenen Scrollcontainer; es hängt sich an den des
Anwendungsrahmens, genau wie die Tabellenkopfzeilen von Katalog und Benutzerverwaltung.

### Kein eigener Scrollbereich — und was das kostet

Beide Spalten sitzen im **einen** Scrollbereich des Anwendungsrahmens
([`frontend-grundlagen.md`](frontend-grundlagen.md) §7). Kein `overflow-y-auto`, kein `h-full`, kein
`h-dvh` — ein zweiter wäre der erste Verstoß gegen genau die Regeln, die dort gemessen worden sind,
und dieselbe Festlegung wie beim Nachrichtenpanel
([`nachrichtendetail.md`](nachrichtendetail.md) §10.7).

**Die Folge ist benannt:** Ein weit aufgeklappter Baum ist mehrere Bildschirme hoch — bei `NEXANS`
54.626 px — und schiebt die Liste daneben nach oben aus dem Bild. Wer weit unten im Baum etwas
wählt, muss zurückscrollen. Der klebende Kopf mildert das für die Eingrenzung und nicht für die
Liste.

**Es gibt bewusst kein „alles aufklappen".** Damit entsteht der Zustand mit 1.158 Zeilen nur, wenn
jemand **425 Knoten** einzeln öffnet — 155 Partnerknoten und die 270 Richtungsknoten darunter.

### M120 — was der Baum kostet

**Der Browser-Teil**, gemessen in kopflosem Chrome: HTML auswerten, Stil und Layout, beste von zehn
nach einem Aufwärmlauf.

| Mandant | zugeklappt | | vollständig aufgeklappt | |
|---|---:|---:|---:|---:|
| | Zeilen / Knoten | ms | Zeilen / Knoten | ms |
| `NEXANS` | 155 / 1.092 | **9,2** | 1.158 / 7.802 | **84,7** |
| `VOTG` | 133 / 932 | 8,2 | 523 / 7.136 | 56,8 |
| `IBIS` | 79 / 554 | 5,0 | 391 / 2.729 | 30,2 |
| `SUTTONS` | 1 / 8 | 0,1 | 18 / 263 | 2,3 |

**Der Startzustand des größten Mandanten kostet 9,2 ms.** Die 84,7 ms sind der Zustand, den ein
Nutzer sich in **425 Klicks** selbst baut — es gibt kein „alles aufklappen".

**Die reinen Funktionen**, gemessen in Node, beste von 20 bis 50 Läufen:

| | `NEXANS` (733) | `VOTG` (390) | `IBIS` (192) | `SUTTONS` (17) |
|---|---:|---:|---:|---:|
| `JSON.parse` der Antwort | 0,420 ms | 0,395 | 0,260 | 0,008 |
| Eingrenzung, ohne Begriff | 0,067 | 0,029 | 0,025 | < 0,001 |
| Eingrenzung, mit Begriff | 0,066 | 0,035 | 0,027 | 0,001 |
| Eingrenzung, mit Schalter | 0,032 | 0,007 | 0,012 | < 0,001 |
| Zeilen, zugeklappt | 0,005 | 0,006 | 0,003 | < 0,001 |
| Zeilen, aufgeklappt | 0,080 | 0,041 | 0,044 | 0,001 |
| alle `aria-label` | 0,959 | 0,731 | 0,640 | 0,013 |

**Die Eingrenzung kostet je Tastendruck 0,07 ms.** Das ist die Zahl, die die Entscheidung gegen eine
Entprellung trägt.

**Kein Virtualisieren — gemessen, nicht vermutet.** Dreißig Sprünge à 1.000 px, jeder mit
erzwungenem Layout, **dasselbe Verfahren wie in [`prozessauswahl.md`](prozessauswahl.md) §7a**:

| | Knoten | Höhe | Summe über 30 Sprünge | schlechtester Einzelwert |
|---|---:|---:|---:|---:|
| `NEXANS`, alles offen | 7.802 | 54.626 px | **0,3 ms** | **0,1 ms** |
| `VOTG`, alles offen | 7.136 | 30.126 px | 0,2 ms | 0,1 ms |

Dieselben Zahlen wie dort (0,3 und 0,1) bei 1,8-mal so vielen Knoten. **Ruckelt es nicht, bleibt es
beim Einfachen.**

> ### ⚠️ Belegvermerk zu M118 und M120 (Regel L10)
>
> *Gemessen war:* die gebaute Komponente, mit den echten Daten der Testkopie, in der laufenden
> Anwendung (echte Schrift, echtes Stylesheet, echte Klassen) — Umbrüche, Zeilenhöhen,
> Gesamthöhen, Auswerte- und Layoutzeit, Scrollzeit; dazu die reinen Funktionen in Node.
>
> *Behauptet wird:* dass die Ansicht **als ganze** so schnell ist. **Das ist sie nicht belegt.** Was
> fehlt, ist die Arbeit von React selbst und der Weg über den Endpunkt — beides braucht eine
> Anmeldung an der laufenden Anwendung, und die stand für diese Runde nicht zur Verfügung.
>
> *Die Lücke ist benannt und nicht überbrückt:* Ein Versuch, die Renderzeit in `jsdom` zu messen,
> hat Zahlen geliefert, die **nicht mit der Zeilenzahl steigen** (`VOTG` mit 523 Zeilen langsamer
> als `NEXANS` mit 1.158). Sie stehen deshalb hier nicht — eine Zahl, deren Rauschen größer ist als
> ihr Gegenstand, ist keine Messung. **Die Lücke ist am 02.09.2026 geschlossen worden** (§21, M123):
> 98 ms zwischen der fertigen Antwort und dem Baum im DOM bei `NEXANS`, und die Werte steigen dort
> mit der Zeilenzahl, wie sie es sollen.

---

## 18. Die Übertragungsliste und das Panel

### Verdrahtet, nicht nachgebaut

Rechts steht **die Nachrichtenliste** — dieselbe Tabelle (`NachrichtenTabelle`), dasselbe Blättern
(`Blaettern`), derselbe Prozessfilter des Endpunkts ([`nachrichtenliste.md`](nachrichtenliste.md)
§1). **An beiden Bausteinen ist nichts geändert worden.**

Was fehlt, fehlt mit Grund: **keine Filterleiste.** Zeitraum und Prozess sind hier gesetzt, und ein
zweiter Zeitraumschalter neben dem im Kopf wäre ein zweiter Standardwert.

Was bleibt, bleibt aus demselben Grund: **Sortierumschalter und automatische Aktualisierung** sind
Teil der Tabelle beziehungsweise des Blätterblocks, und sie hier wegzunehmen hieße, dieselbe Liste
an zwei Orten verschieden zu bauen. Die Sortierung steht in der URL (sie beschreibt den Ausschnitt),
der Aktualisierungsschalter im Komponentenzustand (er beschreibt die Arbeitsweise des Betrachters) —
beides genau wie in der Nachrichtenliste.

> **Die verdeckte Liste aktualisiert nicht weiter.** Sie bleibt montiert, wenn das Panel sie
> verdeckt — das hält ihre Seitenposition —, aber ihr Intervall hängt an beidem: am Schalter **und**
> daran, ob sie im Bild ist. `useNachrichtenSeite` prüft von sich aus nur `document.hidden`, also
> die Registerkarte. Ohne diese Angabe ginge alle sechzig Sekunden eine Abfrage für eine Liste
> hinaus, die niemand sieht — **und abschalten könnte der Nutzer sie nicht, denn der Schalter steckt
> in demselben verdeckten Bereich.** In der Nachrichtenliste tritt das nicht auf: Dort weicht die
> Liste erst unter `xl` und bleibt darüber bedienbar. Gefunden in der Gegenprüfung, §19.

> ### ⚠️ Korrektur vom 02.09.2026 — **die Sonderregel ist mit E‑57 entfallen** (§27)
>
> Der Kasten beschreibt, was bis dahin galt, und er bleibt stehen: Ohne ihn wäre nicht mehr zu
> sehen, wogegen die Angabe einmal gebaut war. **Mit E‑57 weicht der Baum und nicht die Liste** —
> ab `xl` steht sie neben dem Panel und bleibt bedienbar. Der Grund für die Sonderregel ist damit
> weg, und eine Bedingung stehen zu lassen, deren Grund entfallen ist, wäre schlechter als beides:
> Sie sähe aus wie eine Regel und wäre keine. `useNachrichtenSeite` bekommt seither nur noch den
> Schalter, genau wie in der Nachrichtenliste.
>
> ⚠️ **Was zurückbleibt, und es ist nicht nichts:** **Unter `xl` weicht die Liste weiterhin**, und
> dort kehrt der alte Fall zurück — verdeckt, aktualisierend, und der Schalter dazu im verdeckten
> Bereich. Er ist damit **derselbe**, den die Nachrichtenliste seit Schritt 5 trägt: Auch dort steht
> `useNachrichtenSeite` ohne diese Angabe, und auch dort weicht die Liste unter `xl`. Der Satz oben
> („In der Nachrichtenliste tritt das nicht auf") war für **ab** `xl` richtig und für darunter zu
> großzügig. Ihn hier allein zu behandeln hieße wieder, dieselbe Liste an zwei Orten verschieden zu
> bauen; ihn über die Fensterbreite zu behandeln hieße, einen zweiten Umbruchpunkt in JavaScript zu
> führen. Geführt als offener Punkt **121**.

**Vor der Wahl eines Prozesses steht rechts ein Leerzustand** und nicht die ganze Liste des
Mandanten. Dafür gibt es die Nachrichtenliste, und dorthin führt ein Verweis. **Solange kein Prozess
gewählt ist, läuft keine Abfrage** — die Liste ist eine eigene Komponente, damit das nicht von einem
`enabled`-Schalter abhängt: Hooks laufen nicht bedingt, also läuft die Komponente bedingt.

### E‑50 — Das Fenster kommt aus der Antwort des Baums

Die Liste kennt die drei Rollup-Paare nicht; ihre relativen Zeiträume heißen `24h`, `7d`, `30d`
(`lib/filter.ts`), und **keines der sechs fällt mit einem der anderen zusammen**. Es bleibt der
zweite Modus: `von`/`bis`.

**Und das ist nicht der Notausgang, sondern die richtige Antwort.** Beide Seiten lesen dieselbe
Spalte: Der Rollup gruppiert nach `MessageLastUpdate`, und die Liste filtert über dieselbe
(`NachrichtenRepository`, Zeile 171/172). Mit demselben Fenster zeigen Baum und Liste **denselben
Ausschnitt**; mit einem eigenen Zeitraum stünden links und rechts zwei verschiedene Zahlen, und
keine wäre falsch.

**Gerechnet wird im Browser nichts.** Das Fenster kommt aus der Antwort, die es gegen die
*Anwendungsuhr* aufgelöst hat (Regel Z1) — im Browser gerechnet wäre es gegen die Browseruhr
gerechnet, und die Testkopie liegt Monate hinter der realen Uhrzeit.

> **Die eine benannte Ungenauigkeit.** Die obere Grenze des Baums ist **ausschließend** und liegt auf
> einer Eimergrenze; die Liste vergleicht mit `<=`. Eine Nachricht, die exakt auf `bis` liegt,
> erschiene links nicht und rechts schon. Das wird **nicht ausgeglichen**: Eine Sekunde abzuziehen
> wäre eine Rechnung in der Oberfläche, und die ist teurer als die Ungenauigkeit.

**Solange der Baum kein Fenster genannt hat, läuft keine Listenabfrage.** Die Liste hängt damit am
Baum — bei einem tiefen Link auf `?prozess=…` lädt erst der Baum, dann die Liste. Das kostet einen
Umlauf und ist der Preis dafür, dass beide dasselbe Fenster meinen.

### E‑53 — Beim Öffnen des Panels weicht die Liste, der Baum bleibt

> ### ⚠️ Diese Entscheidung hieß im Auftrag **E‑44**, und die Nummer war vergeben
>
> Der Auftrag zu 10c‑2 führt sie als `E‑44` und weist zugleich an, „die höchste
> [Nummer] zu ermitteln". **Beides zusammen geht nicht auf:** `E‑44` steht seit
> 10c‑1 in §10 dieser Datei für den Umzug von `Rollupzeitraum` und
> `Katalogzuordnung` nach `common` — vergeben am selben Tag, wenige Stunden
> vorher, und aus [`dashboard-frontend.md`](dashboard-frontend.md) sowie
> [`frontend-grundlagen.md`](frontend-grundlagen.md) verlinkt.
>
> Die Panel-Entscheidung bekommt deshalb **E‑53**, die nächste freie Nummer nach
> E‑52. Der Auftrag hat die Kollision nicht sehen können: Er ist am 02.09.2026
> geschrieben worden, als 10c‑1 noch nicht abgeschlossen war. **Dieselbe Lage wie
> bei `E‑v` im Dichteumschalter** ([`dichte-umschalter.md`](dichte-umschalter.md)
> §2), und dieselbe Auflösung — die Reihe zählt weiter, der Vorschlag des
> Auftrags wird korrigiert und nicht übernommen.

**Drei Spalten sind bei 1280 px unmöglich**, und das ist jetzt beziffert: Baum (26 rem = 416 px) und
Panel (26 rem = 416 px) sind zusammen 832 px von rund **1.016 px nutzbarer** Inhaltsbreite.

> **1.016 px und nicht 1.072 px, und beides ist richtig.**
> [`nachrichtendetail.md`](nachrichtendetail.md) §10.7 rechnet mit 1.072 px — das ist `main`
> **einschließlich** seines Innenabstands (1280 − 208 px Navigationsspalte). Hier steht, was dem
> Inhalt davon bleibt: abzüglich 2 × 20 px Innenabstand und rund 15 px Bildlaufleiste. Dieselbe
> Kiste, zwei Maße; wer sie gleichsetzt, findet einen Widerspruch, den es nicht gibt.

**Der Baum ist der Kontext, den der Nutzer behalten will** — er hat gerade dort ausgewählt, und die
Liste dazwischen ist der Weg und nicht das Ziel.

**Die Liste weicht, wird aber nicht ausgehängt** (`display: none`): Ihre Seitenposition bleibt
stehen, und beim Schließen geht keine zweite Abfrage hinaus. `Escape` schließt das Panel und bringt
die Liste zurück — über denselben Hook wie überall (`useEscapeSchliesst`), mit denselben zwei
Ausnahmen.

**Das Panel ist der vierte Einhängepunkt derselben Komponente** und bekommt genau vier Angaben:
Kennung, Schließen, Beschriftung, Öffnen. **Ohne Ansichtsumschalter** — sein Rückweg führt an die
Nachrichtenliste (`lib/routen.ts` `ansichtNebenListe`) und damit woandershin, als er herkam; genau
die Überlegung, mit der die Belegsuche ihn seit dem 13.08.2026 weglässt
([`nachrichtendetail.md`](nachrichtendetail.md) §10.7). Die beiden Angaben sind seither freiwillig,
und ohne sie erscheint der Knopf nicht. **Dafür war nichts zu bauen.**

> ### ⚠️ Korrektur vom 02.09.2026 — **E‑57 dreht E‑53 um: es weicht der Baum** (§27)
>
> Alles oben bleibt gültig bis auf **welche** der drei Spalten weicht. Die Rechnung stimmt weiter:
> Baum und Panel sind bei 1280 px zusammen 832 px von rund 1.016 px, drei Spalten gehen dort nicht.
> Der Satz, der nicht mehr gilt, ist der tragende — *„Der Baum ist der Kontext, den der Nutzer
> behalten will."*
>
> **Wer mehrere Nachrichten desselben Prozesses durchsieht, braucht die Liste und nicht den Baum.**
> Der Baum wird einmal am Anfang benutzt; die Liste ist der Ort, an dem weitergeklickt wird. Der
> Kontext geht dabei nicht verloren, er wechselt die Form: Die Überschrift der rechten Spalte trägt
> den Prozessnamen ohnehin.
>
> **Die Bauform bleibt, sie wechselt nur die Spalte:** ausgeblendet und nicht ausgehängt
> (`display: none`), der Aufklappzustand des Baums bleibt, beim Schließen geht keine zweite Abfrage
> hinaus, `Escape` schließt weiter über `useEscapeSchliesst`. Belegt in M129 (§27).
>
> **Zwei Dinge hängen daran und stehen an ihrer Stelle:** die Sonderregel der verdeckten Liste (der
> Kasten weiter oben, entfallen) und der Fokus, wenn der Baum unter ihm weggeblendet wird (§27, die
> zweite Hälfte der Regel aus §15).

### M119 — ⚠️ Der Befund: Bei 1280 px steht die Liste nicht vollständig

**Die Tabelle der Nachrichtenliste braucht 744 px.** Gemessen an der gebauten Tabelle, in kopflosem
Chrome, bei 1280 px und bei 1920 px Fensterbreite — **mit identischem Ergebnis**:

| Spaltenbreite | Zeitpunkt | Status | **Ablauf** | Projekt | Tabelle |
|---:|---:|---:|---:|---:|---:|
| 500 px | 184 | 272 | **0** | 288 | 744 |
| 568 px | 184 | 272 | **0** | 288 | 744 |
| 700 px | 184 | 272 | **0** | 288 | 744 |
| 750 px | 184 | 272 | 4 | 288 | 748 |
| 800 px | 184 | 272 | 54 | 288 | 798 |
| 1.000 px | 184 | 272 | 254 | 288 | 998 |

**`table-fixed` schrumpft die festen Spalten nicht — es nimmt der einzigen freien Spalte alles.**
Zeitpunkt (11,5 rem), Status (17 rem ab `lg`) und Projekt (18 rem) stehen fest; **Ablauf** hat keine
Breitenangabe und bekommt den Rest. Ist kein Rest da, ist die Spalte **null Pixel breit**, und die
Tabelle scrollt waagerecht in dem Container, den `components/ui/table.tsx` selbst mitbringt.

**Was das für die Prozessansicht heißt:** Bei 1280 px bleiben der Liste neben dem Baum rund 584 px.
Die Ablaufspalte — **der Name, an dem man eine Zeile erkennt** — ist dann nicht sichtbar, ohne
waagerecht zu scrollen.

**Und es liegt nicht an der Breite des Baums.** Damit die Liste 744 px bekäme, dürfte er höchstens
**16 rem** breit sein — bei dieser Breite bricht mehr als die Hälfte aller Prozessnamen um (M118).
**Bei 1280 px gibt es entweder einen brauchbaren Baum oder eine vollständige Liste, nicht beides.**

> **Das ist ein Befund und kein Anlass für eine zweite Fassung der Liste.** Der Auftrag sagt das
> selbst: *„Passt einer davon nicht, ist das ein Befund."* Die Tabelle so umzubauen, dass sie neben
> einem Baum anders aussieht als allein, hieße dieselbe Liste zweimal zu bauen — und die feste
> Zeilenhöhe samt fester Spaltenbreiten ist eine gemessene Entscheidung aus Schritt 4
> ([`nachrichtenliste.md`](nachrichtenliste.md) §8.1). Geführt als offener Punkt **114**.
>
> **Ab welcher Fensterbreite sie vollständig steht, ist gerechnet und nicht gemessen:** 416 px Baum
> + 16 px Abstand + 744 px Liste = 1.176 px Inhalt; dazu 40 px Innenabstand, 209 px Navigationsspalte
> samt Trennlinie und rund 15 px Bildlaufleiste ergeben rund **1.440 px**. ~~Am laufenden System
> nachzumessen.~~ **Nachgemessen** (§21, M124): Bei 1.440 px trägt der Kasten die Tabelle ohne
> Querlauf, bei 1.280 px nicht.

---

---

## 19. Tests der Oberfläche

**Geprüft werden die Entscheidungen, nicht das Markup**
([`frontend-grundlagen.md`](frontend-grundlagen.md) §9). Der Regelfall ist eine reine
`*.test.ts`-Datei ohne DOM; die Ausnahme ist begründungspflichtig und wird an einer Stelle gezählt.

| Datei | Was sie hält |
|---|---|
| `tests/prozessbaum.test.ts` | **46 Fälle, alle ohne DOM.** E‑45 in beiden Richtungen: bei zwei Richtungen bleibt die Ebene und die Blätter stehen auf Ebene 3 **ohne** Richtung in der Zeile; bei einer fällt sie weg, die Blätter rücken auf Ebene 2 und **tragen die Richtung**. `null` als Richtung ist **gesetzt und nicht abwesend**. Position und Geschwisterzahl je Ebene und nicht über die flache Liste. Der Pfad zum gewählten Prozess — mit Richtung, ohne Richtung, und **leer** bei einer fremden Kennung. Die Eingrenzung: Partnertreffer behält alle Kinder, Kindtreffer behält den Partner mit nur diesem Kind, kein leerer Ast bleibt stehen, ein `null`-Name trifft nie, die **Richtung** wird nicht durchsucht. **Die Invariante**: Ohne Eingrenzung fallen die nachgerechneten Summen mit den gelieferten zusammen. Die Beschriftung: drei Richtungsfälle samt unbekanntem Wert, die Schwelle **aus der Antwort** (3 und 6 Monate ergeben verschiedene Texte), kein Zusatz bei „bewegt", keine Fehlerzahl ohne Fehler. Und das ganze WAI‑ARIA-Muster der Tastatur, einschließlich des Elternknotens **über eine weggefallene Ebene hinweg**. Dazu `ohnePfad`: dass ein Prozesswechsel genau die Umschaltungen seines Pfades vergisst und sonst keine — und dieselbe Menge zurückgibt, wenn nichts zu räumen ist |
| `tests/prozessansicht.test.ts` | **19 Fälle.** Rundlauf URL → Zustand → URL; leer, solange nichts gewählt ist; feste Reihenfolge; der Schalter steht nur in der URL, wenn er etwas weglässt; unbekannter Zeitraum und unbekannte Sortierung werden **übergangen**; eine leere Kennung ist keine Auswahl; ein unbekannter Parameter wird übergangen. Dazu der Listenfilter: **kein Filter ohne Prozess und keiner ohne Fenster**, das Fenster kommt aus der Antwort, genau ein Prozess, die Sortierung wird durchgereicht — und **die Abfrage ist mit und ohne geöffnetes Panel Zeichen für Zeichen dieselbe**, ohne Cursor, ohne Kennung, ohne `ueberfaellig`. Dazu zwei Proben an der Naht: dass `nachricht` in Parser, Zustand und `lib/routen.ts` **denselben** Parameter meint (der Parser steht unter einem berechneten Schlüssel, an dem der Übersetzer nichts merkt), und dass eine **leere Kennung schon im Parser** wegfällt und nicht erst in `ausSuchparametern` — sonst prüfte der Test eine Regel, die die Anwendung nicht anwendet |
| `tests/prozess-baum.test.tsx` | **10 Fälle, gerenderter Baum — begründete Ausnahme.** Vier Klassen, und alle vier stehen nur im Baum: der **roving `tabindex`** („genau einer, und er liegt auf der gewählten Zeile"), die **ARIA-Ausgabe** der flachen Form (`aria-level` 1/2/3/2/3, `aria-expanded` nur an Gruppen, `aria-selected` an jeder Zeile), und zweimal **Abwesenheit** — bei einer Richtung entsteht **keine** zweite Ebene, und es gibt **keine `sr-only`-Spanne je Zahl**. Dazu die Klassen, die selbst die Regel *sind*: `min-h-beruehrung` an jeder Zeile, `relative` und **kein** `overflow-y` am Baum. Und viertens die **Verdrahtung der Tastatur**: dass ein Pfeil Fokus und roving `tabindex` wirklich weitersetzt — und dass eine **Modifiertaste durchgelassen** wird (`Alt+←` ist das Zurück des Browsers; `tastenbefehl` sieht Modifier gar nicht) |
| `tests/dashboard-bloecke.test.tsx` | **angepasst**: Die drei Rollup-Paare stehen seit dieser Runde auf oberster Ebene der Sprachdateien (E‑47); der Test liest sie von dort |
| `tests/sprachdateien.test.ts` | unverändert — beide Sprachdateien tragen den neuen Abschnitt `prozesse` vollständig, und der verschobene Block `zeitraum` steht in beiden an derselben Stelle |
| `tests/farbwerte.test.ts` | unverändert und **ohne neue Ausnahme**. Die Fehlerfarbe der Zahl kommt über `statusVordergrund` aus `lib/status-farbe.ts`; in den neuen Komponenten steht kein Farbwert und keine Farbklasse |
| `tests/serverbausteine.test.ts` | unverändert. `app/(app)/prozesse/page.tsx` bleibt Server-Komponente und importiert nur die Ansicht, die `"use client"` trägt |

**Die Zählung der gerenderten Fälle wird an genau einer Stelle geführt** — im Kopf von
`frontend/vitest.config.mts`. Sie steht dort jetzt bei **neunundsechzig in elf Dateien**; bei der
Gelegenheit ist ein Widerspruch berichtigt worden, den dieselbe Datei mit sich selbst hatte (die
Tabellensumme nannte neunundfünfzig, der Schlusssatz neunundvierzig).

### ⚠️ Vier Befunde aus der Gegenprüfung — und was sie geändert haben

Die Runde ist nach dem Bau gegen die Abnahmeliste, die Projektregeln und auf Fehler gegengelesen
worden: vier Blickwinkel parallel, danach je eine Gegenprobe mit dem Auftrag, den Befund zu
**widerlegen**. Von 37 Befunden haben 23 standgehalten. **Vier davon waren Fehler im Verhalten**,
und sie stehen hier, weil jeder von ihnen an einer Naht saß, die keine reine Funktion abdeckt.

#### 1. Der tiefe Link schob das Panel aus dem Bild

**Der Fall:** `/prozesse?prozess=…&nachricht=…`, ein Prozess in der Mitte des Baums. Der Effekt, der
die gewählte Zeile ins Bild holt, scrollte den **einen** Scrollbereich des Anwendungsrahmens — und
damit die rechte Spalte mit. Bei `NEXANS` liegt die Zielzeile mehrere tausend Pixel unten; das
Panel stand danach oberhalb des Sichtfensters. **Von den drei Dingen, die der Auftrag verlangt —
Baum, Panel, richtige Stelle —, waren nur zwei zugleich zu haben.**

**Die Änderung:** Der Baum springt nur, **wenn kein Panel offen ist** (`springeZurAuswahl`). Wer
einen Link auf eine *Nachricht* öffnet, will zuerst den Beleg sehen; der Baum ist der Kontext, den
er danach sucht — und er steht offen an der richtigen Stelle, nur eben nicht angesprungen.

**Warum das kein Test gefunden hat:** Der Effekt ruft `scrollIntoView`, und `jsdom` rechnet kein
Layout. ~~Der Fall bleibt am laufenden System nachzusehen.~~ **Nachgesehen am 02.09.2026** (§21,
M125): Mit `&nachricht=` bleibt `main.scrollTop` auf **0**, das Panel steht im Bild — die Änderung
hält. **Der Gegenfall ist dabei aufgefallen und ist der schwerere:** *ohne* `&nachricht=` springt der
Baum wie vorgesehen, und damit verlässt die Liste das Bild (offener Punkt **118**).

#### 2. Der Baum verschluckte `Alt+←` — das Zurück des Browsers

**Der Fall:** `beiTaste` sah nur `ereignis.key` an. `Alt+←` traf damit auf `ArrowLeft`, wurde mit
`preventDefault` abgefangen und klappte einen Knoten zu, statt zurückzunavigieren. **Ausgerechnet
die Taste, für die `prozess` und `nachricht` mit `history: "push"` überhaupt erst Verlaufseinträge
anlegen.** Dasselbe für `Strg+Pos1` und `Strg+Ende`.

**Die Änderung:** Der Handler steigt bei jeder Modifiertaste aus, bevor er etwas unterdrückt — so
wie das WAI‑ARIA-Beispiel für `treeview`.

**Warum das kein Test gefunden hat:** `tastenbefehl` ist eine reine Funktion und **sieht Modifier
gar nicht**; die Verdrahtung darum war ungeprüft. `tests/prozess-baum.test.tsx` hat dafür zwei
Fälle bekommen — einen für `Alt+←`, einen für den Pfeil ohne Modifier als Gegenprobe.

#### 3. `?prozess=` — die Schutzregel lief nur im Test

**Der Fall:** Die Regel „eine leere Kennung ist keine Auswahl" stand in `ausSuchparametern` — also
in der Funktion, die der **Test** ruft, und nicht in der, die die **Anwendung** ruft. Zur Laufzeit
liest `nuqs` über `parseAsString`, und der hält einen leeren String **nicht für abwesend**:
`/prozesse?prozess=` ergab `""`. Folge: Der Baum wich unter `md`, und der Listenfilter bekam
`prozess: [""]`. **Das Backend wirft leere Werte weg, und ein leerer Prozessfilter heißt dort
„alle"** — rechts stand unter der Überschrift eines Prozesses der gesamte Verkehr des Mandanten.

**Die Änderung:** Die Regel ist in einen eigenen Parser gewandert (`parseAsKennung`), und
`ausSuchparametern` benutzt denselben. **Test und Laufzeit prüfen seither dieselbe Regel.**

> **Das ist die allgemeinere Lehre dieser Runde**, und sie gilt über diese Ansicht hinaus: Eine
> Regel, die nur in der Funktion steht, die der Test ruft, ist keine Regel der Anwendung. Der
> Zweischritt aus [`frontend-grundlagen.md`](frontend-grundlagen.md) §8 fragt, *ob* ein Zustand in
> die URL gehört — er fragt nicht, *wer* ihn liest.

#### 4. Die verdeckte Liste fragte weiter ab, und ihr Schalter war unerreichbar

**Der Fall:** Die Übertragungsliste bleibt montiert, wenn das Panel sie verdeckt (E‑53) — das hält
ihre Seitenposition. `useNachrichtenSeite` prüft für sein Intervall aber `document.hidden`, also die
**Registerkarte**, nicht die Sichtbarkeit des Elements. Wer die automatische Aktualisierung
einschaltete und dann eine Nachricht öffnete, schickte alle sechzig Sekunden eine Abfrage auf die
Produktionsdatenbank für eine Liste, die niemand sieht — **und konnte sie nicht abschalten, weil der
Schalter in demselben ausgeblendeten Bereich steckt.**

**Die Änderung:** Die Liste bekommt mit, ob sie im Bild ist, und das Intervall hängt an beidem.

> **In der Nachrichtenliste tritt das nicht auf**, und der Unterschied ist genau E‑53: Dort weicht
> die Liste dem Panel erst unter `xl` und bleibt darüber sichtbar und bedienbar. Hier weicht sie auf
> **jeder** Breite.

#### Was die Gegenprobe verworfen hat, und warum das dazugehört

Vierzehn Befunde haben nicht standgehalten — darunter drei, die aus einer richtigen Beobachtung
einen falschen Schluss zogen: dass `132` und `133` bei `VOTG` ein Selbstwiderspruch seien (es sind
Partner und Knoten), dass `1.016 px` und `1.072 px` dieselbe Größe meinten (es sind `main` mit und
ohne Innenabstand), und dass der Tabellenkasten ein `relative` brauche (`components/ui/table.tsx`
bringt seinen eigenen mit). **Die drei Stellen sind trotzdem angefasst worden** — nicht um einen
Fehler zu beheben, sondern um den Unterschied hinzuschreiben, über den zwei Leser gestolpert sind.

### Was **nicht** geprüft ist, und warum es nicht geprüft werden kann

- **Das Verhalten an den Umbruchpunkten.** `md` und `xl` sind Klassen; ein gerenderter Baum in
  `jsdom` rechnet kein Layout. Prüfbar ist das nur am laufenden System — **nachgeholt am
  02.09.2026**, §21 (M124), einschließlich des Fokus auf „Zurück zum Baum" unter `md`.
- **Der Zusammenbau** (`prozessansicht.tsx`) hat keinen eigenen Test. Seine Entscheidungen stehen als
  reine Funktionen daneben und sind dort geprüft; was übrig bleibt, ist Verdrahtung.

## 20. Was diese Runde nicht zeigt

1. ~~**Keine Sichtprüfung im Browser.**~~ ✔ **Nachgeholt am selben Tag — §21.** Beim Schreiben
   dieses Punktes stand keine Anmeldung zur Verfügung: Das Bootstrap-Konto wies die Zugangsdaten aus
   der Umgebung mit `anmeldung-abgelehnt` ab, und die Browsererweiterung war nicht verbunden. Später
   stand die laufende Sitzung des Auftraggebers zur Verfügung, und damit ist die Liste aus Punkt
   **115** abgearbeitet. **Zwei Befunde sind dabei neu** (offene Punkte **118** und **119**), einer
   hat den vorhandenen Punkt **114** verschärft — alle drei betreffen die Spalte **neben** dem Baum
   und keinen der hier getroffenen Bauentscheidungen.

   > **Was sich auch ohne Anmeldung belegen ließ, und es ist mehr als nichts.** Die Route liefert
   > `200` und rendert serverseitig fehlerfrei; ruft man sie im Browser mit einem **erfundenen**
   > Sitzungscookie auf — das kommt an `src/proxy.ts` vorbei, der nur prüft, *ob* eines da ist —,
   > hängt der Komponentenbaum ein, und die Konsole bleibt **leer**: keine Meldung, keine Warnung,
   > kein Wurf. Danach beantwortet das Backend die Baumabfrage mit `401`, und die Anwendung leitet
   > auf `/anmeldung?weiter=%2Fprozesse` um — genau, was `lib/query-client.ts` zusagt.
   >
   > **Damit ist die Falle aus [`frontend-grundlagen.md`](frontend-grundlagen.md) §8 ausgeschlossen**
   > — ein Baustein ohne `"use client"`, der schon beim Importieren wirft, und den `pnpm check` nicht
   > findet. Belegt ist das Einhängen; **nicht** belegt ist alles, was Daten braucht.
2. ~~**Keine Messung der Renderzeit der ganzen Ansicht.**~~ ✔ **Nachgeholt** (§21, M123): 98 ms von
   der fertigen Antwort bis zum Baum im DOM bei `NEXANS`, 454 ms für alle 425 Gruppen. Die Zahlen
   stammen aus dem Entwicklungsbetrieb und sind damit **obere** Schranken, kein Abnahmewert.
3. **Keine Virtualisierung.** Gemessen, dass sie nicht nötig ist — nicht angenommen (§16).
4. **Keine Überfälligkeit im Baum.** E‑43 gilt unverändert; die Zahl steht in der Liste rechts.
5. ~~**Keine Gegenprüfung im Browser der Befunde aus §19.**~~ ✔ **Nachgeholt** (§21). Die zwei
   Regeln, die nur am laufenden System zu *sehen* sind, halten: Der Baum springt bei geöffnetem
   Panel nicht (`main.scrollTop` bleibt 0), und der Fokus geht unter `md` auf „Zurück zum Baum".
   **Der dritte Befund — das verschluckte `Alt+←` — ist ebenfalls behoben und beinahe als Rückfall
   missdeutet worden**: Im kopflosen Chrome bewegt sich der Verlauf bei `Alt+←` nicht, weil die
   Browseroberfläche fehlt, die die Taste übersetzt. Die Anwendung unterdrückt das Ereignis
   nachweislich nicht.
6. **Kein zweiter Weg in die Ansicht.** Weder das Dashboard noch die Nachrichtenliste verweisen
   hierher. Ob die Verteilung des Dashboards („nach Partner") auf den Partnerknoten des Baums zeigen
   sollte, ist eine eigene Entscheidung — heute sagt sie ausdrücklich, dass ihre Zeilen nicht
   klicken.

---

## 21. Die Sichtprüfung am laufenden System (02.09.2026, nachgeholt)

*Dieser Abschnitt entsteht **nach** §20 und berichtigt dessen ersten Punkt.* Beim Schreiben von §20
stand keine Anmeldung zur Verfügung; sie stand später am selben Tag zur Verfügung, und damit ist
alles nachgeholt, was offener Punkt **115** einzeln aufgezählt hatte. **Zwei Befunde sind dabei neu**
— beide betreffen nicht den Baum, sondern die Spalte daneben.

**Wie gemessen wurde.** Kopfloses Chrome über das DevTools-Protokoll gegen die lokal laufende
Anwendung (`localhost:3000`, Backend `localhost:8080`, Profil `dev`, Testkopie mit 3,34 Millionen
Zeilen in `Message`), mit der **laufenden Sitzung** des Auftraggebers — kein zweites Konto, keine
erfundene Sitzung. Fenstermaße über `Emulation.setDeviceMetricsOverride`, Klicks und Tasten über
`Input.dispatchMouseEvent` und `Input.dispatchKeyEvent`, also **echte Eingabeereignisse** und keine
zugewiesenen Werte (dieselbe Regel wie in [`nachrichtenliste.md`](nachrichtenliste.md) §8.2). Die
Anwendungsuhr steht im Profil `dev` auf dem 30.12.2025; die Zeitfenster in den Bildern sind deshalb
Dezember 2025 und kein Fehler.

**In keinem einzigen Lauf, über alle vier Mandanten und alle Breiten, ist eine Konsolenmeldung
angefallen** — keine Warnung, keine Fehlermeldung, kein Wurf.

### M121 — Sichtbare Baumzeilen bei 1080 px, je Dichtestufe

Gezählt sind die Zeilen, die **ganz** zwischen der Unterkante des klebenden Baumkopfes und der
Unterkante des Scrollbereichs liegen — dasselbe Verfahren wie in
[`dichte-umschalter.md`](dichte-umschalter.md) §5.3 für die Nachrichtenliste.

| Stufe | Wurzelschrift | Zeilenhöhe | Seitenkopf + Baumkopf | **sichtbare Zeilen** | Baumspalte |
|---|---:|---:|---:|---:|---:|
| `xs` | 14 px | 44 px | 213 px | **19** | 364 px |
| `s` | 15 px | 44 px | 225 px | **19** | 390 px |
| `m` | 16 px | 44 px | 237 px | **18** | 416 px |
| `l` | 18 px | 50 px | 267 px | **16** | 468 px |

**Die Vergleichszahl aus der Nachrichtenliste ist 28/26/24/19 — der Baum liefert 19/19/18/16.** Der
Unterschied ist keine Überraschung, sondern E‑51 in Zahlen: Die Baumzeile hält
`--dichte-beruehrung` (44 px in `xs`, `s` und `m`, 49,5 px in `l`) und nicht `--dichte-zeile`.

**Zwei Folgerungen, und die zweite ist unangenehm.** Erstens: `xs` und `s` zeigen **gleich viele**
Zeilen — die kleinste Stufe kauft dem Nutzer nichts. Zweitens: Über die ganze Skala bewegt der
Umschalter **drei Zeilen**; in der Liste sind es neun. Wer die Dichte stellt, um mehr zu sehen,
bekommt im Baum fast nichts dafür. Das ist die gemessene Fassung von offenem Punkt **117**, und die
Entscheidung dort ist damit nicht leichter, aber sie steht auf Zahlen.

### M122 — Berührungsziele am Berührungsgerät

1024 × 1366, Berührungsemulation aktiv, `matchMedia("(pointer: coarse)")` meldet `true` — dasselbe
Verfahren wie [`dichte-umschalter.md`](dichte-umschalter.md) §5.4.

| Element | `xs` | `s` | `m` | `l` |
|---|---:|---:|---:|---:|
| Baumzeile | 44 | 44 | 44 | 50 |
| Eingrenzungsfeld | 44 | 44 | 44 | 50 |
| **Beschriftung** des Schalters | 44 | 44 | 44 | 50 |
| Zeitraumknopf | 44 | 44 | 44 | 50 |
| Navigationseintrag | 44 | 44 | 44 | 50 |
| der `Switch` selbst | 18 | 18 | 18 | 18 |
| … mit seiner `::after`-Vergrößerung | 32 | 33 | 34 | 36 |

**Alles, was diese Ansicht selbst baut, hält die Fläche.** Der `Switch` hält sie nicht — er ist der
Baustein aus dem Generatorbereich mit fester Pixelhöhe und steht seit dem 31.08.2026 als offener
Punkt **96** in [`dichte-umschalter.md`](dichte-umschalter.md). Die Ansicht hat ihn nicht
verschlechtert und repariert ihn auch nicht; sie legt die Berührungsfläche in die **Beschriftung**.

**Und die trägt wirklich** — nachgefahren, nicht behauptet: ein echter Klick auf die Mitte der
Beschriftung (44 px hoch) schaltet den Schalter von `unchecked` auf `checked`, die Adresse bekommt
`?nurMitVerkehr=true`, und der Baum geht von 155 auf 25 Zeilen.

### M123 — Die vier Mandanten, am laufenden System

| | `NEXANS` | `IBIS` | `VOTG` | `SUTTONS` |
|---|---:|---:|---:|---:|
| Antwort **über die Leitung** | 14,5 KiB | 3,9 KiB | 7,1 KiB | 0,6 KiB |
| Antwortzeit des Endpunkts | 60 ms | 42 ms | 43 ms | 33 ms |
| **React: fertige Antwort → Baum im DOM** | **98 ms** | 62 ms | 88 ms | 12 ms |
| Zeilen zugeklappt | 155 | 79 | 133 | 1 |
| Zeilen vollständig aufgeklappt | 1.158 | 391 | 523 | 18 |
| Partnerknoten | 155 | 79 | 133 | 1 |
| **Richtungsknoten** | 270 | 120 | **0** | **0** |
| Blätter | 733 | 192 | 390 | 17 |
| Partner **mit** Richtungsebene | 135 | 60 | **0** | 0 |
| Partner **ohne** (E‑45) | 20 | 19 | **133** | 1 |
| alle Gruppen aufklappen | 454 ms | 227 ms | 238 ms | 43 ms |
| Höhe des offenen Baums | 54.626 px | 18.684 px | 30.126 px | 792 px |

**Das schließt die offene Hälfte von Punkt 109.** Die 150,3 KiB des Rumpfes sind **über die Leitung
14,5 KiB** — der Nutzer wartet auf die komprimierte Zahl. Die Arbeit von React zwischen der fertigen
Antwort und dem Baum im Bild ist **98 ms** beim größten Mandanten; M120 hatte für den Browser-Teil
allein 9,2 ms gemessen, React ist also die größere Hälfte. Alle 425 Gruppen des größten Mandanten
aufzuklappen kostet **454 ms** — der Zustand, für den M120 84,7 ms Auswerten und Layouten gemessen
hatte und für den es weiterhin **kein „alles aufklappen"** gibt.

**Die Zeilenzahlen decken sich mit den Vorhersagen aus §9 und §17, Zeile für Zeile** — 1.158 bei
`NEXANS`, 391 bei `IBIS`, 523 bei `VOTG`, 18 bei `SUTTONS`. Ein Unterschied ist erklärungsbedürftig
und keine Abweichung: §9 zählt **154** Partner, der Baum zeigt **155** Zeilen der Ebene 1. Die
155. ist „nicht zugeordnet" — sie ist kein Partner des Katalogs, steht aber als Knoten da, und sie
trägt bei `NEXANS` selbst Richtungsebenen. Deshalb heißt es dort 134 von 154, hier 135 von 155; es
sind dieselben 20 Partner ohne Ebene.

### E‑45, am Bild geprüft — und sie hält

**Die 20 Partner bei `NEXANS`, die ihre Richtungsebene verlieren**, ausgezählt am gerenderten Baum:
`ADIENT`, `AMPHENOL`, `ARVINMERITOR`, `AUDIO`, `AUTOLIV`, `BMWEVEREST`, `BMWHH`, `BMWSTEYR`,
`COFICABP`, `FALCON`, `GEBAUER`, `HALDEX`, `HENGST`, `KONGSBERG`, `OECHSLER`, `PBELEKTRO`, `PKC`,
`SAAB`, `SIEMENSVDO`, `SONDERPROZESS`. **Achtzehn von ihnen tragen genau einen Prozess** — dort fällt
die Ebene über einem einzigen Enkel weg und wäre reine Einrückung. **Zwei tragen mehr:**
`BMWEVEREST` zwei Prozesse, `SONDERPROZESS` elf. Zusammen sind es die 31 Blätter, die eine Ebene
weiter links stehen als ihre Nachbarn.

> **`SONDERPROZESS` ist der härteste Fall von E‑45, den die Daten hergeben — und er ist der beste
> Beleg für sie.** Elf Prozesse hängen dort direkt unter dem Partner, und die eine Richtung, die der
> Katalog kennt, ist **`null`**: Es ist die **einzige** leere Richtung im ganzen `NEXANS`-Baum (148
> mal `EINGEHEND`, 141 mal `AUSGEHEND`, einmal nichts). Ohne E‑45 stünde dort ein Knoten „nicht
> ermittelt" über elf Zeilen, der nichts ordnet und nur einrückt. Die elf tragen das Zeichen für
> „nicht ermittelt" stattdessen selbst — §3 des Konzepts, eine leere Stelle sagt nichts.

Das Paar, an dem sich die Ungleichförmigkeit ansehen lässt, steht direkt untereinander:

```
ACOME                                   20      ← Ebene 1
  Eingehend                              5      ← Ebene 2, Richtungsknoten
  Ausgehend                             15
ADIENT                                   0      ← Ebene 1
  ↙ Adient LAB (EDIFACT)                 0      ← Ebene 2, aber ein Prozess
AE-SAP                                   0
```

**Die Einrückung ist dieselbe, die Bedeutung nicht** — und genau das war die Sorge bei E‑45. Am Bild
trägt sie: Die Prozesszeile steht mit dem Richtungszeichen am Anfang da, die Richtungszeile ohne
eines; die eine hat ein Aufklappzeichen, die andere nicht. **Zwei Zeilen gleicher Tiefe sind
unterscheidbar, ohne dass man sie anklickt.** Bei `VOTG` ist die Frage gegenstandslos: **null**
Richtungsknoten bei 133 Partnern, keine einzige leere Ebene, und die Richtung steht in jeder der 390
Prozesszeilen. Bei `SUTTONS` steht ein einziger Knoten „nicht zugeordnet" mit 17 Prozessen und 1.337
Nachrichten — die Ansicht ist dort **nicht leer**.

### Die drei Zustände, nebeneinander im Bild

Unter `SONDERPROZESS` stehen sie zufällig alle drei untereinander, und damit ist die Regel aus §3 des
Konzepts — **nie allein über Farbe** — am Bild geprüft:

| Zustand | Was zu sehen ist |
|---|---|
| **bewegt** | normale Zeile, nichts weiter (`Prüfroutinen`, 390) |
| **still** | die Zeile normal, darunter **„seit über 3 Monaten nichts" als Marke** mit eigenem Grund (`PreProzessor/Datenrouter`) |
| **nie** | der ganze Text gedämpft, darunter **das Wort „noch nie"** (`Sonderprozesse-Status ungleich OK`) |

**Kein Zustand hängt an der Farbe allein**: „still" trägt einen Satz, „nie" trägt ein Wort *und*
die Dämpfung, „bewegt" trägt nichts — und die Abwesenheit ist hier die Aussage, weil die beiden
anderen sichtbar etwas tragen. Die Schwelle im Text kommt aus `stilleSchwelleMonate` der Antwort (E‑37); im
Bild steht „3", weil das Backend 3 schickt.

### M124 — Die drei Breitenzustände

Das Fenster wurde **ohne Neuladen** verstellt; die Umbruchpunkte sind Klassen, und ein Neuladen
misste den Ladevorgang statt des Layouts. Die Spalten „Liste" und „Panel" stammen aus zwei
Durchgängen — mit `?prozess=…` und mit `?prozess=…&nachricht=…` —, denn beide stehen nie zugleich
(E‑53).

| Breite | `main` | Baum | Baumspalte | Liste | Panel | „Zurück zum Baum" |
|---:|---:|---|---:|---|---|---|
| 1920 | 1712 | ✔ | 416 px | 1238 px | 1152 px | — |
| 1440 | 1232 | ✔ | 416 px | 758 px | 760 px | — |
| 1280 | 1072 | ✔ | **416 px** | 598 px ⚠️ | 600 px | — |
| 1279 | 1071 | ✔ | 412 px | 601 px | 603 px | — |
| 1024 | 816 | ✔ | 310 px | 448 px | 450 px | — |
| 768 | 560 | ✔ | 208 px | 294 px | 296 px | — |
| 767 | 767 | weicht | — | 741 px | 743 px | ✔ |
| 375 | 375 | weicht | — | 349 px | 351 px | ✔ |

**Die 26 rem stehen ab `xl` und keinen Pixel früher** (416 px bei Wurzelschrift 16 px); unterhalb
übernimmt der Anteil von 40 %, und bei 768 px bleiben davon 208 px — schmal, aber die Namen brechen
dort um statt abzuschneiden (M118). **Das Dokument scrollt in keiner der neun Breiten waagerecht**,
von 375 bis 1920 px; wo etwas quer läuft, läuft es im eigenen Kasten der Tabelle.

**Unter `md` weicht der Baum, und der Fokus geht mit.** Nachgefahren bei 700 px: Nach der Auswahl
eines Prozesses ist der Baum `display: none`, „Zurück zum Baum" ist sichtbar, und
`document.activeElement` **ist diese Schaltfläche**. Die Regel aus §15 hält am laufenden System —
und sie hält über die Klasse `md:hidden`, ohne dass irgendwo eine Fensterbreite abgefragt würde: bei
1920 px bleibt der Fokus nach derselben Auswahl auf der Baumzeile stehen.

### Die Tastatur, von Hand nachgefahren

**Ein Tabstopp**: unter 25 Zeilen trägt genau eine `tabindex="0"`, die übrigen 24 tragen `-1`. Ein
Tabulator führt aus dem Baum heraus auf das nächste Bedienelement — den Sortierknopf „Zeitpunkt"
der Liste rechts. **Pfeil ab und auf** bewegen, **rechts** klappt auf und steigt hinein, **links**
klappt zu und steigt hinauf, **Pos1** springt auf den ersten Partner, **Ende** auf den letzten Knoten
(bei `NEXANS`: „nicht zugeordnet, Partner, Prozesse: 216"). **Eingabetaste und Leertaste wählen** —
beide setzen `?prozess=…` und die Auswahl im Baum. Der Fokus folgt dem roving `tabindex`, und die
gewählte Zeile behält ihn.

### ⚠️ Befund 1 — Ein Scrollbereich, 155 Partner: die rechte Spalte verlässt das Bild (M125)

**Der Fall.** Baum und Übertragungsliste sitzen im **einen** Scrollbereich des Anwendungsrahmens
([`frontend-grundlagen.md`](frontend-grundlagen.md) §7); §15 nennt das und nennt auch die Folge.
Gemessen ist die Folge jetzt, und sie ist größer als „die Liste rutscht etwas nach oben":

| Vorgang bei 1920 × 1080 | `main.scrollTop` | Oberkante der rechten Spalte | im Bild? |
|---|---:|---:|---|
| Partner `VW` aufgeklappt, Prozess gewählt | 6.258 px | **−6.143 px** | **nein** |
| tiefer Link `?prozess=` auf `LEAR` (Platz 79 von 155) | 2.729 px | **−2.614 px** | **nein** |
| derselbe Link **mit** `&nachricht=` | 0 px | +115 px | ja |

**Wer weit unten im Baum auswählt, bekommt das Ergebnis außerhalb des Bildes.** Die Überschrift des
Prozesses, das Zeitfenster und die Tabelle stehen mehrere tausend Pixel über dem Sichtfenster; auf
dem Bildschirm bleibt rechts eine leere Fläche stehen. Bei `NEXANS` betrifft das jeden Partner
unterhalb des ersten Bildschirms — bei 1080 px Höhe stehen 18 bis 19 Zeilen im Bild (M121), es sind
also gut 130 der 155.

**Der tiefe Link mit Nachricht ist der Gegenfall, und er ist der Grund, warum es nicht schlimmer
kommt.** Dort springt der Baum bewusst nicht (§19, Befund 1): Das Panel steht oben und ist lesbar,
der Baum ist an der richtigen Stelle **geöffnet** — die gewählte Zeile liegt dann aber 3.765 px
unter der Kante, und der Nutzer sieht links den Anfang des Alphabets. **Von den drei Dingen, die die
Abnahme verlangt — Baum, Panel, richtige Stelle —, sind auch am laufenden System nur zwei zugleich
sichtbar.** Der Unterschied zu §19 ist, dass jetzt feststeht, welche zwei: Panel und richtige Stelle,
nicht das Bild des Baums.

**Das ist kein Fehler dieser Runde, sondern die Rechnung für eine Regel** — und die Regel ist
älter als diese Ansicht. Zwei Auswege wären denkbar, und beide sind Entscheidungen über
`frontend-grundlagen.md` §7 und nicht über diese Datei: die rechte Spalte kleben lassen
(`sticky`, wie der Baumkopf), oder dem Baum einen eigenen Scrollbereich geben. Der zweite
widerspricht §7 ausdrücklich, der erste nicht. **Offener Punkt 118.**

### ⚠️ Befund 2 — Bei 1280 px steht der Tabellenkopf übereinander (M126)

**Der Fall.** Offener Punkt **114** hielt fest, dass die Ablaufspalte der Liste unter 1280 px auf
0 px fällt und die Tabelle in ihrem Kasten waagerecht scrollt. Was niemand gesehen hatte: **Die
Spalte ist 0 px breit, ihre Beschriftung wird trotzdem gezeichnet.**

| Breite | Kasten | Tabelle | Ablauf (`th`) | Projekt (`th`) | Kopfzeile |
|---:|---:|---:|---:|---:|---|
| 1920 | 1238 | 1238 | 494 px | 288 px | lesbar |
| 1440 | 758 | 758 | **14 px** | 288 px | lesbar, Spalte praktisch weg |
| 1280 | 598 | **744** | **0 px** | 288 px | **„Ablauf" über „Projekt" gedruckt** |
| 1024 | 448 | 744 | 0 px | 288 px | dasselbe |
| 768 | 294 | 640 | 0 px | 288 px | dasselbe |

Die Kästen selbst überlappen nicht — die Zelle ist 0 px breit und sitzt genau dort, wo „Projekt"
beginnt. **Der Text ist nur nicht beschnitten**, und deshalb liegen zwei Wörter aufeinander. Im Bild
ist die Stelle als unlesbarer Klumpen zu sehen.

**Das trifft ausgerechnet 1280 px** — die Breite, für die E‑53 entworfen wurde und mit der §18
gerechnet hat. In der Nachrichtenliste tritt es nicht auf: Dort bekommt die Tabelle die volle
Inhaltsbreite, und die Ablaufspalte behält ihren Platz. **Es entsteht erst, wenn dieselbe Tabelle in
die schmale Spalte neben dem Baum gesetzt wird.**

**Punkt 114 bleibt damit eine Entscheidung über die Spaltenbreiten der Nachrichtenliste**, wie schon
festgehalten — er hat jetzt nur ein sichtbares Gesicht und nicht bloß eine Zahl. Ein Deckel wäre in
dieser Ansicht zu setzen (die Ablaufspalte ausblenden, statt sie auf 0 px zu quetschen) oder in der
gemeinsamen Tabelle (Beschriftungen beschneiden). **Beides ändert eine geteilte Komponente und ist
in dieser Runde nicht getan worden.**

### Befund 3 — Der Rückweg unter `md` verliert den Fokus

Der Weg **hin** ist geprüft und hält (oben): Auswahl → Fokus auf „Zurück zum Baum". Der Weg
**zurück** endet auf `document.body`. Die Schaltfläche verschwindet mit dem Kopf der rechten Spalte,
sobald kein Prozess mehr gewählt ist, und nimmt den Fokus mit — der nächste Tabulator beginnt wieder
oben am Anwendungsrahmen. **Das ist genau der Bruch, den die Gegenrichtung vermeidet.** Der Fokus
gehörte auf die zuletzt gewählte Zeile im Baum, der ja wieder da ist. **Offener Punkt 119.**

### Was die Gegenprobe entlastet hat

**`Alt+←` funktioniert — der erste Anschein täuschte.** Im kopflosen Chrome bewegt sich der Verlauf
bei `Alt+←` nicht, und das sah nach dem verschluckten Zurück aus, das §19 (Befund 2) behoben zu
haben behauptet. Die Gegenprobe trennt beides sauber: Ein Horcher in der Blasenphase am Dokument
zeigt, dass die Anwendung das Ereignis **nicht** unterdrückt (`defaultPrevented: false`), während
derselbe Pfeil **ohne** Modifier sehr wohl unterdrückt wird (`true`) — und `history.back()` aus
demselben Zustand geht zurück. **Die Anwendung reicht die Taste durch; das Übersetzen in eine
Rücknavigation macht die Browseroberfläche, und die gibt es im kopflosen Betrieb nicht.** Ohne diese
Gegenprobe wäre ein Werkzeugartefakt als Befund in die Dokumentation gewandert.

**Der Ansichtsumschalter steht nicht im Panel.** Eine Textsuche im Dokument fand „Ohne Liste
anzeigen" und ließ es so aussehen, als wäre er doch da. Er ist es nicht: Der Treffer steckt in einem
`<script>` des Entwicklungsservers (die Sprachdatei im Datenstrom), nicht in einem sichtbaren
Element — kein einziger Knoten und kein `title` trägt den Text. **`document.body.textContent` sieht
Skripte mit**, und das ist die Falle, nicht die Ansicht.

**Die leere rechte Fläche in den ersten Bildern war kein fehlender Aufbau.** Sie hatte zwei
Ursachen, beide harmlos beziehungsweise anderswo behandelt: der gewählte Prozess trug null
Nachrichten (Leerzustand), und der Baum war weit nach unten gescrollt (Befund 1).

### Die Abnahmeliste, Punkt für Punkt

| Abnahme | Stand | Beleg |
|---|---|---|
| `pnpm check` grün, `farbwerte.test.ts` ohne neue Ausnahme | ✔ | 31 Dateien, 753 Fälle, 02.09.2026 |
| `serverbausteine.test.ts` grün | ✔ | derselbe Lauf |
| Baum mit der Tastatur vollständig bedienbar | ✔ | ein Tabstopp, alle sieben Tasten, Eingabe **und** Leertaste |
| in allen vier Dichtestufen benutzbar, Berührungsziele halten | ✔ | M121, M122 — der `Switch` über seine Beschriftung |
| tiefer Link `?prozess=…&nachricht=…` zeigt Baum, Panel und die richtige Stelle | **teilweise** | M125: Panel und Stelle ja, das Bild des Baums nein — Befund 1 |
| bei `VOTG` keine leere Richtungsebene | ✔ | M123: 0 Richtungsknoten bei 133 Partnern |
| bei `SUTTONS` nicht leer, „nicht zugeordnet" | ✔ | M123: 1 Knoten, 17 Prozesse, 1.337 Nachrichten |
| Renderzeit bei `NEXANS` gemessen und dokumentiert | ✔ | M123: 98 ms React, 60 ms Endpunkt, 14,5 KiB |

> ### ⚠️ Belegvermerk zu M121 bis M126 (Regel L10)
>
> *Gemessen war:* die laufende Anwendung im Entwicklungsbetrieb (`next dev`, Turbopack), mit den
> echten Daten der Testkopie, angemeldet, über vier Mandanten, neun Fensterbreiten und vier
> Dichtestufen — Zeilenzahlen, Höhen, Breiten, Spaltenmaße, Scrollstände, Fokus, Tastenereignisse,
> Zeitmarken aus `PerformanceResourceTiming` und einem `MutationObserver`.
>
> *Behauptet wird nicht:* dass die Zeitwerte für den Produktionsbau gelten. `next dev` baut anders
> und langsamer; die 98 ms sind eine **obere** Schranke für React, kein Abnahmewert. Was von den
> Zeiten trägt, ist ihr Verhältnis zueinander und zu M120.
>
> *Nicht angesehen — vollständig, damit niemand mehr hineinliest, als dasteht:*
>
> - **kein echtes Berührungsgerät** (die Emulation meldet `pointer: coarse`, mehr nicht), **kein
>   anderer Browser als Chromium**, **keine Produktionsdatenbank**
> - **nur der helle Modus und nur Deutsch.** Die dunkle Fassung und die englischen Texte sind in
>   dieser Ansicht nie am Bild gewesen; die Sprachdateien sind über `sprachdateien.test.ts` geprüft,
>   das Bild nicht
> - **nur die vier beauftragten Mandanten** — `NEXANS`, `IBIS`, `VOTG`, `SUTTONS`. Die übrigen sechs
>   sind nicht aufgerufen worden
> - **kein Vorleseprogramm.** Die `aria-label` sind aus dem DOM gelesen und in ihrer Reihenfolge
>   geprüft; **vorgelesen** hat sie niemand
> - **kein Nutzer.** Ob die Ungleichförmigkeit aus E‑45 oder die 391 Zeilen bei `IBIS` jemanden
>   stören, bleibt eine Auslegung (offener Punkt 107)
>
> **Und keine Zeit ist an einer Wanduhr gegen eine Zusicherung geprüft** — die Zahlen beschreiben,
> sie sichern nichts zu (Regel T1).
---

# Vier Korrekturen aus der Sichtprüfung (Schritt 10c‑3, 02.09.2026)

Entsteht in **Schritt 10c‑3**. Auftrag: „Schritt 10c‑3: Prozessansicht — vier Korrekturen aus der
Sichtprüfung", Stand 02.09.2026. Die vier Punkte stammen aus der Sichtprüfung des Auftraggebers am
laufenden System, nach dem Abschluss von 10c‑2.

**Diese Runde ändert ausschließlich die Darstellung.** Kein Endpunkt, kein Statement, kein Feld der
Antwort ist angefasst worden; `zustand`, `richtung` und `stilleSchwelleMonate` werden unverändert
geliefert und unverändert gelesen. Was sich ändert, ändert sich in der Oberfläche — dieselbe
Trennung wie bei E‑46.

**Wo eine Entscheidung eine frühere umdreht, ist die frühere nicht gelöscht**, sondern mit einem
datierten Korrekturblock versehen: E‑51 in §16, das Zeichen und der „nie"-Kasten in §17, E‑53 und
die Sonderregel der verdeckten Liste in §18.

## 22. Nummernvergabe (Teil 3)

| | |
|---|---|
| **Entscheidungen** | **E‑54 bis E‑57**. Höchste vergebene war **E‑53** (§18) — nachgeprüft über `docs/` und alle Markdown-Dateien der Wurzel, kein höherer Treffer |
| **Messungen** | **M127 bis M129**. Höchste vergebene war **M126** (§21) — ebenso nachgeprüft |
| **Offene Punkte** | **120** und **121**. Höchster vergebener war **119** (§13). Die Zahlen 256, 500 bis 502 und 538 aus `messungen-schritt5.md` und `messungen-schritt10.md` sind Messwerte in Tabellen und keine Punkte |

**Alle drei Stände des Auftrags haben gestimmt.** Das ist erwähnenswert, weil sie es zweimal nicht
getan haben (§18, der Kasten zu E‑44).

> **Nachtrag vom 03.09.2026:** **E‑58** und **M130** sind am Tag darauf dazugekommen (§29). Höchste
> vergebene waren E‑57 und M129, beide aus dieser Datei; neue offene Punkte sind keine entstanden.

---

## 23. Wie gemessen worden ist — und die zwei Fallen des Messrahmens

**Am laufenden System, in der Sitzung des Auftraggebers** (`localhost:3000`, Backend
`localhost:8080`, Profil `dev`, Testkopie), über die verbundene Browsererweiterung. Kein zweites
Konto, keine erfundene Sitzung.

**Der Messrahmen ist ein gleichherkunftiger `<iframe>` und nicht das Browserfenster.** Die
Erweiterung kann die Fenstergröße nicht setzen — `resize_window` meldet Erfolg, und `innerHeight`
bleibt, wo es war (gemessen: 945 px bei angeblich 1080). Ein `<iframe>` derselben Herkunft ist
dagegen ein **eigenes Sichtfenster exakter Größe**, dessen DOM von außen vollständig lesbar ist. Das
trägt hier, weil die Anwendung ihre Höhe **nirgends am Fenster bemisst**
([`frontend-grundlagen.md`](frontend-grundlagen.md) §7): Sie rechnet im Rahmen genau so, wie sie es
in einem Fenster dieser Größe täte. Belegt ist das an der Gegenprobe zu M121 (siehe M127).

> ### ⚠️ Zwei Fallen, und beide hätten als Befund in diese Datei gewandert
>
> Dieselbe Klasse wie das verschluckte `Alt+←` aus §21 — ein Werkzeugartefakt, das aussieht wie ein
> Fehler der Anwendung.
>
> **1. In einem `visibility: hidden`-Rahmen ruht `requestAnimationFrame`.** Der erste Aufbau
> versteckte den Messrahmen, um die Seite darunter nicht zu verdecken. Layoutmaße sind davon
> unberührt — Chrome rechnet Layout auch für unsichtbare Rahmen —, aber die Fokusregel aus E‑57
> hängt an einem `requestAnimationFrame`, und **das lief nie**. Die Regel sah kaputt aus und war es
> nicht: Mit sichtbarem Rahmen greift sie in jedem Lauf. Alle Layoutzahlen sind hinterher mit
> **sichtbarem** Rahmen wiederholt worden und Ziffer für Ziffer identisch.
>
> **2. Die Zeitachse des Rahmens steht still.** Die Schublade der Prozessauswahl blendet mit
> `zoom-in-95` ein (0,1 s). Im Rahmen bleibt diese Einblendung auf `running` stehen, und die
> **gemalte** Zeilenhöhe misst deshalb 95 % der Layouthöhe — 41,8 px statt 44. Das sah nach einer
> Verletzung der Mindestfläche aus. `getAnimations().finish()` löst den Endzustand auf: Transform
> `none`, gemalte Höhe **44 px**. Gemessen wird seither `offsetHeight` (transformfrei) und die
> gemalte Höhe erst nach abgelaufener Einblendung.

**Was nicht gemessen worden ist, steht im Belegvermerk am Ende** — und dort vollständig.

---

## 24. E‑54 — Die Baumzeile trägt die Zeilenhöhe, die Berührungsfläche nur am Finger

*Schließt offenen Punkt **117**. Ersetzt E‑51 (§16).*

### Die Lage, die es ausgelöst hat

Die Baumzeile hielt `--dichte-beruehrung` (`max(2.75rem, 44px)`), und dieses Token ist aus der
Dichteskalierung **heraus** ([`visuelles-konzept.md`](visuelles-konzept.md) §5). Gemessene Folge
(M121): **19 / 19 / 18 / 16** sichtbare Zeilen über `xs`/`s`/`m`/`l` gegen 28 / 26 / 24 / 19 in der
Nachrichtenliste. In drei von vier Stufen griff überall derselbe Boden von 44 px — der Umschalter
bewegte im Baum nichts, und der Auftraggeber hat genau das beim Umstellen auf `s` bemerkt.

### Was gebaut ist

Ein eigenes Token, `--dichte-bedienzeile`, **neben** `--dichte-beruehrung` und nicht an dessen
Stelle:

```css
:root { --dichte-bedienzeile: var(--dichte-zeile); }        /* Zeigergerät: eine Zeile */
@media (pointer: coarse) {
  :root { --dichte-bedienzeile: var(--dichte-beruehrung); } /* Finger: ein Ziel */
}
```

**Der Name ist die Begründung.** `--dichte-bedienelement` ist ein Bedienelement im Rahmen (2 rem, am
Finger die Fläche); `--dichte-bedienzeile` ist die **Zeile, die zugleich ein Bedienelement ist** —
Baumzeile und Auswahlzeile. Dieselbe Bauform, dieselbe `@media`-Regel, eine Zeile darunter.

**Warum ein eigenes Token und nicht `--dichte-zeile` in der Komponente.** Am Finger gilt die
Mindestfläche weiterhin, und sie soll an **einer** Stelle stehen statt in zwei Komponenten. Genau
deshalb bleibt **offener Punkt 96 offen**: `--dichte-beruehrung` selbst ist unberührt geblieben, und
die Frage, was dieses Token trägt und was nicht, ist hier nicht entschieden worden.

### `pointer` und nicht `any-pointer` — und was das kostet

`pointer` beschreibt das **primäre** Eingabegerät. **Ein Notebook mit Berührungsbildschirm und
Trackpad meldet `fine` und bekommt die kürzere Zeile** — wer es am Bildschirm bedient, zielt dort
auf 36 px statt auf 44.

**Die Alternative wäre schlechter.** `any-pointer: coarse` trifft, sobald **irgendein**
angeschlossenes Gerät grob ist; dasselbe Notebook fiele dann auf 44 px zurück, und mit ihm jedes
Gerät mit erkanntem Berührungsbildschirm. Die Verkleinerung griffe praktisch nirgends, und das Token
wäre eine Umbenennung von `--dichte-beruehrung`. **Die Wahl ist getroffen, der Preis ist benannt** —
und sie ist dieselbe, die `globals.css` schon für `--dichte-bedienelement` und `--dichte-navzeile`
trifft. Eine dritte Antwort daneben wären zwei Begriffe von „Berührungsgerät".

### Die Prozessauswahl ist mitgezogen

Dieselbe Wahl, dieselbe Begründung — `prozess-filter.tsx`, beide Zeilenarten. **Nicht ausgenommen:**
Zwei Auswahllisten mit zwei Zeilenhöhen wären genau die Drift, die ein gemeinsames Token verhindern
soll.

### M127 — sichtbare Baumzeilen bei 1080 px, je Dichtestufe

`NEXANS`, Baum zugeklappt (155 Partnerzeilen), 1920 × 1080, dieselbe Strecke wie M121: gezählt sind
die Zeilen, die **ganz** zwischen der Unterkante des klebenden Baumkopfes und der Unterkante des
Scrollbereichs liegen.

| Stufe | Wurzelschrift | Kopfunterkante | untere Grenze | **Zeilenhöhe** | **sichtbar** | vorher (M121) |
|---|---:|---:|---:|---:|---:|---:|
| `xs` | 14 px | 215 px | 1.066 px | **31,5 px** | **26** | 19 |
| `s` | 15 px | 225 px | 1.065 px | **33,75 px** | **24** | 19 |
| `m` | 16 px | 235 px | 1.064 px | **36 px** | **22** | 18 |
| `l` | 18 px | 260,5 px | 1.062 px | **40,5 px** | **19** | 16 |

**Der Umschalter bewegt im Baum jetzt sieben Zeilen statt drei**, und `xs` und `s` unterscheiden
sich wieder (26 gegen 24) — die erste Folgerung aus M121 („die kleinste Stufe kauft dem Nutzer
nichts") ist damit erledigt. Zum Vergleich die Nachrichtenliste: **28 / 26 / 24 / 19**. Der Baum
liegt jetzt eine bis zwei Zeilen darunter und nicht mehr neun.

> **Die Gegenprobe, die den Messrahmen trägt.** Dieselbe Messung mit
> `--dichte-bedienzeile: var(--dichte-beruehrung)` stellt den Zustand **vor** E‑54 wieder her. Nach
> der Zählweise von M121 — `floor((untere Grenze − Kopfunterkante) ÷ Zeilenhöhe)` — ergibt sie
> **19 / 19 / 18 / 16**, also M121 Ziffer für Ziffer. Der Rahmen misst dieselbe Anwendung wie das
> Fenster.
>
> **Zwei Zählweisen, und der Unterschied ist eine Zeile.** Direkt ausgezählt (jede Zeile muss
> wirklich ganz im Kasten stehen) sind es 19/18/18/16 vor und **26/24/22/19** nach E‑54; nach der
> M121-Formel 19/19/18/16 und **27/24/23/19**. Der Unterschied ist der Abstand von 7 bis 9 px
> zwischen Baumkopf und erster Zeile, den die Formel überspringt. **Die direkte Zählung steht in der
> Tabelle**, weil eine Zeile erst sichtbar ist, wenn sie im Bild steht; die Formelwerte stehen hier,
> damit der Vergleich mit M121 einer bleibt.

### M127, zweiter Teil — die Mindestfläche am Berührungsgerät

**Das ist die tragende Zusicherung von Teil A**, und sie hält:

| Stufe | Baumzeile am Finger | Auswahlzeile der Prozessauswahl am Finger |
|---|---:|---:|
| `xs` | **44 px** | **44 px** |
| `s` | **44 px** | **44 px** |
| `m` | **44 px** | **44 px** |
| `l` | **49,5 px** | **49,5 px** |

**44 px werden in keiner Stufe unterschritten**, in beiden Ansichten. Die Zahlen decken sich mit
M122 (dort 44/44/44/50 — die 50 ist die gerundete 49,5).

> **Belegvermerk (Regel L10) zu dieser Tabelle.** *Gemessen:* die Höhen, die die Regel des
> Berührungsgeräts an den echten Zeilen der laufenden Anwendung erzeugt — angewandt wurde dafür
> genau die eine Deklaration, die `globals.css` unter `@media (pointer: coarse)` trägt.
> *Nicht emuliert:* das Gerät. Das Messgerät meldet `any-pointer: fine`, und die Erweiterung kann
> `pointer: coarse` nicht vortäuschen (M122 konnte es über das DevTools-Protokoll). **Dass die
> Deklaration in dieser `@media`-Regel steht und in keiner anderen, hält `tests/dichte.test.ts`
> fest** — samt der Gegenprobe, dass die Zeilenhöhe allein die Fläche in **keiner** Stufe trüge
> (36 px in `m`, 31,5 px in `xs`). Regel und Wirkung sind damit je einzeln belegt, ihr
> Zusammentreffen am echten Finger nicht.

---

## 25. E‑55 — Die einzelne Richtung steht als Wort

*Korrekturblock in §17.* Fällt die Richtungsebene weg (E‑45), trägt das Blatt die Richtung — seit
dem 02.09.2026 als **Wort** statt als Zeichen, und „nicht ermittelt" bekommt gar keine Stelle mehr.
Die Tabelle steht im Korrekturblock in §17, die Regel als reine Funktion `richtungswort` in
`prozessbaum.ts`.

### M128 — die Umbruchmessung, wiederholt

**Dieselbe Strecke wie M118, unverändert**: vier Mandanten, Breiten von 18 bis 34 rem, gezählt ist
ein Blatt, dessen Prozessname mehr als eine Zeile braucht (Höhe der Namensspanne gegen die gemessene
`line-height`). Gemessen am **gerenderten** Baum der laufenden Anwendung, alle Gruppen aufgeklappt.

| Breite | `NEXANS` (733) | `VOTG` (390) | `IBIS` (192) | `SUTTONS` (17) | zusammen (1.332) | M118 |
|---:|---:|---:|---:|---:|---:|---:|
| 18 rem | 407 | 339 | 191 | 14 | 951 (71,4 %) | 967 (72,6 %) |
| 20 rem | 206 | 248 | 156 | 5 | 615 (46,2 %) | 665 (49,9 %) |
| 22 rem | 81 | 177 | 93 | 2 | 353 (26,5 %) | 395 (29,7 %) |
| 24 rem | 38 | 111 | 47 | 2 | 198 (14,9 %) | 224 (16,8 %) |
| **26 rem** | **17** | **52** | **22** | **0** | **91 (6,8 %)** | **132 (9,9 %)** |
| 28 rem | 8 | 22 | 9 | 0 | 39 (2,9 %) | 46 (3,5 %) |
| 30 rem | 1 | 9 | 4 | 0 | 14 (1,1 %) | 20 (1,5 %) |
| 32 rem | 0 | 0 | 3 | 0 | 3 (0,2 %) | 3 (0,2 %) |
| 34 rem | 0 | 0 | **3** | 0 | **3 (0,2 %)** | **0** |

### ✔ 26 rem trägt weiter, und E‑49 wird nicht angetastet

**Bei `NEXANS` brechen unverändert 17 von 733 Namen um** — die erste der drei Zahlen, auf denen
E‑49 steht, ist Ziffer für Ziffer dieselbe. Über alle vier Mandanten sind es **91 statt 132**.

**Der Grund, warum das Wort weniger kostet, als der Auftrag befürchtet hat, ist eine Rechnung, die
niemand gemacht hatte:** Das Wort **ersetzt** ein Zeichen. Das Zeichen maß `size-3.5` (14 px) und
stand mit `gap-1.5` (6 px) vor dem Namen — jede Zeile ohne Richtungsebene bekommt also **20 px**
zurück. Wo ein Wort tritt, ist es netto „rund zehn Zeichen minus 20 px"; wo keines tritt, sind es
20 px geschenkt.

**Und Blätter mit Wort sind viel seltener als angenommen:**

| Mandant | Blätter ohne Richtungsebene | davon **mit Wort** | ohne (Richtung `null`) |
|---|---:|---:|---:|
| `NEXANS` | 31 | **20** | 11 |
| `VOTG` | 390 | **0** | 390 |
| `IBIS` | 20 | **20** | 0 |
| `SUTTONS` | 17 | **0** | 17 |
| **zusammen** | **458** | **40 (3,0 % aller Blätter)** | 418 |

> **Die 31 aus dem Auftrag sind für `NEXANS` richtig gezählt und für diese Frage die falsche Zahl.**
> Elf der 31 hängen unter `SONDERPROZESS`, und dessen einzige Richtung ist `null` — es ist die
> einzige leere Richtung im ganzen `NEXANS`-Baum (§21). Diese elf tragen kein Wort und verlieren nur
> den gestrichelten Kreis. **Betroffen sind 20 Zeilen, nicht 31.**

**`VOTG` ist der große Gewinner und war der befürchtete Verlierer:** null Wörter, 390 gestrichelte
Kreise weniger, und die Umbrüche fallen bei 26 rem von 99 auf **52**.

### ⚠️ Was das Wort kostet, und es steht bei `IBIS`

`IBIS` ist der einzige Mandant, der schlechter wird: 22 statt 16 Umbrüche bei 26 rem, und **erstmals
brechen drei Namen selbst bei 34 rem um**, wo M118 über alle vier Mandanten null gezählt hatte. Alle
drei tragen ein Wort:

```
Ausgehend  Ausgehende Rechnung Kaufland KEM (Versand an Markant)
Ausgehend  Ausgehende Rechnung Kaufland KEM PL (Versand an Markant)
Ausgehend  Ausgehende Rechnung Kaufland KEM RO (Versand an Markant)
```

**Das Wort steht dort doppelt, und es ist kein Einzelfall:** Bei `IBIS` beginnen **16 von 20** Namen
mit Wort selbst mit „Eingehende…" oder „Ausgehende…". Bei `NEXANS` sind es **0 von 20** — dort
stehen technische Kurznamen (`AMPHENOL_700026_LAB`). **Der Katalog beschreibt bei `IBIS` also
zweimal dasselbe**, einmal im Feld `richtung` und einmal im Namen.

> **Das ist ein Befund und keine Änderung an E‑55.** Ein Wort zu unterdrücken, weil der Name mit ihm
> beginnt, wäre eine Regel über **Oberflächentext** — genau das, was Regel Q4 und §17 überall sonst
> verbieten (die Eingrenzung greift aus demselben Grund nicht auf ihn zu). Und es wäre eine Regel,
> die in der englischen Fassung nicht mehr griffe. **Die Doppelung ist eine Kuratierungsfrage und
> keine Darstellungsfrage** — dieselbe Art von Befund wie die zwei Schreibweisen aus Punkt 106. Sie
> ist hier gezählt und nicht gelöst.

### Die Stufenprobe — nachgeprüft und nicht übernommen

M118 behauptet, die Dichtestufe ändere die Umbrüche nicht. **In einer zweiten Stufe nachgemessen**
(`NEXANS`, `xs` gegen `m`):

| Breite | `xs` | `m` |
|---:|---:|---:|
| 18 rem | 407 | **408** |
| 22 rem | 81 | 81 |
| 26 rem | 17 | 17 |
| 30 rem | 1 | 1 |
| 34 rem | 0 | 0 |

**Die Aussage hält** — eine Abweichung von einer Zeile bei der schmalsten Breite, dieselbe
Größenordnung wie M118s eigene Ausnahme (`IBIS` bei 22 rem, 88 gegen 89). Breite und Schriftgröße
liegen beide in `rem`; die Stufe ändert die Pixel und nicht die Umbrüche.

---

## 26. E‑56 — „nie" wird in der Zeile nicht mehr gekennzeichnet

*Korrekturblock in §17, unter dem Kasten „Warum ‚nie' trotzdem ein Wort bekommt".* Die Tabelle der
drei Zustände, die Begründung und der Preis stehen dort; hier steht nur, was daran **kein** Messwert
ist:

**Diese Entscheidung hat keine Messung.** Sie ist eine Aussage darüber, was ein Zustand *ist* —
Katalogfrage gegen Vorfall —, und die entscheidet sich nicht an einer Zahl. Was gezählt werden
könnte, ist bereits gezählt: 217 von 733 Prozessen bei `NEXANS` sind „nie" (Kopfzeile, Zeitraum
30 T), bei `VOTG` 89,7 % (M111). **Diese Zahlen sind das Argument für die Entscheidung und nicht ihr
Beleg** — sie sagen, dass die Kennzeichnung häufig ist, nicht, dass sie falsch ist.

**Offener Punkt 120** hält fest, was dabei verloren geht.

---

## 27. E‑57 — Bei offenem Panel weicht der Baum

*Korrekturblock in §18, dreht E‑53 um.* Die Aufteilung und die Begründung stehen dort.

### M129 — die Breitenzustände mit offenem Panel

Dieselbe Strecke wie M124, Stufe `m` (16 px Wurzelschrift, damit die Zahlen mit M119, M124 und M126
vergleichbar bleiben), `NEXANS`, tiefer Link auf `?prozess=…&nachricht=…`.

| Breite | `main` | Baum | Liste (Kasten) | Tabelle | Panel | Ablaufspalte | Querlauf im Dokument |
|---:|---:|---|---:|---:|---:|---:|---|
| 1280 | 1.072 | **weicht** | 585 | 744 | 416 | **0 px** ⚠️ | nein |
| 1440 | 1.232 | **weicht** | 745 | 744 | 416 | **0 px** ⚠️ | nein |
| 1536 | 1.328 | **weicht** | 777 | 775 | **480** | 31 px | nein |
| 1920 | 1.712 | **weicht** | 1.161 | 1.159 | **480** | 415 px | nein |

Zur Gegenprobe dieselben Breiten mit **geschlossenem** Panel, also Baum neben Liste:

| Breite | `main` | Baum | Liste (Kasten) | Tabelle | Ablaufspalte |
|---:|---:|---:|---:|---:|---:|
| 1280 | 1.072 | 416 | 585 | 744 | 0 px |
| 1440 | 1.232 | 416 | 745 | 744 | 0 px |
| 1536 | 1.328 | 416 | 841 | 839 | 95 px |
| 1920 | 1.712 | 416 | 1.225 | 1.223 | 479 px |

**Die beiden Tabellen tragen die Antwort auf Punkt 114, und sie ist besser als befürchtet.** Bis
`2xl` bekommt die Liste neben dem Panel **auf den Pixel dieselbe Breite** wie neben dem Baum — beide
Spalten sind 26 rem, eine ersetzt die andere. Erst ab `2xl` wächst das Panel auf 30 rem, und dort
kostet der Wechsel **64 px**: Die Ablaufspalte fällt von 95 auf 31 px (1536) und von 479 auf 415 px
(1920), bleibt aber in beiden Fällen sichtbar.

> **Die Rechnung des Auftrags ist bestätigt.** *Gerechnet war:* 1.016 px nutzbar minus 416 px Panel
> minus 16 px Abstand sind **584 px**. *Gemessen sind* **585 px**. Die Ablaufspalte fällt dort auf
> 0 px, und „Ablauf" steht über „Projekt" — beide `th` beginnen bei 685 px. **Befund 114 wandert mit
> und bleibt bestehen**, wie der Auftrag es festhält.
>
> ⚠️ **Eine Abweichung gegen M124, die benannt gehört:** Dort steht die Liste bei 1.440 px mit
> **758 px** und die Ablaufspalte mit 14 px; hier sind es 745 px und 0 px. Die 13 px Unterschied
> sind die Bildlaufleiste von `main`, die der Messrahmen zeichnet. **Welche der beiden Zahlen für
> das echte Fenster gilt, ist nicht entschieden** — und der Punkt daran ist nicht die Zahl, sondern
> dass **bei 1.440 px gut zehn Pixel darüber entscheiden**, ob die Ablaufspalte 14 px hat oder gar
> keine. Für den Vergleich der beiden Tabellen oben ist es gleichgültig: Der Versatz trifft beide
> Zustände gleich.

### Die drei Dinge, die daran hängen

**1. Der Aufklappzustand bleibt.** Nachgefahren bei 1920 px: drei Partner weit unten aufgeklappt
(`ETO`, `LEARGUS`, `SIEMENSVDO`), Prozess gewählt, Nachricht geöffnet, `Escape`. **Dieselben drei
stehen vor und nach dem Öffnen offen**, und die 160 Baumzeilen bleiben im DOM — `display: none`
hängt nichts aus. `Escape` schließt über `useEscapeSchliesst`, `nachricht` fällt aus der Adresse.

**2. `main.scrollTop` springt — und der Sprung ist die Verbesserung.**

| Vorgang | `main.scrollTop` | Höhe von `main` | Oberkante der rechten Spalte |
|---|---:|---:|---|
| tief im Baum, Prozess gewählt | 3.701 px | 5.970 px | **−3.586 px** — außerhalb des Bildes |
| Nachricht geöffnet | **0 px** | **1.029 px** | **+115 px** — im Bild |
| `Escape` | **3.701 px** | 5.970 px | −3.586 px |

**Der Auftrag verlangt zu belegen, dass der Scrollstand nicht springt; er springt, und die Aussage
ist damit widerlegt.** Der Grund ist nicht, dass die Anwendung scrollt, sondern dass die Seite
schrumpft: Mit dem Baum verschwinden fünf Sechstel ihrer Höhe, und der Browser kappt den Scrollstand
auf das, was übrig ist. **Beim Schließen kommt er zurück** — dieselbe Zahl, weil die Höhe
zurückkommt.

**Das entschärft offenen Punkt 118 an genau der Stelle, an der er am meisten weh tat.** Unter E‑53
stand die Überschrift der rechten Spalte 3.586 px über dem Sichtfenster, und der Nutzer sah rechts
eine leere Fläche (M125). Unter E‑57 steht das Panel **im Bild**, ohne dass irgendwo ein Sprung
programmiert wäre. **Punkt 118 bleibt trotzdem offen:** Er betrifft den Schritt **davor** — den Weg
vom Baum zur *Liste* —, und dort ist nichts anders geworden.

**3. Der Fokus — er kann im Baum liegen, und der Fall ist geregelt.**

Der Auftrag fragt, ob der Fokus beim Öffnen des Panels im Baum liegen kann. **Er kann, und der Weg
ist nachgefahren:**

| Weg ins Panel | wo der Fokus vorher liegt | wo er danach liegt |
|---|---|---|
| tiefer Link `?prozess=…&nachricht=…` | `document.body` | `document.body` — **unverändert**, kein Sprung |
| Klick oder Eingabetaste auf einer Listenzeile | auf der Zeile (`tabIndex={0}`) | **auf der Zeile** — nichts verschoben |
| Kettenglied im Panel | im Panel | im Panel |
| **Zurück/Vorwärts des Browsers, während eine Baumzeile den Fokus trägt** | **auf der Baumzeile** | **auf dem Panelbereich** |

**Über den Baum selbst ist der Fall nicht erreichbar** — er setzt nur `prozess`, nie `nachricht`.
Erreichbar ist er über den Verlauf, und `prozess` und `nachricht` legen mit `history: "push"` eigens
die Einträge dafür an. Vor der Regel fiel der Fokus dort auf `document.body` oder blieb auf der
**weggeblendeten** Baumzeile stehen; beides bricht die Tabulatorkette an derselben Stelle wie
Befund 3 aus §21.

> **Zwei Fassungen der Regel sind an der Messung gescheitert, und beide Male war die Bedingung
> richtig und nur falsch gestellt:**
>
> 1. *Die Abfrage im Effekt selbst.* Wenn der Effekt läuft, steht `document.activeElement` **noch
>    auf der Baumzeile** — der Browser setzt den Fokus erst zurück, wenn er das Rendern das nächste
>    Mal auffrischt. Daher `requestAnimationFrame`.
> 2. *Nur `document.body` abfragen.* Der Fokus fällt **nicht zuverlässig** dorthin: In einem Teil
>    der Läufe bleibt `document.activeElement` die Baumzeile, obwohl ihr Vorfahr `display: none`
>    trägt. Die Bedingung fängt seither **beide** Fälle.
>
> Ohne den Messrahmen wäre die erste Fassung als „gebaut" in diese Datei gewandert — sie sieht im
> Code richtig aus und tut nichts.

**Aufgefangen wird vom Panel*bereich*** (`tabIndex={-1}`) und nicht von der Schaltfläche
„Schließen". Die wäre das genauere Gegenstück zu „Zurück zum Baum", steht aber in
`nachricht-detail.tsx` — einem Baustein, den diese Ansicht als vierter Einhängepunkt benutzt und in
dieser Runde nicht anfasst. Der Bereich setzt die Tabulatorstelle an den Anfang des Panels; der
nächste Tabulator führt hinein und nicht an den Anwendungsrahmen.

**Kein neuer Umbruchpunkt und keine Abfrage der Fensterbreite.** Die Regel gilt in **jeder** Breite,
weil der Baum seit E‑57 in jeder Breite weicht — sie braucht deshalb keine Klasse wie `md:hidden`,
die §15 für den Rückweg gebraucht hat.

### Zwei Sonderregeln, die gegenstandslos geworden sind

**1. Das Aktualisierungsintervall der verdeckten Liste** — Kasten in §18, dort mit seinem Rest als
offener Punkt **121**.

**2. `max-w-inhalt` und `beschriftung-breit` am Panel.** Sie standen dort, weil das Panel unter E‑53
an die Stelle der Liste trat und deren volle Breite bekam (bei 1920 px gemessene 1.223 px, mehr als
die eigene Route je hatte). **Ab `xl` tut es das nicht mehr, und dort wären beide falsch:** Ein
Deckel von 16 rem ließe der Beschriftungsspalte 256 px von 416 px Panelbreite. Die 10 rem sind genau
für dieses Panel gemessen ([`bam-werte.md`](bam-werte.md) §11a). Das Panel trägt seither **Klasse
für Klasse dieselbe Hülle wie in der Nachrichtenliste** — `xl:w-[26rem] 2xl:w-[30rem]` —, und das
ist die eigentliche Wirkung von E‑57: Der vierte Einhängepunkt sieht aus wie der erste.

---

## 28. Tests, Regelbezug und was diese Runde nicht getan hat

### Die Tests

**Angepasst, nicht entfernt.** `pnpm check` läuft durch: 31 Dateien, **764 Fälle** (753 vor dieser
Runde).

| Datei | was dazugekommen ist |
|---|---|
| `tests/dichte.test.ts` | **Vier Fälle für `--dichte-bedienzeile`**, dieselbe Bauform wie für `--dichte-beruehrung` daneben: dass es am Zeigergerät auf `--dichte-zeile` steht (stünde dort die Fläche, wäre E‑54 eine Umbenennung), dass **genau eine** `@media (pointer: coarse)`-Regel es auf die Fläche zurücksetzt und keine davon `any-pointer` heißt, dass der Rückfall **gebraucht** wird (die Zeilenhöhe allein trüge die 44 px in keiner Stufe — gerechnet, nicht hingeschrieben), und dass `--spacing-bedienzeile` verdrahtet ist |
| `tests/prozessbaum.test.ts` | **Vier Fälle für `richtungswort`**: nur am Blatt ohne Ebene; „nicht ermittelt" bekommt nichts; ein vierter Katalogwert steht da, wie er im Katalog steht (Regel Q4); der Wortlaut kommt aus `texte` und unterscheidet sich zwischen den Sprachen — die Probe, warum die Eingrenzung ihn nicht durchsucht. Dazu **zwei angepasste**: `zustandstext("NIE", …)` ist `null`, und der vorgelesene Name nennt „nie" nicht mehr, während „still" darin bleibt |
| `tests/prozess-baum.test.tsx` | **Zwei Fälle am gerenderten Baum** — die Verdrahtung, die keine reine Funktion zeigt: dass an der Stelle des Zeichens **kein `svg` mehr steht** (Gruppen behalten ihres) und dass die „nie"-Zeile weder das Wort noch `text-muted-foreground` trägt. Dazu der angepasste Höhenfall: `min-h-bedienzeile` an jeder Zeile **und `min-h-beruehrung` an keiner** |

### Regelbezug

| Regel | wie sie hier greift |
|---|---|
| **L10** Belegvermerke | Jeder Zahlensatz trägt seinen Vermerk; die Rechnung aus Teil D ist als gerechnet gekennzeichnet und von M129 mit 585 gegen 584 px belegt |
| **Q4** nichts raten | Kein Oberflächentext wird zur Filtergrundlage. Der vierte Katalogwert der Richtung erscheint, wie er dasteht — und ist damit erstmals von „nicht ermittelt" unterscheidbar. Die Doppelung bei `IBIS` ist gezählt und **nicht** durch eine Textregel behoben |
| **T1** Plantests | Keine Laufzeit als Zusicherung. **Kein Statement ist angefasst**, kein Plan hat sich geändert |
| **Z1** Anwendungsuhr | Nicht berührt |
| **L7** mehrere Mandanten | M128 über dieselben vier Mandanten wie M118, einer davon klein (`SUTTONS`, 17 Blätter) |

### Was diese Runde nicht getan hat

1. **Kein Endpunkt, kein Statement, kein Feld.** `zustand` und `richtung` werden unverändert
   geliefert; E‑35, E‑36 und E‑37 gelten.
2. **Keine zweite Fassung der Nachrichtenliste.** Offener Punkt 114 bleibt offen und ist nur
   fortgeschrieben.
3. **Keine Entscheidung über `--dichte-beruehrung` selbst.** Offener Punkt 96 bleibt offen; das neue
   Token steht **neben** ihm.
4. **Kein neuer Umbruchpunkt.** `md`, `xl` und `2xl` genügen; keine Fensterbreite wird in JavaScript
   abgefragt.
5. **Kein freier Zeitraum.** Der ist Gegenstand von 10c‑4.
6. **Keine Regel gegen die Doppelung „Ausgehend Ausgehende…".** Sie wäre eine Regel über
   Oberflächentext (§25).

> ### ⚠️ Belegvermerk zu M127 bis M129 (Regel L10)
>
> *Gemessen war:* die laufende Anwendung im Entwicklungsbetrieb (`next dev`), angemeldet, mit den
> echten Daten der Testkopie, über vier Mandanten, neun Spaltenbreiten, vier Dichtestufen und vier
> Fensterbreiten — Zeilenhöhen, Kopfhöhen, sichtbare Zeilen, Umbrüche, Spalten- und Kastenbreiten,
> Scrollstände, Seitenhöhen, Aufklappzustände und `document.activeElement`.
>
> *Gemessen im **Rahmen** und nicht im Fenster* (§23). Die Gegenprobe zu M121 stimmt Ziffer für
> Ziffer; die eine bekannte Abweichung ist die Bildlaufleiste von `main` bei 1.440 px (13 px, §27).
>
> *Nicht angesehen — vollständig:*
>
> - **kein echtes Berührungsgerät und keine Emulation.** Angewandt ist die Deklaration, nicht das
>   Gerät; die Regel selbst hält `tests/dichte.test.ts` fest
> - **kein anderer Browser als Chromium**, **keine Produktionsdatenbank**
> - **nur der helle Modus und nur Deutsch.** Die englischen Richtungswörter sind über
>   `sprachdateien.test.ts` und `tests/prozessbaum.test.ts` geprüft, das **Bild** nicht
> - **nur die vier beauftragten Mandanten**
> - **kein Vorleseprogramm.** Dass „nie" aus dem `aria-label` verschwunden ist, ist im Test belegt
>   und nicht gehört
> - **kein Nutzer.** Ob das Wort besser liest als das Zeichen, ob der weggeblendete Baum stört und
>   ob der Schalter den verlorenen „nie"-Hinweis ersetzt (Punkt 120), bleibt in allen drei Fällen
>   eine Auslegung
>
> **Und keine Zeit ist an einer Wanduhr gegen eine Zusicherung geprüft** (Regel T1).
---

# Nachtrag: die Richtung steht überall gleich (03.09.2026)

*Ein Tag nach 10c‑3, nach einer zweiten Sichtprüfung des Auftraggebers am Bild.* Diese Runde nimmt
**E‑55 zurück** und engt **E‑45** ein; sie ändert wieder ausschließlich die Darstellung.

## 29. E‑58 — Die Richtungsebene fällt nur weg, wo es nichts zu schreiben gibt

### Der Einwand, und er steht in einem Bild

E‑55 hat die Richtung eines Blattes ohne Richtungsebene als **Wort vor den Prozessnamen** gestellt.
Am laufenden System sieht das so aus:

```
ACOME                     20          ACOME                     20
  > Eingehend              5            > Eingehend              5
  > Ausgehend             15            > Ausgehend             15
ADIENT                     0          ADIENT                     0
  Eingehend Adient LAB …    0            > Eingehend              0
                                            Adient LAB …          0

       vorher (E‑55)                          jetzt (E‑58)
```

**Zwei Schreibweisen für denselben Sachverhalt, direkt untereinander.** Bei `ACOME` ist „Eingehend"
eine Zeile, bei `ADIENT` ein Vorsatz — und der Unterschied sagt nichts über die Daten aus, sondern
nur darüber, wie viele Richtungen der Partner zufällig führt. Das war schon der Kern des Einwands
gegen die Zeichenfassung (§17); das Wort hat ihn nicht behoben, sondern nur besser lesbar gemacht.

### Die Entscheidung

**Eine bekannte Richtung steht immer als eigene Ebene**, auch über einem einzigen Kind. Das Blatt
trägt sie in keinem Fall mehr — weder als Zeichen noch als Wort noch im `aria-label`.

| Fall | Ebene | am Blatt |
|---|---|---|
| Partner mit **zwei** Richtungen | steht | nichts |
| Partner mit **einer bekannten** Richtung | **steht** *(neu)* | nichts |
| Partner mit einer Richtung **`null`** | **fällt weg** | **nichts** *(neu — vorher das Wort bzw. das Zeichen)* |
| gepflegter, aber unbekannter vierter Wert | **steht**, mit dem Wert als Beschriftung | nichts |

**Was von E‑45 bleibt, ist der Fall, für den sie gebaut war.** Ein Knoten „nicht ermittelt" über
einem einzigen Kind ordnet nichts und schreibt nichts hin — bei `VOTG` wären es **133** solche
Knoten, einer je Partner (M110). Offener Punkt 108 bleibt geschlossen. **Und dort steht auch kein
Ersatz mehr**: Wo die Ebene fehlt, fehlt die Angabe, und das ist dieselbe Aussage.

> **Der Verzicht gegen [`visuelles-konzept.md`](visuelles-konzept.md) §3 bleibt bestehen und wird
> kleiner.** Er betrifft nur noch die Zeilen, an denen die Angabe **fehlt** — nicht mehr die, an
> denen sie vorhanden ist. Ein Zeichen an jeder der 390 `VOTG`-Zeilen sagt weiterhin nichts, was
> der Nutzer nicht schon weiß.

**Ein gepflegter, aber unbekannter Wert ist *bekannt*** und bekommt seine Ebene (Regel Q4): Er
steht als Zeile da, wie er im Katalog steht. Das ist die Fassung, die die Zeichenfassung nicht
konnte — dort fiel er in denselben gestrichelten Kreis wie `null`.

### Verworfen: die Ebene **immer** aufmachen, auch bei `null`

Das wäre die vollständige Gleichförmigkeit, und sie kostet zu viel: **133 Knoten „nicht ermittelt"
bei `VOTG`**, einer je Partner, über je einem Ast, den sie nicht ordnen. Genau dagegen ist E‑45
entstanden, und die Zahl steht seit M110 fest. **Die halbe Gleichförmigkeit ist hier die richtige**,
weil das, was ungleich bleibt, auch nichts anzeigt: Eine Zeile, die weiter links steht, trägt keine
Richtung — es gibt an ihr nichts, was woanders anders stünde.

### M130 — was die Umkehrung an Zeilen und Umbrüchen kostet

Dieselbe Strecke wie M118 und M128: vier Mandanten, alle Gruppen aufgeklappt, Stufe `xs`, gezählt
ist ein Blatt, dessen Prozessname mehr als eine Zeile braucht.

**Die Baumgröße** (in Klammern der Stand von 10c‑2):

| | Zeilen offen | Partnerknoten | Richtungsknoten | Blätter auf Ebene 2 |
|---|---:|---:|---:|---:|
| `NEXANS` | **1.177** *(1.158)* | 155 | **289** *(270)* | **11** *(31)* |
| `VOTG` | 523 *(523)* | 133 | **0** *(0)* | 390 *(390)* |
| `IBIS` | **409** *(391)* | 79 | **138** *(120)* | **4** *(24)* |
| `SUTTONS` | 18 *(18)* | 1 | 0 *(0)* | 17 *(17)* |

**19 Zeilen mehr bei `NEXANS`, 18 bei `IBIS`, keine bei `VOTG` und `SUTTONS`.** Von den 31
`NEXANS`-Blättern, die eine Ebene weiter links standen, bleiben **elf** — die von `SONDERPROZESS`,
dessen einzige Richtung `null` ist (§21). Bei `IBIS` bleiben vier.

**Die Umbrüche** — und die Zeile, auf die es ankommt, ist die mit 26 rem:

| Breite | `NEXANS` (733) | `VOTG` (390) | `IBIS` (192) | `SUTTONS` (17) | zusammen | M128 (10c‑2) | M118 (vor E‑55) |
|---:|---:|---:|---:|---:|---:|---:|---:|
| 18 rem | 398 | 366 | 191 | 16 | 971 | 951 | 967 |
| 20 rem | 202 | 304 | 152 | 10 | 668 | 615 | 665 |
| 22 rem | 78 | 225 | 89 | 4 | 396 | 353 | 395 |
| 24 rem | 38 | 145 | 40 | 2 | 225 | 198 | 224 |
| **26 rem** | **17** | **99** | **17** | **0** | **133 (10,0 %)** | 91 | **132 (9,9 %)** |
| 28 rem | 7 | 33 | 7 | 0 | 47 | 39 | 46 |
| 30 rem | 1 | 16 | 3 | 0 | 20 | 14 | 20 |
| 32 rem | 1 | 0 | 3 | 0 | 4 | 3 | 3 |
| 34 rem | 0 | 0 | 2 | 0 | 2 | 3 | 0 |

**Der Zustand liegt praktisch wieder auf M118, und das war zu erwarten:** `VOTG` und `SUTTONS` sind
**Ziffer für Ziffer** identisch mit M118 — dort ist der gestrichelte Kreis durch einen Platzhalter
derselben Breite ersetzt worden, sonst nichts. Was bleibt, ist der Preis der tieferen Einrückung
für die 40 Blätter, die von Ebene 2 auf Ebene 3 gerückt sind: **1,25 rem weniger Platz**, und daraus
folgt bei 26 rem **eine** zusätzliche Zeile gegenüber M118 (133 gegen 132), bei `IBIS`.

> **E‑49 und die 26 rem sind zum zweiten Mal unangetastet.** `NEXANS` steht bei **17 von 733** — in
> M118, in M128 und in M130 dieselbe Zahl. Die erste der drei Zahlen, auf denen E‑49 steht, hat
> beide Umbauten überstanden.

**Der Befund zu `IBIS` aus §25 bleibt und ist kein neuer mehr:** Dort beginnen die Prozessnamen mit
„Eingehende…" / „Ausgehende…", und über ihnen steht jetzt die Ebene mit demselben Wort. **Das ist
seit Schritt 10c‑2 der Normalfall für jeden Partner mit zwei Richtungen** und keine Eigenheit der
weggefallenen Ebene mehr — die Doppelung ist eine Kuratierungsfrage geblieben und keine
Darstellungsfrage.

### Die Tests

| Datei | was sich geändert hat |
|---|---|
| `tests/prozessbaum.test.ts` | Der `E‑45`-Block heißt jetzt **E‑58** und prüft **vier** Fälle statt zwei: zwei Richtungen, eine **bekannte** (Ebene steht — der Fall aus dem Bild), eine **unbekannte** (Ebene fällt weg), und ein gepflegter vierter Wert (Ebene steht, mit dem Wert als Beschriftung). Dazu **die Abwesenheit als Regel**: keine Blattzeile trägt über alle vier Knotenarten hinweg ein Feld `richtung`, und keine Blattbeschriftung nennt eine der drei Richtungen. **Die vier Fälle zu `richtungswort` sind entfallen**, weil die Funktion es ist |
| `tests/prozess-baum.test.tsx` | Zwei Fälle statt einem: dass die Ebene über einem einzigen Kind **steht**, wenn die Richtung bekannt ist (`aria-level` 1/2/3, das Wort an der Ebene und **nicht** am Blatt), und dass sie bei `null` **wegfällt**, ohne dass irgendetwas nachgetragen würde. Die Zählung der `svg` je Zeile bleibt die Probe für „kein Zeichen am Blatt" |

**`pnpm check` grün: 31 Dateien, 763 Fälle** — einer weniger als in 10c‑3, weil aus den vier Fällen
zu `richtungswort` drei zu E‑58 geworden sind.

> ### ⚠️ Belegvermerk zu M130 (Regel L10)
>
> *Gemessen war:* der gerenderte Baum der laufenden Anwendung, angemeldet, über dieselben vier
> Mandanten und dieselben neun Spaltenbreiten wie M118 und M128 — Zeilenzahlen, Knotenarten je
> `aria-level`, Umbrüche über die gemessene `line-height`. Dazu das Bild von `ACOME` und `ADIENT`
> untereinander, in derselben Stufe wie das Bild des Auftraggebers.
>
> *Nicht neu gemessen:* M127 und M129. Die Zeilenhöhe ändert sich durch E‑58 nicht, und die
> Breitenzustände der rechten Spalte auch nicht — der Baum weicht bei offenem Panel ohnehin
> vollständig. **Was sich ändert, ist die Höhe des Baums** (19 Zeilen bei `NEXANS`), und das
> verschiebt nur eine Zahl, die ohnehin als Größenordnung geführt wird.
>
> *Nicht angesehen:* dasselbe wie in 10c‑3 — kein echtes Berührungsgerät, kein zweiter Browser,
> nur der helle Modus, nur Deutsch, kein Vorleseprogramm, kein Nutzer.

---

# Teil 4 — die Messrunde zum freien Zeitfenster *(10c‑4a, 07.09.2026)*

**Diese Runde misst. Sie baut nichts und entscheidet nichts.** Auftrag: „Prozessansicht — freies
Zeitfenster, Messrunde (Schritt 10c‑4a)", Stand 07.09.2026. Anlass: Die Prozessansicht soll neben
`48H` / `30T` / `12M` ein **freies Zeitfenster** bekommen, stundengenau (E1′‑b), über einen vierten
optionalen Knopf des geteilten Umschalters (E3‑a). Die Vorgabe, aus der alles Weitere folgt: *Der
Nutzer soll nicht sehen, aus welcher Ebene die Daten kommen; wichtig ist nur, dass die Zahlen stimmen
und alles aus dem abgefragten Zeitraum stammt.* Also **kein Ausweiten** auf Eimergrenzen und **kein
Beschneiden** — das Fenster wird exakt beantwortet oder gar nicht.

Zwei Bauformen standen zur Wahl, und diese Runde beziffert sie:

| | |
|---|---|
| **Fassung E** — eine Ebene | Gelesen wird die **gröbste** Ebene, auf deren Eimergrenzen **beide** Fenstergrenzen fallen. Ein stundengenaues, krummes Fenster fällt damit auf die **Stundenebene**, auch über zwölf Monate |
| **Fassung Z** — die Zerlegung | Ränder fein, Mitte grob: Stunden bis zur nächsten Tagesgrenze, Tage bis zur nächsten Monatsgrenze, dazwischen Monate. In zwei Bauformen: **Z‑U**, ein Statement mit `UNION ALL` in einer Ableitung; **Z‑D**, drei Statements, eines je Ebene, in Java summiert |

**Das Tor stand vor der Messung fest** (Auftrag §7) und ist in §36 angewandt — **ohne Auslegung**,
und zwar deshalb, weil die Lesart vorher aufgeschrieben war.

---

## 30. Rahmen, Nummernvergabe, Vergleichsanker, Belegungsprobe

### Nummernvergabe (Teil 4)

| | |
|---|---|
| **Messungen** | **M147 bis M151.** `grep -rnoE '\bM1(4[7-9]\|5[01])\b' docs/ scripts/ *.md` → **kein Treffer**. Gegenprobe `\bM14[0-6]\b` → Treffer (M145 in `messungen-schritt10b.md` und `dashboard.md`, M146 in `dashboard.md`, `dashboard-frontend.md`, `annahmen-korrekturen.md`, `README.md` und `scripts/messung-schritt10c-verdichtung/`) — der Ausdruck greift, jeder Treffer gelesen. **M146 ist die höchste vergebene**, wie der Auftrag sagt |
| **Entscheidungen** | **keine.** `grep -rnE 'E‑9[2-9]'` → kein Treffer; E‑91 steht in `dashboard-frontend.md` und `README.md`. **E‑92 bleibt frei** |
| **Offene Punkte** | ab **137**. Höchster vergebener ist **136** (`dashboard-frontend.md` §9, seit E‑90 gegenstandslos) — der Stand des Auftrags stimmt |

**Alle drei Nummernstände des Auftrags haben gestimmt.**

### Der Rahmen

| | |
|---|---|
| **Datenbank** | Testkopie, `10.6.22-MariaDB-0ubuntu0.22.04.1-log`, `@@global.read_only = 1` in jeder Sitzung geprüft. Benutzer `monitor_read`. **Es ist nichts geschrieben worden, auch nicht in `overlord_monitor`** |
| **Client** | `mysql.exe` 8.0.46 aus MySQL Workbench 8.0 CE, `--ssl-mode=DISABLED`, `--default-character-set=utf8mb4`, `-t` — dieselbe Form wie M116 und wie seit M32 (**Abweichung A1** der Messreihen: `--skip-ssl` ist die MariaDB-Schreibweise, dieser Client kennt sie nicht; ein Clientwechsel kostete die Vergleichbarkeit) |
| **Verfahren** | `SET profiling = 1`, `profiling_history_size = 100`, Laufzeiten aus `information_schema.PROFILING` je `QUERY_ID` — **Aufwärmlauf, dann beste von fünf**, exakt die Form aus M116 (`erzeuge-s4.py` der Vorrunde) |
| **Zeitgrenze** | `SET max_statement_time = 60` in jeder Sitzung. **Kein Statement dieser Runde hat sie gerissen** — der längste Lauf liegt bei 854,991 ms (Aufwärmlauf `NEXANS`/365 T, M147) |
| **Statementtext** | **nicht abgetippt.** `scripts/messung-prozessansicht-frei/erzeuge.py` liest `scripts/messung-prozessansicht/gerendert.txt` — den Text, der in M116 aus `ProzessbaumRepository` gegen die jOOQ‑Attrappe gefallen ist und den `ProzessbaumStatementsTest` wörtlich festhält — und ersetzt **ausschließlich** das Bereichsprädikat und den Mandanten; jede Ersetzung wird gezählt und muss genau einmal greifen. Die vier Eigenschaften aus §6 bleiben damit im Text: `EXISTS` statt Join, keine Funktion um den Eimerschlüssel, jede Lesung auf ihrer Ebene (die Deckelung betrifft nur das Gerüst, das hier nicht gemessen ist) |
| **Mandanten** | `NEXANS` (733) · `VOTG` (390) · `IBIS` (192) · `SUTTONS` (17) — dieselben vier wie M116, drei Größenordnungen (L7) |
| **Skripte** | `scripts/messung-prozessansicht-frei/`: `erzeuge.py` (Generator), sechs Sitzungen `s0` bis `s4`, `filtere-ergebnis.py` (nimmt die Ergebniszeilen heraus — sie tragen Prozesskennungen, G1), `werte.py` (liest die Laufzeiten aus den gefilterten Protokollen, damit keine Zahl abgetippt wird). Eingecheckt sind Generator, Sitzungen und die **gefilterten** Protokolle `ergebnis/*.gefiltert.txt`; die Rohausgaben stehen in `.gitignore` |

**Die Sitzungen**, Serverzeit vom 07.09.2026:

| # | Datei | Inhalt | Serverzeit |
|---|---|---|---|
| 0 | `s0-rahmen.sql` | Bestand, Vergleichsanker, Belegungsprobe, Ergebniszeilen je Mandant | 12:08:25 |
| 1 | `s1-m147-stundenebene.sql` | **M147**, vier Mandanten × fünf Spannen | 12:08:44 – 12:09:01 |
| 2 | `s2-m148-tagesebene.sql` | **M148**, vier Mandanten × fünf Spannen | 12:09:01 – 12:09:19 |
| 3 | `s3-m149-m150-zerlegung.sql` | **M149 / M150**, erster Lauf — **verworfen**, Abweichung A2 | 12:09:19 – 12:09:23 |
| 4 | `s4-m151-gleichheitsprobe.sql` | **M151**, vier Mandanten, je zwei Proben | 12:09:32 |
| 0b | `s0b-zeilen-je-mandant.sql` | Nachtrag: Rollupzeilen **je Mandant** im Fenster — nötig geworden durch die Pläne aus M147 | 12:13:22 |
| 3 | `s3-m149-m150-zerlegung.sql` | **M149 / M150**, zweiter Lauf, mit korrigierter Auswertung | 12:13:25 – 12:13:28 |

> **Abweichung A2 — der erste Lauf von Sitzung 3 ist verworfen.** Die Auswertung „Summe je Runde"
> in M150 zählte die unmittelbar davor gelaufene Auswertung „je Ebene" als siebte Runde mit
> (`anzahl_runden = 7`, und die „beste" war mit 9,435 ms die Auswertungsabfrage selbst). Der
> Generator setzt seither eine obere Schranke auf die `QUERY_ID` (`@basis + 1 + 18`), und die
> Sitzung ist vollständig neu gefahren. Die Werte je Ebene des ersten Laufs — `NEXANS` Z‑U 71,236 ms,
> Z‑D 2,087 / 56,228 / 69,503 ms — liegen innerhalb von 1 % der unten ausgewiesenen; sie stehen hier,
> damit niemand den verworfenen Lauf für einen anderen Befund hält.

### Der Bestand — die Testkopie ist unverändert

| Ebene | Zeilen | `SUM(anzahl)` | erster Eimer | letzter Eimer |
|---|---:|---:|---|---|
| Stunde | **335.610** | 3.341.519 | `2024-10-01 02:00` | `2026-07-08 17:00` |
| Tag | **123.049** | 3.341.519 | `2024-10-01` | `2026-07-08` |
| Monat | **11.957** | 3.341.519 | `2024-10-01` | `2026-07-01` |

Zeichen für Zeichen die Zahlen aus [`rollup.md`](rollup.md) §2 (M87/M94, M107). Die Summenprobe
hält über alle drei Ebenen.

### Der Vergleichsanker — M116 wiederholt

Die beiden Statements aus `gerendert.txt` unverändert, mit den gebundenen Werten von M116:

| Fall | M116 (02.09.2026) | **heute** | Abweichung |
|---|---:|---:|---:|
| `NEXANS`, Kennzahlen 48 h / Stunde | 6,781 ms | **6,761 ms** | **−0,3 %** |
| `NEXANS`, Kennzahlen 12 M / Monat | 75,743 ms | **76,243 ms** | **+0,7 %** |

Beide weit innerhalb der ±25 %, die der Auftrag als Grenze der Vergleichbarkeit gesetzt hat. Die
Pläne sind dieselben wie in §8 (`range` über `PRIMARY`, `key_len` 5 bzw. 3, danach die
`eq_ref`‑Kette); nur die Zeilenschätzung der Monatsebene lautet heute `6117` statt `5961` — eine
Stichprobenschätzung von InnoDB, keine Änderung der Daten (Bestand oben).

### Die Belegungsprobe — mandantenfrei, die Zeilen des Bereichs

Alle Fenster enden auf **`2025-12-30 03:00:00`**, unterhalb des Dev‑Ankers `2025-12-30 04:09:47`
und vollständig im dichten Bestand.

**Stundenebene** (`stunde >= von AND stunde < '2025-12-30 03:00:00'`):

| Spanne | von | Zeilen | `SUM(anzahl)` | belegte Eimer | Prozesse |
|---|---|---:|---:|---:|---:|
| 24 h | `2025-12-29 03:00` | 575 | 6.238 | 24 | 156 |
| 7 T | `2025-12-23 03:00` | 3.097 | 26.961 | 168 | 246 |
| 30 T | `2025-11-30 03:00` | 21.263 | 214.114 | 720 | 643 |
| 90 T | `2025-10-01 03:00` | 70.809 | 680.534 | 2.160 | 693 |
| **365 T** | `2024-12-30 03:00` | **280.980** | 2.713.420 | 8.758 von 8.760 | 732 |

**Tagesebene** (`tag >= von AND tag < '2025-12-30'`):

| Spanne | von | Zeilen | `SUM(anzahl)` | belegte Eimer | Prozesse |
|---|---|---:|---:|---:|---:|
| 1 T | `2025-12-29` | 171 | 6.249 | 1 | 158 |
| 7 T | `2025-12-23` | 710 | 27.026 | 7 | 248 |
| 30 T | `2025-11-30` | 6.865 | 214.330 | 30 | 643 |
| 90 T | `2025-10-01` | 24.060 | 680.872 | 90 | 693 |
| **365 T** | `2024-12-30` | **100.597** | 2.713.376 | 365 | 732 |

Die Summen der beiden Ebenen weichen um wenige Dutzend Nachrichten voneinander ab, **und das ist
richtig**: Die tagesgenauen Fenster liegen um drei Stunden am Anfang und drei Stunden am Ende anders
als die stundengenauen. Zwei verschiedene Fenster, zwei verschiedene Summen.

**Der Bösfall** `2024-12-29 14:00` → `2025-12-30 03:00`, in seine fünf Abschnitte zerlegt und daneben
die ungeteilte Stundenlesung:

| Abschnitt | Ebene | Zeilen | `SUM(anzahl)` | Eimer | Prozesse |
|---|---|---:|---:|---:|---:|
| 1 · 29.12.2024 14:00–24:00 | Stunde | 79 | 286 | 10 | 20 |
| 2 · 30.12.–31.12.2024 | Tag | 356 | 8.309 | 2 | 232 |
| 3 · Januar–November 2025 | Monat | 8.847 | 2.496.435 | 11 | 725 |
| 4 · 01.12.–29.12.2025 | Tag | 6.814 | 208.632 | 29 | 643 |
| 5 · 30.12.2025 00:00–03:00 | Stunde | 52 | 418 | 3 | 22 |
| **zerlegt, Summe** | | **14.148** | **2.714.080** | **55** | |
| **E · ungeteilt** | Stunde | **281.090** | **2.714.080** | 8.771 von 8.773 | 732 |

**Die Zerlegung liest 14.148 Zeilen statt 281.090 — Faktor 19,9 —, und die Summen stimmen schon
mandantenfrei überein.** Die eigentliche Probe je Prozess und Status steht in §34.

> **Abweichung A3 — die Stundenzahl des Bösfalls.** Der Auftrag nennt „55 Zeitscheiben statt
> 8.749 Stunden". Nach dem Kalender umfasst das Fenster **8.773** Stundeneimer (10 + 48 + 8.016 +
> 696 + 3), davon sind **8.771** belegt. Die Zahl im Auftrag ist um einen Tag zu niedrig; sie trägt
> keine Entscheidung und ist hier nur richtiggestellt.
>
> **Abweichung A4 — „die Zeilen, die ein 12‑Monats‑Fenster liest".** Die 280.186 / 100.270 aus
> [`rollup.md`](rollup.md) §2 gelten für Januar bis Dezember 2025. Die 365‑Tage‑Fenster dieser
> Runde beginnen am 30.12.2024 und lesen **280.980 / 100.597** — dieselbe Größenordnung, nicht
> dieselbe Zahl.

### Was je Mandant übrig bleibt — und was der Plan liest

Die Kennzahlenabfrage gibt je `(process_id, message_status)` eine Zeile aus. Gezählt statt aus der
Rohausgabe abgelesen (Sitzung 0, Abschnitt 06, und Sitzung 0b):

| Mandant | Ergebniszeilen 365 T / Bösfall | Rollupzeilen **des Mandanten** im 365‑T‑Fenster, Stunde | dito Tag | in der Zerlegung des Bösfalls behalten |
|---|---:|---:|---:|---:|
| `NEXANS` | 711 / 711 | **130.169** | 65.297 | 11.120 |
| `VOTG` | 40 / 40 | 51.652 | 7.660 | 863 |
| `IBIS` | 114 / 114 | 29.422 | 16.011 | 2.376 |
| `SUTTONS` | 25 / 25 | 56.352 | 5.267 | 761 |

**Die dritte Spalte ist die, an der M147 hängt** — und sie stand vor dieser Runde nirgends. Warum
sie gebraucht wird, sagt der Plan in §31.

---

## 31. M147 — die Stundenebene über wachsende Spannen (Fassung E im Bösfall)

**Das Statement**, `NEXANS` über 365 Tage — das Kennzahlenstatement aus §6, nur mit anderem
Bereich:

```sql
select `overlord_monitor`.`message_rollup`.`process_id`,
       `overlord_monitor`.`message_rollup`.`message_status`,
       sum(`overlord_monitor`.`message_rollup`.`anzahl`)
from `overlord_monitor`.`message_rollup`
where (`overlord_monitor`.`message_rollup`.`stunde` >= timestamp '2024-12-30 03:00:00.0'
   and `overlord_monitor`.`message_rollup`.`stunde` <  timestamp '2025-12-30 03:00:00.0'
   and exists (select 1 as `one`
               from `GlassfishDB`.`Process` as `baum_process`
               join `GlassfishDB`.`ProjectMandant`
                 on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `baum_process`.`ProjectID`
               where (`baum_process`.`ProcessID` = `overlord_monitor`.`message_rollup`.`process_id`
                  and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS')))
group by `overlord_monitor`.`message_rollup`.`process_id`,
         `overlord_monitor`.`message_rollup`.`message_status`
```

### Die Laufzeiten — beste von fünf, in Millisekunden

| Mandant | 24 h | 7 T | 30 T | 90 T | **365 T** |
|---|---:|---:|---:|---:|---:|
| `NEXANS` | 4,511 | 21,065 | 107,691 | 239,126 | **832,090** |
| `VOTG` | 4,410 | 21,511 | 123,612 | 85,670 | **337,603** |
| `IBIS` | 4,454 | 18,229 | 117,828 | 55,546 | **194,514** |
| `SUTTONS` | 4,257 | 24,616 | 52,415 | 124,365 | **345,637** |

**`NEXANS` über 365 Tage: 832,090 ms — 18,9 % über dem Tor von 700 ms.** Alle fünf Läufe liegen
zwischen 832,090 und 850,021 ms; der Aufwärmlauf bei 854,991 ms.

**Und zwei Zeilen, die nicht monoton sind:** `VOTG` braucht über 90 Tage **weniger** als über 30
(85,670 gegen 123,612 ms), `IBIS` ebenso (55,546 gegen 117,828 ms). Das ist kein Messfehler — die
fünf Werte je Fall streuen um höchstens 3 % —, sondern **ein Planwechsel**.

### Die Pläne (Regel L15) — zwei Planfamilien, und die Spanne entscheidet

| Familie | Einstieg | dann | Rolluptabelle |
|---|---|---|---|
| **A — der Bereich** | `message_rollup` `range` über `PRIMARY` (`key_len` 5) | `baum_process` `eq_ref` `PRIMARY` → `ProjectMandant` `eq_ref` `PRIMARY` | liest **alle** Zeilen des Fensters und wirft weg, was nicht zum Mandanten gehört |
| **B — der Mandant** | `ProjectMandant` `ref` über `ProjectMandant_Mandant_idx` | `baum_process` `ref` `Process_ProjectFK` (`Using index`) | `message_rollup` **`ref` über `message_rollup_prozess_idx`**, `Using index condition` — liest **je Prozess des Mandanten** dessen Zeitbereich, geschätzt 459 Zeilen je Prozess |
| C — `SUTTONS`, 24 h | `ProjectMandant` `ref` | `message_rollup` `range` `PRIMARY` mit `Using join buffer (flat, BNL join)` → `baum_process` `eq_ref` | dieselbe Drehung wie in M116 bei `SUTTONS`/12 M |

Welche Familie der Optimierer nimmt, je Fall:

| Mandant | 24 h | 7 T | 30 T | 90 T | 365 T |
|---|---|---|---|---|---|
| `NEXANS` | A | A | **B** | **B** | **B** |
| `VOTG` | A | A | A | **B** | **B** |
| `IBIS` | A | A | A | **B** | **B** |
| `SUTTONS` | C | **B** | **B** | **B** | **B** |

**Der Wechsel auf Familie B ist der Grund, warum die Stundenebene über ein Jahr nicht drei Sekunden
kostet.** Der Index `message_rollup_prozess_idx (process_id, stunde)` aus `V11` — gebaut am
30.08.2026 für die Fensterverengung der Nachrichtenliste, [`rollup.md`](rollup.md) §2 — erlaubt es,
über den Mandanten einzusteigen und **nur dessen Zeilen** zu lesen. Genau das tut der Optimierer,
sobald der Bereich groß und die Mandantenmenge klein genug ist; die Schwelle liegt bei `NEXANS`
zwischen 7 und 30 Tagen, bei `VOTG` und `IBIS` zwischen 30 und 90, bei `SUTTONS` unter 7. Und weil
in Familie B die Zeilen **des Mandanten** gelesen werden und nicht die des Fensters, ist `VOTG` über
90 Tage schneller als über 30: 51.652 Zeilen über den Index gegen 21.263 Zeilen des Bereichs plus
`EXISTS` je Zeile.

**Die Kosten je gelesener Zeile — und jetzt stimmt die Zeilenzahl aus §30:**

| Fall | Plan | gelesene Zeilen | Laufzeit | **µs je Zeile** |
|---|---|---:|---:|---:|
| `NEXANS` 365 T | B | 130.169 (des Mandanten) | 832,090 ms | **6,39** |
| `VOTG` 365 T | B | 51.652 | 337,603 ms | 6,54 |
| `IBIS` 365 T | B | 29.422 | 194,514 ms | 6,61 |
| `SUTTONS` 365 T | B | 56.352 | 345,637 ms | 6,13 |
| `VOTG` 30 T | A | 21.263 (des Fensters) | 123,612 ms | 5,81 |
| `IBIS` 30 T | A | 21.263 | 117,828 ms | 5,54 |
| `NEXANS` 7 T | A | 3.097 | 21,065 ms | 6,80 |

**Alle sieben liegen in den 5,3 bis 11,5 µs aus M94.** Die Kosten hängen an der Zahl gelesener
Zeilen — nur ist *welche* Zeilen der Plan liest keine Eigenschaft des Fensters, sondern der Wahl des
Optimierers.

### Die vorregistrierten Erwartungen

| # | Erwartung | Befund |
|---|---|---|
| **1** | M147 wächst linear mit der gelesenen Zeilenzahl; aus M116 hochgerechnet rund 713 ms, aus den µs‑Kosten 1,5 bis 3,2 s — *„welche trägt, ist genau die offene Frage"* | **trifft zu, mit einer Wendung.** Linear ist es — in den Zeilen, die **der Plan** liest. Die zweite Vorhersage rechnete mit den 280.980 Zeilen des Fensters; gelesen hat Familie B die **130.169 Zeilen des Mandanten**, und 130.169 × 5,3 bis 11,5 µs sind **690 bis 1.497 ms** — 832 ms liegen darin. Die erste Vorhersage (713 ms) rechnete aus einem Tagesebenen-Wert hoch und liegt 14 % unter dem Ergebnis; dass sie so nah liegt, ist Zufall der Zahlen, nicht Bestätigung des Wegs. **Die µs‑Kosten tragen — sobald man die richtige Zeilenzahl einsetzt** |
| **2** | M147 reißt das Tor bei `NEXANS`/365 T | **trifft zu.** 832,090 ms |

> **Belegvermerk (Regel L10).**
> *Gemessen war:* zwanzig Fälle, vier Mandanten × fünf Spannen, warm, Aufwärmlauf und beste von fünf,
> alle bis `2025-12-30 03:00`; je Fall der `EXPLAIN`; die Zeilen je Mandant im 365‑T‑Fenster aus
> Sitzung 0b.
> *Behauptet wird:* Fassung E kostet auf der Stundenebene über ein Jahr beim größten Mandanten
> 832 ms und ist damit über dem Tor; und der Plan kippt ab einer mandantenabhängigen Spanne auf den
> Einstieg über den Mandanten.
> **Die Lücke:** Wo genau die Schwelle liegt, ist **nicht** gemessen — nur, dass sie bei `NEXANS`
> zwischen 7 und 30 Tagen liegt. Sie hängt an den InnoDB‑Statistiken und kann sich mit dem Bestand
> verschieben; auf der Produktion, deren Bestand dichter sein kann, ist sie eine andere. Und die
> µs‑Kosten je Zeile sind auf Familie B **übertragen** — M94 hat sie an Familie A gemessen; dass sie
> hier in denselben Bereich fallen, ist eine Beobachtung an vier Punkten, kein Nachweis.

---

## 32. M148 — die Tagesebene über dieselben Spannen

**Das Statement:** dasselbe wie in §31 mit `message_rollup_tag`, `tag >= date '2024-12-30' and
tag < date '2025-12-30'` — der Text, den der Code heute für `30T` schickt, nur mit anderem Bereich.

### Die Laufzeiten — beste von fünf, in Millisekunden

| Mandant | 1 T | 7 T | 30 T | 90 T | **365 T** |
|---|---:|---:|---:|---:|---:|
| `NEXANS` | 1,993 | 5,817 | 53,421 | 183,014 | **764,233** |
| `VOTG` | 1,602 | 4,641 | 32,993 | 113,155 | **477,748** |
| `IBIS` | 1,914 | 5,194 | 38,199 | 128,647 | **536,110** |
| `SUTTONS` | 1,527 | 4,158 | 30,774 | 105,183 | **430,026** |

**`NEXANS` über 365 Tage: 764,233 ms — 9,2 % über dem Tor.** Die fünf Läufe liegen zwischen 764,233
und 765,910 ms; die Tagesebene misst sich enger als die Stundenebene.

**Der 30‑Tage‑Wert reproduziert M116** (53,421 gegen 53,163 ms; das M116‑Fenster war der Dezember,
dieses der 30.11. bis 29.12. — dieselbe Zeilenzahl auf ein paar Dutzend).

### Die Pläne (Regel L15) — eine Familie, über alle zwanzig Fälle

`NEXANS`, `VOTG`, `IBIS`: **Familie A** bei jeder Spanne — `message_rollup_tag` `range` über
`PRIMARY` (`key_len` 3, geschätzt 171 / 710 / 14.450 / 51.326 / 62.172 Zeilen), dann `baum_process`
`eq_ref` und `ProjectMandant` `eq_ref`. `SUTTONS`: **Familie C** bei jeder Spanne — Einstieg über
`ProjectMandant`, die Tagesebene als `range` mit `BNL`‑Join.

**Familie B gibt es hier nicht, und der Grund steht in [`rollup.md`](rollup.md) §2:** *„Kein
Sekundärindex gilt für die Tagesebene weiterhin."* Ohne einen Index `(process_id, tag)` kann der
Optimierer nicht über den Mandanten einsteigen. Er liest die **100.597 Zeilen des Fensters** und
prüft jede gegen die Mandantenkette — bei jedem Mandanten dieselben Zeilen.

**Die Kosten je gelesener Zeile** (Zeilen des Fensters, weil Familie A sie alle liest):

| Fall | gelesene Zeilen | Laufzeit | µs je Zeile |
|---|---:|---:|---:|
| `NEXANS` 30 T | 6.865 | 53,421 ms | 7,78 |
| `NEXANS` 90 T | 24.060 | 183,014 ms | 7,61 |
| `NEXANS` 365 T | 100.597 | 764,233 ms | **7,60** |
| `VOTG` 365 T | 100.597 | 477,748 ms | 4,75 |
| `IBIS` 365 T | 100.597 | 536,110 ms | 5,33 |
| `SUTTONS` 365 T | 100.597 | 430,026 ms | 4,27 |

**Linear über die Spanne** — `NEXANS` von 30 auf 365 Tage: 14,65‑mal so viele Zeilen, 14,31‑mal so
viel Zeit. Und je Mandant verschieden bei gleicher Zeilenzahl, weil hinter der Kette die Gruppierung
steht: `NEXANS` behält 65.297 der 100.597 Zeilen und gruppiert sie, `SUTTONS` behält 5.267.

### Der Befund, der über den Auftrag hinausweist

**Stundenebene 832 ms, Tagesebene 764 ms — 8 % Unterschied bei 2,8‑mal weniger Zeilen im Fenster.**
Das liegt nicht daran, dass die Tagesebene teuer wäre (7,6 µs je Zeile, mitten in M94), sondern daran,
dass die Stundenebene dank `V11` **weniger Zeilen liest, als im Fenster stehen**, und die Tagesebene
das nicht kann.

**Für das freie Zeitfenster heißt das:** Ein **tagesgenaues** Jahresfenster — beide Grenzen auf
Mitternacht, keine Stunde krumm — läge in Fassung E auf der Tagesebene und kostete beim größten
Mandanten **764 ms**. Fassung E ist damit **nicht nur bei krummen Eingaben** über dem Tor, sondern
bei jedem langen Fenster, das keine Monatsgrenzen trifft. *Was daraus folgt, entscheidet diese Runde
nicht* (offener Punkt 137).

**Die Zahl ist außerdem alt.** [`rollup.md`](rollup.md) §9a hat das Dashboard über die Tagesebene
mit **767,128 ms** gemessen (Verlauf, zwölf Monate, `NEXANS`, 100.270 Zeilen) — 3 ms neben den
764,233 ms hier, über eine andere Abfrage mit einer anderen Gruppierung. Der Preis der Tagesebene
über ein Jahr ist der Preis ihres Bereichszugriffs, gleich, wer ihn bezahlt.

### Die vorregistrierte Erwartung

| # | Erwartung | Befund |
|---|---|---|
| **3** | M148 reißt vermutlich ebenfalls: rund 780 ms bei `NEXANS`. *Falls ja, ist auch ein tagesgenaues freies Jahresfenster ohne Zerlegung nicht tragbar* | **trifft zu.** 764,233 ms, die Schätzung lag 2,1 % daneben. Und der Folgesatz gilt |

> **Belegvermerk (Regel L10).**
> *Gemessen war:* zwanzig Fälle, tagesgenaue Fenster, warm, beste von fünf, je Fall der `EXPLAIN`;
> die Zeilen je Mandant aus Sitzung 0b.
> *Behauptet wird:* Die Tagesebene liest über ein Jahr alle Zeilen des Fensters, weil sie keinen
> Index trägt, der den Einstieg über den Mandanten erlaubte; und ein tagesgenaues Jahresfenster ist
> in Fassung E nicht tragbar.
> **Die Lücke:** Dass ein Index `(process_id, tag)` die Tagesebene auf Familie B brächte, ist aus
> §31 **geschlossen und nicht gemessen** — und ob er gebaut werden sollte, ist eine Frage, die
> [`rollup.md`](rollup.md) §2 ausdrücklich an eine Messung bindet. Sie wird hier gestellt (Punkt 138)
> und nicht beantwortet.

---

## 33. M149 und M150 — die Zerlegung in zwei Bauformen

Der Bösfall aus §30: `2024-12-29 14:00` → `2025-12-30 03:00`, beide Enden krumm, fünf Abschnitte,
alle drei Ebenen beteiligt.

### M149 — Z‑U, **ein** Statement

```sql
select `t`.`process_id`, `t`.`message_status`, sum(`t`.`anzahl`)
from (select `overlord_monitor`.`message_rollup`.`process_id`,
             `overlord_monitor`.`message_rollup`.`message_status`,
             `overlord_monitor`.`message_rollup`.`anzahl`
      from `overlord_monitor`.`message_rollup`
      where ((`overlord_monitor`.`message_rollup`.`stunde` >= timestamp '2024-12-29 14:00:00.0'
          and `overlord_monitor`.`message_rollup`.`stunde` <  timestamp '2024-12-30 00:00:00.0')
          or (`overlord_monitor`.`message_rollup`.`stunde` >= timestamp '2025-12-30 00:00:00.0'
          and `overlord_monitor`.`message_rollup`.`stunde` <  timestamp '2025-12-30 03:00:00.0'))
      union all
      select `overlord_monitor`.`message_rollup_tag`.`process_id`,
             `overlord_monitor`.`message_rollup_tag`.`message_status`,
             `overlord_monitor`.`message_rollup_tag`.`anzahl`
      from `overlord_monitor`.`message_rollup_tag`
      where ((`overlord_monitor`.`message_rollup_tag`.`tag` >= date '2024-12-30'
          and `overlord_monitor`.`message_rollup_tag`.`tag` <  date '2025-01-01')
          or (`overlord_monitor`.`message_rollup_tag`.`tag` >= date '2025-12-01'
          and `overlord_monitor`.`message_rollup_tag`.`tag` <  date '2025-12-30'))
      union all
      select `overlord_monitor`.`message_rollup_monat`.`process_id`,
             `overlord_monitor`.`message_rollup_monat`.`message_status`,
             `overlord_monitor`.`message_rollup_monat`.`anzahl`
      from `overlord_monitor`.`message_rollup_monat`
      where `overlord_monitor`.`message_rollup_monat`.`monat` >= date '2025-01-01'
        and `overlord_monitor`.`message_rollup_monat`.`monat` <  date '2025-12-01') as `t`
where exists (select 1 as `one`
              from `GlassfishDB`.`Process` as `baum_process`
              join `GlassfishDB`.`ProjectMandant`
                on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `baum_process`.`ProjectID`
              where (`baum_process`.`ProcessID` = `t`.`process_id`
                 and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS'))
group by `t`.`process_id`, `t`.`message_status`
```

**Drei Bereichslesungen ohne Mandantenkette in einer Ableitung; die Kette und die Gruppierung stehen
einmal, außen.** Stunden- und Tagesebene tragen je zwei Bereiche (Kopf und Fuß) als `OR` zweier
Intervalle — kein Eimer doppelt, keiner außerhalb des Fensters. **Dieses Statement ist von Hand
gebaut und nicht aus Code gefallen** — den Code gibt es nicht. Was ein Bauauftrag rendert, ist nach
L7 erneut zu messen.

### M150 — Z‑D, **drei** Statements

Jedes ist das Kennzahlenstatement aus §6 mit seiner Ebene; die Prädikate:

| Ebene | Prädikat |
|---|---|
| Stunde | `(stunde >= '2024-12-29 14:00' and stunde < '2024-12-30 00:00') or (stunde >= '2025-12-30 00:00' and stunde < '2025-12-30 03:00')` |
| Tag | `(tag >= '2024-12-30' and tag < '2025-01-01') or (tag >= '2025-12-01' and tag < '2025-12-30')` |
| Monat | `monat >= '2025-01-01' and monat < '2025-12-01'` |

Sechs Runden zu je drei Statements in der Reihenfolge Stunde, Tag, Monat; ausgewertet **je Ebene**
(beste von fünf) und **je Runde** (die Summe der drei, beste von fünf).

### Die Laufzeiten — beste von fünf, in Millisekunden

| Mandant | **M149 · Z‑U** | M150 Stunde | M150 Tag | M150 Monat | **M150 · Σ der Bestwerte** | M150 · beste Runde | Faktor Z‑D / Z‑U |
|---|---:|---:|---:|---:|---:|---:|---:|
| `NEXANS` | **71,055** | 1,912 | 55,884 | 68,877 | **126,673** | 127,342 | 1,78 |
| `VOTG` | **34,753** | 2,190 | 35,522 | 38,727 | **76,439** | 76,659 | 2,20 |
| `IBIS` | **39,527** | 1,599 | 41,135 | 43,685 | **86,419** | 86,621 | 2,19 |
| `SUTTONS` | **33,556** | 1,631 | 34,431 | 36,077 | **72,139** | 72,178 | 2,15 |

**Z‑U ist bei allen vier Mandanten die schnellere Bauform — um Faktor 1,8 bis 2,2.** Und die
M150‑Zahl ist dabei noch **zu klein**: Sie enthält weder die Summierung in Java noch die zwei
zusätzlichen Umläufe zwischen Anwendung und Datenbank. Beides macht Z‑D nur langsamer; die Richtung
des Befunds kann daran nicht kippen.

**Was die drei Z‑D‑Statements einzeln zeigen:** Die Tageslesung über 31 Tage (7.170 Zeilen) kostet
55,884 ms und die Monatslesung über elf Monate (8.847 Zeilen) 68,877 ms — **je 7,8 µs je Zeile**,
dieselben Kosten wie in M116 und M148. Jedes der drei zahlt die Mandantenkette **je Zeile**.

### Die Pläne (Regel L15)

**Z‑U**, Zeile für Zeile gleich bei allen vier Mandanten (nur die Schätzung für `ProjectMandant`
lautet 17 / 39 / 46 / 1):

| id | select_type | table | type | key | rows | Extra |
|---|---|---|---|---|---:|---|
| 1 | PRIMARY | `ProjectMandant` | `ref` | `ProjectMandant_Mandant_idx` | 17 | `Using where; Using index; Using temporary; Using filesort` |
| 1 | PRIMARY | `baum_process` | `ref` | `Process_ProjectFK` | 5 | `Using index` |
| 1 | PRIMARY | **`<derived2>`** | **`ref`** | **`key0`** | 13 | |
| 2 | DERIVED | `message_rollup` | `range` | `PRIMARY` | 131 | `Using where` |
| 3 | UNION | `message_rollup_tag` | `range` | `PRIMARY` | 14.512 | `Using where` |
| 4 | UNION | `message_rollup_monat` | `range` | `PRIMARY` | 6.117 | `Using where` |

**Das ist der Grund für Faktor zwei, und er ist von M114 verschieden.** Die Ableitung wird **einmal
materialisiert** — drei Bereichszugriffe über `PRIMARY`, zusammen 14.148 Zeilen, ohne jede Kette —
und bekommt einen **automatischen Schlüssel** `key0` auf `process_id`. Danach läuft der Plan wie
Familie B aus §31: über den Mandanten einsteigen, je Prozess in die materialisierte Ableitung
greifen. Die Mandantenkette wird **je Prozess** ausgewertet (733 bei `NEXANS`) und nicht je Zeile
(14.148). In M114 stand die Kette in der Ableitung **und** außen und wurde zweimal ausgewertet; hier
steht sie einmal, außen, und der Optimierer hat sie zum Einstieg gemacht.

**Z‑D**: jedes der drei Statements ist **Familie A** — `range` über `PRIMARY` (131 / 14.512 / 6.117
geschätzte Zeilen), dann die `eq_ref`‑Kette — bei `NEXANS`, `VOTG`, `IBIS`; **Familie C** (Einstieg
über `ProjectMandant`, `BNL`‑Join) bei `SUTTONS`. Dieselben Familien wie in M116, und sie sind
gemessen nicht der Engpass: Auch Familie C bleibt bei `SUTTONS` bei 72 ms.

**Was Z‑U nach oben begrenzt, und das folgt aus der Konstruktion:** Ein Fenster von höchstens einem
Jahr zerfällt in höchstens 23 + 23 Stundeneimer, 30 + 30 Tageseimer und 11 Monatseimer. Die
Ableitung materialisiert damit nie mehr als das, was diese 117 Scheiben tragen — auf diesem Bestand
im Bösfall 14.148 Zeilen. *Das ist eine Schranke aus der Bauform, keine gemessene; gemessen ist ein
Fenster.*

### Die vorregistrierten Erwartungen

| # | Erwartung | Befund |
|---|---|---|
| **4** | M149 und M150 bleiben unter 100 ms | **trifft für Z‑U zu** (33,6 bis 71,1 ms) und **für Z‑D bei `NEXANS` nicht** (126,673 ms; die drei anderen 72 bis 86 ms). Die Herleitung — „elf Zwölftel von M116" — war für die Monatslesung richtig (68,9 gegen 75,7 ms), hatte aber die Tageslesung über 31 Tage unterschätzt: Die kostet allein 55,9 ms, so viel wie M116 über 30 Tage |
| **6** | Z‑U ist langsamer als Z‑D, weil die Ableitung dieselbe ist, die M114 mit 98 bis 398 ms gemessen hat | **trifft nicht zu.** Z‑U ist bei allen vier Mandanten um Faktor 1,8 bis 2,2 schneller. Die Ableitung ist **nicht** dieselbe: In M114 stand die Mandantenkette innen und außen; hier steht sie einmal, außen, und der Plan nimmt sie als Einstieg über einen automatischen Schlüssel auf die materialisierte Ableitung |

> **Belegvermerk (Regel L10).**
> *Gemessen war:* ein Fenster (der Bösfall), vier Mandanten, beide Bauformen, warm, beste von fünf
> — M149 als ein Statement, M150 als sechs Runden zu drei Statements, ausgewiesen als **Summe der
> drei SQL‑Laufzeiten**; je Fall die Pläne.
> *Behauptet wird:* Z‑U ist die schnellere Bauform, und beide bleiben weit unter dem Tor.
> **Die Lücke, dreifach:** Die Java‑Summierung und die zwei zusätzlichen Umläufe von Z‑D sind
> **nicht** gemessen — die M150‑Zahl ist eine Untergrenze, und der Abstand zu Z‑U ist in Wahrheit
> größer, nicht kleiner. Z‑U ist ein **von Hand gebautes** Statement, kein gerendertes; Regel L7
> verlangt für die gebaute Fassung eine eigene Messung. Und die Schranke „höchstens 117 Scheiben"
> ist gerechnet, nicht gemessen — ein Fenster ist gemessen, und es ist eines mit fünf Abschnitten.

> **Für den Bauauftrag, ohne hier zu entscheiden:** Z‑U lässt die Zahl der Statements je Aufruf bei
> **zwei** — das Gerüst und eine Kennzahlenabfrage. Was am Text von `ProzessbaumStatementsTest`
> fällt, ist die vierte Eigenschaft aus §6 („jedes Paar liest seine Ebene und keine andere") für das
> freie Fenster; und `ProzessbaumPlanDbIT` müsste für Z‑U die Zugriffsart `<derived2>`/`ref` über den
> automatischen Schlüssel festschreiben — die Reihenfolge nicht, aus demselben Grund wie in §8.

---

## 34. M151 — die Gleichheitsprobe, zerlegt gegen ungeteilt

**Die Frage:** Liefert die Zerlegung über dasselbe Fenster **je `(process_id, message_status)`**
dieselbe Summe wie die ungeteilte Lesung der Stundenebene — nicht nur als Gesamtsumme, die schon in
§30 mandantenfrei übereinstimmt (2.714.080)?

**Die Form:** MariaDB kennt kein `FULL OUTER JOIN`; zwei Anti‑Joins in einem `UNION ALL` leisten
dasselbe. Verglichen wird **über den Schlüssel und über die Summe** — eine Zeile, die auf der anderen
Seite fehlt oder dort eine andere Summe trägt, fällt heraus. Als `LEFT JOIN … IS NULL` und nicht als
`NOT EXISTS`, damit jede Ableitung je Zweig genau einmal ausgewertet wird und die 60‑s‑Grenze nicht in
Gefahr kommt:

```sql
SELECT 'nur in Z-U' AS nur_auf_seite, COUNT(*) AS abweichende_zeilen
FROM (<Z-U, mit sum(...) as summe>) AS l
LEFT JOIN (<E ungeteilt, Stundenebene, mit sum(...) as summe>) AS r
  ON r.process_id = l.process_id AND r.message_status = l.message_status AND r.summe = l.summe
WHERE r.process_id IS NULL
UNION ALL
SELECT 'nur in E ungeteilt', COUNT(*)
FROM (<E ungeteilt>) AS r
LEFT JOIN (<Z-U>) AS l
  ON l.process_id = r.process_id AND l.message_status = r.message_status AND l.summe = r.summe
WHERE l.process_id IS NULL
```

**Zweimal je Mandant:** Z‑U gegen E, und die **Z‑D‑Summe** gegen E — die drei Z‑D‑Statements in
einem `UNION ALL` und darüber `sum(summe)` je Schlüssel. Das ist die Arithmetik, die Java täte, in
SQL nachgebildet; **Java selbst ist nicht gelaufen.**

### Das Ergebnis

| Mandant | Zeilen E / Z‑U / Z‑D‑Summe | Nachrichten E / Z‑U / Z‑D‑Summe | nur in Z‑U | nur in E | nur in Z‑D‑Summe | nur in E |
|---|---|---|---:|---:|---:|---:|
| `NEXANS` | 711 / 711 / 711 | 2.314.856 / 2.314.856 / 2.314.856 | **0** | **0** | **0** | **0** |
| `VOTG` | 40 / 40 / 40 | 114.264 / 114.264 / 114.264 | **0** | **0** | **0** | **0** |
| `IBIS` | 114 / 114 / 114 | 58.960 / 58.960 / 58.960 | **0** | **0** | **0** | **0** |
| `SUTTONS` | 25 / 25 / 25 | 196.510 / 196.510 / 196.510 | **0** | **0** | **0** | **0** |

**Null abweichende Zeilen in allen acht Proben.** Die Zerlegung ist auf diesem Bestand über dieses
Fenster exakt — je Prozess, je Rohstatus, in der Summe.

### Die vorregistrierte Erwartung

| # | Erwartung | Befund |
|---|---|---|
| **5** | M151 findet null abweichende Zeilen | **trifft zu** |

> **Belegvermerk (Regel L10).**
> *Gemessen war:* ein Fenster mit fünf Abschnitten über alle drei Ebenen und beide Randarten, vier
> Mandanten, beide Bauformen, je `(process_id, message_status, summe)` in beide Richtungen.
> *Behauptet wird:* Die Zerlegung ist exakt.
> **Die Lücke:** Was diese Probe **trägt**, ist die Konstruktion aus [`rollup.md`](rollup.md) §5 —
> die Tagesebene ist die Summe ihrer Stundeneimer, die Monatsebene die Summe ihrer Tageseimer, alle
> drei entstehen in **einer** Transaktion und tragen den gesamten Bestand. Dass die Ebenen
> zueinander stimmen, ist damit Sache des Laufs und seiner Tests (`RollupDbIT`), nicht dieser Probe.
> Was die Probe **hinzufügt**, ist der Nachweis, dass die **Schnittführung** des Bösfalls stimmt —
> kein Eimer doppelt, keiner ausgelassen, an allen vier Übergängen. Das gilt für dieses Fenster; für
> jedes andere ist es die Arithmetik der Fensterzerlegung, und die gehört in einen Test des
> Bauauftrags, nicht in eine Messung. Und die Z‑D‑Summe ist in SQL nachgebildet; ob der Java‑Code,
> der sie einmal rechnet, dasselbe tut, ist eine Frage an dessen Test.

---

## 35. Was diese Runde nicht zeigt

- **Keine Messung gegen die Produktion.** Alle Zahlen stammen von der Testkopie mit ihrem Bestand
  von 22 Monaten und rund 7.300 Nachrichten je Tag im dichten Teil. Ein dichterer Bestand liest je
  Fenster mehr Zeilen, und die Schwelle des Planwechsels aus §31 liegt dort anderswo.
- **Alle Werte sind warm.** `FLUSH TABLES` steht `monitor_read` nicht zu, und der Puffer fasst die
  drei Rolluptabellen um ein Vielfaches ([`rollup.md`](rollup.md) §9a). Der Kaltfaktor 9,66 aus M44
  ([`messungen-schritt7.md`](messungen-schritt7.md)) ist eine **Übertragung** aus einer anderen
  Abfrage über eine andere Tabelle: Auf
  832 ms angewandt wären es 8,0 s, auf 764 ms 7,4 s, auf 71 ms 0,69 s — alle unter der 10‑s‑Grenze
  des Lese‑Pools, und keine dieser drei Zahlen ist gemessen. **Genau dieser Abstand ist der Grund,
  warum das Tor bei 700 ms steht und nicht bei 1,0 s.**
- **Die Java‑Summierung in Z‑D ist nicht gemessen**, ebenso wenig die zwei zusätzlichen Umläufe. Die
  M150‑Zahl ist eine Untergrenze.
- **Ein Bösfall, ein Bestand.** M149 bis M151 sind über **ein** Fenster gefahren. Dass es alle drei
  Ebenen und beide Randarten enthält, macht es zum schwersten Fall der Bauform, nicht zum einzigen.
- **Z‑U ist von Hand gebaut.** Es gibt keinen Code, der es rendert; die Messung nach L7 für die
  gebaute Fassung steht dem Bauauftrag bevor. Die vier Z‑D‑Statements dagegen sind Zeichen für
  Zeichen der gerenderte Text mit anderem Prädikat.
- **Das Gerüst ist nicht neu gemessen.** Es kennt kein Fenster (§6) und ändert sich mit dem freien
  Zeitfenster nicht.
- **Die Schwelle des Planwechsels ist nicht bestimmt** — nur eingegrenzt (§31).
- **Nichts über die Oberfläche**, nichts über den Endpunkt, nichts über die Eingabe. Der Auftrag hat
  beides ausdrücklich ausgeschlossen; E3‑a und E1′‑b stehen unverändert.

### Offene Punkte aus dieser Runde

| # | Punkt |
|---|---|
| **137** | **Ein tagesgenaues Jahresfenster ist ohne Zerlegung nicht tragbar** (M148, 764 ms). Fassung Z gilt damit nicht nur für krumme Eingaben, sondern für jedes lange Fenster, das keine Monatsgrenzen trifft. Ob die Regel aus Fassung E — „die gröbste Ebene, auf deren Grenzen beide Enden fallen" — als Sonderfall innerhalb von Z stehen bleibt oder ob Z immer zerlegt, ist im Bauauftrag zu entscheiden; gemessen ist nur, dass E allein nicht reicht |
| **138** | **Die Tagesebene trägt keinen Sekundärindex** und kann deshalb nicht über den Mandanten gelesen werden (§32). Ob ein Index `(process_id, tag)` sie auf Familie B brächte, ist **nicht gemessen**; [`rollup.md`](rollup.md) §2 bindet einen solchen Index an eine gemessene Frage. Unter Fassung Z liest kein Fenster mehr als 60 Tageseimer, und dann stellt sich die Frage nicht — sie stellt sich nur, wenn jemand die Tagesebene über lange Bereiche lesen will |

---

## 36. Der Ausgang des Tors

Die Lesart aus dem Auftrag (§7), vor der Messung festgeschrieben:

| Ausgang | Bedingung | gemessen |
|---|---|---|
| 1 | M151 findet auch nur eine abweichende Zeile | **nein** — null Zeilen, vier Mandanten, acht Proben (§34) |
| 2 | M147 bleibt bei 365 T und `NEXANS` unter 700 ms | **nein** — 832,090 ms (§31) |
| **3** | M147 reißt, die schnellere von M149/M150 bleibt darunter | **ja** — M149 (Z‑U) 71,055 ms (§33) |
| 4 | beide reißen | nein |

**Ausgang 3: Fassung Z, in der Bauform Z‑U.**


---

## 37. Das freie Zeitfenster, gebaut (10c‑4b, 07.09.2026)

Der Bauauftrag zur Messrunde §30 bis §36. **Gebaut ist Ausgang 3 des Tors: Fassung Z in der
Bauform Z‑U** — ein Statement, dessen Ableitung bis zu drei Rollup-Ebenen mit `UNION ALL`
zusammenführt. Die Wahl ist gemessen (M147 bis M151) und in diesem Schritt nicht mehr aufgemacht
worden.

### Nummernvergabe (Teil 5)

| | |
|---|---|
| **Entscheidungen** | **E‑92 bis E‑98.** `grep -rnoE 'E‑9[2-9]\|E‑1[0-9]{2}'` → ein Treffer, und das ist der Satz *„E‑92 bleibt frei"* in §30. E‑91 steht in `dashboard-frontend.md` und `README.md` — der Stand des Auftrags stimmt |
| **Messungen** | **M152.** `grep -rnoE '\bM15[2-9]\b'` über `docs/`, `scripts/`, Backend und Frontend → kein Treffer; M151 ist die höchste vergebene |
| **Offene Punkte** | ab **139**. `grep -rnE '\*\*(139\|14[0-9])\*\*' docs/` → Treffer sind `key_len`‑Werte (146, 148) und Zeilenzahlen (140), kein Punkt; 138 ist der höchste (§35) |

**Alle drei Nummernstände des Auftrags haben gestimmt.**

### Der Rahmen

| | |
|---|---|
| **Reihenfolge der Commits** | Der Auftrag nannte den Endpunkt vor den Kennzahlen. Gebaut ist umgekehrt — erst das Repository mit `UNION ALL`, dann der Endpunkt —, weil der Dienst für ein freies Fenster die Vereinigung schon braucht; ein Endpunkt, der auf ein Repository trifft, das nur ein Segment kennt, wäre ein Zwischenstand gewesen, der kompiliert und falsch antwortet. Die Inhalte der sieben Commits sind die des Auftrags |
| **Die tragende Zusage, vorab gepinnt** | Bevor die Zerlegung gebaut wurde, sind die Texte aller **drei** Paare Zeichen für Zeichen in `ProzessbaumStatementsTest` festgehalten worden — `30T` und `12M` hatten bis dahin nur Teilprüfungen. Der Test lief gegen den unveränderten Code grün und läuft nach dem Bau grün: **Die drei Paare rendern denselben Text wie vorher** |
| **Datenbank** | Testkopie, Profil `dev`, Anker `2025-12-30 04:09:47`. Nichts geschrieben außer den Testkonten der `DbIT`s, die sich selbst wegräumen |

---

## 38. Der Vertrag (E‑94, E‑95)

```
GET /api/prozesse/baum?zeitraum={48H|30T|12M}
GET /api/prozesse/baum?von=<ISO,UTC>&bis=<ISO,UTC>
```

Die Ergänzung von §1 in Kurzform: Die beiden Modi schließen einander aus (`zeitfenster-mehrdeutig`,
keine stille Vorrangregel — dieselbe Festlegung wie in [`nachrichtenliste.md`](nachrichtenliste.md)
§2). Kein Mandantenparameter, in keiner Form; der freie Modus ist keine neue benannte Ausnahme.

### E‑94 — `bis` ist in der Anfrage einschließend, `fenster.bis` in der Antwort ausschließend

**In der Anfrage** ist `bis` die letzte enthaltene Stunde — beidseitig geschlossen wie `von`/`bis`
der Nachrichtenliste. **In der Antwort** bleibt `fenster.bis` ausschließend, wie seit 10c‑1. Das
Backend rechnet `bisAusschliessend = bis + 1 Stunde`, in der Zone der Anwendungsuhr
(`Baumfenster.ausAnfrage`) — nicht im Browser, weil eine Stunde am Umstellungstag keine Stunde ist.
Der Grund für die Asymmetrie ist, dass jede Seite die Konvention ihrer Nachbarn hält; sie steht in
§1 und nicht nur in einem Kommentar. `von = bis` ist erlaubt und heißt *ein Stundeneimer*.

### E‑95 — Ein nicht stundengenaues Fenster wird abgewiesen, nicht gerundet

`zeitfenster-zu-genau`, der einzige neue Code. Nach unten runden weitete das Fenster (`von`)
beziehungsweise beschnitte es (`bis`); beides verstieße gegen *alles kommt aus dem abgefragten
Zeitraum, und alles aus ihm kommt vor*. Die Oberfläche lässt den Zustand über `step=3600` gar nicht
erst entstehen; die Prüfung fängt die von Hand gebaute Adresse. **Geprüft wird der in die Zone der
Anwendungsuhr umgerechnete Wert** — `BaumfensterTest.Anfrage` hält das an `Asia/Kolkata` fest, wo
`08:30Z` die volle Wanduhrstunde ist und `08:00Z` nicht.

### Die sieben Fehlerfälle, in der Reihenfolge der Prüfung

| # | `type` | Wann |
|---|---|---|
| 1 | `zeitraum-unbekannt` | `zeitraum` ist kein Code der drei Paare — unverändert |
| 2 | `zeitpunkt-ungueltig` | ein Zeitpunkt ist nicht als ISO‑Zeitpunkt lesbar — `Zeitpunkte.ausIso`, unverändert |
| 3 | `zeitfenster-mehrdeutig` | `zeitraum` **und** `von`/`bis`, auch neben einem halben Fenster |
| 4 | `zeitfenster-unvollstaendig` | nur einer der beiden Zeitpunkte (ein leerer Wert gilt als nicht angegeben) |
| 5 | `zeitfenster-zu-genau` | ein Zeitpunkt liegt nicht auf einer vollen Stunde der Anwendungszone |
| 6 | `zeitfenster-ungueltig` | `von` liegt hinter `bis` |
| 7 | `zeitfenster-zu-gross` | `von.isBefore(bisAusschliessend.minusYears(1))` — ein Kalenderjahr, gerechnet mit dem ausschließenden Ende |

Die Reihenfolge folgt der Liste: Dort werden die Zeitpunkte in `NachrichtenFilter.aus` gelesen,
bevor `Zeitfenster.aufloesen` die Modi prüft. **Ein Fenster in der Zukunft ist kein Fehler** — es
liefert Nullen, und `ProzessbaumIsolationDbIT.fehlerfaelle_und_zukunft` hält beides fest.

> ⚠️ **Befund: Das Fenster aus M149 ist am Endpunkt zu groß.** `2024-12-29 14:00` bis
> `2025-12-30 03:00` (ausschließend) umfasst 365 Tage und **13 Stunden** — Abweichung A3 in §30 hat
> die 8.773 Stundeneimer schon gezählt. Mit der Regel aus Fall 7 ist das `zeitfenster-zu-gross`;
> der Auftrag hat denselben Bösfall für die Sichtprüfung und für M152 „durch den Endpunkt"
> vorgesehen. **Gelöst, nicht stillschweigend:** Am Repository bleibt das M149‑Fenster messbar und
> ist es (`ProzessbaumGleichheitDbIT.boesfall_zerlegt_gleich_ungeteilt`, M151 als Test); am
> Endpunkt tragen Isolationstest und M152 einen **um einen Tag kürzeren** Bösfall — `2024-12-30
> 14:00` bis einschließlich `2025-12-30 02:00` —, der dieselben fünf Segmente über alle drei Ebenen
> hat, mit einem Tagessegment am Kopf, das einen statt zwei Tage trägt. Ob die Jahresgrenze des
> Baums am einschließenden `bis` hängen sollte (dann wäre ein Jahr *plus* die letzte Stunde
> erlaubt), ist offener Punkt **139** und nicht hier entschieden.

---

## 39. Die Zerlegung (E‑92, E‑93)

### Der Typ: `common/Baumfenster`, und `Rollupzeitraum` bleibt bei drei Werten

`FREI` ist **kein** vierter Wert von `Rollupzeitraum`: Er hätte weder ein `fenster(jetzt)` noch eine
Ebene, und das vollständige `switch` ohne `default` in den Repositories (offener Punkt 113) verlöre
genau die Eigenschaft, die es trägt. Stattdessen `Baumfenster` in `common`, das **beides** aufnimmt
— eines der drei Paare oder ein freies Fenster — und in beiden Fällen `segmente(jetzt)` liefert:

| Eingang | Ausgang |
|---|---|
| eines der drei Paare | **genau ein** Segment, auf der Ebene des Paares (`Rollupzeitraum.ebene()`), mit dem heutigen Fenster |
| ein freies Fenster | die Zerlegung, **ein bis fünf** Segmente — unabhängig von jeder Uhr |

Dazu `common/Rollupebene` (`STUNDE`, `TAG`, `MONAT`) als **Name** der Ebene. Die Zuordnung Name →
generierte Tabelle bleibt im Repository; Punkt 113 ist damit **fortgeschrieben und nicht
geschlossen** (§43).

### E‑93 — Die drei Paare bleiben unzerlegt

Ein 48‑Stunden‑Fenster reicht über zwei Tage und würde als freies Fenster in drei Segmente zerlegt
(`BaumfensterTest.Paare.achtundvierzig_stunden_bleiben_ungeteilt`). Als Paar bleibt es **ein**
Segment: Es ist gemessen (M116) und gebaut, und sein Text ist die tragende Zusage dieses Schritts.
**Das ist eine bewusste Ungleichbehandlung**, und `ProzessbaumGleichheitDbIT` hält fest, dass beide
Wege je `(process_id, message_status)` dieselben Zahlen liefern.

### E‑92 — Das freie Fenster wird immer zerlegt

Nicht nur bei krummen Eingaben: M148 hat gezeigt, dass auch ein tagesgenaues Jahresfenster auf der
Tagesebene das Tor reißt (764,233 ms). Die Regel aus Fassung E — *die gröbste Ebene, auf deren
Grenzen beide Enden fallen* — steht **nicht** als Sonderfall in Z; **Punkt 137 ist damit
beantwortet.** Was die Zerlegung bei einem monatsbündigen Fenster liefert, ist ohnehin ein
einzelnes Monatssegment, und das rendert die ungeteilte Bauform (§40).

### Der Algorithmus — über den Kalender, nicht über Dauern

`Baumfenster.zerlegung(von, bisAusschliessend)`: Der erste ganze Tag beginnt an der nächsten
Tagesgrenze (oder an `von`), der letzte endet an der Tagesgrenze, die `bisAusschliessend` nicht
überschreitet; dazwischen ebenso der erste und der letzte ganze Monat (`YearMonth`). Was leer
bleibt, entsteht nicht — **zwei benachbarte Bereiche derselben Ebene sind einer.** Deshalb ist ein
Fenster über Mitternacht ohne ganzen Tag ein einziges Stundensegment, und 30 ganze Tage über eine
Monatsgrenze hinweg sind ein einziges Tagessegment — nicht Kopf und Fuß getrennt.

| Eingang | Zerlegung |
|---|---|
| innerhalb eines Tages, auch über Mitternacht ohne ganzen Tag | **ein** Stundensegment |
| genau ein ganzer Tag, auch am Monatsanfang und am Monatsende | **ein** Tagessegment |
| genau ganze Monate | **ein** Monatssegment |
| 30 ganze Tage, nicht monatsbündig | **ein** Tagessegment |
| der Bösfall aus M149 | **fünf** Segmente: 10 Stunden, 2 Tage, 11 Monate, 29 Tage, 3 Stunden — 8.773 Stundeneimer, die Zahl aus Abweichung A3 |
| die beiden Umstellungstage | je **ein** Tagessegment; in `Europe/Berlin` hat der Tag 23 bzw. 25 Stunden, und `plusHours(24)` auf einem Zeitpunkt mit Zone träfe `01:00` bzw. `23:00` statt Mitternacht |

**Zwei Eigenschaften über 231 erfundene Fenster** (22 Grenzpunkte — krumme Stunden, Mitternacht,
Monatsanfang, Monatsende, Jahreswechsel, Schaltjahr, beide Umstellungstage — paarweise):
lückenlos, überschneidungsfrei, jede Grenze auf einer Eimergrenze ihrer Ebene, keine zwei
benachbarten Segmente derselben Ebene, und die Stundeneimer aller Segmente sind genau die des
Fensters. Dazu die Schranke der Bauform aus §33, je Ebene nachgezählt: **höchstens 46 Stunden‑, 60
Tages‑ und 12 Monatseimer** für ein Fenster bis ein Jahr — §33 nannte 30 + 30 Tageseimer und 11
Monate; die 60 sind Kopf plus Fuß in einem Segment, die 12 ein monatsbündiges Jahr.

Alle Prüfwerte sind Kalenderarithmetik über erfundenen Zeitpunkten (Regel T2), und keine Uhr wird
gelesen (Regel T1).

---

## 40. Das Statement — Z‑U, gerendert (E‑97)

**Ein Statement für die Kennzahlen, wie heute; weiterhin zwei je Aufruf.** `ProzessbaumRepository.kennzahlen(mandant, segmente)`:

- **Ein Segment** — jedes Paar, und jedes freie Fenster, das die Zerlegung in einem Segment lässt —
  rendert die **ungeteilte** Bauform: Zeichen für Zeichen den Text von vor diesem Schritt.
- **Mehrere Segmente** führen ihre Bereichslesungen in einer Ableitung `t` mit `UNION ALL`
  zusammen, je Ebene **ein** Zweig in der festen Reihenfolge Stunde, Tag, Monat, dessen Bereiche als
  `OR` nebeneinanderstehen; darüber **eine** Gruppierung und **eine** Mandantenkette als `EXISTS`.
  **Leere Ebenen erzeugen keinen Zweig.**

Der Bösfall, so wie der Code ihn rendert (`ProzessbaumStatementsTest.FreiesFenster.boesfall_woertlich`;
mit `?` statt der Literale ist es Zeichen für Zeichen das handgebaute Statement aus M149):

```sql
select `t`.`process_id`, `t`.`message_status`, sum(`t`.`anzahl`)
from (select `overlord_monitor`.`message_rollup`.`process_id`,
             `overlord_monitor`.`message_rollup`.`message_status`,
             `overlord_monitor`.`message_rollup`.`anzahl`
      from `overlord_monitor`.`message_rollup`
      where ((`overlord_monitor`.`message_rollup`.`stunde` >= ? and `overlord_monitor`.`message_rollup`.`stunde` < ?)
          or (`overlord_monitor`.`message_rollup`.`stunde` >= ? and `overlord_monitor`.`message_rollup`.`stunde` < ?))
      union all
      select `overlord_monitor`.`message_rollup_tag`.`process_id`,
             `overlord_monitor`.`message_rollup_tag`.`message_status`,
             `overlord_monitor`.`message_rollup_tag`.`anzahl`
      from `overlord_monitor`.`message_rollup_tag`
      where ((`overlord_monitor`.`message_rollup_tag`.`tag` >= ? and `overlord_monitor`.`message_rollup_tag`.`tag` < ?)
          or (`overlord_monitor`.`message_rollup_tag`.`tag` >= ? and `overlord_monitor`.`message_rollup_tag`.`tag` < ?))
      union all
      select `overlord_monitor`.`message_rollup_monat`.`process_id`,
             `overlord_monitor`.`message_rollup_monat`.`message_status`,
             `overlord_monitor`.`message_rollup_monat`.`anzahl`
      from `overlord_monitor`.`message_rollup_monat`
      where (`overlord_monitor`.`message_rollup_monat`.`monat` >= ? and `overlord_monitor`.`message_rollup_monat`.`monat` < ?)) as `t`
where exists (select 1 as `one`
              from `GlassfishDB`.`Process` as `baum_process`
              join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `baum_process`.`ProjectID`
              where (`baum_process`.`ProcessID` = `t`.`process_id` and `GlassfishDB`.`ProjectMandant`.`MandantID` = ?))
group by `t`.`process_id`, `t`.`message_status`
```

### E‑97 — Eigenschaft 4 aus §6 wird ersetzt, E‑42 bleibt

| | Stand |
|---|---|
| 1. Letzte Bewegung über die Deckelung, nicht `MAX()` | **unverändert** |
| 2. Mandantenkette im Gerüst Join, in den Kennzahlen `EXISTS` | **unverändert** — und in der Vereinigung **einmal, außen**, auf der Ableitung; nicht in jedem Zweig (das wäre M114 mit doppelter Auswertung) |
| 3. Keine Funktion um die Schlüsselspalte der Rollup-Ebene | **unverändert, für jeden Zweig einzeln** — der `DATE`‑Wert für `tag` und `monat` wird in Java geschnitten, nicht in SQL |
| 4. *Jedes Paar liest seine Ebene und keine andere* | **ersetzt** durch: *Jedes **Segment** liest seine Ebene und keine andere, und es steht keine Ebene im Text, die kein Segment trägt.* Für die drei Paare sagt das dasselbe wie bisher |

**E‑42 fällt nicht.** Es sind weiterhin zwei Statements je Aufruf — Gerüst und Kennzahlen —, auch
im freien Fenster; `ProzessbaumStatementsTest` hält das für die Paare und für den Bösfall fest. Der
Auftrag zu 10c‑4a hatte anderes angekündigt; die Korrektur stammt vom Auftraggeber.

Was der Text sonst noch hält: Derselbe Fensterschnitt ergibt denselben Text, gleich in welcher
Reihenfolge die Segmente ankommen; Kopf und Fuß derselben Ebene stehen als `OR` zweier Intervalle
in **einem** Zweig; gruppiert wird einmal, über der Ableitung, ohne `ORDER BY`; ein monatsbündiges
Jahr rendert denselben Text wie `12M`.

### Der Plantest

`ProzessbaumPlanDbIT` schreibt für drei Fensterschnitte — den Bösfall, krumme 30 Tage, ein
monatsbündiges Jahr — bei allen vier Mandanten fest: **je Zweig ein `range` über `PRIMARY` der
jeweiligen Ebene**, keine Ebene im Plan, die kein Segment trägt, die Mandantenkette über Index und
kein `Message` im Plan. **Nicht festgeschrieben:** die Materialisierung (`<derived2>`), der
automatische Schlüssel (`key0`) und die Reihenfolge. Sie sind der Grund für Faktor zwei gegenüber
Z‑D (§33) — aber sie sind eine Entscheidung des Optimierers, und Annahme A10 führt ein Upgrade auf
MariaDB 11 als offenes Risiko. Ein Test, der sie festschriebe, würde an dem Tag rot, an dem sich
nichts Fachliches geändert hat; die Beobachtung steht in §33, nicht in der Zusicherung — dieselbe
Überlegung wie bei der Reihenfolge in `DashboardPlanDbIT`. Der Kommentar steht im Test.

---

## 41. Die Oberfläche (E‑96)

| | |
|---|---|
| **Der vierte Knopf** | `components/zeitraum-umschalter.tsx` bekommt ihn über die **freiwillige** Angabe `aufFrei`. Ohne sie sind es drei Knöpfe, und das Dashboard ruft ihn ohne — `tests/zeitraum-umschalter.test.tsx` hält beides gerendert fest, samt der Verdrahtung: „Frei" ruft `aufFrei` und nicht `aufAuswahl` |
| **Die Datumsfelder** | `features/nachrichten/components/baumfenster-felder.tsx`, **neben** dem Umschalter. Zwei `datetime-local` mit `step=3600`; `validity.badInput` an `keyup` und `blur`, übernommen aus `filterleiste.tsx` und nicht neu gefunden |
| **Die Codes** | `lib/rollupzeitraum.ts`: `FREI` als Code der Antwort (`Baumzeitraum`), **kein** viertes Paar in `ROLLUPZEITRAEUME`; dazu `Baumfensterzustand`, `baumfenstermodus`, `angezeigterBaumfenstermodus`, `mitPaar`, `mitFreiemBaumfenster`, `hervorgehobenerBaumzeitraum`, `baumfensterAlsParameter` — **nachgebaut aus `lib/filter.ts`, nicht importiert**; dort liegen die Zeitraumcodes der Liste, eine andere Menge |
| **Die Beschriftungen** | `texte.zeitraum`: `FREI`, `von`, `bis`, `freiHinweis`, `unvollstaendig`, `beideNoetig`; `fehler["zeitfenster-zu-genau"]`. Beide Sprachen, `sprachdateien.test.ts` ohne neue Ausnahme |

### Zustand und URL

| | in der URL | Verlauf |
|---|---|---|
| `zeitraum` | nur die ausdrückliche Wahl (E‑n) | `replace` |
| `von`, `bis` | **ja**, sobald sie stehen — auch einzeln, denn das Backend prüft | `replace` |
| „frei gewählt, noch nichts eingetragen" | **nein** — im Komponentenzustand `freiGewaehlt` | — |

**Die beiden Modi löschen einander in der Oberfläche**: `setzeZeitraum` schreibt `von`/`bis` auf
`null`, `setzeFreiesFenster` schreibt `zeitraum` auf `null`. `zeitfenster-mehrdeutig` ist über die
Bedienung nicht erreichbar; käme es doch, steht es **über** der Ansicht, weil es ein Befund wäre —
`baumfensterFehler` in `prozessansicht.ts` führt es ausdrücklich nicht.

**Die Abfrage ist der Schlüssel.** `baumabfrage(zustand)` liefert `?zeitraum=…`, `?von=…&bis=…`
oder nichts, und `useProzessbaum` hängt daran; die leere Abfrage ist weiterhin ein eigener Schlüssel
und nicht der des gewählten Paares.

**Eine von Hand gebaute Adresse mit beiden Modi wird als freies Fenster gelesen.** Stehen
`zeitraum` und `von`/`bis` zugleich in der URL, schickt `baumfensterAlsParameter` nur `von`/`bis`
— dieselbe Regel wie `zeitfensterAlsParameter` in `lib/filter.ts` für die Liste, und der Grund ist
derselbe: Der Modus der URL ist „frei", sobald ein Zeitpunkt steht. `zeitfenster-mehrdeutig` ist
damit auch über die Adresse nicht erreichbar; das Backend hält die Prüfung trotzdem, für jeden
anderen Aufrufer. Gesehen in der Sichtprüfung (§45).

### Wo die Meldungen stehen — und was dabei stehen bleibt

`zeitfenster-unvollstaendig`, `zeitfenster-ungueltig`, `zeitpunkt-ungueltig`, `zeitfenster-zu-genau`
und `zeitfenster-zu-gross` stehen an den Datumsfeldern, in der Reihenfolge der Liste: halb getippt
schlägt alles andere, dann die Antwort des Servers, dann der fehlende zweite Zeitpunkt. **Die Prüfung
bleibt im Backend.**

**Der letzte gelieferte Baum bleibt dabei stehen** (`letzterBaum` in `prozessansicht.tsx`): Wer
zwischen „Von" und „Bis" tippt, bekommt `zeitfenster-unvollstaendig`; ihm dafür den Baum
wegzunehmen hieße, die Ansicht zu leeren, weil er noch nicht fertig ist. Dieselbe Bauform wie
`letzteSeite` in `useNachrichtenSeite` — ausdrücklich gehalten und nicht über `placeholderData`.
Beim allerersten Aufruf über eine fehlerhafte Adresse gibt es keinen letzten Baum; dann steht nur
der Hinweis an den Feldern (offener Punkt 141).

**Was die Oberfläche nicht tut:** Sie rechnet kein Fenster aus, rundet nichts, zeigt keine Ebene
und keine Korrektur. `fenster` aus der Antwort geht unverändert an die Übertragungsliste (E‑50),
samt der dort benannten Ungenauigkeit.

---

## 42. Tests, Messung M152 und Regelbezug

### Die Tests

| Test | Was er hält |
|---|---|
| `BaumfensterTest` (32 Fälle) | die Grenzfälle aus §39, die Eigenschaften über 231 Fenster, die Schranke der Bauform, beide Umstellungstage, die drei Paare als ein Segment, das freie Fenster ohne Uhr; die sieben Fehlerfälle einzeln, die volle Stunde nach der Zonenumrechnung, `von = bis` als ein Eimer, `bis + 1 h` am Umstellungstag, die Zukunft als Nullen |
| `ProzessbaumStatementsTest` (24) | **die drei Paare wörtlich** — die tragende Zusage —; der Bösfall wörtlich; keine Ebene ohne Segment; ein Segment ungeteilt; keine Funktion in keinem Zweig; die Kette einmal außen; Kopf und Fuß als `OR`; feste Reihenfolge; eine Gruppierung; genau zwei Statements, auch im freien Fenster; kein `Message` |
| `ProzessbaumServiceTest` (30) | `FREI` und das Fenster in UTC in der Antwort, die durchgereichten Segmente, die Verdichtung wie bei einem Paar |
| `ProzessbaumPlanDbIT` (10, `db`) | §40 |
| `ProzessbaumGleichheitDbIT` (3, `db`) | frei gegen Paar je `(process_id, message_status)` am Repository und am Endpunkt (derselbe Rumpf bis auf den Code); der Bösfall aus M149 zerlegt gegen ungeteilt, vier Mandanten — **M151 als Test** |
| `ProzessbaumIsolationDbIT` (15, `db`) | um den freien Modus erweitert: keine fremde Kennung im Rumpf, die vereinigte Kette am Repository, `?mandant=…` wirkungslos, derselbe Umfang, `400` mit Typ und die Zukunft mit Nullen |
| `tests/prozessansicht.test.ts` (+8), `tests/zeitraum-umschalter.test.tsx` (2) | §41; `pnpm check` grün mit 934 Fällen in 35 Dateien |

Keine Wanduhrzeit in einer Zusicherung (T1); kein Erwartungswert aus dem Bestand (T2) — die
`DbIT`s vergleichen zwei Lesungen desselben Bestands und verlangen nur, dass sie nicht leer sind.

### M152 — am gebauten Endpunkt, beste von fünf, in Millisekunden

`MessungM152DbIT`, dieselbe Form wie M117: vier Mandanten, je Fall ein Aufwärmlauf und fünf Läufe,
zwei Zahlen — **durch den Endpunkt** (HTTP‑Umlauf im Testclient samt Sitzung und Serialisierung)
und **am Dienst** (`ProzessbaumService.baum` im selben Prozess: Zerlegung, beide Statements,
Zusammensetzen). Die Zeilen stehen unverändert in
`scripts/messung-prozessansicht-frei/ergebnis/m152-endpunkt.gefiltert.txt`.

| Mandant | 48H (Paar) | 12M (Paar) | Jahr monatsbündig · 1 Segment | 30 Tage krumm · 3 | Bösfall · 5 |
|---|---:|---:|---:|---:|---:|
| `NEXANS` durch den Endpunkt | 65,872 | 125,584 | **120,554** | 71,480 | 111,784 |
| `NEXANS` am Dienst | 31,447 | 98,411 | 96,309 | 51,682 | 90,723 |
| `VOTG` durch den Endpunkt | 36,401 | 72,146 | 71,806 | 44,560 | 63,635 |
| `VOTG` am Dienst | 18,613 | 53,220 | 53,038 | 26,642 | 46,775 |
| `IBIS` durch den Endpunkt | 32,700 | 72,577 | 72,767 | 44,165 | 64,024 |
| `IBIS` am Dienst | 14,426 | 55,741 | 55,065 | 26,477 | 47,488 |
| `SUTTONS` durch den Endpunkt | 22,446 | 55,044 | 56,056 | 33,231 | 52,954 |
| `SUTTONS` am Dienst | 9,222 | 41,751 | 41,741 | 18,527 | 37,348 |

**Vorregistriert war:** Bleibt die teuerste Fassung beim größten Mandanten unter 150 ms durch den
Endpunkt, trägt der Bau. **Sie bleibt darunter: 120,554 ms** (`NEXANS`, monatsbündiges Jahr) —
der Bösfall mit fünf Segmenten liegt bei 111,784 ms, das Paar `12M` bei 125,584 ms. **Der Bau
trägt.**

Drei Beobachtungen, keine Zusicherungen:

- **Das monatsbündige Jahr und `12M` sind derselbe Text mit anderen Werten** — und dieselbe
  Nachrichtenzahl (`NEXANS` 2.308.005, die Summe aus M113‑Z), auf 3 ms dieselbe Zeit.
- **Der Bösfall ist am Dienst 90,723 ms** gegen 71,055 ms in M149 für das SQL allein; die Differenz
  ist Zerlegung, JDBC, das Gerüst (3,4 ms in M116) und das Zusammensetzen von 733 Blättern. Durch
  den Endpunkt kommen rund 20 ms dazu, bei jedem Fall, gleich welcher Größe — HTTP, Sitzung und
  155 KiB Rumpf.
- **Die Gegenprobe der Paare ist keine Zahl, sondern ein Text.** M116 hat SQL‑Laufzeiten über
  `SET profiling` gemessen; die Zahlen hier enthalten Java und HTTP und sind damit **nicht**
  gegen M116 zu halten. Dass die drei Paare nicht teurer geworden sind, folgt daraus, dass ihr
  Statementtext byteidentisch ist (`ProzessbaumStatementsTest`) — ein unveränderter Text kostet
  auf demselben Bestand dasselbe.

> **Belegvermerk (Regel L10).**
> *Gemessen war:* zwanzig Fälle, vier Mandanten × fünf Fensterschnitte, warm, im Testclient auf
> demselben Rechner wie der Server, Aufwärmlauf und beste von fünf; je Fall zusätzlich der Dienst im
> selben Prozess.
> *Behauptet wird:* Das freie Fenster kostet durch den gebauten Endpunkt beim größten Mandanten
> höchstens 121 ms und liegt damit unter der vorregistrierten Schranke.
> **Die Lücke:** Der Testclient ist nicht der Browser — M123 hat 60 ms für `48H` im Browser
> gesehen, hier sind es 65,872 ms im Java‑Client; die Größenordnung stimmt, der Vergleich ist
> keiner. Kein Fall ist kalt gemessen (§35), und die Sitzung schreibt auf der Testkopie bei jeder
> Anfrage mit — ob das in den rund 20 ms zwischen Dienst und Endpunkt steckt, ist nicht getrennt
> erhoben.

### Regelbezug

| Regel | Wo |
|---|---|
| **L1** | Ein Kalenderjahr als Maximum, kein Fenster ohne beide Grenzen; der Befund zu M149 (§38) |
| **L7** | M152 an der gebauten Fassung; die Pläne in `ProzessbaumPlanDbIT` (**L15**) |
| **L10** | Belegvermerke in §42 |
| **M1, M3, M4** | kein Mandantenparameter; die Kette im Statement, einmal außen; `ProzessbaumIsolationDbIT` um den freien Modus erweitert |
| **Q4** | keine Ebene, keine Korrektur, nichts Geratenes in der Oberfläche |
| **T1, T2** | keine Laufzeit und kein Bestandswert in einer Zusicherung |
| **Z1** | ein Uhrenschlag je Anfrage im Dienst; ein freies Fenster hängt an keiner Uhr; die Zone der Anwendungsuhr für die eine Umrechnung |
| **§8 der Frontend-Grundlagen** | `von`/`bis` in der URL, sobald sie stehen; der Zwischenzustand nicht |

---

## 43. Die Entscheidungen dieser Runde, und die offenen Punkte

| Nr. | Entscheidung | Datum |
|---|---|---|
| **E‑92** | Das freie Fenster wird **zerlegt**, Bauform **Z‑U** — gemessen (M147 bis M151), nicht gewählt; immer, auch monatsbündig (§39) | 07.09.2026 |
| **E‑93** | Die **drei Paare bleiben unzerlegt**; ein Paar ergibt genau ein Segment, und sein Text bleibt byteidentisch (§39) | 07.09.2026 |
| **E‑94** | **`bis` ist in der Anfrage einschließend**, `fenster.bis` in der Antwort ausschließend; die Stunde dazwischen rechnet das Backend (§38) | 07.09.2026 |
| **E‑95** | Ein nicht stundengenaues Fenster wird **abgewiesen**, nicht gerundet — `zeitfenster-zu-genau`, geprüft nach der Zonenumrechnung (§38) | 07.09.2026 |
| **E‑96** | Der vierte Knopf ist **freiwillig**, die Datumsfelder stehen **neben** dem Umschalter (§41) | 07.09.2026 |
| **E‑97** | **Eigenschaft 4 wird ersetzt**, E‑42 bleibt (§40) | 07.09.2026 |
| **E‑98** | **Kein Index auf der Tagesebene** in diesem Schritt: Z‑U macht ihn auf dem kritischen Pfad überflüssig — kein Fenster liest mehr als 60 Tageseimer —, und §9c von [`rollup.md`](rollup.md) hat gemessen, was ein Sekundärindex den nächtlichen Volllauf kostet | 07.09.2026 |

### Offene Punkte

| | |
|---|---|
| **113** *(fortgeschrieben)* | Die Ebenenzuordnung steht weiterhin zweimal — in `DashboardRepository` Paar → Tabelle, in `ProzessbaumRepository` seit heute `Rollupebene` → Tabelle. `common/Rollupebene` trägt nur den **Namen** der Ebene; die Tabellen sind nicht nach `common` gehoben, weil sie generiert sind. Das vollständige `switch` ohne `default` steht an beiden Stellen |
| ~~**137**~~ | **Beantwortet am 07.09.2026 durch E‑92:** Z zerlegt immer; die Regel aus Fassung E steht nicht als Sonderfall. Ein monatsbündiges Fenster ergibt ohnehin ein Segment |
| **138** | **bleibt offen** (E‑98). Die Frage stellt sich unter Z‑U auf dem kritischen Pfad nicht; sie stellt sich, wenn jemand die Tagesebene über lange Bereiche lesen will |
| **139** | **Das Fenster aus M149 übersteigt das Kalenderjahr des Endpunkts um 13 Stunden** (§38). Die Grenze hängt am ausschließenden Ende (`bisAusschliessend.minusYears(1)`), wie der Auftrag sie nennt; am einschließenden `bis` gerechnet wäre ein Jahr plus die letzte Stunde erlaubt. Welche Lesart gilt, entscheidet der Auftraggeber; bis dahin misst M152 einen um einen Tag kürzeren Bösfall mit denselben fünf Segmenten |
| ~~**140**~~ | ~~**Keine Sichtprüfung im Browser** für den freien Modus.~~ **Nachgeholt am 07.09.2026, §45** — angemeldet, am laufenden System, mit `NEXANS` und `IBIS`: vier Knöpfe, die Felder mit `step=3600`, die halbe Eingabe am Feld, der stehende Baum, die Abfrage mit `von`/`bis`, die Liste mit dem Fenster der Antwort, das Dashboard mit drei Knöpfen, keine Konsolenmeldung. Was die Sichtprüfung **nicht** zeigen konnte, steht dort |
| **141** | **Beim allerersten Aufruf über eine fehlerhafte Adresse steht kein Baum**, nur der Hinweis an den Feldern — es gibt keinen letzten Baum, der stehen bleiben könnte. Ob dort ein Leerzustand hingehört oder der Baum der Vorgabe, ist nicht entschieden |

---

## 44. Was dieser Schritt nicht zeigt

- **Keine Messung gegen die Produktion.** Alle Zahlen stammen von der Testkopie; die Schwelle des
  Planwechsels aus §31 liegt dort anderswo, und was die Materialisierung der Ableitung auf einem
  dichteren Bestand kostet, ist nicht gemessen.
- **Alle Werte sind warm** (§35). Kein Fall ist kalt gemessen; der Kaltfaktor aus M44 bleibt eine
  Übertragung.
- **Die Bauform Z‑U ruht auf einer Entscheidung des Optimierers** — Materialisierung und
  automatischer Schlüssel —, die Annahme A10 (MariaDB 11) gefährdet. Der Plantest schreibt sie
  absichtlich nicht fest; wer das Upgrade fährt, misst M152 neu.
- **Punkt 138 ist nicht beantwortet**, sondern absichtlich offen gelassen (E‑98).
- **M152 ist im Java‑Testclient gemessen, nicht im Browser**, und die Paare sind darin nicht gegen
  M116 vergleichbar; ihre Zusage ist der Text.
- **Der Bösfall aus M149 ist durch den Endpunkt nicht gemessen** — er ist dort `zeitfenster-zu-gross`
  (Punkt 139); gemessen ist ein um einen Tag kürzeres Fenster mit derselben Form.
- **Nichts im Browser** (Punkt 140): weder der vierte Knopf noch die Felder noch die Meldungen an
  ihnen sind am laufenden System gesehen worden.

---

## 45. Die Sichtprüfung am laufenden System (07.09.2026, nachgeholt)

Angemeldet als `admin`, Chrome über die Erweiterung, Backend im Profil `dev` mit dem Stand von
Commit `accfd6c`, Frontend `next dev`. Das Backend lief seit dem Morgen mit dem Code von vor diesem
Schritt und ist neu gestartet worden — die JVM lädt nicht nach.

### Was zu sehen war, in dieser Reihenfolge

| Schritt | Beobachtet |
|---|---|
| `/prozesse`, `NEXANS` | vier Knöpfe: 48 Stunden (gedrückt), 30 Tage, 12 Monate, **Frei**; keine Felder |
| Klick auf „Frei" | zwei `datetime-local` mit `step="3600"` neben dem Umschalter, Hinweis *Volle Stunden; „Bis" ist die letzte enthaltene Stunde.*; „Frei" gedrückt; **die Adresse bleibt `/prozesse`**, keine neue Abfrage, der Baum zeigt weiter die 48 Stunden (`ACOME` 20) |
| „Von" halb getippt | `validity.badInput = true`, Hinweis *Bitte Datum und Uhrzeit vollständig eintragen.* — der Zustand, den das Feld nicht über seinen Wert mitteilt, kommt an |
| „Von" vollständig (`30.12.2024 14:00`) | Adresse `?von=2024-12-30T13:00:00.000Z`; Abfrage `baum?von=…` → **400** (einmal wiederholt); Hinweis am Feld *Ein freies Zeitfenster braucht beide Zeitpunkte.* — die übersetzte Antwort des Servers —; **der Baum bleibt stehen** (`ACOME` 20, Kopfzeile unverändert); keine Meldung über der Ansicht |
| „Bis" vollständig (`30.12.2025 02:00`) | Adresse mit `von` und `bis`; Abfrage `baum?von=2024-12-30T13:00:00.000Z&bis=2025-12-30T01:00:00.000Z` → **200**; `ACOME` 29.569; Hinweis zurück auf den Standardtext |
| die Antwort dazu, per `fetch` | `zeitraum: "FREI"`, `fenster.von 2024-12-30T13:00:00Z`, `fenster.bis 2025-12-30T02:00:00Z` (ausschließend: 03:00 Wanduhrzeit), `gesamt.nachrichten` **2.309.634** — die Zahl aus M152 für denselben Bösfall, 155 Partner |
| Prozess gewählt | Liste rechts mit `nachrichten?von=2024-12-30T13:00:00.000Z&bis=2025-12-30T02:00:00.000Z&prozess=…` — **das Fenster der Antwort**, E‑50; Kopf *Zeitraum 30.12.2024, 14:00 bis 30.12.2025, 03:00* |
| Klick auf „30 Tage" | Adresse `?zeitraum=30T` **ohne** `von`/`bis`, Felder weg, „30 Tage" gedrückt, Abfrage `baum?zeitraum=30T` |
| Adresse von Hand, `von=…13:30Z` | Abfrage → **400** `zeitfenster-zu-genau`; Felder zeigen `14:30`; „Frei" gedrückt |
| Adresse von Hand, das Fenster aus M149 | Abfrage → **400** `zeitfenster-zu-gross` (Punkt 139) |
| Adresse von Hand, `zeitraum=48H` **und** `von`/`bis` | die Oberfläche schickt nur `von`/`bis` → 200, „Frei" gedrückt — die Regel aus §41; kein `zeitfenster-mehrdeutig` |
| tiefer Link `?von=…&bis=…&prozess=90300_SAP_KOMMUNIKATION` (`NEXANS`, krumme 30 Tage) | Baum offen bis zur Zeile *SAP Kommunikation 96.758*, Liste mit 50 Zeilen, erste `29.12.2025, 13:45:14` — im Fenster —, Kopf *Zeitraum 29.11.2025, 14:00 bis 29.12.2025, 14:00* |
| `/` (Übersicht) | **drei** Knöpfe, keine Felder |
| Konsole | keine Fehler, keine Ausnahme, über alle Schritte |

### Was die Sichtprüfung nicht zeigen konnte, und woran es lag

- **Die Meldung am Feld bei einer fehlerhaften Adresse am ersten Aufruf.** Bei `zeitfenster-zu-genau`
  und `zeitfenster-zu-gross` über die Adresse blieb die Ansicht im Ladeskelett, obwohl das Backend
  zweimal mit `400` geantwortet hatte. **Das ist die Umgebung, nicht der Bau:** Die unveränderte
  Nachrichtenliste mit nur `von` (`/nachrichten?von=…`) bleibt in derselben Browsersteuerung genauso
  im Skelett, ein `404` ohne Wiederholung (`/nachrichten/gibt-es-nicht`) erscheint sofort — der
  Befund vom 14.08.2026: TanStack Query hält den wiederholten Versuch an (`fetchStatus: paused`).
  Auf der bereits geladenen Seite, beim Tippen, ist derselbe Fehlertyp angekommen (Zeile 4 oben).
  Punkt **141** bleibt offen.
- **Die Tastatureingabe in ein `datetime-local`** ist über die Steuerung segmentweise; das
  Jahresfeld nimmt sechs Ziffern und rückt nicht von selbst weiter. Das ist eine Eigenschaft der
  Steuerung und sagt nichts über die Bedienung von Hand.
- **Der aktive Mandant der Sitzung wechselte während der Prüfung** zwischen `NEXANS` und `IBIS` — die
  Sitzung ist geteilt, ein anderer Tab hat gewechselt. Eine Liste, die zu einem Baum des einen
  Mandanten mit der Sitzung des anderen geladen wird, ist leer (*Nichts im Zeitraum*); das ist kein
  Befund dieses Schritts, sondern derselbe wie beim Mandantenwechsel überhaupt. Die Kette
  Baum → Liste ist deshalb einmal **atomar** in einem Aufruf geprüft worden (Mandant vorher und
  nachher `NEXANS`): 96.758 im Baum, 50 Zeilen und `hasMore` in der Liste.
- **Nichts über `step=3600` als Bedienung:** Ob die Pfeiltasten im Feld stundenweise springen, ist
  nicht nachgefahren; die von Hand gebaute krumme Stunde wird abgewiesen (Zeile 8).
