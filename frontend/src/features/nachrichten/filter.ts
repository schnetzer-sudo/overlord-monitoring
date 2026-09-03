import { createParser, parseAsArrayOf, parseAsBoolean, parseAsString } from "nuqs";

import {
  ZEITFENSTER_PARAMETER,
  zeitfensterAlsParameter,
  type Zeitfensterzustand,
} from "@/lib/filter";
import { ProblemFehler } from "@/lib/http";
import { STATUSARTEN, type Statusart } from "@/lib/status-farbe";

/**
 * Der Filterzustand der Nachrichtenliste — **in der URL, nicht im Komponentenzustand.**
 *
 * In der URL stehen: `zeitraum` **oder** `von`/`bis`, `status`, `prozess`,
 * `suche`, `sortierung` — und seit Schritt 5 die gewählte `nachricht`.
 *
 * **`zwischenschritte` ist am 11.08.2026 entfallen** — mit dem Ausblende-Schalter,
 * den er trug. Ein alter Link, der ihn noch mitbringt, wird nicht abgewiesen: Er
 * wird schlicht übergangen wie jeder unbekannte Suchparameter, und die Liste zeigt
 * ohnehin, was er einblenden sollte (`docs/nachrichtenliste.md` §5).
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
 *
 * **`ZWISCHENSCHRITT` ist am 11.08.2026 in zwei Einträge zerfallen.** Technisch
 * waren `SPLITTED` und `MERGED` dasselbe; für den Nutzer bedeuten sie
 * Gegenteiliges — *aus eins wurde viel* gegen *aus viel wurde eins*. Der Filter
 * bietet sie deshalb einzeln an. Er bleibt eine ausdrückliche Nutzerentscheidung
 * und hat mit der Kette nichts zu tun.
 *
 * **Die Liste selbst wohnt seit dem 01.09.2026 in `lib/status-farbe.ts`** — das
 * Dashboard braucht dieselbe Menge, und ein Feature importiert nicht aus dem
 * Nachbarfeature (`docs/frontend-grundlagen.md` §8). Hier steht nur noch die
 * Ausfuhr für die bestehenden Verwender dieses Moduls.
 */
export { STATUSARTEN };
export type { Statusart };

export function istStatusart(wert: string | null | undefined): wert is Statusart {
  return wert !== null && wert !== undefined && (STATUSARTEN as readonly string[]).includes(wert);
}

export const SORTIERUNGEN = ["neueste", "aelteste"] as const;

export type Sortierung = (typeof SORTIERUNGEN)[number];

/** Mindestlänge des Suchbegriffs (Regel L5). Darunter antwortet das Backend `400`. */
export const SUCHE_MINDESTLAENGE = 3;

/**
 * Die Fenstergrenze der Suche ist bewusst aufgehoben.
 *
 * Bei gesetztem Suchbegriff begrenzt das Backend das Zeitfenster auf 30 Tage und
 * antwortet darüber mit `suche-fenster-zu-gross`; `langeSuche=true` hebt die
 * Grenze bis zur äußeren Grenze an. **Die beiden Zahlen stehen hier nicht** — sie
 * kommen aus der Fehlerantwort. Stünden sie auch im Frontend, liefe eine von
 * beiden der anderen irgendwann hinterher, und weil beide plausibel aussehen,
 * fiele es niemandem auf.
 *
 * **Der Wert steht in der URL wie jeder andere Filter.** Was man sieht, ist was
 * man teilt: Wer einen Link auf eine ausdrücklich lange Suche weitergibt, gibt
 * genau die weiter — und nicht eine, die beim Empfänger mit `400` endet.
 */
export const LANGE_SUCHE_VORGABE = false;

/**
 * `ueberfaellig` ist **kein Filter, sondern eine zweite Abfrageform**
 * (`docs/nachrichtenliste.md` §5b). Vorgabe: aus — die Liste zeigt dann jede
 * Zeile des Fensters.
 *
 * ## ⚠️ Nachgetragen am 01.09.2026, und warum er vorher fehlte
 *
 * Das **Backend** kennt den Parameter seit Schritt 4; die **Oberfläche** kannte
 * ihn nicht. Es gab bis dahin auch keinen Weg zu ihm: Die Filterleiste bietet
 * ihn nicht an, und niemand tippt ihn von Hand. Mit dem Dashboard gibt es einen
 * — die Kachel *Überfällig, im Fenster* verweist genau hierher (Entscheidung
 * E‑m in [`dashboard-frontend.md`](../../../../docs/dashboard-frontend.md)).
 * **Ohne diesen Parameter wäre der Verweis eine Lüge:** `nuqs` überginge ihn
 * stillschweigend, und der Nutzer landete auf der ungefilterten Liste, ohne
 * Hinweis.
 *
 * **Ohne `clearOnDefault: false`, und das ist die Prüfung aus §8.** Die Vorgabe
 * `false` lässt nichts weg — sie zeigt alles. Ein Standardwert, der etwas
 * *weglässt*, gehört in die URL; einer, der etwas *zulässt*, nicht. Genau wie
 * bei {@link LANGE_SUCHE_VORGABE}.
 */
export const UEBERFAELLIG_VORGABE = false;

function literalParser<T extends string>(erlaubt: readonly T[]) {
  return createParser<T>({
    parse: (wert) => ((erlaubt as readonly string[]).includes(wert) ? (wert as T) : null),
    serialize: (wert) => wert,
  });
}

export function istSortierung(wert: string | null | undefined): wert is Sortierung {
  return wert !== null && wert !== undefined && (SORTIERUNGEN as readonly string[]).includes(wert);
}

/**
 * Der Parser der Sortierung — **ausgeführt und nicht zweimal gebaut.**
 *
 * Die Prozessansicht zeigt dieselbe Tabelle und damit denselben
 * Sortierumschalter (`prozessansicht.ts`). Zwei Parser für denselben Parameter
 * liefen bei einem dritten Wert auseinander.
 */
export const parseAsSortierung = literalParser(SORTIERUNGEN);

/**
 * Die Parameter, so wie sie in der URL stehen.
 *
 * **Nur `langeSuche` trägt ein `withDefault`**, und auch das nur, weil ein
 * Wahrheitswert einen braucht. Alles andere bleibt ohne: Ein zweiter Standardwert
 * liefe dem des Backends irgendwann hinterher.
 *
 * Bis zum 11.08.2026 stand hier ein zweiter — `zwischenschritte`, mit
 * `clearOnDefault: false`, damit `nuqs` ihn nicht wieder aus der URL entfernte.
 * Das war richtig, solange die Vorgabe ein Drittel aller Zeilen *wegließ*: Was
 * man sieht, muss man teilen können. Jetzt lässt die Liste nichts mehr weg, und
 * damit gibt es nichts zu teilen (`docs/nachrichtenliste.md` §8.2).
 */
export const NACHRICHTEN_PARAMETER = {
  ...ZEITFENSTER_PARAMETER,
  status: parseAsArrayOf(literalParser(STATUSARTEN)),
  prozess: parseAsArrayOf(parseAsString),
  suche: parseAsString,
  /**
   * Die gewählte Nachricht — der Einhängepunkt der Detailansicht neben der
   * Liste.
   *
   * **Bewusst ohne `withDefault`.** Genau daran hängt die `clearOnDefault`-Falle:
   * `nuqs` entfernt einen Parameter aus der URL, sobald er dem *Standardwert*
   * gleicht — geprüft wird das nur, wenn überhaupt einer gesetzt ist
   * (`parser.defaultValue !== undefined`). Ohne Standardwert kann kein
   * Kennungswert versehentlich verschwinden; `null` entfernt ihn, und genau das
   * ist „Schließen". Käme hier je ein `withDefault` dazu, gehörte
   * `clearOnDefault: false` in derselben Zeile dazu.
   *
   * **`history: "push"` und nicht `replace` wie der Rest der Leiste.** Ein
   * Filter, den man verstellt, ist keine Station, zu der man zurückgeht; eine
   * geöffnete Nachricht ist eine. Am schmalen Fenster füllt die Ansicht den
   * Bildschirm, und das Zurück des Browsers ist dort der Weg heraus — ohne
   * eigenen Verlaufseintrag spränge es an der Liste vorbei.
   */
  nachricht: parseAsString.withOptions({ history: "push" }),
  // **Ohne** `clearOnDefault: false`: Hier wird nichts weggelassen, sondern etwas
  // zugelassen. Steht der Parameter nicht da, gilt die Grenze — und das ist der
  // Normalfall, den keine URL erwähnen muss.
  langeSuche: parseAsBoolean.withDefault(LANGE_SUCHE_VORGABE),
  // Siehe UEBERFAELLIG_VORGABE: dieselbe Bauform wie `langeSuche` und aus
  // demselben Grund ohne `clearOnDefault: false`.
  ueberfaellig: parseAsBoolean.withDefault(UEBERFAELLIG_VORGABE),
  sortierung: parseAsSortierung,
};

export type Nachrichtenfilter = Zeitfensterzustand & {
  status: Statusart[] | null;
  prozess: string[] | null;
  suche: string | null;
  langeSuche: boolean;
  /**
   * Nur überfällige Nachrichten. **Unvereinbar mit einem Statusfilter**, der
   * weder `WARTEND` noch `LAEUFT` enthält — das ist am Endpunkt `400`
   * `ueberfaellig-und-status-unvereinbar`. Die Oberfläche lässt den Zustand gar
   * nicht erst entstehen ({@link ohneUeberfaelligBeiStatus}).
   */
  ueberfaellig: boolean;
  sortierung: Sortierung | null;
  /**
   * Die geöffnete Nachricht. Sie ist Teil des URL-Zustands wie jeder Filter —
   * aber **kein Parameter der Liste**: {@link alsAbfrage} lässt sie weg.
   */
  nachricht: string | null;
};

/**
 * Ein freies Zeitfenster, bei dem genau **einer** der beiden Zeitpunkte steht.
 *
 * Der Zwischenzustand jeder Eingabe eines freien Fensters: Zwischen „Von" und
 * „Bis" gibt es keinen Weg, der ihn überspringt. Er ist **kein Filterzustand,
 * sondern ein Moment beim Ausfüllen** — die Ansicht lässt ihre Liste deshalb
 * stehen, statt sie durch ein Ladeskelett zu ersetzen, dessen Antwort ohnehin
 * nur „es fehlt noch etwas" lauten kann.
 *
 * **Keine zweite Prüfung.** Die Anfrage geht trotzdem hinaus und das Backend
 * entscheidet (`lib/filter.ts`); hier wird nur entschieden, was der Nutzer in
 * der Zwischenzeit sieht.
 */
export function zeitfensterHalb(filter: Nachrichtenfilter): boolean {
  return (filter.von === null) !== (filter.bis === null);
}

/**
 * **Eine Statuswahl beendet die Überfälligkeitsform.**
 *
 * Der Endpunkt weist `ueberfaellig=true` zusammen mit einem `status`, der weder
 * `WARTEND` noch `LAEUFT` enthält, mit `400`
 * `ueberfaellig-und-status-unvereinbar` ab — die Antwort wäre ohne Rücksicht auf
 * die Daten leer. **Die Oberfläche lässt den Zustand gar nicht erst entstehen**,
 * dieselbe Bauform wie bei den beiden Zeitfenstermodi (`lib/filter.ts`
 * {@link mitVorwahl}): Ein Nutzer, der über eine Schaltfläche in einen
 * Fehlerzustand gerät, hat keine Möglichkeit, ihn zu verstehen.
 *
 * **Gelöscht wird auch dann, wenn die Wahl zulässig wäre** — etwa
 * `status=WARTEND`. Das ist Absicht und keine Vereinfachung: `ueberfaellig` ist
 * *kein Filter, sondern eine zweite Abfrageform* (`docs/nachrichtenliste.md`
 * §5b). Wer einen Status wählt, wählt die erste. Eine Regel, die je nach
 * gewähltem Status etwas anderes tut, wäre an der Oberfläche nicht abzulesen.
 *
 * Als reine Funktion und nicht als Bedingung im Hook, damit die Regel prüfbar
 * ist.
 */
export function ohneUeberfaelligBeiStatus(status: Statusart[]): {
  status: Statusart[] | null;
  ueberfaellig: boolean;
} {
  return { status: status.length === 0 ? null : status, ueberfaellig: UEBERFAELLIG_VORGABE };
}

/** Ist der Suchbegriff lang genug, um ihn überhaupt zu schicken? */
export function sucheTraegt(suche: string | null): suche is string {
  return suche !== null && suche.trim().length >= SUCHE_MINDESTLAENGE;
}

/**
 * Die Problemtypen, die einer **Eingabe** gelten und nicht der Ansicht.
 *
 * Alle vier haben dasselbe gemeinsam: Was der Nutzer tun kann, tut er am
 * Suchfeld. Ein Fehlerzustand über der ganzen Ansicht nähme ihm dabei die Liste
 * weg, die er gerade noch gesehen hat — und der Leerzustand behauptete, im
 * Zeitfenster stünde nichts, obwohl gar nicht gesucht wurde.
 *
 * `suche-abgebrochen` gehört dazu, obwohl es kein Prüffehler ist, sondern ein
 * Abbruch an der Zeitgrenze der Datenbank: Auch dort helfen genau die beiden
 * Handlungen, die hier stattfinden — Zeitraum verkleinern, Begriff schärfen. Und
 * eine Schaltfläche „Erneut versuchen" wäre falsch, weil sie dieselbe Abfrage in
 * dieselbe Grenze schickte.
 */
export const AM_SUCHFELD = [
  "suchbegriff-zu-kurz",
  "suchbegriff-zu-unscharf",
  "suche-fenster-zu-gross",
  "suche-abgebrochen",
] as const;

/**
 * Die Problemtypen, die den **Zeitfensterfeldern** gelten und nicht der Ansicht.
 *
 * Derselbe Gedanke wie bei {@link AM_SUCHFELD}, nur an zwei anderen Feldern —
 * und aus demselben Anlass: **Wer ein freies Fenster ausfüllt, ist mitten in
 * einer Eingabe.** Zwischen „Von" und „Bis" liegt zwangsläufig ein Moment, in
 * dem nur einer der beiden Zeitpunkte dasteht. Diesen Moment mit einer roten
 * Meldung über der ganzen Ansicht zu beantworten, hieße dem Nutzer die Liste
 * wegzunehmen, weil er noch nicht fertig getippt hat — genau die Belehrung, die
 * §8.2 für den zu kurzen Suchbegriff bereits ausschließt.
 *
 * **Die Prüfung bleibt im Backend.** Das Frontend hält die Anfrage nicht zurück
 * und rechnet nichts nach; es entscheidet nur, **wo** die Antwort erscheint. Der
 * Unterschied ist wichtig: Eine zweite Prüfung im Browser liefe der ersten
 * irgendwann hinterher (`lib/filter.ts`), eine zweite *Darstellung* kann das
 * nicht.
 *
 * `zeitfenster-mehrdeutig` gehört nicht dazu: Diesen Zustand lässt die
 * Oberfläche gar nicht erst entstehen (§8.2). Käme er trotzdem, ist er ein
 * Befund und gehört sichtbar über die Ansicht.
 */
export const AM_ZEITFENSTER = [
  "zeitfenster-unvollstaendig",
  "zeitfenster-ungueltig",
  "zeitpunkt-ungueltig",
] as const;

function ausKatalog(katalog: readonly string[], fehler: unknown): ProblemFehler | undefined {
  return fehler instanceof ProblemFehler && katalog.includes(fehler.typ) ? fehler : undefined;
}

/**
 * Gehört diese Fehlerantwort an das Suchfeld?
 *
 * Bewusst als reine Funktion und nicht als Bedingung in der Komponente: Es ist
 * eine **Entscheidung**, und Entscheidungen werden hier geprüft, Markup nicht.
 */
export function suchfeldFehler(fehler: unknown): ProblemFehler | undefined {
  return ausKatalog(AM_SUCHFELD, fehler);
}

/** Gehört diese Fehlerantwort an die Zeitfensterfelder? */
export function zeitfensterFehler(fehler: unknown): ProblemFehler | undefined {
  return ausKatalog(AM_ZEITFENSTER, fehler);
}

/**
 * Gehört die Antwort an **irgendein** Feld — und damit nicht über die Ansicht?
 *
 * Die eine Stelle, an der die Ansicht entscheidet, ob sie ihre Liste stehen
 * lässt. Kommt ein weiteres Feld dazu (Schritt 7 bringt die BAM-Suche), wächst
 * hier ein Katalog und nicht eine Bedingung in einer Komponente.
 */
export function feldFehler(fehler: unknown): ProblemFehler | undefined {
  return suchfeldFehler(fehler) ?? zeitfensterFehler(fehler);
}

/**
 * Der Filter als Abfragezeichenkette für `/api/nachrichten`.
 *
 * **`nachricht` steht hier nicht — und das ist der Punkt.** Die gewählte
 * Nachricht ist Zustand der *Ansicht*, kein Filter der Liste: Der Endpunkt kennt
 * den Parameter nicht, und träte er in den Abfrageschlüssel des
 * Zwischenspeichers ein, lüde jeder Klick auf eine Zeile die ganze Liste neu und
 * setzte die Seitenposition zurück. Das Panel lädt über seine eigene Kennung
 * (`NACHRICHTEN_SCHLUESSEL.detail`) und hängt nicht am Ergebnis der Liste.
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
    // Nur zusammen mit dem Suchbegriff: Ohne ihn greift die Grenze im Backend
    // gar nicht, und der Parameter machte nur den Abfrageschlüssel des
    // Zwischenspeichers unnötig verschieden.
    if (filter.langeSuche) {
      parameter.set("langeSuche", "true");
    }
  }
  // **Nur wenn gesetzt.** Ein `ueberfaellig=false` waere kein Filter auf „nicht
  // ueberfaellig", sondern die Vorgabe ein zweites Mal — und es machte den
  // Abfrageschluessel des Zwischenspeichers unnoetig verschieden.
  if (filter.ueberfaellig) {
    parameter.set("ueberfaellig", "true");
  }
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
  // Wie der Suchbegriff selbst: in der URL steht, was der Nutzer eingestellt
  // hat — auch wenn die Anfrage es gerade nicht braucht.
  if (filter.langeSuche) {
    parameter.set("langeSuche", "true");
  }
  if (filter.ueberfaellig) {
    parameter.set("ueberfaellig", "true");
  }
  if (filter.sortierung !== null) {
    parameter.set("sortierung", filter.sortierung);
  }
  // Zuletzt, damit die Kennung am Ende der geteilten URL steht — dort, wo man
  // beim Weitergeben hinsieht. „Schick mir mal den Link" ist die eigentliche
  // Anwendung dieses Parameters.
  if (filter.nachricht !== null && filter.nachricht !== "") {
    parameter.set("nachricht", filter.nachricht);
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
  const langeSuche = suchparameter.get("langeSuche");
  const ueberfaellig = suchparameter.get("ueberfaellig");
  const status = suchparameter.getAll("status").filter(istStatusart);
  const prozess = suchparameter.getAll("prozess").filter((wert) => wert !== "");
  // Eine leere Kennung ist keine Auswahl. Sie entstünde nur aus einer von Hand
  // gebauten URL (`?nachricht=`) und öffnete sonst ein Panel, das garantiert
  // nichts findet.
  const nachricht = suchparameter.get("nachricht");

  return {
    zeitraum: zeitraum === "24h" || zeitraum === "7d" || zeitraum === "30d" ? zeitraum : null,
    von: datum("von"),
    bis: datum("bis"),
    status: status.length === 0 ? null : status,
    prozess: prozess.length === 0 ? null : prozess,
    suche: suchparameter.get("suche"),
    langeSuche: langeSuche === null ? LANGE_SUCHE_VORGABE : langeSuche === "true",
    ueberfaellig: ueberfaellig === null ? UEBERFAELLIG_VORGABE : ueberfaellig === "true",
    // `zwischenschritte` wird hier nicht gelesen und nicht abgewiesen — ein alter
    // Link trägt ihn schlicht ins Leere, wie jeden unbekannten Suchparameter.
    sortierung:
      sortierung === "neueste" || sortierung === "aelteste" ? (sortierung as Sortierung) : null,
    nachricht: nachricht === null || nachricht === "" ? null : nachricht,
  };
}
