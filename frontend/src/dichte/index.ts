/**
 * **Die Anzeigedichte** — vier Stufen, die die Wurzelschriftgröße umstellen und
 * damit jedes `rem` des Projekts.
 *
 * ## Warum ein Umschalter und kein fest verkleinerter Wert
 *
 * Die Frage „welche Größe ist richtig" hat keine Messung und ist je Nutzer eine
 * andere: Wer täglich acht Stunden Listen liest, will nicht die Dichte des
 * externen Kundenmitarbeiters, der zweimal im Monat einen Beleg sucht. Eine
 * Zahl, die für beide gilt, gibt es nicht — also gibt es die Wahl.
 *
 * ## Warum das hier neben `i18n/` liegt und nicht in `lib/`
 *
 * Weil es dieselbe Bauform ist und nicht dieselbe Art Sache. Die Sprache ist
 * eine Eigenschaft des **Nutzers**, sie liegt in einem Cookie, das Wurzel-Layout
 * liest sie **serverseitig**, und umgeschaltet wird über eine **Server-Aktion**
 * in einem Formular. Auf die Dichte trifft Wort für Wort dasselbe zu, und
 * deshalb steht sie als dieselbe Dreiheit da: Definition hier, Leseweg in
 * `server.ts`, Umschaltung in `aktion.ts`, Kontext in `provider.tsx`.
 *
 * `lib/` ist Infrastruktur — `http`, `format`, `routen`, `filter` —, und jedes
 * dieser Module ist eine Datei ohne Serverhälfte. Ein `"use server"` steht am
 * **Dateianfang** und macht *jeden* Export zur Server-Aktion; die Konstanten
 * dieser Datei dürfen das nicht werden. Sie ließen sich also ohnehin nicht in
 * eine Datei mit der Aktion legen (`docs/frontend-grundlagen.md` §8).
 *
 * ## Warum ein Cookie und kein `localStorage`
 *
 * Weil das Wurzel-Layout die Stufe **vor dem ersten Paint** braucht. Es setzt
 * `data-dichte` an `<html>`, und das geschieht auf dem Server. Aus
 * `localStorage` wäre der Wert erst nach der Hydratation zu haben — die
 * Anwendung erschiene bei **jedem** Aufruf einmal in der falschen Größe und
 * spränge dann.
 *
 * Der Nachrüstweg auf eine Spalte an `app_user` bleibt offen, genau wie bei der
 * Sprache: Dann wird das Cookie zum Zwischenspeicher, und an der Oberfläche
 * ändert sich nichts.
 */

/**
 * Die vier Stufen, **in dieser Reihenfolge** — von der dichtesten zur
 * luftigsten. Die Oberfläche zeigt sie so; eine andere Reihenfolge dort wäre
 * eine zweite Wahrheit über eine Skala, die eine Richtung hat.
 *
 * Die Werte, die daran hängen, stehen in `app/globals.css`
 * (`html[data-dichte="…"]`) und nirgends sonst. Hier stehen nur die Namen —
 * eine Prozentzahl in dieser Datei wäre ein zweiter Ort für einen
 * Gestaltungswert, und genau das verhindert das visuelle Konzept.
 */
export const DICHTESTUFEN = ["xs", "s", "m", "l"] as const;

export type Dichtestufe = (typeof DICHTESTUFEN)[number];

/**
 * **`m` ist der heutige Zustand und bleibt die Vorgabe.**
 *
 * Alle bisherigen Messungen des Projekts sind gegen ihn gemessen — die 10 rem
 * aus `docs/bam-werte.md` §11a, die Achsenbreite aus
 * `docs/dashboard-frontend.md` §10.4, die 142 px für vier Navigationseinträge
 * aus `docs/visuelles-konzept.md` §5. Wäre er nicht mehr die Vorgabe, wären sie
 * Messungen eines Zustands, den niemand mehr sieht.
 *
 * Ob die Vorgabe später eine Stufe herunterwandert, ist eine eigene und spätere
 * Entscheidung.
 */
export const STANDARDDICHTE: Dichtestufe = "m";

/** Name des Cookies mit der Dichtewahl. Kein `HttpOnly` — es ist keine Auskunft. */
export const DICHTE_COOKIE = "overlord_dichte";

/**
 * Der Name des Formularfelds, über das die Stufe an die Server-Aktion geht.
 *
 * **Steht hier und nicht zweimal als nackte Zeichenkette.** Die auslösende
 * Schaltfläche schreibt ihn als `name`, die Aktion liest ihn mit
 * `daten.get(…)` — und eine Abweichung zwischen beiden fiele **still** aus:
 * `FormData.get` lieferte `null`, {@link dichteAus} machte daraus die Vorgabe,
 * und der Umschalter sähe aus, als tue nur *Standard* etwas. Kein Fehler, keine
 * Meldung, kein roter Test.
 */
export const DICHTE_FELD = "dichte";

/** Ein Jahr. Wie die Sprachwahl trifft niemand diese Wahl gern zweimal. */
export const DICHTE_COOKIE_DAUER = 60 * 60 * 24 * 365;

export function istDichtestufe(wert: string | undefined | null): wert is Dichtestufe {
  return wert !== undefined && wert !== null && (DICHTESTUFEN as readonly string[]).includes(wert);
}

/**
 * Fällt bei unbekanntem oder fehlendem Wert auf {@link STANDARDDICHTE} zurück.
 *
 * **Nicht raten und nicht der nächstliegenden Stufe zuordnen.** Aus `"xxs"` wird
 * nicht `xs`, aus `"gross"` nicht `l` — das wäre eine Behauptung über einen
 * Wert, über den nichts bekannt ist. Dieselbe Haltung wie Regel Q4: Nicht
 * zugeordnet heißt nicht zugeordnet, und der Rückfall ist definiert statt
 * geraten.
 */
export function dichteAus(wert: string | undefined | null): Dichtestufe {
  return istDichtestufe(wert) ? wert : STANDARDDICHTE;
}
