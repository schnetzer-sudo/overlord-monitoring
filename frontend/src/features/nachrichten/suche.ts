import { createMultiParser, createParser } from "nuqs";

import { ZEITFENSTER_PARAMETER, zeitfensterAlsParameter } from "@/lib/filter";

import type { BamAntwortmodus, BamTrefferWert } from "./api";
import { NACHRICHTEN_PARAMETER } from "./filter";

/**
 * Der Zustand der Belegsuche — **in der URL, nicht im Komponentenzustand.**
 *
 * In der URL stehen: `begriff` (wiederholt), das Zeitfenster als `von`/`bis` und
 * seit Teil 4 der `modus`. Mehr gibt es nicht: kein Zeitraum-Kürzel, kein Status-
 * und kein Prozessfilter, kein Cursor, keine Sortierung. Der Endpunkt kennt sie
 * nicht (`docs/bam-suche.md` §1), und ein Parameter, den niemand liest, ist kein
 * Zustand.
 *
 * **Der Modus gehört dorthin und nicht in `useState`.** Er beschreibt einen
 * anderen *Ausschnitt* — dieselbe Frage, präfixweise beantwortet, findet andere
 * Nachrichten — und keine begonnene Eingabe; damit fällt er im Zweischritt aus
 * `docs/frontend-grundlagen.md` §8 auf die erste Antwort: Die URL kann ihn
 * ausdrücken, also steht er darin. Ein geteilter Link zeigt dieselbe Suche.
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

/**
 * Wie die Suche den Wert vergleicht: **als Ganzes** oder über seinen **Anfang**.
 *
 * Die Codes gehören dem Backend (`bam/Suchmodus`) — klein und deutsch wie jeder
 * andere Parameter dieses Projekts. Ein unbekannter Wert ist dort `400
 * suchmodus-ungueltig` und wird hier gar nicht erst in die URL gelassen.
 *
 * **`exakt` ist die Vorgabe, und sie steht nicht in der URL.** Der Präfixmodus
 * findet mehr, nicht anderes Sortiertes: M49‑3 misst, dass schon ein vollständig
 * eingetippter Wert als Präfix **23 Nachrichten statt einer** findet. Als
 * Voreinstellung änderte er damit die Antwort auch für den Nutzer, der nichts
 * falsch macht — deshalb hängt er am ausdrücklichen Zutun und am leeren Ergebnis
 * (`docs/bam-suche.md` §15).
 */
export const SUCHMODI = ["exakt", "praefix"] as const;

export type Suchmodus = (typeof SUCHMODI)[number];

export function istSuchmodus(wert: string | null | undefined): wert is Suchmodus {
  return wert !== null && wert !== undefined && (SUCHMODI as readonly string[]).includes(wert);
}

/** Ein unbekannter Modus landet nicht in der URL — er wäre ein garantiertes `400`. */
export const parseAsSuchmodus = createParser<Suchmodus>({
  parse: (wert) => (istSuchmodus(wert) ? wert : null),
  serialize: (wert) => wert,
});

/**
 * Der Modus, den die **Antwort** meldet, übersetzt in den der URL.
 *
 * **Die Antwort schreibt ihn groß, der Parameter klein**, und das ist kein
 * Versehen: Die URL-Parameter dieses Projekts sind kleingeschrieben und deutsch
 * (`von`, `bis`, `begriff`), die kontrollierten Vokabulare der Antwort sind es
 * nicht (`statusKind`, `rollen`) — `docs/bam-suche.md` §20.
 *
 * **Ein unbekannter Wert ergibt `undefined` und nicht „exakt".** Das ist die
 * sichere Richtung: Der Rückfall wird nur angeboten, wenn nachweislich exakt
 * gesucht wurde. Wer nicht weiß, welcher Vergleich gelaufen ist, bietet keinen
 * teureren an.
 */
export function modusAusAntwort(gemeldet: BamAntwortmodus | undefined): Suchmodus | undefined {
  return gemeldet === "EXAKT" ? "exakt" : gemeldet === "PRAEFIX" ? "praefix" : undefined;
}

/**
 * Der Deckel des Präfixmodus in Tagen — **dieselbe Zahl wie im Backend, und mit
 * Absicht an beiden Stellen.**
 *
 * Genau wie {@link HOECHSTENS_BEGRIFFE}: Das Backend weist ein größeres Fenster
 * mit `400 praefixsuche-fenster-zu-gross` ab, die Oberfläche lässt es gar nicht
 * erst entstehen (`docs/bam-suche.md` §18). **Wer die Zahl ändert, ändert beide**
 * — hier und `BamSuchfilter.PRAEFIX_FENSTER_MAXIMUM`.
 *
 * **Das ist die eine Stelle, an der dieses Projekt eine Backend-Zahl wiederholt,
 * ohne sie aus einer Antwort zu lesen** — und der Grund ist, dass es die Antwort
 * hier nicht gibt: Die Zahl käme aus dem Fehlerrumpf, und genau diesen Fehler
 * soll die Oberfläche nie auslösen. Ein Wert, den man nur durch den eigenen
 * Fehler erführe, ist keine Quelle.
 *
 * Gemessen ist er in **M50**: Der schlimmste bekannte Präfix des Bestands
 * (1.332.180 Zeilen) kostet über 30 Tage 3,851 s und ist über ein Jahr an der
 * 60‑Sekunden-Grenze abgebrochen.
 */
export const PRAEFIX_FENSTER_TAGE = 30;

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
   * Der Vergleichsmodus — **bewusst ohne `withDefault`.**
   *
   * `null` heißt „keine Angabe" und damit `exakt`, genau wie beim Zeitfenster:
   * Die Vorgabe gehört dem Backend, ein zweiter Standardwert hier liefe dem
   * ersten irgendwann hinterher. Praktisch heißt das, dass **nur der
   * Präfixmodus in der URL steht** — der Normalfall muss nicht erwähnt werden,
   * und `null` setzen *ist* der Rückweg auf „genau suchen".
   */
  modus: parseAsSuchmodus,
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
  modus: Suchmodus | null;
  nachricht: string | null;
};

/** Die Begriffe, immer als Liste — `null` heißt „keiner", nicht „unbekannt". */
export function begriffeAus(zustand: Suchzustand): Suchbegriff[] {
  return zustand.begriff ?? [];
}

/** Der Modus, immer benannt — `null` in der URL heißt `exakt` und nicht „unbekannt". */
export function modusAus(zustand: Suchzustand): Suchmodus {
  return zustand.modus ?? "exakt";
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
 *
 * **`modus=exakt` wird nicht mitgeschickt.** Der Endpunkt sagt zu, dass er sich
 * ohne den Parameter Zeichen für Zeichen wie vor Teil 4 verhält, und
 * `BamSucheDbIT` vergleicht die beiden Rümpfe (`docs/bam-suche.md` §16). Diese
 * Zusage anzunehmen kostet nichts und hält den Abfrageschlüssel — und damit den
 * Zwischenspeicher — für den Normalfall unverändert.
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
  if (modusAus(zustand) === "praefix") {
    parameter.append("modus", "praefix");
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
  return fensterZurueck(bis, JAHRESFENSTER_TAGE);
}

/**
 * Ein Fenster von `tage` Tagen, das auf `bis` endet — **die eine Stelle, an der
 * dieses Projekt aus einer Tageszahl zwei Zeitpunkte macht.**
 *
 * `bis` bleibt immer stehen: Es ist der Zeitpunkt, den das Backend verwendet hat
 * oder den der Nutzer gewählt hat, und in beiden Fällen der Anker (Regel Z1).
 */
export function fensterZurueck(bis: Date, tage: number): { von: Date; bis: Date } {
  return { von: new Date(bis.getTime() - tage * TAG_IN_MS), bis };
}

/** Die Spanne eines Fensters in ganzen Tagen — für die Frage, ob „ein Jahr" noch etwas ändert. */
export function spanneInTagen(von: Date, bis: Date): number {
  return Math.round((bis.getTime() - von.getTime()) / TAG_IN_MS);
}

/**
 * Das Fenster, über das der Präfixmodus laufen darf — **ein Ausschnitt des
 * gewählten, nie ein anderer.**
 *
 * `bis` bleibt stehen, `von` rückt auf `bis` minus {@link PRAEFIX_FENSTER_TAGE}.
 * Ist das gewählte Fenster schon so groß oder kleiner, ändert sich **nichts**,
 * und die Funktion sagt das mit `null` — der Aufrufer schreibt dann keinen
 * Zeitpunkt in die URL und lässt insbesondere die Vorgabe des Backends Vorgabe
 * bleiben.
 *
 * ## Warum das Verkleinern die Regel „es ändert sich genau eine Sache" nicht bricht
 *
 * Die exakte Suche über das große Fenster war **leer**. Über einen Ausschnitt
 * daraus ist sie zwangsläufig ebenfalls leer — das Verkleinern kann am exakten
 * Ergebnis nichts ändern und ist deshalb keine zweite Änderung, sondern eine
 * folgenlose. **Sichtbar gemacht werden muss es trotzdem**, und das ist der Preis
 * dieser Entscheidung: Wer ein Jahr gewählt hat und dreißig Tage bekommt, liest
 * „nicht gefunden" sonst als „nicht vorhanden" (`docs/bam-suche.md` §23).
 *
 * ## Der Anker ist das Fenster der Antwort und nicht die Browseruhr
 *
 * Derselbe Punkt, an dem {@link jahresfensterAb} hängt (Regel Z1): Die
 * Anwendungsuhr steht im Profil `dev` Monate hinter der realen Zeit. Übergeben
 * wird deshalb das Fenster, das das Backend **tatsächlich verwendet** hat.
 */
export function praefixfenster(fenster: { von: Date; bis: Date }): { von: Date; bis: Date } | null {
  if (spanneInTagen(fenster.von, fenster.bis) <= PRAEFIX_FENSTER_TAGE) {
    return null;
  }
  return fensterZurueck(fenster.bis, PRAEFIX_FENSTER_TAGE);
}

/**
 * Ob der Rückfall auf die Präfixsuche angeboten wird — **die vier Bedingungen an
 * einer Stelle, und als reine Funktion statt als Bedingung in einer Komponente.**
 *
 * | Bedingung | Warum |
 * |---|---|
 * | die Antwort meldet `exakt` | Es wird nur angeboten, was noch nicht gelaufen ist. Ein **unbekannter** gemeldeter Modus ist `undefined` und zählt hier nicht als `exakt` ({@link modusAusAntwort}) |
 * | **kein** Treffer | Nur im leeren Ergebnis fehlt die Kehrseite: Dort ist die heutige Antwort leer, und jeder Treffer ist rein zusätzlich. Bei Treffern fände ein vollständig eingetippter Wert als Präfix **23 Nachrichten statt einer** (M49‑3) |
 * | mindestens ein Begriff | Ohne Begriff läuft gar keine Suche, und es gibt nichts zu wiederholen |
 * | **nicht** abgebrochen | Wer gerade an der Zeitgrenze gescheitert ist, bekommt keine **teurere** Suche angeboten. Der Präfixmodus ist die teuerste Zugriffsform dieses Projekts (M50) |
 *
 * Die letzte Bedingung steht ausdrücklich hier und nicht nur in der Reihenfolge
 * der Zweige: Ein Abbruch rendert heute den Abbruchpfad und erreicht den
 * Leerzustand gar nicht — aber das ist eine Eigenschaft des Markups und keine
 * Zusage. Sie wäre bei der nächsten Umstellung still weg.
 */
export function zeigtPraefixAngebot(lage: {
  modus: Suchmodus | undefined;
  treffer: number;
  begriffe: number;
  abgebrochen: boolean;
}): boolean {
  return lage.modus === "exakt" && lage.treffer === 0 && lage.begriffe > 0 && !lage.abgebrochen;
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
