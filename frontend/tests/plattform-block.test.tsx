// @vitest-environment jsdom

import { describe, expect, it } from "vitest";

import { texteFuer } from "@/i18n";
import type { Ablagen, Dienstlampe, Plattform } from "@/features/dashboard/api";
import { PlattformKachel } from "@/features/dashboard/components/plattform-block";

import { rendere } from "./hilfe/rendern";

/**
 * **Die Fälle der Kachel *Plattform*, für die ein gerenderter Baum die einzige
 * Prüfung ist** (Schritt 10d Teil B, Nachbesserung vom 10.09.2026,
 * `docs/frontend-grundlagen.md` §9).
 *
 * Alles Übrige steht als reine Funktion in `tests/dashboard.test.ts`: die
 * beiden Farbzuordnungen samt ihrer Vollständigkeit, die vier unterscheidbaren
 * Zeichen, die Regel zum Rohwert, die Regel zur Zielfarbe, die fünf Sätze und
 * die beiden Auskunftstexte. **Hier stehen die Klassen, die eine reine Funktion
 * nicht trägt** — und alle sind Aussagen darüber, ob etwas im Baum **ankommt**:
 *
 * | Fall | Warum genau dieser |
 * |---|---|
 * | **`title` und `sr-only`** | Die Kachel trägt kein Wort im Bild. Die *Sätze* sind reine Funktionen und dort geprüft; belegt wird hier, dass sie **beide Wege** erreichen — ein `title` allein ließe ein Vorleseprogramm mit einem nackten Zeichen zurück, eine `sr-only`-Spanne allein die Maus |
 * | **die Farbe der Zielzeilen** | `traegtZielfarbe` ist eine reine Funktion; dass die Zeile die Rolle danach wirklich **trägt oder eben nicht**, ist eine Klasse am Element. `mitFarbe ? … : …` bestünde jede Prüfung an der Funktion |
 * | **leere `dienste`** | Wo nichts stünde, steht ein **Satz** (E‑135) — und die Ablagenzeile bleibt daneben stehen. Die naheliegende Schreibweise `dienste.map(…)` bestünde jede Prüfung an einer reinen Funktion |
 * | **was aus dem Bild gefallen ist** | Zeitpunkt, Alter, Grundsatz und Überschriftensatz stehen **nicht mehr** im Baum. Das ist eine Aussage über Abwesenheit und nirgends sonst prüfbar |
 *
 * **Antwortrümpfe gestellt, kein Netz, keine Datenbank.** Die Kachel bekommt
 * ihre Daten als Eigenschaft und lädt nichts nach.
 */

const TEXTE = texteFuer("de");
const P = TEXTE.dashboard.plattform;

/** Die Ablagen im Zustand der Dev-Zeile: abgeschaltet, ohne Ziel, ohne Stand. */
const ABGESCHALTET: Ablagen = {
  zustand: "UNGEKLAERT",
  grund: "ABGESCHALTET",
  ziele: [],
  geprueftAm: null,
  alterSekunden: null,
};

/** Ein frisch geprüftes, erreichbares Ziel — der Fall mit eingeschalteter Prüfung. */
const ERREICHBAR: Ablagen = {
  zustand: "ERREICHBAR",
  grund: null,
  ziele: [{ serviceId: "FILESTOREPROD10", zustand: "ERREICHBAR" }],
  geprueftAm: "2025-12-30T03:09:49Z",
  alterSekunden: 0,
};

/** Derselbe Befund, aber der Stand ist älter als zwei Takte (E‑125, E‑133). */
const VERALTET: Ablagen = {
  zustand: "UNGEKLAERT",
  grund: "STAND_VERALTET",
  ziele: [
    { serviceId: "FILESTOREPROD10", zustand: "ERREICHBAR" },
    { serviceId: "FILESTOREPROD11", zustand: "NICHT_ERREICHBAR" },
  ],
  geprueftAm: "2025-12-30T02:00:00Z",
  alterSekunden: 4_189,
};

function lampe(ueberschreibung: Partial<Dienstlampe> = {}): Dienstlampe {
  return {
    serviceId: "MPSERVICEPROD03",
    zustand: "ZEITUEBERSCHRITTEN",
    rohwert: "ERROR_TIMEOUT",
    stand: "2025-12-30T03:09:41Z",
    alterSekunden: 6,
    ...ueberschreibung,
  };
}

function plattform(ueberschreibung: Partial<Plattform> = {}): Plattform {
  return { dienste: [lampe()], ablagen: ABGESCHALTET, ...ueberschreibung };
}

/** Die Zeilen der Kachel, in der Reihenfolge des Baums. */
function zeilen(behaelter: HTMLElement): HTMLLIElement[] {
  return [...behaelter.querySelectorAll("li")];
}

/** Der `title`-Träger einer Zeile — die Spanne um das Zeichen (Bauform E‑91). */
function auskunft(zeile: HTMLLIElement): { title: string | null; vorleser: string | undefined } {
  const traeger = zeile.querySelector("[title]");
  return {
    title: traeger?.getAttribute("title") ?? null,
    vorleser: traeger?.querySelector(".sr-only")?.textContent ?? undefined,
  };
}

describe("Das Wort steht im `title` und für Vorleser, nicht im Bild (E‑91)", () => {
  /**
   * **Beide Wege, und zwar derselbe Satz.** Ein `title` allein ließe ein
   * Vorleseprogramm mit einem `aria-hidden`-Zeichen zurück — dort stünde dann
   * nur noch die Kennung, und der Zustand wäre **gar nicht** ausgedrückt. Eine
   * `sr-only`-Spanne allein ließe die Maus ohne Auskunft.
   */
  it("legt Wort und Vorlesetext auf dieselbe Zeile", async () => {
    const { behaelter, abbauen } = await rendere(<PlattformKachel plattform={plattform()} />);
    try {
      const erste = auskunft(zeilen(behaelter)[0]);
      expect(erste.title).toBe(P.dienst.ZEITUEBERSCHRITTEN);
      expect(erste.vorleser).toBe(P.dienst.ZEITUEBERSCHRITTEN);

      // **Und im Bild steht es nicht.** `sr-only` ist die einzige Stelle, an
      // der das Wort als Text vorkommt; sichtbar trägt die Zeile die Kennung.
      expect(behaelter.textContent).toContain("MPSERVICEPROD03");
    } finally {
      await abbauen();
    }
  });

  /**
   * **Der Rohwert kommt bei jeder Lampe mit** (E‑118) und wird nur an der
   * ungeklärten genannt (E‑130). Die *Regel* ist `zeigtRohwert` und in
   * `tests/dashboard.test.ts` geprüft; belegt wird hier, dass jemand sie
   * abfragt — `{dienst.rohwert}` ohne Bedingung bestünde jede Prüfung an der
   * Funktion.
   */
  it("verschweigt den Rohwert an einer eingeordneten Zeile", async () => {
    const { behaelter, abbauen } = await rendere(<PlattformKachel plattform={plattform()} />);
    try {
      expect(behaelter.textContent).not.toContain("ERROR_TIMEOUT");
      expect(auskunft(zeilen(behaelter)[0]).title).not.toContain("ERROR_TIMEOUT");
    } finally {
      await abbauen();
    }
  });

  /** **An der ungeklärten Zeile ist er die ganze Auskunft** (Regel Q4). */
  it("nennt an der ungeklärten Zeile Wort und Rohwert", async () => {
    const { behaelter, abbauen } = await rendere(
      <PlattformKachel
        plattform={plattform({ dienste: [lampe({ zustand: "UNGEKLAERT", rohwert: "PAUSED" })] })}
      />,
    );
    try {
      const erste = auskunft(zeilen(behaelter)[0]);
      expect(erste.title).toContain(P.dienst.UNGEKLAERT);
      expect(erste.title).toContain("PAUSED");
      expect(erste.vorleser).toBe(erste.title);
    } finally {
      await abbauen();
    }
  });
});

describe("Eine Zielzeile trägt ihre Farbe nur, solange der Stand gilt (E‑133)", () => {
  /**
   * **Bei gültigem Stand ist die Zielzeile die Auskunft der Kachel** — sie
   * trägt die Rolle, und das Zeichen daneben trägt ihre Form.
   */
  it("färbt das Zeichen, wenn die Prüfung frisch ist", async () => {
    const { behaelter, abbauen } = await rendere(
      <PlattformKachel plattform={plattform({ ablagen: ERREICHBAR })} />,
    );
    try {
      const ziel = zeilen(behaelter).at(-1)!;
      expect(ziel.textContent).toContain("FILESTOREPROD10");
      expect(ziel.querySelector("[title]")?.className).toContain("text-status-abgeschlossen");
    } finally {
      await abbauen();
    }
  });

  /**
   * **Grün überlebt seinen Beleg auch in der Anzeige nicht.** Das Backend nimmt
   * der Kachel ihre Aussage, sobald der Stand älter ist als zwei Takte (E‑125);
   * stünde die Zeile darunter weiterhin grün, wäre dieselbe Aussage über
   * denselben Umweg wieder im Bild.
   *
   * **Genommen wird ihr nur die Farbe, nicht das Wort:** Der `title` nennt
   * weiterhin, was zuletzt festgestellt wurde — sonst wäre der Zustand nur noch
   * über Helligkeit ausgedrückt, und genau das schließt
   * `docs/visuelles-konzept.md` §3 aus.
   */
  it("nimmt beiden Zielzeilen die Rolle bei veraltetem Stand", async () => {
    const { behaelter, abbauen } = await rendere(
      <PlattformKachel plattform={plattform({ ablagen: VERALTET })} />,
    );
    try {
      const ziele = zeilen(behaelter).slice(-2);
      // Der Text einer Zeile ist das Wort für Vorleser und die Kennung — das
      // Zeichen daneben ist `aria-hidden` und trägt keinen Text.
      expect(ziele.map((zeile) => zeile.textContent)).toEqual([
        `${P.ablage.ERREICHBAR}FILESTOREPROD10`,
        `${P.ablage.NICHT_ERREICHBAR}FILESTOREPROD11`,
      ]);

      for (const zeile of ziele) {
        expect(zeile.querySelector("[title]")?.className).not.toContain("text-status-");
      }

      expect(auskunft(ziele[0]).title).toBe(P.ablage.ERREICHBAR);
      expect(auskunft(ziele[1]).title).toBe(P.ablage.NICHT_ERREICHBAR);
    } finally {
      await abbauen();
    }
  });

  /**
   * **Ohne Ziel steht die Sammelzeile da**, mit dem Zeichen für ungeklärt und
   * dem Grund im `title`. Ohne sie verschwände die Ablagenprüfung spurlos aus
   * der Kachel — Abwesenheit ist der schwächste Kanal, den eine Auskunft haben
   * kann.
   */
  it("setzt ohne Ziel die Sammelzeile mit dem Grund", async () => {
    const { behaelter, abbauen } = await rendere(<PlattformKachel plattform={plattform()} />);
    try {
      const letzte = zeilen(behaelter).at(-1)!;
      expect(letzte.textContent).toContain(P.ablagen);
      expect(auskunft(letzte).title).toContain(P.grund.ABGESCHALTET);
      expect(auskunft(letzte).vorleser).toContain(P.grund.ABGESCHALTET);
      expect(letzte.querySelector("[title]")?.className).toContain("text-status-ungeklaert");
    } finally {
      await abbauen();
    }
  });
});

describe("Leere `dienste` zeigen einen Satz und keine leere Stelle (E‑135)", () => {
  /**
   * **Abwesenheit ist der schwächste Kanal, den eine Auskunft haben kann** —
   * dieselbe Begründung wie bei E‑74 und E‑81. Eine Liste ohne Zeilen sähe aus
   * wie ein Fehler im Bau; hier steht, was sie heißt.
   */
  it("schreibt den Satz, wo keine Zeile steht", async () => {
    const { behaelter, abbauen } = await rendere(
      <PlattformKachel plattform={plattform({ dienste: [] })} />,
    );
    try {
      expect(behaelter.textContent).toContain(P.dienstLeer);

      // **Und die Ablagen stehen trotzdem** (E‑125, E‑135): Die Kachel zieht
      // sich nicht auf die halbe Auskunft zusammen, nur weil die andere Hälfte
      // leer ist.
      expect(zeilen(behaelter)).toHaveLength(1);
      expect(zeilen(behaelter)[0].textContent).toContain(P.ablagen);
    } finally {
      await abbauen();
    }
  });

  /** Die Gegenprobe: Mit Diensten steht der Satz **nicht** da. */
  it("schweigt, sobald es Dienste gibt", async () => {
    const { behaelter, abbauen } = await rendere(<PlattformKachel plattform={plattform()} />);
    try {
      expect(behaelter.textContent).not.toContain(P.dienstLeer);
      expect(zeilen(behaelter)).toHaveLength(2);
    } finally {
      await abbauen();
    }
  });
});

describe("Was am 10.09.2026 aus dem Bild gefallen ist, steht nicht mehr im Baum", () => {
  /**
   * **Die Antwort trägt `stand`, `alterSekunden` und `geprueftAm` weiter** — die
   * Oberfläche liest sie nicht mehr. Das ist der Kern der Nachbesserung: Der
   * Block war zu groß, weil jede Zeile drei Angaben trug, von denen keine die
   * Frage *„steht die Anlage"* beantwortet.
   *
   * **Geprüft wird über Zeichen im Text und nicht über Textbausteine**, denn
   * die Bausteine sind mit den Zeilen aus den Sprachdateien verschwunden — ein
   * Vergleich gegen sie wäre ein Vergleich gegen nichts.
   */
  it("nennt weder Zeitpunkt noch Alter", async () => {
    const { behaelter, abbauen } = await rendere(
      <PlattformKachel plattform={plattform({ ablagen: ERREICHBAR })} />,
    );
    try {
      // **Der ganze Text der Kachel, Zeichen für Zeichen** — kein Zeitpunkt,
      // kein Alter, kein Prüfzeitpunkt, kein Satz. Was hier steht, ist der
      // Kopf, je Zeile das Wort für Vorleser und die Kennung, sonst nichts.
      expect(behaelter.textContent).toBe(
        [
          P.titel,
          P.dienst.ZEITUEBERSCHRITTEN,
          "MPSERVICEPROD03",
          P.ablage.ERREICHBAR,
          "FILESTOREPROD10",
        ].join(""),
      );
    } finally {
      await abbauen();
    }
  });

  /**
   * **Kein Satz unter der Überschrift und kein sichtbarer Grund.** Der
   * Überschriftensatz aus E‑136 ist mit dem Block gefallen; der Grund aus
   * E‑134 steht seither im `title` — und dort ist er in den Fällen oben
   * belegt.
   */
  it("schreibt den Grund nicht mehr als Satz ins Bild", async () => {
    const { behaelter, abbauen } = await rendere(<PlattformKachel plattform={plattform()} />);
    try {
      expect(behaelter.textContent).toContain(P.titel);

      const sichtbar = [...behaelter.querySelectorAll("p")].map((p) => p.textContent).join(" ");
      expect(sichtbar).not.toContain(P.grund.ABGESCHALTET);
    } finally {
      await abbauen();
    }
  });
});
