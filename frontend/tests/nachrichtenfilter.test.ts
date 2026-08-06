import { describe, expect, it } from "vitest";

import {
  alsAbfrage,
  alsSuchparameter,
  ausSuchparametern,
  sucheTraegt,
  type Nachrichtenfilter,
} from "@/features/nachrichten/filter";
import { mitFreiemFenster, mitVorwahl, zeitfenstermodus } from "@/lib/filter";

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
  it("hält ein halbes Fenster nicht zurück", () => {
    const abfrage = alsAbfrage({
      ...LEER,
      ...mitFreiemFenster(new Date("2025-12-29T00:00:00Z"), null),
    });

    expect(abfrage).toContain("von=2025-12-29T00%3A00%3A00.000Z");
    expect(abfrage).not.toContain("bis=");
  });
});
