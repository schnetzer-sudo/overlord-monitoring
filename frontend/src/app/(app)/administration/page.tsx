import type { Metadata } from "next";
import Link from "next/link";

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
 *
 * ## Warum hier kein `Button asChild` steht
 *
 * **`components/ui/button.tsx` ist in diesem Projekt client-only, ohne es zu
 * sagen.** Es trägt kein `"use client"`, importiert aber `Slot` aus dem
 * Sammelpaket `radix-ui` — und dessen Auswertung ruft `createContext`. In einer
 * Server-Komponente ergibt das einen `TypeError` beim Modulauswerten, und zwar
 * schon beim Importieren, nicht erst beim Rendern. Alle übrigen dreiundzwanzig
 * Verwender im Projekt sind Client-Komponenten; das fällt deshalb sonst nie auf.
 *
 * **Aufgelöst wird das nicht mit `"use client"` an dieser Datei.** Jede
 * `page.tsx` ist Server-Komponente (`docs/frontend-grundlagen.md` §8), und eine
 * Liste ohne jedes Verhalten ist der schlechteste denkbare Anlass, diese Grenze
 * zu verschieben.
 *
 * **Und es ist ohnehin eine Liste von Verweisen und keine von Schaltflächen.**
 * Sie trägt deshalb die Gestalt, die `components/zustand.tsx` für seine Tafeln
 * benutzt — gerahmte Karte auf `--card` —, dazu den Fokusring der Navigation.
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
              <Link
                href={bereich.pfad}
                className="border-border bg-card hover:bg-muted focus-visible:ring-ring min-h-beruehrung flex items-start gap-3 rounded-lg border px-3 py-2 focus-visible:ring-2 focus-visible:outline-none"
              >
                <Symbol aria-hidden="true" className="mt-0.5 size-4 shrink-0" />
                <span className="flex min-w-0 flex-1 flex-col gap-0.5">
                  <span className="font-medium">{text.titel}</span>
                  <span className="text-muted-foreground text-beiwerk">{text.beschreibung}</span>
                </span>
              </Link>
            </li>
          );
        })}
      </ul>
    </div>
  );
}
