// @vitest-environment jsdom

import { act } from "react";
import { NuqsTestingAdapter } from "nuqs/adapters/testing";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { NeuLaden } from "@/components/neu-laden";
import type { Dashboard } from "@/features/dashboard/api";
import { DashboardAnsicht } from "@/features/dashboard/components/dashboard-ansicht";
import type { Nachricht, Nachrichtendetail, Prozessbaum, Seite } from "@/features/nachrichten/api";
import { NachrichtenAnsicht } from "@/features/nachrichten/components/nachrichten-ansicht";
import { ProzessAnsicht } from "@/features/nachrichten/components/prozessansicht";
import { texteFuer } from "@/i18n";

import { rendere } from "./hilfe/rendern";

/**
 * **„Neu laden" und die automatische Aktualisierung — die Fälle, die nur ein
 * gerenderter Baum trägt** (`docs/neu-laden.md`).
 *
 * Die Regeln selbst sind reine Funktionen und stehen in
 * `tests/aktualisierung.test.ts`: wann das Intervall fragt, was der Schalter
 * zeigt, wohin „Neu laden" führt, wann der Verlauf aufbaut. **Hier steht, dass
 * jemand sie abfragt** — und das ist fast durchweg eine Aussage über
 * **Anfragen, die hinausgehen oder nicht hinausgehen**:
 *
 * | Fall | Warum ein Baum |
 * |---|---|
 * | der Baustein | Ohne `automatik` **kein Schalter** (Abwesenheit); der Knopf zeigt **nur das Symbol** (E‑174); die drei Lagen über `aria-pressed` und das Symbol, und ein Klick beim Laden ruft nichts |
 * | die Stelle | Übersicht und Prozessansicht: der Knopf ist der **nächste Knopf nach dem letzten Zeitraum-Knopf**; Nachrichten: Schalter und Knopf sind die **letzten beiden** der Filterleiste (E‑173); Prozessansicht im freien Modus: der Knopf ist das **letzte Bedienelement des Kopfes**, hinter den Datumsfeldern (E‑175). Den Rand selbst rechnet jsdom nicht — gemessen in M183 und M184 |
 * | Übersicht | ein Klick, **genau eine** weitere Anfrage an dieselbe Adresse — auch im Leerzustand, wo der Knopf stehen bleibt (E‑p) |
 * | Nachrichten | auf Seite zwei: **eine** Anfrage für Seite eins ohne Cursor; bei offenem Panel **keine** an einen Detail- oder Dateiendpunkt (E‑169) |
 * | Nachrichten mit gestellter Uhr | Schalter an und 60 Sekunden: eine Anfrage; Seite zwei: keine; Schalter aus: keine |
 * | Prozessansicht | 120 Sekunden ohne Klick: **keine** Anfrage (E‑164); ein Klick: Baum, dann Liste ab Seite eins, sonst nichts; bringt der Baum ein neues Fenster, **keine** Listenanfrage mit dem alten |
 *
 * **Zugesichert werden Anfragen, nie Zeiten** (Regel T1). Die gestellte Uhr
 * (`vi.useFakeTimers`) ersetzt das Warten, sie misst nichts.
 *
 * **Antwortrümpfe gestellt, kein Netz, keine Datenbank.**
 */

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: () => {}, replace: () => {} }),
  usePathname: () => "/",
}));

const TEXTE = texteFuer("de");
const N = TEXTE.neuLaden;

const FENSTER = { von: "2025-12-28T05:00:00Z", bis: "2025-12-30T05:00:00Z" };
const MESSAGE_ID = "8f3a1c2e-0000-4000-8000-000000000001";

/* ─── das gestellte Netz ─────────────────────────────────────────────────── */

/** Jede Anfrage, in der Reihenfolge, in der sie hinausging. */
let anfragen: string[] = [];

/**
 * Ein `fetch`, der mitschreibt und nach Pfad antwortet. **Was die Weiche nicht
 * kennt, bekommt ein `404`** — es erscheint trotzdem in `anfragen`, und genau
 * darauf kommt es an.
 */
function stelleNetz(weiche: (pfad: string, parameter: URLSearchParams) => unknown) {
  vi.stubGlobal(
    "fetch",
    vi.fn((eingabe: RequestInfo | URL) => {
      const adresse = String(eingabe);
      anfragen.push(adresse);
      const url = new URL(adresse, "http://localhost");
      const rumpf = weiche(url.pathname, url.searchParams);
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

beforeEach(() => {
  anfragen = [];
});

afterEach(() => {
  vi.useRealTimers();
  vi.unstubAllGlobals();
});

/**
 * Einige Züge durch die Warteschlange — gezählt, nicht gewartet (T1). Unter der
 * gestellten Uhr rückt sie um 60 ms, damit auch die Drosselung der URL-Bindung
 * (`nuqs`, 50 ms) durchläuft; das ist ein Tausendstel des Intervalls.
 */
async function zuege(anzahl = 12): Promise<void> {
  for (let zug = 0; zug < anzahl; zug++) {
    await act(async () => {
      if (vi.isFakeTimers()) {
        await vi.advanceTimersByTimeAsync(5);
      } else {
        await new Promise((fertig) => setTimeout(fertig, 0));
      }
    });
  }
}

async function klicke(element: Element): Promise<void> {
  await act(async () => {
    element.dispatchEvent(new MouseEvent("click", { bubbles: true }));
  });
  await zuege();
}

function mitName(behaelter: HTMLElement, name: string): HTMLElement {
  const treffer = behaelter.querySelector<HTMLElement>(`[aria-label="${name}"]`);
  expect(treffer, `Element mit dem Namen ${name}`).not.toBeNull();
  return treffer as HTMLElement;
}

function knopfMitText(behaelter: HTMLElement, text: string): HTMLButtonElement {
  const treffer = [...behaelter.querySelectorAll("button")].find((knopf) =>
    (knopf.textContent ?? "").includes(text),
  );
  expect(treffer, `Schaltfläche ${text}`).toBeDefined();
  return treffer as HTMLButtonElement;
}

/**
 * **„Rechts daneben" heißt hier: der nächste Knopf im Dokument.** Die Zeilen
 * sind nicht umgekehrt (`flex-row`), also folgt die Reihenfolge auf dem
 * Bildschirm der im Baum. Wo in der Zeile das liegt, rechnet jsdom nicht aus.
 */
function knopfNach(behaelter: HTMLElement, knopf: Element): HTMLButtonElement | undefined {
  const knoepfe = [...behaelter.querySelectorAll("button")];
  return knoepfe[knoepfe.indexOf(knopf as HTMLButtonElement) + 1];
}

function letzterKnopfIn(behaelter: HTMLElement, umschalter: string): HTMLButtonElement {
  const gruppe = behaelter.querySelector(`[data-slot="toggle-group"][aria-label="${umschalter}"]`);
  expect(gruppe, `Umschalter ${umschalter}`).not.toBeNull();
  const knoepfe = [...(gruppe as Element).querySelectorAll("button")];
  expect(knoepfe.length, `Knöpfe im Umschalter ${umschalter}`).toBeGreaterThan(1);
  return knoepfe[knoepfe.length - 1];
}

/** Die Anfragen seit einem Stand — der Kern jeder Zusicherung hier. */
function seit(stand: number): string[] {
  return anfragen.slice(stand);
}

/* ─── die Rümpfe ─────────────────────────────────────────────────────────── */

function dashboard(): Dashboard {
  return {
    zeitraum: "30T",
    fenster: FENSTER,
    leer: false,
    verlauf: [
      {
        eimer: "2025-12-28T00:00:00Z",
        gesamt: 9,
        einordnungen: [
          { einordnung: "ABGESCHLOSSEN", anzahl: 8 },
          { einordnung: "FEHLER", anzahl: 1 },
        ],
      },
    ],
    kacheln: {
      nachrichten: 9,
      fehler: { anzahl: 1, arten: [] },
      laeuft: { anzahl: 0, aeltesteSekunden: null, ermittelbar: true },
      wartend: { anzahl: 0, aeltesteSekunden: null, ermittelbar: true },
    },
    verteilung: {
      partner: { zeilen: [{ art: "NICHT_ZUGEORDNET", wert: null, anzahl: 0, enthaltene: null }] },
      richtung: { zeilen: [{ art: "NICHT_ZUGEORDNET", wert: null, anzahl: 0, enthaltene: null }] },
    },
    zuletztAufgefallen: [],
    stand: { beendetAm: "2025-12-30T04:10:00Z", art: "VOLL" },
    liveRest: { zustand: "NICHT_NOETIG", vollstaendigBis: null },
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

/** Seite eins verweist auf Seite zwei, Seite zwei ist die letzte. */
function listenseite(parameter: URLSearchParams): Seite<Nachricht> {
  return parameter.get("cursor") === null
    ? { items: [zeile("zeile-1")], nextCursor: "cursor-2", hasMore: true }
    : { items: [zeile("zeile-2")], nextCursor: null, hasMore: false };
}

function detail(): Nachrichtendetail {
  return {
    messageId: MESSAGE_ID,
    status: "FINISHED",
    statusKind: "ABGESCHLOSSEN",
    processId: "P-0815",
    processName: "Versand Einzel IDOC",
    projectName: "Versand",
    sosName: "Versand Einzel IDOC aus Split",
    rollen: ["SPLIT_KIND"],
    zeitpunkt: "2025-12-29T10:01:09Z",
    start: "2025-12-29T10:00:00Z",
    gesamtdauerSekunden: 69,
    fristSekunden: null,
    eigenschaftenAnzahl: 3,
    bamAnzahl: 2,
    offenerZustand: "KEINER",
    naechsterSchritt: null,
    wartetSeitSekunden: null,
    schritte: [],
    kuratierteEigenschaften: [],
  };
}

function prozessbaum(): Prozessbaum {
  return {
    zeitraum: "48H",
    gliederung: "PARTNER",
    fenster: FENSTER,
    stilleSchwelleMonate: 3,
    liveRest: { zustand: "NICHT_NOETIG", vollstaendigBis: null },
    gesamt: { anzahlProzesse: 1, bewegt: 1, still: 0, nie: 0, nachrichten: 2, fehler: 0 },
    ebenen: ["PARTNER", "PROZESS"],
    knoten: [
      {
        schluessel: "BMW",
        name: "BMW",
        anzahlProzesse: 1,
        nachrichten: 2,
        fehler: 0,
        kinder: [
          {
            schluessel: "P-0815",
            name: "Versand Einzel IDOC",
            processId: "P-0815",
            nachrichten: 2,
            fehler: 0,
            letzteBewegung: "2025-12-29T10:01:09Z",
            zustand: "BEWEGT",
          },
        ],
      },
    ],
  };
}

/** Die Nachrichtenliste samt Detailpanel — alles andere bekommt ein `404`. */
function nachrichtenNetz(pfad: string, parameter: URLSearchParams): unknown {
  if (pfad === "/api/nachrichten") {
    return listenseite(parameter);
  }
  if (pfad === `/api/nachrichten/${MESSAGE_ID}`) {
    return detail();
  }
  return undefined;
}

/* ─── der Baustein ───────────────────────────────────────────────────────── */

describe("Der Baustein", () => {
  it("zeigt ohne automatik keinen Schalter, und der Knopf trägt nur das Symbol und den vollständigen Namen", async () => {
    const gerendert = await rendere(
      <NeuLaden name={N.uebersicht} laedt={false} aufNeuLaden={() => {}} />,
    );
    try {
      const { behaelter } = gerendert;
      expect(behaelter.querySelectorAll("button")).toHaveLength(1);
      expect(behaelter.querySelector("[aria-pressed]")).toBeNull();
      const knopf = mitName(behaelter, N.uebersicht);
      expect(knopf.getAttribute("title")).toBe(N.uebersicht);
      // Kein sichtbares Wort (E‑174): der Name steht nur am Knopf, gezeigt wird
      // allein das Symbol.
      expect(knopf.textContent).toBe("");
      expect(knopf.querySelectorAll('svg[aria-hidden="true"]')).toHaveLength(1);
    } finally {
      await gerendert.abbauen();
    }
  });

  it("unterscheidet aus, an und pausiert ohne Farbe und ruft beim Laden nichts", async () => {
    const aufrufe: string[] = [];
    const lagen = ["aus", "an", "pausiert"] as const;
    const symbole: string[] = [];

    for (const lage of lagen) {
      const gerendert = await rendere(
        <NeuLaden
          name={N.nachrichten}
          laedt
          aufNeuLaden={() => aufrufe.push("neu laden")}
          automatik={{ zustand: lage, aufUmschalten: (an) => aufrufe.push(`schalter ${an}`) }}
        />,
      );
      try {
        const { behaelter } = gerendert;
        const schalter = mitName(behaelter, N.automatik.name);
        expect(schalter.getAttribute("aria-pressed"), lage).toBe(lage === "aus" ? "false" : "true");
        expect(schalter.getAttribute("title")).toBe(N.automatik[lage]);
        expect(schalter.textContent).toBe(N.automatik.knopf);
        symbole.push(schalter.querySelector("svg")?.getAttribute("class") ?? "");

        const knopf = mitName(behaelter, N.nachrichten);
        expect(knopf.getAttribute("aria-busy")).toBe("true");
        await klicke(knopf);
      } finally {
        await gerendert.abbauen();
      }
    }

    // Drei Lagen, drei verschiedene Symbole — die Fläche des gedrückten
    // Zustands allein wäre die Farbe, die nicht allein sprechen darf.
    expect(new Set(symbole).size).toBe(3);
    expect(aufrufe, "Klick beim Laden").toEqual([]);
  });
});

/* ─── Übersicht ──────────────────────────────────────────────────────────── */

describe("Neu laden auf der Übersicht", () => {
  it("stellt genau eine weitere Anfrage an dieselbe Adresse", async () => {
    stelleNetz((pfad) => (pfad === "/api/dashboard" ? dashboard() : undefined));
    const gerendert = await rendere(
      <NuqsTestingAdapter searchParams="?zeitraum=30T" hasMemory>
        <DashboardAnsicht />
      </NuqsTestingAdapter>,
    );
    try {
      await zuege();
      expect(anfragen).toEqual(["/api/dashboard?zeitraum=30T"]);

      const stand = anfragen.length;
      await klicke(mitName(gerendert.behaelter, N.uebersicht));

      expect(seit(stand)).toEqual(["/api/dashboard?zeitraum=30T"]);
    } finally {
      await gerendert.abbauen();
    }
  });

  it("steht unmittelbar rechts neben dem letzten Zeitraum-Knopf", async () => {
    stelleNetz((pfad) => (pfad === "/api/dashboard" ? dashboard() : undefined));
    const gerendert = await rendere(
      <NuqsTestingAdapter searchParams="?zeitraum=30T" hasMemory>
        <DashboardAnsicht />
      </NuqsTestingAdapter>,
    );
    try {
      await zuege();
      const { behaelter } = gerendert;
      const letzter = letzterKnopfIn(behaelter, TEXTE.zeitraum.bezeichnung);
      expect(knopfNach(behaelter, letzter)).toBe(mitName(behaelter, N.uebersicht));
    } finally {
      await gerendert.abbauen();
    }
  });

  /**
   * **E‑p, ergänzt am 16.09.2026:** Im Leerzustand stehen der Satz, der
   * Umschalter — und „Neu laden", bedienbar. Die Eichung ist der Satz selbst:
   * Ohne ihn wäre nicht gezeigt, dass wirklich der Leerzustand steht.
   */
  it("steht im Leerzustand und lädt dort neu", async () => {
    stelleNetz((pfad) => (pfad === "/api/dashboard" ? { ...dashboard(), leer: true } : undefined));
    const gerendert = await rendere(
      <NuqsTestingAdapter hasMemory>
        <DashboardAnsicht />
      </NuqsTestingAdapter>,
    );
    try {
      await zuege();
      expect(gerendert.behaelter.textContent).toContain(TEXTE.dashboard.leerTitel);

      const stand = anfragen.length;
      await klicke(mitName(gerendert.behaelter, N.uebersicht));

      expect(seit(stand)).toEqual(["/api/dashboard"]);
      expect(gerendert.behaelter.textContent).toContain(TEXTE.dashboard.leerTitel);
    } finally {
      await gerendert.abbauen();
    }
  });
});

/* ─── Nachrichten ────────────────────────────────────────────────────────── */

describe("Neu laden in den Nachrichten", () => {
  async function rendereListe(suchparameter = "") {
    stelleNetz(nachrichtenNetz);
    const gerendert = await rendere(
      <NuqsTestingAdapter searchParams={suchparameter} hasMemory>
        <NachrichtenAnsicht />
      </NuqsTestingAdapter>,
    );
    await zuege();
    return gerendert;
  }

  async function aufSeiteZwei(behaelter: HTMLElement): Promise<void> {
    const stand = anfragen.length;
    await klicke(knopfMitText(behaelter, TEXTE.nachrichten.blaettern.vor));
    expect(seit(stand), "Blättern auf Seite zwei").toEqual(["/api/nachrichten?cursor=cursor-2"]);
  }

  /**
   * **E‑173:** Schalter und Knopf stehen hinter allen Filtern, der Knopf außen.
   * Die Filterleiste ist der kleinste Kasten, der Zeitfenster und Knopf
   * umschließt — gesucht über den Baum und nicht über eine Klasse.
   */
  it("stellt Schalter und Knopf als letzte in die Filterleiste, den Knopf außen", async () => {
    const gerendert = await rendereListe();
    try {
      const { behaelter } = gerendert;
      const knopf = mitName(behaelter, N.nachrichten);
      const zeitfenster = behaelter.querySelector(
        `[data-slot="toggle-group"][aria-label="${TEXTE.nachrichten.zeitfenster.bezeichnung}"]`,
      );
      expect(zeitfenster, "Eichung: das Zeitfenster steht").not.toBeNull();

      let leiste: Element | null = knopf.parentElement;
      while (leiste !== null && !leiste.contains(zeitfenster)) {
        leiste = leiste.parentElement;
      }
      expect(leiste, "Filterleiste").not.toBeNull();
      expect(
        leiste?.querySelector('input[type="search"]'),
        "Eichung: die Suche steht darin",
      ).not.toBeNull();

      const knoepfe = [...(leiste as Element).querySelectorAll("button")];
      expect(knoepfe.at(-1)).toBe(knopf);
      expect(knoepfe.at(-2)).toBe(mitName(behaelter, N.automatik.name));
    } finally {
      await gerendert.abbauen();
    }
  });

  it("führt von Seite zwei mit einer Anfrage ohne Cursor zurück auf Seite eins", async () => {
    const gerendert = await rendereListe();
    try {
      const { behaelter } = gerendert;
      await aufSeiteZwei(behaelter);

      const stand = anfragen.length;
      await klicke(mitName(behaelter, N.nachrichten));

      expect(seit(stand)).toEqual(["/api/nachrichten"]);
      // Und die Liste steht danach wirklich auf Seite eins: Zurück ist gesperrt.
      expect(knopfMitText(behaelter, TEXTE.nachrichten.blaettern.zurueck).disabled).toBe(true);
    } finally {
      await gerendert.abbauen();
    }
  });

  /**
   * **E‑169, eine Aussage über Abwesenheit.** Das Panel ist offen und hat seine
   * Anfragen gestellt; nach dem Klick geht **nur** die Liste hinaus — kein
   * Detail, keine Kette, keine Eigenschaften, keine Belegdaten, keine Datei.
   * Die Eichung steckt im Fall selbst: Das Panel **hat** vorher gefragt, der
   * Zähler sieht es also.
   */
  it("fragt bei offenem Panel keinen Detail- oder Dateiendpunkt an", async () => {
    const gerendert = await rendereListe(`?nachricht=${MESSAGE_ID}`);
    try {
      const { behaelter } = gerendert;
      expect(
        anfragen.filter((adresse) => adresse.startsWith(`/api/nachrichten/${MESSAGE_ID}`)),
        "Eichung: das Panel hat angefragt",
      ).not.toEqual([]);

      await aufSeiteZwei(behaelter);
      const stand = anfragen.length;
      await klicke(mitName(behaelter, N.nachrichten));

      expect(seit(stand)).toEqual(["/api/nachrichten"]);
    } finally {
      await gerendert.abbauen();
    }
  });
});

describe("Die automatische Aktualisierung, mit gestellter Uhr", () => {
  async function rendereMitUhr() {
    vi.useFakeTimers();
    stelleNetz(nachrichtenNetz);
    const gerendert = await rendere(
      <NuqsTestingAdapter hasMemory>
        <NachrichtenAnsicht />
      </NuqsTestingAdapter>,
    );
    await zuege();
    return gerendert;
  }

  async function sechzigSekunden(): Promise<void> {
    await act(async () => {
      await vi.advanceTimersByTimeAsync(60_000);
    });
    await zuege();
  }

  it("ist nach dem Laden aus und fragt dann nicht", async () => {
    const gerendert = await rendereMitUhr();
    try {
      const schalter = mitName(gerendert.behaelter, N.automatik.name);
      expect(schalter.getAttribute("aria-pressed")).toBe("false");

      const stand = anfragen.length;
      await sechzigSekunden();
      await sechzigSekunden();
      expect(seit(stand)).toEqual([]);
    } finally {
      await gerendert.abbauen();
    }
  });

  it("fragt eingeschaltet nach 60 Sekunden Seite eins an", async () => {
    const gerendert = await rendereMitUhr();
    try {
      const schalter = mitName(gerendert.behaelter, N.automatik.name);
      await klicke(schalter);
      expect(schalter.getAttribute("aria-pressed")).toBe("true");
      expect(schalter.getAttribute("title")).toBe(N.automatik.an);

      const stand = anfragen.length;
      await sechzigSekunden();
      expect(seit(stand)).toEqual(["/api/nachrichten"]);
    } finally {
      await gerendert.abbauen();
    }
  });

  it("pausiert auf Seite zwei, zeigt es am Schalter und fragt nicht", async () => {
    const gerendert = await rendereMitUhr();
    try {
      const { behaelter } = gerendert;
      const schalter = mitName(behaelter, N.automatik.name);
      await klicke(schalter);
      await klicke(knopfMitText(behaelter, TEXTE.nachrichten.blaettern.vor));
      expect(anfragen.at(-1)).toBe("/api/nachrichten?cursor=cursor-2");
      expect(schalter.getAttribute("aria-pressed")).toBe("true");
      expect(schalter.getAttribute("title")).toBe(N.automatik.pausiert);

      const stand = anfragen.length;
      await sechzigSekunden();
      await sechzigSekunden();
      expect(seit(stand)).toEqual([]);

      // Neu laden führt zurück auf Seite eins — und dort läuft sie wieder.
      await klicke(mitName(behaelter, N.nachrichten));
      expect(schalter.getAttribute("title")).toBe(N.automatik.an);
      const nachNeuLaden = anfragen.length;
      await sechzigSekunden();
      expect(seit(nachNeuLaden)).toEqual(["/api/nachrichten"]);
    } finally {
      await gerendert.abbauen();
    }
  });
});

/* ─── Prozessansicht ─────────────────────────────────────────────────────── */

describe("Neu laden in der Prozessansicht", () => {
  const BAUM = "/api/prozesse/baum?zeitraum=48H";
  const LISTE =
    "/api/nachrichten?von=2025-12-28T05%3A00%3A00.000Z&bis=2025-12-30T05%3A00%3A00.000Z&prozess=P-0815";

  function prozessNetz(pfad: string, parameter: URLSearchParams): unknown {
    if (pfad === "/api/prozesse/baum") {
      return prozessbaum();
    }
    if (pfad === "/api/nachrichten") {
      return listenseite(parameter);
    }
    return undefined;
  }

  async function rendereProzesse() {
    stelleNetz(prozessNetz);
    const gerendert = await rendere(
      <NuqsTestingAdapter searchParams="?zeitraum=48H&prozess=P-0815" hasMemory>
        <ProzessAnsicht />
      </NuqsTestingAdapter>,
    );
    await zuege();
    return gerendert;
  }

  it("aktualisiert ohne Klick nichts, auch nicht nach zwei Minuten", async () => {
    vi.useFakeTimers();
    const gerendert = await rendereProzesse();
    try {
      expect(anfragen, "Eichung: Baum und Liste sind geladen").toEqual([BAUM, LISTE]);
      // Es gibt hier keinen Schalter (E‑164).
      expect(gerendert.behaelter.querySelector(`[aria-label="${N.automatik.name}"]`)).toBeNull();

      const stand = anfragen.length;
      for (let minute = 0; minute < 2; minute++) {
        await act(async () => {
          await vi.advanceTimersByTimeAsync(60_000);
        });
      }
      await zuege();
      expect(seit(stand)).toEqual([]);
    } finally {
      await gerendert.abbauen();
    }
  });

  /**
   * **E‑173 und E‑175:** Ohne freies Fenster steht der Knopf unmittelbar hinter
   * „Frei"; im freien Modus hinter den beiden Datumsfeldern — in beiden Lagen
   * als **letztes Bedienelement des Kopfes**. Die Eichung sind die Felder
   * selbst: Ohne sie wäre der zweite Teil dieselbe Aussage wie der erste.
   */
  it("steht rechts neben dem letzten Zeitraum-Knopf und im freien Modus hinter den Datumsfeldern", async () => {
    const gerendert = await rendereProzesse();
    try {
      const { behaelter } = gerendert;
      const knopf = mitName(behaelter, N.prozesse);
      const letzter = letzterKnopfIn(behaelter, TEXTE.zeitraum.bezeichnung);
      expect(knopfNach(behaelter, letzter)).toBe(knopf);

      const kopf = behaelter.querySelector("h1")?.parentElement;
      expect(kopf, "Kopf der Ansicht").toBeDefined();
      const bedienelemente = () => [...(kopf as Element).querySelectorAll("button, input")];
      expect(bedienelemente().at(-1), "ohne freies Fenster").toBe(knopf);

      await klicke(letzter);
      const felder = (kopf as Element).querySelectorAll('input[type="datetime-local"]');
      expect(felder, "Eichung: die Datumsfelder stehen").toHaveLength(2);
      expect(bedienelemente().at(-1), "im freien Modus").toBe(knopf);
    } finally {
      await gerendert.abbauen();
    }
  });

  it("holt beim Klick den Baum und danach die Liste ab Seite eins, sonst nichts", async () => {
    const gerendert = await rendereProzesse();
    try {
      const { behaelter } = gerendert;
      await klicke(knopfMitText(behaelter, TEXTE.nachrichten.blaettern.vor));
      expect(anfragen.at(-1)).toBe(`${LISTE}&cursor=cursor-2`);

      const stand = anfragen.length;
      await klicke(mitName(behaelter, N.prozesse));

      expect(seit(stand)).toEqual([BAUM, LISTE]);
    } finally {
      await gerendert.abbauen();
    }
  });

  /**
   * **Warum erst der Baum und dann die Liste.** Das Fenster der Liste kommt aus
   * der Antwort des Baums (E‑50). Rückt es beim Neuladen um eine Stunde, stellt
   * die Liste von selbst Seite eins mit dem neuen Fenster — und **keine**
   * Anfrage mit dem alten, deren Antwort niemand mehr zeigte. Die Gegenprobe
   * ist der Fall darüber: gleiches Fenster, die Liste fragt über `neuLaden`.
   */
  it("fragt die Liste nicht mit dem alten Fenster an, wenn der Baum ein neues bringt", async () => {
    let baumAufrufe = 0;
    stelleNetz((pfad, parameter) => {
      if (pfad === "/api/prozesse/baum") {
        baumAufrufe++;
        return baumAufrufe === 1
          ? prozessbaum()
          : {
              ...prozessbaum(),
              fenster: { von: "2025-12-28T06:00:00Z", bis: "2025-12-30T06:00:00Z" },
            };
      }
      return pfad === "/api/nachrichten" ? listenseite(parameter) : undefined;
    });
    const gerendert = await rendere(
      <NuqsTestingAdapter searchParams="?zeitraum=48H&prozess=P-0815" hasMemory>
        <ProzessAnsicht />
      </NuqsTestingAdapter>,
    );
    try {
      await zuege();
      const stand = anfragen.length;
      await klicke(mitName(gerendert.behaelter, N.prozesse));

      expect(seit(stand)).toEqual([
        BAUM,
        "/api/nachrichten?von=2025-12-28T06%3A00%3A00.000Z&bis=2025-12-30T06%3A00%3A00.000Z&prozess=P-0815",
      ]);
    } finally {
      await gerendert.abbauen();
    }
  });
});
