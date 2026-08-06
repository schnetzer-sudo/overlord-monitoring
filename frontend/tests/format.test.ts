import { describe, expect, it } from "vitest";

import {
  ZEITZONE_RUECKFALL,
  formatiereDatum,
  formatiereRelativ,
  formatiereZahl,
  formatiereZeitpunkt,
  formatiereZeitpunktGenau,
  wanduhrzeitFuerEingabe,
  zeitpunktAusWanduhrzeit,
} from "@/lib/format";

/**
 * Die zweite Hälfte der Zeitkette: **UTC der API → dieselbe Wanduhrzeit in der
 * Anzeige.**
 *
 * Die erste Hälfte — Wanduhrzeit der Quelle → UTC — steht im Backend
 * (`ZeitpunkteTest`). Beide Tests halten **dieselben konkreten Werte** fest;
 * läuft eine Seite weg, wird die andere rot.
 *
 * **Dieser Test hielt bis zum 06.08.2026 das Gegenteil fest.** Er verlangte, dass
 * ein Zeitstempel „wie geliefert" angezeigt wird, ausdrücklich auch bei
 * angehängtem `Z`. Für Schritt 3 war das richtig: Kein Endpunkt lieferte
 * Zeitstempel aus `GlassfishDB`, und die Regel schützte davor, einen zonenlosen
 * Wert durch die Browserzone zu schicken. Seit Schritt 4 rechnet das Backend die
 * Wanduhrzeit nach UTC um — und „wie geliefert" wurde damit zu genau der
 * Verschiebung, die die Regel verhindern sollte.
 */

/** Die Zone, in der Anwendungs- und Datenbankserver laufen. */
const BERLIN = "Europe/Berlin";

describe("Die Zeitkette", () => {
  /**
   * Der konkrete Fall aus Messung M9: der letzte Zeitstempel des 29.12.2025.
   *
   * In der Datenbank steht 23:53:50, das Altwerkzeug zeigt 23:53. Genau das muss
   * hier herauskommen — nicht 22:53.
   */
  it("zeigt im Winter wieder die Wanduhrzeit der Datenbank", () => {
    expect(formatiereZeitpunkt("2025-12-29T22:53:50Z", "de", BERLIN)).toBe("29.12.2025, 23:53");
  });

  it("zeigt im Sommer wieder die Wanduhrzeit der Datenbank — dann zwei Stunden", () => {
    expect(formatiereZeitpunkt("2026-07-08T15:21:10Z", "de", BERLIN)).toBe("08.07.2026, 17:21");
  });

  /**
   * Der Kern der Umstellung: Die Anzeige hängt an der gelieferten Zone, **nicht**
   * an der des Betrachters. Ein Nutzer in München und einer in Antwerpen sehen
   * dieselbe Uhrzeit — deshalb wird hier gegen zwei verschiedene Zonen geprüft
   * und nicht gegen die des Testrechners.
   */
  it("hängt an der übergebenen Zone und nicht am Standort des Betrachters", () => {
    const utc = "2025-12-29T22:53:50Z";

    expect(formatiereZeitpunkt(utc, "de", BERLIN)).toBe("29.12.2025, 23:53");
    expect(formatiereZeitpunkt(utc, "de", "UTC")).toBe("29.12.2025, 22:53");
    expect(formatiereZeitpunkt(utc, "de", "America/New_York")).toBe("29.12.2025, 17:53");
  });

  it("liefert den genauen Wert mit Sekunden für den Tooltip", () => {
    expect(formatiereZeitpunktGenau("2025-12-29T22:53:50Z", "de", BERLIN)).toBe(
      "29.12.2025, 23:53:50",
    );
  });

  it("formatiert das Datum in derselben Zone", () => {
    // 00:30 UTC ist in Berlin bereits der 30.12. — das Datum darf nicht am
    // UTC-Tag hängen bleiben.
    expect(formatiereDatum("2025-12-29T23:30:00Z", "de", BERLIN)).toBe("30.12.2025");
  });
});

describe("Zeitpunkte lesen", () => {
  /**
   * Die API überträgt ausschließlich UTC (Richtlinie §5.3). Kommt ein Wert ohne
   * Versatz, wird er **als UTC** gelesen — `new Date("…T22:53:50")` läse ihn
   * sonst als Ortszeit des Browsers, und der Wert bekäme je nach Standort eine
   * andere Bedeutung.
   */
  it("liest einen Wert ohne Versatz als UTC, nicht als Ortszeit des Browsers", () => {
    expect(formatiereZeitpunkt("2025-12-29T22:53:50", "de", BERLIN)).toBe("29.12.2025, 23:53");
    expect(formatiereZeitpunkt("2025-12-29 22:53:50", "de", BERLIN)).toBe("29.12.2025, 23:53");
  });

  it("nimmt einen ausgeschriebenen Versatz ernst", () => {
    // 23:53:50+01:00 ist derselbe Zeitpunkt wie 22:53:50Z.
    expect(formatiereZeitpunkt("2025-12-29T23:53:50+01:00", "de", BERLIN)).toBe(
      "29.12.2025, 23:53",
    );
  });

  it("formatiert in der aktiven Sprache", () => {
    const deutsch = formatiereZeitpunkt("2025-12-29T22:53:50Z", "de", BERLIN);
    const englisch = formatiereZeitpunkt("2025-12-29T22:53:50Z", "en", BERLIN);

    expect(englisch).toContain("2025");
    expect(englisch).not.toBe(deutsch);
  });

  it("reicht einen unlesbaren Wert unverändert durch — lieber roh als falsch", () => {
    expect(formatiereZeitpunkt("kein Zeitpunkt", "de", BERLIN)).toBe("kein Zeitpunkt");
    expect(formatiereZeitpunkt(null, "de", BERLIN)).toBe("");
    expect(formatiereZeitpunkt(undefined, "de", BERLIN)).toBe("");
  });

  /**
   * Fehlt die Zone — etwa solange die Selbstauskunft lädt —, wird UTC gezeigt und
   * **nicht** die Zone des Browsers. Ein Rückfall auf den Browser wäre genau der
   * Fehler, den diese Datei verhindert, nur seltener und damit schwerer zu finden.
   */
  it("fällt ohne oder mit unbrauchbarer Zone auf UTC zurück, nie auf den Browser", () => {
    const inUtc = formatiereZeitpunkt("2025-12-29T22:53:50Z", "de", ZEITZONE_RUECKFALL);

    expect(formatiereZeitpunkt("2025-12-29T22:53:50Z", "de", undefined)).toBe(inUtc);
    expect(formatiereZeitpunkt("2025-12-29T22:53:50Z", "de", "Gibt/EsNicht")).toBe(inUtc);
    expect(inUtc).toBe("29.12.2025, 22:53");
  });
});

describe("Relative Zeit", () => {
  const jetzt = new Date("2025-12-30T04:09:47Z");

  it("nennt die größte Einheit, von der eine ganze vergangen ist", () => {
    expect(formatiereRelativ("2025-12-30T01:09:47Z", "de", jetzt)).toBe("vor 3 Stunden");
    expect(formatiereRelativ("2025-12-30T04:07:47Z", "de", jetzt)).toBe("vor 2 Minuten");
    expect(formatiereRelativ("2025-12-27T04:09:47Z", "de", jetzt)).toBe("vor 3 Tagen");
  });

  it("bleibt bei wenigen Sekunden verständlich statt auf 0 zu springen", () => {
    expect(formatiereRelativ("2025-12-30T04:09:42Z", "de", jetzt)).toBe("vor 5 Sekunden");
    expect(formatiereRelativ("2025-12-30T04:09:47Z", "de", jetzt)).toBe("jetzt");
  });

  it("reicht einen unlesbaren Wert unverändert durch", () => {
    expect(formatiereRelativ("kein Zeitpunkt", "de", jetzt)).toBe("kein Zeitpunkt");
    expect(formatiereRelativ(null, "de", jetzt)).toBe("");
  });
});

/**
 * Die Eingabefelder des freien Zeitfensters.
 *
 * Ein `<input type="datetime-local">` kennt keine Zone. Läse man seinen Wert mit
 * `new Date()`, bekäme er die des Browsers — und das freie Zeitfenster wäre gegen
 * die Daten verschoben, sobald jemand nicht zufällig in der Zone des Servers
 * sitzt. Derselbe Fehler wie oben, nur an der Eingabe statt an der Anzeige.
 */
describe("Wanduhrzeit der Eingabefelder", () => {
  it("liest die Eingabe als Zeit der Anzeigezone", () => {
    expect(zeitpunktAusWanduhrzeit("2025-12-29T23:53", BERLIN)?.toISOString()).toBe(
      "2025-12-29T22:53:00.000Z",
    );
    // Sommer: zwei Stunden statt einer.
    expect(zeitpunktAusWanduhrzeit("2026-07-08T17:21", BERLIN)?.toISOString()).toBe(
      "2026-07-08T15:21:00.000Z",
    );
  });

  it("hängt an der Zone und nicht am Standort des Betrachters", () => {
    const inBerlin = zeitpunktAusWanduhrzeit("2025-12-29T12:00", BERLIN);
    const inUtc = zeitpunktAusWanduhrzeit("2025-12-29T12:00", "UTC");

    expect(inBerlin?.toISOString()).toBe("2025-12-29T11:00:00.000Z");
    expect(inUtc?.toISOString()).toBe("2025-12-29T12:00:00.000Z");
  });

  it("füllt das Feld wieder mit derselben Wanduhrzeit", () => {
    for (const wanduhrzeit of ["2025-12-29T23:53", "2026-07-08T17:21", "2025-12-30T00:00"]) {
      const zeitpunkt = zeitpunktAusWanduhrzeit(wanduhrzeit, BERLIN);
      expect(wanduhrzeitFuerEingabe(zeitpunkt, BERLIN)).toBe(wanduhrzeit);
    }
  });

  /**
   * Der Versatz hängt am Zeitpunkt, den die Umrechnung erst sucht. Ohne den
   * zweiten Durchgang läge sie an den beiden Umstellungstagen im Jahr um eine
   * Stunde daneben — hier die Nacht der Rückstellung 2026.
   */
  it("trifft auch am Tag der Zeitumstellung", () => {
    // 26.10.2025, 03:00 Ortszeit — nach der Rückstellung, also UTC+1.
    expect(zeitpunktAusWanduhrzeit("2025-10-26T03:00", BERLIN)?.toISOString()).toBe(
      "2025-10-26T02:00:00.000Z",
    );
    // 26.10.2025, 01:00 Ortszeit — davor, also UTC+2.
    expect(zeitpunktAusWanduhrzeit("2025-10-26T01:00", BERLIN)?.toISOString()).toBe(
      "2025-10-25T23:00:00.000Z",
    );
  });

  it("liefert nichts zurück, was es nicht lesen kann", () => {
    expect(zeitpunktAusWanduhrzeit("", BERLIN)).toBeNull();
    expect(zeitpunktAusWanduhrzeit("29.12.2025", BERLIN)).toBeNull();
    expect(wanduhrzeitFuerEingabe(null, BERLIN)).toBe("");
  });
});

describe("Zahlen", () => {
  it("werden in der aktiven Sprache getrennt", () => {
    expect(formatiereZahl(1234567, "de")).toBe("1.234.567");
    expect(formatiereZahl(1234567, "en")).toBe("1,234,567");
  });
});
