import { MutationCache, QueryCache, QueryClient, isServer } from "@tanstack/react-query";

import { istEndgueltig, istNichtAngemeldet, istZeitgrenze } from "./http";
import { ROUTEN, WEITER_PARAMETER } from "./routen";

/**
 * Serverzustand liegt ausschließlich in TanStack Query — kein eigener Store,
 * keine eigene Datenhaltung. Das Frontend spricht nur über HTTP mit dem Backend.
 */

/**
 * Die **einzige** Stelle, die auf einen Sitzungsverlust reagiert.
 *
 * Bewusst eine harte Navigation statt `router.replace`: Sie wirft den kompletten
 * Zustand im Speicher weg, nicht nur den Zwischenspeicher. Ein `401` ist kein
 * Fehler, den man anzeigt — er heißt „die Sitzung ist weg", und darauf gibt es
 * genau eine sinnvolle Antwort.
 */
function aufAnmeldungUmleiten(): void {
  if (typeof window === "undefined") {
    return;
  }
  const ort = window.location;
  if (ort.pathname === ROUTEN.anmeldung) {
    // Die abgelehnte Anmeldung selbst antwortet ebenfalls mit 401. Dort gehört
    // eine Meldung hin, keine Weiterleitung im Kreis.
    return;
  }
  const woher = `${ort.pathname}${ort.search}`;
  const ziel = `${ROUTEN.anmeldung}?${WEITER_PARAMETER}=${encodeURIComponent(woher)}`;
  window.location.replace(ziel);
}

function behandleFehler(fehler: unknown): void {
  if (istNichtAngemeldet(fehler)) {
    aufAnmeldungUmleiten();
  }
}

function erzeugeQueryClient(): QueryClient {
  return new QueryClient({
    queryCache: new QueryCache({ onError: behandleFehler }),
    mutationCache: new MutationCache({ onError: behandleFehler }),
    defaultOptions: {
      queries: {
        // Gelesen wird zur Laufzeit auf der Produktionsdatenbank. Jede Abfrage,
        // die nicht gestellt werden muss, wird nicht gestellt.
        staleTime: 30_000,
        refetchOnWindowFocus: false,
        // Kein zweiter Versuch bei 401, 403 und 404: Das Ergebnis stand schon
        // beim ersten Aufruf fest. Ohne diese Regel wartet der Nutzer mehrere
        // Sekunden auf eine Meldung, die sich nicht mehr ändern kann.
        //
        // Und keiner bei einer abgebrochenen Suche: Dort stellte der zweite
        // Versuch dieselbe Abfrage noch einmal und liefe wieder in dieselbe
        // Zeitgrenze — zehn weitere Sekunden auf der Produktionsdatenbank für
        // eine Antwort, die schon feststeht.
        retry: (versuche, fehler) =>
          !istEndgueltig(fehler) && !istZeitgrenze(fehler) && versuche < 1,
      },
      mutations: {
        retry: false,
      },
    },
  });
}

let clientImBrowser: QueryClient | undefined;

/**
 * Auf dem Server jedes Mal ein frischer Client, damit sich Anfragen
 * verschiedener Nutzer keinen Cache teilen — das wäre ein Bruch der
 * Mandantentrennung. Im Browser genau einer.
 */
export function getQueryClient(): QueryClient {
  if (isServer) {
    return erzeugeQueryClient();
  }
  clientImBrowser ??= erzeugeQueryClient();
  return clientImBrowser;
}
