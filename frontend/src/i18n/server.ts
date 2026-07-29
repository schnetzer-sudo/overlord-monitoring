import { cookies } from "next/headers";

import { SPRACHE_COOKIE, spracheAus, texteFuer, type Sprache, type Texte } from "./index";

/**
 * Die aktive Sprache auf dem Server. Nur für Server-Komponenten — im Browser
 * kommt sie aus dem Kontext (`useTexte`).
 *
 * Das Wurzel-Layout liest sie einmal, setzt damit `<html lang>` und reicht die
 * passende Sprachdatei in den Client-Kontext. Dadurch liegt immer nur **eine**
 * Sprachdatei im ausgelieferten Zustand, nicht beide.
 */
export async function aktiveSprache(): Promise<Sprache> {
  const speicher = await cookies();
  return spracheAus(speicher.get(SPRACHE_COOKIE)?.value);
}

export async function aktiveTexte(): Promise<Texte> {
  return texteFuer(await aktiveSprache());
}
