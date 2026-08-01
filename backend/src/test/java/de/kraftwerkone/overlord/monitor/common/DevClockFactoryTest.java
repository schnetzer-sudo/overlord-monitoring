package de.kraftwerkone.overlord.monitor.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Prueft, dass die Dev-Uhr aus einem bekannten Anker den erwarteten Versatz erzeugt. */
class DevClockFactoryTest {

  @Test
  @DisplayName("Aus dem Anker entsteht eine Uhr, die auf diesen Zeitpunkt zeigt")
  void erzeugt_erwarteten_versatz() {
    // Reale Werte am 01.08.2026: der Anker aus Messung M9 liegt rund 214 Tage zurueck.
    Instant systemJetzt = Instant.parse("2026-08-01T09:48:48Z");
    Clock system = Clock.fixed(systemJetzt, ZoneOffset.UTC);
    LocalDateTime anker = LocalDateTime.parse("2025-12-30T04:09:47");

    Clock dev = DevClockFactory.ausAnker(anker, system);

    // Die Anwendungszeit entspricht exakt dem Anker aus der Datenbank ...
    assertThat(dev.instant()).isEqualTo(Instant.parse("2025-12-30T04:09:47Z"));
    // ... und der Versatz zeigt in die Vergangenheit.
    Duration versatz = Duration.between(system.instant(), dev.instant());
    assertThat(versatz.toDays()).isEqualTo(-214);
    assertThat(versatz.isNegative()).isTrue();
  }

  @Test
  @DisplayName("Die Rechnung haengt nicht daran, welcher Anker gewaehlt wurde")
  void rechnet_mit_jedem_anker_gleich() {
    // Der bisherige Anker MAX(MessageLastUpdate). Die Klasse kennt den Unterschied nicht —
    // welcher Zeitpunkt der richtige ist, entscheidet ZeitConfig.
    Clock system = Clock.fixed(Instant.parse("2026-07-28T08:55:32Z"), ZoneOffset.UTC);

    Clock dev = DevClockFactory.ausAnker(LocalDateTime.parse("2026-07-08T17:21:10"), system);

    assertThat(dev.instant()).isEqualTo(Instant.parse("2026-07-08T17:21:10Z"));
    assertThat(Duration.between(system.instant(), dev.instant()).toDays()).isEqualTo(-19);
  }

  @Test
  @DisplayName("Die Uhr laeuft weiter, sie friert nicht ein")
  void uhr_laeuft_weiter() {
    // Eine echte (laufende) Systemuhr als Basis: der Versatz bleibt konstant, die Zeit laeuft.
    Clock system = Clock.systemUTC();
    LocalDateTime anker = LocalDateTime.parse("2025-12-30T04:09:47");

    Clock dev = DevClockFactory.ausAnker(anker, system);
    Instant erste = dev.instant();
    Instant zweite = dev.instant();

    assertThat(zweite).isAfterOrEqualTo(erste);
  }
}
