import { describe, expect, it } from "vitest";

import { NACHRICHTENLISTE } from "@/features/nachrichten/spalten";
import { BENUTZERTABELLE } from "@/features/benutzer/spalten";
import { schwelleInPixeln, spaltenbreiten } from "@/lib/spaltenwahl";

/**
 * **Die Breite jeder Spalte bei einer Hüllenbreite** — die reine Rechnung aus
 * `lib/spaltenwahl.ts` (`docs/spaltenwahl.md` §5.4, Korrektur vom 17.11.2026).
 *
 * Dass die wörtlichen Klassen an den Zellen dieselben Zahlen tragen, hält
 * `tests/spaltenwahl.test.tsx` am gerenderten Baum fest; hier steht nur die
 * Rechnung, ohne React.
 *
 * ## Der Fall, für den es sie gibt
 *
 * Seit E‑162 nahm ab der Schwelle des Projekts **das Projekt** den ganzen
 * Überschuss. Projektnamen sind kurz: Am breiten Fenster stand rechts eine große
 * Lücke, während die Statusspalte den Schritt kürzte (Meldung des Auftraggebers
 * vom 17.11.2026 aus der Produktion, Hülle rund 1.670 px). **Entscheidung vom
 * selben Tag:** E‑149 gilt auch hier — ab der Schwelle tragen Zeitpunkt und
 * Projekt ihre Mindestbreite, und der Überschuss teilt sich auf Status und
 * Ablauf im Verhältnis ihrer Breiten an der Schwelle, 286 : 408.
 */

/** Zeitpunkt · Status · Ablauf · Projekt in Pixeln, `null` heißt: nicht da. */
type Zeile = [number, number, number, number | null];

/**
 * **Die Breiten auf `main` vor dieser Änderung** (`a9d354e`), unter der Schwelle
 * — und genau diese bleiben. Gerechnet aus den Klassen von damals: Zeitpunkt
 * 187, Status 156, der Ablauf bekommt den Rest, kein Projekt. 751 ist die
 * Grundmenge, 1.166 der Pixel unter der Schwelle; die übrigen sind die
 * gemessenen Hüllen aus M176 und M180.
 */
const UNTER_DER_SCHWELLE: Record<number, Zeile> = {
  751: [187, 156, 408, null],
  759: [187, 156, 416, null],
  1015: [187, 156, 672, null],
  1144: [187, 156, 801, null],
  1166: [187, 156, 823, null],
};

/**
 * **Ab der Schwelle**: Zeitpunkt und Projekt fest, Status und Ablauf teilen sich,
 * was über 1.167 px hinausgeht, im Verhältnis 286 : 408. Auf `main` stand hier
 * jeweils 187 · 286 · 408 · Rest (1.208 → Projekt 327, 1.670 → 789, 1.920 → 1.039).
 *
 * Die Zahlen sind ausgeschrieben, nicht hergeleitet: Wer die Rechnung ändert,
 * muss sie hier ändern. Status = 286 + (H − 1.167) · 286/694,
 * Ablauf = 408 + (H − 1.167) · 408/694.
 */
const AB_DER_SCHWELLE: Record<number, Zeile> = {
  1167: [187, 286, 408, 286],
  1208: [187, 302.896, 432.104, 286],
  1670: [187, 493.288, 703.712, 286],
  1920: [187, 596.314, 850.686, 286],
};

function pruefe(huelle: number, [zeitpunkt, status, ablauf, projekt]: Zeile) {
  const breiten = spaltenbreiten(NACHRICHTENLISTE, huelle);
  expect(breiten.get("zeitpunkt"), `${huelle} px: Zeitpunkt`).toBeCloseTo(zeitpunkt, 3);
  expect(breiten.get("status"), `${huelle} px: Status`).toBeCloseTo(status, 3);
  expect(breiten.get("ablauf"), `${huelle} px: Ablauf`).toBeCloseTo(ablauf, 3);
  if (projekt === null) {
    expect(breiten.has("projekt"), `${huelle} px: das Projekt steht nicht da`).toBe(false);
  } else {
    expect(breiten.get("projekt"), `${huelle} px: Projekt`).toBeCloseTo(projekt, 3);
  }
  const summe = [...breiten.values()].reduce((gesamt, px) => gesamt + px, 0);
  expect(summe, `${huelle} px: die Spalten füllen die Hülle, ohne Querlauf`).toBeCloseTo(huelle, 6);
}

describe("Die Spaltenbreiten der Nachrichtenliste", () => {
  it.each(Object.entries(UNTER_DER_SCHWELLE))(
    "unter der Schwelle wie auf main: %s px",
    (huelle, zeile) => {
      pruefe(Number(huelle), zeile);
    },
  );

  it.each(Object.entries(AB_DER_SCHWELLE))(
    "ab der Schwelle Zeitpunkt und Projekt fest, Status und Ablauf anteilig: %s px",
    (huelle, zeile) => {
      pruefe(Number(huelle), zeile);
    },
  );

  it("teilt den Überschuss genau 286 : 408, und kein Pixel geht an Zeitpunkt oder Projekt", () => {
    const schwelle = schwelleInPixeln(NACHRICHTENLISTE, 0);
    expect(schwelle, "die Schwelle bleibt").toBe(1167);
    for (let huelle = schwelle; huelle <= 2400; huelle += 37) {
      const breiten = spaltenbreiten(NACHRICHTENLISTE, huelle);
      const status = breiten.get("status")!;
      const ablauf = breiten.get("ablauf")!;
      expect(status / 286, `${huelle} px: Status und Ablauf im selben Verhältnis`).toBeCloseTo(
        ablauf / 408,
        9,
      );
      expect(status + ablauf, `${huelle} px: der ganze Überschuss`).toBeCloseTo(
        huelle - 187 - 286,
        9,
      );
      expect(breiten.get("zeitpunkt")).toBe(187);
      expect(breiten.get("projekt")).toBe(286);
    }
  });

  it("gibt keiner Spalte 0 px, sobald die Grundmenge erreicht ist", () => {
    for (const huelle of [751, 1166, 1167, 1208, 1670, 1920]) {
      for (const [schluessel, px] of spaltenbreiten(NACHRICHTENLISTE, huelle)) {
        expect(px, `${huelle} px: ${schluessel}`).toBeGreaterThan(0);
      }
    }
  });
});

/**
 * Die Rechnung gilt für jede Tabelle und nicht nur für die Liste: **Ohne freie
 * Spalte** teilt sich der Überschuss auf alle sichtbaren Spalten, ein Faktor für
 * alle (E‑149). Die zwei Zahlen aus der Meldung vom 16.09.2026 sind die
 * Gegenprobe — Mandanten 163 statt 673 px, Benutzer 137 statt 81.
 */
describe("Die Spaltenbreiten ohne freie Spalte", () => {
  it("teilt den Überschuss anteilig auf alle Spalten der Benutzertabelle", () => {
    const breiten = spaltenbreiten(BENUTZERTABELLE, 1408);
    expect(Math.round(breiten.get("mandanten")!)).toBe(163);
    expect(Math.round(breiten.get("benutzer")!)).toBe(137);
    const summe = [...breiten.values()].reduce((gesamt, px) => gesamt + px, 0);
    expect(summe).toBeCloseTo(1408, 6);
  });
});
