package de.kraftwerkone.overlord.monitor.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.catalog.ProzessKatalogRepository.Bestandsflag;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import de.kraftwerkone.overlord.monitor.security.MandantRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * <b>Die Bestandsabfrage gegen den echten Bestand — und gegen die Zahlen aus M83‑5.</b>
 *
 * <p>Sie ist die lesende Haelfte des Bestandslaufs (E14) und reproduziert die Gegenprobe der
 * Messrunde: {@code NEXANS} <b>733 / 516 / 217</b>, {@code SUTTONS} <b>17 / 17 / 0</b>.
 *
 * <p><b>Zwei Mandanten, und das ist Regel L7 und keine Bequemlichkeit</b>: der groesste und ein
 * kleiner. Ein Statement, das nur gegen einen Mandanten geprueft ist, kann an einer Datenmenge
 * haengen, die kein zweiter hat.
 *
 * <p><b>{@code SUTTONS} ist dabei die schaerfere Probe</b>, obwohl es der kleinere Bestand ist:
 * Dort <b>muss</b> die Menge der toten Prozesse leer bleiben. Eine Fassung, die versehentlich
 * Zeilen erzeugt — ein Join zu viel, ein {@code LEFT} statt eines inneren —, faellt genau hier auf
 * und bei {@code NEXANS} nicht, weil dort 217 tote Prozesse ohnehin erwartet werden.
 *
 * <p><b>Er schreibt nichts.</b> {@code process_catalog} wird nicht angefasst; gepruoft ist allein
 * die Abfrage.
 *
 * <p>Schlaegt er fehl, ist entweder das Statement geaendert worden — oder die Testkopie ist neu
 * befuellt. Die Zahlen stammen aus der Erhebung vom <b>21.08.2026</b> (M83‑5) und decken sich dort
 * mit M74b vom 20.08.2026. Dieselbe Art Zusicherung wie in {@code HeuristikBestandDbIT}: an einen
 * Datenstand gebunden und deshalb hier benannt.
 */
@SpringBootTest
@ActiveProfiles("dev")
@Tag("db")
class BestandslaufDbIT {

  @Autowired private ProzessKatalogRepository repository;
  @Autowired private MandantRepository mandantRepository;

  private List<Bestandsflag> flags(String mandantId) {
    assertThat(mandantRepository.existiert(mandantId))
        .as("Ohne den Mandanten %s prueft dieser Test nichts", mandantId)
        .isTrue();
    return repository.findeBestandsflags(new MandantContext(mandantId));
  }

  private static long mitNachrichten(List<Bestandsflag> flags) {
    return flags.stream().filter(Bestandsflag::traegtNachrichten).count();
  }

  private static long ohneNachrichten(List<Bestandsflag> flags) {
    return flags.stream().filter(flag -> !flag.traegtNachrichten()).count();
  }

  @Test
  @DisplayName("NEXANS: 733 Prozesse, 516 mit Nachrichten, 217 ohne (M83-5)")
  void nexans_reproduziert_m83() {
    List<Bestandsflag> flags = flags("NEXANS");

    assertThat(flags).as("Der groesste Mandant, seit L12 an neun Stellen belegt").hasSize(733);
    assertThat(mitNachrichten(flags)).isEqualTo(516);
    assertThat(ohneNachrichten(flags)).isEqualTo(217);
  }

  @Test
  @DisplayName("SUTTONS: 17 Prozesse, alle mit Nachrichten, keiner ohne (M83-5)")
  void suttons_reproduziert_m83() {
    List<Bestandsflag> flags = flags("SUTTONS");

    assertThat(flags).hasSize(17);
    assertThat(mitNachrichten(flags)).isEqualTo(17);
    assertThat(ohneNachrichten(flags))
        .as(
            "Die schaerfere Probe: Hier MUSS die Menge der toten Prozesse leer bleiben. Eine"
                + " Fassung, die versehentlich Zeilen erzeugt, faellt genau hier auf")
        .isZero();
  }

  @Test
  @DisplayName("Der Join erzeugt keine Doubletten — jede ProcessID steht genau einmal")
  void keine_doubletten() {
    for (String mandantId : List.of("NEXANS", "SUTTONS")) {
      List<Bestandsflag> flags = flags(mandantId);
      assertThat(flags.stream().map(Bestandsflag::processId).distinct().count())
          .as(
              "M83-5 hat COUNT(*) gegen COUNT(DISTINCT ProcessID) gehalten und beide gleich"
                  + " gefunden. Weicht das hier ab, erzeugt der Join Doubletten und der"
                  + " Bestandslauf schriebe dieselbe Zeile mehrfach: %s",
              mandantId)
          .isEqualTo(flags.size());
    }
  }

  @Test
  @DisplayName("Die Abfrage sieht ausschliesslich den eigenen Mandanten (Regel M3)")
  void mandantentrennung() {
    List<Bestandsflag> nexans = flags("NEXANS");
    List<Bestandsflag> suttons = flags("SUTTONS");

    assertThat(nexans.stream().map(Bestandsflag::processId))
        .as("Kein Prozess darf in beiden Mengen stehen — M74a: kein Projekt an zwei Mandanten")
        .doesNotContainAnyElementsOf(suttons.stream().map(Bestandsflag::processId).toList());
    assertThat(repository.findeBestandsflags(new MandantContext("gibt-es-nicht")))
        .as("Ein erfundener Mandant sieht nichts, nicht alles")
        .isEmpty();
  }
}
