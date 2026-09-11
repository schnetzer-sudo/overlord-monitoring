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
import {
  ABLAGENGRUENDE,
  type Ablagen,
  type Ablagengrund,
  type Dienstlampe,
} from "@/features/dashboard/api";
import {
  ABLAGENZEICHEN,
  DIENSTZEICHEN,
  grundsatz,
  lampenauskunft,
  sammelzeilenauskunft,
  traegtZielfarbe,
  zeigtRohwert,
} from "@/features/dashboard/plattform";
import { fehlerZiel, laeuftZiel, wartendFenster, wartendZiel } from "@/features/dashboard/verweise";
import {
  ABLAGENZUSTAENDE,
  DIENSTZUSTAENDE,
  STATUSARTEN,
  ablagenrolle,
  dienstrolle,
  statusrolle,
} from "@/lib/status-farbe";

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
 * **Die Verlinkung — asymmetrisch** (Entscheidungen E‑m und **E‑80**).
 *
 * *Fehler* und *Läuft* erben das Fenster der Antwort; **es geht Zeichen für
 * Zeichen durch**, denn ein Ziel, das ein anderes Fenster zeigt als die Kachel,
 * ist derselbe Fehler wie eine Zahl, die nicht stimmt. *Wartend* bringt seinen
 * eigenen Zeitraum mit — genau der Fall, für den E‑m die Klickbarkeit
 * abgeschaltet hatte.
 *
 * ⚠️ **Keine Zahl aus dem Bestand** (Regel T2). Die Werte hier sind gewählt und
 * rund; was die Testkopie heute liefert, steht in der Sichtprüfung und in keiner
 * Erwartung.
 */
describe("Die Verweise der Kacheln", () => {
  it("baut die Adresse der Fehlerkachel", () => {
    expect(fehlerZiel(FENSTER)).toBe(
      "/nachrichten?status=FEHLER&von=2025-12-28T05%3A00%3A00Z&bis=2025-12-30T05%3A00%3A00Z",
    );
  });

  /**
   * **Läuft erbt.** Eine laufende Nachricht ist höchstens so alt wie die
   * Wächterfrist des Altsystems — rund 30 Minuten — und liegt damit in jedem
   * Zeitraum, den der Umschalter anbietet.
   */
  it("lässt die Kachel Läuft das Fenster der Antwort erben", () => {
    expect(laeuftZiel(FENSTER)).toBe(
      "/nachrichten?status=LAEUFT&von=2025-12-28T05%3A00%3A00Z&bis=2025-12-30T05%3A00%3A00Z",
    );
  });

  /**
   * **Wartend bringt seinen Zeitraum mit.** Ohne das zeigte das Ziel bei 48
   * Stunden einen Bruchteil der Zahl, die auf der Kachel steht.
   */
  it("weitet das Fenster der Kachel Wartend bis vor die älteste Zeile", () => {
    expect(wartendZiel(FENSTER, "48H", 6 * 24 * 3600)).toBe(
      "/nachrichten?status=WARTEND&von=2025-12-24T00%3A00%3A00Z&bis=2025-12-30T05%3A00%3A00Z",
    );
  });

  /**
   * **Die Untergrenze ist der Punkt.** `aeltesteSekunden` ist ein Alter gegen
   * die Anwendungsuhr; der einzige Anker in der Antwort ist `fenster.bis`, und
   * der liegt **hinter** `jetzt`. Ohne Luft nach hinten fiele genau die Zeile
   * aus dem Ziel, um derentwillen jemand klickt.
   *
   * Geprüft wird der ungünstigste Fall: `jetzt` ganz am Anfang seines Eimers,
   * also `bis` minus eine Eimerbreite. Die älteste Zeile liegt dann so früh, wie
   * sie überhaupt liegen kann — und `von` muss trotzdem davor liegen.
   */
  it("greift nie zu spät, auch am ungünstigsten Punkt des Eimers nicht", () => {
    const alter = 6 * 24 * 3600;
    const eimerbreite48H = 60 * 60 * 1000;
    const fruehestesJetzt = new Date(FENSTER.bis).getTime() - eimerbreite48H;
    const aelteste = fruehestesJetzt - alter * 1000;

    const eigenes = wartendFenster(FENSTER, "48H", alter);
    if (eigenes === null) {
      throw new Error("Das Fenster der Kachel Wartend fehlt");
    }

    expect(new Date(eigenes.von).getTime()).toBeLessThanOrEqual(aelteste);
    // `bis` bleibt das Ende des gewählten Zeitraums, unverändert.
    expect(eigenes.bis).toBe(FENSTER.bis);
  });

  /**
   * Bei `12M` ist der Eimer ein Monat: `fenster.bis` kann Wochen hinter `jetzt`
   * liegen, und die Luft nach hinten wächst entsprechend mit.
   */
  it("nimmt bei einem breiteren Eimer mehr Luft", () => {
    const alter = 6 * 24 * 3600;
    const eng = wartendFenster(FENSTER, "48H", alter);
    const weit = wartendFenster(FENSTER, "12M", alter);
    if (eng === null || weit === null) {
      throw new Error("Beide Fenster liegen unter einem Jahr und dürfen nicht fehlen");
    }

    expect(new Date(weit.von).getTime()).toBeLessThan(new Date(eng.von).getTime());
  });

  /**
   * **Ohne älteste Zeile wird geerbt.** Das ist der Fall `anzahl = 0`: Es gibt
   * nichts, wofür das Fenster geweitet werden müsste, und das Ziel ist eine
   * leere Liste — die richtige Antwort auf eine Kachel, die `0` zeigt.
   */
  it("erbt, wenn es keine älteste Zeile gibt", () => {
    expect(wartendFenster(FENSTER, "48H", null)).toEqual(FENSTER);
  });

  /**
   * **Die Notbremse** (E‑80). Über der Höchstspanne der Liste — ein Jahr, Regel
   * L1 — weist der Endpunkt mit `zeitfenster-zu-gross` ab. Ein Link, der weniger
   * zeigt als die Kachel nennt, entsteht dann gar nicht erst; die Kachel klickt
   * nicht und sagt in einem Satz warum.
   */
  it("bremst über einem Jahr und gibt kein Ziel aus", () => {
    expect(wartendFenster(FENSTER, "48H", 366 * 24 * 3600)).toBeNull();
    expect(wartendZiel(FENSTER, "48H", 366 * 24 * 3600)).toBeNull();
  });

  /**
   * Die Gegenprobe. Ohne sie bewiese der Test oben nur, dass irgendwann `null`
   * herauskommt — nicht, dass die Grenze bei einem Jahr liegt.
   */
  it("bremst knapp darunter nicht", () => {
    expect(wartendZiel(FENSTER, "48H", 360 * 24 * 3600)).toContain("status=WARTEND");
  });

  /**
   * **Das Wort „überfällig" kommt in keiner Adresse mehr vor** (E‑71). Der
   * Parameter ist am Endpunkt seit dem 03.09.2026 unbekannt — wirkungslos und
   * kein Fehler —, und die Oberfläche erzeugt ihn nicht mehr.
   */
  it("erzeugt in keiner Adresse den alten Parameter", () => {
    const ziele = [
      fehlerZiel(FENSTER),
      laeuftZiel(FENSTER),
      wartendZiel(FENSTER, "48H", 6 * 24 * 3600) ?? "",
    ];

    for (const ziel of ziele) {
      expect(ziel).not.toContain("ueberfaellig");
    }
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

/* ─────────────────────────────────────────────────────────────────────────────
   Der Block *Plattform* (Schritt 10d, Teil B)

   Die vier Entscheidungen der Ansicht stehen als reine Funktionen in
   `features/dashboard/plattform.ts`, die beiden Farbzuordnungen in
   `lib/status-farbe.ts`. Was nur ein gerenderter Baum zeigt, steht in
   `tests/plattform-block.test.tsx` — und zwar nur das.
   ───────────────────────────────────────────────────────────────────────────── */

/** Eine Lampe, wie die Testkopie sie liefert (`docs/dienste.md` §13). */
const LAMPE: Dienstlampe = {
  serviceId: "MPSERVICEPROD03",
  zustand: "ZEITUEBERSCHRITTEN",
  rohwert: "ERROR_TIMEOUT",
  stand: "2025-12-30T03:09:41Z",
  alterSekunden: 6,
};

/** Die Ablagen im Zustand der Dev-Zeile: abgeschaltet, ohne Ziel, ohne Stand. */
const ABLAGEN: Ablagen = {
  zustand: "UNGEKLAERT",
  grund: "ABGESCHALTET",
  ziele: [],
  geprueftAm: null,
  alterSekunden: null,
};

describe("Die beiden neuen Farbzuordnungen sind vollständig (E‑128)", () => {
  /**
   * **Jeder Zustand bekommt eine Rolle**, und zwar eine der vier bestehenden.
   * Das ist die halbe Aussage von E‑121: zweite Anwendung derselben Rollen,
   * keine neue Farbe. Die andere Hälfte — dass in `globals.css` keine Zeile
   * hinzukommt — hält `tests/farbwerte.test.ts`.
   */
  it.each(DIENSTZUSTAENDE)("%s hat eine Rolle", (zustand) => {
    expect(["abgeschlossen", "fehler", "offen", "ungeklaert"]).toContain(dienstrolle(zustand));
  });

  it.each(ABLAGENZUSTAENDE)("%s hat eine Rolle", (zustand) => {
    expect(["abgeschlossen", "fehler", "offen", "ungeklaert"]).toContain(ablagenrolle(zustand));
  });

  /**
   * **Rot ist selten, und hier ist es genau zweimal vergeben** — dem Dienst,
   * der seine Zeitgrenze gerissen hat, und der Ablage, die nicht antwortet.
   * Ein geordnet heruntergefahrener Dienst ist **kein** Befund und deshalb
   * neutral (`docs/dienste.md` §6, `docs/visuelles-konzept.md` §3).
   */
  it("vergibt Rot nur an die beiden Zustände, die niemand wollte", () => {
    expect(DIENSTZUSTAENDE.filter((z) => dienstrolle(z) === "fehler")).toEqual([
      "ZEITUEBERSCHRITTEN",
    ]);
    expect(ABLAGENZUSTAENDE.filter((z) => ablagenrolle(z) === "fehler")).toEqual([
      "NICHT_ERREICHBAR",
    ]);
  });

  /** „Nicht zugeordnet heißt nicht zugeordnet" (Q4) — in beiden Aufzählungen. */
  it("gibt ungeklärt die eigene, zurückhaltende Rolle", () => {
    expect(dienstrolle("UNGEKLAERT")).toBe("ungeklaert");
    expect(ablagenrolle("UNGEKLAERT")).toBe("ungeklaert");
  });

  /**
   * **Die Wörter sind eigene und nicht die der bündelnden Farbrollen**
   * (E‑129).
   *
   * Geprüft wird die Abgrenzung zu **E‑82** an ihrem schärfsten Fall:
   * `MELDET_SICH` und `ERREICHBAR` tragen dieselbe Rolle wie *Erledigt*,
   * `HERUNTERGEFAHREN` dieselbe wie *Ohne Ergebnis* — und alle drei heißen
   * anders. Die beiden Beschriftungen gehören den **Einordnungen von
   * Nachrichten**; ein Dienst ist keines ihrer Mitglieder, und „Erledigt" an
   * einer Dienstlampe wäre kein zusammenfassendes Wort, sondern ein falsches.
   *
   * ⚠️ **Geprüft werden genau die beiden bündelnden Rollen und nicht alle
   * vier.** *Ungeklärt* heißt in beiden Mengen gleich, und das ist richtig:
   * `--status-ungeklaert` bündelt nichts — die Beschriftung ist der Name ihres
   * einzigen Mitglieds, und E‑82 hat dazu nichts zu sagen. Die erste Fassung
   * dieses Falls war hier zu scharf gestellt und wurde am 10.09.2026 auf die
   * Aussage von E‑82 zurückgeführt.
   */
  it("beschriftet nie mit der Beschriftung einer bündelnden Farbrolle", () => {
    const buendelnd = [
      TEXTE.dashboard.verlauf.rollen.offen,
      TEXTE.dashboard.verlauf.rollen.abgeschlossen,
    ];

    for (const zustand of DIENSTZUSTAENDE) {
      expect(buendelnd).not.toContain(TEXTE.dashboard.plattform.dienst[zustand]);
    }
    for (const zustand of ABLAGENZUSTAENDE) {
      expect(buendelnd).not.toContain(TEXTE.dashboard.plattform.ablage[zustand]);
    }

    // Die Gegenprobe: Es sind wirklich dieselben Rollen — die Wörter fallen
    // auseinander, die Farbe nicht (E‑121).
    expect(dienstrolle("MELDET_SICH")).toBe("abgeschlossen");
    expect(dienstrolle("HERUNTERGEFAHREN")).toBe("offen");
    expect(ablagenrolle("ERREICHBAR")).toBe("abgeschlossen");
  });
});

describe("Der Rohwert steht nur an der ungeklärten Lampe (E‑130)", () => {
  it("zeigt ihn bei ungeklärt", () => {
    expect(zeigtRohwert("UNGEKLAERT")).toBe(true);
  });

  /**
   * **Neben „Zeitüberschreitung" sagte `ERROR_TIMEOUT` dasselbe ein zweites
   * Mal**, in der Sprache der Anlage statt in der des Nutzers. Geliefert wird
   * er trotzdem bei jeder Lampe (E‑118) — das ist der Unterschied zwischen dem
   * Vertrag und der Anzeige.
   */
  it.each(DIENSTZUSTAENDE.filter((z) => z !== "UNGEKLAERT"))(
    "verschweigt ihn bei %s",
    (zustand) => {
      expect(zeigtRohwert(zustand)).toBe(false);
    },
  );
});

describe("Eine Zielzeile trägt ihre Farbe nur, solange der Stand gilt (E‑133)", () => {
  it("färbt die Zeilen einer Kachel, die etwas festgestellt hat", () => {
    expect(traegtZielfarbe("ERREICHBAR", null)).toBe(true);
    expect(traegtZielfarbe("NICHT_ERREICHBAR", null)).toBe(true);
  });

  /**
   * **`ZIEL_UNGEKLAERT` ist der eine ungeklärte Fall mit frischem Beleg:** Der
   * Durchgang hat stattgefunden, nur eine Antwort ließ sich nicht einordnen.
   */
  it("färbt die Zeilen auch, wenn nur eine Antwort unklar war", () => {
    expect(traegtZielfarbe("UNGEKLAERT", "ZIEL_UNGEKLAERT")).toBe(true);
  });

  /**
   * **Grün überlebt seinen Beleg auch in der Anzeige nicht.** Das Backend nimmt
   * der Kachel ihre Aussage, sobald der Stand älter ist als zwei Takte (E‑125);
   * stünde die Zeile darunter weiterhin grün, wäre dieselbe Aussage über
   * denselben Umweg wieder im Bild.
   */
  it.each(ABLAGENGRUENDE.filter((g) => g !== "ZIEL_UNGEKLAERT"))(
    "nimmt ihnen die Farbe bei %s",
    (grund: Ablagengrund) => {
      expect(traegtZielfarbe("UNGEKLAERT", grund)).toBe(false);
    },
  );
});

describe("Jeder Grund hat seinen Satz (E‑134)", () => {
  /** Fünf Gründe, fünf Sätze — und jeder ein ganzer, kein Schlagwort. */
  it.each(ABLAGENGRUENDE)("%s nennt den Grund in einem Satz", (grund) => {
    const satz = grundsatz(grund, TEXTE);
    expect(satz).toBeTruthy();
    expect(satz).toMatch(/\.$/);
  });

  /**
   * **Ohne Grund kein Satz.** Bei `ERREICHBAR` und `NICHT_ERREICHBAR` liefert
   * das Backend `grund: null`, und die Auskunft steht in der Plakette; ein
   * zusätzlicher Satz wäre dieselbe Aussage ein zweites Mal.
   */
  it("schreibt nichts, wo es nichts zu erklären gibt", () => {
    expect(grundsatz(null, TEXTE)).toBeNull();
  });
});

describe("Vier Zeichen, vier Formen (10.09.2026)", () => {
  /**
   * **Die Kachel trägt kein Wort im Bild** — je Zeile steht nur Zeichen und
   * Kennung (Bauform E‑91). Damit ist das Zeichen die halbe Aussage
   * **allein**, und `docs/visuelles-konzept.md` §3 verlangt dann, dass es sich
   * in der **Form** unterscheidet und nicht nur in der Farbe.
   *
   * ⚠️ **Der Anlass ist ein Befund der Durchsicht:** Auf der Aufnahme waren
   * das Zeichen für „heruntergefahren" und das der abgeschalteten Ablage nicht
   * auseinanderzuhalten — zwei Kreise mit einem Strich darin. Geprüft wird
   * deshalb, dass die vier Verzeichniseinträge **vier verschiedene**
   * Komponenten sind; welche Umrisse sie zeichnen, sagt erst die Aufnahme.
   */
  it("gibt jedem Dienstzustand ein eigenes Zeichen", () => {
    const zeichen = DIENSTZUSTAENDE.map((zustand) => DIENSTZEICHEN[zustand]);
    expect(new Set(zeichen).size).toBe(DIENSTZUSTAENDE.length);
  });

  it("gibt jedem Ablagenzustand ein eigenes Zeichen", () => {
    const zeichen = ABLAGENZUSTAENDE.map((zustand) => ABLAGENZEICHEN[zustand]);
    expect(new Set(zeichen).size).toBe(ABLAGENZUSTAENDE.length);
  });

  /**
   * **Geteilt ist die Formensprache, nicht das Verzeichnis.** Es sind zwei
   * Aufzählungen (E‑128) — aber „erreichbar" und „meldet sich" sind
   * dieselbe Auskunft in derselben Kachel und tragen dasselbe Häkchen; ein
   * zweites grünes Zeichen wäre ein zweites Vokabular.
   */
  it("nimmt für dieselbe Auskunft dasselbe Zeichen", () => {
    expect(ABLAGENZEICHEN.ERREICHBAR).toBe(DIENSTZEICHEN.MELDET_SICH);
    expect(ABLAGENZEICHEN.NICHT_ERREICHBAR).toBe(DIENSTZEICHEN.ZEITUEBERSCHRITTEN);
    expect(ABLAGENZEICHEN.UNGEKLAERT).toBe(DIENSTZEICHEN.UNGEKLAERT);
  });
});

describe("Was eine Dienstzeile sagt (E‑130 im `title`)", () => {
  /**
   * **Das Wort allein, solange es eine Auskunft ist.** Neben
   * „Zeitüberschreitung" sagte `ERROR_TIMEOUT` dasselbe ein zweites Mal, in
   * der Sprache der Anlage statt in der des Nutzers.
   */
  it("nennt bei einer eingeordneten Zeile nur das Wort", () => {
    expect(lampenauskunft(LAMPE, TEXTE)).toBe(TEXTE.dashboard.plattform.dienst.ZEITUEBERSCHRITTEN);
    expect(lampenauskunft(LAMPE, TEXTE)).not.toContain("ERROR_TIMEOUT");
  });

  /**
   * **Bei „ungeklärt" ist der Rohwert die ganze Auskunft** (Regel Q4): Ohne
   * ihn bliebe ein Achselzucken, und niemand könnte nachsehen, was im
   * Altsystem tatsächlich steht. Beides steht in **einem** Satz — ein `title`
   * kennt keine zweite Zeile.
   */
  it("nennt bei einer ungeklärten Zeile Wort und Rohwert", () => {
    const satz = lampenauskunft({ ...LAMPE, zustand: "UNGEKLAERT", rohwert: "PAUSED" }, TEXTE);
    expect(satz).toContain(TEXTE.dashboard.plattform.dienst.UNGEKLAERT);
    expect(satz).toContain("PAUSED");
  });

  /**
   * **Und wo keiner steht, wird keiner erfunden.** Der Klassifizierer ordnet
   * auch eine leere Spalte `UNGEKLAERT` zu; dann steht dort ein Satz und keine
   * leere Stelle — „nicht zugeordnet heißt nicht zugeordnet".
   */
  it("sagt es, wenn die Anlage gar keinen Wert führt", () => {
    const satz = lampenauskunft({ ...LAMPE, zustand: "UNGEKLAERT", rohwert: null }, TEXTE);
    expect(satz).toContain(TEXTE.dashboard.plattform.rohwertFehlt);
  });
});

describe("Die Sammelzeile der Ablagen (10.09.2026)", () => {
  /**
   * **Wo kein Ziel geprüft wurde, steht trotzdem eine Zeile.** Drei der fünf
   * Gründe liefern gar kein Ziel (`docs/dienste.md` §8); ohne diese Zeile
   * verschwände die Ablagenprüfung **spurlos** aus der Kachel, und Abwesenheit
   * ist der schwächste Kanal, den eine Auskunft haben kann (E‑74, E‑81,
   * E‑135).
   */
  it.each(["ABGESCHALTET", "NOCH_KEIN_DURCHGANG", "KEIN_ZIEL_EINGETRAGEN"] as const)(
    "nennt bei %s das Wort und den Grund",
    (grund: Ablagengrund) => {
      const satz = sammelzeilenauskunft({ ...ABLAGEN, grund }, TEXTE);
      expect(satz).toContain(TEXTE.dashboard.plattform.ablage.UNGEKLAERT);
      expect(satz).toContain(TEXTE.dashboard.plattform.grund[grund]);
    },
  );

  /**
   * **Sie steht fest auf `UNGEKLAERT` und nicht auf dem Zustand der Kachel.**
   * Eine Zeile mit der Aufschrift „Ablagen" und einem grünen Häkchen sagte
   * *alle Ablagen sind erreichbar* — und genau diese Aussage schließt
   * **E‑132** aus: Geprüft wird eine Stichprobe aus
   * `ServiceDefaultFileStore` (offener Punkt 166).
   */
  it("bleibt ungeklärt, auch wenn der Zustand etwas anderes behauptet", () => {
    const satz = sammelzeilenauskunft({ ...ABLAGEN, zustand: "ERREICHBAR", grund: null }, TEXTE);
    expect(satz).toBe(TEXTE.dashboard.plattform.ablage.UNGEKLAERT);
    expect(satz).not.toContain(TEXTE.dashboard.plattform.ablage.ERREICHBAR);
  });

  /** **Und sie schweigt, sobald es Ziele gibt.** Dann nennt die Kachel sie einzeln. */
  it("entfällt, sobald ein Ziel geprüft worden ist", () => {
    const mitZiel: Ablagen = {
      ...ABLAGEN,
      ziele: [{ serviceId: "FILESTOREPROD10", zustand: "ERREICHBAR" }],
    };
    expect(sammelzeilenauskunft(mitZiel, TEXTE)).toBeNull();
  });
});
