#!/usr/bin/env node
/**
 * Der Empfänger für den Messrahmen (`rahmen.js`): Das `javascript_tool` der
 * Chrome-Erweiterung schneidet seine Ausgabe nach rund tausend Zeichen ab —
 * ein Vollergebnis über fünf Breiten passt da nicht durch. Der Rahmen schickt
 * es deshalb per `fetch` hierher, und hier landet es als Datei unter
 * `ergebnis/rahmen/<kennung>.json`, genau so, wie `sichtprobe.mjs` seine
 * Ergebnisse schreibt.
 *
 *   node scripts/sichtprobe-schmal/empfaenger.mjs [--port 3999]
 *
 * Seit Teil 2 liefert er ausserdem die Messskripte selbst aus (`GET /skript/<datei>`,
 * nur die vier Dateien aus `SKRIPTE`), damit sie im `javascript_tool` nicht als
 * vierzig Kilobyte Text eingesetzt werden muessen, sondern per `fetch` kommen.
 *
 * Nur `127.0.0.1`, nur `POST` und dieses eine `GET`, nur die Herkunft der Anwendung (`localhost:3000`,
 * seit Teil 2 auch `localhost:3001` — dort läuft der Produktionsbau neben `next dev`).
 * Nichts davon verlässt den Rechner.
 */
import http from "node:http";
import { mkdir, readFile, writeFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const HIER = path.dirname(fileURLToPath(import.meta.url));
const PORT = Number(process.argv.includes("--port") ? process.argv[process.argv.indexOf("--port") + 1] : 3999);
const HERKUENFTE = new Set(["http://localhost:3000", "http://localhost:3001"]);
const ZIEL = path.join(HIER, "ergebnis", "rahmen");
const SKRIPTE = new Map([
  ["messung.js", "messung.js"],
  ["rahmen.js", "rahmen.js"],
  ["spaltenbedarf.js", "spaltenbedarf.js"],
  ["etiketten.json", path.join("ergebnis", "etiketten.json")],
]);

http
  .createServer(async (req, res) => {
    const herkunft = req.headers.origin;
    if (HERKUENFTE.has(herkunft)) res.setHeader("Access-Control-Allow-Origin", herkunft);
    res.setHeader("Access-Control-Allow-Headers", "content-type");
    res.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
    if (req.method === "OPTIONS") {
      res.writeHead(204);
      res.end();
      return;
    }
    if (req.method === "GET" && req.url.startsWith("/skript/")) {
      const datei = SKRIPTE.get(req.url.slice("/skript/".length));
      if (!datei) {
        res.writeHead(404);
        res.end();
        return;
      }
      res.writeHead(200, { "content-type": "text/plain; charset=utf-8" });
      res.end(await readFile(path.join(HIER, datei), "utf8"));
      return;
    }
    if (req.method !== "POST") {
      res.writeHead(405);
      res.end();
      return;
    }
    const name = decodeURIComponent(req.url.slice(1)).replace(/[^A-Za-z0-9_.\/-]/g, "_");
    let rumpf = "";
    for await (const stueck of req) rumpf += stueck;
    const datei = path.join(ZIEL, `${name}.json`);
    await mkdir(path.dirname(datei), { recursive: true });
    await writeFile(datei, rumpf);
    console.log(`${new Date().toISOString().slice(11, 19)} ${name} ${rumpf.length} Zeichen`);
    res.writeHead(200);
    res.end(String(rumpf.length));
  })
  .listen(PORT, "127.0.0.1", () => console.log(`Empfänger auf http://127.0.0.1:${PORT}, schreibt nach ${ZIEL}`));
