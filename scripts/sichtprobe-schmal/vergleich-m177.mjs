#!/usr/bin/env node
/**
 * Die Gegenprobe zu Teil 2 (M177): dieselben Routen, Breiten und Mandanten wie M176,
 * **vorher** (die Ergebnisdateien aus Teil 1) gegen **nachher** (derselbe Messkern
 * `messung.js`, nach dem Bau, unter `ergebnis/rahmen/nachher/`).
 *
 *   node scripts/sichtprobe-schmal/vergleich-m177.mjs            → Markdown
 *   node scripts/sichtprobe-schmal/vergleich-m177.mjs --json     → JSON
 *
 * Zwei Fragen, und sie sind getrennt:
 *
 * 1. **Halten die Zusagen, die vorher hielten, unverändert?** — kein waagerechter
 *    Überlauf am Dokument (`scrollWidth` = `innerWidth`), das Dokument scrollt nicht,
 *    kein Scrollbereich außer `main` kommt dazu, dieselbe Zeilenzahl der Kopfzeile, der
 *    Mandantencode sichtbar, das Suchfeld in derselben Lage.
 *
 *    Gezählt wird in drei Körben, und nur der erste ist ein Bruch:
 *    - **gebrochen** — eine Zusage hält nachher nicht mehr
 *    - **bekannt** — die Zeilenzahl der Kopfzeile ab 768 px: Die Dateien aus Teil 1 tragen
 *      dort noch die Zählung des alten Kerns (zwei Zeilen), die M176 selbst als Werkzeugfehler
 *      führt und mit einer Zeile ausweist (`messungen-sichtprobe-schmal.md` §1, Abweichung 6)
 *    - **weniger** — ein Scrollbereich, der vorher scrollte, scrollt nachher nicht, weil der
 *      Inhalt kürzer geworden ist; kein neuer ist dazugekommen
 *
 * 2. **Was ist an den Tabellen anders?** — Kasten, Tabellenbreite, die Kopfzellen mit ihrer
 *    Breite, 0-px-Spalten mit Text, Kopfschnitte, gekürzte Zellen ohne `title`; dazu die
 *    gekürzten Werte außerhalb von Tabellen ohne `title` (Punkte 176 und 177) und die Höhe
 *    von `main` (die Zeilenhöhen des Katalogs).
 *
 * Keine Zelltexte: Die Kopfbeschriftungen sind Oberflächentexte der Anwendung.
 */
import { existsSync, readdirSync, readFileSync, statSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const HIER = path.dirname(fileURLToPath(import.meta.url));
const RAHMEN = path.join(HIER, "ergebnis", "rahmen");
const ALS_JSON = process.argv.includes("--json");

const lade = (datei) => (existsSync(datei) ? JSON.parse(readFileSync(datei, "utf8")) : null);

const MANDANTEN = ["NEXANS", "VOTG"];
const routen = new Set();
for (const m of MANDANTEN) {
  const wurzel = path.join(RAHMEN, "nachher", m);
  if (!existsSync(wurzel)) continue;
  for (const n of readdirSync(wurzel)) if (n.endsWith(".json") && statSync(path.join(wurzel, n)).isFile()) routen.add(n.replace(/\.json$/, ""));
}

const aktiv = (m) => m.scroller.senkrecht.filter((s) => s.scrollt).map((s) => (s.istMain ? "main" : s.pfad));

const zusagen = (m) => ({
  ueberlauf: m.ueberlauf.waagerecht,
  dokumentScrollt: m.scroller.dokumentScrollt,
  scroller: aktiv(m),
  kopfzeilenZeilen: m.kopfzeile?.anzahlZeilen ?? null,
  mandantSichtbar: m.kopfzeile?.mandantSichtbar ?? null,
  suchfeldEigeneZeile: m.kopfzeile?.suchfeld?.eigeneZeile ?? null,
});

const tabellen = (m) =>
  m.tabellen.map((t) => ({
    kasten: t.huelle.breite,
    kastenScroll: t.huelle.scrollWidth,
    tabelle: t.breite,
    kopf: t.kopf.map((k) => `${k.text}:${k.w}`),
    nullbreit: t.nullbreit.length,
    schnitte: t.schnitte.length,
    ohneTitle: t.abgeschnitten.ohneTitle,
  }));

const ausserhalb = (m) => ({
  gekuerzt: m.gekuerztAusserhalb.anzahl,
  // Die Beispiele sind auf sechs begrenzt; bei höchstens sechs gekürzten Werten ist die Zahl vollständig.
  ohneTitle: m.gekuerztAusserhalb.beispiele.filter((b) => !b.title).length,
  vollstaendig: m.gekuerztAusserhalb.anzahl <= 6,
});

const ergebnis = [];
const gebrochen = [];
const bekannt = [];
const weniger = [];
for (const m of MANDANTEN) {
  for (const route of [...routen].sort()) {
    const vorher = lade(path.join(RAHMEN, m, `${route}.json`));
    const nachher = lade(path.join(RAHMEN, "nachher", m, `${route}.json`));
    if (!nachher) {
      ergebnis.push({ mandant: m, route, fehlt: "nachher" });
      continue;
    }
    for (const breite of Object.keys(nachher.voll).map(Number).sort((a, b) => a - b)) {
      const n = nachher.voll[breite];
      const v = vorher?.voll?.[breite] ?? null;
      const zn = zusagen(n);
      const zv = v ? zusagen(v) : null;
      const ort = `${m}/${route}@${breite}`;
      if (zn.ueberlauf !== 0) gebrochen.push(`${ort}: waagerechter Überlauf ${zn.ueberlauf}`);
      if (zn.dokumentScrollt) gebrochen.push(`${ort}: das Dokument scrollt`);
      const fremd = zn.scroller.filter((s) => s !== "main" && !(zv?.scroller ?? []).includes(s));
      if (fremd.length > 0) gebrochen.push(`${ort}: neuer Scrollbereich ${fremd.join(", ")}`);
      if (zv) {
        const weg = zv.scroller.filter((s) => !zn.scroller.includes(s));
        if (weg.length > 0) weniger.push(`${ort}: scrollt nicht mehr: ${weg.join(", ")}`);
        if (zv.kopfzeilenZeilen !== zn.kopfzeilenZeilen) {
          const altkern = breite >= 768 && zv.kopfzeilenZeilen === 2 && zn.kopfzeilenZeilen === 1;
          (altkern ? bekannt : gebrochen).push(`${ort}: Kopfzeile ${zv.kopfzeilenZeilen} → ${zn.kopfzeilenZeilen} Zeilen`);
        }
        for (const k of ["mandantSichtbar", "suchfeldEigeneZeile"]) {
          if (zv[k] !== zn[k]) gebrochen.push(`${ort}: ${k} ${zv[k]} → ${zn[k]}`);
        }
      }
      if (breite < 768 && (zn.kopfzeilenZeilen !== 3 || zn.mandantSichtbar !== true || zn.suchfeldEigeneZeile !== true)) {
        gebrochen.push(`${ort}: Kopfzeile unter 768 px — ${zn.kopfzeilenZeilen} Zeilen, Mandant ${zn.mandantSichtbar}, Suchfeld eigene Zeile ${zn.suchfeldEigeneZeile}`);
      }
      ergebnis.push({
        mandant: m,
        route,
        breite,
        zusagenVorher: zv,
        zusagenNachher: zn,
        tabellenVorher: v ? tabellen(v) : null,
        tabellenNachher: tabellen(n),
        ausserhalbVorher: v ? ausserhalb(v) : null,
        ausserhalbNachher: ausserhalb(n),
        mainVorher: v?.scroller.main?.scrollHeight ?? null,
        mainNachher: n.scroller.main?.scrollHeight ?? null,
      });
    }
  }
}

if (ALS_JSON) {
  process.stdout.write(JSON.stringify({ gebrochen, bekannt, weniger, ergebnis }, null, 2));
} else {
  const z = ["# M177 — Gegenprobe vorher / nachher", ""];
  z.push(`**Gebrochene Zusagen: ${gebrochen.length}** · bekannt aus M176: ${bekannt.length} · scrollt nicht mehr: ${weniger.length}`, "");
  for (const a of gebrochen) z.push(`- **gebrochen** ${a}`);
  for (const a of bekannt) z.push(`- bekannt ${a}`);
  for (const a of weniger) z.push(`- weniger ${a}`);
  let route = null;
  for (const e of ergebnis) {
    if (e.fehlt) {
      z.push("", `## ${e.mandant}/${e.route} — nachher fehlt`);
      continue;
    }
    const kennung = `${e.mandant}/${e.route}`;
    if (kennung !== route) {
      route = kennung;
      z.push(
        "",
        `## ${kennung}`,
        "",
        "| Breite | Überlauf v→n | Scroller v→n | Kopfzeile v→n | `main`-Höhe v→n | gekürzt außerhalb ohne `title` v→n | Tabelle (Kasten / Breite / 0-px / Schnitte / ohne title) vorher | nachher | Kopf nachher |",
        "|---:|---|---|---|---|---|---|---|---|",
      );
    }
    const tv = (e.tabellenVorher ?? []).map((t) => `${t.kasten} / ${t.tabelle} / ${t.nullbreit} / ${t.schnitte} / ${t.ohneTitle}`).join("; ") || "—";
    const tn = e.tabellenNachher.map((t) => `${t.kasten} / ${t.tabelle} / ${t.nullbreit} / ${t.schnitte} / ${t.ohneTitle}`).join("; ") || "—";
    const kn = e.tabellenNachher.map((t) => t.kopf.join(" · ")).join("; ") || "—";
    const zv = e.zusagenVorher;
    const zn = e.zusagenNachher;
    const av = e.ausserhalbVorher;
    const an = e.ausserhalbNachher;
    z.push(
      `| ${e.breite} | ${zv?.ueberlauf ?? "—"}→${zn.ueberlauf} | ${zv?.scroller.length ?? "—"}→${zn.scroller.length} | ${zv?.kopfzeilenZeilen ?? "—"}→${zn.kopfzeilenZeilen} | ${e.mainVorher ?? "—"}→${e.mainNachher} | ${av ? av.ohneTitle + (av.vollstaendig ? "" : "+") : "—"}→${an.ohneTitle}${an.vollstaendig ? "" : "+"} | ${tv} | ${tn} | ${kn} |`,
    );
  }
  console.log(z.join("\n"));
}
