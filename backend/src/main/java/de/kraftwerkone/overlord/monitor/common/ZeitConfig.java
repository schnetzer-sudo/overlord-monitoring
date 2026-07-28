package de.kraftwerkone.overlord.monitor.common;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Die Zeitquellen des Projekts. Bewusst in {@code common}: die einzige Stelle, die die Systemuhr
 * liest, ist diese Konfiguration ({@code Clock.systemDefaultZone()} bzw. {@code Clock.systemUTC()}
 * sind erlaubt; {@code LocalDateTime.now()} und Verwandte werden nirgends aufgerufen, Regel Z1).
 *
 * <p>Es gibt zwei Uhren:
 *
 * <ul>
 *   <li><b>Anwendungsuhr</b> ({@link Primary}): relative Zeitfenster und Timeout-Berechnung. In
 *       Produktion die Systemuhr, im Dev-Profil um den Rueckstand der Testkopie zurueckversetzt.
 *   <li><b>{@code systemClock}</b>: sicherheitsrelevante Zeit (Sitzungsablauf, Sperrfristen ab
 *       Schritt 3) und Protokollzeit. Nutzt <b>niemals</b> den Dev-Versatz, immer die echte Uhr in
 *       UTC.
 * </ul>
 */
@Configuration
public class ZeitConfig {

  private static final Logger log = LoggerFactory.getLogger(ZeitConfig.class);

  /** Anwendungsuhr in Produktion und ausserhalb des Dev-Profils: die Systemuhr. */
  @Bean
  @Primary
  @Profile("!dev")
  public Clock clock() {
    return Clock.systemDefaultZone();
  }

  /**
   * Anwendungsuhr im Dev-Profil. Beim Start wird {@code MAX(Message.MessageLastUpdate)} gelesen und
   * die Uhr um den Rueckstand zurueckversetzt. Schlaegt die Abfrage fehl oder ist das Ergebnis
   * leer, wird auf die Systemuhr zurueckgefallen — jeweils mit einer gut sichtbaren {@code
   * WARN}-Zeile.
   */
  @Bean
  @Primary
  @Profile("dev")
  public Clock devClock(@Qualifier("glassfishDsl") DSLContext glassfishDsl) {
    Clock system = Clock.systemDefaultZone();
    try {
      // Unqualifiziert: die Verbindung des Lese-Pools zeigt bereits auf das GlassfishDB-Schema.
      LocalDateTime maximum =
          glassfishDsl
              .resultQuery("select max(MessageLastUpdate) from Message")
              .fetchOne(0, LocalDateTime.class);
      if (maximum == null) {
        log.warn(
            "Dev-Clock: MAX(Message.MessageLastUpdate) ist leer — es wird die Systemuhr verwendet.");
        return system;
      }
      Clock dev = DevClockFactory.ausMaximum(maximum, system);
      Duration versatz = Duration.between(system.instant(), dev.instant());
      log.warn(
          "Dev-Clock aktiv: Anwendungszeit auf {} zurueckversetzt (Versatz {} Tage / {} Stunden)."
              + " Sicherheitsrelevante Zeit nutzt weiterhin die Systemuhr.",
          LocalDateTime.ofInstant(dev.instant(), system.getZone()),
          versatz.toDays(),
          versatz.toHours());
      return dev;
    } catch (RuntimeException ex) {
      log.warn(
          "Dev-Clock: MAX(Message.MessageLastUpdate) konnte nicht gelesen werden ({}) —"
              + " es wird die Systemuhr verwendet.",
          ex.getMessage());
      return system;
    }
  }

  /** Sicherheits- und Protokollzeit: immer die echte Uhr, in UTC. Kein Dev-Versatz. */
  @Bean("systemClock")
  public Clock systemClock() {
    return Clock.systemUTC();
  }
}
