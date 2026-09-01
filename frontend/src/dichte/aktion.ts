"use server";

import { cookies } from "next/headers";
import { revalidatePath } from "next/cache";

import { DICHTE_COOKIE, DICHTE_COOKIE_DAUER, dichteAus } from "./index";

/**
 * Dichtewahl umschalten.
 *
 * Bewusst eine Server-Aktion und kein `document.cookie`: Das Wurzel-Layout liest
 * die Stufe serverseitig und setzt daraus `data-dichte` an `<html>`, also muss
 * der neue Wert schon beim nächsten Rendern dort ankommen.
 * `revalidatePath("/", "layout")` erzwingt genau das — dasselbe Mittel und
 * derselbe Grund wie bei der Sprache (`i18n/aktion.ts`).
 *
 * Nebeneffekt, der es wert ist: Die Umschaltung ist ein Formular. Ohne
 * JavaScript trägt das nur so weit, wie das Menü darüber sich ohne JavaScript
 * öffnen lässt — der Weg selbst braucht keins.
 *
 * Der Wert kommt aus `name="dichte"` der auslösenden Schaltfläche. Ein
 * unbekannter Wert fällt still auf die Vorgabe zurück; hier ist nichts zu
 * validieren, was ein Nutzer falsch machen könnte.
 */
export async function dichteSetzen(daten: FormData): Promise<void> {
  const gewaehlt = daten.get("dichte");
  const speicher = await cookies();
  speicher.set(DICHTE_COOKIE, dichteAus(typeof gewaehlt === "string" ? gewaehlt : null), {
    path: "/",
    maxAge: DICHTE_COOKIE_DAUER,
    sameSite: "lax",
    // Kein httpOnly: Der Wert ist keine Auskunft über den Nutzer.
    httpOnly: false,
  });
  revalidatePath("/", "layout");
}
