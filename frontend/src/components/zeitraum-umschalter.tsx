"use client";

import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group";
import { useTexte } from "@/i18n/provider";
import {
  FREI,
  ROLLUPZEITRAEUME,
  type Baumzeitraum,
  type Rollupzeitraum,
} from "@/lib/rollupzeitraum";

/**
 * Die drei Zeiträume der Rollup-Ansichten: 48 Stunden, 30 Tage, 12 Monate —
 * und auf Wunsch ein vierter Knopf „Frei".
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
 * ## Der vierte Knopf ist freiwillig *(07.09.2026, Schritt 10c‑4b)*
 *
 * Die Prozessansicht kennt seither ein **freies Zeitfenster**; das Dashboard
 * nicht. Der Knopf erscheint nur, wenn der Aufrufer `aufFrei` übergibt — **ohne
 * die Angabe sind es drei Knöpfe**, und das Dashboard ruft ihn ohne. Die
 * Datumsfelder des freien Modus stehen **neben** dem Umschalter, nicht darin:
 * Nur so bleibt die Verwendung im Dashboard zeichengleich, und der Umschalter
 * trägt keinen Zustand, den nur eine seiner Verwendungen kennt.
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
  aufFrei,
  gesperrt = false,
}: {
  /**
   * Das Paar, das gilt — aus der URL oder aus der Antwort. `null`, solange keins
   * feststeht. `FREI` nur dort, wo es den vierten Knopf gibt.
   */
  gewaehlt: Baumzeitraum | null;
  aufAuswahl: (zeitraum: Rollupzeitraum) => void;
  /**
   * **Freiwillig.** Ist die Angabe da, gibt es den vierten Knopf, und ein Klick
   * darauf ruft sie — ohne einen Zeitpunkt, denn der freie Modus beginnt leer.
   */
  aufFrei?: () => void;
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
        if (wert === FREI) {
          aufFrei?.();
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
      {aufFrei === undefined ? null : (
        <ToggleGroupItem value={FREI} disabled={gesperrt} className="min-h-bedienelement px-2.5">
          {texte.zeitraum[FREI]}
        </ToggleGroupItem>
      )}
    </ToggleGroup>
  );
}
