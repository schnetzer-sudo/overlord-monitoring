"use client";

import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group";
import { useTexte } from "@/i18n/provider";

import type { Dashboardzeitraum } from "../api";
import { ZEITRAUM_REIHE } from "../filter";

/**
 * Die drei Zeiträume: 48 Stunden, 30 Tage, 12 Monate.
 *
 * **Hervorgehoben ist, was gilt — nicht, was in der URL steht.** Ohne Klick
 * wählt der Endpunkt selbst und nennt sein Paar in der Antwort; der Umschalter
 * hebt genau dieses hervor. **Zurückgeschrieben wird es nicht** (Entscheidung
 * E‑n): Eine URL, die den Wert trägt, weil er einmal gerendert wurde, behauptet
 * eine Wahl, die niemand getroffen hat — und beim Empfänger des Links könnte der
 * Endpunkt ein anderes Paar nehmen, denn er wählt nach dem **Mandanten**.
 *
 * **Solange keine Antwort da ist, ist keine Schaltfläche gedrückt.** Eine
 * vorgemerkte wäre eine Vermutung, die beim Eintreffen der Antwort springt.
 *
 * **Ein Klick auf die gedrückte Schaltfläche tut nichts.** `ToggleGroup` mit
 * `type="single"` meldet dafür den leeren Wert; hier ist das kein Zustand, den
 * es gibt — es gibt immer genau einen Zeitraum. Ihn abzuwählen sähe nach
 * „zurück zur Vorgabe" aus und ließe die Hervorhebung trotzdem stehen, weil die
 * Antwort weiterhin ein Paar nennt.
 *
 * **Er bleibt auch im Leerzustand bedienbar** (Entscheidung E‑p). Ein Mandant
 * ohne Daten im Fenster darf durchschalten; genau das ist der Weg, auf dem er
 * herausfindet, ob es an seinem Fenster liegt.
 */
export function ZeitraumUmschalter({
  gewaehlt,
  aufAuswahl,
  gesperrt = false,
}: {
  /** Das Paar, das gilt — aus der URL oder aus der Antwort. `null`, solange keins feststeht. */
  gewaehlt: Dashboardzeitraum | null;
  aufAuswahl: (zeitraum: Dashboardzeitraum) => void;
  gesperrt?: boolean;
}) {
  const texte = useTexte();

  return (
    <ToggleGroup
      type="single"
      variant="outline"
      aria-label={texte.dashboard.zeitraum.bezeichnung}
      value={gewaehlt ?? ""}
      onValueChange={(wert) => {
        if (wert === "") {
          return;
        }
        aufAuswahl(wert as Dashboardzeitraum);
      }}
    >
      {ZEITRAUM_REIHE.map((zeitraum) => (
        <ToggleGroupItem
          key={zeitraum}
          value={zeitraum}
          disabled={gesperrt}
          className="min-h-bedienelement px-2.5"
        >
          {texte.dashboard.zeitraum[zeitraum]}
        </ToggleGroupItem>
      ))}
    </ToggleGroup>
  );
}
