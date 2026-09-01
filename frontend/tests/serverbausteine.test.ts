import { readFileSync, readdirSync } from "node:fs";
import { dirname, join, posix, relative } from "node:path";
import { fileURLToPath } from "node:url";

import { describe, expect, it } from "vitest";

/**
 * Blockierungstest: **keine Server-Komponente erreicht einen Baustein, der auf
 * dem Server gar nicht ausgewertet werden kann.**
 *
 * ## Der Anlass, am 24.08.2026
 *
 * `/administration` warf beim Aufrufen einen `TypeError`:
 * *„createContext only works in Client Components."* Ausgelöst hat ihn
 * `components/ui/button.tsx` — der **kein** `"use client"` trägt, aber `Slot`
 * aus dem Sammelpaket `radix-ui` importiert. Dessen Auswertung ruft
 * `createContext`, und das scheitert auf dem Server. **Beim Importieren, nicht
 * beim Rendern:** Es genügt, das Modul in einer Server-Komponente zu erwähnen.
 *
 * **Kein bestehender Test konnte das finden**, und keiner hätte es können:
 *
 * - `pnpm check` rendert **keine einzige Seite**. Lint, Typprüfung, Format und
 *   Vitest sagen nichts über die Grenze zwischen Server- und Client-Komponente.
 * - Die rendernden Tests laufen in `jsdom` und kennen die Grenze ebenfalls
 *   nicht — dort ist alles Client.
 * - `next build` prerendert diese Seite nicht: `generateMetadata` liest die
 *   Sprache aus dem Cookie, die Route ist damit dynamisch.
 *
 * ## Was hier **nicht** geprüft wird
 *
 * **Nicht, dass Server-Komponenten keine Client-Komponenten importieren.** Das
 * ist der Normalfall und ausdrücklich erlaubt — `seiten-platzhalter.tsx` ist
 * Server und rendert `Leer`, und so soll es sein. Die Grenze ist eine Naht und
 * kein Verbot. **Sie ist zugleich die Abbruchkante der Suche unten:** Was hinter
 * einem `"use client"` liegt, wird nicht weiterverfolgt, weil es dort keine
 * Server-Auswertung mehr gibt.
 *
 * ## Was eine Mutationsprüfung am 01.09.2026 gefunden hat
 *
 * **Von fünfzehn Mutanten blieben neun grün.** Die erste Fassung verglich
 * Zeichenketten: Sie suchte in jeder Server-Datei nach `from "@/components/ui/…"`
 * und sonst nichts. Damit fand sie **eine** Schreibweise **eines** Weges.
 *
 * | Mutant | alte Fassung | Grund |
 * |---|---|---|
 * | Import über einen relativen Pfad | grün | nur der `@`-Alias stand im Muster |
 * | Import mit Endung: `…/button.tsx` | grün | dasselbe Muster, Zeichen für Zeichen |
 * | `await import("…/button")` | grün | das Muster verlangte `from` |
 * | `dynamic(() => import("…/button"))` | grün | dasselbe |
 * | Hülle **in** `components/ui`, die button weiterreicht | grün | nur **direkte** Importe wurden angesehen |
 * | Sammelausgang `components/ui/index.ts` | grün | dasselbe |
 * | `badge.tsx` importiert `@radix-ui/react-slot` | grün | erkannt wurde nur das Sammelpaket `radix-ui` |
 * | radix-Baustein **außerhalb** `components/ui` | grün | die unsichere Menge wurde nur dort gesucht |
 * | `"use client"` ohne Semikolon | grün | die Erkennung verlangte eines |
 *
 * **Deshalb steht unten keine Textsuche mehr, sondern ein Importgraph.** Die
 * Spezifizierer werden **aufgelöst** — Alias, relativer Pfad, fehlende Endung,
 * `index`-Datei —, und von jeder Server-Komponente aus wird die Kette verfolgt,
 * bis sie an einem `"use client"` endet. Geprüft wird seither die
 * **Erreichbarkeit**, nicht die Schreibweise.
 *
 * **Was der Test dadurch nicht wird: der Modulauflöser von Next.js.** Er kennt
 * `@/` und relative Pfade; ein Spezifizierer, der auf kein Projektmodul zeigt,
 * gilt als extern und endet die Kette. Ein aus einer Variablen gebauter
 * Importpfad ist für ihn unsichtbar (`docs/testfestigkeit.md` §10, offener
 * Punkt T‑8).
 */

const WURZEL = fileURLToPath(new URL("../src", import.meta.url));

/** Der Generatorbereich von shadcn/ui. */
const GENERATOR = "components/ui/";

/** Endungen, die ein Modul tragen kann — in der Reihenfolge der Auflösung. */
const ENDUNGEN = [".ts", ".tsx", ".js", ".jsx", ".mjs", ".cjs"] as const;

function dateien(verzeichnis: string): string[] {
  return readdirSync(verzeichnis, { withFileTypes: true }).flatMap((eintrag) => {
    const pfad = join(verzeichnis, eintrag.name);
    if (eintrag.isDirectory()) {
      return dateien(pfad);
    }
    return ENDUNGEN.some((endung) => eintrag.name.endsWith(endung)) ? [pfad] : [];
  });
}

const ALLE = dateien(WURZEL).map((pfad) => relative(WURZEL, pfad).split("\\").join("/"));

/**
 * Kleingeschriebener Schlüssel → tatsächlicher Pfad.
 *
 * **Windows unterscheidet die Schreibweise nicht.** `@/components/UI/button`
 * lädt hier dieselbe Datei und stürzt genauso ab; ein Vergleich über ein `Set`
 * exakter Zeichenketten sähe daran vorbei. Auf einem Linux-Bauknecht wäre der
 * Import ein Fehler — hier ist er einer, der läuft, und das ist der schlimmere.
 */
const VORHANDEN = new Map(ALLE.map((pfad) => [pfad.toLowerCase(), pfad]));

function inhalt(pfad: string): string {
  return readFileSync(join(WURZEL, pfad), "utf8");
}

const TEXT = new Map(ALLE.map((pfad) => [pfad, inhalt(pfad)]));

/**
 * Trägt die Datei die Auszeichnung?
 *
 * **Führende Kommentare sind erlaubt, das Semikolon ist es nicht mehr
 * verpflichtend.** Beides sind Formen, die Next.js ehrt und die erste Fassung
 * nicht erkannte — sie hätte eine Client-Komponente für eine Server-Komponente
 * gehalten und damit einen Fehlalarm ausgelöst.
 */
function istClient(text: string): boolean {
  const ohneVorspann = text.replace(/^(?:\s|\/\/[^\n]*\n|\/\*[\s\S]*?\*\/)*/, "");
  return /^["']use client["']\s*;?/.test(ohneVorspann);
}

/**
 * Alle Modulspezifizierer, die zur **Laufzeit** ausgewertet werden.
 *
 * **Nur `import type …` und `export type …` fallen heraus** — die Anweisung
 * als ganze, erkennbar am Schlüsselwort unmittelbar hinter `import`. Ein
 * einzelnes `type` **in** der Klammer tut es nicht:
 * `import { buttonVariants, type ButtonVariante } from "…"` wertet das Modul
 * aus, und das ist die im Projekt vorherrschende Schreibweise.
 *
 * **Genau daran ist die erste Härtung vom 01.09.2026 gescheitert.** Sie hielt
 * jede Zeile für einen Typbezug, in der irgendwo vor dem Anführungszeichen
 * `type` stand — und übersah damit den Anlassfehler in seiner häufigsten Form.
 * Der Fund stammt aus der adversarischen Runde desselben Tages.
 *
 * Die Grenze wird bewusst zur **sicheren** Seite gezogen: `import { type X }`
 * — eine Klammer, in der jedes Glied ein Typ ist — wird gemeldet, obwohl der
 * Übersetzer sie unter Umständen wegwirft. Ein Fehlalarm kostet eine Minute,
 * ein übersehener Serverabsturz eine Fehlersuche.
 */
function spezifizierer(text: string): string[] {
  const gefunden: string[] = [];
  const muster = [
    // import … from "x" / export … from "x" — aber nicht `import type … from`
    /(?:^|[\s;}])(?:import|export)\s+(?!type[\s{])[^"';]*?\bfrom\s*["']([^"']+)["']/gm,
    // Nebenwirkungsimport: import "x"
    /(?:^|[\s;}])import\s*["']([^"']+)["']/gm,
    // import("x") und require("x") — beide werten das Modul aus. Zwischen der
    // Klammer und dem Spezifizierer darf ein Kommentar stehen: der Bündelname
    // (`/* webpackChunkName: … */`) ist eine gängige Schreibweise, und die
    // erste Härtung ließ dort nur Leerraum zu.
    /\b(?:import|require)\s*\(\s*(?:\/\*[\s\S]*?\*\/\s*)*["']([^"']+)["']/g,
  ];
  for (const m of muster) {
    for (const treffer of text.matchAll(m)) {
      gefunden.push(treffer[1]);
    }
  }
  return gefunden;
}

/**
 * Macht aus einem Spezifizierer den Pfad eines Projektmoduls — oder `null`,
 * wenn er auf ein externes Paket zeigt.
 *
 * Hier steckt die Hälfte der Härtung: Alias **und** relativer Pfad, mit und
 * ohne Endung, Datei **und** `index`.
 */
function aufloesen(spez: string, von: string): string | null {
  let roh: string;
  if (spez.startsWith("@/")) {
    roh = spez.slice(2);
  } else if (spez.startsWith("./") || spez.startsWith("../")) {
    roh = posix.normalize(posix.join(dirname(von).split("\\").join("/"), spez));
  } else {
    return null;
  }
  if (roh.startsWith("../")) return null;

  const ohneEndung = roh.replace(/\.(tsx?|jsx?|mjs|cjs)$/, "");
  for (const kandidat of [
    roh,
    ...ENDUNGEN.map((e) => ohneEndung + e),
    ...ENDUNGEN.map((e) => `${ohneEndung}/index${e}`),
  ]) {
    const treffer = VORHANDEN.get(kandidat.toLowerCase());
    if (treffer !== undefined) return treffer;
  }
  return null;
}

/**
 * Bausteine, die `radix-ui` auswerten und **nicht** sagen, dass sie Client
 * sind. Genau sie sprengen eine Server-Komponente.
 *
 * **Über den ganzen Baum berechnet, nicht nur über `components/ui`** — und die
 * Erkennung trifft beide Schreibweisen: das Sammelpaket `radix-ui` und die
 * Einzelpakete `@radix-ui/react-…`, die ältere Generatorstände schreiben.
 */
const UNSICHER = ALLE.filter((pfad) => {
  const text = TEXT.get(pfad) as string;
  return spezifizierer(text).some((s) => /^@?radix-ui(\/|$)/.test(s)) && !istClient(text);
});

const UNSICHER_MENGE = new Set(UNSICHER);

/** Jede Datei ohne die Auszeichnung — für Next.js also eine Server-Komponente. */
const SERVER = ALLE.filter((pfad) => !pfad.startsWith(GENERATOR)).filter(
  (pfad) => !istClient(TEXT.get(pfad) as string),
);

/**
 * Von `start` aus über **Server-Module** erreichbare unsichere Bausteine.
 *
 * Die Suche endet an jedem `"use client"`: Was dahinter liegt, wird im Browser
 * ausgewertet und kann dort nichts sprengen. Genau deshalb ist der Weg über
 * eine Client-Komponente **kein** Befund.
 */
function erreichbareUnsichere(start: string): string[] {
  const gesehen = new Set<string>([start]);
  const offen = [start];
  const gefunden: string[] = [];

  // **Der Anfang zählt mit.** Eine `page.tsx`, die `radix-ui` unmittelbar
  // importiert, statt den Umweg über `components/ui` zu nehmen, stürzt genauso
  // ab — und die erste Härtung hat sie nie gemeldet, weil sie den Startknoten
  // vorab als besucht markierte und nur *erreichbare* Module prüfte. Gefunden
  // in der adversarischen Runde vom 01.09.2026.
  if (UNSICHER_MENGE.has(start)) gefunden.push(start);

  while (offen.length > 0) {
    const pfad = offen.pop() as string;
    for (const spez of spezifizierer(TEXT.get(pfad) as string)) {
      const ziel = aufloesen(spez, pfad);
      if (ziel === null || gesehen.has(ziel)) continue;
      gesehen.add(ziel);
      if (UNSICHER_MENGE.has(ziel)) {
        gefunden.push(ziel);
        continue;
      }
      // Hinter der Naht wird nicht weitergesucht.
      if (!istClient(TEXT.get(ziel) as string)) offen.push(ziel);
    }
  }
  return gefunden;
}

describe("Server-Komponenten und der Generatorbereich", () => {
  it("prüft überhaupt Dateien", () => {
    // Ohne diese Zusicherung bestünde der Test auch dann, wenn die Sammlung
    // leer bliebe — und niemand merkte es. Dieselbe Vorsorge wie in
    // `tests/farbwerte.test.ts`.
    expect(ALLE.length).toBeGreaterThan(50);
    expect(SERVER.length).toBeGreaterThan(5);
  });

  it("findet den Baustein, um den es geht — sonst prüft die Regel nichts", () => {
    // `button.tsx` ist der Fall, an dem die Regel entstanden ist. Verschwindet
    // er aus der Sammlung, weil ein späteres `shadcn add` ihm die Auszeichnung
    // gibt, ist das eine gute Nachricht — aber dann trägt dieser Test nichts
    // mehr, und das soll auffallen.
    expect(UNSICHER).toContain("components/ui/button.tsx");
  });

  it("erkennt die Auszeichnung in den Formen, die Next.js ehrt", () => {
    // Zwei Mutanten der Runde vom 01.09.2026: ein Kommentar davor und ein
    // fehlendes Semikolon. Beides ehrt Next.js, die erste Fassung nicht.
    expect(istClient(`"use client";\nimport x from "y";`)).toBe(true);
    expect(istClient(`"use client"\nimport x from "y";`)).toBe(true);
    expect(istClient(`/* vom Generator */\n"use client";\n`)).toBe(true);
    expect(istClient(`// Hinweis\n"use client";\n`)).toBe(true);
    expect(istClient(`import x from "y";\n"use client";`)).toBe(false);
    expect(istClient(`import x from "y";`)).toBe(false);
  });

  it("löst die Schreibweisen desselben Weges auf denselben Baustein auf", () => {
    // Die Gegenprobe zum Auflöser. Vier der neun Überlebenden waren nichts
    // anderes als eine andere Schreibweise desselben Imports.
    const von = "app/(app)/administration/page.tsx";
    expect(aufloesen("@/components/ui/button", von)).toBe("components/ui/button.tsx");
    expect(aufloesen("@/components/ui/button.tsx", von)).toBe("components/ui/button.tsx");
    expect(aufloesen("../../../components/ui/button", von)).toBe("components/ui/button.tsx");
    expect(aufloesen("react", von)).toBeNull();
    expect(aufloesen("@/gibt-es-nicht", von)).toBeNull();
  });

  it("sieht die Importformen, die ein Modul auswerten", () => {
    // `import type` ist ausdrücklich keine davon — er verschwindet bei der
    // Übersetzung. Die erste Fassung meldete ihn trotzdem.
    expect(spezifizierer(`import { B } from "@/x";`)).toContain("@/x");
    expect(spezifizierer(`export { B } from "@/x";`)).toContain("@/x");
    expect(spezifizierer(`export * from "@/x";`)).toContain("@/x");
    expect(spezifizierer(`import "@/x";`)).toContain("@/x");
    expect(spezifizierer(`const { B } = await import("@/x");`)).toContain("@/x");
    expect(spezifizierer(`const B = dynamic(() => import("@/x"));`)).toContain("@/x");
    expect(spezifizierer(`await import(/* webpackChunkName: "k" */ "@/x");`)).toContain("@/x");

    // **Der gemischte Import ist einer.** Die erste Härtung vom 01.09.2026
    // verwarf ihn, weil irgendwo vor dem Anführungszeichen `type` stand — und
    // übersah damit den Anlassfehler in seiner häufigsten Schreibweise.
    expect(spezifizierer(`import { B, type T } from "@/x";`)).toContain("@/x");
    expect(spezifizierer(`import B, { type T } from "@/x";`)).toContain("@/x");

    // Und die eine Form, die wirklich verschwindet:
    expect(spezifizierer(`import type { B } from "@/x";`)).not.toContain("@/x");
    expect(spezifizierer(`export type { B } from "@/x";`)).not.toContain("@/x");
  });

  it("meldet auch den Einstieg selbst, nicht nur was er erreicht", () => {
    // Eine `page.tsx`, die `radix-ui` unmittelbar importiert, stürzt genauso
    // ab wie eine, die den Umweg über `components/ui` nimmt. Die erste
    // Härtung markierte den Startknoten vorab als besucht und konnte ihn
    // deshalb nie melden.
    for (const unsicher of UNSICHER) {
      expect(erreichbareUnsichere(unsicher), unsicher).toContain(unsicher);
    }
  });

  it("kennt genau einen Modulalias — ein zweiter käme unbemerkt durch", () => {
    // `aufloesen` versteht `@/` und relative Pfade. Trüge `tsconfig.json` einen
    // zweiten Alias auf dieselbe Wurzel (`~/*` ist die übliche Zweitform),
    // fiele jeder Import darüber stillschweigend als „externes Paket" durch.
    // Diese Zusicherung macht aus dem stillen Durchfallen ein rotes Testergebnis.
    const tsconfig = JSON.parse(
      readFileSync(fileURLToPath(new URL("../tsconfig.json", import.meta.url)), "utf8").replace(
        /^\s*\/\/.*$/gm,
        "",
      ),
    ) as { compilerOptions?: { paths?: Record<string, string[]> } };
    expect(Object.keys(tsconfig.compilerOptions?.paths ?? {})).toEqual(["@/*"]);
  });

  it.each(SERVER)("%s erreicht keinen serverunsicheren Baustein", (pfad) => {
    const gefunden = erreichbareUnsichere(pfad);

    expect(
      gefunden,
      `${pfad} ist eine Server-Komponente und erreicht ${gefunden.join(", ")}. ` +
        `Dieser Baustein wertet \`radix-ui\` aus und trägt kein "use client" — ` +
        `der Import allein wirft zur Laufzeit "createContext only works in Client Components". ` +
        `Der Weg kann über mehrere Module gehen; er endet erst an einem "use client". ` +
        `Entweder das Markup ohne ihn bauen — eine gerahmte Karte mit Fokusring tut es auch — ` +
        `oder den Teil in eine eigene Client-Komponente ziehen.`,
    ).toEqual([]);
  });
});
