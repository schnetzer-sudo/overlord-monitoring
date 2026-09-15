#!/usr/bin/env node
/**
 * Auswertung der Rahmen-Ergebnisse (`ergebnis/rahmen/<mandant>/<route>.json`,
 * geschrieben vom Empfänger): je Route eine Tabelle über die Breiten, darunter
 * die Befunde je Breite — dieselben Regeln wie `befunde()` in `sichtprobe.mjs`,
 * nur aus Dateien statt aus dem laufenden Lauf.
 *
 *   node scripts/sichtprobe-schmal/auswertung.mjs [ergebnis/rahmen] > zusammenfassung.md
 */
import { readdirSync, readFileSync, statSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const HIER = path.dirname(fileURLToPath(import.meta.url));
const WURZEL = path.resolve(process.argv[2] ?? path.join(HIER, "ergebnis", "rahmen"));

const dateien = [];
(function sammeln(d) {
  for (const n of readdirSync(d)) {
    const p = path.join(d, n);
    if (statSync(p).isDirectory()) sammeln(p);
    else if (n.endsWith(".json")) dateien.push(p);
  }
})(WURZEL);

const REIHENFOLGE = ["dashboard", "nachrichtenliste", "nachrichtenliste-panel", "nachrichtendetail", "prozessansicht", "prozessansicht-prozess", "belegsuche", "benutzerverwaltung", "prozess-katalog", "anmeldung"];
const laeufe = dateien
  .map((p) => ({ p, ...JSON.parse(readFileSync(p, "utf8")) }))
  .sort((a, b) => a.kennung.localeCompare(b.kennung))
  .sort((a, b) => REIHENFOLGE.indexOf(a.kennung.split("/")[1]) - REIHENFOLGE.indexOf(b.kennung.split("/")[1]))
  .sort((a, b) => a.kennung.split("/")[0].localeCompare(b.kennung.split("/")[0]));

const z = [];
for (const lauf of laeufe) {
  const breiten = Object.keys(lauf.voll)
    .map(Number)
    .sort((a, b) => a - b);
  z.push(`## ${lauf.kennung} — \`${lauf.voll[breiten[0]].url}\` *(${lauf.zeit})*`, "");
  z.push(`| Breite | Dok. Überlauf | main.clientWidth | Kopfzeile | Mandant | Suchfeld | Scroller aktiv/angelegt | Dok. scrollt | Tabellen | Kopfschnitte | 0-px-Spalten | gekürzt (ohne title) | Ziele < 44 |`);
  z.push(`|---:|---:|---:|---|---|---|---|---|---:|---:|---:|---:|---|`);
  for (const b of breiten) {
    const m = lauf.voll[b];
    const k = m.kopfzeile;
    z.push(
      `| ${m.masse.w}×${m.masse.h} | ${m.ueberlauf.waagerecht} px | ${m.scroller.main?.clientWidth ?? "—"} | ${k ? `${k.anzahlZeilen} Zeilen, ${k.hoehe} px` : "—"} | ${k ? (k.mandantSichtbar ? `${k.mandantCode} sichtbar` : `${k.mandantCode ?? "—"} NICHT sichtbar`) : "—"} | ${k?.suchfeld ? (k.suchfeld.eigeneZeile ? "eigene Zeile" : "geteilt") : "—"} | ${m.scroller.senkrechtAktiv}/${m.scroller.senkrechtAngelegt} | ${m.scroller.dokumentScrollt ? "ja" : "nein"} | ${m.tabellen.length} | ${m.tabellen.reduce((s, t) => s + t.schnitte.length, 0)} | ${m.tabellen.reduce((s, t) => s + t.nullbreit.length, 0)} | ${m.tabellen.reduce((s, t) => s + t.abgeschnitten.zellen, 0)} (${m.tabellen.reduce((s, t) => s + t.abgeschnitten.ohneTitle, 0)}) | ${m.ziele.unter44}/${m.ziele.gesamt} |`,
    );
  }
  z.push("");
  for (const b of breiten) {
    const m = lauf.voll[b];
    const f = befunde(m, m.masse);
    z.push(`**${m.masse.w} px** — ${f.length} Befund${f.length === 1 ? "" : "e"}`);
    for (const x of f) z.push(`- ${x.art}: ${x.text}`);
    for (const t of m.tabellen) {
      z.push(`- Tabelle ${t.pfad}: Hülle ${t.huelle.breite} px (scrollWidth ${t.huelle.scrollWidth}, ${t.huelle.overflowX}), Tabelle ${t.breite} px, ${t.zeilen} Zeilen; Kopf: ${t.kopf.map((c) => `${c.text} ${c.w} px${c.abgeschnitten ? " ✂" : ""} (Text ${c.textW} px ab x=${c.textX})`).join(" · ")}`);
      if (t.abgeschnitten.beispiele.length) z.push(`  - gekürzt: ${t.abgeschnitten.beispiele.map((e) => `„${e.text}" ${e.breite} px${e.title ? " +title" : " OHNE title"}`).join("; ")}`);
    }
    if (m.kopfzeile) z.push(`- Kopfzeile: ${m.kopfzeile.zeilen.map((r) => `[y=${r.y}: ${r.inhalte.join(" | ")}]`).join(" ")}; Menüschalter ${m.kopfzeile.menueSchalter ? `${m.kopfzeile.menueSchalter.w}×${m.kopfzeile.menueSchalter.h}` : "—"}; Navspalte ${m.kopfzeile.navSpalteSichtbar ? "sichtbar" : "verborgen"}`);
    if (m.ziele.gruppen.length) z.push(`- kleine Ziele (${m.ziele.nurHoehe} nur Höhe, ${m.ziele.nurBreite} nur Breite, ${m.ziele.beides} beides): ${m.ziele.gruppen.slice(0, 12).map((g) => `${g.name} ${g.w}×${g.h}${g.anzahl > 1 ? ` (×${g.anzahl})` : ""}`).join("; ")}`);
    if (m.scroller.senkrecht.length) z.push(`- Scrollbereiche: ${m.scroller.senkrecht.map((s) => `${s.pfad} ${s.scrollHeight}/${s.clientHeight}${s.scrollt ? " scrollt" : ""}`).join("; ")}`);
    if (m.ueberlauf.beispiele.length) z.push(`- überstehend (${m.ueberlauf.ueberstehendeGesamt}, davon ${m.ueberlauf.ohneHuelle} ohne Hülle): ${m.ueberlauf.beispiele.map((u) => `${u.pfad} bis ${u.rechts}${u.inHuelle ? ` (in ${u.inHuelle})` : ""}`).join("; ")}`);
    if (m.gekuerztAusserhalb.anzahl) z.push(`- gekürzt außerhalb von Tabellen: ${m.gekuerztAusserhalb.anzahl} — ${m.gekuerztAusserhalb.beispiele.map((e) => `${e.pfad} „${e.text}"${e.title ? " +title" : " OHNE title"}`).join("; ")}`);
    z.push("");
  }
}
process.stdout.write(z.join("\n"));

function befunde(m, masse) {
  const liste = [];
  if (m.ueberlauf.waagerecht !== 0) liste.push({ art: "ueberlauf", text: `Dokument ${m.ueberlauf.waagerecht > 0 ? "+" : ""}${m.ueberlauf.waagerecht} px breiter als das Fenster` });
  if (m.ueberlauf.ohneHuelle > 0) liste.push({ art: "ueberstehend", text: `${m.ueberlauf.ohneHuelle} sichtbare Elemente ragen ohne Scrollhülle über den rechten Rand` });
  for (const t of m.tabellen) {
    for (const s of t.schnitte) liste.push({ art: "schnitt", text: `„${s.a}" und „${s.b}" überlappen: Zellen ${s.zellenSchnitt} px, Text ${s.textSchnitt} px` });
    for (const n of t.nullbreit) liste.push({ art: "nullbreit", text: `Spalte „${n.text}" ist ${n.breite} px breit und zeichnet ${n.textbreite} px Text ab x=${n.textVon}` });
    if (t.abgeschnitten.ohneTitle > 0) liste.push({ art: "ohne-title", text: `${t.abgeschnitten.ohneTitle} gekürzte Zellen ohne title (von ${t.abgeschnitten.zellen})` });
    if (t.huelle.scrollWidth > t.huelle.clientWidth + 1) liste.push({ art: "tabelle-scrollt", text: `Tabelle (${t.breite} px) scrollt waagerecht in ihrer Hülle (${t.huelle.clientWidth} px)` });
  }
  const fremde = m.scroller.senkrecht.filter((s) => s.scrollt && !s.istMain);
  if (fremde.length > 0) liste.push({ art: "scroller", text: `${fremde.length} senkrechter Scrollbereich neben main: ${fremde.map((s) => s.pfad).join(", ")}` });
  if (m.scroller.dokumentScrollt && m.kopfzeile) liste.push({ art: "dokument-scrollt", text: `Das Dokument selbst scrollt (${m.dokument.scrollHeight} > ${m.dokument.clientHeight})` });
  if (m.kopfzeile && masse.w < 768) {
    if (m.kopfzeile.anzahlZeilen !== 3) liste.push({ art: "kopfzeile", text: `Kopfzeile hat ${m.kopfzeile.anzahlZeilen} statt 3 Zeilen` });
    if (!m.kopfzeile.mandantSichtbar) liste.push({ art: "mandant", text: `Mandantencode ${m.kopfzeile.mandantCode ?? "(fehlt)"} nicht sichtbar` });
    if (m.kopfzeile.suchfeld && !m.kopfzeile.suchfeld.eigeneZeile) liste.push({ art: "suchfeld", text: "Suchfeld teilt sich die Zeile" });
    if (m.kopfzeile.navSpalteSichtbar) liste.push({ art: "navspalte", text: "Navigationsspalte unter 768 px sichtbar" });
  }
  if (m.ziele.unter44 > 0) liste.push({ art: "ziele", text: `${m.ziele.unter44} von ${m.ziele.gesamt} Zielen unter 44 px (${m.ziele.nurHoehe} nur Höhe, ${m.ziele.nurBreite} nur Breite, ${m.ziele.beides} beides)` });
  return liste;
}
