"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

import { useTexte } from "@/i18n/provider";
import { ADMINISTRATION, istAktiverBereich } from "@/lib/navigation";
import { cn } from "@/lib/utils";

/**
 * Die Unternavigation des Administrationsbereichs — **zwei Links, kein zweiter
 * Menüpunkt** (`docs/frontend-grundlagen.md` §2).
 *
 * Sie steht im Layout des Bereichs und damit auf allen drei Seiten. Der
 * Hauptnavigation gehört weiterhin genau ein Eintrag; wer hier ist, ist an ihm
 * bereits vorbeigekommen und will zwischen den Bereichen wechseln, nicht sie
 * suchen.
 *
 * **Dieselbe Gestalt wie die Hauptnavigation, nur waagerecht** — aktive Zeile
 * auf der Akzentfläche mit dunkler Schrift, alles andere gedämpft
 * (`docs/visuelles-konzept.md` §3). Ein eigenes Vokabular für „Reiter" gibt es
 * bewusst nicht: Es ist dieselbe Sache an einem anderen Ort.
 *
 * Die Einträge kommen wie die der Hauptnavigation aus `lib/navigation.ts`.
 */
export function BereichsNavigation() {
  const texte = useTexte();
  const pfad = usePathname();

  return (
    <nav aria-label={texte.administration.bereichsnavigation}>
      <ul className="flex flex-wrap gap-1">
        {ADMINISTRATION.map((bereich) => {
          const aktiv = istAktiverBereich(bereich, pfad);
          const Symbol = bereich.symbol;
          return (
            <li key={bereich.pfad}>
              <Link
                href={bereich.pfad}
                aria-current={aktiv ? "page" : undefined}
                className={cn(
                  "min-h-bedienelement focus-visible:ring-ring flex items-center gap-2 rounded-md px-2.5",
                  "focus-visible:ring-2 focus-visible:outline-none",
                  aktiv
                    ? "bg-accent text-accent-foreground font-medium"
                    : "text-muted-foreground hover:bg-muted hover:text-foreground",
                )}
              >
                <Symbol aria-hidden="true" className="size-4 shrink-0" />
                {texte.administration.bereiche[bereich.schluessel].titel}
              </Link>
            </li>
          );
        })}
      </ul>
    </nav>
  );
}
