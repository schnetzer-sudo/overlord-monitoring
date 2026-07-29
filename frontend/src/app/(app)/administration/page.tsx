import type { Metadata } from "next";

import { SeitenPlatzhalter } from "@/components/seiten-platzhalter";
import { aktiveTexte } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  const texte = await aktiveTexte();
  return { title: texte.navigation.eintraege.administration };
}

/**
 * Platzhalter. Benutzerverwaltung und Katalogpflege entstehen in Schritt 9, nur
 * fuer Rolle ADMIN.
 *
 * Dass der Menuepunkt fuer andere Rollen ausgeblendet ist, ist Bequemlichkeit —
 * durchgesetzt wird die Rolle im Backend an `/api/admin/**`.
 */
export default async function AdministrationPage() {
  const texte = await aktiveTexte();
  return (
    <SeitenPlatzhalter
      titel={texte.navigation.eintraege.administration}
      platzhalterTitel={texte.platzhalter.titel}
      hinweis={texte.platzhalter.hinweis}
    />
  );
}
