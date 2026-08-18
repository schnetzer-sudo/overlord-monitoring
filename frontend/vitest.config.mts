import { fileURLToPath } from "node:url";
import { defineConfig } from "vitest/config";

/**
 * Bewusst klein: kein React-Plugin, keine Testing Library.
 *
 * Geprüft werden die Entscheidungen, nicht das Markup — der Ablauf nach dem
 * Anmelden, die Gleichheit beider Sprachdateien, die Wortwahl bei 404, das
 * Leeren des Zwischenspeichers und die Zeitstempel ohne Zeitzonenverschiebung.
 * Das sind alles reine Funktionen. Ein gerenderter Baum brächte hier nichts
 * außer Laufzeit und Abhängigkeiten.
 *
 * **Die Ausnahmen sind gezählt, nicht gewachsen** — Stand 17.08.2026 sind es
 * **sechzehn in fünf Dateien**. Diese Zahl wird an genau dieser Stelle geführt;
 * `tests/hilfe/rendern.tsx` und `docs/frontend-grundlagen.md` §9 verweisen
 * darauf, statt sie zu wiederholen (drei Orte für dieselbe Zahl sind zwei zu
 * viel):
 *
 * | Datei | Fälle | Warum ein Baum |
 * |---|---|---|
 * | `tests/detail-baum.test.tsx` | 3 | zwei Sätze, die von Hand grundsätzlich nicht zu sehen sind, und die Regression zum Doppelschlüssel |
 * | `tests/ansicht-umschalter.test.tsx` | 1 | die Sichtbarkeitsregel des Umschalters **ist** eine Klasse, und ihr Umbruchpunkt ist von Hand nicht prüfbar (`docs/frontend-grundlagen.md` §7) |
 * | `tests/bam-block.test.tsx` | 4 | zweimal eine Aussage über **Abwesenheit** (kein Block und keine Anfrage bei `bamAnzahl === 0`, und immer noch keine, solange niemand aufklappt), die Regression zum Schlüssel `(typ, wert)` — und seit dem 13.08.2026 die **Fuge** der zerlegten Beschriftung: ob zwischen Name und Endung ein Leerzeichen entsteht, entscheidet JSX und keine Funktion |
 * | `tests/suche-marken.test.tsx` *(13.08.2026)* | 4 | die Regression zum Schlüssel `(typ, wert)` an den Marken, der unbekannte Typ als Nummer, und zweimal eine Regel, die **selbst** eine Klasse plus ein `title` ist: die Längenregel der Trefferspalte und der Kettenhinweis |
 * | `tests/eigenschaften-block.test.tsx` *(17.08.2026)* | 4 | die Regression zum Schlüssel `${position}:${name}` — derselbe Name in drei Gruppen **ohne `console.error`** —, zweimal eine Aussage über **Abwesenheit** (kein Schalter und keine Anfrage bei `anzahl === 0`, und immer noch keine, solange niemand aufklappt), und der Rückfall „Schritt N", der erst im Baum entsteht |
 *
 * Allen sechzehn ist dasselbe gemeinsam: **Es gibt keinen anderen Ort, an dem sie
 * belegbar wären.** Das ist die Bedingung, nicht „es ließe sich so leichter
 * prüfen". Sie schalten ihre Umgebung selbst über `// @vitest-environment jsdom`
 * um — die Voreinstellung bleibt `node`, damit die übrigen Dateien nichts von
 * einem DOM bezahlen.
 *
 * `setupFiles` trägt das Netz darunter: Ein `console.error` lässt den Testlauf
 * fehlschlagen (`tests/setup/konsole.ts`). Es gilt für **alle** Dateien, nicht
 * nur für die rendernden — eine Meldung aus einer reinen Funktion ist genauso
 * ein Befund.
 */
export default defineConfig({
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
  test: {
    environment: "node",
    include: ["tests/**/*.test.ts", "tests/**/*.test.tsx"],
    setupFiles: ["./tests/setup/konsole.ts"],
  },
});
