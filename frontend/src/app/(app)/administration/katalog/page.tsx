import type { Metadata } from "next";

import { KatalogAnsicht } from "@/features/katalog/components/katalog-ansicht";
import { aktiveTexte } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  const texte = await aktiveTexte();
  return { title: texte.administration.bereiche.katalog.titel };
}

/**
 * Die Katalogpflege (`docs/prozess-katalog-frontend.md`).
 *
 * Server-Komponente ohne eigene Logik — die Ansicht braucht Zustand,
 * TanStack Query und die URL und ist deshalb Client. `"use client"` steht so
 * weit unten im Baum wie möglich.
 */
export default function KatalogPage() {
  return <KatalogAnsicht />;
}
