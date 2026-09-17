package de.kraftwerkone.overlord.monitor.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.common.MandantContext;
import de.kraftwerkone.overlord.monitor.common.MessageStatusClassifier;
import de.kraftwerkone.overlord.monitor.common.Rollupzeitraum;
import de.kraftwerkone.overlord.monitor.common.Zeitfenster;
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
 * <b>M178 — beide Sichten der Verteilung in einer Antwort</b> (Regel L7, E‑161).
 *
 * <h2>Was hier gemessen wird</h2>
 *
 * <p>Seit dem 16.09.2026 laeuft das Verteilungsstatement je Landingpage <b>zweimal</b>, einmal je
 * Sicht. Gemessen werden je {@code NEXANS} und {@code SUTTONS}, je 48 Stunden, 30 Tage, 12 Monate
 * und ohne {@code zeitraum}:
 *
 * <ol>
 *   <li><b>die Richtungsform einzeln</b> — das neue Statement;
 *   <li><b>die Partnerform einzeln</b>, in derselben Sitzung — der Vergleich ohne Tagesdrift;
 *   <li><b>die ganze Landingpage</b>, so wie der Endpunkt sie baut.
 * </ol>
 *
 * <p>Ohne {@code zeitraum} gibt es kein eigenes Verteilungsstatement: Es ist das des Paares, das
 * der Endpunkt waehlt. Der Laeufer fragt das Paar deshalb zuerst beim Service ab und misst dann
 * dessen Statement.
 *
 * <p>Die Vorregistrierung steht in {@code docs/dashboard.md} §8 und ist <b>vor</b> dem ersten Lauf
 * eingecheckt worden. Bauform wie M108 und M146: ein Aufwaermlauf, dann die beste von fuenf; der
 * {@code EXPLAIN} laeuft ueber das <b>gerenderte</b> Statement mit Literalen.
 *
 * <p><b>Dieser Laeufer ist eine Messung und kein Test.</b> Er sichert nichts ueber Wanduhrzeit zu
 * (Regel T1): Die Zeiten gehen nach {@code System.out} und in keine Zusicherung. Zugesichert sind
 * nur <b>Zaehlwerte</b> — dass ueberhaupt etwas gelesen wurde, sonst maesse er die Laufzeit von
 * nichts.
 *
 * <p>Er schreibt nichts: alle Abfragen sind Lesezugriffe ueber {@code glassfishDsl}, und der {@code
 * ReadOnlyExecuteListener} haengt daran.
 */
@SpringBootTest
@ActiveProfiles("dev")
@Tag("db")
class MessungM178DbIT {

  /** Der Anker der Anwendungsuhr im Profil {@code dev} — derselbe Stichtag wie in M108 und M146. */
  private static final LocalDateTime ANKER = LocalDateTime.parse("2025-12-30T04:09:47");

  /** Der dominante und der kleine Mandant — dieselben zwei wie in M108 (Regel L7). */
  private static final List<String> MANDANTEN = List.of("NEXANS", "SUTTONS");

  /** Ein Aufwaermlauf, dann die beste von fuenf — dieselbe Bauform wie M108 und M146. */
  private static final int LAEUFE = 5;

  @Autowired
  @Qualifier("glassfishDsl") private DSLContext glassfishDsl;

  @Autowired private DashboardRepository repository;

  @Autowired private DashboardService service;

  private final List<String> gerendert = new ArrayList<>();

  private static void melde(String schluessel, String wert) {
    System.out.println("M178 | " + schluessel + " | " + wert);
  }

  private static String ms(long nanos) {
    return String.format(Locale.GERMANY, "%.3f", nanos / 1_000_000.0);
  }

  /** Ein Repository, das nur rendert und nichts ausfuehrt — die Quelle fuer den {@code EXPLAIN}. */
  private DashboardRepository attrappe() {
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

  /** Eine Verteilungsform einzeln: Eimer, Summe, beste von fuenf. */
  private void verteilungsform(
      MandantContext mandant, Rollupzeitraum zeitraum, Verteilungssicht sicht, String marke) {
    Zeitfenster fenster = zeitraum.fenster(ANKER);
    String form = sicht.name().toLowerCase(Locale.ROOT);

    List<Verteilungssumme> summen = repository.verteilung(mandant, zeitraum, fenster, sicht);
    melde(form + ".eimer." + marke, String.valueOf(summen.size()));
    melde(
        form + ".nachrichten." + marke,
        String.valueOf(summen.stream().mapToLong(Verteilungssumme::anzahl).sum()));
    melde(
        form + ".ms." + marke,
        ms(besteVonFuenf(() -> repository.verteilung(mandant, zeitraum, fenster, sicht).size())));
  }

  @Test
  @DisplayName("M178 — Richtungsform, Partnerform und ganze Landingpage, je Lage und je Mandant")
  void messung() {
    melde("anker", ANKER.toString());

    for (String mandantId : MANDANTEN) {
      MandantContext mandant = new MandantContext(mandantId);

      // ── 1 und 2: die beiden Formen einzeln, je Paar, erst Richtung, dann Partner ─────────────
      for (Rollupzeitraum zeitraum : Rollupzeitraum.reihe()) {
        String marke = mandantId + "." + zeitraum.code();
        melde(
            "fenster." + marke,
            zeitraum.fenster(ANKER).von() + " bis " + zeitraum.fenster(ANKER).bis());
        verteilungsform(mandant, zeitraum, Verteilungssicht.RICHTUNG, marke);
        verteilungsform(mandant, zeitraum, Verteilungssicht.PARTNER, marke);
      }

      // Ohne zeitraum: das Statement des Paares, das der Endpunkt waehlt.
      Rollupzeitraum gewaehlt =
          Rollupzeitraum.ausCode(service.landingpage(mandant, null).zeitraum());
      String ohne = mandantId + ".ohne(" + gewaehlt.code() + ")";
      verteilungsform(mandant, gewaehlt, Verteilungssicht.RICHTUNG, ohne);
      verteilungsform(mandant, gewaehlt, Verteilungssicht.PARTNER, ohne);

      // ── 3: die ganze Landingpage, so wie der Endpunkt sie baut ─────────────────────────────
      for (Rollupzeitraum zeitraum : Rollupzeitraum.reihe()) {
        DashboardResponse antwort = service.landingpage(mandant, zeitraum);
        melde(
            "landingpage.zeilen." + mandantId + "." + zeitraum.code(),
            "partner "
                + antwort.verteilung().partner().zeilen().size()
                + ", richtung "
                + antwort.verteilung().richtung().zeilen().size()
                + ", nachrichten "
                + antwort.kacheln().nachrichten());
        melde(
            "landingpage.ms." + mandantId + "." + zeitraum.code(),
            ms(besteVonFuenf(() -> service.landingpage(mandant, zeitraum).verlauf().size())));
      }
      melde(
          "landingpage.ms." + mandantId + ".ohne(" + gewaehlt.code() + ")",
          ms(besteVonFuenf(() -> service.landingpage(mandant, null).verlauf().size())));
    }

    // ── Die Plaene der Richtungsform, ueber das gerenderte Statement, fuer beide Mandanten ─────
    DashboardRepository attrappe = attrappe();
    for (String mandantId : MANDANTEN) {
      for (Rollupzeitraum zeitraum : Rollupzeitraum.reihe()) {
        gerendert.clear();
        attrappe.verteilung(
            new MandantContext(mandantId),
            zeitraum,
            zeitraum.fenster(ANKER),
            Verteilungssicht.RICHTUNG);
        assertThat(gerendert).as("Ein Statement je Sicht, nicht zwei").hasSize(1);
        planVon("richtung." + mandantId + "." + zeitraum.code(), gerendert.getFirst());
      }
    }

    // Die einzige Zusicherung dieses Laeufers: Es ist ueberhaupt etwas gelesen worden. Sonst
    // maesse er die Laufzeit von nichts. Der Wert selbst ist NICHT festgenagelt (Regel T2).
    assertThat(
            repository.verteilung(
                new MandantContext("NEXANS"),
                Rollupzeitraum.STUNDEN_48,
                Rollupzeitraum.STUNDEN_48.fenster(ANKER),
                Verteilungssicht.RICHTUNG))
        .as("Ohne Rollupzeilen im Fenster misst dieser Laeufer nichts")
        .isNotEmpty();
  }
}
