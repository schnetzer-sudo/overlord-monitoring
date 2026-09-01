import { describe, expect, it } from "vitest";

import {
  alsAbfrage,
  alsSuchparameter,
  ausSuchparametern,
  feldFehler,
  ohneUeberfaelligBeiStatus,
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
  ueberfaellig: false,
  sortierung: null,
  nachricht: null,
};

describe("URL → Zustand", () => {
  it("stellt einen geteilten Link vollständig wieder her", () => {
    const url = new URLSearchParams(
      "zeitraum=7d&status=FEHLER&status=WARTEND&prozess=abc&prozess=def" +
        "&suche=lieferschein&sortierung=aelteste",
    );

    expect(ausSuchparametern(url)).toEqual({
      zeitraum: "7d",
      von: null,
      bis: null,
      status: ["FEHLER", "WARTEND"],
      prozess: ["abc", "def"],
      suche: "lieferschein",
      langeSuche: false,
      ueberfaellig: false,
      sortierung: "aelteste",
      nachricht: null,
    });
  });

  /**
   * Die beiden Werte, die am 11.08.2026 an die Stelle von `ZWISCHENSCHRITT`
   * getreten sind. Der alte Sammelwert ist kein gültiger Filterwert mehr und
   * fällt wie jeder unbekannte still weg — das Backend wiese ihn mit
   * `status-unbekannt` ab.
   */
  it("liest AUFGETEILT und ZUSAMMENGEFUEHRT, übergeht ZWISCHENSCHRITT", () => {
    expect(
      ausSuchparametern(new URLSearchParams("status=AUFGETEILT&status=ZUSAMMENGEFUEHRT")).status,
    ).toEqual(["AUFGETEILT", "ZUSAMMENGEFUEHRT"]);
    expect(ausSuchparametern(new URLSearchParams("status=ZWISCHENSCHRITT")).status).toBeNull();
  });

  /**
   * **Ein alter Link wird nicht abgewiesen.** `zwischenschritte` ist kein
   * Parameter mehr; er wird übergangen wie jeder unbekannte Suchparameter — kein
   * Fehler, keine Umleitung, keine Wirkung. Er hat nie etwas anderes bewirkt, als
   * das auszublenden, was jetzt ohnehin erscheint.
   */
  it("übergeht einen alten zwischenschritte-Parameter folgenlos", () => {
    expect(ausSuchparametern(new URLSearchParams("zeitraum=7d&zwischenschritte=false"))).toEqual(
      ausSuchparametern(new URLSearchParams("zeitraum=7d")),
    );
    expect(
      alsAbfrage(ausSuchparametern(new URLSearchParams("zwischenschritte=false"))),
    ).not.toContain("zwischenschritte");
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
      sortierung: "aelteste",
    };

    const url = alsSuchparameter(filter);

    expect(url.get("zeitraum")).toBe("30d");
    expect(url.getAll("status")).toEqual(["FEHLER"]);
    expect(url.getAll("prozess")).toEqual(["abc"]);
    expect(url.get("suche")).toBe("lieferschein");
    expect(url.get("sortierung")).toBe("aelteste");
    // Hin und zurück ergibt denselben Zustand — sonst zeigte ein geteilter Link
    // etwas anderes als die Ansicht, aus der er stammt.
    expect(ausSuchparametern(url)).toEqual(filter);
  });

  /**
   * **Ohne Auswahl bleibt die URL leer** — seit dem Wegfall des
   * Ausblende-Schalters am 11.08.2026. Bis dahin stand hier `zwischenschritte`
   * ausdrücklich drin, weil die Liste ein Drittel der Zeilen wegließ und was man
   * sieht, muss man teilen können. Sie lässt nichts mehr weg.
   */
  it("schreibt nichts, was nicht gesetzt ist", () => {
    expect([...alsSuchparameter(LEER).keys()]).toEqual([]);
    expect(alsAbfrage(LEER)).toBe("");
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

/**
 * **Die geöffnete Nachricht steht in der URL — und nicht in der Anfrage.**
 *
 * Zwei Regeln treffen hier aufeinander, und beide gelten: Was der Nutzer sieht,
 * muss er teilen können (`nachricht` gehört also in die URL); und die
 * Detailansicht hängt nicht am Ergebnis der Liste (`nachricht` gehört also
 * **nicht** in den Abfrageschlüssel, mit dem die Liste geladen wird).
 */
describe("Die gewählte Nachricht", () => {
  const kennung = "8f3a1c2e-0000-4000-8000-000000000001";

  it("steht in der URL und lässt sich wieder einlesen", () => {
    const filter: Nachrichtenfilter = { ...LEER, zeitraum: "7d", nachricht: kennung };

    const url = alsSuchparameter(filter);

    expect(url.get("nachricht")).toBe(kennung);
    expect(ausSuchparametern(url)).toEqual(filter);
  });

  /**
   * Der Endpunkt der Liste kennt den Parameter nicht — und träte er in den
   * Abfrageschlüssel ein, lüde **jeder Klick auf eine Zeile die ganze Liste
   * neu** und setzte die Seitenposition zurück. Das Panel lädt über seine eigene
   * Kennung.
   */
  it("taucht in keiner Abfrage der Liste auf", () => {
    const filter: Nachrichtenfilter = { ...LEER, zeitraum: "24h", nachricht: kennung };

    expect(alsAbfrage(filter)).not.toContain("nachricht");
    // Und die Probe darauf, dass sie den Schlüssel wirklich nicht verändert:
    // Mit und ohne geöffnetes Panel dieselbe Abfrage.
    expect(alsAbfrage(filter)).toBe(alsAbfrage({ ...LEER, zeitraum: "24h" }));
  });

  /**
   * Schließen entfernt genau einen Parameter. Wer das Panel zumacht, will seine
   * Liste behalten, wie sie war — dieselbe Seite, dasselbe Fenster, dieselben
   * Filter.
   */
  it("lässt den übrigen Filterzustand unberührt", () => {
    const offen: Nachrichtenfilter = {
      ...LEER,
      zeitraum: "30d",
      status: ["FEHLER"],
      suche: "lieferschein",
      nachricht: kennung,
    };
    const geschlossen = alsSuchparameter({ ...offen, nachricht: null });

    expect(geschlossen.has("nachricht")).toBe(false);
    expect(geschlossen.get("zeitraum")).toBe("30d");
    expect(geschlossen.getAll("status")).toEqual(["FEHLER"]);
    expect(geschlossen.get("suche")).toBe("lieferschein");
  });

  /** Eine leere Kennung ist keine Auswahl — sie öffnete ein Panel, das nichts findet. */
  it("übergeht eine leere Kennung", () => {
    expect(ausSuchparametern(new URLSearchParams("nachricht=")).nachricht).toBeNull();
    expect(alsSuchparameter({ ...LEER, nachricht: "" }).has("nachricht")).toBe(false);
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

/**
 * Die **zweite Abfrageform** der Liste, in der Oberfläche seit dem 01.09.2026.
 *
 * Das Backend kennt `ueberfaellig` seit Schritt 4; die Oberfläche kannte ihn
 * nicht, und es gab auch keinen Weg zu ihm. Mit dem Dashboard gibt es einen —
 * die Kachel *Überfällig, im Fenster* verweist genau hierher. Ohne diesen
 * Parameter überginge `nuqs` ihn stillschweigend und der Nutzer landete auf der
 * **ungefilterten** Liste, ohne Hinweis (`docs/nachrichtenliste.md` §5e).
 */
describe("Nur überfällige", () => {
  it("kommt aus einem geteilten Link an", () => {
    const zustand = ausSuchparametern(new URLSearchParams("ueberfaellig=true&zeitraum=7d"));

    expect(zustand.ueberfaellig).toBe(true);
    expect(zustand.zeitraum).toBe("7d");
  });

  it("steht ohne Angabe auf der Vorgabe und geht dann in keine Anfrage", () => {
    expect(ausSuchparametern(new URLSearchParams()).ueberfaellig).toBe(false);
    // Ein `ueberfaellig=false` waere die Vorgabe ein zweites Mal — und machte
    // den Abfrageschluessel des Zwischenspeichers unnoetig verschieden.
    expect(alsAbfrage(LEER)).not.toContain("ueberfaellig");
    expect(alsSuchparameter(LEER).has("ueberfaellig")).toBe(false);
  });

  it("geht gesetzt in Anfrage und URL", () => {
    const filter: Nachrichtenfilter = { ...LEER, ueberfaellig: true };

    expect(alsAbfrage(filter)).toContain("ueberfaellig=true");
    expect(alsSuchparameter(filter).get("ueberfaellig")).toBe("true");
  });

  it("überlebt den Rundlauf URL → Zustand → URL", () => {
    const filter: Nachrichtenfilter = { ...LEER, ueberfaellig: true, zeitraum: "30d" };

    expect(ausSuchparametern(alsSuchparameter(filter))).toEqual(filter);
  });

  /**
   * **Die beiden erscheinen nie zusammen.** Am Endpunkt sind `ueberfaellig=true`
   * und ein `status`, der weder `WARTEND` noch `LAEUFT` enthaelt, unvereinbar
   * und ergeben `400` `ueberfaellig-und-status-unvereinbar`. Die Oberflaeche
   * laesst den Zustand gar nicht erst entstehen.
   */
  it("endet, sobald ein Status gewählt wird", () => {
    expect(ohneUeberfaelligBeiStatus(["FEHLER"])).toEqual({
      status: ["FEHLER"],
      ueberfaellig: false,
    });
  });

  /**
   * **Auch bei einem Status, den das Backend zuliesse.** `ueberfaellig` ist kein
   * Filter, sondern eine zweite Abfrageform: Wer einen Status waehlt, waehlt die
   * erste. Eine Regel, die je nach gewaehltem Status etwas anderes taete, waere
   * an der Oberflaeche nicht abzulesen.
   */
  it("endet auch bei WARTEND, obwohl das Backend die Kombination zuließe", () => {
    expect(ohneUeberfaelligBeiStatus(["WARTEND"]).ueberfaellig).toBe(false);
  });

  /** Und die leere Statuswahl bleibt die leere Statuswahl. */
  it("macht aus einer geleerten Statuswahl null", () => {
    expect(ohneUeberfaelligBeiStatus([])).toEqual({ status: null, ueberfaellig: false });
  });
});
