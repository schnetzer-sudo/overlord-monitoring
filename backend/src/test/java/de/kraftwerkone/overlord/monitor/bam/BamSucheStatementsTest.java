package de.kraftwerkone.overlord.monitor.bam;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.common.Zeitfenster;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.time.LocalDateTime;
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
 * Hält die <b>unverhandelbaren Regeln</b> der Such-Statements maschinell fest — <b>ohne
 * Datenbank</b>.
 *
 * <p>Der Isolationstest weist die Trennung am laufenden System nach, braucht dafür aber die
 * Testkopie und ist in der CI ausgeschlossen. Dieser Test schließt die Lücke von der anderen Seite:
 * Er rendert jedes Statement gegen eine Attrappe und prüft am Text, was darin stehen muss und was
 * nicht.
 *
 * <p>Er ersetzt weder den Isolationstest noch die Messung — Text ist weder Verhalten noch Laufzeit.
 * Er fängt das ab, was beim nächsten Umbau am ehesten wieder entsteht: das vergessene {@code
 * EXISTS}, ein nachgerüstetes {@code STRAIGHT_JOIN}, die Stammdaten-Joins neben statt über der
 * Deckelung und ein Join von {@code bam_sollaenge} gegen {@code GlassfishDB}.
 */
class BamSucheStatementsTest {

  private static final MandantContext MANDANT = new MandantContext("NEXANS");

  private static final Zeitfenster FENSTER =
      new Zeitfenster(
          LocalDateTime.parse("2025-11-30T00:00:00"), LocalDateTime.parse("2025-12-30T00:00:00"));

  /** Ein abgefangenes Statement: der Text und die gebundenen Werte. */
  private record Ausgefuehrt(String sql, List<Object> werte) {}

  private final List<Ausgefuehrt> gerendert = new ArrayList<>();
  private BamSucheRepository repository;

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
        new BamSucheRepository(DSL.using(new MockConnection(attrappe), SQLDialect.MARIADB));
  }

  private String letztesSql() {
    return gerendert.getLast().sql();
  }

  private static Suchbedingung einBegriff() {
    return new Suchbedingung(null, List.of("1234567", "0001234567"));
  }

  private static Suchbedingung zweiterBegriff() {
    return new Suchbedingung((short) 9018, List.of("7654321"));
  }

  /** Alle drei Statements einmal auslösen, in der Reihenfolge des Aufrufs. */
  private List<String> alleStatements() {
    repository.findeSollaengen(MANDANT);
    repository.findeTreffer(MANDANT, List.of(einBegriff(), zweiterBegriff()), FENSTER);
    repository.findeTrefferWerte(
        MANDANT, List.of("eine-nachricht"), List.of(einBegriff(), zweiterBegriff()));
    return gerendert.stream().map(Ausgefuehrt::sql).toList();
  }

  // ─── Mandantentrennung ────────────────────────────────────────────────────────

  /**
   * <b>Jedes Statement auf dem Quellschema trägt den Filter</b> (Regeln M3 und M5) — auch die
   * zweite Abfrage, obwohl ihre Kennungen aus der ersten stammen. Eine Menge, die einmal gefiltert
   * war, bleibt es nicht dadurch, dass jemand es weiß.
   */
  @Test
  @DisplayName("Jedes Statement auf GlassfishDB traegt den Mandantenfilter als EXISTS")
  void jedes_statement_traegt_den_mandantenfilter() {
    alleStatements();

    List<String> aufGlassfish =
        gerendert.stream()
            .map(Ausgefuehrt::sql)
            .filter(sql -> sql.toLowerCase(Locale.ROOT).contains("glassfishdb"))
            .toList();
    assertThat(aufGlassfish).as("beide Abfragen auf dem Quellschema, keine uebersehen").hasSize(2);

    for (String sql : aufGlassfish) {
      String klein = sql.toLowerCase(Locale.ROOT);
      assertThat(klein)
          .as("Der Mandantenfilter ist Bestandteil JEDES Statements: %s", sql)
          .contains("exists")
          .contains("projectmandant");
    }
  }

  /**
   * <b>Der Vollabzug der Kuratierung filtert über den Mandanten und fasst {@code GlassfishDB} nicht
   * an.</b> {@code bam_sollaenge} wird <b>nie</b> gegen das Quellschema gejoint — die Varianten
   * entstehen vor dem Statement, nicht darin.
   */
  @Test
  @DisplayName("Der Sollaengen-Abzug nennt GlassfishDB nicht und filtert ueber mandant_id")
  void sollaengen_werden_nie_gegen_glassfish_gejoint() {
    repository.findeSollaengen(MANDANT);

    String klein = letztesSql().toLowerCase(Locale.ROOT);
    assertThat(klein).contains("`overlord_monitor`.`bam_sollaenge`").contains("`mandant_id` = ");
    assertThat(klein)
        .as("kein Join gegen das Quellschema: %s", letztesSql())
        .doesNotContain("glassfishdb")
        .doesNotContain("join");
  }

  // ─── Die Abfrageform ──────────────────────────────────────────────────────────

  /**
   * <b>Kein {@code STRAIGHT_JOIN}, und das ist gemessen.</b> Mit ihm wird die Reihenfolge bindend,
   * und die Laufzeit bricht um Faktor 219 (zwei Begriffe) bis 1.094 (fünf) ein (M42‑1, M42‑2). Er
   * steht in der Nachrichtenliste, und genau deshalb steht dieser Test hier: damit ihn niemand „zur
   * Sicherheit" nachrüstet.
   */
  @Test
  @DisplayName("Kein Statement enthaelt STRAIGHT_JOIN")
  void kein_straight_join() {
    for (String sql : alleStatements()) {
      assertThat(sql.toLowerCase(Locale.ROOT))
          .as("der Optimierer waehlt die Einstiegstabelle selbst — und waehlt richtig (M42-1)")
          .doesNotContain("straight_join");
    }
  }

  /** Zwei Abfragen statt einer — und deshalb nirgends ein {@code GROUP_CONCAT}. */
  @Test
  @DisplayName("Kein Statement fasst die Treffer mit GROUP_CONCAT zusammen")
  void kein_group_concat() {
    for (String sql : alleStatements()) {
      assertThat(sql.toLowerCase(Locale.ROOT)).doesNotContain("group_concat");
    }
  }

  /**
   * <b>Je Begriff ein eigener Selbstjoin</b> auf {@code MessageBAM} — und <b>nicht</b> die Bauform
   * {@code IN} plus {@code HAVING COUNT(DISTINCT …)}, die in vier von acht gemessenen
   * Konstellationen abfällt (M42‑3).
   */
  @Test
  @DisplayName("Je Begriff ein Selbstjoin, kein HAVING COUNT(DISTINCT)")
  void je_begriff_ein_selbstjoin() {
    repository.findeTreffer(MANDANT, List.of(einBegriff(), zweiterBegriff()), FENSTER);

    String klein = letztesSql().toLowerCase(Locale.ROOT);
    assertThat(klein).contains("`b1`").contains("`b2`");
    assertThat(klein).as("kein dritter Alias bei zwei Begriffen").doesNotContain("`b3`");
    assertThat(klein).doesNotContain("having");

    repository.findeTreffer(MANDANT, List.of(einBegriff()), FENSTER);
    assertThat(letztesSql().toLowerCase(Locale.ROOT))
        .as("und bei einem Begriff genau ein Alias")
        .contains("`b1`")
        .doesNotContain("`b2`");
  }

  /**
   * <b>Verdichtung auf {@code MessageID}.</b> Bei 4,17 Prozent der Paare steht derselbe Wert unter
   * mehreren Typen (M37); ohne sie stünde dieselbe Nachricht mehrfach in der Liste.
   */
  @Test
  @DisplayName("Die Trefferabfrage verdichtet auf die MessageID")
  void verdichtung_auf_die_messageid() {
    repository.findeTreffer(MANDANT, List.of(einBegriff()), FENSTER);

    assertThat(letztesSql().toLowerCase(Locale.ROOT)).contains("group by").contains("`messageid`");
  }

  /**
   * <b>Das Pflicht-Zeitfenster steht im Kern</b> (Regel L1) — es ist das, was den schlimmsten Fall
   * von 10.752,8 ms auf 1.652,0 ms senkt (M35).
   */
  @Test
  @DisplayName("Die Trefferabfrage traegt das Zeitfenster")
  void die_trefferabfrage_traegt_das_zeitfenster() {
    repository.findeTreffer(MANDANT, List.of(einBegriff()), FENSTER);

    String klein = letztesSql().toLowerCase(Locale.ROOT);
    assertThat(klein).contains("`messagelastupdate` >= ").contains("`messagelastupdate` <= ");
    // Der Treiber bindet LocalDateTime als java.sql.Timestamp — verglichen wird deshalb der
    // Zeitpunkt und nicht der Java-Typ.
    assertThat(gerendert.getLast().werte())
        .map(String::valueOf)
        .contains("2025-11-30 00:00:00.0", "2025-12-30 00:00:00.0");
  }

  /**
   * <b>Das harte Limit sitzt im Statement</b>, und es ist {@code n+1}: Die Zusatzzeile beantwortet
   * „es gäbe mehr" — <b>nach</b> dem Mandantenfilter, weil der im selben Statement steht.
   */
  @Test
  @DisplayName("Das Limit steht im Statement und ist HOECHSTENS_TREFFER + 1")
  void das_limit_sitzt_im_statement() {
    repository.findeTreffer(MANDANT, List.of(einBegriff()), FENSTER);

    // jOOQ rendert die Deckelung fuer MariaDB als `fetch next ? rows only`; die Form ist eine Sache
    // des Dialekts, die Zahl ist die Zusage. Geprueft wird deshalb der gebundene Wert und die
    // Stelle, an der die abgeleitete Tabelle endet.
    assertThat(letztesSql().toLowerCase(Locale.ROOT)).contains(") as `treffer`");
    assertThat(gerendert.getLast().werte().stream().map(String::valueOf).toList())
        .contains(String.valueOf(BamSucheRepository.HOECHSTENS_TREFFER + 1));
  }

  /**
   * <b>Erst deckeln, dann beschriften.</b> Derselbe Befund wie in Teil 1 ({@code docs/bam-werte.md}
   * §4): Dort kostete die Fassung mit den Stammdaten-Joins <i>neben</i> {@code MessageBAM} das
   * Achtzehnfache, weil die Nachschlagevorgänge je Zeile liefen. Hier wäre der Unterschied größer —
   * der schlimmste gemessene Wert erzeugt 234.159 Kandidatenzeilen (M33).
   */
  @Test
  @DisplayName("Process, Project, SOS und SOSAction haengen ueber der Deckelung, nicht daneben")
  void erst_deckeln_dann_beschriften() {
    repository.findeTreffer(MANDANT, List.of(einBegriff()), FENSTER);

    String klein = letztesSql().toLowerCase(Locale.ROOT);
    int deckelung = klein.indexOf(") as `treffer`");
    assertThat(deckelung).as("die Deckelung schliesst die abgeleitete Tabelle ab").isGreaterThan(0);
    for (String tabelle : List.of("`process` ", "`project` ", "`sos` ", "`sosaction` ")) {
      assertThat(klein.indexOf("left outer join `glassfishdb`." + tabelle))
          .as("%s wird erst ueber der abgeleiteten Tabelle angebunden: %s", tabelle, letztesSql())
          .isGreaterThan(deckelung);
    }
  }

  /**
   * <b>Die Typangabe steht in der {@code WHERE}-Klausel</b> und ist Ergebnisverfeinerung, keine
   * Entlastung (M36: +1,5 bis +4 Prozent).
   */
  @Test
  @DisplayName("Ein Begriff mit Typ traegt seine Typbedingung, einer ohne nicht")
  void die_typangabe_steht_in_der_bedingung() {
    repository.findeTreffer(MANDANT, List.of(zweiterBegriff()), FENSTER);
    assertThat(letztesSql().toLowerCase(Locale.ROOT)).contains("`messagebamtype` = ");
    assertThat(gerendert.getLast().werte()).contains((short) 9018);

    repository.findeTreffer(MANDANT, List.of(einBegriff()), FENSTER);
    assertThat(letztesSql().toLowerCase(Locale.ROOT))
        .as("ohne Typangabe wird typlos gesucht — der Normalfall der Oberflaeche")
        .doesNotContain("`messagebamtype` = ");
  }

  /**
   * <b>Die zweite Abfrage steigt über die Kennungen ein</b>, nicht über den Wert — derselbe Pfad
   * wie in Teil 1. Und sie fragt genau die Werte ab, die getroffen haben.
   */
  @Test
  @DisplayName("Die Trefferwerte laufen ueber die MessageID-Liste und die gesuchten Werte")
  void trefferwerte_laufen_ueber_die_kennungen() {
    repository.findeTrefferWerte(MANDANT, List.of("a", "b"), List.of(einBegriff()));

    String klein = letztesSql().toLowerCase(Locale.ROOT);
    assertThat(klein).contains("`messageid` in (").contains("`messagebamvalue` in (");
    assertThat(gerendert.getLast().werte()).contains("a", "b", "1234567", "0001234567");
  }

  /** Ohne Kennungen entsteht keine Abfrage — es gibt nichts nachzuschlagen. */
  @Test
  @DisplayName("Ohne Treffer entsteht keine zweite Abfrage")
  void ohne_treffer_keine_zweite_abfrage() {
    repository.findeTrefferWerte(MANDANT, List.of(), List.of(einBegriff()));

    assertThat(gerendert).isEmpty();
  }

  /**
   * <b>Kein Präfixmuster.</b> Die Suche ist exakt; {@code LIKE} folgt zudem der PAD-SPACE-Regel
   * nicht (M43‑3) und verhielte sich bei folgenden Leerzeichen anders als der {@code =}-Vergleich.
   */
  @Test
  @DisplayName("Kein Statement sucht praefixweise")
  void keine_praefixsuche() {
    for (String sql : alleStatements()) {
      assertThat(sql.toLowerCase(Locale.ROOT))
          .as("exakt, nicht praefixweise: %s", sql)
          .doesNotContain("`messagebamvalue` like");
    }
  }
}
