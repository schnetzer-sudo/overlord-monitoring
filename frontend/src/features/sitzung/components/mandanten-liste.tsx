"use client";

import { Check } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Fehler, Laden, Leer } from "@/components/zustand";
import { useTexte } from "@/i18n/provider";
import { cn } from "@/lib/utils";

import { useMandanten, useMandantWechseln, useSelbstauskunft } from "../hooks";

/**
 * Die wählbaren Mandanten.
 *
 * Die Liste kommt aus `GET /api/mandanten` und ist **genau** die Menge der
 * Berechtigung — für ADMIN alle, für MANDANT die eigenen. Das Frontend filtert
 * nichts nach und kennt keine Regel darüber, wer was sehen darf.
 *
 * Beim Wechsel wird der Zwischenspeicher **geleert**, nicht invalidiert. Sonst
 * stünden nach dem Umschalten für einen Moment die Daten des vorherigen
 * Mandanten auf dem Bildschirm — bei einem Werkzeug, dessen Kernversprechen die
 * Trennung ist, der peinlichste denkbare Fehler.
 */
export function MandantenListe() {
  const texte = useTexte();
  const { data: auskunft } = useSelbstauskunft();
  const { data: mandanten, error, isPending, refetch } = useMandanten();
  const wechseln = useMandantWechseln();

  if (isPending) {
    return <Laden zeilen={3} />;
  }

  if (error) {
    return <Fehler fehler={error} aufWiederholen={() => void refetch()} />;
  }

  if (mandanten.length === 0) {
    return (
      <Leer titel={texte.mandantenauswahl.leerTitel} hinweis={texte.mandantenauswahl.leerHinweis} />
    );
  }

  const aktiveId = auskunft?.mandant?.id;

  return (
    <>
      {wechseln.error ? <Fehler fehler={wechseln.error} /> : null}
      <ul className="mt-4 flex flex-col gap-2">
        {mandanten.map((mandant) => {
          const aktiv = mandant.id === aktiveId;
          const laeuft = wechseln.isPending && wechseln.variables === mandant.id;
          return (
            <li key={mandant.id}>
              <Button
                type="button"
                variant="outline"
                disabled={wechseln.isPending}
                onClick={() => wechseln.mutate(mandant.id)}
                className={cn(
                  "min-h-beruehrung h-auto w-full justify-start gap-3 px-3 py-2 text-left",
                  aktiv && "border-ring",
                )}
              >
                <span className="font-mono font-medium">{mandant.id}</span>
                <span className="text-muted-foreground min-w-0 flex-1 truncate">
                  {mandant.name}
                </span>
                {laeuft ? (
                  <span className="text-beiwerk">{texte.mandantenauswahl.laeuft}</span>
                ) : aktiv ? (
                  <span className="text-beiwerk flex items-center gap-1">
                    <Check aria-hidden="true" className="size-4" />
                    {texte.mandantenauswahl.aktiv}
                  </span>
                ) : (
                  <span className="text-beiwerk sr-only sm:not-sr-only">
                    {texte.mandantenauswahl.waehlen}
                  </span>
                )}
              </Button>
            </li>
          );
        })}
      </ul>
    </>
  );
}
