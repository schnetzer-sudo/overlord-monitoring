import { AlertTriangle, Check, CircleHelp, SquarePower, type LucideIcon } from "lucide-react";

import { einsetzen, type Texte } from "@/i18n";
import type { Ablagenzustand, Dienstzustand } from "@/lib/status-farbe";

import type { Ablagen, Ablagengrund, Dienstlampe } from "./api";

/**
 * Die **Entscheidungen** der Kachel *Plattform* — als reine Funktionen und
 * Verzeichnisse, nicht als Ausdruck in einer Komponente
 * (`docs/frontend-grundlagen.md` §9).
 *
 * Die Farbrollen stehen nicht hier, sondern in `lib/status-farbe.ts`: Dort
 * wohnt jede Zuordnung von Fachlichkeit auf Farbe, und eine zweite Stelle
 * weichte genau die Regel auf, um derentwillen es die Datei gibt
 * (`docs/visuelles-konzept.md` §2). Hier steht, was die **Ansicht** entscheidet
 * — welches Zeichen sie setzt, wann sie dämpft, und welchen Satz sie einem
 * `title` mitgibt.
 *
 * > ### ⚠️ Umbau vom 10.09.2026 — die Kachel ist klein geworden
 * >
 * > Aus dem Block unter der Kachelreihe ist die **fünfte Kachel** geworden. Was
 * > dabei aus der Anzeige gefallen ist, ist hier auch aus dem Code gefallen:
 * > `altersatz` gibt es nicht mehr, denn es gibt keine Zeit mehr im Bild.
 * > `grundsatz` ist geblieben — der Grund steht jetzt im `title` statt in einem
 * > sichtbaren Satz.
 */

/**
 * Die Zeichen der vier Dienstzustände — **vier Formen, nicht vier Farben**.
 *
 * `docs/visuelles-konzept.md` §3 verlangt zu jeder Statusanzeige *„zusätzlich
 * eine Beschriftung **oder** ein Zeichen"*. In dieser Kachel steht kein Wort im
 * Bild; das Zeichen trägt die halbe Aussage deshalb **allein**, und dann muss
 * es sich in der **Form** unterscheiden und nicht nur in der Farbe.
 *
 * | Zustand | Zeichen | Umriss |
 * |---|---|---|
 * | `MELDET_SICH` | Häkchen | ein offener Strich, ohne Rahmen |
 * | `ZEITUEBERSCHRITTEN` | Warndreieck | **Dreieck** — dieselbe Gestalt wie an der Fehlerkachel und in „Zuletzt aufgefallen" |
 * | `HERUNTERGEFAHREN` | Ausschaltzeichen im **Viereck** | Viereck |
 * | `UNGEKLAERT` | Fragezeichen im **Kreis** | Kreis |
 *
 * > ⚠️ **Der Umriss des Ausschaltzeichens ist der Befund der Durchsicht vom
 * > 10.09.2026.** Bis dahin trug `HERUNTERGEFAHREN` ein `PowerOff` und die
 * > abgeschaltete Ablage ein `CircleOff` — zwei Kreise mit einem Strich darin,
 * > auf der Aufnahme nicht auseinanderzuhalten. Seither ist genau **ein**
 * > Zeichen rund (das Fragezeichen), genau eines eckig, genau eines dreieckig,
 * > und das vierte hat gar keinen Rahmen. Das hält auch in Graustufen.
 *
 * **Das Häkchen ist bewusst das nackte und nicht `CircleCheck`.** Die
 * Statusplakette der Liste nimmt dort den Kreis — hier stünde er neben dem
 * runden Fragezeichen, und zwei Kreise mit verschiedenem Innenleben sind bei
 * 14 px keine zwei Formen mehr.
 */
export const DIENSTZEICHEN: Record<Dienstzustand, LucideIcon> = {
  MELDET_SICH: Check,
  ZEITUEBERSCHRITTEN: AlertTriangle,
  HERUNTERGEFAHREN: SquarePower,
  UNGEKLAERT: CircleHelp,
};

/**
 * Dieselben Formen für die Ablagen — **zwei Verzeichnisse und nicht eines**.
 *
 * `Dienstzustand` und `Ablagenzustand` sind zwei Aufzählungen, und dass
 * `UNGEKLAERT` in beiden vorkommt, macht sie nicht zu einer (E‑128, derselbe
 * Gedanke wie bei den beiden Farbzuordnungen). **Geteilt ist die Formensprache**
 * — erreichbar trägt dasselbe Häkchen wie ein Dienst, der sich meldet, denn es
 * ist dieselbe Auskunft in derselben Kachel.
 */
export const ABLAGENZEICHEN: Record<Ablagenzustand, LucideIcon> = {
  ERREICHBAR: Check,
  NICHT_ERREICHBAR: AlertTriangle,
  UNGEKLAERT: CircleHelp,
};

/**
 * **Der Rohwert steht nur an einer ungeklärten Lampe** (Entscheidung **E‑130**,
 * in der Hälfte, die den Umbau überlebt hat).
 *
 * `dienste[].rohwert` kommt bei *jeder* Lampe mit, auch bei den bekannten
 * Werten (E‑118) — er ist der Anker, an dem sich „ungeklärt" festmachen lässt
 * (Regel Q4). **Genannt gehört er trotzdem nur dort.** Neben
 * „Zeitüberschreitung" stünde `ERROR_TIMEOUT` und sagte dasselbe ein zweites
 * Mal, in der Sprache der Anlage statt in der des Nutzers; neben „Ungeklärt"
 * ist er die ganze Auskunft, denn ohne ihn bliebe nur ein Achselzucken.
 *
 * **Er wird nicht übersetzt und nicht zerlegt.** Es ist ein Wert aus
 * `Service.ServiceStatus`, kein Anzeigetext.
 */
export function zeigtRohwert(zustand: Dienstzustand): boolean {
  return zustand === "UNGEKLAERT";
}

/**
 * Was eine Dienstzeile **sagt** — der Satz für `title` und `sr-only`.
 *
 * ## Warum das Wort nicht mehr im Bild steht
 *
 * Die Kachel ist klein, und in ihr steht je Dienst eine Zeile aus **Zeichen und
 * Kennung**. Das ist die Bauform von **E‑91**: Das Zeichen trägt die
 * Farbrolle, das Wort steht im `title` und für Vorleseprogramme im Markup. Ein
 * Wort je Zeile kostete die Breite, die die Kennung braucht — und die Kennung
 * ist das Einzige, was den Dienst benennt (E‑122).
 *
 * ## Bei `UNGEKLAERT` steht der Rohwert dabei
 *
 * Dann ist das Wort allein keine Auskunft. Beides zusammen in **einem** Satz —
 * `title` kennt keine zweite Zeile, und ein Vorleseprogramm liest zwei
 * Geschwister ohnehin hintereinander.
 */
export function lampenauskunft(dienst: Dienstlampe, texte: Texte): string {
  const wort = texte.dashboard.plattform.dienst[dienst.zustand];
  if (!zeigtRohwert(dienst.zustand)) {
    return wort;
  }
  const zusatz =
    dienst.rohwert === null
      ? texte.dashboard.plattform.rohwertFehlt
      : einsetzen(texte.dashboard.plattform.rohwert, { wert: dienst.rohwert });
  return einsetzen(texte.dashboard.plattform.wortUndZusatz, { wort, zusatz });
}

/**
 * **Eine Zielzeile trägt ihre Farbe nur, solange der Stand der Kachel gilt**
 * (Entscheidung **E‑133**, unverändert aus Teil B).
 *
 * | Kachel | Zielzeilen |
 * |---|---|
 * | `ERREICHBAR`, `NICHT_ERREICHBAR` | mit Rolle — sie *sind* die Auskunft der Kachel |
 * | `UNGEKLAERT` · `ZIEL_UNGEKLAERT` | mit Rolle — der Durchgang ist frisch, nur eine Antwort ließ sich nicht einordnen |
 * | `UNGEKLAERT` · `STAND_VERALTET` | **ohne Rolle**, gedämpft — mit ihrem letzten Wort im `title` |
 * | jeder andere Grund | ohne Rolle. Dort gibt es ohnehin keine Zielzeile, sondern die Sammelzeile |
 *
 * **Grün überlebt seinen Beleg auch in der Anzeige nicht** — das ist E‑125
 * einen Schritt weiter gedacht. Das Backend nimmt der *Kachel* ihre Aussage,
 * sobald der Stand älter ist als zwei Takte; stünde die Zielzeile darunter
 * weiterhin grün, wäre dieselbe Aussage über denselben Umweg wieder im Bild.
 *
 * **Und die Zeilen verschwinden trotzdem nicht.** Sie sind nicht mehr die
 * Auskunft der Kachel, aber sie sind das, was zuletzt festgestellt wurde
 * (`docs/dienste.md` §8) — gedämpft und mit ihrem Wort im `title`.
 */
export function traegtZielfarbe(zustand: Ablagenzustand, grund: Ablagengrund | null): boolean {
  if (zustand !== "UNGEKLAERT") {
    return true;
  }
  return grund === "ZIEL_UNGEKLAERT";
}

/**
 * Der Satz zum Grund — **im `title`, seit dem 10.09.2026 nicht mehr im Bild**.
 *
 * `null`, wenn es keinen Grund gibt: Bei `ERREICHBAR` und `NICHT_ERREICHBAR`
 * liefert das Backend `grund: null`, und dort steht die Auskunft im Wort.
 *
 * **Fünf Gründe, fünf Sätze, kein Rückfall.** Ein unbekannter Grund kann nicht
 * entstehen — die Aufzählung ist geschlossen und wird vom Typ erzwungen; käme
 * je ein sechster hinzu, verlangt TypeScript hier eine Zeile, statt still einen
 * leeren `title` zu setzen.
 */
export function grundsatz(grund: Ablagengrund | null, texte: Texte): string | null {
  return grund === null ? null : texte.dashboard.plattform.grund[grund];
}

/**
 * Die **Sammelzeile** „Ablagen" — oder `null`, solange es Ziele gibt.
 *
 * ## Wo keine Ablage geprüft wurde, steht trotzdem eine Zeile
 *
 * Drei der fünf Gründe liefern **kein** Ziel: abgeschaltet, noch kein
 * Durchgang, keines eingetragen (`docs/dienste.md` §8). Ohne diese Zeile
 * verschwände die Ablagenprüfung dann **spurlos** aus der Kachel — und
 * Abwesenheit ist der schwächste Kanal, den eine Auskunft haben kann (E‑74,
 * E‑81, E‑135). Die Zeile sagt: *es gibt hier eine Prüfung, und sie weiß gerade
 * nichts.*
 *
 * ## Sie steht auf `UNGEKLAERT`, und zwar fest
 *
 * Nicht `ablagen.zustand`, sondern der Wert selbst. Ohne Ziel **ist** die
 * Kachel nach E‑125 ungeklärt; stünde hier je etwas anderes, trüge eine Zeile
 * mit der Aufschrift „Ablagen" ein grünes Häkchen — und genau diese Aussage
 * schließt **E‑132** aus: Geprüft wird eine Stichprobe aus
 * `ServiceDefaultFileStore`, nie „alle Ablagen" (offener Punkt 166).
 */
export function sammelzeilenauskunft(ablagen: Ablagen, texte: Texte): string | null {
  if (ablagen.ziele.length > 0) {
    return null;
  }
  const wort = texte.dashboard.plattform.ablage.UNGEKLAERT;
  const zusatz = grundsatz(ablagen.grund, texte);
  return zusatz === null
    ? wort
    : einsetzen(texte.dashboard.plattform.wortUndZusatz, { wort, zusatz });
}
