package de.kraftwerkone.overlord.monitor.dashboard;

import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.SERVICE;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jooq.DSLContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * <b>Die Sicherung gegen neue Dienststatuswerte</b> — nach dem Muster von {@code
 * DatenzugriffDbIT.statuskatalog_entspricht_dokumentierter_menge}.
 *
 * <p><b>Sie ist die Begruendung dafuer, dass ein unbekannter Wert neutral behandelt werden darf</b>
 * ({@code PROJEKTBESCHREIBUNG.md} §4.1): Der Test wird rot, sobald im Altsystem ein Wert auftaucht,
 * den {@link DienstStatusClassifier} nicht kennt — <b>oder</b> ein dokumentierter verschwindet.
 * Reagiert wird durch Pflege des Klassifizierers und von {@code docs/dienste.md}, nie durch Raten
 * (Regel Q4).
 *
 * <h2>Warum die zweite Zusicherung an der Testkopie haengen darf</h2>
 *
 * <p>Regel T2 verbietet Erwartungswerte aus veraenderlichen Daten. <b>{@code Service} ist
 * keine:</b> 20 Zeilen Stammdaten, auf der Testkopie ein eingefrorener Stand — M52 (14.08.2026) und
 * der Screenshot vom 10.09.2026 zeigen dieselben Werte. Und die Zusicherung nennt <b>keine
 * Zahl</b>, sondern eine <b>Menge</b>: „genau die drei bekannten Woerter". Kaeme eine Zeile dazu
 * oder fiele eine weg, bliebe der Test gruen, solange kein neues Wort erscheint.
 */
@SpringBootTest
@ActiveProfiles("dev")
@Tag("db")
class DienstkatalogDbIT {

  @Autowired
  @Qualifier("glassfishDsl") private DSLContext glassfishDsl;

  @Autowired private DienstStatusClassifier classifier;

  @Autowired private DienstLeseRepository dienstLeseRepository;

  private Set<String> vorhandeneStatuswerte() {
    Set<String> vorhanden =
        new HashSet<>(
            glassfishDsl
                .selectDistinct(SERVICE.SERVICESTATUS)
                .from(SERVICE)
                .fetch(SERVICE.SERVICESTATUS));
    vorhanden.remove(null);
    return vorhanden;
  }

  @Test
  @DisplayName("Dienstkatalog: DISTINCT ServiceStatus enthaelt keinen unbekannten Wert")
  void kein_unbekannter_dienststatus() {
    Set<String> vorhanden = vorhandeneStatuswerte();

    // Die eigentliche Sicherung. Ohne sie fiele ein neuer Wert still nach UNGEKLAERT, und niemand
    // erfuehre davon.
    assertThat(classifier.bekannteStatuswerte())
        .as("Neuer, undokumentierter ServiceStatus in der Testkopie: %s", vorhanden)
        .containsAll(vorhanden);
  }

  @Test
  @DisplayName("Und die vorhandenen Werte sind genau die bekannten")
  void vorhandene_sind_genau_die_bekannten() {
    // Anders als beim Nachrichtenstatus gibt es hier keine Ausnahme wie RUNNING: Alle drei
    // bekannten Woerter kommen vor. Verschwindet eines, gehoert docs/dienste.md geprueft.
    assertThat(vorhandeneStatuswerte()).isEqualTo(classifier.bekannteStatuswerte());
  }

  @Test
  @DisplayName("Jede Lampe traegt eine Kennung und einen der bekannten Zustaende")
  void lampen_sind_eingeordnet() {
    List<Dienstzeile> dienste = dienstLeseRepository.dienste();

    assertThat(dienste)
        .as("Ohne eine einzige Zeile mit ServiceTimeout > 0 bewiese dieser Test nichts")
        .isNotEmpty();
    assertThat(dienste)
        .allSatisfy(
            zeile -> {
              assertThat(zeile.serviceId()).isNotBlank();
              assertThat(classifier.einordnung(zeile.rohwert()))
                  .as("%s traegt den Rohwert %s", zeile.serviceId(), zeile.rohwert())
                  .isNotEqualTo(Dienstzustand.UNGEKLAERT);
            });
  }

  @Test
  @DisplayName("Die Lampen kommen sortiert und ohne Wiederholung")
  void lampen_sind_sortiert() {
    // Sortiert nach ServiceID und nicht nach Zustand: Sonst spraenge eine Lampe an eine andere
    // Stelle, sobald sich ihr Zustand aendert -- und genau dann sucht jemand sie an ihrem alten.
    List<String> kennungen =
        dienstLeseRepository.dienste().stream().map(Dienstzeile::serviceId).toList();

    assertThat(kennungen).isSorted().doesNotHaveDuplicates();
  }

  @Test
  @DisplayName("Jedes Pruefziel traegt eine Kennung — aufloesbar oder nicht")
  void pruefziele_sind_benannt() {
    // KEINE Zahl und kein Wert der Spalte (E-119): Geprueft wird die Gestalt. Ob heute eines,
    // zwei oder keines eingetragen ist, ist eine Frage der Daten und keine des Codes.
    assertThat(dienstLeseRepository.pruefziele())
        .allSatisfy(ziel -> assertThat(ziel.serviceId()).isNotBlank())
        .extracting(Ablagenziel::serviceId)
        .doesNotHaveDuplicates();
  }
}
