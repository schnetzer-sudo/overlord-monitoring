// @vitest-environment jsdom

import { act } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { texteFuer } from "@/i18n";
import {
  NACHRICHTEN_SCHLUESSEL,
  type Artefaktliste,
  type Eigenschaft,
  type Nachrichtendetail,
  type Schritt,
} from "@/features/nachrichten/api";
import { NachrichtDetail } from "@/features/nachrichten/components/nachricht-detail";

import { neuerZwischenspeicher, rendere } from "./hilfe/rendern";

/**
 * **Der Block *Technische Eigenschaften*, für die Fälle, in denen ein
 * gerenderter Baum die einzige Prüfung ist** *(17.08.2026, neu gefasst am
 * 21.09.2026 — `docs/nachrichtendetail.md` §10.16)*.
 *
 * Die Einteilung selbst ist reine Funktion und steht in
 * `tests/nachrichtendetail.test.ts` — geprüft werden Entscheidungen, nicht
 * Markup (`docs/frontend-grundlagen.md` §9). Diese Datei ist die begründete
 * Ausnahme: **Es gibt keinen anderen Ort, an dem der Satz belegbar wäre.**
 *
 * Gerendert wird seit dem 21.09.2026 **das ganze Detail** und nicht mehr der
 * Block allein: Die Abfrage gehört dem Ablauf, weil die Zeitleiste dieselbe
 * Antwort braucht (E‑220), und „die Anfrage geht genau einmal mit dem Detail
 * hinaus" ist ohne den Aufrufer nicht zu belegen.
 *
 * | Test | Warum genau dieser |
 * |---|---|
 * | Die Zahl aus `allgemein` | Die Überschrift trägt die Zeilenzahl der allgemeinen Angaben und **nicht** `eigenschaftenAnzahl`. Zugleich die Regression zum Schlüssel `${position}:${name}`: derselbe Name steht unter Eingang und zwei Schritten, **ohne `console.error`** |
 * | `eigenschaftenAnzahl === 0` | **Kein Schalter im Baum und keine Anfrage** — Aussagen über Abwesenheit |
 * | Eine Anfrage mit dem Detail | Genau eine auf `/eigenschaften` beim Einhängen, **keine** beim Aufklappen — weder am Block noch an einer Zeile. Vorher steht die Überschrift ohne Zahl |
 * | Die gescheiterte Abfrage | Der Fehlerbaustein steht **an Stelle des Schalters**, sichtbar ohne Aufklappen; die Zeitleiste bleibt und ist nicht aufklappbar |
 * | Leeres `allgemein` | Ein eigener Satz, der nicht behauptet, es gäbe keine Eigenschaften — und kein Schalter |
 * | Zugeklappt `inert`, Bewegung mit Ausschalter | Beides **ist** ein Attribut beziehungsweise eine Klasse; `jsdom` rechnet weder Layout noch Übergänge. Belegt wie beim Ansichtsumschalter |
 *
 * **Antwortrümpfe gestellt, kein Netz, keine Datenbank.**
 */

const TEXTE = texteFuer("de");
const EIGENSCHAFTEN = TEXTE.nachrichten.detail.eigenschaften;

const MESSAGE_ID = "8f3a1c2e-0000-4000-8000-000000000001";

function eigenschaft(name: string, position: number, wert: string): Eigenschaft {
  return { name, wert, position, gekappt: false, originalLaengeBytes: null };
}

function schritt(position: number, name: string): Schritt {
  return {
    position,
    name,
    namensherkunft: "DIREKT",
    rohwert: "ERFUNDEN_BAUSTEIN",
    start: "2025-12-29T22:41:12Z",
    ende: "2025-12-29T22:41:12Z",
    dauerSekunden: 0,
    timeoutSekunden: 1800,
    laeuftAuf: false,
  };
}

const SCHRITTE = [schritt(1, "Datei gelesen"), schritt(2, "Datei konvertiert")];

function detail(werte: Partial<Nachrichtendetail> = {}): Nachrichtendetail {
  return {
    messageId: MESSAGE_ID,
    status: "FINISHED",
    statusKind: "ABGESCHLOSSEN",
    processId: "P-0815",
    processName: "Erfundener Prozess",
    projectName: "Erfundenes Projekt",
    sosName: "Erfundener Ablauf",
    rollen: [],
    zeitpunkt: "2025-12-29T22:41:20Z",
    start: "2025-12-29T22:41:10Z",
    gesamtdauerSekunden: 10,
    fristSekunden: null,
    eigenschaftenAnzahl: 5,
    bamAnzahl: 0,
    offenerZustand: "KEINER",
    naechsterSchritt: null,
    wartetSeitSekunden: null,
    schritte: SCHRITTE,
    kuratierteEigenschaften: [],
    ...werte,
  };
}

/**
 * **Derselbe Name an drei Stellen** — genau die Gestalt, die einen Schlüssel aus
 * dem Namen allein zerlegte. `Service.Type` steht im gemessenen Tagesfenster auf
 * sieben verschiedenen Schritten (M17 3).
 */
const DERSELBE_NAME_MEHRFACH: Eigenschaft[] = [
  eigenschaft("Message.GUID", 0, "8f3a1c2e-0000-4000-8000-000000000001"),
  eigenschaft("Service.Type", 0, "ScheduleBean"),
  eigenschaft("Service.Type", 1, "FileReader"),
  eigenschaft("Converter.Log.GUID", 2, "log|1"),
  eigenschaft("Service.Type", 2, "Converter"),
];

const KEINE_DATEIEN: Artefaktliste = { messageId: MESSAGE_ID, nutzdaten: [], protokolle: [] };

/** Ein `fetch`, das jeden Aufruf sichtbar macht — und keinen durchlässt. */
let anfragen: string[] = [];

beforeEach(() => {
  anfragen = [];
  vi.stubGlobal(
    "fetch",
    vi.fn((eingabe: RequestInfo | URL) => {
      anfragen.push(String(eingabe));
      return Promise.reject(new Error("In diesem Test darf keine Anfrage hinausgehen"));
    }),
  );
});

afterEach(() => {
  vi.unstubAllGlobals();
});

/** Ein `fetch`, das **nie antwortet** — die Abfrage läuft, statt zu scheitern. */
function anfrageHaengt(): void {
  vi.stubGlobal(
    "fetch",
    vi.fn((eingabe: RequestInfo | URL) => {
      anfragen.push(String(eingabe));
      return new Promise<Response>(() => undefined);
    }),
  );
}

function aufEigenschaften(): string[] {
  return anfragen.filter((adresse) => adresse.includes("/eigenschaften"));
}

/** `null`: kalter Zwischenspeicher — die Abfrage der Eigenschaften geht wirklich hinaus. */
async function rendereDetail(
  werte: Partial<Nachrichtendetail>,
  eigenschaften: Eigenschaft[] | null,
) {
  const zwischenspeicher = neuerZwischenspeicher();
  zwischenspeicher.setQueryData(NACHRICHTEN_SCHLUESSEL.detail(MESSAGE_ID), detail(werte));
  zwischenspeicher.setQueryData(NACHRICHTEN_SCHLUESSEL.dateien(MESSAGE_ID), KEINE_DATEIEN);
  if (eigenschaften !== null) {
    zwischenspeicher.setQueryData(NACHRICHTEN_SCHLUESSEL.eigenschaften(MESSAGE_ID), eigenschaften);
  }

  return rendere(
    <NachrichtDetail
      messageId={MESSAGE_ID}
      aufSchliessen={() => undefined}
      schliessenText="Schließen"
      aufOeffnen={() => undefined}
    />,
    zwischenspeicher,
  );
}

async function klicke(element: Element | null | undefined): Promise<void> {
  expect(element).toBeTruthy();
  await act(async () => {
    element?.dispatchEvent(new MouseEvent("click", { bubbles: true }));
  });
}

/** Der Schalter des Blocks — die Schaltfläche, deren Text mit der Überschrift beginnt. */
function blockschalter(behaelter: HTMLElement): HTMLButtonElement | undefined {
  return [...behaelter.querySelectorAll("button")].find((knopf) =>
    (knopf.textContent ?? "").startsWith(EIGENSCHAFTEN.titelOhneZahl),
  );
}

/** Der Inhalt, den eine Schaltfläche steuert. */
function inhaltVon(behaelter: HTMLElement, schalter: Element | undefined): HTMLElement {
  const id = schalter?.getAttribute("aria-controls");
  expect(id).toBeTruthy();
  const inhalt = behaelter.querySelector<HTMLElement>(`[id="${id}"]`);
  expect(inhalt).not.toBeNull();
  return inhalt as HTMLElement;
}

function zeilentexte(bereich: Element): (string | null)[] {
  return [...bereich.querySelectorAll("ul > li")].map((zeile) => zeile.textContent);
}

describe("Der Block Technische Eigenschaften", () => {
  /**
   * **Die Zahl ist die Zeilenzahl von `allgemein`** (E‑224) — eine von fünf, und
   * nicht die `5` aus dem Kopf. Der Block ist flach: keine Gruppenköpfe mehr.
   *
   * **Und die Regression zum Schlüssel.** `${position}:${name}` ist eindeutig,
   * der Name allein nicht (31 der 101 gemessenen Namen stehen auf mehr als einem
   * Schritt, M17 3). `Service.Type` steht hier unter dem Eingang und unter beiden
   * Schritten — der Nachweis ist das Ausbleiben eines `console.error`
   * (`tests/setup/konsole.ts`).
   */
  it("trägt die Zeilenzahl der allgemeinen Angaben und verteilt den Rest an die Zeitleiste", async () => {
    const { behaelter, abbauen } = await rendereDetail({}, DERSELBE_NAME_MEHRFACH);

    try {
      const schalter = blockschalter(behaelter);
      expect(schalter?.textContent).toBe(EIGENSCHAFTEN.titel.replace("{anzahl}", "1"));
      expect(zeilentexte(inhaltVon(behaelter, schalter))).toEqual([
        "Message.GUID8f3a1c2e-0000-4000-8000-000000000001",
      ]);
      // Flach: Die Gruppenköpfe vom 17.08.2026 gibt es nicht mehr.
      expect(inhaltVon(behaelter, schalter).querySelectorAll("h3, section")).toHaveLength(0);

      // Was nicht allgemein ist, steht an der Zeitleiste: unter dem Eingang und
      // unter seinem Schritt. Zusammen sind es alle fünf.
      const zeilenschalter = [...behaelter.querySelectorAll("button[aria-controls]")].filter(
        (knopf) => knopf !== schalter,
      );
      expect(zeilenschalter.map((knopf) => zeilentexte(inhaltVon(behaelter, knopf)))).toEqual([
        ["Service.TypeScheduleBean"],
        ["Service.TypeFileReader"],
        ["Converter.Log.GUIDlog|1", "Service.TypeConverter"],
      ]);
    } finally {
      await abbauen();
    }
  });

  /**
   * **Kein Schalter und keine Anfrage.** Die Zahl steht im Kopf des
   * Detail-Endpunkts, genau damit diese Entscheidung ohne Zugriff fällt — und
   * das ist seit E‑220 ihr einziger verbliebener Zweck.
   */
  it("zeigt bei eigenschaftenAnzahl = 0 nur eine Zeile Text — kein Schalter, keine Anfrage", async () => {
    const { behaelter, abbauen } = await rendereDetail({ eigenschaftenAnzahl: 0 }, null);

    try {
      expect(blockschalter(behaelter)).toBeUndefined();
      expect(behaelter.textContent).toContain(EIGENSCHAFTEN.keine);
      // Auch an der Zeitleiste ist dann nichts aufklappbar.
      expect(behaelter.querySelectorAll("button[aria-controls]")).toHaveLength(0);
      expect(anfragen).toEqual([]);
    } finally {
      await abbauen();
    }
  });

  /**
   * **Die Anfrage geht genau einmal mit dem Detail hinaus, beim Aufklappen
   * keine** (E‑220). Solange sie läuft, steht die Überschrift **ohne Zahl** —
   * keine erfundene Null — und an der Zeitleiste ist nichts aufklappbar (E‑221).
   */
  it("holt die Eigenschaften einmal mit dem Detail und beim Aufklappen nicht noch einmal", async () => {
    anfrageHaengt();
    const { behaelter, zwischenspeicher, abbauen } = await rendereDetail({}, null);

    try {
      expect(aufEigenschaften()).toHaveLength(1);
      expect(aufEigenschaften()[0]).toContain(`/nachrichten/${MESSAGE_ID}/eigenschaften`);

      expect(blockschalter(behaelter)?.textContent).toBe(EIGENSCHAFTEN.titelOhneZahl);
      expect(behaelter.querySelectorAll("ol button")).toHaveLength(0);

      await klicke(blockschalter(behaelter));
      expect(aufEigenschaften()).toHaveLength(1);

      // Die Antwort trifft ein — und der Makrotask danach, in dem TanStack Query
      // seine Benachrichtigungen zustellt.
      await act(async () => {
        zwischenspeicher.setQueryData(
          NACHRICHTEN_SCHLUESSEL.eigenschaften(MESSAGE_ID),
          DERSELBE_NAME_MEHRFACH,
        );
        await new Promise((fertig) => setTimeout(fertig, 0));
      });

      expect(blockschalter(behaelter)?.textContent).toBe(
        EIGENSCHAFTEN.titel.replace("{anzahl}", "1"),
      );
      const schrittschalter = behaelter.querySelector("ol button");
      await klicke(schrittschalter);
      expect(schrittschalter?.getAttribute("aria-expanded")).toBe("true");
      expect(aufEigenschaften()).toHaveLength(1);
    } finally {
      await abbauen();
    }
  });

  /**
   * **Der Fehlerbaustein steht an Stelle des Schalters, sichtbar ohne
   * Aufklappen.** Hinter einem Schalter verborgen sähe eine gescheiterte Abfrage
   * aus wie eine Nachricht, an deren Zeitleiste schlicht nichts aufzuklappen
   * ist. Die Zeitleiste bleibt — sie hängt am Detail —, ist dann aber nicht
   * aufklappbar.
   */
  it("zeigt eine gescheiterte Abfrage ohne Aufklappen und lässt die Zeitleiste stehen", async () => {
    const { behaelter, abbauen } = await rendereDetail({}, null);

    try {
      // Die Abweisung kommt in einem Makrotask an — TanStack Query stellt seine
      // Benachrichtigungen gebündelt zu.
      await act(async () => {
        await new Promise((fertig) => setTimeout(fertig, 0));
      });

      expect(blockschalter(behaelter)).toBeUndefined();
      expect(behaelter.querySelectorAll('[role="alert"]')).toHaveLength(1);

      expect(behaelter.querySelectorAll("ol > li")).toHaveLength(2);
      expect(behaelter.querySelectorAll("ol button")).toHaveLength(0);
    } finally {
      await abbauen();
    }
  });

  /**
   * **Eigenschaften ja, allgemeine nein.** Der Satz dafür darf nicht behaupten,
   * es gäbe keine — sie stehen an der Zeitleiste. Und ein Schalter, der einen
   * leeren Bereich öffnet, wäre schlimmer als keiner.
   */
  it("sagt bei leerem `allgemein` nicht, es gäbe keine Eigenschaften", async () => {
    const { behaelter, abbauen } = await rendereDetail({ eigenschaftenAnzahl: 1 }, [
      eigenschaft("Service.Type", 1, "FileReader"),
    ]);

    try {
      expect(blockschalter(behaelter)).toBeUndefined();
      expect(behaelter.textContent).toContain(EIGENSCHAFTEN.keineAllgemeinen);
      expect(behaelter.textContent).not.toContain(EIGENSCHAFTEN.keine);
      expect(behaelter.querySelectorAll("ol button")).toHaveLength(1);
    } finally {
      await abbauen();
    }
  });

  /**
   * **Zugeklappt ist der Inhalt `inert`, und die Bewegung trägt ihren
   * Ausschalter** (E‑228). Der Inhalt bleibt eingehängt, damit die Höhe sich
   * bewegen kann; erreichbar ist er zugeklappt trotzdem nicht. Belegt über
   * Attribut und Klassen — `jsdom` rechnet weder Layout noch Übergänge, und die
   * Regel *ist* die Klasse (wie beim Ansichtsumschalter).
   */
  it("hält den zugeklappten Inhalt inert und gibt der Bewegung ihren Ausschalter", async () => {
    const { behaelter, abbauen } = await rendereDetail({}, DERSELBE_NAME_MEHRFACH);

    try {
      const schalter = blockschalter(behaelter);
      const inhalt = inhaltVon(behaelter, schalter);

      expect(schalter?.getAttribute("aria-expanded")).toBe("false");
      expect(inhalt.hasAttribute("inert")).toBe(true);

      await klicke(schalter);
      expect(schalter?.getAttribute("aria-expanded")).toBe("true");
      expect(inhalt.hasAttribute("inert")).toBe(false);
      // `aria-controls` zeigt in beiden Zuständen auf denselben, eingehängten Inhalt.
      expect(inhaltVon(behaelter, schalter)).toBe(inhalt);

      const spur = inhalt.closest('[data-aufklappen="spur"]');
      expect(spur?.getAttribute("data-state")).toBe("open");
      for (const klasse of [
        "grid-rows-[0fr]",
        "data-[state=open]:grid-rows-[1fr]",
        "transition-[grid-template-rows]",
        // 300 ms seit E‑229, auf wie zu — bis dahin 220.
        "duration-[300ms]",
        "ease-[cubic-bezier(0.2,0,0,1)]",
        "motion-reduce:transition-none",
      ]) {
        expect(spur?.classList.contains(klasse), klasse).toBe(true);
      }

      const blende = inhalt.querySelector('[data-aufklappen="inhalt"]');
      for (const klasse of [
        "duration-[110ms]",
        "data-[state=open]:duration-150",
        "data-[state=open]:delay-[55ms]",
        "motion-reduce:transition-none",
      ]) {
        expect(blende?.classList.contains(klasse), klasse).toBe(true);
      }

      await klicke(schalter);
      expect(inhalt.hasAttribute("inert")).toBe(true);
    } finally {
      await abbauen();
    }
  });
});
