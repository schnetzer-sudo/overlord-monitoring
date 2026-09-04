# Dashboard — die Landingpage

Stand: 03.09.2026 · Schritt 10b‑4 · **Backend, keine Oberfläche**

> **Was 10b‑4 geändert hat:** Die Problemkategorie *Überfällig* ist widerlegt und aus dem MVP
> genommen (E‑71); an die Stelle ihrer Kachel treten *Läuft* und *Wartend* (§5). Alle übrigen
> Abschnitte sind vom 31.08.2026 (Schritt 10b‑2 Teil D) und unverändert, soweit kein
> Korrekturblock daneben steht.

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
    "laeuft":      { "anzahl": 0,   "aeltesteSekunden": null,   "ermittelbar": true },
    "wartend":     { "anzahl": 538, "aeltesteSekunden": 579934, "ermittelbar": true }
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

## 2. Die Blöcke — acht, und weiterhin sieben Statements

> ⚠️ **Die Zahl der Statements ist dieselbe geblieben, und das ist eine Falle.** In Schritt
> 10b‑4 sind **drei** weggefallen (zweimal *Überfällig*, die Überfälligkeitshälfte von Block 6)
> und **drei** hinzugekommen (*Läuft*, *Wartend*, die Erscheinungsbedingung). `DashboardStatementsTest`
> hat deshalb aufgehört zu zählen und **benennt** seither jedes Statement einzeln (§9).

| # | Block | Quelle | Statements |
|---|---|---|---:|
| 1 | **Verlauf** je Eimer, nach Einordnung | Rollup-Ebene des Paares × Mandantenkette | 1 |
| 2 | **Kachel Nachrichten** | *derselbe Lesevorgang wie 1* | — |
| 3 | **Kachel Fehler** samt Aufschlüsselung nach Art | *derselbe Lesevorgang wie 1* | — |
| 4 | **Kachel Läuft** — Zahl und Alter der ältesten | **live** über `Message` | 1 |
| 4a | **Kachel Wartend** — dieselben zwei Werte, dazu die Erscheinungsbedingung | **live** über `Message` bzw. `SOSAction` | 2 |
| 5 | **Verteilung** — Partner oder Richtung | Rollup-Ebene × Mandantenkette × `process_catalog` | 1 |
| 6 | **Zuletzt aufgefallen** | `Message`, nur noch die Fehlerbedingung (§7a) | 1 |
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
> Sie sollte `WOC` fangen: **29 von 30 Tagen belegt bei 117 Nachrichten** (M95), also knapp vier am
> Tag — ein Diagramm mit Punkten, das nichts zeigt. **Weil EDI-Verkehr stoßweise ist, liegt aber mit
> hoher Wahrscheinlichkeit ein Tag über fünf, und `WOC` besteht die Bedingung.**
>
> **Das ist gerechnet und nicht gemessen** — M108 misst `NEXANS` und `SUTTONS`, nicht `WOC`. Die
> Rechnung: 117 Nachrichten auf 29 Tage bei stoßweisem Verkehr; damit **kein** Tag über fünf liegt,
> müssten sich die Nachrichten fast gleichmäßig verteilen, und genau das tun sie nicht.
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

## 5. Läuft und Wartend — die zwei benannten Ausnahmen von L2

*Neu am 03.09.2026 (Schritt 10b‑4). Bis dahin stand hier die Kachel **Überfällig** als die erste
benannte Ausnahme; der alte Abschnitt steht als Korrekturblock am Ende dieses Kapitels.*

Zwei Kacheln, **beide live** über `Message`, **beide ohne Zeitfenster**, je **zwei Werte aus einem
Statement**:

```sql
SELECT COUNT(*) AS anzahl, MIN(m.MessageLastUpdate) AS aelteste
FROM GlassfishDB.Message m
WHERE m.MessageStatus = ?              -- 'RUNNING' bzw. 'SUSPENDED'
  AND EXISTS ( … Mandantenkette … );
```

| | |
|---|---|
| **`=` auf den Rohwert, nicht der Klassifizierer** | Die Einordnungen `LAEUFT` und `WARTEND` haben je genau **einen** Rohwert, und `MessageStatusIDX` trägt den Rohwert. Ein `IN` über eine einelementige Menge wäre derselbe Zugriff mit einer Unwahrheit darin |
| **Der Rohwert wird trotzdem gerufen** | `MessageStatusClassifier.einzigerRohwert(einordnung)`. Ein Literal `"SUSPENDED"` im Dashboard wäre dieselbe Zuordnung ein zweites Mal, und sie driftete beim nächsten Statuswert von der Liste weg. Die Methode **wirft**, sobald eine der beiden Einordnungen einen zweiten Rohwert bekäme |
| **Die Mandantenkette ist Bestandteil des Statements** (Regel M3) | als `EXISTS` und nicht als Join — `ProjectMandant` ist n:m, und ein Join vervielfachte Zeilen |
| **`aeltesteSekunden` rechnet das Backend** (E‑75) | gegen die **Anwendungsuhr** (Regel Z1). Bei `anzahl = 0` ist es `null` — ohne Zeile gibt es kein Alter, und eine `0` hieße „seit null Sekunden" |

### Warum das Alter der ältesten Zeile daneben steht (E‑75)

**Es hat zwei Aufgaben, und die zweite ist die wichtigere.**

**Erstens: Es ist die Grundlage eines Fensters, das die Oberfläche selbst wählen kann.** Eine Zahl
ohne Alter sagt „538 warten"; mit Alter sagt sie „538 warten, die älteste seit sieben Tagen". Das
ist der Unterschied zwischen einer Zahl und einer Auskunft, und die Kachel braucht dafür **keinen**
zweiten Aufruf — `MIN(MessageLastUpdate)` liest denselben Indexbereich wie `COUNT(*)`.

**Zweitens: Es ist die laufende Prüfung der Auskunft, auf der dieser ganze Schritt ruht.** Die Regel
sagt, `SUSPENDED`-Nachrichten lägen „höchstens rund eine Woche". **Das Feld zeigt bei jedem Aufruf,
ob das noch stimmt.** Steht dort eines Tages ein Alter von Monaten, ist die Auskunft widerlegt — und
zwar dort, wo jemand hinsieht, statt in einer Messung, die niemand wiederholt.

> **Das ist ausdrücklich keine Schwelle und keine Warnung.** Das Feld trägt eine Zahl und kein
> Urteil; ob sieben Tage viel sind, entscheidet niemand im Backend (Regel Q4). **Es macht die
> ungemessene Auskunft nur beobachtbar** — und das ist das Höchste, was ein Werkzeug ohne Schwelle
> für sie tun kann.

### Ohne Zeitfenster, und das ist durch Regel L9 gedeckt

Gefragt ist, was **jetzt** offen ist. Ein Zeitfenster schnitte gerade die **ältesten** Zeilen weg —
also die, um die es geht: Eine Nachricht, die seit sechs Tagen wartet, fiele aus einem
48‑Stunden‑Fenster heraus und ist trotzdem der Grund, warum es die Kachel gibt.

Es ist außerdem keine Aggregation über einen Bereich, sondern eine Zählung über die wenigen
Indexsätze, auf die `MessageStatusIDX` herunterführt — **538 im ganzen Bestand**.

### Warum sie nicht aus dem Rollup kommen können

**`message_rollup` trägt keine Statushistorie.** Der nächtliche Volllauf rechnet jeden Eimer aus dem
*heutigen* Zustand jeder Nachricht neu; eine Nachricht, die im März `SUSPENDED` war und im April
fertig wurde, hinterlässt im März **nichts**. Der Rollup ist nach jedem Volllauf eine Projektion des
Jetzt, gebucht nach letzter Änderung. Ausgeschrieben in [`rollup.md`](rollup.md) §7a.

**Das ist ein anderer Grund als bei *Überfällig*.** Dort hing die Kennzahl an einer **Frist**, die
zwischen zwei Läufen abläuft. Hier hängt sie an einem **flüchtigen Status**. Beide Male ist das
Ergebnis dasselbe — eine Live-Abfrage —, aber die Begründung ist es nicht, und sie ist einzeln
einzutragen (`PROJEKTBESCHREIBUNG.md` §8).

### 5a. Die Kachel *Wartend* erscheint strukturell (Entscheidung E‑74)

**Sie erscheint nur bei Mandanten, deren Abläufe überhaupt suspendieren.** Zeigt sie dann `0`, ist
das eine Auskunft und kein Rauschen.

```sql
SELECT EXISTS (
  SELECT 1
  FROM GlassfishDB.SOSAction sa
  JOIN GlassfishDB.SOS s ON s.SOSID = sa.SOSID
  WHERE sa.SOSActionServiceProperties LIKE '%SUSPEND%'
    AND EXISTS ( … Mandantenkette über s.ProcessID … )
) AS hat_wartende_ablaeufe;
```

**Die Beziehung ist vor dem Bau geprüft** (M142 a): `SOS` trägt eine `ProcessID`, sie ist
`NULL`-fähig, und **alle 1.818 `SOS`-Zeilen haben eine** — verteilt auf 1.502 der 1.503 Prozesse.
Die Kette ist damit dieselbe wie überall, nur mit `SOS.ProcessID` statt `Message.ProcessID`.

> ### Zwei Wege sind geprüft und beide verworfen
>
> **Nicht über die Zahl selbst.** „Kachel erscheint bei `anzahl > 0`" flackert: *heute wartet
> nichts* und *dieser Mandant wartet nie* sähen gleich aus, und ein Mandant mit nächtlichem
> Sammelversand hätte die Kachel tagsüber nicht. Das ist die bekannte Grenze 2 dieses Dokuments ein
> zweites Mal — **Abwesenheit ist der schwächste Kanal, den ein Zustand haben kann.**
>
> **Nicht über den Rollup.** Er trägt keine Statushistorie (siehe oben). Eine Frage nach *„hat der
> Mandant je gewartet"* ist dort nicht beantwortbar.

#### `SUSPEND` und nicht `WAITUNTIL` — und das ist gemessen, nicht gewählt

M29 (4) hat **beide** Marken bei **allen 538** wartenden Nachrichten gefunden; die Beobachtung am
Bestand entscheidet also nichts. **M144 entscheidet es, über die Stammdaten:**

| Marke | Mandanten mit `true` |
|---|---|
| **`SUSPEND`** | **`NEXANS` und `VOTG`** |
| `WAITUNTIL` | nur `NEXANS` |

**`WAITUNTIL` verlöre `VOTG`.** Dessen einzige `SUSPEND`-Zeile trägt die andere Marke nicht.
`SUSPEND` ist damit zugleich das Wort, das den Zustand benennt, **und** das treffsicherere — die
Entscheidung fällt nicht auf die Bedeutung, sondern auf die Messung.

#### Die Lücke, die dazugehört

M29 hat `MessageAction.SOSActionServiceProperties` gemessen — den **ausgeführten** Baustein. Diese
Abfrage liest `SOSAction.SOSActionServiceProperties` — den **geplanten**.

**M142 (c) hat die Übertragung nachgeprüft:** Bei **538 von 538** trägt auch der geplante Baustein
das Wort, und über beide Schritte aller 538 (1.076 Zeilen) gibt es **null Abweichungen** zwischen
ausgeführt und geplant.

> **Belegvermerk** (Regel L10).
> *Gemessen ist:* dass der geplante Baustein bei 538 von 538 wartenden Nachrichten `SUSPEND` und
> `WAITUNTIL` trägt, und dass ausgeführt und geplant über 1.076 Zeilen nirgends auseinanderfallen.
> *Behauptet wird:* Der geplante Ablauf ist ein tragfähiger Ersatz für den ausgeführten.
> **Die Lücke, und sie ist größer als „538":** Die 538 hängen an **einer** `SOSAction`-Zeile — ein
> Ablauf, eine `SOSActionID`, der Baustein `Send Message to Pool` (M29 4, M142 c). `n = 538` ist
> die Zahl der **Zeilen**, nicht die der **Fälle**; an Vielfalt liegt **eine** vor. Die Messung
> kann die Übertragung **widerlegen**; bestätigen kann sie sie nur für diese eine Gestalt.

#### Sie wird bei jedem Aufruf mitgelesen, nicht bedingt

Sonst hinge die Zahl der Statements am Mandanten und `DashboardStatementsTest` wäre nicht mehr
deterministisch. Die Entscheidung, ob die Kachel in der Antwort steht, fällt im Zusammenbau.

#### Warum ein `LIKE '%…%'` hier zulässig ist, wo M8 dafür 97,976 s gemessen hat

Jene Messung lief über **`MessageAction`** — 10,3 Millionen Zeilen, 3,0 GB. **`SOSAction` ist
Stammdaten: 3.944 Zeilen, 2,0 MiB** (M142 a, gezählt, nicht aus `information_schema`).

**Und der Plan ist die eigentliche Rechtfertigung:** Der Optimierer steigt über
`ProjectMandant_Mandant_idx` ein, also **beim Mandanten** — das `LIKE` läuft nur über dessen eigene
`SOSAction`-Zeilen und **nie über die Tabelle**.

```
pm  ref  ProjectMandant_Mandant_idx  rows 17  Using where; Using index
p   ref  Process_ProjectFK           rows  5  Using index
s   ref  SOS_ProcessFK               rows  1  Using index
sa  ref  PRIMARY                     rows  1  Using where
```

**Gemessen 1,24–1,40 ms** (M143). Das Abbruchkriterium des Auftrags lag bei **200 ms**.

### „Nicht ermittelbar" — und nur hier

Diese beiden Kacheln sind die **einzigen Felder der ganzen Antwort**, die *nicht ermittelbar*
zurückgeben dürfen. Sie sind der einzige Teil, der zur Laufzeit auf der **Produktion** live über
`Message` liest, wo `max_statement_time` nach zehn Sekunden abräumt. Der Rest kommt aus unserer
eigenen Tabelle. **Stirbt die Live-Abfrage, darf nicht die ganze Seite sterben.**

| | |
|---|---|
| **Kein allgemeiner Teilerfolg-Mechanismus** | Genau diese zwei Kacheln. Ein Dashboard, das jeden Block einzeln scheitern lassen kann, zeigt irgendwann eine Seite voller Lücken und nennt das eine Antwort |
| **Auch die Erscheinungsbedingung bekommt keinen** | Sie liest **Stammdaten** und ist damit dieselbe Art Zugriff wie die Mandantenkette in jedem anderen Statement. Ein dritter Block mit eigenem Ausfall wäre der Anfang genau dieser Seite |
| **Gefangen wird genau eine Ausnahme** | `DataAccessException` **mit** `SQLTimeoutException` als Ursache. Ein Syntaxfehler, eine abgerissene Verbindung oder ein fehlendes Recht bleiben technische Fehler mit `500`. **Ein pauschales `catch` machte aus jedem Bruch eine Beruhigung** |
| **Die beiden Kacheln fallen *nicht* zusammen** | Und das ist der Unterschied zur alten Kachel *Überfällig*: Dort waren „im Zeitraum" und „insgesamt" ein **Paar**, das man nebeneinander liest, und eine Zahl ohne die andere lud zu einer Rechnung ein, die nicht aufgeht. *Läuft* und *Wartend* sind **zwei verschiedene Auskünfte**. Fällt eine, steht die andere |
| **`ermittelbar: false` ist nicht `0`** | Null hieße „es läuft nichts". In einem Überwachungswerkzeug ist das die schlimmste falsche Antwort |

`DashboardZeitgrenzeTest` stellt beide Fälle her — den Abbruch an der Zeitgrenze und den
Syntaxfehler — und prüft, dass nur der erste geschluckt wird.

### Was diese beiden Kacheln **nicht** beantworten

**„Hängt hier etwas zu lange?"** Das war die Frage von *Überfällig*, und sie ist mit E‑71
unbeantwortet geblieben. *Läuft* und *Wartend* zählen einen **Zustand** und behaupten kein Problem;
das Alter der ältesten Zeile steht daneben, **ohne eine Schwelle**. Eine Schwelle zu erfinden
verbietet Regel Q4 — `MessageTimeout` ist es nachweislich nicht. **Offener Punkt 130.**

---

> ## ⚠️ Der Stand bis zum 03.09.2026 — die Kachel *Überfällig*
>
> **Er bleibt wortgleich stehen.** Ohne ihn wäre nicht mehr nachlesbar, dass die erste benannte
> Ausnahme von L2 einmal eine andere war, und woran sie gescheitert ist.
>
> > ### 5. Überfällig — die erste benannte Ausnahme von L2
> >
> > Zwei Zahlen, **beide live** über `Message`, beide über
> > `MessageStatusClassifier.ueberfaelligBedingung` (gerufen, nicht nachgebaut), `jetzt` aus der
> > **Anwendungsuhr** (Regel Z1):
> >
> > - **im Fenster** (E‑h) — dieselbe Zahl, die der Klick in die Liste liefert
> > - **insgesamt** — ohne Zeitfenster
> >
> > **Das Wort „ausschließlich" in L2 bleibt stehen, und daneben steht diese Ausnahme** (E‑c vom
> > 24.08.2026). Sie ist einzeln begründet und einzeln gemessen.
> >
> > **Die zweite Zahl hat kein Zeitfenster, und das ist durch Regel L9 gedeckt:** Gefragt ist genau,
> > was *außerhalb* des gezeigten Zeitraums hängt — ein Fenster schnitte die Zeilen weg, um die es
> > geht. Sie ist dabei die **billigere** von beiden (M90, Befund 14).
> >
> > **In der Anzeige trägt „überfällig" keine Farbe von *Fehler*.** Rot gehört ausschließlich der
> > Kategorie *Fehler*; die eigene Farbrolle (orange) ist in
> > [`visuelles-konzept.md`](visuelles-konzept.md) §7a entschieden.
>
> **Was daran gefallen ist und warum:** Die Problemkategorie *Überfällig* ist am 03.09.2026 durch
> eine fachliche Auskunft des Auftraggebers widerlegt (E‑71). Sie markierte auf der Testkopie 538
> Zeilen — **538 `SUSPENDED`, 0 `RUNNING`, also 538 Fehlalarme und kein Treffer.** Vollständig samt
> Herkunftsvermerk in `PROJEKTBESCHREIBUNG.md` §4.2 Punkt 2 und
> [`message-status.md`](message-status.md).
>
> **Was daran unberührt bleibt, und es ist der größere Teil:** die Bauform. Live über `Message`,
> ohne Zeitfenster nach L9, mit `EXISTS` als Mandantenkette, mit dem engen `catch` auf die
> Zeitgrenze und mit `ermittelbar` als eigenem Feld. **Die beiden neuen Kacheln erben sie
> vollständig** — gefallen ist die Kennzahl, nicht ihr Bau.
>
> **Die Berichtigung vom 31.08.2026 zur Farbrolle** — dass sie am Tag jenes Satzes noch nicht
> entschieden war — steht weiterhin in [`visuelles-konzept.md`](visuelles-konzept.md) §7a und ist
> von E‑71 nicht berührt. Die Rolle `--ueberfaellig` bleibt dort bestehen, **ohne Verbraucher**
> (E‑77).

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

Der Abbruch im Wortlaut, aus dem Messlauf vom 31.08.2026:

```
org.jooq.exception.DataAccessException: SQL [select … from `GlassfishDB`.`Message`
  left outer join `GlassfishDB`.`SOS` … where ((`MessageStatus` like 'ERROR\_%' escape '\'
  or `MessageStatus` = 'COMMIT_REJECTED') and `MessageLastUpdate` >= '2025-01-01 00:00:00'
  and `MessageLastUpdate` < '2026-01-01 00:00:00' and exists (… `MandantID` = 'VOTG'))
  order by `MessageLastUpdate` desc, `MessageID` desc fetch next 10 rows only];
(conn=98969) Query execution was interrupted (max_statement_time exceeded)
```

**Der Grund ist die Deckelung.** `ORDER BY … LIMIT 10` ist nur billig, wenn die zehn Zeilen früh
gefunden werden. Ein Mandant **ohne** Fehler im Fenster zwingt die Datenbank, den ganzen Bereich zu
durchsuchen, bevor sie „nichts" sagen darf — **gerade der gute Fall ist der teure**.

> ### ⚠️ Korrektur vom 03.09.2026 — die Begründung der Disjunktheit ist gegenstandslos
>
> **Der Abschnitt darunter bleibt vollständig stehen.** Sein Befund gilt unverändert: Mit einem
> gemeinsamen `OR` steigt MariaDB über den Zeitindex ein, und der Indexhinweis dreht den Plan um.
> **Gegenstandslos ist genau ein Argument** — das der Disjunktheit.
>
> Es lautete: *Aus zweimal zehn neuesten Zeilen sind die zehn neuesten dieselben wie aus einer
> gemeinsamen Abfrage, weil die beiden Mengen disjunkt sind (Fehler ist Endstatus, überfällig setzt
> das Gegenteil voraus).* **Das war richtig und wird nicht mehr gebraucht:** Mit E‑71 ist die
> Überfälligkeitshälfte entfallen, es gibt nur noch **eine** Menge, keine Zusammenführung und keine
> Nachsortierung.
>
> **Der Indexhinweis bleibt, und seine Messung auch.** Er hing nie an der zweiten Hälfte, sondern an
> der Deckelung: `ORDER BY … LIMIT 10` ist nur billig, wenn die zehn Zeilen früh gefunden werden.
> Das gilt für die Fehlerhälfte allein genauso.
>
> **Gemessen nach dem Umbau** (M145): Der Block kostet **21,9 bis 26,1 ms** über alle sechs
> Kombinationen — vorher 29,7 bis 34,1 ms für **beide** Hälften zusammen. Er bleibt von der
> Fensterbreite unabhängig, und das war der Zweck.

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

1. ~~**Die Liste kann diese Frage gar nicht beantworten.**~~ **Entfallen am 03.09.2026 (E‑71):**
   Den Parameter `ueberfaellig` gibt es nicht mehr, und mit ihm nicht die `400`. **Der zweite Grund
   trägt allein — und hat immer allein getragen.** Der Satz bleibt im Wortlaut stehen: Dort sind
   `status=FEHLER` und
   `ueberfaellig=true` ausdrücklich **unvereinbar** und ergeben `400`
   (`ueberfaellig-und-status-unvereinbar`): Überfällig setzt `WARTEND` oder `LAEUFT` voraus, Fehler
   ist ein Endstatus. Über die Liste bräuchte der Block **zwei** Aufrufe, ein Zusammenführen und
   eine Neusortierung — also genau das, was jetzt dasteht, nur mit zwei HTTP-Schichten dazwischen.
2. **Fachpakete kennen einander nicht.** `dashboard` darf nicht aus `message` importieren
   (`PaketstrukturTest.fachpakete_kennen_einander_nicht`); braucht ein zweites Fachpaket einen Typ,
   **wandert der Typ nach `common`**.

**Wiederverwendet ist damit genau das, was driften könnte:** die Fehlerbedingung, aus
`common/MessageStatusClassifier`. **Nachgebaut ist nichts.** *(Bis zum 03.09.2026 stand hier „die
Fehlerbedingung und die Überfälligkeitsbedingung"; die zweite ist mit E‑71 entfallen.)* Die
Mandantenkette schreibt ohnehin jedes Fachpaket selbst — sie kann nicht nach `common` wandern, weil
dort keine `jooq.glassfish`-Typen stehen dürfen.

~~**Die Fensterverengung fällt dabei nicht weg, sie greift ohnehin nicht:** Für `ueberfaellig` ist sie
abgeschaltet, weil der Rollup keine Frist kennt.~~ **Gegenstandslos seit dem 03.09.2026:** Das
Merkmal `UEBERFAELLIG` ist mit E‑71 aus `Abfragemerkmal` entfallen. Es war das **einzige**, das der
Rollup aus einem Grund nicht mittragen konnte, der am *Bestand* lag; übrig bleibt `SUCHBEGRIFF`, und
dessen Grund liegt am *Schema* ([`nachrichtenliste.md`](nachrichtenliste.md) §5d).

> **Aus demselben Grund ist `Pflegestatus` von `catalog` nach `common` gewandert:** Der
> Verteilungsblock braucht `GEPFLEGT`. Die Alternative wäre ein Literal `"GEPFLEGT"` im Dashboard
> gewesen — dieselbe Bedingung an zwei Stellen, und die driftet.

---

## 8. Die Messung — M108 *(31.08.2026)*, überholt durch M145 *(03.09.2026)*

> ### ⚠️ M108 ist für drei Zeilen überholt und für den Rest gültig
>
> **Die ganze Messung bleibt stehen.** Überholt sind genau die Zeilen, deren Statements es nicht
> mehr gibt: *Überfällig — im Fenster*, *Überfällig — insgesamt* und *Zuletzt aufgefallen (beide
> Statements)*. **M145 misst, was an ihrer Stelle steht**, und dazu die ganze Landingpage neu.
>
> #### Die neuen Statements (M143)
>
> | Statement | `NEXANS` | `SUTTONS` | Plan |
> |---|---:|---:|---|
> | Kachel *Läuft* | **1,09–1,25 ms** | **0,94–1,04 ms** | `Message`, `ref` über `MessageStatusIDX`, `key_len 123`, `rows = 1` |
> | Kachel *Wartend* | **4,40–4,66 ms** | **3,34–3,55 ms** | dasselbe, `rows = 538` |
> | Erscheinungsbedingung | **1,24–1,40 ms** | **1,40–1,42 ms** | Einstieg über `ProjectMandant_Mandant_idx`, dann `Process_ProjectFK`, `SOS_ProcessFK`, `SOSAction.PRIMARY` — alle `ref` |
>
> **`ref` statt `range`, und das ist der vorregistrierte Befund.** Erwartet war `range` über
> `MessageStatusIDX` wie bei der alten Kachel. Herausgekommen ist **`ref` über denselben Index** —
> die erwartbare Folge davon, dass das neue Statement mit `=` auf **einen** Rohwert vergleicht, wo
> das alte ein `IN` über zwei trug. **Derselbe Index, engerer Zugriff, kein Rückschritt.**
> `DashboardPlanDbIT` schreibt deshalb den **Index** fest und nicht die Zugriffsart.
>
> #### Die ganze Landingpage (M145) — und sie ist für einen Mandanten teurer geworden
>
> | Paar | `NEXANS` M108 → M145 | `SUTTONS` M108 → M145 |
> |---|---:|---:|
> | 48 h | 62,227 → **49,5–57,7 ms** | 72,007 → **49,5–53,4 ms** |
> | 30 Tage | 152,814 → **149,4–149,8 ms** | 109,829 → **116,0–119,5 ms** |
> | 12 Monate | 199,030 → **194,2–195,0 ms** | 127,038 → **139,7–144,6 ms** |
> | ohne `zeitraum` | 64,706 → **59,2–59,9 ms** | 77,957 → **55,1–59,9 ms** |
>
> **Das Budget von 500 ms ist in jeder Kombination weit unterschritten** — die teuerste überhaupt
> mögliche liegt bei 195 ms.
>
> ##### ⚠️ Die vorregistrierte Erwartung trifft nicht zu, und zwar aus zwei Gründen
>
> Erwartet war eine **Verbesserung** für beide Mandanten. Eingetreten ist sie für `NEXANS`
> (−4 ms bei zwölf Monaten) und **nicht** für `SUTTONS` (+13 bis +18 ms). Zwei Dinge sind
> auseinanderzuhalten:
>
> **1. Die Rechnung der Vorregistrierung war falsch, und der Fehler ist meiner.** Dort stand: *„es
> fällt weg: die Überfälligkeitshälfte von ‚Zuletzt aufgefallen' — **29 bis 34 ms** (M108)."* Die
> 29–34 ms sind in M108 aber die Kosten **beider Hälften zusammen** — die Zeile heißt dort
> ausdrücklich *„Zuletzt aufgefallen (beide Statements)"*. **Die Hälfte, die wegfällt, ist rund
> 8–10 ms wert, nicht 29–34.** Der eigene Beitrag dieses Schritts ist damit:
>
> | | |
> |---|---:|
> | fällt weg: zwei Überfällig-Statements | **−9,7 ms** (`SUTTONS`, M108) |
> | fällt weg: die zweite Hälfte von Block 6 | **−8 bis −10 ms** |
> | kommt hinzu: drei neue Statements | **+5,8 ms** (M143) |
> | **netto** | **rund −13 ms** |
>
> **2. Die Mehrkosten bei `SUTTONS` liegen in einem Block, den dieser Schritt nicht anfasst.**
> Aufgeschlüsselt:
>
> | Block, `SUTTONS` 12 Monate | M108 | M145 |
> |---|---:|---:|
> | Verlauf | 39,687 ms | **39,650 ms** |
> | **Verteilung** | 40,223 ms | **67,253 ms** |
> | Zuletzt aufgefallen | 34,084 ms (beide) | **24,978 ms** (eine) |
>
> **Die Verteilung ist um 27 ms teurer geworden, und an ihr ist keine Zeile geändert worden.**
>
> > **Belegvermerk** (Regel L10).
> > *Gemessen ist:* die Laufzeit je Block, zweimal, ein Aufwärmlauf und die beste von fünf; dazu
> > der Bestand — `message_rollup` trägt unverändert **335.610** Zeilen, der letzte Volllauf
> > stammt vom **31.08.2026**, also demselben Tag wie M108, und `SUTTONS` hat **keine einzige**
> > Katalogzeile (die siebzehn aus offenem Punkt 58 sind nicht mehr da).
> > *Behauptet wird:* Die Mehrkosten stammen **nicht** aus diesem Schritt.
> > **Die Lücke:** Woher sie *stammen*, ist **nicht gemessen**. Weder Datenmenge noch Katalogstand
> > noch Rollup-Stand haben sich geändert; bleibt der Zustand der Instanz (Puffer, Fremdlast). Das
> > ist **plausibel und nicht belegt**, und es wird hier nicht als Ursache behauptet. Offener Punkt
> > **134**.
>
> **Was das für die Abnahme heißt:** Das Budget hält mit Faktor 2,6 Luft, und der eigene Beitrag des
> Schritts ist gemessen negativ. Die Drift im Verteilungsblock ist ein **eigener** Befund und wird
> nicht in diesem Schritt geheilt.

### Die ursprüngliche Messung im Wortlaut — M108 *(31.08.2026)*

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

### Die Pläne (Regel L7)

| Statement | Zugriffspfad auf die Quelle |
|---|---|
| Verlauf 48H | `message_rollup`, `range` über `PRIMARY`, `key_len 5`, `Using temporary; Using filesort` |
| Verlauf 30T | `message_rollup_tag`, `range` über `PRIMARY`, `key_len 3` |
| Verlauf 12M | `message_rollup_monat`, `range` über `PRIMARY`, `key_len 3` |
| Verteilung, alle drei | wie der Verlauf, plus `process_catalog` als `eq_ref` über `PRIMARY` |
| Belegung, alle drei | derselbe Bereich, gekapselt in einer abgeleiteten Tabelle |
| ~~Überfällig, beide~~ | *entfallen (E‑71). An ihrer Stelle:* |
| **Läuft** | `Message`, **`ref`** über **`MessageStatusIDX`**, `key_len 123`, `rows = 1` |
| **Wartend** | `Message`, **`ref`** über **`MessageStatusIDX`**, `key_len 123`, `rows = 538` |
| **Erscheinungsbedingung Wartend** | `ProjectMandant` `ref` über `ProjectMandant_Mandant_idx` → `Process` → `SOS` → `SOSAction`, alle `ref` über Index; **keine Tabelle wird voll gelesen** |
| Zuletzt aufgefallen, Fehlerhälfte | `Message`, `range` über **`MessageStatusIDX`**, `rows = 6.257` |
| ~~Zuletzt aufgefallen, Überfälligkeitshälfte~~ | *entfallen (E‑71)* |
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
| `DashboardIsolationDbIT` | **Regel M4, Pflicht.** Seit 10b‑4 **je neuem Statement einzeln**: *Wartend* zeigt für zwei Mandanten verschiedene Zahlen (fiele der Filter, sähen beide den ganzen Bestand); die Erscheinungsbedingung trennt `NEXANS`/`VOTG` von `SUTTONS`; *Läuft* ist lokal **nicht** prüfbar, weil `RUNNING` null Mal vorkommt — dort trägt der Statementtest.  Zwei Zusicherungen tragen den Nachweis: **verschiedene Summen** über dasselbe Fenster (fiele der Filter, sähen beide die Zahl des ganzen Bestands) und **jede gezeigte Prozesskennung gehört dem eigenen Mandanten**. Dazu: keine fremde Kennung im Rumpf, kein Mandantenparameter, Standardfenster und Leerzustand, alle Blöcke in einer Antwort |
| `DashboardServiceTest` | Einordnung, Fehlerarten, die beiden Restzeilen, „nicht ermittelbar", Standardfenster, Leerzustand — **ohne Datenbank**, alle Prüfwerte erfunden |
| `DashboardStatementsTest` | Das **gerenderte** SQL: `EXISTS` statt `JOIN`, `CASE` ohne Alias, `LEFT JOIN` auf den Katalog, keine Funktion um den Eimerschlüssel. **Seit dem 03.09.2026 benennt er die sieben Statements einzeln, statt sie zu zählen** — in 10b‑4 sind drei weggefallen und drei hinzugekommen, die Zahl blieb sieben, und ein zählender Test hätte bestanden, ohne noch etwas zu bezeugen. Dazu ein Verbot: **kein Statement der Seite rechnet noch mit `MessageTimeout`** |
| `DashboardPlanDbIT` | **`EXPLAIN`, Treibertabelle und Index — keine Zeitmessung.** Der Wächter über den Befund aus §7a. Seit 10b‑4 zusätzlich: beide neuen Kacheln über `MessageStatusIDX`, und die Erscheinungsbedingung liest **keine Tabelle voll** — auch `SOSAction` nicht |
| `DashboardZeitgrenzeTest` | Der Abbruch an der Zeitgrenze wird zu „nicht ermittelbar", **jeder andere Fehler nicht** — seit 10b‑4 an den Kacheln *Läuft* und *Wartend*, und mit der Gegenprobe, dass auch die **Erscheinungsbedingung** nichts abfängt |
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

## 9a. Die Entscheidungen aus Schritt 10b‑4 *(03.09.2026)*

**Sieben, und die erste trägt die übrigen sechs.** Die Nummern sind nach Regel V1 vor der Vergabe
erhoben worden: Die höchste tatsächlich vergebene war **E‑70** ([`dunkelmodus.md`](dunkelmodus.md)),
nicht E‑56 wie im Auftrag angenommen.

| Kennung | Entscheidung |
|---|---|
| **E‑71** | ***Überfällig* ist widerlegt und fällt aus dem MVP** — samt `istUeberfaellig`, `timeoutZeitpunkt`, `ueberfaelligBedingung`, dem Listenparameter `ueberfaellig` und dem Detailfeld. Grundlage ist eine **fachliche Auskunft des Auftraggebers**, keine Messung; der Herkunftsvermerk steht überall dort, wo die Regel geführt wird |
| **E‑72** | **Kachel *Läuft*** — live über `Message`, ohne Zeitfenster, `=` auf den Rohwert `RUNNING`. Erste der zwei neuen benannten Ausnahmen von L2 |
| **E‑73** | **Kachel *Wartend*** — dasselbe für `SUSPENDED`. Zweite Ausnahme |
| **E‑74** | **Die Erscheinungsbedingung ist strukturell** — über `SOSAction`, nicht über die Zahl und nicht über den Rollup. `SUSPEND` und nicht `WAITUNTIL`, **weil M144 es so gemessen hat** |
| **E‑75** | **`aeltesteSekunden` an beiden Kacheln** — als Fenstergrundlage **und** als laufende Prüfung der Auskunft aus E‑71 (§5) |
| **E‑76** | **`fristSekunden` ist bei `WARTEND` `null`** — eine Frist, die niemand durchsetzt, ist eine falsche Auskunft. Bei `RUNNING` bleibt sie und wird erst dadurch richtig ([`nachrichtendetail.md`](nachrichtendetail.md) §3a) |
| **E‑77** | **`--ueberfaellig` bleibt ohne Verbraucher bestehen** — die Farbrolle ist gerechnet und gegengeprobt und wird an dem Tag gebraucht, an dem eine echte Schwelle zurückkommt ([`visuelles-konzept.md`](visuelles-konzept.md) §7a). Dasselbe gilt im Backend für `TIMEOUT_EINHEIT` |

> ### Was an dieser Runde ungewöhnlich ist, und es gehört benannt
>
> **Sechs dieser sieben Entscheidungen hängen an einem Satz, der nicht gemessen ist.** Das Projekt
> hat bisher jede tragende Aussage entweder gemessen oder als ungedeckt gekennzeichnet — hier fällt
> eine gebaute, getestete und gemessene Kategorie **auf eine Auskunft hin**.
>
> **Das ist zulässig und trotzdem eine andere Art von Entscheidung.** Regel Q4 verbietet Raten,
> nicht Auskünfte; und für die Frage, was das Altsystem mit einer Nachricht in `RUNNING` tut, ist
> der Auftraggeber die **einzige** verfügbare Quelle — die Testkopie kennt den Status nicht.
>
> **Was daraus folgt, ist die Disziplin drumherum:** der wörtlich gleiche Herkunftsvermerk an jeder
> Stelle, die offene Prüfung gegen die Produktion in
> [`message-status.md`](message-status.md), und `aeltesteSekunden` als Feld, das die Auskunft im
> laufenden Betrieb beobachtbar hält (E‑75).

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
| **130** | **„Hängt hier etwas zu lange?" ist seit E‑71 unbeantwortet.** *Läuft* und *Wartend* zählen einen Zustand und liefern das Alter der ältesten Zeile — **ohne Schwelle**. Eine Schwelle steht nirgends in den Daten; `MessageTimeout` ist es nachweislich nicht (§5). Sie zu erfinden verbietet Regel Q4 — es ist wörtlich die Lage, in der *Unquittiert* mit E‑d gestorben ist. **Als offener Punkt eingetragen und nicht gebaut** |
| **134** | **Der Verteilungsblock ist bei `SUTTONS` um 27 ms teurer geworden, ohne dass eine Zeile daran geändert wurde** (§8). Bestand, Rollup und Katalogstand sind nachweislich unverändert. **Die Ursache ist nicht gemessen**; plausibel ist der Zustand der Instanz, belegt ist er nicht. Wer M145 nachmisst, sieht, ob es bleibt |
| **135** | **Die Isolation der Kachel *Läuft* ist lokal nicht nachweisbar.** `RUNNING` kommt auf der Testkopie null Mal vor; jeder Mandant sieht `0`, mit und ohne Mandantenfilter. Der Nachweis ruht auf dem **gerenderten Statement** (`DashboardStatementsTest`) und nicht auf Daten. **Gegen die Produktion nachzuholen** |

### Und was hier geschlossen wird

*Die beiden ersten Zeilen sind am **03.09.2026 mit Schritt 10b‑5** nachgetragen, nachdem die
Oberfläche nachgezogen war.*

| Nr. | Woher | Stand |
|---|---|---|
| **131** | [`nachrichtenliste.md`](nachrichtenliste.md) §5e | **erledigt** — die Landingpage war zwischen dem 03.09.2026 und Schritt 10b‑5 im Browser defekt, weil sie `kacheln.ueberfaellig` las. Sie liest jetzt `laeuft` und `wartend`, der Verweis auf die entfallene Abfrageform ist fort, und ein alter Link `?ueberfaellig=true` lädt ohne Fehler ([`dashboard-frontend.md`](dashboard-frontend.md) §5.4) |
| **133** | dieser Datei, `Auffaelligkeit` | **erledigt** — die Aufzählung hat weiterhin **einen** Wert, und das Feld `kategorie` bleibt im Vertrag (Regel Q3). Was den Punkt offen hielt, war die Oberfläche: Sie las ihn und durfte nicht an einem fehlenden Feld brechen. **Sie zeichnet ihn seit 10b‑5 nicht mehr** — eine Plakette, die an jeder Zeile dasselbe Wort sagt, unterscheidet nichts. Was daraus für die Anzeige folgt, ist als Punkt **136** in [`dashboard-frontend.md`](dashboard-frontend.md) §9 benannt |
| **61** | [`messungen-schritt10b.md`](messungen-schritt10b.md) | **erledigt** — die Schwelle ist festgelegt, beide Bedingungen sind gebaut, und die zweite ist als gewählte und nicht gemessene Zahl benannt (§3) |
| **62** | [`messungen-schritt10b.md`](messungen-schritt10b.md) | **erledigt** — der Block verträgt fehlende Restzeilen und hält beide unten (§4) |
| **58** | [`messungen-schritt10b.md`](messungen-schritt10b.md) | **berührt, nicht erledigt.** Er verlangt, `SUTTONS` vor der Abnahme von 10b zu kuratieren. Die Katalogpflege ist mit Teil A freigegeben ([`prozess-katalog.md`](prozess-katalog.md) §1); dass sie geschieht, ist eine Handlung des Auftraggebers und keine Codeänderung |

> ### Nachgesehen und **nicht** als Punkt eingetragen *(03.09.2026, Schritt 10b‑5)*
>
> **Was liefert der Endpunkt, wenn die *Erscheinungsbedingung selbst* nicht auswertbar ist?** Fiele
> der Schlüssel `wartend` dann weg, würde ein Ausfall stillschweigend in die strukturelle Behauptung
> *„dieser Mandant wartet nie"* übersetzt — der schlimmste der drei denkbaren Fehler an dieser
> Stelle ([`dashboard-frontend.md`](dashboard-frontend.md) §5.4, E‑81).
>
> **Er tut es nicht, und §5 sagt es ausdrücklich:** Die Erscheinungsbedingung bekommt **keinen**
> Teilerfolg-Mechanismus. Sie liest Stammdaten und ist damit dieselbe Art Zugriff wie die
> Mandantenkette in jedem anderen Statement; bricht sie, ist die **ganze Antwort** ein Fehler.
> **Kein offener Punkt** — die Frage ist beantwortet, bevor sie die Oberfläche erreicht.

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

   > **Nachtrag 03.09.2026:** Sie war es nicht. Die Frage ist am 03.09.2026 durch eine fachliche
   > Auskunft des Auftraggebers entschieden worden — **gegen die Kategorie** (E‑71). Der Satz oben
   > bleibt stehen: Er benennt genau die Lücke, die diese Messrunde nicht schließen konnte, und die
   > geschlossen worden ist, ohne dass eine Messung sie geschlossen hätte.
6. **Nicht, ob die Auskunft stimmt, die *Überfällig* gestürzt hat.** Sie ist **nicht gemessen**;
   die offene Prüfung dafür steht in [`message-status.md`](message-status.md).
