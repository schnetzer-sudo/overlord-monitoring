import { expect } from "vitest";

/**
 * **Ein sehr kleiner CSS-Leser** — und es gibt genau einen davon.
 *
 * ## Woher er kommt und warum er hier steht *(03.09.2026)*
 *
 * Er ist am 01.09.2026 in `tests/dichte.test.ts` entstanden, und der Anlass war
 * ein Befund: Die erste Fassung jenes Tests war eine Textsuche, und von 23
 * Mutanten blieben **neun grün** — darunter „die vier Stufenregeln löschen und
 * als Kommentar stehen lassen". Der Grund war immer derselbe: Sie prüfte, dass
 * Zeichenfolgen **vorkommen**, nie, dass sie **wirken**
 * (`docs/dichte-umschalter.md` §7).
 *
 * Mit `tests/farbkontrast.test.ts` bekam er am 03.09.2026 einen zweiten
 * Verbraucher. **Kopiert worden ist er dabei nicht.** Zwei Leser wären zwei
 * Pflegestellen und damit genau der Fehler, gegen den das ganze Testwerk dieses
 * Projekts gerichtet ist — die Verschärfung, die der eine bekommt, fehlte dem
 * anderen, und niemand sähe es. Er ist deshalb hierher gewandert; beide Tests
 * lesen denselben.
 *
 * **Der Beleg, dass die Verlagerung nichts geändert hat**, sind die Zusicherungen
 * von `tests/dichte.test.ts`: Sie laufen unverändert durch, und ihre
 * Aufrufstellen sind Zeichen für Zeichen dieselben geblieben — deshalb gibt
 * {@link stilblatt} die Helfer als Bündel zurück und nicht als vier einzelne
 * Ausfuhren mit neuer Signatur.
 *
 * ## Was er kann, und was ausdrücklich nicht
 *
 * Er zerlegt eine Stilblattdatei in Regeln **mit ihrer Verschachtelung** und
 * beantwortet damit die eine Frage, die eine Regex nicht beantworten kann: *in
 * welchem Block steht das?* Er ist **kein** CSS-Parser: Er kennt keine
 * Kaskade, keine Spezifität, keine `@import`-Auflösung und rechnet keine
 * Farbe aus. Was ein Test darüber hinaus braucht, rechnet der Test.
 */

export type Regel = {
  /** Der Selektor bzw. die At-Regel, etwa `:root` oder `@media (pointer: coarse)`. */
  selektor: string;
  /** Die umschließenden Selektoren, von außen nach innen. */
  pfad: readonly string[];
  /** Nur die eigenen Deklarationen — verschachtelte Blöcke sind entfernt. */
  eigene: string;
};

/**
 * Ersetzt jeden Kommentar durch **gleich viele Leerzeichen**.
 *
 * Gleich viele, damit sich keine Position verschiebt — und ersetzt statt
 * übersprungen, damit ein auskommentierter Block nicht mehr wie ein vorhandener
 * aussieht. Genau daran ist die erste Fassung von `tests/dichte.test.ts`
 * gescheitert.
 */
export function ohneKommentare(css: string): string {
  return css.replace(/\/\*[\s\S]*?\*\//g, (treffer) => " ".repeat(treffer.length));
}

function passendeKlammer(css: string, auf: number): number {
  let tiefe = 0;
  for (let i = auf; i < css.length; i++) {
    if (css[i] === "{") tiefe++;
    else if (css[i] === "}" && --tiefe === 0) return i;
  }
  throw new Error(`Keine schließende Klammer zu Position ${auf} — ist die Datei unvollständig?`);
}

/** Wirft jeden verschachtelten Block weg; übrig bleiben die eigenen Deklarationen. */
function eigeneDeklarationen(rumpf: string): string {
  let ergebnis = "";
  let tiefe = 0;
  for (const zeichen of rumpf) {
    if (zeichen === "{") tiefe++;
    else if (zeichen === "}") tiefe--;
    else if (tiefe === 0) ergebnis += zeichen;
  }
  return ergebnis;
}

function lies(css: string, von: number, bis: number, pfad: readonly string[], hinein: Regel[]) {
  let i = von;
  let kopfAnfang = von;
  while (i < bis) {
    const zeichen = css[i];
    if (zeichen === "{") {
      const selektor = css.slice(kopfAnfang, i).trim().replace(/\s+/g, " ");
      const ende = passendeKlammer(css, i);
      hinein.push({ selektor, pfad, eigene: eigeneDeklarationen(css.slice(i + 1, ende)) });
      lies(css, i + 1, ende, [...pfad, selektor], hinein);
      i = ende + 1;
      kopfAnfang = i;
    } else if (zeichen === ";") {
      i++;
      kopfAnfang = i;
    } else {
      i++;
    }
  }
}

/**
 * Steht die Regel unter einer Bedingung, die am Bildschirm nicht immer gilt?
 *
 * Drei Formen: eine Medienabfrage, ein `@supports` — und der **Dunkelzustand**.
 * Der hieß bis zum 03.09.2026 `.dark` und heißt seither `[data-thema="dunkel"]`
 * bzw. `[data-thema="system"]` (E‑59). **Beide Schreibweisen bleiben hier
 * stehen**, und zwar nicht aus Nachlässigkeit: Die Zusicherung, die daran
 * hängt, lautet *„eine Dichtestufe unter dem Dunkelzustand wirkt am Bildschirm
 * nie"*, und sie soll auch dann greifen, wenn jemand die alte Klasse wieder
 * einführt.
 */
export function bedingt(regel: Regel): string | null {
  return (
    [...regel.pfad, regel.selektor].find(
      (teil) =>
        teil.startsWith("@media") ||
        teil.startsWith("@supports") ||
        /\.dark\b/.test(teil) ||
        /\[data-thema/.test(teil),
    ) ?? null
  );
}

/**
 * Der Wert eines Tokens in den eigenen Deklarationen einer Regel — **genau
 * einmal**.
 *
 * Bei zwei Deklarationen gewänne in der Kaskade die zweite, und ein Textleser
 * läse die erste. Das ist einer der Mutanten, an denen die erste Fassung von
 * `tests/dichte.test.ts` gescheitert ist.
 */
export function wert(regel: Regel, token: string): string {
  const treffer = [...regel.eigene.matchAll(new RegExp(`${token}\\s*:\\s*([^;]+);`, "g"))];
  expect(
    treffer.length,
    `${token} steht ${treffer.length}-mal in \`${regel.selektor}\`. ` +
      `Bei zwei Deklarationen gewinnt in der Kaskade die zweite, und dieser Test läse die erste.`,
  ).toBe(1);
  return treffer[0][1].trim();
}

/**
 * Zerlegt ein Stilblatt und gibt die Helfer als **Bündel** zurück.
 *
 * Als Bündel und nicht als vier freie Ausfuhren, damit `setzer(token)` weiter
 * mit einem Argument aufgerufen wird — die Aufrufstellen in
 * `tests/dichte.test.ts` sollten bei der Verlagerung unverändert bleiben, sonst
 * wäre nicht mehr zu sehen, dass sich dort nichts geändert hat.
 */
export function stilblatt(cssRoh: string) {
  const css = ohneKommentare(cssRoh);
  const regeln: Regel[] = [];
  lies(css, 0, css.length, [], regeln);

  return {
    /** Die Datei ohne Kommentare, positionsgleich zum Original. */
    css,
    /** Alle Regeln, mit ihrer Verschachtelung. */
    regeln: regeln as readonly Regel[],
    /** Alle Regeln, deren **eigene** Deklarationen das Token setzen. */
    setzer: (token: string): Regel[] => {
      const muster = new RegExp(`(^|[;\\s])${token}\\s*:`);
      return regeln.filter((regel) => muster.test(regel.eigene));
    },
    wert,
    bedingt,
  };
}
