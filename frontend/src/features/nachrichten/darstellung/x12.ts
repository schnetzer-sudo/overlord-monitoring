import { umbrecheNachSegmentende } from "./segmente";
import { vorspannEnde } from "./vorspann";

/**
 * **ANSI X12** — Umbruch nach jedem Segmentende, sonst wie EDIFACT; ein
 * Freigabezeichen gibt es nicht (`docs/dateiansicht-darstellung.md` §3).
 *
 * ## Erkennung über die feste Länge des ISA-Segments
 *
 * Der Text beginnt mit `ISA`, und das ISA-Segment hat seine feste Länge. Die
 * Feldbreiten ISA01 bis ISA16 sind 2, 10, 2, 10, 2, 15, 2, 15, 6, 4, 1, 5, 9,
 * 1, 1, 1. Daraus folgt, 1-basiert: der Elementtrenner an Stelle 4 und vor
 * jedem weiteren Feld an seiner festen Stelle, der Komponententrenner an
 * Stelle 105, das Segmentende an Stelle 106. Alle sechzehn Trennerstellen
 * müssen dasselbe Zeichen tragen, und die drei Trennzeichen müssen
 * voneinander verschieden sein (E‑199).
 */

const FELDBREITEN = [2, 10, 2, 10, 2, 15, 2, 15, 6, 4, 1, 5, 9, 1, 1, 1] as const;

/** Die 0-basierten Stellen des Elementtrenners: vor ISA01 … vor ISA16. */
const TRENNERSTELLEN: readonly number[] = (() => {
  const stellen: number[] = [];
  let stelle = 3;
  for (const breite of FELDBREITEN) {
    stellen.push(stelle);
    stelle += 1 + breite;
  }
  return stellen;
})();

/** 0-basiert: Komponententrenner an 104, Segmentende an 105. */
const STELLE_KOMPONENTE = 104;
const STELLE_SEGMENTENDE = 105;
const LAENGE_ISA = STELLE_SEGMENTENDE + 1;

type Trennzeichen = { ab: number; segmentende: string };

function trennzeichen(text: string): Trennzeichen | null {
  const anfang = vorspannEnde(text);
  if (!text.startsWith("ISA", anfang) || text.length < anfang + LAENGE_ISA) {
    return null;
  }
  const element = text.charAt(anfang + 3);
  for (const stelle of TRENNERSTELLEN) {
    if (text.charAt(anfang + stelle) !== element) {
      return null;
    }
  }
  const komponente = text.charAt(anfang + STELLE_KOMPONENTE);
  const segmentende = text.charAt(anfang + STELLE_SEGMENTENDE);
  if (element === komponente || element === segmentende || komponente === segmentende) {
    return null;
  }
  return { ab: anfang, segmentende };
}

export function erkenneX12(text: string): boolean {
  return trennzeichen(text) !== null;
}

/** Der Text mit einem Umbruch nach jedem Segmentende — oder `null`, wenn er kein X12 ist. */
export function formatiereX12(text: string): string | null {
  const t = trennzeichen(text);
  if (t === null) {
    return null;
  }
  return umbrecheNachSegmentende(text, t.ab, t.segmentende, null);
}
