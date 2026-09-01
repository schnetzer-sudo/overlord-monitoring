import type { Metadata } from "next";

import { DashboardAnsicht } from "@/features/dashboard/components/dashboard-ansicht";
import { aktiveTexte } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  const texte = await aktiveTexte();
  return { title: texte.dashboard.titel };
}

/**
 * Die Landingpage — **das Dashboard liegt auf `/`** (Entscheidung E‑r,
 * `docs/dashboard-frontend.md`).
 *
 * Hier stand bis zum 01.09.2026 die bewusst leere Startseite mit ihrem
 * Platzhalter. Sie ist **gefüllt** worden und nicht ersetzt: dieselbe Route,
 * derselbe Navigationseintrag, keine Weiterleitung und kein `/dashboard`
 * daneben. `lib/routen.ts` und `lib/navigation.ts` haben das seit Schritt 3
 * ausdrücklich so vorgesehen.
 *
 * Die Seite selbst bleibt **Server-Komponente**: `"use client"` steht so weit
 * unten im Baum wie möglich, hier an der Ansicht. Sie holt nichts vor — der
 * Zeitraum steht in der URL und wird im Browser gelesen, und eine serverseitig
 * geholte Antwort wäre für jede andere Wahl sofort wieder verworfen.
 */
export default function UebersichtPage() {
  return <DashboardAnsicht />;
}
