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
 */

const CSS = readFileSync(fileURLToPath(new URL("../src/app/globals.css", import.meta.url)), "utf8");

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

/**
 * Wertet eine CSS-Länge in Pixeln aus — `rem`, `px` und `max(…)` daraus.
 *
 * Bewusst **unvollständig**: Was diese Funktion nicht kennt, lässt sie
 * fehlschlagen statt es zu überspringen. Ein stiller Rückfall auf null wäre
 * hier das Schlimmste — der Test bestünde weiter und prüfte nichts mehr.
 */
function inPixeln(ausdruck: string, wurzel: number): number {
  const wert = ausdruck.trim();

  const max = /^max\((.*)\)$/.exec(wert);
  if (max !== null) {
    return Math.max(...max[1].split(",").map((glied) => inPixeln(glied, wurzel)));
  }

  const rem = /^([0-9.]+)rem$/.exec(wert);
  if (rem !== null) {
    return Number(rem[1]) * wurzel;
  }

  const px = /^([0-9.]+)px$/.exec(wert);
  if (px !== null) {
    return Number(px[1]);
  }

  throw new Error(
    `\`${ausdruck}\` ist keine Länge, die dieser Test auswerten kann. ` +
      `Entweder ist die Deklaration in globals.css falsch, oder inPixeln() muss ` +
      `die neue Form lernen — stillschweigend übergangen wird sie nicht.`,
  );
}

/** Die Prozentwerte der vier Stufen, aus `html[data-dichte="…"]` gelesen. */
function stufenAusCss(): Map<string, number> {
  const gefunden = new Map<string, number>();
  const muster = /html\[data-dichte="([^"]+)"\]\s*\{[^}]*?--dichte-wurzel:\s*([0-9.]+)%/g;
  for (const treffer of CSS.matchAll(muster)) {
    gefunden.set(treffer[1], Number(treffer[2]));
  }
  return gefunden;
}

/** Die eine Deklaration eines Tokens — und die Zusicherung, dass es genau eine ist. */
function deklaration(token: string): string {
  const treffer = [...CSS.matchAll(new RegExp(`${token}:\\s*([^;]+);`, "g"))];
  expect(
    treffer.length,
    `${token} ist ${treffer.length}-mal deklariert. Genau eine Deklaration wird ` +
      `hier ausgewertet; eine zweite würde je nach Reihenfolge gewinnen und ` +
      `dieser Test sähe die falsche.`,
  ).toBe(1);
  return treffer[0][1];
}

const STUFEN_IM_CSS = stufenAusCss();

/** Die Wurzelschriftgröße einer Stufe, in Pixeln, bei einer gegebenen Browservorgabe. */
function wurzelPx(stufe: Dichtestufe, vorgabe: number): number {
  const prozent = STUFEN_IM_CSS.get(stufe);
  expect(prozent, `Stufe ${stufe} fehlt in globals.css`).toBeDefined();
  return (vorgabe * (prozent as number)) / 100;
}

describe("Die vier Stufen in globals.css", () => {
  it("sind genau die vier, die der Code kennt — in beide Richtungen", () => {
    // Eine Stufe im Code ohne Regel im CSS wäre ein Menüeintrag, der nichts
    // tut. Eine Regel ohne Stufe im Code wäre eine Größe, die niemand
    // erreichen kann. Beides fällt nur auf, wenn beide Seiten verglichen
    // werden.
    expect([...STUFEN_IM_CSS.keys()].sort()).toEqual([...DICHTESTUFEN].sort());
  });

  it("hängen an einem Attribut am Wurzelelement und wirken dort auch", () => {
    // Ohne diese Zeile stünden vier Regeln da, die einen Wert setzen, den
    // niemand liest. `rem` misst gegen das Wurzelelement — ein Wrapper
    // darunter trüge nichts.
    expect(CSS).toMatch(/html\s*\{[^}]*font-size:\s*var\(--dichte-wurzel\)/);
  });

  it("machen `m` zu genau 100 % — dem heutigen Zustand", () => {
    // Die härteste Bedingung dieser Runde: Alle bisherigen Messungen des
    // Projekts sind gegen diesen Zustand gemessen. Verschöbe er sich, wären
    // sie Messungen eines Zustands, den niemand mehr sieht.
    expect(STANDARDDICHTE).toBe("m");
    expect(STUFEN_IM_CSS.get("m")).toBe(100);
    expect(wurzelPx("m", BROWSERVORGABE)).toBe(BROWSERVORGABE);
  });

  it("ordnet die Stufen von der dichtesten zur luftigsten", () => {
    // Die Oberfläche zeigt sie in dieser Reihenfolge. Eine Skala, die eine
    // Richtung hat und sie nicht einhält, liest sich als Zufall.
    const prozente = DICHTESTUFEN.map((stufe) => STUFEN_IM_CSS.get(stufe) as number);
    expect(prozente).toEqual([...prozente].sort((a, b) => a - b));
    expect(new Set(prozente).size).toBe(prozente.length);
  });
});

describe("Die Mindestfläche am Finger", () => {
  const beruehrung = deklaration("--dichte-beruehrung");

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
    // Die Gegenprobe. Ohne sie stünde das `max()` da, ohne dass jemand
    // wüsste, ob es überhaupt etwas tut — und beim nächsten Aufräumen fiele
    // es weg.
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
    for (const wert of [undefined, null, "", "xxs", "XS", "M", "gross", "100%", "medium", "0"]) {
      expect(dichteAus(wert), `Wert: ${String(wert)}`).toBe(STANDARDDICHTE);
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
