"use client";

import type { ReactNode } from "react";
import Link from "next/link";
import { Building2, ChevronsUpDown } from "lucide-react";

import type { Mandant } from "@/features/sitzung/api";
import { useMandanten } from "@/features/sitzung/hooks";
import { useTexte } from "@/i18n/provider";
import { ROUTEN } from "@/lib/routen";
import { cn } from "@/lib/utils";

/**
 * Der aktive Mandant — **auf jeder Bildschirmbreite sichtbar**.
 *
 * Er wandert am Handy nicht ins Hamburger-Menü. Der Grund steht in
 * `docs/mandantentrennung.md`: Der aktive Mandant steht in der Sitzung und nicht
 * in der URL. Ohne ihn in der Kopfzeile zeigte eine Ansicht — gerade bei ADMIN,
 * der zwischen allen Mandanten wechseln kann — unbemerkt einen anderen
 * Ausschnitt.
 *
 * Bei wenig Platz wird der Anzeigename ausgeblendet, **nie** der Code.
 *
 * **Anzeige oder Bedienelement — nie beides zugleich.** Wer genau einen
 * Mandanten hat, kann nichts wechseln; ihm eine Schaltfläche hinzustellen wäre
 * ein Klickversprechen, das ins Leere führt. Wer mehrere hat, muss die
 * Umschaltmöglichkeit erkennen, ohne sie zu suchen. Deshalb entscheidet die
 * tatsächlich zulässige Menge über die Gestalt, nicht die Rolle.
 */
export function MandantAnzeige({
  mandant,
  wechselErlaubt,
  className,
}: {
  mandant: Mandant | null;
  wechselErlaubt: boolean;
  className?: string;
}) {
  // Zwei getrennte Komponenten statt eines Schalters: Nur so darf die untere
  // die Mandantenliste abfragen, ohne das bei gesperrtem Wechsel — also während
  // des Passwort-Änderungszwangs — ebenfalls zu tun. Dort lehnte das Backend
  // jeden anderen Endpunkt ohnehin ab.
  return wechselErlaubt ? (
    <MandantUmschalter mandant={mandant} className={className} />
  ) : (
    <MandantNurAnzeige mandant={mandant} className={className} />
  );
}

/** Der Inhalt ist in beiden Gestalten derselbe — nur die Hülle unterscheidet sich. */
function Inhalt({ mandant }: { mandant: Mandant | null }): ReactNode {
  const texte = useTexte();
  return (
    <>
      <Building2 aria-hidden="true" className="size-4 shrink-0 opacity-70" />
      <span className="sr-only">{texte.kopfzeile.mandantBezeichnung}: </span>
      {mandant === null ? (
        <span className="text-muted-foreground truncate">{texte.kopfzeile.keinMandant}</span>
      ) : (
        <>
          <span className="font-mono font-medium">{mandant.id}</span>
          {mandant.name !== mandant.id ? (
            <span className="text-muted-foreground hidden truncate lg:inline">{mandant.name}</span>
          ) : null}
        </>
      )}
    </>
  );
}

/**
 * Beide Gestalten teilen sich diese Grundform.
 *
 * Sie ist von Hand gesetzt und nicht die Schaltfläche aus `components/ui`, weil
 * genau **ein** Element hier zwischen Anzeige und Bedienelement wechselt: Ist
 * die Grundform dieselbe, unterscheiden sich die beiden Zustände ausschließlich
 * um das, was sie unterscheiden soll — Auswahlpfeil, Hover, Fokus. Über zwei
 * verschiedene Bausteine wäre das ein Zufall, kein Ergebnis.
 */
const GRUNDFORM =
  "min-h-bedienelement border-border bg-card text-beiwerk flex min-w-0 items-center gap-2 rounded-md border px-2.5";

function MandantNurAnzeige({
  mandant,
  className,
}: {
  mandant: Mandant | null;
  className?: string;
}) {
  return (
    <span className={cn(GRUNDFORM, className)}>
      <Inhalt mandant={mandant} />
    </span>
  );
}

/**
 * Wechselbar heißt: Es gibt etwas zu wechseln. Solange die Liste noch lädt oder
 * genau einen Eintrag hat, bleibt es eine reine Anzeige — lieber ein Bedienelement,
 * das einen Moment später erscheint, als eines, das wieder verschwindet.
 *
 * Die Abfrage kostet nichts Zusätzliches: Es ist derselbe Schlüssel, den die
 * Mandantenauswahl verwendet.
 */
function MandantUmschalter({
  mandant,
  className,
}: {
  mandant: Mandant | null;
  className?: string;
}) {
  const texte = useTexte();
  const { data: mandanten } = useMandanten();

  if ((mandanten?.length ?? 0) <= 1) {
    return <MandantNurAnzeige mandant={mandant} className={className} />;
  }

  return (
    <Link
      href={ROUTEN.mandantenauswahl}
      title={texte.kopfzeile.mandantWechseln}
      className={cn(
        GRUNDFORM,
        // Erst diese drei Zustände machen aus der Anzeige ein Bedienelement.
        "hover:bg-muted focus-visible:ring-ring focus-visible:ring-2 focus-visible:outline-none",
        className,
      )}
    >
      <Inhalt mandant={mandant} />
      {/* Der Auswahlpfeil ist das eigentliche Versprechen: Hier lässt sich etwas
          umschalten. Ohne ihn liest sich das Feld wie eine Anzeige. */}
      <ChevronsUpDown aria-hidden="true" className="ml-auto size-3.5 shrink-0 opacity-60" />
      <span className="sr-only">— {texte.kopfzeile.mandantWechseln}</span>
    </Link>
  );
}
