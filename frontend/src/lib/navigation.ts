import { Home, Mails, Settings, Workflow, type LucideIcon } from "lucide-react";

import type { Texte } from "@/i18n";

import { ROUTEN } from "./routen";

/**
 * Die Navigation ist **Daten**, kein Markup.
 *
 * Ab Schritt 4 kommen laufend Einträge dazu. Wäre die Navigation als JSX
 * geschrieben, hieße jeder neue Eintrag: Markup in der Seitenleiste ändern,
 * Markup in der Schublade ändern, Markup in der Kopfzeile ändern — und eines
 * davon vergessen. Hier steht der Eintrag einmal.
 */
export type Navigationseintrag = {
  readonly pfad: string;
  /** Schlüssel in `texte.navigation.eintraege` — nie ein fertiger Text. */
  readonly schluessel: keyof Texte["navigation"]["eintraege"];
  readonly symbol: LucideIcon;
  /**
   * **Reine Bequemlichkeit, keine Absicherung.** Ein ausgeblendeter Eintrag
   * schützt nichts: Wer den Pfad kennt, ruft ihn auf. Verbindlich prüft der
   * Endpunkt dahinter die Rolle (`/api/admin/**` verlangt `ROLE_ADMIN`). Diese
   * Markierung erspart dem Nutzer nur einen Menüpunkt, der ihn ohnehin auf ein
   * `403` führte.
   */
  readonly nurAdmin?: boolean;
};

/**
 * **Startseite und Dashboard sind ein Eintrag, nicht zwei.** Ab Schritt 10 ist
 * das Dashboard die Landingpage — es entsteht also genau hier, unter `/`, und
 * nicht unter einem zweiten Pfad. Zwei Menüpunkte auf dieselbe Sache wären vom
 * ersten Tag an Ballast, und der zweite müsste später wieder verschwinden.
 */
export const NAVIGATION: readonly Navigationseintrag[] = [
  { pfad: ROUTEN.startseite, schluessel: "startseite", symbol: Home },
  { pfad: ROUTEN.nachrichten, schluessel: "nachrichten", symbol: Mails },
  { pfad: ROUTEN.prozesse, schluessel: "prozesse", symbol: Workflow },
  { pfad: ROUTEN.administration, schluessel: "administration", symbol: Settings, nurAdmin: true },
];

/** Siehe {@link Navigationseintrag.nurAdmin} — Bequemlichkeit, keine Berechtigung. */
export function sichtbareNavigation(rolle: string | undefined): readonly Navigationseintrag[] {
  return NAVIGATION.filter((eintrag) => !eintrag.nurAdmin || rolle === "ADMIN");
}

/** Aktiv ist der längste passende Eintrag — sonst leuchtet „/" auf jeder Seite. */
export function istAktiv(eintrag: Navigationseintrag, pfad: string): boolean {
  if (eintrag.pfad === ROUTEN.startseite) {
    return pfad === ROUTEN.startseite;
  }
  return pfad === eintrag.pfad || pfad.startsWith(`${eintrag.pfad}/`);
}
