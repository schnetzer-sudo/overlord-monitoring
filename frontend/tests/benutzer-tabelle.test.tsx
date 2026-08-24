// @vitest-environment jsdom

import { act } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { AngemeldetProvider } from "@/components/angemeldet";
import type { Nutzerzeile } from "@/features/benutzer/api";
import { BenutzerAnsicht } from "@/features/benutzer/components/benutzer-ansicht";
import { BenutzerTabelle } from "@/features/benutzer/components/benutzer-tabelle";
import { ZeilenFormular } from "@/features/benutzer/components/zeilen-formular";
import { useVorgang } from "@/features/benutzer/hooks";
import { texteFuer } from "@/i18n";

import { neuerZwischenspeicher, rendere } from "./hilfe/rendern";

/**
 * **Zehn Fälle, für die ein gerenderter Baum die einzige Prüfung ist.**
 *
 * Die Bedingung steht in `tests/hilfe/rendern.tsx`: nicht „ein Baum wäre
 * bequemer", sondern „es gibt keinen anderen Ort, an dem der Satz belegbar
 * wäre". Alle zehn erfüllen sie, und sie zerfallen in drei Klassen.
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
 * **Die Verdrahtung der Sperre** (zwei Fälle): dass die Schaltfläche jeder
 * *anderen* Zeile wirklich gesperrt ist, solange eine offen ist — und die
 * Gegenprobe. Der Einsatz ist ein bereits getipptes Einmalpasswort, das danach
 * an keiner Stelle mehr steht.
 *
 * **Die Aufrufe gehen an ein gestelltes `fetch`** und nicht ins Netz. Das ist
 * hier nicht Bequemlichkeit, sondern die Bedingung dafür, dass die Fälle zur
 * Verdrahtung überhaupt etwas aussagen: Geprüft wird, **ob** und **womit**
 * gerufen wird.
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

let rufe: { pfad: string; koerper: unknown }[] = [];
let navigationen: string[] = [];
const urspruenglicherOrt = window.location;

/**
 * Netz und Navigation gestellt. Beides braucht jeder Block, der ein Formular
 * aufklappt: Die Mandantenauswahl holt beim Einhängen ihre Liste, und ein
 * erfolgreicher Vorgang am eigenen Konto navigiert.
 */
function stelleUmgebung() {
  rufe = [];
  navigationen = [];
  document.cookie = "XSRF-TOKEN=test-token";
  Object.defineProperty(window, "location", {
    configurable: true,
    value: { ...urspruenglicherOrt, replace: (ziel: string) => navigationen.push(ziel) },
  });
  vi.stubGlobal("fetch", (eingabe: RequestInfo | URL, init?: RequestInit) => {
    const pfad = String(eingabe);
    rufe.push({
      pfad,
      koerper: typeof init?.body === "string" ? JSON.parse(init.body) : undefined,
    });
    const rumpf = pfad.includes("/api/mandanten") ? MANDANTEN : zeile();
    return Promise.resolve(
      new Response(JSON.stringify(rumpf), {
        status: 200,
        headers: { "content-type": "application/json" },
      }),
    );
  });
}

/**
 * Nur die **schreibenden** Aufrufe. Ein aufgeklapptes Formular holt beim
 * Einhängen die wählbaren Mandanten — ein lesender Aufruf, der mit der Frage
 * dieser Fälle nichts zu tun hat und sie sonst um genau eins verschöbe.
 */
function schreibrufe() {
  return rufe.filter((ruf) => ruf.pfad.includes("/api/admin/users/"));
}

function knopfMit(behaelter: HTMLElement, beschriftung: string): HTMLButtonElement | undefined {
  return [...behaelter.querySelectorAll("button")].find((knopf) =>
    (knopf.textContent ?? "").includes(beschriftung),
  );
}

/** Lässt die gestellte Antwort ankommen — die Mandantenliste wird geholt, nicht gestellt. */
async function warteAufAntwort() {
  await act(async () => {
    await new Promise((fertig) => setTimeout(fertig, 0));
  });
}

function raeumeUmgebung() {
  vi.unstubAllGlobals();
  Object.defineProperty(window, "location", { configurable: true, value: urspruenglicherOrt });
}

describe("Die Vorwarnung (E19)", () => {
  beforeEach(stelleUmgebung);
  afterEach(raeumeUmgebung);

  /**
   * Die Mutation liegt im Betrieb in der Ansicht und wird durchgereicht — hier
   * legt diese Hülle sie an. Sie ist die kleinste Nachbildung des echten
   * Einhängepunkts und tut sonst nichts.
   */
  function Huelle({ werte }: { werte: Partial<Nutzerzeile> }) {
    return <ZeilenFormular zeile={zeile(werte)} vorgang={useVorgang()} />;
  }

  async function formular(werte: Partial<Nutzerzeile>, angemeldet: string | undefined) {
    return rendere(
      <AngemeldetProvider username={angemeldet}>
        <Huelle werte={werte} />
      </AngemeldetProvider>,
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
      // Und die Vorwarnung hält, was sie verspricht: Der Vorgang hat gerade die
      // eigene Sitzung verworfen (E5), also führt die Oberfläche selbst auf die
      // Anmeldung — statt eine Seite stehen zu lassen, die es nicht mehr gibt.
      expect(navigationen).toEqual(["/anmeldung"]);
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
      // Ein fremdes Konto meldet niemanden ab — die Gegenprobe zur Zeile darüber.
      expect(navigationen).toEqual([]);
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
    const { behaelter, abbauen } = await formular({ tenants: ["VOTG"] }, "jemand-anders");

    try {
      await warteAufAntwort();
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

/**
 * **Die Verdrahtung der Sperre — zwei Fälle, und der zweite ist die Gegenprobe.**
 *
 * Die *Regel* ist eine reine Funktion (`darfOeffnen`, geprüft in
 * `tests/benutzer.test.ts`). Belegt wird hier, dass sie jemand **abfragt**: dass
 * die andere Zeile ihre Schaltfläche wirklich gesperrt bekommt, solange eine
 * offen ist. Ohne diesen Nachweis sähe eine richtige Regel von außen aus wie
 * keine — und der Preis wäre nicht theoretisch: Ein aufgeklapptes Formular hält
 * ein bereits **getipptes Einmalpasswort**, und das steht danach an keiner
 * Stelle mehr.
 */
describe("Solange eine Zeile offen ist", () => {
  beforeEach(stelleUmgebung);
  afterEach(raeumeUmgebung);

  async function ansichtMitZweiZeilen() {
    const speicher = neuerZwischenspeicher();
    speicher.setQueryData(
      ["benutzer", "liste"],
      [zeile({ id: 1, username: "eins" }), zeile({ id: 2, username: "zwei" })],
    );
    return rendere(<BenutzerAnsicht />, speicher);
  }

  function bearbeitenKnoepfe(behaelter: HTMLElement) {
    return [...behaelter.querySelectorAll("tbody button")].filter(
      (knopf) => (knopf.getAttribute("title") ?? "").length > 0,
    ) as HTMLButtonElement[];
  }

  it("ist die Schaltfläche jeder anderen Zeile gesperrt", async () => {
    const { behaelter, abbauen } = await ansichtMitZweiZeilen();

    try {
      const vorher = bearbeitenKnoepfe(behaelter);
      expect(vorher).toHaveLength(2);

      await act(async () => {
        vorher[0].click();
      });
      await warteAufAntwort();

      const nachher = bearbeitenKnoepfe(behaelter);
      // Die offene Zeile behält ihre Schaltfläche — sie ist ihr Weg wieder zu.
      expect(nachher[0].disabled).toBe(false);
      expect(nachher[1].disabled).toBe(true);
      expect(nachher[1].getAttribute("title")).toBe(B.bearbeitenGesperrt);
      // Und unter der Zeile steht wirklich das Formular, nicht nur ein Zustand.
      expect(behaelter.querySelector('input[type="password"]')).not.toBeNull();
    } finally {
      await abbauen();
    }
  });

  it("ist ohne offene Zeile keine gesperrt", async () => {
    const { behaelter, abbauen } = await ansichtMitZweiZeilen();

    try {
      const knoepfe = bearbeitenKnoepfe(behaelter);
      expect(knoepfe).toHaveLength(2);
      expect(knoepfe.every((knopf) => !knopf.disabled)).toBe(true);
      expect(behaelter.querySelector('input[type="password"]')).toBeNull();
    } finally {
      await abbauen();
    }
  });
});
