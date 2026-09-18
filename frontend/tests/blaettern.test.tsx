// @vitest-environment jsdom

import { act } from "react";
import { NuqsTestingAdapter } from "nuqs/adapters/testing";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import type { Nachricht, Prozessbaum, Seite } from "@/features/nachrichten/api";
import { Blaettern } from "@/features/nachrichten/components/blaettern";
import { NachrichtenAnsicht } from "@/features/nachrichten/components/nachrichten-ansicht";
import { ProzessAnsicht } from "@/features/nachrichten/components/prozessansicht";
import { texteFuer } from "@/i18n";

import { rendere } from "./hilfe/rendern";

/**
 * **Der Σ unter der Liste — die Fälle, die nur ein gerenderter Baum trägt**
 * (`docs/nachrichtenliste.md` §8.3, E‑216).
 *
 * Die Rechnung ist eine reine Funktion und steht in
 * `tests/aktualisierung.test.ts`. **Hier steht, wie der Σ aussieht und dass die
 * Ansichten ihn abfragen:**
 *
 * | Fall | Warum ein Baum |
 * |---|---|
 * | der Block | Das Zeichen ist `aria-hidden`, „Treffer:" steht nur für Vorleseprogramme, der Hinweis im `title` nur bei „mehr als", und der Σ steht **in keinem** `aria-live`-Bereich — Eigenschaften von Elementen. Ohne Angabe steht **kein** Σ (Abwesenheit) |
 * | die Zustände | Der Blätterblock steht in der Nachrichtenliste in allen vier Zuständen, der Σ nur im Datenzustand: Abwesenheit im Leer-, Fehler- und Ladezustand, je mit dem Blätterblock als Eichung. Fehler und Laden sind so gestellt, dass eine Seite im Zwischenspeicher liegt — sonst fehlte der Σ schon mangels Zahl |
 * | die Verdrahtung | Vor, Zurück, Filterwechsel und „Neu laden" führen die Zahl mit; bei einer Rückmeldung am Feld bleibt sie mit der Liste stehen; die Übertragungsliste der Prozessansicht trägt sie mit |
 *
 * **Antwortrümpfe gestellt, kein Netz, keine Datenbank.** Zugesichert wird, was
 * im Baum steht, nie eine Zeit (Regel T1).
 */

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: () => {}, replace: () => {} }),
  usePathname: () => "/",
}));

const TEXTE = texteFuer("de");
const B = TEXTE.nachrichten.blaettern;

/** Jede Anfrage, in der Reihenfolge, in der sie hinausging. */
let anfragen: string[] = [];

type Weiche = (pfad: string, parameter: URLSearchParams) => Response | Promise<Response>;

/** Ein `fetch`, der mitschreibt und die Antwort der Weiche überlässt. */
function stelleNetz(weiche: Weiche) {
  vi.stubGlobal(
    "fetch",
    vi.fn((eingabe: RequestInfo | URL) => {
      const adresse = String(eingabe);
      anfragen.push(adresse);
      const url = new URL(adresse, "http://localhost");
      return Promise.resolve(weiche(url.pathname, url.searchParams));
    }),
  );
}

function json(rumpf: unknown): Response {
  return new Response(JSON.stringify(rumpf), {
    status: 200,
    headers: { "content-type": "application/json" },
  });
}

function problem(status: number, typ: string): Response {
  return new Response(
    JSON.stringify({ type: `https://overlord.example/problem/${typ}`, title: typ, status }),
    { status, headers: { "content-type": "application/problem+json" } },
  );
}

/** Eine Antwort, die erst auf `freigeben()` ankommt — die Anfrage ist gestellt. */
function zurueckgehalten(antwort: () => Response) {
  let freigeben: () => void = () => {};
  const versprochen = new Promise<Response>((fertig) => {
    freigeben = () => fertig(antwort());
  });
  return { versprochen, freigeben: () => freigeben() };
}

beforeEach(() => {
  anfragen = [];
});

afterEach(() => {
  vi.unstubAllGlobals();
});

/** Einige Züge durch die Warteschlange — gezählt, nicht gewartet (T1). */
async function zuege(anzahl = 12): Promise<void> {
  for (let zug = 0; zug < anzahl; zug++) {
    await act(async () => {
      await new Promise((fertig) => setTimeout(fertig, 0));
    });
  }
}

async function klicke(element: Element): Promise<void> {
  await act(async () => {
    element.dispatchEvent(new MouseEvent("click", { bubbles: true }));
  });
  await zuege();
}

function knopfMitText(behaelter: HTMLElement, text: string): HTMLButtonElement {
  const treffer = [...behaelter.querySelectorAll("button")].find((knopf) =>
    (knopf.textContent ?? "").includes(text),
  );
  expect(treffer, `Schaltfläche ${text}`).toBeDefined();
  return treffer as HTMLButtonElement;
}

/** Der Σ: die Spanne, deren vorgelesener Teil „Treffer:" lautet — `null`, wenn keiner dasteht. */
function summe(behaelter: HTMLElement): HTMLElement | null {
  const vorgelesen = [...behaelter.querySelectorAll(".sr-only")].find(
    (spanne) => spanne.textContent === B.treffer,
  );
  return vorgelesen?.parentElement ?? null;
}

function summeDa(behaelter: HTMLElement): HTMLElement {
  const gefunden = summe(behaelter);
  expect(gefunden, "der Σ").not.toBeNull();
  return gefunden as HTMLElement;
}

/** Was vom Σ zu sehen ist: sein Text ohne den nur vorgelesenen Teil. */
function sichtbar(element: HTMLElement): string {
  const kopie = element.cloneNode(true) as HTMLElement;
  kopie.querySelectorAll(".sr-only").forEach((spanne) => spanne.remove());
  return (kopie.textContent ?? "").trim();
}

/** Eichung für jede Abwesenheit: Der Blätterblock selbst steht da. */
function blaetterblockSteht(behaelter: HTMLElement): void {
  knopfMitText(behaelter, B.vor);
  knopfMitText(behaelter, B.zurueck);
}

/* ─── die Rümpfe ─────────────────────────────────────────────────────────── */

function zeile(messageId: string): Nachricht {
  return {
    messageId,
    zeitpunkt: "2025-12-29T10:01:09Z",
    status: "FINISHED",
    statusKind: "ABGESCHLOSSEN",
    bedeutungNichtVerifiziert: false,
    processId: "P-0815",
    processName: "Versand Einzel IDOC",
    projectName: "Versand",
    sosName: "Versand Einzel IDOC aus Split",
    schritt: null,
  };
}

/** Seite eins: drei Zeilen und eine weitere Seite. Seite zwei: zwei Zeilen, die letzte. */
function listenseite(parameter: URLSearchParams): Seite<Nachricht> {
  return parameter.get("cursor") === null
    ? { items: ["a-1", "a-2", "a-3"].map(zeile), nextCursor: "cursor-2", hasMore: true }
    : { items: ["b-1", "b-2"].map(zeile), nextCursor: null, hasMore: false };
}

/** Die Liste und sonst nichts — alles andere bekommt ein `404`. */
function listenNetz(pfad: string, parameter: URLSearchParams): Response {
  return pfad === "/api/nachrichten"
    ? json(listenseite(parameter))
    : problem(404, "nicht-gefunden");
}

function prozessbaum(): Prozessbaum {
  return {
    zeitraum: "48H",
    gliederung: "PARTNER",
    fenster: { von: "2025-12-28T05:00:00Z", bis: "2025-12-30T05:00:00Z" },
    stilleSchwelleMonate: 3,
    liveRest: { zustand: "NICHT_NOETIG", vollstaendigBis: null },
    fehlerLive: { zustand: "ANGEWANDT" },
    gesamt: { anzahlProzesse: 1, bewegt: 1, still: 0, nie: 0, nachrichten: 5, fehler: 0 },
    ebenen: ["PARTNER", "PROZESS"],
    knoten: [
      {
        schluessel: "BMW",
        name: "BMW",
        anzahlProzesse: 1,
        nachrichten: 5,
        fehler: 0,
        kinder: [
          {
            schluessel: "P-0815",
            name: "Versand Einzel IDOC",
            processId: "P-0815",
            nachrichten: 5,
            fehler: 0,
            letzteBewegung: "2025-12-29T10:01:09Z",
            zustand: "BEWEGT",
          },
        ],
      },
    ],
  };
}

/* ─── der Block ──────────────────────────────────────────────────────────── */

describe("Der Blätterblock", () => {
  function rendereBlock(treffer: { anzahl: number; genau: boolean } | undefined) {
    return rendere(
      <Blaettern
        kannZurueck
        kannVor={false}
        aufZurueck={() => {}}
        aufVor={() => {}}
        standVon={0}
        laeuft={false}
        treffer={treffer}
      />,
    );
  }

  it("zeigt auf der letzten Seite die genaue Zahl, das Zeichen verborgen, nichts in aria-live", async () => {
    const gerendert = await rendereBlock({ anzahl: 1234, genau: true });
    try {
      const sigma = summeDa(gerendert.behaelter);
      // Die Zahl über `lib/format.ts`, in der aktiven Sprache.
      expect(sichtbar(sigma)).toBe("1.234");
      expect(sigma.textContent, "vorgelesen").toBe("Treffer: 1.234");
      const zeichen = sigma.querySelector("svg");
      expect(zeichen?.getAttribute("aria-hidden")).toBe("true");
      expect(zeichen?.getAttribute("class")).toContain("lucide-sigma");
      expect(sigma.hasAttribute("title")).toBe(false);
      expect(sigma.className).toContain("tabular-nums");
      // Sonst läse ein Vorleseprogramm jede Aktualisierung vor.
      expect(sigma.closest("[aria-live]")).toBeNull();
    } finally {
      await gerendert.abbauen();
    }
  });

  it("zeigt vor der letzten Seite mehr als, mit dem Hinweis im title", async () => {
    const gerendert = await rendereBlock({ anzahl: 50, genau: false });
    try {
      const sigma = summeDa(gerendert.behaelter);
      expect(sichtbar(sigma)).toBe("mehr als 50");
      expect(sigma.textContent, "vorgelesen").toBe("Treffer: mehr als 50");
      expect(sigma.getAttribute("title")).toBe(B.trefferMehrAlsHinweis);
      expect(sigma.closest("[aria-live]")).toBeNull();
    } finally {
      await gerendert.abbauen();
    }
  });

  it("zeigt ohne Angabe keinen Σ, Stand und Pfeile aber schon", async () => {
    const gerendert = await rendereBlock(undefined);
    try {
      const { behaelter } = gerendert;
      expect(summe(behaelter)).toBeNull();
      expect(behaelter.querySelector(".lucide-sigma")).toBeNull();
      expect(behaelter.textContent).toContain(TEXTE.nachrichten.aktualisierung.standUnbekannt);
      blaetterblockSteht(behaelter);
    } finally {
      await gerendert.abbauen();
    }
  });
});

/* ─── die Nachrichtenliste ───────────────────────────────────────────────── */

describe("Der Σ in der Nachrichtenliste", () => {
  async function rendereListe(weiche: Weiche = listenNetz, suchparameter = "") {
    stelleNetz(weiche);
    const gerendert = await rendere(
      <NuqsTestingAdapter searchParams={suchparameter} hasMemory>
        <NachrichtenAnsicht />
      </NuqsTestingAdapter>,
    );
    await zuege();
    return gerendert;
  }

  it("zählt beim Blättern mit und zeigt nach Zurück wieder die Zahl bis dort", async () => {
    const gerendert = await rendereListe();
    try {
      const { behaelter } = gerendert;
      expect(sichtbar(summeDa(behaelter)), "Seite eins").toBe("mehr als 3");

      await klicke(knopfMitText(behaelter, B.vor));
      expect(sichtbar(summeDa(behaelter)), "Seite zwei, die letzte").toBe("5");
      expect(summeDa(behaelter).hasAttribute("title")).toBe(false);

      await klicke(knopfMitText(behaelter, B.zurueck));
      expect(sichtbar(summeDa(behaelter)), "zurück auf Seite eins").toBe("mehr als 3");
    } finally {
      await gerendert.abbauen();
    }
  });

  it("zeigt im Leerzustand keinen Σ", async () => {
    const gerendert = await rendereListe((pfad) =>
      pfad === "/api/nachrichten"
        ? json({ items: [], nextCursor: null, hasMore: false })
        : problem(404, "nicht-gefunden"),
    );
    try {
      const { behaelter } = gerendert;
      expect(behaelter.textContent, "Eichung: der Leerzustand").toContain(
        TEXTE.nachrichten.leer.titel,
      );
      blaetterblockSteht(behaelter);
      expect(summe(behaelter)).toBeNull();
    } finally {
      await gerendert.abbauen();
    }
  });

  it("zeigt im Fehlerzustand keinen Σ, obwohl die Seite im Zwischenspeicher steht", async () => {
    let scheitert = false;
    const gerendert = await rendereListe((pfad, parameter) =>
      scheitert ? problem(500, "technischer-fehler") : listenNetz(pfad, parameter),
    );
    try {
      const { behaelter } = gerendert;
      expect(sichtbar(summeDa(behaelter)), "Eichung: vorher mit Σ").toBe("mehr als 3");

      scheitert = true;
      const knopf = behaelter.querySelector(`[aria-label="${TEXTE.neuLaden.nachrichten}"]`);
      expect(knopf, "Neu laden").not.toBeNull();
      await klicke(knopf as Element);

      expect(anfragen.at(-1), "Eichung: die gescheiterte Anfrage").toBe("/api/nachrichten");
      expect(
        behaelter.querySelector('[data-slot="alert"]'),
        "Eichung: der Fehlerzustand",
      ).not.toBeNull();
      blaetterblockSteht(behaelter);
      expect(summe(behaelter)).toBeNull();
    } finally {
      await gerendert.abbauen();
    }
  });

  it("zeigt beim Laden der nächsten Seite keinen Σ, auch nicht den der vorigen", async () => {
    const seiteZwei = zurueckgehalten(() =>
      json(listenseite(new URLSearchParams("cursor=cursor-2"))),
    );
    const gerendert = await rendereListe((pfad, parameter) =>
      pfad === "/api/nachrichten" && parameter.get("cursor") === "cursor-2"
        ? seiteZwei.versprochen
        : listenNetz(pfad, parameter),
    );
    try {
      const { behaelter } = gerendert;
      expect(sichtbar(summeDa(behaelter)), "Eichung: Seite eins mit Σ").toBe("mehr als 3");

      await klicke(knopfMitText(behaelter, B.vor));
      expect(
        behaelter.querySelector('div[aria-busy="true"]'),
        "Eichung: der Ladezustand",
      ).not.toBeNull();
      blaetterblockSteht(behaelter);
      expect(summe(behaelter)).toBeNull();

      await act(async () => seiteZwei.freigeben());
      await zuege();
      expect(sichtbar(summeDa(behaelter)), "nach der Antwort").toBe("5");
    } finally {
      await gerendert.abbauen();
    }
  });

  it("beginnt nach einem Filterwechsel wieder bei der ersten Seite", async () => {
    const gerendert = await rendereListe();
    try {
      const { behaelter } = gerendert;
      await klicke(knopfMitText(behaelter, B.vor));
      expect(sichtbar(summeDa(behaelter)), "Seite zwei").toBe("5");

      const zeitfenster = behaelter.querySelector(
        `[data-slot="toggle-group"][aria-label="${TEXTE.nachrichten.zeitfenster.bezeichnung}"]`,
      );
      expect(zeitfenster, "Zeitfensterumschalter").not.toBeNull();
      await klicke(knopfMitText(zeitfenster as HTMLElement, TEXTE.nachrichten.zeitfenster.d7));

      expect(anfragen.at(-1), "Eichung: Seite eins im neuen Fenster").toBe(
        "/api/nachrichten?zeitraum=7d",
      );
      expect(sichtbar(summeDa(behaelter))).toBe("mehr als 3");
    } finally {
      await gerendert.abbauen();
    }
  });

  it("hält beim Neu laden die alte Zahl, bis Seite eins da ist (E‑168)", async () => {
    let seiteEins: ReturnType<typeof zurueckgehalten> | undefined;
    const gerendert = await rendereListe((pfad, parameter) => {
      if (pfad === "/api/nachrichten" && parameter.get("cursor") === null && seiteEins) {
        return seiteEins.versprochen;
      }
      return listenNetz(pfad, parameter);
    });
    try {
      const { behaelter } = gerendert;
      await klicke(knopfMitText(behaelter, B.vor));
      expect(sichtbar(summeDa(behaelter)), "Seite zwei").toBe("5");

      seiteEins = zurueckgehalten(() => json(listenseite(new URLSearchParams())));
      const knopf = behaelter.querySelector(`[aria-label="${TEXTE.neuLaden.nachrichten}"]`);
      await klicke(knopf as Element);
      expect(anfragen.at(-1), "Eichung: Seite eins ist angefragt").toBe("/api/nachrichten");
      expect(sichtbar(summeDa(behaelter)), "solange Seite eins aussteht").toBe("5");

      await act(async () => seiteEins?.freigeben());
      await zuege();
      expect(sichtbar(summeDa(behaelter)), "danach").toBe("mehr als 3");
    } finally {
      await gerendert.abbauen();
    }
  });

  it("lässt die Zahl mit der Liste stehen, wenn die Rückmeldung einem Feld gilt", async () => {
    // Ein Suchbegriff läuft im größeren Fenster in die Zeitgrenze der Datenbank:
    // `suche-abgebrochen` gehört an das Suchfeld, die Liste bleibt, wie sie war.
    const gerendert = await rendereListe(
      (pfad, parameter) =>
        pfad === "/api/nachrichten" && parameter.get("zeitraum") === "30d"
          ? problem(400, "suche-abgebrochen")
          : listenNetz(pfad, parameter),
      "?suche=lieferschein",
    );
    try {
      const { behaelter } = gerendert;
      await klicke(knopfMitText(behaelter, B.vor));
      expect(sichtbar(summeDa(behaelter)), "Seite zwei").toBe("5");

      const zeitfenster = behaelter.querySelector(
        `[data-slot="toggle-group"][aria-label="${TEXTE.nachrichten.zeitfenster.bezeichnung}"]`,
      );
      await klicke(knopfMitText(zeitfenster as HTMLElement, TEXTE.nachrichten.zeitfenster.d30));

      expect(anfragen.at(-1), "Eichung: die abgebrochene Suche").toContain("zeitraum=30d");
      expect(
        behaelter.querySelectorAll("tbody tr"),
        "Eichung: die zwei Zeilen von Seite zwei stehen",
      ).toHaveLength(2);
      expect(sichtbar(summeDa(behaelter))).toBe("5");
    } finally {
      await gerendert.abbauen();
    }
  });
});

/* ─── die Prozessansicht ─────────────────────────────────────────────────── */

describe("Der Σ in der Prozessansicht", () => {
  it("steht unter der Übertragungsliste, wie in der Nachrichtenliste", async () => {
    stelleNetz((pfad, parameter) =>
      pfad === "/api/prozesse/baum" ? json(prozessbaum()) : listenNetz(pfad, parameter),
    );
    const gerendert = await rendere(
      <NuqsTestingAdapter searchParams="?zeitraum=48H&prozess=P-0815" hasMemory>
        <ProzessAnsicht />
      </NuqsTestingAdapter>,
    );
    try {
      await zuege();
      const { behaelter } = gerendert;
      expect(
        behaelter.querySelectorAll("tbody tr"),
        "Eichung: die drei Zeilen der Übertragungsliste stehen",
      ).toHaveLength(3);
      expect(sichtbar(summeDa(behaelter))).toBe("mehr als 3");
    } finally {
      await gerendert.abbauen();
    }
  });
});
