# Dashboard — die Oberfläche

Stand: **04.09.2026 — der Verlauf ist eine Fläche (E‑83 bis E‑86, §5.2)** und die beiden
Sammelrollen heißen anders (E‑82, §5.2) · zuvor 03.09.2026, Schritt 10b‑5 · zuvor 01.09.2026,
Schritt 10b‑3b · **Frontend**

Die Landingpage. Der Endpunkt und seine Begründung stehen in
[`dashboard.md`](dashboard.md) — **die Datei ist der Vertrag**, hier steht, was die Oberfläche
daraus macht und warum sie es so macht.

**Keine Backend-Änderung, kein neuer Endpunkt, keine Migration.** Höchste Migrationsversion bleibt
`V12`.

> ### ⚠️ Schritt 10b‑5 ist eine Reparatur, keine Ergänzung *(03.09.2026)*
>
> Die Oberfläche aus 10b‑3b liest `kacheln.ueberfaellig` und bekommt das Feld seit 10b‑4 nicht mehr:
> Die Problemkategorie *Überfällig* ist durch eine fachliche Auskunft des Auftraggebers widerlegt
> (**E‑71**, [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §4.2). **Die Landingpage war im
> Browser defekt, der Endpunkt nicht** — bewusst in Kauf genommen und als offener Punkt **131**
> geführt. Dieser Schritt schließt ihn.
>
> **Was sich ändert:** §5.4 (drei Kacheln → vier, samt Verlinkung und Fläche), §5.6 (die
> Kategoriekennzeichnung je Zeile fällt), §6 (eine bekannte Grenze fällt, eine kommt) und die
> Entscheidungen **E‑78** bis **E‑81** in §4. **Alter Wortlaut bleibt überall lesbar**; ersetzte
> Abschnitte stehen als Korrekturblock am Ende ihres Kapitels.

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

> ### ⚠️ Korrektur vom 03.09.2026 — E‑u hatte zwei Fälle, jetzt hat es einen (**E‑79**)
>
> **Der Wortlaut oben bleibt stehen; er war für zwei Problemkacheln richtig.** Seit E‑71 gibt es
> eine: *Fehler*. Die Überfällig-Kachel und die Überfällig-Plakette, um derentwillen die Probe
> gefahren wurde, gibt es nicht mehr.
>
> | | |
> |---|---|
> | **Was von E‑u gilt** | *Fehler* trägt weiterhin **Fläche und Vordergrund, keine Kontur** — Kachel wie Plakette. `statusKlassenOhneKontur` und `problemKlassenOhneKontur` stehen unverändert in `lib/status-farbe.ts` |
> | **Was E‑79 hinzufügt** | *Läuft*, *Wartend* und *Nachrichten* tragen **keine Fläche**. „Gefüllt heißt, hier ist etwas zu tun" wird dadurch eindeutig, weil es nur noch einen Fall gibt |
> | **Was die Zustandskacheln stattdessen tragen** | `--status-offen` in der **Statusplakette**, mit allen drei Werten wie in der Liste. Zwei Kacheln **derselben** Rolle können keine Rangfolge bilden — der Fall, den die Probe gefunden hat, entsteht hier nicht |
> | **Was die Probe wert bleibt** | alles. Sie hat gemessen, dass **0,025** Unterschied als Rangfolge gelesen werden, und genau diese Zahl trägt E‑79: Sie ist der Grund, die Fläche **nicht** auf vier Kacheln auszudehnen |
>
> **`problemKlassenOhneKontur` hat damit keinen Verbraucher mehr** und bleibt — dieselbe Begründung
> wie für die Farbrolle selbst ([`visuelles-konzept.md`](visuelles-konzept.md) §7a, **E‑77**): Die
> Zuordnung Rolle → Token gehört zur Rolle und nicht zu ihrem Verbraucher.

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
| **E‑m** | Klickbar sind **Fehler** und **Überfällig im Fenster**. *Überfällig insgesamt* und *Nachrichten* tragen keinen Verweis — *gelöst statt vermieden durch **E‑80**, 03.09.2026* | 01.09.2026 |
| **E‑n** | In der URL steht **nur die ausdrückliche Wahl**. Der vom Endpunkt gewählte Zeitraum wird nie zurückgeschrieben | 01.09.2026 |
| **E‑o** | Zeitpunkte **absolut** in der Anzeigezone, nie relativ | 01.09.2026 |
| **E‑p** | Der Leerzustand zeigt **einen Satz und den bedienbaren Umschalter**, sonst nichts | 01.09.2026 |
| **E‑q** | `ermittelbar: false` wird zu gedämpftem Text mit Zeichen — **keine `0`**, kein Rot, kein Fehlerzustand | 01.09.2026 |
| **E‑r** | Das Dashboard liegt auf **`/`** | 01.09.2026 |
| **E‑t** | Ein **zweiter, schmaler Balkenstreifen** unter dem Verlauf, nur `FEHLER`, mit **eigener beschrifteter Skala** | 01.09.2026 |
| **E‑u** | Die beiden Problemkategorien tragen **Fläche und Vordergrund, keine Kontur** — Kachel wie Plakette (§3). *Geschärft durch **E‑79**, 03.09.2026: Es ist nur noch eine* | 01.09.2026 |
| **E‑v** | Die Balkenbreite ist **gedeckelt** (`maxBarSize={28}`), an beiden Diagrammen mit demselben Wert. Wo die Slotbreite darunter liegt, bewirkt der Deckel nichts (§5.2) | 01.09.2026 |
| **E‑78** | Reihenfolge **Fehler · Läuft · Wartend · Nachrichten** — erst was zu tun ist, dann was in Arbeit ist, dann die Zählung. *Wartend* steht **vor** *Nachrichten*, damit sein Wegfall die Reihe von hinten zusammenzieht (§5.4) | 03.09.2026 |
| **E‑79** | **Fläche nur bei der einen Problemkachel.** Die Zustandskacheln tragen `--status-offen` im kleinen Träger — der Statusplakette der Liste. **Schärft E‑u** (§5.4) | 03.09.2026 |
| **E‑80** | ***Läuft* erbt den Zeitraum, *Wartend* bringt seinen mit**, aus `aeltesteSekunden`; über einem Jahr greift die Notbremse. **Löst, was E‑m nur vermied** (§5.4) | 03.09.2026 |
| **E‑81** | **Drei Zustände, drei Bilder.** Abwesenheit und „nicht ermittelbar" dürfen nie gleich aussehen (§5.4) | 03.09.2026 |
| **E‑82** | **Eine Farbrolle, die mehrere Einordnungen bündelt, trägt eine Beschriftung, die für alle ihre Mitglieder gilt und keines von ihnen wiederholt.** Aus *Offen* wird **„Ohne Ergebnis"**, aus *Abgeschlossen* wird **„Erledigt"** — die Token bleiben `--status-offen` und `--status-abgeschlossen` (§5.2) | 04.09.2026 |
| **E‑83** | **Der Verlauf ist eine Fläche mit der Gesamtsumme je Eimer, keine vier gestapelten Reihen.** Die Legende entfällt; die Aufteilung steht nur noch im Tooltip. **Hebt die Bildhälfte von E‑l auf** (§5.2) | 04.09.2026 |
| **E‑84** | **Die Fläche trägt `--akzent`, ihre Oberkante `--akzent-schrift`** — eine Summe hat keinen Status, und der Akzent ist die eine Farbe, die über die Daten nichts behauptet. Keine neue Farbe, keine Zeile in `globals.css` (§5.2) | 04.09.2026 |
| **E‑85** | **Bewegung nur beim Aufbau** von Verlauf und Fehlerstreifen, im Gleichlauf, 600 ms — sonst keine. Mit `prefers-reduced-motion: reduce` gar keine. **Schränkt [`visuelles-konzept.md`](visuelles-konzept.md) §7 ein, streicht ihn nicht** (§5.2) | 04.09.2026 |
| **E‑86** | **Der Zeitraumwechsel ist ein Neuaufbau und kein Morphing** — ein `key` am Container trägt das Zeitraumpaar. Sonst interpolierte Recharts zwischen Pfaden mit verschieden vielen Stützstellen (§5.2) | 04.09.2026 |
| **E‑87** | **Die Fläche trägt eine eigene Rolle in Ton 230** (`--verlauf-flaeche` / `--verlauf-kontur`) und nicht mehr den Akzent. **Widerruft E‑84 zur Hälfte:** Eine Summe hat weiterhin keinen Status — aber der Akzent sagt sehr wohl etwas, nämlich über die **Anwendung**, und als Fläche erreichte er auf `--card` nur 1,98 : 1 (§5.2) | 04.09.2026 |
| **E‑88** | **Die Fläche der Fehlerkachel wird gedämpft** — weniger Chroma, eine Spur kühler. **Nur die Fläche:** Zahl, Text, Balken, Plaketten und Fehlerart behalten `--status-fehler` Ziffer für Ziffer. Schließt nebenbei offenen Punkt 123 (§5.2, [`visuelles-konzept.md`](visuelles-konzept.md) §7a) | 04.09.2026 |
| **E‑89** | **Die Deckung der beiden Farbverlaufsstopps steht als Token und ist je Block verschieden** (hell 0,35 / 0,03, dunkel 0,28 / 0,04). `fillOpacity` bleibt `1` — die Durchsicht gehört in die Stopps, wo sie ausdrücklich dasteht (§5.2) | 04.09.2026 |
| **E‑90** | **„Zuletzt aufgefallen" trägt eine Zeile je Prozess** mit Anzahl und jüngstem Zeitpunkt, nicht mehr eine je Nachricht. Der Verweis führt in die **Liste**, gefiltert auf diesen Prozess (§5.6) | 04.09.2026 |
| **E‑91** | **Jede Zeile in „Zuletzt aufgefallen" trägt das Zeichen ihrer Kategorie**, das Wort nur im `title` und für Vorleser. **Nimmt die Hälfte von 10b‑5 zurück, die zu viel war:** Mit der Plakette ist auch die Auskunft *dass es Fehler sind* aus dem Bild verschwunden — *aufgefallen* ist keine Kategorie (§5.6) | 04.09.2026 |

---

## 5. Die sieben Blöcke

### 5.1 Der Zeitraumumschalter und die URL (E‑n)

Drei Schaltflächen: 48 Stunden, 30 Tage, 12 Monate. Dazu der Umschalter der Verteilung
(Partner ⇄ Richtung).

> ### Der Umschalter ist am 02.09.2026 nach `components/` gewandert
>
> **Nicht weil am Dashboard etwas falsch war, sondern weil es einen zweiten Verbraucher gibt:** Die
> Prozessansicht trägt denselben Umschalter mit denselben drei Paaren
> ([`process-view.md`](process-view.md) E‑47). Ein Feature importiert nicht aus einem
> Nachbarfeature; der gemeinsame Teil wandert nach `components/` und `lib/`
> ([`frontend-grundlagen.md`](frontend-grundlagen.md) §8).
>
> | Was | Wo es jetzt steht |
> |---|---|
> | die Komponente | `components/zeitraum-umschalter.tsx` |
> | die drei Codes samt Parser und der Regel „hervorgehoben ist, was gilt" | `lib/rollupzeitraum.ts` |
> | die Beschriftungen | `texte.zeitraum` statt `texte.dashboard.zeitraum` |
>
> **`Dashboardzeitraum` heißt seither `Rollupzeitraum`** — dem Backend nach, das dieselbe Bewegung am
> selben Tag gemacht hat (E‑44 dort). Eine Hülle unter dem alten Namen ist bewusst nicht
> stehengeblieben: Zwei Namen für dieselbe Menge sind der Anfang zweier Mengen.
>
> **Am Dashboard ändert sich nichts, was ein Nutzer sähe.** `hervorgehobenerZeitraum` gibt es
> weiterhin und mit derselben Bedeutung; es ruft nur die Regel, die jetzt in `lib` steht.

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

### 5.2 Der Verlauf: vier Reihen, nicht acht (E‑l) — **seit dem 04.09.2026 eine Fläche (E‑83)**

> ### ⚠️ Umbau vom 04.09.2026 — der Verlauf ist **eine Fläche** (**E‑83** bis **E‑86**)
>
> **E‑l fällt zur Hälfte.** Die Zusammenfassung acht Einordnungen → vier Farbrollen bleibt und ist
> unverändert begründet; sie steht ab heute aber **nur noch im Tooltip** und nicht mehr im Bild.
> An die Stelle der vier gestapelten Reihen tritt **eine Fläche mit der Gesamtsumme je Eimer**.
>
> **Die Begründung ist die des Auftraggebers und keine gemessene:** *Die Lesbarkeit der
> Gesamtmenge wiegt schwerer als die Eimertreue der Darstellung.* Wer den Verlauf ansieht, fragt
> zuerst **wie viel** — und ein Stapel aus vier Farben beantwortet das schlechter als eine Linie.
> Der Preis steht daneben und wird nicht kleingeredet: Die Aufteilung ist im Bild nicht mehr zu
> sehen, sondern erst beim Überfahren.
>
> | | vorher | jetzt |
> |---|---|---|
> | Der Verlauf | vier gestapelte Balkenreihen | **eine Fläche**, Farbverlauf nach unten, weiche Kurve |
> | Die Reihe | je Farbrolle eine | **die Gesamtsumme**, `dataKey="gesamt"` |
> | Die Legende | vier Einträge (E‑l) | **entfällt** — bei einer Reihe verspricht sie nichts mehr |
> | Der Tooltip | Rollen mit ihren Einordnungen | **unverändert**, Zeile für Zeile, samt Farbquadraten |
> | Der Fehlerstreifen | Balken, eigene Skala (E‑t) | **unverändert** |
> | Bewegung | keine | **nur beim Aufbau** (E‑85) |
>
> **Vorbild ist das interaktive Flächendiagramm von shadcn/ui** (`ui.shadcn.com/charts/area`).
> Übernommen sind Farbverlauf, weiche Kurve und Aufbau; **nicht** übernommen ist `ChartContainer` —
> die Ansicht färbt seit dem 31.08.2026 über `var(--token)` in einem Prop und hat einen eigenen
> Tooltip, und beides ist gemessen (§8a, §8b in
> [`frontend-grundlagen.md`](frontend-grundlagen.md)).
>
> #### E‑84 — die Fläche trägt den **Akzent**, keine Statusrolle
>
> Eine **Summe hat keinen Status**. `--status-abgeschlossen` behauptete über ihr „alles fertig",
> `--status-offen` das Gegenteil; beides wäre eine Aussage, die die Zahl nicht trägt.
> [`visuelles-konzept.md`](visuelles-konzept.md) §3 beschreibt den Akzent als die eine Farbe, die
> *„nichts sagt, was die Anwendung über die Daten sagt"* — genau das wird hier gebraucht.
>
> **Zwei vorhandene Stufen, beide in beiden Blöcken, beide so vergeben, wie §3 sie vergibt:**
>
> | | Token | warum diese Stufe | gemessen |
> |---|---|---|---|
> | die Fläche | `--akzent` | §3: *„nur Fläche.* Gefüllte Schaltfläche, Kennzeichnung" — und der Farbverlauf ist eine Fläche | in beiden Blöcken **derselbe Wert**, `#b9c022` |
> | die Oberkante | `--akzent-schrift` | §3: *„Verweise, aktive Beschriftungen, **dünne Linien**"* | 5,40 : 1 hell, 10,72 : 1 dunkel auf `--card` ([`dunkelmodus.md`](dunkelmodus.md) §3.2/§3.3) |
>
> **Die Kontur ist keine Zutat, sondern die Behebung, die §3 selbst vorschreibt.** Dort steht als
> *einzige bekannte Grenze* der Farbe: Eine gefüllte Akzentfläche erreicht auf Weiß nur 1,98 : 1 und
> verfehlt die 3 : 1 aus WCAG 1.4.11 — *„wer die Lücke schließen will, gibt gefüllten Flächen
> zusätzlich eine Kontur in `--akzent-schrift`."* Die Datenkante des Diagramms ist genau so eine
> dünne Linie, und sie trägt damit 5,40 : 1 statt 1,98 : 1.
>
> **Verworfen sind:** `--akzent-flaeche` (die blasse Tönung; auf `--card` praktisch unsichtbar) und
> `--akzent-vordergrund` (im Dunkelblock **dunkler als die Karte** — eine Fläche, die als Loch
> gelesen würde). **Keine neue Farbe, keine OKLab-Rechnung, keine Zeile in `globals.css`.**
>
> #### E‑85 — Bewegung, eng gefasst
>
> [`visuelles-konzept.md`](visuelles-konzept.md) §7 sagt *„keine Animationen"*. Der Satz wird
> **eingeschränkt und nicht gestrichen**: Erlaubt ist Bewegung **beim Aufbau** des Verlaufs und des
> Fehlerstreifens, sonst nichts. Die Zitate in [`nachrichtendetail.md`](nachrichtendetail.md)
> bleiben gültig.
>
> **Beide Diagramme bauen im Gleichlauf auf**, 600 ms, Beginn 0. Das ist kein Selbstläufer:
> Recharts' Voreinstellungen sind **verschieden** — `<Area>` 1500 ms, `<Bar>` 400 ms. Ohne die
> gemeinsame Konstante liefe der Streifen fertig, während die Fläche noch wächst.
>
> **Der Ausschalter musste nicht gebaut werden — er war schon da, und das ist nachgesehen.**
> Recharts 3.10.1 setzt `isAnimationActive` standardmäßig auf `'auto'`, und `'auto'` heißt dort:
> kein Aufbau bei `prefers-reduced-motion: reduce` und keiner beim Serverrendern
> (`util/usePrefersReducedMotion.js`). Ein eigener Schalter wäre ein zweiter Weg zu derselben
> Entscheidung gewesen.
>
> #### E‑86 — Zeitraumwechsel ist Neuaufbau
>
> Die Zahl der Eimer wechselt mit dem Zeitraumpaar. Ohne Eingriff interpolierte Recharts zwischen
> zwei Pfaden mit **unterschiedlich vielen** Stützstellen — für den Bruchteil einer Sekunde stünde
> eine Kurve da, die es in keinem der beiden Zeiträume gibt. Umgesetzt über einen `key` am
> Container, der das Zeitraumpaar trägt; er steht an **beiden** Containern, sonst liefen die
> Aufbauten auseinander.
>
> #### Was am laufenden System gemessen worden ist *(NEXANS, Profil `dev`, Anker `2025-12-30 04:09:47`)*
>
> **Neun Lagen** — drei Zeiträume × drei Breiten (die Karte auf 1673 / 768 / 360 px gesetzt):
>
> | Zeitraum | Breite | Zeichenbereich | Bandbreite | Eimer | Balkenbreite | Zeitmarke ↔ Bandmitte | Zeitmarke ↔ Balkenmitte | Vorhersage |
> |---|---:|---|---:|---:|---:|---:|---:|---:|
> | 48 Stunden | 1673 | 48…1639 | 34,587 | 46 | 28 | **0,0000** | 0,2065 | 0,2065 |
> | 48 Stunden | 768 | 48…734 | 14,913 | 46 | 13 | **0,0000** | 0,0435 | 0,0435 |
> | 48 Stunden | 360 | 48…326 | 6,0435 | 46 | 4 | **0,0000** | 0,0218 | 0,0217 |
> | 30 Tage | 1673 | 54…1639 | 52,8333 | 30 | 28 | **0,0000** | 0,0834 | 0,0833 |
> | 30 Tage | 768 | 54…734 | 22,6667 | 30 | 21 | **0,0000** | 0,1667 | 0,1667 |
> | 30 Tage | 360 | 54…326 | 9,0667 | 30 | 7 | **0,0000** | 0,0334 | 0,0333 |
> | 12 Monate | 1673 | 60…1639 | 131,5833 | 12 | 28 | **0,0000** | 0,2084 | 0,2083 |
> | 12 Monate | 768 | 60…734 | 56,1667 | 12 | 28 | **0,0000** | 0,0834 | 0,0833 |
> | 12 Monate | 360 | 60…326 | 22,1667 | 12 | 20 | **0,0000** | 0,0834 | 0,0833 |
>
> **Der Zeichenbereich ist in allen neun Lagen zeichengleich** — die gerechnete Achsenbreite trägt
> unverändert, und sie wächst mit den Zahlen mit (48 → 54 → 60).
>
> **Die Fläche liegt auf der Zeitachse**, auf **0,0005 px** genau über 23 Marken nachgezählt. **Der
> Balken liegt bis zu 0,21 px daneben** — das ist Recharts' `Math.round` auf die Balkenbreite,
> nicht eine Folge dieses Umbaus; Herleitung, Formel und die Schranke von 0,25 px in
> [`frontend-grundlagen.md`](frontend-grundlagen.md) §8b. **Die alte Zusage „Versatz 0,0 px" gilt
> damit nicht mehr wörtlich**, und sie ist hier nicht stillschweigend ersetzt worden.
>
> **Der Aufbau, Bild für Bild gemessen** (Wechsel 48 Stunden → 12 Monate, `requestAnimationFrame`):
> Beide Diagramme beginnen im **selben Bild** (186 ms nach dem Klick, davor läuft die Abfrage) und
> enden im **selben Bild** (zwischen 746 und 773 ms). Der Fortschritt — Breite des Aufbau-Clips der
> Fläche gegen die Summe der Balkenhöhen, je auf ihren Endwert bezogen — stimmt in **jedem
> abgetasteten Bild auf drei Nachkommastellen** überein: 0,028 · 0,079 · 0,153 · 0,347 · 0,834 ·
> 0,998.
>
> **Mit `prefers-reduced-motion: reduce` steht das Bild sofort:** **0** Bilder mit Aufbau-Clip und
> **genau eine** Balkenhöhensumme über den ganzen Beobachtungszeitraum, gegen **41** Bilder und
> **36** verschiedene Summen ohne die Vorgabe. Gemessen auf einer temporären Route außerhalb
> `(app)`, die die **echte** Komponente rendert, weil die Medienabfrage nur über CDP zu stellen ist.
>
> **Der Zeitraumwechsel baut neu auf:** Über den ganzen Wechsel standen im DOM nur **zwei**
> Eimerzahlen — 46 und 12 —, nie eine dritte; der markierte Knoten der alten Fläche war nach 66 ms
> aus dem Dokument verschwunden.
>
> **Nicht angefasst:** `app/globals.css` (**keine Zeile** — die Akzentstufen werden *benutzt*, nicht
> angelegt), `lib/status-farbe.ts`, `tests/farbwerte.test.ts`, die Tooltip-Gruppierung und ihre
> Beschriftungen aus E‑82, der Fehlerstreifen als Diagrammform, die gerechnete Achsenbreite,
> `MAX_BALKENBREITE` (der Wert; sein Kommentar sagt jetzt, dass ihn nur noch ein Diagramm trägt),
> Backend, Endpunkt und Antwortrumpf.
>
> **Eine Zeichenkette ist neu in Gebrauch, aber nicht neu:** Mit der Legende ist das letzte Wort
> gefallen, das den Verlauf beschriftete. Neben der Überschrift steht deshalb `achseAnzahl`
> („Nachrichten" / „Messages") — seit dem 01.09.2026 in beiden Sprachdateien, bis heute ungenutzt.
>
> > **Belegvermerk (L10).** *Gemessen war:* neun Lagen Geometrie am laufenden System (Zeichenbereich
> > beider Diagramme, Bandbreite, Balkenbreite, Lage der Zeitmarken, Lage der Balkenmitten), der
> > Aufbau beider Diagramme Bild für Bild, das Verhalten unter `prefers-reduced-motion: reduce`, und
> > die Zahl der im DOM beobachteten Eimerzahlen während eines Zeitraumwechsels. *Behauptet wird:*
> > Beide Diagramme teilen den Zeichenbereich zeichengleich, die Fläche liegt auf der Zeitachse, die
> > Balkenmitte bis zu 0,21 px daneben, beide bauen im Gleichlauf auf, und mit der Vorgabe *reduce*
> > bewegt sich nichts. — **Nicht gemessen, sondern Augenschein** ist, dass 600 ms die richtige
> > Dauer sind und dass eine Fläche die Gesamtmenge besser lesbar macht als ein Stapel: Beides sind
> > Aussagen über das Bild und keine über eine Zahl. **Nicht gemessen** ist außerdem der Zustand
> > *vor* dem Umbau — die 0,21 px sind über den unveränderten Quelltext hergeleitet und nicht am
> > alten Stand nachgestellt.

> ### ⚠️ Nachbesserung vom 04.09.2026 — die Farbe der Fläche (**E‑87** bis **E‑89**)
>
> **E‑84 fällt zur Hälfte, und die Hälfte, die bleibt, ist die tragende.** *Eine Summe hat keinen
> Status* gilt unverändert; keine der vier Statusrollen darf über der Fläche stehen. **Gefallen ist
> die Wahl des Akzents.**
>
> | | vorher (E‑84) | jetzt (E‑87) |
> |---|---|---|
> | die Fläche | `--akzent` · #b9c022 | **`--verlauf-flaeche`** · `oklch(0.62 0.118 230)` · **#1992bf** |
> | die Kontur | `--akzent-schrift` | **`--verlauf-kontur`** · #0e5d7b hell / #64c4f0 dunkel |
> | die Stopps | `1` → `0.08`, in der Ansicht | **Token, je Block verschieden** (E‑89) |
> | `globals.css` | *„keine Zeile"* | **sechs neue Token**, in beiden Blöcken |
>
> **Der Grund in einem Satz:** Der Akzent sagt nichts über die *Daten* — aber sehr wohl etwas über
> die **Anwendung**, und die größte Fläche der Seite trug damit dieselbe Farbe wie jede
> Schaltfläche, jeder Fokusring und die aktive Navigationszeile. Dazu die Messung: `--akzent`
> erreicht auf `--card` **1,98 : 1**, was §3 selbst *„die einzige bekannte Grenze"* der Farbe nennt.
>
> **Ton 230 behauptet nichts, weil ihn nichts belegt** — Rot 27, Überfällig 80, Akzent 112,4,
> Grün 166. Die vollständige Rechnung mit allen sechs Abständen, beide Blöcke getrennt, steht in
> [`visuelles-konzept.md`](visuelles-konzept.md) §3.
>
> #### E‑89 — die Deckung ist die Stellschraube, nicht der Wert
>
> Die beiden Stopps stehen als **Token** und nicht als Zahl in der Ansicht: Dieselbe Deckung trägt
> auf Weiß weniger auf als auf `--card` 0.21, und eine Zahl in der Komponente wäre eine Zahl für
> zwei Fälle. **`fillOpacity` bleibt `1`** — eine zweite Deckung darüber multiplizierte sich mit
> dieser und wäre in keiner der beiden Zahlen mehr abzulesen; genau dieser Fehler ist am selben Tag
> gemessen worden, als Recharts' Voreinstellung von 0,6 unbemerkt über der Füllung lag.
>
> Eingesetzt über `style` und **nicht** über das Attribut `stop-opacity`: Ein Präsentationsattribut
> löst `var()` nicht auf, eine Stildeklaration schon. Am laufenden System nachgesehen — das Attribut
> steht auf `null`, `getComputedStyle(stop).stopOpacity` liefert `0.28` bzw. `0.04`.
>
> #### E‑88 — die Fehlerkachel gibt nach, das Rot nicht
>
> **Wo zwei Flächen reiben, weicht nach [`visuelles-konzept.md`](visuelles-konzept.md) §3 die
> *ohne* Bedeutung.** Gewichen ist zuerst die Verlaufsfläche — über die Deckung. Die Kachelfläche
> gibt zusätzlich nach, **weil sie es kann, ohne etwas zu verlieren**: Sie ist eine Tönung, und die
> Aussage trägt die Zahl darauf.
>
> `--status-fehler-flaeche` geht von `oklch(0.96 0.028 27)` auf **`oklch(0.96 0.016 22)`** (dunkel
> 0.05 → 0.035). **Zahl, Text, Balken, Plaketten und Fehlerart behalten `--status-fehler`
> unverändert.** Der Kontrast der Zahl auf der Kachel *steigt* dabei von 5,30 auf **5,38 : 1**.
>
> ⚠️ **Der Wert färbt mehr als die Kachel** — jede Fehlerplakette in Liste, Detail, Kette und
> Prozessansicht. Eine kachel-eigene Farbe hätte die Zuordnung aus `lib/status-farbe.ts`
> herausgelöst, und §2 lässt sie nur an einer Stelle stehen. **Nachgesehen an einer Liste mit
> fünfzig Plaketten, hell und dunkel:** Die Plakette der Liste trägt `statusKlassen`, also **alle
> drei** Werte mit Kontur — anders als die Kachel nach E‑u — und liest sich weiter als gefüllt.
>
> #### Was die Sichtprobe gefunden hat, und was sie nicht sagt
>
> Gefahren als `NEXANS` am Anker `2025-12-30 04:09:47`, **beide Blöcke, alle drei Zeiträume**,
> beurteilt an **zwölf Monaten** — dort schwankt der Bestand nur zwischen 165.000 und 220.000 auf
> einer Achse ab null, und die Fläche füllt rund 80 % des Diagramms.
>
> **Das Banding ist gerastert und nicht geschätzt:** Der Farbverlauf ist mit den *aufgelösten*
> Stoppwerten über ein `data:`-SVG auf ein Canvas gelegt und Zeile für Zeile ausgelesen worden —
> 187 verschiedene Stufen im hellen und 165 im dunklen Block über 230 px, **längstes flaches Band
> je 4 px**. Auf den Aufnahmen ist kein Streifen zu sehen; **dass es auf einem anderen Schirm
> genauso ist, sagt diese Zahl nicht.**
>
> **Versatz Verlauf ↔ Streifen: 0,0000 px** — beide Zeichenbereiche 54…1639 bei dreißig Tagen, also
> zeichengleich mit dem Wert von vor der Änderung.
>
> **Nicht angefasst:** `lib/status-farbe.ts`, die vier Statusrollen, `--ueberfaellig`, der Akzent
> und seine vier Stufen, der Fehlerstreifen als Diagrammform, `MAX_BALKENBREITE`, die gerechnete
> Achsenbreite, Backend, Endpunkt und Antwortrumpf.

> ### ✔ Die Kurve verlässt das Datenintervall nicht — nachgemessen am 04.09.2026
>
> **`type="monotone"` und nicht `natural`, und das ist keine Änderung dieser Runde.** Der Auftrag
> zur Nachbesserung nannte als Befund, die Glättung überschwinge und zeichne Werte, die in den Daten
> nicht vorkommen. **Der Quelltext trug `monotone` schon beim ersten Bau von E‑83** — nachgesehen im
> Verlauf der Datei, nicht erinnert: In keinem Stand hat je `natural` dagestanden. Was gefehlt hat,
> war der **Nachweis**; er steht jetzt hier.
>
> **Beide sind weiche Kurven, und nur eine kann überschwingen.** Recharts bildet `natural` auf d3s
> natürliche kubische Spline ab; sie legt eine zweimal stetig differenzierbare Kurve durch alle
> Stützstellen und darf dafür zwischen zweien über sie hinausgehen. `monotone` bildet auf
> `curveMonotoneX` ab: An einem lokalen Hoch- oder Tiefpunkt ist die Steigung **null**, und die
> Kontrolpunkte liegen auf demselben Wert wie die Stützstelle. Zwischen zwei Stützstellen bleibt die
> Kurve damit im Intervall der beiden.
>
> #### Gemessen ist der **gezeichnete Pfad**, nicht die Absicht
>
> Aus dem `d`-Attribut von `.recharts-area-curve` am laufenden System, jede kubische Bézierkurve in
> 200 Schritten abgetastet, das Ergebnis über die y‑Achsenmarken in Nachrichten zurückgerechnet.
> `NEXANS`, Profil `dev`, Anker `2025-12-30 04:09:47`:
>
> | Zeitraum | Eimer | die Daten | der gezeichnete Pfad | Überschwingen oben / unten |
> |---|---:|---|---|---|
> | **48 Stunden** | 45 | **8 … 3.068** | **8,00 … 3.067,99** | **0,0000 px / 0,0000 px** |
> | 30 Tage | 30 | 626 … 15.254 | 625,98 … 15.254,02 | 0,0000 px / 0,0000 px |
> | 12 Monate | 12 | 174.176 … 208.766 | 174.176,36 … 208.766,36 | 0,0000 px / 0,0000 px |
>
> **Die beiden Zahlen, um die es geht, stehen in der ersten Zeile.** Der höchste Eimer trägt 3.068
> Nachrichten, der Pfad erreicht dort 3.067,99; der niedrigste trägt 8, der Pfad erreicht dort 8,00.
> Die Abweichung ist die Auflösung der Rückrechnung über die Achse und kein Überschwingen: In
> **Pixeln** — der Einheit, in der der Pfad wirklich steht — ist der Abstand zwischen dem Scheitel
> der Kurve und der höchsten Stützstelle **0,0000**.
>
> #### Die Gegenprobe: dieselbe Messung fände den schlechten Fall
>
> **Ein Nachweis, der nur in eine Richtung eicht, ist keiner.** Über *dieselben* 45 Stützstellen ist
> deshalb die natürliche Spline nachgerechnet worden — mit dem Rechenweg aus d3s
> `curve/natural.js`, den Recharts für `type="natural"` benutzt:
>
> | | oben | unten |
> |---|---|---|
> | gezeichnet (`monotone`) | 3.067,99 | **8,00** |
> | dieselben Punkte als `natural` | 3.068,74 | **−395,60** |
> | Überschwingen | 0,06 px | **30,52 px** |
>
> **`natural` zöge die Kurve 30,5 Pixel unter die Nulllinie** — gemalte **−396 Nachrichten**, und
> zwar genau an der Stelle, an der auf 3.068 der Eimer mit 8 folgt. Der Befund des Auftrags ist damit
> als *Eigenschaft der Kurvenform* bestätigt, auch wenn er auf diesen Quelltext nie zutraf.
>
> > **Belegvermerk (L10).** *Gemessen war:* das `d`-Attribut der gezeichneten Kurve in allen drei
> > Zeiträumen, dicht abgetastet, gegen die Eimersummen derselben Antwort; dazu die natürliche
> > Spline über dieselben Stützstellen. *Behauptet wird:* Die gezeichnete Kurve verlässt das
> > Intervall zwischen dem niedrigsten und dem höchsten Eimer in keinem der drei Zeiträume, und die
> > Messung wäre in der Lage gewesen, das Gegenteil zu zeigen. — **Nicht gemessen** ist ein anderer
> > Mandant als `NEXANS` und eine andere Fensterbreite als die des Messfensters; die Aussage folgt
> > aber aus der Kurvenform und nicht aus diesen Daten, und die Gegenprobe zeigt, dass die Daten
> > allein sie nicht tragen würden.

> ### ⚠️ Korrektur vom 04.09.2026 — die Beschriftung der beiden Sammelrollen war falsch (**E‑82**)
>
> **Die Gruppierung stimmt, der Name stimmte nicht.** Vier Reihen bleiben vier Reihen, die
> Reihenfolge bleibt, die Farben bleiben. Geändert sind zwei Zeichenketten je Sprachdatei.
>
> | Rolle (Token bleibt) | Beschriftung neu | bisher |
> |---|---|---|
> | `--status-offen` | **„Ohne Ergebnis"** | „Offen" |
> | `--status-abgeschlossen` | **„Erledigt"** | „Abgeschlossen" |
>
> **Der Befund, im Wortlaut.** Beobachtet in einem Eimer mit 60 Nachrichten: *Offen 49* über
> *Aufgeteilt 1* und *Zusammengeführt 48* — **48 dieser 49 Zeilen stehen in einem Endstatus.**
> `istEndstatus` liefert für `AUFGETEILT` und `ZUSAMMENGEFUEHRT` `true`
> ([`message-status.md`](message-status.md), Abschnitt „Endstatus und Überfälligkeit"); die
> Überschrift behauptete das Gegenteil. Die zweite Rolle war nicht falsch, sondern **doppelt**:
> „Abgeschlossen" stand als Überschrift über einem gleichlautenden Eintrag mit anderer Zahl.
>
> > **Nachgerechnet bei der Sichtprobe am 04.09.2026 — der Satz oben untertreibt.** Der Eimer ist
> > wiedergefunden worden (`NEXANS`, 48 Stunden, Eimer **19:00** am 29.12.2025, *Gesamt 60*), und
> > die Aufschlüsselung stimmt aufs Wort: *Ohne Ergebnis 49* über *Aufgeteilt 1* und
> > *Zusammengeführt 48*. **Es sind aber nicht 48 der 49 Zeilen im Endstatus, sondern alle 49** —
> > `AUFGETEILT` ist nach derselben Tabelle ebenso Endstatus wie `ZUSAMMENGEFUEHRT`. Der
> > ursprüngliche Wortlaut bleibt oben stehen; die Zahl macht den Befund nicht kleiner, sondern
> > größer.
>
> **Der Fehler saß im Namen, nicht in der Gruppierung.** Die Zusammenfassung *als Farbe* ist in
> [`visuelles-konzept.md`](visuelles-konzept.md) §3 begründet und bleibt: *„Kein Ergebnis, kein
> Problem. Farbe wäre hier eine Aussage, die es nicht gibt."* Das ist eine Aussage über
> **Neutralität**, nicht über Offenheit — der Rollenname stammte aus der Hälfte seiner Mitglieder,
> die passt. **„Ohne Ergebnis" ist deshalb nicht erfunden**, sondern die Formulierung, mit der das
> visuelle Konzept die Rolle schon vorher begründet hat.
>
> **Vorhergesagt war es.** [`message-status.md`](message-status.md) schreibt seit dem 06.08.2026:
> *„Wer ‚offen‘ im Sinne der Oberfläche braucht, definiert das dort — und begründet es dort."*
> Diese Definition ist nie getroffen worden; stattdessen ist der Name einer Farbrolle in die
> Beschriftungsposition gerutscht.
>
> **Verworfen wurden vier naheliegende Fassungen.** *Zwischenschritt* und *Zwischenstand* sind am
> 11.08.2026 bewusst aus jeder Sprachdatei entfernt worden
> ([`nachrichtenliste.md`](nachrichtenliste.md) §5) — die Fassung mit anderem Suffix holte denselben
> Begriff zurück. *Unterwegs*, *In Bearbeitung* und *Läuft* sind eine Aussage über **Fortschritt**,
> und ob eine gesplittete Nachricht weiterläuft oder hängt, sagt der Status nicht (Regel Q4).
> *Mit Ergebnis* als Gegenstück schäde daran, dass **ein Fehler auch ein Ergebnis ist** — der Name
> gälte für zwei Rollen. *Angekommen* und *Bestätigt* höben `ABGESCHLOSSEN` auf die Aussage von
> `QUITTIERT`, und genau dieser Unterschied ist der ganze Grund, warum es zwei Einordnungen sind.
>
> **Die Beschriftungen der Einordnungen sind unangetastet.** „Aufgeteilt", „Zusammengeführt",
> „Abgeschlossen" und „Quittiert" bleiben, wie sie seit dem 11.08.2026 getrennt geführt werden.
> Dass die Einordnung *Abgeschlossen* jetzt unter der Gruppe *Erledigt* steht, ist gewollt: **Die
> Gruppe ist die weitere Menge.**
>
> **Die Legende liest sich seither: Fehler · Ohne Ergebnis · Erledigt · Ungeklärt.** In der
> englischen Fassung *Errors · No outcome · Done · Unclear* — dort trug die Rolle mit
> „Completed" **wörtlich denselben Text** wie die Einordnung `ABGESCHLOSSEN`.
>
> **Nicht geändert:** `app/globals.css`, `lib/status-farbe.ts`, `features/dashboard/verlauf.ts`,
> die Stapelreihenfolge `fehler, offen, abgeschlossen, ungeklaert`, jeder Bezeichner im Quelltext
> und `tests/farbwerte.test.ts`. Es ist **keine** Entscheidung über Farbe, Gruppierung oder
> Diagrammform.

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

> ### ⚠️ Fortschreibung vom 04.09.2026 — der Streifen bleibt, die Null nicht
>
> **E‑t ist unberührt.** Der Fehlerstreifen bleibt ein **Balken**diagramm mit eigener Skala, zwei
> Beschriftungen und derselben Zeitachse; über ihm steht seit heute eine Fläche (**E‑83**, §5.2).
> **Die verschiedene Form ist erwünscht** — sie hält auseinander, was verschiedene Größen sind, und
> sie sagt dasselbe noch einmal, was der Satz neben der Überschrift in Worten sagt.
>
> **Was der Satz oben aussagt, gilt weiter:** Beide Diagramme bekommen **dieselbe** Achsenbreite,
> und der Zeichenbereich ist in allen neun am 04.09.2026 gemessenen Lagen zeichengleich
> (48…1639 · 54…1639 · 60…1639, je nach Zeitraum).
>
> **Was nicht mehr gilt, ist die Zahl 0,0 px aus dem Vermerk darüber.** Sie stammt aus einer Zeit,
> in der beide Diagramme Balken trugen und dieselbe Rundung bekamen. Die Fläche liegt heute auf der
> Zeitachse — auf **0,0005 px** genau nachgezählt —, der Balken darunter bis zu **0,21 px** daneben.
> Ursache ist `Math.round` auf die Balkenbreite in Recharts (`combineAllBarPositions.js`), Schranke
> 0,25 px, hergeleitet und in neun Lagen auf 0,0001 px bestätigt:
> [`frontend-grundlagen.md`](frontend-grundlagen.md) §8b. **Der alte Wortlaut bleibt stehen**, weil
> die Aussage, um derentwillen er dasteht, unverändert richtig ist.

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

### 5.4 Die vier Kacheln (E‑m, E‑q, **E‑78** bis **E‑81**)

*Neu am 03.09.2026 (Schritt 10b‑5). Bis dahin standen hier drei Kacheln, und die mittlere hieß
**Überfällig**; der alte Abschnitt steht als Korrekturblock am Ende dieses Kapitels.*

**Reihenfolge: Fehler · Läuft · Wartend · Nachrichten** (**E‑78**). Sie folgt dem Leitsatz: erst was
zu tun ist, dann was in Arbeit ist, dann die Zählung. **Und *Wartend* steht vor *Nachrichten*, nicht
dahinter** — sein Wegfall zieht die Reihe dann von hinten auf drei zusammen, statt eine Lücke in die
Mitte zu schlagen.

#### Es sind drei oder vier, und der Unterschied ist eine Auskunft (**E‑81**)

| Zustand | Antwort | Anzeige |
|---|---|---|
| **strukturell abwesend** | Schlüssel `wartend` fehlt | **keine Kachel.** Kein Platzhalter, keine gedämpfte Kachel, kein „nicht verfügbar" |
| **nicht ermittelbar** | `ermittelbar = false` | **Kachel da**, Text nach E‑q, **nicht klickbar** |
| **ermittelt** | `ermittelbar = true` | die Zahl, bei `anzahl > 0` zusätzlich das Alter |

> **Abwesenheit ist eine Auskunft über den Mandanten** („hat keine Abläufe, die suspendieren"),
> `ermittelbar = false` eine über **uns** („wissen es gerade nicht"). Verschwände die Kachel bei
> einem Fehlschlag, würde ein Ausfall stillschweigend in eine **strukturelle Behauptung** übersetzt.
> Das ist der schlimmste der drei denkbaren Fehler an dieser Stelle, und deshalb sind es drei Bilder
> und nicht zwei.

Für *Läuft* gilt dasselbe ohne den ersten Fall: **Die Kachel ist immer da**, laufen kann jeder
Mandant.

> ### ⚠️ Nachgesehen und nicht angenommen: Was, wenn die Erscheinungsbedingung selbst fällt?
>
> Genau dann entstünde der Fehler, den E‑81 ausschließt — der Schlüssel fiele weg, und die
> Oberfläche läse einen Ausfall als „dieser Mandant wartet nie".
>
> **Der Endpunkt lässt das nicht zu**, und es steht dort ausgeschrieben
> ([`dashboard.md`](dashboard.md) §5): *„Auch die Erscheinungsbedingung bekommt keinen
> [Teilerfolg-Mechanismus]. Sie liest **Stammdaten** und ist damit dieselbe Art Zugriff wie die
> Mandantenkette in jedem anderen Statement."* Bricht sie, ist die **ganze Antwort** ein Fehler und
> die Seite zeigt ihren Fehlerzustand. **Kein offener Punkt** — die Frage ist beantwortet, bevor sie
> die Oberfläche erreicht.

#### Genau eine Kachel trägt eine Fläche (**E‑79**)

**E‑u ist für zwei Problemkacheln geschrieben worden; seit E‑71 gibt es eine** — *Fehler*. *Läuft*,
*Wartend* und *Nachrichten* tragen keine Fläche.

**Das ist keine Aufweichung von E‑u, sondern seine Schärfung.** „Gefüllt heißt, hier ist etwas zu
tun" wird eindeutig, weil es nur noch einen Fall gibt. Trügen die beiden Zustandskacheln ebenfalls
Fläche, wäre die Unterscheidung wieder aufgelöst — und [`visuelles-konzept.md`](visuelles-konzept.md)
§7a hat gemessen, wie wenig dafür nötig ist: **0,025** Unterschied sind in der Sichtprobe A.2 als
Rangfolge gelesen worden.

**Damit sie nicht wie nackte Zahlen aussehen**, tragen *Läuft* und *Wartend* `--status-offen` in
einem **kleinen Träger**: der **Statusplakette**, in derselben Gestalt und über dieselbe Verwendung,
die auch die Liste nimmt (`statusKlassen` in `lib/status-farbe.ts`). **Die Kachel sieht aus wie die
Zeilen, auf die sie führt.**

| | |
|---|---|
| **Der Träger steht an der Stelle des Kopfes** | Er trägt Zeichen **und** Wort; ein Kopf darüber sagte dasselbe Wort ein zweites Mal |
| **Alle drei Werte, also mit Kontur** | anders als bei *Fehler*, wo E‑u zwei nimmt. E‑u galt zwei Kategorien, von denen keine lauter sein durfte; hier stehen zwei Kacheln **derselben Rolle** nebeneinander, und `--status-offen-kontur` ist mit 1,35 : 1 auf `--card` die zurückhaltendste der fünf |
| **Keine geteilte Komponente** | `StatusPlakette` liegt in `features/nachrichten`, und ein Feature importiert nicht aus einem Nachbarfeature ([`frontend-grundlagen.md`](frontend-grundlagen.md) §8). Geteilt ist die **Farbe** in `lib/status-farbe.ts`, nicht die Komponente — die Plakette der Liste kann Rohwert, unbestätigte Bedeutung und den aktuellen Schritt, und nichts davon gibt es an einer Kachel |

#### Was daraufsteht

- **Die Wörter sind „Läuft" und „Wartend"** — und sie stehen nicht in `dashboard.kacheln`, sondern
  in `texte.einordnung`. Es sind dieselben, die der Statusfilter und die Plakette der Liste tragen;
  ein eigenes Wort hier wäre dieselbe Sache zum zweiten Mal benannt.
- **Zweite Zeile: „ältester seit 7 Tagen".** Sie **entfällt bei `anzahl = 0`** — kein „—", kein
  „keine". Ohne Zeile gibt es kein Alter, und die Null steht für sich.
- **Beide Kacheln sagen, dass sie den Bestand zählen und nicht den Zeitraum.** Ohne diesen Satz
  widersprechen sich zwei Zahlen auf derselben Seite sichtbar, sobald 48 Stunden gewählt sind — der
  Normalfall.

> **Der Satz steht *in* der Kachel und nicht als geteilte Zeile darunter**, obwohl die Bauform
> daneben (bekannte Grenze 3, unter der Reihe) genau das täte. Der Grund ist E‑81: Eine geteilte
> Zeile müsste **beide Kacheln benennen** — und nennte damit bei einem Mandanten ohne suspendierende
> Abläufe eine Kachel, die es auf seiner Seite gar nicht gibt. Die Bauform ist dieselbe geblieben:
> ein **sichtbarer** Satz und kein `title`, weil es auf einem Berührungsgerät kein Überfahren gibt.

- **Zeitspannen formatiert der vorhandene Formatierer** — `formatiereDauer`, derselbe wie für
  `wartetSeitSekunden` im Detail. Ein zweiter wäre eine zweite Wahrheit über dieselbe Größe.

> ⚠️ **Die fünf Dauerbausteine stehen damit zum dritten Mal in den Sprachdateien** — unter
> `nachrichten.detail.dauer`, `katalog.lauf.dauer` und jetzt `dashboard.kacheln.dauer`. Der offene
> Punkt dazu steht seit dem 26.08.2026 in
> [`prozess-katalog-frontend.md`](prozess-katalog-frontend.md) §11 (Punkt 8) und betrifft jetzt drei
> Stellen statt zwei. **Geteilt ist der Formatierer, nicht der Text** — die Bausteine gehören keiner
> Ansicht und müssten auf die oberste Ebene; sie dorthin zu heben fasst die Schlüssel des
> Nachrichtendetails an und ist eine eigene Runde.

#### Die Verlinkung ist asymmetrisch (**E‑80**)

| Kachel | Ziel | Zeitraum |
|---|---|---|
| **Fehler** | `/nachrichten?status=FEHLER&von=…&bis=…` | **erbt** |
| **Läuft** | `/nachrichten?status=LAEUFT&von=…&bis=…` | **erbt** |
| **Wartend** | `/nachrichten?status=WARTEND&von=…&bis=…` | **eigener**, aus `aeltesteSekunden` |
| **Nachrichten** | — | nicht klickbar |

**Läuft darf erben.** Eine laufende Nachricht ist nach E‑71 höchstens so alt wie die Wächterfrist —
rund 30 Minuten — und liegt damit in jedem Zeitraum, den der Umschalter anbietet.

**Wartend darf nicht.** 579.934 Sekunden sind 6,71 Tage; bei 48 Stunden zeigte das Ziel einen
Bruchteil der genannten Zahl. **E‑m hat für genau diesen Fall die Klickbarkeit abgeschaltet** —
*„Überfällig insgesamt"* trug keinen Verweis, weil die Zahl kein Zeitfenster hat und die Liste eines
braucht. **Hier wird derselbe Fall gelöst statt vermieden:** Der Link bringt sein Fenster mit, und
weil das eine ausdrückliche Wahl ist, steht es nach **E‑n** in der URL.

#### Zu weit zu greifen kostet nichts, zu kurz kostet die Zeile

`aeltesteSekunden` ist ein **Alter** und kein Zeitpunkt; sein Bezug ist die Anwendungsuhr des
Backends, und die darf im Browser nicht nachgerechnet werden — im Profil `dev` steht sie Monate
zurück. Der einzige Anker in der Antwort ist `fenster.bis`, und der liegt **hinter** `jetzt`: Er ist
der Anfang des *nächsten* Eimers ([`dashboard.md`](dashboard.md) §2). `bis - aeltesteSekunden` läge
damit **nach** der ältesten Zeile und schnitte sie weg.

Deshalb lässt `verweise.ts` zweimal Luft nach hinten:

1. **eine Eimerbreite** als feste Obergrenze je Paar — 1 h bei `48H`, 1 d bei `30T`, 31 d bei `12M`.
   `bis` minus eine Eimerbreite liegt garantiert **nicht später** als `jetzt`.
2. **die Abrundung auf den Tagesanfang** — bis zu 24 Stunden mehr, und nebenbei eine Adresse, die
   man in einem geteilten Link lesen kann.

> **Das darf großzügig sein, und zwar beweisbar:** `aeltesteSekunden` gehört zur **ältesten**
> wartenden Zeile, gemessen über `MIN(MessageLastUpdate)` — dieselbe Spalte, nach der die Liste
> filtert. Ein früheres `von` kann deshalb **keine einzige Zeile hinzufügen**; es gibt keine ältere.
> Ein zu spätes ließe genau die Zeile weg, um derentwillen jemand klickt.
>
> **Die Eimerbreite steht als feste Millisekundenzahl da und wird nicht kalendarisch gerechnet.**
> Ein Kalendermonat über `Date` hinge an der Zone, in der man ihn abzieht — das Backend richtet die
> Eimer in der Anwendungszone aus, die Antwort nennt sie in UTC. Nach oben abzurunden kostet nichts,
> also steht für `12M` schlicht der längste Monat.

#### Die Notbremse

**Über der Höchstspanne der Liste — ein Jahr, Regel L1 — ist die Kachel nicht klickbar** und sagt in
einem Satz warum. Ein Link, der weniger zeigt als die Kachel nennt, entsteht nicht, auch nicht still;
der Endpunkt wiese ihn mit `zeitfenster-zu-gross` ab.

> **Eine Stunde Spielraum, und sie ist gerechnet.** Der Listen-Endpunkt misst das Jahr als
> **Kalenderjahr in der Anwendungszone** (`common/Zeitfenster.absolutes`:
> `von.isBefore(bis.minusYears(1))`), `verweise.ts` rechnet in UTC. Die beiden Ergebnisse können sich
> um den Unterschied der Zonenversätze an den beiden Enden unterscheiden — **höchstens eine Stunde**.
> Eine Stunde zu früh zu bremsen ist die richtige Richtung: Die Kachel sagt dann einen Satz, statt
> auf eine Fehlerseite zu führen.

#### Die zweite Zeile ist eine Prüfung

**E‑75 hat `aeltesteSekunden` dafür gebaut**, und das ist ihre wichtigere Aufgabe. Schritt 10b‑4 ruht
auf einer **fachlichen Auskunft und keiner Messung** (E‑71); bis die offene Prüfung gegen die
Produktion gefahren ist ([`message-status.md`](message-status.md)), ist diese Zeile der Ort, an dem
sie beobachtbar bleibt:

> **„ältester seit 40 Tagen" widerlegt die eine Woche. *Läuft* über einer halben Stunde heißt, der
> Wächter hängt.**

Das ist **keine Schwelle und keine Warnung** — die Zeile trägt eine Zahl und kein Urteil, und ob
sieben Tage viel sind, entscheidet niemand hier (Regel Q4, offener Punkt 130). Sie macht die
ungemessene Auskunft nur beobachtbar, und das ist das Höchste, was eine Anzeige ohne Schwelle für sie
tun kann.

#### Die Umbruchregel ist erweitert und nicht ersetzt

`sm:grid-cols-2` bleibt; **nur die Spaltenzahl am breiten Fenster folgt der Zahl der Kacheln** —
`xl:grid-cols-4` bei vier, `xl:grid-cols-3` bei drei. Sonst bliebe bei `SUTTONS` eine leere vierte
Spalte stehen, und eine Lücke sähe aus wie eine fehlende Zahl. Zusammengesetzte Klassennamen
entstehen dafür nicht: Tailwind sucht den Quelltext ab, und beide Formen stehen vollständig da.

#### „Nicht ermittelbar" (E‑q)

| | |
|---|---|
| Die Plakette | bleibt |
| An der Stelle der Zahl | gedämpfter Text mit Zeichen — **keine `0`**, kein Rot, keine Fehler-Kennung |
| Die Kachel | ist **nicht klickbar** |
| Ein Satz | nennt den Grund. Für den Nutzer ist das eine Auskunft, kein technischer Fehler |

**E‑q bleibt als Mechanismus und gilt jetzt für `laeuft.ermittelbar` und `wartend.ermittelbar`.**
Geändert hat sich der Umfang: **Die beiden Zahlen *einer* Kachel fallen zusammen, die beiden
*Kacheln* nicht.** Bei *Überfällig* waren „im Zeitraum" und „insgesamt" ein Paar, das man
nebeneinander liest; *Läuft* und *Wartend* sind zwei Statements und zwei Auskünfte. Fällt eine, steht
die andere ([`dashboard.md`](dashboard.md) §5).

Und es gibt hier weiterhin keine Schaltfläche „Erneut versuchen": Sie verspräche, dass ein zweiter
Versuch etwas ändern könnte, und die übrige Seite steht ja bereits.

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

---

> ## ⚠️ Der Stand bis zum 03.09.2026 — die drei Kacheln mit *Überfällig*
>
> **Er bleibt wortgleich stehen.** Ohne ihn wäre nicht mehr nachlesbar, warum E‑m die Klickbarkeit
> einmal abgeschaltet hat — und E‑80 löst genau den Fall, den E‑m vermieden hat.
>
> > ### 5.4 Die drei Kacheln (E‑m, E‑q)
> >
> > Reihenfolge **Fehler, Überfällig, Nachrichten**. Sie folgt dem Leitsatz; die Zählkachel
> > beantwortet keine Frage, mit der jemand herkommt, und steht deshalb hinten.
> >
> > | Kachel | Ziel |
> > |---|---|
> > | **Fehler** | `/nachrichten?status=FEHLER&von=…&bis=…`, Grenzen aus `fenster` |
> > | **Überfällig, im Fenster** | `/nachrichten?ueberfaellig=true&von=…&bis=…` |
> > | **Überfällig, insgesamt** | **nicht klickbar** |
> > | **Nachrichten** | nicht klickbar |
> >
> > **Warum „insgesamt" nicht klickt:** Die Zahl hat bewusst kein Zeitfenster (Regel L9), die Liste
> > hat ein Pflicht-Zeitfenster (Regel L1). Jedes Ziel zeigte eine **andere Zahl** als die Kachel —
> > und eine Kachel, die auf eine andere Zahl führt als sie nennt, ist schlechter als eine, die
> > nicht klickt. Ein Satz sagt das an der Kachel.
> >
> > **Die beiden Filter werden nie kombiniert.** `status=FEHLER` und `ueberfaellig=true` sind am
> > Listen-Endpunkt ausdrücklich unvereinbar und ergeben `400`. Die Adressen entstehen deshalb in
> > **zwei getrennten Funktionen**, und keine von beiden nimmt den anderen Parameter entgegen.
> >
> > **Verlinkt ist ein Bereich der Kachel und nicht die ganze.** In der Fehlerkachel steht darunter
> > die Schaltfläche für die Aufschlüsselung, und ein `<button>` in einem `<a>` ist kein gültiges
> > Markup.
> >
> > > ### ⚠️ Der Verweis auf „überfällig" hat die Liste eine Änderung gekostet
> > >
> > > Das Backend kennt den Parameter seit Schritt 4 — **die Oberfläche kannte ihn nicht.** Ein
> > > Klick wäre auf der *ungefilterten* Liste gelandet, mit dem Zeitfenster der Kachel und ohne
> > > jeden Hinweis. Der Befund samt Bau steht in [`nachrichtenliste.md`](nachrichtenliste.md) §5e;
> > > hier steht nur, warum er hier auffiel: **Vor dem Dashboard gab es keinen Weg zu diesem
> > > Parameter.**
>
> **Was daran gefallen ist:** die Kachel *Überfällig* samt beider Zahlen, ihr Verweis und der Satz
> daneben. **Was unberührt bleibt, und es ist der größere Teil:** die Bauform — Kachel mit Kopf,
> Zahl, Verweis auf einen Bereich statt auf die ganze Kachel, E‑q als eigener Zweig. **Die beiden
> neuen Kacheln erben sie vollständig.**

---

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

> ### ⚠️ Umbau vom 04.09.2026 — **eine Zeile je Prozess** (**E‑90**)
>
> **Der Befund:** Der Block zeigte zehn Zeilen mit demselben Zeitstempel und demselben Ablauf. Er
> listete **Nachrichten**, wo er **Prozesse** listen sollte — und bei `NEXANS` über 48 Stunden
> stammen 49 der 50 Fehler aus *einem* Prozess. Ein Prozess füllte die Liste allein, und keine
> Zeile trug eine eigene Auskunft.
>
> | | vorher | jetzt |
> |---|---|---|
> | eine Zeile ist | eine **Nachricht** | ein **Prozess** |
> | sie trägt | Zeitpunkt, Prozess bzw. `sosName` | Zeitpunkt (**jüngster**), Prozessname, **Anzahl** |
> | der Name kommt aus | `SOS.SOSName` | **`Process.ProcessName`** |
> | der Verweis führt | ins Detail, `/nachrichten/<id>` | in die **Liste**, gefiltert auf diesen Prozess |
> | sortiert nach | Zeitpunkt | **jüngstem** Zeitpunkt je Prozess |
> | höchstens | zehn | **zehn** (unverändert) |
>
> **Der Name wechselt, weil die Zeile wechselt.** Process zu SOS ist meist 1:1, gelegentlich 1:n —
> eine Gruppe je Prozess kann *mehrere* Ablaufnamen enthalten, und einen davon zu wählen hieße raten
> (Regel Q4). `ProcessName` gehört dem Prozess allein und ist derselbe Anzeigename, den
> Prozesskatalog und Prozessansicht tragen. Fehlt er, steht die Kennung da — **kein Ersatztext**.
>
> **Sichtbar steht an der Zeile nur die Ziffer.** Der Blockkopf sagt bereits, worum es geht; das
> Wort an jeder Zeile sagte es zehnmal. Für ein Vorleseprogramm ist die nackte Zahl aber keine
> Auskunft — dort steht der ganze Satz im `aria-label`, in Einzahl und Mehrzahl getrennt, weil die
> englische Fassung sie unterscheidet.
>
> #### Der Verweis führt in die Liste, und das ist der Ersatz für das Verlorene
>
> Die Zeile trägt einen Prozess mit *n* Nachrichten; eine davon herauszugreifen wäre eine
> Behauptung, die sie nicht macht. Das Ziel ist `/nachrichten?status=FEHLER&prozess=…` mit dem
> Fenster der Antwort — **genau die Menge, die die Zahl daneben nennt**. Am laufenden System
> nachgesehen: Der Klick auf *49* zeigt **49 Zeilen**, und der Rohstatus `ERROR_TIMEOUT` steht dort
> in jeder.
>
> #### ✔ Offener Punkt 136 ist damit **gegenstandslos**, nicht erledigt
>
> Er verlangte den Rohstatus je Zeile zurück, der mit der Plakette gefallen war. **Eine Zeile, die
> einen ganzen Prozess zusammenfasst, hat keinen Rohstatus** — sie kann zwanzig verschiedene
> enthalten. Der Auftrag hat es vorhergesagt: *„erledigt sich mit — durch weniger Zeilen statt mehr
> Angaben je Zeile."*
>
> #### ⚠️ Es sind selten zehn, und das ist die Auskunft und kein Mangel
>
> Über den **gesamten** Bestand der Testkopie hat `NEXANS` Fehler in **drei** Prozessen, `SUTTONS`
> in **einem**, `VOTG` in **einem** (M146). Der Block zeigt damit **null bis drei** Zeilen statt
> zehn. **Der Deckel von zehn bleibt** — er greift auf diesen Daten nur nicht, und ob er es auf der
> Produktion tut, ist **nicht gemessen**.
>
> **Was diese Zahl sagt, ist mehr als das, was die zehn gleichen Zeilen davor sagten:** Es ist immer
> derselbe Prozess.
>
> #### ⚠️ Nachtrag desselben Tages — das **Zeichen** kehrt zurück (**E‑91**)
> 
> **10b‑5 hat mit der Plakette mehr mitgenommen als das Wort.** Die Begründung von damals
> ist richtig geblieben: *Eine Plakette, die an jeder Zeile dasselbe Wort sagt, unterscheidet
> nichts mehr.* Sie hat aber auch die Auskunft **dass es Fehler sind** aus dem Bild genommen —
> und die stand danach **nirgends** mehr: Die Überschrift lautet „Zuletzt aufgefallen", und
> *aufgefallen* ist keine Kategorie.
> 
> | | 10b‑5 | **seit E‑91** |
> |---|---|---|
> | Zeichen je Zeile | — | **Warndreieck in `--status-fehler`** |
> | Wort je Zeile | — | nur `title` und `sr-only` |
> | Plakette (Fläche, Kontur) | — | **weiterhin keine** |
> 
> **Das ist keine Rücknahme von 10b‑5, sondern seine Hälfte.** §3 verlangt *„zusätzlich
> eine Beschriftung **oder** ein Zeichen"* — die Zeichen unterscheiden sich in ihrer **Form**,
> nicht in ihrer Farbe. Ein Zeichen kostet die Zeile nichts an Breite; ein Wort kostete sie und
> sagte an jeder Zeile dasselbe.
> 
> **Nicht die `StatusPlakette`.** Die liegt in `features/nachrichten`, und ein Feature importiert
> nicht aus einem Nachbarfeature ([`frontend-grundlagen.md`](frontend-grundlagen.md) §8) —
> dieselbe Abgrenzung, aus der die Kacheln ihr Zeichen selbst setzen. **Geteilt ist die Farbe**
> über `statusVordergrund` in `lib/status-farbe.ts`; die Datei ist dafür **nicht** geändert
> worden, die Funktion gab es bereits.
> 
> **Zwei Verzeichnisse statt zweier fester Werte**, beide über `Auffaelligkeit` geschlüsselt:
> eines für das Zeichen, eines für die Statusart, aus der die Farbe kommt. Heute steht in beiden
> ein Eintrag. **Der Umweg über die zweite Aufzählung ist Absicht:** `zeile.kategorie` ist eine
> `Auffaelligkeit`, `statusVordergrund` erwartet eine `Statusart` — zwei Mengen, die heute
> zufällig einen Namen teilen. E‑82 ist genau daran entstanden, dass ein Name aus der einen
> Menge in die Position der anderen gerutscht ist.
> 
> **Kommt je eine zweite Kategorie zurück** (Regel Q3), verlangt TypeScript beide Einträge —
> statt still das Warndreieck weiterzumalen. Der Rendertest prüft beides und ist in beide
> Richtungen geeicht: ohne Zeichen rot, mit sichtbarem Wort ebenfalls rot.

> **Nicht geändert:** die Überschrift, der Leerzustand, `kategorie` im Antwortrumpf (Regel Q3 — kommt
> je eine zweite Kategorie zurück, ist dann **je Kategorie** zu gruppieren und nicht darüber
> hinweg), Rollup, Endpunkt und der übrige Antwortrumpf.

*Geändert am 03.09.2026 (Schritt 10b‑5): die Kategoriekennzeichnung je Zeile ist entfallen. Der alte
Wortlaut steht darunter.*

Die auffälligen Nachrichten im Fenster, neueste zuerst. Je Zeile **Zeitpunkt** und **Prozess bzw.
`sosName`**, mit Verweis ins Nachrichtendetail auf seiner **eigenen Route** — `/nachrichten/<id>` und
nicht `?nachricht=<id>`, das brächte eine Liste mit, die niemand angefragt hat.

**Die Überschrift bleibt, die Plakette je Zeile fällt.** Der Block ist seit 10b‑4 **ein einziges
Statement** und trägt nur noch Fehler ([`dashboard.md`](dashboard.md) §7a); `kategorie` ist eine
**Aufzählung mit einem Wert** (offener Punkt 133). Eine Plakette, die an jeder Zeile dasselbe Wort
sagt, unterscheidet nichts mehr und behauptet eine Auswahl, die es nicht gibt. Die Auskunft steht
jetzt einmal über dem Block statt einmal je Zeile — an der Stelle, an der sie noch etwas
unterscheidet.

> ### ⚠️ Mit der Plakette ist der Rohstatus je Zeile gefallen — **offener Punkt 136**
>
> Er stand ausschließlich in ihrem `title` und im Vorlese-Markup, **nie sichtbar**. Ihn jetzt
> sichtbar nachzuziehen wäre eine **neue Gestaltungsentscheidung** über diesen Block — welchen Platz
> er in der Zeile bekommt, ob er eine Plakette trägt, wie er neben dem Zeitpunkt steht — und keine
> Aufräumarbeit. Sie ist in diesem Schritt von niemandem getroffen worden.
>
> **Was der Nutzer heute verliert:** die Angabe, *welcher* Fehler es ist, ohne die Zeile zu öffnen.
> Sie steht im Detail, einen Klick entfernt. **Benannt und nicht nebenbei entschieden.**

**Das Feld `kategorie` bleibt trotzdem im Vertrag.** Regel Q3 verlangt, dass Problemkategorien
getrennt geführt und nie zu „Problem" zusammengefasst werden; kommt je eine zweite zurück, steht dort
ihr Platz — und dann kommt die Plakette mit ihr zurück.

> #### Der Stand bis zum 03.09.2026, wortgleich
>
> > Fehler und Überfällige im Fenster, neueste zuerst. Je Zeile Zeitpunkt, Plakette, Prozess bzw.
> > `sosName`, Verweis ins Nachrichtendetail auf seiner **eigenen Route** — `/nachrichten/<id>` und
> > nicht `?nachricht=<id>`, das brächte eine Liste mit, die niemand angefragt hat.
> >
> > **Die Plakette trägt `kategorie`, nicht `status`.** Ein Rohstatus beantwortete hier eine andere
> > Frage — *welcher Fehler* statt *ist es einer*. Der Rohwert steht im `title` und für
> > Vorleseprogramme im Markup.
> >
> > Beide Mengen sind **disjunkt** ([`dashboard.md`](dashboard.md) §7a): *Überfällig* setzt voraus,
> > dass die Nachricht nicht in einem Endstatus ist, und *Fehler* ist einer. Es gibt deshalb keine
> > Zeile mit zwei Plaketten.

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

## 6. Die bekannten Grenzen

> ### Es waren drei, seit dem 03.09.2026 sind es vier
>
> **Die drei aus 10b‑3b stehen unverändert** — keine von ihnen hing an *Überfällig*. Dazu kommt
> **§6.4**: *Läuft* zeigt lokal immer `0`.
>
> ⚠️ **Was der Auftrag zu 10b‑5 hier vermutet hat, stand nicht hier.** Er nennt „die alte bekannte
> Grenze zur Überfällig-Kachel"; die Aussage *„insgesamt klickt nicht"* stand aber in **§5.4** und
> nie in diesem Kapitel. Sie ist dort als Korrekturblock nachzulesen — **nachgesehen, nicht
> übernommen** (Regel V1).

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

### 6.4 *Läuft* zeigt lokal immer `0` — und das ist keine Aussage über den Bau

**`RUNNING` kommt auf der Testkopie null Mal vor.** Jeder Mandant sieht dort `0`, mit und ohne
Mandantenfilter; die zweite Zeile erscheint nie, und der Verweis führt auf eine leere Liste. **Das
gilt für die Anzeige genauso wie für den Endpunkt** — dort ist es offener Punkt **135** in
[`dashboard.md`](dashboard.md) §11.

| Was daraus folgt | |
|---|---|
| **Nicht gezeigt** | eine *Läuft*-Zahl über null, das Alter einer laufenden Nachricht, und damit auch die Lage, in der die zweite Zeile die Wächterfrist widerlegt |
| **Belegt ist es trotzdem** | über gestellte Antworten in `tests/dashboard-bloecke.test.tsx`, nicht über Daten — und **niemals durch Schreiben in die Testkopie** (Regel S1) |
| **Nachzuholen** | gegen die Produktion, zusammen mit Punkt 135 |

**Dasselbe gilt für drei weitere Lagen**, und alle drei sind über Tests und gestellte Antworten
belegt statt gefahren: „läuft seit X von Y" im Detail, die Notbremse über einem Jahr und
`ermittelbar = false`.

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
| ~~**89**~~ | ~~Es gibt keinen Weg, „nur überfällige" in der Liste selbst einzuschalten.~~ ✔ **Gegenstandslos seit dem 03.09.2026 (E‑71).** Die Abfrageform gibt es nicht mehr — weder am Endpunkt noch in der Oberfläche. Der Punkt bleibt stehen, weil er die Begründung trägt, warum eine Marke und kein Schalter gebaut worden war ([`nachrichtenliste.md`](nachrichtenliste.md) §5e) |
| ~~**136**~~ | ~~**Der Rohstatus je Zeile ist mit der Plakette aus „Zuletzt aufgefallen" gefallen** (§5.6). Er stand nur im `title`, war also nie sichtbar; ihn jetzt sichtbar nachzuziehen wäre eine neue Gestaltungsentscheidung über den Block und keine Aufräumarbeit. **Benannt und nicht nebenbei entschieden**~~ — ✔ **gegenstandslos seit dem 04.09.2026 (E‑90)**, und zwar nicht erledigt: Der Block trägt eine Zeile je **Prozess**, und die hat keinen Rohstatus — sie kann zwanzig verschiedene enthalten. Er steht in der Liste, auf die der Verweis führt (§5.6) |
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

---

## 11. Die Sichtprüfung zu 10b‑5 *(03.09.2026)*

**Zwölf Aufnahmen, alle gefahren.** Acht am laufenden System im angemeldeten Browser gegen die
Testkopie im Profil `dev`; vier auf einer temporären Route mit gestellten Antworten — die
Fensterbreite und der Fall `ermittelbar = false` sind anders nicht herstellbar. Die Route lag
außerhalb der Gruppe `(app)` (Verfahren aus §3), ist **entfernt** und steht in keinem Commit.

> ### ⚠️ Zuerst ein Befund über die Umgebung, und er hat die Prüfung fast gekippt
>
> **Das laufende Backend hielt noch die Klassen von vor 10b‑4.** Der Prozess war am 02.09.2026
> gestartet, das Kompilat stammte vom 03.09.; die Antwort trug weiterhin `kacheln.ueberfaellig`, und
> die nachgezogene Oberfläche lief in einen `TypeError` — *„Cannot read properties of undefined
> (reading 'ermittelbar')"*.
>
> **Das sah aus wie ein Fehler im eigenen Diff und war keiner.** Nach einem Neustart des Backends
> — **ohne eine Zeile Code** — lieferte derselbe Aufruf `laeuft` und `wartend`. Der Vermerk gehört
> hierher, weil der nächste, der die Landingpage nach einem Backend-Schritt ansieht, denselben
> Fehler sehen wird.

### Was gefahren worden ist

| # | Was | Mandant | Ergebnis |
|---|---|---|---|
| **1** | vier Kacheln, *Wartend* mit Zahl **und** Alter | `NEXANS` | ✔ `538` · *ältester seit 6 d 17 h* |
| **2** | **vier Kacheln, *Wartend* mit `0` ohne zweite Zeile** | **`VOTG`** | ✔ die Kachel steht da und zeigt `0` |
| **3** | drei Kacheln, keine Lücke | `SUTTONS` | ✔ die Reihe füllt die Breite, kein Platzhalter |
| **4** | alle drei im **schmalen** Fenster | gestellt | ✔ 390 px, eine Spalte, nichts abgeschnitten, kein waagerechtes Scrollen |
| **5** | genau **eine** Kachel mit Fläche | `NEXANS` | ✔ nur *Fehler*; *Läuft* und *Wartend* tragen die Plakette auf `--card` |
| **6** | **Klick auf *Wartend* → Ziel zeigt 538** | `NEXANS` | ✔ **538 = 538**, über elf Seiten vollständig gezählt |
| **7** | Klick auf *Läuft* → leere Liste, keine Fehlermeldung | `NEXANS` | ✔ `200`, null Zeilen, der Leerzustand der Liste |
| **8** | „Zuletzt aufgefallen" ohne verwaiste Kategoriekennzeichnung | `NEXANS` | ✔ Überschrift bleibt, keine Plakette je Zeile |
| **9** | Detail einer `SUSPENDED`-Nachricht | `NEXANS` | ✔ *„wartet seit 15 h 35 min"*, **keine Frist**, keine Kennzeichnung |
| **10** | alter Link `?ueberfaellig=true` | `NEXANS` | ✔ ungefilterte Liste, *„Status: Alle Status"*, keine Fehlerseite, keine Marke |
| **11** | dunkler Modus, beide neuen Kacheln | gestellt | ✔ Plakette und Zahl lesbar, weiterhin genau eine Fläche |
| **12** | `ermittelbar = false` | gestellt | ✔ **sichtbar anders als 3** — siehe unten |

### 6 ist die Abnahme, und sie geht auf

**Die Kachel nennt 538, das Ziel zeigt 538.** Gezählt wurde die Liste vollständig über den Cursor,
nicht die erste Seite geschätzt: **elf Seiten, 538 Zeilen.**

Die Adresse, die der Klick erzeugt hat:

```
/nachrichten?status=WARTEND&von=2025-12-23T00:00:00Z&bis=2025-12-30T04:00:00Z
```

`aeltesteSekunden` war zum Zeitpunkt der Aufnahme **579.953** — 6 d 17 h 5 min 53 s. Gerechnet:
`bis` 04:00 Z minus eine Eimerbreite (1 h) minus das Alter ergibt `2025-12-23T09:54:07Z`, abgerundet
auf den Tagesanfang **`2025-12-23T00:00:00Z`**. Genau so steht es in der URL.

> ### ⚠️ Und das ist die Zahl, die E‑80 rechtfertigt
>
> **Mit dem *geerbten* Fenster hätte dasselbe Ziel `1` gezeigt** — nachgemessen am selben Bestand:
> `status=WARTEND` über `2025-12-28T04:00Z … 2025-12-30T04:00Z` liefert **eine** Zeile.
>
> **Eine Kachel, die 538 nennt und auf 1 führt.** Genau davor hat E‑m die Klickbarkeit
> abgeschaltet; E‑80 löst denselben Fall, statt ihn zu vermeiden. Der Unterschied zwischen den
> beiden Entscheidungen ist an dieser einen Zahl abzulesen.

### 12 ist die einzige Aufnahme, die E‑81 belegt

**Drei Bilder, und die beiden mittleren dürfen nie zusammenfallen:**

| Zustand | Was im Bild steht |
|---|---|
| **3 — strukturell abwesend** (`SUTTONS`) | Die Kachel **existiert nicht**. Drei Kacheln, die Reihe zieht sich zusammen |
| **12 — nicht ermittelbar** (gestellt) | Die Kachel **steht da**: Plakette *Wartend*, an der Stelle der Zahl ein durchgestrichenes Zeichen und ein Gedankenstrich, darunter der Satz mit dem Grund. **Kein Verweis** |
| **1 — ermittelt** (`NEXANS`) | Plakette, Zahl, Alter, Verweis |

**Der Unterschied zwischen 3 und 12 ist auf einen Blick zu sehen** — die eine Kachel ist weg, die
andere ist da und sagt, dass sie es gerade nicht weiß. Das war die Anforderung.

**Und die übrigen Kacheln bleiben in 12 unberührt:** *Läuft* zeigt seine `0` und klickt weiter. Die
beiden Zahlen **einer** Kachel fallen zusammen, die beiden **Kacheln** nicht.

### Was auf derselben Route mitgeprüft worden ist

Zwei Lagen, die auf der Testkopie nicht vorkommen, sind auf der Probeseite gestellt worden — und
beide sind **auch über Tests** belegt, nie durch Schreiben in die Testkopie (Regel S1):

| Lage | Was zu sehen war |
|---|---|
| **Die Notbremse** | `aeltesteSekunden = 400 d` → die Zahl steht, das Alter steht (*„ältester seit 400 d"*), **kein Verweis**, und der Satz nennt den Grund |
| ***Läuft* über null** | `3` mit *„ältester seit 40 min"*. **Das ist zugleich die Prüfung aus E‑75 in Aktion:** 40 Minuten über einer Wächterfrist von rund 30 wären der Befund, dass der Wächter hängt |

### Das Werkzeug

**Kopfloses Chrome über das DevTools-Protokoll, `deviceScaleFactor: 2`**, für die vier gestellten
Aufnahmen; die acht übrigen im angemeldeten Browser über die Erweiterung.

> **`resize_window` der Erweiterung meldet weiterhin Erfolg und ändert nichts.** Der Befund aus
> §10.6 gilt unverändert: `Emulation.setDeviceMetricsOverride` über CDP wirkt, die Erweiterung
> nicht. Für Aufnahme 4 war das der Grund, überhaupt auf CDP zu gehen.
>
> **Die Probeseite braucht keine Anmeldung, aber ein Cookie.** `src/proxy.ts` prüft nur die
> **Anwesenheit** von `OVERLORD_SESSION`, nicht seine Gültigkeit — ein erfundener Wert über
> `Network.setCookie` genügt, und die Route spricht ohnehin nicht mit dem Backend.
