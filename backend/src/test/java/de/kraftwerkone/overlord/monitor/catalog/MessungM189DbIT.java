package de.kraftwerkone.overlord.monitor.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.common.Baumfenster;
import de.kraftwerkone.overlord.monitor.common.Baumgliederung;
import de.kraftwerkone.overlord.monitor.common.FehlerLiveErgebnis;
import de.kraftwerkone.overlord.monitor.common.FehlerLiveRepository;
import de.kraftwerkone.overlord.monitor.common.FehlerLiveService;
import de.kraftwerkone.overlord.monitor.common.FehlerLiveZustand;
import de.kraftwerkone.overlord.monitor.common.LiveRestService;
import de.kraftwerkone.overlord.monitor.common.MandantContext;
import de.kraftwerkone.overlord.monitor.common.MessageStatusClassifier;
import de.kraftwerkone.overlord.monitor.common.Wasserstand;
import de.kraftwerkone.overlord.monitor.security.Rolle;
import de.kraftwerkone.overlord.monitor.security.SicherheitsTestbasis;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
 * <b>M189 — Fehler live im Prozessbaum</b> (Regel L7; {@code docs/fehler-live.md} §5b,
 * vorregistriert vor dem ersten Lauf). Tor 1 — die Lesung in SQL — steht in {@code
 * scripts/messung-fehler-live/}; hier stehen die Tore am Endpunkt.
 *
 * <h2>Was hier gemessen wird</h2>
 *
 * <ul>
 *   <li><b>Tor 2:</b> die typische Stunde aus M185, {@code NEXANS} und {@code SUTTONS}, je {@code
 *       48H} und {@code 12M} — Live-Rest {@code ANGEWANDT} ueber zwei Eimer.
 *   <li><b>Tor 3:</b> der dichteste Vierstundenbereich aus M185, dieselben Lagen — {@code
 *       ANGEWANDT} ueber vier Eimer.
 *   <li><b>Tor 4:</b> {@code FREI}, das monatsbuendige Jahr und der Boesfall aus M152, am Anker mit
 *       Live-Rest {@code ANGEWANDT} ueber zwei Eimer.
 * </ul>
 *
 * <p>Je Lage <b>durch den Endpunkt</b>, <b>am Dienst</b> und als <b>Bezug in derselben Sitzung</b>:
 * derselbe Baum am Dienst mit ausgesetzter Lesung — der Baum, wie er vor Teil B war. Je Messung ein
 * Aufwaermlauf, dann die beste von fuenf. <b>Uhr und Wasserstand sind gestellt</b> — ueber
 * {@code @TestBean} an {@code devClock} und {@code Wasserstand}, wie in M185; kein Schreibzugriff
 * auf {@code rollup_lauf} oder {@code GlassfishDB} (S1, T2).
 *
 * <p><b>Dieser Laeufer ist eine Messung und kein Test</b> (Regel T1): Zeiten gehen nach {@code
 * System.out} und in keine Zusicherung. Zugesichert sind nur {@code 200}, die Zustaende, dieselbe
 * Kopfzahl an Endpunkt und Dienst und jedes Blatt genau einmal — sonst maesse er die Laufzeit eines
 * falschen Baums.
 */
class MessungM189DbIT extends SicherheitsTestbasis {

  /** Der Anker der Anwendungsuhr im Profil {@code dev}. */
  private static final LocalDateTime ANKER = LocalDateTime.parse("2025-12-30T04:09:47");

  private static final String PFAD = "/api/prozesse/baum";

  private static final String PASSWORT = "einLangesPasswort1";

  private static final int LAEUFE = 5;

  /** Die Zone der Anwendungsuhr im Profil {@code dev}: die Systemzone, wie {@code ZeitConfig}. */
  private static final ZoneId ZONE = ZoneId.systemDefault();

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

  @Autowired private ProzessbaumService prozessbaumService;
  @Autowired private ProzessbaumRepository prozessbaumRepository;
  @Autowired private LiveRestService liveRestService;
  @Autowired private FehlerLiveRepository fehlerLiveRepository;
  @Autowired private MessageStatusClassifier statusClassifier;

  /**
   * Eine Lage: das Tor, die Art, die Uhr, der Wasserstand und die Abfrage — {@code zeitraum} fuer
   * ein Paar, {@code von}/{@code bis} in UTC fuer ein freies Fenster.
   */
  private record Lage(
      String tor,
      String mandant,
      String art,
      LocalDateTime jetzt,
      LocalDateTime wasserstand,
      String name,
      String zeitraum,
      String von,
      String bis) {

    String abfrage() {
      String fenster = zeitraum != null ? "zeitraum=" + zeitraum : "von=" + von + "&bis=" + bis;
      return "?" + fenster + "&gliederung=" + Baumgliederung.PARTNER.name();
    }
  }

  /**
   * Die Lagen der Tore 2 bis 4 fuer einen Mandanten — die Stunden aus M185, die Fenster aus M152.
   */
  private static List<Lage> lagen(String mandant) {
    LocalDateTime typischG =
        mandant.equals("NEXANS")
            ? LocalDateTime.parse("2025-11-09T15:00")
            : LocalDateTime.parse("2025-06-09T19:00");
    LocalDateTime dichtG =
        mandant.equals("NEXANS")
            ? LocalDateTime.parse("2024-10-09T18:00")
            : LocalDateTime.parse("2025-06-12T07:00");
    LocalDateTime typischJetzt = typischG.plusMinutes(90);
    LocalDateTime dichtJetzt = dichtG.plusMinutes(180);
    LocalDateTime ankerW = LocalDateTime.parse("2025-12-30T04:00");
    return List.of(
        new Lage(
            "2", mandant, "typisch", typischJetzt, typischG.plusHours(1), "48H", "48H", null, null),
        new Lage(
            "2", mandant, "typisch", typischJetzt, typischG.plusHours(1), "12M", "12M", null, null),
        new Lage("3", mandant, "dicht", dichtJetzt, dichtG.plusHours(1), "48H", "48H", null, null),
        new Lage("3", mandant, "dicht", dichtJetzt, dichtG.plusHours(1), "12M", "12M", null, null),
        new Lage(
            "4",
            mandant,
            "frei",
            ANKER,
            ankerW,
            "jahr",
            null,
            "2024-12-31T23:00:00Z",
            "2025-12-31T22:00:00Z"),
        new Lage(
            "4",
            mandant,
            "frei",
            ANKER,
            ankerW,
            "boesfall",
            null,
            "2024-12-30T13:00:00Z",
            "2025-12-30T01:00:00Z"));
  }

  @Test
  @DisplayName("M189 Tore 2 bis 4 — der Baum durch den Endpunkt, am Dienst und als Bezug")
  void tore_2_bis_4() throws IOException, InterruptedException {
    assertThat(anwendungsuhr).as("Die Anwendungsuhr ist die gestellte").isSameAs(MESSUHR);
    assertThat(wasserstand).as("Der Wasserstand ist der gesetzte").isSameAs(WASSERSTAND);
    System.out.println(
        "M189 | tor | mandant | art     | lage     | fenster_von      | fenster_bis      |"
            + " endpunkt_best_ms | endpunkt_laeufe_ms | dienst_best_ms | dienst_laeufe_ms |"
            + " bezug_best_ms | bezug_laeufe_ms | zuschlag_ms | nachrichten | fehler | rumpf_bytes");
    ProzessbaumService bezug = bezugsdienst();

    for (String mandantId : MANDANTEN) {
      String nutzer = PRAEFIX + "m189-" + mandantId.toLowerCase(Locale.ROOT);
      legeNutzerAn(nutzer, PASSWORT, Rolle.MANDANT, mandantId);
      Sitzung sitzung = anmelden(nutzer, PASSWORT);
      MandantContext mandant = new MandantContext(mandantId);

      for (Lage lage : lagen(mandantId)) {
        MESSUHR.stelle(lage.jetzt());
        WASSERSTAND.wert = lage.wasserstand();
        Baumfenster fenster = Baumfenster.ausAnfrage(lage.zeitraum(), lage.von(), lage.bis(), ZONE);
        String marke = mandantId + "/" + lage.art() + "/" + lage.name();

        // ── Der Bezug: derselbe Baum am Dienst, die Lesung ausgesetzt ─────────────────────
        ProzessbaumResponse bezugAntwort = bezug.baum(mandant, fenster, Baumgliederung.PARTNER);
        assertThat(bezugAntwort.fehlerLive().zustand())
            .as(marke)
            .isEqualTo(FehlerLiveZustand.AUSGESETZT);
        double[] bezugLaeufe = messe(() -> bezug.baum(mandant, fenster, Baumgliederung.PARTNER));

        // ── Durch den Endpunkt ─────────────────────────────────────────────────────────────
        Antwort letzte = sitzung.hole(PFAD + lage.abfrage());
        assertThat(letzte.status()).as(marke).isEqualTo(200);
        double[] endpunkt = new double[LAEUFE];
        for (int i = 0; i < LAEUFE; i++) {
          long start = System.nanoTime();
          letzte = sitzung.hole(PFAD + lage.abfrage());
          endpunkt[i] = (System.nanoTime() - start) / 1_000_000.0;
          assertThat(letzte.status()).as(marke).isEqualTo(200);
        }

        // ── Am Dienst ──────────────────────────────────────────────────────────────────────
        double[] dienst =
            messe(() -> prozessbaumService.baum(mandant, fenster, Baumgliederung.PARTNER));
        ProzessbaumResponse amDienst =
            prozessbaumService.baum(mandant, fenster, Baumgliederung.PARTNER);

        assertThat(letzte.<String>json("$.fehlerLive.zustand")).as(marke).isEqualTo("ANGEWANDT");
        assertThat(letzte.<String>json("$.liveRest.zustand")).as(marke).isEqualTo("ANGEWANDT");
        assertThat(amDienst.fehlerLive().zustand()).isEqualTo(FehlerLiveZustand.ANGEWANDT);
        long nachrichten = ((Number) letzte.json("$.gesamt.nachrichten")).longValue();
        long fehler = ((Number) letzte.json("$.gesamt.fehler")).longValue();
        assertThat(amDienst.gesamt().nachrichten()).as(marke).isEqualTo(nachrichten);
        assertThat(amDienst.gesamt().fehler()).as(marke).isEqualTo(fehler);
        List<String> blaetter = letzte.json("$..processId");
        int anzahlProzesse = ((Number) letzte.json("$.gesamt.anzahlProzesse")).intValue();
        assertThat(blaetter).as("%s: jedes Blatt genau einmal", marke).hasSize(anzahlProzesse);

        System.out.printf(
            Locale.ROOT,
            "M189 | %s   | %-7s | %-7s | %-8s | %s | %s | %16.3f | %s | %14.3f | %s | %13.3f | %s |"
                + " %11.3f | %11d | %6d | %d%n",
            lage.tor(),
            mandantId,
            lage.art(),
            lage.name(),
            fenster.fenster(lage.jetzt()).von(),
            fenster.fenster(lage.jetzt()).bis(),
            beste(endpunkt),
            laeufe(endpunkt),
            beste(dienst),
            laeufe(dienst),
            beste(bezugLaeufe),
            laeufe(bezugLaeufe),
            beste(dienst) - beste(bezugLaeufe),
            nachrichten,
            fehler,
            letzte.rumpf().getBytes(StandardCharsets.UTF_8).length);
      }
    }
  }

  /**
   * Der Bezug: derselbe Baum aus denselben Bausteinen — dieselbe Uhr, derselbe Live-Rest —, nur die
   * Fehlerlesung ausgesetzt, ohne zu lesen. Das ist der Baum, wie er vor Teil B war.
   */
  private ProzessbaumService bezugsdienst() {
    return new ProzessbaumService(
        prozessbaumRepository,
        statusClassifier,
        anwendungsuhr,
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
