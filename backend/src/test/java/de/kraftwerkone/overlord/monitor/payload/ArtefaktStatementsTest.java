package de.kraftwerkone.overlord.monitor.payload;

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
 * Haelt die <b>unverhandelbaren Regeln</b> der Rohdaten-Statements maschinell fest — <b>ohne
 * Datenbank</b>.
 *
 * <p>Derselbe Aufbau wie {@code NachrichtendetailStatementsTest}: Jedes Statement wird gegen eine
 * Attrappe gerendert und am Text geprueft. Er ersetzt den Isolationstest nicht — Text ist kein
 * Verhalten —, faengt aber das Offensichtliche ab: das vergessene {@code EXISTS} und den Zugriff
 * ueber {@code MessagePropertyValue} (Regel L4).
 */
class ArtefaktStatementsTest {

  private static final MandantContext MANDANT = new MandantContext("VOTG");
  private static final String MESSAGE_ID = "eine-erfundene-nachricht";

  private final List<String> gerendert = new ArrayList<>();
  private ArtefaktRepository repository;

  @BeforeEach
  void attrappeAufbauen() {
    gerendert.clear();
    MockDataProvider attrappe =
        ausfuehrung -> {
          gerendert.add(ausfuehrung.sql());
          DSLContext leer = DSL.using(SQLDialect.MARIADB);
          Field<Integer> platzhalter = DSL.field("platzhalter", Integer.class);
          Result<Record1<Integer>> ergebnis = leer.newResult(platzhalter);
          if (ausfuehrung.sql().trim().toLowerCase(Locale.ROOT).startsWith("select exists")) {
            Record1<Integer> zeile = leer.newRecord(platzhalter);
            zeile.value1(0);
            ergebnis.add(zeile);
          }
          return new MockResult[] {new MockResult(ergebnis.size(), ergebnis)};
        };
    repository =
        new ArtefaktRepository(DSL.using(new MockConnection(attrappe), SQLDialect.MARIADB));
  }

  /** Alle vier Statements einmal ausloesen. */
  private List<String> alleStatements() {
    repository.findeArtefakte(MANDANT, MESSAGE_ID);
    repository.existiert(MANDANT, MESSAGE_ID);
    repository.findeVerbindung(MANDANT, MESSAGE_ID, "BEISPIELPROD42");
    repository.findeOriginaldateiname(MANDANT, MESSAGE_ID, (short) 1);
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
  @DisplayName("Auch die Aufloesung der Ablagenkennung traegt ihn")
  void aufloesung_traegt_ihn_ebenso() {
    repository.findeVerbindung(MANDANT, MESSAGE_ID, "BEISPIELPROD42");

    String sql = gerendert.getLast();
    String klein = sql.toLowerCase(Locale.ROOT);
    assertThat(klein)
        .as(
            "Ohne die Kette waere das eine Auskunftsstelle fuer Verbindungszeichenketten, die nur"
                + " deshalb sicher ist, weil der Aufrufer vorher das Richtige getan hat: %s",
            sql)
        .contains("exists")
        .contains("projectmandant");
    assertThat(sql).contains("`Service`");
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
  @DisplayName("Die Ablage wird nicht ueber den Verweis gejoint — das waere Filtern ueber den Wert")
  void kein_join_ueber_den_verweis() {
    for (String sql : alleStatements()) {
      String klein = sql.toLowerCase(Locale.ROOT);
      assertThat(klein)
          .as(
              "Ein Join ueber substring_index(MessagePropertyValue, '|', 1) waere genau das"
                  + " Filtern ueber den Wert, das L4 verbietet — und nicht die gemessene Form"
                  + " (M58 (1) und (2) sind getrennte Statements): %s",
              sql)
          .doesNotContain("substring_index")
          .doesNotContain("substring(");
    }
  }

  @Test
  @DisplayName("Der Einstieg laeuft ueber die MessageID, nicht ueber den Namen allein (L4)")
  void einstieg_ueber_die_messageid() {
    repository.findeArtefakte(MANDANT, MESSAGE_ID);

    String sql = gerendert.getLast();
    assertThat(sql).contains("`Message`.`MessageID` = ?");
    assertThat(sql)
        .as("Die Namensbedingung kommt dazu, sie ersetzt den Einstieg nicht")
        .contains("`MessagePropertyName` like ?");
  }

  @Test
  @DisplayName("Die LIKE-Muster tragen kein _ — das waere ein zweiter Platzhalter")
  void keine_unterstriche_in_den_mustern() {
    for (String muster : Artefaktnamen.likeMuster()) {
      assertThat(muster)
          .as(
              "Dieselbe Falle, an der das naive LIKE 'ERROR_%%' scheitert (Regel F1,"
                  + " docs/message-status.md)")
          .doesNotContain("_");
    }
  }

  @Test
  @DisplayName("Kein Statement traegt ein Zeitfenster — die Nachricht ist benannt (L1)")
  void kein_zeitfenster() {
    for (String sql : alleStatements()) {
      assertThat(sql.toLowerCase(Locale.ROOT))
          .as("Regel L1 gilt fuer Listen ueber Message; hier ist der Primaerschluessel gesetzt")
          .doesNotContain("messagelastupdate");
    }
  }

  @Test
  @DisplayName("Die gerenderten Statements sind vollstaendig")
  void statements_sind_vollstaendig() {
    assertThat(alleStatements()).hasSize(4);
  }
}
