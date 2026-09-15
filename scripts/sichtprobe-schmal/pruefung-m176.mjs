#!/usr/bin/env node
/**
 * Prüflauf zu M176: hält die Zahlen aus `docs/messungen-sichtprobe-schmal.md`
 * gegen die Ergebnisdateien des Laufs vom 15.09.2026 (`ergebnis/rahmen/…`,
 * `ergebnis/…-OFFEN-messung.json`). Jede Aussage ist eine Zeile; weicht eine
 * ab, steht es hier und nicht in der Doku. Gedacht für genau diesen Lauf —
 * nach Teil 2 gelten andere Zahlen, und dann gehört hier eine neue Fassung her.
 *
 *   node scripts/sichtprobe-schmal/pruefung-m176.mjs
 */
import { readdirSync, readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const HIER = path.dirname(fileURLToPath(import.meta.url));
const ERG = path.join(HIER, "ergebnis");
const L = (m, n) => JSON.parse(readFileSync(path.join(ERG, "rahmen", m, `${n}.json`), "utf8")).voll;

let n = 0;
let f = 0;
const p = (name, ist, soll) => {
  n++;
  if (JSON.stringify(ist) !== JSON.stringify(soll)) {
    f++;
    console.log("ABWEICHUNG", name, "ist", JSON.stringify(ist), "soll", JSON.stringify(soll));
  }
};
const kopf = (m, b, i) => m[b].tabellen[0].kopf[i];
const N = (M) => M === "NEXANS";

for (const M of ["NEXANS", "VOTG"]) {
  const l = L(M, "nachrichtenliste");
  p(`${M} Liste 360 Kasten`, l[360].tabellen[0].huelle.clientWidth, 334);
  p(`${M} Liste 360 Tabelle`, l[360].tabellen[0].breite, 352);
  p(`${M} Liste 360 Ablauf`, [kopf(l, 360, 2).w, kopf(l, 360, 2).textX, kopf(l, 360, 2).textW], [0, 373, 43.2]);
  p(`${M} Liste 390`, [l[390].tabellen[0].breite, kopf(l, 390, 2).w], [364, 12]);
  p(`${M} Liste 430`, [l[430].tabellen[0].breite, kopf(l, 430, 2).w], [404, 52]);
  p(`${M} Liste 744`, [l[744].tabellen[0].huelle.clientWidth, l[744].tabellen[0].breite, kopf(l, 744, 2).w], [718, 718, 366]);
  p(`${M} Liste 768`, [l[768].tabellen[0].huelle.clientWidth, l[768].tabellen[0].breite, kopf(l, 768, 2).w, kopf(l, 768, 3).w, kopf(l, 768, 2).x, kopf(l, 768, 3).x, l[768].tabellen[0].schnitte[0].textSchnitt], [518, 640, 0, 288, 581, 581, 43.2]);
  p(`${M} Liste scrollWidth (hängt an den Daten)`, [l[360].tabellen[0].huelle.scrollWidth, l[430].tabellen[0].huelle.scrollWidth, l[744].tabellen[0].huelle.scrollWidth], N(M) ? [591, 591, 718] : [647, 647, 718]);
  p(`${M} Liste main`, [l[360].scroller.main.scrollHeight, l[360].scroller.main.clientHeight, l[768].scroller.main.clientWidth], [2172, 609, 560]);
  p(`${M} Liste Ziele`, [`${l[360].ziele.unter44}/${l[360].ziele.gesamt}`, `${l[768].ziele.unter44}/${l[768].ziele.gesamt}`], ["69/69", "72/72"]);
  p(`${M} Liste gekürzt`, [l[360].tabellen[0].abgeschnitten.zellen, l[360].tabellen[0].abgeschnitten.ohneTitle, l[430].tabellen[0].abgeschnitten.zellen, l[430].tabellen[0].abgeschnitten.ohneTitle], [151, 1, 150, 0]);
  p(`${M} Kopfzeile`, [l[360].kopfzeile.hoehe, l[360].kopfzeile.anzahlZeilen, l[360].kopfzeile.zeilen.map((z) => z.y), l[768].kopfzeile.hoehe, l[744].kopfzeile.anzahlZeilen, l[360].kopfzeile.mandantSichtbar, l[360].kopfzeile.suchfeld.eigeneZeile], [131, 3, [8, 48, 90], 51, 3, true, true]);
  p(`${M} 768 Produktname ohne title`, l[768].gekuerztAusserhalb.beispiele.filter((e) => !e.title).map((e) => e.text), ["Overlord Monitoring"]);

  const d = L(M, "dashboard");
  p(`${M} Dashboard Ziele`, [`${d[360].ziele.unter44}/${d[360].ziele.gesamt}`, `${d[768].ziele.unter44}/${d[768].ziele.gesamt}`], N(M) ? ["20/22", "23/25"] : ["17/19", "20/22"]);
  p(`${M} Dashboard main`, [d[360].scroller.main.scrollHeight, d[768].scroller.main.scrollHeight], N(M) ? [2009, 1721] : [1776, 1496]);
  p(`${M} Dashboard Überlauf`, [360, 390, 430, 744, 768].map((b) => d[b].ueberlauf.waagerecht), [0, 0, 0, 0, 0]);

  const pa = L(M, "nachrichtenliste-panel");
  const de = L(M, "nachrichtendetail");
  p(`${M} Panel main`, [pa[360].scroller.main.scrollHeight, pa[360].scroller.main.clientHeight], [609, 609]);
  p(`${M} Detail Ziele`, [de[360].ziele.unter44, de[768].ziele.unter44], N(M) ? [20, 23] : [15, 18]);
  p(`${M} Detail ohne title`, [360, 390, 430].map((b) => de[b].gekuerztAusserhalb.beispiele.filter((e) => !e.title).length).concat([de[360].gekuerztAusserhalb.anzahl, de[390].gekuerztAusserhalb.anzahl]), N(M) ? [1, 1, 0, 5, 4] : [2, 1, 0, 5, 4]);

  const pr = L(M, "prozessansicht");
  p(`${M} Baum`, [pr[360].scroller.main.scrollHeight, `${pr[360].ziele.unter44}/${pr[360].ziele.gesamt}`, `${pr[768].ziele.unter44}/${pr[768].ziele.gesamt}`, pr[768].scroller.main.scrollHeight], N(M) ? [8488, "243/243", "240/247", 8592] : [5032, "147/147", "148/151", 5056]);

  const pp = L(M, "prozessansicht-prozess");
  const t = pp[768].tabellen[0];
  const klebend = pp[768].scroller.senkrecht.find((s) => s.scrollt && !s.istMain);
  p(`${M} Prozess 768`, [t.huelle.clientWidth, t.breite, t.kopf[2].w, t.kopf[3].w, t.schnitte[0].textSchnitt, pp[768].scroller.senkrechtAktiv, `${klebend.scrollHeight}/${klebend.clientHeight}`, `${pp[768].ziele.unter44}/${pp[768].ziele.gesamt}`], [294, 640, 0, 288, 43.2, 2, "1944/867", N(M) ? "295/312" : "202/221"]);
  p(`${M} Prozess unter md = Liste`, [360, 390, 430, 744].map((b) => pp[b].tabellen[0].breite), [352, 364, 404, 718]);

  const s = L(M, "belegsuche");
  p(`${M} Suche`, [s[360].tabellen[0].huelle.clientWidth, s[360].tabellen[0].breite, s[360].tabellen[0].kopf.map((k) => k.w), s[744].tabellen[0].kopf.map((k) => k.w), s[768].tabellen[0].huelle.clientWidth, s[768].tabellen[0].kopf[4].w, s[768].tabellen[0].kopf[4].textX], [334, 696, [184, 168, 208, 136], [189.81, 173.3, 214.56, 140.33], 518, 0, 933]);
  p(`${M} Suche gekürzt/Ziele`, [s[360].tabellen[0].abgeschnitten.zellen, s[360].tabellen[0].abgeschnitten.ohneTitle, s[768].tabellen[0].abgeschnitten.zellen, s[768].tabellen[0].abgeschnitten.ohneTitle, `${s[360].ziele.unter44}/${s[360].ziele.gesamt}`, `${s[768].ziele.unter44}/${s[768].ziele.gesamt}`], N(M) ? [155, 0, 206, 1, "58/60", "61/63"] : [2, 0, 4, 1, "9/11", "12/14"]);

  const b = L(M, "benutzerverwaltung");
  p(`${M} Benutzer`, [b[360].tabellen[0].huelle.clientWidth, b[360].tabellen[0].breite, b[360].tabellen[0].kopf.map((k) => k.w), b[768].tabellen[0].huelle.clientWidth, b[768].tabellen[0].breite, b[768].tabellen[0].kopf.map((k) => k.w), b[768].tabellen[0].schnitte[0].textSchnitt, b[768].tabellen[0].abgeschnitten.zellen, b[768].tabellen[0].abgeschnitten.ohneTitle, b[360].tabellen[0].abgeschnitten.ohneTitle, b[360].tabellen[0].kopf[4].textW, b[360].scroller.main.scrollHeight, b[768].scroller.main.scrollHeight, b[360].ziele.unter44, b[768].ziele.unter44], [336, 488, [128, 96, 112, 96, 56], 520, 792, [176, 144, 0, 120, 176, 120, 56], 46.59, 14, 8, 1, 76.89, 617, 1007, 16, 19]);

  const k = L(M, "prozess-katalog");
  p(`${M} Katalog`, [k[360].tabellen[0].huelle.clientWidth, k[360].tabellen[0].breite, k[360].tabellen[0].kopf.map((x) => x.w), k[744].tabellen[0].kopf.map((x) => x.w), k[768].tabellen[0].huelle.clientWidth, k[768].tabellen[0].breite, k[768].tabellen[0].kopf.map((x) => x.w), k[768].tabellen[0].schnitte[0].textSchnitt, k[768].tabellen[0].abgeschnitten.zellen, k[768].tabellen[0].abgeschnitten.ohneTitle, k[360].tabellen[0].abgeschnitten.zellen, k[360].tabellen[0].abgeschnitten.ohneTitle, `${k[360].ziele.unter44}/${k[360].ziele.gesamt}`, `${k[768].ziele.unter44}/${k[768].ziele.gesamt}`, k[360].scroller.main.scrollHeight, k[768].scroller.main.scrollHeight], N(M) ? [336, 336, [144, 96, 96], [192, 432, 96], 520, 696, [256, 0, 128, 176, 136], 52.52, 1467, 734, 733, 0, "744/747", "747/750", 70048, 185708] : [336, 336, [144, 96, 96], [192, 432, 96], 520, 696, [256, 0, 128, 176, 136], 52.52, 781, 391, 390, 0, "401/404", "404/407", 44653, 75925]);
  if (!N(M)) p(`${M} Katalog 744 main`, k[744].scroller.main.scrollHeight, 35851);

  for (const r of ["dashboard", "nachrichtenliste", "nachrichtenliste-panel", "nachrichtendetail", "prozessansicht", "prozessansicht-prozess", "belegsuche", "benutzerverwaltung", "prozess-katalog"]) {
    const v = L(M, r);
    p(`${M} ${r} Überlauf 0, Dokument scrollt nie`, [360, 390, 430, 744, 768].map((w) => `${v[w].ueberlauf.waagerecht}/${v[w].scroller.dokumentScrollt}`), ["0/false", "0/false", "0/false", "0/false", "0/false"]);
    p(`${M} ${r} Kopfzeile unter 768`, [360, 390, 430, 744].map((w) => `${v[w].kopfzeile.anzahlZeilen}/${v[w].kopfzeile.hoehe}/${v[w].kopfzeile.mandantSichtbar}/${v[w].kopfzeile.suchfeld.eigeneZeile}/${v[w].kopfzeile.navSpalteSichtbar}`), Array(4).fill("3/131/true/true/false"));
    p(`${M} ${r} ein Scroller`, [360, 390, 430, 744].map((w) => v[w].scroller.senkrecht.filter((x) => x.scrollt && !x.istMain).length), [0, 0, 0, 0]);
    p(`${M} ${r} Thema/Dichte`, [v[360].thema, v[360].dichte, v[360].pointerCoarse, v[360].dpr], ["dunkel", "m", false, 1]);
  }
}

const z = JSON.parse(readFileSync(path.join(ERG, "rahmen", "VOTG", "katalog-zeilenhoehen.json"), "utf8"));
p("Katalog Zeilenhöhen", [z["360"].min, z["360"].median, z["360"].max, z["768"].min, z["768"].median, z["768"].max, z["768"].partner.h, z["768"].partner.w, z["360"].partner.w, z["360"].ueber48, z["768"].ueber48, z["768"].lineHeight], [71, 111, 151, 79, 189, 739, 189, 0, 96, 390, 390, 22]);

// Eichung des neuen Kerns gegen den alten
const felder = (m) => [m.ueberlauf.waagerecht, m.scroller.senkrecht.map((s) => s.pfad + (s.scrollt ? "*" : "")).join(","), m.kopfzeile?.hoehe, m.kopfzeile?.anzahlZeilen, m.tabellen.map((t) => [t.breite, t.huelle.clientWidth, t.kopf.map((k) => `${k.text}:${k.w}`).join("|"), t.schnitte.length, t.nullbreit.length, t.abgeschnitten.zellen].join(";")).join("||"), `${m.ziele.unter44}/${m.ziele.gesamt}`, m.gekuerztAusserhalb.anzahl];
const dAlt = L("VOTG", "dashboard");
const dNeu = L("VOTG", "dashboard-neu");
p("Eichung Dashboard 360", felder(dNeu[360]), felder(dAlt[360]));
p("Eichung Dashboard 768 (nur die Zeilenzahl weicht)", felder(dNeu[768]).map((x, i) => (i === 3 ? "1↔2" : x)), felder(dAlt[768]).map((x, i) => (i === 3 ? "1↔2" : x)));
p("Eichung Dashboard 768 Zeilen", [dAlt[768].kopfzeile.anzahlZeilen, dNeu[768].kopfzeile.anzahlZeilen], [2, 1]);
p("Eichung Liste 360", felder(L("VOTG", "nachrichtenliste-neu")[360]), felder(L("VOTG", "nachrichtenliste")[360]));

// Anmeldung per CDP
const offen = readdirSync(ERG).find((x) => x.endsWith("OFFEN-messung.json"));
const o = JSON.parse(readFileSync(path.join(ERG, offen), "utf8")).laeufe[0].routen[0].breiten;
p("Anmeldung", [o.map((b) => b.dokument.scrollWidth), o.map((b) => `${b.ziele.unter44}/${b.ziele.gesamt}`), o[0].ziele.gruppen.map((g) => `${g.w}x${g.h}`), o.map((b) => b.zieleCoarse.unter44), o[0].zieleCoarse.token.bedienelement], [[360, 390, 430, 744, 768], Array(5).fill("4/5"), ["40x32", "304x40"], [4, 4, 4, 4, 4], "max(2.75rem, 44px)"]);

console.log(`geprüft: ${n} Aussagen, Abweichungen: ${f}`);
process.exit(f === 0 ? 0 : 1);
