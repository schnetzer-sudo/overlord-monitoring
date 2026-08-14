import { describe, expect, it } from "vitest";

import { texteFuer } from "@/i18n";
import type { BamTrefferWert } from "@/features/nachrichten/api";
import {
  HOECHSTENS_BEGRIFFE,
  PRAEFIX_FENSTER_TAGE,
  abweichendeVarianten,
  alsAbfrage,
  alsParameter,
  ausParameter,
  begriffeAus,
  ergaenze,
  jahresfensterAb,
  modusAus,
  modusAusAntwort,
  nulltrefferHinweis,
  parseAsBegriffe,
  parseAsSuchmodus,
  praefixfenster,
  spanneInTagen,
  trefferTypen,
  zeigtPraefixAngebot,
  type Suchbegriff,
  type Suchzustand,
  type VorigeRunde,
} from "@/features/nachrichten/suche";

/**
 * **Die Entscheidungen der Belegsuche — als reine Funktionen.**
 *
 * Geprüft wird, was entschieden wird, nicht wie es aussieht
 * (`docs/frontend-grundlagen.md` §9). Zwei Sätze dieses Schritts sind ohne
 * gerenderten Baum nicht belegbar und stehen deshalb in
 * `tests/suche-marken.test.tsx`; alles Übrige steht hier.
 */

const zustand = (teil: Partial<Suchzustand>): Suchzustand => ({
  begriff: null,
  von: null,
  bis: null,
  modus: null,
  nachricht: null,
  ...teil,
});

describe("Ein Begriff und seine Parameterform", () => {
  it("teilt am ERSTEN Doppelpunkt — alles dahinter ist Wert", () => {
    // Ob ein BAM-Wert selbst einen Doppelpunkt enthalten kann, ist nicht
    // gemessen. Mit Pflichttrenner und Teilung am ersten Vorkommen ist die Frage
    // gegenstandslos, statt nach Regel Q4 geraten zu werden.
    expect(ausParameter("9012:4711:815")).toEqual({ typ: 9012, wert: "4711:815" });
    expect(ausParameter(":4711:815")).toEqual({ typ: null, wert: "4711:815" });
  });

  it("verlangt den Trenner und einen Wert", () => {
    expect(ausParameter("4711815")).toBeNull();
    expect(ausParameter("9012:")).toBeNull();
    expect(ausParameter(":")).toBeNull();
    expect(ausParameter(":   ")).toBeNull();
  });

  it("lässt keinen Typteil durch, der keine Typnummer ist", () => {
    expect(ausParameter("90x:4711")).toBeNull();
    expect(ausParameter("9,5:4711")).toBeNull();
    expect(ausParameter("-1:4711")).toBeNull();
    expect(ausParameter("9012:4711")).toEqual({ typ: 9012, wert: "4711" });
  });

  it("beschneidet die Ränder, aber nicht den Wert selbst", () => {
    expect(ausParameter(" 9012 : 4711 ")).toEqual({ typ: 9012, wert: "4711" });
    expect(ausParameter(":0Z3 915 902 D")).toEqual({ typ: null, wert: "0Z3 915 902 D" });
  });

  it("ist als Parameterform eindeutig — der Typteil trägt nie einen Doppelpunkt", () => {
    const mitTyp: Suchbegriff = { typ: 9012, wert: "123" };
    const ohneTyp: Suchbegriff = { typ: null, wert: "9012:123" };

    expect(alsParameter(mitTyp)).toBe("9012:123");
    expect(alsParameter(ohneTyp)).toBe(":9012:123");
    expect(alsParameter(mitTyp)).not.toBe(alsParameter(ohneTyp));
  });
});

describe("Die Begriffe stehen als wiederholter Parameter in der URL", () => {
  /**
   * **Der Rundlauf: URL → Zustand → URL.** Ein Neuladen ergibt dieselbe Suche —
   * das ist die ganze Zusage hinter „der Zustand steht in der URL".
   */
  it("liest und schreibt dieselbe Menge in derselben Reihenfolge", () => {
    const roh = ["9012:4711815", ":80337215", "0:0050"];

    const gelesen = parseAsBegriffe.parse(roh);
    expect(gelesen).toEqual([
      { typ: 9012, wert: "4711815" },
      { typ: null, wert: "80337215" },
      { typ: 0, wert: "0050" },
    ]);
    expect(parseAsBegriffe.serialize(gelesen!)).toEqual(roh);
  });

  it("übergeht einen unbrauchbaren Begriff, statt ihn anzuzeigen", () => {
    // Ein solcher Wert entsteht nur aus einer von Hand gebauten URL — und das
    // Backend wiese ihn mit 400 ab. Dieselbe Behandlung wie bei jedem unbekannten
    // Suchparameter der Nachrichtenliste: übergehen, nicht belehren.
    expect(parseAsBegriffe.parse(["ohne-trenner", "9012:4711"])).toEqual([
      { typ: 9012, wert: "4711" },
    ]);
  });

  it("räumt den Parameter aus der URL, wenn kein Begriff übrig bleibt", () => {
    // `null` heißt für nuqs: Schlüssel entfernen. Ohne Begriff ist die URL leer.
    expect(parseAsBegriffe.parse([])).toBeNull();
    expect(parseAsBegriffe.parse(["kaputt"])).toBeNull();
  });

  it("vergleicht Begriffe über ihren Inhalt und nicht über die Referenz", () => {
    expect(parseAsBegriffe.eq([{ typ: 9012, wert: "1" }], [{ typ: 9012, wert: "1" }])).toBe(true);
    expect(parseAsBegriffe.eq([{ typ: 9012, wert: "1" }], [{ typ: 9013, wert: "1" }])).toBe(false);
  });
});

describe("Die Abfrage an das Backend", () => {
  it("schickt jeden Begriff als eigenen Parameter", () => {
    const abfrage = alsAbfrage(
      zustand({
        begriff: [
          { typ: 9012, wert: "4711815" },
          { typ: null, wert: "80337215" },
        ],
      }),
    );

    const parameter = new URLSearchParams(abfrage.slice(1));
    expect(parameter.getAll("begriff")).toEqual(["9012:4711815", ":80337215"]);
  });

  it("schickt das Zeitfenster nur, wenn eines gesetzt ist", () => {
    const ohne = alsAbfrage(zustand({ begriff: [{ typ: null, wert: "1" }] }));
    expect(new URLSearchParams(ohne.slice(1)).has("von")).toBe(false);

    const mit = alsAbfrage(
      zustand({
        begriff: [{ typ: null, wert: "1" }],
        von: new Date("2025-11-30T00:00:00.000Z"),
        bis: new Date("2025-12-30T00:00:00.000Z"),
      }),
    );
    const parameter = new URLSearchParams(mit.slice(1));
    expect(parameter.get("von")).toBe("2025-11-30T00:00:00.000Z");
    expect(parameter.get("bis")).toBe("2025-12-30T00:00:00.000Z");
  });

  /**
   * **`nachricht` steht in der URL und in keiner Abfrage.** Dieselbe Trennung wie
   * in der Nachrichtenliste — und hier wiegt sie schwerer: Träte die geöffnete
   * Nachricht in den Abfrageschlüssel ein, liefe bei jedem Klick auf eine Zeile
   * die teuerste Abfrage dieses Projekts noch einmal.
   */
  it("ist mit und ohne geöffnete Nachricht Zeichen für Zeichen dieselbe", () => {
    const begriff = [{ typ: 9012, wert: "4711815" }];

    expect(
      alsAbfrage(zustand({ begriff, nachricht: "8f3a1c2e-0000-4000-8000-000000000001" })),
    ).toBe(alsAbfrage(zustand({ begriff })));
  });

  it("bleibt leer, solange kein Begriff dasteht", () => {
    expect(alsAbfrage(zustand({}))).toBe("");
    expect(begriffeAus(zustand({}))).toEqual([]);
  });

  /**
   * **Ohne `modus` verhält sich der Endpunkt Zeichen für Zeichen wie vor Teil 4**
   * — das ist eine Zusage, die `BamSucheDbIT` an den beiden Rümpfen festhält
   * (`docs/bam-suche.md` §16). Sie anzunehmen kostet nichts und hält den
   * Abfrageschlüssel des Zwischenspeichers für den Normalfall unverändert.
   */
  it("schickt `modus` nur im Präfixmodus", () => {
    const begriff = [{ typ: 9012, wert: "4711815" }];

    expect(alsAbfrage(zustand({ begriff }))).toBe(alsAbfrage(zustand({ begriff, modus: "exakt" })));
    expect(new URLSearchParams(alsAbfrage(zustand({ begriff })).slice(1)).has("modus")).toBe(false);

    const praefix = new URLSearchParams(
      alsAbfrage(zustand({ begriff, modus: "praefix" })).slice(1),
    );
    expect(praefix.get("modus")).toBe("praefix");
  });
});

describe("Der Modus steht in der URL", () => {
  /**
   * **Er beschreibt einen Ausschnitt und keine begonnene Eingabe** — damit fällt
   * er im Zweischritt aus `docs/frontend-grundlagen.md` §8 auf die erste Antwort:
   * Die URL kann ihn ausdrücken, also steht er darin und ein geteilter Link zeigt
   * dieselbe Suche.
   */
  it("liest und schreibt beide Werte", () => {
    expect(parseAsSuchmodus.parse("praefix")).toBe("praefix");
    expect(parseAsSuchmodus.parse("exakt")).toBe("exakt");
    expect(parseAsSuchmodus.serialize("praefix")).toBe("praefix");
  });

  /** Ein unbekannter Wert wäre am Endpunkt `400 suchmodus-ungueltig` — er kommt gar nicht erst hinein. */
  it("lässt keinen unbekannten Wert in die URL", () => {
    expect(parseAsSuchmodus.parse("prefix")).toBeNull();
    expect(parseAsSuchmodus.parse("PRAEFIX")).toBeNull();
    expect(parseAsSuchmodus.parse("")).toBeNull();
  });

  /** `null` heißt „keine Angabe" und damit `exakt` — die Vorgabe gehört dem Backend. */
  it("liest das Fehlen des Parameters als „exakt“", () => {
    expect(modusAus(zustand({}))).toBe("exakt");
    expect(modusAus(zustand({ modus: "praefix" }))).toBe("praefix");
  });

  /**
   * **Die Antwort schreibt ihn groß, der Parameter klein** (`docs/bam-suche.md`
   * §20). Ein unbekannter Wert ergibt `undefined` und **nicht** „exakt": Angeboten
   * wird nur, was nachweislich noch nicht gelaufen ist.
   */
  it("übersetzt den Modus der Antwort in den der URL", () => {
    expect(modusAusAntwort("EXAKT")).toBe("exakt");
    expect(modusAusAntwort("PRAEFIX")).toBe("praefix");
    expect(modusAusAntwort(undefined)).toBeUndefined();
  });
});

describe("Das Angebot, über den Anfang der Nummer zu suchen", () => {
  const lage = (teil: Partial<Parameters<typeof zeigtPraefixAngebot>[0]>) =>
    zeigtPraefixAngebot({
      modus: "exakt",
      treffer: 0,
      begriffe: 1,
      abgebrochen: false,
      ...teil,
    });

  it("erscheint, wenn die exakte Suche mit Begriffen leer ausgegangen ist", () => {
    expect(lage({})).toBe(true);
  });

  /**
   * **Bei Treffern gibt es die Kehrseite**, die im leeren Ergebnis fehlt: Schon ein
   * vollständig eingetippter Wert findet als Präfix **23 Nachrichten statt einer**
   * (M49‑3).
   */
  it("erscheint nicht, sobald etwas gefunden wurde", () => {
    expect(lage({ treffer: 1 })).toBe(false);
  });

  it("erscheint nicht, wenn schon präfixweise gesucht wurde", () => {
    expect(lage({ modus: "praefix" })).toBe(false);
  });

  /** Ein unbekannter gemeldeter Modus zählt nicht als „exakt“. */
  it("erscheint nicht, wenn unklar ist, welcher Vergleich gelaufen ist", () => {
    expect(lage({ modus: undefined })).toBe(false);
  });

  it("erscheint nicht ohne Begriff", () => {
    expect(lage({ begriffe: 0 })).toBe(false);
  });

  /**
   * **Der Fall, der am leichtesten durchrutscht.** Wer gerade an der Zeitgrenze
   * gescheitert ist, bekommt keine **teurere** Suche angeboten — der Präfixmodus
   * ist die teuerste Zugriffsform dieses Projekts (M50: 3,851 s über dreißig Tage
   * im schlimmsten bekannten Fall).
   */
  it("erscheint nach einem Abbruch ausdrücklich nicht", () => {
    expect(lage({ abgebrochen: true })).toBe(false);
  });
});

describe("Das Fenster, über das der Präfixmodus läuft", () => {
  const bis = new Date("2025-12-30T04:14:00.000Z");

  /**
   * **`bis` bleibt stehen, `von` rückt nach** — das neue Fenster ist ein
   * **Ausschnitt** des alten und kein anderes. Genau daran hängt die Begründung,
   * dass sich nicht zwei Dinge auf einmal ändern: Die exakte Suche über das große
   * Fenster war leer, über einen Ausschnitt daraus ist sie zwangsläufig ebenfalls
   * leer (`docs/bam-suche.md` §23).
   */
  it("verkleinert ein Jahresfenster auf den Deckel", () => {
    const jahr = jahresfensterAb(bis);

    const enger = praefixfenster(jahr);

    expect(enger).not.toBeNull();
    expect(enger!.bis.getTime()).toBe(bis.getTime());
    expect(spanneInTagen(enger!.von, enger!.bis)).toBe(PRAEFIX_FENSTER_TAGE);
    expect(enger!.von.getTime()).toBeGreaterThan(jahr.von.getTime());
  });

  /**
   * **`null` heißt „es ändert sich nichts"**, und der Aufrufer schreibt dann auch
   * nichts: War keine Zeit gewählt, bleibt die Vorgabe des Backends die Vorgabe
   * des Backends — statt still zu einem eigenen Zeitpunkt in der URL zu werden.
   */
  it("lässt ein Fenster in Höhe des Deckels unangetastet", () => {
    expect(praefixfenster({ von: new Date(bis.getTime() - TAGE(30)), bis })).toBeNull();
  });

  it("lässt ein kleineres Fenster unangetastet", () => {
    expect(praefixfenster({ von: new Date(bis.getTime() - TAGE(7)), bis })).toBeNull();
  });

  /**
   * Der Deckel steht hier **und** im Backend (`BamSuchfilter.PRAEFIX_FENSTER_MAXIMUM`),
   * und das ist Absicht — dieselbe Bauform wie bei {@link HOECHSTENS_BEGRIFFE}.
   * Wer die Zahl ändert, ändert beide.
   */
  it("hält den Deckel bei dreißig Tagen", () => {
    expect(PRAEFIX_FENSTER_TAGE).toBe(30);
  });
});

const TAGE = (anzahl: number) => anzahl * 24 * 60 * 60 * 1000;

describe("Ein Begriff kommt dazu", () => {
  const acht: Suchbegriff[] = Array.from({ length: HOECHSTENS_BEGRIFFE }, (_, nummer) => ({
    typ: null,
    wert: String(nummer),
  }));

  it("legt einen neuen Begriff ab", () => {
    const ergebnis = ergaenze([{ typ: null, wert: "1" }], { typ: 9012, wert: "2" });

    expect(ergebnis.begriffe).toHaveLength(2);
    expect(ergebnis.doppelt).toBeNull();
    expect(ergebnis.voll).toBe(false);
  });

  /**
   * **Ein doppelter Begriff erzeugt keine zweite Marke.** Gleicher Typ *und*
   * gleicher Wert — die vorhandene meldet sich stattdessen, und ihr Schlüssel
   * kommt zurück, damit die Ansicht weiß, welche.
   */
  it("legt einen doppelten Begriff nicht ab und nennt die vorhandene Marke", () => {
    const vorher: Suchbegriff[] = [{ typ: 9012, wert: "4711815" }];

    const ergebnis = ergaenze(vorher, { typ: 9012, wert: "4711815" });

    expect(ergebnis.begriffe).toBe(vorher);
    expect(ergebnis.doppelt).toBe("9012:4711815");
  });

  /**
   * **Derselbe Wert unter einem anderen Typ ist ein anderer Begriff.** Bei 4,17
   * Prozent der Paare steht derselbe Wert unter mehreren Typen (M37) — ein
   * Vergleich über den Wert allein verschlucke hier den zweiten.
   */
  it("hält denselben Wert unter zwei Typen auseinander", () => {
    const ergebnis = ergaenze([{ typ: 9006, wert: "0050" }], { typ: 9016, wert: "0050" });

    expect(ergebnis.doppelt).toBeNull();
    expect(ergebnis.begriffe).toHaveLength(2);
  });

  /**
   * **Das Geländer bei acht.** Es ist keine fachliche Grenze, sondern die Zahl
   * der Join-Reihenfolgen, die der Optimierer durchprobiert; gemessen ist bis
   * fünf (M42‑2, M47). Der neunte Begriff wird nicht abgelegt — und das Feld
   * sperrt sein `+`, statt den Nutzer in ein `400` laufen zu lassen.
   */
  it("sperrt den neunten Begriff und lässt die Liste unverändert", () => {
    const ergebnis = ergaenze(acht, { typ: null, wert: "neun" });

    expect(ergebnis.voll).toBe(true);
    expect(ergebnis.begriffe).toBe(acht);
    expect(ergebnis.begriffe).toHaveLength(HOECHSTENS_BEGRIFFE);
  });

  it("nimmt den achten noch an", () => {
    const ergebnis = ergaenze(acht.slice(0, HOECHSTENS_BEGRIFFE - 1), { typ: null, wert: "acht" });

    expect(ergebnis.voll).toBe(false);
    expect(ergebnis.begriffe).toHaveLength(HOECHSTENS_BEGRIFFE);
  });
});

describe("Die Nulltreffer-Zeile", () => {
  const runde = (teil: Partial<VorigeRunde>): VorigeRunde => ({
    begriffe: 2,
    treffer: 12,
    abgeschnitten: false,
    ...teil,
  });

  /**
   * *„Mit diesem Begriff: 0. Ohne ihn: 12."* — sie kostet **keine** zusätzliche
   * Abfrage, weil vor jedem `+` schon gesucht wurde.
   */
  it("nennt die vorige Runde, wenn ein Begriff dazugekommen ist und nichts mehr trifft", () => {
    expect(nulltrefferHinweis(runde({}), 3, 0)).toEqual(runde({}));
  });

  it("erscheint nicht, solange es Treffer gibt", () => {
    expect(nulltrefferHinweis(runde({}), 3, 4)).toBeNull();
  });

  /**
   * **Sie erscheint nur, wenn wirklich ein Begriff dazugekommen ist.** Wer das
   * Zeitfenster verkleinert und dabei auf null fällt, bekommt sie nicht: „ohne
   * ihn" benennte dann etwas, das gar nicht die Ursache war.
   */
  it("erscheint nicht, wenn sich nur das Zeitfenster geändert hat", () => {
    expect(nulltrefferHinweis(runde({}), 2, 0)).toBeNull();
  });

  it("erscheint nicht, wenn die vorige Runde selbst leer war", () => {
    expect(nulltrefferHinweis(runde({ treffer: 0 }), 3, 0)).toBeNull();
    expect(nulltrefferHinweis(null, 1, 0)).toBeNull();
  });

  /**
   * **War die vorige Runde abgeschnitten, ist ihre Zahl die Seitengröße und
   * nicht die Trefferzahl.** Die Zeile muss das sagen können — sie gibt deshalb
   * die *ganze* Runde zurück und nicht nur eine Zahl, und beide Sprachen halten
   * dafür einen eigenen Satz bereit. Gefunden in der Sichtprüfung am
   * 13.08.2026, wo die Zeile „Ohne ihn: 50" schrieb, obwohl es mehr waren.
   */
  it("reicht die Abschneidung mit durch, statt eine Zahl zu behaupten", () => {
    expect(
      nulltrefferHinweis(runde({ treffer: 50, abgeschnitten: true }), 3, 0)?.abgeschnitten,
    ).toBe(true);

    for (const sprache of ["de", "en"] as const) {
      const texte = texteFuer(sprache).suche.ergebnis;
      expect(texte.nulltrefferAbgeschnitten).toContain("{anzahl}");
      expect(texte.nulltrefferAbgeschnitten).not.toBe(texte.nulltreffer);
    }
  });
});

describe("Die Spalte „Treffer“", () => {
  const wert = (typ: number, bezeichnung: string): BamTrefferWert => ({
    typ,
    bezeichnung,
    wert: "0050",
  });

  it("zeigt den ersten Typ und zählt die übrigen", () => {
    expect(
      trefferTypen([wert(9006, "Lieferschein-Nr._L_SAP"), wert(9021, "Transportnummer")]),
    ).toEqual({
      erste: "Lieferschein-Nr._L_SAP",
      weitere: 1,
      alle: ["Lieferschein-Nr._L_SAP", "Transportnummer"],
    });
  });

  /**
   * **Gezählt werden verschiedene Typen und nicht Zeilen.** Zwei Werte desselben
   * Typs sind eine Belegart; ein `+1` dahinter behauptete eine Vielfalt, die es
   * nicht gibt.
   */
  it("zählt zwei Werte desselben Typs als eine Belegart", () => {
    const typen = trefferTypen([wert(9012, "Charge_L_SAP"), wert(9012, "Charge_L_SAP")]);

    expect(typen?.weitere).toBe(0);
    expect(typen?.alle).toEqual(["Charge_L_SAP"]);
  });

  it("bleibt leer, wenn nichts dasteht", () => {
    expect(trefferTypen([])).toBeNull();
  });
});

describe("Die Varianten der Normalisierung", () => {
  it("meldet nur, was von der Eingabe abweicht", () => {
    expect(abweichendeVarianten("80337215", ["80337215", "0080337215"])).toEqual(["0080337215"]);
  });

  it("meldet nichts, wenn nur die Eingabe selbst gesucht wurde", () => {
    expect(abweichendeVarianten("4711815", ["4711815"])).toEqual([]);
  });
});

describe("Das Jahresfenster", () => {
  /**
   * **Verankert am Fenster aus der Antwort und nicht an der Browseruhr.** Die
   * Anwendungsuhr steht im Profil `dev` Monate hinter der realen Zeit; ein aus
   * `Date.now()` gerechnetes Fenster liefe an den Daten vorbei (Regel Z1).
   */
  it("rechnet vom gelieferten Ende zurück", () => {
    const bis = new Date("2025-12-30T00:00:00.000Z");

    const fenster = jahresfensterAb(bis);

    expect(fenster.bis).toBe(bis);
    expect(spanneInTagen(fenster.von, fenster.bis)).toBe(365);
  });

  /**
   * **365 Tage und nicht „ein Kalenderjahr".** Die Grenze des Backends ist
   * `bis.minusYears(1)`; ein im Browser gerechnetes Kalenderjahr träfe sie genau
   * — und am 29. Februar läge es einen Tag darüber, weil JavaScript den Stichtag
   * auf den 1. März schiebt. Dann wäre die Antwort ein `400`.
   */
  it("bleibt auch über einen Schalttag unter einem Kalenderjahr", () => {
    const bis = new Date("2028-02-29T00:00:00.000Z");

    const fenster = jahresfensterAb(bis);

    const kalenderjahr = new Date(bis);
    kalenderjahr.setUTCFullYear(bis.getUTCFullYear() - 1);
    expect(fenster.von.getTime()).toBeGreaterThanOrEqual(kalenderjahr.getTime());
  });
});

describe("Die Abschneidemeldung", () => {
  /**
   * **Sie nennt Abschneidung und Fenster zusammen.** Nur eines von beidem ist
   * irreführend: „mehr als 50" ohne Fenster liest sich wie eine Aussage über den
   * ganzen Bestand — und beim schlimmsten gemessenen Wert findet ein Tagesfenster
   * 279 von 234.159 Nachrichten (M35).
   */
  it.each(["de", "en"] as const)("nennt in %s beide Angaben", (sprache) => {
    const text = texteFuer(sprache).suche.ergebnis.abgeschnitten;

    expect(text).toContain("{anzahl}");
    expect(text).toContain("{von}");
    expect(text).toContain("{bis}");
  });

  it("nennt das Fenster auch dort, wo nichts abgeschnitten wurde", () => {
    for (const sprache of ["de", "en"] as const) {
      const text = texteFuer(sprache).suche.ergebnis.anzahl;
      expect(text).toContain("{von}");
      expect(text).toContain("{bis}");
    }
  });

  /**
   * **Im Präfixmodus rät sie zu mehr Zeichen, und der Rat stimmt:** Die
   * Trefferzahl ist die Kostengröße (E6), und eine längere Eingabe senkt sie. Der
   * Rat zum Zeitraum bleibt daneben stehen, aber nachgeordnet — bei dreißig Tagen
   * ist dort weniger zu holen.
   */
  it.each(["de", "en"] as const)("rät in %s im Präfixmodus zuerst zu mehr Zeichen", (sprache) => {
    const ergebnis = texteFuer(sprache).suche.ergebnis;

    expect(ergebnis.abgeschnittenPraefix).not.toBe(ergebnis.abgeschnitten);
    for (const platzhalter of ["{anzahl}", "{von}", "{bis}"]) {
      expect(ergebnis.abgeschnittenPraefix).toContain(platzhalter);
    }
  });
});

describe("Die Sätze des Präfixmodus", () => {
  /**
   * **Kein Fachwort in der Oberfläche.** „Präfix" hilft dem Nutzer nicht, der kein
   * EDI-Spezialist ist; benannt wird, was passiert. Das Wort steht im Code, in der
   * URL und in dieser Datei — in keinem Satz, den jemand liest.
   */
  it.each(["de", "en"] as const)(
    "nennen in %s die Wirkung und nicht den Fachbegriff",
    (sprache) => {
      const praefix = texteFuer(sprache).suche.praefix;

      for (const satz of [
        praefix.angebot,
        praefix.angebotErwartung,
        praefix.angebotKnopf,
        praefix.laeuft,
        praefix.zurueck,
      ]) {
        expect(satz.toLowerCase()).not.toContain("präfix");
        expect(satz.toLowerCase()).not.toContain("praefix");
        expect(satz.toLowerCase()).not.toContain("prefix");
      }
    },
  );

  /**
   * **Beide Datumsangaben, und der Satz über das, was fehlt.** Ohne ihn liest der
   * Nutzer „nicht gefunden" als „nicht vorhanden" — er hat womöglich ein Jahr
   * gewählt und bekommt dreißig Tage.
   */
  it.each(["de", "en"] as const)(
    "nennen in %s den durchsuchten Zeitraum vollständig",
    (sprache) => {
      const text = texteFuer(sprache).suche.praefix.zeitraum;

      expect(text).toContain("{von}");
      expect(text).toContain("{bis}");
    },
  );

  /**
   * Beide Zahlen kommen aus der Fehlerantwort und nicht aus dem Satz: Die Grenze
   * gehört dorthin, wo sie gemessen wurde (`docs/frontend-grundlagen.md` §6).
   */
  it.each(["de", "en"] as const)("holen in %s die Grenze aus der Antwort", (sprache) => {
    const praefix = texteFuer(sprache).suche.praefix;

    expect(praefix.fensterZuGross).toContain("{grenze}");
    expect(praefix.fensterZuGross).toContain("{angefragt}");
    expect(praefix.fensterVerkleinern).toContain("{grenze}");
  });
});
