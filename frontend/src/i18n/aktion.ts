"use server";

import { cookies } from "next/headers";
import { revalidatePath } from "next/cache";

import { SPRACHE_COOKIE, SPRACHE_COOKIE_DAUER, spracheAus } from "./index";

/**
 * Sprachwahl umschalten.
 *
 * Bewusst eine Server-Aktion und kein `document.cookie`: Das Wurzel-Layout liest
 * die Sprache serverseitig, also muss der neue Wert schon beim nächsten Rendern
 * dort ankommen. `revalidatePath("/", "layout")` erzwingt genau das.
 *
 * Nebeneffekt, der es wert ist: Die Umschaltung ist ein Formular und
 * funktioniert damit auch ohne JavaScript.
 *
 * Der Wert kommt aus `name="sprache"` der auslösenden Schaltfläche. Ein
 * unbekannter Wert fällt still auf Deutsch zurück — hier ist nichts zu
 * validieren, was ein Nutzer falsch machen könnte.
 */
export async function spracheSetzen(daten: FormData): Promise<void> {
  const gewaehlt = daten.get("sprache");
  const speicher = await cookies();
  speicher.set(SPRACHE_COOKIE, spracheAus(typeof gewaehlt === "string" ? gewaehlt : null), {
    path: "/",
    maxAge: SPRACHE_COOKIE_DAUER,
    sameSite: "lax",
    // Kein httpOnly: Der Wert ist keine Auskunft über den Nutzer.
    httpOnly: false,
  });
  revalidatePath("/", "layout");
}
