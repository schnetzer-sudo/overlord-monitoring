# Dashboard — die Landingpage

Stand: 31.08.2026 · Schritt 10b‑2 Teil D · **Backend, keine Oberfläche**

Der eine Endpunkt, aus dem die Landingpage entsteht. Er liest die drei Rollup-Ebenen aus
[`rollup.md`](rollup.md), ordnet die Rohwerte über `MessageStatusClassifier` ein
([`message-status.md`](message-status.md)) und hängt den Prozess-Katalog an
([`prozess-katalog.md`](prozess-katalog.md)).

**Gebaut ist das Backend.** Kein Diagramm, keine Kachel, kein Umschalter — die Oberfläche ist ein
eigener Schritt. Was hier steht, ist die Antwort und ihre Begründung.

**Keine Migration.** Höchste Version bleibt `V12`.

---

## 1. Der Endpunkt

```
GET /api/dashboard?zeitraum={48H|30T|12M}&verteilung={PARTNER|RICHTUNG}
```

| Parameter | Werte | Vorgabe |
|---|---|---|
| `zeitraum` | `48H`, `30T`, `12M` | **keine** — der Endpunkt wählt selbst (§3) und **nennt das gewählte Paar in der Antwort** |
| `verteilung` | `PARTNER`, `RICHTUNG` | `PARTNER` |

**Kein Mandantenparameter, in keiner Form** (Regel M1). Der Mandant kommt aus der Sitzung. Ein
`?mandant=…` ist kein Fehler, sondern wirkungslos — `DashboardIsolationDbIT` hält das fest. **Dieser
Endpunkt ist keine neue benannte Ausnahme**; die drei, die es gibt, stehen in
[`mandantentrennung.md`](mandantentrennung.md) §3 und definieren allesamt eine *Berechtigung*, statt
einen Datenausschnitt abzufragen.

**Kein Eintrag in `SecurityConfig`, und das ist richtig.** Der Pfad fällt unter
`anyRequest().authenticated()`. Eingetragen wird dort nur, wer eine **Rollengrenze** braucht — der
Katalog etwa, weil er `ADMIN` verlangt.

> ### Ein Aufruf, eine Antwort
>
> **Kein Block wird nachgeladen.** Das ist keine Bequemlichkeit, sondern das Leistungsbudget: Sechs
> Anfragen mit je einer Sitzungsprüfung und je einem Verbindungsgriff kosten mehr als sieben
> Abfragen auf einer Verbindung — und auf der Testkopie schreibt jede Anfrage zusätzlich die
> Sitzung fort. Zwei Tests halten es fest: einer auf der Antwort (alle Blöcke sind da), einer auf
> den Statements (es sind genau sieben).

### Die Antwort

```jsonc
{
  "zeitraum": "48H",                       // das gewaehlte Paar, immer gesetzt
  "fenster":  { "von": "…Z", "bis": "…Z" },// die gelesenen Grenzen, UTC, bis ausschliessend
  "leer":     false,                       // der Leerzustand (§6)
  "verlauf":  [ { "eimer": "…Z", "gesamt": 9,
                  "einordnungen": [ { "einordnung": "ABGESCHLOSSEN", "anzahl": 8 } ] } ],
  "kacheln": {
    "nachrichten": 9950,
    "fehler":      { "anzahl": 50,
                     "arten": [ { "rohwert": "ERROR_TIMEOUT", "art": "TIMEOUT", "anzahl": 49 } ] },
    "ueberfaellig":{ "imFenster": 1, "insgesamt": 538, "ermittelbar": true }
  },
  "verteilung": { "sicht": "PARTNER",
                  "zeilen": [ { "art": "WERT", "wert": "…", "anzahl": 8608, "enthaltene": null },
                              { "art": "UEBRIGE", "wert": null, "anzahl": 31, "enthaltene": 14 },
                              { "art": "NICHT_ZUGEORDNET", "wert": null, "anzahl": 490,
                                "enthaltene": null } ] },
  "zuletztAufgefallen": [ { "messageId": "…", "zeitpunkt": "…Z", "status": "ERROR_TIMEOUT",
                            "statusKind": "FEHLER", "kategorie": "FEHLER",
                            "processId": "…", "sosName": "…" } ],
  "stand": { "beendetAm": "…Z", "art": "VOLL" }
}
```

---

## 2. Die sieben Blöcke

| # | Block | Quelle | Statements |
|---|---|---|---:|
| 1 | **Verlauf** je Eimer, nach Einordnung | Rollup-Ebene des Paares × Mandantenkette | 1 |
| 2 | **Kachel Nachrichten** | *derselbe Lesevorgang wie 1* | — |
| 3 | **Kachel Fehler** samt Aufschlüsselung nach Art | *derselbe Lesevorgang wie 1* | — |
| 4 | **Kachel Überfällig** — zwei Zahlen | **live** über `Message` | 2 |
| 5 | **Verteilung** — Partner oder Richtung | Rollup-Ebene × Mandantenkette × `process_catalog` | 1 |
| 6 | **Zuletzt aufgefallen** | `Message`, je Merkmal ein Statement (§7a) | 2 |
| 7 | **Stand** — Zeitpunkt und Laufart | `rollup_lauf` | 1 |

### Die Einordnung entsteht beim Lesen

**Im Rollup steht der Rohwert** (Entscheidung E‑g): `FINISHED`, nicht `ABGESCHLOSSEN`. Die Kategorie
bildet `MessageStatusClassifier.einordnung(rohwert)` — **gerufen, nicht nachgebaut**
([`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §4.1). Ein zweites `switch` über Statuswörter
liefe beim nächsten neuen Statuswert von der Liste weg, und der Unterschied fiele erst auf, wenn
jemand zwei Zahlen nebeneinanderlegt.

**Ein unbekannter Rohwert fällt nach `UNGEKLAERT`** und in keinen bekannten Eimer (Regel Q4) — und
ein unbekannter Wert **mit** `ERROR_`-Präfix nach `FEHLER`, weil dieselbe Regel in SQL gilt.

**Je Eimer nur die Einordnungen, die vorkommen.** Alle acht je Eimer wären bei 48 Stunden 384
Einträge, die meisten null. Die Reihenfolge ist die der Aufzählung und damit über alle Eimer
dieselbe — eine Oberfläche, die Farben nach Position vergibt, bekäme sonst in jedem Balken eine
andere.

### Die Fehlerarten kommen aus demselben Rohwert

`MessageStatusClassifier.fehlerart(rohwert)`, ebenfalls an genau einer Stelle:

| Rohwert | `art` |
|---|---|
| `ERROR_DUPLICATE` | `DUPLICATE` — der Namensteil hinter dem Präfix |
| `COMMIT_REJECTED` | **`Vom Partner abgelehnt`** — der feste Text aus §4.2 |
| alles andere | **der Rohwert, unverändert** (Regel Q4) |

**Der Rohwert steht daneben und wird nicht ersetzt.** Ohne ihn wäre *„Vom Partner abgelehnt"* eine
Zeichenkette, an der sich nichts mehr festmachen ließe — keine Übersetzung, kein Link, kein Filter.

**Kein zweiter Lesevorgang.** Genau deshalb gruppiert Block 1 nach *Rohstatus* und nicht schon nach
Einordnung.

> **„Unquittiert" ist nicht gebaut** (Entscheidung E‑d vom 24.08.2026) — kein Feld, kein
> Platzhalter, keine leere Liste. Zwei Tests halten das fest, einer über die Feldnamen der Records
> und einer über den Antwortrumpf. `COMMIT_REJECTED` gehört ohnehin nicht dorthin: **Das ist eine
> Quittung, nur eine negative.**

### Die Fenstergrenzen liegen auf Eimergrenzen

Beide Grenzen liegen auf einer **Eimergrenze**, die obere ist der Anfang des *nächsten* Eimers und
**ausschließend**. Der angebrochene Eimer, in dem `jetzt` liegt, gehört dazu — genauso setzt
`RollupFenster.ausgedehnt` sein Fenster, und genauso hat M94 gemessen.

**Das unterscheidet sich absichtlich vom Listen-Endpunkt.** Der löst `zeitraum` auf die Sekunde
genau gegen die Anwendungsuhr auf. Für eine Liste ist das richtig; für ein Diagramm wäre es falsch:
Der erste und der letzte Balken wären angebrochen und würden trotzdem so hoch gezeichnet wie ein
ganzer.

### Die Kachel *Nachrichten* zählt Aktivität und nicht Nachrichten

> ### ⚠️ Bekannte Grenze 3
>
> `message_rollup` gruppiert nach **`MessageLastUpdate`**. Wechselt eine Nachricht ihren Status,
> wandert sie in einen anderen Eimer — sie verschwindet dabei aus dem alten, **doppelt gezählt wird
> also nichts**, aber sie erscheint in der Zählung eines Zeitraums, in dem sie nicht entstanden ist.
>
> **Der sichtbare Fall ist der nächtliche Sprung:** Ein Batchlauf, der tausend alte Nachrichten
> anfasst, hebt den Balken der Nachtstunde, ohne dass eine einzige neue Nachricht eingegangen wäre.
>
> **Nicht gebaut wird dagegen etwas.** Die Alternative wäre eine zweite Rolluptabelle über
> `MessageCreated` — eine vierte Ebene, ein zweiter Lauf und eine zweite Wahrheit, zwischen denen
> die Oberfläche wählen müsste. Der Rollup zählt, was sich bewegt hat; das ist für ein Monitoring
> die brauchbarere Größe.

---

## 3. Das Standardfenster richtet sich nach dem Mandanten

Ohne `zeitraum` wird das **erste** Paar der Reihe 48 h → 30 Tage → 12 Monate genommen, das **beide**
Bedingungen erfüllt:

1. **Anteil belegter Eimer ≥ 50 %**
2. **und mindestens ein Eimer mit mehr als fünf Nachrichten**

**Die 50 % sind aus M95 abgeleitet und nicht gewählt:** `NEXANS`, `SUTTONS` und `VOTG` liegen bei
allen drei Paaren auf 100 %; `IBIS` und `IBISGUS` fallen bei 48 Stunden auf 37,50 % und 27,08 %. Der
Sprung liegt damit nicht zwischen groß und klein, sondern beim **verstreutesten** Verkehr — `IBIS`
hat 63 Prozesse auf 235 Nachrichten, `VOTG` 14 auf 399. Genau den soll die Ansicht nicht als
Diagramm mit Lücken zeigen.

**Gesucht wird der Reihe nach und nicht in einem Statement.** Der Normalfall — ein Mandant mit
Verkehr — ist nach der ersten, kleinsten Abfrage entschieden; nur wer bei 48 Stunden durchfällt,
kostet eine zweite. Drei Belegungsproben auf einmal kosteten **immer** auch die teuerste, und die
liest die Monatsebene.

**Mit ausdrücklich genanntem `zeitraum` entfällt die Probe ganz** — ein Aufruf mit Parameter kostet
also *weniger* als einer ohne.

> ### ⚠️ Bekannte Grenze 1: Bedingung 2 ist in der Praxis wirkungslos
>
> Sie sollte `WOC` fangen: **29 von 30 Tagen belegt bei 117 Nachrichten**, also knapp vier am Tag —
> ein Diagramm mit Punkten, das nichts zeigt. **Weil EDI-Verkehr stoßweise ist, liegt aber mit
> Sicherheit ein Tag über fünf, und `WOC` besteht die Bedingung.** Gemessen: Der größte Tageseimer
> von `WOC` liegt deutlich darüber.
>
> **Der Auftraggeber hat das am 31.08.2026 in Kenntnis dieser Folge so entschieden.** Es steht hier
> als bekannte Grenze und wird **nicht nachgebessert**: Eine Schwelle, die `WOC` sicher fängt, finge
> auch Mandanten mit echtem, aber dünnem Verkehr — und die hätten dann kein Diagramm, obwohl es
> etwas zu sehen gäbe.
>
> Damit ist offener Punkt **61** aus [`messungen-schritt10b.md`](messungen-schritt10b.md)
> geschlossen: Die Festlegung ist getroffen, beide Bedingungen sind gebaut, und die zweite ist als
> gewählte und nicht gemessene Zahl benannt.

---

## 4. Die Verteilung

**Ein Block, zwei Sichten** (Partner ⇄ Richtung) über **dasselbe Statement**, nur mit einer anderen
Katalogspalte im Ausdruck. Die Eimerbreite des Paares spielt keine Rolle — gruppiert wird über das
ganze Fenster —, die Fensterbreite schon.

### Die drei Zeilenarten

| `art` | Was | Wann |
|---|---|---|
| `WERT` | ein benannter Partner beziehungsweise eine benannte Richtung | Rang 1 bis 10 |
| `UEBRIGE` | alles ab Rang 11, mit `enthaltene` = wie viele | **nur, wenn es einen Rang 11 gibt** |
| `NICHT_ZUGEORDNET` | Entscheidung E‑i | **immer, auch bei null** |

**„Nicht zugeordnet" erscheint auch bei null**, und das ist eine Aussage über den **Katalog**: Null
heißt *alles kuratiert*, und `IBIS` liefert sie gerade. Wird die Zeile bei null ausgeblendet, ist
*vollständig gepflegt* nicht mehr von *diese Ansicht zeigt das nicht* zu unterscheiden.

**„Übrige" fehlt ohne Rang 11** — eine Null ist dort reines Rangartefakt und sagt nichts.

**Beide Restzeilen stehen immer unten**, unabhängig von ihrer Größe. Bei `IBIS` wäre „Übrige (40)"
mit 27,92 % sonst der größte Balken des Blocks und stünde auf Rang 1, als gäbe es einen Partner
dieses Namens (M98, Befund 21).

> Damit ist offener Punkt **62** geschlossen: Der Block verträgt fehlende Restzeilen — bei drei von
> vier in M98 gemessenen Mandanten gibt es nicht drei Zeilen —, und die Reihenfolge ist so gebaut,
> dass eine große Restzeile nicht nach vorn rutscht.

### Was das Backend nicht tut: beschriften

Die Antwort trägt **keinen** Anzeigetext für die Restzeilen. „nicht zugeordnet" und „Übrige (40)"
sind Beschriftungen; das Backend stellt fest, die Oberfläche beschriftet (Regel Q4). Die Texte
stehen bereits in `frontend/src/i18n/de.ts` und `en.ts`.

### Entscheidung E‑i, als ein Ausdruck

Zugeordnet ist ein Prozess nur, wenn **alle drei** Bedingungen halten; fällt eine, ist der Wert
*nicht zugeordnet*:

1. Es gibt eine Katalogzeile (der `LEFT JOIN` traf).
2. Sie ist `GEPFLEGT` — ein offener Regelvorschlag ist eine Vermutung und keine Zuordnung.
3. Das Feld ist gefüllt — *„gepflegt mit leerem Partner"* heißt **hingesehen, es gibt keinen** (E4)
   und fällt fachlich mit *nicht zugeordnet* zusammen.

**Der `pflegestatus`-Riegel steht auch in der Richtungssicht.** Heute ist er dort folgenlos — nach
der Kuratierung tragen alle Zeilen mit Richtung `GEPFLEGT` —, aber die Regel ist E‑i und nicht der
Zufall dieses Katalogstands.

> ### Zwei Fallen, und beide sind im Statement entschärft
>
> **`LEFT JOIN` und nie `JOIN`.** `WOC` hat keine einzige Katalogzeile; ein innerer Join verlöre
> seine vier Prozesse stillschweigend — und damit ausgerechnet die Zeilen, die als *nicht
> zugeordnet* erscheinen müssten.
>
> **Der `CASE` steht als *ein* Ausdruck in `SELECT`, `GROUP BY` und `ORDER BY`, ohne Alias.** Das
> ist Befund 11 der Vorrunde: MariaDB löst `GROUP BY` **zuerst gegen Tabellenspalten** auf und erst
> danach gegen Ausdrucksaliasse. Hieße der Alias `partner`, gruppierte die Datenbank still nach
> `c.partner` statt nach dem Ausdruck — bei `NEXANS` und `SUTTONS` fällt das nicht auf, bei `VOTG`
> zerfiel der Eimer in **acht** Zeilen. `DashboardStatementsTest` hält beides fest.

**`ORDER BY (schluessel IS NULL), summe DESC`** — *nicht zugeordnet* ist keine Rangposition und
fällt nie in „Übrige". Die Ränge 1…k gehören damit lückenlos den benannten Werten.

**Die Top‑10‑Grenze ist gewählt und nicht gemessen.** M98 hat sie für vier Mandanten durchgerechnet:
Top 10 trägt bei `NEXANS` 76,4 %, bei `VOTG` 76,5 %, bei `IBIS` 72,1 %. Sie steht als Konstante im
Code und nicht in der Konfiguration — ein Schalter dafür wäre eine Gestaltungsentscheidung, die
niemand getroffen hat.

---

## 5. Überfällig — die erste benannte Ausnahme von L2

Zwei Zahlen, **beide live** über `Message`, beide über
`MessageStatusClassifier.ueberfaelligBedingung` (gerufen, nicht nachgebaut), `jetzt` aus der
**Anwendungsuhr** (Regel Z1):

- **im Fenster** (E‑h) — dieselbe Zahl, die der Klick in die Liste liefert
- **insgesamt** — ohne Zeitfenster

**Das Wort „ausschließlich" in L2 bleibt stehen, und daneben steht diese Ausnahme** (E‑c vom
24.08.2026, [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §8). Sie ist einzeln begründet und
einzeln gemessen.

**Die zweite Zahl hat kein Zeitfenster, und das ist durch Regel L9 gedeckt:** Gefragt ist genau, was
*außerhalb* des gezeigten Zeitraums hängt — ein Fenster schnitte die Zeilen weg, um die es geht. Sie
ist dabei die **billigere** von beiden (M90, Befund 14): Das Zeitfenster verengt nichts, weil
`MessageStatusIDX` bereits auf 539 von 3,34 Millionen Zeilen herunterführt.

### „Nicht ermittelbar" — und nur hier

Diese beiden sind die **einzigen Felder der ganzen Antwort**, die *nicht ermittelbar* zurückgeben
dürfen. Sie sind der einzige Teil, der zur Laufzeit auf der **Produktion** live liest, wo
`max_statement_time` nach zehn Sekunden abräumt. Der Rest kommt aus unserer eigenen Tabelle.
**Stirbt die Live-Abfrage, darf nicht die ganze Seite sterben.**

| | |
|---|---|
| **Kein allgemeiner Teilerfolg-Mechanismus** | Genau diese zwei Felder. Ein Dashboard, das jeden Block einzeln scheitern lassen kann, zeigt irgendwann eine Seite voller Lücken und nennt das eine Antwort |
| **Gefangen wird genau eine Ausnahme** | `DataAccessException` **mit** `SQLTimeoutException` als Ursache — dieselbe Unterscheidung wie in `NachrichtenRepository.anDerZeitgrenze`. Ein Syntaxfehler, eine abgerissene Verbindung oder ein fehlendes Recht bleiben technische Fehler mit `500`. **Ein pauschales `catch` machte aus jedem Bruch eine Beruhigung** |
| **Die beiden Zahlen fallen zusammen** | Fällt eine, ist auch die andere `null`. Sie stehen als *Paar* nebeneinander, und eine Kachel mit einer Zahl und einer Lücke lädt zu einer Rechnung ein, die nicht aufgeht |
| **`ermittelbar: false` ist nicht `0`** | Null hieße „es hängt nichts". In einem Überwachungswerkzeug ist das die schlimmste falsche Antwort |

`DashboardZeitgrenzeTest` stellt beide Fälle her — den Abbruch an der Zeitgrenze und den
Syntaxfehler — und prüft, dass nur der erste geschluckt wird.

**In der Anzeige trägt „überfällig" keine Farbe von *Fehler*.** Rot gehört ausschließlich der
Kategorie *Fehler*; die eigene Farbrolle (orange) ist in
[`visuelles-konzept.md`](visuelles-konzept.md) §7a entschieden. **Hier ist sie nicht gebaut** — der
Endpunkt trennt die Kategorie sauber, gefärbt wird in der Oberfläche.

---

## 6. Der Leerzustand

**Ein Feld, `leer`, und es unterscheidet nicht.** Die Oberfläche zeigt einen Satz, und der ist in
beiden Fällen wahr:

- im Zeitraum ist nichts passiert
- dieser Mandant hat überhaupt keine Daten

> ### ⚠️ Bekannte Grenze 2: Ein stiller Sonntag und `EDITIONLINGERI` sehen gleich aus
>
> Das ist die Folge, und sie ist gewollt. Die Alternative wäre eine zweite Abfrage gewesen — „hat
> dieser Mandant überhaupt jemals Verkehr gehabt?" —, um einen Satz anders zu formulieren, den
> niemand zweimal liest.

**Auch im Leerzustand nennt die Antwort ein Paar**, nämlich das erste der Reihe. Die Oberfläche
braucht eines zum Hervorheben und für die URL; ein Mandant ohne Daten sieht damit dieselbe Auswahl
wie jeder andere und darf durchschalten — er findet überall denselben Satz.

**Und auch im Leerzustand sagt der Katalog etwas:** Die Verteilung trägt genau eine Zeile,
`NICHT_ZUGEORDNET` mit null.

---

## 7. Die Statements (Regel L7)

Sieben je Seite, wenn `zeitraum` genannt ist; acht bis neun, wenn der Endpunkt selbst wählt (eine
bis drei Belegungsproben). Alle laufen über **`glassfishDsl`**, den Lese-Pool.

**Der Lese-Kontext und nicht `monitorDsl`** — die Aufteilung ist *lesen gegen schreiben* und nicht
*Quellschema gegen eigenes Schema*: Jede Abfrage hier joint `overlord_monitor.message_rollup*` gegen
`GlassfishDB.Process` und braucht dafür **eine einzige Verbindung**.

### Die Mandantenkette ist ein `EXISTS` und darf kein `JOIN` sein

`ProjectMandant` ist **n:m**. Ein `JOIN` vervielfachte jede Rollupzeile, sobald ein Projekt an
mehreren Mandanten hängt — und damit **die Summe**. Der Fehler wäre still: Die Zahlen sähen plausibel
aus und wären zu hoch.

**`Project` steht nicht in der Kette.** `Process → ProjectMandant` über `Process.ProjectID` liefert
dieselbe Menge; der Umweg über `Project` ist entbehrlich (Befund 48). **Die Messskripte von M94, M98
und M107 fahren die lange Fassung, der Anwendungscode die kurze** — der Unterschied ist in den
Laufzeiten unten enthalten und nicht herausgerechnet.

### Drei Ebenen, drei Statements

Die Ebenen ließen sich mit einem `CAST` auf einen gemeinsamen Schlüsseltyp zusammenfassen; das
kostete eine Funktion um die Schlüsselspalte und damit den Bereichszugriff. `DashboardStatementsTest`
hält fest, dass **um den Eimerschlüssel keine Funktion steht**.

### 7a. ⚠️ Der Befund: „Zuletzt aufgefallen" braucht zwei Statements

**Der erste Bau stellte Fehler und Überfällige mit `OR` in ein Statement.** Er lieferte das Richtige
und war falsch gebaut — und der Plan sagt warum:

| | mit `OR`, ein Statement | gebaut: zwei Statements **und** ein Indexhinweis |
|---|---|---|
| Treiberindex | **`MessageLastUpdateIDX`** | **`MessageStatusIDX`** |
| gelesene Zeilen, 48 h | 23.126 | 6.257 (Fehler) bzw. 539 (überfällig) |
| gelesene Zeilen, 30 Tage | 209.408 | dieselben |
| gelesene Zeilen, 12 Monate | **2.705.843** | dieselben |
| `SUTTONS`, 48 h, ganze Seite | **121,3 ms** | **72,0 ms** |
| `SUTTONS`, 12 Monate, ganze Seite | **2.585,7 ms** | **127,0 ms** |
| `VOTG`, 12 Monate | **`500`** — Abbruch an `max_statement_time` | läuft |

**Der Grund ist die Deckelung.** `ORDER BY … LIMIT 10` ist nur billig, wenn die zehn Zeilen früh
gefunden werden. Ein Mandant **ohne** Fehler im Fenster zwingt die Datenbank, den ganzen Bereich zu
durchsuchen, bevor sie „nichts" sagen darf — **gerade der gute Fall ist der teure**.

### Zwei Änderungen, und die erste allein genügte nicht

**1. Je Merkmal ein Statement.** Zusammengeführt und gedeckelt wird in Java; aus zweimal zehn
neuesten Zeilen sind die zehn neuesten dieselben wie aus einer gemeinsamen Abfrage, weil die beiden
Mengen **disjunkt** sind (Fehler ist Endstatus, überfällig setzt das Gegenteil voraus).

**Das hat den Plan aber nicht gedreht.** Auch die getrennte Fehlerabfrage stieg weiterhin über den
Zeitindex ein: Er liefert die Sortierung gratis, und das ist dem Optimierer mehr wert als der
kleinere Bereich.

**2. `IGNORE INDEX FOR ORDER BY (MessageLastUpdateIDX)`** — der **einzige Indexhinweis dieses
Projekts**. Er verbietet genau eines: den Zeitindex *zur Sortierung* zu verwenden. Erst damit steigt
die Abfrage über `MessageStatusIDX` ein, und der Aufwand hängt an der Zahl der **auffälligen** Zeilen
im Bestand statt an der Breite des Fensters — 822 Fehlerzeilen und 538 offene.

| Fassung | `NEXANS` | `SUTTONS` | `VOTG` |
|---|---|---|---|
| ohne Hinweis, 48 h / 30 T / 12 M | 3,4 / 2,5 / 1,6 ms | 63,2 / **1.092,2** / **2.174,4** ms | 65,0 / **1.065,4** / *Abbruch* |
| `IGNORE INDEX FOR ORDER BY` | 25,1 / 24,0 / 25,0 ms | 23,1 / 24,1 / 24,4 ms | 23,3 / 22,8 / — |
| `FORCE INDEX (MessageStatusIDX)` | 24,3 / 24,1 / 24,1 ms | 23,9 / 24,0 / 23,9 ms | 23,1 / 22,6 / — |

*(nur die Fehlerhälfte; die Überfälligkeitshälfte liegt in allen drei Fassungen bei 4,4 bis 7,1 ms,
weil sie ohnehin über den Statusindex fährt.)*

**`IGNORE` und `FORCE` sind gleich schnell — genommen ist der schwächere Eingriff.** Er nimmt dem
Optimierer eine Möglichkeit und lässt ihm die Wahl des Zugriffspfads; `FORCE` schriebe den Pfad fest.

> ### Warum überhaupt ein Hinweis, wo `STRAIGHT_JOIN` ausgeschlossen ist
>
> Das eine ist ein Verbot der **Join-Reihenfolge** und war in M42 um Faktor 219 bis 1094 schlechter.
> Dies hier betrifft **einen Index und eine Verwendung davon**. Und es ist gemessen, in drei
> Fassungen und für drei Mandanten über alle drei Fensterbreiten — genau das, was offener Punkt 57
> für Index-Hinweise als ungemessen ausweist.
>
> **Der Preis ist benannt:** Im guten Fall — `NEXANS`, Fehler direkt am Fensterrand — kostet der
> Hinweis das Sieben- bis Fünfzehnfache. **Getauscht wird Schwankung gegen Verlässlichkeit:**
> konstante 24 ms bei einem Budget von 500 ms gegen einen Wert zwischen 1,6 ms und einem Abbruch, je
> nachdem, ob der Mandant gerade Fehler hat. Dass die Wette auf die heutige Statistik gesetzt ist,
> steht als offener Punkt 82.

`DashboardPlanDbIT` ist der Wächter: Er verlangt für **beide** Hälften und **alle drei** Paare den
Statusindex und prüft als Gegenprobe, dass `MessageLastUpdateIDX` in keiner Planzeile des Blocks
steht.

### 7b. Warum nicht das bestehende Listen-Repository

Der Auftrag nennt als Quelle für Block 6 *„das bestehende Listen-Repository"*. **Das geht nicht, und
zwar aus zwei Gründen — der erste allein genügte schon:**

1. **Die Liste kann diese Frage gar nicht beantworten.** Dort sind `status=FEHLER` und
   `ueberfaellig=true` ausdrücklich **unvereinbar** und ergeben `400`
   (`ueberfaellig-und-status-unvereinbar`): Überfällig setzt `WARTEND` oder `LAEUFT` voraus, Fehler
   ist ein Endstatus. Über die Liste bräuchte der Block **zwei** Aufrufe, ein Zusammenführen und
   eine Neusortierung — also genau das, was jetzt dasteht, nur mit zwei HTTP-Schichten dazwischen.
2. **Fachpakete kennen einander nicht.** `dashboard` darf nicht aus `message` importieren
   (`PaketstrukturTest.fachpakete_kennen_einander_nicht`); braucht ein zweites Fachpaket einen Typ,
   **wandert der Typ nach `common`**.

**Wiederverwendet ist damit genau das, was driften könnte:** die Fehlerbedingung und die
Überfälligkeitsbedingung, beide aus `common/MessageStatusClassifier`. **Nachgebaut ist nichts.** Die
Mandantenkette schreibt ohnehin jedes Fachpaket selbst — sie kann nicht nach `common` wandern, weil
dort keine `jooq.glassfish`-Typen stehen dürfen.

**Die Fensterverengung fällt dabei nicht weg, sie greift ohnehin nicht:** Für `ueberfaellig` ist sie
abgeschaltet ([`nachrichtenliste.md`](nachrichtenliste.md) §5d), weil der Rollup keine Frist kennt.

> **Aus demselben Grund ist `Pflegestatus` von `catalog` nach `common` gewandert:** Der
> Verteilungsblock braucht `GEPFLEGT`. Die Alternative wäre ein Literal `"GEPFLEGT"` im Dashboard
> gewesen — dieselbe Bedingung an zwei Stellen, und die driftet.

---

## 8. Die Messung — M108 *(31.08.2026)*

Gemessen mit `MessungM108DbIT`: **das, was der Code schickt**, gegen die Testkopie, im Profil `dev`
gegen den Anker `2025-12-30 04:09:47`. Ein Aufwärmlauf, dann die **beste von fünf** — dieselbe
Bauform wie M94, M98 und M107. Der `EXPLAIN` läuft über das **gerenderte** Statement mit Literalen.

**Die Zeiten stehen hier und in keiner Zusicherung** (Regel T1): `MessungM108DbIT` sichert nur
Zählwerte zu.

### Die Bezugswerte und was herausgekommen ist

| Ansicht | erwartet (M94/M107/M90) | **gemessen, `NEXANS`** | **gemessen, `SUTTONS`** |
|---|---:|---:|---:|
| 48 h — Verlauf | 6,8 ms | **8,955 ms** | 6,941 ms |
| 48 h — Verteilung | 10,7 ms | **9,920 ms** | 21,676 ms |
| 30 Tage — Verlauf | ~46 ms | **47,221 ms** | 30,789 ms |
| 30 Tage — Verteilung | ~62 ms | **62,570 ms** | 32,031 ms |
| 12 Monate — Verlauf | 65,4 ms | **63,699 ms** | 39,687 ms |
| 12 Monate — Verteilung | 88,7 ms | **88,519 ms** | 40,223 ms |
| Überfällig — im Fenster | 2,3 ms | 5,102 ms | 4,516 ms |
| Überfällig — insgesamt | 4,3 ms | 6,171 ms | 5,171 ms |
| Zuletzt aufgefallen (beide Statements), 48 h / 30 T / 12 M | — | 32,595 / 31,192 / 31,950 ms | 29,715 / 32,887 / 34,084 ms |
| Belegungsprobe, 48 h / 30 T / 12 M | — | 7,754 / 36,950 / 47,770 ms | 6,252 / 30,380 / 39,317 ms |
| Stand | — | 0,722 ms | 0,781 ms |

**Verlauf und Verteilung treffen die Vorhersage aus M107 auf die dritte Stelle** — 63,699 gegen
65,350 ms und 88,519 gegen 88,672 ms. Das ist der Beleg dafür, dass die gebaute Fassung dieselbe
Abfrage ist wie die gemessene, obwohl sie die Mandantenkette als `EXISTS` statt als `JOIN` fährt und
`Project` weglässt.

**„Zuletzt aufgefallen" ist von der Fensterbreite unabhängig** — 29 bis 34 ms über alle sechs
Kombinationen. Genau das ist der Zweck des Umbaus aus §7a; vorher lag derselbe Block zwischen
1,6 ms und einem Abbruch.

### Die ganze Landingpage

| Paar | **`NEXANS`** | **`SUTTONS`** |
|---|---:|---:|
| 48 h | **62,227 ms** | **72,007 ms** |
| 30 Tage | 152,814 ms | 109,829 ms |
| 12 Monate | **199,030 ms** | 127,038 ms |
| **ohne `zeitraum`** (Standardfenster; beide bekommen `48H`) | **64,706 ms** | **77,957 ms** |

**Das Budget sind 500 ms.** Die Standardansicht kostet 63 bis 78 ms, die teuerste überhaupt
mögliche 199 ms. **Deutlich darunter, wie verlangt.**

### Zwei Abweichungen, und beide sind Befunde

**1. Die Kachel *Überfällig* kostet mehr als M90 sagt** — 5,1 statt 2,3 ms und 6,2 statt 4,3 ms,
also gut das Doppelte. **Der Plan ist identisch** (`range` über `MessageStatusIDX`, `rows = 539`,
Zeichen für Zeichen der aus M90). Die naheliegende Erklärung steht in
[`messungen-schritt10.md`](messungen-schritt10.md) selbst: **M90 hat gegen den falschen Anker
gemessen** (`2026-07-08 17:21:10` statt `2025-12-30 04:09:47`), und im dortigen Fenster D lagen
**null** Treffer statt einem. **Gemessen ist damit nicht dieselbe Frage.** Die absolute Größe bleibt
belanglos — 6 ms von 500 —, und die Rechnung, für die M90 gebraucht wurde, trägt unverändert: Die
Kachel ist billig genug, um live zu laufen.

**2. Die Verteilung ist bei `SUTTONS` im 48‑Stunden‑Fenster teurer als bei `NEXANS`** — 21,7 gegen
9,9 ms, bei einem Achtel der Nachrichten. Das ist kein Widerspruch, sondern derselbe Befund wie in
M98: **Der Bereichszugriff liest die Rollupzeilen des ganzen Fensters**, unabhängig davon, wie viele
davon dem Mandanten gehören. *„Ein kleiner Mandant zahlt im Verteilungsblock fast so viel wie der
größte."*

### Die Pläne (Regel L15)

| Statement | Zugriffspfad auf die Quelle |
|---|---|
| Verlauf 48H | `message_rollup`, `range` über `PRIMARY`, `key_len 5`, `Using temporary; Using filesort` |
| Verlauf 30T | `message_rollup_tag`, `range` über `PRIMARY`, `key_len 3` |
| Verlauf 12M | `message_rollup_monat`, `range` über `PRIMARY`, `key_len 3` |
| Verteilung, alle drei | wie der Verlauf, plus `process_catalog` als `eq_ref` über `PRIMARY` |
| Belegung, alle drei | derselbe Bereich, gekapselt in einer abgeleiteten Tabelle |
| Überfällig, beide | `Message`, `range` über **`MessageStatusIDX`**, `key_len 123`, `rows = 539` |
| Zuletzt aufgefallen, Fehlerhälfte | `Message`, `range` über **`MessageStatusIDX`**, `rows = 6.257` |
| Zuletzt aufgefallen, Überfälligkeitshälfte | `Message`, `range` über **`MessageStatusIDX`**, `rows = 539` |
| Stand | `rollup_lauf`, `range` über `rollup_lauf_stand_idx` |

Die Mandantenkette steht in **jedem** Plan als `eq_ref` über Primärschlüssel — nie als Durchlauf.

> **Welche Tabelle den Einstieg macht, hängt am Mandanten.** Bei `NEXANS` ist es die Rolluptabelle,
> bei `SUTTONS` steigt der Optimierer über `ProjectMandant` ein, weil dieser Mandant wenige Projekte
> hat. **Beide Pläne sind richtig**, beide sind gemessen schnell — und `DashboardPlanDbIT` schreibt
> deshalb **die Zugriffsart und den Index** fest und nicht die Reihenfolge. Dieselbe Überlegung wie
> in [`nachrichtenliste.md`](nachrichtenliste.md) §5b.

> ### Belegvermerk (Regel L10)
>
> *Gemessen ist:* alle sieben Statements und die zusammengesetzte Landingpage, je Paar und für zwei
> Mandanten, **warm**, ein Aufwärmlauf und dann die beste von fünf, gegen die Testkopie im Profil
> `dev`.
>
> *Behauptet wird:* Die Landingpage bleibt in jeder Kombination deutlich unter 500 ms.
>
> **Die Lücken, und es sind drei.** Alle Werte sind **Warmwerte**; `FLUSH TABLES` steht
> `monitor_read` nicht zu. Gemessen sind **zwei von zehn** Mandanten. Und alle Zahlen stammen von
> der Testkopie — über die Produktion sagt keine von ihnen etwas.

---

## 9. Tests

| Test | Was er sichert |
|---|---|
| `DashboardIsolationDbIT` | **Regel M4, Pflicht.** Zwei Zusicherungen tragen den Nachweis: **verschiedene Summen** über dasselbe Fenster (fiele der Filter, sähen beide die Zahl des ganzen Bestands) und **jede gezeigte Prozesskennung gehört dem eigenen Mandanten**. Dazu: keine fremde Kennung im Rumpf, kein Mandantenparameter, Standardfenster und Leerzustand, alle Blöcke in einer Antwort |
| `DashboardServiceTest` | Einordnung, Fehlerarten, die beiden Restzeilen, „nicht ermittelbar", Standardfenster, Leerzustand — **ohne Datenbank**, alle Prüfwerte erfunden |
| `DashboardStatementsTest` | Das **gerenderte** SQL: `EXISTS` statt `JOIN`, `CASE` ohne Alias, `LEFT JOIN` auf den Katalog, keine Funktion um den Eimerschlüssel, sieben Statements je Seite |
| `DashboardPlanDbIT` | **`EXPLAIN`, Treibertabelle und Index — keine Zeitmessung.** Der Wächter über den Befund aus §7a |
| `DashboardZeitgrenzeTest` | Der Abbruch an der Zeitgrenze wird zu „nicht ermittelbar", **jeder andere Fehler nicht** |
| `DashboardzeitraumTest` | Die Fenstergrenzen, gegen den Anker — Eimergrenzen, obere Grenze ausschließend, Kalendermonate statt 365 Tagen |
| `MessungM108DbIT` | Die Messung. Kein Test |

### Die Verletzungsprobe

Ausprobiert am 31.08.2026 und **zurückgenommen**: In `DashboardRepository.mandantenkette` wurde
`PROJECTMANDANT.MANDANTID.eq(mandant.mandantId())` durch `isNotNull()` ersetzt — der
Mandantenfilter aus Regel M3, ausgehängt.

```
[Ohne Mandantenfilter saehen beide dieselbe Summe (48H)]
Expecting actual:
  12004L
not to be equal to:
  12004L
        at DashboardIsolationDbIT.summen_sind_verschieden

[Fremde Prozesskennung in „Zuletzt aufgefallen" (48H)]
Expecting JSONArray:
  ["18100_ARCHROMA_IFTSTA", "08100_ARCHROMA_IFTMIN", … ]   ← die Prozesse von VOTG
to contain: … but could not find the following element(s):
  ["40090_BMW_LAB_VDA", …]                                 ← sie gehoeren NEXANS
        at DashboardIsolationDbIT.nur_eigene_prozesse_in_den_zeilen
```

**12.004 ist die Zahl des ganzen Bestands im 48‑Stunden‑Fenster** (M95, Paar P1). Ohne
Mandantenfilter sieht sie jeder Mandant — die Meldung nennt damit nicht irgendeine Abweichung,
sondern genau den Zustand, den Regel M4 verbietet.

> ### ⚠️ Die Probe hat einen zweiten Test hervorgebracht, und das ist der eigentliche Ertrag
>
> **Beim ersten Durchlauf blieb `keine_fremden_prozesse` grün** — mit ausgehängtem Filter. Der Grund
> ist strukturell und gilt für jedes Dashboard: **Eine aggregierte Antwort trägt kaum Kennungen.**
> Der Verlauf besteht aus Zahlen, der Verteilungsblock zeigt Partnernamen, und die zehn Zeilen aus
> „Zuletzt aufgefallen" gehörten zufällig alle `NEXANS` — gegen dessen Prozessliste hat der Test
> gar nicht geprüft, weil die Paarung `VOTG`/`SUTTONS` ist.
>
> **`nur_eigene_prozesse_in_den_zeilen` dreht die Frage um:** Jede *gezeigte* Prozesskennung muss in
> der **eigenen** Prozessliste stehen. Damit fällt der Test, egal welchem fremden Mandanten die
> Zeile gehört — und er fällt schon dann, wenn nur der Filter dieses einen Blocks ausfällt.
>
> **Das ist genau der Fall, für den [`testfestigkeit.md`](testfestigkeit.md) angelegt worden ist:**
> ein grüner Test, der seine Aussage nicht trägt. Gefunden hat ihn nicht das Nachdenken, sondern die
> Verletzungsprobe.

Der Arbeitsbaum ist danach wiederhergestellt worden; die Änderung ist in keinem Commit.

---

## 10. Regelbezug

| Regel | Stand |
|---|---|
| **M1** Kein Endpunkt nimmt eine Mandanten-ID entgegen | **erfüllt** — und **keine** neue benannte Ausnahme. `?mandant=` ist wirkungslos, geprüft |
| **M2** `MandantContext` erster Pflichtparameter | **erfüllt** — jede öffentliche Methode von `DashboardRepository`. `letzterLauf()` ist **paketprivat**, weil `rollup_lauf` keinen Mandanten trägt und ein Schein-Kontext schlimmer wäre als keiner |
| **M3** Filter im Statement, nicht nachgelagert | **erfüllt** — `EXISTS` in jeder Abfrage |
| **M4** Isolationstest je Endpunkt | **erfüllt** — `DashboardIsolationDbIT` |
| **M5** Trennung gilt auch quer | **erfüllt** — auch die Rollupzeilen, auch die Katalogwerte |
| **L1** Pflicht-Zeitfenster | **erfüllt** — jedes Paar hat eines, und es steht in der Antwort |
| **L2** Keine Live-Aggregation über `Message` | **erfüllt bis auf die eine benannte Ausnahme**, §5 |
| **L7** Jede neue Abfrage gemessen | **erfüllt** — M108, §8, mit `EXPLAIN` je Statement |
| **L9** Durchlauf ohne Zeitfenster nur begründet | **erfüllt** — nur *Überfällig insgesamt*, begründet in §5 |
| **Q3** Die Problemkategorien bleiben getrennt | **erfüllt** — `FEHLER` und `UEBERFAELLIG` sind eigene Werte und werden nie zu „Problem" zusammengefasst |
| **Q4** Nichts raten | **erfüllt** — unbekannte Rohwerte bleiben Rohwerte, Restzeilen tragen keinen Anzeigetext, `sosName` darf `null` sein |
| **T1** Kein Test behauptet etwas über Wanduhrzeit | **erfüllt** — der Plantest prüft den Plan, die Messung sichert nichts zu |
| **T2** Kein Test hängt an veränderlichen Daten | **erfüllt** — der Isolationstest bezieht sich auf die Prozessliste und nicht auf den Pflegestand |
| **Z1** Kein `now()` | **erfüllt** — ein Uhrenschlag je Anfrage, aus der Anwendungsuhr |

---

## 11. Offene Punkte

| Nr. | Punkt |
|---|---|
| **82** | **Der Indexhinweis aus §7a ist der erste des Projekts, und er ist eine Wette auf die Statistik.** `IGNORE INDEX FOR ORDER BY (MessageLastUpdateIDX)` nimmt dem Optimierer eine Möglichkeit, die er heute falsch bewertet. Ändert sich die Verteilung der Fehlerzeilen — etwa weil ein Mandant dauerhaft viele hat —, könnte der Zeitindex wieder die bessere Wahl sein, und der Hinweis stünde dann im Weg. **Gemessen ist der heutige Bestand**, nicht der von morgen. Einen Wächter dagegen gibt es nicht, wohl aber `MessungM108DbIT`: Wer die Zahlen nachmisst, sieht es |
| **83** | **Der Aufwand von „Zuletzt aufgefallen" hängt an der Zahl der auffälligen Zeilen im *Gesamtbestand*, nicht im Fenster.** 822 Fehlerzeilen und 538 offene heute, konstante rund 30 ms. In einem Bestand mit hunderttausend Fehlerzeilen wächst er mit — dann wäre ein Index auf `(MessageStatus, MessageLastUpdate)` die Antwort, und der läge auf `GlassfishDB` (Regel S1). **Gerechnet, nicht gemessen** |
| **84** | **Die Kachel *Überfällig* kostet doppelt so viel wie M90 sagt** (§8, Abweichung 1). Die Erklärung — M90 hat gegen den falschen Anker gemessen — ist plausibel und **nicht nachgemessen**. Eine Nachmessung von M90 am geltenden Anker steht ohnehin aus |
| **85** | **Acht von zehn Mandanten sind ungemessen.** Gemessen sind `NEXANS` und `SUTTONS` (Regel L7). `IBIS` ist der Mandant, bei dem der Verteilungsblock am ungünstigsten aussieht — flache Verteilung, „Übrige (40)" als größter Balken (M98, Befund 21). **Die Gestaltung wird ihn brauchen, die Laufzeit nicht** |
| **86** | **Der Leerzustand ist nicht unterscheidbar** (§6, bekannte Grenze 2). Gewollt, und hier nur benannt, damit es nicht als Fehler gemeldet wird |
| **87** | **Die Kachel *Nachrichten* zählt Aktivität und nicht Nachrichten** (§2, bekannte Grenze 3). Ebenfalls gewollt und ebenfalls nur benannt |

### Und was hier geschlossen wird

| Nr. | Woher | Stand |
|---|---|---|
| **61** | [`messungen-schritt10b.md`](messungen-schritt10b.md) | **erledigt** — die Schwelle ist festgelegt, beide Bedingungen sind gebaut, und die zweite ist als gewählte und nicht gemessene Zahl benannt (§3) |
| **62** | [`messungen-schritt10b.md`](messungen-schritt10b.md) | **erledigt** — der Block verträgt fehlende Restzeilen und hält beide unten (§4) |
| **58** | [`messungen-schritt10b.md`](messungen-schritt10b.md) | **berührt, nicht erledigt.** Er verlangt, `SUTTONS` vor der Abnahme von 10b zu kuratieren. Die Katalogpflege ist mit Teil A freigegeben ([`prozess-katalog.md`](prozess-katalog.md) §1); dass sie geschieht, ist eine Handlung des Auftraggebers und keine Codeänderung |

> **Warum die Vermerke zu 58, 61 und 62 hier stehen und nicht dort, wo die Punkte vergeben sind:**
> [`messungen-schritt10b.md`](messungen-schritt10b.md) ist eine Messdatei und in diesem Schritt
> ausdrücklich nicht anzufassen. Dasselbe Verfahren hat Schritt 10b‑1 für Punkt 55 gewählt.

---

## 12. Was dieser Schritt nicht zeigt

1. **Keine Oberfläche.** Kein Diagramm, keine Kachel, kein Umschalter, keine Farbe.
2. **Nichts über die Produktion.** Alle Zahlen stammen von der Testkopie.
3. **Den Kaltlauf nicht.** `FLUSH TABLES` steht `monitor_read` nicht zu; gemessen ist der Warmfall.
4. **Nichts über die acht übrigen Mandanten.** Gemessen sind `NEXANS` und `SUTTONS` (Regel L7).
5. **Nicht, ob die Kategorie *Überfällig* fachlich richtig geschnitten ist.** `RUNNING` kommt auf der
   Testkopie null Mal vor; gemessen sind Plan und Laufzeit, nicht die fachliche Größenordnung.
