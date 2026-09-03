import { createParser } from "nuqs";

/**
 * Die drei Paare aus **Fensterbreite und Eimerbreite** — `48H`, `30T`, `12M`.
 *
 * ## Warum sie in `lib` stehen und nicht mehr im Dashboard
 *
 * *(seit 02.09.2026, Schritt 10c‑2.)* Sie hießen bis dahin `Dashboardzeitraum`
 * und lagen in `features/dashboard/api.ts`. Die **Prozessansicht** braucht
 * dieselben drei Codes: dieselbe Menge, dieselbe Reihenfolge im Umschalter,
 * dieselbe Prüfung. **Ein Feature importiert nicht aus einem Nachbarfeature**
 * (`docs/frontend-grundlagen.md` §8) — braucht ein zweites Feature einen Typ,
 * wandert der Typ nach `lib` und nicht ins Nachbarfeature.
 *
 * Das ist **dieselbe Bewegung, die das Backend am selben Tag gemacht hat**:
 * `dashboard.Dashboardzeitraum` → `common.Rollupzeitraum`, Entscheidung E‑44 in
 * `docs/process-view.md`. Der Name folgt dorthin, damit dieselbe Sache in beiden
 * Hälften gleich heißt.
 *
 * **Die Alternative wäre eine zweite Liste mit denselben drei Codes gewesen** —
 * und die driftet: Käme je ein viertes Paar dazu, hätte die eine Ansicht es und
 * die andere nicht, und beide sähen richtig aus.
 *
 * ## Was hier bewusst **nicht** steht
 *
 * **Keine Vorgabe.** Welches Paar ohne Angabe gilt, entscheidet das Backend, und
 * es entscheidet je Endpunkt verschieden: Das Dashboard wählt nach Belegung
 * (`docs/dashboard.md` §3), die Prozessansicht nimmt fest `48H` (Entscheidung
 * E‑38). Eine Zahl hier wäre ein zweiter Standardwert und liefe einem von beiden
 * hinterher.
 *
 * **Keine Fenstergrenzen.** Die rechnet das Backend gegen die Anwendungsuhr
 * (Regel Z1) und nennt sie in der Antwort. Im Browser gerechnet wären sie gegen
 * die Browseruhr gerechnet — und die Testkopie liegt Monate hinter der realen
 * Uhrzeit.
 *
 * **Keine Beschriftungen.** Die stehen in den Sprachdateien (`texte.zeitraum`).
 */
export const ROLLUPZEITRAEUME = ["48H", "30T", "12M"] as const;

export type Rollupzeitraum = (typeof ROLLUPZEITRAEUME)[number];

export function istRollupzeitraum(wert: string | null | undefined): wert is Rollupzeitraum {
  return (
    wert !== null && wert !== undefined && (ROLLUPZEITRAEUME as readonly string[]).includes(wert)
  );
}

/**
 * Ein unbekannter Code landet nicht in der URL — er wäre ein garantiertes `400`
 * `zeitraum-unbekannt`, und zwar an **beiden** Endpunkten.
 *
 * **Kein `withDefault`, und damit auch keine `clearOnDefault`-Falle.** Ohne
 * Standardwert prüft `nuqs` gar nicht erst auf ihn; der Parameter verschwindet
 * genau dann, wenn er auf `null` gesetzt wird. Das ist die Regel aus
 * `docs/frontend-grundlagen.md` §8: Ein Standardwert, der etwas *setzt*, gehört
 * nicht in die URL.
 */
export const parseAsRollupzeitraum = createParser<Rollupzeitraum>({
  parse: (wert) => (istRollupzeitraum(wert) ? wert : null),
  serialize: (wert) => wert,
});

/**
 * Welche Zeitraum-Schaltfläche hervorgehoben ist — **was gilt, nicht was in der
 * URL steht.**
 *
 * Solange nichts gewählt ist, hebt der Umschalter das Paar hervor, das der
 * Endpunkt genommen hat; das steht in der Antwort und wird nirgends
 * zurückgeschrieben (Entscheidung E‑n). Fehlt auch die Antwort noch, ist keine
 * Schaltfläche gedrückt — eine vorgemerkte wäre eine Vermutung, die beim
 * Eintreffen der Antwort springt.
 *
 * **Die Regel steht hier und nicht zweimal in zwei Features.** Beide Ansichten,
 * die den Umschalter tragen, brauchen genau sie; und der Unterschied zwischen
 * *gewählt* und *gewirkt* ist der ganze Inhalt von E‑n.
 */
export function hervorgehobenesPaar(
  gewaehlt: Rollupzeitraum | null,
  ausDerAntwort: Rollupzeitraum | undefined,
): Rollupzeitraum | null {
  return gewaehlt ?? ausDerAntwort ?? null;
}

/**
 * Die gelesenen Fenstergrenzen einer Rollup-Antwort, **UTC und `bis`
 * ausschließend**.
 *
 * Beide liegen auf einer **Eimergrenze**; die obere ist der Anfang des
 * *nächsten* Eimers. Das unterscheidet sich absichtlich vom Listen-Endpunkt,
 * der `zeitraum` auf die Sekunde genau auflöst — für ein Diagramm wäre das
 * falsch (`docs/dashboard.md` §2).
 *
 * **Der Typ steht hier und nicht zweimal in zwei Features.** Dashboard und
 * Prozessansicht bekommen dasselbe Feld mit derselben Bedeutung aus demselben
 * Backend-Record (`ZeitfensterResponse`); zweimal deklariert liefe er beim
 * nächsten Feld auseinander, und weil TypeScript strukturell prüft, fiele der
 * Unterschied an keiner Zuweisung auf.
 */
export type Fenster = { von: string; bis: string };
