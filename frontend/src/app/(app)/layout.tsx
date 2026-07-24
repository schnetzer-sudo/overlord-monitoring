import type { ReactNode } from "react";

/**
 * Layout der Routengruppe "angemeldet".
 *
 * <p>Noch ohne Inhalt. Der Anwendungsrahmen — Navigation, Kopfzeile, Nutzermenue,
 * Abmelden — entsteht in Schritt 3, der Schutz der Routen ebenfalls. Die
 * Berechtigungsentscheidung trifft dabei immer das Backend; rollenabhaengige
 * Navigation hier ist reine Bequemlichkeit.
 */
export default function AngemeldetLayout({ children }: { children: ReactNode }) {
  return <>{children}</>;
}
