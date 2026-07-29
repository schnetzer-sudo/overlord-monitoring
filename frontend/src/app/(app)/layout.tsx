import type { ReactNode } from "react";

import { Anwendungsrahmen } from "@/components/anwendungsrahmen";

/**
 * Layout der Routengruppe „angemeldet".
 *
 * Alles darunter läuft im Anwendungsrahmen — Kopfzeile mit aktivem Mandanten,
 * Navigation, Nutzermenü — und durchläuft dort den Ablauf nach dem Anmelden:
 * Änderungszwang vor Mandantenauswahl vor Startseite.
 *
 * **Der Rahmen schützt nichts.** Die Berechtigungsentscheidung trifft immer das
 * Backend.
 */
export default function AngemeldetLayout({ children }: { children: ReactNode }) {
  return <Anwendungsrahmen>{children}</Anwendungsrahmen>;
}
