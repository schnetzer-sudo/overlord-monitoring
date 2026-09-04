import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";

import { describe, expect, it } from "vitest";

import { stilblatt, type Regel } from "./hilfe/css-leser";

/**
 * Die Farbwerte in `app/globals.css` — **erstmals von einem Test gehalten.**
 *
 * `docs/visuelles-konzept.md` §8 sagt: *„Nachrechnen ist wörtlich gemeint."*
 * Gerechnet wurde bisher von Hand, in `scripts/farbwerte/rechne.mjs`, und das
 * Ergebnis stand in einer Dokumentationsdatei. Dieser Test macht daraus eine
 * Zusicherung: Wer einen Wert in `globals.css` so ändert, dass eine Schrift
 * unlesbar wird, bekommt einen roten Lauf.
 *
 * Er liest die **Datei** und nicht ein Abbild davon — dieselbe Bauform wie
 * `tests/dichte.test.ts`, mit demselben CSS-Leser (`tests/hilfe/css-leser.ts`).
 * **Es steht keine zweite Liste hier:** Die Tokens und ihre Werte kommen aus
 * `globals.css`, aus diesem Test kommen nur die Schwellen.
 *
 * ## ⚠️ Was er NICHT kann — und das ist keine Fußnote
 *
 * **Er hält Kontraste, nicht Farbwerte.** Der überlebende Mutant ist **gemessen
 * und nicht ausgedacht**: Im Dunkelblock `--status-abgeschlossen` von
 * `oklch(0.75 0.13 166)` auf `oklch(0.75 0.13 27)` — **Grün wird Rot**, gleiche
 * Helligkeit, gleiche Chroma — und der Lauf bleibt grün. Im hellen Block gilt
 * dasselbe für die beiden **neutralen** Rollen. Die fachliche Aussage
 * *„Rot heißt Fehler"* (`visuelles-konzept.md` §3) ist damit weiterhin ungeprüft.
 *
 * **Was dabei doch hält, ist ein Nebenertrag der Gegenproben:** Ein Tonwechsel an
 * `--status-fehler` fällt in beiden Blöcken, weil die vierzehn Gegenproben das
 * helle Rot über seinen OKLab-Abstand zum Akzent festnageln; `--ueberfaellig`
 * fällt an den sechs Hexwerten aus §7a. Die Umfärbbarkeit endet dort, wo eine
 * Gegenprobe hinreicht — nicht dort, wo jemand sie gezogen hätte.
 *
 * Das ist **T‑10 zur Hälfte**, nicht T‑10 geschlossen
 * (`docs/testfestigkeit.md` §6). Die andere Hälfte bräuchte einen Sollwert je
 * Token — und das wäre die zweite Pflegestelle, die T‑10 ausdrücklich als Grund
 * dafür nennt, dass dort bisher nichts stand. Dass sie ungeschlossen bleibt,
 * ist eine Entscheidung und kein Rest.
 *
 * **Ein Wirkungstest im Browser ist er ebenso wenig.** Er liest eine Datei und
 * rechnet. Was die Werte am Bildschirm tun, steht gemessen in
 * `docs/dunkelmodus.md` §6 (die Sichtprobe) und nirgendwo sonst.
 *
 * **Und er sagt nichts darüber, ob der Dunkelzustand erreichbar ist.** Er war
 * es bis zum 03.09.2026 nicht (`docs/dunkelmodus.md` §1); seit Schritt 11b ist
 * er es — `data-thema` am Wurzelelement, drei Werte, Umschalter im Nutzermenü.
 * Was diesen Test daran ändert, ist **nichts**: Er hat die Werte gehalten, als
 * sie niemand sah, und hält dieselben, seit sie jemand sieht. Genau dafür war
 * er da.
 *
 * **Die Verdrahtung des Umschalters steht in `tests/thema.test.ts`** und nicht
 * hier: dass beide Dunkelzweige dieselbe Tokenmenge umzeigen, dass
 * `@custom-variant dark` beide trifft, dass `color-scheme` in beiden steht.
 * Hier stehen die Zahlen, dort steht der Weg.
 *
 * ## Wie gerechnet wird
 *
 * Wortgleich zu `scripts/farbwerte/rechne.mjs` und zu `visuelles-konzept.md`
 * §7a („Wie gerechnet worden ist"):
 *
 * - Der Kontrast entsteht aus den **ungerundeten** sRGB-Fließkommawerten, nicht
 *   aus dem 8‑Bit-Hexwert. Nur diese Methode gibt die Zahlen aus §3 wieder.
 * - Neben jedem Wert steht der aus dem gerundeten Hexwert; **beide** müssen die
 *   Schwelle halten. Ein Wert, der ungerundet knapp darüber und auf dem
 *   Bildschirm knapp darunter liegt, ist keiner.
 * - Der OKLab-Abstand ist der euklidische Abstand in (L, a, b) **einschließlich
 *   der Helligkeit**.
 *
 * **Die Rechnung steht hier ein zweites Mal**, und das ist Absicht: Das Skript
 * läuft von Hand, dieser Test bei jedem `pnpm test`. Verbunden sind die beiden
 * Fassungen über die **vierzehn Gegenproben** unten — acht bekannte Zahlen aus
 * §3 und die sechs Hexwerte aus §7a, die Lightning CSS beim Bauen unabhängig
 * bestätigt hat. Weicht eine ab, ist die Methode und nicht das Ergebnis die
 * offene Frage.
 */

const CSS_ROH = readFileSync(
  fileURLToPath(new URL("../src/app/globals.css", import.meta.url)),
  "utf8",
);

const { regeln: REGELN, wert } = stilblatt(CSS_ROH);

/** Der Textkontrast aus WCAG 1.4.3 für Fließtext. */
const SCHWELLE_TEXT = 4.5;

/** Der Umriss-Kontrast aus WCAG 1.4.11 — **berichtet, nicht zugesichert**. */
const SCHWELLE_UMRISS = 3;

/** Die fünf Farbrollen: vier Statusrollen und die Problemkategorie. */
const ROLLEN = [
  "status-abgeschlossen",
  "status-fehler",
  "status-offen",
  "status-ungeklaert",
  "ueberfaellig",
] as const;

/** Die drei Werte, die jede Rolle hat. Ein vierter wird nirgends erfunden (§3). */
const TEILE = ["", "-flaeche", "-kontur"] as const;

/**
 * Die vier Akzentstufen. Sie sind **Gegenstand** der Rechnung, die
 * shadcn-Basistokens darunter nur ihr **Bezug** (E‑61).
 */
const AKZENTSTUFEN = ["akzent", "akzent-schrift", "akzent-vordergrund", "akzent-flaeche"] as const;

/**
 * Die zwei Stufen der **Verlaufsfläche** (E‑87, 04.09.2026).
 *
 * Sie sind **keine Rolle** im Sinne von {@link ROLLEN}: Sie tragen keine fachliche
 * Aussage, stehen nicht in `lib/status-farbe.ts` und färben keinen Status. Genau
 * deshalb werden sie geprüft — eine Fläche ohne Bedeutung darf nicht aussehen wie
 * eine mit, und sie ist die **größte** Fläche der Übersichtsseite.
 */
const VERLAUFSSTUFEN = ["verlauf-flaeche", "verlauf-kontur"] as const;

/**
 * Die Untergrenze für „unterscheidbar" — **gemessen und nicht gewählt**.
 *
 * `visuelles-konzept.md` §7a hält aus der Sichtprobe A.2 fest, dass **0,025** in
 * OKLab als **Rangfolge** gelesen worden sind. Was darüber liegt, ist sichtbar
 * verschieden. Die Zahl ist eine Schranke und kein Ziel.
 */
const SICHTPROBE = 0.025;

/**
 * Die engste Strecke des Bestands: `--ueberfaellig` → `--akzent` im Dunkelblock
 * (§7a, Befund 3). **Ungerundet**, aus demselben Grund, aus dem
 * `scripts/farbwerte/rechne.mjs` sie ungerundet führt: Gegen die gerundete Zahl
 * geprüft fiele der unveränderte Bestand an seiner eigenen Untergrenze durch.
 */
const ENGSTE_STRECKE = 0.1169659;

// ───────────────────────────────────────────────────────────────────────────
// OKLCH → OKLab → lineares sRGB → sRGB → WCAG
// ───────────────────────────────────────────────────────────────────────────

type Farbe = { L: number; C: number; h: number };
type Lab = { L: number; a: number; b: number };

function oklab({ L, C, h }: Farbe): Lab {
  const r = (h * Math.PI) / 180;
  return { L, a: C * Math.cos(r), b: C * Math.sin(r) };
}

function linear({ L, a, b }: Lab): number[] {
  const l = (L + 0.3963377774 * a + 0.2158037573 * b) ** 3;
  const m = (L - 0.1055613458 * a - 0.0638541728 * b) ** 3;
  const s = (L - 0.0894841775 * a - 1.291485548 * b) ** 3;
  return [
    4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
    -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
    -0.0041960863 * l - 0.7034186147 * m + 1.707614701 * s,
  ];
}

const gamma = (c: number): number => (c <= 0.0031308 ? 12.92 * c : 1.055 * c ** (1 / 2.4) - 0.055);
const entgamma = (c: number): number => (c <= 0.04045 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4);

type Srgb = { roh: number[]; acht: number[]; hex: string; ueberschuss: number };

function srgb(farbe: Farbe): Srgb {
  const roh = linear(oklab(farbe)).map(gamma);
  const acht = roh.map((c) => Math.round(Math.min(1, Math.max(0, c)) * 255));
  return {
    roh,
    acht,
    hex: "#" + acht.map((v) => v.toString(16).padStart(2, "0")).join(""),
    // Wie weit der Wert aus dem Farbraum herausragt, in Kanalanteilen. Null
    // heißt: er liegt darin. Gemessen und nicht als ja/nein geführt, damit ein
    // bekannter Austritt festgenagelt werden kann, statt übersprungen zu werden.
    ueberschuss: Math.max(0, ...roh.map((c) => c - 1), ...roh.map((c) => -c)),
  };
}

const luminanzRoh = (roh: number[]): number => {
  const [r, g, b] = roh.map((c) => entgamma(Math.min(1, Math.max(0, c))));
  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
};
const luminanzAcht = (acht: number[]): number => {
  const [r, g, b] = acht.map((v) => entgamma(v / 255));
  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
};

const verhaeltnis = (a: number, b: number): number =>
  (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05);

/** WCAG-Kontrast: ungerundet gerechnet, daneben der aus dem Hexwert. */
function kontrast(x: Farbe, y: Farbe): { roh: number; acht: number } {
  return {
    roh: verhaeltnis(luminanzRoh(srgb(x).roh), luminanzRoh(srgb(y).roh)),
    acht: verhaeltnis(luminanzAcht(srgb(x).acht), luminanzAcht(srgb(y).acht)),
  };
}

/** Euklidischer Abstand in OKLab, **einschließlich** der Helligkeit. */
function abstand(x: Farbe, y: Farbe): number {
  const a = oklab(x);
  const b = oklab(y);
  return Math.hypot(a.L - b.L, a.a - b.a, a.b - b.b);
}

/** Lineares sRGB zurück nach OKLCH — für alles, was **vermischt** entsteht. */
function nachOklch(lin: number[]): Farbe {
  const [r, g, b] = lin;
  const l = Math.cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b);
  const m = Math.cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b);
  const s = Math.cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b);
  const L = 0.2104542553 * l + 0.793617785 * m - 0.0040720468 * s;
  const A = 1.9779984951 * l - 2.428592205 * m + 0.4505937099 * s;
  const B = 0.0259040371 * l + 0.7827717662 * m - 0.808675766 * s;
  return { L, C: Math.hypot(A, B), h: ((Math.atan2(B, A) * 180) / Math.PI + 360) % 360 };
}

/**
 * `vorne` mit der Deckung `alpha` über `hinten`.
 *
 * **Gemischt wird in sRGB und nicht im linearen Licht** — gemessen und nicht
 * gewählt. Der erste Bau mischte im linearen Licht; das ist die physikalisch
 * richtige Art, Licht zu addieren, und es ist **nicht**, was der Browser tut:
 * `color-interpolation` steht in SVG auf `sRGB`, und die Alphamischung läuft im
 * gammakodierten Raum.
 *
 * Am 04.09.2026 in Chrome an drei Proben nachgesehen (SVG über `data:`-URL auf
 * ein Canvas, Pixel ausgelesen):
 *
 * | | gemessen | sRGB | linear |
 * |---|---|---|---|
 * | `#1992bf` zu 28 % über `#181818` | `#183a46` | `#183a47` | `#18536d` |
 * | `#1992bf` zu 35 % über `#ffffff` | `#afd9e9` | `#afd9e9` | `#d3e1eb` |
 * | `#ffffff` zu 12 % über `#181818` | `#343434` | `#343434` | `#646464` |
 *
 * Der Unterschied ist keine Feinheit: Beim dritten Paar liegen die beiden
 * Rechenwege **0,19** in OKLab auseinander — mehr als das Siebenfache der
 * Sichtprobe.
 *
 * **Das ist der Unterschied zwischen dem Token und dem Bild.** Die
 * Verlaufsfläche steht in `globals.css` als volle Farbe; auf dem Schirm ist sie
 * nur durch zwei Stopp-Deckungen hindurch zu sehen. Ein Abstand gegen das Token
 * beschriebe eine Fläche, die niemand vor sich hat.
 */
function ueber(vorne: Farbe, hinten: Farbe, alpha: number): Farbe {
  const klemme = (c: number) => Math.min(1, Math.max(0, c));
  const v = srgb(vorne).roh.map(klemme);
  const h = srgb(hinten).roh.map(klemme);
  const misch = v.map((c, i) => alpha * c + (1 - alpha) * h[i]);
  // Erst nach dem Mischen linearisieren: nachOklch() erwartet lineares sRGB.
  return nachOklch(misch.map(entgamma));
}

const zahl = (n: number, s = 2): string => n.toFixed(s).replace(".", ",");

// ───────────────────────────────────────────────────────────────────────────
// Die beiden Blöcke, aus der Datei gelesen
// ───────────────────────────────────────────────────────────────────────────

/**
 * Die Regel eines Blocks — und zwar die auf der **obersten Ebene**.
 *
 * Die Lage und nicht nur der Wert, dieselbe Lehre wie in
 * `tests/dichte.test.ts`: Ein `:root` in `@media (pointer: coarse)` gibt es in
 * dieser Datei wirklich, und ein zweites `:root` am Dateiende wäre ein Block,
 * dessen zweite Hälfte dieser Test nie sähe — beide sähen für eine Textsuche
 * aus wie der gesuchte.
 */
function blockregel(selektor: string): Regel {
  const gefunden = REGELN.filter((regel) => regel.selektor === selektor && regel.pfad.length === 0);
  expect(
    gefunden.length,
    `\`${selektor}\` steht ${gefunden.length}-mal auf der obersten Ebene von globals.css. ` +
      `Erwartet ist genau einmal — bei zweien läse dieser Test die falsche Hälfte des Blocks.`,
  ).toBe(1);
  return gefunden[0];
}

/** Der eine unbedingte `:root`-Block. Beide Wertesätze stehen darin. */
const wurzel = (): Regel => blockregel(":root");

/**
 * Die beiden Wertesätze — und sie stehen seit dem 03.09.2026 **beide unter
 * `:root`**, unterschieden durch den Namen: die hellen als `--<token>`, die
 * dunklen als `--dunkel-<token>`.
 *
 * ## Warum hier kein Selektor mehr steht *(Schritt 11b, E‑59 und E‑60)*
 *
 * Bis dahin las dieser Test zwei Blöcke: `:root` und `.dark`. Mit dem
 * Umschalter hat der Dunkelzustand **zwei Zweige** — den ausdrücklichen
 * (`html[data-thema="dunkel"]`) und den System-Zweig unter
 * `@media (prefers-color-scheme: dark)`. Ein Selektor kann beide nicht zugleich
 * treffen, denn eine Medienabfrage ist keine Selektorbedingung.
 *
 * Stünden die Werte deshalb in **beiden** Zweigen, gäbe es sie zweimal, und
 * dieser Test läse eine von zwei Hälften — die andere könnte abweichen, ohne
 * dass eine Zahl hier falsch würde. Genau der Fehler aus §7.5. Sie stehen
 * deshalb **einmal** unter `:root` als Zwischenschicht, und die beiden Zweige
 * zeigen nur um.
 *
 * **Dass sie das wirklich tun, prüft dieser Test nicht** — das ist die Aufgabe
 * von `tests/thema.test.ts`, und zwar für beide Zweige und in beide Richtungen.
 * Hier stehen die Zahlen; dort steht die Verdrahtung.
 */
const HELL = "";
const DUNKEL = "dunkel-";

const BLOECKE = [
  ["hell", HELL],
  ["dunkel", DUNKEL],
] as const;

/**
 * Eine Farbe aus einem Wertesatz — geparst und **nicht** stillschweigend
 * übergangen, wenn sie eine Form hat, die dieser Test nicht kennt.
 *
 * Ein `var(--akzent)` etwa ist eine gültige Deklaration und für diesen Test
 * trotzdem keine Farbe: Er löst keine Kaskade auf. Ein stiller Rückfall wäre
 * hier das Schlimmste — der Test bestünde weiter und prüfte nichts mehr.
 */
function farbe(praefix: string, token: string): Farbe {
  const name = `--${praefix}${token}`;
  const roh = wert(wurzel(), name);
  const treffer = /^oklch\(\s*([0-9.]+)\s+([0-9.]+)\s+([0-9.]+)\s*\)$/.exec(roh);
  expect(
    treffer,
    `\`${name}: ${roh}\` ist keine Farbe, die dieser Test auswerten kann. ` +
      `Erwartet wird \`oklch(L C H)\` ohne Alphaanteil und ohne \`var()\` — entweder ist die ` +
      `Deklaration falsch, oder farbe() muss die neue Form lernen. Übergangen wird sie nicht.`,
  ).not.toBeNull();
  const [, L, C, h] = treffer as RegExpExecArray;
  return { L: Number(L), C: Number(C), h: Number(h) };
}

/**
 * Eine **Deckung** aus einem Wertesatz — eine blanke Zahl zwischen 0 und 1.
 *
 * Sie steht in derselben Datei wie die Farben und wird genauso gelesen: Was der
 * Test rechnet, soll aus dem stammen, was ausgeliefert wird. Ein stiller
 * Rückfall auf einen Vorgabewert wäre hier dasselbe Übel wie bei {@link farbe} —
 * der Test bestünde weiter und prüfte eine Fläche, die es nicht gibt.
 */
function deckung(praefix: string, token: string): number {
  const name = `--${praefix}${token}`;
  const roh = wert(wurzel(), name);
  const n = Number(roh);
  expect(
    Number.isFinite(n) && n >= 0 && n <= 1,
    `\`${name}: ${roh}\` ist keine Deckung zwischen 0 und 1.`,
  ).toBe(true);
  return n;
}

// ───────────────────────────────────────────────────────────────────────────

describe("Der Leser findet beide Wertesätze", () => {
  it("liest überhaupt Regeln, und die beiden gesuchten darunter", () => {
    // Ohne diese Zusicherung bestünde jeder Test unten auch dann, wenn der
    // Leser nichts fände — dieselbe Vorsorge wie in `tests/dichte.test.ts`.
    expect(REGELN.length).toBeGreaterThan(8);
    expect(wurzel().eigene.length).toBeGreaterThan(200);
    for (const [name, praefix] of BLOECKE) {
      expect(wurzel().eigene, `Der Wertesatz »${name}« steht nicht unter \`:root\`.`).toContain(
        `--${praefix}status-fehler:`,
      );
    }
  });

  it("liest zwei verschiedene Wertesätze und nicht zweimal denselben", () => {
    // Die Gegenprobe zum Leser selbst. Geprüft wird, **dass** sich die beiden
    // Sätze unterscheiden — nicht, welchen Wert `--card` hat: Das ist ein
    // shadcn-Basistoken und damit **Bezug und nicht Gegenstand** (E‑61). Ein
    // fester Sollwert darauf ginge beim nächsten `shadcn add` aus einem Grund
    // rot, der mit dieser Zusicherung nichts zu tun hat.
    for (const grund of ["card", "background"]) {
      expect(
        farbe(HELL, grund).L,
        `--${grund} ist im hellen und im dunklen Wertesatz gleich hell. Entweder liest dieser ` +
          `Test zweimal denselben, oder der Dunkelsatz ist keiner mehr.`,
      ).toBeGreaterThan(farbe(DUNKEL, grund).L);
    }
  });
});

describe("Die vierzehn Gegenproben — die Methode, bevor das Ergebnis", () => {
  /**
   * Acht bekannte Zahlen aus `visuelles-konzept.md` §3. Sie sind mit **dieser**
   * Methode gemessen worden, und nur mit ihr kommen sie heraus.
   */
  it.each([
    ["akzent auf card", "akzent", "card", 1.98],
    ["akzent-schrift auf card", "akzent-schrift", "card", 5.4],
    ["akzent-schrift auf background", "akzent-schrift", "background", 5.18],
    ["akzent-schrift auf akzent-flaeche", "akzent-schrift", "akzent-flaeche", 4.91],
    ["akzent-vordergrund auf akzent", "akzent-vordergrund", "akzent", 9.13],
    ["status-abgeschlossen auf card", "status-abgeschlossen", "card", 6.78],
  ])("gibt §3 wieder: %s", (_name, vorne, hinten, erwartet) => {
    const k = kontrast(farbe(HELL, vorne as string), farbe(HELL, hinten as string));
    expect(Math.abs(k.roh - (erwartet as number)), `gerechnet ${zahl(k.roh, 4)}`).toBeLessThan(
      0.005,
    );
  });

  it("gibt die beiden OKLab-Abstände aus §3 wieder", () => {
    // 0,3435 und 0,3523 — sie tragen die Begründung, warum das Status-Grün von
    // Ton 150 auf 166 gewandert ist. Kämen sie anders heraus, stünde diese
    // Begründung auf einer anderen Rechnung als der hier.
    const gruenAkzent = abstand(farbe(HELL, "status-abgeschlossen"), farbe(HELL, "akzent"));
    const akzentRot = abstand(farbe(HELL, "akzent"), farbe(HELL, "status-fehler"));
    expect(Math.abs(gruenAkzent - 0.343), `gerechnet ${zahl(gruenAkzent, 4)}`).toBeLessThan(0.0005);
    expect(Math.abs(akzentRot - 0.352), `gerechnet ${zahl(akzentRot, 4)}`).toBeLessThan(0.0005);
  });

  /**
   * Die sechs Hexwerte aus §7a. Sie sind dort **doppelt belegt**: einmal aus dem
   * Lauf von `scripts/farbwerte/rechne.mjs` und einmal aus dem Hex-Rückfall, den
   * Lightning CSS beim Bauen schreibt. Das ist die einzige Stelle im Projekt, an
   * der eine Farbrechnung von einem fremden Werkzeug bestätigt worden ist.
   */
  it.each([
    ["hell", HELL, "ueberfaellig", "#886108"],
    ["hell", HELL, "ueberfaellig-flaeche", "#fcf0dd"],
    ["hell", HELL, "ueberfaellig-kontur", "#b88513"],
    ["dunkel", DUNKEL, "ueberfaellig", "#cb9317"],
    ["dunkel", DUNKEL, "ueberfaellig-flaeche", "#312103"],
    ["dunkel", DUNKEL, "ueberfaellig-kontur", "#8b640f"],
  ])("gibt §7a wieder: %s --%s%s = %s", (_block, praefix, token, erwartet) => {
    expect(srgb(farbe(praefix as string, token as string)).hex).toBe(erwartet);
  });

  /**
   * Die fünf Hexwerte aus §3. Sie zählen **nicht** zu den vierzehn und laufen
   * trotzdem mit: `scripts/farbwerte/rechne.mjs` führt sie seit dem 31.08.2026,
   * und was dort geprüft wird, soll hier nicht weniger geprüft sein.
   */
  it.each([
    ["akzent", "#b9c022"],
    ["akzent-schrift", "#6a6f0f"],
    ["akzent-flaeche", "#f3f6dc"],
    ["akzent-vordergrund", "#161802"],
    ["status-abgeschlossen", "#01684c"],
  ])("gibt den Hexwert aus §3 wieder: --%s = %s", (token, erwartet) => {
    expect(srgb(farbe(HELL, token)).hex).toBe(erwartet);
  });
});

describe.each(BLOECKE)("Der Wertesatz %s (`--%s…`)", (block, praefix) => {
  /**
   * **Eine Rolle, die im Dunkelblock fehlt, fällt auf den hellen Wert zurück** —
   * und niemand sieht es, solange niemand umschaltet. Ein helles
   * `--status-offen` (`#525252`) auf `--card` `#181818` erreichte **2,28 : 1**
   * (aus dem Hexwert 2,27), auf seiner dunklen Fläche sogar nur 2,06 : 1.
   *
   * Deshalb steht diese Zusicherung vor allen Kontrasten: Sie prüft nicht, wie
   * gut ein Wert ist, sondern dass es ihn gibt.
   */
  it.each(ROLLEN)("deklariert die Rolle --%s mit allen drei Werten", (rolle) => {
    for (const teil of TEILE) {
      const f = farbe(praefix, `${rolle}${teil}`);
      expect(f.L, `--${praefix}${rolle}${teil} in ${block}`).toBeGreaterThan(0);
    }
  });

  it("deklariert beide Stufen der Verlaufsfläche", () => {
    // Dieselbe Vorsorge wie eine Zeile tiefer: Fehlte `--verlauf-flaeche` im
    // Dunkelblock, fiele die Fläche auf den hellen Wert zurück — hier wäre das
    // folgenlos, weil beide Blöcke denselben Wert tragen, und genau deshalb
    // fiele es niemandem auf, wenn sich das änderte.
    for (const stufe of VERLAUFSSTUFEN) {
      expect(farbe(praefix, stufe).L, `--${praefix}${stufe} in ${block}`).toBeGreaterThan(0);
    }
  });

  it("deklariert alle vier Akzentstufen", () => {
    // Dieselbe Begründung eine Zeile höher, für die Stufen statt für die
    // Rollen: Fehlte `--akzent-schrift` im Dunkelblock, stünde dort die
    // abgedunkelte helle Stufe `#6a6f0f` auf der dunklen `--akzent-flaeche`
    // `#2a2c02` — **2,68 : 1** statt 8,76 : 1, und die aktive Navigationszeile
    // wäre unlesbar, weil `--accent-foreground` darauf zeigt. Auf `--card`
    // wären es 3,28 : 1: über der 3 : 1 aus WCAG 1.4.11 und damit für den
    // Fokusring gerade noch tragbar — die Fläche ist der Fall, der bricht.
    for (const stufe of AKZENTSTUFEN) {
      expect(farbe(praefix, stufe).L, `--${praefix}${stufe} in ${block}`).toBeGreaterThan(0);
    }
  });

  /**
   * **Die Bedingung, an der eine falsche Farbe scheitert.**
   *
   * Drei Untergründe je Rolle, weil eine Rolle auf allen dreien auftritt: als
   * Plakette auf ihrer eigenen Fläche, als Zahl in einer Zeile auf `--card`, und
   * auf dem Inhaltsbereich `--background` daneben.
   */
  it.each(ROLLEN)("hält --%s auf allen drei Untergründen 4,5 : 1", (rolle) => {
    const vorne = farbe(praefix, rolle);
    for (const hinten of [`${rolle}-flaeche`, "card", "background"]) {
      const k = kontrast(vorne, farbe(praefix, hinten));
      expect(
        Math.min(k.roh, k.acht),
        `--${rolle} auf --${hinten} in ${block}: ${zahl(k.roh)} : 1 ungerundet, ` +
          `${zahl(k.acht)} : 1 aus dem Hexwert. Beide müssen die Schwelle halten — ` +
          `ein Wert, der nur ungerundet darüber liegt, ist auf dem Bildschirm keiner.`,
      ).toBeGreaterThanOrEqual(SCHWELLE_TEXT);
    }
  });

  it("hält die drei Akzentstufen, die Schrift sind, auf 4,5 : 1", () => {
    // `--akzent` selbst steht **nicht** hier: Er ist eine Füllfarbe und erreicht
    // auf Weiß 1,98 : 1 (§3). Genau deshalb gibt es die drei anderen Stufen —
    // ihn hier zu fordern hieße, das Farbsystem misszuverstehen.
    for (const [vorne, hinten] of [
      ["akzent-schrift", "card"],
      ["akzent-schrift", "background"],
      ["akzent-schrift", "akzent-flaeche"],
      ["akzent-vordergrund", "akzent"],
    ]) {
      const k = kontrast(farbe(praefix, vorne), farbe(praefix, hinten));
      expect(
        Math.min(k.roh, k.acht),
        `--${vorne} auf --${hinten} in ${block}: ${zahl(k.roh)} : 1 (Hexwert ${zahl(k.acht)})`,
      ).toBeGreaterThanOrEqual(SCHWELLE_TEXT);
    }
  });

  /**
   * **Regel Q3, rechnerisch geprüft.**
   *
   * Fehler und Überfällig sind zwei Problemkategorien, die getrennt **und
   * gleichrangig** geführt werden. Zwei Kacheln nebeneinander, von denen eine
   * dunkler steht, lesen sich als Rangfolge — unterschieden wird über den Ton
   * (166 gegen 27 gegen 80), nie über das Gewicht (`visuelles-konzept.md` §7a).
   */
  it("stellt Fehler und Überfällig auf dieselbe Helligkeit", () => {
    expect(
      farbe(praefix, "ueberfaellig").L,
      `--ueberfaellig und --status-fehler stehen in ${block} auf verschiedenen Helligkeiten. ` +
        `Regel Q3 führt beide gleichrangig; die dunklere läse sich als die nachrangige.`,
    ).toBe(farbe(praefix, "status-fehler").L);
  });

  /**
   * Jeder Wert liegt im sRGB-Farbraum — **ohne Ausnahme, seit dem 04.09.2026.**
   *
   * ## Hier stand eine Ausnahme, und sie ist **entfallen statt gelockert**
   *
   * `hell --status-fehler-flaeche` lag seit Schritt 3 außerhalb: `oklch(0.96 0.028 27)`
   * trieb den roten Kanal auf **1,0211**, der Browser schnitt ab, und was auf dem
   * Schirm stand, war `#ffebe8` = `oklch(0,9555 0,0221 29,4)` — **weder die
   * eingetragene Helligkeit noch der eingetragene Ton**. Der Überschuss war hier
   * festgenagelt, damit er nicht unbemerkt wächst.
   *
   * Mit der Dämpfung aus **E‑88** (Chroma 0,028 → 0,016, Ton 27 → 22) liegt der Wert
   * im Farbraum. **Offener Punkt 123 ist damit geschlossen** (`docs/dunkelmodus.md`
   * §4), und die Bedingung gilt jetzt für jeden Wert — sie ist **schärfer** geworden
   * und nicht weicher. Wer die Ausnahme zurückholen will, holt den Befund mit.
   */
  it("hält jeden Wert im sRGB-Farbraum — ohne Ausnahme", () => {
    const alle = [
      ...ROLLEN.flatMap((r) => TEILE.map((t) => `${r}${t}`)),
      ...AKZENTSTUFEN,
      ...VERLAUFSSTUFEN,
    ];

    for (const token of alle) {
      const u = srgb(farbe(praefix, token)).ueberschuss;
      expect(
        u,
        `--${token} in ${block} liegt außerhalb des sRGB-Farbraums (Überschuss ${u.toFixed(6)}) ` +
          `und wird abgeschnitten. Was der Bildschirm zeigt, ist dann nicht der Wert, der dasteht.`,
      ).toBe(0);
    }
  });

  /**
   * **Die Verlaufsfläche (E‑87) — die Abstände aus dem Auftrag.**
   *
   * Vorgegeben waren *Abstände*, keine Werte. Sie stehen hier als Zusicherung und
   * nicht nur im Skript, weil sie sonst genau das wären, was `farbwerte.test.ts`
   * über sich selbst sagt: eine Behauptung, die niemand nachrechnet.
   *
   * ## Gerechnet wird gegen den **gemalten** Stopp, nicht gegen das Token
   *
   * Das Token ist die volle Farbe; auf dem Schirm steht sie nur durch die beiden
   * Stopp-Deckungen hindurch (hell 0,35 / 0,03, dunkel 0,28 / 0,04). Ein Abstand
   * gegen `--verlauf-flaeche` beschriebe eine Fläche, die niemand vor sich hat.
   * Die Werte des Tokens bleiben trotzdem geprüft — bei den Rollen, als Schranke
   * für den Tag, an dem jemand die Deckung wieder hochdreht.
   *
   * ## Die 3 : 1 hängen an der **Kontur**
   *
   * Die Linie trägt den Kurvenverlauf und ist die Aussage des Diagramms; die
   * Fläche darunter trägt Gewicht. Eine Tönung an der Umriss-Schwelle aus WCAG
   * 1.4.11 zu messen hieße, sie zu einem Umriss zu erklären. Ihr Kontrast steht
   * als **Bericht** in `scripts/farbwerte/rechne.mjs` (1,51 : 1 hell,
   * 1,47 : 1 dunkel) und ist hier ausdrücklich keine Bedingung.
   *
   * **Was hier NICHT steht:** der Abstand zu den Gitterlinien und die
   * Banding-Rechnung. Beide hängen zusätzlich an der Höhe der gezeichneten
   * Fläche und damit an nichts, was in `globals.css` steht; sie stehen mit ihren
   * Zahlen im Skript und in `docs/visuelles-konzept.md` §3.
   */
  describe("Die Verlaufsfläche", () => {
    /** Was oben wirklich gemalt wird: das Token durch die obere Deckung. */
    const gemalt = () =>
      ueber(
        farbe(praefix, "verlauf-flaeche"),
        farbe(praefix, "card"),
        deckung(praefix, "verlauf-deckung-oben"),
      );

    it("hebt sich als Tönung von der Karte ab", () => {
      // Keine Kontrastbedingung — die steht an der Kontur. Hier zählt, ob die
      // Tönung überhaupt zu sehen ist. Der Akzent, den sie ablöst, käme an
      // derselben Deckung im hellen Block auf 0,1085.
      const d = abstand(gemalt(), farbe(praefix, "card"));
      expect(
        d,
        `Der gemalte obere Stopp steht --card in ${block} auf ${zahl(d, 4)} nahe — unter ` +
          `${zahl(SICHTPROBE, 3)} wäre die Fläche kein Bild mehr, sondern ein Hauch.`,
      ).toBeGreaterThanOrEqual(SICHTPROBE);
    });

    it("steht keiner Statusrolle näher als der Bestand sich selbst", () => {
      // Zu nah an --status-offen, und die Fläche läse sich wieder als
      // Statusaussage. Geprüft wird darüber hinaus JEDE Rolle, und zwar gegen
      // den gemalten Stopp UND gegen das Token: Das Token ist die Schranke für
      // den Tag, an dem jemand die Deckung hochdreht.
      const zuOffen = abstand(gemalt(), farbe(praefix, "status-offen"));
      expect(zuOffen, `zu --status-offen in ${block}: ${zahl(zuOffen, 4)}`).toBeGreaterThanOrEqual(
        SICHTPROBE,
      );

      for (const rolle of ROLLEN) {
        for (const [was, f] of [
          ["Der gemalte obere Stopp", gemalt()],
          ["Das Token --verlauf-flaeche", farbe(praefix, "verlauf-flaeche")],
        ] as const) {
          const d = abstand(f, farbe(praefix, rolle));
          expect(
            d,
            `${was} steht --${rolle} in ${block} auf ${zahl(d, 4)} nahe — enger als die ` +
              `engste Strecke des Bestands (${zahl(ENGSTE_STRECKE, 4)}, §7a Befund 3).`,
          ).toBeGreaterThanOrEqual(ENGSTE_STRECKE);
        }
      }
    });

    it("hält Abstand zur Fehlerkachelfläche, und zwar im Buntton", () => {
      // Hier ist der Ton das Maß und nicht die Helligkeit: Die beiden liegen
      // ohnehin in verschiedenen Registern, und was sie auseinanderhält, ist der
      // Ton. Gemessen wird der Ton des GEMALTEN Stopps — Mischen im linearen
      // Licht dreht ihn ein paar Grad gegenüber der Deklaration.
      const kachel = farbe(praefix, "status-fehler-flaeche");
      const roh = Math.abs(gemalt().h - kachel.h);
      const ton = Math.min(roh, 360 - roh);
      expect(ton, `Buntton-Abstand in ${block}: ${ton.toFixed(1)} Grad`).toBeGreaterThanOrEqual(90);
      expect(abstand(gemalt(), kachel)).toBeGreaterThanOrEqual(SICHTPROBE);
    });

    it("läuft nach unten aus, statt als Block zu enden", () => {
      // Die beiden Deckungen sind die eigentliche Stellschraube, und sie haben
      // eine Richtung: Oben trägt die Fläche auf, unten läuft sie aus. Ohne
      // diese Zusicherung ließe sich das Paar vertauschen, ohne dass eine Zahl
      // hier falsch würde — und das Bild stünde auf dem Kopf.
      const oben = deckung(praefix, "verlauf-deckung-oben");
      const unten = deckung(praefix, "verlauf-deckung-unten");
      expect(
        oben,
        `Der obere Stopp (${oben}) deckt in ${block} nicht mehr als der Fuß (${unten}).`,
      ).toBeGreaterThan(unten);
      expect(
        oben,
        `Der obere Stopp deckt in ${block} voll (${oben}). Dann verschwindet die Gitterlinie ` +
          `unter ihm vollständig — gemessen 0,0000 statt der geforderten ${zahl(SICHTPROBE, 3)}.`,
      ).toBeLessThan(1);
    });

    it("trägt eine Kontur, die die 3 : 1 hält und sich von der Fläche abhebt", () => {
      // **Die einzige Stelle, an der die Umriss-Schwelle gefordert wird.**
      const kontur = farbe(praefix, "verlauf-kontur");
      const k = kontrast(kontur, farbe(praefix, "card"));
      expect(
        Math.min(k.roh, k.acht),
        `--verlauf-kontur auf --card in ${block}: ${zahl(k.roh)} : 1 (Hexwert ${zahl(k.acht)}). ` +
          `Die Linie trägt den Kurvenverlauf; sie ist die Aussage des Diagramms.`,
      ).toBeGreaterThanOrEqual(SCHWELLE_UMRISS);
      expect(abstand(kontur, gemalt())).toBeGreaterThanOrEqual(SICHTPROBE);
    });
  });

  /**
   * **Die gedämpfte Fehlerkachelfläche (E‑88) liest sich weiter als gefüllt.**
   *
   * „Gefüllt heißt, hier ist etwas zu tun" (E‑79). Eine Tönung, die von der Karte
   * nicht mehr zu unterscheiden ist, nähme der einen Problemkachel genau das. Der
   * Kontrast der **Zahl** darauf steht bereits in der Schleife über {@link ROLLEN}
   * und ist hier nicht wiederholt.
   */
  it("lässt die Fehlerkachel als gefüllt lesbar", () => {
    const d = abstand(farbe(praefix, "status-fehler-flaeche"), farbe(praefix, "card"));
    expect(
      d,
      `--status-fehler-flaeche steht --card in ${block} auf ${zahl(d, 4)} nahe. Unter ` +
        `${zahl(SICHTPROBE, 3)} wäre die Kachel keine gefüllte mehr.`,
    ).toBeGreaterThanOrEqual(SICHTPROBE);
  });
});

/**
 * **Die Verdrahtung — sonst gilt jeder gerechnete Wert für nichts.**
 *
 * ## Der Mutant, der die erste Fassung überlebt hat *(03.09.2026)*
 *
 * Eine adversarische Durchsicht hat die erste Fassung dieses Tests an genau
 * einer Stelle zerlegt: Sie las `:root` und `.dark` und **nie** den Block
 * `@theme inline`. Dort steht aber die einzige Verbindung zwischen dem
 * geprüften Token und der Klasse, die eine Komponente schreibt.
 *
 * Gemessen, nicht behauptet: `--color-status-fehler: var(--status-fehler)` auf
 * `var(--status-abgeschlossen)` gedreht — **49 von 49 grün, die volle Suite
 * 812 von 812 grün.** Jedes `text-status-fehler` im Projekt hätte danach die
 * grüne Farbe getragen, und alle Kontraste dieses Tests wären weiter richtig
 * gewesen: Er hätte den Wert geprüft, den niemand mehr sieht.
 *
 * **Es ist derselbe Mutant, der in `tests/dichte.test.ts` „am weitesten trug"**
 * (`--spacing-beruehrung` auf ein anderes Token zeigen lassen). Dass er hier
 * nach derselben Lehre trotzdem offenstand, ist der Befund — und der Grund,
 * warum diese Zusicherung nachgereicht ist und nicht von Anfang an dastand.
 */
describe("Die Verdrahtung in `@theme inline`", () => {
  const thema = () => {
    const gefunden = REGELN.filter((r) => r.selektor === "@theme inline" && r.pfad.length === 0);
    expect(gefunden, "Kein `@theme inline`-Block auf der obersten Ebene").toHaveLength(1);
    return gefunden[0];
  };

  it.each([...ROLLEN.flatMap((r) => TEILE.map((t) => `${r}${t}`)), ...AKZENTSTUFEN])(
    "zeigt --color-%s auf sein eigenes Token",
    (token) => {
      // **Auf sein EIGENES** und nicht bloß „auf irgendein `var()`": Genau die
      // Verwechslung ist der Mutant. Der reguläre Ausdruck bindet deshalb
      // beide Seiten aneinander.
      expect(
        thema().eigene,
        `\`--color-${token}\` zeigt in \`@theme inline\` nicht auf \`var(--${token})\`. ` +
          `Damit trägt die Klasse \`text-${token}\` bzw. \`bg-${token}\` einen anderen Wert ` +
          `als den, den dieser Test nachgerechnet hat — und keine Zahl hier wäre falsch geworden.`,
      ).toMatch(new RegExp(`(^|[;\\s])--color-${token}:\\s*var\\(--${token}\\)\\s*;`));
    },
  );

  it("hängt unbedingt und steht genau einmal da", () => {
    // Dieselbe Lehre wie bei den beiden Blöcken: Ein `@theme inline` in
    // `@media print` gäbe es zwar, und die Klassen entstünden trotzdem nie.
    expect(thema().pfad).toEqual([]);
  });
});

/**
 * **Berichtet, nicht zugesichert** — und das ist eine Entscheidung, keine Lücke.
 *
 * §7a, Befund 4 hält fest, dass vier der fünf Konturen die 3 : 1 aus WCAG 1.4.11
 * verfehlen, und dass **die Ungleichheit der Befund ist**: *„Wer sie auflöst,
 * tut es für alle fünf Rollen zugleich und rechnet dabei den ganzen Bestand
 * nach."* Ein Test darauf wäre heute rot und würde eine getroffene Entscheidung
 * umdrehen — die aus `dashboard-frontend.md` §3 (E‑u), wo das Auge gesagt hat,
 * dass gerade die *saubere* Kontur aus zwei gleichrangigen Kacheln eine
 * Rangfolge macht.
 */
describe("Die Konturen", () => {
  it("werden berichtet und nicht zugesichert", () => {
    const zeilen: string[] = [];
    for (const [block, praefix] of BLOECKE) {
      for (const rolle of ROLLEN) {
        const token = `${rolle}-kontur`;
        const f = farbe(praefix, token);
        const k = kontrast(f, farbe(praefix, "card")).roh;
        zeilen.push(
          `  ${block.padEnd(7)} --${token.padEnd(30)} ${srgb(f).hex}  ${zahl(k)} : 1  ` +
            `${k >= SCHWELLE_UMRISS ? "erfuellt" : "verfehlt"}`,
        );
      }
    }
    // Ohne diese Zusicherung stünde der Bericht da, ohne dass jemand wüsste, ob
    // er überhaupt etwas gefunden hat — dieselbe Vorsorge wie oben.
    expect(zeilen).toHaveLength(ROLLEN.length * BLOECKE.length);
    console.log(`\nDie Konturen auf --card (WCAG 1.4.11, Bericht):\n${zeilen.join("\n")}\n`);
  });
});
