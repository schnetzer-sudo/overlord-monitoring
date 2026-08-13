import { createMultiParser } from "nuqs";

import { ZEITFENSTER_PARAMETER, zeitfensterAlsParameter } from "@/lib/filter";

import type { BamTrefferWert } from "./api";
import { NACHRICHTEN_PARAMETER } from "./filter";

/**
 * Der Zustand der Belegsuche — **in der URL, nicht im Komponentenzustand.**
 *
 * In der URL stehen: `begriff` (wiederholt) und das Zeitfenster als `von`/`bis`.
 * Mehr gibt es nicht: kein Zeitraum-Kürzel, kein Status- und kein Prozessfilter,
 * kein Cursor, keine Sortierung. Der Endpunkt kennt sie nicht
 * (`docs/bam-suche.md` §1), und ein Parameter, den niemand liest, ist kein
 * Zustand.
 *
 * ## Warum die Begriffe **wiederholt** in der URL stehen und nicht getrennt
 *
 * `nuqs` kann beides; die Wahl fällt nicht aus Geschmack. Eine Liste in *einem*
 * Parameter braucht ein Trennzeichen — und für jedes Trennzeichen wäre
 * ungemessen, ob ein BAM-Wert es enthält. Genau diese Annahme schließt Regel Q4
 * aus, und der Endpunkt hat sie schon einmal umgangen: Der Doppelpunkt zwischen
 * Typ und Wert ist Pflicht und wird am **ersten** Vorkommen geteilt, womit die
 * Frage gegenstandslos wird statt geraten (`docs/bam-suche.md` §1). Ein
 * Listentrenner holte sie zurück.
 *
 * Umgesetzt über `createMultiParser`: Er liest mit `getAll` und schreibt mit
 * `append` — die URL trägt damit dieselbe Form, die der Endpunkt entgegennimmt.
 *
 * ## Dieses Modul ist bewusst frei von React
 *
 * Die Umrechnung Zustand → Anfrage, die Prüfung auf Doppelte und die Regel für
 * die Nulltreffer-Zeile sind reine Funktionen und werden als solche geprüft
 * (`tests/suche.test.ts`).
 */

/**
 * Ein Suchbegriff, so wie ihn der Endpunkt entgegennimmt.
 *
 * @property typ die gewählte Belegart — `null` heißt **unter jedem Typ**. Er ist
 *   Verfeinerung und keine Pflicht: M36 misst, dass er die Suche nicht
 *   beschleunigt (+1,5 bis +4 Prozent), und die Vorgabe ist deshalb *kein* Typ.
 * @property wert der Wert, wie der Nutzer ihn getippt hat, an den Rändern
 *   beschnitten. **Nicht normalisiert** — die führende Null ergänzt das Backend
 *   aus der Kuratierung und meldet sie in der Antwort zurück
 *   (`docs/bam-suche.md` §3).
 */
export type Suchbegriff = { typ: number | null; wert: string };

/**
 * Das Schutzgeländer aus `docs/bam-suche.md` §1 — **keine fachliche Grenze**.
 *
 * Begrenzt wird die Zahl der Join-Reihenfolgen, die der Optimierer durchprobiert;
 * gemessen ist bis fünf Begriffe (M42‑2, M47), acht ist die Zahl, die das
 * Geländer trägt. Sie steht hier **und** im Backend (`BamSuchfilter`), und das
 * ist Absicht: Das Backend weist den neunten Begriff ab, die Oberfläche lässt ihn
 * gar nicht erst entstehen und sagt warum. Wer die Zahl ändert, ändert beide —
 * ein Frontend, das mehr zuließe, führte den Nutzer in ein `400`.
 */
export const HOECHSTENS_BEGRIFFE = 8;

/** Der Trenner zwischen Typ und Wert. Pflicht, und am **ersten** Vorkommen geteilt. */
const TRENNER = ":";

/**
 * Ein Begriff als Parameterwert: `<typ>:<wert>`, ohne Typ `:<wert>`.
 *
 * <b>Das ist zugleich der Schlüssel der Liste</b> — und er ist eindeutig: Der
 * Typteil enthält nie einen Doppelpunkt, der erste Doppelpunkt ist also immer der
 * Trenner. Ein Schlüssel aus dem Wert allein wäre es nicht: Bei 4,17 Prozent der
 * Paare steht derselbe Wert unter mehreren Typen (M37).
 */
export function alsParameter(begriff: Suchbegriff): string {
  return `${begriff.typ ?? ""}${TRENNER}${begriff.wert}`;
}

/**
 * Die Gegenrichtung — und sie ist streng: Was der Endpunkt mit `400` abwiese,
 * kommt hier gar nicht erst durch.
 *
 * `null` bei fehlendem Trenner, leerem Wert oder einem Typteil, der keine
 * Typnummer ist. Ein unbrauchbarer Begriff aus einer von Hand gebauten URL wird
 * damit **übergangen**, nicht angezeigt — dieselbe Behandlung wie bei jedem
 * unbekannten Suchparameter der Nachrichtenliste.
 */
export function ausParameter(roh: string): Suchbegriff | null {
  const trenner = roh.indexOf(TRENNER);
  if (trenner < 0) {
    return null;
  }
  const typteil = roh.slice(0, trenner).trim();
  const wert = roh.slice(trenner + 1).trim();
  if (wert === "") {
    return null;
  }
  if (typteil === "") {
    return { typ: null, wert };
  }
  // Bewusst nicht `Number.parseInt`: Der nähme auch „90x" an und machte 90 daraus.
  const typ = Number(typteil);
  return Number.isInteger(typ) && typ >= 0 ? { typ, wert } : null;
}

/** Zwei Begriffe sind derselbe, wenn **Typ und Wert** übereinstimmen. */
export function istGleich(einer: Suchbegriff, anderer: Suchbegriff): boolean {
  return alsParameter(einer) === alsParameter(anderer);
}

/**
 * Was beim Hinzufügen herauskommt — **und ob es überhaupt etwas Neues war.**
 *
 * @property begriffe die Liste danach; bei einem Doppelten unverändert
 * @property doppelt der Schlüssel der **vorhandenen** Marke, wenn der Begriff
 *   schon da war. Die Oberfläche hebt sie kurz hervor, statt eine zweite
 *   danebenzustellen — eine zweite Marke mit demselben Inhalt sähe aus, als hätte
 *   der Klick etwas anderes getan als er tat
 * @property voll die Grenze war erreicht, der Begriff ist nicht abgelegt
 */
export type Ergaenzung = {
  begriffe: Suchbegriff[];
  doppelt: string | null;
  voll: boolean;
};

export function ergaenze(begriffe: Suchbegriff[], neuer: Suchbegriff): Ergaenzung {
  const vorhanden = begriffe.find((begriff) => istGleich(begriff, neuer));
  if (vorhanden !== undefined) {
    return { begriffe, doppelt: alsParameter(vorhanden), voll: false };
  }
  if (begriffe.length >= HOECHSTENS_BEGRIFFE) {
    return { begriffe, doppelt: null, voll: true };
  }
  return { begriffe: [...begriffe, neuer], doppelt: null, voll: false };
}

/**
 * Die Begriffe als **wiederholter** Parameter.
 *
 * `null` statt einer leeren Liste: `nuqs` entfernt den Parameter dann aus der
 * URL, und ohne Begriff ist die URL leer — so wie die der Nachrichtenliste ohne
 * Auswahl.
 */
export const parseAsBegriffe = createMultiParser<Suchbegriff[]>({
  parse: (werte) => {
    const begriffe = werte
      .map(ausParameter)
      .filter((begriff): begriff is Suchbegriff => begriff !== null);
    return begriffe.length === 0 ? null : begriffe;
  },
  serialize: (begriffe) => begriffe.map(alsParameter),
  // Ohne eigenen Vergleich prüfte `nuqs` auf Referenzgleichheit, und zwei
  // inhaltsgleiche Listen wären für ihn verschieden.
  eq: (einer, anderer) =>
    einer.length === anderer.length &&
    einer.every((begriff, stelle) => istGleich(begriff, anderer[stelle]!)),
});

/**
 * Die Parameter der Suche.
 *
 * **Nur `von` und `bis` aus dem Zeitfenster, kein `zeitraum`.** Der Endpunkt
 * kennt die Kürzel nicht (`docs/bam-suche.md` §1) — und rechnen darf die
 * Oberfläche sie nicht: Ein im Browser gerechnetes Fenster umginge die
 * Anwendungsuhr (Regel Z1) und wäre im Profil `dev` Monate neben den Daten.
 * Fehlen beide, setzt das Backend seine Vorgabe von 30 Tagen und **nennt sie in
 * der Antwort**; genau daraus baut die Ansicht ihre Zeitfensterzeile.
 */
export const SUCHE_PARAMETER = {
  begriff: parseAsBegriffe,
  von: ZEITFENSTER_PARAMETER.von,
  bis: ZEITFENSTER_PARAMETER.bis,
  /**
   * Die geöffnete Nachricht — **derselbe Parameter wie neben der Liste**, samt
   * seinem `history: "push"`. Kein neuer Mechanismus und keine eigene Route: Was
   * man sieht, muss man teilen können, und am schmalen Fenster ist das Zurück des
   * Browsers der Weg aus der geöffneten Nachricht heraus.
   *
   * **Er steht in der URL und in keiner Abfrage** ({@link alsAbfrage}) — dieselbe
   * Trennung wie in der Nachrichtenliste: Was in der URL steht, beschreibt die
   * Ansicht; was in der Abfrage steht, die Frage an das Backend.
   */
  nachricht: NACHRICHTEN_PARAMETER.nachricht,
};

export type Suchzustand = {
  begriff: Suchbegriff[] | null;
  von: Date | null;
  bis: Date | null;
  nachricht: string | null;
};

/** Die Begriffe, immer als Liste — `null` heißt „keiner", nicht „unbekannt". */
export function begriffeAus(zustand: Suchzustand): Suchbegriff[] {
  return zustand.begriff ?? [];
}

/**
 * Der Zustand als Abfragezeichenkette für `/api/bam/suche`.
 *
 * **Ohne Begriff wird nicht gefragt.** Der Endpunkt gibt es ohne Suchbegriff
 * nicht — jeder Aufruf trägt mindestens einen —, und ein Aufruf ohne wäre ein
 * garantiertes `400`. Die Ansicht zeigt dann ihren Leerzustand.
 *
 * **`nachricht` steht hier nicht.** Die geöffnete Nachricht ist Zustand der
 * *Ansicht* und kein Parameter der Suche; träte sie in den Abfrageschlüssel des
 * Zwischenspeichers ein, liefe bei jedem Klick auf eine Zeile die ganze Suche
 * noch einmal — die teuerste Abfrage dieses Projekts, für eine Ansicht, die ihre
 * Daten ohnehin selbst holt.
 */
export function alsAbfrage(zustand: Suchzustand): string {
  const parameter = new URLSearchParams();
  for (const begriff of begriffeAus(zustand)) {
    parameter.append("begriff", alsParameter(begriff));
  }
  for (const [name, wert] of zeitfensterAlsParameter({
    zeitraum: null,
    von: zustand.von,
    bis: zustand.bis,
  })) {
    parameter.append(name, wert);
  }
  const abfrage = parameter.toString();
  return abfrage === "" ? "" : `?${abfrage}`;
}

/**
 * Wie viele Tage ein Jahresfenster zurückreicht — **365 und nicht „ein
 * Kalenderjahr".**
 *
 * Das Maximum aus Regel L1 ist ein Kalenderjahr (`bis.minusYears(1)`), und das
 * sind im Schaltjahr 366 Tage. Ein hier gerechnetes Kalenderjahr träfe die Grenze
 * genau — und am 29. Februar läge es einen Tag darüber, weil JavaScript den
 * Stichtag auf den 1. März schiebt. 365 Tage liegen immer darunter.
 */
const JAHRESFENSTER_TAGE = 365;

const TAG_IN_MS = 24 * 60 * 60 * 1000;

/**
 * Ein Jahresfenster, **verankert am Fenster aus der Antwort** und nicht an der
 * Uhr des Browsers.
 *
 * Das ist der Unterschied, an dem Regel Z1 hängt: Die Anwendungsuhr steht im
 * Profil `dev` Monate hinter der realen Zeit, und ein aus `Date.now()`
 * gerechnetes Fenster liefe an den Daten vorbei. Das `bis` aus der Antwort ist
 * dagegen genau der Zeitpunkt, den das Backend selbst verwendet hat.
 */
export function jahresfensterAb(bis: Date): { von: Date; bis: Date } {
  return { von: new Date(bis.getTime() - JAHRESFENSTER_TAGE * TAG_IN_MS), bis };
}

/** Die Spanne eines Fensters in ganzen Tagen — für die Frage, ob „ein Jahr" noch etwas ändert. */
export function spanneInTagen(von: Date, bis: Date): number {
  return Math.round((bis.getTime() - von.getTime()) / TAG_IN_MS);
}

/**
 * Die Spalte „Treffer": **welche Belegart getroffen hat, nicht welcher Wert.**
 *
 * Den Wert hat der Nutzer selbst getippt, er steht in seiner Marke über der
 * Liste; ihn je Zeile zu wiederholen wäre Rauschen — alle Zeilen haben denselben
 * Begriff getroffen. Was er **nicht** weiß, ist, worauf die Nummer getroffen hat,
 * und genau das entscheidet, ob er den richtigen Beleg vor sich hat.
 *
 * **Gezählt werden verschiedene Typen und nicht Zeilen.** Zwei Werte desselben
 * Typs sind eine Belegart und keine zwei; ein `+1` dahinter behauptete sonst eine
 * Vielfalt, die es nicht gibt. Mehrere Typen auf derselben Nachricht sind kein
 * Randfall: M37 misst 4,17 Prozent.
 *
 * @returns `null`, wenn nichts dasteht — dann bleibt die Zelle leer, statt einen
 *   Ersatztext zu erfinden
 */
export function trefferTypen(
  treffer: BamTrefferWert[],
): { erste: string; weitere: number; alle: string[] } | null {
  const gesehen = new Set<number>();
  const alle: string[] = [];
  for (const eintrag of treffer) {
    if (!gesehen.has(eintrag.typ)) {
      gesehen.add(eintrag.typ);
      alle.push(eintrag.bezeichnung);
    }
  }
  const erste = alle[0];
  return erste === undefined ? null : { erste, weitere: alle.length - 1, alle };
}

/**
 * Die Fassungen, nach denen zusätzlich gesucht wurde — **nur wenn sie von der
 * Eingabe abweichen.**
 *
 * Die Antwort führt die Eingabe an erster Stelle; steht sonst nichts darin, gab
 * es keine Normalisierung und es gibt nichts zu melden. **Keine stille
 * Korrektur**, aber auch keine Zeile, die nur wiederholt, was der Nutzer getippt
 * hat.
 */
export function abweichendeVarianten(eingabe: string, varianten: string[]): string[] {
  return varianten.filter((variante) => variante !== eingabe);
}

/**
 * Die letzte Runde — <b>samt der Zahl der Begriffe, mit der sie zustande kam</b>.
 *
 * **`abgeschnitten` gehört dazu, und das ist keine Kleinigkeit.** Greift das
 * harte Limit, ist die gelieferte Zahl nicht die Trefferzahl, sondern die
 * Seitengröße. Eine Nulltreffer-Zeile, die daraus „ohne ihn: 50" machte, nennte
 * eine Zahl, die es so nicht gibt — ausgerechnet in der einen Zeile, die den
 * Nutzer vor einem falschen Schluss bewahren soll.
 */
export type VorigeRunde = { begriffe: number; treffer: number; abgeschnitten: boolean };

/**
 * Die Nulltreffer-Zeile: **„Mit diesem Begriff: 0. Ohne ihn: 12."**
 *
 * Jede Marke verengt. Landet die dritte bei null, sieht der Nutzer nicht, welche
 * es war — und mit einer nicht mitgetippten führenden Null passiert genau das.
 * Die vorige Trefferzahl steht ohnehin schon auf dem Schirm; sie zu behalten
 * kostet **keine** zusätzliche Abfrage.
 *
 * **Sie erscheint nur, wenn wirklich ein Begriff dazugekommen ist.** Wer das
 * Zeitfenster verkleinert und dabei auf null fällt, bekommt sie nicht: „ohne ihn"
 * benennte dann etwas, das gar nicht die Ursache war.
 *
 * @returns die **ganze** vorige Runde, oder `null`, wenn die Zeile nicht gilt.
 *   Nicht nur die Zahl: War die vorige Runde abgeschnitten, ist sie *„mehr als
 *   50"* und nicht *„50"*, und die Zeile muss das sagen können
 *   ({@link VorigeRunde})
 */
export function nulltrefferHinweis(
  vorige: VorigeRunde | null,
  aktuelleBegriffe: number,
  aktuelleTreffer: number,
): VorigeRunde | null {
  if (aktuelleTreffer > 0 || vorige === null || vorige.treffer === 0) {
    return null;
  }
  return vorige.begriffe < aktuelleBegriffe ? vorige : null;
}
