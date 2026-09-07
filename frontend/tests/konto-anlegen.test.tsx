// @vitest-environment jsdom

import { act } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { BENUTZER_SCHLUESSEL, type Nutzerzeile } from "@/features/benutzer/api";
import { BenutzerAnsicht } from "@/features/benutzer/components/benutzer-ansicht";
import { texteFuer } from "@/i18n";

import { neuerZwischenspeicher, rendere } from "./hilfe/rendern";

/**
 * **Sechs Fälle, für die ein gerenderter Baum die einzige Prüfung ist** (9c).
 *
 * Die Bedingung steht in `tests/hilfe/rendern.tsx`: nicht „ein Baum wäre
 * bequemer", sondern **„es gibt keinen anderen Ort, an dem der Satz belegbar
 * wäre"**. Die *Regeln* dieser Maske sind reine Funktionen und stehen in
 * `tests/benutzer.test.ts` — `anfrageAus` prüft den Entwurf, `darfOeffnen`
 * entscheidet die Sperre. Was dort grundsätzlich nicht steht, ist, ob jemand sie
 * **abfragt**, und was nach der Antwort passiert. Genau das steht hier, in drei
 * Klassen:
 *
 * 1. **Die Verdrahtung der Sperre, in beide Richtungen** (E22), je mit
 *    Gegenprobe im selben Fall: vor dem Aufklappen frei, danach gesperrt. Ohne
 *    die Gegenprobe bewiese der Test nur, dass irgendein Knopf gesperrt ist —
 *    und eine richtige Regel, die niemand abfragt, sieht von außen aus wie
 *    keine. Der Einsatz ist derselbe wie an der Zeile: ein bereits **getipptes
 *    Einmalpasswort**, das danach an keiner Stelle mehr steht.
 * 2. **Was nach dem Erfolg passiert** (E24, E25). Dass die Liste **neu geholt**
 *    und der Zwischenspeicher **nicht gesetzt** wird, ist eine Aussage über die
 *    Aufrufe auf der Leitung und über den Inhalt des Speichers — an einer reinen
 *    Funktion gäbe es dazu nichts. Die naheliegende Abkürzung wäre, die neun
 *    Felder der Zeile aus den vier der Antwort zu ergänzen; sie bestünde jede
 *    Prüfung, die nur auf den Bildschirm sieht. Und dass das **Passwortfeld
 *    leer** ist, entsteht erst im Feld.
 * 3. **Dass die Fehlermeldung im Formular steht und die Maske offen bleibt.**
 *    Eine Aussage über den Ort einer Meldung — schlösse sich die Maske, wäre ein
 *    `409` nirgends zu sehen und der Bildschirm sähe aus, als sei nichts
 *    passiert.
 *
 * **Die Aufrufe gehen an ein gestelltes `fetch`** und nicht ins Netz. Das ist
 * die Bedingung dafür, dass die Fälle überhaupt etwas aussagen: Geprüft wird,
 * **ob**, **womit** und **in welcher Reihenfolge** gerufen wird.
 */

const TEXTE = texteFuer("de");
const A = TEXTE.benutzer.anlegen;

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

/** Die Liste, wie sie vor **und** nach dem Anlegen vom Backend käme. */
const BESTAND = [zeile({ id: 1, username: "eins" }), zeile({ id: 2, username: "zwei" })];

type Ruf = { pfad: string; methode: string; koerper?: unknown };

let rufe: Ruf[] = [];
/** Antwort auf den nächsten `POST /api/admin/users` — Erfolg, falls nicht gesetzt. */
let anlegeAntwort: Response | null = null;
/**
 * Solange gesetzt, bleibt der `POST` **hängen**: Der Aufruf läuft, und genau in
 * diesem Zustand wird geprüft, dass sich die Maske nicht zuklappen lässt.
 */
let loesePost: ((antwort: Response) => void) | null = null;

function stelleUmgebung() {
  rufe = [];
  anlegeAntwort = null;
  loesePost = null;
  haengen = false;
  document.cookie = "XSRF-TOKEN=test-token";
  vi.stubGlobal("fetch", (eingabe: RequestInfo | URL, init?: RequestInit) => {
    const pfad = String(eingabe);
    const methode = init?.method ?? "GET";
    rufe.push({
      pfad,
      methode,
      koerper: typeof init?.body === "string" ? JSON.parse(init.body) : undefined,
    });

    if (pfad.includes("/api/mandanten")) {
      return Promise.resolve(antwort(MANDANTEN));
    }
    if (methode === "POST" && haengen) {
      return new Promise<Response>((fertig) => {
        loesePost = fertig;
      });
    }
    if (methode === "POST") {
      return Promise.resolve(anlegeAntwort ?? erfolg());
    }
    // `GET /api/admin/users` — **derselbe Bestand wie vorher.** Das ist die
    // Bedingung für den E24-Fall: Taucht das neue Konto trotzdem im
    // Zwischenspeicher auf, kann es nur aus der Antwort des `POST`
    // zusammengesetzt worden sein.
    return Promise.resolve(antwort(BESTAND));
  });
}

/** Ob der nächste `POST` hängen bleibt, statt sofort zu antworten. */
let haengen = false;

function erfolg(): Response {
  return antwort({ id: 9, username: "wegwerf.neun", role: "MANDANT", mandantId: "VOTG" }, 201);
}

function antwort(rumpf: unknown, status = 200): Response {
  return new Response(JSON.stringify(rumpf), {
    status,
    headers: { "content-type": "application/json" },
  });
}

function raeumeUmgebung() {
  vi.unstubAllGlobals();
}

/** Lässt die gestellten Antworten ankommen. */
async function warte() {
  await act(async () => {
    await new Promise((fertig) => setTimeout(fertig, 0));
  });
}

async function ansicht() {
  const speicher = neuerZwischenspeicher();
  speicher.setQueryData(BENUTZER_SCHLUESSEL.liste, BESTAND);
  const gerendert = await rendere(<BenutzerAnsicht />, speicher);
  await warte();
  return gerendert;
}

/** Der Knopf über der Liste, der die Maske auf- und zuklappt. */
function anlegenKnopf(behaelter: HTMLElement): HTMLButtonElement {
  const knopf = [...behaelter.querySelectorAll("button")].find((k) =>
    (k.textContent ?? "").includes(A.oeffnen),
  );
  expect(knopf, "der Anlegen-Knopf fehlt").toBeDefined();
  return knopf as HTMLButtonElement;
}

/** Die Bearbeiten-Schaltflächen der Zeilen — dieselbe Abfrage wie in `benutzer-tabelle.test.tsx`. */
function zeilenKnoepfe(behaelter: HTMLElement): HTMLButtonElement[] {
  return [...behaelter.querySelectorAll("tbody button")].filter(
    (knopf) => (knopf.getAttribute("title") ?? "").length > 0,
  ) as HTMLButtonElement[];
}

async function klick(knopf: HTMLElement | null | undefined) {
  await act(async () => {
    (knopf as HTMLButtonElement | null)?.click();
  });
  await warte();
}

/** Die Anlegemaske, an ihrer Überschrift erkannt — `null`, wenn sie zu ist. */
function maske(behaelter: HTMLElement): HTMLFormElement | null {
  return (
    [...behaelter.querySelectorAll("form")].find((form) =>
      (form.textContent ?? "").includes(A.titel),
    ) ?? null
  );
}

function feld(behaelter: HTMLElement, kennzeichnung: string): HTMLInputElement {
  const beschriftung = [...behaelter.querySelectorAll("label")].find(
    (label) => (label.textContent ?? "").trim() === kennzeichnung,
  );
  const eingabe = behaelter.querySelector<HTMLInputElement>(
    `#${CSS.escape(beschriftung?.getAttribute("for") ?? "")}`,
  );
  expect(eingabe, `Feld „${kennzeichnung}" fehlt`).not.toBeNull();
  return eingabe as HTMLInputElement;
}

/** React hört auf den nativen Setter, nicht auf `value =`. */
async function tippe(eingabe: HTMLElement, wert: string) {
  await act(async () => {
    const setzer = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, "value")?.set;
    setzer?.call(eingabe, wert);
    eingabe.dispatchEvent(new Event("input", { bubbles: true }));
  });
}

async function waehle(behaelter: HTMLElement, kennzeichnung: string, wert: string) {
  const beschriftung = [...behaelter.querySelectorAll("label")].find(
    (label) => (label.textContent ?? "").trim() === kennzeichnung,
  );
  const auswahl = behaelter.querySelector<HTMLSelectElement>(
    `#${CSS.escape(beschriftung?.getAttribute("for") ?? "")}`,
  );
  expect(auswahl, `Auswahl „${kennzeichnung}" fehlt`).not.toBeNull();
  await act(async () => {
    const setzer = Object.getOwnPropertyDescriptor(
      window.HTMLSelectElement.prototype,
      "value",
    )?.set;
    setzer?.call(auswahl, wert);
    auswahl?.dispatchEvent(new Event("change", { bubbles: true }));
  });
}

/** Die Maske vollständig ausfüllen — vier Felder, sonst geht der Knopf nicht auf. */
async function fuelleAus(behaelter: HTMLElement) {
  await tippe(feld(behaelter, A.benutzername), "wegwerf.neun");
  await waehle(behaelter, A.rolle, "MANDANT");
  await waehle(behaelter, A.mandant, "VOTG");
  await tippe(feld(behaelter, A.passwort), "zwoelfzeichen");
}

async function schickeAb(behaelter: HTMLElement) {
  await act(async () => {
    maske(behaelter)?.dispatchEvent(new Event("submit", { bubbles: true, cancelable: true }));
  });
  await warte();
}

describe("Die Sperre gilt in beide Richtungen (E22)", () => {
  beforeEach(stelleUmgebung);
  afterEach(raeumeUmgebung);

  it("sperrt jede Zeile, solange die Maske offen ist", async () => {
    const { behaelter, abbauen } = await ansicht();

    try {
      // Gegenprobe im selben Fall: Ohne offene Maske ist keine Zeile gesperrt.
      const vorher = zeilenKnoepfe(behaelter);
      expect(vorher).toHaveLength(2);
      expect(vorher.every((knopf) => !knopf.disabled)).toBe(true);

      await klick(anlegenKnopf(behaelter));
      expect(maske(behaelter)).not.toBeNull();

      const nachher = zeilenKnoepfe(behaelter);
      expect(nachher.every((knopf) => knopf.disabled)).toBe(true);
      // Und mit dem Satz, der sagt, warum — demselben wie zwischen zwei Zeilen.
      expect(nachher[0].getAttribute("title")).toBe(TEXTE.benutzer.bearbeitenGesperrt);
    } finally {
      await abbauen();
    }
  });

  it("sperrt die Maske, solange eine Zeile offen ist", async () => {
    const { behaelter, abbauen } = await ansicht();

    try {
      // Gegenprobe im selben Fall: Ohne offene Zeile lässt sich die Maske öffnen.
      expect(anlegenKnopf(behaelter).disabled).toBe(false);

      await klick(zeilenKnoepfe(behaelter)[0]);
      // Die Zeile ist wirklich offen — sonst bewiese der Rest nichts.
      expect(behaelter.querySelector('input[type="password"]')).not.toBeNull();

      expect(anlegenKnopf(behaelter).disabled).toBe(true);
      expect(anlegenKnopf(behaelter).getAttribute("title")).toBe(A.gesperrt);
      expect(maske(behaelter)).toBeNull();
    } finally {
      await abbauen();
    }
  });
});

describe("Nach dem Anlegen", () => {
  beforeEach(stelleUmgebung);
  afterEach(raeumeUmgebung);

  /**
   * **E24 in beiden Hälften.** Erstens geht wirklich ein `GET /api/admin/users`
   * hinaus, und zwar **nach** dem `POST`. Zweitens steht danach im
   * Zwischenspeicher genau das, was dieser `GET` geliefert hat — der gestellte
   * Bestand ohne das neue Konto. Stünde es darin, wäre es aus den vier Feldern
   * der `POST`-Antwort zusammengesetzt worden, und genau das ist die Abkürzung,
   * die E24 verbietet: In den Zwischenspeicher zu schreiben, was der Server
   * nicht gesagt hat.
   */
  it("wird die Liste neu geholt und der Zwischenspeicher nicht gesetzt", async () => {
    const { behaelter, zwischenspeicher, abbauen } = await ansicht();

    try {
      await klick(anlegenKnopf(behaelter));
      await fuelleAus(behaelter);
      await schickeAb(behaelter);

      const schreibend = rufe.findIndex((ruf) => ruf.methode === "POST");
      expect(schreibend, "kein POST gegangen").toBeGreaterThanOrEqual(0);
      expect(rufe[schreibend].pfad).toContain("/api/admin/users");
      expect(rufe[schreibend].koerper).toEqual({
        username: "wegwerf.neun",
        role: "MANDANT",
        mandantId: "VOTG",
        initialPassword: "zwoelfzeichen",
      });

      const nachgeholt = rufe
        .slice(schreibend + 1)
        .filter((ruf) => ruf.methode === "GET" && ruf.pfad.endsWith("/api/admin/users"));
      expect(nachgeholt).toHaveLength(1);

      const liste = zwischenspeicher.getQueryData<Nutzerzeile[]>(BENUTZER_SCHLUESSEL.liste);
      expect(liste).toHaveLength(BESTAND.length);
      expect(liste?.some((eintrag) => eintrag.username === "wegwerf.neun")).toBe(false);
    } finally {
      await abbauen();
    }
  });

  /**
   * **Solange der Aufruf läuft, lässt sich die Maske nicht zuklappen** — aus
   * demselben Grund, aus dem sich eine Zeile währenddessen nicht schließen
   * lässt: Die Meldung steht *im* Formular, und verschwände es mitten im Aufruf,
   * wäre ein `409` nirgends zu sehen. Der Fall ist nur an einem Baum belegbar;
   * er hängt an einem Zwischenzustand, den keine reine Funktion kennt.
   */
  it("lässt sich die Maske nicht zuklappen, solange der Aufruf läuft", async () => {
    const { behaelter, abbauen } = await ansicht();

    try {
      await klick(anlegenKnopf(behaelter));
      await fuelleAus(behaelter);
      haengen = true;
      await schickeAb(behaelter);

      // Der Aufruf ist unterwegs, die Antwort steht aus.
      expect(rufe.some((ruf) => ruf.methode === "POST")).toBe(true);
      expect(anlegenKnopf(behaelter).disabled).toBe(true);
      expect(maske(behaelter)).not.toBeNull();
      // Auch der Absendeknopf wartet — zwei gleichzeitige POST legten zwei Konten an.
      expect(
        maske(behaelter)?.querySelector<HTMLButtonElement>('button[type="submit"]')?.disabled,
      ).toBe(true);

      await act(async () => {
        loesePost?.(erfolg());
      });
      await warte();
      expect(anlegenKnopf(behaelter).disabled).toBe(false);
    } finally {
      await abbauen();
    }
  });

  /**
   * **E25.** Die Maske bleibt offen, geleert, mit der Meldung darin — und das
   * Passwortfeld ist der Teil, auf den es ankommt: Es steht danach an keiner
   * Stelle mehr, auch nicht im Protokoll.
   */
  it("bleibt die Maske offen und geleert — das Passwortfeld zuerst", async () => {
    const { behaelter, abbauen } = await ansicht();

    try {
      await klick(anlegenKnopf(behaelter));
      await fuelleAus(behaelter);
      expect(feld(behaelter, A.passwort).value).toBe("zwoelfzeichen");

      await schickeAb(behaelter);

      expect(maske(behaelter), "die Maske hat sich geschlossen").not.toBeNull();
      expect(feld(behaelter, A.passwort).value).toBe("");
      expect(feld(behaelter, A.benutzername).value).toBe("");
      expect(maske(behaelter)?.textContent ?? "").toContain(A.erfolgTitel);
      // Die Meldung nennt das angelegte Konto und nicht nur, dass es geklappt hat.
      expect(maske(behaelter)?.textContent ?? "").toContain("wegwerf.neun");
    } finally {
      await abbauen();
    }
  });

  /**
   * **Die Meldung steht im Formular** — und deshalb darf sich das Formular nicht
   * schließen. Schlösse es sich, wäre ein `409` nirgends zu sehen, und der
   * Bildschirm sähe aus, als sei nichts passiert.
   */
  it("steht ein 409 im Formular, und die Maske bleibt offen", async () => {
    const { behaelter, abbauen } = await ansicht();
    anlegeAntwort = new Response(
      JSON.stringify({
        type: "https://overlord.example/problem/benutzername-vergeben",
        title: "Benutzername vergeben",
        status: 409,
        detail: "Diesen Benutzernamen gibt es bereits. Waehle einen anderen.",
      }),
      { status: 409, headers: { "content-type": "application/problem+json" } },
    );

    try {
      await klick(anlegenKnopf(behaelter));
      await fuelleAus(behaelter);
      await schickeAb(behaelter);

      const formular = maske(behaelter);
      expect(formular, "die Maske hat sich geschlossen").not.toBeNull();
      // Übersetzt nach `type` und nicht aus `detail` übernommen.
      expect(formular?.textContent ?? "").toContain(TEXTE.fehler["benutzername-vergeben"]);
      expect(formular?.textContent ?? "").not.toContain("Waehle einen anderen.");
      // Der Entwurf bleibt stehen: Der Nutzer soll den Namen ändern und nicht
      // alles noch einmal tippen.
      expect(feld(behaelter, A.benutzername).value).toBe("wegwerf.neun");
      // Und die Liste ist **nicht** neu geholt worden — es gibt nichts Neues.
      expect(
        rufe.filter((ruf) => ruf.methode === "GET" && ruf.pfad.endsWith("/api/admin/users")),
      ).toHaveLength(0);
    } finally {
      await abbauen();
    }
  });
});
