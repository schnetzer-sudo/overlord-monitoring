import type { Metadata } from "next";

import { NachrichtSeite } from "@/features/nachrichten/components/nachricht-seite";
import { aktiveTexte } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  const texte = await aktiveTexte();
  return { title: texte.nachrichten.detail.titel };
}

/**
 * Der zweite Einhängepunkt der Detailansicht: **dieselbe Komponente als eigene
 * Seite**.
 *
 * `/nachrichten?nachricht=<id>` zeigt sie neben der Liste, `/nachrichten/<id>`
 * für sich. **Keine abfangende Route** (`(.)`-Konvention des App Routers) — das
 * ist der komplexeste Teil des Routings für einen Gewinn, den wir nicht
 * brauchen. Zwei schlichte Einhängepunkte, eine Komponente.
 *
 * **Die Kennung wird hier nicht geprüft.** Ob es sie gibt und ob sie zum
 * Mandanten der Sitzung gehört, entscheidet ausschließlich das Backend — und es
 * beantwortet beides mit derselben Antwort. Next.js trifft keine
 * Berechtigungsentscheidungen.
 *
 * Die Seite bleibt **Server-Komponente**; `"use client"` steht an der Ansicht
 * darunter.
 */
export default async function NachrichtDetailPage({
  params,
}: {
  params: Promise<{ messageId: string }>;
}) {
  const { messageId } = await params;
  return <NachrichtSeite messageId={messageId} />;
}
