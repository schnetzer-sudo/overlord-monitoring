import { describe, expect, it } from "vitest";

import { NACHRICHTENLISTE } from "@/features/nachrichten/spalten";
import { TREFFERLISTE, TREFFERLISTE_OHNE_TREFFER } from "@/features/nachrichten/treffer-spalten";
import {
  liesPanelbreite,
  PANEL_NEBEN_LISTE_AB_XL,
  panelbreiteCookie,
  panelbreiteStil,
  panelNebenListe,
  PANELBREITE_COOKIE,
  rundePanelbreite,
} from "@/lib/panelbreite";
import { grundmengeInPixeln, PX_JE_REM } from "@/lib/spaltenwahl";

/**
 * **Die ziehbare Panelbreite — die reinen Regeln** *(E‑1208 bis E‑1210,
 * `docs/nachrichtendetail.md` §10.21)*: Lesen des Cookies, Rundung, die Zeile
 * für `document.cookie`, die Variable am `<html>`, das Maximum aus der
 * Spaltendefinition und die Klassen der Hülle.
 */

describe("liesPanelbreite", () => {
  it.each([
    ["34.25", 34.25],
    ["30", 30],
    ["26", 26],
    ["320", 320],
    ["40.5", 40.5],
  ])("liest den gültigen Wert %s", (wert, rem) => {
    expect(liesPanelbreite(wert)).toBe(rem);
  });

  it("rundet einen Wert zwischen den Stufen auf 0,25 rem", () => {
    expect(liesPanelbreite("34.3")).toBe(34.25);
    expect(liesPanelbreite("34.4")).toBe(34.5);
    expect(liesPanelbreite("34.125")).toBe(34.25);
  });

  it.each(["25.75", "0", "320.25", "999"])("verwirft %s — außerhalb der Grenzen", (wert) => {
    expect(liesPanelbreite(wert)).toBeNull();
  });

  it.each(["", "abc", "30rem", "-30", "1e2", " 30", "30.12345", "NaN", "Infinity", "3,5"])(
    "verwirft den ungültigen Wert %j",
    (wert) => {
      expect(liesPanelbreite(wert)).toBeNull();
    },
  );

  it("liefert ohne Cookie null", () => {
    expect(liesPanelbreite(undefined)).toBeNull();
  });
});

describe("rundePanelbreite", () => {
  it("rundet auf 0,25 rem", () => {
    expect(rundePanelbreite(34.12)).toBe(34);
    expect(rundePanelbreite(34.13)).toBe(34.25);
    expect(rundePanelbreite(34.375)).toBe(34.5);
    expect(rundePanelbreite(30)).toBe(30);
  });
});

describe("Cookie und Variable", () => {
  it("schreibt das Cookie mit den Eigenschaften von overlord_dichte", () => {
    const zeile = panelbreiteCookie(34.25);
    expect(zeile).toBe(`${PANELBREITE_COOKIE}=34.25; Max-Age=31536000; Path=/; SameSite=Lax`);
    expect(zeile).not.toMatch(/HttpOnly/i);
  });

  it("löscht das Cookie mit null", () => {
    expect(panelbreiteCookie(null)).toBe(`${PANELBREITE_COOKIE}=; Max-Age=0; Path=/; SameSite=Lax`);
  });

  it("setzt die Variable in rem — und ohne Breite keine", () => {
    expect(panelbreiteStil(34.25)).toEqual({ "--panelbreite": "34.25rem" });
    expect(panelbreiteStil(null)).toBeUndefined();
  });
});

describe("Das Maximum lässt der Liste ihre Grundmenge", () => {
  it("nimmt die Grundmenge aus der Spaltendefinition, nicht als zweite Zahl", () => {
    const rem = grundmengeInPixeln(NACHRICHTENLISTE) / PX_JE_REM;
    expect(panelNebenListe(NACHRICHTENLISTE)).toEqual({
      "--panel-abzug": `calc(1rem + 2px + ${rem}rem)`,
    });
  });

  it("unterscheidet die Trefferliste mit und ohne Spalte „Treffer“", () => {
    const mit = panelNebenListe(TREFFERLISTE) as Record<string, string>;
    const ohne = panelNebenListe(TREFFERLISTE_OHNE_TREFFER) as Record<string, string>;
    expect(mit["--panel-abzug"]).toContain(`${grundmengeInPixeln(TREFFERLISTE) / PX_JE_REM}rem`);
    expect(ohne["--panel-abzug"]).toContain(
      `${grundmengeInPixeln(TREFFERLISTE_OHNE_TREFFER) / PX_JE_REM}rem`,
    );
    expect(mit["--panel-abzug"]).not.toBe(ohne["--panel-abzug"]);
  });

  it("hält in der Prozessansicht die Bildlaufleiste der Liste frei", () => {
    const stil = panelNebenListe(NACHRICHTENLISTE, { listeScrolltSelbst: true }) as Record<
      string,
      string
    >;
    expect(stil["--panel-abzug"]).toContain("+ 15px");
  });
});

describe("Die Klassen der Hülle", () => {
  const klassen = PANEL_NEBEN_LISTE_AB_XL.split(/\s+/).filter(Boolean);

  it("gelten nur ab xl — darunter gibt es keine Breitenwahl", () => {
    for (const klasse of klassen) {
      expect(klasse, klasse).toMatch(/^2?xl:/);
    }
  });

  it("tragen Minimum, Wert, Maximum und den Deckel der Namensspalte", () => {
    expect(klassen).toEqual(
      expect.arrayContaining([
        "xl:[--panel-min:26rem]",
        "2xl:[--panel-min:30rem]",
        "xl:w-[var(--panelbreite,var(--panel-min))]",
        "xl:min-w-[var(--panel-min)]",
        "xl:max-w-[calc(100%-var(--panel-abzug))]",
        "xl:[--eigenschaft-name-max:16rem]",
      ]),
    );
  });
});
