package de.kraftwerkone.overlord.monitor.message;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.common.MessageStatusClassifier;
import de.kraftwerkone.overlord.monitor.common.Seitenposition;
import de.kraftwerkone.overlord.monitor.common.Zeitfenster;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.Settings;
import org.jooq.conf.StatementType;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockDataProvider;
import org.jooq.tools.jdbc.MockResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Das Statement der Nachrichtenliste, <b>gerendert statt nachgebildet</b> — ohne Datenbank.
 *
 * <p><b>Der Anlass war der Parameter {@code ueberfaellig} aus Schritt 10b-1</b>: Er ist gemessen
 * worden (M97 und {@code docs/nachrichtenliste.md} §5b), <b>bevor</b> er gebaut war. Regel L7
 * verlangt die Messung <b>der</b> Abfrage, und dieser Test hielt fest, dass der Code Zeichen fuer
 * Zeichen dieselbe Bedingung schickt.
 *
 * <p><b>Der Parameter ist am 03.09.2026 mit E-71 entfallen</b>, und mit ihm drei der vier Faelle.
 * <b>Die Klasse bleibt trotzdem, und ihr wichtigster Fall ist jetzt ein Verbot:</b> dass im
 * Statement der Liste <i>keine</i> Frist mehr steht. Ein uebriggebliebenes {@code date_add} waere
 * die widerlegte Kategorie an einer Stelle, an der niemand sie mehr vermutet — und es zoege den
 * Treiber der Liste still von {@code MessageLastUpdateIDX} auf {@code MessageStatusIDX}.
 *
 * <p><b>{@link StatementType#STATIC_STATEMENT}</b>, damit die Werte im Text stehen und nicht als
 * {@code ?}: Ein Test, der nur Fragezeichen sieht, kann den Stichtag nicht pruefen. Im Betrieb
 * laeuft dieselbe Abfrage als vorbereitetes Statement; am Text aendert das nur die Bindeform.
 *
 * <p>Vorbild ist {@code RollupStatementsTest}; die Bauform ist dieselbe.
 */
class NachrichtenStatementsTest {

  private static final MandantContext MANDANT = new MandantContext("NEXANS");

  /** Der Anker der Anwendungsuhr im Profil {@code dev} — derselbe Stichtag wie in M97. */
  private static final LocalDateTime JETZT = LocalDateTime.parse("2025-12-30T04:09:47");

  private static final Zeitfenster FENSTER =
      new Zeitfenster(LocalDateTime.parse("2025-11-30T04:09:47"), JETZT);

  private final List<String> gerendert = new ArrayList<>();
  private NachrichtenRepository repository;

  @BeforeEach
  void attrappeAufbauen() {
    gerendert.clear();
    MockDataProvider attrappe =
        ausfuehrung -> {
          gerendert.add(ausfuehrung.sql());
          DSLContext leer = DSL.using(SQLDialect.MARIADB);
          return new MockResult[] {new MockResult(0, leer.newResult())};
        };
    DSLContext attrappenKontext =
        DSL.using(
            new MockConnection(attrappe),
            SQLDialect.MARIADB,
            new Settings().withStatementType(StatementType.STATIC_STATEMENT));
    repository = new NachrichtenRepository(attrappenKontext, new MessageStatusClassifier());
  }

  private String einziges() {
    assertThat(gerendert).hasSize(1);
    return gerendert.getFirst().replaceAll("\\s+", " ").trim();
  }

  private Nachrichtenabfrage abfrage(Seitenposition cursor) {
    return new Nachrichtenabfrage(FENSTER, Set.of(), List.of(), null, true, cursor, 50);
  }

  /**
   * <b>Der Waechter ueber E-71.</b> {@code MessageTimeout} kommt in der Spaltenliste der Liste gar
   * nicht vor und {@code date_add} nirgends sonst — beide Woerter koennen nur aus der entfallenen
   * Ueberfaelligkeitsbedingung stammen.
   */
  @Test
  @DisplayName("Das Statement der Liste rechnet mit keiner Frist mehr")
  void keine_ueberfaelligkeitsbedingung_mehr() {
    repository.finde(MANDANT, abfrage(null));

    assertThat(einziges())
        .doesNotContain("MessageTimeout")
        .doesNotContain("date_add")
        .doesNotContain("in ('RUNNING', 'SUSPENDED')");
  }

  /**
   * <b>Und die Gegenprobe, ohne die das Verbot oben nichts wert waere:</b> Das Statement steht
   * ueberhaupt und traegt sein Pflicht-Zeitfenster (Regel L1) und die Mandantenkette (Regel M3).
   * Ohne sie bezeugte der Test nur, dass irgendein Text ohne {@code date_add} herauskommt.
   */
  @Test
  @DisplayName("Zeitfenster und Mandantenkette stehen weiterhin im Statement")
  void fenster_und_kette_stehen_da() {
    repository.finde(MANDANT, abfrage(null));

    assertThat(einziges())
        .contains("`GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp")
        .contains("`GlassfishDB`.`Message`.`MessageLastUpdate` <= timestamp")
        .contains("exists (select 1 as `one`");
  }

  /**
   * <b>Der Cursor, und er ist seit E-71 wieder der Normalfall.</b> Neben der Ueberfaelligkeitsform
   * wirkte er <i>anders</i> — der Plan nutzte ihn nicht mehr als Indexbereich ({@code
   * docs/nachrichtenliste.md} §5b). Die Form ist entfallen; die Liste hat wieder genau einen
   * Zugriffspfad, und die Cursor-Messung aus M4/L8 gilt fuer jede Fassung.
   */
  @Test
  @DisplayName("Der Cursor filtert ueber beide Schluessel, mit dem Tiebreaker auf der MessageID")
  void cursor_filtert_ueber_beide_schluessel() {
    repository.finde(
        MANDANT, abfrage(new Seitenposition(LocalDateTime.parse("2025-12-24T06:19:16"), "abc")));

    assertThat(einziges())
        .contains(
            "`GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-24 06:19:16.0'"
                + " or (`GlassfishDB`.`Message`.`MessageLastUpdate`"
                + " = timestamp '2025-12-24 06:19:16.0'"
                + " and `GlassfishDB`.`Message`.`MessageID` < 'abc')");
  }
}
