import { describe, expect, it } from "vitest";

import type { Nachrichtendetail, Schritt } from "@/features/nachrichten/api";
import {
  BALKEN_MINDESTANTEIL,
  LUECKE_SCHWELLE_SEKUNDEN,
  balkenanteil,
  bedeutungNichtVerifiziert,
  laengsteDauer,
  luecke,
  zeitleiste,
} from "@/features/nachrichten/detail";

/**
 * Die Zeitleiste der Detailansicht.
 *
 * Geprüft werden die **Entscheidungen**, nicht das Markup: Wie breit ein Balken
 * wird, wann eine Lücke eine eigene Zeile bekommt und was der offene Zustand
 * anhängt. Alles drei ist reine Rechnung und liegt deshalb neben der Komponente
 * und nicht in ihr.
 */

function schritt(werte: Partial<Schritt> & { position: number }): Schritt {
  return {
    name: `Schritt ${werte.position}`,
    namensherkunft: "DIREKT",
    rohwert: "NXS_FILE_CONVERT|E2A|UNWRAP",
    start: "2025-12-29T22:41:12Z",
    ende: "2025-12-29T22:41:12Z",
    dauerSekunden: 0,
    timeoutSekunden: 1800,
    laeuftAuf: false,
    ...werte,
  };
}

function detail(werte: Partial<Nachrichtendetail> = {}): Nachrichtendetail {
  return {
    messageId: "8f3a1c2e-0000-4000-8000-000000000001",
    status: "FINISHED",
    statusKind: "ABGESCHLOSSEN",
    processId: "p1",
    processName: "40000_AMG_LAB_VDA",
    projectName: "300_KundenEingehend",
    sosName: "Versand Einzel IDOC aus Split",
    zeitpunkt: "2025-12-29T22:53:50Z",
    start: "2025-12-29T22:41:12Z",
    timeoutSekunden: 1800,
    eigenschaftenAnzahl: 22,
    offenerZustand: "KEINER",
    naechsterSchritt: null,
    schritte: [],
    kuratierteEigenschaften: [],
    ...werte,
  };
}

/**
 * **Der Balken ist auf die Nachricht normiert, nicht auf eine absolute Skala.**
 *
 * Ein Wartschritt von 30 Minuten neben vier Schritten von Sekundenbruchteilen
 * ergäbe auf einer absoluten Zeitachse einen vollen Balken und vier unsichtbare
 * Striche — korrekt und nutzlos. Normiert beantwortet die Leiste die Frage, für
 * die sie da ist: welcher Schritt hat die Zeit gefressen.
 */
describe("Die Normierung des Balkens", () => {
  it("gibt dem längsten Schritt die volle Breite und allen anderen ihren Anteil", () => {
    const schritte = [
      schritt({ position: 1, dauerSekunden: 1800 }),
      schritt({ position: 2, dauerSekunden: 900 }),
      schritt({ position: 3, dauerSekunden: 450 }),
    ];

    expect(laengsteDauer(schritte)).toBe(1800);
    expect(balkenanteil(1800, 1800)).toBe(1);
    expect(balkenanteil(900, 1800)).toBe(0.5);
    expect(balkenanteil(450, 1800)).toBe(0.25);
  });

  /**
   * Sehr kurze Schritte sollen sich als „praktisch nichts" lesen, nicht als
   * „nicht vorhanden". Ohne Mindestbreite verschwänden vier von fünf Zeilen
   * neben einem Wartschritt.
   */
  it("lässt einen sehr kurzen Schritt nicht verschwinden", () => {
    expect(balkenanteil(1, 1800)).toBe(BALKEN_MINDESTANTEIL);
    expect(balkenanteil(0, 1800)).toBe(BALKEN_MINDESTANTEIL);
  });

  /**
   * Lief jeder Schritt unter einer Sekunde, gibt es nichts zu vergleichen —
   * dann bekommt jeder die Mindestbreite. Eine Division durch null gibt es
   * nicht.
   */
  it("hält eine Nachricht aus, in der jeder Schritt unter einer Sekunde lief", () => {
    const schritte = [schritt({ position: 1 }), schritt({ position: 2 })];

    expect(laengsteDauer(schritte)).toBe(0);
    expect(balkenanteil(0, 0)).toBe(BALKEN_MINDESTANTEIL);
  });

  /**
   * **Keine Dauer heißt kein Balken.** Eine Fläche der Breite null sähe aus wie
   * „praktisch nichts" — und das ist etwas anderes als „nicht aufgezeichnet".
   * Das Backend liefert eine negative Dauer als `null`; hier kommt sie deshalb
   * gar nicht erst an.
   */
  it("zeichnet ohne Dauer keinen Balken", () => {
    expect(balkenanteil(null, 1800)).toBeNull();
    expect(laengsteDauer([schritt({ position: 1, dauerSekunden: null, ende: null })])).toBe(0);
  });
});

/**
 * **Die Lücke zwischen zwei Schritten ist eine eigene Zeile.**
 *
 * Ohne sie steht die Wartezeit in keiner Schrittdauer — und genau sie ist bei
 * einer hängenden Nachricht oft die ganze Antwort.
 */
describe("Die Lücke zwischen zwei Schritten", () => {
  const ersterSchritt = schritt({
    position: 1,
    start: "2025-12-29T20:00:00Z",
    ende: "2025-12-29T20:00:05Z",
    dauerSekunden: 5,
  });

  it("erscheint ab der Schwelle und darunter nicht", () => {
    const knappDarunter = schritt({ position: 2, start: "2025-12-29T20:00:59Z" });
    const genauAufDerSchwelle = schritt({ position: 2, start: "2025-12-29T20:01:05Z" });
    const lange = schritt({ position: 2, start: "2025-12-29T23:12:05Z" });

    expect(luecke(ersterSchritt, knappDarunter)).toBeNull();
    expect(luecke(ersterSchritt, genauAufDerSchwelle)).toBe(LUECKE_SCHWELLE_SEKUNDEN);
    expect(luecke(ersterSchritt, lange)).toBe(3 * 3600 + 12 * 60);
  });

  /** Negative oder null-Lücken erzeugen keine Zeile. */
  it("erzeugt bei negativem oder null-Abstand keine Zeile", () => {
    expect(
      luecke(ersterSchritt, schritt({ position: 2, start: "2025-12-29T20:00:05Z" })),
    ).toBeNull();
    expect(
      luecke(ersterSchritt, schritt({ position: 2, start: "2025-12-29T19:59:00Z" })),
    ).toBeNull();
  });

  /**
   * Nach einem Schritt ohne Ende gibt es keinen Zwischenraum, sondern einen
   * offenen Schritt. Eine Lücke „seit dem Nichts" wäre eine erfundene Zahl.
   */
  it("erzeugt nach einem Schritt ohne Ende keine Zeile", () => {
    const offen = schritt({ position: 1, ende: null, dauerSekunden: null });

    expect(luecke(offen, schritt({ position: 2, start: "2025-12-29T23:12:05Z" }))).toBeNull();
  });

  it("hängt die Zeile zwischen die beiden Schritte", () => {
    const zeilen = zeitleiste(
      detail({
        schritte: [ersterSchritt, schritt({ position: 2, start: "2025-12-29T23:12:05Z" })],
      }),
    );

    expect(zeilen.map((zeile) => zeile.art)).toEqual(["schritt", "luecke", "schritt"]);
    expect(zeilen[1]).toMatchObject({ art: "luecke", sekunden: 3 * 3600 + 12 * 60 });
  });
});

/**
 * **Der offene Zustand wird gezeigt, nicht errechnet.** Das Backend liefert ihn
 * als Feld; die Oberfläche stellt ihn dar und leitet nichts selbst ab.
 */
describe("Der offene Zustand", () => {
  /**
   * `WARTET_VOR` ist der **gemessene Normalfall** des Wartens (M16 3): Bei allen
   * 538 wartenden Nachrichten der Testkopie ist jede Aktion beendet. Nach dem
   * letzten ausgeführten Schritt steht deshalb der nächste als noch nicht
   * begonnen.
   */
  it("hängt bei WARTET_VOR den nächsten Schritt an", () => {
    const zeilen = zeitleiste(
      detail({
        statusKind: "WARTEND",
        offenerZustand: "WARTET_VOR",
        naechsterSchritt: "Send Message to Pool",
        schritte: [schritt({ position: 1 })],
      }),
    );

    expect(zeilen.map((zeile) => zeile.art)).toEqual(["schritt", "erwartet"]);
    expect(zeilen[1]).toMatchObject({
      art: "erwartet",
      name: "Send Message to Pool",
      bereitsGelaufen: false,
    });
  });

  /**
   * **Der Befund aus der Sichtprüfung vom 07.08.2026, und der Grund für
   * `bereitsGelaufen`.**
   *
   * Bei den wartenden Nachrichten der Testkopie benennt `naechsterSchritt`
   * denselben Schritt, der gerade gelaufen ist: „Send Message to Pool" mit dem
   * Rohwert `NXS_MERGE|BMW|WAITUNTIL|…|SUSPEND` — der Schritt, der die Nachricht
   * schlafen legt. Ohne diese Unterscheidung stünde „noch nicht begonnen"
   * unmittelbar unter „2 min".
   */
  it("erkennt, wenn der benannte nächste Schritt schon gelaufen ist", () => {
    const zeilen = zeitleiste(
      detail({
        statusKind: "WARTEND",
        offenerZustand: "WARTET_VOR",
        naechsterSchritt: "Send Message to Pool",
        schritte: [
          schritt({ position: 1, name: "Konverter VDA4908 an GSVERF IDOC" }),
          schritt({
            position: 2,
            name: "Send Message to Pool",
            rohwert: "NXS_MERGE|BMW|WAITUNTIL|now+170H|SUSPEND",
            dauerSekunden: 120,
          }),
        ],
      }),
    );

    expect(zeilen.at(-1)).toMatchObject({
      art: "erwartet",
      name: "Send Message to Pool",
      bereitsGelaufen: true,
    });
  });

  /**
   * Ist der nächste Schritt `null`, wird das **benannt und nicht weggelassen**:
   * Die Nachricht wartet, wir wissen nur nicht worauf. Ohne die Zeile sähe eine
   * offene Nachricht wie eine abgeschlossene aus.
   */
  it("hängt die Zeile auch ohne benannten nächsten Schritt an", () => {
    const zeilen = zeitleiste(
      detail({
        statusKind: "WARTEND",
        offenerZustand: "WARTET_VOR",
        naechsterSchritt: null,
        schritte: [schritt({ position: 1 })],
      }),
    );

    expect(zeilen.at(-1)).toMatchObject({ art: "erwartet", name: null });
  });

  /**
   * Bei `LAEUFT_AUF` markiert die Leiste den betroffenen Schritt — und hängt
   * **nichts** an. Abgeschnitten wird trotzdem nichts: Der laufende Schritt ist
   * in den Daten der letzte, und gemessene Zeilen wegzulassen wäre etwas
   * anderes als „die Leiste endet dort".
   */
  it("hängt bei LAEUFT_AUF nichts an und lässt nichts weg", () => {
    const zeilen = zeitleiste(
      detail({
        statusKind: "LAEUFT",
        offenerZustand: "LAEUFT_AUF",
        schritte: [
          schritt({ position: 1 }),
          schritt({ position: 2, ende: null, dauerSekunden: null, laeuftAuf: true }),
        ],
      }),
    );

    expect(zeilen.map((zeile) => zeile.art)).toEqual(["schritt", "schritt"]);
    expect(zeilen[1]).toMatchObject({ art: "schritt", anteil: null });
  });

  /**
   * `OHNE_SCHRITT` und ein abgeschlossener Fall ohne Schritt sehen in der Liste
   * der Zeilen gleich aus — leer. Was der Nutzer liest, entscheidet die
   * Komponente über den Zustand und **nicht** über die Länge dieser Liste;
   * sonst hieße „offen und ohne Schritt" dasselbe wie „fertig und ohne Schritt".
   */
  it("hängt bei OHNE_SCHRITT und KEINER nichts an", () => {
    expect(zeitleiste(detail({ statusKind: "WARTEND", offenerZustand: "OHNE_SCHRITT" }))).toEqual(
      [],
    );
    expect(zeitleiste(detail({ offenerZustand: "KEINER" }))).toEqual([]);
  });
});

/**
 * Das Detail führt `bedeutungNichtVerifiziert` **nicht** — es ist genau die
 * Einordnung `UNGEKLAERT` und damit aus der Antwort ableitbar. Ein zweites Feld
 * dafür wäre eine zweite Wahrheit; abgeleitet wird es an einer Stelle.
 */
describe("Bedeutung nicht verifiziert", () => {
  it("ist genau die Einordnung UNGEKLAERT", () => {
    expect(bedeutungNichtVerifiziert("UNGEKLAERT")).toBe(true);
    expect(bedeutungNichtVerifiziert("WARTEND")).toBe(false);
    expect(bedeutungNichtVerifiziert("FEHLER")).toBe(false);
  });
});
