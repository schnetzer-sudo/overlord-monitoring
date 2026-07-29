import { Suspense } from "react";
import type { Metadata } from "next";

import { Sprachumschaltung } from "@/components/sprachumschaltung";
import { Laden } from "@/components/zustand";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { AnmeldeFormular } from "@/features/sitzung/components/anmelde-formular";
import { aktiveTexte } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  const texte = await aktiveTexte();
  return { title: texte.anmeldung.titel };
}

/**
 * Die einzige Seite außerhalb des Anwendungsrahmens.
 *
 * Die Sprachumschaltung steht auch hier — wer sich nicht anmelden kann, muss die
 * Fehlermeldung trotzdem lesen können.
 *
 * Das Formular liegt hinter einer Suspense-Grenze, weil es `useSearchParams`
 * verwendet (den `weiter`-Parameter). Ohne diese Grenze bricht der Build erst
 * beim Erzeugen der statischen Seiten ab — und dann sucht man an der falschen
 * Stelle.
 */
export default async function AnmeldungPage() {
  const texte = await aktiveTexte();

  return (
    <main className="flex min-h-dvh flex-col items-center justify-center px-3 py-8">
      <div className="w-full max-w-sm space-y-4">
        <div className="flex items-center justify-between gap-2">
          <span className="text-ueberschrift min-w-0 truncate font-semibold">
            {texte.anwendung.name}
          </span>
          <Sprachumschaltung />
        </div>

        <Card>
          <CardHeader>
            <CardTitle>{texte.anmeldung.titel}</CardTitle>
            <CardDescription>{texte.anmeldung.einleitung}</CardDescription>
          </CardHeader>
          <CardContent>
            <Suspense fallback={<Laden zeilen={3} />}>
              <AnmeldeFormular />
            </Suspense>
          </CardContent>
        </Card>
      </div>
    </main>
  );
}
