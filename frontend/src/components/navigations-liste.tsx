"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

import { useTexte } from "@/i18n/provider";
import { istAktiv, sichtbareNavigation } from "@/lib/navigation";
import { cn } from "@/lib/utils";

/**
 * Die Navigation wird aus `lib/navigation.ts` **erzeugt**, nicht geschrieben.
 * Ein neuer Eintrag ab Schritt 4 ist eine Zeile dort und keine Änderung hier.
 *
 * Rollenabhängige Einträge sind reine Bequemlichkeit — die Begründung steht an
 * der Datenstruktur, damit sie nicht in einer Komponente untergeht.
 *
 * Die Zeilenhöhe kommt aus `--dichte-navzeile` und ist am Zeigergerät bewusst
 * knapp: Wer dieses Werkzeug öffnet, sucht Übersicht, nicht Atmosphäre. Am
 * Berührungsgerät fällt derselbe Wert auf die Mindestfläche von 44 Pixel
 * zurück — die Umschaltung steht in `globals.css`, nicht hier.
 */
export function NavigationsListe({
  rolle,
  aufAuswahl,
  className,
}: {
  rolle: string | undefined;
  /** Schließt die Schublade, nachdem am Handy etwas ausgewählt wurde. */
  aufAuswahl?: () => void;
  className?: string;
}) {
  const texte = useTexte();
  const pfad = usePathname();
  const eintraege = sichtbareNavigation(rolle);

  return (
    <nav aria-label={texte.navigation.bezeichnung} className={className}>
      <ul className="flex flex-col gap-0.5">
        {eintraege.map((eintrag) => {
          const aktiv = istAktiv(eintrag, pfad);
          const Symbol = eintrag.symbol;
          return (
            <li key={eintrag.pfad}>
              <Link
                href={eintrag.pfad}
                onClick={aufAuswahl}
                aria-current={aktiv ? "page" : undefined}
                className={cn(
                  "min-h-navzeile focus-visible:ring-ring flex items-center gap-2.5 rounded-md px-2.5",
                  "focus-visible:ring-2 focus-visible:outline-none",
                  aktiv
                    ? "bg-accent text-accent-foreground font-medium"
                    : "text-muted-foreground hover:bg-muted hover:text-foreground",
                )}
              >
                <Symbol aria-hidden="true" className="size-4 shrink-0" />
                <span className="truncate">{texte.navigation.eintraege[eintrag.schluessel]}</span>
              </Link>
            </li>
          );
        })}
      </ul>
    </nav>
  );
}
