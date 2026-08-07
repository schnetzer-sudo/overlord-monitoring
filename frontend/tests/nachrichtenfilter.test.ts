import { describe, expect, it } from "vitest";

import {
  alsAbfrage,
  alsSuchparameter,
  ausSuchparametern,
  feldFehler,
  sucheTraegt,
  suchfeldFehler,
  zeitfensterFehler,
  zeitfensterHalb,
  type Nachrichtenfilter,
} from "@/features/nachrichten/filter";
import {
  angezeigterModus,
  mitFreiemFenster,
  mitVorwahl,
  ohneZeitfenster,
  zeitfenstermodus,
} from "@/lib/filter";
import { ProblemFehler } from "@/lib/http";

/**
 * Der Filterzustand liegt in der **URL**, damit Ansichten teilbar sind.
 *
 * Geprüft werden die Entscheidungen, nicht das Markup: Ein geteilter Link muss
 * denselben Ausschnitt herstellen, eine Änderung muss in der URL ankommen — und
 * die Seitenposition darf dort **nie** landen.
 */

const LEER: Nachrichtenfilter = {
  zeitraum: null,
  von: null,
  bis: null,
  status: null,
  prozess: null,
  suche: null,
  langeSuche: false,
  zwischenschritte: false,
  sortierung: null,
};

describe("URL → Zustand", () => {
  it("stellt einen geteilten Link vollständig wieder her", () => {
    const url = new URLSearchParams(
      "zeitraum=7d&status=FEHLER&status=WARTEND&prozess=abc&prozess=def" +
        "&suche=lieferschein&zwischenschritte=true&sortierung=aelteste",
    );

    expect(ausSuchparametern(url)).toEqual({
      zeitraum: "7d",
      von: null,
      bis: null,
      status: ["FEHLER", "WARTEND"],
      prozess: ["abc", "def"],
      suche: "lieferschein",
      langeSuche: false,
      zwischenschritte: true,
      sortierung: "aelteste",
    });
  });

  it("liest ein freies Zeitfenster als Zeitpunkte", () => {
    const filter = ausSuchparametern(
      new URLSearchParams("von=2025-12-29T00:00:00Z&bis=2025-12-30T00:00:00Z"),
    );

    expect(filter.von?.toISOString()).toBe("2025-12-29T00:00:00.000Z");
    expect(filter.bis?.toISOString()).toBe("2025-12-30T00:00:00.000Z");
    expect(zeitfenstermodus(filter)).toBe("frei");
  });

  /**
   * Ein unbekannter Wert wird nicht stillschweigend auf eine Vorgabe gezogen und
   * auch nicht durchgereicht: Das Backend antwortete darauf mit `400`, und der
   * Nutzer sähe einen Fehler statt einer Liste — für einen Tippfehler in einer
   * URL, die er vielleicht gar nicht selbst geschrieben hat.
   */
  it("übergeht unbekannte Werte, statt sie weiterzureichen", () => {
    const filter = ausSuchparametern(
      new URLSearchParams("zeitraum=24&status=ERROR_DUPLICATE&sortierung=egal"),
    );

    expect(filter.zeitraum).toBeNull();
    expect(filter.status).toBeNull();
    expect(filter.sortierung).toBeNull();
  });

  /** Ohne Angabe gilt die Vorgabe — Zwischenschritte bleiben draußen. */
  it("blendet Zwischenschritte ohne gegenteilige Angabe aus", () => {
    expect(ausSuchparametern(new URLSearchParams("")).zwischenschritte).toBe(false);
    expect(ausSuchparametern(new URLSearchParams("zwischenschritte=true")).zwischenschritte).toBe(
      true,
    );
  });

  /**
   * Das Zeitfenster hat **keinen** Standardwert im Frontend. Fehlt es, setzt das
   * Backend die 24 Stunden aus Regel L1; ein zweiter Standardwert liefe dem
   * ersten irgendwann hinterher.
   */
  it("setzt ohne Angabe kein Zeitfenster", () => {
    const filter = ausSuchparametern(new URLSearchParams(""));

    expect(filter.zeitraum).toBeNull();
    expect(filter.von).toBeNull();
    expect(filter.bis).toBeNull();
    expect(zeitfenstermodus(filter)).toBe("offen");
    expect(alsAbfrage(filter)).not.toContain("zeitraum");
    expect(alsAbfrage(filter)).not.toContain("von=");
  });
});

describe("Zustand → URL", () => {
  it("bildet jede Änderung ab und lässt sich wieder einlesen", () => {
    const filter: Nachrichtenfilter = {
      ...LEER,
      zeitraum: "30d",
      status: ["FEHLER"],
      prozess: ["abc"],
      suche: "lieferschein",
      zwischenschritte: true,
      sortierung: "aelteste",
    };

    const url = alsSuchparameter(filter);

    expect(url.get("zeitraum")).toBe("30d");
    expect(url.getAll("status")).toEqual(["FEHLER"]);
    expect(url.getAll("prozess")).toEqual(["abc"]);
    expect(url.get("suche")).toBe("lieferschein");
    expect(url.get("zwischenschritte")).toBe("true");
    expect(url.get("sortierung")).toBe("aelteste");
    // Hin und zurück ergibt denselben Zustand — sonst zeigte ein geteilter Link
    // etwas anderes als die Ansicht, aus der er stammt.
    expect(ausSuchparametern(url)).toEqual(filter);
  });

  /**
   * Was ausgeblendet ist, muss man teilen können: `zwischenschritte` steht
   * ausdrücklich in der URL, ab dem ersten Rendern und auch dann, wenn es der
   * Vorgabe entspricht.
   */
  it("schreibt zwischenschritte auch dann, wenn es der Vorgabe entspricht", () => {
    expect(alsSuchparameter(LEER).get("zwischenschritte")).toBe("false");
  });

  it("schreibt nichts, was nicht gesetzt ist", () => {
    expect([...alsSuchparameter(LEER).keys()]).toEqual(["zwischenschritte"]);
  });

  /**
   * Die URL bildet ab, was der Nutzer **eingestellt** hat — nicht, was gerade
   * abgefragt wird. Ein Link mitten im Tippen gibt den Stand des Absenders
   * weiter und keinen halben.
   */
  it("behält einen noch zu kurzen Suchbegriff in der URL, schickt ihn aber nicht", () => {
    const filter: Nachrichtenfilter = { ...LEER, suche: "ab" };

    expect(alsSuchparameter(filter).get("suche")).toBe("ab");
    expect(sucheTraegt(filter.suche)).toBe(false);
    expect(alsAbfrage(filter)).not.toContain("suche");
  });
});

/**
 * Die bewusst aufgehobene Fenstergrenze der Suche steht in der URL wie jeder
 * andere Filter — was man sieht, ist was man teilt. Ein Link auf eine
 * ausdrücklich lange Suche zeigt beim Empfänger dieselbe Liste und nicht `400`.
 */
describe("langeSuche", () => {
  it("steht in der URL und lässt sich wieder einlesen", () => {
    const filter: Nachrichtenfilter = { ...LEER, suche: "lieferschein", langeSuche: true };

    const url = alsSuchparameter(filter);

    expect(url.get("langeSuche")).toBe("true");
    expect(ausSuchparametern(url)).toEqual(filter);
  });

  it("fehlt in der URL, solange die Grenze steht", () => {
    expect(alsSuchparameter({ ...LEER, suche: "lieferschein" }).has("langeSuche")).toBe(false);
    expect(ausSuchparametern(new URLSearchParams("")).langeSuche).toBe(false);
  });

  /**
   * Ohne Suchbegriff greift die Grenze im Backend gar nicht. Den Parameter
   * trotzdem mitzuschicken machte nur den Abfrageschlüssel des Zwischenspeichers
   * unnötig verschieden — dieselbe Antwort läge dann zweimal darin.
   */
  it("wird nur zusammen mit dem Suchbegriff geschickt", () => {
    expect(alsAbfrage({ ...LEER, suche: "lieferschein", langeSuche: true })).toContain(
      "langeSuche=true",
    );
    expect(alsAbfrage({ ...LEER, langeSuche: true })).not.toContain("langeSuche");
    expect(alsAbfrage({ ...LEER, suche: "ab", langeSuche: true })).not.toContain("langeSuche");
  });
});

/**
 * **Welche Rückmeldung an das Suchfeld gehört und welche über die Ansicht.**
 *
 * Der Unterschied ist nicht kosmetisch: Am Feld bleibt die Liste stehen, über der
 * Ansicht verschwindet sie. Wer beim Tippen in die Fenstergrenze läuft, soll
 * weiter sehen, was er vorher gesehen hat — und nicht einen Leerzustand, der
 * behauptet, im Zeitfenster stünde nichts.
 */
describe("Rückmeldungen am Suchfeld", () => {
  const problem = (typ: string, angaben: Record<string, unknown> = {}) =>
    new ProblemFehler({ status: 400, typ, angaben });

  it("nimmt die vier Typen an, die einer Eingabe gelten", () => {
    for (const typ of [
      "suchbegriff-zu-kurz",
      "suchbegriff-zu-unscharf",
      "suche-fenster-zu-gross",
      "suche-abgebrochen",
    ]) {
      expect(suchfeldFehler(problem(typ)), typ).toBeDefined();
    }
  });

  it("lässt alles andere Fehlerzustand der Ansicht bleiben", () => {
    expect(suchfeldFehler(problem("zeitfenster-zu-gross"))).toBeUndefined();
    expect(suchfeldFehler(problem("cursor-ungueltig"))).toBeUndefined();
    expect(suchfeldFehler(new Error("irgendwas"))).toBeUndefined();
    expect(suchfeldFehler(undefined)).toBeUndefined();
  });

  /**
   * Die beiden Zahlen der konkreten Meldung kommen aus der Antwort. Fehlt eine —
   * etwa weil eine ältere Fassung antwortet —, greift der allgemeine Satz aus
   * dem Fehlerkatalog statt einer Meldung mit einer Lücke darin.
   */
  it("liest Grenze und angefragte Spanne aus der Antwort", () => {
    const fehler = problem("suche-fenster-zu-gross", { grenzeTage: 30, angefragtTage: 60 });

    expect(fehler.zahl("grenzeTage")).toBe(30);
    expect(fehler.zahl("angefragtTage")).toBe(60);
    expect(problem("suche-fenster-zu-gross").zahl("grenzeTage")).toBeUndefined();
    expect(
      problem("suche-fenster-zu-gross", { grenzeTage: "30" }).zahl("grenzeTage"),
    ).toBeUndefined();
  });
});

describe("Der Cursor gehört nicht in die URL", () => {
  /**
   * Der eigentliche Punkt: Ein Link auf Seite sieben eines relativen Fensters
   * zeigte beim Empfänger auf andere Zeilen — das Fenster wird bei ihm neu
   * aufgelöst, und der Cursor zeigt in einen Bereich, den es dort so nicht mehr
   * gibt. Deshalb nimmt `alsSuchparameter` gar keinen Cursor entgegen.
   */
  it("taucht in keiner erzeugten URL auf", () => {
    const filter: Nachrichtenfilter = {
      ...LEER,
      zeitraum: "24h",
      status: ["FEHLER"],
      suche: "lieferschein",
    };

    const url = alsSuchparameter(filter).toString();

    expect(url).not.toContain("cursor");
  });

  it("wird ausschließlich an die Abfrage gehängt, nicht an die URL", () => {
    const filter: Nachrichtenfilter = { ...LEER, zeitraum: "24h" };

    expect(alsAbfrage(filter, "MjAyNS0xMi0yOQ")).toContain("cursor=MjAyNS0xMi0yOQ");
    expect(alsAbfrage(filter, null)).not.toContain("cursor");
    expect(alsSuchparameter(filter).has("cursor")).toBe(false);
  });

  /**
   * Und die Gegenprobe: Stünde er doch einmal in einer geöffneten URL, wird er
   * beim Einlesen nicht zum Zustand — die Liste beginnt auf Seite eins.
   */
  it("wird beim Einlesen einer URL nicht übernommen", () => {
    const filter = ausSuchparametern(new URLSearchParams("zeitraum=24h&cursor=MjAyNS0xMi0yOQ"));

    expect(alsAbfrage(filter)).not.toContain("cursor");
    expect(alsSuchparameter(filter).has("cursor")).toBe(false);
  });
});

describe("Die beiden Zeitfenstermodi schließen einander aus", () => {
  /**
   * Beides zugleich wäre `400` `zeitfenster-mehrdeutig` — bewusst statt einer
   * stillen Vorrangregel. Das Frontend lässt den Zustand deshalb gar nicht erst
   * entstehen.
   */
  it("löscht beim Wechsel jeweils den anderen Modus", () => {
    const frei = mitFreiemFenster(
      new Date("2025-12-29T00:00:00Z"),
      new Date("2025-12-30T00:00:00Z"),
    );
    expect(frei.zeitraum).toBeNull();

    const vorwahl = mitVorwahl("7d");
    expect(vorwahl.von).toBeNull();
    expect(vorwahl.bis).toBeNull();
  });

  it("schickt niemals beide Modi zugleich", () => {
    const abfrage = alsAbfrage({ ...LEER, ...mitVorwahl("7d") });

    expect(abfrage).toContain("zeitraum=7d");
    expect(abfrage).not.toContain("von=");
    expect(abfrage).not.toContain("bis=");
  });

  /**
   * Ein unvollständiges freies Fenster wird **mitgeschickt** und nicht hier
   * abgefangen: Das Backend antwortet `zeitfenster-unvollstaendig`, und die
   * Oberfläche übersetzt den Typ. Zwei Stellen mit derselben Prüfung driften
   * auseinander — und die im Browser ist die, auf die kein Verlass ist.
   */
  /**
   * **Der freie Modus muss erreichbar bleiben.** Ein freies Fenster ohne beide
   * Zeitpunkte ist in der URL nicht von „keine Auswahl" zu unterscheiden — beides
   * ist `zeitraum=null, von=null, bis=null`. Leitete die Oberfläche ihren Modus
   * allein daraus ab, täte der Klick auf „Frei" sichtbar nichts: Die
   * Eingabefelder erschienen nie, und nur eine von Hand gebaute URL käme noch in
   * den Modus. Genau das war bis zum 06.08.2026 der Fall.
   */
  it("zeigt den freien Modus auch, solange kein Zeitpunkt eingetragen ist", () => {
    const leer = ohneZeitfenster();

    expect(zeitfenstermodus(leer)).toBe("offen");
    expect(angezeigterModus(leer, false)).toBe("offen");
    expect(angezeigterModus(leer, true)).toBe("frei");
  });

  it("lässt die URL gewinnen, sobald sie etwas sagt", () => {
    // Ein gesetztes Fenster ist ohnehin „frei" — der Komponentenzustand ändert
    // daran nichts.
    const frei = mitFreiemFenster(new Date("2025-12-29T00:00:00Z"), null);
    expect(angezeigterModus(frei, false)).toBe("frei");

    // Und eine Vorwahl schlägt einen stehengebliebenen Komponentenzustand:
    // Sonst zeigte die Leiste „Frei" und filterte nach sieben Tagen.
    expect(angezeigterModus(mitVorwahl("7d"), true)).toBe("vorwahl");
  });

  it("hält ein halbes Fenster nicht zurück", () => {
    const abfrage = alsAbfrage({
      ...LEER,
      ...mitFreiemFenster(new Date("2025-12-29T00:00:00Z"), null),
    });

    expect(abfrage).toContain("von=2025-12-29T00%3A00%3A00.000Z");
    expect(abfrage).not.toContain("bis=");
  });
});

/**
 * Das freie Zeitfenster ist eine **Eingabe**, kein Schalter — und zwischen „Von"
 * und „Bis" liegt zwangsläufig ein Moment, in dem erst einer der beiden
 * Zeitpunkte dasteht. Was die Oberfläche in diesem Moment tut, ist hier
 * festgehalten: Sie sagt, was noch fehlt, und nimmt dem Nutzer nicht die Liste
 * weg (Nachbesserung zu Schritt 4, 07.08.2026).
 */
describe("Das halb ausgefüllte freie Zeitfenster", () => {
  const zeitpunkt = new Date("2025-12-29T00:00:00Z");

  it("erkennt genau den Zustand, in dem ein Zeitpunkt fehlt", () => {
    expect(zeitfensterHalb({ ...LEER, ...mitFreiemFenster(zeitpunkt, null) })).toBe(true);
    expect(zeitfensterHalb({ ...LEER, ...mitFreiemFenster(null, zeitpunkt) })).toBe(true);
    // Beide gesetzt ist ein vollständiges Fenster, keiner gesetzt ist „keine
    // Auswahl" — in beiden Fällen wartet die Ansicht auf nichts.
    expect(zeitfensterHalb({ ...LEER, ...mitFreiemFenster(zeitpunkt, zeitpunkt) })).toBe(false);
    expect(zeitfensterHalb(LEER)).toBe(false);
    expect(zeitfensterHalb({ ...LEER, ...mitVorwahl("7d") })).toBe(false);
  });

  /**
   * Bis zum 07.08.2026 ersetzte `zeitfenster-unvollstaendig` die ganze Liste
   * durch eine rote Meldung — für eine Eingabe, die der Nutzer gerade erst zur
   * Hälfte gemacht hatte. Dieselbe Belehrung, die §8.2 für den zu kurzen
   * Suchbegriff längst ausschließt.
   */
  it("gehört an die Datumsfelder und nicht über die Ansicht", () => {
    const unvollstaendig = new ProblemFehler({ status: 400, typ: "zeitfenster-unvollstaendig" });
    const ungueltig = new ProblemFehler({ status: 400, typ: "zeitfenster-ungueltig" });

    expect(zeitfensterFehler(unvollstaendig)).toBe(unvollstaendig);
    expect(zeitfensterFehler(ungueltig)).toBe(ungueltig);
    expect(feldFehler(unvollstaendig)).toBe(unvollstaendig);

    // Der Suchfeld-Katalog bleibt davon unberührt — zwei Felder, zwei Kataloge.
    expect(suchfeldFehler(unvollstaendig)).toBeUndefined();
  });

  /**
   * `zeitfenster-mehrdeutig` lässt die Oberfläche gar nicht erst entstehen: Eine
   * Vorwahl löscht `von`/`bis`, ein freies Fenster löscht `zeitraum`. Käme der
   * Typ trotzdem, ist er ein Befund und gehört sichtbar über die Ansicht.
   */
  it("lässt einen echten Befund über der Ansicht stehen", () => {
    const mehrdeutig = new ProblemFehler({ status: 400, typ: "zeitfenster-mehrdeutig" });

    expect(zeitfensterFehler(mehrdeutig)).toBeUndefined();
    expect(feldFehler(mehrdeutig)).toBeUndefined();
  });
});
