# Der Dichteumschalter — vier Stufen an der Wurzel

Entstanden am 01.09.2026. Beschreibt **warum** es einen Umschalter für die Anzeigegröße gibt, wo
seine Werte liegen, und was die Messung über die Stellen sagt, die **nicht** mitskalieren.

Das visuelle Konzept selbst steht in [`visuelles-konzept.md`](visuelles-konzept.md); die Stufen sind
dort in §4 und §5 eingetragen. Das Cookie-Muster steht in
[`frontend-grundlagen.md`](frontend-grundlagen.md) §4, neben dem der Sprache — es ist dasselbe.

---

## 1. Warum ein Umschalter und kein kleinerer Wert

Die Nachrichtenliste ist der Ort, an dem sich Dichte auszahlt: Wer einen Beleg sucht, überfliegt
viele Zeilen, und scrollen heißt den Überblick verlieren (`visuelles-konzept.md` §5). Die
naheliegende Antwort wäre gewesen, die Basisschrift einmal zu verkleinern.

**Die Frage „welche Größe ist richtig" hat aber keine Messung**, und sie ist je Nutzer eine andere:
Wer täglich acht Stunden Listen liest, will nicht die Dichte des externen Kundenmitarbeiters, der
zweimal im Monat einen Beleg sucht. Eine Zahl, die für beide gilt, gibt es nicht — also gibt es die
Wahl.

**Was sie einbringt, ist gemessen und steht in §5.3:** In der kleinsten Stufe sind bei 1080 px
Fensterhöhe **28 Zeilen** sichtbar statt 24, in der größten **19**.

---

## 2. Die Entscheidungen mit Datum

| | Entscheidung | Datum |
|---|---|---|
| **E‑w** | Ein **Umschalter mit vier Stufen**, kein fest verkleinerter Wert. Die richtige Größe ist nicht messbar und je Nutzer verschieden (§1) | 01.09.2026 |
| **E‑x** | Die Wahl liegt im **Cookie**, nicht in `localStorage`. Das Wurzel-Layout liest sie **serverseitig** und setzt sie vor dem ersten Paint; aus `localStorage` wäre sie erst nach der Hydratation zu haben und die Anwendung erschiene bei jedem Aufruf einmal in der falschen Größe. Der Nachrüstweg auf eine Spalte an `app_user` bleibt offen, genau wie bei der Sprache | 01.09.2026 |
| **E‑y** | Skaliert wird die **Wurzel**, nicht die drei Schriftrollen einzeln. Das Ziel ist mehr Zeilen je Bildschirm; dafür muss `--dichte-zeile` mitgehen, und die Abstände auch | 01.09.2026 |
| **E‑z** | **Die heutige Größe ist `m` und bleibt die Vorgabe.** Alle bisherigen Messungen des Projekts sind gegen diesen Zustand gemessen — die 10 rem aus [`bam-werte.md`](bam-werte.md) §11a, die Achsenbreite aus [`dashboard-frontend.md`](dashboard-frontend.md) §10.4, die 142 px für vier Navigationseinträge aus `visuelles-konzept.md` §5. Wäre er nicht mehr die Vorgabe, wären sie Messungen eines Zustands, den niemand mehr sieht. **Ob die Vorgabe später eine Stufe herunterwandert, ist eine eigene und spätere Entscheidung** | 01.09.2026 |

> ⚠️ **Die IDs weichen vom Auftrag ab, und das ist Absicht.** Der Auftrag schlug `E‑v` bis `E‑y`
> vor, ausdrücklich als Vorschlag und nicht als Prüfung. **`E‑v` ist bereits vergeben** —
> [`dashboard-frontend.md`](dashboard-frontend.md) §4 führt sie seit demselben Tag für den Deckel
> der Balkenbreite (`maxBarSize={28}`). Gezählt worden ist über alle Dateien in `docs/` und die
> Markdown-Dateien im Wurzelverzeichnis: Die Buchstabenreihe läuft `a`–`j`, `l`–`r`, `t`–`v`
> (`E‑k` und `E‑s` sind nie vergeben worden; die drei Treffer auf `E‑s` sind `(E‑schmal)` aus
> [`messungen-schritt6.md`](messungen-schritt6.md) und keine Entscheidung). Vergeben ist deshalb ab
> **`E‑w`** fortlaufend.
>
> **Damit ist die Buchstabenreihe aufgebraucht.** Wer die nächste Entscheidung dieser Art vergibt,
> braucht eine Fortsetzungsregel — sie steht bewusst nicht hier, weil sie das ganze Projekt betrifft
> und nicht diese Runde (offener Punkt **99**).

---

## 3. Die vier Stufen

Sie stehen in `src/app/globals.css` und nirgends sonst. Der Code kennt sie unter `xs`, `s`, `m`,
`l` (`src/dichte/index.ts`); wie sie in der Oberfläche heißen, ist eine Übersetzungsfrage (§4).

| Stufe | `--dichte-wurzel` | Wurzel | Basisschrift | `--dichte-zeile` | `--dichte-beruehrung` |
|---|---:|---:|---:|---:|---:|
| `xs` | 87,5 % | 14 px | 13,125 px | 31,5 px | **44 px** (Boden) |
| `s` | 93,75 % | 15 px | 14,0625 px | 33,75 px | **44 px** (Boden) |
| **`m`** | **100 %** | **16 px** | **15 px** | **36 px** | **44 px** |
| `l` | 112,5 % | 18 px | 16,875 px | 40,5 px | 49,5 px |

Alle Werte **gemessen** am laufenden System (`getComputedStyle` an `<html>` und `<body>`, die Tokens
über ein verborgenes Element mit `height: var(…)`), nicht gerechnet.

### Prozent und nicht Pixel

Eine feste Pixelangabe an der Wurzel überschriebe die Grundschriftgröße, die ein Nutzer im Browser
eingestellt hat. Wer sie hochgesetzt hat, hat einen Grund, und dieser Umschalter ist kein Anlass,
ihn zu übergehen. Mit Prozent bleibt die Nutzereinstellung die Basis und die Stufe wird darauf
gerechnet.

### Ein Attribut am Wurzelelement, keine Klasse an einem Wrapper

`html[data-dichte="xs"]` und so fort. **`rem` misst gegen das Wurzelelement** — ein Wrapper darunter
trüge nichts. Gesetzt wird das Attribut im Wurzel-Layout, serverseitig, aus dem Cookie.

### `--dichte-beruehrung` ist aus der Skalierung heraus

`visuelles-konzept.md` §5 sagt zu diesem Token: **„Wird nirgends unterschritten."** Skalierte es mit,
fiele es in `xs` auf 2,75 × 14 = **38,5 px**, und der Satz wäre nicht mehr wahr.

Es steht deshalb als `max(2.75rem, 44px)` da. Der Boden greift in den beiden kleinen Stufen; in `l`
(49,5 px) und bei hochgesetzter Browserschrift greift weiterhin der rem-Wert. **Ein festes `44px`
wäre die schlechtere Lösung** — es nähme dem Nutzer mit vergrößerter Grundschrift den Zuwachs.

> **Eine Nebenwirkung, die hierhergehört:** Bei einer Browservorgabe **unter** 16 px lieferte das
> Token bisher weniger als 44 px — bei 14 px Grundschrift 38,5. Das war eine stille Verletzung des
> Satzes aus §5, und sie ist mit dem `max()` behoben. Für diese Nutzer ist `m` also **nicht**
> pixelgleich zu vorher, und das ist die Korrektur eines Fehlers und keine Abweichung von E‑z.

---

## 4. Die Oberfläche

Vier Einträge im **Nutzermenü der Kopfzeile**, neben der Sprachwahl: Dort erwartet man
Einstellungen, die den Nutzer betreffen und nicht die Ansicht. Die Reihenfolge im Menü ist seither
**wer man ist → wie es aussehen soll → was man tun kann**; *Abmelden* bleibt der letzte Eintrag.

### Beschriftung: „Anzeigegröße", nicht „Dichte"

| Stufe | Deutsch | Englisch |
|---|---|---|
| Überschrift | Anzeigegröße | Display size |
| `xs` | Sehr klein | Very small |
| `s` | Klein | Small |
| `m` | Standard | Default |
| `l` | Groß | Large |

**Drei der vier Stufen nennen die Größe, die vierte nennt die Vorgabe.** Das ist Absicht: An dieser
Stelle ist *„hierhin komme ich zurück"* die nützlichere Auskunft als *„mittelgroß"*.

Und *„Dichte"* ist ein Wort für den Code. Der typische Nutzer ist kein EDI-Spezialist und erst recht
kein Gestalter; er liest „Anzeigegröße". Die Übersetzung ist die Stelle, an der solche Namen
auseinandergehen dürfen — die Texte stehen in `i18n/de.ts` und `en.ts`, **keine Zeichenkette in der
Komponente**.

### Ein Formular und keine Radiogruppe — obwohl beide Bausteine da sind

`components/ui/dropdown-menu.tsx` bringt `DropdownMenuRadioGroup` **und** `DropdownMenuRadioItem`
bereits mit. **Kein `shadcn add` war nötig, und keiner der beiden ist hier verwendbar:** Sie
schalten über `onValueChange` im Browser, und dieser Umschalter muss über eine **Server-Aktion**
schalten. Ein `asChild` um eine Absende-Schaltfläche scheidet aus, weil `RadioItem` seine Kinder
selbst zusammensetzt (Anzeigehäkchen plus `children`).

Nachgeholt wird davon genau **ein Attributpaar** — `role="menuitemradio"` und `aria-checked` —, und
das ist dieselbe Auszeichnung, die Radix an einem `RadioItem` selbst erzeugt. Die **Gestalt** kommt
unverändert aus dem Generatorbereich; in `components/ui` ist nichts geändert und nichts
nachinstalliert worden.

Der gemessene Baum im geöffneten Menü:

```
menu
├─ div            (Angemeldet als / admin / EDI-Betreuung)
├─ separator
├─ form[role=none]                     ← nimmt das Formular aus den Menürollen
│  ├─ div         (Anzeigegröße)
│  └─ div[role=group, aria-labelledby=…]
│     ├─ button[role=menuitemradio, name=dichte, value=xs]
│     ├─ button[role=menuitemradio, name=dichte, value=s]
│     ├─ button[role=menuitemradio, name=dichte, value=m]
│     └─ button[role=menuitemradio, name=dichte, value=l]
├─ separator
├─ a[role=menuitem]    (Passwort ändern)
└─ div[role=menuitem]  (Abmelden)
```

### Das Menü bleibt beim Umschalten offen

`onSelect` wird abgefangen. Das ist der Zweck des Bedienelements und keine Bequemlichkeit: Wer die
Anzeigegröße sucht, will sie **sehen** und dann entscheiden. Bliebe das Menü nicht stehen, müsste er
es für jeden Vergleich viermal neu öffnen — und die Wirkung tritt hinter dem Menü ein, nicht darin.

### Unbekannter Wert

Fällt auf `m`. **Nicht geraten und nicht der nächstliegenden Stufe zugeordnet** — aus `xxs` wird
nicht `xs`, aus `XS` auch nicht. Dieselbe Haltung wie Regel Q4.

---

## 5. Was gemessen worden ist *(01.09.2026)*

Gegen die Testkopie im Profil `dev`, Anker `2025-12-30 04:09:47`, angemeldet als `admin`, Mandant
**NEXANS**. Kopfloses Chrome ist nicht nötig gewesen — gemessen wurde über CDP an einem sichtbaren
Fenster, Fenstermaße über `Emulation.setDeviceMetricsOverride`
([`dashboard-frontend.md`](dashboard-frontend.md) §10.6).

**Alle vier Stufen sind geprüft worden. Keine ist ausgefallen.**

### 5.1 Der Recharts-Baum — kein Befund, und das ist die Auskunft

`dashboard-frontend.md` §5.3 nennt Pixelkonstanten, die von der Wurzelgröße nichts wissen:
Achsenbreite, `minTickGap={12}`, Diagrammhöhe 260, `maxBarSize={28}`, `fontSize: 11`. Die Erwartung
war, dass mindestens eine davon in `l` oder `xs` kippt.

**Sie kippt nicht, und der Grund ist messbar: der Diagrammbaum ist von der Wurzelschrift
vollständig entkoppelt.** Die y-Achse ist in allen vier Stufen **bitgleich**:

| | `xs` | `s` | `m` | `l` |
|---|---|---|---|---|
| Beschriftungen (NEXANS, 12M) | `0 · 55.000 · 110.000 · 165.000 · 220.000` | ebenso | ebenso | ebenso |
| Breite der Beschriftungen | 8 · 38,8 · 40,2 · 42,5 · 45,9 px | ebenso | ebenso | ebenso |
| `x` des Achsentexts | 52 | 52 | 52 | 52 |
| Schriftgröße der Achse | 11 px | 11 px | 11 px | 11 px |
| **kleinster Abstand zum Rand** | **6,8 px** | 6,7 px | 6,8 px | 6,8 px |

**Nichts wird abgeschnitten**, in keiner Stufe — nachgesehen an den Kästen der `<text>`-Knoten, wie
§5.3 es getan hat, und ausdrücklich an dem Fall, an dem die feste Zahl damals gescheitert ist
(`220.000` bei NEXANS über zwölf Monate). Die längste Beschriftung misst 45,9 px gegen 52 px Platz.

**Die x-Beschriftungen überlappen nirgends**, bei 48 Eimern (`48H`) und in jeder Stufe:

| Fensterbreite | `xs` | `s` | `m` | `l` |
|---:|---:|---:|---:|---:|
| 360 px | 5 (20,7 px) | 5 (20,1) | 5 (19,3) | 5 (17,7) |
| 768 px | 10 (16,0 px) | 10 (14,2) | 10 (12,3) | **8** (16,3) |
| 1500 px | 24 (17,5 px) | 24 (16,7) | 24 (16,0) | 24 (14,5) |

*(Zahl der beschrifteten Eimer, in Klammern der kleinste gemessene Abstand. Überlappungen: null, in
allen zwölf Fällen.)*

> **Die fette 8 ist der eigentliche Fund.** Bei 768 px und Stufe `l` **nimmt Recharts von sich aus
> eine Beschriftung weniger**, um `minTickGap={12}` zu halten. Genau der Mechanismus, der in §10.4
> die gerechnete Konstante `achsenabstand` abgelöst hat, trägt damit auch die Stufen — **ohne von
> ihnen zu wissen**. Eine Zahl, die von der Breite nichts weiß, kann bei zwei Breiten nicht richtig
> sein; eine, die von der Schriftgröße nichts weiß, ebenso wenig. `equidistantPreserveStart` weiß
> von beidem nichts und misst stattdessen nach.

Die übrigen drei Fragen aus dem Auftrag, alle in jeder Stufe und bei jeder der drei Breiten:

| Frage | Befund |
|---|---|
| Stehen Verlauf und Fehlerstreifen übereinander? | **ja** — Versatz des ersten Balkens **0,0 px** in allen 24 gemessenen Lagen, Achsenbreite in beiden Diagrammen identisch |
| Steht die Null der Hauptachse noch da? | **ja**, in allen 24 Lagen. Die 14 px Fußmarge aus §5.3 tragen unverändert |
| Wird waagerecht gescrollt? | **nein** — `scrollWidth === innerWidth` bei 360, 768 und 1500 px, in jeder Stufe |

**Es ist deshalb nichts geändert worden.** Der Auftrag sagt: *„Behebe nur, was du als Befund
gemessen hast."* Es gibt keinen. Der Weg über den Stufenfaktor als Parameter der reinen Funktion in
`features/dashboard/verlauf.ts` bleibt ungegangen und ist damit weiterhin der einzige zulässige,
falls je einer nötig wird — **niemals `getComputedStyle` zur Laufzeit**.

> **Was das nicht heißt.** Der Diagrammbaum *skaliert nicht mit*. In `l` wachsen Überschrift,
> Legende und Tooltip um 12,5 %, während Achsenschrift (11 px), Diagrammhöhe (260/88 px) und
> Balkenbreite (28 px) stehen bleiben; in `xs` umgekehrt. Das ist sichtbar und kein Defekt —
> geführt als offener Punkt **95**.

#### Belegvermerk zu §5.3 und §10.4 (Regel L10)

> **Gemessen war:** Bei `m` und 1500 px trägt die Zeitachse **24** Beschriftungen mit 16,0 px
> kleinstem Abstand, bei 768 px **10** mit 12,3 px, bei 360 px **5** mit 19,3 px.
>
> **Behauptet wird** in §10.4: 24 bei 1500 px (15 px), 10 bei 768 px (12 px), **6** bei 360 px
> (15 px).
>
> Zwei der drei Breiten stimmen. **Bei 360 px steht eine Beschriftung weniger da als dort
> vermerkt.** Die Ursache ist nicht gemessen; in Frage kommen der Eimerschnitt am verschobenen
> Anker und die Breite der senkrechten Bildlaufleiste. **Überlappt wird in beiden Zählungen
> nicht**, und die Aussage von §10.4 — die Dichte hängt an der Breite und nicht an einer Konstante
> — trägt unverändert. §10.4 ist deshalb **nicht** geändert worden.

#### Ein zweiter Belegvermerk: §5.3 widerspricht sich selbst

> **Gemessen war:** Die Achsenbreite ist gerechnet, nicht fest —
> `Math.max(48, Math.ceil((laengste + 1) * 6.2) + 10)` (`features/dashboard/verlauf.ts`), und bei
> NEXANS über zwölf Monate ergibt das den Achsentext bei `x = 52`, nicht bei 48.
>
> **Behauptet wird** in §5.3: *„Beide Diagramme haben dieselbe Achsenbreite (48 px)."* Zwölf Zeilen
> weiter, im selben Abschnitt, steht das Gegenteil: *„Die Breite der y-Achse folgt der längsten
> Beschriftung … Eine feste Zahl schnitt bei `NEXANS` über zwölf Monate `220.000` zu `:20.000` ab."*
>
> Der Code gibt der zweiten Stelle recht. Der erste Satz ist beim Umbau vom 01.09.2026 stehen
> geblieben. **Ein datierter Vermerk steht seit heute daneben**; gelöscht ist nichts.
>
> **Der Auftrag selbst trägt denselben Irrtum weiter** — er sagt, die 48 px seien *„gegen `220.000`
> in 15‑px‑Schrift gemessen worden"*. Beide Hälften stimmen nicht: Die 48 px sind an `220.000`
> **gescheitert**, und die Achsenschrift ist 11 px, nicht 15. Die 15 px im Dokument sind der
> kleinste Beschriftungsabstand aus der Tabelle in §10.4.

### 5.2 `--dichte-beschriftung` — das Verhältnis bleibt, und zwar gemessen

Geprüft im **Belegdaten-Block im Panel** (10 rem) und auf der eigenen Route über
`.beschriftung-breit` (16 rem), an Nachricht `ea1060ee-…f993e5` mit acht Belegdatengruppen.

**Bei 1920 px Fensterbreite und Stufe `m` reproduziert die Messung [`bam-werte.md`](bam-werte.md)
§11a auf den Pixel** — Gruppenzeile 454 px, 282 px bleiben dem Wert, die längste gemessene
Belegnummer braucht als Marke 285 px. Damit ist die Messstrecke geeicht und die Zahlen der anderen
Stufen sind mit ihr vergleichbar.

**Im Panel, 1920 px:**

| Stufe | Beschriftungsspalte | bleibt dem Wert | Gruppenzeile | Marke mit 35 Zeichen | Verhältnis Marke : Platz |
|---|---:|---:|---:|---:|---:|
| `xs` | 140 px | 246,5 px | 397 px | 249,3 px | 1,011 |
| `s` | 150 px | 264,25 px | 425,5 px | 267 px | 1,010 |
| **`m`** | **160 px** | **282 px** | **454 px** | **285 px** | **1,011** |
| `l` | 180 px | 317,5 px | 511 px | 320,5 px | 1,009 |

**Auf der eigenen Route, 1920 px:**

| Stufe | Beschriftungsspalte | bleibt dem Wert | Gruppenzeile | Marke mit 35 Zeichen | passt? |
|---|---:|---:|---:|---:|---|
| `xs` | 224 px | 750,5 px | 985 px | 249,3 px | ja |
| `s` | 240 px | 804,25 px | 1055,5 px | 267 px | ja |
| **`m`** | **256 px** | **858 px** | **1126 px** | **285 px** | ja |
| `l` | 288 px | 965,5 px | 1267 px | 320,5 px | ja |

**Die Erwartung ist der Befund**, und das ist hier ausnahmsweise die ganze Nachricht: Das Verhältnis
zwischen dem, was die Marke braucht, und dem, was ihr bleibt, ist über alle vier Stufen auf **0,2 %
konstant**. Jede beteiligte Größe skaliert mit demselben Faktor — die Spalte, weil sie in rem steht;
die Marke, weil ihre Schrift in rem steht.

Dazu, in jeder Stufe und an beiden Einhängepunkten:

- **Keine Marke bricht um.** Nicht eine, in keiner Stufe.
- **Im Panel brechen 5 der 8 Beschriftungen um, höchstens auf 2 Zeilen** — in jeder Stufe dieselben
  fünf. Genau die Bauabsicht aus §11a: Die Beschriftung kostet zwei Zeilen in ihrer *eigenen* Zelle,
  statt die Hauptinformation in den Umbruch zu zwingen.
- **Auf der eigenen Route bricht keine Beschriftung um** — 16 rem tragen sie in jeder Stufe einzeilig.

> **Der 3-Pixel-Rückstand im Panel ist geerbt und nicht neu.** Die längste Marke braucht 285 px, ihr
> bleiben 282 — das steht so schon in §11a. Der Umschalter macht ihn nicht größer: Er skaliert
> proportional mit (2,8 px in `xs`, 3,0 in `m`, 3,0 in `l`).

### 5.3 Die Nachrichtenliste — der Zweck dieser Runde

Bei **1500 × 1080 px**, Mandant NEXANS, 50 geladene Zeilen. Gezählt sind die Zeilen, die **ganz**
zwischen der Unterkante des Tabellenkopfes und der Unterkante des Scrollbereichs liegen.

| Stufe | Wurzel | Zeilenhöhe | Seitenkopf | Tabellenkopf | **sichtbare Zeilen** | gegen `m` |
|---|---:|---:|---:|---:|---:|---:|
| `xs` | 14 px | 31,5 px | 44,8 px | 28 px | **28** | **+4 (+16,7 %)** |
| `s` | 15 px | 33,75 px | 47,9 px | 30 px | **26** | +2 (+8,3 %) |
| **`m`** | **16 px** | **36 px** | **51 px** | **32 px** | **24** | ±0 |
| `l` | 18 px | 40,5 px | 57,3 px | 36 px | **19** | −5 (−20,8 %) |

**Das ist die Zahl, an der sich der Umbau messen lässt.** Vier Zeilen mehr sind eine Zeile mehr je
sechs — für jemanden, der eine Liste überfliegt, ist das der Unterschied zwischen einem Blick und
zwei.

> Der Seitenkopf von **51 px** in `m` bestätigt nebenbei `visuelles-konzept.md` §5 („die Kopfzeile
> 51 px statt 57"). Der Wert setzt sich aus `--dichte-kopfzeile` (3,125 rem = 50 px) und der
> Trennlinie (1 px) zusammen.

#### ⚠️ Befund: Die Tabellenkopfzeile hält **nicht** — und das gilt für alle vier Stufen

> **Gemessen war:** Der Tabellenkopf der Nachrichtenliste trägt `position: static`. Beim Scrollen
> des Inhaltsbereichs um 400 px wandert er um genau 400 px mit (von 180 px auf −220 px in `m`) und
> verschwindet. In **jeder** Stufe.
>
> **Behauptet wird** an zwei Stellen: `components/anwendungsrahmen.tsx` schreibt *„Ab Schritt 4
> sitzt in diesem Inhaltsbereich eine Nachrichtenliste mit **feststehender Tabellenkopfzeile**"*,
> und [`frontend-grundlagen.md`](frontend-grundlagen.md) §7 begründet den eigenen Scrollbereich
> damit, dass *„die Nachrichtenliste eine feststehende Tabellenkopfzeile über einem scrollenden
> Bereich"* braucht.
>
> **Es ist kein Befund dieser Runde.** `features/nachrichten/components/nachrichten-tabelle.tsx`
> hat nie eine klebende Kopfzeile gebaut; `benutzer-tabelle.tsx` und `katalog-tabelle.tsx` tun es
> (beide `sticky -top-4`), die Nachrichtenliste nicht. Der Dichteumschalter macht es weder besser
> noch schlechter — er hat es sichtbar gemacht, weil diese Runde zum ersten Mal danach gesehen hat.
>
> **Nicht behoben.** Es ist eine Entscheidung über die Nachrichtenliste und keine über die Dichte,
> und der Auftrag grenzt beides ausdrücklich ab. Geführt als offener Punkt **97**.

### 5.4 Das Berührungsgerät

Bei `pointer: coarse` (1024 × 1366, Berührungsemulation aktiv; `matchMedia("(pointer: coarse)")`
meldet in jeder Stufe `true`). Gemessen ist die **Layouthöhe** (`offsetHeight`).

| Token | `xs` | `s` | `m` | `l` |
|---|---:|---:|---:|---:|
| `--dichte-beruehrung` | **44** | **44** | **44** | 49,5 |
| `--dichte-bedienelement` | **44** | **44** | **44** | 49,5 |
| `--dichte-navzeile` | **44** | **44** | **44** | 49,5 |
| `--dichte-kopfzeile` | 49 | 52,5 | 56 | 63 |
| `--dichte-feld` | 35 | 37,5 | 40 | 45 |
| `--dichte-zeile` | 31,5 | 33,75 | 36 | 40,5 |
| **Eintrag des Umschalters** | **44** | **44** | **44** | **50** |

**Der Umschalter selbst erfüllt das Kriterium in jeder Stufe**, und mit ihm alles, was
`--dichte-beruehrung` oder `--dichte-bedienelement` trägt: Navigationseinträge,
Mandantenumschalter, Sprachwahl, Nutzermenü, sämtliche Menüeinträge.

#### ⚠️ Befund: Drei Klassen bleiben darunter — in **jeder** Stufe, `m` eingeschlossen

> **Gemessen war:** Von 80 sichtbaren Bedienelementen liegen 52 unter 44 px, und es sind in jeder
> Stufe dieselben drei Klassen.
>
> | Was | `xs` | `s` | `m` | `l` | Warum |
> |---|---:|---:|---:|---:|---|
> | Sortierknopf im Tabellenkopf („Zeitpunkt") | 18 | 19 | 20 | 23 | trägt **keine** `min-h`-Klasse |
> | jede Tabellenzeile (50 Stück) | 32 | 34 | 36 | 41 | `--dichte-zeile`, laut §5 für beide Zeigerarten 2,25 rem |
> | `Switch` aus `components/ui` | 18 | 18 | 18 | 18 | `h-[18.4px]` — feste Pixel im Generatorbereich, skaliert **gar nicht** |
> | *(`--dichte-feld`, kein Element in dieser Ansicht)* | 35 | 37,5 | 40 | 45 | Formularfeld, „bleibt bewusst komfortabel" |
>
> **Behauptet wird** in der Abnahme dieser Runde: *„am Berührungsgerät sind alle Bedienelemente in
> jeder Stufe mindestens 44 px hoch."* **Das Kriterium ist nicht erfüllt** — und es war vor dieser
> Runde ebenso wenig erfüllt: In `m` sind es 20, 36 und 18 px, also genau dieselben drei Klassen.
>
> **Nicht behoben, und der Grund ist E‑z.** Ein Boden an `--dichte-feld` (`max(2.5rem, 44px)`) hätte
> in `m` aus 40 px 44 px gemacht — eine Änderung am heutigen Zustand, und damit genau das, was E‑z
> ausschließt. Der Sortierknopf und die Zeilenhöhe sind Entscheidungen über die Nachrichtenliste,
> der `Switch` ist Generatorbereich (`visuelles-konzept.md` §2). Geführt als offener Punkt **96**.

### 5.5 Die Umschaltung selbst

Durchgeklickt am echten Menüeintrag, `m → xs → l → s → m`:

| | Befund |
|---|---|
| Wirkt ohne Neuladen von Hand | **ja**, in allen vier Übergängen. Die Zeilenhöhe folgt live: 36 → 31,5 → 40,5 → 33,75 → 36 px |
| Menü bleibt offen | **ja**, in allen vier Übergängen |
| `aria-checked` folgt | **ja** — genau ein Eintrag `true`, die drei anderen `false` |
| Häkchen folgt | **ja** — genau eines sichtbar, drei auf `invisible` |
| Adresse und Liste unberührt | **ja** — Pfad bleibt `/nachrichten`, die 50 geladenen Zeilen werden nicht neu geholt |
| Cookie gesetzt | **ja** — `overlord_dichte=<stufe>` nach jedem Klick |

**Kein Flackern beim nächsten Aufruf** — und das ist nicht angesehen, sondern am ausgelieferten
Dokument nachgewiesen. `data-dichte` steht im servergerenderten HTML an **Byte 31**, also im
`<html>`-Tag selbst, vor jedem Stylesheet und jedem Skript:

| Cookie | ausgeliefert |
|---|---|
| `xs` / `s` / `m` / `l` | `data-dichte="xs"` / `"s"` / `"m"` / `"l"` |
| `quatsch` | `data-dichte="m"` |
| *keins* | `data-dichte="m"` |

Es gibt keinen Zeitpunkt, zu dem der Browser eine andere Größe kennt als die endgültige. **Mit
`localStorage` gäbe es ihn bei jedem Aufruf** — das ist E‑x, belegt.

### 5.6 Pixelgleichheit in `m` — die härteste Bedingung

Die vollständige Liste der **echten** CSS-Änderungen dieser Runde (Kommentare herausgerechnet, aus
dem Diff gegen den Stand davor):

```
- --dichte-beruehrung: 2.75rem;
+ --dichte-wurzel: 100%;
+ --dichte-beruehrung: max(2.75rem, 44px);
+ html[data-dichte="xs"] { --dichte-wurzel: 87.5%;  }
+ html[data-dichte="s"]  { --dichte-wurzel: 93.75%; }
+ html[data-dichte="m"]  { --dichte-wurzel: 100%;   }
+ html[data-dichte="l"]  { --dichte-wurzel: 112.5%; }
+ html { font-size: var(--dichte-wurzel); }
```

Sechzehn Zeilen, und in `m` ist jede davon wirkungsgleich zum Zustand davor:

| Änderung | in `m` | vorher |
|---|---|---|
| `font-size: 100%` an `<html>` | die Grundschriftgröße des Browsers | keine Angabe, also `medium` — dieselbe Größe |
| `max(2.75rem, 44px)` | `max(44px, 44px)` = 44 px | `2.75rem` = 44 px |
| `html[data-dichte="m"]` | setzt `--dichte-wurzel: 100%`, den `:root`-Wert | — |

Gemessen bestätigt: Wurzel **16 px**, Basisschrift **15 px**, Zeilenhöhe **36 px**, Seitenkopf
**51 px**, `--dichte-beruehrung` **44 px**, Gruppenzeile im Panel **454 px** bei 1920 px, auf der
Route **1126 px**, y-Achsenbeschriftungen unverändert. Jede dieser Zahlen steht so schon in
`visuelles-konzept.md` §5 bzw. `bam-werte.md` §11a.

**Die eine Ausnahme steht in §3**: Bei einer Browservorgabe unter 16 px liefert
`--dichte-beruehrung` jetzt 44 px statt weniger. Das ist die Behebung einer stillen Verletzung von
§5 und keine Abweichung von E‑z.

### 5.7 Was **nicht** geprüft worden ist

Damit es dasteht und nicht fehlt:

- **Kein Bildschirmvergleich Pixel für Pixel** zwischen `m` und dem Stand davor. Belegt ist die
  Gleichheit über den CSS-Diff (§5.6) und über sechs nachgemessene Einzelwerte, **nicht** über einen
  Bildvergleich. Ein solcher hätte einen zweiten Entwicklungsserver auf dem Vorzustand gebraucht.
- **Nur ein Mandant.** Alles ist gegen **NEXANS** gemessen. Für §5.1 ist das der schärfste Fall (die
  sechsstellige Achsenbeschriftung), für §5.3 nicht notwendigerweise — die Zeilenhöhe hängt nicht am
  Mandanten, die Spaltenbreiten könnten es. Dieselbe Einschränkung, die
  [`dashboard.md`](dashboard.md) als offenen Punkt 85 führt.
- **Nur eine Nachricht für §5.2** (`ea1060ee-…f993e5`, acht Gruppen). Die längste Belegnummer des
  Bestands ist **nicht** an einer echten Nachricht gemessen, sondern als 35-Zeichen-Marke in der
  Gestalt der echten verborgen nachgemessen. Das ist die Zahl aus §11a und keine neue Messung.
- **Kein echtes Berührungsgerät.** `pointer: coarse` ist emuliert. Die Emulation meldet sich
  korrekt, die Media-Regel greift — was ein Finger auf einem Glas tatsächlich trifft, sagt das nicht.
- **Kein Vorleseprogramm.** Die Auszeichnung ist am Markup nachgesehen (`role="menuitemradio"`,
  `aria-checked`, `role="group"` mit `aria-labelledby`), nicht angehört.
- **Zwei Werkzeugbefunde, keine Anwendungsbefunde**, hier vermerkt, damit der nächste Lauf nicht
  wieder darauf hereinfällt: `Input.dispatchMouseEvent` über CDP öffnet das Radix-Menü **nicht**
  (`aria-expanded` bleibt `false`); synthetische `PointerEvent`s im Dokument öffnen es. Und
  `getBoundingClientRect()` misst am gerade geöffneten Menü die **Zoomanimation** mit — 44 × 0,95 =
  41,8 px sah wie ein Unterschreiten der Mindestfläche aus und war keines. Gemessen wird die
  Zielfläche mit `offsetHeight`.

---

## 6. Aufteilung des Codes

```
src/dichte/
├─ index.ts      vier Stufen, Cookie-Name, Dauer, definierter Rückfall
├─ server.ts     aktive Stufe für Server-Komponenten
├─ aktion.ts     Server-Aktion zum Umschalten
└─ provider.tsx  Kontext für Client-Komponenten (`useDichte`)

src/components/dichte-umschaltung.tsx   der Umschalter im Nutzermenü
src/app/globals.css                     die vier Stufen und der Boden
```

**Ein eigener Ordner neben `i18n/` und keine Datei in `lib/`**, und die Begründung ist die Naht und
nicht die Bequemlichkeit: Sprache und Dichte sind dieselbe Bauform — Eigenschaft des Nutzers,
Cookie, serverseitiger Leseweg, Server-Aktion zum Umschalten —, und `i18n/` ist genau deshalb ein
eigener Ordner. `lib/` ist Infrastruktur, und jedes Modul dort ist eine Datei ohne Serverhälfte.

Dazu ein harter Grund: **`"use server"` steht am Dateianfang und macht jeden Export zur
Server-Aktion.** Die Konstanten könnten also gar nicht neben der Aktion wohnen.

Der **Umschalter** liegt dagegen in `components/`, neben `sprachumschaltung.tsx` und aus demselben
Grund: `src/dichte/` hält die Daten und die Verdrahtung, das sichtbare Bedienelement ist
Zusammensetzung.

---

## 7. Tests

`frontend/tests/dichte.test.ts`, achtzehn Zusicherungen, **alle rechnerisch und ohne Ansicht**. Der
Test liest `globals.css` als Datei — dieselbe Bauform wie `tests/farbwerte.test.ts` und
`tests/serverbausteine.test.ts`; eine zweite Liste im Test wäre eine zweite Pflegestelle.

**Es steht keine abgeleitete Zahl in der Datei** — kein `38,5` und kein `49,5`. Ein kleiner
CSS-Rechner wertet die gelesene Deklaration aus (`rem`, `px`, `max()`), und was er nicht kennt,
lässt er fehlschlagen statt es zu überspringen.

| Was | Warum |
|---|---|
| Stufen im CSS == `DICHTESTUFEN` im Code, **in beide Richtungen** | Eine Stufe ohne Regel wäre ein Menüeintrag, der nichts tut; eine Regel ohne Stufe eine Größe, die niemand erreicht |
| `html { font-size: var(--dichte-wurzel) }` steht da | Ohne diese Zeile setzen vier Regeln einen Wert, den niemand liest |
| `m` ist genau 100 % | E‑z, die härteste Bedingung der Runde |
| die vier Prozentwerte steigen und sind verschieden | Eine Skala mit Richtung, die sie nicht einhält, liest sich als Zufall |
| `--dichte-beruehrung` ≥ 44 px in jeder Stufe, gegen **drei** Browservorgaben (12, 16, 20 px) | Der Boden muss auch bei kleiner gestellter Grundschrift tragen |
| **Gegenprobe:** ohne den Boden fiele `xs` darunter | Sonst stünde das `max()` da, ohne dass jemand wüsste, ob es etwas tut — und fiele beim nächsten Aufräumen weg |
| in `l` skaliert es weiterhin über 44 px hinaus | Ein festes `44px` nähme dem Nutzer mit vergrößerter Grundschrift den Zuwachs |
| in `m` ist es **exakt** 44 px | pixelgleich zu vorher |
| `dichteAus` fällt auf `m` — auch bei `XS`, `xxs`, `medium` | Regel Q4 in klein |
| beide Sprachdateien tragen alle vier Stufen, mit **vier verschiedenen** Texten | `tests/sprachdateien.test.ts` fängt Abweichungen, aber nur bei angelegten Schlüsseln |
| `--dichte-beruehrung` ist **genau einmal** deklariert | Eine zweite Deklaration gewänne je nach Reihenfolge, und der Test sähe die falsche |

**Der Test ist gegengeprüft worden**, nicht nur geschrieben: Wird das `max()` entfernt, fallen vier
Zusicherungen; wird die Stufe `m` aus dem CSS gelöscht, fallen fünf.

`tests/farbwerte.test.ts` hat **keine neue Ausnahme** bekommen und ist grün.

---

## 8. Regelbezug

| Regel | Umsetzung |
|---|---|
| **Q4** Nicht zugeordnet heißt nicht zugeordnet | `dichteAus` fällt auf `m` und ordnet **nicht** der nächstliegenden Stufe zu |
| Keine Zeichenkette in einer Komponente | Alle Beschriftungen in `i18n/de.ts` und `en.ts`; `dichte-umschaltung.tsx` enthält keine |
| Kein Gestaltungswert außerhalb von `globals.css` | Die Prozentwerte stehen **nur** dort. `src/dichte/index.ts` kennt die Namen der Stufen, nicht ihre Größen |
| Kein `getComputedStyle` zur Laufzeit für Gestaltung | Die aktive Stufe kommt aus dem Kontext, nicht aus dem Dokument |
| **L10** Belegvermerk *„gemessen war X / behauptet wird Y"* | §5.1 (zweimal), §5.3, §5.4 |
| Generatorbereich wird nicht von Hand umgebaut | `components/ui` ist unberührt; kein `shadcn add` war nötig |

---

## 9. Offene Punkte

*Offene Punkte setzen bei **93** an. Projektweit höchster vergebener Stand war **92**
([`dashboard-frontend.md`](dashboard-frontend.md) §9) — nachgezählt am 01.09.2026 über alle Dateien
in `docs/` und die Markdown-Dateien im Wurzelverzeichnis, in drei Schreibweisen (Tabellenzeile,
Listenform, Fließtext), und gegengeprüft.*

| Nr. | Punkt |
|---|---|
| **93** | **Es gibt keinen Test, der feste `px`-Schriftgrößen in eigenen Komponenten verbietet.** `tests/farbwerte.test.ts` tut das für Farbe; das Analogon für Größe fehlt. Ohne es driftet die Tokendisziplin bei Größen genauso auseinander, wie sie es bei Farbe täte. **Heute wäre der Test grün**: Außerhalb von `components/ui` gibt es im ganzen Projekt keine Tailwind-Klasse der Form `[Npx]` und keine feste `font-size`; das einzige Pixel außerhalb von Kommentaren in `globals.css` ist der Boden im `max()`. Genau deshalb ist jetzt der billige Zeitpunkt |
| **94** | **Die Stufe liegt nur im Cookie**, also je Gerät und Browser. Dieselbe Einschränkung, die [`frontend-grundlagen.md`](frontend-grundlagen.md) §4 für die Sprache bereits führt, und derselbe Nachrüstweg: eine Spalte an `app_user`, das Cookie wird zum Zwischenspeicher |
| **95** | **Der Recharts-Baum skaliert nicht mit.** Achsenschrift (11 px), Diagrammhöhe (260/88 px), Balkenbreite (28 px), `minTickGap` (12 px) und die Ränder sind Pixel und wissen von der Wurzelgröße nichts. **Gemessen ist, dass nichts kaputtgeht** (§5.1) — nichts wird abgeschnitten, nichts überlappt, die Balken stehen übereinander. Sichtbar ist es trotzdem: In `l` steht die Diagrammschrift still, während alles daneben wächst. Der Weg wäre der Stufenfaktor als Parameter der reinen Funktion in `verlauf.ts`, **niemals** `getComputedStyle` zur Laufzeit |
| **96** | **Drei Klassen von Bedienelementen bleiben am Berührungsgerät unter 44 px, in jeder Stufe einschließlich `m`** (§5.4): der Sortierknopf im Tabellenkopf (20 px in `m`), jede Tabellenzeile (36 px) und der `Switch` aus dem Generatorbereich (18,4 px, feste Pixel). Dazu `--dichte-feld` mit 40 px. **Alle vier waren es vorher auch.** Ein Boden an `--dichte-feld` scheidet aus, solange E‑z gilt — er änderte `m`. Es ist eine Entscheidung über die Nachrichtenliste und über den gemeinsamen Baustein, nicht über die Dichte |
| **97** | **Die Tabellenkopfzeile der Nachrichtenliste hält nicht** (§5.3). `position: static`, sie scrollt vollständig weg — in jeder Stufe und schon vorher. `components/anwendungsrahmen.tsx` und [`frontend-grundlagen.md`](frontend-grundlagen.md) §7 behaupten beide das Gegenteil. `benutzer-tabelle.tsx` und `katalog-tabelle.tsx` machen es vor (`sticky -top-4`) |
| **98** | **Ohne JavaScript ist der Umschalter nicht erreichbar.** Der *Weg* braucht keins — es ist ein Formular mit Server-Aktion, genau wie die Sprachumschaltung. Nur öffnet sich das Nutzermenü darüber ohne JavaScript nicht. Die Sprachwahl steht frei in der Kopfzeile und hat das Problem nicht. Der Nebeneffekt aus E‑x ist damit für dieses Bedienelement **nicht eingelöst**, und das steht hier, statt es zu behaupten |
| **99** | **Die Buchstabenreihe der Entscheidungs-IDs ist mit `E‑z` aufgebraucht** (§2). Es gibt keine Fortsetzungsregel. Sie zu erfinden betrifft das ganze Projekt und nicht diese Runde; [`README.md`](README.md) („Die Nummernkreise") ist der Ort dafür |
