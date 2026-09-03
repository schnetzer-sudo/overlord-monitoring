import { describe, expect, it } from "vitest";

import type {
  Partnerknoten,
  Prozessbaum,
  Prozessknoten,
  Richtungsknoten,
} from "@/features/nachrichten/api";
import {
  baumzeilen,
  eingegrenzterBaum,
  ohnePfad,
  partnerSchluessel,
  partnertext,
  partnerVon,
  pfadZuProzess,
  prozessAus,
  prozessSchluessel,
  richtungsSchluessel,
  richtungsebeneFaelltWeg,
  richtungstext,
  sichtbareProzesse,
  tastenbefehl,
  zeilenbeschriftung,
  zustandstext,
} from "@/features/nachrichten/prozessbaum";
import { texteFuer } from "@/i18n";

/**
 * Die **Entscheidungen** des Prozessbaums, geprüft ohne Ansicht
 * (`docs/frontend-grundlagen.md` §9). Was nur ein gerenderter Baum zeigt, steht
 * in `tests/prozess-baum.test.tsx` — und zwar nur das.
 *
 * **Alle Prüfwerte sind erfunden** (Regel T2). Kein Fall hängt an einer Zeile
 * der geteilten Testkopie; die Zahlen aus `docs/process-view.md` §9 stehen in
 * den Kommentaren und nirgends in einer Zusicherung.
 */

const TEXTE = texteFuer("de");
const ZAHL = (wert: number) => String(wert);
const SCHWELLE = 3;

function blatt(
  processId: string,
  processName: string | null,
  nachrichten: number,
  fehler = 0,
  zustand: Prozessknoten["zustand"] = "BEWEGT",
): Prozessknoten {
  return {
    processId,
    processName,
    nachrichten,
    fehler,
    letzteBewegung: zustand === "NIE" ? null : "2025-12-29T22:00:00Z",
    zustand,
  };
}

function gruppe(richtung: string | null, prozesse: Prozessknoten[]): Richtungsknoten {
  return {
    richtung,
    anzahlProzesse: prozesse.length,
    nachrichten: prozesse.reduce((wert, prozess) => wert + prozess.nachrichten, 0),
    fehler: prozesse.reduce((wert, prozess) => wert + prozess.fehler, 0),
    prozesse,
  };
}

function knoten(partner: string | null, richtungen: Richtungsknoten[]): Partnerknoten {
  return {
    partner,
    anzahlProzesse: richtungen.reduce((wert, gruppe2) => wert + gruppe2.anzahlProzesse, 0),
    nachrichten: richtungen.reduce((wert, gruppe2) => wert + gruppe2.nachrichten, 0),
    fehler: richtungen.reduce((wert, gruppe2) => wert + gruppe2.fehler, 0),
    richtungen,
  };
}

/** Ein Partner mit **zwei** Richtungen — die Ebene bleibt stehen. */
const MIT_EBENE = knoten("ACME", [
  gruppe("EINGEHEND", [blatt("p1", "ACME Bestellung", 10, 2)]),
  gruppe("AUSGEHEND", [blatt("p2", "ACME Lieferschein", 5)]),
]);

/**
 * Ein Partner mit **einer, nicht ermittelten** Richtung — die Ebene fällt weg
 * (E‑58). Sie trüge einen Knoten „nicht ermittelt“ über zwei Blättern und
 * schriebe nichts hin.
 */
const OHNE_EBENE = knoten("BOSCH", [
  gruppe(null, [blatt("p3", "BOSCH Rechnung", 0, 0, "STILL"), blatt("p4", null, 7)]),
]);

/**
 * Ein Partner mit **einer, bekannten** Richtung — die Ebene **steht** (E‑58).
 * Der Fall aus dem Bild: `ADIENT` mit genau einem eingehenden Prozess.
 */
const EINE_RICHTUNG = knoten("CONTI", [gruppe("EINGEHEND", [blatt("p6", "CONTI Bestellung", 4)])]);

/** „nicht zugeordnet“ — bei `SUTTONS` die einzige Gruppe, und ohne Richtung. */
const OHNE_PARTNER = knoten(null, [gruppe(null, [blatt("p5", "Freier Prozess", 0, 0, "NIE")])]);

const BAUM: Prozessbaum = {
  zeitraum: "48H",
  fenster: { von: "2025-12-28T05:00:00Z", bis: "2025-12-30T05:00:00Z" },
  stilleSchwelleMonate: SCHWELLE,
  gesamt: { anzahlProzesse: 5, bewegt: 3, still: 1, nie: 1, nachrichten: 22, fehler: 2 },
  partner: [MIT_EBENE, OHNE_EBENE, OHNE_PARTNER],
};

const ALLES_OFFEN = () => true;
const ALLES_ZU = () => false;

describe("E‑58 — die Richtungsebene fällt nur weg, wo es nichts zu schreiben gibt", () => {
  it("erkennt die Fälle am Knoten und nicht an der Antwort", () => {
    expect(richtungsebeneFaelltWeg(MIT_EBENE), "zwei Richtungen").toBe(false);
    // ⚠️ **Das ist die Umkehrung vom 03.09.2026.** Bis dahin genügte „genau
    // eine Richtung“; seither muss sie auch **unbekannt** sein. Eine bekannte
    // Richtung steht immer als Ebene, sonst schriebe der Baum denselben
    // Sachverhalt an zwei Stellen verschieden.
    expect(richtungsebeneFaelltWeg(EINE_RICHTUNG), "eine bekannte Richtung").toBe(false);
    expect(richtungsebeneFaelltWeg(OHNE_EBENE), "eine unbekannte Richtung").toBe(true);
    expect(richtungsebeneFaelltWeg(OHNE_PARTNER), "ohne Partner, ohne Richtung").toBe(true);
  });

  it("lässt die Ebene auch über einem einzigen Kind stehen, wenn die Richtung bekannt ist", () => {
    // Der Fall aus dem Bild: `ADIENT` trägt einen Prozess, und „Eingehend“
    // stand deshalb als Vorsatz in seiner Zeile, während dasselbe Wort beim
    // Nachbarn `ACOME` eine Zeile war.
    const zeilen = baumzeilen([EINE_RICHTUNG], ALLES_OFFEN);

    expect(zeilen.map((zeile) => zeile.art)).toEqual(["PARTNER", "RICHTUNG", "PROZESS"]);
    expect(zeilen.map((zeile) => zeile.ebene)).toEqual([1, 2, 3]);
    expect(zeilen[1].art === "RICHTUNG" && zeilen[1].richtung).toBe("EINGEHEND");
  });

  it("gibt auch einem gepflegten, aber unbekannten Wert seine Ebene (Regel Q4)", () => {
    // Er ist **bekannt** — nur nicht übersetzbar. Er steht als Zeile da, wie er
    // im Katalog steht; „nicht ermittelt“ ist etwas anderes.
    const vierter = knoten("ACME", [gruppe("RUECKMELDUNG", [blatt("p9", "Quittung", 1)])]);
    expect(richtungsebeneFaelltWeg(vierter)).toBe(false);

    const zeilen = baumzeilen([vierter], ALLES_OFFEN);
    expect(zeilen[1].art === "RICHTUNG" && zeilen[1].richtung).toBe("RUECKMELDUNG");
    expect(zeilenbeschriftung(zeilen[1], SCHWELLE, TEXTE, ZAHL)).toContain("RUECKMELDUNG");
  });

  it("lässt bei zwei Richtungen die Ebene stehen und die Blätter auf Ebene 3", () => {
    const zeilen = baumzeilen([MIT_EBENE], ALLES_OFFEN);

    expect(zeilen.map((zeile) => zeile.art)).toEqual([
      "PARTNER",
      "RICHTUNG",
      "PROZESS",
      "RICHTUNG",
      "PROZESS",
    ]);
    expect(zeilen.map((zeile) => zeile.ebene)).toEqual([1, 2, 3, 2, 3]);
  });

  it("überspringt die Ebene bei nicht ermittelter Richtung — und schreibt nichts nach", () => {
    // Der Fall von `VOTG`: keine einzige kuratierte Richtung, 133 Knoten „nicht
    // ermittelt“, die nichts ordnen. Die Ebene fällt weg, und die Blätter
    // tragen **keinen Ersatz** — weder ein Zeichen noch ein Wort.
    const zeilen = baumzeilen([OHNE_EBENE], ALLES_OFFEN);

    expect(zeilen.map((zeile) => zeile.art)).toEqual(["PARTNER", "PROZESS", "PROZESS"]);
    expect(zeilen.map((zeile) => zeile.ebene)).toEqual([1, 2, 2]);
  });

  it("gibt einem Blatt in keinem Fall eine eigene Richtung mit", () => {
    // **Die Abwesenheit ist die Regel** (E‑58): Wo die Richtung bekannt ist,
    // trägt sie die Ebene; wo nicht, gibt es sie nicht. Ein Feld an der
    // Blattzeile wäre die zweite Schreibweise, die abgeschafft worden ist.
    const alle = [
      ...baumzeilen([MIT_EBENE], ALLES_OFFEN),
      ...baumzeilen([EINE_RICHTUNG], ALLES_OFFEN),
      ...baumzeilen([OHNE_EBENE], ALLES_OFFEN),
      ...baumzeilen([OHNE_PARTNER], ALLES_OFFEN),
    ].filter((zeile) => zeile.art === "PROZESS");

    expect(alle.length).toBeGreaterThan(0);
    expect(alle.every((zeile) => !("richtung" in zeile))).toBe(true);
  });
});

describe("Die sichtbaren Zeilen", () => {
  it("zeigt zugeklappt genau die Partner", () => {
    const zeilen = baumzeilen(BAUM.partner, ALLES_ZU);

    expect(zeilen).toHaveLength(3);
    expect(zeilen.every((zeile) => zeile.art === "PARTNER")).toBe(true);
    expect(zeilen.every((zeile) => zeile.ebene === 1)).toBe(true);
  });

  it("zählt Position und Geschwister je Ebene und nicht über die ganze Liste", () => {
    const zeilen = baumzeilen(BAUM.partner, ALLES_OFFEN);

    // Die drei Partner sind 1..3 von 3.
    const partner = zeilen.filter((zeile) => zeile.art === "PARTNER");
    expect(partner.map((zeile) => zeile.position)).toEqual([1, 2, 3]);
    expect(partner.every((zeile) => zeile.geschwister === 3)).toBe(true);

    // Die beiden Richtungen unter ACME sind 1..2 von 2.
    const richtungen = zeilen.filter((zeile) => zeile.art === "RICHTUNG");
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

  it("gibt jeder Zeile einen eindeutigen Schlüssel", () => {
    const schluessel = baumzeilen(BAUM.partner, ALLES_OFFEN).map((zeile) => zeile.schluessel);
    expect(new Set(schluessel).size).toBe(schluessel.length);
  });

  it("unterscheidet „nicht zugeordnet“ von einem Partner, der leer heißt", () => {
    // Beide sind für den Nutzer verschieden, und beide müssen einen eigenen
    // Aufklappzustand haben können (Regel Q4).
    expect(partnerSchluessel(null)).not.toBe(partnerSchluessel(""));
    expect(richtungsSchluessel("A", null)).not.toBe(richtungsSchluessel("A", ""));
  });
});

describe("Der Pfad zum gewählten Prozess", () => {
  it("nennt Partner und Richtung, wenn die Ebene steht", () => {
    expect(pfadZuProzess(BAUM.partner, "p1")).toEqual([
      partnerSchluessel("ACME"),
      richtungsSchluessel("ACME", "EINGEHEND"),
    ]);
  });

  it("nennt nur den Partner, wenn die Ebene weggefallen ist", () => {
    // Sonst stünde in der URL-Ableitung ein Knoten, den es im Baum nicht gibt —
    // und der Partner bliebe zu.
    expect(pfadZuProzess(BAUM.partner, "p3")).toEqual([partnerSchluessel("BOSCH")]);
  });

  it("bleibt leer, wenn die Kennung in diesem Baum nicht vorkommt", () => {
    // Ein geteilter Link kann eine `ProcessID` eines anderen Mandanten tragen.
    expect(pfadZuProzess(BAUM.partner, "fremd")).toEqual([]);
    expect(pfadZuProzess(BAUM.partner, null)).toEqual([]);
  });

  it("öffnet den Pfad tatsächlich — die Zeile des Prozesses steht dann in der Liste", () => {
    const pfad = new Set(pfadZuProzess(BAUM.partner, "p1"));
    const zeilen = baumzeilen(BAUM.partner, (schluessel) => pfad.has(schluessel));

    expect(zeilen.map((zeile) => zeile.schluessel)).toContain(prozessSchluessel("p1"));
    // Und nur dieser Ast: Der zweite Partner bleibt zu.
    expect(zeilen.map((zeile) => zeile.schluessel)).not.toContain(prozessSchluessel("p3"));
  });
});

describe("Der Aufklappzustand entlang eines Pfades", () => {
  it("vergisst genau die Umschaltungen des neuen Pfades und sonst keine", () => {
    // Der Fall, der die Funktion nötig gemacht hat: Ein Partner, den der Nutzer
    // einmal zugeklappt hat, bliebe sonst zu — auch wenn ein später gewählter
    // Prozess unter ihm hängt. `??` fällt bei `false` nicht durch.
    const bisher = new Map([
      [partnerSchluessel("ACME"), false],
      [partnerSchluessel("BOSCH"), true],
    ]);

    const neu = ohnePfad(bisher, pfadZuProzess(BAUM.partner, "p1"));

    expect(neu.has(partnerSchluessel("ACME"))).toBe(false);
    // Was der Nutzer anderswo aufgeklappt hat, bleibt aufgeklappt.
    expect(neu.get(partnerSchluessel("BOSCH"))).toBe(true);
  });

  it("räumt auch die Richtungsebene, wenn sie im Pfad liegt", () => {
    const pfad = pfadZuProzess(BAUM.partner, "p1");
    expect(pfad).toHaveLength(2);

    const bisher = new Map(pfad.map((schluessel) => [schluessel, false]));
    expect(ohnePfad(bisher, pfad).size).toBe(0);
  });

  it("gibt dieselbe Menge zurück, wenn nichts zu räumen ist", () => {
    // Kein neues Objekt heißt: kein neues Rendern — die Funktion läuft bei
    // jedem Prozesswechsel.
    const bisher = new Map([[partnerSchluessel("BOSCH"), true]]);
    expect(ohnePfad(bisher, pfadZuProzess(BAUM.partner, "p1"))).toBe(bisher);
    expect(ohnePfad(bisher, [])).toBe(bisher);
  });
});

describe("Die Eingrenzung", () => {
  it("lässt ohne Begriff und ohne Schalter alles stehen", () => {
    const gefiltert = eingegrenzterBaum(BAUM.partner, "", false);
    expect(gefiltert).toHaveLength(3);
    expect(sichtbareProzesse(gefiltert)).toBe(BAUM.gesamt.anzahlProzesse);
  });

  it("rechnet die Summen aus den Blättern — und trifft ohne Eingrenzung die gelieferten", () => {
    // **Die Invariante**: Die Kennzahlen sind über die Blätter additiv, und der
    // Dienst bildet sie genauso. Wäre das nicht so, zeigte eine eingegrenzte
    // Ansicht andere Zahlen als eine ungefilterte — und beide sähen richtig aus.
    const gefiltert = eingegrenzterBaum(BAUM.partner, "", false);

    for (const [index, knoten2] of gefiltert.entries()) {
      expect(knoten2.anzahlProzesse).toBe(BAUM.partner[index].anzahlProzesse);
      expect(knoten2.nachrichten).toBe(BAUM.partner[index].nachrichten);
      expect(knoten2.fehler).toBe(BAUM.partner[index].fehler);
    }
  });

  it("behält bei einem Partnertreffer alle seine Prozesse", () => {
    // Wer nach einem Partner sucht, will dessen Prozesse sehen — und nicht die
    // Teilmenge, deren Namen zufällig denselben Text tragen.
    const gefiltert = eingegrenzterBaum(BAUM.partner, "bosch", false);

    expect(gefiltert).toHaveLength(1);
    expect(gefiltert[0].partner).toBe("BOSCH");
    expect(gefiltert[0].anzahlProzesse).toBe(2);
  });

  it("lässt einen Partner stehen, dessen Kind trifft — mit nur diesem Kind", () => {
    const gefiltert = eingegrenzterBaum(BAUM.partner, "lieferschein", false);

    expect(gefiltert).toHaveLength(1);
    expect(gefiltert[0].partner).toBe("ACME");
    expect(gefiltert[0].anzahlProzesse).toBe(1);
    // Die leere Richtungsgruppe fällt mit weg — kein leerer Ast.
    expect(gefiltert[0].richtungen).toHaveLength(1);
    expect(gefiltert[0].richtungen[0].richtung).toBe("AUSGEHEND");
    // Und die Zahlen beschreiben, was zu sehen ist.
    expect(gefiltert[0].nachrichten).toBe(5);
  });

  it("ist unabhängig von der Groß- und Kleinschreibung", () => {
    expect(eingegrenzterBaum(BAUM.partner, "ACME", false)).toHaveLength(1);
    expect(eingegrenzterBaum(BAUM.partner, "acme", false)).toHaveLength(1);
  });

  it("übergeht Leerraum am Rand", () => {
    expect(eingegrenzterBaum(BAUM.partner, "   ", false)).toHaveLength(3);
    expect(eingegrenzterBaum(BAUM.partner, "  bosch  ", false)).toHaveLength(1);
  });

  it("greift nicht auf einen Namen zu, den es nicht gibt", () => {
    // `processName` darf `null` sein, und `null` trifft nichts — es wird nicht
    // stillschweigend als leerer Text behandelt.
    const gefiltert = eingegrenzterBaum([OHNE_EBENE], "rechnung", false);
    expect(gefiltert[0].anzahlProzesse).toBe(1);
    expect(gefiltert[0].richtungen[0].prozesse[0].processId).toBe("p3");
  });

  it("filtert nicht über die Richtung", () => {
    // „Eingehend“ ist ein Text der Oberfläche und stünde in zwei Sprachen
    // verschieden da. Gefiltert wird über die Werte der Antwort (Regel Q4).
    expect(eingegrenzterBaum(BAUM.partner, "eingehend", false)).toHaveLength(0);
  });

  it("blendet mit dem Schalter aus, was im Zeitraum nichts getragen hat", () => {
    const gefiltert = eingegrenzterBaum(BAUM.partner, "", true);

    // p3 (still, 0) und p5 (nie, 0) fallen weg; „nicht zugeordnet“ verliert
    // damit seinen einzigen Prozess und verschwindet ganz.
    expect(sichtbareProzesse(gefiltert)).toBe(3);
    expect(gefiltert.map((knoten2) => knoten2.partner)).toEqual(["ACME", "BOSCH"]);
  });

  it("verbindet Begriff und Schalter mit UND", () => {
    expect(sichtbareProzesse(eingegrenzterBaum(BAUM.partner, "bosch", true))).toBe(1);
  });

  it("gibt eine leere Liste zurück, wenn nichts passt — und keinen leeren Ast", () => {
    expect(eingegrenzterBaum(BAUM.partner, "gibtesnicht", false)).toEqual([]);
  });
});

describe("Der gewählte Prozess", () => {
  it("wird im ganzen Baum gefunden, samt seiner Zuordnung", () => {
    expect(prozessAus(BAUM, "p2")?.processName).toBe("ACME Lieferschein");
    expect(partnerVon(BAUM, "p2")).toEqual({ partner: "ACME", richtung: "AUSGEHEND" });
  });

  it("unterscheidet „nicht gefunden“ von „nicht zugeordnet“", () => {
    // `undefined` heißt: Die Kennung kommt in diesem Baum nicht vor — dann zeigt
    // die Ansicht die Kennung. `null` heißt: nicht zugeordnet (Regel Q4).
    expect(partnerVon(BAUM, "fremd")).toBeUndefined();
    expect(partnerVon(BAUM, "p5")).toEqual({ partner: null, richtung: null });
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
    const nieZeile = baumzeilen([OHNE_PARTNER], ALLES_OFFEN)[1];
    const name = zeilenbeschriftung(nieZeile, SCHWELLE, TEXTE, ZAHL);

    expect(name).toContain("Freier Prozess");
    expect(name).toContain("Nachrichten: 0");
    // Die Gegenprobe zur Gegenprobe: „still“ steht weiterhin drin, sonst
    // prüfte dieser Test nur, dass der Name überhaupt kurz ist.
    expect(name).not.toContain("nie");
    expect(
      zeilenbeschriftung(baumzeilen([OHNE_EBENE], ALLES_OFFEN)[1], SCHWELLE, TEXTE, ZAHL),
    ).toContain(zustandstext("STILL", SCHWELLE, TEXTE) as string);
  });

  it("nennt im vorgelesenen Namen die Ebene, die Zahlen und den Zustand", () => {
    const zeilen = baumzeilen(BAUM.partner, ALLES_OFFEN);
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

  it("nennt keine Fehler, wo es keine gibt", () => {
    // Eine `0` vorzulesen behauptet eine Aussage, die die Zeile nicht macht.
    const zeilen = baumzeilen([OHNE_EBENE], ALLES_OFFEN);
    const ohneFehler = zeilen[2];
    expect(zeilenbeschriftung(ohneFehler, SCHWELLE, TEXTE, ZAHL)).not.toContain("Fehler");
  });

  it("nennt die Richtung an der Ebene und an keinem Blatt (E‑58)", () => {
    // **Sichtbare und vorgelesene Fassung dürfen nicht auseinanderlaufen.** Bis
    // zum 03.09.2026 stand „nicht ermittelt“ im Namen genau der Blätter, die
    // sichtbar nichts trugen.
    const richtungszeile = baumzeilen([EINE_RICHTUNG], ALLES_OFFEN)[1];
    expect(zeilenbeschriftung(richtungszeile, SCHWELLE, TEXTE, ZAHL)).toContain(
      TEXTE.prozesse.richtung.EINGEHEND,
    );

    for (const knotenmenge of [[MIT_EBENE], [EINE_RICHTUNG], [OHNE_EBENE], [OHNE_PARTNER]]) {
      for (const zeile of baumzeilen(knotenmenge, ALLES_OFFEN)) {
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
    const still = baumzeilen([OHNE_EBENE], ALLES_OFFEN)[1];
    expect(zeilenbeschriftung(still, SCHWELLE, TEXTE, ZAHL)).toContain(
      zustandstext("STILL", SCHWELLE, TEXTE) as string,
    );
  });
});

describe("Die Tastatur — das WAI‑ARIA-Muster", () => {
  const offen = baumzeilen(BAUM.partner, ALLES_OFFEN);
  const zu = baumzeilen(BAUM.partner, ALLES_ZU);

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
    const zeilen = baumzeilen([OHNE_EBENE], ALLES_OFFEN);
    expect(tastenbefehl(zeilen, 2, "ArrowLeft")).toEqual({
      art: "FOKUS",
      schluessel: zeilen[0].schluessel,
    });
  });

  it("tut mit links auf einem zugeklappten Partner der obersten Ebene nichts", () => {
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
