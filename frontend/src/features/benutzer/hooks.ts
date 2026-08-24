"use client";

import { useQuery } from "@tanstack/react-query";

import { BENUTZER_SCHLUESSEL, holeNutzer, type Nutzerzeile } from "./api";

/**
 * Die Kontenliste. **Ein Fetch, keine Paginierung** (E16).
 *
 * **Ohne eigenes `staleTime`**, also mit den 30 Sekunden aus
 * `lib/query-client.ts`. Das ist hier die richtige Voreinstellung und nicht die
 * bequeme: Die Liste ändert sich fast nur durch die fünf Vorgänge dieser Seite
 * selbst, und deren Antwort **ist** die geänderte Zeile — sie wird gesetzt und
 * nicht nachgeholt.
 *
 * **Kein Mandant im Schlüssel** (E2): Die Liste ist mandantenfrei. Der
 * Mandantenwechsel leert den Zwischenspeicher trotzdem vollständig
 * (`lib/zwischenspeicher.ts`) — das kostet hier einen Fetch und ist der Preis
 * dafür, dass die Regel ohne Ausnahme gilt.
 */
export function useNutzer() {
  return useQuery<Nutzerzeile[]>({
    queryKey: BENUTZER_SCHLUESSEL.liste,
    queryFn: holeNutzer,
  });
}
