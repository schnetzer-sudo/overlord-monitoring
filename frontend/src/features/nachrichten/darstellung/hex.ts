/**
 * **Hex** — wie `hexdump -C`, aber ohne Zusammenfassung wiederholter Zeilen und
 * ohne Schlusszeile (`docs/dateiansicht-darstellung.md` §3): Versatz
 * achtstellig, 16 Bytes je Zeile in Kleinbuchstaben mit Lücke nach acht,
 * rechts zwischen `|` die Zeichen `0x20` bis `0x7E`, alle anderen als `.`.
 *
 * ## Die Bytes sind die Zeichencodes — und nur dort, wo das stimmt
 *
 * `ISO-8859-1` bildet jedes Byte auf genau ein Zeichen ab, ASCII ebenso. Für
 * beide Kodierungen ist der Zeichencode das Byte, und das Backend liefert den
 * Text außer der Dekodierung unverändert (`Inhaltseinstufung.stufeEin`). Für
 * `UTF_8` gilt das nicht: Ein `ä` ist dort ein Zeichen mit dem Code `0xE4`,
 * aber zwei Bytes `C3 A4` — der Zeichencode wäre ein erfundenes Byte —, und
 * das Backend entfernt außerdem eine führende BOM aus dem Text. Hex ist
 * deshalb **nur bei `ASCII` und `ISO_8859_1`** eine Darstellung der Datei
 * (E‑204); bei jeder anderen Kodierung und bei jedem Zeichencode über `0xFF`
 * bleibt es beim Original mit Vermerk — nie ein erfundenes Byte.
 *
 * ## Gekappt auf 131.072 Bytes (E‑203)
 *
 * 8.192 Zeilen zu 78 Zeichen plus 8.191 Zeilenumbrüche sind 647.167 Zeichen —
 * dieselbe Größenordnung wie die größte gemessene Rohanzeige (609.995 Bytes,
 * M60), deren Verhalten im `<pre>` selbst noch ungemessen ist
 * (`docs/rohdaten-frontend.md` §11, Punkt 9). Mehr Hex als Rohtext wäre
 * nicht zu begründen.
 */

export const HEX_GRENZE_BYTES = 131_072;

const BYTES_JE_ZEILE = 16;

/** Die 256 Bytes als zwei Kleinbuchstaben, einmal gebaut. */
const HEX: readonly string[] = Array.from({ length: 256 }, (_, wert) =>
  wert.toString(16).padStart(2, "0"),
);

/** Die Kodierungen, in denen der Zeichencode das Byte ist. */
const BYTEGLEICH: readonly string[] = ["ASCII", "ISO_8859_1"];

export type Hexdarstellung = {
  text: string;
  /** Ob die Darstellung an `HEX_GRENZE_BYTES` endet. */
  gekappt: boolean;
};

/**
 * Die Hex-Darstellung des Texts — oder `null`, wenn die Zeichencodes nicht die
 * Bytes der Datei sind: bei jeder Kodierung außer ASCII und ISO-8859-1, und
 * bei einem Zeichencode über `0xFF`.
 */
export function formatiereHex(text: string, kodierung: string | null): Hexdarstellung | null {
  if (kodierung === null || !BYTEGLEICH.includes(kodierung)) {
    return null;
  }
  for (let i = 0; i < text.length; i += 1) {
    if (text.charCodeAt(i) > 0xff) {
      return null;
    }
  }

  const laenge = Math.min(text.length, HEX_GRENZE_BYTES);
  const zeilen: string[] = [];
  for (let versatz = 0; versatz < laenge; versatz += BYTES_JE_ZEILE) {
    let hex = "";
    let zeichen = "";
    for (let k = 0; k < BYTES_JE_ZEILE; k += 1) {
      if (k === BYTES_JE_ZEILE / 2) {
        hex += " ";
      }
      const stelle = versatz + k;
      if (stelle < laenge) {
        const wert = text.charCodeAt(stelle);
        hex += HEX[wert] + " ";
        zeichen += wert >= 0x20 && wert <= 0x7e ? text.charAt(stelle) : ".";
      } else {
        hex += "   ";
      }
    }
    zeilen.push(`${versatz.toString(16).padStart(8, "0")}  ${hex} |${zeichen}|`);
  }

  return { text: zeilen.join("\n"), gekappt: text.length > HEX_GRENZE_BYTES };
}
