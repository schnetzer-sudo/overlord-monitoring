package de.kraftwerkone.overlord.monitor.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.common.FehlerLiveRepository;
import de.kraftwerkone.overlord.monitor.common.FehlerLiveService;
import de.kraftwerkone.overlord.monitor.common.LiveRestRepository;
import de.kraftwerkone.overlord.monitor.common.LiveRestService;
import de.kraftwerkone.overlord.monitor.common.MandantContext;
import de.kraftwerkone.overlord.monitor.common.MessageStatusClassifier;
import de.kraftwerkone.overlord.monitor.common.MessageStatusKind;
import de.kraftwerkone.overlord.monitor.common.Rollupzeitraum;
import de.kraftwerkone.overlord.monitor.common.WasserstandRepository;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record4;
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
 * <p><b>Seit dem 17.09.2026 (Live-Rest, Teil B) setzt eine Seite bis zu dreizehn Statements ab</b>
 * — die neun von vorher, den Wasserstand, bei {@code ANGEWANDT} die zwei Live-Lesungen des
 * Bausteins und, nur wenn im Fenster etwas zu verrechnen ist, die Katalog-Nachlesung. Sie stehen
 * einzeln benannt in {@link LiveRest}; die Attrappe stellt den Wasserstand und eine Live-Zeile.
 *
 * <p><b>Seit dem 18.09.2026 (Fehler live, E-208) sind es elf bis vierzehn</b> — die Fehlerlesung
 * steht an zweiter Stelle, vor Block 5, und je nach ihrem Zustand laufen die zwei
 * Verteilungsstatements ohne die Fehler des Rollups oder im heutigen Wortlaut. Jede Lage steht mit
 * ihren Statements einzeln benannt in {@link FehlerLive}; die Attrappe liefert auf Wunsch eine
 * Fehlerzeile oder laesst die Lesung ausfallen.
 *
 * <p>Vorbild ist {@code RollupStatementsTest}; die Bauform ist dieselbe.
 */
class DashboardStatementsTest {

  private static final MandantContext MANDANT = new MandantContext("ERFUNDEN");
  private static final LocalDateTime JETZT = LocalDateTime.parse("2025-12-30T04:09:47");

  private final List<String> gerendert = new ArrayList<>();

  private DashboardRepository repository;

  /**
   * Die zweite Leseklasse der Seite — sie liest {@code Service} und <b>ohne {@code
   * MandantContext}</b> (dritte benannte Ausnahme von Regel M2, E‑123). Sie haengt an derselben
   * Attrappe, damit die Reihenfolge der Statements einer Seite in einer Liste steht.
   */
  private DienstLeseRepository dienstRepository;

  /**
   * Der Baustein aus {@code common} an derselben Attrappe — seine Statements gehoeren zur Seite.
   */
  private LiveRestService liveRestService;

  /** Die Fehlerlesung aus {@code common} an derselben Attrappe (E-208). */
  private FehlerLiveService fehlerLiveService;

  /** Dieselbe Lesung ohne den Dienst — fuer ihren Text allein. */
  private FehlerLiveRepository fehlerLiveRepository;

  /**
   * Was die Attrappe auf die Wasserstandsabfrage antwortet — {@code null} heisst „nie gerechnet",
   * und dann bleibt es bei zehn Statements.
   */
  private LocalDateTime wasserstand;

  /**
   * Ob die Attrappe auf die Live-Lesung aus {@code Message} <b>eine</b> Zeile liefert. Nur dann
   * gibt es etwas zu verrechnen, und nur dann laeuft die Katalog-Nachlesung (E-191).
   */
  private boolean liveZeile;

  /** Der Rohwert dieser Live-Zeile — eine Korrektur, die ein Fehler ist, geht nicht in Block 5. */
  private String liveStatus;

  /**
   * Ob die Attrappe auf die Fehlerlesung <b>eine</b> Zeile liefert ({@code ERROR_TIMEOUT} eines
   * eigenen Prozesses). Dann bekommt Block 5 etwas zuzurechnen, und die Nachlesung laeuft.
   */
  private boolean fehlerZeile;

  /**
   * Ob die Fehlerlesung ausfaellt — die Attrappe wirft, der Baustein setzt aus (E-185 sinngemaess),
   * und die Verteilung laeuft im heutigen Wortlaut.
   */
  private boolean fehlerLiveFaellt;

  @BeforeEach
  void attrappeAufbauen() {
    gerendert.clear();
    wasserstand = null;
    liveZeile = false;
    liveStatus = "FINISHED";
    fehlerZeile = false;
    fehlerLiveFaellt = false;
    MockDataProvider attrappe =
        ausfuehrung -> {
          gerendert.add(ausfuehrung.sql());
          DSLContext leer = DSL.using(SQLDialect.MARIADB);
          if (ausfuehrung.sql().contains("max(`overlord_monitor`.`rollup_lauf`")
              && wasserstand != null) {
            Field<LocalDateTime> max = DSL.field("max", SQLDataType.LOCALDATETIME);
            Result<Record1<LocalDateTime>> ergebnis = leer.newResult(max);
            Record1<LocalDateTime> satz = leer.newRecord(max);
            satz.value1(wasserstand);
            ergebnis.add(satz);
            return new MockResult[] {new MockResult(1, ergebnis)};
          }
          if (ausfuehrung.sql().contains("`fehler_process`") && fehlerLiveFaellt) {
            throw new SQLException("Query execution was interrupted", "70100", 1969);
          }
          if (ausfuehrung.sql().contains("`fehler_process`") && fehlerZeile) {
            return new MockResult[] {zeile(leer, "ERFUNDENER-FEHLERPROZESS", "ERROR_TIMEOUT")};
          }
          if (ausfuehrung.sql().contains("date_format(")
              && ausfuehrung.sql().contains("`live_process`")
              && liveZeile) {
            return new MockResult[] {zeile(leer, "ERFUNDENER-PROZESS", liveStatus)};
          }
          return new MockResult[] {new MockResult(0, leer.newResult())};
        };
    DSLContext kontext = DSL.using(new MockConnection(attrappe), SQLDialect.MARIADB);
    repository = new DashboardRepository(kontext, new MessageStatusClassifier());
    dienstRepository = new DienstLeseRepository(kontext);
    liveRestService =
        new LiveRestService(new LiveRestRepository(kontext), new WasserstandRepository(kontext));
    fehlerLiveRepository = new FehlerLiveRepository(kontext, new MessageStatusClassifier());
    fehlerLiveService = new FehlerLiveService(fehlerLiveRepository);
  }

  /** Eine Zeile der Stundenbildung — dieselbe Gestalt fuer Live-Rest B und die Fehlerlesung. */
  private static MockResult zeile(DSLContext leer, String prozessId, String rohwert) {
    Field<String> stunde = DSL.field("stunde", SQLDataType.VARCHAR);
    Field<String> prozess = DSL.field("ProcessID", SQLDataType.VARCHAR);
    Field<String> status = DSL.field("MessageStatus", SQLDataType.VARCHAR);
    Field<Integer> anzahl = DSL.field("count", SQLDataType.INTEGER);
    Result<Record4<String, String, String, Integer>> ergebnis =
        leer.newResult(stunde, prozess, status, anzahl);
    Record4<String, String, String, Integer> satz = leer.newRecord(stunde, prozess, status, anzahl);
    satz.value1("2025-12-30 04:00:00");
    satz.value2(prozessId);
    satz.value3(rohwert);
    satz.value4(1);
    ergebnis.add(satz);
    return new MockResult(1, ergebnis);
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

  private String verteilungOhneFehler(Rollupzeitraum zeitraum, Verteilungssicht sicht) {
    gerendert.clear();
    repository.verteilungOhneFehler(MANDANT, zeitraum, zeitraum.fenster(JETZT), sicht);
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
   *
   * <p><b>Seit Schritt 10d sind es acht</b>: Der plattformweite Block kostet <b>ein</b> Statement —
   * die Dienste mit Zeitgrenze. <b>Die Ablagenpruefung steht nicht darin und darf es nicht</b>: Sie
   * laeuft im Hintergrund, liest ihre Ziele in ihrem eigenen Takt und wuerde die Seite sonst an die
   * Zeitgrenzen fremder Knoten haengen ({@code docs/dienste.md} §7).
   *
   * <h2>Seit dem 16.09.2026 sind es neun — und die Seite wird nicht mehr nachgestellt</h2>
   *
   * <p>Die Antwort traegt beide Sichten der Verteilung, und das Verteilungsstatement laeuft <b>je
   * Sicht einmal</b> ({@code docs/dashboard.md} §4). Das neunte steht an dritter Stelle und ist
   * hier einzeln benannt.
   *
   * <p><b>Bis zu diesem Tag stellte der Test die Seite von Hand nach</b>: acht Aufrufe am
   * Repository in der Reihenfolge, in der der Service sie macht. Das bewies die Gestalt der
   * Statements, aber nicht, dass die Seite sie absetzt — ein Service, der das zweite
   * Verteilungsstatement gar nicht schickte, haette ihn bestehen lassen. <b>Er ruft deshalb jetzt
   * {@link DashboardService#landingpage} selbst</b>, ueber dieselbe Attrappe; das Repository
   * vertraegt deren leere Ergebnisse ausdruecklich (siehe {@code offeneNachrichten} und {@code
   * hatWartendeAblaeufe}).
   *
   * <h2>Seit dem 17.09.2026 sind es zehn — und bei angewandtem Live-Rest zwoelf oder dreizehn</h2>
   *
   * <p>Das zehnte ist der Wasserstand ({@code common/WasserstandRepository}); die Seite fragt ihn
   * bei jedem Aufruf. Ohne Lauf bleibt es dabei. Was bei {@code ANGEWANDT} dazukommt, steht in
   * {@link LiveRest} — einzeln benannt, nicht gezaehlt.
   *
   * <h2>Seit dem 18.09.2026 sind es elf (Fehler live, E-208)</h2>
   *
   * <p>Die Fehlerlesung steht an <b>zweiter</b> Stelle — vor Block 5, dessen zwei Statements an
   * ihrem Zustand haengen; alle folgenden ruecken um eins. Die Attrappe liefert keine Fehlerzeile,
   * die Lesung ist angewandt, und die Verteilung laeuft deshalb ohne die Fehler des Rollups. <b>Die
   * Zusicherung „ohne Lauf liest keine Seite {@code date_format(}" ist gefallen</b>: Die
   * Fehlerlesung bildet ihre Eimer wie der Rollup-Job. Sie heisst jetzt: Ohne Lauf ist die
   * Fehlerlesung das <i>einzige</i> Statement mit Stundenbildung. Die uebrigen Lagen stehen in
   * {@link FehlerLive}.
   */
  @Test
  @DisplayName(
      "Eine Landingpage mit genanntem Zeitraum setzt genau diese elf Statements ab — neun, die"
          + " Fehlerlesung und den Wasserstand")
  void die_elf_statements_je_seite() {
    List<String> knapp = statementsEinerSeite();

    assertThat(knapp)
        .as("Elf Statements — und jedes einzeln benannt, damit ein Tausch auffaellt")
        .hasSize(11);
    assertThat(knapp.get(0)).as("1 Verlauf").contains("from `overlord_monitor`.`message_rollup`");
    assertThat(knapp.get(1))
        .as("2 Die Fehlerlesung — seit dem 18.09.2026, vor Block 5 (E-208)")
        .isEqualTo(FEHLERLESUNG);
    assertThat(knapp.get(2))
        .as("3 Verteilung, Partnersicht — ohne die Fehler des Rollups, die Lesung ist angewandt")
        .contains("left outer join `overlord_monitor`.`process_catalog`")
        .contains("`overlord_monitor`.`process_catalog`.`partner`")
        .doesNotContain("`overlord_monitor`.`process_catalog`.`richtung`")
        .contains(" and not (`overlord_monitor`.`message_rollup`.`message_status` like ?");
    assertThat(knapp.get(3))
        .as("4 Verteilung, Richtungssicht — seit dem 16.09.2026")
        .contains("left outer join `overlord_monitor`.`process_catalog`")
        .contains("`overlord_monitor`.`process_catalog`.`richtung`")
        .doesNotContain("`overlord_monitor`.`process_catalog`.`partner`");
    assertThat(knapp.get(3).replace("`richtung`", "`partner`"))
        .as("Die Richtungssicht ist die Partnersicht mit der anderen Spalte, Zeichen fuer Zeichen")
        .isEqualTo(knapp.get(2));
    assertThat(knapp.get(4))
        .as("5 Kachel Laeuft")
        .startsWith("select count(*), min(")
        .contains("`MessageStatus` = ?");
    assertThat(knapp.get(5))
        .as("6 Erscheinungsbedingung der Kachel Wartend")
        .startsWith("select exists (")
        .contains("`GlassfishDB`.`SOSAction`");
    assertThat(knapp.get(6))
        .as("7 Kachel Wartend")
        .startsWith("select count(*), min(")
        .contains("`MessageStatus` = ?");
    assertThat(knapp.get(7))
        .as("8 Zuletzt aufgefallen — eine Haelfte, nicht zwei, und je Prozess gruppiert")
        .contains("`MessageStatus` like ? escape")
        .contains("group by `GlassfishDB`.`Message`.`ProcessID`");
    assertThat(knapp.get(8)).as("9 Stand").contains("from `overlord_monitor`.`rollup_lauf`");
    assertThat(knapp.get(9))
        .as("10 Die Dienste mit Zeitgrenze — der plattformweite Block (Schritt 10d)")
        .contains("from `GlassfishDB`.`Service`")
        .contains("`ServiceTimeout` > ?");
    assertThat(knapp.get(10))
        .as("11 Der Wasserstand des Live-Rests (Teil B, 17.09.2026) — ohne Lauf bleibt es dabei")
        .startsWith("select max(`overlord_monitor`.`rollup_lauf`.`fenster_bis`)")
        .contains("`beendet_am` is not null")
        .contains("`fehler` is null");
    assertThat(knapp)
        .as("Ohne Lauf bildet nur die Fehlerlesung Stundeneimer aus Message — der Live-Rest nicht")
        .filteredOn(sql -> sql.contains("date_format("))
        .containsExactly(FEHLERLESUNG);
  }

  /**
   * <b>Kein zusammengelegtes Statement ueber beide Katalogspalten</b> (16.09.2026).
   *
   * <p>Die naheliegende Verdichtung — ein Bereichszugriff statt zwei, gruppiert nach beiden {@code
   * CASE}-Ausdruecken — waere eine <b>andere</b> Abfrage als die gemessene, und sie ist nicht
   * gebaut. Faellt dieser Test, ist sie entstanden: dann gehoert sie gemessen und entschieden,
   * nicht nebenbei eingefuehrt.
   */
  @Test
  @DisplayName("Keine gruppierende Abfrage der Seite liest Partner und Richtung zugleich")
  void kein_zusammengelegtes_verteilungsstatement() {
    for (List<String> knapp :
        List.of(
            statementsEinerSeite(),
            statementsMitKorrekturzeile(),
            statementsMitFehlerzeile(),
            statementsAusgesetzt())) {
      assertThat(knapp)
          .as("Genau zwei Statements gruppieren ueber den Katalog — eines je Sicht")
          .filteredOn(sql -> sql.contains("`overlord_monitor`.`process_catalog`"))
          .filteredOn(sql -> sql.contains("group by"))
          .hasSize(2)
          .noneMatch(
              sql ->
                  sql.contains("`overlord_monitor`.`process_catalog`.`partner`")
                      && sql.contains("`overlord_monitor`.`process_catalog`.`richtung`"));
    }
  }

  /**
   * Die Statements einer Landingpage mit genanntem Zeitraum, <b>so wie der Service sie absetzt</b>
   * — ueber die Attrappe, ohne Datenbank, ohne Ablagenpruefung.
   */
  private List<String> statementsEinerSeite() {
    DashboardService service =
        new DashboardService(
            repository,
            new MessageStatusClassifier(),
            Clock.fixed(JETZT.toInstant(ZoneOffset.UTC), ZoneOffset.UTC),
            dienstRepository,
            new DienstStatusClassifier(),
            Optional.empty(),
            liveRestService,
            fehlerLiveService);
    gerendert.clear();
    service.landingpage(MANDANT, Rollupzeitraum.STUNDEN_48);
    return gerendert.stream().map(sql -> sql.replaceAll("\\s+", " ").trim()).toList();
  }

  /** Die vollste Seite: Live-Rest angewandt, eine Live-Zeile, also auch die Nachlesung. */
  private List<String> statementsMitKorrekturzeile() {
    wasserstand = JETZT.truncatedTo(ChronoUnit.HOURS).plusHours(1);
    liveZeile = true;
    return statementsEinerSeite();
  }

  /** Die Fehlerlesung liefert eine Zeile — Block 5 rechnet sie ueber die Nachlesung zu. */
  private List<String> statementsMitFehlerzeile() {
    fehlerZeile = true;
    return statementsEinerSeite();
  }

  /** Die Fehlerlesung faellt aus — die Seite laeuft im heutigen Wortlaut. */
  private List<String> statementsAusgesetzt() {
    fehlerLiveFaellt = true;
    return statementsEinerSeite();
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
    assertThat(statementsMitKorrekturzeile())
        .as("Die vollste Seite: vierzehn Statements, keines mit einer Frist")
        .hasSize(14)
        .allSatisfy(
            sql -> assertThat(sql).doesNotContain("MessageTimeout").doesNotContain("date_add("));
  }

  // ─── Der Live-Rest der laufenden Stunde (Teil B, 17.09.2026) ────────────────

  /**
   * <b>Was bei angewandtem Live-Rest dazukommt — einzeln benannt.</b> Der Wasserstand steht bei
   * jeder Seite; die Attrappe stellt ihn ({@link #wasserstand}) und liefert auf Wunsch eine
   * Live-Zeile ({@link #liveZeile}). Die Gestalt der zwei Live-Lesungen ist in {@code
   * ProzessbaumStatementsTest.EinAufruf} woertlich gepinnt; hier zaehlt, <b>dass die Seite sie
   * absetzt</b>, in dieser Reihenfolge, und was die Nachlesung dazu liest.
   */
  @Nested
  @DisplayName("Der Live-Rest: Wasserstand, zwei Live-Lesungen, die Nachlesung")
  class LiveRest {

    @Test
    @DisplayName("Nicht noetig (der Lauf deckt die Stunde): elf Statements, kein Live-Rest-Eimer")
    void nicht_noetig_elf() {
      wasserstand = JETZT.plusDays(2);

      List<String> knapp = statementsEinerSeite();

      assertThat(knapp).hasSize(11);
      assertThat(knapp)
          .as("Keine Live-Lesung — die Fehlerlesung bildet ihre Eimer selbst und zaehlt hier nicht")
          .noneMatch(sql -> sql.contains("`live_process`"));
    }

    @Test
    @DisplayName("Angewandt ohne Korrekturzeile: dreizehn — und keine Nachlesung")
    void angewandt_ohne_korrekturzeile_dreizehn() {
      wasserstand = JETZT.truncatedTo(ChronoUnit.HOURS).plusHours(1);

      List<String> knapp = statementsEinerSeite();

      assertThat(knapp).hasSize(13);
      assertThat(knapp.get(11))
          .as("12 Die Rollupzeilen des Live-Bereichs (A)")
          .startsWith("select `overlord_monitor`.`message_rollup`.`stunde`")
          .contains("`overlord_monitor`.`message_rollup`.`stunde` >= ?")
          .contains("`live_process`");
      assertThat(knapp.get(12))
          .as("13 Die Zaehlung aus Message mit der Stundenbildung des Jobs (B)")
          .startsWith("select date_format(")
          .contains("from `GlassfishDB`.`Message`")
          .contains("`live_process`");
      assertThat(knapp)
          .as("Ohne Korrekturzeile keine Nachlesung — sie fragte nach nichts")
          .noneMatch(sql -> sql.contains("`process_catalog`.`process_id` in ("));
    }

    @Test
    @DisplayName("Angewandt mit Korrekturzeile: vierzehn — die Nachlesung ist das letzte")
    void angewandt_mit_korrekturzeile_vierzehn() {
      List<String> knapp = statementsMitKorrekturzeile();

      assertThat(knapp).hasSize(14);
      assertThat(knapp.subList(0, 13))
          .as("Die dreizehn davor sind dieselben wie ohne Korrekturzeile")
          .containsExactlyElementsOf(statementsOhneKorrekturzeileAberAngewandt());
      assertThat(knapp.get(13))
          .as("14 Die Katalog-Nachlesung fuer die Prozesse der Korrekturzeilen (E-191)")
          .isEqualTo(NACHLESUNG);
    }

    private List<String> statementsOhneKorrekturzeileAberAngewandt() {
      liveZeile = false;
      wasserstand = JETZT.truncatedTo(ChronoUnit.HOURS).plusHours(1);
      return statementsEinerSeite();
    }

    /**
     * Die Nachlesung — <b>woertlich</b>: E-i als derselbe {@code CASE} wie im Verteilungsstatement,
     * je Sicht einer, Primaerschluessel im {@code IN}, die Mandantenkette als {@code EXISTS}, und
     * <b>kein {@code GROUP BY}</b>. Genau das unterscheidet sie vom zusammengelegten
     * Verteilungsstatement, das {@link #kein_zusammengelegtes_verteilungsstatement} ausschliesst.
     */
    private static final String NACHLESUNG =
        "select `overlord_monitor`.`process_catalog`.`process_id`, case when"
            + " (`overlord_monitor`.`process_catalog`.`process_id` is not null and"
            + " `overlord_monitor`.`process_catalog`.`pflegestatus` = ? and"
            + " `overlord_monitor`.`process_catalog`.`partner` is not null and"
            + " `overlord_monitor`.`process_catalog`.`partner` <> ?) then"
            + " `overlord_monitor`.`process_catalog`.`partner` end, case when"
            + " (`overlord_monitor`.`process_catalog`.`process_id` is not null and"
            + " `overlord_monitor`.`process_catalog`.`pflegestatus` = ? and"
            + " `overlord_monitor`.`process_catalog`.`richtung` is not null and"
            + " `overlord_monitor`.`process_catalog`.`richtung` <> ?) then"
            + " `overlord_monitor`.`process_catalog`.`richtung` end from"
            + " `overlord_monitor`.`process_catalog` where"
            + " (`overlord_monitor`.`process_catalog`.`process_id` in (?) and exists (select 1 as"
            + " `one` from `GlassfishDB`.`Process` as `dashboard_process` join"
            + " `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` ="
            + " `dashboard_process`.`ProjectID` where (`dashboard_process`.`ProcessID` ="
            + " `overlord_monitor`.`process_catalog`.`process_id` and"
            + " `GlassfishDB`.`ProjectMandant`.`MandantID` = ?)))";

    @Test
    @DisplayName(
        "Die Nachlesung gruppiert nicht, filtert ueber den Schluessel und traegt die Kette")
    void nachlesung_gestalt() {
      String nachlesung = statementsMitKorrekturzeile().get(13);

      assertThat(nachlesung)
          .doesNotContain("group by")
          .doesNotContain("join `overlord_monitor`")
          .contains("`overlord_monitor`.`process_catalog`.`process_id` in (?)")
          .contains("exists (select 1 as `one` from `GlassfishDB`.`Process` as `dashboard_process`")
          .contains("`overlord_monitor`.`process_catalog`.`pflegestatus` = ?")
          .contains("then `overlord_monitor`.`process_catalog`.`partner` end")
          .contains("then `overlord_monitor`.`process_catalog`.`richtung` end");
    }
  }

  // ─── Fehler live (18.09.2026, E-208) ─────────────────────────────────────────

  /**
   * <b>Die Fehlerlesung, woertlich</b> — {@code common/FehlerLiveRepository}: die Fehlerbedingung
   * aus {@code MessageStatusClassifier}, der Zeitbereich ohne Funktion um {@code
   * MessageLastUpdate}, die Kette als {@code EXISTS}, das {@code GROUP BY} ueber den vollen
   * Ausdruck (Befund 11), kein Indexhinweis, keine Sortierung. Genau dieser Text ist in M188
   * gemessen ({@code docs/fehler-live.md} §8).
   */
  static final String FEHLERLESUNG =
      "select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'),"
          + " `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`,"
          + " count(*) from `GlassfishDB`.`Message` where"
          + " ((`GlassfishDB`.`Message`.`MessageStatus` like ? escape '\\\\' or"
          + " `GlassfishDB`.`Message`.`MessageStatus` = ?) and"
          + " `GlassfishDB`.`Message`.`MessageLastUpdate` >= ? and"
          + " `GlassfishDB`.`Message`.`MessageLastUpdate` < ? and exists (select 1 as `one` from"
          + " `GlassfishDB`.`Process` as `fehler_process` join `GlassfishDB`.`ProjectMandant` on"
          + " `GlassfishDB`.`ProjectMandant`.`ProjectID` = `fehler_process`.`ProjectID` where"
          + " (`fehler_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID` and"
          + " `GlassfishDB`.`ProjectMandant`.`MandantID` = ?))) group by"
          + " date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'),"
          + " `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`";

  /** Die Verteilung, Partnersicht, 48 Stunden — woertlich, im heutigen Wortlaut (M178). */
  static final String VERTEILUNG_48H_PARTNER =
      "select case when (`overlord_monitor`.`process_catalog`.`process_id` is not null and"
          + " `overlord_monitor`.`process_catalog`.`pflegestatus` = ? and"
          + " `overlord_monitor`.`process_catalog`.`partner` is not null and"
          + " `overlord_monitor`.`process_catalog`.`partner` <> ?) then"
          + " `overlord_monitor`.`process_catalog`.`partner` end,"
          + " sum(`overlord_monitor`.`message_rollup`.`anzahl`) from"
          + " `overlord_monitor`.`message_rollup` left outer join"
          + " `overlord_monitor`.`process_catalog` on"
          + " `overlord_monitor`.`process_catalog`.`process_id` ="
          + " `overlord_monitor`.`message_rollup`.`process_id` where"
          + " (`overlord_monitor`.`message_rollup`.`stunde` >= ? and"
          + " `overlord_monitor`.`message_rollup`.`stunde` < ? and exists (select 1 as `one` from"
          + " `GlassfishDB`.`Process` as `dashboard_process` join `GlassfishDB`.`ProjectMandant` on"
          + " `GlassfishDB`.`ProjectMandant`.`ProjectID` = `dashboard_process`.`ProjectID` where"
          + " (`dashboard_process`.`ProcessID` = `overlord_monitor`.`message_rollup`.`process_id`"
          + " and `GlassfishDB`.`ProjectMandant`.`MandantID` = ?))) group by case when"
          + " (`overlord_monitor`.`process_catalog`.`process_id` is not null and"
          + " `overlord_monitor`.`process_catalog`.`pflegestatus` = ? and"
          + " `overlord_monitor`.`process_catalog`.`partner` is not null and"
          + " `overlord_monitor`.`process_catalog`.`partner` <> ?) then"
          + " `overlord_monitor`.`process_catalog`.`partner` end order by (case when"
          + " (`overlord_monitor`.`process_catalog`.`process_id` is not null and"
          + " `overlord_monitor`.`process_catalog`.`pflegestatus` = ? and"
          + " `overlord_monitor`.`process_catalog`.`partner` is not null and"
          + " `overlord_monitor`.`process_catalog`.`partner` <> ?) then"
          + " `overlord_monitor`.`process_catalog`.`partner` end is null),"
          + " sum(`overlord_monitor`.`message_rollup`.`anzahl`) desc";

  /**
   * Dieselbe, <b>ohne die Fehler des Rollups</b> — woertlich: genau eine Bedingung mehr, hinter der
   * Mandantenkette, {@code NOT} ueber der Fehlerbedingung auf {@code message_status}.
   */
  static final String VERTEILUNG_OHNE_FEHLER_48H_PARTNER =
      "select case when (`overlord_monitor`.`process_catalog`.`process_id` is not null and"
          + " `overlord_monitor`.`process_catalog`.`pflegestatus` = ? and"
          + " `overlord_monitor`.`process_catalog`.`partner` is not null and"
          + " `overlord_monitor`.`process_catalog`.`partner` <> ?) then"
          + " `overlord_monitor`.`process_catalog`.`partner` end,"
          + " sum(`overlord_monitor`.`message_rollup`.`anzahl`) from"
          + " `overlord_monitor`.`message_rollup` left outer join"
          + " `overlord_monitor`.`process_catalog` on"
          + " `overlord_monitor`.`process_catalog`.`process_id` ="
          + " `overlord_monitor`.`message_rollup`.`process_id` where"
          + " (`overlord_monitor`.`message_rollup`.`stunde` >= ? and"
          + " `overlord_monitor`.`message_rollup`.`stunde` < ? and exists (select 1 as `one` from"
          + " `GlassfishDB`.`Process` as `dashboard_process` join `GlassfishDB`.`ProjectMandant` on"
          + " `GlassfishDB`.`ProjectMandant`.`ProjectID` = `dashboard_process`.`ProjectID` where"
          + " (`dashboard_process`.`ProcessID` = `overlord_monitor`.`message_rollup`.`process_id`"
          + " and `GlassfishDB`.`ProjectMandant`.`MandantID` = ?)) and not"
          + " (`overlord_monitor`.`message_rollup`.`message_status` like ? escape '\\\\' or"
          + " `overlord_monitor`.`message_rollup`.`message_status` = ?)) group by case when"
          + " (`overlord_monitor`.`process_catalog`.`process_id` is not null and"
          + " `overlord_monitor`.`process_catalog`.`pflegestatus` = ? and"
          + " `overlord_monitor`.`process_catalog`.`partner` is not null and"
          + " `overlord_monitor`.`process_catalog`.`partner` <> ?) then"
          + " `overlord_monitor`.`process_catalog`.`partner` end order by (case when"
          + " (`overlord_monitor`.`process_catalog`.`process_id` is not null and"
          + " `overlord_monitor`.`process_catalog`.`pflegestatus` = ? and"
          + " `overlord_monitor`.`process_catalog`.`partner` is not null and"
          + " `overlord_monitor`.`process_catalog`.`partner` <> ?) then"
          + " `overlord_monitor`.`process_catalog`.`partner` end is null),"
          + " sum(`overlord_monitor`.`message_rollup`.`anzahl`) desc";

  /**
   * <b>Fehler live — die Lesung, beide Fassungen der Verteilung und jede Lage mit ihren Statements
   * einzeln benannt</b> ({@code docs/fehler-live.md} §5, §9).
   *
   * <p>Jede Lage steht als Folge von <b>Namen</b> da, nicht als Zahl: Ein Statement, das die Seite
   * nicht mehr absetzt, oder eines an der falschen Stelle faellt auf, auch wenn die Zahl zufaellig
   * gleich bleibt ({@code docs/testfestigkeit.md}).
   */
  @Nested
  @DisplayName("Fehler live: die Lesung, beide Fassungen der Verteilung, jede Lage benannt")
  class FehlerLive {

    /** Die Seite bei angewandter Lesung — die Verteilung ohne die Fehler des Rollups. */
    private static final List<String> SEITE_ANGEWANDT =
        List.of(
            "Verlauf",
            "Fehlerlesung",
            "Verteilung Partner ohne Fehler",
            "Verteilung Richtung ohne Fehler",
            "Offene Kachel",
            "Erscheinungsbedingung",
            "Offene Kachel",
            "Zuletzt aufgefallen",
            "Stand",
            "Dienste",
            "Wasserstand");

    /** Die Seite bei ausgefallener Lesung — die Verteilung im heutigen Wortlaut. */
    private static final List<String> SEITE_AUSGESETZT =
        List.of(
            "Verlauf",
            "Fehlerlesung",
            "Verteilung Partner",
            "Verteilung Richtung",
            "Offene Kachel",
            "Erscheinungsbedingung",
            "Offene Kachel",
            "Zuletzt aufgefallen",
            "Stand",
            "Dienste",
            "Wasserstand");

    /** Das Statement bei seinem Gegenstand genannt — jedes Merkmal an genau einer Stelle. */
    private static String name(String sql) {
      if (sql.contains("`fehler_process`")) {
        return "Fehlerlesung";
      }
      if (sql.contains("`live_process`")) {
        return sql.startsWith("select date_format(") ? "Live-Rest B" : "Live-Rest A";
      }
      if (sql.contains("`overlord_monitor`.`process_catalog`.`process_id` in (")) {
        return "Nachlesung";
      }
      if (sql.contains("left outer join `overlord_monitor`.`process_catalog`")) {
        String sicht =
            sql.contains("`overlord_monitor`.`process_catalog`.`richtung`")
                ? "Richtung"
                : "Partner";
        return "Verteilung " + sicht + (sql.contains(" and not (") ? " ohne Fehler" : "");
      }
      if (sql.startsWith("select max(`overlord_monitor`.`rollup_lauf`.`fenster_bis`)")) {
        return "Wasserstand";
      }
      if (sql.contains("from `overlord_monitor`.`rollup_lauf`")) {
        return "Stand";
      }
      if (sql.contains("from `GlassfishDB`.`Service`")) {
        return "Dienste";
      }
      if (sql.startsWith("select exists (")) {
        return "Erscheinungsbedingung";
      }
      if (sql.startsWith("select count(*), min(")) {
        return "Offene Kachel";
      }
      if (sql.contains("group by `GlassfishDB`.`Message`.`ProcessID`")) {
        return "Zuletzt aufgefallen";
      }
      if (sql.contains("from `overlord_monitor`.`message_rollup`")) {
        return "Verlauf";
      }
      return "UNBEKANNT: " + sql;
    }

    private static List<String> namen(List<String> statements) {
      return statements.stream().map(FehlerLive::name).toList();
    }

    private static List<String> mit(List<String> seite, String... dazu) {
      List<String> alle = new ArrayList<>(seite);
      alle.addAll(List.of(dazu));
      return alle;
    }

    private String nachlesungIn(List<String> statements) {
      return statements.stream()
          .filter(sql -> "Nachlesung".equals(name(sql)))
          .findFirst()
          .orElseThrow();
    }

    @Test
    @DisplayName(
        "Die Lesung, woertlich — und dieselbe fuer jedes Paar, nur die Bindewerte wechseln")
    void die_lesung_woertlich() {
      for (Rollupzeitraum paar : Rollupzeitraum.reihe()) {
        gerendert.clear();
        fehlerLiveRepository.ausDerQuelle(
            MANDANT, paar.fenster(JETZT).von(), paar.fenster(JETZT).bis());
        assertThat(einziges()).as("%s", paar.code()).isEqualTo(FEHLERLESUNG);
      }
    }

    @Test
    @DisplayName(
        "Die Lesung: keine Funktion um den Zeitstempel, voller Ausdruck im GROUP BY, Kette als"
            + " EXISTS, kein Indexhinweis, keine Sortierung, keine Deckelung")
    void die_lesung_gestalt() {
      assertThat(FEHLERLESUNG)
          .as(
              "Der Zeitbereich ist ein Indexbereich — keine Funktion um die Spalte in der Bedingung")
          .contains("`GlassfishDB`.`Message`.`MessageLastUpdate` >= ?")
          .contains("`GlassfishDB`.`Message`.`MessageLastUpdate` < ?")
          .as(
              "Die Fehlerbedingung aus MessageStatusClassifier: LIKE mit ESCAPE und COMMIT_REJECTED")
          .contains("`GlassfishDB`.`Message`.`MessageStatus` like ? escape")
          .contains("or `GlassfishDB`.`Message`.`MessageStatus` = ?")
          .as("Befund 11: der volle Ausdruck im GROUP BY, kein Alias")
          .contains(
              "group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`,"
                  + " '%Y-%m-%d %H:00:00')")
          .doesNotContain(" as `stunde`")
          .as("Die Kette als EXISTS, kein Join gegen ProjectMandant auf der aeusseren Ebene")
          .contains("exists (select 1 as `one` from `GlassfishDB`.`Process` as `fehler_process`")
          .doesNotContain("from `GlassfishDB`.`Message` join")
          .as("Kein Indexhinweis — die Wahl des Statusindex belegt der Plan, nicht der Text")
          .doesNotContain("index (")
          .doesNotContain(" order by ")
          .doesNotContain(" limit ")
          .doesNotContain("rows only");
    }

    @Test
    @DisplayName(
        "Beide Fassungen der Verteilung, woertlich — die ohne Fehler hat genau eine Bedingung mehr")
    void verteilung_beide_fassungen_woertlich() {
      assertThat(verteilung(Rollupzeitraum.STUNDEN_48, Verteilungssicht.PARTNER))
          .as("Die heutige Fassung, Zeichen fuer Zeichen die gemessene (M178)")
          .isEqualTo(VERTEILUNG_48H_PARTNER);
      assertThat(verteilungOhneFehler(Rollupzeitraum.STUNDEN_48, Verteilungssicht.PARTNER))
          .isEqualTo(VERTEILUNG_OHNE_FEHLER_48H_PARTNER);

      for (Rollupzeitraum paar : Rollupzeitraum.reihe()) {
        String tabelle =
            switch (paar) {
              case STUNDEN_48 -> "`overlord_monitor`.`message_rollup`";
              case TAGE_30 -> "`overlord_monitor`.`message_rollup_tag`";
              case MONATE_12 -> "`overlord_monitor`.`message_rollup_monat`";
            };
        for (Verteilungssicht sicht : Verteilungssicht.values()) {
          String heute = verteilung(paar, sicht);
          String ohneFehler = verteilungOhneFehler(paar, sicht);
          assertThat(heute).as("%s/%s", paar.code(), sicht).containsOnlyOnce("))) group by");
          assertThat(ohneFehler)
              .as("%s/%s: dieselbe Abfrage mit NOT fehlerBedingung hinter der Kette", paar, sicht)
              .isEqualTo(
                  heute.replace(
                      "))) group by",
                      ")) and not ("
                          + tabelle
                          + ".`message_status` like ? escape '\\\\' or "
                          + tabelle
                          + ".`message_status` = ?)) group by"));
        }
      }
    }

    @Test
    @DisplayName(
        "Angewandt, kein Fehler im Fenster: elf — die Verteilung ohne Fehler, keine Nachlesung")
    void angewandt_ohne_fehlerzeile() {
      assertThat(namen(statementsEinerSeite())).containsExactlyElementsOf(SEITE_ANGEWANDT);
    }

    @Test
    @DisplayName("Angewandt mit Fehlerzeile: zwoelf — die Nachlesung fuer den Prozess des Fehlers")
    void angewandt_mit_fehlerzeile() {
      List<String> knapp = statementsMitFehlerzeile();

      assertThat(namen(knapp)).containsExactlyElementsOf(mit(SEITE_ANGEWANDT, "Nachlesung"));
      assertThat(knapp.getLast())
          .as("Die Nachlesung ist dieselbe wie fuer den Live-Rest (E-191), mit einer Kennung")
          .isEqualTo(LiveRest.NACHLESUNG);
    }

    @Test
    @DisplayName("Ausgesetzt: elf — die Verteilung im heutigen Wortlaut, Zeichen fuer Zeichen")
    void ausgesetzt_im_heutigen_wortlaut() {
      List<String> knapp = statementsAusgesetzt();

      assertThat(namen(knapp)).containsExactlyElementsOf(SEITE_AUSGESETZT);
      String partner = verteilung(Rollupzeitraum.STUNDEN_48, Verteilungssicht.PARTNER);
      String richtung = verteilung(Rollupzeitraum.STUNDEN_48, Verteilungssicht.RICHTUNG);
      assertThat(knapp.get(2)).isEqualTo(partner).isEqualTo(VERTEILUNG_48H_PARTNER);
      assertThat(knapp.get(3)).isEqualTo(richtung);
    }

    @Test
    @DisplayName("Ausgesetzt, Live-Rest mit Korrekturzeile: vierzehn — wie vor dem 18.09.2026")
    void ausgesetzt_mit_korrekturzeile() {
      fehlerLiveFaellt = true;

      assertThat(namen(statementsMitKorrekturzeile()))
          .containsExactlyElementsOf(
              mit(SEITE_AUSGESETZT, "Live-Rest A", "Live-Rest B", "Nachlesung"));
    }

    @Test
    @DisplayName("Angewandt, Live-Rest ohne Korrekturzeile: dreizehn — keine Nachlesung")
    void angewandt_live_rest_ohne_korrekturzeile() {
      wasserstand = JETZT.truncatedTo(ChronoUnit.HOURS).plusHours(1);

      assertThat(namen(statementsEinerSeite()))
          .containsExactlyElementsOf(mit(SEITE_ANGEWANDT, "Live-Rest A", "Live-Rest B"));
    }

    @Test
    @DisplayName("Angewandt, Live-Rest mit Korrekturzeile: vierzehn — eine Nachlesung")
    void angewandt_mit_korrekturzeile() {
      assertThat(namen(statementsMitKorrekturzeile()))
          .containsExactlyElementsOf(
              mit(SEITE_ANGEWANDT, "Live-Rest A", "Live-Rest B", "Nachlesung"));
    }

    @Test
    @DisplayName(
        "Angewandt mit Fehlerzeile und Korrekturzeile: vierzehn — eine Nachlesung fuer beide")
    void angewandt_mit_fehlerzeile_und_korrekturzeile() {
      fehlerZeile = true;

      List<String> knapp = statementsMitKorrekturzeile();

      assertThat(namen(knapp))
          .containsExactlyElementsOf(
              mit(SEITE_ANGEWANDT, "Live-Rest A", "Live-Rest B", "Nachlesung"));
      assertThat(nachlesungIn(knapp))
          .as("Beide Prozesse in einer Nachlesung — die Korrektur und der Fehler")
          .contains("`overlord_monitor`.`process_catalog`.`process_id` in (?, ?)");
    }

    /**
     * <b>Die Korrekturzeile ist ein Fehler, und die Lesung ist angewandt:</b> Block 5 hat nichts
     * zuzurechnen — die Korrektur darf keinen Fehler nachtragen, den die Verteilung aus der Lesung
     * bekommt —, und die Nachlesung fragte nach nichts. Sie laeuft nicht.
     */
    @Test
    @DisplayName(
        "Angewandt, Korrekturzeile ist ein Fehler: dreizehn — Block 5 hat nichts zuzurechnen")
    void angewandt_korrekturzeile_ist_ein_fehler() {
      liveStatus = "ERROR_TIMEOUT";

      assertThat(namen(statementsMitKorrekturzeile()))
          .containsExactlyElementsOf(mit(SEITE_ANGEWANDT, "Live-Rest A", "Live-Rest B"));
    }
  }

  // ─── Der plattformweite Block (Schritt 10d) ──────────────────────────────────

  /**
   * Die zwei Statements auf {@code Service} — <b>gerendert, weil sich hier zeigt, was gelesen wird
   * und was nicht</b>.
   *
   * <p>Regel G1 haengt an dieser Stelle: Eine Spalte, die im {@code SELECT} nicht vorkommt, kann in
   * keiner Antwort landen. {@code PlattformAntwortTest} prueft dieselbe Zusage von der anderen
   * Seite — an den Typen.
   */
  @Nested
  @DisplayName("Die Dienste und die Pruefziele")
  class Plattform {

    private String einzelnesStatementVon(Runnable aufruf) {
      gerendert.clear();
      aufruf.run();
      return einziges();
    }

    @Test
    @DisplayName("Die Lampen lesen drei Spalten, filtern auf ServiceTimeout > 0 und sortieren")
    void dienste() {
      String sql = einzelnesStatementVon(dienstRepository::dienste);

      assertThat(sql)
          .contains("select `GlassfishDB`.`Service`.`ServiceID`")
          .contains("`GlassfishDB`.`Service`.`ServiceStatus`")
          .contains("`GlassfishDB`.`Service`.`ServiceLastUpdate`")
          .contains("from `GlassfishDB`.`Service`")
          .contains("where `GlassfishDB`.`Service`.`ServiceTimeout` > ?")
          .contains("order by `GlassfishDB`.`Service`.`ServiceID`");
    }

    @Test
    @DisplayName("Und sie lesen keine der vier gesperrten Spalten (Regel G1, E-122)")
    void dienste_lesen_nichts_gesperrtes() {
      String sql = einzelnesStatementVon(dienstRepository::dienste);

      assertThat(sql)
          .as("Was nicht gelesen wird, kann nicht ausgeliefert werden")
          .doesNotContain("ServiceConnectString")
          .doesNotContain("ServiceName")
          .doesNotContain("ServiceDescription")
          .doesNotContain("ServiceLastStatusMessage");
    }

    @Test
    @DisplayName("Die Pruefziele sind ein LEFT JOIN auf dieselbe Tabelle, distinct und sortiert")
    void pruefziele() {
      String sql = einzelnesStatementVon(dienstRepository::pruefziele);

      assertThat(sql)
          .startsWith("select distinct")
          .contains("`GlassfishDB`.`Service`.`ServiceDefaultFileStore`")
          .contains("left outer join `GlassfishDB`.`Service` as `ablagenziel`")
          .contains("`ablagenziel`.`ServiceID` = `GlassfishDB`.`Service`.`ServiceDefaultFileStore`")
          .contains("order by `GlassfishDB`.`Service`.`ServiceDefaultFileStore`");
    }

    @Test
    @DisplayName("Ein LEFT JOIN und kein JOIN — sonst verschwaende ein nicht aufloesbares Ziel")
    void pruefziele_lassen_nichts_verschwinden() {
      // Ein inner join liesse die Kachel gruen bleiben, weil das unaufloesbare Ziel gar nicht erst
      // geprueft wuerde. Die Zeile ist der ganze Unterschied zwischen "rot" und "unbemerkt".
      String sql = einzelnesStatementVon(dienstRepository::pruefziele);

      assertThat(sql).doesNotContain("inner join").doesNotContain("right outer join");
    }

    @Test
    @DisplayName("NULL und Leerstring gelten beide als leer")
    void pruefziele_lassen_leeres_weg() {
      String sql = einzelnesStatementVon(dienstRepository::pruefziele);

      assertThat(sql)
          .contains("`GlassfishDB`.`Service`.`ServiceDefaultFileStore` is not null")
          .contains("`GlassfishDB`.`Service`.`ServiceDefaultFileStore` <> ?");
    }

    @Test
    @DisplayName("Beide Statements tragen keinen Mandantenfilter — es gaebe nichts zu filtern")
    void kein_schein_filter() {
      // Kein ProjectMandant, kein EXISTS, kein Parameter, der wie ein Mandant aussieht: Service
      // kennt keinen. Ein Filter, der nichts filtert, saehe von aussen wie Mandantentrennung aus
      // (E-123).
      for (String sql :
          List.of(
              einzelnesStatementVon(dienstRepository::dienste),
              einzelnesStatementVon(dienstRepository::pruefziele))) {
        assertThat(sql)
            .doesNotContain("ProjectMandant")
            .doesNotContain("MandantID")
            .doesNotContain("exists");
      }
    }

    @Test
    @DisplayName("Und keines von beiden liest Message, MessageProperty oder den Rollup")
    void nur_service() {
      for (String sql :
          List.of(
              einzelnesStatementVon(dienstRepository::dienste),
              einzelnesStatementVon(dienstRepository::pruefziele))) {
        assertThat(sql)
            .doesNotContain("`Message`")
            .doesNotContain("`MessageProperty`")
            .doesNotContain("message_rollup");
      }
    }
  }
}
