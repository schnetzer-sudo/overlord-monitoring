# Dashboard — die Oberfläche

Stand: 01.09.2026 · Schritt 10b‑3b · **Frontend**

Die Landingpage. Der Endpunkt und seine Begründung stehen in
[`dashboard.md`](dashboard.md) — **die Datei ist der Vertrag**, hier steht, was die Oberfläche
daraus macht und warum sie es so macht.

**Keine Backend-Änderung, kein neuer Endpunkt, keine Migration.** Höchste Migrationsversion bleibt
`V12`.

---

## 1. Route und Navigation

**Das Dashboard liegt auf `/`** (Entscheidung E‑r). Die bewusst leere Startseite ist **gefüllt**
worden, nicht ersetzt: dieselbe Route, derselbe Navigationseintrag, keine Weiterleitung, kein
`/dashboard` daneben.

Das war seit Schritt 3 so vorgesehen und stand als Kommentar an beiden Stellen:

| Datei | Was schon dastand |
|---|---|
| `lib/routen.ts` | *„Kein eigener `/dashboard`-Pfad: Das Dashboard entsteht ab Schritt 10 auf der Startseite, nicht daneben."* |
| `lib/navigation.ts` | *„**Startseite und Dashboard sind ein Eintrag, nicht zwei.** […] Zwei Menüpunkte auf dieselbe Sache wären vom ersten Tag an Ballast."* |

> ### ⚠️ Befund: Der Navigationseintrag war schon da
>
> Der Auftrag verlangt *„einen Navigationseintrag ‚Übersicht' — ohne ihn gibt es keinen
> Rückweg"*. **Den Rückweg gab es bereits**; `NAVIGATION` führt seit Schritt 3 einen Eintrag auf
> `/`. Zu tun war deshalb nicht, einen anzulegen, sondern **einen umzubenennen**: Er hieß
> „Startseite" — ein Wort über den *Ort* — und heißt jetzt „Übersicht", ein Wort über den
> *Inhalt*.
>
> **Der Schlüssel bleibt `startseite`.** Er gehört zur Route und nicht zur Beschriftung; ihn
> mitzuändern hieße, `lib/navigation.ts`, beide Sprachdateien und den Typ `Texte` für eine
> Umbenennung anzufassen, die der Nutzer gar nicht sieht.

Dabei ist der Block `startseite` aus beiden Sprachdateien **entfallen**. Er trug `titel`,
`platzhalterTitel` und `platzhalterHinweis` — Texte eines Platzhalters, den es nicht mehr gibt. An
seine Stelle ist `dashboard` getreten.

---

## 2. Ein Aufruf, sieben Blöcke

Die Seite holt **eine** Antwort und baut alles daraus. **Kein Block lädt nach** — auch die
Verteilung beim Umschalten der Sicht nicht: Ein Sichtwechsel ist ein neuer Aufruf **derselben
Adresse mit anderem Parameter** und kein Teilnachladen.

Umgesetzt ist das über den Abfrageschlüssel (`features/dashboard/api.ts`):

```ts
landingpage: (zeitraum, sicht) => ["dashboard", "landingpage", zeitraum, sicht]
```

**Beide Parameter gehören hinein**, denn beide sind Anfrageparameter: eine andere Sicht ist eine
andere Antwort. Der Mandant steht aus demselben Grund **nicht** darin, aus dem er in keinem
anderen Schlüssel steht — beim Wechsel wird der gesamte Zwischenspeicher **geleert** und nicht
invalidiert ([`frontend-grundlagen.md`](frontend-grundlagen.md) §5).

**`null` und nicht das gewählte Paar.** Der Schlüssel trägt genau das, was in der Anfrage steht.
Schriebe die Ansicht das vom Endpunkt gewählte Paar zurück, entstünde beim ersten Rendern ein
zweiter Schlüssel und damit **eine zweite Anfrage für dieselbe Antwort** — und der Aufruf ohne
Parameter, der den Endpunkt eine Belegungsprobe extra kostet, wäre nach einer Sekunde ohnehin
verschwunden.

### Die Reihenfolge der Blöcke

Kacheln → Verlauf mit Fehlerstreifen → Zuletzt aufgefallen → Verteilung → Stand.

**Sie folgt dem Leitsatz und nicht der Reihenfolge des Vertrags.** *Das Werkzeug wird geöffnet,
wenn etwas nicht stimmt* — wer es öffnet, will zuerst wissen **ob**, dann **seit wann**, dann
**welche**. Die Verteilung beantwortet keine dieser drei Fragen; sie ist Hintergrund und steht
deshalb unten, am breiten Fenster neben den Zeilen statt über ihnen.

---

## 3. ⚠️ Die Sichtprobe A.2 — und was sie entschieden hat

[`visuelles-konzept.md`](visuelles-konzept.md) §7a hat die Rolle `--ueberfaellig` gerechnet und
dabei zwei Zahlen gemessen, die **niemand auf ihre Begegnung im Dashboard bezogen hat**:

| | |
|---|---|
| `--ueberfaellig-flaeche` → `--status-fehler-flaeche` | **0,025** — der Bestand liegt bei 0,049, also doppelt so weit |
| die Konturen | die neue bei **L 0.65**, die vier bestehenden bei **L 0.85 bis 0.90** |

§7a begründet die **Helligkeit** des Vordergrunds ausdrücklich damit, dass zwei Kacheln
nebeneinander, von denen eine dunkler steht, sich als **Rangfolge** lesen — und Regel Q3 führt
beide Kategorien gleichrangig. **Bei Fläche und Kontur ist diese Überlegung nicht gezogen worden.**

### Wie geprüft worden ist

Eine temporäre Route `src/app/kachelprobe/page.tsx`, **außerhalb der Gruppe `(app)`** — dieselbe
Bauform wie die Farbprobe aus [`frontend-grundlagen.md`](frontend-grundlagen.md) §8a, damit weder
Anwendungsrahmen noch Backend noch Anmeldung im Weg stehen. Statische Zahlen, kein Netz.

Angesehen wurden **drei Fassungen** derselben zwei Kacheln und derselben *gemischten*
Plakettenliste — Fehler- und Überfällig-Zeilen abwechselnd untereinander, die dichtere Begegnung
von beiden:

| Fassung | Was sie trägt |
|---|---|
| **A** | Fläche **und** Kontur — die Fassung, die `statusKlassen` seit Schritt 4 liefert |
| **B** | nur Fläche und Vordergrund |
| **C** | nur Vordergrund auf `--card` |

Kopfloses Chrome über das DevTools-Protokoll, 1440 px, `deviceScaleFactor: 2`. **Der Probecode ist
entfernt**; es bleibt dieser Befund.

### Was zu sehen war

**Fassung A ist der Fall, vor dem §7a warnt — nur andersherum als erwartet.** Die
Überfällig-Kachel bekommt einen **deutlich gezeichneten** Rand, die Fehler-Kachel praktisch
keinen. Nebeneinander liest sich das als Rangfolge, und zwar mit **Orange vorn** — ausgerechnet die
Kategorie, die *nicht* die dringendere ist. In der gemischten Plakettenliste ist es schärfer, weil
dort **jede zweite Zeile** den Ring trägt: Die Liste sieht gestreift aus, und die Streifen bedeuten
nichts.

Das ist keine Überraschung, sondern Befund 4 aus §7a an einer zweiten Stelle: *„Die neue Kontur ist
bei L 0.65 deutlich dunkler als die vier bestehenden bei L 0.85 bis 0.90 und wird als Plakettenrand
kräftiger wirken."* Gemessen sind die Kontraste gegen `--card`: **3,29 : 1** gegen 1,35 : 1 bis
1,57 : 1.

**Fassung C nimmt beiden Kacheln ihren Stand.** Sie sehen dann aus wie die neutrale Kachel
*Nachrichten* — ein weißes Feld mit farbiger Schrift. Gleichrangig sind sie damit, aber ein
Dashboard, auf dem die Fehlerkachel aussieht wie die Zählkachel, begräbt genau das, wofür das
Werkzeug geöffnet wird. Als Plakette verliert sie zusätzlich die Gestalt, die im ganzen Projekt
*„hier steht ein Zustand"* bedeutet.

**Fassung B trägt.** Beide Flächen liegen bei L 0.96 und Chroma 0.028 — dieselbe Helligkeit,
dieselbe Sättigung, keine trägt einen Ring, den die andere nicht hat. Die beiden Zahlen stehen bei
L 0.52 und damit ebenfalls gleich. Gegen die neutrale Kachel heben sich beide gleichermaßen ab, und
das ist richtig: Q3 verlangt Gleichrangigkeit zwischen **Fehler und Überfällig**, nicht zwischen
einer Problemkategorie und einer Zählung.

### Die Entscheidung — E‑u

> **In dieser Ansicht tragen die beiden Problemkategorien Fläche und Vordergrund und
> keine Kontur.** Für die Kachel **und** für die Plakette, mit derselben Begründung und ohne
> Unterschied zwischen beiden.

**Für Kachel und Zeile fällt sie gleich aus**, und das ist der eigentliche Ertrag der Probe: Der
Fehler ist derselbe, nur in der Zeile dichter. Zwei verschiedene Regeln für dieselbe Frage wären
zwei Stellen, an denen jemand die falsche wählt.

| | |
|---|---|
| **Was sie nicht ist** | eine neue Farbe. §7a ist abgeschlossen; entschieden ist, **welche der drei Werte** die Ansicht benutzt, nicht wie sie aussehen |
| **Was sie kostet** | nichts auf der Fehlerseite: Deren Kontur ist mit 1,57 : 1 ohnehin kaum zu sehen, Fassung A und B unterscheiden sich dort mit bloßem Auge nicht |
| **Was mit der Kontur ist** | sie bleibt, wo sie hingehört. `--ueberfaellig-kontur` erfüllt als einzige der fünf die 3 : 1 aus WCAG 1.4.11 — und die gilt für **Umrisse von Bedienelementen**. Eine Kachel ist keines; ihre Erkennbarkeit trägt der Ring, den `Card` ohnehin allen dreien gleich gibt |
| **Was ausdrücklich nicht geschieht** | die vier bestehenden Konturen nachzudunkeln oder die neue aufzuhellen. §7a schließt beides aus: *„Wer sie auflöst, tut es für alle fünf Rollen zugleich und rechnet dabei den ganzen Bestand nach."* |

### Umgesetzt an genau einer Stelle

`lib/status-farbe.ts` hält die Zuordnung Rolle → Token weiterhin allein
([`visuelles-konzept.md`](visuelles-konzept.md) §2). Geteilt ist dort nicht der **Wert**, sondern
seine **Verwendung**:

```ts
statusKlassen(art)              // alle drei Werte  — eine Rolle allein (Liste, Detail, Kette)
statusKlassenOhneKontur(art)    // zwei der drei    — zwei Rollen nebeneinander (Dashboard)
problemKlassen(kategorie)
problemKlassenOhneKontur(kategorie)
```

**Keine Komponente kennt dabei eine Farbe**, und `tests/farbwerte.test.ts` bleibt ohne neue
Ausnahme. `border-transparent` gehört zur Fassung ohne Kontur und ist kein Beiwerk: `Badge` mit
`variant="outline"` setzt sonst `border-border` und zöge einen grauen Ring an genau die Stelle, die
leer bleiben soll.

### Was die Farbe hier **nicht** leistet, und warum das genügt

Die beiden Flächen liegen 0,025 auseinander — bei Plakettengröße ist Rosa gegen Creme ein
schwacher Unterschied. **Das ist die halbe Aussage, und mehr war sie nie.** Jede Kachel und jede
Zeile trägt zusätzlich ein **Wort** und ein **Zeichen**, und die beiden Zeichen unterscheiden sich
in der Form und nicht in der Farbe: Warndreieck gegen Uhr. Genau so steht es in
[`visuelles-konzept.md`](visuelles-konzept.md) §3 und im Kopf von `lib/status-farbe.ts` — *„nie
allein über Farbe"* —, und §7a hat es für diese Rolle noch einmal ausdrücklich festgehalten:
*„Für zwei Kacheln, die nebeneinander stehen, heißt das Wort und Zeichen an jeder von beiden."*

---

## 4. Die Entscheidungen mit Datum

| | Entscheidung | Datum |
|---|---|---|
| **E‑l** | Der Verlauf zeigt **vier Reihen, nicht acht** — eine je Farbrolle. Die Legende hat vier Einträge, der Tooltip nennt die enthaltenen Einordnungen einzeln | 01.09.2026 |
| **E‑m** | Klickbar sind **Fehler** und **Überfällig im Fenster**. *Überfällig insgesamt* und *Nachrichten* tragen keinen Verweis | 01.09.2026 |
| **E‑n** | In der URL steht **nur die ausdrückliche Wahl**. Der vom Endpunkt gewählte Zeitraum wird nie zurückgeschrieben | 01.09.2026 |
| **E‑o** | Zeitpunkte **absolut** in der Anzeigezone, nie relativ | 01.09.2026 |
| **E‑p** | Der Leerzustand zeigt **einen Satz und den bedienbaren Umschalter**, sonst nichts | 01.09.2026 |
| **E‑q** | `ermittelbar: false` wird zu gedämpftem Text mit Zeichen — **keine `0`**, kein Rot, kein Fehlerzustand | 01.09.2026 |
| **E‑r** | Das Dashboard liegt auf **`/`** | 01.09.2026 |
| **E‑t** | Ein **zweiter, schmaler Balkenstreifen** unter dem Verlauf, nur `FEHLER`, mit **eigener beschrifteter Skala** | 01.09.2026 |
| **E‑u** | Die beiden Problemkategorien tragen **Fläche und Vordergrund, keine Kontur** — Kachel wie Plakette (§3) | 01.09.2026 |
| **E‑v** | Die Balkenbreite ist **gedeckelt** (`maxBarSize={28}`), an beiden Diagrammen mit demselben Wert. Wo die Slotbreite darunter liegt, bewirkt der Deckel nichts (§5.2) | 01.09.2026 |

---

## 5. Die sieben Blöcke

### 5.1 Der Zeitraumumschalter und die URL (E‑n)

Drei Schaltflächen: 48 Stunden, 30 Tage, 12 Monate. Dazu der Umschalter der Verteilung
(Partner ⇄ Richtung).

| | |
|---|---|
| Ohne Klick | **kein** `zeitraum` in der URL, **kein** `verteilung`. Der Endpunkt wählt ([`dashboard.md`](dashboard.md) §3) |
| Nach einem Klick | der gewählte Wert steht in der URL, über `nuqs`, wie in jeder anderen Ansicht |
| **Nie zurückgeschrieben** | das vom Endpunkt **gewählte** Paar landet nicht in der URL. Es steuert allein, welche Schaltfläche hervorgehoben ist |

Das ist die Regel aus [`frontend-grundlagen.md`](frontend-grundlagen.md) §8: *Ein Standardwert, der
etwas weglässt, gehört in die URL; einer, der etwas setzt, nicht.*

**Der Unterschied zwischen *gewählt* und *gewirkt* ist der ganze Inhalt dieser Entscheidung.** Wäre
er keiner, müsste die Ansicht das Paar der Antwort zurückschreiben — und dann entstünde beim ersten
Rendern ein zweiter Abfrageschlüssel und damit **eine zweite Anfrage für dieselbe Antwort**. Der
Nebeneffekt geht ohnehin in die richtige Richtung: Ein Aufruf **mit** `zeitraum` kostet den Endpunkt
die Belegungsprobe weniger — aber nur, wenn der Parameter eine Absicht ausdrückt.

`verteilung=PARTNER` ist die Vorgabe des Endpunkts und steht aus demselben Grund nicht in der URL;
`RICHTUNG` schon. **Das ist eine Ableitung aus E‑n, keine neue Entscheidung.**

`hervorgehobenerZeitraum(zustand, ausDerAntwort)` hält den Unterschied als reine Funktion fest: Die
Wahl schlägt die Antwort, und ohne beides ist **keine** Schaltfläche gedrückt — eine vorgemerkte
wäre eine Vermutung, die beim Eintreffen der Antwort springt.

### 5.2 Der Verlauf: vier Reihen, nicht acht (E‑l)

Der Endpunkt liefert je Eimer die vorkommenden **Einordnungen** — acht mögliche.
`lib/status-farbe.ts` bildet sie auf **vier** Farbrollen ab. Acht Reihen mit vier Farben ergäben
einen Balken, in dem `ABGESCHLOSSEN` und `QUITTIERT` sowie `WARTEND`, `LAEUFT`, `AUFGETEILT` und
`ZUSAMMENGEFUEHRT` jeweils farbgleich aneinanderstoßen und **wie ein Segment aussehen**. Die Legende
verspräche acht Unterscheidungen, das Bild lieferte vier.

| | |
|---|---|
| **Der Stapel** | vier Reihen, eine je Farbrolle. Die Zusammenfassung ist eine **reine Funktion** (`features/dashboard/verlauf.ts`) und wird ohne Ansicht geprüft |
| **Die Reihenfolge** | `fehler, offen, abgeschlossen, ungeklaert`, fest und über alle Eimer dieselbe. Recharts stapelt in der Reihenfolge der Reihen im Baum, und das erste Segment sitzt **unten an der Achse** — dort steht *Fehler*, die Kategorie, wegen der jemand das Werkzeug öffnet |
| **Die Legende** | vier Einträge — und **eine Rolle, die über alle Eimer null ist, bekommt keinen**. Ein Eintrag ohne Segment im Bild verspricht eine Unterscheidung, die es nicht gibt |
| **Der Tooltip** | nennt die enthaltenen Einordnungen **einzeln mit ihren Zahlen**. Dort geht der Unterschied zwischen *aufgeteilt* und *zusammengeführt* nicht verloren |

**Farben kommen aus `lib/status-farbe.ts`, nie nach Position vergeben** — `rollenfuellung(rolle)`
liefert den fertigen `var(--token)`-Wert für das Prop. Ein zusammengesetztes
`var(--status-${rolle})` stünde nirgends vollständig im Quelltext und wäre bei einer Umbenennung
nicht auffindbar.

> ### Ein unbekannter Wert fällt nach `ungeklaert` und in keinen belegten Eimer
>
> Das Backend liefert einen Aufzählungswert; eine neunte Einordnung entstünde nur, wenn dort eine
> dazukäme. Dann gilt Regel Q4: Sie fällt in die Rolle, deren ganze Bedeutung *„unbekannter
> Statuswert"* ist — und ausdrücklich nicht nach `offen` oder `abgeschlossen`.
>
> **Weggelassen wird sie nicht.** Dann wäre der Balken niedriger als `gesamt`, und die Zahl im
> Tooltip passte nicht zu dem, was danebensteht. Der Test prüft beides.

#### Die Balkenbreite ist gedeckelt (E‑v) — *gemessen am 01.09.2026*

**Das Problem, gemessen:** Bei zwölf Eimern über die volle Fensterbreite war der Balken **94 px
breit in einem Slot von 96,33 px** — 97,6 % Füllung, 2,33 px Luft zwischen zwei Zählungen. Der
Verlauf las sich damit als **eine zusammenhängende Farbfläche** und nicht als Reihe einzelner
Eimer. Bei 48 Eimern trat das nicht auf: dort sind es 22 px in einem Slot von 24,33 px. **Das Bild
hängt an der Zahl der Eimer und nicht an der Bauform.**

**Die Antwort ist ein Deckel und keine feste Breite:** `maxBarSize={28}`. Recharts rechnet
`min(Slotbreite, maxBarSize)` und rückt den schmaleren Balken in die **Mitte** seines Slots; wo die
Slotbreite ohnehin darunter liegt, bewirkt die Zahl **nichts**. Genau das unterscheidet sie von der
gerechneten Konstante, die an dieser Stelle schon einmal gescheitert ist (`achsenabstand`, §5.3):
Eine Zahl, die von der Breite nichts weiß, kann bei 360 und bei 1500 px nicht beide Male richtig
sein — ein Deckel muss das auch nicht, er tritt nur an der einen Seite in Kraft.

**Gemessen an den `<path class="recharts-rectangle">`-Knoten im DOM** (Attribute `x` und `width`),
im Profil `dev` am Anker `2025-12-30 04:09:47`, angemeldet als `NEXANS`. Die Slotbreite ist der
Abstand zweier benachbarter Eimer, die Lücke ist Slot minus Balken:

| Fenster | Eimer | Slot | Balken **vorher** | Balken **jetzt** | Lücke vorher | Lücke jetzt | Füllung jetzt |
|---:|---:|---:|---:|---:|---:|---:|---:|
| 360 px | 48 | 5,25 | 3 | **3** | 2,25 | 2,25 | 57,1 % |
| 360 px | 30 | 8,20 | 6 | **6** | 2,20 | 2,20 | 73,2 % |
| 360 px | 12 | 20,00 | 18 | **18** | 2,00 | 2,00 | 90,0 % |
| 768 px | 48 | 8,77 | 7 | **7** | 1,77 | 1,77 | 79,8 % |
| 768 px | 30 | 13,83 | 12 | **12** | 1,83 | 1,83 | 86,8 % |
| 768 px | 12 | 34,08 | 32 | **28** | 2,08 | 6,08 | 82,2 % |
| 1500 px | 48 | 24,33 | 22 | **22** | 2,33 | 2,33 | 90,4 % |
| 1500 px | 30 | 38,73 | 37 | **28** | 1,73 | 10,73 | 72,3 % |
| 1500 px | 12 | 96,33 | 94 | **28** | 2,33 | 68,33 | 29,1 % |

**Der Deckel greift in drei von neun Fällen.** In den anderen sechs sind `x` **und** `width` jedes
einzelnen Rechtecks vor und nach der Änderung **zeichengleich** — nachgesehen und nicht
gefolgert. **Bei 360 px greift er in keinem der drei Paare**, und das ist die Stelle, an der die
Zahl zu prüfen war: 48 Eimer ergeben dort 3 px Balken, und `min(3, 28) = 3`.

**Verlauf und Fehlerstreifen stehen weiter übereinander.** Geprüft ist nicht die Absicht, sondern
die Koordinate: die Menge der `x`-Werte des Streifens gegen die des Verlaufs — bei `NEXANS` in
allen neun Fällen deckungsgleich und bei gleicher Balkenbreite, bei `SUTTONS` in den sechs, in
denen der Streifen überhaupt Balken hat. Deshalb steht der Wert als **eine** Konstante
(`MAX_BALKENBREITE`) an beiden Diagrammen — aus demselben Grund wie die gemeinsame Achsenbreite.
Zwei verschiedene Deckel trennten die Balken bei kleiner Eimerzahl, und die gemeinsame Zeitachse
verspräche dann eine Zuordnung, die es nicht gäbe.

Beim kleinen Mandanten (`SUTTONS`) dieselben Zahlen bis auf die schmalere y-Achse: 12 Eimer bei
1500 px von 95 auf 28 px bei einem Slot von 96,83, bei 768 px von 34 auf 28. Über 48 Stunden hat
`SUTTONS` **keinen** Eimer mit Fehlern; der Streifen ist dort leer und zeigt seinen Satz — der
Leerzustand aus E‑t, unverändert.

> **Belegvermerk (L10).** *Gemessen war:* Balken- und Slotbreite an den Rechteck-Knoten beider
> Diagramme, für drei Fensterbreiten × drei Paare × zwei Mandanten, dazu die Zeichengleichheit von
> `x` und `width` vor und nach der Änderung. *Behauptet wird:* Der Deckel wirkt genau dort, wo der
> Slot breiter als 28 px ist, verändert am schmalen Fenster nichts und trennt die beiden Diagramme
> nie. — **Nicht gemessen, sondern Augenschein** ist der Schluss, dass 28 px die Balken *als
> einzelne Zählungen* lesbar machen: Das ist eine Aussage über das Bild und keine über eine Zahl.

**Die eine gemessene Grenze:** Bei **768 px und zwölf Eimern** bleiben 6,08 px Lücke bei 82 %
Füllung. Die Balken trennen sich sichtbar — vorher waren es 2,08 px —, aber der Block liest sich
dort weiter dicht. Ein kleinerer Deckel vergrößerte die Lücke dort und machte den Balken überall
sonst schmaler; **wo dieser Abwägungspunkt liegt, ist nicht gemessen.** Entschieden ist er
zugunsten der Breite, an der das Problem aufgetreten ist — und der Deckel bleibt an einer Stelle
änderbar.

**E‑l und E‑t bleiben unberührt.** Vier Reihen, ihre Reihenfolge, der Fehlerstreifen als zweites
Diagramm mit eigener Skala, Legende, Tooltip und die Achsenlogik aus §5.3 sind nicht angefasst;
`maxBarSize` ist das einzige geänderte Prop. **Dies ist keine Entscheidung über die Diagrammform.**

### 5.3 Der Fehlerstreifen (E‑t)

**Das Problem, gemessen:** Über den Gesamtbestand ist die Einordnung `FEHLER` **0,03 %**. In einem
gestapelten Balken von 260 Pixeln Höhe ist das **unter einem Pixel** — die wichtigste Kategorie des
Werkzeugs wäre im Verlauf unsichtbar, bei jedem Mandanten und in jedem Fenster.

**Deshalb ein zweiter, schmaler Balkenstreifen unter dem Verlauf**, nur `FEHLER`, mit **eigener
Skala** und derselben Zeitachse. Kein zusätzlicher Lesevorgang — es sind dieselben Daten aus
Block 1, zweimal dargestellt.

> **Die eigene Skala trägt zwei Beschriftungen: null und ihren tatsächlichen Höchstwert.** Recharts
> rundete sonst von zwei auf vier auf und beschriftete mittendrin — der Streifen behauptete damit
> einen Kopfraum, den es nicht gibt. Genau darum geht es hier: **Es sind einzelne Zeilen und kein
> Anteil des Verkehrs.** Der Satz neben der Überschrift sagt dasselbe noch einmal in Worten, und die
> beiden getrennten Flächen sind der Grund, warum es **keine zweite y-Achse in einem Diagramm** ist.

**Die Zeitachse steht einmal, unter dem Streifen.** Beide Diagramme haben dieselbe Achsenbreite
(48 px), ihre Balken stehen damit übereinander; zweimal dieselbe Beschriftung wäre doppelt gelesener
Platz.

> ⚠️ **Berichtigung vom 01.09.2026 — die Klammer „(48 px)" stimmt nicht mehr, der Satz davor und
> danach schon.** Aufgefallen beim Nachmessen des Recharts-Baums für den Dichteumschalter
> ([`dichte-umschalter.md`](dichte-umschalter.md) §5.1).
>
> **Gemessen war:** Die Achsenbreite ist **gerechnet und nicht fest** —
> `Math.max(48, Math.ceil((laengste + 1) * 6.2) + 10)` in `features/dashboard/verlauf.ts`. Bei
> `NEXANS` über zwölf Monate steht der Achsentext bei `x = 52`, nicht bei 48.
>
> **Behauptet wird** hier „(48 px)". Zwölf Zeilen weiter, **im selben Abschnitt**, steht bereits das
> Gegenteil: *„Die Breite der y-Achse folgt der längsten Beschriftung … Eine feste Zahl schnitt bei
> `NEXANS` über zwölf Monate `220.000` zu `:20.000` ab (§10.4)."*
>
> Die Klammer ist beim Umbau desselben Tages stehen geblieben. **48 ist heute die Untergrenze der
> Rechnung, nicht die Breite.** Was der Satz *aussagt* — beide Diagramme bekommen **dieselbe**
> Breite, damit ihre Balken übereinanderstehen — ist unverändert richtig und am 01.09.2026 in allen
> vier Dichtestufen und bei drei Fensterbreiten nachgemessen: Versatz **0,0 px** in allen 24 Lagen.
> Gelöscht wird deshalb nichts.

**Bei 48 Eimern wird nicht jeder beschriftet, und wie viele es sind, entscheidet die Breite.**
`interval="equidistantPreserveStart"` mit `minTickGap={12}` wählt einen gleichabständigen
Ausschnitt, der in die vorhandene Breite passt — gemessen 6 Beschriftungen bei 360 px, 10 bei 768
und 24 bei 1500 (§10.4).

> Hier stand eine gerechnete Konstante (`achsenabstand`, Deckel zwölf). Sie trug am breiten Fenster
> und ließ die Beschriftungen **bei 360 px um 12 Pixel überlappen**. Eine Zahl, die von der Breite
> nichts weiß, kann bei beiden nicht richtig sein.

**Die Breite der y-Achse folgt der längsten Beschriftung**, die vorkommen kann, und beide
Diagramme bekommen dieselbe — sonst stünden ihre Balken nicht mehr übereinander. Eine feste Zahl
schnitt bei `NEXANS` über zwölf Monate `220.000` zu `:20.000` ab (§10.4).

> ### Die Null der Hauptachse ist nachgesehen und nicht geschätzt
>
> Recharts lässt eine Achsenbeschriftung weg, deren Textkasten über den Zeichenbereich hinausragte.
> Mit acht Pixeln Fußmarge fiel die **Null** deshalb weg und die Skala begann sichtbar bei 55 — der
> Balken sähe kürzer aus, als er ist. Nachgesehen an den `<text>`-Knoten im DOM: Mit vierzehn Pixeln
> steht sie da. Am Streifen entsteht die Marge ohnehin durch seine Zeitachse.

**Überfällig läuft nicht mit**, in keiner Variante: Die Kategorie entsteht live über `Message` und
steht nicht je Eimer im Rollup.

### 5.4 Die drei Kacheln (E‑m, E‑q)

Reihenfolge **Fehler, Überfällig, Nachrichten**. Sie folgt dem Leitsatz; die Zählkachel beantwortet
keine Frage, mit der jemand herkommt, und steht deshalb hinten.

| Kachel | Ziel |
|---|---|
| **Fehler** | `/nachrichten?status=FEHLER&von=…&bis=…`, Grenzen aus `fenster` |
| **Überfällig, im Fenster** | `/nachrichten?ueberfaellig=true&von=…&bis=…` |
| **Überfällig, insgesamt** | **nicht klickbar** |
| **Nachrichten** | nicht klickbar |

**Warum „insgesamt" nicht klickt:** Die Zahl hat bewusst kein Zeitfenster (Regel L9), die Liste hat
ein Pflicht-Zeitfenster (Regel L1). Jedes Ziel zeigte eine **andere Zahl** als die Kachel — und eine
Kachel, die auf eine andere Zahl führt als sie nennt, ist schlechter als eine, die nicht klickt. Ein
Satz sagt das an der Kachel.

**Die beiden Filter werden nie kombiniert.** `status=FEHLER` und `ueberfaellig=true` sind am
Listen-Endpunkt ausdrücklich unvereinbar und ergeben `400`. Die Adressen entstehen deshalb in **zwei
getrennten Funktionen**, und keine von beiden nimmt den anderen Parameter entgegen.

**Verlinkt ist ein Bereich der Kachel und nicht die ganze.** In der Fehlerkachel steht darunter die
Schaltfläche für die Aufschlüsselung, und ein `<button>` in einem `<a>` ist kein gültiges Markup.

> ### ⚠️ Der Verweis auf „überfällig" hat die Liste eine Änderung gekostet
>
> Das Backend kennt den Parameter seit Schritt 4 — **die Oberfläche kannte ihn nicht.** Ein Klick
> wäre auf der *ungefilterten* Liste gelandet, mit dem Zeitfenster der Kachel und ohne jeden
> Hinweis. Der Befund samt Bau steht in [`nachrichtenliste.md`](nachrichtenliste.md) §5e; hier steht
> nur, warum er hier auffiel: **Vor dem Dashboard gab es keinen Weg zu diesem Parameter.**

#### „Nicht ermittelbar" (E‑q)

| | |
|---|---|
| Der Kacheltitel | bleibt |
| An der Stelle der Zahl | gedämpfter Text mit Zeichen — **keine `0`**, kein Rot, keine Fehler-Kennung |
| Die Kachel | ist **nicht klickbar** |
| Ein Satz | nennt den Grund. Für den Nutzer ist das eine Auskunft, kein technischer Fehler |

**Beide Zahlen fallen zusammen** — das gibt der Vertrag vor. Die übrigen Blöcke stehen normal. Und
es gibt hier keine Schaltfläche „Erneut versuchen": Sie verspräche, dass ein zweiter Versuch etwas
ändern könnte, und die übrige Seite steht ja bereits.

#### Die Fehlerarten (C.4)

Aufschlüsselung nach Art **inline in der Kachel, aufklappbar**. Der Rohwert steht als `title`
daneben.

> ### ⚠️ Beschriftet wird über den **Rohwert**, nicht über das gelieferte Feld `art`
>
> [`dashboard.md`](dashboard.md) §2 hält fest, dass `MessageStatusClassifier.fehlerart` für
> `COMMIT_REJECTED` den **festen deutschen Text** *„Vom Partner abgelehnt"* liefert. Stünde der hier
> unverändert, läse ihn auch ein englischer Nutzer — der Endpunkt beschriftet an dieser einen Stelle
> und nicht die Oberfläche.
>
> **Aufgelöst ist das ohne Änderung am Backend:** `fehlerartText` schlägt den Rohwert in der
> Sprachdatei nach und nimmt `art` als Rückfall. Für jeden anderen Rohwert ist `art` kein
> Anzeigetext, sondern ein **Wert** — der Namensteil hinter `ERROR_` oder der Rohwert selbst —, und
> Werte werden nicht übersetzt (Regel Q4). Der Test belegt beide Richtungen, einschließlich eines
> Rumpfes, in dem `art` etwas anderes sagt.

### 5.5 Die Verteilung (Teil D)

**Ein Block, zwei Sichten.** Der Endpunkt liefert die Zeilen fertig sortiert und gebündelt; das
Frontend rechnet **nichts** nach — es sortiert nicht, es summiert nicht, es lässt nichts weg.

| Zeilenart | Darstellung |
|---|---|
| `WERT` | Rang 1 bis 10, absteigend |
| `UEBRIGE` | „Übrige (n)" aus `enthaltene`. **Kommt nicht, wenn der Endpunkt sie nicht liefert** |
| `NICHT_ZUGEORDNET` | **immer, auch bei null** |

**Beide Restzeilen stehen immer unten** — und genau deshalb wird hier nicht sortiert. Bei `IBIS`
wäre „Übrige (40)" mit 27,92 % der größte Balken des Blocks und stünde auf Rang 1, als gäbe es einen
Partner dieses Namens (M98, Befund 21). Der Test benutzt diese Gestalt als Prüfwert.

**Der Bezugswert der Balken ist der größte Wert des Blocks und nicht die Summe.** Gefragt ist der
Vergleich der Zeilen untereinander — und „Übrige" wäre in einer Anteilsrechnung doppelt enthalten.

**Keine Zeile ist klickbar**, und das steht als Satz da, damit niemand danach sucht: Die Liste kennt
keinen Partnerfilter, und der Umweg über Prozess-IDs ist in
[`nachrichtenliste.md`](nachrichtenliste.md) §5a mit **7.459 ms** gemessen.

**Der Balken trägt keine Statusfarbe.** Eine Verteilung sagt nichts über *gut oder schlecht*; eine
Farbe dort wäre eine Aussage, die es nicht gibt.

### 5.6 Zuletzt aufgefallen (Teil E)

Fehler und Überfällige im Fenster, neueste zuerst. Je Zeile Zeitpunkt, Plakette, Prozess bzw.
`sosName`, Verweis ins Nachrichtendetail auf seiner **eigenen Route** — `/nachrichten/<id>` und
nicht `?nachricht=<id>`, das brächte eine Liste mit, die niemand angefragt hat.

**Die Plakette trägt `kategorie`, nicht `status`.** Ein Rohstatus beantwortete hier eine andere
Frage — *welcher Fehler* statt *ist es einer*. Der Rohwert steht im `title` und für Vorleseprogramme
im Markup.

Beide Mengen sind **disjunkt** ([`dashboard.md`](dashboard.md) §7a): *Überfällig* setzt voraus, dass
die Nachricht nicht in einem Endstatus ist, und *Fehler* ist einer. Es gibt deshalb keine Zeile mit
zwei Plaketten.

### 5.7 Stand und Leerzustand (Teil F)

**Der Stand:** `beendetAm` und `art` aus `rollup_lauf`, **absolut in der Anzeigezone** (E‑o). Die
Laufart wird übersetzt, wenn sie bekannt ist (`VOLL`, `DELTA`), und sonst roh gezeigt (Regel Q4).
Fehlt der Lauf — oder sein Ende —, steht dort der Satz, dass es noch keinen gab.

**Der Leerzustand** (E‑p): der Satz aus `components/zustand.tsx` — Zustand *Leer*, nicht *Fehler* —
und **der Zeitraumumschalter bleibt bedienbar**. Sonst nichts: keine Kacheln mit Nullen, kein leeres
Diagramm, **auch nicht die Zeile „nicht zugeordnet"**. Sie sagt etwas über den Katalog; über einen
Mandanten ohne Nachrichten sagt sie nichts. Der Stand bleibt stehen — er sagt, wie alt die Auskunft
ist, und das gilt auch dann, wenn die Auskunft „nichts" lautet.

---

## 6. Die drei bekannten Grenzen

### 6.1 Der Leerzustand unterscheidet nicht

**Ein Text für beide Fälle** — „im Zeitraum nichts" und „dieser Mandant hat keine Daten" werden
nicht unterschieden. Der Satz ist in beiden Fällen wahr, und die Antwort trägt kein Feld, das sie
trennte ([`dashboard.md`](dashboard.md) §6, bekannte Grenze 2).

**Bekannte Folge:** Ein stiller Sonntag und `EDITIONLINGERI` sehen gleich aus. Der Hinweis unter dem
Satz nennt den Ausweg, der beides trennt — einen größeren Zeitraum wählen.

### 6.2 Der Stand ist absolut und nicht relativ

**Keine relative Zeit** (E‑o). Die Antwort trägt kein `jetzt`-Feld, und `formatiereRelativ` rechnet
gegen die Uhr des **Browsers**; im Profil `dev` stünde dort „vor acht Monaten", weil die
Anwendungsuhr auf dem 30.12.2025 steht. `fenster.bis` als Bezug hilft nicht: Bei `12M` liegt es bis
zu einen Monat von der Uhr entfernt.

**Bekannte Grenze:** *„Wie alt sind diese Zahlen"* bleibt eine Kopfrechnung. Ein `jetzt`-Feld
nachzurüsten wäre eine Backend-Änderung und gehört nicht in einen Frontend-Schritt.

### 6.3 Die Fehlerkachel klickt bei `12M` — **nachgesehen, nicht angenommen**

C.2 des Auftrags fragt, ob die Verlinkung bei `12M` trägt: Der Listen-Endpunkt weist eine Spanne
über einem Jahr mit `zeitfenster-zu-gross` ab, und das Dashboard rechnet `12M` in Kalendermonaten —
im Schaltjahr sind das 366 Tage.

**Die Antwort ist ja, und sie steht im Code:**

```java
// common/Zeitfenster.absolutes
// Ein Jahr als Kalenderjahr, nicht als 365 Tage — sonst haengt die Grenze am Schaltjahr.
if (von.isBefore(bis.minusYears(1))) { … "zeitfenster-zu-gross" … }
```

`Dashboardzeitraum.MONATE_12.fenster(jetzt)` setzt `von = letzter.minusMonths(11)` und
`bis = letzter.plusMonths(1)`, wobei `letzter` der Monatserste ist — das Fenster ist damit **genau
zwölf Kalendermonate von Monatsanfang zu Monatsanfang** und damit exakt ein Kalenderjahr.
`von.isBefore(bis.minusYears(1))` ist dann `false`. Die Prüfung greift nicht, auch nicht im
Schaltjahr.

**Die Umrechnung unterwegs ändert daran nichts:** Das Dashboard liefert die Grenzen als `Instant`
(UTC), der Listen-Endpunkt liest sie mit `Zeitpunkte.ausIso(wert, anwendungsuhr.getZone(), …)`
zurück — dieselbe Zone, aus der sie entstanden sind. Der Rundlauf ist verlustfrei.

**Verkürzt wird deshalb nichts.** Ein um einen Tag verkleinertes Fenster wäre derselbe Fehler wie
eine Kachel, die auf eine andere Zahl führt als sie nennt — und der Test hält fest, dass die Grenzen
Zeichen für Zeichen durchgereicht werden.

> ### ⚠️ Die eine Grenze, die bleibt: die obere Fenstergrenze ist verschieden gemeint
>
> Das Dashboard führt `fenster.bis` **ausschließend** (der Anfang des nächsten Eimers,
> [`dashboard.md`](dashboard.md) §2); der Listen-Endpunkt führt sein Fenster **beidseitig
> geschlossen** (`common/Zeitfenster`: `von <= MessageLastUpdate <= bis`). Eine Nachricht, deren
> `MessageLastUpdate` **exakt** auf der Eimergrenze liegt, zählt damit im Dashboard zum nächsten
> Eimer und in der Liste noch zum Fenster.
>
> **Nicht umgangen, und zwar bewusst.** Eine Sekunde abzuziehen machte die Adresse zu einem anderen
> Fenster als dem der Kachel — genau der Fehler, den C.1 und C.2 ausschließen. Der Unterschied ist
> auf einen einzigen Zeitpunkt begrenzt, das Umgehen wäre systematisch.

---

## 7. Was gemessen worden ist

### 7.1 Die Zahl der Anfragen

**Eine je Seitenaufruf, und eine je Sichtwechsel.** Belegt in `tests/dashboard-bloecke.test.tsx`
über einen `fetch`, der jeden Aufruf mitschreibt:

| Vorgang | Anfragen |
|---|---|
| Laden ohne Wahl | `["/api/dashboard"]` |
| Laden mit `?zeitraum=30T` | `["/api/dashboard?zeitraum=30T"]` |
| Klick auf „Richtung" | `["/api/dashboard", "/api/dashboard?verteilung=RICHTUNG"]` |

Der dritte Fall ist die eigentliche Aussage: **Ein Sichtwechsel lädt nicht nach, er ruft neu.** Es
ist dieselbe Adresse mit einem anderen Parameter und kein zweiter Endpunkt; wer zurückschaltet,
bekommt die vorherige Antwort aus dem Zwischenspeicher, ohne dass eine dritte Anfrage hinausgeht.

Und der zweite belegt E‑n von der anderen Seite: Die **Vorgabe** der Verteilung steht nicht in der
Adresse, der **gewählte** Zeitraum schon.

### 7.2 Der Zuwachs des Bündels durch Recharts

Gemessen an `next build` (Turbopack, Next.js 16.2.11) über **alles Client-JS** unter
`.next/static/chunks`. **Derselbe Baum zweimal gebaut**, einziger Unterschied: einmal mit dem echten
Diagramm, einmal mit einem Platzhalter, der `recharts` nicht importiert. Damit ist der Zuwachs
wirklich der von Recharts und nicht der des Dashboards.

| | Dateien | roh | gepackt (gzip) |
|---|---:|---:|---:|
| **ohne den Recharts-Import** | 34 | 1.155.813 B · **1.128,7 kB** | 356.070 B · **347,7 kB** |
| **mit Recharts 3.10.1** | 35 | 1.518.640 B · **1.483,0 kB** | 460.148 B · **449,4 kB** |
| **Zuwachs** | **+1** | **+362.827 B · +354,3 kB** | **+104.078 B · +101,6 kB** |

**Recharts landet in genau einem eigenen Chunk** — 375.294 B roh, 107.258 B gepackt; er trägt
`recharts` und `CartesianGrid` im Text und keinen der d3-Paketnamen (Recharts 3 bringt sie gebündelt
mit). Dass der Chunk etwas größer ist als der Zuwachs, ist Umverteilung zwischen den Chunks.

**Das ist keine Abweichung von der Erwartung.** Recharts 3 wird durchgängig mit rund hundert
Kilobyte gepackt geführt, und genau das ist gemessen worden: **101,6 kB.** Es gibt hier also nichts
zu melden und nichts wegzuoptimieren — und **selektive Importe sind ausdrücklich nicht versucht
worden**, weil die Zahl erst bekannt sein musste.

Zum Vergleich der Größenordnung: Vor diesem Schritt — ohne jedes Dashboard — lag dasselbe Maß bei
33 Dateien und 1.132.013 B roh. **Das ganze Dashboard-Frontend ohne Recharts kostet damit rund
23,8 kB roh.**

> ### Belegvermerk (Regel L10)
>
> *Gemessen ist:* die Summe aller `.js`-Dateien unter `.next/static/chunks` eines
> `next build`, roh und mit `gzip` gepackt, für zwei Bäume, die sich in genau einer Datei
> unterscheiden.
>
> *Behauptet wird:* Recharts 3.10.1 kostet dieses Frontend rund 102 kB über die Leitung.
>
> **Die Lücken.** Turbopack schreibt kein `app-build-manifest.json`; **eine Zahl je Route gibt es
> deshalb nicht**, gemessen ist das gesamte Client-JS. Und `gzip` ist nicht `brotli` — ein Server,
> der brotli spricht, liefert weniger aus.

---

## 8. Die Recharts-Lagen, die §8a offen gelassen hat

[`frontend-grundlagen.md`](frontend-grundlagen.md) §8a hat `var()` in einem Recharts-Prop für
`<Bar>`, Legende und Tooltip gemessen und drei Lagen ausdrücklich als **nicht angesehen**
ausgewiesen. Diese Ansicht fasst zwei davon nicht an und misst die dritte nach:

| Lage | Wie sie hier steht |
|---|---|
| `<Bar fill>` | gemessen in §8a. Benutzt |
| **Legende** | **nicht Recharts'** — eigenes Markup. Sie kann nicht, was gebraucht wird: eine Rolle weglassen, die über alle Eimer null ist |
| **Tooltip** | **eigener `content`** — eigenes JSX mit Tailwind-Klassen, also gar kein Recharts-Farbweg. Der Standard nennt je Reihe eine Zahl; gebraucht wird je Reihe eine **Liste** |
| `activeBar` | ausdrücklich `false` — in §8a als „nicht angesehen" ausgewiesen und hier nicht gebraucht |
| `Cell`, Farbverläufe | kommen nicht vor |
| **Achse, Gitter, Tooltip-Zeiger** | `var()` in einem Prop, **und das war nicht gemessen** — nachgeholt, Ergebnis in [`frontend-grundlagen.md`](frontend-grundlagen.md) §8a |

---

## 9. Offene Punkte

| Nr. | Punkt |
|---|---|
| **89** | **Es gibt keinen Weg, „nur überfällige" in der Liste selbst einzuschalten** — vergeben und begründet in [`nachrichtenliste.md`](nachrichtenliste.md) §9 |
| ~~**90**~~ | ~~Die Achsendichte ist am breiten Fenster angesehen und nicht am schmalsten.~~ ✔ **Erledigt am 01.09.2026, noch vor der ersten Abgabe.** Bei 360 px überlappten die Beschriftungen um 12 Pixel; die Konstante ist entfallen, die Dichte hängt jetzt an der Breite (§10.4). Der Punkt bleibt stehen, weil er die Messung trägt |
| **92** | **Die gedrückte Schaltfläche des Umschalters ist mit 1,07 : 1 kaum zu sehen** (§10.5). Für ein Vorleseprogramm ist der Zustand sauber ausgezeichnet, für das Auge nicht. Es ist die Gestalt von `components/ui/toggle-group.tsx` — Generatorbereich — und die **Zeitfensterwahl der Nachrichtenliste** trägt sie seit Schritt 4 genauso. Eine Änderung ist eine Entscheidung über den gemeinsamen Baustein und gehört in einen eigenen Schritt |
| **91** | **Der Verlauf ist ein Bild und trägt keine Tabelle daneben.** „Nie allein über Farbe" ist über Legende, Tooltip und die vier unterscheidbaren Rollen eingehalten; für ein Vorleseprogramm ist ein SVG voller `<path>` trotzdem kein Diagramm. Eine Textfassung der Zahlen wäre der nächste Schritt und ist hier nicht gebaut |

---

## 10. Die Abnahme am laufenden System *(01.09.2026)*

Gefahren im angemeldeten Browser gegen die Testkopie im Profil `dev`, Anker
`2025-12-30 04:09:47`. Der Browser lief kopflos steuerbar über das DevTools-Protokoll; die Anmeldung
hat der Auftraggeber selbst vorgenommen.

### 10.1 Was durchgelaufen ist

| Punkt | Ergebnis |
|---|---|
| **`EDITIONLINGERI` liefert den Leerzustand** | ✔ ein Satz, ein **bedienbarer** Umschalter (alle drei Schaltflächen ohne `disabled`), keine Nullkacheln, **keine Verteilungszeile**. Die Standzeile bleibt |
| **`NEXANS` ohne `zeitraum` bekommt 48 Stunden** | ✔ `data-state="on"` und `aria-pressed="true"` an „48 Stunden", **und in der URL steht nichts** |
| **Ein Klick auf die Fehlerkachel zeigt dieselbe Zahl** | ✔ für **alle drei Paare**, siehe unten |
| **Der Fehlerstreifen hat eine beschriftete Skala** | ✔ bei `NEXANS`/48 h steht dort **0 … 49** neben 9.950 Nachrichten im selben Fenster — an dieser Skala ist abzulesen, dass es um einzelne Zeilen geht |
| **Ein Aufruf** | ✔ siehe 10.3 |
| **`tests/farbwerte.test.ts` ohne neue Ausnahme** | ✔ auch mit Recharts im Baum |

### 10.2 Kachel gegen Liste — die Probe aus C.2 mitgemessen

Gezählt wurde die Liste **vollständig**, über den Cursor geblättert, nicht die erste Seite geschätzt:

| Paar | Fenster (UTC) | Spanne | Kachel *Fehler* | Liste | Kachel *Überfällig* | Liste |
|---|---|---:|---:|---:|---:|---:|
| `48H` | `2025-12-28T04:00Z` → `2025-12-30T04:00Z` | 2 Tage | **50** | **50** | **1** | **1** |
| `30T` | `2025-11-30T23:00Z` → `2025-12-30T23:00Z` | 30 Tage | **55** | **55** | **538** | **538** |
| `12M` | `2024-12-31T23:00Z` → `2025-12-31T23:00Z` | **365 Tage** | **711** | **711** *(4 Seiten)* | **538** | **538** *(3 Seiten)* |

**Damit ist C.2 nicht nur gerechnet, sondern gefahren:** Die Liste antwortet bei `12M` mit `200` und
nicht mit `zeitfenster-zu-gross`. Die Fehlerkachel klickt bei allen drei Paaren, und die Zahl stimmt
jedes Mal genau.

**Und die Überfälligkeitsform trägt** — sie ist der Grund, warum
[`nachrichtenliste.md`](nachrichtenliste.md) §5e entstanden ist. Ohne den nachgerüsteten Parameter
stünden in der rechten Spalte 200, 200 und 200 statt 1, 538 und 538.

### 10.3 Ein Aufruf — sechs Klicks, vier Anfragen

Mitgeschrieben wurden **alle** `/api`-Anfragen der Seite:

| Schritt | URL danach | Anfrage |
|---|---|---|
| Laden | *(leer)* | `GET /api/dashboard` |
| Klick „Richtung" | `?verteilung=RICHTUNG` | `…?verteilung=RICHTUNG` |
| Klick „Partner" | *(leer)* | **keine** — aus dem Zwischenspeicher |
| Klick „30 Tage" | `?zeitraum=30T` | `…?zeitraum=30T` |
| Klick „Richtung" | `?zeitraum=30T&verteilung=RICHTUNG` | `…?zeitraum=30T&verteilung=RICHTUNG` |
| Klick „48 Stunden" | `?zeitraum=48H&verteilung=RICHTUNG` | `…?zeitraum=48H&verteilung=RICHTUNG` |

**Drei Aussagen auf einmal**, und alle drei sind E‑n: Ohne Klick steht nichts in der URL. Die
**Vorgabe** `PARTNER` verschwindet wieder, die ausdrückliche Wahl `48H` bleibt stehen, obwohl der
Endpunkt sie ohnehin genommen hätte — sie ist eine Absicht und wird geteilt. Und der Rückweg auf
eine schon gesehene Kombination kostet **keine** Anfrage.

Kein einziger Aufruf ging an einen anderen Endpunkt. Die drei Anfragen des Rahmens
(`/api/auth/me`, `/api/mandanten`, `/api/bam/typen`) gehören ihm und nicht dieser Ansicht.

### 10.4 ⚠️ Zwei Befunde aus der Sichtprüfung — beide behoben

**1. Die y-Achse schnitt sechsstellige Zahlen ab.** Bei `NEXANS` über zwölf Monate stand am oberen
Rand `:20.000` statt `220.000`: Recharts beschneidet die Beschriftung an der Achsenbreite und meldet
nichts. Die Breite war eine feste Zahl (48 px, genug für vier Stellen). Sie folgt jetzt der längsten
Beschriftung, die vorkommen kann (`achsenbreite` in `verlauf.ts`), und **beide** Diagramme bekommen
dieselbe — sonst stünden ihre Balken nicht mehr übereinander.

> **Kein Test hätte das gefunden**, und das ist die eigentliche Auskunft: In den Prüfwerten stehen
> zweistellige Zahlen, und eine abgeschnittene Beschriftung ist kein Fehler, den ein Baum meldet.

**2. Die Achsendichte war eine Konstante und passte nur bei einer Breite.** `achsenabstand` rechnete
aus der Zahl der Eimer und einem Deckel von zwölf Beschriftungen einen festen `interval`-Wert. Bei
1500 px trug das; **bei 360 px überlappten die Beschriftungen um 12 Pixel** — gemessen an den Kästen
der `<text>`-Knoten.

Die Funktion ist **entfallen**. Die Dichte entscheidet jetzt `interval="equidistantPreserveStart"`
mit `minTickGap={12}` — gleichabständig und so eng, wie die vorhandene Breite es hergibt:

| Fensterbreite | beschriftete Eimer | kleinster Abstand |
|---:|---:|---:|
| 360 px | 6 | 15 px |
| 768 px | 10 | 12 px |
| 1500 px | 24 | 15 px |

**Kein waagerechtes Scrollen** an keiner der drei Breiten (`scrollWidth === innerWidth`).

Damit ist auch der Satz aus dem Auftrag eingelöst, der vorher nur behauptet war: *„Welche Dichte
tragbar ist, entscheidet der Augenschein am schmalsten unterstützten Fenster."*

### 10.5 ⚠️ Ein Befund, der **nicht** behoben ist

**Die gedrückte Schaltfläche des Umschalters ist kaum zu sehen.** Gemessen am laufenden System:

| | |
|---|---|
| Untergrund der Seite | `lab(98.26 0 0)` — `#fafafa` |
| Fläche der gedrückten Schaltfläche | `lab(95.36 0 0)` — `#f2f2f2` |
| **Kontrast** | **rund 1,07 : 1** |

Für ein Vorleseprogramm ist der Zustand sauber ausgezeichnet (`data-state="on"`,
`aria-pressed="true"`); **für das Auge ist er es kaum.**

**Nicht geändert, und zwar aus zwei Gründen.** Erstens ist das die Gestalt von
`components/ui/toggle-group.tsx` — Generatorbereich, der nicht von Hand umgebaut wird
([`visuelles-konzept.md`](visuelles-konzept.md) §2). Zweitens trägt die **Zeitfensterwahl der
Nachrichtenliste** dieselbe Gestalt seit Schritt 4: Hier eine zweite, kräftigere zu bauen hieße,
dasselbe Bedienelement an zwei Stellen verschieden aussehen zu lassen.

**Es ist eine Entscheidung über den gemeinsamen Baustein und keine über diese Ansicht** — offener
Punkt **92**.

### 10.6 Was die Abnahme über das Werkzeug gelernt hat

> **Die Fensterbreite lässt sich sehr wohl ändern** — über
> `Emulation.setDeviceMetricsOverride` im DevTools-Protokoll, nicht über
> `resize_window` der Browsererweiterung.

[`frontend-grundlagen.md`](frontend-grundlagen.md) §8 („Was der Browsertest nicht kann") hält seit
dem 10.08.2026 fest, dass Verhalten am schmalen Fenster **von Hand** zu prüfen sei, weil
`resize_window` wirkungslos bleibt. Der Satz ist über die Erweiterung richtig und über CDP nicht:
Die Messung in 10.4 ist bei 360, 768 und 1500 px gefahren worden, und die Zahlen darin stammen aus
dem Dokument selbst.

**Der Abschnitt dort ist entsprechend ergänzt**, ohne den alten Wortlaut zu streichen — er
beschreibt die Grenze des Werkzeugs, das er nennt.
