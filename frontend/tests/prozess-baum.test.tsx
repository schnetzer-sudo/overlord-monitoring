// @vitest-environment jsdom

import { act } from "react";
import { describe, expect, it } from "vitest";

import type { Partnerknoten, Prozessknoten, Richtungsknoten } from "@/features/nachrichten/api";
import { ProzessBaum } from "@/features/nachrichten/components/prozess-baum";
import { partnerSchluessel, prozessSchluessel } from "@/features/nachrichten/prozessbaum";

import { rendere } from "./hilfe/rendern";

/**
 * **Zehn Fälle, für die ein gerenderter Baum die einzige Prüfung ist.**
 *
 * Die Entscheidungen des Prozessbaums stehen in `tests/prozessbaum.test.ts` —
 * Schachtelung, Überspringen, Eingrenzung, Tastatur — und werden dort ohne DOM
 * geprüft. Was hier steht, ist genau das, was dort nicht belegbar wäre:
 *
 * 1. **Der `roving tabindex`.** „Genau ein Tabstopp, nicht 1.158“ ist eine
 *    Aussage über den Baum als ganzen und über ein Attribut, das keine reine
 *    Funktion erzeugt.
 * 2. **`aria-level` und `aria-expanded` am richtigen Knoten.** Die flache
 *    DOM-Form trägt die Tiefe allein in diesen Attributen; stünden sie falsch,
 *    wäre der Baum für ein Vorleseprogramm keiner. Die Zeilen selbst sind
 *    geprüft, ihre **Ausgabe** ist es nicht.
 * 3. **Die Abwesenheit einer Ebene** (E‑45): Bei einem Partner mit genau einer
 *    Richtung steht **kein** `treeitem` mit `aria-level="2"` und
 *    `aria-expanded`. Das ist eine Aussage über Abwesenheit — genau die Klasse,
 *    für die `tests/hilfe/rendern.tsx` da ist.
 * 4. **Der zugängliche Name kommt aus `aria-label`.** Er ersetzt bei über
 *    tausend Zeilen mehr als zweitausend `sr-only`-Spannen; dass er tatsächlich
 *    am Element steht und nicht im Text untergeht, zeigt nur der Baum.
 *
 * **Alle Prüfwerte sind erfunden** (Regel T2).
 */

function blatt(processId: string, name: string): Prozessknoten {
  return {
    processId,
    processName: name,
    nachrichten: 3,
    fehler: 0,
    letzteBewegung: "2025-12-29T22:00:00Z",
    zustand: "BEWEGT",
  };
}

function gruppe(richtung: string | null, prozesse: Prozessknoten[]): Richtungsknoten {
  return { richtung, anzahlProzesse: prozesse.length, nachrichten: 3, fehler: 0, prozesse };
}

function knoten(partner: string | null, richtungen: Richtungsknoten[]): Partnerknoten {
  return {
    partner,
    anzahlProzesse: richtungen.reduce((wert, g) => wert + g.anzahlProzesse, 0),
    nachrichten: 3,
    fehler: 0,
    richtungen,
  };
}

const MIT_EBENE = knoten("ACME", [
  gruppe("EINGEHEND", [blatt("p1", "ACME Bestellung")]),
  gruppe("AUSGEHEND", [blatt("p2", "ACME Lieferschein")]),
]);

/**
 * Ein Partner mit **einer, bekannten** Richtung — die Ebene steht trotzdem
 * (E‑58). Der Fall aus dem Bild des Auftraggebers: `ADIENT` mit einem Prozess.
 */
const EINE_RICHTUNG = knoten("BOSCH", [gruppe("EINGEHEND", [blatt("p3", "BOSCH Rechnung")])]);

/**
 * Ein Partner ohne kuratierte Richtung — bei `VOTG` alle 133 (M123). **Nur hier
 * fällt die Ebene weg**, und die Blätter bekommen keinen Ersatz.
 */
const OHNE_EBENE = knoten("VOTG", [gruppe(null, [blatt("p4", "Freier Prozess")])]);

async function baum(partner: Partnerknoten[], gewaehlt: string | null = null) {
  return rendere(
    <ProzessBaum
      partner={partner}
      stilleSchwelleMonate={3}
      gewaehlt={gewaehlt}
      springeZurAuswahl
      istOffen={() => true}
      aufUmschalten={() => undefined}
      aufAuswahl={() => undefined}
    />,
  );
}

function zeilen(behaelter: HTMLElement): HTMLElement[] {
  return [...behaelter.querySelectorAll('[role="treeitem"]')] as HTMLElement[];
}

describe("Der Prozessbaum im Baum", () => {
  it("hat genau einen Tabstopp, egal wie viele Zeilen offen stehen", async () => {
    const { behaelter, abbauen } = await baum([MIT_EBENE, OHNE_EBENE]);

    try {
      const alle = zeilen(behaelter);
      expect(alle.length).toBeGreaterThan(5);

      const tabbar = alle.filter((zeile) => zeile.getAttribute("tabindex") === "0");
      expect(tabbar).toHaveLength(1);
      // Alle anderen sind erreichbar, aber kein eigener Tabstopp.
      expect(alle.filter((zeile) => zeile.getAttribute("tabindex") === "-1")).toHaveLength(
        alle.length - 1,
      );
    } finally {
      await abbauen();
    }
  });

  it("legt den Tabstopp auf die gewählte Zeile und nicht auf die erste", async () => {
    // Wer über einen tiefen Link kommt und dann tabbt, landet an seiner Stelle
    // und nicht am Anfang des Baums.
    const { behaelter, abbauen } = await baum([MIT_EBENE, EINE_RICHTUNG], "p3");

    try {
      const tabbar = zeilen(behaelter).find((zeile) => zeile.getAttribute("tabindex") === "0");
      expect(tabbar?.getAttribute("aria-selected")).toBe("true");
      expect(tabbar?.textContent).toContain("BOSCH Rechnung");
    } finally {
      await abbauen();
    }
  });

  it("trägt die Tiefe in `aria-level` und das Aufklappen nur an Gruppen", async () => {
    const { behaelter, abbauen } = await baum([MIT_EBENE]);

    try {
      const alle = zeilen(behaelter);
      expect(alle.map((zeile) => zeile.getAttribute("aria-level"))).toEqual([
        "1",
        "2",
        "3",
        "2",
        "3",
      ]);

      // `aria-expanded` steht an Partner und Richtung, nie an einem Blatt: Ein
      // Blatt, das „zugeklappt“ meldet, verspricht Kinder, die es nicht gibt.
      const mitExpanded = alle.filter((zeile) => zeile.hasAttribute("aria-expanded"));
      expect(mitExpanded).toHaveLength(3);
      expect(mitExpanded.every((zeile) => zeile.getAttribute("aria-expanded") === "true")).toBe(
        true,
      );

      // Und die Auswahl steht an jeder Zeile — auswählbar ist nur ein Prozess,
      // eine Gruppe trägt deshalb `false` und nie `true`.
      expect(alle.every((zeile) => zeile.getAttribute("aria-selected") === "false")).toBe(true);
    } finally {
      await abbauen();
    }
  });

  it("macht bei nicht ermittelter Richtung **keine** zweite Ebene auf (E‑58)", async () => {
    const { behaelter, abbauen } = await baum([OHNE_EBENE]);

    try {
      const alle = zeilen(behaelter);

      // Partner auf Ebene 1, Blatt auf Ebene 2 — und dazwischen nichts.
      expect(alle.map((zeile) => zeile.getAttribute("aria-level"))).toEqual(["1", "2"]);
      // Genau ein aufklappbarer Knoten: der Partner. Der Richtungsknoten, den es
      // im Endpunkt gibt, erscheint hier nicht.
      expect(alle.filter((zeile) => zeile.hasAttribute("aria-expanded"))).toHaveLength(1);
      // **Und die Richtung wird nirgends nachgetragen** — weder sichtbar noch
      // im vorgelesenen Namen. „nicht ermittelt“ hätte hier nichts zu sagen.
      expect(alle[1].getAttribute("aria-label")).not.toContain("ermittelt");
    } finally {
      await abbauen();
    }
  });

  it("lässt die Ebene über einem einzigen Kind stehen, wenn die Richtung bekannt ist (E‑58)", async () => {
    // ⚠️ **Die Umkehrung vom 03.09.2026, und sie ist am Bild entschieden
    // worden:** Bei `ACOME` stand „Eingehend“ als Zeile, beim Nachbarn `ADIENT`
    // dasselbe Wort als Vorsatz in der Prozesszeile. Zwei Schreibweisen für
    // denselben Sachverhalt, direkt untereinander.
    const { behaelter, abbauen } = await baum([EINE_RICHTUNG]);

    try {
      const alle = zeilen(behaelter);

      expect(alle.map((zeile) => zeile.getAttribute("aria-level"))).toEqual(["1", "2", "3"]);
      expect(alle[1].getAttribute("aria-label")).toContain("Eingehend");
      // Das Blatt trägt sie **nicht** noch einmal.
      expect(alle[2].getAttribute("aria-label")).toContain("BOSCH Rechnung");
      expect(alle[2].getAttribute("aria-label")).not.toContain("Eingehend");
    } finally {
      await abbauen();
    }
  });

  it("beschriftet jede Zeile über `aria-label` statt über verborgene Spannen", async () => {
    const { behaelter, abbauen } = await baum([MIT_EBENE]);

    try {
      const alle = zeilen(behaelter);
      expect(alle.every((zeile) => (zeile.getAttribute("aria-label") ?? "").length > 0)).toBe(true);
      expect(alle[0].getAttribute("aria-label")).toContain("Nachrichten: 3");

      // **Keine `sr-only`-Spanne je Zahl.** Bei 1.158 Zeilen wären das über
      // zweitausend zusätzliche Knoten, jeder davon `position: absolute` — und
      // damit ein Kandidat für den Befund aus `frontend-grundlagen.md` §7.
      expect(behaelter.querySelectorAll('[role="treeitem"] .sr-only')).toHaveLength(0);
    } finally {
      await abbauen();
    }
  });

  it("gibt dem Baum selbst eine Rolle und einen Namen", async () => {
    const { behaelter, abbauen } = await baum([MIT_EBENE]);

    try {
      const wurzel = behaelter.querySelector('[role="tree"]');
      expect(wurzel).not.toBeNull();
      expect(wurzel?.getAttribute("aria-label")).toBeTruthy();
      // **`relative`**: `sr-only` ist `position: absolute`, und ein solches
      // Element ohne positionierten Vorfahren macht in einer langen Liste die
      // ganze Seite scrollbar (`docs/frontend-grundlagen.md` §7, Bedingung 3).
      expect(wurzel?.className.split(/\s+/)).toContain("relative");
      // Und **kein zweiter Scrollbereich** — Bedingung 2 derselben Stelle.
      expect(wurzel?.className).not.toContain("overflow-y");
      expect(wurzel?.className).not.toContain("h-dvh");
    } finally {
      await abbauen();
    }
  });

  it("hält an jeder Zeile die Zeilenhöhe, die am Finger zur Fläche wird", async () => {
    // **E‑54, 02.09.2026:** `min-h-bedienzeile` statt `min-h-beruehrung`. Das
    // Token trägt am Zeigergerät `--dichte-zeile` und fällt in
    // `@media (pointer: coarse)` auf `--dichte-beruehrung` zurück — die
    // Umschaltung steht in `globals.css` und nicht hier.
    //
    // **Geprüft wird die Klasse und nicht der Pixelwert**, und das ist kein
    // Notbehelf: `jsdom` rechnet kein Layout und wertet keine `@media`-Regel
    // aus. Was die Klasse in Pixeln ergibt, steht als M127 in
    // `docs/process-view.md` §24 — am laufenden System gemessen, in beiden
    // Zeigerarten.
    //
    // Die **Abwesenheit** steht mit im Test: Bliebe `min-h-beruehrung` an einer
    // Zeile stehen, griffe dort weiterhin der Boden von 44 px, und der
    // Dichteumschalter bewegte genau diese Zeile nicht.
    const { behaelter, abbauen } = await baum([MIT_EBENE, OHNE_EBENE]);

    try {
      const klassen = zeilen(behaelter).map((zeile) => zeile.className.split(/\s+/));
      expect(klassen.every((k) => k.includes("min-h-bedienzeile"))).toBe(true);
      expect(klassen.some((k) => k.includes("min-h-beruehrung"))).toBe(false);
    } finally {
      await abbauen();
    }
  });

  it("schreibt die Richtung nur an die Ebene und nirgends in eine Blattzeile (E‑58)", async () => {
    // **Die Verdrahtung, nicht die Regel.** Welche Ebene entsteht, steht als
    // reine Funktion in `prozessbaum.ts` und wird dort geprüft; dass an der
    // Blattzeile **weder ein Zeichen noch ein Wort** steht, zeigt nur der
    // gerenderte Baum.
    //
    // Die Zeile hat zwei Fassungen gehabt und keine gehalten: bis zum
    // 02.09.2026 ein Zeichen (`↙`, `↗`, gestrichelter Kreis), danach einen Tag
    // lang das Wort vor dem Namen. Gezählt werden hier die `svg` je Zeile —
    // Gruppen behalten ihr Aufklappzeichen (eins), Blätter tragen keines (null).
    const { behaelter, abbauen } = await baum([EINE_RICHTUNG, OHNE_EBENE]);

    try {
      const [bosch, richtung, blatt3, votg, blatt4] = zeilen(behaelter);

      expect(bosch.querySelectorAll("svg")).toHaveLength(1);
      expect(richtung.textContent).toContain("Eingehend");
      expect(richtung.querySelectorAll("svg")).toHaveLength(1);

      // Das Blatt darunter trägt den Namen und sonst nichts.
      expect(blatt3.textContent).toContain("BOSCH Rechnung");
      expect(blatt3.textContent).not.toContain("Eingehend");
      expect(blatt3.querySelectorAll("svg")).toHaveLength(0);

      // **Und wo die Ebene wegfällt, wird sie nicht ersetzt.**
      expect(votg.querySelectorAll("svg")).toHaveLength(1);
      expect(blatt4.textContent).toContain("Freier Prozess");
      expect(blatt4.textContent).not.toContain("ermittelt");
      expect(blatt4.querySelectorAll("svg")).toHaveLength(0);
    } finally {
      await abbauen();
    }
  });

  it("kennzeichnet „noch nie“ in der Zeile nicht mehr (E‑56)", async () => {
    // Beides zusammen, und das ist der Kern: **kein Wort und keine Dämpfung.**
    // Bliebe die Dämpfung allein stehen, wäre der Zustand nur über Helligkeit
    // ausgedrückt — der Fall, den `visuelles-konzept.md` §3 verbietet.
    //
    // `text-muted-foreground` steht im Baum an anderen Stellen (Zahlenblock);
    // geprüft wird deshalb die **Zeile selbst**.
    const nie = knoten("ACME", [
      gruppe(null, [{ ...blatt("p5", "Nie benutzt"), zustand: "NIE", letzteBewegung: null }]),
    ]);
    const { behaelter, abbauen } = await baum([nie]);

    try {
      const zeile = zeilen(behaelter)[1];
      expect(zeile.textContent).toContain("Nie benutzt");
      expect(zeile.textContent).not.toContain("noch nie");
      expect(zeile.className.split(/\s+/)).not.toContain("text-muted-foreground");
      // Und die Gegenprobe: „still“ trägt seine Marke unverändert weiter.
      expect(zeile.getAttribute("aria-label")).not.toContain("nie");
    } finally {
      await abbauen();
    }
  });

  it("lässt eine Modifiertaste durch — `Alt+←` gehört dem Browser", async () => {
    // ⚠️ Der Baum sah zuerst nur `ereignis.key` an und verschluckte damit
    // ausgerechnet die Rücknavigation, auf der sein eigener Verlaufsentwurf
    // beruht: `prozess` und `nachricht` legen mit `history: "push"` je einen
    // Eintrag an, und `Alt+←` ist der Weg zurück. Die reine Funktion
    // `tastenbefehl` sieht Modifier gar nicht — belegbar ist der Satz nur hier.
    const { behaelter, abbauen } = await baum([MIT_EBENE]);

    try {
      const zeile = zeilen(behaelter)[0];

      const mitAlt = new KeyboardEvent("keydown", {
        key: "ArrowLeft",
        altKey: true,
        bubbles: true,
        cancelable: true,
      });
      await act(async () => {
        zeile.dispatchEvent(mitAlt);
      });
      expect(mitAlt.defaultPrevented).toBe(false);

      // Gegenprobe: ohne Modifier nimmt der Baum die Taste sehr wohl an.
      const ohneAlt = new KeyboardEvent("keydown", {
        key: "ArrowLeft",
        bubbles: true,
        cancelable: true,
      });
      await act(async () => {
        zeile.dispatchEvent(ohneAlt);
      });
      expect(ohneAlt.defaultPrevented).toBe(true);
    } finally {
      await abbauen();
    }
  });

  it("bewegt den Fokus mit den Pfeiltasten — die Verdrahtung, nicht die Regel", async () => {
    // Die Regel steht in `tests/prozessbaum.test.ts`. Dass sie auch **ankommt**
    // — Tastendruck, `tastenbefehl`, `focus()`, roving `tabindex` —, zeigt nur
    // der Baum.
    const { behaelter, abbauen } = await baum([MIT_EBENE]);

    try {
      const alle = zeilen(behaelter);
      // `focus()` löst `onFocus` und damit einen Zustandswechsel aus — auch das
      // gehört in ein `act`, sonst meldet React es über `console.error` und der
      // Lauf schlägt fehl (`tests/setup/konsole.ts`).
      await act(async () => {
        alle[0].focus();
      });

      await act(async () => {
        alle[0].dispatchEvent(
          new KeyboardEvent("keydown", { key: "ArrowDown", bubbles: true, cancelable: true }),
        );
      });

      expect(document.activeElement).toBe(alle[1]);
      expect(alle[1].getAttribute("tabindex")).toBe("0");
      expect(alle[0].getAttribute("tabindex")).toBe("-1");
    } finally {
      await abbauen();
    }
  });

  it("benutzt für die Schlüssel den Wert und nicht die Position", async () => {
    // Die Gegenprobe zur reinen Funktion: Dass der Baum wirklich diese Schlüssel
    // verwendet, sieht man erst an den gerenderten Zeilen — und daran hängt, ob
    // ein aufgeklappter Partner eine Eingrenzung überlebt.
    expect(partnerSchluessel("ACME")).not.toBe(partnerSchluessel("BOSCH"));
    expect(prozessSchluessel("p1")).not.toBe(prozessSchluessel("p2"));

    const { behaelter, abbauen } = await baum([MIT_EBENE, OHNE_EBENE]);
    try {
      // Zwei Partner, zwei verschiedene Zeilen — und kein doppelter
      // React-Schlüssel (der meldete sich über `console.error` und ließe den
      // Lauf fehlschlagen, siehe `tests/setup/konsole.ts`).
      expect(
        zeilen(behaelter).filter((zeile) => zeile.getAttribute("aria-level") === "1"),
      ).toHaveLength(2);
    } finally {
      await abbauen();
    }
  });
});
