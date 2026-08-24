import { readFileSync, readdirSync } from "node:fs";
import { join, relative } from "node:path";
import { fileURLToPath } from "node:url";

import { describe, expect, it } from "vitest";

/**
 * Blockierungstest: **keine Server-Komponente importiert einen Baustein, der auf
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
 * Sichtbar war der Fehler ausschließlich, indem man die Seite **aufruft**. Genau
 * das ist die Lücke, die dieser Test schließt.
 *
 * ## Was hier **nicht** geprüft wird
 *
 * **Nicht, dass Server-Komponenten keine Client-Komponenten importieren.** Das
 * ist der Normalfall und ausdrücklich erlaubt — `seiten-platzhalter.tsx` ist
 * Server und rendert `Leer`, und so soll es sein. Die Grenze ist eine Naht und
 * kein Verbot.
 *
 * Geprüft wird der **Sonderfall eines falsch ausgezeichneten Moduls**: eines,
 * das `radix-ui` auswertet und trotzdem nicht sagt, dass es Client ist. Es
 * verhält sich wie eine Server-Komponente und ist keine.
 *
 * ## Warum die Liste berechnet und nicht geschrieben wird
 *
 * `components/ui` ist Generatorbereich. Welche Datei die Auszeichnung trägt,
 * entscheidet `shadcn add` und nicht dieses Projekt; eine Liste von Hand liefe
 * dem nächsten `add` hinterher. Der Test liest sie deshalb bei jedem Lauf neu
 * aus den Dateien.
 */

const WURZEL = fileURLToPath(new URL("../src", import.meta.url));

function dateien(verzeichnis: string): string[] {
  return readdirSync(verzeichnis, { withFileTypes: true }).flatMap((eintrag) => {
    const pfad = join(verzeichnis, eintrag.name);
    if (eintrag.isDirectory()) {
      return dateien(pfad);
    }
    return /\.tsx?$/.test(eintrag.name) ? [pfad] : [];
  });
}

const ALLE = dateien(WURZEL).map((pfad) => relative(WURZEL, pfad).split("\\").join("/"));

function inhalt(pfad: string): string {
  return readFileSync(join(WURZEL, pfad), "utf8");
}

/** Trägt die Datei die Auszeichnung? Sie muss ganz oben stehen, vor jedem Import. */
function istClient(text: string): boolean {
  return /^\s*["']use client["'];/.test(text);
}

/**
 * Bausteine aus `components/ui`, die `radix-ui` auswerten und **nicht** sagen,
 * dass sie Client sind. Genau sie sprengen eine Server-Komponente.
 */
const UNSICHER = ALLE.filter((pfad) => pfad.startsWith("components/ui/")).filter((pfad) => {
  const text = inhalt(pfad);
  return /from\s+["']radix-ui["']/.test(text) && !istClient(text);
});

/** Jede Datei ohne die Auszeichnung — für Next.js also eine Server-Komponente. */
const SERVER = ALLE.filter((pfad) => !pfad.startsWith("components/ui/")).filter(
  (pfad) => !istClient(inhalt(pfad)),
);

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

  it.each(SERVER)("%s importiert keinen serverunsicheren Baustein", (pfad) => {
    const text = inhalt(pfad);

    for (const baustein of UNSICHER) {
      const alias = `@/${baustein.replace(/\.tsx?$/, "")}`;
      const treffer = new RegExp(`from\\s+["']${alias}["']`).exec(text);

      expect(
        treffer,
        `${pfad} ist eine Server-Komponente und importiert ${alias}. ` +
          `Dieser Baustein wertet \`radix-ui\` aus und trägt kein "use client" — ` +
          `der Import allein wirft zur Laufzeit "createContext only works in Client Components". ` +
          `Entweder das Markup ohne ihn bauen — eine gerahmte Karte mit Fokusring tut es auch — ` +
          `oder den Teil in eine eigene Client-Komponente ziehen.`,
      ).toBeNull();
    }
  });
});
