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
 * <p>Der Anlass ist der Parameter {@code ueberfaellig} aus Schritt 10b-1: Er ist gemessen worden
 * (M97 und {@code docs/nachrichtenliste.md} §5b), <b>bevor</b> er gebaut war. Regel L7 verlangt die
 * Messung <b>der</b> Abfrage — dieser Test haelt fest, dass der Code Zeichen fuer Zeichen dieselbe
 * Bedingung schickt, die gemessen wurde.
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

  private Nachrichtenabfrage abfrage(boolean ueberfaellig, Seitenposition cursor) {
    return new Nachrichtenabfrage(
        FENSTER, Set.of(), List.of(), null, ueberfaellig, JETZT, true, cursor, 50);
  }

  /**
   * {@code MessageStatus} steht ohne den Parameter nur in der Spaltenliste — die Liste zeigt den
   * Rohwert auf jeder Zeile. Geprueft wird deshalb auf die Bestandteile der <b>Bedingung</b>:
   * {@code MessageTimeout} kommt in der Spaltenliste gar nicht vor, und {@code date_add} nur hier.
   */
  @Test
  @DisplayName("Ohne den Parameter steht keine Ueberfaelligkeitsbedingung im Statement")
  void ohne_parameter_keine_ueberfaelligkeitsbedingung() {
    repository.finde(MANDANT, abfrage(false, null));

    assertThat(einziges())
        .doesNotContain("MessageTimeout")
        .doesNotContain("date_add")
        .doesNotContain("in ('RUNNING', 'SUSPENDED')");
  }

  /**
   * Die Bedingung Zeichen fuer Zeichen. Sie ist das SQL-Gegenstueck zu {@code
   * MessageStatusClassifier.istUeberfaellig} und in M97 in genau dieser Form gemessen worden — dort
   * als {@code MessageLastUpdate + INTERVAL MessageTimeout SECOND}, was MariaDB auf dasselbe {@code
   * date_add} abbildet.
   */
  @Test
  @DisplayName("Mit dem Parameter steht die gemessene Ueberfaelligkeitsbedingung im Statement")
  void mit_parameter_steht_die_gemessene_bedingung_da() {
    repository.finde(MANDANT, abfrage(true, null));

    assertThat(einziges())
        .contains(
            "`GlassfishDB`.`Message`.`MessageStatus` in ('RUNNING', 'SUSPENDED')"
                + " and `GlassfishDB`.`Message`.`MessageTimeout` is not null"
                + " and `GlassfishDB`.`Message`.`MessageTimeout` > 0"
                + " and date_add(`GlassfishDB`.`Message`.`MessageLastUpdate`,"
                + " interval `GlassfishDB`.`Message`.`MessageTimeout` second)"
                + " < timestamp '2025-12-30 04:09:47.0'");
  }

  /**
   * <b>Der Stichtag kommt aus der Anwendungsuhr und nirgendwo sonst.</b> Im Profil {@code dev}
   * liegt sie Monate zurueck; mit der Systemuhr waere lokal jede offene Nachricht ueberfaellig, und
   * der Parameter waere dort ohne Aussage.
   */
  @Test
  @DisplayName("Der Stichtag ist der uebergebene Zeitpunkt und nicht die obere Fenstergrenze")
  void stichtag_ist_die_anwendungsuhr() {
    repository.finde(
        MANDANT,
        new Nachrichtenabfrage(
            new Zeitfenster(
                LocalDateTime.parse("2020-01-01T00:00:00"),
                LocalDateTime.parse("2020-12-31T00:00:00")),
            Set.of(),
            List.of(),
            null,
            true,
            LocalDateTime.parse("2024-06-05T12:00:00"),
            true,
            null,
            50));

    assertThat(einziges())
        .contains("< timestamp '2024-06-05 12:00:00.0'")
        .doesNotContain("< timestamp '2020-12-31 00:00:00.0'");
  }

  /**
   * Der Cursor bleibt neben der Ueberfaelligkeit stehen. Er wirkt dort <b>anders</b> — der Plan
   * nutzt ihn nicht mehr als Indexbereich ({@code docs/nachrichtenliste.md} §5b) —, aber er filtert
   * weiterhin, und genau darauf ruht die Richtigkeit des Blaetterns.
   */
  @Test
  @DisplayName("Cursor und Ueberfaelligkeit stehen beide im Statement")
  void cursor_und_ueberfaelligkeit_zusammen() {
    repository.finde(
        MANDANT,
        abfrage(true, new Seitenposition(LocalDateTime.parse("2025-12-24T06:19:16"), "abc")));

    String sql = einziges();
    assertThat(sql).contains("date_add(").contains("interval");
    assertThat(sql)
        .contains(
            "`GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '2025-12-24 06:19:16.0'"
                + " or (`GlassfishDB`.`Message`.`MessageLastUpdate`"
                + " = timestamp '2025-12-24 06:19:16.0'"
                + " and `GlassfishDB`.`Message`.`MessageID` < 'abc')");
  }
}
