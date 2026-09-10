// @vitest-environment jsdom

import { act, useEffect, useRef, useState } from "react";
import { describe, expect, it } from "vitest";

import { useBeginntOben, useInSicht, useZuletztGeschlossen } from "@/lib/in-sicht-bringen";

import { rendere } from "./hilfe/rendern";

/**
 * **Elf Fälle, für die ein gerenderter Baum die einzige Prüfung ist** — die
 * Haken aus E‑114 und E‑115 (`docs/frontend-grundlagen.md` §7, vierte
 * Bedingung; `docs/process-view.md` §46 und §47).
 *
 * Ein Haken ist ohne Komponente nicht aufrufbar, und was er tut, ist eine
 * Änderung am DOM: der Scrollstand **eines** Kastens. `jsdom` rechnet kein
 * Layout — alle Rechtecke sind null, `scrollTop` bleibt 0 —, deshalb werden die
 * Maße gestellt ({@link stelleKasten}) und der Scrollstand als eigene
 * Eigenschaft daruntergelegt. Geprüft wird damit die Frage, die E‑115 stellt:
 * **welcher Kasten sich bewegt und um wie viel** — und welcher nicht.
 *
 * **Was diese Tests nicht zeigen:** ob am Ende tatsächlich etwas ins Bild
 * kommt, und ob überhaupt etwas klebt. Das entscheidet allein der Browser, und
 * dort ist es gemessen (M172, M173).
 */

/** Die Griffe von außen — gesetzt in einem Effekt, nicht beim Rendern. */
const steuerung: {
  setze: (schluessel: string | null) => void;
  rendereNeu: () => void;
} = {
  setze: () => undefined,
  rendereNeu: () => undefined,
};

type Bauform =
  /** Das Ziel liegt unmittelbar im Kasten — die Lage unterhalb der Umbruchpunkte. */
  | "im-kasten"
  /** Das Ziel liegt in einer Spalte, die für sich scrollt, aber nicht klebt. */
  | "in-spalte"
  /** Das Ziel **ist** die klebende Spalte und scrollt für sich — die Panelhülle. */
  | "ist-klebende-spalte"
  /** Das Ziel liegt in einem klebenden Rahmen und **enthält** den Scrollkasten — die rechte Spalte mit ihrer Liste. */
  | "in-klebendem-rahmen";

function Ziel({
  schluessel,
  bauform,
  oben,
}: {
  schluessel: string | null;
  bauform: Bauform;
  oben: boolean;
}) {
  const ref = useRef<HTMLDivElement>(null);
  // Beide Haken stehen unbedingt da; aktiv ist der, dessen Schlüssel gesetzt ist.
  useInSicht(ref, oben ? null : schluessel);
  useBeginntOben(ref, oben ? schluessel : null);

  const ziel =
    bauform === "in-klebendem-rahmen" ? (
      <div ref={ref} data-ziel="">
        <div data-liste="" style={{ overflowY: "auto" }} />
      </div>
    ) : (
      <div
        ref={ref}
        data-ziel=""
        style={bauform === "ist-klebende-spalte" ? { position: "sticky", overflowY: "auto" } : {}}
      />
    );

  return (
    <div data-kasten="" style={{ overflowY: "auto" }}>
      {bauform === "in-spalte" || bauform === "in-klebendem-rahmen" ? (
        <div
          data-spalte=""
          style={bauform === "in-klebendem-rahmen" ? { position: "sticky" } : { overflowY: "auto" }}
        >
          {ziel}
        </div>
      ) : (
        ziel
      )}
    </div>
  );
}

function Huelle({
  start,
  bauform = "im-kasten",
  oben = false,
}: {
  start: string | null;
  bauform?: Bauform;
  oben?: boolean;
}) {
  const [schluessel, setSchluessel] = useState(start);
  const [, setDurchlauf] = useState(0);
  useEffect(() => {
    steuerung.setze = setSchluessel;
    steuerung.rendereNeu = () => setDurchlauf((n) => n + 1);
  }, []);
  return <Ziel schluessel={schluessel} bauform={bauform} oben={oben} />;
}

function rechteck(top: number, hoehe: number): DOMRect {
  return {
    top,
    bottom: top + hoehe,
    height: hoehe,
    left: 0,
    right: 0,
    width: 0,
    x: 0,
    y: top,
    toJSON: () => undefined,
  } as DOMRect;
}

/**
 * Stellt einem Element die Maße eines Sichtfensters und legt einen echten
 * Scrollstand darunter. `jsdom` gibt für `scrollTop` immer 0 zurück und nimmt
 * keinen Wert an — ohne diese Eigenschaft wäre die Frage „welcher Kasten hat
 * sich bewegt" nicht zu stellen.
 */
function stelleKasten(
  el: HTMLElement,
  { top = 0, hoehe = 400, inhalt = 5000, stand = 0 } = {},
): { stand: () => number } {
  let scrollstand = stand;
  el.getBoundingClientRect = () => rechteck(top, hoehe);
  Object.defineProperty(el, "scrollHeight", { value: inhalt, configurable: true });
  Object.defineProperty(el, "clientHeight", { value: hoehe, configurable: true });
  Object.defineProperty(el, "scrollTop", {
    configurable: true,
    get: () => scrollstand,
    set: (wert: number) => {
      scrollstand = wert;
    },
  });
  return { stand: () => scrollstand };
}

function teile(behaelter: HTMLElement) {
  const hole = (wahl: string) => behaelter.querySelector(wahl) as HTMLElement;
  return {
    kasten: hole("[data-kasten]"),
    spalte: hole("[data-spalte]"),
    ziel: hole("[data-ziel]"),
    liste: hole("[data-liste]"),
  };
}

describe("Der Haken useInSicht — E‑114, und er bewegt genau einen Kasten", () => {
  it("holt das Ziel beim ersten Rendern mit Schlüssel ins Bild — den kürzeren Weg, wenn es hineinpasst", async () => {
    const { behaelter, abbauen } = await rendere(<Huelle start={null} />);
    try {
      const { kasten, ziel } = teile(behaelter);
      const k = stelleKasten(kasten, { stand: 500 });
      ziel.getBoundingClientRect = () => rechteck(-300, 100);
      await act(async () => steuerung.setze("a"));
      // Oberkante 300 px über dem Rand, also 300 px zurück — nicht weiter.
      expect(k.stand()).toBe(200);
    } finally {
      await abbauen();
    }
  });

  it("bewegt nichts, solange der Schlüssel derselbe bleibt", async () => {
    const { behaelter, abbauen } = await rendere(<Huelle start="a" />);
    try {
      const { kasten, ziel } = teile(behaelter);
      const k = stelleKasten(kasten, { stand: 500 });
      ziel.getBoundingClientRect = () => rechteck(-300, 100);
      await act(async () => steuerung.rendereNeu());
      await act(async () => steuerung.setze("a"));
      expect(k.stand()).toBe(500);
    } finally {
      await abbauen();
    }
  });

  it("bewegt bei jedem Wechsel des Schlüssels erneut", async () => {
    const { behaelter, abbauen } = await rendere(<Huelle start="a" />);
    try {
      const { kasten, ziel } = teile(behaelter);
      const k = stelleKasten(kasten, { stand: 500 });
      ziel.getBoundingClientRect = () => rechteck(-300, 100);
      await act(async () => steuerung.setze("b"));
      expect(k.stand()).toBe(200);
      // Das Ziel liegt jetzt wieder oberhalb — der zweite Wechsel wirkt erneut.
      ziel.getBoundingClientRect = () => rechteck(-150, 100);
      await act(async () => steuerung.setze("c"));
      expect(k.stand()).toBe(50);
    } finally {
      await abbauen();
    }
  });

  it("tut ohne Schlüssel nichts — auch nicht, wenn er auf null zurückfällt", async () => {
    const { behaelter, abbauen } = await rendere(<Huelle start={null} />);
    try {
      const { kasten, ziel } = teile(behaelter);
      const k = stelleKasten(kasten, { stand: 500 });
      ziel.getBoundingClientRect = () => rechteck(-300, 100);
      expect(k.stand()).toBe(500);
      await act(async () => steuerung.setze("a"));
      expect(k.stand()).toBe(200);
      await act(async () => steuerung.setze(null));
      expect(k.stand()).toBe(200);
    } finally {
      await abbauen();
    }
  });

  it("holt ein Ziel, das höher ist als sein Scrollbereich und darüber liegt, mit der Oberkante an den oberen Rand", async () => {
    // Der Fall aus Punkt 118: Liste 1.200 px hoch, Sichtfenster 400 px, die
    // Oberkante 3.000 px darüber. `nearest` richtete hier die Unterkante aus.
    const { behaelter, abbauen } = await rendere(<Huelle start={null} />);
    try {
      const { kasten, ziel } = teile(behaelter);
      const k = stelleKasten(kasten, { stand: 3000 });
      ziel.getBoundingClientRect = () => rechteck(-3000, 1200);
      await act(async () => steuerung.setze("a"));
      expect(k.stand()).toBe(0);
    } finally {
      await abbauen();
    }
  });

  it("bewegt ein hohes Ziel nicht, dessen Oberkante schon im Bild steht", async () => {
    // Oberkante bei +100 im Sichtfenster, Unterkante weit darunter. `nearest`
    // schöbe die Oberkante an den Rand — eine Bewegung ohne Anlass.
    const { behaelter, abbauen } = await rendere(<Huelle start={null} />);
    try {
      const { kasten, ziel } = teile(behaelter);
      const k = stelleKasten(kasten, { stand: 800 });
      ziel.getBoundingClientRect = () => rechteck(100, 1200);
      await act(async () => steuerung.setze("a"));
      expect(k.stand()).toBe(800);
    } finally {
      await abbauen();
    }
  });

  it("bewegt bei einem Ziel in einer eigenen Spalte nur deren Scrollstand — `main` bleibt stehen", async () => {
    // **Der Rückweg in der Prozessansicht** (E‑115): Die Zeile sitzt in der
    // Liste, die für sich scrollt. `scrollIntoView` hätte hier beide bewegt,
    // und mit `main` den Baum daneben.
    const { behaelter, abbauen } = await rendere(<Huelle start={null} bauform="in-spalte" />);
    try {
      const { kasten, spalte, ziel } = teile(behaelter);
      const k = stelleKasten(kasten, { stand: 700 });
      const s = stelleKasten(spalte, { hoehe: 300, inhalt: 2000, stand: 400 });
      ziel.getBoundingClientRect = () => rechteck(-250, 40);
      await act(async () => steuerung.setze("a"));
      expect(s.stand()).toBe(150);
      expect(k.stand()).toBe(700);
    } finally {
      await abbauen();
    }
  });
});

describe("Der Haken useBeginntOben — E‑115", () => {
  it("setzt den eigenen Scrollbereich einer klebenden Spalte auf den Anfang — und bewegt `main` nicht", async () => {
    // **Das Panel** ab `xl`: Es klebt und ist damit von sich aus im Bild. Wer
    // im Panel gelesen hat und über ein Kettenglied weiterspringt, beginnt in
    // der neuen Nachricht wieder oben; die Liste daneben bleibt stehen.
    const { behaelter, abbauen } = await rendere(
      <Huelle start={null} bauform="ist-klebende-spalte" oben />,
    );
    try {
      const { kasten, ziel } = teile(behaelter);
      const k = stelleKasten(kasten, { stand: 868 });
      const z = stelleKasten(ziel, { hoehe: 400, inhalt: 2300, stand: 950 });
      await act(async () => steuerung.setze("a"));
      expect(z.stand()).toBe(0);
      expect(k.stand()).toBe(868);
    } finally {
      await abbauen();
    }
  });

  it("setzt in einem klebenden Rahmen den Scrollbereich **darin** auf den Anfang", async () => {
    // **Die rechte Spalte der Prozessansicht**: Der Rahmen klebt und scrollt
    // nicht selbst, die Liste in ihm tut es. Ein zweiter Prozess setzt sie
    // wieder oben an.
    const { behaelter, abbauen } = await rendere(
      <Huelle start={null} bauform="in-klebendem-rahmen" oben />,
    );
    try {
      const { kasten, liste } = teile(behaelter);
      const k = stelleKasten(kasten, { stand: 6422 });
      const l = stelleKasten(liste, { hoehe: 940, inhalt: 1813, stand: 781 });
      await act(async () => steuerung.setze("a"));
      expect(l.stand()).toBe(0);
      expect(k.stand()).toBe(6422);
    } finally {
      await abbauen();
    }
  });

  it("holt das Ziel ins Bild wie E‑114, solange nichts klebt", async () => {
    // Unterhalb der Umbruchpunkte tragen die Hüllen dieselben Klassen und
    // kleben trotzdem nicht — dann gilt die Regel vom 09.09.2026 unverändert.
    const { behaelter, abbauen } = await rendere(<Huelle start={null} oben />);
    try {
      const { kasten, ziel } = teile(behaelter);
      const k = stelleKasten(kasten, { stand: 1004 });
      ziel.getBoundingClientRect = () => rechteck(-989, 492);
      await act(async () => steuerung.setze("a"));
      expect(k.stand()).toBe(15);
    } finally {
      await abbauen();
    }
  });
});

function Rueckweg({ start }: { start: string | null }) {
  const [offen, setOffen] = useState(start);
  useEffect(() => {
    steuerung.setze = setOffen;
  }, []);
  const geschlossen = useZuletztGeschlossen(offen);
  return <output data-geschlossen={geschlossen ?? "null"} />;
}

describe("Der Rückweg useZuletztGeschlossen", () => {
  it("nennt die Kennung genau nach dem Schließen — und beim Öffnen nichts", async () => {
    const { behaelter, abbauen } = await rendere(<Rueckweg start="a" />);
    try {
      const wert = () => behaelter.querySelector("output")?.getAttribute("data-geschlossen");
      expect(wert()).toBe("null");
      await act(async () => steuerung.setze(null));
      expect(wert()).toBe("a");
      // Bleibt es geschlossen, bleibt der Wert stehen — kein zweiter Wechsel.
      await act(async () => steuerung.setze(null));
      expect(wert()).toBe("a");
      await act(async () => steuerung.setze("b"));
      expect(wert()).toBe("null");
      await act(async () => steuerung.setze(null));
      expect(wert()).toBe("b");
    } finally {
      await abbauen();
    }
  });
});
