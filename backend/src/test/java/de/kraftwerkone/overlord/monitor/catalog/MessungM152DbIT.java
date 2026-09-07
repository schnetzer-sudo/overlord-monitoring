package de.kraftwerkone.overlord.monitor.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.common.Baumfenster;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import de.kraftwerkone.overlord.monitor.security.Rolle;
import de.kraftwerkone.overlord.monitor.security.SicherheitsTestbasis;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <b>Messung M152: das freie Zeitfenster am gebauten Endpunkt</b> — dieselbe Form wie M117, mit
 * Laufzeit statt Groesse als ausgegebener Zahl.
 *
 * <h2>Warum am Endpunkt und nicht am Statement</h2>
 *
 * <p>M149 ist eine SQL-Zahl ueber ein handgebautes Statement. Was darin fehlt, entsteht erst hier:
 * die Zerlegung in Java, das gerenderte Statement (Regel L7 verlangt die gebaute Fassung), das
 * Zusammensetzen des Baums und die Serialisierung. Und was M149 nicht gemessen hat, kommt dazu: ein
 * monatsbuendiges Jahr (ein Monatssegment) und krumme 30 Tage (nur Stunden und Tage).
 *
 * <h2>Was gemessen wird, und was nur ausgegeben</h2>
 *
 * <p><b>Keine Laufzeit in einer Zusicherung</b> (Regel T1). Zugesichert wird ausschliesslich, dass
 * jede Antwort {@code 200} ist und in sich stimmig. Die Laufzeiten werden <b>ausgegeben</b> — beste
 * von fuenf nach einem Aufwaermlauf, in Millisekunden — und stehen mit ihrem Messdatum in {@code
 * docs/process-view.md} §37 ff.
 *
 * <p>Zwei Zahlen je Fall: <b>durch den Endpunkt</b> (HTTP, Sitzung, Dienst, Serialisierung) und
 * <b>am Dienst</b> ({@code ProzessbaumService.baum}, also Zerlegung, beide Statements und das
 * Zusammensetzen — ohne HTTP und ohne die Sitzung, die auf der Testkopie bei jeder Anfrage
 * fortgeschrieben wird). Die zweite ist die, die sich mit M116 und M149 vergleichen laesst.
 *
 * <h2>Der Boesfall ist einen Tag kuerzer als in M149</h2>
 *
 * <p>Das Fenster aus M149 umfasst 365 Tage und 13 Stunden und uebersteigt das Kalenderjahr, das der
 * Endpunkt zulaesst ({@code zeitfenster-zu-gross}). Gemessen wird deshalb {@code 2024-12-30 14:00}
 * bis einschliesslich {@code 2025-12-30 02:00} — dieselben fuenf Segmente ueber alle drei Ebenen,
 * das Tagessegment am Kopf einen Tag kuerzer.
 */
class MessungM152DbIT extends SicherheitsTestbasis {

  private static final String PFAD = "/api/prozesse/baum";

  /** Dieselben vier wie in M116 und M149. */
  private static final List<String> MANDANTEN = List.of("NEXANS", "VOTG", "IBIS", "SUTTONS");

  private static final String PASSWORT = "einLangesPasswort1";

  private static final int LAEUFE = 5;

  /** Ein Fall: Name, Abfrage — und die Parameter fuer den Dienst. */
  private record Fall(String name, String zeitraum, String von, String bis) {
    String abfrage() {
      if (zeitraum != null) {
        return "?zeitraum=" + zeitraum;
      }
      return "?von=" + von + "&bis=" + bis;
    }
  }

  /**
   * Die Faelle, alle in UTC, wie die API sie verlangt; die Zone der Anwendungsuhr rechnet beim
   * Empfang zurueck (im Winter eine Stunde). Alle enden auf oder vor dem Anker der Dev-Uhr.
   */
  private static final List<Fall> FAELLE =
      List.of(
          // Gegenprobe: ein Paar, unveraendert — Bezug ist M116.
          new Fall("48H (Paar)", "48H", null, null),
          new Fall("12M (Paar)", "12M", null, null),
          // Monatsbuendiges Jahr: 2025-01-01 00:00 bis einschliesslich 2025-12-31 23:00 —
          // ein Monatssegment, die ungeteilte Bauform mit anderen Werten.
          new Fall("Jahr monatsbuendig", null, "2024-12-31T23:00:00Z", "2025-12-31T22:00:00Z"),
          // Krumme 30 Tage: 2025-11-29 14:00 bis einschliesslich 2025-12-29 13:00 — Stunden,
          // Tage, Stunden; kein Monat.
          new Fall("30 Tage krumm", null, "2025-11-29T13:00:00Z", "2025-12-29T12:00:00Z"),
          // Der Boesfall am Endpunkt: 2024-12-30 14:00 bis einschliesslich 2025-12-30 02:00 —
          // fuenf Segmente ueber alle drei Ebenen.
          new Fall("Boesfall", null, "2024-12-30T13:00:00Z", "2025-12-30T01:00:00Z"));

  @Autowired private ProzessbaumService prozessbaumService;

  @Autowired private Clock anwendungsuhr;

  @Test
  @DisplayName("M152 — das freie Zeitfenster am gebauten Endpunkt, beste von fuenf")
  void freies_zeitfenster_am_endpunkt() throws IOException, InterruptedException {
    System.out.println(
        "M152 | mandant | fall               | segmente | endpunkt_best_ms | endpunkt_laeufe_ms"
            + " | dienst_best_ms | dienst_laeufe_ms | rumpf_bytes | nachrichten");

    for (String mandant : MANDANTEN) {
      String nutzer = PRAEFIX + "m152-" + mandant.toLowerCase(Locale.ROOT);
      legeNutzerAn(nutzer, PASSWORT, Rolle.MANDANT, mandant);
      Sitzung sitzung = anmelden(nutzer, PASSWORT);
      MandantContext kontext = new MandantContext(mandant);

      for (Fall fall : FAELLE) {
        Baumfenster fenster =
            Baumfenster.ausAnfrage(
                fall.zeitraum(), fall.von(), fall.bis(), anwendungsuhr.getZone());
        int segmente = fenster.segmente(LocalDateTime.now(anwendungsuhr)).size();

        // Aufwaermlauf, dann fuenf gemessene Laeufe — durch den Endpunkt.
        Antwort aufgewaermt = sitzung.hole(PFAD + fall.abfrage());
        assertThat(aufgewaermt.status()).as("%s/%s", mandant, fall.name()).isEqualTo(200);
        double[] endpunkt = new double[LAEUFE];
        Antwort letzte = aufgewaermt;
        for (int i = 0; i < LAEUFE; i++) {
          long start = System.nanoTime();
          letzte = sitzung.hole(PFAD + fall.abfrage());
          endpunkt[i] = (System.nanoTime() - start) / 1_000_000.0;
          assertThat(letzte.status()).as("%s/%s Lauf %d", mandant, fall.name(), i).isEqualTo(200);
        }

        // Dasselbe am Dienst — ohne HTTP, ohne Sitzung, ohne Jackson.
        prozessbaumService.baum(kontext, fenster);
        double[] dienst = new double[LAEUFE];
        for (int i = 0; i < LAEUFE; i++) {
          long start = System.nanoTime();
          prozessbaumService.baum(kontext, fenster);
          dienst[i] = (System.nanoTime() - start) / 1_000_000.0;
        }

        // Zugesichert: in sich stimmig, wie in M117.
        List<String> blaetter = letzte.json("$.partner[*].richtungen[*].prozesse[*].processId");
        int anzahlProzesse = ((Number) letzte.json("$.gesamt.anzahlProzesse")).intValue();
        long nachrichten = ((Number) letzte.json("$.gesamt.nachrichten")).longValue();
        assertThat(blaetter)
            .as("%s/%s: jedes Blatt genau einmal", mandant, fall.name())
            .hasSize(anzahlProzesse);
        assertThat(letzte.<String>json("$.zeitraum"))
            .isEqualTo(fall.zeitraum() == null ? "FREI" : fall.zeitraum());

        System.out.printf(
            Locale.ROOT,
            "M152 | %-7s | %-18s | %8d | %16.3f | %s | %14.3f | %s | %11d | %d%n",
            mandant,
            fall.name(),
            segmente,
            beste(endpunkt),
            laeufe(endpunkt),
            beste(dienst),
            laeufe(dienst),
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
