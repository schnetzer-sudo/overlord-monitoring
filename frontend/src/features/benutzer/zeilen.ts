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
 * Ob sich **diese** Zeile öffnen lässt, solange eine andere offen ist.
 *
 * Dieselbe Regel und dieselbe Begründung wie bei der Katalogpflege
 * (`features/katalog/zuordnung.ts` `darfOeffnen`): **Ungespeicherte Eingaben
 * werden nie stillschweigend verworfen.** Ein aufgeklapptes Formular hält hier
 * zwei Entwürfe, die nirgendwo sonst stehen — die gesetzten Mandanten-Häkchen
 * und, schwerer wiegend, ein bereits **getipptes Einmalpasswort**. Klappte
 * daneben eine zweite Zeile auf, wäre beides weg, ohne dass jemand danach
 * gefragt hätte; und das Passwort steht danach an keiner Stelle mehr, auch
 * nicht im Protokoll.
 *
 * Die offene Zeile selbst darf immer — das ist ihr Weg wieder zu.
 */
export function darfOeffnen(offeneZeile: number | null, id: number): boolean {
  return offeneZeile === null || offeneZeile === id;
}
