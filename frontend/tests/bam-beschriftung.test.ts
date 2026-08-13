import { describe, expect, it } from "vitest";

import { zerlegeBeschriftung } from "@/lib/bam-beschriftung";

/**
 * **Die Zerlegung der BAM-Typbeschreibung** — reine Funktion, kein Baum.
 *
 * Die Fälle sind **gemessen und nicht erfunden**: Sie stammen aus M45‑1
 * (`docs/messungen-schritt7.md`), das die 62 Typbeschreibungen des Bestands und
 * ihre Endungen erhoben hat. Genau zwei davon sind Fallen, und beide sind dort
 * an echten Daten aufgetreten:
 *
 * | Falle | Was sie kaputt macht |
 * |---|---|
 * | `[A-Z]` mit `i`-Flag | `Sender_Ident_FORS` bekäme `_Ident_FORS` — der Fehler, den M45‑2 unter `general_ci` in SQL gemessen hat |
 * | „ab dem letzten Unterstrich" | 33 Typen bekämen `_SAP` statt `_L_SAP`/`_K_SAP` — und verlören die Unterscheidung, um die es geht |
 *
 * Beide sind unten als **Gegenprobe** ausgeschrieben und nicht nur als
 * Nebenwirkung eines positiven Falls: Eine Regel, deren Fehlfassung kein Test
 * trifft, ist nicht abgesichert.
 */

describe("zerlegeBeschriftung", () => {
  it.each([
    ["Kundenmaterialnummer_K_SAP", "Kundenmaterialnummer", "_K_SAP"],
    ["Sender_Ident_FORS", "Sender_Ident", "_FORS"],
    ["Empf_Ident_FORS", "Empf_Ident", "_FORS"],
    ["Material-Nr. beim Lieferanten_L_SAP", "Material-Nr. beim Lieferanten", "_L_SAP"],
    ["Lieferantennummer beim Kunden_K_SAP", "Lieferantennummer beim Kunden", "_K_SAP"],
  ])("trennt %s in Name und Endung", (bezeichnung, name, endung) => {
    expect(zerlegeBeschriftung(bezeichnung)).toEqual({ name, endung });
  });

  it.each([
    // 22 der 62 Typen tragen keine Endung (M45‑1).
    ["Bestellnummer"],
    ["Abladestelle"],
    // Fehlt die Zeile in `MessageBAMType`, steht statt der Beschreibung die
    // Typnummer (`docs/bam-werte.md` §6). Ziffern treffen die Regel nicht.
    ["9018"],
  ])("lässt %s ungeteilt", (bezeichnung) => {
    expect(zerlegeBeschriftung(bezeichnung)).toEqual({ name: bezeichnung, endung: null });
  });

  /**
   * **Nicht getrimmt.** Typ 9008 heißt `Beleg-Nr.··TSL·_L_SAP` — zwei
   * Leerzeichen im Namen und eines vor dem Unterstrich. Der Name behält seine
   * Zeichen unverändert; das Leerzeichen davor ist dort sogar die bessere
   * Umbruchstelle.
   */
  it("behält die Leerzeichen des Namens, auch die vor der Endung", () => {
    expect(zerlegeBeschriftung("Beleg-Nr.  TSL _L_SAP")).toEqual({
      name: "Beleg-Nr.  TSL ",
      endung: "_L_SAP",
    });
  });

  /**
   * **Die Gegenprobe zum `i`-Flag.** Mit ihm wäre die Endung `_Ident_FORS` — der
   * Fehler, den M45‑2 unter der Kollation `general_ci` gemessen hat. Ohne Flag
   * ist `[A-Z]` zeichengenau, und `_Ident` bleibt beim Namen.
   */
  it("zählt ein Segment mit Kleinbuchstaben nicht zur Endung", () => {
    expect(zerlegeBeschriftung("Sender_Ident_FORS").name).toBe("Sender_Ident");
    expect(zerlegeBeschriftung("Foo_Bar_SAP").endung).toBe("_SAP");
  });

  /**
   * **Die Gegenprobe zu „ab dem letzten Unterstrich".** Die naive Fassung
   * lieferte für 33 Typen `_SAP` — und `Abladestelle_L_SAP` gegen
   * `Abladestelle_K_SAP` wäre nicht mehr zu unterscheiden. Die beiden stehen auf
   * 3.405 Nachrichten eines Monats gemeinsam (M45‑1).
   */
  it("nimmt den ganzen abschließenden Lauf und nicht nur das letzte Segment", () => {
    expect(zerlegeBeschriftung("Abladestelle_L_SAP").endung).toBe("_L_SAP");
    expect(zerlegeBeschriftung("Abladestelle_K_SAP").endung).toBe("_K_SAP");
  });

  /**
   * Zusammengesetzt ergibt die Zerlegung wieder die Beschriftung — Zeichen für
   * Zeichen. **Das ist die eigentliche Zusicherung dieser Datei:** Gezeigt wird
   * dieselbe Zeichenkette, nur in zwei Teilen. Ginge dabei etwas verloren, wäre
   * es eine Kürzung, und die ist nach M45 ausgeschlossen.
   */
  it.each([
    "Kundenmaterialnummer_K_SAP",
    "Sender_Ident_FORS",
    "Beleg-Nr.  TSL _L_SAP",
    "Bestellnummer",
    "9018",
    "",
  ])("verliert bei %s kein Zeichen", (bezeichnung) => {
    const { name, endung } = zerlegeBeschriftung(bezeichnung);
    expect(`${name}${endung ?? ""}`).toBe(bezeichnung);
  });
});
