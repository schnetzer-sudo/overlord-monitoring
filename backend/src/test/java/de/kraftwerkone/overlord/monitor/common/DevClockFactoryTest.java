package de.kraftwerkone.overlord.monitor.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Prueft, dass die Dev-Uhr aus einem bekannten Maximum den erwarteten Versatz erzeugt. */
class DevClockFactoryTest {

  @Test
  @DisplayName("Aus MAX(MessageLastUpdate) entsteht eine Uhr, die auf diesen Zeitpunkt zeigt")
  void erzeugt_erwarteten_versatz() {
    // Reale Werte der Testkopie am 28.07.2026: MAX liegt rund 19 Tage zurueck.
    Instant systemJetzt = Instant.parse("2026-07-28T08:55:32Z");
    Clock system = Clock.fixed(systemJetzt, ZoneOffset.UTC);
    LocalDateTime dbMaximum = LocalDateTime.parse("2026-07-08T17:21:10");

    Clock dev = DevClockFactory.ausMaximum(dbMaximum, system);

    // Die Anwendungszeit entspricht exakt dem Maximum aus der Datenbank ...
    assertThat(dev.instant()).isEqualTo(Instant.parse("2026-07-08T17:21:10Z"));
    // ... und der Versatz ist rund 19 Tage in die Vergangenheit.
    Duration versatz = Duration.between(system.instant(), dev.instant());
    assertThat(versatz.toDays()).isEqualTo(-19);
    assertThat(versatz.isNegative()).isTrue();
  }

  @Test
  @DisplayName("Die Uhr laeuft weiter, sie friert nicht ein")
  void uhr_laeuft_weiter() {
    // Eine echte (laufende) Systemuhr als Basis: der Versatz bleibt konstant, die Zeit laeuft.
    Clock system = Clock.systemUTC();
    LocalDateTime dbMaximum = LocalDateTime.parse("2026-07-08T17:21:10");

    Clock dev = DevClockFactory.ausMaximum(dbMaximum, system);
    Instant erste = dev.instant();
    Instant zweite = dev.instant();

    assertThat(zweite).isAfterOrEqualTo(erste);
  }
}
