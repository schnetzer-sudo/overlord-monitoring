"use client";

import { useCallback } from "react";
import { useRouter } from "next/navigation";

import { useTexte } from "@/i18n/provider";
import { ROUTEN } from "@/lib/routen";

import { NachrichtDetail } from "./nachricht-detail";

/**
 * Die Detailansicht als **eigene Seite** — `/nachrichten/<id>`.
 *
 * Derselbe Baustein wie im Panel; hier nur ohne die Liste daneben. Die Route
 * existiert, weil ein Link auf einen Beleg die eigentliche Anwendung ist
 * („schick mir mal den Link") und weil die BAM-Suche in Schritt 7 einen Einstieg
 * ohne Liste braucht.
 *
 * ## Schließen führt zurück zur Liste
 *
 * Und zwar **mit dem Filterzustand, der in der URL steht**. Der wird nicht
 * nebenher gemerkt: Was die URL nicht ausdrückt, existiert nicht
 * (`frontend-grundlagen.md` §8). Trägt der geöffnete Link Filter — etwa weil er
 * aus einer geteilten Ansicht stammt —, kommen sie mit; trägt er keine, gilt auf
 * der Liste wieder das Standardfenster des Servers.
 *
 * Gelesen wird die Abfragezeichenkette **im Ereignis** aus `window.location`
 * und nicht über `useSearchParams`. Der Hook zwingt die Seite unter eine
 * Suspense-Grenze; gebraucht wird der Wert aber erst beim Klick, und dort ist
 * der Browser ohnehin da.
 */
export function NachrichtSeite({ messageId }: { messageId: string }) {
  const texte = useTexte();
  const router = useRouter();

  const zurueck = useCallback(() => {
    router.push(`${ROUTEN.nachrichten}${window.location.search}`);
  }, [router]);

  return (
    // Eine Maximalbreite **innerhalb** der Ansicht, nie am Rahmen
    // (`visuelles-konzept.md` §5): Ohne sie liefe der Kopf über 1920 px
    // auseinander, und die Zeitleiste ist kein Datenraster, das Breite braucht.
    <div className="max-w-inhalt flex flex-col gap-4">
      <NachrichtDetail
        messageId={messageId}
        aufSchliessen={zurueck}
        schliessenText={texte.nachrichten.detail.zurueckZurListe}
      />
    </div>
  );
}
