// @vitest-environment jsdom

import { act, useEffect, useState } from "react";
import { NuqsTestingAdapter } from "nuqs/adapters/testing";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { Kopfzeile } from "@/components/kopfzeile";
import type { Vorschlag } from "@/features/assistent/api";
import { AssistentEingang } from "@/features/assistent/components/assistent-eingang";
import { DashboardAnsicht } from "@/features/dashboard/components/dashboard-ansicht";
import { PRUEFVERZOEGERUNG_MS } from "@/features/assistent/hooks";
import { NACHRICHTEN_SCHLUESSEL, type Suchfelder } from "@/features/nachrichten/api";
import type { Selbstauskunft } from "@/features/sitzung/api";
import { texteFuer } from "@/i18n";
import type { Mandant } from "@/lib/mandanten";

import { neuerZwischenspeicher, rendere } from "./hilfe/rendern";

/**
 * **Chatbox und Karte im Baum, gegen gestellte Endpunkte** (A3,
 * `docs/assistent-frontend.md` §8).
 *
 * Die Regeln selbst sind reine Funktionen und stehen in
 * `tests/assistent.test.ts`. Hier steht, was nur der Baum belegt: **welche
 * Anfragen hinausgehen** — womit, in welcher Reihenfolge, mit welcher
 * `anfrageId` —, **was im Dokument ankommt** (Textknoten statt HTML, das Feld
 * mit seinem Fehler, das fehlende Symbol) und **wo der Fokus steht**.
 *
 * Kein Netz, keine Datenbank, kein Modell: `fetch` ist gestellt, je Endpunkt
 * eine Antwort oder eine Warteschlange. Keine Zusicherung über Wanduhrzeit;
 * gewartet wird auf Zustände (`bis`), einmal auf die Verzögerung der Prüfung.
 */

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: () => {}, replace: () => {} }),
  usePathname: () => "/",
}));

const TEXTE = texteFuer("de");
const A = TEXTE.assistent;
const K = A.karte;

const NEXANS: Mandant = { id: "NEXANS", name: "Nexans autoelectric GmbH" };
const IBIS: Mandant = { id: "IBIS", name: "IBIS Beispiel GmbH" };
const TYPNAME = "Anpassen der Werk-/Abladestellentabelle";

const VORSCHLAG: Vorschlag = {
  typ: "WERK_ABLADESTELLE",
  name: TYPNAME,
  aktion: "Anpassung",
  felder: [
    { name: "lieferantennummer", label: "Lieferantennummer", hinweis: null },
    { name: "werk", label: "Werk", hinweis: null },
    { name: "abladestelle", label: "Abladestelle", hinweis: null },
    {
      name: "sndprn",
      label: "SNDPRN",
      hinweis: "Bei Anpassung heißt leer: der Wert wird entfernt.",
    },
    {
      name: "sndprn4908",
      label: "SNDPRN_4908",
      hinweis: "Für VDA-4908-Prozesse. Bei Anpassung heißt leer: der Wert wird entfernt.",
    },
  ],
  eintraege: [
    {
      werte: {
        lieferantennummer: null,
        werk: "72332",
        abladestelle: "DDB0",
        sndprn: "120764",
        sndprn4908: null,
      },
      schluessel: "72332/DDB0",
    },
  ],
};

function mitEintraegen(anzahl: number): Vorschlag {
  return {
    ...VORSCHLAG,
    eintraege: Array.from({ length: anzahl }, (_, nummer) => ({
      werte: { ...VORSCHLAG.eintraege[0].werte, werk: String(70000 + nummer) },
      schluessel: `${70000 + nummer}/DDB0`,
    })),
  };
}

// ── Die gestellten Endpunkte ────────────────────────────────────────────────

type Ruf = { methode: string; pfad: string; koerper: Record<string, unknown> | undefined };
type Bedienung = () => Response | Promise<Response>;

let rufe: Ruf[] = [];
let stand: Bedienung;
let eroeffnen: Bedienung;
let nachrichten: Bedienung[];
let pruefung: (koerper: Record<string, unknown> | undefined) => Response | Promise<Response>;
let anlegen: Bedienung[];
/** Was `GET /api/tasks` liefert — leer, bis ein Anlegen durchgeht (A4). */
let auftragsliste: unknown[];

function json(rumpf: unknown, status = 200): Response {
  return new Response(JSON.stringify(rumpf), {
    status,
    headers: { "content-type": "application/json" },
  });
}

function problem(status: number, typ: string, angaben: Record<string, unknown> = {}): Response {
  return new Response(
    JSON.stringify({
      type: `https://overlord.kraftwerkone.de/probleme/${typ}`,
      title: typ,
      status,
      detail: "Text aus dem Backend.",
      ...angaben,
    }),
    { status, headers: { "content-type": "application/problem+json" } },
  );
}

/** Eine Antwort, die erst kommt, wenn der Test es sagt. */
function haengend(): { bedienung: Bedienung; loese: (antwort: Response) => void } {
  let loese: (antwort: Response) => void = () => {};
  const versprechen = new Promise<Response>((fertig) => {
    loese = fertig;
  });
  return { bedienung: () => versprechen, loese: (antwort) => loese(antwort) };
}

const AUFTRAG = {
  taskId: "5b8f3c1e-0000-4000-8000-000000000001",
  typ: "WERK_ABLADESTELLE",
  name: TYPNAME,
  aktion: "Anpassung",
  status: "OFFEN",
  einordnung: "OFFEN",
  eingestelltAm: "2026-10-19T10:00:00Z",
  abgeholtAm: null,
  ausgefuehrtAm: null,
  fehlercode: null,
  fehlertext: null,
  eintraege: [
    {
      werte: { werk: "72332", abladestelle: "DDB0", sndprn: "120764" },
      schluessel: "72332/DDB0",
      rueckgabe: null,
      ergebnis: null,
    },
  ],
};

beforeEach(() => {
  rufe = [];
  stand = () => json({ verfuegbar: true, typen: [TYPNAME] });
  eroeffnen = () => json({ gespraechId: "g-1" }, 201);
  nachrichten = [];
  pruefung = () => json({ errors: [] });
  anlegen = [];
  auftragsliste = [];
  document.cookie = "XSRF-TOKEN=test-token";
  vi.stubGlobal("fetch", (eingabe: RequestInfo | URL, init?: RequestInit) => {
    const pfad = String(eingabe);
    const methode = init?.method ?? "GET";
    const koerper =
      typeof init?.body === "string"
        ? (JSON.parse(init.body) as Record<string, unknown>)
        : undefined;
    rufe.push({ methode, pfad, koerper });

    if (methode === "GET" && pfad === "/api/assistent") {
      return Promise.resolve(stand());
    }
    if (methode === "POST" && pfad === "/api/assistent/gespraeche") {
      return Promise.resolve(eroeffnen());
    }
    if (methode === "POST" && pfad.endsWith("/nachrichten")) {
      const naechste = nachrichten.shift();
      return Promise.resolve(
        naechste ? naechste() : json({ text: "Wie kann ich helfen?", vorschlag: null }),
      );
    }
    if (methode === "DELETE") {
      return Promise.resolve(new Response(null, { status: 204 }));
    }
    if (methode === "POST" && pfad === "/api/tasks/pruefung") {
      return Promise.resolve(pruefung(koerper));
    }
    if (methode === "POST" && pfad === "/api/tasks") {
      const naechste = anlegen.shift();
      if (!naechste) {
        auftragsliste = [{ ...AUFTRAG, felder: [] }];
      }
      return Promise.resolve(naechste ? naechste() : json(AUFTRAG, 201));
    }
    if (methode === "GET" && pfad === "/api/tasks") {
      return Promise.resolve(json(auftragsliste));
    }
    return Promise.resolve(problem(404, "nicht-gefunden"));
  });
});

afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

const nur = (methode: string, teil: string) =>
  rufe.filter((ruf) => ruf.methode === methode && ruf.pfad.includes(teil));

// ── Bedienung ───────────────────────────────────────────────────────────────

/** Ein Zug: Antworten kommen an, Floating UI rechnet, Radix setzt den Fokus. */
async function zug() {
  await act(async () => {
    await new Promise((fertig) => setTimeout(fertig, 0));
  });
}

/** Wartet Zug um Zug, bis die Bedingung gilt — höchstens fünfzig Züge. */
async function bis(bedingung: () => boolean, was: string) {
  for (let versuch = 0; versuch < 50 && !bedingung(); versuch += 1) {
    await zug();
  }
  expect(bedingung(), was).toBe(true);
}

async function klick(element: Element | null | undefined) {
  expect(element, "das Element fehlt").toBeTruthy();
  await act(async () => {
    (element as HTMLElement).click();
  });
  await zug();
}

async function taste(element: Element, key: string, zusatz: KeyboardEventInit = {}) {
  await act(async () => {
    element.dispatchEvent(
      new KeyboardEvent("keydown", { key, bubbles: true, cancelable: true, ...zusatz }),
    );
  });
  await zug();
}

/** React hört auf den nativen Setter, nicht auf `value =`. */
async function tippe(element: HTMLTextAreaElement | HTMLInputElement, wert: string) {
  await act(async () => {
    const prototyp =
      element instanceof HTMLTextAreaElement
        ? window.HTMLTextAreaElement.prototype
        : window.HTMLInputElement.prototype;
    Object.getOwnPropertyDescriptor(prototyp, "value")?.set?.call(element, wert);
    element.dispatchEvent(new Event("input", { bubbles: true }));
  });
}

const symbol = () => document.querySelector<HTMLButtonElement>('[data-teil="assistent-symbol"]');
const chatbox = () => document.querySelector<HTMLElement>('[data-teil="chatbox"]');
const eingabe = () => chatbox()?.querySelector("textarea") as HTMLTextAreaElement;
const karte = () => document.querySelector<HTMLElement>('[data-slot="vorschlag-karte"]');
const hinweis = () => chatbox()?.querySelector<HTMLElement>('[data-teil="hinweis"]') ?? null;

function knopf(wurzel: Element | null, text: string): HTMLButtonElement | undefined {
  return [...(wurzel?.querySelectorAll("button") ?? [])].find(
    (kandidat) => (kandidat.textContent ?? "").trim() === text,
  );
}

const sendenKnopf = () => knopf(chatbox(), A.eingabe.senden);

async function eingang(mandant: Mandant = NEXANS) {
  const gerendert = await rendere(<AssistentEingang key={mandant.id} mandant={mandant} />);
  await zug();
  return gerendert;
}

async function oeffne() {
  await klick(symbol());
  await bis(() => chatbox() !== null, "die Chatbox ist offen");
}

/** Schreibt und sendet mit Enter; wartet, bis die Antwort oder ein Hinweis da ist. */
async function sende(text: string) {
  await tippe(eingabe(), text);
  await taste(eingabe(), "Enter");
  await bis(() => chatbox()?.querySelector('[data-teil="wartet"]') === null, "die Antwort ist da");
}

/** Die Chatbox mit einer Karte darin. */
async function mitKarte(vorschlag: Vorschlag = VORSCHLAG) {
  nachrichten.push(() => json({ text: "Bitte prüfen Sie den Vorschlag.", vorschlag }));
  const gerendert = await eingang();
  await oeffne();
  await sende("Bitte bei Werk 72332, Abladestelle DDB0 die SNDPRN auf 120764 setzen");
  await bis(() => karte() !== null, "die Karte ist da");
  return gerendert;
}

/** Wartet die Verzögerung der Prüfung ab (F5). Das ist kein Messen: Erst danach geht der Aufruf hinaus. */
async function warteAufPruefung() {
  await act(async () => {
    await new Promise((fertig) => setTimeout(fertig, PRUEFVERZOEGERUNG_MS + 20));
  });
  await zug();
}

// ── Die Fälle ───────────────────────────────────────────────────────────────

describe("Das Symbol (E‑1068)", () => {
  it("fehlt bei verfuegbar: false — ohne jede weitere Anfrage", async () => {
    stand = () => json({ verfuegbar: false, typen: [] });
    const { abbauen } = await eingang();
    try {
      await bis(() => nur("GET", "/api/assistent").length === 1, "der Stand ist gefragt");
      await zug();
      expect(symbol()).toBeNull();
      expect(rufe.map((ruf) => `${ruf.methode} ${ruf.pfad}`)).toEqual(["GET /api/assistent"]);
    } finally {
      await abbauen();
    }
  });

  it("fehlt, wenn GET /api/assistent scheitert", async () => {
    stand = () => problem(500, "technischer-fehler");
    const { abbauen } = await eingang();
    try {
      await bis(() => nur("GET", "/api/assistent").length >= 1, "der Stand ist gefragt");
      await zug();
      expect(symbol()).toBeNull();
    } finally {
      await abbauen();
    }
  });

  it("steht bei verfuegbar: true links neben dem Nutzermenü, mit Beschriftung und Tooltip", async () => {
    const zwischenspeicher = neuerZwischenspeicher();
    zwischenspeicher.setQueryData(NACHRICHTEN_SCHLUESSEL.suchfelder, ANGEBOT);
    const { abbauen } = await rendere(
      <NuqsTestingAdapter searchParams="" hasMemory>
        <Kopfzeile auskunft={auskunft(NEXANS)} navigationSichtbar mandantenwechselErlaubt={false} />
      </NuqsTestingAdapter>,
      zwischenspeicher,
    );
    try {
      await bis(() => symbol() !== null, "das Symbol steht");
      expect(symbol()?.getAttribute("aria-label")).toBe(A.symbol);
      expect(symbol()?.getAttribute("title")).toBe(A.symbol);
      // Gegenprobe zur Stelle: Das nächste Bedienelement ist der Knopf des Nutzermenüs.
      const nachbar = symbol()?.nextElementSibling;
      expect(nachbar?.getAttribute("aria-label")).toBe(TEXTE.nutzermenue.oeffnen);
    } finally {
      await abbauen();
    }
  });
});

describe("Tastatur und Fokus", () => {
  it("öffnet mit dem Fokus im Eingabefeld, begrüßt mit den Typen, Esc schließt und gibt den Fokus dem Symbol zurück", async () => {
    const { abbauen } = await eingang();
    try {
      await oeffne();
      expect(document.activeElement).toBe(eingabe());
      expect(symbol()?.getAttribute("aria-expanded")).toBe("true");

      // Die Begrüßung ist ein fester Text mit den Typen aus GET /api/assistent —
      // und kein Aufruf des Modells. Drei Zeilen, die Liste zwischen der zweiten
      // und der dritten, in dieser Reihenfolge.
      const begruessung = chatbox()?.querySelector('[data-beitrag="begruessung"]');
      const zeilen = [...(begruessung?.querySelectorAll("p, li") ?? [])].map(
        (zeile) => zeile.textContent,
      );
      expect(zeilen).toEqual([
        "Guten Tag, ich bin der Assistent des Overlord Monitorings.",
        "Ich nehme Ihre Änderungswünsche an der EDI-Konfiguration entgegen und bereite daraus einen Auftrag vor. Derzeit möglich:",
        TYPNAME,
        "Beschreiben Sie einfach, was Sie ändern möchten. Bevor etwas angelegt wird, zeige ich Ihnen den Auftrag zur Prüfung.",
      ]);
      expect(nur("POST", "/api/assistent")).toEqual([]);

      await taste(eingabe(), "Escape");
      await bis(() => chatbox() === null, "die Chatbox ist zu");
      await bis(() => document.activeElement === symbol(), "der Fokus steht auf dem Symbol");
    } finally {
      await abbauen();
    }
  });

  it("schließt mit dem Knopf im Kopf, ebenfalls mit dem Fokus zurück am Symbol", async () => {
    const { abbauen } = await eingang();
    try {
      await oeffne();
      const schliessen = chatbox()?.querySelector(`[aria-label="${A.schliessen}"]`);
      await klick(schliessen);
      await bis(() => chatbox() === null, "die Chatbox ist zu");
      await bis(() => document.activeElement === symbol(), "der Fokus steht auf dem Symbol");
    } finally {
      await abbauen();
    }
  });
});

describe("Senden und Warten (E‑1075, E‑1082)", () => {
  it("eröffnet mit der ersten Nachricht, sperrt während der Antwort und zeigt sie danach", async () => {
    const antwort = haengend();
    nachrichten.push(antwort.bedienung);
    const { abbauen } = await eingang();
    try {
      await oeffne();
      // Umschalt+Enter bricht um und sendet nicht.
      await tippe(eingabe(), "Zeile eins");
      await taste(eingabe(), "Enter", { shiftKey: true });
      expect(nur("POST", "/api/assistent")).toEqual([]);

      await taste(eingabe(), "Enter");
      await bis(() => nur("POST", "/nachrichten").length === 1, "die Nachricht ist unterwegs");
      // Erst eröffnen, dann senden — mit dem Text als einzigem Inhalt.
      expect(nur("POST", "/api/assistent").map((ruf) => ruf.pfad)).toEqual([
        "/api/assistent/gespraeche",
        "/api/assistent/gespraeche/g-1/nachrichten",
      ]);
      expect(nur("POST", "/nachrichten")[0].koerper).toEqual({ text: "Zeile eins" });

      // Während des Wartens: Anzeige, Feld gesperrt, Senden gesperrt, angesagt.
      expect(chatbox()?.querySelector('[data-teil="wartet"]')?.textContent).toContain(A.wartet);
      expect(eingabe().readOnly).toBe(true);
      expect(eingabe().getAttribute("aria-disabled")).toBe("true");
      expect(sendenKnopf()?.getAttribute("aria-disabled")).toBe("true");
      expect(document.activeElement).toBe(eingabe());
      expect(chatbox()?.querySelector('[data-teil="ansage"]')?.textContent).toBe(A.wartet);
      // Ein zweites Enter schickt nichts.
      await tippe(eingabe(), "noch eine");
      await taste(eingabe(), "Enter");
      expect(nur("POST", "/nachrichten")).toHaveLength(1);

      antwort.loese(json({ text: "Welches Werk meinen Sie?", vorschlag: null }));
      await bis(
        () => chatbox()?.querySelector('[data-teil="wartet"]') === null,
        "die Antwort ist da",
      );
      const texte = [...(chatbox()?.querySelectorAll('[data-teil="text"]') ?? [])].map(
        (absatz) => absatz.textContent,
      );
      expect(texte).toEqual(["Zeile eins", "Welches Werk meinen Sie?"]);
      expect(eingabe().readOnly).toBe(false);
      expect(chatbox()?.querySelector('[data-teil="ansage"]')?.textContent).toBe(
        "Assistent: Welches Werk meinen Sie?",
      );
    } finally {
      await abbauen();
    }
  });

  it("zeigt Text, der wie HTML oder Markdown aussieht, wörtlich als einen Textknoten (E‑1074)", async () => {
    const roh =
      "<img src=x onerror=alert(1)> und **fett**\nzweite Zeile [Link](https://example.org)";
    nachrichten.push(() => json({ text: roh, vorschlag: null }));
    const { abbauen } = await eingang();
    try {
      await oeffne();
      await sende(roh);
      const absaetze = [...(chatbox()?.querySelectorAll('[data-teil="text"]') ?? [])];
      expect(absaetze).toHaveLength(2);
      for (const absatz of absaetze) {
        expect(absatz.childNodes).toHaveLength(1);
        expect(absatz.firstChild?.nodeType).toBe(Node.TEXT_NODE);
        expect(absatz.textContent).toBe(roh);
      }
      expect(chatbox()?.querySelector("img, strong, b, a, em")).toBeNull();
    } finally {
      await abbauen();
    }
  });

  it("übersteht das Schließen: derselbe Verlauf, dasselbe Gespräch (E‑1076)", async () => {
    nachrichten.push(() => json({ text: "Erste Antwort.", vorschlag: null }));
    const { abbauen } = await eingang();
    try {
      await oeffne();
      await sende("Erste Frage");
      await tippe(eingabe(), "Entwurf, noch nicht gesendet");
      await taste(eingabe(), "Escape");
      await bis(() => chatbox() === null, "die Chatbox ist zu");

      await oeffne();
      const texte = [...(chatbox()?.querySelectorAll('[data-teil="text"]') ?? [])].map(
        (absatz) => absatz.textContent,
      );
      expect(texte).toEqual(["Erste Frage", "Erste Antwort."]);
      expect(eingabe().value).toBe("Entwurf, noch nicht gesendet");

      await taste(eingabe(), "Enter");
      await bis(
        () => nur("POST", "/nachrichten").length === 2,
        "die zweite Nachricht ist unterwegs",
      );
      expect(
        nur("POST", "/api/assistent/gespraeche").filter((ruf) => ruf.pfad.endsWith("gespraeche")),
      ).toHaveLength(1);
    } finally {
      await abbauen();
    }
  });

  it("beendet mit „Neues Gespräch“ das alte über DELETE und beginnt bei der Begrüßung", async () => {
    const { abbauen } = await eingang();
    try {
      await oeffne();
      await sende("Erste Frage");
      await klick(knopf(chatbox(), A.neuesGespraech));
      await bis(
        () => nur("DELETE", "/api/assistent").length === 1,
        "das alte Gespräch ist beendet",
      );
      expect(nur("DELETE", "/api/assistent")[0].pfad).toBe("/api/assistent/gespraeche/g-1");
      expect(
        [...(chatbox()?.querySelectorAll("[data-beitrag]") ?? [])].map((li) =>
          li.getAttribute("data-beitrag"),
        ),
      ).toEqual(["begruessung"]);
      // Eröffnet wird erst wieder mit der nächsten Nachricht (E‑1082).
      expect(
        nur("POST", "/api/assistent/gespraeche").filter((ruf) => ruf.pfad.endsWith("gespraeche")),
      ).toHaveLength(1);
    } finally {
      await abbauen();
    }
  });
});

describe("Die Karte (E‑1069, E‑1070, E‑1071)", () => {
  it("nennt Typ, Aktion und Mandanten und zeigt jedes Feld, leere ausdrücklich mit Hinweis", async () => {
    const { abbauen } = await mitKarte();
    try {
      const wurzel = karte()!;
      expect(wurzel.querySelector("h3")?.textContent).toBe(TYPNAME);
      expect(wurzel.textContent).toContain("Anpassung");
      const mandant = wurzel.querySelector('[data-teil="mandant"]')?.textContent ?? "";
      expect(mandant).toContain(K.giltFuer);
      expect(mandant).toContain("NEXANS");
      expect(mandant).toContain("Nexans autoelectric GmbH");
      expect(wurzel.querySelector("h4")?.textContent).toContain("72332/DDB0");

      const zelle = (feld: string) => wurzel.querySelector(`[data-feld="${feld}"]`);
      expect([...wurzel.querySelectorAll("dt")].map((dt) => dt.textContent)).toEqual([
        "Lieferantennummer",
        "Werk",
        "Abladestelle",
        "SNDPRN",
        "SNDPRN_4908",
      ]);
      expect(zelle("werk")?.querySelector('[data-teil="wert"]')?.textContent).toBe("72332");
      // Leer mit Hinweis: das Wort und darunter die Bedeutung aus der Definition.
      expect(zelle("sndprn4908")?.querySelector('[data-teil="leer"]')?.textContent).toBe(K.leer);
      expect(zelle("sndprn4908")?.querySelector('[data-teil="hinweis"]')?.textContent).toBe(
        "Für VDA-4908-Prozesse. Bei Anpassung heißt leer: der Wert wird entfernt.",
      );
      // Leer ohne Hinweis: nur das Wort.
      expect(zelle("lieferantennummer")?.querySelector('[data-teil="leer"]')).not.toBeNull();
      expect(zelle("lieferantennummer")?.querySelector('[data-teil="hinweis"]')).toBeNull();
      // Ein Feld mit Wert trägt keinen Hinweis, auch wenn es einen hätte.
      expect(zelle("sndprn")?.querySelector('[data-teil="hinweis"]')).toBeNull();

      expect(chatbox()?.querySelector('[data-teil="ansage"]')?.textContent).toContain(
        `Vorschlag auf der Karte: ${TYPNAME}, ein Eintrag.`,
      );
    } finally {
      await abbauen();
    }
  });

  it("prüft Änderungen über das Backend, zeigt den Fehler am Feld und sperrt „Übernehmen“, bis die Prüfung frei ist", async () => {
    const { abbauen } = await mitKarte();
    try {
      await klick(knopf(karte(), K.aendern));
      const sndprn = karte()!.querySelector<HTMLInputElement>('[data-feld="sndprn"] input')!;
      // Der Fokus steht im ersten Feld.
      expect(document.activeElement).toBe(
        karte()!.querySelector('[data-feld="lieferantennummer"] input'),
      );
      const uebernehmen = () => knopf(karte(), K.uebernehmen);
      expect(uebernehmen()?.getAttribute("aria-disabled")).toBe("false");

      const fehlerhaft = haengend();
      pruefung = () => fehlerhaft.bedienung();
      await tippe(sndprn, "12076");
      // Sofort gesperrt — die Prüfung ist noch nicht einmal hinaus.
      expect(uebernehmen()?.getAttribute("aria-disabled")).toBe("true");
      expect(nur("POST", "/api/tasks/pruefung")).toEqual([]);

      await warteAufPruefung();
      await bis(() => nur("POST", "/api/tasks/pruefung").length === 1, "die Prüfung ist hinaus");
      expect(nur("POST", "/api/tasks/pruefung")[0].koerper).toEqual({
        typ: "WERK_ABLADESTELLE",
        aktion: "Anpassung",
        eintraege: [
          {
            lieferantennummer: null,
            werk: "72332",
            abladestelle: "DDB0",
            sndprn: "12076",
            sndprn4908: null,
          },
        ],
      });
      // Während die Prüfung läuft: weiter gesperrt.
      expect(uebernehmen()?.getAttribute("aria-disabled")).toBe("true");
      expect(karte()!.querySelector('[data-teil="pruefstand"]')?.textContent).toBe(
        K.pruefungLaeuft,
      );

      const meldung = "Eintrag 1, SNDPRN: Der Wert „12076“ hat nicht die erwartete Form.";
      fehlerhaft.loese(json({ errors: [{ index: 0, feld: "sndprn", code: "FORMAT", meldung }] }));
      await bis(
        () => karte()!.querySelector('[data-feld="sndprn"] [data-teil="fehler"]') !== null,
        "der Fehler steht am Feld",
      );
      expect(karte()!.querySelector('[data-feld="sndprn"] [data-teil="fehler"]')?.textContent).toBe(
        meldung,
      );
      expect(sndprn.getAttribute("aria-invalid")).toBe("true");
      expect(uebernehmen()?.getAttribute("aria-disabled")).toBe("true");
      await klick(uebernehmen());
      expect(karte()!.getAttribute("data-modus")).toBe("aendern");

      pruefung = () => json({ errors: [] });
      await tippe(sndprn, "120765");
      await warteAufPruefung();
      await bis(
        () => uebernehmen()?.getAttribute("aria-disabled") === "false",
        "„Übernehmen“ ist frei",
      );
      expect(karte()!.querySelector('[data-teil="fehler"]')).toBeNull();

      await klick(uebernehmen());
      expect(karte()!.getAttribute("data-modus")).toBe("ansicht");
      expect(karte()!.querySelector('[data-feld="sndprn"] [data-teil="wert"]')?.textContent).toBe(
        "120765",
      );
      // E‑1080: Der Schlüssel aus dem Vorschlag passt nicht mehr sicher — Rückfall.
      expect(karte()!.querySelector("h4")?.textContent).toBe("Eintrag 1");
      expect(document.activeElement).toBe(knopf(karte(), K.aendern));
    } finally {
      await abbauen();
    }
  });

  it("zeigt bis fünf Einträge offen und darüber „Alle n anzeigen“ (F6)", async () => {
    const { abbauen } = await mitKarte(mitEintraegen(20));
    try {
      expect(karte()!.querySelectorAll("[data-eintrag]")).toHaveLength(5);
      const alle = knopf(karte(), "Alle 20 anzeigen");
      expect(alle?.getAttribute("aria-expanded")).toBe("false");
      await klick(alle);
      expect(karte()!.querySelectorAll("[data-eintrag]")).toHaveLength(20);
      expect(knopf(karte(), "Nur die ersten 5 anzeigen")?.getAttribute("aria-expanded")).toBe(
        "true",
      );
    } finally {
      await abbauen();
    }
  });
});

describe("Anlegen (E‑1072, E‑1073)", () => {
  it("schickt die Werte der Karte mit ihrer anfrageId, beim zweiten Versuch dieselbe, und sperrt die Karte danach", async () => {
    anlegen.push(() => problem(503, "technischer-fehler"));
    const { abbauen } = await mitKarte();
    try {
      await klick(knopf(karte(), K.anlegen));
      await bis(
        () => karte()!.querySelector('[data-teil="pruefstand"]')?.textContent === K.anlegenGestoert,
        "der Hinweis auf der Karte steht",
      );
      const erster = nur("POST", "/api/tasks")[0].koerper!;
      expect(erster).toEqual({
        anfrageId: expect.stringMatching(/^[0-9a-f-]{36}$/),
        typ: "WERK_ABLADESTELLE",
        aktion: "Anpassung",
        eintraege: [
          {
            lieferantennummer: null,
            werk: "72332",
            abladestelle: "DDB0",
            sndprn: "120764",
            sndprn4908: null,
          },
        ],
      });

      await klick(knopf(karte(), K.anlegen));
      await bis(() => karte()!.querySelector('[data-teil="angelegt"]') !== null, "angelegt");
      const zweiter = nur("POST", "/api/tasks").filter((ruf) => ruf.pfad === "/api/tasks")[1]
        .koerper!;
      expect(zweiter.anfrageId).toBe(erster.anfrageId);

      const status = karte()!.querySelector('[data-teil="angelegt"]')!;
      expect(status.textContent).toBe("Angelegt, Status: Offen");
      expect(document.activeElement).toBe(status);
      // Gesperrt: kein Knopf mehr auf der Karte, und ein dritter Aufruf ging nie hinaus.
      expect(knopf(karte(), K.anlegen)).toBeUndefined();
      expect(knopf(karte(), K.aendern)).toBeUndefined();
      expect(karte()!.querySelectorAll("input")).toHaveLength(0);
      expect(nur("POST", "/api/tasks").filter((ruf) => ruf.pfad === "/api/tasks")).toHaveLength(2);
    } finally {
      await abbauen();
    }
  });

  it("zeigt Prüffehler beim Anlegen am Feld, lässt die Karte änderbar und sperrt das Anlegen (F8)", async () => {
    const meldung = "Eintrag 1, Werk: Der Wert „72332“ hat nicht die erwartete Form.";
    anlegen.push(() =>
      problem(400, "eingabe-ungueltig", {
        errors: [{ index: 0, feld: "werk", code: "FORMAT", meldung }],
      }),
    );
    const { abbauen } = await mitKarte();
    try {
      await klick(knopf(karte(), K.anlegen));
      await bis(
        () => karte()!.querySelector('[data-feld="werk"] [data-teil="fehler"]') !== null,
        "der Fehler steht am Feld",
      );
      expect(karte()!.querySelector('[data-feld="werk"] [data-teil="fehler"]')?.textContent).toBe(
        meldung,
      );
      expect(knopf(karte(), K.anlegen)?.getAttribute("aria-disabled")).toBe("true");
      expect(knopf(karte(), K.aendern)?.getAttribute("aria-disabled")).toBe("false");

      await klick(knopf(karte(), K.anlegen));
      expect(nur("POST", "/api/tasks").filter((ruf) => ruf.pfad === "/api/tasks")).toHaveLength(1);

      await klick(knopf(karte(), K.aendern));
      const werk = karte()!.querySelector<HTMLInputElement>('[data-feld="werk"] input')!;
      expect(werk.getAttribute("aria-invalid")).toBe("true");
    } finally {
      await abbauen();
    }
  });
});

describe("Verhalten bei Fehlern (§6)", () => {
  const FAELLE: {
    name: string;
    antwort: () => Response;
    text: string;
    neuesGespraech: boolean;
  }[] = [
    {
      name: "429 zu-viele-nachrichten: bitte kurz warten",
      antwort: () => problem(429, "zu-viele-nachrichten", { hoechstensJeStunde: 60 }),
      text: A.hinweis.zuVieleNachrichten,
      neuesGespraech: false,
    },
    {
      name: "429 gespraech-ausgeschoepft: Höchstzahl aus dem Fehlerformat und „Neues Gespräch“ (E‑1081)",
      antwort: () => problem(429, "gespraech-ausgeschoepft", { hoechstensNachrichten: 20 }),
      text: "Dieses Gespräch hat die Höchstzahl von 20 Nachrichten erreicht. Bitte beginnen Sie ein neues Gespräch.",
      neuesGespraech: true,
    },
    {
      name: "400 nachricht-ungueltig: Höchstzahl aus dem Fehlerformat",
      antwort: () => problem(400, "nachricht-ungueltig", { hoechstensZeichen: 4000 }),
      text: "Die Nachricht ist leer oder zu lang. Bitte schreiben Sie höchstens 4.000 Zeichen.",
      neuesGespraech: false,
    },
    {
      name: "502: nicht erreichbar",
      antwort: () => problem(502, "assistent-gestoert"),
      text: A.hinweis.nichtErreichbar,
      neuesGespraech: false,
    },
    {
      name: "503: nicht erreichbar",
      antwort: () => problem(503, "assistent-voruebergehend-gestoert"),
      text: A.hinweis.nichtErreichbar,
      neuesGespraech: false,
    },
  ];

  for (const fall of FAELLE) {
    it(`${fall.name}; die Eingabe bleibt erhalten`, async () => {
      nachrichten.push(fall.antwort);
      const { abbauen } = await eingang();
      try {
        await oeffne();
        await sende("Meine Nachricht");
        await bis(() => hinweis() !== null, "der Hinweis steht");
        expect(hinweis()?.textContent).toContain(fall.text);
        expect(hinweis()?.getAttribute("role")).toBe("alert");
        expect(eingabe().value).toBe("Meine Nachricht");
        // Die gescheiterte Nachricht steht nicht im Verlauf (F4).
        expect(chatbox()?.querySelector('[data-beitrag="kunde"]')).toBeNull();
        expect(Boolean(knopf(hinweis(), A.neuesGespraech))).toBe(fall.neuesGespraech);
        // Erneut senden geht.
        expect(sendenKnopf()?.getAttribute("aria-disabled")).toBe("false");
      } finally {
        await abbauen();
      }
    });
  }

  it("503 schon beim Eröffnen: nicht erreichbar, und der nächste Versuch eröffnet neu", async () => {
    eroeffnen = () => problem(503, "assistent-nicht-verfuegbar");
    const { abbauen } = await eingang();
    try {
      await oeffne();
      await sende("Meine Nachricht");
      await bis(() => hinweis() !== null, "der Hinweis steht");
      expect(hinweis()?.textContent).toContain(A.hinweis.nichtErreichbar);
      expect(nur("POST", "/nachrichten")).toEqual([]);

      eroeffnen = () => json({ gespraechId: "g-2" }, 201);
      await taste(eingabe(), "Enter");
      await bis(() => nur("POST", "/nachrichten").length === 1, "die Nachricht ist unterwegs");
      expect(nur("POST", "/nachrichten")[0].pfad).toBe("/api/assistent/gespraeche/g-2/nachrichten");
    } finally {
      await abbauen();
    }
  });

  it("409 nachricht-laeuft: nichts angezeigt, nur gemeldet (F12); die Eingabe bleibt erhalten", async () => {
    const warnung = vi.spyOn(console, "warn").mockImplementation(() => {});
    nachrichten.push(() => problem(409, "nachricht-laeuft"));
    const { abbauen } = await eingang();
    try {
      await oeffne();
      await sende("Meine Nachricht");
      await bis(() => eingabe().value === "Meine Nachricht", "die Eingabe ist zurück");
      expect(hinweis()).toBeNull();
      expect(warnung).toHaveBeenCalledTimes(1);
    } finally {
      await abbauen();
    }
  });

  it("404 auf das Gespräch: abgelaufen, Senden gesperrt, „Neues Gespräch“ ohne DELETE und mit neuem Gespräch beim nächsten Senden", async () => {
    nachrichten.push(() => json({ text: "Erste Antwort.", vorschlag: null }));
    nachrichten.push(() => problem(404, "nicht-gefunden"));
    const { abbauen } = await eingang();
    try {
      await oeffne();
      await sende("Erste Frage");
      await sende("Zweite Frage");
      await bis(() => hinweis() !== null, "der Hinweis steht");
      expect(hinweis()?.textContent).toContain(A.hinweis.abgelaufen);
      expect(eingabe().value).toBe("Zweite Frage");
      expect(sendenKnopf()?.getAttribute("aria-disabled")).toBe("true");
      await taste(eingabe(), "Enter");
      expect(nur("POST", "/nachrichten")).toHaveLength(2);

      eroeffnen = () => json({ gespraechId: "g-2" }, 201);
      await klick(knopf(hinweis(), A.neuesGespraech));
      // Ein abgelaufenes Gespräch gibt es nicht mehr — nichts zu beenden.
      expect(nur("DELETE", "/api/assistent")).toEqual([]);
      expect(hinweis()).toBeNull();
      expect(eingabe().value).toBe("Zweite Frage");

      await taste(eingabe(), "Enter");
      await bis(() => nur("POST", "/nachrichten").length === 3, "die Nachricht ist unterwegs");
      expect(nur("POST", "/nachrichten")[2].pfad).toBe("/api/assistent/gespraeche/g-2/nachrichten");
    } finally {
      await abbauen();
    }
  });
});

// ── Mandantenwechsel ────────────────────────────────────────────────────────

const ANGEBOT: Suchfelder = { bam: [], felder: [] };

function auskunft(mandant: Mandant): Selbstauskunft {
  return {
    username: "beispielnutzer",
    role: "ADMIN",
    mandant,
    mustChangePassword: false,
    anzeigezone: "Europe/Berlin",
  };
}

/** Die Kopfzeile, deren Mandant ein Ereignis am Fenster umstellt — wie nach dem Wechsel. */
function WechselbareKopfzeile() {
  const [mandant, setzeMandant] = useState<Mandant>(NEXANS);
  useEffect(() => {
    const beiWechsel = (ereignis: Event) => setzeMandant((ereignis as CustomEvent<Mandant>).detail);
    window.addEventListener("test-mandantenwechsel", beiWechsel);
    return () => window.removeEventListener("test-mandantenwechsel", beiWechsel);
  }, []);
  return (
    <Kopfzeile auskunft={auskunft(mandant)} navigationSichtbar mandantenwechselErlaubt={false} />
  );
}

describe("Mandantenwechsel (E‑1076)", () => {
  it("setzt die Chatbox zurück: kein Verlauf, kein Gespräch des alten Mandanten", async () => {
    const zwischenspeicher = neuerZwischenspeicher();
    zwischenspeicher.setQueryData(NACHRICHTEN_SCHLUESSEL.suchfelder, ANGEBOT);
    nachrichten.push(() => json({ text: "Bitte prüfen Sie den Vorschlag.", vorschlag: VORSCHLAG }));
    const { abbauen } = await rendere(
      <NuqsTestingAdapter searchParams="" hasMemory>
        <WechselbareKopfzeile />
      </NuqsTestingAdapter>,
      zwischenspeicher,
    );
    try {
      await bis(() => symbol() !== null, "das Symbol steht");
      await oeffne();
      await sende("Erste Frage");
      await bis(() => karte() !== null, "die Karte ist da");
      await tippe(eingabe(), "Entwurf für NEXANS");
      await taste(eingabe(), "Escape");
      await bis(() => chatbox() === null, "die Chatbox ist zu");

      await act(async () => {
        window.dispatchEvent(new CustomEvent("test-mandantenwechsel", { detail: IBIS }));
      });
      await bis(() => symbol() !== null, "das Symbol steht wieder");
      await oeffne();
      expect(
        [...(chatbox()?.querySelectorAll("[data-beitrag]") ?? [])].map((li) =>
          li.getAttribute("data-beitrag"),
        ),
      ).toEqual(["begruessung"]);
      expect(karte()).toBeNull();
      expect(eingabe().value).toBe("");
      expect(chatbox()?.querySelector('[data-teil="mandant"]')?.textContent).toContain("IBIS");

      // Das nächste Senden eröffnet ein neues Gespräch, statt g-1 weiterzuführen.
      eroeffnen = () => json({ gespraechId: "g-2" }, 201);
      await sende("Frage für IBIS");
      expect(nur("POST", "/nachrichten").at(-1)?.pfad).toBe(
        "/api/assistent/gespraeche/g-2/nachrichten",
      );
    } finally {
      await abbauen();
    }
  });
});

describe("Nach dem Anlegen (A4, E‑1109)", () => {
  /**
   * **Die eine Änderung an A3:** Nach dem Anlegen lädt die Liste der Aufträge
   * neu, und der Block „Ihre Aufträge“ erscheint in der Übersicht, **ohne die
   * Seite neu zu laden**. Chatbox und Übersicht teilen sich hier den
   * Zwischenspeicher wie in der Anwendung; die Übersicht selbst antwortet nicht
   * (`404`) — dann steht der Block allein unter ihrer Meldung (E‑1121).
   */
  it("lässt „Ihre Aufträge“ erscheinen, ohne die Seite neu zu laden", async () => {
    const ihreAuftraege = () =>
      [...document.querySelectorAll("section")].find(
        (abschnitt) =>
          abschnitt.querySelector("h2")?.textContent === TEXTE.dashboard.auftraege.titel,
      ) ?? null;
    nachrichten.push(() => json({ text: "Bitte prüfen Sie den Vorschlag.", vorschlag: VORSCHLAG }));
    const { abbauen } = await rendere(
      <NuqsTestingAdapter hasMemory>
        <AssistentEingang key={NEXANS.id} mandant={NEXANS} />
        <DashboardAnsicht />
      </NuqsTestingAdapter>,
    );
    try {
      await bis(() => nur("GET", "/api/tasks").length === 1, "die Liste ist gefragt");
      await zug();
      expect(ihreAuftraege(), "vor dem Anlegen kein Block").toBeNull();

      await oeffne();
      await sende("Bitte bei Werk 72332, Abladestelle DDB0 die SNDPRN auf 120764 setzen");
      await bis(() => karte() !== null, "die Karte ist da");
      await klick(knopf(karte(), K.anlegen));

      await bis(() => ihreAuftraege() !== null, "der Block steht");
      expect(rufe.filter((ruf) => ruf.methode === "GET" && ruf.pfad === "/api/tasks")).toHaveLength(
        2,
      );
      expect(ihreAuftraege()!.textContent).toContain("72332/DDB0");
    } finally {
      await abbauen();
    }
  });
});
