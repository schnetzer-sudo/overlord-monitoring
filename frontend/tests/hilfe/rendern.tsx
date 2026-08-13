import { act, type ReactNode } from "react";
import { createRoot } from "react-dom/client";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

import { texteFuer } from "@/i18n";
import { SpracheProvider } from "@/i18n/provider";

/**
 * **Die kleinste Hülle, die eine Komponente dieses Projekts rendert.**
 *
 * Geprüft werden hier weiterhin Entscheidungen und kein Markup
 * (`docs/frontend-grundlagen.md` §9). Es gibt sieben Fälle, für die das nicht
 * reicht, und für sie existiert diese Datei: drei in
 * `tests/detail-baum.test.tsx` — zwei davon von Hand grundsätzlich nicht
 * erreichbar, der dritte die Regression zum Doppelschlüssel vom 11.08.2026 —,
 * einer in `tests/ansicht-umschalter.test.tsx`, wo die zu prüfende Regel selbst
 * eine CSS-Klasse ist und ihr Umbruchpunkt von Hand nicht prüfbar
 * (`docs/frontend-grundlagen.md` §7), und drei in `tests/bam-block.test.tsx`.
 *
 * **Der Zuwachs am 12.08.2026 ist eine Entscheidung und keine Bequemlichkeit.**
 * Die drei neuen Fälle sind dieselbe Klasse wie der Doppelschlüssel: zweimal
 * eine Aussage über **Abwesenheit** (kein Block im Baum, keine Anfrage) und
 * einmal die Regression zum Schlüssel `(typ, wert)`, die genau dann besteht,
 * wenn **kein `console.error`** fällt. Keiner davon ist ohne Baum belegbar.
 *
 * **Die Zahl steht hier, damit sie beim nächsten Zuwachs eine Entscheidung
 * verlangt.** Die Bedingung ist nicht „ein Baum wäre bequemer", sondern „es gibt
 * keinen anderen Ort, an dem der Satz belegbar wäre".
 *
 * **Bewusst ohne Testing Library und ohne React-Plugin.** Gebraucht wird ein
 * Baum im DOM, mehr nicht: `createRoot` plus `act` leisten das, und die einzige
 * neue Abhängigkeit ist `jsdom`. Abgefragt wird über `querySelector` — für drei
 * Tests ist das kürzer als eine Abfragesprache, die man erst lernen muss.
 *
 * **Zeitzone ohne Provider.** `useAnzeigezone` fällt ohne Kontext auf UTC
 * zurück (`components/zeitzone.tsx`), und genau das ist hier richtig: Die Tests
 * sagen nichts über die Anzeigezone.
 */

(
  globalThis as typeof globalThis & { IS_REACT_ACT_ENVIRONMENT?: boolean }
).IS_REACT_ACT_ENVIRONMENT = true;

/**
 * Ein Zwischenspeicher, der **nichts holt**: Die Antwortrümpfe werden gestellt
 * (`setQueryData`), es gibt kein Netz und keine Datenbank. `staleTime: Infinity`
 * verhindert, dass eine gestellte Antwort beim Einhängen doch noch nachgeladen
 * wird; `retry: false`, damit ein Fehlgriff sofort auffällt statt nach Wartezeit.
 *
 * **Nicht `getQueryClient()`** aus `lib/query-client.ts`: Der ist im Browser ein
 * Einzelstück und trüge Daten von einem Test in den nächsten.
 */
export function neuerZwischenspeicher(): QueryClient {
  return new QueryClient({
    defaultOptions: { queries: { retry: false, staleTime: Infinity, gcTime: Infinity } },
  });
}

export type Gerendert = {
  behaelter: HTMLElement;
  zwischenspeicher: QueryClient;
  /** Hängt den Baum wieder aus. Gehört in ein `finally` — sonst überlebt er den Test. */
  abbauen: () => Promise<void>;
};

export async function rendere(
  baum: ReactNode,
  zwischenspeicher: QueryClient = neuerZwischenspeicher(),
): Promise<Gerendert> {
  const behaelter = document.createElement("div");
  document.body.appendChild(behaelter);
  const wurzel = createRoot(behaelter);

  // `await act`, nicht `act`: Ein nicht abgewartetes `act` meldet sich selbst
  // über console.error — und das ist seit `tests/setup/konsole.ts` ein
  // Testfehler. Die Regel prüft sich hier also gleich mit.
  await act(async () => {
    wurzel.render(
      <QueryClientProvider client={zwischenspeicher}>
        <SpracheProvider sprache="de" texte={texteFuer("de")}>
          {baum}
        </SpracheProvider>
      </QueryClientProvider>,
    );
  });

  return {
    behaelter,
    zwischenspeicher,
    abbauen: async () => {
      await act(async () => {
        wurzel.unmount();
      });
      behaelter.remove();
    },
  };
}
