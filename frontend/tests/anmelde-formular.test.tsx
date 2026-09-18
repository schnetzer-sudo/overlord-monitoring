// @vitest-environment jsdom

import { describe, expect, it, vi } from "vitest";

import { AnmeldeFormular } from "@/features/sitzung/components/anmelde-formular";
import { texteFuer } from "@/i18n";

import { rendere } from "./hilfe/rendern";

/**
 * **Der Fokus beim Laden der Anmeldeseite** (`docs/frontend-grundlagen.md` §3,
 * E‑215) — ein Fall, den nur ein gerenderter Baum trägt: Wo der Fokus steht,
 * ist ein Zustand des Dokuments (`document.activeElement`), und gesetzt wird er
 * von React beim Einhängen, nicht von einer Funktion dieses Projekts.
 *
 * **Geprüft ist der clientseitige Weg** — die Umleitung aus einer geschützten
 * Route, bei der React das Formular einhängt und den Fokus setzt. Beim direkten
 * Aufruf wertet der Browser das serverseitig gerenderte Attribut `autofocus`
 * aus; dafür müsste eine Seite laden, und das kann `jsdom` nicht. Diesen Weg
 * prüft die Sichtprüfung im Browser.
 *
 * **Ohne `toHaveFocus()`:** Das Projekt rendert ohne Testing Library
 * (`vitest.config.mts`). Verglichen wird deshalb `document.activeElement` mit
 * dem Feld, und genau das prüfte dieser Matcher. Gefunden wird das Feld über
 * seinen zugänglichen Namen: Den gibt ihm die Beschriftung (`<label for>`),
 * also führt der Weg über sie und `label.control` und nicht über die `id`.
 *
 * **Alle Prüfwerte sind erfunden**; es gibt keine Anfrage.
 */

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: () => {}, replace: () => {} }),
  useSearchParams: () => new URLSearchParams(),
}));

const TEXTE = texteFuer("de");

/** Das Eingabefeld, das eine Beschriftung mit genau diesem Text benennt. */
function feldMitName(behaelter: HTMLElement, name: string): HTMLInputElement {
  const beschriftung = [...behaelter.querySelectorAll("label")].find(
    (kandidat) => kandidat.textContent === name,
  );
  expect(beschriftung, `Beschriftung ${name}`).toBeDefined();
  const feld = beschriftung?.control;
  expect(feld, `Feld zur Beschriftung ${name}`).toBeInstanceOf(HTMLInputElement);
  return feld as HTMLInputElement;
}

describe("Das Anmeldeformular", () => {
  it("setzt beim Einhängen den Fokus in das Feld Benutzername", async () => {
    const gerendert = await rendere(<AnmeldeFormular />);
    try {
      const feld = feldMitName(gerendert.behaelter, TEXTE.anmeldung.benutzername);
      expect(document.activeElement).toBe(feld);
    } finally {
      await gerendert.abbauen();
    }
  });
});
