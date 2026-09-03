# Visuelles Konzept

Entsteht in Schritt 3, Teil 2 (Frontend). Beschreibt **warum** die Oberfläche so aussieht, wo die
Werte liegen und wie man sie ändert.

Der Aufbau der Anwendung — Rewrite, Routensperre, Sprachdateien, Zwischenspeicher, Fehlerformat —
steht in [`frontend-grundlagen.md`](frontend-grundlagen.md).

---

## 1. Der Leitsatz

> Der typische Nutzer ist kein EDI-Spezialist. Er sucht einen Beleg und will wissen, wo dieser
> steht. Interne IDs, Statuscodes und Servicenamen sind Beiwerk.

Daraus folgt alles Weitere. Und aus einem zweiten Satz: **Das Werkzeug wird geöffnet, wenn etwas
nicht stimmt.** Wer es aufmacht, ist schon angespannt. Eine Oberfläche, die selbst nervös wirkt —
Farbflächen, wandernde Elemente, blinkende Zähler —, macht die Lage schlechter, nicht besser.

Ruhig, dicht, ohne dekorative Bewegung.

---

## 2. Das Konzept ist ein Vorschlag — deshalb ist es austauschbar

Das ist Abnahmekriterium, nicht Absichtserklärung:

| Regel | Wo durchgesetzt |
|---|---|
| Alle Farben ausschließlich über CSS-Variablen | Test `tests/farbwerte.test.ts` |
| Kein Hex-Wert, keine Tailwind-Farbklasse in einer Komponente | derselbe Test |
| Statusfarben hinter **semantischen** Tokens | `src/app/globals.css` |
| Zuordnung Status → Token an **genau einer** Stelle | `src/lib/status-farbe.ts` |
| Typografie und Dichte über eine kleine benannte Menge | `src/app/globals.css` |

**Ergebnis:** Das Konzept lässt sich in `src/app/globals.css` ändern, ohne eine Komponente
anzufassen. Wer die Zuordnung ändern will — etwa „quittiert bekommt eine eigene Farbe" — ändert
`src/lib/status-farbe.ts` und sonst nichts.

Der Test nimmt `src/components/ui` aus. Dort liegt der Generatorbereich von shadcn/ui; was der
Generator schreibt, wird nicht von Hand umgebaut, und ihn zu prüfen hieße, den Test bei jedem
`shadcn add` zu reparieren. Für eigene Bausteine gilt die Regel ohne Ausnahme.

---

## 3. Farbe

### Die Statusfarben sind fachlich vergeben, nicht gestalterisch

Sie stehen nicht zur Wahl. Grün heißt abgeschlossen, Rot heißt Fehler — das ist die Erwartung, mit
der jeder Nutzer ankommt, und sie umzudeuten kostet mehr, als jede Gestaltung gewinnen kann.

| Rolle | Bedeutung | Farbe | Warum |
|---|---|---|---|
| `--status-abgeschlossen` | fertig und quittiert | Grün, Ton 166 | Der Endzustand, den der Nutzer sucht. Grün heißt hier „nichts zu tun". Tiefer und blaustichiger als die naheliegende Wahl — siehe „Der Akzent". |
| `--status-fehler` | `ERROR_*` und `COMMIT_REJECTED` | Rot | Der einzige Zustand, der sofort Aufmerksamkeit verlangt. Rot ist deshalb selten. |
| `--status-offen` | wartend, laufend, **aufgeteilt, zusammengeführt** | Neutral | Kein Ergebnis, kein Problem. Farbe wäre hier eine Aussage, die es nicht gibt — `SPLITTED` und `MERGED` machen zusammen rund ein Drittel aller Zeilen aus. **Beide teilen sich seit dem 11.08.2026 diese eine Rolle, obwohl sie zwei Statusarten sind:** Der Unterschied zwischen „aufgeteilt" und „zusammengeführt" ist keine Aussage über *gut oder schlecht*, und nur die trägt eine Farbe. Er steckt in Beschriftung und Zeichen. |
| `--status-ungeklaert` | unbekannter Statuswert | Neutral, gedämpft | „Nicht zugeordnet heißt nicht zugeordnet" (Regel Q4). Ein geratener Wert wäre schlimmer als sichtbare Zurückhaltung. |

Jede Rolle hat drei Werte: Vordergrund, `-flaeche` und `-kontur`. Damit lässt sich ein Status als
Text, als Plakette oder als Zeilenmarkierung zeigen, ohne dass irgendwo ein vierter Wert erfunden
wird.

**Es gibt keine zweite Bedeutung von Grün oder Rot.** Insbesondere keine grüne Hauptschaltfläche —
sie hieße „abgeschlossen" an einer Stelle, an der noch nichts abgeschlossen ist. `--destructive`
aus shadcn/ui zeigt bewusst auf denselben Wert wie `--status-fehler`: ein zweites Rot wäre ein
zweites Vokabular.

### Status wird nie allein über Farbe ausgedrückt

**Jede Statusanzeige trägt zusätzlich eine Beschriftung oder ein Zeichen.** Die Farbrolle ist die
halbe Aussage, nie die ganze. Die Regel steht hier und als Kommentar an `lib/status-farbe.ts`,
damit die Nachrichtenliste in Schritt 4 von Anfang an so gebaut wird — nachträglich ist sie in
jeder Zelle einzeln nachzurüsten.

Zwei Gründe, beide für sich ausreichend:

1. **Rot-Grün-Schwäche betrifft rund acht Prozent der Männer.** Dieses Werkzeug ist ein
   Fehlermelder; ein Status, den man nicht unterscheiden kann, ist keiner.
2. **Der Akzent ist seit dem Wechsel ein Gelbgrün** und liegt damit zwischen den beiden Zonen, die
   Abschnitt 4.1 der Projektbeschreibung fachlich belegt. Eine farbige Hervorhebung darf nicht als
   Statusaussage lesbar sein.

> ### Zwei Anwendungen dieser Regel in der Prozessansicht, und eine Abweichung davon *(02.09.2026)*
>
> **Die Regel selbst hat sich durchgesetzt.** Der Zustand „noch nie" trug im Prozessbaum eine
> Dämpfung *und* ein Wort; als die Kennzeichnung fiel, fiel **beides zusammen**
> ([`process-view.md`](process-view.md) §17, E‑56). Die Dämpfung allein stehen zu lassen wäre genau
> der verbotene Fall gewesen — ein Zustand nur über Helligkeit. Das ist der Grund, warum die
> Entscheidung nicht die halbe sein konnte.
>
> ⚠️ **Die Abweichung betrifft nicht Farbe, sondern die leere Stelle.** Ein Blatt des Baums, dessen
> Richtung *nicht ermittelt* ist, trug ein eigenes Zeichen — nach demselben Gedanken wie „nicht
> zugeordnet heißt nicht zugeordnet": Eine leere Stelle sagt nichts. Seit E‑58 trägt es **nichts**.
> Der Verzicht ist bewusst und begründet: Die Angabe steht im Katalog, und ein Zeichen an *jeder*
> Zeile eines Mandanten ohne kuratierte Richtung — bei `VOTG` alle 390 — unterscheidet nichts mehr.
> **Er gilt für diese eine Stelle und nicht als neue Regel**; überall sonst bekommt das Fehlende
> weiterhin ein Wort ([`process-view.md`](process-view.md) §29).
>
> **Und er ist am 03.09.2026 kleiner geworden.** Einen Tag lang stand an derselben Stelle bei
> *bekannter* Richtung ein Wort (E‑55) — damit gab es die Angabe in zwei Schreibweisen
> nebeneinander, als Ebene und als Vorsatz. Seither trägt die Ebene sie überall, wo sie bekannt
> ist; die leere Stelle bleibt nur dort, wo auch die Angabe fehlt.

### Der Akzent

`--akzent: oklch(0.777 0.1643 112.4)` — das ist genau `#b9c022`, ein Gelbgrün.

**Er ist eine Füllfarbe, keine Schriftfarbe.** Auf Weiß erreicht er 1,98 : 1 und fällt damit
deutlich durch jede Anforderung an Textkontrast. Deshalb hat der Akzent vier Stufen statt einer:

| Token | Wert | Gemessen | Wofür |
|---|---|---|---|
| `--akzent` | `#b9c022` | 1,98 : 1 auf Weiß | **nur Fläche.** Gefüllte Schaltfläche, Kennzeichnung |
| `--akzent-schrift` | `#6a6f0f` | 5,40 : 1 auf `--card`, 5,18 : 1 auf `--background`, 4,91 : 1 auf `--akzent-flaeche` | Verweise, aktive Beschriftungen, dünne Linien, **Fokusring** |
| `--akzent-vordergrund` | `#161802` | 9,13 : 1 auf `--akzent` | Schrift **auf** der Akzentfläche |
| `--akzent-flaeche` | `#f3f6dc` | — | blasse Tönung, etwa die aktive Navigationszeile |

**Auf der Akzentfläche steht dunkle Schrift, niemals weiße.** Weiß käme dort auf 1,98 : 1, die
dunkle Variante auf 9,13 : 1. Das betrifft gefüllte Schaltflächen, Kennzeichnungen und den aktiven
Navigationseintrag. Durchgesetzt ist es an einer Stelle: `--primary-foreground` und
`--accent-foreground` in `globals.css` zeigen auf die dunklen Stufen, nicht auf `--akzent`.

**Der Abstand zu den Statusfarben ist der eigentliche Preis dieser Farbe.** Zwei Maßnahmen, beide
umgesetzt:

- Das **Status-Grün** ist von Ton 150 auf **166** gewandert und zugleich tiefer geworden
  (`oklch(0.46 0.095 166)`, `#01684c`, 6,78 : 1 auf Weiß). Der Abstand zum Akzent in OKLab wächst
  damit von 0,295 auf 0,343 — praktisch derselbe Abstand, den der Akzent zu Rot hat (0,352). Rot
  bei Ton 27 bleibt unverändert; es liegt 85 Grad entfernt.
- Status wird **nie allein über Farbe** ausgedrückt, siehe oben.

**Ein Status-Gelb gibt es in diesem Farbsystem nicht.** `--status-offen` ist bewusst neutral
(Begründung in der Tabelle oben). Entsteht in Schritt 4 oder 10 doch eine gelbe Rolle — etwa für
„überfällig" —, gehört sie auf die **orange** Seite, Ton höchstens 85. Zwischen 90 und 135 stünde
sie dem Akzent zu nahe.

> **Genau das ist am 31.08.2026 geschehen, und der Satz hat gehalten:** `--ueberfaellig` liegt bei
> **Ton 80**. Es ist keine Statusrolle — *überfällig* ist eine Problemkategorie und liegt quer zur
> Einordnung —, und sie steht deshalb nicht in der Tabelle oben, sondern in **§7a** mit allen
> gerechneten Werten.

**Der Fokusrahmen** ist `--akzent-schrift`, nicht `--akzent`: Er muss sich vom Untergrund abheben,
nicht zur Marke passen. Auf hellem Grund sind das 5,40 : 1. Im Dunkelmodus-Block kehrt sich das um
— dort ist `--akzent-schrift` eine aufgehellte Stufe (`#c9d151`, 10,7 : 1 auf `--card`).

**Was diese Farbe nicht kann:** Eine gefüllte Akzentfläche erreicht gegen Weiß nur 1,98 : 1 und
verfehlt damit die 3 : 1, die WCAG 1.4.11 für die Umrisse von Bedienelementen verlangt. Erkennbar
ist die Schaltfläche über ihre Beschriftung (9,13 : 1); wer die Lücke schließen will, gibt
gefüllten Flächen zusätzlich eine Kontur in `--akzent-schrift` — das ist die einzige bekannte
Grenze der Farbe und bewusst nicht heimlich umgangen worden.

Der Akzent trägt Schaltflächen, den Fokusring und die aktive Navigationszeile — also alles, was die
Anwendung über sich selbst sagt, und nichts, was sie über die Daten sagt.

### Die Grundfläche

Achromatisches Neutral. **Begründung:** In einer Liste, die zu einem Drittel aus Zwischenprodukten
besteht, muss jeder Farbfleck etwas bedeuten. Eine getönte Grundfläche zöge Aufmerksamkeit, die den
vier Statusfarben gehört.

Der Inhaltsbereich ist minimal dunkler als die Karten darauf (`--background` 0.985 gegen `--card`
1.0). Das trennt Flächen ohne eine einzige zusätzliche Linie.

---

## 4. Schrift

| Rolle | Wert | Warum |
|---|---|---|
| `--font-sans` | Geist | Neutrale Grotesk mit großer x-Höhe — auf Listen ausgelegt, nicht auf Fließtext. Keine Persönlichkeit, die bei jedem Blick mitspricht. |
| `--font-mono` | Geist Mono | Für Mandantencodes, `MessageID`, Kennungen. Feste Laufweite macht Vergleichen und Abtippen möglich; in einer Proportionalschrift sind `l`, `1` und `I` genau dort nicht unterscheidbar, wo es darauf ankommt. |

Drei Größenrollen statt einer freien Skala:

| Token | Wert | Verwendung |
|---|---|---|
| `--text-ueberschrift` | 1.125 rem | Seitentitel, Produktname |
| `--text-basis` | 0.9375 rem | alles Übrige |
| `--text-beiwerk` | 0.8125 rem | IDs, Statuscodes, Servicenamen, Hinweise |

**Begründung für drei:** Der Leitsatz sagt, was Beiwerk ist. Eine eigene Größe dafür macht die
Unterscheidung sichtbar, ohne dass jemand pro Komponente entscheiden muss. Mehr als drei Stufen
wären eine Entscheidung, die niemand konsistent trifft.

Die Basisgröße ist mit 15 px etwas kleiner als die üblichen 16 px — bewusst: Das Werkzeug zeigt
Listen, und eine Zeile mehr auf dem Bildschirm ist hier mehr wert als ein Punkt Schriftgröße.

Ziffern laufen in Tabellen und Zeitangaben mit fester Breite (`font-variant-numeric: tabular-nums`),
sonst tanzen Zeitstempel von Zeile zu Zeile.

### Die drei Rollen sind Verhältnisse, keine Pixel — seit dem 01.09.2026 sichtbar

*Die Werte oben stehen in `rem`, und das war schon immer so. Was sich geändert hat, ist, dass es
jetzt einen Unterschied macht.*

**Der Nutzer wählt die Anzeigegröße in vier Stufen** (`docs/dichte-umschalter.md`). Umgestellt wird
dabei **die Wurzel** und nicht die drei Rollen einzeln — `html { font-size: var(--dichte-wurzel) }`.
Die Rollen bleiben unverändert dort stehen, wo sie stehen; sie rechnen ab jetzt nur gegen eine
andere Bezugsgröße.

| Rolle | Wert | `xs` (87,5 %) | `s` (93,75 %) | **`m` (100 %)** | `l` (112,5 %) |
|---|---|---:|---:|---:|---:|
| `--text-ueberschrift` | 1.125 rem | 15,75 px | 16,875 px | **18 px** | 20,25 px |
| `--text-basis` | 0.9375 rem | 13,125 px | 14,0625 px | **15 px** | 16,875 px |
| `--text-beiwerk` | 0.8125 rem | 11,375 px | 12,1875 px | **13 px** | 14,625 px |

*(Bei einer Browservorgabe von 16 px. Die Basiszeile ist gemessen, die beiden anderen sind daraus
gerechnet.)*

**Warum die Wurzel und nicht die drei Rollen.** Das Ziel ist mehr Zeilen je Bildschirm. Dafür muss
`--dichte-zeile` mitgehen und mit ihm jeder Innen- und Außenabstand — drei umgestellte
Schriftgrößen in einem unveränderten Raster ergäben kleine Schrift in großen Zellen und keine
einzige Zeile mehr. Gemessen: **28 statt 24 sichtbare Zeilen** in `xs`, 19 in `l`
(`dichte-umschalter.md` §5.3).

**`m` ist der heutige Zustand und bleibt die Vorgabe.** Jede Zahl in diesem Dokument ist gegen ihn
gemessen; wäre er nicht mehr die Vorgabe, wären sie Messungen eines Zustands, den niemand mehr
sieht.

---

## 5. Dichte und Breite

### Die Anwendung läuft über die volle Fensterbreite

**Am Rahmen gibt es keine Maximalbreite.** Kopfzeile, Navigationsspalte und Inhaltsbereich spannen
über das gesamte Fenster; die Navigationsspalte sitzt bündig an der linken Fensterkante, abgesetzt
durch eine Trennlinie.

**Begründung:** Für eine Website ist ein zentrierter Container richtig, für eine Anwendung nicht.
Bei 1700 px Fensterbreite blieben mit der alten Maximalbreite rund 290 px links **neben** der
Navigation ungenutzt — die Seitenleiste schwebte in der Mitte. Ab Schritt 4 zeigt der
Inhaltsbereich eine Nachrichtenliste mit Zeitstempel, Status, Partner, Prozess, Belegnummer und
BAM-Werten. Die braucht jede Spalte, die das Fenster hergibt.

**Die Ausnahme:** Eine Maximalbreite darf es weiterhin **innerhalb** einer Ansicht geben, wenn dort
Fließtext steht — `--dichte-inhaltsbreite` (72 rem) und die schmaleren Karten von
Mandantenauswahl und Passwortseite tun genau das. Sie gehört in die Ansicht, **nie** in den Rahmen.

**`--dichte-inhaltsbreite` begrenzt seit Schritt 5, Teil 2 auch die Detailansicht auf ihrer eigenen
Route** (`/nachrichten/<id>`), und seit dem 11.08.2026 ist das dort die tragende Breite: Ein
Umschalter im Kopf führt aus dem Panel dorthin und zurück
([`nachrichtendetail.md`](nachrichtendetail.md) §10.7). **Linksbündig, nicht zentriert**, damit der
Lesebeginn beim Umschalten an derselben x-Position bleibt.

> ⚠️ **Für diesen Zweck sind die 72 rem gewählt, nicht gemessen.** Die Begründung unten gilt dem
> **Fließtext**, und eine Detailansicht ist keiner. Es gibt keine Messung, die eine andere Zahl
> trägt — und es wird für diesen Zweck auch keine erfunden. Ein vorhandenes Token ist hier besser als
> eine zweite frei gewählte Zahl daneben.

### Die Maße

| Token | Zeigergerät | Berührungsgerät | Warum |
|---|---|---|---|
| `--dichte-wurzel` | **100 %** | 100 % | Die Schriftgröße des **Wurzelelements** und damit der Bezug jedes `rem`. Der eine Wert, den der Dichteumschalter umstellt — vier Stufen, siehe die Anmerkung unter dieser Tabelle |
| `--dichte-beruehrung` | `max(2.75rem, 44px)` | ebenso | Mindestfläche am Finger. Wird **nirgends** unterschritten — und ist deshalb das einzige Maß, das aus der Skalierung **heraus** ist |
| `--dichte-bedienelement` | 2 rem (32 px) | → `beruehrung` | Schaltfläche im Rahmen: Menü, Mandant, Sprache, Nutzermenü |
| `--dichte-navzeile` | 2.125 rem (34 px) | → `beruehrung` | Navigationseintrag, vorher 44 px |
| `--dichte-kopfzeile` | 3.125 rem (50 px) | 3.5 rem | Kopfzeile, vorher 56 px |
| `--dichte-navspalte` | 13 rem (208 px) | 13 rem | Navigationsspalte einschließlich Innenabstand |
| `--dichte-suchbereich` | 18 rem (288 px) | 18 rem | reservierter Platz für die BAM-Suche, Schritt 7 |
| `--dichte-baumspalte` | **26 rem** (416 px) | ebenso | Breite der Baumspalte der Prozessansicht **ab `xl`**, seit dem 02.09.2026. Darunter bekommt sie einen **Anteil** (40 %) und keine feste Breite: Bei 768 px blieben von rund 520 px Inhalt sonst gut hundert für die Liste daneben. **Gemessen und nicht gewählt** — [`process-view.md`](process-view.md) §16 (M118) |
| `--dichte-feld` | 2.5 rem | 2.5 rem | Eingabefeld im Formular — bleibt bewusst komfortabel |
| `--dichte-zeile` | 2.25 rem | 2.25 rem | Tabellenzeile ab Schritt 4 |
| `--dichte-bedienzeile` | → `zeile` | → `beruehrung` | **Die Zeile, die zugleich ein Bedienelement ist**, seit dem 02.09.2026: Baumzeile der Prozessansicht und Auswahlzeile der Prozessauswahl. Am Zeigergerät eine Zeile, am Finger ein Ziel — dieselbe Bauform wie `bedienelement` und `navzeile` eine Zeile höher. **Gemessen und nicht gewählt** — [`process-view.md`](process-view.md) §24 (E‑54, M127) |
| `--dichte-beschriftung` | **10 rem** im Panel · **16 rem** auf der eigenen Route | ebenso | **gedeckelte** Breite einer Beschriftungsspalte neben ihren Werten, seit Schritt 7. Der Deckel gehört zum **Einhängepunkt**: Die Route hebt den Wert über `.beschriftung-breit` herauf |
| `--dichte-inhaltsbreite` | 72 rem | 72 rem | Maximalbreite **innerhalb** einer Ansicht |

> ### `--dichte-wurzel` und die eine Ausnahme *(01.09.2026)*
>
> **Alle Maße dieser Tabelle liegen in `rem`**, und über die Abstandsskala von Tailwind gilt das
> auch für jeden Innen- und Außenabstand. Deshalb genügt **eine** Zahl, um die ganze Oberfläche
> dichter oder luftiger zu machen: die Schriftgröße des Wurzelelements. Der Dichteumschalter stellt
> genau sie um, in vier Stufen — `87,5 % · 93,75 % · 100 % · 112,5 %`, eingehängt über
> `html[data-dichte="…"]`. Begründung, Messung und Bedienung stehen in
> [`dichte-umschalter.md`](dichte-umschalter.md).
>
> **Prozent und nicht Pixel:** Eine feste Pixelangabe an der Wurzel überschriebe die
> Grundschriftgröße, die ein Nutzer im Browser eingestellt hat. Wer sie hochgesetzt hat, hat einen
> Grund.
>
> ### Die Bedienzeile — warum ein Token dazugekommen ist *(02.09.2026)*
>
> **Eine Zeile, die man anklickt, ist zwei Dinge zugleich**, und bis heute hat sie sich für eines
> entschieden: Baumzeile und Auswahlzeile trugen `--dichte-beruehrung` und waren damit in `xs`, `s`
> und `m` gleich hoch. **Gemessen** ([`process-view.md`](process-view.md) §16, M121): Der
> Dichteumschalter bewegte im Prozessbaum drei Zeilen über die ganze Skala, in der Nachrichtenliste
> neun.
>
> `--dichte-bedienzeile` löst das, **ohne `--dichte-beruehrung` anzufassen**: Es steht am
> Zeigergerät auf `--dichte-zeile` und fällt unter `@media (pointer: coarse)` auf die Fläche zurück
> — genau die Regel, die `--dichte-bedienelement` und `--dichte-navzeile` schon tragen.
> Nachgemessen (M127): **26 / 24 / 22 / 19** sichtbare Baumzeilen statt 19/19/18/16, und am Finger
> unverändert 44 / 44 / 44 / 49,5 px.
>
> ⚠️ **`pointer` und nicht `any-pointer`, und der Preis steht dazu:** Ein Notebook mit
> Berührungsbildschirm **und** Trackpad meldet `fine` und bekommt die kürzere Zeile.
> `any-pointer: coarse` erfasste es — und ließe die Verkleinerung dann praktisch nirgends greifen.
> Die Wahl ist dieselbe wie in der `@media`-Regel, die diese Datei schon führt; eine zweite Antwort
> daneben wären zwei Begriffe von „Berührungsgerät".

> **`--dichte-beruehrung` ist aus der Skalierung heraus, und das ist der Preis für den Satz in
> seiner Zeile.** „Wird nirgends unterschritten" wäre nicht mehr wahr, wenn das Token mitskalierte:
> In der Stufe `xs` fiele es auf 2,75 × 14 = **38,5 px**. Es steht deshalb als
> `max(2.75rem, 44px)` da — der Boden greift in den beiden kleinen Stufen, die Skalierung greift in
> `l` (49,5 px) weiterhin. **Ein festes `44px` wäre die schlechtere Lösung**: Es nähme dem Nutzer
> mit vergrößerter Grundschrift den Zuwachs.
>
> Gemessen bei `pointer: coarse`: 44 · 44 · 44 · 49,5 px. **Was das Token trägt, hält das Maß in
> jeder Stufe — in der HÖHE**: Navigationseinträge, Mandantenumschalter, Sprachwahl, Nutzermenü und
> jeder Menüeintrag. **Eine Fläche von 44 × 44 ist es nicht.** `min-h-*` sagt über die Breite
> nichts, und ein Symbolknopf trägt daneben `size-8`; gemessen 28 × 44 px in `xs`, 32 × 44 in `m`.
> **Was das Token gar nicht trägt, hielt das Maß auch vorher nicht**: `--dichte-feld` (40 px in
> `m`), die Tabellenzeile (36 px) und der Sortierknopf im Tabellenkopf (20 px). Ausgeschrieben in
> [`dichte-umschalter.md`](dichte-umschalter.md) §5.4, geführt als offener Punkt 96.

> ### Die 26 rem der Baumspalte sind **gemessen**, anders als die 72 rem darüber *(02.09.2026)*
>
> Drei Zahlen tragen sie, und alle drei stehen in [`process-view.md`](process-view.md) §16:
>
> 1. **Bei `NEXANS` brechen 17 von 733 Prozessnamen um** (2,3 %), und ab hier liegt die Gesamthöhe
>    des aufgeklappten Baums innerhalb von 0,4 % ihres Grenzwerts.
> 2. **Es ist der größte Wert, bei dem die Übertragungsliste daneben bei 1440 px noch vollständig
>    steht** — ihre Tabelle braucht gemessene 744 px.
> 3. **Es ist die Breite, die das Nachrichtenpanel ab `xl` ohnehin hat.** Eine Zahl statt zwei.
>
> **Die 34 rem der flachen Prozessauswahl (§7a dort) gelten hier ausdrücklich nicht.** Dort steht der
> Name in einer Schublade über der ganzen Breite; hier kommen Einrückung, Aufklappzeichen und zwei
> Zahlen dazu, und die Spalte steht neben einer Tabelle, der jeder Pixel fehlt, den der Baum nimmt.
>
> **Und ein Befund, der über diese Zahl hinausgeht:** Die Umbruchzahlen sind in **allen vier
> Dichtestufen identisch**. Breite und Schriftgröße liegen beide in `rem` und skalieren mit derselben
> Zahl — die Stufe ändert die Pixel, nicht die Umbrüche. Wer eine Breite in `rem` prüft, prüft sie
> für alle vier Stufen zugleich.

> **`--dichte-beschriftung` ist heute an genau einer Stelle im Einsatz** — im Belegdaten-Block
> ([`bam-werte.md`](bam-werte.md) §11a). Es steht hier, weil die Dichtewerte in `globals.css` wohnen
> und diese Tabelle ihr Verzeichnis ist; die **Bauform** dahinter (Beschriftung links, Werte rechts
> als Marken) steht bewusst **nicht** hier, solange sie eine einzige Ansicht betrifft.
>
> **Die 10 rem sind gemessen und nicht gewählt** — anders als die 72 rem eine Zeile darunter. Im
> Panel bleiben damit 282 px für die Werte, und die längste gemessene Belegnummer braucht als Marke
> rund 285 px. Eine breitere Beschriftungsspalte spart der Beschriftung eine Zeile und zwingt dafür
> die **Hauptinformation** in den Umbruch; die Herleitung steht in `bam-werte.md` §11a. **Wer die
> Zahl ändert, prüft sie im Panel und nicht auf der eigenen Route** — dort ist die Zeile doppelt so
> breit und der Fehler unsichtbar.
>
> **Deshalb trägt die Zeile seit dem 13.08.2026 zwei Werte** *(Nachbesserung nach der Nacharbeit
> desselben Tages)*. Der Satz oben bleibt richtig und bleibt stehen — er ist genau der Grund für die
> Teilung: Die 10 rem sind die Rechnung *„was bleibt dem Wert übrig"*, und auf der eigenen Route
> (Gruppenzeile **1.126 px** statt 454) bleibt dem Wert reichlich — bei 10 rem wären es dort 954 px
> für eine Marke von rund 285 px gewesen. Die Beschriftungen brachen also um, ohne dass jemand
> dadurch Platz gewann. **16 rem = 256 px**, gegen die längste gemessene Beschriftung von
> **249 px**; es bleibt ein Deckel und keine feste Breite.
>
> **Umgesetzt am Einhängepunkt, nicht am Block.** `.beschriftung-breit` in `globals.css` setzt
> `--dichte-beschriftung` auf dem Wrapper der Route herauf; der Block liest den Wert wie bisher und
> erfährt nicht, wo er hängt. Das trägt, weil `@theme inline` den Verweis in die Utility-Klasse
> schreibt statt den aufgelösten Wert (`grid-template-columns: var(--dichte-beschriftung) minmax(0,
> 1fr)`, im gebauten CSS nachgesehen) — **wer diese Klasse ändert, prüft das dort erneut nach.**

Gemessen bei 1920 px: Vier Navigationseinträge belegen 142 px statt vorher rund 236 px, die
Kopfzeile 51 px statt 57.

**Begründung für die Dichte:** Das Werkzeug wird geöffnet, wenn etwas nicht stimmt. Wer eine
Nachricht sucht, überfliegt viele Zeilen; großzügige Abstände bedeuten hier scrollen, und scrollen
bedeutet, den Überblick zu verlieren. Übersicht schlägt Atmosphäre.

**Begründung für die Unterscheidung nach Zeiger:** Die Umschaltung hängt an
`@media (pointer: coarse)`, **nicht** an der Bildschirmbreite. Ein schmales Browserfenster am
Rechner ist kein Handy, und ein breites Tablet ist keins mit Maus. Die Breite sagt nichts darüber
aus, womit jemand zielt. Nachgemessen: Unter der Regel sind Navigationseinträge, Mandantenumschalter,
Sprachumschaltung und Nutzermenü sämtlich 44 px hoch, die Kopfzeile 56.

**Begründung für die 72 rem im Fließtext:** Längere Zeilen sind schwer zu lesen — der Grund gilt
für Text, nicht für Tabellen. Deshalb steht die Grenze in der Ansicht und nicht am Rahmen.

### Der reservierte Suchplatz — ✔ **gefüllt am 13.08.2026 (Schritt 7, Teil 3)**

In der Kopfzeile steht links neben dem Mandantenumschalter ein Bereich fester Breite
(`data-bereich="suche"`).

> **Der ursprüngliche Vermerk bleibt stehen, weil er die Entscheidung trägt.** Er lautete: „Kein
> Eingabefeld, kein Platzhalter, keine Attrappe — **bis Schritt 7 ihn füllt**." Genau das ist am
> 13.08.2026 geschehen: Dort sitzt jetzt das Belegnummern-Suchfeld samt optionaler Belegart und
> `+`-Schaltfläche ([`bam-suche.md`](bam-suche.md) §11.1). Gelöscht wird der Vermerk nicht —
> **erledigt gekennzeichnet**, damit erkennbar bleibt, dass die Breite von Anfang an für dieses
> Feld reserviert war und nicht nachträglich zurechtgeschoben wurde.

**Begründung:** Die Suche ist laut Leitsatz der Haupteinstieg. Steht ihr Platz nicht von Anfang an
fest, drängt sie sich später zwischen Mandant, Sprache und Nutzermenü. Ein Feld, das nichts tut,
wäre trotzdem schlechter als keins: Es verspricht eine Funktion, die es nicht gibt.

> ⚠️ **Ein Satz von damals gilt nicht mehr, und das ist eine Änderung und kein Versehen.** Hier
> stand: „Am Handy entfällt der Bereich ganz — dort ist jeder Pixel Breite vergeben." **Das galt
> für den leeren Platz.** Für den Haupteinstieg gilt es nicht: Eine Suche, die es am schmalen
> Fenster nicht gibt, ist keine. Unter 768 px ist das Feld deshalb eine **eigene, volle Zeile** der
> Kopfzeile — es konkurriert dort um keine Breite mehr, sondern kostet eine Zeile Höhe. Das ist
> dieselbe Abwägung, die §6 beim Umbruch der Kopfzeile selbst trifft. **Kein neuer Umbruchpunkt**:
> `md` ist der des Projekts.

### Die Marke — eine Gestalt, ein Ort

*Aufgenommen am 13.08.2026, als sie die zweite Ansicht bekam.*

Gedämpfte Fläche (`--muted`), kleiner Radius (`rounded-sm`), **kein Rahmen**, feste Laufweite für
den Wert. Sie entstand in Schritt 7, Teil 1 für die Belegdaten im Detail
([`bam-werte.md`](bam-werte.md) §11a) und trägt seit Teil 3 auch die Begriffe der Suche.

**Warum sie jetzt hier steht und vorher nicht.** §11a hielt ausdrücklich fest, dass eine Bauform an
genau einer Stelle nicht ins Konzept gehört — sonst wird es zur Sammelstelle —, und ebenso
ausdrücklich: *„Wenn die BAM-Suche in Teil 2 oder 3 dieselbe Marke braucht, wandert sie."* Sie
braucht sie. Der Code liegt in `components/marke.tsx`; **es gibt keine zweite Marken-Gestalt im
Projekt.**

**Die Bedienbarkeit ist ein Schalter und springt nicht per Voreinstellung an.** Das ist der Teil,
der hierher gehört und nicht in eine Ansicht:

| Ohne Schließen-Schaltfläche | Mit Schließen-Schaltfläche |
|---|---|
| eine **Anzeige** — kein Zeigerwechsel, kein Fokusrahmen, kein `title` | ein **Bedienelement**; bedienbar ist der Knopf *in* der Marke, nicht die Marke |
| so steht sie im Belegdaten-Block, und daran ändert sich nichts | so stehen die Begriffe der Suche |

**Keine Statusfarbe und keine neue Farbrolle.** Die Marke sagt nichts über einen Zustand (§3).

### Der Mandantenumschalter: Anzeige oder Bedienelement

Sind **mehrere** Mandanten wählbar, sieht er aus wie ein Bedienelement: Auswahlpfeil, Hover- und
Fokuszustand, Rahmen. Ist nur **einer** zulässig, bleibt er eine reine Anzeige ohne Klickversprechen.

**Begründung:** Wer genau einen Mandanten hat, kann nichts wechseln — eine Schaltfläche führte ihn
ins Leere. Entschieden wird über die tatsächlich zulässige Menge (`GET /api/mandanten`), nicht über
die Rolle. Solange die Liste lädt, bleibt es die Anzeige: Ein Bedienelement, das einen Moment
später erscheint, ist besser als eines, das wieder verschwindet.

---

## 6. Verhalten am kleinen Bildschirm

Unter 768 px:

- Die **Navigation** wird zur Schublade.
- Der **aktive Mandant bleibt in der Kopfzeile** — er wandert nicht ins Menü.
- Die Kopfzeile bricht dafür in zwei Zeilen um: oben Produktname und Nutzermenü, darunter Mandant
  und Sprache.
- **Seit dem 13.08.2026 kommt eine dritte Zeile dazu: das Belegnummern-Suchfeld** (§5). Es entfällt
  dort ausdrücklich **nicht** — es ist der Haupteinstieg des Werkzeugs, und einer, den es am
  schmalen Fenster nicht gibt, ist keiner. Der Preis ist dieselbe Art Preis wie beim Umbruch
  selbst: eine Zeile Höhe statt einer weggelassenen Funktion.

**Begründung für den Umbruch:** Bei 360 px passen Menüschalter, Produktname, Mandant,
Sprachumschaltung und Nutzermenü nicht nebeneinander, ohne dass etwas unleserlich wird. Der Umbruch
kostet 40 px Höhe; die Alternative wäre, etwas auszublenden — und der Mandant darf es nicht sein.

**Warum der Mandant nicht ins Menü darf:** Er steht in der Sitzung, nicht in der URL (siehe
[`mandantentrennung.md`](mandantentrennung.md) §1). Wäre er unsichtbar, zeigte eine Ansicht
unbemerkt einen anderen Ausschnitt — besonders bei ADMIN, der zwischen allen Mandanten wechseln
kann. Bei wenig Platz entfällt der Anzeigename, **nie** der Code.

Kein horizontales Scrollen bei 360 px — nachgemessen: `scrollWidth` ist dort exakt 360.

Die **Dichte** hängt an dieser Grenze ausdrücklich **nicht**. Sie folgt dem Zeigergerät (§5); ein
schmales Fenster am Rechner bleibt dicht. Was unter 768 px umbricht, ist die Anordnung, nicht die
Größe der Bedienflächen.

---

## 7. Was bewusst fehlt

- **Kein Dunkelmodus.** Der Block in `globals.css` steht nur, damit die shadcn-Komponenten
  vollständig bleiben; umgeschaltet wird nichts. Ein späterer Dunkelmodus entsteht an genau dieser
  Stelle.
- **Keine Animationen** außer denen, die shadcn/ui für Schublade und Menü mitbringt. Bewegung zieht
  Aufmerksamkeit, und die gehört den Daten.
- **Keine Farbe für „überfällig" und „unquittiert".** Diese beiden Problemkategorien bleiben laut
  Regel Q3 von „Fehler" getrennt, haben aber noch keine Ansicht. Sie brauchen ab Schritt 4
  beziehungsweise 10 eine eigene Rolle — und die darf **nicht** Rot sein, sonst verschmelzen die
  drei Kategorien in der Wahrnehmung, obwohl sie im Code getrennt bleiben. Das ist der einzige
  bekannte offene Punkt am Farbsystem.

  > **Nachgezogen am 31.08.2026 (Schritt 10b‑3a): Für *überfällig* gilt der Satz nicht mehr.** Die
  > Rolle `--ueberfaellig` steht in `globals.css`, in beiden Blöcken, mit drei gerechneten Werten —
  > die Herleitung samt vier Befunden in **§7a**. **Für *unquittiert* gilt er unverändert**, und
  > zwar aus einem stärkeren Grund als „noch keine Ansicht": Die Kategorie ist mit Entscheidung E‑d
  > vom 24.08.2026 aus dem MVP genommen und hat bis heute keine operative Definition
  > ([`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §4.2).
  >
  > **Der Satz oben bleibt trotzdem stehen.** Er trägt die Begründung — *die Rolle darf nicht Rot
  > sein* —, und die gilt für die entschiedene Farbe genauso wie für die offene. Gestrichen wäre sie
  > nirgends mehr nachlesbar.

---

## 7a. Offene Punkte

### „Überfällig" hat keine Farbrolle — ✔ **entschieden am 31.08.2026 (Schritt 10b‑3a)**

*Aufgenommen am 10.08.2026 im Nachtrag zu Schritt 5.*

Seit Schritt 5 zeigt das Nachrichtendetail die Problemkategorie *Überfällig* an — und **bewusst ohne
Farbe**: Zeichen, Wort und Schriftstärke gegen die gedämpfte Umgebung
([`nachrichtendetail.md`](nachrichtendetail.md) §10.4). Das war richtig: Rot gehört nach §3 und
Regel Q3 ausschließlich dem *Fehler*, und ein Status-Gelb gibt es in diesem Farbsystem nicht.

**Es ist aber keine getroffene Entscheidung, sondern eine vertagte** — und der Punkt gehört benannt,
solange er noch keinen Schaden anrichtet.

**Beißen wird sie im Dashboard.** Dort stehen die drei Problemkategorien nebeneinander. Trägt nur
eine davon Farbe, liest sich das als **Rangfolge** — und genau die schließt Regel Q3 aus, wo Fehler,
Überfällig und Unquittiert ausdrücklich getrennt und **gleichrangig** geführt werden. Eine
Kategorie ohne Farbe neben einer roten ist keine neutrale Darstellung, sie ist eine leisere.

| | |
|---|---|
| **Rahmen** | den §3 bereits gesetzt hat: **orange, Ton höchstens 85**. Zwischen 90 und 135 stünde die Rolle dem Akzent (`#b9c022`, Ton 112,4) zu nahe. Rot bei Ton 27 bleibt dem Fehler |
| **Umfang** | drei Werte wie bei jeder Rolle — Vordergrund, `-flaeche`, `-kontur`; sonst wird irgendwo ein vierter erfunden |
| **Nachzurechnen** | der Textkontrast, wie bei den vier Akzentstufen (§8). „Nachrechnen" ist wörtlich gemeint |
| **Zeitpunkt** | **spätestens beim Dashboard** (Schritt 10) und nicht später. Dort fällt sie ohnehin an — sie dann *nebenbei* zu treffen wäre der Fehler |
| **Bis dahin** | im Detail **Text und Zeichen**, keine Farbe. Das wird **nicht** geändert; „nie allein über Farbe" gilt ohnehin, und eine Rolle hier zu erfinden hieße, der Entscheidung vorzugreifen |

**Für „unquittiert" gilt dasselbe**, nur später: Die Kategorie hat bis heute keine Ansicht.

#### Der Abschluss — drei Werte je Block, jeder gerechnet *(31.08.2026)*

**Der Vermerk oben wird nicht gestrichen und nicht umgeschrieben.** Er ist der Rahmen, gegen den
gerechnet worden ist; ohne ihn wäre nicht nachlesbar, warum die Werte so und nicht anders ausfallen.
Vier Vorgaben standen dort, und alle vier sind eingelöst:

| Vorgabe von damals | Eingelöst |
|---|---|
| **Ton** orange, höchstens 85 | **80.** Fünf Grad Luft zur Grenze sind Absicht, nicht Rest |
| **Umfang** drei Werte | Vordergrund, `-flaeche`, `-kontur` — im hellen **und** im dunklen Block |
| **Nachzurechnen** | `scripts/farbrolle-ueberfaellig/rechne.mjs`. **Jede Zahl unten stammt aus seinem Lauf** |
| **Zeitpunkt** spätestens beim Dashboard | **vor** dem Dashboard-Frontend, in einem eigenen Schritt und nicht nebenbei |

##### Die Werte

| Token | hell | gemessen auf | dunkel | gemessen auf |
|---|---|---|---|---|
| `--ueberfaellig` | `oklch(0.52 0.105 80)` · `#886108` | **5,59 : 1** auf `--card`, **5,35 : 1** auf `--background`, **4,97 : 1** auf `--ueberfaellig-flaeche` | `oklch(0.7 0.14 80)` · `#cb9317` | **6,53 : 1** auf `--card`, **7,15 : 1** auf `--background`, **5,75 : 1** auf `--ueberfaellig-flaeche` |
| `--ueberfaellig-flaeche` | `oklch(0.96 0.028 80)` · `#fcf0dd` | blasse Tönung; L und Chroma sind die von `--status-fehler-flaeche` | `oklch(0.26 0.05 80)` · `#312103` | ebenso |
| `--ueberfaellig-kontur` | `oklch(0.65 0.13 80)` · `#b88513` | **3,29 : 1** auf `--card` (WCAG 1.4.11) | `oklch(0.53 0.105 80)` · `#8b640f` | **3,31 : 1** auf `--card` |

**Die Helligkeit ist in beiden Blöcken die von `--status-fehler`** (0.52 hell, 0.7 dunkel), und das
ist die eine gestalterische Entscheidung, die hier fällt: Regel Q3 führt Fehler und Überfällig
getrennt und **gleichrangig**. Zwei Kacheln nebeneinander, von denen eine dunkler steht, lesen sich
als Rangfolge. Unterschieden wird über den **Ton**, nicht über das Gewicht.

**Die Chroma bleibt knapp unter dem Rand des sRGB-Farbraums** — 0.105 bei einem Höchstwert von
0.1077, dunkel 0.14 bei 0.145. Auf den Rand gesetzt hätte ein anderer Rechenweg die Farbe
abgeschnitten; die zwei bis drei Prozent Abstand sind der Preis dafür, dass das nicht passiert.

**Die Rolle heißt nicht `--status-ueberfaellig`.** Ein Name mit diesem Präfix verspräche eine
Zuordnung über `MessageStatusKind`, und die gibt es nicht: *Überfällig* liegt **quer** zur
Einordnung — dieselbe `WARTEND`-Zeile kann überfällig sein oder nicht. Zugeordnet wird sie trotzdem
in derselben Datei wie die vier Statusrollen (`lib/status-farbe.ts`, zweite Tabelle
`PROBLEM_ZUORDNUNG`); zwei Dateien mit Farbzuordnung weichten §2 auf.

##### Die OKLab-Abstände — **berichtet, nicht als Bedingung geprüft**

Vergleichsmaß ist §3: Grün 166 hat zum Akzent **0,343**, der Akzent zu Rot **0,352**.

| Strecke | hell | dunkel |
|---|---|---|
| `--ueberfaellig` → `--status-fehler` (`#be2323`) | **0,152** | **0,141** |
| `--ueberfaellig` → `--akzent` (`#b9c022`) | 0,274 | **0,117** |
| `--ueberfaellig` → `--status-abgeschlossen` | 0,149 | 0,191 |
| *zum Vergleich:* `--status-abgeschlossen` → `--akzent` | 0,343 | 0,139 |
| *zum Vergleich:* `--akzent` → `--status-fehler` | 0,352 | 0,240 |
| *zum Vergleich:* `--status-abgeschlossen` → `--status-fehler` | 0,276 | 0,286 |
| die Flächen: `--ueberfaellig-flaeche` → `--status-fehler-flaeche` | 0,025 | 0,045 |
| *zum Vergleich:* `--status-abgeschlossen-flaeche` → `--status-fehler-flaeche` | 0,049 | 0,084 |

##### ⚠️ Befund 1: Der Abstand zu Rot ist 0,152 — **44 % des Vergleichsmaßes**

**Er ist nicht durch einen anderen Wert zu retten, und ein Ton über 85 kommt nicht in Frage.** Die
Rechnung dahinter ist kurz:

1. Ein Vordergrund, der **4,5 : 1 auf Weiß** trägt, ist auf etwa L ≤ 0.55 gedeckelt.
2. `--status-fehler` liegt bei L 0.52. Die Helligkeit trägt zum Abstand also **nichts** bei — anders
   als beim Vergleichsmaß, wo Grün (0.46) und Akzent (0.777) über 0,3 allein in L auseinanderliegen.
3. Was bleibt, ist die Tondifferenz. Sie ist bei Ton 80 gegen Rot bei Ton 27 **53 Grad**, und weil
   beide Farben bei dieser Helligkeit nur rund 0,11 bis 0,19 Chroma tragen können, wird daraus kein
   großer Weg.

Bei Ton 85 — der Obergrenze — wären es **0,161** statt 0,152. **Das rechtfertigt die Grenze nicht
auszureizen**: Der Gewinn ist ein Hundertstel, der Verlust wäre die Luft zum Akzent.

**Das Vergleichsmaß aus §3 ist für diese Strecke ohnehin das falsche.** Es misst einen
**Anwendungs**farbton gegen einen **Status**farbton. Die beiden bestehenden Statusfarben liegen
untereinander bei **0,276**, nicht bei 0,343 — und dieser Wert ist die ehrlichere Schranke. Auch an
ihr gemessen erreicht die neue Rolle nur **55 %**.

**Was daraus folgt, folgt nicht aus der Farbe.** Regel Q3 verlangt, dass die Kategorien getrennt
bleiben; §3 sagt, wie: **nie allein über Farbe**. Für zwei Kacheln, die nebeneinander stehen, heißt
das Wort und Zeichen an jeder von beiden — und das war ohnehin verbindlich, bevor diese Zahl bekannt
war. Die Farbe ist die halbe Aussage, und hier ist sie es messbar.

##### ⚠️ Befund 2: Zu Grün ist die Rolle **genauso weit weg wie zu Rot** (0,149)

Nicht erwartet und deshalb notiert. Der Grund ist derselbe: Alle drei sind dunkle, mäßig gesättigte
Vordergrundfarben und liegen zwangsläufig eng beieinander. **Praktisch harmlos** — Grün und Orange
sind über den Ton weit getrennt (166 gegen 80) und stehen nicht in derselben Kachelreihe. Es hält
nur fest, dass die 0,343 aus §3 zwischen zwei *Vordergrund*farben von niemandem erreichbar sind.

##### ⚠️ Befund 3: Im Dunkelblock ist der engere Nachbar der **Akzent** (0,117), nicht Rot

Strukturell: Der Akzent behält im Dunkelblock seinen hellen Wert (`#b9c022`, §3), und eine Schrift,
die auf dunklem Grund lesbar sein soll, muss selbst hell sein. Ein helles Orange und ein helles
Gelbgrün liegen dann nah beieinander.

**Das ist keine Eigenheit dieser Rolle.** Dieselbe Rechnung für den Bestand: `--status-abgeschlossen`
liegt im Dunkelblock **0,139** vom Akzent entfernt — dichter, als die neue Rolle an Rot liegt. **Der
Dunkelblock ist nie nachgerechnet worden**, und §7 sagt auch, warum: Er steht dort „nur, damit die
shadcn-Komponenten vollständig bleiben". Die drei neuen Werte sind trotzdem gerechnet und nicht
gespiegelt; sie halten alle Kontrastbedingungen gegen `--card` 0.21 und `--background` 0.16 ein.
**Wer den Dunkelmodus einschaltet, rechnet den ganzen Block nach — nicht nur diese Rolle.**

##### ⚠️ Befund 4: **Keine** der vier bestehenden Konturen erfüllt die 3 : 1 aus WCAG 1.4.11

Gemessen gegen `--card`:

| Kontur | auf `--card` | |
|---|---|---|
| `--status-fehler-kontur` `#fcc0b8` | 1,57 : 1 | verfehlt |
| `--status-abgeschlossen-kontur` `#b0d9c6` | 1,55 : 1 | verfehlt |
| `--status-offen-kontur` `#d4d4d4` | 1,48 : 1 | verfehlt |
| `--status-ungeklaert-kontur` `#dedede` | 1,35 : 1 | verfehlt |
| **`--ueberfaellig-kontur` `#b88513`** | **3,29 : 1** | **erfüllt** |

Die Bedingung stand im Auftrag zu diesem Schritt und ist eingehalten. **Der Preis ist sichtbar:** Die
neue Kontur ist bei L 0.65 deutlich dunkler als die vier bestehenden bei L 0.85 bis 0.90 und wird als
Plakettenrand kräftiger wirken. **Das wird hier nicht heimlich geglättet** — weder durch eine hellere
Kontur, die die Bedingung verfehlte, noch durch ein Nachdunkeln der vier anderen, das den Bestand
umfärbte. Die Ungleichheit ist der Befund. Wer sie auflöst, tut es für alle fünf Rollen zugleich und
rechnet dabei den ganzen Bestand nach.

##### Wie gerechnet worden ist — und die Gegenprobe dazu

**Der Kontrast wird aus den ungerundeten sRGB-Fließkommawerten gebildet**, nicht aus dem 8‑Bit-Hexwert.
Das ist die Methode, mit der die vier Akzentstufen in §3 gemessen worden sind: **Nur sie gibt deren
Zahlen wieder.** Deshalb rechnet das Skript sie zuerst nach und bricht ab, wenn eine abweicht —

> `akzent auf card 1,98` · `akzent-schrift auf card 5,40` · `auf background 5,18` ·
> `auf akzent-flaeche 4,91` · `akzent-vordergrund auf akzent 9,13` ·
> `status-abgeschlossen auf card 6,78` · `Grün–Akzent 0,3435` · `Akzent–Rot 0,3523`

— alle acht wiedergegeben, dazu die fünf Hexwerte aus §3. **Ohne diese Gegenprobe wäre die Methode
und nicht das Ergebnis die offene Frage.** Neben jedem neuen Kontrast steht zusätzlich der aus dem
gerundeten Hexwert; keiner der beiden liegt je auf der anderen Seite seiner Schwelle.

**Der OKLab-Abstand ist der euklidische Abstand in (L, a, b) einschließlich der Helligkeit** — auch
das ist die Methode aus §3 und keine andere.

**Unabhängig bestätigt vom Werkzeug selbst:** Lightning CSS schreibt beim Bauen zu jedem `oklch()`
einen Hex-Rückfall. Im gebauten CSS stehen `#886108`, `#fcf0dd`, `#b88513`, `#cb9317`, `#312103`,
`#8b640f` — dieselben sechs Werte, die das Skript ausrechnet, Ziffer für Ziffer.

##### Was hier ausdrücklich **nicht** geändert worden ist

**Das Nachrichtendetail bleibt farblos.** §10.4 dort zeigt *überfällig* über Zeichen, Wort und
Schriftstärke; die Rolle entsteht hier, ihre Verwendung dort ist eine Änderung an einer bestehenden,
abgenommenen Ansicht. Steht als **offener Punkt 88** unten.

**Für „unquittiert" ändert sich nichts.** Die Kategorie ist mit Entscheidung E‑d vom 24.08.2026 aus
dem MVP genommen und hat bis heute keine operative Definition. Eine Farbe dafür wäre eine Farbe für
nichts.

### Offener Punkt 88 — das Nachrichtendetail zieht die Farbe nicht nach

*Aufgenommen am 31.08.2026 (Schritt 10b‑3a). Projektweit höchste bis dahin vergebene Nummer: 87
([`dashboard.md`](dashboard.md) §11).*

| | |
|---|---|
| **88** | **Die Rolle `--ueberfaellig` ist gebaut, das Nachrichtendetail benutzt sie nicht.** [`nachrichtendetail.md`](nachrichtendetail.md) §10.4 hebt die Wartezeile bei `ueberfaellig` seit Schritt 5 **ohne jede Farbe** hervor — mit der ausdrücklichen Begründung, eine Rolle dort zu erfinden hieße, dieser Entscheidung vorzugreifen. Die Entscheidung ist jetzt gefallen, und damit ist die Begründung entfallen; die Darstellung bleibt trotzdem, wie sie ist. **Das ist Absicht und kein Vergessen:** Ein Schritt, der eine Farbe definiert, ändert keine abgenommene Ansicht mit. Wer sie nachzieht, tut es an der Ansicht und prüft dort, ob „Zeichen, Wort und Schriftstärke" **neben** der Farbe bestehen bleiben — nicht an ihrer Stelle (§3, „nie allein über Farbe") |

---

## 8. Wie man das Konzept ändert

| Änderung | Datei | Aufwand |
|---|---|---|
| Andere Akzentfarbe | `src/app/globals.css`, die vier Werte `--akzent*` | vier Zeilen, **nachrechnen** |
| Anderes Grün für „abgeschlossen" | `globals.css`, drei Werte `--status-abgeschlossen*` | drei Zeilen |
| Anderes Orange für „überfällig" | `globals.css`, drei Werte `--ueberfaellig*` je Block | sechs Zeilen, **nachrechnen** — `scripts/farbrolle-ueberfaellig/rechne.mjs` prüft die vier Bedingungen und berichtet die Abstände (§7a) |
| Status bekommt eine andere Rolle | `src/lib/status-farbe.ts`, Tabelle `ZUORDNUNG` | eine Zeile |
| Eine Problemkategorie bekommt eine andere Rolle | `src/lib/status-farbe.ts`, Tabelle `PROBLEM_ZUORDNUNG` | eine Zeile |
| Andere Stufenwerte des Dichteumschalters | `globals.css`, die vier `html[data-dichte="…"]` | vier Zeilen, **nachmessen** — `tests/dichte.test.ts` rechnet die Mindestfläche nach, aber ob die Nachrichtenliste, der Belegdaten-Block und der Recharts-Baum die neue Stufe tragen, sagt kein Test ([`dichte-umschalter.md`](dichte-umschalter.md) §5) |
| Dichtere oder luftigere Navigation | `globals.css`, `--dichte-navzeile` | eine Zeile |
| Dichtere oder luftigere Listen | `globals.css`, `--dichte-zeile` | eine Zeile |
| Breitere oder schmalere Beschriftungsspalte | `globals.css`, `--dichte-beschriftung` | eine Zeile, **im Panel nachsehen** |
| Schmalere oder breitere Navigationsspalte | `globals.css`, `--dichte-navspalte` | eine Zeile |
| Andere Schrift | `src/app/layout.tsx` (`next/font`) | zwei Zeilen |
| Dunkelmodus | `globals.css`, Block `.dark`, plus ein Umschalter | überschaubar |

Keine dieser Änderungen fasst eine Komponente an. Der Wechsel auf `#b9c022` ist der Beleg: Er hat
ausschließlich `globals.css` berührt.

**„Nachrechnen" ist wörtlich gemeint.** Ein Akzent braucht vier Stufen, und drei davon hängen an
gemessenen Kontrastwerten (§3). Wer nur `--akzent` austauscht, bekommt eine Anwendung, deren
Verweise und Fokusringe unlesbar sind — geschätzt wird das nicht, gerechnet schon.

---

## 9. Regelbezug

| Regel | Umsetzung |
|---|---|
| **Q3** Die drei Problemkategorien bleiben getrennt | Rot ist ausschließlich `FEHLER`. „Überfällig" hat seit dem 31.08.2026 die eigene Rolle `--ueberfaellig` (Ton 80), gerechnet und mit vier Befunden belegt in §7a. „Unquittiert" bekommt keine, solange die Kategorie nicht im MVP ist (E‑d). **Der Abstand zu Rot trägt die Trennung allein nicht** — 0,152 gegen 0,343 aus §3; sie hängt an „nie allein über Farbe" |
| **Q4** Nicht zugeordnet heißt nicht zugeordnet | eigene Rolle `--status-ungeklaert`, kein geratener Wert |
| Statusabbildung nur über den `MessageStatusClassifier` | `lib/status-farbe.ts` bildet nur die **Einordnung** auf Farbe ab, nie einen Rohwert |
| Status nie allein über Farbe | §3; als Kommentar an `lib/status-farbe.ts`, wo die Zuordnung entsteht |
