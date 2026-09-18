/**
 * **Fehler live** (`docs/fehler-live.md`): ob die Fehlerzahlen einer Ansicht aus
 * der Live-Lesung über `Message` kommen — oder, weil die Lesung ausgefallen ist,
 * aus der stündlichen Aggregation, in der eine nachverarbeitete Nachricht noch
 * als Fehler zählen kann.
 *
 * **Hier und nicht in einem Feature**, gebaut wie `lib/live-rest.ts`: Die
 * Übersicht liest den Block seit dem 18.09.2026, der Prozessbaum seit Teil B am
 * selben Tag, und ein Feature importiert nicht aus einem Nachbarfeature
 * (`docs/frontend-grundlagen.md` §8). Der Hinweis dazu steht in
 * `components/fehler-live-hinweis.tsx`.
 *
 * **Die Typen spiegeln `common/FehlerLiveResponse` des Backends** und rechnen
 * nichts nach.
 */

/**
 * Die zwei Zustände. Nur bei `AUSGESETZT` sagt die Oberfläche etwas; bei
 * `ANGEWANDT` steht nichts.
 */
export type FehlerLiveZustand = "ANGEWANDT" | "AUSGESETZT";

/**
 * **Ohne Zeitangabe**, anders als `LiveRest`: Ein Abgang aus einem alten Eimer
 * kann jeden Eimer vor dem letzten Volllauf treffen — es gibt keinen Zeitpunkt,
 * bis zu dem die Fehlerzahlen stimmen.
 */
export type FehlerLive = {
  zustand: FehlerLiveZustand;
};
