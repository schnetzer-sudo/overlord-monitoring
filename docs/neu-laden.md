# Neu laden und automatische Aktualisierung

*16.09.2026.* Entscheidungen **E‑163** bis **E‑172**, Messungen **M181** (Sitzung) und **M182**
(Sichtprüfung am laufenden System), neue offene Punkte **184** und **185**. Fortgeschrieben: Punkt
**121** ([`process-view.md`](process-view.md) §13) und Punkt **168**
([`dashboard-frontend.md`](dashboard-frontend.md) §9). **Am selben Tag korrigiert:** Entscheidungen
**E‑173** (Ort) und **E‑174** (Knopf ohne Wort), Messung **M183** — Korrekturblock unmittelbar unter
dem Ertrag; in einer dritten Runde **E‑175** (auch im freien Modus ganz rechts) und Messung **M184**,
Block darunter.

**Die Frage dieser Datei:** Wie holt sich jemand, der einen Beleg sucht, den neuesten Stand — auf der
Übersicht, in der Nachrichtenliste und in der Prozessansicht —, ohne dass die Ansicht dabei springt,
ihm die Stelle nimmt oder auf der Produktionsdatenbank mehr fragt als nötig?

> ### Der Ertrag in einem Absatz
>
> **Alle drei Seiten tragen „Neu laden" unmittelbar links neben den Zeitraum-Schaltflächen**, in
> deren Höhe und mit deren Abstand — gemessen in 24 Lagen (M182). **Die automatische
> Aktualisierung gibt es nur noch in den Nachrichten**, als Schalter „Auto" direkt davor; die
> Prozessansicht hat ihre verloren, und das ist gewollt. Die Regeln der Aktualisierung sind
> unverändert (60 s, nur Seite eins, nur bei sichtbarem Tab, Vorgabe aus). **„Neu laden" führt in
> einer Liste auf Seite eins**, holt nie das Panel und bewegt auf der Übersicht nichts — das Letzte
> war **nicht** von selbst so und ist gebaut (E‑170). **Zwei Befunde:** Unter `xl` bei offenem
> Panel sind Schalter und Knopf in den Nachrichten nicht zu sehen (Punkt 121 bleibt dort offen), und
> **ein automatischer Abruf verlängert die Sitzung** (M181, Punkt 184).

> ### ⚠️ Korrektur vom 16.09.2026, zweite Runde — der Knopf steht rechts, in den Nachrichten am Rand, und ohne Wort (E‑173, E‑174)
>
> **Der Ertrag darüber bleibt stehen; zwei seiner Angaben gelten nicht mehr.** Nach der ersten
> Abnahme hat der Auftraggeber am selben Tag entschieden:
>
> | | bis zur Korrektur | seither |
> |---|---|---|
> | **Übersicht, Prozessansicht** | „Neu laden" unmittelbar **links** neben den Zeitraum-Schaltflächen (E‑163) | unmittelbar **rechts** neben dem letzten Zeitraum-Knopf — „12 Monate" bzw. „Frei" — und damit rechts außen im Kopf (E‑173) |
> | **Nachrichten** | Schalter, Knopf, Zeitraum — eine Gruppe vor dem Zeitfenster (E‑163) | Schalter und Knopf **am rechten Rand der Filterleiste**, der Knopf außen, bündig mit der Tabelle (E‑173) |
> | **Beschriftung des Knopfes** | Pfeile und „Neu laden" (E‑166) | **nur die Pfeile**; „Auto" am Schalter bleibt (E‑174) |
>
> **Was gleich bleibt:** die Höhe eines Zeitraum-Knopfes, Name und Tooltip je Ansicht, Sanduhr und
> `aria-busy` beim Laden, das Verhalten aus §3 und die Anfragen aus M182‑3. Der Knopf ist jetzt
> **quadratisch**: `size="icon"` samt `min-w-bedienelement`, damit er auch am Berührungsgerät
> quadratisch bleibt.
>
> **Gemessen in M183** (§6), 24 von 24 Lagen. **Zwei Folgen, die man sieht:** Bei 1280 px ohne
> Panel stehen Schalter und Knopf in den Nachrichten **in einer zweiten Zeile** der Filterleiste,
> rechts (bei `xs` im Bild: allein darin); bei 1920 px in der Zeile des Zeitfensters. Und im freien Modus der
> Prozessansicht erscheinen die Datumsfelder **rechts** vom Knopf — er bleibt neben „Frei" und rückt
> mit dem Umschalter nach links (M183‑3). **Punkt 185 wiegt dadurch schwerer** (§9).

> ### ⚠️ Korrektur vom 16.09.2026, dritte Runde — auch im freien Modus ganz rechts (E‑175)
>
> **Der Block darüber gilt bis auf seinen Satz zum freien Modus.** Der Auftraggeber hat entschieden:
> Der Knopf steht **auch im freien Modus der Prozessansicht ganz rechts**, hinter den Datumsfeldern
> und ihrem Hinweis. Umschalter, Felder und Knopf sind dafür Kinder **einer** Reihe; der Knopf trägt
> `ml-auto` und steht damit auch dann am Rand, wenn „Bis" umbricht, und die Reihe trägt es ebenfalls
> und steht rechts, wenn sie nicht neben die Überschrift passt.
>
> **Gemessen in M184** (§6): In 18 Lagen steht der Knopf vor und nach dem Klick auf „Frei" am
> rechten Rand, waagrecht unverändert, als letztes Bedienelement. **Zwei Nebenwirkungen, die man
> sieht:** Die Datumsfelder stehen **0,5 statt 0,75 rem** voneinander und vom Umschalter entfernt —
> derselbe Abstand wie zwischen den Datumsfeldern der Nachrichtenliste. Und bricht die Bedienreihe
> unter die Überschrift um, steht sie **rechts** statt links — auf Übersicht und Prozessansicht,
> gemessen bei 1280 px in Dichte `l` und bei 360 und 400 px.

**Kein Endpunkt, keine Datenquelle, keine Migration.** Alles hier ruft Endpunkte, die es gibt, mit
denselben Parametern wie vorher: `/api/dashboard`, `/api/nachrichten`, `/api/prozesse/baum`.

---

## 1. Nummernvergabe

Verfahren aus [`process-view.md`](process-view.md) §46: Python-Suche mit Wortgrenzen über
`docs/*.md`, Strich als `-` oder U+2011, auf `main`, auf `fix/dashboard-verteilung-beide-sichten`
(der Basis dieses Zweigs), auf den nicht gemergten `feat/suchfeld-untermenues` und
`test/indexbestand-e37` **und auf dem unversionierten Arbeitsbaum** von
`fix/nachrichtenliste-spaltenbreiten`, auf dem zur selben Zeit eine andere Sitzung arbeitete. Jeder
Treffer im oberen Bereich gelesen; die Suche ist unmittelbar vor dem Commit wiederholt worden.

| | |
|---|---|
| **Entscheidung** | **E‑163 bis E‑172.** Höchste in `main` **E‑160**, auf der Basis **E‑161** ([`dashboard.md`](dashboard.md) §9b). **E‑162** steht nur im unversionierten Arbeitsbaum der anderen Sitzung ([`nachrichtenliste.md`](nachrichtenliste.md) §8.1 dort) — gelesen, vergeben, deshalb nicht genommen. **E‑151 bis E‑158** kommen nirgends vor; E‑150 und E‑159 sind Suchbereiche in der Vergabetabelle von [`spaltenwahl.md`](spaltenwahl.md), keine Vergaben. **E‑780** ist der bekannte Falschtreffer |
| **Messung** | **M181, M182.** Höchste auf der Basis **M178**; **M179** ist Fließtext ([`messungen-sichtprobe-schmal.md`](messungen-sichtprobe-schmal.md) §3); **M180** steht im Arbeitsbaum der anderen Sitzung |
| **Zweite Runde, am selben Tag** | **E‑173, E‑174, M183.** Dieselbe Suche wiederholt auf `main`, `feat/neu-laden`, `fix/nachrichtenliste-spaltenbreiten` (Commit und unversionierter Arbeitsbaum), `feat/suchfeld-untermenues`, `test/indexbestand-e37` (Commit und Arbeitsbaum) und `fix/dashboard-verteilung-beide-sichten`: höchste Entscheidung **E‑172**, höchste Messung **M182**, beide nur auf diesem Zweig; **E‑162** und **M180** weiterhin nur im Arbeitsbaum der anderen Sitzung. Kein neuer offener Punkt |
| **Dritte Runde, am selben Tag** | **E‑175, M184.** Dieselbe Suche auf denselben Zweigen und Arbeitsbäumen: höchste Entscheidung **E‑174**, höchste Messung **M183**, beide nur auf diesem Zweig |
| **Offener Punkt** | **184, 185.** **182** ist vergeben ([`dashboard.md`](dashboard.md) §11, die Wanduhrabhängigkeit von `DashboardIsolationDbIT.keine_mandanten_id`), **183** im Arbeitsbaum der anderen Sitzung ([`spaltenwahl.md`](spaltenwahl.md) §11 dort). Die Treffer **185** bis **188** in `spaltenwahl.md` sind Tabellenwerte, gelesen |

---

## 2. Der Baustein — `components/neu-laden.tsx`

**In `components/` und nicht in einem Feature**, weil drei Ansichten ihn tragen
([`frontend-grundlagen.md`](frontend-grundlagen.md) §8). Er rendert den Knopf und **auf Wunsch**
davor den Schalter — dasselbe Muster wie `aufFrei` am Zeitraumumschalter: **ohne die Angabe
`automatik` erscheint kein Schalter** (E‑164).

```
components/
└─ neu-laden.tsx                   „Neu laden" und, freiwillig, der Schalter „Auto"
features/nachrichten/
└─ aktualisierung.ts               wann die Liste fragt, wohin „Neu laden" führt — rein
features/dashboard/
└─ verlauf.ts                      + aufbauAktiv: Aufbau einmal je Zeitraum (E‑170)
```

| | Knopf „Neu laden" | Schalter „Auto" |
|---|---|---|
| **Sichtbar** | Pfeile (`RefreshCw`) und „Neu laden" | Uhr und „Auto" |
| **Zugänglicher Name** | je Ansicht: „Übersicht neu laden", „Nachrichten neu laden, ab Seite eins", „Prozessbaum und Übertragungen neu laden" | „Automatische Aktualisierung" |
| **Tooltip** (`title`) | derselbe Name | die Lage samt Grund, drei Sätze |
| **Lädt** | **Sanduhr statt Pfeile**, `aria-busy="true"`; ein Klick ruft nichts. Nicht `disabled` — ein gesperrter Knopf verlöre unter der Tastatur den Fokus | — |
| **Aus / an / pausiert** | — | **Uhr durchgestrichen / Uhr / Pause** — drei Symbole, dazu `aria-pressed` (`false` / `true` / `true`) |
| **Maße** | `min-h-bedienelement px-2.5`, dieselben Klassen wie ein Knopf des Zeitraumumschalters | ebenso |
| **Farbe** | Generatorbaustein `Button`, Variante `outline` | Generatorbaustein `Toggle`, Variante `outline` — die gedrückte Fläche ist die des Zeitraumumschalters |

**Keine neue Farbrolle, kein Orange, keine Bewegung.** Der gedrückte Zustand trägt `bg-muted` wie
jede gedrückte Schaltfläche des Zeitraumumschalters; dass diese Fläche mit 1,07 : 1 kaum zu sehen ist,
ist offener Punkt **92** ([`dashboard-frontend.md`](dashboard-frontend.md) §9) und hier **nicht**
schlimmer: Die Lage steht im Symbol, die Fläche ist die halbe Aussage
([`visuelles-konzept.md`](visuelles-konzept.md) §3).

> ⚠️ **Korrektur vom 18.09.2026 (E‑217) — der eingeschaltete Schalter „Auto“ trägt die
> Akzentfläche.** Der Absatz darüber bleibt stehen. **Sein erster Satz gilt weiter:** keine neue
> Farbrolle, kein Orange (E‑167), keine Bewegung. **Der zweite gilt für den Schalter nicht mehr:**
> Eingeschaltet trägt er nicht `bg-muted` wie ein gedrückter Zeitraum-Knopf, sondern die Fläche
> der gefüllten Schaltfläche. Zeile *Farbe* der Tabelle, Spalte *Schalter „Auto“*, seither:
> Generatorbaustein `Toggle`, Variante `outline`, eingeschaltet mit
>
> | Lage | `aria-pressed` | Fläche | Symbol und „Auto“ | Kontur |
> |---|---|---|---|---|
> | aus | `false` | unverändert | unverändert | unverändert |
> | an | `true` | `--akzent` | `--akzent-vordergrund` | `--akzent-schrift` |
> | pausiert | `true` | `--akzent` | `--akzent-vordergrund` | `--akzent-schrift` |
>
> - **Pausiert ist eingeschaltet.** Die Fläche sagt „an“, die Pause zeigt weiterhin das Symbol.
> - **Nur per `className` in `components/neu-laden.tsx`**, `components/ui/` ist unberührt. Der
>   Generator zeichnet den gedrückten Zustand **zweimal** aus, mit `aria-pressed:bg-muted` und
>   `data-[state=on]:bg-muted`, und Radix setzt beide Attribute (`aria-pressed="true"`,
>   `data-state="on"`, an wie pausiert). Überschrieben wird die Fläche deshalb unter **beiden**
>   Selektoren; `cn` im Generator verdrängt die gleichnamigen `bg-muted`. Schrift und Kontur hängen
>   an `aria-pressed` allein, dort setzt der Generator nichts.
> - **Warum beide — nachgesehen im ausgelieferten Stylesheet, nicht angenommen:** Tailwind stellt
>   die `data-*`-Varianten hinter die `aria-*`-Varianten. `data-[state=on]:bg-muted` steht bei
>   gleicher Spezifität (0,2,0) später als `aria-pressed:bg-akzent`; wer nur `aria-pressed:`
>   überschriebe, behielte am Element die graue Fläche. Probe 1 unten ist genau das, Probe 2 dasselbe
>   mit falschem Wert im Selektor; Probe 9 ist der Stand davor, ganz ohne Überschreibung.
> - **Klassen:** `aria-pressed:bg-akzent`, `data-[state=on]:bg-akzent`,
>   `aria-pressed:text-akzent-vordergrund`, `aria-pressed:border-akzent-schrift`,
>   `aria-pressed:hover:bg-akzent/80`, `aria-pressed:hover:text-akzent-vordergrund`. `bg-akzent` ist
>   `--akzent` (#b9c022, [`frontend-grundlagen.md`](frontend-grundlagen.md) §8b), **nicht** shadcns
>   `bg-accent` — jenes ist in diesem Projekt die blasse `--akzent-flaeche`.
> - **Überfahren wie die gefüllte Schaltfläche** (`Button`, Variante `default`, `hover:bg-primary/80`
>   mit `--primary` = `--akzent`), nie wie „aus“: Die beiden `hover`-Regeln des Schalters stehen bei
>   (0,3,0), `hover:bg-muted` und `hover:text-foreground` des Generators bei (0,2,0) — sie gewinnen
>   unabhängig von der Reihenfolge. Die Kontur (0,2,0) schlägt `border-input` (0,1,0). Der
>   **Fokusring** bleibt der des Generators, `--ring` = `--akzent-schrift`, und liegt außen.
> - **Kein neuer Wert.** Die Paarungen sind gerechnet ([`dunkelmodus.md`](dunkelmodus.md) §3.3):
>   `--akzent-vordergrund` auf `--akzent` 9,13 : 1 in beiden Blöcken, `--akzent-schrift` auf
>   `--card` 5,40 hell und 10,72 dunkel. `tests/farbkontrast.test.ts` führt Paarungen **je Token**,
>   nicht je Verwendung; beide sind dort in beiden Blöcken schon zugesichert — einzutragen war nichts.
> - **Kein Übergang hinzugefügt.** Der Generator bringt `transition-all` mit, und zwar an jeder
>   Schaltfläche und jedem Umschalter des Projekts; im Stylesheet 150 ms
>   (`--default-transition-duration: .15s`). Die Fläche blendet deshalb ein. Das steht quer zu
>   [`visuelles-konzept.md`](visuelles-konzept.md) §7 („Kein Übergang beim Überfahren“) — projektweit
>   und seit dem Generator, nicht erst hier. *Entschieden vom Auftraggeber am 18.09.2026:* hier
>   nichts abschalten, als Befund führen.
> - **Der Zeitraumumschalter bleibt, wie er ist.** Für ihn ist Punkt **92** unverändert offen
>   ([`dashboard-frontend.md`](dashboard-frontend.md) §9).
>
> **Test:** `tests/neu-laden.test.tsx`, *„trägt eingeschaltet die Akzentfläche, an wie pausiert, und
> aus nicht (E‑217)“* — der gerenderte Schalter in allen drei Lagen. Ob eine Klasse greift,
> entscheidet dort `matches` mit der Bedingung, die Tailwind aus der Variante macht. Dazu: keine
> Akzentklasse ohne Bedingung, keine der beiden grauen Flächen am Element, die `hover`-Klasse da.
>
> **Verletzungsproben** — je ein Eingriff in `components/neu-laden.tsx`, zurückgespielt aus einer
> Sicherungskopie und mit `cmp` verglichen, in keinem Commit; eine Leerprobe ohne Eingriff blieb
> grün:
>
> | Probe | Eingriff | rot |
> |---|---|---|
> | 1 | Fläche nur unter `aria-pressed:` | der neue Fall, schon bei *aus*: `data-[state=on]:bg-muted` bleibt am Element |
> | 2 | `data-[state=active]:` statt `data-[state=on]:` | derselbe, aus demselben Grund |
> | 3 | zusätzlich `bg-akzent` ohne Bedingung | der neue Fall: *„aus: expected [ 'bg-akzent' ] to deeply equal []“* |
> | 4 | ohne `aria-pressed:hover:bg-akzent/80` | der neue Fall |
> | 5 | shadcns `bg-accent` statt `bg-akzent` | der neue Fall, bei *an* |
> | 6 | ohne Kontur | der neue Fall, bei *an* |
> | 7 | ohne Schrift auf der Fläche | der neue Fall, bei *an* |
> | 8 | pausiert nicht eingeschaltet (`pressed` nur bei *an*) | drei Fälle: der neue, *„unterscheidet aus, an und pausiert …“* und *„pausiert auf Seite zwei, zeigt es am Schalter …“* |
> | 9 | der alte Stand, ohne die Klassen | der neue Fall, schon bei *aus* |
>
> > **Belegvermerk (L10).** *Gemessen war:* der Testlauf und die Proben, die Meldungen wörtlich aus
> > der Ausgabe, und das Stylesheet, das `next dev` für diesen Stand ausliefert — Reihenfolge und
> > Selektoren der Regeln abgelesen, abgerufen über die Anmeldeseite ohne Anmeldung. *Nicht
> > gemessen:* wie die Fläche im Browser aussieht, in beiden Blöcken, und ob das Überfahren dort
> > greift — `jsdom` rechnet kein CSS; dafür gibt es die Sichtprüfung beim Auftraggeber.
>
> *Nummer:* höchste vergebene **E‑216** (auf dem nicht gemergten `feat/nachrichtenliste-summe`),
> **E‑215** auf `fix/anmeldung-fokus`; gesucht nach dem Verfahren aus §1.

**Warum der Knopf je Ansicht einen anderen Namen trägt.** Er tut nicht überall dasselbe: In den
Nachrichten führt er zurück auf Seite eins, in der Prozessansicht holt er zwei Dinge. Ein Name, der
das sagt, ist die Auskunft, die der Nutzer vor dem Klick braucht — sichtbar bleibt überall dasselbe
kurze Wort.

> ⚠️ **Korrektur vom 16.09.2026, zweite Runde (E‑174).** Zeile *Sichtbar* der Tabelle: Der Knopf
> zeigt **nur die Pfeile**, kein Wort. Zeile *Maße*: `size="icon"` mit `min-h-bedienelement
> min-w-bedienelement` — quadratisch und so hoch wie ein Zeitraum-Knopf (M183‑1). Und der letzte Satz
> darüber heißt seither: **sichtbar bleibt überall dasselbe Symbol.** Am Schalter ändert sich nichts.

---

## 3. Verhalten je Ansicht

| | Übersicht | Nachrichten | Prozessansicht |
|---|---|---|---|
| **Ort** | Kopf, links neben dem Zeitraumumschalter | Filterleiste: Schalter, Knopf, Zeitraum — in **einer** Gruppe, die als Ganzes umbricht | Kopf, links neben dem Zeitraumumschalter |
| **Klick** | **eine** Anfrage an `/api/dashboard` mit denselben Parametern | **eine** Anfrage für Seite eins, ohne Cursor | Baum mit denselben Parametern, **danach** die Liste ab Seite eins, wenn ein Prozess gewählt ist |
| **Was stehen bleibt** | alle Blöcke, bis die Antwort da ist; kein Ladezustand, keine Bewegung (E‑170) | die angezeigte Seite, bis Seite eins da ist; das Panel und alles darin (E‑169) | Aufklappzustand, gewählter Prozess, geöffnete Nachricht, Scrollstände (M182) |
| **Automatisch** | nein (E‑137 unverändert) | Schalter, Vorgabe aus, 60 s, nur Seite eins, nur bei sichtbarem Tab | **nein, nicht mehr** (E‑164) |

> ⚠️ **Korrektur vom 16.09.2026, zweite Runde (E‑173), Zeile *Ort*:** Übersicht und Prozessansicht —
> Kopf, unmittelbar **rechts** neben dem letzten Zeitraum-Knopf. Nachrichten — **am rechten Rand der
> Filterleiste**, Schalter vor Knopf, als letzte Elemente der Leiste (`amRechtenRand`, `ml-auto`).
> In der Prozessansicht folgen im freien Modus Datumsfelder und Hinweis **hinter** dem Knopf.

> ⚠️ **Korrektur vom 16.09.2026, dritte Runde (E‑175):** Im freien Modus steht der Knopf **hinter**
> Datumsfeldern und Hinweis, ganz rechts; der Satz darüber gilt nicht mehr.

### Übersicht

`antwort.refetch({ cancelRefetch: false })` — derselbe Schlüssel, die Daten bleiben stehen, kein
Block fällt in den Ladezustand. **Der Knopf steht außerhalb der Zustandskette** wie der
Zeitraumumschalter und damit **auch im Leerzustand** (E‑p, ergänzt; Korrekturblock in
[`dashboard-frontend.md`](dashboard-frontend.md) §5.7).

**Die Bewegung ist nach dem ersten Aufbau aus, und zwar seit diesem Schritt** (E‑170). Nachgesehen
am Quelltext von Recharts 3.10.1 (`animation/AnimatedItems.js`): Jeder neue Satz Stützstellen
bekommt eine neue Animationskennung, und Recharts interpoliert vom vorigen Stand zum neuen. Mit
geänderten Zahlen hätte „Neu laden" also 600 ms gemorpht. Gebaut ist dagegen **eine reine Funktion
und ein Ereignis**: `aufbauAktiv(aufgebautFuer, zeitraum)` in `features/dashboard/verlauf.ts` liefert
`"auto"`, solange für diesen Zeitraum noch kein Aufbau zu Ende gelaufen ist, danach `false`; gesetzt
wird der Zeitraum in `onAnimationEnd` an Fläche und Balken, nie in einem Effekt. Ein
Zeitraumwechsel trifft einen anderen Wert und baut weiterhin auf (E‑86); `"auto"` behält Recharts'
eigenen Ausschalter für `prefers-reduced-motion`. **Belegt am Browser mit Gegenprobe** (§6, M182‑5).

### Nachrichten

**Auf Seite eins** holt `neuLaden` dieselbe Abfrage neu. **Auf einer späteren Seite wird zuerst
Seite eins geholt und erst danach gewechselt** — über `fetchQuery` mit `staleTime: 0`, damit eine
gehaltene ältere Seite eins nicht als Antwort durchgeht. So bleibt die vorhandene Seite stehen, bis
die neue da ist; der Nutzer sieht weder ein Skelett noch eine veraltete Seite eins, die gleich darauf
springt. Beim Wechsel ist die Antwort frisch, also geht keine zweite Anfrage hinaus. Hat der Nutzer
in der Zwischenzeit weitergeblättert oder den Filter geändert, bleibt seine neue Stelle stehen
(`stapelNachNeuLaden` vergleicht den Stapel selbst, nicht seine Länge).

**Der Schalter** hängt am Komponentenzustand und nicht an der URL
([`nachrichtenliste.md`](nachrichtenliste.md) §8.2). Seine Lage leitet `automatikzustand(an,
aufSeiteEins)` ab; das Intervall `aktualisierungsintervall({ an, aufSeiteEins, sichtbar })`. **Die
Standanzeige bleibt im Blätterblock** (E‑171), ebenso die Pfeile vor und zurück.

**Der Knopf „Jetzt aktualisieren" im Blätterblock ist entfallen** (E‑172). Er holte die *aktuelle*
Seite neu, auch Seite sieben, und damit genau den Cursor, den E‑168 vermeidet. „Wiederholen" im
Fehlerzustand bleibt; es ist ein anderer Vorgang.

### Prozessansicht

**Erst der Baum, dann die Liste.** Das Fenster der Liste kommt aus der Antwort des Baums (E‑50).
Rückt es beim Neuladen — ein relativer Zeitraum über eine volle Stunde —, ändert sich der Filter der
Liste, und sie beginnt **von selbst** auf Seite eins mit dem neuen Fenster. Gleichzeitig abgeschickt,
ginge vorher eine Anfrage mit dem alten Fenster hinaus, deren Antwort niemand mehr zeigte. Ist das
Fenster gleich geblieben, ruft die Ansicht `neuLaden` der Liste — die Liste reicht es über `ref`
herauf. **Getestet in beiden Richtungen** (§5).

**`lib/in-sicht-bringen.ts` springt nicht**: Seine Schlüssel sind `prozess` und `nachricht`, und
beide ändern sich nicht. Wechselt die Liste dabei von einer späteren Seite auf Seite eins, gilt
dasselbe wie beim Blättern (Punkt **161**), ohne Sonderweg. **Die Übertragungsliste aktualisiert
nicht mehr automatisch** — `useNachrichtenSeite(filter, false)`.

> **Bestätigt am 17.09.2026 (Live-Rest, Punkt 189 in [`live-rest.md`](live-rest.md)).** Der Auftrag zum
> Live-Rest wollte den Baum *im Takt der Übertragungsliste* nachladen, solange deren automatische
> Aktualisierung eingeschaltet ist — ein Takt, den es hier seit E‑164 nicht mehr gibt. Der
> Auftraggeber hat entschieden: **E‑164 gilt**, kein Nachladen in der Prozessansicht (E‑186 dort).
> Baum und Liste laden weiter nur von Hand, dann beide zusammen; der Block `liveRest` der Antwort
> kommt dabei mit.

---

## 4. Die Entscheidungen

| | Entscheidung | Herkunft |
|---|---|---|
| **E‑163** | ⚠️ ***Am selben Tag korrigiert durch E‑173.*** **„Neu laden" steht auf allen drei Seiten unmittelbar links neben den Zeitraum-Schaltflächen**, in deren Höhe und Innenabstand | Auftraggeber, Entscheidung 1 vom 16.09.2026 |
| **E‑164** | **Automatische Aktualisierung nur in den Nachrichten.** Übersicht und Prozessansicht laden nur von Hand; die Prozessansicht verliert ihre bisherige Aktualisierung | Auftraggeber, Entscheidung 2 |
| **E‑165** | **Keine Intervallwahl.** Es gelten weiter die Regeln aus [`nachrichtenliste.md`](nachrichtenliste.md) §8.3 | Auftraggeber, Entscheidung 3 |
| **E‑166** | ⚠️ ***Für den Knopf am selben Tag korrigiert durch E‑174.*** **Symbol und kurzes Wort** — „Neu laden", „Auto"; der vollständige Name als zugänglicher Name und Tooltip, in beiden Sprachdateien | Auftraggeber, Entscheidung 4 |
| **E‑167** | **Vom Lobster-Vorbild bleibt nur „Neu laden".** Keine Plus- und Minus-Schaltflächen, keine Sekundenanzeige, **kein Orange** (Schaltflächen tragen den Akzent, Orange ist `--ueberfaellig`), **kein drehendes Symbol** | Auftraggeber, Entscheidung 5 |
| **E‑168** | **„Neu laden" führt in einer Liste auf Seite eins** und setzt den Blätterstapel zurück. Die vorhandene Seite bleibt stehen, bis Seite eins da ist | Festlegung 6 des Auftrags, **bestätigt und präzisiert** (§7) |
| **E‑169** | **„Neu laden" holt nur die Hauptdaten der Ansicht**, nie das offene Panel und nichts darin | Festlegung 7, **bestätigt** |
| **E‑170** | **„Neu laden" auf der Übersicht ist kein Aufbau** im Sinne von E‑85: keine Bewegung, kein Morphing. Gebaut über `aufbauAktiv` | Festlegung 8, **bestätigt mit Befund** (§7) |
| **E‑171** | **Die Standanzeige bleibt im Blätterblock.** Der Schalter wandert nach oben, und der Hinweis, dass er pausiert, wandert mit — **in den Schalter**: Symbol, `title`, zugängliche Beschreibung | Festlegung 9, **bestätigt** |
| **E‑172** | **Der Knopf „Jetzt aktualisieren" im Blätterblock entfällt**, in Nachrichtenliste und Prozessansicht | Antwort des Auftraggebers vom 16.09.2026 auf die Rückfrage — der Auftrag nannte den Knopf nicht |
| **E‑173** | ⚠️ ***Für den freien Modus am selben Tag korrigiert durch E‑175.*** **„Neu laden" steht rechts.** Auf Übersicht und Prozessansicht unmittelbar rechts neben dem letzten Zeitraum-Knopf, in den Nachrichten Schalter und Knopf am rechten Rand der Filterleiste, der Knopf außen. **Korrigiert E‑163.** Im freien Modus der Prozessansicht bleibt der Knopf neben „Frei", die Datumsfelder folgen dahinter — das war nicht vorgegeben und ist bei der Umsetzung gewählt (§10) | Auftraggeber, 16.09.2026, nach der ersten Abnahme |
| **E‑174** | **Der Knopf zeigt kein Wort, nur das Symbol**; „Auto" am Schalter bleibt. Name und Tooltip je Ansicht unverändert. **Korrigiert E‑166 für den Knopf** | ebenda |
| **E‑175** | **Auch im freien Modus der Prozessansicht steht der Knopf ganz rechts**, hinter den Datumsfeldern und ihrem Hinweis. Umschalter, Felder und Knopf sind Kinder einer Reihe, die Felder stehen dadurch 0,5 statt 0,75 rem auseinander. Die Bedienreihe von Übersicht und Prozessansicht bleibt auch nach einem Umbruch unter die Überschrift rechts (`ml-auto`). **Korrigiert E‑173 für den freien Modus** | Auftraggeber, 16.09.2026, nach der zweiten Abnahme |

---

## 5. Tests

| Datei | Fälle | Was |
|---|---:|---|
| `tests/aktualisierung.test.ts` | 11 | **rein.** Das Intervall über alle acht Lagen von Schalter, Seite und Sichtbarkeit; 60 s ohne Wahl; aus/an/pausiert; `abfrageNachNeuLaden` ohne Cursor, mit allen Filtern, ohne `nachricht`; `stapelNachNeuLaden` setzt nur zurück, wenn niemand inzwischen blätterte; `aufbauAktiv` für erstes Bild, Neuladen und Zeitraumwechsel |
| `tests/neu-laden.test.tsx` | 15 | **gerendert**, zugesichert werden Anfragen und nie Zeiten (T1). Baustein ohne Schalter; drei Lagen über `aria-pressed` und drei Symbole, Klick beim Laden ruft nichts. Übersicht: genau eine weitere Anfrage, auch im Leerzustand. Nachrichten: Seite zwei → eine Anfrage ohne Cursor; bei offenem Panel keine an Detail- oder Dateiendpunkte. Gestellte Uhr: aus → nichts; an → nach 60 s eine; Seite zwei → pausiert und nichts, nach „Neu laden" wieder eine. Prozessansicht: zwei Minuten nichts; Klick → Baum, Liste; neues Fenster → keine Listenanfrage mit dem alten. **Seit E‑173 drei mehr** (bis dahin 12): Übersicht und Prozessansicht — der Knopf ist der nächste Knopf nach dem letzten Zeitraum-Knopf; Nachrichten — Schalter und Knopf sind die letzten beiden Knöpfe der Filterleiste, die Leiste über den Baum gesucht. Am Baustein zusätzlich **kein sichtbares Wort** und genau ein Symbol (E‑174). **Seit E‑175** prüft der Fall der Prozessansicht auch den freien Modus: Nach dem Klick auf „Frei" ist der Knopf das **letzte Bedienelement des Kopfes**, hinter beiden Datumsfeldern — keine Fallzahl mehr |

Der Zählkopf in `frontend/vitest.config.mts` ist fortgeschrieben: **152 gerenderte Fälle in zwanzig
Dateien, 1.107 im ganzen Lauf** — dort auch, dass der Kopf davor um eins zu hoch stand. **Fortgeschrieben
nach E‑173: 155 in zwanzig Dateien, 1.110 im ganzen Lauf**; je Datei verglichen, geändert hat sich
allein `tests/neu-laden.test.tsx`.

### Die Verletzungsproben — ausgeführt, zurückgenommen, in keinem Commit

Zurückgespielt jeweils aus einer Sicherungskopie und mit `cmp` gegen sie verglichen, **nicht** mit
`git checkout`.

| Probe | Eingriff | Ergebnis |
|---|---|---|
| **(a)** „nur Seite eins" ausgehängt, an der Aufrufstelle | `aufSeiteEins: true` in `useNachrichtenSeite` | **rot**, genau ein Fall: *„pausiert auf Seite zwei, zeigt es am Schalter und fragt nicht"* — `AssertionError: expected [ …(2) ] to deeply equal []`, empfangen zweimal `"/api/nachrichten?cursor=cursor-2"`. Die reine Funktion blieb dabei grün — die Probe trifft die **Verdrahtung** |
| **(a′)** dieselbe Bedingung in der reinen Funktion | `lage.an && lage.sichtbar` | **rot**, zwei Fälle: *„an=true aufSeiteEins=false sichtbar=true: expected 60000 to be false"* und derselbe gerenderte Fall wie in (a) |
| **(b)** das Panel beim Neuladen mitholen | `invalidateQueries` auf `NACHRICHTEN_SCHLUESSEL.detail(gewaehlt)` im Klick | **rot**, genau ein Fall: *„fragt bei offenem Panel keinen Detail- oder Dateiendpunkt an"* — `expected [ '/api/nachrichten', …(1) ] to deeply equal [ '/api/nachrichten' ]`, zusätzlich empfangen `"/api/nachrichten/8f3a1c2e-0000-4000-8000-000000000001"` |
| Eichung Prozessansicht | `useNachrichtenSeite(filter, true)` | **rot**: *„aktualisiert ohne Klick nichts, auch nicht nach zwei Minuten"*, empfangen zweimal die Listenanfrage |
| **(c)** die Stelle von E‑163 zurück | Knopf vor den Umschalter (Übersicht, Prozessansicht), Schalter und Knopf vor das Zeitfenster, das Wort „Neu laden" zurück in den Knopf | **rot**, vier Fälle: *„steht unmittelbar rechts neben dem letzten Zeitraum-Knopf"* (Übersicht und Prozessansicht) und *„stellt Schalter und Knopf als letzte in die Filterleiste, den Knopf außen"* je `expected <button …> to be <button data-slot="button" …>`; am Baustein `expected 'Neu laden' to be ''`. Die übrigen elf Fälle blieben grün |
| **(d)** der freie Modus von E‑173 zurück | Knopf wieder unmittelbar hinter den Umschalter, die Felder dahinter | **rot**, genau ein Fall: *„steht rechts neben dem letzten Zeitraum-Knopf und im freien Modus hinter den Datumsfeldern"* — `im freien Modus: expected <input data-slot="input" …(5)></input> to be <button data-slot="button" …(7)>…(1)</button>`; die übrigen 14 grün. Gefahren gegen den ersten Umbau mit eigener Gruppe für Umschalter und Felder; der Fall ist seither unverändert |
| Eichung Reihenfolge | Liste immer neu laden, ohne Fenstervergleich | **rot**: *„fragt die Liste nicht mit dem alten Fenster an …"*, zusätzlich empfangen die Listenanfrage mit `05:00`-Fenster |

> **Belegvermerk (L10).** *Gemessen war:* fünf Läufe, je mit genau einem Eingriff, und die
> Meldungen oben wörtlich aus der Ausgabe. *Behauptet wird:* Die beiden verlangten Proben (a) und (b)
> werden rot, und zwar am jeweils gemeinten Fall. — **Nicht behauptet** ist, dass jede denkbare
> Verletzung rot wird: Ein `invalidateQueries` auf den ganzen Präfix `["nachrichten"]` ist nicht
> gefahren. **Zu (c):** *gemessen war* **ein** Lauf mit allen vier Eingriffen zugleich, zurückgespielt
> aus Sicherungskopien und mit `cmp` verglichen. Jeder rote Fall gehört zu genau einem Eingriff; dass
> jeder Eingriff **allein** seinen Fall rot macht, ist daraus geschlossen und nicht einzeln gefahren.
> **Zu (d):** ein Lauf mit genau einem Eingriff.

---

## 6. Messungen

### M181 — Verschiebt ein automatischer Abruf den Sitzungsablauf?

**Rahmen:** Profil `dev`, Mandant `NEXANS`, eigenes Chrome mit Debug-Port und frischem Profil,
**angemeldet vom Nutzer**; gelesen mit dem Lesenutzer ausschließlich `SELECT` auf
`overlord_monitor.SPRING_SESSION`. Die Sitzung ist über `PRINCIPAL_NAME` des angemeldeten Kontos
und den jüngsten `LAST_ACCESS_TIME` gewählt; **die Sitzungskennung ist weder gelesen noch
ausgegeben**. Zum Zeitpunkt der Messung standen drei Sitzungen dieses Kontos in der Tabelle.

| Zeitpunkt (UTC) | Vorgang | `LAST_ACCESS_TIME` | `EXPIRY_TIME` |
|---|---|---|---|
| 13:25:11.084 | Schalter an, Seite eins | | |
| 13:25:52.147 | gelesen — **keine** Anfrage der Seite seit dem Einschalten (mitgeschrieben) | 13:25:02.674 | 13:55:02.674 |
| 13:26:11.091 | automatischer Abruf `GET /api/nachrichten` (60,007 s nach dem Einschalten) | | |
| 13:26:31.284 | gelesen | **13:26:11.104** | **13:56:11.104** |

`MAX_INACTIVE_INTERVAL` 1800 s in beiden Lesungen.

> **Belegvermerk (L10).** *Gemessen war:* `LAST_ACCESS_TIME` und `EXPIRY_TIME` der jüngsten Sitzung
> des angemeldeten Kontos vor und nach genau einem automatischen Abruf, ohne andere Anfrage der Seite
> dazwischen; der neue Wert liegt **13 ms** hinter dem Anfragezeitpunkt, der Ablauf rückt um
> **68,430 s**. *Behauptet wird:* **Ein automatischer Abruf verlängert die Sitzung** wie jede andere
> Anfrage. — **Nicht gemessen** ist, dass die gelesene Zeile die Sitzung dieses Fensters ist: Die
> Zuordnung hängt an der Zeitgleichheit, nicht an der Kennung, und es gab drei Sitzungen dieses
> Kontos. **Nicht gemessen** ist auch das Verhalten in Produktion; der Codepfad ist derselbe
> (Spring Session JDBC, [`authentifizierung.md`](authentifizierung.md) §5).

**Die Folge, als offener Punkt 184 geführt und nicht behoben:** Ein Tab, der sichtbar auf Seite eins
steht und den Schalter an hat, läuft **nie** ab — alle 60 Sekunden rückt der Ablauf um eine Minute.
Die Regel „30 Minuten ohne Aktivität" gilt für diesen Fall nicht mehr. Pausiert (Seite zwei) und
verdeckt fragt die Liste nicht, dort läuft die Sitzung wie bisher ab.

### M182 — die Sichtprüfung am laufenden System

**Rahmen:** wie M181; `next dev` des Zweigs auf `:3001` gegen das Backend auf `:8080`, Maße über
`Emulation.setDeviceMetricsOverride`, Anfragen über `Network.requestWillBeSent` (die Quelle des
Netzwerk-Tabs), Bilder je Bild über `requestAnimationFrame`. **Nicht kopflos**: Anmelden darf nur der
Nutzer. Das Fenster lag zeitweise verdeckt; **jeder zeitabhängige Lauf trägt ein
Sichtbarkeitsprotokoll**, und zwei Läufe, in denen es verdeckt wurde, zählen nicht (M182‑6).

**M182‑1 — Lage, 24 Kombinationen** (Übersicht, Nachrichten, Prozessansicht × 1280 und 1920 px ×
`xs` und `l` × hell und dunkel): In **allen 24** ist der Knopf so hoch wie der erste Knopf des
Zeitraumumschalters (**28 px** bei `xs`, **36 px** bei `l`), seine Oberkante liegt auf derselben
Höhe, und der Abstand zum Umschalter ist **0,5 rem** (7 bzw. 9 px). In den Nachrichten gilt dasselbe
für den Schalter vor dem Knopf. `data-dichte` und die Farben des Knopfes wechseln mit Cookie und
Thema (hell `lab(98.26 0 0)`, dunkel `oklab(… / 0.048)` als Hintergrund).

**M182‑2 — unter `xl` bei offenem Panel, Nachrichten:**

| Breite | ohne Panel | mit Panel |
|---:|---|---|
| 1024 | Schalter und Knopf sichtbar | **beide nicht sichtbar** (`checkVisibility()` falsch, Breite 0) |
| 1279 | sichtbar | **beide nicht sichtbar** |
| 1280 | sichtbar | sichtbar; die Leiste bricht um — Schalter, Knopf, Zeitraum in Zeile 1, Status in Zeile 2 |
| 1920 | sichtbar | sichtbar, eine Zeile |

Unter `xl` weicht die **ganze** linke Spalte dem Panel (`hidden xl:flex`), mit ihr Filterleiste,
Schalter und Knopf. **Kein Sonderweg gebaut**; Punkt 121 bleibt für `/nachrichten` offen (§8).

**M182‑3 — Anfragen je Klick** (1920 px, `NEXANS`):

| Ansicht, Lage | nach genau einem Klick |
|---|---|
| Übersicht, `?zeitraum=30T` | `/api/dashboard?zeitraum=30T` — **eine** |
| Nachrichten, Panel offen, Seite zwei | `/api/nachrichten` — **eine**, ohne Cursor. Danach ist „Vorherige Seite" gesperrt. **Eichung:** Beim Laden hatte das Panel `/api/nachrichten/<id>` und `/api/nachrichten/<id>/dateien` gefragt, die Seite `/api/auth/me`, `/api/mandanten`, `/api/bam/suchfelder`, `/api/prozesse` |
| Prozessansicht, `12M`, `90300_SAP_KOMMUNIKATION`, Nachricht offen, Seite zwei | `/api/prozesse/baum?zeitraum=12M`, dann `/api/nachrichten?von=…&bis=…&prozess=90300_SAP_KOMMUNIKATION` — **zwei**, ohne Cursor. Adresse vorher und nachher gleich (`prozess` und `nachricht` stehen), aufgeklappte Knoten **1 → 1** |

**M182‑4 — Scrollstände in der Prozessansicht:** `main` auf 600 px, die klebende Spalte auf 200 px
gesetzt, dann „Neu laden": **600 und 200** danach.

**M182‑5 — Bewegung auf der Übersicht**, mit gestellter Antwort (`window.fetch` im Fenster
ersetzt, jede zweite Stützstelle halbiert bzw. geviertelt — lokal ändern sich die Zahlen nicht):

| Vorgang | Bilder | Stände der Fläche (`d`) | Stände der Balken | Bilder mit Skelett |
|---|---:|---:|---:|---:|
| **Neu laden** | 442 | **2** | **2** | 0 |
| Zeitraumwechsel 30 T → 12 M (Eichung) | 446 | 3 | **47** | 25 |
| **Neu laden** nach dem Wechsel | 448 | **2** | **2** | 0 |
| *Gegenprobe ohne Sperre* — Neu laden | 443 | **47** | **46** | 0 |
| *Gegenprobe ohne Sperre* — Neu laden nach dem Wechsel | 447 | **46** | **46** | 0 |

Zwei Stände heißen: vorher und nachher, **kein Zwischenbild**. Die Fläche baut beim Zeitraumwechsel
über ihren Clip auf und nicht über `d`, deshalb dort nur 3 — die Balken zeigen, dass der Abtaster
Bewegung sieht. Die Gegenprobe (`isAnimationActive="auto"` statt `aufbau`) ist zurückgenommen.

**M182‑6 — die automatische Aktualisierung, mit Sichtbarkeitsprotokoll:**

| Lauf | sichtbar | Anfragen |
|---|---|---|
| Nachrichten, Seite eins, Schalter an um 13:44:40.193, 185 s | durchgehend | `/api/nachrichten` um 13:45:40.204, 13:46:40.261, 13:47:40.320 — **eine je Minute** |
| Seite zwei, Schalter an, 125 s (13:54:32–13:56:37) | bis auf 1,3 s (13:55:47–13:55:48); beide fälligen Zeitpunkte sichtbar | **keine**; der Schalter zeigt das Pause-Symbol |
| „Neu laden" um 13:56:37.101, dann 62 s | durchgehend | `/api/nachrichten` sofort, **eine automatische** um 13:57:37.219; Uhr-Symbol |
| Prozessansicht, `12M`, Prozess gewählt, 125 s ohne Klick | durchgehend | **keine**; kein Schalter im Baum |
| *nicht gewertet:* Nachrichten, Schalter an 13:31:14, 185 s | verdeckt ab 13:31:50 | keine — richtig für einen verdeckten Tab, aber kein Beleg für „eine je Minute" |
| *nicht gewertet:* Prozessansicht ab 13:34:27, 125 s | durchgehend verdeckt | keine — sagt nichts |

Nach dem Laden stand der Schalter auf **aus** (`aria-pressed="false"`, Uhr durchgestrichen).

**M182‑7 — was ein Vorleseprogramm bekommt** (Barrierefreiheitsbaum von Chromium, kein
Vorleseprogramm): Rolle `button`, Name „Automatische Aktualisierung", `pressed` **false**, nach einem
Klick **true** mit der Beschreibung „Automatische Aktualisierung: an. …"; Rolle `button`, Name
„Nachrichten neu laden, ab Seite eins", **Beschreibung derselbe Satz** (Punkt 185).

> **Belegvermerk (L10) zu M182.** *Gemessen war:* Maße aus `getBoundingClientRect` in 24 Lagen und
> an vier Breiten mit und ohne Panel; die URLs aus `Network.requestWillBeSent` je Klick; Scrollstände
> vor und nach; Attributwerte `d` je Bild über 6 s; Anfragezeitpunkte und Sichtbarkeitswechsel über
> drei und zwei Minuten; der Barrierefreiheitsbaum. *Behauptet wird:* Der Knopf steht auf allen
> drei Seiten unmittelbar links neben den Zeitraum-Schaltflächen und ist so hoch wie sie; ein Klick
> ergibt genau die Anfragen aus §3; die Übersicht bewegt sich beim Neuladen nicht, ohne die Sperre
> schon; die Aktualisierung fragt je Minute auf Seite eins, auf Seite zwei nicht, die
> Prozessansicht gar nicht. — **Nicht gemessen:** das Bild selbst (Farben, Symbole) ist nur an vier
> Ausschnitten **angesehen**; die Anfragen **auf dem Server** sind nicht gezählt, nur die aus dem
> Browser; neue Zeilen erscheinen lokal nicht, weil die Testkopie keine bekommt.

### M183 — die Stelle nach E‑173 und E‑174

**Rahmen:** wie M182, dasselbe Chrome, vom Nutzer neu angemeldet; Mandant `NEXANS` über
`POST /api/auth/mandant` gewählt. Maße über `getBoundingClientRect`, Bildausschnitte über
`Page.captureScreenshot`. **Nur Maße, keine Zeiten.** M182‑1 bleibt als Messung des damaligen Stands
stehen; seine Aussage zur Lage gilt seit E‑173 nicht mehr.

**M183‑1 — 24 Lagen** (drei Seiten × 1280 und 1920 px × `xs` und `l` × hell und dunkel). In **allen
24** ist der Knopf **quadratisch** und so hoch wie der erste Zeitraum-Knopf — **28 × 28 px** bei `xs`,
**36 × 36 px** bei `l` —, sein sichtbarer Text ist leer, und er trägt genau ein Symbol. Hintergrund
hell `lab(98.26 0 0)`, dunkel `oklab(… / 0.048)`.

| Seite | gemessen in allen acht Lagen der Seite |
|---|---|
| **Übersicht** | Der Knopf ist der **nächste Knopf nach „12 Monate"**, Abstand **0,5 rem** (7 bzw. 9 px), Oberkante gleich; rechte Kante **auf der rechten Kante des Kopfes** (Abstand 0) |
| **Prozessansicht** | ebenso, nach **„Frei"** |
| **Nachrichten** | Rechte Kante des Knopfes = rechte Kante der Filterleiste = rechte Kante der Tabelle (Abstand je **0**); „Auto" **0,5 rem** davor, gleich hoch, gleiche Oberkante. **Bei 1280 px** stehen beide in der **zweiten Zeile** der Leiste (Leiste 82,25 px hoch bei `xs`, 105,75 px bei `l` — je zwei Zeilen; bei `xs` im Bild angesehen: Zeitfenster, Status, Prozess und Suche in der ersten, Schalter und Knopf allein in der zweiten), **bei 1920 px** in der Zeile des Zeitfensters (47,25 bzw. 60,75 px) |

**M183‑2 — Nachrichten mit offenem Panel** (Dichte `m`, hell):

| Breite | Schalter und Knopf |
|---:|---|
| 1024 | **nicht sichtbar** (`checkVisibility()` falsch) — wie M182‑2 |
| 1279 | **nicht sichtbar** |
| 1280 | sichtbar, rechte Kante auf der Leiste (Abstand 0), 102 px unter der Oberkante des Zeitfensters — im Bild die **dritte** Zeile der Leiste |
| 1920 | sichtbar, Abstand 0, 62 px unter dem Zeitfenster — die zweite Zeile, aus dem Versatz geschlossen und nicht im Bild angesehen |

**M183‑3 — freier Modus der Prozessansicht** (1920 px, `m`): linke Kante des Knopfes vor dem Klick auf
„Frei" bei **1.853 px**, danach bei **1.401,13 px**; beide Datumsfelder stehen **rechts** davon. Der
Knopf bleibt neben „Frei" und rückt mit dem Umschalter nach links; die rechte Kante des Kopfes gehört
dann den Feldern. Bei 1280 px im Bild angesehen.

**M183‑4 — Berührungsgerät** (`Emulation.setTouchEmulationEnabled`, `matchMedia('(pointer:
coarse)')` wahr; Übersicht, 1280 px): Knopf **44 × 44 px**, Zeitraum-Knopf 44 px hoch.

> **Belegvermerk (L10) zu M183.** *Gemessen war:* Maße aus `getBoundingClientRect` in 24 Lagen, an
> vier Breiten mit offenem Panel, vor und nach „Frei" und unter `pointer: coarse`; sichtbarer Text und
> Zahl der Symbole des Knopfes; die Reihenfolge der Knöpfe im Dokument. *Behauptet wird:* Auf
> Übersicht und Prozessansicht steht der Knopf unmittelbar rechts neben dem letzten Zeitraum-Knopf
> und rechts außen im Kopf, in den Nachrichten mit dem Schalter am rechten Rand der Leiste; er zeigt
> kein Wort und ist quadratisch. — **Angesehen, nicht gemessen:** fünf Bildausschnitte (Übersicht,
> Nachrichten mit und ohne Panel, Prozessansicht mit und ohne freien Modus). **Nicht erneut
> gemessen:** Anfragen je Klick und automatische Aktualisierung — am Verhalten ist nichts geändert,
> und die Tests darüber sind unverändert grün.

### M184 — der freie Modus nach E‑175

**Rahmen:** wie M183, dieselbe Sitzung. Je Lage: Seite laden, nach oben scrollen, messen, auf „Frei"
klicken, nach 1,2 s noch einmal messen. **Nur Maße.**

**M184‑0 — zwei Fassungen davor, verworfen.** Der erste Umbau stellte Umschalter und Felder in eine
eigene Gruppe und den Knopf dahinter. Ohne `ml-auto` stand die Reihe bei 1280 px in Dichte `l` nach
dem Klick unter der Überschrift **links**, der Knopf **65,16 px** vor dem Rand. Mit `ml-auto`, aber
noch mit eigener Gruppe, brach bei 1024 px in Dichte `m` „Bis" um, und der Knopf stand **allein in
einer dritten Zeile** (im Bild angesehen). Beides ist behoben; die Zahlen unten gelten für die
gebaute Fassung.

**M184‑1 — 18 Lagen** (Prozessansicht und Nachrichten × 1024, 1280, 1920 px × `xs`, `m`, `l`). In
**allen 18** ist „Frei" danach gedrückt, zwei Datumsfelder sind sichtbar, und der Knopf steht **vor
und nach dem Klick am rechten Rand** (Abstand 0 zur Kante des Kopfes bzw. der Leiste), mit
**derselben linken Kante**, als **letztes Bedienelement**; beide Felder liegen links von ihm. In der
Prozessansicht ist der Abstand zum letzten Zeitraum-Knopf ohne freies Fenster **0,5 rem** (7, 8,
9 px), im freien Modus zum letzten Feld ebenso, wo keine Zeile umbricht.

| Prozessansicht | was umbricht |
|---|---|
| 1920 px in allen Dichten; 1280 px in `xs` und `m` | nichts — eine Zeile |
| 1280 px in `l`; 1024 px in `xs` | die Reihe unter die Überschrift, **rechts** ausgerichtet (im Bild angesehen bei 1280 px, `l`) |
| 1024 px in `m` und `l` | zusätzlich „Bis" in die nächste Zeile; der Knopf steht **in dessen Zeile am rechten Rand** (im Bild angesehen bei `m`) |

In den Nachrichten rückt der Knopf bei 1024 und 1280 px beim Klick **nach unten**, weil die Felder
unter dem Zeitfenster die erste Zeile höher machen; waagrecht bleibt er am Rand. Bei 1920 px bewegt er
sich gar nicht.

**M184‑2 — Übersicht an schmalen Breiten** (Dichte `m`): Bei **360** und **400 px** bricht die Reihe
unter die Überschrift und steht **rechts** (Abstand 0), der Knopf in der Zeile der Zeitraum-Knöpfe;
bei 768, 1024 und 1280 px eine Zeile, Abstand 0. Im Bild angesehen bei 400 px.

> **Belegvermerk (L10) zu M184.** *Gemessen war:* Maße aus `getBoundingClientRect` vor und nach dem
> Klick auf „Frei" in 18 Lagen, dazu die Übersicht an fünf Breiten; die Reihenfolge der
> Bedienelemente im Dokument. *Behauptet wird:* Der Knopf steht auf Übersicht und Prozessansicht
> auch im freien Modus und nach jedem gemessenen Umbruch ganz rechts, und in den Nachrichten ändert
> der freie Modus daran nichts. — **Angesehen, nicht gemessen:** drei Bildausschnitte. **Nicht
> gemessen:** der Hinweis unter den Feldern (erscheint nur bei halber Eingabe oder Fehler) und Breiten
> unter 1024 px in der Prozessansicht.

---

## 7. Die Festlegungen 6 bis 9 des Auftrags

| | Stand |
|---|---|
| **6** — Seite eins, wie eine Filteränderung | **Bestätigt, mit einer Präzisierung.** Der Stapel wird zurückgesetzt, die Anfrage trägt keinen Cursor (M182‑3). **In der Anzeige ist es keine Filteränderung:** Eine Filteränderung zeigt das Skelett; „Neu laden" hält die vorhandene Seite, bis Seite eins da ist — sonst verletzte es Punkt 6 der Zustände im Auftrag (*„Eine vorhandene Antwort bleibt stehen, bis die neue da ist"*). Das ist eine Auflösung zwischen zwei Sätzen desselben Auftrags und steht deshalb hier |
| **7** — nur die Hauptdaten, nie das Panel | **Bestätigt**, im Test mit Verletzungsprobe (b) und am laufenden System (M182‑3): Das Panel hat beim Laden gefragt, beim Neuladen nicht |
| **8** — auf der Übersicht kein Aufbau | **Bestätigt, mit Befund:** Es war **nicht** von selbst so. Recharts hätte bei geänderten Zahlen gemorpht (Quelltext und Gegenprobe, M182‑5). Gebaut ist `aufbauAktiv` (E‑170) |
| **9** — Stand bleibt, Schalter und Pausenhinweis wandern | **Bestätigt.** Der Satz *„Pausiert, solange geblättert wird."* ist als sichtbarer Text entfallen und steht jetzt **im** Schalter: Pause-Symbol, `title` und zugängliche Beschreibung nennen den Grund. Sichtbar bleibt „Auto" |

---

## 8. Regelbezug

| Regel | Wortlaut ([`DEVELOPMENT_GUIDELINES.md`](../DEVELOPMENT_GUIDELINES.md)) | wie sie hier greift |
|---|---|---|
| **L3** | *„Keine `OFFSET`-Paginierung. Cursor-basiert über `(MessageLastUpdate, MessageID)`."* | Unverändert. „Neu laden" wirft den Cursor weg, statt ihn neu zu schicken — genau deshalb, weil ein Cursor eines relativen Fensters aus dem Fenster fallen kann (E‑168) |
| **L10** | *„Jeder Befundsatz trägt zwei Zeilen: was gemessen wurde und was behauptet wird. Sind sie identisch, steht das da und kostet eine Zeile. Weichen sie ab, ist die Abweichung der Inhalt des Vermerks — sie ist der Schluss, den der Satz zieht, und genau der ist nicht gemessen."* | Vermerke bei den Verletzungsproben, M181 und M182 |
| **Q4** | *„Nicht zugeordnet heißt „nicht zugeordnet". Partner, Standort, Richtung und Belegart werden kuratiert, nicht geparst. Eine Heuristik befüllt vor, die Wahrheit steht im `process_catalog`. Nicht zugeordnete Prozesse erscheinen in Auswertungen sichtbar als „nicht zugeordnet" — niemals als geratener Wert, niemals stillschweigend verteilt."* | Nicht berührt: kein Wert wird abgeleitet, keine Beschriftung geraten. Die Namen der Knöpfe beschreiben, was die Ansicht tut, nicht die Daten |
| **T1** | *„Wo eine Laufzeiteigenschaft geprüft werden soll, wird die Ursache geprüft und nicht die Uhr: die Zahl der Datenbankzugriffe, der `EXPLAIN`-Plan, der Treiberindex, der abgesetzte Statement-Text. Eine Dauer darf gemessen und ausgegeben werden; in eine Zusicherung gehört sie nicht."* | Die Tests sichern **Anfragen** zu; die gestellte Uhr ersetzt das Warten und wird nie gemessen. Im Browser sind Zeitpunkte **ausgegeben**, zugesichert ist nichts |
| **V1** | ***Nicht in `DEVELOPMENT_GUIDELINES.md` geführt*** — gesucht nach `V1` und nach dem Wortlaut; derselbe Befund steht schon in [`dashboard.md`](dashboard.md) §10 (Regelbezug zu E‑161). Die Kurzfassung von dort: *„Was ein Dokument über den Code sagt, wird am Code nachgesehen und nicht übernommen."* | **Angewandt:** Der Auftrag setzte voraus, die Bewegung sei nach dem ersten Aufbau schon aus — am Quelltext von Recharts nachgesehen, war sie es nicht. Die Verbraucher von `Blaettern` und `useNachrichtenSeite` sind im Code gesucht, nicht aus der Doku übernommen. **Die fehlende Fundstelle ist gemeldet und nicht still ergänzt** |

---

## 9. Offene Punkte

| Nr. | Punkt |
|---|---|
| **121** | *Fortgeschrieben, Stand in [`process-view.md`](process-view.md) §13.* **Für `/prozesse` erledigt** — ohne automatische Aktualisierung fragt die verdeckte Liste nichts ab (E‑164). **Für `/nachrichten` offen:** Der Schalter ist in den Kopf der Ansicht gewandert, aber unter `xl` bei offenem Panel weicht die ganze linke Spalte samt Kopf, und dort sind Schalter und Knopf nicht zu sehen (M182‑2). Ist der Schalter an, aktualisiert die verdeckte Liste weiter — am Code abgelesen (`useSichtbar` fragt nur die Registerkarte), nicht gemessen. Der zweite Ausweg aus dem Punkt — die Sichtbarkeit über einen `IntersectionObserver` — ist ausdrücklich nicht gebaut |
| **168** | *Fortgeschrieben, Stand in [`dashboard-frontend.md`](dashboard-frontend.md) §9.* Die Übersicht hat jetzt **einen sichtbaren Weg** zum neuen Stand; ein Takt ist weiterhin nicht gebaut und nach E‑164 auch nicht vorgesehen |
| **184** | **Ein automatischer Abruf verlängert die Sitzung** (M181). Ein sichtbarer Tab auf Seite eins mit eingeschaltetem Schalter läuft nie ab; die Regel „30 Minuten ohne Aktivität" ([`authentifizierung.md`](authentifizierung.md) §5) gilt für ihn nicht. **Nichts dagegen gebaut.** Wer den Punkt aufmacht, entscheidet, ob ein automatischer Abruf als Aktivität zählen soll — und damit über eine Unterscheidung, die heute weder Frontend noch Backend trifft |
| **185** | **Der Knopf trägt Namen und gleichlautende Beschreibung** (M182‑7): Chromium stellt das `title` neben das `aria-label`. Ob ein Vorleseprogramm den Satz zweimal ansagt, ist **nicht geprüft**. Dazu: Der Tooltip ist ein `title` — wie überall im Projekt — und erscheint deshalb nur unter der Maus, nicht beim Fokus mit der Tastatur. **Fortgeschrieben am selben Tag (E‑174):** Ohne Wort ist der Tooltip die **einzige sichtbare Beschriftung** des Knopfes. Wer mit der Tastatur arbeitet und nichts vorlesen lässt, sieht beim Fokus nur die Pfeile |

---

## 10. Abweichungen vom Auftrag

1. **Basis und Arbeitsort.** Der Zweig ist von `fix/dashboard-verteilung-beide-sichten` (`01a6db2`)
   abgezweigt und nicht von `main` nach dessen Merge — der Merge stand aus. Gearbeitet ist in einem
   eigenen `git worktree`, weil im Hauptarbeitsbaum eine andere Sitzung unversionierte Änderungen
   hielt. **Beides auf Rückfrage entschieden.**
2. **E‑172** — der Knopf „Jetzt aktualisieren" entfällt. Der Auftrag nannte ihn nicht; **auf
   Rückfrage entschieden**.
3. **E‑170 ist gebaut und nicht nur nachgewiesen.** Der Auftrag verlangte einen *Nachweis am Code,
   wodurch die Bewegung nach dem ersten Aufbau aus ist*; es gab keinen solchen Mechanismus.
4. **E‑168 präzisiert** (§7, Festlegung 6): die vorhandene Seite bleibt stehen, bis Seite eins da ist.
5. **Die Prozessansicht lädt nacheinander**, nicht gleichzeitig (§3) — eine Anfrage weniger, wenn
   das Fenster rückt.
6. **V1** ist nicht in den Richtlinien geführt (§8).
7. **Die reinen Tests heißen `tests/aktualisierung.test.ts`** nach dem Modul, die gerenderten
   `tests/neu-laden.test.tsx`.
8. **Der Zählkopf** in `vitest.config.mts` stand am Ausgangsstand um eins zu hoch (1.081 statt
   1.080) — berichtigt und dort vermerkt.
9. **Die Sichtprüfung lief nicht kopflos**, sondern in einem eigenen Chrome mit Debug-Port, in dem
   sich der Nutzer angemeldet hat; der Netzwerk-Tab ist über dieselbe Quelle ausgelesen und nicht von
   Hand angesehen.
10. **E‑173 im freien Modus der Prozessansicht.** Die Vorgabe *„ganz nach rechts neben den letzten
    Zeitraum-Knopf"* trifft zwei Stellen, sobald Datumsfelder erscheinen. Gewählt ist **neben
    „Frei"**: Die Felder bringen ihren Hinweis als eigenes Element der Zeile mit, und der stünde sonst
    zwischen Feldern und Knopf. Der Knopf steht damit im freien Modus nicht rechts außen (M183‑3).
    ⚠️ **Am selben Tag vom Auftraggeber anders entschieden (E‑175):** auch im freien Modus ganz
    rechts; der Hinweis steht seither zwischen Feldern und Knopf, wenn er erscheint.
11. **Verletzungsprobe (c)** lief als **ein** Lauf mit vier Eingriffen zugleich, nicht vier Läufe
    (§5).

---

## 11. Was nicht gebaut und was nicht gezeigt ist

**Nicht gebaut**, wie abgegrenzt: keine Intervallwahl, keine Plus- und Minus-Schaltflächen, keine
Sekundenanzeige; keine automatische Aktualisierung auf Übersicht und Prozessansicht; kein Neuladen
von Panel, Rohdaten, Rahmen oder Stammdaten; keine Tastenkürzel; der Schalter wird weder über
Seitenwechsel noch im Cookie gemerkt; kein `IntersectionObserver`; kein Sonderweg für Punkt 161;
keine Maßnahme gegen Punkt 184; kein Backend, kein Endpunkt, keine Migration; die Standanzeige ist
nicht verlegt.

**Nicht gezeigt:**

- **Kein Vorleseprogramm** — nur der Barrierefreiheitsbaum von Chromium (M182‑7).
- **Das schmale Fenster** unter 1024 px und das **Berührungsgerät** (`pointer: coarse`): Die Maße
  hängen an `min-h-bedienelement` wie beim Zeitraumumschalter; angesehen ist es nicht. *Seit M183‑4:*
  am Berührungsgerät gemessen, 44 × 44 px auf der Übersicht; das schmale Fenster weiterhin nicht.
  *Seit M184‑2:* die Übersicht bei 360 und 400 px gemessen; Nachrichten und Prozessansicht unter
  1024 px nicht.
- **Die englischen Texte** im Browser — sie sind nur über `tests/sprachdateien.test.ts` belegt.
- **Der Tooltip** selbst: Ein `title` lässt sich synthetisch nicht einblenden.
- **Neue Zeilen** nach dem Neuladen — die Testkopie bekommt keine; die Wirkung ist über Anfragen und
  eine gestellte Antwort gezeigt.
- **Das rückende Fenster** der Prozessansicht (volle Stunde zwischen zwei Baumabfragen) nur im Test,
  nicht am laufenden System.
- **`prefers-reduced-motion`** beim Neuladen: `aufbauAktiv` liefert beim ersten Bild `"auto"`, und
  Recharts schaltet den Aufbau dann selbst ab; nachgemessen ist das in dieser Runde nicht.
- **Der Mandantenwechsel** leert weiterhin den Zwischenspeicher
  ([`frontend-grundlagen.md`](frontend-grundlagen.md) §5); daran ist nichts geändert und nichts neu
  geprüft.
