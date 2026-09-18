/**
 * **Der Vorspann: vorangestellter Leerraum und eine UTF-8-BOM.**
 *
 * Beides wird bei der Erkennung übersprungen und in der Darstellung unverändert
 * ausgegeben (`docs/dateiansicht-darstellung.md` §3). Die BOM kommt in zwei
 * Gestalten an: als `U+FEFF`, wenn das Backend den Text als UTF-8 gelesen hat —
 * dort fällt sie zwar aus dem Text, aber ein Wert, den diese Oberfläche nicht
 * kennt, könnte sie tragen —, und als die drei Zeichen `ï»¿`, wenn die Bytes
 * `EF BB BF` als ISO-8859-1 gelesen wurden.
 */

const BOM = "\uFEFF";
const BOM_ALS_ISO_8859_1 = "\u00EF\u00BB\u00BF";

function istLeerraum(zeichen: string): boolean {
  return zeichen === " " || zeichen === "\t" || zeichen === "\r" || zeichen === "\n";
}

/** Die Stelle, an der der Inhalt beginnt — nach Leerraum und BOM in beliebiger Folge. */
export function vorspannEnde(text: string): number {
  let i = 0;
  for (;;) {
    if (text.startsWith(BOM, i)) {
      i += BOM.length;
    } else if (text.startsWith(BOM_ALS_ISO_8859_1, i)) {
      i += BOM_ALS_ISO_8859_1.length;
    } else if (i < text.length && istLeerraum(text.charAt(i))) {
      i += 1;
    } else {
      return i;
    }
  }
}

/** Ob das Zeichen ein Zeilenende ist — `\r` oder `\n`. */
export function istZeilenende(zeichen: string): boolean {
  return zeichen === "\r" || zeichen === "\n";
}
