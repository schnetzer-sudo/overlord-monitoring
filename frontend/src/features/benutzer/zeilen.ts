import type { Nutzerzeile } from "./api";

/**
 * Die geänderte Zeile **an ihrer Stelle** in die Liste setzen — reine Funktion,
 * frei von React.
 *
 * **An ihrer Stelle und nicht angehängt.** Die Reihenfolge kommt vom Backend
 * (nach Benutzername), und keiner der fünf Vorgänge ändert den Namen. Eine Zeile
 * ans Ende zu hängen verschöbe sie also grundlos — mitten in einer Arbeit, bei
 * der jemand dieselbe Zeile mehrmals anfasst.
 *
 * **Verglichen wird über die `id`** und nicht über den Benutzernamen: Der ist in
 * der Datenbank zwar eindeutig, aber ohne Rücksicht auf Groß- und
 * Kleinschreibung — ein Vergleich in JavaScript ist es nicht. Die `id` ist der
 * Schlüssel, den auch das Backend benutzt.
 *
 * Fehlt die Zeile in der Liste, bleibt die Liste **unverändert**. Das ist der
 * ehrlichere der beiden Ausgänge: Sie einzufügen hieße zu raten, an welche
 * Stelle sie gehört, und der einzige Weg zu diesem Fall wäre eine Liste, die
 * ohnehin nicht mehr stimmt.
 */
export function mitAktualisierterZeile(
  liste: readonly Nutzerzeile[],
  geaendert: Nutzerzeile,
): Nutzerzeile[] {
  return liste.map((zeile) => (zeile.id === geaendert.id ? geaendert : zeile));
}

/**
 * Die Anlegemaske über der Liste — **kein Zeilenformular und trotzdem einer**
 * (E22, seit 9c).
 *
 * Sie ist kein `number`, weil sie zu keiner Zeile gehört; sie steht trotzdem im
 * selben Zustand wie eine offene Zeile, weil sie sich mit ihnen die Sperre
 * teilt. Ein zweiter Zustand daneben („Maske offen" *und* „Zeile offen") ließe
 * einen Augenblick zu, in dem beide offen sind — und genau den soll es nicht
 * geben.
 */
export const MASKE = "maske" as const;

/** Was gerade aufgeklappt ist: eine Zeile (ihre `id`), die Maske, oder nichts. */
export type Aufgeklappt = number | typeof MASKE | null;

/**
 * Ob sich **dieses** Formular öffnen lässt, solange ein anderes offen ist.
 *
 * Dieselbe Regel und dieselbe Begründung wie bei der Katalogpflege
 * (`features/katalog/zuordnung.ts` `darfOeffnen`): **Ungespeicherte Eingaben
 * werden nie stillschweigend verworfen.** Ein aufgeklapptes Formular hält hier
 * zwei Entwürfe, die nirgendwo sonst stehen — die gesetzten Mandanten-Häkchen
 * und, schwerer wiegend, ein bereits **getipptes Einmalpasswort**. Klappte
 * daneben ein zweites auf, wäre beides weg, ohne dass jemand danach gefragt
 * hätte; und das Passwort steht danach an keiner Stelle mehr, auch nicht im
 * Protokoll.
 *
 * Das offene Formular selbst darf immer — das ist sein Weg wieder zu.
 *
 * ## Seit 9c gilt die Regel in beide Richtungen, und sie ist dieselbe geblieben
 *
 * Die Anlegemaske hält denselben Einsatz: ein getipptes Einmalpasswort (E22).
 * **Erweitert worden ist deshalb, was „offen" sein kann — nicht, was die Regel
 * sagt.** Der Rumpf ist unverändert; er trägt den neuen Fall von selbst, weil
 * {@link MASKE} keiner `id` gleicht. Eine zweite Funktion „darf die Maske
 * öffnen" wäre dieselbe Bedingung ein zweites Mal, und die zweite liefe der
 * ersten irgendwann hinterher.
 *
 * @param was die Zeile, um die es geht — oder {@link MASKE}
 */
export function darfOeffnen(aufgeklappt: Aufgeklappt, was: number | typeof MASKE): boolean {
  return aufgeklappt === null || aufgeklappt === was;
}
