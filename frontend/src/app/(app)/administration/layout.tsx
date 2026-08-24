import type { ReactNode } from "react";

import { BereichsNavigation } from "@/components/bereichs-navigation";

/**
 * Der Rahmen des Administrationsbereichs: die Unternavigation über allem, was
 * darin liegt.
 *
 * **Er trägt keine Überschrift.** Jede der drei Seiten setzt ihr eigenes `h1` —
 * die Übersicht „Administration", die Katalogpflege „Prozess-Katalog", die
 * Benutzerverwaltung „Benutzer". Eine Überschrift hier und eine dort ergäbe zwei
 * `h1` auf derselben Seite, und `seiten-platzhalter.tsx` bringt seine ohnehin
 * mit.
 *
 * **Er prüft keine Berechtigung.** Das könnte er auch nicht: Der Rahmen läuft
 * auf dem Server ohne Sitzungsauflösung, und die Rolle steht nicht im Cookie
 * (`src/proxy.ts`). Verbindlich entscheidet das Backend an `/api/katalog`, und
 * die Katalogseite zeigt dessen `403` als eigenen Zustand.
 */
export default function AdministrationLayout({ children }: { children: ReactNode }) {
  return (
    <div className="flex flex-col gap-4">
      <BereichsNavigation />
      {children}
    </div>
  );
}
