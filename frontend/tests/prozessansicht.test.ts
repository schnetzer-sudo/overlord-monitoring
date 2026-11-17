import { describe, expect, it } from "vitest";

import type { Fenster } from "@/features/nachrichten/api";
import { alsAbfrage } from "@/features/nachrichten/filter";
import {
  AM_BAUMFENSTER,
  LEERE_PROZESSANSICHT,
  PROZESSANSICHT_PARAMETER,
  absprungZiel,
  absprungfenster,
  kalendertagfenster,
  baumabfrage,
  baumfensterFehler,
  hervorgehobeneGliederung,
  parseAsKennung,
  alsSuchparameter,
  ausSuchparametern,
  listenfilter,
  type Prozessansichtzustand,
} from "@/features/nachrichten/prozessansicht";
import { ProblemFehler } from "@/lib/http";
import {
  FREI,
  angezeigterBaumfenstermodus,
  baumfenstermodus,
  hervorgehobenerBaumzeitraum,
  mitFreiemBaumfenster,
  mitPaar,
} from "@/lib/rollupzeitraum";
import { NACHRICHT_PARAMETER } from "@/lib/routen";

/**
 * Der URL-Zustand der Prozessansicht und der Filter, den er für die
 * Übertragungsliste ergibt — **reine Funktionen, ohne Ansicht**
 * (`docs/frontend-grundlagen.md` §9).
 *
 * Der Prüfkanon ist der der Nachbarn (`tests/nachrichtenfilter.test.ts`,
 * `tests/dashboard.test.ts`): Rundlauf URL → Zustand → URL, unbrauchbare Werte
 * werden übergangen, eine Vorgabe steht nicht in der URL, und **kein Cursor
 * taucht in einer erzeugten Adresse auf**.
 */

const FENSTER: Fenster = { von: "2025-12-28T05:00:00Z", bis: "2025-12-30T05:00:00Z" };

function zustand(teile: Partial<Prozessansichtzustand> = {}): Prozessansichtzustand {
  return { ...LEERE_PROZESSANSICHT, ...teile };
}

function urlVon(z: Prozessansichtzustand): string {
  return alsSuchparameter(z).toString();
}

describe("Der Zustand in der URL", () => {
  it("ist leer, solange nichts gewählt ist", () => {
    // Ohne Klick wählt der Endpunkt den Zeitraum (E‑38), und `nurMitVerkehr`
    // lässt in seiner Vorgabe nichts weg. Beides gehört dann nicht in die URL.
    expect(urlVon(LEERE_PROZESSANSICHT)).toBe("");
  });

  it("läuft in beide Richtungen rund", () => {
    const voll = zustand({
      zeitraum: "30T",
      prozess: "p1",
      nachricht: "m1",
      nurMitVerkehr: true,
      sortierung: "aelteste",
    });

    expect(ausSuchparametern(alsSuchparameter(voll))).toEqual(voll);
  });

  it("hält die Reihenfolge fest, damit zwei gleiche Zustände dieselbe URL ergeben", () => {
    const a = zustand({ zeitraum: "12M", prozess: "p1", nachricht: "m1" });
    const b = zustand({ nachricht: "m1", prozess: "p1", zeitraum: "12M" });

    expect(urlVon(a)).toBe(urlVon(b));
    // Die Kennung steht zuletzt — dort, wo man beim Weitergeben hinsieht.
    expect(urlVon(a).endsWith("nachricht=m1")).toBe(true);
  });

  it("schreibt den Schalter nur, wenn er etwas weglässt", () => {
    // Die Regel aus §8: Ein Standardwert, der etwas *weglässt*, gehört in die
    // URL; einer, der etwas *setzt*, nicht.
    expect(urlVon(zustand({ nurMitVerkehr: false }))).toBe("");
    expect(urlVon(zustand({ nurMitVerkehr: true }))).toBe("nurMitVerkehr=true");
  });

  it("übergeht einen unbekannten Zeitraum, statt ihn weiterzureichen", () => {
    // Ein unbekannter Code wäre am Endpunkt ein garantiertes `400`
    // `zeitraum-unbekannt`.
    expect(ausSuchparametern(new URLSearchParams("zeitraum=7d")).zeitraum).toBeNull();
    expect(ausSuchparametern(new URLSearchParams("zeitraum=48H")).zeitraum).toBe("48H");
  });

  it("übergeht eine unbekannte Sortierung", () => {
    expect(ausSuchparametern(new URLSearchParams("sortierung=groesste")).sortierung).toBeNull();
    expect(ausSuchparametern(new URLSearchParams("sortierung=aelteste")).sortierung).toBe(
      "aelteste",
    );
  });

  it("liest eine leere Kennung als „keine Auswahl“", () => {
    // Sie entstünde nur aus einer von Hand gebauten URL und öffnete sonst eine
    // Liste, die garantiert nichts findet.
    const gelesen = ausSuchparametern(new URLSearchParams("prozess=&nachricht="));
    expect(gelesen.prozess).toBeNull();
    expect(gelesen.nachricht).toBeNull();
  });

  it("meint mit `nachricht` denselben Parameter wie Liste und Belegsuche", () => {
    // Der Parser steht unter einem **berechneten** Schlüssel
    // (`[NACHRICHT_PARAMETER]`), der Setzer im Hook schreibt `{ nachricht: … }`
    // wörtlich, und der Zustandstyp nennt das Feld ebenfalls wörtlich. Änderte
    // sich die Konstante, liefen die drei auseinander — und zwar lautlos, denn
    // `useQueryStates` gibt einen Indexzugriffstyp zurück, an dem der Übersetzer
    // nichts merkt.
    expect(NACHRICHT_PARAMETER).toBe("nachricht");
    expect(Object.keys(PROZESSANSICHT_PARAMETER)).toContain(NACHRICHT_PARAMETER);
    expect(Object.keys(LEERE_PROZESSANSICHT)).toContain(NACHRICHT_PARAMETER);
  });

  it("wirft eine leere Kennung schon im **Parser** weg, nicht erst daneben", () => {
    // ⚠️ Die Regel stand zuerst nur in `ausSuchparametern` — also an der
    // Funktion, die dieser Test ruft, und nicht an der, die die Anwendung
    // ruft. `nuqs` hält einen leeren String **nicht** für abwesend:
    // `/prozesse?prozess=` ergab damit `""`, `etwasGewaehlt` wurde wahr, und
    // der Listenfilter bekam `prozess: [""]` — das Backend wirft leere Werte
    // weg, und ein leerer Prozessfilter heißt dort **alle**.
    expect(parseAsKennung.parse("")).toBeNull();
    expect(parseAsKennung.parse("p1")).toBe("p1");
    expect(PROZESSANSICHT_PARAMETER.prozess.parse("")).toBeNull();
    expect(PROZESSANSICHT_PARAMETER[NACHRICHT_PARAMETER].parse("")).toBeNull();
  });

  it("übergeht einen unbekannten Parameter, statt ihn abzuweisen", () => {
    const gelesen = ausSuchparametern(new URLSearchParams("zwischenschritte=true&prozess=p1"));
    expect(gelesen.prozess).toBe("p1");
    expect(gelesen).toEqual(zustand({ prozess: "p1" }));
  });
});

describe("Der Filter der Übertragungsliste", () => {
  it("entsteht nicht ohne gewählten Prozess", () => {
    // Ohne ihn läuft **keine** Abfrage — rechts steht der Leerzustand.
    expect(listenfilter(LEERE_PROZESSANSICHT, FENSTER)).toBeNull();
  });

  it("entsteht nicht, solange der Baum kein Fenster genannt hat", () => {
    // Das Fenster kommt aus der Antwort und wird nirgends im Browser gerechnet
    // (Regel Z1).
    expect(listenfilter(zustand({ prozess: "p1" }), undefined)).toBeNull();
  });

  it("nimmt das Fenster des Baums und nicht einen eigenen Zeitraum", () => {
    // Die Liste kennt die drei Rollup-Paare nicht; ihre relativen Zeiträume
    // heißen `24h`, `7d`, `30d`. Es bleibt der zweite Modus — und mit demselben
    // Fenster zeigen Baum und Liste denselben Ausschnitt.
    const filter = listenfilter(zustand({ prozess: "p1", zeitraum: "30T" }), FENSTER);

    expect(filter?.zeitraum).toBeNull();
    // Derselbe Zeitpunkt, nur mit Millisekunden ausgeschrieben — verglichen wird
    // der Wert und nicht seine Schreibweise.
    expect(filter?.von?.getTime()).toBe(Date.parse(FENSTER.von));
    expect(filter?.bis?.getTime()).toBe(Date.parse(FENSTER.bis));
  });

  it("filtert auf genau den einen Prozess", () => {
    expect(listenfilter(zustand({ prozess: "p1" }), FENSTER)?.prozess).toEqual(["p1"]);
  });

  it("reicht die Sortierung durch und setzt sonst keine Vorgabe", () => {
    expect(listenfilter(zustand({ prozess: "p1" }), FENSTER)?.sortierung).toBeNull();
    expect(
      listenfilter(zustand({ prozess: "p1", sortierung: "aelteste" }), FENSTER)?.sortierung,
    ).toBe("aelteste");
  });

  it("trägt die geöffnete Nachricht nicht in den Filter", () => {
    // Sie ist Zustand der **Ansicht** und kein Filter der Liste; träte sie in
    // den Abfrageschlüssel ein, lüde jeder Klick auf eine Zeile die Liste neu.
    const filter = listenfilter(zustand({ prozess: "p1", nachricht: "m1" }), FENSTER);
    expect(filter?.nachricht).toBeNull();
  });

  it("ergibt dieselbe Abfrage mit und ohne geöffnetes Panel", () => {
    const ohne = listenfilter(zustand({ prozess: "p1" }), FENSTER);
    const mit = listenfilter(zustand({ prozess: "p1", nachricht: "m1" }), FENSTER);

    expect(alsAbfrage(mit as NonNullable<typeof mit>)).toBe(
      alsAbfrage(ohne as NonNullable<typeof ohne>),
    );
  });

  it("erzeugt eine Abfrage ohne Cursor und ohne Kennung", () => {
    const abfrage = alsAbfrage(
      listenfilter(zustand({ prozess: "p1", nachricht: "m1" }), FENSTER) as NonNullable<
        ReturnType<typeof listenfilter>
      >,
    );

    expect(abfrage).toContain("prozess=p1");
    expect(abfrage).toContain("von=2025-12-28T05%3A00%3A00.000Z");
    expect(abfrage).not.toContain("cursor");
    expect(abfrage).not.toContain("nachricht");
    // Und keine zweite Abfrageform: `ueberfaellig` ist hier nichts, was jemand
    // gewählt hätte (`docs/nachrichtenliste.md` §5b).
    expect(abfrage).not.toContain("ueberfaellig");
  });

  it("gibt bei einem unbrauchbaren Fenster keinen Filter zurück", () => {
    // Lieber der Leerzustand als eine Anfrage mit `Invalid Date`.
    expect(
      listenfilter(zustand({ prozess: "p1" }), { von: "keinDatum", bis: "auchNicht" }),
    ).toBeNull();
  });
});

/**
 * Das freie Zeitfenster (10c‑4b): die beiden Modi löschen einander, der
 * Zwischenzustand steht nicht in der URL, und die Abfrage trägt genau einen
 * Modus. Alle Zeitpunkte erfunden.
 */
describe("Das freie Zeitfenster", () => {
  const VON = new Date("2025-12-29T13:00:00.000Z");
  const BIS = new Date("2025-12-30T01:00:00.000Z");

  it("läuft mit von und bis in beide Richtungen rund", () => {
    const frei = zustand({ von: VON, bis: BIS, prozess: "p1" });

    const url = urlVon(frei);
    expect(url).toContain("von=2025-12-29T13%3A00%3A00.000Z");
    expect(url).toContain("bis=2025-12-30T01%3A00%3A00.000Z");
    expect(url).not.toContain("zeitraum=");
    expect(ausSuchparametern(alsSuchparameter(frei))).toEqual(frei);
  });

  it("übergeht einen unlesbaren Zeitpunkt, statt ihn weiterzureichen", () => {
    const gelesen = ausSuchparametern(new URLSearchParams("von=gestern&bis=2025-12-30T01:00:00Z"));
    expect(gelesen.von).toBeNull();
    expect(gelesen.bis?.toISOString()).toBe("2025-12-30T01:00:00.000Z");
  });

  it("lässt die beiden Modi einander löschen", () => {
    // Ein Paar löscht das Fenster, ein Fenster löscht das Paar — beide zugleich
    // wären am Endpunkt `400` `zeitfenster-mehrdeutig`, und diesen Zustand soll
    // die Bedienung gar nicht erreichen können.
    expect(mitPaar("30T")).toEqual({ zeitraum: "30T", von: null, bis: null });
    expect(mitFreiemBaumfenster(VON, BIS)).toEqual({ zeitraum: null, von: VON, bis: BIS });
    expect(baumfenstermodus(mitPaar("30T"))).toBe("vorwahl");
    expect(baumfenstermodus(mitFreiemBaumfenster(VON, null))).toBe("frei");
    expect(baumfenstermodus(mitFreiemBaumfenster(null, null))).toBe("offen");
  });

  it("hält den Zwischenzustand „frei gewählt“ im Komponentenzustand, nicht in der URL", () => {
    // Beides ist `zeitraum=null, von=null, bis=null` — und soll es sein: Der
    // freie Modus beginnt leer, und ein leeres Fenster zeigt denselben Baum wie
    // gar keine Auswahl. Ohne die eigene Funktion wäre der Modus über die
    // Oberfläche nicht erreichbar.
    const leer = mitFreiemBaumfenster(null, null);
    expect(urlVon(zustand(leer))).toBe("");
    expect(angezeigterBaumfenstermodus(leer, false)).toBe("offen");
    expect(angezeigterBaumfenstermodus(leer, true)).toBe("frei");
    // Die URL gewinnt, sobald sie etwas sagt.
    expect(angezeigterBaumfenstermodus(mitPaar("48H"), true)).toBe("vorwahl");
    expect(angezeigterBaumfenstermodus(mitFreiemBaumfenster(VON, null), false)).toBe("frei");
  });

  it("hebt im freien Modus „Frei“ hervor, sonst das Paar, das gilt", () => {
    expect(hervorgehobenerBaumzeitraum(mitFreiemBaumfenster(null, null), true, "48H")).toBe(FREI);
    expect(hervorgehobenerBaumzeitraum(mitFreiemBaumfenster(VON, BIS), false, FREI)).toBe(FREI);
    // Unverändert die Regel des Dashboards: Die Wahl schlägt die Antwort, und
    // ohne beides ist keine Schaltfläche gedrückt.
    expect(hervorgehobenerBaumzeitraum(mitPaar("12M"), false, "48H")).toBe("12M");
    expect(hervorgehobenerBaumzeitraum(mitFreiemBaumfenster(null, null), false, "48H")).toBe("48H");
    expect(hervorgehobenerBaumzeitraum(mitFreiemBaumfenster(null, null), false, undefined)).toBe(
      null,
    );
  });

  it("baut die Abfrage aus genau einem Modus", () => {
    expect(baumabfrage(LEERE_PROZESSANSICHT)).toBe("");
    expect(baumabfrage(zustand({ zeitraum: "30T" }))).toBe("?zeitraum=30T");
    expect(baumabfrage(zustand({ von: VON, bis: BIS }))).toBe(
      "?von=2025-12-29T13%3A00%3A00.000Z&bis=2025-12-30T01%3A00%3A00.000Z",
    );
    // Ein halbes Fenster wird mitgeschickt und nicht hier abgefangen — das
    // Backend antwortet `zeitfenster-unvollstaendig`.
    expect(baumabfrage(zustand({ von: VON }))).toBe("?von=2025-12-29T13%3A00%3A00.000Z");
  });

  it("stellt die Antworten zum Fenster an die Felder und die mehrdeutige über die Ansicht", () => {
    const problem = (typ: string) => new ProblemFehler({ status: 400, typ });
    for (const typ of AM_BAUMFENSTER) {
      expect(baumfensterFehler(problem(typ))?.typ).toBe(typ);
    }
    expect(AM_BAUMFENSTER).toContain("zeitfenster-zu-genau");
    expect(AM_BAUMFENSTER).toContain("zeitfenster-zu-gross");
    // `zeitfenster-mehrdeutig` lässt die Bedienung nicht entstehen — käme es
    // doch, ist es ein Befund und gehört sichtbar.
    expect(baumfensterFehler(problem("zeitfenster-mehrdeutig"))).toBeUndefined();
    expect(baumfensterFehler(problem("zeitraum-unbekannt"))).toBeUndefined();
    expect(baumfensterFehler(new Error("kein Problem"))).toBeUndefined();
  });

  it("gibt das Fenster der Antwort an die Liste weiter, nicht das der URL", () => {
    // E‑50 unverändert: Das Fenster kommt aus der Antwort — im freien Modus mit
    // dem ausschließenden `bis`, das das Backend gerechnet hat.
    const filter = listenfilter(zustand({ prozess: "p1", von: VON, bis: BIS }), FENSTER);
    expect(filter?.von?.getTime()).toBe(Date.parse(FENSTER.von));
    expect(filter?.bis?.getTime()).toBe(Date.parse(FENSTER.bis));
  });
});

describe("Der Absprung aus dem Detailpanel in den Prozessbaum (E‑103, E‑104, E‑111, E‑1196)", () => {
  const nachricht = new Date("2025-12-15T10:20:30.000Z");
  /** Die Anwendungszone — die des Backends, mit zwei Umstellungstagen im Jahr. */
  const ZONE = "Europe/Berlin";

  /**
   * **Nach außen auf volle Stunden gerundet**, weil der Baum nur stundengenaue
   * Zeitpunkte annimmt und `bis` dort die letzte enthaltene Stunde ist
   * (`docs/process-view.md` §38). Das Herkunftsfenster liegt ganz im Ergebnis,
   * und das Ergebnis ist an jedem Ende höchstens 59:59 weiter.
   */
  it("rundet von nach unten und bis auf die Stunde, in der es liegt", () => {
    const fenster = absprungfenster(
      { von: new Date("2025-11-30T04:14:07.000Z"), bis: new Date("2025-12-30T04:14:07.000Z") },
      nachricht,
      ZONE,
    );

    expect(fenster).toEqual({
      von: new Date("2025-11-30T04:00:00.000Z"),
      bis: new Date("2025-12-30T04:00:00.000Z"),
    });
  });

  it("lässt ein schon stundengenaues Fenster unverändert", () => {
    const von = new Date("2025-11-30T00:00:00.000Z");
    const bis = new Date("2025-12-30T00:00:00.000Z");
    expect(absprungfenster({ von, bis }, nachricht, ZONE)).toEqual({ von, bis });
  });

  /**
   * **Die Nachricht ist im Zielfenster per Konstruktion enthalten — und das
   * wird geprüft, nicht angenommen.** Liegt sie außerhalb des Herkunftsfensters
   * (tiefer Link auf einen Beleg außerhalb des Suchfensters), gab es bis zum
   * 17.11.2026 keinen Link. **Seit E‑1196 fällt der Absprung dann auf den
   * Kalendertag der Nachricht zurück** — und der Wächter prüft weiterhin, dass
   * sie im Ziel liegt.
   */
  it("fällt auf den Kalendertag zurück, wenn die Nachricht außerhalb des Fensters liegt", () => {
    const fenster = {
      von: new Date("2025-11-30T00:00:00.000Z"),
      bis: new Date("2025-12-30T00:00:00.000Z"),
    };
    const davor = new Date("2025-11-29T22:59:59.000Z");
    const danach = new Date("2025-12-30T01:00:00.000Z");

    expect(absprungfenster(fenster, davor, ZONE)).toEqual(kalendertagfenster(davor, ZONE));
    expect(absprungfenster(fenster, danach, ZONE)).toEqual(kalendertagfenster(danach, ZONE));
    // Innerhalb der letzten enthaltenen Stunde: das Herkunftsfenster, unverändert.
    expect(absprungfenster(fenster, new Date("2025-12-30T00:59:59.000Z"), ZONE)).toEqual(fenster);
    expect(absprungfenster(fenster, new Date("nicht lesbar"), ZONE)).toBeNull();
  });

  /**
   * **Ohne Herkunftsfenster der Kalendertag** *(17.11.2026, E‑1196)* — im
   * relativen Modus der Liste, auf der eigenen Route, in der Belegsuche vor der
   * ersten Antwort. Gegen die Browseruhr wird dabei nicht gerechnet (Regel Z1):
   * Der Tag kommt aus dem Zeitpunkt der Nachricht.
   */
  it("nimmt ohne Herkunftsfenster den Kalendertag der Nachricht in der Anwendungszone", () => {
    expect(absprungfenster(undefined, nachricht, ZONE)).toEqual({
      von: new Date("2025-12-14T23:00:00.000Z"),
      bis: new Date("2025-12-15T22:00:00.000Z"),
    });
  });

  /**
   * **Am Jahr gedeckelt.** „Auf ein Jahr erweitern" wählt 365 Tage; nach außen
   * gerundet wären das mehr als der Baum annimmt (`zeitfenster-zu-gross`, gegen
   * das ausschließende Ende gerechnet). `von` rückt deshalb auf 365 Tage vor dem
   * ausschließenden Ende — und eine Nachricht in der dadurch verlorenen Stunde
   * bekommt keinen Link.
   */
  it("deckelt ein Jahresfenster so, dass der Baum es annimmt", () => {
    const bis = new Date("2025-12-30T04:14:00.000Z");
    const jahr = { von: new Date(bis.getTime() - 365 * 24 * 60 * 60 * 1000), bis };

    const fenster = absprungfenster(jahr, nachricht, ZONE);

    expect(fenster).not.toBeNull();
    expect(fenster!.bis).toEqual(new Date("2025-12-30T04:00:00.000Z"));
    // ausschließendes Ende 05:00 minus 365 Tage — nicht 04:00 des Vorjahres.
    expect(fenster!.von).toEqual(new Date("2024-12-30T05:00:00.000Z"));
    // Die Stunde, die der Deckel abschneidet: seit E‑1196 der Kalendertag statt keines Links.
    const verloren = new Date("2024-12-30T04:30:00.000Z");
    expect(absprungfenster(jahr, verloren, ZONE)).toEqual(kalendertagfenster(verloren, ZONE));
  });

  /**
   * **Die bestehende Route, unverändert**: `von`/`bis` absolut, `prozess`,
   * `nachricht` — nie ein relativer Zeitraum, kein aufgeklappter Partner. Dieselbe
   * Gestalt, die die Prozessansicht selbst schreibt, und derselbe Parser liest sie.
   */
  it("baut die Adresse der Prozessansicht mit absolutem Fenster", () => {
    const fenster = {
      von: new Date("2025-11-30T04:00:00.000Z"),
      bis: new Date("2025-12-30T04:00:00.000Z"),
    };

    const ziel = absprungZiel("p-1", "m-1", fenster);

    expect(ziel).toBe(
      "/prozesse?von=2025-11-30T04%3A00%3A00.000Z&bis=2025-12-30T04%3A00%3A00.000Z&prozess=p-1&nachricht=m-1",
    );
    const gelesen = ausSuchparametern(new URL(ziel, "http://localhost").searchParams);
    expect(gelesen).toEqual(
      zustand({ von: fenster.von, bis: fenster.bis, prozess: "p-1", nachricht: "m-1" }),
    );
    expect(gelesen.zeitraum).toBeNull();
    // Und ohne Gliederung (E‑143): Wer aus dem Detail springt, landet in der
    // Vorgabe seines eigenen Kontos.
    expect(ziel).not.toContain("gliederung");
  });
});

describe("Die Gliederung in der URL (E‑143)", () => {
  it("steht nur darin, wenn sie gewählt ist — und läuft dann rund", () => {
    expect(urlVon(zustand())).not.toContain("gliederung");

    const gewaehlt = zustand({ zeitraum: "30T", gliederung: "PROJEKT" });
    const url = urlVon(gewaehlt);
    expect(url).toContain("gliederung=PROJEKT");
    expect(ausSuchparametern(new URLSearchParams(url))).toEqual(gewaehlt);
  });

  it("übergeht eine unbekannte Gliederung, statt sie weiterzureichen", () => {
    // Am Endpunkt wäre sie `400 gliederung-unbekannt`; aus der URL gelesen gilt
    // sie als nicht gewählt, und der Server setzt die Vorgabe ein.
    expect(ausSuchparametern(new URLSearchParams("gliederung=SOS")).gliederung).toBeNull();
    expect(PROZESSANSICHT_PARAMETER.gliederung.parse("SOS")).toBeNull();
    expect(PROZESSANSICHT_PARAMETER.gliederung.parse("PARTNER")).toBe("PARTNER");
  });

  it("geht in die Abfrage des Baums, wenn gewählt, und sonst nicht", () => {
    // Ohne Parameter ist es ein eigener Schlüssel — der der Vorgabe des Kontos.
    expect(baumabfrage(zustand())).toBe("");
    expect(baumabfrage(zustand({ gliederung: "PARTNER" }))).toBe("?gliederung=PARTNER");
    expect(baumabfrage(zustand({ zeitraum: "12M", gliederung: "PROJEKT" }))).toBe(
      "?zeitraum=12M&gliederung=PROJEKT",
    );
  });

  it("lässt die Liste rechts unberührt — beide Gliederungen enden im selben Blatt", () => {
    const ohne = listenfilter(zustand({ prozess: "p-1" }), FENSTER);
    const mit = listenfilter(zustand({ prozess: "p-1", gliederung: "PROJEKT" }), FENSTER);
    expect(mit).toEqual(ohne);
  });

  it("hebt hervor, was gilt: die gewählte, sonst die der Antwort, sonst keine", () => {
    expect(hervorgehobeneGliederung(zustand({ gliederung: "PARTNER" }), "PROJEKT")).toBe("PARTNER");
    expect(hervorgehobeneGliederung(zustand(), "PROJEKT")).toBe("PROJEKT");
    expect(hervorgehobeneGliederung(zustand(), undefined)).toBeNull();
  });
});

/**
 * **Der Kalendertag der Nachricht** *(17.11.2026, E‑1196)* — der Rückfall des
 * Absprungs, wenn kein Herkunftsfenster bekannt ist oder die Nachricht nicht
 * darin liegt. `von` ist 00:00, `bis` 23:00 als **letzte enthaltene Stunde**
 * (E‑94), beides in der Anwendungszone und mit denselben Umrechnungen wie das
 * freie Fenster (`lib/format.ts`).
 */
describe("Der Kalendertag als Zielfenster (E‑1196)", () => {
  const ZONE = "Europe/Berlin";
  const STUNDE = 60 * 60 * 1000;
  const laenge = (f: { von: Date; bis: Date }) =>
    (f.bis.getTime() + STUNDE - f.von.getTime()) / STUNDE;

  it("reicht im Winter und im Sommer von 00:00 bis 23:00 der Anwendungszone", () => {
    expect(kalendertagfenster(new Date("2025-12-29T10:01:09Z"), ZONE)).toEqual({
      von: new Date("2025-12-28T23:00:00.000Z"),
      bis: new Date("2025-12-29T22:00:00.000Z"),
    });
    expect(kalendertagfenster(new Date("2026-07-08T09:30:00Z"), ZONE)).toEqual({
      von: new Date("2026-07-07T22:00:00.000Z"),
      bis: new Date("2026-07-08T21:00:00.000Z"),
    });
  });

  it("nimmt den Tag der Anwendungszone und nicht den in UTC", () => {
    // 23:30 UTC ist in Berlin schon 00:30 des Folgetags.
    expect(kalendertagfenster(new Date("2025-12-14T23:30:00Z"), ZONE)?.von).toEqual(
      new Date("2025-12-14T23:00:00.000Z"),
    );
    // 22:59 UTC ist 23:59 desselben Tags — in der letzten enthaltenen Stunde.
    expect(kalendertagfenster(new Date("2025-12-15T22:59:59Z"), ZONE)?.bis).toEqual(
      new Date("2025-12-15T22:00:00.000Z"),
    );
  });

  it("hat am Tag der Vorstellung 23 Stunden", () => {
    const tag = kalendertagfenster(new Date("2026-03-29T12:00:00Z"), ZONE);

    expect(tag).toEqual({
      von: new Date("2026-03-28T23:00:00.000Z"),
      bis: new Date("2026-03-29T21:00:00.000Z"),
    });
    expect(laenge(tag!)).toBe(23);
    // Die Nachricht aus der letzten Stunde des Tags liegt darin.
    expect(absprungfenster(undefined, new Date("2026-03-29T21:59:59Z"), ZONE)).toEqual(tag);
  });

  it("hat am Tag der Rückstellung 25 Stunden", () => {
    const tag = kalendertagfenster(new Date("2026-10-25T12:00:00Z"), ZONE);

    expect(tag).toEqual({
      von: new Date("2026-10-24T22:00:00.000Z"),
      bis: new Date("2026-10-25T22:00:00.000Z"),
    });
    expect(laenge(tag!)).toBe(25);
    // Beide Stunden zwischen 02:00 und 03:00 Ortszeit gehören zum Tag.
    expect(absprungfenster(undefined, new Date("2026-10-25T00:30:00Z"), ZONE)).toEqual(tag);
    expect(absprungfenster(undefined, new Date("2026-10-25T01:30:00Z"), ZONE)).toEqual(tag);
  });

  it("liegt auf vollen Stunden, die der Baum annimmt (E‑95)", () => {
    for (const zeitpunkt of [
      "2025-12-29T10:01:09Z",
      "2026-03-29T12:00:00Z",
      "2026-10-25T12:00:00Z",
    ]) {
      const tag = kalendertagfenster(new Date(zeitpunkt), ZONE)!;
      expect(tag.von.getTime() % STUNDE, zeitpunkt).toBe(0);
      expect(tag.bis.getTime() % STUNDE, zeitpunkt).toBe(0);
    }
  });

  /**
   * **Der Wächter bleibt — und er greift**, wo die letzte Stunde des Tags nicht
   * die einzige ab 23:00 ist: In `America/Santiago` endet die Sommerzeit um
   * Mitternacht, am 04.04.2026 gibt es die Stunde ab 23:00 zweimal. `bis` ist die
   * erste; eine Nachricht aus der zweiten läge **nach** dem Zielfenster, und statt
   * eines Links, der sie nicht zeigt, gibt es keinen. In `Europe/Berlin` tritt
   * der Fall nicht auf (die Umstellung liegt um 02:00 und 03:00).
   */
  it("gibt keinen Link, wenn die Nachricht nicht in ihrem Kalendertag liegt", () => {
    const SANTIAGO = "America/Santiago";
    const zweite = new Date("2026-04-05T03:30:00Z"); // 23:30 Ortszeit, zum zweiten Mal

    expect(kalendertagfenster(zweite, SANTIAGO)).toEqual({
      von: new Date("2026-04-04T03:00:00.000Z"),
      bis: new Date("2026-04-05T02:00:00.000Z"),
    });
    expect(absprungfenster(undefined, zweite, SANTIAGO)).toBeNull();
    // Gegenprobe: 23:30 zum ersten Mal liegt darin.
    expect(absprungfenster(undefined, new Date("2026-04-05T02:30:00Z"), SANTIAGO)).not.toBeNull();
  });

  it("gibt bei unlesbarem Zeitpunkt oder unbrauchbarer Zone keinen Link", () => {
    expect(kalendertagfenster(new Date("nicht lesbar"), ZONE)).toBeNull();
    expect(kalendertagfenster(new Date("2025-12-29T10:01:09Z"), "Nirgendwo/Erfunden")).toBeNull();
    expect(absprungfenster(undefined, new Date("nicht lesbar"), ZONE)).toBeNull();
  });

  it("führt auf die bestehende Route mit dem Tag als absolutem Fenster", () => {
    const tag = kalendertagfenster(new Date("2025-12-29T10:01:09Z"), ZONE)!;

    expect(absprungZiel("p-1", "m-1", tag)).toBe(
      "/prozesse?von=2025-12-28T23%3A00%3A00.000Z&bis=2025-12-29T22%3A00%3A00.000Z&prozess=p-1&nachricht=m-1",
    );
  });
});

/**
 * **Die Vollansicht in der Prozessansicht** *(17.11.2026, E‑1194)* — derselbe
 * Parameter wie in Liste und Belegsuche. Er steht in der URL und in keiner
 * Abfrage: weder in der des Baums noch in der der Übertragungsliste.
 */
describe("Die Vollansicht in der URL der Prozessansicht", () => {
  it("läuft rund und steht nur darin, solange sie gilt", () => {
    const offen = zustand({ zeitraum: "30T", prozess: "p1", nachricht: "m1" });

    expect(urlVon({ ...offen, voll: true })).toContain("voll=true");
    expect(urlVon(offen)).not.toContain("voll");
    expect(ausSuchparametern(alsSuchparameter({ ...offen, voll: true }))).toEqual({
      ...offen,
      voll: true,
    });
    expect(PROZESSANSICHT_PARAMETER.voll.history).toBe("push");
  });

  it("ändert weder die Abfrage des Baums noch die der Übertragungsliste", () => {
    const ohne = zustand({ zeitraum: "30T", prozess: "p1" });
    const mit = zustand({ zeitraum: "30T", prozess: "p1", nachricht: "m1", voll: true });

    expect(baumabfrage(mit)).toBe(baumabfrage(ohne));
    expect(alsAbfrage(listenfilter(mit, FENSTER)!)).toBe(alsAbfrage(listenfilter(ohne, FENSTER)!));
    expect(listenfilter(mit, FENSTER)?.voll).toBe(false);
  });
});
