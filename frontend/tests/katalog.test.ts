import { describe, expect, it } from "vitest";

import type { Katalogzeile, Vorschlagsuebernahme } from "@/features/katalog/api";
import {
  einLaufHatStattgefunden,
  fortschritt,
  hinweisNoetig,
  ohneJedenPartnervorschlag,
  projekteAus,
} from "@/features/katalog/kennzahlen";
import {
  OHNE_RICHTUNG,
  alsAnfrage,
  darfOeffnen,
  entwurfAus,
  massenwert,
  mitAktualisierterZeile,
  mitPartner,
  passendeVorschlaege,
  speichertOhnePartner,
} from "@/features/katalog/zuordnung";
import { darfUebernehmen, uebernahmesaetze } from "@/features/katalog/uebernahme";
import { de } from "@/i18n/de";
import { en } from "@/i18n/en";
import { fehleranzeige } from "@/lib/fehlertext";
import { ProblemFehler, istKeinZugriff } from "@/lib/http";

/**
 * Die Entscheidungen der Katalogpflege, die keine Ansicht brauchen.
 *
 * Vier Gruppen, und jede beantwortet eine Frage, die falsch zu beantworten still
 * wehtut: Was heißt ein leeres Feld? Wann darf eine zweite Zeile auf? Über
 * welcher Menge wird gezählt? Und welcher der drei `403` ist gemeint?
 */

function zeile(werte: Partial<Katalogzeile> & { processId: string }): Katalogzeile {
  return {
    projectId: "300_KundenEingehend",
    projectName: "Kunden eingehend",
    processName: null,
    partner: null,
    richtung: null,
    pflegestatus: "OFFEN",
    vorschlagHerkunft: "KEINE",
    traegtNachrichten: null,
    bestandGeprueftAm: null,
    ...werte,
  };
}

describe("Der leere Partner ist eine Angabe", () => {
  it("wird als `null` geschickt und nicht als leere Zeichenkette", () => {
    // `null` heißt leer und nicht „unverändert": Der Endpunkt setzt die Zeile
    // als Ganzes und macht sie `GEPFLEGT`. Ohne die Speicherbarkeit des leeren
    // Werts stünden Auffangprozesse dauerhaft auf „offen", und der Fortschritt
    // erreichte nie sein Ende (E4).
    expect(alsAnfrage({ partner: "", richtung: OHNE_RICHTUNG })).toEqual({
      partner: null,
      richtung: null,
    });
  });

  it("macht aus reinem Leerraum leer — kein dritter Zustand durch die Hintertür", () => {
    expect(alsAnfrage({ partner: "   ", richtung: OHNE_RICHTUNG }).partner).toBeNull();
  });

  it("schneidet den Leerraum um einen gefüllten Wert ab", () => {
    expect(alsAnfrage({ partner: "  BAYER  ", richtung: "EINGEHEND" })).toEqual({
      partner: "BAYER",
      richtung: "EINGEHEND",
    });
  });

  it("sagt genau dann, was das Speichern bedeutet, wenn das Feld leer ist", () => {
    expect(speichertOhnePartner({ partner: "", richtung: OHNE_RICHTUNG })).toBe(true);
    expect(speichertOhnePartner({ partner: "  ", richtung: "AUSGEHEND" })).toBe(true);
    expect(speichertOhnePartner({ partner: "BAYER", richtung: OHNE_RICHTUNG })).toBe(false);
  });

  it("öffnet die Zeile mit ihrem eigenen Stand", () => {
    expect(entwurfAus(zeile({ processId: "a", partner: "BAYER", richtung: "EINGEHEND" }))).toEqual({
      partner: "BAYER",
      richtung: "EINGEHEND",
    });
    expect(entwurfAus(zeile({ processId: "a" }))).toEqual({
      partner: "",
      richtung: OHNE_RICHTUNG,
    });
  });

  it("gilt für die Massenzuordnung genauso", () => {
    expect(massenwert("PARTNER", "  ", OHNE_RICHTUNG)).toBeNull();
    expect(massenwert("PARTNER", " BAYER ", OHNE_RICHTUNG)).toBe("BAYER");
    expect(massenwert("RICHTUNG", "BAYER", OHNE_RICHTUNG)).toBeNull();
    expect(massenwert("RICHTUNG", "", "AUSGEHEND")).toBe("AUSGEHEND");
  });
});

describe("Nur eine Zeile ist offen", () => {
  it("lässt jede Zeile öffnen, solange keine offen ist", () => {
    expect(darfOeffnen(null, "40000_AMG_LAB_VDA")).toBe(true);
  });

  it("lässt eine zweite Zeile **nicht** öffnen, solange eine offen ist", () => {
    // Der Entwurf lebt im Komponentenzustand. Eine zweite Zeile zu öffnen hieße,
    // den ersten stillschweigend zu verwerfen — ausgerechnet den Teil der
    // Arbeit, den noch niemand geschrieben hat (E19).
    expect(darfOeffnen("40000_AMG_LAB_VDA", "40001_AMG_LAB_EDI")).toBe(false);
  });

  it("lässt die offene Zeile selbst durch", () => {
    expect(darfOeffnen("40000_AMG_LAB_VDA", "40000_AMG_LAB_VDA")).toBe(true);
  });
});

describe("Die Antwort geht in den Zwischenspeicher", () => {
  const alt = zeile({ processId: "b", partner: null, pflegestatus: "OFFEN" });
  const neu = zeile({ processId: "b", partner: "BAYER", pflegestatus: "GEPFLEGT" });

  it("ersetzt die Zeile an ihrer Stelle und lässt die Reihenfolge stehen", () => {
    const liste = [zeile({ processId: "a" }), alt, zeile({ processId: "c" })];

    const danach = mitAktualisierterZeile(liste, neu);

    expect(danach.map((z) => z.processId)).toEqual(["a", "b", "c"]);
    expect(danach[1]).toBe(neu);
    expect(danach[1].pflegestatus).toBe("GEPFLEGT");
  });

  it("hängt eine unbekannte Zeile **nicht** an", () => {
    // Eine Zeile, die der Server nicht in dieser Liste hat, gehört auch nicht
    // hinein — etwa in die Fassung mit `nurOffene`, aus der sie eben erst
    // herausgefallen ist.
    const liste = [zeile({ processId: "a" })];

    expect(mitAktualisierterZeile(liste, neu)).toHaveLength(1);
  });

  it("nimmt einen neu getippten Partner alphabetisch in die Auswahl auf", () => {
    expect(mitPartner(["AMG", "ZAST"], "BAYER")).toEqual(["AMG", "BAYER", "ZAST"]);
  });

  it("nimmt weder leer noch doppelt auf", () => {
    expect(mitPartner(["AMG"], null)).toEqual(["AMG"]);
    expect(mitPartner(["AMG"], "  ")).toEqual(["AMG"]);
    expect(mitPartner(["AMG"], "AMG")).toEqual(["AMG"]);
  });
});

describe("Die Partnervorschläge", () => {
  const ALLE = ["AMG", "BAYER", "NXS_BAYER", "ZAST"];

  it("zeigen bei leerem Feld alles — dann sucht der Nutzer noch", () => {
    expect(passendeVorschlaege(ALLE, "")).toEqual(ALLE);
    expect(passendeVorschlaege(ALLE, "   ")).toEqual(ALLE);
  });

  it("treffen an beliebiger Stelle und ohne Rücksicht auf Groß- und Kleinschreibung", () => {
    // Partnernamen sind Kürzel: Wer `bay` tippt, meint auch `NXS_BAYER`.
    expect(passendeVorschlaege(ALLE, "bay")).toEqual(["BAYER", "NXS_BAYER"]);
  });

  it("filtern den getippten Wert nicht heraus", () => {
    // Steht er schon genau so in der Liste, ist das die Auskunft, dass es ihn
    // gibt — und genau dafür sind die Vorschläge da (E21).
    expect(passendeVorschlaege(ALLE, "AMG")).toEqual(["AMG"]);
  });

  it("liefern nichts, wenn nichts passt — frei tippbar bleibt es trotzdem", () => {
    expect(passendeVorschlaege(ALLE, "GIBTESNICHT")).toEqual([]);
  });
});

describe("Der Fortschritt", () => {
  it("zählt gepflegte Zeilen gegen **alle** Zeilen", () => {
    const liste = [
      zeile({ processId: "a", pflegestatus: "GEPFLEGT" }),
      zeile({ processId: "b", pflegestatus: "GEPFLEGT" }),
      zeile({ processId: "c", pflegestatus: "OFFEN" }),
      zeile({ processId: "d", pflegestatus: "OFFEN" }),
    ];

    expect(fortschritt(liste)).toEqual({ gepflegt: 2, gesamt: 4, anteil: 0.5 });
  });

  it("zählt tote Prozesse und den Auffangprozess mit", () => {
    // Der Nenner sind alle (E18). Eine zweite Quote „gepflegt unter denen mit
    // Nachrichten" stünde als bequemere Zahl neben der richtigen und wäre binnen
    // einer Woche die berichtete: Bei `VOTG` sähe die Kuratierung dann zu
    // 89,74 % fertig aus, während 350 Zeilen unbearbeitet stehen.
    const liste = [
      zeile({ processId: "00001_Undefined", pflegestatus: "OFFEN", traegtNachrichten: true }),
      zeile({ processId: "b", pflegestatus: "GEPFLEGT", traegtNachrichten: false }),
      zeile({ processId: "c", pflegestatus: "OFFEN", traegtNachrichten: false }),
      zeile({ processId: "d", pflegestatus: "OFFEN", traegtNachrichten: null }),
    ];

    expect(fortschritt(liste).gesamt).toBe(4);
    expect(fortschritt(liste).gepflegt).toBe(1);
  });

  it("liefert auf einer leeren Liste `0` und nicht `NaN`", () => {
    expect(fortschritt([])).toEqual({ gepflegt: 0, gesamt: 0, anteil: 0 });
  });
});

describe("Der Hinweis über der Liste", () => {
  it("erscheint, wenn **keine** Zeile eine Herkunft außer `KEINE` trägt", () => {
    const liste = [
      zeile({ processId: "a", vorschlagHerkunft: "KEINE" }),
      zeile({ processId: "b", vorschlagHerkunft: "KEINE", richtung: "EINGEHEND" }),
    ];

    expect(ohneJedenPartnervorschlag(liste)).toBe(true);
  });

  it("erscheint **nicht**, sobald eine einzige Zeile einen Vorschlag trägt", () => {
    const liste = [
      zeile({ processId: "a", vorschlagHerkunft: "KEINE" }),
      zeile({ processId: "b", vorschlagHerkunft: "REGEL_A", partner: "AMG" }),
    ];

    expect(ohneJedenPartnervorschlag(liste)).toBe(false);
  });

  it("hängt am Partner allein und nicht an der Richtung", () => {
    // `NXHBE` ist genau dieser Fall: Richtung für alle 17 Prozesse aus dem
    // Projektnamen, für keinen einzigen ein Partnervorschlag. Ein Hinweis, der
    // wegen einer gefüllten Richtung verschwände, verschwände genau dort, wo die
    // eigentliche Arbeit noch aussteht (E17).
    const nxhbe = [
      zeile({ processId: "a", richtung: "EINGEHEND", vorschlagHerkunft: "KEINE" }),
      zeile({ processId: "b", richtung: "AUSGEHEND", vorschlagHerkunft: "KEINE" }),
    ];

    expect(ohneJedenPartnervorschlag(nxhbe)).toBe(true);
  });

  it("bleibt stehen, wenn ein Mensch die Partner von Hand eingetragen hat", () => {
    // `vorschlagHerkunft` beschreibt, welche Regelfassung diese Zeile einmal
    // vorgeschlagen hat, und wird durch eine Kuratierung nicht falsch, sondern
    // historisch. Vorgeschlagen hat den Partner trotzdem keiner.
    const liste = [
      zeile({
        processId: "a",
        partner: "BAYER",
        pflegestatus: "GEPFLEGT",
        vorschlagHerkunft: "KEINE",
      }),
    ];

    expect(ohneJedenPartnervorschlag(liste)).toBe(true);
  });

  it("sagt über eine leere Liste nichts", () => {
    // `every` wäre auf der leeren Liste wahr, und ein Hinweis über null Zeilen
    // sagt nichts über einen Mandanten.
    expect(ohneJedenPartnervorschlag([])).toBe(false);
  });
});

describe("Vor dem ersten Lauf sagt der Hinweis nichts", () => {
  // Gefunden in der Sichtpruefung am 24.08.2026: Auf einem frischen `NEXANS`
  // stand der Hinweis, obwohl derselbe Mandant beim ersten Knopfdruck 509
  // Partnervorschlaege bekommt. Der Grund ist das Einebnen auf der Leitung —
  // Prozesse ohne Katalogzeile tragen ebenfalls `KEINE`.
  const nieGelaufen = [
    zeile({ processId: "a", bestandGeprueftAm: null }),
    zeile({ processId: "b", bestandGeprueftAm: null }),
  ];
  const gelaufen = [
    zeile({ processId: "a", bestandGeprueftAm: "2026-08-24T11:07:00" }),
    zeile({ processId: "b", bestandGeprueftAm: "2026-08-24T11:07:00" }),
  ];

  it("erkennt den Lauf an `bestandGeprueftAm` und an nichts sonst", () => {
    expect(einLaufHatStattgefunden(nieGelaufen)).toBe(false);
    expect(einLaufHatStattgefunden(gelaufen)).toBe(true);
    // Eine einzige gestempelte Zeile genuegt: Der dritte Schritt schreibt auf
    // alle Zeilen des Mandanten (E15), ein Stempel belegt also den Lauf.
    expect(einLaufHatStattgefunden([nieGelaufen[0], gelaufen[0]])).toBe(true);
  });

  it("haelt den Hinweis zurueck, solange nie ein Lauf stattgefunden hat", () => {
    // Die Herkunftsbedingung allein ist erfuellt — und trotzdem steht der
    // Hinweis nicht da.
    expect(ohneJedenPartnervorschlag(nieGelaufen)).toBe(true);
    expect(hinweisNoetig(nieGelaufen)).toBe(false);
  });

  it("zeigt ihn, sobald ein Lauf gelaufen ist und nichts gefunden hat", () => {
    expect(hinweisNoetig(gelaufen)).toBe(true);
  });

  it("zeigt ihn nach einem Lauf mit Treffer nicht", () => {
    const mitTreffer = [
      zeile({ processId: "a", bestandGeprueftAm: "2026-08-24T11:07:00" }),
      zeile({
        processId: "b",
        bestandGeprueftAm: "2026-08-24T11:07:00",
        vorschlagHerkunft: "REGEL_A",
        partner: "AMG",
      }),
    ];

    expect(hinweisNoetig(mitTreffer)).toBe(false);
  });
});

describe("Die Projekte der Massenzuordnung", () => {
  it("entstehen aus der Liste, in ihrer Reihenfolge, mit ihrer Zeilenzahl", () => {
    const liste = [
      zeile({ processId: "a", projectId: "110_VTG", projectName: "VTG" }),
      zeile({ processId: "b", projectId: "110_VTG", projectName: "VTG" }),
      zeile({ processId: "c", projectId: "300_Kunden", projectName: null }),
    ];

    expect(projekteAus(liste)).toEqual([
      { projectId: "110_VTG", projectName: "VTG", anzahl: 2 },
      { projectId: "300_Kunden", projectName: null, anzahl: 1 },
    ]);
  });

  it("liefert für eine leere Liste keine Auswahl", () => {
    expect(projekteAus([])).toEqual([]);
  });
});

describe("Der Zustand „kein Zugriff“", () => {
  function problem(status: number, typ: string) {
    return new ProblemFehler({ status, typ });
  }

  it("greift bei `403` mit dem Typ `zugriff-verweigert`", () => {
    expect(istKeinZugriff(problem(403, "zugriff-verweigert"))).toBe(true);
  });

  it("greift **nicht** bei den beiden anderen `403` dieses Backends", () => {
    // `403` ist dreifach vergeben, und die drei bedeuten Verschiedenes: Beim
    // CSRF-Token hilft ein erneuter Versuch, bei fehlendem Mandanten schickt der
    // Anwendungsrahmen den Nutzer zur Auswahl. Eine Prüfung auf den Status
    // zeigte allen dreien dieselbe Meldung, und zwei davon wären falsch.
    expect(istKeinZugriff(problem(403, "csrf-token-ungueltig"))).toBe(false);
    expect(istKeinZugriff(problem(403, "kein-mandant-gewaehlt"))).toBe(false);
  });

  it("greift bei keinem anderen Statuscode und bei keinem fremden Fehler", () => {
    expect(istKeinZugriff(problem(404, "nicht-gefunden"))).toBe(false);
    expect(istKeinZugriff(problem(401, "nicht-angemeldet"))).toBe(false);
    expect(istKeinZugriff(new Error("irgendwas"))).toBe(false);
  });

  it("wird in beiden Sprachen aus dem Fehlerkatalog übersetzt", () => {
    // Ein eigener Schlüssel und nicht der 404-Text: Dort *ist* fehlende
    // Berechtigung nicht die Wahrheit, hier schon.
    for (const sprachdatei of [de, en]) {
      const anzeige = fehleranzeige(problem(403, "zugriff-verweigert"), sprachdatei);
      expect(anzeige.text).toBe(sprachdatei.fehler["zugriff-verweigert"]);
      expect(anzeige.text).not.toBe(sprachdatei.fehler["nicht-gefunden"]);
      // Keine Fehler-Kennung: Die hilft nur bei einem technischen Fehler.
      expect(anzeige.kennung).toBeUndefined();
    }
  });
});

describe("Die Fehler der Zeilenbearbeitung", () => {
  it("übersetzt `partner-zu-lang` und `richtung-unbekannt` in beiden Sprachen", () => {
    const faelle = ["partner-zu-lang", "richtung-unbekannt"] as const;

    for (const sprachdatei of [de, en]) {
      for (const typ of faelle) {
        const antwort = new ProblemFehler({
          status: 400,
          typ,
          // Der deutsche Text des Backends. Er darf die Übersetzung nicht
          // schlagen — übersetzt wird nach dem maschinenlesbaren `type`.
          detail: "Text aus dem Backend.",
        });

        expect(fehleranzeige(antwort, sprachdatei).text).toBe(sprachdatei.fehler[typ]);
      }
    }
  });

  it("sagt bei `404` nichts über Berechtigung", () => {
    // Eine `processId` außerhalb des aktiven Mandanten ergibt `404`, nicht
    // `403`, und ist von einer erfundenen Kennung nicht zu unterscheiden.
    const antwort = new ProblemFehler({ status: 404, typ: "nicht-gefunden" });

    expect(fehleranzeige(antwort, de).text).toBe(de.fehler["nicht-gefunden"]);
    expect(istKeinZugriff(antwort)).toBe(false);
  });
});

// ─── Die Übernahme der Partnervorschläge (E22 bis E24) ───────────────────────

function uebernahme(werte: Partial<Vorschlagsuebernahme> = {}): Vorschlagsuebernahme {
  return { modus: "VORSCHAU", betroffen: 0, regelA: 0, regelB: 0, ...werte };
}

describe("Was der Übernahme-Dialog sagt", () => {
  it("nennt bei null Vorschlägen genau einen Satz und keine Zahl", () => {
    // „Es gibt nichts zu übernehmen" ist eine Auskunft und kein Fehler. Die
    // Aufschlüsselung wäre hier dreimal die Null und damit Rauschen.
    expect(uebernahmesaetze(uebernahme(), false, de, "de")).toEqual([de.katalog.uebernahme.keine]);
  });

  it("schlüsselt bei reiner Regel A auf und sagt die Null in Worten", () => {
    // Der Fall `NEXANS`: 509 aus Regel A, keiner aus Regel B (§3.5). „0 aus
    // Regel B" läse sich als Tabelle, die in einen Satz gerutscht ist.
    const saetze = uebernahmesaetze(
      uebernahme({ betroffen: 509, regelA: 509, regelB: 0 }),
      false,
      de,
      "de",
    );

    expect(saetze).toHaveLength(3);
    expect(saetze[0]).toContain("509");
    expect(saetze[0]).toContain(de.katalog.uebernahme.keinen);
  });

  it("nennt bei gemischtem Bestand beide Zahlen", () => {
    const saetze = uebernahmesaetze(
      uebernahme({ betroffen: 5, regelA: 2, regelB: 3 }),
      false,
      de,
      "de",
    );

    expect(saetze[0]).toContain("2");
    expect(saetze[0]).toContain("3");
    expect(saetze[0]).not.toContain(de.katalog.uebernahme.keinen);
  });

  it("sagt „einen“ statt „1“ — an beiden Stellen", () => {
    const saetze = uebernahmesaetze(
      uebernahme({ betroffen: 1, regelA: 1, regelB: 0 }),
      false,
      de,
      "de",
    );

    expect(saetze[0]).toContain(de.katalog.uebernahme.uebernimmtEins);
    expect(saetze[0]).toContain(de.katalog.uebernahme.einen);
  });

  it("trägt den dritten Satz immer mit — er ist Pflicht und kein Beiwerk", () => {
    // Ohne ihn liest ein Administrator bei `NEXANS` „509" über 733 Zeilen und
    // sucht die fehlenden 224 in einem Fehler statt in E22.
    for (const zahlen of [
      { betroffen: 509, regelA: 509, regelB: 0 },
      { betroffen: 1, regelA: 0, regelB: 1 },
    ]) {
      expect(uebernahmesaetze(uebernahme(zahlen), false, de, "de")).toContain(
        de.katalog.uebernahme.ohneVorschlagBleibtOffen,
      );
    }
  });
});

describe("Der Zusatzsatz zum Filter", () => {
  it("erscheint **nur**, wenn „nur mit Nachrichten“ gesetzt ist", () => {
    const zahlen = uebernahme({ betroffen: 509, regelA: 509, regelB: 0 });

    expect(uebernahmesaetze(zahlen, false, de, "de")).not.toContain(
      de.katalog.uebernahme.filterWirktNicht,
    );
    expect(uebernahmesaetze(zahlen, true, de, "de")).toContain(
      de.katalog.uebernahme.filterWirktNicht,
    );
  });

  it("erscheint auch bei null Vorschlägen — sonst verdächtigt der Nutzer den Filter", () => {
    expect(uebernahmesaetze(uebernahme(), true, de, "de")).toEqual([
      de.katalog.uebernahme.keine,
      de.katalog.uebernahme.filterWirktNicht,
    ]);
  });

  it("steht in beiden Sprachen und nennt in beiden den Filter beim Namen", () => {
    expect(uebernahmesaetze(uebernahme(), true, en, "en")).toContain(
      en.katalog.uebernahme.filterWirktNicht,
    );
    expect(de.katalog.uebernahme.filterWirktNicht).toContain(de.katalog.filter.nurMitNachrichten);
    expect(en.katalog.uebernahme.filterWirktNicht).toContain(en.katalog.filter.nurMitNachrichten);
  });
});

describe("„Übernehmen“ ist gesperrt, bis eine Zahl dasteht", () => {
  it("bleibt ohne Vorschau gesperrt", () => {
    // Der Nutzer bestätigt eine Zahl — ohne Vorschau gibt es keine.
    expect(darfUebernehmen(null, false)).toBe(false);
  });

  it("bleibt bei `betroffen = 0` gesperrt", () => {
    expect(darfUebernehmen(uebernahme(), false)).toBe(false);
  });

  it("bleibt gesperrt, solange der Aufruf läuft", () => {
    expect(darfUebernehmen(uebernahme({ betroffen: 509, regelA: 509 }), true)).toBe(false);
  });

  it("gibt frei, sobald eine Vorschau mit Treffern vorliegt", () => {
    expect(darfUebernehmen(uebernahme({ betroffen: 509, regelA: 509 }), false)).toBe(true);
  });
});

describe("Die Fehler der Übernahme", () => {
  it("übersetzt `modus-unbekannt` in beiden Sprachen", () => {
    const antwort = new ProblemFehler({
      status: 400,
      typ: "modus-unbekannt",
      detail: "Text aus dem Backend.",
    });

    for (const sprachdatei of [de, en]) {
      expect(fehleranzeige(antwort, sprachdatei).text).toBe(sprachdatei.fehler["modus-unbekannt"]);
    }
  });

  it("zeigt bei `403` mit `zugriff-verweigert` den Zustand und keine rote Meldung", () => {
    // Der Dialog erbt die Rollengrenze von `/api/katalog/**`. Sie ist kein
    // Fehler dieses Aufrufs, sondern eine Grenze der Ansicht.
    const antwort = new ProblemFehler({ status: 403, typ: "zugriff-verweigert" });

    expect(istKeinZugriff(antwort)).toBe(true);
  });

  it("fällt bei einem unübersetzten Typ auf `detail` zurück, in beiden Sprachen", () => {
    const antwort = new ProblemFehler({
      status: 500,
      typ: "noch-nicht-uebersetzt",
      detail: "Text aus dem Backend.",
    });

    for (const sprachdatei of [de, en]) {
      expect(fehleranzeige(antwort, sprachdatei).text).toBe("Text aus dem Backend.");
    }
  });
});
