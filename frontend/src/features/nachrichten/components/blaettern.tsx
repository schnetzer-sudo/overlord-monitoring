"use client";

import { ChevronLeft, ChevronRight } from "lucide-react";

import { useAnzeigezone } from "@/components/zeitzone";
import { Button } from "@/components/ui/button";
import { einsetzen } from "@/i18n";
import { useSprache, useTexte } from "@/i18n/provider";
import { formatiereZeitpunktGenau } from "@/lib/format";

/**
 * Blättern und der Stand der Liste.
 *
 * ## Keine Seitenzahlen
 *
 * Es gibt keine Gesamtzahl — ein `COUNT` über `Message` wäre genau die
 * Live-Aggregation, die Regel L2 verbietet, und über ein Jahresfenster kostete er
 * mehr als die Seite selbst. Eine erfundene Zahl wäre schlimmer als keine, also
 * gibt es „vorwärts" und „rückwärts" und sonst nichts.
 *
 * ## Der Stand bleibt hier — Schalter und Knopf sind nach oben gewandert *(16.09.2026)*
 *
 * **Der Zeitpunkt der letzten Aktualisierung ist immer sichtbar**, auch bei
 * manueller Bedienung. Ohne ihn weiß niemand, ob er auf Daten von vor drei
 * Sekunden oder von vor drei Stunden schaut — und genau das ist die Frage, wegen
 * der jemand dieses Werkzeug öffnet. Er steht weiterhin unter der Liste (E‑171).
 *
 * **Bis zum 16.09.2026 standen hier auch der Schalter der automatischen
 * Aktualisierung, der Hinweis, dass sie beim Blättern pausiert, und ein Knopf
 * „Jetzt aktualisieren".** Der Schalter steht seither samt Pausenanzeige im Kopf
 * der Nachrichtenliste, direkt vor „Neu laden" (`components/neu-laden.tsx`,
 * E‑163, E‑171) — so ist er auch dann erreichbar, wenn die Liste lang ist. Der
 * Knopf ist entfallen (E‑172): Er holte die *aktuelle* Seite neu, auch Seite
 * sieben, und damit genau den Cursor, den „Neu laden" vermeidet (E‑168). Zwei
 * Knöpfe, die dasselbe versprechen und Verschiedenes tun, wären schlechter als
 * einer.
 */
export function Blaettern({
  kannZurueck,
  kannVor,
  aufZurueck,
  aufVor,
  standVon,
  laeuft,
}: {
  kannZurueck: boolean;
  kannVor: boolean;
  aufZurueck: () => void;
  aufVor: () => void;
  /** Zeitstempel der letzten erfolgreichen Antwort, `0` solange es keine gibt. */
  standVon: number;
  laeuft: boolean;
}) {
  const texte = useTexte();
  const sprache = useSprache();
  const zone = useAnzeigezone();

  const stand =
    standVon === 0
      ? texte.nachrichten.aktualisierung.standUnbekannt
      : einsetzen(texte.nachrichten.aktualisierung.stand, {
          zeit: formatiereZeitpunktGenau(new Date(standVon).toISOString(), sprache, zone),
        });

  return (
    <div className="flex flex-wrap items-center justify-between gap-x-4 gap-y-2">
      <div className="text-muted-foreground text-beiwerk flex items-center gap-2">
        <span aria-live="polite">{laeuft ? texte.nachrichten.aktualisierung.laeuft : stand}</span>
      </div>

      <div className="flex items-center gap-1">
        <Button
          type="button"
          variant="outline"
          className="min-h-bedienelement"
          disabled={!kannZurueck}
          onClick={aufZurueck}
        >
          <ChevronLeft aria-hidden="true" />
          <span className="hidden sm:inline">{texte.nachrichten.blaettern.zurueck}</span>
          <span className="sr-only sm:hidden">{texte.nachrichten.blaettern.zurueck}</span>
        </Button>
        <Button
          type="button"
          variant="outline"
          className="min-h-bedienelement"
          disabled={!kannVor}
          onClick={aufVor}
        >
          <span className="hidden sm:inline">{texte.nachrichten.blaettern.vor}</span>
          <span className="sr-only sm:hidden">{texte.nachrichten.blaettern.vor}</span>
          <ChevronRight aria-hidden="true" />
        </Button>
      </div>
    </div>
  );
}
