/**
 * **Die Darstellungen der Dateiansicht** — die acht Einträge der Auswahl, in
 * der Reihenfolge, in der sie dort stehen (`docs/dateiansicht-darstellung.md`,
 * E‑193): das Original, die sechs erkennbaren Formate, Hex.
 *
 * Dieses Modul ist frei von React. Was hier steht, sind Werte und Typen; die
 * Regeln je Darstellung liegen in je einer eigenen Datei daneben, die Erkennung
 * in `erkennung.ts`, die Anwendung in `index.ts`.
 */
export const DARSTELLUNGEN = [
  "original",
  "edifact",
  "x12",
  "vda",
  "idoc",
  "xml",
  "json",
  "hex",
] as const;

export type Darstellung = (typeof DARSTELLUNGEN)[number];

/** Beim Öffnen steht immer das Original — nichts wird ohne Wahl des Nutzers angewandt (E‑195). */
export const DARSTELLUNG_VORGABE: Darstellung = "original";

/**
 * Die sechs Formate, die erkannt werden können. **Original und Hex werden nie
 * erkannt**: Das eine ist die Vorgabe, das andere passt auf jede Datei aus
 * Bytes.
 */
export const FORMATE = ["edifact", "x12", "vda", "idoc", "xml", "json"] as const;

export type Format = (typeof FORMATE)[number];

export function istDarstellung(wert: string): wert is Darstellung {
  return (DARSTELLUNGEN as readonly string[]).includes(wert);
}
