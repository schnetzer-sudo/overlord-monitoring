import type { Artefaktart, Artefaktzustand } from "../api";
import { type Darstellung, type Format } from "./arten";
import { formatiereEdifact } from "./edifact";
import { formatiereHex } from "./hex";
import { formatiereIdoc } from "./idoc";
import { formatiereJson } from "./json";
import { formatiereVda } from "./vda";
import { formatiereX12 } from "./x12";
import { formatiereXml } from "./xml";

export { DARSTELLUNGEN, DARSTELLUNG_VORGABE, FORMATE, istDarstellung } from "./arten";
export type { Darstellung, Format } from "./arten";
export { erkenneFormat } from "./erkennung";
export { HEX_GRENZE_BYTES } from "./hex";

/**
 * **Die Darstellungswahl der Dateiansicht als reine Funktionen** — kein
 * Backendaufruf, kein Endpunkt, kein Feld, kein Audit-Ereignis (E‑196,
 * `docs/dateiansicht-darstellung.md`).
 *
 * Formatieren heißt hier: Struktur sichtbar machen, also Zeilenumbrüche und
 * Einrückung. Eine Darstellung fügt Leerraum ein oder entfernt Zeilenumbrüche,
 * ausschließlich außerhalb von Daten; jedes andere Zeichen bleibt, wie und wo
 * es ist (E‑197). Inhalte zu erklären gehört nicht dazu.
 *
 * Das Ergebnis ist ein einziger Text und bleibt genau ein Textknoten im
 * `<pre>` — die Sicherheitsregel aus `docs/rohdaten-frontend.md` §4 gilt
 * unverändert.
 */

const FORMATIERER: Record<Format, (text: string) => string | null> = {
  edifact: formatiereEdifact,
  x12: formatiereX12,
  vda: formatiereVda,
  idoc: formatiereIdoc,
  xml: formatiereXml,
  json: formatiereJson,
};

export type Darstellungsergebnis = {
  /** Der Text für das `<pre>` — formatiert, oder das Original, wenn die Darstellung nicht passt. */
  text: string;
  /** Die Datei passt nicht zur gewählten Darstellung; `text` ist das Original (E‑200). */
  passtNicht: boolean;
  /** Die Hex-Darstellung endet an `HEX_GRENZE_BYTES` (E‑203). */
  hexGekappt: boolean;
};

const ORIGINAL = (text: string): Darstellungsergebnis => ({
  text,
  passtNicht: false,
  hexGekappt: false,
});

/**
 * Der Text in der gewählten Darstellung.
 *
 * **Passt die Datei nicht zur Wahl — auch bei gekappter Anzeige —, kommt das
 * Original mit `passtNicht`, nie ein halb formatiertes Ergebnis** (E‑200).
 * `kodierung` braucht allein Hex: Nur bei ASCII und ISO-8859-1 sind die
 * Zeichencodes die Bytes (E‑204).
 */
export function stelleDar(
  text: string,
  darstellung: Darstellung,
  kodierung: string | null,
): Darstellungsergebnis {
  if (darstellung === "original") {
    return ORIGINAL(text);
  }
  if (darstellung === "hex") {
    const hex = formatiereHex(text, kodierung);
    return hex === null
      ? { text, passtNicht: true, hexGekappt: false }
      : { text: hex.text, passtNicht: false, hexGekappt: hex.gekappt };
  }
  const formatiert = FORMATIERER[darstellung](text);
  return formatiert === null
    ? { text, passtNicht: true, hexGekappt: false }
    : { text: formatiert, passtNicht: false, hexGekappt: false };
}

/**
 * Die drei Vermerke der Darstellungswahl, nach dem Muster der drei Vermerke
 * der Anzeige (`docs/rohdaten-frontend.md` §5). Jeder sagt, dass hier nicht
 * die Datei steht, wie sie ist — oder dass der Download etwas anderes liefert.
 *
 * | Vermerk | Auslöser | betrifft den Download |
 * |---|---|---|
 * | `PASST_NICHT` | die Datei passt nicht zur gewählten Darstellung; angezeigt ist das Original | nein |
 * | `HEX_GEKAPPT` | die Hex-Darstellung endet an der Grenze | nein |
 * | `DOWNLOAD_ORIGINAL` | eine Darstellung außer Original ist angewandt | **ja** — er liefert die Originaldatei, nicht die Darstellung (E‑201) |
 */
export type Darstellungsvermerk = "PASST_NICHT" | "HEX_GEKAPPT" | "DOWNLOAD_ORIGINAL";

/**
 * Die Vermerke zu einem Ergebnis, in Anzeigereihenfolge: zuerst, was gezeigt
 * wird, zuletzt, was der Download liefert.
 *
 * **Der Download-Vermerk steht nur, wo eine Darstellung tatsächlich angewandt
 * ist.** Passt sie nicht, steht das Original — und das ist auch die Datei, die
 * der Download liefert; ein Vermerk, der dort vor einem Unterschied warnte,
 * behauptete einen, den es nicht gibt.
 */
export function darstellungsvermerke(
  darstellung: Darstellung,
  ergebnis: Darstellungsergebnis,
): Darstellungsvermerk[] {
  const vermerke: Darstellungsvermerk[] = [];
  if (ergebnis.passtNicht) {
    vermerke.push("PASST_NICHT");
  }
  if (ergebnis.hexGekappt) {
    vermerke.push("HEX_GEKAPPT");
  }
  if (darstellung !== "original" && !ergebnis.passtNicht) {
    vermerke.push("DOWNLOAD_ORIGINAL");
  }
  return vermerke;
}

/**
 * Ob die Auswahl überhaupt im Baum steht: **nur Nutzdaten im Zustand
 * `ANZEIGBAR`** (E‑194). Bei Protokollen und in den fünf übrigen Zuständen
 * gibt es keine Auswahl; ebenso bei der leeren Datei, die als benannter
 * Zustand und nicht als Inhalt erscheint — dort gäbe es nichts darzustellen.
 */
export function darstellungWaehlbar(anzeige: {
  zustand: Artefaktzustand;
  art: Artefaktart;
  text: string;
}): boolean {
  return anzeige.zustand === "ANZEIGBAR" && anzeige.art === "NUTZDATEN" && anzeige.text !== "";
}
