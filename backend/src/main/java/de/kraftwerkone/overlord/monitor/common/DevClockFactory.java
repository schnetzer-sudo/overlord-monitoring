package de.kraftwerkone.overlord.monitor.common;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Reine Rechenlogik fuer die Dev-Uhr: baut aus einem <b>Anker</b> in den Daten der Testkopie und
 * der Systemuhr einen {@link Clock}, der um die Differenz zurueckversetzt ist. Die Zeit <b>laeuft
 * weiter</b> (kein Einfrieren), sonst verhalten sich relative Zeitfenster und Timeout-Berechnungen
 * anders als in Produktion.
 *
 * <p>Getrennt von {@code ZeitConfig}, damit die Berechnung ohne Datenbank testbar ist.
 *
 * <p><b>Welcher Anker das ist, entscheidet {@code ZeitConfig} — nicht diese Klasse.</b> Bis zum
 * 01.08.2026 war es {@code MAX(Message.MessageLastUpdate)}; seither der juengste Zeitpunkt an einem
 * Tag mit mehreren Mandanten. Diese Klasse rechnet mit jedem Zeitpunkt gleich.
 */
public final class DevClockFactory {

  private DevClockFactory() {}

  /**
   * @param anker ein Zeitpunkt aus der Testkopie (Wanduhrzeit des Servers, ohne
   *     Zeitzonenkonvertierung)
   * @param systemClock die reale Systemuhr (deren Zeitzone als Serverzeitzone interpretiert wird)
   * @return ein Clock, dessen aktuelle Zeit dem {@code anker} entspricht und weiterlaeuft
   */
  public static Clock ausAnker(LocalDateTime anker, Clock systemClock) {
    Instant zeitpunkt = anker.atZone(systemClock.getZone()).toInstant();
    Duration versatz = Duration.between(systemClock.instant(), zeitpunkt);
    return Clock.offset(systemClock, versatz);
  }
}
