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
import { ArtefaktAnsicht } from "@/features/nachrichten/components/artefakt-ansicht";

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
 * | Die vier Zustände | „Keiner davon ist ein leeres Feld" ist eine Aussage über **Anwesenheit** von Text und **Abwesenheit** des Inhaltsfelds |
 * | Der Ausschnitt-Vermerk | dass er **fehlt**, wenn er nicht greift, ist ohne Baum nicht zu treffen |
 * | Der Download-Knopf | dieselbe Art Aussage — und die tragende von Entscheidung 9: Ein Knopf, der etwas anderes verspricht als die Anzeige, **darf nicht im Baum stehen** |
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
    ueberfaellig: false,
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
    eingang: {
      artefaktId: "0-Message.Payload.GUID",
      name: "Message.Payload.GUID",
      familie: "Message",
      art: "NUTZDATEN",
      schritt: 0,
      beschnittMoeglich: false,
    },
    nutzdaten: [
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
    kodierung: "ISO-8859-1",
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

/** Rendert die Ansicht mit gestellten Antwortrümpfen für alle drei Abfragen. */
async function rendereAnsicht(gestellt: Artefaktanzeige, artefaktId = ARTEFAKT_ID) {
  const zwischenspeicher = neuerZwischenspeicher();
  zwischenspeicher.setQueryData(NACHRICHTEN_SCHLUESSEL.detail(MESSAGE_ID), detail());
  zwischenspeicher.setQueryData(NACHRICHTEN_SCHLUESSEL.dateien(MESSAGE_ID), liste());
  zwischenspeicher.setQueryData(
    NACHRICHTEN_SCHLUESSEL.dateiInhalt(MESSAGE_ID, artefaktId),
    gestellt,
  );

  return rendere(
    <ArtefaktAnsicht messageId={MESSAGE_ID} artefaktId={artefaktId} />,
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
 * **Die vier Zustände aus `docs/rohdaten.md` §8 — je einer.**
 *
 * Geprüft wird für jeden dasselbe: Er trägt seinen **eigenen** Text, er ist
 * **kein leeres Feld**, und er zeigt **kein Inhaltsfeld**. Das ist der
 * Unterschied zum Altsystem, das bei allen vier dieselbe leere Fläche zeigt.
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

describe("Die vier benannten Zustände", () => {
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
