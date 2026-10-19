// @vitest-environment jsdom

import { act } from "react";
import { NuqsTestingAdapter } from "nuqs/adapters/testing";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { ZeitzoneProvider } from "@/components/zeitzone";
import type { Auftrag, Auftragseintrag, Dashboard } from "@/features/dashboard/api";
import { DashboardAnsicht } from "@/features/dashboard/components/dashboard-ansicht";
import { texteFuer } from "@/i18n";
import { auftragVordergrund, type Auftragseinordnung } from "@/lib/status-farbe";

import { rendere } from "./hilfe/rendern";

/**
 * **„Ihre Aufträge“ im Baum, gegen gestellte Endpunkte** (Linie `assistent`,
 * A4, `docs/assistent-frontend.md` §12).
 *
 * Die Regeln selbst sind reine Funktionen und stehen in
 * `tests/auftraege.test.ts`. Hier steht, was nur der Baum belegt: **ob und wo
 * der Block steht** (Abwesenheit, Reihenfolge, die Spalten der Reihe), **was
 * im Dokument ankommt** (Wort und Zeichen, Textknoten statt HTML), **welche
 * Anfragen hinausgehen** und **wo der Fokus steht**.
 *
 * Kein Netz, keine Datenbank: `fetch` ist gestellt. Keine Zusicherung über
 * Wanduhrzeit (T1); die Zeitpunkte sind feste Werte, die Zone ist gesetzt, und
 * gewartet wird auf Zustände.
 */

const TEXTE = texteFuer("de");
const T = TEXTE.dashboard.auftraege;
const ZONE = "Europe/Berlin";

// ── Gestellte Antworten ─────────────────────────────────────────────────────

function dashboard(teil: Partial<Dashboard> = {}): Dashboard {
  return {
    zeitraum: "48H",
    fenster: { von: "2026-10-17T12:00:00Z", bis: "2026-10-19T12:00:00Z" },
    leer: false,
    verlauf: [
      {
        eimer: "2026-10-19T10:00:00Z",
        gesamt: 3,
        einordnungen: [{ einordnung: "ABGESCHLOSSEN", anzahl: 3 }],
      },
    ],
    kacheln: {
      nachrichten: 3,
      fehler: { anzahl: 0, arten: [] },
      laeuft: { anzahl: 0, aeltesteSekunden: null, ermittelbar: true },
      wartend: { anzahl: 0, aeltesteSekunden: null, ermittelbar: true },
    },
    verteilung: {
      partner: { zeilen: [{ art: "NICHT_ZUGEORDNET", wert: null, anzahl: 0, enthaltene: null }] },
      richtung: { zeilen: [{ art: "NICHT_ZUGEORDNET", wert: null, anzahl: 0, enthaltene: null }] },
    },
    zuletztAufgefallen: [],
    stand: { beendetAm: "2026-10-19T11:10:00Z", art: "VOLL" },
    liveRest: { zustand: "NICHT_NOETIG", vollstaendigBis: null },
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
    ...teil,
  };
}

const FELDER = [
  { name: "lieferantennummer", label: "Lieferantennummer" },
  { name: "werk", label: "Werk" },
  { name: "abladestelle", label: "Abladestelle" },
  { name: "sndprn", label: "SNDPRN" },
  { name: "sndprn4908", label: "SNDPRN_4908" },
];

/** Ein Eintrag mit erfundenen Werten (Werk 47110, Tore wie in der Sichtprobe M1004). */
function eintrag(tor: string, teil: Partial<Auftragseintrag> = {}): Auftragseintrag {
  return {
    werte: { werk: "47110", abladestelle: tor, sndprn: "120764" },
    schluessel: `47110/${tor}`,
    rueckgabe: null,
    ergebnis: null,
    ...teil,
  };
}

let laufendeNummer = 0;

function auftrag(teil: Partial<Auftrag> = {}): Auftrag {
  laufendeNummer += 1;
  return {
    taskId: `00000000-0000-4000-8000-${String(laufendeNummer).padStart(12, "0")}`,
    typ: "WERK_ABLADESTELLE",
    name: "Anpassen der Werk-/Abladestellentabelle",
    aktion: "Neuanlage",
    status: "OFFEN",
    einordnung: "OFFEN",
    eingestelltAm: "2026-10-19T12:32:10Z",
    abgeholtAm: null,
    ausgefuehrtAm: null,
    fehlercode: null,
    fehlertext: null,
    eintraege: [eintrag("TOR3"), eintrag("TOR4")],
    felder: FELDER,
    ...teil,
  };
}

/** Ein Auftrag in einem Stand, mit allem, was die Plattform dazu geschrieben hätte. */
function imStand(einordnung: Auftragseinordnung): Auftrag {
  switch (einordnung) {
    case "OFFEN":
      return auftrag();
    case "IN_AUSFUEHRUNG":
      return auftrag({
        status: "IN_AUSFUEHRUNG",
        einordnung,
        abgeholtAm: "2026-10-19T12:35:00Z",
      });
    case "ERLEDIGT":
      return auftrag({
        status: "ERLEDIGT",
        einordnung,
        abgeholtAm: "2026-10-19T12:35:00Z",
        ausgefuehrtAm: "2026-10-19T12:36:00Z",
        eintraege: [
          eintrag("TOR3", { rueckgabe: "Ok", ergebnis: "OK" }),
          eintrag("TOR4", { rueckgabe: "Ok", ergebnis: "OK" }),
        ],
      });
    case "TEILWEISE_ERLEDIGT":
      return auftrag({
        status: "ERLEDIGT",
        einordnung,
        abgeholtAm: "2026-10-19T12:35:00Z",
        ausgefuehrtAm: "2026-10-19T12:36:00Z",
        eintraege: [
          eintrag("TOR3", { rueckgabe: "Ok", ergebnis: "OK" }),
          eintrag("TOR4", { rueckgabe: "Schlüssel nicht gefunden", ergebnis: "NICHT_OK" }),
        ],
      });
    case "FEHLGESCHLAGEN":
      return auftrag({
        status: "FEHLGESCHLAGEN",
        einordnung,
        abgeholtAm: "2026-10-19T12:35:00Z",
        ausgefuehrtAm: "2026-10-19T12:36:00Z",
        fehlercode: "TABELLE_GESPERRT",
        fehlertext: "Die Tabelle konnte nicht gespeichert werden.",
      });
    case "UNSTIMMIG":
      return auftrag({
        status: "ERLEDIGT",
        einordnung,
        abgeholtAm: "2026-10-19T12:35:00Z",
        ausgefuehrtAm: "2026-10-19T12:36:00Z",
        eintraege: [eintrag("TOR3", { rueckgabe: "Ok", ergebnis: "OK" }), eintrag("TOR4")],
      });
    case "UNBEKANNT":
      return auftrag({ status: "ZURUECKGESTELLT", einordnung });
  }
}

// ── Das gestellte Netz ──────────────────────────────────────────────────────

type Ruf = { methode: string; pfad: string };

let rufe: Ruf[] = [];
let uebersicht: () => Response | Promise<Response>;
let liste: () => Response | Promise<Response>;
let loeschen: () => Response | Promise<Response>;

function json(rumpf: unknown, status = 200): Response {
  return new Response(JSON.stringify(rumpf), {
    status,
    headers: { "content-type": "application/json" },
  });
}

function problem(status: number, typ: string): Response {
  return new Response(
    JSON.stringify({
      type: `https://overlord.kraftwerkone.de/probleme/${typ}`,
      title: typ,
      status,
      detail: "Text aus dem Backend.",
      traceId: "spur-1",
    }),
    { status, headers: { "content-type": "application/problem+json" } },
  );
}

/** Stellt die Liste der Aufträge — bei jedem Abruf dieselbe, bis der Test sie ändert. */
function mitAuftraegen(auftraege: Auftrag[]): void {
  liste = () => json(auftraege);
}

beforeEach(() => {
  rufe = [];
  laufendeNummer = 0;
  uebersicht = () => json(dashboard());
  mitAuftraegen([]);
  loeschen = () => new Response(null, { status: 204 });
  document.cookie = "XSRF-TOKEN=test-token";
  vi.stubGlobal("fetch", (eingabe: RequestInfo | URL, init?: RequestInit) => {
    const pfad = String(eingabe);
    const methode = init?.method ?? "GET";
    rufe.push({ methode, pfad });
    if (methode === "GET" && pfad.startsWith("/api/dashboard")) {
      return Promise.resolve(uebersicht());
    }
    if (methode === "GET" && pfad === "/api/tasks") {
      return Promise.resolve(liste());
    }
    if (methode === "DELETE" && pfad.startsWith("/api/tasks/")) {
      return Promise.resolve(loeschen());
    }
    if (methode === "GET" && pfad === "/api/assistent") {
      return Promise.resolve(json({ verfuegbar: true, typen: ["Anpassen"] }));
    }
    return Promise.resolve(problem(404, "nicht-gefunden"));
  });
});

afterEach(() => {
  vi.unstubAllGlobals();
});

const nur = (methode: string, pfad: string) =>
  rufe.filter((ruf) => ruf.methode === methode && ruf.pfad === pfad);

// ── Bedienung ───────────────────────────────────────────────────────────────

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

async function taste(element: Element, key: string) {
  await act(async () => {
    element.dispatchEvent(new KeyboardEvent("keydown", { key, bubbles: true, cancelable: true }));
  });
  await zug();
}

async function ansicht() {
  const gerendert = await rendere(
    <ZeitzoneProvider zone={ZONE}>
      <NuqsTestingAdapter hasMemory>
        <DashboardAnsicht />
      </NuqsTestingAdapter>
    </ZeitzoneProvider>,
  );
  await bis(
    () => !(gerendert.behaelter.textContent ?? "").includes(TEXTE.zustand.laedt),
    "die Übersicht ist geladen",
  );
  await bis(() => nur("GET", "/api/tasks").length >= 1, "die Aufträge sind gefragt");
  await zug();
  return gerendert;
}

/** Der Block „Ihre Aufträge“ — der Abschnitt, dessen Überschrift so heißt. */
function block(): HTMLElement | null {
  return (
    [...document.querySelectorAll<HTMLElement>("section")].find(
      (abschnitt) => abschnitt.querySelector("h2")?.textContent === T.titel,
    ) ?? null
  );
}

/** Die Auslöser der Zeilen, in der Reihenfolge des Blocks. */
function zeilen(): HTMLButtonElement[] {
  return [...(block()?.querySelectorAll<HTMLButtonElement>("li > button[aria-expanded]") ?? [])];
}

function knopf(wurzel: Element | null, text: string): HTMLButtonElement | undefined {
  return [...(wurzel?.querySelectorAll("button") ?? [])].find(
    (kandidat) => (kandidat.textContent ?? "").trim() === text,
  );
}

/** Die Überschriften der Übersicht, in der Reihenfolge des Dokuments. */
function ueberschriften(behaelter: HTMLElement): string[] {
  return [...behaelter.querySelectorAll("h2")].map((h) => h.textContent ?? "");
}

/** Das Gitter der Reihe: die Hülle um „Zuletzt aufgefallen“. */
function reihe(behaelter: HTMLElement): HTMLElement {
  const titel = [...behaelter.querySelectorAll("h2")].find(
    (h) => h.textContent === TEXTE.dashboard.aufgefallen.titel,
  );
  const gitter = titel?.closest<HTMLElement>(".grid");
  expect(gitter, "die Reihe").toBeTruthy();
  return gitter as HTMLElement;
}

// ── Die Fälle ───────────────────────────────────────────────────────────────

describe("Ort und Reihe (E‑1098, E‑1099, E‑1121, F18)", () => {
  /**
   * **Eine Aussage über Abwesenheit, und die wichtigste:** Ohne Auftrag sieht
   * die Übersicht aus wie vorher — kein Rahmen, keine Überschrift, die Reihe
   * mit zwei Spalten und genau den Klassen von vorher.
   */
  it("fehlt ohne Aufträge, und die Reihe ist die von vorher", async () => {
    const { behaelter, abbauen } = await ansicht();
    try {
      expect(block()).toBeNull();
      expect(behaelter.textContent).not.toContain(T.titel);
      const gitter = reihe(behaelter);
      expect(gitter.className).toBe("grid gap-4 xl:grid-cols-[3fr_2fr]");
      expect(gitter.children).toHaveLength(2);
    } finally {
      await abbauen();
    }
  });

  it("steht mit Aufträgen zwischen Zuletzt aufgefallen und Verteilung", async () => {
    mitAuftraegen([auftrag()]);
    const { behaelter, abbauen } = await ansicht();
    try {
      const gitter = reihe(behaelter);
      expect(gitter.className).toBe("grid gap-4 xl:grid-cols-[3fr_3fr_4fr]");
      expect(gitter.children).toHaveLength(3);
      const inDerReihe = [...gitter.querySelectorAll("h2")].map((h) => h.textContent);
      expect(inDerReihe).toEqual([
        TEXTE.dashboard.aufgefallen.titel,
        T.titel,
        TEXTE.dashboard.verteilung.titelPartner,
      ]);
    } finally {
      await abbauen();
    }
  });

  /** F18: Kein Platzhalter, solange die Liste nicht da ist — die Reihe bleibt bei zwei. */
  it("steht beim ersten Laden nicht, auch nicht als Platzhalter", async () => {
    liste = () => new Promise<Response>(() => {});
    const { behaelter, abbauen } = await ansicht();
    try {
      expect(block()).toBeNull();
      const gitter = reihe(behaelter);
      expect(gitter.className).toBe("grid gap-4 xl:grid-cols-[3fr_2fr]");
      expect(gitter.querySelectorAll("[aria-busy='true']")).toHaveLength(0);
    } finally {
      await abbauen();
    }
  });

  /** E‑1121: Im Leerzustand steht er allein, zwischen dem Satz und dem Stand. */
  it("steht im Leerzustand der Übersicht allein, zwischen Satz und Stand", async () => {
    uebersicht = () => json(dashboard({ leer: true }));
    mitAuftraegen([auftrag()]);
    const { behaelter, abbauen } = await ansicht();
    try {
      await bis(() => block() !== null, "der Block steht");
      const text = behaelter.textContent ?? "";
      const leer = text.indexOf(TEXTE.dashboard.leerTitel);
      const titel = text.indexOf(T.titel);
      expect(leer).toBeGreaterThanOrEqual(0);
      expect(titel).toBeGreaterThan(leer);
      // Der Stand kommt danach — gesucht über die Überschriftenfolge: Es gibt keine weitere.
      expect(ueberschriften(behaelter)).toEqual([T.titel]);
      expect(behaelter.textContent).not.toContain(TEXTE.dashboard.aufgefallen.titel);
    } finally {
      await abbauen();
    }
  });

  it("zeigt bei einem Fehler Satz und „Erneut versuchen“, und Zuletzt aufgefallen bleibt", async () => {
    liste = () => problem(503, "technischer-fehler");
    const { behaelter, abbauen } = await ansicht();
    try {
      await bis(() => block() !== null, "der Block steht");
      expect(block()!.textContent).toContain(T.ladenFehler);
      expect(ueberschriften(behaelter)).toContain(TEXTE.dashboard.aufgefallen.titel);

      const vorher = nur("GET", "/api/tasks").length;
      mitAuftraegen([auftrag()]);
      await klick(knopf(block(), TEXTE.zustand.erneutVersuchen));
      await bis(() => zeilen().length === 1, "die Liste ist da");
      expect(nur("GET", "/api/tasks").length).toBe(vorher + 1);
    } finally {
      await abbauen();
    }
  });

  /**
   * E‑1100 und E‑1098: kein Knopf, der einen Auftrag anlegt — und der Block
   * fragt den Assistenten nicht, auch wenn er verfügbar wäre.
   */
  it("hat keinen Knopf „Neuer Auftrag“ und fragt den Assistenten nicht", async () => {
    mitAuftraegen([auftrag(), imStand("ERLEDIGT")]);
    const { abbauen } = await ansicht();
    try {
      for (const zeile of zeilen()) {
        await klick(zeile);
      }
      const knoepfe = [...block()!.querySelectorAll("button")].map((k) => k.textContent ?? "");
      // „Neuanlage“ ist eine Aktion und steht in der Zeile; gesucht ist ein Knopf zum Anlegen.
      expect(knoepfe.filter((text) => /neuer auftrag|anlegen/i.test(text))).toEqual([]);
      expect(rufe.filter((ruf) => ruf.pfad.startsWith("/api/assistent"))).toEqual([]);
    } finally {
      await abbauen();
    }
  });
});

describe("Die Stände (§12.4, E‑1105)", () => {
  const HINWEIS: Record<Auftragseinordnung, string | null> = {
    OFFEN: T.hinweis.OFFEN,
    IN_AUSFUEHRUNG: T.hinweis.IN_AUSFUEHRUNG,
    ERLEDIGT: null,
    TEILWEISE_ERLEDIGT: null,
    FEHLGESCHLAGEN: null,
    UNSTIMMIG: T.hinweis.UNSTIMMIG,
    UNBEKANNT: T.hinweis.UNBEKANNT,
  };

  it.each(Object.keys(HINWEIS) as Auftragseinordnung[])(
    "%s: Wort und Zeichen in der Farbe des Stands, aufgeklappt sein Hinweis",
    async (einordnung) => {
      const gestellt = imStand(einordnung);
      mitAuftraegen([gestellt]);
      const { abbauen } = await ansicht();
      try {
        const [zeile] = zeilen();
        const wort = einordnung === "UNBEKANNT" ? "ZURUECKGESTELLT" : T.stand[einordnung];
        // Die innerste Spanne mit genau dem Wort; ihr Elternteil trägt Farbe und Zeichen.
        const stand = [...zeile.querySelectorAll("span")]
          .filter((s) => s.textContent === wort)
          .pop();
        expect(stand, `das Wort ${wort}`).toBeTruthy();
        const traeger = stand!.parentElement!;
        expect(traeger.className).toContain(auftragVordergrund(einordnung));
        expect(traeger.querySelector("svg[aria-hidden='true']"), "das Zeichen").not.toBeNull();

        await klick(zeile);
        const text = block()!.textContent ?? "";
        for (const [andere, hinweis] of Object.entries(HINWEIS)) {
          if (hinweis !== null && hinweis !== HINWEIS[einordnung]) {
            expect(text, `kein Hinweis von ${andere}`).not.toContain(hinweis);
          }
        }
        if (HINWEIS[einordnung] !== null) {
          expect(text).toContain(HINWEIS[einordnung]);
        }
        if (einordnung === "FEHLGESCHLAGEN") {
          expect(text).toContain(T.meldungDerPlattform);
          expect(text).toContain("Die Tabelle konnte nicht gespeichert werden.");
        } else {
          expect(text).not.toContain(T.meldungDerPlattform);
        }
        if (einordnung === "UNSTIMMIG") {
          expect(text).toContain(T.keineRueckmeldung);
        } else {
          expect(text).not.toContain(T.keineRueckmeldung);
        }
      } finally {
        await abbauen();
      }
    },
  );

  /** Farbe nie allein (E‑1105): Jeder Stand hat ein Zeichen in eigener Form (F17). */
  it("gibt jedem der sieben Stände ein eigenes Zeichen", async () => {
    const sieben: Auftragseinordnung[] = [
      "OFFEN",
      "IN_AUSFUEHRUNG",
      "ERLEDIGT",
      "TEILWEISE_ERLEDIGT",
      "FEHLGESCHLAGEN",
      "UNSTIMMIG",
      "UNBEKANNT",
    ];
    mitAuftraegen(sieben.map(imStand));
    const { abbauen } = await ansicht();
    try {
      await klick(knopf(block(), T.weitereViele.replace("{anzahl}", "2")));
      const zeichen = zeilen().map((zeile) => {
        const svg = zeile.querySelectorAll("svg")[1];
        return [...(svg?.classList ?? [])].find((k) => k.startsWith("lucide-")) ?? "";
      });
      expect(zeichen).toHaveLength(7);
      expect(new Set(zeichen).size).toBe(7);
    } finally {
      await abbauen();
    }
  });
});

describe("Die Zeile (E‑1102 bis E‑1104, E‑1107, E‑1122)", () => {
  it("zeigt zugeklappt Aktion, Anzahl, zwei Schlüssel mit „und 1 weiterer“ und den Zeitpunkt in der Anzeigezone", async () => {
    mitAuftraegen([auftrag({ eintraege: [eintrag("TOR3"), eintrag("TOR4"), eintrag("TOR5")] })]);
    const { abbauen } = await ansicht();
    try {
      const [zeile] = zeilen();
      expect(zeile.getAttribute("aria-expanded")).toBe("false");
      const text = zeile.textContent ?? "";
      expect(text).toContain("Neuanlage · 3 Einträge");
      expect(text).toContain("47110/TOR3, 47110/TOR4 und 1 weiterer");
      // 12:32:10 UTC ist am 19.10.2026 in Berlin 14:32 (Sommerzeit) — ein fester Wert, keine Uhr.
      const zeit = [...zeile.querySelectorAll("span")].find(
        (s) => s.textContent === "19.10.2026, 14:32",
      );
      expect(zeit, "der Zeitpunkt in der Anzeigezone").toBeTruthy();
      expect(zeit!.getAttribute("title")).toBe("19.10.2026, 14:32:10");
      expect(text).not.toContain(T.loeschen);
    } finally {
      await abbauen();
    }
  });

  it("zeigt bei Teilweise erledigt statt der Schlüssel „1 von 2 nicht erfolgreich“", async () => {
    mitAuftraegen([imStand("TEILWEISE_ERLEDIGT")]);
    const { abbauen } = await ansicht();
    try {
      const text = zeilen()[0].textContent ?? "";
      expect(text).toContain("1 von 2 nicht erfolgreich");
      expect(text).not.toContain("47110/TOR3");
    } finally {
      await abbauen();
    }
  });

  it("nennt den Typ oben nur, wenn die Liste mehrere Typen hat", async () => {
    const zweiter = auftrag({ typ: "GATTUNG", name: "Gattung aus dem Test", aktion: "Sperren" });
    mitAuftraegen([auftrag(), zweiter]);
    const { abbauen } = await ansicht();
    try {
      expect(zeilen()[0].textContent).toContain(
        "Anpassen der Werk-/Abladestellentabelle · Neuanlage",
      );
      expect(zeilen()[1].textContent).toContain("Gattung aus dem Test · Sperren");
    } finally {
      await abbauen();
    }

    mitAuftraegen([auftrag()]);
    const nurEiner = await ansicht();
    try {
      expect(zeilen()[0].textContent).not.toContain("Anpassen der Werk-/Abladestellentabelle");
    } finally {
      await nurEiner.abbauen();
    }
  });

  it("zeigt aufgeklappt Typ, Zeiten, Werte mit label und das Ergebnis je Eintrag", async () => {
    mitAuftraegen([imStand("TEILWEISE_ERLEDIGT")]);
    const { abbauen } = await ansicht();
    try {
      const [zeile] = zeilen();
      await klick(zeile);
      expect(zeile.getAttribute("aria-expanded")).toBe("true");
      const bereich = document.getElementById(zeile.getAttribute("aria-controls")!);
      expect(bereich, "aria-controls zeigt auf den aufgeklappten Teil").not.toBeNull();

      const text = bereich!.textContent ?? "";
      expect(text).toContain("Anpassen der Werk-/Abladestellentabelle");
      expect(text).toContain("eingestellt 19.10.2026, 14:32");
      expect(text).toContain("ausgeführt 19.10.2026, 14:36");

      const eintraege = [...bereich!.querySelectorAll("li")];
      expect(eintraege).toHaveLength(2);
      const begriffe = [...eintraege[0].querySelectorAll("dt")].map((dt) => dt.textContent);
      expect(begriffe).toEqual(["Werk", "Abladestelle", "SNDPRN"]);
      expect(eintraege[0].textContent).toContain("Ok(erfolgreich)");
      expect(eintraege[1].textContent).toContain("Schlüssel nicht gefunden(nicht erfolgreich)");
      // Getönt ist nur der nicht erfolgreiche Eintrag (E‑1106).
      expect(eintraege[0].className).not.toContain("bg-teilweise-flaeche");
      expect(eintraege[1].className).toContain("bg-teilweise-flaeche");
    } finally {
      await abbauen();
    }
  });

  /** E‑1107: Was die Plattform schreibt, ist reiner Text — auch wenn es nach HTML aussieht. */
  it("zeigt fehlertext und Rückgabe mit <img src=x onerror=alert(1)> wörtlich als Text", async () => {
    const angriff = "<img src=x onerror=alert(1)>";
    mitAuftraegen([
      auftrag({
        status: "FEHLGESCHLAGEN",
        einordnung: "FEHLGESCHLAGEN",
        fehlertext: angriff,
      }),
      auftrag({
        status: "ERLEDIGT",
        einordnung: "TEILWEISE_ERLEDIGT",
        eintraege: [eintrag("TOR3", { rueckgabe: angriff, ergebnis: "UNBEKANNT" })],
      }),
    ]);
    const { abbauen } = await ansicht();
    try {
      for (const zeile of zeilen()) {
        await klick(zeile);
      }
      expect(block()!.querySelectorAll("img")).toHaveLength(0);
      const textknoten = [...block()!.querySelectorAll("p, span")].filter(
        (element) => element.textContent === angriff,
      );
      expect(textknoten.length).toBeGreaterThanOrEqual(2);
      for (const element of textknoten) {
        expect(element.children).toHaveLength(0);
      }
    } finally {
      await abbauen();
    }
  });

  it("zeigt bei sechs Aufträgen fünf, „1 weiteren anzeigen“ zeigt den sechsten", async () => {
    mitAuftraegen(Array.from({ length: 6 }, () => auftrag()));
    const { abbauen } = await ansicht();
    try {
      expect(zeilen()).toHaveLength(5);
      await klick(knopf(block(), T.weitereEins));
      expect(zeilen()).toHaveLength(6);
      await klick(knopf(block(), T.wenigerAnzeigen));
      expect(zeilen()).toHaveLength(5);
    } finally {
      await abbauen();
    }
  });

  it("zeigt von sieben Einträgen fünf und auf Wunsch alle", async () => {
    mitAuftraegen([
      auftrag({
        eintraege: ["T1", "T2", "T3", "T4", "T5", "T6", "T7"].map((tor) => eintrag(tor)),
      }),
    ]);
    const { abbauen } = await ansicht();
    try {
      await klick(zeilen()[0]);
      const eintraege = () => block()!.querySelectorAll("li li");
      expect(eintraege()).toHaveLength(5);
      await klick(knopf(block(), T.alleEintraege.replace("{anzahl}", "7")));
      expect(eintraege()).toHaveLength(7);
    } finally {
      await abbauen();
    }
  });
});

describe("Löschen (E‑1111, E‑1112, E‑1116, F22)", () => {
  it("bietet „Auftrag löschen“ nur bei Offen und nur aufgeklappt an", async () => {
    mitAuftraegen([auftrag(), imStand("IN_AUSFUEHRUNG")]);
    const { abbauen } = await ansicht();
    try {
      expect(knopf(block(), T.loeschen)).toBeUndefined();
      await klick(zeilen()[1]);
      expect(knopf(block(), T.loeschen), "nicht bei In Ausführung").toBeUndefined();
      await klick(zeilen()[0]);
      const knoepfe = [...block()!.querySelectorAll("button")].filter(
        (k) => (k.textContent ?? "").trim() === T.loeschen,
      );
      expect(knoepfe).toHaveLength(1);
      expect(zeilen()[0].closest("li")!.contains(knoepfe[0])).toBe(true);
    } finally {
      await abbauen();
    }
  });

  it("fragt nach, setzt den Fokus auf „Abbrechen“, und Esc bricht ohne Aufruf ab", async () => {
    mitAuftraegen([auftrag()]);
    const { abbauen } = await ansicht();
    try {
      await klick(zeilen()[0]);
      await klick(knopf(block(), T.loeschen));
      expect(block()!.textContent).toContain(T.frage);
      expect(document.activeElement).toBe(knopf(block(), T.abbrechen));

      await taste(document.activeElement!, "Escape");
      expect(block()!.textContent).not.toContain(T.frage);
      expect(document.activeElement).toBe(knopf(block(), T.loeschen));

      await klick(knopf(block(), T.loeschen));
      await klick(knopf(block(), T.abbrechen));
      expect(block()!.textContent).not.toContain(T.frage);
      expect(rufe.filter((ruf) => ruf.methode === "DELETE")).toEqual([]);
    } finally {
      await abbauen();
    }
  });

  it("löscht mit „Löschen“, lädt neu und meldet oben, mit dem Fokus darauf", async () => {
    const offen = auftrag();
    mitAuftraegen([offen]);
    const { behaelter, abbauen } = await ansicht();
    try {
      await klick(zeilen()[0]);
      await klick(knopf(block(), T.loeschen));
      const vorher = nur("GET", "/api/tasks").length;
      mitAuftraegen([]);
      await klick(knopf(block(), T.bestaetigen));
      await bis(() => (block()?.textContent ?? "").includes(T.geloescht), "der Satz steht");

      expect(nur("DELETE", `/api/tasks/${offen.taskId}`)).toHaveLength(1);
      await bis(() => nur("GET", "/api/tasks").length === vorher + 1, "neu geladen");
      await bis(() => zeilen().length === 0, "die Zeile ist weg");
      // Der Block bleibt für den Satz stehen, auch ohne Auftrag (F22).
      const satz = [...block()!.querySelectorAll("[role='status']")].find(
        (s) => s.textContent === T.geloescht,
      );
      expect(satz).toBeTruthy();
      expect(document.activeElement).toBe(satz);

      // „Neu laden“ nimmt ihn weg — und mit ihm den Block, denn es gibt keinen Auftrag mehr.
      await klick(behaelter.querySelector(`[aria-label="${TEXTE.neuLaden.uebersicht}"]`));
      await bis(() => block() === null, "der Block ist weg");
    } finally {
      await abbauen();
    }
  });

  it("meldet bei 409 in der Zeile, dass die Plattform übernommen hat, und lädt neu", async () => {
    const offen = auftrag();
    mitAuftraegen([offen]);
    loeschen = () => problem(409, "auftrag-nicht-offen");
    const { abbauen } = await ansicht();
    try {
      await klick(zeilen()[0]);
      await klick(knopf(block(), T.loeschen));
      mitAuftraegen([{ ...offen, status: "IN_AUSFUEHRUNG", einordnung: "IN_AUSFUEHRUNG" }]);
      await klick(knopf(block(), T.bestaetigen));

      await bis(
        () => (zeilen()[0]?.textContent ?? "").includes(T.stand.IN_AUSFUEHRUNG),
        "die Zeile zeigt den neuen Stand",
      );
      const zeile = zeilen()[0].closest("li")!;
      const satz = [...zeile.querySelectorAll("[role='status']")].find(
        (s) => s.textContent === T.uebernommen,
      );
      expect(satz, "der Satz steht in der Zeile").toBeTruthy();
      expect(document.activeElement).toBe(satz);
      expect(knopf(block(), T.loeschen)).toBeUndefined();
      expect(block()!.textContent).not.toContain(T.geloescht);
    } finally {
      await abbauen();
    }
  });

  it("meldet bei 404 oben, dass der Auftrag nicht mehr vorhanden ist, und lädt neu", async () => {
    const offen = auftrag();
    mitAuftraegen([offen, auftrag()]);
    loeschen = () => problem(404, "nicht-gefunden");
    const { abbauen } = await ansicht();
    try {
      await klick(zeilen()[0]);
      await klick(knopf(block(), T.loeschen));
      mitAuftraegen([offen, auftrag()].filter((a) => a.taskId !== offen.taskId).slice(0, 1));
      await klick(knopf(block(), T.bestaetigen));

      await bis(() => (block()?.textContent ?? "").includes(T.verschwunden), "der Satz steht");
      await bis(() => zeilen().length === 1, "neu geladen");
      expect(document.activeElement?.textContent).toBe(T.verschwunden);
      // Kein Wort über Berechtigung: Der Satz sagt nur, dass es ihn nicht gibt.
      expect(T.verschwunden).not.toMatch(/berecht|zugriff/i);
    } finally {
      await abbauen();
    }
  });

  it("meldet eine Störung in der Zeile, lädt nicht neu und gibt „Auftrag löschen“ frei", async () => {
    mitAuftraegen([auftrag()]);
    loeschen = () => problem(503, "technischer-fehler");
    const { abbauen } = await ansicht();
    try {
      await klick(zeilen()[0]);
      await klick(knopf(block(), T.loeschen));
      const vorher = nur("GET", "/api/tasks").length;
      await klick(knopf(block(), T.bestaetigen));

      await bis(() => (block()?.textContent ?? "").includes(T.gestoert), "der Satz steht");
      expect(nur("GET", "/api/tasks").length).toBe(vorher);
      const wieder = knopf(block(), T.loeschen);
      expect(wieder, "„Auftrag löschen“ steht wieder da").toBeTruthy();
      expect(wieder!.getAttribute("aria-disabled")).toBeNull();
    } finally {
      await abbauen();
    }
  });
});
