package de.kraftwerkone.overlord.monitor.message;

/**
 * Woher der Name eines Prozessschritts stammt.
 *
 * <p><b>Die Herkunft steht in der Antwort und ist keine Warnung an den Nutzer</b>, sondern
 * Nachweis: Ein spaeterer Zweifel an einem Schrittnamen kostet damit eine Abfrage statt einer
 * Suche. Dasselbe Muster wie die Einheit von {@code MessageTimeout}, die an genau einer Stelle
 * benannt steht.
 *
 * <p>Die drei Stufen werden in genau dieser Reihenfolge versucht; die erste, die traegt, gewinnt.
 * Begruendung und Zahlen in {@code docs/nachrichtendetail.md}.
 */
public enum Namensherkunft {

  /**
   * Es gibt eine {@code SOSAction}-Zeile zu {@code (ma.SOSID, ma.SOSActionID)}; ihr {@code
   * SOSActionName} wird verwendet.
   *
   * <p>Der Regelweg. Er traegt 71,46 Prozent der echten Schritte im dichten Tag und 77,99 Prozent
   * im dichten Monat (M18). Dass der Join semantisch richtig ist und nicht nur zufaellig eine Zeile
   * trifft, belegt M15 ueber den Vergleich des <i>ausgefuehrten</i> mit dem <i>geplanten</i>
   * Baustein: null Abweichungen auf 10.078 aufgeloeste Zeilen im Tag, sieben auf 366.343 im Monat.
   */
  DIREKT,

  /**
   * Es gibt keine solche Zeile — der Name kommt ueber die <b>erste Marke</b> aus {@code
   * ma.SOSActionServiceProperties}, aufgeloest im <b>selben Ablauf</b> und <b>nur bei genau einem
   * Treffer</b>.
   *
   * <p>Der Anlass ist gemessen (M20): Die Ablaufdefinition nummeriert ihre Schritte nicht
   * lueckenlos, die Ausfuehrung dagegen fortlaufend. Der eine Ablauf, der 3.985 der 4.025
   * namenlosen Schritte des Tages stellt, definiert die Kennungen 1, 98 und 99 — der ausgefuehrte
   * Schritt traegt die Position 2 und dieselbe Marke wie der geplante Schritt 98. Es fehlt nicht
   * der Schritt, es fehlt die Uebersetzung seiner Nummer.
   *
   * <p><b>Die Eindeutigkeitsbedingung ist nicht verhandelbar.</b> M19 hat gemessen, dass 96 bis 99
   * Prozent der namenlosen Schritte im selben Ablauf genau einen Schritt mit derselben Marke finden
   * und <b>nie mehrere</b>. Die Bedingung schneidet also nichts weg, was heute traegt — und sie
   * haelt die Tuer zu, sobald ein Ablauf mehrdeutig wird. {@code FTPSender} loest anderswo auf 25
   * verschiedene {@code SOSActionName} auf (M19); genau dort darf nicht geraten werden.
   */
  HERGELEITET,

  /**
   * Keine der beiden Stufen traegt — geliefert wird {@code ma.SOSActionServiceProperties}
   * unveraendert.
   *
   * <p><b>Das ist ein Regelweg und kein Randfall</b> (M15): Die nicht aufgeloesten Schritte sind
   * fast alle Zeilen, deren {@code SOSAction} es im heutigen Stand nicht mehr gibt, und dieser
   * Anteil waechst fuer alte Nachrichten mit der Zeit. Kein einziger namenloser Schritt steht dabei
   * leer da — alle 4.025 (Fenster A) beziehungsweise 103.402 (Fenster B) tragen ihre Bausteine.
   */
  ROHWERT
}
