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
 * **Die Ausnahmen sind gezählt, nicht gewachsen** — Stand 18.08.2026 sind es
 * **vierunddreißig in sieben Dateien**. Diese Zahl wird an genau dieser Stelle geführt;
 * `tests/hilfe/rendern.tsx` und `docs/frontend-grundlagen.md` §9 verweisen
 * darauf, statt sie zu wiederholen (drei Orte für dieselbe Zahl sind zwei zu
 * viel):
 *
 * | Datei | Fälle | Warum ein Baum |
 * |---|---|---|
 * | `tests/detail-baum.test.tsx` | 3 | zwei Sätze, die von Hand grundsätzlich nicht zu sehen sind, und die Regression zum Doppelschlüssel |
 * | `tests/ansicht-umschalter.test.tsx` | 2 | die Sichtbarkeitsregel des Umschalters **ist** eine Klasse, und ihr Umbruchpunkt ist von Hand nicht prüfbar (`docs/frontend-grundlagen.md` §7); dazu, dass er im Panel und auf der eigenen Route Verschiedenes sagt. **Am 18.08.2026 von 1 auf 2 berichtigt** — die Datei trug seit Schritt 6 zwei Fälle, die Zählung nur einen. Genau die Drift, gegen die die Regel „an einer Stelle geführt" gerichtet ist |
 * | `tests/bam-block.test.tsx` | 4 | zweimal eine Aussage über **Abwesenheit** (kein Block und keine Anfrage bei `bamAnzahl === 0`, und immer noch keine, solange niemand aufklappt), die Regression zum Schlüssel `(typ, wert)` — und seit dem 13.08.2026 die **Fuge** der zerlegten Beschriftung: ob zwischen Name und Endung ein Leerzeichen entsteht, entscheidet JSX und keine Funktion |
 * | `tests/suche-marken.test.tsx` *(13.08.2026)* | 4 | die Regression zum Schlüssel `(typ, wert)` an den Marken, der unbekannte Typ als Nummer, und zweimal eine Regel, die **selbst** eine Klasse plus ein `title` ist: die Längenregel der Trefferspalte und der Kettenhinweis |
 * | `tests/eigenschaften-block.test.tsx` *(17.08.2026)* | 4 | die Regression zum Schlüssel `${position}:${name}` — derselbe Name in drei Gruppen **ohne `console.error`** —, zweimal eine Aussage über **Abwesenheit** (kein Schalter und keine Anfrage bei `anzahl === 0`, und immer noch keine, solange niemand aufklappt), und der Rückfall „Schritt N", der erst im Baum entsteht |
 * | `tests/artefakt-ansicht.test.tsx` *(18.08.2026)* | 9 | **der Textknoten** — ob aus `<b>fett</b>` ein Element wird oder Text, entscheidet React beim Rendern und keine Funktion; derselbe Test belegt die Bauvorgabe aus M60 (**ein** Kind, kein Element je Zeile). Dazu die **vier Zustände** aus `docs/rohdaten.md` §8, je einer: „keiner ist ein leeres Feld" ist eine Aussage über Anwesenheit von Text und Abwesenheit des Inhaltsfelds. Dazu der Ausschnitt-Vermerk in **beide** Richtungen, der Download-Knopf (Entscheidung 9: Ein Knopf, der etwas anderes verspricht als die Anzeige, **darf nicht im Baum stehen**) und die Beschriftung ohne Nachladen |
 * | `tests/zeitleiste-ziele.test.tsx` *(18.08.2026, Nachbesserung)* | 8 | An die Stelle von `tests/dateien-block.test.tsx` getreten, als der eigene Dateienblock entfiel. Fünf Aussagen, die kein reiner Aufruf trägt: **welche Zeile welches Ziel bekommt** in den drei Lagen (beide Arten, nur eine, keine); dass die Artefakte des **Metadaten-Schritts** über der Leiste erreichbar bleiben, obwohl die Leiste Schritt `0` nicht führt (`docs/nachrichtendetail.md` §4) — eine Aussage über Anwesenheit an einer Stelle ohne Zeile; die **Belastungsprobe aus M55** mit fünfzehn eigenen Zielen und **ohne doppelten React-Schlüssel**; das **Anspringen** der Eigenschaftengruppe, das auf `document.activeElement` endet und damit auf einem Zustand des Dokuments; und die Gegenprobe dazu — ohne Eigenschaften **kein Schalter** am Schrittnamen |
 *
 * Allen vierunddreißig ist dasselbe gemeinsam: **Es gibt keinen anderen Ort, an dem sie
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
