#!/usr/bin/env node
/**
 * Auswertung des Spaltenbedarfs (Sichtprobe am schmalen Fenster, Teil 2, M177).
 *
 * Liest die Ergebnisse von `spaltenbedarf.js` (`ergebnis/rahmen/bedarf/<Mandant>/<Lauf>.json`)
 * und der Typenmessung (`ergebnis/rahmen/typen/<Mandant>.json`) und fasst sie je Tabelle und
 * Spalte über alle Läufe zusammen: beide Mandanten, beide Sprachen, jede gemessene Seite.
 *
 *   node scripts/sichtprobe-schmal/auswertung-bedarf.mjs [ergebnis/rahmen/bedarf]          → Markdown
 *   node scripts/sichtprobe-schmal/auswertung-bedarf.mjs [ergebnis/rahmen/bedarf] --json   → JSON
 *
 * Je Spalte, in Pixeln bei der Wurzelschrift des Laufs:
 *   kopf       die längere der beiden Kopfbeschriftungen samt Innenabstand und Sortierpfeil
 *   einzeilig  der längste Zellinhalt in einer Zeile (mit dem Lauf, aus dem er stammt)
 *   unteilbar  das längste Stück, das ohne Notumbruch nicht kleiner wird
 *   plakette   die Statusplakette allein
 *   rechnung   die Rechnung über die endliche Menge (Einordnungen, Kettenrollen, Ziffern, Wörter)
 *
 * **Und die Mindestbreite nach den Regeln aus `docs/spaltenwahl.md` §3** — die Zahl, die im
 * Code steht. Sie wird hier berechnet und nicht abgetippt:
 *
 *   Grundregel    das Größte aus kopf, unteilbar und rechnung
 *   status        kopf, plakette, rechnung — ohne den Zusatz „Schritt: …", der unter der
 *                 Plakette bewusst kürzt (nachrichtenliste.md §8.1)
 *   kette         kopf, unteilbar und die Rollen **einzeln** — die Verkettung aller vier
 *                 steht in keiner Zeile und zählt nicht
 *   treffer       kopf, unteilbar und die längste Belegart-Bezeichnung des Mandanten allein
 *   Zeitpunkt, Status, Ablauf
 *                 dieselbe Zelle in Nachrichten- und Trefferliste, also dieselbe Zahl:
 *                 das Größte aus beiden Tabellen
 *
 * Aufgerundet auf ganze Pixel. Keine Zelltexte — die Dateien tragen keine (G1).
 */
import { existsSync, readdirSync, readFileSync, statSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const HIER = path.dirname(fileURLToPath(import.meta.url));
const argumente = process.argv.slice(2);
const WURZEL = path.resolve(argumente.find((a) => !a.startsWith("--")) ?? path.join(HIER, "ergebnis", "rahmen", "bedarf"));
const TYPEN = path.join(WURZEL, "..", "typen");
const ALS_JSON = argumente.includes("--json");

const sammeln = (d, liste = []) => {
  if (!existsSync(d)) return liste;
  for (const n of readdirSync(d)) {
    const p = path.join(d, n);
    if (statSync(p).isDirectory()) sammeln(p, liste);
    else if (n.endsWith(".json")) liste.push(p);
  }
  return liste;
};

const groesser = (alt, wert, lauf, zusatz = {}) =>
  wert !== null && wert !== undefined && (alt === null || wert > alt.wert) ? { wert, lauf, ...zusatz } : alt;

const tabellen = new Map();
const laeufe = [];
for (const datei of sammeln(WURZEL).sort()) {
  const { kennung, zeit, bedarf } = JSON.parse(readFileSync(datei, "utf8"));
  const lauf = kennung.replace(/^bedarf\//, "");
  laeufe.push({ lauf, zeit, sprache: bedarf.sprache, wurzel: bedarf.wurzel, dichte: bedarf.dichte, tabellen: bedarf.tabellen.map((t) => `${t.name ?? "?"} (${t.zeilen ?? 0})`) });
  for (const t of bedarf.tabellen) {
    if (!t.name) continue;
    const spalten = tabellen.get(t.name) ?? new Map();
    tabellen.set(t.name, spalten);
    for (const s of t.spalten) {
      const a =
        spalten.get(s.schluessel) ??
        { schluessel: s.schluessel, art: s.art, index: s.index, wurzel: bedarf.wurzel, zeilen: 0, kopf: null, einzeilig: null, unteilbar: null, plakette: null, rechnung: null, einzeln: null };
      a.zeilen += s.zellen.einzeilig.n;
      a.kopf = groesser(a.kopf, s.kopf.bedarfMax, lauf);
      a.einzeilig = groesser(a.einzeilig, s.zellen.einzeilig.max, lauf, { p50: s.zellen.einzeilig.p50, p90: s.zellen.einzeilig.p90, zeichen: s.zellen.einzeilig.zeichen });
      a.unteilbar = groesser(a.unteilbar, s.zellen.unteilbar.max, lauf, { zeichen: s.zellen.unteilbar.zeichen });
      a.plakette = groesser(a.plakette, s.zellen.plakette.max, lauf);
      if (s.rechnung) {
        a.rechnung = groesser(a.rechnung, s.rechnung.max, lauf, { art: s.rechnung.art, ziffer: s.rechnung.ziffer ?? null });
        if (s.rechnung.art === "kette") {
          // Die Rollen einzeln: alle Einträge bis auf den letzten, die Verkettung.
          const einzeln = Math.max(...["de", "en"].flatMap((spr) => s.rechnung.je[spr].slice(0, -1).map((x) => x.breite)));
          a.einzeln = groesser(a.einzeln, einzeln, lauf);
        }
      }
      spalten.set(s.schluessel, a);
    }
  }
}

const typen = sammeln(TYPEN).map((p) => JSON.parse(readFileSync(p, "utf8")));
const typAllein = typen.length === 0 ? null : Math.max(...typen.map((t) => t.allein));

const kurz = (schluessel) => schluessel.split(".").pop();
const ohneRechnung = (s) => Math.max(...[s.kopf, s.unteilbar].filter(Boolean).map((x) => x.wert));

/** Die Mindestbreite je Spalte nach den Regeln im Kopf — vor dem Aufrunden. */
function roh(name, s) {
  const k = kurz(s.schluessel);
  if (s.art === "status") return Math.max(...[s.kopf, s.plakette, s.rechnung].filter(Boolean).map((x) => x.wert));
  if (s.art === "kette") return Math.max(ohneRechnung(s), s.einzeln?.wert ?? 0);
  if (name === "trefferliste" && k === "treffer") return Math.max(ohneRechnung(s), typAllein ?? 0);
  return Math.max(...[s.kopf, s.unteilbar, s.rechnung].filter(Boolean).map((x) => x.wert));
}

const GETEILT = ["zeitpunkt", "status", "ablauf"];
const mindestbreiten = {};
for (const [name, spalten] of tabellen) {
  mindestbreiten[name] = {};
  for (const s of spalten.values()) mindestbreiten[name][kurz(s.schluessel)] = roh(name, s);
}
for (const k of GETEILT) {
  const beide = ["nachrichtenliste", "trefferliste"].map((n) => mindestbreiten[n]?.[k]).filter((x) => x !== undefined);
  if (beide.length === 0) continue;
  const groesste = Math.max(...beide);
  for (const n of ["nachrichtenliste", "trefferliste"]) if (mindestbreiten[n]?.[k] !== undefined) mindestbreiten[n][k] = groesste;
}
const aufgerundet = Object.fromEntries(
  Object.entries(mindestbreiten).map(([n, s]) => [n, Object.fromEntries(Object.entries(s).map(([k, v]) => [k, { roh: Math.round(v * 100) / 100, px: Math.ceil(v - 1e-9) }]))]),
);

const zahl = (x) => (x === null || x === undefined ? "—" : (Math.round(x * 100) / 100).toLocaleString("de-DE"));

if (ALS_JSON) {
  process.stdout.write(
    JSON.stringify(
      {
        laeufe,
        typen,
        mindestbreiten: aufgerundet,
        tabellen: Object.fromEntries([...tabellen].map(([n, s]) => [n, [...s.values()].sort((a, b) => a.index - b.index)])),
      },
      null,
      2,
    ),
  );
} else {
  const z = ["# Spaltenbedarf — Auswertung (M177)", "", `${laeufe.length} Läufe aus \`${path.relative(process.cwd(), WURZEL)}\``, ""];
  z.push("| Lauf | Zeit | Sprache | Wurzel | Dichte | Tabellen (Zeilen) |", "|---|---|---|---:|---|---|");
  for (const l of laeufe) z.push(`| ${l.lauf} | ${l.zeit} | ${l.sprache} | ${l.wurzel} | ${l.dichte} | ${l.tabellen.join(", ")} |`);
  if (typen.length > 0) {
    z.push("", "## Belegart-Bezeichnungen (Spalte „Treffer\")", "", "| Mandant | Typen | längste allein | mit „ +1\" |", "|---|---:|---:|---:|");
    for (const t of typen) z.push(`| ${t.kennung} | ${t.typen} | ${zahl(t.allein)} | ${zahl(t.mitEins)} |`);
  }
  for (const [name, spalten] of tabellen) {
    z.push("", `## ${name}`, "");
    z.push("| Spalte | Art | Zeilen | Kopf | einzeilig (Lauf) | p50 / p90 | unteilbar | Plakette | Rechnung | **Mindestbreite** |");
    z.push("|---|---|---:|---:|---|---|---:|---:|---|---:|");
    for (const s of [...spalten.values()].sort((a, b) => a.index - b.index)) {
      const m = aufgerundet[name][kurz(s.schluessel)];
      const rechnung = s.rechnung
        ? `${zahl(s.rechnung.wert)} (${s.rechnung.art}${s.rechnung.ziffer !== null ? ", Ziffer " + s.rechnung.ziffer : ""}${s.einzeln ? ", einzeln " + zahl(s.einzeln.wert) : ""})`
        : "—";
      z.push(
        `| ${kurz(s.schluessel)} | ${s.art ?? ""} | ${s.zeilen} | ${zahl(s.kopf?.wert)} | ${zahl(s.einzeilig?.wert)} (${s.einzeilig?.lauf ?? "—"}, ${s.einzeilig?.zeichen ?? "—"} Z.) | ${zahl(s.einzeilig?.p50)} / ${zahl(s.einzeilig?.p90)} | ${zahl(s.unteilbar?.wert)} | ${zahl(s.plakette?.wert)} | ${rechnung} | **${m.px}** (${zahl(m.roh)}) |`,
      );
    }
  }
  console.log(z.join("\n"));
}
