package de.kraftwerkone.overlord.monitor.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.common.Rollupzeitraum;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockDataProvider;
import org.jooq.tools.jdbc.MockResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Die Statements der Prozessansicht, <b>gerendert statt nachgebildet</b>: Das Repository laeuft
 * gegen eine jOOQ-Attrappe, und geprueft wird der Text, der wirklich herausfaellt.
 *
 * <p>Regel L7 verlangt die Messung <b>der Abfrage</b> und nicht einer aehnlichen. Dieser Test ist
 * die Bruecke: Er haelt fest, dass das, was {@code docs/process-view.md} §7 als gemessen ausweist,
 * auch das ist, was der Code schickt.
 *
 * <p><b>Vier Eigenschaften haengen hier und nirgends sonst</b>, weil sie sich nur am Text zeigen
 * und nicht am Ergebnis:
 *
 * <ol>
 *   <li>Die letzte Bewegung kommt als {@code ORDER BY … DESC} mit Deckelung und <b>nicht als {@code
 *       MAX()}</b> — beide liefern denselben Wert, die eine kostet bei {@code SUTTONS} das
 *       Zweiundzwanzigfache (M112 gegen M114).
 *   <li>Die Mandantenkette steht im Geruest als <b>Join</b> und in den Kennzahlen als <b>{@code
 *       EXISTS}</b> — ein Join in der Aggregation gaebe zu hohe Summen, und die saehen plausibel
 *       aus.
 *   <li>Um die Schluesselspalte der Rollup-Ebene steht <b>keine Funktion</b> — sonst faellt der
 *       Bereichszugriff weg, und die Abfrage waere langsamer, ohne falsch zu sein.
 *   <li>Jedes Paar liest <b>seine</b> Ebene und keine andere.
 * </ol>
 *
 * <p>Vorbild ist {@code DashboardStatementsTest}; die Bauform ist dieselbe.
 */
class ProzessbaumStatementsTest {

  private static final MandantContext MANDANT = new MandantContext("ERFUNDEN");
  private static final LocalDateTime JETZT = LocalDateTime.parse("2025-12-30T04:09:47");

  private final List<String> gerendert = new ArrayList<>();

  private ProzessbaumRepository repository;

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
        new ProzessbaumRepository(DSL.using(new MockConnection(attrappe), SQLDialect.MARIADB));
  }

  private String einziges() {
    assertThat(gerendert).hasSize(1);
    return gerendert.getFirst().replaceAll("\\s+", " ").trim();
  }

  private String geruest() {
    gerendert.clear();
    repository.geruest(MANDANT);
    return einziges();
  }

  private String kennzahlen(Rollupzeitraum zeitraum) {
    gerendert.clear();
    repository.kennzahlen(MANDANT, zeitraum, zeitraum.fenster(JETZT));
    return einziges();
  }

  // ─── Das Geruest ──────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("Das Geruest: alle Prozesse des Mandanten, mit letzter Bewegung")
  class Geruest {

    /**
     * Der ganze Text, Zeichen fuer Zeichen. Er steht hier vollstaendig, weil <b>jede</b> seiner
     * Eigenschaften eine gemessene Entscheidung traegt — und weil eine Teilpruefung nicht faende,
     * wenn jemand eine fuenfte Tabelle dazujoint.
     */
    @Test
    @DisplayName("woertlich")
    void woertlich() {
      assertThat(geruest())
          .isEqualTo(
              "select `GlassfishDB`.`Process`.`ProcessID`,"
                  + " `GlassfishDB`.`Process`.`ProcessName`,"
                  + " `overlord_monitor`.`process_catalog`.`partner`,"
                  + " `overlord_monitor`.`process_catalog`.`richtung`,"
                  + " `overlord_monitor`.`process_catalog`.`pflegestatus`, (select"
                  + " `overlord_monitor`.`message_rollup`.`stunde` from"
                  + " `overlord_monitor`.`message_rollup` where"
                  + " `overlord_monitor`.`message_rollup`.`process_id` ="
                  + " `GlassfishDB`.`Process`.`ProcessID` order by"
                  + " `overlord_monitor`.`message_rollup`.`stunde` desc fetch next ? rows only)"
                  + " from `GlassfishDB`.`Process` join `GlassfishDB`.`ProjectMandant` on"
                  + " `GlassfishDB`.`ProjectMandant`.`ProjectID` ="
                  + " `GlassfishDB`.`Process`.`ProjectID` left outer join"
                  + " `overlord_monitor`.`process_catalog` on"
                  + " `overlord_monitor`.`process_catalog`.`process_id` ="
                  + " `GlassfishDB`.`Process`.`ProcessID` where"
                  + " `GlassfishDB`.`ProjectMandant`.`MandantID` = ? order by"
                  + " `GlassfishDB`.`Process`.`ProcessName` asc");
    }

    /**
     * <b>Die teuerste Zeile dieses Tests.</b> {@code MAX(stunde)} liefert denselben Wert und liest
     * dafuer je Prozess den ganzen Indexbereich: 32,577 ms gegen 1,460 ms bei {@code SUTTONS}
     * (M112-A gegen M114-D). Der Plan sieht in beiden Faellen gleich aus — der Unterschied steht
     * <b>nur</b> im Statementtext, und deshalb steht er hier.
     */
    @Test
    @DisplayName("Die letzte Bewegung kommt ueber die Deckelung und nicht ueber MAX()")
    void keine_max_aggregation() {
      assertThat(geruest())
          .contains("order by `overlord_monitor`.`message_rollup`.`stunde` desc")
          .contains("fetch next ? rows only")
          .doesNotContain("max(");
    }

    /**
     * Regel M3: Der Filter ist Bestandteil des Statements. Hier als <b>Join</b> — {@code
     * ProjectMandant} ist der selektivste Teil der Bedingung und soll den Zugriff treiben, und
     * vervielfachen kann er nichts, weil sein Primaerschluessel {@code (ProjectID, MandantID)} ist.
     */
    @Test
    @DisplayName("Die Mandantenkette steht als Join im Statement")
    void mandantenkette_als_join() {
      assertThat(geruest())
          .contains(
              "join `GlassfishDB`.`ProjectMandant` on"
                  + " `GlassfishDB`.`ProjectMandant`.`ProjectID` ="
                  + " `GlassfishDB`.`Process`.`ProjectID`")
          .contains("where `GlassfishDB`.`ProjectMandant`.`MandantID` = ?")
          .doesNotContain("exists");
    }

    /**
     * {@code SUTTONS} und {@code WOC} haben keine einzige Katalogzeile (M110). Ein innerer Join
     * verloere ihre Prozesse stillschweigend — und damit ausgerechnet die, die vollstaendig unter
     * „nicht zugeordnet" erscheinen muessten.
     */
    @Test
    @DisplayName("Der Katalog haengt als LEFT JOIN dran")
    void katalog_als_left_join() {
      assertThat(geruest()).contains("left outer join `overlord_monitor`.`process_catalog`");
    }

    /** Ein Aufruf, ein Statement. Der Baum laedt nichts nach. */
    @Test
    @DisplayName("Ein Statement und kein zweites")
    void ein_statement() {
      repository.geruest(MANDANT);
      assertThat(gerendert).hasSize(1);
    }
  }

  // ─── Die Kennzahlen ───────────────────────────────────────────────────────────

  @Nested
  @DisplayName("Die Kennzahlen lesen je Paar ihre eigene Ebene")
  class Kennzahlen {

    @Test
    @DisplayName("48H: message_rollup, gruppiert nach Prozess und Rohstatus")
    void stundenebene() {
      assertThat(kennzahlen(Rollupzeitraum.STUNDEN_48))
          .isEqualTo(
              "select `overlord_monitor`.`message_rollup`.`process_id`,"
                  + " `overlord_monitor`.`message_rollup`.`message_status`,"
                  + " sum(`overlord_monitor`.`message_rollup`.`anzahl`) from"
                  + " `overlord_monitor`.`message_rollup` where"
                  + " (`overlord_monitor`.`message_rollup`.`stunde` >= ? and"
                  + " `overlord_monitor`.`message_rollup`.`stunde` < ? and exists (select 1 as"
                  + " `one` from `GlassfishDB`.`Process` as `baum_process` join"
                  + " `GlassfishDB`.`ProjectMandant` on"
                  + " `GlassfishDB`.`ProjectMandant`.`ProjectID` = `baum_process`.`ProjectID`"
                  + " where (`baum_process`.`ProcessID` ="
                  + " `overlord_monitor`.`message_rollup`.`process_id` and"
                  + " `GlassfishDB`.`ProjectMandant`.`MandantID` = ?))) group by"
                  + " `overlord_monitor`.`message_rollup`.`process_id`,"
                  + " `overlord_monitor`.`message_rollup`.`message_status`");
    }

    @Test
    @DisplayName("30T liest die Tagesebene, 12M die Monatsebene — und keines die Stundenebene")
    void abgeleitete_ebenen() {
      assertThat(kennzahlen(Rollupzeitraum.TAGE_30))
          .contains("`overlord_monitor`.`message_rollup_tag`")
          .doesNotContain("`overlord_monitor`.`message_rollup`.");
      assertThat(kennzahlen(Rollupzeitraum.MONATE_12))
          .contains("`overlord_monitor`.`message_rollup_monat`")
          .doesNotContain("`overlord_monitor`.`message_rollup`.")
          .doesNotContain("`overlord_monitor`.`message_rollup_tag`");
    }

    /**
     * <b>Hier wird summiert, und deshalb darf die Kette kein Join sein.</b> {@code ProjectMandant}
     * ist n:m; ein Join vervielfachte jede Rollupzeile, sobald ein Projekt an mehreren Mandanten
     * haengt — und damit die Summe. Der Fehler waere still.
     */
    @Test
    @DisplayName("Die Mandantenkette ist ein EXISTS und kein Join auf die Rolluptabelle")
    void mandantenkette_als_exists() {
      for (Rollupzeitraum zeitraum : Rollupzeitraum.values()) {
        assertThat(kennzahlen(zeitraum))
            .as("Paar %s", zeitraum.code())
            .contains("exists (select 1 as `one` from `GlassfishDB`.`Process` as `baum_process`")
            .contains("`GlassfishDB`.`ProjectMandant`.`MandantID` = ?");
      }
    }

    /**
     * Eine Funktion um die Schluesselspalte kostet den Bereichszugriff — die Abfrage waere
     * langsamer, ohne falsch zu sein. Genau solche Fehler fallen sonst nirgends auf.
     */
    @Test
    @DisplayName("Um den Eimerschluessel steht keine Funktion")
    void keine_funktion_um_den_schluessel() {
      assertThat(kennzahlen(Rollupzeitraum.STUNDEN_48))
          .contains("`overlord_monitor`.`message_rollup`.`stunde` >= ?")
          .doesNotContain("date(")
          .doesNotContain("date_format(")
          .doesNotContain("cast(");
      assertThat(kennzahlen(Rollupzeitraum.TAGE_30))
          .contains("`overlord_monitor`.`message_rollup_tag`.`tag` >= ?")
          .doesNotContain("date(")
          .doesNotContain("date_format(")
          .doesNotContain("cast(");
      assertThat(kennzahlen(Rollupzeitraum.MONATE_12))
          .contains("`overlord_monitor`.`message_rollup_monat`.`monat` >= ?")
          .doesNotContain("date(")
          .doesNotContain("date_format(")
          .doesNotContain("cast(");
    }

    /**
     * Der Baum wird in {@code ProzessbaumService} sortiert — nach Partner, Richtung und Name. Ein
     * {@code ORDER BY} hier kostete eine Sortierung, die niemand liest.
     */
    @Test
    @DisplayName("Kein ORDER BY — sortiert wird im Dienst")
    void ohne_sortierung() {
      for (Rollupzeitraum zeitraum : Rollupzeitraum.values()) {
        assertThat(kennzahlen(zeitraum)).as("Paar %s", zeitraum.code()).doesNotContain("order by");
      }
    }

    /**
     * <b>Gruppiert wird nach dem Rohstatus</b> und nicht schon nach einer Einordnung: Die
     * Einordnung ist eine Regel, die sich aendern kann (E-g), und sie entsteht beim Lesen in {@code
     * common/MessageStatusClassifier}. Stuende sie hier, waere sie in SQL nachgebaut.
     */
    @Test
    @DisplayName("Gruppiert wird nach Rohstatus, nicht nach einer eingebauten Einordnung")
    void gruppierung_nach_rohstatus() {
      assertThat(kennzahlen(Rollupzeitraum.STUNDEN_48))
          .contains(
              "group by `overlord_monitor`.`message_rollup`.`process_id`,"
                  + " `overlord_monitor`.`message_rollup`.`message_status`")
          .doesNotContain("case");
    }
  }

  // ─── Der Umfang ───────────────────────────────────────────────────────────────

  /**
   * <b>Zwei Statements je Aufruf, und mehr werden es nicht.</b> Das Dashboard haelt dieselbe Zusage
   * mit sieben; hier sind es zwei, weil es weder eine Belegungsprobe noch eine Live-Abfrage gibt.
   * Faellt dieser Test, hat jemand eine dritte Abfrage eingebaut — etwa eine Ueberfaelligkeit je
   * Prozess, und genau die ist ausgeschlossen ({@code docs/process-view.md} §5).
   */
  @Test
  @DisplayName("Ein Aufruf des Baums kostet genau zwei Statements")
  void genau_zwei_statements() {
    gerendert.clear();
    repository.geruest(MANDANT);
    repository.kennzahlen(
        MANDANT, Rollupzeitraum.STUNDEN_48, Rollupzeitraum.STUNDEN_48.fenster(JETZT));
    assertThat(gerendert).hasSize(2);
  }

  /**
   * <b>Keine Abfrage der Prozessansicht fasst {@code Message} an.</b> Regel L2: Kennzahlen kommen
   * aus dem Rollup. Die eine benannte Ausnahme des Projekts ist die Ueberfaelligkeitskachel des
   * Dashboards, und sie ist ausdruecklich nicht hierher uebernommen worden.
   */
  @Test
  @DisplayName("Keine Live-Aggregation ueber Message (Regel L2)")
  void keine_live_aggregation() {
    gerendert.clear();
    repository.geruest(MANDANT);
    for (Rollupzeitraum zeitraum : Rollupzeitraum.values()) {
      repository.kennzahlen(MANDANT, zeitraum, zeitraum.fenster(JETZT));
    }
    assertThat(gerendert)
        .isNotEmpty()
        .allSatisfy(sql -> assertThat(sql).doesNotContain("`GlassfishDB`.`Message`"));
  }
}
