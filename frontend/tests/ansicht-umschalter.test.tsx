// @vitest-environment jsdom

import { describe, expect, it } from "vitest";

import { texteFuer } from "@/i18n";
import { AnsichtUmschalter } from "@/features/nachrichten/components/ansicht-umschalter";

import { rendere } from "./hilfe/rendern";

/**
 * **Der vierte Fall, für den ein gerenderter Baum die einzige Prüfung ist** —
 * derselben Bauart wie die drei in `tests/detail-baum.test.tsx`, und aus einem
 * verwandten Grund: Was hier belegt wird, ist von Hand nicht zu sehen.
 *
 * Der Umschalter erscheint erst ab `xl` (1280 px). **Die Browsersteuerung kann
 * das Fenster nicht verkleinern** (`docs/frontend-grundlagen.md` §7), eine
 * Sichtprüfung unter dem Umbruchpunkt ist also nur von Hand möglich. Was über
 * die Klassen belegbar ist, wird deshalb hier belegt — und zwar an der Klasse
 * selbst, weil die Regel genau eine Klasse *ist*: Sie steht bewusst nicht in
 * JavaScript, damit es keinen zweiten Umbruchpunkt neben dem der Ansicht gibt.
 *
 * Der zweite Test hält die andere Hälfte fest: **dieselbe Komponente, zwei
 * Aussagen.** Ein Umschalter, der auf der eigenen Route „Ohne Liste anzeigen"
 * verspricht, führte auf die Ansicht, in der man schon steht.
 */

const TEXTE = texteFuer("de");
const DETAIL = TEXTE.nachrichten.detail;

function knopf(behaelter: HTMLElement): HTMLButtonElement {
  const treffer = behaelter.querySelector("button");
  expect(treffer).not.toBeNull();
  return treffer as HTMLButtonElement;
}

describe("Der Umschalter der Detailansicht", () => {
  it("ist unter `xl` ausgeblendet — über die Klasse, nicht über JavaScript", async () => {
    const { behaelter, abbauen } = await rendere(
      <AnsichtUmschalter zu="ohneListe" aufUmschalten={() => undefined} />,
    );

    try {
      const klassen = knopf(behaelter).className.split(/\s+/);
      expect(klassen).toContain("hidden");
      expect(klassen).toContain("xl:inline-flex");
      // `tailwind-merge` löst `inline-flex` aus dem Knopf gegen `hidden` auf.
      // Bliebe es stehen, entschiede die Reihenfolge im Stylesheet — und der
      // Knopf stünde am schmalen Fenster doch da.
      expect(klassen).not.toContain("inline-flex");
    } finally {
      await abbauen();
    }
  });

  it("sagt im Panel und auf der eigenen Route Verschiedenes", async () => {
    const panel = await rendere(
      <AnsichtUmschalter zu="ohneListe" aufUmschalten={() => undefined} />,
    );

    try {
      // `aria-label` **und** `title`: das eine für das Vorleseprogramm, das
      // andere für den Zeiger. Ein Icon-Knopf ohne beides ist für eine der
      // beiden Seiten stumm.
      expect(knopf(panel.behaelter).getAttribute("aria-label")).toBe(DETAIL.ansichtOhneListe);
      expect(knopf(panel.behaelter).title).toBe(DETAIL.ansichtOhneListe);
    } finally {
      await panel.abbauen();
    }

    const route = await rendere(
      <AnsichtUmschalter zu="nebenListe" aufUmschalten={() => undefined} />,
    );

    try {
      expect(knopf(route.behaelter).getAttribute("aria-label")).toBe(DETAIL.ansichtNebenListe);
      expect(knopf(route.behaelter).title).toBe(DETAIL.ansichtNebenListe);
    } finally {
      await route.abbauen();
    }
  });
});
