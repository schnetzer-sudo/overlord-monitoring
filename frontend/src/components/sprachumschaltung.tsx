"use client";

import { spracheSetzen } from "@/i18n/aktion";
import { SPRACHEN, type Sprache } from "@/i18n";
import { useSprache, useTexte } from "@/i18n/provider";
import { cn } from "@/lib/utils";

/**
 * Sprachumschaltung — zwei Schaltflächen, ein Formular.
 *
 * Kein Sprachpräfix in der URL: Die Sprache ist eine Eigenschaft des Nutzers,
 * nicht der Ansicht. Ein geteilter Link erscheint beim Empfänger in dessen
 * Sprache.
 *
 * Als Formular mit Server-Aktion statt als `onClick`: Das Wurzel-Layout liest
 * die Sprache serverseitig aus dem Cookie, also muss der neue Wert dort ankommen
 * — und nebenbei funktioniert die Umschaltung damit auch ohne JavaScript.
 */
export function Sprachumschaltung({ className }: { className?: string }) {
  const aktiv = useSprache();
  const texte = useTexte();

  const beschriftung: Record<Sprache, { lang: string; kurz: string }> = {
    de: { lang: texte.kopfzeile.spracheDeutsch, kurz: texte.kopfzeile.spracheDeutschKurz },
    en: { lang: texte.kopfzeile.spracheEnglisch, kurz: texte.kopfzeile.spracheEnglischKurz },
  };

  return (
    <form
      action={spracheSetzen}
      aria-label={texte.kopfzeile.spracheBezeichnung}
      className={cn("border-border flex shrink-0 items-center rounded-md border", className)}
    >
      {SPRACHEN.map((sprache) => (
        <button
          key={sprache}
          type="submit"
          name="sprache"
          value={sprache}
          aria-pressed={sprache === aktiv}
          title={beschriftung[sprache].lang}
          className={cn(
            "min-h-bedienelement text-beiwerk focus-visible:ring-ring rounded-[inherit] px-2.5 font-medium",
            "focus-visible:ring-2 focus-visible:outline-none",
            sprache === aktiv
              ? "bg-accent text-accent-foreground"
              : "text-muted-foreground hover:text-foreground",
          )}
        >
          <span aria-hidden="true">{beschriftung[sprache].kurz}</span>
          <span className="sr-only">{beschriftung[sprache].lang}</span>
        </button>
      ))}
    </form>
  );
}
