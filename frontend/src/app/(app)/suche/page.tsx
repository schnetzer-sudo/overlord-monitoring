import type { Metadata } from "next";

import { SucheAnsicht } from "@/features/nachrichten/components/suche-ansicht";
import { aktiveTexte } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  const texte = await aktiveTexte();
  return { title: texte.suche.titel };
}

/**
 * Die Belegsuche — der Einstiegspunkt des MVP.
 *
 * Die Seite selbst bleibt **Server-Komponente**: `"use client"` steht so weit
 * unten im Baum wie möglich, hier an der Ansicht. Sie holt nichts vor — die
 * Begriffe stehen in der URL und werden im Browser gelesen, und eine serverseitig
 * geholte erste Antwort wäre für jeden anderen Begriff sofort wieder verworfen.
 *
 * **Es gibt keinen Navigationseintrag hierher.** Das Feld in der Kopfzeile ist der
 * Weg, und es steht auf jeder Seite.
 */
export default function SuchePage() {
  return <SucheAnsicht />;
}
