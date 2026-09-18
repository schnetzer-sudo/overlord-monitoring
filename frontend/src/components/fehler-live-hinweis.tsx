"use client";

import { Info } from "lucide-react";

import { Alert, AlertDescription } from "@/components/ui/alert";
import { useTexte } from "@/i18n/provider";
import type { FehlerLive } from "@/lib/fehler-live";

/**
 * **Der Hinweis zu Fehler live** (`docs/fehler-live.md` §6) — in Bauform und Ort
 * der Hinweis zum Live-Rest (`components/live-rest-hinweis.tsx`, E‑192): über
 * den Kacheln, auch im Leerzustand; `Alert` ohne Variante, `Info`-Zeichen, kein
 * Rot, kein neues Farbtoken. Stehen beide, steht der Hinweis zum Live-Rest zuerst.
 *
 * **Nur bei `AUSGESETZT` steht etwas**: Dann kommen die Fehler aus der
 * stündlichen Aggregation, und eine nachverarbeitete Nachricht kann darin noch
 * als Fehler zählen — das gehört dorthin, wo die Zahlen stehen, nicht in eine
 * Fehlermeldung. Bei `ANGEWANDT` rendert der Baustein **nichts**; ein Hinweis, der
 * immer da ist, wird nicht mehr gelesen. `tests/fehler-live.test.tsx` hält beides
 * fest.
 */
export function FehlerLiveHinweis({ fehlerLive }: { fehlerLive: FehlerLive }) {
  const texte = useTexte();

  if (fehlerLive.zustand !== "AUSGESETZT") {
    return null;
  }

  return (
    <Alert>
      <Info aria-hidden="true" />
      <AlertDescription>{texte.fehlerLive.ausgesetzt}</AlertDescription>
    </Alert>
  );
}
