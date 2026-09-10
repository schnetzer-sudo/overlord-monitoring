package de.kraftwerkone.overlord.monitor.dashboard;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Der Abstand eines Zeitpunkts zur <b>Anwendungsuhr</b>, in ganzen Sekunden — <b>an einer
 * Stelle</b>.
 *
 * <p>Die Regel stammt aus Entscheidung E‑75 (das Alter der aeltesten offenen Nachricht) und gilt
 * seit Schritt 10d fuer drei weitere Felder: das Alter jeder Dienstlampe und das des
 * Pruefzeitpunkts der Ablagenkachel. <b>Dieselbe Rechnung an vier Stellen waere vier Gelegenheiten,
 * sie unterschiedlich zu machen</b> — insbesondere bei der Frage, was ein negativer Abstand
 * bedeutet.
 *
 * <p><b>Ein Repository liest keine Uhr</b>: Der Zeitpunkt kommt roh aus der Datenbank, {@code
 * jetzt} kommt einmal je Anfrage aus der Anwendungsuhr, und gerechnet wird hier.
 */
final class Alter {

  private Alter() {}

  /**
   * @param zeitpunkt der rohe Zeitpunkt, <b>Wanduhrzeit des Quellservers</b> und nicht konvertiert
   * @param jetzt derselbe Uhrenschlag, den auch das Fenster der Anfrage benutzt
   * @return die Sekunden dazwischen; {@code null} bei fehlendem Zeitpunkt <b>und bei negativem
   *     Abstand</b>
   */
  static Long sekunden(LocalDateTime zeitpunkt, LocalDateTime jetzt) {
    if (zeitpunkt == null) {
      return null;
    }
    long sekunden = Duration.between(zeitpunkt, jetzt).toSeconds();
    return sekunden < 0 ? null : sekunden;
  }
}
