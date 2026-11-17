import { describe, expect, it } from "vitest";

import { de } from "@/i18n/de";
import { en } from "@/i18n/en";
import {
  ZEITZONE_RUECKFALL,
  formatiereDatum,
  formatiereDauer,
  formatiereRohzeitpunkt,
  formatiereRelativ,
  formatiereAnteil,
  formatiereZahl,
  formatiereZeitpunkt,
  formatiereZeitpunktGenau,
  datumUndUhrzeitFuerEingabe,
  wanduhrzeitFuerEingabe,
  zeitpunktAusDatumUndUhrzeit,
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

/**
 * **Zeitpunkte, die ein Auftrag als Text trägt** (E‑1191,
 * `docs/assistent-frontend.md` §12.15): der Start eines Neudurchlaufs, so, wie
 * er an die Plattform geht. Im Block steht er im Format aller Zeitpunkte — mit
 * Komma —, ohne dass sich Tag oder Uhrzeit verschieben.
 */
describe("Zeitpunkte als Rohwert", () => {
  it("steht mit Komma, im Format der übrigen Zeitpunkte", () => {
    expect(formatiereRohzeitpunkt("25.03.2026 10:35", "de")).toBe("25.03.2026, 10:35");
    // Dasselbe Format wie ein Zeitpunkt der API, der in der Anzeigezone dieselbe Wanduhrzeit hat.
    expect(formatiereZeitpunkt("2026-03-25T09:35:00Z", "de", BERLIN)).toBe("25.03.2026, 10:35");
  });

  it("verschiebt weder Tag noch Uhrzeit — der Wert hat keine Zone", () => {
    expect(formatiereRohzeitpunkt("31.12.2026 23:59", "de")).toBe("31.12.2026, 23:59");
    expect(formatiereRohzeitpunkt("01.01.2026 00:00", "de")).toBe("01.01.2026, 00:00");
    // Der Tag der Zeitumstellung: Eine Wanduhrzeit, die es in Berlin nicht gibt, bleibt stehen.
    expect(formatiereRohzeitpunkt("29.03.2026 02:30", "de")).toBe("29.03.2026, 02:30");
  });

  it("folgt in jeder Sprache dem Format der übrigen Zeitpunkte", () => {
    expect(formatiereRohzeitpunkt("25.03.2026 10:35", "en")).toBe(
      formatiereZeitpunkt("2026-03-25T10:35:00Z", "en", ZEITZONE_RUECKFALL),
    );
  });

  it("lässt jeden anderen Wert unverändert — lieber roh als falsch", () => {
    for (const wert of [
      "c70f6022-e801-41b5-a614-cbd64a99b31a",
      "5",
      "72332",
      "25.03.2026",
      "25.03.2026  10:35",
      " 25.03.2026 10:35",
      "25.03.2026 10:35 ",
      "25.03.2026, 10:35",
      "2026-03-25 10:35",
      "31.02.2026 10:35",
      "25.13.2026 10:35",
      "25.03.2026 24:00",
      "25.03.2026 10:60",
      "",
    ]) {
      expect(formatiereRohzeitpunkt(wert, "de"), wert).toBe(wert);
    }
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

/**
 * **Datum und Uhrzeit getrennt, die Uhrzeit freiwillig** *(17.11.2026, E‑1198,
 * `docs/nachrichtenliste.md` §8.2)*.
 *
 * Ohne Uhrzeit beginnt „Von" um 00:00. Wo „Bis" endet, hängt am **Vertrag des
 * Einhängepunkts**: Liste und Belegsuche lesen `bis` einschließend, auf die
 * Sekunde (`MessageLastUpdate <= bis`, Spalte `timestamp` ohne Bruchteil) — das
 * Ende des Tages ist 23:59:59. Die Prozessansicht liest `bis` als letzte
 * enthaltene volle Stunde (E‑94, E‑95) — das Ende des Tages ist 23:00.
 */
describe("Datum und Uhrzeit der Zeitfensterfelder", () => {
  const gelesen = (
    datum: string,
    uhrzeit: string,
    grenze: "von" | "bis",
    vertrag: "sekunde" | "stunde",
    zone = BERLIN,
  ) => zeitpunktAusDatumUndUhrzeit(datum, uhrzeit, grenze, vertrag, zone)?.toISOString() ?? null;

  it("beginnt „Von“ ohne Uhrzeit um 00:00, in beiden Verträgen", () => {
    expect(gelesen("2026-07-07", "", "von", "sekunde")).toBe("2026-07-06T22:00:00.000Z");
    expect(gelesen("2026-07-07", "", "von", "stunde")).toBe("2026-07-06T22:00:00.000Z");
    expect(gelesen("2026-01-07", "", "von", "sekunde")).toBe("2026-01-06T23:00:00.000Z");
  });

  it("endet „Bis“ ohne Uhrzeit am Ende des Tages im Vertrag des Einhängepunkts", () => {
    expect(gelesen("2026-07-07", "", "bis", "sekunde"), "Liste, Suche").toBe(
      "2026-07-07T21:59:59.000Z",
    );
    expect(gelesen("2026-07-07", "", "bis", "stunde"), "Prozessansicht").toBe(
      "2026-07-07T21:00:00.000Z",
    );
  });

  it("nimmt eine eingetragene Uhrzeit, wie sie dasteht — Minuten, Sekunden, Millisekunden", () => {
    expect(gelesen("2026-07-07", "14:30", "von", "sekunde")).toBe("2026-07-07T12:30:00.000Z");
    expect(gelesen("2026-07-07", "14:30", "bis", "sekunde")).toBe("2026-07-07T12:30:00.000Z");
    expect(gelesen("2026-07-07", "14:00", "bis", "stunde")).toBe("2026-07-07T12:00:00.000Z");
    expect(gelesen("2026-07-07", "14:30:15", "bis", "sekunde")).toBe("2026-07-07T12:30:15.000Z");
    expect(gelesen("2026-07-07", "14:30:15.250", "von", "sekunde")).toBe(
      "2026-07-07T12:30:15.250Z",
    );
  });

  it("hängt an der Anzeigezone und nicht am Standort", () => {
    expect(gelesen("2026-07-07", "", "von", "sekunde", "UTC")).toBe("2026-07-07T00:00:00.000Z");
    expect(gelesen("2026-07-07", "", "bis", "sekunde", "UTC")).toBe("2026-07-07T23:59:59.000Z");
  });

  /**
   * Am Tag der Vorstellung hat der Tag 23 Stunden, am Tag der Rückstellung 25.
   * Ohne den zweiten Durchgang läge eine der beiden Grenzen eine Stunde daneben.
   */
  it("trifft beide Umstellungstage 2026, den 29.03. und den 25.10.", () => {
    expect(gelesen("2026-03-29", "", "von", "sekunde")).toBe("2026-03-28T23:00:00.000Z");
    expect(gelesen("2026-03-29", "", "bis", "sekunde")).toBe("2026-03-29T21:59:59.000Z");
    expect(gelesen("2026-03-29", "", "bis", "stunde")).toBe("2026-03-29T21:00:00.000Z");
    expect(gelesen("2026-03-29", "03:00", "von", "stunde")).toBe("2026-03-29T01:00:00.000Z");

    expect(gelesen("2026-10-25", "", "von", "sekunde")).toBe("2026-10-24T22:00:00.000Z");
    expect(gelesen("2026-10-25", "", "bis", "sekunde")).toBe("2026-10-25T22:59:59.000Z");
    expect(gelesen("2026-10-25", "", "bis", "stunde")).toBe("2026-10-25T22:00:00.000Z");
    expect(gelesen("2026-10-25", "03:00", "von", "stunde")).toBe("2026-10-25T02:00:00.000Z");
  });

  it("liest nichts, was kein Datum oder keine Uhrzeit ist", () => {
    expect(gelesen("", "", "von", "sekunde"), "ohne Datum").toBeNull();
    expect(gelesen("", "14:30", "von", "sekunde"), "Uhrzeit ohne Datum").toBeNull();
    expect(gelesen("07.07.2026", "", "von", "sekunde")).toBeNull();
    expect(gelesen("2026-02-30", "", "von", "sekunde"), "kein Kalendertag").toBeNull();
    expect(gelesen("2026-07-07", "24:00", "bis", "sekunde")).toBeNull();
    expect(gelesen("2026-07-07", "1430", "von", "sekunde")).toBeNull();
  });

  const felder = (iso: string | null, grenze: "von" | "bis", vertrag: "sekunde" | "stunde") =>
    datumUndUhrzeitFuerEingabe(iso === null ? null : new Date(iso), grenze, vertrag, BERLIN);

  /**
   * **Die Uhrzeit bleibt leer genau dann, wenn der Wert der Vorgabe für „ohne
   * Uhrzeit" entspricht** — sonst steht sie da, auf die Millisekunde.
   */
  it("lässt die Uhrzeit genau dann leer, wenn sie der Vorgabe entspricht", () => {
    expect(felder("2026-07-06T22:00:00.000Z", "von", "sekunde")).toEqual({
      datum: "2026-07-07",
      uhrzeit: "",
    });
    expect(felder("2026-07-07T21:59:59.000Z", "bis", "sekunde")).toEqual({
      datum: "2026-07-07",
      uhrzeit: "",
    });
    expect(felder("2026-07-07T21:00:00.000Z", "bis", "stunde")).toEqual({
      datum: "2026-07-07",
      uhrzeit: "",
    });

    // Dieselben Zeitpunkte im anderen Vertrag oder an der anderen Grenze: keine Vorgabe.
    expect(felder("2026-07-07T21:00:00.000Z", "bis", "sekunde").uhrzeit).toBe("23:00");
    expect(felder("2026-07-07T21:59:59.000Z", "bis", "stunde").uhrzeit).toBe("23:59:59");
    expect(felder("2026-07-06T22:00:00.000Z", "bis", "sekunde").uhrzeit).toBe("00:00");
    expect(felder("2026-07-07T21:59:00.000Z", "bis", "sekunde").uhrzeit).toBe("23:59");
    expect(felder("2026-07-07T21:59:59.500Z", "bis", "sekunde").uhrzeit).toBe("23:59:59.500");
    expect(felder("2026-07-07T12:30:15.000Z", "von", "sekunde").uhrzeit).toBe("14:30:15");
    expect(felder(null, "von", "sekunde")).toEqual({ datum: "", uhrzeit: "" });
  });

  /** Jede volle und halbe Stunde, dazu krumme Sekunden, über beide Umstellungstage 2026. */
  function zeitpunkteUm(tagUtc: string): string[] {
    const start = Date.parse(`${tagUtc}T00:00:00.000Z`) - 6 * 3_600_000;
    const liste: string[] = [];
    for (let schritt = 0; schritt < 2 * 36; schritt++) {
      liste.push(new Date(start + schritt * 1_800_000).toISOString());
    }
    liste.push(new Date(start + 7 * 3_600_000 + 17_250).toISOString());
    return liste;
  }

  /**
   * **URL → Felder → URL, Zeichen für Zeichen.** Die eine Lücke ist die Stunde,
   * die es am 25.10. zweimal gibt: Die Felder kennen keine Zone, „02:30" steht
   * für beide. Gelesen wird sie als die **spätere** — die Felder zeigen beim
   * ersten Durchlauf dieselbe Wanduhrzeit, die Adresse verschiebt sich um eine
   * Stunde. Ausdrücklich festgehalten statt ausgelassen.
   */
  it("bringt die Adresse über die Felder zeichengleich zurück, an beiden Umstellungstagen", () => {
    const doppelt = (iso: string) => iso >= "2026-10-25T00:00" && iso < "2026-10-25T01:00";
    for (const vertrag of ["sekunde", "stunde"] as const) {
      for (const grenze of ["von", "bis"] as const) {
        for (const iso of [...zeitpunkteUm("2026-03-29"), ...zeitpunkteUm("2026-10-25")]) {
          const { datum, uhrzeit } = felder(iso, grenze, vertrag);
          const zurueck = gelesen(datum, uhrzeit, grenze, vertrag);
          if (doppelt(iso)) {
            expect(zurueck, `${iso} ist die frühere 02:xx`).toBe(
              new Date(Date.parse(iso) + 3_600_000).toISOString(),
            );
          } else {
            expect(zurueck, `${grenze} ${vertrag} ${iso}`).toBe(iso);
          }
        }
      }
    }
  });

  it("bringt die Felder über die Adresse unverändert zurück", () => {
    const eingaben: [string, string, "von" | "bis", "sekunde" | "stunde"][] = [
      ["2026-07-07", "", "von", "sekunde"],
      ["2026-07-07", "", "bis", "sekunde"],
      ["2026-07-07", "", "bis", "stunde"],
      ["2026-07-07", "14:30", "bis", "sekunde"],
      ["2026-07-07", "14:00", "von", "stunde"],
      ["2026-03-29", "", "bis", "sekunde"],
      ["2026-10-25", "", "bis", "stunde"],
      ["2026-10-25", "04:00", "von", "stunde"],
    ];
    for (const [datum, uhrzeit, grenze, vertrag] of eingaben) {
      const adresse = zeitpunktAusDatumUndUhrzeit(datum, uhrzeit, grenze, vertrag, BERLIN);
      expect(datumUndUhrzeitFuerEingabe(adresse, grenze, vertrag, BERLIN)).toEqual({
        datum,
        uhrzeit,
      });
    }
  });
});

describe("Zahlen", () => {
  it("werden in der aktiven Sprache getrennt", () => {
    expect(formatiereZahl(1234567, "de")).toBe("1.234.567");
    expect(formatiereZahl(1234567, "en")).toBe("1,234,567");
  });
});

/**
 * Der Anteil des Kuratierungsfortschritts (Schritt 9b).
 *
 * **Über `Intl` und nicht von Hand.** Das Prozentzeichen steht nicht in jeder
 * Sprache an derselben Stelle und nicht mit demselben Abstand davor — und eine
 * selbst zusammengesetzte Zeichenkette wäre in genau einer der beiden Sprachen
 * falsch, und zwar unauffällig. Deshalb steht der Unterschied hier als Zusage.
 */
describe("Anteile", () => {
  it("tragen in beiden Sprachen ihre eigene Schreibweise", () => {
    // Deutsch mit schmalem geschütztem Leerzeichen vor dem Zeichen, englisch
    // ohne. Genau das ist der Grund für `Intl`.
    expect(formatiereAnteil(0.56, "de")).toBe("56\u00a0%");
    expect(formatiereAnteil(0.56, "en")).toBe("56%");
  });

  it("runden auf ganze Prozent — der Fortschritt ist eine Größenordnung", () => {
    expect(formatiereAnteil(0.5638, "de")).toBe("56\u00a0%");
    expect(formatiereAnteil(1, "de")).toBe("100\u00a0%");
    expect(formatiereAnteil(0, "de")).toBe("0\u00a0%");
  });

  it("machen aus einer unbrauchbaren Zahl `0` und keine Panne", () => {
    expect(formatiereAnteil(Number.NaN, "de")).toBe("0\u00a0%");
  });
});

/**
 * Die Dauern der Zeitleiste (Schritt 5).
 *
 * **Die Einheiten kommen aus der Sprachdatei** und stehen nicht im Formatierer —
 * auch „s" und „min" sind Text, den ein Nutzer sieht. Hier wird deshalb mit
 * genau den Bausteinen der Leitsprache geprüft und nicht mit erfundenen.
 */
describe("Dauern", () => {
  const einheiten = de.nachrichten.detail.dauer;

  it("nennt höchstens zwei Einheiten", () => {
    expect(formatiereDauer(45, einheiten)).toBe("45 s");
    expect(formatiereDauer(65, einheiten)).toBe("1 min 5 s");
    expect(formatiereDauer(120, einheiten)).toBe("2 min");
    expect(formatiereDauer(11520, einheiten)).toBe("3 h 12 min");
    expect(formatiereDauer(3600, einheiten)).toBe("1 h");
    // Sekunden fallen ab einer Stunde weg — sie beantworten dort keine Frage
    // mehr, und die Spalte soll in jeder Zeile gleich breit bleiben.
    expect(formatiereDauer(3661, einheiten)).toBe("1 h 1 min");
    expect(formatiereDauer(90000, einheiten)).toBe("1 d 1 h");
    expect(formatiereDauer(172800, einheiten)).toBe("2 d");
  });

  /**
   * Das Backend rechnet in ganzen Sekunden; ein Schritt mit `0` hat zwischen
   * null und einer Sekunde gedauert. „0 s" behauptete eine Genauigkeit, die die
   * Zahl nicht hat — und die Zeitleiste soll „praktisch nichts" sagen, nicht
   * „gar nichts".
   */
  it("schreibt für null Sekunden nicht „0 s“", () => {
    expect(formatiereDauer(0, einheiten)).toBe("< 1 s");
  });

  /**
   * Eine negative Dauer kommt nicht vor — das Backend liefert dort `null`. Käme
   * sie doch, ist ein Minuszeichen in einer Zeitleiste schlechter als gar
   * nichts.
   */
  it("zeigt niemals eine negative Dauer", () => {
    expect(formatiereDauer(-5, einheiten)).toBe("< 1 s");
    expect(formatiereDauer(Number.NaN, einheiten)).toBe("< 1 s");
  });

  it("nutzt die Bausteine der aktiven Sprache", () => {
    expect(formatiereDauer(65, en.nachrichten.detail.dauer)).toBe("1 min 5 s");
  });
});
