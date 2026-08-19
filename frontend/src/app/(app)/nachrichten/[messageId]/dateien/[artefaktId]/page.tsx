import type { Metadata } from "next";

import { ArtefaktAnsicht } from "@/features/nachrichten/components/artefakt-ansicht";
import { aktiveTexte } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  const texte = await aktiveTexte();
  return { title: texte.nachrichten.detail.dateien.ansichtTitel };
}

/**
 * **Ein Artefakt auf seiner eigenen Route** — `/nachrichten/<id>/dateien/<artefaktId>`.
 *
 * Kein Sheet und kein Dialog über der Detailansicht (`docs/rohdaten.md` §3,
 * Entscheidung 7). Das ist ausdrücklich eine spätere Zugabe und nicht Teil
 * dieses Baus. Zwei Gründe, und beide folgen aus den Messungen:
 *
 * 1. **Verlinkbar.** „Schick mir mal den Link" ist bei diesem Werkzeug die
 *    eigentliche Anwendung, und bei einer Datei erst recht.
 * 2. **Eigene Fläche, eigener Bildlauf.** Der Inhalt geht bis 609.995 Byte
 *    (M60). Ein Sheet neben der Liste hätte dafür weder die Breite noch die
 *    Höhe, und es entstünde eine zweite Bildlaufleiste — genau das, was
 *    `docs/frontend-grundlagen.md` §7 ausschließt.
 *
 * ## Volle Inhaltsbreite, anders als beim Nachrichtendetail
 *
 * `/nachrichten/<id>` begrenzt sich auf `--dichte-inhaltsbreite`, weil dort
 * Beschriftungen und Fließtext stehen. **Hier steht keiner.** Der Inhalt ist
 * Rohtext in Festbreitenschrift, und er wird umgebrochen statt waagerecht
 * geschoben (`artefakt-ansicht.tsx`) — jeder Pixel Breite nimmt ihm einen
 * künstlichen Zeilenumbruch weg. Eine Maximalbreite gehört nach
 * `docs/visuelles-konzept.md` §5 in eine Ansicht *mit Fließtext*; die Sätze der
 * Zustandsfelder tragen sie deshalb einzeln (`max-w-prose`), die Datei nicht.
 *
 * ## Hier wird nichts geprüft
 *
 * Weder, ob es die Nachricht gibt, noch ob sie dem Mandanten der Sitzung gehört,
 * noch ob die Kennung eine brauchbare Form hat. Das entscheidet ausschließlich
 * das Backend — und es beantwortet alle drei mit derselben Antwort. **Next.js
 * trifft keine Berechtigungsentscheidungen** (`docs/frontend-grundlagen.md` §2).
 *
 * Die Seite bleibt **Server-Komponente**; `"use client"` steht an der Ansicht
 * darunter.
 */
export default async function ArtefaktPage({
  params,
}: {
  params: Promise<{ messageId: string; artefaktId: string }>;
}) {
  const { messageId, artefaktId } = await params;
  return <ArtefaktAnsicht messageId={messageId} artefaktId={artefaktId} />;
}
