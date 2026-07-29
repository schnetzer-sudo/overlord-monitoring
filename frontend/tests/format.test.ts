import { describe, expect, it } from "vitest";

import { formatiereDatum, formatiereZahl, formatiereZeitpunkt } from "@/lib/format";

/**
 * Der Zeitstempel darf sich **nicht** verschieben.
 *
 * Die Werte aus `GlassfishDB` sind Wanduhrzeit des Altsystem-Servers. Würde man
 * sie durch eine Zeitzonenumrechnung schicken, stünde ein um 14:23 verarbeiteter
 * Beleg je nach Standort des Browsers um 16:23 in der Liste — plausibel genug,
 * dass es niemand merkt, und falsch genug, dass die Suche nach dem Zeitpunkt aus
 * dem Altwerkzeug ins Leere geht.
 */
describe("Zeitpunkte", () => {
  it("zeigt die gelieferten Felder unverändert an", () => {
    expect(formatiereZeitpunkt("2026-07-08T14:23:00Z", "de")).toBe("08.07.2026, 14:23");
  });

  it("verschiebt sich auch dann nicht, wenn ein Zeitzonenversatz mitgeliefert wird", () => {
    // Der Versatz wird bewusst ignoriert: Die Quelle führt keine Zeitzone, und
    // was das Backend anhängt, ändert nichts an der abgelesenen Wanduhrzeit.
    expect(formatiereZeitpunkt("2026-07-08T14:23:00+02:00", "de")).toBe("08.07.2026, 14:23");
    expect(formatiereZeitpunkt("2026-07-08T14:23:00", "de")).toBe("08.07.2026, 14:23");
    expect(formatiereZeitpunkt("2026-07-08 14:23:00.123", "de")).toBe("08.07.2026, 14:23");
  });

  it("formatiert in der aktiven Sprache", () => {
    expect(formatiereZeitpunkt("2026-07-08T14:23:00Z", "en")).toContain("2026");
    expect(formatiereZeitpunkt("2026-07-08T14:23:00Z", "en")).not.toBe(
      formatiereZeitpunkt("2026-07-08T14:23:00Z", "de"),
    );
    expect(formatiereDatum("2026-07-08T14:23:00Z", "de")).toBe("08.07.2026");
  });

  it("reicht einen unlesbaren Wert unverändert durch — lieber roh als falsch", () => {
    expect(formatiereZeitpunkt("kein Zeitpunkt", "de")).toBe("kein Zeitpunkt");
    expect(formatiereZeitpunkt(null, "de")).toBe("");
  });
});

describe("Zahlen", () => {
  it("werden in der aktiven Sprache getrennt", () => {
    expect(formatiereZahl(1234567, "de")).toBe("1.234.567");
    expect(formatiereZahl(1234567, "en")).toBe("1,234,567");
  });
});
