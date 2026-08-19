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
 * | Das Anspringen | Der Klick auf einen Schritt klappt den Eigenschaftenblock auf und setzt den Fokus auf **seine** Gruppe. Fokus ist ein Zustand des Dokuments |
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
    ueberfaellig: false,
    schritte: [],
    kuratierteEigenschaften: [],
    ...werte,
  };
}

/**
 * **Die gemessene Gestalt einer Nachricht: die Zeitleiste führt drei Schritte,
 * die Artefakte liegen auf vier Positionen.**
 *
 * Auf Schritt `0` sitzen die eingegangene Datei *und* das Paar des Lesedienstes
 * (M57, Fenster A) — er kommt in `schritte[]` nicht vor, weil das Backend den
 * Metadaten-Schritt dort ausnimmt (`docs/nachrichtendetail.md` §4).
 */
const SCHRITTE = [
  schritt(1, "Datei konvertiert"),
  schritt(2, "Datei versendet"),
  schritt(3, "Bestätigung verarbeitet"),
];

const LISTE: Artefaktliste = {
  messageId: MESSAGE_ID,
  eingang: artefakt(0, "Message", "NUTZDATEN"),
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
  eigenschaften: Eigenschaft[] | null = [],
) {
  const zwischenspeicher = neuerZwischenspeicher();
  zwischenspeicher.setQueryData(NACHRICHTEN_SCHLUESSEL.detail(MESSAGE_ID), detail(werte));
  zwischenspeicher.setQueryData(NACHRICHTEN_SCHLUESSEL.dateien(MESSAGE_ID), liste);
  // `null` heißt **kalter Zwischenspeicher**: Die Eigenschaften sind beim Klick
  // noch nicht da, und die Abfrage dafür läuft ins Leere (der `fetch`-Rumpf
  // löst nie auf). Genau die Lage, in der ein Sprung warten muss.
  if (eigenschaften !== null) {
    zwischenspeicher.setQueryData(NACHRICHTEN_SCHLUESSEL.eigenschaften(MESSAGE_ID), eigenschaften);
  }

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

/**
 * Ein `fetch`, das **nie antwortet** — der kalte Zwischenspeicher.
 *
 * Der Rumpf aus `beforeEach` weist jede Anfrage ab; das ist für die übrigen
 * Fälle richtig, hier aber falsch: Eine abgewiesene Abfrage ist fertig, und
 * gebraucht wird eine, die **läuft**.
 */
function anfrageHaengt(): void {
  vi.stubGlobal(
    "fetch",
    vi.fn((eingabe: RequestInfo | URL) => {
      anfragen.push(String(eingabe));
      return new Promise<Response>(() => undefined);
    }),
  );
}

/**
 * Die Antwort auf die laufende Abfrage — **und der Makrotask danach.**
 *
 * TanStack Query bündelt seine Benachrichtigungen und stellt sie in die
 * Aufgabenschlange. Ohne diesen Durchlauf bliebe der Baum im Ladezustand
 * stehen, obwohl im Zwischenspeicher längst Daten liegen — ein Mikrotask
 * genügt dafür nicht.
 */
async function antwortTrifftEin(
  zwischenspeicher: ReturnType<typeof neuerZwischenspeicher>,
  eigenschaften: Eigenschaft[],
): Promise<void> {
  await act(async () => {
    zwischenspeicher.setQueryData(NACHRICHTEN_SCHLUESSEL.eigenschaften(MESSAGE_ID), eigenschaften);
    await new Promise((fertig) => setTimeout(fertig, 0));
  });
}

/** Ein Klick, wie ihn die meisten Browser auslösen: Er setzt vorher den Fokus. */
async function klicke(element: Element | null | undefined): Promise<void> {
  expect(element).toBeTruthy();
  await act(async () => {
    (element as HTMLElement).focus();
    element?.dispatchEvent(new MouseEvent("click", { bubbles: true }));
  });
}

/**
 * Ein Klick, der den Fokus **nicht** mitnimmt.
 *
 * Das ist kein erfundener Fall: Safari auf macOS fokussiert eine Schaltfläche
 * beim Klicken nicht. Wer sich darauf verlässt, dass ein Klick den Fokus
 * bewegt, baut eine Regel, die auf einem Drittel der Geräte nicht greift —
 * deshalb prüft dieser Weg die Regel, die ohne den Fokus auskommt.
 */
async function klickeOhneFokus(element: Element | null | undefined): Promise<void> {
  expect(element).toBeTruthy();
  await act(async () => {
    element?.dispatchEvent(new MouseEvent("click", { bubbles: true }));
  });
}

/** Die Zeilen der Zeitleiste samt der Ziele, die an ihnen hängen. */
function zeilen(behaelter: HTMLElement): { name: string; ziele: string[] }[] {
  return [...behaelter.querySelectorAll("ol > li")].map((zeile) => ({
    name: (zeile.querySelector("button, span")?.textContent ?? "").trim(),
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
   * ungefiltert — und auf Schritt `0` liegt **nicht nur** der Eingang, sondern
   * auch das Paar des Lesedienstes (M57).
   *
   * Sie bekommen deshalb eine eigene Zeile **über** der Leiste, und dort heißen
   * sie nach ihrer Familie statt nach einer Schrittnummer, die es in der Leiste
   * nicht gibt.
   */
  it("stellt die Artefakte des Metadaten-Schritts über die Leiste, mit Familie statt Nummer", async () => {
    const { behaelter, abbauen } = await rendereDetail({ schritte: SCHRITTE }, LISTE);

    try {
      const eingang = [...behaelter.querySelectorAll("div")].find(
        (kasten) => kasten.firstElementChild?.textContent === DATEIEN.eingang,
      );
      expect(eingang).toBeDefined();

      expect(
        [...(eingang?.querySelectorAll("a") ?? [])].map((verweis) => ({
          name: verweis.querySelector(".sr-only")?.textContent,
          ziel: verweis.getAttribute("href"),
        })),
      ).toEqual([
        {
          name: "Eingegangene Datei",
          ziel: `/nachrichten/${MESSAGE_ID}/dateien/0-Message.Payload.GUID`,
        },
        {
          name: "Nutzdaten · SAPReader",
          ziel: `/nachrichten/${MESSAGE_ID}/dateien/0-SAPReader.Payload.GUID`,
        },
        {
          name: "Protokoll · SAPReader — Ausschnitt",
          ziel: `/nachrichten/${MESSAGE_ID}/dateien/0-SAPReader.Log.GUID`,
        },
      ]);

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
   */
  it("bleibt bei fünfzehn Artefakten bedienbar", async () => {
    const viele: Artefaktliste = {
      messageId: MESSAGE_ID,
      eingang: artefakt(0, "Message", "NUTZDATEN"),
      nutzdaten: [
        artefakt(0, "FileReader", "NUTZDATEN"),
        artefakt(1, "Converter", "NUTZDATEN"),
        artefakt(2, "Converter", "NUTZDATEN"),
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

      // Acht Schrittzeilen tragen zusammen zwölf Ziele; die drei auf Schritt 0
      // hängen an der Eingangszeile darüber.
      expect(zeilen(behaelter).flatMap((zeile) => zeile.ziele)).toHaveLength(12);

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
   * **Der Weg von der Zeitleiste zu den technischen Eigenschaften.**
   *
   * Ein Klick auf einen Schritt klappt den Block auf und setzt den Fokus auf
   * **seine** Gruppe. Beides ist Zustand des Dokuments und in keiner Funktion
   * belegbar: `aria-expanded` am Schalter und `document.activeElement`.
   *
   * Es entsteht dabei **kein neuer Block und keine Duplizierung** — die
   * Gruppierung nach Schritt besteht seit dem 17.08.2026, es fehlte nur der Weg
   * dorthin.
   */
  it("springt aus der Zeitleiste in die Eigenschaftengruppe desselben Schritts", async () => {
    const { behaelter, abbauen } = await rendereDetail(
      { schritte: SCHRITTE, eigenschaftenAnzahl: EIGENSCHAFTEN.length },
      LISTE,
      EIGENSCHAFTEN,
    );

    try {
      const schalter = [...behaelter.querySelectorAll("ol > li button")];
      expect(schalter).toHaveLength(3);
      // Der sichtbare Name steht im zugänglichen Namen (WCAG 2.5.3).
      expect(schalter[1]?.getAttribute("aria-label")).toBe(
        DATEIEN.zuEigenschaften.replace("{name}", "Datei versendet"),
      );

      await act(async () => {
        schalter[1]?.dispatchEvent(new MouseEvent("click", { bubbles: true }));
      });

      const block = behaelter.querySelector('[aria-expanded="true"]');
      expect(block).not.toBeNull();

      // Der Fokus sitzt auf dem Abschnitt zu Schritt 2 — und der trägt die
      // Überschrift, die eine Zeile darüber in der Zeitleiste steht.
      const fokussiert = document.activeElement as HTMLElement | null;
      expect(fokussiert?.tagName).toBe("SECTION");
      expect(fokussiert?.querySelector("h3")?.textContent).toContain("Datei versendet");

      expect(anfragen).toEqual([]);
    } finally {
      await abbauen();
    }
  });

  /**
   * **Ein Sprung, den niemand mehr will, kommt nicht nach.**
   *
   * Bei kaltem Zwischenspeicher liegt zwischen Klick und Gruppe die Abfrage —
   * in dieser Umgebung Sekunden, weil jede Anfrage die Sitzung schreibt. Wer in
   * der Wartezeit von Hand zuklappt, hat den Sprung aufgegeben. Ohne die Regel
   * käme er beim nächsten Aufklappen nach, Minuten später und ohne Anlass, und
   * risse den Nutzer aus dem heraus, was er inzwischen liest.
   *
   * Der Fall ist im Baum und nirgends sonst belegbar: Er besteht aus einer
   * Reihenfolge von Betätigungen und endet auf `document.activeElement`.
   */
  it("holt einen Sprung nicht nach, den der Nutzer beim Warten zugeklappt hat", async () => {
    anfrageHaengt();
    const { behaelter, zwischenspeicher, abbauen } = await rendereDetail(
      { schritte: SCHRITTE, eigenschaftenAnzahl: EIGENSCHAFTEN.length },
      LISTE,
      // Kalt: Die Eigenschaften sind beim Klick noch nicht da.
      null,
    );

    try {
      const schrittSchalter = [...behaelter.querySelectorAll("ol > li button")];
      await klicke(schrittSchalter[1]);

      // Der Block ist offen und lädt — es gibt noch nichts anzuspringen.
      const blockSchalter = behaelter.querySelector('[aria-expanded="true"]');
      expect(blockSchalter).not.toBeNull();
      expect(document.activeElement).toBe(schrittSchalter[1]);

      // Zuklappen und wieder aufklappen, beides von Hand — und **ohne dass der
      // Klick den Fokus mitnimmt**, wie es Safari tut. Damit hängt der Nachweis
      // an der Regel „Zuklappen erledigt den Sprung" und nicht am Fokusvergleich
      // daneben.
      await klickeOhneFokus(blockSchalter);
      await klickeOhneFokus(behaelter.querySelector('[aria-expanded="false"]'));

      // Erst jetzt trifft die Antwort ein.
      await antwortTrifftEin(zwischenspeicher, EIGENSCHAFTEN);

      // Die Gruppen stehen da — aber der Fokus ist geblieben, wo der Nutzer ihn
      // zuletzt selbst hingesetzt hat.
      expect(behaelter.querySelectorAll("section h3").length).toBeGreaterThan(0);
      expect((document.activeElement as HTMLElement | null)?.tagName).not.toBe("SECTION");
      expect(document.activeElement).toBe(schrittSchalter[1]);
    } finally {
      await abbauen();
    }
  });

  /**
   * **Wer in der Wartezeit weitergegangen ist, wird nicht zurückgerissen.**
   *
   * Der zweite Weg in dieselbe Lage, und er kommt ohne Zuklappen aus: Der
   * Nutzer tabbt weiter, während die Abfrage läuft. Trifft die Antwort dann ein,
   * risse ein Sprung ihn aus dem heraus, was er gerade tut — bei einem
   * Vorleseprogramm mitten in der Ansage.
   */
  it("springt nicht, wenn der Fokus in der Wartezeit weitergewandert ist", async () => {
    anfrageHaengt();
    const { behaelter, zwischenspeicher, abbauen } = await rendereDetail(
      { schritte: SCHRITTE, eigenschaftenAnzahl: EIGENSCHAFTEN.length },
      LISTE,
      null,
    );

    try {
      await klicke([...behaelter.querySelectorAll("ol > li button")][1]);

      // Weiter zum Protokoll-Ziel derselben Zeile — nur der Fokus, kein Klick.
      const weiter = behaelter.querySelectorAll("ol > li a")[1] as HTMLElement;
      await act(async () => {
        weiter.focus();
      });
      expect(document.activeElement).toBe(weiter);

      await antwortTrifftEin(zwischenspeicher, EIGENSCHAFTEN);

      // Die Gruppen stehen da, der Fokus ist geblieben, wo der Nutzer ihn
      // hingesetzt hat.
      expect(behaelter.querySelectorAll("section h3").length).toBeGreaterThan(0);
      expect(document.activeElement).toBe(weiter);
    } finally {
      await abbauen();
    }
  });

  /**
   * **Die Gegenprobe: Wer wartet, bekommt seinen Sprung.**
   *
   * Derselbe kalte Zwischenspeicher, nur ohne Zwischenhandlung — der Fokus
   * bleibt auf der Schaltfläche, und mit der Antwort springt die Ansicht. Ohne
   * diesen Fall bestünde der Test darüber auch dann, wenn der Sprung gar nicht
   * mehr funktionierte.
   */
  it("springt nach, sobald die Antwort da ist — wenn der Nutzer stehen geblieben ist", async () => {
    anfrageHaengt();
    const { behaelter, zwischenspeicher, abbauen } = await rendereDetail(
      { schritte: SCHRITTE, eigenschaftenAnzahl: EIGENSCHAFTEN.length },
      LISTE,
      null,
    );

    try {
      const schrittSchalter = [...behaelter.querySelectorAll("ol > li button")];
      await klicke(schrittSchalter[1]);
      expect(document.activeElement).toBe(schrittSchalter[1]);

      await antwortTrifftEin(zwischenspeicher, EIGENSCHAFTEN);

      const fokussiert = document.activeElement as HTMLElement | null;
      expect(fokussiert?.tagName).toBe("SECTION");
      expect(fokussiert?.querySelector("h3")?.textContent).toContain("Datei versendet");
    } finally {
      await abbauen();
    }
  });

  /**
   * **Ohne Eigenschaften gibt es unten keinen Block — und oben keinen Weg
   * dorthin.** Ein Bedienelement, das ins Leere führte, wäre schlechter als
   * keines; es ist dieselbe Regel, aus der der Block bei `eigenschaftenAnzahl
   * === 0` einen Satz statt eines Schalters zeigt.
   */
  it("macht die Schrittnamen nicht bedienbar, wenn es keine Eigenschaften gibt", async () => {
    const { behaelter, abbauen } = await rendereDetail({ schritte: SCHRITTE }, LISTE);

    try {
      expect(behaelter.querySelectorAll("ol > li button")).toHaveLength(0);
    } finally {
      await abbauen();
    }
  });
});
