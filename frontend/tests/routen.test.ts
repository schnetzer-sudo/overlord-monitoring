import { describe, expect, it } from "vitest";

import { ROUTEN, sicheresZiel } from "@/lib/routen";

/**
 * Der `weiter`-Parameter bringt den Nutzer nach der Anmeldung dorthin zurück,
 * wo er hinwollte. Ungeprüft wäre er eine offene Weiterleitung: Ein Link auf
 * `/anmeldung?weiter=https://…` führte nach erfolgreicher Anmeldung auf eine
 * fremde Seite — mit dem Vertrauen, das der Nutzer gerade dieser Anwendung
 * entgegengebracht hat.
 */
describe("weiter-Ziel", () => {
  it("nimmt einen Pfad innerhalb der Anwendung", () => {
    expect(sicheresZiel("/nachrichten")).toBe("/nachrichten");
    expect(sicheresZiel("/nachrichten?von=2026-07-08T00:00:00Z")).toBe(
      "/nachrichten?von=2026-07-08T00:00:00Z",
    );
  });

  it("weist alles zurück, was nach draußen führen könnte", () => {
    for (const ziel of [
      "https://beispiel.invalid/",
      "//beispiel.invalid/",
      "/\\beispiel.invalid",
      "/pfad\\mit\\backslash",
      "javascript:alert(1)",
      "",
      null,
      undefined,
    ]) {
      expect(sicheresZiel(ziel), `Ziel: ${String(ziel)}`).toBe(ROUTEN.startseite);
    }
  });

  it("führt nicht auf die Anmeldung zurück", () => {
    expect(sicheresZiel(ROUTEN.anmeldung)).toBe(ROUTEN.startseite);
    expect(sicheresZiel("/anmeldung?weiter=/")).toBe(ROUTEN.startseite);
  });
});
