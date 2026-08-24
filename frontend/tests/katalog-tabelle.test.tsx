// @vitest-environment jsdom

import { describe, expect, it } from "vitest";

import type { Katalogzeile } from "@/features/katalog/api";
import { KatalogTabelle } from "@/features/katalog/components/katalog-tabelle";
import { texteFuer } from "@/i18n";

import { rendere } from "./hilfe/rendern";

/**
 * **Zwei Fälle, für die ein gerenderter Baum die einzige Prüfung ist.**
 *
 * Die Bedingung dafür steht in `tests/hilfe/rendern.tsx`: nicht „ein Baum wäre
 * bequemer", sondern „es gibt keinen anderen Ort, an dem der Satz belegbar
 * wäre". Beide Fälle erfüllen sie, und beide auf dieselbe Art — sie sind
 * Aussagen über **Anwesenheit und Abwesenheit im Baum**.
 *
 * 1. **`false` und `null` sagen Verschiedenes.** Die Unterscheidung entsteht
 *    erst in der Zelle: `zeile.traegtNachrichten ? A : B` wäre die naheliegende
 *    Schreibweise und träfe `null` und `false` in denselben Zweig. Keine reine
 *    Funktion fängt das — der Filter (`sichtbareZeilen`) hält die Zeilen
 *    auseinander, die *Anzeige* muss es getrennt noch einmal tun. Ohne sie
 *    stünde bei `VOTG` an 350 Zeilen dasselbe wie an einer nie gemessenen, und
 *    genau diesen Unterschied legt E14 an.
 * 2. **Bei offener Zeile lässt sich keine zweite öffnen.** Die *Regel* steht als
 *    reine Funktion daneben (`darfOeffnen`, geprüft in `tests/katalog.test.ts`);
 *    was hier belegt wird, ist die **Verdrahtung**: dass die andere Zeile ihre
 *    Schaltfläche wirklich gesperrt bekommt und die offene sie durch das
 *    Formular ersetzt. Eine richtige Regel, die niemand abfragt, sieht von außen
 *    aus wie keine.
 */

const TEXTE = texteFuer("de");
const KATALOG = TEXTE.katalog;

function zeile(werte: Partial<Katalogzeile> & { processId: string }): Katalogzeile {
  return {
    projectId: "300_KundenEingehend",
    projectName: "Kunden eingehend",
    processName: "Eingehender IFTMIN",
    partner: null,
    richtung: null,
    pflegestatus: "OFFEN",
    vorschlagHerkunft: "KEINE",
    traegtNachrichten: null,
    bestandGeprueftAm: null,
    ...werte,
  };
}

function tabelle(zeilen: Katalogzeile[], offeneZeile: string | null = null) {
  return (
    <KatalogTabelle
      zeilen={zeilen}
      offeneZeile={offeneZeile}
      aufOeffnen={() => undefined}
      aufSchliessen={() => undefined}
      vorschlaege={[]}
    />
  );
}

describe("Die drei Zustände von `traegtNachrichten`", () => {
  it("stehen als drei verschiedene Sätze da, und `false` ist nicht `null`", async () => {
    const { behaelter, abbauen } = await rendere(
      tabelle([
        zeile({ processId: "a", traegtNachrichten: true }),
        zeile({ processId: "b", traegtNachrichten: false }),
        zeile({ processId: "c", traegtNachrichten: null }),
      ]),
    );

    try {
      const text = behaelter.textContent ?? "";

      expect(text).toContain(KATALOG.bestand.traegt);
      expect(text).toContain(KATALOG.bestand.traegtNicht);
      expect(text).toContain(KATALOG.bestand.ungeprueft);

      // Die drei Sätze sind wirklich drei und nicht zweimal derselbe.
      const saetze = new Set([
        KATALOG.bestand.traegt,
        KATALOG.bestand.traegtNicht,
        KATALOG.bestand.ungeprueft,
      ]);
      expect(saetze.size).toBe(3);
    } finally {
      await abbauen();
    }
  });

  it("zeigt für eine nie geprüfte Zeile **nicht** den Satz der toten", async () => {
    // Die Gegenprobe, und sie ist der eigentliche Befund: Ein `? :` über den
    // Wahrheitswert schriebe hier „trägt keine Nachrichten" — plausibel und
    // falsch.
    const { behaelter, abbauen } = await rendere(
      tabelle([zeile({ processId: "c", traegtNachrichten: null })]),
    );

    try {
      const text = behaelter.textContent ?? "";

      expect(text).toContain(KATALOG.bestand.ungeprueft);
      expect(text).not.toContain(KATALOG.bestand.traegtNicht);
      expect(text).not.toContain(KATALOG.bestand.traegt);
    } finally {
      await abbauen();
    }
  });
});

describe("Solange eine Zeile offen ist", () => {
  it("ist die Schaltfläche jeder anderen Zeile gesperrt", async () => {
    const { behaelter, abbauen } = await rendere(
      tabelle([zeile({ processId: "a" }), zeile({ processId: "b" })], "a"),
    );

    try {
      const oeffnen = [...behaelter.querySelectorAll("button")].filter(
        (knopf) => knopf.getAttribute("title") === KATALOG.bearbeiten.oeffnen,
      );

      // Die offene Zeile trägt ihre Schaltfläche gar nicht mehr — dort steht
      // das Formular. Übrig bleibt die der anderen Zeile, und die ist gesperrt.
      expect(oeffnen).toHaveLength(1);
      expect(oeffnen[0].disabled).toBe(true);
    } finally {
      await abbauen();
    }
  });

  it("steht unter ihr das Formular und der Satz, warum die anderen gesperrt sind", async () => {
    const { behaelter, abbauen } = await rendere(
      tabelle([zeile({ processId: "a" }), zeile({ processId: "b" })], "a"),
    );

    try {
      // Eine zweite `<tr>` unter der Zeile, nicht Eingabefelder in ihren Zellen.
      expect(behaelter.querySelectorAll("tbody tr")).toHaveLength(3);
      expect(behaelter.querySelector("form")).not.toBeNull();
      expect(behaelter.textContent ?? "").toContain(KATALOG.bearbeiten.gesperrt);
    } finally {
      await abbauen();
    }
  });

  it("trägt ohne offene Zeile jede Schaltfläche und keine ist gesperrt", async () => {
    const { behaelter, abbauen } = await rendere(
      tabelle([zeile({ processId: "a" }), zeile({ processId: "b" })]),
    );

    try {
      const oeffnen = [...behaelter.querySelectorAll("button")].filter(
        (knopf) => knopf.getAttribute("title") === KATALOG.bearbeiten.oeffnen,
      );

      expect(oeffnen).toHaveLength(2);
      expect(oeffnen.every((knopf) => !knopf.disabled)).toBe(true);
      expect(behaelter.querySelector("form")).toBeNull();
    } finally {
      await abbauen();
    }
  });
});
