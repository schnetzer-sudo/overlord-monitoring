package de.kraftwerkone.overlord.monitor.bam;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
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

/**
 * Haelt die Regeln des Suchfelder-Statements maschinell fest — <b>ohne Datenbank</b>. Dieselbe
 * Bauform wie {@code BamTypenStatementsTest}.
 *
 * <p><b>Der eine Punkt, der hier mehr Gewicht hat als dort:</b> Der Mandantenfilter hat <i>zwei</i>
 * Zweige. Ein {@code WHERE MandantID = ?} ohne den {@code NULL}-Zweig loeschte acht von vierzehn
 * Eintraegen aus dem Angebot (M161: alle Typ-0-Namen sind global), ein Statement ohne Filter zeigte
 * die Konfiguration fremder Mandanten. Beides sieht in einem gruenen Isolationstest gleich aus,
 * solange nur ein Mandant gebundene Eintraege hat — deshalb steht die Form hier am Text fest.
 */
class SuchfelderStatementsTest {

  private static final MandantContext MANDANT = new MandantContext("NEXANS");

  private record Ausgefuehrt(String sql, List<Object> werte) {}

  private final List<Ausgefuehrt> gerendert = new ArrayList<>();
  private SuchfelderRepository repository;

  @BeforeEach
  void attrappeAufbauen() {
    gerendert.clear();
    MockDataProvider attrappe =
        ausfuehrung -> {
          gerendert.add(new Ausgefuehrt(ausfuehrung.sql(), Arrays.asList(ausfuehrung.bindings())));
          DSLContext leer = DSL.using(SQLDialect.MARIADB);
          Field<Integer> platzhalter = DSL.field("platzhalter", Integer.class);
          Result<Record1<Integer>> ergebnis = leer.newResult(platzhalter);
          return new MockResult[] {new MockResult(0, ergebnis)};
        };
    repository =
        new SuchfelderRepository(DSL.using(new MockConnection(attrappe), SQLDialect.MARIADB));
  }

  private String sql() {
    repository.findeKonfigurierteFelder(MANDANT);
    return gerendert.getLast().sql();
  }

  /**
   * <b>Beide Zweige, verodert, und der Mandant gebunden.</b> {@code NULL} heisst „gilt fuer alle" —
   * eine Auskunft des Auftraggebers, keine Messung — und traegt die Haelfte des Angebots.
   */
  @Test
  @DisplayName("Der Filter ist MandantID IS NULL OR MandantID = ?, und der Wert ist gebunden")
  void der_filter_hat_beide_zweige() {
    String sql = sql();
    String klein = sql.toLowerCase(Locale.ROOT);

    assertThat(klein)
        .as("gerendert: %s", sql)
        .contains("`mandantid` is null")
        .contains(" or ")
        .contains("`mandantid` = ");
    assertThat(gerendert.getLast().werte())
        .as("der Mandant kommt gebunden und nicht als Literal")
        .contains(MANDANT.mandantId());
  }

  /**
   * <b>Keine Existenzfrage ueber den Bestand.</b> Das Angebot sagt, was konfiguriert ist, und
   * behauptet nichts darueber, was vorkommt. Ein Blick in {@code MessageProperty} kostete beim
   * Zaehlen eines einzigen Namens 125,527 s (M157) — und braeuchte ein Zeitfenster, das das Angebot
   * nicht hat (Regel L1).
   */
  @Test
  @DisplayName("Weder Message noch MessageProperty werden angefasst — und nichts wird gejoint")
  void nur_die_konfigurationstabelle() {
    String klein = sql().toLowerCase(Locale.ROOT);

    assertThat(klein)
        .contains("`messagepropertysearchlistentry`")
        .doesNotContain("`messageproperty`")
        .doesNotContain("`message`")
        .doesNotContain("messagelastupdate")
        .doesNotContain("join");
  }

  /**
   * <b>Sortiert nach dem Namen</b> — die Tabelle kennt keine Ordnung (M153, Punkt 147), und eine
   * Antwort, deren Reihenfolge an der Speicherung hinge, zeigte dieselbe Auswahl zweimal
   * verschieden.
   */
  @Test
  @DisplayName("Sortiert wird ueber den Feldnamen")
  void die_ordnung_ist_der_name() {
    String klein = sql().toLowerCase(Locale.ROOT);

    int wo = klein.indexOf("order by");
    assertThat(wo).isGreaterThan(0);
    assertThat(klein.substring(wo)).contains("`messagepropertyname`");
  }

  /** Die {@code MandantID} ist Bedingung und verlaesst das Repository nicht als Spalte. */
  @Test
  @DisplayName("Die MandantID wird nicht ausgewaehlt, nur gefiltert")
  void die_mandantid_ist_keine_auswahlspalte() {
    String klein = sql().toLowerCase(Locale.ROOT);

    String auswahl = klein.substring(0, klein.indexOf(" from "));
    assertThat(auswahl)
        .contains("`messagepropertyname`")
        .contains("`messagepropertytype`")
        .doesNotContain("`mandantid`");
  }
}
