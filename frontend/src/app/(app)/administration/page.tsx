import type { Metadata } from "next";
import Link from "next/link";

import { Button } from "@/components/ui/button";
import { aktiveTexte } from "@/i18n/server";
import { ADMINISTRATION } from "@/lib/navigation";

export async function generateMetadata(): Promise<Metadata> {
  const texte = await aktiveTexte();
  return { title: texte.navigation.eintraege.administration };
}

/**
 * Die Übersicht des Administrationsbereichs — **ein Navigationseintrag, zwei
 * Unterseiten** (`docs/frontend-grundlagen.md` §2).
 *
 * Sie wiederholt die Unternavigation nicht, sondern ergänzt sie: Dort stehen die
 * beiden Namen, hier steht dazu, wofür jeder Bereich da ist. Wer „Administration"
 * anklickt, weiß danach, wohin er will.
 *
 * **Sie prüft die Rolle nicht.** Ein ausgeblendeter Menüpunkt ist Bequemlichkeit,
 * eine Prüfung im Browser wäre eine Berechtigungsentscheidung am falschen Ort —
 * dieselbe Grenze, die `src/proxy.ts` mit einem eigenen Absatz zieht. Diese
 * Seite ruft kein Backend auf und kann deshalb auch nichts erfahren; wer sie
 * ohne die Rolle `ADMIN` erreicht, sieht zwei Verweise und bekommt den Zustand
 * „kein Zugriff" dort, wo er belegt ist: an der Katalogpflege, aus deren `403`.
 */
export default async function AdministrationPage() {
  const texte = await aktiveTexte();

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-col gap-1">
        <h1 className="text-ueberschrift font-semibold">
          {texte.navigation.eintraege.administration}
        </h1>
        <p className="text-muted-foreground text-beiwerk">{texte.administration.einleitung}</p>
      </div>

      <ul className="max-w-inhalt flex flex-col gap-2">
        {ADMINISTRATION.map((bereich) => {
          const text = texte.administration.bereiche[bereich.schluessel];
          const Symbol = bereich.symbol;
          return (
            <li key={bereich.pfad}>
              <Button
                asChild
                variant="outline"
                className="min-h-beruehrung h-auto w-full justify-start gap-3 px-3 py-2 text-left"
              >
                <Link href={bereich.pfad}>
                  <Symbol aria-hidden="true" className="size-4 shrink-0 self-start" />
                  <span className="flex min-w-0 flex-1 flex-col gap-0.5">
                    <span className="font-medium">{text.titel}</span>
                    <span className="text-muted-foreground text-beiwerk whitespace-normal">
                      {text.beschreibung}
                    </span>
                  </span>
                </Link>
              </Button>
            </li>
          );
        })}
      </ul>
    </div>
  );
}
