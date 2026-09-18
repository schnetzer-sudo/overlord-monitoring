package de.kraftwerkone.overlord.monitor.common;

/**
 * Die zwei Zustaende von <b>Fehler live</b> — ob die Einordnung {@code FEHLER} einer Ansicht aus
 * der Live-Lesung ueber {@code Message} kommt oder, weil die Lesung ausgefallen ist, weiter aus dem
 * Rollup ({@code docs/fehler-live.md}).
 *
 * <p>Die Antwort eines Endpunkts nennt den Zustand, damit die Oberflaeche bei {@link #AUSGESETZT}
 * sagen kann, dass nachverarbeitete Nachrichten noch als Fehler zaehlen koennen — und bei {@link
 * #ANGEWANDT} nichts sagt.
 */
public enum FehlerLiveZustand {

  /**
   * Die Fehler kommen aus der Live-Lesung: Die Rollupzeilen mit der Einordnung {@code FEHLER} sind
   * ersetzt. Der Normalfall.
   */
  ANGEWANDT,

  /**
   * Die Live-Lesung ist ausgefallen (E-185 sinngemaess, {@code docs/fehler-live.md} §4). Die Fehler
   * kommen wie vor diesem Schritt aus Rollup und Live-Rest, und eine Nachricht, die nach einem
   * Fehler nachverarbeitet wurde, kann darin bis zum naechsten Volllauf noch als Fehler zaehlen.
   */
  AUSGESETZT
}
