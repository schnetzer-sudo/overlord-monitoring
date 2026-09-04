package de.kraftwerkone.overlord.monitor.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.common.MessageStatusClassifier;
import de.kraftwerkone.overlord.monitor.common.Rollupzeitraum;
import de.kraftwerkone.overlord.monitor.common.Zeitfenster;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.conf.Settings;
import org.jooq.conf.StatementType;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockDataProvider;
import org.jooq.tools.jdbc.MockResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * <b>M146 — „Zuletzt aufgefallen" je Prozess statt je Nachricht</b> (Regel L7).
 *
 * <h2>Was hier gemessen wird und warum es diesen Laeufer braucht</h2>
 *
 * <p>Der Block gruppiert seit E‑90 nach Prozess. Damit faellt der Grund weg, aus dem der
 * Indexhinweis in M108 gewirkt hat: Er verbot, den Zeitindex <i>zur Sortierung</i> zu nehmen, und
 * ueber einer Gruppierung gibt es keine freie Sortierung mehr. <b>Der Optimierer koennte den
 * Zeitindex trotzdem fuer den Bereich waehlen</b> — und dann laese die Abfrage 2,7 Millionen Zeilen
 * statt 6.257, genau der Befund aus {@code docs/dashboard.md} §7a.
 *
 * <p>Die Vorarbeit dazu ist in SQL gefahren ({@code scripts/messung-schritt10c-verdichtung/}): vier
 * Fassungen — ohne Hinweis, {@code IGNORE INDEX FOR ORDER BY}, volles {@code IGNORE INDEX}, {@code
 * FORCE INDEX} — und alle vier steigen ueber {@code MessageStatusIDX} ein. <b>Dieser Laeufer misst
 * dasselbe an dem, was der Code wirklich schickt</b>, und schliesst damit die Luecke zwischen der
 * handgeschriebenen Abfrage und dem gerenderten Statement.
 *
 * <p><b>Dieser Laeufer ist eine Messung und kein Test.</b> Er sichert nichts ueber Wanduhrzeit zu
 * (Regel T1): Die Zeiten gehen nach {@code System.out} und in keine Zusicherung. Zugesichert sind
 * nur <b>Zaehlwerte</b> — dass ueberhaupt etwas gelesen wurde, sonst maesse er die Laufzeit von
 * nichts.
 *
 * <p><b>Vergleichsmass</b> ist M108/M145: konstante 22 bis 26 ms ueber alle Mandanten und
 * Fensterbreiten. Reisst eine Zahl die Groessenordnung, ist das ein <b>Befund</b> und gehoert
 * gemeldet, nicht nachgebessert.
 *
 * <p>Er schreibt nichts: alle Abfragen sind Lesezugriffe ueber {@code glassfishDsl}, und der {@code
 * ReadOnlyExecuteListener} haengt daran.
 */
@SpringBootTest
@ActiveProfiles("dev")
@Tag("db")
class MessungM146DbIT {

  /** Der Anker der Anwendungsuhr im Profil {@code dev} — derselbe Stichtag wie in M108. */
  private static final LocalDateTime ANKER = LocalDateTime.parse("2025-12-30T04:09:47");

  /**
   * Die drei Mandanten aus M108 — und {@code VOTG} ist der wichtigste von ihnen: Er hat ueber den
   * <i>gesamten</i> Bestand acht Fehlerzeilen in einem einzigen Prozess. <b>Der gute Fall ist der
   * teure</b>, weil die Datenbank den ganzen Bereich durchsuchen muss, bevor sie „fast nichts"
   * sagen darf.
   */
  private static final List<String> MANDANTEN = List.of("NEXANS", "SUTTONS", "VOTG");

  /** Ein Aufwaermlauf, dann die beste von fuenf — dieselbe Bauform wie M94, M98, M107 und M108. */
  private static final int LAEUFE = 5;

  @Autowired
  @Qualifier("glassfishDsl") private DSLContext glassfishDsl;

  @Autowired private DashboardRepository repository;

  private final List<String> gerendert = new ArrayList<>();

  private static void melde(String schluessel, String wert) {
    System.out.println("M146 | " + schluessel + " | " + wert);
  }

  private static String ms(long nanos) {
    return String.format(Locale.GERMANY, "%.3f", nanos / 1_000_000.0);
  }

  /** Ein Repository, das nur rendert und nichts ausfuehrt — die Quelle fuer den {@code EXPLAIN}. */
  private DashboardRepository attrappe() {
    gerendert.clear();
    MockDataProvider mock =
        ausfuehrung -> {
          gerendert.add(ausfuehrung.sql());
          DSLContext leer = DSL.using(SQLDialect.MARIADB);
          return new MockResult[] {new MockResult(0, leer.newResult())};
        };
    return new DashboardRepository(
        DSL.using(
            new MockConnection(mock),
            SQLDialect.MARIADB,
            new Settings().withStatementType(StatementType.STATIC_STATEMENT)),
        new MessageStatusClassifier());
  }

  private long besteVonFuenf(Supplier<Integer> aufruf) {
    aufruf.get();
    long beste = Long.MAX_VALUE;
    for (int i = 0; i < LAEUFE; i++) {
      long vorher = System.nanoTime();
      aufruf.get();
      beste = Math.min(beste, System.nanoTime() - vorher);
    }
    return beste;
  }

  /**
   * Der Plan des gerenderten Statements — und die <b>Zusicherung</b>, dass der Treiberindex der
   * Statusindex geblieben ist.
   *
   * <p>Das ist keine Zeitmessung und deshalb nach Regel T1 zusicherbar: Geprueft wird die
   * <i>Ursache</i>, nicht die Wanduhrzeit.
   */
  private void planVon(String bezeichnung, String sql) {
    List<String> zeilen = new ArrayList<>();
    String indexAufMessage = null;
    for (Record satz : glassfishDsl.fetch("explain " + sql)) {
      zeilen.add(
          satz.get("table")
              + " | "
              + satz.get("type")
              + " | key="
              + satz.get("key")
              + " | rows="
              + satz.get("rows")
              + " | "
              + satz.get("Extra"));
      if ("m".equals(String.valueOf(satz.get("table")))
          || "Message".equals(String.valueOf(satz.get("table")))) {
        indexAufMessage = String.valueOf(satz.get("key"));
      }
    }
    assertThat(zeilen).as("Ein leerer Plan waere kein Plan").isNotEmpty();
    for (String zeile : zeilen) {
      melde("plan." + bezeichnung, zeile);
    }
    assertThat(indexAufMessage)
        .as(
            "Ueber MessageLastUpdateIDX laese die Abfrage den ganzen Zeitbereich statt der"
                + " auffaelligen Zeilen — der Befund aus §7a, und die Gruppierung darf ihn nicht"
                + " zurueckholen (%s)",
            bezeichnung)
        .isEqualTo("MessageStatusIDX");
  }

  @Test
  @DisplayName("M146: der verdichtete Block, drei Mandanten, drei Fenster — Plan und Laufzeit")
  void verdichteter_block() {
    for (String mandantId : MANDANTEN) {
      MandantContext mandant = new MandantContext(mandantId);

      for (Rollupzeitraum zeitraum : Rollupzeitraum.reihe()) {
        Zeitfenster fenster = zeitraum.fenster(ANKER);

        List<AuffaelligerProzess> zeilen =
            repository.zuletztAufgefallen(mandant, fenster, DashboardService.AUFFAELLIG_HOECHSTENS);

        // Die Zahl der Zeilen ist die Auskunft dieses Schritts: Wo vorher zehn gleiche Zeilen
        // standen, stehen jetzt so viele, wie es betroffene Prozesse gibt.
        melde(
            "zeilen." + mandantId + "." + zeitraum.code(),
            zeilen.size()
                + " Prozesse, Summe "
                + zeilen.stream().mapToInt(AuffaelligerProzess::anzahl).sum()
                + " Nachrichten");
        for (AuffaelligerProzess zeile : zeilen) {
          melde(
              "zeile." + mandantId + "." + zeitraum.code(),
              zeile.anzahl() + " × " + (zeile.processName() == null ? "(ohne Namen)" : "…"));
        }

        melde(
            "ms." + mandantId + "." + zeitraum.code(),
            ms(
                besteVonFuenf(
                    () ->
                        repository
                            .zuletztAufgefallen(
                                mandant, fenster, DashboardService.AUFFAELLIG_HOECHSTENS)
                            .size())));

        DashboardRepository attrappe = attrappe();
        attrappe.zuletztAufgefallen(mandant, fenster, DashboardService.AUFFAELLIG_HOECHSTENS);
        assertThat(gerendert).as("Ein Statement, nicht zwei").hasSize(1);
        planVon(mandantId + "." + zeitraum.code(), gerendert.getFirst());
      }
    }

    // Die einzige zaehlende Zusicherung: Bei NEXANS ueber 48 Stunden muss ueberhaupt etwas
    // dastehen. Sonst maesse dieser Laeufer die Laufzeit von nichts — und alle Zahlen oben waeren
    // wertlos. Der Wert selbst ist NICHT festgenagelt (Regel T2): Er haengt am Bestand der
    // geteilten Testkopie.
    assertThat(
            repository
                .zuletztAufgefallen(
                    new MandantContext("NEXANS"),
                    Rollupzeitraum.STUNDEN_48.fenster(ANKER),
                    DashboardService.AUFFAELLIG_HOECHSTENS)
                .size())
        .as("Ohne auffaellige Zeilen im Fenster misst dieser Laeufer nichts")
        .isPositive();
  }
}
