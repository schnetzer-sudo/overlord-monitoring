// @vitest-environment jsdom

import { act, useEffect, useRef, useState } from "react";
import { afterEach, beforeEach, describe, expect, it } from "vitest";

import { useInSicht, useZuletztGeschlossen } from "@/lib/in-sicht-bringen";

import { rendere } from "./hilfe/rendern";

/**
 * **Sieben Fälle, für die ein gerenderter Baum die einzige Prüfung ist** — der
 * Haken aus E‑114 (`docs/frontend-grundlagen.md` §7, vierte Bedingung).
 *
 * Ein Haken ist ohne Komponente nicht aufrufbar, und was er tut, ist ein
 * Aufruf am DOM: `scrollIntoView` auf dem Element hinter der Referenz. `jsdom`
 * bringt die Methode nicht mit und rechnet kein Layout — sie wird deshalb als
 * Spion auf `Element.prototype` gestellt, und die Frage lautet **ob** und
 * **womit** er gerufen wird: beim ersten Rendern mit Schlüssel, bei jedem
 * Wechsel des Schlüssels, nicht ohne Wechsel, nicht ohne Schlüssel. Dazu die
 * Höhenregel, die aus der Vorprobe in Chrome stammt (`docs/process-view.md`
 * M172): Für ein Ziel, das höher ist als sein Scrollbereich, zählt allein die
 * Oberkante — die Maße dafür werden gestellt, weil `jsdom` keine hat.
 *
 * **Was diese Tests nicht zeigen:** ob am Ende tatsächlich etwas ins Bild
 * kommt. Das entscheidet allein der Browser, und dort ist es gemessen (M172).
 */

type Aufruf = { ziel: Element; block: ScrollLogicalPosition | undefined };
let aufrufe: Aufruf[] = [];

beforeEach(() => {
  aufrufe = [];
  Element.prototype.scrollIntoView = function (
    this: Element,
    arg?: boolean | ScrollIntoViewOptions,
  ) {
    aufrufe.push({ ziel: this, block: typeof arg === "object" ? arg.block : undefined });
  };
});

afterEach(() => {
  // `jsdom` kennt die Methode nicht — nach dem Test soll das wieder so sein.
  delete (Element.prototype as { scrollIntoView?: unknown }).scrollIntoView;
});

/** Die Griffe von außen — gesetzt in einem Effekt, nicht beim Rendern. */
const steuerung: {
  setze: (schluessel: string | null) => void;
  rendereNeu: () => void;
} = {
  setze: () => undefined,
  rendereNeu: () => undefined,
};

function Ziel({ schluessel }: { schluessel: string | null }) {
  const ref = useRef<HTMLDivElement>(null);
  useInSicht(ref, schluessel);
  return <div ref={ref} data-ziel="" />;
}

/** Ein Ziel in einem Kasten, der senkrecht scrollt — die Lage des Anwendungsrahmens. */
function ZielImKasten({ schluessel }: { schluessel: string | null }) {
  const ref = useRef<HTMLDivElement>(null);
  useInSicht(ref, schluessel);
  return (
    <div data-kasten="" style={{ overflowY: "auto" }}>
      <div ref={ref} data-ziel="" />
    </div>
  );
}

function Huelle({ start, imKasten = false }: { start: string | null; imKasten?: boolean }) {
  const [schluessel, setSchluessel] = useState(start);
  const [, setDurchlauf] = useState(0);
  useEffect(() => {
    steuerung.setze = setSchluessel;
    steuerung.rendereNeu = () => setDurchlauf((n) => n + 1);
  }, []);
  return imKasten ? <ZielImKasten schluessel={schluessel} /> : <Ziel schluessel={schluessel} />;
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

/** Stellt dem Kasten die Maße eines Sichtfensters von 400 px und dem Ziel die genannten. */
function stelleMasse(behaelter: HTMLElement, zielTop: number, zielHoehe: number) {
  const kasten = behaelter.querySelector("[data-kasten]") as HTMLElement;
  const ziel = behaelter.querySelector("[data-ziel]") as HTMLElement;
  kasten.getBoundingClientRect = () => rechteck(0, 400);
  Object.defineProperty(kasten, "scrollHeight", { value: 5000, configurable: true });
  Object.defineProperty(kasten, "clientHeight", { value: 400, configurable: true });
  ziel.getBoundingClientRect = () => rechteck(zielTop, zielHoehe);
}

describe("Der Haken useInSicht", () => {
  it("holt das Ziel beim ersten Rendern mit Schlüssel ins Bild — mit `nearest`, wenn es hineinpasst", async () => {
    const { behaelter, abbauen } = await rendere(<Huelle start="a" />);
    try {
      expect(aufrufe).toHaveLength(1);
      expect(aufrufe[0]?.block).toBe("nearest");
      expect(aufrufe[0]?.ziel).toBe(behaelter.querySelector("[data-ziel]"));
    } finally {
      await abbauen();
    }
  });

  it("feuert nicht noch einmal, solange der Schlüssel derselbe bleibt", async () => {
    const { abbauen } = await rendere(<Huelle start="a" />);
    try {
      await act(async () => steuerung.rendereNeu());
      await act(async () => steuerung.setze("a"));
      expect(aufrufe).toHaveLength(1);
    } finally {
      await abbauen();
    }
  });

  it("feuert bei jedem Wechsel des Schlüssels erneut", async () => {
    const { abbauen } = await rendere(<Huelle start="a" />);
    try {
      await act(async () => steuerung.setze("b"));
      expect(aufrufe).toHaveLength(2);
      await act(async () => steuerung.setze("c"));
      expect(aufrufe).toHaveLength(3);
    } finally {
      await abbauen();
    }
  });

  it("tut ohne Schlüssel nichts — auch nicht, wenn er auf null zurückfällt", async () => {
    const { abbauen } = await rendere(<Huelle start={null} />);
    try {
      expect(aufrufe).toHaveLength(0);
      await act(async () => steuerung.setze("a"));
      expect(aufrufe).toHaveLength(1);
      await act(async () => steuerung.setze(null));
      expect(aufrufe).toHaveLength(1);
    } finally {
      await abbauen();
    }
  });

  it("holt ein Ziel, das höher ist als sein Scrollbereich und darüber liegt, mit `start` an den oberen Rand", async () => {
    // Der Fall aus Punkt 118: Liste 1.200 px hoch, Sichtfenster 400 px, die
    // Oberkante 3.000 px darüber. `nearest` richtete hier die Unterkante aus.
    const { behaelter, abbauen } = await rendere(<Huelle start={null} imKasten />);
    try {
      stelleMasse(behaelter, -3000, 1200);
      await act(async () => steuerung.setze("a"));
      expect(aufrufe).toHaveLength(1);
      expect(aufrufe[0]?.block).toBe("start");
    } finally {
      await abbauen();
    }
  });

  it("bewegt ein hohes Ziel nicht, dessen Oberkante schon im Bild steht", async () => {
    // Oberkante bei +100 im Sichtfenster, Unterkante weit darunter. `nearest`
    // schöbe die Oberkante an den Rand — eine Bewegung ohne Anlass.
    const { behaelter, abbauen } = await rendere(<Huelle start={null} imKasten />);
    try {
      stelleMasse(behaelter, 100, 1200);
      await act(async () => steuerung.setze("a"));
      expect(aufrufe).toHaveLength(0);
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
