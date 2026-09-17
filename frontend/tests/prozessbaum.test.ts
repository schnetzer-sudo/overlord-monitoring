import { describe, expect, it } from "vitest";

import type {
  Baumebene,
  Baumknoten,
  Gruppenknoten,
  Prozessbaum,
  Prozessknoten,
} from "@/features/nachrichten/api";
import {
  baumzeilen,
  ebeneFaelltWeg,
  eingegrenzterBaum,
  gruppenSchluessel,
  istGruppe,
  knotentext,
  ohnePfad,
  partnertext,
  pfadZuProzess,
  prozessAus,
  prozessSchluessel,
  richtungstext,
  sichtbareProzesse,
  tastenbefehl,
  zeilenbeschriftung,
  zuordnungVon,
  zustandstext,
} from "@/features/nachrichten/prozessbaum";
import { texteFuer } from "@/i18n";

/**
 * Die **Entscheidungen** des Prozessbaums, geprüft ohne Ansicht
 * (`docs/frontend-grundlagen.md` §9). Was nur ein gerenderter Baum zeigt, steht
 * in `tests/prozess-baum.test.tsx` — und zwar nur das.
 *
 * **Seit dem 15.09.2026 für zwei Gliederungen** (E‑139, E‑140): Die Antwort ist
 * rekursiv, und dieselben Funktionen tragen Partner → Richtung → Prozess und
 * Projekt → Prozess. Die Fälle stehen deshalb je an beiden Bäumen, wo eine Regel
 * für beide gilt, und nur an einem, wo sie nur für einen entschieden ist (E‑145).
 *
 * **Alle Prüfwerte sind erfunden** (Regel T2). Kein Fall hängt an einer Zeile
 * der geteilten Testkopie; die Zahlen aus `docs/process-view.md` §9 stehen in
 * den Kommentaren und nirgends in einer Zusicherung.
 */

const TEXTE = texteFuer("de");
const ZAHL = (wert: number) => String(wert);
const SCHWELLE = 3;

const PARTNER_EBENEN: Baumebene[] = ["PARTNER", "RICHTUNG", "PROZESS"];
const PROJEKT_EBENEN: Baumebene[] = ["PROJEKT", "PROZESS"];

function blatt(
  processId: string,
  name: string | null,
  nachrichten: number,
  fehler = 0,
  zustand: Prozessknoten["zustand"] = "BEWEGT",
): Prozessknoten {
  return {
    schluessel: processId,
    name,
    processId,
    nachrichten,
    fehler,
    letzteBewegung: zustand === "NIE" ? null : "2025-12-29T22:00:00Z",
    zustand,
  };
}

/** Eine Gruppe, wie der Dienst sie liefert: Schlüssel hochgestellt, Summen aus den Kindern. */
function gruppe(name: string | null, kinder: Baumknoten[]): Gruppenknoten {
  return {
    schluessel: name === null ? null : name.toUpperCase(),
    name,
    anzahlProzesse: kinder.reduce(
      (wert, kind) => wert + (istGruppe(kind) ? kind.anzahlProzesse : 1),
      0,
    ),
    nachrichten: kinder.reduce((wert, kind) => wert + kind.nachrichten, 0),
    fehler: kinder.reduce((wert, kind) => wert + kind.fehler, 0),
    kinder,
  };
}

/** Ein Partner mit **zwei** Richtungen — die Ebene bleibt stehen. */
const MIT_EBENE = gruppe("ACME", [
  gruppe("EINGEHEND", [blatt("p1", "ACME Bestellung", 10, 2)]),
  gruppe("AUSGEHEND", [blatt("p2", "ACME Lieferschein", 5)]),
]);

/**
 * Ein Partner mit **einer, nicht ermittelten** Richtung — die Ebene fällt weg
 * (E‑58). Sie trüge einen Knoten „nicht ermittelt“ über zwei Blättern und
 * schriebe nichts hin.
 */
const OHNE_EBENE = gruppe("BOSCH", [
  gruppe(null, [blatt("p3", "BOSCH Rechnung", 0, 0, "STILL"), blatt("p4", null, 7)]),
]);

/**
 * Ein Partner mit **einer, bekannten** Richtung — die Ebene **steht** (E‑58).
 * Der Fall aus dem Bild: `ADIENT` mit genau einem eingehenden Prozess.
 */
const EINE_RICHTUNG = gruppe("CONTI", [gruppe("EINGEHEND", [blatt("p6", "CONTI Bestellung", 4)])]);

/** „nicht zugeordnet“ — bei `SUTTONS` die einzige Gruppe, und ohne Richtung. */
const OHNE_PARTNER = gruppe(null, [gruppe(null, [blatt("p5", "Freier Prozess", 0, 0, "NIE")])]);

const FENSTER = { von: "2025-12-28T05:00:00Z", bis: "2025-12-30T05:00:00Z" };
const GESAMT = { anzahlProzesse: 5, bewegt: 3, still: 1, nie: 1, nachrichten: 22, fehler: 2 };

const BAUM: Prozessbaum = {
  zeitraum: "48H",
  gliederung: "PARTNER",
  fenster: FENSTER,
  stilleSchwelleMonate: SCHWELLE,
  liveRest: { zustand: "NICHT_NOETIG", vollstaendigBis: null },
  gesamt: GESAMT,
  ebenen: PARTNER_EBENEN,
  knoten: [MIT_EBENE, OHNE_EBENE, OHNE_PARTNER],
};

/**
 * **Dieselben fünf Prozesse**, nach Projekt gegliedert — so, wie der Dienst sie
 * liefert: alphabetisch nach dem hochgestellten Schlüssel (E‑142), die Blätter
 * in der Reihenfolge der Abfrage.
 */
const AUSGANG = gruppe("Ausgang", [blatt("p2", "ACME Lieferschein", 5), blatt("p4", null, 7)]);
const EINGANG = gruppe("Eingang von Kunden", [
  blatt("p1", "ACME Bestellung", 10, 2),
  blatt("p3", "BOSCH Rechnung", 0, 0, "STILL"),
  blatt("p5", "Freier Prozess", 0, 0, "NIE"),
]);

const PROJEKTBAUM: Prozessbaum = {
  ...BAUM,
  gliederung: "PROJEKT",
  ebenen: PROJEKT_EBENEN,
  knoten: [AUSGANG, EINGANG],
};

const ALLES_OFFEN = () => true;
const ALLES_ZU = () => false;

describe("E‑58 — die Richtungsebene fällt nur weg, wo es nichts zu schreiben gibt", () => {
  it("erkennt die Fälle am Knoten und nicht an der Antwort", () => {
    expect(ebeneFaelltWeg(MIT_EBENE, "RICHTUNG"), "zwei Richtungen").toBe(false);
    // ⚠️ **Das ist die Umkehrung vom 03.09.2026.** Bis dahin genügte „genau
    // eine Richtung“; seither muss sie auch **unbekannt** sein. Eine bekannte
    // Richtung steht immer als Ebene, sonst schriebe der Baum denselben
    // Sachverhalt an zwei Stellen verschieden.
    expect(ebeneFaelltWeg(EINE_RICHTUNG, "RICHTUNG"), "eine bekannte Richtung").toBe(false);
    expect(ebeneFaelltWeg(OHNE_EBENE, "RICHTUNG"), "eine unbekannte Richtung").toBe(true);
    expect(ebeneFaelltWeg(OHNE_PARTNER, "RICHTUNG"), "ohne Partner, ohne Richtung").toBe(true);
  });

  it("lässt die Ebene auch über einem einzigen Kind stehen, wenn die Richtung bekannt ist", () => {
    // Der Fall aus dem Bild: `ADIENT` trägt einen Prozess, und „Eingehend“
    // stand deshalb als Vorsatz in seiner Zeile, während dasselbe Wort beim
    // Nachbarn `ACOME` eine Zeile war.
    const zeilen = baumzeilen([EINE_RICHTUNG], PARTNER_EBENEN, ALLES_OFFEN);

    expect(zeilen.map((zeile) => zeile.art)).toEqual(["GRUPPE", "GRUPPE", "PROZESS"]);
    expect(zeilen.map((zeile) => zeile.ebene)).toEqual([1, 2, 3]);
    expect(zeilen[1].art === "GRUPPE" && zeilen[1].ebenenname).toBe("RICHTUNG");
    expect(zeilen[1].art === "GRUPPE" && zeilen[1].name).toBe("EINGEHEND");
  });

  it("gibt auch einem gepflegten, aber unbekannten Wert seine Ebene (Regel Q4)", () => {
    // Er ist **bekannt** — nur nicht übersetzbar. Er steht als Zeile da, wie er
    // im Katalog steht; „nicht ermittelt“ ist etwas anderes.
    const vierter = gruppe("ACME", [gruppe("RUECKMELDUNG", [blatt("p9", "Quittung", 1)])]);
    expect(ebeneFaelltWeg(vierter, "RICHTUNG")).toBe(false);

    const zeilen = baumzeilen([vierter], PARTNER_EBENEN, ALLES_OFFEN);
    expect(zeilen[1].art === "GRUPPE" && zeilen[1].name).toBe("RUECKMELDUNG");
    expect(zeilenbeschriftung(zeilen[1], SCHWELLE, TEXTE, ZAHL)).toContain("RUECKMELDUNG");
  });

  it("lässt bei zwei Richtungen die Ebene stehen und die Blätter auf Ebene 3", () => {
    const zeilen = baumzeilen([MIT_EBENE], PARTNER_EBENEN, ALLES_OFFEN);

    expect(zeilen.map((zeile) => zeile.art)).toEqual([
      "GRUPPE",
      "GRUPPE",
      "PROZESS",
      "GRUPPE",
      "PROZESS",
    ]);
    expect(zeilen.map((zeile) => zeile.ebene)).toEqual([1, 2, 3, 2, 3]);
  });

  it("überspringt die Ebene bei nicht ermittelter Richtung — und schreibt nichts nach", () => {
    // Der Fall von `VOTG`: keine einzige kuratierte Richtung, 133 Knoten „nicht
    // ermittelt“, die nichts ordnen. Die Ebene fällt weg, und die Blätter
    // tragen **keinen Ersatz** — weder ein Zeichen noch ein Wort.
    const zeilen = baumzeilen([OHNE_EBENE], PARTNER_EBENEN, ALLES_OFFEN);

    expect(zeilen.map((zeile) => zeile.art)).toEqual(["GRUPPE", "PROZESS", "PROZESS"]);
    expect(zeilen.map((zeile) => zeile.ebene)).toEqual([1, 2, 2]);
  });

  it("gibt einem Blatt in keinem Fall eine eigene Richtung mit", () => {
    // **Die Abwesenheit ist die Regel** (E‑58): Wo die Richtung bekannt ist,
    // trägt sie die Ebene; wo nicht, gibt es sie nicht. Ein Feld an der
    // Blattzeile wäre die zweite Schreibweise, die abgeschafft worden ist.
    const alle = [
      ...baumzeilen([MIT_EBENE], PARTNER_EBENEN, ALLES_OFFEN),
      ...baumzeilen([EINE_RICHTUNG], PARTNER_EBENEN, ALLES_OFFEN),
      ...baumzeilen([OHNE_EBENE], PARTNER_EBENEN, ALLES_OFFEN),
      ...baumzeilen([OHNE_PARTNER], PARTNER_EBENEN, ALLES_OFFEN),
    ].filter((zeile) => zeile.art === "PROZESS");

    expect(alle.length).toBeGreaterThan(0);
    expect(alle.every((zeile) => !("richtung" in zeile))).toBe(true);
  });
});

describe("E‑145 — das Überspringen bleibt auf die Richtungsebene beschränkt", () => {
  it("fragt nach der Ebene der Kinder und nicht nach ihrer Zahl", () => {
    // Derselbe Knoten, einmal über einer Richtungsebene und einmal über
    // Prozessen: Nur die Richtung fällt weg. Eine Verallgemeinerung auf „eine
    // Ebene mit einem Knoten“ ist weder entschieden noch gewünscht.
    expect(ebeneFaelltWeg(OHNE_EBENE, "RICHTUNG")).toBe(true);
    expect(ebeneFaelltWeg(OHNE_EBENE, "PROZESS")).toBe(false);
    expect(ebeneFaelltWeg(gruppe("Einziges Projekt", [blatt("p1", "A", 1)]), "PROZESS")).toBe(
      false,
    );
  });

  it("zeigt ein einziges Projekt mit einem einzigen Prozess als zwei Zeilen", () => {
    // `SUTTONS` und `WOC` haben je ein Projekt — es steht als Zeile da.
    const einziges = gruppe("Einziges Projekt", [blatt("p1", "Alleiniger Prozess", 1)]);
    const zeilen = baumzeilen([einziges], PROJEKT_EBENEN, ALLES_OFFEN);

    expect(zeilen.map((zeile) => zeile.art)).toEqual(["GRUPPE", "PROZESS"]);
    expect(zeilen.map((zeile) => zeile.ebene)).toEqual([1, 2]);
  });
});

describe("Die sichtbaren Zeilen", () => {
  it("zeigt zugeklappt genau die oberste Ebene", () => {
    const partner = baumzeilen(BAUM.knoten, BAUM.ebenen, ALLES_ZU);
    expect(partner).toHaveLength(3);
    expect(partner.every((zeile) => zeile.art === "GRUPPE" && zeile.ebene === 1)).toBe(true);

    const projekte = baumzeilen(PROJEKTBAUM.knoten, PROJEKTBAUM.ebenen, ALLES_ZU);
    expect(projekte).toHaveLength(2);
    expect(
      projekte.every((zeile) => zeile.art === "GRUPPE" && zeile.ebenenname === "PROJEKT"),
    ).toBe(true);
  });

  it("zählt Position und Geschwister je Ebene und nicht über die ganze Liste", () => {
    const zeilen = baumzeilen(BAUM.knoten, BAUM.ebenen, ALLES_OFFEN);

    // Die drei Partner sind 1..3 von 3.
    const partner = zeilen.filter((zeile) => zeile.art === "GRUPPE" && zeile.ebene === 1);
    expect(partner.map((zeile) => zeile.position)).toEqual([1, 2, 3]);
    expect(partner.every((zeile) => zeile.geschwister === 3)).toBe(true);

    // Die beiden Richtungen unter ACME sind 1..2 von 2.
    const richtungen = zeilen.filter(
      (zeile) => zeile.art === "GRUPPE" && zeile.ebenenname === "RICHTUNG",
    );
    expect(richtungen.map((zeile) => zeile.position)).toEqual([1, 2]);
    expect(richtungen.every((zeile) => zeile.geschwister === 2)).toBe(true);

    // Die beiden Blätter unter BOSCH sind 1..2 von 2 — obwohl sie in der
    // flachen Liste an Position 6 und 7 stehen.
    const bosch = zeilen.filter(
      (zeile) => zeile.art === "PROZESS" && ["p3", "p4"].includes(zeile.prozess.processId),
    );
    expect(bosch.map((zeile) => zeile.position)).toEqual([1, 2]);
    expect(bosch.every((zeile) => zeile.geschwister === 2)).toBe(true);
  });

  it("macht aus dem Projektbaum zwei Ebenen über denselben Weg", () => {
    const zeilen = baumzeilen(PROJEKTBAUM.knoten, PROJEKTBAUM.ebenen, ALLES_OFFEN);

    expect(zeilen.map((zeile) => zeile.art)).toEqual([
      "GRUPPE",
      "PROZESS",
      "PROZESS",
      "GRUPPE",
      "PROZESS",
      "PROZESS",
      "PROZESS",
    ]);
    expect(zeilen.map((zeile) => zeile.ebene)).toEqual([1, 2, 2, 1, 2, 2, 2]);
  });

  it("gibt jeder Zeile einen eindeutigen Schlüssel", () => {
    for (const baum of [BAUM, PROJEKTBAUM]) {
      const schluessel = baumzeilen(baum.knoten, baum.ebenen, ALLES_OFFEN).map(
        (zeile) => zeile.schluessel,
      );
      expect(new Set(schluessel).size).toBe(schluessel.length);
    }
  });

  it("unterscheidet die Gruppe ohne Wert von einer, die leer heißt — und Partner von Projekt", () => {
    // Beide sind für den Nutzer verschieden, und beide müssen einen eigenen
    // Aufklappzustand haben können (Regel Q4).
    expect(gruppenSchluessel(null, "PARTNER", null)).not.toBe(
      gruppenSchluessel(null, "PARTNER", ""),
    );
    // Beide Gliederungen teilen sich den Aufklappzustand der Ansicht: Ein
    // Partner und ein Projekt mit demselben Wert dürfen sich nicht treffen.
    expect(gruppenSchluessel(null, "PARTNER", "ACME")).not.toBe(
      gruppenSchluessel(null, "PROJEKT", "ACME"),
    );
    const acme = gruppenSchluessel(null, "PARTNER", "ACME");
    expect(gruppenSchluessel(acme, "RICHTUNG", null)).not.toBe(
      gruppenSchluessel(acme, "RICHTUNG", ""),
    );
  });
});

describe("Der Pfad zum gewählten Prozess", () => {
  const acme = gruppenSchluessel(null, "PARTNER", "ACME");

  it("nennt Partner und Richtung, wenn die Ebene steht", () => {
    expect(pfadZuProzess(BAUM.knoten, BAUM.ebenen, "p1")).toEqual([
      acme,
      gruppenSchluessel(acme, "RICHTUNG", "EINGEHEND"),
    ]);
  });

  it("nennt nur den Partner, wenn die Ebene weggefallen ist", () => {
    // Sonst stünde in der URL-Ableitung ein Knoten, den es im Baum nicht gibt —
    // und der Partner bliebe zu.
    expect(pfadZuProzess(BAUM.knoten, BAUM.ebenen, "p3")).toEqual([
      gruppenSchluessel(null, "PARTNER", "BOSCH"),
    ]);
  });

  it("nennt im Projektbaum das Projekt", () => {
    expect(pfadZuProzess(PROJEKTBAUM.knoten, PROJEKTBAUM.ebenen, "p3")).toEqual([
      gruppenSchluessel(null, "PROJEKT", "EINGANG VON KUNDEN"),
    ]);
  });

  it("bleibt leer, wenn die Kennung in diesem Baum nicht vorkommt", () => {
    // Ein geteilter Link kann eine `ProcessID` eines anderen Mandanten tragen.
    expect(pfadZuProzess(BAUM.knoten, BAUM.ebenen, "fremd")).toEqual([]);
    expect(pfadZuProzess(BAUM.knoten, BAUM.ebenen, null)).toEqual([]);
  });

  it("öffnet den Pfad tatsächlich — die Zeile des Prozesses steht dann in der Liste", () => {
    for (const baum of [BAUM, PROJEKTBAUM]) {
      const pfad = new Set(pfadZuProzess(baum.knoten, baum.ebenen, "p2"));
      const schluessel = baumzeilen(baum.knoten, baum.ebenen, (s) => pfad.has(s)).map(
        (zeile) => zeile.schluessel,
      );

      expect(schluessel).toContain(prozessSchluessel("p2"));
      // Und nur dieser Ast: Der Prozess unter einer anderen Gruppe bleibt zu.
      expect(schluessel).not.toContain(prozessSchluessel("p3"));
    }
  });
});

describe("Der Aufklappzustand entlang eines Pfades", () => {
  const acme = gruppenSchluessel(null, "PARTNER", "ACME");
  const bosch = gruppenSchluessel(null, "PARTNER", "BOSCH");

  it("vergisst genau die Umschaltungen des neuen Pfades und sonst keine", () => {
    // Der Fall, der die Funktion nötig gemacht hat: Ein Partner, den der Nutzer
    // einmal zugeklappt hat, bliebe sonst zu — auch wenn ein später gewählter
    // Prozess unter ihm hängt. `??` fällt bei `false` nicht durch.
    const bisher = new Map([
      [acme, false],
      [bosch, true],
    ]);

    const neu = ohnePfad(bisher, pfadZuProzess(BAUM.knoten, BAUM.ebenen, "p1"));

    expect(neu.has(acme)).toBe(false);
    // Was der Nutzer anderswo aufgeklappt hat, bleibt aufgeklappt.
    expect(neu.get(bosch)).toBe(true);
  });

  it("räumt auch die Richtungsebene, wenn sie im Pfad liegt", () => {
    const pfad = pfadZuProzess(BAUM.knoten, BAUM.ebenen, "p1");
    expect(pfad).toHaveLength(2);

    const bisher = new Map(pfad.map((schluessel) => [schluessel, false]));
    expect(ohnePfad(bisher, pfad).size).toBe(0);
  });

  it("gibt dieselbe Menge zurück, wenn nichts zu räumen ist", () => {
    // Kein neues Objekt heißt: kein neues Rendern — die Funktion läuft bei
    // jedem Prozesswechsel.
    const bisher = new Map([[bosch, true]]);
    expect(ohnePfad(bisher, pfadZuProzess(BAUM.knoten, BAUM.ebenen, "p1"))).toBe(bisher);
    expect(ohnePfad(bisher, [])).toBe(bisher);
  });
});

describe("Die Eingrenzung", () => {
  it("lässt ohne Begriff und ohne Schalter alles stehen", () => {
    for (const baum of [BAUM, PROJEKTBAUM]) {
      const gefiltert = eingegrenzterBaum(baum.knoten, baum.ebenen, "", false);
      expect(gefiltert).toHaveLength(baum.knoten.length);
      expect(sichtbareProzesse(gefiltert)).toBe(baum.gesamt.anzahlProzesse);
    }
  });

  it("rechnet die Summen aus den Blättern — und trifft ohne Eingrenzung die gelieferten", () => {
    // **Die Invariante**: Die Kennzahlen sind über die Blätter additiv, und der
    // Dienst bildet sie genauso. Wäre das nicht so, zeigte eine eingegrenzte
    // Ansicht andere Zahlen als eine ungefilterte — und beide sähen richtig aus.
    for (const baum of [BAUM, PROJEKTBAUM]) {
      const gefiltert = eingegrenzterBaum(baum.knoten, baum.ebenen, "", false);

      for (const [index, knoten] of gefiltert.entries()) {
        const geliefert = baum.knoten[index] as Gruppenknoten;
        expect((knoten as Gruppenknoten).anzahlProzesse).toBe(geliefert.anzahlProzesse);
        expect(knoten.nachrichten).toBe(geliefert.nachrichten);
        expect(knoten.fehler).toBe(geliefert.fehler);
      }
    }
  });

  it("behält bei einem Partnertreffer alle seine Prozesse", () => {
    // Wer nach einem Partner sucht, will dessen Prozesse sehen — und nicht die
    // Teilmenge, deren Namen zufällig denselben Text tragen.
    const gefiltert = eingegrenzterBaum(BAUM.knoten, BAUM.ebenen, "bosch", false);

    expect(gefiltert).toHaveLength(1);
    expect(gefiltert[0].name).toBe("BOSCH");
    expect(sichtbareProzesse(gefiltert)).toBe(2);
  });

  it("behält bei einem Projekttreffer alle seine Prozesse", () => {
    // Die Projektbeschreibung ist ein Wert der Antwort und wird deshalb
    // durchsucht — anders als die Richtung.
    const gefiltert = eingegrenzterBaum(PROJEKTBAUM.knoten, PROJEKTBAUM.ebenen, "kunden", false);

    expect(gefiltert).toHaveLength(1);
    expect(gefiltert[0].name).toBe("Eingang von Kunden");
    expect(sichtbareProzesse(gefiltert)).toBe(3);
  });

  it("lässt eine Gruppe stehen, deren Kind trifft — mit nur diesem Kind", () => {
    const gefiltert = eingegrenzterBaum(BAUM.knoten, BAUM.ebenen, "lieferschein", false);

    expect(gefiltert).toHaveLength(1);
    const acme = gefiltert[0] as Gruppenknoten;
    expect(acme.name).toBe("ACME");
    expect(acme.anzahlProzesse).toBe(1);
    // Die leere Richtungsgruppe fällt mit weg — kein leerer Ast.
    expect(acme.kinder).toHaveLength(1);
    expect(acme.kinder[0].name).toBe("AUSGEHEND");
    // Und die Zahlen beschreiben, was zu sehen ist.
    expect(acme.nachrichten).toBe(5);

    const projekte = eingegrenzterBaum(
      PROJEKTBAUM.knoten,
      PROJEKTBAUM.ebenen,
      "lieferschein",
      false,
    );
    expect(projekte.map((knoten) => knoten.name)).toEqual(["Ausgang"]);
    expect(sichtbareProzesse(projekte)).toBe(1);
  });

  it("ist unabhängig von der Groß- und Kleinschreibung", () => {
    expect(eingegrenzterBaum(BAUM.knoten, BAUM.ebenen, "ACME", false)).toHaveLength(1);
    expect(eingegrenzterBaum(BAUM.knoten, BAUM.ebenen, "acme", false)).toHaveLength(1);
  });

  it("übergeht Leerraum am Rand", () => {
    expect(eingegrenzterBaum(BAUM.knoten, BAUM.ebenen, "   ", false)).toHaveLength(3);
    expect(eingegrenzterBaum(BAUM.knoten, BAUM.ebenen, "  bosch  ", false)).toHaveLength(1);
  });

  it("greift nicht auf einen Namen zu, den es nicht gibt", () => {
    // Ein Prozessname darf `null` sein, und `null` trifft nichts — es wird nicht
    // stillschweigend als leerer Text behandelt.
    const gefiltert = eingegrenzterBaum([OHNE_EBENE], PARTNER_EBENEN, "rechnung", false);
    expect(sichtbareProzesse(gefiltert)).toBe(1);
    const richtung = (gefiltert[0] as Gruppenknoten).kinder[0] as Gruppenknoten;
    expect((richtung.kinder[0] as Prozessknoten).processId).toBe("p3");
  });

  it("filtert nicht über die Richtung", () => {
    // „Eingehend“ ist ein Text der Oberfläche und stünde in zwei Sprachen
    // verschieden da. Gefiltert wird über die Werte der Antwort (Regel Q4).
    expect(eingegrenzterBaum(BAUM.knoten, BAUM.ebenen, "eingehend", false)).toHaveLength(0);
  });

  it("blendet mit dem Schalter aus, was im Zeitraum nichts getragen hat", () => {
    const gefiltert = eingegrenzterBaum(BAUM.knoten, BAUM.ebenen, "", true);

    // p3 (still, 0) und p5 (nie, 0) fallen weg; „nicht zugeordnet“ verliert
    // damit seinen einzigen Prozess und verschwindet ganz.
    expect(sichtbareProzesse(gefiltert)).toBe(3);
    expect(gefiltert.map((knoten) => knoten.name)).toEqual(["ACME", "BOSCH"]);

    // Im Projektbaum dieselben drei Prozesse.
    expect(
      sichtbareProzesse(eingegrenzterBaum(PROJEKTBAUM.knoten, PROJEKTBAUM.ebenen, "", true)),
    ).toBe(3);
  });

  it("verbindet Begriff und Schalter mit UND", () => {
    expect(sichtbareProzesse(eingegrenzterBaum(BAUM.knoten, BAUM.ebenen, "bosch", true))).toBe(1);
  });

  it("gibt eine leere Liste zurück, wenn nichts passt — und keinen leeren Ast", () => {
    expect(eingegrenzterBaum(BAUM.knoten, BAUM.ebenen, "gibtesnicht", false)).toEqual([]);
  });
});

describe("Der gewählte Prozess", () => {
  it("wird im ganzen Baum gefunden, samt seiner Zuordnung", () => {
    expect(prozessAus(BAUM, "p2")?.name).toBe("ACME Lieferschein");
    expect(zuordnungVon(BAUM, "p2")).toEqual([
      { ebene: "PARTNER", name: "ACME" },
      { ebene: "RICHTUNG", name: "AUSGEHEND" },
    ]);
  });

  it("nennt im Projektbaum das Projekt als einzige Zuordnung", () => {
    expect(prozessAus(PROJEKTBAUM, "p4")?.processId).toBe("p4");
    expect(zuordnungVon(PROJEKTBAUM, "p4")).toEqual([{ ebene: "PROJEKT", name: "Ausgang" }]);
  });

  it("unterscheidet „nicht gefunden“ von „nicht zugeordnet“", () => {
    // `undefined` heißt: Die Kennung kommt in diesem Baum nicht vor — dann zeigt
    // die Ansicht die Kennung. `null` heißt: nicht zugeordnet (Regel Q4).
    expect(zuordnungVon(BAUM, "fremd")).toBeUndefined();
    expect(zuordnungVon(BAUM, "p5")).toEqual([
      { ebene: "PARTNER", name: null },
      { ebene: "RICHTUNG", name: null },
    ]);
    expect(prozessAus(undefined, "p1")).toBeUndefined();
    expect(prozessAus(BAUM, null)).toBeUndefined();
  });
});

describe("Beschriftung", () => {
  it("übersetzt die beiden bekannten Richtungen und lässt einen dritten Wert stehen", () => {
    expect(richtungstext("EINGEHEND", TEXTE)).toBe(TEXTE.prozesse.richtung.EINGEHEND);
    expect(richtungstext("AUSGEHEND", TEXTE)).toBe(TEXTE.prozesse.richtung.AUSGEHEND);
    expect(richtungstext(null, TEXTE)).toBe(TEXTE.prozesse.richtung.nichtErmittelt);
    // Der Katalog führt `richtung` als `varchar(20)`; ein vierter Wert bekommt
    // keine geratene Übersetzung (Regel Q4).
    expect(richtungstext("TRANSIT", TEXTE)).toBe("TRANSIT");
  });

  it("nennt einen fehlenden Partner „nicht zugeordnet“ und nie leer", () => {
    expect(partnertext(null, TEXTE)).toBe(TEXTE.prozesse.nichtZugeordnet);
    expect(partnertext("ACME", TEXTE)).toBe("ACME");
  });

  it("beschriftet einen Knoten ohne Wert je Ebene mit ihrem eigenen Wort", () => {
    // Vier Ebenen, vier Bedeutungen von `null` — und ein vorhandener Wert steht
    // in jeder, wie er geliefert wurde.
    expect(knotentext("PARTNER", null, TEXTE)).toBe(TEXTE.prozesse.nichtZugeordnet);
    expect(knotentext("RICHTUNG", null, TEXTE)).toBe(TEXTE.prozesse.richtung.nichtErmittelt);
    expect(knotentext("PROJEKT", null, TEXTE)).toBe(TEXTE.prozesse.ohneBeschreibung);
    expect(knotentext("PROZESS", null, TEXTE)).toBe(TEXTE.prozesse.ohneNamen);
    expect(knotentext("PROJEKT", "Eingang von Kunden", TEXTE)).toBe("Eingang von Kunden");
    expect(knotentext("RICHTUNG", "EINGEHEND", TEXTE)).toBe(TEXTE.prozesse.richtung.EINGEHEND);
  });

  it("beschriftet „still“ mit der Schwelle aus der Antwort und rechnet nichts nach", () => {
    // Die Zahl kommt aus `stilleSchwelleMonate` (Entscheidung E‑37) — eine
    // andere Antwort ergibt einen anderen Text, ohne Codeänderung.
    expect(zustandstext("STILL", 3, TEXTE)).toContain("3");
    expect(zustandstext("STILL", 6, TEXTE)).toContain("6");
    expect(zustandstext("STILL", 3, TEXTE)).not.toBe(zustandstext("STILL", 6, TEXTE));
  });

  it("gibt weder „bewegt“ noch „nie“ einen Zusatz (E‑56)", () => {
    // **Bis zum 02.09.2026 trug „nie“ hier ein Wort.** Es ist mit der Dämpfung
    // zusammen gefallen und **nur** zusammen mit ihr: Die Dämpfung allein wäre
    // eine Aussage über Helligkeit, und die verbietet `visuelles-konzept.md` §3.
    //
    // „nie“ ist eine **Katalogfrage** und kein Vorfall (E‑36); ein neu
    // angelegter Partner ohne Verkehr sähe in der Zeile sonst aus wie ein
    // Fehlerfall. Die Zahl bleibt — sie steht in der Kopfzeile.
    expect(zustandstext("BEWEGT", 3, TEXTE)).toBeNull();
    expect(zustandstext("NIE", 3, TEXTE)).toBeNull();
    expect(zustandstext("STILL", 3, TEXTE)).not.toBeNull();
  });

  it("nennt „nie“ auch im vorgelesenen Namen nicht mehr (E‑52 + E‑56)", () => {
    // **Sichtbare und vorgelesene Fassung dürfen nicht auseinanderlaufen.** Der
    // Zustand steht in der Zeile nicht mehr; stünde er im `aria-label`, hörte
    // ein Nutzer eine Einordnung, die kein anderer sieht.
    const nieZeile = baumzeilen([OHNE_PARTNER], PARTNER_EBENEN, ALLES_OFFEN)[1];
    const name = zeilenbeschriftung(nieZeile, SCHWELLE, TEXTE, ZAHL);

    expect(name).toContain("Freier Prozess");
    expect(name).toContain("Nachrichten: 0");
    // Die Gegenprobe zur Gegenprobe: „still“ steht weiterhin drin, sonst
    // prüfte dieser Test nur, dass der Name überhaupt kurz ist.
    expect(name).not.toContain("nie");
    expect(
      zeilenbeschriftung(
        baumzeilen([OHNE_EBENE], PARTNER_EBENEN, ALLES_OFFEN)[1],
        SCHWELLE,
        TEXTE,
        ZAHL,
      ),
    ).toContain(zustandstext("STILL", SCHWELLE, TEXTE) as string);
  });

  it("nennt im vorgelesenen Namen die Ebene, die Zahlen und den Zustand", () => {
    const zeilen = baumzeilen(BAUM.knoten, BAUM.ebenen, ALLES_OFFEN);
    const partnerzeile = zeilen[0];
    const fehlerzeile = zeilen.find(
      (zeile) => zeile.art === "PROZESS" && zeile.prozess.processId === "p1",
    ) as (typeof zeilen)[number];

    const partnername = zeilenbeschriftung(partnerzeile, SCHWELLE, TEXTE, ZAHL);
    expect(partnername).toContain("ACME");
    expect(partnername).toContain(TEXTE.prozesse.baum.ebenePartner);
    // Ohne Beschriftung wäre das „ACME 2 15 2“ — vorgelesen bedeutungslos.
    expect(partnername).toContain("Prozesse: 2");
    expect(partnername).toContain("Nachrichten: 15");
    expect(partnername).toContain("Fehler: 2");

    expect(zeilenbeschriftung(fehlerzeile, SCHWELLE, TEXTE, ZAHL)).toContain("Fehler: 2");
  });

  it("nennt am Projekt die Ebene „Projekt“ und seine Summen", () => {
    const projektzeile = baumzeilen(PROJEKTBAUM.knoten, PROJEKTBAUM.ebenen, ALLES_ZU)[1];
    const name = zeilenbeschriftung(projektzeile, SCHWELLE, TEXTE, ZAHL);

    expect(name).toContain("Eingang von Kunden");
    expect(name).toContain(TEXTE.prozesse.baum.ebeneProjekt);
    expect(name).toContain("Prozesse: 3");
    expect(name).not.toContain(TEXTE.prozesse.baum.ebenePartner);
  });

  it("nennt keine Fehler, wo es keine gibt", () => {
    // Eine `0` vorzulesen behauptet eine Aussage, die die Zeile nicht macht.
    const zeilen = baumzeilen([OHNE_EBENE], PARTNER_EBENEN, ALLES_OFFEN);
    const ohneFehler = zeilen[2];
    expect(zeilenbeschriftung(ohneFehler, SCHWELLE, TEXTE, ZAHL)).not.toContain("Fehler");
  });

  it("nennt die Richtung an der Ebene und an keinem Blatt (E‑58)", () => {
    // **Sichtbare und vorgelesene Fassung dürfen nicht auseinanderlaufen.** Bis
    // zum 03.09.2026 stand „nicht ermittelt“ im Namen genau der Blätter, die
    // sichtbar nichts trugen.
    const richtungszeile = baumzeilen([EINE_RICHTUNG], PARTNER_EBENEN, ALLES_OFFEN)[1];
    expect(zeilenbeschriftung(richtungszeile, SCHWELLE, TEXTE, ZAHL)).toContain(
      TEXTE.prozesse.richtung.EINGEHEND,
    );

    for (const knotenmenge of [[MIT_EBENE], [EINE_RICHTUNG], [OHNE_EBENE], [OHNE_PARTNER]]) {
      for (const zeile of baumzeilen(knotenmenge, PARTNER_EBENEN, ALLES_OFFEN)) {
        if (zeile.art !== "PROZESS") {
          continue;
        }
        const name = zeilenbeschriftung(zeile, SCHWELLE, TEXTE, ZAHL);
        expect(name).not.toContain(TEXTE.prozesse.richtung.EINGEHEND);
        expect(name).not.toContain(TEXTE.prozesse.richtung.AUSGEHEND);
        expect(name).not.toContain(TEXTE.prozesse.richtung.nichtErmittelt);
      }
    }
  });

  it("nennt den Zustand eines stillen Prozesses", () => {
    const still = baumzeilen([OHNE_EBENE], PARTNER_EBENEN, ALLES_OFFEN)[1];
    expect(zeilenbeschriftung(still, SCHWELLE, TEXTE, ZAHL)).toContain(
      zustandstext("STILL", SCHWELLE, TEXTE) as string,
    );
  });
});

describe("Die Tastatur — das WAI‑ARIA-Muster", () => {
  const offen = baumzeilen(BAUM.knoten, BAUM.ebenen, ALLES_OFFEN);
  const zu = baumzeilen(BAUM.knoten, BAUM.ebenen, ALLES_ZU);

  it("bewegt sich mit auf und ab über die sichtbaren Zeilen", () => {
    expect(tastenbefehl(offen, 0, "ArrowDown")).toEqual({
      art: "FOKUS",
      schluessel: offen[1].schluessel,
    });
    expect(tastenbefehl(offen, 1, "ArrowUp")).toEqual({
      art: "FOKUS",
      schluessel: offen[0].schluessel,
    });
  });

  it("bleibt an den Enden stehen, statt umzuspringen", () => {
    expect(tastenbefehl(offen, 0, "ArrowUp")).toBeNull();
    expect(tastenbefehl(offen, offen.length - 1, "ArrowDown")).toBeNull();
  });

  it("springt mit Pos1 und Ende an die Enden", () => {
    expect(tastenbefehl(offen, 4, "Home")).toEqual({
      art: "FOKUS",
      schluessel: offen[0].schluessel,
    });
    expect(tastenbefehl(offen, 0, "End")).toEqual({
      art: "FOKUS",
      schluessel: offen[offen.length - 1].schluessel,
    });
  });

  it("klappt mit rechts auf und springt erst beim zweiten Druck ins Kind", () => {
    // Ein Sprung in einem Zug überspränge die Rückmeldung, dass etwas aufging.
    expect(tastenbefehl(zu, 0, "ArrowRight")).toEqual({
      art: "UMSCHALTEN",
      schluessel: zu[0].schluessel,
    });
    expect(tastenbefehl(offen, 0, "ArrowRight")).toEqual({
      art: "FOKUS",
      schluessel: offen[1].schluessel,
    });
  });

  it("tut mit rechts auf einem Blatt nichts", () => {
    const blattIndex = offen.findIndex((zeile) => zeile.art === "PROZESS");
    expect(tastenbefehl(offen, blattIndex, "ArrowRight")).toBeNull();
  });

  it("klappt mit links zu und geht sonst zum Elternknoten", () => {
    expect(tastenbefehl(offen, 0, "ArrowLeft")).toEqual({
      art: "UMSCHALTEN",
      schluessel: offen[0].schluessel,
    });
    // Von einem Blatt auf Ebene 3 zurück zur Richtungsgruppe auf Ebene 2.
    expect(tastenbefehl(offen, 2, "ArrowLeft")).toEqual({
      art: "FOKUS",
      schluessel: offen[1].schluessel,
    });
  });

  it("findet den Elternknoten auch dort, wo die Richtungsebene weggefallen ist", () => {
    // E‑45: Das Blatt steht auf Ebene 2, sein Elternknoten ist der Partner —
    // und nicht die Richtungsgruppe, die es nicht gibt.
    const zeilen = baumzeilen([OHNE_EBENE], PARTNER_EBENEN, ALLES_OFFEN);
    expect(tastenbefehl(zeilen, 2, "ArrowLeft")).toEqual({
      art: "FOKUS",
      schluessel: zeilen[0].schluessel,
    });
  });

  it("findet den Elternknoten im Projektbaum über dieselbe Regel", () => {
    const zeilen = baumzeilen(PROJEKTBAUM.knoten, PROJEKTBAUM.ebenen, ALLES_OFFEN);
    expect(tastenbefehl(zeilen, 2, "ArrowLeft")).toEqual({
      art: "FOKUS",
      schluessel: zeilen[0].schluessel,
    });
  });

  it("tut mit links auf einer zugeklappten Gruppe der obersten Ebene nichts", () => {
    expect(tastenbefehl(zu, 0, "ArrowLeft")).toBeNull();
  });

  it("wählt mit Eingabe und Leertaste ein Blatt und klappt eine Gruppe um", () => {
    const blattIndex = offen.findIndex((zeile) => zeile.art === "PROZESS");

    for (const taste of ["Enter", " "]) {
      expect(tastenbefehl(offen, blattIndex, taste)).toEqual({ art: "WAEHLEN", processId: "p1" });
      expect(tastenbefehl(offen, 0, taste)).toEqual({
        art: "UMSCHALTEN",
        schluessel: offen[0].schluessel,
      });
    }
  });

  it("übergeht jede andere Taste und jede Zeile, die es nicht gibt", () => {
    expect(tastenbefehl(offen, 0, "a")).toBeNull();
    expect(tastenbefehl(offen, 99, "ArrowDown")).toBeNull();
    expect(tastenbefehl([], 0, "ArrowDown")).toBeNull();
  });
});
