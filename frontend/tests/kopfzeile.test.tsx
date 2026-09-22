// @vitest-environment jsdom

import { NuqsTestingAdapter } from "nuqs/adapters/testing";
import { describe, expect, it, vi } from "vitest";

import { Kopfzeile } from "@/components/kopfzeile";
import { NACHRICHTEN_SCHLUESSEL, type Suchfelder } from "@/features/nachrichten/api";
import type { Selbstauskunft } from "@/features/sitzung/api";

import { neuerZwischenspeicher, rendere } from "./hilfe/rendern";

/**
 * **Die Kopfzeile mit und ohne Suchfeld — wer den freien Raum bekommt**
 * (E‑232, Befund des Auftraggebers vom 22.09.2026 auf `/mandantenauswahl`).
 *
 * Seit der Suchbereich selbst wächst, hält **sein** `ml-auto` Mandant, Sprache
 * und Nutzermenü am rechten Rand, und der Produktname ist ab `md` eine
 * Beschriftung (`flex-initial`) statt des Füllers. Ohne Navigation gibt es aber
 * kein Suchfeld — auf `/mandantenauswahl` und `/passwort` —, und dann muss der
 * Produktname wieder der Füller sein, sonst rückt alles nach links an ihn
 * heran. Die Regel ist selbst eine Klasse (`docs/frontend-grundlagen.md` §9),
 * und ob sie an das Vorhandensein des Suchfelds gebunden ist, sagt nur der Baum.
 *
 * `next/navigation` ist ersetzt (`useRouter` wirft außerhalb des App-Routers),
 * das Angebot des Suchfelds ist gestellt — kein Netz, keine Datenbank.
 */

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: () => {}, replace: () => {} }),
  usePathname: () => "/",
}));

const AUSKUNFT: Selbstauskunft = {
  username: "beispielnutzer",
  role: "ADMIN",
  mandant: { id: "NEXANS", name: "Nexans autoelectric GmbH" },
  mustChangePassword: false,
  anzeigezone: "Europe/Berlin",
};

const ANGEBOT: Suchfelder = {
  bam: [{ quelle: "bam", typ: 9006, bezeichnung: "Lieferschein-Nr._L_SAP", sortIndex: 6 }],
  felder: [{ quelle: "feld", name: "Message.SNDPRN", spalte: false }],
};

async function rendereKopfzeile(navigationSichtbar: boolean) {
  const zwischenspeicher = neuerZwischenspeicher();
  zwischenspeicher.setQueryData(NACHRICHTEN_SCHLUESSEL.suchfelder, ANGEBOT);
  return rendere(
    <NuqsTestingAdapter searchParams="" hasMemory>
      <Kopfzeile
        auskunft={AUSKUNFT}
        navigationSichtbar={navigationSichtbar}
        mandantenwechselErlaubt={false}
      />
    </NuqsTestingAdapter>,
    zwischenspeicher,
  );
}

function produktname(behaelter: HTMLElement): HTMLElement {
  const name = behaelter.querySelector<HTMLElement>("header span[title]");
  if (name === null) {
    throw new Error("Der Produktname steht nicht in der Kopfzeile");
  }
  return name;
}

describe("Die Kopfzeile und der freie Raum", () => {
  it("lässt mit Suchfeld den Suchbereich wachsen und macht den Produktnamen zur Beschriftung", async () => {
    const { behaelter, abbauen } = await rendereKopfzeile(true);
    try {
      const bereich = behaelter.querySelector<HTMLElement>('[data-bereich="suche"]');
      expect(bereich).not.toBeNull();
      const klassen = [...bereich!.classList];
      // Das eine Element, das den Rest nach rechts hält — und es wächst zwischen den Tokens.
      expect(klassen).toContain("md:ml-auto");
      expect(klassen).toContain("md:flex-1");
      expect(klassen).toContain("md:min-w-suchbereich");
      expect(klassen).toContain("md:max-w-suchbereich-max");
      expect(klassen.filter((k) => /^md:w-/.test(k))).toEqual([]);

      const name = [...produktname(behaelter).classList];
      expect(name).toContain("flex-1");
      expect(name).toContain("md:flex-initial");
    } finally {
      await abbauen();
    }
  });

  it("bleibt ohne Suchfeld beim Produktnamen als Füller, damit nichts nach links rückt", async () => {
    const { behaelter, abbauen } = await rendereKopfzeile(false);
    try {
      // Abwesenheit: kein Suchbereich, also auch kein `ml-auto` im Kopf.
      expect(behaelter.querySelector('[data-bereich="suche"]')).toBeNull();
      expect(
        [...behaelter.querySelectorAll("header *")].some((e) => e.classList.contains("md:ml-auto")),
      ).toBe(false);

      const name = [...produktname(behaelter).classList];
      expect(name).toContain("flex-1");
      expect(name).not.toContain("md:flex-initial");
    } finally {
      await abbauen();
    }
  });
});
