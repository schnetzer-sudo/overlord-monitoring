import { describe, expect, it } from "vitest";

import { texteFuer } from "@/i18n";
import type { Verlaufspunkt, Verteilungszeile } from "@/features/dashboard/api";
import { fehlerartText, verteilungszeileText } from "@/features/dashboard/beschriftung";
import {
  alsSuchparameter,
  ausSuchparametern,
  hervorgehobeneSicht,
  hervorgehobenerZeitraum,
  mitSicht,
  type Dashboardzustand,
} from "@/features/dashboard/filter";
import {
  STAPELREIHENFOLGE,
  achsenaufloesung,
  einordnungenDerRolle,
  rolleKommtVor,
  verlaufszeilen,
} from "@/features/dashboard/verlauf";
import { fehlerZiel, ueberfaelligZiel } from "@/features/dashboard/verweise";
import { STATUSARTEN, statusrolle } from "@/lib/status-farbe";

/**
 * Die **Entscheidungen** des Dashboard-Frontends, geprüft ohne Ansicht
 * (`docs/frontend-grundlagen.md` §9). Was nur ein gerenderter Baum zeigt, steht
 * in `tests/dashboard-blöcke.test.tsx` — und zwar nur das.
 */

const TEXTE = texteFuer("de");

const FENSTER = { von: "2025-12-28T05:00:00Z", bis: "2025-12-30T05:00:00Z" };

const LEER: Dashboardzustand = { zeitraum: null, verteilung: null };

/** Ein Eimer mit genau einer Einordnung — der kleinste Fall der Zusammenfassung. */
function eimer(einordnung: string, anzahl: number): Verlaufspunkt {
  return {
    eimer: "2025-12-28T05:00:00Z",
    gesamt: anzahl,
    einordnungen: [{ einordnung: einordnung as never, anzahl }],
  };
}

describe("Der Verlauf fasst acht Einordnungen zu vier Reihen zusammen", () => {
  /**
   * **Jede der acht landet in ihrer Rolle.** Die Zuordnung selbst steht in
   * `lib/status-farbe.ts` und wird hier nicht nachgebaut — geprüft wird, dass
   * die Zusammenfassung sie *benutzt* und nicht etwas Eigenes rechnet.
   */
  it.each(STATUSARTEN)("%s landet in ihrer Rolle", (art) => {
    const [zeile] = verlaufszeilen([eimer(art, 7)]);
    const rolle = statusrolle(art);

    expect(zeile[rolle]).toBe(7);
    // Und in keiner der drei anderen.
    for (const andere of STAPELREIHENFOLGE.filter((r) => r !== rolle)) {
      expect(zeile[andere]).toBe(0);
    }
  });

  /**
   * **Ein unbekannter Wert fällt in keinen der drei fachlich belegten Eimer.**
   *
   * Das Backend liefert einen Aufzählungswert; eine neunte Einordnung entstünde
   * nur, wenn dort eine dazukäme. Dann gilt Regel Q4: Sie fällt nach
   * `ungeklaert` — der Rolle, deren ganze Bedeutung *„unbekannter Statuswert"*
   * ist — und nicht nach `offen` oder `abgeschlossen`.
   *
   * **Weggelassen wird sie nicht**: Dann wäre der Balken niedriger als `gesamt`.
   */
  it("legt einen unbekannten Wert nach ungeklaert und in keinen bekannten Eimer", () => {
    const [zeile] = verlaufszeilen([eimer("NEUER_WERT_AUS_DER_ZUKUNFT", 4)]);

    expect(zeile.ungeklaert).toBe(4);
    expect(zeile.fehler).toBe(0);
    expect(zeile.offen).toBe(0);
    expect(zeile.abgeschlossen).toBe(0);
    // Die Summe der vier Reihen bleibt die Zahl, die die Antwort nennt.
    expect(zeile.fehler + zeile.offen + zeile.abgeschlossen + zeile.ungeklaert).toBe(zeile.gesamt);
  });

  /** Vier Einordnungen, eine Reihe — das ist der Grund für E‑l. */
  it("summiert die vier offenen Einordnungen zu einer Reihe", () => {
    const [zeile] = verlaufszeilen([
      {
        eimer: "2025-12-28T05:00:00Z",
        gesamt: 10,
        einordnungen: [
          { einordnung: "WARTEND", anzahl: 1 },
          { einordnung: "LAEUFT", anzahl: 2 },
          { einordnung: "AUFGETEILT", anzahl: 3 },
          { einordnung: "ZUSAMMENGEFUEHRT", anzahl: 4 },
        ],
      },
    ]);

    expect(zeile.offen).toBe(10);
    expect(STAPELREIHENFOLGE).toHaveLength(4);
  });

  /**
   * **Der Tooltip bekommt zurück, was der Stapel zusammengefasst hat.** Ohne
   * ihn wäre die Zusammenfassung ein Verlust: *aufgeteilt* und
   * *zusammengeführt* wären im Bild und im Text dasselbe.
   */
  it("gibt dem Tooltip die enthaltenen Einordnungen einzeln zurück", () => {
    const [zeile] = verlaufszeilen([
      {
        eimer: "2025-12-28T05:00:00Z",
        gesamt: 5,
        einordnungen: [
          { einordnung: "AUFGETEILT", anzahl: 2 },
          { einordnung: "ZUSAMMENGEFUEHRT", anzahl: 3 },
          { einordnung: "FEHLER", anzahl: 0 },
        ],
      },
    ]);

    expect(einordnungenDerRolle(zeile, "offen").map((e) => e.einordnung)).toEqual([
      "AUFGETEILT",
      "ZUSAMMENGEFUEHRT",
    ]);
    expect(einordnungenDerRolle(zeile, "fehler").map((e) => e.einordnung)).toEqual(["FEHLER"]);
  });

  /** Eine Rolle ohne ein einziges Segment bekommt keinen Legendeneintrag. */
  it("erkennt eine Rolle, die ueber alle Eimer null ist", () => {
    const zeilen = verlaufszeilen([eimer("ABGESCHLOSSEN", 9), eimer("ABGESCHLOSSEN", 3)]);

    expect(rolleKommtVor(zeilen, "abgeschlossen")).toBe(true);
    expect(rolleKommtVor(zeilen, "fehler")).toBe(false);
  });

  /** Die Achsenauflösung folgt der Eimerbreite und nicht der Zahl der Eimer. */
  it("wählt die Achsenauflösung nach dem Paar", () => {
    expect(achsenaufloesung("48H")).toBe("stunde");
    expect(achsenaufloesung("30T")).toBe("tag");
    expect(achsenaufloesung("12M")).toBe("monat");
  });

  /*
   * **Hier stand ein Fall zu `achsenabstand`** — der gerechnete `interval`-Wert
   * für die Zeitachse. Die Funktion ist am 01.09.2026 entfallen, weil sie bei
   * 360 px Fensterbreite überlappende Beschriftungen ergab; die Dichte
   * entscheidet seither Recharts über `equidistantPreserveStart` und
   * `minTickGap`. Ein Test dafür wäre ein Test über Recharts und nicht über eine
   * Entscheidung dieses Projekts — gemessen ist sie am laufenden System
   * (`docs/dashboard-frontend.md` §5.3).
   */
});

/**
 * **In der URL steht nur die ausdrückliche Wahl** (Entscheidung E‑n).
 *
 * Der Unterschied zwischen *gewählt* und *gewirkt* ist der ganze Inhalt dieser
 * Entscheidung: Wäre der gewählte Wert derselbe Zustand wie der gewirkte, müsste
 * die Ansicht ihn zurückschreiben — und ein Aufruf ohne Parameter wäre nach dem
 * ersten Rendern keiner mehr.
 */
describe("Der URL-Zustand", () => {
  it("ist ohne Klick leer — beide Parameter", () => {
    expect([...alsSuchparameter(LEER).entries()]).toEqual([]);
    expect(ausSuchparametern(new URLSearchParams())).toEqual(LEER);
  });

  it("trägt nach einem Klick den gewählten Zeitraum", () => {
    const zustand: Dashboardzustand = { zeitraum: "30T", verteilung: null };

    expect(alsSuchparameter(zustand).get("zeitraum")).toBe("30T");
    expect(ausSuchparametern(alsSuchparameter(zustand))).toEqual(zustand);
  });

  /**
   * **`PARTNER` ist die Vorgabe des Endpunkts und steht deshalb nicht in der
   * URL** — dieselbe Regel wie beim Zeitraum, nur eine Ebene tiefer. Das ist
   * eine Ableitung aus E‑n und keine neue Entscheidung.
   */
  it("schreibt RICHTUNG in die URL und PARTNER nicht", () => {
    expect(alsSuchparameter({ zeitraum: null, verteilung: "RICHTUNG" }).get("verteilung")).toBe(
      "RICHTUNG",
    );
    expect(alsSuchparameter({ zeitraum: null, verteilung: "PARTNER" }).has("verteilung")).toBe(
      false,
    );
    expect(mitSicht("PARTNER")).toBeNull();
    expect(mitSicht("RICHTUNG")).toBe("RICHTUNG");
  });

  it("trägt beide Parameter auch in Kombination", () => {
    const zustand: Dashboardzustand = { zeitraum: "12M", verteilung: "RICHTUNG" };
    const url = alsSuchparameter(zustand);

    expect(url.toString()).toBe("zeitraum=12M&verteilung=RICHTUNG");
    expect(ausSuchparametern(url)).toEqual(zustand);
  });

  it("übergeht einen unbrauchbaren Wert, statt ihn in die URL zu lassen", () => {
    const url = new URLSearchParams("zeitraum=48&verteilung=KUNDE");

    expect(ausSuchparametern(url)).toEqual(LEER);
  });

  /**
   * **Der vom Endpunkt gewählte Zeitraum wird nie zurückgeschrieben.** Er steuert
   * allein, welche Schaltfläche gedrückt aussieht — und beim Empfänger eines
   * geteilten Links könnte der Endpunkt ein anderes Paar wählen, denn er wählt
   * nach dem *Mandanten*.
   */
  it("hebt das Paar der Antwort hervor, ohne es in die URL zu schreiben", () => {
    expect(hervorgehobenerZeitraum(LEER, "30T")).toBe("30T");
    expect(alsSuchparameter(LEER).has("zeitraum")).toBe(false);
  });

  /** Die Wahl schlägt die Antwort — sonst spränge die Hervorhebung beim Klick. */
  it("hebt die Wahl hervor, auch wenn die Antwort ein anderes Paar nennt", () => {
    expect(hervorgehobenerZeitraum({ zeitraum: "12M", verteilung: null }, "48H")).toBe("12M");
  });

  /** Solange nichts feststeht, ist keine Schaltfläche gedrückt. */
  it("hebt nichts hervor, solange weder Wahl noch Antwort dasteht", () => {
    expect(hervorgehobenerZeitraum(LEER, undefined)).toBeNull();
  });

  it("zeigt die Verteilungssicht der Vorgabe, solange nichts gewählt ist", () => {
    expect(hervorgehobeneSicht(LEER)).toBe("PARTNER");
    expect(hervorgehobeneSicht({ zeitraum: null, verteilung: "RICHTUNG" })).toBe("RICHTUNG");
  });
});

/**
 * **Die Verlinkung** (Entscheidung E‑m).
 *
 * Das Zeitfenster wird **unverändert** durchgereicht: Ein Ziel, das ein anderes
 * Fenster zeigt als die Kachel, ist derselbe Fehler wie eine Zahl, die nicht
 * stimmt.
 */
describe("Die Verweise der Kacheln", () => {
  it("baut die Adresse der Fehlerkachel", () => {
    expect(fehlerZiel(FENSTER)).toBe(
      "/nachrichten?status=FEHLER&von=2025-12-28T05%3A00%3A00Z&bis=2025-12-30T05%3A00%3A00Z",
    );
  });

  it("baut die Adresse der Überfälligkachel", () => {
    expect(ueberfaelligZiel(FENSTER)).toBe(
      "/nachrichten?ueberfaellig=true&von=2025-12-28T05%3A00%3A00Z&bis=2025-12-30T05%3A00%3A00Z",
    );
  });

  /**
   * **Die beiden Filter erscheinen nie zusammen.** Am Listen-Endpunkt sind sie
   * ausdrücklich unvereinbar und ergeben `400`
   * `ueberfaellig-und-status-unvereinbar`: Überfällig setzt `WARTEND` oder
   * `LAEUFT` voraus, Fehler ist ein Endstatus.
   */
  it("mischt die beiden Filter in keiner der beiden Adressen", () => {
    expect(fehlerZiel(FENSTER)).not.toContain("ueberfaellig");
    expect(ueberfaelligZiel(FENSTER)).not.toContain("status");
  });

  /**
   * **Das Fenster geht Zeichen für Zeichen durch.** Nicht durch `Date` und
   * wieder heraus: Das änderte die Schreibweise (`…:00Z` würde zu `…:00.000Z`),
   * ohne den Zeitpunkt zu ändern — und in einer geteilten URL sieht man den
   * Unterschied.
   */
  it("reicht die Fenstergrenzen unverändert durch", () => {
    const ziel = new URL(fehlerZiel(FENSTER), "https://example.invalid");

    expect(ziel.searchParams.get("von")).toBe(FENSTER.von);
    expect(ziel.searchParams.get("bis")).toBe(FENSTER.bis);
  });

  /**
   * **Auch bei `12M`.** Der Listen-Endpunkt weist eine Spanne über einem Jahr
   * mit `zeitfenster-zu-gross` ab und misst dabei ein **Kalenderjahr**
   * (`Zeitfenster.absolutes`: `von.isBefore(bis.minusYears(1))`), ausdrücklich
   * nicht 365 Tage. Das `12M`-Fenster ist genau zwölf Kalendermonate von
   * Monatsanfang zu Monatsanfang — die Prüfung greift nicht, auch nicht im
   * Schaltjahr.
   *
   * Der Test prüft die **Voraussetzung** dieser Aussage: dass das Ziel genau die
   * Grenzen der Antwort trägt und die Oberfläche nichts um einen Tag verkürzt,
   * um an der Grenze vorbeizukommen.
   */
  it("verkürzt das Zwölfmonatsfenster nicht, um an der Jahresgrenze vorbeizukommen", () => {
    const jahr = { von: "2024-03-01T00:00:00Z", bis: "2025-03-01T00:00:00Z" };
    const ziel = new URL(fehlerZiel(jahr), "https://example.invalid");

    expect(ziel.searchParams.get("von")).toBe(jahr.von);
    expect(ziel.searchParams.get("bis")).toBe(jahr.bis);
  });
});

/**
 * **Die Fehlerarten** — beschriftet über den **Rohwert** und nicht über die
 * gelieferte `art`.
 */
describe("Die Beschriftung der Fehlerarten", () => {
  it("übersetzt COMMIT_REJECTED", () => {
    expect(fehlerartText({ rohwert: "COMMIT_REJECTED", art: "Vom Partner abgelehnt" }, TEXTE)).toBe(
      "Vom Partner abgelehnt",
    );
  });

  /**
   * **Und zwar aus der Sprachdatei und nicht aus der Antwort.** Der Beleg ist
   * ein Rumpf, in dem `art` etwas anderes sagt: Wäre das Feld die Quelle, käme
   * hier sein Wert heraus — und im englischen Baum stünde Deutsch.
   */
  it("nimmt den Text aus der Sprachdatei und nicht aus dem Feld `art`", () => {
    expect(fehlerartText({ rohwert: "COMMIT_REJECTED", art: "irgendetwas anderes" }, TEXTE)).toBe(
      "Vom Partner abgelehnt",
    );
    expect(
      fehlerartText({ rohwert: "COMMIT_REJECTED", art: "Vom Partner abgelehnt" }, texteFuer("en")),
    ).toBe("Rejected by partner");
  });

  /** Ein unbekannter Rohwert bleibt Rohwert (Regel Q4). */
  it("lässt einen unbekannten Rohwert stehen", () => {
    expect(fehlerartText({ rohwert: "ERROR_TIMEOUT", art: "TIMEOUT" }, TEXTE)).toBe("TIMEOUT");
    expect(fehlerartText({ rohwert: "WAS_NEUES", art: "WAS_NEUES" }, TEXTE)).toBe("WAS_NEUES");
  });
});

/**
 * **Die Beschriftung der Verteilungszeilen.** Die Antwort trägt für die beiden
 * Restzeilen ausdrücklich keinen Text — das Backend stellt fest, die Oberfläche
 * beschriftet (Regel Q4).
 */
describe("Die Beschriftung der Verteilung", () => {
  const wert = (art: Verteilungszeile["art"], w: string | null, enthaltene: number | null) =>
    ({ art, wert: w, anzahl: 1, enthaltene }) satisfies Verteilungszeile;

  it("beschriftet die Sammelzeile mit der Zahl der enthaltenen Werte", () => {
    expect(verteilungszeileText(wert("UEBRIGE", null, 40), "PARTNER", TEXTE)).toBe("Übrige (40)");
  });

  it("beschriftet die Katalogzeile als nicht zugeordnet", () => {
    expect(verteilungszeileText(wert("NICHT_ZUGEORDNET", null, null), "PARTNER", TEXTE)).toBe(
      "nicht zugeordnet",
    );
  });

  /** Ein Partnername gehört dem Mandanten und wird nie übersetzt. */
  it("lässt einen Partnernamen unverändert", () => {
    expect(verteilungszeileText(wert("WERT", "EINGEHEND", null), "PARTNER", TEXTE)).toBe(
      "EINGEHEND",
    );
  });

  /** Die Richtung ist dagegen eine geschlossene Menge aus `catalog/Richtung`. */
  it("übersetzt die Richtung und lässt einen unbekannten Wert roh", () => {
    expect(verteilungszeileText(wert("WERT", "EINGEHEND", null), "RICHTUNG", TEXTE)).toBe(
      "Eingehend",
    );
    expect(verteilungszeileText(wert("WERT", "QUERGEHEND", null), "RICHTUNG", TEXTE)).toBe(
      "QUERGEHEND",
    );
  });
});
