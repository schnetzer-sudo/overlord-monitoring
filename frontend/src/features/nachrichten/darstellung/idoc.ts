import { vorspannEnde } from "./vorspann";

/**
 * **IDoc (Flatfile)** — ein Satz je Zeile, Datensätze nach Hierarchieebene
 * eingerückt (`docs/dateiansicht-darstellung.md` §3).
 *
 * ## Erkennung
 *
 * Der Text beginnt mit `EDI_DC40`, und jeder Datensatz beginnt mit einem
 * Segmentnamen — Großbuchstaben, Ziffern, `_` oder `/`, linksbündig auf 30
 * Stellen aufgefüllt — und trägt an Stelle 62 und 63 eine zweistellige
 * Hierarchieebene. Mit Zeilenumbrüchen ist jede Zeile ein Satz; ohne sie wird
 * nach den festen Längen geteilt: Kontrollsatz `EDI_DC40` 524 Zeichen,
 * Datensatz 1063 (Segmentname 30, Mandant 3, IDoc-Nummer 16, Segmentnummer 6,
 * übergeordnetes Segment 6, Hierarchieebene 2, Daten 1000).
 *
 * ## Einrückung
 *
 * Datensätze werden um zwei Leerzeichen je Ebene eingerückt, gerechnet ab der
 * kleinsten vorkommenden Ebene; Kontrollsätze nie. Eingefügt wird nichts als
 * dieser Leerraum am Zeilenanfang (E‑197) — und weil kein Satz mit einem
 * Leerzeichen beginnt, lässt er sich Zeichen für Zeichen wieder entfernen.
 *
 * **Belegvermerk (Regel L10):** Längen und Stellen stammen aus der
 * SAP-Satzstruktur `EDI_DC40` und `EDI_DD40` und sind an keiner Datei dieses
 * Bestands geprüft (offener Punkt 196).
 */

export const LAENGE_KONTROLLSATZ = 524;
export const LAENGE_DATENSATZ = 1063;

const KONTROLLSATZ = "EDI_DC40";
const SEGMENTNAME = /^[A-Z0-9_/]+ *$/;
const BREITE_SEGMENTNAME = 30;
/** 0-basiert: Stelle 62 und 63 sind Index 61 und 62. */
const STELLE_EBENE = 61;

type Satz = { text: string; ebene: number | null };

function abschluss(kern: string): { kern: string; schluss: string } {
  if (kern.endsWith("\r\n")) {
    return { kern: kern.slice(0, -2), schluss: "\r\n" };
  }
  if (kern.endsWith("\n")) {
    return { kern: kern.slice(0, -1), schluss: "\n" };
  }
  return { kern, schluss: "" };
}

/** Der Satz, wenn er einer ist — Kontrollsatz ohne Ebene, Datensatz mit —, sonst `null`. */
function pruefeSatz(satz: string): Satz | null {
  if (satz.startsWith(KONTROLLSATZ)) {
    return { text: satz, ebene: null };
  }
  if (satz.length < STELLE_EBENE + 2) {
    return null;
  }
  if (!SEGMENTNAME.test(satz.slice(0, BREITE_SEGMENTNAME))) {
    return null;
  }
  const ebene = satz.slice(STELLE_EBENE, STELLE_EBENE + 2);
  if (!/^\d\d$/.test(ebene)) {
    return null;
  }
  return { text: satz, ebene: Number(ebene) };
}

type Aufteilung = { vorspann: string; saetze: Satz[]; schluss: string };

function teileIdoc(text: string): Aufteilung | null {
  const anfang = vorspannEnde(text);
  if (!text.startsWith(KONTROLLSATZ, anfang)) {
    return null;
  }
  const vorspann = text.slice(0, anfang);
  const { kern, schluss } = abschluss(text.slice(anfang));

  const saetze: Satz[] = [];

  if (/[\r\n]/.test(kern)) {
    // Zeile für Zeile. Das `\r` einer `\r\n`-Folge bleibt am Satz und geht
    // unverändert mit; geprüft wird die Zeile ohne es. Eine leere Zeile ist
    // kein Satz und bleibt, wie sie ist.
    for (const zeile of kern.split("\n")) {
      const ohneCr = zeile.endsWith("\r") ? zeile.slice(0, -1) : zeile;
      if (ohneCr === "") {
        saetze.push({ text: zeile, ebene: null });
        continue;
      }
      const satz = pruefeSatz(ohneCr);
      if (satz === null) {
        return null;
      }
      saetze.push({ text: zeile, ebene: satz.ebene });
    }
    return { vorspann, saetze, schluss };
  }

  let i = 0;
  while (i < kern.length) {
    const laenge = kern.startsWith(KONTROLLSATZ, i) ? LAENGE_KONTROLLSATZ : LAENGE_DATENSATZ;
    const rohsatz = kern.slice(i, i + laenge);
    if (rohsatz.length !== laenge) {
      return null;
    }
    const satz = pruefeSatz(rohsatz);
    if (satz === null) {
      return null;
    }
    saetze.push(satz);
    i += laenge;
  }
  return { vorspann, saetze, schluss };
}

export function erkenneIdoc(text: string): boolean {
  return teileIdoc(text) !== null;
}

/** Ein Satz je Zeile, Datensätze nach Ebene eingerückt — oder `null`, wenn der Text kein IDoc-Flatfile ist. */
export function formatiereIdoc(text: string): string | null {
  const aufteilung = teileIdoc(text);
  if (aufteilung === null) {
    return null;
  }
  // Kein `Math.min(...ebenen)`: Bei Zehntausenden Sätzen überschritte die
  // Argumentliste die Grenze der Laufzeit.
  let kleinste = Number.POSITIVE_INFINITY;
  for (const satz of aufteilung.saetze) {
    if (satz.ebene !== null && satz.ebene < kleinste) {
      kleinste = satz.ebene;
    }
  }

  const zeilen = aufteilung.saetze.map((satz) =>
    satz.ebene === null ? satz.text : "  ".repeat(satz.ebene - kleinste) + satz.text,
  );
  return aufteilung.vorspann + zeilen.join("\n") + aufteilung.schluss;
}
