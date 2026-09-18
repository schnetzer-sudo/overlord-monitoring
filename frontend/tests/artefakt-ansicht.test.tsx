// @vitest-environment jsdom

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { texteFuer } from "@/i18n";
import {
  NACHRICHTEN_SCHLUESSEL,
  type Artefaktanzeige,
  type Artefaktliste,
  type Artefaktzustand,
  type Nachrichtendetail,
} from "@/features/nachrichten/api";
import { Dateiansicht } from "@/features/nachrichten/components/artefakt-ansicht";
import { DARSTELLUNGEN, type Darstellung } from "@/features/nachrichten/darstellung";

import { neuerZwischenspeicher, rendere } from "./hilfe/rendern";

/**
 * **Die Fälle der Dateiansicht, für die ein gerenderter Baum die einzige Prüfung
 * ist** *(18.08.2026, Schritt 8 Teil Frontend)*.
 *
 * Die Regeln selbst sind reine Funktionen und stehen in `tests/rohdaten.test.ts`
 * — Beschriftung, Gleichlauf, Vermerke. Diese Datei ist die begründete Ausnahme
 * nach derselben Bedingung wie ihre Vorgänger: **Es gibt keinen anderen Ort, an
 * dem der Satz belegbar wäre.**
 *
 * | Test | Warum genau dieser |
 * |---|---|
 * | Der Inhalt ist ein Textknoten | Ob aus `<b>fett</b>` ein Element wird oder Text, entscheidet React beim Rendern und keine Funktion. Zugleich der Beleg für die Bauvorgabe aus M60: **ein** Kind, kein Element je Zeile |
 * | Die fünf Zustände | „Keiner davon ist ein leeres Feld" ist eine Aussage über **Anwesenheit** von Text und **Abwesenheit** des Inhaltsfelds. **Seit dem 17.09.2026 fünf:** das EBCDIC-Muster |
 * | Der Ausschnitt-Vermerk | dass er **fehlt**, wenn er nicht greift, ist ohne Baum nicht zu treffen |
 * | Der Download-Knopf | dieselbe Art Aussage — und die tragende von Entscheidung 9: Ein Knopf, der etwas anderes verspricht als die Anzeige, **darf nicht im Baum stehen** |
 * | Die Herkunftszeile | *(17.09.2026)* die Beschriftung je Kodierung ist eine reine Funktion (`tests/rohdaten.test.ts`); **ob sie in der Zeile steht und außerhalb von `ANZEIGBAR` fehlt**, ist eine Aussage über Anwesenheit und Abwesenheit im Baum |
 *
 * ## Kein Testdatensatz enthält echten Dateiinhalt
 *
 * Jeder Inhalt hier ist **frei erfunden** — kein echter Partner, kein echter
 * Knoten, kein echter Pfad, keine echte Kennung, keine echte Belegnummer. Das
 * gilt auch für den Binärfall: Dort steht gar kein Inhalt, weil das Backend in
 * diesem Zustand keinen liefert.
 *
 * **Antwortrümpfe gestellt, kein Netz, keine Datenbank.**
 */

const TEXTE = texteFuer("de");
const DATEIEN = TEXTE.nachrichten.detail.dateien;

const MESSAGE_ID = "8f3a1c2e-0000-4000-8000-000000000001";
const ARTEFAKT_ID = "2-Converter.Payload.GUID";

/**
 * **Ein erfundener Inhalt, der zugleich gültiges HTML ist.**
 *
 * Genau die Gestalt, gegen die die Regel gerichtet ist: Eine EDI-Datei kann
 * gültiges HTML oder SVG enthalten, und der Inhalt kommt vom Partner — er ist
 * von außen befüllbar.
 */
const INHALT_ALS_HTML =
  "<script>fensterAuf()</script>\n" +
  "<b>fett</b> <img src=x onerror=fensterAuf()>\n" +
  "<svg><a href=#>x</a></svg>\n" +
  "UNB+UNOC:3+ERFUNDEN+ERFUNDEN+250101:1200+1'";

function detail(): Nachrichtendetail {
  return {
    messageId: MESSAGE_ID,
    status: "COMMITTED",
    statusKind: "ABGESCHLOSSEN",
    processId: "erfunden",
    processName: "ERFUNDEN_PROZESS",
    projectName: "ERFUNDEN_PROJEKT",
    sosName: "Erfundener Ablauf",
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
    schritte: [
      {
        position: 2,
        name: "Datei konvertiert",
        namensherkunft: "DIREKT",
        rohwert: "ERFUNDEN_KONVERT|A2B",
        start: "2025-12-29T22:41:12Z",
        ende: "2025-12-29T22:41:12Z",
        dauerSekunden: 0,
        timeoutSekunden: 1800,
        laeuftAuf: false,
      },
    ],
    kuratierteEigenschaften: [],
  };
}

function liste(): Artefaktliste {
  return {
    messageId: MESSAGE_ID,
    nutzdaten: [
      // Das Paar des Lesedienstes auf Schritt 0 — seit dem 19.08.2026 alles, was
      // dort liegt. `Message.Payload.GUID` stand bis dahin in einem eigenen Feld
      // `eingang` und ist mit M73 entfallen.
      {
        artefaktId: "0-FileReader.Payload.GUID",
        name: "FileReader.Payload.GUID",
        familie: "FileReader",
        art: "NUTZDATEN",
        schritt: 0,
        beschnittMoeglich: false,
      },
      {
        artefaktId: ARTEFAKT_ID,
        name: "Converter.Payload.GUID",
        familie: "Converter",
        art: "NUTZDATEN",
        schritt: 2,
        beschnittMoeglich: false,
      },
    ],
    protokolle: [
      {
        artefaktId: "2-Converter.Log.GUID",
        name: "Converter.Log.GUID",
        familie: "Converter",
        art: "PROTOKOLL",
        schritt: 2,
        beschnittMoeglich: true,
      },
    ],
  };
}

function anzeige(teile: Partial<Artefaktanzeige> = {}): Artefaktanzeige {
  return {
    artefaktId: ARTEFAKT_ID,
    name: "Converter.Payload.GUID",
    art: "NUTZDATEN",
    zustand: "ANZEIGBAR",
    text: "UNB+UNOC:3+ERFUNDEN+ERFUNDEN+250101:1200+1'",
    groesseBytes: 43,
    gekuerzt: false,
    beschnitten: false,
    // Bis zum 17.09.2026 stand hier die feste Zeichenkette "ISO-8859-1". Der
    // erfundene Inhalt darüber ist reines ASCII, und genau das stellt das
    // Backend seither fest.
    kodierung: "ASCII",
    zipEintraege: 1,
    ...teile,
  };
}

/** Ein `fetch`, das jeden Aufruf sichtbar macht — und keinen durchlässt. */
let anfragen: string[] = [];

beforeEach(() => {
  anfragen = [];
  vi.stubGlobal(
    "fetch",
    vi.fn((eingabe: RequestInfo | URL) => {
      anfragen.push(String(eingabe));
      return Promise.reject(new Error("In diesem Test darf keine Anfrage hinausgehen"));
    }),
  );
});

afterEach(() => {
  vi.unstubAllGlobals();
});

/**
 * Rendert die Ansicht mit gestellten Antwortrümpfen für alle drei Abfragen.
 *
 * Gerendert wird `Dateiansicht`, die Ansicht **unter** der Adresshülle: Sie
 * bekommt Darstellung und Setter als Props, und der Test kommt ohne `nuqs`
 * aus (`docs/dateiansicht-darstellung.md` §4). Ohne Angabe steht das
 * Original — wie beim Öffnen.
 */
async function rendereAnsicht(
  gestellt: Artefaktanzeige,
  artefaktId = ARTEFAKT_ID,
  darstellung: Darstellung = "original",
) {
  const zwischenspeicher = neuerZwischenspeicher();
  zwischenspeicher.setQueryData(NACHRICHTEN_SCHLUESSEL.detail(MESSAGE_ID), detail());
  zwischenspeicher.setQueryData(NACHRICHTEN_SCHLUESSEL.dateien(MESSAGE_ID), liste());
  zwischenspeicher.setQueryData(
    NACHRICHTEN_SCHLUESSEL.dateiInhalt(MESSAGE_ID, artefaktId),
    gestellt,
  );

  return rendere(
    <Dateiansicht
      messageId={MESSAGE_ID}
      artefaktId={artefaktId}
      darstellung={darstellung}
      aufDarstellung={() => undefined}
    />,
    zwischenspeicher,
  );
}

describe("Der Dateiinhalt", () => {
  /**
   * **Die eine Regel, die nicht verhandelbar ist: als Textknoten, niemals als
   * HTML.**
   *
   * Eine EDI-Datei kann gültiges HTML oder SVG enthalten, und der Inhalt kommt
   * vom Partner — er ist von außen befüllbar. Der Test stellt genau so einen
   * Inhalt und weist nach, dass daraus **kein einziges Element** entsteht.
   *
   * **Derselbe Test belegt die Bauvorgabe aus M60**: Das `<pre>` hat genau *ein*
   * Kind, und das ist ein Textknoten. Ein Element je Zeile erzeugte bei einer
   * 600-KB-Datei Zehntausende Knoten — und wäre hier sofort sichtbar, weil aus
   * einem Kind vier würden.
   */
  it("landet als Textknoten im Baum und nicht als Markup", async () => {
    const { behaelter, abbauen } = await rendereAnsicht(anzeige({ text: INHALT_ALS_HTML }));

    try {
      const feld = behaelter.querySelector("[data-inhalt]");
      expect(feld).not.toBeNull();

      // Kein Element aus dem Inhalt — weder ausführbar noch ladend noch
      // zeichnend. Die Prüfung läuft **im Inhaltsfeld**: Die Ansicht selbst
      // trägt SVG-Zeichen aus der Symbolbibliothek, und die sind unser Markup
      // und nicht das des Partners.
      expect(feld?.querySelectorAll("*")).toHaveLength(0);
      expect(feld?.querySelector("svg")).toBeNull();
      // Im ganzen Baum gibt es dagegen weder ein Skript noch ein ladendes Bild —
      // die Ansicht selbst bringt keines mit, also kann nur der Inhalt eines
      // erzeugt haben.
      expect(behaelter.querySelector("script")).toBeNull();
      expect(behaelter.querySelector("img")).toBeNull();

      // Ein Kind, und es ist Text. Kein Element je Zeile, obwohl der Inhalt vier
      // Zeilen hat.
      expect(feld?.childNodes).toHaveLength(1);
      expect(feld?.childNodes[0]?.nodeType).toBe(3);

      // Und zwar Zeichen für Zeichen der gelieferte Inhalt.
      expect(feld?.textContent).toBe(INHALT_ALS_HTML);
    } finally {
      await abbauen();
    }
  });
});

/**
 * **Die fünf Zustände aus `docs/rohdaten.md` §8 — je einer.**
 *
 * Geprüft wird für jeden dasselbe: Er trägt seinen **eigenen** Text, er ist
 * **kein leeres Feld**, und er zeigt **kein Inhaltsfeld**. Das ist der
 * Unterschied zum Altsystem, das bei allen dieselbe leere Fläche zeigt.
 * **Seit dem 17.09.2026 fünf:** Das EBCDIC-Muster bekommt dieselbe ruhige
 * Gestalt wie die vier anderen — und keinen zweiten Versuch.
 */
const ZUSTAENDE: readonly {
  zustand: Artefaktzustand;
  gestellt: Partial<Artefaktanzeige>;
  titel: string;
  mitZweitemVersuch: boolean;
}[] = [
  {
    zustand: "BINAERDATEI",
    // Kein erfundener Binärinhalt nötig: In diesem Zustand liefert das Backend
    // gar keinen Text, nur die Größe.
    gestellt: { zustand: "BINAERDATEI", text: "", groesseBytes: 4711 },
    titel: DATEIEN.binaerTitel,
    mitZweitemVersuch: false,
  },
  {
    zustand: "EBCDIC_DATEI",
    // Wie beim Binärfall: kein Inhalt, nur die Größe — und keine Kodierung.
    gestellt: { zustand: "EBCDIC_DATEI", text: "", groesseBytes: 2048, kodierung: null },
    titel: DATEIEN.ebcdicTitel,
    mitZweitemVersuch: false,
  },
  {
    zustand: "KEIN_ANZEIGBARER_PROTOKOLLTEIL",
    gestellt: {
      zustand: "KEIN_ANZEIGBARER_PROTOKOLLTEIL",
      art: "PROTOKOLL",
      text: "",
      groesseBytes: 0,
      beschnitten: true,
    },
    titel: DATEIEN.keinProtokollteilTitel,
    mitZweitemVersuch: false,
  },
  {
    zustand: "DATEI_NICHT_VORHANDEN",
    gestellt: { zustand: "DATEI_NICHT_VORHANDEN", text: "", groesseBytes: 0 },
    titel: DATEIEN.nichtVorhandenTitel,
    mitZweitemVersuch: false,
  },
  {
    zustand: "ABLAGE_NICHT_ERREICHBAR",
    gestellt: { zustand: "ABLAGE_NICHT_ERREICHBAR", text: "", groesseBytes: 0 },
    titel: DATEIEN.ablageTitel,
    mitZweitemVersuch: true,
  },
];

describe("Die fünf benannten Zustände", () => {
  it.each(ZUSTAENDE)(
    "$zustand trägt einen eigenen Text und kein leeres Feld",
    async ({ zustand, gestellt, titel, mitZweitemVersuch }) => {
      const { behaelter, abbauen } = await rendereAnsicht(anzeige(gestellt));

      try {
        const feld = behaelter.querySelector(`[data-zustand="${zustand}"]`);
        expect(feld).not.toBeNull();
        expect(feld?.textContent).toContain(titel);
        // Kein leeres Feld: Neben der Überschrift steht ein erklärender Satz.
        expect((feld?.textContent ?? "").length).toBeGreaterThan(titel.length + 40);

        // Und kein Inhaltsfeld — es gibt nichts anzuzeigen.
        expect(behaelter.querySelector("[data-inhalt]")).toBeNull();

        // Nur „Ablage nicht erreichbar" lohnt einen zweiten Versuch. Der
        // Unterschied zu „Datei nicht vorhanden" wird über das Angebot getragen
        // und nicht über Farbe.
        expect(feld?.querySelector("button") !== null).toBe(mitZweitemVersuch);
      } finally {
        await abbauen();
      }
    },
  );
});

describe("Der Ausschnitt-Vermerk", () => {
  /**
   * **Er erscheint, wenn der Beschnitt gegriffen hat, und fehlt, wenn nicht.**
   *
   * Der Nutzer soll wissen, dass er einen Ausschnitt sieht — und ebenso, dass er
   * es *nicht* tut. Ein Vermerk, der immer dasteht, sagt nichts.
   */
  it("steht über der Anzeige, wenn beschnitten wurde", async () => {
    const { behaelter, abbauen } = await rendereAnsicht(
      anzeige({ art: "PROTOKOLL", beschnitten: true, text: "ERFUNDEN: Schritt begonnen\n" }),
    );

    try {
      expect(behaelter.querySelector('[data-vermerk="AUSSCHNITT"]')?.textContent).toContain(
        DATEIEN.vermerkAusschnitt,
      );
      // Der Inhalt steht trotzdem da: Der Ausschnitt ist der Regelfall des
      // beschnittenen Protokolls und kein Ausfall.
      expect(behaelter.querySelector("[data-inhalt]")).not.toBeNull();
    } finally {
      await abbauen();
    }
  });

  it("fehlt, wenn nicht beschnitten wurde", async () => {
    const { behaelter, abbauen } = await rendereAnsicht(anzeige({ beschnitten: false }));

    try {
      expect(behaelter.querySelector('[data-vermerk="AUSSCHNITT"]')).toBeNull();
      expect(behaelter.textContent).not.toContain(DATEIEN.vermerkAusschnitt);
    } finally {
      await abbauen();
    }
  });
});

describe("Der Download-Knopf", () => {
  /**
   * **Entscheidung 9 im Baum.** Im Altsystem steht der Knopf über einem leeren
   * Feld und liefert die vollständige Datei. Hier steht er nur dort, wo der
   * Endpunkt etwas liefert — und dass er in den anderen Lagen **nicht im Baum
   * ist**, lässt sich nur an einem Baum zeigen.
   */
  it("steht dort, wo die Anzeige etwas hergibt, und nirgends sonst", async () => {
    const mit = await rendereAnsicht(anzeige());
    try {
      const verweis = mit.behaelter.querySelector('a[href*="/download"]');
      expect(verweis).not.toBeNull();
      expect(verweis?.getAttribute("href")).toBe(
        `/api/nachrichten/${MESSAGE_ID}/dateien/${ARTEFAKT_ID}/download`,
      );
      // Ein gewöhnlicher Verweis auf das Backend — keine Blob-URL, kein
      // `download`-Attribut, das dem Backend den Dateinamen streitig machte.
      expect(verweis?.getAttribute("download")).toBeNull();
    } finally {
      await mit.abbauen();
    }

    for (const zustand of [
      "KEIN_ANZEIGBARER_PROTOKOLLTEIL",
      "DATEI_NICHT_VORHANDEN",
      "ABLAGE_NICHT_ERREICHBAR",
    ] as const) {
      const ohne = await rendereAnsicht(anzeige({ zustand, text: "", groesseBytes: 0 }));
      try {
        expect(ohne.behaelter.querySelector('a[href*="/download"]'), zustand).toBeNull();
      } finally {
        await ohne.abbauen();
      }
    }

    // Das binäre Protokoll für `MANDANT`: Der Endpunkt antwortet `409`, und
    // deshalb steht hier kein Knopf. Die binäre **Nutzdatei** bleibt dagegen
    // herunterladbar — dort gibt es keinen Beschnitt, an dem etwas
    // vorbeiführen könnte.
    const binaeresProtokoll = await rendereAnsicht(
      anzeige({ zustand: "BINAERDATEI", art: "PROTOKOLL", text: "", beschnitten: true }),
    );
    try {
      expect(binaeresProtokoll.behaelter.querySelector('a[href*="/download"]')).toBeNull();
    } finally {
      await binaeresProtokoll.abbauen();
    }

    const binaereNutzdatei = await rendereAnsicht(
      anzeige({ zustand: "BINAERDATEI", text: "", groesseBytes: 4711 }),
    );
    try {
      expect(binaereNutzdatei.behaelter.querySelector('a[href*="/download"]')).not.toBeNull();
    } finally {
      await binaereNutzdatei.abbauen();
    }

    // Dasselbe Paar für das EBCDIC-Muster (17.09.2026): Die Nutzdatei bleibt
    // herunterladbar, das Protokoll für `MANDANT` nicht — der Endpunkt
    // antwortet dort `409`, und die Bytes verlassen das Backend nicht.
    const ebcdicProtokoll = await rendereAnsicht(
      anzeige({ zustand: "EBCDIC_DATEI", art: "PROTOKOLL", text: "", beschnitten: true }),
    );
    try {
      expect(ebcdicProtokoll.behaelter.querySelector('a[href*="/download"]')).toBeNull();
    } finally {
      await ebcdicProtokoll.abbauen();
    }

    const ebcdicNutzdatei = await rendereAnsicht(
      anzeige({ zustand: "EBCDIC_DATEI", text: "", groesseBytes: 2048, kodierung: null }),
    );
    try {
      expect(ebcdicNutzdatei.behaelter.querySelector('a[href*="/download"]')).not.toBeNull();
    } finally {
      await ebcdicNutzdatei.abbauen();
    }
  });
});

describe("Die Herkunftszeile", () => {
  /**
   * **Der Wert aus der Antwort, nicht eine feste Zeichenkette** (17.09.2026).
   * Bis dahin stand dort für jede Datei „Kodierung ISO-8859-1"; seither stellt
   * das Backend die Kodierung je Datei fest, und die Zeile sagt nur, was an den
   * Bytes feststeht — für den Rückfall „gelesen als" statt „Kodierung", und für
   * einen Wert, den diese Oberfläche nicht kennt, den rohen Schlüssel.
   */
  it.each([
    { kodierung: "ASCII", angabe: DATEIEN.kodierung.ASCII },
    { kodierung: "UTF_8", angabe: DATEIEN.kodierung.UTF_8 },
    { kodierung: "ISO_8859_1", angabe: DATEIEN.kodierung.ISO_8859_1 },
    { kodierung: "X_UNBEKANNT", angabe: "X_UNBEKANNT" },
  ])("nennt bei $kodierung „$angabe“", async ({ kodierung, angabe }) => {
    const { behaelter, abbauen } = await rendereAnsicht(anzeige({ kodierung }));

    try {
      const zeile = behaelter.querySelector("ul[data-ziffern]");
      expect(zeile).not.toBeNull();
      const angaben = [...(zeile?.querySelectorAll("li") ?? [])].map((li) => li.textContent);
      expect(angaben).toContain(angabe);
      // Genau eine Angabe zur Kodierung — nicht die alte feste daneben.
      expect(angaben.filter((a) => a === angabe)).toHaveLength(1);
      // Und nirgends die alte feste Zeichenkette: Auch der Rueckfall heisst nicht "Kodierung ISO".
      expect(zeile?.textContent).not.toContain("Kodierung ISO");
    } finally {
      await abbauen();
    }
  });

  /**
   * **Außerhalb von `ANZEIGBAR` steht kein Kodierungswert** — auch dann nicht,
   * wenn die Antwort einen trüge. Die Kodierung beschreibt, wie *dieser* Text
   * entstanden ist; ohne Text gibt es nichts zu beschreiben. Gestellt wird
   * deshalb bewusst ein Wert, der laut Vertrag `null` sein müsste.
   */
  it("zeigt außerhalb von ANZEIGBAR keinen Kodierungswert", async () => {
    for (const zustand of [
      "BINAERDATEI",
      "EBCDIC_DATEI",
      "KEIN_ANZEIGBARER_PROTOKOLLTEIL",
      "DATEI_NICHT_VORHANDEN",
      "ABLAGE_NICHT_ERREICHBAR",
    ] as const) {
      const { behaelter, abbauen } = await rendereAnsicht(
        anzeige({ zustand, text: "", groesseBytes: 4711, kodierung: "UTF_8" }),
      );
      try {
        const zeile = behaelter.querySelector("ul[data-ziffern]");
        expect(zeile, zustand).not.toBeNull();
        expect(zeile?.textContent, zustand).not.toContain(DATEIEN.kodierung.UTF_8);
        expect(zeile?.textContent, zustand).not.toContain("UTF_8");
      } finally {
        await abbauen();
      }
    }

    // Und bei `null`, dem Vertragsfall, erst recht nicht.
    const { behaelter, abbauen } = await rendereAnsicht(
      anzeige({ zustand: "BINAERDATEI", text: "", groesseBytes: 4711, kodierung: null }),
    );
    try {
      const angaben = [...(behaelter.querySelectorAll("ul[data-ziffern] li") ?? [])].map(
        (li) => li.textContent,
      );
      expect(angaben).toEqual(["Converter.Payload.GUID", DATEIEN.art.NUTZDATEN, "4.711 Bytes"]);
    } finally {
      await abbauen();
    }
  });
});

describe("Die Ansicht insgesamt", () => {
  /**
   * Die Beschriftung kommt aus **derselben** Funktion wie die Zeile, aus der der
   * Nutzer kam — und stellt keine zweite Anfrage: Detail und Liste liegen im
   * Zwischenspeicher.
   */
  it("beschriftet mit dem Schrittnamen und holt dafür nichts nach", async () => {
    const { behaelter, abbauen } = await rendereAnsicht(anzeige());

    try {
      expect(behaelter.querySelector("h1")?.textContent).toBe("Datei konvertiert");
      expect(anfragen).toEqual([]);
    } finally {
      await abbauen();
    }
  });
});

/**
 * **Die Darstellungswahl** *(17.09.2026, `docs/dateiansicht-darstellung.md` §7,
 * Teil B)* — nur die Sätze, die keine reine Funktion trägt. Die Regeln je
 * Darstellung stehen in `tests/darstellung.test.ts`.
 *
 * | Test | Warum genau dieser |
 * |---|---|
 * | Ein Textknoten in jeder Darstellung | ob aus dem formatierten Text ein Element wird, entscheidet React beim Rendern — in allen acht Lagen |
 * | Wohlgeformtes XML mit `<script>` und `<svg onload>` | der einzige Fall, in dem ein Inhalt mit Markup **tatsächlich formatiert** wird — und trotzdem Text bleibt |
 * | Keine Auswahl bei Protokoll und in den fünf Zuständen | eine Aussage über **Abwesenheit** im Baum |
 * | Der Download-Vermerk | Anwesenheit bei einer Darstellung, Abwesenheit beim Original |
 */
const EDIFACT_ERFUNDEN =
  "UNB+UNOC:3+ERFUNDEN+ERFUNDEN+250101:1200+1'UNH+1+ORDERS:D:96A:UN'UNT+2+1'UNZ+1+1'";

/** Wohlgeformt, und genau die Gestalt, gegen die die Textknoten-Regel gerichtet ist. */
const XML_MIT_SKRIPT =
  '<?xml version="1.0"?><beleg><script>fensterAuf()</script>' +
  '<svg onload="fensterAuf()"/><img src="x" onerror="fensterAuf()"/></beleg>';

describe("Die Darstellungswahl", () => {
  it("landet in jeder der acht Darstellungen als ein Textknoten und nie als Markup", async () => {
    for (const darstellung of DARSTELLUNGEN) {
      const { behaelter, abbauen } = await rendereAnsicht(
        anzeige({ text: INHALT_ALS_HTML }),
        ARTEFAKT_ID,
        darstellung,
      );
      try {
        const feld = behaelter.querySelector("[data-inhalt]");
        expect(feld, darstellung).not.toBeNull();
        expect(feld?.querySelectorAll("*"), darstellung).toHaveLength(0);
        expect(behaelter.querySelector("script"), darstellung).toBeNull();
        expect(behaelter.querySelector("img"), darstellung).toBeNull();
        expect(feld?.childNodes, darstellung).toHaveLength(1);
        expect(feld?.childNodes[0]?.nodeType, darstellung).toBe(3);

        // Der erfundene Inhalt ist keines der sechs Formate: Dort steht das
        // Original mit dem Vermerk. Original und Hex tragen ihn nicht.
        const passtNicht = behaelter.querySelector('[data-vermerk="PASST_NICHT"]');
        if (darstellung === "original" || darstellung === "hex") {
          expect(passtNicht, darstellung).toBeNull();
        } else {
          expect(passtNicht, darstellung).not.toBeNull();
          expect(passtNicht?.textContent, darstellung).toContain(
            DATEIEN.darstellung.eintraege[darstellung],
          );
          expect(feld?.textContent, darstellung).toBe(INHALT_ALS_HTML);
        }
        if (darstellung === "hex") {
          expect(
            feld?.textContent?.startsWith("00000000  3c 73 63 72 69 70 74 3e"),
            darstellung,
          ).toBe(true);
        }
      } finally {
        await abbauen();
      }
    }
  });

  it("formatiert ein wohlgeformtes XML mit <script> und <svg onload> als XML — und als Text", async () => {
    const { behaelter, abbauen } = await rendereAnsicht(
      anzeige({ text: XML_MIT_SKRIPT }),
      ARTEFAKT_ID,
      "xml",
    );
    try {
      const feld = behaelter.querySelector("[data-inhalt]");
      expect(feld?.querySelectorAll("*")).toHaveLength(0);
      expect(behaelter.querySelector("script")).toBeNull();
      expect(behaelter.querySelector("img")).toBeNull();
      expect(feld?.childNodes).toHaveLength(1);
      expect(feld?.childNodes[0]?.nodeType).toBe(3);
      // Tatsächlich als XML formatiert: eingerückt, ein Token je Zeile.
      expect(feld?.textContent).toBe(
        '<?xml version="1.0"?>\n<beleg>\n  <script>fensterAuf()</script>\n' +
          '  <svg onload="fensterAuf()"/>\n  <img src="x" onerror="fensterAuf()"/>\n</beleg>',
      );
      expect(behaelter.querySelector('[data-vermerk="PASST_NICHT"]')).toBeNull();
    } finally {
      await abbauen();
    }
  });

  it("steht nur bei Nutzdaten im Zustand ANZEIGBAR — nicht beim Protokoll, nicht in den fünf Zuständen", async () => {
    const mit = await rendereAnsicht(anzeige({ text: EDIFACT_ERFUNDEN }));
    try {
      expect(mit.behaelter.querySelector("[data-darstellung-auswahl]")).not.toBeNull();
      expect(mit.behaelter.textContent).toContain(DATEIEN.darstellung.beschriftung);
    } finally {
      await mit.abbauen();
    }

    // Das Protokoll: keine Auswahl — und der Parameter ist wirkungslos, auch
    // wenn er in der Adresse steht. Der Text bleibt, wie er ist, ohne Vermerk.
    const protokoll = await rendereAnsicht(
      anzeige({ art: "PROTOKOLL", text: EDIFACT_ERFUNDEN }),
      ARTEFAKT_ID,
      "edifact",
    );
    try {
      expect(protokoll.behaelter.querySelector("[data-darstellung-auswahl]")).toBeNull();
      expect(protokoll.behaelter.querySelector("[data-inhalt]")?.textContent).toBe(
        EDIFACT_ERFUNDEN,
      );
      expect(protokoll.behaelter.querySelector("[data-vermerk]")).toBeNull();
    } finally {
      await protokoll.abbauen();
    }

    for (const { gestellt, zustand } of ZUSTAENDE) {
      const ohne = await rendereAnsicht(anzeige(gestellt), ARTEFAKT_ID, "edifact");
      try {
        expect(ohne.behaelter.querySelector("[data-darstellung-auswahl]"), zustand).toBeNull();
        // Die Vermerke der Anzeige (etwa der Ausschnitt) stehen weiter; die
        // drei der Darstellung nicht.
        for (const vermerk of ["PASST_NICHT", "HEX_GEKAPPT", "DOWNLOAD_ORIGINAL"]) {
          expect(ohne.behaelter.querySelector(`[data-vermerk="${vermerk}"]`), zustand).toBeNull();
        }
      } finally {
        await ohne.abbauen();
      }
    }
  });

  it("nennt den Download bei einer Darstellung und nicht beim Original", async () => {
    const formatiert = await rendereAnsicht(
      anzeige({ text: EDIFACT_ERFUNDEN }),
      ARTEFAKT_ID,
      "edifact",
    );
    try {
      const vermerk = formatiert.behaelter.querySelector('[data-vermerk="DOWNLOAD_ORIGINAL"]');
      expect(vermerk?.textContent).toContain(DATEIEN.darstellung.vermerkDownloadOriginal);
      // Der Inhalt ist formatiert — und der Download-Verweis derselbe wie zuvor:
      // kein zweiter Knopf, kein Parameter, das Original.
      expect(formatiert.behaelter.querySelector("[data-inhalt]")?.textContent).toBe(
        EDIFACT_ERFUNDEN.split("'").slice(0, -1).join("'\n") + "'",
      );
      expect(formatiert.behaelter.querySelector('a[href*="/download"]')?.getAttribute("href")).toBe(
        `/api/nachrichten/${MESSAGE_ID}/dateien/${ARTEFAKT_ID}/download`,
      );
      expect(formatiert.behaelter.querySelectorAll('a[href*="/download"]')).toHaveLength(1);
    } finally {
      await formatiert.abbauen();
    }

    const original = await rendereAnsicht(anzeige({ text: EDIFACT_ERFUNDEN }));
    try {
      expect(original.behaelter.querySelector('[data-vermerk="DOWNLOAD_ORIGINAL"]')).toBeNull();
      expect(original.behaelter.textContent).not.toContain(
        DATEIEN.darstellung.vermerkDownloadOriginal,
      );
    } finally {
      await original.abbauen();
    }
  });
});
