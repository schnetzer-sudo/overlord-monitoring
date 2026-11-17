// @vitest-environment jsdom

import { act } from "react";
import { NuqsTestingAdapter, type UrlUpdateEvent } from "nuqs/adapters/testing";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { ZeitzoneProvider } from "@/components/zeitzone";
import type { BamSuchergebnis, Nachricht, Prozessbaum, Seite } from "@/features/nachrichten/api";
import { NachrichtenAnsicht } from "@/features/nachrichten/components/nachrichten-ansicht";
import { ProzessAnsicht } from "@/features/nachrichten/components/prozessansicht";
import { SucheAnsicht } from "@/features/nachrichten/components/suche-ansicht";
import { einsetzen, texteFuer } from "@/i18n";

import { rendere } from "./hilfe/rendern";

/**
 * **Freies Zeitfenster: Datum ohne Uhrzeit sucht den ganzen Tag** *(17.11.2026,
 * E‑1198, `docs/nachrichtenliste.md` §8.2, `docs/bam-suche.md` §11.6,
 * `docs/process-view.md` §41)* — die Fälle, die nur ein gerenderter Baum trägt.
 *
 * | Fall | Warum ein Baum |
 * |---|---|
 * | nur Datum ergibt `von`/`bis` des ganzen Tages | was in der Adresse steht, entscheidet `nuqs` beim Schreiben, und die Verdrahtung zweier Felder je Zeitpunkt ist Markup |
 * | die Liste lädt mit diesem Fenster | eine Aussage über die **Anfrage, die hinausgeht** — gezählt am gestellten `fetch` (Regel T1) |
 * | eine halbe Eingabe meldet sich am Feld, die Liste bleibt | Abwesenheit einer Anfrage und Anwesenheit der Zeilen; der Zustand kommt allein über `validity.badInput`, und den gibt es nur am Element |
 * | eine Antwort des Servers steht am Feld | dass sie **am Feld** steht (`aria-describedby`) und nicht als Fehlerbaustein über der Ansicht |
 * | die Felder zeigen, was in der Adresse steht | der Rückweg Adresse → Felder ist Markup; die Rechnung selbst ist rein (`tests/format.test.ts`) |
 *
 * **Die Anzeigezone ist gestellt** (`Europe/Berlin`): Ohne sie fiele der Rahmen
 * auf UTC zurück, und „00:00 bis 23:59:59" wäre von der Rechnung in UTC nicht zu
 * unterscheiden.
 *
 * **Antwortrümpfe gestellt, kein Netz, keine Datenbank.** Eine halbe Eingabe
 * kann `jsdom` nicht tippen; gestellt wird sie, wie der Browser sie herausgibt —
 * über `validity.badInput` am Element, gelesen an `keyup`.
 */

const navigation = vi.hoisted(() => {
  const push = vi.fn();
  const back = vi.fn();
  return { push, back, router: { push, back, replace: () => {} } };
});

vi.mock("next/navigation", () => ({
  useRouter: () => navigation.router,
  usePathname: () => "/nachrichten",
}));

const TEXTE = texteFuer("de");
const Z = TEXTE.zeitraum;
const BERLIN = "Europe/Berlin";

/** Jede Anfrage, in der Reihenfolge, in der sie hinausging. */
let anfragen: string[] = [];
/** Gestellte Abweichungen je Pfad: liefert die Funktion eine Antwort, gilt sie. */
let sonderfall: (pfad: string, parameter: URLSearchParams) => Response | undefined;

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

const ZEITPUNKT = "2026-07-07T10:01:09Z";

function zeile(messageId: string): Nachricht {
  return {
    messageId,
    zeitpunkt: ZEITPUNKT,
    status: "ERROR_ERFUNDEN",
    statusKind: "FEHLER",
    bedeutungNichtVerifiziert: false,
    processId: "P-0815",
    processName: "Versand Einzel IDOC",
    projectName: "Versand",
    sosName: "Versand Einzel IDOC aus Split",
    schritt: null,
  };
}

function listenseite(): Seite<Nachricht> {
  return { items: ["a-1", "a-2", "a-3"].map(zeile), nextCursor: null, hasMore: false };
}

function suchergebnis(parameter: URLSearchParams): BamSuchergebnis {
  return {
    nachrichten: ["a-1", "a-2", "a-3"].map((messageId) => ({
      ...zeile(messageId),
      rollen: [],
      treffer: [],
    })),
    begriffe: [],
    felder: [],
    von: parameter.get("von") ?? "2026-06-08T04:09:47Z",
    bis: parameter.get("bis") ?? "2026-07-08T04:09:47Z",
    abgeschnitten: false,
    modus: "EXAKT",
  };
}

/** Der Baum, wie das Backend ihn für ein freies Fenster liefert — `fenster.bis` ausschließend. */
function prozessbaum(parameter: URLSearchParams): Prozessbaum {
  const von = parameter.get("von");
  const bis = parameter.get("bis");
  return {
    zeitraum: von === null ? "48H" : "FREI",
    gliederung: "PARTNER",
    fenster:
      von === null || bis === null
        ? { von: "2026-07-06T05:00:00Z", bis: "2026-07-08T05:00:00Z" }
        : { von, bis: new Date(Date.parse(bis) + 3_600_000).toISOString() },
    stilleSchwelleMonate: 3,
    liveRest: { zustand: "NICHT_NOETIG", vollstaendigBis: null },
    fehlerLive: { zustand: "ANGEWANDT" },
    gesamt: { anzahlProzesse: 1, bewegt: 1, still: 0, nie: 0, nachrichten: 5, fehler: 5 },
    ebenen: ["PARTNER", "PROZESS"],
    knoten: [
      {
        schluessel: "BMW",
        name: "BMW",
        anzahlProzesse: 1,
        nachrichten: 5,
        fehler: 5,
        kinder: [
          {
            schluessel: "P-0815",
            name: "Versand Einzel IDOC",
            processId: "P-0815",
            nachrichten: 5,
            fehler: 5,
            letzteBewegung: ZEITPUNKT,
            zustand: "BEWEGT",
          },
        ],
      },
    ],
  };
}

/**
 * Die Prüfung des Baums auf volle Stunden (E‑95), wie das Backend sie hält: In
 * Berlin ist jede volle Stunde der Zone auch eine volle Stunde in UTC.
 */
function vollStuendig(wert: string | null): boolean {
  return wert === null || Date.parse(wert) % 3_600_000 === 0;
}

beforeEach(() => {
  anfragen = [];
  sonderfall = () => undefined;
  navigation.push.mockClear();
  (document.activeElement as HTMLElement | null)?.blur();
  vi.stubGlobal(
    "fetch",
    vi.fn((eingabe: RequestInfo | URL) => {
      const adresse = String(eingabe);
      anfragen.push(adresse);
      const url = new URL(adresse, "http://localhost");
      const gestellt = sonderfall(url.pathname, url.searchParams);
      if (gestellt !== undefined) {
        return Promise.resolve(gestellt);
      }
      switch (url.pathname) {
        case "/api/nachrichten":
          return Promise.resolve(json(listenseite()));
        case "/api/bam/suche":
          return Promise.resolve(json(suchergebnis(url.searchParams)));
        case "/api/bam/suchfelder":
          return Promise.resolve(json({ bam: [], felder: [] }));
        case "/api/prozesse/baum":
          return Promise.resolve(
            vollStuendig(url.searchParams.get("von")) && vollStuendig(url.searchParams.get("bis"))
              ? json(prozessbaum(url.searchParams))
              : problem(400, "zeitfenster-zu-genau"),
          );
        default:
          return Promise.resolve(problem(404, "nicht-gefunden"));
      }
    }),
  );
});

afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

/** Die Anfragen an einen Pfad, als Suchparameter. */
function abrufe(pfad: string): URLSearchParams[] {
  return anfragen
    .map((adresse) => new URL(adresse, "http://localhost"))
    .filter((url) => url.pathname === pfad)
    .map((url) => url.searchParams);
}

/** Einige Züge durch die Warteschlange — gezählt, nicht gewartet (T1). */
async function zuege(anzahl = 12): Promise<void> {
  for (let zug = 0; zug < anzahl; zug++) {
    await act(async () => {
      await new Promise((fertig) => setTimeout(fertig, 0));
    });
  }
}

async function ereignis(element: Element, art: Event): Promise<void> {
  await act(async () => {
    element.dispatchEvent(art);
  });
  await zuege();
}

const klicke = (element: Element) =>
  ereignis(element, new MouseEvent("click", { bubbles: true, cancelable: true }));

const taste = (element: Element, key: string) =>
  ereignis(element, new KeyboardEvent("keydown", { key, bubbles: true, cancelable: true }));

/** Setzt ein Feld, wie eine vollständige Eingabe es tut: Wert, dann `input`. */
async function trageEin(feld: HTMLInputElement, wert: string): Promise<void> {
  const setzer = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, "value")?.set;
  await act(async () => {
    setzer?.call(feld, wert);
    feld.dispatchEvent(new Event("input", { bubbles: true }));
  });
  await zuege();
}

/**
 * Eine **halbe** Eingabe, wie Chrome sie herausgibt: `value` bleibt leer, kein
 * `input`, allein `validity.badInput` — gelesen an `keyup`.
 */
async function tippeHalb(feld: HTMLInputElement): Promise<void> {
  Object.defineProperty(feld, "validity", { configurable: true, value: { badInput: true } });
  await ereignis(feld, new KeyboardEvent("keyup", { key: "1", bubbles: true }));
}

function knopfMitText(behaelter: HTMLElement, text: string): HTMLButtonElement {
  const treffer = [...behaelter.querySelectorAll("button")].find(
    (kandidat) => (kandidat.textContent ?? "").trim() === text,
  );
  expect(treffer, `Schaltfläche ${text}`).toBeDefined();
  return treffer as HTMLButtonElement;
}

function zeilen(behaelter: HTMLElement): HTMLTableRowElement[] {
  return [...behaelter.querySelectorAll<HTMLTableRowElement>("tbody tr")];
}

function datumsfelder(behaelter: HTMLElement): [HTMLInputElement, HTMLInputElement] {
  const felder = behaelter.querySelectorAll<HTMLInputElement>('input[type="date"]');
  expect(felder, "zwei Datumsfelder").toHaveLength(2);
  return [felder[0] as HTMLInputElement, felder[1] as HTMLInputElement];
}

function uhrzeitfelder(behaelter: HTMLElement): [HTMLInputElement, HTMLInputElement] {
  const felder = behaelter.querySelectorAll<HTMLInputElement>('input[type="time"]');
  expect(felder, "zwei Uhrzeitfelder").toHaveLength(2);
  return [felder[0] as HTMLInputElement, felder[1] as HTMLInputElement];
}

/** Der Text, auf den ein Feld über `aria-describedby` zeigt — die Meldung **am Feld**. */
function meldungAm(feld: HTMLElement): string | null {
  const kennung = feld.getAttribute("aria-describedby");
  return kennung === null ? null : (document.getElementById(kennung)?.textContent ?? null);
}

/** Kein Fehlerbaustein über der Ansicht. */
function ohneFehlerbaustein(behaelter: HTMLElement): void {
  expect(behaelter.querySelector('[role="alert"]'), "kein Fehlerbaustein").toBeNull();
  expect(behaelter.textContent).not.toContain(TEXTE.zustand.fehlerTitel);
}

type Ansicht = "nachrichten" | "suche" | "prozesse";

async function rendereAnsicht(ansicht: Ansicht, suchparameter: string) {
  const adressen: UrlUpdateEvent[] = [];
  const gerendert = await rendere(
    <ZeitzoneProvider zone={BERLIN}>
      <NuqsTestingAdapter
        searchParams={suchparameter}
        hasMemory
        onUrlUpdate={(aenderung) => adressen.push(aenderung)}
      >
        {ansicht === "nachrichten" ? (
          <NachrichtenAnsicht />
        ) : ansicht === "suche" ? (
          <SucheAnsicht />
        ) : (
          <ProzessAnsicht />
        )}
      </NuqsTestingAdapter>
    </ZeitzoneProvider>,
  );
  await zuege();
  const adresse = () => adressen.at(-1)?.searchParams ?? new URLSearchParams(suchparameter);
  return { ...gerendert, adresse };
}

/* ────────────────────────────────────────────────────────────────────────── */

describe("Die Nachrichtenliste: freies Fenster mit Datum und freiwilliger Uhrzeit", () => {
  async function frei(suchparameter = "") {
    const gerendert = await rendereAnsicht("nachrichten", suchparameter);
    const umschalter = gerendert.behaelter.querySelector(
      `[data-slot="toggle-group"][aria-label="${TEXTE.nachrichten.zeitfenster.bezeichnung}"]`,
    ) as HTMLElement;
    expect(umschalter, "Zeitfensterumschalter").not.toBeNull();
    if (suchparameter === "") {
      await klicke(knopfMitText(umschalter, TEXTE.nachrichten.zeitfenster.frei));
    }
    return gerendert;
  }

  it("sucht mit nur einem Datum in Von und Bis die ganzen Tage, und die Liste lädt", async () => {
    const gerendert = await frei();
    try {
      const [von, bis] = datumsfelder(gerendert.behaelter);
      await trageEin(von, "2026-07-06");
      await trageEin(bis, "2026-07-07");

      expect(gerendert.adresse().get("von")).toBe("2026-07-05T22:00:00.000Z");
      expect(gerendert.adresse().get("bis")).toBe("2026-07-07T21:59:59.000Z");
      const letzte = abrufe("/api/nachrichten").at(-1);
      expect(letzte?.get("von")).toBe("2026-07-05T22:00:00.000Z");
      expect(letzte?.get("bis")).toBe("2026-07-07T21:59:59.000Z");
      expect(zeilen(gerendert.behaelter)).toHaveLength(3);
      ohneFehlerbaustein(gerendert.behaelter);
    } finally {
      await gerendert.abbauen();
    }
  });

  it("macht aus demselben Datum zweimal genau einen Tag", async () => {
    const gerendert = await frei();
    try {
      const [von, bis] = datumsfelder(gerendert.behaelter);
      await trageEin(von, "2026-07-07");
      await trageEin(bis, "2026-07-07");

      expect(gerendert.adresse().get("von")).toBe("2026-07-06T22:00:00.000Z");
      expect(gerendert.adresse().get("bis")).toBe("2026-07-07T21:59:59.000Z");
      expect(zeilen(gerendert.behaelter)).toHaveLength(3);
    } finally {
      await gerendert.abbauen();
    }
  });

  it("zeigt eine Adresse in den Feldern, die Uhrzeit nur, wo sie nicht die Vorgabe ist", async () => {
    const gerendert = await frei("?von=2026-07-06T22:00:00.000Z&bis=2026-07-07T12:30:00.000Z");
    try {
      const [vonDatum, bisDatum] = datumsfelder(gerendert.behaelter);
      const [vonZeit, bisZeit] = uhrzeitfelder(gerendert.behaelter);
      expect([vonDatum.value, vonZeit.value]).toEqual(["2026-07-07", ""]);
      expect([bisDatum.value, bisZeit.value]).toEqual(["2026-07-07", "14:30"]);
    } finally {
      await gerendert.abbauen();
    }
  });

  it("meldet eine halbe Uhrzeit am Feld und lässt die Liste stehen", async () => {
    const gerendert = await frei("?von=2026-07-06T22:00:00.000Z&bis=2026-07-07T21:59:59.000Z");
    try {
      expect(zeilen(gerendert.behaelter), "Eichung: die Liste steht").toHaveLength(3);
      const vorher = abrufe("/api/nachrichten").length;
      const [, bisZeit] = uhrzeitfelder(gerendert.behaelter);

      await tippeHalb(bisZeit);

      expect(meldungAm(bisZeit)).toBe(Z.uhrzeitUnvollstaendig);
      expect(zeilen(gerendert.behaelter)).toHaveLength(3);
      expect(abrufe("/api/nachrichten"), "keine neue Anfrage").toHaveLength(vorher);
      expect(gerendert.adresse().get("bis"), "die Adresse bleibt").toBe("2026-07-07T21:59:59.000Z");
      ohneFehlerbaustein(gerendert.behaelter);
    } finally {
      await gerendert.abbauen();
    }
  });

  /**
   * **Die Felder springen beim Tippen nicht.** Kommt der Wert zurück, den die
   * Felder selbst geschrieben haben, bleibt der Entwurf stehen; erst beim
   * Verlassen wird die Uhrzeit so geschrieben, wie die Adresse sie zurückgäbe —
   * leer, wenn sie die Vorgabe ist.
   */
  it("lässt eine getippte Vorgabe-Uhrzeit stehen, bis das Feld verlassen wird", async () => {
    const gerendert = await frei();
    try {
      const [vonDatum] = datumsfelder(gerendert.behaelter);
      const [vonZeit] = uhrzeitfelder(gerendert.behaelter);
      await trageEin(vonDatum, "2026-07-07");
      await trageEin(vonZeit, "00:00");

      expect(gerendert.adresse().get("von"), "der ganze Tag").toBe("2026-07-06T22:00:00.000Z");
      expect(vonZeit.value, "beim Tippen bleibt, was getippt ist").toBe("00:00");

      await ereignis(vonZeit, new FocusEvent("focusout", { bubbles: true }));
      expect(vonZeit.value, "nach dem Verlassen: leer heißt dasselbe").toBe("");
      expect(gerendert.adresse().get("von")).toBe("2026-07-06T22:00:00.000Z");
    } finally {
      await gerendert.abbauen();
    }
  });

  it("stellt ein zu großes Fenster an die Felder und lässt die Liste stehen", async () => {
    const gerendert = await frei("?von=2026-07-06T22:00:00.000Z&bis=2026-07-07T21:59:59.000Z");
    sonderfall = (pfad, parameter) =>
      pfad === "/api/nachrichten" && parameter.get("von") === "2025-07-06T22:00:00.000Z"
        ? problem(400, "zeitfenster-zu-gross")
        : undefined;
    try {
      expect(zeilen(gerendert.behaelter), "Eichung: die Liste steht").toHaveLength(3);
      const [von] = datumsfelder(gerendert.behaelter);

      await trageEin(von, "2025-07-07");

      expect(abrufe("/api/nachrichten").at(-1)?.get("von"), "Eichung: gefragt").toBe(
        "2025-07-06T22:00:00.000Z",
      );
      expect(meldungAm(von)).toBe(TEXTE.fehler["zeitfenster-zu-gross"]);
      expect(zeilen(gerendert.behaelter)).toHaveLength(3);
      ohneFehlerbaustein(gerendert.behaelter);
    } finally {
      await gerendert.abbauen();
    }
  });
});

/* ────────────────────────────────────────────────────────────────────────── */

describe("Die Belegsuche: Zeitfenster mit Datum und freiwilliger Uhrzeit", () => {
  async function suche(suchparameter = "?begriff=:4711") {
    const gerendert = await rendereAnsicht("suche", suchparameter);
    if (gerendert.behaelter.querySelectorAll('input[type="date"]').length === 0) {
      await klicke(knopfMitText(gerendert.behaelter, TEXTE.suche.fenster.aendern));
    }
    return gerendert;
  }

  it("sucht mit nur einem Datum in Von und Bis die ganzen Tage, und die Trefferliste lädt", async () => {
    const gerendert = await suche();
    try {
      const [von, bis] = datumsfelder(gerendert.behaelter);
      await trageEin(von, "2026-07-06");
      await trageEin(bis, "2026-07-07");

      expect(gerendert.adresse().get("von")).toBe("2026-07-05T22:00:00.000Z");
      expect(gerendert.adresse().get("bis")).toBe("2026-07-07T21:59:59.000Z");
      const letzte = abrufe("/api/bam/suche").at(-1);
      expect(letzte?.get("von")).toBe("2026-07-05T22:00:00.000Z");
      expect(letzte?.get("bis")).toBe("2026-07-07T21:59:59.000Z");
      expect(zeilen(gerendert.behaelter)).toHaveLength(3);
      ohneFehlerbaustein(gerendert.behaelter);
    } finally {
      await gerendert.abbauen();
    }
  });

  it("macht aus demselben Datum zweimal genau einen Tag", async () => {
    const gerendert = await suche();
    try {
      const [von, bis] = datumsfelder(gerendert.behaelter);
      await trageEin(von, "2026-07-07");
      await trageEin(bis, "2026-07-07");

      expect(gerendert.adresse().get("von")).toBe("2026-07-06T22:00:00.000Z");
      expect(gerendert.adresse().get("bis")).toBe("2026-07-07T21:59:59.000Z");
    } finally {
      await gerendert.abbauen();
    }
  });

  it("meldet eine halbe Uhrzeit am Feld und lässt die Trefferliste stehen", async () => {
    const gerendert = await suche(
      "?begriff=:4711&von=2026-07-06T22:00:00.000Z&bis=2026-07-07T21:59:59.000Z",
    );
    try {
      expect(zeilen(gerendert.behaelter), "Eichung: die Trefferliste steht").toHaveLength(3);
      const vorher = abrufe("/api/bam/suche").length;
      const [vonZeit] = uhrzeitfelder(gerendert.behaelter);

      await tippeHalb(vonZeit);

      expect(meldungAm(vonZeit)).toBe(Z.uhrzeitUnvollstaendig);
      expect(zeilen(gerendert.behaelter)).toHaveLength(3);
      expect(abrufe("/api/bam/suche"), "keine neue Anfrage").toHaveLength(vorher);
      ohneFehlerbaustein(gerendert.behaelter);
    } finally {
      await gerendert.abbauen();
    }
  });

  /**
   * **Neu mit E‑1198:** Bis dahin ersetzte ein `zeitfenster-*`-Fehler die
   * Trefferliste durch den Fehlerbaustein. Jetzt steht er am Feld, und die
   * zuletzt gelieferte Antwort bleibt stehen.
   */
  it("stellt ein zu großes Fenster an die Felder und lässt die letzte Antwort stehen", async () => {
    const gerendert = await suche(
      "?begriff=:4711&von=2026-07-06T22:00:00.000Z&bis=2026-07-07T21:59:59.000Z",
    );
    sonderfall = (pfad, parameter) =>
      pfad === "/api/bam/suche" && parameter.get("von") === "2025-07-06T22:00:00.000Z"
        ? problem(400, "zeitfenster-zu-gross")
        : undefined;
    try {
      expect(zeilen(gerendert.behaelter), "Eichung: die Trefferliste steht").toHaveLength(3);
      const [von] = datumsfelder(gerendert.behaelter);

      await trageEin(von, "2025-07-07");

      expect(abrufe("/api/bam/suche").at(-1)?.get("von"), "Eichung: gefragt").toBe(
        "2025-07-06T22:00:00.000Z",
      );
      expect(meldungAm(von)).toBe(TEXTE.fehler["zeitfenster-zu-gross"]);
      expect(zeilen(gerendert.behaelter)).toHaveLength(3);
      ohneFehlerbaustein(gerendert.behaelter);
    } finally {
      await gerendert.abbauen();
    }
  });

  it("zeigt ohne frühere Antwort nur die Meldung am Feld, keinen Fehlerbaustein", async () => {
    sonderfall = (pfad, parameter) =>
      pfad === "/api/bam/suche" && parameter.get("bis") === null
        ? problem(400, "zeitfenster-unvollstaendig")
        : undefined;
    const gerendert = await suche("?begriff=:4711&von=2026-07-06T22:00:00.000Z");
    try {
      expect(abrufe("/api/bam/suche"), "Eichung: gefragt und abgewiesen").not.toHaveLength(0);
      const [von] = datumsfelder(gerendert.behaelter);
      expect(meldungAm(von)).toBe(TEXTE.fehler["zeitfenster-unvollstaendig"]);
      expect(zeilen(gerendert.behaelter)).toHaveLength(0);
      ohneFehlerbaustein(gerendert.behaelter);
    } finally {
      await gerendert.abbauen();
    }
  });

  it("setzt mit „Auf ein Jahr erweitern“ beide Zeitpunkte, und die Felder zeigen sie", async () => {
    const gerendert = await rendereAnsicht("suche", "?begriff=:4711");
    try {
      await klicke(knopfMitText(gerendert.behaelter, TEXTE.suche.fenster.einJahr));

      // Der Anker ist `bis` der Antwort: 08.07.2026, 04:09:47 UTC — in Berlin 06:09:47.
      expect(gerendert.adresse().get("bis")).toBe("2026-07-08T04:09:47.000Z");
      expect(gerendert.adresse().get("von")).toBe("2025-07-08T04:09:47.000Z");
      const [vonDatum, bisDatum] = datumsfelder(gerendert.behaelter);
      const [vonZeit, bisZeit] = uhrzeitfelder(gerendert.behaelter);
      expect([vonDatum.value, vonZeit.value]).toEqual(["2025-07-08", "06:09:47"]);
      expect([bisDatum.value, bisZeit.value]).toEqual(["2026-07-08", "06:09:47"]);
    } finally {
      await gerendert.abbauen();
    }
  });
});

/* ────────────────────────────────────────────────────────────────────────── */

describe("Die Prozessansicht: freies Fenster mit Datum und Stundenwahl", () => {
  async function frei(suchparameter = "?zeitraum=48H") {
    const gerendert = await rendereAnsicht("prozesse", suchparameter);
    const umschalter = gerendert.behaelter.querySelector(
      `[data-slot="toggle-group"][aria-label="${Z.bezeichnung}"]`,
    ) as HTMLElement;
    expect(umschalter, "Zeitraumumschalter").not.toBeNull();
    if (!suchparameter.includes("von=")) {
      await klicke(knopfMitText(umschalter, Z.FREI));
    }
    return gerendert;
  }

  /** Die Stundenwahl einer Grenze — der Auslöser, gefunden über seine Beschriftung. */
  function stundenwahl(grenze: "von" | "bis"): HTMLButtonElement {
    const text = einsetzen(Z.stunde, { grenze: grenze === "von" ? Z.von : Z.bis });
    const beschriftung = [...document.querySelectorAll("label")].find(
      (kandidat) => kandidat.textContent === text,
    );
    expect(beschriftung, `Beschriftung ${text}`).toBeDefined();
    const feld = document.getElementById(beschriftung?.htmlFor ?? "");
    expect(feld?.getAttribute("aria-haspopup"), "eine Liste der Anwendung").toBe("listbox");
    return feld as HTMLButtonElement;
  }

  it("macht aus nur einem Datum 00:00 bis 23:00 der Zone, ohne zeitfenster-zu-genau", async () => {
    const gerendert = await frei();
    try {
      expect(
        gerendert.behaelter.querySelectorAll('input[type="time"]'),
        "keine Uhrzeitfelder",
      ).toHaveLength(0);
      expect(gerendert.behaelter.querySelector("select"), "kein natives Auswahlfeld").toBeNull();
      const [von, bis] = datumsfelder(gerendert.behaelter);
      await trageEin(von, "2026-07-06");
      await trageEin(bis, "2026-07-07");

      expect(gerendert.adresse().get("von")).toBe("2026-07-05T22:00:00.000Z");
      expect(gerendert.adresse().get("bis")).toBe("2026-07-07T21:00:00.000Z");
      const letzte = abrufe("/api/prozesse/baum").at(-1);
      expect(letzte?.get("von")).toBe("2026-07-05T22:00:00.000Z");
      expect(letzte?.get("bis")).toBe("2026-07-07T21:00:00.000Z");
      expect(gerendert.behaelter.textContent).not.toContain(TEXTE.fehler["zeitfenster-zu-genau"]);
      expect(gerendert.behaelter.textContent, "der Baum steht").toContain("BMW");
      expect(stundenwahl("von").textContent).toBe(Z.ganzerTag);
      expect(stundenwahl("bis").textContent).toBe(Z.ganzerTag);
    } finally {
      await gerendert.abbauen();
    }
  });

  it("macht aus demselben Datum zweimal genau einen Tag — 00:00 bis 23:00", async () => {
    const gerendert = await frei();
    try {
      const [von, bis] = datumsfelder(gerendert.behaelter);
      await trageEin(von, "2026-07-07");
      await trageEin(bis, "2026-07-07");

      expect(gerendert.adresse().get("von")).toBe("2026-07-06T22:00:00.000Z");
      expect(gerendert.adresse().get("bis")).toBe("2026-07-07T21:00:00.000Z");
    } finally {
      await gerendert.abbauen();
    }
  });

  it("wählt die Stunde per Tastatur, und die letzte Stunde von Bis ist wieder der ganze Tag", async () => {
    const gerendert = await frei("?von=2026-07-06T22:00:00.000Z&bis=2026-07-07T21:00:00.000Z");
    try {
      await taste(stundenwahl("von"), "ArrowDown");
      const liste = document.querySelector('[role="listbox"]') as HTMLElement;
      expect(liste, "Eichung: die Liste ist offen").not.toBeNull();
      await taste(liste, "1");
      await taste(liste, "4");
      await taste(document.activeElement as Element, "Enter");
      expect(gerendert.adresse().get("von")).toBe("2026-07-07T12:00:00.000Z");
      expect(stundenwahl("von").textContent).toBe("14:00");

      // „Bis" nennt das Ende der Stunde; gespeichert wird die volle Stunde (E‑94).
      await klicke(stundenwahl("bis"));
      await klicke(document.querySelector('[role="option"][data-wert="12:00"]') as HTMLElement);
      expect(gerendert.adresse().get("bis")).toBe("2026-07-07T10:00:00.000Z");
      expect(stundenwahl("bis").textContent).toBe("12:59");

      await klicke(stundenwahl("bis"));
      await klicke(document.querySelector('[role="option"][data-wert="23:00"]') as HTMLElement);
      expect(gerendert.adresse().get("bis")).toBe("2026-07-07T21:00:00.000Z");
      expect(stundenwahl("bis").textContent, "die Vorgabe zeigt sich als ganzer Tag").toBe(
        Z.ganzerTag,
      );
    } finally {
      await gerendert.abbauen();
    }
  });

  it("meldet ein halbes Datum am Feld und lässt den Baum stehen", async () => {
    const gerendert = await frei("?von=2026-07-06T22:00:00.000Z&bis=2026-07-07T21:00:00.000Z");
    try {
      expect(gerendert.behaelter.textContent, "Eichung: der Baum steht").toContain("BMW");
      const vorher = abrufe("/api/prozesse/baum").length;
      const [von] = datumsfelder(gerendert.behaelter);

      await tippeHalb(von);

      expect(meldungAm(von)).toBe(Z.datumUnvollstaendig);
      expect(gerendert.behaelter.textContent).toContain("BMW");
      expect(abrufe("/api/prozesse/baum"), "keine neue Anfrage").toHaveLength(vorher);
      ohneFehlerbaustein(gerendert.behaelter);
    } finally {
      await gerendert.abbauen();
    }
  });
});
