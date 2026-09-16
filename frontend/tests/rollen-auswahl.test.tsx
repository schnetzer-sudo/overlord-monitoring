// @vitest-environment jsdom

import { act } from "react";
import { describe, expect, it, vi } from "vitest";

import { RollenAuswahl } from "@/features/benutzer/components/rollen-auswahl";
import { texteFuer } from "@/i18n";

import { rendere } from "./hilfe/rendern";

/**
 * **Die Rollenauswahl ist eine Liste der Anwendung und kein natives Feld mehr**
 * (Punkt 180, `docs/benutzerverwaltung-frontend.md` §18).
 *
 * Die Bedingung für einen gerenderten Baum (`tests/hilfe/rendern.tsx`) erfüllen
 * alle vier Fälle, und zwar in den beiden Klassen, für die es sie gibt:
 *
 * 1. **Abwesenheit im Baum.** Dass kein `<select>` mehr dasteht, ist der ganze
 *    Anlass des Umbaus: Die Liste eines nativen Felds zeichnet der Browser, und
 *    keine Prüfung des Projekts erreicht ihre Farbe. Eine reine Funktion kann das
 *    nicht sagen.
 * 2. **Verdrahtung.** Ob eine gesperrte Rolle zwar dasteht, aber weder Klick noch
 *    Taste sie wählen; ob eine Wahl desselben Werts **nichts** meldet (im
 *    Zeilenformular hieße jede Meldung ein `PUT`, und jedes `PUT` verwirft alle
 *    Sitzungen des Kontos, E5); und ob `↓` am geschlossenen Feld nur **öffnet** —
 *    all das entsteht erst im Zusammenspiel von Ereignis und Zustand.
 *
 * Geöffnet wird über `click`, Radix' eigener Weg für `Popover`; die Zeilen hängen
 * im Portal und damit am `document`.
 */

const TEXTE = texteFuer("de");

const feld = (behaelter: HTMLElement) => behaelter.querySelector<HTMLButtonElement>("#rolle");
const liste = () => document.querySelector<HTMLElement>('[role="listbox"]');
const zeile = (wert: string) =>
  document.querySelector<HTMLElement>(`[role="option"][data-wert="${wert}"]`);

async function klick(ziel: HTMLElement | null) {
  expect(ziel).not.toBeNull();
  await act(async () => {
    ziel?.click();
  });
}

async function taste(ziel: Element | null, key: string) {
  expect(ziel).not.toBeNull();
  await act(async () => {
    ziel?.dispatchEvent(new KeyboardEvent("keydown", { key, bubbles: true, cancelable: true }));
  });
}

describe("Die Rollenauswahl", () => {
  it("ist kein natives Auswahlfeld und lässt beide Rollen wählen, auch ADMIN", async () => {
    const aufWahl = vi.fn();
    const { behaelter, abbauen } = await rendere(
      <RollenAuswahl
        id="rolle"
        beschriftung={TEXTE.benutzer.anlegen.rolle}
        wert=""
        leer={TEXTE.benutzer.anlegen.waehlen}
        aufWahl={aufWahl}
      />,
    );

    try {
      expect(document.querySelector("select")).toBeNull();
      expect(feld(behaelter)?.textContent).toBe(TEXTE.benutzer.anlegen.waehlen);

      await klick(feld(behaelter));
      expect(liste()?.getAttribute("aria-label")).toBe(TEXTE.benutzer.anlegen.rolle);
      expect(zeile("ADMIN")?.textContent).toBe(TEXTE.rolle.ADMIN);
      expect(zeile("MANDANT")?.textContent).toBe(TEXTE.rolle.MANDANT);

      await klick(zeile("ADMIN"));
      expect(aufWahl).toHaveBeenCalledExactlyOnceWith("ADMIN");
      expect(feld(behaelter)?.getAttribute("aria-expanded")).toBe("false");
    } finally {
      await abbauen();
    }
  });

  it("zeigt eine nicht wählbare Rolle, wählt sie aber weder per Klick noch per Taste", async () => {
    const aufWahl = vi.fn();
    const { behaelter, abbauen } = await rendere(
      <RollenAuswahl
        id="rolle"
        beschriftung={TEXTE.benutzer.formular.rolle}
        wert="ADMIN"
        nichtWaehlbar={["MANDANT"]}
        aufWahl={aufWahl}
      />,
    );

    try {
      await klick(feld(behaelter));
      expect(zeile("MANDANT")?.getAttribute("aria-disabled")).toBe("true");

      await klick(zeile("MANDANT"));
      await taste(zeile("MANDANT"), "Enter");
      expect(aufWahl).not.toHaveBeenCalled();

      // Gegenprobe: dieselbe Zeile ohne Sperre ist wählbar.
      await taste(zeile("ADMIN"), "Escape");
    } finally {
      await abbauen();
    }

    const frei = vi.fn();
    const zweiter = await rendere(
      <RollenAuswahl
        id="rolle"
        beschriftung={TEXTE.benutzer.formular.rolle}
        wert="ADMIN"
        aufWahl={frei}
      />,
    );
    try {
      await klick(feld(zweiter.behaelter));
      await taste(zeile("MANDANT"), "Enter");
      expect(frei).toHaveBeenCalledExactlyOnceWith("MANDANT");
    } finally {
      await zweiter.abbauen();
    }
  });

  it("meldet nichts, wenn der schon gewählte Wert noch einmal gewählt wird", async () => {
    const aufWahl = vi.fn();
    const { behaelter, abbauen } = await rendere(
      <RollenAuswahl
        id="rolle"
        beschriftung={TEXTE.benutzer.formular.rolle}
        wert="MANDANT"
        aufWahl={aufWahl}
      />,
    );

    try {
      await klick(feld(behaelter));
      expect(zeile("MANDANT")?.getAttribute("aria-selected")).toBe("true");
      await klick(zeile("MANDANT"));
      expect(aufWahl).not.toHaveBeenCalled();
      expect(feld(behaelter)?.getAttribute("aria-expanded")).toBe("false");
    } finally {
      await abbauen();
    }
  });

  it("öffnet mit ↓ am geschlossenen Feld, ohne den Wert zu ändern, und wählt mit ↓ und Enter", async () => {
    const aufWahl = vi.fn();
    const { behaelter, abbauen } = await rendere(
      <RollenAuswahl
        id="rolle"
        beschriftung={TEXTE.benutzer.formular.rolle}
        wert="ADMIN"
        aufWahl={aufWahl}
      />,
    );

    try {
      await taste(feld(behaelter), "ArrowDown");
      expect(liste()).not.toBeNull();
      expect(aufWahl).not.toHaveBeenCalled();
      // Der Fokus steht beim Öffnen auf dem gespeicherten Wert.
      expect(document.activeElement).toBe(zeile("ADMIN"));

      await taste(liste(), "ArrowDown");
      expect(document.activeElement).toBe(zeile("MANDANT"));
      await taste(document.activeElement, "Enter");
      expect(aufWahl).toHaveBeenCalledExactlyOnceWith("MANDANT");
    } finally {
      await abbauen();
    }
  });
});
