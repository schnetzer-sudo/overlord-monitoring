import type { Metadata } from "next";

import { ProzessAnsicht } from "@/features/nachrichten/components/prozessansicht";
import { aktiveTexte } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  const texte = await aktiveTexte();
  return { title: texte.navigation.eintraege.prozesse };
}

/**
 * Die Prozessansicht — **der Baum Partner → Richtung → Prozess**
 * (`docs/process-view.md` §15).
 *
 * Hier stand bis zum 02.09.2026 ein Platzhalter. Die Route ist dieselbe
 * geblieben; `lib/routen.ts` und `lib/navigation.ts` führen sie seit Schritt 3
 * und haben sie ausdrücklich für diesen Schritt freigehalten.
 *
 * Die Seite bleibt **Server-Komponente**: `"use client"` steht so weit unten im
 * Baum wie möglich, hier an der Ansicht. Sie holt nichts vor — Zeitraum und
 * gewählter Prozess stehen in der URL und werden im Browser gelesen, und ein
 * serverseitig geholter Baum wäre für jede andere Wahl sofort wieder verworfen.
 */
export default function ProzessePage() {
  return <ProzessAnsicht />;
}
