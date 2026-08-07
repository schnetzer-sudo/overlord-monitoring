package de.kraftwerkone.overlord.monitor.message;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.util.ArrayList;
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
 * Haelt die <b>unverhandelbaren Regeln</b> der Detail-Statements maschinell fest — <b>ohne
 * Datenbank</b>.
 *
 * <p>Der Isolationstest weist die Trennung am laufenden System nach, braucht dafuer aber die
 * Testkopie und ist in der CI ausgeschlossen. Dieser Test schliesst die Luecke von der anderen
 * Seite: Er rendert jedes Statement gegen eine Attrappe und prueft am Text, dass der
 * Mandantenfilter darin steht. <b>Ein Statement ohne ihn faellt hier auf, nicht erst beim naechsten
 * Lauf mit Datenbankzugang.</b>
 *
 * <p>Er ersetzt den Isolationstest nicht — Text ist kein Verhalten. Er faengt das Offensichtliche
 * ab: das vergessene {@code EXISTS} und das Filtern ueber {@code MessagePropertyValue} (Regel L4).
 */
class NachrichtendetailStatementsTest {

  private static final MandantContext MANDANT = new MandantContext("NEXANS");
  private static final String MESSAGE_ID = "eine-nachricht";

  private final List<String> gerendert = new ArrayList<>();
  private NachrichtendetailRepository repository;

  @BeforeEach
  void attrappeAufbauen() {
    gerendert.clear();
    MockDataProvider attrappe =
        ausfuehrung -> {
          gerendert.add(ausfuehrung.sql());
          DSLContext leer = DSL.using(SQLDialect.MARIADB);
          Field<Integer> platzhalter = DSL.field("platzhalter", Integer.class);
          Result<Record1<Integer>> ergebnis = leer.newResult(platzhalter);
          // Eine Existenzfrage braucht eine Zeile, sonst laeuft fetchExists ins Leere. Der Wert
          // ist gleichgueltig — geprueft wird hier der Text des Statements, nicht sein Ergebnis.
          if (ausfuehrung.sql().trim().toLowerCase(Locale.ROOT).startsWith("select exists")) {
            Record1<Integer> zeile = leer.newRecord(platzhalter);
            zeile.value1(0);
            ergebnis.add(zeile);
          }
          return new MockResult[] {new MockResult(ergebnis.size(), ergebnis)};
        };
    repository =
        new NachrichtendetailRepository(
            DSL.using(new MockConnection(attrappe), SQLDialect.MARIADB));
  }

  /** Alle fuenf Statements einmal ausloesen. */
  private List<String> alleStatements() {
    repository.findeKopf(MANDANT, MESSAGE_ID);
    repository.findeAktionen(MANDANT, MESSAGE_ID);
    repository.findeAblaufschritte(MANDANT, MESSAGE_ID);
    repository.findeKuratierteEigenschaften(MANDANT, MESSAGE_ID);
    repository.findeEigenschaften(MANDANT, MESSAGE_ID);
    return List.copyOf(gerendert);
  }

  @Test
  @DisplayName("Jedes Statement traegt den Mandantenfilter als EXISTS (Regel M3)")
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

  @Test
  @DisplayName("Die Existenzpruefung traegt ihn ebenso")
  void existenzpruefung_traegt_ihn_ebenso() {
    repository.existiert(MANDANT, MESSAGE_ID);

    assertThat(gerendert).isNotEmpty();
    assertThat(gerendert.getLast().toLowerCase(Locale.ROOT))
        .contains("exists")
        .contains("projectmandant");
  }

  @Test
  @DisplayName("Kein Statement filtert, gruppiert oder sortiert ueber MessagePropertyValue (L4)")
  void kein_zugriff_ueber_den_eigenschaftswert() {
    for (String sql : alleStatements()) {
      String klein = sql.toLowerCase(Locale.ROOT);
      int wo = klein.indexOf("where");
      String hinterWhere = wo < 0 ? "" : klein.substring(wo);
      assertThat(hinterWhere)
          .as(
              "Die Indizes auf MessagePropertyValue sind Praefix-Indizes ueber 50 Zeichen und fuer"
                  + " Filter, Gruppierung und Sortierung ungeeignet: %s",
              sql)
          .doesNotContain("messagepropertyvalue");
    }
  }

  @Test
  @DisplayName("Der Eigenschaftswert wird schon in der Abfrage begrenzt")
  void eigenschaftswert_wird_in_der_abfrage_begrenzt() {
    repository.findeEigenschaften(MANDANT, MESSAGE_ID);

    String sql = gerendert.getLast().toLowerCase(Locale.ROOT);
    assertThat(sql)
        .as("ein mediumtext laesst 16 MB je Zelle zu — was der Treiber holt, ist schon Last")
        .contains("left(");
    assertThat(sql)
        .as("die ungekappte Laenge muss in Bytes danebenstehen, sonst ist die Kappung unsichtbar")
        .containsPattern("octet_length|length\\(");
  }

  @Test
  @DisplayName("Die Ablaufdefinition wird ueber MessageAction.SOSID gejoint, nie ueber Message")
  void ablaufdefinition_ueber_messageaction() {
    repository.findeAblaufschritte(MANDANT, MESSAGE_ID);

    String sql = gerendert.getLast();
    assertThat(sql)
        .as(
            "Ueber Message.SOSID zu joinen liefert in 3,83 Prozent der Faelle einen fachlich"
                + " falschen Namen, ohne dass es auffiele (M15)")
        .contains("`MessageAction`.`SOSID`");
    assertThat(sql).contains("`SOSAction`");
  }

  /** Nur zum Nachschlagen beim Messen — die Laufzeiten stehen in {@code nachrichtendetail.md}. */
  @Test
  @DisplayName("Die gerenderten Statements sind vollstaendig")
  void statements_sind_vollstaendig() {
    assertThat(alleStatements()).hasSize(5);
  }
}
