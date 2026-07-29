import { describe, expect, it } from "vitest";

import { de } from "@/i18n/de";
import { en } from "@/i18n/en";
import { ProblemFehler } from "@/lib/http";
import { fehleranzeige } from "@/lib/fehlertext";

/** Alle Blattpfade eines verschachtelten Textobjekts, etwa `anmeldung.titel`. */
function pfade(wert: unknown, praefix = ""): string[] {
  if (typeof wert !== "object" || wert === null) {
    return [praefix];
  }
  return Object.entries(wert).flatMap(([schluessel, unterwert]) =>
    pfade(unterwert, praefix === "" ? schluessel : `${praefix}.${schluessel}`),
  );
}

describe("Sprachdateien", () => {
  it("haben denselben Schlüsselsatz — kein fehlender, kein überzähliger", () => {
    const deutsch = pfade(de).sort();
    const englisch = pfade(en).sort();

    expect(englisch.filter((s) => !deutsch.includes(s))).toEqual([]);
    expect(deutsch.filter((s) => !englisch.includes(s))).toEqual([]);
    expect(deutsch).toEqual(englisch);
  });

  it("enthalten keine leeren Texte", () => {
    for (const sprachdatei of [de, en]) {
      const leere = Object.entries(flach(sprachdatei)).filter(([, text]) => text.trim() === "");
      expect(leere).toEqual([]);
    }
  });
});

/**
 * Ein 404 sagt niemals etwas über Berechtigung.
 *
 * Das Backend verbirgt sorgfältig, ob eine Ressource nicht existiert oder einem
 * fremden Mandanten gehört — beides liefert denselben Statuscode und denselben
 * Antwortrumpf. Ein Text wie „kein Zugriff" oder „nicht berechtigt" in der
 * Oberfläche hebelte genau das aus: Er verriete, dass die Zeile existiert.
 */
const VERRAETERISCH = [
  "berecht",
  "zugriff",
  "erlaub",
  "verboten",
  "gesperrt",
  "permission",
  "access",
  "denied",
  "forbidden",
  "unauthor",
  "allowed",
];

describe("404", () => {
  it("wird in keiner Sprache als fehlende Berechtigung formuliert", () => {
    for (const sprachdatei of [de, en]) {
      const text = sprachdatei.fehler["nicht-gefunden"].toLowerCase();
      for (const wort of VERRAETERISCH) {
        expect(text, `"${text}" enthält "${wort}"`).not.toContain(wort);
      }
    }
  });

  it("wird übersetzt und nicht aus dem Antworttext übernommen", () => {
    const antwort = new ProblemFehler({
      status: 404,
      typ: "nicht-gefunden",
      titel: "Nicht gefunden",
      detail: "Die angeforderte Ressource wurde nicht gefunden.",
    });

    for (const sprachdatei of [de, en]) {
      const anzeige = fehleranzeige(antwort, sprachdatei);
      expect(anzeige.text).toBe(sprachdatei.fehler["nicht-gefunden"]);
      // Und keine Fehler-Kennung: Die hilft nur bei einem technischen Fehler.
      expect(anzeige.kennung).toBeUndefined();
    }
  });

  it("fällt auf `detail` zurück, wenn ein Typ noch nicht übersetzt ist", () => {
    const antwort = new ProblemFehler({
      status: 409,
      typ: "irgendein-neuer-typ",
      detail: "Text aus dem Backend.",
    });
    expect(fehleranzeige(antwort, de).text).toBe("Text aus dem Backend.");
  });
});

function flach(wert: unknown, praefix = ""): Record<string, string> {
  if (typeof wert === "string") {
    return { [praefix]: wert };
  }
  if (typeof wert !== "object" || wert === null) {
    return {};
  }
  return Object.assign(
    {},
    ...Object.entries(wert).map(([schluessel, unterwert]) =>
      flach(unterwert, praefix === "" ? schluessel : `${praefix}.${schluessel}`),
    ),
  );
}
