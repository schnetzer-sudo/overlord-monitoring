import { describe, expect, it } from "vitest";

import type { Eigenschaft, Nachrichtendetail, Schritt } from "@/features/nachrichten/api";
import {
  BALKEN_MINDESTANTEIL,
  balkenanteil,
  bedeutungNichtVerifiziert,
  laengsteDauer,
  schrittAufklappbar,
  verteileEigenschaften,
  wartezeile,
  zeitleiste,
  zusatzzeile,
  type Eigenschaftenverteilung,
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
    bamAnzahl: 0,
    offenerZustand: "KEINER",
    naechsterSchritt: null,
    wartetSeitSekunden: null,
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
 * Gerechnet wird hier nichts: `wartetSeitSekunden` und `fristSekunden` kommen
 * fertig aus dem Backend, gegen die **Anwendungsuhr** gerechnet. Die Uhr steht im
 * Profil `dev` Monate zurück — eine Dauer aus `Date.now()` wäre dort um Monate
 * falsch. Entschieden wird hier nur, ob die Zeile erscheint und welches Verb sie
 * trägt.
 *
 * ⚠️ **Die Kategorie ist am 03.09.2026 aus der Zeile gefallen** (E‑71). Hier
 * stand ein drittes Feld `ueberfaellig`, und zwei Fälle prüften es. Sie sind
 * **entfernt und nicht übersprungen**: Ein `skip` hinterließe einen Test, der
 * eine Gestalt bezeugt, die es nicht mehr gibt.
 */
describe("Die Wartezeile", () => {
  /**
   * **Der Rumpf trägt hier `fristSekunden`, obwohl `WARTEND` es seit E‑76 nie
   * mehr liefert** — und das ist Absicht: Geprüft ist, dass die Zeile *durchreicht
   * und nicht nachrechnet*. Dass bei `WARTEND` `null` kommt, ist eine Zusage des
   * Backends (`NachrichtendetailDbIT`) und keine Bedingung dieser Funktion; sie
   * hier nachzubauen wäre dieselbe Regel ein zweites Mal.
   */
  it("erscheint beim Warten und reicht Dauer und Frist durch", () => {
    expect(
      wartezeile(
        detail({
          statusKind: "WARTEND",
          offenerZustand: "WARTET_IN",
          wartetSeitSekunden: 15_120,
          fristSekunden: 1800,
        }),
      ),
    ).toEqual({ laeuft: false, sekunden: 15_120, fristSekunden: 1800 });
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
    ).toMatchObject({ fristSekunden: null });
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
        }),
      ),
    ).toEqual({ laeuft: false, sekunden: 4620, fristSekunden: 1800 });
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

/**
 * **Wohin jede technische Eigenschaft gehört** *(21.09.2026, E‑219,
 * `docs/nachrichtendetail.md` §10.16)*.
 *
 * `verteileEigenschaften` ersetzt `gruppiereEigenschaften` vom 17.08.2026 und
 * mit ihr deren zehn Fälle. Die Eigenschaften stehen nicht mehr gruppiert in
 * einem Block, sondern **unter ihrem Schritt in der Zeitleiste**; im Block
 * bleiben allein die allgemeinen Angaben zur Nachricht. Eingeteilt wird an genau
 * dieser einen Stelle, und deshalb steht die Einteilung als reine Funktion neben
 * der Komponente.
 *
 * Geprüft wird vor allem, was die Funktion **nicht** tut: umsortieren,
 * zusammenfassen, etwas fallen lassen oder einen Namen großzügig lesen.
 */
function eigenschaft(
  werte: Partial<Eigenschaft> & { name: string; position: number },
): Eigenschaft {
  return {
    wert: "0050",
    gekappt: false,
    originalLaengeBytes: null,
    ...werte,
  };
}

/** Die Namen eines Teils, in seiner Reihenfolge. */
function namen(eintraege: readonly Eigenschaft[]): string[] {
  return eintraege.map((eintrag) => eintrag.name);
}

/** Wie viele Eigenschaften die vier Teile zusammen tragen. */
function summe(verteilung: Eigenschaftenverteilung): number {
  return (
    verteilung.allgemein.length +
    verteilung.eingang.length +
    [...verteilung.jeSchritt.values()].reduce((bisher, teil) => bisher + teil.length, 0) +
    verteilung.ohneZeile.reduce((bisher, teil) => bisher + teil.eintraege.length, 0)
  );
}

describe("Die Einteilung der Eigenschaften", () => {
  /**
   * **Die vier Teile an einer Nachricht**, wie sie gemessen aussieht: die
   * `Message.*`-Familie und die des Lesedienstes auf Schritt `0`, dazu zwei
   * ausgeführte Schritte — und eine Position, zu der es keine Zeile gibt.
   */
  it("teilt in allgemein, Eingang, je Schritt und ohne Zeile", () => {
    const verteilung = verteileEigenschaften(
      [
        eigenschaft({ name: "Converter.Type", position: 1 }),
        eigenschaft({ name: "Message.GUID", position: 0 }),
        eigenschaft({ name: "OFTPReader.VirtualFilename", position: 0 }),
        eigenschaft({ name: "Service.Type", position: 2 }),
        eigenschaft({ name: "Service.Type", position: 9 }),
      ],
      [schritt({ position: 1 }), schritt({ position: 2 })],
    );

    expect(namen(verteilung.allgemein)).toEqual(["Message.GUID"]);
    expect(namen(verteilung.eingang)).toEqual(["OFTPReader.VirtualFilename"]);
    expect([...verteilung.jeSchritt.keys()]).toEqual([1, 2]);
    expect(namen(verteilung.jeSchritt.get(1) ?? [])).toEqual(["Converter.Type"]);
    expect(namen(verteilung.jeSchritt.get(2) ?? [])).toEqual(["Service.Type"]);
    expect(verteilung.ohneZeile.map((teil) => teil.position)).toEqual([9]);
  });

  /**
   * **Das Präfix ist `Message.` — exakt, mit Punkt und in dieser Schreibung.**
   * `Message` ohne Punkt, `MessageX.…` und `message.…` gehören nicht zu den
   * allgemeinen Angaben. **Sie fallen dadurch nicht heraus:** Sie stehen
   * sichtbar im Eingang, und ein anders geschriebener Name bleibt damit
   * auffindbar statt stillschweigend einsortiert.
   */
  it("nimmt in allgemein nur, was exakt mit `Message.` beginnt", () => {
    const verteilung = verteileEigenschaften(
      [
        eigenschaft({ name: "Message", position: 0 }),
        eigenschaft({ name: "Message.SendingPartner", position: 0 }),
        eigenschaft({ name: "MessageX.Irgendwas", position: 0 }),
        eigenschaft({ name: "message.klein", position: 0 }),
        eigenschaft({ name: "MESSAGE.GROSS", position: 0 }),
      ],
      [],
    );

    expect(namen(verteilung.allgemein)).toEqual(["Message.SendingPartner"]);
    expect(namen(verteilung.eingang)).toEqual([
      "Message",
      "MessageX.Irgendwas",
      "message.klein",
      "MESSAGE.GROSS",
    ]);
  });

  /**
   * **Die Regel hängt an beiden Bedingungen, nicht am Namen allein.** Gemessen
   * steht die `Message.*`-Familie ausnahmslos auf Schritt `0` (M17 3) — das ist
   * eine Messung und keine Zusage des Schemas. Stünde eines Tages ein
   * `Message.*` an einem Schritt, gehört es dorthin und nicht in den Block.
   */
  it("lässt ein `Message.*` auf einer Position ungleich 0 an seinem Schritt", () => {
    const verteilung = verteileEigenschaften(
      [eigenschaft({ name: "Message.SplitCount", position: 2 })],
      [schritt({ position: 2 })],
    );

    expect(verteilung.allgemein).toEqual([]);
    expect(namen(verteilung.jeSchritt.get(2) ?? [])).toEqual(["Message.SplitCount"]);
  });

  /**
   * **Position `0` geht immer in die ersten beiden Teile** — auch wenn
   * `schritte[]` eines Tages eine Zeile mit Position `0` führte. Die Leiste
   * bekommt für den Metadaten-Schritt keine Zeile, und seine Eigenschaften
   * hätten sonst zwei mögliche Orte.
   */
  it("gibt Position 0 nie an einen Schritt, auch wenn `schritte[]` sie führt", () => {
    const verteilung = verteileEigenschaften(
      [
        eigenschaft({ name: "Message.GUID", position: 0 }),
        eigenschaft({ name: "Service.Type", position: 0 }),
      ],
      [schritt({ position: 0 }), schritt({ position: 1 })],
    );

    expect(verteilung.jeSchritt.size).toBe(0);
    expect(namen(verteilung.allgemein)).toEqual(["Message.GUID"]);
    expect(namen(verteilung.eingang)).toEqual(["Service.Type"]);
  });

  /**
   * **Die Invariante: Die Summe der vier Teile ist die Länge der Eingabe.** An
   * einer Eingabe, die jeden Teil füllt und jede Falle enthält — denselben
   * Namen mehrfach, eine Position ohne Zeile, ein klein geschriebenes
   * `message.`.
   */
  it("verliert und erfindet nichts: die Summe der Teile ist die Länge der Eingabe", () => {
    const eingabe = [
      eigenschaft({ name: "Converter.Log.GUID", position: 1 }),
      eigenschaft({ name: "Converter.Log.GUID", position: 2 }),
      eigenschaft({ name: "Message.GUID", position: 0 }),
      eigenschaft({ name: "Service.Type", position: 0 }),
      eigenschaft({ name: "Service.Type", position: 1 }),
      eigenschaft({ name: "Service.Type", position: 2 }),
      eigenschaft({ name: "Service.Type", position: 7 }),
      eigenschaft({ name: "message.klein", position: 0 }),
    ];

    const verteilung = verteileEigenschaften(eingabe, [
      schritt({ position: 1 }),
      schritt({ position: 2 }),
    ]);

    expect(summe(verteilung)).toBe(eingabe.length);
  });

  /**
   * **In jedem Teil bleibt die Reihenfolge der Antwort** — kein
   * Ersatzsortierer. Sie ist die des Backends (`MessagePropertyName`, dann
   * `MessageActionID`) und keine Zusage dieser Funktion; hier steht sie
   * absichtlich *nicht* alphabetisch, damit ein Sortierlauf auffiele.
   */
  it("sortiert in keinem Teil um", () => {
    const verteilung = verteileEigenschaften(
      [
        eigenschaft({ name: "Message.Z", position: 0 }),
        eigenschaft({ name: "Reader.Z", position: 0 }),
        eigenschaft({ name: "Schritt.Z", position: 1 }),
        eigenschaft({ name: "Rest.Z", position: 8 }),
        eigenschaft({ name: "Message.A", position: 0 }),
        eigenschaft({ name: "Reader.A", position: 0 }),
        eigenschaft({ name: "Schritt.A", position: 1 }),
        eigenschaft({ name: "Rest.A", position: 8 }),
      ],
      [schritt({ position: 1 })],
    );

    expect(namen(verteilung.allgemein)).toEqual(["Message.Z", "Message.A"]);
    expect(namen(verteilung.eingang)).toEqual(["Reader.Z", "Reader.A"]);
    expect(namen(verteilung.jeSchritt.get(1) ?? [])).toEqual(["Schritt.Z", "Schritt.A"]);
    expect(namen(verteilung.ohneZeile[0]?.eintraege ?? [])).toEqual(["Rest.Z", "Rest.A"]);
  });

  /**
   * **Die Schrittfolge kommt aus `schritte[]` und nicht aus der Zahl.** Die
   * Ordnung der Schritte steht an genau einer Stelle, im `ORDER BY` von
   * `findeAktionen`; eine zweite Sortierung hier wäre die Drift, gegen die
   * diese Regel gerichtet ist.
   */
  it("folgt in `jeSchritt` der Reihenfolge von `schritte[]` und nicht der Zahl", () => {
    const verteilung = verteileEigenschaften(
      [
        eigenschaft({ name: "Service.Type", position: 1 }),
        eigenschaft({ name: "Service.Type", position: 2 }),
        eigenschaft({ name: "Service.Type", position: 3 }),
      ],
      [schritt({ position: 3 }), schritt({ position: 1 }), schritt({ position: 2 })],
    );

    expect([...verteilung.jeSchritt.keys()]).toEqual([3, 1, 2]);
  });

  /** Ohne Zeile ist die Zahl das einzige, was es an Ordnung gibt. */
  it("ordnet `ohneZeile` aufsteigend nach der Position", () => {
    const verteilung = verteileEigenschaften(
      [
        eigenschaft({ name: "Service.Type", position: 12 }),
        eigenschaft({ name: "Service.Type", position: 4 }),
        eigenschaft({ name: "Service.Type", position: 7 }),
      ],
      [],
    );

    expect(verteilung.ohneZeile.map((teil) => teil.position)).toEqual([4, 7, 12]);
  });

  /**
   * **Derselbe Name in mehreren Teilen bleibt mehrfach stehen** — genau das ist
   * der Punkt: 31 der 101 gemessenen Namen kommen auf mehr als einem Schritt vor
   * (M17 3), und es sind verschiedene Einträge mit verschiedenen Werten.
   */
  it("fasst denselben Namen in mehreren Teilen nicht zusammen", () => {
    const verteilung = verteileEigenschaften(
      [
        eigenschaft({ name: "Service.Type", position: 0, wert: "ScheduleBean" }),
        eigenschaft({ name: "Service.Type", position: 1, wert: "FileReader" }),
        eigenschaft({ name: "Service.Type", position: 5, wert: "Converter" }),
      ],
      [schritt({ position: 1 })],
    );

    expect(verteilung.eingang.map((eintrag) => eintrag.wert)).toEqual(["ScheduleBean"]);
    expect((verteilung.jeSchritt.get(1) ?? []).map((eintrag) => eintrag.wert)).toEqual([
      "FileReader",
    ]);
    expect(verteilung.ohneZeile[0]?.eintraege.map((eintrag) => eintrag.wert)).toEqual([
      "Converter",
    ]);
  });

  /** Ein gekappter Wert bleibt gekappt — mit seiner ursprünglichen Länge. */
  it("reicht `gekappt` und `originalLaengeBytes` unverändert durch", () => {
    const gekappt = eigenschaft({
      name: "Converter.Payload",
      position: 1,
      gekappt: true,
      originalLaengeBytes: 48211,
    });

    const verteilung = verteileEigenschaften([gekappt], [schritt({ position: 1 })]);

    expect(verteilung.jeSchritt.get(1)).toEqual([gekappt]);
  });

  it("liefert für eine leere Eingabe vier leere Teile", () => {
    const verteilung = verteileEigenschaften([], [schritt({ position: 1 })]);

    expect(verteilung.allgemein).toEqual([]);
    expect(verteilung.eingang).toEqual([]);
    expect(verteilung.jeSchritt.size).toBe(0);
    expect(verteilung.ohneZeile).toEqual([]);
  });
});

/**
 * **Aufklappbar ist nur, was Inhalt hat** *(E‑221)* — und solange die
 * Eigenschaften laden, nichts. Eine Schaltfläche, die einen leeren Bereich
 * öffnet, ist schlimmer als keine.
 */
describe("Was an der Zeitleiste aufklappbar ist", () => {
  const verteilung = verteileEigenschaften(
    [
      eigenschaft({ name: "Service.Type", position: 1 }),
      eigenschaft({ name: "Service.Type", position: 9 }),
    ],
    // Schritt 502 ist der gemessene Fall: Er steht in `MessageAction` und fehlt
    // in `MessageProperty` (M17 3).
    [schritt({ position: 1 }), schritt({ position: 502 })],
  );

  it("macht einen Schritt mit Eigenschaften aufklappbar und einen ohne nicht", () => {
    expect(schrittAufklappbar(verteilung, 1)).toBe(true);
    expect(schrittAufklappbar(verteilung, 502)).toBe(false);
  });

  it("macht einen Schritt nicht aufklappbar, dessen Eigenschaften ohne Zeile geführt werden", () => {
    // Position 9 hat Eigenschaften, aber keine Zeile — sie stehen im Rest.
    expect(schrittAufklappbar(verteilung, 9)).toBe(false);
  });

  it("macht nichts aufklappbar, solange die Eigenschaften laden oder ihre Abfrage gescheitert ist", () => {
    expect(schrittAufklappbar(undefined, 1)).toBe(false);
  });

  /**
   * **Eingang und Rest: Es gibt die Zeile, wenn dort ein Artefakt oder eine
   * Eigenschaft liegt; aufklappbar ist sie nur mit Eigenschaften** (E‑222,
   * E‑223). Bis zum 21.09.2026 hing ihre Existenz allein an den Artefakten.
   */
  it("entscheidet über Eingangs- und Restzeile aus Zielen und Eigenschaften", () => {
    // nur Ziele — der Lesedienst ohne weitere Eigenschaft auf Schritt 0
    expect(zusatzzeile(2, 0)).toEqual({ vorhanden: true, aufklappbar: false });
    // nur Eigenschaften — kein Lesedienst, aber etwas liegt dort
    expect(zusatzzeile(0, 3)).toEqual({ vorhanden: true, aufklappbar: true });
    // beides
    expect(zusatzzeile(2, 7)).toEqual({ vorhanden: true, aufklappbar: true });
    // nichts — wo nichts liegt, hängt nichts
    expect(zusatzzeile(0, 0)).toEqual({ vorhanden: false, aufklappbar: false });
  });
});
