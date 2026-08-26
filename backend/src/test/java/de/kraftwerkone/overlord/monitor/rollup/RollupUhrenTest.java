package de.kraftwerkone.overlord.monitor.rollup;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * <b>Die Zuordnung der beiden Uhren — der Test, der einen vertauschten Aufruf faengt.</b>
 *
 * <p>Der Fehler waere in Produktion unsichtbar: Dort liefern Anwendungsuhr und {@code systemClock}
 * bis auf die Zone dasselbe. Sichtbar wuerde er erst im Profil {@code dev} — und dort saehe er aus
 * wie ein Datenproblem und nicht wie ein Codefehler: Der Lauf rechnete ueber ein Fenster, in dem
 * die Testkopie keine Zeilen hat, und schriebe null. Deshalb sind die beiden Uhren hier bewusst
 * <b>acht Monate auseinander</b> gestellt.
 */
class RollupUhrenTest {

  /** Der Anker der Testkopie: 2025-12-30 04:09:47 Wanduhrzeit, also 03:09:47 UTC im Winter. */
  private static final Instant ANKER_UTC = Instant.parse("2025-12-30T03:09:47Z");

  /** Ein Zeitpunkt, zu dem der Lauf tatsaechlich laeuft — Monate nach dem Anker. */
  private static final Instant JETZT_UTC = Instant.parse("2026-08-26T12:34:56Z");

  private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

  private final RollupUhren uhren =
      new RollupUhren(Clock.fixed(ANKER_UTC, BERLIN), Clock.fixed(JETZT_UTC, ZoneOffset.UTC));

  @Test
  @DisplayName("Datenzeit kommt aus der Anwendungsuhr, in deren Zone")
  void datenzeit_kommt_aus_der_anwendungsuhr() {
    assertThat(uhren.datenzeit())
        .as(
            "Das ist der Anker der Testkopie in Wanduhrzeit — genau die Form, in der"
                + " Message.MessageLastUpdate gefuehrt wird (docs/datenzugriff.md §7)")
        .isEqualTo(LocalDateTime.parse("2025-12-30T04:09:47"));
  }

  @Test
  @DisplayName("Protokollzeit kommt aus systemClock, in UTC")
  void protokollzeit_kommt_aus_der_systemuhr() {
    assertThat(uhren.protokollzeit())
        .as("Protokollzeit wie geaendert_am und audit_log (Regel A5): echte Uhr, UTC")
        .isEqualTo(LocalDateTime.parse("2026-08-26T12:34:56"));
  }

  @Test
  @DisplayName("Die beiden Uhren sind nicht vertauscht")
  void die_beiden_uhren_sind_nicht_vertauscht() {
    assertThat(uhren.datenzeit())
        .as(
            "Waeren sie vertauscht, rechnete der Lauf im Profil dev ueber ein Fenster, in dem die"
                + " Testkopie keine Zeilen hat — und das saehe wie ein Datenproblem aus")
        .isBefore(uhren.protokollzeit());
    assertThat(uhren.datenzeit().getYear()).isEqualTo(2025);
    assertThat(uhren.protokollzeit().getYear()).isEqualTo(2026);
  }

  @Test
  @DisplayName("Die Protokollzeit traegt den Zonenversatz der Anwendungsuhr nicht mit")
  void protokollzeit_ohne_zonenversatz_der_anwendungsuhr() {
    RollupUhren gleicheInstant =
        new RollupUhren(Clock.fixed(JETZT_UTC, BERLIN), Clock.fixed(JETZT_UTC, ZoneOffset.UTC));

    assertThat(gleicheInstant.datenzeit())
        .as("Berlin liegt im Sommer zwei Stunden vor UTC")
        .isEqualTo(LocalDateTime.parse("2026-08-26T14:34:56"));
    assertThat(gleicheInstant.protokollzeit())
        .as("Derselbe Augenblick, aber die Protokollzeit steht immer in UTC")
        .isEqualTo(LocalDateTime.parse("2026-08-26T12:34:56"));
  }
}
