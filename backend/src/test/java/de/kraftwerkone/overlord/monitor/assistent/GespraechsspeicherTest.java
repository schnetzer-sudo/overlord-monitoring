package de.kraftwerkone.overlord.monitor.assistent;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.common.MandantContext;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Der Gespraechsspeicher (E‑1038) mit eingespeister Uhr (Regel T1): Ablauf, Hoechstzahl je Sitzung,
 * Bindung an Sitzung, Nutzer und Mandant, Mandantenwechsel.
 */
class GespraechsspeicherTest {

  private static final MandantContext NEXANS = new MandantContext("NEXANS");
  private static final MandantContext IBIS = new MandantContext("IBIS");

  private Stelluhr uhr;
  private Gespraechsspeicher speicher;

  @BeforeEach
  void aufbau() {
    uhr = new Stelluhr();
    speicher = new Gespraechsspeicher(uhr, Duration.ofMinutes(30), 3);
  }

  private Gespraech eroeffne(MandantContext mandant, long nutzer, String sitzung) {
    return speicher.eroeffne(mandant, nutzer, sitzung, "Anweisung", List.of());
  }

  @Test
  @DisplayName("Gefunden nur mit Sitzung, Nutzer und Mandant — jede Abweichung fuer sich")
  void bindung() {
    UUID id = eroeffne(NEXANS, 1, "s1").id();

    assertThat(speicher.finde(NEXANS, 1, "s1", id)).isPresent();
    assertThat(speicher.finde(NEXANS, 1, "s2", id)).as("andere Sitzung").isEmpty();
    assertThat(speicher.finde(NEXANS, 2, "s1", id)).as("anderer Nutzer").isEmpty();
    assertThat(speicher.finde(IBIS, 1, "s1", id)).as("anderer Mandant").isEmpty();
    assertThat(speicher.finde(NEXANS, 1, "s1", UUID.randomUUID())).as("erfunden").isEmpty();
    // Die fremden Zugriffe haben es nicht beendet.
    assertThat(speicher.finde(NEXANS, 1, "s1", id)).isPresent();
  }

  @Test
  @DisplayName("Ablauf 30 Minuten nach der letzten Aktivitaet, nicht nach der Eroeffnung")
  void ablauf() {
    Gespraech gespraech = eroeffne(NEXANS, 1, "s1");

    uhr.vor(Duration.ofMinutes(29));
    assertThat(speicher.finde(NEXANS, 1, "s1", gespraech.id())).isPresent();
    gespraech.beruehre(uhr.instant());
    uhr.vor(Duration.ofMinutes(29));
    assertThat(speicher.finde(NEXANS, 1, "s1", gespraech.id())).isPresent();
    uhr.vor(Duration.ofMinutes(1));
    assertThat(speicher.finde(NEXANS, 1, "s1", gespraech.id())).isEmpty();
    assertThat(speicher.anzahl()).isZero();
  }

  @Test
  @DisplayName("Hoechstens drei je Sitzung: das vierte beendet das am laengsten ruhende (F9)")
  void hoechstens_drei_je_sitzung() {
    Gespraech erstes = eroeffne(NEXANS, 1, "s1");
    uhr.vor(Duration.ofSeconds(1));
    Gespraech zweites = eroeffne(NEXANS, 1, "s1");
    uhr.vor(Duration.ofSeconds(1));
    Gespraech drittes = eroeffne(NEXANS, 1, "s1");
    uhr.vor(Duration.ofSeconds(1));
    erstes.beruehre(uhr.instant());
    Gespraech andereSitzung = eroeffne(NEXANS, 1, "s2");

    Gespraech viertes = eroeffne(NEXANS, 1, "s1");

    assertThat(speicher.finde(NEXANS, 1, "s1", zweites.id())).as("am laengsten ruhend").isEmpty();
    assertThat(speicher.finde(NEXANS, 1, "s1", erstes.id())).isPresent();
    assertThat(speicher.finde(NEXANS, 1, "s1", drittes.id())).isPresent();
    assertThat(speicher.finde(NEXANS, 1, "s1", viertes.id())).isPresent();
    assertThat(speicher.finde(NEXANS, 1, "s2", andereSitzung.id())).isPresent();
  }

  @Test
  @DisplayName("Mandantenwechsel beendet die Gespraeche dieser Sitzung, nicht die anderer")
  void mandantenwechsel() {
    Gespraech eigenes = eroeffne(NEXANS, 1, "s1");
    Gespraech andereSitzung = eroeffne(NEXANS, 1, "s2");

    speicher.beendeNachMandantenwechsel(IBIS, "s1");

    assertThat(speicher.finde(NEXANS, 1, "s1", eigenes.id())).isEmpty();
    assertThat(speicher.finde(IBIS, 1, "s1", eigenes.id())).isEmpty();
    assertThat(speicher.finde(NEXANS, 1, "s2", andereSitzung.id())).isPresent();
  }

  @Test
  @DisplayName("Beenden nur mit derselben Bindung; danach ist es weg")
  void beenden() {
    UUID id = eroeffne(NEXANS, 1, "s1").id();

    assertThat(speicher.beende(NEXANS, 2, "s1", id)).isFalse();
    assertThat(speicher.beende(NEXANS, 1, "s1", id)).isTrue();
    assertThat(speicher.finde(NEXANS, 1, "s1", id)).isEmpty();
    assertThat(speicher.beende(NEXANS, 1, "s1", id)).isFalse();
  }
}
