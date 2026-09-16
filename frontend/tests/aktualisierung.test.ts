import { describe, expect, it } from "vitest";

import {
  AKTUALISIERUNG_INTERVALL_MS,
  abfrageNachNeuLaden,
  aktualisierungsintervall,
  automatikzustand,
  stapelNachNeuLaden,
} from "@/features/nachrichten/aktualisierung";
import { aufbauAktiv } from "@/features/dashboard/verlauf";
import { alsAbfrage, type Nachrichtenfilter } from "@/features/nachrichten/filter";

/**
 * **Wann die Liste von selbst fragt, und wohin „Neu laden" führt** — als reine
 * Funktionen (`docs/neu-laden.md`). Dass die Ansichten sie auch abfragen, steht
 * in `tests/neu-laden.test.tsx`.
 */

const LEER: Nachrichtenfilter = {
  zeitraum: null,
  von: null,
  bis: null,
  status: null,
  prozess: null,
  suche: null,
  langeSuche: false,
  sortierung: null,
  nachricht: null,
};

describe("Das Intervall der automatischen Aktualisierung", () => {
  it("fragt nur, wenn der Schalter an ist, Seite eins steht und der Tab sichtbar ist", () => {
    for (const an of [true, false]) {
      for (const aufSeiteEins of [true, false]) {
        for (const sichtbar of [true, false]) {
          const erwartet = an && aufSeiteEins && sichtbar ? 60_000 : false;
          expect(
            aktualisierungsintervall({ an, aufSeiteEins, sichtbar }),
            `an=${an} aufSeiteEins=${aufSeiteEins} sichtbar=${sichtbar}`,
          ).toBe(erwartet);
        }
      }
    }
  });

  it("hat keine Wahl: sechzig Sekunden", () => {
    expect(AKTUALISIERUNG_INTERVALL_MS).toBe(60_000);
  });
});

describe("Was der Schalter zeigt", () => {
  it("ist aus, solange er aus ist — auf jeder Seite", () => {
    expect(automatikzustand(false, true)).toBe("aus");
    expect(automatikzustand(false, false)).toBe("aus");
  });

  it("ist an auf Seite eins und pausiert auf jeder späteren", () => {
    expect(automatikzustand(true, true)).toBe("an");
    expect(automatikzustand(true, false)).toBe("pausiert");
  });
});

describe("Wohin Neu laden führt", () => {
  const GEFILTERT: Nachrichtenfilter = {
    ...LEER,
    zeitraum: "7d",
    status: ["FEHLER"],
    prozess: ["P-0815"],
    suche: "lieferschein",
    sortierung: "aelteste",
  };

  it("auf Seite eins desselben Filters, ohne Cursor", () => {
    const ziel = abfrageNachNeuLaden(GEFILTERT);
    expect(ziel).toBe(alsAbfrage(GEFILTERT));
    expect(ziel).not.toContain("cursor");
    for (const teil of ["zeitraum=7d", "status=FEHLER", "prozess=P-0815", "suche=lieferschein"]) {
      expect(ziel).toContain(teil);
    }
    // Von Seite sieben aus dasselbe Ziel wie von Seite eins.
    expect(alsAbfrage(GEFILTERT, "cursor-7")).toContain("cursor=cursor-7");
    expect(ziel).toBe(abfrageNachNeuLaden({ ...GEFILTERT }));
  });

  it("nimmt die geöffnete Nachricht nicht mit (E‑169)", () => {
    expect(abfrageNachNeuLaden({ ...GEFILTERT, nachricht: "8f3a1c2e" })).toBe(
      abfrageNachNeuLaden(GEFILTERT),
    );
  });

  it("setzt den Stapel zurück, wenn seit dem Klick niemand geblättert hat", () => {
    const stapel = ["cursor-2", "cursor-3"];
    expect(stapelNachNeuLaden(stapel, stapel)).toEqual([]);
  });

  it("lässt eine neue Stelle stehen, die der Nutzer inzwischen gewählt hat", () => {
    const beimKlick = ["cursor-2"];
    const weitergeblaettert = [...beimKlick, "cursor-3"];
    expect(stapelNachNeuLaden(beimKlick, weitergeblaettert)).toBe(weitergeblaettert);
    // Auch ein gleich langer, aber neuer Stapel ist eine neue Stelle — etwa nach
    // einer Filteränderung und einem Blättern.
    const neu = ["cursor-9"];
    expect(stapelNachNeuLaden(beimKlick, neu)).toBe(neu);
  });
});

describe("Der Aufbau des Verlaufs (E‑170)", () => {
  it("baut beim ersten Bild eines Zeitraums auf", () => {
    expect(aufbauAktiv(null, "48H")).toBe("auto");
  });

  it("steht beim Neuladen desselben Zeitraums still", () => {
    expect(aufbauAktiv("48H", "48H")).toBe(false);
  });

  it("baut nach einem Zeitraumwechsel wieder auf (E‑86)", () => {
    expect(aufbauAktiv("48H", "30T")).toBe("auto");
  });
});
