package de.kraftwerkone.overlord.monitor.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.common.Pflegestatus;
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
    repository.findeUebernehmbareVorschlaege(MANDANT);
    repository.uebernehmeVorschlaege(
        MANDANT, List.of(PROZESS, "AuslagerungAusgehendERFUNDEN"), JETZT, BENUTZER);
    return List.copyOf(gerendert);
  }

  /** Nur das Statement des Bestandslaufs, ohne die uebrigen. */
  private String bestandsStatement() {
    repository.findeBestandsflags(MANDANT);
    return gerendert.getLast();
  }

  /** Nur das lesende Statement der Vorschlagsuebernahme (E22). */
  private String uebernahmeLesetext() {
    gerendert.clear();
    repository.findeUebernehmbareVorschlaege(MANDANT);
    assertThat(gerendert).as("Die Lesung ist genau ein Statement").hasSize(1);
    return gerendert.getFirst();
  }

  /** Nur das schreibende Statement der Vorschlagsuebernahme (E22). */
  private String uebernahmeSchreibtext() {
    gerendert.clear();
    repository.uebernehmeVorschlaege(MANDANT, List.of(PROZESS), JETZT, BENUTZER);
    assertThat(gerendert).as("Das Schreiben ist genau ein Statement, kein Stapel").hasSize(1);
    return gerendert.getFirst();
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

  /** Genau ein Schreibweg, einzeln gerendert — der Text, den er wirklich absetzt. */
  private String schreibweg(Runnable aufruf) {
    gerendert.clear();
    aufruf.run();
    List<String> schreibend =
        gerendert.stream().filter(sql -> !klein(sql).startsWith("select")).toList();
    assertThat(schreibend).as("Ein Schreibweg setzt genau ein Statement ab").hasSize(1);
    return klein(schreibend.getFirst());
  }

  /**
   * <b>Die Schreibwege des Katalogs, namentlich gefuehrt — drei mit Upsert, zwei ohne, jeder mit
   * seinem Grund.</b>
   *
   * <p>Dieser Test hat zwei Schaerfungen hinter sich, und beide sind aus demselben Satz begruendet:
   * <b>Eine Ausnahme, die nur durch Weglassen entstuende, waere beim naechsten Umbau wieder da.</b>
   *
   * <ol>
   *   <li><b>21.08.2026, der Bestandslauf.</b> Der Test verlangte von <i>jedem</i> Schreiben ein
   *       {@code INSERT … ON DUPLICATE KEY UPDATE}. Der Bestandslauf ist bewusst keins (E15) — also
   *       ist er als eigener Schreibweg gefuehrt worden, mit der Forderung nach dem Gegenteil.
   *   <li><b>26.08.2026, die Vorschlagsuebernahme.</b> Die Aufteilung lief bis dahin ueber ein
   *       Textmerkmal ({@code traegt_nachrichten} im Statement). Ein zweiter Schreibweg ohne Upsert
   *       waere darueber stillschweigend in die falsche Haelfte gerutscht und rot geworden — oder,
   *       schlimmer, ueber ein zweites Merkmal wieder herausgenommen worden. <b>Gefuehrt werden
   *       deshalb die Wege und nicht die Merkmale.</b>
   * </ol>
   *
   * <p>Die Zaehlung unten ist der Riegel davor: Ein sechster Schreibweg laesst diesen Test
   * fehlschlagen, bevor jemand vergisst, ihn hier zu benennen.
   */
  @Test
  @DisplayName("Fuenf Schreibwege, namentlich: drei kuratierende mit Upsert, zwei ohne")
  void schreibende_statements_sind_upserts() {
    // ─── Mit Upsert: die drei, bei denen die Zeile fehlen darf ────────────────
    //
    // Alle drei koennen auf einen Prozess treffen, zu dem es noch KEINE Katalogzeile gibt — und
    // das sind genau die, um die es geht. Ein reines UPDATE liesse sie unberuehrt.

    // 1. Die einzelne Zuordnung (E19): Der Nutzer kuratiert eine Zeile, die es noch nie gab.
    assertThat(
            schreibweg(
                () ->
                    repository.speichereZuordnung(
                        MANDANT, PROZESS, "BAYER", Richtung.EINGEHEND, JETZT, BENUTZER)))
        .as("speichereZuordnung schreibt auf eine Zeile, die es noch nicht geben muss (E4)")
        .startsWith("insert into")
        .contains("on duplicate key update");

    // 2. Die Massenzuordnung (E11): Ein Projekt enthaelt Prozesse ohne Katalogzeile.
    assertThat(
            schreibweg(
                () -> {
                  ergebnisEineProjektzeile();
                  repository.speichereFeld(
                      MANDANT, PROJEKT, Zuordnungsfeld.PARTNER, "BAYER", JETZT, BENUTZER);
                }))
        .as("speichereFeld legt fuer Prozesse ohne Zeile eine an (E11)")
        .startsWith("insert into")
        .contains("on duplicate key update");

    // 3. Der Heuristik-Lauf (E13): Sein erster Schritt IST das Anlegen fehlender Zeilen.
    assertThat(
            schreibweg(
                () ->
                    repository.speichereVorschlaege(
                        MANDANT,
                        List.of(
                            new ProzessKatalogRepository.Vorschlagszeile(
                                PROZESS, Partnervorschlag.KEINER)),
                        JETZT,
                        BENUTZER)))
        .as("speichereVorschlaege legt fehlende Zeilen an — das ist Schritt 1 des Laufs (E13)")
        .startsWith("insert into")
        .contains("on duplicate key update");

    // ─── Ohne Upsert: die zwei, bei denen die Zeile notwendigerweise existiert ─

    // 4. Der Bestandslauf (E15) — das einzige Schreiben auf GEPFLEGTE Zeilen.
    //    Ein INSERT muesste die NOT-NULL-Spalten mitliefern (pflegestatus, vorschlag_herkunft,
    //    geaendert_am, geaendert_von) und ueberschriebe genau die Kuratierung, die er nicht
    //    anfassen darf. Fuer ihn ist das UPDATE nicht die schwaechere, sondern die einzig
    //    richtige Form. Dass er Prozesse ohne Katalogzeile nicht erreicht, ist folgenlos: Der
    //    Dienst faehrt ihn NACH speichereVorschlaege.
    String bestandslauf =
        schreibweg(
            () ->
                repository.speichereBestandsflags(
                    MANDANT,
                    List.of(new ProzessKatalogRepository.Bestandsflag(PROZESS, true)),
                    JETZT));
    assertThat(bestandslauf)
        .as("Der Bestandslauf ist bewusst kein Upsert (E15): %s", bestandslauf)
        .startsWith("update")
        .doesNotContain("on duplicate key update")
        .contains("traegt_nachrichten");

    // 5. Die Vorschlagsuebernahme (E22) — der erste KURATIERENDE Weg ohne Upsert.
    //    Die Zeile existiert notwendigerweise: Nur der Lauf schreibt REGEL_A/REGEL_B, und er
    //    schreibt sie in eine vorhandene oder eben angelegte Zeile. Ein INSERT-Zweig waere
    //    unerreichbar, und der Upsert muesste vier Spalten zurueckschreiben, die er gar nicht
    //    aendern will: partner, richtung, vorschlag_herkunft und den Schluessel.
    String uebernahme =
        schreibweg(
            () -> repository.uebernehmeVorschlaege(MANDANT, List.of(PROZESS), JETZT, BENUTZER));
    assertThat(uebernahme)
        .as("Die Uebernahme ist bewusst kein Upsert (E22): %s", uebernahme)
        .startsWith("update")
        .doesNotContain("on duplicate key update")
        .contains("pflegestatus");

    // ─── Der Riegel: ein sechster Schreibweg faellt hier auf ──────────────────
    // Geleert, weil die Aufrufe oben bereits in `gerendert` stehen: `alleStatements` haengt an,
    // statt zu ersetzen.
    gerendert.clear();
    long schreibwege =
        alleStatements().stream().filter(sql -> !klein(sql).startsWith("select")).count();
    assertThat(schreibwege)
        .as(
            "Fuenf Schreibwege sind oben namentlich gefuehrt. Kommt ein sechster dazu, gehoert er"
                + " hierher — mit seinem Grund und nicht durch Weglassen")
        .isEqualTo(5);
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
    assertThat(alleStatements()).hasSize(15);
  }

  // ─── Die Vorschlagsuebernahme (E22 bis E24) ──────────────────────────────────

  /**
   * <b>Die Lesung der Uebernahme steht auf {@code ProjectMandant} und traegt die volle Bedingung
   * aus E22.</b>
   *
   * <p>Geprueft werden Text <b>und</b> Bindewerte. Eine reine Textpruefung auf {@code 'OFFEN'}
   * ginge still ins Leere: jOOQ <b>bindet</b> Werte, statt sie einzusetzen — im gerenderten
   * Statement steht ein {@code ?}.
   */
  @Test
  @DisplayName("Die Uebernahme liest ueber ProjectMandant und filtert auf OFFEN plus Regel A/B")
  void uebernahme_liest_mit_der_vollen_bedingung() {
    String sql = uebernahmeLesetext();
    String klein = klein(sql);

    assertThat(klein)
        .as("Einstieg ueber ProjectMandant — wie in jedem lesenden Statement (M3). %s", sql)
        .contains("from `glassfishdb`.`projectmandant`");
    assertThat(klein).as("Und er filtert ueber die MandantID: %s", sql).contains("mandantid");
    assertThat(sql)
        .as("Ohne volle Qualifizierung laeuft der schemauebergreifende Join ins Leere: %s", sql)
        .contains("GlassfishDB")
        .contains("overlord_monitor");
    assertThat(klein)
        .as("Beide Spalten der Bedingung stehen im Text: %s", sql)
        .contains("pflegestatus")
        .contains("vorschlag_herkunft");
    assertThat(bindungen)
        .as("Uebernommen wird, was offen ist und einen Partnervorschlag traegt (E22)")
        .contains(
            MANDANT.mandantId(),
            Pflegestatus.OFFEN.name(),
            VorschlagHerkunft.REGEL_A.name(),
            VorschlagHerkunft.REGEL_B.name());
    assertThat(bindungen)
        .as(
            "KEINE gehoert ausdruecklich NICHT dazu — sonst erfaende der Knopf bei NEXANS 224"
                + " Behauptungen, die kein Mensch getroffen hat (E4, E22)")
        .doesNotContain(VorschlagHerkunft.KEINE.name());
  }

  /**
   * <b>{@code INNER JOIN} auf {@code process_catalog}, kein {@code LEFT JOIN}</b> — der Unterschied
   * zur Pflegeliste, und er ist die Aussage.
   *
   * <p>Die Pflegeliste ebnet Prozesse ohne Katalogzeile auf {@code OFFEN}/{@code KEINE} ein, weil
   * sie fuer den Nutzer genau das sind. Hier waere dieselbe Einebnung falsch: Eine Zeile, die es
   * nicht gibt, kann keinen Vorschlag tragen.
   */
  @Test
  @DisplayName("Die Uebernahme haengt process_catalog als INNER JOIN an, nicht als LEFT JOIN")
  void uebernahme_ebnet_fehlende_zeilen_nicht_ein() {
    String sql = uebernahmeLesetext();
    String klein = klein(sql);

    assertThat(klein).contains("`overlord_monitor`.`process_catalog`");
    assertThat(klein)
        .as(
            "Ein LEFT JOIN behauptete im Text, eine fehlende Zeile koenne einen Vorschlag tragen."
                + " Gerendert: %s",
            sql)
        .doesNotContain("left outer join");
    assertThat(klein)
        .as("Gerendert: %s", sql)
        .contains("join `overlord_monitor`.`process_catalog`");
  }

  @Test
  @DisplayName("Die Uebernahme fasst Message nicht an und traegt keinen STRAIGHT_JOIN")
  void uebernahme_kennt_keinen_verkehr() {
    String klein = klein(uebernahmeLesetext());

    assertThat(klein)
        .as("Diese Abfrage kennt keinen Verkehr und will keinen (L2)")
        .doesNotContain("`message`");
    assertThat(klein)
        .as("Kein STRAIGHT_JOIN, auch nicht als Reparatur (M42)")
        .doesNotContain("straight_join");
  }

  /**
   * <b>Vorschau und Ausfuehrung fahren denselben Lesetext</b> (E24) — und hier ist die Zusicherung
   * <b>staerker</b> als bei der Massenzuordnung.
   *
   * <p>Dort ruft {@code speichereFeld} die Lesung selbst auf, und der Test vergleicht zwei
   * gerenderte Texte. Hier gibt es <b>gar keinen zweiten Text</b>: Der Dienst liest <b>einmal</b>,
   * zaehlt {@code regelA}/{@code regelB} ueber genau diese Liste und schreibt genau sie — der
   * Schreibweg traegt <b>kein</b> {@code SELECT}. Damit kann nichts driften, weil es nichts gibt,
   * was auseinanderlaufen koennte.
   *
   * <p>Geprueft wird beides: dass die Lesung Zeichen fuer Zeichen dieselbe bleibt, und dass das
   * Schreiben keine eigene Lesung mitbringt.
   */
  @Test
  @DisplayName("Die Uebernahme hat genau einen Lesetext, und das Schreiben bringt keinen zweiten")
  void vorschau_und_ausfuehrung_teilen_ein_statement_bei_der_uebernahme() {
    String ersteLesung = uebernahmeLesetext();
    String zweiteLesung = uebernahmeLesetext();

    assertThat(zweiteLesung)
        .as(
            "Getrennt gebaut driften die beiden auseinander, und der Nutzer bestaetigt eine Zahl,"
                + " die nicht die ist, die passiert")
        .isEqualTo(ersteLesung);

    assertThat(klein(uebernahmeSchreibtext()))
        .as(
            "Ein SELECT im Schreibweg waere die zweite Stelle, an der dieselbe Bedingung stuende —"
                + " genau das, was E24 ausschliesst")
        .doesNotContain("select");
  }

  /**
   * <b>Das Schreiben setzt genau drei Spalten</b> und fasst Partner, Richtung und Herkunft nicht
   * an.
   *
   * <p>Uebernehmen ist <b>kein Kopieren von Werten</b>: Partner und Richtung stehen bereits in der
   * Zeile, die Heuristik hat sie beim Lauf geschrieben. Und {@code vorschlag_herkunft} bleibt
   * stehen, weil sie danach der einzige Hinweis darauf ist, dass der Wert aus einer Regel und nicht
   * aus einem Kopf stammt.
   */
  @Test
  @DisplayName("Das Schreiben der Uebernahme setzt genau drei Spalten (E22)")
  void uebernahme_schreibt_nur_den_status_und_den_vermerk() {
    String sql = uebernahmeSchreibtext();
    String klein = klein(sql);

    assertThat(klein)
        .startsWith("update")
        .contains("pflegestatus")
        .contains("geaendert_am")
        .contains("geaendert_von");
    assertThat(klein)
        .as(
            "Partner und Richtung stehen schon da; die Herkunft ist der einzige verbliebene Beleg"
                + " dafuer, dass der Wert aus einer Regel stammt. Gerendert: %s",
            sql)
        .doesNotContain("partner")
        .doesNotContain("richtung")
        .doesNotContain("vorschlag_herkunft");
    assertThat(bindungen)
        .as(
            "Gesetzt wird GEPFLEGT — die Zahl, die der Nutzer bestaetigt hat, ist die, die passiert")
        .contains(Pflegestatus.GEPFLEGT.name());
    assertThat(sql)
        .as("Kein Schreibzugriff beruehrt jemals GlassfishDB: %s", sql)
        .doesNotContain("GlassfishDB");
  }

  @Test
  @DisplayName("Eine leere Menge setzt gar kein UPDATE ab")
  void uebernahme_ohne_zeilen_schreibt_nicht() {
    int geschrieben = repository.uebernehmeVorschlaege(MANDANT, List.of(), JETZT, BENUTZER);

    assertThat(geschrieben).isZero();
    assertThat(gerendert)
        .as(
            "Eine IN-Liste ohne Werte waere entweder ein Syntaxfehler oder ein UPDATE ohne"
                + " Bedingung — beides will hier niemand")
        .isEmpty();
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
