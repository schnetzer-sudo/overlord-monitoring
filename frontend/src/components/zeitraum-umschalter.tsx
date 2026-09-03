"use client";

import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group";
import { useTexte } from "@/i18n/provider";
import { ROLLUPZEITRAEUME, type Rollupzeitraum } from "@/lib/rollupzeitraum";

/**
 * Die drei Zeiträume der Rollup-Ansichten: 48 Stunden, 30 Tage, 12 Monate.
 *
 * ## Warum er in `components/` liegt und nicht mehr im Dashboard
 *
 * *(verschoben am 02.09.2026, Schritt 10c‑2.)* Er entstand in Schritt 10b‑3 für
 * die Landingpage und stand in `features/dashboard/components/`. Die
 * **Prozessansicht** braucht denselben Umschalter — dieselben drei Paare,
 * dieselbe Reihenfolge, dieselbe Beschriftung. **Ein Feature importiert nicht
 * aus einem Nachbarfeature** (`docs/frontend-grundlagen.md` §8); der gemeinsame
 * Teil wandert nach `components/`, nicht ins Nachbarfeature. Genau dieser Weg
 * ist dort seit Schritt 4 vorgezeichnet.
 *
 * **Kein Satz seiner Begründung ist dadurch falsch geworden** — er tut, was er
 * vorher tat, an einer Stelle, die beide Ansichten erreichen.
 *
 * ## Hervorgehoben ist, was gilt — nicht, was in der URL steht
 *
 * Ohne Klick wählt der Endpunkt selbst und nennt sein Paar in der Antwort; der
 * Umschalter hebt genau dieses hervor. **Zurückgeschrieben wird es nicht**
 * (Entscheidung E‑n): Eine URL, die den Wert trägt, weil er einmal gerendert
 * wurde, behauptet eine Wahl, die niemand getroffen hat — und beim Empfänger des
 * Links könnte der Endpunkt ein anderes Paar nehmen, denn das Dashboard wählt
 * nach dem **Mandanten**.
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
  gewaehlt: Rollupzeitraum | null;
  aufAuswahl: (zeitraum: Rollupzeitraum) => void;
  gesperrt?: boolean;
}) {
  const texte = useTexte();

  return (
    <ToggleGroup
      type="single"
      variant="outline"
      aria-label={texte.zeitraum.bezeichnung}
      value={gewaehlt ?? ""}
      onValueChange={(wert) => {
        if (wert === "") {
          return;
        }
        aufAuswahl(wert as Rollupzeitraum);
      }}
    >
      {ROLLUPZEITRAEUME.map((zeitraum) => (
        <ToggleGroupItem
          key={zeitraum}
          value={zeitraum}
          disabled={gesperrt}
          className="min-h-bedienelement px-2.5"
        >
          {texte.zeitraum[zeitraum]}
        </ToggleGroupItem>
      ))}
    </ToggleGroup>
  );
}
