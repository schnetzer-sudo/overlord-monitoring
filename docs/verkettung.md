# Verkettung

Entsteht in Schritt 6. **Teil 1 (Backend)** ist §1 bis §7, **Teil 2b (Oberfläche)** ist §8.

**Teil 2a ist am 11.08.2026 erledigt** und steht dort, wo er hingehört — bei der Liste, die er
anfasst: der Wegfall des Ausblende-Schalters samt `GET /api/nachrichten/merkmale`
([`nachrichtenliste.md`](nachrichtenliste.md) §5), die Aufteilung von `ZWISCHENSCHRITT` in
`AUFGETEILT` und `ZUSAMMENGEFUEHRT` ([`message-status.md`](message-status.md)) und die Statuszelle
ohne Präposition ([`nachrichtenliste.md`](nachrichtenliste.md) §8.1).

**Teil 2b ist am 11.08.2026 dazugekommen** und steht in §8: die Kettenfläche im Detailpanel und die
Navigation entlang der Kette. **Kein Feld ist dafür am Backend ergänzt worden — bis auf eines:**
`rollen` im Kopf des Detail-Endpunkts (§8.1). Der Hinweis auf die Verzögerung des
`MatchInterchange`-Events ist **nicht** entstanden; er steht als offener Punkt in §11.

Der Ketten-Endpunkt beantwortet die dritte der drei Fragen des Werkzeugs: nicht *„wo steht mein
Beleg"* (das ist [`nachrichtenliste.md`](nachrichtenliste.md)) und nicht *„was ist im Einzelnen
passiert"* (das ist [`nachrichtendetail.md`](nachrichtendetail.md)), sondern **„was hängt daran"**.

Grundlage ist die Erhebung [`messungen-schritt6.md`](messungen-schritt6.md) — M23 bis M28, die
ergänzenden E1 bis E5 und der Messblock **M30**, der vor diesem Teil erhoben wurde. Wo unten eine
Zahl steht, steht dort ihr Statement.

---

## 1. Warum es zwei Beziehungen sind und nicht eine

Die Verkettung steht in **vier** Spalten von `Message`. Sie sind **nicht** vier Sichten auf dieselbe
Beziehung, sondern **zwei Beziehungen mal zwei Richtungen** (M25‑2):

| Angabe | Steht auf | Bedeutet | Beziehung |
|---|---|---|---|
| `SourceMessageID` | dem **Kind** | „mein Elternteil ist …" | Aufteilung |
| `Source` | der **Wurzel** | „ich habe Kinder" | Aufteilung, Gegenrichtung |
| `TargetMessageID` | dem **Merge-Eingang** | „ich bin zusammengeführt worden nach …" | Zusammenführung |
| `Target` | dem **Merge-Ergebnis** | „ich bin aus einer Zusammenführung entstanden" | Zusammenführung, Gegenrichtung |

**„Eine Spalte genügt" war nie eine Option.** M25‑2 hat gemessen: In 432 von 432 Fällen trägt der
Merge-Nachfolger keinen Rückverweis, in 4.096 von 4.096 Fällen trägt der Split-Elternteil keinen
Vorwärtsverweis. Welche der beiden man wählte, verlöre die Hälfte der Fälle.

**Die Flags sind der kostenlose Rückwärtsindex** (E4): `Source = 1` genau dann, wenn mindestens ein
Kind existiert (479/479, und 0 von 5.770 mit `Source = 0`); `Target = 1` genau dann, wenn eine
`MERGED`-Zeile auf die Nachricht zeigt (6/6, 0 von 6.243). Ob eine Nachricht überhaupt eine Kette
hat, steht damit **auf der Zeile selbst** — ohne Abfrage. Das ist die Grundlage für die *Liste*, die
diese Frage je Zeile beantworten muss; **im Ketten-Endpunkt wird die Abkürzung bewusst nicht
genommen** (§4).

**Der Status sagt über die Stellung nichts Verlässliches.** Meist trägt die Wurzel `SPLITTED` und
das Kind `FINISHED` — bei `IBIS`, `IBISGUS` und `ZAST` trägt die Wurzel `FINISHED`, und das sind
gerade die Mandanten, die über den ganzen Bestand **keine einzige** Zwischenschritt-Zeile haben
(M24‑3). Verlässlich ist die Spalte, nicht `MessageStatus`.

---

## 2. Die beiden Endpunkte

```
GET /api/nachrichten/{messageId}/kette
GET /api/nachrichten/{messageId}/kette/abwaerts?cursor=…&limit=…
```

> ### Umbenannt am 11.08.2026 — und warum
>
> Bis heute hießen die beiden Listen `vorgaenger` und `nachfolger`, und der zweite Endpunkt hieß
> `…/kette/nachfolger`. **Diese Namen benannten eine Bedeutung, und die stimmt nur bei der
> Aufteilung** (§8.3): Beim Merge-Eingang steht im `vorgaenger` das *Ergebnis*, beim Merge-Ergebnis
> stehen im `nachfolger` seine *Eingänge*.
>
> **Der Anlass ist ein tatsächlicher Fehler.** Die Aufgabenstellung zu Teil 2b hat die Feldnamen für
> die Bedeutung genommen und dabei bei **38.628 Zeilen** (M30‑4, Fenster B) das Gegenteil dessen
> behauptet, was passiert ist. Die Umsetzung hat es gemerkt und die Bedeutungsspalte gewählt —
> **korrigiert wurde damit die Oberfläche, nicht die Ursache.**
>
> Ein Name, der in fünf von sechs Fällen stimmt, ist gefährlicher als einer, der nichts behauptet.
> Dasselbe Argument hat in diesem Projekt schon einmal gewonnen: `monitor_root` heißt seit dem
> 28.07.2026 `monitor_write`, weil der nächste Kollege den Namen liest und nicht die Dokumentation.
> Hier kommt hinzu, dass der nächste Leser **kein Kollege** ist: Ausbaustufe 1
> ([`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §10) hängt den Chatbot an genau diese
> Endpunkte, und ein Werkzeugfeld namens `vorgaenger` bringt jedes Modell dazu, bei jedem
> Merge-Eingang die Flussrichtung umzudrehen. Dort steht dann keine Sichtprüfung mehr dazwischen.
>
> **Seither benennt die API den Mechanismus**, und die Bedeutung tragen `beziehung` und `ebene` —
> also genau die Felder, die die Oberfläche ohnehin auswertet.
>
> | bisher | neu |
> |---|---|
> | `vorgaenger` | `aufwaerts` |
> | `nachfolger` | `abwaerts` |
> | `nachfolgerGesamt` | `abwaertsGesamt` |
> | `…/kette/nachfolger` | `…/kette/abwaerts` |
> | — | `abwaertsCursor` (**neu**, siehe unten) |
>
> **Der alte Pfad ist entfernt und nicht als Weiche behalten.** Zwei Pfade auf dasselbe wären genau
> die Drift, gegen die die Umbenennung antritt. Es gibt genau **einen** Konsumenten, im selben
> Repository; später kostet dieselbe Änderung ein Vielfaches. Die Einteilung nach der Flussrichtung
> (§8.3) und die sichtbaren Beschriftungen „Kommt von" und „Wurde zu" sind **unverändert** — sie
> waren nie das Problem.

Angemeldet, **Mandant aus der Sitzung**. Beide nehmen eine `MessageID` entgegen und **keine
Mandanten-ID** (Regel M1); die Ausnahmeliste in [`mandantentrennung.md`](mandantentrennung.md) §3
bleibt bei zwei Einträgen und wächst hier nicht.

**Kein Zeitfenster.** Regel L1 gilt für *Listen* über `Message`. Hier ist die Menge durch einen
Primärschlüssel benannt — dieselbe Begründung wie beim Detail-Endpunkt. Beim Blätter-Endpunkt
wäre ein Fenster sogar aktiv falsch: Es schnitte gerade die Kinder ab, die außerhalb liegen.

### Antwort des Ketten-Endpunkts

```json
{
  "messageId": "…",
  "rollen": ["SPLIT_WURZEL"],
  "aufwaerts": [ … Kettenglied … ],
  "abwaerts": [ … Kettenglied … ],
  "abwaertsGesamt": 27,
  "abwaertsCursor": "eyJ0Ijoi…",
  "weitereVorhanden": true,
  "tiefeErreicht": false,
  "zyklusErkannt": false
}
```

Ein **Kettenglied**:

```json
{
  "messageId": "…",
  "status": "FINISHED",
  "statusKind": "ABGESCHLOSSEN",
  "zeitpunkt": "2025-12-29T10:01:09Z",
  "sosName": "Versand Einzel IDOC aus Split",
  "rollen": ["SPLIT_KIND"],
  "ebene": 1,
  "beziehung": "AUFTEILUNG"
}
```

**Genug für eine Zeile, nicht mehr.** Keine Schrittfolge, keine Eigenschaften, keine BAM-Werte — wer
ein Glied ansehen will, öffnet es, und dafür gibt es den Detail-Endpunkt. Eine Kette mit 50 Gliedern
wäre sonst 50 Detailantworten in einer.

**`statusKind` kommt aus `common/MessageStatusClassifier`.** Hier wird nicht neu klassifiziert — die
Einordnung entsteht an genau einer Stelle im Code und wird nirgends nachgebaut.

**`beziehung` steht ausdrücklich dabei**, obwohl sie aus `rollen` und `ebene` ableitbar wäre: Der
Unterschied zwischen „aufgeteilt" und „zusammengeführt" ist das, was die Oberfläche dem Nutzer sagen
muss, und er soll nicht dort zusammengerechnet werden, wo er bei der nächsten Änderung
auseinanderfällt.

**`rollen` ist immer vorhanden — leer statt fehlend.** Ein fehlendes Feld zwänge jeden Aufrufer zu
einer Fallunterscheidung, die nichts bedeutet.

### Der `abwaertsCursor` — die Position hinter der ersten Seite

*Ergänzt am 11.08.2026.* `/kette` liefert die erste Seite der Abwärtsglieder; bis heute lieferte es
**keine Position**, ab der sich weiterblättern ließe. Der Block holte deshalb beim ersten Nachladen
die erste Seite ein zweites Mal, ersetzte sie durch die inhaltsgleiche Antwort des
Blätter-Endpunkts und zog die zweite Seite hinterher — **zwei Anfragen für einen Klick** (§8.7). Mit
dem Feld ist es eine.

Vier Regeln, und die erste ist die wichtigste:

1. **Der Wert entsteht an derselben Stelle wie die Cursor des Blätter-Endpunkts**
   (`KettenService.position`, kodiert über `Seitenposition.kodiere`). Ein zweiter Kodierer wäre die
   Drift, die das Blättern zerlegt — sie fiele erst auf, wenn eine Seite Zeilen überspringt oder
   wiederholt. `KettenServiceTest` hält Zeichen für Zeichen fest, dass beide denselben Wert liefern.
2. Er zeigt auf die **letzte ausgelieferte Abwärtszeile**, in der Ordnung `(MessageLastUpdate,
   MessageID)`.
3. **`null`, wenn `weitereVorhanden` falsch ist.** Ein Cursor ohne nächste Seite behauptet, es gäbe
   eine.
4. **Sonderfall aus M30‑6:** Trägt die letzte ausgelieferte Zeile keinen `MessageLastUpdate`, hat
   sie in der Ordnung keine Position, und der Cursor ist `null` — auch wenn `weitereVorhanden` wahr
   ist. Gemessen kommt das **0 von 3.341.519** Mal vor. Der Block zeigt in dieser Kombination
   **keine** Schaltfläche zum Nachladen (§11).

> **Belegvermerk** (Regel L10).
> *Gemessen (M30‑6, n = 3.341.519):* **0** Zeilen ohne `MessageLastUpdate`.
> *Behauptet wird:* Regel 4 beschreibt einen Fall, der in der Testkopie **nie** eintritt.
> **Die Lücke:** Dass er nie eintritt, ist gemessen; dass er nie eintreten *kann*, ist es nicht —
> die Spalte lässt `NULL` zu, und die Produktion muss sich nicht an die Testkopie halten. Regel 4
> ist deshalb gebaut und im Einheitstest belegt, nicht im Datenbanktest.

**Der Unterschied zum Blätter-Endpunkt bei Regel 4 ist Absicht.** Dort ist eine ganze Seite ohne
Zeitpunkt ein technischer Fehler (`500`, §2 „Fehlerfälle" berührt das nicht — es ist keine fachliche
Antwort): Der Endpunkt kann seine Zusage, weiterblättern zu lassen, nicht halten. Hier ist die
Antwort vollständig, und nur der Cursor fehlt.

### Antwort des Blätter-Endpunkts

Die einheitliche `Seite`-Hülle aus Richtlinie §5.4:

```json
{ "items": [ … Kettenglied … ], "nextCursor": "eyJ0Ijoi…", "hasMore": true }
```

Cursor-basiert über `(MessageLastUpdate, MessageID)` (Regel L3), **innerhalb der festen Wurzel**.
Seitengröße 50, Maximum wie in der Liste (`NachrichtenFilter.LIMIT_MAXIMUM`).

> **Warum ein eigener Endpunkt und kein Filter an der Liste.** Naheliegend wäre
> `GET /api/nachrichten?wurzel=…`. Das bräche Regel L1: Die Liste verlangt ein Pflicht-Zeitfenster,
> und die Kinder einer drei Monate alten Wurzel lägen außerhalb jedes vernünftigen Fensters. Entweder
> man höhlte L1 für einen Sonderfall aus, oder die Liste lieferte für eine gültige Anfrage nichts.
> Der eigene Endpunkt umgeht beides — und er ist der billigere Zugriff, weil er über
> `SourceMessageIDIDX` einsteigt statt über `MessageLastUpdateIDX` (E5, M30‑1).

### Fehlerfälle

| `type` | Status | Wann |
|---|---|---|
| `nicht-gefunden` | 404 | Die `MessageID` gibt es nicht **oder** sie gehört einem fremden Mandanten |
| `kein-mandant-gewaehlt` | 403 | Kein aktiver Mandant in der Sitzung |
| `cursor-ungueltig` | 400 | Der Cursor ist nicht lesbar |
| `limit-ungueltig` | 400 | Seitengröße außerhalb 1 … 200 |

**Es entsteht kein neuer Problemtyp.** `nicht-gefunden` ist der bestehende aus
`common/error/RessourceNichtGefundenException`, den ein unbekannter Pfad ebenfalls bekommt — genau
damit sich diese Fälle nicht unterscheiden lassen.

---

## 3. Die Auflösung ist bewusst unsymmetrisch

> **Nach oben vollständig, nach unten eine Ebene.**

Das ist keine Geschmacksfrage, sondern die Folge zweier gemessener Kostenkurven:

- **Aufwärts** kostet jede Ebene genau **einen Primärschlüsselzugriff** — `SourceMessageID` bei einem
  Split-Kind, `TargetMessageID` bei einem Merge-Eingang. **Nie beide auf derselben Zeile** (M28‑1:
  `Kind + Eingang` = 0 über beide Bezugsfenster), es gibt je Ebene also genau *einen* Vorgänger. Der
  Aufstieg ist ein **Weg**, kein Baum. Gemessen 0,48 bis 0,51 ms je Ebene (M30‑1).
- **Abwärts** fächert es auf: bis **3.350** Kinder an einer Wurzel und bis **897** Eingänge an einem
  Merge-Ergebnis (M30‑2). Zwei Ebenen abwärts wären im schlechtesten Fall Millionen Zeilen.

Der Nutzer bekommt damit die Herkunft vollständig und die Aufteilung Schritt für Schritt. Weiter nach
unten führt ein neuer Aufruf auf das jeweilige Glied.

**Beide Abwärtsrichtungen kommen in *einer* Liste.** Ein Glied kann beides sein: 25 Zeilen sind
zugleich Split-Wurzel und Merge-Ergebnis (M30‑4). Zwei getrennte Listen zwängen die Oberfläche zu
einer Entscheidung, die das Backend besser trifft — sortiert wird nach `(MessageLastUpdate,
MessageID)`, nicht nach Richtung.

### Die vier Richtungen und ihr Zugriffspfad

Alle vier laufen über einen Index, alle vier tragen den Mandantenfilter. Gemessen am 10.08.2026
gegen die Testkopie (M30‑1), beste von fünf nach einem Aufwärmlauf:

| # | Richtung | Spalte | `EXPLAIN` | Normalfall | Breitfall |
|---|---|---|---|---:|---:|
| 1 | mein Elternteil | `SourceMessageID` → PK | `const` | **0,505 ms** | — |
| 2 | meine Kinder | `SourceMessageIDIDX` rückwärts | `ref`, `Using filesort` | **0,653 ms** | **25,995 ms** (n = 3.048) |
| 3 | mein Merge-Ergebnis | `TargetMessageID` → PK | `const` | **0,482 ms** | — |
| 4 | meine Merge-Eingänge | `TargetMessageIDIDX` rückwärts | `ref`, `Using filesort` | **0,596 ms** | **5,659 ms** (n = 749) |
| 5a | Zählung der Kinder | `SourceMessageIDIDX` | `ref` | **0,544 ms** | **19,760 ms** (n = 3.350) |
| 5b | Zählung der Eingänge | `TargetMessageIDIDX` | `ref` | **0,505 ms** | **4,904 ms** (n = 897) |

> **Die Spalte „Breitfall" nennt je Zeile ihr eigenes `n`, und die Bezugszeile ist nicht überall
> dieselbe.** Für die beiden Seitenabfragen (2 und 4) ist es die breiteste Zeile des **Bezugsmonats**
> — 3.048 Kinder, 749 Eingänge —, für die beiden Zählungen (5a und 5b) die breiteste des **gesamten
> Bestands** — 3.350 und 897. Das ist Absicht: An den Zählungen hängt die Entscheidung „genaue Zahl
> statt ‚mehr als 50'", und die muss am schlimmsten Fall des Bestands hängen und nicht am schlimmsten
> eines Monats. Beide Bezugszeilen sind in M30 auch jeweils andersherum gemessen.

**1 und 3 sind dasselbe Statement**, nur mit verschiedener Herkunft des Schlüssels. Zwei Methoden
wären zwei Statements mit identischem Text.

**Der Aufstieg bleibt linear in der Tiefe:** drei Ebenen kosten zusammen 1,441 ms, zehn Ebenen —
die Grenze — hochgerechnet unter 5 ms. Jeder Schritt ist ein `const`-Zugriff und sieht die anderen
nicht.

**Der teuerste denkbare Ketten-Aufruf** — breiteste Wurzel des Bestands, Seite plus Zählung plus die
Merge-Richtung plus drei Aufstiegsschritte — bleibt unter **55 ms**.

### Zwei Befunde, die man kennen muss

**Das `LIMIT` begrenzt die Ausgabe, nicht die Arbeit.** `SourceMessageIDIDX` steht auf
`SourceMessageID` und liefert die Sortierreihenfolge nicht mit; das `Using filesort` im Plan bedeutet,
dass jede Trefferzeile gesehen werden muss, **bevor** das `LIMIT` greifen kann. Wer 3.048 Kinder hat,
zahlt 26 ms — ob er 50 oder 3.048 Zeilen bekommt. Das ist tragbar (Größenordnung eines
Listenaufrufs, weit unter der Zeitgrenze von 10 s auf dem Lese-Pool) und trotzdem kein Grund, die
Grenze zu streichen: Sie begrenzt die **Antwort**, und 3.350 Zeilen in einem JSON-Rumpf sind
unabhängig von der Datenbank ein Problem.

**Die Breitengrenze greift beim Merge zehnmal häufiger als beim Split.** 11,38 Prozent der
Merge-Ergebnisse haben mehr als 50 Eingänge, gegen 1,07 Prozent der Wurzeln (M30‑2). Für die
Oberfläche heißt das: `weitereVorhanden` ist beim Merge der **Regelfall** und nicht der Ausnahmefall,
den der Split nahelegt.

---

## 4. Die drei Grenzen

| Grenze | Wert | Verhalten beim Erreichen |
|---|---|---|
| **Tiefe aufwärts** | 10 Ebenen (`KettenService.TIEFE_GRENZE`) | `tiefeErreicht: true`; die Kette wird **nicht** als vollständig ausgegeben |
| **Breite abwärts** | 50 Glieder (`KettenService.BREITE_GRENZE`) | `weitereVorhanden: true` plus `abwaertsGesamt`; weiter über `abwaertsCursor` |
| **Zyklus** | eine bereits besuchte `MessageID` | Abbruch, `zyklusErkannt: true` |

**Der Zyklus wird vor der Tiefe geprüft.** Träfen beide zu, wäre „tief" die schwächere Auskunft: Sie
beschreibt, wo abgebrochen wurde, nicht warum. Genau dafür gibt es zwei Felder und nicht eines.

**Zwei der drei Grenzen sprechen in der Testkopie nie an, die dritte ständig** — und alle drei
bleiben:

- Die tiefste Kette hat **vier Glieder**, also drei Aufstiegsschritte; Stufe fünf ist über alle
  214.330 Startzeilen aus Fenster B **leer** (M30‑3). Die Zehn ist Schutz, kein Betriebsmittel.
- Es gibt **keinen Zyklus** und **null Selbstverweise** über 3,34 Millionen Zeilen (M30‑2, M30‑3).
  Der Schutz ist trotzdem gebaut: Die Kette entsteht durch **Datenbank-Events, die uns nicht
  gehören** (`MatchInterchange`, `SetTargetFlag`, `MoveDTNA997` —
  [`datenmodell.md`](datenmodell.md) §6), und was heute keinen Kreis bildet, muss morgen keinen
  bilden. Ohne den Schutz stünde an dieser Stelle eine Endlosschleife statt einer Antwort.
- **Die Breitengrenze greift dagegen ständig** — bei 1,07 Prozent der Wurzeln und bei 11,38 Prozent
  der Merge-Ergebnisse (M30‑2). Sie ist die einzige der drei, die im Normalbetrieb etwas tut, und
  deshalb die einzige mit einem Datenbanktest am echten Breitfall.

**Tiefengrenze und Zyklusschutz sind deshalb nur im Einheitstest prüfbar**, mit erfundenen Daten. Ein
Datenbanktest könnte sie gegen die Testkopie gar nicht auslösen.

**Ein Vorgänger, der nicht auflöst, beendet den Aufstieg still.** Zwei Fälle, beide richtig so
behandelt: ein Verweis ins Leere (M24‑1 hat null gefunden, die Quelle erzwingt es aber nicht) oder ein
Vorgänger eines **fremden** Mandanten — den darf es für diesen Aufrufer nicht geben, auch nicht als
Hinweis „hier geht es weiter".

### Warum die Flags nicht zum Abkürzen benutzt werden

E4 belegt, dass `Source` und `Target` sich exakt mit der Verkettung decken. Es wäre also möglich,
die beiden Abwärtsabfragen zu überspringen, wenn beide Flags `0` sind. **Das geschieht bewusst
nicht:**

- Es wäre das Vertrauen in eine **Beobachtung**, erkauft für einen Zugriff von einer halben
  Millisekunde.
- Es hätte eine Nebenwirkung: Die **Zählung** müsste dieselbe Abkürzung nehmen, sonst nennte die
  Antwort eine Zahl, zu der sie keine Zeilen liefert.

Die Flags sind der richtige Weg für die **Liste**, die je Zeile ohne Abfrage entscheiden muss, ob ein
Ketten-Hinweis erscheint. Innerhalb dieses Endpunkts sind sie es nicht.

---

## 5. Die Rolle ist eine Menge

`common/Kettenrolle` mit vier Werten, `common/Kettenrollen` mit der Rechenregel:

| Rolle | Bedingung |
|---|---|
| `SPLIT_WURZEL` | `Source` ist gesetzt |
| `SPLIT_KIND` | `SourceMessageID` ist belegt |
| `MERGE_EINGANG` | `TargetMessageID` ist belegt |
| `MERGE_ERGEBNIS` | `Target` ist gesetzt |

**Warum eine Menge und kein Aufzählungswert.** Über Fenster B tragen **514 von 214.330** Zeilen zwei
Rollen (0,240 %), bei `IBISGUS` sind es **1,278 %** (M28‑1c). Selten, aber nicht nie — ein einzelner
Wert gäbe diesen Zeilen still ein falsches Etikett. **Nie mehr als zwei** Rollen je Zeile; das ist
gemessen und keine Garantie, weshalb es der Bestandstest festhält, statt dass der Code es
voraussetzt.

**„Belegt" heißt: nicht `null` und nicht leer.** [`datenmodell.md`](datenmodell.md) §5.9 hat
gemessen, dass unbelegte Verkettung
ausnahmslos `NULL` ist — über 220.579 geprüfte Zeilen kein einziger leerer String. Die
Leerstring-Prüfung bleibt trotzdem: Die Produktion muss sich nicht daran halten, was die Testkopie
zufällig enthält.

**Ein `null`-Flag zählt als nicht gesetzt.** Beide Spalten sind `NULL`-fähig mit Vorgabe `b'0'`
(M23‑1); ein fehlender Wert ist keine Behauptung, es gebe Kinder.

**Die Reihenfolge in der Antwort ist die Deklarationsreihenfolge.** `Kettenrollen.aus` liefert ein
`EnumSet`, und das läuft in Deklarationsreihenfolge — die Reihenfolge hängt damit nicht daran, in
welcher Reihenfolge die Spalten geprüft werden.

**Wo die Regel liegt und warum.** In `common`, dieselbe Aufteilung wie bei `BamSpaltenRegel`: die
Regel gemeinsam, die Statements je Fachpaket. Ab Schritt 7 braucht `bam` die Rollen für die
Trefferliste, und Fachpakete kennen einander nicht — ein Import aus `message` wäre der Regelbruch,
ein zweiter Nachbau die Drift. Der **Datenzugriff** kann dort nicht liegen: `common` darf von keinem
anderen Anwendungspaket abhängen und damit auch nicht vom `MandantContext`, den Regel M2 als ersten
Pflichtparameter verlangt.

### Der Ersatz für das vollständige `switch`

Beim `MessageStatusKind` schützt ein `switch` ohne `default` davor, dass ein neuer Wert eine stille
Voreinstellung erbt. **Bei einer Menge gibt es diesen Schutz nicht** — jede der sechzehn
Spaltenbelegungen ist syntaktisch gültig, und keine löst einen Compilerfehler aus.

Ersatz ist **`KettenrollenBestandTest`** (`@Tag("db")`): Er erhebt die *vorkommenden*
Rollenkombinationen über Fenster B und vergleicht sie gegen
`Kettenrollen.FORMULIERTE_KOMBINATIONEN`. Taucht eine auf, für die es keine Formulierung gibt, wird
er rot. Zusätzlich hält er fest, dass keine Zeile drei oder vier Rollen trägt.

> **Er baut die Regel nicht in SQL nach.** Gruppiert wird in der Datenbank — höchstens sechzehn
> Zeilen kommen zurück —, die Rollen entstehen dann über `Kettenrollen.aus`. Damit prüft der Test die
> Regel, die die Anwendung tatsächlich benutzt, und nicht eine zweite Fassung derselben Regel.

**Die hinterlegte Liste enthält alle sechs Paare, nicht fünf.** Gemessen kommen **drei** vor
(M28‑1c, M30‑4) — und die drei, die nicht vorkommen, enthalten alle `MERGE_EINGANG`:

| Kombination | Fenster A | Fenster B |
|---|---:|---:|
| `SPLIT_WURZEL` + `SPLIT_KIND` | 4 | 456 |
| `SPLIT_KIND` + `MERGE_ERGEBNIS` | 0 | 33 |
| `SPLIT_WURZEL` + `MERGE_ERGEBNIS` | 0 | 25 |
| `SPLIT_WURZEL` + `MERGE_EINGANG` | **0** | **0** |
| `SPLIT_KIND` + `MERGE_EINGANG` | **0** | **0** |
| `MERGE_EINGANG` + `MERGE_ERGEBNIS` | **0** | **0** |

**Das sechste Paar war nicht ungemessen.** `SPLIT_WURZEL` + `MERGE_EINGANG` ist in M28‑1c über
Fenster B erhoben (null) und in M30‑4 über Fenster A nachgezogen (ebenfalls null). Alle drei leeren
stehen trotzdem in der Liste: „kommt nicht vor" und „ist nicht formulierbar" sind zweierlei — die
Liste sagt, worüber sich etwas sagen lässt, nicht, was vorgekommen ist.

**Nur eines der drei ist auch *ausgeschlossen*, die anderen beiden sind nur *leer*.**
`SPLIT_KIND` + `MERGE_EINGANG` kann es nach M28‑1 nicht geben — keine Zeile trägt beide ID-Spalten.
Für `SPLIT_WURZEL` + `MERGE_EINGANG` und `MERGE_EINGANG` + `MERGE_ERGEBNIS` gilt das **nicht**:
`Source` und `Target` sind Flags und keine ID-Spalten, aus M28‑1 folgt für sie nichts. Dort ist die
Null gemessen und nicht abgeleitet — und deshalb ist sie der Fall, den der Bestandstest im Auge
behält.

### Die `bit(1)`-Falle greift hier nicht

`Message.Source` und `Message.Target` sind `bit(1)`, und `SUM(Source)` liefert in SQL **keinen
brauchbaren Wert** — dort braucht es `SUM(Source + 0)`
([`datenmodell.md`](datenmodell.md) §5.8, M23‑2).

**Im Anwendungscode tritt das nicht auf.** Der jOOQ-Codegen bildet `bit(1)` per `forcedType` auf
`Boolean` ab ([`datenzugriff.md`](datenzugriff.md) §9); die Felder heißen in Java schlicht `Boolean`.
Wer hier ein `+ 0` schreibt, hat die Erhebung von Hand mit dem Anwendungscode verwechselt. Der Satz
steht hier, weil §5.8 die Falle beschreibt, ohne zu sagen, wo sie **nicht** gilt.

---

## 6. Mandantentrennung

**Der Filter steht in jedem der fünf Statements** als `EXISTS` über `Process → ProjectMandant` —
Wort für Wort derselbe wie in `NachrichtenRepository` und `NachrichtendetailRepository` (Regel M3).
Als `EXISTS` und nicht als Join: `ProjectMandant` ist im Schema n:m, ein Join könnte Zeilen
vervielfachen, sobald ein Projekt mehreren Mandanten gehört.

**Regel M5 hat hier eine zweite Seite: die Zählung.** `abwaertsGesamt` trägt denselben Filter wie
die Zeilen. Ohne ihn nennte die Antwort 27 Teile und lieferte 25 — und der Unterschied sähe wie ein
Fehler des Werkzeugs aus, nicht wie ein Leck.

**Der `abwaertsCursor` ist eine Position und keine Berechtigung.** Er beschreibt eine Zeile, die den
Mandantenfilter bereits passiert hat, und trägt damit keine Auskunft über fremde Daten; der
Blätter-Endpunkt filtert unabhängig von ihm weiter. Wer ihn manipuliert, verschiebt höchstens seine
eigene Seitenposition innerhalb seines eigenen Datenausschnitts — dieselbe Begründung, aus der der
Cursor der Liste nicht signiert ist ([`nachrichtenliste.md`](nachrichtenliste.md)).

**Der Preis ist gemessen und wird bezahlt.** Ohne Filter wäre die Zählung `Using index`; mit Filter
muss sie in die Tabelle, weil `Message.ProcessID` in keinem der beiden Verkettungsindizes steht. Das
kostet 18,7 ms für 3.048 Zeilen (M30‑1). Die Alternative wäre eine Zahl, die einen anderen Bestand
beschreibt als die Zeilen darunter.

**M27 ändert daran nichts.** Die Kette überschreitet die Mandantengrenze nicht — in allen 9.101
Zeilen mit Prozesswechsel führen Quell- und Zielprozess zum selben Mandanten, und M30‑5 hat für die
beiden Bezugszeilen nachgeprüft, dass gefilterte und ungefilterte Zählung übereinstimmen (3.048/3.048
und 749/749). **Der Filter ist die Zusicherung, nicht die Beobachtung.**

### 404 statt 403, und woran die Zusage hängt

Eine fremde `MessageID` und eine erfundene liefern **dieselbe** Antwort. Die Zusage hängt nicht
daran, dass die Rümpfe gleich aussehen, sondern daran, dass beide Fälle **dasselbe Statement mit null
Zeilen** sind ([`mandantentrennung.md`](mandantentrennung.md) §5). Wer eine Existenzprüfung davorbaut,
bricht sie — auch wenn kein Test rot wird, weil „gibt es nicht" einen Zugriff kostet und „gehört
einem anderen" zwei.

`instance` ist die eine Stelle, an der sich die beiden Antworten unterscheiden, und sie ist keine
Auskunft: Nach RFC 9457 ist es der angefragte Pfad, und weil die Kennung *im Pfad* steht, enthält
`instance` sie zwangsläufig. Es ist das **Zitat der Frage**.

---

## 7. Aufbau im Code

```
common/                              message/
├─ Kettenrolle.java                  ├─ KettenController.java     REST, nimmt nie eine Mandanten-ID
└─ Kettenrollen.java                 ├─ KettenService.java        aufsteigen, absteigen, begrenzen
   die Rechenregel, ohne DB und      ├─ KettenRepository.java     die fuenf Statements, je gefiltert
   ohne MandantContext               ├─ Kettengliedzeile.java     die rohe Zeile
                                     ├─ Kettenbeziehung.java      AUFTEILUNG / ZUSAMMENFUEHRUNG
                                     ├─ KettengliedResponse.java
                                     └─ KetteResponse.java
```

**Paket `message`, kein eigenes Wurzelpaket.** [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §6
führt die Verkettung dort ausdrücklich (`message/ Liste, Detail, Verkettung`).

**Jede Repository-Methode bekommt `MandantContext` als ersten Pflichtparameter** (Regel M2), auch
die, die nur einen Primärschlüssel auflöst. `PaketstrukturTest` prüft das maschinell.

**Ein dritter Controller unter `/api/nachrichten`.** Aus demselben Grund, aus dem es einen zweiten
gibt: Liste, Detail und Kette beantworten drei verschiedene Fragen und haben nichts gemeinsam außer
dem Pfadpräfix. Spring löst über alle Controller hinweg auf und bevorzugt das wörtliche Segment vor
der Pfadvariablen; `KettenDbIT.bestehende_endpunkte_bleiben_erreichbar` hält das fest.

**Der Zugriff läuft über den Lese-`DSLContext`** (`glassfishDsl`). Schemaübergreifend gelesen wird
nichts, aber das Quellschema ist die Quelle, und `jooq.glassfish` ist am Import erkennbar.

### Tests

| Datei | Was, und ob mit Datenbank |
|---|---|
| `KettenrollenTest` | **ohne DB** — alle vier Einzelrollen, **alle sechs Paare**, die leere Menge, `null`-Flags, leere und blanke Kennungen, die Deklarationsreihenfolge, die Vollständigkeit der hinterlegten Liste und der Nachweis, dass jede erreichbare Belegung darin steht |
| `KettenrollenBestandTest` | `@Tag("db")` — die vorkommenden Kombinationen gegen die hinterlegte Liste, „nie mehr als zwei Rollen", und die Gegenprobe, dass alle vier Einzelrollen und drei Doppelrollen überhaupt vorkommen |
| `KettenServiceTest` | **ohne DB** — Aufstieg bis zur Wurzel, Aufstieg über `TargetMessageID`, Tiefengrenze, **Zyklus** (zwei Formen, plus „Zyklus schlägt Tiefe"), ein unsichtbares Glied darüber, Breitengrenze, Mischung beider Abwärtsrichtungen, beide Richtungen werden immer gefragt, Cursor, unlesbarer Cursor, unzulässige Seitengröße, `404` vor der leeren Seite, UTC-Umrechnung, `statusKind` aus dem Classifier, **eine Zeile ohne `MessageLastUpdate`** (wird ausgeliefert und bricht das Blättern nicht) und der eine Fall, der übrig bleibt (eine ganze Seite ohne Zeitpunkt). Dazu die drei Prüfungen zum **`abwaertsCursor`**: `null` ohne weitere Seite, `null` bei einer letzten Zeile ohne Zeitpunkt, und — die wichtigste — **derselbe Wert, den der Blätter-Endpunkt nach seiner ersten Seite liefert** |
| `KettenStatementsTest` | **ohne DB** — Mandantenfilter in **jedem** Statement einschließlich beider Zählungen, Zugriff über die richtige Spalte je Richtung, Sortierschlüssel, `limit + 1`, kein `OFFSET`, kein Zeitfenster |
| `KettenDbIT` | `@Tag("db")` — echte Ketten: Aufstieg beim Split und beim Merge, der Breitenfall an der breitesten Wurzel, **`abwaertsGesamt` gegen die über den Cursor erreichbare Menge** — die Runde beginnt mit der Seite aus `/kette` und dem `abwaertsCursor`, nicht mit einem cursorlosen Erstaufruf —, `abwaertsCursor` genau dann, wenn es weitergeht, das breiteste Merge-Ergebnis, `rollen` leer statt fehlend, keine Grenze spricht an, die bestehenden Endpunkte bleiben erreichbar |
| `KettenIsolationDbIT` | `@Tag("db")` — **die zwei Pflicht-Isolationstests** (Regel M4), samt der Gegenprobe mit einer **echten fremden Kennung** über den Mandantenwechsel als ADMIN, dem Laufzeitvergleich und `403` ohne aktiven Mandanten |

**Keine fest eingetragene `MessageID` in den Datenbanktests.** Die Bezugszeilen werden nach ihrer
**Gestalt** gesucht — „die breiteste Wurzel im Fenster", „ein Merge-Eingang" —, nicht nach ihrer
Kennung. Und die Suche trägt **denselben Mandantenfilter** wie der Anwendungscode: Ohne ihn suchte
der Test Bezugszeilen im ganzen Bestand und fragte sie als `NEXANS`-Nutzer ab — die Antwort wäre
korrekterweise `404`, und der Test scheiterte an seiner eigenen Auswahl statt an einem Fehler im
Code.

> **Zur Benennung.** `KettenrollenBestandTest` endet auf `Test` und trägt trotzdem `@Tag("db")`,
> anders als das sonst verbindliche `<Thema>DbIT`. Er läuft damit über Surefire statt über Failsafe;
> die Ausschlussoption `-DexcludedGroups=db` wirkt in beiden gleich, die CI schließt ihn also
> korrekt aus. Der Name ist der aus dem Auftrag, und er beschreibt genau, was der Test ist: die
> Bestandsprüfung zu `KettenrollenTest` und kein Integrationstest eines Endpunkts.

---

## 8. Die Oberfläche (Teil 2b)

Entsteht am 11.08.2026. Die Kette bekommt **einen Block im Detailpanel**, zwischen Kopf und
Zeitleiste — keine eigene Route, keine eigene Ansicht.

```
features/nachrichten/
├─ api.ts                      + Kettentypen und die zwei Aufrufe
├─ kette.ts                    reine Funktionen: Einteilung, Zahl, „gibt es einen Block"
├─ hooks.ts                    + useKette, useKettenAbwaerts
└─ components/
   ├─ kette-block.tsx          der Block: Abschnitte, Zeilen, Hinweise, Nachladen
   ├─ nachricht-detail.tsx     der Block sitzt zwischen Kopf und Zeitleiste
   ├─ nachricht-seite.tsx      + der Weg, den ein Glied auf der eigenen Route nimmt
   └─ status-plakette.tsx      + die kompakte Fassung fuer eine 26-rem-Zeile
```

### 8.1 `rollen` im Detail-Kopf

**Das eine ergänzte Feld.** `GET /api/nachrichten/{messageId}` liefert seit heute `rollen` als
Liste — **immer vorhanden, leer statt fehlend**. Ein fehlendes Feld hieße „unbekannt", ein leeres
heißt „nicht in einer Kette", und das ist eine Aussage, die die Zeile tatsächlich macht.

**Es kostet keinen Join und kein zweites Statement.** Die vier Verkettungsspalten liegen auf der
`Message`-Zeile, die `findeKopf` ohnehin liest; der Zugriffspfad aus
[`nachrichtendetail.md`](nachrichtendetail.md) §8 ändert sich nicht, `Message` bleibt `const`. Genau
dafür war **E4** die Messung: Ob eine Nachricht eine Kette hat, steht auf der Zeile — ohne Abfrage.

**Abgeleitet wird in `common/Kettenrollen`** und nirgends sonst (§5). Der Detail-Service ruft
dieselbe Regel wie der Ketten-Service; ein zweiter Nachbau wäre die Drift, gegen die die Regel in
`common` liegt.

### 8.2 Der Block erscheint bedingt — und darf deshalb offen stehen

**Ist `rollen` leer, gibt es den Block nicht.** Keine Überschrift, kein leerer Kasten, kein
Platzhalter — und **keine Anfrage auf `/kette`**. Dieselbe Regel, die im Panel schon für die
technischen Eigenschaften bei `eigenschaftenAnzahl === 0` gilt und in der Liste für einen Mandanten
ohne BAM-Konfiguration: *Eine Fläche ohne Inhalt behauptet, es gäbe dort etwas zu sehen.*

> **Belegvermerk** (Regel L10).
> *Gemessen (M30‑4, Fenster B, n = 214.330):* **39.090 Zeilen** tragen keine einzige Rolle — 18,2
> Prozent.
> *Behauptet wird:* „Rund 60 Prozent aller Zeilen tragen keine Kette", die Zahl aus der
> Aufgabenstellung.
> **Die Lücke:** Die 18 Prozent sind über ein Fenster gemessen, in dem `NEXANS` praktisch das ganze
> Aufkommen trägt und ein Drittel aller Zeilen Split-Kinder sind. Für die Entscheidung ist die
> Abweichung folgenlos — sie geht in dieselbe Richtung: Der Block entfällt bei einem Fünftel bis
> gut der Hälfte der Nachrichten, und in **keinem** Fall entsteht dort eine zweite Anfrage.

**Das ist der Grund, warum der Block dauerhaft sichtbar sein darf statt eingeklappt.** Sein
Hauptnachteil wäre gewesen, dass er bei der Mehrheit der Nachrichten Platz ohne Inhalt kostet — und
den trägt er mit der Bedingung nicht mehr. Die Kette ist nach dem Leitsatz die Antwort auf „wo ist
mein Lieferschein" und damit **kein Beiwerk**, das hinter einen Klick gehört.

**Kommt trotz gesetzter Rollen nichts zurück, gibt es ebenfalls keinen Block.** Die Flags sagen, dass
es eine Kette gibt; die Zeilen dazu holt der Endpunkt trotzdem selbst (§4). Bleiben beide Listen leer
und meldet auch keine Grenze etwas, ist nichts zu zeigen — und dann steht dort nichts.

### 8.3 Zwei Abschnitte, eingeteilt nach der Flussrichtung

| Abschnitt | Was darin steht |
|---|---|
| **Kommt von** | bei Aufteilung die Wurzel (und weitere Vorfahren), bei Zusammenführung die **Eingänge** |
| **Wurde zu** | bei Aufteilung die **Teile**, bei Zusammenführung das **Ergebnis** |

> ⚠️ **Das ist nicht dieselbe Einteilung wie `aufwaerts` gegen `abwaerts`** — und der Unterschied
> ist der Kern dieses Abschnitts.

Für die **Aufteilung** decken sich beide: Der Aufstieg führt zur Wurzel, der Abstieg zu den Teilen.
Für die **Zusammenführung** ist es vertauscht, weil der Aufstieg dort `TargetMessageID` folgt — und
die zeigt vom Eingang auf das Ergebnis, also **mit** dem Datenfluss und nicht gegen ihn:

| Ich bin | API-Richtung | Was dort steht | Im Fluss ist das |
|---|---|---|---|
| Split-Kind | `aufwaerts` | meine Wurzel | woher ich komme |
| Split-Wurzel | `abwaerts` | meine Teile | was aus mir wurde |
| **Merge-Eingang** | `aufwaerts` | **das Ergebnis** | **was aus mir wurde** |
| **Merge-Ergebnis** | `abwaerts` | **meine Eingänge** | **woher ich komme** |

**Nur die Spaltenüberschrift „API-Richtung" hat am 11.08.2026 neue Namen bekommen** (§2) — die
Bedeutung in den beiden rechten Spalten ist Wort für Wort dieselbe. Die Namen behaupten seither
nichts mehr, und die Tabelle sagt, was sie schon vorher sagte: Der Mechanismus ist nicht die
Bedeutung.

**Wer die beiden Listen unverändert als „Kommt von" und „Wurde zu" beschriftete, schriebe bei jedem
Merge-Eingang das Gegenteil dessen hin, was passiert ist.** Über Fenster B sind das **38.628**
Zeilen (M30‑4) — kein Randfall, sondern gut ein Sechstel aller Zeilen.

Entschieden wird deshalb über **`beziehung` zusammen mit der Ebene**, in `features/nachrichten/kette.ts`
und nicht in einer Komponente. Genau dafür liefert Teil 1 das Feld ausdrücklich mit, statt es
ableiten zu lassen (§2). Die Bedingung ist eine Zeile: *Stimmen Richtung und Beziehung überein,
kommt das Glied her; sonst ist es geworden.*

**Ein zickzackender Aufstieg verteilt sich auf beide Abschnitte.** Ein Merge-Ergebnis, das selbst ein
Split-Kind ist, hat eine Stufe „wohin" und darüber eine „woher". Die Reihenfolge innerhalb eines
Abschnitts bleibt dabei die des Aufstiegs.

> ### Korrigiert am 11.08.2026 — von „richtig so" zu „bekannte Verzerrung"
>
> Der Absatz oben nannte den Fall bis heute *„33 Zeilen über Fenster B (M30‑4)"* und schloss:
> *„Das ist der Preis der ehrlichen Einteilung und richtig so: Beide Stufen sagen die Wahrheit."*
> **Beides ist zu korrigieren**, und zwar auf zwei verschiedene Arten.
>
> **Erstens: Die 33 beantworten eine andere Frage.** Sie zählen **Knoten** — Zeilen, die zugleich
> Merge-Ergebnis und Split-Kind sind —, nicht **Ansichten**. An einem solchen Knoten hängen mehrere
> Merge-Eingänge, und **jeder einzelne von ihnen** sieht die verzerrte Stufe. M31 hat das
> nachgezählt:
>
> | | Fenster B | ganzer Bestand |
> |---|---:|---:|
> | Knoten (Merge-Ergebnis **und** Split-Kind) | 33 (M30‑4, in M31‑0 zeilengenau reproduziert) | **795** |
> | **Ansichten daran** (M31‑1) | **390** | **7.935** |
> | Ansichten je Knoten | 12,19 | 9,98 |
> | breitester solcher Knoten | 40 Eingänge | 50 Eingänge |
>
> Der **Spiegelfall** — ich bin Split-Kind, meine Wurzel ist selbst Merge-Eingang — ist über Fenster B
> **null**, und zwar über 104.538 Split-Kinder und fünf Mandanten (M31‑2). Über den Bestand gibt es
> ihn **vier** Mal, bei einem Mandanten, der in keiner Fenstermessung dieses Projekts vorkommt.
> Zusammen also **7.939** verzerrte Ansichten im Bestand. **Die Verzerrung ist einseitig.**
>
> **Und die Zahl, gegen die sie zu halten ist**, steht wenige Zeilen weiter oben: Die Einteilung nach
> der Flussrichtung ist mit **38.628** Zeilen begründet worden — den Merge-Eingängen aus Fenster B,
> die unter der API-Richtung falsch beschriftet wären (M30‑4).
>
> | | Zeilen (Fenster B) | Anteil an 214.330 |
> |---|---:|---:|
> | falsch beschriftet unter der **API-Richtung** | **38.628** | 18,02 % |
> | eine Stufe falsch beschriftet unter der **Flussrichtung** | **390** | 0,18 % |
>
> **Faktor 99.** Die Einteilung nach der Flussrichtung bleibt damit die richtige — korrigiert wird
> der Satz darüber, nicht die Entscheidung darunter.
>
> **Zweitens: „Beide Stufen sagen die Wahrheit" stimmt nicht.** Die **Zeile** sagt sie — sie trägt
> Ebene und Beziehung, und beide sind richtig. Die **Überschrift** sagt sie nicht: Wer einen
> Merge-Eingang geöffnet hat, findet auf Stufe −2 unter „Kommt von" eine Nachricht, von der er nicht
> kommt. Sie ist die Wurzel seines *Ergebnisses*, nicht seine eigene. **Das ist dieselbe
> Fehlerklasse, gegen die dieser Abschnitt insgesamt geschrieben ist** — nur eine Stufe höher und um
> den Faktor 99 seltener.
>
> **Die Verzerrung ist immer zweistufig.** Einen Wechsel über zwei Stufen hinaus gibt es nicht: Die
> einzige Kombination mit Wechsel (`Z` → `A`) endet auf Stufe −2, und die einzige dreistufige Kette
> wechselt nirgends — gemessen über die 143.166 Zeilen mit Aufstieg in Fenster B (M31‑1/2).
>
> **Zwei Handlungsmöglichkeiten, und hier wird keine gewählt:**
>
> | Möglichkeit | Was daran hängt |
> |---|---|
> | **Den Aufstieg beim Beziehungswechsel abbrechen** | Er endete bei 390 von 143.166 Zeilen (0,27 %) eine Stufe früher, alle übrigen Ketten blieben unverändert. Der Block behauptete dann nur noch Wahres über die geöffnete Nachricht — und weiter kommt man ohnehin per Klick (§8.6) |
> | **Die Verzerrung dokumentiert stehen lassen** | Sie beträfe 0,18 % der Zeilen des Fensters und 0,34 % der Zeilen mit Aufstieg im Bestand, immer auf Stufe −2 und nie tiefer |
>
> **Die Entscheidung fällt im Sparring zu Schritt 7. Am Verhalten ist heute nichts geändert.**
>
> > **Belegvermerk** (Regel L10).
> > *Gemessen (M31‑1, Fenster B, n = 38.628 Merge-Eingänge):* **390** haben ein Ergebnis, das selbst
> > Split-Kind ist. *(M31‑2, n = 104.538 Split-Kinder über fünf Mandanten):* **0** im Spiegelfall.
> > *Behauptet wird:* dass ebenso viele **Ansichten** eine falsche Überschrift tragen.
> > **Die Lücke:** Gemessen ist die Gestalt der Daten, nicht die Zahl der Aufrufe. Wie oft eine
> > dieser Zeilen tatsächlich geöffnet wird, ist nicht erhoben und von hier aus nicht erhebbar — der
> > Anteil ist eine Obergrenze über die Zeilen und keine Aussage über die Nutzung. Dazu ist die
> > Verzerrung **mandantengebunden**: Alle 390 gehören `NEXANS`, und kein anderer Mandant hat auch
> > nur einen Merge-Eingang.

**Innerhalb eines Abschnitts wird nicht neu sortiert:** erst die Aufwärtsglieder in der Ordnung des
Aufstiegs (Ebene `-1` zuerst), dann die Abwärtsglieder in ihrer Ordnung `(MessageLastUpdate,
MessageID)`. Die zweite ist der Sortierschlüssel des Cursors; eine zweite Ordnung an einer zweiten
Stelle wäre genau die Drift, die das Blättern zerlegt.

**Die aktuelle Nachricht wird im Block nicht wiederholt.** Sie steht darüber im Kopf; eine zweite
Zeile für dieselbe Nachricht wäre eine Aussage, die keine ist.

### 8.4 Die Zahl in der Überschrift — und wo sie fehlt

**Die Überschrift nennt die Zahl, wo es eine gibt:** „Wurde zu — 3.350 Teile", „Kommt von — 897
Eingänge". **M30 hat das entschieden** und nicht eine Abwägung: Die Zählung kostet an der breitesten
Wurzel des gesamten Bestands 19,8 ms (M30‑1), also nicht die Hälfte der Grenze von 50 ms, ab der auf
die Ersatzform „mehr als 50" umgestellt worden wäre. `abwaertsGesamt` ist damit eine gemessene,
mandantengefilterte Zahl, und die Beschriftung darf sie sagen.

**Sie steht nur am Abschnitt, der die Abwärtsglieder trägt** — und nur, wenn er sie **alle** trägt.
`abwaertsGesamt` zählt beide Abwärtsrichtungen zusammen; verteilen sie sich auf beide Abschnitte
(25 Zeilen sind zugleich Split-Wurzel und Merge-Ergebnis, M30‑4), ließe sich die Summe keiner der
beiden Überschriften zuordnen, ohne sie zu erfinden. Dann nennt keine eine Zahl.

**Der Aufstieg bekommt keine.** Der Endpunkt liefert für ihn keine, und die Länge der Liste ist bei
`tiefeErreicht` gerade **nicht** die Gesamtzahl. Eine Zahl, die der Endpunkt nicht liefert, behauptet
die Überschrift nicht.

### 8.5 Mehrstufigkeit, Ebene und die beiden Abbrüche

**Eingerückt wird nicht.** Das Panel ist 26 rem breit, und eine Einrückung je Ebene frisst genau die
Breite, die die Ablaufnamen brauchen — gemessen bis 61 Zeichen. Die Reihenfolge trägt die Ebene; ab
der **zweiten** Stufe steht sie zusätzlich als Beiwerk in der Zeile („Stufe 2"), und im `title` steht
sie immer, zusammen mit der Beziehung.

**`tiefeErreicht` und `zyklusErkannt` werden sichtbar gemacht**, nicht verschwiegen: ein Satz in der
ruhigen Farbrolle, ohne Warnzeichen und ohne Farbe. *Die Kette ist länger als hier gezeigt.* / *Die
Kette führt im Kreis — hier ist sie abgebrochen.* **Zwei Sätze und nicht einer**, aus demselben Grund,
aus dem es zwei Felder gibt (§4): „tief" beschreibt, wo abgebrochen wurde, „im Kreis" warum.

**Sie stehen unter beiden Abschnitten und nicht an einem.** Der Aufstieg kann über beide hinweg
verlaufen (§8.3); ein Satz an einem Abschnitt hinge dann am falschen. Ein Block, der **nur** einen
Abbruch zu melden hat und kein einziges Glied, erscheint trotzdem — ein Selbstverweis bräche den
Aufstieg schon auf der ersten Stufe. Gemessen kommt das null Mal vor (M30‑2, n = 3,34 Mio.).

### 8.6 Navigation: ein Klick öffnet das Glied

**Über den bestehenden Weg, ohne neuen Mechanismus und ohne eigene Route.** Welcher Weg das ist,
entscheidet der Einhängepunkt — dieselbe Aufteilung, die das Schließen schon hat:

| Einhängepunkt | Ein Klick auf ein Glied |
|---|---|
| Panel (`/nachrichten?nachricht=…`) | setzt den Parameter `nachricht`, das Panel lädt neu |
| eigene Route (`/nachrichten/<id>`) | führt auf **dieselbe** Route mit der neuen Kennung, samt der Abfragezeichenkette |

Auf der eigenen Route wäre `nachricht` falsch: Der Parameter gehört zur Liste, und diese Route hat
keine. Beides ist trotzdem derselbe Weg — **die Ansicht steht in der URL und bleibt teilbar** —, und
der Zurück-Knopf des Browsers führt Glied für Glied zurück, weil `nachricht` am Parser
`history: "push"` trägt.

Die Zeile trägt dieselbe Anfassbarkeit wie eine Listenzeile: Zeigehand, Hover-Fläche, Fokusring,
`Tab`/`Enter`/`Leertaste`. Sie ist ein `button` und bringt das alles von sich aus mit.

### 8.7 Nachladen statt Sprung

**Über die Breitengrenze hinaus wird im Block nachgeladen.** Eine Schaltfläche unter dem Abschnitt
holt die nächste Seite über `…/kette/abwaerts?cursor=…` und hängt sie an, statt sie zu ersetzen.
Verteilen sich die Abwärtsglieder auf beide Abschnitte (§8.4), steht die Schaltfläche darunter für
sich — sie ließe sich sonst keinem zuordnen.

> **Warum nicht in die Liste springen.** Naheliegend wäre ein Filter `?wurzel=…` an der
> Nachrichtenliste. Das bricht **Regel L1**: Die Liste verlangt ein Pflicht-Zeitfenster, und die
> Kinder einer drei Monate alten Wurzel lägen außerhalb jedes vernünftigen Fensters. Entweder man
> höhlte L1 für einen Sonderfall aus, oder die Liste lieferte für eine gültige Anfrage nichts. Der
> Cursor-Endpunkt aus Teil 1 ist ohnehin der billigere Zugriff — er steigt über `SourceMessageIDIDX`
> ein und nicht über `MessageLastUpdateIDX` (E5, M30‑1).

**Ein Klick ist eine Anfrage.** `/kette` liefert die ersten fünfzig Abwärtsglieder **und mit
`abwaertsCursor` die Position dahinter** (§2). Die erste nachgeladene Seite setzt deshalb *hinter*
dem an, was schon dasteht: Die gezeigten fünfzig bleiben stehen, fünfzig neue kommen dazu.

> **Bis zum 11.08.2026 war das ein Umweg.** `/kette` lieferte keinen Cursor — die `Seite`-Hülle
> hatte nur der Blätter-Endpunkt. Eine Position entstand erst mit dessen erster Antwort, und die war
> inhaltsgleich mit dem, was der Block schon zeigte: Er ersetzte die gezeigten fünfzig durch
> dieselben fünfzig und zog die zweite Seite sofort nach, damit ein Klick überhaupt etwas brachte —
> **zwei Anfragen für einen Klick.** Das war eine Folge der Abgrenzung von Teil 2b (Teil 1 sollte
> unverändert bleiben) und stand als offener Punkt in §11. Mit `abwaertsCursor` ist der Nachzug in
> `kette-block.tsx` **ersatzlos entfallen**.

**Trägt die Antwort keinen Cursor, gibt es keine Schaltfläche** — auch dann nicht, wenn
`weitereVorhanden` wahr ist. Das ist der Sonderfall aus M30‑6 (§2, Regel 4): Ohne Position gäbe die
Schaltfläche eine Zusage, die niemand einlösen kann. Gemessen tritt er nie ein.

**Bei 3.048 Kindern wird das Panel lang. Das ist in Ordnung** — es scrollt mit `main`, dem einzigen
senkrechten Scroller ([`frontend-grundlagen.md`](frontend-grundlagen.md) §7). **Kein eigener
Scrollbereich im Block.**

### 8.8 Die Zustände des Blocks

| Zustand | Was zu sehen ist |
|---|---|
| **Laden** | ein Platzhalter in der Gestalt des späteren Blocks — Überschrift und zwei Zeilen |
| **Fehler** | inline im Block, der bestehende Baustein aus `components/zustand.tsx` |
| **Leer** | gibt es nicht — dann gibt es keinen Block (§8.2) |

**Der Ladezustand liegt im Block, nicht im Panel.** Wer die Kette lädt, will die Zeitleiste nicht
verlieren — dieselbe Regel wie bei den technischen Eigenschaften. Und ein Fehler beim Auflösen der
Kette darf das Detail nicht mitreißen: Panel und Zeitleiste bleiben stehen.

**Fremde oder unbekannte Kennung eines Glieds: kein eigener Fall.** Die Kette überschreitet die
Mandantengrenze nicht (M27), und der Endpunkt filtert ohnehin. Tritt es doch auf, gilt der bestehende
Fehlertext des Panels — keine eigene Formulierung, und vor allem kein Wort über Berechtigungen.

**Keine Animation**, auch nicht beim Nachladen ([`visuelles-konzept.md`](visuelles-konzept.md) §7).

### 8.9 Farbe

**Der Block trägt keine eigene Farbe.** Die Statusplakette je Glied nutzt die bestehenden
Statusfarben und sonst nichts; die Kettenrolle bekommt **keine** Farbrolle. Sie ist eine Aussage über
die Anwendung beziehungsweise die Struktur, nicht über den Zustand der Daten — so wie die geöffnete
Listenzeile die blasse Akzenttönung trägt und keine Statusfarbe.

**Die Plakette erscheint kompakt: nur das Zeichen.** In einer Zeile, die neben dem Status noch
Ablaufnamen und Zeitpunkt trägt, ist für die Beschriftung kein Platz. Das ist **keine Ausnahme von
„nie allein über Farbe"**: Die Regel verlangt „zusätzlich eine Beschriftung **oder** ein Zeichen"
([`visuelles-konzept.md`](visuelles-konzept.md) §3), und die acht Zeichen unterscheiden sich in ihrer
Form. Die Beschriftung geht nicht verloren — sie steht im `title` und für Vorleseprogramme im Markup.
**Ein zweites Zeichen- oder Farbverzeichnis entsteht dafür nicht:** Die kompakte Fassung ist eine
Eigenschaft der bestehenden `StatusPlakette` und keine zweite Komponente.

**Feste Zeilenhöhe** (`--dichte-zeile`), gekürzt, Vollwert im `title`, `tabular-nums` am Zeitpunkt.

### 8.10 Tests

| Datei | Was |
|---|---|
| `tests/kette.test.ts` | die Einteilung nach der Flussrichtung samt der **vertauschten** Zusammenführung; die Reihenfolge innerhalb eines Abschnitts; der zickzackende Aufstieg; die Zahl nur am Abschnitt der Abwärtsglieder und **gar nicht**, wenn sie sich verteilen; dass die Zahl beim Nachladen die gezählte bleibt; und wann es überhaupt einen Block gibt — auch der Fall, in dem nur ein Abbruch zu melden ist |
| `NachrichtendetailServiceTest` | `rollen` im Kopf: leer statt fehlend, alle vier Einzelrollen, eine Doppelrolle in Deklarationsreihenfolge, `null`-Flag und leere Kennung zählen nicht |
| `NachrichtendetailDbIT` | `rollen` gegen die Testkopie über **beide** Sortierrichtungen: immer vorhanden, nie mehr als zwei, und in der Stichprobe kommen verkettete **und** unverkettete Nachrichten vor |
| `tests/detail-baum.test.tsx` *(neu, 11.08.2026)* | **die drei gerenderten Bäume** — siehe §8.14 |

Kein gerenderter Baum: Geprüft werden die **Entscheidungen**, nicht das Markup
([`frontend-grundlagen.md`](frontend-grundlagen.md) §9). **Seit dem 11.08.2026 gilt das mit drei
benannten Ausnahmen** (§8.14); die Regel selbst ist unverändert.

### 8.11 Sichtprüfung im Browser (11.08.2026)

> **Dieser Abschnitt ist ein Protokoll und wird nicht nachträglich umgeschrieben.** Er entstand
> **vor** der Umbenennung desselben Tages (§2) und nennt deshalb an zwei Stellen die damaligen
> Feldnamen: `nachfolgerGesamt` heißt heute `abwaertsGesamt`, `nachfolger` heißt `aufwaerts`
> beziehungsweise `abwaerts`. **Beobachtet wurde nichts anderes** — die Umbenennung hat kein
> Verhalten der Oberfläche geändert, und Punkt 5 ist der einzige, den der später ergänzte
> `abwaertsCursor` betrifft: Dort waren es zwei Anfragen für den ersten Klick, heute ist es eine
> (§8.7). Nachgeprüft ist das in **§8.13**.

Gegen die laufende Anwendung im Profil `dev`, Mandant `NEXANS`, Fenster 1568 × 726. **Geklickt und
getippt, nicht zugewiesen.** Die Anwendung ist vor der Prüfung **neu gestartet** worden — ein
laufendes Backend kennt das neue Antwortfeld `rollen` nicht, und eine Prüfung gegen die alte Instanz
bewiese nichts.

**Die vier Nachrichten sind nach ihrer Gestalt gewählt, nicht nach Aktualität.** Gesucht wurden sie
über die Endpunkte — Listen nach Status, dann `…/kette` je Kandidat —, geöffnet ausschließlich über
die Adresszeile und über Klicks. Die Kennungen stehen hier nicht; beschrieben wird die Gestalt.

| Gestalt | wie gefunden |
|---|---|
| **(a)** ohne Kette | `rollen` leer, aus einer Stichprobe von 60 Nachrichten des dichten Fensters |
| **(b)** Split-Wurzel, schmal | `status=AUFGETEILT`, `nachfolgerGesamt = 2` |
| **(c)** Split-Wurzel, breit | `status=AUFGETEILT`, breiteste der Stichprobe: **169** Teile |
| **(d)** Merge-Ergebnis | Vorgänger eines `status=ZUSAMMENGEFUEHRT`, **77** Eingänge |

| # | Geprüft | Ergebnis |
|---|---|---|
| 1 | **(a) Kein Block ohne Kette** | Der Kopf geht unmittelbar in die Zeitleiste über — keine Überschrift, kein leerer Kasten |
| 2 | **(a) Keine Anfrage auf `/kette`** | In den Netzanfragen stehen `auth/me`, `mandanten`, `prozesse`, das Detail und die Liste — **kein** `/kette` |
| 3 | **(b) „Wurde zu — 2 Teile"** | zwei Zeilen, je Statusplakette, Ablaufname und Zeitpunkt |
| 4 | **(c) „Wurde zu — 169 Teile"** | Überschrift nennt die Zahl des Endpunkts; **50** Zeilen, darunter die Schaltfläche |
| 5 | **(c) Nachladen hängt an** | ein Klick: 50 → **100**, ein zweiter: → **150**; die erste Zeile bleibt dieselbe (`03:42:47`), die letzte wandert (`03:42:53`) |
| 6 | **(c) Feste Zeilenhöhe** | jede Gliedzeile **36 px**, also `--dichte-zeile` |
| 7 | **(c) Der Tooltip trägt den Vollwert** | `Versand Einzel IDOC aus Split` · `30.12.2025, 03:42:47` · `Aufteilung · Stufe 1` |
| 8 | **(d) „Kommt von — 77 Eingänge"** | am **Merge-Ergebnis**, obwohl die Glieder aus `nachfolger` kommen — die Einteilung nach der Flussrichtung (§8.3) |
| 9 | **Ein Klick auf ein Glied öffnet es** | `?nachricht=…` in der URL, das Panel lädt neu |
| 10 | **Die Gegenprobe zur Einteilung** | Das geöffnete Glied ist ein **Merge-Eingang**: Sein Block sagt **„Wurde zu"** mit dem Ergebnis darin. Unter der API-Richtung stünde dort „Kommt von" — und das wäre falsch |
| 11 | **Neuladen stellt die Ansicht her** | `F5`: derselbe Block, `Wurde zu` mit einer Zeile, Tooltip `Zusammenführung · Stufe 1` |
| 12 | **Der Ladezustand liegt im Block** | im Neuladen gesehen: Platzhalter (Überschrift und zwei Zeilen) zwischen Kopf und Zeitleiste, **während die Zeitleiste stehen bleibt** |
| 13 | **Tastatur: `Tab` erreicht ein Glied** | nach dem Kopierknopf der Kennung genau ein `Tab`; Fokusring 2 px in `lab(45.08 -10.22 46.64)` = `--ring` |
| 14 | **Tastatur: `Enter` öffnet** | die URL wechselt auf das Kind, der Kopf zeigt dessen Ablaufnamen |
| 15 | **Tastatur: `Escape` schließt** | `nachricht` fällt aus der URL, das Panel ist weg |
| 16 | **Eine Bildlaufleiste, auch bei 150 Gliedern** | Dokument scrollt nicht, `main` ist der **einzige** senkrechte Scroller, waagerechter Überlauf **0** |
| 17 | **Keine Konsolenmeldung** | nach der Nachbesserung unten: keine einzige — siehe §8.12 |
| 18 | **Schmales Fenster** | **nicht gesehen** — dieselbe Grenze wie immer ([`frontend-grundlagen.md`](frontend-grundlagen.md) §8) |

**Nicht gesehen, weil es sie nicht gibt:** `tiefeErreicht` und `zyklusErkannt`. Die tiefste Kette der
Testkopie hat vier Glieder gegen zehn erlaubte, und es gibt keinen Zyklus (M30‑3). Beide Sätze sind
ausschließlich durch Vitest belegt — **eine Unmöglichkeit, kein Versäumnis**, dieselbe Art von Lücke
wie `LAEUFT_AUF` in [`nachrichtendetail.md`](nachrichtendetail.md) §10.12.

> ⚠️ **Die breiteste Wurzel des Bestands ist nicht darunter.** M24‑2 nennt **3.048** Kinder, M30‑2
> für den ganzen Bestand **3.350**; gefunden wurden über zwei Stichproben — 40 Wurzeln des dichten
> Fensters und 158 Ketten quer über den dichten Monat — höchstens **169**. Gesucht wurde dabei
> zweimal: einmal über Wurzeln, einmal über *Kinder* (dort ist die Wahrscheinlichkeit, eine breite
> Wurzel zu ziehen, proportional zu ihrer Breite). **Für die Prüfung ist das folgenlos** — die
> Breitengrenze greift ab 51, und 169 löst genau dieselben drei Dinge aus, die zu prüfen waren:
> genaue Zahl, gedeckelte Liste, Nachladen. **Der Unterschied zwischen 169 und 3.350 ist die Länge
> des Panels, und die ist gemessen, nicht gesehen.**

### 8.12 Ein Befund aus der Sichtprüfung — und was er geändert hat

**Zwei Geschwister trugen denselben React-`key`.** Der Kettenblock und der Eigenschaftenblock
bekommen beide `key={messageId}`, damit sie beim Blättern zwischen Nachrichten neu aufgebaut werden
— und standen damit im selben Elternteil mit demselben Schlüssel. Für React ist das derselbe Platz
im Baum; die Konsole meldete *„Encountered two children with the same key"*.

**Gesehen, nicht gedacht.** Der Fehler ist in keinem Test aufgefallen und hätte es auch nicht können:
`tests/kette.test.ts` prüft die Entscheidungen und rendert keinen Baum, und sichtbar falsch war
nichts. Er stand in der Konsole, und die Konsole zu lesen gehört zur Sichtprüfung.

**Behoben** durch je einen Namen vor dem Schlüssel (`kette-…`, `eigenschaften-…`). Danach: keine
Konsolenmeldung mehr, Verhalten unverändert.

### 8.13 Sichtprüfung der Nacharbeit Teil B (11.08.2026)

Gegen die **neu gestartete** Anwendung im Profil `dev`, Mandant `NEXANS` — ein laufendes Backend
kennt den umbenannten Pfad nicht, und eine Prüfung gegen die alte Instanz bewiese nichts.

Die Bezugsnachricht ist nach ihrer **Gestalt** gesucht, nicht nach ihrer Kennung: ein
Merge-Ergebnis mit **77 Eingängen**, gefunden über `status=ZUSAMMENGEFUEHRT` und den Aufstieg der
Treffer. Dieselbe Gestalt wie Fall (d) in §8.11.

| # | Geprüft | Ergebnis |
|---|---|---|
| 1 | **Der alte Pfad antwortet nicht mehr** | `…/kette/nachfolger` → **404** `application/problem+json`, mit gültiger Sitzung. `…/kette/abwaerts` und `…/kette` → 200 |
| 2 | **Der Block sagt weiterhin „Kommt von"** | „Kommt von — 77 Eingänge" am Merge-Ergebnis, obwohl die Glieder aus `abwaerts` kommen — die Einteilung nach der Flussrichtung (§8.3) ist unberührt |
| 3 | **Ein Klick, eine Anfrage** | genau **eine** Netzanfrage: `…/kette/abwaerts?cursor=MjAyNS0xMi0yOVQyMzoyMjo0MXw…`. **Kein** cursorloser Erstaufruf, **kein** Nachzug einer zweiten Seite |
| 4 | **Die Seite wird angehängt, nicht ersetzt** | 50 Zeilen → **77**. Der Endpunkt lieferte 27, `hasMore: false`, `nextCursor: null` |
| 5 | **Keine Zeile doppelt** | Vereinigung aus `/kette` und der Cursor-Seite: 77 Einträge, **77 verschiedene** Kennungen |
| 6 | **Die Schaltfläche verschwindet** | nach dem Klick trägt der Block keinen Nachladeknopf mehr; im Panel bleiben nur „Ansicht schließen", „Kennung kopieren" und „Technische Eigenschaften (31)" |
| 7 | **Die Ansage stimmt** | `aria-live`: „77 Glieder geladen" |
| 8 | **Die Gegenprobe zur Einteilung** | Ein Klick auf einen Eingang öffnet ihn; sein Block sagt **„Wurde zu"**. Unter der API-Richtung stünde dort „Kommt von" — und das wäre falsch |
| 9 | **Die Konsole bleibt still** | zwei Meldungen, beide vom Entwicklungsserver (React-DevTools-Hinweis, `[HMR] connected`). Keine aus der Anwendung |

> ⚠️ **Der Sonderfall aus M30‑6 ist nicht gesehen** — eine ganze Seite ohne `MessageLastUpdate`
> kommt in der Testkopie 0 von 3.341.519 Mal vor. Dass `abwaertsCursor` dann fehlt und die
> Schaltfläche entfällt, ist ausschließlich durch `KettenServiceTest` und `tests/kette.test.ts`
> belegt. **Eine Unmöglichkeit, kein Versäumnis** — dieselbe Art von Lücke wie bei Tiefengrenze und
> Zyklusschutz (§8.11).
>
> ⚠️ **Die breiteste Wurzel des Bestands ist auch hier nicht gesehen** (§11). 77 Eingänge lösen
> dieselben drei Dinge aus, die zu prüfen waren — genaue Zahl, gedeckelte Liste, Nachladen —, aber
> nur eine Nachladeseite. Ein Fall mit **mehreren** Cursor-Seiten hintereinander ist im Browser
> nicht durchgeklickt; er ist in `KettenDbIT` über die volle Runde belegt.

### 8.14 Das Netz unter der Konsole (11.08.2026)

**Der Doppelschlüssel aus §8.12 ist von keinem Test gefunden worden, und er hätte es nicht können.**
Das war keine Nachlässigkeit, sondern eine Lücke im Werkzeug: Die ganze Fehlerklasse „steht nur in
der Konsole" hatte kein Netz, und die schließt sich nicht dadurch, dass man beim nächsten Mal
genauer hinsieht.

**Seither lässt ein `console.error` den Frontend-Testlauf fehlschlagen.** Die Regel, ihre drei
Nebenbedingungen und der Nachweis, dass sie tatsächlich greift, stehen in
[`frontend-grundlagen.md`](frontend-grundlagen.md) §9 — dort, wo die Teststrategie des Frontends
insgesamt steht, und nicht in dieser Feature-Datei.

**Dazu drei gerenderte Bäume und genau drei** (`tests/detail-baum.test.tsx`). Zwei davon sind der
Ersatz für eine Sichtprüfung, die es nicht geben kann:

| Test | Warum genau dieser |
|---|---|
| `tiefeErreicht` | Die tiefste Kette der Testkopie hat vier Glieder (M30‑3); die Grenze von zehn spricht dort nie an. Der Satz *„Die Kette ist länger als hier gezeigt"* ist ausschließlich durch Vitest belegbar |
| `zyklusErkannt` | Null Zyklen und null Selbstverweise über 3,34 Mio. Zeilen (M30‑2, M30‑3). Der Satz *„Die Kette führt im Kreis"* ist von Hand nie zu sehen. Dazu der Selbstverweis, der den Aufstieg auf der ersten Stufe bricht: Der Block erscheint auch dann, wenn er **nur** den Abbruch zu melden hat |
| Doppelschlüssel | Ketten- und Eigenschaftenblock am selben Detail. Der Test besteht, wenn **kein `console.error`** fällt — er hängt also ganz an der Regel oben und wäre ohne sie wertlos |

**Beide Abbruchsätze werden auf ihre Lage geprüft und nicht nur auf ihr Vorhandensein:** Sie stehen
**unter beiden** Abschnitten und in keinem drin (§8.5). Die Gestalt dafür ist der zickzackende
Aufstieg — Stufe −1 im einen Abschnitt, Stufe −2 im anderen —, also genau die 390 Ansichten, die
M31‑1 gezählt hat. Das ist der Teil der Regel, der beim Umbauen als Erstes verloren geht.

**Was dabei nicht passiert ist:** keine Komponente zerlegt, keine Änderung an einer Komponente,
damit ein Test sie greifen kann. Die Hülle ist ein Query-Client mit gestellten Antwortrümpfen und
der Sprachprovider (`tests/hilfe/rendern.tsx`); die Anzeigezone braucht keinen, weil sie ohne
Kontext auf UTC zurückfällt.

---

## 9. Was hier ausdrücklich nicht passiert

- ~~**Keine Änderung an der Nachrichtenliste.**~~ **Erledigt in Teil 2a (11.08.2026):** Der
  Ausblende-Schalter, der Chip, der Parameter `zwischenschritte` und `/api/nachrichten/merkmale`
  sind entfallen — Backend und Oberfläche gemeinsam, weil getrennt ausgeführt das Projekt zwischen
  den Teilen kaputt bliebe. Begründung und Zahlen in
  [`nachrichtenliste.md`](nachrichtenliste.md) §5.
- ~~**Keine Aufteilung von `ZWISCHENSCHRITT`.**~~ **Erledigt in Teil 2a:** `AUFGETEILT` und
  `ZUSAMMENGEFUEHRT` sind an seine Stelle getreten ([`message-status.md`](message-status.md)).
  `istEndstatus` ist unverändert.
- ~~**Keine Oberfläche.**~~ **Erledigt in Teil 2b (11.08.2026):** der Kettenblock im Detailpanel,
  §8. Kein eigenes Panel, keine eigene Route, keine Graphenvisualisierung.
- **Keine BAM-Werte** im Kettenglied. Ob ein Kind den Wert seines Elternteils zeigt, gehört zu
  Schritt 7 und braucht eine eigene Messung. Auch Teil 2b zeigt keine.
- **Keine Rollenkennzeichnung in der Liste.** Entscheidung des Auftraggebers; die Rolle erscheint
  ausschließlich im Detail. Die Flags wären dort der richtige Weg (§1) — gebraucht werden sie nicht.
- **Kein Hinweis auf die Verzögerung des `MatchInterchange`-Events.** Der Implementierungsplan nennt
  ihn für Schritt 6, und Teil 2b hat ihn **nicht** gebaut: Wie lange das Event nachläuft, ist
  **nicht gemessen** — weder in dieser Runde noch in einer früheren —, und Regel Q4 lässt keinen
  geratenen Satz zu. Er steht als offener Punkt in §11.
- **Keine Deutung von `MoveDTNA997`.** M27 hat die **Folge** für die Mandantengrenze geprüft, nicht
  die Ursache.

---

## 10. Regelbezug

| Regel | Wo umgesetzt |
|---|---|
| **M1** Kein Endpunkt nimmt eine Mandanten-ID entgegen | §2; die Ausnahmeliste bleibt bei zwei Einträgen |
| **M2** Mandant als erster Pflichtparameter | §7, `KettenRepository`, geprüft durch `PaketstrukturTest` |
| **M3** Filter im Statement, nicht nachgelagert | §6, geprüft durch `KettenStatementsTest` |
| **M4** Isolationstest je Endpunkt | §7, `KettenIsolationDbIT` — zwei Endpunkte, zwei Tests |
| **M5** Trennung gilt auch quer | §6 — für jedes Glied **und für die Zählung** |
| **L1** Pflicht-Zeitfenster | Nicht anwendbar: Die Menge ist durch einen Primärschlüssel benannt. Begründet in §2 |
| **L2** Keine Live-Aggregation über `Message` | Nicht berührt: Der `COUNT` ist ein `ref`-Zugriff auf höchstens 3.350 Zeilen zu **einer** benannten Wurzel, keine Dashboard-Kennzahl |
| **L3** Keine `OFFSET`-Paginierung | §2, Cursor über `(MessageLastUpdate, MessageID)` in der gemessenen ODER-Form. **Auch `abwaertsCursor`**: Er entsteht an derselben Stelle wie die Cursor des Blätter-Endpunkts, damit es nicht zwei Kodierungen gibt |
| **M5** für den `abwaertsCursor` | §6 — er beschreibt eine Zeile, die den Filter bereits passiert hat, und ist eine **Position, keine Berechtigung**; der Blätter-Endpunkt filtert unabhängig von ihm weiter |
| **API benennt den Mechanismus** *(neu am 11.08.2026)* | §2 — `aufwaerts`/`abwaerts` statt einer Bedeutung, die nur bei der Aufteilung stimmt. Die Bedeutung tragen `beziehung` und `ebene`; die Einteilung der Oberfläche (§8.3) ist unverändert |
| **L7** Jede neue Abfrage gemessen | §3 und [`messungen-schritt6.md`](messungen-schritt6.md) M30 |
| **Z1** Kein direkter `now()`-Aufruf | Die Zeitumrechnung nutzt die Zone der Anwendungsuhr; ein Zeitpunkt wird hier nirgends gebildet. Die Oberfläche rechnet keine Dauer — die Kette zeigt Zeitpunkte, keine Spannen |
| **404 statt 403** | §6, geprüft durch Vergleich beider Antwortrümpfe **und** der Laufzeit; die Oberfläche nennt für „gibt es nicht" und „fremd" denselben Text (§8.8) |
| **L7** für `rollen` im Detail-Kopf | Keine neue Abfrage: Die vier Spalten stehen auf der Zeile, die `findeKopf` ohnehin liest (§8.1). `EXPLAIN` und Laufzeit bleiben die aus [`nachrichtendetail.md`](nachrichtendetail.md) §8 |
| **Keine Zeichenkette in einer Komponente** | alles in `texte.nachrichten.kette`, einschließlich der beiden Abbruchsätze und der Beziehungsnamen |
| **Kein Farbwert in einer Komponente** | der Block trägt keine eigene Farbe (§8.9); `tests/farbwerte.test.ts` deckt die neuen Dateien ab |
| **Status nie allein über Farbe** | dieselbe `StatusPlakette`, kompakt: Zeichen plus Beschriftung im `title` und im Markup (§8.9) |
| **Die URL ist die einzige Quelle des Filterzustands** | ein Glied öffnet sich über `nachricht` beziehungsweise über die Route; der Nachladezustand ist **kein** Filterzustand und liegt in der Komponente (§8.6, §8.7) |

---

## 11. Offene Punkte

- **`RUNNING` in der Kette** ist unbeobachtbar. Der Status existiert in der Testkopie null Mal
  ([`message-status.md`](message-status.md)); ob eine laufende Nachricht in einer Kette vorkommt,
  bleibt bis zu einer Stichprobe gegen die Produktion offen.
- **Die Tiefengrenze ist ungetestet gegen echte Daten.** Sie greift in der Testkopie nie (vier
  Glieder gegen zehn erlaubte). Der Einheitstest deckt sie ab; ein Beleg aus der Produktion fehlt.
- ~~**Ob die Kette über mehr als eine Ebene abwärts gebraucht wird**, ist eine Frage an die
  Oberfläche und fällt in Teil 2.~~ **Beantwortet in Teil 2b, und zwar durch Verzicht:** Der Block
  zeigt eine Ebene, und wer tiefer will, öffnet ein Glied — das ist ein neuer Aufruf und kein
  tieferes Ergebnis (§8.6). Ob das im Betrieb reicht, ist damit **nicht** beantwortet; es steht
  hier, damit die Frage nicht als erledigt gilt.
- ~~**Die Reihenfolge der Vorgänger**~~ **Entschieden in Teil 2b:** Sie bleibt die des Aufstiegs
  (Ebene `-1` zuerst) und wird **nicht** umgedreht. Die nächste Stufe ist die, die ein Nutzer
  sucht; die Wurzel oben zu zeigen hieße, bei drei Ebenen von der entferntesten aus zu lesen.
- ~~**`/kette` liefert keinen Cursor, und deshalb wiederholt die erste nachgeladene Seite die schon
  gezeigten fünfzig.** Der Umweg ist eine Folge der Abgrenzung — Teil 1 bleibt unverändert — und
  kostet einen Zugriff bei dem, der nachlädt. Die saubere Lösung wäre ein Cursor an der
  Ketten-Antwort; er ist **nicht** gebaut, weil das eine Änderung am Endpunkt wäre. Wer ihn
  nachrüstet, streicht in `kette-block.tsx` den Nachzug der zweiten Seite.~~
  **Erledigt am 11.08.2026:** `abwaertsCursor` ist gebaut (§2), der Nachzug in `kette-block.tsx`
  ist gestrichen, ein Klick ist eine Anfrage (§8.7).
  **Warum der Punkt überhaupt entstanden ist:** Die Abgrenzung von Teil 2b hat Teil 1 ausgenommen,
  und ein Feld an `/kette` wäre eine Änderung am Endpunkt gewesen. Der Umweg war damit keine
  Nachlässigkeit, sondern der Preis einer Schnittlinie — und sie hat genau einen Tag gehalten. Das
  ist der Vermerk wert: Eine Abgrenzung, die einen bekannten Umweg erzwingt, kauft weniger, als sie
  kostet, wenn der nächste Schritt ohnehin folgt.
- **Der Hinweis auf die Verzögerung des `MatchInterchange`-Events fehlt.** Der Implementierungsplan
  nennt ihn für Schritt 6; Teil 2b hat ihn nicht gebaut, weil **nicht gemessen ist, wie lange das
  Event nachläuft** — und ein Satz wie „kann einige Minuten dauern" wäre nach Regel Q4 geraten. Die
  Frage gehört an das Altsystem und nicht an eine Erhebung der Testkopie: Das Event gehört uns nicht
  ([`datenmodell.md`](datenmodell.md) §6).
  **Nachgetragen am 11.08.2026:** M31‑3 hat versucht, den Takt von der Testkopie aus zu belegen, und
  **er ist nicht belegt** — die direkte Auskunft ist durch fehlende Rechte verschlossen, und der
  volumengleiche Ballungsvergleich unterscheidet `COMMIT_RECEIVED` nicht von Status, die kein Event
  schreibt. Damit ist der Hinweis **weiterhin ungedeckt**, und zwar jetzt gemessen ungedeckt statt
  bloß ungemessen. Die Kennzeichnung in
  [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §3.3 ist am selben Tag nachgezogen.
- **Zwischen „Kinder existieren" und „Flag gesetzt" zeigt das Panel gar nichts** *(neu am
  11.08.2026)*. Der Block entfällt bei leerem `rollen`, samt der Anfrage auf `/kette` (§8.2) — und
  das ist richtig entschieden. Aber `SPLIT_WURZEL` und `MERGE_ERGEBNIS` stammen nicht aus einer
  Beziehung, sondern aus den **Flags** `Source` und `Target` (§5), und die setzen **Events, die uns
  nicht gehören**: `SetTargetFlag` hat in [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §3.3
  nicht einmal einen Takt. Solange das Flag nachläuft, gibt es keine Fläche, keinen Hinweis und
  **keine Anfrage** — genau der Ausgang, den dieses Werkzeug verhindern soll: Der Nutzer sieht nicht
  „noch nichts da", sondern gar nichts.

  **E4 hat die Gegenprobe sauber gemacht** — 0 von 5.770 Zeilen mit `Source = 0` haben ein Kind, 0
  von 6.243 mit `Target = 0` werden angezeigt. Nur: auf einem **Standbild**, in dem alle Events
  längst nachgezogen sind. **Eine Testkopie kann ein Nachlauffenster strukturell nicht abbilden.**
  Das ist dieselbe Argumentation, die dieses Projekt bei `RUNNING` bereits akzeptiert hat
  ([`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §4.1): null Treffer in der Testkopie, in der
  Produktion sehr wohl vorhanden. Dazu steht E4 für `Target = 1` auf **sechs** positiven
  Beobachtungen — belastbar für die Richtung, dünn für eine Zusicherung.

  **Hier wird nichts gebaut.** Wo ein Hinweis stünde und unter welcher Bedingung er erschiene, ist
  eine Gestaltungsfrage fürs Sparring; ein geratener Satz über die Dauer wäre nach Regel Q4 ohnehin
  nicht zulässig. Aufgeschrieben ist er, damit die Lücke nicht als „gemessen ausgeschlossen" gilt.
- **Der Block ist gegen keine tiefe und keine abgebrochene Kette gesehen worden.** Die tiefste Kette
  der Testkopie hat vier Glieder (M30‑3), Tiefengrenze und Zyklusschutz sprechen dort nie an — die
  beiden Sätze aus §8.5 sind deshalb ausschließlich durch Vitest belegt und nicht durch eine
  Sichtprüfung. Dieselbe Art von Lücke wie `LAEUFT_AUF` in
  [`nachrichtendetail.md`](nachrichtendetail.md) §10.12: **eine Unmöglichkeit, kein Versäumnis.**
  **Nachgezogen am 11.08.2026 (§8.14):** Beide Sätze sind jetzt an einem **gerenderten Baum** belegt
  — samt ihrer Lage unter beiden Abschnitten —, nicht mehr nur an der Entscheidung dahinter. Der
  Punkt bleibt trotzdem stehen: Ein Beleg aus echten Daten fehlt weiter, und er kann in der
  Testkopie nicht entstehen. **Er ist deshalb keine offene Sichtprüfung** und steht nicht in der
  Tabelle in [`README.md`](README.md).
- **Die breiteste Wurzel des Bestands ist nicht gesehen.** Zwei Stichproben haben höchstens 169
  Teile gefunden, gemessen sind 3.350 (§8.11). Was daran offen bleibt, ist **die Länge des Panels**
  — 3.350 Zeilen zu je 36 px sind rund 120 Meter, und ob das Nachladen dort noch das richtige
  Bedienelement ist, sagt keine Messung. Ein Sprung in die Liste ist es nach L1 jedenfalls nicht
  (§8.7).
  **Vorausverweis auf Schritt 7** *(ergänzt am 11.08.2026)*: Die 3.350 sind vermutlich **kein
  Blätterproblem, sondern ein Suchproblem** — wer wissen will, welches der 3.350 Kinder seines ist,
  will die BAM-Suche und keine 67 Klicks. Die Suche **innerhalb** der Kette gehört damit in die
  Abwägung zu Schritt 7 und ist dort ausdrücklich zu prüfen.
- **Das schmale Fenster ist nicht gesehen** (§8.11, Punkt 18). Die Browsersteuerung kann das Fenster
  nicht verkleinern; geprüft ist das Regelwerk, nicht die Darstellung. Derselbe offene Punkt wie in
  [`nachrichtendetail.md`](nachrichtendetail.md) §10.10 und aus demselben Grund — er gehört von Hand
  nachgeholt. Der Kettenblock liegt im Panel und erbt dessen Breitenverhalten; eigene Umbruchpunkte
  hat er nicht.
- **Eine ganze Seite Abwärtsglieder ohne `MessageLastUpdate` lässt sich nicht blättern.** Der
  Sortierschlüssel ist `(MessageLastUpdate, MessageID)`; eine Zeile ohne Zeitpunkt hat darin keine
  Position. Gemessen kommt das nicht vor (`0` von 3.341.519, M30‑6), und eine *einzelne* solche
  Zeile ist unschädlich, weil `null` zuerst sortiert. **Seit dem 11.08.2026 gehört ein Satz dazu:**
  Trifft es die letzte Zeile der Kettenantwort, bleibt `abwaertsCursor` `null`, und der Block zeigt
  dann **keine** Schaltfläche zum Nachladen — auch wenn `weitereVorhanden` wahr ist. Bliebe der Fall
  je übrig, wäre die saubere
  Lösung ein Cursor, der den `NULL`-Block eigens adressiert — gebaut ist er nicht, weil es für ihn
  keinen einzigen Beleg gibt.
