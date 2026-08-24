import type { Metadata } from "next";

import { SeitenPlatzhalter } from "@/components/seiten-platzhalter";
import { aktiveTexte } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  const texte = await aktiveTexte();
  return { title: texte.administration.bereiche.benutzer.titel };
}

/**
 * Platzhalter. Die Benutzerverwaltung hat ihr Backend seit Schritt 9a
 * (`docs/benutzerverwaltung-backend.md`); die Oberfläche dazu ist ein eigener
 * Auftrag und ausdrücklich nicht Teil von Schritt 9b
 * (`docs/benutzerverwaltung.md` §7a).
 *
 * **Er ruft nichts auf und zeigt deshalb kein „kein Zugriff".** Der Zustand
 * entsteht dort, wo ein Endpunkt ihn belegt — hier gibt es noch keinen.
 */
export default async function BenutzerPage() {
  const texte = await aktiveTexte();
  return (
    <SeitenPlatzhalter
      titel={texte.administration.bereiche.benutzer.titel}
      platzhalterTitel={texte.platzhalter.titel}
      hinweis={texte.platzhalter.hinweis}
    />
  );
}
