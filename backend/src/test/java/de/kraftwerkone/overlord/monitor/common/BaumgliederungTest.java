package de.kraftwerkone.overlord.monitor.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.kraftwerkone.overlord.monitor.common.error.FachlicheAusnahme;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Die Whitelist der Baumgliederung — <b>zwei Lesarten, und sie sind absichtlich verschieden
 * streng</b>.
 *
 * <p>Aus der Anfrage heisst „nichts" <i>nicht angegeben</i> und ein unbekannter Wert {@code 400};
 * aus der Datenbank gibt es kein „nichts", und ein unbekannter Wert ist ein Datenfehler.
 */
class BaumgliederungTest {

  @Test
  @DisplayName("Ohne Angabe heisst nicht angegeben — und nicht PARTNER")
  void ohne_angabe_ist_null() {
    assertThat(Baumgliederung.ausText(null)).isNull();
    assertThat(Baumgliederung.ausText("")).isNull();
    assertThat(Baumgliederung.ausText("   ")).isNull();
  }

  @Test
  @DisplayName("Beide Werte werden erkannt, ohne Ruecksicht auf Schreibweise und Rand")
  void bekannte_werte() {
    assertThat(Baumgliederung.ausText("PARTNER")).isEqualTo(Baumgliederung.PARTNER);
    assertThat(Baumgliederung.ausText("projekt")).isEqualTo(Baumgliederung.PROJEKT);
    assertThat(Baumgliederung.ausText(" Projekt ")).isEqualTo(Baumgliederung.PROJEKT);
  }

  /**
   * Ein unbekannter Wert faellt <b>nicht</b> auf die Vorgabe zurueck — sonst zeigte ein Tippfehler
   * in einem geteilten Link die Vorgabe des Empfaengers, und niemand wuesste warum.
   */
  @Test
  @DisplayName("Ein unbekannter Wert ist 400 gliederung-unbekannt")
  void unbekannter_wert() {
    assertThatThrownBy(() -> Baumgliederung.ausText("SOS"))
        .isInstanceOf(FachlicheAusnahme.class)
        .satisfies(
            ausnahme ->
                assertThat(((FachlicheAusnahme) ausnahme).titel())
                    .isEqualTo("Gliederung unbekannt"));
  }

  @Test
  @DisplayName("Aus der Datenbank streng: genau die Namen, sonst ein Datenfehler")
  void aus_der_datenbank_streng() {
    assertThat(Baumgliederung.ausDatenbank("PROJEKT")).isEqualTo(Baumgliederung.PROJEKT);
    assertThatThrownBy(() -> Baumgliederung.ausDatenbank("projekt"))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> Baumgliederung.ausDatenbank(null))
        .isInstanceOf(IllegalStateException.class);
  }
}
