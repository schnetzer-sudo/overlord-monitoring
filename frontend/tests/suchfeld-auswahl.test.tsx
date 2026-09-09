// @vitest-environment jsdom

import { act } from "react";
import { NuqsTestingAdapter } from "nuqs/adapters/testing";
import { describe, expect, it, vi } from "vitest";

import { NACHRICHTEN_SCHLUESSEL, type Suchfelder } from "@/features/nachrichten/api";
import { Suchfeld } from "@/features/nachrichten/components/suchfeld";
import { einsetzen, texteFuer } from "@/i18n";

import { neuerZwischenspeicher, rendere } from "./hilfe/rendern";

/**
 * **Die Auswahl neben dem Suchfeld — zwei Untermenüs statt einer flachen Liste
 * (E‑112, 09.09.2026).**
 *
 * Als flache Liste hatte das Menü für `NEXANS` 55 Einträge, und die zweite
 * Gruppe stand unterhalb des Sichtbereichs. Was hier geprüft wird, steht nur im
 * Baum — Aufbau, Abwesenheit und Verdrahtung (`docs/frontend-grundlagen.md` §9):
 *
 * | Fall | Warum genau dieser |
 * |---|---|
 * | Die oberste Ebene trägt genau drei Einträge | Aufbau: der typlose Eintrag und je ein Auslöser pro Gruppe — und **keine** Überschrift, kein einzelner Belegart- oder Feldeintrag mehr auf dieser Ebene |
 * | Ein Untermenü zeigt seine Einträge und nicht die des anderen | Abwesenheit: Die Namen der anderen Gruppe stehen nirgends im Dokument, nicht verborgen, sondern nicht gerendert |
 * | Ohne Belegart (`WOC`) fehlt der Auslöser der Belegarten vollständig | Abwesenheit, nicht Leere: kein leerer Auslöser, keine Überschrift |
 * | Eine Wahl im zweiten Untermenü hebt die im ersten auf | Verdrahtung: genau eine Auswahl über alles hinweg — am Schalter, am Auslöser und am Häkchen |
 * | Der Auslöser der Gruppe mit der Auswahl trägt den gewählten Eintrag im zugänglichen Namen | Der Name kommt aus dem Inhalt; **kein `aria-checked`** am Auslöser, der ist `menuitem` mit `aria-haspopup` |
 * | Gleichlautende Einträge in beiden Gruppen ohne `console.error` | Die Regression zum Schlüssel: `bam:` und `feld:` voran, sonst wären `9012` als Typ und als Name derselbe React-Schlüssel |
 *
 * **Geöffnet wird so, wie Radix öffnet:** das Menü auf `pointerdown` mit der
 * linken Taste, ein Untermenü auf `ArrowRight` am Auslöser — beides ohne
 * Zeitgeber, anders als das Öffnen beim Überfahren mit dem Zeiger (100 ms).
 * Kein Test dieser Datei sagt etwas über eine Dauer (Regel T1).
 *
 * **`next/navigation` ist ersetzt, nicht der Prüfling.** `useRouter` wirft
 * außerhalb des App-Routers; das Feld braucht ihn nur, um von einer anderen
 * Seite nach `/suche` zu führen, und das prüft hier niemand. Der Zustand der
 * URL kommt aus dem Testadapter von `nuqs`, das Angebot ist gestellt — kein
 * Netz, keine Datenbank.
 */

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: () => {}, replace: () => {} }),
  usePathname: () => "/",
}));

const TYPWAHL = texteFuer("de").suche.typwahl;

/** Drei Belegarten und drei Feldnamen — die Gestalt von `NEXANS`, nicht seine Zahl (Regel T2). */
const NEXANS: Suchfelder = {
  bam: [
    { quelle: "bam", typ: 9006, bezeichnung: "Lieferschein-Nr._L_SAP", sortIndex: 6 },
    { quelle: "bam", typ: 9016, bezeichnung: "Abladestelle_K_SAP", sortIndex: 16 },
    { quelle: "bam", typ: 9018, bezeichnung: "Kundenmaterialnummer_K_SAP", sortIndex: 18 },
  ],
  felder: [
    { quelle: "feld", name: "Message.GUID", spalte: false },
    { quelle: "feld", name: "Message.SNDPRN", spalte: false },
    { quelle: "feld", name: "Message.Status", spalte: true },
  ],
};

/** Ein Mandant ohne konfigurierte Belegart — `WOC` (M40): nur die zweite Gruppe. */
const WOC: Suchfelder = { bam: [], felder: NEXANS.felder };

async function rendereSuchfeld(angebot: Suchfelder) {
  const zwischenspeicher = neuerZwischenspeicher();
  zwischenspeicher.setQueryData(NACHRICHTEN_SCHLUESSEL.suchfelder, angebot);
  return rendere(
    <NuqsTestingAdapter searchParams="" hasMemory>
      <Suchfeld />
    </NuqsTestingAdapter>,
    zwischenspeicher,
  );
}

/**
 * Ein Zug durch die Warteschlange, **innerhalb** von `act`: Radix' Popper
 * rechnet seine Lage in einer Promise-Kette aus und setzt danach Zustand — das
 * soll im Test abgeschlossen sein, nicht danach als „update not wrapped in act"
 * in der Konsole stehen. Gewartet wird auf die Warteschlange, nicht auf Zeit.
 */
async function ausgeruhrt(): Promise<void> {
  await act(async () => {
    await new Promise((fertig) => setTimeout(fertig, 0));
  });
}

/** Der Schalter neben dem Feld — der einzige Knopf im Baum, der ein Menü aufmacht. */
function schalter(behaelter: HTMLElement): HTMLButtonElement {
  const knopf = behaelter.querySelector<HTMLButtonElement>('button[aria-haspopup="menu"]');
  if (knopf === null) {
    throw new Error("Der Schalter der Auswahl steht nicht im Baum");
  }
  return knopf;
}

/** Radix öffnet ein Menü auf `pointerdown` mit der linken Taste — ein `click` tut nichts. */
async function oeffneMenue(behaelter: HTMLElement): Promise<HTMLElement> {
  await act(async () => {
    schalter(behaelter).dispatchEvent(
      new PointerEvent("pointerdown", {
        bubbles: true,
        cancelable: true,
        button: 0,
        pointerType: "mouse",
      }),
    );
  });
  await ausgeruhrt();
  const menue = document.querySelector<HTMLElement>(
    '[role="menu"][data-slot="dropdown-menu-content"]',
  );
  if (menue === null) {
    throw new Error("Das Menü ist nicht aufgegangen");
  }
  return menue;
}

/** Die Einträge der obersten Ebene — was in einem Untermenü steht, zählt nicht dazu. */
function obersteEbene(menue: HTMLElement): HTMLElement[] {
  return [
    ...menue.querySelectorAll<HTMLElement>('[role="menuitem"], [role="menuitemradio"]'),
  ].filter((eintrag) => eintrag.closest('[data-slot="dropdown-menu-sub-content"]') === null);
}

/** Der Auslöser eines Untermenüs, gefunden über den Anfang seines Textes. */
function ausloeser(menue: HTMLElement, beschriftung: string): HTMLElement {
  const treffer = obersteEbene(menue).find(
    (eintrag) =>
      eintrag.getAttribute("aria-haspopup") === "menu" &&
      (eintrag.textContent ?? "").startsWith(beschriftung),
  );
  if (treffer === undefined) {
    throw new Error(`Kein Untermenü-Auslöser mit der Beschriftung ${beschriftung}`);
  }
  return treffer;
}

/**
 * Öffnet ein Untermenü über die Tastatur. `ArrowRight` ist Radix' Öffnungstaste
 * für `ltr`; gefunden wird der Inhalt über `aria-controls` des Auslösers — nicht
 * über „das eine Untermenü im Dokument", denn beim Wechsel zwischen zwei
 * Untermenüs kann das andere noch stehen.
 */
async function oeffneUntermenue(menue: HTMLElement, beschriftung: string): Promise<HTMLElement> {
  const trigger = ausloeser(menue, beschriftung);
  await act(async () => {
    trigger.focus();
    trigger.dispatchEvent(
      new KeyboardEvent("keydown", { key: "ArrowRight", bubbles: true, cancelable: true }),
    );
  });
  await ausgeruhrt();
  const kennung = trigger.getAttribute("aria-controls");
  const inhalt = kennung === null ? null : document.getElementById(kennung);
  if (inhalt === null || inhalt.getAttribute("data-slot") !== "dropdown-menu-sub-content") {
    throw new Error(`Das Untermenü ${beschriftung} ist nicht aufgegangen`);
  }
  return inhalt;
}

function eintraege(inhalt: HTMLElement): HTMLElement[] {
  return [...inhalt.querySelectorAll<HTMLElement>('[role="menuitemradio"]')];
}

function eintrag(inhalt: HTMLElement, text: string): HTMLElement {
  const treffer = eintraege(inhalt).find((kandidat) => kandidat.textContent === text);
  if (treffer === undefined) {
    throw new Error(`Kein Eintrag ${text} im Untermenü`);
  }
  return treffer;
}

function angehakt(inhalt: HTMLElement): (string | null)[] {
  return eintraege(inhalt)
    .filter((kandidat) => kandidat.getAttribute("aria-checked") === "true")
    .map((kandidat) => kandidat.textContent);
}

/** Wählt einen Eintrag — Radix wählt auf `click`, und das Menü schließt damit. */
async function waehle(ziel: HTMLElement): Promise<void> {
  await act(async () => {
    ziel.click();
  });
  await ausgeruhrt();
}

describe("Die Auswahl neben dem Suchfeld", () => {
  it("trägt auf der obersten Ebene genau drei Einträge: den typlosen und zwei Untermenü-Auslöser", async () => {
    const { behaelter, abbauen } = await rendereSuchfeld(NEXANS);
    try {
      const menue = await oeffneMenue(behaelter);
      const ebene = obersteEbene(menue);

      expect(ebene.map((e) => e.textContent)).toEqual([
        TYPWAHL.alle,
        TYPWAHL.gruppeBelegarten,
        TYPWAHL.gruppeFelder,
      ]);
      expect(ebene.map((e) => e.getAttribute("role"))).toEqual([
        "menuitemradio",
        "menuitem",
        "menuitem",
      ]);
      // Ohne Auswahl ist der typlose Eintrag angehakt — und nur er ist ein Radioeintrag.
      expect(ebene[0]?.getAttribute("aria-checked")).toBe("true");
      expect(ebene.slice(1).map((e) => e.getAttribute("aria-haspopup"))).toEqual(["menu", "menu"]);
      expect(ebene.slice(1).some((e) => e.hasAttribute("aria-checked"))).toBe(false);
      // Keine Überschrift mehr, kein Untermenü offen: Die 55 Einträge der flachen Liste sind weg.
      expect(menue.querySelector('[data-slot="dropdown-menu-label"]')).toBeNull();
      expect(document.querySelector('[data-slot="dropdown-menu-sub-content"]')).toBeNull();
    } finally {
      await abbauen();
    }
  });

  it("zeigt beim Öffnen eines Untermenüs dessen Einträge und nicht die des anderen", async () => {
    const belegarten = NEXANS.bam.map((e) => e.bezeichnung);
    const felder = NEXANS.felder.map((e) => e.name);

    const erstes = await rendereSuchfeld(NEXANS);
    try {
      const menue = await oeffneMenue(erstes.behaelter);
      const inhalt = await oeffneUntermenue(menue, TYPWAHL.gruppeBelegarten);
      expect(eintraege(inhalt).map((e) => e.textContent)).toEqual(belegarten);
      // Die Feldnamen stehen nirgends — nicht verborgen, sondern nicht gerendert.
      for (const name of felder) {
        expect(document.body.textContent).not.toContain(name);
      }
      expect(document.querySelectorAll('[data-slot="dropdown-menu-sub-content"]')).toHaveLength(1);
    } finally {
      await erstes.abbauen();
    }

    const zweites = await rendereSuchfeld(NEXANS);
    try {
      const menue = await oeffneMenue(zweites.behaelter);
      const inhalt = await oeffneUntermenue(menue, TYPWAHL.gruppeFelder);
      expect(eintraege(inhalt).map((e) => e.textContent)).toEqual(felder);
      // Feldnamen in fester Laufweite (E‑105) — die Klasse ist die Regel.
      expect(eintraege(inhalt).every((e) => e.className.includes("font-mono"))).toBe(true);
      for (const bezeichnung of belegarten) {
        expect(document.body.textContent).not.toContain(bezeichnung);
      }
    } finally {
      await zweites.abbauen();
    }
  });

  it("lässt für einen Mandanten ohne Belegart den Auslöser der Belegarten vollständig weg", async () => {
    const { behaelter, abbauen } = await rendereSuchfeld(WOC);
    try {
      const menue = await oeffneMenue(behaelter);

      expect(obersteEbene(menue).map((e) => e.textContent)).toEqual([
        TYPWAHL.alle,
        TYPWAHL.gruppeFelder,
      ]);
      expect(menue.querySelectorAll('[aria-haspopup="menu"]')).toHaveLength(1);
      // Abwesenheit, nicht Leere: Kein Element des Menüs trägt die Beschriftung der Belegarten.
      expect(
        [...menue.querySelectorAll("*")].some((e) => e.textContent === TYPWAHL.gruppeBelegarten),
      ).toBe(false);

      // Das eine Untermenü ist das der Eigenschaften, und es öffnet wie bei NEXANS.
      const inhalt = await oeffneUntermenue(menue, TYPWAHL.gruppeFelder);
      expect(eintraege(inhalt).map((e) => e.textContent)).toEqual(WOC.felder.map((e) => e.name));
    } finally {
      await abbauen();
    }
  });

  it("hebt mit einer Wahl im zweiten Untermenü die Auswahl im ersten auf", async () => {
    const { behaelter, abbauen } = await rendereSuchfeld(NEXANS);
    try {
      // Erst eine Belegart.
      let menue = await oeffneMenue(behaelter);
      await waehle(
        eintrag(await oeffneUntermenue(menue, TYPWAHL.gruppeBelegarten), "Abladestelle_K_SAP"),
      );
      expect(schalter(behaelter).getAttribute("aria-label")).toBe(
        einsetzen(TYPWAHL.gewaehlt, { belegart: "Abladestelle_K_SAP" }),
      );
      // Die Wahl schließt das Menü.
      expect(document.querySelector('[data-slot="dropdown-menu-content"]')).toBeNull();

      // Dann eine Eigenschaft.
      menue = await oeffneMenue(behaelter);
      await waehle(eintrag(await oeffneUntermenue(menue, TYPWAHL.gruppeFelder), "Message.SNDPRN"));
      expect(schalter(behaelter).getAttribute("aria-label")).toBe(
        einsetzen(TYPWAHL.gewaehltesFeld, { feld: "Message.SNDPRN" }),
      );

      // Genau eine Auswahl über alles hinweg: Die Belegart ist weg — am
      // Auslöser, am typlosen Eintrag und am Häkchen im Untermenü.
      menue = await oeffneMenue(behaelter);
      expect(ausloeser(menue, TYPWAHL.gruppeBelegarten).textContent).toBe(TYPWAHL.gruppeBelegarten);
      expect(ausloeser(menue, TYPWAHL.gruppeFelder).textContent).toBe(
        `${TYPWAHL.gruppeFelder} Message.SNDPRN`,
      );
      expect(obersteEbene(menue)[0]?.getAttribute("aria-checked")).toBe("false");
      expect(angehakt(await oeffneUntermenue(menue, TYPWAHL.gruppeBelegarten))).toEqual([]);
      expect(angehakt(await oeffneUntermenue(menue, TYPWAHL.gruppeFelder))).toEqual([
        "Message.SNDPRN",
      ]);
    } finally {
      await abbauen();
    }
  });

  it("trägt am Auslöser der Gruppe mit der Auswahl den gewählten Eintrag im zugänglichen Namen, ohne aria-checked", async () => {
    const { behaelter, abbauen } = await rendereSuchfeld(NEXANS);
    try {
      let menue = await oeffneMenue(behaelter);
      await waehle(
        eintrag(await oeffneUntermenue(menue, TYPWAHL.gruppeBelegarten), "Lieferschein-Nr._L_SAP"),
      );

      menue = await oeffneMenue(behaelter);
      const belegarten = ausloeser(menue, TYPWAHL.gruppeBelegarten);
      const felder = ausloeser(menue, TYPWAHL.gruppeFelder);

      // Der Name kommt aus dem Inhalt — mit Leerzeichen zwischen Beschriftung
      // und Eintrag —, und nichts überschreibt ihn.
      expect(belegarten.textContent).toBe(`${TYPWAHL.gruppeBelegarten} Lieferschein-Nr._L_SAP`);
      expect(belegarten.getAttribute("aria-label")).toBeNull();
      expect(belegarten.getAttribute("aria-labelledby")).toBeNull();
      expect(felder.textContent).toBe(TYPWAHL.gruppeFelder);

      // Ein Auslöser ist ein Menüeintrag mit Untermenü und kein Radioeintrag.
      for (const auszeichnung of [belegarten, felder]) {
        expect(auszeichnung.getAttribute("role")).toBe("menuitem");
        expect(auszeichnung.getAttribute("aria-haspopup")).toBe("menu");
        expect(auszeichnung.hasAttribute("aria-checked")).toBe(false);
      }

      // Das Häkchen steht beim gewählten Eintrag im Untermenü.
      expect(angehakt(await oeffneUntermenue(menue, TYPWAHL.gruppeBelegarten))).toEqual([
        "Lieferschein-Nr._L_SAP",
      ]);
    } finally {
      await abbauen();
    }
  });

  /**
   * **Die Regression zum Schlüssel, eine Ebene tiefer.** Eine Belegart ohne
   * Beschreibung erscheint als Typnummer (`docs/bam-suche.md` §10), und ein
   * Feldname kann genauso lauten; ein Feldname kann zudem wie eine Beschreibung
   * heißen. Trügen die Einträge ihren Text als Schlüssel oder Wert, meldete
   * React einen doppelten Schlüssel — in der Konsole, und sichtbar falsch wäre
   * nichts. Der Nachweis ist das Ausbleiben eines `console.error`
   * (`tests/setup/konsole.ts`); dazu, dass die Wahl des einen den anderen nicht
   * anhakt.
   */
  it("rendert gleichlautende Einträge in beiden Gruppen ohne Meldung in der Konsole", async () => {
    const gleichlautend: Suchfelder = {
      bam: [
        { quelle: "bam", typ: 9012, bezeichnung: "9012", sortIndex: 12 },
        { quelle: "bam", typ: 9006, bezeichnung: "Message.SNDPRN", sortIndex: 6 },
      ],
      felder: [
        { quelle: "feld", name: "9012", spalte: false },
        { quelle: "feld", name: "Message.SNDPRN", spalte: false },
      ],
    };
    const { behaelter, abbauen } = await rendereSuchfeld(gleichlautend);
    try {
      let menue = await oeffneMenue(behaelter);
      expect(
        eintraege(await oeffneUntermenue(menue, TYPWAHL.gruppeBelegarten)).map(
          (e) => e.textContent,
        ),
      ).toEqual(["9012", "Message.SNDPRN"]);
      const felder = await oeffneUntermenue(menue, TYPWAHL.gruppeFelder);
      expect(eintraege(felder).map((e) => e.textContent)).toEqual(["9012", "Message.SNDPRN"]);

      await waehle(eintrag(felder, "9012"));
      expect(schalter(behaelter).getAttribute("aria-label")).toBe(
        einsetzen(TYPWAHL.gewaehltesFeld, { feld: "9012" }),
      );

      // Die Belegart 9012 ist davon unberührt: Im anderen Untermenü ist nichts angehakt.
      menue = await oeffneMenue(behaelter);
      expect(angehakt(await oeffneUntermenue(menue, TYPWAHL.gruppeBelegarten))).toEqual([]);
      expect(angehakt(await oeffneUntermenue(menue, TYPWAHL.gruppeFelder))).toEqual(["9012"]);
    } finally {
      await abbauen();
    }
  });
});
