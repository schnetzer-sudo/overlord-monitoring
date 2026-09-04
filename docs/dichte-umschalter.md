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

Der Recharts-Baum steckt voller Pixelkonstanten, die von der Wurzelgröße nichts wissen.
[`dashboard-frontend.md`](dashboard-frontend.md) §5.3 nennt davon die **Achsenbreite**, den
`minTickGap={12}` und die **Diagrammhöhe 260 px**; `maxBarSize={28}` steht in §5.2 und
`fontSize: 11` nur im Code (`features/dashboard/components/verlauf-diagramm.tsx`). Die Erwartung
war, dass mindestens eine davon in `l` oder `xs` kippt.

**Sie kippt nicht, und der Grund ist messbar: der Diagrammbaum ist von der Wurzelschrift
vollständig entkoppelt.** Die y-Achse ist in allen vier Stufen **maßgleich** — dieselben
Beschriftungen, dieselben Kastenbreiten, dieselbe Achsenlage:

| | `xs` | `s` | `m` | `l` |
|---|---|---|---|---|
| Beschriftungen (NEXANS, 12M) | `0 · 55.000 · 110.000 · 165.000 · 220.000` | ebenso | ebenso | ebenso |
| Breite der Beschriftungen | 8 · 38,8 · 40,2 · 42,5 · 45,9 px | ebenso | ebenso | ebenso |
| `x` des Achsentexts | 52 | 52 | 52 | 52 |
| Schriftgröße der Achse | 11 px | 11 px | 11 px | 11 px |
| **kleinster Abstand zum Rand** | **6,8 px** | 6,7 px | 6,8 px | 6,8 px |

*(Die 6,7 px in `s` sind die einzige Abweichung der ganzen Tabelle und liegen bei einem Zehntel
Pixel — Rundung des Textkastens, nicht Wirkung der Stufe. „Maßgleich" ist deshalb der richtige
Ausdruck und „bitgleich" wäre der falsche.)*

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
> zwei Beschriftungen weniger** — 8 statt 10 —, um `minTickGap={12}` zu halten. Genau der
> Mechanismus, der in [`dashboard-frontend.md`](dashboard-frontend.md) §10.4 die gerechnete
> Konstante `achsenabstand` abgelöst hat, trägt damit auch die Stufen — **ohne von ihnen zu
> wissen**. Eine Zahl, die von der Breite nichts weiß, kann bei zwei Breiten nicht richtig
> sein; eine, die von der Schriftgröße nichts weiß, ebenso wenig. `equidistantPreserveStart` weiß
> von beidem nichts und misst stattdessen nach.

Die übrigen drei Fragen aus dem Auftrag, alle in jeder Stufe und bei jeder der drei Breiten:

| Frage | Befund |
|---|---|
| Stehen Verlauf und Fehlerstreifen übereinander? | **ja** — Versatz des ersten Balkens **0,0 px** in allen 24 gemessenen Lagen, Achsenbreite in beiden Diagrammen identisch |
| Steht die Null der Hauptachse noch da? | **ja**, in allen 24 Lagen. Die 14 px Fußmarge aus §5.3 tragen unverändert |
| Wird waagerecht gescrollt? | **nein** — bei 360, 768 und 1500 px, in jeder Stufe. **Der ursprüngliche Test dafür war eine Tautologie**: `scrollWidth === innerWidth` kann wegen `html { overflow-x: hidden }` (`globals.css`) gar nicht fehlschlagen. Nachgehalten wird er von der elementweisen Prüfung gegen `window.innerWidth` in der Gegenprüfung — **kein Element steht über dem Rand**, in 204 Lagen |

> ### ⚠️ Die erste Zeile ist seit dem 04.09.2026 eine Aussage über **zwei Balkendiagramme**
>
> Sie ist für ihren Gegenstand richtig und bleibt stehen. Der Gegenstand hat sich geändert: Der
> Verlauf ist seither **eine Fläche** (**E‑83**,
> [`dashboard-frontend.md`](dashboard-frontend.md) §5.2), und einen „ersten Balken" gibt es dort
> nicht mehr.
>
> **Was an die Stelle tritt, in neun Lagen nachgemessen:** Die Achsenbreite ist in beiden
> Diagrammen unverändert identisch; die Stützstellen der Fläche liegen auf **0,0005 px** genau auf
> der gemeinsamen Zeitachse; die **Balkenmitte** des Streifens liegt bis zu **0,21 px** daneben,
> weil Recharts die Balkenbreite auf eine ganze Zahl rundet. Das galt für den Balken auch vorher —
> nur hoben sich damals zwei gleich gerundete Balken gegeneinander auf.
> ([`frontend-grundlagen.md`](frontend-grundlagen.md) §8b.)

**Es ist deshalb nichts geändert worden.** Der Auftrag sagt: *„Behebe nur, was du als Befund
gemessen hast."* Es gibt keinen. Der Weg über den Stufenfaktor als Parameter der reinen Funktion in
`features/dashboard/verlauf.ts` bleibt ungegangen und ist damit weiterhin der einzige zulässige,
falls je einer nötig wird — **niemals `getComputedStyle` zur Laufzeit**.

> **Was das nicht heißt.** Der Diagrammbaum *skaliert nicht mit*. In `l` wachsen Überschrift,
> Legende und Tooltip um 12,5 %, während Achsenschrift (11 px), Diagrammhöhe (260/88 px) und
> Balkenbreite (28 px) stehen bleiben; in `xs` umgekehrt. Das ist sichtbar und kein Defekt —
> geführt als offener Punkt **95**.

#### Nachgeprüft, breiter als der Auftrag verlangt hat

Die vier Aussagen oben sind gegen **204 frisch geladene Lagen** gehalten worden: drei Zeiträume
(48 / 30 / 12 Eimer) × sieben Breiten (360 bis 2560 px) × vier Stufen, dazu ein **zweiter Mandant**
(`SUTTONS`, dessen Größenordnungen vier Zehnerpotenzen unter `NEXANS` liegen) und die englische
Sprachfassung. Überall dasselbe: **0 beschnittene Texte, 0 Überlappungen**, kleinster x-Abstand
**exakt 12,0 px** — also der `minTickGap`, den `equidistantPreserveStart` hält und nie unterschreitet.

Dabei springt die gerechnete Achsenbreite sichtbar mit den Daten (Achsentext bei `x = 40` für fünf
Zeichen, 46 für sechs, 52 für sieben) — sie folgt also wirklich der längsten Beschriftung und keiner
Konstante.

**Der Melder ist gegengeprobt**, damit die Nullen keine blinden Nullen sind: Zwingt man die
Achsenschrift per `!important` auf 16 px, meldet derselbe Melder sofort `2.400` und `3.200` als um
4,2 bzw. 4,3 px beschnitten; bei 20 px überlappen bis zu 22 x-Beschriftungen.

**Drei Vorbehalte, alle gemessen:**

| | |
|---|---|
| **Der Tooltip in `l` bei 360 px hat seine Reserve aufgebraucht** | Er skaliert mit der Wurzelschrift, das Diagramm darunter wird mit steigender Stufe schmaler. Bei 360 px und `12M`: `xs` 184,7 px breit mit 65,3 px Luft zum Kartenrand, `m` 211,1 / 33,9, **`l` 237,5 px gegen 282 px Diagrammbreite — 15,5 px über den Diagrammkasten hinaus und 2,5 px vor dem Kartenrand.** Abgeschnitten wird nichts. Es ist die **einzige** Zahl der ganzen Messreihe, die sich mit steigender Dichte auf null zubewegt; ein längerer Rollenname, eine fünfte Stufe oder eine Stelle mehr im Eimer kippt sie |
| **Ein latenter Fehler an der Millionengrenze** | `achsenbreite` gibt *ein* Zeichen Zuschlag, weil Recharts die oberste Marke aufrundet. An jeder Zehnerpotenz, an der ein **Tausenderpunkt dazukommt**, sind es zwei: aus 999.999 (7 Zeichen, 60 px Achse, 52 px Text) wird die Marke `1.000.000`, und die misst **53,55 px** — 1,55 px zu breit. Genau der Fehler, gegen den die Funktion geschrieben wurde. **Mit keinem der zehn Mandanten der Testkopie erreichbar** (größter Eimerwert: 220.000), deshalb kein Befund — und er hängt **nicht** an der Dichtestufe, er träfe alle vier gleich |
| **Alles hängt daran, dass die Achsenschrift 11 px bleibt** | Eine im Browser eingestellte **Mindestschriftgröße** überlebt das feste `fontSize: 11` nicht. Bei erzwungenen 16 px fallen drei der vier Zusicherungen sofort. „Kein Befund" ist die Auskunft über einen Baum, der von der **Wurzelschrift** entkoppelt ist — nicht die Auskunft, dass er gegen größere Schrift robust wäre |

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
`.beschriftung-breit` (16 rem), an Nachricht `ea1060ee-…f993e5` mit **sieben** Belegdatengruppen.

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

**Im Panel bei 1500 px** — die zweite mitgemessene Breite, und die unbequemere:

| Stufe | Beschriftungsspalte | bleibt dem Wert | Gruppenzeile | Marke mit 35 Zeichen | Rückstand |
|---|---:|---:|---:|---:|---:|
| `xs` | 140 px | 190,5 px | 341 px | 249,3 px | **58,8 px** |
| `s` | 150 px | 204,25 px | 365,5 px | 267 px | 62,8 px |
| **`m`** | **160 px** | **218 px** | **390 px** | **285 px** | **67,0 px** |
| `l` | 180 px | 245,5 px | 439 px | 320,5 px | 75,0 px |

> **Der Rückstand hängt an der Fensterbreite, nicht an der Stufe.** Bei 1920 px sind es 3 px, bei
> 1500 px sind es 67 — das Panel ist schmaler, die Marke bleibt gleich lang. Auch das ist geerbt:
> `bam-werte.md` §11a rechnet mit der Gruppenzeile von 454 px, und die gibt es erst ab etwa
> 1900 px Fensterbreite. **Die Stufe verändert daran nichts** — der Rückstand skaliert mit
> demselben Faktor wie alles andere (58,8 / 67,0 = 0,877 gegen den Stufenfaktor 0,875).

**Die Erwartung ist der Befund**, und das ist hier ausnahmsweise die ganze Nachricht: Das Verhältnis
zwischen dem, was die Marke braucht, und dem, was ihr bleibt, ist über alle vier Stufen auf **0,2 %
konstant**. Jede beteiligte Größe skaliert mit demselben Faktor — die Spalte, weil sie in rem steht;
die Marke, weil ihre Schrift in rem steht.

Dazu, in jeder Stufe und an beiden Einhängepunkten:

- **Keine Marke bricht um.** Nicht eine, in keiner Stufe.
- **Im Panel brechen 5 der 7 Beschriftungen um, höchstens auf 2 Zeilen** — in jeder Stufe dieselben
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

> ### ⚠️ Und die Gegenzahl aus einer anderen Ansicht: **im Prozessbaum bewegt der Umschalter fast
> nichts** *(02.09.2026)*
>
> Die Baumzeile der Prozessansicht hält `--dichte-beruehrung` und nicht `--dichte-zeile` — sie ist
> ein Bedienziel und keine Zeile Daten ([`process-view.md`](process-view.md) E‑51). Damit greift in
> `xs`, `s` und `m` überall der Boden von 44 px, und nur `l` rechnet darüber hinaus:
>
> | Stufe | Partnerzeile im Baum | Tabellenzeile der Liste |
> |---|---:|---:|
> | `xs` | **44 px** | 31,5 px |
> | `s` | **44 px** | 33,75 px |
> | `m` | **44 px** | 36 px |
> | `l` | **50 px** | 40,5 px |
>
> **Das ist kein Fehler, sondern der Preis für den Satz in §3:** „Wird nirgends unterschritten."
> Wo eine Ansicht ihn einhält, hört der Dichteumschalter auf zu wirken — drei gleiche Stufen und
> eine, die weniger zeigt.
>
> **Der Zusammenhang ist derselbe wie in §5.4 und in offenem Punkt 96**: Was das Token trägt, hält
> das Maß; was es nicht trägt (die Tabellenzeile), hielt es auch vorher nicht. Neu ist nur, dass
> jetzt eine ganze Ansicht auf der einen Seite dieser Grenze steht. Geführt als offener Punkt 117 in
> [`process-view.md`](process-view.md) §13.

#### ⚠️ Befund: In `l` fehlt eine Zeile mehr, als die Skalierung erklärt

Die drei kleineren Stufen skalieren **exakt** proportional. Der Kopf über der ersten Datenzeile
misst 185,8 px in `xs`, 198,9 in `s`, 212,0 in `m` — bei einem Faktor von 0,875 bzw. 0,9375 wären
185,5 und 198,8 zu erwarten. Abweichung: **drei Zehntel Pixel.**

**In `l` sind es 283,3 px statt der proportionalen 238,5 — ein Überschuss von 44,8 px**, und der
kostet genau die fünfte Zeile: ohne ihn wären es `(1080 − 238,5) / 40,5 = 20,8`, also 20 Zeilen
statt der gemessenen 19.

**Woher er kommt, ist nachgemessen und nicht geraten.** Der Überschuss sitzt in **einem** Element,
der Filterleiste über der Tabelle (`flex flex-wrap items-start gap-2`):

| | `m` | `l` | proportional wäre |
|---|---:|---:|---:|
| Filterleiste | 54,0 px | **105,8 px** | 60,8 px |
| alles darüber und darunter | proportional | proportional | — |

Sie ist `flex-wrap`, ihr Inhalt wächst um 12,5 %, und bei 1500 px passt er dann nicht mehr in eine
Zeile: Sie **bricht um** und wird fast doppelt so hoch.

#### Und der Grund dahinter: die Umbruchpunkte gehen nicht mit

**`rem` in einer Media Query misst gegen die Anfangsschriftgröße, nicht gegen das Wurzelelement.**
`--dichte-wurzel` verschiebt deshalb **keinen einzigen** Umbruchpunkt. Gemessen bei 1200 px und bei
1279 px, in `xs`, `m` und `l`: `sm`/`md`/`lg` treffen zu, `xl` nicht — **in allen drei Stufen
identisch**, während der Inhalt daneben um 28 % auseinanderliegt.

Das ist keine Eigenheit dieses Projekts, sondern die Definition von `rem` in einer Media Query, und
es ist die **eine** Stelle, an der der Satz „alles skaliert mit" nicht gilt. Für den Rahmen heißt
es: Wo eine Zeile *knapp* passt, kann sie eine Stufe höher umbrechen, ohne dass der Umbruchpunkt
sich bewegt. Gefunden ist genau ein solcher Fall (die Filterleiste bei 1500 px); **systematisch
abgesucht ist der Rahmen nicht** — geführt als offener Punkt 95.

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
| `--dichte-bedienzeile` *(ab 02.09.2026)* | **44** | **44** | **44** | 49,5 |
| **Eintrag des Umschalters** | **44** | **44** | **44** | **50** |

**Der Umschalter selbst erfüllt das Kriterium in jeder Stufe**, und mit ihm alles, was
`--dichte-beruehrung` oder `--dichte-bedienelement` trägt: Navigationseinträge,
Mandantenumschalter, Sprachwahl, Nutzermenü, sämtliche Menüeinträge.

> ### `--dichte-bedienzeile` ist am 02.09.2026 dazugekommen — und ändert an Punkt 96 nichts
>
> Baumzeile und Auswahlzeile trugen bis dahin `--dichte-beruehrung` und waren damit in `xs`, `s`
> und `m` gleich hoch; der Umschalter bewegte im Prozessbaum drei Zeilen über die ganze Skala
> ([`process-view.md`](process-view.md) §16, M121). Seither steht das neue Token am Zeigergerät auf
> `--dichte-zeile` und **fällt hier auf die Fläche zurück** — die Zeile darüber in der Tabelle zeigt
> es. Nachgemessen als M127 ([`process-view.md`](process-view.md) §24), mit **derselben
> Einschränkung wie überall in diesem Abschnitt**: gemessen ist die Layouthöhe, nicht die Fläche.
>
> **Offener Punkt 96 bleibt unberührt**, in beide Richtungen: `--dichte-beruehrung` ist nicht
> angefasst worden, und die drei Klassen, die dort unter 44 px bleiben, sind dieselben. Die
> **Tabellenzeile** (`--dichte-zeile`, 36 px) bleibt ausdrücklich darunter — sie ist eine Zeile
> Daten und kein Ziel, und genau diese Unterscheidung trägt das neue Token im Namen.

> ⚠️ **Und zwar in der HÖHE. Eine Fläche von 44 × 44 ist das nicht**, und der Unterschied gehört
> hierher, weil er leicht überlesen wird. `min-h-beruehrung` hebt die Höhe an und sagt über die
> Breite nichts. Ein Symbolknopf trägt daneben `size-8` (aus `components/ui/button.tsx`,
> `size="icon"`), und `tailwind-merge` löst das nicht auf — beide Klassen bleiben stehen.
>
> Gemessen am Berührungsgerät, Breite × Höhe: **28 × 44** in `xs`, 30 × 44 in `s`, 32 × 44 in `m`,
> 36 × 50 in `l`. Dasselbe gilt für die Sprachumschaltung (35–45 px breit) und die
> Belegart-Auswahl (30–38 px). Auf `/administration/katalog` sind es 737 von 747 Trägern.
>
> **Das Abnahmekriterium spricht von „mindestens 44 px hoch", und in dieser Lesart ist es für
> diese Elemente erfüllt.** Als Zusicherung einer Berührungsfläche wäre es das in keiner Stufe —
> auch nicht in `m`, auch nicht vor dieser Runde.

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
| Adresse unberührt | **ja** — der Pfad bleibt `/nachrichten`, kein Parameter kommt hinzu |
| Liste bleibt stehen | **ja**, im Sinne von: nach jedem Übergang stehen wieder **50** Zeilen im Baum, und der Ladezustand erscheint nicht. **Ob die Liste dabei neu geholt wird, ist nicht gemessen** — dafür wäre ein Netzmitschnitt nötig, und der Zähler im Messskript war ein Blindgänger |
| Cookie gesetzt | **ja** — `overlord_dichte=<stufe>` nach jedem Klick |
| Cookie-Eigenschaften | nach echtem Klick über `Network.getCookies` abgelesen: **365,00 Tage**, `SameSite=Lax`, `httpOnly: false`, `path: "/"` — die vier Zusagen aus Teil 2, gemessen statt behauptet |

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
Route **1126 px**, y-Achsenbeschriftungen unverändert.

**Vier dieser Werte stehen so schon im Projekt und sind damit Wiederholungen einer fremden
Messung, keine neue Behauptung:** der Seitenkopf mit 51 px (`visuelles-konzept.md` §5), die
Gruppenzeile mit 454 px, die 282 px für den Wert und die 1.126 px auf der Route
([`bam-werte.md`](bam-werte.md) §11a). Wurzel, Basisschrift und Zeilenhöhe sind hier zum ersten
Mal in Pixeln gemessen; `visuelles-konzept.md` §4 nennt die Basis als 15 px, die Zeilenhöhe steht
dort nur als `2.25 rem`.

**Zwei Ausnahmen, und beide sind benannt statt weggelassen:**

1. **Bei einer Browservorgabe unter 16 px** liefert `--dichte-beruehrung` jetzt 44 px statt
   weniger — bei Chromes Stufe 12 px waren es vorher 33 px, bei 9 px 24,75 px. Nachgemessen auf
   `/passwort`: Der Absendeknopf springt von 33 auf 44 px, die Karte wird 11 px höher. Über eine
   ganze Route gerechnet sind bei erzwungenen 12 px **15.663 von 15.776 Kästen** verschoben; bei
   16 px sind es **null**. Das ist die Behebung einer stillen Verletzung von §5 und keine
   Abweichung von E‑z — aber es ist eine Änderung, und sie steht deshalb hier.
2. **Das geöffnete Nutzermenü ist 152,95 px höher** (286,90 statt 133,95 px bei 1280 × 900). Das
   ist die neue Funktion selbst; „pixelgleich" ist trotzdem eine absolute Aussage, und ein
   geöffnetes Menü gehört zur Oberfläche.

**Was darüber hinaus aktiv gesucht und nicht gefunden wurde:** `html { font-size: 100% }` gegen
gar keine Angabe ergibt über sechs Routen **null** verschobene Kästen und im verschränkten
Bildvergleich (A/B/A/B/A/B auf `/passwort`, 1280 × 800) **null** dauerhaft abweichende Pixel.

### 5.7 Was **nicht** geprüft worden ist

Damit es dasteht und nicht fehlt:

- **Kein Bildschirmvergleich Pixel für Pixel** zwischen `m` und dem Stand davor. Belegt ist die
  Gleichheit über den CSS-Diff (§5.6) und über sechs nachgemessene Einzelwerte, **nicht** über einen
  Bildvergleich. Ein solcher hätte einen zweiten Entwicklungsserver auf dem Vorzustand gebraucht.
- **Nur ein Mandant.** Alles ist gegen **NEXANS** gemessen. Für §5.1 ist das der schärfste Fall (die
  sechsstellige Achsenbeschriftung), für §5.3 nicht notwendigerweise — die Zeilenhöhe hängt nicht am
  Mandanten, die Spaltenbreiten könnten es. Dieselbe Einschränkung, die
  [`dashboard.md`](dashboard.md) als offenen Punkt 85 führt.
- **Nur eine Nachricht für §5.2** (`ea1060ee-…f993e5`, sieben Gruppen). Die längste Belegnummer des
  Bestands ist **nicht** an einer echten Nachricht gemessen, sondern als 35-Zeichen-Marke in der
  Gestalt der echten verborgen nachgemessen. Das ist die Zahl aus §11a und keine neue Messung.
- **Kein echtes Berührungsgerät.** `pointer: coarse` ist emuliert. Die Emulation meldet sich
  korrekt, die Media-Regel greift — was ein Finger auf einem Glas tatsächlich trifft, sagt das nicht.
- **Kein Vorleseprogramm.** Die Auszeichnung ist am Markup nachgesehen (`role="menuitemradio"`,
  `aria-checked`, `role="group"` mit `aria-labelledby`), nicht angehört.
- **Kein Netzmitschnitt.** Dass beim Umschalten nichts nachgeladen wird, ist **nicht** gemessen;
  gezählt sind nur die Zeilen im Baum (§5.5).
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

`frontend/tests/dichte.test.ts`, **siebenundzwanzig** Zusicherungen, alle rechnerisch und ohne
Ansicht. Der Test liest `globals.css` als Datei — dieselbe Bauform wie `tests/farbwerte.test.ts` und
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

**Und seit dem 01.09.2026 sieben Zusicherungen über die _Lage_**, nicht nur über den Wert — sie
sind der Inhalt der Nachbesserung und fehlten in der ersten Fassung vollständig:

| Was | Warum |
|---|---|
| jede der vier Stufenregeln hängt **unbedingt** am Wurzelelement | Eine Regel in `@media print` oder unter `.dark` steht da und wirkt am Bildschirm nie |
| `html { font-size: var(--dichte-wurzel) }` steht **unbedingt** da | Stünde sie in `@media print`, änderte der Umschalter nur den Ausdruck |
| `--dichte-beruehrung` wird an **genau einer** Stelle gesetzt, und die ist ein unbedingtes `:root` | In `@media print` löst das Token am Bildschirm zu `0px` auf |
| `--dichte-wurzel` steht **einmal** je Stufenblock | Bei zwei Deklarationen gewinnt die Kaskade die zweite, ein Textleser die erste |
| `--spacing-beruehrung` zeigt auf `--dichte-beruehrung` | Die **einzige** Verbindung zwischen dem geprüften Token und den vierzig Komponenten mit `min-h-beruehrung` |
| `--spacing-bedienelement` zeigt auf `--dichte-bedienelement` | dasselbe für das zweite Token |
| genau **ein** `@media (pointer: coarse)` leitet `--dichte-bedienelement` auf `--dichte-beruehrung` um | Die halbe Zusicherung aus `visuelles-konzept.md` §5 — ohne sie gilt die Mindestfläche am Finger für nichts |

Möglich wird das durch einen kleinen **CSS-Leser** im Test: Kommentare werden durch gleich viele
Leerzeichen ersetzt, dann wird die Datei in Regeln **mit ihrer Verschachtelung** zerlegt. Damit ist
die Frage beantwortbar, die eine Regex nicht beantworten kann: *in welchem Block steht das?*

**Der Test ist gegengeprüft worden**, nicht nur geschrieben — und die erste Fassung hat die
Gegenprüfung nicht überstanden. **Von 23 Mutanten blieben neun grün**, darunter das Löschen aller
vier Stufenregeln, solange sie als Kommentar stehen blieben. Der Grund war immer derselbe: Sie
prüfte, dass Zeichenfolgen *vorkommen*, nie, dass sie *wirken*.

Gegen die heutige Fassung sind **zehn** Mutanten gefahren worden, darunter alle acht früheren
Überlebenden. **Keiner überlebt:**

| Mutant | tote Zusicherungen |
|---|---:|
| Stufenregeln nach `@media print` | 4 |
| Stufenregeln unter `.dark` | 4 |
| Stufenregeln gelöscht, als Kommentar stehen gelassen | 14 |
| `--dichte-wurzel` im `xs`-Block auskommentiert | 4 |
| zweite `--dichte-wurzel: 300%` im `m`-Block | 5 |
| `html { font-size: … }` nach `@media print` | 1 |
| `--dichte-beruehrung` nach `@media print { :root }` | 1 |
| `--spacing-beruehrung` auf ein anderes Token gezeigt | 1 |
| Boden im `max()` auf 40 px gesenkt | 4 |
| `pointer: coarse`-Rückfall auf einen festen Wert | 1 |

`globals.css` ist danach byte-gleich wiederhergestellt worden.

> **Was der Test trotzdem nicht ist: ein Wirkungstest.** Er liest eine Datei und rechnet; ob die
> Regeln im Browser das tun, was sie sollen, steht gemessen in §5 und nirgendwo sonst.

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
| **95** | **Der Recharts-Baum skaliert nicht mit.** Achsenschrift (11 px), Diagrammhöhe (260/88 px), Balkenbreite (28 px), `minTickGap` (12 px) und die Ränder sind Pixel und wissen von der Wurzelgröße nichts. **Gemessen ist, dass nichts kaputtgeht** (§5.1) — nichts wird abgeschnitten, nichts überlappt, die Balken stehen übereinander. Sichtbar ist es trotzdem: In `l` steht die Diagrammschrift still, während alles daneben wächst. Der Weg wäre der Stufenfaktor als Parameter der reinen Funktion in `verlauf.ts`, **niemals** `getComputedStyle` zur Laufzeit. **Derselbe Punkt trägt eine zweite, größere Hälfte:** Auch die **Umbruchpunkte von Tailwind** gehen nicht mit — `rem` in einer Media Query misst gegen die Anfangsschriftgröße, nicht gegen das Wurzelelement (gemessen bei 1200 und 1279 px: `sm`/`md`/`lg`/`xl` in `xs`, `m` und `l` identisch). Ein Fall ist gefunden und gemessen (die Filterleiste bricht in `l` um und kostet eine Zeile, §5.3); **systematisch abgesucht ist der Anwendungsrahmen nicht** |
| **96** | **Drei Klassen von Bedienelementen bleiben am Berührungsgerät unter 44 px, in jeder Stufe einschließlich `m`** (§5.4): der Sortierknopf im Tabellenkopf (20 px in `m`), jede Tabellenzeile (36 px) und der `Switch` aus dem Generatorbereich (18,4 px, feste Pixel). Dazu `--dichte-feld` mit 40 px. **Alle vier waren es vorher auch.** Ein Boden an `--dichte-feld` scheidet aus, solange E‑z gilt — er änderte `m`. Es ist eine Entscheidung über die Nachrichtenliste und über den gemeinsamen Baustein, nicht über die Dichte |
| **97** | **Die Tabellenkopfzeile der Nachrichtenliste hält nicht** (§5.3). `position: static`, sie scrollt vollständig weg — in jeder Stufe und schon vorher. `components/anwendungsrahmen.tsx` und [`frontend-grundlagen.md`](frontend-grundlagen.md) §7 behaupten beide das Gegenteil. `benutzer-tabelle.tsx` und `katalog-tabelle.tsx` machen es vor (`sticky -top-4`) |
| **98** | **Ohne JavaScript ist der Umschalter nicht erreichbar — und die Sprachumschaltung im Anwendungsrahmen genauso wenig.** Der *Weg* braucht keins: Beides sind Formulare mit Server-Aktion. **Nur wird der Anwendungsrahmen im Browser gebaut.** Gemessen an der angemeldeten Antwort für `/nachrichten`: **1.379 Zeichen** Markup, darin **kein** `<header>`, **kein** `<form>`, **keine** `$ACTION_ID_` — die übrigen 47.456 Zeichen sind RSC-Nutzlast. Auf der **Anmeldeseite** steht das Sprachformular dagegen wirklich im Markup und funktioniert dort ohne JavaScript. Der Nebeneffekt, den [`frontend-grundlagen.md`](frontend-grundlagen.md) §4 seit Schritt 3 für die Sprache in Anspruch nimmt, trägt also nur dort — ein Befund über eine bestehende Zusage, nicht über diese Runde |
| **99** | **Die Buchstabenreihe der Entscheidungs-IDs ist mit `E‑z` aufgebraucht** (§2). Es gibt keine Fortsetzungsregel. Sie zu erfinden betrifft das ganze Projekt und nicht diese Runde; [`README.md`](README.md) („Die Nummernkreise") ist der Ort dafür. **Am 01.09.2026 hat die Mutationsrunde numerisch fortgesetzt** — `E‑a` ist die erste, `E‑z` die sechsundzwanzigste, die nächste heißt `E‑27` ([`testfestigkeit.md`](testfestigkeit.md) §10.6 vergibt E‑27 bis E‑31; vorher trug keine Datei eine numerische `E‑<n>`). **Der Punkt bleibt offen:** Das ist eine gelebte Fortsetzung und keine Festlegung im Register, und genau diese Lücke beschreibt Punkt 104 |
| **100** | **Nach der Auswahl springt der Tastaturfokus aus der Gruppe heraus.** Dreimal über Fokusereignisse reproduziert: Der gewählte Eintrag bekommt den Fokus, und sobald die Antwort der Server-Aktion ankommt (rund 480 ms später), wandert er auf den Menürumpf; danach trägt kein Eintrag mehr `data-highlighted`, und das nächste `ArrowDown` beginnt wieder oben. Wer mit der Tastatur zwei Stufen vergleichen will, verliert dabei jedes Mal seine Stelle. Ursache ist das Neurendern des Menüs durch `revalidatePath` |
| **101** | **`revalidatePath` läuft auch, wenn die gewählte Stufe schon die aktive ist.** Bei `role="menuitemradio"` ist das erneute Wählen des angehakten Eintrags eine normale Handlung; sie kostet dann einen vollen RSC-Umlauf und den anwendungsweiten Verwurf des Router-Zwischenspeichers für einen Nullvorgang. Ein Vergleich mit dem Cookie vor dem Schreiben genügte — hier bewusst nicht eingebaut, weil er einen eigenen Test bräuchte und die Runde ihn nicht verlangt |
| **102** | **Kein Test rendert den Umschalter.** `tests/dichte.test.ts` prueft die Datei `globals.css` und die reine Funktion `dichteAus`; `components/dichte-umschaltung.tsx`, `dichte/provider.tsx`, `dichte/aktion.ts` und `dichte/server.ts` haben **keine** Abdeckung. Ungeprueft bleibt damit alles, was Teil 3 zugesagt hat: die vier Eintraege, `role="menuitemradio"`, `aria-checked`, das abgefangene `onSelect`, das Haekchen im Fluss. **Gemessen ist es** (§4, §5.5), zugesichert nicht. Das Projekt zaehlt gerenderte Testbaeume bewusst ab (`vitest.config.mts`) — ein weiterer waere also eine Entscheidung und keine Selbstverstaendlichkeit |
| **103** | **Auch die vier Cookie-Eigenschaften sind gemessen und nicht zugesichert.** Ein Jahr, `SameSite=Lax`, kein `HttpOnly`, `path="/"` — am gesetzten Cookie abgelesen (§5.5), aber kein Test hielte eine Aenderung auf. Dasselbe gilt fuer den serverseitigen Leseweg und `revalidatePath` |
| **104** | **Das Register der Nummernkreise kennt die Buchstabenreihe nicht.** [`README.md`](README.md) („Die Nummernkreise“, 21.08.2026) legt fuer `E` fest: *je Feature-Datei fortlaufend, beginnend bei E1* — und *„dasselbe E14 in zwei Feature-Dateien ist der Regelfall“*. Die Reihe `E‑a` … `E‑v` verhaelt sich nachweislich anders: Sie beginnt in **keiner** Datei neu (`dashboard.md` fuehrt c, d, g, h, i; `dashboard-frontend.md` daneben l bis v). Es gibt also **zwei** Reihen unter demselben Buchstaben, und das Register beschreibt nur eine. §2 dieser Datei ist nach der gelebten Praxis vergeben worden, nicht nach dem Register — festzulegen ist das in `README.md` und nicht hier (vgl. Punkt 99) |
