import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";

import { describe, expect, it } from "vitest";

import { DICHTESTUFEN, STANDARDDICHTE, dichteAus, type Dichtestufe } from "@/dichte";
import { de } from "@/i18n/de";
import { en } from "@/i18n/en";

import { stilblatt, type Regel } from "./hilfe/css-leser";

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
 * **Deshalb steht ein kleiner CSS-Leser dahinter.** Er entfernt zuerst die
 * Kommentare, zerlegt die Datei dann in Regeln **mit ihrer Verschachtelung** und
 * beantwortet damit die Frage, die eine Regex nicht beantworten kann: *in
 * welchem Block steht das?* Geprüft wird seither beides — der Wert und die Lage.
 *
 * **Er stand bis zum 03.09.2026 in dieser Datei** und liegt seither in
 * `tests/hilfe/css-leser.ts`: `tests/farbkontrast.test.ts` braucht denselben,
 * und zwei Kopien wären zwei Pflegestellen. Verändert hat sich dabei nichts —
 * die Aufrufstellen unten sind Zeichen für Zeichen dieselben geblieben, und
 * dass die Zusicherungen weiter durchlaufen, ist der Beleg. (`docs/dichte-umschalter.md`
 * §7 zählt siebenundzwanzig; `vitest` meldet 31 Fälle, weil `it.each` sie aufspannt —
 * dieselbe Menge, zweimal gezählt.)
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
// Der CSS-Leser steht in `tests/hilfe/css-leser.ts` — siehe den Kopf oben.
// ───────────────────────────────────────────────────────────────────────────

const { css: CSS, regeln: REGELN, setzer, wert, bedingt } = stilblatt(CSS_ROH);

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

/**
 * Die **Bedienzeile** — die Zeile, die zugleich ein Ziel ist *(E‑54, 02.09.2026)*.
 *
 * Baumzeile der Prozessansicht und Auswahlzeile der Prozessauswahl. Sie hielten
 * bis heute `--dichte-beruehrung` und damit in `xs`, `s` und `m` überall
 * denselben Boden von 44 px; der Dichteumschalter bewegte im Baum drei Zeilen
 * über die ganze Skala (M121). Seither tragen sie am **Zeigergerät**
 * `--dichte-zeile` und fallen am **Berührungsgerät** auf die Mindestfläche
 * zurück.
 *
 * **Die tragende Zusicherung ist die zweite Hälfte**, und sie steht in genau
 * einer Regel: Ohne den Rückfall wäre die Fläche am Finger in jeder Stufe
 * unterschritten — 36 px in `m`, 31,5 px in `xs`. Dass die Regel greift, ist
 * am laufenden System gemessen (M127, `docs/process-view.md` §24); dass sie
 * **dasteht**, prüft dieser Test.
 */
describe("Die Bedienzeile", () => {
  const setzende = setzer("--dichte-bedienzeile");

  it("steht am Zeigergerät auf der Zeilenhöhe und nicht auf der Fläche", () => {
    // Der Punkt der Entscheidung: Was hier `var(--dichte-beruehrung)` stünde,
    // wäre der Zustand vor E‑54 unter neuem Namen — der Umschalter bewegte
    // die Zeile weiterhin nicht.
    const amZeiger = setzende.filter((regel) => bedingt(regel) === null);
    expect(amZeiger, "--dichte-bedienzeile wird in :root nicht gesetzt").toHaveLength(1);
    expect(amZeiger[0].selektor).toBe(":root");
    expect(wert(amZeiger[0], "--dichte-bedienzeile")).toBe("var(--dichte-zeile)");
  });

  it("fällt am Berührungsgerät auf die Mindestfläche zurück", () => {
    // **`pointer` und nicht `any-pointer`, und der Preis ist benannt:** Ein
    // Notebook mit Berührungsbildschirm und Trackpad meldet `fine` und bekommt
    // die kürzere Zeile. `any-pointer: coarse` erfasste dieses Gerät — und
    // ließe die Verkleinerung dann praktisch nirgends greifen
    // (`docs/process-view.md` §24, E‑54).
    const grob = REGELN.filter(
      (regel) =>
        [...regel.pfad, regel.selektor].some((teil) => /@media[^{]*pointer:\s*coarse/.test(teil)) &&
        /--dichte-bedienzeile\s*:/.test(regel.eigene),
    );
    expect(grob, "Kein `@media (pointer: coarse)` setzt --dichte-bedienzeile").toHaveLength(1);
    expect(grob[0].pfad.concat(grob[0].selektor).join(" > ")).not.toMatch(/any-pointer/);
    expect(wert(grob[0], "--dichte-bedienzeile")).toBe("var(--dichte-beruehrung)");
  });

  it("bräuchte den Rückfall — ohne ihn läge die Zeile in jeder Stufe darunter", () => {
    // Die Gegenprobe, dieselbe Bauform wie beim Boden im `max()` darüber: Ohne
    // sie stünde die `@media`-Regel da, ohne dass jemand wüsste, ob sie etwas
    // tut. Gerechnet und nicht hingeschrieben — 36 px in `m`, 31,5 px in `xs`.
    const zeile = wert(setzer("--dichte-zeile")[0], "--dichte-zeile");
    for (const stufe of DICHTESTUFEN) {
      expect(
        inPixeln(zeile, wurzelPx(stufe, BROWSERVORGABE)),
        `Stufe ${stufe}: die Zeilenhöhe allein trägt die Fläche schon`,
      ).toBeLessThan(MINDESTFLAECHE);
    }
  });

  it("ist mit den Komponenten verdrahtet — sonst gilt sie für nichts", () => {
    // Derselbe Mutant wie eine Beschreibung höher: `--spacing-bedienzeile` auf
    // ein anderes Token zeigen lassen. `min-h-bedienzeile` hinge dann an etwas
    // anderem, ohne dass eine einzige Zahl falsch geworden wäre.
    const thema = REGELN.find((regel) => regel.selektor === "@theme inline");
    expect((thema as Regel).eigene).toMatch(
      /--spacing-bedienzeile:\s*var\(--dichte-bedienzeile\)\s*;/,
    );
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
