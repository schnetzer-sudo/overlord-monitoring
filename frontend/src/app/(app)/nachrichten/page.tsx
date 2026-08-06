import type { Metadata } from "next";

import { NachrichtenAnsicht } from "@/features/nachrichten/components/nachrichten-ansicht";
import { aktiveTexte } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  const texte = await aktiveTexte();
  return { title: texte.navigation.eintraege.nachrichten };
}

/**
 * Die Nachrichtenliste.
 *
 * Die Seite selbst bleibt **Server-Komponente**: `"use client"` steht so weit
 * unten im Baum wie möglich, hier an der Ansicht. Sie holt nichts vor — der
 * Filterzustand steht in der URL und wird im Browser gelesen, und eine
 * serverseitig geholte erste Seite wäre für jeden anderen Filter sofort wieder
 * verworfen.
 *
 * Das Detail einer Zeile entsteht in Schritt 5; bis dahin hat der Zeilenklick
 * keine Funktion.
 */
export default function NachrichtenPage() {
  return <NachrichtenAnsicht />;
}
