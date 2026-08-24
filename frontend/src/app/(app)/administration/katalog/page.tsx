import type { Metadata } from "next";

import { SeitenPlatzhalter } from "@/components/seiten-platzhalter";
import { aktiveTexte } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  const texte = await aktiveTexte();
  return { title: texte.administration.bereiche.katalog.titel };
}

/** Die Pflegeliste entsteht in Teil 3 dieses Auftrags. */
export default async function KatalogPage() {
  const texte = await aktiveTexte();
  return (
    <SeitenPlatzhalter
      titel={texte.administration.bereiche.katalog.titel}
      platzhalterTitel={texte.platzhalter.titel}
      hinweis={texte.platzhalter.hinweis}
    />
  );
}
