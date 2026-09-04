/**
 * **Das Erscheinungsbild** — drei Werte, die den Dunkelblock in
 * `app/globals.css` ein- und ausschalten.
 *
 * ## Warum das hier neben `i18n/` und `dichte/` liegt und nicht in `lib/`
 *
 * Weil es dieselbe Bauform ist. Sprache, Dichte und Thema sind alle drei eine
 * Eigenschaft des **Nutzers**, sie liegen in einem Cookie, das Wurzel-Layout
 * liest sie **serverseitig**, und umgeschaltet wird über eine **Server-Aktion**
 * in einem Formular. Deshalb steht auch dieses hier als dieselbe Vierheit:
 * Definition hier, Leseweg in `server.ts`, Umschaltung in `aktion.ts`, Kontext
 * in `provider.tsx`.
 *
 * `lib/` ist Infrastruktur — `http`, `format`, `routen`, `filter` —, und jedes
 * dieser Module ist eine Datei ohne Serverhälfte. Dazu der harte Grund: Ein
 * `"use server"` steht am **Dateianfang** und macht *jeden* Export zur
 * Server-Aktion; die Konstanten dieser Datei dürfen das nicht werden. Sie
 * ließen sich also ohnehin nicht in eine Datei mit der Aktion legen
 * (`docs/frontend-grundlagen.md` §8, `docs/dichte-umschalter.md` §6).
 *
 * ## Warum ein Cookie und kein `localStorage`
 *
 * Weil das Wurzel-Layout den Wert **vor dem ersten Paint** braucht. Es setzt
 * `data-thema` an `<html>`, und das geschieht auf dem Server. Aus
 * `localStorage` wäre der Wert erst nach der Hydratation zu haben — die
 * Anwendung erschiene bei **jedem** Aufruf einmal hell und spränge dann ins
 * Dunkle. Das ist E‑x, und es gilt hier wörtlich wie bei der Dichte;
 * nachgewiesen als M136 in `docs/dunkelmodus.md`.
 *
 * Der Nachrüstweg auf eine Spalte an `app_user` bleibt offen, genau wie bei
 * Sprache und Dichte.
 *
 * ## Warum ein Attribut und keine Klasse *(E‑59)*
 *
 * `data-dichte` steht als Attribut am selben Element. Eine Klasse `dark`
 * daneben stellte zwei Nutzereinstellungen in zwei Bauformen an dasselbe
 * Element — und vor allem ist eine Klasse eine **Menge**, in der ein dritter
 * Wert *System* keine Stelle hat. Ein Attribut trägt genau einen Wert, und das
 * ist die Wahl.
 */

/**
 * Die drei Werte, **in dieser Reihenfolge** — die beiden ausdrücklichen zuerst,
 * dann der Rückweg zu „keine Wahl".
 *
 * Was daran hängt, steht in `app/globals.css` (`html[data-thema="…"]`) und
 * nirgends sonst. Hier stehen nur die Namen — ein Farbwert in dieser Datei wäre
 * ein zweiter Ort für einen Gestaltungswert, und genau das verhindert das
 * visuelle Konzept (`docs/visuelles-konzept.md` §2).
 */
export const THEMAWERTE = ["hell", "dunkel", "system"] as const;

export type Themawert = (typeof THEMAWERTE)[number];

/**
 * **`system` ist die Vorgabe, nicht `hell`.**
 *
 * Wer nie umgeschaltet hat, hat keine Wahl getroffen — und genau das bedeutet
 * `system`: Es gilt, was das Betriebssystem sagt. Der Rückfall auf `hell` wäre
 * eine Behauptung über einen Nutzer, über den nichts bekannt ist, und er
 * überginge die Auskunft, die sein Gerät schon gegeben hat (E‑60).
 */
export const STANDARDTHEMA: Themawert = "system";

/** Name des Cookies mit der Themawahl. Kein `HttpOnly` — es ist keine Auskunft. */
export const THEMA_COOKIE = "overlord_thema";

/**
 * Der Name des Formularfelds, über das der Wert an die Server-Aktion geht.
 *
 * **Steht hier und nicht zweimal als nackte Zeichenkette.** Die auslösende
 * Schaltfläche schreibt ihn als `name`, die Aktion liest ihn mit
 * `daten.get(…)` — und eine Abweichung zwischen beiden fiele **still** aus:
 * `FormData.get` lieferte `null`, {@link themaAus} machte daraus die Vorgabe,
 * und der Umschalter sähe aus, als tue nur *Systemeinstellung* etwas. Kein
 * Fehler, keine Meldung, kein roter Test.
 */
export const THEMA_FELD = "thema";

/** Ein Jahr. Wie Sprache und Dichte trifft niemand diese Wahl gern zweimal. */
export const THEMA_COOKIE_DAUER = 60 * 60 * 24 * 365;

export function istThemawert(wert: string | undefined | null): wert is Themawert {
  return wert !== undefined && wert !== null && (THEMAWERTE as readonly string[]).includes(wert);
}

/**
 * Fällt bei unbekanntem oder fehlendem Wert auf {@link STANDARDTHEMA} zurück.
 *
 * **Nicht raten und nicht sinngemäß zuordnen.** Aus `"dark"` wird nicht
 * `dunkel`, aus `"DUNKEL"` auch nicht und aus `"nacht"` erst recht nicht — das
 * wäre eine Behauptung über einen Wert, über den nichts bekannt ist. Regel Q4:
 * Nicht zugeordnet heißt nicht zugeordnet.
 *
 * **Und der Rückfall ist hier zusätzlich der inhaltlich richtige:** Ein
 * unbekannter Wert heißt „keine Wahl getroffen", und keine Wahl ist genau das,
 * was `system` bedeutet.
 */
export function themaAus(wert: string | undefined | null): Themawert {
  return istThemawert(wert) ? wert : STANDARDTHEMA;
}
