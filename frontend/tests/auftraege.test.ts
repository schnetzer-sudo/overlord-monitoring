import { describe, expect, it } from "vitest";

import type { Auftrag, Auftragseintrag } from "@/features/dashboard/api";
import {
  BLOCK_ANFANG,
  SICHTBARE_AUFTRAEGE,
  beschrifteteWerte,
  blockSichtbar,
  blockschritt,
  einordnungVon,
  ergebnisVon,
  laedtNeuNach,
  loeschbar,
  loeschergebnis,
  mehrereTypen,
  nichtErfolgreich,
  schluesselZugeklappt,
  sichtbare,
  weitere,
  type Blockzustand,
} from "@/features/dashboard/auftraege";
import { texteFuer, SPRACHEN } from "@/i18n";
import { ProblemFehler } from "@/lib/http";
import {
  AUFTRAGSEINORDNUNGEN,
  ZEILENERGEBNISSE,
  auftragVordergrund,
  auftragsrolle,
  ergebnisFlaeche,
  ergebnisrolle,
} from "@/lib/status-farbe";

/**
 * **Die Regeln des Blocks „Ihre Aufträge“ als reine Funktionen** (Linie
 * `assistent`, A4, `docs/assistent-frontend.md` §12). Was nur der Baum belegt —
 * wo der Block steht, was im Dokument ankommt, wohin der Fokus geht —, steht in
 * `tests/auftraege-block.test.tsx`.
 */

function eintrag(teil: Partial<Auftragseintrag> = {}): Auftragseintrag {
  return { werte: {}, schluessel: null, rueckgabe: null, ergebnis: null, ...teil };
}

function auftrag(teil: Partial<Auftrag> = {}): Auftrag {
  return {
    taskId: "t-1",
    typ: "WERK_ABLADESTELLE",
    name: "Anpassen der Werk-/Abladestellentabelle",
    aktion: "Neuanlage",
    status: "OFFEN",
    einordnung: "OFFEN",
    eingestelltAm: "2026-10-19T12:32:10Z",
    abgeholtAm: null,
    ausgefuehrtAm: null,
    fehlercode: null,
    fehlertext: null,
    eintraege: [],
    felder: [],
    ...teil,
  };
}

describe("Einordnung und Ergebnis kommen aus dem Backend", () => {
  it.each(AUFTRAGSEINORDNUNGEN)("%s wird so übernommen, wie er kommt", (einordnung) => {
    expect(einordnungVon({ einordnung })).toBe(einordnung);
  });

  /** Regel Q4: Ein Wert, den das Frontend nicht kennt, bekommt kein geratenes Wort. */
  it("liest eine unbekannte Einordnung als UNBEKANNT", () => {
    expect(einordnungVon({ einordnung: "ZURUECKGESTELLT" as never })).toBe("UNBEKANNT");
  });

  it("liest ein unbekanntes Ergebnis als UNBEKANNT und null als null", () => {
    expect(ergebnisVon({ ergebnis: null })).toBeNull();
    for (const ergebnis of ZEILENERGEBNISSE) {
      expect(ergebnisVon({ ergebnis })).toBe(ergebnis);
    }
    expect(ergebnisVon({ ergebnis: "VIELLEICHT" as never })).toBe("UNBEKANNT");
  });
});

describe("Sichtbar sind fünf, der Rest hinter „weitere anzeigen“ (E‑1101)", () => {
  const sechs = ["a", "b", "c", "d", "e", "f"];

  it("zeigt fünf von sechs und nennt einen weiteren", () => {
    expect(SICHTBARE_AUFTRAEGE).toBe(5);
    expect(sichtbare(sechs, false, SICHTBARE_AUFTRAEGE)).toEqual(["a", "b", "c", "d", "e"]);
    expect(weitere(sechs, SICHTBARE_AUFTRAEGE)).toBe(1);
  });

  it("zeigt alle, wenn sie aufgeklappt sind, und bei fünf gibt es keine weiteren", () => {
    expect(sichtbare(sechs, true, SICHTBARE_AUFTRAEGE)).toEqual(sechs);
    expect(weitere(sechs.slice(0, 5), SICHTBARE_AUFTRAEGE)).toBe(0);
  });
});

describe("Die zugeklappte Zeile (E‑1102, E‑1103)", () => {
  const rueckfall = (nummer: number) => `Eintrag ${nummer}`;

  it("nennt die ersten zwei Schlüssel und zählt den Rest", () => {
    const eintraege = ["A/1", "B/2", "C/3", "D/4"].map((schluessel) => eintrag({ schluessel }));
    expect(schluesselZugeklappt(eintraege, rueckfall)).toEqual({
      schluessel: ["A/1", "B/2"],
      weitere: 2,
    });
    expect(schluesselZugeklappt(eintraege.slice(0, 2), rueckfall).weitere).toBe(0);
    expect(schluesselZugeklappt(eintraege.slice(0, 1), rueckfall).schluessel).toEqual(["A/1"]);
  });

  /** F23: Ohne Schlüssel steht „Eintrag <n>“ — gezählt ab 1, wie auf der Karte. */
  it("setzt ohne Schlüssel den Rückfall mit der Nummer des Eintrags", () => {
    const eintraege = [eintrag({ schluessel: null }), eintrag({ schluessel: "B/2" })];
    expect(schluesselZugeklappt(eintraege, rueckfall).schluessel).toEqual(["Eintrag 1", "B/2"]);
  });

  it("erkennt mehrere Typen über die ganze Liste, nicht über die sichtbaren", () => {
    const fuenfGleiche = Array.from({ length: 5 }, () => ({ typ: "A" }));
    expect(mehrereTypen(fuenfGleiche)).toBe(false);
    expect(mehrereTypen([...fuenfGleiche, { typ: "B" }])).toBe(true);
  });

  /** Gezählt wird jeder, der nicht OK ist — UNBEKANNT zählt mit, wie im Backend. */
  it("zählt die nicht erfolgreichen Einträge, auch n von n", () => {
    const gemischt = [
      eintrag({ ergebnis: "OK" }),
      eintrag({ ergebnis: "NICHT_OK" }),
      eintrag({ ergebnis: "UNBEKANNT" }),
    ];
    expect(nichtErfolgreich(gemischt)).toBe(2);
    expect(
      nichtErfolgreich([eintrag({ ergebnis: "NICHT_OK" }), eintrag({ ergebnis: "NICHT_OK" })]),
    ).toBe(2);
    expect(nichtErfolgreich([eintrag({ ergebnis: "OK" })])).toBe(0);
  });
});

describe("Die Werte mit ihrem label (E‑1104, E‑1120, F21)", () => {
  const felder = [
    { name: "werk", label: "Werk" },
    { name: "abladestelle", label: "Abladestelle" },
    { name: "sndprn", label: "SNDPRN" },
  ];

  it("ordnet nach der Definition und lässt Felder ohne Wert weg", () => {
    expect(beschrifteteWerte(felder, { sndprn: "120764", werk: "72332" })).toEqual([
      { name: "werk", label: "Werk", wert: "72332" },
      { name: "sndprn", label: "SNDPRN", wert: "120764" },
    ]);
  });

  /** Regel Q4: Was der Auftrag trägt, wird gezeigt — notfalls mit seinem Namen. */
  it("zeigt einen Wert, den felder nicht kennt, mit seinem Namen am Ende", () => {
    expect(beschrifteteWerte(felder, { neu: "x", werk: "1" })).toEqual([
      { name: "werk", label: "Werk", wert: "1" },
      { name: "neu", label: "neu", wert: "x" },
    ]);
  });
});

describe("Löschbar ist nur OFFEN (E‑1110)", () => {
  it.each(AUFTRAGSEINORDNUNGEN)("%s", (einordnung) => {
    expect(loeschbar(auftrag({ einordnung }))).toBe(einordnung === "OFFEN");
  });
});

describe("Wie ein Löschen ausgeht (E‑1116, F22)", () => {
  it("erkennt 409 am Problemtyp, 404 am Status und alles andere als Störung", () => {
    expect(loeschergebnis(new ProblemFehler({ status: 409, typ: "auftrag-nicht-offen" }))).toBe(
      "uebernommen",
    );
    expect(loeschergebnis(new ProblemFehler({ status: 404, typ: "nicht-gefunden" }))).toBe(
      "verschwunden",
    );
    // Ein anderes 409 ist nicht „die Plattform hat übernommen“.
    expect(loeschergebnis(new ProblemFehler({ status: 409, typ: "anderer-konflikt" }))).toBe(
      "gestoert",
    );
    expect(loeschergebnis(new ProblemFehler({ status: 0, typ: "netzwerk" }))).toBe("gestoert");
    expect(loeschergebnis(new ProblemFehler({ status: 503, typ: "technischer-fehler" }))).toBe(
      "gestoert",
    );
    expect(loeschergebnis(new Error("irgendwas"))).toBe("gestoert");
  });

  it("lädt nach 204, 409 und 404 neu, nach einer Störung nicht", () => {
    expect(laedtNeuNach("geloescht")).toBe(true);
    expect(laedtNeuNach("uebernommen")).toBe(true);
    expect(laedtNeuNach("verschwunden")).toBe(true);
    expect(laedtNeuNach("gestoert")).toBe(false);
  });
});

describe("Der Zustand des Blocks (F20, F22)", () => {
  const schritte = (...folge: Parameters<typeof blockschritt>[1][]): Blockzustand =>
    folge.reduce(blockschritt, BLOCK_ANFANG);

  it("klappt je Zeile auf und zu", () => {
    expect(schritte({ art: "umschalten", taskId: "a" }).offen).toEqual(["a"]);
    expect(
      schritte({ art: "umschalten", taskId: "a" }, { art: "umschalten", taskId: "b" }).offen,
    ).toEqual(["a", "b"]);
    expect(
      schritte({ art: "umschalten", taskId: "a" }, { art: "umschalten", taskId: "a" }).offen,
    ).toEqual([]);
  });

  it("setzt nach dem Löschen den Satz oben, zählt ihn und schließt Rückfrage und Zeile", () => {
    const zustand = schritte(
      { art: "umschalten", taskId: "a" },
      { art: "frage", taskId: "a" },
      { art: "loeschenBegonnen", taskId: "a" },
      { art: "loeschenBeendet", taskId: "a", ergebnis: "geloescht" },
    );
    expect(zustand.oben).toBe("geloescht");
    expect(zustand.obenNummer).toBe(1);
    expect(zustand.frage).toBeNull();
    expect(zustand.offen).toEqual([]);

    // Derselbe Satz ein zweites Mal ist eine neue Nummer — sonst bekäme er den Fokus nicht.
    const nochmal = blockschritt(zustand, {
      art: "loeschenBeendet",
      taskId: "b",
      ergebnis: "geloescht",
    });
    expect(nochmal.obenNummer).toBe(2);
  });

  it("setzt 409 und Störung in die Zeile, schließt die Rückfrage und lässt die Zeile offen", () => {
    const zustand = schritte(
      { art: "umschalten", taskId: "a" },
      { art: "frage", taskId: "a" },
      { art: "loeschenBegonnen", taskId: "a" },
      { art: "loeschenBeendet", taskId: "a", ergebnis: "uebernommen" },
    );
    expect(zustand.zeilen).toEqual({ a: "uebernommen" });
    expect(zustand.oben).toBeNull();
    expect(zustand.frage).toBeNull();
    expect(zustand.offen).toEqual(["a"]);
  });

  it("nimmt mit einem neuen Versuch die Sätze des alten weg", () => {
    const zustand = schritte(
      { art: "loeschenBeendet", taskId: "a", ergebnis: "gestoert" },
      { art: "loeschenBeendet", taskId: "b", ergebnis: "verschwunden" },
      { art: "loeschenBegonnen", taskId: "a" },
    );
    expect(zustand.zeilen).toEqual({});
    expect(zustand.oben).toBeNull();
  });

  /** F22 und F20: „Neu laden“ nimmt die Sätze weg und lässt Aufgeklapptes stehen. */
  it("„Neu laden“ nimmt alle Sätze weg und lässt die offenen Zeilen offen", () => {
    const zustand = schritte(
      { art: "umschalten", taskId: "a" },
      { art: "alleEintraege", taskId: "a", an: true },
      { art: "loeschenBeendet", taskId: "b", ergebnis: "geloescht" },
      { art: "loeschenBeendet", taskId: "a", ergebnis: "uebernommen" },
      { art: "neuLaden" },
    );
    expect(zustand.oben).toBeNull();
    expect(zustand.zeilen).toEqual({});
    expect(zustand.offen).toEqual(["a"]);
    expect(zustand.alleEintraege).toEqual(["a"]);
  });
});

describe("Ob der Block steht (E‑1098, F18, F22)", () => {
  it("steht beim ersten Laden nicht — kein Platzhalter", () => {
    expect(blockSichtbar("pending", 0, null)).toBe(false);
  });

  it("steht bei einem Fehler, mit Satz und „Erneut versuchen“", () => {
    expect(blockSichtbar("error", 0, null)).toBe(true);
  });

  it("steht mit mindestens einem Auftrag und fehlt ohne", () => {
    expect(blockSichtbar("success", 1, null)).toBe(true);
    expect(blockSichtbar("success", 0, null)).toBe(false);
  });

  it("bleibt nach dem Löschen des letzten Auftrags für den Satz stehen", () => {
    expect(blockSichtbar("success", 0, "geloescht")).toBe(true);
    expect(blockSichtbar("success", 0, "verschwunden")).toBe(true);
  });
});

describe("Die Farbrollen der Stände (F16, F17)", () => {
  it("gibt jedem Stand eine Rolle, und Teilweise erledigt die eigene", () => {
    expect(Object.fromEntries(AUFTRAGSEINORDNUNGEN.map((e) => [e, auftragsrolle(e)]))).toEqual({
      OFFEN: "offen",
      IN_AUSFUEHRUNG: "offen",
      ERLEDIGT: "abgeschlossen",
      TEILWEISE_ERLEDIGT: "teilweise",
      UNSTIMMIG: "ungeklaert",
      FEHLGESCHLAGEN: "fehler",
      UNBEKANNT: "ungeklaert",
    });
  });

  /**
   * **Nicht `ueberfaellig`** — die Rolle bleibt der Kategorie Überfällig
   * vorbehalten (E‑71, E‑77), und keine Klasse eines Auftrags nennt sie.
   */
  it("nennt in keiner Klasse eines Stands oder Ergebnisses die Rolle ueberfaellig", () => {
    for (const einordnung of AUFTRAGSEINORDNUNGEN) {
      expect(auftragVordergrund(einordnung)).not.toContain("ueberfaellig");
    }
    for (const ergebnis of ZEILENERGEBNISSE) {
      expect(ergebnisFlaeche(ergebnis)).not.toContain("ueberfaellig");
    }
    expect(auftragVordergrund("TEILWEISE_ERLEDIGT")).toBe("text-teilweise");
  });

  it("tönt nur den nicht erfolgreichen Eintrag", () => {
    expect(ergebnisFlaeche("OK")).toBe("");
    expect(ergebnisFlaeche("NICHT_OK")).toBe("bg-teilweise-flaeche");
    expect(ergebnisFlaeche("UNBEKANNT")).toBe("bg-teilweise-flaeche");
    expect(ergebnisrolle("OK")).toBe("abgeschlossen");
  });
});

describe("Die Wörter stehen in beiden Sprachen", () => {
  it.each(SPRACHEN)(
    "%s: ein Wort je Stand außer UNBEKANNT, ein Hinweis, wo die Tabelle einen hat",
    (sprache) => {
      const t = texteFuer(sprache).dashboard.auftraege;
      for (const einordnung of AUFTRAGSEINORDNUNGEN) {
        if (einordnung !== "UNBEKANNT") {
          expect(t.stand[einordnung].length, einordnung).toBeGreaterThan(0);
        }
      }
      expect(Object.keys(t.hinweis).sort()).toEqual(
        ["IN_AUSFUEHRUNG", "OFFEN", "UNBEKANNT", "UNSTIMMIG"].sort(),
      );
    },
  );

  /** Der Wortlaut ist vorgegeben (E‑1112, E‑1116, §12.4) — wörtlich, und gesiezt (E‑1124). */
  it("trägt den vorgegebenen deutschen Wortlaut", () => {
    const t = texteFuer("de").dashboard.auftraege;
    expect(t.titel).toBe("Ihre Aufträge");
    expect(t.frage).toBe(
      "Auftrag löschen? Die Plattform hat ihn noch nicht abgeholt. Er wird nicht ausgeführt.",
    );
    expect(t.geloescht).toBe("Auftrag gelöscht. Er wird nicht ausgeführt.");
    expect(t.uebernommen).toBe(
      "Die Plattform hat den Auftrag gerade übernommen. Löschen geht nicht mehr.",
    );
    expect(t.verschwunden).toBe("Der Auftrag ist nicht mehr vorhanden.");
    expect(t.hinweis.OFFEN).toBe(
      "Wartet auf die Plattform. Bis sie ihn abholt, können Sie ihn noch löschen.",
    );
    expect(t.stand.UNSTIMMIG).toBe("Rückmeldung unvollständig");
  });
});
