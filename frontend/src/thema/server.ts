import { cookies } from "next/headers";

import { THEMA_COOKIE, themaAus, type Themawert } from "./index";

/**
 * Das aktive Erscheinungsbild auf dem Server. Nur für Server-Komponenten — im
 * Browser kommt es aus dem Kontext (`useThema`).
 *
 * Das Wurzel-Layout liest es **einmal**: für `data-thema` am Dokument und für
 * den Kontext darunter. Beides muss vor dem ersten Paint feststehen, sonst
 * erschiene die Anwendung kurz im falschen Erscheinungsbild.
 */
export async function aktivesThema(): Promise<Themawert> {
  const speicher = await cookies();
  return themaAus(speicher.get(THEMA_COOKIE)?.value);
}
