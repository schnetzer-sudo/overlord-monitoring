// @vitest-environment jsdom

import { act } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { texteFuer } from "@/i18n";
import { NACHRICHTEN_SCHLUESSEL, type BamWerte } from "@/features/nachrichten/api";
import { BamBlock } from "@/features/nachrichten/components/bam-block";

import { neuerZwischenspeicher, rendere } from "./hilfe/rendern";

/**
 * **Die drei Fälle des BAM-Blocks, für die ein gerenderter Baum die einzige
 * Prüfung ist.**
 *
 * Alles andere an diesem Block ist Abzählung und Einteilung und steht als reine
 * Funktion im Backend — geprüft werden Entscheidungen, nicht Markup
 * (`docs/frontend-grundlagen.md` §9). Diese Datei ist die begründete Ausnahme,
 * und sie bleibt bei drei Tests:
 *
 * | Test | Warum genau dieser |
 * |---|---|
 * | Derselbe Wert unter zwei Typen | Der Schlüssel der Liste ist `(typ, wert)`. Wäre er der Wert allein, meldete React einen **doppelten Schlüssel** — in der Konsole, und sichtbar falsch wäre nichts. M37 misst den Fall auf 4,17 % der Paare |
 * | `bamAnzahl === 0` | Der Block ist **nicht im Baum**, und es geht **keine Anfrage** hinaus. Beides ist eine Aussage über Abwesenheit, und die ist ohne Baum nicht zu treffen. Bei 80,6 % der Nachrichten ist das der Fall (M41) |
 * | eingeklappt mit Werten | Die Gegenprobe: Ohne sie bewiese der vorige Test nur, dass ein leerer Baum keine Anfrage stellt — nicht, dass der Block **wartet**, bis jemand aufklappt |
 *
 * Der erste hängt vollständig an `tests/setup/konsole.ts`: Er besteht genau
 * dann, wenn **kein `console.error`** fällt. Eine pauschale Unterdrückung wäre
 * seine Abschaffung.
 *
 * **Antwortrümpfe gestellt, kein Netz, keine Datenbank.**
 */

const TEXTE = texteFuer("de");
const BAM = TEXTE.nachrichten.detail.bam;

const MESSAGE_ID = "8f3a1c2e-0000-4000-8000-000000000001";

/**
 * **Derselbe Wert unter zwei Typen** — genau die Gestalt, die einen Schlüssel
 * aus dem Wert allein zerlegte. Die Zahlen sind die von `WOC` (9014) und einem
 * konfigurierten Nachbartyp.
 */
const ZWEI_TYPEN_EIN_WERT: BamWerte = {
  messageId: MESSAGE_ID,
  gruppen: [
    {
      typ: 9014,
      bezeichnung: "Lieferantennummer beim Kunden_K_SAP",
      gesamt: 1,
      werte: ["0050"],
      weitereVorhanden: false,
    },
    {
      typ: 9016,
      bezeichnung: "Abladestelle_K_SAP",
      gesamt: 3007,
      werte: ["0050"],
      weitereVorhanden: true,
    },
  ],
};

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

/** Klickt den Schalter des Blocks — aufklappen heißt hier zugleich laden. */
async function klappeAuf(behaelter: HTMLElement): Promise<void> {
  const schalter = behaelter.querySelector("button");
  expect(schalter).not.toBeNull();
  await act(async () => {
    schalter?.dispatchEvent(new MouseEvent("click", { bubbles: true }));
  });
}

describe("Der BAM-Block", () => {
  /**
   * **Die Regression zum Schlüssel.** `(typ, wert)` ist innerhalb einer
   * Nachricht eindeutig — der Primärschlüssel ist
   * `(MessageID, MessageBAMType, MessageBAMValue)`. Der Wert allein ist es
   * nicht: M37 misst, dass bei 4,17 Prozent der Paare derselbe Wert unter
   * mehreren Typen steht. Ein Schlüssel aus dem Wert allein erzeugte doppelte
   * Schlüssel, und die sind für Vitest unsichtbar, solange kein Baum gerendert
   * wird.
   */
  it("rendert denselben Wert unter zwei Typen ohne doppelten React-Schlüssel", async () => {
    const zwischenspeicher = neuerZwischenspeicher();
    zwischenspeicher.setQueryData(NACHRICHTEN_SCHLUESSEL.bam(MESSAGE_ID), ZWEI_TYPEN_EIN_WERT);

    const { behaelter, abbauen } = await rendere(
      <BamBlock messageId={MESSAGE_ID} anzahl={3008} />,
      zwischenspeicher,
    );

    try {
      await klappeAuf(behaelter);

      const gruppen = [...behaelter.querySelectorAll("section")];
      expect(gruppen.map((gruppe) => gruppe.querySelector("h3")?.textContent)).toEqual([
        "Lieferantennummer beim Kunden_K_SAP",
        "Abladestelle_K_SAP",
      ]);

      // Beide Gruppen zeigen denselben Wert — genau darum geht es.
      expect(
        gruppen.map((gruppe) =>
          [...gruppe.querySelectorAll("li[data-wert]")].map((marke) => marke.textContent),
        ),
      ).toEqual([["0050"], ["0050"]]);

      // Und die ehrliche Restangabe steht nur dort, wo es etwas übrig gibt —
      // hinter den Marken in derselben Zelle, aber selbst keine Marke.
      expect(behaelter.textContent).toContain("und 3.006 weitere");
      const reste = [...behaelter.querySelectorAll("li[data-rest]")];
      expect(reste).toHaveLength(1);
      expect(reste[0]?.hasAttribute("data-wert")).toBe(false);
      expect(reste[0]?.previousElementSibling?.hasAttribute("data-wert")).toBe(true);
    } finally {
      await abbauen();
    }
    // Der eigentliche Nachweis ist das Ausbleiben eines `console.error` — dafür
    // sorgt `tests/setup/konsole.ts`, und deshalb steht hier keine Zusicherung.
  });

  /**
   * **Kein leerer Rahmen und keine Anfrage.** Die Zahl steht im Kopf des
   * Detail-Endpunkts, genau damit diese Entscheidung ohne Zugriff fällt. Bei
   * 80,6 Prozent aller Nachrichten und bei *allen* Merge-Eingängen ist sie null
   * (M41).
   */
  it("ist bei bamAnzahl = 0 nicht im Baum und stellt keine Anfrage", async () => {
    const { behaelter, abbauen } = await rendere(<BamBlock messageId={MESSAGE_ID} anzahl={0} />);

    try {
      expect(behaelter.innerHTML).toBe("");
      expect(behaelter.textContent).not.toContain("Belegdaten");
      expect(behaelter.querySelector("button")).toBeNull();
      expect(anfragen).toEqual([]);
    } finally {
      await abbauen();
    }
  });

  /**
   * Die Gegenprobe zum vorigen Test: Mit Werten gibt es den Schalter — und
   * **auch dann noch keine Anfrage**, solange niemand aufklappt. Ohne diesen
   * Test bewiese der vorige nur, dass ein Baum ohne Inhalt keine Anfrage
   * stellt.
   */
  it("zeigt eingeklappt nur die Überschrift mit der Zahl — ohne zu laden", async () => {
    const { behaelter, abbauen } = await rendere(<BamBlock messageId={MESSAGE_ID} anzahl={3008} />);

    try {
      expect(behaelter.querySelector("button")?.textContent).toBe(
        BAM.titel.replace("{anzahl}", "3.008"),
      );
      expect(behaelter.querySelector("button")?.getAttribute("aria-expanded")).toBe("false");
      expect(behaelter.querySelectorAll("section")).toHaveLength(0);
      expect(anfragen).toEqual([]);
    } finally {
      await abbauen();
    }
  });
});
