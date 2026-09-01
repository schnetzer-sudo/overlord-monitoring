import { existsSync, readFileSync, readdirSync } from "node:fs";
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

/**
 * Das Frontend-Wurzelverzeichnis — der **zweite** Suchpfad.
 *
 * `src/` ist nicht der einzige Ort, aus dem Next.js lädt. Eine adversarische
 * Runde am 01.09.2026 hat vier Wege daneben gefunden: `public/` (unverändert
 * ausgeliefert), ein Geschwisterordner wie `styles/` (aus einer Komponente
 * importiert), `instrumentation-client.ts` neben `package.json` (auf jeder
 * Seite ausgewertet) und `next.config.ts` (`env` wird zur Bauzeit als Literal
 * ins Bündel gesetzt). **Keiner davon lag im Suchpfad.**
 */
const RAHMEN = fileURLToPath(new URL("..", import.meta.url));

/** Verzeichnisse neben `src`, aus denen Next.js ebenfalls lädt. */
const NEBENVERZEICHNISSE = ["public", "styles", "app", "pages"];

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

/**
 * Alles, worin eine Farbe stehen kann. Die erste Fassung las nur `.tsx?`.
 *
 * `.svg` steht dabei, weil Next.js `src/app/icon.svg` **allein über den
 * Dateinamen** aufnimmt und auf jeder Seite einhängt — ohne einen einzigen
 * Importbefehl, den irgendein Test sehen könnte.
 */
const ENDUNGEN = /\.(tsx?|jsx?|mjs|cjs|css|scss|sass|less|svg|json)$/;

/** Stilblätter — davon darf es genau eines geben, und das ist das Konzept. */
const STILBLATT = /\.(css|scss|sass|less)$/;

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

/**
 * Die CSS-Systemfarben.
 *
 * **`Field` und `Mark` fehlen absichtlich.** Beide sind gewöhnliche Wörter;
 * `i18n/en.ts` trägt heute das Literal `"Field"` als Spaltenüberschrift. Ein
 * Test, der daran rot wird, wird abgeschaltet und nicht behoben. Die beiden
 * sind damit eine benannte Lücke und keine übersehene.
 */
const SYSTEMFARBEN = [
  "Canvas|CanvasText|LinkText|VisitedText|ActiveText|ButtonFace|ButtonText|ButtonBorder",
  "AccentColor|AccentColorText|Highlight|HighlightText|SelectedItem|SelectedItemText",
  "GrayText|FieldText|MarkText",
].join("|");

const ALLE_FARBNAMEN = `${CSS_FARBNAMEN}|${SYSTEMFARBEN}`;

const VERBOTEN: readonly { name: string; muster: RegExp }[] = [
  {
    // Auch **kodiert**: In einer `data:`-URL muss `#` als `%23` stehen, sonst
    // begänne dort ein Fragmentbezeichner — jedes Inline-SVG schreibt es so.
    // Und JSX löst `&#35;` in Attributwerten zum fertigen `#` auf.
    name: "Hex-Farbwert",
    muster: /(#|%23|&#0*35;)[0-9a-fA-F]{3,8}\b/,
  },
  {
    // `i`, weil CSS die Schreibweise nicht unterscheidet und `RGB(…)` genauso
    // malt wie `rgb(…)`. `color(` ist die generische Form aus CSS Color 4 und
    // fehlte in der ersten Härtung; der Rückgriff verhindert, dass ein
    // `.color(` aus einer Bibliothek daran hängenbleibt.
    name: "CSS-Farbfunktion",
    muster: /(?<![\w.-])(oklch|oklab|lch|lab|hwb|rgba?|hsla?|color|color-mix|light-dark)\s*\(/i,
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
    // Der Name **allein** zwischen den Anführungszeichen: `color: "red"`.
    name: "benannte CSS-Farbe",
    muster: new RegExp(String.raw`(["'])(${ALLE_FARBNAMEN})\1`, "i"),
  },
  {
    // Der Name in einem **zusammengesetzten** Wert: `"0 0 0 1px firebrick"`.
    // Angehängt an eine CSS-Länge, damit Fließtext nicht mitgeht — „a badge
    // with a question mark" enthält einen Farbnamen und ist keiner.
    name: "benannte CSS-Farbe in einem zusammengesetzten Wert",
    muster: new RegExp(
      String.raw`["'][^"']*\d(?:px|rem|em|%|vh|vw)\s[^"']*(?<![a-zA-Z-])(${ALLE_FARBNAMEN})(?![a-zA-Z-])`,
      "i",
    ),
  },
  {
    // Der Name in einem Tailwind-Beliebigwert: `bg-[crimson]`,
    // `shadow-[0_0_0_2px_red]`, `[background-color:red]`. Dort trennt ein
    // Unterstrich oder ein Doppelpunkt, und beide beendeten die Muster oben.
    name: "benannte CSS-Farbe in einem Tailwind-Beliebigwert",
    muster: new RegExp(
      String.raw`\[[^\]\s]*(?<![a-zA-Z-])(${ALLE_FARBNAMEN})(?![a-zA-Z-])[^\]]*\]`,
      "i",
    ),
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

/**
 * Die Dateien neben `src`, aus denen Next.js ebenfalls lädt: die
 * Konfigurations- und Einstiegsdateien im Wurzelverzeichnis (**nicht**
 * rekursiv — dort liegen `node_modules` und `.next`) und die vier
 * Verzeichnisse aus `NEBENVERZEICHNISSE`, falls es sie gibt.
 *
 * Die Pfade tragen ein `../` im Namen, damit im Fehlertext sofort steht, dass
 * der Fund außerhalb von `src` liegt.
 */
const DANEBEN = [
  ...readdirSync(RAHMEN, { withFileTypes: true })
    .filter((eintrag) => eintrag.isFile() && ENDUNGEN.test(eintrag.name))
    .map((eintrag) => eintrag.name),
  ...NEBENVERZEICHNISSE.filter((name) => existsSync(join(RAHMEN, name))).flatMap((name) =>
    dateien(join(RAHMEN, name)).map((pfad) => relative(RAHMEN, pfad).split("\\").join("/")),
  ),
].map((pfad) => `../${pfad}`);

function lies(pfad: string): string {
  return pfad.startsWith("../")
    ? readFileSync(join(RAHMEN, pfad.slice(3)), "utf8")
    : readFileSync(join(WURZEL, pfad), "utf8");
}

const GEPRUEFT = [...ALLE, ...DANEBEN]
  .filter((pfad) => !AUSGENOMMEN.some((teil) => pfad.startsWith(teil)))
  .filter((pfad) => pfad !== KONZEPTDATEI);

describe("Farbwerte", () => {
  it("prüft überhaupt Dateien", () => {
    // Ohne diese Zusicherung bestünde der Test auch dann, wenn das Muster nie
    // eine Datei fände — und niemand merkte es.
    expect(GEPRUEFT.length).toBeGreaterThan(10);
  });

  it("liest das visuelle Konzept als einzige Ausnahme — und keine zweite", () => {
    // Der Suchpfad umfasst seit dem 01.09.2026 auch Stilblätter. Damit gibt es
    // genau eine Datei, in der Farbwerte stehen dürfen. Käme eine zweite dazu,
    // wäre das visuelle Konzept auf zwei Dateien verteilt — und der Ausschluss
    // oben nähme sie stillschweigend mit heraus, wenn er über die Endung ginge
    // statt über den Namen.
    //
    // **Über beide Suchpfade**, denn ein `styles/marke.css` neben `src` wäre
    // dieselbe zweite Stelle, nur einen Ordner weiter oben.
    expect([...ALLE, ...DANEBEN].filter((pfad) => STILBLATT.test(pfad))).toEqual([KONZEPTDATEI]);
  });

  it("sieht auch neben `src` nach — dort lädt Next.js ebenfalls", () => {
    // Vier Wege aus der adversarischen Runde vom 01.09.2026 lagen außerhalb
    // von `src`. Diese Zusicherung hält fest, dass der zweite Suchpfad nicht
    // leer läuft: `next.config.ts` gibt es, und die Nebenverzeichnisse werden
    // aufgenommen, sobald jemand sie anlegt.
    expect(DANEBEN).toContain("../next.config.ts");
    expect(DANEBEN.length).toBeGreaterThan(3);

    // **Gezählt, nicht auf Existenz geprüft.** Ein leeres `styles/` trägt keine
    // Farbe; an seiner blossen Anwesenheit rot zu werden hiesse, den Test an
    // etwas zu hängen, das nichts aussagt. Gemessen wird, ob von dem, was
    // wirklich darin liegt, nichts durchfällt.
    for (const name of NEBENVERZEICHNISSE) {
      const ort = join(RAHMEN, name);
      if (!existsSync(ort)) continue;
      const drin = dateien(ort).length;
      expect(
        DANEBEN.filter((pfad) => pfad.startsWith(`../${name}/`)).length,
        `${name}/ trägt ${drin} Dateien und wird nicht vollständig gelesen`,
      ).toBe(drin);
    }
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

    // Die sechs aus der adversarischen Runde vom 01.09.2026:
    expect(trifft(`fill="&#35;b3261e"`)).toBe(true);
    expect(trifft(`url("data:image/svg+xml,%3Csvg fill='%23b3261e'")`)).toBe(true);
    expect(trifft(`fill="color(srgb 0.7 0.7 0.7)"`)).toBe(true);
    expect(trifft(`className="bg-[crimson]"`)).toBe(true);
    expect(trifft(`className="shadow-[0_0_0_2px_red]"`)).toBe(true);
    expect(trifft(`className="[background-color:red]"`)).toBe(true);
    expect(trifft(`style={{ boxShadow: "0 0 0 1px firebrick" }}`)).toBe(true);
    expect(trifft(`style={{ color: "Highlight" }}`)).toBe(true);

    // Und was ausdrücklich erlaubt bleibt:
    expect(trifft(`fill="var(--status-fehler)"`)).toBe(false);
    expect(trifft(`className="text-muted-foreground text-beiwerk"`)).toBe(false);
    expect(trifft(`className="min-h-beruehrung bg-card border-border"`)).toBe(false);
    expect(trifft(`className="max-w-[24rem] grid-cols-[auto_1fr]"`)).toBe(false);
    expect(trifft(`titel: "A badge with a question mark"`)).toBe(false);
    expect(trifft(`// Der Akzent ist ein Gelbgrün, kein Orange`)).toBe(false);
  });

  it.each(GEPRUEFT)("%s enthält keinen festen Farbwert", (pfad) => {
    const inhalt = lies(pfad);
    for (const { name, muster } of VERBOTEN) {
      const treffer = muster.exec(inhalt);
      expect(treffer, `${name} in ${pfad}: ${treffer?.[0]}`).toBeNull();
    }
  });
});
