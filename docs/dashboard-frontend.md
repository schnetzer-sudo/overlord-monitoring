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
