package de.kraftwerkone.overlord.monitor.message;

/**
 * Wodurch ein Glied mit der angefragten Nachricht zusammenhaengt — <b>die eine Angabe, die die
 * Oberflaeche dem Nutzer sagen muss</b>.
 *
 * <p>Sie steht ausdruecklich in jeder Antwortzeile, obwohl sie sich aus den Rollen und der Ebene
 * ableiten liesse. Der Unterschied zwischen „aufgeteilt" und „zusammengefuehrt" ist das, was der
 * Nutzer verstehen soll; er soll nicht in der Oberflaeche aus zwei Feldern zusammengerechnet
 * werden, wo er bei der naechsten Aenderung auseinanderfaellt.
 *
 * <p>Die beiden Beziehungen liegen auf <b>verschiedenen Spalten</b> und sind nicht zwei Sichten auf
 * dieselbe (M25‑2): {@code SourceMessageID} traegt die Aufteilung, {@code TargetMessageID} die
 * Zusammenfuehrung. Wer nur eine der beiden liest, verliert die Haelfte der Faelle.
 */
public enum Kettenbeziehung {

  /**
   * Aufteilung — die Wurzel ist in Teile zerlegt worden. Getragen von {@code SourceMessageID} auf
   * dem Kind und {@code Source} auf der Wurzel. Bis zu <b>3.350</b> Kinder an einer Wurzel, 87
   * Prozent haben genau eines (M24‑2, M30‑2).
   */
  AUFTEILUNG,

  /**
   * Zusammenfuehrung — mehrere Eingaenge sind zu einer Nachricht geworden. Getragen von {@code
   * TargetMessageID} auf dem Eingang und {@code Target} auf dem Ergebnis. Der Grad liegt bei <b>23
   * : 1</b>, im Hoechstfall bei <b>897 : 1</b> (M25‑1, M30‑2).
   */
  ZUSAMMENFUEHRUNG
}
