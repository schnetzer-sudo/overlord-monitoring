"use server";

import { cookies } from "next/headers";
import { revalidatePath } from "next/cache";

import { DICHTE_COOKIE, DICHTE_COOKIE_DAUER, DICHTE_FELD, dichteAus } from "./index";

/**
 * Dichtewahl umschalten.
 *
 * Bewusst eine Server-Aktion und kein `document.cookie`: Das Wurzel-Layout liest
 * die Stufe serverseitig und setzt daraus `data-dichte` an `<html>`, also muss
 * der neue Wert schon beim nächsten Rendern dort ankommen.
 * `revalidatePath("/", "layout")` erzwingt genau das — dasselbe Mittel und
 * derselbe Grund wie bei der Sprache (`i18n/aktion.ts`).
 *
 * Die Umschaltung ist ein **Formular** und kein `onClick` — sie funktioniert
 * damit auch dann, wenn die Hydratation der Seite hängt.
 *
 * > ⚠️ **Ohne JavaScript funktioniert sie trotzdem nicht, und das ist gemessen
 * > und nicht geschätzt.** Der Anwendungsrahmen wird im Browser gebaut: Die
 * > servergerenderte Antwort von `/nachrichten` ist **1.379 Zeichen** Markup —
 * > kein `<header>`, kein `<form>`, keine `$ACTION_ID_`. Was der Server
 * > ausliefert, ist ein leerer Rahmen. Der Nebeneffekt, den `i18n/aktion.ts`
 * > für die Sprache in Anspruch nimmt, trägt deshalb **nur auf der
 * > Anmeldeseite** — dort steht das Sprachformular wirklich im Markup.
 * > Ausgeschrieben in `docs/dichte-umschalter.md`, offener Punkt 98.
 *
 * Der Wert kommt aus dem Feld {@link DICHTE_FELD} der auslösenden Schaltfläche;
 * der Name steht dort und nicht als zweite Zeichenkette hier. Ein unbekannter
 * Wert fällt still auf die Vorgabe zurück — hier ist nichts zu validieren, was
 * ein Nutzer falsch machen könnte.
 */
export async function dichteSetzen(daten: FormData): Promise<void> {
  const gewaehlt = daten.get(DICHTE_FELD);
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
