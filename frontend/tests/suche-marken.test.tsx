// @vitest-environment jsdom

import { describe, expect, it } from "vitest";

import {
  NACHRICHTEN_SCHLUESSEL,
  type BamTreffer,
  type Suchfelder,
} from "@/features/nachrichten/api";
import { MarkenLeiste } from "@/features/nachrichten/components/marken-leiste";
import { TrefferTabelle } from "@/features/nachrichten/components/treffer-tabelle";
import type { Suchmarke } from "@/features/nachrichten/suche";

import { neuerZwischenspeicher, rendere } from "./hilfe/rendern";

/**
 * **Die Fälle der Belegsuche, für die ein gerenderter Baum die einzige Prüfung
 * ist.**
 *
 * Alles andere an dieser Ansicht ist Entscheidung und steht als reine Funktion in
 * `tests/suche.test.ts` (`docs/frontend-grundlagen.md` §9). Diese Datei ist die
 * begründete Ausnahme:
 *
 * | Test | Warum genau dieser |
 * |---|---|
 * | Derselbe Wert unter zwei Typen | Der Schlüssel der Marken ist `(typ, wert)`. Wäre er der Wert allein, meldete React einen **doppelten Schlüssel** — in der Konsole, und sichtbar falsch wäre nichts. M37 misst den Fall auf 4,17 % der Paare |
 * | Dieselbe Parameterform als BAM- und als Feld-Marke *(Teil 2 der Property-Suche)* | Derselbe Grund, eine Ebene höher: Der Schlüssel trägt die **Art** voran. Ohne sie wären `9012:4711` als Belegart und als Feldname derselbe React-Schlüssel |
 * | Die Längenregel der Trefferspalte | Die Regel **ist** eine Klasse plus ein `title`; ohne Baum gibt es nichts, woran sie abzulesen wäre. M45 hat das Kürzen der Beschreibungen selbst ausgeschlossen — gekürzt wird die Zelle |
 * | Keine Spalte „Treffer" ohne Belegnummer *(Teil 2)* | Eine Aussage über **Abwesenheit** im Baum (E‑110): Die Spalte entfällt, sie bleibt nicht leer — und die Zellen der Zeile rücken nach |
 *
 * Die ersten beiden hängen vollständig an `tests/setup/konsole.ts`: Sie bestehen
 * genau dann, wenn **kein `console.error`** fällt. Eine pauschale Unterdrückung
 * wäre ihre Abschaffung.
 *
 * **Antwortrümpfe gestellt, kein Netz, keine Datenbank.**
 */

/**
 * Die längste im Bestand vorkommende Beschreibung (35 Zeichen, M38/E6) — und
 * zwei weitere Typen auf derselben Nachricht.
 */
const LAENGSTE = "Lieferantennummer beim Kunden_K_SAP";

const ANGEBOT: Suchfelder = {
  bam: [
    { quelle: "bam", typ: 9006, bezeichnung: "Lieferschein-Nr._L_SAP", sortIndex: 6 },
    { quelle: "bam", typ: 9016, bezeichnung: "Abladestelle_K_SAP", sortIndex: 16 },
  ],
  felder: [{ quelle: "feld", name: "Message.SNDPRN", spalte: false }],
};

const TREFFERZEILE: BamTreffer = {
  messageId: "8f3a1c2e-0000-4000-8000-000000000001",
  zeitpunkt: "2025-12-29T22:53:50Z",
  status: "FINISHED",
  statusKind: "ABGESCHLOSSEN",
  bedeutungNichtVerifiziert: false,
  processId: "p-1",
  processName: "KUNDE_B_MX_000000_LAB",
  projectName: "300_KundenEingehend",
  sosName: "Versand Einzel IDOC aus Split",
  schritt: null,
  rollen: ["SPLIT_WURZEL"],
  treffer: [
    { typ: 9014, bezeichnung: LAENGSTE, wert: "0050" },
    { typ: 9006, bezeichnung: "Lieferschein-Nr._L_SAP", wert: "0050" },
    { typ: 9021, bezeichnung: "Transportnummer_K_SAP", wert: "0050" },
  ],
};

const bam = (typ: number | null, wert: string): Suchmarke => ({
  art: "bam",
  begriff: { typ, wert },
});

describe("Die Marken der Belegsuche", () => {
  /**
   * **Die Regression zum Schlüssel.** `(typ, wert)` ist eindeutig, der Wert
   * allein ist es nicht: M37 misst, dass bei 4,17 Prozent der Paare derselbe
   * Wert unter mehreren Typen steht. Genau diese Gestalt steht hier — zwei
   * Marken mit demselben Wert unter zwei Typen.
   */
  it("rendert denselben Wert unter zwei Typen ohne doppelten React-Schlüssel", async () => {
    const zwischenspeicher = neuerZwischenspeicher();
    zwischenspeicher.setQueryData(NACHRICHTEN_SCHLUESSEL.suchfelder, ANGEBOT);

    const { behaelter, abbauen } = await rendere(
      <MarkenLeiste marken={[bam(9006, "0050"), bam(9016, "0050")]} aufMarken={() => {}} />,
      zwischenspeicher,
    );

    try {
      const marken = [...behaelter.querySelectorAll("li[data-marke]")];
      expect(marken.map((marke) => marke.getAttribute("data-marke"))).toEqual([
        "bam:9006:0050",
        "bam:9016:0050",
      ]);
      // Beide Marken zeigen denselben Wert — genau darum geht es.
      expect(marken.map((marke) => marke.textContent)).toEqual([
        "Lieferschein-Nr._L_SAP: 0050",
        "Abladestelle_K_SAP: 0050",
      ]);
    } finally {
      await abbauen();
    }
    // Der eigentliche Nachweis ist das Ausbleiben eines `console.error` — dafür
    // sorgt `tests/setup/konsole.ts`, und deshalb steht hier keine Zusicherung.
  });

  /**
   * **Eine Belegart ohne Beschriftung erscheint als Typnummer.** Ein geteilter
   * Link kann einen Typ tragen, den dieser Mandant nicht konfiguriert hat —
   * sichtbar unfertig statt lautlos weggelassen.
   */
  it("zeigt einen unbekannten Typ als Nummer", async () => {
    const zwischenspeicher = neuerZwischenspeicher();
    zwischenspeicher.setQueryData(NACHRICHTEN_SCHLUESSEL.suchfelder, ANGEBOT);

    const { behaelter, abbauen } = await rendere(
      <MarkenLeiste marken={[bam(2000, "0050")]} aufMarken={() => {}} />,
      zwischenspeicher,
    );

    try {
      expect(behaelter.querySelector("li[data-marke]")?.textContent).toBe("2000: 0050");
    } finally {
      await abbauen();
    }
  });
});

describe("Die Spalte „Treffer“ der Trefferliste", () => {
  /**
   * **Die Längenregel an der längsten vorkommenden Beschreibung.**
   *
   * `Lieferantennummer beim Kunden_K_SAP` misst 35 Zeichen, mit `+2` dahinter 38.
   * Gekürzt wird die **Zelle** und nicht der Text: M45 hat das Kürzen der
   * Beschreibungen ausgeschlossen — ohne ihre Endungen fallen 62 Beschreibungen
   * auf 57, und zwei `Abladestelle`-Typen stünden untereinander mit identischer
   * Überschrift.
   *
   * Und **die Hauptinformation der Zeile weicht dabei nicht**: Zeitpunkt und
   * Status haben ihre eigene Spalte mit eigener Breite; die Trefferspalte kürzt
   * in ihrer eigenen Zelle.
   */
  it("kürzt in der Zelle und trägt alle Typen im title", async () => {
    const { behaelter, abbauen } = await rendere(
      <TrefferTabelle
        zeilen={[TREFFERZEILE]}
        gewaehlt={null}
        aufAuswahl={() => {}}
        mitTrefferspalte={true}
      />,
    );

    try {
      const zellen = [...behaelter.querySelectorAll("tbody td")];
      // Zeitpunkt · Status · Treffer · Kette · Ablauf
      const treffer = zellen[2]?.querySelector("span");
      expect(treffer).not.toBeNull();

      expect(treffer?.textContent).toContain(`${LAENGSTE} +2`);
      expect(treffer?.className).toContain("truncate");
      expect(treffer?.getAttribute("title")).toContain(LAENGSTE);
      expect(treffer?.getAttribute("title")).toContain("Transportnummer_K_SAP");
      expect(treffer?.getAttribute("title")).toContain("Lieferschein-Nr._L_SAP");
    } finally {
      await abbauen();
    }
  });

  /**
   * **Der Kettenhinweis steht in der Zeile.** Die Suche findet fast immer die
   * Wurzel (96,87 Prozent der Wurzeln tragen BAM-Werte gegen 2,42 Prozent der
   * Kinder, M26‑1b), und die trägt bei einer Aufteilung einen Endstatus, der die
   * eigentliche Frage nicht beantwortet.
   */
  it("nennt die Stellung in der Kette neben dem Endstatus", async () => {
    const { behaelter, abbauen } = await rendere(
      <TrefferTabelle
        zeilen={[TREFFERZEILE]}
        gewaehlt={null}
        aufAuswahl={() => {}}
        mitTrefferspalte={true}
      />,
    );

    try {
      const kette = [...behaelter.querySelectorAll("tbody td")][3];
      expect(kette?.textContent).toContain("Aufgeteilt");
      expect(kette?.querySelector("span")?.getAttribute("title")).toContain("aufgeteilt");
    } finally {
      await abbauen();
    }
  });
});
