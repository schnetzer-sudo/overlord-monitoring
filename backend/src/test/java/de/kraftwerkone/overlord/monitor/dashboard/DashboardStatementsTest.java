package de.kraftwerkone.overlord.monitor.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.common.MessageStatusClassifier;
import de.kraftwerkone.overlord.monitor.common.Zeitfenster;
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
 * Die Statements des Dashboards, <b>gerendert statt nachgebildet</b>: Das Repository laeuft gegen
 * eine jOOQ-Attrappe, und geprueft wird der Text, der wirklich herausfaellt.
 *
 * <p>Regel L7 verlangt die Messung <b>der Abfrage</b> und nicht einer aehnlichen. Dieser Test ist
 * die Bruecke: Er haelt fest, dass das, was {@code docs/dashboard.md} als gemessen ausweist, auch
 * das ist, was der Code schickt.
 *
 * <p><b>Drei Eigenschaften haengen hier und nirgends sonst</b>, weil sie sich nur am Text zeigen
 * und nicht am Ergebnis:
 *
 * <ol>
 *   <li>Die Mandantenkette ist ein {@code EXISTS} und kein {@code JOIN} — ein Join gaebe zu hohe
 *       Summen, und die saehen plausibel aus.
 *   <li>Der {@code CASE} der Verteilung steht als <b>voller Ausdruck</b> im {@code GROUP BY} und
 *       nicht als Alias — Befund 11.
 *   <li>Um die Schluesselspalte der Rollup-Ebene steht <b>keine Funktion</b> — sonst faellt der
 *       Bereichszugriff weg, und die Abfrage waere langsamer, ohne falsch zu sein.
 * </ol>
 *
 * <p>Vorbild ist {@code RollupStatementsTest}; die Bauform ist dieselbe.
 */
class DashboardStatementsTest {

  private static final MandantContext MANDANT = new MandantContext("ERFUNDEN");
  private static final LocalDateTime JETZT = LocalDateTime.parse("2025-12-30T04:09:47");

  private final List<String> gerendert = new ArrayList<>();

  private DashboardRepository repository;

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
        new DashboardRepository(
            DSL.using(new MockConnection(attrappe), SQLDialect.MARIADB),
            new MessageStatusClassifier());
  }

  private String einziges() {
    assertThat(gerendert).hasSize(1);
    return gerendert.getFirst().replaceAll("\\s+", " ").trim();
  }

  private String verlauf(Dashboardzeitraum zeitraum) {
    gerendert.clear();
    repository.verlauf(MANDANT, zeitraum, zeitraum.fenster(JETZT));
    return einziges();
  }

  private String verteilung(Dashboardzeitraum zeitraum, Verteilungssicht sicht) {
    gerendert.clear();
    repository.verteilung(MANDANT, zeitraum, zeitraum.fenster(JETZT), sicht);
    return einziges();
  }

  // ─── Verlauf ──────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("Der Verlauf liest je Paar seine eigene Ebene")
  class Verlauf {

    @Test
    @DisplayName("48H: message_rollup, gruppiert nach stunde und Rohstatus")
    void stundenebene() {
      assertThat(verlauf(Dashboardzeitraum.STUNDEN_48))
          .isEqualTo(
              "select `overlord_monitor`.`message_rollup`.`stunde`,"
                  + " `overlord_monitor`.`message_rollup`.`message_status`,"
                  + " sum(`overlord_monitor`.`message_rollup`.`anzahl`) from"
                  + " `overlord_monitor`.`message_rollup` where"
                  + " (`overlord_monitor`.`message_rollup`.`stunde` >= ? and"
                  + " `overlord_monitor`.`message_rollup`.`stunde` < ? and exists (select 1 as"
                  + " `one` from `GlassfishDB`.`Process` as `dashboard_process` join"
                  + " `GlassfishDB`.`ProjectMandant` on"
                  + " `GlassfishDB`.`ProjectMandant`.`ProjectID` = `dashboard_process`.`ProjectID`"
                  + " where (`dashboard_process`.`ProcessID` ="
                  + " `overlord_monitor`.`message_rollup`.`process_id` and"
                  + " `GlassfishDB`.`ProjectMandant`.`MandantID` = ?))) group by"
                  + " `overlord_monitor`.`message_rollup`.`stunde`,"
                  + " `overlord_monitor`.`message_rollup`.`message_status` order by"
                  + " `overlord_monitor`.`message_rollup`.`stunde`,"
                  + " `overlord_monitor`.`message_rollup`.`message_status`");
    }

    @Test
    @DisplayName("30T liest die Tagesebene, 12M die Monatsebene — und keines die Stundenebene")
    void abgeleitete_ebenen() {
      assertThat(verlauf(Dashboardzeitraum.TAGE_30))
          .contains("`overlord_monitor`.`message_rollup_tag`")
          .doesNotContain("`overlord_monitor`.`message_rollup`.");
      assertThat(verlauf(Dashboardzeitraum.MONATE_12))
          .contains("`overlord_monitor`.`message_rollup_monat`")
          .doesNotContain("`overlord_monitor`.`message_rollup_tag`");
    }

    /**
     * <b>Keine Funktion um die Schluesselspalte.</b> Ein {@code CAST}, das die drei Ebenen auf
     * einen gemeinsamen Typ braechte, stuende im {@code WHERE} und im {@code GROUP BY} — und
     * kostete den Bereichszugriff. Der Fehler waere kein falsches Ergebnis, sondern eine Abfrage,
     * die zehnmal so lange laeuft.
     */
    @Test
    @DisplayName("Um den Eimerschluessel steht keine Funktion")
    void kein_cast_um_den_schluessel() {
      for (Dashboardzeitraum zeitraum : Dashboardzeitraum.reihe()) {
        assertThat(verlauf(zeitraum))
            .as("%s", zeitraum.code())
            .doesNotContain("cast(")
            .doesNotContain("date(")
            .doesNotContain("date_format(");
      }
    }

    /**
     * <b>{@code EXISTS} und nicht {@code JOIN}</b> — {@code ProjectMandant} ist n:m, ein Join
     * vervielfachte die Rollupzeilen und damit die Summe. Der Fehler waere still.
     */
    @Test
    @DisplayName("Die Mandantenkette ist ein EXISTS und steht im Statement (M3)")
    void mandantenkette_ist_exists() {
      for (Dashboardzeitraum zeitraum : Dashboardzeitraum.reihe()) {
        String sql = verlauf(zeitraum);
        assertThat(sql).as("%s", zeitraum.code()).contains("exists (select 1 as `one`");
        assertThat(sql)
            .as("Kein Join gegen ProjectMandant auf der aeusseren Ebene (%s)", zeitraum.code())
            .doesNotContain("from `overlord_monitor`.`message_rollup` join");
      }
    }

    /**
     * {@code Project} ist aus der Kette entbehrlich (Befund 48). Die Messskripte fahren die lange
     * Fassung; der Anwendungscode nimmt durchgehend die kurze, und das steht hier fest.
     */
    @Test
    @DisplayName("Project steht nicht in der Mandantenkette")
    void ohne_project() {
      assertThat(verlauf(Dashboardzeitraum.STUNDEN_48)).doesNotContain("`GlassfishDB`.`Project`.");
    }
  }

  // ─── Verteilung ───────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("Die Verteilung")
  class Verteilung {

    /**
     * <b>Befund 11, maschinell festgehalten.</b> MariaDB loest {@code GROUP BY} zuerst gegen
     * Tabellenspalten auf. Stuende dort ein Alias {@code partner}, gruppierte die Datenbank still
     * nach {@code c.partner} statt nach dem Ausdruck — bei zwei von drei Mandanten sieht das
     * Ergebnis dann trotzdem richtig aus.
     */
    @Test
    @DisplayName("Der CASE steht als voller Ausdruck im GROUP BY und nicht als Alias")
    void case_ohne_alias() {
      String sql = verteilung(Dashboardzeitraum.STUNDEN_48, Verteilungssicht.PARTNER);

      assertThat(sql).contains("group by case when (");
      assertThat(sql)
          .as("Und im ORDER BY ebenso — auch dort bindet ein Alias an die Spalte")
          .contains("order by (case when (");
      assertThat(sql)
          .as("Es gibt gar keinen Alias, an den etwas binden koennte")
          .doesNotContain("as `partner`");
    }

    /**
     * <b>{@code LEFT JOIN} und nicht {@code JOIN}.</b> {@code WOC} hat keine einzige Katalogzeile;
     * ein innerer Join verloere seine vier Prozesse still — und damit ausgerechnet die Zeilen, die
     * als „nicht zugeordnet" erscheinen muessten.
     */
    @Test
    @DisplayName("Der Katalog haengt als LEFT JOIN an, nie als JOIN")
    void katalog_ist_left_join() {
      assertThat(verteilung(Dashboardzeitraum.STUNDEN_48, Verteilungssicht.PARTNER))
          .contains("left outer join `overlord_monitor`.`process_catalog`");
    }

    /** Entscheidung E-i: drei Bedingungen, und alle drei stehen im Ausdruck. */
    @Test
    @DisplayName("E-i steht vollstaendig im CASE: Zeile vorhanden, GEPFLEGT, Feld gefuellt")
    void e_i_vollstaendig() {
      assertThat(verteilung(Dashboardzeitraum.STUNDEN_48, Verteilungssicht.PARTNER))
          .contains("`overlord_monitor`.`process_catalog`.`process_id` is not null")
          .contains("`overlord_monitor`.`process_catalog`.`pflegestatus` = ?")
          .contains("`overlord_monitor`.`process_catalog`.`partner` is not null")
          .contains("`overlord_monitor`.`process_catalog`.`partner` <> ?");
    }

    /**
     * Der {@code pflegestatus}-Riegel steht auch in der Richtungssicht. <b>Heute ist er dort
     * folgenlos</b> — nach der Kuratierung tragen alle Zeilen mit Richtung {@code GEPFLEGT} —, aber
     * die Regel ist E-i und nicht der Zufall dieses Katalogstands.
     */
    @Test
    @DisplayName(
        "Die Richtungssicht ist dasselbe Statement mit der anderen Spalte, Riegel inklusive")
    void richtung_ist_dieselbe_form() {
      String partner = verteilung(Dashboardzeitraum.STUNDEN_48, Verteilungssicht.PARTNER);
      String richtung = verteilung(Dashboardzeitraum.STUNDEN_48, Verteilungssicht.RICHTUNG);

      assertThat(richtung)
          .contains("`overlord_monitor`.`process_catalog`.`richtung`")
          .contains("`overlord_monitor`.`process_catalog`.`pflegestatus` = ?")
          .doesNotContain("`overlord_monitor`.`process_catalog`.`partner`");
      assertThat(richtung.replace("`richtung`", "`partner`"))
          .as("Bis auf die Spalte Zeichen fuer Zeichen dasselbe Statement")
          .isEqualTo(partner);
    }

    /**
     * <b>„Nicht zugeordnet" ist keine Rangposition.</b> {@code ORDER BY (schluessel IS NULL)} zieht
     * den Eimer ohne Namen ans Ende, damit die Raenge 1…k lueckenlos den benannten Werten gehoeren
     * und die Restzeile „Übrige" nie den Eimer ohne Namen enthaelt.
     */
    @Test
    @DisplayName("Sortiert wird erst nach „ist null“, dann absteigend nach Summe")
    void sortierung() {
      assertThat(verteilung(Dashboardzeitraum.STUNDEN_48, Verteilungssicht.PARTNER))
          .contains("end is null), sum(`overlord_monitor`.`message_rollup`.`anzahl`) desc");
    }

    @Test
    @DisplayName("Jedes Paar verteilt ueber seine eigene Ebene")
    void je_paar_die_eigene_ebene() {
      assertThat(verteilung(Dashboardzeitraum.TAGE_30, Verteilungssicht.PARTNER))
          .contains("from `overlord_monitor`.`message_rollup_tag`");
      assertThat(verteilung(Dashboardzeitraum.MONATE_12, Verteilungssicht.PARTNER))
          .contains("from `overlord_monitor`.`message_rollup_monat`");
    }
  }

  // ─── Belegung, Ueberfaellig, Block 6 und 7 ────────────────────────────────────

  @Nested
  @DisplayName("Die uebrigen Statements")
  class Uebrige {

    @Test
    @DisplayName("Die Belegungsprobe ist ein Statement: innen je Eimer summiert, aussen gezaehlt")
    void belegung() {
      repository.belegung(
          MANDANT, Dashboardzeitraum.STUNDEN_48, Dashboardzeitraum.STUNDEN_48.fenster(JETZT));

      assertThat(einziges())
          .startsWith("select count(*), max(`belegte_eimer`.`nachrichten`) from (select")
          .contains("group by `overlord_monitor`.`message_rollup`.`stunde`) as `belegte_eimer`");
    }

    @Test
    @DisplayName("Ueberfaellig im Fenster: Statusmenge, Frist, Zeitfenster und Mandantenkette")
    void ueberfaellig_im_fenster() {
      repository.ueberfaelligImFenster(MANDANT, Dashboardzeitraum.STUNDEN_48.fenster(JETZT), JETZT);

      assertThat(einziges())
          .startsWith("select count(*) from `GlassfishDB`.`Message` where")
          .contains("`GlassfishDB`.`Message`.`MessageStatus` in (?, ?)")
          .contains(
              "date_add(`GlassfishDB`.`Message`.`MessageLastUpdate`, interval"
                  + " `GlassfishDB`.`Message`.`MessageTimeout` second) < ?")
          .contains("`GlassfishDB`.`Message`.`MessageLastUpdate` >= ?")
          .contains("exists (select 1 as `one`");
    }

    /**
     * <b>Regel L9, am Statement nachweisbar.</b> Die zweite Zahl hat kein Zeitfenster — gefragt ist
     * genau, was <i>ausserhalb</i> des gezeigten Zeitraums haengt. Sie ist dabei die billigere von
     * beiden (M90, Befund 14).
     */
    @Test
    @DisplayName("Ueberfaellig insgesamt hat kein Zeitfenster, und das ist der Unterschied")
    void ueberfaellig_insgesamt() {
      repository.ueberfaelligInsgesamt(MANDANT, JETZT);

      assertThat(einziges())
          .contains("`GlassfishDB`.`Message`.`MessageStatus` in (?, ?)")
          .doesNotContain("`GlassfishDB`.`Message`.`MessageLastUpdate` >= ?")
          .doesNotContain("`GlassfishDB`.`Message`.`MessageLastUpdate` < ?");
    }

    /**
     * <b>Die Fehlerbedingung ist die gerufene und nicht das naive {@code LIKE 'ERROR_%'}.</b> In
     * SQL ist {@code _} ein Platzhalter fuer ein beliebiges Zeichen; ohne {@code ESCAPE} traefe die
     * Bedingung auch {@code ERRORX…}.
     */
    @Test
    @DisplayName("Zuletzt aufgefallen: zwei Statements, je Merkmal eines")
    void zuletzt_aufgefallen() {
      gerendert.clear();
      repository.zuletztAufgefallen(
          MANDANT, Dashboardzeitraum.STUNDEN_48.fenster(JETZT), JETZT, 10);

      assertThat(gerendert)
          .as(
              "Mit einem gemeinsamen OR steigt MariaDB ueber MessageLastUpdateIDX ein und liest den"
                  + " ganzen Zeitbereich (M108) — je Merkmal ein Statement dreht den Plan um")
          .hasSize(2);
      List<String> beide =
          gerendert.stream().map(sql -> sql.replaceAll("\\s+", " ").trim()).toList();
      String fehler =
          beide.stream().filter(sql -> sql.contains("like ?")).findFirst().orElseThrow();
      String ueberfaellig =
          beide.stream().filter(sql -> sql.contains("date_add(")).findFirst().orElseThrow();

      assertThat(fehler)
          .as("Die Fehlerhaelfte: das LIKE mit ESCAPE und COMMIT_REJECTED, und keine Frist")
          .contains("`GlassfishDB`.`Message`.`MessageStatus` like ? escape")
          .contains("or `GlassfishDB`.`Message`.`MessageStatus` = ?")
          .doesNotContain("date_add(");
      assertThat(ueberfaellig)
          .as("Die Ueberfaelligkeitshaelfte: die offene Statusmenge und die Frist, und kein LIKE")
          .contains("`GlassfishDB`.`Message`.`MessageStatus` in (?, ?)")
          .contains("date_add(`GlassfishDB`.`Message`.`MessageLastUpdate`, interval")
          .doesNotContain("like ?");
      for (String sql : List.of(fehler, ueberfaellig)) {
        assertThat(sql)
            .contains("`GlassfishDB`.`Message`.`MessageLastUpdate` >= ?")
            .contains(
                "order by `GlassfishDB`.`Message`.`MessageLastUpdate` desc,"
                    + " `GlassfishDB`.`Message`.`MessageID` desc")
            .contains("rows only")
            .contains("exists (select 1 as `one`");
      }
    }

    /**
     * Dieselbe Bedingung wie der Wasserstand des Jobs. Waere sie eine andere, zeigte die Seite
     * einen Stand an, den der Job selbst nicht anerkennt.
     */
    @Test
    @DisplayName("Der Stand nimmt nur abgeschlossene, fehlerfreie Laeufe")
    void stand() {
      repository.letzterLauf();

      assertThat(einziges())
          .contains("`overlord_monitor`.`rollup_lauf`.`beendet_am` is not null")
          .contains("`overlord_monitor`.`rollup_lauf`.`fehler` is null")
          .contains("order by `overlord_monitor`.`rollup_lauf`.`beendet_am` desc");
    }
  }

  /**
   * <b>Die Zahl der Statements einer Landingpage.</b> Sie steht hier, weil sie sonst unbemerkt
   * wachsen kann: Ein Block, der sich seine Zahl selbst nachholt, faellt in keinem fachlichen Test
   * auf — nur in der Laufzeit, und dort erst in Produktion.
   */
  @Test
  @DisplayName("Eine Landingpage mit genanntem Zeitraum kostet sieben Statements")
  void sieben_statements_je_seite() {
    Zeitfenster fenster = Dashboardzeitraum.STUNDEN_48.fenster(JETZT);
    gerendert.clear();

    repository.verlauf(MANDANT, Dashboardzeitraum.STUNDEN_48, fenster);
    repository.verteilung(MANDANT, Dashboardzeitraum.STUNDEN_48, fenster, Verteilungssicht.PARTNER);
    repository.ueberfaelligImFenster(MANDANT, fenster, JETZT);
    repository.ueberfaelligInsgesamt(MANDANT, JETZT);
    repository.zuletztAufgefallen(MANDANT, fenster, JETZT, 10);
    repository.letzterLauf();

    assertThat(gerendert)
        .as("Verlauf, Verteilung, zweimal Ueberfaellig, zweimal Block 6 und der Stand — sieben")
        .hasSize(7);
  }
}
