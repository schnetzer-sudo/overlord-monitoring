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
| `--status-offen` | wartend, laufend, Zwischenschritt | Neutral | Kein Ergebnis, kein Problem. Farbe wäre hier eine Aussage, die es nicht gibt — `SPLITTED` und `MERGED` machen zusammen rund ein Drittel aller Zeilen aus. |
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

### Die Maße

| Token | Zeigergerät | Berührungsgerät | Warum |
|---|---|---|---|
| `--dichte-beruehrung` | 2.75 rem (44 px) | 2.75 rem | Mindestfläche am Finger. Wird **nirgends** unterschritten. |
| `--dichte-bedienelement` | 2 rem (32 px) | → `beruehrung` | Schaltfläche im Rahmen: Menü, Mandant, Sprache, Nutzermenü |
| `--dichte-navzeile` | 2.125 rem (34 px) | → `beruehrung` | Navigationseintrag, vorher 44 px |
| `--dichte-kopfzeile` | 3.125 rem (50 px) | 3.5 rem | Kopfzeile, vorher 56 px |
| `--dichte-navspalte` | 13 rem (208 px) | 13 rem | Navigationsspalte einschließlich Innenabstand |
| `--dichte-suchbereich` | 18 rem (288 px) | 18 rem | reservierter Platz für die BAM-Suche, Schritt 7 |
| `--dichte-feld` | 2.5 rem | 2.5 rem | Eingabefeld im Formular — bleibt bewusst komfortabel |
| `--dichte-zeile` | 2.25 rem | 2.25 rem | Tabellenzeile ab Schritt 4 |
| `--dichte-inhaltsbreite` | 72 rem | 72 rem | Maximalbreite **innerhalb** einer Ansicht |

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

### Der reservierte Suchplatz

In der Kopfzeile steht links neben dem Mandantenumschalter ein **leerer** Bereich fester Breite
(`data-bereich="suche"`). Kein Eingabefeld, kein Platzhalter, keine Attrappe — bis Schritt 7 ihn
füllt.

**Begründung:** Die Suche ist laut Leitsatz der Haupteinstieg. Steht ihr Platz nicht von Anfang an
fest, drängt sie sich später zwischen Mandant, Sprache und Nutzermenü. Ein Feld, das nichts tut,
wäre trotzdem schlechter als keins: Es verspricht eine Funktion, die es nicht gibt. Am Handy
entfällt der Bereich ganz — dort ist jeder Pixel Breite vergeben.

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

---

## 8. Wie man das Konzept ändert

| Änderung | Datei | Aufwand |
|---|---|---|
| Andere Akzentfarbe | `src/app/globals.css`, die vier Werte `--akzent*` | vier Zeilen, **nachrechnen** |
| Anderes Grün für „abgeschlossen" | `globals.css`, drei Werte `--status-abgeschlossen*` | drei Zeilen |
| Status bekommt eine andere Rolle | `src/lib/status-farbe.ts`, Tabelle `ZUORDNUNG` | eine Zeile |
| Dichtere oder luftigere Navigation | `globals.css`, `--dichte-navzeile` | eine Zeile |
| Dichtere oder luftigere Listen | `globals.css`, `--dichte-zeile` | eine Zeile |
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
| **Q3** Die drei Problemkategorien bleiben getrennt | Rot ist ausschließlich `FEHLER`. „Überfällig" und „unquittiert" bekommen eine eigene Rolle, siehe §7 |
| **Q4** Nicht zugeordnet heißt nicht zugeordnet | eigene Rolle `--status-ungeklaert`, kein geratener Wert |
| Statusabbildung nur über den `MessageStatusClassifier` | `lib/status-farbe.ts` bildet nur die **Einordnung** auf Farbe ab, nie einen Rohwert |
| Status nie allein über Farbe | §3; als Kommentar an `lib/status-farbe.ts`, wo die Zuordnung entsteht |
