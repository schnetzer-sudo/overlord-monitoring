/**
 * **Die Regeln des Kopierens im Nachrichtendetail** — rein, ohne React und ohne
 * Browser, damit sie ohne gerenderten Baum prüfbar sind
 * (`docs/nachrichtendetail.md` §10.21, E‑1207).
 */

/**
 * **So lange steht das Häkchen**, nach Knopf wie nach Tippen. Danach kehrt das
 * Zeichen zum Kopierzeichen zurück — ein Häkchen, das stehen bliebe, sagte beim
 * zweiten Kopieren nichts mehr.
 */
export const KOPIERT_ANZEIGE_MS = 1500;

/** Die beiden Zeigerarten, bei denen ein Tippen auf den Wert kopiert. */
const TIPPEN = new Set(["touch", "pen"]);

/**
 * **Kopiert ein Klick auf den Wert?** Nur, wenn er aus einem Finger oder Stift
 * kommt — entschieden **je Ereignis**, nicht über `matchMedia`: Ein Laptop mit
 * Maus und Touchscreen kann beides, und die Maus soll dort Text markieren.
 *
 * **Zwei Quellen, weil Safari die erste nicht verlässlich liefert.** Maßgeblich
 * ist zuerst die Zeigerart am Klick selbst (`PointerEvent.pointerType`, so in
 * Chrome und Firefox). Safari feuert den Klick erst seit 18.2 als
 * `PointerEvent`, und meldet dort beim Tippen `"mouse"` statt `"touch"`
 * (WebKit-Bug 282988); davor trug der Klick gar keine Zeigerart. Das
 * `pointerup` derselben Geste trägt sie dort richtig — deshalb zählt es als
 * zweite Quelle. Eine echte Maus meldet an beiden Stellen `"mouse"`.
 *
 * @param amKlick die Zeigerart am Klickereignis, `undefined` ohne `PointerEvent`
 * @param amLoslassen die Zeigerart des `pointerup` derselben Geste, sonst `null`
 */
export function tippenKopiert(amKlick: string | undefined, amLoslassen: string | null): boolean {
  return (
    (amKlick !== undefined && TIPPEN.has(amKlick)) ||
    (amLoslassen !== null && TIPPEN.has(amLoslassen))
  );
}
