import { describe, expect, it } from "vitest";

import type { Fenster } from "@/features/nachrichten/api";
import { alsAbfrage } from "@/features/nachrichten/filter";
import {
  AM_BAUMFENSTER,
  LEERE_PROZESSANSICHT,
  PROZESSANSICHT_PARAMETER,
  baumabfrage,
  baumfensterFehler,
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
