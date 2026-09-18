package de.kraftwerkone.overlord.monitor.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.common.FehlerLiveErgebnis;
import de.kraftwerkone.overlord.monitor.common.FehlerLiveRepository;
import de.kraftwerkone.overlord.monitor.common.FehlerLiveService;
import de.kraftwerkone.overlord.monitor.common.FehlerLiveZeile;
import de.kraftwerkone.overlord.monitor.common.LiveRestService;
import de.kraftwerkone.overlord.monitor.common.MandantContext;
import de.kraftwerkone.overlord.monitor.common.MessageStatusClassifier;
import de.kraftwerkone.overlord.monitor.common.Rollupzeitraum;
import de.kraftwerkone.overlord.monitor.common.Wasserstand;
import de.kraftwerkone.overlord.monitor.common.Zeitfenster;
import de.kraftwerkone.overlord.monitor.security.Rolle;
import de.kraftwerkone.overlord.monitor.security.SicherheitsTestbasis;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.convention.TestBean;

/**
 * <b>M188 — Fehler live</b> (Regel L7; {@code docs/fehler-live.md} §8, vorregistriert vor dem
 * ersten Lauf).
 *
 * <h2>Was hier gemessen wird</h2>
 *
 * <ul>
 *   <li><b>Tor 1, am Code:</b> die Lesung {@code FehlerLiveRepository.ausDerQuelle} — dieselben
 *       zwoelf Lagen wie in SQL ({@code scripts/messung-fehler-live/}), also mit jOOQ-Rendering,
 *       Verbindung und Zeilenabbildung.
 *   <li><b>Tor 2, am Repository wie M178:</b> die Verteilung ohne Fehler, beide Sichten, und in
 *       derselben Sitzung die Form ohne Zusatzbedingung daneben.
 *   <li><b>Tor 3:</b> die Seite durch den Endpunkt, am Dienst und als <b>Bezug</b> am Dienst mit
 *       ausgesetzter Lesung — die Seite, wie sie vor diesem Schritt war. Am Anker mit dem Live-Rest
 *       {@code NICHT_NOETIG}; <i>zusaetzlich zum Auftrag</i> die teuerste Lage aus M186 mit
 *       angewandtem Live-Rest.
 * </ul>
 *
 * <p>Je Lage ein Aufwaermlauf, dann die beste von fuenf. <b>Uhr und Wasserstand sind gestellt</b> —
 * ueber {@code @TestBean} an {@code devClock} und {@code Wasserstand}, wie in M186; kein
 * Schreibzugriff auf {@code rollup_lauf} oder {@code GlassfishDB} (S1, T2).
 *
 * <p><b>Dieser Laeufer ist eine Messung und kein Test</b> (Regel T1): Zeiten gehen nach {@code
 * System.out} und in keine Zusicherung. Zugesichert sind nur {@code 200}, die Zustaende und dass
 * Kachel und beide Sichten dieselbe Zahl tragen — sonst maesse er die Laufzeit einer falschen
 * Seite.
 */
class MessungM188DbIT extends SicherheitsTestbasis {

  /** Der Anker der Anwendungsuhr im Profil {@code dev} — derselbe Stichtag wie M146 und M178. */
  private static final LocalDateTime ANKER = LocalDateTime.parse("2025-12-30T04:09:47");

  private static final String PFAD = "/api/dashboard";

  private static final String PASSWORT = "einLangesPasswort1";

  private static final int LAEUFE = 5;

  private static final ZoneId ZONE = ZoneId.systemDefault();

  /**
   * Die vier Mandanten von Tor 1. {@code IBIS} ist der vierte, bestimmt per Zaehlung nach der
   * vorregistrierten Regel (Sitzung 0: keine einzige Fehlerzeile im ganzen Bestand, davon der mit
   * den meisten Nachrichten).
   */
  private static final List<String> TOR1_MANDANTEN = List.of("NEXANS", "SUTTONS", "VOTG", "IBIS");

  /** Die Mandanten von Tor 2 und Tor 3 — dieselben wie in M178. */
  private static final List<String> MANDANTEN = List.of("NEXANS", "SUTTONS");

  static final class Messuhr extends Clock {
    private volatile Instant jetzt = ANKER.atZone(ZONE).toInstant();

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

  @Autowired private FehlerLiveRepository fehlerLiveRepository;
  @Autowired private DashboardRepository dashboardRepository;
  @Autowired private DashboardService dashboardService;
  @Autowired private LiveRestService liveRestService;
  @Autowired private MessageStatusClassifier statusClassifier;
  @Autowired private DienstLeseRepository dienstLeseRepository;
  @Autowired private DienstStatusClassifier dienstClassifier;

  // ─── Tor 1 ────────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("M188 Tor 1 — die Lesung am Code, vier Mandanten je Paar, beste von fuenf")
  void tor1_die_lesung() {
    System.out.println(
        "M188-1 | mandant | paar | zeilen | fehler | aufwaermlauf_ms | beste_ms | laeufe_ms");
    for (String mandantId : TOR1_MANDANTEN) {
      MandantContext mandant = new MandantContext(mandantId);
      for (Rollupzeitraum paar : Rollupzeitraum.reihe()) {
        Zeitfenster fenster = paar.fenster(ANKER);
        Supplier<List<FehlerLiveZeile>> lesung =
            () -> fehlerLiveRepository.ausDerQuelle(mandant, fenster.von(), fenster.bis());

        long start = System.nanoTime();
        List<FehlerLiveZeile> zeilen = lesung.get();
        double aufwaermlauf = (System.nanoTime() - start) / 1_000_000.0;
        double[] laeufe = new double[LAEUFE];
        for (int i = 0; i < LAEUFE; i++) {
          start = System.nanoTime();
          zeilen = lesung.get();
          laeufe[i] = (System.nanoTime() - start) / 1_000_000.0;
        }

        System.out.printf(
            Locale.ROOT,
            "M188-1 | %-7s | %-4s | %6d | %6d | %15.3f | %8.3f | %s%n",
            mandantId,
            paar.code(),
            zeilen.size(),
            zeilen.stream().mapToLong(FehlerLiveZeile::anzahl).sum(),
            aufwaermlauf,
            beste(laeufe),
            laeufe(laeufe));
      }
    }
  }

  // ─── Tor 2 ────────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("M188 Tor 2 — die Verteilung ohne Fehler am Repository, daneben die heutige Form")
  void tor2_die_verteilung_ohne_fehler() {
    System.out.println(
        "M188-2 | mandant | paar | sicht    | heute_best_ms | heute_laeufe_ms | ohne_fehler_best_ms"
            + " | ohne_fehler_laeufe_ms | summe_heute | summe_ohne_fehler");
    for (String mandantId : MANDANTEN) {
      MandantContext mandant = new MandantContext(mandantId);
      for (Rollupzeitraum paar : Rollupzeitraum.reihe()) {
        Zeitfenster fenster = paar.fenster(ANKER);
        for (Verteilungssicht sicht :
            List.of(Verteilungssicht.RICHTUNG, Verteilungssicht.PARTNER)) {
          Supplier<List<Verteilungssumme>> heute =
              () -> dashboardRepository.verteilung(mandant, paar, fenster, sicht);
          Supplier<List<Verteilungssumme>> ohneFehler =
              () -> dashboardRepository.verteilungOhneFehler(mandant, paar, fenster, sicht);

          double[] heuteLaeufe = messe(heute);
          double[] ohneLaeufe = messe(ohneFehler);

          System.out.printf(
              Locale.ROOT,
              "M188-2 | %-7s | %-4s | %-8s | %13.3f | %s | %19.3f | %s | %11d | %17d%n",
              mandantId,
              paar.code(),
              sicht,
              beste(heuteLaeufe),
              laeufe(heuteLaeufe),
              beste(ohneLaeufe),
              laeufe(ohneLaeufe),
              summe(heute.get()),
              summe(ohneFehler.get()));
        }
      }
    }
  }

  // ─── Tor 3 ────────────────────────────────────────────────────────────────────

  /** Eine Lage von Tor 3: die Uhr, der Wasserstand und ob der Live-Rest dort angewandt ist. */
  private record Lage(
      String art, LocalDateTime jetzt, LocalDateTime wasserstand, String liveRest) {}

  @Test
  @DisplayName(
      "M188 Tor 3 — die Seite durch den Endpunkt, am Dienst und als Bezug, beste von fuenf")
  void tor3_die_seite() throws IOException, InterruptedException {
    List<Lage> lagen =
        List.of(
            new Lage("anker", ANKER, ANKER.plusDays(2), "NICHT_NOETIG"),
            new Lage(
                "dicht",
                LocalDateTime.parse("2024-10-09T21:00"),
                LocalDateTime.parse("2024-10-09T19:00"),
                "ANGEWANDT"));
    System.out.println(
        "M188-3 | mandant | lage  | paar | endpunkt_best_ms | endpunkt_laeufe_ms | dienst_best_ms"
            + " | dienst_laeufe_ms | bezug_best_ms | bezug_laeufe_ms | zuschlag_ms | nachrichten"
            + " | fehler | rumpf_bytes");
    DashboardService bezug = bezugsdienst();

    for (String mandantId : MANDANTEN) {
      String nutzer = PRAEFIX + "m188-" + mandantId.toLowerCase(Locale.ROOT);
      legeNutzerAn(nutzer, PASSWORT, Rolle.MANDANT, mandantId);
      Sitzung sitzung = anmelden(nutzer, PASSWORT);
      MandantContext mandant = new MandantContext(mandantId);

      for (Lage lage : lagen) {
        if (lage.art().equals("dicht") && !mandantId.equals("NEXANS")) {
          continue;
        }
        MESSUHR.stelle(lage.jetzt());
        WASSERSTAND.wert = lage.wasserstand();

        for (Rollupzeitraum paar : Rollupzeitraum.reihe()) {
          String abfrage = "?zeitraum=" + paar.code();

          // ── Der Bezug: dieselbe Seite am Dienst, die Lesung ausgesetzt ───────────────────
          DashboardResponse bezugAntwort = bezug.landingpage(mandant, paar);
          assertThat(bezugAntwort.fehlerLive().zustand().name()).isEqualTo("AUSGESETZT");
          double[] bezugLaeufe = messe(() -> bezug.landingpage(mandant, paar));

          // ── Durch den Endpunkt ───────────────────────────────────────────────────────────
          Antwort letzte = sitzung.hole(PFAD + abfrage);
          assertThat(letzte.status()).isEqualTo(200);
          double[] endpunkt = new double[LAEUFE];
          for (int i = 0; i < LAEUFE; i++) {
            long start = System.nanoTime();
            letzte = sitzung.hole(PFAD + abfrage);
            endpunkt[i] = (System.nanoTime() - start) / 1_000_000.0;
            assertThat(letzte.status()).isEqualTo(200);
          }

          // ── Am Dienst ────────────────────────────────────────────────────────────────────
          double[] dienst = messe(() -> dashboardService.landingpage(mandant, paar));
          DashboardResponse amDienst = dashboardService.landingpage(mandant, paar);

          String marke = mandantId + "/" + lage.art() + "/" + paar.code();
          assertThat(letzte.<String>json("$.fehlerLive.zustand")).as(marke).isEqualTo("ANGEWANDT");
          assertThat(letzte.<String>json("$.liveRest.zustand"))
              .as(marke)
              .isEqualTo(lage.liveRest());
          long nachrichten = ((Number) letzte.json("$.kacheln.nachrichten")).longValue();
          for (String sicht : List.of("partner", "richtung")) {
            List<Number> zeilen = letzte.json("$.verteilung." + sicht + ".zeilen[*].anzahl");
            assertThat(zeilen.stream().mapToLong(Number::longValue).sum())
                .as("%s: die Sicht %s zaehlt, was die Kachel zaehlt", marke, sicht)
                .isEqualTo(nachrichten);
          }
          assertThat(amDienst.kacheln().nachrichten()).isEqualTo(nachrichten);

          System.out.printf(
              Locale.ROOT,
              "M188-3 | %-7s | %-5s | %-4s | %16.3f | %s | %14.3f | %s | %13.3f | %s | %11.3f"
                  + " | %11d | %6d | %d%n",
              mandantId,
              lage.art(),
              paar.code(),
              beste(endpunkt),
              laeufe(endpunkt),
              beste(dienst),
              laeufe(dienst),
              beste(bezugLaeufe),
              laeufe(bezugLaeufe),
              beste(dienst) - beste(bezugLaeufe),
              nachrichten,
              amDienst.kacheln().fehler().anzahl(),
              letzte.rumpf().getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
        }
      }
    }
  }

  /**
   * Der Bezug: dieselbe Seite aus denselben Bausteinen — dieselbe Uhr, derselbe Live-Rest —, nur
   * die Fehlerlesung ausgesetzt. Das ist die Seite, wie sie vor diesem Schritt war.
   */
  private DashboardService bezugsdienst() {
    return new DashboardService(
        dashboardRepository,
        statusClassifier,
        anwendungsuhr,
        dienstLeseRepository,
        dienstClassifier,
        Optional.empty(),
        liveRestService,
        new FehlerLiveService(fehlerLiveRepository) {
          @Override
          public FehlerLiveErgebnis ermittle(
              MandantContext mandant, LocalDateTime von, LocalDateTime bis) {
            return FehlerLiveErgebnis.ausgesetzt();
          }
        });
  }

  /** Ein Aufwaermlauf, dann fuenf Laeufe in Millisekunden. */
  private static double[] messe(Supplier<?> aufruf) {
    aufruf.get();
    double[] laeufe = new double[LAEUFE];
    for (int i = 0; i < LAEUFE; i++) {
      long start = System.nanoTime();
      aufruf.get();
      laeufe[i] = (System.nanoTime() - start) / 1_000_000.0;
    }
    return laeufe;
  }

  private static long summe(List<Verteilungssumme> zeilen) {
    return zeilen.stream().mapToLong(Verteilungssumme::anzahl).sum();
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
