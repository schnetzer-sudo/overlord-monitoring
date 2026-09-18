package de.kraftwerkone.overlord.monitor.common;

import java.time.LocalDateTime;

/**
 * Eine Zeile der Live-Lesung der Fehler — dieselbe Gestalt wie eine Zeile von {@code
 * message_rollup}: je Stundeneimer, Prozess und <b>Rohstatus</b> eine Anzahl ({@code
 * docs/fehler-live.md} §4).
 *
 * <p>Rohstatus und nicht Einordnung (Entscheidung E-g in {@code docs/rollup.md} §2): Welche Zeile
 * {@code FEHLER} ist, hat die Lesung in SQL ueber {@code MessageStatusClassifier.fehlerBedingung}
 * entschieden; die Fehlerart bildet der Verbraucher beim Lesen, ueber denselben Klassifizierer.
 *
 * <p>Anders als eine {@link LiveRestZeile} in einer Korrektur ist {@code anzahl} hier <b>nie
 * negativ</b> — die Lesung zaehlt, sie verrechnet nichts.
 *
 * @param stunde der Stundeneimer, Wanduhrzeit des Quellservers — gebildet wie im Rollup-Job ({@link
 *     Stundeneimer})
 * @param processId die {@code ProcessID}
 * @param messageStatus der Rohwert, etwa {@code ERROR_TIMEOUT} oder {@code COMMIT_REJECTED}
 * @param anzahl wie viele Nachrichten dieses Prozesses mit diesem Rohwert im Eimer stehen
 */
public record FehlerLiveZeile(
    LocalDateTime stunde, String processId, String messageStatus, long anzahl) {

  public FehlerLiveZeile {
    if (stunde == null || processId == null) {
      throw new IllegalArgumentException("Eine Fehlerzeile braucht Stunde und Prozess");
    }
    if (anzahl <= 0) {
      throw new IllegalArgumentException("Eine Fehlerzeile zaehlt mindestens eine Nachricht");
    }
  }
}
