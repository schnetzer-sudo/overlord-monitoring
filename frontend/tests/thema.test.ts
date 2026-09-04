import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";

import { describe, expect, it } from "vitest";

import { STANDARDTHEMA, THEMAWERTE, themaAus, type Themawert } from "@/thema";
import { de } from "@/i18n/de";
import { en } from "@/i18n/en";

import { stilblatt, type Regel } from "./hilfe/css-leser";

/**
 * Der Themaumschalter — **rechnerisch geprüft, ohne Ansicht.**
 *
 * Drei Werte schalten den Dunkelzustand ein und aus. Was daran schiefgehen
 * kann, geht **still** schief, und zwar schlimmer als bei der Dichte: Der
 * Dunkelzustand hat **zwei Zweige** — den ausdrücklichen
 * (`html[data-thema="dunkel"]`) und den System-Zweig unter
 * `@media (prefers-color-scheme: dark)`. Ein Token, das nur in einem der
 * beiden steht, ist im System-Fall hell und im ausdrücklichen dunkel, **und
 * niemand sieht es, weil niemand beide Wege zugleich geht.**
 *
 * Deshalb liest dieser Test die **Datei** und nicht ein Abbild davon — dieselbe
 * Bauform wie `tests/dichte.test.ts` und `tests/farbkontrast.test.ts`, mit
 * demselben CSS-Leser aus `tests/hilfe/css-leser.ts`. **Keine zweite Kopie**
 * (E‑64): Die Verschärfung, die der eine bekommt, fehlte dem anderen, und
 * niemand sähe es.
 *
 * ## Die Arbeitsteilung mit `tests/farbkontrast.test.ts`
 *
 * | | |
 * |---|---|
 * | **dort** | die **Zahlen** — jeder Vordergrund ≥ 4,5 : 1 in beiden Wertesätzen, Regel Q3, sRGB, die vierzehn Gegenproben, die Verdrahtung in `@theme inline` |
 * | **hier** | der **Weg** — dass es beide Zweige gibt, dass sie dieselbe Tokenmenge umzeigen, dass jede Umzeigung auf **ihr eigenes** Token zeigt, dass `@custom-variant dark` beide trifft, dass `color-scheme` in beiden steht |
 *
 * Ein Wert, der stimmt, und ein Zweig, der ihn nicht liest, sähen ohne diese
 * Teilung gleich aus.
 *
 * ## Zusicherungen über die **Lage**, nicht nur über den Wert
 *
 * Das ist die Lehre aus `docs/dichte-umschalter.md` §7 und aus
 * `docs/dunkelmodus.md` §7.5, und sie ist in diesem Projekt zweimal teuer
 * bezahlt worden: Eine Regel in `@media print` steht da und wirkt am Bildschirm
 * nie; ein `--color-<token>`, das auf ein fremdes Token zeigt, macht jede
 * gerechnete Zahl richtig und wertlos zugleich. **Beide Fehlerklassen haben
 * hier ihre Entsprechung**, und beide sind zugesichert.
 *
 * **Ein Wirkungstest im Browser wird er dadurch nicht.** Was die Regeln am
 * laufenden System tatsächlich bewirken — dass nichts aufblitzt, dass Recharts
 * ohne Neumontage nachzieht, dass der System-Zweig beim ersten Paint greift —
 * steht gemessen in `docs/dunkelmodus.md` §12 bis §14 und nirgendwo sonst.
 */

const CSS_ROH = readFileSync(
  fileURLToPath(new URL("../src/app/globals.css", import.meta.url)),
  "utf8",
);

const { css: CSS, regeln: REGELN, setzer, wert } = stilblatt(CSS_ROH);

/** Das Präfix, unter dem die Dunkelwerte **einmal** an `:root` stehen. */
const DUNKELPRAEFIX = "--dunkel-";

/** Die Medienabfrage, die den System-Zweig trägt — wortgleich zur Datei. */
const SYSTEMABFRAGE = "@media (prefers-color-scheme: dark)";

// ───────────────────────────────────────────────────────────────────────────

/** Die Regel `html[data-thema="…"]` eines Werts. Es muss genau eine geben. */
function themaregel(wahl: string): Regel {
  const gefunden = REGELN.filter((regel) =>
    new RegExp(`^html\\[data-thema=["']?${wahl}["']?\\]$`).test(regel.selektor),
  );
  expect(
    gefunden.length,
    `\`html[data-thema="${wahl}"]\` steht ${gefunden.length}-mal in globals.css. ` +
      `Erwartet ist genau einmal — bei zweien läse dieser Test die falsche Hälfte.`,
  ).toBe(1);
  return gefunden[0];
}

/** Alle Namen von Eigenschaften, die eine Regel in ihren **eigenen** Deklarationen setzt. */
function gesetzt(regel: Regel): string[] {
  return [...regel.eigene.matchAll(/(?:^|[;{\s])(--[a-z0-9-]+)\s*:/g)].map((t) => t[1]).sort();
}

/** Der eine unbedingte `:root`-Block — dort stehen die Dunkelwerte. */
function wurzel(): Regel {
  const gefunden = REGELN.filter((regel) => regel.selektor === ":root" && regel.pfad.length === 0);
  expect(gefunden, "Kein einzelnes unbedingtes `:root` in globals.css").toHaveLength(1);
  return gefunden[0];
}

const AUSDRUECKLICH = themaregel("dunkel");
const SYSTEM = themaregel("system");

// ───────────────────────────────────────────────────────────────────────────

describe("Der CSS-Leser selbst", () => {
  it("findet überhaupt Regeln, und die bekannten darunter", () => {
    // Ohne diese Zusicherung bestünde jeder Test unten auch dann, wenn der
    // Leser nichts fände — dieselbe Vorsorge wie in `tests/dichte.test.ts`.
    expect(REGELN.length).toBeGreaterThan(8);
    expect(REGELN.map((r) => r.selektor)).toContain(":root");
    expect(REGELN.map((r) => r.selektor)).toContain("@custom-variant dark");
  });
});

describe("Die drei Werte in globals.css", () => {
  it("sind genau die drei, die der Code kennt — in beide Richtungen", () => {
    // Ein Wert im Code ohne Regel im CSS wäre ein Menüeintrag, der nichts tut.
    // Eine Regel ohne Wert im Code wäre ein Zustand, den niemand erreichen
    // kann. Beides fällt nur auf, wenn beide Seiten verglichen werden.
    const imCss = new Set(
      [...CSS.matchAll(/\[data-thema=["']?([a-z]+)["']?\]/g)].map((treffer) => treffer[1]),
    );
    expect([...imCss].sort()).toEqual([...THEMAWERTE].sort());
  });

  it.each(THEMAWERTE)("trägt für %s genau eine Regel am Wurzelelement", (wahl) => {
    // `themaregel` selbst sichert die Einmaligkeit zu; hier steht, dass es sie
    // für jeden der drei Werte gibt.
    expect(themaregel(wahl).selektor).toContain(wahl);
  });

  it("hängt keinen Themablock unter `@media print`", () => {
    // Dieselbe Falle, die `tests/dichte.test.ts` für die Stufenregeln stellt:
    // Eine Regel in `@media print` steht da und wirkt am Bildschirm nie. Sie
    // ist für eine Textsuche von der richtigen nicht zu unterscheiden.
    const themaregeln = [
      wurzel(),
      ...REGELN.filter((regel) =>
        [...regel.pfad, regel.selektor].some((teil) => teil.includes("[data-thema")),
      ),
    ];
    expect(themaregeln.length).toBeGreaterThan(4);
    for (const regel of themaregeln) {
      const druck = [...regel.pfad, regel.selektor].find((teil) => /\bprint\b/.test(teil));
      expect(
        druck,
        `\`${regel.selektor}\` steht unter \`${druck}\` und wirkt damit nur im Ausdruck.`,
      ).toBeUndefined();
    }
  });
});

describe("Die beiden Dunkelzweige", () => {
  it("hängt den ausdrücklichen Zweig unbedingt an das Wurzelelement", () => {
    // Keine Medienabfrage, kein `@supports`, kein Wrapper: Wer `dunkel` gewählt
    // hat, bekommt es, und zwar unabhängig davon, was sein Gerät meldet.
    expect(
      AUSDRUECKLICH.pfad,
      `\`html[data-thema="dunkel"]\` steht unter \`${AUSDRUECKLICH.pfad.join(" > ")}\`.`,
    ).toEqual([]);
  });

  it("trägt den System-Zweig genau in der Medienabfrage, die ihn begründet", () => {
    // **`prefers-color-scheme` und keine Auswertung im Browser** (E‑60). Das
    // ist der Grund, aus dem *System* ohne Aufblitzen auskommt: Die Bedingung
    // steht im Stilblatt und nicht in einem Skript, das nach der Hydratation
    // läuft. Stünde hier etwas anderes, wäre entweder die Begründung falsch
    // oder der Zweig unerreichbar.
    expect(
      SYSTEM.pfad,
      `Der System-Zweig steht unter \`${SYSTEM.pfad.join(" > ")}\` statt unter \`${SYSTEMABFRAGE}\`.`,
    ).toEqual([SYSTEMABFRAGE]);
  });

  it("zeigt in beiden Zweigen dieselbe Tokenmenge um — in beide Richtungen", () => {
    // **Die tragende Zusicherung dieser Datei.** Ein Token, das nur in einem
    // Zweig steht, ist im System-Fall hell und im ausdrücklichen dunkel — und
    // niemand sieht es, weil niemand beide Wege zugleich geht.
    //
    // Verglichen werden **alle** gesetzten Eigenschaften und nicht nur die
    // umgezeigten: So fällt auch auf, wenn in einem Zweig ein Token dazukommt
    // oder ein roher Wert an die Stelle einer Umzeigung tritt.
    const ausdruecklich = gesetzt(AUSDRUECKLICH);
    const system = gesetzt(SYSTEM);
    expect(
      system,
      `Die beiden Dunkelzweige setzen verschiedene Tokenmengen. Nur ausdrücklich: ` +
        `${ausdruecklich.filter((t) => !system.includes(t)).join(", ") || "—"}. ` +
        `Nur im System-Zweig: ${system.filter((t) => !ausdruecklich.includes(t)).join(", ") || "—"}.`,
    ).toEqual(ausdruecklich);
    expect(ausdruecklich.length).toBeGreaterThan(40);
  });

  it.each([
    ["ausdrücklich", () => AUSDRUECKLICH],
    ["System", () => SYSTEM],
  ])("zeigt im %s-Zweig jedes Token auf sein eigenes --dunkel-Token", (_name, hole) => {
    // **Der Mutant aus §7.5, an dieser Stelle vorweggenommen.** Dort zeigte
    // `--color-status-fehler` auf `var(--status-abgeschlossen)`, und 812 von
    // 812 Fällen blieben grün: Der Test hätte den Wert nachgerechnet, den
    // niemand mehr sieht. Hier wäre dieselbe Verwechslung
    // `--card: var(--dunkel-background)` — eine gültige Deklaration, ein
    // sauberer Kontrast, und die falsche Farbe auf dem Bildschirm.
    //
    // Deshalb bindet die Prüfung **beide Seiten aneinander** und begnügt sich
    // nicht mit „irgendein `var()`".
    const regel = hole();
    for (const token of gesetzt(regel)) {
      expect(
        wert(regel, token),
        `\`${token}\` zeigt im Zweig nicht auf \`var(${DUNKELPRAEFIX}${token.slice(2)})\`.`,
      ).toBe(`var(${DUNKELPRAEFIX}${token.slice(2)})`);
    }
  });

  it("setzt `color-scheme` in beiden Zweigen auf `dark`", () => {
    // Ohne sie bleiben Bildlaufleisten, Auswahlfelder, Datumswähler und jedes
    // andere native Bedienelement hell — auf einer Anwendung, die sonst
    // vollständig dunkel ist. Das fällt erst im Betrieb auf, und dann an einer
    // Stelle, die niemand in `globals.css` sucht.
    for (const [name, regel] of [
      ["ausdrücklich", AUSDRUECKLICH],
      ["System", SYSTEM],
    ] as const) {
      const treffer = /(?:^|[;{\s])color-scheme\s*:\s*([^;]+);/.exec(regel.eigene);
      expect(treffer?.[1]?.trim(), `Im ${name}-Zweig fehlt \`color-scheme\`.`).toBe("dark");
    }
  });

  it("setzt `color-scheme` auch für die ausdrückliche Wahl `hell`", () => {
    // Die Gegenrichtung, und sie ist kein Beiwerk: Wer **hell** gewählt hat,
    // soll es auch dann bekommen, wenn sein Betriebssystem dunkel steht. Ohne
    // die Zeile bliebe der Wert auf `normal`.
    const hell = themaregel("hell");
    const treffer = /(?:^|[;{\s])color-scheme\s*:\s*([^;]+);/.exec(hell.eigene);
    expect(treffer?.[1]?.trim(), "Für `hell` fehlt `color-scheme`.").toBe("light");
  });
});

describe("Die Dunkelwerte stehen einmal", () => {
  it("stehen unbedingt unter `:root` und nirgends sonst", () => {
    // **Der Grund, aus dem es diese Zwischenschicht gibt** (E‑60): Zwei Blöcke
    // mit gleichem Inhalt wären eine zweite Pflegestelle, und eine Änderung an
    // nur einem wäre im anderen Zweig unsichtbar. Ein Selektor kann beide
    // Zweige nicht zugleich treffen — eine Medienabfrage ist keine
    // Selektorbedingung —, also stehen die Werte einmal und die Zweige zeigen
    // nur um.
    const dunkelwerte = gesetzt(wurzel()).filter((t) => t.startsWith(DUNKELPRAEFIX));
    expect(dunkelwerte.length).toBeGreaterThan(40);

    for (const token of dunkelwerte) {
      const setzende = setzer(token);
      expect(
        setzende.map((r) => `${r.pfad.join(" > ")} ${r.selektor}`.trim()),
        `\`${token}\` wird an mehr als einer Stelle gesetzt.`,
      ).toHaveLength(1);
      expect(setzende[0].selektor).toBe(":root");
      expect(setzende[0].pfad).toEqual([]);
    }
  });

  it("haben für jedes umgezeigte Token einen Wert — und keinen ohne Abnehmer", () => {
    // In beide Richtungen. Ein `--dunkel-*` ohne Abnehmer wäre ein Wert, den
    // niemand sieht; ein umgezeigtes Token ohne `--dunkel-*` löste zu nichts
    // auf und fiele auf den ererbten Wert zurück — also auf den **hellen**.
    const vorhanden = new Set(gesetzt(wurzel()).filter((t) => t.startsWith(DUNKELPRAEFIX)));
    const gebraucht = new Set(gesetzt(AUSDRUECKLICH).map((t) => `${DUNKELPRAEFIX}${t.slice(2)}`));
    expect([...gebraucht].sort()).toEqual([...vorhanden].sort());
  });
});

describe("Die dark-Variante von Tailwind", () => {
  const variante = () => {
    const gefunden = REGELN.filter((r) => r.selektor === "@custom-variant dark");
    expect(gefunden, "Kein `@custom-variant dark` in globals.css").toHaveLength(1);
    return gefunden[0];
  };

  it("steht genau einmal in der Datei", () => {
    // Nicht nur „genau eine Regel": Die **Kurzform** `@custom-variant dark (…);`
    // endet mit einem Semikolon und wird vom Leser gar nicht erst zu einer
    // Regel. Stünde sie zusätzlich da, gewänne je nach Reihenfolge die eine
    // oder die andere, und dieser Test läse die falsche.
    expect([...CSS.matchAll(/@custom-variant\s+dark\b/g)]).toHaveLength(1);
    expect(variante().pfad).toEqual([]);
  });

  it("trifft beide Zweige — sonst bleiben vierzig Bausteine hell", () => {
    // **Der Fehler, den diese Zusicherung fängt, ist unsichtbar.** Über vierzig
    // Bausteine in `components/ui` tragen `dark:`-Klassen; sie sind
    // Generatorbereich und werden nicht von Hand umgebaut. Ihre einzige
    // Verbindung zum Dunkelzustand ist diese eine Regel. Träfe sie nur den
    // ausdrücklichen Zweig, wäre im System-Fall der eigene Bestand dunkel und
    // der Generatorbereich hell — und die eigenen Tokens sähen dabei völlig
    // richtig aus.
    const innen = REGELN.filter((r) => r.pfad.includes("@custom-variant dark"));

    const ausdruecklich = innen.filter(
      (r) => /\[data-thema=["']?dunkel["']?\]/.test(r.selektor) && r.pfad.length === 1,
    );
    expect(
      ausdruecklich,
      "`@custom-variant dark` trifft den ausdrücklichen Zweig nicht unbedingt.",
    ).toHaveLength(1);

    const system = innen.filter(
      (r) => /\[data-thema=["']?system["']?\]/.test(r.selektor) && r.pfad.includes(SYSTEMABFRAGE),
    );
    expect(
      system,
      `\`@custom-variant dark\` trifft den System-Zweig nicht unter \`${SYSTEMABFRAGE}\`. ` +
        `Im System-Fall blieben damit alle \`dark:\`-Klassen aus components/ui hell, ` +
        `während die eigenen Tokens dunkel stehen.`,
    ).toHaveLength(1);

    // Ohne `@slot` erzeugt der Zweig zwar eine Regel, aber keine Deklarationen.
    for (const regel of [...ausdruecklich, ...system]) {
      expect(regel.eigene, `\`${regel.selektor}\` enthält kein \`@slot\`.`).toMatch(/@slot\s*;/);
    }
  });
});

describe("Der Rückfall bei fehlender oder unbekannter Wahl", () => {
  it("nimmt jeden der drei Werte unverändert an", () => {
    for (const wahl of THEMAWERTE) {
      expect(themaAus(wahl)).toBe(wahl);
    }
  });

  it("fällt auf `system` — und ordnet nicht sinngemäß zu", () => {
    // Regel Q4 in klein: Nicht zugeordnet heißt nicht zugeordnet. Aus `dark`
    // wird nicht `dunkel`, aus `DUNKEL` auch nicht und aus `nacht` erst recht
    // nicht.
    //
    // **Und der Rückfall ist hier zusätzlich der inhaltlich richtige:** Ein
    // unbekannter Wert heißt „keine Wahl getroffen", und keine Wahl ist genau
    // das, was `system` bedeutet. Ein Rückfall auf `hell` überginge die
    // Auskunft, die das Betriebssystem schon gegeben hat.
    expect(STANDARDTHEMA).toBe("system");
    for (const wahl of [
      undefined,
      null,
      "",
      " ",
      "DUNKEL",
      "Dunkel",
      "dark",
      "light",
      "nacht",
      "auto",
      "HELL",
      "0",
    ]) {
      expect(themaAus(wahl), `Wert: ${String(wahl)}`).toBe("system");
    }
  });
});

describe("Die Sprachdateien", () => {
  it.each(THEMAWERTE)("tragen für %s in beiden Sprachen einen Text", (wahl) => {
    // `tests/sprachdateien.test.ts` vergleicht beide Schlüsselsätze und fängt
    // jede Abweichung — aber nur, wenn die Schlüssel überhaupt angelegt sind.
    // Ein fehlender Wert fehlte dort in *beiden* Dateien und fiele nicht auf.
    for (const [name, sprachdatei] of [
      ["de", de],
      ["en", en],
    ] as const) {
      const text = sprachdatei.thema.werte[wahl as Themawert];
      expect(typeof text, `${name}.thema.werte.${wahl}`).toBe("string");
      expect(text.trim(), `${name}.thema.werte.${wahl}`).not.toBe("");
    }
  });

  it("beschriften jeden Wert unterschiedlich", () => {
    // Zwei gleich beschriftete Einträge in einem Menü mit drei Auswahlen sind
    // schlimmer als zwei Einträge.
    for (const sprachdatei of [de, en]) {
      const texte = THEMAWERTE.map((wahl) => sprachdatei.thema.werte[wahl]);
      expect(new Set(texte).size).toBe(texte.length);
    }
  });
});
