import { createParser, parseAsArrayOf, parseAsBoolean, parseAsString } from "nuqs";

import {
  ZEITFENSTER_PARAMETER,
  zeitfensterAlsParameter,
  type Zeitfensterzustand,
} from "@/lib/filter";

/**
 * Der Filterzustand der Nachrichtenliste — **in der URL, nicht im Komponentenzustand.**
 *
 * In der URL stehen: `zeitraum` **oder** `von`/`bis`, `status`, `prozess`,
 * `suche`, `zwischenschritte`, `sortierung`.
 *
 * ## Was ausdrücklich *nicht* in der URL steht: der Cursor
 *
 * Filter und Zeitfenster ja, die Seitenposition nein. Ein geteilter Link auf
 * Seite sieben eines relativen Fensters zeigte beim Empfänger auf andere Zeilen —
 * das Fenster wird bei ihm neu aufgelöst, und der Cursor zeigt in einen Bereich,
 * den es dort so nicht mehr gibt. **Beim Öffnen eines Links beginnt die Liste
 * deshalb immer auf Seite eins.**
 *
 * Ebenfalls nicht in der URL: der Schalter für die automatische Aktualisierung.
 * Er betrifft die Arbeitsweise des Betrachters, nicht den gezeigten Ausschnitt.
 *
 * ## Dieses Modul ist bewusst frei von React
 *
 * Die Umrechnung Zustand → Anfrage ist eine reine Funktion und wird als solche
 * geprüft (`tests/nachrichtenfilter.test.ts`). Der Hook, der den Zustand an die
 * URL bindet, steht in `hooks.ts`.
 */

/**
 * Die fachlichen Einordnungen aus `common/MessageStatusKind`.
 *
 * **Gefiltert wird über die Einordnung, nie über einen Rohwert.** Ein Nutzer sucht
 * „Fehler", nicht `ERROR_DUPLICATE`; und die Menge der Rohwerte je Kategorie
 * gehört dem Altsystem, nicht der Oberfläche. Das Backend weist einen Rohwert an
 * dieser Stelle mit `400` `status-unbekannt` ab.
 */
export const STATUSARTEN = [
  "FEHLER",
  "WARTEND",
  "LAEUFT",
  "ZWISCHENSCHRITT",
  "ABGESCHLOSSEN",
  "QUITTIERT",
  "UNGEKLAERT",
] as const;

export type Statusart = (typeof STATUSARTEN)[number];

export function istStatusart(wert: string | null | undefined): wert is Statusart {
  return wert !== null && wert !== undefined && (STATUSARTEN as readonly string[]).includes(wert);
}

export const SORTIERUNGEN = ["neueste", "aelteste"] as const;

export type Sortierung = (typeof SORTIERUNGEN)[number];

/** Mindestlänge des Suchbegriffs (Regel L5). Darunter antwortet das Backend `400`. */
export const SUCHE_MINDESTLAENGE = 3;

/**
 * Zwischenschritte (`SPLITTED`, `MERGED`) sind ausgeblendet.
 *
 * Der Grund ist keine technische Erwägung: **34,38 Prozent aller Zeilen** sind
 * Zwischenprodukte. Eine Liste, die zu einem Drittel aus Begriffen besteht, die
 * der Zielnutzer nicht kennt, kostet beim ersten Kontakt Vertrauen.
 *
 * **Anders als beim Zeitfenster steht dieser Wert ausdrücklich in der URL**, ab
 * dem ersten Rendern. Das ist kein Widerspruch zu „kein Standardwert im
 * Frontend", sondern die andere Seite derselben Münze: Hier wird etwas
 * *weggelassen*, und was man sieht, muss man auch teilen können. Bekäme der
 * Empfänger eines Links eine Liste ohne den Chip, sähe er weniger Zeilen als der
 * Absender und wüsste nicht warum.
 */
export const ZWISCHENSCHRITTE_VORGABE = false;

function literalParser<T extends string>(erlaubt: readonly T[]) {
  return createParser<T>({
    parse: (wert) => ((erlaubt as readonly string[]).includes(wert) ? (wert as T) : null),
    serialize: (wert) => wert,
  });
}

/**
 * Die Parameter, so wie sie in der URL stehen.
 *
 * `zwischenschritte` trägt als einziger ein `withDefault` — siehe
 * {@link ZWISCHENSCHRITTE_VORGABE}. Alles andere bleibt ohne: Ein zweiter
 * Standardwert liefe dem des Backends irgendwann hinterher.
 *
 * **`clearOnDefault: false` ist hier der ganze Punkt.** `nuqs` entfernt einen
 * Parameter aus der URL, sobald er dem Standardwert entspricht — sinnvoll, damit
 * URLs kurz bleiben, und hier genau falsch: Der Chip stünde sichtbar auf
 * „ausgeblendet", die URL sagte dazu nichts, und der Empfänger eines Links sähe
 * dieselbe Ansicht mit anderen Zeilen. Was man sieht, muss man teilen können.
 */
export const NACHRICHTEN_PARAMETER = {
  ...ZEITFENSTER_PARAMETER,
  status: parseAsArrayOf(literalParser(STATUSARTEN)),
  prozess: parseAsArrayOf(parseAsString),
  suche: parseAsString,
  zwischenschritte: parseAsBoolean
    .withDefault(ZWISCHENSCHRITTE_VORGABE)
    .withOptions({ clearOnDefault: false }),
  sortierung: literalParser(SORTIERUNGEN),
};

export type Nachrichtenfilter = Zeitfensterzustand & {
  status: Statusart[] | null;
  prozess: string[] | null;
  suche: string | null;
  zwischenschritte: boolean;
  sortierung: Sortierung | null;
};

/** Ist der Suchbegriff lang genug, um ihn überhaupt zu schicken? */
export function sucheTraegt(suche: string | null): suche is string {
  return suche !== null && suche.trim().length >= SUCHE_MINDESTLAENGE;
}

/**
 * Der Filter als Abfragezeichenkette für `/api/nachrichten`.
 *
 * **Was leer ist, wird nicht geschickt.** Ein `?status=` wäre kein Filter auf den
 * leeren Status, sondern Rauschen — und es machte den Abfrageschlüssel des
 * Zwischenspeichers unnötig verschieden.
 *
 * **Ein zu kurzer Suchbegriff wird zurückgehalten.** Nicht, weil das Backend ihn
 * nicht abwiese — es tut es, mit `suchbegriff-zu-kurz` —, sondern weil der Nutzer
 * beim Tippen zwangsläufig durch diesen Zustand läuft. Ihm nach dem zweiten
 * Zeichen eine Fehlermeldung hinzustellen, wäre eine Belehrung für etwas, das er
 * gerade tut. Der Hinweis am Feld sagt stattdessen, was noch fehlt.
 *
 * @param cursor die Seitenposition. Sie kommt aus dem Komponentenzustand und
 *   **nie** aus der URL.
 */
export function alsAbfrage(filter: Nachrichtenfilter, cursor?: string | null): string {
  const parameter = new URLSearchParams();

  for (const [name, wert] of zeitfensterAlsParameter(filter)) {
    parameter.append(name, wert);
  }
  for (const art of filter.status ?? []) {
    parameter.append("status", art);
  }
  for (const prozess of filter.prozess ?? []) {
    parameter.append("prozess", prozess);
  }
  if (sucheTraegt(filter.suche)) {
    parameter.set("suche", filter.suche.trim());
  }
  // Ausdrücklich auch dann, wenn er der Vorgabe entspricht: Was ausgeblendet ist,
  // gehört sichtbar in die Anfrage.
  parameter.set("zwischenschritte", String(filter.zwischenschritte));
  if (filter.sortierung !== null) {
    parameter.set("sortierung", filter.sortierung);
  }
  if (cursor) {
    parameter.set("cursor", cursor);
  }

  const abfrage = parameter.toString();
  return abfrage === "" ? "" : `?${abfrage}`;
}

/**
 * Der Filter als **URL** — das, was ein Nutzer weitergibt.
 *
 * Zwei Unterschiede zu {@link alsAbfrage}, und beide sind Absicht:
 *
 * 1. **Kein Cursor.** Nicht „meistens nicht", sondern nie: Diese Funktion nimmt
 *    keinen entgegen. Ein Link auf Seite sieben eines relativen Fensters zeigte
 *    beim Empfänger auf andere Zeilen.
 * 2. **Der Suchbegriff steht auch dann in der URL, wenn er zu kurz ist.** Die URL
 *    bildet ab, was der Nutzer eingestellt hat — nicht, was gerade abgefragt
 *    wird. Wer einen Link mitten im Tippen weitergibt, gibt seinen Stand weiter
 *    und keinen halben.
 *
 * Die Reihenfolge der Parameter ist fest, damit zwei gleiche Filter dieselbe URL
 * ergeben.
 */
export function alsSuchparameter(filter: Nachrichtenfilter): URLSearchParams {
  const parameter = new URLSearchParams();

  if (filter.zeitraum !== null) {
    parameter.set("zeitraum", filter.zeitraum);
  }
  if (filter.von !== null) {
    parameter.set("von", filter.von.toISOString());
  }
  if (filter.bis !== null) {
    parameter.set("bis", filter.bis.toISOString());
  }
  for (const art of filter.status ?? []) {
    parameter.append("status", art);
  }
  for (const prozess of filter.prozess ?? []) {
    parameter.append("prozess", prozess);
  }
  if (filter.suche !== null && filter.suche !== "") {
    parameter.set("suche", filter.suche);
  }
  // Ausdrücklich, ab dem ersten Rendern — siehe ZWISCHENSCHRITTE_VORGABE.
  parameter.set("zwischenschritte", String(filter.zwischenschritte));
  if (filter.sortierung !== null) {
    parameter.set("sortierung", filter.sortierung);
  }

  return parameter;
}

/**
 * Liest den Filter aus einer URL — die Gegenrichtung zu dem, was `nuqs` beim
 * Rendern tut, als reine Funktion für den Test.
 */
export function ausSuchparametern(suchparameter: URLSearchParams): Nachrichtenfilter {
  const datum = (name: string) => {
    const wert = suchparameter.get(name);
    if (wert === null) {
      return null;
    }
    const zeitpunkt = new Date(wert);
    return Number.isNaN(zeitpunkt.getTime()) ? null : zeitpunkt;
  };
  const zeitraum = suchparameter.get("zeitraum");
  const sortierung = suchparameter.get("sortierung");
  const zwischenschritte = suchparameter.get("zwischenschritte");
  const status = suchparameter.getAll("status").filter(istStatusart);
  const prozess = suchparameter.getAll("prozess").filter((wert) => wert !== "");

  return {
    zeitraum: zeitraum === "24h" || zeitraum === "7d" || zeitraum === "30d" ? zeitraum : null,
    von: datum("von"),
    bis: datum("bis"),
    status: status.length === 0 ? null : status,
    prozess: prozess.length === 0 ? null : prozess,
    suche: suchparameter.get("suche"),
    zwischenschritte:
      zwischenschritte === null ? ZWISCHENSCHRITTE_VORGABE : zwischenschritte === "true",
    sortierung:
      sortierung === "neueste" || sortierung === "aelteste" ? (sortierung as Sortierung) : null,
  };
}
