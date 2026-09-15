// @vitest-environment jsdom

import { act } from "react";
import { describe, expect, it, vi } from "vitest";

import { GliederungUmschalter } from "@/features/nachrichten/components/gliederung-umschalter";
import { texteFuer } from "@/i18n";

import { rendere } from "./hilfe/rendern";

/**
 * **Zwei Fälle, für die ein gerenderter Schalter die einzige Prüfung ist**
 * (`docs/process-view.md` §48, E‑143, E‑146).
 *
 * Seit dem 15.09.2026 ist die Gliederung ein Schalter: aus Partner, an Projekt.
 * Welche gilt, steht als `aria-checked` am Schalter — die Regel dahinter
 * (`hervorgehobeneGliederung`) ist eine reine Funktion und in
 * `tests/prozessansicht.test.ts` geprüft; dass der Schalter sie **zeigt**, steht
 * nur im Baum. Dazu die **Verdrahtung**: Umschalten meldet den **anderen** Code,
 * und die Beschriftung schaltet mit — sie trägt die Berührungsfläche.
 *
 * **Alle Prüfwerte sind erfunden**; es gibt keine Antwort und keine Anfrage.
 */

const T = texteFuer("de").prozesse.gliederung;

function schalter(behaelter: HTMLElement): HTMLButtonElement {
  const gefunden = behaelter.querySelector<HTMLButtonElement>('[role="switch"]');
  if (gefunden === null) {
    throw new Error("Kein Schalter gerendert");
  }
  return gefunden;
}

describe("Der Gliederungsschalter", () => {
  it("steht an, wo Projekt gilt — und sonst aus", async () => {
    const faelle = [
      ["PROJEKT", "true"],
      ["PARTNER", "false"],
      [null, "false"],
    ] as const;

    for (const [gewaehlt, erwartet] of faelle) {
      const gerendert = await rendere(
        <GliederungUmschalter gewaehlt={gewaehlt} aufAuswahl={() => {}} />,
      );
      try {
        const knopf = schalter(gerendert.behaelter);
        expect(knopf.getAttribute("aria-checked")).toBe(erwartet);
        expect(gerendert.behaelter.querySelector(`label[for="${knopf.id}"]`)?.textContent).toBe(
          T.schalter,
        );
        const hinweis = knopf.getAttribute("aria-describedby") ?? "";
        expect(gerendert.behaelter.querySelector(`#${hinweis}`)?.textContent).toBe(T.hinweis);
      } finally {
        await gerendert.abbauen();
      }
    }
  });

  it("meldet beim Umschalten den anderen Code — auch über die Beschriftung", async () => {
    const aufAuswahl = vi.fn();

    const projekt = await rendere(
      <GliederungUmschalter gewaehlt="PROJEKT" aufAuswahl={aufAuswahl} />,
    );
    try {
      await act(async () => {
        schalter(projekt.behaelter).dispatchEvent(new MouseEvent("click", { bubbles: true }));
      });
      expect(aufAuswahl).toHaveBeenLastCalledWith("PARTNER");
    } finally {
      await projekt.abbauen();
    }

    const partner = await rendere(
      <GliederungUmschalter gewaehlt="PARTNER" aufAuswahl={aufAuswahl} />,
    );
    try {
      await act(async () => {
        partner.behaelter
          .querySelector("label")
          ?.dispatchEvent(new MouseEvent("click", { bubbles: true }));
      });
      expect(aufAuswahl).toHaveBeenLastCalledWith("PROJEKT");
      expect(aufAuswahl).toHaveBeenCalledTimes(2);
    } finally {
      await partner.abbauen();
    }
  });
});
