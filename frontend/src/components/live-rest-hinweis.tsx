"use client";

import { Info } from "lucide-react";

import { Alert, AlertDescription } from "@/components/ui/alert";
import { useAnzeigezone } from "@/components/zeitzone";
import { einsetzen } from "@/i18n";
import { useSprache, useTexte } from "@/i18n/provider";
import { formatiereZeitpunkt } from "@/lib/format";
import type { LiveRest } from "@/lib/live-rest";

/**
 * **Der Hinweis zum Live-Rest** (`docs/live-rest.md` §10) — **ein Baustein für
 * beide Ansichten** (E‑192): der Prozessbaum zeigt ihn bei seinen Kopfzahlen, die
 * Übersicht über den Kacheln. Zwei Stellen, die denselben Satz aus derselben
 * Antwort bilden, driften; deshalb steht er hier und nicht je Feature.
 *
 * **Nur bei `AUSGESETZT` steht etwas**: Dann fehlt in den Zahlen der Verkehr seit
 * dem letzten Rollup-Lauf, und das gehört dorthin, wo die Zahlen stehen, nicht in
 * eine Fehlermeldung. Bei `ANGEWANDT` und `NICHT_NOETIG` rendert der Baustein
 * **nichts** — ein Hinweis, der immer da ist, wird nicht mehr gelesen. Das ist
 * die Aussage, die `tests/live-rest.test.tsx` in beiden Ansichten festhält.
 *
 * Die Bauform ist die des Katalog-Hinweises (`katalog-kennzahlen.tsx`): `Alert`
 * ohne Variante, also ohne Rot, ohne neues Farbtoken und ohne neues Dichtemaß.
 * Mit Lauf trägt der Satz **G** absolut in der Anzeigezone, wie der Stand der
 * Übersicht (`docs/dashboard-frontend.md` §5.7); ohne Lauf gibt es nichts zu
 * beziffern.
 */
export function LiveRestHinweis({ liveRest }: { liveRest: LiveRest }) {
  const texte = useTexte();
  const sprache = useSprache();
  const zone = useAnzeigezone();

  if (liveRest.zustand !== "AUSGESETZT") {
    return null;
  }

  return (
    <Alert>
      <Info aria-hidden="true" />
      <AlertDescription>
        {liveRest.vollstaendigBis === null
          ? texte.liveRest.ausgesetztOhneLauf
          : einsetzen(texte.liveRest.ausgesetztMitLauf, {
              vollstaendigBis: formatiereZeitpunkt(liveRest.vollstaendigBis, sprache, zone),
            })}
      </AlertDescription>
    </Alert>
  );
}
