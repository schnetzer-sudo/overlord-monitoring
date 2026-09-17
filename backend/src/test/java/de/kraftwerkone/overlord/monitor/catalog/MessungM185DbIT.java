package de.kraftwerkone.overlord.monitor.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.common.Baumfenster;
import de.kraftwerkone.overlord.monitor.common.Baumgliederung;
import de.kraftwerkone.overlord.monitor.common.LiveRestRepository;
import de.kraftwerkone.overlord.monitor.common.MandantContext;
import de.kraftwerkone.overlord.monitor.common.Rollupzeitraum;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.convention.TestBean;

/**
 * <b>Messung M185: der Live-Rest am gebauten Endpunkt</b> — dieselbe Form wie M152: vier Mandanten,
 * je Fall ein Aufwaermlauf und die beste von fuenf, strikt nacheinander, zwei Zahlen (durch den
 * Endpunkt und am Dienst).
 *
 * <h2>Uhr und Wasserstand sind gesetzt</h2>
 *
 * <p>Damit historische Stunden aus dem dichten Bestand als laufende Stunde dienen, ersetzt dieser
 * Test die Anwendungsuhr ({@code devClock}) und den {@code Wasserstand} im Anwendungskontext —
 * ueber {@link TestBean}, ohne in {@code rollup_lauf} zu schreiben. Je Fall stehen beide auf den
 * Werten, die {@code docs/live-rest.md} §8 vorab bestimmt hat:
 *
 * <ul>
 *   <li><b>typische Stunde</b> — der Median der Stunden mit Verkehr je Mandant, als G; {@code
 *       jetzt} = G + 1 h 30, also ein Live-Bereich von zwei Eimern
 *   <li><b>dichtester Bereich</b> — der dichteste zusammenhaengende Bereich von vier Stundeneimern,
 *       als G; {@code jetzt} = G + 3 h, der groesste Live-Bereich, den die Obergrenze zulaesst
 * </ul>
 *
 * <h2>Vorregistriert, bevor gemessen wird</h2>
 *
 * <ol>
 *   <li>Typische Stunde: {@code 48H} und {@code 12M} durch den Endpunkt unter <b>150 ms</b> (M152).
 *   <li>Dichtester Bereich: {@code 48H} und {@code 12M} durch den Endpunkt unter <b>500 ms</b> (§8
 *       der Projektbeschreibung).
 * </ol>
 *
 * <p><b>Keine Laufzeit in einer Zusicherung</b> (Regel T1). Zugesichert wird, dass jede Antwort
 * {@code 200} ist, den Zustand {@code ANGEWANDT} traegt (sonst misst der Fall nichts) und in sich
 * stimmig ist. Die Laufzeiten werden <b>ausgegeben</b>; ob die Schranken halten, steht im Bericht.
 */
class MessungM185DbIT extends SicherheitsTestbasis {

  private static final String PFAD = "/api/prozesse/baum";

  private static final String PASSWORT = "einLangesPasswort1";

  private static final int LAEUFE = 5;

  /** Die Zone der Anwendungsuhr im Profil {@code dev}: die Systemzone, wie {@code ZeitConfig}. */
  private static final ZoneId ZONE = ZoneId.systemDefault();

  /** Eine Uhr, die der Test stellt — die Zeit steht, wie bei {@code Clock.fixed}. */
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

  /** Ein Wasserstand, den der Test setzt — nichts wird in {@code rollup_lauf} geschrieben. */
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

  /**
   * Ein Fall: Mandant, Art, G und der Abstand von G zu jetzt. Die Stunden sind die aus {@code
   * docs/live-rest.md} §8, dort mit ihren Nachrichtenzahlen und der Herleitung.
   */
  private record Fall(String mandant, String art, LocalDateTime g, int minutenNachG) {
    LocalDateTime jetzt() {
      return g.plusMinutes(minutenNachG);
    }
  }

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

  private static final List<Rollupzeitraum> PAARE =
      List.of(Rollupzeitraum.STUNDEN_48, Rollupzeitraum.MONATE_12);

  @Autowired private ProzessbaumService prozessbaumService;
  @Autowired private LiveRestRepository liveRestRepository;

  @Test
  @DisplayName("M185 — der Live-Rest am gebauten Endpunkt, beste von fuenf")
  void live_rest_am_endpunkt() throws IOException, InterruptedException {
    assertThat(anwendungsuhr).as("Die Anwendungsuhr ist die gestellte").isSameAs(MESSUHR);
    assertThat(wasserstand).as("Der Wasserstand ist der gesetzte").isSameAs(WASSERSTAND);
    System.out.println(
        "M185 | mandant | art     | paar | g                | live_eimer | rollupzeilen | quellzeilen"
            + " | endpunkt_best_ms | endpunkt_laeufe_ms | dienst_best_ms | dienst_laeufe_ms"
            + " | rumpf_bytes | nachrichten");

    for (Fall fall : FAELLE) {
      String nutzer =
          PRAEFIX + "m185-" + fall.mandant().toLowerCase(Locale.ROOT) + "-" + fall.art();
      legeNutzerAn(nutzer, PASSWORT, Rolle.MANDANT, fall.mandant());
      Sitzung sitzung = anmelden(nutzer, PASSWORT);
      MandantContext kontext = new MandantContext(fall.mandant());

      MESSUHR.stelle(fall.jetzt());
      WASSERSTAND.wert = fall.g().plusHours(1);
      LocalDateTime liveBis =
          fall.jetzt().truncatedTo(java.time.temporal.ChronoUnit.HOURS).plusHours(1);
      int liveEimer = (int) java.time.Duration.between(fall.g(), liveBis).toHours();
      int rollupzeilen = liveRestRepository.ausDemRollup(kontext, fall.g(), liveBis).size();
      int quellzeilen = liveRestRepository.ausDerQuelle(kontext, fall.g(), liveBis).size();

      for (Rollupzeitraum paar : PAARE) {
        String abfrage = "?zeitraum=" + paar.code();
        Baumfenster fenster = Baumfenster.paar(paar);

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

        prozessbaumService.baum(kontext, fenster, Baumgliederung.PARTNER);
        double[] dienst = new double[LAEUFE];
        for (int i = 0; i < LAEUFE; i++) {
          long start = System.nanoTime();
          prozessbaumService.baum(kontext, fenster, Baumgliederung.PARTNER);
          dienst[i] = (System.nanoTime() - start) / 1_000_000.0;
        }

        assertThat(letzte.<String>json("$.liveRest.zustand"))
            .as("%s/%s: ohne ANGEWANDT misst der Fall nichts", fall.mandant(), fall.art())
            .isEqualTo("ANGEWANDT");
        List<String> blaetter = letzte.json("$..processId");
        int anzahlProzesse = ((Number) letzte.json("$.gesamt.anzahlProzesse")).intValue();
        assertThat(blaetter).as("jedes Blatt genau einmal").hasSize(anzahlProzesse);

        System.out.printf(
            Locale.ROOT,
            "M185 | %-7s | %-7s | %-4s | %s | %10d | %12d | %11d | %16.3f | %s | %14.3f | %s | %11d | %d%n",
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
            letzte.rumpf().getBytes(StandardCharsets.UTF_8).length,
            ((Number) letzte.json("$.gesamt.nachrichten")).longValue());
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
