// @vitest-environment jsdom

import { act } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { AngemeldetProvider } from "@/components/angemeldet";
import type { Nutzerzeile } from "@/features/benutzer/api";
import { BenutzerTabelle } from "@/features/benutzer/components/benutzer-tabelle";
import { ZeilenFormular } from "@/features/benutzer/components/zeilen-formular";
import { texteFuer } from "@/i18n";

import { rendere } from "./hilfe/rendern";

/**
 * **Acht Fälle, für die ein gerenderter Baum die einzige Prüfung ist.**
 *
 * Die Bedingung steht in `tests/hilfe/rendern.tsx`: nicht „ein Baum wäre
 * bequemer", sondern „es gibt keinen anderen Ort, an dem der Satz belegbar
 * wäre". Alle acht erfüllen sie, und sie zerfallen in zwei Klassen.
 *
 * **Aussagen über Anwesenheit und Abwesenheit im Baum** (vier Fälle, je mit
 * ihrer Gegenprobe — ohne sie bewiese jeder nur, dass irgendwo irgendetwas
 * steht):
 *
 * 1. **`lastLogin = null` heißt „nie angemeldet" und ist keine leere Zelle.**
 *    Der Unterschied entsteht erst in der Zelle, und er ist genau der zwischen
 *    einer Auskunft und einer fehlenden Angabe (E17). Keine reine Funktion
 *    fängt ihn: Es gibt nichts zu rechnen, nur etwas hinzuschreiben.
 * 2. **Die zwei Sperren stehen in zwei Zellen und werden nie zu einer** (E14,
 *    E20). Dass `locked` und `lockedUntil` **beide** dastehen und jede ihren
 *    eigenen Satz trägt, ist eine Aussage über die Tabelle und über nichts
 *    sonst. Die naheliegende Zusammenfassung — eine Spalte „gesperrt", die auf
 *    beides anspringt — bestünde jede Prüfung an einer reinen Funktion.
 *
 * **Die Verdrahtung der Vorwarnung und der Mengenersetzung** (vier Fälle): Die
 * *Regeln* sind reine Funktionen und stehen in `tests/benutzer.test.ts`. Was
 * hier belegt wird, ist, dass jemand sie **abfragt**, bevor der Aufruf losläuft
 * — beim eigenen Konto kommt zuerst der Dialog und **kein Aufruf**, beim fremden
 * läuft er sofort, und die Mengenersetzung schickt die **vollständige**
 * Zielmenge über die Leitung statt einer Differenz. **Eine richtige Regel, die
 * niemand abfragt, sieht von außen aus wie keine.**
 *
 * **Die Aufrufe gehen an ein gestelltes `fetch`** und nicht ins Netz. Das ist
 * hier nicht Bequemlichkeit, sondern die Bedingung dafür, dass die letzten vier
 * Fälle überhaupt etwas aussagen: Geprüft wird, **ob** und **womit** gerufen
 * wird.
 */

const TEXTE = texteFuer("de");
const B = TEXTE.benutzer;

const MANDANTEN = [
  { id: "VOTG", name: "VOTG Tanktainer GmbH" },
  { id: "NEXANS", name: "Nexans autoelectric GmbH" },
];

function zeile(werte: Partial<Nutzerzeile> = {}): Nutzerzeile {
  return {
    id: 7,
    username: "beispielnutzer",
    role: "MANDANT",
    tenants: ["VOTG"],
    locked: false,
    lockedUntil: null,
    active: true,
    mustChangePassword: false,
    lastLogin: null,
    ...werte,
  };
}

describe("Die Kontenliste zeigt", () => {
  it("„nie angemeldet“ statt einer leeren Zelle, wenn lastLogin null ist", async () => {
    const { behaelter, abbauen } = await rendere(
      <BenutzerTabelle zeilen={[zeile({ lastLogin: null })]} />,
    );

    try {
      expect(behaelter.textContent ?? "").toContain(B.anmeldung.nie);
    } finally {
      await abbauen();
    }
  });

  it("bei vorhandener Anmeldung den Zeitpunkt und nicht den Satz", async () => {
    const { behaelter, abbauen } = await rendere(
      <BenutzerTabelle zeilen={[zeile({ lastLogin: "2026-08-20T06:01:10.374Z" })]} />,
    );

    try {
      const text = behaelter.textContent ?? "";
      expect(text).not.toContain(B.anmeldung.nie);
      // Ohne Zeitzonen-Provider formatiert `useAnzeigezone` in UTC — der Test
      // sagt nichts über die Zone, nur darüber, dass ein Zeitpunkt dasteht.
      expect(text).toContain("20.08.2026");
    } finally {
      await abbauen();
    }
  });
});

/**
 * **Zwei Sperren, zwei Zellen — und keine der beiden Aussagen darf die andere
 * verdecken.** Die Gegenprobe steht in beiden Richtungen: administrativ
 * gesperrt ohne Zeitsperre, und zeitgesperrt ohne administrative Sperre. Ohne
 * sie bewiese der Test nur, dass irgendwo „gesperrt“ steht.
 */
describe("Die zwei Sperren", () => {
  it("stehen getrennt: administrativ gesperrt, ohne laufende Zeitsperre", async () => {
    const { behaelter, abbauen } = await rendere(
      <BenutzerTabelle zeilen={[zeile({ locked: true, lockedUntil: null })]} />,
    );

    try {
      const zellen = [...behaelter.querySelectorAll("tbody td")].map(
        (zelle) => zelle.textContent ?? "",
      );

      // Genau der Wortlaut und nicht „enthält": „nicht gesperrt" enthält
      // „gesperrt", und eine Teilstringprüfung ginge hier in beide Richtungen
      // durch — also gerade an der Unterscheidung vorbei, um die es geht.
      expect(zellen[3].trim()).toBe(B.sperre.gesperrt);
      expect(zellen[4].trim()).toBe(B.zeitsperre.keine);
      // Die Zeitsperre-Zelle trägt keinen Zeitpunkt — es läuft keine.
      expect(zellen[4]).not.toMatch(/\d{2}\.\d{2}\.\d{4}/);
    } finally {
      await abbauen();
    }
  });

  it("stehen getrennt: zeitgesperrt, ohne administrative Sperre", async () => {
    const { behaelter, abbauen } = await rendere(
      <BenutzerTabelle zeilen={[zeile({ locked: false, lockedUntil: "2026-08-24T12:14:30Z" })]} />,
    );

    try {
      const zellen = [...behaelter.querySelectorAll("tbody td")].map(
        (zelle) => zelle.textContent ?? "",
      );

      // Die administrative Sperre bleibt ausdrücklich „nicht gesperrt“ — sie
      // wird von der laufenden Zeitsperre nicht mitgezogen (E14).
      expect(zellen[3].trim()).toBe(B.sperre.offen);
      expect(zellen[4]).toContain("24.08.2026");
      expect(zellen[4].trim()).not.toBe(B.zeitsperre.keine);
    } finally {
      await abbauen();
    }
  });
});

describe("Die Vorwarnung (E19)", () => {
  let rufe: { pfad: string; koerper: unknown }[];

  beforeEach(() => {
    rufe = [];
    // Der CSRF-Token steht im Cookie, damit `lib/http` ihn nicht erst holt.
    document.cookie = "XSRF-TOKEN=test-token";

    vi.stubGlobal("fetch", (eingabe: RequestInfo | URL, init?: RequestInit) => {
      const pfad = String(eingabe);
      rufe.push({
        pfad,
        koerper: typeof init?.body === "string" ? JSON.parse(init.body) : undefined,
      });
      // Je Pfad die richtige Gestalt. Ein Stub, der überall dasselbe liefert,
      // beantwortet `/api/mandanten` mit einem Objekt statt einer Liste — und
      // die Auswahl fiele mit einem Fehler um, den kein Testfall meint.
      const rumpf = pfad.includes("/api/mandanten") ? MANDANTEN : zeile();
      return Promise.resolve(
        new Response(JSON.stringify(rumpf), {
          status: 200,
          headers: { "content-type": "application/json" },
        }),
      );
    });
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  async function formular(werte: Partial<Nutzerzeile>, angemeldet: string | undefined) {
    return rendere(
      <AngemeldetProvider username={angemeldet}>
        <ZeilenFormular zeile={zeile(werte)} />
      </AngemeldetProvider>,
    );
  }

  /**
   * Nur die **schreibenden** Aufrufe. Das Formular holt beim Einhängen die
   * wählbaren Mandanten — ein lesender Aufruf, der mit der Frage dieser Fälle
   * nichts zu tun hat und sie sonst um genau eins verschöbe.
   */
  function schreibrufe() {
    return rufe.filter((ruf) => ruf.pfad.includes("/api/admin/users/"));
  }

  function knopfMit(behaelter: HTMLElement, beschriftung: string): HTMLButtonElement | undefined {
    return [...behaelter.querySelectorAll("button")].find((knopf) =>
      (knopf.textContent ?? "").includes(beschriftung),
    );
  }

  async function passwortAbschicken(behaelter: HTMLElement) {
    const feld = behaelter.querySelector<HTMLInputElement>('input[type="password"]');
    await act(async () => {
      // React hört auf den nativen Setter, nicht auf `value =`.
      const setzer = Object.getOwnPropertyDescriptor(
        window.HTMLInputElement.prototype,
        "value",
      )?.set;
      setzer?.call(feld, "einneuesgeheimnis");
      feld?.dispatchEvent(new Event("input", { bubbles: true }));
    });
    await act(async () => {
      behaelter
        .querySelector("form")
        ?.dispatchEvent(new Event("submit", { bubbles: true, cancelable: true }));
    });
  }

  /**
   * **Der Grenzfall, für den die Regel gebaut ist:** Das Konto heißt `Lukas`,
   * angemeldet ist `lukas`. Ein `===` hielte die Zeile für eine fremde und
   * schickte den Admin ohne ein Wort auf die Anmeldung.
   */
  it("erscheint beim eigenen Konto — auch bei abweichender Schreibweise — und ruft noch nicht", async () => {
    const { behaelter, abbauen } = await formular({ username: "Lukas" }, "lukas");

    try {
      await passwortAbschicken(behaelter);

      expect(document.body.textContent ?? "").toContain(B.vorwarnung.titel);
      expect(schreibrufe()).toHaveLength(0);
    } finally {
      await abbauen();
    }
  });

  it("führt den Vorgang aus, sobald bestätigt wird", async () => {
    const { behaelter, abbauen } = await formular({ username: "Lukas" }, "lukas");

    try {
      await passwortAbschicken(behaelter);

      const bestaetigen = knopfMit(document.body, B.vorwarnung.bestaetigen);
      expect(bestaetigen).toBeDefined();
      await act(async () => {
        bestaetigen?.click();
      });

      expect(schreibrufe()).toHaveLength(1);
      expect(schreibrufe()[0].pfad).toContain("/api/admin/users/7/password");
    } finally {
      await abbauen();
    }
  });

  it("erscheint nicht bei einem fremden Konto — dort läuft der Aufruf sofort", async () => {
    const { behaelter, abbauen } = await formular({ username: "jemand-anders" }, "lukas");

    try {
      await passwortAbschicken(behaelter);

      expect(document.body.textContent ?? "").not.toContain(B.vorwarnung.titel);
      expect(schreibrufe()).toHaveLength(1);
      expect(schreibrufe()[0].pfad).toContain("/api/admin/users/7/password");
    } finally {
      await abbauen();
    }
  });

  /**
   * **Die Mengenersetzung schickt die vollständige Zielmenge** und keine
   * Differenz (E4). Das Konto trägt `VOTG`; nach dem Anhaken von `NEXANS` gehen
   * **beide** über die Leitung — nicht `{NEXANS}` als Zusatz und nicht
   * `{+NEXANS}` als Anweisung.
   */
  it("schickt bei der Mandantenmenge die vollständige Zielmenge", async () => {
    const zwischenspeicher = (await import("@tanstack/react-query")).QueryClient;
    const speicher = new zwischenspeicher({
      defaultOptions: { queries: { retry: false, staleTime: Infinity, gcTime: Infinity } },
    });
    speicher.setQueryData(
      ["mandanten"],
      [
        { id: "VOTG", name: "VOTG Tanktainer GmbH" },
        { id: "NEXANS", name: "Nexans autoelectric GmbH" },
      ],
    );

    const { behaelter, abbauen } = await rendere(
      <AngemeldetProvider username="jemand-anders">
        <ZeilenFormular zeile={zeile({ tenants: ["VOTG"] })} />
      </AngemeldetProvider>,
      speicher,
    );

    try {
      const haken = [...behaelter.querySelectorAll('button[role="checkbox"]')];
      expect(haken).toHaveLength(2);

      // `VOTG` ist gesetzt und als letzte Zuordnung gesperrt (E10); `NEXANS`
      // ist frei. Genau das ist die Gegenprobe zur Regel.
      expect(haken[1].getAttribute("data-state")).toBe("checked");
      expect((haken[1] as HTMLButtonElement).disabled).toBe(true);

      await act(async () => {
        (haken[0] as HTMLButtonElement).click();
      });
      await act(async () => {
        knopfMit(behaelter, B.mandanten.speichern)?.click();
      });

      const mandantenruf = rufe.find((ruf) => ruf.pfad.includes("/tenants"));
      expect(mandantenruf).toBeDefined();
      expect(mandantenruf?.koerper).toEqual({ tenants: ["VOTG", "NEXANS"] });
    } finally {
      await abbauen();
    }
  });
});
