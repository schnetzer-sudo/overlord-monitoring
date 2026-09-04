// @vitest-environment jsdom

import { describe, expect, it } from "vitest";

import { texteFuer } from "@/i18n";
import {
  NACHRICHTEN_SCHLUESSEL,
  type Kette,
  type Kettenglied,
  type Nachrichtendetail,
} from "@/features/nachrichten/api";
import { KettenBlock } from "@/features/nachrichten/components/kette-block";
import { NachrichtDetail } from "@/features/nachrichten/components/nachricht-detail";

import { neuerZwischenspeicher, rendere } from "./hilfe/rendern";

/**
 * **Die drei Fälle, für die ein gerenderter Baum die einzige Prüfung ist.**
 *
 * Alles andere an dieser Ansicht ist Einteilung und Abzählung und steht als
 * reine Funktion in `tests/kette.test.ts` und `tests/nachrichtendetail.test.ts`
 * — geprüft werden Entscheidungen, nicht Markup
 * (`docs/frontend-grundlagen.md` §9). Diese Datei ist die begründete Ausnahme,
 * und sie bleibt bei drei Tests:
 *
 * | Test | Warum genau dieser |
 * |---|---|
 * | `tiefeErreicht` | Die tiefste Kette der Testkopie hat vier Glieder (M30‑3); die Grenze von zehn spricht dort nie an. Der Satz ist **von Hand nicht zu sehen** |
 * | `zyklusErkannt` | Null Zyklen und null Selbstverweise über 3,34 Mio. Zeilen (M30‑2, M30‑3). Auch dieser Satz ist von Hand nie zu sehen |
 * | Doppelschlüssel | Die Regression zum Befund vom 11.08.2026 (`docs/verkettung.md` §8.12). Sie besteht, wenn **kein `console.error`** fällt — und hängt damit an `tests/setup/konsole.ts` |
 *
 * Die beiden ersten prüfen zusätzlich **wo** der Satz steht: unter *beiden*
 * Abschnitten und nicht an einem (`docs/verkettung.md` §8.5). Der Aufstieg kann
 * über beide hinweg verlaufen — ein Merge-Ergebnis, das selbst ein Split-Kind
 * ist, hat Glieder in beiden (390 Ansichten über Fenster B, M31‑1) —, und ein
 * Satz an einem Abschnitt hinge dann am falschen. Das ist der Teil der Regel,
 * der beim Umbauen als Erstes verloren geht, und deshalb steht er hier.
 *
 * **Antwortrümpfe gestellt, kein Netz, keine Datenbank.**
 */

const TEXTE = texteFuer("de");
const KETTE = TEXTE.nachrichten.kette;

const MESSAGE_ID = "8f3a1c2e-0000-4000-8000-000000000001";

function glied(werte: Partial<Kettenglied> & { messageId: string }): Kettenglied {
  return {
    status: "FINISHED",
    statusKind: "ABGESCHLOSSEN",
    zeitpunkt: "2025-12-29T10:01:09Z",
    sosName: "Versand Einzel IDOC aus Split",
    rollen: [],
    ebene: 1,
    beziehung: "AUFTEILUNG",
    ...werte,
  };
}

function kette(werte: Partial<Kette> = {}): Kette {
  return {
    messageId: MESSAGE_ID,
    rollen: ["SPLIT_KIND", "MERGE_ERGEBNIS"],
    aufwaerts: [],
    abwaerts: [],
    abwaertsGesamt: 0,
    abwaertsCursor: null,
    weitereVorhanden: false,
    tiefeErreicht: false,
    zyklusErkannt: false,
    ...werte,
  };
}

function detail(werte: Partial<Nachrichtendetail> = {}): Nachrichtendetail {
  return {
    messageId: MESSAGE_ID,
    status: "FINISHED",
    statusKind: "ABGESCHLOSSEN",
    processId: "P-0815",
    processName: "Versand Einzel IDOC",
    projectName: "Versand",
    sosName: "Versand Einzel IDOC aus Split",
    rollen: ["SPLIT_KIND", "MERGE_ERGEBNIS"],
    zeitpunkt: "2025-12-29T10:01:09Z",
    start: "2025-12-29T10:00:00Z",
    gesamtdauerSekunden: 69,
    fristSekunden: null,
    eigenschaftenAnzahl: 31,
    // Ohne Belegdaten: Der BAM-Block ist dann nicht im Baum, und dieser Test
    // sagt nichts ueber ihn — er hat seinen eigenen (`tests/bam-block.test.tsx`).
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
 * **Der zickzackende Aufstieg** — genau die Gestalt, die einen Abbruchsatz auf
 * beide Abschnitte laufen lässt: Stufe −1 ist das Merge-Ergebnis („Wurde zu"),
 * Stufe −2 dessen Wurzel („Kommt von").
 */
const ERGEBNIS = glied({ messageId: "ergebnis", ebene: -1, beziehung: "ZUSAMMENFUEHRUNG" });
const DESSEN_WURZEL = glied({ messageId: "wurzel", ebene: -2, beziehung: "AUFTEILUNG" });

/** Der Baum, den der Block rendert — mit gestellter Antwort auf `/kette`. */
async function rendereKette(daten: Kette) {
  const zwischenspeicher = neuerZwischenspeicher();
  zwischenspeicher.setQueryData(NACHRICHTEN_SCHLUESSEL.kette(MESSAGE_ID), daten);
  return rendere(
    <KettenBlock detail={detail({ rollen: daten.rollen })} aufOeffnen={() => undefined} />,
    zwischenspeicher,
  );
}

/** Der eine Absatz mit diesem Satz. Mehr als einer wäre schon der Befund. */
function hinweis(behaelter: HTMLElement, satz: string): HTMLParagraphElement {
  const treffer = [...behaelter.querySelectorAll("p")].filter(
    (absatz) => absatz.textContent === satz,
  );
  expect(treffer).toHaveLength(1);
  return treffer[0];
}

/**
 * Der Satz steht **unter beiden** Abschnitten: in keinem drin, und im Baum
 * hinter beiden.
 */
function stehtUnterBeidenAbschnitten(behaelter: HTMLElement, satz: string): void {
  const abschnitte = [...behaelter.querySelectorAll("section")];
  expect(abschnitte.map((abschnitt) => abschnitt.querySelector("h3")?.textContent)).toEqual([
    KETTE.kommtVon.titel,
    KETTE.wurdeZu.titel,
  ]);

  const absatz = hinweis(behaelter, satz);
  expect(absatz.closest("section")).toBeNull();
  for (const abschnitt of abschnitte) {
    expect(abschnitt.compareDocumentPosition(absatz) & Node.DOCUMENT_POSITION_FOLLOWING).not.toBe(
      0,
    );
  }
}

describe("Der Abbruch der Kette wird gesagt, nicht verschwiegen", () => {
  /**
   * **Von Hand nicht erreichbar.** Die tiefste Kette der Testkopie hat vier
   * Glieder gegen zehn erlaubte, und Stufe fünf ist über alle 214.330
   * Startzeilen aus Fenster B leer (M30‑3). Die Grenze spricht dort nie an —
   * der Satz *„Die Kette ist länger als hier gezeigt"* ist ausschließlich hier
   * belegbar.
   */
  it("zeigt bei `tiefeErreicht` seinen Satz unter beiden Abschnitten", async () => {
    const { behaelter, abbauen } = await rendereKette(
      kette({ aufwaerts: [ERGEBNIS, DESSEN_WURZEL], tiefeErreicht: true }),
    );

    try {
      stehtUnterBeidenAbschnitten(behaelter, KETTE.tiefeErreicht);
      // Zwei Sätze und nicht einer: „tief" beschreibt, wo abgebrochen wurde,
      // „im Kreis" warum. Der andere darf hier nicht mitkommen.
      expect(behaelter.textContent).not.toContain(KETTE.zyklusErkannt);
    } finally {
      await abbauen();
    }
  });

  /**
   * **Ebenfalls von Hand nicht erreichbar.** Über 3,34 Millionen Zeilen gibt es
   * keinen Zyklus und keinen Selbstverweis (M30‑2, M30‑3). Der Schutz ist
   * trotzdem gebaut: Die Kette entsteht durch Datenbank-Events, die uns nicht
   * gehören, und was heute keinen Kreis bildet, muss morgen keinen bilden.
   */
  it("zeigt bei `zyklusErkannt` seinen Satz unter beiden Abschnitten", async () => {
    const zickzack = await rendereKette(
      kette({ aufwaerts: [ERGEBNIS, DESSEN_WURZEL], zyklusErkannt: true }),
    );

    try {
      stehtUnterBeidenAbschnitten(zickzack.behaelter, KETTE.zyklusErkannt);
      expect(zickzack.behaelter.textContent).not.toContain(KETTE.tiefeErreicht);
    } finally {
      await zickzack.abbauen();
    }

    // Und derselbe Fall ohne ein einziges Glied: Ein **Selbstverweis** bräche
    // den Aufstieg schon auf der ersten Stufe. Der Block erscheint trotzdem —
    // sonst bräche die Kette stillschweigend ab, und das ist schlimmer als ein
    // Abbruch, der sich meldet (`docs/verkettung.md` §8.5).
    const nurAbbruch = await rendereKette(kette({ zyklusErkannt: true }));

    try {
      expect(nurAbbruch.behaelter.querySelectorAll("section")).toHaveLength(0);
      expect(hinweis(nurAbbruch.behaelter, KETTE.zyklusErkannt).closest("section")).toBeNull();
    } finally {
      await nurAbbruch.abbauen();
    }
  });
});

/**
 * **Die Regression zum Befund vom 11.08.2026** (`docs/verkettung.md` §8.12).
 *
 * Ketten- und Eigenschaftenblock stehen im selben Elternteil und tragen beide
 * einen `key` aus derselben `MessageID`. Trügen sie ihn **blank**, wäre das für
 * React derselbe Platz im Baum, und die Konsole meldete *„Encountered two
 * children with the same key"*. Sichtbar falsch ist dabei nichts — deshalb
 * besteht dieser Test genau dann, wenn **kein `console.error`** fällt.
 *
 * Er hängt damit vollständig an `tests/setup/konsole.ts` und wäre ohne die
 * Datei wertlos. Nachgewiesen ist das durch den Selbstversuch: Mit
 * zurückgebautem Präfix wird der Lauf rot.
 */
describe("Das Detailpanel", () => {
  it("hängt Ketten- und Eigenschaftenblock ohne doppelten React-Schlüssel ein", async () => {
    const zwischenspeicher = neuerZwischenspeicher();
    const daten = detail();
    zwischenspeicher.setQueryData(NACHRICHTEN_SCHLUESSEL.detail(MESSAGE_ID), daten);
    zwischenspeicher.setQueryData(
      NACHRICHTEN_SCHLUESSEL.kette(MESSAGE_ID),
      kette({ abwaerts: [glied({ messageId: "teil" })], abwaertsGesamt: 1 }),
    );

    const { behaelter, abbauen } = await rendere(
      <NachrichtDetail
        messageId={MESSAGE_ID}
        aufSchliessen={() => undefined}
        schliessenText={TEXTE.nachrichten.detail.schliessen}
        aufOeffnen={() => undefined}
        umschaltenZu="ohneListe"
        aufUmschalten={() => undefined}
      />,
      zwischenspeicher,
    );

    try {
      // Beide Blöcke sind wirklich da — ohne das wäre die Konsolenprüfung
      // oben eine Aussage über einen Baum, den es nicht gibt.
      expect(behaelter.querySelector("section section h3")?.textContent).toBe(
        KETTE.wurdeZu.titelEins,
      );
      expect(behaelter.textContent).toContain(
        `Technische Eigenschaften (${daten.eigenschaftenAnzahl})`,
      );
    } finally {
      await abbauen();
    }
  });
});
