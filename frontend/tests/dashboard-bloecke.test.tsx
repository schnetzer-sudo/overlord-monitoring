// @vitest-environment jsdom

import { act } from "react";
import { NuqsTestingAdapter } from "nuqs/adapters/testing";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { texteFuer } from "@/i18n";
import type { Dashboard, Verteilung } from "@/features/dashboard/api";
import { DashboardAnsicht } from "@/features/dashboard/components/dashboard-ansicht";
import { AufgefallenBlock } from "@/features/dashboard/components/aufgefallen-block";
import { Kacheln } from "@/features/dashboard/components/kacheln";
import { VerteilungBlock } from "@/features/dashboard/components/verteilung-block";

import { rendere } from "./hilfe/rendern";

/**
 * **Die Fälle des Dashboards, für die ein gerenderter Baum die einzige Prüfung
 * ist.**
 *
 * Alles Übrige steht als reine Funktion in `tests/dashboard.test.ts` — die
 * Zusammenfassung auf vier Reihen, der URL-Zustand, die Adressen der Kacheln,
 * die Beschriftungen. Hier stehen die Fälle, die Aussagen über **Abwesenheit**
 * oder über **Reihenfolge** sind; beide sind ohne Baum nicht zu treffen
 * (`docs/frontend-grundlagen.md` §9).
 *
 * | Fall | Warum genau dieser |
 * |---|---|
 * | **die drei Zustände von *Wartend*** | strukturell abwesend, nicht ermittelbar, ermittelt — **drei Bilder, und zwei davon dürfen nie gleich aussehen** (E‑81). Alle drei sind Aussagen über Abwesenheit; eine reine Funktion sagt nichts darüber, was *nicht* gerendert wird, und die naheliegende Schreibweise `anzahl ?? 0` bestünde jede Prüfung an ihr |
 * | die Notbremse und die zweite Zeile | ein Verweis, der **nicht** da ist, und eine Zeile, die bei `anzahl = 0` **entfällt** |
 * | die Reihenfolge und der Umbruch | **vier Kacheln, und bei drei keine Lücke.** Eine Reihenfolge ist ohne Baum nicht zu treffen, und die Spaltenzahl steht in der Klassenkette des Gitters |
 * | die beiden Restzeilen | „Übrige" **fehlt**, wenn der Endpunkt sie nicht liefert; „nicht zugeordnet" **steht da**, auch bei null. Dazu die **Reihenfolge**: Beide bleiben unten, auch wenn „Übrige" der größte Balken ist. Ein Sortiervergleich ist kein Vorhandensein |
 * | der Leerzustand | **Satz und Umschalter, sonst nichts** — samt der Gegenprobe, dass die Nullzeile der Verteilung dort *nicht* steht |
 * | ein Aufruf | Genau **eine** Anfrage an `/api/dashboard`, und ein Sichtwechsel lädt **nicht nach**, sondern ruft neu. Beides sind Aussagen über die Zahl der Anfragen |
 *
 * **Antwortrümpfe gestellt, kein Netz, keine Datenbank.**
 */

const TEXTE = texteFuer("de");
const D = TEXTE.dashboard;
// Die drei Rollup-Paare stehen seit dem 02.09.2026 auf oberster Ebene: Der
// Umschalter ist nach `components/` gewandert und liest keinen Textblock eines
// Features mehr (`docs/process-view.md` E-47).
const Z = TEXTE.zeitraum;

const FENSTER = { von: "2025-12-28T05:00:00Z", bis: "2025-12-30T05:00:00Z" };

const VERTEILUNG_VOLL: Verteilung = {
  sicht: "PARTNER",
  zeilen: [
    { art: "WERT", wert: "BMW", anzahl: 120, enthaltene: null },
    { art: "WERT", wert: "ARCHROMA", anzahl: 40, enthaltene: null },
    // **Die größte Zeile des Blocks**, und sie steht trotzdem unten. Genau die
    // Gestalt aus M98, Befund 21: Bei `IBIS` trägt „Übrige (40)" 27,92 %.
    { art: "UEBRIGE", wert: null, anzahl: 900, enthaltene: 40 },
    { art: "NICHT_ZUGEORDNET", wert: null, anzahl: 0, enthaltene: null },
  ],
};

/**
 * Die fünfte Kachel der Reihe seit dem 10.09.2026 (Schritt 10d Teil B). Sie
 * gehört zum Vertrag und darf deshalb auch im gestellten Rumpf nicht fehlen;
 * **geprüft wird sie hier nicht** — ihre Fälle stehen in
 * `tests/plattform-block.test.tsx`.
 */
const PLATTFORM: Dashboard["plattform"] = {
  dienste: [
    {
      serviceId: "MPSERVICEPROD03",
      zustand: "ZEITUEBERSCHRITTEN",
      rohwert: "ERROR_TIMEOUT",
      stand: "2025-12-30T03:09:41Z",
      alterSekunden: 6,
    },
  ],
  ablagen: {
    zustand: "UNGEKLAERT",
    grund: "ABGESCHALTET",
    ziele: [],
    geprueftAm: null,
    alterSekunden: null,
  },
};

function antwort(ueberschreibung: Partial<Dashboard> = {}): Dashboard {
  return {
    zeitraum: "48H",
    fenster: FENSTER,
    leer: false,
    verlauf: [
      {
        eimer: "2025-12-28T05:00:00Z",
        gesamt: 9,
        einordnungen: [
          { einordnung: "ABGESCHLOSSEN", anzahl: 8 },
          { einordnung: "FEHLER", anzahl: 1 },
        ],
      },
    ],
    kacheln: {
      nachrichten: 9950,
      fehler: {
        anzahl: 50,
        arten: [{ rohwert: "ERROR_TIMEOUT", art: "TIMEOUT", anzahl: 49 }],
      },
      laeuft: { anzahl: 0, aeltesteSekunden: null, ermittelbar: true },
      wartend: { anzahl: 12, aeltesteSekunden: 6 * 24 * 3600, ermittelbar: true },
    },
    verteilung: VERTEILUNG_VOLL,
    zuletztAufgefallen: [],
    stand: { beendetAm: "2025-12-30T04:10:00Z", art: "VOLL" },
    // Der neunte Block seit Schritt 10d. Er gehört zum Vertrag und darf
    // deshalb auch im Rumpf nicht fehlen; **geprüft wird er hier nicht** —
    // seine eigenen Fälle stehen in `tests/plattform-block.test.tsx`.
    plattform: PLATTFORM,
    ...ueberschreibung,
  };
}

/** Ein `fetch`, das jeden Aufruf sichtbar macht. */
let anfragen: string[] = [];

function stelleAntwort(rumpf: Dashboard): void {
  vi.stubGlobal(
    "fetch",
    vi.fn((eingabe: RequestInfo | URL) => {
      const adresse = String(eingabe);
      anfragen.push(adresse);
      return Promise.resolve(
        new Response(JSON.stringify(rumpf), {
          status: 200,
          headers: { "content-type": "application/json" },
        }),
      );
    }),
  );
}

beforeEach(() => {
  anfragen = [];
});

afterEach(() => {
  vi.unstubAllGlobals();
});

/**
 * Lässt die gestellte Antwort ankommen.
 *
 * `rendere` wartet ein `act` ab; die Antwort von `fetch` braucht danach noch
 * einen Zug durch die Warteschlange von TanStack Query. Gewartet wird auf den
 * **Zustand** und nicht auf eine Zeitspanne — eine feste Wartezeit wäre eine
 * Zusicherung über die Wanduhr.
 */
async function warteAufAntwort(behaelter: HTMLElement): Promise<void> {
  for (let zug = 0; zug < 20; zug++) {
    if (!(behaelter.textContent ?? "").includes(TEXTE.zustand.laedt)) {
      return;
    }
    await act(async () => {
      await new Promise((fertig) => setTimeout(fertig, 0));
    });
  }
  throw new Error("Die gestellte Antwort ist nicht angekommen");
}

/** Die Ansicht in der kleinsten Umgebung, die sie braucht: Texte, Speicher, URL. */
async function rendereAnsicht(suchparameter = "") {
  const gerendert = await rendere(
    <NuqsTestingAdapter searchParams={suchparameter} hasMemory>
      <DashboardAnsicht />
    </NuqsTestingAdapter>,
  );
  await warteAufAntwort(gerendert.behaelter);
  return gerendert;
}

const D_KACHELN = D.kacheln;

/** Die Bausteine der gestellten Kachelantworten — bewusst runde, gewählte Zahlen (Regel T2). */
const FEHLERKACHEL = { anzahl: 50, arten: [] };
const LAEUFT_LEER = { anzahl: 0, aeltesteSekunden: null, ermittelbar: true };
const SECHS_TAGE = 6 * 24 * 3600;

/**
 * **Die Plattform ist seit dem 10.09.2026 die fünfte Kachel der Reihe** und
 * gehört deshalb zu ihren Pflichtangaben. **Geprüft wird sie hier nicht** —
 * ihre eigenen Fälle stehen in `tests/plattform-block.test.tsx`; hier steht sie
 * nur, damit die Reihe vollständig ist.
 */
async function rendereKacheln(kacheln: Dashboard["kacheln"]) {
  return rendere(
    <Kacheln kacheln={kacheln} plattform={PLATTFORM} fenster={FENSTER} zeitraum="48H" />,
  );
}

function ziele(behaelter: HTMLElement): (string | null)[] {
  return [...behaelter.querySelectorAll("a")].map((a) => a.getAttribute("href"));
}

/**
 * **Drei Zustände, drei Bilder** (Entscheidung E‑81).
 *
 * Abwesenheit ist eine Auskunft über den **Mandanten** („hat keine Abläufe, die
 * suspendieren"), `ermittelbar: false` eine über **uns** („wissen es gerade
 * nicht"). Verschwände die Kachel bei einem Fehlschlag, würde ein Ausfall
 * stillschweigend in eine strukturelle Behauptung übersetzt — der schlimmste der
 * drei denkbaren Fehler an dieser Stelle.
 */
describe("Die drei Zustände der Kachel Wartend", () => {
  /**
   * **Fehlt der Schlüssel, gibt es die Kachel nicht.** Kein Platzhalter, keine
   * gedämpfte Kachel, kein „nicht verfügbar" — und die Reihe zieht sich von
   * hinten auf drei zusammen, statt eine Lücke in die Mitte zu schlagen.
   */
  it("zeichnet ohne den Schlüssel gar keine Kachel", async () => {
    const gerendert = await rendereKacheln({
      nachrichten: 9950,
      fehler: FEHLERKACHEL,
      laeuft: LAEUFT_LEER,
    });

    try {
      const text = gerendert.behaelter.textContent ?? "";

      expect(text).not.toContain(TEXTE.einordnung.WARTEND);
      expect(text).not.toContain(D_KACHELN.nichtErmittelbarHinweis);
      // Die drei übrigen stehen, und keine von ihnen führt auf `WARTEND`.
      expect(text).toContain(TEXTE.einordnung.LAEUFT);
      expect(text).toContain(D_KACHELN.fehler);
      expect(text).toContain(D_KACHELN.nachrichten);
      expect(ziele(gerendert.behaelter).some((ziel) => ziel?.includes("WARTEND"))).toBe(false);
    } finally {
      await gerendert.abbauen();
    }
  });

  /**
   * **„Nicht ermittelbar" ist nicht `0`** (Entscheidung E‑q) und **sichtbar
   * anders als die fehlende Kachel**: Die Plakette steht da, ein Satz nennt den
   * Grund, und geklickt wird nicht.
   */
  it("zeigt bei ermittelbar: false weder Null noch Verweis — und steht trotzdem da", async () => {
    const gerendert = await rendereKacheln({
      nachrichten: 9950,
      fehler: FEHLERKACHEL,
      laeuft: LAEUFT_LEER,
      wartend: { anzahl: null, aeltesteSekunden: null, ermittelbar: false },
    });

    try {
      const text = gerendert.behaelter.textContent ?? "";

      // Genau der Unterschied zum Fall darüber: Die Kachel ist da.
      expect(text).toContain(TEXTE.einordnung.WARTEND);
      expect(text).toContain(D_KACHELN.nichtErmittelbar);
      expect(text).toContain(D_KACHELN.nichtErmittelbarHinweis);
      expect(ziele(gerendert.behaelter).some((ziel) => ziel?.includes("WARTEND"))).toBe(false);

      // **Die beiden Kacheln fallen nicht zusammen** — zwei Statements, zwei
      // Auskünfte. Läuft steht mit seiner Null da und klickt weiter.
      expect(text).toContain(TEXTE.einordnung.LAEUFT);
      expect(ziele(gerendert.behaelter).some((ziel) => ziel?.includes("LAEUFT"))).toBe(true);
    } finally {
      await gerendert.abbauen();
    }
  });

  /**
   * Die Gegenprobe zu beiden. Ohne sie bewiesen die Tests oben nur, dass
   * irgendetwas fehlt — nicht, dass es **wegen** des Zustands fehlt.
   */
  it("zeigt ermittelt die Zahl, das Alter und den Verweis mit eigenem Fenster", async () => {
    const gerendert = await rendereKacheln({
      nachrichten: 9950,
      fehler: FEHLERKACHEL,
      laeuft: LAEUFT_LEER,
      wartend: { anzahl: 12, aeltesteSekunden: SECHS_TAGE, ermittelbar: true },
    });

    try {
      const text = gerendert.behaelter.textContent ?? "";

      expect(text).toContain("12");
      expect(text).not.toContain(D_KACHELN.nichtErmittelbarHinweis);
      // Die zweite Zeile — nach E‑75 die laufende Prüfung der Auskunft, auf der
      // E‑71 ruht.
      expect(text).toContain("6 d");
      // **Das eigene Fenster steht in der Adresse** (E‑80): weiter zurück als
      // das der Antwort, damit die älteste Zeile im Ziel liegt.
      const wartend = ziele(gerendert.behaelter).find((ziel) => ziel?.includes("WARTEND"));
      expect(wartend).toContain("von=2025-12-24T00%3A00%3A00Z");
      expect(wartend).toContain("bis=2025-12-30T05%3A00%3A00Z");
    } finally {
      await gerendert.abbauen();
    }
  });

  /**
   * **Die zweite Zeile entfällt bei `anzahl = 0`** — kein „—", kein „keine". Die
   * Null steht für sich; ohne Zeile gibt es kein Alter. Das ist der Fall, den
   * die Erscheinungsbedingung von „dieser Mandant wartet nie" unterscheidet: Die
   * Kachel steht da und zeigt eine Null.
   */
  it("lässt die zweite Zeile bei null weg und klickt trotzdem", async () => {
    const gerendert = await rendereKacheln({
      nachrichten: 9950,
      fehler: FEHLERKACHEL,
      laeuft: LAEUFT_LEER,
      wartend: { anzahl: 0, aeltesteSekunden: null, ermittelbar: true },
    });

    try {
      const text = gerendert.behaelter.textContent ?? "";

      expect(text).toContain(TEXTE.einordnung.WARTEND);
      expect(text).not.toContain(D_KACHELN.aeltesterSeit.replace(" {dauer}", ""));
      // Das Ziel erbt dann das Fenster der Antwort und zeigt eine leere Liste —
      // die richtige Antwort auf eine Kachel, die `0` zeigt.
      const wartend = ziele(gerendert.behaelter).find((ziel) => ziel?.includes("WARTEND"));
      expect(wartend).toContain("von=2025-12-28T05%3A00%3A00Z");
    } finally {
      await gerendert.abbauen();
    }
  });

  /**
   * **Die Notbremse im Baum** (E‑80): Liegt die älteste Zeile über einem Jahr
   * zurück, klickt die Kachel **nicht** und sagt in einem Satz warum. Die Zahl
   * bleibt stehen — sie ist richtig, nur ihr Ziel wäre es nicht.
   */
  it("klickt über einem Jahr nicht und nennt den Grund", async () => {
    const gerendert = await rendereKacheln({
      nachrichten: 9950,
      fehler: FEHLERKACHEL,
      laeuft: LAEUFT_LEER,
      wartend: { anzahl: 12, aeltesteSekunden: 366 * 24 * 3600, ermittelbar: true },
    });

    try {
      const text = gerendert.behaelter.textContent ?? "";

      expect(text).toContain("12");
      expect(text).toContain(D_KACHELN.wartendOhneVerweis);
      expect(ziele(gerendert.behaelter).some((ziel) => ziel?.includes("WARTEND"))).toBe(false);
      // Kein Fehlerzustand: Die übrigen Kacheln klicken weiter.
      expect(ziele(gerendert.behaelter).some((ziel) => ziel?.includes("status=FEHLER"))).toBe(true);
    } finally {
      await gerendert.abbauen();
    }
  });
});

/**
 * **Fehler · Läuft · Wartend · Nachrichten** (Entscheidung E‑78) — erst was zu
 * tun ist, dann was in Arbeit ist, dann die Zählung.
 *
 * *Wartend* steht **vor** *Nachrichten*, damit sein Wegfall die Reihe von hinten
 * auf drei zusammenzieht statt eine Lücke in die Mitte zu schlagen. Und die
 * Spaltenzahl am breiten Fenster folgt der Zahl der Kacheln: Bei drei bliebe
 * sonst eine leere vierte Spalte stehen, und eine Lücke sähe aus wie eine
 * fehlende Zahl.
 */
describe("Die Reihenfolge der Kacheln", () => {
  /** Die Kachelköpfe in der Reihenfolge, in der sie im Baum stehen. */
  function reihenfolge(behaelter: HTMLElement): string[] {
    const text = behaelter.textContent ?? "";
    return [
      D_KACHELN.fehler,
      TEXTE.einordnung.LAEUFT,
      TEXTE.einordnung.WARTEND,
      D_KACHELN.nachrichten,
      TEXTE.dashboard.plattform.titel,
    ]
      .map((wort) => [wort, text.indexOf(wort)] as const)
      .filter(([, stelle]) => stelle >= 0)
      .sort((eins, zwei) => eins[1] - zwei[1])
      .map(([wort]) => wort);
  }

  it("steht bei fünf Kacheln in der Reihenfolge des Leitsatzes", async () => {
    const gerendert = await rendereKacheln({
      nachrichten: 9950,
      fehler: FEHLERKACHEL,
      laeuft: LAEUFT_LEER,
      wartend: { anzahl: 12, aeltesteSekunden: SECHS_TAGE, ermittelbar: true },
    });

    try {
      expect(reihenfolge(gerendert.behaelter)).toEqual([
        D_KACHELN.fehler,
        TEXTE.einordnung.LAEUFT,
        TEXTE.einordnung.WARTEND,
        D_KACHELN.nachrichten,
        TEXTE.dashboard.plattform.titel,
      ]);
      expect(gerendert.behaelter.querySelector("[class*='xl:grid-cols-5']")).not.toBeNull();
    } finally {
      await gerendert.abbauen();
    }
  });

  it("zieht sich bei vier Kacheln zusammen, ohne eine Spalte leer zu lassen", async () => {
    const gerendert = await rendereKacheln({
      nachrichten: 9950,
      fehler: FEHLERKACHEL,
      laeuft: LAEUFT_LEER,
    });

    try {
      expect(reihenfolge(gerendert.behaelter)).toEqual([
        D_KACHELN.fehler,
        TEXTE.einordnung.LAEUFT,
        D_KACHELN.nachrichten,
        TEXTE.dashboard.plattform.titel,
      ]);
      expect(gerendert.behaelter.querySelector("[class*='xl:grid-cols-4']")).not.toBeNull();
      expect(gerendert.behaelter.querySelector("[class*='xl:grid-cols-5']")).toBeNull();
    } finally {
      await gerendert.abbauen();
    }
  });
});

/**
 * **Die beiden Restzeilen verhalten sich verschieden**, und das Frontend rechnet
 * nichts nach: Es zeigt, was der Endpunkt liefert, in der Reihenfolge, in der er
 * es liefert.
 */
describe("Die Restzeilen der Verteilung", () => {
  async function rendereVerteilung(verteilung: Verteilung) {
    return rendere(<VerteilungBlock verteilung={verteilung} aufSicht={() => undefined} />);
  }

  /**
   * **Die Reihenfolge, nicht das Vorhandensein.** „Übrige" ist hier mit 900 der
   * größte Balken des Blocks — nach Größe sortiert stünde sie auf Rang 1, als
   * gäbe es einen Partner dieses Namens (M98, Befund 21).
   */
  it("hält beide Restzeilen unten, auch wenn Übrige der größte Balken ist", async () => {
    const gerendert = await rendereVerteilung(VERTEILUNG_VOLL);

    try {
      const zeilen = [...gerendert.behaelter.querySelectorAll("li")].map(
        (li) => li.textContent ?? "",
      );

      expect(zeilen).toHaveLength(4);
      expect(zeilen[0]).toContain("BMW");
      expect(zeilen[2]).toContain("Übrige (40)");
      expect(zeilen[3]).toContain(D.verteilung.nichtZugeordnet);
    } finally {
      await gerendert.abbauen();
    }
  });

  /**
   * **„Übrige" fehlt, wenn der Endpunkt sie nicht liefert.** Ohne Rang 11 gibt
   * es sie nicht, und eine Null wäre dort reines Rangartefakt.
   */
  it("lässt Übrige weg, wenn der Endpunkt sie nicht liefert", async () => {
    const gerendert = await rendereVerteilung({
      sicht: "PARTNER",
      zeilen: [
        { art: "WERT", wert: "BMW", anzahl: 120, enthaltene: null },
        { art: "NICHT_ZUGEORDNET", wert: null, anzahl: 7, enthaltene: null },
      ],
    });

    try {
      const zeilen = [...gerendert.behaelter.querySelectorAll("li")].map(
        (li) => li.textContent ?? "",
      );

      expect(zeilen).toHaveLength(2);
      expect(gerendert.behaelter.textContent).not.toContain("Übrige");
      expect(zeilen[1]).toContain(D.verteilung.nichtZugeordnet);
    } finally {
      await gerendert.abbauen();
    }
  });

  /**
   * **„Nicht zugeordnet" erscheint auch bei null.** Sie ist eine Aussage über
   * den *Katalog*: Null heißt „alles kuratiert". Ausgeblendet wäre *vollständig
   * gepflegt* nicht mehr von *diese Ansicht zeigt das nicht* zu unterscheiden.
   */
  it("zeigt nicht zugeordnet auch bei null", async () => {
    const gerendert = await rendereVerteilung({
      sicht: "PARTNER",
      zeilen: [{ art: "NICHT_ZUGEORDNET", wert: null, anzahl: 0, enthaltene: null }],
    });

    try {
      const zeilen = [...gerendert.behaelter.querySelectorAll("li")];
      expect(zeilen).toHaveLength(1);
      expect(zeilen[0]?.textContent).toContain(D.verteilung.nichtZugeordnet);
      expect(zeilen[0]?.textContent).toContain("0");
    } finally {
      await gerendert.abbauen();
    }
  });
});

/**
 * **Der Leerzustand: ein Satz und ein bedienbarer Umschalter, sonst nichts**
 * (Entscheidung E‑p).
 */
describe("Der Leerzustand", () => {
  it("zeigt den Satz und lässt den Umschalter bedienbar", async () => {
    stelleAntwort(antwort({ leer: true }));
    const gerendert = await rendereAnsicht();

    try {
      const text = gerendert.behaelter.textContent ?? "";
      expect(text).toContain(D.leerTitel);
      expect(text).toContain(D.leerHinweis);

      // **Der Umschalter bleibt bedienbar** — er ist der einzige Weg
      // herauszufinden, ob es am Zeitraum liegt.
      const schalter = [...gerendert.behaelter.querySelectorAll("button")].filter(
        (knopf) => knopf.textContent === Z["30T"],
      );
      expect(schalter).toHaveLength(1);
      expect(schalter[0]?.hasAttribute("disabled")).toBe(false);
    } finally {
      await gerendert.abbauen();
    }
  });

  /**
   * **Die Gegenprobe, und sie ist der eigentliche Inhalt dieses Falls:** Die
   * Zeile „nicht zugeordnet" sagt etwas über den *Katalog* — über einen
   * Mandanten ohne Nachrichten sagt sie nichts. Der Endpunkt liefert sie auch im
   * Leerzustand mit; die Ansicht zeigt sie dort **nicht**.
   */
  it("zeigt weder Kacheln noch die Nullzeile der Verteilung", async () => {
    stelleAntwort(antwort({ leer: true }));
    const gerendert = await rendereAnsicht();

    try {
      const text = gerendert.behaelter.textContent ?? "";

      expect(text).not.toContain(D.verteilung.nichtZugeordnet);
      expect(text).not.toContain(D.kacheln.nachrichtenHinweis);
      expect(text).not.toContain(D.verlauf.titel);
      // Keine Kachel mit Nullen, also auch keine Zahl aus der Antwort.
      expect(text).not.toContain("9.950");
      // Der Stand steht trotzdem: Er sagt, wie alt die Auskunft ist, und das
      // gilt auch dann, wenn die Auskunft „nichts" lautet.
      expect(text).toContain(D.stand.artVOLL);
    } finally {
      await gerendert.abbauen();
    }
  });
});

/**
 * **Ein Aufruf, eine Antwort.** Kein Block lädt nach — auch die Verteilung beim
 * Umschalten der Sicht nicht.
 */
describe("Die Zahl der Anfragen", () => {
  it("stellt beim Laden genau eine Anfrage, und die geht an /api/dashboard", async () => {
    stelleAntwort(antwort());
    const gerendert = await rendereAnsicht();

    try {
      expect(anfragen).toHaveLength(1);
      expect(anfragen[0]).toBe("/api/dashboard");
    } finally {
      await gerendert.abbauen();
    }
  });

  /**
   * **Ohne Klick steht nichts in der Adresse** (Entscheidung E‑n) — der Endpunkt
   * wählt selbst, und das kostet ihn die Belegungsprobe. Mit Wahl steht sie
   * darin, und der Aufruf ist der billigere.
   */
  it("schickt den gewählten Zeitraum mit und die Vorgabe der Verteilung nicht", async () => {
    stelleAntwort(antwort({ zeitraum: "30T" }));
    const gerendert = await rendereAnsicht("?zeitraum=30T");

    try {
      expect(anfragen).toEqual(["/api/dashboard?zeitraum=30T"]);
    } finally {
      await gerendert.abbauen();
    }
  });

  /**
   * **Ein Sichtwechsel lädt nicht nach, sondern ruft neu.** Es ist dieselbe
   * Adresse mit anderem Parameter und kein zweiter Endpunkt: Nach dem Klick
   * stehen genau zwei Anfragen da, beide an `/api/dashboard`, und die zweite
   * trägt `verteilung=RICHTUNG`.
   */
  it("ruft beim Sichtwechsel dieselbe Adresse mit anderem Parameter", async () => {
    stelleAntwort(antwort());
    const gerendert = await rendereAnsicht();

    try {
      const richtung = [...gerendert.behaelter.querySelectorAll("button")].find(
        (knopf) => knopf.textContent === D.verteilung.richtung,
      );
      expect(richtung).toBeDefined();

      await act(async () => {
        richtung?.dispatchEvent(new MouseEvent("click", { bubbles: true }));
      });
      // Der Zustandswechsel läuft über `nuqs`; die Anfrage folgt im nächsten Zug.
      await warteAufAntwort(gerendert.behaelter);

      expect(anfragen).toEqual(["/api/dashboard", "/api/dashboard?verteilung=RICHTUNG"]);
      expect(anfragen.every((adresse) => adresse.startsWith("/api/dashboard"))).toBe(true);
    } finally {
      await gerendert.abbauen();
    }
  });
});

/**
 * **„Zuletzt aufgefallen" — eine Zeile je Prozess** (Entscheidung **E‑90**).
 *
 * Der Block steht hier und nicht in `tests/dashboard.test.ts`, weil seine
 * Aussagen **Reihenfolge** und **Zusammenfassung** betreffen: dass aus zehn
 * gleichen Zeilen zwei verschiedene werden, dass die jüngste oben steht und dass
 * die Zahl daneben zu dem Verweis passt, der darauf zeigt. Nichts davon ist ohne
 * Baum zu treffen.
 */
describe("Zuletzt aufgefallen", () => {
  const ZEILEN = [
    {
      processId: "40105_BMW_GI_EDIFACT",
      processName: "BMW Global Invoice (EDIFACT)",
      anzahl: 1,
      zuletzt: "2025-12-29T11:48:16Z",
      kategorie: "FEHLER" as const,
    },
    {
      processId: "40090_BMW_LAB_VDA",
      processName: "BMW LAB (VDA)",
      anzahl: 49,
      zuletzt: "2025-12-30T03:09:47Z",
      kategorie: "FEHLER" as const,
    },
  ];

  /**
   * **Der Fall, um dessentwillen der Block umgebaut worden ist.** Vorher standen
   * hier zehn Zeilen mit demselben Zeitstempel und demselben Ablauf; jetzt sagt
   * jede Zeile etwas Eigenes. Geprüft wird beides: dass es **so viele Zeilen wie
   * Prozesse** sind, und dass jede ihre **eigene Zahl** trägt.
   */
  it("zeigt eine Zeile je Prozess, mit Anzahl und jüngstem Zeitpunkt", async () => {
    const gerendert = await rendere(<AufgefallenBlock zeilen={ZEILEN} fenster={FENSTER} />);

    try {
      const eintraege = [...gerendert.behaelter.querySelectorAll("li")];
      expect(eintraege).toHaveLength(2);

      const namen = eintraege.map((eintrag) => eintrag.querySelector("a")?.textContent);
      expect(namen).toEqual(["BMW Global Invoice (EDIFACT)", "BMW LAB (VDA)"]);

      // Die Zahlen stehen je Zeile und nicht als Summe darunter.
      const zahlen = eintraege.map((eintrag) => eintrag.querySelector("[aria-label]")?.textContent);
      expect(zahlen).toEqual(["1", "49"]);

      // Und sie tragen ihr Wort für das Vorleseprogramm — Einzahl und Mehrzahl
      // getrennt, weil die englische Fassung sie unterscheidet.
      const beschriftungen = eintraege.map((eintrag) =>
        eintrag.querySelector("[aria-label]")?.getAttribute("aria-label"),
      );
      expect(beschriftungen).toEqual([D.aufgefallen.anzahlEins, "49 Nachrichten"]);
    } finally {
      await gerendert.abbauen();
    }
  });

  /**
   * **Das Zeichen je Zeile** (E‑91). Es steht wieder da, nachdem die Plakette am
   * 03.09.2026 gefallen war — als **Zeichen**, nicht als Wort: Ohne es sagt im
   * Bild nichts mehr, dass es sich um Fehler handelt („Zuletzt aufgefallen" ist
   * keine Kategorie).
   *
   * Geprüft ist beides, was daran leicht auseinanderfällt: dass **jede** Zeile
   * eines trägt, und dass das **Wort** nur im Vorlese-Markup steht und nicht im
   * Bild. Ohne die zweite Zusicherung wäre die naheliegende Verschlimmbesserung
   * — das Wort danebenzuschreiben — nicht zu bemerken.
   */
  it("trägt je Zeile das Zeichen der Kategorie, und das Wort nur für Vorleser", async () => {
    const gerendert = await rendere(<AufgefallenBlock zeilen={ZEILEN} fenster={FENSTER} />);

    try {
      const eintraege = [...gerendert.behaelter.querySelectorAll("li")];

      for (const eintrag of eintraege) {
        const traeger = eintrag.querySelector(`[title="${D.aufgefallen.kategorie.FEHLER}"]`);
        expect(traeger, "Jede Zeile trägt das Zeichen ihrer Kategorie").not.toBeNull();
        expect(traeger?.querySelector("svg")).not.toBeNull();

        // Die Farbe kommt aus `lib/status-farbe.ts` und steht nicht als Wert in
        // der Komponente — geprüft an der Klasse, nicht am Pixel.
        expect(traeger?.className).toContain("text-status-fehler");

        // Das Wort steht im Markup, aber nicht im Bild: `sr-only` blendet es aus.
        const wort = traeger?.querySelector(".sr-only");
        expect(wort?.textContent).toBe(D.aufgefallen.kategorie.FEHLER);
      }

      // Die Gegenprobe: sichtbar steht es nirgends. Der sichtbare Text der Zeile
      // sind Zeitpunkt, Name und Zahl — sonst nichts.
      const sichtbar = eintraege.map((eintrag) => {
        const kopie = eintrag.cloneNode(true) as HTMLElement;
        kopie.querySelectorAll(".sr-only").forEach((knoten) => knoten.remove());
        return kopie.textContent ?? "";
      });
      expect(sichtbar.some((text) => text.includes(D.aufgefallen.kategorie.FEHLER))).toBe(false);
    } finally {
      await gerendert.abbauen();
    }
  });

  /**
   * **Der Verweis führt in die Liste und nicht ins Detail** — und zwar auf genau
   * die Menge, die die Zahl daneben nennt: dieser Prozess, `FEHLER`, dasselbe
   * Fenster. Eine einzelne Nachricht herauszugreifen wäre eine Behauptung, die
   * die Zeile nicht macht.
   */
  it("verweist auf die Liste, gefiltert auf diesen Prozess und auf Fehler", async () => {
    const gerendert = await rendere(<AufgefallenBlock zeilen={ZEILEN} fenster={FENSTER} />);

    try {
      const ziele = [...gerendert.behaelter.querySelectorAll("a")].map((verweis) =>
        verweis.getAttribute("href"),
      );
      for (const ziel of ziele) {
        expect(ziel).toContain("status=FEHLER");
        expect(ziel).toContain(`von=${encodeURIComponent(FENSTER.von)}`);
        expect(ziel).toContain(`bis=${encodeURIComponent(FENSTER.bis)}`);
      }
      expect(ziele[0]).toContain("prozess=40105_BMW_GI_EDIFACT");
      expect(ziele[1]).toContain("prozess=40090_BMW_LAB_VDA");

      // Die Gegenprobe: kein Verweis zeigt mehr auf eine einzelne Nachricht.
      expect(ziele.some((ziel) => ziel?.startsWith("/nachrichten/"))).toBe(false);
    } finally {
      await gerendert.abbauen();
    }
  });

  /**
   * **Ohne Namen steht die Kennung da und kein Ersatztext** (Regel Q4). Das
   * Backend liefert `processName` als `null`, wenn die Spalte im Altsystem leer
   * ist — geraten wird hier nichts, auch nicht aus der Kennung.
   */
  it("fällt ohne Klarnamen auf die Prozesskennung zurück", async () => {
    const gerendert = await rendere(
      <AufgefallenBlock zeilen={[{ ...ZEILEN[1], processName: null }]} fenster={FENSTER} />,
    );

    try {
      expect(gerendert.behaelter.querySelector("a")?.textContent).toBe("40090_BMW_LAB_VDA");
    } finally {
      await gerendert.abbauen();
    }
  });

  it("zeigt im Leerzustand einen Satz und keine leere Liste", async () => {
    const gerendert = await rendere(<AufgefallenBlock zeilen={[]} fenster={FENSTER} />);

    try {
      expect(gerendert.behaelter.querySelector("ul")).toBeNull();
      expect(gerendert.behaelter.textContent).toContain(D.aufgefallen.leer);
    } finally {
      await gerendert.abbauen();
    }
  });
});
