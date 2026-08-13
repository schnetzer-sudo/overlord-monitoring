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
 * Haelt die Regeln des Typen-Statements maschinell fest — <b>ohne Datenbank</b>.
 *
 * <p>Dieselbe Bauform wie {@code BamStatementsTest} und {@code BamSucheStatementsTest}: Der
 * Isolationstest weist die Trennung am laufenden System nach, braucht dafuer aber die Testkopie und
 * ist in der CI ausgeschlossen. Dieser Test schliesst die Luecke von der anderen Seite — er rendert
 * das Statement gegen eine Attrappe und prueft am Text, dass der Mandant darin steht.
 *
 * <p><b>Der zweite Teil ist hier der wichtigere.</b> Dieser Endpunkt liest ausschliesslich
 * Stammdaten; die Versuchung, aus der Auswahl eine Aussage ueber den <i>Bestand</i> zu machen
 * („biete nur Typen an, die tatsaechlich vorkommen"), ist gross und waere eine Existenzfrage ueber
 * den Gesamtbestand eines Mandanten — genau die Gestalt, die {@code docs/nachrichtenliste.md} §1
 * als L15-Falle fuehrt (13,2 Sekunden fuer {@code IBIS}). Deshalb steht hier fest, dass {@code
 * Message} und {@code MessageBAM} nicht vorkommen.
 */
class BamTypenStatementsTest {

  private static final MandantContext MANDANT = new MandantContext("NEXANS");

  /** Ein abgefangenes Statement: der Text und die gebundenen Werte. */
  private record Ausgefuehrt(String sql, List<Object> werte) {}

  private final List<Ausgefuehrt> gerendert = new ArrayList<>();
  private BamTypenRepository repository;

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
        new BamTypenRepository(DSL.using(new MockConnection(attrappe), SQLDialect.MARIADB));
  }

  private String sql() {
    repository.findeKonfigurierteTypen(MANDANT);
    return gerendert.getLast().sql();
  }

  /**
   * <b>Der Mandant ist die Bedingung selbst</b> (Regel M3) und nicht ein Filter ueber einer
   * ungefilterten Menge. Ohne ihn liefe der Endpunkt ueber die Konfiguration <i>aller</i> Mandanten
   * — und die Auswahl neben dem Suchfeld nennte Belegarten, die es beim eigenen Mandanten gar nicht
   * gibt.
   */
  @Test
  @DisplayName("Das Statement filtert ueber MandantID, und der Wert ist gebunden")
  void das_statement_traegt_den_mandanten() {
    String sql = sql();

    assertThat(sql.toLowerCase(Locale.ROOT))
        .as("gerendert: %s", sql)
        .contains("`messagebammandant`.`mandantid` = ");
    assertThat(gerendert.getLast().werte())
        .as("der Mandant kommt gebunden und nicht als Literal im Text")
        .contains(MANDANT.mandantId());
  }

  /**
   * <b>Keine Existenzfrage ueber den Bestand.</b> Die Auswahl sagt, was konfiguriert ist, und
   * behauptet nichts darueber, was vorkommt. Ein Blick in {@code MessageBAM} waere zugleich der
   * Verstoss gegen Regel L1: Er brauchte ein Zeitfenster, und die Auswahl hat keines.
   */
  @Test
  @DisplayName("Weder Message noch MessageBAM werden angefasst")
  void nur_stammdaten() {
    String klein = sql().toLowerCase(Locale.ROOT);

    assertThat(klein)
        .as("die Auswahl ist ein Angebot und keine Aussage ueber den Bestand")
        .doesNotContain("`messagebam`")
        .doesNotContain("`message`")
        .doesNotContain("messagelastupdate");
    assertThat(klein).contains("`messagebammandant`").contains("`messagebamtype`");
  }

  /**
   * <b>Die Beschriftung siebt nicht.</b> Fehlt die Zeile in {@code MessageBAMType}, verschwindet
   * der konfigurierte Typ nicht — er bekommt seine Typnummer ({@link Typbezeichnung}). Dieselbe
   * Regel wie im Belegdaten-Block.
   */
  @Test
  @DisplayName("MessageBAMType haengt an einem LEFT JOIN")
  void die_beschriftung_siebt_nicht() {
    String sql = sql();
    String klein = sql.toLowerCase(Locale.ROOT);

    int wo = klein.indexOf("`messagebamtype` on");
    assertThat(wo).as("die Beschriftungstabelle wird angebunden: %s", sql).isGreaterThan(0);
    assertThat(klein.substring(0, wo)).endsWith("left outer join `glassfishdb`.");
  }

  /**
   * <b>Zwei Sortierschluessel, und der zweite ist nicht Kosmetik.</b> {@code
   * MessageBAMTypeSortIndex} ist im Schema nicht eindeutig — bei {@code VOTG} tragen die Typen 2002
   * und 2011 beide den Index 2002 (gemessen am 13.08.2026 gegen die Testkopie). Ohne die Typnummer
   * als zweiten Schluessel entschiede dort die Reihenfolge der Speicherung, und zwei Aufrufe
   * zeigten dieselbe Auswahl verschieden.
   */
  @Test
  @DisplayName("Sortiert wird ueber Sortierindex und Typnummer")
  void die_ordnung_ist_eindeutig() {
    String sql = sql();
    String klein = sql.toLowerCase(Locale.ROOT);

    int wo = klein.indexOf("order by");
    assertThat(wo).as("gerendert: %s", sql).isGreaterThan(0);
    String ordnung = klein.substring(wo);
    assertThat(ordnung).contains("`messagebamtypesortindex`").contains("`messagebamtype`");
    assertThat(ordnung.indexOf("`messagebamtypesortindex`"))
        .as("der Sortierindex fuehrt, die Typnummer entscheidet nur den Gleichstand")
        .isLessThan(ordnung.indexOf("`messagebamtype` asc"));
  }
}
