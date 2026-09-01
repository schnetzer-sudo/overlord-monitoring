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

**Bei 48 Eimern wird nicht jeder beschriftet** — `achsenabstand(eimer)` rechnet aus der Zahl der
Eimer und einer Höchstzahl von zwölf Beschriftungen den `interval`-Wert von Recharts. Bei 48 Eimern
ist das jeder vierte.

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
| **90** | **Die Achsendichte ist am breiten Fenster angesehen und nicht am schmalsten.** `achsenabstand` deckelt auf zwölf Beschriftungen; bei 1440 px trägt das sichtbar. Was bei 360 px passiert, hängt an derselben Zahl — und die ist **gewählt und nicht gemessen** |
| **91** | **Der Verlauf ist ein Bild und trägt keine Tabelle daneben.** „Nie allein über Farbe" ist über Legende, Tooltip und die vier unterscheidbaren Rollen eingehalten; für ein Vorleseprogramm ist ein SVG voller `<path>` trotzdem kein Diagramm. Eine Textfassung der Zahlen wäre der nächste Schritt und ist hier nicht gebaut |
