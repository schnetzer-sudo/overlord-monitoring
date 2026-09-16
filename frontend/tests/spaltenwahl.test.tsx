// @vitest-environment jsdom

import { describe, expect, it } from "vitest";

import type { Nutzerzeile } from "@/features/benutzer/api";
import { BenutzerTabelle } from "@/features/benutzer/components/benutzer-tabelle";
import { BENUTZERTABELLE } from "@/features/benutzer/spalten";
import type { Katalogzeile } from "@/features/katalog/api";
import { KatalogTabelle } from "@/features/katalog/components/katalog-tabelle";
import { KATALOG, KATALOG_BAUFORM } from "@/features/katalog/spalten";
import type { BamTreffer, Nachricht } from "@/features/nachrichten/api";
import { NachrichtenTabelle } from "@/features/nachrichten/components/nachrichten-tabelle";
import { TrefferTabelle } from "@/features/nachrichten/components/treffer-tabelle";
import { NACHRICHTENLISTE } from "@/features/nachrichten/spalten";
import { TREFFERLISTE, TREFFERLISTE_OHNE_TREFFER } from "@/features/nachrichten/treffer-spalten";
import { texteFuer } from "@/i18n";
import {
  PX_JE_REM,
  grundmengeInPixeln,
  mindestbreiteVon,
  schwelleInPixeln,
  sichtbareSpalten,
  stufeVon,
  type Spaltenwahl,
} from "@/lib/spaltenwahl";

import { rendere } from "./hilfe/rendern";

/**
 * **Spaltenwahl nach dem Platz** (`docs/spaltenwahl.md`, E‑147) — je Tabelle ein
 * Test an den gemessenen Containerbreiten.
 *
 * ## Warum ein gerenderter Baum
 *
 * Die Bedingung aus `tests/hilfe/rendern.tsx` ist erfüllt, und zwar in der
 * Klasse, für die es sie gibt: **Die Regel ist selbst eine Klasse.** Welche
 * Spalte ab welcher Containerbreite dasteht, entscheidet `@min-[…]/<name>:table-cell`
 * am `th` **und** an jedem `td` — als wörtliche Zeichenkette, weil Tailwind nur
 * findet, was im Quelltext steht. Die Rechnung dahinter ist eine reine Funktion
 * (`lib/spaltenwahl.ts`); dass die Klasse im Baum dieselbe Zahl trägt, sagt nur
 * der Baum.
 *
 * ## Was geprüft wird, und was davon nur gerechnet ist
 *
 * 1. **Die Schwelle ist hergeleitet und nicht gewählt** — die Zahl in der Klasse
 *    ist auf den Pixel die Summe der Mindestbreiten aller Spalten, die ab dort
 *    sichtbar sind. Eine um einen Pixel verschobene Klasse fällt hier.
 * 2. **An den gemessenen Containerbreiten** (M176: 334, 364, 404, 718, 518 für
 *    Nachrichten- und Trefferliste — für die Liste dazu die 294 px der Spalte
 *    neben dem Baum —, 336, 366, 406, 720, 520 für die beiden Verwaltungstabellen)
 *    und **an jeder Schwellenkante** (ein Pixel darunter, genau darauf): keine
 *    sichtbare Spalte schmaler als ihre Mindestbreite, keine Stufe zu früh und
 *    keine zu spät. Die Mindestbreite enthält die Kopfbeschriftung in beiden
 *    Sprachen und den längsten Zellinhalt — **eine Spalte, die nicht schmaler ist,
 *    lässt ihre Beschriftung nicht über die Nachbarin laufen, und kein Textkasten
 *    ragt aus ihr hinaus.**
 * 3. **`th` und `td` tragen dieselbe Sichtbarkeit.** Verschwinden heißt
 *    `display: none` an beiden (Auftrag 3.3), nie eine Breite von 0 px.
 * 4. **Keine Fensterschwelle an einer Zelle.** `sm:`, `md:`, `lg:` an einer Spalte
 *    waren die Ursache aller vier Befunde aus M176.
 *
 * 5. **Der Überschuss gehört allen Spalten** (E‑149): Hat eine Tabelle keine
 *    freie Spalte, wächst jede um denselben Faktor. Die Benutzertabelle hat seit
 *    dem 16.09.2026 keine — vorher hortete die Mandantenspalte den ganzen Rest.
 *
 * ⚠️ **`jsdom` rechnet kein Layout.** Die Breiten unter 2. und 5. sind nach dem
 * Verfahren von `table-layout: fixed` **gerechnet** — feste Spalten tragen ihre
 * Klasse, die freie bekommt den Rest, und ohne freie teilt sich der Überschuss
 * anteilig. Dass Chrome dasselbe tut, belegt die Gegenprobe im Browser (M177 nach
 * dem Bau, `docs/spaltenwahl.md` §7.2 und §5.5), nicht dieser Test.
 */

const TEXTE = texteFuer("de");

type Gelesen = {
  schluessel: string;
  index: number;
  /** Ab welcher Containerbreite (px) die Spalte dasteht — `null` heißt immer. */
  ab: number | null;
  /** Die Breitenklassen: ab welcher Containerbreite welche Breite (px). */
  breiten: { ab: number; px: number }[];
  klassen: string[];
};

const inPixeln = (rem: string) => Math.round(parseFloat(rem) * PX_JE_REM * 1000) / 1000;

/** Liest die Spaltenklassen eines `th` (oder `td`) — nur das, was die Regel ist. */
function lies(zelle: Element, container: string, schluessel: string, index: number): Gelesen {
  const klassen = [...zelle.classList];
  const sichtbarAb = new RegExp(`^@min-\\[([\\d.]+)rem\\]/${container}:table-cell$`);
  const bedingteBreite = new RegExp(`^@min-\\[([\\d.]+)rem\\]/${container}:w-\\[([\\d.]+)rem\\]$`);
  let ab: number | null = null;
  const breiten: Gelesen["breiten"] = [];
  for (const klasse of klassen) {
    const sichtbar = sichtbarAb.exec(klasse);
    if (sichtbar) ab = inPixeln(sichtbar[1]!);
    const basis = /^w-\[([\d.]+)rem\]$/.exec(klasse);
    if (basis) breiten.push({ ab: 0, px: inPixeln(basis[1]!) });
    const bedingt = bedingteBreite.exec(klasse);
    if (bedingt) breiten.push({ ab: inPixeln(bedingt[1]!), px: inPixeln(bedingt[2]!) });
  }
  if (ab !== null) {
    expect(
      klassen,
      `${container}/${schluessel}: ohne \`hidden\` stünde sie auch darunter da`,
    ).toContain("hidden");
  } else {
    expect(klassen, `${container}/${schluessel}: \`hidden\` ohne Schwelle käme nie`).not.toContain(
      "hidden",
    );
  }
  return { schluessel, index, ab, breiten: breiten.sort((a, b) => a.ab - b.ab), klassen };
}

/** Die Kopfzeile als Spaltenliste, über die Beschriftungen der Sprachdatei. */
function spaltenAus(
  behaelter: HTMLElement,
  container: string,
  beschriftungen: Record<string, string>,
) {
  const koepfe = [...behaelter.querySelectorAll("thead th")];
  return koepfe.map((th, index) => {
    const text = (th.textContent ?? "").trim();
    const schluessel = Object.entries(beschriftungen).find(([, wert]) => wert === text)?.[0];
    expect(schluessel, `Kopf „${text}" gehört zu keiner Spalte von ${container}`).toBeDefined();
    return lies(th, container, schluessel!, index);
  });
}

/**
 * `table-layout: fixed`, gerechnet: feste Spalten tragen ihre Klasse, die freie
 * den Rest — und **ohne sichtbare freie Spalte teilt sich der Überschuss
 * anteilig** auf alle sichtbaren Spalten auf (E‑149).
 *
 * Das ist keine Annahme über den Browser, sondern zweimal gemessen: an der
 * Trefferliste bei 718 px Kasten (192,91 · 159,89 · 288,84 · 76,36 statt
 * 187 · 155 · 280 · 74) und an der Benutzertabelle bei 520 px, wo die freie
 * Spalte ausgeblendet war (103,73 · 94,77 · 135,75 · 124,23 · 61,52 statt
 * 81 · 74 · 106 · 97 · 48) — beide Reihen sind auf ±0,04 px der Mindestbreite
 * mal Kasten durch Spaltensumme (`docs/spaltenwahl.md` §5.5, §7.2).
 */
function layout(spalten: Gelesen[], frei: string | undefined, breite: number) {
  const sichtbar = spalten.filter((s) => s.ab === null || breite >= s.ab);
  const breiteVon = (s: Gelesen) =>
    s.breiten.filter((b) => breite >= b.ab).reduce<number | null>((_, b) => b.px, null);
  const freieSpalte = sichtbar.find((s) => s.schluessel === frei);
  const fest = sichtbar
    .filter((s) => s !== freieSpalte)
    .reduce((summe, s) => summe + (breiteVon(s) ?? 0), 0);
  // Schmaler als die Summe wird keine Spalte: Dort läuft die Tabelle über, statt
  // zu schrumpfen — deshalb der Faktor erst ab 1.
  const faktor = freieSpalte === undefined && fest > 0 ? Math.max(1, breite / fest) : 1;
  const breiten = new Map(
    sichtbar.map((s) => [
      s.schluessel,
      s === freieSpalte ? Math.max(0, breite - fest) : (breiteVon(s) ?? 0) * faktor,
    ]),
  );
  return {
    sichtbar: sichtbar.map((s) => s.schluessel),
    breiten,
    ueberlauf: Math.max(0, fest - breite),
  };
}

/** Die gemessenen Breiten plus jede Kante: ein Pixel darunter und genau darauf. */
function breitenFuer(wahl: Spaltenwahl, gemessen: number[]) {
  const kanten = [
    grundmengeInPixeln(wahl),
    ...wahl.stufen.map((_, i) => schwelleInPixeln(wahl, i)),
  ];
  return [...new Set([...gemessen, ...kanten.flatMap((k) => [k - 1, k])])].sort((a, b) => a - b);
}

/** Die vier Zusagen, die für jede Tabelle gleich sind. */
function pruefeTabelle(
  behaelter: HTMLElement,
  wahl: Spaltenwahl,
  beschriftungen: Record<string, string>,
  gemessen: number[],
  unterDerGrundmenge: (ergebnis: ReturnType<typeof layout>, breite: number) => void,
) {
  const spalten = spaltenAus(behaelter, wahl.container, beschriftungen);

  // 1. Hergeleitet, nicht gewählt.
  // Ohne freie Spalte trägt **jede** eine Breite: Eine Spalte ohne Klasse bekäme
  // sonst wieder den ganzen Überschuss (E‑149) — das war der Befund vom
  // 16.09.2026 an der Mandantenspalte.
  if (wahl.frei === undefined) {
    for (const spalte of spalten) {
      expect(
        spalte.breiten.length,
        `${wahl.container}/${spalte.schluessel}: ohne freie Spalte trägt jede Spalte eine Breite`,
      ).toBeGreaterThan(0);
    }
  }
  for (const spalte of spalten) {
    const stufe = stufeVon(wahl, spalte.schluessel);
    expect(spalte.ab, `${wahl.container}/${spalte.schluessel}: Schwelle`).toBe(
      stufe === null ? null : schwelleInPixeln(wahl, stufe),
    );
    const mindest = mindestbreiteVon(wahl, spalte.schluessel);
    if (spalte.schluessel === wahl.frei) {
      expect(
        spalte.breiten,
        `${wahl.container}/${spalte.schluessel}: die freie Spalte trägt keine Breite`,
      ).toEqual([]);
    } else {
      // Sobald sie dasteht — ab der Grundmenge oder ab ihrer Stufe —, trägt jede
      // feste Spalte genau ihre Mindestbreite (Entscheidung vom 15.09.2026).
      const ab = stufe === null ? grundmengeInPixeln(wahl) : schwelleInPixeln(wahl, stufe);
      expect(
        layout(spalten, wahl.frei, ab).breiten.get(spalte.schluessel),
        `${wahl.container}/${spalte.schluessel}: Breite ab ${ab} px`,
      ).toBe(mindest);
    }
  }

  // 3. und 4. — dieselbe Sichtbarkeit an jeder Zelle, und keine Fensterschwelle.
  const zeilen = [...behaelter.querySelectorAll("tbody tr")].filter(
    (tr) => tr.children.length === spalten.length,
  );
  expect(zeilen.length, `${wahl.container}: ohne Zeilen prüft der Test nichts`).toBeGreaterThan(0);
  for (const spalte of spalten) {
    const regel = spalte.klassen.filter((k) => k === "hidden" || k.endsWith(":table-cell"));
    for (const zeile of zeilen) {
      const td = zeile.children[spalte.index]!;
      const tdRegel = [...td.classList].filter((k) => k === "hidden" || k.endsWith(":table-cell"));
      expect(tdRegel, `${wahl.container}/${spalte.schluessel}: td wie th`).toEqual(regel);
    }
    for (const zelle of [
      spalte.klassen,
      ...zeilen.map((z) => [...z.children[spalte.index]!.classList]),
    ]) {
      expect(
        zelle.filter((k) => /^(sm|md|lg|xl|2xl):/.test(k)),
        `${wahl.container}/${spalte.schluessel}: Fensterschwelle an der Zelle`,
      ).toEqual([]);
    }
  }

  // 2. An den gemessenen Breiten und an jeder Kante.
  for (const breite of breitenFuer(wahl, gemessen)) {
    const ergebnis = layout(spalten, wahl.frei, breite);
    const erwartet = sichtbareSpalten(wahl, breite).map((s) => s.schluessel);
    expect(
      [...ergebnis.sichtbar].sort(),
      `${wahl.container} bei ${breite} px: welche Spalten dastehen`,
    ).toEqual([...erwartet].sort());

    if (breite >= grundmengeInPixeln(wahl)) {
      expect(ergebnis.ueberlauf, `${wahl.container} bei ${breite} px: Überlauf`).toBe(0);
      for (const [schluessel, px] of ergebnis.breiten) {
        expect(
          px,
          `${wahl.container} bei ${breite} px: ${schluessel} unter ihrer Mindestbreite`,
        ).toBeGreaterThanOrEqual(mindestbreiteVon(wahl, schluessel));
      }
    } else {
      unterDerGrundmenge(ergebnis, breite);
    }
  }
}

// ── Die Nachrichtenliste ────────────────────────────────────────────────────

const LISTENZEILE: Nachricht = {
  messageId: "8f3a1c2e-0000-4000-8000-000000000002",
  zeitpunkt: "2025-12-29T22:53:50Z",
  status: "FINISHED",
  statusKind: "ABGESCHLOSSEN",
  bedeutungNichtVerifiziert: false,
  processId: "p-1",
  processName: "Eingehender Lieferschein",
  projectName: "300_KundenEingehend",
  sosName: "Versand Einzel IDOC aus Split",
  schritt: null,
};

const LISTEN_BESCHRIFTUNG = {
  zeitpunkt: TEXTE.nachrichten.spalten.zeitpunkt,
  status: TEXTE.nachrichten.spalten.status,
  ablauf: TEXTE.nachrichten.spalten.ablauf,
  projekt: TEXTE.nachrichten.spalten.projekt,
};

/**
 * M176 §4.3 — die Hülle der Liste bei 360, 390, 430, 744 und 768 px, dazu die
 * **schmale Spalte neben dem Baum** (294 px, §4.6): Dort steht dieselbe Tabelle,
 * und dort ist der Befund aus Punkt 114 entstanden.
 */
const LISTE_GEMESSEN = [294, 334, 364, 404, 718, 518];

describe("Die Nachrichtenliste an den gemessenen Containerbreiten", () => {
  it("Schwelle hergeleitet, keine Spalte unter ihrer Mindestbreite, darunter kürzt der Ablauf", async () => {
    const { behaelter, abbauen } = await rendere(
      <NachrichtenTabelle
        zeilen={[LISTENZEILE]}
        sortierung="neueste"
        aufSortierung={() => {}}
        gewaehlt={null}
        aufAuswahl={() => {}}
      />,
    );
    try {
      pruefeTabelle(
        behaelter,
        NACHRICHTENLISTE,
        LISTEN_BESCHRIFTUNG,
        LISTE_GEMESSEN,
        (ergebnis, breite) => {
          // Die heutige Bauform unter der Grundmenge (Antwort des Auftraggebers
          // vom 15.09.2026): Die drei Spalten stehen, Zeitpunkt und Status
          // behalten ihre Breite, der Ablauf bekommt den Rest und kürzt — mit
          // vollem `title`, wie bei jeder Breite. **Das Projekt kommt nicht.**
          expect(ergebnis.sichtbar.sort(), `nachrichtenliste bei ${breite} px`).toEqual([
            "ablauf",
            "status",
            "zeitpunkt",
          ]);
          const fest = mindestbreiteVon(NACHRICHTENLISTE, "zeitpunkt") + 155;
          expect(ergebnis.breiten.get("ablauf"), `nachrichtenliste bei ${breite} px: Ablauf`).toBe(
            Math.max(0, breite - fest),
          );
          expect(ergebnis.ueberlauf, `nachrichtenliste bei ${breite} px: Überlauf`).toBe(
            Math.max(0, fest - breite),
          );
        },
      );
    } finally {
      await abbauen();
    }
  });
});

// ── Die Trefferliste ────────────────────────────────────────────────────────

const TREFFERZEILE: BamTreffer = {
  messageId: "8f3a1c2e-0000-4000-8000-000000000001",
  zeitpunkt: "2025-12-29T22:53:50Z",
  status: "FINISHED",
  statusKind: "ABGESCHLOSSEN",
  bedeutungNichtVerifiziert: false,
  processId: "p-1",
  processName: "Eingehender Lieferschein",
  projectName: "300_KundenEingehend",
  sosName: "Versand Einzel IDOC aus Split",
  schritt: null,
  rollen: ["SPLIT_WURZEL"],
  treffer: [{ typ: 9014, bezeichnung: "Lieferschein-Nr._L_SAP", wert: "0050" }],
};

const TREFFER_BESCHRIFTUNG = {
  zeitpunkt: TEXTE.nachrichten.spalten.zeitpunkt,
  status: TEXTE.nachrichten.spalten.status,
  treffer: TEXTE.suche.spalten.treffer,
  kette: TEXTE.suche.spalten.kette,
  ablauf: TEXTE.nachrichten.spalten.ablauf,
};

/** M176 §4.7 — die Hülle der Trefferliste bei 360, 390, 430, 744 und 768 px. */
const TREFFER_GEMESSEN = [334, 364, 404, 718, 518];

describe("Die Trefferliste an den gemessenen Containerbreiten", () => {
  it.each([
    ['mit der Spalte „Treffer"', true, TREFFERLISTE],
    ["ohne sie (reine Feldsuche, E‑110)", false, TREFFERLISTE_OHNE_TREFFER],
  ] as const)(
    "%s: Schwelle hergeleitet, keine Spalte unter ihrer Mindestbreite",
    async (_, mitTreffer, wahl) => {
      const { behaelter, abbauen } = await rendere(
        <TrefferTabelle
          zeilen={[TREFFERZEILE]}
          gewaehlt={null}
          aufAuswahl={() => {}}
          mitTrefferspalte={mitTreffer}
        />,
      );
      try {
        pruefeTabelle(
          behaelter,
          wahl,
          TREFFER_BESCHRIFTUNG,
          TREFFER_GEMESSEN,
          (ergebnis, breite) => {
            // Die heutige Bauform: Die Grundmenge steht, und die Tabelle scrollt in
            // ihrer Hülle (property-suche.md §14, Punkt 11). Keine Stufe darunter.
            expect(ergebnis.sichtbar.sort(), `trefferliste bei ${breite} px`).toEqual(
              wahl.grundmenge.map((s) => s.schluessel).sort(),
            );
            expect(ergebnis.ueberlauf, `trefferliste bei ${breite} px: Überlauf`).toBe(
              grundmengeInPixeln(wahl) - breite,
            );
          },
        );
      } finally {
        await abbauen();
      }
    },
  );
});

// ── Die Benutzertabelle ─────────────────────────────────────────────────────

const NUTZERZEILE: Nutzerzeile = {
  id: 7,
  username: "beispielnutzer",
  role: "MANDANT",
  tenants: ["VOTG"],
  locked: false,
  lockedUntil: null,
  active: true,
  mustChangePassword: false,
  lastLogin: null,
  treeLayout: "PARTNER",
};

const BENUTZER_BESCHRIFTUNG = {
  benutzer: TEXTE.benutzer.spalten.benutzer,
  rolle: TEXTE.benutzer.spalten.rolle,
  mandanten: TEXTE.benutzer.spalten.mandanten,
  sperre: TEXTE.benutzer.spalten.sperre,
  zeitsperre: TEXTE.benutzer.spalten.zeitsperre,
  aktiv: TEXTE.benutzer.spalten.aktiv,
  passwort: TEXTE.benutzer.spalten.passwort,
  letzteAnmeldung: TEXTE.benutzer.spalten.letzteAnmeldung,
  aktionen: TEXTE.benutzer.spalten.aktionen,
};

/** M176 §4.8 und §4.9 — der Kasten beider Verwaltungstabellen bei 360, 390, 430, 744 und 768 px. */
const VERWALTUNG_GEMESSEN = [336, 366, 406, 720, 520];

describe("Die Benutzertabelle an den gemessenen Containerbreiten", () => {
  it("Schwellen hergeleitet, keine Spalte unter ihrer Mindestbreite", async () => {
    const { behaelter, abbauen } = await rendere(
      <BenutzerTabelle
        zeilen={[NUTZERZEILE]}
        aktionenFuer={() => <button type="button">x</button>}
      />,
    );
    try {
      pruefeTabelle(
        behaelter,
        BENUTZERTABELLE,
        BENUTZER_BESCHRIFTUNG,
        VERWALTUNG_GEMESSEN,
        (ergebnis, breite) => {
          // Die heutige Bauform: Die Grundmenge steht und läuft über (Entscheidung
          // vom 15.09.2026). Keine Stufe darunter.
          expect(ergebnis.sichtbar.sort(), `benutzertabelle bei ${breite} px`).toEqual(
            BENUTZERTABELLE.grundmenge.map((s) => s.schluessel).sort(),
          );
          expect(ergebnis.ueberlauf).toBe(grundmengeInPixeln(BENUTZERTABELLE) - breite);
        },
      );
    } finally {
      await abbauen();
    }
  });
});

/**
 * **Der Überschuss gehört allen Spalten** (E‑149, 16.09.2026). Der Befund des
 * Auftraggebers am breiten Fenster: Die drei linken Spalten klebten aneinander,
 * rechts davon stand eine Lücke — die Mandantenspalte war die freie und bekam bei
 * 1.408 px Hülle **673 px** für eine Reihe kurzer Marken, während „lschnetzer"
 * auf 81 px und „EDI-Betreuung" auf 89 px zweizeilig umbrachen.
 *
 * Geprüft wird die Aufteilung an der Breite der Meldung, und zwar als **ein
 * Faktor für alle**: Jede Spalte steht auf ihrer Mindestbreite mal Kasten durch
 * Spaltensumme. Eine wiederkehrende freie Spalte fällt hier, ebenso eine
 * fehlende Breitenklasse — beides gäbe wieder einer Spalte den ganzen Rest.
 */
describe("Die Benutzertabelle am breiten Fenster", () => {
  it("teilt den Überschuss anteilig auf alle Spalten, statt ihn einer zu geben", async () => {
    const { behaelter, abbauen } = await rendere(
      <BenutzerTabelle
        zeilen={[NUTZERZEILE]}
        aktionenFuer={() => <button type="button">x</button>}
      />,
    );
    try {
      const spalten = spaltenAus(behaelter, BENUTZERTABELLE.container, BENUTZER_BESCHRIFTUNG);
      /** Die Hülle auf der eigenen Route bei 1.440 px Fenster — die Breite der Meldung. */
      const kasten = 1408;
      const ergebnis = layout(spalten, BENUTZERTABELLE.frei, kasten);

      // Alle neun stehen da, und zusammen füllen sie den Kasten genau aus.
      expect(ergebnis.sichtbar).toHaveLength(9);
      const summe = [...ergebnis.breiten.values()].reduce((gesamt, px) => gesamt + px, 0);
      expect(summe, `die Spalten füllen die ${kasten} px des Kastens`).toBeCloseTo(kasten, 6);

      // **Ein Faktor für alle** — das ist die Zusage. Die Spaltensumme ist die
      // letzte Schwelle: Dort trägt jede Spalte genau ihre Mindestbreite.
      const faktor = kasten / schwelleInPixeln(BENUTZERTABELLE, 1);
      for (const [schluessel, px] of ergebnis.breiten) {
        expect(px, `benutzertabelle bei ${kasten} px: ${schluessel} anteilig`).toBeCloseTo(
          mindestbreiteVon(BENUTZERTABELLE, schluessel) * faktor,
          6,
        );
      }

      // Die zwei Zahlen des Befunds, gegengeprüft: Die Mandanten horten nicht
      // mehr, und der Benutzername bekommt Platz für eine Zeile.
      expect(Math.round(ergebnis.breiten.get("mandanten")!), "Mandanten, vorher 673 px").toBe(163);
      expect(Math.round(ergebnis.breiten.get("benutzer")!), "Benutzer, vorher 81 px").toBe(137);
    } finally {
      await abbauen();
    }
  });
});

// ── Der Prozess-Katalog ─────────────────────────────────────────────────────

const KATALOGZEILE: Katalogzeile = {
  processId: "40000_AMG_LAB_VDA",
  projectId: "300_KundenEingehend",
  projectName: "Kunden eingehend",
  processName: "Eingehender IFTMIN",
  partner: null,
  richtung: null,
  pflegestatus: "OFFEN",
  vorschlagHerkunft: "KEINE",
  traegtNachrichten: null,
  bestandGeprueftAm: null,
};

const KATALOG_BESCHRIFTUNG = {
  prozess: TEXTE.katalog.spalten.prozess,
  projekt: TEXTE.katalog.spalten.projekt,
  partner: TEXTE.katalog.spalten.partner,
  richtung: TEXTE.katalog.spalten.richtung,
  bestand: TEXTE.katalog.spalten.bestand,
  pflege: TEXTE.katalog.spalten.pflege,
};

describe("Der Prozess-Katalog an den gemessenen Containerbreiten", () => {
  it("Schwellen hergeleitet, keine Spalte unter ihrer Mindestbreite, darunter die dreispaltige Bauform", async () => {
    const { behaelter, abbauen } = await rendere(
      <KatalogTabelle
        zeilen={[KATALOGZEILE]}
        offeneZeile={null}
        aufOeffnen={() => undefined}
        aufSchliessen={() => undefined}
        vorschlaege={[]}
      />,
    );
    try {
      pruefeTabelle(
        behaelter,
        KATALOG,
        KATALOG_BESCHRIFTUNG,
        VERWALTUNG_GEMESSEN,
        (ergebnis, breite) => {
          // Die Bauform bei 360 px (Auftrag §4: „seine Bauform und kein Fehler"):
          // drei Spalten, Prozess und Pflege in ihrer heutigen Breite, Partner der
          // Rest — und der Rest ist nie null.
          expect(ergebnis.sichtbar.sort(), `katalog bei ${breite} px`).toEqual([
            "partner",
            "pflege",
            "prozess",
          ]);
          expect(ergebnis.breiten.get("prozess")).toBe(KATALOG_BAUFORM.prozess);
          expect(ergebnis.breiten.get("pflege")).toBe(KATALOG_BAUFORM.pflege);
          expect(
            ergebnis.breiten.get("partner"),
            `katalog bei ${breite} px: Partner`,
          ).toBeGreaterThan(0);
          expect(ergebnis.ueberlauf).toBe(0);
        },
      );
    } finally {
      await abbauen();
    }
  });
});
