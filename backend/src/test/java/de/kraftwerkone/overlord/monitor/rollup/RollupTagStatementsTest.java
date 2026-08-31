package de.kraftwerkone.overlord.monitor.rollup;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
 * Die Statements der <b>Tagesebene</b> (Schritt 10b-1, Teil C), gerendert statt nachgebildet.
 *
 * <p>Gegenstueck zu {@code RollupStatementsTest}, der dasselbe fuer die Leseseite tut. Hier geht es
 * um die Statements, die {@code RollupSchreibRepository.ersetzeFenster} schickt — und besonders um
 * das eine, das die Tagesebene aus der Stundenebene ableitet. <b>Seit Schritt 10b-2 sind es sechs
 * statt vier</b>; die Reihenfolge <b>aller</b> sechs steht hier, die Monatsebene und ihr eigener
 * Ausdruck in {@code RollupMonatStatementsTest}.
 *
 * <p><b>{@link StatementType#STATIC_STATEMENT}</b>, damit die Grenzen im Text stehen und nicht als
 * {@code ?}: Ein Test, der nur Fragezeichen sieht, kann den Unterschied zwischen dem Fenster des
 * Laufs und den <b>ganzen Tagen</b> nicht pruefen — und genau der ist hier der Punkt.
 */
class RollupTagStatementsTest {

  /** Ein Delta-Fenster von zwei Stunden, mitten am Tag. */
  private static final RollupFenster ZWEI_STUNDEN =
      new RollupFenster(
          LocalDateTime.parse("2025-12-30T03:00"), LocalDateTime.parse("2025-12-30T05:00"));

  private final List<String> gerendert = new ArrayList<>();
  private RollupSchreibRepository repository;

  @BeforeEach
  void attrappeAufbauen() {
    gerendert.clear();
    MockDataProvider attrappe =
        ausfuehrung -> {
          gerendert.add(ausfuehrung.sql());
          DSLContext leer = DSL.using(SQLDialect.MARIADB);
          return new MockResult[] {new MockResult(0, leer.newResult())};
        };
    repository =
        new RollupSchreibRepository(
            DSL.using(
                new MockConnection(attrappe),
                SQLDialect.MARIADB,
                new Settings().withStatementType(StatementType.STATIC_STATEMENT)));
  }

  private String enthaelt(String teil) {
    List<String> treffer =
        gerendert.stream()
            .map(s -> s.replaceAll("\\s+", " ").trim())
            .filter(s -> s.contains(teil))
            .toList();
    assertThat(treffer).as("Genau ein Statement mit '%s'", teil).hasSize(1);
    return treffer.getFirst();
  }

  /**
   * <b>Sechs Statements, in dieser Reihenfolge</b> — und die Reihenfolge ist nicht beliebig: Jede
   * Ebene wird aus der naechstfeineren gerechnet, also muss die naechstfeinere vorher stehen. Die
   * Tagesebene aus der Stundenebene, die Monatsebene aus der Tagesebene.
   *
   * <p><b>Genau dieser Test faellt, wenn jemand die Monatsebene vor die Tagesebene zieht</b> — und
   * der Fehler waere sonst still: Die Monatsebene truege den Stand von vor diesem Lauf, und eine
   * Summenprobe ueber ein einzelnes Fenster faende das nicht.
   */
  @Test
  @DisplayName("Ein Lauf schickt sechs Statements: dreimal loeschen, dreimal einfuegen")
  void sechs_statements_in_dieser_reihenfolge() {
    repository.ersetzeFenster(
        ZWEI_STUNDEN,
        List.of(new RollupZeile(LocalDateTime.parse("2025-12-30T03:00"), "p", "FINISHED", 7)));

    List<String> knapp =
        gerendert.stream()
            .map(s -> s.replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT))
            .toList();
    assertThat(knapp).hasSize(6);
    assertThat(knapp.get(0)).startsWith("delete from `overlord_monitor`.`message_rollup` ");
    assertThat(knapp.get(1)).startsWith("insert into `overlord_monitor`.`message_rollup` ");
    assertThat(knapp.get(2)).startsWith("delete from `overlord_monitor`.`message_rollup_tag` ");
    assertThat(knapp.get(3)).startsWith("insert into `overlord_monitor`.`message_rollup_tag` ");
    assertThat(knapp.get(4)).startsWith("delete from `overlord_monitor`.`message_rollup_monat` ");
    assertThat(knapp.get(5)).startsWith("insert into `overlord_monitor`.`message_rollup_monat` ");
  }

  /**
   * <b>Der Kern: Die Tagesebene liest aus der Stundenebene, nicht aus {@code Message}.</b> Stuende
   * hier {@code GlassfishDB}, waere es eine zweite Quelllesung — teuer und, schlimmer, eine zweite
   * Wahrheit.
   */
  @Test
  @DisplayName("Die Tagesebene wird aus message_rollup abgeleitet, nie aus GlassfishDB")
  void tagesebene_kommt_aus_der_stundenebene() {
    repository.ersetzeFenster(ZWEI_STUNDEN, List.of());

    String einfuegen = enthaelt("insert into `overlord_monitor`.`message_rollup_tag`");

    assertThat(einfuegen).contains("from `overlord_monitor`.`message_rollup`");
    for (String statement : gerendert) {
      assertThat(statement)
          .as("Der Schreib-Kontext darf ausschliesslich overlord_monitor (PROJEKTBESCHREIBUNG §6)")
          .doesNotContain("GlassfishDB");
    }
  }

  /**
   * <b>Der Tageseimer wird ueber den GANZEN Tag gerechnet, nicht ueber das Fenster.</b> Ein
   * Zwei-Stunden-Fenster am 30.12. muss den Tageseimer aus allen 24 Stunden des 30.12. bilden —
   * sonst truege er zwei Stunden und behauptete, ein Tag zu sein.
   */
  @Test
  @DisplayName("Gerechnet wird ueber ganze Tage, nicht ueber das Fenster des Laufs")
  void ganze_tage_und_nicht_das_fenster() {
    repository.ersetzeFenster(ZWEI_STUNDEN, List.of());

    String einfuegen = enthaelt("insert into `overlord_monitor`.`message_rollup_tag`");

    assertThat(einfuegen)
        .as("Der Tag beginnt um Mitternacht und endet am naechsten Mitternacht")
        .contains("timestamp '2025-12-30 00:00:00.0'")
        .contains("timestamp '2025-12-31 00:00:00.0'");
    assertThat(einfuegen)
        .as("Die Grenzen des Laufs haben in diesem Statement nichts zu suchen")
        .doesNotContain("03:00:00")
        .doesNotContain("05:00:00");
    assertThat(enthaelt("delete from `overlord_monitor`.`message_rollup_tag`"))
        .as("Geloescht wird ueber die Tage, nicht ueber Zeitstempel")
        .contains("date '2025-12-30'")
        .doesNotContain("00:00:00");
  }

  /** {@code DATE(stunde)} — Zeichen fuer Zeichen die Form, die M94 gemessen hat. */
  @Test
  @DisplayName("Der Tag entsteht als DATE(stunde), und GROUP BY schreibt den Ausdruck aus")
  void tag_entsteht_als_date_von_stunde() {
    repository.ersetzeFenster(ZWEI_STUNDEN, List.of());

    String einfuegen = enthaelt("insert into `overlord_monitor`.`message_rollup_tag`");

    assertThat(einfuegen).contains("date(`overlord_monitor`.`message_rollup`.`stunde`)");
    assertThat(einfuegen)
        .as(
            "Der volle Ausdruck in GROUP BY, nicht der Alias — die Lehre aus Befund 11: Hiesse ein"
                + " Alias wie eine Tabellenspalte, baende MariaDB still an die Spalte")
        .contains("group by date(`overlord_monitor`.`message_rollup`.`stunde`)");
  }

  /**
   * <b>Geloescht und neu geschrieben, nie hochgezaehlt.</b> Ein {@code ON DUPLICATE KEY UPDATE}
   * liesse einen Tageseimer stehen, den die Stundenebene nicht mehr hergibt — der Fehler waere
   * still und stuende erst im Dashboard.
   */
  @Test
  @DisplayName("Kein ON DUPLICATE KEY UPDATE, in keinem der sechs Statements")
  void kein_hochzaehlen() {
    repository.ersetzeFenster(ZWEI_STUNDEN, List.of());

    for (String statement : gerendert) {
      assertThat(statement.toLowerCase(Locale.ROOT))
          .doesNotContain("on duplicate key")
          .doesNotContain("anzahl` + ");
    }
  }

  /**
   * Ein Fenster ueber eine Mitternachtsgrenze beruehrt <b>zwei</b> Tage — der praktische Fall ist
   * der Delta-Lauf in der ersten Stunde eines Tages, dessen Rueckgriff in den Vortag reicht.
   */
  @Test
  @DisplayName("Ein Fenster ueber Mitternacht raeumt und schreibt beide Tage")
  void fenster_ueber_mitternacht_beruehrt_zwei_tage() {
    repository.ersetzeFenster(
        new RollupFenster(
            LocalDateTime.parse("2025-12-30T23:00"), LocalDateTime.parse("2025-12-31T01:00")),
        List.of());

    assertThat(enthaelt("delete from `overlord_monitor`.`message_rollup_tag`"))
        .contains("date '2025-12-30'")
        .contains("date '2025-12-31'");
    assertThat(enthaelt("insert into `overlord_monitor`.`message_rollup_tag`"))
        .contains("timestamp '2025-12-30 00:00:00.0'")
        .contains("timestamp '2026-01-01 00:00:00.0'");
  }

  /**
   * <b>Ein leeres Fenster fasst die Tagesebene nicht an.</b> Es gibt keinen beruehrten Tag, und ein
   * {@code DELETE} ueber einen leeren Bereich waere zwar folgenlos, aber es waere auch eine
   * Behauptung: dass hier etwas neu gerechnet worden sei.
   */
  @Test
  @DisplayName("Ein leeres Fenster schickt kein Statement an die Tagesebene")
  void leeres_fenster_laesst_die_tagesebene_in_ruhe() {
    LocalDateTime gleich = LocalDateTime.parse("2025-12-30T03:00");
    RollupZeilenzahlen zahlen =
        repository.ersetzeFenster(new RollupFenster(gleich, gleich), List.of());

    assertThat(zahlen.tageszeilen()).isZero();
    for (String statement : gerendert) {
      assertThat(statement).doesNotContain("message_rollup_tag");
    }
  }
}
