import { describe, expect, it } from "vitest";

import {
  nachAbmeldung,
  nachMandantenwechsel,
  zielNachMandantenwechsel,
} from "@/lib/zwischenspeicher";

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

/**
 * Ein Teil des Zustands liegt gar nicht im Zwischenspeicher, sondern in der
 * Adresszeile — und der **Prozessfilter** ist der Fall, an dem das weh tut:
 * `ProcessID`s sind mandantengebunden. Bliebe der Filter über den Wechsel hinweg
 * stehen, sähe der Nutzer eine dauerhaft leere Liste, deren Ursache in einem
 * Auswahlfeld steckt, das ihm nichts mehr anzeigen kann.
 */
describe("Das Ziel nach dem Mandantenwechsel", () => {
  it("trägt keine Filter aus der vorherigen Ansicht", () => {
    expect(zielNachMandantenwechsel("/")).toBe("/");
    expect(zielNachMandantenwechsel("/nachrichten?prozess=abc&zeitraum=7d")).toBe("/nachrichten");
    expect(zielNachMandantenwechsel("/nachrichten?prozess=abc#unten")).toBe("/nachrichten");
  });
});
