package de.kraftwerkone.overlord.monitor.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.common.Baumfenster;
import de.kraftwerkone.overlord.monitor.common.Baumfenster.Segment;
import de.kraftwerkone.overlord.monitor.common.Rollupzeitraum;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.conf.Settings;
import org.jooq.conf.StatementType;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockDataProvider;
import org.jooq.tools.jdbc.MockResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * <b>Der Plantest der Prozessansicht</b> — er prueft <b>Treibertabelle und Index, nicht die
 * Laufzeit</b> (Regel T1).
 *
 * <h2>Warum es diesen Test gibt, und zwar genau hier</h2>
 *
 * <p>Die teuerste Entscheidung dieses Schritts steht <b>nicht</b> im Statementtext, sondern im
 * Zugriffspfad: {@code message_rollup_prozess_idx (process_id, stunde)} traegt die Abfrage nach der
 * letzten Bewegung. <b>Ohne ihn kostet dasselbe Statement das Achthundert- bis
 * Zweitausenddreihundertfache</b> — 2,858 s statt 3,520 ms bei {@code NEXANS}, 6,083 s statt 2,598
 * ms bei {@code VOTG} (M115, gemessen mit {@code IGNORE INDEX}).
 *
 * <p><b>Der Index gehoert diesem Projekt und nicht dieser Ansicht.</b> Er ist am 30.08.2026 fuer
 * die Fensterverengung der Nachrichtenliste angelegt worden ({@code V11}, {@code docs/rollup.md}
 * §9b) — die Prozessansicht ist sein <b>zweiter</b> Verbraucher und hat ihn vorgefunden. Genau
 * deshalb steht hier ein Waechter: Wer ihn eines Tages fuer den einen Verbraucher aendert, soll es
 * beim anderen merken.
 *
 * <p><b>Was er absichtlich nicht prueft:</b> Zeilenschaetzungen, Kosten und die Reihenfolge der
 * Tabellen. Sie haengen an der Statistik, und eine Statistik festzuschreiben hiesse, den Test bei
 * der naechsten Analyse rot zu machen, ohne dass jemand etwas falsch gemacht haette — dieselbe
 * Ueberlegung wie in {@code DashboardPlanDbIT}.
 *
 * <p>Der {@code EXPLAIN} laeuft ueber das <b>gerenderte</b> Statement mit Literalen ({@code
 * StatementType.STATIC_STATEMENT}) — mit Fragezeichen gaebe es keine Bereichsanalyse.
 */
@SpringBootTest
@ActiveProfiles("dev")
@Tag("db")
class ProzessbaumPlanDbIT {

  /** Der Anker der Anwendungsuhr im Profil {@code dev} — derselbe Stichtag wie in M112 bis M115. */
  private static final LocalDateTime ANKER = LocalDateTime.parse("2025-12-30T04:09:47");

  /**
   * Drei Groessenordnungen (Regel L7): der groesste Mandant, ein mittlerer und ein kleiner.
   *
   * <p>{@code VOTG} steht dabei nicht wegen seiner Groesse in der Liste, sondern weil er in M115
   * der <b>teuerste</b> Fall ohne Index war — 6,083 s. Cross-Mandanten-Instabilitaet ist in diesem
   * Projekt gemessen worden, und ein Plantest ueber nur einen Mandanten faende sie nicht.
   */
  private static final List<String> MANDANTEN = List.of("NEXANS", "VOTG", "IBIS", "SUTTONS");

  /** Der Index, an dem die ganze Abfrage nach der letzten Bewegung haengt ({@code V11}). */
  private static final String PROZESSINDEX = "message_rollup_prozess_idx";

  @Autowired
  @Qualifier("glassfishDsl") private DSLContext glassfishDsl;

  private final List<String> gerendert = new ArrayList<>();
  private ProzessbaumRepository attrappe;

  /** Eine Zeile aus {@code EXPLAIN} — nur die Spalten, die eine Aussage tragen. */
  private record Plan(String tabelle, String zugriff, String index, String extra) {}

  @BeforeEach
  void attrappeAufbauen() {
    gerendert.clear();
    MockDataProvider mock =
        ausfuehrung -> {
          gerendert.add(ausfuehrung.sql());
          DSLContext leer = DSL.using(SQLDialect.MARIADB);
          return new MockResult[] {new MockResult(0, leer.newResult())};
        };
    attrappe =
        new ProzessbaumRepository(
            DSL.using(
                new MockConnection(mock),
                SQLDialect.MARIADB,
                new Settings().withStatementType(StatementType.STATIC_STATEMENT)));
  }

  private String geruestSql(String mandant) {
    gerendert.clear();
    attrappe.geruest(new MandantContext(mandant));
    assertThat(gerendert).hasSize(1);
    return gerendert.getFirst();
  }

  private String kennzahlenSql(String mandant, Rollupzeitraum zeitraum) {
    return kennzahlenSql(mandant, Baumfenster.paar(zeitraum).segmente(ANKER));
  }

  private String kennzahlenSql(String mandant, List<Segment> segmente) {
    gerendert.clear();
    attrappe.kennzahlen(new MandantContext(mandant), segmente);
    assertThat(gerendert).hasSize(1);
    return gerendert.getFirst();
  }

  /**
   * Die drei Fensterschnitte des freien Modus, die M152 misst: der Boesfall aus M149 (fuenf
   * Segmente, alle drei Ebenen), krumme 30 Tage (Stunden und Tage, kein Monat) und ein
   * monatsbuendiges Jahr (ein Monatssegment — die ungeteilte Bauform mit anderen Werten).
   *
   * <p>Alle drei enden auf oder vor dem Anker der Dev-Uhr, im dichten Bestand der Testkopie.
   */
  private static final List<List<Segment>> FREIE_FENSTER =
      List.of(
          Baumfenster.zerlegung(
              LocalDateTime.parse("2024-12-29T14:00"), LocalDateTime.parse("2025-12-30T03:00")),
          Baumfenster.zerlegung(
              LocalDateTime.parse("2025-11-29T14:00"), LocalDateTime.parse("2025-12-29T14:00")),
          Baumfenster.zerlegung(
              LocalDateTime.parse("2025-01-01T00:00"), LocalDateTime.parse("2026-01-01T00:00")));

  private List<Plan> plan(String sql) {
    List<Plan> zeilen = new ArrayList<>();
    for (Record satz : glassfishDsl.fetch("explain " + sql)) {
      zeilen.add(
          new Plan(
              String.valueOf(satz.get("table")),
              String.valueOf(satz.get("type")),
              String.valueOf(satz.get("key")),
              String.valueOf(satz.get("Extra")).toLowerCase(Locale.ROOT)));
    }
    assertThat(zeilen).as("Ein leerer Plan waere kein Plan").isNotEmpty();
    return zeilen;
  }

  /**
   * Die Planzeile zu einer Tabelle.
   *
   * <p><b>Geprueft wird, wie eine Tabelle erreicht wird, und nicht, an welcher Stelle sie
   * steht.</b> Welche Tabelle den Einstieg macht, haengt am Mandanten und an der Statistik; eine
   * Reihenfolge festzuschreiben machte den Test bei der naechsten Analyse rot, ohne dass jemand
   * etwas falsch gemacht haette.
   */
  private static Plan zeileFuer(List<Plan> plan, String tabelle, String marke) {
    return plan.stream()
        .filter(zeile -> zeile.tabelle().equals(tabelle))
        .findFirst()
        .orElseThrow(
            () ->
                new AssertionError(
                    "Im Plan von "
                        + marke
                        + " kommt die Tabelle "
                        + tabelle
                        + " gar nicht vor. Gefunden: "
                        + plan));
  }

  // ─── Das Geruest ──────────────────────────────────────────────────────────────

  /**
   * <b>Die tragende Zusicherung dieses Tests.</b> Die Unterabfrage nach der letzten Bewegung muss
   * ueber {@link #PROZESSINDEX} laufen. Faellt sie auf den Primaerschluessel oder auf einen
   * Durchlauf zurueck, liest sie je Prozess die Tabelle statt einer Indexposition — und die Ansicht
   * kostet Sekunden statt Millisekunden, ohne falsch zu werden.
   */
  @Test
  @DisplayName("Die letzte Bewegung laeuft ueber message_rollup_prozess_idx")
  void letzte_bewegung_ueber_den_prozessindex() {
    for (String mandant : MANDANTEN) {
      List<Plan> plan = plan(geruestSql(mandant));
      Plan rollup = zeileFuer(plan, "message_rollup", "Geruest/" + mandant);

      assertThat(rollup.index())
          .as("Geruest/%s: Treiberindex der letzten Bewegung", mandant)
          .isEqualTo(PROZESSINDEX);
      assertThat(rollup.zugriff())
          .as("Geruest/%s: Zugriffsart auf message_rollup", mandant)
          .isIn("ref", "index_subquery");
      assertThat(rollup.extra())
          .as("Geruest/%s: die Unterabfrage bleibt im Index", mandant)
          .contains("using index");
    }
  }

  /**
   * Die Gegenprobe: Der Primaerschluessel {@code (stunde, process_id, message_status)} beantwortet
   * diese Frage <b>nicht</b> — er fuehrt ueber die Zeit und muesste danach jede Zeile auf ihren
   * Prozess pruefen. Steht er hier, ist der Index aus {@code V11} weg oder unbrauchbar geworden.
   */
  @Test
  @DisplayName("Die letzte Bewegung laeuft nicht ueber den Primaerschluessel")
  void letzte_bewegung_nicht_ueber_primary() {
    for (String mandant : MANDANTEN) {
      List<Plan> plan = plan(geruestSql(mandant));
      assertThat(zeileFuer(plan, "message_rollup", "Geruest/" + mandant).index())
          .as("Geruest/%s", mandant)
          .isNotEqualTo("PRIMARY");
    }
  }

  /** Regel M3, im Plan: Die Kette steigt ueber den Mandantenindex ein und nicht ueber alles. */
  @Test
  @DisplayName("Das Geruest steigt ueber ProjectMandant_Mandant_idx ein")
  void geruest_steigt_ueber_den_mandantenindex_ein() {
    for (String mandant : MANDANTEN) {
      List<Plan> plan = plan(geruestSql(mandant));
      Plan kette = zeileFuer(plan, "ProjectMandant", "Geruest/" + mandant);

      assertThat(kette.index()).as("Geruest/%s", mandant).isEqualTo("ProjectMandant_Mandant_idx");
      assertThat(kette.zugriff()).as("Geruest/%s", mandant).isEqualTo("ref");
    }
  }

  /** Der Katalog haengt am Primaerschluessel — eine Zeile je Prozess, nicht mehr. */
  @Test
  @DisplayName("Der Katalog wird ueber seinen Primaerschluessel erreicht")
  void katalog_ueber_primaerschluessel() {
    for (String mandant : MANDANTEN) {
      List<Plan> plan = plan(geruestSql(mandant));
      Plan katalog = zeileFuer(plan, "process_catalog", "Geruest/" + mandant);

      assertThat(katalog.zugriff()).as("Geruest/%s", mandant).isEqualTo("eq_ref");
      assertThat(katalog.index()).as("Geruest/%s", mandant).isEqualTo("PRIMARY");
    }
  }

  // ─── Die Kennzahlen ───────────────────────────────────────────────────────────

  /**
   * Jedes Paar steigt ueber den <b>Primaerschluessel seiner eigenen Ebene</b> ein, als {@code
   * range}. Faellt das, liest die Ansicht die Tabelle voll — richtig, aber um Groessenordnungen
   * teurer.
   */
  @Test
  @DisplayName("Die Kennzahlen lesen je Paar einen Bereich ueber den Primaerschluessel")
  void kennzahlen_als_bereich_ueber_primary() {
    for (String mandant : MANDANTEN) {
      for (Rollupzeitraum zeitraum : Rollupzeitraum.values()) {
        String marke = "Kennzahlen/" + mandant + "/" + zeitraum.code();
        List<Plan> plan = plan(kennzahlenSql(mandant, zeitraum));
        Plan ebene = zeileFuer(plan, tabelle(zeitraum), marke);

        assertThat(ebene.zugriff()).as("%s: Zugriffsart", marke).isEqualTo("range");
        assertThat(ebene.index()).as("%s: Treiberindex", marke).isEqualTo("PRIMARY");
      }
    }
  }

  /**
   * Die Mandantenkette wird je Rollupzeile ausgewertet. Ein Durchlauf dort waere das Ende jeder
   * Laufzeit — geprueft wird deshalb, dass beide Tabellen der Kette ueber einen Index erreicht
   * werden.
   */
  @Test
  @DisplayName("Die Mandantenkette der Kennzahlen laeuft nie als Durchlauf")
  void mandantenkette_nie_als_durchlauf() {
    for (String mandant : MANDANTEN) {
      for (Rollupzeitraum zeitraum : Rollupzeitraum.values()) {
        String marke = "Kennzahlen/" + mandant + "/" + zeitraum.code();
        List<Plan> plan = plan(kennzahlenSql(mandant, zeitraum));

        assertThat(zeileFuer(plan, "baum_process", marke).zugriff())
            .as("%s: Process in der Kette", marke)
            .isIn("eq_ref", "ref", "const");
        assertThat(zeileFuer(plan, "ProjectMandant", marke).zugriff())
            .as("%s: ProjectMandant in der Kette", marke)
            .isIn("eq_ref", "ref", "const");
      }
    }
  }

  /**
   * <b>Regel L2, im Plan.</b> Keine Abfrage dieser Ansicht fasst {@code Message} an — die eine
   * benannte Ausnahme des Projekts ist die Ueberfaelligkeitskachel des Dashboards, und sie ist
   * ausdruecklich nicht hierher uebernommen worden ({@code docs/process-view.md} §5).
   */
  @Test
  @DisplayName("Kein Plan dieser Ansicht enthaelt Message")
  void kein_plan_enthaelt_message() {
    for (String mandant : MANDANTEN) {
      assertThat(plan(geruestSql(mandant)))
          .as("Geruest/%s", mandant)
          .noneMatch(zeile -> zeile.tabelle().equalsIgnoreCase("Message"));
      for (Rollupzeitraum zeitraum : Rollupzeitraum.values()) {
        assertThat(plan(kennzahlenSql(mandant, zeitraum)))
            .as("Kennzahlen/%s/%s", mandant, zeitraum.code())
            .noneMatch(zeile -> zeile.tabelle().equalsIgnoreCase("Message"));
      }
    }
  }

  // ─── Das freie Fenster ────────────────────────────────────────────────────────

  /**
   * <b>Je Zweig ein Bereichszugriff ueber den Primaerschluessel seiner Ebene</b> — das ist die
   * Zusicherung fuer die Bauform Z-U, und es ist die einzige.
   *
   * <p><b>Was hier absichtlich nicht festgeschrieben wird:</b> die Materialisierung der Ableitung
   * ({@code <derived2>}), der automatische Schluessel ({@code key0}) und die Reihenfolge der
   * Tabellen. Materialisierung und automatischer Schluessel sind der Grund, warum Z-U schneller ist
   * als Z-D (M149 gegen M150, {@code docs/process-view.md} §33) — aber sie sind eine Entscheidung
   * des Optimierers, und Annahme A10 fuehrt ein Upgrade auf MariaDB 11 als offenes Risiko. Ein
   * Test, der sie festschriebe, wuerde an dem Tag rot, an dem sich nichts Fachliches geaendert hat.
   * Die Beobachtung gehoert in die Datei, nicht in die Zusicherung — dieselbe Ueberlegung wie bei
   * der Reihenfolge in {@code DashboardPlanDbIT}.
   */
  @Test
  @DisplayName("Im freien Fenster liest jeder Zweig einen Bereich ueber den Primaerschluessel")
  void freies_fenster_je_zweig_ein_bereich_ueber_primary() {
    for (String mandant : MANDANTEN) {
      for (List<Segment> segmente : FREIE_FENSTER) {
        String marke = "Frei/" + mandant + "/" + segmente.size() + " Segmente";
        List<Plan> plan = plan(kennzahlenSql(mandant, segmente));

        for (Segment segment : segmente) {
          Plan zweig = zeileFuer(plan, tabelle(segment), marke);
          assertThat(zweig.zugriff())
              .as("%s: Zugriffsart auf %s", marke, tabelle(segment))
              .isEqualTo("range");
          assertThat(zweig.index())
              .as("%s: Treiberindex auf %s", marke, tabelle(segment))
              .isEqualTo("PRIMARY");
        }
      }
    }
  }

  /**
   * Eigenschaft 4 in der Fassung vom 07.09.2026, im Plan: Es steht keine Ebene im Plan, die kein
   * Segment traegt. Ein leerer Zweig waere ein Bereichszugriff ueber ein leeres Intervall.
   */
  @Test
  @DisplayName("Im freien Fenster steht keine Ebene im Plan, die kein Segment traegt")
  void freies_fenster_keine_ebene_ohne_segment() {
    for (List<Segment> segmente : FREIE_FENSTER) {
      List<Plan> plan = plan(kennzahlenSql("NEXANS", segmente));
      List<String> getragen =
          segmente.stream().map(ProzessbaumPlanDbIT::tabelle).distinct().toList();
      for (String ebene : List.of("message_rollup", "message_rollup_tag", "message_rollup_monat")) {
        boolean imPlan = plan.stream().anyMatch(zeile -> zeile.tabelle().equals(ebene));
        assertThat(imPlan)
            .as("%d Segmente: %s im Plan", segmente.size(), ebene)
            .isEqualTo(getragen.contains(ebene));
      }
    }
  }

  /** Die Mandantenkette bleibt auch ueber der Ableitung ein Indexzugriff und kein Durchlauf. */
  @Test
  @DisplayName("Die Mandantenkette des freien Fensters laeuft nie als Durchlauf")
  void freies_fenster_mandantenkette_nie_als_durchlauf() {
    for (String mandant : MANDANTEN) {
      for (List<Segment> segmente : FREIE_FENSTER) {
        String marke = "Frei/" + mandant + "/" + segmente.size() + " Segmente";
        List<Plan> plan = plan(kennzahlenSql(mandant, segmente));

        assertThat(zeileFuer(plan, "baum_process", marke).zugriff())
            .as("%s: Process in der Kette", marke)
            .isIn("eq_ref", "ref", "const");
        assertThat(zeileFuer(plan, "ProjectMandant", marke).zugriff())
            .as("%s: ProjectMandant in der Kette", marke)
            .isIn("eq_ref", "ref", "const");
        assertThat(plan)
            .as("%s: Regel L2", marke)
            .noneMatch(zeile -> zeile.tabelle().equalsIgnoreCase("Message"));
      }
    }
  }

  private static String tabelle(Segment segment) {
    return switch (segment.ebene()) {
      case STUNDE -> "message_rollup";
      case TAG -> "message_rollup_tag";
      case MONAT -> "message_rollup_monat";
    };
  }

  private static String tabelle(Rollupzeitraum zeitraum) {
    return switch (zeitraum) {
      case STUNDEN_48 -> "message_rollup";
      case TAGE_30 -> "message_rollup_tag";
      case MONATE_12 -> "message_rollup_monat";
    };
  }
}
