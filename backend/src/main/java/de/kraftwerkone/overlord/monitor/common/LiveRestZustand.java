package de.kraftwerkone.overlord.monitor.common;

/**
 * Die drei Zustaende des <b>Live-Rests</b> — der Verkehr seit dem letzten Rollup-Lauf, den eine
 * Ansicht ihren Rollup-Zahlen dazurechnet, damit Baum und Uebertragungsliste fuer dasselbe Fenster
 * dieselbe Zahl zeigen ({@code docs/live-rest.md}).
 *
 * <p>Entschieden wird in {@link LiveRest#entscheide}, ohne Datenbank; die Antwort eines Endpunkts
 * nennt den Zustand, damit die Oberflaeche bei {@link #AUSGESETZT} sagen kann, dass die Zahlen
 * unvollstaendig sind — und bei den anderen beiden nichts sagt.
 */
public enum LiveRestZustand {

  /**
   * Der Live-Bereich wird gerechnet: minus die Rollupzeilen darin, plus die Zaehlung aus der
   * Quelle. Der Normalfall im Betrieb.
   */
  ANGEWANDT,

  /**
   * Der Rollup reicht ueber die Uhr hinaus — der Eimer des letzten Laufs liegt <b>nach</b> dem
   * Anfang der aktuellen Stunde. Nichts zu korrigieren. Im Profil {@code dev} nach einem Lauf gegen
   * die Systemuhr der uebliche Fall.
   */
  NICHT_NOETIG,

  /**
   * Nicht gerechnet: Entweder gibt es keinen abgeschlossenen, fehlerfreien Lauf, oder der letzte
   * liegt laenger zurueck als {@link LiveRest#OBERGRENZE_STUNDEN} Stunden. Die Zahlen sind dann
   * unvollstaendig, und die Oberflaeche sagt es.
   */
  AUSGESETZT
}
