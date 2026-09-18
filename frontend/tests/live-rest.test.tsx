// @vitest-environment jsdom

import { act } from "react";
import { NuqsTestingAdapter } from "nuqs/adapters/testing";
import { afterEach, describe, expect, it, vi } from "vitest";

import type { Dashboard } from "@/features/dashboard/api";
import { DashboardAnsicht } from "@/features/dashboard/components/dashboard-ansicht";
import type { Prozessbaum } from "@/features/nachrichten/api";
import { ProzessAnsicht } from "@/features/nachrichten/components/prozessansicht";
import { texteFuer } from "@/i18n";
import type { LiveRest } from "@/lib/live-rest";

import { rendere } from "./hilfe/rendern";

/**
 * **Der Hinweis zum Live-Rest — Aussagen über Anwesenheit und Abwesenheit im
 * Baum und in der Übersicht** (`docs/live-rest.md`).
 *
 * Bei `AUSGESETZT` steht bei den Zahlen des Baums ein Satz: mit Zeitangabe,
 * wenn es einen Lauf gab, ohne, wenn nicht. Bei `ANGEWANDT` und `NICHT_NOETIG`
 * steht **kein** Satz — und genau das ist die Aussage, die keine reine Funktion
 * trägt: Die naheliegende Schreibweise (der Hinweis immer da, nur mit anderem
 * Text) bestünde jede Prüfung an der Beschriftung.
 *
 * **Seit Teil B (17.09.2026) derselbe Baustein in der Übersicht** (E‑192,
 * `components/live-rest-hinweis.tsx`): über den Kacheln, auch im Leerzustand, und
 * bei `ANGEWANDT` nichts — mit den Kacheln als Eichung, dass die Seite steht.
 *
 * **Die Zeitangabe wird nicht abgetippt:** Der Test formatiert denselben
 * Zeitpunkt über denselben Weg (`formatiereZeitpunkt`, UTC ohne Provider) und
 * prüft, dass der Satz ihn trägt.
 *
 * **Kein Nachladen** (Punkt 189, entschieden am 17.09.2026: E‑164 gilt) — die
 * Prozessansicht lädt weiter nur von Hand, und das prüft
 * `tests/neu-laden.test.tsx` bereits.
 */

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: () => {}, replace: () => {} }),
  usePathname: () => "/",
}));

const TEXTE = texteFuer("de");
const G = "2025-12-30T13:00:00Z";

function prozessbaum(liveRest: LiveRest): Prozessbaum {
  return {
    zeitraum: "48H",
    gliederung: "PARTNER",
    fenster: { von: "2025-12-28T14:00:00Z", bis: "2025-12-30T14:00:00Z" },
    stilleSchwelleMonate: 3,
    liveRest,
    fehlerLive: { zustand: "ANGEWANDT" },
    gesamt: { anzahlProzesse: 1, bewegt: 1, still: 0, nie: 0, nachrichten: 2, fehler: 0 },
    ebenen: ["PARTNER", "PROZESS"],
    knoten: [
      {
        schluessel: "ERFUNDEN",
        name: "ERFUNDEN",
        anzahlProzesse: 1,
        nachrichten: 2,
        fehler: 0,
        kinder: [
          {
            schluessel: "P-0815",
            name: "Ein erfundener Prozess",
            processId: "P-0815",
            nachrichten: 2,
            fehler: 0,
            letzteBewegung: "2025-12-30T13:30:00Z",
            zustand: "BEWEGT",
          },
        ],
      },
    ],
  };
}

function stelleNetz(baum: Prozessbaum) {
  vi.stubGlobal(
    "fetch",
    vi.fn((eingabe: RequestInfo | URL) => {
      const url = new URL(String(eingabe), "http://localhost");
      const rumpf = url.pathname === "/api/prozesse/baum" ? baum : undefined;
      if (rumpf === undefined) {
        return Promise.resolve(
          new Response(JSON.stringify({ type: "about:blank", title: "Not Found", status: 404 }), {
            status: 404,
            headers: { "content-type": "application/problem+json" },
          }),
        );
      }
      return Promise.resolve(
        new Response(JSON.stringify(rumpf), {
          status: 200,
          headers: { "content-type": "application/json" },
        }),
      );
    }),
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

async function rendereMit(liveRest: LiveRest) {
  stelleNetz(prozessbaum(liveRest));
  const gerendert = await rendere(
    <NuqsTestingAdapter searchParams="?zeitraum=48H" hasMemory>
      <ProzessAnsicht />
    </NuqsTestingAdapter>,
  );
  await zuege();
  return gerendert;
}

/** Die Hinweiskästen im Baum — die Fehleranzeige trägt dieselbe Bauform, aber ihren eigenen Titel. */
function hinweise(behaelter: HTMLElement): string[] {
  return [...behaelter.querySelectorAll('[data-slot="alert"]')].map(
    (kasten) => kasten.textContent ?? "",
  );
}

describe("Der Hinweis zum Live-Rest", () => {
  it("steht bei AUSGESETZT mit Lauf, mit der Zeitangabe in der Anzeigezone", async () => {
    const gerendert = await rendereMit({ zustand: "AUSGESETZT", vollstaendigBis: G });
    try {
      const { behaelter } = gerendert;
      expect(behaelter.querySelector('[role="tree"]'), "Eichung: der Baum steht").not.toBeNull();

      const kaesten = hinweise(behaelter);
      expect(kaesten).toHaveLength(1);
      // Die Zeitangabe über denselben Weg wie die Ansicht: UTC ohne Provider (tests/hilfe/rendern.tsx).
      const { formatiereZeitpunkt } = await import("@/lib/format");
      const zeit = formatiereZeitpunkt(G, "de", undefined);
      expect(zeit).not.toBe("");
      expect(kaesten[0]).toContain(zeit);
      expect(kaesten[0]).toContain("Vollständig sind die Zahlen bis");
      expect(kaesten[0]).not.toContain(TEXTE.liveRest.ausgesetztOhneLauf);
    } finally {
      await gerendert.abbauen();
    }
  });

  it("steht bei AUSGESETZT ohne Lauf, ohne Zeitangabe", async () => {
    const gerendert = await rendereMit({ zustand: "AUSGESETZT", vollstaendigBis: null });
    try {
      const kaesten = hinweise(gerendert.behaelter);
      expect(kaesten).toHaveLength(1);
      expect(kaesten[0]).toBe(TEXTE.liveRest.ausgesetztOhneLauf);
    } finally {
      await gerendert.abbauen();
    }
  });

  it("steht bei ANGEWANDT nicht", async () => {
    const gerendert = await rendereMit({ zustand: "ANGEWANDT", vollstaendigBis: null });
    try {
      expect(gerendert.behaelter.querySelector('[role="tree"]'), "Eichung").not.toBeNull();
      expect(hinweise(gerendert.behaelter)).toEqual([]);
    } finally {
      await gerendert.abbauen();
    }
  });

  it("steht bei NICHT_NOETIG nicht", async () => {
    const gerendert = await rendereMit({ zustand: "NICHT_NOETIG", vollstaendigBis: null });
    try {
      expect(gerendert.behaelter.querySelector('[role="tree"]'), "Eichung").not.toBeNull();
      expect(hinweise(gerendert.behaelter)).toEqual([]);
    } finally {
      await gerendert.abbauen();
    }
  });

  /** Kein Rot: Der Kasten trägt nicht die Variante der Fehleranzeige. */
  it("ist kein Fehler — die Fehlerfarbe steht nicht am Kasten", async () => {
    const gerendert = await rendereMit({ zustand: "AUSGESETZT", vollstaendigBis: null });
    try {
      const kasten = gerendert.behaelter.querySelector('[data-slot="alert"]');
      expect(kasten).not.toBeNull();
      expect(kasten?.className).not.toContain("destructive");
    } finally {
      await gerendert.abbauen();
    }
  });
});

/* ─── Die Übersicht (Teil B) ─────────────────────────────────────────────── */

function dashboard(liveRest: LiveRest, leer = false): Dashboard {
  return {
    zeitraum: "48H",
    fenster: { von: "2025-12-28T14:00:00Z", bis: "2025-12-30T14:00:00Z" },
    leer,
    verlauf: leer
      ? []
      : [
          {
            eimer: "2025-12-30T13:00:00Z",
            gesamt: 2,
            einordnungen: [{ einordnung: "ABGESCHLOSSEN", anzahl: 2 }],
          },
        ],
    kacheln: {
      nachrichten: leer ? 0 : 2,
      fehler: { anzahl: 0, arten: [] },
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
    fehlerLive: { zustand: "ANGEWANDT" },
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

async function rendereUebersicht(liveRest: LiveRest, leer = false) {
  stelleUebersicht(dashboard(liveRest, leer));
  const gerendert = await rendere(
    <NuqsTestingAdapter searchParams="?zeitraum=48H" hasMemory>
      <DashboardAnsicht />
    </NuqsTestingAdapter>,
  );
  await zuege();
  return gerendert;
}

/** Die Eichung: Die Kachelreihe steht, die Antwort ist also angekommen. */
function kachelnStehen(behaelter: HTMLElement): boolean {
  // Jede Kachel ist eine Card; der Leerzustand ist keine (`components/zustand.tsx`).
  return behaelter.querySelector('[data-slot="card"]') !== null;
}

describe("Der Hinweis zum Live-Rest in der Übersicht", () => {
  it("steht bei AUSGESETZT mit Lauf über den Kacheln, mit der Zeitangabe", async () => {
    const gerendert = await rendereUebersicht({ zustand: "AUSGESETZT", vollstaendigBis: G });
    try {
      const { behaelter } = gerendert;
      expect(kachelnStehen(behaelter), "Eichung: die Kacheln stehen").toBe(true);

      const kaesten = hinweise(behaelter);
      expect(kaesten).toHaveLength(1);
      const { formatiereZeitpunkt } = await import("@/lib/format");
      expect(kaesten[0]).toContain(formatiereZeitpunkt(G, "de", undefined));

      // Über den Kacheln: Der Kasten kommt im Dokument vor der ersten Kachel.
      const kasten = behaelter.querySelector('[data-slot="alert"]');
      const kachel = behaelter.querySelector('[data-slot="card"]');
      expect(kasten).not.toBeNull();
      expect(kachel).not.toBeNull();
      expect(
        kasten!.compareDocumentPosition(kachel!) & Node.DOCUMENT_POSITION_FOLLOWING,
      ).toBeTruthy();
    } finally {
      await gerendert.abbauen();
    }
  });

  it("steht bei AUSGESETZT ohne Lauf auch im Leerzustand", async () => {
    const gerendert = await rendereUebersicht(
      { zustand: "AUSGESETZT", vollstaendigBis: null },
      true,
    );
    try {
      const { behaelter } = gerendert;
      expect(behaelter.textContent, "Eichung: der Leerzustand steht").toContain(
        TEXTE.dashboard.leerTitel,
      );
      expect(kachelnStehen(behaelter), "Gegenprobe: keine Kacheln im Leerzustand").toBe(false);
      expect(hinweise(behaelter)).toEqual([TEXTE.liveRest.ausgesetztOhneLauf]);
    } finally {
      await gerendert.abbauen();
    }
  });

  it("steht bei ANGEWANDT nicht", async () => {
    const gerendert = await rendereUebersicht({ zustand: "ANGEWANDT", vollstaendigBis: null });
    try {
      expect(kachelnStehen(gerendert.behaelter), "Eichung").toBe(true);
      expect(hinweise(gerendert.behaelter)).toEqual([]);
    } finally {
      await gerendert.abbauen();
    }
  });

  it("steht bei NICHT_NOETIG nicht", async () => {
    const gerendert = await rendereUebersicht({ zustand: "NICHT_NOETIG", vollstaendigBis: null });
    try {
      expect(kachelnStehen(gerendert.behaelter), "Eichung").toBe(true);
      expect(hinweise(gerendert.behaelter)).toEqual([]);
    } finally {
      await gerendert.abbauen();
    }
  });
});
