package de.kraftwerkone.overlord.monitor.bam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Die Fachlogik des Angebots — <b>ohne Datenbank</b>: welche Konfigurationszeile zu welchem Eintrag
 * wird, und welche zu keinem.
 *
 * <p>Alle Namen sind erfunden bis auf die, die {@link Typ0Feld} kennt (Regel G1).
 */
@ExtendWith(MockitoExtension.class)
class SuchfelderServiceTest {

  private static final MandantContext MANDANT = new MandantContext("NEXANS");

  @Mock private BamTypenService bamTypenService;
  @Mock private SuchfelderRepository repository;

  private SuchfelderService service() {
    return new SuchfelderService(bamTypenService, repository);
  }

  private void gib(List<BamTypResponse> typen, List<SuchfeldZeile> felder) {
    when(bamTypenService.typen(any())).thenReturn(typen);
    when(repository.findeKonfigurierteFelder(any())).thenReturn(felder);
  }

  /**
   * <b>Die BAM-Gruppe ist die Antwort von {@code /api/bam/typen}, Zeile fuer Zeile</b> — mit der
   * Quelle davor. Nichts wird nachgebaut, nichts umsortiert.
   */
  @Test
  @DisplayName("Die BAM-Gruppe uebernimmt die Belegarten unveraendert und nennt ihre Quelle")
  void bam_gruppe_uebernimmt_die_belegarten() {
    gib(
        List.of(
            new BamTypResponse((short) 9018, "Kundenmaterialnummer", (short) 18),
            new BamTypResponse((short) 9012, "9012", (short) 20)),
        List.of());

    SuchfelderResponse antwort = service().angebot(MANDANT);

    assertThat(antwort.bam())
        .containsExactly(
            new SuchfeldBamResponse("bam", (short) 9018, "Kundenmaterialnummer", (short) 18),
            new SuchfeldBamResponse("bam", (short) 9012, "9012", (short) 20));
    assertThat(antwort.felder()).isEmpty();
  }

  /**
   * <b>Typ 1 wird angeboten, Typ 0 nur mit Abbildung.</b> Ein Typ-0-Name, den {@link Typ0Feld}
   * nicht kennt, faellt still weg — und darf das nur, weil {@code Typ0AbbildungDbIT} ihn meldet.
   */
  @Test
  @DisplayName("Typ 1 wird angeboten, Typ 0 nur mit Abbildung — der unbekannte faellt weg")
  void typ0_nur_mit_abbildung() {
    gib(
        List.of(),
        List.of(
            new SuchfeldZeile("Converter.TransactionID", 1),
            new SuchfeldZeile("Message.Status", 0),
            new SuchfeldZeile("Message.Unbekannt", 0),
            new SuchfeldZeile("Message.VFN", 1)));

    SuchfelderResponse antwort = service().angebot(MANDANT);

    assertThat(antwort.felder())
        .containsExactly(
            new SuchfeldFeldResponse("feld", "Converter.TransactionID", false),
            new SuchfeldFeldResponse("feld", "Message.Status", true),
            new SuchfeldFeldResponse("feld", "Message.VFN", false));
  }

  /**
   * <b>Ein unbekannter Typ ist kein Angebot.</b> {@code NULL} oder ein dritter Wert: Fuer beide
   * steht weder Spaltenpraedikat noch EAV-Zugriff fest, und geraten wird nicht (Regel Q4).
   */
  @Test
  @DisplayName("NULL und ein dritter Typ werden nicht angeboten")
  void unbekannter_typ_wird_nicht_angeboten() {
    gib(
        List.of(),
        List.of(
            new SuchfeldZeile("Message.GUID", null),
            new SuchfeldZeile("Message.Irgendwas", 2),
            new SuchfeldZeile("Message.SNDPRN", 1)));

    SuchfelderResponse antwort = service().angebot(MANDANT);

    assertThat(antwort.felder())
        .containsExactly(new SuchfeldFeldResponse("feld", "Message.SNDPRN", false));
  }

  /** <b>Der Name bleibt, wie er konfiguriert ist</b> (E‑105) — auch in seiner Schreibweise. */
  @Test
  @DisplayName("Der Name wird unveraendert weitergegeben, auch bei abweichender Schreibweise")
  void der_name_bleibt_wie_konfiguriert() {
    gib(List.of(), List.of(new SuchfeldZeile("message.status", 0)));

    SuchfelderResponse antwort = service().angebot(MANDANT);

    assertThat(antwort.felder())
        .as("gefunden ueber die Kollation, gemeldet wie konfiguriert")
        .containsExactly(new SuchfeldFeldResponse("feld", "message.status", true));
  }
}
