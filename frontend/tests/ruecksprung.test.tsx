// @vitest-environment jsdom

import { act } from "react";
import { NuqsTestingAdapter, type UrlUpdateEvent } from "nuqs/adapters/testing";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import {
  NACHRICHTEN_SCHLUESSEL,
  type Artefaktliste,
  type Nachricht,
  type Nachrichtendetail,
  type Seite,
} from "@/features/nachrichten/api";
import { NachrichtSeite } from "@/features/nachrichten/components/nachricht-seite";
import { NachrichtenAnsicht } from "@/features/nachrichten/components/nachrichten-ansicht";
import { texteFuer } from "@/i18n";
import { artefaktAnsicht } from "@/lib/routen";

import { neuerZwischenspeicher, rendere } from "./hilfe/rendern";

/**
 * **Der Rückweg aus der Nachricht** *(14.10.2026, E‑241 und E‑242,
 * `docs/nachrichtendetail.md` §10.2)* — die Fälle, die nur ein gerenderter Baum
 * trägt.
 *
 * | Fall | Warum ein Baum |
 * |---|---|
 * | die Adresse nach dem Schließen | `X` und `Escape` schreiben über `nuqs`, und was danach in der Adresse steht, entscheidet der Adapter beim Schreiben — keine reine Funktion kennt die Zeichenkette (S2) |
 * | der Fokus nach dem Schließen | Wo der Fokus steht, ist ein **Zustand des Dokuments** (S4) |
 * | kein zusätzlicher Abruf | eine Aussage über **Anfragen, die nicht hinausgehen** — gezählt am gestellten `fetch`, nicht an der Uhr (S3, Regel T1) |
 * | die eigene Route bei Direktaufruf | das Ziel liest `window.location.search` **im Ereignis**; ohne Dokument gibt es keins (S5) |
 * | der Fokus nach der Rückkehr aus einer Datei | **Verdrahtung:** Beide Einhängepunkte der Nachrichtenliste — Panel und eigene Route — verlangen ihn (`fokusNachDatei`). Die Regel selbst steht in `tests/zeitleiste-ziele.test.tsx` (S4) |
 *
 * **Was hier nicht steht:** der Weg über die Datei. Er wechselt die Route, und
 * die Liste wird dabei ausgehängt — Seitenstapel, Scrollstand und der Schalter
 * der automatischen Aktualisierung gehen verloren (offener Punkt,
 * `docs/nachrichtenliste.md` §8.3). Den Schritt zurück selbst belegen
 * `tests/artefakt-ansicht.test.tsx` und `tests/routen.test.ts`.
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

/**
 * **Eine Listen-Adresse mit jedem Parameter der Liste** — Zeitfenster, Status,
 * Prozess, Suche samt aufgehobener Grenze, Sortierung. So, wie `nuqs` sie
 * schreibt: Eine von Hand anders kodierte Adresse wäre danach inhaltlich gleich,
 * aber nicht zeichengleich (`docs/nachrichtendetail.md` §10.2, Korrektur vom
 * 14.10.2026).
 *
 * „Suche samt Feldname" aus dem Auftrag gibt es in der Liste nicht: Die
 * Kopfsuche führt nach `/suche` (`docs/bam-suche.md`), die Liste kennt nur den
 * Freitext `suche`.
 */
const LISTEN_ADRESSE =
  "?zeitraum=7d&status=FEHLER&prozess=P-0815&suche=Versand&langeSuche=true&sortierung=aelteste";

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

function zeile(messageId: string): Nachricht {
  return {
    messageId,
    zeitpunkt: "2025-12-29T10:01:09Z",
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

/** Seite eins: drei Zeilen und eine weitere Seite. Seite zwei: zwei Zeilen, die letzte. */
function listenseite(parameter: URLSearchParams): Seite<Nachricht> {
  return parameter.get("cursor") === null
    ? { items: ["a-1", "a-2", "a-3"].map(zeile), nextCursor: "cursor-2", hasMore: true }
    : { items: ["b-1", "b-2"].map(zeile), nextCursor: null, hasMore: false };
}

/**
 * Die Liste und sonst nichts — alles andere bekommt ein `404`. Das Panel zeigt
 * dann seinen Fehlerzustand; sein Kopf mit dem `X` steht in jedem Zustand.
 */
beforeEach(() => {
  anfragen = [];
  navigation.push.mockClear();
  navigation.back.mockClear();
  vi.stubGlobal(
    "fetch",
    vi.fn((eingabe: RequestInfo | URL) => {
      const adresse = String(eingabe);
      anfragen.push(adresse);
      const url = new URL(adresse, "http://localhost");
      return Promise.resolve(
        url.pathname === "/api/nachrichten"
          ? json(listenseite(url.searchParams))
          : problem(404, "nicht-gefunden"),
      );
    }),
  );
});

afterEach(() => {
  vi.unstubAllGlobals();
});

/** Wie oft die Liste abgefragt wurde — der Nachweis für S3. */
function listenabrufe(): number {
  return anfragen.filter(
    (adresse) => new URL(adresse, "http://localhost").pathname === "/api/nachrichten",
  ).length;
}

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

async function taste(element: Element, key: string): Promise<void> {
  await act(async () => {
    element.dispatchEvent(new KeyboardEvent("keydown", { key, bubbles: true, cancelable: true }));
  });
  await zuege();
}

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

async function rendereListe(suchparameter: string, zwischenspeicher = neuerZwischenspeicher()) {
  const adressen: UrlUpdateEvent[] = [];
  const gerendert = await rendere(
    <NuqsTestingAdapter
      searchParams={suchparameter}
      hasMemory
      onUrlUpdate={(ereignis) => adressen.push(ereignis)}
    >
      <NachrichtenAnsicht />
    </NuqsTestingAdapter>,
    zwischenspeicher,
  );
  await zuege();
  return { ...gerendert, adressen };
}

/* ─── die Rückkehr aus einer Datei ───────────────────────────────────────── */

/** Die Datei, aus der man zurückkommt: die konvertierte Nutzdatei an Schritt 1. */
const ARTEFAKT_ID = "1-Converter.Payload.GUID";

/**
 * Der Verlauf, wie ihn die Navigation API zeigt — gestellt, weil jsdom sie nicht
 * kennt. `vi.unstubAllGlobals()` im `afterEach` räumt ihn wieder ab.
 */
function stelleVerlauf(pfade: string[], aktuell: number) {
  const eintraege = pfade.map((pfad, index) => ({
    url: new URL(pfad, window.location.origin).href,
    index,
  }));
  vi.stubGlobal("navigation", { currentEntry: eintraege[aktuell], entries: () => eintraege });
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
    zeitpunkt: "2025-12-29T10:01:09Z",
    start: "2025-12-29T10:01:00Z",
    gesamtdauerSekunden: 9,
    fristSekunden: null,
    eigenschaftenAnzahl: 0,
    bamAnzahl: 0,
    offenerZustand: "KEINER",
    naechsterSchritt: null,
    wartetSeitSekunden: null,
    schritte: [
      {
        position: 1,
        name: "Datei konvertiert",
        namensherkunft: "DIREKT",
        rohwert: "ERFUNDEN_BAUSTEIN",
        start: "2025-12-29T10:01:00Z",
        ende: "2025-12-29T10:01:01Z",
        dauerSekunden: 1,
        timeoutSekunden: 1800,
        laeuftAuf: false,
      },
    ],
    kuratierteEigenschaften: [],
  };
  const dateien: Artefaktliste = {
    messageId,
    nutzdaten: [
      {
        artefaktId: ARTEFAKT_ID,
        name: "Converter.Payload.GUID",
        familie: "Converter",
        art: "NUTZDATEN",
        schritt: 1,
        beschnittMoeglich: false,
      },
    ],
    protokolle: [],
  };
  const zwischenspeicher = neuerZwischenspeicher();
  zwischenspeicher.setQueryData(NACHRICHTEN_SCHLUESSEL.detail(messageId), detail);
  zwischenspeicher.setQueryData(NACHRICHTEN_SCHLUESSEL.dateien(messageId), dateien);
  zwischenspeicher.setQueryData(NACHRICHTEN_SCHLUESSEL.eigenschaften(messageId), []);
  return zwischenspeicher;
}

describe("Das Schließen der Nachricht neben der Liste", () => {
  const SCHLIESSEN = TEXTE.nachrichten.detail.schliessen;

  it("führt über das X und über Escape auf die Listen-Adresse von vorher zurück", async () => {
    const { behaelter, adressen, abbauen } = await rendereListe(LISTEN_ADRESSE);

    try {
      await klicke(zeilen(behaelter)[1]);
      expect(adressen.at(-1)?.searchParams.get("nachricht")).toBe("a-2");

      await klicke(knopf(behaelter, SCHLIESSEN));
      expect(adressen.at(-1)?.queryString, "über das X").toBe(LISTEN_ADRESSE);

      await klicke(zeilen(behaelter)[1]);
      await taste(knopf(behaelter, SCHLIESSEN), "Escape");
      expect(adressen.at(-1)?.queryString, "über Escape").toBe(LISTEN_ADRESSE);
    } finally {
      await abbauen();
    }
  });

  it("gibt den Fokus an die Zeile zurück, die geöffnet hat — über das X und über Escape", async () => {
    const { behaelter, abbauen } = await rendereListe(LISTEN_ADRESSE);

    try {
      // Geöffnet mit der Tastatur, dann mit `Tab` ins Panel: Der Fokus steht
      // auf dem X, und das verschwindet beim Schließen samt Panel.
      zeilen(behaelter)[1].focus();
      await taste(zeilen(behaelter)[1], "Enter");
      knopf(behaelter, SCHLIESSEN).focus();
      await klicke(knopf(behaelter, SCHLIESSEN));
      expect(document.activeElement, "über das X").toBe(zeilen(behaelter)[1]);

      zeilen(behaelter)[2].focus();
      await taste(zeilen(behaelter)[2], "Enter");
      knopf(behaelter, SCHLIESSEN).focus();
      await taste(knopf(behaelter, SCHLIESSEN), "Escape");
      expect(document.activeElement, "über Escape").toBe(zeilen(behaelter)[2]);
    } finally {
      await abbauen();
    }
  });

  it("fragt die Liste beim Öffnen und Schließen nicht neu ab, auch nicht auf Seite zwei", async () => {
    const { behaelter, abbauen } = await rendereListe(LISTEN_ADRESSE);

    try {
      expect(listenabrufe(), "Seite eins").toBe(1);
      await klicke(knopfMitText(behaelter, TEXTE.nachrichten.blaettern.vor));
      expect(listenabrufe(), "Seite zwei").toBe(2);
      expect(zeilen(behaelter)).toHaveLength(2);

      await klicke(zeilen(behaelter)[0]);
      await klicke(knopf(behaelter, SCHLIESSEN));

      expect(listenabrufe(), "nach Öffnen und Schließen").toBe(2);
      // Eichung: Es ist weiterhin Seite zwei — der Stapel steht noch.
      expect(zeilen(behaelter)).toHaveLength(2);
    } finally {
      await abbauen();
    }
  });
});

/**
 * **Direktaufruf der eigenen Route** (S5): Schließen führt zur Liste mit den
 * Parametern, die die Adresse trägt — ohne solche zur Standardliste. Ein
 * `router.back()` gibt es hier nicht; der vorige Eintrag könnte außerhalb der
 * Anwendung liegen.
 */
describe("Das Schließen der Nachricht auf ihrer eigenen Route", () => {
  it("führt bei Direktaufruf zur Liste mit den Parametern der Adresse und bleibt in der Anwendung", async () => {
    const ZURUECK = TEXTE.nachrichten.detail.zurueckZurListe;

    try {
      window.history.replaceState(null, "", "/nachrichten/a-2?status=FEHLER&zeitraum=7d");
      const mitFiltern = await rendere(<NachrichtSeite messageId="a-2" />);
      try {
        await klicke(knopf(mitFiltern.behaelter, ZURUECK));
        expect(navigation.push).toHaveBeenCalledExactlyOnceWith(
          "/nachrichten?status=FEHLER&zeitraum=7d",
        );
      } finally {
        await mitFiltern.abbauen();
      }

      navigation.push.mockClear();
      window.history.replaceState(null, "", "/nachrichten/a-2");
      const ohneFilter = await rendere(<NachrichtSeite messageId="a-2" />);
      try {
        await klicke(knopf(ohneFilter.behaelter, ZURUECK));
        expect(navigation.push).toHaveBeenCalledExactlyOnceWith("/nachrichten");
      } finally {
        await ohneFilter.abbauen();
      }

      expect(navigation.back).not.toHaveBeenCalled();
    } finally {
      window.history.replaceState(null, "", "/");
    }
  });
});

/**
 * **Der Fokus nach der Rückkehr aus einer Datei, an beiden Einhängepunkten**
 * (E‑241, S4). Nach dem Schritt zurück ist die Datei der folgende Eintrag im
 * Verlauf; ihr Ziel in der Nachricht bekommt den Fokus. Hier steht allein die
 * Verdrahtung — die Regel samt Gegenproben steht in `tests/zeitleiste-ziele.test.tsx`.
 */
describe("Die Rückkehr aus einer Datei", () => {
  beforeEach(() => {
    (document.activeElement as HTMLElement | null)?.blur();
  });

  it("gibt in der Nachricht neben der Liste dem Ziel der Datei den Fokus", async () => {
    const PANEL = `${LISTEN_ADRESSE}&nachricht=a-2`;
    stelleVerlauf([`/nachrichten${PANEL}`, artefaktAnsicht("a-2", ARTEFAKT_ID)], 0);
    const { abbauen } = await rendereListe(PANEL, mitNachricht("a-2"));

    try {
      expect(document.activeElement?.getAttribute("href")).toBe(
        artefaktAnsicht("a-2", ARTEFAKT_ID),
      );
    } finally {
      await abbauen();
    }
  });

  it("gibt auf der eigenen Route der Nachricht dem Ziel der Datei den Fokus", async () => {
    try {
      window.history.replaceState(null, "", "/nachrichten/a-2?status=FEHLER");
      stelleVerlauf(["/nachrichten/a-2?status=FEHLER", artefaktAnsicht("a-2", ARTEFAKT_ID)], 0);
      const { abbauen } = await rendere(<NachrichtSeite messageId="a-2" />, mitNachricht("a-2"));
      try {
        expect(document.activeElement?.getAttribute("href")).toBe(
          artefaktAnsicht("a-2", ARTEFAKT_ID),
        );
      } finally {
        await abbauen();
      }
    } finally {
      window.history.replaceState(null, "", "/");
    }
  });
});
