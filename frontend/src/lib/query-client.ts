import { QueryClient, isServer } from "@tanstack/react-query";

/**
 * Serverzustand liegt ausschliesslich in TanStack Query — kein eigener Store,
 * keine eigene Datenhaltung. Das Frontend spricht nur ueber HTTP mit dem Backend.
 */
function erzeugeQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        // Gelesen wird zur Laufzeit auf der Produktionsdatenbank. Jede Abfrage,
        // die nicht gestellt werden muss, wird nicht gestellt.
        staleTime: 30_000,
        refetchOnWindowFocus: false,
        retry: 1,
      },
    },
  });
}

let clientImBrowser: QueryClient | undefined;

/**
 * Auf dem Server jedes Mal ein frischer Client, damit sich Anfragen
 * verschiedener Nutzer keinen Cache teilen — das waere ein Bruch der
 * Mandantentrennung. Im Browser genau einer.
 */
export function getQueryClient(): QueryClient {
  if (isServer) {
    return erzeugeQueryClient();
  }
  clientImBrowser ??= erzeugeQueryClient();
  return clientImBrowser;
}
