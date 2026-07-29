import type { Metadata } from "next";

import { SeitenPlatzhalter } from "@/components/seiten-platzhalter";
import { aktiveTexte } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  const texte = await aktiveTexte();
  return { title: texte.navigation.eintraege.prozesse };
}

/** Platzhalter. Die nach kuratiertem Partner gruppierte Ansicht entsteht in Schritt 10. */
export default async function ProzessePage() {
  const texte = await aktiveTexte();
  return (
    <SeitenPlatzhalter
      titel={texte.navigation.eintraege.prozesse}
      platzhalterTitel={texte.platzhalter.titel}
      hinweis={texte.platzhalter.hinweis}
    />
  );
}
