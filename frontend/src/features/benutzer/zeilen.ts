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
