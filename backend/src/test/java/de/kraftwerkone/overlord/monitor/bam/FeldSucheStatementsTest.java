package de.kraftwerkone.overlord.monitor.bam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
 * Haelt die Regeln der <b>Property-Suche</b> im Statement maschinell fest — <b>ohne Datenbank</b>,
 * dieselbe Bauform wie {@code BamSucheStatementsTest}.
 *
 * <p>Was hier steht, ist das, was beim naechsten Umbau am ehesten wieder entstuende: ein {@code
 * LIKE} auf {@code MessagePropertyValue}, ein Typ-0-Name, der doch ueber {@code MessageProperty}
 * laeuft, ein vergessenes {@code EXISTS}, ein {@code STRAIGHT_JOIN} „zur Sicherheit" — und die
 * Stammdaten-Joins neben statt ueber der Deckelung.
 */
class FeldSucheStatementsTest {

  private static final MandantContext MANDANT = new MandantContext("NEXANS");

  private static final Zeitfenster FENSTER =
      new Zeitfenster(
          LocalDateTime.parse("2025-11-30T00:00:00"), LocalDateTime.parse("2025-12-30T00:00:00"));

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

  private String klein() {
    return letztesSql().toLowerCase(Locale.ROOT);
  }

  private List<Object> werte() {
    return gerendert.getLast().werte();
  }

  private static Suchbedingung bam() {
    return new Suchbedingung(null, List.of("1234567"), Suchmodus.EXAKT);
  }

  private static Feldbedingung eigenschaft(String name, String wert) {
    return new Feldbedingung(name, wert, null);
  }

  private static Feldbedingung spalte(Typ0Feld feld, String wert) {
    return new Feldbedingung(feld.feldname(), wert, feld);
  }

  // ─── Der EAV-Zugriff (Typ 1) ──────────────────────────────────────────────────

  /**
   * <b>Die Fassung aus M166</b>: ein Join auf {@code MessageProperty}, Name und Wert je {@code =},
   * beide gebunden. Neben einem BAM-Begriff haengt der Join an dessen Kennung.
   */
  @Test
  @DisplayName("Ein Feldbegriff wird zum Join auf MessageProperty mit Name und Wert je gleich")
  void eav_zugriff_ist_name_und_wert_gleich() {
    repository.findeTreffer(
        MANDANT, List.of(bam()), List.of(eigenschaft("Message.GUID", "abc-123")), FENSTER);

    assertThat(klein())
        .as("gerendert: %s", letztesSql())
        .contains(
            "join `glassfishdb`.`messageproperty` as `mp1` on `mp1`.`messageid` = `b1`.`messageid`")
        .contains("`mp1`.`messagepropertyname` = ?")
        .contains("`mp1`.`messagepropertyvalue` = ?");
    assertThat(werte()).contains("Message.GUID", "abc-123");
  }

  /** Allein ueber ein Feld: {@code MessageProperty} fuehrt, {@code MessageBAM} kommt nicht vor. */
  @Test
  @DisplayName("Eine Suche allein ueber ein Feld fasst MessageBAM nicht an")
  void feld_allein_ohne_messagebam() {
    repository.findeTreffer(
        MANDANT, List.of(), List.of(eigenschaft("Converter.TransactionID", "T1")), FENSTER);

    assertThat(klein())
        .contains("from `glassfishdb`.`messageproperty` as `mp1` join `glassfishdb`.`message` on")
        .doesNotContain("`messagebam`");
  }

  /** Je Feldbegriff ein eigener Join — die Verundung ist die zwischen den Joins. */
  @Test
  @DisplayName("Zwei Feldbegriffe sind zwei Joins auf MessageProperty, kein dritter")
  void je_feld_ein_join() {
    repository.findeTreffer(
        MANDANT,
        List.of(),
        List.of(eigenschaft("Message.GUID", "a"), eigenschaft("Message.SNDPRN", "b")),
        FENSTER);

    assertThat(klein()).contains("`mp1`").contains("`mp2`").doesNotContain("`mp3`");
    assertThat(klein()).contains("`mp2`.`messageid` = `mp1`.`messageid`");
  }

  /**
   * <b>Kein {@code LIKE} auf {@code MessagePropertyValue}</b> — auch nicht im Praefixmodus der
   * BAM-Begriffe. Ein Praefixzugriff ueber den 50-Zeichen-Praefixindex ist nicht gemessen, und
   * {@code modus=praefix} wirkt ausschliesslich auf {@code MessageBAMValue}.
   */
  @Test
  @DisplayName("Auf MessagePropertyValue steht nie ein LIKE — auch nicht im Praefixmodus")
  void nie_like_auf_dem_eigenschaftswert() {
    repository.findeTreffer(
        MANDANT,
        List.of(new Suchbedingung(null, List.of("1234567"), Suchmodus.PRAEFIX)),
        List.of(eigenschaft("Message.GUID", "abc")),
        FENSTER);

    assertThat(klein())
        .contains("`messagebamvalue` like")
        .doesNotContain("`messagepropertyvalue` like")
        .contains("`messagepropertyvalue` = ?");
    assertThat(werte()).contains("abc").doesNotContain("abc%");
  }

  // ─── Das Spaltenpraedikat (Typ 0) ─────────────────────────────────────────────

  /**
   * <b>Ein Typ-0-Name fasst {@code MessageProperty} nicht an</b> (E‑101). Allein ueber eine Spalte
   * fuehrt {@code Message} selbst.
   */
  @Test
  @DisplayName("Ein Typ-0-Feld ist ein Spaltenpraedikat auf Message, ohne MessageProperty")
  void typ0_ohne_messageproperty() {
    repository.findeTreffer(
        MANDANT, List.of(), List.of(spalte(Typ0Feld.STATUS, "FINISHED")), FENSTER);

    assertThat(klein())
        .as("gerendert: %s", letztesSql())
        .doesNotContain("`messageproperty`")
        .doesNotContain("`messagebam`")
        .contains("from `glassfishdb`.`message` where")
        .contains("`glassfishdb`.`message`.`messagestatus` = ?");
    assertThat(werte()).contains("FINISHED");
  }

  /**
   * <b>Die acht Abbildungen, je auf ihre Spalte</b> — die Zuordnung aus M155, hier am gerenderten
   * Text: wortgleich, umgestellt, umbenannt, andere Tabelle.
   */
  @Test
  @DisplayName("Jedes der acht Typ-0-Felder rendert seine Zielspalte")
  void jedes_typ0_feld_seine_spalte() {
    record Erwartung(Typ0Feld feld, String spalte) {}
    List<Erwartung> erwartungen =
        List.of(
            new Erwartung(Typ0Feld.MESSAGE_ID, "`glassfishdb`.`message`.`messageid` = ?"),
            new Erwartung(
                Typ0Feld.MESSAGE_ID_SOURCE, "`glassfishdb`.`message`.`sourcemessageid` = ?"),
            new Erwartung(
                Typ0Feld.MESSAGE_ID_TARGET, "`glassfishdb`.`message`.`targetmessageid` = ?"),
            new Erwartung(Typ0Feld.PROCESS_ID, "`glassfishdb`.`message`.`processid` = ?"),
            new Erwartung(Typ0Feld.PROCESS_NAME, "`feld_process`.`processname` = ?"),
            new Erwartung(Typ0Feld.SOS_ID, "`glassfishdb`.`message`.`sosid` = ?"),
            new Erwartung(Typ0Feld.SOS_NAME, "`feld_sos`.`sosname` = ?"),
            new Erwartung(Typ0Feld.STATUS, "`glassfishdb`.`message`.`messagestatus` = ?"));

    for (Erwartung erwartung : erwartungen) {
      repository.findeTreffer(MANDANT, List.of(), List.of(spalte(erwartung.feld(), "x")), FENSTER);
      assertThat(klein())
          .as("%s: %s", erwartung.feld(), letztesSql())
          .contains(erwartung.spalte())
          .doesNotContain("`messageproperty`");
    }
  }

  /**
   * <b>Die zwei Namen in anderen Tabellen brauchen einen Join</b> — ueber {@code ProcessID}
   * beziehungsweise {@code SOSID}, unter eigenem Alias, weil {@code Process} in derselben Abfrage
   * schon zweimal steht.
   */
  @Test
  @DisplayName("ProcessName und SOSName joinen Process und SOS unter eigenem Alias")
  void prozessname_und_sosname_joinen() {
    repository.findeTreffer(
        MANDANT,
        List.of(),
        List.of(spalte(Typ0Feld.PROCESS_NAME, "P"), spalte(Typ0Feld.SOS_NAME, "S")),
        FENSTER);

    assertThat(klein())
        .contains(
            "join `glassfishdb`.`process` as `feld_process` on `feld_process`.`processid` ="
                + " `glassfishdb`.`message`.`processid`")
        .contains(
            "join `glassfishdb`.`sos` as `feld_sos` on `feld_sos`.`sosid` ="
                + " `glassfishdb`.`message`.`sosid`");
  }

  /** Derselbe Name zweimal ergibt zwei Praedikate, aber nur einen Join. */
  @Test
  @DisplayName("Derselbe Join-Name zweimal: zwei Praedikate, ein Join")
  void derselbe_name_zweimal_ein_join() {
    repository.findeTreffer(
        MANDANT,
        List.of(),
        List.of(spalte(Typ0Feld.PROCESS_NAME, "P1"), spalte(Typ0Feld.PROCESS_NAME, "P2")),
        FENSTER);

    assertThat(klein().split("as `feld_process` on", -1)).hasSize(2);
    assertThat(klein().split("`feld_process`.`processname` = \\?", -1)).hasSize(3);
  }

  // ─── Was fuer alle Formen gilt ────────────────────────────────────────────────

  /** Regel M3 — auch dort, wo {@code Message} allein fuehrt. */
  @Test
  @DisplayName("Jede Form traegt den Mandantenfilter als EXISTS, das Fenster und das Limit")
  void mandantenfilter_fenster_limit_in_jeder_form() {
    List<List<Feldbedingung>> formen =
        List.of(
            List.of(eigenschaft("Message.GUID", "a")),
            List.of(spalte(Typ0Feld.STATUS, "FINISHED")),
            List.of(eigenschaft("Message.GUID", "a"), spalte(Typ0Feld.PROCESS_NAME, "P")));

    for (List<Feldbedingung> form : formen) {
      repository.findeTreffer(MANDANT, List.of(), form, FENSTER);
      String klein = klein();
      assertThat(klein)
          .as("gerendert: %s", letztesSql())
          .contains("exists")
          .contains("projectmandant")
          .contains("`messagelastupdate` >= ")
          .contains("`messagelastupdate` <= ")
          .contains("group by")
          .contains(") as `treffer`")
          .doesNotContain("straight_join")
          .doesNotContain("group_concat");
      assertThat(werte().stream().map(String::valueOf).toList())
          .contains(String.valueOf(BamSucheRepository.HOECHSTENS_TREFFER + 1))
          .contains(MANDANT.mandantId());
    }
  }

  /** Erst deckeln, dann beschriften — der Befund aus M47, auch fuer die neuen Formen. */
  @Test
  @DisplayName("Die Anzeigetabellen haengen auch bei Feldbegriffen ueber der Deckelung")
  void erst_deckeln_dann_beschriften() {
    repository.findeTreffer(
        MANDANT,
        List.of(bam()),
        List.of(eigenschaft("Message.GUID", "a"), spalte(Typ0Feld.SOS_NAME, "S")),
        FENSTER);

    String klein = klein();
    int deckelung = klein.indexOf(") as `treffer`");
    assertThat(deckelung).isGreaterThan(0);
    for (String tabelle : List.of("`process` ", "`project` ", "`sos` ", "`sosaction` ")) {
      assertThat(klein.indexOf("left outer join `glassfishdb`." + tabelle))
          .as("%s ueber der Deckelung: %s", tabelle, letztesSql())
          .isGreaterThan(deckelung);
    }
    assertThat(klein.indexOf("as `feld_sos` on"))
        .as("der Feld-Join sitzt im Kern, vor der Deckelung")
        .isLessThan(deckelung);
  }

  /** Eine Suche ohne jede Bedingung laese das ganze Fenster — sie gibt es nicht. */
  @Test
  @DisplayName("Ohne jede Bedingung entsteht kein Statement")
  void ohne_bedingung_kein_statement() {
    assertThatThrownBy(() -> repository.findeTreffer(MANDANT, List.of(), List.of(), FENSTER))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(gerendert).isEmpty();
  }
}
