package de.kraftwerkone.overlord.monitor.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.common.Baumfenster;
import de.kraftwerkone.overlord.monitor.common.Baumfenster.Segment;
import de.kraftwerkone.overlord.monitor.common.Rollupebene;
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
 *   <li>Jedes <b>Segment</b> liest seine Ebene und keine andere, und es steht keine Ebene im Text,
 *       die kein Segment traegt — die Fassung vom 07.09.2026 (E-97); fuer die drei Paare sagt sie
 *       dasselbe wie die alte: <i>Jedes Paar liest seine Ebene und keine andere.</i>
 * </ol>
 *
 * <p><b>Seit dem 07.09.2026 (Schritt 10c-4b) kommt eine fuenfte dazu, und sie steht ueber den
 * anderen:</b> Die drei Paare rendern <b>Zeichen fuer Zeichen den Text von vor diesem Schritt</b>.
 * Der Text ist vor dem Bau der Zerlegung gepinnt worden und hat den Bau ueberstanden.
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
    repository.kennzahlen(MANDANT, Baumfenster.paar(Rollupzeitraum.STUNDEN_48).segmente(JETZT));
    assertThat(gerendert).hasSize(2);
  }

  /**
   * <b>E-42 faellt mit dem freien Zeitfenster nicht.</b> Auch fuenf Segmente ueber drei Ebenen sind
   * <b>eine</b> Kennzahlenabfrage (Bauform Z-U, M149) — nicht drei (Z-D, M150). Faellt dieser Test,
   * hat jemand die Vereinigung in Einzelstatements zerlegt, und die Summierung liefe in Java.
   */
  @Test
  @DisplayName("Auch ein freies Fenster kostet genau zwei Statements")
  void genau_zwei_statements_im_freien_fenster() {
    gerendert.clear();
    repository.geruest(MANDANT);
    repository.kennzahlen(MANDANT, boesfall());
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
      repository.kennzahlen(MANDANT, Baumfenster.paar(zeitraum).segmente(JETZT));
    }
    repository.kennzahlen(MANDANT, boesfall());
    assertThat(gerendert)
        .isNotEmpty()
        .allSatisfy(sql -> assertThat(sql).doesNotContain("`GlassfishDB`.`Message`"));
  }
}
