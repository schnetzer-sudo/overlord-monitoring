import type { Metadata } from "next";

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { MandantenListe } from "@/features/sitzung/components/mandanten-liste";
import { aktiveTexte } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  const texte = await aktiveTexte();
  return { title: texte.mandantenauswahl.titel };
}

/**
 * Mandantenauswahl.
 *
 * Erscheint von selbst, solange kein Mandant aktiv ist — bei jedem ADMIN und bei
 * jedem Nutzer mit mehreren Mandanten ist das der Zustand direkt nach dem
 * Anmelden. Wer genau einen Mandanten hat, bekommt ihn beim Anmelden gesetzt und
 * sieht diese Seite nur, wenn er sie selbst aufruft.
 *
 * Dieselbe Seite ist der Weg zum **Wechsel**: Der aktive Mandant in der
 * Kopfzeile führt hierher.
 */
export default async function MandantenauswahlPage() {
  const texte = await aktiveTexte();

  return (
    <div className="mx-auto w-full max-w-md">
      <Card>
        <CardHeader>
          <CardTitle>{texte.mandantenauswahl.titel}</CardTitle>
          <CardDescription>{texte.mandantenauswahl.einleitung}</CardDescription>
        </CardHeader>
        <CardContent>
          <MandantenListe />
        </CardContent>
      </Card>
    </div>
  );
}
