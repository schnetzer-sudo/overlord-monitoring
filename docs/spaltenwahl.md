# Spaltenwahl nach dem Platz

*Teil 2 der Sichtprobe am schmalen Fenster (Bau), 15.09.2026.* Entscheidung **E‑147**, Messung
**M177**, neuer offener Punkt **180** ([`benutzerverwaltung-frontend.md`](benutzerverwaltung-frontend.md)
§18). Vorlauf und Eichung: [`messungen-sichtprobe-schmal.md`](messungen-sichtprobe-schmal.md) (M176).

**Die Frage dieser Datei:** Welche Spalten einer Tabelle stehen bei welcher Breite da — und woher kommt
die Zahl? Der Nutzer, für den das Werkzeug gebaut ist, sucht einen Beleg und will wissen, wo er steht.
Eine Spalte, die ihre Beschriftung ohne Platz über die Nachbarin druckt, beantwortet das nicht; eine,
die gar nicht dasteht, lässt die Zeile wenigstens lesbar.

> ### Der Ertrag in einem Absatz
>
> **Drei der vier Tabellen wählen ihre Spalten jetzt nach dem Platz ihrer eigenen Hülle** —
> Trefferliste, Benutzertabelle, Prozess-Katalog. Jede Schwelle ist die **Summe gemessener
> Mindestbreiten**, keine Zahl, die gut aussieht; jede feste Spalte trägt genau ihre Mindestbreite;
> eine Spalte ist da oder nicht da. **Die Nachrichtenliste ist nicht umgebaut**, und das ist der
> Befund, den der Auftrag in 3.4 vorsieht: Zeitpunkt (187 px) und Status (155 px) lassen dem Ablauf im
> schmalsten Kasten (334 px) **−8 px**. Punkt 114 bleibt deshalb offen, jetzt mit der Zahl.
>
> **Die Gegenprobe im Browser hält** (§7, 90 Messungen, beide Mandanten): **null** gebrochene Zusagen des
> Rahmens. Bei 768 px hat keine der drei Tabellen mehr eine 0-px-Spalte (vorher je eine), die
> Benutzertabelle ist 520 statt 792 px breit, und die Katalogseite bei `NEXANS` ist 68.610 statt
> 185.708 px hoch. Dazu die zwei kleinen Befunde — Projekt und Prozess im Detail und der Produktname der
> Kopfzeile tragen ihren `title` — und die Rollenauswahl als Liste der Anwendung (Punkt 180).

> ### Nachtrag vom 16.09.2026, zweite Meldung desselben Tages — E‑149
>
> **Am breiten Fenster klebten in der Benutzertabelle die drei linken Spalten aneinander, und rechts
> daneben stand eine Lücke.** Ursache war die **freie Spalte**: Die Mandanten trugen keine Breite und
> bekamen deshalb bei jeder Breite den ganzen Rest — bei 1.408 px Hülle **673 px** für eine Reihe
> kurzer Marken, während „lschnetzer" auf 81 px und „EDI-Betreuung" auf 89 px zweizeilig umbrachen.
> Seither hat diese Tabelle **keine freie Spalte**: Jede trägt ihre Mindestbreite, und
> `table-layout: fixed` teilt den Überschuss **anteilig** auf (§5.5). Die Schwellen sind unberührt.

---

## 1. ⚠️ Abweichungen vom Auftrag — gemeldet, keine still aufgelöst

| # | Der Auftrag | Was geschehen ist | Warum |
|---|---|---|---|
| 1 | vier Tabellen bauen, Punkt **114** schließen (§7 des Auftrags) | **Die Nachrichtenliste ist nicht gebaut, 114 bleibt offen** | Der Fall aus 3.4: Beim schmalsten Container bleibt für den Ablauf keine brauchbare Breite (§6). Der Auftrag schreibt für diesen Fall vor, die übrigen drei zu bauen und die Zahl vorzulegen. **Am 16.09.2026 nachgeholt** (§5.4, §6 Nachtrag): Der Auftraggeber hat den Klumpen erneut gemeldet, die Entscheidung ist als E‑148 gefallen, Punkt 114 ist geschlossen |
| 2 | *(nicht geregelt)* wie breit eine feste Spalte oberhalb ihrer Schwelle ist, und was unterhalb der Grundmenge gilt | **zwei Fragen an den Auftraggeber, beantwortet am 15.09.2026** (§2) | Beides legt der Auftrag nicht fest; bei allen drei Tabellen passt die Grundmenge nicht in den schmalsten Kasten. **Die Frage nannte für die Benutzertabelle vorläufige 371 px Grundmenge und 35 px Überlauf** — nach der vollständigen Messung (Wortrechnung, §3) sind es **406 px und 70 px** bei 336 px Kasten |
| 3 | das Skript aus Teil 1 misst die Mindestbreite | **ein zweiter Messkern daneben**, `scripts/sichtprobe-schmal/spaltenbedarf.js`; `messung.js` ist unverändert und trägt die Gegenprobe (§7) | `messung.js` misst Kästen, Schnitte und 0-px-Spalten, aber keine Inhaltsbreite. **Zwei Fehler des neuen Kerns sind in dieser Runde gefunden und behoben** (§4), beide Läufe wiederholt |
| 4 | Produktionsbau | `next build` + `next start` **auf `:3001`**, neben `next dev` auf `:3000` | Der Entwicklungsserver auf `:3000` lief; `next build` räumt `.next/dev` nicht (`recursiveDeleteSyncWithAsyncRetries(distDir, /^(cache\|dev\|lock)/)`), beide stehen nebeneinander. Das Sitzungscookie gilt für den Rechner und nicht für den Port |
| 5 | Messrahmen | **der Rahmen im Chrome der Erweiterung** (Bauform B aus Teil 1), **der Tab war verdeckt** | Angemeldet wird nur durch den Auftraggeber. Im verdeckten Tab laufen Übergänge und Animationen nicht — das hat einen Beinahe-Befund erzeugt und ist dort beschrieben ([`benutzerverwaltung-frontend.md`](benutzerverwaltung-frontend.md) §18) |
| 6 | „umgebaut wird nur die Rollenauswahl" | **beide Rollenfelder** — Zeilenformular und Maske „Konto anlegen" | Es ist dasselbe Feld an zwei Stellen; zwei Bauformen dafür wären die Drift, gegen die der Umbau gerichtet ist. Mandant und Baumgliederung bleiben nativ |
| 7 | Tastaturbedienung der Rollenauswahl unverändert | **`↓` am geschlossenen Feld öffnet die Liste**, statt den Wert zu ändern | Im Zeilenformular hieße eine Wertänderung ein `PUT`, und jedes verwirft alle Sitzungen des Kontos (`benutzerverwaltung.md` E5) |
| 8 | die aufgeklappte Liste in Chromium **und** Firefox ansehen | **nur Chromium, gemessen im DOM** | Firefox 155 ist installiert, hat aber keine Sitzung |
| 9 | Kontrasttest „bleibt bei seinen 69 Fällen" | **83 Fälle, vor und nach dieser Runde** | Die 69 sind der Stand von Schritt 11b ([`dunkelmodus.md`](dunkelmodus.md) §18.1). Und der Test prüft keine Paarung, die die neue Liste benutzt ([`benutzerverwaltung-frontend.md`](benutzerverwaltung-frontend.md) §18) |
| 10 | — | **Die Statusspalte der Trefferliste ist ab `lg` nicht mehr breiter**; der Zusatz „Schritt: …" kürzt bei jeder Breite | Folge der Antwort auf Frage 1: Eine feste Spalte trägt ihre Mindestbreite, und die der Statusspalte ist die Plakette ([`nachrichtenliste.md`](nachrichtenliste.md) §8.1, „der Zusatz kürzt dann") |
| 11 | beide Rollen fahren | **nur `ADMIN`** | Wie in Teil 1: Ein Konto der Rolle `MANDANT` stand nicht zur Verfügung. Die Tabellen dieser Runde sind Verwaltungstabellen und die Trefferliste; Letztere ist rollenfrei |

---

## 2. Die Entscheidung — E‑147

> **Die Spaltenmenge einer Tabelle folgt der Breite ihres Containers, nicht der des Fensters.**

Getroffen vom Auftraggeber, hier eingetragen. **Der Grund steht in M176:** Bei 768 px erscheint die
Navigationsspalte, `main` hat 560 px, und im selben Moment kam an der Fensterschwelle `md` eine Spalte
dazu, für die kein Platz war — 0 px breit, die Beschriftung trotzdem gezeichnet. Eine verschobene
Fensterschwelle müsste die Breite der Navigationsspalte und die des Panels festschreiben und bräche
beim nächsten Umbau erneut. **Der Container weiß beides von selbst.**

| | |
|---|---|
| **Mechanik** | CSS-Container-Queries, in Tailwind 4 eingebaut: `@container/<name>` an der Hülle, `hidden @min-[…rem]/<name>:table-cell` an der Zelle. Keine neue Abhängigkeit. Nachgesehen am Compiler (Tailwind 4.3.3): Die Klasse erzeugt `@container <name> (width >= …)` |
| **Wo der Container sitzt** | an einer **eigenen Hülle** um die Tabelle, **benannt** (`trefferliste`, `benutzertabelle`, `katalog`) — **nicht** an `main`, das selbst ein Größencontainer ist (`lib/klebende-spalte.ts`), und **nicht** in `components/ui/table.tsx` (Generatorbereich, nicht angefasst) |
| **Eine Spalte verschwindet** | über `display: none` am `th` **und** an jedem `td` — nie über eine Breite von 0 px |
| **Wörtliche Klassen** | Tailwind findet eine Klasse nur, wenn sie im Quelltext steht. Die Sichtbarkeit steht deshalb als Zeichenkette in `features/<feature>/…spalten.ts`, die Breite als `w-[…rem]` an der Kopfzelle; `tests/spaltenwahl.test.tsx` hält beide gegen die Rechnung (§9) |
| **Kein neuer Umbruchpunkt** | 768 px bleiben die Grenze des Rahmens ([`visuelles-konzept.md`](visuelles-konzept.md) §6, dort eingetragen) |

### Die zwei Antworten des Auftraggebers vom 15.09.2026

| Frage | Antwort | Was daraus folgt |
|---|---|---|
| **Wie breit ist eine feste Spalte, sobald sie dasteht?** | **Ihre gemessene Mindestbreite**, bei jeder Containerbreite | Die freie Spalte bekommt den Rest und an ihrer Schwelle genau ihre Mindestbreite. Die Breiten an `sm`, `md` und `lg` sind entfallen; bei breitem Fenster sind mehrere Spalten schmaler als vorher (§5) |

> ⚠️ **Die erste Antwort hatte eine Lücke, und sie ist am 16.09.2026 aufgefallen** (E‑149, §5.5): Sie
> sagt, wie breit eine **feste** Spalte ist — nicht, wohin der Rest geht, wenn die **freie** ihn nicht
> braucht. Bei der Benutzertabelle ging er vollständig an eine Reihe kurzer Marken. **Eine freie Spalte
> hat eine Tabelle seither nur dann, wenn ihr Inhalt den Platz auch nutzt.**
| **Was gilt unterhalb der Grundmenge?** | **Die heutige Bauform** | Trefferliste und Benutzertabelle laufen über — die Trefferliste in ihrer Hülle, wie seit Schritt 7 ([`property-suche.md`](property-suche.md) §14, Punkt 11) —; der Katalog bleibt dreispaltig mit Prozess 9 rem und Pflege 6 rem und bricht um, die Bauform, die der Auftrag bei 360 px ausdrücklich nicht als Fehler führt |

---

## 3. Die Mindestbreite — was gemessen wird

**Eine Schwelle ist die Summe der Mindestbreiten der Spalten, die ab dort sichtbar sind, plus Rinne.**
Die Mindestbreite einer Spalte ist gemessen, nicht geschätzt (Regel Q4), und zwar als das Größte aus:

| Größe | Wie |
|---|---|
| **Kopfbeschriftung** | in **beiden** Sprachen, **einzeilig**, samt Innenabstand und dem, was um den Text steht (Sortierpfeil) |
| **längster Zellinhalt ohne Notumbruch** | jede Zelle in einer Sonde außerhalb der Tabelle, mit den geerbten Schrifteigenschaften der Zelle, `width: min-content` und `overflow-wrap: normal`. **`word-break` bleibt, wie die Zelle es setzt:** Eine Kennung mit `break-all` darf überall brechen, das ist ihre Bauform. `break-words` ist der Notumbruch, der am 15.09.2026 buchstabenweise umgebrochen hat, und genau den schließt die Messung aus. Bei gekürzten Zellen (`nowrap`) ist das der ganze Inhalt in einer Zeile |
| **endliche Beschriftungsmengen** | was in einer Zelle stehen **kann**, auch wenn die Testkopie es gerade nicht zeigt: die acht Einordnungen als Plakette, die Kettenrollen, jede Ziffer an jeder Stelle des Zeitpunkts, die festen Texte aus den Sprachdateien („deaktiviert", „Wechsel erforderlich", ein Zeitpunkt hinter „bis") |

Aufgerundet auf ganze Pixel, geschrieben in `rem` (16 px je `rem`).

**Vier Regeln, und sie stehen im Auswerteskript, nicht nur hier** (`auswertung-bedarf.mjs`):

1. **Status:** die Plakette **ohne** den Zusatz „Schritt: …" — der kürzt bewusst
   ([`nachrichtenliste.md`](nachrichtenliste.md) §8.1).
2. **Kette:** die Rollen **einzeln**. Die Verkettung aller vier (221 px) steht in keiner gemessenen Zeile
   und zählt nicht.
3. **Treffer:** die längste Belegart-Bezeichnung **allein**. Mit „ +1" dahinter wären es 299 px; die
   Zelle kürzt mit vollem `title`, wie sie es seit Schritt 7 tut ([`bam-suche.md`](bam-suche.md) §11.5).
4. **Dieselbe Zelle, dieselbe Zahl:** Zeitpunkt, Status und Ablauf sind in Nachrichten- und Trefferliste
   dieselben Komponenten und tragen das Größte aus beiden Tabellen. Der Ablauf der Trefferliste trägt
   deshalb die 304 px aus der Nachrichtenliste (300 Zeilen) und nicht die 191 px aus seinen eigenen 51.

**Rinne = 0 px.** Die Abfrage misst die Inhaltsbox der eigenen Hülle; die Tabelle ist `w-full`, die
Verwaltungstabellen tragen `border-spacing: 0`, der Rahmen der Karte liegt außerhalb. Eine
Bildlaufleiste von `main` liegt ebenfalls außerhalb — der Container **misst** sie mit, und eine
Schwelle greift dann bei einem entsprechend breiteren Fenster. Das ist der Vorzug der Container-Abfrage
und kein Fehler der Zahl.

**Dichte:** gemessen in Stufe `m` (16 px Wurzelschrift). Schrift und Spaltenbreite liegen beide in `rem`
und skalieren miteinander ([`visuelles-konzept.md`](visuelles-konzept.md) §5); **gemessen** ist nur `m`.

---

## 4. Die Messung M177 — der Spaltenbedarf

| | |
|---|---|
| **Ziel** | `http://localhost:3001` — Produktionsbau (`next build` + `next start`, Next.js 16.2.11), Backend `spring-boot:run` im Profil `dev`, die Testkopie. Nichts gegen die Produktion |
| **Browser** | Chrome 152 der Erweiterung, angemeldet durch den Auftraggeber (`ADMIN`), Dichte `m`; Rahmen `rahmen.js` in der Oberseite, gemessen an einem 1.600 × 1.000 px großen Rahmen — die Sonde misst ausgeblendete Spalten mit |
| **Messkern** | `scripts/sichtprobe-schmal/spaltenbedarf.js`, Beschriftungen aus `etiketten.mjs` (aus den Sprachdateien gelesen), Auswertung `auswertung-bedarf.mjs` |
| **Mandanten** | `NEXANS` und `VOTG` (Regel L7), gewechselt über `/mandantenauswahl` im Rahmen, danach zurück auf `NEXANS` |
| **Sprachen** | Deutsch und Englisch über das Cookie der Anwendung (`overlord_sprache`, kein `HttpOnly`); am Ende auf `de` zurückgesetzt, den Wert des Nutzers |
| **Zeilen** | Nachrichtenliste 300 (24 h und 30 Tage, beide Mandanten, beide Sprachen bei `NEXANS`), Trefferliste 101 (`NEXANS` 50 je Sprache, `VOTG` 1), Benutzertabelle 12 (6 Konten, zwei Sprachen), Katalog 1.856 (733 × 2 und 390) |
| **Belegart-Bezeichnungen** | vollständig aus `GET /api/bam/suchfelder`: `NEXANS` 40 Typen, längste 279,22 px; `VOTG` 12, längste 161,95 px |

**Zwei Fehler des neuen Kerns, beide in dieser Runde gefunden, behoben und nachgemessen:**

1. **`word-break` mit abgeschaltet.** Die erste Fassung setzte in der Sonde auch `word-break` auf
   `normal` und machte damit Kennungen unteilbar, die mit `break-all` gebaut sind. Der Katalog meldete
   für den Prozess 340 px, die Benutzertabelle für den Namen 106 px. Benutzertabelle und Katalog neu
   gemessen; Prozess **316,3** px (der längste Prozessname), Benutzer **25** px (die Kennung bricht).
2. **Die Wortrechnung teilte nur am Leerzeichen.** „EDI-Betreuung" zählte als ein Wort (117,97 px);
   der Browser bricht nach dem Bindestrich regulär um, gemessen **88,14** px. Die Benutzertabelle neu
   gemessen. **Die Frage an den Auftraggeber war vor dieser Korrektur gestellt** (Abweichung 2).

> ### Belegvermerk zu M177 (Regel L10)
>
> *Gemessen war:* am Produktionsbau im Chrome 152 der Erweiterung, Dichte `m`, beide Mandanten, beide
> Sprachen — je Tabelle und Spalte die Kopfbeschriftung einzeilig, jede Zelle in einer Sonde einzeilig
> und ohne Notumbruch, die Plakette allein, und die Rechnung über die endlichen Mengen; dazu alle
> Belegart-Bezeichnungen je Mandant in der Schrift der Trefferzelle.
>
> *Behauptet wird:* dass die Mindestbreiten in §5 den längsten Inhalt tragen, **der in diesen Zeilen und
> in diesen Beschriftungsmengen vorkommt**.
>
> *Behauptet wird nicht:* dass kein längerer Ablauf-, Prozess- oder Partnername existiert — die
> Produktion kennt mehr Zeilen als die Testkopie; dass die Zahlen in den Dichtestufen `xs`, `s` und `l`
> auf den Pixel dieselben sind; dass ein Benutzername der Produktion die Kopfbeschriftung übersteigt
> (er bricht mit `break-all`); dass die „+n"-Endung der Trefferzelle je hineinpasst.

---

## 5. Die Schwellen je Tabelle

### 5.1 Die Trefferliste

`features/nachrichten/treffer-spalten.ts`, Container `trefferliste`.

| Spalte | Mindestbreite | `rem` | gemessen an | vorher |
|---|---:|---:|---|---|
| Zeitpunkt | **187** px (186,53) | 11.6875 | der englischen Form `12/30/2025, 04:02:04 AM`, breiteste Ziffer 0 — in den Daten derselbe Wert | 184 px |
| Status | **155** px (154,78) | 9.6875 | der breitesten der acht Einordnungen als Plakette („Zusammengeführt") | 168 px, ab `lg` 240 px |
| Treffer | **280** px (279,22) | 17.5 | der längsten Belegart-Bezeichnung (`NEXANS`) — in den Daten dieselbe | 208 px, ab `lg` 256 px |
| Kette | **74** px (73,67) | 4.625 | der längsten Rolle einzeln („Aufgeteilt") | 136 px |
| Ablauf *(frei)* | **304** px (303,33) | — | dem längsten Ablaufnamen der Nachrichtenliste (§3, Regel 4) | der Rest; bei 768 px 0 px |

| Stufe | Spalten | Schwelle | Klasse |
|---|---|---:|---|
| Grundmenge mit „Treffer" | Zeitpunkt · Status · Treffer · Kette | **696** px | — |
| Ablauf | + Ablauf | **1.000** px | `hidden @min-[62.5rem]/trefferliste:table-cell` |
| Grundmenge ohne „Treffer" (reine Feldsuche) | Zeitpunkt · Status · Kette | **416** px | — |
| Ablauf | + Ablauf | **720** px | `hidden @min-[45rem]/trefferliste:table-cell` |

**Unter der Grundmenge** läuft die Tabelle in ihrer Hülle über, wie seit Schritt 7.

> ### ⚠️ Korrektur 22.09.2026 — ohne die Spalte „Kette" (E‑231)
>
> Die Trefferliste hat die Spalte „Kette" verloren (Entscheidung des Auftraggebers,
> [`bam-suche.md`](bam-suche.md) §11.5, Korrekturblock). Die beiden Tabellen oben nennen den Stand
> bis dahin und bleiben stehen. **Die Rechnung ist dieselbe, mit einer Spalte weniger** — die
> Breiten sind weiterhin die aus M177 in `treffer-spalten.ts` (Zeitpunkt 187, Status 155, Treffer
> 280, Ablauf 304 px); die Zahlen aus M180 sind **nicht** übernommen, Punkt 183 bleibt offen (§11).
>
> | Stufe | Spalten | Schwelle | Rechnung | Klasse |
> |---|---|---:|---|---|
> | Grundmenge mit „Treffer" | Zeitpunkt · Status · Treffer | **622** px | 187 + 155 + 280 | — |
> | Ablauf | + Ablauf | **926** px | 622 + 304; 926 ÷ 16 = 57,875 | `hidden @min-[57.875rem]/trefferliste:table-cell` |
> | Grundmenge ohne „Treffer" (reine Feldsuche) | Zeitpunkt · Status | **342** px | 187 + 155 | — |
> | Ablauf | + Ablauf | **646** px | 342 + 304; 646 ÷ 16 = 40,375 | `hidden @min-[40.375rem]/trefferliste:table-cell` |
>
> Jede Schwelle ist um genau die 74 px der Kette gesunken (1.000 → 926, 720 → 646). Unter der
> Grundmenge gilt weiter die heutige Bauform. Gegenprobe mit Mutanten in §9 (Nachtrag vom
> 22.09.2026).

### 5.2 Die Benutzertabelle

`features/benutzer/spalten.ts`, Container `benutzertabelle`.

| Spalte | Mindestbreite | `rem` | gemessen an | vorher |
|---|---:|---:|---|---|
| Benutzer | **81** px (80,23) | 5.0625 | der Kopfbeschriftung; die Kennung bricht mit `break-all` | 128 px, ab `md` 176 px |
| Rolle | **89** px (88,14) | 5.5625 | „EDI-" \| „Betreuung" — der Umbruch nach dem Bindestrich ist regulär | 144 px ab `md` |
| Mandanten | **96** px (95,28) | 6 | der Kopfbeschriftung | der Rest; bei 768 px 0 px |
| Sperre | **74** px (73,63) | 4.625 | dem längsten Wort der beiden Sperrtexte | 96 px, ab `md` 120 px |
| Zeitsperre | **106** px (105,42) | 6.625 | dem Zeitpunkt hinter „bis" — in keiner der sechs Zeilen | 112 px, ab `md` 176 px |
| Konto | **97** px (96,38) | 6.0625 | „deaktiviert" / „deactivated" — in keiner der sechs Zeilen | 96 px, ab `md` 120 px |
| Passwort | **95** px (94,28) | 5.9375 | „Wechsel erforderlich" — in keiner der sechs Zeilen | 144 px ab `lg` |
| Letzte Anmeldung | **145** px (144,48) | 9.0625 | der Kopfbeschriftung, einzeilig | 176 px ab `lg` |
| Bearbeiten | **48** px | 3 | dem Knopf | 56 px |

| Stufe | Spalten | Schwelle | Klasse |
|---|---|---:|---|
| Grundmenge | Benutzer · Sperre · Zeitsperre · Konto · Bearbeiten | **406** px | — |
| 1 | + Rolle · Mandanten | **591** px | `hidden @min-[36.9375rem]/benutzertabelle:table-cell` |
| 2 | + Passwort · Letzte Anmeldung | **831** px | `hidden @min-[51.9375rem]/benutzertabelle:table-cell` |

**Welche Spalte weicht, ist unverändert** ([`benutzerverwaltung-frontend.md`](benutzerverwaltung-frontend.md)
§4). **Unter der Grundmenge** läuft die Tabelle über.

**Diese Tabelle hat seit dem 16.09.2026 keine freie Spalte** — die Mandanten tragen ihre 96 px wie
jede andere, und der Überschuss teilt sich anteilig (E‑149, §5.5). Die drei Schwellen sind davon
unberührt: An jeder ist der Überschuss null.

### 5.3 Der Prozess-Katalog

`features/katalog/spalten.ts`, Container `katalog`.

| Spalte | Mindestbreite | `rem` | gemessen an | vorher |
|---|---:|---:|---|---|
| Prozess | **317** px (316,3) | 19.8125 | dem längsten unteilbaren Stück eines Prozessnamens (`NEXANS`); die Kennung bricht mit `break-all` | 144 / 192 / 256 / 320 px an Basis, `sm`, `md`, `lg` |
| Projekt | **163** px (162,5) | 10.1875 | dem längsten Wort eines Projektnamens (`VOTG`) | 224 px ab `lg` |
| Partner *(frei)* | **145** px (144,83) | — | dem längsten Wort eines Partnernamens (`VOTG`) | der Rest; bei 768 px 0 px |
| Richtung | **95** px (94,19) | 5.9375 | dem längsten Wort der Richtungstexte | 128 px ab `md` |
| Nachrichten | **106** px (105,42) | 6.625 | dem Zeitpunkt hinter „geprüft am" | 176 px ab `md` |
| Pflege | **76** px (75,56) | 4.75 | der Kopfbeschriftung | 96 px, ab `md` 136 px |

| Stufe | Spalten | Schwelle | Klasse |
|---|---|---:|---|
| Bauform | Prozess 9 rem · Partner · Pflege 6 rem | unter **538** px | `w-[9rem]`, `w-[6rem]` |
| Grundmenge | Prozess · Partner · Pflege in ihrer Mindestbreite | **538** px | `@min-[33.625rem]/katalog:w-[19.8125rem]`, `…:w-[4.75rem]` |
| 1 | + Richtung · Nachrichten | **739** px | `hidden @min-[46.1875rem]/katalog:table-cell` |
| 2 | + Projekt | **902** px | `hidden @min-[56.375rem]/katalog:table-cell` |

**Der Katalog kürzt nicht, er bricht um** — daran ändert sich nichts. Unter 538 px gilt die dreispaltige
Bauform mit drei- bis fünfzeiligen Zeilen; der Partner bekommt dort den Rest und **nie** 0 px.

---

### 5.4 Die Nachrichtenliste *(nachgetragen am 16.09.2026, E‑148)*

`features/nachrichten/spalten.ts`, Container `nachrichtenliste`. **Am 15.09.2026 war sie der Befund
aus §6** — gebaut ist sie einen Tag später, nachdem der Auftraggeber den Klumpen erneut gemeldet hat
(§6, Nachtrag). Die Zahlen sind unverändert die aus M177; entschieden ist, was dort offen stand.

| Spalte | Mindestbreite | `rem` | gemessen an | vorher |
|---|---:|---:|---|---|
| Zeitpunkt | **187** px (186,53) | 11.6875 | der englischen Form `12/30/2025, 04:02:04 AM`, breiteste Ziffer | 184 px |
| Status | **155** px (154,78) | 9.6875 | der breitesten der acht Einordnungen als Plakette | 168 px, ab `lg` 272 px |
| Ablauf *(frei)* | **304** px (303,33) | — | dem längsten Ablaufnamen, 300 Zeilen beider Mandanten | der Rest; bei 768 px 0 px |
| Projekt | **286** px | 17.875 | dem längsten Projektnamen | 288 px, ab `md` |

| Stufe | Spalten | Schwelle | Klasse |
|---|---|---:|---|
| Grundmenge | Zeitpunkt · Status · Ablauf | **646** px | — |
| Projekt | + Projekt | **932** px | `hidden @min-[58.25rem]/nachrichtenliste:table-cell` |

**Unter der Grundmenge gilt die heutige Bauform** (Antwort des Auftraggebers vom 15.09.2026):
Zeitpunkt und Status behalten ihre Breite, der Ablauf bekommt den Rest und kürzt — mit vollem
`title`, wie bei jeder Breite. Das ist die schmale Spalte neben dem Baum (294 px) und das Fenster
unter 430 px. Bei 294 px bleiben dem Ablauf 0 px; **das ist derselbe Zustand wie vorher und keine
neue 0-px-Spalte über einer Schwelle** — die Grundmenge ist dort nicht erreicht.

**Was die Runde kostet, und es steht hier und nicht im Kleingedruckten:** Zwischen 646 und 932 px
Kastenbreite ist das **Projekt nicht zu sehen**. Vorher stand es dort — aber ohne Platz für den
Ablauf, und mit seiner Beschriftung über der des Ablaufs. Das ist der Tausch: eine Spalte weniger
gegen die Spalte, an der man die Zeile erkennt.

| Kasten | wo er vorkommt | vorher | seit E‑148 |
|---:|---|---|---|
| 294 px | Liste neben dem Baum, `md` | Ablauf 0 px, Projekt weg | unverändert (Bauform unter der Grundmenge) |
| 518 px | 768 px Fenster, eigene Route | Ablauf **0 px**, „Ablauf" über „Projekt" | Ablauf 176 px, Projekt weg |
| 585 px | 1280 px Fenster, neben Baum oder Panel | Ablauf **0 px**, dasselbe übereinander | Ablauf 243 px, Projekt weg |
| 652 px | 894 px Fenster, eigene Route *(die Meldung vom 16.09.2026)* | Ablauf **12 px**, Beschriftungen übereinander | Ablauf 310 px, Projekt weg |
| 932 px | ab ~1.180 px Fenster auf der eigenen Route | Ablauf 306 px | **alle vier Spalten**, Ablauf 304 px |
| 1.161 px | 1920 px Fenster neben dem Panel | Ablauf 535 px | vier Spalten, Ablauf 533 px |

#### Im Browser nachgemessen — die Schwelle, nicht der Testrechner

An der **gebauten Tabelle** im Chrome des Auftraggebers, Route `/nachrichten`, Fenster 915 px. Die
Kastenbreite ist am `@container`-Element über eine **Inline-Breite gestellt** und danach wieder
entfernt; gelesen sind `getBoundingClientRect` je Kopfzelle, `getComputedStyle(…).display` an `th`
**und** `td` der Projektspalte und — als Prüfung auf den alten Befund — der Überhang der
Beschriftung über die rechte Kante ihrer Zelle (`Range.getBoundingClientRect`):

| Kasten | Zeitpunkt | Status | Ablauf | Projekt | Kopf über der Nachbarin |
|---:|---:|---:|---:|---|---|
| 645 px | 187 | 155 | 303 | `display: none` | keiner |
| **646 px** *(Grundmenge)* | 187 | 155 | **304** | `display: none` | keiner |
| 650 px *(das Fenster, wie es stand)* | 187 | 155 | 308 | `display: none` | keiner |
| 931 px | 187 | 155 | 589 | `display: none` | keiner |
| **932 px** *(Schwelle)* | 187 | 155 | **304** | **286** | keiner |
| 1.161 px | 187 | 155 | 533 | 286 | keiner |

**Die freie Spalte landet an beiden Kanten auf den Pixel auf ihrer Mindestbreite** — 304 px bei 646
und wieder bei 932. Das ist die Rechnung aus `lib/spaltenwahl.ts`, im Browser bestätigt und nicht
nur in `jsdom` gerechnet. **Das Projekt verschwindet über `display: none` an beiden Zellenarten**,
nicht über eine Breite von 0 px.

> **Belegvermerk** *(Regel L10)*. *Gemessen:* Chrome des Auftraggebers, **Entwicklungsbau** auf
> `:3000` (der lief; ein zweiter Produktionsbau ist dafür nicht aufgesetzt worden), Mandant und
> Sitzung die des Nutzers, `n` = 6 Kastenbreiten × 4 Spalten. *Nicht gemessen:* der zweite Mandant,
> die englische Sprache, die übrigen Dichtestufen, Firefox — und **nicht** die Breiten, die sich aus
> einem echten Fenster ergeben; gestellt ist die Breite des Kastens, nicht die des Fensters. Für den
> Zusammenhang Fenster → Kasten gilt weiter M176.

**Die Statusspalte ist ab `lg` nicht mehr breiter** — dieselbe Folge der Antwort „Mindestbreite" wie
in der Trefferliste (§1, Abweichung 10). Der Zusatz „Schritt: …" kürzt jetzt bei jeder Breite und
steht vollständig im `title`. **Damit sind Liste und Trefferliste wieder Spalte für Spalte gleich
breit**, was die Zusage des Endpunkts verlangt ([`bam-suche.md`](bam-suche.md) §1).

> ### ⚠️ Korrektur vom 16.09.2026, zweite Meldung — Schwelle 1.167 px, und ab ihr nimmt das Projekt den Rest (E‑162)
>
> **Der Abschnitt bleibt wortgleich stehen**, die Zahlen darin sind überholt
> ([`nachrichtenliste.md`](nachrichtenliste.md) §8.1, Kasten unter „Umgesetzt über `table-fixed`",
> M180). Gemeldet hat es der Auftraggeber mit einem Bild aus der Produktion, Dichte `xs`: Die Plakette
> „Zusammengeführt" war gekürzt, neben „Wartend" stand nur „Schritt…", und zwischen Ablauf und
> Projekt lagen rund 700 px leer. Die Ursache war der Ablauf als freie Spalte bei jeder Breite.
>
> | | bis heute (dieser Abschnitt) | seit E‑162 |
> |---|---|---|
> | Status | 155 px | 156 px (9,75 rem), **ab der Schwelle 286 px** (17,875 rem) |
> | Ablauf | frei | frei unter der Schwelle, **ab ihr 408 px** (25,5 rem) |
> | Projekt | 286 px ab 932 px | **frei** ab 1.167 px (`@min-[72.9375rem]/nachrichtenliste`) |
> | Grundmenge | 646 px | 751 px |
>
> **Zwei Mindestbreiten aus M177 waren zu klein.** Die Plakette braucht bei `xs` 9,692 rem, weil
> ihr 1‑px-Rahmen nicht mit der Schrift skaliert; gemessen hatte M177 nur `m` (§3, „Dichte"). Und der
> breiteste der **1.403** Ablaufnamen misst 407,56 px, M177 hatte in 300 Zeilen höchstens 304 px
> gesehen. **Die Mechanik hat sich nicht geändert** — eigene Hülle, eine Schwelle, `display: none`.
> Neu ist in `lib/spaltenwahl.ts` der **Umbau an einer Stufe**: Ab ihrer Schwelle tragen Spalten, die
> schon dastehen, eine andere Breite, und die freie Spalte wechselt. **Kein neuer Umbruchpunkt:**
> Die eine Schwelle der Liste ist verschoben, nicht ergänzt.
>
> **„Spalte für Spalte gleich breit" gilt damit nicht mehr.** Die Trefferliste behält 155 px Status
> und 304 px Ablauf, weil sie nach dem Auftrag nicht umgebaut wird. Offen als Punkt **183** (§11).

---

### 5.5 Der Überschuss — wohin er geht, wenn die freie Spalte ihn nicht braucht *(16.09.2026, E‑149)*

**Der Befund des Auftraggebers**, gemeldet am breiten Fenster: *„Die ersten 3 sind sehr nah
aneinander, dann kommt eine riesen Lücke."* Auf `/benutzer` klebten Benutzer, Rolle und Mandanten
aneinander — der Benutzername brach nach sieben Zeichen um, „EDI-Betreuung" nach dem Bindestrich —,
und rechts daneben stand leere Fläche.

**Die Ursache steht in §2.** Die Antwort vom 15.09.2026 regelt, wie breit eine **feste** Spalte ist.
Was sie nicht regelt: wohin der **Rest** geht, wenn die freie Spalte ihn nicht braucht. Er ging
vollständig an die eine Spalte ohne Breite — und das ist hier die mit dem kürzesten Inhalt.

| Kasten 1.408 px | Benutzer | Rolle | Mandanten | Sperre | Zeitsperre | Konto | Passwort | Letzte Anm. | Bearb. |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| **vorher** | 81 | 89 | **673** | 74 | 106 | 97 | 95 | 145 | 48 |
| **seit E‑149** | 137,2 | 150,8 | 162,7 | 125,4 | 179,6 | 164,4 | 161,0 | 245,7 | 81,3 |

> **E‑149:** **Eine Tabelle hat nur dann eine freie Spalte, wenn deren Inhalt den Platz auch nutzt.**
> Sonst trägt jede Spalte ihre gemessene Mindestbreite, und der Überschuss teilt sich anteilig.
> Ein gekürzter Ablaufname zeigt mit jedem Pixel mehr — eine Reihe kurzer Marken nicht.

**Anteilig ist keine Wahl, sondern das Verhalten von `table-layout: fixed`:** Sind alle sichtbaren
Spalten bemaßt und ist der Kasten breiter als ihre Summe, verteilt der Browser die Differenz **im
Verhältnis der Breiten**. Das steht in M177 zweimal gemessen, und beide Reihen treffen die Rechnung
*Mindestbreite × Kasten ÷ Spaltensumme* auf **±0,04 px**:

| gemessen (M177, §7.2) | Kasten | gemessene Spaltenbreiten | dieselbe Reihe gerechnet |
|---|---:|---|---|
| Trefferliste, Ablauf nicht sichtbar | 718 px | 192,91 · 159,89 · 288,84 · 76,36 | 192,91 · 159,90 · 288,85 · 76,34 |
| **Benutzertabelle, Mandanten ausgeblendet** | 520 px | 103,73 · 94,77 · 135,75 · 124,23 · 61,52 | 103,74 · 94,78 · 135,76 · 124,24 · 61,48 |

**Die zweite Zeile ist der eigentliche Beleg.** Unterhalb von 591 px stand die freie Spalte gar nicht
da — und dort teilte **dieselbe Tabelle** den Überschuss längst anteilig auf. E‑149 macht aus diesem
Sonderfall den Normalfall; die schmale Bauform bleibt damit genau die, die schon vorher galt.

**Was es kostet, und es steht hier:** Spalten mit endlicher Aussage werden breiter, als sie brauchen —
„Letzte Anmeldung" bekommt 245,7 px für einen 145 px breiten Zeitpunkt. Der Tausch ist bewusst: Eine
gleichmäßig zu weite Tabelle liest sich als Tabelle, eine mit einer 673-px-Lücke neben drei
zusammengeklebten Spalten nicht. **Die Alternative** — die Tabelle am breiten Fenster über eine
Höchstbreite gar nicht erst so weit wachsen zu lassen — wäre eine Entscheidung über den Rahmen und
nicht über diese Tabelle; sie steht offen.

**Nicht geändert:** die drei Schwellen (406 / 591 / 831 px), welche Spalte wann weicht, jede
Mindestbreite, und **die drei übrigen Tabellen**. Trefferliste, Nachrichtenliste und Katalog behalten
ihre freie Spalte: Ablauf und Partner sind gekürzte bzw. umbrechende Namen, und dort zeigt jeder
zusätzliche Pixel mehr Text.

> **Belegvermerk** *(Regel L10)*. *Gemessen:* die beiden Reihen aus M177 — sie tragen die Aussage über
> `table-layout: fixed`, und die zweite stammt aus genau dieser Tabelle. *Gerechnet, nicht gemessen:*
> die Zeile „seit E‑149" (Faktor 1.408 ÷ 831). **Die Sichtprobe im Browser steht aus:** Der Tab, den
> ich öffnen kann, ist nicht angemeldet, und angemeldet wird nur durch den Auftraggeber. Bau und
> Testlauf sind grün, und die Klasse steht wörtlich im Quelltext.

---

## 6. Die Nachrichtenliste — Befund, nicht gebaut

**Der Fall aus 3.4 des Auftrags.** Die Spalte, an der man eine Zeile erkennt, ist der **Ablauf**, und er
soll als letzte weichen. Gemessen (§4, 300 Zeilen, beide Mandanten, Zeitpunkt und Status in beiden
Sprachen):

| Spalte | Mindestbreite | gemessen an |
|---|---:|---|
| Zeitpunkt | **187** px (deutsch 154 px) | `12/30/2025, 04:02:04 AM` bzw. `30.12.2025, 04:02:04`, breiteste Ziffer |
| Status | **155** px | „Zusammengeführt" als Plakette |
| Ablauf | **304** px | längster Ablaufname (`VOTG`); Median seines Laufs 259,58 px, 90. Perzentil 278,41 px |
| Projekt | **286** px | längster Projektname |

| Kasten | Fenster | Zeitpunkt + Status | bleibt für den Ablauf | er braucht |
|---:|---:|---:|---:|---:|
| **334** px | 360 px | 342 px | **−8 px** (deutsch 25 px) | 304 px |
| 364 px | 390 px | 342 px | 22 px | 304 px |
| 404 px | 430 px | 342 px | 62 px | 304 px |
| 518 px | 768 px | 342 px | 176 px | 304 px |
| 718 px | 744 px | 342 px | 376 px | 304 px |

**Beim schmalsten Container bleibt keine brauchbare Breite.** Die Grundmenge Zeitpunkt · Status · Ablauf
braucht 646 px — mehr als der Kasten bei 768 px Fensterbreite (518 px) —, und das Projekt käme erst ab
932 px dazu. **Nicht gebaut**, wie der Auftrag es für diesen Fall vorschreibt: keine 0-px-Spalte, keine
Kartendarstellung, kein Ausweichen. Die Liste zeichnet weiter, was sie vorher zeichnete.

**Was zu entscheiden wäre** — hier vorgelegt und nicht vorweggenommen: welche von Zeitpunkt und Status
unter 646 px weicht, oder ob der Ablauf schmaler als sein längster Name stehen darf (er kürzt mit vollem
`title`, wie heute) und welche Breite dann „brauchbar" heißt.

**Die offene Zahl aus M176 ist nachgesehen** — `scrollWidth` 591 (`NEXANS`) bzw. 647 (`VOTG`) des
Tabellenkastens bei 360 bis 430 px: Es sind die **50 `sr-only`-Spannen der Ablaufzelle**, als
`position: absolute` am Kasten, hinter dem ungekürzten Text. Beschrieben und nicht behoben in
[`nachrichtenliste.md`](nachrichtenliste.md) §8.1 (Korrektur vom 15.09.2026); sie gehört zu Punkt 114.

> ### Nachtrag vom 16.09.2026 — gebaut, E‑148, Punkt 114 geschlossen
>
> **Der Abschnitt oben bleibt stehen**, weil er die Zahlen trägt, auf denen die Entscheidung steht.
> Überholt ist allein sein Schluss („nicht gebaut").
>
> **Der Anlass:** Der Auftraggeber hat den Klumpen am 16.09.2026 erneut gemeldet, mit einem Bild von
> der eigenen Route bei **894 px** Fensterbreite — „Ablauf" und „Projekt" übereinander gedruckt. Das
> ist derselbe Befund wie bei 768 px, nur an einer Breite, die Teil 1 nicht gemessen hatte: Kasten
> 652 px, dem Ablauf bleiben 12 px.
>
> **Die Entscheidung (E‑148):** *Das Projekt kommt erst, wenn alle vier Spalten ihre Mindestbreite
> tragen.* Die Schwelle ist damit **932 px** — hergeleitet wie jede andere, als Summe 187 + 155 + 304
> + 286. Der Ablauf bleibt die freie Spalte und weicht als letzte (Auftrag Teil 2, 3.4); unter 646 px
> gilt die heutige Bauform.
>
> **Warum das den Fall aus 3.4 nicht aufhebt:** Bei 334 px bleiben dem Ablauf weiterhin −8 px. Der
> Unterschied ist, dass diese Breite **unter** der Grundmenge liegt und dort die heutige Bauform gilt
> — genau wie bei Benutzertabelle und Trefferliste. Die Runde vom 15.09.2026 hat die Grundmenge als
> Bedingung für den Bau gelesen; sie ist es nicht. **Gemeldet, nicht still aufgelöst.**
>
> **Die Wahl, die nicht getroffen wurde:** Der Ablauf könnte schmaler stehen als sein längster Name —
> beim 90. Perzentil (278,41 px) läge die Schwelle bei 906 statt 932 px. **26 px sind den Bruch der
> Regel nicht wert**, dass eine Mindestbreite am längsten tatsächlichen Inhalt gemessen wird (§3,
> Regel 4). Wer sie doch will, ändert eine Zahl in `spalten.ts`, und der Test sagt, ob die Klasse
> mitgegangen ist.
>
> **Nicht behoben:** die 591 / 647 px Scrollbreite. Sie hängt an den `sr-only`-Spannen und nicht an
> der Spaltenbreite — sie steht jetzt als eigener Punkt **181** und nicht mehr unter 114.

---

## 7. Die Gegenprobe im Browser

*Nach dem Bau, 15.09.2026.* Derselbe Messkern wie in M176 (`messung.js`, unverändert), derselbe
Rahmen, **dieselben neun Zustände, dieselben fünf Breiten (360, 390, 430, 744, 768 px), beide
Mandanten** — am Produktionsbau mit dem Umbau (`next build` + `next start`, `:3001`). Verglichen wird
Datei gegen Datei mit `scripts/sichtprobe-schmal/vergleich-m177.mjs`: vorher die Ergebnisdateien aus
Teil 1, nachher `ergebnis/rahmen/nachher/`.

### 7.1 Die Zusagen des Rahmens — unverändert

| | |
|---|---|
| **Messungen** | **90** — neun Zustände × fünf Breiten × zwei Mandanten |
| **gebrochene Zusagen** | **0.** Kein waagerechter Überlauf am Dokument (`scrollWidth` = `innerWidth` bei allen 90), das Dokument scrollt nie, kein neuer Scrollbereich neben `main`, die Kopfzeile unter 768 px bei allen 72 Messungen dreizeilig mit sichtbarem Mandantencode und dem Suchfeld als eigener Zeile |
| **bekannt aus M176** | **18** — die Zeilenzahl der Kopfzeile bei 768 px. Die Dateien aus Teil 1 tragen dort noch die Zählung des alten Kerns (zwei Zeilen), die M176 selbst als Werkzeugfehler führt und mit **einer** Zeile ausweist ([`messungen-sichtprobe-schmal.md`](messungen-sichtprobe-schmal.md) §1, Abweichung 6); nachher überall eine |
| **scrollt nicht mehr** | **2** — die Benutzerverwaltung bei 768 px, beide Mandanten: `main` 1.007 → **973** px bei 973 px Höhe, der Inhalt passt ins Fenster |

### 7.2 Die Tabellen — was sich geändert hat

Beide Mandanten zeigen dieselben Zahlen, wo nichts anderes steht.

| Tabelle · Fenster | Kasten | Tabelle | 0-px-Spalten | Kopfschnitte | gekürzt ohne `title` | Spalten nachher (px) |
|---|---:|---|---|---|---|---|
| **Trefferliste · 768** | 518 | 696 → 696 | 1 → **0** | 0 → 0 | 1 → **0** | Zeitpunkt 187 · Status 155 · Treffer 280 · Kette 74 |
| Trefferliste · 744 | 718 | 718 → 718 | 0 → 0 | 0 → 0 | 0 → 0 | 192,91 · 159,89 · 288,84 · 76,36 — ohne sichtbare freie Spalte verteilt `table-layout: fixed` die übrigen 22 px anteilig |
| Trefferliste · 360 | 334 | 696 → 696 | 0 → 0 | 0 → 0 | 0 → 0 | dieselben vier; die Tabelle läuft in ihrer Hülle über, wie vorher |
| **Benutzertabelle · 768** | 520 | 792 → **520** | 1 → **0** | 1 → **0** | 8 → **1** | Benutzer 103,73 · Sperre 94,77 · Zeitsperre 135,75 · Konto 124,23 · Bearbeiten 61,52 |
| Benutzertabelle · 744 | 720 | 720 → 720 | 0 → 0 | 0 → 0 | 1 → 1 | Benutzer 81 · Rolle 89 · **Mandanten 225** · Sperre 74 · Zeitsperre 106 · Konto 97 · Bearbeiten 48 |
| Benutzertabelle · 430 | 406 | 488 → **406** | 0 → 0 | 0 → 0 | 1 → 1 | die Grundmenge, genau im Kasten |
| Benutzertabelle · 360 | 336 | 488 → **406** | 0 → 0 | 0 → 0 | 1 → 1 | die Grundmenge; Überlauf 152 → **70** px |
| **Katalog · 768** | 520 | 696 → **520** | 1 → **0** | 1 → **0** | 734 → **0** (`NEXANS`), 391 → **0** (`VOTG`) | die Bauform: Prozess 144 · Partner 280 · Pflege 96 |
| Katalog · 744 | 720 | 720 → 720 | 0 → 0 | 0 → 0 | 0 → 0 | Prozess 317 · Partner 327 · Pflege 76 |
| Katalog · 360 | 336 | 336 → 336 | 0 → 0 | 0 → 0 | 0 → 0 | unverändert: Prozess 144 · Partner 96 · Pflege 96 |
| Nachrichtenliste | — | **unverändert** in allen Zahlen | | | | nicht gebaut (§6) |

**Das eine gekürzte Element ohne `title` in der Benutzertabelle** ist der Kopf „Bearbeiten": Er steht
`sr-only` und wird vom Messkern als gekürzt gezählt, vorher wie nachher (M176 §4.8).

**Die Höhe von `main` im Katalog** — die Zeilen, die bei 768 px bis zu 739 px hoch wurden:

| | 744 px | 768 px | je Zeile bei 768 px |
|---|---|---|---|
| `NEXANS`, 733 Zeilen | 58.140 → **52.496** | 185.708 → **68.610** | 253 → **94** px |
| `VOTG`, 390 Zeilen | 35.851 → **28.219** | 75.925 → **44.501** | 195 → **114** px |

### 7.3 Die zwei kleinen Befunde

| Gekürzte Werte außerhalb von Tabellen ohne `title` | vorher → nachher |
|---|---|
| Detail und Panel, 360 px (Punkt 176) | `NEXANS` 1 → **0**, `VOTG` 2 → **0** |
| Detail und Panel, 390 px (Punkt 176) | 1 → **0** bei beiden |
| alle neun Zustände, 768 px — der Produktname (Punkt 177) | 1 → **0** bei beiden |

### 7.4 Die klebende Kopfzeile unter der Container-Hülle

`container-type` macht die Hülle nicht zum Scrollbereich, und das ist nachgemessen: Katalog bei
`main.scrollTop` 900, Benutzertabelle bei 230 (Rahmen 1.600 × 420 px) — Kopfzeile und `main` beide bei
**y = 51**, das Dokument scrollt nicht, `container-type` der Hülle `inline-size`.

> ### Belegvermerk zur Gegenprobe (Regel L10)
>
> *Gemessen war:* der Produktionsbau nach dem Umbau im Chrome 152 der Erweiterung, Rahmen mit DPR 1 und
> ohne Rinne des Scrollbalkens, **Dunkelmodus, Dichte `m`, `pointer: fine`, Deutsch**, Rolle `ADMIN`,
> `NEXANS` und `VOTG`; dieselben Prüfdaten wie in M176.
>
> *Behauptet wird:* dass die Zusagen des Rahmens bei diesen 90 Messungen unverändert halten und dass die
> Tabellen die Zahlen aus §7.2 zeigen.
>
> *Behauptet wird nicht:* dass ein Gerät mit `pointer: coarse` dieselben Zahlen zeigt (Punkt 178); dass
> Englisch, eine andere Dichtestufe oder die Rolle `MANDANT` sie zeigen; dass Firefox sie zeigt.
>
> ⚠️ **Ein Werkzeugbefund, in dieser Runde gefunden:** Im verdeckten Tab drosselt Chrome verkettete
> `setTimeout` nach wenigen Minuten auf etwa einen Aufruf je Minute; der erste Anlauf der Gegenprobe stand
> nach 45 Minuten bei der zweiten Route. `rahmen.js` wartet im verdeckten Tab seitdem über
> `MessageChannel`; der zweite Anlauf lief in einem Zug durch. Die Zahlen des ersten, verworfenen Anlaufs
> sind nicht verwendet — bis auf das Dashboard von `NEXANS`, das vor der Drosselung fertig war.

---

## 8. Die Rollenauswahl

Beschrieben in [`benutzerverwaltung-frontend.md`](benutzerverwaltung-frontend.md) §18: die drei
`color-scheme`-Werte vor dem Umbau, der Baustein, die Tastatur, die Messung in Chromium in allen drei
Zuständen, die übrigen nativen Formularelemente mit Fundstelle und offener Punkt **180**.

**Nachtrag vom 16.09.2026** (§18.1 dort): Der Auftraggeber hat dasselbe Bild für das Feld daneben
gemeldet — „Prozessbaum beginnt mit". **Auch es trägt jetzt die Liste der Anwendung**, und die
Bauform ist dabei aus dem Feature nach `components/auswahl-feld.tsx` gezogen: Mit der zweiten Stelle
gehört sie dorthin, und die dritte (Mandant, Massenzuordnung) liegt außerhalb der Benutzerverwaltung.
Am laufenden System im Zustand `dunkel` nachgemessen — Fläche, Schrift, gewählte Zeile und Zeilenhöhe
treffen `--popover`, `--popover-foreground`, `--accent` und `--dichte-bedienzeile`; im geöffneten
Zeilenformular steht **kein** `<select>` mehr.

---

## 9. Tests

**`tests/spaltenwahl.test.tsx`** — gerenderter Baum, **sechs Fälle** *(an zwei Meldungen des
16.09.2026 gewachsen; vorher vier)*: die **Nachrichtenliste**, die Trefferliste mit und ohne Spalte
„Treffer", die Benutzertabelle, der Katalog — und die Benutzertabelle ein zweites Mal **am breiten
Fenster** (E‑149). Je Tabelle:

1. **Hergeleitet, nicht gewählt.** Die Zahl in `@min-[…]/<name>:table-cell` ist auf den Pixel die Summe der
   Mindestbreiten aus `…spalten.ts`; jede feste Spalte trägt ab ihrer Schwelle genau ihre Mindestbreite,
   die freie keine.
2. **An den gemessenen Containerbreiten** (M176 §4.7–§4.9: 334, 364, 404, 718, 518 px bzw. 336, 366, 406,
   720, 520 px) **und an jeder Kante** (Grundmenge und jede Schwelle, ein Pixel darunter und genau darauf):
   dieselben Spalten wie die Rechnung, keine sichtbare Spalte unter ihrer Mindestbreite, kein Überlauf ab
   der Grundmenge; darunter die Bauform der Tabelle.
3. **`td` wie `th`** — dieselbe Sichtbarkeitsklasse an jeder Zelle der Spalte.
4. **Keine Fensterschwelle** (`sm:` bis `2xl:`) an einer Zelle.
5. **Der Überschuss gehört allen Spalten** (E‑149): Hat eine Tabelle keine freie Spalte, trägt
   **jede** eine Breitenklasse, und bei 1.408 px Kasten steht jede auf ihrer Mindestbreite mal
   1.408 ÷ 831 — ein Faktor für alle. Die zwei Zahlen des Befunds stehen als Gegenprobe im Test:
   Mandanten 163 px statt 673, Benutzer 137 px statt 81.

⚠️ `jsdom` rechnet kein Layout; die Breiten unter 2. sind nach `table-layout: fixed` **gerechnet**. Den
Beleg im Browser trägt §7.

### Die Gegenprobe mit Mutanten — in beide Richtungen geeicht

| Mutant | Datei | Ergebnis |
|---|---|---|
| Ablauf der Trefferliste **+1 px** (`62.5625rem`) | `treffer-spalten.ts` | **rot**, 1 von 4 |
| Ablauf der Trefferliste **−1 px** (`62.4375rem`) | `treffer-spalten.ts` | **rot**, 1 von 4 |
| Rolle **+1 px** (`37rem`) | `benutzer/spalten.ts` | **rot**, 1 von 4 |
| Rolle **−1 px** (`36.875rem`) | `benutzer/spalten.ts` | **rot**, 1 von 4 |
| Richtung **+1 px** (`46.25rem`) | `katalog/spalten.ts` | **rot**, 1 von 4 |
| Richtung **−1 px** (`46.125rem`) | `katalog/spalten.ts` | **rot**, 1 von 4 |
| Wechsel der Katalog-Bauform **+1 px** (`33.6875rem`) | `katalog-tabelle.tsx` | **rot**, 1 von 4 |
| Wechsel der Katalog-Bauform **−1 px** (`33.5625rem`) | `katalog-tabelle.tsx` | **rot**, 1 von 4 |
| `td` der Richtung ohne Sichtbarkeitsklasse | `katalog-zeile.tsx` | **rot**, 1 von 4 |
| Fensterschwelle `md:w-[11rem]` an der Kopfzelle „Benutzer" | `benutzer-tabelle.tsx` | **rot**, 1 von 4 |
| **Original** | — | **grün, 4 von 4** |

**Alle sechs Dateien danach byte-gleich** (SHA-256 vor und nach dem Lauf verglichen).

#### Nachgetragen am 16.09.2026 — neun Mutanten für E‑148 und den Baustein

Dieselbe Eichung in beide Richtungen: Das Original läuft **vor** und **nach** der Reihe grün, jede
Datei ist danach über SHA-256 gegen den Ausgangsstand geprüft.

| Mutant | Datei | Ergebnis |
|---|---|---|
| Projekt-Schwelle **+1 px** (`58.3125rem`) | `nachrichten/spalten.ts` | **rot** |
| Projekt-Schwelle **−1 px** (`58.1875rem`) | `nachrichten/spalten.ts` | **rot** |
| Projekt-Breite **−1 px** (`17.8125rem`) | `nachrichten-tabelle.tsx` | **rot** |
| Zeitpunkt-Breite **+1 px** (`11.75rem`) | `nachrichten-tabelle.tsx` | **rot** |
| `td` des Projekts ohne Sichtbarkeitsklasse | `nachrichten-tabelle.tsx` | **rot** |
| Fensterschwelle `md:w-[11rem]` an der Kopfzelle „Status" | `nachrichten-tabelle.tsx` | **rot** |
| Riegel gegen die Wahl desselben Werts entfernt | `components/auswahl-feld.tsx` | **rot** |
| gesperrte Zeile am Klick wählbar | `components/auswahl-feld.tsx` | **rot** |
| `data-wert` an der Zeile entfernt | `components/auswahl-feld.tsx` | **rot** |
| **Original**, davor und danach | — | **grün** |

**Neun von neun gefallen**, drei Dateien danach byte-gleich.

> ⚠️ **Ein Lauf hat gelogen und ist berichtigt.** Im ersten Durchgang meldete das Skript *„9 von 9
> gefallen"* — der fünfte Mutant war aber gar nicht gesetzt worden: Seine Suchzeichenkette passte
> nach Prettier nicht mehr (`FUNDSTELLE 0x`), und „übersprungen" zählte in der Summe wie „gefallen".
> **Der Zähler eines Mutantenläufers muss zwischen *nicht gesetzt* und *gefallen* unterscheiden**;
> die Zeile oben ist der Lauf **nach** der Berichtigung.

#### Nachgetragen am 16.09.2026, zweite Meldung — sieben Mutanten für E‑149

Dieselbe Eichung in beide Richtungen — und diesmal mit einem Läufer, der **„übersprungen" nicht mehr
als „gefallen" zählt**: Die Berichtigung aus dem Kasten darüber steht jetzt im Läufer selbst und nicht
nur in dieser Datei.

| Mutant | Datei | Ergebnis |
|---|---|---|
| Mandanten **ohne Breitenklasse** — die freie Spalte kehrt zurück | `benutzer-tabelle.tsx` | **rot** |
| `frei: "mandanten"` wieder in der Spaltenwahl | `benutzer/spalten.ts` | **rot** |
| Mandanten-Breite **+1 px** (`6.0625rem`) | `benutzer-tabelle.tsx` | **rot** |
| Mandanten-Breite **−1 px** (`5.9375rem`) | `benutzer-tabelle.tsx` | **rot** |
| Benutzer-Breite **+1 px** (`5.125rem`) | `benutzer-tabelle.tsx` | **rot** |
| Mindestbreite der Mandanten im Modell auf 120 px | `benutzer/spalten.ts` | **rot** |
| `td` der Mandanten ohne Sichtbarkeitsklasse | `benutzer-zeile.tsx` | **rot** |
| **Original**, davor und danach | — | **grün** |

**Sieben von sieben gefallen, null lebendig, null übersprungen**; die drei Dateien danach byte-gleich
(SHA-256 vor und nach dem Lauf). **Die ersten beiden sind die eigentliche Gegenprobe zu E‑149:** Beide
stellen den alten Zustand wieder her — einmal über die fehlende Klasse an der Kopfzelle, einmal über
das Modell —, und beide fallen. Der sechste sichert, dass die Schwelle weiter aus der Mindestbreite
kommt und nicht umgekehrt.

#### Nachgetragen am 22.09.2026 — fünf Mutanten für E‑231 (die Trefferliste ohne „Kette")

Dieselbe Eichung in beide Richtungen, derselbe Läufer-Aufbau (übersprungen zählt nicht als
gefallen): Das Original läuft **vor** und **nach** der Reihe grün, beide Dateien sind danach über
SHA-256 gegen den Ausgangsstand geprüft. Gelaufen sind `tests/spaltenwahl.test.tsx` und
`tests/suche-marken.test.tsx`.

| Mutant | Datei | Ergebnis |
|---|---|---|
| Ablauf mit „Treffer" **+1 px** (`57.9375rem`) | `treffer-spalten.ts` | **rot** |
| Ablauf mit „Treffer" **−1 px** (`57.8125rem`) | `treffer-spalten.ts` | **rot** |
| Ablauf ohne „Treffer" **+1 px** (`40.4375rem`) | `treffer-spalten.ts` | **rot** |
| Ablauf ohne „Treffer" **−1 px** (`40.3125rem`) | `treffer-spalten.ts` | **rot** |
| Kopfzelle „Kette" kurz zurück (`th` ohne `td`) | `treffer-tabelle.tsx` | **rot** — der Abwesenheitsfall in `suche-marken`, und `spaltenwahl` findet keine Zeile mit passender Zellenzahl |
| **Original**, davor und danach | — | **grün** |

**Fünf von fünf gefallen, null lebendig, null übersprungen**; beide Dateien danach byte-gleich.
**Der ganze Lauf am 22.09.2026:** `vitest run` über alle 48 Dateien, **1.285 Fälle, 0
fehlgeschlagen**; gerenderte Fälle **201 in vierundzwanzig Dateien** — aus dem Lauf gezählt
(`vitest run --reporter=json`), dieselbe Zahl wie vor dem Umbau: Der Fall zum Kettenhinweis ist
ersetzt, nicht gestrichen.

**`tests/rollen-auswahl.test.tsx`** — vier Fälle, drei Mutanten, beschrieben in
[`benutzerverwaltung-frontend.md`](benutzerverwaltung-frontend.md) §18.

**Der ganze Lauf:** `vitest run` über alle 41 Dateien, **1.068 Fälle, 0 fehlgeschlagen**; gerenderte Fälle
**133 in neunzehn Dateien** (vorher 125 in siebzehn), fortgeschrieben im Kopf von `vitest.config.mts`.
**Stand 16.09.2026:** **1.072 Fälle** in denselben 41 Dateien, gerenderte **134 in neunzehn** — aus dem
Lauf gezählt (`vitest run --reporter=json`), nicht weitergezählt (Regel L10). **Nach der zweiten
Meldung desselben Tages (E‑149): 1.073 Fälle**, gerenderte **135 in neunzehn**, wieder aus dem Lauf.
`console.error` lässt den Lauf weiterhin fehlschlagen ([`frontend-grundlagen.md`](frontend-grundlagen.md) §9).

---

## 10. Regelbezug

| Regel | Wie sie hier gilt |
|---|---|
| **Q4** | Jede Mindestbreite gemessen, nicht geschätzt; was nicht gemessen ist — Firefox, andere Dichtestufen, die Rolle `MANDANT`, die Ursache der hellen Liste in Chrome —, steht als offen da |
| **L7** | zwei Mandanten in Messung und Gegenprobe, `NEXANS` und `VOTG` |
| **L10** | Belegvermerke in §4 und §7; die Zahlen dieser Datei kommen aus `auswertung-bedarf.mjs` und `vergleich-m177.mjs` und sind nicht abgetippt |
| **T1** | keine Wanduhrzeit als Kriterium; Ruhe- und Einschwingzeiten sind Wartezeiten des Werkzeugs |
| **G1** | keine Kennungen, keine Belegwerte, keine Prozess- oder Partnernamen in dieser Datei; `scripts/sichtprobe-schmal/ergebnis/` steht in `.gitignore` |
| **S1** | kein Schreibzugriff auf `GlassfishDB`. Mandantenwechsel und Sprach-Cookie sind Sitzungs- und Nutzerzustand und am Ende zurückgesetzt |
| **M1–M4, L1–L3, Z1** | nicht berührt — kein Endpunkt, keine Abfrage, keine Uhr |

---

## 11. Offene Punkte

| Punkt | Stand |
|---|---|
| **114** | ~~offen~~ **geschlossen am 16.09.2026** (E‑148) — die Nachrichtenliste ist gebaut (§5.4, §6 Nachtrag). **Nachtrag, zweite Meldung desselben Tages (E‑162):** Die Schwelle des Projekts liegt jetzt bei 1.167 px; der Punkt bleibt geschlossen (§5.4, Korrektur) |
| **181** | **neu** — die 591 / 647 px Scrollbreite des Tabellenkastens: die `sr-only`-Spannen der Ablaufzelle, absolut positioniert am Kasten aus `components/ui/table.tsx` und vom `overflow: hidden` der gekürzten Spanne nicht beschnitten (§6). Sie hängt **nicht** an der Spaltenbreite und ist deshalb aus 114 herausgelöst. **Was ihm fehlt:** die Entscheidung, ob die Spanne einen eigenen Positionierungsvorfahren bekommt oder der Text anders angeboten wird — beides berührt `components/ui/table.tsx` oder jede Zelle |
| **173**, **174**, **175** | **geschlossen**, mit der Zahl in [`messungen-sichtprobe-schmal.md`](messungen-sichtprobe-schmal.md) §5 und §7 hier |
| **176**, **177** | **geschlossen**, ebenda |
| **178** | **offen** — nicht Teil dieser Runde. **Was ihm fehlt:** eine Messung mit `pointer: coarse` an einem **echten** Gerät; die Emulation setzt das Merkmal über `setEmulatedMedia` nicht, und der Rahmen im Chrome der Erweiterung kann es gar nicht (M176, Abweichung 3) |
| **180** | **neu** — native Formularelemente ([`benutzerverwaltung-frontend.md`](benutzerverwaltung-frontend.md) §18) |
| **183** | **neu am 16.09.2026** (E‑162, [`nachrichtenliste.md`](nachrichtenliste.md) §8.1) — **die Trefferliste trägt noch die Breiten aus M177.** Ihre Statusspalte ist `w-[9.6875rem]`. Mit derselben Plakette kürzt „Zusammengeführt" dort bei `xs` gerechnet um 0,063 px — *gerechnet, nicht gemessen*, eingesetzt ist die Probe nur in der Nachrichtenliste. Ihr Ablauf rechnet mit 304 px Mindestbreite, der breiteste Ablaufname misst 408 px (M180). Damit sind Liste und Trefferliste nicht mehr Spalte für Spalte gleich breit (§5.4, Korrektur). **Was ihm fehlt:** ein Auftrag für die Trefferliste. Die Zahlen aus M180 gelten für dieselben Zellen (§3, Regel 4), die Schwellen `@min-[62.5rem]` und `@min-[45rem]` wären neu herzuleiten. **Vermerk 22.09.2026 (E‑231):** Die Spalte „Kette" ist entfallen, die Schwellen heißen seitdem `@min-[57.875rem]` und `@min-[40.375rem]` (§5.1, Korrektur) — gesunken allein um die 74 px der Kette, die Breiten aus M180 sind weiterhin nicht übernommen; **der Punkt bleibt offen** |

### Die Nummernvergabe — belegt per `grep`, jede Fundstelle gelesen

| Nummer | Suche | Ergebnis |
|---|---|---|
| **E‑147** | Python über `docs/*.md` und die Markdown-Dateien der Wurzel, jede Schreibweise `E<Strich><Zahl>` (U+2011, U+2010, `-`, U+2013), auf `HEAD`, `main`, `feat/suchfeld-untermenues` und `test/indexbestand-e37` | höchste **E‑146** ([`process-view.md`](process-view.md) §48); **E‑780** ist der bekannte Falschtreffer aus der Messdatei der Property-Suche. Kein Stand trägt E‑147 bis E‑159 |
| **M177** | `\bM17[7-9]\b`, `\bM18\d\b` auf denselben vier Ständen | höchste **M176**. `M178` steht nur in *„`M176` bis `M178` … kein Treffer"* ([`messungen-sichtprobe-schmal.md`](messungen-sichtprobe-schmal.md) §3), `M179` nur im Fließtext von [`messungen-property-suche.md`](messungen-property-suche.md) — beides gelesen, keine Vergabe |
| **180** | `\*\*18[0-9]\*\*` und `Punkt 18[0-9]` auf denselben vier Ständen | höchster vergebener Punkt **179** ([`process-view.md`](process-view.md) §48). Die Treffer **185** bis **188** sind Zahlen in Tabellen — `README.md` (99. Perzentil 186), `messungen-schritt7.md` (ein Tabellenwert 186), `messungen-schritt8.md` (`Content-Length` 185), `messungen-schritt9.md` (Tabellenwerte: 188 für `IBIS`, „Regel B 187") — gelesen, keine Punkte. **180** kommt nicht vor |
| **E‑148** *(16.09.2026)* | `E[^0-9]{1,3}14[6-9]` und `…15[0-9]` auf `HEAD`, `main`, `feat/suchfeld-untermenues`, `test/indexbestand-e37` und `feat/prozessbaum-projektgliederung` | **E‑146** gibt 8 Treffer (die Eichung), **E‑147 bis E‑150 null** auf jedem Stand. E‑147 steht nur im Arbeitsbaum dieser Runde und ist damit dieselbe Vergabe wie am Vortag |
| **181** *(16.09.2026)* | `Punkt 181` und `\*\*181\*\*` auf denselben fünf Ständen | **null** Treffer; im Arbeitsbaum nur die zwei Dateien dieser Runde |
| **E‑149** *(16.09.2026, zweite Meldung)* | `E[^0-9]{1,3}(146\|148\|149\|150)` auf denselben fünf Ständen, dazu der **Arbeitsbaum** | **E‑146** gibt auf drei Ständen 7 Treffer in `docs/` (die Eichung), **E‑148, E‑149 und E‑150 null**; im Arbeitsbaum findet dieselbe Suche E‑147 in neun Dateien und E‑149 in keiner. Auch `Punkt 182` ist frei — gebraucht wird er nicht |
| **E‑231**, **E‑232** *(22.09.2026, beide auf einmal vergeben)* | `git grep -E 'E[^0-9]{1,3}(2[3-9][0-9]\|[3-9][0-9][0-9])'` über den **ganzen** Baum (nicht nur `docs/`, E‑Nummern stehen auch in Code-Kommentaren) auf `main`, allen **41** lokalen Branches und dem Arbeitsbaum; `[^0-9]{1,3}` deckt U+2011, U+2010, `-` und U+2013 byteweise ab | **E‑230** (die Eichung) auf `main`, `fix/nutzertexte-ohne-altsystem` und im Arbeitsbaum; **E‑231 bis E‑249 auf keinem Stand.** Die höheren Treffer sind gelesen und keine Vergaben: 250 und 290 sind EDIFACT-Beispielstrings (`ERFUNDEN+250101`) und ein Lockfile-Hash, 400 bis 955 HTTP-Codes und Tabellenwerte, **E‑780** der bekannte Falschtreffer. E‑231 gehört dieser Korrektur, E‑232 dem Suchfeld in der Kopfzeile ([`bam-suche.md`](bam-suche.md) §11.1, Vermerk) |

> ⚠️ **Die erste Suche war kaputt, und nur die Eichung hat es gezeigt.** `E.146` fand auf **allen**
> Ständen null — der Punkt steht für ein **Byte**, und der Strich in `E‑146` ist U+2011 mit drei
> Bytes in UTF-8. Ein „nirgends vergeben" aus diesem Lauf hätte für jede Nummer gegolten, auch für
> eine längst vergebene. Deshalb steht in jeder Zeile oben eine **bekannt vergebene** Nummer als
> Gegenprobe (Regel Q4, und dieselbe Falle wie am 15.09.2026 bei der Zeichenklasse `[‑-]`).

---

## 12. Die Dateien

```
frontend/src/
├─ lib/spaltenwahl.ts                              NEU — die Rechnung: Grundmenge, Schwelle, sichtbare Spalten
├─ components/auswahl-feld.tsx                     NEU (16.09.) — die Auswahlliste der Anwendung, gemeinsamer Baustein
├─ features/nachrichten/
│  ├─ treffer-spalten.ts                           NEU — Mindestbreiten, Stufen, Sichtbarkeit der Trefferliste
│  ├─ spalten.ts                                   NEU (16.09.) — dasselbe für die Nachrichtenliste (E‑148)
│  ├─ components/nachrichten-tabelle.tsx           (16.09.) Hülle als Container, Breiten, Projekt nach Schwelle
│  └─ components/treffer-tabelle.tsx               Hülle als Container, Breiten, Ablauf nach Schwelle
├─ features/benutzer/
│  ├─ spalten.ts                                   NEU — dasselbe für die Benutzertabelle; (16.09., E‑149) ohne freie Spalte
│  └─ components/benutzer-tabelle.tsx, benutzer-zeile.tsx   (16.09.) die Mandanten tragen ihre 6 rem
├─ features/katalog/
│  ├─ spalten.ts                                   NEU — dasselbe für den Katalog, dazu die Bauform
│  └─ components/katalog-tabelle.tsx, katalog-zeile.tsx
├─ features/nachrichten/components/nachricht-detail.tsx   Punkt 176 — Projekt und Prozess mit `title`
└─ components/kopfzeile.tsx                        Punkt 177 — der Produktname mit `title`
frontend/tests/spaltenwahl.test.tsx                NEU — fünf Fälle (seit 16.09. mit der Nachrichtenliste)
scripts/sichtprobe-schmal/
├─ spaltenbedarf.js                                NEU — der Messkern für Mindestbreiten
├─ etiketten.mjs                                   NEU — die Beschriftungsmengen aus den Sprachdateien
├─ auswertung-bedarf.mjs                           NEU — die Regeln aus §3, die Zahlen für den Code
├─ vergleich-m177.mjs                              NEU — vorher gegen nachher
├─ rahmen.js                                       `bedarf`, `kurzBedarf`, `sprache`
└─ empfaenger.mjs                                  Herkunft `:3001`, liefert die Messskripte aus (`GET /skript/…`)
```

Die Rollenauswahl (`rollen-auswahl.tsx`, `rollen-auswahl.test.tsx`) steht in
[`benutzerverwaltung-frontend.md`](benutzerverwaltung-frontend.md) §18.
