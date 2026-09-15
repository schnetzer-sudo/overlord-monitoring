# Messungen — Sichtprobe am schmalen Fenster, Teil 1 (Erhebung)

Erhoben am **15.09.2026** gegen die **lokal laufende Anwendung** (Produktionsbau des Frontends,
Backend im Profil `dev` gegen die Testkopie). Auftrag: „Sichtprobe am schmalen Fenster, Teil 1
(Erhebung)", Stand 15.09.2026. Messung **M176** (Nummernvergabe in §3).

**Diese Runde baut nichts und ändert keine Darstellung.** Keine Spaltenbreite, kein Umbruchpunkt,
kein Farbwert, keine Datei unter `components/ui`. Was sie findet, ist Befund und offener Punkt;
der Umbau ist Teil 2 und bekommt einen eigenen Auftrag. Offener Punkt **114** — die
Spaltenbreiten der Nachrichtenliste — wird hier weder entschieden noch umgangen; er wird
**bestätigt und erweitert** (§5).

**Gemessen im DOM, nicht am Bild:** acht Routen und zwei Zustände, fünf Breiten, zwei Mandanten
(`NEXANS` und `VOTG`), Rolle `ADMIN`. Drei Bildschirmfotos sind entstanden, je eines für einen
Befund, und sie sind nicht eingecheckt (§2).

> ### Der Ertrag in einem Absatz
>
> **Die Zusagen des Rahmens halten:** kein waagerechter Überlauf am Dokument bei einer einzigen
> der 100 Messungen (`scrollWidth` = `innerWidth` bei 360, 390, 430, 744 und 768 px), das Dokument
> scrollt nie, `main` ist der einzige scrollende Bereich (mit der zugesagten Ausnahme der klebenden
> Spalte ab `md`), die Kopfzeile bricht unter 768 px in genau drei Zeilen um, der Mandantencode
> bleibt sichtbar, das Suchfeld ist eine eigene Zeile. **Was nicht hält, sind die Tabellen — und
> zwar alle vier auf dieselbe Weise:** `table-fixed` mit festen Spalten und einer freien; sobald die
> festen den Kasten füllen, fällt die freie auf **0 px**, ihre Beschriftung wird trotzdem
> gezeichnet und steht über der Nachbarspalte. In der Nachrichtenliste trifft es die Ablaufspalte
> bei 360 px **und** bei 768 px (Punkt 114, jetzt auf der eigenen Route), in der Trefferliste
> und in der Benutzerverwaltung bei 768 px, und im Prozess-Katalog bei 768 px die Partnerspalte —
> dort mit dem sichtbarsten Symptom der Runde: Der Partnername bricht **buchstabenweise** um, und
> jede Zeile wird 79 bis 739 px hoch.

---

## 1. ⚠️ Gemeldete Abweichungen vom Auftrag — keine davon ist still

### Abweichung 1 — die Bauform: Messrahmen im angemeldeten Chrome statt kopflosem Chrome mit Formularanmeldung

Der Auftrag verlangt ein kopfloses Chrome über das DevTools-Protokoll, die Anmeldung echt über
das Formular mit Zugangsdaten aus Umgebungsvariablen. **Das ist gebaut und geht** — bis zur
Anmeldung: In der Umgebung stehen nur `OVERLORD_BOOTSTRAP_ADMIN_USER`/`_PASSWORD`, und das ist
das **Einmalpasswort** des Bootstrap-Kontos (`admin/BootstrapAdminRunner`: „nach der ersten
Anmeldung ist ihr Inhalt wertlos"). Der eine Versuch über das Formular (09:51:46 Ortszeit) kam
beim Backend an und wurde abgelehnt:

```
POST /api/auth/login -> 401 (anmeldung-abgelehnt): Falsches Passwort
```

Es blieb bei dem einen Versuch — `security/AnmeldeService` sperrt nach **fünf** Fehlversuchen für
**15 Minuten**. Kein Wegwerfkonto, keine Sitzungskennung aus `SPRING_SESSION`, kein
Platzhalter-Cookie, kein Passwortwechsel an einem fremden Konto.

**Der Nutzer hat sich daraufhin selbst im Chrome der Erweiterung angemeldet** (Rolle `ADMIN`) und
den Lauf dort freigegeben. Der angemeldete Teil ist deshalb mit der **zweiten Bauform** gefahren,
die [`process-view.md`](process-view.md) §23 („Der Messrahmen ist ein gleichherkunftiger `<iframe>`
und nicht das Browserfenster") seit dem 02.09.2026 beschreibt und die E‑114 (09.09.2026) getragen
hat: ein gleichherkunftiger `<iframe>` mit exakter Größe in der Oberseite, sein Dokument lesbar, die
Sitzung die echte. **Was gegenüber dem Auftrag fehlt, und was es ersetzt:**

| Auftrag | Rahmen | Folge |
|---|---|---|
| `Emulation.setDeviceMetricsOverride`, `mobile: true`, `deviceScaleFactor: 2` | `<iframe>` mit `width`/`height` in CSS-Pixeln, DPR 1 (das Fenster des Nutzers) | Breiten exakt, Medienabfragen gehen mit; **kein** `mobile`-Sichtfenster. Die Rinne des klassischen Scrollbalkens, die es am Handy nicht gibt, ist per `scrollbar-width: none` im Rahmen entfernt — **die einzige Änderung am gemessenen Dokument** |
| Berührungsemulation als obere Schranke (Abweichung 3) | nicht möglich | Messgröße 6 trägt im angemeldeten Teil nur die Hauptzahl (`pointer: fine`); die Wirkung des Tokens ist auf `/anmeldung` per CDP belegt (Abweichung 3) und wird in §5 als Rechnung herangezogen |
| Bildschirmfoto per `Page.captureScreenshot`, Faktor 1 | `zoom` der Erweiterung auf den Rahmen, Faktor 0,5 | drei Bilder, je Befund eines; **ein Bild ist keine Messung** |
| Anmeldung durch das Skript, Mandantenwahl über `/mandantenauswahl` | Anmeldung durch den Nutzer, Mandantenwahl über `/mandantenauswahl` **im Rahmen** (Klick auf die Schaltfläche mit dem Code) | dieselbe Sitzung, derselbe Weg |
| Konsole und Netzwerkantworten je Route | nicht erhoben (kein Protokollzugang aus der Seite) | Punkt 172 stammt aus dem CDP-Lauf auf `/anmeldung` |

**Der Rahmen ist gegen die CDP-Bauform geeicht, und zwar an einer schon dokumentierten Zahl:**
Prozessansicht mit gewähltem Prozess bei 768 px, Kasten **294**, Tabelle **640**, Ablauf **0**,
Projekt **288** — Ziffer für Ziffer die Zeile für 768 px aus M126
([`process-view.md`](process-view.md) §21, kopfloses Chrome, 02.09.2026). Das Werkzeug ist
gewechselt, die Zahlen nicht.

**Der Rahmen lief im Dunkelmodus** (`data-thema="dunkel"`, die Einstellung des Nutzers) und in
Dichtestufe `m`. Kein Layoutmaß dieser Runde hängt an einer Farbe; die Dichte ist die Vorgabe.

**Gefahren, nicht gemessen:** Ein erster Durchgang des `VOTG`-Dashboards lief, während der Tab
verdeckt war (`document.hidden`). Chrome drosselt dort die Zeitgeber, und Recharts misst nicht neu.
Der Durchgang ist verworfen und mit sichtbarem Tab wiederholt; nur die Wiederholung zählt.

### Abweichung 2 — die Rolle `MANDANT` ist nicht gefahren

Der Auftrag verlangt beide Rollen. Angemeldet war `ADMIN` (alle acht Routen); ein Konto der Rolle
`MANDANT` stand nicht zur Verfügung. **Was damit fehlt, ist klein und benannt:** die Kopfzeile mit
dem Mandanten als **Anzeige** statt als Bedienelement ([`visuelles-konzept.md`](visuelles-konzept.md)
§5 — die Anzeige ist ein `span` mit derselben Grundform wie der Verweis, die Breite des Codes
dieselbe), und die Navigationsschublade ohne den Eintrag *Administration*. Alles andere ist rollenfrei.
Das CDP-Skript kann die Rolle fahren, sobald `SICHTPROBE_MANDANT_USER`/`_PASSWORD` gesetzt sind (§7).

### Abweichung 3 — `Emulation.setEmulatedMedia` kennt `pointer` nicht; die obere Schranke kommt aus der Berührungsemulation *(CDP-Teil)*

Auf `/anmeldung` per CDP gemessen: `mobile: true` allein setzt `pointer: coarse` **nicht**
(`matchMedia` fünfmal `false`), `setEmulatedMedia` mit dem Merkmal `pointer` wird still ignoriert,
`Emulation.setTouchEmulationEnabled` (`maxTouchPoints: 1`) setzt es — und dann springt
`--dichte-bedienelement` von `2rem` auf `max(2.75rem, 44px)`, der erste Knopf von 32 auf 44 px;
nach dem Abschalten wieder `false`. Der Vermerk des Auftrags ist damit **bestätigt und beziffert**;
die Hauptzahl bleibt `pointer: fine`.

### Abweichung 4 — zwei *Zustände* neben den acht Routen, keine neunte Route

**Nachrichtenliste mit geöffnetem Panel** (`?nachricht=…`) und **Prozessansicht mit gewähltem
Prozess** (`?prozess=…`). Lagen derselben Route: Unter `md` ersetzt das Panel die Liste und die
Liste den Baum ([`process-view.md`](process-view.md) §18); Punkt 114 lebt in der zweiten Lage.
Beide sind gefahren.

### Abweichung 5 — `pnpm exec next build` statt `pnpm build`

`pnpm build` fährt zuerst `pnpm check`. Gebaut ist mit `next build` allein; der Zweig stand sauber
auf `825dfd8`, wo `pnpm check` schon gegolten hat. Derselbe Produktionsbau (Next.js 16.2.11).

### Abweichung 6 — der Messkern in der Datei ist zwei Griffe weiter als der, der gelaufen ist

`scripts/sichtprobe-schmal/messung.js` trägt seit dem Lauf zwei Änderungen: die Zeilenerkennung
der Kopfzeile über die **Höhenüberlappung** statt über die Oberkante (der alte Kern zählte in der
einzeiligen Kopfzeile ab `md` zwei Zeilen, weil der Produktname vier Pixel tiefer sitzt als die
Schaltflächen), und ein Zwischenspeicher je Elternelement für die Scrollhülle (der Katalog mit
15.672 Elementen brauchte sonst über eine Minute je Breite). **Geeicht:** der neue Kern gegen den
alten auf `VOTG`, Dashboard bei 360 und 768 px und Nachrichtenliste bei 360 px, 14 Felder je
Messung verglichen — **eine Abweichung**, die erwartete: Kopfzeile bei 768 px **1 statt 2 Zeilen**.
Alle Zahlen unter 768 px sind unberührt; die Kopfzeile ab `md` ist in diesem Dokument als **eine**
Zeile geführt.

---

## 2. Bauform

| | |
|---|---|
| **Browser (CDP-Teil, `/anmeldung`)** | das installierte **Chrome 152.0.7977.84**, `--headless=new`, DevTools-Protokoll 1.3, über Nodes globales `WebSocket`; `Emulation.setDeviceMetricsOverride` mit `mobile: true`, `deviceScaleFactor: 2`. Kein Playwright, keine Abhängigkeit |
| **Browser (angemeldeter Teil)** | dasselbe Chrome 152 als Chrome der Erweiterung, Fenster 1920 × 889, DPR 1, Dunkelmodus, Dichte `m`; Messrahmen `scripts/sichtprobe-schmal/rahmen.js` + Messkern `messung.js`, Ergebnisse per `fetch` an `empfaenger.mjs` (127.0.0.1:3999), weil das `javascript_tool` seine Ausgabe nach rund tausend Zeichen abschneidet |
| **Node** | v24.18.0 |
| **Ziel** | `http://localhost:3000` — **Produktionsbau** (`next build` + `next start`, Next.js 16.2.11), Backend `spring-boot:run` im Profil `dev` auf `:8080`. **Nicht `next dev`, nicht die Produktion** |
| **Datenbank** | die Testkopie (MariaDB 10.6.22, jOOQ 3.21.5) |
| **Zeitanker** | `2025-12-30T04:09:47`, Versatz −259 Tage (Startprotokoll: *„Dev-Clock aktiv"*). Das Standardfenster trägt bei beiden Mandanten Daten: je **50 Zeilen** in der 24‑h-Liste |
| **Breiten** | **360×740, 390×844, 430×932, 744×1133, 768×1024** — 744 als Kontrollpunkt (§4.3), 768 als `md` |
| **Ruhebedingung** | vor jeder Messung: kein `[data-slot="skeleton"]`, nichts `aria-busy`, `readyState` `complete`, Zahl der Ressourceneinträge und der DOM-Knoten dreimal in Folge unverändert (Abstand 500 ms); nach jedem Breitenwechsel 1.500 ms Einschwingzeit und `document.fonts.ready` |
| **Mandanten** | `NEXANS` (trägt das Aufkommen) **und** `VOTG` (Regel L7). Gewechselt über `/mandantenauswahl`, angeboten waren zehn |
| **Rollen** | `ADMIN`. `MANDANT` nicht gefahren (§1, Abweichung 2) |
| **Prüfdaten** | je Mandant die **erste Zeile** der 24‑h-Liste (Klick, Kennung aus der URL): bei `NEXANS` eine Nachricht im Status `ERROR_TIMEOUT` vom `2025-12-30T03:09:47Z` mit sieben Belegwertgruppen — ihr erster Wert (Typ 9014) ist der Suchbegriff, **50 Treffer**; bei `VOTG` eine Nachricht im Status `FINISHED` vom `2025-12-30T03:02:04Z` — **keine der 50 Zeilen des Tagesfensters und keine der ersten 50 des 30‑Tage-Fensters trägt einen Belegwert**, deshalb Präfixsuche `2000:0` mit **einem** Treffer. Der Prozess ist jeweils der der ersten Zeile. Kennungen und Werte stehen nicht in dieser Datei (Regel G1) |
| **Was der Kern misst** | die sieben Messgrößen des Auftrags, je Breite ein Ausdruck: Dokumentbreite; je Tabelle Kopfzellen als Kasten **und** als Textkasten (`Range.getBoundingClientRect`), paarweise geschnitten; 0-px-Spalten mit Text; gekürzte Zellen mit und ohne `title`; Scrollbereiche (angelegt und scrollend); Berührungsflächen als `offsetWidth × offsetHeight`; Kopfzeile in Zeilen gruppiert |
| **Ergebnisse** | `scripts/sichtprobe-schmal/ergebnis/rahmen/<Mandant>/<Route>.json` (18 Dateien, dazu zwei `-neu` für die Eichung), `…/OFFEN-messung.json` für `/anmeldung`, `zusammenfassung.md` aus `auswertung.mjs`, drei Bilder `bild-VOTG-*.png`. **Nicht eingecheckt** — sie tragen Prozessnamen und Belegwerte der Testkopie |
| **Zeit** | `NEXANS` 10:37 bis 10:45, `VOTG` 10:56 bis 11:00 Ortszeit; `/anmeldung` per CDP zuletzt 11:15 (Lauf `2026-09-15T09-15-18`, Zahlen unverändert gegenüber 09:58) |

---

## 3. Nummernvergabe — belegt per `grep`, jede Fundstelle gelesen

### Messungen

```
grep -rnoE '\bM17[6-9]\b|\bM18[0-9]\b|\bM19[0-9]\b' docs/ scripts/ *.md
```

| Ergebnis | |
|---|---|
| `M176` bis `M178`, `M18x`, `M19x` | **kein Treffer** |
| `M175` | **vergeben** — [`dienste.md`](dienste.md) §11; dazu Verweise in `dashboard.md`, `IMPLEMENTIERUNGSPLAN_MVP.md`, `README.md`. Die höchste echte |
| `M179` | **Fließtext, keine Vergabe** — neun Treffer in zwei Dateien, jeder gelesen (`messungen-property-suche.md` 77, 1339, 2251; `process-view.md` 4028, 4335): Aussagen **über** Nummern |
| `messungen-schritt10.md` Zeile 21 | *„Der Bereich M86–M99 ist frei"* — trifft das Muster nicht (ganze Nummern ab 176, keine Bereiche); angesehen |

**Diese Runde ist M176.**

### Offene Punkte

```
grep -rnoE '^\| \*\*1[2-9][0-9]\*\*|^\| ~~\*\*1[2-9][0-9]\*\*|Punkt \*\*1[2-9][0-9]\*\*|Punkt 1[2-9][0-9]\b' docs/*.md
grep -rnE  '\*\*17[2-9]\*\*|Punkt 17[2-9]' docs/*.md
```

**171** ist die höchste vergebene ([`dashboard-frontend.md`](dashboard-frontend.md) §9); **168** und
**169** stehen in `dienste.md` **und** `dashboard-frontend.md` — gelesen: dieselben zwei Punkte,
in beiden Dateien geführt; **172 und aufwärts** ohne Treffer. **Diese Runde vergibt 172 bis 178**
(§5).

---

## 4. Ergebnisse je Route

Die Layoutzahlen sind bei `NEXANS` und `VOTG` **identisch** — sie hängen an Spaltenbreiten und
Umbruchpunkten, nicht an den Daten. Wo eine Zahl vom Mandanten abhängt (Zeilen, Ziele, Höhe von
`main`), stehen beide.

### 4.1 Anmeldung (`/anmeldung`) — CDP, ohne Sitzung

| Breite | `scrollWidth` | Überlauf | `scrollHeight` / `clientHeight` | Scroller | Tabellen | Ziele < 44 px (fine / coarse) |
|---:|---:|---:|---|---|---:|---|
| 360 | 360 | **0** | 740 / 740 | 0 / 0 | 0 | 4 von 5 / 4 von 5 |
| 390 | 390 | **0** | 844 / 844 | 0 / 0 | 0 | 4 / 4 |
| 430 | 430 | **0** | 932 / 932 | 0 / 0 | 0 | 4 / 4 |
| 744 | 744 | **0** | 1133 / 1133 | 0 / 0 | 0 | 4 / 4 |
| 768 | 768 | **0** | 1024 / 1024 | 0 / 0 | 0 | 4 / 4 |

Die vier: Sprachschaltflächen „Deutsch" und „English" **40 × 32** (coarse: 40 × 44 — es fehlt die
Breite), Eingabefelder **304 × 40** bei 360 px (334 bei 390, 352 ab 430; coarse unverändert —
`--dichte-feld` hat keinen Rückfall). *Anmelden* 44 px. Konsole: je Aufruf ein `404 /favicon.ico`.

### 4.2 Dashboard (`/`)

| Breite | Überlauf | Kopfzeile | Mandant | Suchfeld | Scroller aktiv / angelegt | `main` (N / V) | Ziele < 44 (N / V) | gekürzt außerhalb Tabellen |
|---:|---:|---|---|---|---|---|---|---|
| 360 | 0 | 3 Zeilen, 131 px | sichtbar | eigene Zeile | 1 / 1 (`main`) | 2009 / 1776 von 609 | 20 von 22 / 17 von 19 | 1 (N, mit `title`) |
| 390 | 0 | 3 Zeilen, 131 px | sichtbar | eigene Zeile | 1 / 1 | 1937 / 1686 von 713 | 20 / 22 · 17 / 19 | 1 (N, mit `title`) |
| 430 | 0 | 3 Zeilen, 131 px | sichtbar | eigene Zeile | 1 / 1 | 1903 / 1652 von 801 | 20 / 22 · 17 / 19 | 0 |
| 744 | 0 | 3 Zeilen, 131 px | sichtbar | eigene Zeile | 1 / 1 | 1681 / 1456 von 1002 | 20 / 22 · 17 / 19 | 0 |
| 768 | 0 | 1 Zeile, 51 px, Navspalte | sichtbar | in der Zeile | 1 / 2 (`aside` scrollt nicht) | 1721 / 1496 von 973 | 23 / 25 · 20 / 22 | 1: **Produktname ohne `title`** (`main` 560 px) |

Keine Tabelle, kein Überlauf. Die Ziele unter 44 px bei 360 px (`NEXANS`): drei **18 px** hohe
Verweise und Schaltflächen im Fließtext der Kennzahlkarten („Diese 49 in der Nachrichtenliste
zeigen" 128 × 18, „Diese 1 …" 135 × 18, „Nach Art aufschlüsseln" 163 × 18 — bei `VOTG` gibt es die
beiden Verweise nicht, daher 17 statt 20), drei Kartenverweise „Diese Nachrichten in der Liste
öffnen" **312 × 30**, und der Rest **32 px** hoch: Menü 34 × 32, Belegart 34 × 32, Benutzermenü
34 × 32, Sprache 40 × 32 ×2, die Umschalter des Zeitraums und der Verteilung (71 bis 278 × 32), der
Mandant 246 × 32, das Suchfeld 262 × 32.

### 4.3 Nachrichtenliste (`/nachrichten`) — beide Mandanten je 50 Zeilen

| Breite | Kasten | Tabelle | Zeitpunkt | Status | **Ablauf** | Projekt | Kopfschnitt | gekürzte Zellen (ohne `title`) | Ziele < 44 | `main` |
|---:|---:|---:|---:|---:|---:|---:|---|---|---|---|
| 360 | 334 | **352** | 184 | 168 | **0** | — | — | 151 (1: der Kopf „Ablauf") | 69 von 69 | 2172 von 609 |
| 390 | 364 | 364 | 184 | 168 | **12** | — | — | 151 (1) | 69 / 69 | 2172 von 713 |
| 430 | 404 | 404 | 184 | 168 | **52** | — | — | 150 (0) | 69 / 69 | 2132 von 801 |
| **744** | 718 | 718 | 184 | 168 | **366** | — | — | 150 (0) | 69 / 69 | 2066 von 1002 |
| **768** | **518** | **640** | 184 | 168 | **0** | 288 | **„Ablauf" über „Projekt", Text 43,2 px** | 151 (1) | 72 / 72 | 2172 von 973 |

**Bei 360 px ist die Spalte, an der man eine Zeile erkennt, nicht da.** Zeitpunkt (11,5 rem) und
Status (10,5 rem) füllen 352 px in einem 334 px breiten Kasten; „Ablauf" hat 0 px, sein Kopftext
(43,2 px) beginnt bei x = 373 — **13 px rechts vom Fensterrand** — und alle 50 Ablaufzellen sind
0 px breit (Vollwert im `title`). Bei 390 px sind es 12 px, bei 430 px 52 px. **Bei 744 px trägt
die Liste** (Projekt ist unter `md` ausgeblendet, Ablauf bekommt 366 px), **bei 768 px fällt sie
wieder:** Projekt kommt mit 288 px zurück, 640 px Tabelle in 518 px Kasten, Ablauf 0 px, und
„Ablauf" steht an derselben x-Position wie „Projekt" (581) — der Klumpen aus M126, jetzt auf der
eigenen Route (Bild `bild-VOTG-nachrichtenliste-768.png`).

**Der Kontrollpunkt 744 aus dem Auftrag zeigt damit etwas anderes als erwartet:** Die 744 px aus
[`nachrichtenliste.md`](nachrichtenliste.md) sind die Breite der **vierspaltigen** Tabelle. Unter
`md` gibt es sie nicht — dort ist die Tabelle dreispaltig und braucht 352 px. Die Schwelle liegt
deshalb nicht bei 744 px Fensterbreite, sondern zweimal: **unter 430 px** (dreispaltig) und
**genau bei 768 px** (vierspaltig, `main` 560 px). Zwischen 430 und 767 px ist die Liste vollständig.

Die Kästen: `scrollWidth` des Tabellenkastens ist bei 360 bis 430 px **591** bei `NEXANS` und
**647** bei `VOTG`, bei 744 px 718 — die Tabelle selbst ist 352 bis 404 px breit, bei beiden gleich.
**Die scrollbare Breite über die Tabelle hinaus (239 beziehungsweise 295 px bei 360 px) hängt also
an den Daten, nicht am Layout; woher sie kommt, ist in dieser Runde nicht bestimmt** — in Teil 2
nachzusehen, bevor jemand „die Tabelle scrollt um 18 px" schreibt.

Ziele unter 44 px: alle 50 Zeilen 352 × **36** (`--dichte-zeile`), der Sortierknopf 89 × **20**,
der Schalter „Automatisch aktualisieren" 32 × **18**, „Nächste Seite" 38 × 32, die Filter (Status
166 × 32, Prozess 192 × 32, Suchfeld 259 × 32, Zeitfenster 307 × 32), Menü, Belegart,
Benutzermenü, Sprache. Scrollbereiche: `main` scrollt; der Listenkasten und der Tabellenkasten sind
mit `overflow-y: auto` angelegt und scrollen nicht (1832 von 1832).

### 4.4 Nachrichtenliste mit Panel (`?nachricht=…`) und Nachrichtendetail (`/nachrichten/<id>`) — Zahlen identisch

| Breite | Überlauf | Kopfzeile | Scroller aktiv / angelegt | `main` | Ziele < 44 (N / V) | gekürzt außerhalb Tabellen (ohne `title`) |
|---:|---:|---|---|---|---|---|
| 360 | 0 | 3 Zeilen, 131 px | 0 / 1 | 609 von 609 | 20 von 20 / 15 von 15 | 5 (N: **1**, V: **2**) |
| 390 | 0 | 3 Zeilen | 0 / 1 | 713 von 713 | 20 / 15 | 4 (N: 1, V: 1) |
| 430 | 0 | 3 Zeilen | 0 / 1 | 801 von 801 | 20 / 15 | 3 / 2 (0) |
| 744 | 0 | 3 Zeilen | 0 / 1 | 1002 von 1002 | 20 / 15 | 0 |
| 768 | 0 | 1 Zeile, Navspalte | 0 / 2 | 973 von 973 | 23 / 18 | 2 (Produktname ohne `title`) |

Unter `md` ersetzt das Panel die Liste; keine Tabelle, nichts scrollt (die beiden Nachrichten sind
kurz). **Ohne `title` gekürzt sind die Felder *Projekt* und *Prozess* im Kopf des Details** — bei
`NEXANS` das Projekt bei 360 und 390 px, bei `VOTG` Projekt und Prozess bei 360 px, Prozess bei
390 px. Der Baustein `Feld` in `nachricht-detail.tsx` sagt über sich: *„Eine Zeile hoch, gekürzt,
Vollwert im `title` — dieselbe Regel wie in der Liste"* — und bekommt für diese beiden Felder keinen
`hinweis`, also keinen `title`. Die anderen gekürzten Werte (Kennung, Schrittnamen der Zeitleiste)
tragen ihren. Ziele: Schließen / Zurück 32 × 32, Kennung kopieren 32 × 32, die Artefakt-Zeichen der
Zeitleiste **32 × 32** (sechs Stück), die Schrittknöpfe 58 × 36, Belegdaten 115 × 32, Eigenschaften
213 × 32.

### 4.5 Prozessansicht (`/prozesse`), Baum

| Breite | Überlauf | Kopfzeile | Scroller | `main` (N / V) | Ziele < 44 (N / V) |
|---:|---:|---|---|---|---|
| 360–744 | 0 | 3 Zeilen | 1 / 1 | 8488 / 5032 (744: 8454 / 4998) | 243 von 243 / 147 von 147 |
| 768 | 0 | 1 Zeile, Navspalte | 1 / 2 | 8592 / 5056 | 240 / 247 · 148 / 151 |

Keine Tabelle. Unter 44 px: alle Baumzeilen 336 × **36** (229 bei `NEXANS`, `--dichte-bedienzeile`
am Zeigergerät), der Schalter „Nur mit Verkehr im Zeitraum" 32 × **18**, das Eingrenzungsfeld
336 × 32, die Zeitraum-Umschalter.

### 4.6 Prozessansicht mit gewähltem Prozess (`?prozess=…`)

Unter `md` ersetzt die Liste den Baum — **alle Tabellenzahlen sind die aus 4.3** (352 / 364 / 404 /
718 px, Ablauf 0 / 12 / 52 / 366). Bei 768 px stehen Baum und Liste nebeneinander:

| | Kasten | Tabelle | Ablauf | Projekt | Kopfschnitt | Scroller aktiv / angelegt |
|---|---:|---:|---:|---:|---|---|
| 768 | **294** | **640** | **0** | 288 | „Ablauf" über „Projekt", 43,2 px | **2** / 5 — `main` und die klebende Spalte (1944 von 867) |

Die 294 / 640 / 0 / 288 sind M126 (§1, Eichung). Der zweite Scrollbereich ist die **zugesagte**
Ausnahme aus [`frontend-grundlagen.md`](frontend-grundlagen.md) §7 (E‑115: `/prozesse`, keine
Nachricht offen, ab `md`) und kein Befund. Ziele bei 768 px: 295 von 312 (`NEXANS`) — Baumzeilen
und Listenzeilen zusammen.

### 4.7 Belegsuche mit Treffern (`/suche?begriff=…`) — `NEXANS` 50 Treffer, `VOTG` 1

| Breite | Kasten | Tabelle | Zeitpunkt | Status | Treffer | Kette | Ablauf | gekürzt (ohne `title`, N) | Ziele < 44 (N / V) |
|---:|---:|---:|---:|---:|---:|---:|---:|---|---|
| 360 | 334 | **696** | 184 | 168 | 208 | 136 | — | 155 (0) | 58 von 60 / 9 von 11 |
| 390 | 364 | 696 | 184 | 168 | 208 | 136 | — | 155 (0) | 58 / 60 · 9 / 11 |
| 430 | 404 | 696 | 184 | 168 | 208 | 136 | — | 155 (0) | 58 / 60 · 9 / 11 |
| 744 | 718 | 718 | 189,8 | 173,3 | 214,6 | 140,3 | — | 155 (0) | 58 / 60 · 9 / 11 |
| 768 | **518** | 696 | 184 | 168 | 208 | 136 | **0** | 206 (1: der Kopf „Ablauf") | 61 / 63 · 12 / 14 |

Unter `md` scrollt die 696 px breite Tabelle in ihrem Kasten (bei 360 px um 362 px) — **bekannt**
([`property-suche.md`](property-suche.md) §14, Punkt 11: „die Tabelle scrollt in ihrer Hülle"). Bei
744 px füllt sie den Kasten und die Spalten wachsen anteilig. **Bei 768 px kommt die fünfte Spalte
„Ablauf" ohne Breite dazu und bekommt 0 px** (Kopftext 43,2 px bei x = 933, rechts außerhalb); ein
Schnitt entsteht nicht, weil rechts nichts mehr steht. Ziele: die Zeilen 696 × 36, „Begriff
entfernen" **32 × 32** (bekannt, ebenda Punkt 3), die 32-px-Klasse.

### 4.8 Benutzerverwaltung (`/administration/benutzer`) — 6 Konten, beide Mandanten identisch

| Breite | Kasten | Tabelle | Spalten | Kopfschnitt | gekürzt (ohne `title`) | Ziele < 44 | `main` |
|---:|---:|---:|---|---|---|---|---|
| 360 | 336 | **488** | Benutzer 128 · Sperre 96 · Zeitsperre 112 · Konto 96 · Bearbeiten 56 | — | 7 (1: der Kopf „Bearbeiten", 76,9 px Text in 56 px) | 16 von 16 | 617 von 609 |
| 390–430 | 366 / 406 | 488 | dieselben | — | 7 (1) | 16 / 16 | passt |
| 744 | 720 | 720 | | — | 7 (1) | 16 / 16 | passt |
| 768 | **520** | **792** | Benutzer 176 · Rolle 144 · **Mandanten 0** · Sperre 120 · Zeitsperre 176 · Konto 120 · Bearbeiten 56 | **„Mandanten" über „Sperre", 46,6 px** | 14 (**8**: Kopf „Mandanten", sechs Mandantenzellen à 0 px, Kopf „Bearbeiten") | 19 / 19 | 1007 von 973 |

Unter `md` scrollt die 488 px breite Tabelle in ihrem 336 px breiten Kasten (152 px). Bei 768 px
fällt die Spalte ohne Breitenklasse — *Mandanten* — auf 0 px; ihre sechs Zellen tragen keinen
`title`, **der Mandant eines Kontos ist bei 768 px unsichtbar**. Ziele: sechs Bearbeiten-Knöpfe
**32 × 32**, „Konto anlegen" 138 × 32, die Reiter 107 und 157 × 32.

### 4.9 Prozess-Katalog (`/administration/katalog`) — `NEXANS` 733 Zeilen, `VOTG` 390

| Breite | Kasten | Tabelle | Spalten | Kopfschnitt | gekürzt (ohne `title`) N / V | Ziele < 44 N / V | `main` N / V |
|---:|---:|---:|---|---|---|---|---|
| 360 | 336 | 336 | Prozess 144 · Partner 96 · Pflege 96 | — | 733 (0) / 390 (0) | 744 von 747 / 401 von 404 | 70.048 / 44.653 von 609 |
| 390–430 | 366 / 406 | = Kasten | | — | | | |
| 744 | 720 | 720 | Prozess 192 · Partner 432 · Pflege 96 | — | 733 (0) / 390 (0) | | 35.851 (V) |
| 768 | **520** | **696** | Prozess 256 · **Partner 0** · Richtung 128 · Nachrichten 176 · Pflege 136 | **„Partner" über „Richtung", 52,5 px** | 1467 (**734**) / 781 (**391**) | 747 / 750 · 404 / 407 | **185.708 / 75.925** von 973 |

Unter `md` passt die dreispaltige Tabelle **genau** in den Kasten und scrollt nicht; die
Pflegezellen (Zustand + Bearbeiten) sind gekürzt mit `title`. **Bei 768 px verliert der Katalog die
Partnerspalte** — sie hat keine Breitenklasse, die vier anderen füllen 696 px in 520 px —, und weil
ihre Zellen `white-space: normal` und `overflow: visible` tragen, **bricht jeder Partnername
buchstabenweise um** (Bild `bild-VOTG-prozess-katalog-768.png`). Nachgemessen an `VOTG`:

| | 360 px | 768 px |
|---|---:|---:|
| Zeilenhöhe min / Median / max | 71 / 111 / 151 px | **79 / 189 / 739 px** |
| Zeilen höher als 48 px | 390 von 390 | 390 von 390 |
| Zeilenhöhe der Tabellenzeile laut Konzept | 36 px | 36 px |
| Partnerzelle der ersten Zeile | 96 × 93 | **0 × 189** (acht Buchstaben, Zeilenabstand 22 px) |
| `main.scrollHeight` | 44.653 | 75.925 |

Bei `NEXANS` sind es 185.708 px für 733 Zeilen — **253 px je Zeile** bei 768 px, 96 bei 360.
Auch bei 360 px sind die Zeilen drei- bis fünfzeilig (Prozessname in 144 px, Partner in 96 px);
das ist kein Fehler des Rahmens, sondern die Bauform des Katalogs (er kürzt nicht, er bricht um —
[`prozess-katalog-frontend.md`](prozess-katalog-frontend.md)), aber es ist nie bei 360 px
angesehen worden (dort §12, Punkt 3). Die 734 Zellen ohne `title` sind der Kopf „Partner" und alle
733 Partnerzellen. Ziele: 733 Bearbeiten-Knöpfe **32 × 32**, zwei Kontrollkästchen **16 × 16**
(„nur offene", „nur mit Nachrichten"), die Reiter.

### 4.10 Die Kopfzeile — auf allen Routen, beiden Mandanten, allen Breiten unter 768 px

| | 360 · 390 · 430 · 744 px | 768 px |
|---|---|---|
| Zeilen | **3** — y = 8: Menüschalter, Benutzermenü, Produktname · y = 48: Mandant und Sprache · y = 90: Suchfeld | 1 (Produktname, Suchfeld 288 px, Mandant, Sprache, Benutzermenü) |
| Höhe | **131 px** | 51 px |
| Mandantencode | sichtbar, nicht gekürzt (`NEXANS`, `VOTG`) | sichtbar |
| Suchfeld | **eigene Zeile**, volle Breite | in der Zeile |
| Navigation | Schublade (Schalter 34 × 32), `aside` verborgen | Spalte sichtbar, `main` 560 px |
| Produktname | vollständig | **gekürzt ohne `title`** („Overlord Monitoring") |

**Die Zusage aus [`visuelles-konzept.md`](visuelles-konzept.md) §6 hält bei allen 90 Messungen des
angemeldeten Teils** (neun Zustände × fünf Breiten × zwei Mandanten): drei Zeilen, Code sichtbar,
Suchfeld eigene Zeile, `scrollWidth` = `innerWidth`.

> ### Belegvermerk zu M176 (Regel L10)
>
> *Gemessen war:* der Produktionsbau im Chrome 152 — die Anmeldeseite kopflos per CDP mit
> Geräteemulation, die acht Routen und zwei Zustände im angemeldeten Chrome der Erweiterung in
> einem gleichherkunftigen Rahmen, DPR 1, Dunkelmodus, Dichte `m`, `pointer: fine`; bei fünf
> Breiten und zwei Mandanten Dokumentmaße, Kopfzellen als Kasten und Textkasten, Zellenkürzungen,
> Scrollbereiche, Layoutmaße der Bedienelemente, die Kopfzeile; dazu die Zeilenhöhen des Katalogs
> bei `VOTG`.
>
> *Behauptet wird:* dass die Zusagen des Rahmens (§6, §7) bei diesen Breiten halten, dass die
> vier Tabellen unter denselben Bedingungen eine Spalte auf 0 px verlieren, und dass die Zahlen
> für beide Mandanten dieselben sind, wo sie am Layout hängen.
>
> *Behauptet wird nicht:* dass ein echtes Gerät dieselben Berührungsflächen zeigt (`pointer: coarse`
> ist im Rahmen nicht gesetzt; welche Klassen der Token hebt, ist per CDP belegt und in §5
> gerechnet, nicht dort gemessen); dass ein `mobile`-Sichtfenster mit DPR 2 auf den Pixel dieselben
> Maße liefert (ein Rahmen mit DPR 1 und ohne Rinne ist das Nächste, was ohne CDP-Sitzung geht —
> geeicht an M126); dass die Zahlen für die Rolle `MANDANT`, für Englisch, für den hellen Modus
> oder für eine andere Dichtestufe gelten.

---

## 5. Die drei Körbe

### (a) widerspricht einer verbindlichen Zusage, Behebung ohne Entscheidung möglich

| | Befund | Route · Breite | gemessen | Zusage | kleinster Eingriff |
|---|---|---|---|---|---|
| **176** *(neu)* | **Projekt und Prozess im Kopf des Details werden ohne `title` gekürzt** | Detail und Panel · 360, 390 px | `NEXANS`: Projekt 360/390; `VOTG`: Projekt und Prozess 360, Prozess 390 (§4.4) | der Baustein `Feld` selbst: *„Eine Zeile hoch, gekürzt, Vollwert im `title` — dieselbe Regel wie in der Liste"* ([`nachrichtenliste.md`](nachrichtenliste.md) §8.1) | den beiden `Feld`-Aufrufen in `nachricht-detail.tsx` den Wert als `hinweis` mitgeben. Keine Entscheidung: Die Regel steht, sie ist nur an zwei Stellen nicht angewandt |

### (b) braucht eine Entscheidung

| | Befund | Route · Breite | gemessen | welche Entscheidung |
|---|---|---|---|---|
| **114** *(bestätigt und erweitert)* | **Die Ablaufspalte der Nachrichtenliste fällt auf 0 px — auf der eigenen Route.** Der Satz *„Auf der eigenen Route ist das kein Fall"* ([`nachrichtenliste.md`](nachrichtenliste.md) §8.1) ist gemessen falsch: bei 768 px (`main` 560 px) und unter 430 px | Liste und Prozessansicht-Zustand · 360, 390, 430, 768 px | 0 / 12 / 52 px unter `md`; bei 768 px 0 px mit „Ablauf" über „Projekt" (Textschnitt 43,2 px), Kasten 518 / 294, Tabelle 640 (§4.3, §4.6) | **die Spaltenbreiten der Nachrichtenliste** — unverändert die Entscheidung aus 114, jetzt mit drei Schwellen: unter 430 px dreispaltig, bei 768 px vierspaltig, und die Frage, ob eine Spalte ohne Platz ihre Beschriftung zeichnen darf. Dazu die offene Zahl: `scrollWidth` 591 (`NEXANS`) beziehungsweise 647 (`VOTG`) des Kastens bei 360 bis 430 px (§4.3) |
| **173** *(neu)* | **Die Trefferliste bekommt bei 768 px eine fünfte Spalte ohne Platz**: „Ablauf" 0 px, Text rechts außerhalb | Belegsuche · 768 px | Kasten 518, Tabelle 696 + 0; unter `md` scrollt die 696-px-Tabelle (bekannt, siehe (c)) (§4.7) | die Spaltenbreiten der **Trefferliste** — eigene Tabelle (`treffer-tabelle.tsx`), eigene Breiten, dieselbe Bauform wie 114. Ob „Ablauf" erst ab `lg` kommt oder eine Breite bekommt |
| **174** *(neu)* | **Die Benutzerverwaltung verliert bei 768 px die Mandantenspalte**, und unter `md` scrollt ihre Tabelle 152 px | Benutzerverwaltung · 360–430 und 768 px | 768: Mandanten 0 px, „Mandanten" über „Sperre" (46,6 px), sechs Zellen ohne `title`; 360: 488 px Tabelle in 336 px Kasten, Kopf „Bearbeiten" 56 px für 76,9 px Text (§4.8) | die Spaltenbreiten der **Benutzertabelle** unter `lg` — welche Spalte bei 768 px weicht (heute die ohne Breite: der Mandant) und ob die Tabelle unter `md` scrollen darf. [`benutzerverwaltung-frontend.md`](benutzerverwaltung-frontend.md) §14 führte „360 px" als offen; jetzt liegt die Zahl vor |
| **175** *(neu)* | **Der Prozess-Katalog verliert bei 768 px die Partnerspalte, und die Zeilen werden 79 bis 739 px hoch** | Prozess-Katalog · 768 px (und die Zeilenhöhe auch bei 360 px) | Partner 0 px, „Partner" über „Richtung" (52,5 px), 734 / 391 Zellen ohne `title`, Median 189 px je Zeile, `main` 185.708 px bei `NEXANS`; bei 360 px Median 111 px (§4.9) | die Spaltenbreiten des **Katalogs** bei `md` (welche Spalte weicht: Partner ist die tragende) und ob der Katalog unter `md` kürzt statt umbricht. [`prozess-katalog-frontend.md`](prozess-katalog-frontend.md) §12 Punkt 3 fragte, „ob die Tabelle mit ihren drei sichtbaren Spalten trägt" — bei 360 px trägt sie, mit drei- bis fünfzeiligen Zeilen |
| **177** *(neu)* | **Der Produktname der Kopfzeile ist bei 768 px gekürzt, ohne `title`** | alle Routen · 768 px | `main` 560 px; Suchfeld 288 px, Mandant, Sprache, Benutzermenü lassen dem Namen (`flex-1 truncate`) nicht die 154 px, die er braucht (§4.10) | ob der Name bei `md` gekürzt werden darf (dann mit `title`), umbrechen soll oder bei `md` auf ein Kürzel fällt. Keine Zusage im Konzept betrifft ihn — die Zusage „nie der Code" gilt dem Mandanten, und der hält |
| **178** *(neu)* | **Vier Klassen von Bedienelementen bleiben auch am Finger unter 44 px, die Punkt 96 nicht führt** | Dashboard, Detail, Katalog · alle Breiten | Verweise und Schaltflächen im Fließtext der Kennzahlkarten **18 px** hoch (128 bis 163 px breit), Kartenverweise **312 × 30**, die Artefakt-Zeichen der Zeitleiste **32 × 32**, die Kontrollkästchen des Katalogs **16 × 16**; dazu die Breite der Symbolknöpfe (32) und der Sprachschaltflächen (40) — Klassen, die `--dichte-bedienelement` in der Höhe hebt und in der Breite nicht (§4.2, §4.4, §4.9, Abweichung 3) | dieselbe wie bei 96 — ob Berührungsziele im Fließtext und Symbolknöpfe eine Fläche von 44 × 44 bekommen — nur um vier Klassen erweitert. Gerechnet, nicht gemessen: Mit `pointer: coarse` bleiben von den 69 Zielen der Liste bei 360 px die 50 Zeilen (36), der Sortierknopf (20) und der Schalter (18) unter 44 px, die 32-px-Klasse steigt auf 44 — das ist Punkt 96 unverändert |

### (c) bereits bekannt, mit der Nummer des offenen Punkts

| Befund | Route · Breite | gemessen | bekannt als |
|---|---|---|---|
| Tabellenzeilen 36 px, Sortierknopf 20 px, Schalter 18 px, Eingabefelder 40 px | Liste, Suche, Baum, Anmeldung | 50 Zeilen 352 × 36, 89 × 20, 32 × 18; 304 × 40 (§4.1, §4.3) | **96** ([`dichte-umschalter.md`](dichte-umschalter.md) §9) |
| Baumzeilen 36 px am Zeigergerät | Prozessansicht | 229 × 336 × 36 (§4.5) | E‑54 / **117** geschlossen ([`process-view.md`](process-view.md) §24): `--dichte-bedienzeile`, am Finger 44 |
| Symbolknöpfe 32 px breit, Sprachschaltflächen 40 px breit, auch am Finger | alle Routen | 34 × 32, 40 × 32 (§4.1, §4.2) | [`dichte-umschalter.md`](dichte-umschalter.md) §5.4: *„Eine Fläche von 44 × 44 ist das nicht … 28 × 44 in `xs` … Sprachumschaltung (35–45 px breit)"* |
| Die Trefferliste scrollt unter `md` in ihrer Hülle, die Marken-Schließknöpfe 32 × 32 | Belegsuche · 360–430 px | 696 px in 334 px, „Begriff entfernen" 32 × 32 (§4.7) | [`property-suche.md`](property-suche.md) §14, Punkte 11 und 3; [`README.md`](README.md) „Offene Sichtprüfungen", Schritt 7 Teil 3 |
| Zwei Scrollbereiche in der Prozessansicht ab `md` | Prozessansicht-Zustand · 768 px | `main` und die klebende Spalte 1944 von 867 (§4.6) | E‑115, [`frontend-grundlagen.md`](frontend-grundlagen.md) §7, benannte Ausnahme |
| Die Artefakt-Zeichen der Zeitleiste am Finger | Detail · alle Breiten | 32 × 32, sechs Stück (§4.4) | [`rohdaten-frontend.md`](rohdaten-frontend.md) §11 Punkt 1 fragte danach — jetzt gemessen, als Klasse in **178** geführt |

### Nebenbefund außerhalb der sieben Messgrößen

| | |
|---|---|
| **172** *(neu)* | **Jeder Seitenaufruf protokolliert `404 /favicon.ico`.** Gemessen per CDP auf `/anmeldung` bei allen fünf Breiten, je Aufruf ein Ereignis; kein `favicon.ico` unter `src/app/`, kein `public/`. Kein Widerspruch zu einer Zusage, keine Entscheidung nötig; was auf dem Zeichen zu sehen wäre, gehört ins [`visuelles-konzept.md`](visuelles-konzept.md) |

### Was die Körbe nicht tragen — die beiden Zusagen, die halten

**Kein waagerechter Überlauf und nur ein Scrollbereich**, beides 100 von 100 Messungen: Das gehört
festgehalten, weil die Auftragsbegründung (übereinanderstehende Beschriftungen) den Verdacht nahelegte,
dass am schmalen Fenster mehr bricht als Tabellenspalten. Es bricht nichts anderes. Die vier
Tabellenbefunde teilen eine Ursache und liegen nebeneinander in (b); wer 114 entscheidet, hat 173
bis 175 mit auf dem Tisch — **dieselbe Entscheidung, viermal, nicht vier Entscheidungen**.

---

## 6. Was diese Runde nicht zeigt

1. **Die Rolle `MANDANT`** — Mandantenanzeige statt Umschalter, Schublade ohne Administration
   (§1, Abweichung 2). Fahrbar mit dem CDP-Skript, sobald ein Konto benannt ist.
2. **Kein echtes Berührungsgerät und kein `pointer: coarse` im angemeldeten Teil.** Was der Token
   hebt, ist per CDP auf `/anmeldung` belegt; §5 rechnet damit, misst es aber nicht an den
   angemeldeten Routen. `mobile: true` setzt `pointer: coarse` nicht ([`bam-suche.md`](bam-suche.md)
   §13, Punkt 8 gilt weiter).
3. **Nur Chromium, nur der Dunkelmodus, nur Deutsch, nur Dichte `m`, nur die Testkopie, DPR 1.**
   Die englischen Texte sind länger; die Kopfzeile bei 360 px ist mit ihnen nicht am Bild gewesen.
4. **Kein Zeilenklick, kein Menü, keine Schublade im Messlauf.** Gemessen ist der Ruhezustand jeder
   Route; die Prüfdaten sind über einen Zeilenklick bei 1280 px gewonnen. Ob die Schublade oder
   ein Radix-Menü bei 360 px aus dem Bild läuft, bleibt die eigene Frage aus
   [`README.md`](README.md) „Offene Sichtprüfungen" (Untermenüs im Suchfeld).
5. **Punkt 97** (die Kopfzeile der Nachrichtenliste hält nicht) ist nicht nachgemessen — keine der
   sieben Messgrößen.
6. **Die 239 beziehungsweise 295 px** scrollbarer Breite im Listenkasten unter 430 px, deren Ursache
   offen ist und die an den Daten hängt (§4.3).
7. **Die Nachrichtendetails sind kurz** — beide Prüfnachrichten passen bei 360 px in 609 px. Ein
   Detail mit Kette, vielen Belegwerten und Eigenschaften ist nicht am schmalen Fenster gewesen; das
   Panel scrollt dann in `main`, und mehr ist über es nicht gemessen.
8. **Keine Wanduhrzeit als Kriterium** (Regel T1). Ruhe- und Einschwingzeiten sind Wartezeiten des
   Werkzeugs.
9. **Die Bilder sind mit Faktor 0,5 aufgenommen** und liegen lokal; sie belegen nichts, was nicht
   als Zahl in §4 steht.

---

## 7. Wie die Erhebung wiederholt wird

Nach Teil 2 — mit einer der beiden Bauformen, je nachdem, ob ein Konto in der Umgebung liegt.

```
# Backend im Profil dev und Frontend als Produktionsbau
cd backend  && ./mvnw spring-boot:run -Djooq.codegen.skip=true
cd frontend && pnpm exec next build && pnpm start

# Bauform A — kopfloses Chrome, Anmeldung per Formular (Zugangsdaten nur in der Shell)
#   $env:SICHTPROBE_ADMIN_USER = '…'; $env:SICHTPROBE_ADMIN_PASSWORD = '…'
node scripts/sichtprobe-schmal/sichtprobe.mjs --rolle ADMIN   --mandanten NEXANS,VOTG
node scripts/sichtprobe-schmal/sichtprobe.mjs --rolle MANDANT
node scripts/sichtprobe-schmal/sichtprobe.mjs --ohne-anmeldung        # nur /anmeldung

# Bauform B — der Nutzer meldet sich im Chrome der Erweiterung an
node scripts/sichtprobe-schmal/empfaenger.mjs                          # nimmt die Ergebnisse an
#   im javascript_tool: messung.js und rahmen.js in die Oberseite (http://localhost:3000/) einsetzen,
#   dann je Route  await __sp.route('NEXANS/dashboard', '/')  — eine Route je Aufruf,
#   der Tab muss sichtbar bleiben; Ergebnisse unter ergebnis/rahmen/
node scripts/sichtprobe-schmal/auswertung.mjs > scripts/sichtprobe-schmal/ergebnis/rahmen/zusammenfassung.md
```

Die Zahlen in dieser Datei stammen aus den Ergebnisdateien und sind nicht abgetippt; `auswertung.mjs`
erzeugt die Tabellen je Route, und `pruefung-m176.mjs` hält **131 Aussagen** dieser Datei gegen die
JSON-Dateien (Stand nach dem Lauf: null Abweichungen; §8).

---

## 8. Regelbezug

| Regel | Wie sie hier gilt |
|---|---|
| **L7** | zwei Mandanten auf jeder angemeldeten Route: `NEXANS` und `VOTG`; die Layoutzahlen sind gleich, die Zeilen, Ziele und Höhen stehen je Mandant |
| **L10** | Belegvermerk in §4; jede Zahl aus `ergebnis/rahmen/…json` beziehungsweise `…OFFEN-messung.json`, mit einem Prüfskript gegengehalten |
| **Q4** | nichts geraten: Die Ursache der 239 / 295 px (§4.3) und die Rolle `MANDANT` stehen als offen da, nicht als Vermutung |
| **T1** | keine Wanduhrzeit als Kriterium |
| **G1** | keine Zugangsdaten, keine Kennungen, keine Belegwerte, keine Prozessnamen in dieser Datei; `ergebnis/` in `.gitignore` |
| **S1** | kein Schreibzugriff — die Sitzung ist die des Nutzers, Mandantenwahl und Zeilenklick sind Sitzungszustand |
| **§6 des Auftrags** | nichts gegen die Produktion, keine Spaltenbreite, kein Umbruchpunkt, keine Datei unter `components/ui`, kein Farbwert, keine Korrektur im selben Lauf, keine Rohdatenansicht, kein Bild mit Rohdaten |
