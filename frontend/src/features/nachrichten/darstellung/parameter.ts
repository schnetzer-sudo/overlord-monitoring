import { createParser } from "nuqs";

import { DARSTELLUNG_VORGABE, istDarstellung, type Darstellung } from "./arten";

/**
 * **Der Parameter `darstellung` in der Adresse** (E‑202) — Werte `original`,
 * `edifact`, `x12`, `vda`, `idoc`, `xml`, `json`, `hex`, Standard `original`.
 *
 * Ein unbekannter Wert ergibt das Original: Der Parser liefert `null`, und
 * `withDefault` setzt die Vorgabe. Was man sieht, muss man teilen können —
 * eine kopierte Adresse mit `darstellung=edifact` öffnet dieselbe Darstellung.
 *
 * **Bewusst ohne `clearOnDefault: false`.** Die Regel aus
 * `docs/frontend-grundlagen.md` §8 lautet: *Ein Standardwert, der etwas
 * weglässt, gehört in die URL; einer, der etwas setzt, nicht.* Das Original
 * lässt nichts weg — es ist die Datei, wie sie ist —, also verschwindet
 * `darstellung=original` aus der Adresse, wie `nuqs` es von selbst tut.
 *
 * Verweise **in** die Ansicht tragen den Parameter nicht
 * (`docs/rohdaten-frontend.md` §8): Ein geteilter Verweis auf eine Datei
 * handelt von der Datei. Wo keine Auswahl steht (Protokolle, die fünf
 * inhaltslosen Zustände), ist der Parameter wirkungslos.
 *
 * Dieses Modul ist frei von React; die Bindung steht in `../hooks.ts`.
 */
export const parseAsDarstellung = createParser<Darstellung>({
  parse: (wert) => (istDarstellung(wert) ? wert : null),
  serialize: (wert) => wert,
});

export const DARSTELLUNG_PARAMETER = {
  darstellung: parseAsDarstellung.withDefault(DARSTELLUNG_VORGABE),
};
