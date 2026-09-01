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
 * Ausgenommen ist `src/components/ui/` — dort liegt der Generatorbereich von
 * shadcn/ui. Was der Generator schreibt, wird nicht von Hand umgebaut; es dort
 * zu prüfen hieße, den Test bei jedem `shadcn add` zu reparieren. Der Umweg über
 * eigene Bausteine bleibt trotzdem verbindlich.
 *
 * ## `var(--token)` ist erlaubt, und zwar von Anfang an *(vermerkt 31.08.2026)*
 *
 * Diagramme färben nicht über eine Klasse, sondern über ein Prop:
 * `<Bar fill="…" />`. Die zulässige Form dafür ist `fill="var(--status-fehler)"`
 * — und die **stand nie auf der Liste unten**: Die Muster treffen Hex-Werte, die
 * Farbfunktionen und die Tailwind-Palette. `var(` ist keines davon.
 *
 * Der Vermerk steht hier, weil er beim Lesen sonst offen bleibt und weil er
 * gemessen ist: Recharts 3.10.1 schreibt die Zeichenkette unverändert ins
 * SVG-Attribut, und der Browser löst sie auf — an vier Stellen nachgesehen,
 * einschließlich der Pixel im Bild (`docs/frontend-grundlagen.md` §8a).
 * **Am Test war dafür nichts zu ändern.** Was er weiterhin blockiert, ist
 * genau die Form aus der Recharts-Dokumentation: `fill="#b3261e"`.
 *
 * ## Was eine Mutationsprüfung am 01.09.2026 gefunden hat
 *
 * **Von zwölf Mutanten blieben neun grün** — und keinen davon fing ein anderer
 * Test der Suite auf. Der Grund war nie ein falsches Muster, sondern immer
 * derselbe: **Der Test sah an der Stelle nicht hin, an der die Farbe stand.**
 *
 * | Mutant | alte Fassung | Grund |
 * |---|---|---|
 * | Hex in einer `.css`-Datei unter `src/` | grün | nur `.tsx?` wurde gelesen |
 * | Hex in einer `.js`-Datei unter `src/` | grün | dasselbe |
 * | Farbe in `src/components/uikarte.tsx` | grün | `startsWith("components/ui")` **ohne Schrägstrich** |
 * | `style={{ color: "red" }}` | grün | benannte CSS-Farben standen auf keiner Liste |
 * | `RGB(179, 38, 30)` | grün | das Muster trug kein `i`-Flag |
 * | `ring-offset-red-500` | grün | `ring-offset` fehlte in der Präfixliste |
 * | `"#" + "b3261e"` | grün | siehe unten, offener Punkt |
 * | Statusfarbe in `globals.css` umgedreht | grün | ausdrücklich nicht seine Aufgabe |
 * | handgeschriebene Datei in `components/ui/` | grün | die Ausnahme ist pfad- und nicht generatorbasiert |
 *
 * **Sechs davon sind hier abgestellt** — der Suchpfad umfasst jetzt auch `.css`,
 * `.js` und ihre Geschwister, die Ausnahme trägt ihren Schrägstrich, und die
 * Muster kennen benannte Farben, Großschreibung und beliebige Klassenpräfixe.
 * Die drei übrigen stehen als offene Punkte in `docs/testfestigkeit.md` §10.
 *
 * **Was der Test dadurch nicht wird: eine Auswertung.** Er liest Text. Eine
 * Farbe, die erst zur Laufzeit entsteht (`"#" + "b3261e"`), findet er nicht und
 * kann er nicht finden.
 */

const WURZEL = fileURLToPath(new URL("../src", import.meta.url));

/** Der Generatorbereich von shadcn/ui — **mit** Schrägstrich, sonst trifft die
 *  Ausnahme auch `components/uikarte.tsx`. Genau daran ist die erste Fassung
 *  gescheitert. */
const AUSGENOMMEN = ["components/ui/"];

/**
 * Die eine Datei, in der Farbwerte **stehen sollen** — das visuelle Konzept
 * selbst. Sie wird namentlich ausgenommen und nicht über ihre Endung: Eine
 * zweite `.css` unter `src/` wäre eine zweite Stelle mit Farbe, und genau das
 * soll auffallen.
 */
const KONZEPTDATEI = "app/globals.css";

/** Alles, worin eine Farbe stehen kann. Die erste Fassung las nur `.tsx?`. */
const ENDUNGEN = /\.(tsx?|jsx?|mjs|cjs|css)$/;

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

/**
 * Die benannten CSS-Farben. Getroffen wird nur ein **vollständiges**
 * Zeichenkettenliteral (`"red"`), nie das Wort im Fließtext — sonst führe jeder
 * Kommentar, in dem „gold" steht, den Test rot.
 */
const CSS_FARBNAMEN = [
  "aliceblue|antiquewhite|aqua|aquamarine|azure|beige|bisque|black|blanchedalmond|blue",
  "blueviolet|brown|burlywood|cadetblue|chartreuse|chocolate|coral|cornflowerblue|cornsilk",
  "crimson|cyan|darkblue|darkcyan|darkgoldenrod|darkgray|darkgreen|darkgrey|darkkhaki",
  "darkmagenta|darkolivegreen|darkorange|darkorchid|darkred|darksalmon|darkseagreen",
  "darkslateblue|darkslategray|darkslategrey|darkturquoise|darkviolet|deeppink|deepskyblue",
  "dimgray|dimgrey|dodgerblue|firebrick|floralwhite|forestgreen|fuchsia|gainsboro|ghostwhite",
  "gold|goldenrod|gray|green|greenyellow|grey|honeydew|hotpink|indianred|indigo|ivory|khaki",
  "lavender|lavenderblush|lawngreen|lemonchiffon|lightblue|lightcoral|lightcyan",
  "lightgoldenrodyellow|lightgray|lightgreen|lightgrey|lightpink|lightsalmon|lightseagreen",
  "lightskyblue|lightslategray|lightslategrey|lightsteelblue|lightyellow|lime|limegreen",
  "linen|magenta|maroon|mediumaquamarine|mediumblue|mediumorchid|mediumpurple|mediumseagreen",
  "mediumslateblue|mediumspringgreen|mediumturquoise|mediumvioletred|midnightblue|mintcream",
  "mistyrose|moccasin|navajowhite|navy|oldlace|olive|olivedrab|orange|orangered|orchid",
  "palegoldenrod|palegreen|paleturquoise|palevioletred|papayawhip|peachpuff|peru|pink|plum",
  "powderblue|purple|rebeccapurple|red|rosybrown|royalblue|saddlebrown|salmon|sandybrown",
  "seagreen|seashell|sienna|silver|skyblue|slateblue|slategray|slategrey|snow|springgreen",
  "steelblue|tan|teal|thistle|tomato|turquoise|violet|wheat|white|whitesmoke|yellow",
  "yellowgreen",
].join("|");

const VERBOTEN: readonly { name: string; muster: RegExp }[] = [
  { name: "Hex-Farbwert", muster: /#[0-9a-fA-F]{3,8}\b/ },
  {
    // `i`, weil CSS die Schreibweise nicht unterscheidet und `RGB(…)` genauso
    // malt wie `rgb(…)`. Dazu die Funktionen, die seit der ersten Fassung
    // dazugekommen sind.
    name: "CSS-Farbfunktion",
    muster: /\b(oklch|oklab|lch|lab|hwb|rgba?|hsla?|color-mix|light-dark)\s*\(/i,
  },
  {
    // **Kein Präfixverzeichnis mehr.** Die erste Fassung zählte auf, welche
    // Eigenschaften eine Farbe tragen dürfen, und `ring-offset` fehlte darin.
    // Getroffen wird jetzt jede Klasse, die auf einen Palettennamen endet —
    // die Liste, die gepflegt werden muss, ist damit die Palette und nicht die
    // Menge der Eigenschaften.
    name: "Tailwind-Farbklasse",
    muster: new RegExp(String.raw`\b[a-z][a-z0-9]*(?:-[a-z0-9]+)*-(${TAILWIND_PALETTE})\b`),
  },
  {
    name: "benannte CSS-Farbe",
    muster: new RegExp(String.raw`(["'])(${CSS_FARBNAMEN})\1`, "i"),
  },
];

function dateien(verzeichnis: string): string[] {
  return readdirSync(verzeichnis, { withFileTypes: true }).flatMap((eintrag) => {
    const pfad = join(verzeichnis, eintrag.name);
    if (eintrag.isDirectory()) {
      return dateien(pfad);
    }
    return ENDUNGEN.test(eintrag.name) ? [pfad] : [];
  });
}

const ALLE = dateien(WURZEL).map((pfad) => relative(WURZEL, pfad).split("\\").join("/"));

const GEPRUEFT = ALLE.filter((pfad) => !AUSGENOMMEN.some((teil) => pfad.startsWith(teil))).filter(
  (pfad) => pfad !== KONZEPTDATEI,
);

describe("Farbwerte", () => {
  it("prüft überhaupt Dateien", () => {
    // Ohne diese Zusicherung bestünde der Test auch dann, wenn das Muster nie
    // eine Datei fände — und niemand merkte es.
    expect(GEPRUEFT.length).toBeGreaterThan(10);
  });

  it("liest das visuelle Konzept als einzige Ausnahme — und keine zweite", () => {
    // Der Suchpfad umfasst seit dem 01.09.2026 auch `.css`. Damit gibt es genau
    // eine Datei unter `src/`, in der Farbwerte stehen dürfen. Käme eine zweite
    // dazu, wäre das visuelle Konzept auf zwei Dateien verteilt — und der
    // Ausschluss oben nähme sie stillschweigend mit heraus, wenn er über die
    // Endung ginge statt über den Namen.
    expect(ALLE.filter((pfad) => pfad.endsWith(".css"))).toEqual([KONZEPTDATEI]);
  });

  it("nimmt genau den Generatorbereich aus, nicht seine Namensvettern", () => {
    // Der Mutant, an dem die erste Fassung gescheitert ist:
    // `startsWith("components/ui")` ohne Schrägstrich nimmt auch
    // `components/uikarte.tsx` heraus — eine handgeschriebene Datei, für die
    // die Regel sehr wohl gilt.
    for (const teil of AUSGENOMMEN) {
      expect(teil.endsWith("/"), `\`${teil}\` muss auf einen Schrägstrich enden`).toBe(true);
    }
    expect(AUSGENOMMEN.some((teil) => "components/uikarte.tsx".startsWith(teil))).toBe(false);
    expect(AUSGENOMMEN.some((teil) => "components/ui/button.tsx".startsWith(teil))).toBe(true);
  });

  it("erkennt die Formen, die eine Farbe annehmen kann", () => {
    // Die Gegenprobe zu den Mustern selbst. Ohne sie stünde die Liste da, ohne
    // dass jemand wüsste, ob sie noch trifft — und die vier Zeilen unten sind
    // genau die Mutanten, die die erste Fassung überlebt haben.
    const trifft = (text: string) => VERBOTEN.some(({ muster }) => muster.test(text));

    expect(trifft(`style={{ borderColor: "#b3261e" }}`)).toBe(true);
    expect(trifft(`className="text-blue-600"`)).toBe(true);
    expect(trifft(`className="ring-offset-red-500"`)).toBe(true);
    expect(trifft(`style={{ color: "RGB(179, 38, 30)" }}`)).toBe(true);
    expect(trifft(`style={{ color: "red" }}`)).toBe(true);
    expect(trifft(`background: oklch(0.6 0.2 25)`)).toBe(true);

    // Und was ausdrücklich erlaubt bleibt:
    expect(trifft(`fill="var(--status-fehler)"`)).toBe(false);
    expect(trifft(`className="text-muted-foreground text-beiwerk"`)).toBe(false);
    expect(trifft(`className="min-h-beruehrung bg-card border-border"`)).toBe(false);
  });

  it.each(GEPRUEFT)("%s enthält keinen festen Farbwert", (pfad) => {
    const inhalt = readFileSync(join(WURZEL, pfad), "utf8");
    for (const { name, muster } of VERBOTEN) {
      const treffer = muster.exec(inhalt);
      expect(treffer, `${name} in ${pfad}: ${treffer?.[0]}`).toBeNull();
    }
  });
});
