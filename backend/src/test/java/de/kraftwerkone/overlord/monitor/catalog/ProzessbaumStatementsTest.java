package de.kraftwerkone.overlord.monitor.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.common.Baumfenster;
import de.kraftwerkone.overlord.monitor.common.Baumfenster.Segment;
import de.kraftwerkone.overlord.monitor.common.Baumgliederung;
import de.kraftwerkone.overlord.monitor.common.FehlerLiveRepository;
import de.kraftwerkone.overlord.monitor.common.FehlerLiveService;
import de.kraftwerkone.overlord.monitor.common.LiveRestRepository;
import de.kraftwerkone.overlord.monitor.common.LiveRestService;
import de.kraftwerkone.overlord.monitor.common.MandantContext;
import de.kraftwerkone.overlord.monitor.common.MessageStatusClassifier;
import de.kraftwerkone.overlord.monitor.common.Rollupebene;
import de.kraftwerkone.overlord.monitor.common.Rollupzeitraum;
import de.kraftwerkone.overlord.monitor.common.WasserstandRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
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
 *   <li>Jedes <b>Segment</b> liest seine Ebene und keine andere, und es steht keine Ebene im Text,
 *       die kein Segment traegt — die Fassung vom 07.09.2026 (E-97); fuer die drei Paare sagt sie
 *       dasselbe wie die alte: <i>Jedes Paar liest seine Ebene und keine andere.</i>
 * </ol>
 *
 * <p><b>Seit dem 07.09.2026 (Schritt 10c-4b) kommt eine fuenfte dazu, und sie steht ueber den
 * anderen:</b> Die drei Paare rendern <b>Zeichen fuer Zeichen den Text von vor diesem Schritt</b>.
 * Der Text ist vor dem Bau der Zerlegung gepinnt worden und hat den Bau ueberstanden.
 *
 * <p><b>Seit dem 17.09.2026 (Live-Rest) ist die Zusicherung „genau zwei Statements je Aufruf und
 * kein {@code Message}" bewusst gefallen.</b> Ein Aufruf setzt bei angewandtem Live-Rest fuenf
 * Statements ab, und das fuenfte liest {@code Message} — einzeln benannt in {@link EinAufruf}, wie
 * in {@code DashboardStatementsTest}. Die zwei Statements des Rollup-Teils (E-42) sind unveraendert
 * und weiter woertlich gepinnt.
 *
 * <p><b>Seit dem 18.09.2026 (Fehler live, Teil B) kommt ein Statement dazu, und zwar als
 * letztes:</b> die Fehlerlesung aus {@code common}, sechs Statements bei angewandtem Live-Rest,
 * sonst vier — {@code Message} steht damit in zwei Statements eines angewandten Aufrufs und in
 * einem sonst. Der Text der Lesung ist derselbe wie in der Uebersicht und bleibt dort gepinnt
 * ({@code DashboardStatementsTest}); hier wird er gegen das gerenderte Repository gehalten.
 *
 * <p>Vorbild ist {@code DashboardStatementsTest}; die Bauform ist dieselbe.
 */
class ProzessbaumStatementsTest {

  private static final MandantContext MANDANT = new MandantContext("ERFUNDEN");
  private static final LocalDateTime JETZT = LocalDateTime.parse("2025-12-30T04:09:47");

  private final List<String> gerendert = new ArrayList<>();

  private ProzessbaumRepository repository;

  /**
   * Der Kontext der Attrappe — auch fuer die Klassen des Live-Rests (E-42 ist um sie erweitert).
   */
  private DSLContext kontext;

  /**
   * Was die Attrappe auf die Wasserstandsabfrage antwortet — {@code null} heisst „nie gerechnet".
   * Alle anderen Statements bekommen ein leeres Ergebnis.
   */
  private LocalDateTime wasserstand;

  private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");

  @BeforeEach
  void attrappeAufbauen() {
    gerendert.clear();
    wasserstand = null;
    MockDataProvider attrappe =
        ausfuehrung -> {
          gerendert.add(ausfuehrung.sql());
          DSLContext leer = DSL.using(SQLDialect.MARIADB);
          if (ausfuehrung.sql().contains("`rollup_lauf`") && wasserstand != null) {
            Field<LocalDateTime> max = DSL.field("max", SQLDataType.LOCALDATETIME);
            Result<Record1<LocalDateTime>> ergebnis = leer.newResult(max);
            Record1<LocalDateTime> satz = leer.newRecord(max);
            satz.value1(wasserstand);
            ergebnis.add(satz);
            return new MockResult[] {new MockResult(1, ergebnis)};
          }
          return new MockResult[] {new MockResult(0, leer.newResult())};
        };
    kontext = DSL.using(new MockConnection(attrappe), SQLDialect.MARIADB);
    repository = new ProzessbaumRepository(kontext);
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
    return kennzahlen(Baumfenster.paar(zeitraum).segmente(JETZT));
  }

  private String kennzahlen(List<Segment> segmente) {
    gerendert.clear();
    repository.kennzahlen(MANDANT, segmente);
    return einziges();
  }

  private static LocalDateTime t(String iso) {
    return LocalDateTime.parse(iso);
  }

  private static Segment segment(Rollupebene ebene, String von, String bis) {
    return new Segment(ebene, t(von), t(bis));
  }

  /** Der Boesfall aus M149: fuenf Segmente, alle drei Ebenen, beide Randarten. */
  private static List<Segment> boesfall() {
    return Baumfenster.zerlegung(t("2024-12-29T14:00"), t("2025-12-30T03:00"));
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
                  + " `GlassfishDB`.`Project`.`ProjectDescription`,"
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
                  + " `GlassfishDB`.`Project` on `GlassfishDB`.`Project`.`ProjectID` ="
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

    /**
     * <b>Das Projekt haengt als {@code LEFT JOIN} dran</b> <i>(seit 15.09.2026)</i>, und genau eine
     * Spalte kommt daraus in die Projektion: {@code ProjectDescription}, der Schluessel der
     * Gliederung {@code PROJEKT}. {@code LEFT} wie in {@code docs/prozessauswahl.md} §4 — die
     * Sichtbarkeit haengt an der Mandantenkette und nicht an einer Beschreibung.
     *
     * <p><b>Ein Geruest fuer beide Gliederungen</b>: Die Kette bleibt der eine Join auf {@code
     * ProjectMandant}, und aus {@code Project} wird nichts gelesen ausser der Beschreibung.
     */
    @Test
    @DisplayName("Das Projekt haengt als LEFT JOIN dran und liefert genau die Beschreibung")
    void projekt_als_left_join() {
      String text = geruest();
      assertThat(text)
          .contains(
              "left outer join `GlassfishDB`.`Project` on `GlassfishDB`.`Project`.`ProjectID` ="
                  + " `GlassfishDB`.`Process`.`ProjectID`")
          .contains("`GlassfishDB`.`Project`.`ProjectDescription`")
          .doesNotContain("`GlassfishDB`.`Project`.`ProjectName`");
      assertThat(text.split("join `GlassfishDB`.`ProjectMandant`", -1))
          .as("Die Mandantenkette steht genau einmal im Geruest")
          .hasSize(2);
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

    /**
     * <b>Die tragende Zusage von Schritt 10c-4b:</b> Die drei Paare rendern nach dem Bau des freien
     * Zeitfensters <b>denselben</b> Text wie davor — Zeichen fuer Zeichen. Der Text hier ist am
     * 07.09.2026 gegen den unveraenderten Code gepinnt worden, bevor die Zerlegung gebaut wurde.
     */
    @Test
    @DisplayName("30T: message_rollup_tag, woertlich der Text von vor 10c-4b")
    void tagesebene_woertlich() {
      assertThat(kennzahlen(Rollupzeitraum.TAGE_30))
          .isEqualTo(
              "select `overlord_monitor`.`message_rollup_tag`.`process_id`,"
                  + " `overlord_monitor`.`message_rollup_tag`.`message_status`,"
                  + " sum(`overlord_monitor`.`message_rollup_tag`.`anzahl`) from"
                  + " `overlord_monitor`.`message_rollup_tag` where"
                  + " (`overlord_monitor`.`message_rollup_tag`.`tag` >= ? and"
                  + " `overlord_monitor`.`message_rollup_tag`.`tag` < ? and exists (select 1 as"
                  + " `one` from `GlassfishDB`.`Process` as `baum_process` join"
                  + " `GlassfishDB`.`ProjectMandant` on"
                  + " `GlassfishDB`.`ProjectMandant`.`ProjectID` = `baum_process`.`ProjectID`"
                  + " where (`baum_process`.`ProcessID` ="
                  + " `overlord_monitor`.`message_rollup_tag`.`process_id` and"
                  + " `GlassfishDB`.`ProjectMandant`.`MandantID` = ?))) group by"
                  + " `overlord_monitor`.`message_rollup_tag`.`process_id`,"
                  + " `overlord_monitor`.`message_rollup_tag`.`message_status`");
    }

    /** Siehe {@link #tagesebene_woertlich()}. */
    @Test
    @DisplayName("12M: message_rollup_monat, woertlich der Text von vor 10c-4b")
    void monatsebene_woertlich() {
      assertThat(kennzahlen(Rollupzeitraum.MONATE_12))
          .isEqualTo(
              "select `overlord_monitor`.`message_rollup_monat`.`process_id`,"
                  + " `overlord_monitor`.`message_rollup_monat`.`message_status`,"
                  + " sum(`overlord_monitor`.`message_rollup_monat`.`anzahl`) from"
                  + " `overlord_monitor`.`message_rollup_monat` where"
                  + " (`overlord_monitor`.`message_rollup_monat`.`monat` >= ? and"
                  + " `overlord_monitor`.`message_rollup_monat`.`monat` < ? and exists (select 1"
                  + " as `one` from `GlassfishDB`.`Process` as `baum_process` join"
                  + " `GlassfishDB`.`ProjectMandant` on"
                  + " `GlassfishDB`.`ProjectMandant`.`ProjectID` = `baum_process`.`ProjectID`"
                  + " where (`baum_process`.`ProcessID` ="
                  + " `overlord_monitor`.`message_rollup_monat`.`process_id` and"
                  + " `GlassfishDB`.`ProjectMandant`.`MandantID` = ?))) group by"
                  + " `overlord_monitor`.`message_rollup_monat`.`process_id`,"
                  + " `overlord_monitor`.`message_rollup_monat`.`message_status`");
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

  // ─── Das freie Fenster ────────────────────────────────────────────────────────

  /**
   * Die Bauform Z-U ({@code docs/process-view.md} §33), gerendert statt von Hand gebaut. M149 hat
   * ein handgeschriebenes Statement gemessen; Regel L7 verlangt fuer die gebaute Fassung eine
   * eigene Messung (M152) — und dieser Test haelt fest, <b>was</b> gemessen wird.
   */
  @Nested
  @DisplayName("Das freie Fenster: eine Ableitung mit UNION ALL, darueber alles einmal")
  class FreiesFenster {

    /** Der ganze Text des Boesfalls, Zeichen fuer Zeichen — das ist die Fassung, die M152 misst. */
    @Test
    @DisplayName("Der Boesfall aus M149, woertlich")
    void boesfall_woertlich() {
      assertThat(kennzahlen(boesfall()))
          .isEqualTo(
              "select `t`.`process_id`, `t`.`message_status`, sum(`t`.`anzahl`) from (select"
                  + " `overlord_monitor`.`message_rollup`.`process_id`,"
                  + " `overlord_monitor`.`message_rollup`.`message_status`,"
                  + " `overlord_monitor`.`message_rollup`.`anzahl` from"
                  + " `overlord_monitor`.`message_rollup` where"
                  + " ((`overlord_monitor`.`message_rollup`.`stunde` >= ? and"
                  + " `overlord_monitor`.`message_rollup`.`stunde` < ?) or"
                  + " (`overlord_monitor`.`message_rollup`.`stunde` >= ? and"
                  + " `overlord_monitor`.`message_rollup`.`stunde` < ?)) union all select"
                  + " `overlord_monitor`.`message_rollup_tag`.`process_id`,"
                  + " `overlord_monitor`.`message_rollup_tag`.`message_status`,"
                  + " `overlord_monitor`.`message_rollup_tag`.`anzahl` from"
                  + " `overlord_monitor`.`message_rollup_tag` where"
                  + " ((`overlord_monitor`.`message_rollup_tag`.`tag` >= ? and"
                  + " `overlord_monitor`.`message_rollup_tag`.`tag` < ?) or"
                  + " (`overlord_monitor`.`message_rollup_tag`.`tag` >= ? and"
                  + " `overlord_monitor`.`message_rollup_tag`.`tag` < ?)) union all select"
                  + " `overlord_monitor`.`message_rollup_monat`.`process_id`,"
                  + " `overlord_monitor`.`message_rollup_monat`.`message_status`,"
                  + " `overlord_monitor`.`message_rollup_monat`.`anzahl` from"
                  + " `overlord_monitor`.`message_rollup_monat` where"
                  + " (`overlord_monitor`.`message_rollup_monat`.`monat` >= ? and"
                  + " `overlord_monitor`.`message_rollup_monat`.`monat` < ?)) as `t` where exists"
                  + " (select 1 as `one` from `GlassfishDB`.`Process` as `baum_process` join"
                  + " `GlassfishDB`.`ProjectMandant` on"
                  + " `GlassfishDB`.`ProjectMandant`.`ProjectID` = `baum_process`.`ProjectID`"
                  + " where (`baum_process`.`ProcessID` = `t`.`process_id` and"
                  + " `GlassfishDB`.`ProjectMandant`.`MandantID` = ?)) group by `t`.`process_id`,"
                  + " `t`.`message_status`");
    }

    /**
     * <b>Eigenschaft 4 in der Fassung vom 07.09.2026:</b> Jedes Segment liest seine Ebene und keine
     * andere, und es steht keine Ebene im Text, die kein Segment traegt. Ein leerer Zweig kostete
     * einen Bereichszugriff ueber ein leeres Intervall — nicht falsch, aber ein Zugriff.
     */
    @Test
    @DisplayName("Keine Ebene im Text, die kein Segment traegt")
    void keine_ebene_ohne_segment() {
      // Stunden und Tage, kein Monat: die krummen 30 Tage.
      String ohneMonat =
          kennzahlen(
              List.of(
                  segment(Rollupebene.STUNDE, "2025-11-29T14:00", "2025-11-30T00:00"),
                  segment(Rollupebene.TAG, "2025-11-30T00:00", "2025-12-29T00:00"),
                  segment(Rollupebene.STUNDE, "2025-12-29T00:00", "2025-12-29T14:00")));
      assertThat(ohneMonat)
          .contains("`overlord_monitor`.`message_rollup`.")
          .contains("`overlord_monitor`.`message_rollup_tag`")
          .doesNotContain("`overlord_monitor`.`message_rollup_monat`");

      // Tage und Monate, keine Stunde.
      String ohneStunde =
          kennzahlen(
              List.of(
                  segment(Rollupebene.TAG, "2025-01-30T00:00", "2025-02-01T00:00"),
                  segment(Rollupebene.MONAT, "2025-02-01T00:00", "2025-04-01T00:00"),
                  segment(Rollupebene.TAG, "2025-04-01T00:00", "2025-04-03T00:00")));
      assertThat(ohneStunde)
          .doesNotContain("`overlord_monitor`.`message_rollup`.")
          .contains("`overlord_monitor`.`message_rollup_tag`")
          .contains("`overlord_monitor`.`message_rollup_monat`");
    }

    /**
     * Ein einzelnes Segment — gleich auf welcher Ebene und gleich, ob es aus einem Paar oder einer
     * Zerlegung kommt — rendert die <b>ungeteilte</b> Bauform: kein {@code union}, keine Ableitung.
     * Ein monatsbuendiges Jahr liest damit denselben Text wie {@code 12M}, nur mit anderen Werten.
     */
    @Test
    @DisplayName("Ein Segment rendert die ungeteilte Bauform ohne Ableitung")
    void ein_segment_ungeteilt() {
      List<Segment> jahr = Baumfenster.zerlegung(t("2025-01-01T00:00"), t("2026-01-01T00:00"));
      assertThat(jahr).hasSize(1);
      assertThat(kennzahlen(jahr))
          .isEqualTo(kennzahlen(Rollupzeitraum.MONATE_12))
          .doesNotContain("union")
          .doesNotContain("`t`.");
    }

    /**
     * Eigenschaft 3, <b>fuer jeden Zweig einzeln</b>: Um keine der drei Schluesselspalten steht
     * eine Funktion. Der Vergleichswert fuer {@code tag} und {@code monat} wird in Java auf das
     * Datum geschnitten, nicht in SQL.
     */
    @Test
    @DisplayName("Um den Eimerschluessel steht in keinem Zweig eine Funktion")
    void keine_funktion_in_keinem_zweig() {
      assertThat(kennzahlen(boesfall()))
          .contains("`overlord_monitor`.`message_rollup`.`stunde` >= ?")
          .contains("`overlord_monitor`.`message_rollup_tag`.`tag` >= ?")
          .contains("`overlord_monitor`.`message_rollup_monat`.`monat` >= ?")
          .doesNotContain("date(")
          .doesNotContain("date_format(")
          .doesNotContain("cast(");
    }

    /**
     * Eigenschaft 2, und dazu die Bauform Z-U: Die Mandantenkette steht <b>einmal, aussen</b>, auf
     * der Ableitung — nicht in jedem Zweig (das waere M114 mit doppelter Auswertung) und nicht als
     * Join (das vervielfachte die Summe).
     */
    @Test
    @DisplayName("Die Mandantenkette steht genau einmal, aussen, als EXISTS auf der Ableitung")
    void mandantenkette_einmal_aussen() {
      String text = kennzahlen(boesfall());
      assertThat(text.split("exists \\(", -1)).as("genau ein EXISTS").hasSize(2);
      assertThat(text)
          .contains("where (`baum_process`.`ProcessID` = `t`.`process_id`")
          .doesNotContain("join `overlord_monitor`");
    }

    /**
     * Kopf und Fuss eines Fensters liegen auf derselben Ebene und stehen als <b>zwei Bereiche in
     * einem Zweig</b> — nicht als zwei Zweige. Jeder Bereich ist fuer sich ein Bereichszugriff
     * ueber den Primaerschluessel; M149 hat genau diese Form gemessen.
     */
    @Test
    @DisplayName("Kopf und Fuss derselben Ebene stehen als OR zweier Intervalle in einem Zweig")
    void kopf_und_fuss_als_or() {
      String text = kennzahlen(boesfall());
      assertThat(text.split("from `overlord_monitor`.`message_rollup` where", -1))
          .as("ein Zweig auf der Stundenebene")
          .hasSize(2);
      assertThat(text)
          .contains(
              "(`overlord_monitor`.`message_rollup`.`stunde` >= ? and"
                  + " `overlord_monitor`.`message_rollup`.`stunde` < ?) or"
                  + " (`overlord_monitor`.`message_rollup`.`stunde` >= ? and"
                  + " `overlord_monitor`.`message_rollup`.`stunde` < ?)");
    }

    /**
     * Derselbe Fensterschnitt ergibt denselben Text, gleich in welcher Reihenfolge die Segmente
     * ankommen — die Zweige stehen in der festen Reihenfolge Stunde, Tag, Monat. Sonst zaehlte ein
     * Zwischenspeicher der Datenbank zwei Texte fuer eine Frage.
     */
    @Test
    @DisplayName("Die Reihenfolge der Zweige haengt nicht an der Reihenfolge der Segmente")
    void feste_reihenfolge() {
      List<Segment> vorwaerts = boesfall();
      List<Segment> rueckwaerts = new ArrayList<>(vorwaerts);
      java.util.Collections.reverse(rueckwaerts);

      assertThat(kennzahlen(rueckwaerts)).isEqualTo(kennzahlen(vorwaerts));
    }

    /** Eine Gruppierung, ueber der Ableitung — und kein {@code ORDER BY}, wie bei den Paaren. */
    @Test
    @DisplayName("Gruppiert wird einmal ueber der Ableitung, nach Rohstatus, ohne Sortierung")
    void eine_gruppierung() {
      String text = kennzahlen(boesfall());
      assertThat(text.split("group by", -1)).hasSize(2);
      assertThat(text)
          .endsWith("group by `t`.`process_id`, `t`.`message_status`")
          .doesNotContain("order by")
          .doesNotContain("case");
    }
  }

  // ─── Die Statements eines Aufrufs ─────────────────────────────────────────────

  /**
   * <b>Die Zusicherung „genau zwei Statements je Aufruf und kein {@code Message}" ist am 17.09.2026
   * bewusst gefallen</b> ({@code docs/live-rest.md}). An ihre Stelle treten <b>einzeln benannte
   * Statements</b>, wie in {@code DashboardStatementsTest}: Der Dienst wird gerufen, und jedes
   * Statement, das er absetzt, steht hier mit Namen. {@code Message} steht ausschliesslich im
   * Live-Teil — mit Zeitbereich ab G und Mandantenkette.
   *
   * <p>Die Attrappe antwortet auf die Wasserstandsabfrage mit dem Wert in {@link #wasserstand};
   * daran haengt, welcher der drei Zustaende eintritt und wie viele Statements folgen.
   *
   * <p><b>Seit dem 18.09.2026 steht jede Lage auch als Folge von Namen da</b> ({@link #namen}), wie
   * in {@code DashboardStatementsTest.FehlerLive}: Die Fehlerlesung ist das letzte Statement jedes
   * Aufrufs — sechs bei angewandtem Live-Rest, sonst vier.
   */
  @Nested
  @DisplayName("Die Statements eines Aufrufs, einzeln benannt")
  class EinAufruf {

    private List<String> statementsEinesAufrufs(Baumfenster fenster) {
      ProzessbaumService dienst =
          new ProzessbaumService(
              repository,
              new MessageStatusClassifier(),
              Clock.fixed(JETZT.atZone(ZONE).toInstant(), ZONE),
              new LiveRestService(
                  new LiveRestRepository(kontext), new WasserstandRepository(kontext)),
              new FehlerLiveService(
                  new FehlerLiveRepository(kontext, new MessageStatusClassifier())));
      gerendert.clear();
      dienst.baum(MANDANT, fenster, Baumgliederung.PARTNER);
      return gerendert.stream().map(sql -> sql.replaceAll("\\s+", " ").trim()).toList();
    }

    /** Das Statement bei seinem Gegenstand genannt — jedes Merkmal an genau einer Stelle. */
    private static String name(String sql) {
      if (sql.contains("`fehler_process`")) {
        return "Fehlerlesung";
      }
      if (sql.contains("`live_process`")) {
        return sql.startsWith("select date_format(") ? "Live-Rest B" : "Live-Rest A";
      }
      if (sql.startsWith("select max(`overlord_monitor`.`rollup_lauf`.`fenster_bis`)")) {
        return "Wasserstand";
      }
      if (sql.contains("`baum_process`")) {
        return "Kennzahlen";
      }
      if (sql.startsWith("select `GlassfishDB`.`Process`.`ProcessID`")) {
        return "Geruest";
      }
      return "UNBEKANNT: " + sql;
    }

    private static List<String> namen(List<String> statements) {
      return statements.stream().map(EinAufruf::name).toList();
    }

    /** Die Lesung, wie das Repository aus {@code common} sie fuer dieses Fenster rendert. */
    private String lesungAusCommon(Baumfenster fenster) {
      gerendert.clear();
      new FehlerLiveRepository(kontext, new MessageStatusClassifier())
          .ausDerQuelle(MANDANT, fenster.fenster(JETZT).von(), fenster.fenster(JETZT).bis());
      return einziges();
    }

    private static final String KETTE_AUF_ROLLUP =
        "exists (select 1 as `one` from `GlassfishDB`.`Process` as `live_process` join"
            + " `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` ="
            + " `live_process`.`ProjectID` where (`live_process`.`ProcessID` ="
            + " `overlord_monitor`.`message_rollup`.`process_id` and"
            + " `GlassfishDB`.`ProjectMandant`.`MandantID` = ?))";

    private static final String KETTE_AUF_MESSAGE =
        "exists (select 1 as `one` from `GlassfishDB`.`Process` as `live_process` join"
            + " `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` ="
            + " `live_process`.`ProjectID` where (`live_process`.`ProcessID` ="
            + " `GlassfishDB`.`Message`.`ProcessID` and"
            + " `GlassfishDB`.`ProjectMandant`.`MandantID` = ?))";

    /**
     * Angewandt: G ist die Stichtagsstunde. Sechs Statements, jedes mit Namen — das sechste ist
     * seit dem 18.09.2026 die Fehlerlesung.
     */
    @Test
    @DisplayName(
        "Angewandt: Geruest, Kennzahlen, Wasserstand, Rollup und Message im Live-Bereich,"
            + " Fehlerlesung")
    void angewandt_sechs_statements() {
      wasserstand = JETZT.truncatedTo(java.time.temporal.ChronoUnit.HOURS).plusHours(1);

      List<String> knapp = statementsEinesAufrufs(Baumfenster.paar(Rollupzeitraum.STUNDEN_48));

      assertThat(namen(knapp))
          .as("Sechs Statements — und jedes einzeln benannt")
          .containsExactly(
              "Geruest", "Kennzahlen", "Wasserstand", "Live-Rest A", "Live-Rest B", "Fehlerlesung");
      assertThat(knapp.get(0))
          .as("1 Geruest")
          .startsWith("select `GlassfishDB`.`Process`.`ProcessID`")
          .contains("join `GlassfishDB`.`ProjectMandant` on");
      assertThat(knapp.get(1))
          .as("2 Kennzahlen")
          .startsWith("select `overlord_monitor`.`message_rollup`.`process_id`")
          .contains("sum(`overlord_monitor`.`message_rollup`.`anzahl`)")
          .contains("`baum_process`");
      assertThat(knapp.get(2))
          .as("3 Wasserstand")
          .isEqualTo(
              "select max(`overlord_monitor`.`rollup_lauf`.`fenster_bis`) from"
                  + " `overlord_monitor`.`rollup_lauf` where"
                  + " (`overlord_monitor`.`rollup_lauf`.`beendet_am` is not null and"
                  + " `overlord_monitor`.`rollup_lauf`.`fehler` is null)");
      assertThat(knapp.get(3))
          .as("4 Rollup im Live-Bereich — ab G, ohne Summe, mit Kette")
          .startsWith("select `overlord_monitor`.`message_rollup`.`stunde`")
          .contains("`overlord_monitor`.`message_rollup`.`stunde` >= ?")
          .contains(KETTE_AUF_ROLLUP)
          .doesNotContain("sum(")
          .doesNotContain("group by");
      assertThat(knapp.get(4))
          .as("5 Message im Live-Bereich — ab G, gruppiert wie der Rollup, mit Kette")
          .contains("from `GlassfishDB`.`Message` where")
          .contains("`GlassfishDB`.`Message`.`MessageLastUpdate` >= ?")
          .contains("`GlassfishDB`.`Message`.`MessageLastUpdate` < ?")
          .contains(KETTE_AUF_MESSAGE)
          .contains("group by date_format(");
      assertThat(knapp.get(5))
          .as("6 Fehlerlesung — ueber das Fenster der Antwort, mit eigener Kette, zuletzt")
          .isEqualTo(lesungAusCommon(Baumfenster.paar(Rollupzeitraum.STUNDEN_48)));
    }

    /**
     * <b>{@code Message} steht im Live-Teil und in der Fehlerlesung</b> — seit dem 18.09.2026 in
     * zwei Statements eines angewandten Aufrufs. Regel L2 mit ihrer dritten benannten Ausnahme (der
     * Bereich ab G) und ihrer vierten (die Einordnung {@code FEHLER} im Fenster); keines der beiden
     * joint {@code Message}, beide tragen ihre Kette als {@code EXISTS}.
     */
    @Test
    @DisplayName(
        "Message steht in genau zwei Statements: im Live-Bereich ab G und in der Fehlerlesung")
    void message_im_live_teil_und_in_der_lesung() {
      wasserstand = JETZT.truncatedTo(java.time.temporal.ChronoUnit.HOURS).plusHours(1);

      List<String> knapp = statementsEinesAufrufs(Baumfenster.paar(Rollupzeitraum.STUNDEN_48));
      List<String> mitMessage =
          knapp.stream().filter(sql -> sql.contains("`GlassfishDB`.`Message`")).toList();

      assertThat(namen(mitMessage)).containsExactly("Live-Rest B", "Fehlerlesung");
      assertThat(mitMessage.getFirst())
          .isSameAs(knapp.get(4))
          .contains("`GlassfishDB`.`Message`.`MessageLastUpdate` >= ?")
          .contains(KETTE_AUF_MESSAGE)
          .doesNotContain("join `GlassfishDB`.`Message`");
      assertThat(mitMessage.getLast())
          .isSameAs(knapp.getLast())
          .contains("`GlassfishDB`.`Message`.`MessageLastUpdate` >= ?")
          .contains("exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process`")
          .doesNotContain("join `GlassfishDB`.`Message`");
    }

    /**
     * Ohne Lauf: ausgesetzt — vier Statements; die Quelle liest allein die Fehlerlesung, zuletzt.
     */
    @Test
    @DisplayName("Ausgesetzt ohne Lauf: vier Statements, Message nur in der Fehlerlesung")
    void ausgesetzt_ohne_lauf_vier_statements() {
      wasserstand = null;

      List<String> knapp = statementsEinesAufrufs(Baumfenster.paar(Rollupzeitraum.STUNDEN_48));

      assertThat(namen(knapp))
          .containsExactly("Geruest", "Kennzahlen", "Wasserstand", "Fehlerlesung");
      assertThat(knapp.get(2))
          .as("3 Wasserstand")
          .contains("from `overlord_monitor`.`rollup_lauf`");
      assertThat(knapp.subList(0, 3))
          .allSatisfy(sql -> assertThat(sql).doesNotContain("`GlassfishDB`.`Message`"));
    }

    /** Der Rollup reicht ueber die Uhr hinaus: nicht noetig — dieselben vier. */
    @Test
    @DisplayName("Nicht noetig: vier Statements, Message nur in der Fehlerlesung")
    void nicht_noetig_vier_statements() {
      wasserstand = JETZT.plusDays(2);

      List<String> knapp = statementsEinesAufrufs(Baumfenster.paar(Rollupzeitraum.STUNDEN_48));

      assertThat(namen(knapp))
          .containsExactly("Geruest", "Kennzahlen", "Wasserstand", "Fehlerlesung");
      assertThat(knapp.subList(0, 3))
          .allSatisfy(sql -> assertThat(sql).doesNotContain("`GlassfishDB`.`Message`"));
    }

    /**
     * Auch das freie Fenster kostet bei angewandtem Live-Rest sechs — E-42 bleibt bei zwei fuer den
     * Rollup-Teil, und die Lesung liest das freie Fenster.
     */
    @Test
    @DisplayName("Das freie Fenster setzt dieselben sechs Statements ab")
    void freies_fenster_sechs_statements() {
      wasserstand = JETZT.truncatedTo(java.time.temporal.ChronoUnit.HOURS).plusHours(1);
      Baumfenster frei = Baumfenster.frei(t("2024-12-29T14:00"), t("2025-12-30T03:00"));

      List<String> knapp = statementsEinesAufrufs(frei);

      assertThat(namen(knapp))
          .containsExactly(
              "Geruest", "Kennzahlen", "Wasserstand", "Live-Rest A", "Live-Rest B", "Fehlerlesung");
      assertThat(knapp.get(1)).as("2 Kennzahlen, vereinigt").contains("union all");
      assertThat(knapp.get(4))
          .as("5 Message im Live-Bereich")
          .contains("from `GlassfishDB`.`Message` where");
      assertThat(knapp.get(5)).as("6 Fehlerlesung").isEqualTo(lesungAusCommon(frei));
    }

    /**
     * <b>Die Lesung ist derselbe Text wie in der Uebersicht</b> — dasselbe Repository aus {@code
     * common}, fuer jedes Paar und das freie Fenster; gepinnt bleibt der Text dort ({@code
     * DashboardStatementsTest.FehlerLive.die_lesung_woertlich}), hier wird er gegen das Repository
     * gehalten, damit der Baum keine eigene Fassung bekommt.
     */
    @Test
    @DisplayName("Die Fehlerlesung ist der Text der Uebersicht, in jedem Modus")
    void die_lesung_ist_der_text_der_uebersicht() {
      wasserstand = null;
      List<Baumfenster> modi =
          List.of(
              Baumfenster.paar(Rollupzeitraum.STUNDEN_48),
              Baumfenster.paar(Rollupzeitraum.TAGE_30),
              Baumfenster.paar(Rollupzeitraum.MONATE_12),
              Baumfenster.frei(t("2024-12-29T14:00"), t("2025-12-30T03:00")));

      for (Baumfenster modus : modi) {
        List<String> knapp = statementsEinesAufrufs(modus);
        assertThat(knapp.getLast()).as(modus.code()).isEqualTo(lesungAusCommon(modus));
        assertThat(knapp.getLast())
            .as("%s: keine Sortierung, keine Deckelung, kein Indexhinweis", modus.code())
            .doesNotContain(" order by ")
            .doesNotContain(" limit ")
            .doesNotContain("rows only")
            .doesNotContain("index (");
      }
    }

    /**
     * Die beiden Live-Statements <b>woertlich</b> — das ist die Fassung, die M185 misst (Regel L7),
     * und die Fassung, deren Plan {@code ProzessbaumPlanDbIT} festhaelt.
     */
    @Test
    @DisplayName("Die beiden Live-Statements, woertlich")
    void live_teil_woertlich() {
      wasserstand = JETZT.truncatedTo(java.time.temporal.ChronoUnit.HOURS).plusHours(1);

      List<String> knapp = statementsEinesAufrufs(Baumfenster.paar(Rollupzeitraum.STUNDEN_48));

      assertThat(knapp.get(3))
          .isEqualTo(
              "select `overlord_monitor`.`message_rollup`.`stunde`,"
                  + " `overlord_monitor`.`message_rollup`.`process_id`,"
                  + " `overlord_monitor`.`message_rollup`.`message_status`,"
                  + " `overlord_monitor`.`message_rollup`.`anzahl` from"
                  + " `overlord_monitor`.`message_rollup` where"
                  + " (`overlord_monitor`.`message_rollup`.`stunde` >= ? and"
                  + " `overlord_monitor`.`message_rollup`.`stunde` < ? and "
                  + KETTE_AUF_ROLLUP
                  + ")");
      assertThat(knapp.get(4))
          .isEqualTo(
              "select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d"
                  + " %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`,"
                  + " `GlassfishDB`.`Message`.`MessageStatus`, count(*) from"
                  + " `GlassfishDB`.`Message` where (`GlassfishDB`.`Message`.`MessageLastUpdate`"
                  + " >= ? and `GlassfishDB`.`Message`.`MessageLastUpdate` < ? and "
                  + KETTE_AUF_MESSAGE
                  + ") group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`,"
                  + " '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`,"
                  + " `GlassfishDB`.`Message`.`MessageStatus`");
    }
  }
}
