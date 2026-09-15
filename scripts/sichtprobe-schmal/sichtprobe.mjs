#!/usr/bin/env node
/**
 * Sichtprobe am schmalen Fenster — die Erhebung.
 *
 * Fährt das installierte Chrome kopflos über das DevTools-Protokoll gegen die
 * lokal laufende Anwendung (`next build` + `next start`, Backend im Profil
 * `dev`), meldet sich **echt über das Anmeldeformular** an und misst je Route
 * und Fensterbreite im DOM — nicht am Bildschirmfoto. Die Bauform ist die aus
 * `docs/frontend-grundlagen.md` §8a: kein Playwright, keine Abhängigkeit, kein
 * heruntergeladener Browser. Es braucht nur Node (≥ 22, globales `WebSocket`
 * und `fetch`).
 *
 *   node scripts/sichtprobe-schmal/sichtprobe.mjs [--rolle ADMIN|MANDANT]
 *        [--mandanten NEXANS,VOTG] [--breiten 360x740,...] [--basis http://localhost:3000]
 *        [--nur dashboard,nachrichtenliste] [--ohne-bilder] [--ohne-anmeldung]
 *        [--chrome <pfad>] [--port 9333]
 *
 * Voraussetzung: Backend im Profil `dev` auf :8080 und das Frontend als
 * Produktionsbau auf :3000 (`cd frontend && pnpm exec next build && pnpm start`).
 * `--ohne-anmeldung` misst nur die öffentliche Anmeldeseite und braucht keine
 * Zugangsdaten.
 *
 * ## Zugangsdaten
 *
 * Ausschließlich aus Umgebungsvariablen, nie aus Argumenten, nie im Protokoll:
 *
 *   SICHTPROBE_ADMIN_USER / SICHTPROBE_ADMIN_PASSWORD      (Rolle ADMIN;
 *       Rückfall: OVERLORD_BOOTSTRAP_ADMIN_USER / _PASSWORD)
 *   SICHTPROBE_MANDANT_USER / SICHTPROBE_MANDANT_PASSWORD  (Rolle MANDANT)
 *
 * Ein Konto mit Änderungszwang wird **nicht** angefasst: Steht nach der
 * Anmeldung `mustChangePassword`, bricht der Lauf ab. Das Skript ändert nie ein
 * Passwort und legt kein Konto an.
 *
 * ## Was je Route und Breite erhoben wird (Auftrag §4)
 *
 *   1. waagerechter Überlauf am Dokument (`scrollWidth` gegen `innerWidth`)
 *   2. übereinanderliegende Kopfzellen (`th`/`td`), paarweise, als Zellen-
 *      **und** als Textkasten-Schnitt (`Range.getBoundingClientRect`) — eine
 *      0 px breite Zelle überlappt nicht, ihr Text schon
 *   3. Spalten mit Breite 0, die trotzdem Text tragen
 *   4. abgeschnittener Inhalt je Zelle (`scrollWidth > clientWidth`) samt
 *      `title`
 *   5. Zahl der senkrechten Scrollbereiche (angelegt und tatsächlich scrollend)
 *   6. Berührungsflächen unter 44 px — Layoutmaß `offsetWidth`/`offsetHeight`,
 *      einmal wie die Geräteemulation es liefert (`pointer: fine`, denn
 *      `mobile: true` setzt kein `pointer: coarse`) und einmal mit
 *      `Emulation.setTouchEmulationEnabled` (`pointer: coarse`) als obere
 *      Schranke; `matchMedia` steht in beiden Fällen daneben
 *   7. die Kopfzeile: Zeilenzahl, Mandantencode sichtbar, Suchfeld als eigene
 *      Zeile
 *
 * Bildschirmfotos entstehen nur dort, wo eine Zahl einen Befund zeigt —
 * höchstens eines je Route, mit `deviceScaleFactor: 1`. Sie liegen unter
 * `ergebnis/` und werden nicht eingecheckt (sie zeigen Daten der Testkopie).
 *
 * ## Was das Skript ausdrücklich nicht tut
 *
 *   - nichts gegen die Produktion — `--basis` ist auf `localhost` beschränkt
 *   - keine Rohdatenansicht, kein Artefaktabruf
 *   - kein Schreibvorgang außer Anmeldung, Mandantenwahl und dem Öffnen einer
 *     Zeile (alles Sitzungszustand)
 */

import { spawn } from "node:child_process";
import { existsSync, readFileSync } from "node:fs";
import { mkdir, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import path from "node:path";
import { fileURLToPath } from "node:url";

// ─────────────────────────────────────────────────────────────────────────────
// Argumente und Konstanten
// ─────────────────────────────────────────────────────────────────────────────

const HIER = path.dirname(fileURLToPath(import.meta.url));

const ARGS = argumente(process.argv.slice(2));
const BASIS = (ARGS.basis ?? "http://localhost:3000").replace(/\/$/, "");
const ROLLE = (ARGS.rolle ?? "ADMIN").toUpperCase();
const MANDANTEN = (ARGS.mandanten ?? "NEXANS,VOTG").split(",").map((m) => m.trim()).filter(Boolean);
const BREITEN = (ARGS.breiten ?? "360x740,390x844,430x932,744x1133,768x1024")
  .split(",")
  .map((b) => b.trim().split("x").map(Number))
  .map(([w, h]) => ({ w, h }));
const NUR = ARGS.nur ? ARGS.nur.split(",").map((s) => s.trim()) : null;
const OHNE_BILDER = Boolean(ARGS["ohne-bilder"]);
/** Nur die öffentliche Anmeldeseite messen und dann aufhören — braucht keine Zugangsdaten. */
const OHNE_ANMELDUNG = Boolean(ARGS["ohne-anmeldung"]);
const CHROME =
  ARGS.chrome ??
  process.env.SICHTPROBE_CHROME ??
  "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe";
const PORT = Number(ARGS.port ?? 9333);
const ERGEBNIS = path.resolve(ARGS.ergebnis ?? path.join(HIER, "ergebnis"));
const STEMPEL = new Date().toISOString().replace(/[:.]/g, "-").slice(0, 19);

/** Wartezeit nach einer Breitenänderung: Recharts' `ResponsiveContainer` ist erst nach ~700 ms stabil. */
const EINSCHWINGZEIT_MS = 1500;
/** Höchstens so lange auf Ruhe (keine offene Anfrage, kein Skelett) warten — die Testkopie schreibt Sitzungen langsam. */
const RUHE_TIMEOUT_MS = 90_000;

if (!/^https?:\/\/(localhost|127\.0\.0\.1)(:\d+)?$/.test(BASIS)) {
  fehler(`--basis muss auf localhost zeigen, nicht auf ${BASIS}. Nichts gegen die Produktion.`);
}
if (!["ADMIN", "MANDANT"].includes(ROLLE)) fehler(`--rolle muss ADMIN oder MANDANT sein.`);
if (!existsSync(CHROME)) fehler(`Chrome nicht gefunden: ${CHROME} (--chrome oder SICHTPROBE_CHROME)`);

const ZUGANG = OHNE_ANMELDUNG ? null : zugangsdaten(ROLLE);

// ─────────────────────────────────────────────────────────────────────────────
// Die Routen des Auftrags — in dieser Reihenfolge und nicht mehr.
// „Zustand" ist eine Lage derselben Route (Panel offen, Prozess gewählt), keine
// weitere Route.
// ─────────────────────────────────────────────────────────────────────────────

const ROUTEN = [
  { kennung: "anmeldung", titel: "Anmeldung", oeffentlich: true, pfad: () => "/anmeldung" },
  { kennung: "dashboard", titel: "Dashboard", pfad: () => "/" },
  { kennung: "nachrichtenliste", titel: "Nachrichtenliste", pfad: (d) => d.listePfad },
  {
    kennung: "nachrichtenliste-panel",
    titel: "Nachrichtenliste, Panel offen (Zustand)",
    pfad: (d) => (d.messageId ? `${d.listePfad}${d.listePfad.includes("?") ? "&" : "?"}nachricht=${enc(d.messageId)}` : null),
  },
  { kennung: "nachrichtendetail", titel: "Nachrichtendetail (eigene Route)", pfad: (d) => (d.messageId ? `/nachrichten/${enc(d.messageId)}` : null) },
  { kennung: "prozessansicht", titel: "Prozessansicht (Baum)", pfad: () => "/prozesse" },
  {
    kennung: "prozessansicht-prozess",
    titel: "Prozessansicht, Prozess gewählt (Zustand)",
    pfad: (d) => (d.processId ? `/prozesse?prozess=${enc(d.processId)}` : null),
  },
  { kennung: "belegsuche", titel: "Belegsuche mit Treffern", pfad: (d) => (d.suchbegriff ? `/suche?begriff=${enc(d.suchbegriff)}` : null) },
  { kennung: "benutzerverwaltung", titel: "Benutzerverwaltung", nurAdmin: true, pfad: () => "/administration/benutzer" },
  { kennung: "prozess-katalog", titel: "Prozess-Katalog", nurAdmin: true, pfad: () => "/administration/katalog" },
];

// ─────────────────────────────────────────────────────────────────────────────
// CDP-Klient — nur globales WebSocket, keine Abhängigkeit
// ─────────────────────────────────────────────────────────────────────────────

class Cdp {
  constructor(ws) {
    this.ws = ws;
    this.naechsteId = 0;
    this.wartend = new Map();
    this.horcher = new Map();
    ws.addEventListener("message", (e) => this.empfang(JSON.parse(e.data)));
    ws.addEventListener("close", () => {
      for (const w of this.wartend.values()) w.nein(new Error("Verbindung zu Chrome geschlossen"));
      this.wartend.clear();
    });
  }

  static async verbinden(url) {
    const ws = new WebSocket(url);
    await new Promise((ok, nein) => {
      ws.addEventListener("open", ok, { once: true });
      ws.addEventListener("error", () => nein(new Error(`WebSocket zu ${url} fehlgeschlagen`)), { once: true });
    });
    return new Cdp(ws);
  }

  empfang(n) {
    if (n.id !== undefined) {
      const w = this.wartend.get(n.id);
      if (!w) return;
      this.wartend.delete(n.id);
      if (n.error) w.nein(new Error(`${w.methode}: ${n.error.message}`));
      else w.ok(n.result);
      return;
    }
    if (n.method) for (const h of this.horcher.get(n.method) ?? []) h(n.params, n.sessionId);
  }

  senden(methode, params = {}, sessionId) {
    const id = ++this.naechsteId;
    const nachricht = { id, method: methode, params };
    if (sessionId) nachricht.sessionId = sessionId;
    this.ws.send(JSON.stringify(nachricht));
    return new Promise((ok, nein) => this.wartend.set(id, { ok, nein, methode }));
  }

  auf(methode, h) {
    const liste = this.horcher.get(methode) ?? [];
    liste.push(h);
    this.horcher.set(methode, liste);
    return () => {
      const i = liste.indexOf(h);
      if (i >= 0) liste.splice(i, 1);
    };
  }
}

/** Eine Seite (ein Ziel) mit ihrer Sitzungskennung. */
class Seite {
  constructor(cdp, sessionId) {
    this.cdp = cdp;
    this.sessionId = sessionId;
    this.konsole = [];
    this.geladen = null;
  }

  static async oeffnen(cdp) {
    const { targetId } = await cdp.senden("Target.createTarget", { url: "about:blank" });
    const { sessionId } = await cdp.senden("Target.attachToTarget", { targetId, flatten: true });
    const seite = new Seite(cdp, sessionId);
    await seite.senden("Page.enable");
    await seite.senden("Runtime.enable");
    await seite.senden("Log.enable");
    await seite.senden("Network.enable");
    // Zähler für offene fetch-Aufrufe: die Ruhebedingung nach jeder Navigation.
    await seite.senden("Page.addScriptToEvaluateOnNewDocument", {
      source: `(() => {
        const orig = window.fetch;
        window.__sichtprobeOffen = 0; window.__sichtprobeAnfragen = 0;
        window.fetch = function (...a) {
          window.__sichtprobeOffen++; window.__sichtprobeAnfragen++;
          return orig.apply(this, a).finally(() => { window.__sichtprobeOffen--; });
        };
      })();`,
    });
    cdp.auf("Runtime.consoleAPICalled", (p, sid) => {
      if (sid !== sessionId) return;
      if (p.type !== "error" && p.type !== "warning") return;
      seite.konsole.push({ art: p.type, text: kurz(p.args.map((a) => a.value ?? a.description ?? "").join(" "), 160) });
    });
    cdp.auf("Runtime.exceptionThrown", (p, sid) => {
      if (sid !== sessionId) return;
      seite.konsole.push({ art: "ausnahme", text: kurz(p.exceptionDetails?.exception?.description ?? p.exceptionDetails?.text ?? "", 160) });
    });
    cdp.auf("Log.entryAdded", (p, sid) => {
      if (sid !== sessionId) return;
      if (p.entry.level !== "error") return;
      seite.konsole.push({ art: "log", text: kurz(`${p.entry.source}: ${p.entry.text}`, 160) });
    });
    cdp.auf("Network.responseReceived", (p, sid) => {
      if (sid !== sessionId || p.response.status < 400) return;
      seite.konsole.push({ art: "antwort", text: `${p.response.status} ${p.type} ${kurz(p.response.url.replace(BASIS, ""), 120)}` });
    });
    cdp.auf("Page.loadEventFired", (_p, sid) => {
      if (sid === sessionId && seite.geladen) seite.geladen();
    });
    return seite;
  }

  senden(methode, params) {
    return this.cdp.senden(methode, params, this.sessionId);
  }

  /** Wertet einen Ausdruck in der Seite aus; Promises werden abgewartet, das Ergebnis kommt als Wert. */
  async auswerten(ausdruck) {
    const r = await this.senden("Runtime.evaluate", { expression: ausdruck, awaitPromise: true, returnByValue: true });
    if (r.exceptionDetails) {
      throw new Error(`Auswertung fehlgeschlagen: ${r.exceptionDetails.exception?.description ?? r.exceptionDetails.text}`);
    }
    return r.result.value;
  }

  async navigieren(url) {
    const geladen = new Promise((ok) => (this.geladen = ok));
    await this.senden("Page.navigate", { url });
    await Promise.race([geladen, schlafen(60_000).then(() => Promise.reject(new Error(`Laden von ${url} dauert länger als 60 s`)))]);
    this.geladen = null;
  }

  /** Ruhe: keine offene fetch-Anfrage, kein Skelett, nichts `aria-busy` — dreimal hintereinander. */
  async ruhe(timeoutMs = RUHE_TIMEOUT_MS) {
    const start = Date.now();
    let treffer = 0;
    let letzter = null;
    while (Date.now() - start < timeoutMs) {
      letzter = await this.auswerten(
        `({ offen: window.__sichtprobeOffen ?? 0, anfragen: window.__sichtprobeAnfragen ?? 0,
            skelette: document.querySelectorAll('[data-slot="skeleton"]').length,
            busy: document.querySelectorAll('[aria-busy="true"]').length, ready: document.readyState })`,
      );
      if (letzter.offen === 0 && letzter.skelette === 0 && letzter.busy === 0 && letzter.ready === "complete") {
        if (++treffer >= 3) return { erreicht: true, dauerMs: Date.now() - start, anfragen: letzter.anfragen };
      } else treffer = 0;
      await schlafen(250);
    }
    return { erreicht: false, dauerMs: Date.now() - start, anfragen: letzter?.anfragen ?? null, letzter };
  }

  async warteBis(ausdruck, timeoutMs = 60_000, schritt = 250) {
    const start = Date.now();
    while (Date.now() - start < timeoutMs) {
      if (await this.auswerten(ausdruck)) return true;
      await schlafen(schritt);
    }
    return false;
  }

  async masse({ w, h }, dsf = 2) {
    await this.senden("Emulation.setDeviceMetricsOverride", {
      width: w,
      height: h,
      deviceScaleFactor: dsf,
      mobile: true,
      screenWidth: w,
      screenHeight: h,
    });
  }

  /**
   * `pointer: coarse` ein- oder ausschalten. `Emulation.setEmulatedMedia` kennt
   * das Merkmal `pointer` nicht (geprüft am 15.09.2026: `matchMedia` blieb
   * `false`); was es setzt, ist die Berührungsemulation — dieselbe, mit der
   * `docs/dichte-umschalter.md` §5.4 und M122 gemessen haben.
   */
  async zeiger(grob) {
    await this.senden("Emulation.setTouchEmulationEnabled", grob ? { enabled: true, maxTouchPoints: 1 } : { enabled: false });
  }

  async bild(datei, masse) {
    await this.masse(masse, 1);
    await schlafen(EINSCHWINGZEIT_MS);
    const { data } = await this.senden("Page.captureScreenshot", { format: "png" });
    await writeFile(datei, Buffer.from(data, "base64"));
    await this.masse(masse, 2);
    await schlafen(EINSCHWINGZEIT_MS);
  }

  konsoleAbholen() {
    const k = this.konsole;
    this.konsole = [];
    return k;
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Die Messfunktion — läuft IN der Seite. Ein einziger Ausdruck, ein Ergebnis.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Der Messkern liegt in `messung.js` — eine Funktion `(document, window)`, die
 * auch der Messrahmen `rahmen.js` im Chrome der Erweiterung verwendet. Hier
 * wird sie als Ausdruck in die Seite geschickt.
 */
const MESSUNG = `(${readFileSync(path.join(HIER, "messung.js"), "utf8")})(document, window)`;

const ZIELE_KURZ = `(() => {
  const sichtbar = (el) => { const cs = getComputedStyle(el); if (cs.display === "none" || cs.visibility === "hidden") return false; const r = el.getClientRects(); return r.length > 0 && (r[0].width > 0 || r[0].height > 0); };
  const ZIELE = 'a[href], button, input:not([type="hidden"]), select, textarea, summary, [role="button"], [role="link"], [role="menuitem"], [role="menuitemradio"], [role="menuitemcheckbox"], [role="switch"], [role="checkbox"], [role="radio"], [role="tab"], [role="option"], [role="treeitem"], [role="combobox"], [tabindex="0"]';
  const ziele = Array.from(document.querySelectorAll(ZIELE)).filter(sichtbar).filter((el) => !el.closest('[aria-hidden="true"]') && !el.disabled);
  let klein = 0, nurBreite = 0, nurHoehe = 0, beides = 0; const arten = new Map();
  for (const el of ziele) {
    const w = el.offsetWidth, h = el.offsetHeight; if (w >= 44 && h >= 44) continue;
    klein++; if (w < 44 && h < 44) beides++; else if (w < 44) nurBreite++; else nurHoehe++;
    const k = el.tagName.toLowerCase() + (el.closest("table") ? "@table" : el.closest("header") ? "@header" : "") + (h < 44 ? " h" + h : " b" + w);
    arten.set(k, (arten.get(k) ?? 0) + 1);
  }
  const wurzel = getComputedStyle(document.documentElement);
  const knopf = document.querySelector("button");
  return { pointerCoarse: matchMedia("(pointer: coarse)").matches, hoverHover: matchMedia("(hover: hover)").matches, gesamt: ziele.length, unter44: klein, nurBreite, nurHoehe, beides, arten: Object.fromEntries(arten),
           kopfzeileHoehe: document.querySelector("header")?.getBoundingClientRect().height ?? null,
           token: { bedienelement: wurzel.getPropertyValue("--dichte-bedienelement").trim(), beruehrung: wurzel.getPropertyValue("--dichte-beruehrung").trim(), feld: wurzel.getPropertyValue("--dichte-feld").trim() },
           ersterKnopf: knopf ? { minHeight: getComputedStyle(knopf).minHeight, h: knopf.offsetHeight, klassen: knopf.className.slice(0, 80) } : null };
})()`;

// ─────────────────────────────────────────────────────────────────────────────
// Ablauf
// ─────────────────────────────────────────────────────────────────────────────

async function main() {
  await mkdir(ERGEBNIS, { recursive: true });
  const profil = path.join(tmpdir(), `sichtprobe-schmal-${process.pid}`);
  const chrome = spawn(
    CHROME,
    [
      "--headless=new",
      `--remote-debugging-port=${PORT}`,
      `--user-data-dir=${profil}`,
      "--no-first-run",
      "--no-default-browser-check",
      "--disable-extensions",
      "--disable-background-networking",
      "--disable-gpu",
      "--lang=de-DE",
      "--window-size=1280,900",
      "about:blank",
    ],
    { stdio: "ignore" },
  );
  const protokoll = { stempel: STEMPEL, basis: BASIS, rolle: ROLLE, mandanten: MANDANTEN, breiten: BREITEN, chrome: null, node: process.version, laeufe: [] };

  try {
    const version = await warteAufChrome();
    protokoll.chrome = version.Browser;
    protokoll.protokollVersion = version["Protocol-Version"];
    meldung(`Chrome ${version.Browser}, Node ${process.version}, Ziel ${BASIS}, Rolle ${ROLLE}`);

    const cdp = await Cdp.verbinden(version.webSocketDebuggerUrl);
    const seite = await Seite.oeffnen(cdp);

    // Die öffentliche Seite zuerst — ohne Anmeldung, wie ein Nutzer sie sieht.
    if (routeGewollt("anmeldung")) {
      const lauf = { rolle: "keine", mandant: null, routen: [] };
      protokoll.laeufe.push(lauf);
      lauf.routen.push(await routeMessen(seite, ROUTEN[0], {}, lauf));
      await schreiben(protokoll);
    }
    if (OHNE_ANMELDUNG) return;

    const auskunft = await anmelden(seite);
    meldung(`angemeldet als Rolle ${auskunft.role}${auskunft.mandant ? `, Mandant ${auskunft.mandant.id}` : ""}`);
    protokoll.angemeldeteRolle = auskunft.role;

    const zielMandanten = auskunft.role === "ADMIN" ? MANDANTEN : [auskunft.mandant?.id ?? MANDANTEN[0]];
    for (const mandant of zielMandanten) {
      const aktuell = await mandantWaehlen(seite, mandant);
      if (!aktuell) {
        warnung(`Mandant ${mandant} konnte nicht gewählt werden — übersprungen.`);
        continue;
      }
      meldung(`Mandant ${aktuell.id} (${aktuell.name ?? ""}) aktiv`);
      const lauf = { rolle: auskunft.role, mandant: aktuell.id, mandantName: aktuell.name ?? null, daten: null, routen: [] };
      protokoll.laeufe.push(lauf);
      lauf.daten = await datenErmitteln(seite);
      meldung(`Prüfdaten: Liste ${lauf.daten.listePfad}, Nachricht ${lauf.daten.messageId ? "gefunden" : "keine"}, Prozess ${lauf.daten.processId ? "gefunden" : "keiner"}, Suchbegriff ${lauf.daten.suchbegriff ? "gefunden" : "keiner"}`);
      for (const route of ROUTEN.slice(1)) {
        if (route.nurAdmin && auskunft.role !== "ADMIN") continue;
        if (!routeGewollt(route.kennung)) continue;
        lauf.routen.push(await routeMessen(seite, route, lauf.daten, lauf));
        // Nach jeder Route schreiben: Ein Abbruch verliert dann nichts Gemessenes.
        await schreiben(protokoll, true);
      }
    }

    await schreiben(protokoll);
  } finally {
    chrome.kill();
    await schlafen(500);
    await rm(profil, { recursive: true, force: true }).catch(() => {});
  }
}

async function warteAufChrome() {
  for (let i = 0; i < 100; i++) {
    try {
      const r = await fetch(`http://127.0.0.1:${PORT}/json/version`);
      if (r.ok) return await r.json();
    } catch {
      /* noch nicht da */
    }
    await schlafen(200);
  }
  throw new Error(`Chrome meldet sich nicht auf Port ${PORT}`);
}

/** Echte Anmeldung über das Formular: tippen, Eingabetaste, warten. */
async function anmelden(seite) {
  await seite.masse(BREITEN[0]);
  await seite.navigieren(`${BASIS}/anmeldung`);
  if (!(await seite.warteBis(`document.querySelector('#benutzername') !== null && document.querySelector('#passwort') !== null`, 30_000))) {
    throw new Error("Anmeldeformular nicht gefunden");
  }
  await seite.auswerten(`document.querySelector('#benutzername').focus()`);
  await seite.senden("Input.insertText", { text: ZUGANG.benutzer });
  await seite.auswerten(`document.querySelector('#passwort').focus()`);
  await seite.senden("Input.insertText", { text: ZUGANG.passwort });
  const stimmt = await seite.auswerten(
    `document.querySelector('#benutzername').value === ${JSON.stringify(ZUGANG.benutzer)} && document.querySelector('#passwort').value.length === ${ZUGANG.passwort.length}`,
  );
  if (!stimmt) throw new Error("Die Eingabe ist nicht im Formular angekommen");
  await seite.senden("Input.dispatchKeyEvent", { type: "keyDown", key: "Enter", code: "Enter", windowsVirtualKeyCode: 13, text: "\r" });
  await seite.senden("Input.dispatchKeyEvent", { type: "keyUp", key: "Enter", code: "Enter", windowsVirtualKeyCode: 13 });

  const weg = await seite.warteBis(`location.pathname !== '/anmeldung' || document.querySelector('form [role="alert"], form [data-slot="alert"]') !== null`, 90_000);
  if (!weg) throw new Error("Nach der Anmeldung tut sich nichts (90 s)");
  const auskunft = await seite.auswerten(`fetch('/api/auth/me').then(r => r.ok ? r.json() : { status: r.status })`);
  if (auskunft.status) {
    // Nur, was die Seite dem Nutzer zeigt — nie die Eingabe.
    const gezeigt = await seite.auswerten(`(document.querySelector('form [role="alert"], form [data-slot="alert"]')?.textContent ?? '').trim().slice(0, 160)`);
    throw new Error(`Anmeldung fehlgeschlagen: /api/auth/me antwortet ${auskunft.status}. Das Formular zeigt: „${gezeigt || "(nichts)"}". Zugangsdaten prüfen — nach fünf Fehlversuchen sperrt das Backend das Konto für 15 Minuten.`);
  }
  if (auskunft.mustChangePassword) {
    throw new Error("Das Konto steht unter Änderungszwang. Das Skript ändert kein Passwort — bitte von Hand anmelden und das Passwort setzen.");
  }
  if (auskunft.role !== ROLLE) warnung(`Angemeldete Rolle ist ${auskunft.role}, verlangt war ${ROLLE}.`);
  await seite.ruhe();
  return auskunft;
}

/** Mandant über die Auswahlseite wählen — der Weg, den der Nutzer geht. */
async function mandantWaehlen(seite, mandant) {
  let auskunft = await seite.auswerten(`fetch('/api/auth/me').then(r => r.json())`);
  if (auskunft.mandant?.id === mandant) return auskunft.mandant;
  await seite.navigieren(`${BASIS}/mandantenauswahl`);
  const da = await seite.warteBis(`Array.from(document.querySelectorAll('button .font-mono')).some(s => s.textContent.trim() === ${JSON.stringify(mandant)})`, 60_000);
  if (!da) {
    const vorhanden = await seite.auswerten(`Array.from(document.querySelectorAll('button .font-mono')).map(s => s.textContent.trim())`);
    warnung(`Mandant ${mandant} steht nicht zur Wahl. Angeboten: ${vorhanden.join(", ") || "(nichts)"}`);
    return null;
  }
  await seite.auswerten(`Array.from(document.querySelectorAll('button .font-mono')).find(s => s.textContent.trim() === ${JSON.stringify(mandant)}).closest('button').click()`);
  const gewechselt = await seite.warteBis(`fetch('/api/auth/me').then(r => r.json()).then(a => a.mandant && a.mandant.id === ${JSON.stringify(mandant)})`, 90_000, 500);
  if (!gewechselt) return null;
  auskunft = await seite.auswerten(`fetch('/api/auth/me').then(r => r.json())`);
  await seite.ruhe();
  return auskunft.mandant;
}

/**
 * Prüfdaten je Mandant, über die Oberfläche und die Endpunkte der Sitzung:
 * die Liste (24 h, sonst 30 Tage), eine Nachricht daraus (Zeilenklick), ihr
 * Prozess und ein Belegwert für die Suche.
 */
async function datenErmitteln(seite) {
  const daten = { listePfad: "/nachrichten", messageId: null, processId: null, suchbegriff: null, hinweise: [] };
  await seite.masse({ w: 1280, h: 900 });
  await seite.navigieren(`${BASIS}/nachrichten`);
  await seite.ruhe();
  let zeilen = await seite.auswerten(`document.querySelectorAll('table tbody tr').length`);
  if (zeilen === 0) {
    daten.hinweise.push("24-h-Fenster leer; 30-Tage-Fenster verwendet");
    daten.listePfad = "/nachrichten?zeitraum=30d";
    await seite.navigieren(`${BASIS}${daten.listePfad}`);
    await seite.ruhe();
    zeilen = await seite.auswerten(`document.querySelectorAll('table tbody tr').length`);
  }
  daten.zeilenInListe = zeilen;
  if (zeilen === 0) {
    daten.hinweise.push("auch das 30-Tage-Fenster ist leer — keine Nachricht, kein Prozess, keine Suche");
    return daten;
  }
  // Zeilen der Reihe nach öffnen, bis eine Belegwerte trägt (80 % tragen keine).
  for (let i = 0; i < Math.min(zeilen, 8); i++) {
    await seite.auswerten(`document.querySelectorAll('table tbody tr')[${i}].querySelector('td').click()`);
    const offen = await seite.warteBis(`new URLSearchParams(location.search).has('nachricht')`, 30_000);
    if (!offen) break;
    const id = await seite.auswerten(`new URLSearchParams(location.search).get('nachricht')`);
    await seite.ruhe();
    if (!daten.messageId) {
      daten.messageId = id;
      const detail = await seite.auswerten(`fetch('/api/nachrichten/' + encodeURIComponent(${JSON.stringify(id)})).then(r => r.ok ? r.json() : null)`);
      daten.processId = detail?.processId ?? null;
    }
    const bam = await seite.auswerten(`fetch('/api/nachrichten/' + encodeURIComponent(${JSON.stringify(id)}) + '/bam').then(r => r.ok ? r.json() : null)`);
    const gruppe = bam?.gruppen?.find((g) => g.werte?.length > 0);
    if (gruppe) {
      daten.suchbegriff = `${gruppe.typ}:${gruppe.werte[0]}`;
      daten.suchbegriffTyp = gruppe.typ;
      daten.suchNachricht = id;
      break;
    }
    daten.hinweise.push(`Zeile ${i + 1}: keine Belegwerte`);
  }
  if (!daten.suchbegriff) daten.hinweise.push("keine der geöffneten Zeilen trägt Belegwerte — Belegsuche ohne Treffer nicht messbar");
  return daten;
}

/** Eine Route über alle Breiten: einmal navigieren, dann nur die Maße wechseln. */
async function routeMessen(seite, route, daten, lauf) {
  const pfad = route.pfad(daten);
  const ergebnis = { kennung: route.kennung, titel: route.titel, pfad, breiten: [], konsole: [], bild: null };
  if (!pfad) {
    ergebnis.uebersprungen = "keine Prüfdaten für diese Route";
    warnung(`${route.titel}: ${ergebnis.uebersprungen}`);
    return ergebnis;
  }
  meldung(`${route.titel} — ${pfad}`);
  await seite.masse(BREITEN[0]);
  seite.konsoleAbholen();
  await seite.navigieren(`${BASIS}${pfad}`);
  ergebnis.ruhe = await seite.ruhe();
  ergebnis.urlNachLaden = await seite.auswerten(`location.pathname + location.search`);
  if (!ergebnis.ruhe.erreicht) warnung(`  Ruhe nicht erreicht nach ${ergebnis.ruhe.dauerMs} ms: ${JSON.stringify(ergebnis.ruhe.letzter)}`);

  for (const masse of BREITEN) {
    await seite.masse(masse);
    await schlafen(EINSCHWINGZEIT_MS);
    await seite.auswerten(`document.fonts.ready.then(() => true)`);
    await seite.ruhe(15_000);
    const messung = await seite.auswerten(MESSUNG);
    // Berührungsflächen als obere Schranke: pointer: coarse per Berührungsemulation.
    await seite.zeiger(true);
    await schlafen(400);
    messung.zieleCoarse = await seite.auswerten(ZIELE_KURZ);
    await seite.zeiger(false);
    await schlafen(400);
    messung.zeigerZurueck = await seite.auswerten(`matchMedia('(pointer: coarse)').matches`);
    messung.masse = masse;
    messung.befunde = befunde(messung, masse);
    ergebnis.breiten.push(messung);
    meldung(`  ${masse.w}×${masse.h}: Überlauf ${messung.ueberlauf.waagerecht} px · Kopfzeile ${messung.kopfzeile ? messung.kopfzeile.anzahlZeilen + " Zeilen" : "—"} · Scroller ${messung.scroller.senkrechtAktiv}/${messung.scroller.senkrechtAngelegt} · Tabellenschnitte ${messung.tabellen.reduce((s, t) => s + t.schnitte.length, 0)} · 0-px-Spalten ${messung.tabellen.reduce((s, t) => s + t.nullbreit.length, 0)} · Ziele < 44: ${messung.ziele.unter44}/${messung.ziele.gesamt} (coarse ${messung.zieleCoarse.unter44}) · Befunde ${messung.befunde.length}`);
  }
  ergebnis.konsole = seite.konsoleAbholen();

  if (!OHNE_BILDER) {
    const mitBefund = ergebnis.breiten.find((b) => b.befunde.some((f) => f.bild));
    if (mitBefund) {
      const datei = path.join(ERGEBNIS, `${STEMPEL}-${lauf.rolle}-${lauf.mandant ?? "offen"}-${route.kennung}-${mitBefund.masse.w}.png`);
      await seite.bild(datei, mitBefund.masse);
      ergebnis.bild = path.basename(datei);
      meldung(`  Bild: ${ergebnis.bild}`);
    }
  }
  return ergebnis;
}

/** Was in einer Messung als Befund zählt — Zahlen, keine Deutung. */
function befunde(m, masse) {
  const liste = [];
  if (m.ueberlauf.waagerecht !== 0) liste.push({ art: "ueberlauf", text: `Dokument ${m.ueberlauf.waagerecht > 0 ? "+" : ""}${m.ueberlauf.waagerecht} px breiter als das Fenster`, bild: true });
  for (const t of m.tabellen) {
    for (const s of t.schnitte) liste.push({ art: "schnitt", text: `„${s.a}" und „${s.b}" überlappen: Zellen ${s.zellenSchnitt} px, Text ${s.textSchnitt} px`, bild: true });
    for (const n of t.nullbreit) liste.push({ art: "nullbreit", text: `Spalte „${n.text}" ist ${n.breite} px breit und zeichnet ${n.textbreite} px Text`, bild: true });
    if (t.abgeschnitten.ohneTitle > 0) liste.push({ art: "ohne-title", text: `${t.abgeschnitten.ohneTitle} gekürzte Zellen ohne title (von ${t.abgeschnitten.zellen})`, bild: false });
  }
  const fremde = m.scroller.senkrecht.filter((s) => s.scrollt && !s.istMain);
  if (fremde.length > 0) liste.push({ art: "scroller", text: `${fremde.length} senkrechter Scrollbereich neben main: ${fremde.map((s) => s.pfad).join(", ")}`, bild: true });
  if (m.scroller.dokumentScrollt && m.kopfzeile) liste.push({ art: "dokument-scrollt", text: `Das Dokument selbst scrollt (${m.dokument.scrollHeight} > ${m.dokument.clientHeight})`, bild: true });
  if (m.kopfzeile && masse.w < 768) {
    if (m.kopfzeile.anzahlZeilen !== 3) liste.push({ art: "kopfzeile", text: `Kopfzeile hat ${m.kopfzeile.anzahlZeilen} statt 3 Zeilen`, bild: true });
    if (!m.kopfzeile.mandantSichtbar) liste.push({ art: "mandant", text: `Mandantencode ${m.kopfzeile.mandantCode ?? "(fehlt)"} nicht sichtbar`, bild: true });
    if (m.kopfzeile.suchfeld && !m.kopfzeile.suchfeld.eigeneZeile) liste.push({ art: "suchfeld", text: "Suchfeld teilt sich die Zeile", bild: true });
    if (m.kopfzeile.navSpalteSichtbar) liste.push({ art: "navspalte", text: "Navigationsspalte unter 768 px sichtbar", bild: true });
  }
  if (m.ziele.unter44 > 0) liste.push({ art: "ziele", text: `${m.ziele.unter44} von ${m.ziele.gesamt} Zielen unter 44 px (pointer: fine); mit pointer: coarse ${m.zieleCoarse.unter44}`, bild: false });
  return liste;
}

// ─────────────────────────────────────────────────────────────────────────────
// Ausgabe: das vollständige JSON und eine Zusammenfassung in Markdown
// ─────────────────────────────────────────────────────────────────────────────

async function schreiben(protokoll, still = false) {
  const kennung = OHNE_ANMELDUNG ? "OFFEN" : ROLLE;
  const json = path.join(ERGEBNIS, `${STEMPEL}-${kennung}-messung.json`);
  await writeFile(json, JSON.stringify(protokoll, null, 2));
  const md = path.join(ERGEBNIS, `${STEMPEL}-${kennung}-zusammenfassung.md`);
  await writeFile(md, zusammenfassung(protokoll));
  if (!still) {
    meldung(`geschrieben: ${json}`);
    meldung(`geschrieben: ${md}`);
  }
}

function zusammenfassung(p) {
  const z = [];
  z.push(`# Sichtprobe schmal — ${p.stempel}`, "");
  z.push(`| | |`, `|---|---|`, `| Chrome | ${p.chrome} (Protokoll ${p.protokollVersion}) |`, `| Node | ${p.node} |`, `| Ziel | ${p.basis} |`, `| Rolle | ${p.angemeldeteRolle ?? p.rolle} |`, `| Breiten | ${p.breiten.map((b) => `${b.w}×${b.h}`).join(", ")} |`, "");
  for (const lauf of p.laeufe) {
    z.push(`## ${lauf.rolle === "keine" ? "ohne Anmeldung" : `${lauf.rolle} · ${lauf.mandant}`}`, "");
    if (lauf.daten) z.push(`Prüfdaten: Liste \`${lauf.daten.listePfad}\` (${lauf.daten.zeilenInListe} Zeilen), Nachricht ${lauf.daten.messageId ? "ja" : "nein"}, Prozess ${lauf.daten.processId ? "ja" : "nein"}, Suchbegriff Typ ${lauf.daten.suchbegriffTyp ?? "—"}${lauf.daten.hinweise.length ? `; Hinweise: ${lauf.daten.hinweise.join("; ")}` : ""}`, "");
    for (const r of lauf.routen) {
      z.push(`### ${r.titel} — \`${r.pfad ?? "—"}\``, "");
      if (r.uebersprungen) {
        z.push(`übersprungen: ${r.uebersprungen}`, "");
        continue;
      }
      z.push(`| Breite | Dok. Überlauf | Kopfzeile | Mandant | Suchfeld eigene Zeile | Scroller aktiv/angelegt | Dok. scrollt | Tabellen | Kopfschnitte | 0-px-Spalten | gekürzt (ohne title) | Ziele < 44 fine / coarse | Konsole |`);
      z.push(`|---:|---:|---|---|---|---|---|---:|---:|---:|---:|---|---|`);
      for (const b of r.breiten) {
        const k = b.kopfzeile;
        z.push(
          `| ${b.masse.w}×${b.masse.h} | ${b.ueberlauf.waagerecht} px | ${k ? `${k.anzahlZeilen} Zeilen, ${k.hoehe} px` : "—"} | ${k ? (k.mandantSichtbar ? `${k.mandantCode} sichtbar` : `${k.mandantCode ?? "—"} NICHT sichtbar`) : "—"} | ${k?.suchfeld ? (k.suchfeld.eigeneZeile ? "ja" : "nein") : "—"} | ${b.scroller.senkrechtAktiv}/${b.scroller.senkrechtAngelegt} | ${b.scroller.dokumentScrollt ? "ja" : "nein"} | ${b.tabellen.length} | ${b.tabellen.reduce((s, t) => s + t.schnitte.length, 0)} | ${b.tabellen.reduce((s, t) => s + t.nullbreit.length, 0)} | ${b.tabellen.reduce((s, t) => s + t.abgeschnitten.zellen, 0)} (${b.tabellen.reduce((s, t) => s + t.abgeschnitten.ohneTitle, 0)}) | ${b.ziele.unter44}/${b.ziele.gesamt} · ${b.zieleCoarse.unter44}/${b.zieleCoarse.gesamt} | ${r.konsole.length} |`,
        );
      }
      z.push("");
      for (const b of r.breiten) {
        if (b.befunde.length === 0) continue;
        z.push(`**${b.masse.w} px:**`);
        for (const f of b.befunde) z.push(`- ${f.art}: ${f.text}`);
        for (const t of b.tabellen) {
          z.push(`- Tabelle ${t.pfad}: Hülle ${t.huelle.breite} px (scrollWidth ${t.huelle.scrollWidth}), Tabelle ${t.breite} px, ${t.zeilen} Zeilen; Kopf: ${t.kopf.map((c) => `${c.text} ${c.w} px${c.abgeschnitten ? " ✂" : ""}`).join(" · ")}`);
        }
        if (b.ziele.gruppen.length) z.push(`- kleine Ziele: ${b.ziele.gruppen.slice(0, 8).map((g) => `${g.name} ${g.w}×${g.h}${g.anzahl > 1 ? ` (×${g.anzahl})` : ""}`).join("; ")}`);
        if (b.scroller.senkrecht.length) z.push(`- Scrollbereiche: ${b.scroller.senkrecht.map((s) => `${s.pfad} ${s.scrollHeight}/${s.clientHeight}${s.scrollt ? " scrollt" : ""}`).join("; ")}`);
        if (b.ueberlauf.beispiele.length) z.push(`- überstehend: ${b.ueberlauf.beispiele.map((u) => `${u.pfad} bis ${u.rechts}${u.inHuelle ? ` (in ${u.inHuelle})` : ""}`).join("; ")}`);
        z.push("");
      }
      if (r.konsole.length) z.push(`Konsole: ${r.konsole.map((k) => `[${k.art}] ${k.text}`).join(" · ")}`, "");
      if (r.bild) z.push(`Bild: \`${r.bild}\``, "");
    }
  }
  return z.join("\n");
}

// ─────────────────────────────────────────────────────────────────────────────
// Helfer
// ─────────────────────────────────────────────────────────────────────────────

function argumente(argv) {
  const a = {};
  for (let i = 0; i < argv.length; i++) {
    const t = argv[i];
    if (!t.startsWith("--")) continue;
    const name = t.slice(2);
    const naechster = argv[i + 1];
    if (naechster === undefined || naechster.startsWith("--")) a[name] = true;
    else {
      a[name] = naechster;
      i++;
    }
  }
  return a;
}

function zugangsdaten(rolle) {
  const [u, p] =
    rolle === "ADMIN"
      ? [process.env.SICHTPROBE_ADMIN_USER || process.env.OVERLORD_BOOTSTRAP_ADMIN_USER, process.env.SICHTPROBE_ADMIN_PASSWORD || process.env.OVERLORD_BOOTSTRAP_ADMIN_PASSWORD]
      : [process.env.SICHTPROBE_MANDANT_USER, process.env.SICHTPROBE_MANDANT_PASSWORD];
  if (!u || !p) {
    fehler(
      rolle === "ADMIN"
        ? "Keine Zugangsdaten: SICHTPROBE_ADMIN_USER/SICHTPROBE_ADMIN_PASSWORD (oder OVERLORD_BOOTSTRAP_ADMIN_USER/_PASSWORD) setzen."
        : "Keine Zugangsdaten: SICHTPROBE_MANDANT_USER/SICHTPROBE_MANDANT_PASSWORD setzen.",
    );
  }
  return { benutzer: u, passwort: p };
}

function routeGewollt(kennung) {
  return NUR === null || NUR.includes(kennung);
}

const enc = (s) => encodeURIComponent(s);
const schlafen = (ms) => new Promise((ok) => setTimeout(ok, ms));
const kurz = (s, n) => (s.length > n ? s.slice(0, n - 1) + "…" : s);
const meldung = (t) => console.log(`[${new Date().toISOString().slice(11, 19)}] ${t}`);
const warnung = (t) => console.warn(`[${new Date().toISOString().slice(11, 19)}] ⚠ ${t}`);
function fehler(t) {
  console.error(`Fehler: ${t}`);
  process.exit(2);
}

main().catch((e) => {
  console.error(`Abbruch: ${e.message}`);
  process.exit(1);
});
