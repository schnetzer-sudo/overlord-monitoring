import { isValidElement } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";

/**
 * **Das Wurzel-Layout setzt die Panelbreite nur bei gültigem Cookie**
 * *(E‑1209, `docs/nachrichtendetail.md` §10.21)* — als Variable am `<html>`,
 * serverseitig vor dem ersten Paint, derselbe Leseweg wie die Anzeigegröße.
 *
 * Das Layout ist eine asynchrone Server-Komponente; aufgerufen wird es hier
 * wie von Next.js, mit gestellten Cookies. Gerendert wird nichts: Geprüft ist
 * das `style` des zurückgegebenen `<html>`.
 */

let wert: string | undefined;

vi.mock("next/headers", () => ({
  cookies: async () => ({
    get: (name: string) =>
      name === "overlord_panelbreite" && wert !== undefined ? { name, value: wert } : undefined,
  }),
}));
vi.mock("next/font/google", () => ({
  Geist: () => ({ variable: "schrift" }),
  Geist_Mono: () => ({ variable: "schrift-mono" }),
}));
vi.mock("@/app/providers", () => ({ Providers: () => null }));

const { default: RootLayout } = await import("@/app/layout");

async function stilAmHtml(): Promise<unknown> {
  const element = await RootLayout({ children: null });
  expect(isValidElement(element)).toBe(true);
  expect((element as { type: unknown }).type).toBe("html");
  return (element as { props: { style?: unknown } }).props.style;
}

beforeEach(() => {
  wert = undefined;
});

describe("Das Wurzel-Layout und die Panelbreite", () => {
  it("setzt die Variable bei gültigem Cookie", async () => {
    wert = "34.25";
    expect(await stilAmHtml()).toEqual({ "--panelbreite": "34.25rem" });
  });

  it("rundet auf 0,25 rem", async () => {
    wert = "34.3";
    expect(await stilAmHtml()).toEqual({ "--panelbreite": "34.25rem" });
  });

  it.each(["abc", "20", "999", "-30", ""])("setzt bei %j keine Variable", async (ungueltig) => {
    wert = ungueltig;
    expect(await stilAmHtml()).toBeUndefined();
  });

  it("setzt ohne Cookie keine Variable", async () => {
    expect(await stilAmHtml()).toBeUndefined();
  });
});
