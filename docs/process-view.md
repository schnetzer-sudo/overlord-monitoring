# Prozessansicht — der Baum, Backend

Entsteht in **Schritt 10c‑1** (02.09.2026). Auftrag: „Schritt 10c‑1: Prozessansicht, Backend",
Stand 01.09.2026.

**Backend, keine Oberfläche.** Der Frontend-Teil ist 10c‑2 und wird erst geschrieben, wenn die
Messwerte hier vorliegen — wie groß der Baum je Mandant tatsächlich ist, entscheidet über
Vorklappen, Ladeverhalten und Virtualisierung. §8 ist die Antwort darauf.

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
```

| Parameter | Werte | Vorgabe |
|---|---|---|
| `zeitraum` | `48H`, `30T`, `12M` | **`48H`** (E‑38) — die Antwort nennt das gewählte Paar |

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
  "zeitraum": "48H",                        // das gewaehlte Paar, immer gesetzt
  "fenster": { "von": "…Z", "bis": "…Z" },  // die gelesenen Grenzen, UTC, bis ausschliessend
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
| **107** | **Bei `IBIS` ordnet der Baum kaum noch:** 139 Gruppen auf 192 Prozesse, größte Gruppe vier. Ob eine Baumdarstellung dafür die richtige Bauform ist, ist eine Gestaltungsfrage für 10c‑2 (§9) |
| **108** | **`VOTG` trägt keine einzige kuratierte Richtung.** Die mittlere Ebene besteht dort aus 133 Knoten „nicht ermittelt". Ob 10c‑2 die Ebene dann einklappt, überspringt oder stehen lässt, ist nicht entschieden (§9) |
| **109** | **Der Rumpf ist bei `NEXANS` 150,3 KiB.** Was das Rendern kostet, ist nicht gemessen — das ist 10c‑2 (§9) |
| **110** | **Die Antwort unterscheidet bei der Richtung nicht zwischen „gepflegt und leer" und „offen".** Dass die Unterscheidung gebraucht würde, ist **nicht gemessen**; ein drittes Katalogfeld entsteht deshalb nicht (§2) |
| **111** | **Die Feldnamen der Antwort sind deutsch, Richtlinie §5.2 schreibt englisch.** Das Dashboard hat sich schon anders entschieden; diese Datei folgt den Nachbarn. Ob die Regel nachzuziehen oder das Projekt zurückzudrehen ist, ist eine eigene Runde (§1) |
| **112** | **Überfälligkeit je Prozess ist nicht gemessen.** Wer den Punkt aufmacht, misst nicht die Zahl der Überfälligen, sondern den **Plan** einer Gruppierung je Prozess — und stellt daneben, was der Endpunkt dann an Bauform mitbekäme: ein drittes Statement, das als einziges live liest, samt Teilerfolg-Mechanismus (§5) |
| **113** | **Die Ebenenzuordnung Paar → Rolluptabelle steht zweimal im Code** — in `DashboardRepository` und in `ProzessbaumRepository`. Sie ließe sich nicht nach `common` heben, ohne die generierten Tabellen dorthin mitzunehmen. Das vollständige `switch` ohne `default` macht die Doppelung compilergesichert, aber sie bleibt eine Doppelung |

---

## 14. Was dieser Schritt nicht zeigt

1. **Keine Oberfläche.** Das ist 10c‑2, und die Zahlen in §9 sind die Vorlage dafür.
2. **Keine Sichtprüfung im Browser.** Es gibt nichts zu sehen; der Endpunkt ist über
   `MessungM117DbIT` und die Isolationstests abgenommen und nicht über eine Ansicht.
3. **Keine Messung gegen die Produktion.** Alle Zahlen stammen von der Testkopie mit 3,34 Millionen
   Zeilen in `Message` und 335.610 im Rollup. Die Projektbeschreibung rechnet mit bis zu 36
   Millionen; die Rolluptabelle wächst mit, und die Kennzahlenabfrage liest sie im Bereich.
4. **Kein Filter, keine Suche im Baum.** Bei 733 Blättern ist das eine berechtigte Frage — sie
   gehört zu 10c‑2, wo auch entschieden wird, ob örtlich oder serverseitig gefiltert wird
   (dieselbe Abwägung wie in [`prozessauswahl.md`](prozessauswahl.md) §9).
5. **Kein Einstieg aus dem Baum in die Liste.** Der Prozessfilter der Übertragungsliste existiert
   ([`nachrichtenliste.md`](nachrichtenliste.md) §1), und der Baum liefert die `processId`, die er
   braucht. Verdrahtet ist nichts — das ist eine Oberflächenentscheidung.
