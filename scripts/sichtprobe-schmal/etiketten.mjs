#!/usr/bin/env node
/**
 * Die endlichen Beschriftungsmengen der vier Tabellen, in beiden Sprachen —
 * Eingabe für `spaltenbedarf.js` (Sichtprobe am schmalen Fenster, Teil 2).
 *
 * Gelesen aus den Sprachdateien selbst (`frontend/src/i18n/de.ts`, `en.ts`), nicht
 * abgeschrieben: Node entfernt die Typen beim Laden, und beide Dateien tragen nur
 * `import type`. Ändert jemand eine Beschriftung, misst der nächste Lauf die neue.
 *
 *   node scripts/sichtprobe-schmal/etiketten.mjs            → ergebnis/etiketten.json
 *   node scripts/sichtprobe-schmal/etiketten.mjs --drucken  → dasselbe auf stdout, einzeilig
 *
 * Die Spaltenfolge je Tabelle ist die der Komponenten. `art` sagt dem Messkern,
 * welche Rechnung über eine endliche Menge zur Spalte gehört:
 *   zeitpunkt   jede Ziffer an jeder Stelle des angezeigten Werts
 *   status      die acht Einordnungen als Plakette
 *   kette       die vier Kettenrollen einzeln und alle zusammen
 *   nurVorlesen die Kopfbeschriftung ist `sr-only` und braucht keine Breite
 *
 * `woerter` sind die festen Texte, die in einer Zelle stehen **können** — auch
 * die, die in der Testkopie gerade nirgends vorkommen („deaktiviert",
 * „Wechsel erforderlich", „hingesehen, es gibt keinen"). Der Kern misst sie in
 * der Schrift der Zelle: als ganzen Text, wo die Zelle nicht umbricht, sonst
 * Wort für Wort. Ein Zeitpunkt darin steht mit Nullen, weil die Null die
 * breiteste Ziffer ist (gemessen, `ziffern` im Ergebnis der Zeitpunktspalte).
 */
import { mkdir, writeFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";

const HIER = path.dirname(fileURLToPath(import.meta.url));
const I18N = path.resolve(HIER, "..", "..", "frontend", "src", "i18n");

const { de } = await import(pathToFileURL(path.join(I18N, "de.ts")).href);
const { en } = await import(pathToFileURL(path.join(I18N, "en.ts")).href);
const TEXTE = { de, en };

/** `formatiereZeitpunkt` ohne Sekunden, mit der breitesten Ziffer. */
const NULLZEIT = { de: "00.00.0000, 00:00", en: "00/00/0000, 00:00 AM" };

const WOERTER = {
  "benutzer.spalten.rolle": (t) => Object.values(t.rolle),
  "benutzer.spalten.mandanten": (t) => [t.benutzer.ohneMandanten],
  "benutzer.spalten.sperre": (t) => Object.values(t.benutzer.sperre),
  "benutzer.spalten.zeitsperre": (t, s) => [t.benutzer.zeitsperre.keine, t.benutzer.zeitsperre.bis.replace("{zeitpunkt}", NULLZEIT[s])],
  "benutzer.spalten.aktiv": (t) => Object.values(t.benutzer.aktiv),
  "benutzer.spalten.passwort": (t) => Object.values(t.benutzer.passwort),
  "benutzer.spalten.letzteAnmeldung": (t, s) => [t.benutzer.anmeldung.nie, NULLZEIT[s]],
  "katalog.spalten.prozess": (t) => [t.katalog.ohneNamen, t.katalog.auffangprozess],
  "katalog.spalten.projekt": (t) => [t.katalog.ohneNamen],
  "katalog.spalten.partner": (t) => [t.katalog.nichtZugeordnet, ...Object.values(t.katalog.vermerk)],
  "katalog.spalten.richtung": (t) => [t.katalog.nichtZugeordnet, ...Object.values(t.katalog.richtungen)],
  "katalog.spalten.bestand": (t, s) => [
    t.katalog.bestand.traegt,
    t.katalog.bestand.traegtNicht,
    t.katalog.bestand.ungeprueft,
    t.katalog.bestand.geprueftAm.replace("{zeitpunkt}", NULLZEIT[s]),
  ],
  "katalog.spalten.pflege": (t) => Object.values(t.katalog.pflegestatus),
};

const TABELLEN = {
  nachrichtenliste: [
    ["nachrichten.spalten.zeitpunkt", "zeitpunkt"],
    ["nachrichten.spalten.status", "status"],
    ["nachrichten.spalten.ablauf"],
    ["nachrichten.spalten.projekt"],
  ],
  trefferliste: [
    ["nachrichten.spalten.zeitpunkt", "zeitpunkt"],
    ["nachrichten.spalten.status", "status"],
    ["suche.spalten.treffer"],
    ["suche.spalten.kette", "kette"],
    ["nachrichten.spalten.ablauf"],
  ],
  benutzertabelle: [
    ["benutzer.spalten.benutzer"],
    ["benutzer.spalten.rolle"],
    ["benutzer.spalten.mandanten"],
    ["benutzer.spalten.sperre"],
    ["benutzer.spalten.zeitsperre"],
    ["benutzer.spalten.aktiv"],
    ["benutzer.spalten.passwort"],
    ["benutzer.spalten.letzteAnmeldung"],
    ["benutzer.spalten.aktionen", "nurVorlesen"],
  ],
  katalog: [
    ["katalog.spalten.prozess"],
    ["katalog.spalten.projekt"],
    ["katalog.spalten.partner"],
    ["katalog.spalten.richtung"],
    ["katalog.spalten.bestand"],
    ["katalog.spalten.pflege"],
  ],
};

const lies = (texte, schluessel) => {
  const wert = schluessel.split(".").reduce((o, k) => (o == null ? undefined : o[k]), texte);
  if (typeof wert !== "string") throw new Error(`kein Text unter ${schluessel}`);
  return wert;
};

const woerter = (schluessel) => {
  const quelle = WOERTER[schluessel];
  if (!quelle) return undefined;
  const ergebnis = {};
  for (const s of ["de", "en"]) {
    const liste = quelle(TEXTE[s], s);
    if (!liste.every((w) => typeof w === "string" && w.length > 0)) throw new Error(`leerer Text in ${schluessel} (${s})`);
    ergebnis[s] = liste;
  }
  return ergebnis;
};

const ergebnis = {
  quelle: "frontend/src/i18n/de.ts, en.ts",
  tabellen: Object.fromEntries(
    Object.entries(TABELLEN).map(([name, spalten]) => [
      name,
      spalten.map(([schluessel, art]) => ({
        schluessel,
        art: art ?? null,
        de: lies(de, schluessel),
        en: lies(en, schluessel),
        ...(WOERTER[schluessel] ? { woerter: woerter(schluessel) } : {}),
      })),
    ]),
  ),
  status: { de: Object.values(de.einordnung), en: Object.values(en.einordnung) },
  kette: { de: Object.values(de.suche.kette.kurz), en: Object.values(en.suche.kette.kurz) },
};

if (process.argv.includes("--drucken")) {
  process.stdout.write(JSON.stringify(ergebnis));
} else {
  const datei = path.join(HIER, "ergebnis", "etiketten.json");
  await mkdir(path.dirname(datei), { recursive: true });
  await writeFile(datei, JSON.stringify(ergebnis, null, 2));
  console.log(`${datei}: ${Object.keys(ergebnis.tabellen).length} Tabellen`);
}
