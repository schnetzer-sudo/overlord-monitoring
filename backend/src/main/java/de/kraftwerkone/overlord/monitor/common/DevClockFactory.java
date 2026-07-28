package de.kraftwerkone.overlord.monitor.common;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Reine Rechenlogik fuer die Dev-Uhr: baut aus dem Maximum von {@code Message.MessageLastUpdate}
 * und der Systemuhr einen {@link Clock}, der um die Differenz zurueckversetzt ist. Die Zeit
 * <b>laeuft weiter</b> (kein Einfrieren), sonst verhalten sich relative Zeitfenster und
 * Timeout-Berechnungen anders als in Produktion.
 *
 * <p>Getrennt von {@code ZeitConfig}, damit die Berechnung ohne Datenbank testbar ist.
 */
public final class DevClockFactory {

  private DevClockFactory() {}

  /**
   * @param dbMaximum das Maximum aus {@code Message.MessageLastUpdate} (Wanduhrzeit des Servers,
   *     ohne Zeitzonenkonvertierung)
   * @param systemClock die reale Systemuhr (deren Zeitzone als Serverzeitzone interpretiert wird)
   * @return ein Clock, dessen aktuelle Zeit dem {@code dbMaximum} entspricht und weiterlaeuft
   */
  public static Clock ausMaximum(LocalDateTime dbMaximum, Clock systemClock) {
    Instant maximum = dbMaximum.atZone(systemClock.getZone()).toInstant();
    Duration versatz = Duration.between(systemClock.instant(), maximum);
    return Clock.offset(systemClock, versatz);
  }
}
