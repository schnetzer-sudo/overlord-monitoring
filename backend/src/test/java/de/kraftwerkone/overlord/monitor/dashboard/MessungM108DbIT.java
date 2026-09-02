package de.kraftwerkone.overlord.monitor.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.common.MessageStatusClassifier;
import de.kraftwerkone.overlord.monitor.common.Rollupzeitraum;
import de.kraftwerkone.overlord.monitor.common.Zeitfenster;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
 * <b>M108 — die Messung des Dashboard-Endpunkts</b> (Regel L7), je Zeitraumpaar und je Mandant.
 *
 * <h2>Was hier gemessen wird und was nicht</h2>
 *
 * <p>Gemessen wird <b>das, was der Code schickt</b>: Die Statements kommen aus {@link
 * DashboardRepository} und werden nicht abgeschrieben. Der {@code EXPLAIN} laeuft ueber das
 * <b>gerenderte</b> Statement mit Literalen ({@code StatementType.STATIC_STATEMENT}) — mit
 * Fragezeichen gaebe es keine Bereichsanalyse.
 *
 * <p><b>Dieser Laeufer ist eine Messung und kein Test.</b> Er sichert nichts ueber Wanduhrzeit zu
 * (Regel T1): Die Zeiten gehen ueber {@link #melde} nach {@code System.out} und in keine
 * Zusicherung. Die einzigen Zusicherungen betreffen <b>Zaehlwerte</b> — dass ueberhaupt etwas
 * gelesen wurde, sonst maesse er die Laufzeit von nichts.
 *
 * <p>Bezugswerte, gegen die die Zahlen zu lesen sind ({@code docs/dashboard.md} §Messung):
 *
 * <table>
 *   <caption>Erwartung aus M94, M107 und M90</caption>
 *   <tr><th>Ansicht</th><th>erwartet</th></tr>
 *   <tr><td>48 h — Verlauf / Verteilung</td><td>6,8 / 10,7 ms</td></tr>
 *   <tr><td>30 Tage — ueber die Tagesebene</td><td>rund 46 / 62 ms</td></tr>
 *   <tr><td>12 Monate — ueber die Monatsebene</td><td>65,4 / 88,7 ms</td></tr>
 *   <tr><td>Ueberfaellig — im Fenster / insgesamt</td><td>2,3 / 4,3 ms</td></tr>
 *   <tr><td><b>Landingpage gesamt</b></td><td><b>deutlich unter 500 ms</b></td></tr>
 * </table>
 *
 * <p><b>Weicht etwas deutlich ab, ist das ein Befund</b> — er gehoert gemeldet und nicht
 * nachgebessert.
 *
 * <p>Er schreibt nichts: alle Abfragen sind Lesezugriffe ueber {@code glassfishDsl}, und der {@code
 * ReadOnlyExecuteListener} haengt daran.
 */
@SpringBootTest
@ActiveProfiles("dev")
@Tag("db")
class MessungM108DbIT {

  /** Der Anker der Anwendungsuhr im Profil {@code dev} — derselbe Stichtag wie in M94 und M107. */
  private static final LocalDateTime ANKER = LocalDateTime.parse("2025-12-30T04:09:47");

  private static final List<String> MANDANTEN = List.of("NEXANS", "SUTTONS");

  /** Ein Aufwaermlauf, dann die beste von fuenf — dieselbe Bauform wie M94, M98 und M107. */
  private static final int LAEUFE = 5;

  @Autowired
  @Qualifier("glassfishDsl") private DSLContext glassfishDsl;

  @Autowired private DashboardRepository repository;

  @Autowired private DashboardService service;

  private final List<String> gerendert = new ArrayList<>();

  private static void melde(String schluessel, String wert) {
    System.out.println("M108 | " + schluessel + " | " + wert);
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

  /** Die beste von fuenf Laeufen nach einem Aufwaermlauf, in Nanosekunden. */
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

  private void planVon(String bezeichnung, String sql) {
    List<String> zeilen = new ArrayList<>();
    for (Record satz : glassfishDsl.fetch("explain " + sql)) {
      zeilen.add(
          satz.get("table")
              + " | "
              + satz.get("type")
              + " | key="
              + satz.get("key")
              + " | key_len="
              + satz.get("key_len")
              + " | rows="
              + satz.get("rows")
              + " | "
              + satz.get("Extra"));
    }
    assertThat(zeilen).as("Ein leerer Plan waere kein Plan").isNotEmpty();
    for (String zeile : zeilen) {
      melde("plan." + bezeichnung, zeile);
    }
  }

  @Test
  @DisplayName("M108 — der ganze Endpunkt, je Paar und je Mandant, mit Plaenen")
  void messung() {
    melde("anker", ANKER.toString());

    Map<String, Long> gesamtJeMandant = new LinkedHashMap<>();

    for (String mandantId : MANDANTEN) {
      MandantContext mandant = new MandantContext(mandantId);

      for (Rollupzeitraum zeitraum : Rollupzeitraum.reihe()) {
        Zeitfenster fenster = zeitraum.fenster(ANKER);
        String marke = mandantId + "." + zeitraum.code();
        melde("fenster." + marke, fenster.von() + " bis " + fenster.bis());

        List<Rollupsumme> verlauf = repository.verlauf(mandant, zeitraum, fenster);
        melde("verlauf.zeilen." + marke, String.valueOf(verlauf.size()));
        melde(
            "verlauf.nachrichten." + marke,
            String.valueOf(verlauf.stream().mapToLong(Rollupsumme::anzahl).sum()));
        melde(
            "verlauf.ms." + marke,
            ms(besteVonFuenf(() -> repository.verlauf(mandant, zeitraum, fenster).size())));

        List<Verteilungssumme> verteilung =
            repository.verteilung(mandant, zeitraum, fenster, Verteilungssicht.PARTNER);
        melde("verteilung.eimer." + marke, String.valueOf(verteilung.size()));
        melde(
            "verteilung.ms." + marke,
            ms(
                besteVonFuenf(
                    () ->
                        repository
                            .verteilung(mandant, zeitraum, fenster, Verteilungssicht.PARTNER)
                            .size())));

        Belegung belegung = repository.belegung(mandant, zeitraum, fenster);
        melde(
            "belegung." + marke,
            belegung.belegteEimer()
                + " von "
                + zeitraum.eimer()
                + ", groesster Eimer "
                + belegung.groessterEimer());
        melde(
            "belegung.ms." + marke,
            ms(
                besteVonFuenf(
                    () -> repository.belegung(mandant, zeitraum, fenster).belegteEimer())));
      }

      Zeitfenster kachelfenster = Rollupzeitraum.STUNDEN_48.fenster(ANKER);
      melde(
          "ueberfaellig.imFenster." + mandantId,
          String.valueOf(
              repository.ueberfaelligImFenster(mandant, kachelfenster, ANKER).orElse(-1)));
      melde(
          "ueberfaellig.imFenster.ms." + mandantId,
          ms(
              besteVonFuenf(
                  () ->
                      (int)
                          repository
                              .ueberfaelligImFenster(mandant, kachelfenster, ANKER)
                              .orElse(-1))));
      melde(
          "ueberfaellig.insgesamt." + mandantId,
          String.valueOf(repository.ueberfaelligInsgesamt(mandant, ANKER).orElse(-1)));
      melde(
          "ueberfaellig.insgesamt.ms." + mandantId,
          ms(
              besteVonFuenf(
                  () -> (int) repository.ueberfaelligInsgesamt(mandant, ANKER).orElse(-1))));

      melde(
          "aufgefallen.zeilen." + mandantId,
          String.valueOf(
              repository
                  .zuletztAufgefallen(
                      mandant, kachelfenster, ANKER, DashboardService.AUFFAELLIG_HOECHSTENS)
                  .size()));
      for (Rollupzeitraum zeitraum : Rollupzeitraum.reihe()) {
        Zeitfenster weit = zeitraum.fenster(ANKER);
        melde(
            "aufgefallen.ms." + mandantId + "." + zeitraum.code(),
            ms(
                besteVonFuenf(
                    () ->
                        repository
                            .zuletztAufgefallen(
                                mandant, weit, ANKER, DashboardService.AUFFAELLIG_HOECHSTENS)
                            .size())));
      }
      melde(
          "aufgefallen.ms." + mandantId,
          ms(
              besteVonFuenf(
                  () ->
                      repository
                          .zuletztAufgefallen(
                              mandant, kachelfenster, ANKER, DashboardService.AUFFAELLIG_HOECHSTENS)
                          .size())));
      melde("stand.ms", ms(besteVonFuenf(() -> repository.letzterLauf().isPresent() ? 1 : 0)));

      // ── Die ganze Landingpage, so wie der Endpunkt sie baut ──────────────────
      for (Rollupzeitraum zeitraum : Rollupzeitraum.reihe()) {
        long beste =
            besteVonFuenf(
                () ->
                    service
                        .landingpage(mandant, zeitraum, Verteilungssicht.PARTNER)
                        .verlauf()
                        .size());
        melde("landingpage.ms." + mandantId + "." + zeitraum.code(), ms(beste));
        gesamtJeMandant.merge(mandantId, beste, Math::max);
      }

      long ohneAngabe =
          besteVonFuenf(
              () -> service.landingpage(mandant, null, Verteilungssicht.PARTNER).verlauf().size());
      melde("landingpage.ms." + mandantId + ".standardfenster", ms(ohneAngabe));
      melde(
          "landingpage.zeitraum." + mandantId,
          service.landingpage(mandant, null, Verteilungssicht.PARTNER).zeitraum());
    }

    gesamtJeMandant.forEach(
        (mandantId, nanos) -> melde("landingpage.teuerstes_paar.ms." + mandantId, ms(nanos)));

    // ── Die Plaene, ueber das gerenderte Statement ────────────────────────────
    DashboardRepository attrappe = attrappe();
    MandantContext nexans = new MandantContext("NEXANS");
    for (Rollupzeitraum zeitraum : Rollupzeitraum.reihe()) {
      Zeitfenster fenster = zeitraum.fenster(ANKER);

      gerendert.clear();
      attrappe.verlauf(nexans, zeitraum, fenster);
      planVon("verlauf." + zeitraum.code(), gerendert.getFirst());

      gerendert.clear();
      attrappe.verteilung(nexans, zeitraum, fenster, Verteilungssicht.PARTNER);
      planVon("verteilung." + zeitraum.code(), gerendert.getFirst());

      gerendert.clear();
      attrappe.belegung(nexans, zeitraum, fenster);
      planVon("belegung." + zeitraum.code(), gerendert.getFirst());
    }

    Zeitfenster fenster = Rollupzeitraum.STUNDEN_48.fenster(ANKER);
    gerendert.clear();
    attrappe.ueberfaelligImFenster(nexans, fenster, ANKER);
    planVon("ueberfaellig.imFenster", gerendert.getFirst());

    gerendert.clear();
    attrappe.ueberfaelligInsgesamt(nexans, ANKER);
    planVon("ueberfaellig.insgesamt", gerendert.getFirst());

    for (Rollupzeitraum zeitraum : Rollupzeitraum.reihe()) {
      gerendert.clear();
      attrappe.zuletztAufgefallen(
          nexans, zeitraum.fenster(ANKER), ANKER, DashboardService.AUFFAELLIG_HOECHSTENS);
      for (int i = 0; i < gerendert.size(); i++) {
        planVon("aufgefallen." + zeitraum.code() + "." + (i + 1), gerendert.get(i));
      }
    }

    gerendert.clear();
    attrappe.letzterLauf();
    planVon("stand", gerendert.getFirst());

    // Die einzige Zusicherung dieses Laeufers: Es ist ueberhaupt etwas gelesen worden. Sonst
    // maesse er die Laufzeit von nichts — und alle Zahlen oben waeren wertlos.
    assertThat(
            repository
                .verlauf(
                    new MandantContext("NEXANS"),
                    Rollupzeitraum.STUNDEN_48,
                    Rollupzeitraum.STUNDEN_48.fenster(ANKER))
                .size())
        .as("Ohne Zeilen im Fenster misst dieser Laeufer nichts")
        .isPositive();
  }
}
