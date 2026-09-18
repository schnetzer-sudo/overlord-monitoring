import { describe, expect, it } from "vitest";

import {
  AKTUALISIERUNG_INTERVALL_MS,
  abfrageNachNeuLaden,
  aktualisierungsintervall,
  automatikzustand,
  blaettere,
  stapelNachNeuLaden,
  trefferBisHier,
  type Stapeleintrag,
} from "@/features/nachrichten/aktualisierung";
import { aufbauAktiv } from "@/features/dashboard/verlauf";
import { alsAbfrage, type Nachrichtenfilter } from "@/features/nachrichten/filter";

/**
 * **Wann die Liste von selbst fragt, und wohin „Neu laden" führt** — als reine
 * Funktionen (`docs/neu-laden.md`). Dass die Ansichten sie auch abfragen, steht
 * in `tests/neu-laden.test.tsx`.
 *
 * *Seit dem 18.09.2026 (E‑216)* auch der Blätterstapel und der Σ unter der
 * Liste (`docs/nachrichtenliste.md` §8.3). Wie er aussieht und dass die
 * Ansichten ihn abfragen, steht in `tests/blaettern.test.tsx`.
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

describe("Der Blätterstapel und der Σ (E‑216)", () => {
  /** Eine Seite mit so vielen Zeilen — und, wenn es weitergeht, dem Cursor dorthin. */
  function seite(zeilen: number, weiter: string | null = null) {
    return {
      items: Array.from({ length: zeilen }, (_, zeile) => zeile),
      nextCursor: weiter,
      hasMore: weiter !== null,
    };
  }

  /** Zweimal „Vor" von Seite eins aus, je über eine volle Seite: der Stapel auf Seite drei. */
  function aufSeiteDrei(): Stapeleintrag[] {
    const zwei = blaettere([], { art: "vor", seite: seite(50, "cursor-2") });
    return blaettere(zwei, { art: "vor", seite: seite(50, "cursor-3") });
  }

  it("zählt eine Seite ohne weitere genau, und Vor ändert dort nichts", () => {
    expect(trefferBisHier([], seite(17))).toEqual({ anzahl: 17, genau: true });
    const leer: Stapeleintrag[] = [];
    expect(blaettere(leer, { art: "vor", seite: seite(17) })).toBe(leer);
  });

  it("zählt Seite eins mit weiteren als Untergrenze", () => {
    expect(trefferBisHier([], seite(50, "cursor-2"))).toEqual({ anzahl: 50, genau: false });
  });

  it("summiert bis zur letzten Seite nach zweimal Blättern", () => {
    const stapel = aufSeiteDrei();
    expect(stapel.map((eintrag) => eintrag.cursor)).toEqual(["cursor-2", "cursor-3"]);
    expect(trefferBisHier(stapel, seite(17))).toEqual({ anzahl: 117, genau: true });
  });

  it("zeigt nach Zurück die Zahl bis zu jener Seite", () => {
    const zwei = blaettere(aufSeiteDrei(), { art: "zurueck" });
    expect(trefferBisHier(zwei, seite(50, "cursor-3"))).toEqual({ anzahl: 100, genau: false });
    const eins = blaettere(zwei, { art: "zurueck" });
    expect(eins).toEqual([]);
    expect(trefferBisHier(eins, seite(50, "cursor-2"))).toEqual({ anzahl: 50, genau: false });
  });

  it("beginnt nach einem Filterwechsel wieder bei der ersten Seite", () => {
    const neu = blaettere(aufSeiteDrei(), { art: "neuerFilter" });
    expect(neu).toEqual([]);
    expect(trefferBisHier(neu, seite(7))).toEqual({ anzahl: 7, genau: true });
  });

  it("zählt, was ankam, und nicht Seitentiefe mal Seitengröße", () => {
    // Eine kürzere Seite vor der letzten schließt das Backend heute aus
    // (`Seite.aus`); gestellt ist sie hier, weil die Summe daran nicht hängen darf.
    const zwei = blaettere([], { art: "vor", seite: seite(50, "cursor-2") });
    const drei = blaettere(zwei, { art: "vor", seite: seite(48, "cursor-3") });
    expect(trefferBisHier(drei, seite(17))).toEqual({ anzahl: 115, genau: true });
  });
});
