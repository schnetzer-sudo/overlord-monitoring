"use client";

import { parseAsIsoDateTime, useQueryStates } from "nuqs";

/**
 * Filterzustand liegt in der **URL**, nicht in `useState`.
 *
 * Der Grund ist Teilbarkeit: Wer eine Störung meldet, schickt einen Link, und
 * der Empfänger sieht denselben Ausschnitt — mit einer Ausnahme, die im Entwurf
 * bewusst in Kauf genommen wurde: Der aktive Mandant steht in der Sitzung, nicht
 * in der URL. Deshalb steht er sichtbar in der Kopfzeile.
 *
 * **Hier noch ohne Wirkung.** In Schritt 3 gibt es keine Liste, die gefiltert
 * werden könnte. Die Abstraktion entsteht trotzdem jetzt, damit Schritt 4 nicht
 * anfängt, Zeitfenster in Komponentenzustand zu legen und später umzubauen.
 *
 * Das Zeitfenster ist der einzige Filter, den **jeder** Listen-Endpunkt hat: Es
 * ist Pflicht (Regel L1), Standard 24 Stunden, Maximum ein Jahr. Durchgesetzt
 * wird beides im Backend — hier wird es nur transportiert.
 */

export const ZEITFENSTER_STANDARD_STUNDEN = 24;
export const ZEITFENSTER_MAXIMUM_TAGE = 365;

/**
 * Kein `withDefault`: Fehlt das Zeitfenster in der URL, setzt das **Backend**
 * den Standard. Ein zweiter Standardwert im Frontend liefe dem ersten
 * irgendwann hinterher.
 */
export const ZEITFENSTER_PARAMETER = {
  von: parseAsIsoDateTime,
  bis: parseAsIsoDateTime,
};

export function useZeitfenster() {
  return useQueryStates(ZEITFENSTER_PARAMETER);
}
