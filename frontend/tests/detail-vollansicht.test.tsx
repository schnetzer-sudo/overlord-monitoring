// @vitest-environment jsdom

import { act } from "react";
import { NuqsTestingAdapter, type UrlUpdateEvent } from "nuqs/adapters/testing";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import {
  NACHRICHTEN_SCHLUESSEL,
  type Artefaktliste,
  type BamSuchergebnis,
  type Nachricht,
  type Nachrichtendetail,
  type Prozessbaum,
  type Seite,
} from "@/features/nachrichten/api";
import { NachrichtSeite } from "@/features/nachrichten/components/nachricht-seite";
import { NachrichtenAnsicht } from "@/features/nachrichten/components/nachrichten-ansicht";
import { ProzessAnsicht } from "@/features/nachrichten/components/prozessansicht";
import { SucheAnsicht } from "@/features/nachrichten/components/suche-ansicht";
import { texteFuer } from "@/i18n";
import { NACHRICHTENLISTE } from "@/features/nachrichten/spalten";
import { TREFFERLISTE_OHNE_TREFFER } from "@/features/nachrichten/treffer-spalten";
import { PANEL_NEBEN_LISTE_AB_XL, panelNebenListe } from "@/lib/panelbreite";

import { neuerZwischenspeicher, rendere } from "./hilfe/rendern";

/**
 * **Vollansicht überall, Strg- und Mittelklick, Absprung immer verfügbar**
 * *(17.11.2026, E‑1194 bis E‑1197, `docs/nachrichtendetail.md` §10.20)* — die
 * Fälle, die nur ein gerenderter Baum trägt.
 *
 * | Fall | Warum ein Baum |
 * |---|---|
 * | was ab `xl` weicht und was die Breite nimmt | die Regel **ist** eine Klasse — der Umbruchpunkt steht bewusst nicht in JavaScript |
 * | die Adresse nach Maximieren, Verkleinern, Escape und Schließen | was in der Adresse steht, entscheidet `nuqs` beim Schreiben |
 * | kein zusätzlicher Abruf beim Umschalten | eine Aussage über **Anfragen, die nicht hinausgehen** — gezählt am gestellten `fetch` (Regel T1) |
 * | der Fokus nach dem Schließen aus der Vollansicht | ein Zustand des Dokuments |
 * | dass jede der drei Tabellen die Zeilenbedienung trägt | Verdrahtung; die Entscheidung je Ereignis steht in `tests/zeilenbedienung.test.ts` |
 * | wo der Absprung erscheint | Abwesenheit eines Verweises — und sein Ziel hängt an der Zone des Rahmens |
 *
 * **Was hier nicht steht:** ob die Liste nach dem Verkleinern auf demselben
 * Pixel steht. `jsdom` rechnet kein Layout; der Bildlauf wird in
 * `tests/in-sicht-bringen.test.tsx` am gestellten Kasten geprüft und im Browser
 * gesehen.
 *
 * **Antwortrümpfe gestellt, kein Netz, keine Datenbank.** Zugesichert wird, was
 * im Baum und in der Adresse steht, nie eine Zeit (Regel T1).
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
const DETAIL = TEXTE.nachrichten.detail;

const LISTEN_ADRESSE = "?zeitraum=7d&status=FEHLER&sortierung=aelteste";
const SUCHE_ADRESSE = "?begriff=:4711&feld=Message.SourceMessageID:ERFUNDEN-1";
const PROZESS_ADRESSE = "?zeitraum=48H&prozess=P-0815";

/** Jede Anfrage, in der Reihenfolge, in der sie hinausging. */
let anfragen: string[] = [];

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

const ZEITPUNKT = "2025-12-29T10:01:09Z";

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

function suchergebnis(): BamSuchergebnis {
  return {
    nachrichten: ["a-1", "a-2", "a-3"].map((messageId) => ({
      ...zeile(messageId),
      rollen: [],
      treffer: [],
    })),
    begriffe: [],
    felder: [],
    von: "2025-11-30T04:09:47Z",
    bis: "2025-12-30T04:09:47Z",
    abgeschnitten: false,
    modus: "EXAKT",
  };
}

function prozessbaum(): Prozessbaum {
  return {
    zeitraum: "48H",
    gliederung: "PARTNER",
    fenster: { von: "2025-12-28T05:00:00Z", bis: "2025-12-30T05:00:00Z" },
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

/** Seite eins: drei Zeilen und eine weitere Seite. Seite zwei: zwei Zeilen, die letzte. */
function listenseite(parameter: URLSearchParams): Seite<Nachricht> {
  return parameter.get("cursor") === null
    ? { items: ["a-1", "a-2", "a-3"].map(zeile), nextCursor: "cursor-2", hasMore: true }
    : { items: ["b-1", "b-2"].map(zeile), nextCursor: null, hasMore: false };
}

beforeEach(() => {
  anfragen = [];
  navigation.push.mockClear();
  navigation.back.mockClear();
  (document.activeElement as HTMLElement | null)?.blur();
  vi.stubGlobal(
    "fetch",
    vi.fn((eingabe: RequestInfo | URL) => {
      const adresse = String(eingabe);
      anfragen.push(adresse);
      const url = new URL(adresse, "http://localhost");
      switch (url.pathname) {
        case "/api/nachrichten":
          return Promise.resolve(json(listenseite(url.searchParams)));
        case "/api/bam/suche":
          return Promise.resolve(json(suchergebnis()));
        case "/api/bam/suchfelder":
          return Promise.resolve(json({ bam: [], felder: [] }));
        case "/api/prozesse/baum":
          return Promise.resolve(json(prozessbaum()));
        default:
          return Promise.resolve(problem(404, "nicht-gefunden"));
      }
    }),
  );
});

afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
  window.history.replaceState(null, "", "/");
});

function abrufe(pfad: string): number {
  return anfragen.filter((adresse) => new URL(adresse, "http://localhost").pathname === pfad)
    .length;
}

/** Einige Züge durch die Warteschlange — gezählt, nicht gewartet (T1). */
async function zuege(anzahl = 12): Promise<void> {
  for (let zug = 0; zug < anzahl; zug++) {
    await act(async () => {
      await new Promise((fertig) => setTimeout(fertig, 0));
    });
  }
}

async function ereignis(element: Element, art: Event): Promise<Event> {
  await act(async () => {
    element.dispatchEvent(art);
  });
  await zuege();
  return art;
}

const klicke = (element: Element, zusatz: MouseEventInit = {}) =>
  ereignis(element, new MouseEvent("click", { bubbles: true, cancelable: true, ...zusatz }));

const taste = (element: Element, key: string, zusatz: KeyboardEventInit = {}) =>
  ereignis(
    element,
    new KeyboardEvent("keydown", { key, bubbles: true, cancelable: true, ...zusatz }),
  );

function zeilen(behaelter: HTMLElement): HTMLTableRowElement[] {
  return [...behaelter.querySelectorAll<HTMLTableRowElement>("tbody tr")];
}

function knopf(behaelter: HTMLElement, titel: string): HTMLButtonElement {
  const treffer = behaelter.querySelector<HTMLButtonElement>(`button[title="${titel}"]`);
  expect(treffer, `Schaltfläche ${titel}`).not.toBeNull();
  return treffer as HTMLButtonElement;
}

function knopfMitText(behaelter: HTMLElement, text: string): HTMLButtonElement {
  const treffer = [...behaelter.querySelectorAll("button")].find((kandidat) =>
    (kandidat.textContent ?? "").includes(text),
  );
  expect(treffer, `Schaltfläche ${text}`).toBeDefined();
  return treffer as HTMLButtonElement;
}

/** Die Klassen, mit denen ein Element ab `xl` wieder sichtbar wird. */
const SICHTBAR_AB_XL = ["xl:flex", "xl:block", "xl:inline-flex", "md:block", "md:flex"];

/**
 * **Steht das Element ab `xl` im Bild?** Nein, sobald ein Vorfahr `hidden` trägt
 * und keine Klasse, die ihn ab `xl` wieder einblendet — `display: none` über
 * die Klasse, wie die Ansicht es entscheidet, ohne Umbruchpunkt in JavaScript.
 */
function sichtbarAbXl(element: Element, bis: Element): boolean {
  for (let el: Element | null = element; el !== null && el !== bis; el = el.parentElement) {
    const klassen = [...el.classList];
    if (klassen.includes("hidden") && !klassen.some((k) => SICHTBAR_AB_XL.includes(k))) {
      return false;
    }
  }
  return true;
}

/** Die Hülle des Panels — das Element um den Abschnitt der Nachricht. */
function panelHuelle(behaelter: HTMLElement): HTMLElement {
  const abschnitt = behaelter.querySelector("section[aria-labelledby]");
  expect(abschnitt, "Panel").not.toBeNull();
  return (abschnitt as HTMLElement).parentElement as HTMLElement;
}

/** Klassen auf dem Weg vom Panel bis zum Behälter — für „kein eigener Scrollbereich". */
function klassenBisOben(element: Element, bis: Element): string[] {
  const klassen: string[] = [];
  for (let el: Element | null = element; el !== null && el !== bis; el = el.parentElement) {
    klassen.push(...el.classList);
  }
  return klassen;
}

/** Detail, Artefakte und Eigenschaften im Zwischenspeicher — das Panel fragt dann nichts an. */
function mitNachricht(messageId: string) {
  const detail: Nachrichtendetail = {
    messageId,
    status: "ERROR_ERFUNDEN",
    statusKind: "FEHLER",
    processId: "P-0815",
    processName: "Versand Einzel IDOC",
    projectName: "Versand",
    sosName: "Versand Einzel IDOC aus Split",
    rollen: [],
    zeitpunkt: ZEITPUNKT,
    start: "2025-12-29T10:01:00Z",
    gesamtdauerSekunden: 9,
    fristSekunden: null,
    eigenschaftenAnzahl: 0,
    bamAnzahl: 0,
    offenerZustand: "KEINER",
    naechsterSchritt: null,
    wartetSeitSekunden: null,
    schritte: [],
    kuratierteEigenschaften: [],
  };
  const dateien: Artefaktliste = { messageId, nutzdaten: [], protokolle: [] };
  const zwischenspeicher = neuerZwischenspeicher();
  zwischenspeicher.setQueryData(NACHRICHTEN_SCHLUESSEL.detail(messageId), detail);
  zwischenspeicher.setQueryData(NACHRICHTEN_SCHLUESSEL.dateien(messageId), dateien);
  zwischenspeicher.setQueryData(NACHRICHTEN_SCHLUESSEL.eigenschaften(messageId), []);
  return zwischenspeicher;
}

type Ansicht = "nachrichten" | "suche" | "prozesse";

async function rendereAnsicht(
  ansicht: Ansicht,
  suchparameter: string,
  zwischenspeicher = neuerZwischenspeicher(),
) {
  const adressen: UrlUpdateEvent[] = [];
  const gerendert = await rendere(
    <NuqsTestingAdapter
      searchParams={suchparameter}
      hasMemory
      onUrlUpdate={(aenderung) => adressen.push(aenderung)}
    >
      {/* Steht für `main`: der eine Scrollbereich des Rahmens. Ohne CSS rechnet
          jsdom ihn nicht; sein Stand wird in den Fällen gestellt, die ihn brauchen. */}
      <div data-kasten="" style={{ overflowY: "auto" }}>
        {ansicht === "nachrichten" ? (
          <NachrichtenAnsicht />
        ) : ansicht === "suche" ? (
          <SucheAnsicht />
        ) : (
          <ProzessAnsicht />
        )}
      </div>
    </NuqsTestingAdapter>,
    zwischenspeicher,
  );
  await zuege();
  return { ...gerendert, adressen };
}

/** Ein Bild weiter — die Prozessansicht setzt den Fokus beim Öffnen im `requestAnimationFrame`. */
async function einBild(): Promise<void> {
  await act(async () => {
    await new Promise((fertig) => requestAnimationFrame(() => fertig(null)));
  });
}

const ANSICHTEN = [
  {
    ansicht: "nachrichten" as const,
    name: "der Nachrichtenliste",
    adresse: LISTEN_ADRESSE,
    liste: "/api/nachrichten",
    blaettert: true,
  },
  {
    ansicht: "suche" as const,
    name: "der Belegsuche",
    adresse: SUCHE_ADRESSE,
    liste: "/api/bam/suche",
    blaettert: false,
  },
  {
    ansicht: "prozesse" as const,
    name: "der Prozessansicht",
    adresse: PROZESS_ADRESSE,
    liste: "/api/nachrichten",
    blaettert: true,
  },
];

/* ─── Die Vollansicht (E‑1194, E‑1197) ───────────────────────────────────── */

describe.each(ANSICHTEN)("Die Vollansicht $name", ({ ansicht, adresse, liste, blaettert }) => {
  const OHNE_LISTE = DETAIL.ansichtOhneListe;
  const NEBEN_LISTE = DETAIL.ansichtNebenListe;
  const SCHLIESSEN = DETAIL.schliessen;

  it("hat den Umschalter im Panelkopf vor dem Schließen-Knopf, unter `xl` unsichtbar", async () => {
    const { behaelter, abbauen } = await rendereAnsicht(ansicht, `${adresse}&nachricht=a-2`);

    try {
      const umschalter = knopf(behaelter, OHNE_LISTE);
      expect(umschalter.getAttribute("aria-label")).toBe(OHNE_LISTE);
      const klassen = umschalter.className.split(/\s+/);
      expect(klassen).toContain("hidden");
      expect(klassen).toContain("xl:inline-flex");
      expect(umschalter.nextElementSibling).toBe(knopf(behaelter, SCHLIESSEN));
    } finally {
      await abbauen();
    }
  });

  it("maximiert als Verlaufseintrag: die Liste weicht und bleibt montiert, das Panel nimmt die Inhaltsbreite", async () => {
    const { behaelter, adressen, abbauen } = await rendereAnsicht(ansicht, adresse);

    try {
      await klicke(zeilen(behaelter)[1]);
      await einBild();
      expect(adressen.at(-1)?.searchParams.get("nachricht")).toBe("a-2");
      // Eichung: Neben der Liste weicht ab `xl` nichts, und die Hülle klebt.
      expect(sichtbarAbXl(zeilen(behaelter)[0], behaelter)).toBe(true);
      expect(panelHuelle(behaelter).className).not.toContain("max-w-inhalt");

      await klicke(knopf(behaelter, OHNE_LISTE));

      const zuletzt = adressen.at(-1);
      expect(zuletzt?.searchParams.get("voll")).toBe("true");
      expect(zuletzt?.searchParams.get("nachricht")).toBe("a-2");
      expect(zuletzt?.options.history).toBe("push");

      expect(zeilen(behaelter), "montiert").toHaveLength(3);
      expect(sichtbarAbXl(zeilen(behaelter)[0], behaelter), "weicht").toBe(false);
      const huelle = panelHuelle(behaelter);
      expect(huelle.className).toContain("xl:max-w-inhalt");
      // Eine Spalte, kein eigener Scrollbereich: nichts klebt, nichts scrollt für sich.
      const aufDemWeg = klassenBisOben(huelle, behaelter);
      expect(aufDemWeg).not.toContain("xl:sticky");
      expect(aufDemWeg).not.toContain("xl:overflow-y-auto");
      expect(aufDemWeg).not.toContain("xl:max-h-[100cqh]");
      expect(knopf(behaelter, NEBEN_LISTE).getAttribute("aria-label")).toBe(NEBEN_LISTE);
    } finally {
      await abbauen();
    }
  });

  it("verkleinert per Umschalter und per Escape auf das Panel — ohne zusätzlichen Abruf und auf derselben Seite", async () => {
    const { behaelter, adressen, abbauen } = await rendereAnsicht(ansicht, adresse);

    try {
      if (blaettert) {
        await klicke(knopfMitText(behaelter, TEXTE.nachrichten.blaettern.vor));
        expect(zeilen(behaelter), "Seite zwei").toHaveLength(2);
      }
      const vorher = abrufe(liste);
      const baumVorher = abrufe("/api/prozesse/baum");
      const zeilenVorher = zeilen(behaelter).length;

      await klicke(zeilen(behaelter)[0]);
      await einBild();
      await klicke(knopf(behaelter, OHNE_LISTE));
      await klicke(knopf(behaelter, NEBEN_LISTE));

      const offen = blaettert ? "b-1" : "a-1";
      expect(adressen.at(-1)?.searchParams.has("voll"), "per Umschalter").toBe(false);
      expect(adressen.at(-1)?.searchParams.get("nachricht"), "nur eine Ebene").toBe(offen);
      expect(adressen.at(-1)?.options.history).toBe("push");
      expect(sichtbarAbXl(zeilen(behaelter)[0], behaelter)).toBe(true);

      await klicke(knopf(behaelter, OHNE_LISTE));
      await taste(knopf(behaelter, NEBEN_LISTE), "Escape");

      expect(adressen.at(-1)?.searchParams.has("voll"), "per Escape").toBe(false);
      expect(adressen.at(-1)?.searchParams.get("nachricht"), "nur eine Ebene").toBe(offen);
      expect(sichtbarAbXl(zeilen(behaelter)[0], behaelter)).toBe(true);

      expect(abrufe(liste), "Liste").toBe(vorher);
      expect(abrufe("/api/prozesse/baum"), "Baum").toBe(baumVorher);
      expect(zeilen(behaelter), "dieselbe Seite").toHaveLength(zeilenVorher);
    } finally {
      await abbauen();
    }
  });

  it("schließt aus der Vollansicht per X ganz und gibt den Fokus an die Zeile", async () => {
    const { behaelter, adressen, abbauen } = await rendereAnsicht(ansicht, adresse);

    try {
      const vorher = adressen.at(-1)?.queryString ?? adresse;
      zeilen(behaelter)[1].focus();
      await taste(zeilen(behaelter)[1], "Enter");
      await einBild();
      await klicke(knopf(behaelter, OHNE_LISTE));
      expect(adressen.at(-1)?.searchParams.get("voll")).toBe("true");

      knopf(behaelter, SCHLIESSEN).focus();
      await klicke(knopf(behaelter, SCHLIESSEN));

      expect(adressen.at(-1)?.queryString).toBe(vorher);
      expect(document.activeElement).toBe(zeilen(behaelter)[1]);
      expect(sichtbarAbXl(zeilen(behaelter)[1], behaelter)).toBe(true);
    } finally {
      await abbauen();
    }
  });

  /**
   * **Verkleinern zeigt die Liste mit dem Bildlauf von vorher.** Beim Maximieren
   * weicht die Liste, und der Browser klemmt den Stand von `main` auf das, was
   * übrig ist — hier gestellt, indem der Stand nach dem Maximieren auf null
   * fällt. Gemerkt wird im Ereignis, hergestellt beim Verlassen der Vollansicht:
   * per Umschalter wie per X.
   */
  it("stellt beim Verkleinern und beim Schließen den Bildlauf von vor dem Maximieren her", async () => {
    const { behaelter, abbauen } = await rendereAnsicht(ansicht, adresse);
    const kasten = behaelter.querySelector("[data-kasten]") as HTMLElement;
    let stand = 0;
    Object.defineProperty(kasten, "scrollTop", {
      configurable: true,
      get: () => stand,
      set: (wert: number) => {
        stand = wert;
      },
    });

    try {
      // Der Stand beim Öffnen — in der Prozessansicht der des Baums, der weicht.
      stand = 300;
      await klicke(zeilen(behaelter)[2]);
      await einBild();

      stand = 700;
      await klicke(knopf(behaelter, OHNE_LISTE));
      stand = 0;
      await klicke(knopf(behaelter, NEBEN_LISTE));
      expect(stand, "per Umschalter").toBe(700);

      stand = 640;
      await klicke(knopf(behaelter, OHNE_LISTE));
      stand = 0;
      await klicke(knopf(behaelter, SCHLIESSEN));
      // Beim Schließen steht erst die Liste, dann bekommt in der Prozessansicht der
      // Baum, der zurückkehrt, seinen eigenen Stand von vor dem Öffnen (E‑115).
      expect(stand, "per X").toBe(ansicht === "prozesse" ? 300 : 640);
    } finally {
      await abbauen();
    }
  });

  it("geht mit Escape eine Ebene: erst verkleinern, dann schließen", async () => {
    const { behaelter, adressen, abbauen } = await rendereAnsicht(
      ansicht,
      `${adresse}&nachricht=a-2&voll=true`,
    );

    try {
      await taste(knopf(behaelter, SCHLIESSEN), "Escape");
      expect(adressen.at(-1)?.searchParams.has("voll")).toBe(false);
      expect(adressen.at(-1)?.searchParams.get("nachricht")).toBe("a-2");

      await taste(knopf(behaelter, SCHLIESSEN), "Escape");
      expect(adressen.at(-1)?.searchParams.has("nachricht")).toBe(false);
    } finally {
      await abbauen();
    }
  });

  it("zeigt eine Adresse mit `voll` neu geladen in der Vollansicht", async () => {
    const { behaelter, abbauen } = await rendereAnsicht(
      ansicht,
      `${adresse}&nachricht=a-2&voll=true`,
      mitNachricht("a-2"),
    );

    try {
      expect(knopf(behaelter, NEBEN_LISTE)).toBeDefined();
      expect(panelHuelle(behaelter).className).toContain("xl:max-w-inhalt");
      expect(sichtbarAbXl(zeilen(behaelter)[0], behaelter)).toBe(false);
    } finally {
      await abbauen();
    }
  });

  it("lässt `voll` ohne Nachricht folgenlos — eine Zeile öffnet das Panel neben der Liste", async () => {
    const { behaelter, adressen, abbauen } = await rendereAnsicht(ansicht, `${adresse}&voll=true`);

    try {
      expect(sichtbarAbXl(zeilen(behaelter)[0], behaelter)).toBe(true);
      await klicke(zeilen(behaelter)[1]);
      await einBild();

      expect(adressen.at(-1)?.searchParams.get("nachricht")).toBe("a-2");
      expect(adressen.at(-1)?.searchParams.has("voll")).toBe(false);
      expect(knopf(behaelter, OHNE_LISTE)).toBeDefined();
      expect(sichtbarAbXl(zeilen(behaelter)[0], behaelter)).toBe(true);
    } finally {
      await abbauen();
    }
  });
});

it("nimmt `voll` beim Wechsel des Prozesses mit der Nachricht weg", async () => {
  const { behaelter, adressen, abbauen } = await rendereAnsicht(
    "prozesse",
    `${PROZESS_ADRESSE}&nachricht=a-2&voll=true`,
  );

  try {
    await klicke(knopfMitText(behaelter, TEXTE.prozesse.liste.zurueckZumBaum));

    const zuletzt = adressen.at(-1)?.searchParams;
    expect(zuletzt?.has("prozess")).toBe(false);
    expect(zuletzt?.has("nachricht")).toBe(false);
    expect(zuletzt?.has("voll")).toBe(false);
  } finally {
    await abbauen();
  }
});

/* ─── Strg-, Cmd- und Mittelklick (E‑1195) ───────────────────────────────── */

describe.each([
  {
    ansicht: "nachrichten" as const,
    name: "der Nachrichtenliste",
    adresse: LISTEN_ADRESSE,
    pfad: "/nachrichten",
    // Aus der Liste mit ihrer Abfragezeichenkette, ohne `nachricht`.
    ziel: `/nachrichten/a-2${LISTEN_ADRESSE}`,
  },
  {
    ansicht: "suche" as const,
    name: "der Belegsuche",
    adresse: SUCHE_ADRESSE,
    pfad: "/suche",
    ziel: "/nachrichten/a-2",
  },
  {
    ansicht: "prozesse" as const,
    name: "der Prozessansicht",
    adresse: PROZESS_ADRESSE,
    pfad: "/prozesse",
    ziel: "/nachrichten/a-2",
  },
])("Eine Zeile $name im neuen Tab", ({ ansicht, adresse, pfad, ziel }) => {
  /** Die Adresse, wie sie im Augenblick des Klicks in der Adresszeile steht — mit offenem Panel. */
  const IM_FENSTER = `${pfad}${adresse}&nachricht=a-1`;

  it("öffnet mit Strg-, Cmd- und Mittelklick und mit Strg+Enter das Ziel — und die Ausgangsseite bleibt", async () => {
    window.history.replaceState(null, "", IM_FENSTER);
    const oeffne = vi.spyOn(window, "open").mockReturnValue(null);
    const { behaelter, adressen, abbauen } = await rendereAnsicht(
      ansicht,
      `${adresse}&nachricht=a-1`,
    );

    try {
      const anzahl = adressen.length;
      const zweite = () => zeilen(behaelter)[1];

      await klicke(zweite(), { ctrlKey: true });
      await klicke(zweite(), { metaKey: true });
      await ereignis(
        zweite(),
        new MouseEvent("auxclick", { bubbles: true, cancelable: true, button: 1 }),
      );
      zweite().focus();
      await taste(zweite(), "Enter", { ctrlKey: true });

      expect(oeffne).toHaveBeenCalledTimes(4);
      for (const aufruf of oeffne.mock.calls) {
        expect(aufruf).toEqual([ziel, "_blank", "noopener"]);
      }
      expect(adressen, "keine neue Adresse").toHaveLength(anzahl);
      expect(zweite().getAttribute("aria-current")).toBeNull();
      expect(navigation.push).not.toHaveBeenCalled();
    } finally {
      await abbauen();
    }
  });

  it("unterdrückt beim Drücken der mittleren Taste das automatische Scrollen", async () => {
    const { behaelter, abbauen } = await rendereAnsicht(ansicht, adresse);

    try {
      const druck = await ereignis(
        zeilen(behaelter)[1],
        new MouseEvent("mousedown", { bubbles: true, cancelable: true, button: 1 }),
      );
      expect(druck.defaultPrevented).toBe(true);
    } finally {
      await abbauen();
    }
  });

  it("öffnet beim gewöhnlichen Klick und mit Enter wie bisher das Panel", async () => {
    const oeffne = vi.spyOn(window, "open").mockReturnValue(null);
    const { behaelter, adressen, abbauen } = await rendereAnsicht(ansicht, adresse);

    try {
      await klicke(zeilen(behaelter)[1]);
      await einBild();
      expect(adressen.at(-1)?.searchParams.get("nachricht")).toBe("a-2");

      zeilen(behaelter)[2].focus();
      await taste(zeilen(behaelter)[2], "Enter");
      await einBild();
      expect(adressen.at(-1)?.searchParams.get("nachricht")).toBe("a-3");
      expect(oeffne).not.toHaveBeenCalled();
    } finally {
      await abbauen();
    }
  });
});

/* ─── Der Absprung in den Prozessbaum (E‑1196) ──────────────────────────── */

/** Der Absprung im Baum — oder `null`. */
function absprung(behaelter: HTMLElement): string | null {
  return behaelter.querySelector('a[data-absprung="prozessbaum"]')?.getAttribute("href") ?? null;
}

/**
 * Der Kalendertag der Nachricht in der Zone des Rahmens. Ohne Provider ist das
 * UTC (`components/zeitzone.tsx`); die Umrechnung in eine echte Zone samt
 * Umstellungstagen prüft `tests/prozessansicht.test.ts`.
 */
const TAG =
  "/prozesse?von=2025-12-29T00%3A00%3A00.000Z&bis=2025-12-29T23%3A00%3A00.000Z&prozess=P-0815&nachricht=a-2";

describe("Der Absprung in den Prozessbaum", () => {
  it("steht in der Nachrichtenliste auch im relativen Modus — mit dem Kalendertag", async () => {
    const { behaelter, abbauen } = await rendereAnsicht(
      "nachrichten",
      "?zeitraum=7d&nachricht=a-2",
      mitNachricht("a-2"),
    );

    try {
      expect(absprung(behaelter)).toBe(TAG);
    } finally {
      await abbauen();
    }
  });

  it("nimmt im freien Fenster der Liste wie bisher das Herkunftsfenster", async () => {
    const { behaelter, abbauen } = await rendereAnsicht(
      "nachrichten",
      "?von=2025-12-28T00:00:00.000Z&bis=2025-12-30T00:00:00.000Z&nachricht=a-2",
      mitNachricht("a-2"),
    );

    try {
      expect(absprung(behaelter)).toBe(
        "/prozesse?von=2025-12-28T00%3A00%3A00.000Z&bis=2025-12-30T00%3A00%3A00.000Z&prozess=P-0815&nachricht=a-2",
      );
    } finally {
      await abbauen();
    }
  });

  it("nimmt in der Belegsuche wie bisher das Fenster der Antwort", async () => {
    const { behaelter, abbauen } = await rendereAnsicht(
      "suche",
      `${SUCHE_ADRESSE}&nachricht=a-2`,
      mitNachricht("a-2"),
    );

    try {
      expect(absprung(behaelter)).toBe(
        "/prozesse?von=2025-11-30T04%3A00%3A00.000Z&bis=2025-12-30T04%3A00%3A00.000Z&prozess=P-0815&nachricht=a-2",
      );
    } finally {
      await abbauen();
    }
  });

  it("steht in der Vollansicht", async () => {
    const { behaelter, abbauen } = await rendereAnsicht(
      "nachrichten",
      "?zeitraum=7d&nachricht=a-2&voll=true",
      mitNachricht("a-2"),
    );

    try {
      expect(absprung(behaelter)).toBe(TAG);
    } finally {
      await abbauen();
    }
  });

  it("steht auf der eigenen Route — mit dem Kalendertag", async () => {
    window.history.replaceState(null, "", "/nachrichten/a-2?status=FEHLER");
    const { behaelter, abbauen } = await rendere(
      <NachrichtSeite messageId="a-2" />,
      mitNachricht("a-2"),
    );

    try {
      expect(absprung(behaelter)).toBe(TAG);
    } finally {
      await abbauen();
    }
  });

  it("fehlt in der Prozessansicht, neben der Liste wie in der Vollansicht", async () => {
    for (const zusatz of ["&nachricht=a-2", "&nachricht=a-2&voll=true"]) {
      const { behaelter, abbauen } = await rendereAnsicht(
        "prozesse",
        `${PROZESS_ADRESSE}${zusatz}`,
        mitNachricht("a-2"),
      );

      try {
        // Eichung: Das Panel steht mit seinem Kopf da.
        expect(behaelter.querySelector("section[aria-labelledby] dl"), zusatz).not.toBeNull();
        expect(absprung(behaelter), zusatz).toBeNull();
      } finally {
        await abbauen();
      }
    }
  });
});

/* ─── Die ziehbare Breite (E‑1208 bis E‑1210) ────────────────────────────── */

/**
 * **Die Verdrahtung an den drei Einhängepunkten** *(17.11.2026,
 * `docs/nachrichtendetail.md` §10.21)*: Neben der Liste steht der Griff vor der
 * Hülle, und die Hülle trägt Breite und Maximum — das Maximum aus der
 * Spaltendefinition der Liste, die daneben steht. In der Vollansicht gibt es
 * beides nicht. Was Griff und Regeln selbst tun, steht in
 * `tests/panel-griff.test.tsx` und `tests/panelbreite.test.ts`.
 */
const ABZUG = {
  nachrichten: panelNebenListe(NACHRICHTENLISTE),
  // Die gestellte Antwort zitiert keinen BAM-Begriff (`begriffe: []`): Die
  // Trefferliste hat keine Spalte „Treffer“, und ihre Grundmenge ist die ohne.
  // Den Zweig mit „Treffer“ belegt der Browser (§10.21).
  suche: panelNebenListe(TREFFERLISTE_OHNE_TREFFER),
  prozesse: panelNebenListe(NACHRICHTENLISTE, { listeScrolltSelbst: true }),
} as unknown as Record<string, Record<string, string>>;

describe.each(ANSICHTEN)("Die ziehbare Breite $name", ({ ansicht, adresse }) => {
  it("hat neben der Liste den Griff vor der Hülle, die Breite ab xl und das Maximum der Liste", async () => {
    const { behaelter, abbauen } = await rendereAnsicht(ansicht, `${adresse}&nachricht=a-2`);

    try {
      const huelle = panelHuelle(behaelter);
      const griff = huelle.previousElementSibling;
      expect(griff?.getAttribute("role")).toBe("separator");
      expect(behaelter.querySelectorAll('[role="separator"]')).toHaveLength(1);
      const klassen = huelle.className.split(/\s+/);
      for (const klasse of PANEL_NEBEN_LISTE_AB_XL.split(/\s+/)) {
        expect(klassen, klasse).toContain(klasse);
      }
      // Die alte feste Breite ist weg.
      expect(klassen).not.toContain("xl:w-[26rem]");
      expect(huelle.style.getPropertyValue("--panel-abzug")).toBe(ABZUG[ansicht]!["--panel-abzug"]);
    } finally {
      await abbauen();
    }
  });

  it("hat in der Vollansicht weder Griff noch Breite", async () => {
    const { behaelter, abbauen } = await rendereAnsicht(
      ansicht,
      `${adresse}&nachricht=a-2&voll=true`,
    );

    try {
      const huelle = panelHuelle(behaelter);
      expect(behaelter.querySelector('[role="separator"]')).toBeNull();
      expect(huelle.className).not.toContain("--panelbreite");
      expect(huelle.className).not.toContain("--eigenschaft-name-max");
      expect(huelle.style.getPropertyValue("--panel-abzug")).toBe("");
      expect(huelle.className).toContain("xl:max-w-inhalt");
    } finally {
      await abbauen();
    }
  });
});
