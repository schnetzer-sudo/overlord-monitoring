/**
 * Die Farbwerte des Projekts — nachgerechnet, nicht geschaetzt.
 *
 * `docs/visuelles-konzept.md` §8 sagt: „Nachrechnen ist woertlich gemeint."
 * Dieses Skript ist das Woertlichnehmen. Es wandelt OKLCH nach sRGB, rechnet
 * WCAG-Kontraste und OKLab-Abstaende und prueft jede Bedingung, die der Rahmen
 * aus §7a den Rollen stellt.
 *
 *   node scripts/farbwerte/rechne.mjs
 *
 * Es braucht kein Paket und keine Installation — nur Node.
 *
 * ## Warum es nicht mehr `farbrolle-ueberfaellig` heisst *(03.09.2026)*
 *
 * Es rechnete eine Rolle. Es rechnet jetzt den **Bestand**: fuenf Rollen zu je
 * drei Werten und vier Akzentstufen, in **beiden** Bloecken. Der Anlass ist
 * `visuelles-konzept.md` §7a, Befund 3 — „Der Dunkelblock ist nie nachgerechnet
 * worden … Wer den Dunkelmodus einschaltet, rechnet den ganzen Block nach, nicht
 * nur diese Rolle." Das ist Schritt 11a, ausgeschrieben in `docs/dunkelmodus.md`.
 *
 * **Die Gegenprobe unten ist dabei nicht angefasst worden, sondern erweitert.**
 * Die acht bekannten Werte aus §3 stehen Zeichen fuer Zeichen wie vorher; dazu
 * kommen die sechs Hexwerte aus §7a, die Lightning CSS beim Bauen bestaetigt hat.
 * Dass diese vierzehn nach der Umbenennung unveraendert durchlaufen, ist der
 * Beleg, dass die Umbenennung nichts geaendert hat.
 *
 * ## Wie hier gerechnet wird, und warum genau so
 *
 * Der Kontrast wird aus den **ungerundeten** sRGB-Fliesskommawerten gebildet,
 * nicht aus dem 8-Bit-Hexwert. Das ist die Methode, mit der die vier
 * Akzentstufen in §3 gemessen worden sind: Nur sie gibt deren Zahlen (5,40 /
 * 5,18 / 4,91 / 9,13 / 1,98) wieder. Der Block GEGENPROBE unten rechnet sie
 * nach; weicht eine ab, stimmt die Methode nicht mehr und der Lauf sagt es.
 *
 * Zur Sicherheit steht neben jedem Kontrast der aus dem gerundeten Hexwert —
 * das ist der, den ein Bildschirm wirklich zeigt. Beide duerfen nie auf
 * verschiedenen Seiten einer Schwelle liegen; auch das prueft der Lauf.
 *
 * Der OKLab-Abstand ist der euklidische Abstand in (L, a, b), **einschliesslich
 * der Helligkeit**. Auch das ist die Methode aus §3: Sie und nur sie gibt die
 * dort genannten 0,343 (Gruen zu Akzent) und 0,352 (Akzent zu Rot).
 */

// ─── OKLCH → OKLab → lineares sRGB → sRGB ────────────────────────────────────

const oklab = ({ L, C, h }) => {
  const r = (h * Math.PI) / 180;
  return { L, a: C * Math.cos(r), b: C * Math.sin(r) };
};

const linear = ({ L, a, b }) => {
  const l = (L + 0.3963377774 * a + 0.2158037573 * b) ** 3;
  const m = (L - 0.1055613458 * a - 0.0638541728 * b) ** 3;
  const s = (L - 0.0894841775 * a - 1.291485548 * b) ** 3;
  return [
    4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
    -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
    -0.0041960863 * l - 0.7034186147 * m + 1.707614701 * s,
  ];
};

const gamma = (c) => (c <= 0.0031308 ? 12.92 * c : 1.055 * c ** (1 / 2.4) - 0.055);
const entgamma = (c) => (c <= 0.04045 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4);

/** sRGB als Fliesskomma, als 8-Bit-Tripel, als Hexwert — und ob es passt. */
function srgb(farbe) {
  const roh = linear(oklab(farbe)).map(gamma);
  // Toleranz von einem halben Tausendstel: Rundungsrauschen ist kein Austritt
  // aus dem Farbraum.
  const imRaum = roh.every((c) => c >= -0.0005 && c <= 1.0005);
  const acht = roh.map((c) => Math.round(Math.min(1, Math.max(0, c)) * 255));
  return {
    roh,
    acht,
    hex: "#" + acht.map((v) => v.toString(16).padStart(2, "0")).join(""),
    imRaum,
  };
}

const luminanzRoh = (roh) => {
  const [r, g, b] = roh.map((c) => entgamma(Math.min(1, Math.max(0, c))));
  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
};
const luminanzAcht = (acht) => {
  const [r, g, b] = acht.map((v) => entgamma(v / 255));
  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
};

const verhaeltnis = (a, b) => (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05);

/** WCAG-Kontrast, ungerundet gerechnet — daneben der aus dem Hexwert. */
const kontrast = (x, y) => ({
  roh: verhaeltnis(luminanzRoh(srgb(x).roh), luminanzRoh(srgb(y).roh)),
  acht: verhaeltnis(luminanzAcht(srgb(x).acht), luminanzAcht(srgb(y).acht)),
});

/** Euklidischer Abstand in OKLab, einschliesslich Helligkeit. */
function abstand(x, y) {
  const a = oklab(x);
  const b = oklab(y);
  return Math.hypot(a.L - b.L, a.a - b.a, a.b - b.b);
}

const ok = (L, C, h) => ({ L, C, h });
const zahl = (n, s = 2) => n.toFixed(s).replace(".", ",");

/** sRGB (linear, 0..1) zurueck nach OKLCH — fuer alles, was VERMISCHT entsteht. */
function nachOklch(linearRgb) {
  const [r, g, b] = linearRgb;
  const l = Math.cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b);
  const m = Math.cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b);
  const s = Math.cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b);
  const L = 0.2104542553 * l + 0.793617785 * m - 0.0040720468 * s;
  const A = 1.9779984951 * l - 2.428592205 * m + 0.4505937099 * s;
  const B = 0.0259040371 * l + 0.7827717662 * m - 0.808675766 * s;
  return { L, C: Math.hypot(A, B), h: ((Math.atan2(B, A) * 180) / Math.PI + 360) % 360 };
}

/**
 * `vorne` mit der Deckung `alpha` ueber `hinten`.
 *
 * GEMISCHT WIRD IN sRGB UND NICHT IM LINEAREN LICHT, und das ist gemessen und
 * nicht gewaehlt. Der erste Bau dieser Funktion mischte im linearen Licht — das
 * ist die physikalisch richtige Art, Licht zu addieren, und es ist NICHT, was
 * der Browser tut: `color-interpolation` steht in SVG auf `sRGB`, und die
 * Alphamischung laeuft im gammakodierten Raum.
 *
 * Am 04.09.2026 in Chrome an drei Proben nachgesehen (SVG ueber data:-URL auf
 * ein Canvas, Pixel ausgelesen):
 *
 *   #1992bf zu 28 % ueber #181818   gemessen #183a46   sRGB #183a47   linear #18536d
 *   #1992bf zu 35 % ueber #ffffff   gemessen #afd9e9   sRGB #afd9e9   linear #d3e1eb
 *   #ffffff zu 12 % ueber #181818   gemessen #343434   sRGB #343434   linear #646464
 *
 * Der Unterschied ist keine Feinheit: Beim dritten Paar liegen die beiden
 * Rechenwege 0,19 in OKLab auseinander — mehr als das Siebenfache der
 * Sichtprobe. Wer hier linear mischt, beschreibt eine Flaeche, die niemand
 * sieht.
 */
function ueber(vorne, hinten, alpha) {
  const klemme = (c) => Math.min(1, Math.max(0, c));
  const v = srgb(vorne).roh.map(klemme);
  const h = srgb(hinten).roh.map(klemme);
  const misch = v.map((c, i) => alpha * c + (1 - alpha) * h[i]);
  // Erst nach dem Mischen linearisieren: nachOklch() erwartet lineares sRGB.
  return nachOklch(misch.map(entgamma));
}

// ─── Die Palette, wie sie in src/app/globals.css steht ───────────────────────
//
// GELESEN und nicht gewaehlt: Die shadcn-Basistokens (card, background, muted)
// sind der BEZUG dieser Rechnung, nicht ihr Gegenstand (E-61). Sie werden aus
// dem Generator uebernommen und hier nur zitiert.

const HELL = {
  card: ok(1, 0, 0),
  background: ok(0.985, 0, 0),
  muted: ok(0.96, 0, 0),
  akzent: ok(0.777, 0.1643, 112.4),
  "akzent-schrift": ok(0.52, 0.11, 112.4),
  "akzent-flaeche": ok(0.965, 0.035, 112.4),
  "akzent-vordergrund": ok(0.2, 0.04, 112.4),
  "status-abgeschlossen": ok(0.46, 0.095, 166),
  "status-abgeschlossen-flaeche": ok(0.96, 0.024, 166),
  "status-abgeschlossen-kontur": ok(0.85, 0.05, 166),
  "status-fehler": ok(0.52, 0.19, 27),
  // GEDAEMPFT am 04.09.2026 (E-88), von ok(0.96, 0.028, 27) — weniger Chroma,
  // eine Spur kuehler. Damit zugleich IM sRGB-Farbraum: offener Punkt 123 zu.
  "status-fehler-flaeche": ok(0.96, 0.016, 22),
  "status-fehler-kontur": ok(0.86, 0.07, 27),
  "status-offen": ok(0.44, 0, 0),
  "status-offen-flaeche": ok(0.96, 0, 0),
  "status-offen-kontur": ok(0.87, 0, 0),
  // 0.58 bis zum 03.09.2026 — verfehlte die 4,5 : 1 dreifach (M131).
  "status-ungeklaert": ok(0.55, 0, 0),
  "status-ungeklaert-flaeche": ok(0.98, 0, 0),
  "status-ungeklaert-kontur": ok(0.9, 0, 0),
  // ── neu, Schritt 10b-3a ──
  ueberfaellig: ok(0.52, 0.105, 80),
  "ueberfaellig-flaeche": ok(0.96, 0.028, 80),
  "ueberfaellig-kontur": ok(0.65, 0.13, 80),
  // ── neu, 04.09.2026 (E-87) ──
  "verlauf-flaeche": ok(0.62, 0.118, 230),
  "verlauf-kontur": ok(0.45, 0.085, 230),
  border: ok(0.9, 0, 0),
};

const DUNKEL = {
  card: ok(0.21, 0, 0),
  background: ok(0.16, 0, 0),
  muted: ok(0.27, 0, 0),
  akzent: ok(0.777, 0.1643, 112.4),
  "akzent-schrift": ok(0.83, 0.15, 112.4),
  "akzent-flaeche": ok(0.28, 0.06, 112.4),
  "akzent-vordergrund": ok(0.2, 0.04, 112.4),
  "status-abgeschlossen": ok(0.75, 0.13, 166),
  "status-abgeschlossen-flaeche": ok(0.26, 0.04, 166),
  "status-abgeschlossen-kontur": ok(0.4, 0.07, 166),
  "status-fehler": ok(0.7, 0.17, 27),
  // GEDAEMPFT am 04.09.2026 (E-88), von ok(0.26, 0.05, 27) — getrennt gerechnet.
  "status-fehler-flaeche": ok(0.26, 0.035, 22),
  "status-fehler-kontur": ok(0.4, 0.09, 27),
  "status-offen": ok(0.78, 0, 0),
  "status-offen-flaeche": ok(0.25, 0, 0),
  "status-offen-kontur": ok(0.35, 0, 0),
  "status-ungeklaert": ok(0.62, 0, 0),
  "status-ungeklaert-flaeche": ok(0.22, 0, 0),
  "status-ungeklaert-kontur": ok(0.32, 0, 0),
  // ── neu, Schritt 10b-3a ──
  ueberfaellig: ok(0.7, 0.14, 80),
  "ueberfaellig-flaeche": ok(0.26, 0.05, 80),
  "ueberfaellig-kontur": ok(0.53, 0.105, 80),
  // ── neu, 04.09.2026 (E-87). DERSELBE Flaechenwert wie im hellen Block, wie
  //    beim Akzent; die Kontur kehrt sich um. Beides getrennt nachgerechnet. ──
  "verlauf-flaeche": ok(0.62, 0.118, 230),
  "verlauf-kontur": ok(0.78, 0.11, 230),
};

/* --border ist im Dunkelblock oklch(1 0 0 / 12%) — halbdurchlaessig und damit
   keine Farbe, sondern eine Deckung. Was die Linie zeigt, ist Weiss zu 12 %
   ueber --card. GERECHNET und nicht eingetippt. */
DUNKEL.border = ueber(ok(1, 0, 0), DUNKEL.card, 0.12);

/** Die fuenf Farbrollen — vier Statusrollen und die Problemkategorie. */
const ROLLEN = [
  "status-abgeschlossen",
  "status-fehler",
  "status-offen",
  "status-ungeklaert",
  "ueberfaellig",
];

/** Die vier Akzentstufen. Sie sind Gegenstand der Rechnung, nicht ihr Bezug. */
const AKZENTSTUFEN = ["akzent", "akzent-schrift", "akzent-vordergrund", "akzent-flaeche"];

/**
 * Die zwei Stufen der Verlaufsflaeche (E-87, 04.09.2026). Sie sind KEINE Rolle im
 * Sinne von ROLLEN: Sie tragen keine fachliche Aussage und stehen deshalb nicht in
 * lib/status-farbe.ts, sondern in features/dashboard/verlauf.ts. Gerechnet werden
 * sie trotzdem — eine Flaeche ohne Bedeutung darf nicht aussehen wie eine mit.
 */
const VERLAUFSSTUFEN = ["verlauf-flaeche", "verlauf-kontur"];

// ─── Lauf ────────────────────────────────────────────────────────────────────

let fehlgeschlagen = 0;
const pruefe = (bedingung, text) => {
  if (!bedingung) fehlgeschlagen++;
  console.log(`  ${bedingung ? "OK  " : "NEIN"}  ${text}`);
};

console.log("=== GEGENPROBE: die Zahlen aus §3 mit derselben Methode ===\n");
{
  const soll = [
    ["akzent auf card", HELL.akzent, HELL.card, 1.98],
    ["akzent-schrift auf card", HELL["akzent-schrift"], HELL.card, 5.4],
    ["akzent-schrift auf background", HELL["akzent-schrift"], HELL.background, 5.18],
    ["akzent-schrift auf akzent-flaeche", HELL["akzent-schrift"], HELL["akzent-flaeche"], 4.91],
    ["akzent-vordergrund auf akzent", HELL["akzent-vordergrund"], HELL.akzent, 9.13],
    ["status-abgeschlossen auf card", HELL["status-abgeschlossen"], HELL.card, 6.78],
  ];
  for (const [name, x, y, erwartet] of soll) {
    const k = kontrast(x, y).roh;
    pruefe(Math.abs(k - erwartet) < 0.005, `${name}: ${zahl(k)} : 1 (§3 nennt ${zahl(erwartet)})`);
  }
  const dGruenAkzent = abstand(HELL["status-abgeschlossen"], HELL.akzent);
  const dAkzentRot = abstand(HELL.akzent, HELL["status-fehler"]);
  pruefe(Math.abs(dGruenAkzent - 0.343) < 0.0005, `Abstand Gruen–Akzent: ${zahl(dGruenAkzent, 4)}`);
  pruefe(Math.abs(dAkzentRot - 0.352) < 0.0005, `Abstand Akzent–Rot:   ${zahl(dAkzentRot, 4)}`);
  const hexe = [
    ["akzent", HELL.akzent, "#b9c022"],
    ["akzent-schrift", HELL["akzent-schrift"], "#6a6f0f"],
    ["akzent-flaeche", HELL["akzent-flaeche"], "#f3f6dc"],
    ["akzent-vordergrund", HELL["akzent-vordergrund"], "#161802"],
    ["status-abgeschlossen", HELL["status-abgeschlossen"], "#01684c"],
  ];
  for (const [name, farbe, erwartet] of hexe) {
    pruefe(srgb(farbe).hex === erwartet, `${name} = ${srgb(farbe).hex} (§3 nennt ${erwartet})`);
  }
}

console.log("\n=== GEGENPROBE: die sechs Hexwerte aus §7a ===\n");
{
  // Sie stehen dort doppelt belegt: einmal aus dem Lauf dieses Skripts und
  // einmal aus dem Hex-Rueckfall, den Lightning CSS beim Bauen schreibt. Wenn
  // die Methode wandert, wandern sie mit — und dann ist die Methode und nicht
  // das Ergebnis die offene Frage.
  const hexe = [
    ["hell   --ueberfaellig", HELL.ueberfaellig, "#886108"],
    ["hell   --ueberfaellig-flaeche", HELL["ueberfaellig-flaeche"], "#fcf0dd"],
    ["hell   --ueberfaellig-kontur", HELL["ueberfaellig-kontur"], "#b88513"],
    ["dunkel --ueberfaellig", DUNKEL.ueberfaellig, "#cb9317"],
    ["dunkel --ueberfaellig-flaeche", DUNKEL["ueberfaellig-flaeche"], "#312103"],
    ["dunkel --ueberfaellig-kontur", DUNKEL["ueberfaellig-kontur"], "#8b640f"],
  ];
  for (const [name, farbe, erwartet] of hexe) {
    pruefe(srgb(farbe).hex === erwartet, `${name} = ${srgb(farbe).hex} (§7a nennt ${erwartet})`);
  }
}

// ─── Die Erhebung: beide Bloecke, fuenf Rollen, vier Akzentstufen ────────────

for (const [name, P] of [
  ["HELL", HELL],
  ["DUNKEL", DUNKEL],
]) {
  console.log(`\n\n########## ${name} ##########\n`);

  console.log("--- Die Werte ---\n");
  const zeige = (rolle) => {
    const f = P[rolle];
    const s = srgb(f);
    console.log(
      `  --${rolle.padEnd(30)} oklch(${f.L} ${f.C} ${f.h})`.padEnd(60) +
        `  ${s.hex}${s.imRaum ? "" : "   *** AUSSERHALB sRGB ***"}`,
    );
  };
  for (const rolle of ROLLEN) {
    for (const teil of ["", "-flaeche", "-kontur"]) zeige(`${rolle}${teil}`);
  }
  for (const stufe of AKZENTSTUFEN) zeige(stufe);
  console.log("\n  … und der Bezug, uebernommen und nicht gerechnet (E-61):\n");
  for (const basis of ["card", "background", "muted"]) zeige(basis);

  console.log(`\n--- SCHWELLE 1: jeder Vordergrund >= 4,5 : 1 (Bedingung) ---\n`);
  for (const rolle of ROLLEN) {
    for (const [was, grund] of [
      [`${rolle}-flaeche`, "auf der eigenen Flaeche"],
      ["card", "auf --card"],
      ["background", "auf --background"],
    ]) {
      const k = kontrast(P[rolle], P[was]);
      pruefe(
        k.roh >= 4.5,
        `--${rolle} ${grund}`.padEnd(50) + ` ${zahl(k.roh)} : 1  (aus dem Hexwert ${zahl(k.acht)})`,
      );
      pruefe(
        k.roh >= 4.5 === k.acht >= 4.5,
        `${"  … gerundet und ungerundet auf derselben Seite".padEnd(50)}`,
      );
    }
  }

  console.log(`\n--- SCHWELLE 1: die vier Akzentstufen (Bedingung nur, wo sie Schrift sind) ---\n`);
  for (const [text, x, y, schwelle] of [
    ["--akzent-schrift auf --card", P["akzent-schrift"], P.card, 4.5],
    ["--akzent-schrift auf --background", P["akzent-schrift"], P.background, 4.5],
    ["--akzent-schrift auf --akzent-flaeche", P["akzent-schrift"], P["akzent-flaeche"], 4.5],
    ["--akzent-vordergrund auf --akzent", P["akzent-vordergrund"], P.akzent, 4.5],
  ]) {
    const k = kontrast(x, y);
    pruefe(
      k.roh >= schwelle,
      `${text.padEnd(50)} ${zahl(k.roh)} : 1  (aus dem Hexwert ${zahl(k.acht)})`,
    );
    pruefe(
      k.roh >= schwelle === k.acht >= schwelle,
      `${"  … gerundet und ungerundet auf derselben Seite".padEnd(50)}`,
    );
  }
  // --akzent ist eine FUELLFARBE und keine Schriftfarbe (§3). Sein Kontrast auf
  // --card wird berichtet und nicht gefordert; auf Weiss sind es 1,98 : 1, und
  // genau deshalb gibt es die drei anderen Stufen.
  {
    const k = kontrast(P.akzent, P.card);
    console.log(
      `  ----  ${"--akzent auf --card (Fuellfarbe, Bericht)".padEnd(50)} ${zahl(k.roh)} : 1`,
    );
  }

  console.log(
    `\n--- SCHWELLE 3: Fehler und Ueberfaellig auf derselben Helligkeit (Bedingung) ---\n`,
  );
  pruefe(
    P.ueberfaellig.L === P["status-fehler"].L,
    `--ueberfaellig L=${P.ueberfaellig.L} == --status-fehler L=${P["status-fehler"].L}  (Regel Q3)`,
  );

  console.log(`\n--- Alle Werte liegen im sRGB-Farbraum (Bedingung, OHNE Ausnahme) ---\n`);
  {
    // HIER STAND EINE AUSNAHME, und sie ist am 04.09.2026 ENTFALLEN statt gelockert:
    // `hell --status-fehler-flaeche` lag seit Schritt 3 ausserhalb des Farbraums
    // (Ueberschuss 0,021112, hier festgenagelt), wurde abgeschnitten und zeigte
    // #ffebe8 = oklch(0,9555 0,0221 29,4) — also weder die eingetragene Helligkeit
    // noch den eingetragenen Ton. Mit der Daempfung aus E-88 (Chroma 0,028 -> 0,016)
    // liegt der Wert im Farbraum. OFFENER PUNKT 123 IST GESCHLOSSEN, und die
    // Bedingung gilt jetzt fuer jeden Wert ohne Ausnahme.
    for (const rolle of ROLLEN) {
      for (const teil of ["", "-flaeche", "-kontur"]) {
        pruefe(srgb(P[`${rolle}${teil}`]).imRaum, `--${rolle}${teil} liegt im sRGB-Farbraum`);
      }
    }
    for (const stufe of [...AKZENTSTUFEN, ...VERLAUFSSTUFEN]) {
      pruefe(srgb(P[stufe]).imRaum, `--${stufe} liegt im sRGB-Farbraum`);
    }
  }

  console.log(`\n--- SCHWELLE 5: die Konturen auf --card (BERICHT, keine Bedingung) ---\n`);
  for (const rolle of ROLLEN) {
    const n = `${rolle}-kontur`;
    const k = kontrast(P[n], P.card).roh;
    console.log(
      `  --${n.padEnd(30)} ${srgb(P[n]).hex}  ${zahl(k)} : 1  ` +
        `${k >= 3 ? "erfuellt" : "verfehlt"} (WCAG 1.4.11)`,
    );
  }

  console.log(`\n--- SCHWELLE 4: OKLab-Abstand jeder Rolle zum Akzent (Bedingung) ---\n`);
  {
    // NACHGEREICHT am 03.09.2026: Bis dahin hat dieser Abschnitt nur BERICHTET.
    // Schwelle 4 stand aber als Abbruchbedingung vorregistriert — „Verbesserung
    // ist Ertrag, Verschlechterung ist Abbruchbedingung" —, und eine
    // Abbruchbedingung, die nichts abbricht, ist keine. Gemessen war die Luecke:
    // Der dunkle --status-abgeschlossen von Ton 166 auf 140 gedreht faellt auf
    // 0,0823 (30 % unter der Untergrenze), und der Lauf endete trotzdem mit
    // „Alle Bedingungen erfuellt." Aufgefallen ist es in der Durchsicht zu
    // Schritt 11a, nicht beim Schreiben.
    //
    // DIE ZAHL IST GEMESSEN UND NICHT GEWAEHLT: Sie ist der Abstand
    // --ueberfaellig → --akzent im Dunkelblock, erhoben am 31.08.2026
    // (visuelles-konzept.md §7a). Sie sagt nicht „gut", sie sagt „nicht
    // schlechter als damals" — das Vergleichsmass aus §3 ist 0,343, und keine
    // Strecke zum Akzent erreicht im Dunkeln die Haelfte davon.
    //
    // UNGERUNDET und nicht als 0.117: §7a nennt „0,117", der wirkliche Wert ist
    // 0,1169659…, also 3,4 Hunderttausendstel darunter. Gegen die gerundete Zahl
    // geprueft faellt der Bestand an seiner eigenen Untergrenze durch — und eine
    // Bedingung, die den unveraenderten Stand nicht besteht, wird beim naechsten
    // Aufraeumen entschaerft statt verstanden. Die Zahl steht deshalb mit so
    // vielen Stellen da, wie sie hat.
    const UNTERGRENZE = 0.1169659;
    for (const rolle of ROLLEN) {
      const d = abstand(P[rolle], P.akzent);
      pruefe(
        d >= UNTERGRENZE,
        `--${rolle.padEnd(30)} → --akzent   ${zahl(d, 4)}  (>= ${zahl(UNTERGRENZE, 3)})`,
      );
    }
  }

  console.log(`\n--- Die OKLab-Abstaende der Rollen untereinander (Bericht) ---\n`);
  for (let i = 0; i < ROLLEN.length; i++) {
    for (let j = i + 1; j < ROLLEN.length; j++) {
      const d = abstand(P[ROLLEN[i]], P[ROLLEN[j]]);
      console.log(`  ${`--${ROLLEN[i]} → --${ROLLEN[j]}`.padEnd(60)} ${zahl(d, 4)}`);
    }
  }
  console.log("\n  … und dieselben Strecken zwischen den Flaechen:\n");
  for (let i = 0; i < ROLLEN.length; i++) {
    for (let j = i + 1; j < ROLLEN.length; j++) {
      const a = `${ROLLEN[i]}-flaeche`;
      const b = `${ROLLEN[j]}-flaeche`;
      console.log(`  ${`--${a} → --${b}`.padEnd(60)} ${zahl(abstand(P[a], P[b]), 4)}`);
    }
  }

  console.log(
    `\n--- Offener Punkt 92: die gedrueckte Schaltflaeche (Messung, keine Bedingung) ---\n`,
  );
  {
    // `toggleVariants` faerbt den gedrueckten Zustand mit `bg-muted`; darunter
    // liegt die Seitenflaeche `--background`. Im Hellen sind das 1,07 : 1
    // (`docs/dashboard-frontend.md` §10.5). Gemessen, nicht behoben — die
    // Gestalt gehoert `components/ui/toggle-group.tsx` und damit dem
    // Generatorbereich.
    const k = kontrast(P.muted, P.background);
    console.log(
      `  --muted ${srgb(P.muted).hex} auf --background ${srgb(P.background).hex}` +
        `  ${zahl(k.roh)} : 1  (aus dem Hexwert ${zahl(k.acht)})`,
    );
  }
}

// ─── Die Bedingungen, die nur die Rolle --ueberfaellig betreffen ─────────────

console.log("\n\n########## --ueberfaellig: der Rahmen aus §7a ##########\n");
for (const [name, P] of [
  ["HELL", HELL],
  ["DUNKEL", DUNKEL],
]) {
  console.log(`--- ${name} ---\n`);
  const fg = P.ueberfaellig;
  const paare = [
    ["--ueberfaellig auf --card", fg, P.card, 4.5],
    ["--ueberfaellig auf --background", fg, P.background, 4.5],
    ["--ueberfaellig auf --ueberfaellig-flaeche", fg, P["ueberfaellig-flaeche"], 4.5],
    ["--ueberfaellig-kontur auf --card", P["ueberfaellig-kontur"], P.card, 3],
  ];
  for (const [text, x, y, schwelle] of paare) {
    const k = kontrast(x, y);
    pruefe(
      k.roh >= schwelle,
      `${text.padEnd(42)} ${zahl(k.roh)} : 1  (>= ${zahl(schwelle)}; aus dem Hexwert ${zahl(k.acht)})`,
    );
    pruefe(
      k.roh >= schwelle === k.acht >= schwelle,
      `${"  … gerundet und ungerundet auf derselben Seite".padEnd(42)}`,
    );
  }
  pruefe(P.ueberfaellig.h <= 85, `Ton ${P.ueberfaellig.h} <= 85`);
  console.log("");
}

console.log("=== Warum der Ton nicht auf 85 gelegt worden ist ===\n");
{
  // Groesste Chroma, die bei dieser Helligkeit und diesem Ton noch in sRGB
  // liegt. Auf den Rand gesetzt wuerde die Farbe bei jedem abweichenden
  // Rechenweg abgeschnitten — deshalb steht daneben, was gewaehlt worden ist.
  const maxChroma = (L, h) => {
    let lo = 0;
    let hi = 0.45;
    for (let i = 0; i < 60; i++) {
      const m = (lo + hi) / 2;
      if (srgb(ok(L, m, h)).imRaum) lo = m;
      else hi = m;
    }
    return lo;
  };
  for (const [block, L, gewaehlt, bezug] of [
    ["hell  ", 0.52, 0.105, HELL["status-fehler"]],
    ["dunkel", 0.7, 0.14, DUNKEL["status-fehler"]],
  ]) {
    for (const h of [80, 85]) {
      const c = Math.floor(maxChroma(L, h) * 10000) / 10000;
      const f = ok(L, h === 80 ? gewaehlt : c, h);
      console.log(
        `  ${block} L=${L} Ton ${h}: Chroma hoechstens ${zahl(c, 4)}` +
          `, gerechnet mit ${zahl(f.C, 4)} → Abstand zu Rot ${zahl(abstand(f, bezug), 4)}`,
      );
    }
  }
}

// ─── E-87 und E-88: die Verlaufsflaeche und die gedaempfte Fehlerflaeche ─────
//
// Vorgegeben waren ABSTAENDE, keine Werte (Auftrag vom 04.09.2026, Korrektur
// desselben Tages). Die sechs stehen hier einzeln, in BEIDEN Bloecken getrennt —
// der Dunkelblock ist keine Umkehrung des hellen.
//
// GERECHNET WIRD GEGEN DAS, WAS GEMALT WIRD. Das Token ist die volle Farbe;
// auf dem Schirm steht sie nur durch die beiden Stopp-Deckungen hindurch. Ein
// Abstand gegen das Token beschriebe eine Flaeche, die niemand sieht.
//
// UNTERGRENZE fuer "unterscheidbar" ist die Sichtprobe A.2 aus
// docs/visuelles-konzept.md §7a: 0,025 in OKLab sind dort als RANGFOLGE gelesen
// worden. Was darueber liegt, ist sichtbar verschieden; das ist die Schranke,
// nicht das Ziel.
//
// DIE 3 : 1 AUS WCAG 1.4.11 HAENGEN AN DER KONTUR und nicht an der Flaeche. Die
// Linie traegt den Kurvenverlauf — sie ist die Aussage —, die Flaeche traegt
// Gewicht. Der Kontrast der Flaeche wird BERICHTET und nicht gefordert.

console.log("\n\n########## E-87 / E-88: die sechs Abstaende ##########");

const SICHTPROBE = 0.025;
/**
 * Die engste Strecke des Bestands: --ueberfaellig -> --akzent im Dunkelblock
 * (docs/visuelles-konzept.md §7a, Befund 3). Ungerundet, aus demselben Grund,
 * aus dem sie oben bei SCHWELLE 4 ungerundet steht.
 */
const ENGSTE_STRECKE = 0.1169659;

/* Die Deckung der beiden Farbverlaufsstopps, aus app/globals.css. Sie sind je
   Block verschieden: Dieselbe Deckung traegt auf Weiss weniger auf als auf
   --card 0.21. */
HELL.deckungOben = 0.35;
HELL.deckungUnten = 0.03;
DUNKEL.deckungOben = 0.28;
DUNKEL.deckungUnten = 0.04;

for (const [name, P] of [
  ["HELL", HELL],
  ["DUNKEL", DUNKEL],
]) {
  const token = P["verlauf-flaeche"];
  const kontur = P["verlauf-kontur"];
  const kachel = P["status-fehler-flaeche"];
  const gemalt = ueber(token, P.card, P.deckungOben);
  const fuss = ueber(token, P.card, P.deckungUnten);

  console.log(`\n--- ${name}  (Stopps ${P.deckungOben} / ${P.deckungUnten}) ---\n`);
  console.log(`  Token unvermischt        ${srgb(token).hex}`);
  console.log(
    `  GEMALTER oberer Stopp    ${srgb(gemalt).hex}  ` +
      `oklch(${zahl(gemalt.L, 4)} ${zahl(gemalt.C, 4)} ${zahl(gemalt.h, 1)})`,
  );
  console.log(`  GEMALTER Fuss            ${srgb(fuss).hex}\n`);

  // 1. Flaeche ↔ Kartenhintergrund am oberen Stopp. KEINE Kontrastbedingung —
  //    die steht bei 7 an der Kontur. Hier zaehlt, ob die Toenung ueberhaupt zu
  //    sehen ist.
  {
    const d = abstand(gemalt, P.card);
    const k = kontrast(gemalt, P.card);
    const akzentGemalt = ueber(P.akzent, P.card, P.deckungOben);
    pruefe(
      d >= SICHTPROBE,
      `1  gemalter Stopp ↔ --card            ${zahl(d, 4)} OKLab  ` +
        `(Kontrast ${zahl(k.roh)} : 1 — BERICHT, keine Bedingung)`,
    );
    console.log(
      `  ----  … derselbe Stopp mit --akzent`.padEnd(52) +
        `${zahl(abstand(akzentGemalt, P.card), 4)} / ${zahl(kontrast(akzentGemalt, P.card).roh)} : 1`,
    );
  }

  // 2. Flaeche ↔ Gitterlinien. Die Linie liegt UNTER der Flaeche (nachgesehen an
  //    den Recharts-Lagen im DOM, nicht angenommen), also haengt alles an der
  //    Deckung: gemessen wird der Abstand zwischen "Flaeche ueber Linie" und
  //    "Flaeche ueber Karte". Der SCHLECHTESTE Fall ist der obere Stopp.
  {
    const beiOben = abstand(
      ueber(token, P.border, P.deckungOben),
      ueber(token, P.card, P.deckungOben),
    );
    const beiUnten = abstand(
      ueber(token, P.border, P.deckungUnten),
      ueber(token, P.card, P.deckungUnten),
    );
    pruefe(
      Math.min(beiOben, beiUnten) >= SICHTPROBE,
      `2  Gitterlinie unter der Flaeche      ${zahl(beiOben, 4)} am oberen Stopp, ` +
        `${zahl(beiUnten, 4)} am Fuss`,
    );
    console.log(
      `  ----  … mit voller Deckung waere sie`.padEnd(52) +
        `${zahl(abstand(ueber(token, P.border, 1), ueber(token, P.card, 1)), 4)} — unsichtbar`,
    );
  }

  // 3. Flaeche ↔ --status-offen. Die neutrale Statusrolle: zu nah, und die
  //    Flaeche laese sich wieder als Statusaussage. GEGEN DEN GEMALTEN STOPP,
  //    denn das ist, was jemand sieht. Der Wert des Tokens steht daneben — er
  //    ist die Schranke fuer den Tag, an dem jemand die Deckung hochdreht.
  {
    const d = abstand(gemalt, P["status-offen"]);
    pruefe(
      d >= SICHTPROBE,
      `3  gemalter Stopp ↔ --status-offen    ${zahl(d, 4)} OKLab  ` +
        `(das Token allein: ${zahl(abstand(token, P["status-offen"]), 4)})`,
    );
    let engste = [null, Infinity];
    for (const rolle of ROLLEN) {
      const dr = abstand(gemalt, P[rolle]);
      if (dr < engste[1]) engste = [rolle, dr];
      console.log(
        `  ----  … zu --${rolle}`.padEnd(52) +
          `${zahl(dr, 4)}   (Token: ${zahl(abstand(token, P[rolle]), 4)})`,
      );
    }
    pruefe(
      engste[1] >= ENGSTE_STRECKE,
      `3b engste Strecke zu einer Rolle: --${engste[0]}  ${zahl(engste[1], 4)}  ` +
        `(>= ${zahl(ENGSTE_STRECKE, 4)}, der engsten des Bestands)`,
    );
  }

  // 4. Flaeche ↔ Fehlerkachelflaeche. HIER IST DER BUNTTON DAS MASS und nicht
  //    die Helligkeit. Der Ton des gemalten Stopps weicht leicht vom Ton des
  //    Tokens ab — Mischen im linearen Licht dreht ihn ein paar Grad —, deshalb
  //    steht hier der gemessene und nicht die 230 aus der Deklaration.
  {
    const roh = Math.abs(gemalt.h - kachel.h);
    const dTon = Math.min(roh, 360 - roh);
    pruefe(
      dTon >= 90 && abstand(gemalt, kachel) >= SICHTPROBE,
      `4  gemalter Stopp ↔ Fehlerkachel      ${dTon.toFixed(1)} Grad Buntton, ` +
        `${zahl(abstand(gemalt, kachel), 4)} OKLab`,
    );
  }

  // 5. Kachelflaeche ↔ Zahl und Text darauf. Die Schwelle aus SCHWELLE 1 gilt
  //    unveraendert; sie steht hier ein zweites Mal, weil der Auftrag sie
  //    ausdruecklich nachgerechnet sehen wollte. Dazu die Frage, ob die Kachel
  //    ueberhaupt noch als GEFUELLT gelesen wird.
  {
    const k = kontrast(P["status-fehler"], kachel);
    pruefe(
      Math.min(k.roh, k.acht) >= 4.5,
      `5  --status-fehler auf der Kachel     ${zahl(k.roh)} : 1  (aus dem Hexwert ${zahl(k.acht)})`,
    );
    const d = abstand(kachel, P.card);
    pruefe(
      d >= SICHTPROBE,
      `5b Kachelflaeche ↔ --card             ${zahl(d, 4)} OKLab — die Kachel liest sich weiter als gefuellt`,
    );
  }

  // 6. Banding. Der Farbverlauf laeuft ueber die DECKUNG vom oberen Stopp bis
  //    zum Fuss; ueber die Hoehe der Flaeche (gemessen 245 px) ergibt das so
  //    viele 8-Bit-Stufen je Pixel. BERICHT — was das Auge sieht, entscheidet
  //    die Sichtprobe, und die ist am laufenden System gefahren worden.
  {
    const hoehe = 245;
    const a = srgb(token).acht;
    const c = srgb(P.card).acht;
    const groesste = Math.max(...a.map((v, i) => Math.abs(v - c[i])));
    const proPixel = (groesste * (P.deckungOben - P.deckungUnten)) / hoehe;
    console.log(
      `  ----  6  Banding`.padEnd(52) +
        `${proPixel.toFixed(3)} Stufen/px → eine Stufe alle ${(1 / proPixel).toFixed(1)} px`,
    );
  }

  // 7. DIE KONTUR — hier und nur hier gilt die 3 : 1 aus WCAG 1.4.11. Sie liegt
  //    auf der Grenze zwischen Flaeche und Karte und muss gegen beide bestehen.
  {
    const kCard = kontrast(kontur, P.card);
    pruefe(
      Math.min(kCard.roh, kCard.acht) >= 3,
      `7  Kontur auf --card                  ${zahl(kCard.roh)} : 1  ` +
        `(aus dem Hexwert ${zahl(kCard.acht)}; WCAG 1.4.11)`,
    );
    pruefe(
      abstand(kontur, gemalt) >= SICHTPROBE,
      `7b Kontur ↔ gemalter oberer Stopp     ${zahl(abstand(kontur, gemalt), 4)} OKLab`,
    );
    pruefe(
      abstand(kontur, fuss) >= SICHTPROBE,
      `7c Kontur ↔ gemalter Fuss             ${zahl(abstand(kontur, fuss), 4)} OKLab`,
    );
  }

  console.log(
    `\n  Die Hexwerte: Token ${srgb(token).hex}, Kontur ${srgb(kontur).hex}, ` +
      `Kachelflaeche ${srgb(kachel).hex}`,
  );
}

console.log(
  `\n${fehlgeschlagen === 0 ? "Alle Bedingungen erfuellt." : `${fehlgeschlagen} Bedingung(en) verfehlt.`}`,
);
process.exit(fehlgeschlagen === 0 ? 0 : 1);
