# Dunkelmodus — Schritt 11a: die Werte

**Was diese Runde tut:** Sie rechnet den nie nachgerechneten Dunkelblock in `app/globals.css`
durch, belegt seine Werte und lässt die Kontraste erstmals von einem Test halten — **ohne
Umschalter und ohne eine einzige sichtbare Änderung an einer bestehenden Ansicht.**

Der Anlass steht wörtlich in [`visuelles-konzept.md`](visuelles-konzept.md) §7a, Befund 3:

> „Der Dunkelblock ist nie nachgerechnet worden … **Wer den Dunkelmodus einschaltet, rechnet den
> ganzen Block nach — nicht nur diese Rolle.**"

Der Umschalter selbst ist **Schritt 11b** und steht hier nicht. Was 11a von 11b trennt, steht in
[`IMPLEMENTIERUNGSPLAN_MVP.md`](IMPLEMENTIERUNGSPLAN_MVP.md), Schritt 11.

> ### 📌 Nachgetragen am 03.09.2026: **Schritt 11b steht seither ab §12 in derselben Datei**
>
> Der Satz eine Zeile höher — *„Der Umschalter selbst ist Schritt 11b und steht hier nicht"* — war
> richtig, als er geschrieben wurde, und ist es an diesem Tag nicht mehr. **Gestrichen ist er
> nicht:** §1 bis §11 sind der Bericht von 11a und stehen unverändert, samt der Vorbedingung „nichts
> schaltet den Block ein", die genau bis §12 galt.
>
> Der Umschalter steht **ab §12**. Wer wissen will, *welche Werte* der Dunkelzustand trägt, liest
> §1–§11; wer wissen will, *wie er eingeschaltet wird*, liest §12–§24.

> ### ⚠️ Das Ergebnis in zwei Sätzen, weil es die Prämisse der Runde umdreht
>
> **Der Dunkelblock hält jede der sechs vorregistrierten Schwellen — ohne dass ein einziger Wert
> geändert werden musste.** Der **helle** Block, der als gegeben galt, hielt sie nicht: `--status-ungeklaert`
> verfehlte die 4,5 : 1 dreifach und ist der einzige Wert, den diese Runde ändert (§4).

---

## 1. Teil 0 — die Vorbedingung, und sie ist belegt

> **Ergebnis: Nichts im Frontend schaltet heute auf den Dunkelblock.** Kein Element bekommt jemals
> die Klasse `dark`, und die Bedingung ist nirgends ein zweites Mal definiert. Der Block wird
> gebaut, ausgeliefert und steht ungenutzt im Stilblatt.

Das ist **Abbruchkriterium** und nicht Beiwerk: Der ganze Zuschnitt dieses Schritts ruht darauf,
dass geänderte Dunkelwerte unsichtbar bleiben. Wäre der Block erreichbar, gingen neue Werte
ungemessen in eine Ansicht — genau das, was diese Runde verhindern soll.

**Eine unbelegte Vorbedingung ist keine.** Deshalb steht hier die vollständige Liste dessen, wonach
gesucht worden ist, und nicht nur das Ergebnis.

### 1.1 Wonach gesucht worden ist — vier Linsen und eine Vollständigkeitsprüfung

| Linse | Wonach | Ergebnis |
|---|---|---|
| **Quelltext** | `grep -rniI "dark"` über `src/`, `tests/` und alle Wurzeldateien; `class=`/`className=` (auch alle 80 nicht-literalen); `classList.add/toggle/remove`; `setAttribute`; `documentElement`; `document.body`; `dangerouslySetInnerHTML`; `<script`; zusammengesetzte Namen (`"da"+`, `+"rk"`, `String.fromCharCode`, `atob(`, Base64 `ZGFyaw`); alle **drei** `layout.tsx`; `providers.tsx`; `src/lib/utils.ts` (`cn`) | **22 Zeilen, kein Setzer** — nachgezählt gegen den Stand *vor* dieser Runde (`git archive HEAD`, dann `grep -rniI "dark" src tests`): **13** in `components/ui/*` (Tailwind-`dark:`-Varianten, also Benutzungen und keine Aktivierungen; als *Vorkommen* gezählt 28), **2** in `globals.css` selbst (Zeile 5 und der Block) und **7** in zwei Testdateien (`farbwerte.test.ts` 4, `dichte.test.ts` 3) |
| **CSS und Tailwind** | `globals.css` vollständig; `postcss.config.mjs`; `components.json`; `next.config.ts`; alle drei `@import`-Ziele in `node_modules` aufgelöst (`tailwindcss/{index,theme,preflight,utilities}.css`, `tw-animate-css/dist/*.css`, `shadcn/dist/tailwind.css`); die eingebaute Variantentabelle in `tailwindcss/dist/lib.js`; `prefers-color-scheme`; `color-scheme`; `light-dark(`; `@custom-variant` | **Kein zweiter Setzer.** Zeile 5 `@custom-variant dark (&:is(.dark *))` **ersetzt** die Tailwind-Vorgabe `@media (prefers-color-scheme: dark)`; die drei importierten Dateien bringen 0 × `.dark`, 0 × `@custom-variant dark`, 0 × `prefers-color-scheme` mit. Der einzige `dark`-Bezug im shadcn-CSS ist ein `@variant dark` **innerhalb** von `@utility shimmer` — eine Benutzung |
| **Das Erzeugnis** | alle acht gebauten CSS-Bündel, Entwicklung **und** Produktion, auf `prefers-color-scheme` und auf `.dark`; das ausgelieferte HTML | **`prefers-color-scheme`: null Treffer in jeder einzelnen Datei.** Produktion: 31 × `:is(.dark *)`, der Block zweimal als `.dark{…}` (Hex- und `lab()`-Rückfallform). Die Bedingung ist **ausschließlich klassenbasiert** |
| **Mechanismen** | `next-themes`, `ThemeProvider`, `useTheme`, `setTheme`, `darkMode` in Quelltext **und** `pnpm-lock.yaml`; `instrumentation-client.ts`; `next/script`; `<meta name="color-scheme">`; `export const viewport`; jeder Cookie- und `localStorage`-Leser; `.env.example`/`.env.local` | **Kein Treffer.** Cookies berührt die Anwendung an fünf Stellen, **gelesen** wird an dreien: `dichte/server.ts`, `i18n/server.ts` und `lib/http.ts` (CSRF-Token); die beiden `aktion.ts` **schreiben** nur. Kein Thema-Cookie, keine Thema-Bibliothek im Abhängigkeitsbaum |
| **Vollständigkeit** (was die vier nicht abgedeckt hatten) | `proxy.ts`; `template.tsx`/`global-error.tsx`; ein Klassenname **aus Daten** gebaut; **ganz `node_modules`** mit `rg --no-ignore` (635 MB, in beide Richtungen geeicht); `tailwind-merge` als möglicher Setzer; `next/font`-Klassennamen im ausgelieferten HTML; eingespritztes HTML (`srcDoc`, `<iframe>`, `innerHTML`, `createPortal`); das Repository außerhalb `frontend/` | **Kein Treffer.** In `node_modules` nur das Next-Dev-Overlay und der Bundle-Analyzer (beide unbenutzt); im Repository außerhalb `frontend/` nur `-ForegroundColor DarkGray` in `scripts/dev-start.ps1` |

### 1.2 Zwei Belege, die keiner Lesart bedürfen

**Erstens der Overlay-Einwand, und er ist ausgeräumt.** Das Dev-Overlay von Next.js setzt eine
Klasse — es hängt sich aber als **Geschwister** an `document.body` und wird nie zum Vorfahren.
Nachgeprüft mit einem `MutationObserver`, der den Knoten bei Entfernung wieder anhängt.

**Zweitens die Messung am laufenden System.** Beim Aufruf einer produktiven Route steht im
ausgelieferten Markup:

```html
<html lang="de" data-dichte="m" class="font-sans geist_…__variable geist_mono_…__variable">
```

Kein `dark` — weder am `<html>` noch am `<body>`. Während der Sichtprobe (§6) war das **einzige**
Element im ganzen Dokument mit dem Klassentoken `dark` der eigens dafür gebaute Wrapper der
temporären Route; er ist danach entfernt worden.

> **⚠️ Ein methodischer Warnhinweis, weil er die Belegkraft betrifft.** Eine Verzeichnissuche mit
> dem Standardwerkzeug **überspringt `.gitignore`-ignorierte Pfade** und findet in `node_modules`
> nichts, ohne das zu sagen. Aufgefallen ist das nur an einer Eichung gegen eine bekannte
> Fundstelle (Verzeichnis: 0 Treffer, dieselbe Datei einzeln benannt: 2 Treffer). Die Zahlen oben
> stammen deshalb aus `rg --no-ignore`, in beide Richtungen geeicht. **Eine Suche, die nur „nichts
> gefunden" sagen kann, sieht genauso aus wie eine, die funktioniert.**

### 1.3 Was daraus **nicht** folgt

Der Block ist heute nicht erreichbar. Das heißt **nicht**, dass er es nicht wäre, sobald jemand die
Klasse setzt: Die Sichtprobe hat genau das getan, und der Block hat auf Anhieb gegriffen — mit
seinen 50 Deklarationen auf dem Wrapper selbst und über 24 `dark:`-Varianten im Teilbaum darunter.
**Er funktioniert. Er wird nur nirgends eingeschaltet.**

---

## 2. Das Skript wird allgemein

`scripts/farbrolle-ueberfaellig/` heißt seit dem 03.09.2026 **`scripts/farbwerte/`**. Es rechnet
nicht mehr eine Rolle, sondern den Bestand: fünf Rollen zu je drei Werten und vier Akzentstufen,
in **beiden** Blöcken.

```
node scripts/farbwerte/rechne.mjs
```

**Die Gegenprobe ist nicht angefasst, sondern erweitert worden.** Sie ist der Beleg, dass die
Umbenennung nichts geändert hat, und sie ist zugleich die Antwort auf die Frage, die vor dem
Ergebnis steht: *Rechnet dieses Skript überhaupt so, wie [`visuelles-konzept.md`](visuelles-konzept.md) §3 gerechnet worden ist?*

### Die vierzehn

| | Was | Woher | Erwartet |
|---|---|---|---|
| 1 | `akzent` auf `card` | `visuelles-konzept.md` §3 | 1,98 : 1 |
| 2 | `akzent-schrift` auf `card` | `visuelles-konzept.md` §3 | 5,40 : 1 |
| 3 | `akzent-schrift` auf `background` | `visuelles-konzept.md` §3 | 5,18 : 1 |
| 4 | `akzent-schrift` auf `akzent-flaeche` | `visuelles-konzept.md` §3 | 4,91 : 1 |
| 5 | `akzent-vordergrund` auf `akzent` | `visuelles-konzept.md` §3 | 9,13 : 1 |
| 6 | `status-abgeschlossen` auf `card` | `visuelles-konzept.md` §3 | 6,78 : 1 |
| 7 | OKLab-Abstand Grün → Akzent | `visuelles-konzept.md` §3 | 0,3435 |
| 8 | OKLab-Abstand Akzent → Rot | `visuelles-konzept.md` §3 | 0,3523 |
| 9–14 | die sechs Hexwerte `#886108` `#fcf0dd` `#b88513` `#cb9317` `#312103` `#8b640f` | `visuelles-konzept.md` §7a | Ziffer für Ziffer |

**Alle vierzehn laufen nach der Umbenennung unverändert durch** — und sie stehen zusätzlich in
`tests/farbkontrast.test.ts`, wo sie bei jedem `pnpm test` mitlaufen (§7).

Die fünf Hexwerte aus [`visuelles-konzept.md`](visuelles-konzept.md) §3 (`#b9c022`, `#6a6f0f`, `#f3f6dc`, `#161802`, `#01684c`) zählen nicht zu den
vierzehn und laufen weiter mit; sie standen schon vorher im Skript und sind nicht entfallen.

### Die Methode ist vorgegeben und nicht neu gewählt

Wörtlich aus [`visuelles-konzept.md`](visuelles-konzept.md) §7a, „Wie gerechnet worden ist":

- **Der Kontrast entsteht aus den ungerundeten sRGB-Fließkommawerten**, nicht aus dem 8‑Bit-Hexwert.
  Nur diese Methode gibt die Zahlen aus §3 dort wieder. Neben jedem Wert steht zusätzlich der aus dem
  gerundeten Hexwert — der, den ein Bildschirm wirklich zeigt.
- **Der OKLab-Abstand ist der euklidische Abstand in (L, a, b) einschließlich der Helligkeit.**

> **Unabhängig bestätigt, und zwar von einer dritten Rechnung.** Während der Sichtprobe ist am
> laufenden Chrome abgefragt worden, wozu die Tokens auflösen. Für jeden unbunten Wert stimmt das
> Ergebnis exakt: `--status-ungeklaert` hell wird `lab(47.8% 0 0)`, und 116 · 0,55 − 16 = 47,8;
> `--card` dunkel wird `lab(8.36% 0 0)`, und 116 · 0,21 − 16 = 8,36; `--background` dunkel wird
> `lab(3.6999% 0 0)` — dort greift der lineare Zweig, 903,3 · 0,16³ = 3,6999. Und die beiden Werte,
> die [`dashboard-frontend.md`](dashboard-frontend.md) §10.5 am 01.09.2026 am laufenden System
> gemessen hat (`lab(98.26 0 0)` und `lab(95.36 0 0)`), kommen Ziffer für Ziffer wieder heraus.

---

## 3. Die Erhebung *(M131 hell, M132 dunkel)*

Alle Zahlen aus `scripts/farbwerte/rechne.mjs`. Format je Zelle: **ungerundet** *(aus dem Hexwert)*.

### 3.1 Die Werte

| Token | hell | dunkel |
|---|---|---|
| `--status-abgeschlossen` | `oklch(0.46 0.095 166)` · `#01684c` | `oklch(0.75 0.13 166)` · `#49c89b` |
| `--status-abgeschlossen-flaeche` | `oklch(0.96 0.024 166)` · `#e4f7ee` | `oklch(0.26 0.04 166)` · `#0e2a1f` |
| `--status-abgeschlossen-kontur` | `oklch(0.85 0.05 166)` · `#b0d9c6` | `oklch(0.4 0.07 166)` · `#19543f` |
| `--status-fehler` | `oklch(0.52 0.19 27)` · `#be2323` | `oklch(0.7 0.17 27)` · `#f66d62` |
| `--status-fehler-flaeche` | `oklch(0.96 0.028 27)` · `#ffebe8` ⚠️ | `oklch(0.26 0.05 27)` · `#391a17` |
| `--status-fehler-kontur` | `oklch(0.86 0.07 27)` · `#fcc0b8` | `oklch(0.4 0.09 27)` · `#70322c` |
| `--status-offen` | `oklch(0.44 0 0)` · `#525252` | `oklch(0.78 0 0)` · `#b7b7b7` |
| `--status-offen-flaeche` | `oklch(0.96 0 0)` · `#f2f2f2` | `oklch(0.25 0 0)` · `#222222` |
| `--status-offen-kontur` | `oklch(0.87 0 0)` · `#d4d4d4` | `oklch(0.35 0 0)` · `#3a3a3a` |
| **`--status-ungeklaert`** | **`oklch(0.55 0 0)` · `#717171`** ⚠️ geändert | `oklch(0.62 0 0)` · `#868686` |
| `--status-ungeklaert-flaeche` | `oklch(0.98 0 0)` · `#f8f8f8` | `oklch(0.22 0 0)` · `#1b1b1b` |
| `--status-ungeklaert-kontur` | `oklch(0.9 0 0)` · `#dedede` | `oklch(0.32 0 0)` · `#333333` |
| `--ueberfaellig` | `oklch(0.52 0.105 80)` · `#886108` | `oklch(0.7 0.14 80)` · `#cb9317` |
| `--ueberfaellig-flaeche` | `oklch(0.96 0.028 80)` · `#fcf0dd` | `oklch(0.26 0.05 80)` · `#312103` |
| `--ueberfaellig-kontur` | `oklch(0.65 0.13 80)` · `#b88513` | `oklch(0.53 0.105 80)` · `#8b640f` |
| `--akzent` | `oklch(0.777 0.1643 112.4)` · `#b9c022` | **dieselbe** · `#b9c022` |
| `--akzent-schrift` | `oklch(0.52 0.11 112.4)` · `#6a6f0f` | `oklch(0.83 0.15 112.4)` · `#c9d151` |
| `--akzent-vordergrund` | `oklch(0.2 0.04 112.4)` · `#161802` | **dieselbe** · `#161802` |
| `--akzent-flaeche` | `oklch(0.965 0.035 112.4)` · `#f3f6dc` | `oklch(0.28 0.06 112.4)` · `#2a2c02` |
| *Bezug:* `--card` | `#ffffff` | `#181818` |
| *Bezug:* `--background` | `#fafafa` | `#0d0d0d` |
| *Bezug:* `--muted` | `#f2f2f2` | `#262626` |

**Die drei Bezugstokens sind gelesen und nicht gerechnet** (E‑61). Sie kommen aus dem Generator;
sie zu ändern wäre eine zweite Pflegestelle gegen jedes künftige `shadcn add`.

### 3.2 Die Vordergründe — Schwelle 1

| Rolle | hell: eigene Fläche · `--card` · `--background` | dunkel: eigene Fläche · `--card` · `--background` |
|---|---|---|
| `--status-abgeschlossen` | 6,09 *(6,10)* · 6,78 *(6,81)* · 6,49 *(6,52)* | 7,28 *(7,31)* · 8,43 *(8,47)* · 9,24 *(9,27)* |
| `--status-fehler` | 5,30 *(5,30)* · 6,08 *(6,08)* · 5,82 *(5,82)* | 5,49 *(5,46)* · 6,16 *(6,16)* · 6,74 *(6,74)* |
| `--status-offen` | 6,91 *(6,98)* · 7,77 *(7,81)* · 7,44 *(7,49)* | 7,99 *(7,93)* · 8,85 *(8,85)* · 9,70 *(9,69)* |
| `--status-ungeklaert` **neu** | **4,58** *(4,60)* · **4,85** *(4,88)* · **4,65** *(4,68)* | 4,75 *(4,73)* · 4,87 *(4,88)* · 5,33 *(5,34)* |
| `--status-ungeklaert` *alt* | *4,04 · 4,28 · 4,10* — **dreimal verfehlt** | — |
| `--ueberfaellig` | 4,97 *(4,96)* · 5,59 *(5,58)* · 5,35 *(5,35)* | 5,75 *(5,72)* · 6,53 *(6,53)* · 7,15 *(7,15)* |

### 3.3 Die vier Akzentstufen

| | hell | dunkel |
|---|---|---|
| `--akzent-schrift` auf `--card` | 5,40 *(5,39)* | 10,72 *(10,76)* |
| `--akzent-schrift` auf `--background` | 5,18 *(5,17)* | 11,75 *(11,77)* |
| `--akzent-schrift` auf `--akzent-flaeche` | 4,91 *(4,89)* | 8,76 *(8,72)* |
| `--akzent-vordergrund` auf `--akzent` | 9,13 *(9,12)* | 9,13 *(9,12)* |
| `--akzent` auf `--card` *(Füllfarbe, Bericht)* | **1,98** | **8,97** |

**`--akzent` steht bewusst nicht unter Schwelle 1.** Er ist eine Füllfarbe und keine Schriftfarbe
(§3) — genau deshalb gibt es die drei anderen Stufen. Die letzte Zeile ist trotzdem ein Befund:
Dieselbe Farbe trägt auf dunklem Grund **8,97 : 1** statt 1,98 : 1. Die Anwendungsfarbe ist im
Dunkelblock erheblich lauter als im hellen; die Sichtprobe bestätigt es (§6).

### 3.4 Die Konturen — berichtet, nicht zugesichert (Schwelle 5)

Gemessen gegen `--card`:

| Kontur | hell | | dunkel | |
|---|---|---|---|---|
| `--status-abgeschlossen-kontur` | 1,55 : 1 | verfehlt | 2,00 : 1 | verfehlt |
| `--status-fehler-kontur` | 1,57 : 1 | verfehlt | 1,84 : 1 | verfehlt |
| `--status-offen-kontur` | 1,48 : 1 | verfehlt | 1,57 : 1 | verfehlt |
| `--status-ungeklaert-kontur` | 1,35 : 1 | verfehlt | 1,40 : 1 | verfehlt |
| **`--ueberfaellig-kontur`** | **3,29 : 1** | **erfüllt** | **3,31 : 1** | **erfüllt** |

**Befund 4 aus §7a gilt im Dunkelblock unverändert**, und zwar mit derselben Gestalt: vier von fünf
verfehlen die 3 : 1 aus WCAG 1.4.11, dieselbe eine erfüllt sie. Die vier liegen im Dunkeln
allerdings **enger beieinander und höher** (1,40 bis 2,00 gegen 1,35 bis 1,57) — die Ungleichheit
ist dort geringfügig kleiner, aber sie besteht.

### 3.5 Die OKLab-Abstände zum Akzent — Schwelle 4

| Rolle | hell | dunkel |
|---|---|---|
| `--status-abgeschlossen` | 0,3435 | **0,1388** |
| `--status-fehler` | 0,3523 | 0,2395 |
| `--status-offen` | 0,3749 | 0,1643 |
| `--status-ungeklaert` | 0,2802 *(vorher 0,2565)* | 0,2273 |
| `--ueberfaellig` | 0,2737 | **0,1170** ← das Minimum des Bestands |

**Die 0,117 sind bestätigt und nicht widerlegt.** Sie bleiben die kürzeste Strecke zum Akzent im
ganzen Bestand; die zweitkürzeste ist weiterhin das dunkle Grün mit 0,139, dann `--status-offen`
mit 0,164. Schwelle 4 ist damit gehalten — nichts ist schlechter geworden, und die eine Strecke,
die sich bewegt hat (`--status-ungeklaert` hell), ist **größer** geworden.

### 3.6 Die Abstände der Rollen untereinander — Bericht

| Strecke | hell | dunkel |
|---|---|---|
| abgeschlossen → fehler | 0,2756 | 0,2858 |
| abgeschlossen → offen | 0,0971 | 0,1334 |
| abgeschlossen → ungeklaert | 0,1309 *(vorher 0,1531)* | 0,1838 |
| abgeschlossen → ueberfaellig | 0,1492 | 0,1909 |
| fehler → offen | 0,2062 | 0,1879 |
| fehler → ungeklaert | 0,1924 *(vorher 0,1992)* | 0,1879 |
| fehler → ueberfaellig | 0,1520 | 0,1409 |
| offen → ungeklaert | **0,1100** *(vorher 0,1400)* | 0,1600 |
| offen → ueberfaellig | 0,1320 | 0,1612 |
| ungeklaert → ueberfaellig | **0,1092** *(vorher 0,1209)* | 0,1612 |

… und dieselben Strecken zwischen den **Flächen**, die als Kacheln nebeneinanderstehen:

| Strecke | hell | dunkel |
|---|---|---|
| abgeschlossen → fehler | 0,0487 | 0,0844 |
| abgeschlossen → offen | 0,0240 | 0,0412 |
| abgeschlossen → ungeklaert | 0,0312 | 0,0566 |
| abgeschlossen → ueberfaellig | 0,0356 | 0,0618 |
| fehler → offen | 0,0280 | 0,0510 |
| fehler → ungeklaert | 0,0344 | 0,0640 |
| fehler → ueberfaellig | **0,0250** | **0,0446** |
| offen → ungeklaert | 0,0200 | 0,0300 |
| offen → ueberfaellig | 0,0280 | 0,0510 |
| ungeklaert → ueberfaellig | 0,0344 | 0,0640 |

**Die Flächen liegen im Dunkelblock durchweg weiter auseinander** — die kritische Strecke aus
[`dashboard-frontend.md`](dashboard-frontend.md) §3 (Überfällig gegen Fehler) wächst von 0,025 auf
0,045, also fast auf das Doppelte. Das ist die einzige Zahl dieser Runde, die dem Dunkelblock einen
Vorteil gegenüber dem hellen bescheinigt.

### 3.7 Offener Punkt 92 — gemessen, nicht behoben *(M133)*

Die gedrückte Schaltfläche des Umschalters trägt `bg-muted`, darunter liegt `--background`:

| | Fläche gedrückt | Untergrund | Kontrast |
|---|---|---|---|
| hell | `--muted` `#f2f2f2` | `--background` `#fafafa` | **1,08 : 1** *(aus dem Hexwert 1,07)* |
| dunkel | `--muted` `#262626` | `--background` `#0d0d0d` | **1,29 : 1** *(aus dem Hexwert 1,28)* |

**Der Wert 1,07 aus [`dashboard-frontend.md`](dashboard-frontend.md) §10.5 ist damit
nachgerechnet** — dort am laufenden System abgelesen, hier aus den Tokens gerechnet, und beide
Wege geben dieselbe Zahl.

**Im Dunkelblock ist der Zustand rund ein Fünftel besser sichtbar und trotzdem weit von brauchbar
entfernt.** Die Sichtprobe bestätigt beides (§6). Behoben wird hier nichts: Es ist die Gestalt von
`components/ui/toggle-group.tsx`, Generatorbereich, und die Zeitfensterwahl der Nachrichtenliste
trägt sie genauso. Offener Punkt **92** bleibt offen.

---

## 4. Die Werte — was geändert worden ist, und was nicht

### 4.1 Im Dunkelblock: **nichts** *(E‑63)*

Teil 3 des Auftrags sagt: *„Für Rollen, die sie heute schon einhalten, ändere nichts — eine
Änderung ohne Anlass wäre eine Umfärbung des Bestands."* Gemessen halten **alle fünf Rollen und
alle vier Akzentstufen** jede Schwelle. Also ist kein Dunkelwert angefasst worden.

**Das ist das Ergebnis und kein ausgebliebenes.** Der Block war nie nachgerechnet; jetzt ist er es,
und er trägt. Der knappste Wert ist `--status-ungeklaert` mit 4,75 : 1 auf seiner eigenen Fläche —
0,25 Luft zur Schwelle.

**`--ueberfaellig` ist nachgerechnet und nicht geändert**, wie der Auftrag es verlangt. Alle sechs
Hexwerte aus §7a kommen Ziffer für Ziffer wieder heraus, die vier Kontrastbedingungen halten, die
0,117 zum Akzent sind bestätigt. **Es kommt keine andere Zahl heraus als die verzeichnete.**

### 4.2 Im hellen Block: **ein Wert** *(E‑62)*

> ⚠️ **Hier weicht die Runde von ihrer eigenen Abgrenzung ab, und zwar bewusst und auf Entscheidung
> des Auftraggebers vom 03.09.2026.** Der Auftrag schrieb „Keine Änderung am hellen Block. Nicht ein
> Wert" und zugleich Schwelle 1 als Abbruchbedingung *„in beiden Blöcken"*. Beides zugleich ging
> nicht: Der helle Block verfehlt Schwelle 1 dreifach. Vorgelegt worden sind drei Wege — Befund
> ohne Änderung, Mitkorrektur, Abbruch; gewählt worden ist die **Mitkorrektur**.

**Gemessen war** (M131), und es ist nie zuvor gemessen worden — §3 hat den Akzent und das Grün
nachgerechnet, die beiden neutralen Rollen nicht:

| `--status-ungeklaert` `oklch(0.58 0 0)` · `#7a7a7a` | |
|---|---|
| auf `--status-ungeklaert-flaeche` | **4,04 : 1** |
| auf `--card` | **4,28 : 1** |
| auf `--background` | **4,10 : 1** |

**Behauptet wird**: Mit `oklch(0.55 0 0)` · `#717171` sind es **4,58 / 4,85 / 4,65** — ungerundet
wie aus dem Hexwert. Die Rolle **ist** Schrift: `text-status-ungeklaert` steht in der Plakette, im
Prozessbaum und als Diagrammfüllung (`lib/status-farbe.ts`), und WCAG 1.4.3 verlangt für Fließtext
4,5 : 1.

**Warum genau 0.55 und nicht knapper.** Die bindende Bedingung ist die eigene Fläche bei L 0.98.
Dort fallen die beiden Rechenwege **auseinander**, und zwar in genau dem Bereich, in dem ein
knapperer Wert läge (Bisektion, 80 Schritte):

| | ungerundet ≥ 4,5 bis | aus dem Hexwert ≥ 4,5 bis |
|---|---|---|
| `--status-ungeklaert` auf seiner Fläche | **L 0,554253** | **L 0,553804** |

Bei L 0.554 trägt der ungerundete Wert also noch **4,5048 : 1**, der **gerundete Hexwert** aber nur
noch **4,4648 : 1** — und der ist der, den ein Bildschirm zeigt. **Ein Wert, der ungerundet knapp
hält und auf dem Bildschirm knapp nicht, ist keiner.** 0.55 ist der nächste glatte Wert, bei dem
beide Wege tragen (4,58 gegen 4,60).

**Was es kostet, und es steht hier statt geglättet zu werden:**

| | vorher | nachher |
|---|---|---|
| OKLab-Abstand zu `--status-offen` | 0,1400 | **0,1100** |
| OKLab-Abstand zu `--ueberfaellig` | 0,1209 | **0,1092** |
| OKLab-Abstand zu `--status-abgeschlossen` | 0,1531 | 0,1309 |
| OKLab-Abstand zum **Akzent** | 0,2565 | **0,2802** |

Die beiden neutralen Rollen rücken zusammen. Sie sind über die Helligkeit weiterhin klar getrennt
(L 0.44 gegen 0.55, in der Sichtprobe deutlich zu unterscheiden) und tragen ohnehin Wort und
Zeichen — §3, *„nie allein über Farbe"*. Der Abstand zum Akzent **wächst**, Schwelle 4 ist also
nicht berührt.

### 4.3 ⚠️ Befund: `--status-fehler-flaeche` liegt außerhalb sRGB *(offener Punkt 123)*

`oklch(0.96 0.028 27)` kommt im roten Kanal auf **1,0211** und wird abgeschnitten; was der
Bildschirm zeigt, ist `#ffebe8`. Bei L 0.96 und Ton 27 trägt sRGB höchstens Chroma **0,0199**, hier
stehen 0,028.

**Die beiden Geschwister halten den Rahmen:** `--status-abgeschlossen-flaeche` steht bei 0,024 von
möglichen 0,0587, `--ueberfaellig-flaeche` bei 0,028 von 0,0368. Es ist die eine Rolle, bei der der
Rand überschritten worden ist — und zwar seit Schritt 3, unbemerkt.

**Nicht geändert, mit Grund:** Es ist eine **Fläche** und kein Vordergrund; die abgeschnittene
Farbe ist eine gültige blasse Tönung, und ihr Kontrast zu `--status-fehler` ist mit 5,30 : 1
gemessen und ausreichend. Sie zu korrigieren hieße, den Rotton der Fehlerfläche im ganzen Bestand
zu verschieben — eine eigene Entscheidung, und sie war nicht Gegenstand der Wahl, die der
Auftraggeber getroffen hat.

**Der Überschuss ist festgenagelt statt übersprungen** — in `scripts/farbwerte/rechne.mjs` und in
`tests/farbkontrast.test.ts`. Er darf bleiben, wo er ist; wächst er, fällt es auf. **Eine
übersprungene Ausnahme sähe irgendwann aus wie ein geprüfter Wert.**

> ### ✔ Geschlossen am 04.09.2026 (E‑88) — und der Anlass war ein anderer
>
> **Der Abschnitt darüber bleibt vollständig stehen.** Er beschreibt den Zustand, der bis heute
> galt, und er nennt den Grund, aus dem er stehen blieb: Eine Korrektur verschöbe den Rotton der
> Fehlerfläche im ganzen Bestand und wäre eine eigene Entscheidung. **Genau die ist jetzt getroffen
> worden — nur nicht wegen des Farbraums.**
>
> Die Verlaufsfläche der Übersicht trägt seit **E‑87** einen kräftigen Petrolton. Wo zwei Flächen
> reiben, weicht nach [`visuelles-konzept.md`](visuelles-konzept.md) §3 die **ohne** Bedeutung;
> gewichen ist zuerst die Verlaufsfläche (über die Deckung), und die Kachelfläche gibt zusätzlich
> nach, weil sie es kann, ohne etwas zu verlieren. **Der Austritt aus dem Farbraum fällt dabei mit
> — als Nebenwirkung, nicht als Zweck.**
>
> | | hell | dunkel |
> |---|---|---|
> | bis 04.09.2026 | `oklch(0.96 0.028 27)`, gezeigt `#ffebe8` | `oklch(0.26 0.05 27)` |
> | **seither** | `oklch(0.96 0.016 22)` · **#fdeeed** | `oklch(0.26 0.035 22)` · **#331d1c** |
> | Chroma von der Decke | 0,016 von 0,0199 — **80 %** | 0,035 von 0,1049 — 33 % |
> | Überschuss | **0** *(war 0,021112)* | 0 *(war 0)* |
> | `--status-fehler` darauf | 5,30 → **5,38 : 1** *(Hexwert 5,39)* | 5,49 → **5,46 : 1** |
>
> **Der Dunkelblock lag immer im Farbraum** — der Austritt war ein Befund des hellen. Er ist
> trotzdem mitgewandert, und zwar **getrennt gerechnet und nicht gespiegelt**: Bei L 0.26 trägt
> sRGB bis 0,1049 Chroma, die Dämpfung ist dort also frei wählbar und folgt dem hellen Block im
> Verhältnis, nicht in der Zahl.
>
> **Die Ausnahme in `tests/farbkontrast.test.ts` ist entfallen statt gelockert.** Der Test fordert
> die sRGB-Bedingung jetzt für jeden Wert ohne Ausnahme — er ist **schärfer** geworden. Geprüft in
> beide Richtungen: Mit dem alten Wert läuft er rot.

### 4.4 Die neue Rolle `--verlauf-*` — beide Blöcke, getrennt gerechnet *(E‑87, 04.09.2026)*

**Der Dunkelblock ist hier keine Umkehrung des hellen, und an einer Stelle ist er es doch — mit
Grund.** Die **Fläche** trägt in beiden Blöcken denselben Wert, wie `--akzent` es tut: Sie ist nie
Schrift, und was von ihr gemalt wird, regelt ohnehin die Deckung darunter. Die **Kontur** kehrt
sich um, aus demselben Grund wie `--akzent-schrift`: Sie liegt auf der Grenze zwischen Fläche und
Karte, und welche der beiden die dunklere ist, hängt am Block.

| Token | hell | dunkel |
|---|---|---|
| `--verlauf-flaeche` | `oklch(0.62 0.118 230)` · #1992bf | **derselbe Wert** |
| `--verlauf-kontur` | `oklch(0.45 0.085 230)` · #0e5d7b | `oklch(0.78 0.11 230)` · #64c4f0 |
| `--verlauf-deckung-oben` | 0.35 | **0.28** |
| `--verlauf-deckung-unten` | 0.03 | **0.04** |

**Die beiden Deckungen sind der Teil, den der Dunkelblock wirklich für sich hat.** Dieselbe Deckung
trägt auf `--card` 0.21 *etwas* mehr auf als auf Weiß — bei 0,35 wären es 0,1597 OKLab gegen
0,1485, also acht Prozent. Mit 0,28 sind es **0,1287**; die Deckung geht damit über den Ausgleich
hinaus und stellt die dunkle Fläche dreizehn Prozent leiser als die helle. **Das ist eine
Entscheidung nach Augenschein und keine, die aus einer Zahl folgt** — sie steht hier als solche.
Der Fuß liegt umgekehrt höher (0,04 gegen 0,03), weil eine Tönung auf dunklem Grund schneller
verschwindet als auf hellem.

**Der Kontrast auf `--card`:** die Kontur 7,29 : 1 hell und 9,06 : 1 dunkel — die 3 : 1 aus WCAG
1.4.11 hängen an ihr und nicht an der Fläche. Der gemalte obere Stopp liegt bei 1,51 : 1 bzw.
1,47 : 1; **das ist ein Bericht und keine Bedingung**.

> #### ⚠️ Ein Befund über den **Rechenweg**, und er betrifft jede Deckung in dieser Datei
>
> Die erste Fassung von `ueber()` mischte im **linearen Licht**. Das ist die physikalisch richtige
> Art, Licht zu addieren — und es ist **nicht**, was der Browser tut: `color-interpolation` steht in
> SVG auf `sRGB`, und die Alphamischung läuft im gammakodierten Raum. Am 04.09.2026 in Chrome an
> drei Proben nachgesehen (SVG über `data:`-URL auf ein Canvas, Pixel ausgelesen):
>
> | | gemessen | sRGB | linear |
> |---|---|---|---|
> | `#1992bf` zu 28 % über `#181818` | **#183a46** | #183a47 | #18536d |
> | `#1992bf` zu 35 % über `#ffffff` | **#afd9e9** | #afd9e9 | #d3e1eb |
> | `#ffffff` zu 12 % über `#181818` | **#343434** | #343434 | #646464 |
>
> **Die dritte Zeile ist die teuerste:** Sie ist `--border` im Dunkelblock, also `oklch(1 0 0 /
> 12%)` über der Karte — die **Gitterlinie** des Verlaufsdiagramms. Die beiden Rechenwege liegen
> dort **0,19 in OKLab** auseinander, mehr als das Siebenfache der Sichtprobe. Wer hier linear
> mischt, beschreibt eine Linie, die niemand sieht.
>
> Umgestellt in `scripts/farbwerte/rechne.mjs` und in `tests/farbkontrast.test.ts`, mit der Messung
> als Kommentar an der Funktion. **Alle Zahlen dieses Abschnitts stammen aus dem Lauf nach der
> Umstellung.**

---

## 5. Die sechs vorregistrierten Schwellen und ihre Ergebnisse

**Sie standen fest, bevor eine Zahl bekannt war.** Hier unverändert, mit dem Ergebnis darunter.

| | Bedingung | Auslegung, wenn sie nicht hält | **Ergebnis** |
|---|---|---|---|
| **1** | Jeder Vordergrund ≥ 4,5 : 1 auf seiner Fläche, auf `--card` und auf `--background` | **Abbruchbedingung.** Der Wert wird geändert, bis sie hält | **Dunkel: gehalten, ohne eine Änderung** — fünf Vordergründe auf je drei Untergründen, 15 von 15 Bedingungen. **Hell: dreifach verfehlt** — der Wert ist geändert worden (§4.2) |
| **2** | Die vierzehn Gegenproben laufen durch | **Abbruchbedingung.** Weicht eine ab, ist die Methode die offene Frage. Melden, nicht weiterrechnen | **Gehalten**, vor und nach der Umbenennung, im Skript **und** im Test |
| **3** | `--ueberfaellig` und `--status-fehler` stehen je Block bei derselben Helligkeit | **Abbruchbedingung** (Q3) | **Gehalten.** L 0.52 hell, L 0.7 dunkel |
| **4** | Keine Strecke von einer Rolle zum Akzent fällt unter die heute gemessenen **0,117** | Verbesserung ist Ertrag. Verschlechterung ist **Abbruchbedingung** | **Gehalten.** Minimum unverändert 0,1170; die einzige bewegte Strecke ist **gewachsen** (0,2565 → 0,2802). ⚠️ **Bis zur Durchsicht am selben Tag war sie nur *berichtet* und nicht geprüft** — das dunkle Grün auf Ton 140 gedreht fällt auf 0,0823 und der Lauf endete trotzdem mit „Alle Bedingungen erfüllt". Seither ist sie im Skript eine echte Bedingung, gegen die **ungerundete** Untergrenze 0,1169659; gegen die gerundeten 0,117 fiele der Bestand an seiner eigenen Grenze durch |
| **5** | Die Konturen werden **berichtet**. Kein neues absolutes Ziel | Eine Kontur, die die 3 : 1 verfehlt, ist kein Fehlschlag dieser Runde | **Berichtet** (§3.4). Vier von fünf verfehlen sie in **beiden** Blöcken |
| **6** | Die Sichtprobe wird gefahren und ihr Befund ausgeschrieben — auch wenn er „nichts aufgefallen" lautet | **Ein weggelassener Befund ist kein Befund** | **Gefahren, und er lautet nicht „nichts aufgefallen"** (§6) |

**Zu Schwelle 4, damit die Zahl nicht überinterpretiert wird:** 0,117 ist der heutige Abstand
`--ueberfaellig` → `--akzent` im Dunkelblock (§7a, 31.08.2026). Das Vergleichsmaß aus §3 ist 0,343,
und **keine** Strecke zum Akzent erreicht im Dunkeln die Hälfte davon. Die Schwelle sagt deshalb
nicht „gut", sie sagt „nicht schlechter als heute". Ob 0,117 tragbar ist, entscheidet §6 und nicht
die Rechnung — und §6 hat dazu etwas zu sagen.

---

## 6. Die Sichtprobe *(M134, 03.09.2026)*

**Zahlen entscheiden diese Runde nicht allein.** Der bindende Präzedenzfall steht in
[`dashboard-frontend.md`](dashboard-frontend.md) §3: Dort sagten die Zahlen,
`--ueberfaellig-kontur` sei mit 3,29 : 1 die einzig saubere der fünf — und das Auge sagte, genau
sie mache aus zwei gleichrangigen Kacheln eine Rangfolge mit Orange vorn. **Entschieden hat das
Bild, nicht der Wert.**

### 6.1 Wie geprüft worden ist

Bauform A.2, dieselbe wie dort:

| | |
|---|---|
| **Aufbau** | temporäre Route `src/app/farbprobe-dunkel/page.tsx`, **außerhalb der Gruppe `(app)`**. Statische Zahlen, kein Netz, kein Backend |
| **Der Wrapper** | trägt `class="dark"` **fest verdrahtet** an einem `div`. Kein Umschalter, keine Server-Aktion, keine Datei unter `src/thema/`, kein `data-thema`. Das ist 11b |
| **Daneben** | derselbe Aufbau **ohne** die Klasse. Nur so ist zu sehen, ob der Dunkelblock dieselbe Ordnung trägt wie der helle — und der helle hat in dieser Runde selbst einen Wert geändert |
| **Browser** | das installierte Chrome, kopflos, über das DevTools-Protokoll. `deviceScaleFactor: 2`, für die Ausschnitte 3 |
| **Anmeldung** | keine. Die Routensperre prüft nur, ob ein Sitzungs-Cookie **da** ist ([`frontend-grundlagen.md`](frontend-grundlagen.md) §2); ein Platzhalter über `Network.setCookie` genügte |
| **Gezeigt** | die fünf Rollen als Plakette (alle drei Werte) **und** als Kachel (Fläche und Vordergrund, E‑u); eine **gemischte** Liste aus Fehler- und Überfällig-Zeilen; die aktive Navigationszeile; eine gefüllte Schaltfläche; der Fokusring; der gedrückte Zustand für Punkt 92 |
| **Der Fokusring** | mit der **Tastatur** ausgelöst (`Input.dispatchKeyEvent`, Tab), nicht mit `element.focus()` — `:focus-visible` greift sonst nicht zuverlässig. Am Ziel nachgeprüft: `matches(":focus-visible") === true` |

**Der Probecode ist entfernt.** Es bleibt dieser Befund.

### 6.2 ⚠️ Was zu sehen war — der Befund

**1. Überfällig liest sich im Dunkelblock lauter als Fehler.** In der gemischten Liste geht der
Blick auf die Gold-Plaketten und nicht auf die roten; bei den Kacheln ebenso. **Im hellen Block ist
es umgekehrt oder ausgeglichen.** Regel Q3 führt beide Kategorien gleichrangig — die Zahlen sagen
„gleichrangig" (beide Vordergründe bei L 0.7, beide Flächen bei L 0.26), das Auge sagt „Überfällig
vorn".

**Die Ursache ist messbar und keine Geschmacksfrage.** Bei identischer OKLab-Helligkeit trägt das
Gold mehr relative Luminanz als das Rot, und der WCAG-Kontrast zeigt es:

| auf | `--ueberfaellig` | `--status-fehler` | |
|---|---|---|---|
| `--card` **dunkel** | **6,53** | 6,16 | Überfällig trägt mehr |
| `--background` **dunkel** | **7,15** | 6,74 | Überfällig trägt mehr |
| `--card` **hell** | 5,59 | **6,08** | Fehler trägt mehr |
| `--background` **hell** | 5,35 | **5,82** | Fehler trägt mehr |

**Die Rangfolge kippt zwischen den Blöcken**, und die Gleichheit der Helligkeit — die eine
gestalterische Entscheidung, die §7a getroffen hat — trägt sie nicht. OKLab bildet den
Helmholtz-Kohlrausch-Effekt konstruktionsbedingt nicht ab: Ein gesättigtes Gelb wirkt bei gleicher
Helligkeit heller als ein Rot, und auf dunklem Grund schlägt das stärker durch.

**Was daraus folgt, folgt nicht aus der Farbe.** Genau wie bei Befund 1 in §7a: Jede Kachel und
jede Zeile trägt zusätzlich **Wort und Zeichen**, und die beiden Zeichen unterscheiden sich in der
Form, nicht in der Farbe — Warndreieck gegen Uhr. Die Farbe ist die halbe Aussage, und hier ist sie
es messbar. **Geändert wird trotzdem nichts:** Der Auftraggeber hat am 03.09.2026 entschieden, den
Dunkelblock unverändert zu lassen (E‑63), und die Runde hat keinen Auftrag, eine getroffene
Entscheidung nach der Probe umzudrehen. **Der Befund steht als offener Punkt 122** — 11b schaltet
den Block ein und hat ihn dann vor sich.

**2. Im Dunkelblock zieht die Abgeschlossen-Kachel den Blick zuerst.** Das dunkle Grün `#49c89b`
trägt 8,43 : 1 auf `--card`, das Rot `#f66d62` nur 6,16 : 1 — grün ist heller (L 0.75 gegen 0.7)
**und** kontrastreicher. Im hellen Block dominiert die Fehlerkachel. §1 des visuellen Konzepts
sagt: *„Das Werkzeug wird geöffnet, wenn etwas nicht stimmt."* Eine Übersicht, auf der
*„nichts zu tun"* am lautesten ist, liest sich dagegen. **Auch das ist Teil von Punkt 122.**

**3. Die Konturen bestätigen Befund 4 im Dunkeln.** `--ueberfaellig-kontur` ist auch dort die
einzige sichtbar gezeichnete; die vier anderen sind Andeutungen. Die Fassung `…OhneKontur` aus
E‑u trägt im Dunkelblock genauso, wie sie im hellen trägt.

**4. Der gedrückte Zustand ist im Dunkeln erkennbar, im Hellen kaum.** Der Kasten unter „24 h"
ist im Dunkelblock als Fläche zu sehen, im hellen praktisch nicht — 1,29 : 1 gegen 1,08 : 1.
Punkt 92 wird im Dunkeln **kleiner, nicht kleiner als spürbar**.

**5. Die gefüllte Schaltfläche ist im Dunkelblock erheblich präsenter.** `--akzent` bleibt
`#b9c022` und sitzt auf fast schwarzem Grund mit 8,97 : 1 statt 1,98 : 1. Beschriftung und
Lesbarkeit sind tadellos (9,13 : 1); die Frage ist die Lautstärke, nicht die Lesbarkeit. Der Akzent
trägt nur, was die Anwendung über sich selbst sagt, und eine gefüllte Schaltfläche ist im Rahmen
selten — **berichtet, nicht geändert**.

**6. Der geänderte helle Wert trägt.** `--status-ungeklaert` ist als Plakette und als Kachel gut
lesbar und bleibt sichtbar zurückhaltender als `--status-offen` daneben. Der Zweck der Rolle —
*„nicht zugeordnet heißt nicht zugeordnet"*, Regel Q4 — ist nicht verlorengegangen.

**7. Fokusring und aktive Navigationszeile: kein Befund.** In beiden Blöcken klar markiert und gut
lesbar; im Dunkeln trägt der Ring die aufgehellte Stufe `#c9d151` mit 10,72 : 1.

### 6.3 Der Belegvermerk *(Regel L10)*

> **Gemessen ist:** die Erhebung aus §3 (gerechnet), die Auflösung der Tokens am laufenden Chrome
> (drei unbunte Werte exakt bestätigt, dazu die beiden Zahlen aus `dashboard-frontend.md` §10.5),
> und zwei Bildpaare bei `deviceScaleFactor` 2 und 3.
>
> **Behauptet wird:** dass die Rangfolge zwischen Überfällig und Fehler im Dunkelblock kippt. Das
> ist eine **Aussage über einen Seheindruck**, gestützt auf vier gerechnete Kontraste, die in
> dieselbe Richtung zeigen — kein Wahrnehmungsversuch mit mehreren Betrachtern. Ein solcher hat
> nicht stattgefunden, und das ist die Grenze dieser Aussage.
>
> **Nicht geprüft ist:** die ganze Anwendung im Dunkeln. Gezeigt worden sind die Bausteine aus dem
> Auftrag; Diagramm, Prozessbaum, Formulare, Dialoge und die Rohdatenansicht sind im Dunkelblock
> **ungesehen** (offener Punkt 125).

---

## 7. Der Test — `tests/farbkontrast.test.ts`

Er liest `globals.css` als **Datei** — dieselbe Bauform wie `tests/dichte.test.ts`. **Keine zweite
Liste:** Die Tokens und ihre Werte kommen aus der Datei, die Schwellen aus dem Test.

### 7.1 Was zugesichert wird

| Was | Warum |
|---|---|
| jeder Vordergrund ≥ 4,5 : 1 auf seiner Fläche, auf `--card` und auf `--background`, in **beiden** Blöcken — und zwar **ungerundet und aus dem Hexwert** | die Bedingung, an der eine falsche Farbe scheitert. Ein Wert, der nur ungerundet darüber liegt, ist auf dem Bildschirm keiner |
| die drei Akzentstufen, die Schrift sind, ebenso. `--akzent` **nicht** | er ist eine Füllfarbe und erreicht auf Weiß 1,98 : 1 (§3). Ihn hier zu fordern hieße, das Farbsystem misszuverstehen |
| `--ueberfaellig` und `--status-fehler` stehen je Block bei **derselben Helligkeit** | Regel Q3, rechnerisch prüfbar |
| die **vierzehn** Gegenproben aus §3 und §7a, dazu die fünf Hexwerte aus §3 | ohne sie wäre die Methode und nicht das Ergebnis die offene Frage |
| jede der fünf Rollen ist in **beiden** Blöcken mit **allen drei** Werten deklariert | eine Rolle, die im Dunkelblock fehlt, fällt auf den hellen Wert zurück und niemand sieht es. Ein helles `--status-offen` `#525252` auf `--card` `#181818` erreichte **2,28 : 1** *(aus dem Hexwert 2,27)*, auf seiner dunklen Fläche 2,06 : 1 |
| **dieselbe Zusicherung für die vier Akzentstufen** | fehlte `--akzent-schrift` im Dunkelblock, stünde dort `#6a6f0f` auf der dunklen `--akzent-flaeche` `#2a2c02` — **2,68 : 1** statt 8,76 : 1, und die aktive Navigationszeile wäre unlesbar, weil `--accent-foreground` darauf zeigt. Auf `--card` wären es 3,28 : 1 — über der 3 : 1 aus WCAG 1.4.11 und für den *Fokusring* damit gerade noch tragbar; **die Fläche ist der Fall, der bricht** |
| **die Verdrahtung in `@theme inline`**: `--color-<token>` zeigt auf sein **eigenes** Token, für alle 19 | nachgereicht am 03.09.2026, siehe §7.5. Ohne sie prüft der Test einen Wert, den niemand mehr sieht |
| jeder Wert liegt im sRGB-Farbraum, mit **einer benannten und festgenagelten Ausnahme** | siehe §4.3. Übersprungen sähe sie irgendwann aus wie ein geprüfter Wert |
| beide Blöcke stehen **genau einmal auf der obersten Ebene** | die Lage, nicht nur der Wert. Ein `:root` in `@media (pointer: coarse)` gibt es in dieser Datei wirklich; ein `.dark` in `@media print` wäre ein Block, den niemand je sieht |
| die Konturen werden **berichtet, nicht zugesichert** | Befund 4 hält fest, dass vier von fünf die 3 : 1 verfehlen und dass **das der Befund ist**. Ein Test darauf wäre heute rot und würde eine getroffene Entscheidung umdrehen — die aus `dashboard-frontend.md` §3 (E‑u) |

### 7.2 ⚠️ Was er **nicht** kann — und T‑10 wird damit kleiner, nicht geschlossen

**Er hält Kontraste, nicht Farbwerte.** Der überlebende Mutant ist **gemessen und nicht ausgedacht**
(§7.4, Nr. 11): Im Dunkelblock `--status-abgeschlossen` von `oklch(0.75 0.13 166)` auf
`oklch(0.75 0.13 27)` — **Grün wird Rot**, gleiche Helligkeit, gleiche Chroma —, und der Lauf bleibt
grün. Dasselbe gilt im hellen Block für die beiden **neutralen** Rollen: `--status-offen` auf ein
kräftiges Blau und `--status-ungeklaert` auf ein Rot zu stellen überlebt ebenfalls. Die fachliche
Aussage *„Rot heißt Fehler"* ([`visuelles-konzept.md`](visuelles-konzept.md) §3) bleibt ungeprüft.

> **Was dabei unerwartet **doch** hält, und es ist kein Verdienst dieser Runde:** Ein Tonwechsel an
> `--status-fehler` fällt in **beiden** Blöcken, und `--ueberfaellig` fällt in beiden ebenfalls —
> die vierzehn Gegenproben nageln das helle Rot, das helle Grün und die vier Akzentstufen über die
> OKLab-Abstände fest, die sechs Hexwerte aus §7a das Orange in beiden Blöcken, und Regel Q3 bindet
> die Helligkeit von Rot an die von Orange. **Die Umfärbbarkeit endet also dort, wo eine Gegenprobe
> hinreicht** — nicht dort, wo jemand sie gezogen hätte. Genau deshalb ist „zur Hälfte" die
> ehrliche Angabe und nicht „fast ganz".

Die andere Hälfte bräuchte einen **Sollwert je Token** — und genau das nennt T‑10 als Grund dafür,
dass dort bisher nichts stand: eine zweite Pflegestelle. Dass sie offen bleibt, ist eine
Entscheidung und kein Rest. Fortgeschrieben in
[`testfestigkeit.md`](testfestigkeit.md) §6, **nicht gestrichen**.

**Ein Wirkungstest im Browser ist er ebenso wenig.** Er liest eine Datei und rechnet. Was die Werte
am Bildschirm tun, steht in §6 und nirgendwo sonst.

### 7.3 Der CSS-Leser wandert — `tests/hilfe/css-leser.ts` *(E‑64)*

Der Leser stand seit dem 01.09.2026 in `tests/dichte.test.ts`. Mit dem zweiten Verbraucher liegt er
in `tests/hilfe/css-leser.ts`; **kopiert worden ist er nicht.** Zwei Leser wären zwei
Pflegestellen, und die Verschärfung, die der eine bekommt, fehlte dem anderen.

**Der Beleg, dass die Verlagerung nichts geändert hat:** Die Aufrufstellen in `dichte.test.ts` sind
Zeichen für Zeichen dieselben geblieben — deshalb gibt `stilblatt()` die Helfer als Bündel zurück
und nicht als vier Ausfuhren mit neuer Signatur —, und seine Zusicherungen laufen unverändert
durch.

### 7.4 Die Gegenprobe — dreizehn Mutanten, in beide Richtungen geeicht *(M135)*

Jeder Mutant wird **einzeln** angewandt, der Test läuft, danach wird `globals.css` aus einer
Bytesicherung wiederhergestellt und die Prüfsumme verglichen. **Kein `git checkout`** — das würfe
alle unversionierten Änderungen derselben Datei mit weg.

**Zwei Mutanten müssen überleben, und aus zwei verschiedenen Gründen.** Nr. 0 ist folgenlos — die
Eichung nach oben, ohne die ein Läufer, der nur „gefallen" sagen kann, genauso aussieht wie einer,
der funktioniert ([`testfestigkeit.md`](testfestigkeit.md) §10.2). **Nr. 11 ist die offene Hälfte
von T‑10 selbst**, als Zusicherung geführt: Sie *soll* durchgehen, und sie steht hier, damit
niemand glaubt, der Test hielte Farbwerte. Fiele sie eines Tages, wäre das eine Nachricht und
kein Fehler.

| | Mutant | erwartet | Ergebnis | tote Zusicherungen |
|---|---|---|---|---:|
| 0 | ein Wort in einem **Kommentar** geändert | grün | **grün** | 0 |
| 1 | dunkel `--status-ungeklaert` 0.62 → 0.55 — alle drei Kontraste fallen, der bindende auf der eigenen Fläche auf **3,57** *(Hexwert 3,53)*; auf `--card` 3,65, auf `--background` 4,00 | rot | **rot** | 1 |
| 2 | dunkel `--status-fehler` L 0.7 → 0.75 (Regel Q3 bricht) | rot | **rot** | 2 |
| 3 | dunkel `--status-offen-kontur` gelöscht | rot | **rot** | 3 |
| 4 | dunkel `--akzent-schrift` gelöscht | rot | **rot** | 3 |
| 5 | **zweiter** `.dark`-Block am Dateiende | rot | **rot** | 20 |
| 6 | **zweite** `--status-fehler`-Deklaration im Dunkelblock | rot | **rot** | 4 |
| 7 | hell `--akzent-schrift` L 0.52 → 0.60 (Gegenprobe 5,40 aus §3 fällt) | rot | **rot** | 5 |
| 8 | hell `--status-fehler-flaeche` Chroma 0.028 → 0.05 (der Austritt wächst) | rot | **rot** | 1 |
| 9 | **`@theme inline`: `--color-status-fehler` zeigt auf `--status-abgeschlossen`** | rot | **rot** | 1 |
| 10 | **`@theme inline`: `--color-ueberfaellig` zeigt auf `--status-offen`** | rot | **rot** | 1 |
| 11 | **UMFÄRBUNG dunkel: `--status-abgeschlossen` Ton 166 → 27, Grün wird Rot** | **grün** | **grün** | 0 |
| 12 | dunkel `--ueberfaellig` auf `var(--status-fehler)` gezeigt | rot | **rot** | 5 |

**`globals.css` ist danach byte-gleich wiederhergestellt** — `sha256` vor und nach dem Lauf:
`0fdae7ef7f5989902a09a0cd1711a294df9356c98484e7fe6f6f144bce8f53ca`, identisch. Der Lauf ist nach
der letzten Änderung am Test wiederholt worden und **Zeile für Zeile gleich ausgefallen**.

`tests/farbwerte.test.ts` hat **keine neue Ausnahme** bekommen und ist grün.

### 7.5 ⚠️ Ein Loch, das eine Durchsicht am selben Tag gefunden hat

**Die erste Fassung dieses Tests las `:root` und `.dark` — und nie `@theme inline`.** Dort steht
aber die einzige Verbindung zwischen dem geprüften Token und der Klasse, die eine Komponente
schreibt. **Gemessen, nicht behauptet:**

| Mutant | farbkontrast | volle Suite |
|---|---|---|
| `--color-status-fehler: var(--status-fehler)` → `var(--status-abgeschlossen)` | **49/49 grün** | **812/812 grün** |
| `--color-ueberfaellig: var(--ueberfaellig)` → `var(--status-offen)` | **49/49 grün** | **812/812 grün** |

Jedes `text-status-fehler` im Projekt hätte danach die **grüne** Farbe getragen, und **jede Zahl
dieses Tests wäre weiter richtig gewesen**: Er hätte den Wert nachgerechnet, den niemand mehr
sieht. Genau das ist der Fehler, den [`visuelles-konzept.md`](visuelles-konzept.md) §2 ausschließen
soll — Farbe nur über Tokens ist nichts wert, wenn der Name auf ein anderes Token zeigt.

**Es ist derselbe Mutant, der in `tests/dichte.test.ts` „am weitesten trug"** —
`--spacing-beruehrung` auf ein anderes Token zeigen lassen, „ohne dass eine einzige Zahl in dieser
Datei falsch geworden wäre" ([`dichte-umschalter.md`](dichte-umschalter.md) §7). **Die Lehre lag
vor, die Anwendung fehlte.** Dass sie fehlte, ist der eigentliche Befund dieser Nachbesserung: Eine
bekannte Fehlerklasse schützt nicht davor, sie im nächsten Test wieder offenzulassen.

**Geschlossen** durch neunzehn Zusicherungen — je eine für jede der fünfzehn Rollenwerte und die
vier Akzentstufen —, die den Namen an **sein eigenes** Token binden und nicht bloß an irgendein
`var()`. Dazu die Lage: `@theme inline` steht genau einmal und unbedingt.

**Der Test zählt seither 69 Fälle statt 49**, die Mutantentabelle dreizehn Zeilen statt zehn.


---

## 8. Die Entscheidungen

### E‑61 — hier nur eingetragen, getroffen am 03.09.2026

> Nachgerechnet werden die **fünf Rollen** (je drei Werte) und die **vier Akzentstufen**. Die
> shadcn-Basistokens werden übernommen und sind der **Bezug**, nicht der Gegenstand.
>
> **Verworfen:** *Alles nachrechnen* — `components/ui` ist Generatorbereich, eine eigene
> Basispalette wäre eine zweite Pflegestelle gegen jedes künftige `shadcn add`. *Nur die fünf
> Rollen* — der Akzent ist ein Gelbgrün, auf dunklem Grund verschiebt sich seine Wirkung stärker
> als die der Statusfarben; ihn auszulassen hieße, ihn zu raten.

Dass die Zeile in §3.3 (`--akzent` auf `--card`: 1,98 gegen 8,97) genau diese Begründung bestätigt,
ist der Ertrag dieser Entscheidung und kein Zufall.

### Die Entscheidungen dieser Runde

| Nr. | Entscheidung | Warum nicht anders |
|---|---|---|
| **E‑62** | **Der helle Block wird mitkorrigiert.** `--status-ungeklaert` wandert von `oklch(0.58 0 0)` auf `oklch(0.55 0 0)`; alles Übrige im hellen Block bleibt unberührt | Schwelle 1 stand als Abbruchbedingung *„in beiden Blöcken"* fest, bevor eine Zahl bekannt war, und die Rolle ist Schrift. Die Alternative — Befund ohne Änderung, mit einer benannten Ausnahme im Test — hätte die Abgrenzung gewahrt und eine bekannte Unterschreitung im Bestand stehen gelassen. Der Auftraggeber hat am 03.09.2026 die Korrektur gewählt. **Die Abgrenzung „kein Byte am hellen Block" und Abnahmepunkt 7 fallen damit ausdrücklich**, und zwar an dieser einen Stelle |
| **E‑63** | **Der Dunkelblock bleibt unverändert.** Kein Wert wird angefasst | Gemessen hält er jede Schwelle. Teil 3 sagt: *„Für Rollen, die sie heute schon einhalten, ändere nichts."* Eine Änderung ohne Anlass wäre eine Umfärbung des Bestands — und die Sichtprobe hat zwar einen Befund geliefert (§6.2), aber keinen, der ohne eine Entscheidung über **alle fünf Rollen zugleich** aufzulösen wäre (§7a, Befund 4) |
| **E‑64** | **Der CSS-Leser liegt in `tests/hilfe/css-leser.ts`**; `dichte.test.ts` und `farbkontrast.test.ts` lesen denselben | Eine Kopie im neuen Test wäre eine zweite Pflegestelle gewesen — genau der Fehler, gegen den das Testwerk dieses Projekts gerichtet ist. Der Auftrag sagt: *„dockt an diese Bauform an und erfindet keine zweite"* |
| **E‑65** | **Ein bekannter sRGB-Austritt wird festgenagelt, nicht übersprungen.** Der Test prüft den *Überschuss* und nicht ein Ja/Nein | Eine übersprungene Ausnahme sieht nach zwei Runden aus wie ein geprüfter Wert. So darf sie bleiben, wo sie ist, und nicht wachsen |

**E‑59 und E‑60 gehören zu Schritt 11b** und werden hier nicht getroffen. Sie stehen im
Plan-Eintrag ([`IMPLEMENTIERUNGSPLAN_MVP.md`](IMPLEMENTIERUNGSPLAN_MVP.md), Schritt 11). Der Block
heißt in 11a weiterhin `.dark`; dass der Selektor mit E‑59 wechselt, ist in
[`visuelles-konzept.md`](visuelles-konzept.md) §8 datiert vermerkt — sonst liest ihn in 11b jemand
als Vorgabe und baut die Klasse.

---

## 9. Regelbezug

| Regel | Umsetzung |
|---|---|
| **Q3** Die drei Problemkategorien bleiben getrennt und **gleichrangig** | Der Test sichert zu, dass `--ueberfaellig` und `--status-fehler` je Block auf **derselben Helligkeit** stehen. Dass die Gleichrangigkeit damit nicht erreicht ist, steht als Befund in §6.2 und als offener Punkt 122 — getragen wird sie von Wort und Zeichen, nicht von der Farbe |
| **Q4** Nicht zugeordnet heißt nicht zugeordnet | `--status-ungeklaert` behält seine Rolle und seine Zurückhaltung; geändert ist nur die Helligkeit, und zwar bis zur Lesbarkeitsschwelle und nicht darüber hinaus |
| **§2** Alle Farben ausschließlich über CSS-Variablen, Zuordnung an genau einer Stelle | Die **Zuordnung** in `lib/status-farbe.ts` ist unberührt; angefasst ist dort **ein Kommentar**, der den umbenannten Skriptpfad nachzieht — zwei Zeilen, kein Codezeichen (`git diff` zeigt es). Dasselbe gilt für [`frontend-grundlagen.md`](frontend-grundlagen.md) §8a. `tests/farbwerte.test.ts` hat keine neue Ausnahme, und auch die temporäre Probe-Route kannte keinen Farbwert. **Neu zugesichert ist seit dem 03.09.2026 die Verdrahtung** `--color-<token>` auf `var(--<token>)` in `@theme inline` — ohne sie stand §2 an einer Stelle, die kein Test las (§7.5) |
| **§3** Status wird nie allein über Farbe ausgedrückt | Trägt in dieser Runde die ganze Last: Sowohl Befund 1 aus §7a als auch der neue Befund in §6.2 laufen darauf hinaus, dass die Farbe die halbe Aussage ist |
| **§8** „Nachrechnen ist wörtlich gemeint" | **38 Werte** (19 je Block), **52 Kontraste** (je Block 15 Vordergrund-Bedingungen, 4 Akzentbedingungen, 1 Akzentbericht, 5 Konturen, 1 für Punkt 92) und **50 OKLab-Abstände** (je Block 5 zum Akzent, 10 zwischen den Rollen, 10 zwischen den Flächen). Zweimal gerechnet — Skript und Test —, dazu am laufenden Browser gegengeprüft |
| **L10** Belegvermerk *„gemessen war X / behauptet wird Y"* | §4.2 und §6.3 |
| Generatorbereich wird nicht von Hand umgebaut | `components/ui` ist unberührt, kein `shadcn add` war nötig. Punkt 92 ist **gemessen und nicht behoben**, genau aus diesem Grund |

---

## 10. Offene Punkte

### Die Nummernvergabe, belegt

Die Hausregel der Vorrunden: Jeder vergebene Nummernkreis trägt seine Freiheitsprüfung mit
([`process-view.md`](process-view.md) §22, [`rollup.md`](rollup.md)). Gelaufen am 03.09.2026 gegen
den Stand **vor** dieser Runde:

| Kreis | Vergeben | Gegenprobe | Ergebnis |
|---|---|---|---|
| **Messungen** | M131–M135 | `git grep -nE 'M13[1-9]' HEAD` und dieselbe Suche für M14x–M19x | kein Treffer. Höchste vorher: **M130** ([`process-view.md`](process-view.md) §29) |
| **Entscheidungen** | E‑62–E‑65 *(E‑61 nur eingetragen, E‑59/E‑60 nur im Plan)* | ein Python-Durchlauf über `docs/*.md` und die Wurzeldateien, der jede Schreibweise `E<Strich><Zahl>` einsammelt und die größte nennt | kein Treffer über 58. Höchste vorher: **E‑58** ([`process-view.md`](process-view.md) §29) |
| **Offene Punkte** | 122–125 | über alle Dateien in `docs/` und die Markdown-Dateien im Wurzelverzeichnis | kein Treffer über 121 |

*Offene Punkte setzen bei **122** an. Projektweit höchster vergebener Stand war **121**
([`process-view.md`](process-view.md) **§22**, „Nummernvergabe (Teil 3)" — der Auftrag zu dieser Runde nennt dort „§2206", das ist die *Zeilen*nummer und kein Abschnitt; nachgesehen). Nachgezählt am 03.09.2026 über alle Dateien in
`docs/` und die Markdown-Dateien im Wurzelverzeichnis. **Achtung, dieselbe Falle wie dort:**
`nachrichtenliste.md` führt eine `**123**` und `messungen-schritt5.md` eine `**124**` — beides sind
Messwerte in Tabellen und keine Punkte.*

| Nr. | Punkt |
|---|---|
| **122** | **Im Dunkelblock kippt die Rangfolge zwischen Überfällig und Fehler, und Grün wird zur lautesten Kachel** (§6.2). Gemessen: `--ueberfaellig` trägt auf `--card` **6,53 : 1** gegen **6,16 : 1** beim Fehler, auf `--background` 7,15 gegen 6,74 — im hellen Block ist es umgekehrt. Ursache ist der Helmholtz-Kohlrausch-Effekt, den OKLab konstruktionsbedingt nicht abbildet; die gleiche Helligkeit (L 0.7) trägt die Gleichrangigkeit aus Regel Q3 deshalb **nicht**. Dasselbe in der zweiten Richtung: dunkles Grün trägt 8,43 : 1 und zieht den Blick vor dem Rot mit 6,16 : 1 — auf einer Übersicht, die geöffnet wird, *weil* etwas nicht stimmt. **Aufzulösen wäre es nur für alle fünf Rollen zugleich** (§7a, Befund 4), und dann mit einem Maß, das Sättigung einbezieht — nicht mit OKLab-Helligkeit allein. Gehört zu 11b, wenn der Block eingeschaltet wird |
| ~~**123**~~ | ~~**`--status-fehler-flaeche` liegt im hellen Block außerhalb des sRGB-Farbraums** (§4.3) und wird auf `#ffebe8` abgeschnitten — seit Schritt 3, unbemerkt. Der Überschuss ist in Skript und Test festgenagelt und kann nicht mehr unbemerkt wachsen; behoben ist er nicht. Eine Korrektur verschöbe den Rotton der Fehlerfläche im ganzen Bestand und ist eine eigene Entscheidung~~ — ✔ **geschlossen am 04.09.2026 (E‑88)**, und der Anlass war ein anderer: Die Fläche ist gedämpft worden, weil die neue Verlaufsfläche daneben steht; der Austritt fällt als Nebenwirkung mit. Der Wert liegt jetzt bei 80 % der Chroma-Decke, die **Ausnahme im Test ist entfallen statt gelockert** (§4.3) |
| **124** | **`--border` und `--input` tragen im Dunkelblock einen Alphaanteil** (`oklch(1 0 0 / 12%)` und `/ 16%`) und sind deshalb **nicht als deckende Farbe nachgerechnet**. Sie sind Bezug und nicht Gegenstand (E‑61), und der Test lässt jede Form, die er nicht kennt, ausdrücklich **fehlschlagen** statt sie zu überspringen — er liest sie nur nicht. Wer sie nachrechnen will, muss sie zuerst über `--card` und `--background` überlagern, und das ist eine Rechnung, die dieses Projekt noch nirgends führt |
| **125** | **Die Sichtprobe deckt nicht die ganze Anwendung ab.** Gezeigt worden sind die Bausteine aus dem Auftrag — Plakette, Kachel, Liste, Navigationszeile, Schaltflächen, Fokusring. **Ungesehen im Dunkelblock sind:** das Verlaufsdiagramm samt Legende und Tooltip (Recharts färbt über `var()`-Props, [`frontend-grundlagen.md`](frontend-grundlagen.md) §8a), der Prozessbaum mit seinen 1.158 Zeilen, Formulare und Eingabefelder, Dialoge und Schubladen, die Rohdatenansicht und die Benutzerverwaltung. Das ist keine Lücke dieser Runde — sie hatte den Auftrag, die Werte zu belegen —, aber es ist der Umfang, den 11b vor sich hat |

---

## 11. Was ausdrücklich **nicht** gebaut worden ist

- **Kein Umschalter.** Kein `src/thema/`, kein Cookie, keine Server-Aktion, kein Eintrag im
  Nutzermenü, keine Zeichenkette in `i18n/`. Das ist 11b
- **Kein `data-thema`.** Der Block heißt weiterhin `.dark`. Die Umzeigung ist E‑59 und gehört zu 11b
- **Die vier Konturen sind nicht aufgelöst.** Befund 4 aus §7a gilt unverändert, in beiden Blöcken
- **`components/ui` ist unberührt.** Generatorbereich
- **Offener Punkt 88 bleibt offen.** Ein Schritt, der eine Farbe definiert, ändert keine abgenommene
  Ansicht mit
- **Offener Punkt 92 ist gemessen, nicht behoben** (§3.7)
- **T‑7, T‑8, T‑9 sind nicht angefasst.** T‑10 ist fortgeschrieben, nicht gestrichen
- **Keine Datenbank.** Diese Runde hat keine Zeile gelesen

---
---

# Schritt 11b: die Mechanik *(03.09.2026)*

**Was diese Runde tut:** Sie schaltet den in 11a durchgerechneten Dunkelblock ein — dreiwertig,
ohne Aufblitzen, **ohne einen einzigen Farbwert anzufassen.**

> ### ⚠️ Das Ergebnis in drei Sätzen
>
> **Der Umschalter steht, und die Zusage aus [`frontend-grundlagen.md`](frontend-grundlagen.md) §8a
> hält:** Recharts zieht beim Themawechsel nach, ohne dass die Komponente neu montiert — 0
> Mutationen im Diagrammbaum, 49 von 49 Knoten identisch, die Farben gewechselt (§15).
>
> **Der Weg, den der Nutzer wirklich geht, nimmt diese Zusage trotzdem nicht in Anspruch:**
> `revalidatePath("/", "layout")` erneuert den ganzen Baum, und die 49 Balkenknoten werden dabei neu
> erzeugt. Die Farbe bräuchte das nicht. Offener Punkt **127**.
>
> **Kein aufgelöster Wert hat sich durch den Umbau geändert** — je Zustand **51 Werte**, 0
> Abweichungen, am laufenden Chrome vor und nach dem Umbau abgefragt (§13).

---

## 12. Was gebaut worden ist

### 12.1 Die Aufteilung — dieselbe wie bei Sprache und Dichte

```
src/thema/
├─ index.ts      drei Werte, Cookie-Name, Feldname, Dauer, definierter Rückfall
├─ server.ts     aktiver Wert für Server-Komponenten (`aktivesThema`)
├─ aktion.ts     Server-Aktion zum Umschalten (`themaSetzen`)
└─ provider.tsx  Kontext für Client-Komponenten (`useThema`)

src/components/thema-umschaltung.tsx   der Umschalter im Nutzermenü
src/app/globals.css                    die drei Werte und die beiden Dunkelzweige
```

**Ein eigener Ordner neben `i18n/` und `dichte/`, keine Datei in `lib/`** — die Begründung ist
Wort für Wort die aus [`dichte-umschalter.md`](dichte-umschalter.md) §6, und sie ist es, weil es
dieselbe Bauform ist: Eigenschaft des Nutzers, Cookie, serverseitiger Leseweg, Server-Aktion.

Dazu der harte Grund, der die Vierteilung erzwingt: **`"use server"` steht am Dateianfang von
`aktion.ts` und macht jeden Export zur Server-Aktion.** Die Konstanten können deshalb nicht neben
der Aktion wohnen.

| | Wert |
|---|---|
| Werte | `hell`, `dunkel`, `system` — in dieser Reihenfolge im Menü |
| Cookie | `overlord_thema`, ein Jahr, `SameSite=Lax`, kein `HttpOnly`, `path="/"` — **so steht es im Code**; gemessen am gesetzten Cookie ist davon nur das fehlende `HttpOnly` (Punkt **129**) |
| Rückfall | **`system`**, nicht `hell` (Regel Q4, siehe §12.5) |
| Beschriftung | „Erscheinungsbild" / „Appearance"; drei verschiedene Texte je Sprache |

### 12.2 Das Attribut *(E‑59, hier umgesetzt)*

`data-thema` steht am `<html>` und wird im Wurzel-Layout **serverseitig aus dem Cookie** gesetzt —
genau wie `data-dichte`, an demselben Element, in derselben Bauform. Das ausgelieferte Tag:

```html
<html lang="de" data-dichte="m" data-thema="dunkel" class="font-sans geist_…__variable geist_mono_…__variable">
```

Der Block `.dark` heißt seither `html[data-thema="dunkel"]`.

### 12.3 Die Werte stehen einmal *(E‑60, hier umgesetzt — die Form ist E‑66)*

*System* heißt, dass die Dunkelwerte an **zwei** Stellen wirken müssen: unter dem ausdrücklichen
Attribut **und** unter `@media (prefers-color-scheme: dark)`. Ein Selektor kann beide nicht zugleich
treffen — **eine Medienabfrage ist keine Selektorbedingung.**

Stünden die Werte deshalb in beiden Blöcken, gäbe es sie zweimal. Eine spätere Änderung an nur einem
wäre im anderen Zweig unsichtbar, *weil niemand beide Wege zugleich geht*. Genau der Fehler, gegen
den das Testwerk dieses Projekts gerichtet ist.

**Die Form:** Die 50 Dunkelwerte stehen **einmal** als Zwischenschicht `--dunkel-<token>` im
unbedingten `:root`; die beiden Zweige zeigen nur um.

```css
:root {
  /* … die hellen Werte … */
  --dunkel-status-fehler: oklch(0.7 0.17 27);   /* 50 Werte, genau einmal */
}

html[data-thema="dunkel"] {
  color-scheme: dark;
  --status-fehler: var(--dunkel-status-fehler); /* 50 Umzeigungen */
}

@media (prefers-color-scheme: dark) {
  html[data-thema="system"] {
    color-scheme: dark;
    --status-fehler: var(--dunkel-status-fehler); /* dieselben 50 */
  }
}
```

**Dass es dieselben 50 sind, ist zugesichert und nicht zugesagt** — `tests/thema.test.ts` vergleicht
beide Mengen in beide Richtungen und prüft zusätzlich, dass jede Umzeigung auf **ihr eigenes**
`--dunkel-`-Token zeigt (§18.2).

> **Der Preis, benannt statt verschwiegen:** Es sind 50 Werte plus zweimal 50 Umzeigungen statt
> zweimal 50 Werte — die Datei wird **länger**, nicht kürzer. Was sie gewinnt, ist die eine Stelle,
> an der ein Wert steht. Wer den nächsten Zweig braucht (etwa einen Kontrastmodus), schreibt eine
> dritte Umzeigeliste und **keinen** dritten Wertesatz.

### 12.4 `color-scheme` — und sie ist kein Beiwerk

| Zweig | `color-scheme` |
|---|---|
| `html[data-thema="hell"]` | `light` |
| `html[data-thema="dunkel"]` | `dark` |
| `@media (prefers-color-scheme: dark) html[data-thema="system"]` | `dark` |
| *System bei hellem Betriebssystem* | keine Angabe → `normal` (Chrome zeigt hell) |

**Warum `hell` eine eigene Regel bekommt, obwohl seine Werte in `:root` stehen:** wegen dieser einen
Zeile. Wer *hell* ausdrücklich gewählt hat, soll sie auch dann bekommen, wenn sein Betriebssystem
dunkel steht (E‑68). Gemessen: `data-thema="hell"` bei emulierter dunkler Systemeinstellung ergibt
`color-scheme: light` und den hellen Wertesatz (§16).

**Was ohne sie passierte, ist gemessen und nicht behauptet** — §17.1.

### 12.5 Der Rückfall fällt auf `system`

`themaAus` fällt bei unbekanntem oder fehlendem Wert auf **`system`** und ordnet **nicht** sinngemäß
zu: aus `dark` wird nicht `dunkel`, aus `DUNKEL` auch nicht, aus `nacht` erst recht nicht. Regel Q4
in klein — **und hier zusätzlich der inhaltlich richtige Rückfall:** Ein unbekannter Wert heißt
„keine Wahl getroffen", und keine Wahl ist genau das, was `system` bedeutet. Ein Rückfall auf `hell`
überginge die Auskunft, die das Betriebssystem schon gegeben hat.

Am ausgelieferten Dokument nachgewiesen (§14): `quatsch` → `system`, `dark` → `system`, kein Cookie
→ `system`.

### 12.6 Die Oberfläche

Ein Untermenü im **Nutzermenü der Kopfzeile**, direkt unter *Anzeigegröße* und ohne Trenner
dazwischen: Es sind zwei Fragen desselben Themas, und ein Trenner behauptete zwei. Der gemessene
Baum im geöffneten Untermenü:

```
menu
├─ div            (Angemeldet als / admin / EDI-Betreuung)
├─ separator
├─ menuitem  → submenu  (Anzeigegröße)
├─ menuitem  → submenu  (Erscheinungsbild)      ← neu
│  └─ form[role=none]
│     └─ div[role=group, aria-labelledby=<Auslöser>]
│        ├─ button[role=menuitemradio, name=thema, value=hell]
│        ├─ button[role=menuitemradio, name=thema, value=dunkel]
│        └─ button[role=menuitemradio, name=thema, value=system]
├─ separator
├─ a[role=menuitem]    (Passwort ändern)
└─ div[role=menuitem]  (Abmelden)
```

Gemessen am laufenden System: genau **ein** Eintrag trägt `aria-checked="true"`, die beiden anderen
`false`; das Häkchen steht bei den inaktiven Werten unsichtbar im Fluss, damit die Beschriftungen
beim Umschalten nicht hin- und herrücken.

**Kein zweites Zeichen je Zeile** *(E‑70)*: Naheliegend wären Sonne, Mond und Bildschirm an den drei
Einträgen. Der Haken ist in diesem Menü das Zeichen für *„das gilt"*, und ein zweites daneben machte
aus einer Auskunft zwei. Der Auslöser trägt eines (`SunMoon`), so wie der Dichteauslöser eines trägt.

**Warum ein Formular und keine Radiogruppe** — dieselbe Begründung wie beim Dichteumschalter, Wort
für Wort ([`dichte-umschalter.md`](dichte-umschalter.md) §4): `DropdownMenuRadioItem` schaltet über
`onValueChange` im Browser, und dieser Umschalter muss über eine **Server-Aktion** schalten.
Nachgeholt wird davon genau ein Attributpaar. **In `components/ui` ist nichts geändert und nichts
nachinstalliert worden.**

### 12.7 Die `dark`-Variante von Tailwind wird umgezeigt

`@custom-variant dark` stand auf `(&:is(.dark *))` und traf damit genau einen Zweig. Sie steht
seither in der **Blockform mit zwei `@slot`** *(E‑67)*:

```css
@custom-variant dark {
  &:is([data-thema="dunkel"], [data-thema="dunkel"] *) { @slot; }
  @media (prefers-color-scheme: dark) {
    &:is([data-thema="system"], [data-thema="system"] *) { @slot; }
  }
}
```

**Ohne sie bliebe im System-Fall der eigene Bestand dunkel und der Generatorbereich hell** — und
niemand sähe es, weil niemand beide Wege zugleich geht. Dass sie wirklich beide trifft, ist im
**gebauten** CSS nachgezählt und an einem echten Baustein am laufenden System gemessen (§17.2).

---

## 13. Teil 3 belegt: **kein aufgelöster Wert hat sich geändert** *(M139)*

Die Umstellung verschiebt 50 Werte aus `.dark` nach `:root` und legt zwei Umzeigelisten darüber.
**Das darf am Bildschirm nichts ändern**, und „darf nicht" ist keine Messung. Also gemessen:

| | |
|---|---|
| **Wie** | Das installierte Chrome, kopflos, über das DevTools-Protokoll; `/anmeldung` als Träger des vollständigen Stilblatts. Abgefragt wird `getComputedStyle(document.documentElement)` für jedes Token |
| **Vorher** | Der Dunkelzustand über `classList.add("dark")` — der einzige Weg, der vor dem Umbau dorthin führte |
| **Nachher** | Derselbe Zustand über `setAttribute("data-thema", "dunkel")` |
| **Dazu** | Derselbe Vergleich für den **hellen** Zustand |

**Ergebnis, Zeichen für Zeichen verglichen:**

| Zustand | abgefragte Namen | **davon mit Wert** | **Abweichungen** |
|---|---:|---:|---:|
| dunkel | 69 | **51** | **0** |
| hell | 69 | **51** | **0** |

**Die 51 sind die Zahl, auf die es ankommt** — die übrigen 18 lösen zu nichts auf und wären als
„verglichen" gezählt eine geschönte Zahl (siehe unten).

Dazu unverändert: die Schriftfarbe des `<body>` (`lab(96.52 …)` dunkel, `lab(3.6999 0 0)` hell) und
seine Hintergrundfarbe (`lab(3.6999 0 0)` dunkel, `lab(98.26 0 0)` hell).

**Die eine Größe, die sich ändert, ist die beabsichtigte:** `color-scheme` geht von `normal` auf
`dark` bzw. auf `light`. Vor dem Umbau stand sie nirgends in der Datei.

> **Der Vergleich ist in beide Richtungen geeicht.** Ein künstlich verfälschter Wert
> (`--status-fehler` auf `lab(1% 0 0)`) wird als genau eine Abweichung gemeldet. Ohne diese Eichung
> sähe ein Vergleich, der immer „gleich" sagt, genauso aus wie einer, der funktioniert
> ([`testfestigkeit.md`](testfestigkeit.md) §10.2).

### ⚠️ Achtzehn der 69 Namen lösen zu nichts auf — und das gehört in die Zahl

Abgefragt sind **69** Namen: die **50** des Dunkelsatzes und die **19** `--color-*`-Namen aus
`@theme inline`. Davon liefern **51** einen Wert und **18** eine leere Zeichenkette — in **jedem**
der vier Läufe dieselben achtzehn, und es sind ausschließlich `--color-*`-Namen. Der Grund ist
`@theme inline`: Tailwind **setzt** diese Namen beim Bauen ein, statt sie als Eigenschaft
auszuliefern; nur die, die eine gebaute Regel per `var()` wirklich braucht, stehen im Dokument.

**„69 verglichen, 0 Abweichungen" wäre deshalb geschönt** — achtzehn dieser Vergleiche sind *leer
gegen leer* und können gar nicht fehlschlagen. Die tragende Zahl ist **51**.

**Dass die achtzehn vorher wie nachher dieselben sind, ist trotzdem ein Ergebnis** und keine
Fußnote: Hätte der Umbau die Verdrahtung in `@theme inline` verschoben, stünde hier eine andere
Zahl. Sie ist die aus dem Lauf vor dem Umbau.

### 13.1 Und derselbe Nachweis über den Diff

Der Zeilendiff zeigt 50 gelöschte und 50 neue Zeilen und beantwortet die Frage nicht. Verglichen
sind deshalb die **Werte**, Token für Token, gegen `git HEAD` (den Stand **vor 11a und 11b**):

| Wertesatz | verglichene Tokens | Abweichungen |
|---|---:|---|
| hell (`:root`) | 70 | **1** — `--status-ungeklaert` `oklch(0.58 0 0)` → `oklch(0.55 0 0)` |
| dunkel (`.dark` → `--dunkel-*`) | 50 | **0** |

**Die eine Abweichung ist die von 11a** (E‑62, §4.2) und nicht die dieser Runde. Gegen die Sicherung
vom Beginn dieser Sitzung verglichen — also gegen den Stand **nach 11a** — hat der helle Wertesatz
**0 Abweichungen** in 70 Tokens. Auch dieser Vergleich ist gegengeprobt: eine künstliche Änderung an
`--akzent` wird gemeldet.

---

## 14. M136 — kein Aufblitzen: `data-thema` steht an **Byte 47**

Bauform wie E‑x, wo `data-dichte` an Byte 31 nachgewiesen ist
([`dichte-umschalter.md`](dichte-umschalter.md) §5.5). Gemessen am ausgelieferten Dokument von
`/anmeldung`, nicht angesehen:

| Cookie `overlord_thema` | ausgeliefert | Byte |
|---|---|---:|
| `hell` | `data-thema="hell"` | **47** |
| `dunkel` | `data-thema="dunkel"` | **47** |
| `system` | `data-thema="system"` | **47** |
| `quatsch` | `data-thema="system"` | **47** |
| `dark` *(die alte Klasse)* | `data-thema="system"` | **47** |
| *keins* | `data-thema="system"` | **47** |

**Und was danach kommt:**

| | Byteposition |
|---|---:|
| `data-dichte` | 31 |
| **`data-thema`** | **47** |
| erstes `<link>` | 266 / 268 |
| erstes `stylesheet` | 277 / 279 |
| erstes `<script>` | 578 / 580 |

Das Attribut steht **im `<html>`-Tag selbst**, vor jedem Stylesheet und jedem Skript. Es gibt keinen
Zeitpunkt, zu dem der Browser ein anderes Erscheinungsbild kennt als das endgültige. **Mit
`localStorage` gäbe es ihn bei jedem Aufruf** — das ist E‑x, und es gilt hier wörtlich.

---

## 15. M137 — **Recharts zieht ohne Neurendern nach.** Die Zusage aus §8a hält

[`frontend-grundlagen.md`](frontend-grundlagen.md) §8a schließt den Weg über `getComputedStyle` zur
Laufzeit mit dieser Begründung aus:

> „… ein späterer Dunkelmodus käme ohne Neurendern nicht nach."

**Diese Zusage ist nie geprüft worden, und diese Runde ist der Anlass.** Sie hält.

### 15.1 Wie gemessen worden ist

Am **laufenden, angemeldeten System** (Mandant NEXANS, Übersichtsseite, 48-Stunden-Fenster, 49
Balkensegmente im Verlaufsdiagramm), im Chrome des Auftraggebers über die Erweiterung.

Drei Messmittel zugleich, weil jedes für sich zu wenig sagt:

| | Was es beantwortet |
|---|---|
| **Marke am Knoten** (`path.__m137 = "marke-<i>"`) | *Sind es dieselben DOM-Knoten?* Würde React sie neu erzeugen, wäre die Marke weg. Ein Attributvergleich kann das nicht leisten |
| **`MutationObserver`** über den ganzen Diagrammbaum (`childList`, `subtree`, `attributes`, `characterData`) | *Ist irgendetwas angefasst worden?* |
| **`getComputedStyle(path).fill`** | *Hat sich die Farbe geändert?* |

**Umgeschaltet wird durch Setzen des Attributs am Wurzelelement** — ohne React, ohne Server-Aktion,
ohne Neurendern. Was sich dabei ändert, ändert allein die Kaskade.

### 15.2 Das Ergebnis

| | dunkel | hell |
|---|---|---|
| Balken **Fehler**, `fill`-Attribut | `var(--status-fehler)` | **unverändert** |
| Balken **Fehler**, aufgelöst | `lab(63.5691 52.8643 33.3674)` | `lab(42.4236 59.8149 41.9956)` |
| Balken **Offen**, aufgelöst | `lab(74.48 0 0)` | `lab(35.04 0 0)` |
| **Achsenbeschriftung** (`fill="var(--muted-foreground)"`) | `lab(65.2 0 0)` | `lab(42 0 0)` |
| **Gitterlinie** (`stroke="var(--border)"`) | `lab(100 0 0 / 0.12)` | `lab(88.4 0 0.0000119209)` |
| **Mutationen im Diagrammbaum** | **0** | |
| **Balkenknoten mit erhaltener Marke** | **49 von 49** | |

**Die beiden hellen Werte sind Ziffer für Ziffer die, die §8a am *Pixel* gelesen hat:**
`lab(42.4236 59.8149 41.9956)` ist dort `#be2323`, `lab(88.4 0 0.0000119209)` ist dort `#dedede`.
Die Kette *Prop → aufgelöster Wert → gemaltes Pixel* ist damit an genau diesen Knoten schon belegt;
diese Runde misst das erste Glied unter Themawechsel, nicht das letzte noch einmal.

> **Belegvermerk (Regel L10).** **Gemessen ist** der aufgelöste Wert (`getComputedStyle`) an den
> Knoten des laufenden Diagramms, dazu die Zahl der Mutationen und die Knotenidentität.
> **Behauptet wird** damit, dass sich auch das *Pixel* ändert. Das ruht auf der Messung vom
> 31.08.2026 (§8a), die für dieselben Knoten aufgelösten Wert und Pixel gleichgesetzt hat, **nicht
> auf einer eigenen Pixelmessung dieser Runde** — dafür hätte es das DevTools-Protokoll gebraucht,
> und die Sichtprobe lief über die Erweiterung. Der Seheindruck bestätigt es (§19).

### 15.3 ⚠️ Der Gegenbefund, und er gehört daneben

**Der Weg, den der Nutzer wirklich geht, nimmt die Zusage nicht in Anspruch.** Derselbe Wechsel über
den Menüeintrag, mit denselben drei Messmitteln:

| | Attribut von Hand | über den Menüeintrag |
|---|---:|---:|
| Mutationen im Diagrammbaum | **0** | **6** (`childList`) |
| Balkenknoten mit erhaltener Marke | **49 / 49** | **0 / 49** |
| Farben gewechselt | ja | ja |

`themaSetzen` ruft `revalidatePath("/", "layout")`, und das erneuert den ganzen Baum — die 49
Balkenknoten werden dabei neu erzeugt. **Die Farbe bräuchte das nicht.** Das ist derselbe
Zusammenhang, den [`dichte-umschalter.md`](dichte-umschalter.md) als offene Punkte 100 und 101
führt, an einer neuen Stelle sichtbar. Geführt als offener Punkt **127**.

**Was das für §8a heißt:** Die Begründung von Schritt 3 ist **bestätigt und nicht widerlegt** — der
`var()`-Weg trägt einen Themawechsel ohne Neurendern, der Laufzeitweg über `getComputedStyle` täte
es nicht. Dass der heutige Umschalter trotzdem neu rendert, ist eine Eigenschaft der
**Server-Aktion** und keine der Farbe.

---

## 16. M138 — der System-Zweig greift beim **ersten Paint**

Bei `data-thema="system"` und emulierter dunkler Systemeinstellung
(`Emulation.setEmulatedMedia` über CDP) steht der Dunkelzustand, bevor irgendetwas gemalt wird.

Ein Skript über `Page.addScriptToEvaluateOnNewDocument` läuft am **Dokumentanfang** und nimmt
Proben: sofort, beim ersten `<html>`, bei `DOMContentLoaded`, im ersten `requestAnimationFrame` und
an den `paint`-Einträgen der Performance-API.

**System + Betriebssystem dunkel:**

| Probe | `data-thema` | `--background` | `color-scheme` | seit |
|---|---|---|---|---:|
| Dokumentanfang | — | *(noch kein `<html>`)* | — | — |
| **erstes `<html>`** | `system` | **`lab(3.6999% 0 0)`** | **`dark`** | 137,8 ms |
| DOMContentLoaded | `system` | `lab(3.6999% 0 0)` | `dark` | 137,9 ms |
| erstes `requestAnimationFrame` | `system` | `lab(3.6999% 0 0)` | `dark` | 141,2 ms |
| **first-paint** | `system` | `lab(3.6999% 0 0)` | `dark` | 266 ms |
| **first-contentful-paint** | `system` | `lab(3.6999% 0 0)` | `dark` | 266 ms |

**Ein einziger Wert über alle Proben.** Es gibt keinen Zeitpunkt, zu dem etwas anderes dasteht — und
zwar 128 ms bevor der erste Paint stattfindet.

### 16.1 Vier Gegenproben, ohne die die Zahl nichts sagte

| Lage | `data-thema` | Ergebnis |
|---|---|---|
| System + Betriebssystem **hell** | `system` | durchweg `lab(98.26% 0 0)`, `color-scheme: normal` |
| **kein Cookie** + Betriebssystem dunkel | `system` *(Rückfall)* | durchweg dunkel — der Erstbesucher mit dunklem Gerät landet dunkel |
| **`hell` gewählt** + Betriebssystem dunkel | `hell` | durchweg hell, `color-scheme: light` — die Wahl schlägt das Gerät |
| **`dunkel` gewählt** + Betriebssystem hell | `dunkel` | durchweg dunkel, `color-scheme: dark` |

### 16.2 Und dasselbe am Pixel, Einzelbild für Einzelbild

Die Probenreihe sagt etwas über den *aufgelösten Wert*. Ob dabei ein helles Bild aufblitzt, sagt sie
nicht. Also `Page.startScreencast` über den ganzen Ladevorgang, jedes Einzelbild zurück in die Seite
als `data:`-URL, auf eine Leinwand gemalt, mittlere Helligkeit gelesen — dieselbe Bauform wie „das
Pixel" in [`frontend-grundlagen.md`](frontend-grundlagen.md) §8a:

| Lauf | Einzelbilder | mittlere Helligkeit (0–255) | davon **hell** (> 128) |
|---|---:|---|---:|
| Betriebssystem **dunkel** | 5 | 18 · 15,9 · 15,9 · 15,9 · 15,9 | **0** |
| Betriebssystem **hell** *(Eichung)* | 5 | 255 · 248,6 · 248,6 · 248,5 · 248,5 | 5 |

**Kein einziges helles Einzelbild im dunklen Lauf**, und die Eichung nach oben zeigt, dass der
Melder überhaupt etwas zu sagen hat. Bemerkenswert ist das **erste** Bild: Es ist bereits dunkel
(18), nicht weiß — `color-scheme: dark` färbt auch die Leinwand des Browsers, bevor die Seite steht.

### 16.3 Bestätigt außerhalb der Emulation

Der Rechner des Auftraggebers steht auf dunkel (`matchMedia("(prefers-color-scheme: dark)")` meldet
`true`). Mit `data-thema="system"` am laufenden, angemeldeten System: `color-scheme: dark`,
`--status-fehler` = `lab(63.5691% 52.8643 33.3674)`, Körperhintergrund `lab(3.6999 0 0)` — **Ziffer
für Ziffer derselbe Zustand wie im ausdrücklichen Zweig.**

---

## 17. Zwei Gegenproben zur Mechanik *(M140)*

### 17.1 `color-scheme` — was ohne sie passiert

Teil 2 verlangt sie mit der Begründung, ohne sie blieben native Bedienelemente hell. **Statt das zu
glauben, ist es ausgelöst worden:** am laufenden dunklen System `color-scheme` auf `normal` gestellt,
sonst nichts geändert.

| | Bildlaufleiste am rechten Rand |
|---|---|
| `color-scheme: dark` | dunkle Spur, heller Griff |
| `color-scheme: normal` | **weiße Spur** — neben einer fast schwarzen Seite |

Kein Farbtoken hat sich dabei bewegt. **Die Zeile tut also genau das, wofür sie dasteht**, und der
Fehler, den sie verhindert, wäre erst im Betrieb aufgefallen — an einer Stelle, die niemand in
`globals.css` sucht.

### 17.2 Die `dark`-Variante — trifft sie wirklich **beide** Zweige?

**Erstens im gebauten CSS**, nachgezählt an der vom Entwicklungsserver ausgelieferten Datei
(110.523 Bytes):

| | ausdrücklicher Zweig | System-Zweig |
|---|---:|---:|
| **verschiedene `dark:`-Selektoren** | **15** | **15** |
| Mengen gleich (nach Ersetzen des Zweignamens) | **ja** | |
| `html[data-thema="…"]`-Block je Wert | genau **1** (hell, dunkel, system) | |

> **Warum hier die Zahl der *Selektoren* steht und nicht die der Regeln.** Zwei Auszüge derselben
> Datei kamen auf **31** und auf **26** Regeln, weil sie eine mehrzeilige Selektorliste verschieden
> zerschneiden. **Die Zahl der verschiedenen Selektoren ist gegen diesen Unterschied unempfindlich
> und in beiden Auszügen dieselbe: 15 je Zweig.** Eine Zahl, die von der Schnittweise abhängt,
> steht hier nicht — sie sähe genauer aus, als sie ist.

Beispiel aus der Datei, unverändert:

```css
.dark\:border-input:is([data-thema="dunkel"], [data-thema="dunkel"] *) { border-color: var(--input); }

@media (prefers-color-scheme: dark) {
  .dark\:border-input:is([data-thema="system"], [data-thema="system"] *) { … }
}
```

> **In beide Richtungen geeicht.** Entfernt man einen der 15 Selektoren aus der einen Menge, meldet
> der Vergleich sie als ungleich. Ein erster Anlauf verglich Listen **mit** Dubletten und meldete
> „gleich", nachdem eine Zeile entfallen war — das wäre ein blinder Beleg gewesen und steht hier,
> damit der nächste Lauf nicht darauf hereinfällt.

**Zweitens an einem echten Baustein**, am laufenden System. Der `Switch` aus `components/ui` trägt
`dark:data-unchecked:bg-input/80` — eine Klasse, die es **nur** im Dunkelzweig gibt, und deren `/80`
sich am Alphawert ablesen lässt:

| `data-thema` | `--input` | Hintergrund des Schalters |
|---|---|---|
| `hell` | `lab(87.24% 0 0)` | `lab(87.24 0 0)` — deckend, also die **helle** Regel |
| `dunkel` | `lab(100% 0 0 / .16)` | `oklab(1 … / 0.128)` = 0,16 × 0,8 — also die **`dark:`**-Regel |
| `system` *(Gerät dunkel)* | `lab(100% 0 0 / .16)` | `oklab(1 … / 0.128)` — **derselbe Wert** |

**Damit ist die Zusage nicht nur im Quelltext und im Erzeugnis, sondern am Bildschirm belegt** — und
zwar für beide Zweige. Das ist der Fehler, den Teil 2 als unsichtbar bezeichnet: Die eigenen Tokens
sähen in jedem Fall richtig aus.

---

## 18. Die Tests

### 18.1 `tests/farbkontrast.test.ts` — **69 Fälle, unverändert**

Der Test las `:root` und `.dark`. Der Dunkelzustand hat jetzt **zwei Zweige**, und die Werte stehen
in keinem von beiden — sie stehen als `--dunkel-*` unter `:root`. Der Test liest sie deshalb über
ein **Präfix** statt über einen zweiten Selektor *(E‑69)*:

```ts
const BLOECKE = [
  ["hell", ""],
  ["dunkel", "dunkel-"],
] as const;
```

| | vorher | nachher |
|---|---:|---:|
| Fälle | 69 | **69** |
| die vierzehn Gegenproben | laufen | **laufen** |
| die neunzehn Verdrahtungszusicherungen aus §7.5 | halten | **halten** |

**Die Zahlen haben sich nicht bewegt** — nachgezählt mit `--reporter=verbose`: 69 Fälle, 69 grün.
Hätte sich eine bewegt, hätte Teil 2 mehr getan als umbenannt.

Zwei Zusicherungen haben ihre Formulierung nachgezogen und ihren Gehalt behalten: *„liest zwei
verschiedene Blöcke"* heißt jetzt *„liest zwei verschiedene Wertesätze"* und vergleicht weiterhin die
Helligkeit von `--card` und `--background` gegen `--dunkel-card` und `--dunkel-background`.

### 18.2 `tests/thema.test.ts` — neu, **23 Fälle**

Bauform wie `dichte.test.ts`, mit **demselben** CSS-Leser aus `tests/hilfe/css-leser.ts`. **Keine
zweite Kopie** (E‑64): Die Verschärfung, die der eine bekommt, fehlte dem anderen.

**Die Arbeitsteilung mit `farbkontrast.test.ts` ist ausdrücklich:** dort die **Zahlen**, hier der
**Weg**. Ein Wert, der stimmt, und ein Zweig, der ihn nicht liest, sähen ohne diese Teilung gleich
aus.

| Was zugesichert wird | Warum |
|---|---|
| **beide Dunkelzweige setzen dieselbe Tokenmenge**, in beide Richtungen verglichen | Ein Token, das nur in einem steht, ist im System-Fall hell und im ausdrücklichen dunkel — und niemand sieht es |
| **jede Umzeigung zeigt auf ihr *eigenes* `--dunkel-`-Token** | Der Mutant aus §7.5, hier vorweggenommen: `--card: var(--dunkel-background)` wäre eine gültige Deklaration, ein sauberer Kontrast und die falsche Farbe |
| jedes `--dunkel-*` steht **genau einmal**, unbedingt, unter `:root` | Bei zwei Deklarationen gewänne die Kaskade die zweite und ein Textleser die erste |
| **kein `--dunkel-*` ohne Abnehmer, kein Abnehmer ohne `--dunkel-*`** | Ein Wert, den niemand liest, und eine Umzeigung, die auf nichts zeigt und auf den **hellen** Wert zurückfällt |
| der ausdrückliche Zweig hängt **unbedingt** am Wurzelelement | keine Medienabfrage, kein `@supports`: Wer `dunkel` wählt, bekommt es |
| der System-Zweig steht **genau** unter `@media (prefers-color-scheme: dark)` | Das ist der Grund, aus dem *System* ohne Aufblitzen auskommt (E‑60). Stünde dort etwas anderes, wäre die Begründung falsch oder der Zweig unerreichbar |
| `@custom-variant dark` steht **genau einmal** und trifft **beide** Zweige, jeder mit `@slot` | Sonst bleiben vierzig Generatorbausteine im System-Fall hell |
| `color-scheme` ist in **beiden** Zweigen `dark`, und für `hell` steht `light` | siehe §17.1 |
| **kein Themablock hängt unter `@media print`** | Dieselbe Falle, die `dichte.test.ts` §7 für die Stufenregeln stellt |
| Werte im CSS == `THEMAWERTE` im Code, **in beide Richtungen** | Ein Wert ohne Regel wäre ein Menüeintrag, der nichts tut; eine Regel ohne Wert ein Zustand, den niemand erreicht |
| `themaAus` fällt auf `system` — auch bei `DUNKEL`, `Dunkel`, `dark`, `light`, `nacht`, `auto`, `HELL`, `""`, `" "`, `"0"` | Regel Q4 in klein |
| beide Sprachdateien tragen alle drei Werte, mit **drei verschiedenen** Texten je Sprache | `tests/sprachdateien.test.ts` fängt Abweichungen, aber nur bei angelegten Schlüsseln |

**Die Einmaligkeit des `@custom-variant` wird über den Dateitext geprüft und nicht über die Regeln**,
und das hat einen Grund: Die **Kurzform** `@custom-variant dark (…);` endet mit einem Semikolon und
wird vom Leser gar nicht erst zu einer Regel. Stünde sie zusätzlich da, gewänne je nach Reihenfolge
die eine oder die andere.

### 18.3 Die Gegenprobe — **sieben Mutanten, in beide Richtungen geeicht**

Jeder Mutant einzeln angewandt, danach Wiederherstellung aus einer **Bytesicherung** mit
Prüfsummenvergleich — **kein `git checkout`**, das würfe alle unversionierten Änderungen derselben
Datei mit weg.

| | Mutant | erwartet | **Ergebnis** | tote Zusicherungen |
|---|---|---|---|---:|
| **0** | ein Wort in einem **Kommentar** geändert | grün | **grün** | 0 |
| 1 | ein Token aus dem System-Zweig gelöscht (`--status-fehler-kontur`) | rot | **rot** | 1 |
| 2 | `@custom-variant dark` zeigt nur auf den ausdrücklichen Zweig | rot | **rot** | 1 |
| 3 | `color-scheme` im System-Zweig gelöscht | rot | **rot** | 1 |
| 4 | ein Wert aus `THEMAWERTE` entfernt, CSS unverändert | rot | **rot** | 1 von 21 |
| 5 | der Dunkelblock nach `@media print` verschoben | rot | **rot** | 2 |
| 6 | `themaAus` fällt auf `hell` statt `system` | rot | **rot** | 1 |

**Mutant 0 überlebt**, und das ist die Eichung nach oben: Ohne sie sieht ein Läufer, der nur
„gefallen" sagen kann, genauso aus wie einer, der funktioniert
([`testfestigkeit.md`](testfestigkeit.md) §10.2).

**Beide berührten Dateien sind danach byte-gleich** — `sha256` vor und nach dem Lauf, identisch:

| Datei | `sha256` |
|---|---|
| `frontend/src/app/globals.css` | `b97c95f91e9a8750310b7df4a9dde1eded3ee533eff9cb885ba564125736d229` |
| `frontend/src/thema/index.ts` | `e87929d9594cb456813590ee26a9aa38ba15eba1b241e5f583f8e5003dd3d641` |

Der Lauf ist zweimal gefahren worden und **Zeile für Zeile gleich ausgefallen**.

### 18.4 `tests/hilfe/css-leser.ts` — eine Zeile, und sie ist begründet

`bedingt()` erkennt eine Regel als „gilt nicht immer", wenn im Pfad `@media`, `@supports` **oder der
Dunkelzustand** steht. Der hieß `.dark` und heißt jetzt `[data-thema=…]`. **Beide Schreibweisen
bleiben stehen**, und zwar nicht aus Nachlässigkeit: Die Zusicherung, die daran hängt, lautet *„eine
Dichtestufe unter dem Dunkelzustand wirkt am Bildschirm nie"*, und sie soll auch dann greifen, wenn
jemand die alte Klasse wieder einführt. `tests/dichte.test.ts` läuft unverändert durch.

---

## 19. Die Sichtprobe *(M141, 03.09.2026)*

**Am laufenden, angemeldeten System** und nicht auf einer Probeseite.

| | |
|---|---|
| **Browser** | das Chrome des Auftraggebers, über die Erweiterung. Fenster **1568 × 726** — die Größe ließ sich nicht setzen (bekannte Einschränkung), gemessen wurde in dieser einen |
| **Anmeldung** | echt, als `admin`, Mandant **NEXANS**, Dev-Anker `2025-12-30 04:09:47` |
| **Zustände** | alle drei: `hell`, `dunkel`, `system`. Das Betriebssystem steht auf dunkel, `system` ist dort also mit `dunkel` deckungsgleich — belegt in §16.3 |
| **Sprachen** | beide. „Erscheinungsbild" / „Appearance", Light / Dark / System setting |
| **Umfang** | Verlaufsdiagramm samt Legende und Tooltip · Prozessbaum · Formulare und Eingabefelder · Dialoge und Schubladen |
| **Nicht angesehen** | **Rohdatenansicht** und **Benutzerverwaltung** — ausdrücklich außerhalb dieser Runde (Punkt **125**) |

> ### ⚠️ Die Regel für Funde, und sie ist eingehalten
>
> **Was die Sichtprobe gefunden hat, ist Befund und offener Punkt — nicht Korrektur.** Geändert
> worden ist ausschließlich, was die **Mechanik** zerbräche; ein solcher Fall ist nicht aufgetreten.
> **Kein Farbwert ist angefasst worden** (§13.1).

### 19.1 Was zu sehen war — die Befunde

**1. ⚠️ Die Überlagerung des Dialogs verliert im Dunkeln ihre dunkelnde Hälfte.**
`components/ui/dialog.tsx` legt `bg-black/10` **plus** `backdrop-blur(4px)` über die Seite. Im
hellen Zustand dunkelt das schwarze Zehntel sichtbar ab. Im dunklen liegt es über `--background`
`lab(3.6999 0 0)` — rund **eine 8‑Bit-Stufe** Unterschied, also praktisch nichts. Was den Dialog vom
Rest trennt, ist dort allein der Weichzeichner und der Haarlinienring `ring-foreground/10`
(`lab(100 0 0 / 0.12)`). **Die Trennung trägt — sie wird nur von der anderen Hälfte des Rezepts
getragen.** Generatorbereich, deshalb Befund und keine Korrektur. Offener Punkt **126**.

**2. Der Tooltip des Diagramms hat dieselbe Fläche wie die Karte, über der er schwebt.** Beide
`bg-card` = `lab(8.36 0 0)`. Getrennt wird er von `ring-foreground/10` und `shadow-md`. Im Bild ist
er als eigene Fläche zu erkennen; die Aussage ist, dass er es **nicht über die Farbe** ist. Im hellen
Zustand ist die Lage dieselbe (`--card` weiß über `--background` `#fafafa`), dort trägt der Schatten
mehr. **Kein neuer Punkt** — es ist dieselbe Gestalt in beiden Zuständen und keine Eigenheit des
Dunkelzustands.

**3. Offener Punkt 122 ist am laufenden System bestätigt, und zwar in beiden Hälften.** Auf der
Übersicht nebeneinander: Im **hellen** Zustand ist die Fehlerkachel die lautere. Im **dunklen** zieht
die Gold-Kachel mindestens gleich, und im Verlaufsdiagramm ist der mintgrüne Balken die lauteste
Fläche der ganzen Seite — auf einer Übersicht, die geöffnet wird, *weil* etwas nicht stimmt.
**Bekannt, dokumentiert, bewusst stehen gelassen** (Abgrenzung dieser Runde).

**4. Offener Punkt 92 ist im Dunkeln messbar besser und bleibt.** Die gedrückte bzw. überfahrene
Zeile trägt `--muted` `lab(15.32 0 0)` auf `--background` `lab(3.6999 0 0)`. §3.7 rechnet dafür
1,29 : 1 gegen 1,08 : 1 im Hellen; die Zahlen sind hier am laufenden System wiedergefunden worden.
Sichtbar ist der Zustand im Dunkeln, **gut** sichtbar ist er nicht.

**5. Die gefüllte Schaltfläche ist im Dunkeln erheblich präsenter** — §6.2, Befund 5, am echten
Bestand bestätigt: „Vorschläge und Bestand erheben" auf `/administration/katalog` ist die lauteste
Fläche der Seite. Berichtet, nicht geändert.

**6. Die gesperrte Hauptschaltfläche.** `opacity: 0.5` über `--akzent` mit `--akzent-vordergrund`
darauf. **Der zusammengesetzte Kontrast ist nicht gerechnet worden**, und ein gesperrtes
Bedienelement ist von WCAG 1.4.3 ausgenommen — es steht hier, weil es beim Ansehen auffällt und
nicht, weil es ein Fehler wäre.

**7. Kein Befund:** Prozessbaum (1.158 Zeilen, Fehlerzahlen, Plaketten „seit über 3 Monaten
nichts"), Übertragungsliste, Formularfelder samt Fokusring, Auswahlfeld, `ToggleGroup`, Schublade
(`bg-popover` `lab(8.36 0 0)`), Navigationszeile, Kopfzeile, Nutzermenü in beiden Sprachen. Alles
lesbar, nichts unmarkiert, keine unlesbare Stelle.

### 19.2 Zwei Beinahe-Befunde, die die Gegenprobe kassiert hat

**Sie stehen hier, weil beide ohne Nachmessen als Befund in dieser Datei gelandet wären.**

| Erster Eindruck | Was die Nachmessung ergab |
|---|---|
| *„Die gewählte Baumzeile trägt im Dunkeln `--muted`, im Hellen `--accent` — die Auswahl sieht in den beiden Zuständen verschieden aus."* | **Falsch.** Gemessen war der **Überfahr**zustand: Der Zeiger stand auf der Zeile. Ohne Zeiger trägt sie in **beiden** Zuständen `--accent` (`lab(16.8911 -5.54553 23.2512)` dunkel, `lab(96.2605 -4.39042 12.4753)` hell) |
| *„Der Platzhaltertext löst auf `--foreground` statt auf `--muted-foreground` auf."* | **Falsch.** Gemessen war ein Feld **ohne** Platzhalter, das den ererbten Wert meldet. An einem Feld mit Platzhalter (`Belegnummer suchen`) ist es `lab(65.2 0 0)` = `--muted-foreground`, wie die Klasse sagt |

### 19.3 Zwei Werkzeugbefunde, keine Anwendungsbefunde

Hier vermerkt, damit der nächste Lauf nicht darauf hereinfällt:

1. **Ein Radix-Menü öffnet sich nicht auf ein synthetisches `element.click()`** — dieselbe Klasse wie
   der CDP-Befund in [`dichte-umschalter.md`](dichte-umschalter.md) §5.7. Geöffnet wird mit einem
   echten Zeigerklick; das **Untermenü** öffnet danach zuverlässig über `ArrowRight`/`Enter` am
   Auslöser.
2. **Ein echter Mausklick, der neben dem Untermenü endet, schließt es** — Radix schließt ein
   Untermenü, wenn der Zeiger es verlässt. Das sah zweimal wie ein Befund *„das Menü bleibt beim
   Umschalten nicht offen"* aus und war keiner: Mit einem sauber im Eintrag platzierten Klick
   stehen bei **0, 150, 300, 600 und 1000 ms** nach dem Umschalten weiterhin alle **drei** Einträge
   da, und `data-thema` ist gewechselt. Dasselbe gilt für die programmatische Auslösung.

### 19.4 Der Belegvermerk *(Regel L10)*

> **Gemessen ist:** die Auflösung der Tokens am laufenden Chrome (`getComputedStyle`) für Diagramm,
> Baum, Formular, Dialog und Schublade; die Mutationen und die Knotenidentität im Diagrammbaum; die
> Byteposition im ausgelieferten Dokument; die Helligkeit jedes Einzelbilds während des Ladens; die
> Zahl der `dark:`-Regeln je Zweig im gebauten CSS.
>
> **Behauptet wird** darüber hinaus, dass die Anwendung in den vier Bereichen „lesbar" ist. Das ist
> eine **Aussage über einen Seheindruck** an Bildschirmfotos einer einzigen Fenstergröße
> (1568 × 726), eines Mandanten und einer Dichtestufe — kein Wahrnehmungsversuch mit mehreren
> Betrachtern, keine gerechnete Kontrastprüfung über die ganze Oberfläche. Die gerechneten Kontraste
> stehen in §3 und gelten für die fünf Rollen und die vier Akzentstufen, nicht für jede Paarung, die
> in einer Ansicht vorkommt.
>
> **Nicht angesehen ist:** die **Rohdatenansicht** und die **Benutzerverwaltung** (Punkt 125), das
> Berührungsgerät, ein Vorleseprogramm, jede andere Fenstergröße und jeder andere Mandant.

---

## 20. Die Entscheidungen dieser Runde

**E‑59 und E‑60 sind hier _umgesetzt_ und nicht getroffen** — sie sind am 03.09.2026 gefallen und
stehen im Plan-Eintrag ([`IMPLEMENTIERUNGSPLAN_MVP.md`](IMPLEMENTIERUNGSPLAN_MVP.md), Schritt 11).
Neu ist ab **E‑66**:

| Nr. | Entscheidung | Warum nicht anders |
|---|---|---|
| **E‑66** | **Die Dunkelwerte stehen als Zwischenschicht `--dunkel-<token>` im unbedingten `:root`; beide Zweige zeigen nur um.** E‑60 verlangt „die Werte stehen einmal" und lässt die Form offen — das ist die Form | Zwei Wertesätze wären eine zweite Pflegestelle, und eine Änderung an nur einem wäre im anderen Zweig unsichtbar. **Verworfen:** `light-dark()` — es setzt beide Werte in **eine** Deklaration und wäre kürzer, verlangt aber, dass jeder Wert über `color-scheme` schaltet, verträgt sich nicht mit den beiden Alphawerten in `--border`/`--input` und hätte `tests/farbkontrast.test.ts` umgebaut statt umbenannt. **Verworfen:** der „space toggle" über leere Custom Properties — er käme mit einer einzigen Wertliste aus, ändert aber den aufgelösten Wert um ein führendes Leerzeichen und ist für den nächsten Leser dieser Datei nicht mehr lesbar |
| **E‑67** | **`@custom-variant dark` steht in der Blockform mit zwei `@slot` und `:is(…, … *)`** | Die Kurzform trifft genau einen Zweig; eine zweite `@custom-variant`-Zeile daneben gewänne je nach Reihenfolge. `:is()` statt `:where()`, weil die alte Fassung `:is(.dark *)` war und die Spezifität damit bleibt, wo sie war (0,1,0). `…, … *` statt nur `… *`, weil `data-thema` am Wurzelelement selbst steht |
| **E‑68** | **`html[data-thema="hell"]` bekommt eine eigene Regel — nur für `color-scheme: light`** | Ohne sie bekäme ein Nutzer, der *hell* ausdrücklich gewählt hat, auf einem dunkel gestellten Betriebssystem `color-scheme: normal`. Und die Regel macht die drei Werte im CSS zu drei Regeln, was der Test in beide Richtungen prüfen kann |
| **E‑69** | **`tests/farbkontrast.test.ts` liest beide Wertesätze aus `:root` über ein Präfix**, nicht über zwei Selektoren | Die Dunkelwerte stehen in keinem der beiden Zweige. Ein Test, der einen Zweig läse, läse eine Umzeigung und keine Farbe. **Die 69 Fälle bleiben** — mehr als der Leseweg hat sich nicht geändert |
| **E‑70** | **Kein zweites Zeichen je Menüeintrag.** Sonne, Mond und Bildschirm bleiben weg; der Haken ist das Zeichen | Der Haken sagt in diesem Menü *„das gilt"*. Ein zweites Zeichen daneben machte aus einer Auskunft zwei — und der Dichteumschalter drei Zeilen höher hat auch keins |

---

## 21. Regelbezug

| Regel | Umsetzung |
|---|---|
| **Q4** Nicht zugeordnet heißt nicht zugeordnet | `themaAus` fällt auf `system` und ordnet **nicht** sinngemäß zu. Am ausgelieferten Dokument nachgewiesen (§14) |
| **§2** Alle Farben ausschließlich über CSS-Variablen, Zuordnung an genau einer Stelle | **Kein Farbwert ist angefasst worden** (§13.1). `lib/status-farbe.ts` ist unberührt, `tests/farbwerte.test.ts` hat keine neue Ausnahme. Die 19 Verdrahtungszusicherungen in `@theme inline` halten unverändert |
| **§3** Status wird nie allein über Farbe ausgedrückt | Trägt Punkt 122 weiterhin: Jede Kachel und jede Zeile führt Wort und Zeichen, und die beiden Zeichen unterscheiden sich in der **Form** |
| **E‑x** Die Wahl liegt im Cookie, das Wurzel-Layout liest sie serverseitig vor dem ersten Paint | `data-thema` an **Byte 47**, vor jedem Stylesheet und jedem Skript (§14) |
| **L10** Belegvermerk *„gemessen war X / behauptet wird Y"* | §15.2, §19.4 |
| Kein `getComputedStyle` zur Laufzeit für Gestaltung | Der aktive Wert kommt aus dem Kontext. `useThema` sagt ausdrücklich **nicht**, ob es gerade dunkel *ist* — bei `system` entscheidet das die Medienabfrage, und wer hier `matchMedia` befragte, baute genau den zweiten Weg, den E‑60 vermeidet |
| Generatorbereich wird nicht von Hand umgebaut | `components/ui` ist unberührt, kein `shadcn add` war nötig. Die beiden Befunde, die dort liegen (Punkt 92 und der neue Punkt 126), sind **gemessen und nicht behoben** |
| Keine Zeichenkette in einer Komponente | Alle Beschriftungen in `i18n/de.ts` und `en.ts`; `thema-umschaltung.tsx` enthält keine |

---

## 22. Offene Punkte

### Die Nummernvergabe, belegt

Gelaufen am 03.09.2026 gegen den Stand **vor** dieser Runde:

| Kreis | Vergeben | Gegenprobe | Ergebnis |
|---|---|---|---|
| **Messungen** | M136–M141 | `M1[3-9][0-9]` über `docs/*.md` und die Wurzeldateien | höchste vorher **M135** (§7.4); kein Treffer darüber |
| **Entscheidungen** | E‑66–E‑70 | ein Python-Durchlauf, der jede Schreibweise `E<Strich><Zahl>` einsammelt | höchste vorher **E‑65** (§8); kein Treffer darüber |
| **Offene Punkte** | 126–129 | über alle Dateien in `docs/` und die Markdown-Dateien im Wurzelverzeichnis | höchster vorher **125** (§10) |

> ⚠️ **Dieselbe Falle wie in §10, und sie hat diesmal dreimal zugeschlagen.** `**126**`, `**127**`
> und `**128**` kommen im Bestand bereits vor — als *„126 Pläne erhoben"*
> ([`nachrichtenliste.md`](nachrichtenliste.md)), als Tabellenwert in
> [`messungen-schritt8.md`](messungen-schritt8.md) und als Faktor 128 in
> [`messungen-schritt10.md`](messungen-schritt10.md). **Es sind Messwerte und keine Punkte.**
> Nachgesehen, Zeile für Zeile.

| Nr. | Punkt |
|---|---|
| **126** | **Die Überlagerung des Dialogs dunkelt im Dunkelzustand praktisch nicht ab** (§19.1). `components/ui/dialog.tsx` legt `bg-black/10` über die Seite; über `--background` `lab(3.6999 0 0)` sind das rund **eine 8‑Bit-Stufe**. Getragen wird die Trennung dort allein vom `backdrop-blur(4px)` und vom Haarlinienring — **sie trägt**, aber von der anderen Hälfte des Rezepts. Generatorbereich (`visuelles-konzept.md` §2); wer es auflöst, tut es an einer umschließenden Komponente und nicht im Generator |
| **127** | **Der Umschalter nimmt die Zusage aus §8a nicht in Anspruch, obwohl sie hält** (§15.3). `themaSetzen` ruft `revalidatePath("/", "layout")`; gemessen sind danach **6 `childList`-Mutationen** im Diagrammbaum und **0 von 49** erhaltenen Balkenknoten — der Baum wird neu erzeugt. Über das Attribut allein sind es **0 Mutationen und 49 von 49**. **Die Farbe bräuchte das Neurendern nicht.** Derselbe Zusammenhang wie die offenen Punkte **100** und **101** aus [`dichte-umschalter.md`](dichte-umschalter.md), an einer neuen Stelle sichtbar; eine Auflösung wäre eine Entscheidung über die Server-Aktion und nicht über den Dunkelmodus |
| **128** | **Kein Test rendert den Themaumschalter.** `tests/thema.test.ts` prüft `globals.css` und die reine Funktion `themaAus`; `components/thema-umschaltung.tsx`, `thema/provider.tsx`, `thema/aktion.ts` und `thema/server.ts` haben **keine** Abdeckung. Ungeprüft bleibt damit alles, was §12.6 zusagt: die drei Einträge, `role="menuitemradio"`, `aria-checked`, das abgefangene `onSelect`, das Häkchen im Fluss. **Gemessen ist es** (§12.6, §19.3), zugesichert nicht. Wortgleich zu Punkt **102** für den Dichteumschalter — das Projekt zählt gerenderte Testbäume bewusst ab (`vitest.config.mts`) |
| **129** | **Die Cookie-Eigenschaften des Themas sind weder gemessen noch zugesichert.** Gemessen ist genau eine der vier: Das Cookie ist über `document.cookie` lesbar, trägt also **kein `HttpOnly`**. Ein Jahr, `SameSite=Lax` und `path="/"` stehen im Code und sind **nicht** am gesetzten Cookie abgelesen worden. `thema/aktion.ts` ist Zeile für Zeile `dichte/aktion.ts`, deren vier Eigenschaften in [`dichte-umschalter.md`](dichte-umschalter.md) §5.5 gemessen sind — **das ist ein Argument und keine Messung.** Wortgleich zu Punkt **103** |

### Punkt 125 — **fortgeschrieben, nicht geschlossen**

> **125** *(Stand 03.09.2026 nach 11b)*: **Die Sichtprobe deckt die Anwendung weiterhin nicht
> vollständig ab.** Abgetragen sind mit dieser Runde: das **Verlaufsdiagramm** samt Legende und
> Tooltip, der **Prozessbaum**, **Formulare und Eingabefelder**, **Dialoge und Schubladen** —
> alle vier am laufenden, angemeldeten System und in allen drei Zuständen (§19).
>
> **Ungesehen im Dunkelzustand bleiben:**
>
> | | |
> |---|---|
> | **Rohdatenansicht** | ausdrücklich außerhalb dieser Runde |
> | **Benutzerverwaltung** | ausdrücklich außerhalb dieser Runde |
> | *dazu, und das ist neu benannt:* | jede andere **Fenstergröße** (gemessen ist 1568 × 726), jede andere **Dichtestufe** (gemessen ist `s` und `m`), jeder andere **Mandant** (gemessen ist NEXANS), das **Berührungsgerät** und ein **Vorleseprogramm** |
>
> Die beiden erstgenannten sind die Restliste des Auftrags; die dritte Zeile ist der Zuschnitt jeder
> Sichtprobe dieses Projekts und steht hier, damit sie nicht als abgedeckt gilt.

---

## 23. Was ausdrücklich **nicht** getan worden ist

- **Kein Farbwert geändert**, in keinem Wertesatz — über den Wertevergleich nachgewiesen (§13.1)
- **Punkt 122 ist nicht behoben.** Der Auftraggeber hat am 03.09.2026 entschieden: *11b baut alles,
  122 bleibt offen.* Die Rangfolge kippt im Dunkeln und Grün ist die lauteste Kachel — **bekannt,
  dokumentiert, bewusst stehen gelassen.** Bestätigt am laufenden System (§19.1)
- **Punkt 123 und 124 unberührt**
- **Punkt 88 und 92 unberührt.** 92 ist im Dunkeln nachgemessen und nicht behoben (§19.1)
- **`components/ui` unberührt.** Generatorbereich; der neue Punkt 126 liegt dort und ist deshalb
  Befund
- **Keine Spalte an `app_user`.** Der Nachrüstweg bleibt offen, genau wie bei Sprache und Dichte
- **Kein `next-themes`** und keine andere Bibliothek. Der Abhängigkeitsbaum hat keinen Eintrag
  bekommen
- **Keine Datenbank.** Diese Runde hat keine Zeile geschrieben und keine gelesen, die nicht die
  Anwendung selbst gelesen hätte

---

## 24. Abnahme — Punkt für Punkt

| | Abnahmepunkt | Ergebnis |
|---|---|---|
| **1** | `pnpm check` und `pnpm test` grün | ✅ **33 Dateien, 863 Fälle grün.** `pnpm check` läuft Lint, Typprüfung, Formatprüfung und Tests, Rückgabewert 0 |
| **2** | `farbkontrast.test.ts`: weiterhin **69 Fälle**, vierzehn Gegenproben durch, neunzehn Verdrahtungszusicherungen halten | ✅ **69 Fälle**, mit `--reporter=verbose` nachgezählt. Die vierzehn Gegenproben und die neunzehn Verdrahtungszusicherungen laufen unverändert (§18.1) |
| **3** | `thema.test.ts` läuft; die sieben Mutanten fallen wie erwartet, Mutant 0 überlebt; `globals.css` danach **byte-gleich**, mit `sha256` vor und nach | ✅ **23 Fälle.** Sieben Mutanten, alle wie erwartet, Mutant 0 grün. `globals.css` und `thema/index.ts` byte-gleich, `sha256` in §18.3 |
| **4** | **M136** — Byteposition von `data-thema` genannt | ✅ **Byte 47**, in allen sechs Cookie-Fällen. Vor dem ersten `<link>` (266), dem ersten `stylesheet` (277) und dem ersten `<script>` (578) — §14 |
| **5** | **M137** — Recharts wechselt ohne Neumontage, oder der Gegenbefund steht ausgeschrieben | ✅ **Beides.** Über das Attribut: **0 Mutationen, 49 von 49 Knoten identisch**, Farben gewechselt — die Zusage aus §8a hält. Über den Menüeintrag: **6 Mutationen, 0 von 49** — der Gegenbefund steht ausgeschrieben und als Punkt **127** (§15) |
| **6** | **M138** — System-Zweig greift beim ersten Paint | ✅ Ein einziger `--background`-Wert über alle Proben, dunkel ab dem ersten `<html>` (137,8 ms), first-paint bei 266 ms. Vier Gegenproben. Am Pixel: **0 von 5 Einzelbildern hell** im dunklen Lauf, 5 von 5 in der Eichung (§16) |
| **7** | **Teil 3 belegt:** kein aufgelöster Wert hat sich durch den Umbau geändert | ✅ Je Zustand **69 Namen abgefragt, 51 mit Wert, 0 Abweichungen** — dunkel wie hell, am laufenden Chrome vor und nach dem Umbau. Die 18 leeren sind `--color-*`-Namen, die `@theme inline` beim Bauen einsetzt, und sie sind vorher wie nachher dieselben. Vergleich in beide Richtungen geeicht (§13) |
| **8** | Umschalter im Nutzermenü, drei Werte, in beiden Sprachen beschriftet | ✅ Untermenü *Erscheinungsbild* / *Appearance* unter *Anzeigegröße*. Drei Einträge, genau einer mit `aria-checked="true"`. Deutsch: Hell · Dunkel · Systemeinstellung. Englisch: Light · Dark · System setting (§12.6, §19) |
| **9** | Sichtprobe über die vier Bereiche gefahren, Befunde ausgeschrieben, **keiner davon korrigiert** | ✅ Alle vier, in allen drei Zuständen, am angemeldeten System. Sechs Befunde, zwei Beinahe-Befunde von der Gegenprobe kassiert, zwei Werkzeugbefunde. **Nichts korrigiert** (§19) |
| **10** | Punkt 125 fortgeschrieben mit der Restliste | ✅ Vier Bereiche abgetragen; Rohdatenansicht und Benutzerverwaltung bleiben, dazu die neu benannten Grenzen der Sichtprobe (§22) |
| **11** | **Kein Farbwert geändert** — über den Diff nachgewiesen | ✅ Dunkler Wertesatz: **50 Tokens, 0 Abweichungen** gegen `git HEAD`. Heller Wertesatz: 70 Tokens, **1** Abweichung — und die ist die von **11a** (E‑62); gegen den Stand nach 11a sind es **0**. Beide Vergleiche gegengeprobt (§13.1) |
