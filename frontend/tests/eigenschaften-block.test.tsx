// @vitest-environment jsdom

import { act } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { texteFuer } from "@/i18n";
import { NACHRICHTEN_SCHLUESSEL, type Eigenschaft, type Schritt } from "@/features/nachrichten/api";
import { EigenschaftenBlock } from "@/features/nachrichten/components/eigenschaften-block";

import { neuerZwischenspeicher, rendere } from "./hilfe/rendern";

/**
 * **Die vier Fälle des Eigenschaftenblocks, für die ein gerenderter Baum die
 * einzige Prüfung ist** *(17.08.2026)*.
 *
 * Die Einteilung selbst ist reine Funktion und steht in
 * `tests/nachrichtendetail.test.ts` — geprüft werden Entscheidungen, nicht
 * Markup (`docs/frontend-grundlagen.md` §9). Diese Datei ist die begründete
 * Ausnahme, nach derselben Bedingung wie `tests/bam-block.test.tsx`: **Es gibt
 * keinen anderen Ort, an dem der Satz belegbar wäre.**
 *
 * | Test | Warum genau dieser |
 * |---|---|
 * | Derselbe Name in zwei Gruppen | Der Schlüssel der Liste ist `${position}:${name}`. Wäre er der Name allein, meldete React einen **doppelten Schlüssel** — in der Konsole, und sichtbar falsch wäre nichts. 31 der 101 gemessenen Namen kommen auf mehr als einem Schritt vor (M17 3) |
 * | `eigenschaftenAnzahl === 0` | **Kein Schalter im Baum und keine Anfrage.** Beides ist eine Aussage über Abwesenheit, und die ist ohne Baum nicht zu treffen |
 * | eingeklappt mit Werten | Die Gegenprobe: Ohne sie bewiese der vorige Test nur, dass ein Block ohne Inhalt nicht lädt — nicht, dass der Block **wartet**, bis jemand aufklappt |
 * | aufgeklappt ohne `schritte` | Der Rückfall aus Regel 5 der Einteilung ist eine Zeichenkette der Sprachdatei und entsteht erst im Baum. Er tritt ein, solange das Detail noch nicht da ist |
 *
 * Der erste hängt vollständig an `tests/setup/konsole.ts`: Er besteht genau
 * dann, wenn **kein `console.error`** fällt.
 *
 * **Antwortrümpfe gestellt, kein Netz, keine Datenbank.**
 */

const TEXTE = texteFuer("de");
const EIGENSCHAFTEN = TEXTE.nachrichten.detail.eigenschaften;

const MESSAGE_ID = "8f3a1c2e-0000-4000-8000-000000000001";

function eigenschaft(name: string, position: number, wert: string): Eigenschaft {
  return { name, wert, position, gekappt: false, originalLaengeBytes: null };
}

function schritt(position: number, name: string): Schritt {
  return {
    position,
    name,
    namensherkunft: "DIREKT",
    rohwert: "NXS_FILE_CONVERT|E2A|UNWRAP",
    start: "2025-12-29T22:41:12Z",
    ende: "2025-12-29T22:41:12Z",
    dauerSekunden: 0,
    timeoutSekunden: 1800,
    laeuftAuf: false,
  };
}

/**
 * **Derselbe Name auf drei Schritten** — genau die Gestalt, die einen Schlüssel
 * aus dem Namen allein zerlegte. `Service.Type` steht im gemessenen Tagesfenster
 * auf sieben verschiedenen Schritten (M17 3).
 */
const DERSELBE_NAME_MEHRFACH: Eigenschaft[] = [
  eigenschaft("Message.GUID", 0, "8f3a1c2e-0000-4000-8000-000000000001"),
  eigenschaft("Service.Type", 0, "ScheduleBean"),
  eigenschaft("Service.Type", 1, "FileReader"),
  eigenschaft("Converter.Log.GUID", 2, "log|1"),
  eigenschaft("Service.Type", 2, "Converter"),
];

const SCHRITTE = [schritt(1, "Datei gelesen"), schritt(2, "Datei konvertiert")];

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

/** Klickt den Schalter des Blocks — aufklappen heißt hier zugleich laden. */
async function klappeAuf(behaelter: HTMLElement): Promise<void> {
  const schalter = behaelter.querySelector("button");
  expect(schalter).not.toBeNull();
  await act(async () => {
    schalter?.dispatchEvent(new MouseEvent("click", { bubbles: true }));
  });
}

/** Die Überschriften der Gruppen, in Baumreihenfolge. */
function gruppenkoepfe(behaelter: HTMLElement): (string | null)[] {
  return [...behaelter.querySelectorAll("section h3")].map((kopf) => kopf.textContent);
}

describe("Der Eigenschaftenblock", () => {
  /**
   * **Die Regression zum Schlüssel.** `${position}:${name}` ist eindeutig — der
   * Primärschlüssel ist `(MessageID, MessagePropertyName, MessageActionID)`. Der
   * Name allein ist es nicht: 31 der 101 gemessenen Namen stehen auf mehr als
   * einem Schritt (M17 3). Ein Schlüssel aus dem Namen allein erzeugte doppelte
   * Schlüssel, und die sind für Vitest unsichtbar, solange kein Baum gerendert
   * wird.
   *
   * Nebenbei belegt der Test die eigentliche Absicht der Nacharbeit: Die
   * scheinbare Dublette verteilt sich sichtbar auf drei Gruppen, und die Summe
   * der Zahlen in den Gruppenköpfen ist die Zahl im Blockkopf.
   */
  it("zeigt denselben Namen in drei Gruppen ohne doppelten React-Schlüssel", async () => {
    const zwischenspeicher = neuerZwischenspeicher();
    zwischenspeicher.setQueryData(
      NACHRICHTEN_SCHLUESSEL.eigenschaften(MESSAGE_ID),
      DERSELBE_NAME_MEHRFACH,
    );

    const { behaelter, abbauen } = await rendere(
      <EigenschaftenBlock messageId={MESSAGE_ID} anzahl={5} schritte={SCHRITTE} />,
      zwischenspeicher,
    );

    try {
      await klappeAuf(behaelter);

      // Die Nachricht zuerst, danach die Schritte in der Reihenfolge der
      // Zeitleiste — und wortgleich mit ihren Namen.
      expect(gruppenkoepfe(behaelter)).toEqual([
        "Nachricht (2)",
        "Datei gelesen (1)",
        "Datei konvertiert (2)",
      ]);

      // Derselbe Name steht dreimal da, in drei verschiedenen Gruppen — genau
      // darum geht es.
      expect(
        [...behaelter.querySelectorAll("section")].map((gruppe) =>
          [...gruppe.querySelectorAll("li")].map((zeile) => zeile.textContent),
        ),
      ).toEqual([
        ["Message.GUID8f3a1c2e-0000-4000-8000-000000000001", "Service.TypeScheduleBean"],
        ["Service.TypeFileReader"],
        ["Converter.Log.GUIDlog|1", "Service.TypeConverter"],
      ]);

      // Der Tooltip des Gruppenkopfs ist derselbe wie in der Zeitleiste. Bei der
      // Nachricht gibt es keinen Schritt und deshalb auch keinen Tooltip.
      expect(
        [...behaelter.querySelectorAll("section h3")].map((kopf) => kopf.getAttribute("title")),
      ).toEqual([
        null,
        "Datei gelesen\nBaustein: NXS_FILE_CONVERT|E2A|UNWRAP\nName aus der Ablaufdefinition",
        "Datei konvertiert\nBaustein: NXS_FILE_CONVERT|E2A|UNWRAP\nName aus der Ablaufdefinition",
      ]);
    } finally {
      await abbauen();
    }
    // Der eigentliche Nachweis ist das Ausbleiben eines `console.error` — dafür
    // sorgt `tests/setup/konsole.ts`, und deshalb steht hier keine Zusicherung.
  });

  /**
   * **Kein Schalter und keine Anfrage.** Die Zahl steht im Kopf des
   * Detail-Endpunkts, genau damit diese Entscheidung ohne Zugriff fällt. Anders
   * als der BAM-Block bleibt hier eine Zeile Text stehen: Dass eine Nachricht
   * gar keine Eigenschaft trägt, ist der Ausnahmefall — im gemessenen
   * Tagesfenster hat jede der 6.249 Nachrichten welche (M17 1).
   */
  it("zeigt bei anzahl = 0 nur eine Zeile Text — kein Schalter, keine Anfrage", async () => {
    const { behaelter, abbauen } = await rendere(
      <EigenschaftenBlock messageId={MESSAGE_ID} anzahl={0} schritte={SCHRITTE} />,
    );

    try {
      expect(behaelter.querySelector("button")).toBeNull();
      expect(behaelter.textContent).toBe(EIGENSCHAFTEN.keine);
      expect(behaelter.querySelectorAll("section")).toHaveLength(0);
      expect(anfragen).toEqual([]);
    } finally {
      await abbauen();
    }
  });

  /**
   * Die Gegenprobe: Mit Eigenschaften gibt es den Schalter — und **auch dann
   * noch keine Anfrage**, solange niemand aufklappt. Ohne diesen Test bewiese
   * der vorige nur, dass ein Block ohne Inhalt nichts lädt.
   */
  it("zeigt eingeklappt nur die Überschrift mit der Zahl — ohne zu laden", async () => {
    const { behaelter, abbauen } = await rendere(
      <EigenschaftenBlock messageId={MESSAGE_ID} anzahl={22} schritte={SCHRITTE} />,
    );

    try {
      expect(behaelter.querySelector("button")?.textContent).toBe(
        EIGENSCHAFTEN.titel.replace("{anzahl}", "22"),
      );
      expect(behaelter.querySelector("button")?.getAttribute("aria-expanded")).toBe("false");
      expect(behaelter.querySelectorAll("section")).toHaveLength(0);
      expect(anfragen).toEqual([]);
    } finally {
      await abbauen();
    }
  });

  /**
   * **Ohne `schritte` trägt jede Gruppe den Rückfall.** Der Fall tritt ein,
   * solange das Detail noch nicht da ist; die Einteilung hängt nicht an den
   * Schritten, nur die Beschriftung tut es. **Kein erfundener Name** — die
   * Nummer ist das einzige, was über die Position bekannt ist.
   */
  it("beschriftet ohne schritte jede Gruppe mit dem Rückfall", async () => {
    const zwischenspeicher = neuerZwischenspeicher();
    zwischenspeicher.setQueryData(NACHRICHTEN_SCHLUESSEL.eigenschaften(MESSAGE_ID), [
      eigenschaft("Service.Type", 1, "FileReader"),
      eigenschaft("Service.Type", 2, "Converter"),
    ]);

    const { behaelter, abbauen } = await rendere(
      <EigenschaftenBlock messageId={MESSAGE_ID} anzahl={2} />,
      zwischenspeicher,
    );

    try {
      await klappeAuf(behaelter);

      expect(gruppenkoepfe(behaelter)).toEqual(["Schritt 1 (1)", "Schritt 2 (1)"]);
      // Ohne Schritt gibt es nichts zu erklären — und deshalb keinen Tooltip.
      expect(
        [...behaelter.querySelectorAll("section h3")].map((kopf) => kopf.getAttribute("title")),
      ).toEqual([null, null]);
    } finally {
      await abbauen();
    }
  });
});
