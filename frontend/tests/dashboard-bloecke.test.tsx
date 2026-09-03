// @vitest-environment jsdom

import { act } from "react";
import { NuqsTestingAdapter } from "nuqs/adapters/testing";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { texteFuer } from "@/i18n";
import type { Dashboard, Verteilung } from "@/features/dashboard/api";
import { DashboardAnsicht } from "@/features/dashboard/components/dashboard-ansicht";
import { Kacheln } from "@/features/dashboard/components/kacheln";
import { VerteilungBlock } from "@/features/dashboard/components/verteilung-block";

import { rendere } from "./hilfe/rendern";

/**
 * **Die Fälle des Dashboards, für die ein gerenderter Baum die einzige Prüfung
 * ist.**
 *
 * Alles Übrige steht als reine Funktion in `tests/dashboard.test.ts` — die
 * Zusammenfassung auf vier Reihen, der URL-Zustand, die beiden Adressen, die
 * Beschriftungen. Hier stehen vier Fälle, und alle vier sind Aussagen über
 * **Abwesenheit** oder über **Reihenfolge**; beide sind ohne Baum nicht zu
 * treffen (`docs/frontend-grundlagen.md` §9).
 *
 * | Fall | Warum genau dieser |
 * |---|---|
 * | „nicht ermittelbar" | **Keine `0` und kein Verweis** im Baum, und die übrigen Blöcke stehen. Eine reine Funktion sagt nichts darüber, was *nicht* gerendert wird — und die naheliegende Schreibweise `imFenster ?? 0` bestünde jede Prüfung an ihr |
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
      ueberfaellig: { imFenster: 1, insgesamt: 538, ermittelbar: true },
    },
    verteilung: VERTEILUNG_VOLL,
    zuletztAufgefallen: [],
    stand: { beendetAm: "2025-12-30T04:10:00Z", art: "VOLL" },
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

/**
 * **„Nicht ermittelbar" ist nicht `0`** (Entscheidung E‑q).
 *
 * Null hieße „es hängt nichts", und das ist in einem Überwachungswerkzeug die
 * schlimmste falsche Antwort.
 */
describe("Die Kachel Überfällig, wenn die Live-Abfrage gestorben ist", () => {
  it("zeigt keine Null und keinen Verweis, und die übrigen Kacheln stehen", async () => {
    const gerendert = await rendere(
      <Kacheln
        kacheln={{
          nachrichten: 9950,
          fehler: { anzahl: 50, arten: [] },
          ueberfaellig: { imFenster: null, insgesamt: null, ermittelbar: false },
        }}
        fenster={FENSTER}
      />,
    );

    try {
      const text = gerendert.behaelter.textContent ?? "";

      // Der Kacheltitel bleibt, und der Grund steht daneben.
      expect(text).toContain(D.kacheln.ueberfaellig);
      expect(text).toContain(D.kacheln.nichtErmittelbarHinweis);
      expect(text).toContain(D.kacheln.nichtErmittelbar);

      // **Kein Verweis in die Liste.** Der einzige Verweis der drei Kacheln ist
      // der der Fehlerkachel; einer auf `ueberfaellig=true` darf nicht dastehen.
      const ziele = [...gerendert.behaelter.querySelectorAll("a")].map((a) =>
        a.getAttribute("href"),
      );
      expect(ziele.some((ziel) => ziel?.includes("ueberfaellig"))).toBe(false);

      // Die übrigen Blöcke stehen normal — der Vertrag lässt genau diese zwei
      // Felder ausfallen und keinen ganzen Block.
      expect(text).toContain(D.kacheln.nachrichten);
      expect(ziele.some((ziel) => ziel?.includes("status=FEHLER"))).toBe(true);
    } finally {
      await gerendert.abbauen();
    }
  });

  /**
   * Die Gegenprobe. Ohne sie bewiese der Test oben nur, dass irgendetwas fehlt —
   * nicht, dass es **wegen** `ermittelbar: false` fehlt.
   */
  it("zeigt mit ermittelbar: true beide Zahlen und den Verweis", async () => {
    const gerendert = await rendere(
      <Kacheln
        kacheln={{
          nachrichten: 9950,
          fehler: { anzahl: 50, arten: [] },
          ueberfaellig: { imFenster: 1, insgesamt: 538, ermittelbar: true },
        }}
        fenster={FENSTER}
      />,
    );

    try {
      const text = gerendert.behaelter.textContent ?? "";
      expect(text).toContain("538");
      expect(text).not.toContain(D.kacheln.nichtErmittelbarHinweis);

      const ziele = [...gerendert.behaelter.querySelectorAll("a")].map((a) =>
        a.getAttribute("href"),
      );
      expect(ziele.some((ziel) => ziel?.includes("ueberfaellig=true"))).toBe(true);

      // **„Insgesamt" trägt trotzdem keinen Verweis.** Kein zweiter Verweis auf
      // die Überfälligkeitsform, und der Satz sagt, warum.
      expect(ziele.filter((ziel) => ziel?.includes("ueberfaellig")).length).toBe(1);
      expect(text).toContain(D.kacheln.insgesamtOhneVerweis);
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
