package de.kraftwerkone.overlord.monitor.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.kraftwerkone.overlord.monitor.common.error.FachlicheAusnahme;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Regel L1 ohne Datenbank: Ohne Zeitfenster keine Abfrage, Maximum ein Jahr. */
class ZeitfensterTest {

  /** Der Anker der Dev-Uhr aus Messung M9 — ein Zeitpunkt, an dem die Testkopie Daten hat. */
  private static final Clock UHR =
      Clock.fixed(Instant.parse("2025-12-30T04:09:47Z"), ZoneOffset.UTC);

  private static final LocalDateTime JETZT = LocalDateTime.parse("2025-12-30T04:09:47");

  @Test
  @DisplayName("Ohne Angabe gilt die Vorgabe von 24 Stunden")
  void vorgabe_ist_24_stunden() {
    Zeitfenster fenster = Zeitfenster.aufloesen(null, null, null, UHR);

    assertThat(fenster.bis()).isEqualTo(JETZT);
    assertThat(fenster.von()).isEqualTo(JETZT.minusHours(24));
  }

  @Test
  @DisplayName("Ein relativer Zeitraum wird gegen die Anwendungsuhr aufgeloest")
  void relativer_zeitraum_kommt_aus_der_anwendungsuhr() {
    Zeitfenster fenster = Zeitfenster.aufloesen(Zeitraum.LETZTE_7_TAGE, null, null, UHR);

    assertThat(fenster.von()).isEqualTo(JETZT.minusDays(7));
    assertThat(fenster.bis()).isEqualTo(JETZT);
  }

  @Test
  @DisplayName("Zeitraum und Zeitpunkte zugleich sind 400 — keine stille Vorrangregel")
  void beide_modi_zugleich_sind_400() {
    assertThatThrownBy(
            () ->
                Zeitfenster.aufloesen(
                    Zeitraum.LETZTE_24_STUNDEN,
                    LocalDateTime.parse("2025-12-29T00:00:00"),
                    LocalDateTime.parse("2025-12-30T00:00:00"),
                    UHR))
        .isInstanceOf(FachlicheAusnahme.class)
        .extracting(fehler -> ((FachlicheAusnahme) fehler).problemTyp())
        .isEqualTo("zeitfenster-mehrdeutig");
  }

  @Test
  @DisplayName("Ein halbes absolutes Fenster ist 400")
  void nur_von_ist_400() {
    assertThatThrownBy(
            () ->
                Zeitfenster.aufloesen(null, LocalDateTime.parse("2025-12-29T00:00:00"), null, UHR))
        .isInstanceOf(FachlicheAusnahme.class)
        .extracting(fehler -> ((FachlicheAusnahme) fehler).problemTyp())
        .isEqualTo("zeitfenster-unvollstaendig");
  }

  @Test
  @DisplayName("bis vor von ist 400")
  void verdrehte_grenzen_sind_400() {
    assertThatThrownBy(
            () ->
                Zeitfenster.aufloesen(
                    null,
                    LocalDateTime.parse("2025-12-30T00:00:00"),
                    LocalDateTime.parse("2025-12-29T00:00:00"),
                    UHR))
        .isInstanceOf(FachlicheAusnahme.class)
        .extracting(fehler -> ((FachlicheAusnahme) fehler).problemTyp())
        .isEqualTo("zeitfenster-ungueltig");
  }

  @Test
  @DisplayName("Genau ein Jahr ist erlaubt, eine Sekunde mehr nicht")
  void ein_jahr_ist_die_grenze() {
    LocalDateTime bis = LocalDateTime.parse("2025-12-30T00:00:00");

    assertThat(Zeitfenster.aufloesen(null, bis.minusYears(1), bis, UHR).von())
        .isEqualTo(bis.minusYears(1));

    assertThatThrownBy(
            () -> Zeitfenster.aufloesen(null, bis.minusYears(1).minusSeconds(1), bis, UHR))
        .isInstanceOf(FachlicheAusnahme.class)
        .extracting(fehler -> ((FachlicheAusnahme) fehler).problemTyp())
        .isEqualTo("zeitfenster-zu-gross");
  }

  @Test
  @DisplayName("Das Fenster ist beidseitig geschlossen")
  void grenzen_gehoeren_dazu() {
    Zeitfenster fenster = Zeitfenster.aufloesen(null, null, null, UHR);

    assertThat(fenster.enthaelt(fenster.von())).isTrue();
    assertThat(fenster.enthaelt(fenster.bis())).isTrue();
    assertThat(fenster.enthaelt(fenster.von().minusSeconds(1))).isFalse();
    assertThat(fenster.enthaelt(fenster.bis().plusSeconds(1))).isFalse();
  }

  @Test
  @DisplayName("Ein unbekannter Zeitraum-Code ist 400 und wird nicht auf die Vorgabe gezogen")
  void unbekannter_code_ist_400() {
    assertThatThrownBy(() -> Zeitraum.ausCode("24"))
        .isInstanceOf(FachlicheAusnahme.class)
        .extracting(fehler -> ((FachlicheAusnahme) fehler).problemTyp())
        .isEqualTo("zeitraum-unbekannt");

    assertThat(Zeitraum.ausCode("30d")).isEqualTo(Zeitraum.LETZTE_30_TAGE);
  }
}
