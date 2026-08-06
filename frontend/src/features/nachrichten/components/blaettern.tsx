"use client";

import { useId } from "react";
import { ChevronLeft, ChevronRight, RefreshCw } from "lucide-react";

import { useAnzeigezone } from "@/components/zeitzone";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { einsetzen } from "@/i18n";
import { useSprache, useTexte } from "@/i18n/provider";
import { formatiereZeitpunktGenau } from "@/lib/format";
import { cn } from "@/lib/utils";

/**
 * Blättern und Aktualisieren.
 *
 * ## Keine Seitenzahlen
 *
 * Es gibt keine Gesamtzahl — ein `COUNT` über `Message` wäre genau die
 * Live-Aggregation, die Regel L2 verbietet, und über ein Jahresfenster kostete er
 * mehr als die Seite selbst. Eine erfundene Zahl wäre schlimmer als keine, also
 * gibt es „vorwärts" und „rückwärts" und sonst nichts.
 *
 * ## Die automatische Aktualisierung
 *
 * **Standardmäßig aus.** Das Werkzeug wird geöffnet, wenn etwas nicht stimmt; wer
 * es aufmacht, ist schon angespannt. Eine Liste, die unter den Händen springt,
 * macht die Lage schlechter.
 *
 * **Sie pausiert beim Blättern** — sonst zeigte die Ansicht plötzlich einen
 * anderen Ausschnitt, oder der Cursor läge außerhalb des Fensters und die Antwort
 * wäre `400`. Dass sie pausiert, steht daneben; ein Schalter, der an ist und
 * nichts tut, ist schlimmer als einer, der aus ist.
 *
 * **Der Zeitpunkt der letzten Aktualisierung ist immer sichtbar**, auch bei
 * manueller Bedienung. Ohne ihn weiß niemand, ob er auf Daten von vor drei
 * Sekunden oder von vor drei Stunden schaut — und genau das ist die Frage, wegen
 * der jemand dieses Werkzeug öffnet.
 */
export function Blaettern({
  kannZurueck,
  kannVor,
  aufZurueck,
  aufVor,
  aufSeiteEins,
  standVon,
  laeuft,
  aktualisierungAn,
  aufAktualisierung,
  aufAktualisieren,
}: {
  kannZurueck: boolean;
  kannVor: boolean;
  aufZurueck: () => void;
  aufVor: () => void;
  aufSeiteEins: boolean;
  /** Zeitstempel der letzten erfolgreichen Antwort, `0` solange es keine gibt. */
  standVon: number;
  laeuft: boolean;
  aktualisierungAn: boolean;
  aufAktualisierung: (an: boolean) => void;
  aufAktualisieren: () => void;
}) {
  const texte = useTexte();
  const sprache = useSprache();
  const zone = useAnzeigezone();
  const schalterId = useId();

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
        {aktualisierungAn && !aufSeiteEins ? (
          <span>· {texte.nachrichten.aktualisierung.pausiertGeblaettert}</span>
        ) : null}
      </div>

      <div className="flex flex-wrap items-center gap-2">
        <div className="flex items-center gap-2">
          <Switch id={schalterId} checked={aktualisierungAn} onCheckedChange={aufAktualisierung} />
          <Label htmlFor={schalterId} className="text-beiwerk font-normal">
            {texte.nachrichten.aktualisierung.schalter}
          </Label>
        </div>

        <Button
          type="button"
          variant="outline"
          size="icon"
          className="min-h-bedienelement"
          onClick={aufAktualisieren}
          title={texte.nachrichten.aktualisierung.jetztAktualisieren}
        >
          {/* Kein Drehen: Bewegung zieht Aufmerksamkeit, und die gehört den
              Daten (visuelles Konzept §7). Dass etwas läuft, steht links als
              Text. */}
          <RefreshCw aria-hidden="true" className={cn(laeuft && "opacity-50")} />
          <span className="sr-only">{texte.nachrichten.aktualisierung.jetztAktualisieren}</span>
        </Button>

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
    </div>
  );
}
