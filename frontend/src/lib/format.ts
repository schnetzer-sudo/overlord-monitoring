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

export function formatiereZahl(wert: number, sprache: Sprache): string {
  return new Intl.NumberFormat(sprache).format(wert);
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
  return alsUtc - zeitpunkt.getTime();
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
  if (!/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2})?$/.test(roh)) {
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
