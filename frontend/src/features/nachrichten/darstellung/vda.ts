import { vorspannEnde } from "./vorspann";

/**
 * **VDA** — die Festlängenformate: Sätze zu 128 Zeichen, die Satzart in den
 * ersten drei Stellen als Ziffern (`docs/dateiansicht-darstellung.md` §3).
 *
 * Zwei Gestalten, und beide werden erkannt:
 *
 * - **Mit Zeilenumbrüchen**, wenn jede nicht leere Zeile ohne Zeilenende genau
 *   128 Zeichen hat und mit drei Ziffern beginnt. Die Darstellung gleicht dann
 *   dem Original.
 * - **Ohne Zeilenumbrüche**, wenn die Länge ohne abschließenden Zeilenumbruch
 *   ein Vielfaches von 128 ist und jeder Block mit drei Ziffern beginnt. Dann
 *   Umbruch nach je 128 Zeichen.
 *
 * Ein abschließender Zeilenumbruch ist erlaubt und geht unverändert mit.
 *
 * **Belegvermerk (Regel L10):** 128 *Zeichen*, nicht Bytes — für ASCII und
 * ISO-8859-1 dasselbe; ob im Bestand eine VDA-Datei als UTF-8 mit Bytes über
 * `0x7F` vorkommt, ist nicht erhoben (offener Punkt 197).
 */

export const SATZLAENGE_VDA = 128;

const SATZART = /^\d{3}/;

type Aufteilung = {
  /** Der Vorspann, unverändert. */
  vorspann: string;
  /** Die Sätze in Reihenfolge — bei Zeilen ohne ihr Zeilenende, aber mit dem `\r` einer `\r\n`-Folge nicht. */
  saetze: string[];
  /** Ob die Sätze im Original schon je auf einer Zeile stehen. */
  mitZeilen: boolean;
  /** Der abschließende Zeilenumbruch, falls vorhanden. */
  schluss: string;
};

function abschluss(kern: string): { kern: string; schluss: string } {
  if (kern.endsWith("\r\n")) {
    return { kern: kern.slice(0, -2), schluss: "\r\n" };
  }
  if (kern.endsWith("\n")) {
    return { kern: kern.slice(0, -1), schluss: "\n" };
  }
  return { kern, schluss: "" };
}

function teileVda(text: string): Aufteilung | null {
  const anfang = vorspannEnde(text);
  const vorspann = text.slice(0, anfang);
  const { kern, schluss } = abschluss(text.slice(anfang));
  if (kern.length === 0) {
    return null;
  }

  if (/[\r\n]/.test(kern)) {
    const zeilen = kern.split(/\r?\n/);
    const saetze = zeilen.filter((zeile) => zeile !== "");
    if (saetze.length === 0) {
      return null;
    }
    for (const satz of saetze) {
      if (satz.length !== SATZLAENGE_VDA || !SATZART.test(satz)) {
        return null;
      }
    }
    return { vorspann, saetze: zeilen, mitZeilen: true, schluss };
  }

  if (kern.length % SATZLAENGE_VDA !== 0) {
    return null;
  }
  const saetze: string[] = [];
  for (let i = 0; i < kern.length; i += SATZLAENGE_VDA) {
    const satz = kern.slice(i, i + SATZLAENGE_VDA);
    if (!SATZART.test(satz)) {
      return null;
    }
    saetze.push(satz);
  }
  return { vorspann, saetze, mitZeilen: false, schluss };
}

export function erkenneVda(text: string): boolean {
  return teileVda(text) !== null;
}

/** Ein Satz je Zeile — oder `null`, wenn der Text kein VDA-Festlängenformat ist. */
export function formatiereVda(text: string): string | null {
  const aufteilung = teileVda(text);
  if (aufteilung === null) {
    return null;
  }
  if (aufteilung.mitZeilen) {
    // Die Sätze stehen schon je auf einer Zeile; die Darstellung gleicht dem
    // Original — auch mit `\r\n` und leeren Zeilen, die unverändert bleiben.
    return text;
  }
  return aufteilung.vorspann + aufteilung.saetze.join("\n") + aufteilung.schluss;
}
