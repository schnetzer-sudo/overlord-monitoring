// @vitest-environment jsdom

import { act } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { texteFuer } from "@/i18n";
import {
  NACHRICHTEN_SCHLUESSEL,
  type Artefakt,
  type Artefaktliste,
  type Eigenschaft,
  type Nachrichtendetail,
  type Schritt,
} from "@/features/nachrichten/api";
import { NachrichtDetail } from "@/features/nachrichten/components/nachricht-detail";

import { neuerZwischenspeicher, rendere } from "./hilfe/rendern";

/**
 * **Die Artefakte hängen an der Zeitleiste** — Nachbesserung vom 18.08.2026.
 *
 * Der eigene Block „Dateien und Protokolle" ist entfallen; er stand mit neun
 * Zeilen und vier sich wiederholenden Schrittnamen unter derselben Schrittfolge,
 * die drei Zeilen darüber schon mit Dauer und Balken stand. Was an seine Stelle
 * getreten ist, lässt sich nur im Baum belegen — es ist eine Aussage darüber,
 * **wo** ein Verweis hängt und **dass** keiner verschwunden ist:
 *
 * | Test | Warum genau dieser |
 * |---|---|
 * | Drei Lagen je Schritt | Beide Arten, nur eine, keine — welche Zeile welches Ziel trägt, entscheidet das Markup und keine Funktion |
 * | Der Eingang | **Die Zeitleiste führt Schritt `0` nicht** (`docs/nachrichtendetail.md` §4), die Artefakte liegen aber dort. Dass sie trotzdem erreichbar sind, ist eine Aussage über Anwesenheit an einer Stelle, an der es keine Zeile gibt |
 * | Fünfzehn Artefakte | Die Belastungsprobe aus M55 — fünfzehn eigene Ziele, **kein doppelter React-Schlüssel** |
 * | Das Aufklappen *(21.09.2026)* | Ein Klick auf die Zeile schaltet **genau einmal**, ein Klick auf ein Ziel **nicht** — das Ziel ist kein Nachfahre der Schaltfläche. `aria-expanded`, `aria-controls` und `inert` sind Zustand des Dokuments |
 * | Schritt ohne Eigenschaften | **kein `button`** — eine Aussage über Abwesenheit |
 * | Bewegung mit Ausschalter | die Regel **ist** eine Klasse, wie beim Ansichtsumschalter |
 * | Eingang und Rest | die Zeile gibt es mit Eigenschaften ohne Ziele und umgekehrt; der Rest mit Eigenschaften ist von Hand nie zu sehen (M57, Befund 1) |
 *
 * **Die vier Sprungfälle vom 18.08.2026 sind am 21.09.2026 entfallen** — samt der
 * Mechanik, die sie belegten (E‑225, `docs/nachrichtendetail.md` §10.16).
 *
 * **Kein Testdatensatz enthält echten Dateiinhalt**, keinen echten Partner,
 * keinen echten Knoten, keine echte Kennung. Alles hier ist erfunden.
 */

const TEXTE = texteFuer("de");
const DATEIEN = TEXTE.nachrichten.detail.dateien;

const MESSAGE_ID = "8f3a1c2e-0000-4000-8000-000000000003";

function artefakt(
  schritt: number,
  familie: string,
  art: Artefakt["art"],
  beschnittMoeglich = false,
): Artefakt {
  const name = `${familie}.${art === "NUTZDATEN" ? "Payload" : "Log"}.GUID`;
  return { artefaktId: `${schritt}-${name}`, name, familie, art, schritt, beschnittMoeglich };
}

function schritt(position: number, name: string): Schritt {
  return {
    position,
    name,
    namensherkunft: "DIREKT",
    rohwert: "ERFUNDEN_BAUSTEIN",
    start: "2025-12-29T22:41:12Z",
    ende: "2025-12-29T22:41:13Z",
    dauerSekunden: 1,
    timeoutSekunden: 1800,
    laeuftAuf: false,
  };
}

function detail(werte: Partial<Nachrichtendetail> = {}): Nachrichtendetail {
  return {
    messageId: MESSAGE_ID,
    status: "FINISHED",
    statusKind: "ABGESCHLOSSEN",
    processId: "P-0815",
    processName: "Erfundener Prozess",
    projectName: "Erfundenes Projekt",
    sosName: "Erfundener Ablauf",
    rollen: [],
    zeitpunkt: "2025-12-29T22:41:20Z",
    start: "2025-12-29T22:41:10Z",
    gesamtdauerSekunden: 10,
    fristSekunden: null,
    eigenschaftenAnzahl: 0,
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
 * **Die gemessene Gestalt einer Nachricht: die Zeitleiste führt drei Schritte,
 * die Artefakte liegen auf vier Positionen.**
 *
 * Auf Schritt `0` sitzt das Paar des Lesedienstes (M57, Fenster A) — er kommt in
 * `schritte[]` nicht vor, weil das Backend den Metadaten-Schritt dort ausnimmt
 * (`docs/nachrichtendetail.md` §4).
 *
 * **`Message.Payload.GUID` stand bis zum 19.08.2026 daneben** und galt als die
 * eingegangene Datei. Nach M73 zeigt der Name auf die Nutzdatenzeile mit dem
 * höchsten `MessageActionID` derselben Nachricht; das Backend führt ihn nicht
 * mehr.
 */
const SCHRITTE = [
  schritt(1, "Datei konvertiert"),
  schritt(2, "Datei versendet"),
  schritt(3, "Bestätigung verarbeitet"),
];

const LISTE: Artefaktliste = {
  messageId: MESSAGE_ID,
  nutzdaten: [artefakt(0, "SAPReader", "NUTZDATEN"), artefakt(1, "Converter", "NUTZDATEN")],
  protokolle: [
    artefakt(0, "SAPReader", "PROTOKOLL", true),
    artefakt(1, "Converter", "PROTOKOLL", true),
    // Schritt 2 trägt nur ein Protokoll und keine Nutzdatei; Schritt 3 gar
    // nichts. Beides kommt vor — und beides darf keine leere Stelle erzeugen.
    artefakt(2, "FTPSender", "PROTOKOLL", true),
  ],
};

const EIGENSCHAFTEN: Eigenschaft[] = [
  {
    name: "Message.SourceMessageID",
    wert: "ERFUNDEN-1",
    position: 0,
    gekappt: false,
    originalLaengeBytes: null,
  },
  {
    name: "Converter.Type",
    wert: "ERFUNDEN",
    position: 1,
    gekappt: false,
    originalLaengeBytes: null,
  },
  {
    name: "Service.Type",
    wert: "ERFUNDEN",
    position: 2,
    gekappt: false,
    originalLaengeBytes: null,
  },
];

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

async function rendereDetail(
  werte: Partial<Nachrichtendetail>,
  liste: Artefaktliste,
  eigenschaften: Eigenschaft[] = [],
) {
  const zwischenspeicher = neuerZwischenspeicher();
  zwischenspeicher.setQueryData(NACHRICHTEN_SCHLUESSEL.detail(MESSAGE_ID), detail(werte));
  zwischenspeicher.setQueryData(NACHRICHTEN_SCHLUESSEL.dateien(MESSAGE_ID), liste);
  zwischenspeicher.setQueryData(NACHRICHTEN_SCHLUESSEL.eigenschaften(MESSAGE_ID), eigenschaften);

  return rendere(
    <NachrichtDetail
      messageId={MESSAGE_ID}
      aufSchliessen={() => undefined}
      schliessenText="Schließen"
      aufOeffnen={() => undefined}
    />,
    zwischenspeicher,
  );
}

async function klicke(element: Element | null | undefined): Promise<void> {
  expect(element).toBeTruthy();
  await act(async () => {
    element?.dispatchEvent(new MouseEvent("click", { bubbles: true }));
  });
}

/** Die gestrichelte Zeile über der Leiste — der ganze Eintrag, samt Zielen und Inhalt. */
function eingangszeile(behaelter: HTMLElement): Element | undefined {
  return [...behaelter.querySelectorAll("div.border-dashed")].find((kasten) =>
    (kasten.textContent ?? "").startsWith(DATEIEN.eingang),
  );
}

/** Die Zeilen der Zeitleiste samt der Ziele, die an ihnen hängen. */
function zeilen(behaelter: HTMLElement): { name: string; ziele: string[] }[] {
  return [...behaelter.querySelectorAll("ol > li")].map((zeile) => ({
    // Der Name ist das erste `span` der Zeile — in der Schaltfläche wie im
    // Text. Was dahinter nur für Vorleseprogramme steht, zählt hier nicht mit.
    name: [...(zeile.querySelector("span")?.childNodes ?? [])]
      .filter((knoten) => knoten.nodeType === Node.TEXT_NODE)
      .map((knoten) => knoten.textContent ?? "")
      .join("")
      .trim(),
    ziele: [...zeile.querySelectorAll("a")].map(
      (verweis) => verweis.querySelector(".sr-only")?.textContent ?? "",
    ),
  }));
}

describe("Die Ziele an der Zeitleiste", () => {
  /**
   * **Drei Lagen in einer Nachricht**, und das ist der gemessene Regelfall:
   * ein Schritt mit Datei *und* Protokoll, einer mit nur einem Protokoll, einer
   * ohne alles. Wo nichts liegt, hängt nichts — kein leeres Zeichen, keine
   * ausgegraute Stelle, kein Platzhalter.
   *
   * Die Zeitleiste bleibt, was sie war: **drei Zeilen für drei ausgeführte
   * Schritte**, in derselben Reihenfolge und mit denselben Namen.
   */
  it("hängt an jede Zeile genau die Artefakte ihres Schritts", async () => {
    const { behaelter, abbauen } = await rendereDetail({ schritte: SCHRITTE }, LISTE);

    try {
      expect(zeilen(behaelter)).toEqual([
        {
          name: "Datei konvertiert",
          ziele: ["Nutzdaten · Datei konvertiert", "Protokoll · Datei konvertiert — Ausschnitt"],
        },
        { name: "Datei versendet", ziele: ["Protokoll · Datei versendet — Ausschnitt"] },
        { name: "Bestätigung verarbeitet", ziele: [] },
      ]);

      // Kein zweiter Block mehr: Die Überschrift „Dateien und Protokolle" ist
      // samt ihren beiden Zählungen entfallen, und mit ihr die vier
      // wiederholten Schrittnamen.
      expect(behaelter.textContent).not.toContain("Dateien und Protokolle");
      expect(anfragen).toEqual([]);
    } finally {
      await abbauen();
    }
  });

  /**
   * **Die Antwort auf die Lücke, wegen der diese Nachbesserung entstanden ist.**
   *
   * Die Zeitleiste zeigte drei Schritte, die Artefaktlisten nannten vier —
   * darunter `Schritt 0 · SAPReader`. Der Grund ist keine Fehldarstellung,
   * sondern eine Ausnahme im Backend: `schritte[]` lässt den Metadaten-Schritt
   * weg (`SOSActionID = 0`), die Artefaktliste führt ihre `MessageActionID`
   * ungefiltert — und auf Schritt `0` liegt das Paar des Lesedienstes (M57).
   *
   * Es bekommt deshalb eine eigene Zeile **über** der Leiste, und dort heißen
   * die beiden nach ihrer Familie statt nach einer Schrittnummer, die es in der
   * Leiste nicht gibt.
   *
   * **Seit dem 19.08.2026 sind es zwei Ziele statt dreier.** Das dritte war
   * `Message.Payload.GUID`, geführt als *Eingegangene Datei*; nach M73 zeigt der
   * Name auf die Nutzdatenzeile mit dem höchsten `MessageActionID` derselben
   * Nachricht — dieselbe Datei, die die Leiste ohnehin führt.
   */
  it("stellt die Artefakte des Metadaten-Schritts über die Leiste, mit Familie statt Nummer", async () => {
    const { behaelter, abbauen } = await rendereDetail({ schritte: SCHRITTE }, LISTE);

    try {
      const eingang = eingangszeile(behaelter);
      expect(eingang).toBeDefined();

      expect(
        [...(eingang?.querySelectorAll("a") ?? [])].map((verweis) => ({
          name: verweis.querySelector(".sr-only")?.textContent,
          ziel: verweis.getAttribute("href"),
        })),
      ).toEqual([
        {
          name: "Nutzdaten · SAPReader",
          ziel: `/nachrichten/${MESSAGE_ID}/dateien/0-SAPReader.Payload.GUID`,
        },
        {
          name: "Protokoll · SAPReader — Ausschnitt",
          ziel: `/nachrichten/${MESSAGE_ID}/dateien/0-SAPReader.Log.GUID`,
        },
      ]);

      // Kein Ziel auf die Kennung, die es einen Tag lang gab (M73).
      expect(behaelter.innerHTML).not.toContain("0-Message.Payload.GUID");

      // Niemals „Schritt 0": Die Zeitleiste führt ihn nicht, also findet der
      // Nutzer die Nummer nirgends wieder.
      expect(behaelter.textContent).not.toContain("Schritt 0");
      // Und keine Zeile der Leiste hat ihn bekommen — die Leiste bleibt bei
      // ihren drei ausgeführten Schritten.
      expect(behaelter.querySelectorAll("ol > li")).toHaveLength(3);
    } finally {
      await abbauen();
    }
  });

  /**
   * **Die Belastungsprobe sind fünfzehn Artefakte, nicht drei** (M55).
   *
   * Fünfzehn eigene Ziele, alle unter der eigenen Route, keines mit einer
   * Abfragezeichenkette — und **kein doppelter React-Schlüssel**, obwohl
   * `Converter.Log.GUID` mehrfach vorkommt. Der Beleg dafür ist das Ausbleiben
   * eines `console.error` (`tests/setup/konsole.ts`).
   *
   * **Die Zahl bleibt fünfzehn.** Eines der fünfzehn war bis zum 19.08.2026
   * `Message.Payload.GUID` im Feld `eingang`; an seine Stelle tritt hier eine
   * Nutzdatei auf Schritt `3`, damit die Belastungsprobe die aus M55 bleibt und
   * nicht stillschweigend auf vierzehn schrumpft.
   */
  it("bleibt bei fünfzehn Artefakten bedienbar", async () => {
    const viele: Artefaktliste = {
      messageId: MESSAGE_ID,
      nutzdaten: [
        artefakt(0, "FileReader", "NUTZDATEN"),
        artefakt(1, "Converter", "NUTZDATEN"),
        artefakt(2, "Converter", "NUTZDATEN"),
        artefakt(3, "Router", "NUTZDATEN"),
        artefakt(4, "Splitter", "NUTZDATEN"),
        artefakt(6, "Converter", "NUTZDATEN"),
      ],
      protokolle: [
        artefakt(0, "FileReader", "PROTOKOLL", true),
        artefakt(1, "Converter", "PROTOKOLL", true),
        artefakt(2, "Converter", "PROTOKOLL", true),
        artefakt(3, "Router", "PROTOKOLL", true),
        artefakt(4, "Splitter", "PROTOKOLL", true),
        artefakt(5, "FTPSender", "PROTOKOLL", true),
        artefakt(6, "Converter", "PROTOKOLL", true),
        artefakt(7, "HTTPSender", "PROTOKOLL", true),
        artefakt(8, "FTPSender", "PROTOKOLL", true),
      ],
    };
    const achtSchritte = [1, 2, 3, 4, 5, 6, 7, 8].map((nummer) =>
      // Nur die ersten vier lösen zu einem Namen auf — das ist die gemessene
      // Mehrheit: 55,98 % der Artefakte tragen keinen (M57).
      schritt(nummer, nummer <= 4 ? `Erfundener Schritt ${nummer}` : ""),
    );

    const { behaelter, abbauen } = await rendereDetail({ schritte: achtSchritte }, viele);

    try {
      const ziele = [...behaelter.querySelectorAll("a")];
      expect(ziele).toHaveLength(15);

      const adressen = ziele.map((verweis) => verweis.getAttribute("href") ?? "");
      expect(new Set(adressen).size).toBe(15);
      expect(
        adressen.every((adresse) => adresse.startsWith(`/nachrichten/${MESSAGE_ID}/dateien/`)),
      ).toBe(true);
      expect(adressen.some((adresse) => adresse.includes("?"))).toBe(false);

      // Acht Schrittzeilen tragen zusammen dreizehn Ziele; die zwei auf Schritt 0
      // hängen an der Eingangszeile darüber.
      expect(zeilen(behaelter).flatMap((zeile) => zeile.ziele)).toHaveLength(13);

      // Der Rückfall greift genau dort, wo die Schrittfolge keinen Namen liefert
      // — und er erfindet keinen.
      expect(zeilen(behaelter)[5]?.ziele).toEqual([
        "Nutzdaten · Schritt 6 · Converter",
        "Protokoll · Schritt 6 · Converter — Ausschnitt",
      ]);

      expect(anfragen).toEqual([]);
    } finally {
      await abbauen();
    }
    // Der Nachweis zum Schlüssel ist das Ausbleiben eines `console.error`.
  });

  /**
   * **Ein Klick auf die Zeile schaltet genau einmal, ein Klick auf ein Ziel
   * nicht** (`docs/nachrichtendetail.md` §10.16).
   *
   * Die Schaltfläche spannt die ganze Zeile, die Ziele liegen als Geschwister
   * darüber — nicht in ihr, das wäre verschachtelte Bedienung. Dass ein Klick auf
   * ein Ziel die Zeile nicht mitschaltet, ist eine Aussage über den **Baum**: Das
   * Ziel ist kein Nachfahre der Schaltfläche, also erreicht sein Klick sie nicht.
   *
   * Der Zustand liegt allein in `aria-expanded`; `aria-controls` zeigt in beiden
   * Zuständen auf den eingehängten Inhalt, und der zugängliche Name beginnt mit
   * dem sichtbaren Schrittnamen (WCAG 2.5.3). Der `title` bleibt die Herkunft.
   */
  it("schaltet die Zeile mit einem Klick genau einmal — und mit einem Klick auf ein Ziel gar nicht", async () => {
    const { behaelter, abbauen } = await rendereDetail(
      { schritte: SCHRITTE, eigenschaftenAnzahl: EIGENSCHAFTEN.length },
      LISTE,
      EIGENSCHAFTEN,
    );

    try {
      const zeile = behaelter.querySelectorAll("ol > li")[0];
      const schalter = zeile.querySelector("button");
      expect(schalter?.textContent?.startsWith("Datei konvertiert")).toBe(true);
      expect(schalter?.getAttribute("title")).toBe(
        "Datei konvertiert\nBaustein: ERFUNDEN_BAUSTEIN\nName aus der Ablaufdefinition",
      );
      expect(schalter?.getAttribute("aria-expanded")).toBe("false");

      const inhalt = behaelter.querySelector<HTMLElement>(
        `[id="${schalter?.getAttribute("aria-controls")}"]`,
      );
      expect(inhalt).not.toBeNull();
      expect(zeile.contains(inhalt)).toBe(true);
      expect(inhalt?.hasAttribute("inert")).toBe(true);

      // Die Ziele sind Geschwister der Schaltfläche, keine Nachfahren.
      const ziele = [...zeile.querySelectorAll("a")];
      expect(ziele).toHaveLength(2);
      expect(ziele.some((ziel) => schalter?.contains(ziel))).toBe(false);

      // `jsdom` kann nicht navigieren und meldete es sonst. Mit gedrückter
      // Strg-Taste überlässt `next/link` den Klick dem Browser, und
      // `preventDefault` am Verweis selbst nimmt `jsdom` die Navigation ab. Am
      // Weg des Ereignisses durch den Baum ändert beides nichts — und nur um
      // den geht es hier.
      ziele[0].addEventListener("click", (ereignis) => ereignis.preventDefault());
      await act(async () => {
        ziele[0].dispatchEvent(
          new MouseEvent("click", { bubbles: true, cancelable: true, ctrlKey: true }),
        );
      });
      expect(schalter?.getAttribute("aria-expanded")).toBe("false");

      await klicke(schalter);
      expect(schalter?.getAttribute("aria-expanded")).toBe("true");
      expect(inhalt?.hasAttribute("inert")).toBe(false);
      expect([...(inhalt?.querySelectorAll("ul > li") ?? [])].map((z) => z.textContent)).toEqual([
        "Converter.TypeERFUNDEN",
      ]);

      // Offen trägt der Abschnitt die Akzentlinie und der Name die Akzentfarbe —
      // Anwendungszustand, keine Statusaussage; Strichbreite und Art bleiben.
      expect(zeile.classList.contains("border-akzent-schrift")).toBe(true);
      expect(zeile.classList.contains("border-l-2")).toBe(true);
      expect(zeile.classList.contains("border-dashed")).toBe(false);

      // Die Zeilen sind unabhängig: Die zweite bleibt zu, und sie lässt sich
      // dazu öffnen, ohne dass die erste zufällt.
      const zweite = behaelter.querySelectorAll("ol > li")[1].querySelector("button");
      expect(zweite?.getAttribute("aria-expanded")).toBe("false");
      await klicke(zweite);
      expect(zweite?.getAttribute("aria-expanded")).toBe("true");
      expect(schalter?.getAttribute("aria-expanded")).toBe("true");

      await klicke(schalter);
      expect(schalter?.getAttribute("aria-expanded")).toBe("false");
      expect(inhalt?.hasAttribute("inert")).toBe(true);
      expect(zeile.classList.contains("border-akzent-schrift")).toBe(false);

      expect(anfragen).toEqual([]);
    } finally {
      await abbauen();
    }
  });

  /**
   * **Aufklappbar ist nur, was Inhalt hat** (E‑221). Schritt 3 trägt keine
   * Eigenschaft — gemessen möglich, `MessageActionID = 502` steht in
   * `MessageAction` und fehlt in `MessageProperty` (M17 3). Seine Zeile bleibt
   * Text: **kein `button`**, und damit auch kein Inhalt, der sich öffnen ließe.
   *
   * Die Gegenprobe steht daneben: Ohne jede Eigenschaft ist keine Zeile eine
   * Schaltfläche, und es geht keine Anfrage hinaus.
   */
  it("macht aus einem Schritt ohne Eigenschaften keine Schaltfläche", async () => {
    const mit = await rendereDetail(
      { schritte: SCHRITTE, eigenschaftenAnzahl: EIGENSCHAFTEN.length },
      LISTE,
      EIGENSCHAFTEN,
    );

    try {
      const zeilenMit = [...mit.behaelter.querySelectorAll("ol > li")];
      expect(zeilenMit.map((zeile) => zeile.querySelector("button") !== null)).toEqual([
        true,
        true,
        false,
      ]);
      expect(zeilenMit[2].querySelector("[inert]")).toBeNull();
      // Keine Marke, keine Zahl, kein Pfeil an der eingeklappten Zeile.
      expect(mit.behaelter.querySelectorAll("ol svg.lucide-chevron-right")).toHaveLength(0);
    } finally {
      await mit.abbauen();
    }

    const ohne = await rendereDetail({ schritte: SCHRITTE, eigenschaftenAnzahl: 0 }, LISTE);

    try {
      expect(zeilen(ohne.behaelter).map((zeile) => zeile.name)).toEqual([
        "Datei konvertiert",
        "Datei versendet",
        "Bestätigung verarbeitet",
      ]);
      expect(ohne.behaelter.querySelectorAll("ol button")).toHaveLength(0);
      expect(anfragen).toEqual([]);
    } finally {
      await ohne.abbauen();
    }
  });

  /**
   * **Der zugeklappte Inhalt ist `inert`, und die Bewegung trägt ihren
   * Ausschalter** (E‑228). Belegt über Attribut und Klassen, wie beim
   * Ansichtsumschalter: `jsdom` rechnet weder Layout noch Übergänge, und die
   * Regel *ist* die Klasse. Dazu die beiden Farbübergänge an Linie und Name —
   * und dass an der Schaltfläche selbst **kein** Übergang hängt: Das Überfahren
   * bewegt nichts.
   */
  it("hält den zugeklappten Inhalt der Zeile inert und gibt jeder Bewegung ihren Ausschalter", async () => {
    const { behaelter, abbauen } = await rendereDetail(
      { schritte: SCHRITTE, eigenschaftenAnzahl: EIGENSCHAFTEN.length },
      LISTE,
      EIGENSCHAFTEN,
    );

    try {
      const zeile = behaelter.querySelectorAll("ol > li")[0];
      const schalter = zeile.querySelector("button");
      const spur = zeile.querySelector('[data-aufklappen="spur"]');
      const blende = zeile.querySelector('[data-aufklappen="inhalt"]');

      expect(spur?.querySelector("[inert]")).not.toBeNull();
      for (const klasse of [
        "transition-[grid-template-rows]",
        "duration-[220ms]",
        "ease-[cubic-bezier(0.2,0,0,1)]",
        "motion-reduce:transition-none",
      ]) {
        expect(spur?.classList.contains(klasse), klasse).toBe(true);
      }
      expect(blende?.classList.contains("motion-reduce:transition-none")).toBe(true);

      // Linie und Name wechseln die Farbe in denselben 220 ms.
      for (const element of [zeile, schalter?.querySelector("span")]) {
        expect(element?.classList.contains("duration-[220ms]")).toBe(true);
        expect(element?.classList.contains("motion-reduce:transition-none")).toBe(true);
      }
      expect(zeile.classList.contains("transition-[border-color]")).toBe(true);
      expect(schalter?.querySelector("span")?.classList.contains("transition-[color]")).toBe(true);

      // Kein Übergang beim Überfahren: Die Hover-Fläche liegt an der
      // Schaltfläche, und die trägt keinen.
      expect([...(schalter?.classList ?? [])].filter((k) => k.startsWith("transition"))).toEqual(
        [],
      );
    } finally {
      await abbauen();
    }
  });

  /**
   * **Die Eingangszeile gibt es, wenn auf Schritt `0` ein Artefakt *oder* eine
   * Eigenschaft liegt; aufklappbar ist sie nur mit Eigenschaften** (E‑222).
   *
   * Drei Lagen, und die mittlere ist neu: Bis zum 21.09.2026 verschwand die
   * Zeile ohne Lesedienst ganz. Sie bleibt in jeder Lage gestrichelt, ohne
   * Balken und ohne Dauer — und **ein `Message.*` steht nie darin**, das gehört
   * in den Block.
   */
  it("zeigt den Eingang mit Eigenschaften ohne Ziele — und mit Zielen ohne Eigenschaften", async () => {
    const aufSchrittNull: Eigenschaft[] = [
      ...EIGENSCHAFTEN,
      {
        name: "SAPReader.Filename",
        wert: "ERFUNDEN.idoc",
        position: 0,
        gekappt: false,
        originalLaengeBytes: null,
      },
    ];
    const ohneLesedienst: Artefaktliste = {
      messageId: MESSAGE_ID,
      nutzdaten: [artefakt(1, "DataWarehouse", "NUTZDATEN")],
      protokolle: [],
    };

    // Eigenschaften ohne Ziele: Die Zeile gibt es, und sie klappt auf.
    const nurEigenschaften = await rendereDetail(
      { schritte: SCHRITTE, eigenschaftenAnzahl: aufSchrittNull.length },
      ohneLesedienst,
      aufSchrittNull,
    );
    try {
      const eingang = eingangszeile(nurEigenschaften.behaelter);
      expect(eingang).toBeDefined();
      expect(eingang?.classList.contains("border-dashed")).toBe(true);
      expect(eingang?.querySelectorAll("a")).toHaveLength(0);

      const schalter = eingang?.querySelector("button");
      expect(schalter?.textContent?.startsWith(DATEIEN.eingang)).toBe(true);
      await klicke(schalter);
      expect(schalter?.getAttribute("aria-expanded")).toBe("true");
      expect([...(eingang?.querySelectorAll("ul > li") ?? [])].map((z) => z.textContent)).toEqual([
        "SAPReader.FilenameERFUNDEN.idoc",
      ]);
      // Offen bleibt die Linie gestrichelt — und trägt den Akzent.
      expect(eingang?.classList.contains("border-dashed")).toBe(true);
      expect(eingang?.classList.contains("border-akzent-schrift")).toBe(true);
    } finally {
      await nurEigenschaften.abbauen();
    }

    // Ziele ohne Eigenschaften: Die Zeile gibt es, aber sie ist Text.
    const nurZiele = await rendereDetail(
      { schritte: SCHRITTE, eigenschaftenAnzahl: EIGENSCHAFTEN.length },
      LISTE,
      EIGENSCHAFTEN.filter((eintrag) => eintrag.position !== 0),
    );
    try {
      const eingang = eingangszeile(nurZiele.behaelter);
      expect(eingang?.querySelectorAll("a")).toHaveLength(2);
      expect(eingang?.querySelector("button")).toBeNull();
    } finally {
      await nurZiele.abbauen();
    }

    // Nur ein `Message.*` auf Schritt 0 und kein Lesedienst: Dort liegt nichts,
    // was in den Eingang gehörte — wo nichts liegt, hängt nichts.
    const nichts = await rendereDetail(
      { schritte: SCHRITTE, eigenschaftenAnzahl: EIGENSCHAFTEN.length },
      ohneLesedienst,
      EIGENSCHAFTEN,
    );
    try {
      expect(eingangszeile(nichts.behaelter)).toBeUndefined();
      // Und weiterhin keine eigene Zeile für Schritt 0 in der Leiste.
      expect(nichts.behaelter.querySelectorAll("ol > li")).toHaveLength(3);
    } finally {
      await nichts.abbauen();
    }
  });

  /**
   * **Der Rest, den es gemessen nicht gibt** (E‑223): Eigenschaften auf einer
   * Position ungleich `0`, für die die Leiste keine Zeile führt. Von Hand ist
   * das nie zu sehen — `ohne_schrittzeile` ist in beiden Fenstern `0` (M57,
   * Befund 1). Gebaut ist es, damit nichts lautlos herausfällt: Die Zeile *Ohne
   * Schritt in der Zeitleiste* wird aufklappbar, und jede Position trägt den
   * Rückfall *Schritt N*. **Kein erfundener Name.**
   */
  it("führt Eigenschaften ohne Zeile unter dem Rest, je Position mit dem Rückfall", async () => {
    const mitRest: Eigenschaft[] = [
      ...EIGENSCHAFTEN,
      {
        name: "Service.Type",
        wert: "SPAET",
        position: 9,
        gekappt: false,
        originalLaengeBytes: null,
      },
      {
        name: "Service.Type",
        wert: "FRUEH",
        position: 7,
        gekappt: false,
        originalLaengeBytes: null,
      },
    ];

    const { behaelter, abbauen } = await rendereDetail(
      { schritte: SCHRITTE, eigenschaftenAnzahl: mitRest.length },
      LISTE,
      mitRest,
    );

    try {
      const rest = [...behaelter.querySelectorAll("div.border-dashed")].find((kasten) =>
        (kasten.textContent ?? "").startsWith(DATEIEN.ohneZeile),
      );
      expect(rest).toBeDefined();

      await klicke(rest?.querySelector("button"));
      expect([...(rest?.querySelectorAll("p") ?? [])].map((kopf) => kopf.textContent)).toEqual([
        "Schritt 7",
        "Schritt 9",
      ]);
      expect([...(rest?.querySelectorAll("ul > li") ?? [])].map((z) => z.textContent)).toEqual([
        "Service.TypeFRUEH",
        "Service.TypeSPAET",
      ]);
    } finally {
      await abbauen();
    }
  });
});
