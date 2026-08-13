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
 * Haelt die <b>unverhandelbaren Regeln</b> der BAM-Statements maschinell fest — <b>ohne
 * Datenbank</b>.
 *
 * <p>Der Isolationstest weist die Trennung am laufenden System nach, braucht dafuer aber die
 * Testkopie und ist in der CI ausgeschlossen. Dieser Test schliesst die Luecke von der anderen
 * Seite: Er rendert jedes Statement gegen eine Attrappe und prueft am Text, dass der
 * Mandantenfilter darin steht — <b>auch in der Zaehlung</b>. Genau dort ist er am leichtesten zu
 * vergessen, weil eine Zahl ohne Filter nicht falsch <i>aussieht</i>.
 *
 * <p>Er ersetzt den Isolationstest nicht — Text ist kein Verhalten. Er faengt das Offensichtliche
 * ab: das vergessene {@code EXISTS}, die fehlende Deckelung und den Einstieg ueber den Wert.
 */
class BamStatementsTest {

  private static final MandantContext MANDANT = new MandantContext("NEXANS");
  private static final String MESSAGE_ID = "eine-nachricht";

  /** Ein abgefangenes Statement: der Text und die gebundenen Werte. */
  private record Ausgefuehrt(String sql, List<Object> werte) {}

  private final List<Ausgefuehrt> gerendert = new ArrayList<>();
  private BamRepository repository;

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
    repository = new BamRepository(DSL.using(new MockConnection(attrappe), SQLDialect.MARIADB));
  }

  private String letztesSql() {
    return gerendert.getLast().sql();
  }

  /**
   * Alle drei Statements einmal ausloesen.
   *
   * <p>{@code existiert} laeuft ueber {@code fetchExists} und braucht dafuer eine echte Zeile in
   * der Antwort; die Attrappe liefert eine leere. Das Statement ist trotzdem gerendert und
   * abgefangen — die Ausnahme danach ist ein Artefakt der Attrappe und keine Aussage ueber den
   * Code.
   */
  private List<String> alleStatements() {
    try {
      repository.existiert(MANDANT, MESSAGE_ID);
    } catch (RuntimeException artefaktDerAttrappe) {
      // Das Statement steht trotzdem in `gerendert` — genau darum geht es hier.
    }
    repository.zaehleJeTyp(MANDANT, MESSAGE_ID);
    repository.findeWerte(MANDANT, MESSAGE_ID);
    return gerendert.stream().map(Ausgefuehrt::sql).toList();
  }

  @Test
  @DisplayName("Jedes Statement traegt den Mandantenfilter als EXISTS (Regeln M3 und M5)")
  void jedes_statement_traegt_den_mandantenfilter() {
    List<String> statements = alleStatements();
    assertThat(statements).as("drei Statements, keines uebersehen").hasSize(3);

    for (String sql : statements) {
      String klein = sql.toLowerCase(Locale.ROOT);
      assertThat(klein)
          .as(
              "Der Mandantenfilter ist Bestandteil JEDES Statements, nicht nachgelagerte Pruefung."
                  + " Fehlendes exists in: %s",
              sql)
          .contains("exists");
      assertThat(klein)
          .as("die Kette laeuft ueber ProjectMandant: %s", sql)
          .contains("projectmandant");
    }
  }

  /**
   * <b>Auch die Zaehlung traegt ihn.</b> Ohne Filter nennte die Antwort eine Zahl, zu der sie keine
   * Zeilen liefert — und der Unterschied saehe nach einem Fehler des Werkzeugs aus, nicht nach
   * einem Leck.
   */
  @Test
  @DisplayName("Die Zaehlung je Typ traegt den Mandantenfilter und gruppiert ueber den Typ")
  void die_zaehlung_traegt_den_mandantenfilter() {
    repository.zaehleJeTyp(MANDANT, MESSAGE_ID);

    String klein = letztesSql().toLowerCase(Locale.ROOT);
    assertThat(klein).contains("count(").contains("exists").contains("projectmandant");
    assertThat(klein).as("gruppiert wird ueber den Typ").contains("group by");
  }

  /**
   * <b>{@code MessageBAM} wird ausschliesslich ueber die {@code MessageID} angefasst.</b> Der
   * Einstieg ueber den Wert ist Teil 2 und hat in diesem Schritt in keinem Statement etwas zu
   * suchen — er traegt ganz andere Kosten (M33: bis 234.159 Treffer auf einem Wert).
   */
  @Test
  @DisplayName("Kein Statement filtert ueber MessageBAMValue")
  void kein_einstieg_ueber_den_wert() {
    for (String sql : alleStatements()) {
      String klein = sql.toLowerCase(Locale.ROOT);
      int wo = klein.indexOf("where");
      String hinterWhere = wo < 0 ? "" : klein.substring(wo);
      assertThat(hinterWhere)
          .as("der Einstieg ueber den Wert ist Teil 2: %s", sql)
          .doesNotContain("`messagebamvalue` =")
          .doesNotContain("`messagebamvalue` like");
      assertThat(klein)
          .as("und der Einstieg laeuft ueber die MessageID: %s", sql)
          .contains("`messageid` = ");
    }
  }

  /**
   * <b>Die Deckelung sitzt im Statement und nicht in der Anzeige.</b> Ohne sie haenge die
   * Antwortgroesse an einer Zahl, die niemand gemessen hat — ueber den ganzen Bestand ist das
   * Maximum der Werte je Nachricht unbekannt (M41 hat einen Monat gemessen).
   */
  @Test
  @DisplayName("Die Werte werden je Typgruppe im Statement gedeckelt")
  void die_deckelung_sitzt_im_statement() {
    repository.findeWerte(MANDANT, MESSAGE_ID);

    assertThat(letztesSql().toLowerCase(Locale.ROOT))
        .as("gedeckelt wird je Typgruppe, nicht ueber die Nachricht")
        .contains("row_number() over (partition by")
        .contains("`messagebamtype`")
        .contains("`rang` <= ");

    // Die Grenze steht als gebundener Wert im Statement, nicht als Literal im Text — deshalb
    // wird sie an den Bindungen geprueft und nicht mit einer Zeichenkettensuche.
    assertThat(gerendert.getLast().werte().stream().map(String::valueOf).toList())
        .as("und sie ist die eine Konstante des Endpunkts")
        .contains(String.valueOf(BamRepository.WERTE_JE_GRUPPE));
  }

  /**
   * <b>Kein Zeitfenster.</b> Regel L1 gilt fuer Listen ueber {@code Message}; hier ist die Menge
   * durch einen Primaerschluessel benannt. {@code MessageBAM} traegt ohnehin keinen Zeitstempel.
   */
  @Test
  @DisplayName("Kein Statement filtert ueber MessageLastUpdate")
  void kein_zeitfenster() {
    for (String sql : alleStatements()) {
      String klein = sql.toLowerCase(Locale.ROOT);
      int wo = klein.indexOf("where");
      String hinterWhere = wo < 0 ? "" : klein.substring(wo);
      assertThat(hinterWhere)
          .as(
              "ein Zeitfenster koennte hier nur ausschliessen, was der Aufrufer gesagt hat: %s",
              sql)
          .doesNotContain("messagelastupdate >=")
          .doesNotContain("messagelastupdate <=");
    }
  }

  /**
   * <b>Die Konfiguration ist Ordnung und kein Sieb.</b> Ein innerer Join auf {@code
   * MessageBAMMandant} liesse {@code WOC} nichts sehen — dieser Mandant hat keinen konfigurierten
   * Typ und traegt trotzdem 2.067 BAM-Zeilen (M40).
   */
  @Test
  @DisplayName("MessageBAMMandant haengt an einem LEFT JOIN, nicht am WHERE")
  void die_konfiguration_siebt_nicht() {
    repository.zaehleJeTyp(MANDANT, MESSAGE_ID);

    String klein = letztesSql().toLowerCase(Locale.ROOT);
    int wo = klein.indexOf("`messagebammandant` on");
    assertThat(wo).as("die Konfigurationstabelle wird angebunden").isGreaterThan(0);
    assertThat(klein.substring(0, wo))
        .as("und zwar als LEFT JOIN: %s", letztesSql())
        .endsWith("left outer join `glassfishdb`.");
    assertThat(klein)
        .as("die Einschraenkung auf den Mandanten steht in der Join-Bedingung, nicht im WHERE")
        .contains("`messagebammandant`.`mandantid` = ");
  }

  /**
   * Dieselbe Regel fuer die Beschriftung: Fehlt die Zeile in {@code MessageBAMType}, verschwindet
   * die Gruppe nicht — sie bekommt ihre Typnummer.
   */
  @Test
  @DisplayName("MessageBAMType haengt an einem LEFT JOIN")
  void die_beschriftung_siebt_nicht() {
    repository.zaehleJeTyp(MANDANT, MESSAGE_ID);

    String klein = letztesSql().toLowerCase(Locale.ROOT);
    int wo = klein.indexOf("`messagebamtype` on");
    assertThat(wo).as("die Beschriftungstabelle wird angebunden").isGreaterThan(0);
    assertThat(klein.substring(0, wo)).endsWith("left outer join `glassfishdb`.");
  }

  /**
   * <b>Erst gruppieren, dann beschriften.</b> Die erste Fassung jointe die beiden
   * Stammdatentabellen neben {@code MessageBAM} und liess sie damit <i>je Zeile</i> laufen: 68,9
   * statt 3,8 Millisekunden auf der fettesten Nachricht. Der Plan sah in beiden Fassungen gleich
   * gut aus — deshalb steht die Gestalt hier fest und nicht nur in der Dokumentation.
   */
  @Test
  @DisplayName("Die Stammdaten werden an die Gruppen gejoint, nicht an die Zeilen")
  void erst_gruppieren_dann_beschriften() {
    repository.zaehleJeTyp(MANDANT, MESSAGE_ID);

    String klein = letztesSql().toLowerCase(Locale.ROOT);
    assertThat(klein.indexOf("group by"))
        .as("das GROUP BY steht in der abgeleiteten Tabelle und damit VOR den beiden LEFT JOINs")
        .isLessThan(klein.indexOf("`messagebamtype` on"))
        .isLessThan(klein.indexOf("`messagebammandant` on"));
  }
}
