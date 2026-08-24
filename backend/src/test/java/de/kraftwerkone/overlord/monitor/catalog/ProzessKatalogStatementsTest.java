package de.kraftwerkone.overlord.monitor.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record2;
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
 * Die Statements des Prozess-Katalogs, <b>gerendert statt nachgebildet</b>: Das Repository laeuft
 * gegen eine jOOQ-Attrappe, und geprueft wird der Text, der wirklich herausfaellt.
 *
 * <p>Er beantwortet die eine Frage, die ein Integrationstest nur mittelbar beantwortet: Steht der
 * Mandantenfilter <b>in</b> jedem lesenden Statement (Regel M3) — oder wird er nachgelagert
 * geprueft? Ein nachgelagerter Filter faellt beim naechsten Umbau lautlos weg.
 *
 * <p>Vorbild ist {@code ArtefaktStatementsTest}; die Bauform ist dieselbe.
 */
class ProzessKatalogStatementsTest {

  private static final MandantContext MANDANT = new MandantContext("VOTG");
  private static final String PROZESS = "40000_ERFUNDEN_LAB_VDA";
  private static final String PROJEKT = "300_ErfundenEingehend";
  private static final LocalDateTime JETZT = LocalDateTime.parse("2026-08-20T12:00:00");
  private static final String BENUTZER = "it-katalog";

  private final List<String> gerendert = new ArrayList<>();

  /**
   * Die gebundenen Werte. Sie stehen neben dem Text, weil jOOQ Werte standardmaessig <b>bindet</b>
   * statt sie einzusetzen: Ein {@code 'OFFEN'} taucht im gerenderten Statement gar nicht auf, und
   * eine Textpruefung darauf ginge still ins Leere.
   */
  private final List<Object> bindungen = new ArrayList<>();

  /** Stapelsaetze, deren Wertezahl nicht zur Vorlage passt — siehe {@code pruefeBindungszahl}. */
  private final List<String> bindungsfehler = new ArrayList<>();

  private Result<?> naechstesErgebnis;
  private ProzessKatalogRepository repository;

  @BeforeEach
  void attrappeAufbauen() {
    gerendert.clear();
    bindungen.clear();
    bindungsfehler.clear();
    naechstesErgebnis = null;
    MockDataProvider attrappe =
        ausfuehrung -> {
          erfasse(ausfuehrung.batchSQL(), ausfuehrung.sql());
          merkeBindungen(ausfuehrung.bindings(), ausfuehrung.batchBindings());
          pruefeBindungszahl(ausfuehrung.sql(), ausfuehrung.batchBindings());
          Result<?> ergebnis = naechstesErgebnis == null ? leer().newResult() : naechstesErgebnis;
          naechstesErgebnis = null;
          int stapel =
              ausfuehrung.batchBindings() == null
                  ? 1
                  : Math.max(1, ausfuehrung.batchBindings().length);
          MockResult[] antwort = new MockResult[stapel];
          for (int i = 0; i < stapel; i++) {
            antwort[i] = new MockResult(ergebnis.size(), ergebnis);
          }
          return antwort;
        };
    DSLContext attrappenKontext = DSL.using(new MockConnection(attrappe), SQLDialect.MARIADB);
    repository = new ProzessKatalogRepository(attrappenKontext, attrappenKontext);
  }

  private void erfasse(String[] stapelSql, String einzelSql) {
    if (stapelSql != null && stapelSql.length > 0) {
      for (String sql : stapelSql) {
        gerendert.add(sql);
      }
      return;
    }
    gerendert.add(einzelSql);
  }

  private void merkeBindungen(Object[] einzeln, Object[][] stapel) {
    if (stapel != null && stapel.length > 0) {
      for (Object[] satz : stapel) {
        bindungen.addAll(java.util.Arrays.asList(satz));
      }
      return;
    }
    if (einzeln != null) {
      bindungen.addAll(java.util.Arrays.asList(einzeln));
    }
  }

  /**
   * Ein Stapelsatz muss so viele Werte binden, wie die Vorlage Plaetze hat.
   *
   * <p>Diese Pruefung steht hier, weil jOOQ eine Untermenge <b>nicht</b> als Fehler behandelt: Es
   * schreibt eine Zeile ins Protokoll und fuellt den Rest mit {@code null}. Genau so ist beim Bau
   * dieses Repositorys ein Fehler entstanden — die Vorlage trug feste Statuswerte, die ebenfalls
   * Bindeplaetze sind, und jede Zeile waere um zwei Spalten verschoben geschrieben worden.
   */
  private void pruefeBindungszahl(String sql, Object[][] stapel) {
    if (sql == null || stapel == null || stapel.length == 0) {
      return;
    }
    long plaetze = sql.chars().filter(zeichen -> zeichen == '?').count();
    for (Object[] satz : stapel) {
      if (satz.length != plaetze) {
        bindungsfehler.add(
            "Stapelsatz bindet " + satz.length + " Werte, die Vorlage hat " + plaetze + ": " + sql);
      }
    }
  }

  private static DSLContext leer() {
    return DSL.using(SQLDialect.MARIADB);
  }

  /** Eine Zeile fuer {@code fetchExists}. */
  private void ergebnisJa() {
    DSLContext leer = leer();
    Field<Integer> spalte = DSL.field(DSL.name("eins"), Integer.class);
    Result<Record1<Integer>> ergebnis = leer.newResult(spalte);
    Record1<Integer> zeile = leer.newRecord(spalte);
    zeile.value1(1);
    ergebnis.add(zeile);
    naechstesErgebnis = ergebnis;
  }

  /** Eine Zeile fuer {@code findeProjektbestand}, damit der Stapel danach ueberhaupt entsteht. */
  private void ergebnisEineProjektzeile() {
    DSLContext leer = leer();
    Field<String> kennung = DSL.field(DSL.name("ProcessID"), String.class);
    Field<String> status = DSL.field(DSL.name("pflegestatus"), String.class);
    Result<Record2<String, String>> ergebnis = leer.newResult(kennung, status);
    Record2<String, String> zeile = leer.newRecord(kennung, status);
    zeile.value1(PROZESS);
    zeile.value2(Pflegestatus.GEPFLEGT.name());
    ergebnis.add(zeile);
    naechstesErgebnis = ergebnis;
  }

  /** Alle Statements einmal ausloesen. */
  private List<String> alleStatements() {
    repository.findePflegeliste(MANDANT, false);
    repository.findePflegeliste(MANDANT, true);
    repository.findeZeile(MANDANT, PROZESS);
    repository.findePartner(MANDANT);
    repository.findeBestand(MANDANT);
    repository.findeBestandsflags(MANDANT);
    repository.findeProjektbestand(MANDANT, PROJEKT);
    ergebnisJa();
    repository.projektGehoertZumMandanten(MANDANT, PROJEKT);
    repository.speichereZuordnung(MANDANT, PROZESS, "BAYER", Richtung.EINGEHEND, JETZT, BENUTZER);
    ergebnisEineProjektzeile();
    repository.speichereFeld(MANDANT, PROJEKT, Zuordnungsfeld.PARTNER, "BAYER", JETZT, BENUTZER);
    repository.speichereVorschlaege(
        MANDANT,
        List.of(
            new ProzessKatalogRepository.Vorschlagszeile(
                PROZESS,
                new Partnervorschlag("ERFUNDEN", Richtung.EINGEHEND, VorschlagHerkunft.REGEL_A)),
            new ProzessKatalogRepository.Vorschlagszeile(
                "AuslagerungAusgehendERFUNDEN",
                new Partnervorschlag("ERFUNDEN", Richtung.AUSGEHEND, VorschlagHerkunft.REGEL_B))),
        JETZT,
        BENUTZER);
    repository.speichereBestandsflags(
        MANDANT,
        List.of(
            new ProzessKatalogRepository.Bestandsflag(PROZESS, true),
            new ProzessKatalogRepository.Bestandsflag("AuslagerungAusgehendERFUNDEN", false)),
        JETZT);
    return List.copyOf(gerendert);
  }

  /** Nur das Statement des Bestandslaufs, ohne die uebrigen. */
  private String bestandsStatement() {
    repository.findeBestandsflags(MANDANT);
    return gerendert.getLast();
  }

  private List<String> lesendeStatements() {
    return alleStatements().stream().filter(sql -> klein(sql).startsWith("select")).toList();
  }

  private static String klein(String sql) {
    return sql.toLowerCase(Locale.ROOT);
  }

  @Test
  @DisplayName("Jedes lesende Statement traegt den Mandantenfilter ueber ProjectMandant (Regel M3)")
  void jedes_lesende_statement_traegt_den_mandantenfilter() {
    List<String> lesend = lesendeStatements();
    assertThat(lesend).as("Ohne lesende Statements prueft dieser Test nichts").isNotEmpty();
    for (String sql : lesend) {
      assertThat(klein(sql))
          .as(
              "Der Mandantenfilter ist Bestandteil JEDES Statements, nicht nachgelagerte Pruefung."
                  + " Fehlendes ProjectMandant in: %s",
              sql)
          .contains("projectmandant");
      assertThat(klein(sql))
          .as("Und er filtert ueber die MandantID: %s", sql)
          .contains("mandantid");
    }
  }

  @Test
  @DisplayName("Beide Schemata stehen voll qualifiziert im Text — sonst braeche der Join")
  void schemata_sind_voll_qualifiziert() {
    String pflegeliste = lesendeStatements().getFirst();
    assertThat(pflegeliste)
        .as("Ohne volle Qualifizierung laeuft der schemauebergreifende Join ins Leere")
        .contains("GlassfishDB")
        .contains("overlord_monitor");
  }

  @Test
  @DisplayName("Die Pflegeliste haengt process_catalog als LEFT JOIN an (E5)")
  void pflegeliste_zeigt_auch_prozesse_ohne_katalogzeile() {
    repository.findePflegeliste(MANDANT, false);
    String sql = klein(gerendert.getFirst());
    assertThat(sql)
        .as("Ein innerer Join zeigte nur, was schon kuratiert ist — also genau das Falsche")
        .contains("left outer join")
        .contains("process_catalog");
  }

  @Test
  @DisplayName("Die Pflegeliste sortiert nach ProjectID, dann ProcessID (E6)")
  void pflegeliste_sortiert_nach_zwei_schluesseln() {
    repository.findePflegeliste(MANDANT, false);
    String sql = klein(gerendert.getFirst());
    int ab = sql.indexOf("order by");
    assertThat(ab)
        .as("Ohne Sortierung waere die Reihenfolge die des Optimierers")
        .isGreaterThan(-1);

    String sortierung = sql.substring(ab);
    assertThat(sortierung)
        .as("Namen sortieren nicht: ProcessName ist ueber den Bestand nicht eindeutig")
        .doesNotContain("projectname")
        .doesNotContain("processname");
    assertThat(sortierung.indexOf("projectid")).as("Erst das Projekt").isGreaterThan(-1);
    assertThat(sortierung.indexOf("processid"))
        .as("Dann der Prozess — ohne Zweitschluessel haette die Liste keine feste Reihenfolge")
        .isGreaterThan(sortierung.indexOf("projectid"));
  }

  @Test
  @DisplayName("Die Pflegeliste kennt keine Paginierung und kein Zeitfenster (E8)")
  void pflegeliste_ohne_deckel() {
    repository.findePflegeliste(MANDANT, false);
    String sql = klein(gerendert.getFirst());
    assertThat(sql).doesNotContain(" limit ").doesNotContain("offset");
    assertThat(sql)
        .as("Ein Zeitfenster blendete ausgerechnet die stillen Prozesse aus")
        .doesNotContain("messagelastupdate");
  }

  @Test
  @DisplayName("Der Filter nurOffene zaehlt Prozesse ohne Katalogzeile als offen")
  void nur_offene_zaehlt_fehlende_zeilen_mit() {
    repository.findePflegeliste(MANDANT, true);
    String sql = klein(gerendert.getFirst());
    assertThat(sql)
        .as("Ohne das IS NULL verschwaenden genau die Prozesse, die noch nie angesehen wurden")
        .contains("pflegestatus")
        .contains("is null");
    assertThat(bindungen)
        .as("Ausgeblendet wird, was gepflegt ist — der Wert steht als Bindewert daneben")
        .contains(Pflegestatus.GEPFLEGT.name());
  }

  @Test
  @DisplayName("Kein Statement fasst Message an — der Katalog ist reine Stammdatenarbeit (L2)")
  void kein_zugriff_auf_message() {
    for (String sql : alleStatements()) {
      assertThat(klein(sql))
          .as("Eine Live-Aggregation ueber Message waere Regel L2 unmittelbar: %s", sql)
          .doesNotContain("\"message\"")
          .doesNotContain("messagebam");
    }
  }

  @Test
  @DisplayName("Geschrieben wird ausschliesslich in overlord_monitor.process_catalog (Regel S1)")
  void schreibende_statements_fassen_nur_das_eigene_schema_an() {
    List<String> schreibend =
        alleStatements().stream().filter(sql -> !klein(sql).startsWith("select")).toList();
    assertThat(schreibend).as("Ohne schreibende Statements prueft dieser Test nichts").isNotEmpty();
    for (String sql : schreibend) {
      assertThat(klein(sql)).contains("overlord_monitor").contains("process_catalog");
      assertThat(sql)
          .as("Kein Schreibzugriff beruehrt jemals GlassfishDB: %s", sql)
          .doesNotContain("GlassfishDB");
    }
  }

  /**
   * <b>Jedes <i>kuratierende</i> Schreiben ist ein Upsert</b> — eine fehlende Zeile entsteht dabei.
   *
   * <p><b>Der Bestandslauf ist davon ausgenommen, und die Ausnahme ist die Aussage</b> (E15): Er
   * ist das einzige Schreiben, das auf <b>gepflegte</b> Zeilen geht. Ein {@code INSERT … ON
   * DUPLICATE KEY} muesste dabei die {@code NOT NULL}-Spalten mitliefern — {@code pflegestatus},
   * {@code vorschlag_herkunft}, {@code geaendert_am}, {@code geaendert_von} — und ueberschriebe
   * genau die Kuratierung, die er nicht anfassen darf. Fuer ihn ist das {@code UPDATE} deshalb
   * nicht die schwaechere, sondern die einzig richtige Form.
   *
   * <p>Dass er dabei Prozesse <b>ohne</b> Katalogzeile nicht erreicht, ist folgenlos: Der Dienst
   * faehrt ihn <b>nach</b> {@code speichereVorschlaege}, und danach hat jeder Prozess des Mandanten
   * eine Zeile. Genau diese Reihenfolge sichert {@code ProzessKatalogDbIT} am laufenden Endpunkt
   * ab.
   */
  @Test
  @DisplayName("Jedes kuratierende Schreiben ist ein Upsert — der Bestandslauf ist die Ausnahme")
  void schreibende_statements_sind_upserts() {
    List<String> schreibend =
        alleStatements().stream().filter(sql -> !klein(sql).startsWith("select")).toList();
    assertThat(schreibend).as("Ohne schreibende Statements prueft dieser Test nichts").isNotEmpty();

    List<String> kuratierend =
        schreibend.stream().filter(sql -> !klein(sql).contains("traegt_nachrichten")).toList();
    List<String> bestand =
        schreibend.stream().filter(sql -> klein(sql).contains("traegt_nachrichten")).toList();

    assertThat(kuratierend).as("Die drei kuratierenden Schreibwege").isNotEmpty();
    for (String sql : kuratierend) {
      assertThat(klein(sql))
          .as(
              "Ein reines UPDATE liesse Prozesse ohne Katalogzeile unberuehrt — und das sind genau"
                  + " die, um die es geht: %s",
              sql)
          .startsWith("insert into")
          .contains("on duplicate key update");
    }

    assertThat(bestand)
        .as("Der Bestandslauf muss als eigener Schreibweg auftauchen, sonst prueft der Rest nichts")
        .hasSize(1);
    assertThat(klein(bestand.getFirst()))
        .as(
            "Er ist bewusst KEIN Upsert: ein INSERT muesste die NOT-NULL-Spalten mitliefern und"
                + " ueberschriebe die Kuratierung, die E15 unangetastet laesst: %s",
            bestand.getFirst())
        .startsWith("update")
        .doesNotContain("on duplicate key update");
  }

  @Test
  @DisplayName("Die Massenzuordnung setzt genau ein Feld (E11)")
  void massenzuordnung_setzt_genau_ein_feld() {
    ergebnisEineProjektzeile();
    repository.speichereFeld(MANDANT, PROJEKT, Zuordnungsfeld.PARTNER, "BAYER", JETZT, BENUTZER);
    String stapel = klein(gerendert.getLast());
    assertThat(stapel).contains("partner");
    assertThat(stapel)
        .as("Das andere Feld bleibt unangetastet — feldweise, nie beide zugleich")
        .doesNotContain("richtung");

    gerendert.clear();
    ergebnisEineProjektzeile();
    repository.speichereFeld(
        MANDANT, PROJEKT, Zuordnungsfeld.RICHTUNG, "EINGEHEND", JETZT, BENUTZER);
    String zweiter = klein(gerendert.getLast());
    assertThat(zweiter).contains("richtung");
    assertThat(zweiter).doesNotContain("partner");
  }

  @Test
  @DisplayName("Vorschau und Ausfuehrung fahren dieselbe Abfrage mit derselben Bedingung")
  void vorschau_und_ausfuehrung_teilen_ein_statement() {
    repository.findeProjektbestand(MANDANT, PROJEKT);
    String vorschau = gerendert.getFirst();

    gerendert.clear();
    ergebnisEineProjektzeile();
    repository.speichereFeld(MANDANT, PROJEKT, Zuordnungsfeld.PARTNER, "BAYER", JETZT, BENUTZER);
    String beimAusfuehren = gerendert.getFirst();

    assertThat(beimAusfuehren)
        .as(
            "Getrennt gebaut driften die beiden auseinander, und der Nutzer bestaetigt eine Zahl,"
                + " die nicht die ist, die passiert")
        .isEqualTo(vorschau);
  }

  @Test
  @DisplayName("Der Heuristik-Lauf schreibt immer den Status OFFEN")
  void heuristik_schreibt_offen() {
    repository.speichereVorschlaege(
        MANDANT,
        List.of(new ProzessKatalogRepository.Vorschlagszeile(PROZESS, Partnervorschlag.KEINER)),
        JETZT,
        BENUTZER);
    String sql = klein(gerendert.getLast());
    assertThat(sql).contains("pflegestatus").contains("vorschlag_herkunft");
    assertThat(bindungen)
        .as("Die Heuristik schlaegt vor und entscheidet nicht")
        .contains(Pflegestatus.OFFEN.name())
        .doesNotContain(Pflegestatus.GEPFLEGT.name());
  }

  @Test
  @DisplayName("Jeder Stapelsatz bindet so viele Werte, wie die Vorlage Plaetze hat")
  void stapelsaetze_binden_vollstaendig() {
    alleStatements();
    assertThat(bindungsfehler)
        .as(
            "jOOQ faengt das nicht ab: Es protokolliert eine Zeile und fuellt den Rest mit null."
                + " Eine zu kurze Bindeliste schriebe jede Zeile um Spalten verschoben.")
        .isEmpty();
  }

  @Test
  @DisplayName("Die gerenderten Statements sind vollstaendig")
  void statements_sind_vollstaendig() {
    assertThat(alleStatements()).hasSize(13);
  }

  /**
   * <b>Der Bestandslauf rendert Fassung A aus M83‑1</b> — die gemessene Form und keine andere.
   *
   * <p>M83 sagt ausdruecklich, dass der dort gemessene Text <b>nicht gerendert</b> ist: Beide
   * Fassungen stammen woertlich aus dem Messauftrag, Anwendungscode existierte noch nicht.
   * Vergleichbar sind deshalb nicht die Zeichen, sondern die <b>planbestimmenden Merkmale</b> —
   * genau die vier, an denen der gemessene {@code EXPLAIN} haengt:
   *
   * <ol>
   *   <li>Einstiegstabelle {@code ProjectMandant}
   *   <li>{@code EXISTS} und nicht {@code IN} — nur {@code EXISTS} bricht beim ersten Indexeintrag
   *       ab
   *   <li>die Unterabfrage vergleicht {@code Message.ProcessID} gegen {@code Process.ProcessID}
   *   <li>kein zusaetzlicher Join, insbesondere <b>kein</b> {@code Project}
   * </ol>
   *
   * <p>Weicht eines davon ab, ist die Messung nicht mehr die zu diesem Code — <b>dann ist neu zu
   * messen und nicht nachzubessern</b>. Kein {@code STRAIGHT_JOIN}, auch nicht als Reparatur (M42).
   */
  @Test
  @DisplayName("Der Bestandslauf rendert Fassung A aus M83-1 (Regel L7)")
  void bestandslauf_rendert_fassung_a() {
    String sql = bestandsStatement();
    String klein = klein(sql);

    assertThat(klein)
        .as("Einstieg ueber ProjectMandant — die gemessene Fassung A. Gerendert: %s", sql)
        .contains("from `glassfishdb`.`projectmandant`");
    assertThat(klein)
        .as("EXISTS statt IN: nur EXISTS bricht beim ersten Indexeintrag ab. Gerendert: %s", sql)
        .contains("exists")
        .doesNotContain(" in (select");
    assertThat(klein)
        .as("Die Unterabfrage liest Message. Gerendert: %s", sql)
        .contains("`glassfishdb`.`message`");
    assertThat(klein)
        .as(
            "Kein zusaetzlicher Join — Project gehoert nicht in dieses Statement. Gerendert: %s",
            sql)
        .doesNotContain("`glassfishdb`.`project`.");
    assertThat(klein)
        .as("Kein STRAIGHT_JOIN, auch nicht als Reparatur (M42). Gerendert: %s", sql)
        .doesNotContain("straight_join");
    assertThat(klein)
        .as("Der Mandantenfilter steht IM Statement (Regel M3). Gerendert: %s", sql)
        .contains("mandantid");
  }

  /**
   * <b>Der Bestandslauf fasst ausschliesslich die zwei Beobachtungsspalten an</b> (E15).
   *
   * <p>Er schreibt auf <b>alle</b> Zeilen des Mandanten, auch auf gepflegte — und genau deshalb
   * muss das Statement beweisen, dass es die Kuratierung nicht beruehrt. Ein {@code INSERT … ON
   * DUPLICATE KEY} muesste die {@code NOT NULL}-Spalten mitliefern und ueberschriebe sie; ein
   * {@code UPDATE} auf zwei Spalten kann es nicht.
   */
  @Test
  @DisplayName("Der Bestandslauf schreibt nur traegt_nachrichten und bestand_geprueft_am (E15)")
  void bestandslauf_schreibt_nur_die_beobachtungsspalten() {
    repository.speichereBestandsflags(
        MANDANT, List.of(new ProzessKatalogRepository.Bestandsflag(PROZESS, true)), JETZT);
    String sql = klein(gerendert.getLast());

    assertThat(sql)
        .startsWith("update")
        .contains("traegt_nachrichten")
        .contains("bestand_geprueft_am");
    assertThat(sql)
        .as(
            "Kuratierung bleibt unangetastet: weder Pflegestatus noch Herkunft noch die"
                + " Aenderungsspuren duerfen im Statement stehen. Gerendert: %s",
            sql)
        .doesNotContain("pflegestatus")
        .doesNotContain("vorschlag_herkunft")
        .doesNotContain("geaendert_am")
        .doesNotContain("geaendert_von")
        .doesNotContain("partner")
        .doesNotContain("richtung");
    assertThat(sql)
        .as("Kein INSERT — der wuerde die NOT-NULL-Spalten mitschreiben. Gerendert: %s", sql)
        .doesNotContain("insert");
  }
}
