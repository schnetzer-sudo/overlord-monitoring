package de.kraftwerkone.overlord.monitor.assistent;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.common.MandantContext;
import de.kraftwerkone.overlord.monitor.security.MandantResponse;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Welcher Name des Mandanten in die Anweisung geht (E‑1059, {@code docs/assistent.md} §3.1): der
 * Name des aktiven Mandanten, <b>nie ersatzweise seine Kennung</b>.
 *
 * <p>Der Anzeigename aus {@code MandantRepository} faellt ohne {@code MandantName} auf die Kennung
 * zurueck. Auf der Testkopie hat jeder Mandant einen Namen; dieser Fall laesst sich dort also nicht
 * belegen und steht deshalb hier.
 */
class MandantennameTest {

  private static final MandantContext NEXANS = new MandantContext("NEXANS");

  @Test
  @DisplayName("Der Name des aktiven Mandanten")
  void name() {
    assertThat(
            AssistentController.mandantenname(
                Optional.of(new MandantResponse("NEXANS", "Beispielkunde GmbH")), NEXANS))
        .isEqualTo("Beispielkunde GmbH");
  }

  @Test
  @DisplayName("Ist der Anzeigename die Kennung, geht kein Name hin")
  void kennung_ist_kein_name() {
    assertThat(
            AssistentController.mandantenname(
                Optional.of(new MandantResponse("NEXANS", "NEXANS")), NEXANS))
        .isNull();
  }

  @Test
  @DisplayName("Ein anderer aktiver Mandant als der Kontext: kein Name")
  void anderer_mandant() {
    assertThat(
            AssistentController.mandantenname(
                Optional.of(new MandantResponse("IBIS", "Anderer Kunde AG")), NEXANS))
        .isNull();
  }

  @Test
  @DisplayName("Kein aktiver Mandant: kein Name")
  void ohne_aktiven() {
    assertThat(AssistentController.mandantenname(Optional.empty(), NEXANS)).isNull();
  }
}
