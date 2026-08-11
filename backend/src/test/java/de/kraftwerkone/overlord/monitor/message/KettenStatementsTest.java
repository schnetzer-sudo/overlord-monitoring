package de.kraftwerkone.overlord.monitor.message;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.common.Seitenposition;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.time.LocalDateTime;
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
 * Haelt die <b>unverhandelbaren Regeln</b> der Ketten-Statements maschinell fest — <b>ohne
 * Datenbank</b>.
 *
 * <p>Der Isolationstest weist die Trennung am laufenden System nach, braucht dafuer aber die
 * Testkopie und ist in der CI ausgeschlossen. Dieser Test schliesst die Luecke von der anderen
 * Seite: Er rendert jedes Statement gegen eine Attrappe und prueft am Text, dass der
 * Mandantenfilter darin steht — <b>auch in den beiden Zaehlungen</b>. Genau dort ist er am
 * leichtesten zu vergessen, weil eine Zahl ohne Filter nicht falsch <i>aussieht</i>.
 *
 * <p>Er ersetzt den Isolationstest nicht — Text ist kein Verhalten. Er faengt das Offensichtliche
 * ab: das vergessene {@code EXISTS}, die fehlende Sortierung und das fehlende {@code LIMIT}.
 */
class KettenStatementsTest {

  private static final MandantContext MANDANT = new MandantContext("NEXANS");
  private static final String MESSAGE_ID = "eine-nachricht";

  /** Ein abgefangenes Statement: der Text und die gebundenen Werte. */
  private record Ausgefuehrt(String sql, List<Object> werte) {}

  private final List<Ausgefuehrt> gerendert = new ArrayList<>();
  private KettenRepository repository;

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
    repository = new KettenRepository(DSL.using(new MockConnection(attrappe), SQLDialect.MARIADB));
  }

  private String letztesSql() {
    return gerendert.getLast().sql();
  }

  /** Alle fuenf Statements einmal ausloesen. */
  private List<String> alleStatements() {
    repository.findeGlied(MANDANT, MESSAGE_ID);
    repository.findeKinder(MANDANT, MESSAGE_ID, null, 50);
    repository.findeMergeEingaenge(MANDANT, MESSAGE_ID, null, 50);
    repository.zaehleKinder(MANDANT, MESSAGE_ID);
    repository.zaehleMergeEingaenge(MANDANT, MESSAGE_ID);
    return gerendert.stream().map(Ausgefuehrt::sql).toList();
  }

  @Test
  @DisplayName("Jedes Statement traegt den Mandantenfilter als EXISTS (Regeln M3 und M5)")
  void jedes_statement_traegt_den_mandantenfilter() {
    for (String sql : alleStatements()) {
      String klein = sql.toLowerCase(Locale.ROOT);
      assertThat(klein)
          .as(
              "Der Mandantenfilter ist Bestandteil JEDES Statements, nicht nachgelagerte Pruefung."
                  + " Fehlendes exists in: %s",
              sql)
          .contains("exists");
      assertThat(klein)
          .as("Die Kette laeuft ueber ProjectMandant: %s", sql)
          .contains("projectmandant");
    }
  }

  /**
   * <b>Die Zaehlung traegt ihn auch.</b> Ohne Filter nennte die Antwort 3.048 Teile und lieferte
   * 3.000 — und der Unterschied saehe wie ein Fehler des Werkzeugs aus, nicht wie ein Leck.
   */
  @Test
  @DisplayName("Auch die beiden Zaehlungen tragen den Mandantenfilter")
  void auch_die_zaehlungen_tragen_den_mandantenfilter() {
    repository.zaehleKinder(MANDANT, MESSAGE_ID);
    repository.zaehleMergeEingaenge(MANDANT, MESSAGE_ID);

    assertThat(gerendert).hasSize(2);
    for (Ausgefuehrt ausgefuehrt : gerendert) {
      String klein = ausgefuehrt.sql().toLowerCase(Locale.ROOT);
      assertThat(klein).contains("count(").contains("exists").contains("projectmandant");
    }
  }

  @Test
  @DisplayName("Die Aufwaertsrichtung greift ueber den Primaerschluessel")
  void aufwaerts_ueber_den_primaerschluessel() {
    repository.findeGlied(MANDANT, MESSAGE_ID);

    String sql = letztesSql();
    assertThat(sql).contains("`Message`.`MessageID` = ");
    assertThat(sql.toLowerCase(Locale.ROOT))
        .as("ein einzelnes Glied braucht weder Sortierung noch Grenze")
        .doesNotContain("order by")
        .doesNotContain("limit");
  }

  @Test
  @DisplayName("Die Kinder laufen ueber SourceMessageID, die Eingaenge ueber TargetMessageID")
  void abwaerts_ueber_die_beiden_verkettungsspalten() {
    repository.findeKinder(MANDANT, MESSAGE_ID, null, 50);
    assertThat(letztesSql())
        .contains("`Message`.`SourceMessageID` = ")
        .doesNotContain("`Message`.`TargetMessageID` = ");

    repository.findeMergeEingaenge(MANDANT, MESSAGE_ID, null, 50);
    assertThat(letztesSql())
        .contains("`Message`.`TargetMessageID` = ")
        .doesNotContain("`Message`.`SourceMessageID` = ");
  }

  /**
   * Der Sortierschluessel ist derselbe wie in der Liste — sonst passte der Cursor nicht, und die
   * Zusammenfuehrung beider Richtungen in Java stimmte nicht mit den beiden Statements ueberein.
   */
  @Test
  @DisplayName("Beide Abwaertsrichtungen sortieren ueber (MessageLastUpdate, MessageID)")
  void abwaerts_sortiert_ueber_den_seitenschluessel() {
    repository.findeKinder(MANDANT, MESSAGE_ID, null, 50);
    repository.findeMergeEingaenge(MANDANT, MESSAGE_ID, null, 50);

    for (Ausgefuehrt ausgefuehrt : gerendert) {
      assertThat(ausgefuehrt.sql().toLowerCase(Locale.ROOT))
          .contains("order by")
          .contains("`message`.`messagelastupdate` asc")
          .contains("`message`.`messageid` asc");
    }
  }

  /**
   * <b>{@code limit + 1}</b>: Die Zusatzzeile beantwortet „gibt es weitere" ohne einen zweiten
   * Zugriff — dieselbe Bauform wie in der Liste (Regel L2).
   */
  @Test
  @DisplayName("Beide Abwaertsrichtungen lesen limit + 1 Zeilen")
  void abwaerts_liest_eine_zeile_mehr() {
    repository.findeKinder(MANDANT, MESSAGE_ID, null, 50);

    // Die Grenze steht als gebundener Wert im Statement, nicht als Literal im Text — deshalb
    // wird sie an den Bindungen geprueft und nicht mit einer Zeichenkettensuche.
    assertThat(grenzwerte())
        .as("die Zusatzzeile beantwortet „gibt es weitere\" ohne einen zweiten Zugriff")
        .contains("51");
    assertThat(letztesSql().toLowerCase(Locale.ROOT))
        .as("und eine Zeilengrenze steht ueberhaupt im Statement")
        .containsPattern("limit|fetch next");

    repository.findeMergeEingaenge(MANDANT, MESSAGE_ID, null, 50);
    assertThat(grenzwerte()).contains("51");
  }

  /**
   * Die gebundenen Werte des letzten Statements als Text. Ueber die Zeichenkette und nicht ueber
   * den Typ: Ob der Treiber die Grenze als {@code Integer} oder als {@code Long} bindet, ist eine
   * Eigenschaft von jOOQ und keine Aussage ueber dieses Statement.
   */
  private List<String> grenzwerte() {
    return gerendert.getLast().werte().stream().map(String::valueOf).toList();
  }

  /** Cursor-basiert und niemals {@code OFFSET} (Regel L3). */
  @Test
  @DisplayName("Der Cursor wird als Bereichsbedingung gerendert, niemals als OFFSET")
  void cursor_ist_kein_offset() {
    repository.findeKinder(
        MANDANT,
        MESSAGE_ID,
        new Seitenposition(LocalDateTime.parse("2025-12-29T10:00:00"), "k0"),
        50);

    String sql = letztesSql().toLowerCase(Locale.ROOT);
    assertThat(sql).as("cursor-basiert, niemals OFFSET (Regel L3)").doesNotContain("offset");

    String bedingung = sql.substring(sql.indexOf("where"), sql.indexOf("order by"));
    assertThat(bedingung)
        .as("die ODER-Form ergibt einen Bereich ueber beide Spalten (Messung L8): %s", bedingung)
        .contains("`messagelastupdate` > ?")
        .contains(" or ")
        .contains("`messageid` > ?");
  }

  /**
   * <b>Kein Zeitfenster.</b> Regel L1 gilt fuer Listen ueber {@code Message}; hier ist die Menge
   * durch einen Primaerschluessel benannt. Ein Fenster schnitte gerade die Kinder ab, die
   * ausserhalb liegen.
   */
  @Test
  @DisplayName("Kein Statement filtert ueber MessageLastUpdate")
  void kein_zeitfenster() {
    for (String sql : alleStatements()) {
      String klein = sql.toLowerCase(Locale.ROOT);
      int wo = klein.indexOf("where");
      String hinterWhere = wo < 0 ? "" : klein.substring(wo);
      assertThat(hinterWhere)
          .as("ein Zeitfenster schnitte die Kinder einer alten Wurzel weg: %s", sql)
          .doesNotContain("messagelastupdate >=")
          .doesNotContain("messagelastupdate <=");
    }
  }

  @Test
  @DisplayName("Die gerenderten Statements sind vollstaendig")
  void statements_sind_vollstaendig() {
    assertThat(alleStatements()).hasSize(5);
  }
}
