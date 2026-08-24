import { parseAsBoolean } from "nuqs";

import type { Katalogzeile } from "./api";

/**
 * Der Filterzustand der Pflegeliste — **in der URL, nicht im Komponentenzustand.**
 *
 * Zwei Filter, und sie werden an verschiedenen Orten gerechnet:
 *
 * | Filter | Wo gerechnet | Warum dort |
 * |---|---|---|
 * | `nurOffene` | **Backend** | Der Endpunkt kennt den Parameter; er ist der Arbeitsmodus (E7) |
 * | `nurMitNachrichten` | **Browser** | Die volle Liste liegt ohnehin vor (E8, E20) |
 *
 * **Der zweite ist bewusst kein Serverparameter.** Er wäre eine zweite Abfrage
 * auf dieselbe Menge — und könnte eine andere Antwort geben als die Liste, die
 * gerade auf dem Bildschirm steht. Das Backend führt ihn deshalb ausdrücklich
 * nicht (`docs/prozess-katalog-backend.md` §10,
 * Punkt 9).
 *
 * **Dieses Modul ist frei von React.** Die Umrechnung Zustand → Anfrage und die
 * Auswahl der sichtbaren Zeilen sind reine Funktionen und werden als solche
 * geprüft (`tests/katalogfilter.test.ts`). Der Hook, der den Zustand an die URL
 * bindet, steht in `hooks.ts`.
 *
 * **Es liegt hier und nicht in `lib/filter.ts`.** Dort steht allein die
 * Zeitfenster-Abstraktion — der eine Filter, den jeder Listen-Endpunkt hat. Diese
 * Liste hat keins; `lib` ist Infrastruktur, nie Fachlichkeit
 * (`docs/frontend-grundlagen.md` §8).
 */

/**
 * Beide Vorgaben zeigen **alles** und lassen nichts weg.
 *
 * Daraus folgt unmittelbar, dass hier **kein `clearOnDefault: false`** steht:
 * Die Regel lautet *„ein Standardwert, der etwas weglässt, gehört in die URL;
 * einer, der etwas setzt, nicht"* (`docs/frontend-grundlagen.md` §8). Diese
 * beiden lassen im Vorgabezustand nichts weg — es gibt nichts zu teilen, und
 * eine URL ohne Parameter zeigt die vollständige Liste.
 *
 * Ein Wahrheitswert braucht trotzdem ein `withDefault`, sonst wäre er `null`
 * statt `false`. Das ist dieselbe Ausnahme wie bei `langeSuche` in der
 * Nachrichtenliste.
 */
export const NUR_OFFENE_VORGABE = false;
export const NUR_MIT_NACHRICHTEN_VORGABE = false;

export const KATALOG_PARAMETER = {
  nurOffene: parseAsBoolean.withDefault(NUR_OFFENE_VORGABE),
  nurMitNachrichten: parseAsBoolean.withDefault(NUR_MIT_NACHRICHTEN_VORGABE),
};

export type Katalogfilter = {
  nurOffene: boolean;
  nurMitNachrichten: boolean;
};

export const LEERER_FILTER: Katalogfilter = {
  nurOffene: NUR_OFFENE_VORGABE,
  nurMitNachrichten: NUR_MIT_NACHRICHTEN_VORGABE,
};

/**
 * Der Filter als Suchparameter — die **URL**, wie `nuqs` sie schreibt.
 *
 * Ein Wert, der der Vorgabe gleicht, steht nicht darin; genau das tut `nuqs`
 * mit `clearOnDefault` von selbst. Die Funktion bildet es nach, damit der
 * Rundlauf URL → Zustand → URL prüfbar ist, ohne React zu rendern.
 */
export function alsSuchparameter(filter: Katalogfilter): URLSearchParams {
  const parameter = new URLSearchParams();
  if (filter.nurOffene !== NUR_OFFENE_VORGABE) {
    parameter.set("nurOffene", String(filter.nurOffene));
  }
  if (filter.nurMitNachrichten !== NUR_MIT_NACHRICHTEN_VORGABE) {
    parameter.set("nurMitNachrichten", String(filter.nurMitNachrichten));
  }
  return parameter;
}

/**
 * Liest den Filter aus einer URL — die Gegenrichtung zu dem, was `nuqs` beim
 * Rendern tut, als reine Funktion für den Test.
 *
 * **Ein unbrauchbarer Wert wird übergangen**, nicht abgewiesen: `nurOffene=ja`
 * ist kein Wahrheitswert, und ein alter oder von Hand gebauter Link soll die
 * Liste nicht sprengen. Er verhält sich dann wie ein fehlender Parameter.
 */
export function ausSuchparametern(parameter: URLSearchParams): Katalogfilter {
  return {
    nurOffene: wahrheitswert(parameter.get("nurOffene"), NUR_OFFENE_VORGABE),
    nurMitNachrichten: wahrheitswert(
      parameter.get("nurMitNachrichten"),
      NUR_MIT_NACHRICHTEN_VORGABE,
    ),
  };
}

function wahrheitswert(roh: string | null, vorgabe: boolean): boolean {
  if (roh === "true") {
    return true;
  }
  if (roh === "false") {
    return false;
  }
  return vorgabe;
}

/**
 * Die Kennung, an der ein Auffangprozess erkannt wird (E16).
 *
 * **Niemals `^0+_`.** M78 hat den zuerst beauftragten Ausdruck gefahren und
 * **sechs** Treffer bekommen, von denen **vier keine Auffangprozesse sind** —
 * regulär benannte Prozesse mit einem Nummernpräfix aus Nullen, darunter einer
 * mit **1.602** Nachrichten. Ein Filter auf das Präfix erklärte den größten
 * davon zum Auffangbecken.
 *
 * Echte Auffangprozesse sind im Bestand zwei: `00001_Undefined` (`VOTG`, drei
 * Nachrichten) und `Undefined` (`SYSTEM`, 151 — ein technischer Mandant).
 * **Belegt ist das über die `ProcessID`** und nicht über den `ProcessName`; M78
 * nennt für diese Prozesse keinen Anzeigenamen. Deshalb prüft diese Funktion die
 * Kennung und nichts sonst.
 *
 * **Groß- und Kleinschreibung zählen.** Gemessen ist die Schreibweise
 * `Undefined`; eine unscharfe Prüfung wäre eine Annahme über Werte, die niemand
 * erhoben hat (Regel Q4).
 */
export const AUFFANGKENNUNG = "Undefined";

export function istAuffangprozess(processId: string): boolean {
  return processId.includes(AUFFANGKENNUNG);
}

/**
 * Die Zeilen, die der Filter „nur mit Nachrichten" übrig lässt (E20).
 *
 * **Zeilen mit `null` bleiben sichtbar**, auch bei aktivem Filter. Das ist die
 * Stelle, an der die drei Zustände aus E14 tragen: `null` heißt „noch nie
 * geprüft" und nicht „ohne Nachrichten". Eine Zeile, für die nie ein
 * Bestandslauf lief, verschwände sonst aus **beiden** Filterstellungen — und vor
 * dem ersten Lauf wäre die Liste vollständig leer, obwohl es Prozesse gibt.
 *
 * `nurOffene` kommt hier nicht vor: Den rechnet das Backend, und die Zeilen sind
 * bereits ausgesiebt, wenn sie hier ankommen.
 */
export function sichtbareZeilen(
  zeilen: readonly Katalogzeile[],
  filter: Katalogfilter,
): Katalogzeile[] {
  if (!filter.nurMitNachrichten) {
    return [...zeilen];
  }
  return zeilen.filter((zeile) => zeile.traegtNachrichten !== false);
}
