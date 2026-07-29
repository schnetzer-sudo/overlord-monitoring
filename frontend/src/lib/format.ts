import type { Sprache } from "@/i18n";

/**
 * Datum und Zahlen über `Intl`, mit der aktiven Sprache.
 *
 * **Der wichtigste Teil dieser Datei ist, was sie nicht tut.**
 *
 * Die Zeitstempel stammen aus `GlassfishDB` und sind Wanduhrzeit des
 * Altsystem-Servers ohne Zeitzone. Sie werden angezeigt **wie geliefert**: keine
 * Umrechnung, keine Angabe eines `timeZone`. Würde man den Wert an `new Date()`
 * geben und mit der Zeitzone des Browsers formatieren, verschöbe sich die
 * Anzeige um Stunden — und niemand merkte es, weil das Ergebnis plausibel
 * aussieht. Ein Beleg, der um 14:23 verarbeitet wurde, stünde dann um 16:23 in
 * der Liste, und die Suche nach dem Zeitpunkt aus dem Altwerkzeug ginge ins
 * Leere.
 *
 * Deshalb wird der übertragene Wert **feldweise** gelesen und daraus ein Datum
 * in der Zeitzone des Browsers gebaut. Formatiert man das ohne `timeZone`,
 * kommen exakt die gelieferten Felder wieder heraus — unabhängig davon, wo der
 * Browser steht.
 */

const ZEITSTEMPEL =
  /^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2})(?::(\d{2}))?(?:\.\d+)?(?:Z|[+-]\d{2}:?\d{2})?$/;

/**
 * Baut aus den gelieferten Feldern ein Datum, dessen **örtliche** Felder mit den
 * gelieferten übereinstimmen. Nur so bleibt die Anzeige verschiebungsfrei.
 */
function alsOertlichesDatum(wert: string): Date | null {
  const treffer = ZEITSTEMPEL.exec(wert.trim());
  if (treffer === null) {
    return null;
  }
  const [, jahr, monat, tag, stunde, minute, sekunde] = treffer;
  return new Date(
    Number(jahr),
    Number(monat) - 1,
    Number(tag),
    Number(stunde),
    Number(minute),
    Number(sekunde ?? "0"),
  );
}

const ZEITPUNKT: Intl.DateTimeFormatOptions = {
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
  hour: "2-digit",
  minute: "2-digit",
  // Ausdrücklich kein `timeZone`. Siehe oben.
};

const DATUM: Intl.DateTimeFormatOptions = {
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
};

/**
 * Zeitpunkt in der aktiven Sprache. Ein unlesbarer Wert wird unverändert
 * durchgereicht — lieber roh als falsch.
 */
export function formatiereZeitpunkt(wert: string | null | undefined, sprache: Sprache): string {
  if (!wert) {
    return "";
  }
  const datum = alsOertlichesDatum(wert);
  return datum === null ? wert : new Intl.DateTimeFormat(sprache, ZEITPUNKT).format(datum);
}

export function formatiereDatum(wert: string | null | undefined, sprache: Sprache): string {
  if (!wert) {
    return "";
  }
  const datum = alsOertlichesDatum(wert);
  return datum === null ? wert : new Intl.DateTimeFormat(sprache, DATUM).format(datum);
}

export function formatiereZahl(wert: number, sprache: Sprache): string {
  return new Intl.NumberFormat(sprache).format(wert);
}
