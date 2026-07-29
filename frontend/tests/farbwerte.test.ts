import { readFileSync, readdirSync } from "node:fs";
import { join, relative } from "node:path";
import { fileURLToPath } from "node:url";

import { describe, expect, it } from "vitest";

/**
 * Blockierungstest: **kein Farbwert in einer Komponente.**
 *
 * Das visuelle Konzept ist ein Vorschlag und muss austauschbar bleiben. Es lässt
 * sich nur dann in einer Datei ändern, wenn keine Komponente eine Farbe selbst
 * kennt — weder als Hex-Wert noch als Tailwind-Farbklasse. Sobald irgendwo
 * `text-blue-600` steht, ist die Änderung nicht mehr billig, sondern eine Suche
 * über das ganze Projekt.
 *
 * Ausgenommen ist `src/components/ui` — dort liegt der Generatorbereich von
 * shadcn/ui. Was der Generator schreibt, wird nicht von Hand umgebaut; es dort
 * zu prüfen hieße, den Test bei jedem `shadcn add` zu reparieren. Der Umweg über
 * eigene Bausteine bleibt trotzdem verbindlich.
 */

const WURZEL = fileURLToPath(new URL("../src", import.meta.url));
const AUSGENOMMEN = ["components/ui"];

const TAILWIND_PALETTE = [
  "slate",
  "gray",
  "zinc",
  "neutral",
  "stone",
  "red",
  "orange",
  "amber",
  "yellow",
  "lime",
  "green",
  "emerald",
  "teal",
  "cyan",
  "sky",
  "blue",
  "indigo",
  "violet",
  "purple",
  "fuchsia",
  "pink",
  "rose",
  "white",
  "black",
].join("|");

const VERBOTEN: readonly { name: string; muster: RegExp }[] = [
  { name: "Hex-Farbwert", muster: /#[0-9a-fA-F]{3,8}\b/ },
  { name: "CSS-Farbfunktion", muster: /\b(oklch|oklab|rgba?|hsla?|color-mix)\s*\(/ },
  {
    name: "Tailwind-Farbklasse",
    muster: new RegExp(
      String.raw`\b(bg|text|border|ring|outline|shadow|fill|stroke|from|via|to|decoration|divide|placeholder|caret|accent)-(${TAILWIND_PALETTE})\b`,
    ),
  },
];

function dateien(verzeichnis: string): string[] {
  return readdirSync(verzeichnis, { withFileTypes: true }).flatMap((eintrag) => {
    const pfad = join(verzeichnis, eintrag.name);
    if (eintrag.isDirectory()) {
      return dateien(pfad);
    }
    return /\.tsx?$/.test(eintrag.name) ? [pfad] : [];
  });
}

const GEPRUEFT = dateien(WURZEL)
  .map((pfad) => relative(WURZEL, pfad).split("\\").join("/"))
  .filter((pfad) => !AUSGENOMMEN.some((teil) => pfad.startsWith(teil)));

describe("Farbwerte", () => {
  it("prüft überhaupt Dateien", () => {
    // Ohne diese Zusicherung bestünde der Test auch dann, wenn das Muster nie
    // eine Datei fände — und niemand merkte es.
    expect(GEPRUEFT.length).toBeGreaterThan(10);
  });

  it.each(GEPRUEFT)("%s enthält keinen festen Farbwert", (pfad) => {
    const inhalt = readFileSync(join(WURZEL, pfad), "utf8");
    for (const { name, muster } of VERBOTEN) {
      const treffer = muster.exec(inhalt);
      expect(treffer, `${name} in ${pfad}: ${treffer?.[0]}`).toBeNull();
    }
  });
});
