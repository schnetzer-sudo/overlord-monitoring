// @vitest-environment jsdom

import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

import { act } from "react";
import { compile } from "tailwindcss";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { einsetzen, texteFuer } from "@/i18n";
import {
  NACHRICHTEN_SCHLUESSEL,
  type Artefaktliste,
  type Eigenschaft,
  type Nachrichtendetail,
} from "@/features/nachrichten/api";
import {
  EIGENSCHAFT_KOPIERKNOPF,
  EigenschaftenListe,
} from "@/features/nachrichten/components/eigenschaft-zeile";
import { NachrichtDetail } from "@/features/nachrichten/components/nachricht-detail";
import { KOPIERT_ANZEIGE_MS } from "@/features/nachrichten/kopieren";

import { stilblatt } from "./hilfe/css-leser";
import { neuerZwischenspeicher, rendere } from "./hilfe/rendern";

/**
 * **Der Kopierknopf je Eigenschaftswert und das Tippen auf den Wert** *(E‑1207,
 * 17.11.2026, `docs/nachrichtendetail.md` §10.21)*.
 *
 * Gerendert, weil die Sätze hier am Baum hängen und an keiner Funktion: welcher
 * Wert **an der Zwischenablage ankommt** (der aus den Daten, nicht der gekürzte
 * aus dem DOM), dass ein Tippen und ein Mausklick auf **dasselbe Element**
 * Verschiedenes tun, wo der Fokus landen kann, dass ein Klick in einer
 * aufklappbaren Zeile **nichts schaltet** — und die Sichtbarkeitsregel, die
 * selbst eine Klasse ist. Die Entscheidung je Geste ist rein und steht in
 * `tests/nachrichtendetail.test.ts` (`tippenKopiert`).
 *
 * **Die Sichtbarkeitsregel wird gegen Tailwind geprüft und nicht nur am
 * Klassennamen.** Dass `group-hover:` nur unter `@media (hover: hover)` wirkt,
 * ist eine Eigenschaft von Tailwind 4 und keine des Namens; der Test übersetzt
 * die Klassen des Knopfs mit dem `globals.css` des Projekts und liest nach, in
 * welchem Block jede Regel steht. Eine eigene `@custom-variant hover` dort oder
 * eine andere Tailwind-Fassung fiele damit auf.
 *
 * **Antwortrümpfe gestellt, kein Netz, keine Datenbank; die Zwischenablage ist
 * gestellt.**
 */

const TEXTE = texteFuer("de");
const EIGENSCHAFTEN = TEXTE.nachrichten.detail.eigenschaften;

const MESSAGE_ID = "8f3a1c2e-0000-4000-8000-000000000001";

/** Länger, als eine Zeile im Panel je zeigt — die Anzeige ist gekürzt, der Wert nicht. */
const LANGER_WERT = `<ActionProperties>${"<Wert>ABC-0815</Wert>".repeat(40)}</ActionProperties>`;

function eigenschaft(
  name: string,
  position: number,
  wert: string,
  gekappt: Pick<Eigenschaft, "gekappt" | "originalLaengeBytes"> = {
    gekappt: false,
    originalLaengeBytes: null,
  },
): Eigenschaft {
  return { name, wert, position, ...gekappt };
}

const ZEILEN: Eigenschaft[] = [
  eigenschaft("Message.SOSActionServiceProperties", 0, LANGER_WERT),
  eigenschaft("Message.SendingPartner", 0, "4012345000009"),
  // Gekappt: Geliefert ist der Anfang, das Original war 20.000 Bytes lang.
  eigenschaft("Message.Payload", 0, "X".repeat(64), { gekappt: true, originalLaengeBytes: 20000 }),
];

/** Was an der gestellten Zwischenablage ankommt. */
let geschrieben: string[] = [];

function stelleZwischenablage(vorhanden: boolean): void {
  Object.defineProperty(navigator, "clipboard", {
    configurable: true,
    value: vorhanden
      ? {
          writeText: vi.fn((text: string) => {
            geschrieben.push(text);
            return Promise.resolve();
          }),
        }
      : undefined,
  });
}

beforeEach(() => {
  geschrieben = [];
  stelleZwischenablage(true);
  vi.stubGlobal(
    "fetch",
    vi.fn(() => Promise.reject(new Error("In diesem Test darf keine Anfrage hinausgehen"))),
  );
});

afterEach(() => {
  vi.useRealTimers();
  vi.unstubAllGlobals();
  Reflect.deleteProperty(navigator, "clipboard");
});

function zeilen(behaelter: HTMLElement): HTMLLIElement[] {
  return [...behaelter.querySelectorAll<HTMLLIElement>("ul > li")];
}

function knopfIn(zeile: Element): HTMLButtonElement {
  const knoepfe = zeile.querySelectorAll<HTMLButtonElement>("button");
  expect(knoepfe).toHaveLength(1);
  return knoepfe[0] as HTMLButtonElement;
}

/** Der Wert einer Zeile — das Element, das den Vollwert im `title` trägt. */
function wertIn(zeile: Element, wert: string): HTMLElement {
  const treffer = [...zeile.querySelectorAll<HTMLElement>("span[title]")].find(
    (span) => span.title === wert,
  );
  expect(treffer).toBeDefined();
  return treffer as HTMLElement;
}

function zeichen(knopf: Element): string {
  return knopf.querySelector("svg")?.getAttribute("class") ?? "";
}

async function klicke(element: Element): Promise<void> {
  await act(async () => {
    element.dispatchEvent(new MouseEvent("click", { bubbles: true }));
  });
}

/**
 * Eine Geste mit Finger, Stift oder Maus: `pointerup` und der Klick, beide mit
 * derselben Zeigerart — so, wie Chrome und Firefox sie liefern.
 */
async function geste(element: Element, art: "touch" | "pen" | "mouse"): Promise<void> {
  await act(async () => {
    element.dispatchEvent(new PointerEvent("pointerup", { bubbles: true, pointerType: art }));
    element.dispatchEvent(new PointerEvent("click", { bubbles: true, pointerType: art }));
  });
}

function rendereListe(eintraege: Eigenschaft[] = ZEILEN) {
  return rendere(<EigenschaftenListe eintraege={eintraege} />);
}

describe("Der Kopierknopf je Eigenschaftswert", () => {
  it("steht genau einmal in jeder Zeile und nennt den Namen der Eigenschaft", async () => {
    const { behaelter, abbauen } = await rendereListe();

    try {
      const alle = zeilen(behaelter);
      expect(alle).toHaveLength(ZEILEN.length);
      alle.forEach((zeile, i) => {
        const knopf = knopfIn(zeile);
        const name = einsetzen(EIGENSCHAFTEN.wertKopieren, { name: ZEILEN[i]!.name });
        // `aria-label` für das Vorleseprogramm, `title` für den Zeiger.
        expect(knopf.getAttribute("aria-label")).toBe(name);
        expect(knopf.title).toBe(name);
        expect(knopf.type).toBe("button");
        // Die dritte Spalte steht am Zeilenende — auch hinter dem
        // Kappungskennzeichen.
        expect(zeile.lastElementChild).toBe(knopf);
      });
    } finally {
      await abbauen();
    }
  });

  it("kopiert den vollen Wert aus den Daten, nicht die gekürzte Anzeige — gekappt den gelieferten Teil", async () => {
    const { behaelter, abbauen } = await rendereListe();

    try {
      const [lang, , gekappt] = zeilen(behaelter);
      // Die Anzeige ist gekürzt (`truncate`); der Text im DOM ist es nicht,
      // aber kopiert wird ohnehin der Wert aus den Daten.
      expect(wertIn(lang!, LANGER_WERT).className).toContain("truncate");

      await klicke(knopfIn(lang!));
      expect(geschrieben).toEqual([LANGER_WERT]);

      await klicke(knopfIn(gekappt!));
      expect(geschrieben).toEqual([LANGER_WERT, "X".repeat(64)]);
      // Das Kappungskennzeichen bleibt, wie es ist.
      expect(gekappt!.textContent).toContain(EIGENSCHAFTEN.gekappt);
    } finally {
      await abbauen();
    }
  });

  it("kopiert beim Tippen auf den Wert und zeigt das Häkchen in der dritten Spalte", async () => {
    const { behaelter, abbauen } = await rendereListe();

    try {
      const zeile = zeilen(behaelter)[0]!;
      const knopf = knopfIn(zeile);
      expect(zeichen(knopf)).toContain("lucide-copy");

      await geste(wertIn(zeile, LANGER_WERT), "touch");

      expect(geschrieben).toEqual([LANGER_WERT]);
      expect(zeichen(knopf)).toContain("lucide-check");
      // Sichtbar, auch wenn der Knopf sonst verborgen ist: `data-kopiert`
      // trägt die Deckkraft, solange das Häkchen steht.
      expect(knopf.dataset.kopiert).toBe("true");
      expect(knopf.getAttribute("aria-label")).toBe(
        einsetzen(EIGENSCHAFTEN.wertKopiert, { name: ZEILEN[0]!.name }),
      );
      // Der Wert selbst wird nicht ersetzt.
      expect(wertIn(zeile, LANGER_WERT).textContent).toBe(LANGER_WERT);

      // Ein Stift kopiert ebenso.
      await geste(wertIn(zeilen(behaelter)[1]!, "4012345000009"), "pen");
      expect(geschrieben).toEqual([LANGER_WERT, "4012345000009"]);
    } finally {
      await abbauen();
    }
  });

  it("kopiert beim Mausklick auf den Wert nicht", async () => {
    const { behaelter, abbauen } = await rendereListe();

    try {
      const zeile = zeilen(behaelter)[0]!;
      await geste(wertIn(zeile, LANGER_WERT), "mouse");
      // Auch ein Klick ohne Zeigerart (ein MouseEvent) nicht.
      await klicke(wertIn(zeile, LANGER_WERT));

      expect(geschrieben).toEqual([]);
      expect(zeichen(knopfIn(zeile))).toContain("lucide-copy");
    } finally {
      await abbauen();
    }
  });

  it("wechselt auf das Häkchen und nach anderthalb Sekunden wieder zurück", async () => {
    vi.useFakeTimers({ toFake: ["setTimeout", "clearTimeout"] });
    const { behaelter, abbauen } = await rendereListe();

    try {
      const knopf = knopfIn(zeilen(behaelter)[1]!);
      await klicke(knopf);
      expect(zeichen(knopf)).toContain("lucide-check");

      await act(async () => {
        vi.advanceTimersByTime(KOPIERT_ANZEIGE_MS - 1);
      });
      expect(zeichen(knopf)).toContain("lucide-check");

      await act(async () => {
        vi.advanceTimersByTime(1);
      });
      expect(zeichen(knopf)).toContain("lucide-copy");
      expect(knopf.dataset.kopiert).toBeUndefined();
      expect(knopf.getAttribute("aria-label")).toBe(
        einsetzen(EIGENSCHAFTEN.wertKopieren, { name: ZEILEN[1]!.name }),
      );
    } finally {
      await abbauen();
    }
  });

  it("lässt das Zeichen stehen, wenn es keine Zwischenablage gibt", async () => {
    // Kein sicherer Kontext: `navigator.clipboard` fehlt. Kein Fehler, kein
    // Häkchen — der Wert bleibt sichtbar und markierbar (§10.3).
    stelleZwischenablage(false);
    const { behaelter, abbauen } = await rendereListe();

    try {
      const zeile = zeilen(behaelter)[0]!;
      await klicke(knopfIn(zeile));
      await geste(wertIn(zeile, LANGER_WERT), "touch");
      expect(zeichen(knopfIn(zeile))).toContain("lucide-copy");
    } finally {
      await abbauen();
    }
  });

  it("ist fokussierbar, ohne dass die Zeile überfahren wird", async () => {
    const { behaelter, abbauen } = await rendereListe();

    try {
      const knopf = knopfIn(zeilen(behaelter)[0]!);
      await act(async () => {
        knopf.focus();
      });
      expect(document.activeElement).toBe(knopf);
      // Verborgen nur über die Deckkraft: in der Tab-Reihenfolge und für
      // Vorleseprogramme vorhanden.
      expect(knopf.tabIndex).toBe(0);
      expect(knopf.hasAttribute("hidden")).toBe(false);
      expect(knopf.getAttribute("aria-hidden")).toBeNull();
    } finally {
      await abbauen();
    }
  });

  it("hängt seine Sichtbarkeit an Fokus und Überfahren, das Überfahren an (hover: hover)", async () => {
    const { behaelter, abbauen } = await rendereListe();

    try {
      const zeile = zeilen(behaelter)[0]!;
      const klassen = knopfIn(zeile).className.split(/\s+/);
      expect(zeile.className.split(/\s+/)).toContain("group/eigenschaft");
      expect(klassen).toEqual(
        expect.arrayContaining([
          "opacity-0",
          "group-hover/eigenschaft:opacity-100",
          "group-focus-within/eigenschaft:opacity-100",
          "data-kopiert:opacity-100",
          // Kein Übergang beim Überfahren (`visuelles-konzept.md` §7):
          // `tailwind-merge` löst das `transition-all` des Buttons dagegen auf.
          "transition-none",
        ]),
      );
      expect(klassen).not.toContain("transition-all");
      // Nicht über `visibility` oder `display` verborgen.
      for (const verboten of ["invisible", "hidden", "sr-only"]) {
        expect(klassen).not.toContain(verboten);
      }

      // Und gegen Tailwind: Wo steht welche Regel?
      const css = await uebersetze(klassen.filter((k) => k.includes("opacity")));
      const { regeln } = stilblatt(css);
      const opazitaet = regeln.filter((regel) => /opacity:\s*100%/.test(regel.eigene));
      const ueberfahren = opazitaet.filter((regel) => regel.selektor.includes(":hover"));
      const fokus = opazitaet.filter((regel) => regel.selektor.includes(":focus-within"));
      expect(ueberfahren).toHaveLength(1);
      expect(fokus).toHaveLength(1);
      expect(ueberfahren[0]!.pfad.some((teil) => /@media\s*\(hover:\s*hover\)/.test(teil))).toBe(
        true,
      );
      // Der Fokus gilt überall, auch am Touchgerät.
      expect(fokus[0]!.pfad.some((teil) => teil.startsWith("@media"))).toBe(false);
      // Die Konstante ist dieselbe, die der Knopf trägt.
      for (const klasse of EIGENSCHAFT_KOPIERKNOPF.split(/\s+/).filter((k) =>
        k.includes("opacity"),
      )) {
        expect(klassen).toContain(klasse);
      }
    } finally {
      await abbauen();
    }
  });
});

describe("Die Namensspalte", () => {
  /**
   * **40 % der Zeile, gedeckelt nur, wo die Hülle es sagt** *(E‑1210,
   * Variante 1)*: Neben der Liste setzt die Hülle `--eigenschaft-name-max` auf
   * 16 rem; auf der eigenen Route und in der Vollansicht gibt es die Variable
   * nicht, und `max-width: var(…)` ohne Rückfall ist dort `none`. Geprüft an der
   * Klasse und an dem, was Tailwind daraus macht.
   */
  it("wächst mit der Zeile und hat ihren Deckel allein aus der Variable", async () => {
    const { behaelter, abbauen } = await rendereListe();

    try {
      const name = zeilen(behaelter)[0]!.querySelector("span")!;
      const klassen = name.className.split(/\s+/);
      expect(klassen).toContain("w-2/5");
      expect(klassen).toContain("max-w-(--eigenschaft-name-max)");
      const css = await uebersetze(["max-w-(--eigenschaft-name-max)"]);
      const regel = stilblatt(css).regeln.find((r) => r.selektor.includes("eigenschaft-name-max"));
      expect(regel?.eigene).toMatch(/max-width:\s*var\(--eigenschaft-name-max\)\s*;?\s*$/);
    } finally {
      await abbauen();
    }
  });
});

describe("Der Kopierknopf in der aufklappbaren Zeile und an der Kennung", () => {
  const KEINE_DATEIEN: Artefaktliste = { messageId: MESSAGE_ID, nutzdaten: [], protokolle: [] };

  function detail(): Nachrichtendetail {
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
      eigenschaftenAnzahl: 1,
      bamAnzahl: 0,
      offenerZustand: "KEINER",
      naechsterSchritt: null,
      wartetSeitSekunden: null,
      schritte: [
        {
          position: 1,
          name: "Datei gelesen",
          namensherkunft: "DIREKT",
          rohwert: "ERFUNDEN_BAUSTEIN",
          start: "2025-12-29T22:41:12Z",
          ende: "2025-12-29T22:41:12Z",
          dauerSekunden: 0,
          timeoutSekunden: 1800,
          laeuftAuf: false,
        },
      ],
      kuratierteEigenschaften: [],
    };
  }

  async function rendereDetail() {
    const zwischenspeicher = neuerZwischenspeicher();
    zwischenspeicher.setQueryData(NACHRICHTEN_SCHLUESSEL.detail(MESSAGE_ID), detail());
    zwischenspeicher.setQueryData(NACHRICHTEN_SCHLUESSEL.dateien(MESSAGE_ID), KEINE_DATEIEN);
    zwischenspeicher.setQueryData(NACHRICHTEN_SCHLUESSEL.eigenschaften(MESSAGE_ID), [
      eigenschaft("Service.Type", 1, "FileReader"),
    ]);
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

  it("schaltet die Zeile weder beim Klick auf den Knopf noch beim Tippen auf den Wert", async () => {
    const { behaelter, abbauen } = await rendereDetail();

    try {
      const schalter = [...behaelter.querySelectorAll("button[aria-controls]")].find((knopf) =>
        (knopf.textContent ?? "").startsWith("Datei gelesen"),
      );
      expect(schalter).toBeDefined();
      await klicke(schalter!);
      expect(schalter!.getAttribute("aria-expanded")).toBe("true");

      const inhalt = behaelter.querySelector(`[id="${schalter!.getAttribute("aria-controls")}"]`);
      const zeile = inhalt!.querySelector("li")!;
      await klicke(knopfIn(zeile));
      await geste(wertIn(zeile, "FileReader"), "touch");
      await geste(wertIn(zeile, "FileReader"), "mouse");

      expect(geschrieben).toEqual(["FileReader", "FileReader"]);
      expect(schalter!.getAttribute("aria-expanded")).toBe("true");
    } finally {
      await abbauen();
    }
  });

  it("ist derselbe Baustein wie an der Nachrichten-Kennung", async () => {
    const { behaelter, abbauen } = await rendereDetail();

    try {
      const kennung = [...behaelter.querySelectorAll("button")].find(
        (knopf) => knopf.getAttribute("aria-label") === TEXTE.nachrichten.detail.kennungKopieren,
      );
      expect(kennung).toBeDefined();
      expect(kennung!.dataset.kopieren).toBe("knopf");
      expect(kennung!.title).toBe(TEXTE.nachrichten.detail.kennungKopieren);

      await klicke(kennung!);
      expect(geschrieben).toEqual([MESSAGE_ID]);
      expect(kennung!.getAttribute("aria-label")).toBe(TEXTE.nachrichten.detail.kennungKopiert);
      expect(zeichen(kennung!)).toContain("lucide-check");
    } finally {
      await abbauen();
    }
  });
});

/**
 * **Übersetzt Klassen mit dem `globals.css` des Projekts** — so, wie der Bau es
 * tut, nur ohne Dateisuche: Die drei Importe werden auf ihre Dateien in
 * `node_modules` gelegt.
 */
async function uebersetze(klassen: string[]): Promise<string> {
  const frontend = join(dirname(fileURLToPath(import.meta.url)), "..");
  const pakete = join(frontend, "node_modules");
  const importe: Record<string, string> = {
    tailwindcss: join(pakete, "tailwindcss", "index.css"),
    "tw-animate-css": join(pakete, "tw-animate-css", "dist", "tw-animate.css"),
    "shadcn/tailwind.css": join(pakete, "shadcn", "dist", "tailwind.css"),
  };
  const uebersetzer = await compile(
    readFileSync(join(frontend, "src", "app", "globals.css"), "utf8"),
    {
      base: frontend,
      loadStylesheet: async (id: string, basis: string) => {
        const pfad = importe[id] ?? join(basis, id);
        return { path: pfad, base: dirname(pfad), content: readFileSync(pfad, "utf8") };
      },
    },
  );
  return uebersetzer.build(klassen);
}
