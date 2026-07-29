import { describe, expect, it } from "vitest";

import { nachAbmeldung, nachMandantenwechsel } from "@/lib/zwischenspeicher";

/**
 * Der Nachweis zu Abnahmepunkt 5: Ein Mandantenwechsel leert den
 * Zwischenspeicher — **und zwar bevor** irgendetwas anderes passiert.
 *
 * Die Reihenfolge ist der Punkt. Würde erst navigiert und dann geleert, zeigte
 * die neue Ansicht für einen Moment die Daten des vorherigen Mandanten.
 */
function protokoll() {
  const schritte: string[] = [];
  return {
    schritte,
    speicher: { clear: () => schritte.push("geleert") },
    weiter: () => schritte.push("weiter"),
  };
}

describe("Zwischenspeicher", () => {
  it("wird beim Mandantenwechsel geleert, bevor es weitergeht", () => {
    const { schritte, speicher, weiter } = protokoll();
    nachMandantenwechsel(speicher, weiter);
    expect(schritte).toEqual(["geleert", "weiter"]);
  });

  it("wird beim Abmelden geleert, bevor es weitergeht", () => {
    const { schritte, speicher, weiter } = protokoll();
    nachAbmeldung(speicher, weiter);
    expect(schritte).toEqual(["geleert", "weiter"]);
  });
});
