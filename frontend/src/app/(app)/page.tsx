import type { Metadata } from "next";

import { Leer } from "@/components/zustand";
import { aktiveTexte } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  const texte = await aktiveTexte();
  return { title: texte.startseite.titel };
}

/**
 * Die Startseite: geschützt und bewusst leer.
 *
 * Auch sie behandelt ihre Zustände sichtbar — hier ist es der Zustand „leer",
 * und der sieht nicht aus wie ein Fehler. Ab Schritt 10 steht hier das
 * Dashboard.
 */
export default async function StartseitePage() {
  const texte = await aktiveTexte();

  return (
    <div className="space-y-4">
      <h1 className="text-ueberschrift font-semibold">{texte.startseite.titel}</h1>
      <Leer
        titel={texte.startseite.platzhalterTitel}
        hinweis={texte.startseite.platzhalterHinweis}
      />
    </div>
  );
}
