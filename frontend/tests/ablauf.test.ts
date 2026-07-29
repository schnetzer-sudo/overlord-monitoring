import { describe, expect, it } from "vitest";

import { zielRoute, type Sitzungszustand } from "@/lib/ablauf";
import { ROUTEN } from "@/lib/routen";

const ZWANG: Sitzungszustand = {
  art: "angemeldet",
  passwortwechselNoetig: true,
  mandantAktiv: false,
};

const OHNE_MANDANT: Sitzungszustand = {
  art: "angemeldet",
  passwortwechselNoetig: false,
  mandantAktiv: false,
};

const FERTIG: Sitzungszustand = {
  art: "angemeldet",
  passwortwechselNoetig: false,
  mandantAktiv: true,
};

describe("Ablauf nach dem Anmelden", () => {
  it("stellt den Änderungszwang vor die Mandantenauswahl", () => {
    // Beide Bedingungen zugleich — der Zwang gewinnt. Das ist der eigentliche
    // Inhalt der Reihenfolge: Wer noch ein Einmalpasswort hat, kommt an keinem
    // anderen Endpunkt vorbei, auch nicht an der Mandantenliste.
    expect(zielRoute(ZWANG, ROUTEN.startseite)).toBe(ROUTEN.passwort);
    expect(zielRoute(ZWANG, ROUTEN.mandantenauswahl)).toBe(ROUTEN.passwort);
    expect(zielRoute(ZWANG, ROUTEN.prozesse)).toBe(ROUTEN.passwort);
  });

  it("lässt die Passwortseite selbst in Ruhe, sonst gäbe es keinen Ausweg", () => {
    expect(zielRoute(ZWANG, ROUTEN.passwort)).toBeNull();
  });

  it("stellt die Mandantenauswahl vor die Startseite", () => {
    expect(zielRoute(OHNE_MANDANT, ROUTEN.startseite)).toBe(ROUTEN.mandantenauswahl);
    expect(zielRoute(OHNE_MANDANT, ROUTEN.nachrichten)).toBe(ROUTEN.mandantenauswahl);
    expect(zielRoute(OHNE_MANDANT, ROUTEN.mandantenauswahl)).toBeNull();
  });

  it("lässt einen fertigen Zustand überall stehen", () => {
    expect(zielRoute(FERTIG, ROUTEN.startseite)).toBeNull();
    expect(zielRoute(FERTIG, ROUTEN.nachrichten)).toBeNull();
    // Freiwillig erreichbar: Passwortwechsel und Mandantenwechsel.
    expect(zielRoute(FERTIG, ROUTEN.passwort)).toBeNull();
    expect(zielRoute(FERTIG, ROUTEN.mandantenauswahl)).toBeNull();
  });

  it("holt einen Angemeldeten von der Anmeldeseite weg", () => {
    expect(zielRoute(FERTIG, ROUTEN.anmeldung)).toBe(ROUTEN.startseite);
  });

  it("schickt einen Abgemeldeten zur Anmeldung — und lässt ihn dort", () => {
    expect(zielRoute({ art: "abgemeldet" }, ROUTEN.startseite)).toBe(ROUTEN.anmeldung);
    expect(zielRoute({ art: "abgemeldet" }, ROUTEN.anmeldung)).toBeNull();
  });

  it("leitet nicht um, solange die Selbstauskunft noch lädt", () => {
    // Sonst blitzt bei jedem Seitenwechsel kurz die Anmeldeseite auf.
    expect(zielRoute({ art: "laedt" }, ROUTEN.startseite)).toBeNull();
    expect(zielRoute({ art: "laedt" }, ROUTEN.nachrichten)).toBeNull();
  });
});
