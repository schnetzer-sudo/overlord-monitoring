/**
 * Die Farbrolle `--ueberfaellig` — nachgerechnet, nicht geschaetzt.
 *
 * `docs/visuelles-konzept.md` §8 sagt: „Nachrechnen ist woertlich gemeint."
 * Dieses Skript ist das Woertlichnehmen. Es wandelt OKLCH nach sRGB, rechnet
 * WCAG-Kontraste und OKLab-Abstaende und prueft jede Bedingung, die der Rahmen
 * aus §7a der Rolle stellt. Die Zahlen in §7a stammen aus seinem Lauf.
 *
 *   node scripts/farbrolle-ueberfaellig/rechne.mjs
 *
 * Es braucht kein Paket und keine Installation — nur Node.
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

// ─── Die Palette, wie sie in src/app/globals.css steht ───────────────────────

const HELL = {
  card: ok(1, 0, 0),
  background: ok(0.985, 0, 0),
  akzent: ok(0.777, 0.1643, 112.4),
  "akzent-schrift": ok(0.52, 0.11, 112.4),
  "akzent-flaeche": ok(0.965, 0.035, 112.4),
  "akzent-vordergrund": ok(0.2, 0.04, 112.4),
  "status-abgeschlossen": ok(0.46, 0.095, 166),
  "status-abgeschlossen-flaeche": ok(0.96, 0.024, 166),
  "status-abgeschlossen-kontur": ok(0.85, 0.05, 166),
  "status-fehler": ok(0.52, 0.19, 27),
  "status-fehler-flaeche": ok(0.96, 0.028, 27),
  "status-fehler-kontur": ok(0.86, 0.07, 27),
  "status-offen-kontur": ok(0.87, 0, 0),
  "status-ungeklaert-kontur": ok(0.9, 0, 0),
  // ── neu, Schritt 10b-3a ──
  ueberfaellig: ok(0.52, 0.105, 80),
  "ueberfaellig-flaeche": ok(0.96, 0.028, 80),
  "ueberfaellig-kontur": ok(0.65, 0.13, 80),
};

const DUNKEL = {
  card: ok(0.21, 0, 0),
  background: ok(0.16, 0, 0),
  akzent: ok(0.777, 0.1643, 112.4),
  "status-abgeschlossen": ok(0.75, 0.13, 166),
  "status-abgeschlossen-flaeche": ok(0.26, 0.04, 166),
  "status-abgeschlossen-kontur": ok(0.4, 0.07, 166),
  "status-fehler": ok(0.7, 0.17, 27),
  "status-fehler-flaeche": ok(0.26, 0.05, 27),
  "status-fehler-kontur": ok(0.4, 0.09, 27),
  // ── neu, Schritt 10b-3a ──
  ueberfaellig: ok(0.7, 0.14, 80),
  "ueberfaellig-flaeche": ok(0.26, 0.05, 80),
  "ueberfaellig-kontur": ok(0.53, 0.105, 80),
};

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

for (const [name, P, weiss] of [
  ["HELL", HELL, true],
  ["DUNKEL", DUNKEL, false],
]) {
  console.log(`\n=== ${name}: die drei Werte ===\n`);
  for (const rolle of ["ueberfaellig", "ueberfaellig-flaeche", "ueberfaellig-kontur"]) {
    const f = P[rolle];
    const s = srgb(f);
    console.log(
      `  --${rolle.padEnd(20)} oklch(${f.L} ${f.C} ${f.h})  ${s.hex}` +
        `${s.imRaum ? "" : "   *** AUSSERHALB sRGB ***"}`,
    );
  }

  console.log(`\n--- ${name}: Bedingungen ---\n`);
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
  for (const rolle of ["ueberfaellig", "ueberfaellig-flaeche", "ueberfaellig-kontur"]) {
    pruefe(srgb(P[rolle]).imRaum, `--${rolle} liegt im sRGB-Farbraum`);
  }
  pruefe(P.ueberfaellig.h <= 85, `Ton ${P.ueberfaellig.h} <= 85`);

  console.log(`\n--- ${name}: OKLab-Abstaende (Bericht, keine Bedingung) ---\n`);
  const strecken = [
    ["--ueberfaellig → --akzent", fg, P.akzent],
    ["--ueberfaellig → --status-fehler", fg, P["status-fehler"]],
    ["--ueberfaellig → --status-abgeschlossen", fg, P["status-abgeschlossen"]],
    ["  … zum Vergleich, dieselbe Palette unter sich:", null, null],
    ["--status-abgeschlossen → --akzent", P["status-abgeschlossen"], P.akzent],
    ["--akzent → --status-fehler", P.akzent, P["status-fehler"]],
    ["--status-abgeschlossen → --status-fehler", P["status-abgeschlossen"], P["status-fehler"]],
    ["  … und die Flaechen, die als Kacheln nebeneinanderstehen:", null, null],
    [
      "--ueberfaellig-flaeche → --status-fehler-flaeche",
      P["ueberfaellig-flaeche"],
      P["status-fehler-flaeche"],
    ],
    [
      "--status-abgeschlossen-flaeche → --status-fehler-flaeche",
      P["status-abgeschlossen-flaeche"],
      P["status-fehler-flaeche"],
    ],
  ];
  for (const [text, x, y] of strecken) {
    if (x === null) console.log(text);
    else console.log(`  ${text.padEnd(56)} ${zahl(abstand(x, y), 4)}`);
  }
}

console.log("\n=== Warum der Ton nicht auf 85 gelegt worden ist ===\n");
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

console.log("\n=== Die bestehenden Konturen an der Schwelle aus WCAG 1.4.11 ===\n");
for (const rolle of [
  "status-fehler-kontur",
  "status-abgeschlossen-kontur",
  "status-offen-kontur",
  "status-ungeklaert-kontur",
  "ueberfaellig-kontur",
]) {
  const k = kontrast(HELL[rolle], HELL.card).roh;
  console.log(
    `  --${rolle.padEnd(28)} ${srgb(HELL[rolle]).hex}  ${zahl(k)} : 1  ${k >= 3 ? "erfuellt" : "verfehlt"}`,
  );
}

console.log(
  `\n${fehlgeschlagen === 0 ? "Alle Bedingungen erfuellt." : `${fehlgeschlagen} Bedingung(en) verfehlt.`}`,
);
process.exit(fehlgeschlagen === 0 ? 0 : 1);
