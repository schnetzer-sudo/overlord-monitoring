import type { KeyboardEvent, MouseEvent } from "react";

/**
 * **Was ein Ereignis auf einer Zeile bedeutet** — für die Nachrichtenliste, die
 * Übertragungsliste der Prozessansicht und die Trefferliste der Suche, an einer
 * Stelle *(17.11.2026, E‑1195, `docs/nachrichtendetail.md` §10.2)*.
 *
 * | Ereignis | Bedeutung |
 * |---|---|
 * | Klick, `Enter`, `Leertaste` | öffnet das Panel — **unverändert** |
 * | Strg-Klick, Cmd-Klick (macOS), Mittelklick, Strg+Enter, Cmd+Enter | öffnet die Nachricht auf ihrer eigenen Route **im neuen Tab**; die Ausgangsseite bleibt, wie sie ist |
 * | irgendetwas davon auf einem Verweis, einer Schaltfläche oder einem Feld *in* der Zeile | ist dessen Sache, die Zeile tut nichts |
 *
 * ## Die Zeile bleibt eine Zeile
 *
 * Kein Anker um die Zeile: Ein `<a>` um eine Tabellenzeile ist kein gültiges
 * Markup, und ein Verweis in jeder Zelle machte aus einer Zeile, die man mit
 * `Tab` erreicht, fünf. Der neue Tab geht deshalb über `window.open` mit
 * `noopener` — die neue Seite bekommt keinen Griff auf diese.
 *
 * ## Der Mittelklick und das automatische Scrollen
 *
 * Unter Windows startet die mittlere Taste auf einer Fläche ohne Verweis das
 * automatische Scrollen — der Mauszeiger wird zum Kompass, und der Klick kommt
 * nie als Klick an. Unterdrückt wird es beim **Drücken** (`mousedown`), denn dort
 * beginnt es; auf einem Verweis in der Zeile nicht, dort öffnet der Browser den
 * Verweis selbst. Der Tab öffnet sich beim **Loslassen** (`auxclick`), wie bei
 * einem Verweis.
 *
 * **Ohne Ziel bleibt alles beim Alten**: Gibt ein Einhängepunkt kein `neuerTab`
 * mit, öffnet auch ein Strg-Klick das Panel. Alle drei geben eins mit.
 *
 * @param neuerTab das Ziel im neuen Tab, **im Ereignis** gerechnet
 *   (`lib/routen.ts` `zeileImNeuenTab`) — der Einhängepunkt liest dort die
 *   Adresse, die gerade dasteht
 */
export function zeilenbedienung(
  messageId: string,
  aufAuswahl: (messageId: string) => void,
  neuerTab?: (messageId: string) => string,
) {
  const imNeuenTab =
    neuerTab === undefined ? undefined : () => oeffneImNeuenTab(neuerTab(messageId));

  return {
    onClick(ereignis: MouseEvent<HTMLElement>) {
      // Ein Klick auf einen Verweis oder eine Schaltfläche *in* der Zeile öffnet
      // nichts mit.
      if (eigenesZiel(ereignis.target)) {
        return;
      }
      if (imNeuenTab !== undefined && mitTabTaste(ereignis)) {
        imNeuenTab();
        return;
      }
      aufAuswahl(messageId);
    },
    onAuxClick(ereignis: MouseEvent<HTMLElement>) {
      if (
        ereignis.button !== MITTLERE_TASTE ||
        imNeuenTab === undefined ||
        eigenesZiel(ereignis.target)
      ) {
        return;
      }
      ereignis.preventDefault();
      imNeuenTab();
    },
    onMouseDown(ereignis: MouseEvent<HTMLElement>) {
      if (
        ereignis.button === MITTLERE_TASTE &&
        imNeuenTab !== undefined &&
        !eigenesZiel(ereignis.target)
      ) {
        ereignis.preventDefault();
      }
    },
    onKeyDown(ereignis: KeyboardEvent<HTMLElement>) {
      if (ereignis.target !== ereignis.currentTarget) {
        return;
      }
      if (ereignis.key === "Enter" && imNeuenTab !== undefined && mitTabTaste(ereignis)) {
        ereignis.preventDefault();
        imNeuenTab();
        return;
      }
      if (ereignis.key === "Enter" || ereignis.key === " ") {
        // Sonst blättert die Leertaste die Ansicht weiter, während sie öffnet.
        ereignis.preventDefault();
        aufAuswahl(messageId);
      }
    },
  };
}

/** `MouseEvent.button` der mittleren Taste. */
const MITTLERE_TASTE = 1;

/** Was in einer Zeile seine eigenen Klicks hat (`nachrichtendetail.md` §10.2). */
const EIGENE_ZIELE = "a, button, input, label";

function eigenesZiel(ziel: EventTarget | null): boolean {
  const element = ziel as Element | null;
  return typeof element?.closest === "function" && element.closest(EIGENE_ZIELE) !== null;
}

/** Strg unter Windows und Linux, Cmd unter macOS — wie bei einem Verweis. */
function mitTabTaste(ereignis: { ctrlKey: boolean; metaKey: boolean }): boolean {
  return ereignis.ctrlKey || ereignis.metaKey;
}

function oeffneImNeuenTab(ziel: string): void {
  // Nur `noopener` als Angabe: Damit ist es kein Aufklappfenster, sondern ein
  // Tab (HTML, „window open steps": `noopener` zählt nicht als Fenstermerkmal).
  window.open(ziel, "_blank", "noopener");
}
