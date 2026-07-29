import type { Metadata } from "next";

import { SeitenPlatzhalter } from "@/components/seiten-platzhalter";
import { aktiveTexte } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  const texte = await aktiveTexte();
  return { title: texte.navigation.eintraege.nachrichten };
}

/** Platzhalter. Die Liste entsteht in Schritt 4, das Detail in Schritt 5. */
export default async function NachrichtenPage() {
  const texte = await aktiveTexte();
  return (
    <SeitenPlatzhalter
      titel={texte.navigation.eintraege.nachrichten}
      platzhalterTitel={texte.platzhalter.titel}
      hinweis={texte.platzhalter.hinweis}
    />
  );
}
