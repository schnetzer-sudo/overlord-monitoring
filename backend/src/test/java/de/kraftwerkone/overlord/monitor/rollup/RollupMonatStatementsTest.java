package de.kraftwerkone.overlord.monitor.rollup;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
 * Die Statements der <b>Monatsebene</b> (Schritt 10b-2), gerendert statt nachgebildet.
 *
 * <p>Dieselbe Bauform wie {@code RollupTagStatementsTest} eine Ebene tiefer, und aus demselben
 * Grund: {@link StatementType#STATIC_STATEMENT}, damit die Grenzen im Text stehen und nicht als
 * {@code ?}. Ein Test, der nur Fragezeichen sieht, kann den Unterschied zwischen dem Fenster des
 * Laufs und den <b>ganzen Monaten</b> nicht pruefen — und genau der ist hier der Punkt.
 *
 * <p><b>Die Reihenfolge aller sechs Statements steht nicht hier</b>, sondern in {@code
 * RollupTagStatementsTest}: Sie ist eine Eigenschaft des ganzen Laufs und gehoert an eine Stelle,
 * nicht an zwei.
 */
class RollupMonatStatementsTest {

  /** Ein Delta-Fenster von zwei Stunden, mitten im Monat. */
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
   * <b>Der Kern: Die Monatsebene liest aus der Tagesebene, nicht aus der Stundenebene und nicht aus
   * {@code Message}.</b>
   *
   * <p>Stuende hier {@code GlassfishDB}, waere es eine zweite Quelllesung — teuer und, schlimmer,
   * eine zweite Wahrheit. Stuende hier {@code message_rollup}, waere es dieselbe Zahl fuer die
   * 2,73-fache Lesearbeit (M87, Variante 1 gegen Variante 3).
   */
  @Test
  @DisplayName("Die Monatsebene wird aus message_rollup_tag abgeleitet, nie aus GlassfishDB")
  void monatsebene_kommt_aus_der_tagesebene() {
    repository.ersetzeFenster(ZWEI_STUNDEN, List.of());

    assertThat(enthaelt("insert into `overlord_monitor`.`message_rollup_monat`"))
        .contains("from `overlord_monitor`.`message_rollup_tag`")
        .doesNotContain("`overlord_monitor`.`message_rollup` ")
        .doesNotContain("GlassfishDB");
    assertThat(gerendert).noneSatisfy(sql -> assertThat(sql).contains("GlassfishDB"));
  }

  /**
   * <b>Ganze Monate, nicht das Fenster.</b> Ein Monatseimer ist die Summe seiner Tageseimer; aus
   * einem Zwei-Stunden-Fenster gerechnet truege er zwei Stunden und behauptete, ein Monat zu sein.
   */
  @Test
  @DisplayName("Gerechnet wird ueber ganze Monate und nicht ueber das Fenster des Laufs")
  void ganze_monate_und_nicht_das_fenster() {
    repository.ersetzeFenster(ZWEI_STUNDEN, List.of());

    assertThat(enthaelt("insert into `overlord_monitor`.`message_rollup_monat`"))
        .as("Vom Monatsersten bis zum naechsten Monatsersten")
        .contains("date '2025-12-01'")
        .contains("date '2026-01-01'")
        .as("Und ausdruecklich nicht die Stunden des Fensters")
        .doesNotContain("03:00")
        .doesNotContain("05:00");
    assertThat(enthaelt("delete from `overlord_monitor`.`message_rollup_monat`"))
        .contains("date '2025-12-01'");
  }

  /**
   * <b>Der Monat entsteht als {@code DATE(DATE_FORMAT(tag, '%Y-%m-01'))} — und der Ausdruck steht
   * im {@code GROUP BY} voll ausgeschrieben.</b>
   *
   * <p>Zwei Dinge in einem Test, weil sie zwei Seiten derselben Sache sind. <b>Das umschliessende
   * {@code DATE(…)}</b> ist kein Beiwerk: Ohne es stuende eine Zeichenkette in einer {@code
   * DATE}-Spalte, und MariaDB wandelte sie still um. <b>Der volle Ausdruck im {@code GROUP BY}</b>
   * ist die Lehre aus Befund 11 der Vorrunde: Hiesse ein Alias wie eine Tabellenspalte, baende
   * MariaDB still an die Spalte — und bei zwei von drei Mandanten saehe das Ergebnis trotzdem
   * richtig aus.
   */
  @Test
  @DisplayName("Monat entsteht als date(date_format(tag)) — auch im GROUP BY")
  void monat_entsteht_als_date_von_date_format() {
    repository.ersetzeFenster(ZWEI_STUNDEN, List.of());

    String einfuegen = enthaelt("insert into `overlord_monitor`.`message_rollup_monat`");
    assertThat(einfuegen)
        .contains("date(date_format(`overlord_monitor`.`message_rollup_tag`.`tag`, '%Y-%m-01'))");
    assertThat(einfuegen)
        .as("Der volle Ausdruck im GROUP BY, nicht der Alias")
        .contains(
            "group by date(date_format(`overlord_monitor`.`message_rollup_tag`.`tag`,"
                + " '%Y-%m-01'))");
  }

  /** Geloescht und neu geschrieben, nie hochgezaehlt — wie auf beiden Ebenen darunter. */
  @Test
  @DisplayName("Kein Hochzaehlen auf der Monatsebene")
  void kein_hochzaehlen() {
    repository.ersetzeFenster(
        ZWEI_STUNDEN,
        List.of(new RollupZeile(LocalDateTime.parse("2025-12-30T03:00"), "p", "FINISHED", 7)));

    assertThat(gerendert)
        .noneSatisfy(sql -> assertThat(sql.toLowerCase()).contains("on duplicate key"));
    assertThat(enthaelt("insert into `overlord_monitor`.`message_rollup_monat`"))
        .doesNotContain("`anzahl` +");
  }

  /**
   * <b>Ein Fenster ueber den Monatswechsel beruehrt zwei Monate</b> — und raeumt und schreibt
   * beide. Der praktische Fall ist der Delta-Lauf am Monatsersten um 00:05: Sein Rueckgriff von
   * fuenfzehn Minuten liegt im Vormonat.
   */
  @Test
  @DisplayName("Ein Fenster ueber den Monatswechsel raeumt und schreibt beide Monate")
  void fenster_ueber_den_monatswechsel_beruehrt_zwei_monate() {
    repository.ersetzeFenster(
        new RollupFenster(
            LocalDateTime.parse("2025-11-30T23:00"), LocalDateTime.parse("2025-12-01T01:00")),
        List.of());

    assertThat(enthaelt("delete from `overlord_monitor`.`message_rollup_monat`"))
        .contains("date '2025-11-01'")
        .contains("date '2025-12-01'");
    assertThat(enthaelt("insert into `overlord_monitor`.`message_rollup_monat`"))
        .as("Gerechnet wird ueber beide Monate ganz")
        .contains("date '2025-11-01'")
        .contains("date '2026-01-01'");
  }

  /**
   * <b>Ein Fenster, das genau am Monatsersten um Mitternacht endet, beruehrt den neuen Monat
   * nicht.</b> {@code bis} ist ausschliessend; der Dezember faengt erst dort an, wo das Fenster
   * aufhoert. Ohne diese Unterscheidung raeumte der Lauf am Monatsersten einen Monatseimer aus, den
   * er anschliessend nicht neu schreibt.
   */
  @Test
  @DisplayName("Ein Fenster bis zum Monatsersten um Mitternacht laesst den neuen Monat in Ruhe")
  void bis_ist_ausschliessend() {
    repository.ersetzeFenster(
        new RollupFenster(
            LocalDateTime.parse("2025-11-30T23:00"), LocalDateTime.parse("2025-12-01T00:00")),
        List.of());

    assertThat(enthaelt("delete from `overlord_monitor`.`message_rollup_monat`"))
        .contains("date '2025-11-01'")
        .doesNotContain("date '2025-12-01'");
  }

  /** Ein leeres Fenster fasst keine der abgeleiteten Ebenen an. */
  @Test
  @DisplayName("Ein leeres Fenster laesst die Monatsebene in Ruhe")
  void leeres_fenster_laesst_die_monatsebene_in_ruhe() {
    LocalDateTime punkt = LocalDateTime.parse("2025-12-30T03:00");

    RollupZeilenzahlen zahlen =
        repository.ersetzeFenster(new RollupFenster(punkt, punkt), List.of());

    assertThat(zahlen.monatszeilen()).isZero();
    assertThat(gerendert).noneSatisfy(sql -> assertThat(sql).contains("`message_rollup_monat`"));
  }
}
