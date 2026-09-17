/**
 * Der **Live-Rest der laufenden Stunde** (`docs/live-rest.md`): ob der Verkehr
 * seit dem letzten Rollup-Lauf in den Zahlen einer Ansicht steckt.
 *
 * **Hier und nicht in einem Feature**, weil zwei Features denselben Block lesen —
 * der Prozessbaum seit Teil A, die Übersicht seit Teil B (17.09.2026) — und ein
 * Feature nicht aus einem Nachbarfeature importiert (`docs/frontend-grundlagen.md`
 * §8; dieselbe Bauform wie `lib/rollupzeitraum.ts`). Der Hinweis dazu steht in
 * `components/live-rest-hinweis.tsx`.
 *
 * **Die Typen spiegeln `common/LiveRestResponse` des Backends** und rechnen
 * nichts nach.
 */

/**
 * Die drei Zustände. Nur bei `AUSGESETZT` sagt die Oberfläche etwas — die Zahlen
 * sind dann unvollständig; bei `ANGEWANDT` und `NICHT_NOETIG` steht nichts.
 */
export type LiveRestZustand = "ANGEWANDT" | "NICHT_NOETIG" | "AUSGESETZT";

export type LiveRest = {
  zustand: LiveRestZustand;
  /**
   * **G**, in UTC — nur bei `AUSGESETZT` mit vorhandenem Lauf, sonst `null`: Bis
   * hierhin sind die Zahlen vollständig, ab hier fehlt Verkehr. Angezeigt absolut in
   * der Anzeigezone, wie der Stand der Übersicht.
   */
  vollstaendigBis: string | null;
};
