import type { Metadata } from "next";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { PasswortBereich } from "@/features/sitzung/components/passwort-bereich";
import { aktiveTexte } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  const texte = await aktiveTexte();
  return { title: texte.passwort.titel };
}

/**
 * Passwortänderung — erzwungen bei gesetztem Änderungszwang, sonst über das
 * Nutzermenü erreichbar. Beide Wege führen hierher; die Regeln stehen nur an
 * einer Stelle.
 */
export default async function PasswortPage() {
  const texte = await aktiveTexte();

  return (
    <div className="mx-auto w-full max-w-md">
      <Card>
        <CardHeader>
          <CardTitle>{texte.passwort.titel}</CardTitle>
        </CardHeader>
        <CardContent>
          <PasswortBereich />
        </CardContent>
      </Card>
    </div>
  );
}
