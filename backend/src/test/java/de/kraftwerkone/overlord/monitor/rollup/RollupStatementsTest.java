package de.kraftwerkone.overlord.monitor.rollup;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockDataProvider;
import org.jooq.tools.jdbc.MockResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Die Statements des Rollups, <b>gerendert statt nachgebildet</b>: Das Repository laeuft gegen eine
 * jOOQ-Attrappe, und geprueft wird der Text, der wirklich herausfaellt.
 *
 * <p>Er beantwortet die Frage, die ein Integrationstest nur mittelbar beantwortet: Ist das, was
 * gegen die Testkopie gemessen worden ist (M88), auch das, was der Code schickt? Regel L7 verlangt
 * die Messung <b>der Abfrage</b> — nicht einer aehnlichen.
 *
 * <p>Vorbild ist {@code ProzessKatalogStatementsTest}; die Bauform ist dieselbe.
 */
class RollupStatementsTest {

  private static final LocalDateTime VON = LocalDateTime.parse("2025-12-07T17:00:00");
  private static final LocalDateTime BIS = LocalDateTime.parse("2025-12-07T18:00:00");

  private final List<String> gerendert = new ArrayList<>();
  private final List<Object> bindungen = new ArrayList<>();

  private RollupLeseRepository repository;

  @BeforeEach
  void attrappeAufbauen() {
    gerendert.clear();
    bindungen.clear();
    MockDataProvider attrappe =
        ausfuehrung -> {
          gerendert.add(ausfuehrung.sql());
          if (ausfuehrung.bindings() != null) {
            bindungen.addAll(List.of(ausfuehrung.bindings()));
          }
          DSLContext leer = DSL.using(SQLDialect.MARIADB);
          return new MockResult[] {new MockResult(0, leer.newResult())};
        };
    DSLContext attrappenKontext = DSL.using(new MockConnection(attrappe), SQLDialect.MARIADB);
    repository = new RollupLeseRepository(attrappenKontext);
  }

  private String einziges() {
    assertThat(gerendert).hasSize(1);
    return gerendert.getFirst().replaceAll("\\s+", " ").trim();
  }

  @Test
  @DisplayName("Die Aggregation ist woertlich die gemessene Fassung aus M88")
  void aggregation_ist_die_gemessene_fassung() {
    repository.aggregiere(VON, BIS);

    assertThat(einziges())
        .isEqualTo(
            "select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`,"
                + " '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`,"
                + " `GlassfishDB`.`Message`.`MessageStatus`, count(*) from"
                + " `GlassfishDB`.`Message` where (`GlassfishDB`.`Message`.`MessageLastUpdate` >= ?"
                + " and `GlassfishDB`.`Message`.`MessageLastUpdate` < ?) group by"
                + " date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'),"
                + " `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`");
  }

  @Test
  @DisplayName("Die Fenstergrenzen werden gebunden: unten einschliessend, oben ausschliessend")
  void fenstergrenzen_werden_gebunden() {
    repository.aggregiere(VON, BIS);

    assertThat(bindungen)
        .as(
            "Genau zwei Werte, in der Reihenfolge des Fensters. jOOQ bindet sie als"
                + " java.sql.Timestamp — der Vergleich muss das mitmachen, sonst prueft er nichts")
        .containsExactly(Timestamp.valueOf(VON), Timestamp.valueOf(BIS));
    assertThat(einziges())
        .as(
            "BETWEEN waere beidseitig geschlossen und zaehlte die volle Stunde am oberen Rand ein"
                + " zweites Mal in den naechsten Eimer")
        .doesNotContain("between");
  }

  @Test
  @DisplayName("GROUP BY schreibt den vollen Ausdruck aus und nicht den Alias (Befund 11)")
  void group_by_schreibt_den_vollen_ausdruck_aus() {
    repository.aggregiere(VON, BIS);

    assertThat(einziges())
        .as(
            "In M91 hat GROUP BY an eine gleichnamige Tabellenspalte gebunden statt an den"
                + " Ausdrucksalias, und bei zwei von drei Mandanten sah das Ergebnis trotzdem"
                + " richtig aus. Der volle Ausdruck kann das nicht")
        .contains("group by date_format(");
  }

  @Test
  @DisplayName("Kein STRAIGHT_JOIN, in keiner Fassung (M42)")
  void kein_straight_join() {
    repository.aggregiere(VON, BIS);
    repository.fruehesteAenderung();

    for (String statement : gerendert) {
      assertThat(statement.toLowerCase(Locale.ROOT))
          .as("M42 hat dafuer Faktor 219 bis 1094 gemessen")
          .doesNotContain("straight_join");
    }
  }

  @Test
  @DisplayName("Der fruehester Zeitstempel kommt aus einer Indexspitze, ohne Bedingung")
  void fruehester_zeitstempel_ohne_bedingung() {
    repository.fruehesteAenderung();

    assertThat(einziges())
        .isEqualTo(
            "select min(`GlassfishDB`.`Message`.`MessageLastUpdate`) from `GlassfishDB`.`Message`");
    assertThat(bindungen).as("Nichts zu binden — der Bestand hat genau einen Anfang").isEmpty();
  }

  @Test
  @DisplayName("Beide Statements qualifizieren das Schema voll aus")
  void schema_wird_voll_qualifiziert() {
    repository.aggregiere(VON, BIS);
    repository.fruehesteAenderung();

    for (String statement : gerendert) {
      assertThat(statement)
          .as(
              "Ohne defaultSchema rendert jOOQ voll qualifiziert — nur so funktionieren"
                  + " schemauebergreifende Abfragen (docs/datenzugriff.md §3)")
          .contains("`GlassfishDB`.`Message`");
    }
  }
}
