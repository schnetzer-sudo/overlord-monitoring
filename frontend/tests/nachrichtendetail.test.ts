import { describe, expect, it } from "vitest";

import type { Nachrichtendetail, Schritt } from "@/features/nachrichten/api";
import {
  BALKEN_MINDESTANTEIL,
  balkenanteil,
  bedeutungNichtVerifiziert,
  laengsteDauer,
  wartezeile,
  zeitleiste,
} from "@/features/nachrichten/detail";

/**
 * Die Zeitleiste der Detailansicht.
 *
 * Geprüft werden die **Entscheidungen**, nicht das Markup: Wie breit ein Balken
 * wird, was der offene Zustand anhängt und wann die Wartezeile erscheint. Alles
 * drei ist reine Rechnung und liegt deshalb neben der Komponente und nicht in
 * ihr.
 *
 * **Die Lückenzeile ist am 10.08.2026 entfernt worden** und wird hier deshalb
 * nicht mehr geprüft. Sie stand zwischen zwei Schritten und ist über rund 700
 * geprüfte Nachrichten nie erschienen: Die Wartezeit steckt in der Dauer des
 * `WAITUNTIL`-Schritts, nicht im Zwischenraum. Begründung in
 * `nachrichtendetail.md` §10.12.
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
    // Leer statt fehlend: „nicht in einer Kette" ist eine Aussage, „unbekannt"
    // wäre keine. Was daraus wird, prüft `kette.test.ts`.
    rollen: [],
    zeitpunkt: "2025-12-29T22:53:50Z",
    start: "2025-12-29T22:41:12Z",
    gesamtdauerSekunden: 758,
    fristSekunden: 1800,
    eigenschaftenAnzahl: 22,
    offenerZustand: "KEINER",
    naechsterSchritt: null,
    wartetSeitSekunden: null,
    ueberfaellig: false,
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
 * **Es entsteht keine Zeile zwischen zwei Schritten** — auch nicht bei einem
 * Abstand von Stunden.
 *
 * Der Test steht hier, damit die Streichung eine Zusage ist und kein Versehen:
 * Die Lückenzeile ist gebaut worden, über rund 700 geprüfte Nachrichten nie
 * erschienen (größter Abstand: **eine Sekunde**) und am 10.08.2026 entfernt
 * worden. Wer sie wieder einbaut, macht diesen Test rot und findet die
 * Begründung in `nachrichtendetail.md` §10.12.
 *
 * Die Zeit, die doch einmal zwischen zwei Schritten steckt, wird über
 * `gesamtdauerSekunden` im Kopf sichtbar.
 */
describe("Der Abstand zwischen zwei Schritten", () => {
  it("bekommt keine eigene Zeile, auch bei drei Stunden nicht", () => {
    const zeilen = zeitleiste(
      detail({
        schritte: [
          schritt({
            position: 1,
            start: "2025-12-29T20:00:00Z",
            ende: "2025-12-29T20:00:05Z",
            dauerSekunden: 5,
          }),
          schritt({ position: 2, start: "2025-12-29T23:12:05Z" }),
        ],
      }),
    );

    expect(zeilen.map((zeile) => zeile.art)).toEqual(["schritt", "schritt"]);
  });
});

/**
 * **Der offene Zustand wird gezeigt, nicht errechnet.** Das Backend liefert ihn
 * als Feld; die Oberfläche stellt ihn dar und leitet nichts selbst ab.
 */
describe("Der offene Zustand", () => {
  /**
   * ⚠️ `WARTET_VOR` kommt in der Testkopie **null Mal** vor (M29, `n = 538`).
   * Der Zweig ist gebaut und ausschließlich hier belegt — nicht gegen echte
   * Daten. Steht so in `nachrichtendetail.md` §10.12.
   */
  it("hängt bei WARTET_VOR den noch nicht begonnenen Schritt an", () => {
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
   * **`WARTET_IN` ist der gemessene Normalfall** — 538 von 538 (M29). Der Verweis
   * zeigt auf den Schritt, der die Nachricht schlafen gelegt hat: „Send Message
   * to Pool" mit dem Rohwert `NXS_MERGE|BMW|WAITUNTIL|…|SUSPEND`. Sein Name steht
   * bereits in der Leiste; ihn zu wiederholen ergäbe „noch nicht begonnen"
   * unmittelbar unter „2 min".
   *
   * **Entschieden wird das im Backend.** Bis zum 10.08.2026 verglich diese Datei
   * dafür Namen — jetzt trägt der gelieferte Zustand die Antwort, und der Test
   * setzt kein einziges Feld, aus dem sich hier noch etwas ableiten ließe.
   */
  it("wiederholt bei WARTET_IN den Namen nicht", () => {
    const zeilen = zeitleiste(
      detail({
        statusKind: "WARTEND",
        offenerZustand: "WARTET_IN",
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
   * **Gleiche Namen entscheiden nichts mehr.** Trüge ein *anderer* Schritt
   * zufällig denselben Namen wie der Verweis, hätte der alte Namensvergleich
   * `WARTET_IN` daraus gemacht. Jetzt steht `WARTET_VOR` im Feld, und die Zeile
   * folgt dem Feld.
   */
  it("folgt dem gelieferten Zustand und nicht dem Namen", () => {
    const zeilen = zeitleiste(
      detail({
        statusKind: "WARTEND",
        offenerZustand: "WARTET_VOR",
        naechsterSchritt: "Send Message to Pool",
        schritte: [schritt({ position: 1, name: "Send Message to Pool", dauerSekunden: 120 })],
      }),
    );

    expect(zeilen.at(-1)).toMatchObject({ art: "erwartet", bereitsGelaufen: false });
  });

  /**
   * Ist der benannte Schritt `null`, wird das **benannt und nicht weggelassen**:
   * Die Nachricht wartet, wir wissen nur nicht worauf. Ohne die Zeile sähe eine
   * offene Nachricht wie eine abgeschlossene aus.
   */
  it("hängt die Zeile auch ohne benannten Schritt an", () => {
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
   * `EMPFANGEN`, `OHNE_AKTION` und ein abgeschlossener Fall ohne Schritt sehen in
   * der Liste der Zeilen gleich aus — leer. Was der Nutzer liest, entscheidet die
   * Komponente über den **Zustand** und nicht über die Länge dieser Liste; sonst
   * hieße „angekommen und seitdem nichts" dasselbe wie „fertig und ohne Schritt".
   *
   * **Der Metadaten-Schritt wird auch bei `EMPFANGEN` keine Zeile** (S1). Was sich
   * am 10.08.2026 geändert hat, ist der Satz über der leeren Leiste — nicht ihr
   * Inhalt.
   */
  it("hängt bei EMPFANGEN, OHNE_AKTION und KEINER nichts an", () => {
    expect(zeitleiste(detail({ statusKind: "WARTEND", offenerZustand: "EMPFANGEN" }))).toEqual([]);
    expect(zeitleiste(detail({ statusKind: "WARTEND", offenerZustand: "OHNE_AKTION" }))).toEqual(
      [],
    );
    expect(zeitleiste(detail({ offenerZustand: "KEINER" }))).toEqual([]);
  });
});

/**
 * **Die Wartezeile am offenen Zustand** — sie ersetzt die Lückenzeile.
 *
 * Gerechnet wird hier nichts: `wartetSeitSekunden`, `fristSekunden` und
 * `ueberfaellig` kommen fertig aus dem Backend, gegen die **Anwendungsuhr**
 * gerechnet. Die Uhr steht im Profil `dev` Monate zurück — eine Dauer aus
 * `Date.now()` wäre dort um Monate falsch. Entschieden wird hier nur, ob die
 * Zeile erscheint und welches Verb sie trägt.
 */
describe("Die Wartezeile", () => {
  it("erscheint beim Warten und trägt Dauer, Frist und Kategorie", () => {
    expect(
      wartezeile(
        detail({
          statusKind: "WARTEND",
          offenerZustand: "WARTET_IN",
          wartetSeitSekunden: 15_120,
          fristSekunden: 1800,
          ueberfaellig: true,
        }),
      ),
    ).toEqual({ laeuft: false, sekunden: 15_120, fristSekunden: 1800, ueberfaellig: true });
  });

  it("unterscheidet Laufen vom Warten, damit die Zeile das richtige Verb trägt", () => {
    expect(
      wartezeile(
        detail({
          statusKind: "LAEUFT",
          offenerZustand: "LAEUFT_AUF",
          wartetSeitSekunden: 42,
        }),
      ),
    ).toMatchObject({ laeuft: true, sekunden: 42 });
  });

  /**
   * Eine Nachricht ohne gesetzten Timeout wartet trotzdem — dann steht eben nur
   * die eine Hälfte da. Eine erfundene Frist wäre schlechter als keine.
   */
  it("kommt ohne Frist aus", () => {
    expect(
      wartezeile(
        detail({
          statusKind: "WARTEND",
          offenerZustand: "WARTET_IN",
          wartetSeitSekunden: 90,
          fristSekunden: null,
        }),
      ),
    ).toMatchObject({ fristSekunden: null, ueberfaellig: false });
  });

  /**
   * Ohne Wartedauer gibt es nichts zu sagen: bei `OHNE_AKTION` — dort gibt es
   * keinen Anker — und bei jeder nicht offenen Nachricht.
   */
  it("bleibt ohne Wartedauer aus", () => {
    expect(wartezeile(detail({ offenerZustand: "KEINER" }))).toBeNull();
    expect(wartezeile(detail({ statusKind: "WARTEND", offenerZustand: "OHNE_AKTION" }))).toBeNull();
  });

  /**
   * **Bei `EMPFANGEN` steht sie da wie bei den anderen offenen Zuständen.** Das
   * ist die Korrektur vom 10.08.2026: Eine Nachricht, die um 14:32 angekommen und
   * seitdem nicht angefasst worden ist, hängt — mit `null` sagte das Feld an der
   * einzigen Stelle nichts, an der es etwas zu sagen hätte.
   *
   * Das Verb ist „wartet", nicht „läuft": `laeuft` hängt allein an `LAEUFT_AUF`.
   */
  it("erscheint bei EMPFANGEN mit dem Verb des Wartens", () => {
    expect(
      wartezeile(
        detail({
          statusKind: "WARTEND",
          offenerZustand: "EMPFANGEN",
          wartetSeitSekunden: 4620,
          ueberfaellig: true,
        }),
      ),
    ).toEqual({ laeuft: false, sekunden: 4620, fristSekunden: 1800, ueberfaellig: true });
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
