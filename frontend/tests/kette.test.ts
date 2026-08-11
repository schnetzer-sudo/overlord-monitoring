import { describe, expect, it } from "vitest";

import type { Kette, Kettenglied } from "@/features/nachrichten/api";
import {
  abschnittFuer,
  abwaertsAbschnitt,
  gezeigteAbwaertsglieder,
  hatInhalt,
  kettenabschnitte,
  nachladenMoeglich,
} from "@/features/nachrichten/kette";

/**
 * Die Kettenfläche des Detailpanels.
 *
 * Geprüft werden die **Entscheidungen**, nicht das Markup: in welchen Abschnitt
 * ein Glied gehört, welche Überschrift eine Zahl nennen darf und wann es
 * überhaupt einen Block gibt.
 *
 * **Der Kern ist die Einteilung nach der Flussrichtung.** Der Endpunkt liefert
 * `aufwaerts` (Aufstieg) und `abwaerts` (Abstieg); für die Zusammenführung
 * ist das gegenüber dem Datenfluss vertauscht, weil der Aufstieg dort
 * `TargetMessageID` folgt — und die zeigt vom Eingang auf das Ergebnis, also
 * mit dem Fluss. Wer die beiden Listen unverändert beschriftete, schriebe bei
 * jedem Merge-Eingang das Gegenteil dessen hin, was passiert ist.
 */

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
    messageId: "8f3a1c2e-0000-4000-8000-000000000001",
    rollen: ["SPLIT_WURZEL"],
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

const WURZEL = glied({ messageId: "wurzel", ebene: -1, beziehung: "AUFTEILUNG" });
const TEIL = glied({ messageId: "teil", ebene: 1, beziehung: "AUFTEILUNG" });
const ERGEBNIS = glied({ messageId: "ergebnis", ebene: -1, beziehung: "ZUSAMMENFUEHRUNG" });
const EINGANG = glied({ messageId: "eingang", ebene: 1, beziehung: "ZUSAMMENFUEHRUNG" });

describe("Die Einteilung folgt dem Datenfluss, nicht der Richtung der API", () => {
  /**
   * Für die **Aufteilung** decken sich beide: Der Aufstieg führt zur Wurzel, der
   * Abstieg zu den Teilen.
   */
  it("legt die Wurzel in den Abschnitt der Herkunft und die Teile in den des Ergebnisses", () => {
    expect(abschnittFuer(WURZEL)).toBe("kommtVon");
    expect(abschnittFuer(TEIL)).toBe("wurdeZu");
  });

  /**
   * ⚠️ **Für die Zusammenführung ist es vertauscht.** Ein Merge-Eingang findet
   * sein Ergebnis im `aufwaerts` — im Fluss ist das aber, was aus ihm wurde.
   * Ein Merge-Ergebnis findet seine Eingänge im `abwaerts` — im Fluss ist das,
   * woher es kommt. Über Fenster B sind das 38.628 Eingänge und 1.611
   * Ergebnisse (M30‑4): kein Randfall.
   */
  it("dreht die Zusammenführung um — das Ergebnis wurde, die Eingänge kamen", () => {
    expect(abschnittFuer(ERGEBNIS)).toBe("wurdeZu");
    expect(abschnittFuer(EINGANG)).toBe("kommtVon");
  });
});

describe("Die Abschnitte", () => {
  it("nennen die Herkunft zuerst und lassen jeden Abschnitt ohne Inhalt weg", () => {
    const abschnitte = kettenabschnitte(kette({ aufwaerts: [WURZEL], abwaerts: [TEIL] }));

    expect(abschnitte.map((abschnitt) => abschnitt.art)).toEqual(["kommtVon", "wurdeZu"]);
    expect(abschnitte[0].glieder).toEqual([WURZEL]);
    expect(abschnitte[1].glieder).toEqual([TEIL]);
  });

  /**
   * **Ein mehrstufiger Aufstieg bleibt in seiner Reihenfolge** — Ebene `-1`
   * zuerst, wie geliefert. Hier wird nicht ein zweites Mal sortiert.
   */
  it("behalten die gelieferte Reihenfolge: erst der Aufstieg, dann der Abstieg", () => {
    const naeher = glied({ messageId: "e1", ebene: -1, beziehung: "AUFTEILUNG" });
    const ferner = glied({ messageId: "e2", ebene: -2, beziehung: "AUFTEILUNG" });
    const eingang = glied({ messageId: "eingang", ebene: 1, beziehung: "ZUSAMMENFUEHRUNG" });

    const [kommtVon] = kettenabschnitte(
      kette({ aufwaerts: [naeher, ferner], abwaerts: [eingang] }),
    );

    expect(kommtVon.glieder.map((eintrag) => eintrag.messageId)).toEqual(["e1", "e2", "eingang"]);
  });

  /**
   * Ein zickzackender Aufstieg — ein Merge-Ergebnis, das selbst ein Split-Kind
   * ist (33 Zeilen über Fenster B, M30‑4) — verteilt sich auf beide Abschnitte.
   * Das ist der Preis der ehrlichen Einteilung und richtig so: Die eine Stufe
   * sagt „woher", die andere „wohin".
   */
  it("verteilen einen zickzackenden Aufstieg auf beide Abschnitte", () => {
    const ergebnis = glied({ messageId: "ergebnis", ebene: -1, beziehung: "ZUSAMMENFUEHRUNG" });
    const dessenWurzel = glied({ messageId: "wurzel", ebene: -2, beziehung: "AUFTEILUNG" });

    const [kommtVon, wurdeZu] = kettenabschnitte(kette({ aufwaerts: [ergebnis, dessenWurzel] }));

    expect(wurdeZu.glieder.map((eintrag) => eintrag.messageId)).toEqual(["ergebnis"]);
    expect(kommtVon.glieder.map((eintrag) => eintrag.messageId)).toEqual(["wurzel"]);
  });
});

/**
 * **Die Zahl steht nur dort, wo der Endpunkt eine liefert.**
 *
 * `abwaertsGesamt` zählt beide Abwärtsrichtungen zusammen. Verteilen sie sich
 * auf beide Abschnitte, ließe sich die Summe keiner der beiden Überschriften
 * zuordnen, ohne sie zu erfinden.
 */
describe("Die Zahl in der Überschrift", () => {
  it("steht am Abschnitt, der die Abwärtsglieder trägt", () => {
    const [kommtVon, wurdeZu] = kettenabschnitte(
      kette({ aufwaerts: [WURZEL], abwaerts: [TEIL], abwaertsGesamt: 3350 }),
    );

    expect(wurdeZu.gesamt).toBe(3350);
    expect(kommtVon.gesamt).toBeNull();
  });

  it("steht beim Merge-Ergebnis am Abschnitt der Herkunft", () => {
    const [kommtVon, wurdeZu] = kettenabschnitte(
      kette({ abwaerts: [EINGANG], abwaertsGesamt: 897 }),
    );

    expect(kommtVon.gesamt).toBe(897);
    expect(wurdeZu.gesamt).toBeNull();
  });

  /**
   * 25 Zeilen sind zugleich Split-Wurzel und Merge-Ergebnis (M30‑4). Ihre
   * Abwärtsglieder verteilen sich, und dann behauptet keine Überschrift eine
   * Zahl.
   */
  it("fehlt, wenn die Abwärtsglieder sich auf beide Abschnitte verteilen", () => {
    const abschnitte = kettenabschnitte(kette({ abwaerts: [TEIL, EINGANG], abwaertsGesamt: 12 }));

    expect(abschnitte.map((abschnitt) => abschnitt.gesamt)).toEqual([null, null]);
    expect(abwaertsAbschnitt([TEIL, EINGANG])).toBeNull();
  });

  /** Ohne Abwärtsglieder gibt es nichts zuzuordnen — und nichts zu blättern. */
  it("fehlt ohne Abwärtsglieder", () => {
    expect(abwaertsAbschnitt([])).toBeNull();
    expect(
      kettenabschnitte(kette({ aufwaerts: [ERGEBNIS] })).map((abschnitt) => abschnitt.gesamt),
    ).toEqual([null, null]);
  });

  /**
   * Nach dem Nachladen zeigt der Block mehr Zeilen. Die Gesamtzahl bleibt die
   * aus `/kette` — sie ist gezählt und nicht abgeleitet.
   */
  it("bleibt beim Nachladen die gezählte und nicht die gezeigte", () => {
    const geladen = kette({ abwaerts: [TEIL], abwaertsGesamt: 3350, weitereVorhanden: true });
    const gezeigt = gezeigteAbwaertsglieder(geladen, [[glied({ messageId: "teil-2" })]]);
    const [, wurdeZu] = kettenabschnitte(geladen, gezeigt);

    expect(wurdeZu.glieder).toHaveLength(2);
    expect(wurdeZu.gesamt).toBe(3350);
  });
});

/**
 * **Der erste Klick auf „Mehr laden" kostet eine Anfrage, nicht zwei.**
 *
 * `/kette` liefert seit dem 11.08.2026 mit `abwaertsCursor` die Position hinter
 * der Seite, die schon dasteht. Vorher gab es die nicht: Der Block holte die
 * erste Seite ein zweites Mal, um an einen Cursor zu kommen, verwarf dafür die
 * gezeigten fünfzig und zog die zweite Seite sofort nach.
 *
 * Was hier geprüft wird, ist die Entscheidung dahinter — die Anfrage selbst
 * stellt TanStack Query, und gerendert wird in diesen Tests nichts
 * (`frontend-grundlagen.md` §9).
 */
describe("Das Nachladen", () => {
  const ERSTE_SEITE = Array.from({ length: 50 }, (_, nummer) =>
    glied({ messageId: `teil-${nummer}` }),
  );
  const ZWEITE_SEITE = Array.from({ length: 50 }, (_, nummer) =>
    glied({ messageId: `teil-${50 + nummer}` }),
  );

  const BREITE_WURZEL = kette({
    abwaerts: ERSTE_SEITE,
    abwaertsGesamt: 169,
    abwaertsCursor: "eyJ0Ijoi…",
    weitereVorhanden: true,
  });

  /**
   * **50 → 100 durch eine einzige Seite.** Der Umweg hätte für dieselben 100
   * Zeilen zwei Anfragen gebraucht; hätte er die erste Seite *zusätzlich*
   * angehängt, stünden hier 150 und fünfzig davon doppelt.
   */
  it("hängt die geholte Seite an die gezeigte an, statt sie zu ersetzen", () => {
    expect(gezeigteAbwaertsglieder(BREITE_WURZEL, [])).toHaveLength(50);

    const nachEinemKlick = gezeigteAbwaertsglieder(BREITE_WURZEL, [ZWEITE_SEITE]);

    expect(nachEinemKlick).toHaveLength(100);
    // Keine Zeile doppelt: Die geholte Seite setzt hinter dem Cursor an.
    expect(new Set(nachEinemKlick.map((eintrag) => eintrag.messageId)).size).toBe(100);
    expect(nachEinemKlick[0]).toBe(ERSTE_SEITE[0]);
    expect(nachEinemKlick.at(-1)).toBe(ZWEITE_SEITE.at(-1));
  });

  it("zeigt die Schaltfläche, solange es weitergeht", () => {
    expect(nachladenMoeglich(BREITE_WURZEL)).toBe(true);
    expect(nachladenMoeglich(kette({ abwaerts: [TEIL], abwaertsGesamt: 1 }))).toBe(false);
  });

  /**
   * **Der Sonderfall aus M30‑6.** Trägt die letzte gelieferte Zeile keinen
   * Zeitpunkt, hat sie im Sortierschlüssel keine Position, und der Endpunkt
   * lässt den Cursor weg — auch wenn es weitergeht. Eine Schaltfläche ohne Ziel
   * wäre eine Zusage, die niemand einlöst. Gemessen kommt das 0 von 3.341.519
   * Mal vor.
   */
  it("fehlt ohne Cursor, auch wenn es weitergeht", () => {
    const ohneCursor = kette({
      abwaerts: ERSTE_SEITE,
      abwaertsGesamt: 169,
      abwaertsCursor: null,
      weitereVorhanden: true,
    });

    expect(nachladenMoeglich(ohneCursor)).toBe(false);
  });
});

/**
 * **Ohne Inhalt gibt es keinen Block.** Eine Fläche ohne Inhalt behauptet, es
 * gäbe dort etwas zu sehen — dieselbe Regel wie beim Eigenschaftenblock.
 */
describe("Ob es einen Block gibt", () => {
  it("verneint eine Kette ohne Glieder und ohne Hinweis", () => {
    expect(hatInhalt(kette())).toBe(false);
  });

  it("bejaht sie, sobald ein Glied da ist", () => {
    expect(hatInhalt(kette({ aufwaerts: [WURZEL] }))).toBe(true);
    expect(hatInhalt(kette({ abwaerts: [TEIL] }))).toBe(true);
  });

  /**
   * Auch ein Abbruch ohne ein einziges Glied ist etwas, das gesagt werden muss.
   * Ein Zyklus kann schon auf der ersten Stufe greifen — ein Selbstverweis; über
   * 3,34 Millionen Zeilen gemessen null Mal (M30‑2), die Spalte lässt es zu.
   */
  it("bejaht sie auch, wenn nur ein Abbruch zu melden ist", () => {
    expect(hatInhalt(kette({ tiefeErreicht: true }))).toBe(true);
    expect(hatInhalt(kette({ zyklusErkannt: true }))).toBe(true);
  });
});
