import type { CSSProperties } from "react";

import { grundmengeInPixeln, PX_JE_REM, type Spaltenwahl } from "@/lib/spaltenwahl";

/**
 * **Die ziehbare Breite des Panels neben der Liste** *(17.11.2026, E‑1208 bis
 * E‑1210, `docs/nachrichtendetail.md` §10.21)* — die eine Stelle, an der sie
 * definiert ist, neben der klebenden Spalte (`lib/klebende-spalte.ts`), an deren
 * Hülle sie hängt.
 *
 * ## Was gilt
 *
 * | | |
 * |---|---|
 * | **Wo** | an den drei Hüllen neben einer Liste — `/nachrichten`, `/suche`, `/prozesse` mit offener Nachricht —, **ab `xl`**. Nicht darunter, nicht in der Vollansicht, nicht auf der eigenen Route |
 * | **Minimum** | die Breite bis dahin: 26 rem ab `xl`, 30 rem ab `2xl` (`--panel-min`). Ohne Cookie bleibt es dabei |
 * | **Maximum** | lässt der Liste daneben ihre Grundmenge: die Breite der Zeile, die Liste und Panel teilen (`100%`), abzüglich Abstand, Rahmen der Tabellenhülle und Grundmenge (`--panel-abzug`). **In CSS gerechnet**; kein JavaScript misst Fenster oder `main` |
 * | **Maximum unter Minimum** | gilt das Minimum (um 1.280 px): `min-width` schlägt `max-width` |
 * | **Wert** | `--panelbreite` in rem — am `<html>` aus dem Cookie, serverseitig vor dem ersten Paint; während des Ziehens an der Hülle |
 *
 * **rem und nicht px:** Die 26 und 30 rem gehen mit der Anzeigegröße mit
 * (`docs/dichte-umschalter.md`); eine Pixelzahl im Cookie täte das nicht.
 *
 * ## Warum das Maximum eine Variable an der Hülle ist und keine Zahl im Klassennamen
 *
 * Die Grundmenge steht in den Spaltendefinitionen (`features/nachrichten/
 * spalten.ts`, `treffer-spalten.ts`) und wird **nicht** als zweite Zahl
 * abgeschrieben. Tailwind findet aber nur Klassen, die wörtlich im Quelltext
 * stehen; eine aus `spalten.ts` errechnete Klasse gäbe es im Stylesheet nicht.
 * Die Rechnung steht deshalb in {@link panelNebenListe}, als Variable im
 * `style` der Hülle, und die Klasse liest sie.
 */

/** Der Name des Cookies — je Browser, wie die Anzeigegröße. */
export const PANELBREITE_COOKIE = "overlord_panelbreite";

/** Ein Jahr in Sekunden — dieselbe Lebensdauer wie `overlord_dichte`. */
const EIN_JAHR_S = 365 * 24 * 60 * 60;

/** Auf diese Stufe wird gerundet, in rem. */
export const PANELBREITE_STUFE_REM = 0.25;

/** So weit verschiebt eine Pfeiltaste, in rem. */
export const PANELBREITE_TASTE_REM = 1;

/**
 * **Die Grenzen, in denen ein Cookie gültig ist**, in rem. Unten das kleinste
 * Minimum (26 rem ab `xl`); was darunter liegt, kann der Griff nie geschrieben
 * haben. Oben eine Plausibilitätsgrenze, größer als jedes Panel neben einer
 * Liste auf einem 5K-Bildschirm (5.120 px sind 320 rem bei 16 px). Was dazwischen
 * liegt und für das Fenster zu breit ist, begrenzt das CSS — der Wert bleibt
 * gültig und gilt wieder, wenn das Fenster breit genug ist.
 */
export const PANELBREITE_GRENZEN = { min: 26, max: 320 } as const;

/** Rundet auf {@link PANELBREITE_STUFE_REM}. */
export function rundePanelbreite(rem: number): number {
  return Math.round(rem / PANELBREITE_STUFE_REM) * PANELBREITE_STUFE_REM;
}

/**
 * **Liest den Cookie-Wert** — eine Zahl in rem, ohne Einheit (`34.25`).
 * Ungültig, außerhalb von {@link PANELBREITE_GRENZEN} oder fehlend heißt `null`:
 * Dann wird keine Variable gesetzt, und es gilt die heutige Breite.
 */
export function liesPanelbreite(wert: string | undefined): number | null {
  if (wert === undefined || !/^\d{1,3}(\.\d{1,4})?$/.test(wert)) return null;
  const rem = rundePanelbreite(Number(wert));
  return rem >= PANELBREITE_GRENZEN.min && rem <= PANELBREITE_GRENZEN.max ? rem : null;
}

/** Der Wert, wie er im Cookie und in der Variable steht. */
function alsZahl(rem: number): string {
  return String(rundePanelbreite(rem));
}

/** Die Variable am `<html>` — `undefined`, wenn es keine gültige Breite gibt. */
export function panelbreiteStil(rem: number | null): CSSProperties | undefined {
  return rem === null ? undefined : ({ "--panelbreite": `${alsZahl(rem)}rem` } as CSSProperties);
}

/**
 * **Die Zeile für `document.cookie`** — `null` löscht. Eigenschaften wie bei
 * `overlord_dichte`: ein Jahr, `SameSite=Lax`, **nicht** `HttpOnly` (das kann
 * `document.cookie` ohnehin nicht), `path=/`.
 *
 * **Geschrieben wird im Client, ohne Server-Aktion** — die benannte Abweichung
 * vom Cookie-Muster (`docs/frontend-grundlagen.md` §4): Die Breite ist im
 * Client schon angewandt, und der Server braucht sie erst bei der nächsten
 * Anfrage. Eine Server-Aktion mit `revalidatePath` wäre bei jedem Ziehen ein
 * RSC-Umlauf samt Verwurf des Router-Zwischenspeichers (Punkt 101).
 */
export function panelbreiteCookie(rem: number | null): string {
  return rem === null
    ? `${PANELBREITE_COOKIE}=; Max-Age=0; Path=/; SameSite=Lax`
    : `${PANELBREITE_COOKIE}=${alsZahl(rem)}; Max-Age=${EIN_JAHR_S}; Path=/; SameSite=Lax`;
}

/**
 * **Was zwischen dem Inhalt von `main` und der Liste liegt, außer dem Panel:**
 * der Abstand `gap-4` der Zeile und der Rahmen der Tabellenhülle (`border`, je
 * 1 px). An allen drei Einhängepunkten gleich gebaut.
 */
const ZWISCHEN_LISTE_UND_PANEL = "1rem + 2px";

/**
 * **Die Variable `--panel-abzug` für die Hülle** — was von `100cqw` abgeht,
 * damit der Liste ihre Grundmenge bleibt. Die Grundmenge kommt aus der
 * Spaltendefinition (`grundmengeInPixeln`); sie ist in Dichte `m` gemessen und
 * geht in rem in die Rechnung, wie die Schwellen der Liste (`@min-[…rem]`).
 */
export function panelNebenListe(
  liste: Spaltenwahl,
  { listeScrolltSelbst = false }: { listeScrolltSelbst?: boolean } = {},
): CSSProperties {
  const grundmenge = grundmengeInPixeln(liste) / PX_JE_REM;
  const leiste = listeScrolltSelbst ? ` + ${BILDLAUFLEISTE_RESERVE}` : "";
  return {
    "--panel-abzug": `calc(${ZWISCHEN_LISTE_UND_PANEL}${leiste} + ${grundmenge}rem)`,
  } as CSSProperties;
}

/**
 * **Die Reserve für eine Bildlaufleiste in der Liste selbst** — nur in der
 * Prozessansicht, wo die Übertragungsliste für sich scrollt
 * (`IM_RAHMEN_SCROLLT_AB_MD`). Ihre Leiste liegt innerhalb der Liste und nimmt
 * der Tabelle Breite; wie breit sie ist, weiß CSS nicht. Gemessen am
 * 17.11.2026 unter Windows bei 1.920 px: 15 px, die Liste behielt am Anschlag
 * 736 statt 751 px. 15 px ist die Leiste unter Windows bei 100 % Zoom und die
 * breiteste, die vorkommt; ist sie schmaler oder legt sie sich über den Inhalt
 * (macOS), bleibt der Liste etwas mehr als ihre Grundmenge, nie weniger.
 */
const BILDLAUFLEISTE_RESERVE = "15px";

/**
 * **Die Hülle des Panels neben der Liste, ab `xl`** — Breite, Grenzen und der
 * Deckel der Namensspalte.
 *
 * - `--panel-min` ist das Minimum je Umbruchpunkt, `min-width` hält es.
 * - `width` ist `--panelbreite`, ohne sie das Minimum.
 * - `max-width` lässt der Liste ihre Grundmenge (`--panel-abzug`, im `style`).
 *   **`100%` und nicht `100cqw`:** Bezug ist die Zeile, die Liste und Panel
 *   teilen — in allen drei Ansichten die Inhaltsbreite von `main` ohne seine
 *   Bildlaufleiste. `100cqw` zählt in Chrome die Bildlaufleiste von `main` mit;
 *   gemessen am 17.11.2026 bei 1.920 px: 1.672 statt 1.657 px, und die Liste
 *   behielt am Anschlag 736 statt 751 px (`docs/nachrichtendetail.md` §10.21).
 * - `--eigenschaft-name-max` deckelt die Namensspalte der Eigenschaften auf
 *   16 rem (E‑1210). Die Spalte ist `w-2/5` der Zeile und wächst damit von
 *   selbst mit dem Panel; der Deckel gilt nur hier — eigene Route und
 *   Vollansicht behalten ihre 40 % (Entscheidung des Auftraggebers vom
 *   17.11.2026, Variante 1). 16 rem wie die Beschriftungsspalte der eigenen
 *   Route (`docs/bam-werte.md` §11a).
 */
export const PANEL_NEBEN_LISTE_AB_XL =
  "xl:shrink-0 xl:[--panel-min:26rem] 2xl:[--panel-min:30rem] " +
  "xl:w-[var(--panelbreite,var(--panel-min))] xl:min-w-[var(--panel-min)] " +
  "xl:max-w-[calc(100%-var(--panel-abzug))] xl:[--eigenschaft-name-max:16rem]";
