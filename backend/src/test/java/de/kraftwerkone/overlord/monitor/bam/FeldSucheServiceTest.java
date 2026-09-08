package de.kraftwerkone.overlord.monitor.bam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.kraftwerkone.overlord.monitor.common.MessageStatusClassifier;
import de.kraftwerkone.overlord.monitor.common.Zeitfenster;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Die Fachlogik der Property-Suche im Service — <b>ohne Datenbank</b>: Welcher Feldbegriff wird zur
 * Spalte, welcher zur Zeile, und was steht davon in der Antwort.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FeldSucheServiceTest {

  private static final MandantContext MANDANT = new MandantContext("NEXANS");
  private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");

  private static final Zeitfenster FENSTER =
      new Zeitfenster(
          LocalDateTime.parse("2025-11-30T00:00:00"), LocalDateTime.parse("2025-12-30T00:00:00"));

  @Mock private BamSucheRepository repository;

  @Captor private ArgumentCaptor<List<Feldbedingung>> feldbedingungen;

  private BamSucheService service() {
    when(repository.findeSollaengen(any())).thenReturn(List.of());
    when(repository.findeTreffer(any(), anyList(), any())).thenReturn(List.of());
    when(repository.findeTreffer(any(), anyList(), anyList(), any())).thenReturn(List.of());
    when(repository.findeTrefferWerte(any(), anyList(), anyList())).thenReturn(List.of());
    return new BamSucheService(
        repository, new MessageStatusClassifier(), Clock.fixed(Instant.EPOCH, ZONE));
  }

  private static BamSuchfilter filter(List<Suchbegriff> begriffe, List<Feldbegriff> felder) {
    return new BamSuchfilter(begriffe, felder, FENSTER, Suchmodus.EXAKT);
  }

  private static BamTrefferZeile zeile(String messageId) {
    return new BamTrefferZeile(
        messageId,
        LocalDateTime.parse("2025-12-29T12:00:00"),
        "FINISHED",
        "ein-prozess",
        "EIN_PROZESS",
        "Ein Projekt",
        "Ein Ablauf",
        "Ein Schritt",
        null,
        null,
        null,
        null);
  }

  /**
   * <b>Spalte oder Zeile, entschieden ueber die Abbildung</b> (E‑101): {@code Message.Status} kennt
   * {@link Typ0Feld}, {@code Message.GUID} nicht — und ein Name in anderer Schreibweise wird wie
   * die Spalte gefunden.
   */
  @Test
  @DisplayName("Ein abgebildeter Name wird zur Spalte, jeder andere zur Zeile")
  void spalte_oder_zeile_ueber_die_abbildung() {
    BamSucheService service = service();

    service.suche(
        MANDANT,
        filter(
            List.of(),
            List.of(
                new Feldbegriff("Message.Status", "FINISHED"),
                new Feldbegriff("Message.GUID", "abc"),
                new Feldbegriff("message.processname", "P"))));

    verify(repository).findeTreffer(any(), anyList(), feldbedingungen.capture(), any());
    assertThat(feldbedingungen.getValue())
        .containsExactly(
            new Feldbedingung("Message.Status", "FINISHED", Typ0Feld.STATUS),
            new Feldbedingung("Message.GUID", "abc", null),
            new Feldbedingung("message.processname", "P", Typ0Feld.PROCESS_NAME));
  }

  /** Das Zitat der Frage: Name und Wert wie eingegeben, dazu ob als Spalte gesucht wurde. */
  @Test
  @DisplayName("Die Antwort nennt jeden Feldbegriff mit dem Kennzeichen spalte")
  void die_antwort_zitiert_die_felder() {
    BamSucheResponse antwort =
        service()
            .suche(
                MANDANT,
                filter(
                    List.of(),
                    List.of(
                        new Feldbegriff("Message.Status", "FINISHED"),
                        new Feldbegriff("Message.GUID", "abc"))));

    assertThat(antwort.felder())
        .containsExactly(
            new FeldBegriffResponse("Message.Status", "FINISHED", true),
            new FeldBegriffResponse("Message.GUID", "abc", false));
    assertThat(antwort.begriffe()).isEmpty();
  }

  /**
   * <b>Ohne BAM-Begriff keine zweite Abfrage.</b> Es gibt keinen BAM-Treffer zu beschriften — und
   * ohne Bedingung liefe Abfrage (b) ueber alle Werte der gefundenen Nachrichten.
   */
  @Test
  @DisplayName("Eine Suche allein ueber Felder stellt keine Trefferwerte-Abfrage")
  void ohne_bam_begriff_keine_trefferwerte() {
    when(repository.findeTreffer(any(), anyList(), anyList(), any()))
        .thenReturn(List.of(zeile("m1")));
    BamSucheService service = service();
    when(repository.findeTreffer(any(), anyList(), anyList(), any()))
        .thenReturn(List.of(zeile("m1")));

    BamSucheResponse antwort =
        service.suche(MANDANT, filter(List.of(), List.of(new Feldbegriff("Message.GUID", "a"))));

    verify(repository, never()).findeTrefferWerte(any(), anyList(), anyList());
    assertThat(antwort.nachrichten()).hasSize(1);
    assertThat(antwort.nachrichten().getFirst().treffer()).as("leer statt fehlend").isEmpty();
  }

  /**
   * Mit BAM- und Feldbegriffen laeuft der neue Pfad, und Abfrage (b) beschriftet die BAM-Treffer.
   */
  @Test
  @DisplayName(
      "Mit beiden Begriffsarten laeuft der neue Pfad, und die BAM-Treffer werden beschriftet")
  void beide_arten_zusammen() {
    BamSucheService service = service();

    service.suche(
        MANDANT,
        filter(
            List.of(new Suchbegriff(null, "4711")), List.of(new Feldbegriff("Message.GUID", "a"))));

    verify(repository).findeTreffer(any(), anyList(), anyList(), any());
    verify(repository, never()).findeTreffer(any(), anyList(), any());
    verify(repository).findeTrefferWerte(any(), anyList(), anyList());
  }
}
