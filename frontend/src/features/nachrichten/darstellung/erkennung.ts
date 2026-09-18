import { FORMATE, type Format } from "./arten";
import { erkenneEdifact } from "./edifact";
import { erkenneIdoc } from "./idoc";
import { erkenneJson } from "./json";
import { erkenneVda } from "./vda";
import { erkenneX12 } from "./x12";
import { erkenneXml } from "./xml";

/**
 * **Die Erkennung — nur über feste Syntaxmerkmale, höchstens ein Treffer**
 * (E‑199, `docs/dateiansicht-darstellung.md` §3).
 *
 * Das erkannte Format wird in der Auswahl **vorgemerkt** und nicht angewandt
 * (E‑195): Beim Öffnen steht immer das Original. Trifft kein Merkmal zu, ist
 * nichts vorgemerkt — eine Datei, die keines der sechs Merkmale trägt, wird
 * nicht erraten (Regel Q4). Original und Hex werden nie erkannt.
 *
 * Die sechs Merkmale schließen einander schon am ersten Zeichen nach dem
 * Vorspann aus: `UNA`/`UNB` · `ISA` · drei Ziffern · `EDI_DC40` · `<` · `{`
 * oder `[`. Geprüft wird trotzdem jedes gegen jedes
 * (`tests/darstellung.test.ts`, Kreuzprobe).
 */
const ERKENNER: Record<Format, (text: string) => boolean> = {
  edifact: erkenneEdifact,
  x12: erkenneX12,
  vda: erkenneVda,
  idoc: erkenneIdoc,
  xml: erkenneXml,
  json: erkenneJson,
};

/** Das eine erkannte Format — oder `null`. */
export function erkenneFormat(text: string): Format | null {
  const treffer = FORMATE.filter((format) => ERKENNER[format](text));
  return treffer.length === 1 ? treffer[0]! : null;
}

/** Für die Kreuzprobe: welche Erkennungen auf den Text anschlagen. */
export function erkannteFormate(text: string): Format[] {
  return FORMATE.filter((format) => ERKENNER[format](text));
}
