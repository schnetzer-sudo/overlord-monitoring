import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";

import { describe, expect, it } from "vitest";

import { DICHTESTUFEN, STANDARDDICHTE, dichteAus, type Dichtestufe } from "@/dichte";
import { de } from "@/i18n/de";
import { en } from "@/i18n/en";

/**
 * Der Dichteumschalter — **rechnerisch geprüft, ohne Ansicht.**
 *
 * Vier Stufen stellen die Wurzelschriftgröße um und damit jedes `rem` des
 * Projekts. Was daran schiefgehen kann, geht **still** schief: Eine Stufe, die
 * in `globals.css` fehlt, sieht in der Oberfläche aus wie eine, die nichts tut;
 * ein `--dichte-beruehrung`, das mitskaliert, unterschreitet die Mindestfläche
 * am Finger, ohne dass irgendwo etwas fehlschlägt.
 *
 * Deshalb liest dieser Test die **Datei** und nicht ein Abbild davon — dieselbe
 * Bauform wie `tests/farbwerte.test.ts` und `tests/serverbausteine.test.ts`. Was
 * in `globals.css` steht, ist die Wahrheit; eine zweite Liste hier wäre eine
 * zweite Pflegestelle, die der ersten hinterherliefe.
 *
 * **Die Zahlen unten sind gerechnet und nicht hingeschrieben.** Es steht nirgends
 * `38.5` oder `49.5` in dieser Datei: Der Test wertet die Deklaration aus, die er
 * gelesen hat. Wer die Stufenwerte in `globals.css` ändert, bekommt hier ein
 * neues Ergebnis und keine veraltete Behauptung.
 *
 * ## Warum hier ein CSS-Leser steht und keine Handvoll Regexe *(01.09.2026)*
 *
 * **Die erste Fassung war eine Textsuche, und eine Gegenprüfung hat sie
 * zerlegt.** Von 23 Mutanten blieben **neun grün**, darunter drei, die den
 * Umschalter vollständig totlegen:
 *
 * | Mutant | alte Fassung |
 * |---|---|
 * | die vier Stufenregeln nach `@media print` verschoben | grün |
 * | die vier Stufenregeln unter `.dark` verschachtelt | grün |
 * | die vier Stufenregeln **gelöscht** und als Kommentar stehen gelassen | grün |
 * | `--dichte-wurzel` im `xs`-Block auskommentiert | grün |
 * | eine **zweite** `--dichte-wurzel: 300%` im `m`-Block | grün |
 * | `html { font-size: … }` nach `@media print` verschoben | grün |
 * | `--dichte-beruehrung` aus `:root` nach `@media print { :root }` | grün |
 * | `--spacing-beruehrung` auf ein anderes Token gezeigt | grün |
 *
 * Der Grund war immer derselbe: Die alte Fassung prüfte, dass Zeichenfolgen
 * **vorkommen**, nie, dass sie **wirken**. Ein Wertetest, kein Lagetest.
 *
 * **Deshalb steht unten ein kleiner CSS-Leser.** Er entfernt zuerst die
 * Kommentare, zerlegt die Datei dann in Regeln **mit ihrer Verschachtelung** und
 * beantwortet damit die Frage, die eine Regex nicht beantworten kann: *in
 * welchem Block steht das?* Geprüft wird seither beides — der Wert und die Lage.
 *
 * **Ein Wirkungstest im Browser wird er dadurch nicht.** Was die Regeln am
 * laufenden System tatsächlich bewirken, steht gemessen in
 * `docs/dichte-umschalter.md` §5 und nirgendwo sonst.
 */

const CSS_ROH = readFileSync(
  fileURLToPath(new URL("../src/app/globals.css", import.meta.url)),
  "utf8",
);

/**
 * Die Grundschriftgröße, gegen die Prozentangaben an der Wurzel rechnen.
 *
 * **Es ist eine Annahme und keine Messung** — es ist die Voreinstellung
 * praktisch jedes Browsers, aber ein Nutzer darf sie verstellen, und genau
 * deshalb steht in `globals.css` Prozent und keine feste Pixelzahl. Die
 * Zusicherung zur Mindestfläche wird unten trotzdem gegen **drei** Vorgaben
 * geprüft, nicht nur gegen diese eine.
 */
const BROWSERVORGABE = 16;

/** Die Mindestfläche am Finger, in Pixeln (`docs/visuelles-konzept.md` §5). */
const MINDESTFLAECHE = 44;

// ───────────────────────────────────────────────────────────────────────────
// Ein sehr kleiner CSS-Leser
// ───────────────────────────────────────────────────────────────────────────

/**
 * Ersetzt jeden Kommentar durch **gleich viele Leerzeichen**.
 *
 * Gleich viele, damit sich keine Position verschiebt — und ersetzt statt
 * übersprungen, damit ein auskommentierter Block nicht mehr wie ein vorhandener
 * aussieht. Genau daran ist die erste Fassung gescheitert.
 */
function ohneKommentare(css: string): string {
  return css.replace(/\/\*[\s\S]*?\*\//g, (treffer) => " ".repeat(treffer.length));
}

const CSS = ohneKommentare(CSS_ROH);

type Regel = {
  /** Der Selektor bzw. die At-Regel, etwa `:root` oder `@media (pointer: coarse)`. */
  selektor: string;
  /** Die umschließenden Selektoren, von außen nach innen. */
  pfad: readonly string[];
  /** Nur die eigenen Deklarationen — verschachtelte Blöcke sind entfernt. */
  eigene: string;
};

function passendeKlammer(css: string, auf: number): number {
  let tiefe = 0;
  for (let i = auf; i < css.length; i++) {
    if (css[i] === "{") tiefe++;
    else if (css[i] === "}" && --tiefe === 0) return i;
  }
  throw new Error(`Keine schließende Klammer zu Position ${auf} — ist globals.css unvollständig?`);
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

const REGELN: readonly Regel[] = (() => {
  const gesammelt: Regel[] = [];
  lies(CSS, 0, CSS.length, [], gesammelt);
  return gesammelt;
})();

/** Steht die Regel unter einer Bedingung, die am Bildschirm nicht immer gilt? */
function bedingt(regel: Regel): string | null {
  return (
    [...regel.pfad, regel.selektor].find(
      (teil) => teil.startsWith("@media") || teil.startsWith("@supports") || /\.dark\b/.test(teil),
    ) ?? null
  );
}

/** Alle Regeln, deren **eigene** Deklarationen das Token setzen. */
function setzer(token: string): Regel[] {
  const muster = new RegExp(`(^|[;\\s])${token}\\s*:`);
  return REGELN.filter((regel) => muster.test(regel.eigene));
}

/** Der Wert eines Tokens in den eigenen Deklarationen einer Regel — genau einmal. */
function wert(regel: Regel, token: string): string {
  const treffer = [...regel.eigene.matchAll(new RegExp(`${token}\\s*:\\s*([^;]+);`, "g"))];
  expect(
    treffer.length,
    `${token} steht ${treffer.length}-mal in \`${regel.selektor}\`. ` +
      `Bei zwei Deklarationen gewinnt in der Kaskade die zweite, und dieser Test läse die erste.`,
  ).toBe(1);
  return treffer[0][1].trim();
}

/**
 * Wertet eine CSS-Länge in Pixeln aus — `rem`, `px` und `max(…)` daraus.
 *
 * Bewusst **unvollständig**: Was diese Funktion nicht kennt, lässt sie
 * fehlschlagen statt es zu überspringen. Ein stiller Rückfall auf null wäre
 * hier das Schlimmste — der Test bestünde weiter und prüfte nichts mehr.
 */
function inPixeln(ausdruck: string, wurzel: number): number {
  const roh = ausdruck.trim();

  const max = /^max\(([^()]*)\)$/.exec(roh);
  if (max !== null) {
    return Math.max(...max[1].split(",").map((glied) => inPixeln(glied, wurzel)));
  }

  const rem = /^([0-9.]+)rem$/.exec(roh);
  if (rem !== null) {
    return Number(rem[1]) * wurzel;
  }

  const px = /^([0-9.]+)px$/.exec(roh);
  if (px !== null) {
    return Number(px[1]);
  }

  throw new Error(
    `\`${ausdruck}\` ist keine Länge, die dieser Test auswerten kann. ` +
      `Entweder ist die Deklaration in globals.css falsch, oder inPixeln() muss ` +
      `die neue Form lernen — stillschweigend übergangen wird sie nicht.`,
  );
}

/** Die Regel `html[data-dichte="…"]` einer Stufe. */
function stufenregel(stufe: string): Regel {
  const gefunden = REGELN.find((regel) =>
    new RegExp(`^html\\[data-dichte=["']?${stufe}["']?\\]$`).test(regel.selektor),
  );
  expect(gefunden, `Keine Regel html[data-dichte="${stufe}"] in globals.css`).toBeDefined();
  return gefunden as Regel;
}

/** Die Wurzelschriftgröße einer Stufe, in Pixeln, bei einer gegebenen Browservorgabe. */
function wurzelPx(stufe: Dichtestufe, vorgabe: number): number {
  const prozent = Number(wert(stufenregel(stufe), "--dichte-wurzel").replace("%", ""));
  return (vorgabe * prozent) / 100;
}

// ───────────────────────────────────────────────────────────────────────────

describe("Der CSS-Leser selbst", () => {
  it("findet überhaupt Regeln, und die bekannten darunter", () => {
    // Ohne diese Zusicherung bestünde jeder Test unten auch dann, wenn der
    // Leser nichts fände — dieselbe Vorsorge wie in `tests/farbwerte.test.ts`.
    expect(REGELN.length).toBeGreaterThan(8);
    expect(REGELN.map((r) => r.selektor)).toContain(":root");
    expect(REGELN.map((r) => r.selektor)).toContain("@theme inline");
  });

  it("entfernt Kommentare, statt sie zu übersehen", () => {
    // Der Mutant, an dem die erste Fassung gescheitert ist: Regeln löschen und
    // als Kommentar stehen lassen. Gleiche Länge, damit keine Position wandert.
    expect(CSS).not.toContain("/*");
    expect(CSS.length).toBe(CSS_ROH.length);
  });
});

describe("Die vier Stufen in globals.css", () => {
  it("sind genau die vier, die der Code kennt — in beide Richtungen", () => {
    // Eine Stufe im Code ohne Regel im CSS wäre ein Menüeintrag, der nichts
    // tut. Eine Regel ohne Stufe im Code wäre eine Größe, die niemand
    // erreichen kann. Beides fällt nur auf, wenn beide Seiten verglichen
    // werden.
    const imCss = REGELN.map((r) => /^html\[data-dichte=["']?([^"'\]]+)["']?\]$/.exec(r.selektor))
      .filter((treffer): treffer is RegExpExecArray => treffer !== null)
      .map((treffer) => treffer[1]);
    expect([...imCss].sort()).toEqual([...DICHTESTUFEN].sort());
  });

  it.each(DICHTESTUFEN)("hängt Stufe %s unbedingt am Wurzelelement", (stufe) => {
    // **Die Lage, nicht nur der Wert.** Eine Stufenregel in `@media print` oder
    // unter `.dark` steht da und wirkt am Bildschirm nie.
    const regel = stufenregel(stufe);
    expect(
      bedingt(regel),
      `html[data-dichte="${stufe}"] steht unter \`${bedingt(regel)}\` und wirkt damit nicht ` +
        `in jeder Lage. Die Stufen gehören auf die oberste Ebene.`,
    ).toBeNull();
    expect(wert(regel, "--dichte-wurzel")).toMatch(/^[0-9.]+%$/);
  });

  it("wirkt über die Wurzelschriftgröße — unbedingt", () => {
    // Ohne diese Regel setzen vier Blöcke einen Wert, den niemand liest. Und
    // stünde sie in `@media print`, änderte der Umschalter nur den Ausdruck.
    const traeger = REGELN.filter(
      (regel) =>
        /(^|,)\s*html\s*$/.test(regel.selektor) &&
        /font-size:\s*var\(--dichte-wurzel\)/.test(regel.eigene),
    );
    expect(
      traeger.length,
      "Keine Regel setzt `font-size: var(--dichte-wurzel)` am `html`-Element.",
    ).toBeGreaterThan(0);
    for (const regel of traeger) {
      expect(
        bedingt(regel),
        `\`html { font-size: … }\` steht unter \`${bedingt(regel)}\``,
      ).toBeNull();
    }
  });

  it("macht `m` zu genau 100 % — dem heutigen Zustand", () => {
    // Die härteste Bedingung dieser Runde: Alle bisherigen Messungen des
    // Projekts sind gegen diesen Zustand gemessen. Verschöbe er sich, wären
    // sie Messungen eines Zustands, den niemand mehr sieht.
    expect(STANDARDDICHTE).toBe("m");
    expect(wert(stufenregel("m"), "--dichte-wurzel")).toBe("100%");
    expect(wurzelPx("m", BROWSERVORGABE)).toBe(BROWSERVORGABE);
  });

  it("ordnet die Stufen von der dichtesten zur luftigsten", () => {
    // Die Oberfläche zeigt sie in dieser Reihenfolge. Eine Skala, die eine
    // Richtung hat und sie nicht einhält, liest sich als Zufall.
    const prozente = DICHTESTUFEN.map((stufe) => wurzelPx(stufe, BROWSERVORGABE));
    expect(prozente).toEqual([...prozente].sort((a, b) => a - b));
    expect(new Set(prozente).size).toBe(prozente.length);
  });
});

describe("Die Mindestfläche am Finger", () => {
  const setzende = setzer("--dichte-beruehrung");

  it("wird an genau einer Stelle festgelegt, und die gilt immer", () => {
    // Eine zweite Deklaration gewänne je nach Reihenfolge, und dieser Test
    // sähe die falsche. Eine in `@media print` löste am Bildschirm zu nichts
    // auf — nachgemessen: `0px`.
    expect(
      setzende.map((regel) => `${regel.pfad.join(" > ")} ${regel.selektor}`.trim()),
      "--dichte-beruehrung wird an mehr als einer Stelle gesetzt",
    ).toHaveLength(1);
    expect(setzende[0].selektor).toBe(":root");
    expect(bedingt(setzende[0]), `:root steht unter \`${bedingt(setzende[0])}\``).toBeNull();
  });

  const beruehrung = wert(setzende[0], "--dichte-beruehrung");

  it.each(DICHTESTUFEN)("wird in Stufe %s nicht unterschritten", (stufe) => {
    // Gegen drei Browservorgaben, nicht nur gegen 16: Der Boden muss auch
    // dann tragen, wenn jemand seine Grundschrift kleiner gestellt hat.
    for (const vorgabe of [12, BROWSERVORGABE, 20]) {
      const hoehe = inPixeln(beruehrung, wurzelPx(stufe, vorgabe));
      expect(
        hoehe,
        `Stufe ${stufe} bei ${vorgabe} px Browservorgabe: ${hoehe} px`,
      ).toBeGreaterThanOrEqual(MINDESTFLAECHE);
    }
  });

  it("bräuchte den Boden — ohne ihn fiele die kleinste Stufe darunter", () => {
    // Die Gegenprobe. Ohne sie stünde das `max()` da, ohne dass jemand wüsste,
    // ob es überhaupt etwas tut — und beim nächsten Aufräumen fiele es weg.
    const remGlied = /([0-9.]+rem)/.exec(beruehrung)?.[1];
    expect(remGlied, `In \`${beruehrung}\` steht kein rem-Glied`).toBeDefined();

    const kleinste = DICHTESTUFEN[0];
    expect(inPixeln(remGlied as string, wurzelPx(kleinste, BROWSERVORGABE))).toBeLessThan(
      MINDESTFLAECHE,
    );
  });

  it("skaliert in der größten Stufe weiterhin mit", () => {
    // Ein festes `44px` wäre die schlechtere Lösung: Es nähme dem Nutzer mit
    // vergrößerter Grundschrift den Zuwachs. Der Boden greift nur nach unten.
    const groesste = DICHTESTUFEN[DICHTESTUFEN.length - 1];
    expect(inPixeln(beruehrung, wurzelPx(groesste, BROWSERVORGABE))).toBeGreaterThan(
      MINDESTFLAECHE,
    );
  });

  it("ist in Stufe `m` genau so hoch wie vor dem Umbau", () => {
    // 2.75 rem bei 16 px Wurzel waren 44 px, und 44 px sollen es bleiben.
    expect(inPixeln(beruehrung, wurzelPx("m", BROWSERVORGABE))).toBe(MINDESTFLAECHE);
  });

  it("ist mit den Komponenten verdrahtet — sonst gilt sie für nichts", () => {
    // Der Mutant, der am weitesten trug: `--spacing-beruehrung` auf ein anderes
    // Token zeigen lassen. Über vierzig Komponenten benutzen
    // `min-h-beruehrung`, und alle hingen dann an etwas anderem — ohne dass
    // eine einzige Zahl in dieser Datei falsch geworden wäre.
    const thema = REGELN.find((regel) => regel.selektor === "@theme inline");
    expect(thema, "Kein `@theme inline`-Block in globals.css").toBeDefined();
    expect((thema as Regel).eigene).toMatch(
      /--spacing-beruehrung:\s*var\(--dichte-beruehrung\)\s*;/,
    );
    expect((thema as Regel).eigene).toMatch(
      /--spacing-bedienelement:\s*var\(--dichte-bedienelement\)\s*;/,
    );
  });

  it("trägt am Berührungsgerät auch das Bedienelement", () => {
    // Am Zeigergerät ist `--dichte-bedienelement` 2 rem. Dass es am Finger auf
    // die Mindestfläche zurückfällt, ist die halbe Zusicherung aus
    // `visuelles-konzept.md` §5 — und sie steht in genau einer Regel.
    //
    // **Was das nicht zusichert:** eine Fläche von 44 × 44. Es ist eine Aussage
    // über die Höhe; ein Symbolknopf (`size-8`) bleibt in jeder Stufe schmaler
    // (`docs/dichte-umschalter.md` §5.4, offener Punkt 96).
    const grob = REGELN.filter(
      (regel) =>
        [...regel.pfad, regel.selektor].some((teil) => /@media[^{]*pointer:\s*coarse/.test(teil)) &&
        /--dichte-bedienelement\s*:/.test(regel.eigene),
    );
    expect(grob, "Kein `@media (pointer: coarse)` setzt --dichte-bedienelement").toHaveLength(1);
    expect(wert(grob[0], "--dichte-bedienelement")).toBe("var(--dichte-beruehrung)");
  });
});

describe("Der Rückfall bei fehlender oder unbekannter Wahl", () => {
  it("nimmt jede der vier Stufen unverändert an", () => {
    for (const stufe of DICHTESTUFEN) {
      expect(dichteAus(stufe)).toBe(stufe);
    }
  });

  it("fällt auf `m` — und ordnet nicht der nächstliegenden Stufe zu", () => {
    // Regel Q4 in klein: Nicht zugeordnet heißt nicht zugeordnet. Aus `xxs`
    // wird nicht `xs`, aus `XS` auch nicht — ein geratener Wert wäre eine
    // Behauptung über etwas, worüber nichts bekannt ist.
    for (const wahl of [undefined, null, "", "xxs", "XS", "M", "gross", "100%", "medium", "0"]) {
      expect(dichteAus(wahl), `Wert: ${String(wahl)}`).toBe(STANDARDDICHTE);
    }
  });
});

describe("Die Sprachdateien", () => {
  it.each(DICHTESTUFEN)("tragen für Stufe %s in beiden Sprachen einen Text", (stufe) => {
    // `tests/sprachdateien.test.ts` vergleicht beide Schlüsselsätze und fängt
    // jede Abweichung — aber nur, wenn die Schlüssel überhaupt angelegt sind.
    // Eine fehlende Stufe fehlte dort in *beiden* Dateien und fiele nicht auf.
    for (const [name, sprachdatei] of [
      ["de", de],
      ["en", en],
    ] as const) {
      const text = sprachdatei.dichte.stufen[stufe];
      expect(typeof text, `${name}.dichte.stufen.${stufe}`).toBe("string");
      expect(text.trim(), `${name}.dichte.stufen.${stufe}`).not.toBe("");
    }
  });

  it("beschriften jede Stufe unterschiedlich", () => {
    // Zwei gleich beschriftete Einträge in einem Menü mit vier Auswahlen sind
    // schlimmer als drei Einträge.
    for (const sprachdatei of [de, en]) {
      const texte = DICHTESTUFEN.map((stufe) => sprachdatei.dichte.stufen[stufe]);
      expect(new Set(texte).size).toBe(texte.length);
    }
  });
});
