import { cookies } from "next/headers";

import { liesPanelbreite, PANELBREITE_COOKIE } from "./panelbreite";

/**
 * **Die gewählte Panelbreite, serverseitig gelesen** — derselbe Leseweg wie die
 * Anzeigegröße (`dichte/server.ts`): Das Wurzel-Layout setzt sie als Variable
 * am `<html>`, bevor der Browser das erste Byte Stylesheet sieht. Ohne gültiges
 * Cookie `null` — dann keine Variable, und es gilt die Breite bis dahin
 * (`lib/panelbreite.ts`, E‑1209).
 */
export async function aktivePanelbreite(): Promise<number | null> {
  const speicher = await cookies();
  return liesPanelbreite(speicher.get(PANELBREITE_COOKIE)?.value);
}
