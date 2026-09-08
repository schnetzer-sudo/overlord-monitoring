package de.kraftwerkone.overlord.monitor.bam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.kraftwerkone.overlord.monitor.common.error.FachlicheAusnahme;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Die Parameterpruefung der Property-Suche — <b>ohne Datenbank</b>: der Feldbegriff und das
 * Zusammenspiel beider Begriffsarten im Filter. Was {@code BamSuchfilterTest} fuer die BAM-Begriffe
 * festhaelt, gilt unveraendert.
 *
 * <p>Alle Pruefwerte sind erfunden (Regel G1).
 */
class FeldSuchfilterTest {

  private static final Clock UHR =
      Clock.fixed(Instant.parse("2026-03-15T12:00:00Z"), ZoneId.of("Europe/Berlin"));

  private static BamSuchfilter filter(List<String> begriffe, List<String> felder) {
    return BamSuchfilter.aus(begriffe, felder, null, null, null, UHR);
  }

  private static void weistAb(ThrowingCallable aufruf, String problemTyp) {
    assertThatThrownBy(aufruf)
        .isInstanceOf(FachlicheAusnahme.class)
        .extracting(fehler -> ((FachlicheAusnahme) fehler).problemTyp())
        .isEqualTo(problemTyp);
  }

  // ─── Der Feldbegriff ──────────────────────────────────────────────────────────

  /** Geteilt am <b>ersten</b> Doppelpunkt — alles dahinter ist Wert. */
  @Test
  @DisplayName("Ein Feldbegriff wird am ersten Doppelpunkt geteilt, Raender beschnitten")
  void teilung_am_ersten_doppelpunkt() {
    Feldbegriff begriff = Feldbegriff.ausParameter(" Message.GUID : FILESTORE|a:b:c ");

    assertThat(begriff.name()).isEqualTo("Message.GUID");
    assertThat(begriff.wert()).isEqualTo("FILESTORE|a:b:c");
  }

  /** <b>Der Feldname ist Pflicht</b> (E‑100) — {@code :4711} ist kein Begriff ohne Feld. */
  @Test
  @DisplayName("Ein leerer Feldname ist 400 feldname-fehlt")
  void leerer_name_ist_400() {
    weistAb(() -> Feldbegriff.ausParameter(":4711"), "feldname-fehlt");
    weistAb(() -> Feldbegriff.ausParameter("  :4711"), "feldname-fehlt");
  }

  /** Ohne Trenner gibt es nichts zu teilen — und nichts zu raten. */
  @Test
  @DisplayName("Ein Feldbegriff ohne Doppelpunkt ist 400 feldbegriff-ohne-trenner")
  void ohne_trenner_ist_400() {
    weistAb(() -> Feldbegriff.ausParameter("Message.GUID"), "feldbegriff-ohne-trenner");
  }

  /** Ein leerer Wert ist keine Suche nach dem leeren Wert — dieselbe Regel wie bei {@code :}. */
  @Test
  @DisplayName("Ein leerer Wert ergibt keinen Begriff")
  void leerer_wert_faellt_weg() {
    assertThat(Feldbegriff.ausParameter("Message.GUID:")).isNull();
    assertThat(Feldbegriff.ausParameter("Message.GUID:   ")).isNull();
  }

  // ─── Das Zusammenspiel im Filter ──────────────────────────────────────────────

  /** Eine Suche allein ueber Felder ist zulaessig; die BAM-Liste bleibt leer. */
  @Test
  @DisplayName("Allein ueber Felder: begriffe leer, felder gefuellt")
  void allein_ueber_felder() {
    BamSuchfilter filter = filter(null, List.of("Message.GUID:abc", "Message.Status:FINISHED"));

    assertThat(filter.begriffe()).isEmpty();
    assertThat(filter.felder())
        .containsExactly(
            new Feldbegriff("Message.GUID", "abc"), new Feldbegriff("Message.Status", "FINISHED"));
  }

  /** Ohne jeden Begriff — auch wenn ein Feld mit leerem Wert genannt ist — {@code 400}. */
  @Test
  @DisplayName("Weder Belegnummer noch brauchbares Feld: 400 suchbegriff-fehlt")
  void ohne_jeden_begriff_ist_400() {
    weistAb(() -> filter(null, null), "suchbegriff-fehlt");
    weistAb(() -> filter(List.of(), List.of()), "suchbegriff-fehlt");
    weistAb(() -> filter(List.of(":"), List.of("Message.GUID:")), "suchbegriff-fehlt");
    weistAb(() -> filter(null, List.of("", "  ")), "suchbegriff-fehlt");
  }

  /** Das Gelaender zaehlt beide Arten zusammen: acht sind erlaubt, neun nicht. */
  @Test
  @DisplayName("Belegnummern und Felder zusammen: acht erlaubt, neun sind 400")
  void gelaender_zaehlt_beide_arten() {
    List<String> vierBam = Collections.nCopies(4, ":4711");
    List<String> vierFeld = Collections.nCopies(4, "Message.GUID:a");
    assertThat(filter(vierBam, vierFeld).felder()).hasSize(4);

    List<String> achtFeld = Collections.nCopies(8, "Message.GUID:a");
    assertThat(filter(null, achtFeld).felder()).hasSize(8);
    weistAb(() -> filter(List.of(":4711"), achtFeld), "zu-viele-suchbegriffe");
  }

  /** Der Modus bleibt eine Sache der BAM-Begriffe; der Filter traegt ihn unveraendert. */
  @Test
  @DisplayName("modus=praefix wird mit Feldbegriffen angenommen und gilt fuer die BAM-Begriffe")
  void praefixmodus_neben_feldern() {
    BamSuchfilter filter =
        BamSuchfilter.aus(List.of(":4711"), List.of("Message.GUID:a"), null, null, "praefix", UHR);

    assertThat(filter.modus()).isEqualTo(Suchmodus.PRAEFIX);
    assertThat(filter.felder()).hasSize(1);
  }

  /** Die alte Signatur ist die neue mit leerer Feldliste. */
  @Test
  @DisplayName("Die Signatur ohne feld ergibt eine leere Feldliste")
  void alte_signatur_ohne_felder() {
    BamSuchfilter filter = BamSuchfilter.aus(List.of(":4711"), null, null, null, UHR);

    assertThat(filter.felder()).isEmpty();
    assertThat(filter.begriffe()).hasSize(1);
  }

  /** Ein Fehler in einem Feldbegriff ist der Fehler des Endpunkts, nicht ein stilles Weglassen. */
  @Test
  @DisplayName("Ein fehlerhafter Feldbegriff neben gueltigen Belegnummern ist 400")
  void fehlerhaftes_feld_neben_gueltigen_begriffen() {
    weistAb(() -> filter(List.of(":4711"), List.of("ohnetrenner")), "feldbegriff-ohne-trenner");
    weistAb(() -> filter(List.of(":4711"), List.of(":wert")), "feldname-fehlt");
  }
}
