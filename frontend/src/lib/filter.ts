import { createParser, parseAsIsoDateTime } from "nuqs";

/**
 * Filterzustand liegt in der **URL**, nicht in `useState`.
 *
 * Der Grund ist Teilbarkeit: Wer eine Störung meldet, schickt einen Link, und
 * der Empfänger sieht denselben Ausschnitt — mit einer Ausnahme, die im Entwurf
 * bewusst in Kauf genommen wurde: Der aktive Mandant steht in der Sitzung, nicht
 * in der URL. Deshalb steht er sichtbar in der Kopfzeile.
 *
 * Hier steht der Teil, den **jeder** Listen-Endpunkt hat: das Zeitfenster. Es ist
 * Pflicht (Regel L1), Standard 24 Stunden, Maximum ein Jahr — durchgesetzt wird
 * beides im Backend. Was nur die Nachrichtenliste betrifft (Status, Prozess,
 * Suche, Zwischenschritte, Sortierung), liegt im Feature; `lib` ist
 * Infrastruktur, nie Fachlichkeit.
 *
 * Entstanden in Schritt 3 ohne Wirkung, damit Schritt 4 nicht anfängt,
 * Zeitfenster in Komponentenzustand zu legen und später umzubauen. Seit Schritt 4
 * ist es der Filter der Nachrichtenliste.
 */

/** Zur Erinnerung, was das Backend durchsetzt — **keine** Vorgabe dieser Seite. */
export const ZEITFENSTER_STANDARD_STUNDEN = 24;
export const ZEITFENSTER_MAXIMUM_TAGE = 365;

/**
 * Die wählbaren relativen Zeiträume. Die Codes gehören dem Backend
 * (`common/Zeitraum`); ein unbekannter Wert wird dort mit `400` abgewiesen und
 * **nicht** stillschweigend auf die Vorgabe gezogen.
 *
 * **Relative Zeiträume werden niemals im Frontend gerechnet.** Der Aufrufer
 * schickt `24h`, nicht zwei Zeitpunkte: Käme das Fenster aus der Browseruhr, wäre
 * die Anwendungsuhr des Backends umgangen — und die Liste lokal immer leer, weil
 * die Testkopie Monate hinter der realen Uhrzeit liegt (Regel Z1).
 */
export const ZEITRAEUME = ["24h", "7d", "30d"] as const;

export type Zeitraum = (typeof ZEITRAEUME)[number];

export function istZeitraum(wert: string | null | undefined): wert is Zeitraum {
  return wert !== null && wert !== undefined && (ZEITRAEUME as readonly string[]).includes(wert);
}

/** Ein unbekannter Code landet nicht in der URL — er wäre ein garantiertes `400`. */
export const parseAsZeitraum = createParser<Zeitraum>({
  parse: (wert) => (istZeitraum(wert) ? wert : null),
  serialize: (wert) => wert,
});

/**
 * **Kein `withDefault`.** Fehlt das Zeitfenster, setzt das *Backend* den Standard
 * aus Regel L1. Ein zweiter Standardwert im Frontend liefe dem ersten irgendwann
 * hinterher — und weil beide plausibel aussehen, fiele es niemandem auf.
 */
export const ZEITFENSTER_PARAMETER = {
  zeitraum: parseAsZeitraum,
  von: parseAsIsoDateTime,
  bis: parseAsIsoDateTime,
};

/**
 * Das Zeitfenster, wie es in der URL steht.
 *
 * Die beiden Modi schließen einander aus. Sind `zeitraum` **und** `von`/`bis`
 * zugleich gesetzt, antwortet das Backend mit `400` `zeitfenster-mehrdeutig` — und
 * zwar bewusst statt einer stillen Vorrangregel. Das Frontend lässt den Zustand
 * deshalb gar nicht erst entstehen: {@link mitVorwahl} und {@link mitFreiemFenster}
 * löschen jeweils den anderen Modus.
 */
export type Zeitfensterzustand = {
  zeitraum: Zeitraum | null;
  von: Date | null;
  bis: Date | null;
};

export type Zeitfenstermodus = "vorwahl" | "frei" | "offen";

/**
 * `offen` heißt: Der Nutzer hat nichts gewählt, es gilt der Standard des Backends.
 * Das ist ein **eigener Zustand** und nicht „24 Stunden" — sonst stünde die
 * Vorgabe an zwei Stellen.
 */
export function zeitfenstermodus(zustand: Zeitfensterzustand): Zeitfenstermodus {
  if (zustand.von !== null || zustand.bis !== null) {
    return "frei";
  }
  return zustand.zeitraum !== null ? "vorwahl" : "offen";
}

/**
 * Der Modus, den die Oberfläche **zeigt** — er kann einen Schritt vor dem der URL
 * liegen.
 *
 * „Frei gewählt, aber noch nichts eingetragen" lässt sich in der URL nicht
 * ausdrücken: Ein freies Fenster ohne beide Zeitpunkte ist von „keine Auswahl"
 * nicht zu unterscheiden, beides ist `zeitraum=null, von=null, bis=null`. Und es
 * *soll* sich nicht ausdrücken lassen — der freie Modus beginnt bewusst leer (ein
 * vorbelegtes Fenster wäre ein zweiter Standardwert), und ein leeres freies
 * Fenster zeigt denselben Ausschnitt wie gar keine Auswahl. In der URL steht,
 * was man sieht; hier geht es darum, was der Nutzer gerade ausfüllt.
 *
 * Ohne diese Unterscheidung ist der freie Modus über die Oberfläche **gar nicht
 * erreichbar**: Der Klick auf „Frei" schriebe einen Zustand, der sich vom
 * vorherigen nicht unterscheidet, die Eingabefelder erschienen nie, und nur eine
 * von Hand gebaute URL käme noch in den Modus. Aufgefallen in der Sichtprüfung
 * am 06.08.2026.
 *
 * @param freiGewaehlt Komponentenzustand: Hat der Nutzer „Frei" gedrückt?
 */
export function angezeigterModus(
  zustand: Zeitfensterzustand,
  freiGewaehlt: boolean,
): Zeitfenstermodus {
  const ausDerUrl = zeitfenstermodus(zustand);
  return ausDerUrl === "offen" && freiGewaehlt ? "frei" : ausDerUrl;
}

/** Eine Vorwahl schlägt ein freies Fenster — beide zugleich wären `400`. */
export function mitVorwahl(zeitraum: Zeitraum): Zeitfensterzustand {
  return { zeitraum, von: null, bis: null };
}

/** Und umgekehrt. */
export function mitFreiemFenster(von: Date | null, bis: Date | null): Zeitfensterzustand {
  return { zeitraum: null, von, bis };
}

/** Zurück auf den Standard des Backends. */
export function ohneZeitfenster(): Zeitfensterzustand {
  return { zeitraum: null, von: null, bis: null };
}

/**
 * Das Zeitfenster als Anfrageparameter — **ISO 8601 in UTC**, wie es Richtlinie
 * §5.3 verlangt. `toISOString` liefert genau das.
 *
 * Ein unvollständiges freies Fenster (nur `von` oder nur `bis`) wird
 * mitgeschickt und **nicht** hier abgefangen: Das Backend antwortet
 * `zeitfenster-unvollstaendig`, und die Oberfläche übersetzt den Typ. Zwei
 * Stellen, die dieselbe Prüfung machen, driften auseinander — und die im Browser
 * ist die, auf die kein Verlass ist.
 */
export function zeitfensterAlsParameter(zustand: Zeitfensterzustand): [string, string][] {
  if (zeitfenstermodus(zustand) === "frei") {
    const parameter: [string, string][] = [];
    if (zustand.von !== null) {
      parameter.push(["von", zustand.von.toISOString()]);
    }
    if (zustand.bis !== null) {
      parameter.push(["bis", zustand.bis.toISOString()]);
    }
    return parameter;
  }
  return zustand.zeitraum === null ? [] : [["zeitraum", zustand.zeitraum]];
}
