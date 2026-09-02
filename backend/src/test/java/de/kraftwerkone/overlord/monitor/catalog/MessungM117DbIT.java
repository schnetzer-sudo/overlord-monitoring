package de.kraftwerkone.overlord.monitor.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.security.Rolle;
import de.kraftwerkone.overlord.monitor.security.SicherheitsTestbasis;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * <b>Messung M117: die tatsaechliche Baumgroesse je Mandant, am gebauten Endpunkt.</b>
 *
 * <p>Sie ist <b>der eigentliche Ertrag dieser Runde fuer 10c-2</b>: Ein Baum mit vier Partnern und
 * einer mit hundertvierundfuenfzig sind zwei verschiedene Ansichten, und ohne die Zahl laesst sich
 * die Oberflaeche nicht entwerfen. M110 hat dieselben Zahlen ueber SQL erhoben; <b>diese Messung
 * nimmt sie am Endpunkt ab</b> — inklusive der Groesse des Antwortrumpfs, die nur hier entsteht.
 *
 * <h2>Warum sie ein Test ist und kein Skript</h2>
 *
 * <p>Der Rumpf entsteht aus dem gebauten Endpunkt samt Jackson-Serialisierung. Ein SQL-Skript
 * koennte die Knotenzahlen nachrechnen, aber nicht die Byte, die der Browser tatsaechlich bekommt —
 * und genau die entscheiden, ob 10c-2 nachladen muss.
 *
 * <h2>Was sie zusichert und was sie nur ausgibt</h2>
 *
 * <p><b>Zugesichert werden ausschliesslich pflegeunabhaengige Eigenschaften</b> (Regel T2): dass
 * die Zaehler der Antwort zu ihren eigenen Knoten passen. <b>Die Zahlen des Bestands werden
 * ausgegeben und nicht behauptet</b> — sie stehen mit ihrem Messdatum in {@code
 * docs/process-view.md} §8 und aendern sich mit jeder Kuratierung.
 *
 * <p><b>Keine Laufzeit in einer Zusicherung</b> (Regel T1). Die Laufzeiten der beiden Statements
 * stehen in M116, gemessen serverseitig ueber {@code SET profiling}.
 */
class MessungM117DbIT extends SicherheitsTestbasis {

  private static final String PFAD = "/api/prozesse/baum";

  /** Vier Groessenordnungen — dieselben wie in M112 bis M116. */
  private static final List<String> MANDANTEN = List.of("NEXANS", "VOTG", "IBIS", "SUTTONS");

  private static final String PASSWORT = "einLangesPasswort1";

  @Test
  @DisplayName("M117 — Baumgroesse und Rumpfgroesse je Mandant")
  void baumgroesse_je_mandant() throws IOException, InterruptedException {
    System.out.println(
        "M117 | mandant | prozesse | partner | ohne_partner | gruppen | ohne_richtung"
            + " | bewegt | still | nie | rumpf_bytes");

    for (String mandant : MANDANTEN) {
      String nutzer = PRAEFIX + "m117-" + mandant.toLowerCase(java.util.Locale.ROOT);
      legeNutzerAn(nutzer, PASSWORT, Rolle.MANDANT, mandant);
      Antwort antwort = anmelden(nutzer, PASSWORT).hole(PFAD);
      assertThat(antwort.status()).as("Mandant %s", mandant).isEqualTo(200);

      List<String> partner = antwort.json("$.partner[*].partner");
      List<String> richtungen = antwort.json("$.partner[*].richtungen[*].richtung");
      List<String> blaetter = antwort.json("$.partner[*].richtungen[*].prozesse[*].processId");
      int anzahlProzesse = ((Number) antwort.json("$.gesamt.anzahlProzesse")).intValue();
      int bewegt = ((Number) antwort.json("$.gesamt.bewegt")).intValue();
      int still = ((Number) antwort.json("$.gesamt.still")).intValue();
      int nie = ((Number) antwort.json("$.gesamt.nie")).intValue();

      // Zugesichert: Die Antwort ist in sich stimmig. Das haengt an keiner Kuratierung.
      assertThat(blaetter)
          .as("Mandant %s: jedes Blatt genau einmal", mandant)
          .hasSize(anzahlProzesse);
      assertThat(bewegt + still + nie)
          .as("Mandant %s: die drei Zustaende sind disjunkt und vollstaendig", mandant)
          .isEqualTo(anzahlProzesse);

      // Ausgegeben, nicht behauptet: die Zahlen des Bestands.
      System.out.printf(
          "M117 | %-8s | %8d | %7d | %12d | %7d | %13d | %6d | %5d | %3d | %11d%n",
          mandant,
          anzahlProzesse,
          partner.stream().filter(wert -> wert != null).count(),
          partner.stream().filter(wert -> wert == null).count(),
          richtungen.size(),
          richtungen.stream().filter(wert -> wert == null).count(),
          bewegt,
          still,
          nie,
          antwort.rumpf().getBytes(StandardCharsets.UTF_8).length);
    }
  }
}
