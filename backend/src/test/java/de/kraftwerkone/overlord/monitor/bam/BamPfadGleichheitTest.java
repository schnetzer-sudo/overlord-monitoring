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
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockDataProvider;
import org.jooq.tools.jdbc.MockResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * <b>Der Gleichheitstest des BAM-Pfads</b>: Ohne den Parameter {@code feld} verhaelt sich die Suche
 * <b>Zeichen fuer Zeichen</b> wie vor der Property-Suche — dieselbe Zusage wie beim Praefixmodus
 * ({@code docs/bam-suche.md} §15), diesmal am eingefrorenen Text festgemacht.
 *
 * <p><b>Der Sollwert ist kein Nachbau, sondern ein Abzug.</b> Die drei Dateien unter {@code
 * src/test/resources/bam/} sind am 08.09.2026 <i>vor</i> der ersten Codeaenderung aus dem damaligen
 * Repository gerendert worden — Statementtext und Bindewerte, so wie jOOQ sie an den Treiber gibt.
 * Wer den Kern umbaut und den BAM-Pfad dabei auch nur um ein Leerzeichen veraendert, sieht es hier
 * — und muss dann entweder die Aenderung begruenden und den Abzug neu ziehen oder den Umbau
 * zuruecknehmen.
 *
 * <p>Dazu die Zusage auf der Ebene des Service: Ohne Feldbegriffe geht der Aufruf den Weg von Teil
 * 2b und nicht den neuen mit leerer Liste — dass beide dasselbe rendern, prueft der dritte Fall.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BamPfadGleichheitTest {

  private static final MandantContext MANDANT = new MandantContext("NEXANS");
  private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");

  private static final Zeitfenster FENSTER =
      new Zeitfenster(
          LocalDateTime.parse("2025-11-30T00:00:00"), LocalDateTime.parse("2025-12-30T00:00:00"));

  private static final Suchbedingung EINS =
      new Suchbedingung(null, List.of("1234567", "0001234567"), Suchmodus.EXAKT);
  private static final Suchbedingung ZWEI =
      new Suchbedingung((short) 9018, List.of("7654321"), Suchmodus.EXAKT);
  private static final Suchbedingung PRAEFIX =
      new Suchbedingung(null, List.of("1234567"), Suchmodus.PRAEFIX);

  private final List<String> gerendert = new ArrayList<>();
  private BamSucheRepository repository;

  @Mock private BamSucheRepository attrappe;

  @BeforeEach
  void attrappeAufbauen() {
    gerendert.clear();
    MockDataProvider provider =
        ausfuehrung -> {
          gerendert.add(
              ausfuehrung.sql() + "\n-- bindings: " + Arrays.asList(ausfuehrung.bindings()));
          DSLContext leer = DSL.using(SQLDialect.MARIADB);
          Field<Integer> platzhalter = DSL.field("platzhalter", Integer.class);
          Result<Record1<Integer>> ergebnis = leer.newResult(platzhalter);
          return new MockResult[] {new MockResult(0, ergebnis)};
        };
    repository =
        new BamSucheRepository(DSL.using(new MockConnection(provider), SQLDialect.MARIADB));
  }

  private static String abzug(String datei) throws IOException {
    try (InputStream quelle = BamPfadGleichheitTest.class.getResourceAsStream("/bam/" + datei)) {
      assertThat(quelle).as("Abzug %s fehlt unter src/test/resources/bam", datei).isNotNull();
      return new String(quelle.readAllBytes(), StandardCharsets.UTF_8).strip();
    }
  }

  private String letztes() {
    return gerendert.getLast().strip();
  }

  // ─── Der eingefrorene Text ────────────────────────────────────────────────────

  /** Zwei exakte Begriffe, einer davon mit Typ — der Normalfall der Verundung. */
  @Test
  @DisplayName("Zwei exakte Begriffe rendern Zeichen fuer Zeichen wie vor der Property-Suche")
  void zwei_begriffe_exakt_unveraendert() throws IOException {
    repository.findeTreffer(MANDANT, List.of(EINS, ZWEI), FENSTER);

    assertThat(letztes()).isEqualTo(abzug("bam-suche-treffer-zwei-begriffe-exakt.sql"));
  }

  /** Ein Begriff im Praefixmodus — der Pfad aus Teil 4. */
  @Test
  @DisplayName("Ein Praefixbegriff rendert Zeichen fuer Zeichen wie vor der Property-Suche")
  void ein_begriff_praefix_unveraendert() throws IOException {
    repository.findeTreffer(MANDANT, List.of(PRAEFIX), FENSTER);

    assertThat(letztes()).isEqualTo(abzug("bam-suche-treffer-ein-begriff-praefix.sql"));
  }

  /** Abfrage (b), die Trefferwerte — sie ist nicht angefasst worden, und das steht hier fest. */
  @Test
  @DisplayName("Die Trefferwerte-Abfrage rendert Zeichen fuer Zeichen wie vor der Property-Suche")
  void trefferwerte_unveraendert() throws IOException {
    repository.findeTrefferWerte(MANDANT, List.of("a", "b"), List.of(EINS, ZWEI));

    assertThat(letztes()).isEqualTo(abzug("bam-suche-trefferwerte-zwei-begriffe.sql"));
  }

  /**
   * <b>Die neue Signatur mit leerer Feldliste rendert dasselbe wie die alte.</b> Damit haengt die
   * Gleichheit nicht daran, welche der beiden Methoden ein Aufrufer waehlt.
   */
  @Test
  @DisplayName("Die neue Signatur mit leerer Feldliste rendert denselben Text wie die alte")
  void leere_feldliste_ist_derselbe_text() {
    repository.findeTreffer(MANDANT, List.of(EINS, ZWEI), FENSTER);
    String alt = letztes();
    repository.findeTreffer(MANDANT, List.of(EINS, ZWEI), List.of(), FENSTER);
    String neu = letztes();

    assertThat(neu).isEqualTo(alt);
  }

  // ─── Die Zusage auf der Ebene des Service ─────────────────────────────────────

  /**
   * <b>Ohne Feldbegriffe ruft der Service den Weg von Teil 2b</b> — nicht die neue Methode mit
   * leerer Liste. Das ist die Zusage „ohne den Parameter unveraendert" auf der Ebene, auf der der
   * Controller sie sieht.
   */
  @Test
  @DisplayName("Ohne feld ruft der Service den Pfad von Teil 2b und nie den neuen")
  void ohne_feld_der_alte_pfad() {
    when(attrappe.findeSollaengen(any())).thenReturn(List.of());
    when(attrappe.findeTreffer(any(), anyList(), any())).thenReturn(List.of());
    when(attrappe.findeTrefferWerte(any(), anyList(), anyList())).thenReturn(List.of());
    BamSucheService service =
        new BamSucheService(
            attrappe, new MessageStatusClassifier(), Clock.fixed(Instant.EPOCH, ZONE));
    BamSuchfilter filter =
        new BamSuchfilter(List.of(new Suchbegriff(null, "1234567")), FENSTER, Suchmodus.EXAKT);

    BamSucheResponse antwort = service.suche(MANDANT, filter);

    verify(attrappe).findeTreffer(any(), anyList(), any());
    verify(attrappe, never()).findeTreffer(any(), anyList(), anyList(), any());
    assertThat(antwort.felder()).as("leer statt fehlend").isEmpty();
  }
}
