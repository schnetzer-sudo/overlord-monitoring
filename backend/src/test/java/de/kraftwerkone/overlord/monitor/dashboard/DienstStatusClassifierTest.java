package de.kraftwerkone.overlord.monitor.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Die Einordnung der Dienststatuswerte — <b>alle vier Faelle, und die beiden Wege nach {@link
 * Dienstzustand#UNGEKLAERT} einzeln</b>.
 *
 * <p>Der Test braucht keine Datenbank: Er prueft eine Zuordnung und keinen Bestand.
 */
class DienstStatusClassifierTest {

  private final DienstStatusClassifier classifier = new DienstStatusClassifier();

  @ParameterizedTest(name = "{0} wird {1}")
  @CsvSource({
    "HEARTBEAT, MELDET_SICH",
    "ERROR_TIMEOUT, ZEITUEBERSCHRITTEN",
    "SHUTDOWN, HERUNTERGEFAHREN"
  })
  @DisplayName("Die drei bekannten Rohwerte")
  void bekannte_rohwerte(String rohwert, Dienstzustand erwartet) {
    assertThat(classifier.einordnung(rohwert)).isEqualTo(erwartet);
  }

  @Test
  @DisplayName("NULL wird UNGEKLAERT und nicht MELDET_SICH")
  void null_wird_ungeklaert() {
    // Eine leere Spalte heisst "keine Angabe" und nicht "alles in Ordnung". Die Richtung ist der
    // ganze Punkt: Ein Dienst ohne Statuswort darf nicht gruen leuchten.
    assertThat(classifier.einordnung(null)).isEqualTo(Dienstzustand.UNGEKLAERT);
  }

  @ParameterizedTest(name = "{0} wird UNGEKLAERT")
  @ValueSource(
      strings = {
        "ERFUNDEN",
        // Die beiden Faelle, die ein Praefixvergleich stillschweigend einsortiert haette:
        "ERROR_STARTUP",
        "ERRORX",
        // Gross- und Kleinschreibung ist nicht derselbe Wert -- verglichen wird exakt.
        "heartbeat",
        "",
        " HEARTBEAT"
      })
  @DisplayName("Jeder andere Wert wird UNGEKLAERT — auch einer mit ERROR_-Praefix")
  void unbekannte_werte(String rohwert) {
    assertThat(classifier.einordnung(rohwert)).isEqualTo(Dienstzustand.UNGEKLAERT);
  }

  @Test
  @DisplayName("Die bekannte Menge ist genau die, gegen die der Drift-Test prueft")
  void bekannte_menge() {
    // Sie ist die Begruendung dafuer, dass UNGEKLAERT neutral behandelt werden darf: Taucht im
    // Altsystem ein vierter Wert auf, wird DienstkatalogDbIT rot.
    assertThat(classifier.bekannteStatuswerte())
        .containsExactlyInAnyOrder("HEARTBEAT", "ERROR_TIMEOUT", "SHUTDOWN");
  }

  @Test
  @DisplayName("Jeder bekannte Wert hat eine eigene Einordnung — keiner faellt nach UNGEKLAERT")
  void kein_bekannter_wert_faellt_durch() {
    // Ohne diese Zusicherung koennte jemand einen Wert in bekannteStatuswerte eintragen, ohne ihn
    // im switch zu behandeln: Der Drift-Test blieb gruen, die Lampe waere trotzdem ungeklaert.
    assertThat(classifier.bekannteStatuswerte())
        .allSatisfy(
            rohwert ->
                assertThat(classifier.einordnung(rohwert)).isNotEqualTo(Dienstzustand.UNGEKLAERT));
  }
}
