package de.kraftwerkone.overlord.monitor.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.common.LiveRestRepository;
import de.kraftwerkone.overlord.monitor.common.MandantContext;
import de.kraftwerkone.overlord.monitor.common.Rollupzeitraum;
import de.kraftwerkone.overlord.monitor.common.Wasserstand;
import de.kraftwerkone.overlord.monitor.security.Rolle;
import de.kraftwerkone.overlord.monitor.security.SicherheitsTestbasis;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.convention.TestBean;

/**
 * <b>M186 — der Live-Rest am Dashboard</b> (Teil B, Regel L7; {@code docs/live-rest.md} §8b).
 *
 * <h2>Was hier gemessen wird</h2>
 *
 * <p>Dieselben acht Faelle wie M185 — je Mandant die typische Stunde (Live-Bereich zwei Eimer) und
 * der dichteste Vierstundenbereich —, und je Fall die <b>drei Paare</b> der Landingpage: durch den
 * Endpunkt (HTTP-Umlauf im Testclient samt Sitzung und Serialisierung) und am Dienst ({@code
 * DashboardService.landingpage} im selben Prozess). Dazu <b>als Bezug in derselben Sitzung</b>: die
 * Seite am Dienst mit einem Wasserstand, der die Stunde deckt ({@code NICHT_NOETIG}) — dieselbe
 * Seite ohne die drei bis vier Statements des Live-Rests. Die Differenz ist der Zuschlag.
 *
 * <p><b>Uhr und Wasserstand sind gestellt</b> — ueber {@code @TestBean} an {@code devClock} und
 * {@code Wasserstand}, wie in M185; kein Schreibzugriff auf {@code rollup_lauf}. Ein Aufwaermlauf,
 * dann die beste von fuenf.
 *
 * <p><b>Dieser Laeufer ist eine Messung und kein Test</b> (Regel T1): Zeiten gehen nach {@code
 * System.out} und in keine Zusicherung. Zugesichert sind nur {@code 200}, {@code ANGEWANDT} und
 * dass Kachel und beide Sichten dieselbe Zahl tragen — sonst maesse er die Laufzeit einer falschen
 * Seite.
 */
class MessungM186DbIT extends SicherheitsTestbasis {

  private static final String PFAD = "/api/dashboard";

  private static final String PASSWORT = "einLangesPasswort1";

  private static final int LAEUFE = 5;

  private static final ZoneId ZONE = ZoneId.systemDefault();

  static final class Messuhr extends Clock {
    private volatile Instant jetzt = Instant.EPOCH;

    void stelle(LocalDateTime wanduhr) {
      jetzt = wanduhr.atZone(ZONE).toInstant();
    }

    @Override
    public ZoneId getZone() {
      return ZONE;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return jetzt;
    }
  }

  static final class GesetzterWasserstand implements Wasserstand {
    private volatile LocalDateTime wert;

    @Override
    public Optional<LocalDateTime> wasserstand() {
      return Optional.ofNullable(wert);
    }
  }

  private static final Messuhr MESSUHR = new Messuhr();
  private static final GesetzterWasserstand WASSERSTAND = new GesetzterWasserstand();

  @TestBean(name = "devClock", methodName = "messuhr")
  private Clock anwendungsuhr;

  static Clock messuhr() {
    return MESSUHR;
  }

  @TestBean(methodName = "gesetzterWasserstand")
  private Wasserstand wasserstand;

  static Wasserstand gesetzterWasserstand() {
    return WASSERSTAND;
  }

  private record Fall(String mandant, String art, LocalDateTime g, int minutenNachG) {
    LocalDateTime jetzt() {
      return g.plusMinutes(minutenNachG);
    }
  }

  /** Die Messstunden aus M185, unveraendert ({@code docs/live-rest.md} §8). */
  private static final List<Fall> FAELLE =
      List.of(
          new Fall("NEXANS", "typisch", LocalDateTime.parse("2025-11-09T15:00"), 90),
          new Fall("NEXANS", "dicht", LocalDateTime.parse("2024-10-09T18:00"), 180),
          new Fall("VOTG", "typisch", LocalDateTime.parse("2025-06-22T01:00"), 90),
          new Fall("VOTG", "dicht", LocalDateTime.parse("2025-06-26T07:00"), 180),
          new Fall("IBIS", "typisch", LocalDateTime.parse("2025-08-13T14:00"), 90),
          new Fall("IBIS", "dicht", LocalDateTime.parse("2024-11-29T10:00"), 180),
          new Fall("SUTTONS", "typisch", LocalDateTime.parse("2025-06-09T19:00"), 90),
          new Fall("SUTTONS", "dicht", LocalDateTime.parse("2025-06-12T07:00"), 180));

  @Autowired private DashboardService dashboardService;
  @Autowired private LiveRestRepository liveRestRepository;

  @Test
  @DisplayName("M186 — die Landingpage mit Live-Rest, je Paar, beste von fuenf")
  void live_rest_am_dashboard() throws IOException, InterruptedException {
    assertThat(anwendungsuhr).as("Die Anwendungsuhr ist die gestellte").isSameAs(MESSUHR);
    assertThat(wasserstand).as("Der Wasserstand ist der gesetzte").isSameAs(WASSERSTAND);
    System.out.println(
        "M186 | mandant | art     | paar | g                | live_eimer | rollupzeilen | quellzeilen"
            + " | endpunkt_best_ms | endpunkt_laeufe_ms | dienst_best_ms | dienst_laeufe_ms"
            + " | bezug_best_ms | bezug_laeufe_ms | zuschlag_ms | rumpf_bytes | nachrichten");

    for (Fall fall : FAELLE) {
      String nutzer =
          PRAEFIX + "m186-" + fall.mandant().toLowerCase(Locale.ROOT) + "-" + fall.art();
      legeNutzerAn(nutzer, PASSWORT, Rolle.MANDANT, fall.mandant());
      Sitzung sitzung = anmelden(nutzer, PASSWORT);
      MandantContext kontext = new MandantContext(fall.mandant());

      MESSUHR.stelle(fall.jetzt());
      LocalDateTime liveBis = fall.jetzt().truncatedTo(ChronoUnit.HOURS).plusHours(1);
      int liveEimer = (int) Duration.between(fall.g(), liveBis).toHours();
      int rollupzeilen = liveRestRepository.ausDemRollup(kontext, fall.g(), liveBis).size();
      int quellzeilen = liveRestRepository.ausDerQuelle(kontext, fall.g(), liveBis).size();

      for (Rollupzeitraum paar : Rollupzeitraum.reihe()) {
        String abfrage = "?zeitraum=" + paar.code();

        // ── Der Bezug: dieselbe Seite, der Wasserstand deckt die Stunde (NICHT_NOETIG) ────────
        WASSERSTAND.wert = liveBis.plusHours(1);
        DashboardResponse bezugAntwort = dashboardService.landingpage(kontext, paar);
        assertThat(bezugAntwort.liveRest().zustand().name()).isEqualTo("NICHT_NOETIG");
        double[] bezug = new double[LAEUFE];
        for (int i = 0; i < LAEUFE; i++) {
          long start = System.nanoTime();
          dashboardService.landingpage(kontext, paar);
          bezug[i] = (System.nanoTime() - start) / 1_000_000.0;
        }

        // ── Die Seite mit Live-Rest, durch den Endpunkt ─────────────────────────────────────
        WASSERSTAND.wert = fall.g().plusHours(1);
        Antwort aufgewaermt = sitzung.hole(PFAD + abfrage);
        assertThat(aufgewaermt.status())
            .as("%s/%s/%s", fall.mandant(), fall.art(), paar.code())
            .isEqualTo(200);
        double[] endpunkt = new double[LAEUFE];
        Antwort letzte = aufgewaermt;
        for (int i = 0; i < LAEUFE; i++) {
          long start = System.nanoTime();
          letzte = sitzung.hole(PFAD + abfrage);
          endpunkt[i] = (System.nanoTime() - start) / 1_000_000.0;
          assertThat(letzte.status()).isEqualTo(200);
        }

        // ── … und am Dienst ─────────────────────────────────────────────────────────────────
        DashboardResponse amDienst = dashboardService.landingpage(kontext, paar);
        double[] dienst = new double[LAEUFE];
        for (int i = 0; i < LAEUFE; i++) {
          long start = System.nanoTime();
          amDienst = dashboardService.landingpage(kontext, paar);
          dienst[i] = (System.nanoTime() - start) / 1_000_000.0;
        }

        assertThat(letzte.<String>json("$.liveRest.zustand"))
            .as("%s/%s: ohne ANGEWANDT misst der Fall nichts", fall.mandant(), fall.art())
            .isEqualTo("ANGEWANDT");
        long nachrichten = ((Number) letzte.json("$.kacheln.nachrichten")).longValue();
        for (String sicht : List.of("partner", "richtung")) {
          List<Number> zeilen = letzte.json("$.verteilung." + sicht + ".zeilen[*].anzahl");
          assertThat(zeilen.stream().mapToLong(Number::longValue).sum())
              .as(
                  "%s/%s/%s: die Sicht %s zaehlt, was die Kachel zaehlt",
                  fall.mandant(), fall.art(), paar.code(), sicht)
              .isEqualTo(nachrichten);
        }
        assertThat(amDienst.kacheln().nachrichten()).isEqualTo(nachrichten);

        System.out.printf(
            Locale.ROOT,
            "M186 | %-7s | %-7s | %-4s | %s | %10d | %12d | %11d | %16.3f | %s | %14.3f | %s"
                + " | %13.3f | %s | %11.3f | %11d | %d%n",
            fall.mandant(),
            fall.art(),
            paar.code(),
            fall.g(),
            liveEimer,
            rollupzeilen,
            quellzeilen,
            beste(endpunkt),
            laeufe(endpunkt),
            beste(dienst),
            laeufe(dienst),
            beste(bezug),
            laeufe(bezug),
            beste(dienst) - beste(bezug),
            letzte.rumpf().getBytes(StandardCharsets.UTF_8).length,
            nachrichten);
      }
    }
  }

  private static double beste(double[] werte) {
    double beste = Double.MAX_VALUE;
    for (double wert : werte) {
      beste = Math.min(beste, wert);
    }
    return beste;
  }

  private static String laeufe(double[] werte) {
    StringBuilder text = new StringBuilder();
    for (double wert : werte) {
      if (!text.isEmpty()) {
        text.append('/');
      }
      text.append(String.format(Locale.ROOT, "%.1f", wert));
    }
    return text.toString();
  }
}
