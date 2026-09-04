package de.kraftwerkone.overlord.monitor.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.common.MessageStatusClassifier;
import de.kraftwerkone.overlord.monitor.common.MessageStatusKind;
import de.kraftwerkone.overlord.monitor.common.Rollupzeitraum;
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

  private String verlauf(Rollupzeitraum zeitraum) {
    gerendert.clear();
    repository.verlauf(MANDANT, zeitraum, zeitraum.fenster(JETZT));
    return einziges();
  }

  private String verteilung(Rollupzeitraum zeitraum, Verteilungssicht sicht) {
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
      assertThat(verlauf(Rollupzeitraum.STUNDEN_48))
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
      assertThat(verlauf(Rollupzeitraum.TAGE_30))
          .contains("`overlord_monitor`.`message_rollup_tag`")
          .doesNotContain("`overlord_monitor`.`message_rollup`.");
      assertThat(verlauf(Rollupzeitraum.MONATE_12))
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
      for (Rollupzeitraum zeitraum : Rollupzeitraum.reihe()) {
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
      for (Rollupzeitraum zeitraum : Rollupzeitraum.reihe()) {
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
      assertThat(verlauf(Rollupzeitraum.STUNDEN_48)).doesNotContain("`GlassfishDB`.`Project`.");
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
      String sql = verteilung(Rollupzeitraum.STUNDEN_48, Verteilungssicht.PARTNER);

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
      assertThat(verteilung(Rollupzeitraum.STUNDEN_48, Verteilungssicht.PARTNER))
          .contains("left outer join `overlord_monitor`.`process_catalog`");
    }

    /** Entscheidung E-i: drei Bedingungen, und alle drei stehen im Ausdruck. */
    @Test
    @DisplayName("E-i steht vollstaendig im CASE: Zeile vorhanden, GEPFLEGT, Feld gefuellt")
    void e_i_vollstaendig() {
      assertThat(verteilung(Rollupzeitraum.STUNDEN_48, Verteilungssicht.PARTNER))
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
      String partner = verteilung(Rollupzeitraum.STUNDEN_48, Verteilungssicht.PARTNER);
      String richtung = verteilung(Rollupzeitraum.STUNDEN_48, Verteilungssicht.RICHTUNG);

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
      assertThat(verteilung(Rollupzeitraum.STUNDEN_48, Verteilungssicht.PARTNER))
          .contains("end is null), sum(`overlord_monitor`.`message_rollup`.`anzahl`) desc");
    }

    @Test
    @DisplayName("Jedes Paar verteilt ueber seine eigene Ebene")
    void je_paar_die_eigene_ebene() {
      assertThat(verteilung(Rollupzeitraum.TAGE_30, Verteilungssicht.PARTNER))
          .contains("from `overlord_monitor`.`message_rollup_tag`");
      assertThat(verteilung(Rollupzeitraum.MONATE_12, Verteilungssicht.PARTNER))
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
          MANDANT, Rollupzeitraum.STUNDEN_48, Rollupzeitraum.STUNDEN_48.fenster(JETZT));

      assertThat(einziges())
          .startsWith("select count(*), max(`belegte_eimer`.`nachrichten`) from (select")
          .contains("group by `overlord_monitor`.`message_rollup`.`stunde`) as `belegte_eimer`");
    }

    @Test
    @DisplayName("Laeuft: = auf den Rohwert, COUNT und MIN in einem Statement")
    void kachel_laeuft() {
      gerendert.clear();
      repository.offeneNachrichten(MANDANT, MessageStatusKind.LAEUFT);

      assertThat(einziges())
          .startsWith(
              "select count(*), min(`GlassfishDB`.`Message`.`MessageLastUpdate`) from"
                  + " `GlassfishDB`.`Message` where")
          .as("= auf den Rohwert und nicht IN ueber eine einelementige Menge")
          .contains("`GlassfishDB`.`Message`.`MessageStatus` = ?")
          .doesNotContain("`GlassfishDB`.`Message`.`MessageStatus` in (")
          .contains("exists (select 1 as `one`");
    }

    /**
     * <b>Regel L9, am Statement nachweisbar.</b> Beide Kacheln haben <b>kein</b> Zeitfenster —
     * gefragt ist, was <i>jetzt</i> offen ist, und ein Fenster schnitte gerade die aeltesten Zeilen
     * weg.
     */
    @Test
    @DisplayName("Wartend traegt kein Zeitfenster, und das ist der Punkt")
    void kachel_wartend_ohne_zeitfenster() {
      gerendert.clear();
      repository.offeneNachrichten(MANDANT, MessageStatusKind.WARTEND);

      assertThat(einziges())
          .contains("`GlassfishDB`.`Message`.`MessageStatus` = ?")
          .doesNotContain("`GlassfishDB`.`Message`.`MessageLastUpdate` >= ?")
          .doesNotContain("`GlassfishDB`.`Message`.`MessageLastUpdate` < ?");
    }

    /**
     * <b>Keine Frist im Statement.</b> Bis zum 03.09.2026 stand hier die Ueberfaelligkeitsbedingung
     * mit {@code date_add(… interval MessageTimeout second)}. Sie ist mit E-71 entfallen; bliebe
     * sie stehen, zaehlte die Kachel etwas anderes, als ihr Name sagt.
     */
    @Test
    @DisplayName("Keine der beiden Kacheln rechnet noch mit einer Frist")
    void kacheln_ohne_frist() {
      for (MessageStatusKind einordnung :
          List.of(MessageStatusKind.LAEUFT, MessageStatusKind.WARTEND)) {
        gerendert.clear();
        repository.offeneNachrichten(MANDANT, einordnung);
        assertThat(einziges())
            .as("%s", einordnung)
            .doesNotContain("date_add(")
            .doesNotContain("MessageTimeout");
      }
    }

    /**
     * <b>Die Erscheinungsbedingung der Kachel <i>Wartend</i></b> (E-74). Sie fragt die
     * <b>Stammdaten</b> und nicht den Bestand: Hat der Mandant einen Prozess, dessen geplanter
     * Ablauf einen {@code SUSPEND}-Baustein traegt?
     */
    @Test
    @DisplayName("Die Erscheinungsbedingung geht ueber SOSAction, SOS und die Mandantenkette")
    void erscheinungsbedingung() {
      gerendert.clear();
      repository.hatWartendeAblaeufe(MANDANT);

      assertThat(einziges())
          .startsWith("select exists (select 1 as `one` from `GlassfishDB`.`SOSAction`")
          .contains("join `GlassfishDB`.`SOS` on `GlassfishDB`.`SOS`.`SOSID` =")
          .as("Das LIKE steht auf dem GEPLANTEN Baustein, nicht auf dem ausgefuehrten")
          .contains("`GlassfishDB`.`SOSAction`.`SOSActionServiceProperties` like ?")
          .as("Die Kette haengt an SOS.ProcessID — Message kommt hier gar nicht vor")
          .contains("`dashboard_process`.`ProcessID` = `GlassfishDB`.`SOS`.`ProcessID`")
          .doesNotContain("`GlassfishDB`.`Message`");
    }

    /**
     * <b>Der Block hatte zwei Haelften, dann eine — und seit dem 04.09.2026 gruppiert er.</b> Die
     * Ueberfaelligkeitshaelfte ist mit E-71 entfallen; E-90 macht aus der Zeile je Nachricht eine
     * Zeile je Prozess. Der Indexhinweis darunter gilt unveraendert (M108, nachgemessen in M146).
     */
    @Test
    @DisplayName("Zuletzt aufgefallen: ein Statement, gruppiert nach Prozess")
    void zuletzt_aufgefallen() {
      gerendert.clear();
      repository.zuletztAufgefallen(MANDANT, Rollupzeitraum.STUNDEN_48.fenster(JETZT), 10);

      assertThat(gerendert).as("Ein Statement, nicht zwei").hasSize(1);
      assertThat(einziges())
          .as("Das LIKE mit ESCAPE und COMMIT_REJECTED, und keine Frist")
          .contains("`GlassfishDB`.`Message`.`MessageStatus` like ? escape")
          .contains("or `GlassfishDB`.`Message`.`MessageStatus` = ?")
          .doesNotContain("date_add(")
          .doesNotContain("`GlassfishDB`.`Message`.`MessageStatus` in (")
          .contains("`GlassfishDB`.`Message`.`MessageLastUpdate` >= ?")
          .contains("rows only")
          .contains("exists (select 1 as `one`");

      assertThat(einziges())
          .as("Gruppiert wird nach dem SCHLUESSEL und dem Namen — nicht nach dem Namen allein")
          .contains(
              "group by `GlassfishDB`.`Message`.`ProcessID`,"
                  + " `GlassfishDB`.`Process`.`ProcessName`")
          .as("Anzahl und juengster Zeitpunkt je Gruppe")
          .contains("count(*)")
          .contains("max(`GlassfishDB`.`Message`.`MessageLastUpdate`)")
          .as("Sortiert wird nach dem juengsten Zeitpunkt, nicht nach der Spalte")
          .contains("order by max(`GlassfishDB`.`Message`.`MessageLastUpdate`) desc")
          .as("Der zweite Schluessel haelt die Reihenfolge fest")
          .contains("`GlassfishDB`.`Message`.`ProcessID` desc");

      assertThat(einziges())
          .as("Der Name kommt aus Process und nicht mehr aus SOS — sonst waere er geraten")
          .contains("left outer join `GlassfishDB`.`Process`")
          .doesNotContain("`GlassfishDB`.`SOS`.`SOSName`");
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
   * <b>Die Statements einer Landingpage — benannt und nicht gezaehlt.</b>
   *
   * <h2>⚠️ Warum dieser Test am 03.09.2026 umgebaut worden ist, obwohl er gruen war</h2>
   *
   * <p>Er hielt fest, dass eine Seite <b>sieben</b> Statements kostet. In diesem Schritt fallen
   * <b>drei</b> weg (zweimal <i>Ueberfaellig</i>, die Ueberfaelligkeitshaelfte von Block 6) und
   * <b>drei</b> kommen hinzu (<i>Laeuft</i>, <i>Wartend</i>, die Erscheinungsbedingung). <b>Die
   * Zahl bleibt sieben — und der Test haette bestanden, ohne noch etwas zu bezeugen.</b>
   *
   * <p>Ein Test, der eine Zahl prueft, wo eine Gestalt gemeint ist, wird in genau dem Augenblick
   * still, in dem sich die Gestalt aendert. Er nennt deshalb jetzt <b>jedes</b> Statement bei
   * seinem Gegenstand. Kommt eines hinzu, faellt er; faellt eines weg, faellt er auch — und die
   * Meldung sagt, welches.
   *
   * <p>Das ist derselbe Befund wie in {@code docs/testfestigkeit.md}: ein gruener Test, der seine
   * Aussage nicht traegt. Gefunden hat ihn nicht das Nachdenken, sondern die Buchhaltung des
   * Auftrags.
   */
  @Test
  @DisplayName("Eine Landingpage mit genanntem Zeitraum setzt genau diese sieben Statements ab")
  void die_sieben_statements_je_seite() {
    Zeitfenster fenster = Rollupzeitraum.STUNDEN_48.fenster(JETZT);
    gerendert.clear();

    repository.verlauf(MANDANT, Rollupzeitraum.STUNDEN_48, fenster);
    repository.verteilung(MANDANT, Rollupzeitraum.STUNDEN_48, fenster, Verteilungssicht.PARTNER);
    repository.offeneNachrichten(MANDANT, MessageStatusKind.LAEUFT);
    repository.hatWartendeAblaeufe(MANDANT);
    repository.offeneNachrichten(MANDANT, MessageStatusKind.WARTEND);
    repository.zuletztAufgefallen(MANDANT, fenster, 10);
    repository.letzterLauf();

    List<String> knapp = gerendert.stream().map(sql -> sql.replaceAll("\s+", " ").trim()).toList();

    assertThat(knapp)
        .as("Sieben Statements — und jedes einzeln benannt, damit ein Tausch auffaellt")
        .hasSize(7);
    assertThat(knapp.get(0)).as("1 Verlauf").contains("from `overlord_monitor`.`message_rollup`");
    assertThat(knapp.get(1))
        .as("2 Verteilung")
        .contains("left outer join `overlord_monitor`.`process_catalog`");
    assertThat(knapp.get(2))
        .as("3 Kachel Laeuft")
        .startsWith("select count(*), min(")
        .contains("`MessageStatus` = ?");
    assertThat(knapp.get(3))
        .as("4 Erscheinungsbedingung der Kachel Wartend")
        .startsWith("select exists (")
        .contains("`GlassfishDB`.`SOSAction`");
    assertThat(knapp.get(4))
        .as("5 Kachel Wartend")
        .startsWith("select count(*), min(")
        .contains("`MessageStatus` = ?");
    assertThat(knapp.get(5))
        .as("6 Zuletzt aufgefallen — eine Haelfte, nicht zwei, und je Prozess gruppiert")
        .contains("`MessageStatus` like ? escape")
        .contains("group by `GlassfishDB`.`Message`.`ProcessID`");
    assertThat(knapp.get(6)).as("7 Stand").contains("from `overlord_monitor`.`rollup_lauf`");
  }

  /**
   * <b>Die Gegenprobe zum Test darueber, und sie ist der Grund fuer diesen Schritt.</b> Kein
   * Statement der Landingpage rechnet noch mit {@code MessageTimeout} — die Problemkategorie
   * <i>Ueberfaellig</i> ist widerlegt (E-71), und ein uebriggebliebenes {@code date_add} waere die
   * Kategorie an einer Stelle, an der niemand sie mehr vermutet.
   */
  @Test
  @DisplayName("Kein Statement der Landingpage rechnet noch mit MessageTimeout")
  void keine_frist_mehr_in_der_ganzen_seite() {
    Zeitfenster fenster = Rollupzeitraum.STUNDEN_48.fenster(JETZT);
    gerendert.clear();

    repository.verlauf(MANDANT, Rollupzeitraum.STUNDEN_48, fenster);
    repository.verteilung(MANDANT, Rollupzeitraum.STUNDEN_48, fenster, Verteilungssicht.PARTNER);
    repository.offeneNachrichten(MANDANT, MessageStatusKind.LAEUFT);
    repository.hatWartendeAblaeufe(MANDANT);
    repository.offeneNachrichten(MANDANT, MessageStatusKind.WARTEND);
    repository.zuletztAufgefallen(MANDANT, fenster, 10);
    repository.letzterLauf();

    assertThat(gerendert)
        .allSatisfy(
            sql -> assertThat(sql).doesNotContain("MessageTimeout").doesNotContain("date_add("));
  }
}
