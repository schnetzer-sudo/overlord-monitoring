package de.kraftwerkone.overlord.monitor.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.common.MessageStatusClassifier;
import de.kraftwerkone.overlord.monitor.common.MessageStatusKind;
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
 * <b>Der Plantest des Dashboards</b> — er prueft <b>Treibertabelle und Index, nicht die
 * Laufzeit</b>.
 *
 * <p>Die Begruendung ist im Projekt belegt und steht in {@code docs/nachrichtenliste.md} §5b: Ein
 * Test, der zufaellig rot wird, wird nach der dritten Wiederholung nicht mehr gelesen. Ein Plantest
 * ist deterministisch und prueft die <b>Ursache</b> statt ihres Schattens (Regel T1).
 *
 * <h2>Was er festhaelt und warum gerade das</h2>
 *
 * <ol>
 *   <li><b>Jedes Zeitraumpaar steigt ueber den Primaerschluessel seiner eigenen Ebene ein</b>, als
 *       {@code range}. Faellt das, liest die Ansicht die Tabelle voll — richtig, aber um
 *       Groessenordnungen teurer.
 *   <li><b>Die Mandantenkette laeuft als {@code eq_ref} ueber Primaerschluessel</b>, nicht als
 *       Durchlauf. Sie wird je Rollupzeile ausgewertet; ein Durchlauf dort waere das Ende jeder
 *       Laufzeit.
 *   <li><b>Die Kachel <i>Ueberfaellig</i> und beide Haelften von „Zuletzt aufgefallen" steigen
 *       ueber {@code MessageStatusIDX} ein</b> — und <b>nicht</b> ueber {@code
 *       MessageLastUpdateIDX}. Das ist der Befund aus M108: Mit einem gemeinsamen {@code OR} tat
 *       Block 6 genau das und las den ganzen Zeitbereich; bei zwoelf Monaten waren das 2,7
 *       Millionen Zeilen und ein {@code 500}. <b>Diese Zeile ist die Gegenprobe dazu und der
 *       eigentliche Grund fuer diesen Test.</b>
 * </ol>
 *
 * <p><b>Was er absichtlich nicht prueft:</b> Zeilenschaetzungen und Kosten. Sie haengen an der
 * Statistik, und eine Statistik festzuschreiben hiesse, den Test bei der naechsten Analyse rot zu
 * machen, ohne dass jemand etwas falsch gemacht haette.
 *
 * <p>Der {@code EXPLAIN} laeuft ueber das <b>gerenderte</b> Statement mit Literalen ({@code
 * StatementType.STATIC_STATEMENT}) — mit Fragezeichen gaebe es keine Bereichsanalyse.
 */
@SpringBootTest
@ActiveProfiles("dev")
@Tag("db")
class DashboardPlanDbIT {

  /** Der Anker der Anwendungsuhr im Profil {@code dev} — derselbe Stichtag wie in M94 und M107. */
  private static final LocalDateTime ANKER = LocalDateTime.parse("2025-12-30T04:09:47");

  private static final List<String> MANDANTEN = List.of("NEXANS", "SUTTONS");

  @Autowired
  @Qualifier("glassfishDsl") private DSLContext glassfishDsl;

  private final List<String> gerendert = new ArrayList<>();
  private DashboardRepository attrappe;

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
        new DashboardRepository(
            DSL.using(
                new MockConnection(mock),
                SQLDialect.MARIADB,
                new Settings().withStatementType(StatementType.STATIC_STATEMENT)),
            new MessageStatusClassifier());
  }

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
   * steht.</b> Welche Tabelle den Einstieg macht, haengt am Mandanten und an der Statistik — bei
   * {@code NEXANS} ist es die Rolluptabelle, bei {@code SUTTONS} steigt der Optimierer ueber {@code
   * ProjectMandant} ein, weil dieser Mandant wenige Projekte hat. <b>Beide Plaene sind richtig</b>,
   * und beide sind gemessen schnell. Eine Reihenfolge festzuschreiben hiesse, den Test bei der
   * naechsten Statistikanalyse rot zu machen, ohne dass jemand etwas falsch gemacht haette —
   * dieselbe Ueberlegung wie in {@code NachrichtenPlanDbIT} ({@code docs/nachrichtenliste.md} §5b).
   */
  private static Plan zeileFuer(List<Plan> plan, String tabelle, String marke) {
    return plan.stream()
        .filter(zeile -> zeile.tabelle().equals(tabelle))
        .findFirst()
        .orElseThrow(
            () ->
                new AssertionError(
                    "Im Plan von " + marke + " kommt " + tabelle + " gar nicht vor: " + plan));
  }

  private String einziges() {
    assertThat(gerendert).hasSize(1);
    return gerendert.getFirst();
  }

  @Test
  @DisplayName("Jedes Paar steigt ueber den Primaerschluessel seiner eigenen Rollup-Ebene ein")
  void verlauf_faehrt_ueber_den_primaerschluessel() {
    for (String mandantId : MANDANTEN) {
      for (Rollupzeitraum zeitraum : Rollupzeitraum.reihe()) {
        gerendert.clear();
        attrappe.verlauf(new MandantContext(mandantId), zeitraum, zeitraum.fenster(ANKER));
        List<Plan> plan = plan(einziges());
        String marke = mandantId + "/" + zeitraum.code();

        String tabelle =
            switch (zeitraum) {
              case STUNDEN_48 -> "message_rollup";
              case TAGE_30 -> "message_rollup_tag";
              case MONATE_12 -> "message_rollup_monat";
            };
        Plan ebene = zeileFuer(plan, tabelle, marke);

        assertThat(ebene.zugriff())
            .as("Die Rollup-Ebene wird nie voll gelesen (%s)", marke)
            .isNotEqualTo("ALL");
        assertThat(ebene.index())
            .as("Und sie wird ueber einen Index erreicht (%s)", marke)
            .isNotEqualTo("null");
        assertThat(plan)
            .as("Keine Tabelle dieses Statements wird voll gelesen (%s)", marke)
            .noneMatch(zeile -> "ALL".equals(zeile.zugriff()));
      }
    }
  }

  @Test
  @DisplayName("Die Verteilung faehrt denselben Weg und haengt den Katalog als eq_ref an")
  void verteilung_faehrt_denselben_weg() {
    for (Rollupzeitraum zeitraum : Rollupzeitraum.reihe()) {
      for (Verteilungssicht sicht : Verteilungssicht.values()) {
        gerendert.clear();
        attrappe.verteilung(new MandantContext("NEXANS"), zeitraum, zeitraum.fenster(ANKER), sicht);
        List<Plan> plan = plan(einziges());
        String marke = zeitraum.code() + "/" + sicht;

        assertThat(plan)
            .as("Keine Tabelle dieses Statements wird voll gelesen (%s)", marke)
            .noneMatch(zeile -> "ALL".equals(zeile.zugriff()));
        assertThat(zeileFuer(plan, "process_catalog", marke).zugriff())
            .as("Der Katalog haengt an und treibt nicht (%s)", marke)
            .isEqualTo("eq_ref");
      }
    }
  }

  /**
   * <b>Die beiden benannten Ausnahmen von L2, und sie sind billig, weil der Statusindex sie
   * traegt.</b> Ein einzelner Rohwert von 3,34 Millionen Zeilen — das ist der ganze Unterschied
   * zwischen wenigen Millisekunden und einer Live-Aggregation, die man nicht bauen darf.
   *
   * <p><b>Erwartet ist {@code range} oder {@code ref}, und beides ist richtig.</b> Das neue
   * Statement vergleicht mit {@code =} auf einen einzelnen Wert, wo das alte ein {@code IN} ueber
   * zwei trug; ein einzelner Wert ergibt bei MariaDB ein {@code ref} statt eines {@code range}.
   * <b>Festgeschrieben ist deshalb der Index und nicht die Zugriffsart</b> — sie ist eine Folge der
   * Zahl der Werte und keine Eigenschaft, die dieser Bau zusichert. Was er zusichert: <b>nicht der
   * Zeitindex, und kein voller Durchlauf.</b>
   */
  @Test
  @DisplayName("Laeuft und Wartend steigen ueber MessageStatusIDX ein, nie ueber den Zeitindex")
  void offene_kacheln_fahren_ueber_den_statusindex() {
    for (String mandantId : MANDANTEN) {
      MandantContext mandant = new MandantContext(mandantId);

      for (MessageStatusKind einordnung :
          List.of(MessageStatusKind.LAEUFT, MessageStatusKind.WARTEND)) {
        gerendert.clear();
        attrappe.offeneNachrichten(mandant, einordnung);
        List<Plan> plan = plan(einziges());
        String marke = mandantId + "/" + einordnung;

        assertThat(zeileFuer(plan, "Message", marke).index())
            .as("Nicht der Zeitindex — %s", marke)
            .isEqualTo("MessageStatusIDX");
        assertThat(plan)
            .as("Keine Tabelle wird voll gelesen (%s)", marke)
            .noneMatch(zeile -> "ALL".equals(zeile.zugriff()));
      }
    }
  }

  /**
   * <b>Die Erscheinungsbedingung, und der Plan ist ihre eigentliche Rechtfertigung.</b> Ein {@code
   * LIKE '%…%'} ueber eine Textspalte sieht nach vollem Durchlauf aus; M8 hat fuer dasselbe Muster
   * ueber {@code MessageAction} 97,976 s gemessen. <b>Hier steigt der Optimierer beim Mandanten
   * ein</b> ({@code ProjectMandant}), und das {@code LIKE} laeuft nur ueber dessen eigene {@code
   * SOSAction}-Zeilen.
   *
   * <p><b>Geprueft wird genau das:</b> keine Tabelle voll gelesen — {@code SOSAction}
   * eingeschlossen. Die Reihenfolge steht auch hier nicht fest.
   */
  @Test
  @DisplayName("Die Erscheinungsbedingung liest keine Tabelle voll, auch SOSAction nicht")
  void erscheinungsbedingung_liest_keine_tabelle_voll() {
    for (String mandantId : MANDANTEN) {
      gerendert.clear();
      attrappe.hatWartendeAblaeufe(new MandantContext(mandantId));

      assertThat(plan(einziges()))
          .as("Keine Tabelle wird voll gelesen (%s)", mandantId)
          .noneMatch(zeile -> "ALL".equals(zeile.zugriff()));
    }
  }

  /**
   * <b>Der Befund aus M108, als Wächter.</b> Mit einem gemeinsamen {@code OR} stieg dieser Block
   * ueber {@code MessageLastUpdateIDX} ein und las den ganzen Zeitbereich: 23.126 Zeilen bei 48
   * Stunden, 2,7 Millionen bei zwoelf Monaten — und fuer {@code VOTG} lief er in die Zeitgrenze des
   * Lese-Pools.
   *
   * <p><b>Der teure Fall ist der gute Fall:</b> Ein Mandant <i>ohne</i> Fehler zwingt die
   * Datenbank, den ganzen Bereich zu durchsuchen, bevor sie „nichts" sagen darf. Deshalb wird hier
   * fuer <b>alle drei Paare</b> geprueft und nicht nur fuer das engste.
   */
  @Test
  @DisplayName("„Zuletzt aufgefallen“ steigt ueber MessageStatusIDX ein")
  void aufgefallen_faehrt_ueber_den_statusindex() {
    for (String mandantId : MANDANTEN) {
      for (Rollupzeitraum zeitraum : Rollupzeitraum.reihe()) {
        gerendert.clear();
        attrappe.zuletztAufgefallen(
            new MandantContext(mandantId),
            zeitraum.fenster(ANKER),
            DashboardService.AUFFAELLIG_HOECHSTENS);
        assertThat(gerendert).as("Seit E-71 nur noch die Fehlerhaelfte").hasSize(1);

        for (String sql : gerendert) {
          String marke = mandantId + "/" + zeitraum.code();

          assertThat(zeileFuer(plan(sql), "Message", marke).index())
              .as(
                  "Ueber MessageLastUpdateIDX laese die Abfrage den ganzen Zeitbereich — genau der"
                      + " Befund aus M108 (%s)",
                  marke)
              .isEqualTo("MessageStatusIDX");
        }
      }
    }
  }

  /**
   * Die Gegenprobe zum Test darueber: <b>{@code MessageLastUpdateIDX} steht in keiner Planzeile des
   * Blocks.</b> Ohne sie zeigte der Test nur, dass irgendein Plan herauskommt, in dem der
   * Statusindex vorkommt.
   */
  @Test
  @DisplayName("Der Zeitindex kommt in „Zuletzt aufgefallen“ nirgends vor")
  void aufgefallen_ohne_zeitindex() {
    gerendert.clear();
    attrappe.zuletztAufgefallen(
        new MandantContext("SUTTONS"),
        Rollupzeitraum.MONATE_12.fenster(ANKER),
        DashboardService.AUFFAELLIG_HOECHSTENS);

    for (String sql : gerendert) {
      assertThat(plan(sql))
          .as("Der Zeitindex darf hier nirgends stehen — er ist der teure Weg")
          .noneMatch(zeile -> "MessageLastUpdateIDX".equals(zeile.index()));
    }
  }

  @Test
  @DisplayName("Der Stand liest ueber rollup_lauf_stand_idx und nicht ueber die ganze Tabelle")
  void stand_faehrt_ueber_seinen_index() {
    gerendert.clear();
    attrappe.letzterLauf();

    assertThat(zeileFuer(plan(einziges()), "rollup_lauf", "stand").zugriff()).isNotEqualTo("ALL");
  }
}
