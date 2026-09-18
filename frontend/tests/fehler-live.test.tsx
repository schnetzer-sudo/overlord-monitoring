// @vitest-environment jsdom

import { act } from "react";
import { NuqsTestingAdapter } from "nuqs/adapters/testing";
import { afterEach, describe, expect, it, vi } from "vitest";

import type { Dashboard } from "@/features/dashboard/api";
import { DashboardAnsicht } from "@/features/dashboard/components/dashboard-ansicht";
import type { Prozessbaum } from "@/features/nachrichten/api";
import { ProzessAnsicht } from "@/features/nachrichten/components/prozessansicht";
import { einsetzen, texteFuer } from "@/i18n";
import type { FehlerLive } from "@/lib/fehler-live";
import type { LiveRest } from "@/lib/live-rest";

import { rendere } from "./hilfe/rendern";

/**
 * **Der Hinweis zu Fehler live — Aussagen über Anwesenheit, Abwesenheit und
 * Reihenfolge im Baum der Übersicht** (`docs/fehler-live.md` §6).
 *
 * Bei `AUSGESETZT` steht über den Kacheln ein Satz: Die Fehler kommen aus der
 * stündlichen Aggregation. Bei `ANGEWANDT` steht **keiner** — die naheliegende
 * Schreibweise (der Kasten immer da, nur mit anderem Text) bestünde jede Prüfung
 * an der Beschriftung, und die Kacheln sind die Eichung dafür, dass die Ansicht
 * steht. **Auch im Leerzustand** steht er: „nichts im Zeitraum" kann aus dem
 * Rollup stammen. Und stehen beide Hinweise, steht der zum Live-Rest **zuerst** —
 * eine Aussage über die Reihenfolge im Dokument, die keine reine Funktion trägt.
 *
 * **Seit Teil B (18.09.2026) derselbe Baustein im Prozessbaum** (§5b, E‑214): im
 * klebenden Kopf der Baumspalte, unter den Kopfzahlen und unter dem Hinweis zum
 * Live-Rest — mit dem Baum als Eichung, dass die Ansicht steht.
 */

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: () => {}, replace: () => {} }),
  usePathname: () => "/",
}));

const TEXTE = texteFuer("de");

const LIVE_REST_ANGEWANDT: LiveRest = { zustand: "ANGEWANDT", vollstaendigBis: null };

function dashboard(fehlerLive: FehlerLive, liveRest: LiveRest, leer = false): Dashboard {
  return {
    zeitraum: "48H",
    fenster: { von: "2025-12-28T14:00:00Z", bis: "2025-12-30T14:00:00Z" },
    leer,
    verlauf: leer
      ? []
      : [
          {
            eimer: "2025-12-30T13:00:00Z",
            gesamt: 3,
            einordnungen: [
              { einordnung: "FEHLER", anzahl: 1 },
              { einordnung: "ABGESCHLOSSEN", anzahl: 2 },
            ],
          },
        ],
    kacheln: {
      nachrichten: leer ? 0 : 3,
      fehler: leer
        ? { anzahl: 0, arten: [] }
        : { anzahl: 1, arten: [{ rohwert: "ERROR_TIMEOUT", art: "TIMEOUT", anzahl: 1 }] },
      laeuft: { anzahl: 0, aeltesteSekunden: null, ermittelbar: true },
      wartend: { anzahl: 0, aeltesteSekunden: null, ermittelbar: true },
    },
    verteilung: {
      partner: { zeilen: [{ art: "NICHT_ZUGEORDNET", wert: null, anzahl: 0, enthaltene: null }] },
      richtung: { zeilen: [{ art: "NICHT_ZUGEORDNET", wert: null, anzahl: 0, enthaltene: null }] },
    },
    zuletztAufgefallen: [],
    stand: { beendetAm: "2025-12-30T13:05:00Z", art: "DELTA" },
    liveRest,
    fehlerLive,
    plattform: {
      dienste: [],
      ablagen: {
        zustand: "UNGEKLAERT",
        grund: "ABGESCHALTET",
        ziele: [],
        geprueftAm: null,
        alterSekunden: null,
      },
    },
  };
}

function stelleUebersicht(rumpf: Dashboard) {
  vi.stubGlobal(
    "fetch",
    vi.fn(() =>
      Promise.resolve(
        new Response(JSON.stringify(rumpf), {
          status: 200,
          headers: { "content-type": "application/json" },
        }),
      ),
    ),
  );
}

afterEach(() => {
  vi.unstubAllGlobals();
});

async function zuege(anzahl = 12): Promise<void> {
  for (let zug = 0; zug < anzahl; zug++) {
    await act(async () => {
      await new Promise((fertig) => setTimeout(fertig, 0));
    });
  }
}

async function rendereUebersicht(fehlerLive: FehlerLive, liveRest: LiveRest, leer = false) {
  stelleUebersicht(dashboard(fehlerLive, liveRest, leer));
  const gerendert = await rendere(
    <NuqsTestingAdapter searchParams="?zeitraum=48H" hasMemory>
      <DashboardAnsicht />
    </NuqsTestingAdapter>,
  );
  await zuege();
  return gerendert;
}

/** Die Hinweiskästen der Seite — die Fehleranzeige trägt dieselbe Bauform, aber ihren eigenen Titel. */
function hinweise(behaelter: HTMLElement): string[] {
  return [...behaelter.querySelectorAll('[data-slot="alert"]')].map(
    (kasten) => kasten.textContent ?? "",
  );
}

/** Die Eichung: Die Kachelreihe steht, die Antwort ist also angekommen. */
function kachelnStehen(behaelter: HTMLElement): boolean {
  // Jede Kachel ist eine Card; der Leerzustand ist keine (`components/zustand.tsx`).
  return behaelter.querySelector('[data-slot="card"]') !== null;
}

describe("Der Hinweis zu Fehler live in der Übersicht", () => {
  it("steht bei AUSGESETZT über den Kacheln", async () => {
    const gerendert = await rendereUebersicht({ zustand: "AUSGESETZT" }, LIVE_REST_ANGEWANDT);
    try {
      const { behaelter } = gerendert;
      expect(kachelnStehen(behaelter), "Eichung: die Kacheln stehen").toBe(true);
      expect(hinweise(behaelter)).toEqual([TEXTE.fehlerLive.ausgesetzt]);

      // Über den Kacheln: Der Kasten kommt im Dokument vor der ersten Kachel.
      const kasten = behaelter.querySelector('[data-slot="alert"]');
      const kachel = behaelter.querySelector('[data-slot="card"]');
      expect(
        kasten!.compareDocumentPosition(kachel!) & Node.DOCUMENT_POSITION_FOLLOWING,
      ).toBeTruthy();
    } finally {
      await gerendert.abbauen();
    }
  });

  it("steht bei AUSGESETZT auch im Leerzustand", async () => {
    const gerendert = await rendereUebersicht({ zustand: "AUSGESETZT" }, LIVE_REST_ANGEWANDT, true);
    try {
      const { behaelter } = gerendert;
      expect(behaelter.textContent, "Eichung: der Leerzustand steht").toContain(
        TEXTE.dashboard.leerTitel,
      );
      expect(kachelnStehen(behaelter), "Gegenprobe: keine Kacheln im Leerzustand").toBe(false);
      expect(hinweise(behaelter)).toEqual([TEXTE.fehlerLive.ausgesetzt]);
    } finally {
      await gerendert.abbauen();
    }
  });

  it("steht bei ANGEWANDT nicht", async () => {
    const gerendert = await rendereUebersicht({ zustand: "ANGEWANDT" }, LIVE_REST_ANGEWANDT);
    try {
      expect(kachelnStehen(gerendert.behaelter), "Eichung").toBe(true);
      expect(hinweise(gerendert.behaelter)).toEqual([]);
    } finally {
      await gerendert.abbauen();
    }
  });

  it("steht bei ANGEWANDT auch im Leerzustand nicht", async () => {
    const gerendert = await rendereUebersicht({ zustand: "ANGEWANDT" }, LIVE_REST_ANGEWANDT, true);
    try {
      expect(gerendert.behaelter.textContent, "Eichung").toContain(TEXTE.dashboard.leerTitel);
      expect(hinweise(gerendert.behaelter)).toEqual([]);
    } finally {
      await gerendert.abbauen();
    }
  });

  /** Kein Rot: Der Kasten trägt nicht die Variante der Fehleranzeige. */
  it("ist kein Fehler — die Fehlerfarbe steht nicht am Kasten", async () => {
    const gerendert = await rendereUebersicht({ zustand: "AUSGESETZT" }, LIVE_REST_ANGEWANDT);
    try {
      const kasten = gerendert.behaelter.querySelector('[data-slot="alert"]');
      expect(kasten).not.toBeNull();
      expect(kasten?.className).not.toContain("destructive");
    } finally {
      await gerendert.abbauen();
    }
  });

  it("steht unter dem Hinweis zum Live-Rest, wenn beide stehen", async () => {
    const gerendert = await rendereUebersicht(
      { zustand: "AUSGESETZT" },
      { zustand: "AUSGESETZT", vollstaendigBis: null },
    );
    try {
      expect(hinweise(gerendert.behaelter), "Beide, der zum Live-Rest zuerst").toEqual([
        TEXTE.liveRest.ausgesetztOhneLauf,
        TEXTE.fehlerLive.ausgesetzt,
      ]);
    } finally {
      await gerendert.abbauen();
    }
  });
});

/* ─── Der Prozessbaum (Teil B) ───────────────────────────────────────────── */

function prozessbaum(fehlerLive: FehlerLive, liveRest: LiveRest): Prozessbaum {
  return {
    zeitraum: "48H",
    gliederung: "PARTNER",
    fenster: { von: "2025-12-28T14:00:00Z", bis: "2025-12-30T14:00:00Z" },
    stilleSchwelleMonate: 3,
    liveRest,
    fehlerLive,
    gesamt: { anzahlProzesse: 1, bewegt: 1, still: 0, nie: 0, nachrichten: 2, fehler: 1 },
    ebenen: ["PARTNER", "PROZESS"],
    knoten: [
      {
        schluessel: "ERFUNDEN",
        name: "ERFUNDEN",
        anzahlProzesse: 1,
        nachrichten: 2,
        fehler: 1,
        kinder: [
          {
            schluessel: "P-0815",
            name: "Ein erfundener Prozess",
            processId: "P-0815",
            nachrichten: 2,
            fehler: 1,
            letzteBewegung: "2025-12-30T13:30:00Z",
            zustand: "BEWEGT",
          },
        ],
      },
    ],
  };
}

function stelleBaum(baum: Prozessbaum) {
  vi.stubGlobal(
    "fetch",
    vi.fn((eingabe: RequestInfo | URL) => {
      const url = new URL(String(eingabe), "http://localhost");
      if (url.pathname !== "/api/prozesse/baum") {
        return Promise.resolve(
          new Response(JSON.stringify({ type: "about:blank", title: "Not Found", status: 404 }), {
            status: 404,
            headers: { "content-type": "application/problem+json" },
          }),
        );
      }
      return Promise.resolve(
        new Response(JSON.stringify(baum), {
          status: 200,
          headers: { "content-type": "application/json" },
        }),
      );
    }),
  );
}

async function rendereBaum(fehlerLive: FehlerLive, liveRest: LiveRest) {
  stelleBaum(prozessbaum(fehlerLive, liveRest));
  const gerendert = await rendere(
    <NuqsTestingAdapter searchParams="?zeitraum=48H" hasMemory>
      <ProzessAnsicht />
    </NuqsTestingAdapter>,
  );
  await zuege();
  return gerendert;
}

/** Die Eichung: Der Baum steht, die Antwort ist also angekommen. */
function baumSteht(behaelter: HTMLElement): boolean {
  return behaelter.querySelector('[role="tree"]') !== null;
}

describe("Der Hinweis zu Fehler live im Prozessbaum", () => {
  it("steht bei AUSGESETZT im klebenden Kopf der Baumspalte, unter den Kopfzahlen", async () => {
    const gerendert = await rendereBaum({ zustand: "AUSGESETZT" }, LIVE_REST_ANGEWANDT);
    try {
      const { behaelter } = gerendert;
      expect(baumSteht(behaelter), "Eichung: der Baum steht").toBe(true);
      expect(hinweise(behaelter)).toEqual([TEXTE.fehlerLive.ausgesetzt]);

      const kasten = behaelter.querySelector('[data-slot="alert"]');
      // Der klebende Kopf ist der, der das Eingrenzungsfeld trägt.
      const kopf = behaelter.querySelector("#baum-eingrenzung")?.closest(".sticky");
      expect(kopf, "Eichung: der klebende Kopf der Baumspalte").not.toBeNull();
      expect(kopf!.contains(kasten)).toBe(true);

      // Unter den Kopfzahlen: Der Absatz mit den drei Zuständen kommt im Dokument davor.
      const verteilung = einsetzen(TEXTE.prozesse.baum.verteilung, {
        prozesse: 1,
        bewegt: 1,
        still: 0,
        nie: 0,
      });
      const kopfzahlen = [...kopf!.querySelectorAll("p")].find(
        (absatz) => absatz.textContent === verteilung,
      );
      expect(kopfzahlen, "Eichung: die Kopfzahlen stehen im Kopf").toBeDefined();
      expect(
        kopfzahlen!.compareDocumentPosition(kasten!) & Node.DOCUMENT_POSITION_FOLLOWING,
      ).toBeTruthy();
      // Und über dem Baum.
      const baum = behaelter.querySelector('[role="tree"]');
      expect(
        kasten!.compareDocumentPosition(baum!) & Node.DOCUMENT_POSITION_FOLLOWING,
      ).toBeTruthy();
    } finally {
      await gerendert.abbauen();
    }
  });

  it("steht unter dem Hinweis zum Live-Rest, wenn beide stehen", async () => {
    const gerendert = await rendereBaum(
      { zustand: "AUSGESETZT" },
      { zustand: "AUSGESETZT", vollstaendigBis: null },
    );
    try {
      expect(baumSteht(gerendert.behaelter), "Eichung").toBe(true);
      expect(hinweise(gerendert.behaelter), "Beide, der zum Live-Rest zuerst").toEqual([
        TEXTE.liveRest.ausgesetztOhneLauf,
        TEXTE.fehlerLive.ausgesetzt,
      ]);
    } finally {
      await gerendert.abbauen();
    }
  });

  it("steht bei ANGEWANDT nicht", async () => {
    const gerendert = await rendereBaum({ zustand: "ANGEWANDT" }, LIVE_REST_ANGEWANDT);
    try {
      expect(baumSteht(gerendert.behaelter), "Eichung").toBe(true);
      expect(hinweise(gerendert.behaelter)).toEqual([]);
    } finally {
      await gerendert.abbauen();
    }
  });

  /** Kein Rot: Der Kasten trägt nicht die Variante der Fehleranzeige. */
  it("ist kein Fehler — die Fehlerfarbe steht nicht am Kasten", async () => {
    const gerendert = await rendereBaum({ zustand: "AUSGESETZT" }, LIVE_REST_ANGEWANDT);
    try {
      const kasten = gerendert.behaelter.querySelector('[data-slot="alert"]');
      expect(kasten).not.toBeNull();
      expect(kasten?.className).not.toContain("destructive");
    } finally {
      await gerendert.abbauen();
    }
  });
});
