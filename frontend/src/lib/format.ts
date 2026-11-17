import type { Sprache } from "@/i18n";

/**
 * Datum, Zeit und Zahlen über `Intl`.
 *
 * ## Die eine Entscheidung, um die es hier geht
 *
 * Ein Zeitstempel durchläuft drei Stationen, und an jeder bedeutet er etwas
 * anderes:
 *
 * ```
 * GlassfishDB     2025-12-29 23:53:50     Wanduhrzeit des Altsystem-Servers, ohne Zone
 * API             2025-12-29T22:53:50Z    UTC — dort ist der Wert eindeutig
 * Anzeige         29.12.2025, 23:53       wieder die Wanduhrzeit
 * ```
 *
 * Die mittlere Station ist nicht verhandelbar: `von` und `bis` müssen einen
 * Punkt auf der Zeitachse benennen, und das kann nur UTC (Richtlinie §5.3). Die
 * letzte ist es ebenso wenig — der Nutzer hält den Zeitpunkt gegen das
 * Altwerkzeug, und dort steht 23:53.
 *
 * **Formatiert wird deshalb in einer festen Zone, nicht in der des Browsers.**
 * Die Zone liefert das Backend mit der Selbstauskunft (`anzeigezone`); es ist
 * dieselbe, mit der `common/Zeitpunkte` die Wanduhrzeit nach UTC umrechnet. Die
 * Kette schließt sich damit über genau einen Wert, der an genau einer Stelle
 * gepflegt wird und beim Umzug des Servers nicht auseinanderläuft.
 *
 * Ein Nutzer in München und einer in Antwerpen sehen dieselbe Uhrzeit, und beide
 * dieselbe wie im Altsystem. Das ist Absicht: Der Zeitpunkt ist hier eine
 * **Eigenschaft des Belegs**, kein Termin im Kalender des Betrachters.
 *
 * ## Was hier vorher stand
 *
 * Bis zum 06.08.2026 las diese Datei die gelieferten Felder einzeln und zeigte
 * sie „wie geliefert" an — ohne jede Umrechnung, ausdrücklich auch bei
 * angehängtem `Z`. Für Schritt 3 war das richtig: Es gab keinen Endpunkt, der
 * Zeitstempel aus `GlassfishDB` lieferte, und die Regel bewahrte davor, einen
 * zonenlosen Wert durch die Browserzone zu schicken. Mit dem Listen-Endpunkt aus
 * Schritt 4 wanderte die Umrechnung ins Backend — und ab da war „wie geliefert"
 * genau die Verschiebung, die sie verhindern sollte: Aus 23:53 in der Datenbank
 * wurde 22:53 in der Anzeige, im Sommer 21:53.
 */

/** Ein ISO-Wert mit ausdrücklichem Zonenversatz — `…Z`, `…+02:00` oder `…+0200`. */
const MIT_ZONENVERSATZ = /(?:Z|[+-]\d{2}:?\d{2})$/;

/**
 * Die Zone, in der formatiert wird, wenn keine brauchbare vorliegt — etwa weil
 * die Selbstauskunft noch lädt.
 *
 * **UTC und nicht die Zone des Browsers.** Ein Rückfall auf die Browserzone wäre
 * genau der Fehler, den diese Datei verhindert, nur seltener und damit schwerer
 * zu finden: Die Anzeige wäre je nach Standort verschoben, ohne dass irgendwo
 * etwas fehlschlägt. UTC ist dagegen für alle gleich und weicht sichtbar ab.
 */
export const ZEITZONE_RUECKFALL = "UTC";

/**
 * Liest einen Zeitstempel der API.
 *
 * Fehlt der Zonenversatz, wird der Wert **als UTC** gelesen und nicht als
 * Ortszeit des Browsers. `new Date("2025-12-29T22:53:50")` täte Letzteres — der
 * Wert bekäme je nach Standort eine andere Bedeutung, obwohl die API laut
 * Richtlinie §5.3 ausschließlich UTC überträgt.
 */
function alsZeitpunkt(wert: string): Date | null {
  const roh = wert.trim().replace(" ", "T");
  const zeitpunkt = new Date(MIT_ZONENVERSATZ.test(roh) ? roh : `${roh}Z`);
  return Number.isNaN(zeitpunkt.getTime()) ? null : zeitpunkt;
}

/**
 * Ein ungültiger Zonenname lässt `Intl` mit `RangeError` scheitern. Das darf
 * keine Tabellenzelle sprengen — gezeigt wird dann UTC, siehe
 * {@link ZEITZONE_RUECKFALL}.
 */
function formatierer(
  sprache: Sprache,
  zone: string | undefined,
  optionen: Intl.DateTimeFormatOptions,
): Intl.DateTimeFormat {
  try {
    return new Intl.DateTimeFormat(sprache, { ...optionen, timeZone: zone ?? ZEITZONE_RUECKFALL });
  } catch {
    return new Intl.DateTimeFormat(sprache, { ...optionen, timeZone: ZEITZONE_RUECKFALL });
  }
}

const ZEITPUNKT: Intl.DateTimeFormatOptions = {
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
  hour: "2-digit",
  minute: "2-digit",
};

/** Mit Sekunden — für den Tooltip, wo der genaue Wert gefragt ist. */
const ZEITPUNKT_GENAU: Intl.DateTimeFormatOptions = { ...ZEITPUNKT, second: "2-digit" };

const UHRZEIT_GENAU: Intl.DateTimeFormatOptions = {
  hour: "2-digit",
  minute: "2-digit",
  second: "2-digit",
};

const DATUM: Intl.DateTimeFormatOptions = {
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
};

/**
 * Zeitpunkt in der aktiven Sprache und der übergebenen Anzeigezone.
 *
 * Ein unlesbarer Wert wird unverändert durchgereicht — lieber roh als falsch.
 */
export function formatiereZeitpunkt(
  wert: string | null | undefined,
  sprache: Sprache,
  zone: string | undefined,
): string {
  if (!wert) {
    return "";
  }
  const zeitpunkt = alsZeitpunkt(wert);
  return zeitpunkt === null ? wert : formatierer(sprache, zone, ZEITPUNKT).format(zeitpunkt);
}

/** Wie {@link formatiereZeitpunkt}, zusätzlich mit Sekunden. */
export function formatiereZeitpunktGenau(
  wert: string | null | undefined,
  sprache: Sprache,
  zone: string | undefined,
): string {
  if (!wert) {
    return "";
  }
  const zeitpunkt = alsZeitpunkt(wert);
  return zeitpunkt === null ? wert : formatierer(sprache, zone, ZEITPUNKT_GENAU).format(zeitpunkt);
}

/**
 * Nur die Uhrzeit, mit Sekunden: **die hintere Hälfte von
 * {@link formatiereZeitpunktGenau}**, in derselben Sprache und Zone. Für die Zeile
 * eines Tages, dessen Datum schon dasteht (`docs/nachrichtendetail.md` §10.19).
 */
export function formatiereUhrzeitGenau(
  wert: string | null | undefined,
  sprache: Sprache,
  zone: string | undefined,
): string {
  if (!wert) {
    return "";
  }
  const zeitpunkt = alsZeitpunkt(wert);
  return zeitpunkt === null ? wert : formatierer(sprache, zone, UHRZEIT_GENAU).format(zeitpunkt);
}

export function formatiereDatum(
  wert: string | null | undefined,
  sprache: Sprache,
  zone: string | undefined,
): string {
  if (!wert) {
    return "";
  }
  const zeitpunkt = alsZeitpunkt(wert);
  return zeitpunkt === null ? wert : formatierer(sprache, zone, DATUM).format(zeitpunkt);
}

/**
 * Nur die Uhrzeit — für die **Achsenbeschriftung** eines Stundendiagramms.
 *
 * Dort steht das Datum schon in der Überschrift des Zeitraums und würde jede
 * Beschriftung dreimal so breit machen; bei 48 Eimern entscheidet die Breite,
 * wie viele überhaupt lesbar sind.
 */
const UHRZEIT: Intl.DateTimeFormatOptions = { hour: "2-digit", minute: "2-digit" };

/** Tag und Monat ohne Jahr — die Achse eines Tagesdiagramms über 30 Tage. */
const TAG_MONAT: Intl.DateTimeFormatOptions = { day: "2-digit", month: "2-digit" };

/** Monat und Jahr — die Achse eines Monatsdiagramms über zwölf Monate. */
const MONAT_JAHR: Intl.DateTimeFormatOptions = { month: "2-digit", year: "numeric" };

/**
 * Die drei Achsenauflösungen eines Zeitdiagramms.
 *
 * **Sie stehen hier und nicht im Diagramm.** Eine Achsenbeschriftung ist ein
 * formatierter Zeitpunkt wie jeder andere: Sie läuft über dieselbe
 * {@link ZEITZONE_RUECKFALL}-Regel, dieselbe Sprache und denselben unlesbaren
 * Rückfall. Eine zweite `Intl`-Stelle in einer Komponente wäre genau die Drift,
 * gegen die diese Datei angelegt ist.
 */
export type Zeitaufloesung = "stunde" | "tag" | "monat";

const ACHSE: Record<Zeitaufloesung, Intl.DateTimeFormatOptions> = {
  stunde: UHRZEIT,
  tag: TAG_MONAT,
  monat: MONAT_JAHR,
};

/** Ein Zeitpunkt in der Auflösung, die zur Eimerbreite passt. */
export function formatiereAchsenzeit(
  wert: string | null | undefined,
  aufloesung: Zeitaufloesung,
  sprache: Sprache,
  zone: string | undefined,
): string {
  if (!wert) {
    return "";
  }
  const zeitpunkt = alsZeitpunkt(wert);
  return zeitpunkt === null
    ? wert
    : formatierer(sprache, zone, ACHSE[aufloesung]).format(zeitpunkt);
}

/** Absteigend geprüft: die erste Einheit, von der mindestens eine ganze vergangen ist. */
const EINHEITEN: readonly (readonly [Intl.RelativeTimeFormatUnit, number])[] = [
  ["year", 365 * 24 * 60 * 60],
  ["month", 30 * 24 * 60 * 60],
  ["day", 24 * 60 * 60],
  ["hour", 60 * 60],
  ["minute", 60],
  ["second", 1],
];

/**
 * Der Abstand zu **jetzt**, in Worten: „vor 3 Stunden".
 *
 * Gedacht als Ergänzung zum absoluten Zeitpunkt, nie als Ersatz. Der absolute
 * Wert ist der, den man gegen das Altwerkzeug hält und in eine Störungsmeldung
 * schreibt; der relative sagt auf einen Blick, ob etwas gerade eben passiert ist.
 *
 * **Der Bezugspunkt ist die Uhr des Browsers**, nicht die Anwendungsuhr des
 * Backends. In Produktion ist das dasselbe. Im Profil `dev` liegt die Testkopie
 * Monate zurück, und der Tooltip liest sich entsprechend („vor 7 Monaten") — er
 * sagt dann die Wahrheit über die realen Daten und nicht über die verstellte
 * Uhr. Der absolute Wert daneben bleibt davon unberührt.
 *
 * @param jetzt Bezugspunkt. Als Parameter, damit die Ausgabe prüfbar ist.
 */
export function formatiereRelativ(
  wert: string | null | undefined,
  sprache: Sprache,
  jetzt: Date = new Date(),
): string {
  if (!wert) {
    return "";
  }
  const zeitpunkt = alsZeitpunkt(wert);
  if (zeitpunkt === null) {
    return wert;
  }
  const sekunden = Math.round((zeitpunkt.getTime() - jetzt.getTime()) / 1000);
  const format = new Intl.RelativeTimeFormat(sprache, { numeric: "auto" });
  for (const [einheit, laenge] of EINHEITEN) {
    if (Math.abs(sekunden) >= laenge) {
      return format.format(Math.trunc(sekunden / laenge), einheit);
    }
  }
  return format.format(0, "second");
}

/** `TT.MM.JJJJ HH:MM` — die Schreibweise, in der ein Auftrag Zeitpunkte an die Plattform gibt. */
const ROHZEITPUNKT = /^(\d{2})\.(\d{2})\.(\d{4}) (\d{2}):(\d{2})$/;

/**
 * Ein Wert, den der Kunde **als Text** eingegeben hat und der ganz ein Zeitpunkt
 * in der Schreibweise `TT.MM.JJJJ HH:MM` ist — etwa der Start eines
 * Neudurchlaufs („25.03.2026 10:35“) —, im **Format von
 * {@link formatiereZeitpunkt}**: „25.03.2026, 10:35“ (E‑1191,
 * `docs/assistent-frontend.md` §12.15). Jeder andere Wert kommt unverändert
 * zurück — lieber roh als falsch.
 *
 * **Erkannt am Wert, nicht am Feld.** Die Typdefinition kennt keine Feldart
 * (`docs/tasks.md` §3.2), und am Namen des Felds wird nichts entschieden.
 * Erkannt wird nur die vollständige Schreibweise mit einem Tag, den es gibt,
 * und einer Uhrzeit bis 23:59.
 *
 * **Ohne Umrechnung.** Der Wert ist eine Wanduhrzeit ohne Zone und keine
 * UTC-Angabe der API. Er wird deshalb als UTC gelesen und in UTC formatiert:
 * Ziffern und Tag bleiben, wie sie eingegeben sind, und nur die Form ist die
 * aller Zeitpunkte im Block — mit Komma, in jeder Sprache die von `Intl`.
 */
export function formatiereRohzeitpunkt(wert: string, sprache: Sprache): string {
  const teile = ROHZEITPUNKT.exec(wert);
  if (teile === null) {
    return wert;
  }
  const [tag, monat, jahr, stunde, minute] = teile.slice(1).map(Number);
  const zeitpunkt = new Date(Date.UTC(jahr, monat - 1, tag, stunde, minute));
  // `Date.UTC` rechnet einen 31.02. still in den März und 24:00 in den
  // nächsten Tag um; dann ist es keiner der Zeitpunkte, die gemeint sein können.
  const gibtEsIhn =
    zeitpunkt.getUTCFullYear() === jahr &&
    zeitpunkt.getUTCMonth() === monat - 1 &&
    zeitpunkt.getUTCDate() === tag &&
    zeitpunkt.getUTCHours() === stunde &&
    zeitpunkt.getUTCMinutes() === minute;
  return gibtEsIhn ? formatierer(sprache, "UTC", ZEITPUNKT).format(zeitpunkt) : wert;
}

export function formatiereZahl(wert: number, sprache: Sprache): string {
  return new Intl.NumberFormat(sprache).format(wert);
}

/**
 * Ein Anteil zwischen 0 und 1 als Prozentangabe, in der aktiven Sprache.
 *
 * **Über `Intl` und nicht von Hand.** Das Prozentzeichen steht nicht in jeder
 * Sprache an derselben Stelle und nicht mit demselben Abstand davor — deutsch
 * `56 %`, englisch `56%`. Eine selbst zusammengesetzte Zeichenkette wäre in
 * genau einer der beiden Sprachen falsch, und zwar unauffällig.
 *
 * **Ohne Nachkommastellen.** Der Fortschritt der Kuratierung ist eine
 * Größenordnung, keine Messung; `56,3 %` behauptete eine Genauigkeit, die die
 * Zahl nicht hat.
 */
export function formatiereAnteil(anteil: number, sprache: Sprache): string {
  const wert = Number.isFinite(anteil) ? anteil : 0;
  return new Intl.NumberFormat(sprache, {
    style: "percent",
    maximumFractionDigits: 0,
  }).format(wert);
}

/**
 * Die Einheitenbausteine für {@link formatiereDauer}.
 *
 * **Sie kommen aus der Sprachdatei und stehen nicht hier.** Auch „s" und „min"
 * sind Text, den ein Nutzer sieht — und eine Ausnahme von der Regel „keine
 * Zeichenkette außerhalb von `i18n`" für kurze Wörter ist genau die Ausnahme,
 * die die Regel aufweicht.
 */
export type Dauereinheiten = {
  /** Für alles unter einer Sekunde — das Backend liefert dort `0`. */
  unterSekunde: string;
  sekunden: string;
  minuten: string;
  stunden: string;
  tage: string;
};

const MINUTE = 60;
const STUNDE = 60 * MINUTE;
const TAG = 24 * STUNDE;

/**
 * Eine Dauer in ganzen Sekunden als lesbarer Text: `3 h 12 min`, `45 s`.
 *
 * **Höchstens zwei Einheiten.** „1 h 3 min 7 s" beantwortet keine Frage, die
 * „1 h 3 min" nicht schon beantwortet — und die Zeitleiste braucht eine Spalte,
 * die in jeder Zeile gleich breit bleibt.
 *
 * **`0` wird zu „< 1 s" und nicht zu „0 s".** Das Backend rechnet die Dauer in
 * ganzen Sekunden; ein Schritt mit `0` hat zwischen null und einer Sekunde
 * gedauert. „0 s" behauptete eine Genauigkeit, die die Zahl nicht hat.
 *
 * Eine negative Dauer kommt nicht vor — das Backend liefert dort `null` — und
 * wird hier wie `0` behandelt, statt ein Minuszeichen anzuzeigen.
 */
export function formatiereDauer(sekunden: number, einheiten: Dauereinheiten): string {
  const ganz = Math.floor(sekunden);
  if (!Number.isFinite(ganz) || ganz <= 0) {
    return einheiten.unterSekunde;
  }
  const teil = (baustein: string, wert: number) => baustein.replace("{wert}", String(wert));

  if (ganz < MINUTE) {
    return teil(einheiten.sekunden, ganz);
  }
  if (ganz < STUNDE) {
    const rest = ganz % MINUTE;
    const minuten = teil(einheiten.minuten, Math.floor(ganz / MINUTE));
    return rest === 0 ? minuten : `${minuten} ${teil(einheiten.sekunden, rest)}`;
  }
  if (ganz < TAG) {
    const rest = Math.floor((ganz % STUNDE) / MINUTE);
    const stunden = teil(einheiten.stunden, Math.floor(ganz / STUNDE));
    return rest === 0 ? stunden : `${stunden} ${teil(einheiten.minuten, rest)}`;
  }
  const rest = Math.floor((ganz % TAG) / STUNDE);
  const tage = teil(einheiten.tage, Math.floor(ganz / TAG));
  return rest === 0 ? tage : `${tage} ${teil(einheiten.stunden, rest)}`;
}

/* ─────────────────────────────────────────────────────────────────────────────
   Wanduhrzeit ↔ Zeitpunkt — für die Eingabefelder des freien Zeitfensters

   Ein `<input type="datetime-local">` kennt keine Zone: Es liefert und erwartet
   „2025-12-29T00:00" als reine Wanduhrzeit. Läse man diesen Wert mit `new Date()`,
   bekäme er die Zone des Browsers — und das freie Zeitfenster wäre gegen die Daten
   verschoben, sobald jemand nicht zufällig in der Zone des Servers sitzt. Genau der
   Fehler, den `formatiereZeitpunkt` oben vermeidet, nur an der Eingabe statt an der
   Anzeige.

   Deshalb wird auch hier in der **Anzeigezone** gerechnet, mit `Intl` und ohne
   Bibliothek.
   ───────────────────────────────────────────────────────────────────────────── */

/** Teilt einen Zeitpunkt in die Wanduhrzeitfelder einer Zone auf. */
const TEILE: Intl.DateTimeFormatOptions = {
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
  hour: "2-digit",
  minute: "2-digit",
  second: "2-digit",
  // `h23` und nicht `hour12: false`: Letzteres liefert für Mitternacht je nach
  // ICU-Fassung „24" statt „00".
  hourCycle: "h23",
};

/**
 * Der Versatz der Zone zu **diesem** Zeitpunkt, in Millisekunden. Positiv östlich
 * von Greenwich. Nicht konstant — Sommerzeit.
 */
function zonenversatz(zeitpunkt: Date, zone: string): number {
  const teile = new Intl.DateTimeFormat("en-US", { ...TEILE, timeZone: zone }).formatToParts(
    zeitpunkt,
  );
  const feld = (art: Intl.DateTimeFormatPartTypes) =>
    Number(teile.find((teil) => teil.type === art)?.value ?? "0");
  const alsUtc = Date.UTC(
    feld("year"),
    feld("month") - 1,
    feld("day"),
    feld("hour"),
    feld("minute"),
    feld("second"),
  );
  // Gegen die volle Sekunde gerechnet: Die Teile tragen keine Millisekunden, und
  // ohne das Abschneiden gingen sie als Teil des Versatzes durch — in zwei
  // Durchgängen doppelt (gefunden am 17.11.2026, E‑1198, seit die Felder
  // Millisekunden einer Adresse zurückgeben).
  return alsUtc - (zeitpunkt.getTime() - zeitpunkt.getUTCMilliseconds());
}

/**
 * Liest eine Wanduhrzeit (`2025-12-29T00:00`) **als Zeit in der Anzeigezone** und
 * liefert den Zeitpunkt, der auf die Leitung geht.
 *
 * **Zwei Durchgänge, und das ist kein Feinschliff.** Der Versatz hängt am
 * Zeitpunkt, den wir gerade erst suchen. Beim ersten Durchgang wird er am falschen
 * Zeitpunkt abgelesen — an den beiden Umstellungstagen im Jahr liegt er dann um
 * eine Stunde daneben. Der zweite Durchgang liest ihn am Ergebnis des ersten ab
 * und trifft.
 *
 * Bleibt die eine Stunde, die es zweimal gibt (Rückstellung im Herbst), und die
 * eine, die es nicht gibt (Vorstellung im Frühjahr). Dort ist die Eingabe
 * mehrdeutig beziehungsweise unmöglich; das Ergebnis ist dann der frühere
 * beziehungsweise der nächstgelegene Zeitpunkt. Für eine Fenstergrenze ist das
 * folgenlos — eine Stunde Unschärfe an einer Grenze, die der Nutzer selbst grob
 * wählt.
 */
export function zeitpunktAusWanduhrzeit(wanduhrzeit: string, zone: string): Date | null {
  const roh = wanduhrzeit.trim();
  // Sekunden und Millisekunden sind freiwillig: Eine Adresse kann sie tragen
  // („Auf ein Jahr erweitern" rechnet vom `bis` der Antwort), und die Felder
  // geben sie unverändert zurück (E‑1198).
  if (!/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2}(\.\d{1,3})?)?$/.test(roh)) {
    return null;
  }
  // Erst so lesen, als wäre die Wanduhrzeit UTC — dann den Versatz abziehen.
  const naiv = Date.parse(`${roh.length === 16 ? `${roh}:00` : roh}Z`);
  if (Number.isNaN(naiv)) {
    return null;
  }
  const ersterVersuch = naiv - zonenversatz(new Date(naiv), zone);
  return new Date(naiv - zonenversatz(new Date(ersterVersuch), zone));
}

/**
 * Die Gegenrichtung: ein Zeitpunkt als Wanduhrzeit der Anzeigezone, im Format,
 * das `<input type="datetime-local">` erwartet (`2025-12-29T00:00`).
 */
export function wanduhrzeitFuerEingabe(zeitpunkt: Date | null, zone: string): string {
  if (zeitpunkt === null || Number.isNaN(zeitpunkt.getTime())) {
    return "";
  }
  const teile = new Intl.DateTimeFormat("en-US", { ...TEILE, timeZone: zone }).formatToParts(
    zeitpunkt,
  );
  const feld = (art: Intl.DateTimeFormatPartTypes) =>
    teile.find((teil) => teil.type === art)?.value ?? "00";
  return `${feld("year")}-${feld("month")}-${feld("day")}T${feld("hour")}:${feld("minute")}`;
}

/* ─────────────────────────────────────────────────────────────────────────────
   Datum und Uhrzeit getrennt — die Felder des freien Zeitfensters (E‑1198)

   Seit dem 17.11.2026 steht je Zeitpunkt ein Datum und eine **freiwillige**
   Uhrzeit, an allen drei Stellen: freies Fenster der Nachrichtenliste,
   Zeitfenster der Belegsuche, freies Fenster der Prozessansicht
   (`docs/nachrichtenliste.md` §8.2). Wer nur ein Datum einträgt, meint den
   ganzen Tag.

   **Wo der ganze Tag endet, entscheidet nicht das Feld, sondern der Vertrag des
   Einhängepunkts** — sein `bis`:

   | Vertrag | wer | `bis` | ohne Uhrzeit |
   |---|---|---|---|
   | `sekunde` | Liste, Belegsuche | einschließend, `MessageLastUpdate <= bis`; die Spalte ist `timestamp` ohne Bruchteil | 23:59:59 |
   | `stunde` | Prozessansicht | die letzte enthaltene volle Stunde (E‑94, E‑95) | 23:00 |

   „Von" beginnt ohne Uhrzeit in beiden um 00:00. Gerechnet wird in der
   Anzeigezone und mit den zwei Durchgängen von {@link zeitpunktAusWanduhrzeit};
   damit treffen beide Umstellungstage, der Tag mit 23 und der mit 25 Stunden.
   ───────────────────────────────────────────────────────────────────────────── */

/** Wie ein Einhängepunkt sein `bis` liest — und damit, wo der ganze Tag endet. */
export type Fenstervertrag = "sekunde" | "stunde";

/** Welcher der beiden Zeitpunkte. */
export type Fenstergrenze = "von" | "bis";

/** Datum und Uhrzeit eines Zeitpunkts, so wie die beiden Felder sie führen. */
export type DatumUndUhrzeit = { datum: string; uhrzeit: string };

/**
 * Die Uhrzeit, die gilt, wenn keine eingetragen ist — in der Schreibweise, die
 * {@link datumUndUhrzeitFuerEingabe} liefert, damit sie Zeichen für Zeichen
 * vergleichbar ist.
 */
function uhrzeitOhneEingabe(grenze: Fenstergrenze, vertrag: Fenstervertrag): string {
  if (grenze === "von") {
    return "00:00";
  }
  return vertrag === "stunde" ? "23:00" : "23:59:59";
}

const EINGABE_DATUM = /^(\d{4})-(\d{2})-(\d{2})$/;
const EINGABE_UHRZEIT = /^([01]\d|2[0-3]):[0-5]\d(:[0-5]\d(\.\d{1,3})?)?$/;

/**
 * **Datum und freiwillige Uhrzeit → der Zeitpunkt, der in die Adresse geht.**
 *
 * Ohne Uhrzeit gilt für „Von" 00:00 und für „Bis" das Ende des Tages im
 * {@link Fenstervertrag}. Eine eingetragene Uhrzeit gilt, wie sie dasteht —
 * Minuten, und wenn die Adresse sie trug, Sekunden und Millisekunden.
 *
 * @returns `null` ohne Datum, bei einem Datum, das kein Kalendertag ist (JavaScript
 *   schöbe den 30.02. still auf den 02.03.), und bei einer unlesbaren Uhrzeit
 */
export function zeitpunktAusDatumUndUhrzeit(
  datum: string,
  uhrzeit: string,
  grenze: Fenstergrenze,
  vertrag: Fenstervertrag,
  zone: string,
): Date | null {
  const tag = EINGABE_DATUM.exec(datum.trim());
  if (tag === null) {
    return null;
  }
  const [, jahr, monat, kalendertag] = tag.map(Number);
  const probe = new Date(Date.UTC(jahr ?? 0, (monat ?? 0) - 1, kalendertag ?? 0));
  if (probe.getUTCMonth() !== (monat ?? 0) - 1 || probe.getUTCDate() !== kalendertag) {
    return null;
  }
  const zeit = uhrzeit.trim() === "" ? uhrzeitOhneEingabe(grenze, vertrag) : uhrzeit.trim();
  if (!EINGABE_UHRZEIT.test(zeit)) {
    return null;
  }
  return zeitpunktAusWanduhrzeit(`${datum.trim()}T${zeit}`, zone);
}

/**
 * **Die Gegenrichtung: ein Zeitpunkt der Adresse → Datum und Uhrzeit der Felder.**
 *
 * **Die Uhrzeit bleibt leer genau dann, wenn sie der Vorgabe für „ohne Uhrzeit"
 * entspricht** — sonst steht sie da, und zwar so genau, wie der Zeitpunkt es
 * verlangt: `14:30`, `14:30:15`, `14:30:15.250`. Damit kommt eine Adresse über
 * die Felder Zeichen für Zeichen zurück.
 *
 * **Die eine Lücke** ist die Stunde, die es am Tag der Rückstellung zweimal gibt:
 * Die Felder kennen keine Zone, „02:30" steht für beide, und
 * {@link zeitpunktAusWanduhrzeit} liest die spätere. Ein Zeitpunkt aus der
 * früheren kommt eine Stunde später zurück (`tests/format.test.ts`).
 */
export function datumUndUhrzeitFuerEingabe(
  zeitpunkt: Date | null,
  grenze: Fenstergrenze,
  vertrag: Fenstervertrag,
  zone: string,
): DatumUndUhrzeit {
  if (zeitpunkt === null || Number.isNaN(zeitpunkt.getTime())) {
    return { datum: "", uhrzeit: "" };
  }
  const teile = new Intl.DateTimeFormat("en-US", { ...TEILE, timeZone: zone }).formatToParts(
    zeitpunkt,
  );
  const feld = (art: Intl.DateTimeFormatPartTypes) =>
    teile.find((teil) => teil.type === art)?.value ?? "00";
  // Kein Versatz einer Zone hat Bruchteile einer Sekunde — die Millisekunden
  // sind überall dieselben.
  const millisekunden = zeitpunkt.getUTCMilliseconds();
  const minuten = `${feld("hour")}:${feld("minute")}`;
  const uhrzeit =
    millisekunden !== 0
      ? `${minuten}:${feld("second")}.${String(millisekunden).padStart(3, "0")}`
      : feld("second") !== "00"
        ? `${minuten}:${feld("second")}`
        : minuten;
  return {
    datum: `${feld("year")}-${feld("month")}-${feld("day")}`,
    uhrzeit: uhrzeit === uhrzeitOhneEingabe(grenze, vertrag) ? "" : uhrzeit,
  };
}
