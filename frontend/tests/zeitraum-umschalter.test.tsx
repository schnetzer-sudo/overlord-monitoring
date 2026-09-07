// @vitest-environment jsdom

import { act } from "react";
import { describe, expect, it, vi } from "vitest";

import { ZeitraumUmschalter } from "@/components/zeitraum-umschalter";

import { rendere } from "./hilfe/rendern";

/**
 * **Zwei Fälle, für die ein gerenderter Umschalter die einzige Prüfung ist.**
 *
 * Der vierte Knopf „Frei" ist **freiwillig** (`docs/process-view.md` §37 ff.):
 * Ohne die Angabe `aufFrei` sind es drei Knöpfe, mit ihr vier — und das
 * Dashboard ruft den Umschalter ohne. Das ist eine Aussage über **Abwesenheit
 * im Baum**, und die naheliegende Schreibweise (der Knopf immer da, nur ohne
 * Wirkung) bestünde jede Prüfung an einer reinen Funktion. Dazu die
 * **Verdrahtung**: Ein Klick auf „Frei" ruft `aufFrei` und **nicht**
 * `aufAuswahl` — sonst käme im Feature ein Code an, den die Liste der drei
 * Paare nicht kennt.
 *
 * **Alle Prüfwerte sind erfunden**; es gibt keine Antwort und keine Anfrage.
 */

function knoepfe(behaelter: HTMLElement): HTMLButtonElement[] {
  return [...behaelter.querySelectorAll("button")];
}

describe("Der Zeitraumumschalter", () => {
  it("hat ohne die freiwillige Angabe drei Knöpfe, mit ihr vier", async () => {
    const ohne = await rendere(<ZeitraumUmschalter gewaehlt={null} aufAuswahl={() => {}} />);
    try {
      expect(knoepfe(ohne.behaelter).map((k) => k.textContent)).toEqual([
        "48 Stunden",
        "30 Tage",
        "12 Monate",
      ]);
    } finally {
      await ohne.abbauen();
    }

    const mit = await rendere(
      <ZeitraumUmschalter gewaehlt={null} aufAuswahl={() => {}} aufFrei={() => {}} />,
    );
    try {
      expect(knoepfe(mit.behaelter).map((k) => k.textContent)).toEqual([
        "48 Stunden",
        "30 Tage",
        "12 Monate",
        "Frei",
      ]);
    } finally {
      await mit.abbauen();
    }
  });

  it("ruft bei „Frei“ die freiwillige Angabe und nicht die Auswahl eines Paares", async () => {
    const aufAuswahl = vi.fn();
    const aufFrei = vi.fn();
    const gerendert = await rendere(
      <ZeitraumUmschalter gewaehlt="30T" aufAuswahl={aufAuswahl} aufFrei={aufFrei} />,
    );
    try {
      const frei = knoepfe(gerendert.behaelter).find((k) => k.textContent === "Frei");
      expect(frei).toBeDefined();
      await act(async () => {
        frei?.dispatchEvent(new MouseEvent("click", { bubbles: true }));
      });
      expect(aufFrei).toHaveBeenCalledTimes(1);
      expect(aufAuswahl).not.toHaveBeenCalled();

      // Und die Gegenprobe: Ein Paar ruft die Auswahl mit seinem Code.
      const zwoelf = knoepfe(gerendert.behaelter).find((k) => k.textContent === "12 Monate");
      await act(async () => {
        zwoelf?.dispatchEvent(new MouseEvent("click", { bubbles: true }));
      });
      expect(aufAuswahl).toHaveBeenCalledWith("12M");
      expect(aufFrei).toHaveBeenCalledTimes(1);
    } finally {
      await gerendert.abbauen();
    }
  });
});
