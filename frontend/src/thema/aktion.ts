"use server";

import { cookies } from "next/headers";
import { revalidatePath } from "next/cache";

import { THEMA_COOKIE, THEMA_COOKIE_DAUER, THEMA_FELD, themaAus } from "./index";

/**
 * Themawahl umschalten.
 *
 * Bewusst eine Server-Aktion und kein `document.cookie`: Das Wurzel-Layout
 * liest den Wert serverseitig und setzt daraus `data-thema` an `<html>`, also
 * muss der neue Wert schon beim nächsten Rendern dort ankommen.
 * `revalidatePath("/", "layout")` erzwingt genau das — dasselbe Mittel und
 * derselbe Grund wie bei Sprache und Dichte (`i18n/aktion.ts`,
 * `dichte/aktion.ts`).
 *
 * Die Umschaltung ist ein **Formular** und kein `onClick` — sie funktioniert
 * damit auch dann, wenn die Hydratation der Seite hängt.
 *
 * > ⚠️ **Ohne JavaScript funktioniert sie trotzdem nicht**, aus demselben
 * > gemessenen Grund wie bei der Dichte: Der Anwendungsrahmen wird im Browser
 * > gebaut, die servergerenderte Antwort von `/nachrichten` trägt kein
 * > `<form>`. Offener Punkt 98, `docs/dichte-umschalter.md`.
 *
 * Der Wert kommt aus dem Feld {@link THEMA_FELD} der auslösenden Schaltfläche;
 * der Name steht dort und nicht als zweite Zeichenkette hier. Ein unbekannter
 * Wert fällt still auf `system` zurück — hier ist nichts zu validieren, was ein
 * Nutzer falsch machen könnte.
 */
export async function themaSetzen(daten: FormData): Promise<void> {
  const gewaehlt = daten.get(THEMA_FELD);
  const speicher = await cookies();
  speicher.set(THEMA_COOKIE, themaAus(typeof gewaehlt === "string" ? gewaehlt : null), {
    path: "/",
    maxAge: THEMA_COOKIE_DAUER,
    sameSite: "lax",
    // Kein httpOnly: Der Wert ist keine Auskunft über den Nutzer.
    httpOnly: false,
  });
  revalidatePath("/", "layout");
}
