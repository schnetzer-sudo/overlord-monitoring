import type { Metadata } from "next";

import { BenutzerAnsicht } from "@/features/benutzer/components/benutzer-ansicht";
import { aktiveTexte } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  const texte = await aktiveTexte();
  return { title: texte.administration.bereiche.benutzer.titel };
}

/**
 * Die Benutzerverwaltung (`docs/benutzerverwaltung-frontend.md`).
 *
 * Server-Komponente ohne eigene Logik — die Ansicht braucht Zustand und
 * TanStack Query und ist deshalb Client. `"use client"` steht so weit unten im
 * Baum wie möglich.
 *
 * **Sie prüft die Rolle nicht.** Das könnte sie auch nicht: Sie ruft kein
 * Backend auf. Verbindlich entscheidet `/api/admin/**`, und die Ansicht zeigt
 * dessen `403` als eigenen Zustand — dieselbe Bauform wie bei der Katalogpflege
 * und keine zweite.
 *
 * **Ohne `Button` und ohne jeden anderen Baustein, der `radix-ui` auswertet.**
 * `components/ui/button.tsx` trägt kein `"use client"` und wirft in einer
 * Server-Komponente schon beim Importieren (`docs/frontend-grundlagen.md` §8);
 * `tests/serverbausteine.test.ts` hält das seit dem 24.08.2026 fest. Diese Seite
 * importiert nur die Client-Ansicht, und das ist der Normalfall.
 */
export default function BenutzerPage() {
  return <BenutzerAnsicht />;
}
