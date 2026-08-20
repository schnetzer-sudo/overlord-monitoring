import { describe, expect, it } from "vitest";

import { texteFuer } from "@/i18n";
import type { Artefakt, Artefaktanzeige, Artefaktliste, Schritt } from "@/features/nachrichten/api";
import {
  anzeigevermerke,
  artefaktBeschriftung,
  artefakteintraege,
  artefaktziel,
  downloadMoeglich,
  downloadPfad,
  erneutVersuchenSinnvoll,
  findeArtefakt,
  zieleJeSchritt,
  zieleOhneZeile,
} from "@/features/nachrichten/rohdaten";
import { artefaktAnsicht, nachrichtAnsicht } from "@/lib/routen";

/**
 * **Die Entscheidungen von Schritt 8, Teil Frontend** — als reine Funktionen und
 * ohne einen gerenderten Baum (`docs/frontend-grundlagen.md` §9).
 *
 * Drei davon tragen das Feature:
 *
 * 1. **Die Beschriftung.** Die Artefaktliste liefert keinen lesbaren
 *    Schrittnamen — 55,98 % der Artefakte lösen zu gar keinem auf (M57). Was der
 *    Nutzer liest, entsteht hier.
 * 2. **Der Gleichlauf.** Ein Download-Knopf darf nur dort stehen, wo der
 *    Endpunkt auch etwas liefert (`docs/rohdaten.md` §3, Entscheidung 9).
 * 3. **Die Vermerke.** Wo Ausschnitt oder Kappung gegriffen haben, muss es
 *    dastehen — und wo nicht, darf nichts dastehen.
 *
 * **Kein Testdatensatz enthält echten Dateiinhalt**, keinen echten Partner,
 * keinen echten Knoten, keine echte Kennung. Alles hier ist erfunden.
 */

const TEXTE = texteFuer("de");

function artefakt(teile: Partial<Artefakt> & Pick<Artefakt, "artefaktId">): Artefakt {
  return {
    name: "Converter.Payload.GUID",
    familie: "Converter",
    art: "NUTZDATEN",
    schritt: 2,
    beschnittMoeglich: false,
    ...teile,
  };
}

function schritt(position: number, name: string): Schritt {
  return {
    position,
    name,
    namensherkunft: "DIREKT",
    rohwert: "ERFUNDEN_KONVERT|A2B",
    start: "2025-12-29T22:41:12Z",
    ende: "2025-12-29T22:41:12Z",
    dauerSekunden: 0,
    timeoutSekunden: 1800,
    laeuftAuf: false,
  };
}

function anzeige(teile: Partial<Artefaktanzeige> = {}): Artefaktanzeige {
  return {
    artefaktId: "2-Converter.Payload.GUID",
    name: "Converter.Payload.GUID",
    art: "NUTZDATEN",
    zustand: "ANZEIGBAR",
    text: "ERFUNDEN+1'",
    groesseBytes: 11,
    gekuerzt: false,
    beschnitten: false,
    kodierung: "ISO-8859-1",
    zipEintraege: 1,
    ...teile,
  };
}

/**
 * **Das Paar des Lesedienstes auf Schritt `0`** — seit dem 19.08.2026 alles, was
 * dort liegt.
 *
 * `Message.Payload.GUID` stand bis dahin daneben und galt als die eingegangene
 * Datei. Nach **M73** trägt der Name in 6.249 von 6.249 und 214.330 von 214.330
 * Nachrichten den Verweis der Nutzdatenzeile mit dem **höchsten
 * `MessageActionID`** derselben Nachricht; das Backend führt ihn nicht mehr.
 */
const LESEDIENST_DATEI = artefakt({
  artefaktId: "0-SAPReader.Payload.GUID",
  name: "SAPReader.Payload.GUID",
  familie: "SAPReader",
  schritt: 0,
});

const LESEDIENST_PROTOKOLL = artefakt({
  artefaktId: "0-SAPReader.Log.GUID",
  name: "SAPReader.Log.GUID",
  familie: "SAPReader",
  art: "PROTOKOLL",
  schritt: 0,
});

const LISTE: Artefaktliste = {
  messageId: "8f3a1c2e-0000-4000-8000-000000000001",
  nutzdaten: [LESEDIENST_DATEI, artefakt({ artefaktId: "2-Converter.Payload.GUID" })],
  protokolle: [
    LESEDIENST_PROTOKOLL,
    artefakt({
      artefaktId: "2-Converter.Log.GUID",
      name: "Converter.Log.GUID",
      art: "PROTOKOLL",
    }),
    artefakt({
      artefaktId: "3-FTPSender.Log.GUID",
      name: "FTPSender.Log.GUID",
      familie: "FTPSender",
      art: "PROTOKOLL",
      schritt: 3,
      beschnittMoeglich: true,
    }),
  ],
};

describe("Die Beschriftung eines Artefakts", () => {
  /**
   * **Der Regelfall, und er ist die Minderheit.** Nur 44,02 % der Artefakte
   * lösen zu einem `SOSActionName` auf (M57). Wo es einen gibt, steht er da —
   * wortgleich mit der Zeile der Zeitleiste darüber, weil er aus derselben
   * Antwort kommt.
   */
  it("nimmt den Schrittnamen, wo der Schritt auflöst", () => {
    const beschriftung = artefaktBeschriftung(
      artefakt({ artefaktId: "2-Converter.Payload.GUID" }),
      [schritt(1, "Datei gelesen"), schritt(2, "Datei konvertiert")],
      TEXTE,
    );

    expect(beschriftung).toBe("Datei konvertiert");
  });

  /**
   * **Der Rückfall für 55,98 % der Artefakte** (M57) — Schrittnummer plus
   * technische Familie.
   *
   * **Nichts wird geraten** (Regel Q4): keine Umschrift von `Converter`, keine
   * erfundene Bezeichnung. Die Familie ist die Zeichenkette aus dem
   * `MessagePropertyName` und wird als solche gezeigt.
   */
  it("fällt auf Schrittnummer und Familie zurück, wo der Schritt nicht auflöst", () => {
    const beschriftung = artefaktBeschriftung(
      artefakt({
        artefaktId: "3-FTPSender.Log.GUID",
        familie: "FTPSender",
        schritt: 3,
      }),
      [schritt(1, "Datei gelesen"), schritt(2, "Datei konvertiert")],
      TEXTE,
    );

    expect(beschriftung).toBe("Schritt 3 · FTPSender");
  });

  /**
   * Ohne Schrittfolge trägt **jedes** Artefakt den Rückfall. Der Fall tritt ein,
   * solange das Detail noch lädt — und auf der eigenen Route auch dann, wenn es
   * gar nicht geladen werden konnte. Die Ansicht bleibt benutzbar; sie
   * beschriftet nur zurückhaltender.
   */
  it("trägt ohne Schrittfolge überall den Rückfall", () => {
    const beschriftung = artefaktBeschriftung(
      artefakt({ artefaktId: "2-Converter.Payload.GUID" }),
      [],
      TEXTE,
    );

    expect(beschriftung).toBe("Schritt 2 · Converter");
  });

  /**
   * Die Gegenprobe zur Regel: **Es wird nicht über die Familie geraten.** Auch
   * ein aufgelöster Schritt verwandelt `FTPSender` in keinen deutschen Begriff,
   * und ein nicht aufgelöster erfindet keinen.
   */
  it("übersetzt die Familie nirgends", () => {
    const ohneSchritt = artefaktBeschriftung(
      artefakt({
        artefaktId: "4-HTTPSender.Log.GUID",
        familie: "HTTPSender",
        schritt: 4,
      }),
      [],
      TEXTE,
    );

    expect(ohneSchritt).toBe("Schritt 4 · HTTPSender");
  });

  /**
   * **Auf Schritt `0` sitzen die Artefakte des Lesedienstes**, je Nachricht ein
   * Paar aus Datei und Protokoll (M57, Fenster A: `SAPReader.*` 443 Zeilen je
   * Richtung, `FileReader.*` 4.199, dazu sieben weitere Reader-Familien).
   *
   * Sie tragen **die Familie allein**. „Schritt 0 · SAPReader" wäre technisch
   * richtig und fachlich falsch: Schritt `0` steht in keiner Zeile der
   * Zeitleiste, und eine Nummer, die der Nutzer nirgends wiederfindet, ist keine
   * Auskunft.
   *
   * **Seit dem 19.08.2026 ist das die erste Lage der Regel** — die Lage davor
   * galt dem Eingang und ist entfallen (M73).
   */
  it("nennt ein Artefakt auf Schritt 0 bei seiner Familie — ohne die Nummer", () => {
    const beschriftung = artefaktBeschriftung(LESEDIENST_PROTOKOLL, [], TEXTE);

    expect(beschriftung).toBe("SAPReader");
    expect(beschriftung).not.toContain("0");
  });
});

describe("Die Ziele an der Zeitleiste", () => {
  /**
   * **Die Art steht davor, damit sich zwei Ziele derselben Zeile unterscheiden
   * lassen.** Sichtbar ist nur das Zeichen; der Name hier ist der für
   * Vorleseprogramme, und ohne die Art hießen Datei und Protokoll desselben
   * Schritts gleich.
   *
   * Der Name des Schritts kommt aus derselben Funktion wie die Überschrift der
   * Ansicht — die Lektion vom 17.08.2026.
   */
  it("stellt die Art vor den Schrittnamen", () => {
    const ziel = artefaktziel(
      artefakt({
        artefaktId: "2-Converter.Log.GUID",
        name: "Converter.Log.GUID",
        art: "PROTOKOLL",
      }),
      [schritt(2, "Datei konvertiert")],
      TEXTE,
    );

    expect(ziel.name).toBe("Protokoll · Datei konvertiert");
    expect(ziel.titel).toBe("Converter.Log.GUID");
  });

  /**
   * **Jedes Ziel trägt seine Art — auch die auf Schritt `0`.**
   *
   * Die eine Ausnahme galt dem Eingang, weil *Nutzdaten · Eingegangene Datei*
   * zweimal dasselbe sagte. Sie ist am 19.08.2026 mit dem Eingang entfallen
   * (M73). An der Eingangszeile hängen jetzt zwei Ziele desselben Schritts, und
   * genau dort ist die Art das einzige, was sie im Vorleseprogramm
   * unterscheidet.
   */
  it("trägt die Art auch an den beiden Zielen der Eingangszeile", () => {
    expect(artefaktziel(LESEDIENST_DATEI, [], TEXTE).name).toBe("Nutzdaten · SAPReader");
    expect(artefaktziel(LESEDIENST_PROTOKOLL, [], TEXTE).name).toBe("Protokoll · SAPReader");
  });

  /**
   * **Der Ausschnitt wird angekündigt, bevor jemand klickt** — und zwar an
   * beiden Stellen: im Namen für Vorleseprogramme und im `title` für den Zeiger.
   * Ein `title` erscheint auf einem Berührungsgerät nicht, und ein
   * Vorleseprogramm liest ihn nicht verlässlich.
   *
   * Die **sichtbare** Marke ist am 18.08.2026 entfallen: `beschnittMoeglich` ist
   * wahr nur bei Protokollen und nur für `MANDANT` — für einen gegebenen Nutzer
   * also entweder an jedem Protokoll oder an keinem. Eine Marke an jedem
   * Protokollzeichen sagte dasselbe wie das Zeichen.
   */
  it("kündigt den Ausschnitt im Namen und im Tooltip an", () => {
    const ziel = artefaktziel(
      artefakt({
        artefaktId: "3-FTPSender.Log.GUID",
        name: "FTPSender.Log.GUID",
        familie: "FTPSender",
        art: "PROTOKOLL",
        schritt: 3,
        beschnittMoeglich: true,
      }),
      [],
      TEXTE,
    );

    expect(ziel.name).toBe("Protokoll · Schritt 3 · FTPSender — Ausschnitt");
    expect(ziel.titel).toBe(
      `FTPSender.Log.GUID\n${TEXTE.nachrichten.detail.dateien.ausschnittAnkuendigung}`,
    );
  });

  /**
   * **Eingeteilt wird nach `MessageActionID`, umsortiert wird nichts.** Innerhalb
   * eines Eimers bleibt die Reihenfolge die des Backends: Nutzdaten, dann
   * Protokolle.
   */
  it("teilt die Ziele nach Schritt ein und behält die Reihenfolge", () => {
    const eimer = zieleJeSchritt(LISTE, [schritt(2, "Datei konvertiert")], TEXTE);

    expect([...eimer.keys()]).toEqual([0, 2, 3]);
    expect(eimer.get(0)?.map((ziel) => ziel.artefaktId)).toEqual([
      "0-SAPReader.Payload.GUID",
      "0-SAPReader.Log.GUID",
    ]);
    expect(eimer.get(2)?.map((ziel) => ziel.artefaktId)).toEqual([
      "2-Converter.Payload.GUID",
      "2-Converter.Log.GUID",
    ]);
    expect(eimer.get(2)?.map((ziel) => ziel.name)).toEqual([
      "Nutzdaten · Datei konvertiert",
      "Protokoll · Datei konvertiert",
    ]);
  });

  /**
   * **`Message.Payload.GUID` erzeugt kein Ziel** — der Fall, der diese Runde
   * trägt.
   *
   * Nach **M73** (19.08.2026) trägt der Name in **6.249 von 6.249** Nachrichten
   * (Fenster A) und **214.330 von 214.330** (Fenster B) den Verweis der
   * Nutzdatenzeile mit dem **höchsten `MessageActionID`** derselben Nachricht.
   * Kein Gegenfall. Er benennt damit keine eigene Datei, sondern zeigt auf eine,
   * die ohnehin an ihrem Schritt hängt — und die Eingangszeile zeigte dieselbe
   * Datei ein zweites Mal, die die Zeitleiste am letzten Schritt schon führte.
   *
   * **Das Artefakt ist deshalb aus der Antwort des Backends entfallen**, und
   * damit aus der Oberfläche. Dieser Test hält beide Hälften fest: Die
   * Eingangszeile trägt genau das Paar des Lesedienstes, und **nirgends** hängt
   * ein Ziel unter dieser Kennung.
   *
   * **Nicht gemessen ist, ob die Datei die zuletzt erzeugte ist.** „Höchster
   * `MessageActionID`" ist die Größe, die auf dem Tisch liegt; dass die
   * Schrittnummer die Ausführungsreihenfolge ist, wäre eine Deutung. Sie steht
   * deshalb in keiner Beschriftung — und in diesem Test auch nicht.
   */
  it("erzeugt für Message.Payload.GUID kein Ziel", () => {
    const eimer = zieleJeSchritt(LISTE, [schritt(2, "Datei konvertiert")], TEXTE);
    const alle = [...eimer.values()].flat();

    expect(eimer.get(0)?.map((ziel) => ziel.artefaktId)).toEqual([
      "0-SAPReader.Payload.GUID",
      "0-SAPReader.Log.GUID",
    ]);
    expect(alle.map((ziel) => ziel.artefaktId)).not.toContain("0-Message.Payload.GUID");
    expect(alle.map((ziel) => ziel.titel)).not.toContain("Message.Payload.GUID");
    expect(alle.map((ziel) => ziel.name).join(" ")).not.toContain("Eingegangene Datei");
  });

  /** Ohne Liste keine Ziele — der Zustand, solange die Abfrage läuft. */
  it("liefert ohne Liste eine leere Einteilung", () => {
    expect(zieleJeSchritt(undefined, [], TEXTE).size).toBe(0);
  });

  /**
   * **Kein Artefakt darf lautlos verschwinden.**
   *
   * Gemessen kommt der Fall nicht vor — `ohne_schrittzeile` ist in allen 63
   * Kombinationen aus Fenster A und über 1.516.642 Zeilen in Fenster B **0**
   * (M57, Befund 1). Die Messung ist aber keine Zusage des Schemas, und seit die
   * Artefakte an der Zeitleiste hängen, fiele ein Artefakt ohne Zeile sonst aus
   * der Oberfläche.
   *
   * **Schritt `0` gehört ausdrücklich nicht dazu**: Er hat seine eigene Zeile
   * über der Leiste.
   */
  it("sammelt ein, was weder Eingangszeile noch Zeile der Zeitleiste ist", () => {
    const eimer = zieleJeSchritt(LISTE, [schritt(2, "Datei konvertiert")], TEXTE);

    expect(
      zieleOhneZeile(eimer, [schritt(2, "Datei konvertiert")]).map((ziel) => ziel.name),
    ).toEqual(["Protokoll · Schritt 3 · FTPSender — Ausschnitt"]);
  });

  /** Der gemessene Normalfall: Jede Position hat ihren Ort, der Rest ist leer. */
  it("liefert nichts, solange jede Position eine Zeile hat", () => {
    const schritte = [schritt(2, "Datei konvertiert"), schritt(3, "Datei versendet")];

    expect(zieleOhneZeile(zieleJeSchritt(LISTE, schritte, TEXTE), schritte)).toEqual([]);
  });
});

describe("Die Artefakte einer Nachricht", () => {
  /**
   * Nutzdaten, dann Protokolle — in dieser Reihenfolge und **ohne Umsortieren**.
   * Die Ordnung innerhalb der beiden Listen ist die des Backends (`ORDER BY
   * MessageActionID, MessagePropertyName`) und steht damit an genau einer
   * Stelle.
   *
   * **Das dritte Feld davor ist am 19.08.2026 entfallen** (M73): Die Antwort ist
   * zweigeteilt, und diese Funktion hängt zwei Listen aneinander statt drei.
   */
  it("führt sie in Anzeigereihenfolge und ordnet nichts um", () => {
    expect(artefakteintraege(LISTE).map((eintrag) => eintrag.artefaktId)).toEqual([
      "0-SAPReader.Payload.GUID",
      "2-Converter.Payload.GUID",
      "0-SAPReader.Log.GUID",
      "2-Converter.Log.GUID",
      "3-FTPSender.Log.GUID",
    ]);
  });

  it("findet ein Artefakt über seine Kennung", () => {
    expect(findeArtefakt(LISTE, "3-FTPSender.Log.GUID")?.familie).toBe("FTPSender");
    expect(findeArtefakt(LISTE, "0-SAPReader.Log.GUID")?.schritt).toBe(0);
  });

  /**
   * **`null` ist kein Fehler**, sondern der Zustand, solange die Liste lädt —
   * und der Zustand bei einer Kennung aus einer fremden URL. Die Ansicht kommt
   * ohne den Eintrag aus; ihr fehlt dann die Beschriftung, nicht der Inhalt.
   *
   * **`0-Message.Payload.GUID` steht hier stellvertretend für eine Kennung, die
   * es einen Tag lang gab.** Sie ist seit dem 19.08.2026 eine unbekannte wie
   * jede andere — kein Umleitungspfad, kein Sonderfall (M73).
   */
  it("liefert null für eine unbekannte Kennung und für eine fehlende Liste", () => {
    expect(findeArtefakt(LISTE, "9-Gibtesnicht.Log.GUID")).toBeNull();
    expect(findeArtefakt(LISTE, "0-Message.Payload.GUID")).toBeNull();
    expect(findeArtefakt(undefined, "0-SAPReader.Payload.GUID")).toBeNull();
  });
});

describe("Der Gleichlauf von Anzeige und Download", () => {
  /**
   * **Die Kernaussage von Entscheidung 9 in der Oberfläche.**
   *
   * Im Altsystem steht der Download-Knopf über einem leeren Feld und liefert die
   * vollständige Datei. Hier gibt es ihn nur dort, wo der Endpunkt auch etwas
   * liefert — alles andere wäre ein Knopf, der etwas anderes verspricht als die
   * Anzeige.
   */
  it("bietet den Download genau in den beiden lieferbaren Lagen an", () => {
    expect(downloadMoeglich(anzeige({ zustand: "ANZEIGBAR" }))).toBe(true);
    // Nutzdatei oder ADMIN: Beide bekommen die Datei ohnehin vollständig; die
    // Anzeige kann Bytes nur nicht als Text darstellen.
    expect(downloadMoeglich(anzeige({ zustand: "BINAERDATEI", beschnitten: false }))).toBe(true);
  });

  /**
   * **Das binäre Protokoll für `MANDANT` ist die Ausnahme, die Entscheidung 9
   * trägt.** Eine binäre Datei hat keinen Innenbereich zwischen Marken — es gibt
   * für diesen Aufrufer nichts, und der Endpunkt antwortet `409`. Ein Knopf
   * daneben führte genau in diese Antwort.
   */
  it("bietet ihn beim binären Protokoll für Mandanten nicht an", () => {
    expect(downloadMoeglich(anzeige({ zustand: "BINAERDATEI", beschnitten: true }))).toBe(false);
  });

  it("bietet ihn in keinem der drei inhaltslosen Zustände an", () => {
    for (const zustand of [
      "KEIN_ANZEIGBARER_PROTOKOLLTEIL",
      "DATEI_NICHT_VORHANDEN",
      "ABLAGE_NICHT_ERREICHBAR",
    ] as const) {
      expect(downloadMoeglich(anzeige({ zustand })), zustand).toBe(false);
      expect(downloadMoeglich(anzeige({ zustand, beschnitten: true })), zustand).toBe(false);
    }
  });

  /**
   * **Der beschnittene Regelfall bleibt herunterladbar** — und er liefert
   * denselben Ausschnitt, den die Anzeige zeigt. Das ist der Gleichlauf und
   * nicht seine Ausnahme.
   */
  it("bietet ihn beim beschnittenen, anzeigbaren Protokoll an", () => {
    expect(downloadMoeglich(anzeige({ zustand: "ANZEIGBAR", beschnitten: true }))).toBe(true);
  });
});

describe("Die Vermerke über der Anzeige", () => {
  it("bleiben leer, wenn der Nutzer die Datei sieht, wie sie ist", () => {
    expect(anzeigevermerke(anzeige())).toEqual([]);
  });

  /**
   * **Der Ausschnitt steht vorn**, weil er als einziger etwas über den Inhalt
   * aussagt: Was fehlt, fehlt für diesen Nutzer und fehlt auch im Download.
   */
  it("nennt Ausschnitt, Kappung und Archiveinträge in dieser Reihenfolge", () => {
    expect(
      anzeigevermerke(anzeige({ beschnitten: true, gekuerzt: true, zipEintraege: 2 })),
    ).toEqual(["AUSSCHNITT", "GEKAPPT", "MEHRERE_EINTRAEGE"]);
  });

  it("nennt den Ausschnitt genau dann, wenn der Beschnitt gegriffen hat", () => {
    expect(anzeigevermerke(anzeige({ beschnitten: true }))).toEqual(["AUSSCHNITT"]);
    expect(anzeigevermerke(anzeige({ beschnitten: false }))).not.toContain("AUSSCHNITT");
  });

  /**
   * **Ein Eintrag ist der gemessene Normalfall** — in 693 geholten Dateien
   * ausnahmslos (Teil B). Der Vermerk erscheint erst ab dem zweiten, und dann
   * erscheint er: Das Altsystem verwirft den Rest stillschweigend.
   */
  it("vermerkt mehrere Archiveinträge erst ab dem zweiten", () => {
    expect(anzeigevermerke(anzeige({ zipEintraege: 1 }))).toEqual([]);
    expect(anzeigevermerke(anzeige({ zipEintraege: 2 }))).toEqual(["MEHRERE_EINTRAEGE"]);
  });
});

describe("Datei weg gegen Ablage aus", () => {
  /**
   * **Die vier Zustandstexte sind paarweise verschieden — in beiden Sprachen.**
   *
   * Ohne diese Zusicherung wäre die Trennung aus `docs/rohdaten.md` §8 eine
   * Behauptung: Vier Zustände mit demselben Satz wären dasselbe „Fehler beim
   * Laden", nur viermal hingeschrieben. Besonders die letzten beiden dürfen
   * nicht verschmelzen — für den Betrieb ist genau ihre Unterscheidung die
   * wichtigere.
   */
  it("sagt in keinen zwei Zuständen dasselbe", () => {
    for (const sprache of ["de", "en"] as const) {
      const bausteine = texteFuer(sprache).nachrichten.detail.dateien;
      const saetze = [
        bausteine.binaerText,
        bausteine.keinProtokollteilText,
        bausteine.nichtVorhandenText,
        bausteine.ablageText,
      ];
      expect(new Set(saetze).size, sprache).toBe(4);
    }
  });

  /**
   * **Die beiden Zustände verschmelzen nicht**, und sichtbar wird der
   * Unterschied nicht über Farbe, sondern über das Angebot: Nur einer lohnt
   * einen zweiten Versuch.
   */
  it("bietet den zweiten Versuch nur bei der nicht erreichbaren Ablage an", () => {
    expect(erneutVersuchenSinnvoll("ABLAGE_NICHT_ERREICHBAR")).toBe(true);
    expect(erneutVersuchenSinnvoll("DATEI_NICHT_VORHANDEN")).toBe(false);
    expect(erneutVersuchenSinnvoll("KEIN_ANZEIGBARER_PROTOKOLLTEIL")).toBe(false);
    expect(erneutVersuchenSinnvoll("BINAERDATEI")).toBe(false);
    expect(erneutVersuchenSinnvoll("ANZEIGBAR")).toBe(false);
  });
});

describe("Die Pfade", () => {
  /**
   * **Kein Pfad trägt eine GUID, eine Ablagenkennung oder einen
   * Filestore-Verweis.** Die `artefaktId` benennt ein Artefakt *innerhalb seiner
   * Nachricht*; der Verweis wird serverseitig hergeleitet
   * (`docs/rohdaten-backend.md` §2). Nähme ein Endpunkt ihn entgegen, wäre er
   * ein offener Proxy vor einer Produktionsablage.
   */
  it("führen über das Backend und tragen keinen Verweis", () => {
    const pfad = downloadPfad("8f3a1c2e-0000-4000-8000-000000000001", "2-Converter.Payload.GUID");

    expect(pfad).toBe(
      "/api/nachrichten/8f3a1c2e-0000-4000-8000-000000000001/dateien/2-Converter.Payload.GUID/download",
    );
    expect(pfad.startsWith("/api/")).toBe(true);
    expect(pfad).not.toContain("|");
    expect(pfad).not.toContain("FILESTORE");
  });

  /**
   * **Der Inhalt ist Pfad, nicht Abfrage** (`docs/frontend-grundlagen.md` §8) —
   * und die Ansicht trägt bewusst keine Abfragezeichenkette: Ein geteilter
   * Verweis auf eine Datei handelt von der Datei.
   */
  it("legen die Ansicht in den Pfad und nicht in die Abfrage", () => {
    const ziel = artefaktAnsicht("8f3a1c2e-0000-4000-8000-000000000001", "2-Converter.Log.GUID");

    expect(ziel).toBe(
      "/nachrichten/8f3a1c2e-0000-4000-8000-000000000001/dateien/2-Converter.Log.GUID",
    );
    expect(ziel).not.toContain("?");
  });

  /** Was von außen kommt, wird kodiert — sonst entscheidet ein Schrägstrich, welche Route greift. */
  it("kodieren, was aus der URL kommt", () => {
    expect(artefaktAnsicht("a/b", "1-X.Log.GUID")).toBe("/nachrichten/a%2Fb/dateien/1-X.Log.GUID");
    expect(downloadPfad("a", "1-X/Y")).toBe("/api/nachrichten/a/dateien/1-X%2FY/download");
    expect(nachrichtAnsicht("a/b")).toBe("/nachrichten/a%2Fb");
  });
});
