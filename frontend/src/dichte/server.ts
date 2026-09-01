import { cookies } from "next/headers";

import { DICHTE_COOKIE, dichteAus, type Dichtestufe } from "./index";

/**
 * Die aktive Dichtestufe auf dem Server. Nur für Server-Komponenten — im
 * Browser kommt sie aus dem Kontext (`useDichte`).
 *
 * Das Wurzel-Layout liest sie **einmal**: für `data-dichte` am Dokument und für
 * den Kontext darunter. Beides muss vor dem ersten Paint feststehen, sonst
 * erschiene die Anwendung kurz in der falschen Größe.
 */
export async function aktiveDichte(): Promise<Dichtestufe> {
  const speicher = await cookies();
  return dichteAus(speicher.get(DICHTE_COOKIE)?.value);
}
