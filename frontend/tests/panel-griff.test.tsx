// @vitest-environment jsdom

import { act, Profiler, useRef } from "react";
import { afterEach, beforeEach, describe, expect, it } from "vitest";

import { texteFuer } from "@/i18n";
import { PanelGriff } from "@/features/nachrichten/components/panel-griff";
import { PANELBREITE_COOKIE } from "@/lib/panelbreite";

import { rendere } from "./hilfe/rendern";

/**
 * **Der Griff zwischen Liste und Panel** *(E‑1208 bis E‑1210,
 * `docs/nachrichtendetail.md` §10.21)* — gerendert, weil die Sätze am Dokument
 * hängen: ARIA-Werte am Element, die Variable an Hülle und `<html>`, das Cookie
 * und wie oft es geschrieben wird, die Textauswahl während des Ziehens und dass
 * React dabei nicht rendert.
 *
 * **Das CSS ist gestellt.** `jsdom` rechnet kein Layout: Die Breite der Hülle
 * liefert ein gestelltes `getBoundingClientRect` — aus einer Wunschbreite im
 * `style`, sonst aus der Variable, begrenzt auf Minimum und Maximum, so wie das
 * CSS sie anwendet (`lib/panelbreite.ts`). Geprüft wird der Griff, nicht das Stylesheet; das
 * steht in `tests/panelbreite.test.ts` und im Browser (§10.21).
 */

const TEXTE = texteFuer("de").nachrichten.detail.panelbreite;

const MIN_PX = 480; // 30 rem bei 16 px
const MAX_PX = 888; // 55,5 rem

/** Was am Cookie ankommt — gestellt, damit jede Zeile zählt. */
let cookies: string[] = [];
let commits = 0;

beforeEach(() => {
  cookies = [];
  commits = 0;
  Object.defineProperty(document, "cookie", {
    configurable: true,
    get: () => "",
    set: (zeile: string) => {
      cookies.push(zeile);
    },
  });
  if (!("setPointerCapture" in HTMLElement.prototype)) {
    Object.assign(HTMLElement.prototype, { setPointerCapture: () => undefined });
  }
  document.documentElement.removeAttribute("style");
});

afterEach(() => {
  Reflect.deleteProperty(document, "cookie");
  document.documentElement.removeAttribute("style");
});

function wertIn(element: HTMLElement): number | null {
  const roh = element.style.getPropertyValue("--panelbreite");
  return roh === "" ? null : parseFloat(roh) * 16;
}

/** Die Hülle, wie das CSS sie bemisst: Variable an ihr, sonst am `<html>`, sonst das Minimum. */
function Aufbau() {
  const huelle = useRef<HTMLDivElement>(null);
  return (
    <div>
      <Profiler id="griff" onRender={() => (commits += 1)}>
        <PanelGriff huelle={huelle} />
      </Profiler>
      <div
        ref={(element) => {
          huelle.current = element;
          if (element === null) return;
          element.getBoundingClientRect = () => {
            // Eine Wunschbreite im `style` geht vor — so fragt der Griff Minimum
            // und Maximum ab; sonst die Variable an der Hülle, am `<html>`, das
            // Minimum.
            const wunsch = element.style.width === "" ? null : parseFloat(element.style.width);
            const px = wunsch ?? wertIn(element) ?? wertIn(document.documentElement) ?? MIN_PX;
            const breite = Math.min(MAX_PX, Math.max(MIN_PX, px));
            return { width: breite, height: 600, x: 0, y: 0 } as DOMRect;
          };
        }}
        data-huelle=""
      />
    </div>
  );
}

function griffIn(behaelter: HTMLElement): HTMLDivElement {
  const griff = behaelter.querySelector<HTMLDivElement>('[role="separator"]');
  expect(griff).not.toBeNull();
  return griff as HTMLDivElement;
}

function aria(griff: HTMLElement) {
  return ["aria-valuenow", "aria-valuemin", "aria-valuemax"].map((name) =>
    griff.getAttribute(name),
  );
}

async function taste(griff: HTMLElement, key: string, mehr: KeyboardEventInit = {}) {
  let ereignis!: KeyboardEvent;
  await act(async () => {
    ereignis = new KeyboardEvent("keydown", { key, bubbles: true, cancelable: true, ...mehr });
    griff.dispatchEvent(ereignis);
  });
  return ereignis;
}

async function zeiger(griff: HTMLElement, typ: string, clientX: number) {
  let ereignis!: PointerEvent;
  await act(async () => {
    ereignis = new PointerEvent(typ, {
      bubbles: true,
      cancelable: true,
      clientX,
      button: 0,
      pointerId: 1,
      pointerType: "mouse",
    });
    griff.dispatchEvent(ereignis);
  });
  return ereignis;
}

const html = () => document.documentElement.style.getPropertyValue("--panelbreite");

describe("Der Griff zwischen Liste und Panel", () => {
  it("ist ein senkrechter Trenner mit Werten in rem, fokussierbar und nur ab xl", async () => {
    const { behaelter, abbauen } = await rendere(<Aufbau />);

    try {
      const griff = griffIn(behaelter);
      expect(griff.getAttribute("aria-orientation")).toBe("vertical");
      expect(griff.getAttribute("aria-label")).toBe(TEXTE.griff);
      expect(griff.title).toBe(TEXTE.hinweis);
      expect(griff.tabIndex).toBe(0);
      expect(aria(griff)).toEqual(["30", "30", "55.5"]);

      const klassen = griff.className.split(/\s+/);
      expect(klassen).toEqual(
        expect.arrayContaining([
          "hidden",
          "xl:block",
          "cursor-col-resize",
          "focus-visible:ring-2",
          "self-stretch",
        ]),
      );
      // Kein Übergang, und die Haarlinie ist nur Zeichnung.
      expect(griff.innerHTML).not.toMatch(/transition|duration-|animate-/);
      expect(griff.querySelector("span")?.getAttribute("aria-hidden")).toBe("true");
    } finally {
      await abbauen();
    }
  });

  it("verbreitert mit Pfeil links und verschmälert mit Pfeil rechts, je 1 rem, Cookie je Taste", async () => {
    const { behaelter, abbauen } = await rendere(<Aufbau />);

    try {
      const griff = griffIn(behaelter);
      const links = await taste(griff, "ArrowLeft");
      expect(links.defaultPrevented).toBe(true);
      expect(html()).toBe("31rem");
      expect(aria(griff)[0]).toBe("31");
      expect(cookies).toEqual([`${PANELBREITE_COOKIE}=31; Max-Age=31536000; Path=/; SameSite=Lax`]);

      await taste(griff, "ArrowLeft");
      await taste(griff, "ArrowRight");
      expect(html()).toBe("31rem");
      expect(cookies).toHaveLength(3);

      // Unten hält das Minimum.
      await taste(griff, "ArrowRight");
      await taste(griff, "ArrowRight");
      expect(html()).toBe("30rem");
      expect(aria(griff)[0]).toBe("30");

      // Eine Taste mit Modifier gehört dem Browser.
      const mitAlt = await taste(griff, "ArrowLeft", { altKey: true });
      expect(mitAlt.defaultPrevented).toBe(false);
      expect(cookies).toHaveLength(5);
    } finally {
      await abbauen();
    }
  });

  it("hält oben das Maximum", async () => {
    document.documentElement.style.setProperty("--panelbreite", "55rem");
    const { behaelter, abbauen } = await rendere(<Aufbau />);

    try {
      const griff = griffIn(behaelter);
      await taste(griff, "ArrowLeft");
      await taste(griff, "ArrowLeft");
      expect(html()).toBe("55.5rem");
      expect(aria(griff)).toEqual(["55.5", "30", "55.5"]);
    } finally {
      await abbauen();
    }
  });

  it("setzt beim Doppelklick zurück und löscht das Cookie", async () => {
    const { behaelter, abbauen } = await rendere(<Aufbau />);

    try {
      const griff = griffIn(behaelter);
      await taste(griff, "ArrowLeft");
      expect(html()).toBe("31rem");

      await act(async () => {
        griff.dispatchEvent(new MouseEvent("dblclick", { bubbles: true }));
      });
      expect(html()).toBe("");
      expect(cookies.at(-1)).toBe(`${PANELBREITE_COOKIE}=; Max-Age=0; Path=/; SameSite=Lax`);
      expect(aria(griff)[0]).toBe("30");
    } finally {
      await abbauen();
    }
  });

  it("setzt beim Ziehen nur die Variable an der Hülle und schreibt das Cookie einmal, am Ende", async () => {
    const { behaelter, abbauen } = await rendere(<Aufbau />);

    try {
      const griff = griffIn(behaelter);
      const huelle = behaelter.querySelector<HTMLElement>("[data-huelle]")!;
      const vorher = commits;

      const druck = await zeiger(griff, "pointerdown", 1000);
      // Keine Textauswahl: weder der Druck selbst noch unterwegs.
      expect(druck.defaultPrevented).toBe(true);
      expect(document.documentElement.style.userSelect).toBe("none");

      for (let x = 990; x >= 900; x -= 10) {
        await zeiger(griff, "pointermove", x);
      }
      // 100 px nach links bei 16 px je rem: 6,25 rem breiter.
      expect(huelle.style.getPropertyValue("--panelbreite")).toBe("36.25rem");
      expect(html()).toBe("");
      expect(cookies).toEqual([]);
      // Kein React-Zustand je Bewegung: Unterwegs rendert nichts.
      expect(commits).toBe(vorher);

      await zeiger(griff, "pointerup", 900);
      expect(cookies).toEqual([
        `${PANELBREITE_COOKIE}=36.25; Max-Age=31536000; Path=/; SameSite=Lax`,
      ]);
      expect(html()).toBe("36.25rem");
      expect(huelle.style.getPropertyValue("--panelbreite")).toBe("");
      expect(document.documentElement.style.userSelect).toBe("");
      expect(aria(griff)[0]).toBe("36.25");

      // Ein spätes lostpointercapture schreibt nicht noch einmal.
      await act(async () => {
        griff.dispatchEvent(
          new PointerEvent("lostpointercapture", { bubbles: true, pointerId: 1 }),
        );
      });
      expect(cookies).toHaveLength(1);
    } finally {
      await abbauen();
    }
  });

  it("begrenzt das Ziehen auf das Maximum und schreibt ohne Bewegung nichts", async () => {
    const { behaelter, abbauen } = await rendere(<Aufbau />);

    try {
      const griff = griffIn(behaelter);
      await zeiger(griff, "pointerdown", 1000);
      await zeiger(griff, "pointerup", 1000);
      expect(cookies).toEqual([]);

      await zeiger(griff, "pointerdown", 1000);
      await zeiger(griff, "pointermove", 100);
      await zeiger(griff, "pointerup", 100);
      expect(cookies).toEqual([
        `${PANELBREITE_COOKIE}=55.5; Max-Age=31536000; Path=/; SameSite=Lax`,
      ]);
    } finally {
      await abbauen();
    }
  });
});
