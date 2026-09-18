import type { Automatikzustand } from "@/components/neu-laden";

import { alsAbfrage, type Nachrichtenfilter } from "./filter";

/**
 * Wann die Liste von selbst neu fragt, und wohin „Neu laden" führt — **rein,
 * ohne React** (`docs/neu-laden.md`, `docs/nachrichtenliste.md` §8.3).
 *
 * Die Regeln stehen hier und nicht als Bedingung in einem Haken, damit sie als
 * Funktionen geprüft werden können (`tests/aktualisierung.test.ts`; bis zum
 * 18.09.2026 stand hier `tests/neu-laden.test.ts`, eine Datei, die es nie gab).
 *
 * *Seit dem 18.09.2026 (E‑216)* auch der Blätterstapel selbst und der Σ unter
 * der Liste: was ein Schritt durch die Liste aus dem Stapel macht, und wie viele
 * Zeilen bis zur angezeigten Seite angekommen sind.
 */

/** Das Intervall der automatischen Aktualisierung. Keine Wahl (E‑165). */
export const AKTUALISIERUNG_INTERVALL_MS = 60_000;

/**
 * **Die automatische Aktualisierung fragt nur, wenn alle drei gelten** —
 * unverändert seit Schritt 4 (`docs/nachrichtenliste.md` §8.3):
 *
 * - **der Schalter ist an** — die Vorgabe ist aus; wer das Werkzeug öffnet, ist
 *   angespannt, und eine Liste, die unter den Händen springt, hilft nicht;
 * - **Seite eins** — sonst zeigte die Ansicht plötzlich einen anderen
 *   Ausschnitt, oder der Cursor läge außerhalb des Fensters;
 * - **der Tab ist sichtbar** — sonst stellte ein über Nacht offenes Fenster
 *   480 Abfragen auf der Produktionsdatenbank.
 *
 * Zurück kommt der Wert für `refetchInterval`: die Millisekunden oder `false`.
 */
export function aktualisierungsintervall(lage: {
  an: boolean;
  aufSeiteEins: boolean;
  sichtbar: boolean;
}): number | false {
  return lage.an && lage.aufSeiteEins && lage.sichtbar ? AKTUALISIERUNG_INTERVALL_MS : false;
}

/**
 * **Was der Schalter zeigt** (E‑171). Pausiert ist er, wenn er an ist und die
 * Liste nicht auf Seite eins steht — und zwar **am Schalter selbst**, denn ein
 * Schalter, der an ist und nichts tut, ist schlimmer als einer, der aus ist.
 *
 * **Der verdeckte Tab pausiert ihn nicht.** Wer ihn sieht, hat den Tab ohnehin
 * im Vordergrund; die Bedingung wirkt allein auf das Intervall.
 */
export function automatikzustand(an: boolean, aufSeiteEins: boolean): Automatikzustand {
  if (!an) {
    return "aus";
  }
  return aufSeiteEins ? "an" : "pausiert";
}

/**
 * **Wohin „Neu laden" führt: Seite eins desselben Filters** (E‑168).
 *
 * Kein Cursor — auch dann nicht, wenn gerade Seite sieben zu sehen ist. Zwei
 * Gründe, und jeder reicht allein: Bei der Vorgabesortierung über
 * `MessageLastUpdate` wandert eine geänderte Nachricht nach oben, die Seiten
 * dahinter verschieben sich also; und der Cursor eines relativen Fensters kann
 * inzwischen aus dem Fenster gefallen sein (`cursor-ungueltig`).
 *
 * **Die geöffnete Nachricht gehört nicht dazu** (E‑169) — `alsAbfrage` kennt sie
 * ohnehin nicht (`docs/frontend-grundlagen.md` §8, dritte Regel). Neu geladen
 * werden die Hauptdaten der Ansicht, nie das Panel.
 */
export function abfrageNachNeuLaden(filter: Nachrichtenfilter): string {
  return alsAbfrage(filter, null);
}

/**
 * **Der Blätterstapel, nachdem Seite eins angekommen ist.**
 *
 * „Neu laden" holt auf einer späteren Seite zuerst Seite eins und wechselt erst
 * danach — so bleibt die vorhandene Seite stehen, bis die neue da ist. In der
 * Zwischenzeit kann der Nutzer weitergeblättert oder den Filter geändert haben;
 * dann hat er eine neue Stelle gewählt, und die Rückkehr auf Seite eins
 * überginge sie. Verglichen wird deshalb der **Stapel selbst**, nicht seine
 * Länge: Jedes Blättern und jede Filteränderung setzt einen neuen.
 */
export function stapelNachNeuLaden<T>(beimKlick: T[], jetzt: T[]): T[] {
  return jetzt === beimKlick ? [] : jetzt;
}

/**
 * **Ein Eintrag des Blätterstapels** (E‑216): der Cursor der Seite, auf die
 * „Vor" geführt hat, und wie viele Zeilen die Seiten **davor** zusammen
 * geliefert haben.
 *
 * **Die Zahl reist mit dem Cursor**, statt aus Seitentiefe mal Seitengröße
 * gerechnet zu werden. Die Seitengröße gehört dem Backend; das Frontend schickt
 * keine und kennt keine (`api.ts`). Dass heute jede Seite vor der letzten genau
 * so viele Zeilen trägt, wie die Seitengröße sagt, ist im Backend garantiert
 * (`Seite.aus`) und nicht hier — gezählt wird, was ankam.
 */
export type Stapeleintrag = { cursor: string; davor: number };

/**
 * **Die Treffer bis hier — der Σ unter der Liste** (E‑216,
 * `docs/nachrichtenliste.md` §8.3).
 *
 * **Keine Gesamtzahl** (Regel L2, `docs/nachrichtenliste.md` §1): `anzahl` sind
 * die tatsächlich gelieferten Zeilen von Seite eins bis einschließlich der
 * angezeigten. **Genau** ist sie erst, wenn es keine weitere Seite gibt; bis
 * dahin steht sie als „mehr als".
 */
export type Treffer = { anzahl: number; genau: boolean };

/** Was eine Seite zum Blättern und Zählen beiträgt — die Felder aus `Seite`. */
type Seitenumfang = { items: readonly unknown[]; nextCursor: string | null; hasMore: boolean };

function bisHier(stapel: Stapeleintrag[], seite: Pick<Seitenumfang, "items">): number {
  return (stapel.at(-1)?.davor ?? 0) + seite.items.length;
}

/** Der Σ zur angezeigten Seite: die Zeilen davor aus dem Stapel, dazu ihre eigenen. */
export function trefferBisHier(
  stapel: Stapeleintrag[],
  seite: Pick<Seitenumfang, "items" | "hasMore">,
): Treffer {
  return { anzahl: bisHier(stapel, seite), genau: !seite.hasMore };
}

/**
 * **Ein Schritt durch die Liste** — und wie der Stapel danach aussieht.
 *
 * - **Vor** legt den Cursor der nächsten Seite ab, zusammen mit den Zeilen bis
 *   einschließlich der angezeigten. Ohne nächsten Cursor bleibt der Stapel
 *   derselbe.
 * - **Zurück** nimmt den obersten Eintrag weg; der Σ zeigt danach wieder die
 *   Zahl bis zu jener Seite.
 * - **Ein neuer Filter** beginnt auf Seite eins, mit leerem Stapel: Der Cursor
 *   der alten Abfrage trägt einen Zeitpunkt, der im neuen Fenster nichts zu
 *   suchen hat (`cursor-ungueltig`), und die gezählten Zeilen gehörten zu einer
 *   anderen Liste.
 */
export type Blaetterschritt =
  | { art: "vor"; seite: Pick<Seitenumfang, "items" | "nextCursor"> }
  | { art: "zurueck" }
  | { art: "neuerFilter" };

export function blaettere(stapel: Stapeleintrag[], schritt: Blaetterschritt): Stapeleintrag[] {
  switch (schritt.art) {
    case "vor":
      return schritt.seite.nextCursor === null
        ? stapel
        : [...stapel, { cursor: schritt.seite.nextCursor, davor: bisHier(stapel, schritt.seite) }];
    case "zurueck":
      return stapel.slice(0, -1);
    case "neuerFilter":
      return [];
  }
}
