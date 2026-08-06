package de.kraftwerkone.overlord.monitor.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.kraftwerkone.overlord.monitor.common.BamSpalte;
import de.kraftwerkone.overlord.monitor.common.MessageStatusClassifier;
import de.kraftwerkone.overlord.monitor.common.Seite;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Was aus den beiden Abfragen wird: Gruppierung der BAM-Werte, Kuerzung ab dem vierten, und die
 * Spaltenausrichtung ueber alle Zeilen. Ohne Datenbank — die Statements pruefen die
 * Integrationstests und die Messungen.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NachrichtenServiceTest {

  private static final MandantContext MANDANT = new MandantContext("NEXANS");
  private static final LocalDateTime ZEITPUNKT = LocalDateTime.parse("2025-12-29T23:22:41");

  private static final short LIEFERSCHEIN = 9006;
  private static final short ABRUFNUMMER = 9001;

  @Mock private NachrichtenRepository nachrichtenRepository;
  @Mock private BamSpaltenRepository bamSpaltenRepository;

  /** Kein Mock: Die Einordnung ist Fachlogik und soll hier mitlaufen, nicht wegdefiniert werden. */
  private final MessageStatusClassifier statusClassifier = new MessageStatusClassifier();

  private static NachrichtenFilter filter() {
    return NachrichtenFilter.aus(
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        10,
        Clock.fixed(Instant.parse("2025-12-30T04:09:47Z"), ZoneOffset.UTC));
  }

  private static NachrichtZeile zeile(String id) {
    return new NachrichtZeile(id, ZEITPUNKT, "FINISHED", "prozess-1", "40000_AMG", "300_Kunden");
  }

  private NachrichtenService service() {
    return new NachrichtenService(
        nachrichtenRepository,
        bamSpaltenRepository,
        statusClassifier,
        Clock.fixed(Instant.parse("2025-12-30T04:09:47Z"), ZoneOffset.UTC));
  }

  @Test
  @DisplayName("Mehr als drei Werte werden gekuerzt, die Zahl der uebrigen steht dabei")
  void mehr_als_drei_werte_werden_gekuerzt() {
    when(nachrichtenRepository.finde(any(), any())).thenReturn(List.of(zeile("m1")));
    when(bamSpaltenRepository.spalten(MANDANT))
        .thenReturn(
            List.of(
                new BamSpalte(1, LIEFERSCHEIN, "Lieferschein-Nr."),
                new BamSpalte(2, ABRUFNUMMER, "Abrufnummer")));
    when(nachrichtenRepository.findeBamWerte(any(), any(), any()))
        .thenReturn(
            List.of(
                new BamWert("m1", LIEFERSCHEIN, "LS-5"),
                new BamWert("m1", LIEFERSCHEIN, "LS-1"),
                new BamWert("m1", LIEFERSCHEIN, "LS-3"),
                new BamWert("m1", LIEFERSCHEIN, "LS-2"),
                new BamWert("m1", LIEFERSCHEIN, "LS-4")));

    Seite<NachrichtResponse> seite = service().liste(MANDANT, filter());
    List<NachrichtResponse.BamWerte> werte = seite.items().getFirst().bamWerte();

    assertThat(werte).hasSize(2);
    assertThat(werte.getFirst().werte())
        .as("sortiert und auf drei gekuerzt")
        .containsExactly("LS-1", "LS-2", "LS-3");
    assertThat(werte.getFirst().weitere()).isEqualTo(2);
    assertThat(werte.get(1).werte()).as("Die zweite Spalte bleibt leer, aber sie bleibt").isEmpty();
    assertThat(werte.get(1).weitere()).isZero();
  }

  @Test
  @DisplayName("Derselbe Wert zweimal ist keine zusaetzliche Auskunft")
  void doppelte_werte_fallen_weg() {
    when(nachrichtenRepository.finde(any(), any())).thenReturn(List.of(zeile("m1")));
    when(bamSpaltenRepository.spalten(MANDANT))
        .thenReturn(List.of(new BamSpalte(1, LIEFERSCHEIN, "Lieferschein-Nr.")));
    when(nachrichtenRepository.findeBamWerte(any(), any(), any()))
        .thenReturn(
            List.of(
                new BamWert("m1", LIEFERSCHEIN, "LS-1"), new BamWert("m1", LIEFERSCHEIN, "LS-1")));

    List<NachrichtResponse.BamWerte> werte =
        service().liste(MANDANT, filter()).items().getFirst().bamWerte();

    assertThat(werte.getFirst().werte()).containsExactly("LS-1");
    assertThat(werte.getFirst().weitere()).isZero();
  }

  @Test
  @DisplayName("Ohne BAM-Konfiguration wird nicht mit leerer Typliste abgefragt")
  void ohne_konfiguration_keine_abfrage() {
    when(nachrichtenRepository.finde(any(), any())).thenReturn(List.of(zeile("m1")));
    when(bamSpaltenRepository.spalten(MANDANT)).thenReturn(List.of());

    Seite<NachrichtResponse> seite = service().liste(MANDANT, filter());

    assertThat(seite.items().getFirst().bamWerte()).isEmpty();
    verify(nachrichtenRepository, never()).findeBamWerte(any(), any(), any());
  }

  @Test
  @DisplayName("Eine leere Seite laedt keine BAM-Werte nach")
  void leere_seite_laedt_nichts_nach() {
    when(nachrichtenRepository.finde(any(), any())).thenReturn(List.of());

    Seite<NachrichtResponse> seite = service().liste(MANDANT, filter());

    assertThat(seite.items()).isEmpty();
    assertThat(seite.hasMore()).isFalse();
    verify(bamSpaltenRepository, never()).spalten(any());
    verify(nachrichtenRepository, never()).findeBamWerte(any(), any(), any());
  }

  /**
   * Die Werte werden nur fuer die <b>gelieferten</b> Zeilen nachgeladen — nicht fuer die
   * Zusatzzeile, an der die Seite „gibt es eine naechste" erkennt.
   */
  @Test
  @DisplayName("Die Zusatzzeile fuer hasMore bekommt keine BAM-Werte")
  void zusatzzeile_bekommt_keine_werte() {
    when(nachrichtenRepository.finde(any(), any()))
        .thenReturn(
            List.of(
                zeile("m1"),
                zeile("m2"),
                zeile("m3"),
                zeile("m4"),
                zeile("m5"),
                zeile("m6"),
                zeile("m7"),
                zeile("m8"),
                zeile("m9"),
                zeile("m10"),
                zeile("m11")));
    when(bamSpaltenRepository.spalten(MANDANT))
        .thenReturn(List.of(new BamSpalte(1, LIEFERSCHEIN, "Lieferschein-Nr.")));
    when(nachrichtenRepository.findeBamWerte(any(), any(), any())).thenReturn(List.of());

    Seite<NachrichtResponse> seite = service().liste(MANDANT, filter());

    assertThat(seite.items()).hasSize(10);
    assertThat(seite.hasMore()).isTrue();
    verify(nachrichtenRepository)
        .findeBamWerte(
            MANDANT,
            List.of("m1", "m2", "m3", "m4", "m5", "m6", "m7", "m8", "m9", "m10"),
            List.of(LIEFERSCHEIN));
  }
}
