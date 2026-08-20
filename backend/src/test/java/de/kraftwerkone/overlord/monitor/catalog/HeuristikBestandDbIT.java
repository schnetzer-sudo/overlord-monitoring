package de.kraftwerkone.overlord.monitor.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.catalog.ProzessKatalogRepository.Bestandszeile;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import de.kraftwerkone.overlord.monitor.security.MandantRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * <b>Die Heuristik gegen den echten Bestand — und gegen die Zahlen, die sie treffen muss.</b>
 *
 * <p>{@code docs/prozess-katalog.md} §3.5 nennt fuer Regel A <b>887</b> Prozesse (NEXANS 509, VOTG
 * 378), fuer Regel B <b>281</b> (IBIS 192, IBISGUS 89) und <b>322</b> ohne Vorschlag,
 * aufgeschluesselt je Mandant. Dieser Test rechnet sie nach.
 *
 * <p><b>Warum er wichtiger ist als er aussieht.</b> Die Regeln waren an zwei Stellen mehrdeutig
 * formuliert — ob der Nummernpraefix Bedingung von Regel A ist, und ob „ohne Trennzeichen"
 * Bedingung oder Zweck von Regel B ist. Beide Lesarten sind an genau diesen Zahlen entschieden
 * worden. Ohne diesen Test waere die Entscheidung eine Ueberlegung; mit ihm ist sie eine Messung,
 * die bei jeder Regelaenderung erneut gefahren wird.
 *
 * <p><b>Er schreibt nichts.</b> Er liest den Prozessbestand je Mandant und laesst die reine
 * Funktion darueber laufen; {@code process_catalog} wird nicht angefasst.
 *
 * <p>Schlaegt er fehl, ist entweder eine Regel geaendert worden — oder die Testkopie ist neu
 * befuellt. Die Zahlen stammen aus der Erhebung vom <b>20.08.2026</b> (M74a, M75, M76). Sie sind
 * damit dieselbe Art Zusicherung wie in {@code BamSollaengeDriftDbIT}: an einen Datenstand gebunden
 * und deshalb hier benannt.
 */
@SpringBootTest
@ActiveProfiles("dev")
@Tag("db")
class HeuristikBestandDbIT {

  @Autowired private ProzessKatalogRepository repository;
  @Autowired private MandantRepository mandantRepository;

  /** Die Aufschluesselung aus {@code docs/prozess-katalog.md} §3.5, Stand 20.08.2026. */
  private static final Map<String, int[]> ERWARTET = erwartet();

  private static Map<String, int[]> erwartet() {
    // je Mandant: { Regel A, Regel B, ohne Vorschlag }
    Map<String, int[]> zahlen = new LinkedHashMap<>();
    zahlen.put("NEXANS", new int[] {509, 0, 224});
    zahlen.put("VOTG", new int[] {378, 0, 12});
    // GEMESSEN 20.08.2026, ABWEICHEND VON §3.5. Dort stehen 192 und 89 — also alle Prozesse
    // dieser beiden Mandanten. Gemessen tragen 5 bzw. 1 Prozess den Anker Eingehend/Ausgehend
    // ueberhaupt nicht (die mit Unterstrich, plus einer ohne). Keine Fassung der Regel kann sie
    // erreichen; §3.5 ist an dieser Stelle eine Projektion und keine Zaehlung.
    zahlen.put("IBIS", new int[] {0, 187, 5});
    zahlen.put("IBISGUS", new int[] {0, 88, 1});
    zahlen.put("SUTTONS", new int[] {0, 0, 17});
    zahlen.put("ZAST", new int[] {0, 0, 35});
    zahlen.put("NXHBE", new int[] {0, 0, 17});
    // GEMESSEN 20.08.2026, ABWEICHEND VON §3.5. Dort stehen alle 9 unter "ohne Vorschlag".
    // Regel B trifft hier sehr wohl: 5 der 9 Prozessnamen tragen den Anker in CamelCase. Die
    // Projektion hatte Regel B stillschweigend auf IBIS/IBISGUS beschraenkt — eine Regel gilt
    // aber, wo ihr Muster steht, und nicht, wo man sie gemeint hat.
    zahlen.put("EDITIONLINGERI", new int[] {0, 5, 4});
    zahlen.put("WOC", new int[] {0, 0, 4});
    zahlen.put("SYSTEM", new int[] {0, 0, 4});
    return zahlen;
  }

  private int[] zaehle(String mandantId) {
    List<Bestandszeile> bestand = repository.findeBestand(new MandantContext(mandantId));
    int[] zahlen = new int[3];
    for (Bestandszeile zeile : bestand) {
      Partnervorschlag vorschlag =
          Partnerheuristik.vorschlag(mandantId, zeile.processId(), zeile.projectId());
      switch (vorschlag.herkunft()) {
        case REGEL_A -> zahlen[0]++;
        case REGEL_B -> zahlen[1]++;
        case KEINE -> zahlen[2]++;
      }
    }
    return zahlen;
  }

  private static String alsZeile(String mandant, int[] zahlen) {
    return "%s A=%d B=%d keine=%d".formatted(mandant, zahlen[0], zahlen[1], zahlen[2]);
  }

  @Test
  @DisplayName("Je Mandant treffen Regel A, Regel B und „ohne Vorschlag\" die gemessenen Zahlen")
  void je_mandant_stimmen_die_zahlen() {
    // Bewusst ALLE Mandanten in einem Durchgang und eine einzige Zusicherung am Ende: Ein Abbruch
    // beim ersten Unterschied zeigt einen Mandanten und verschweigt die anderen neun — und beim
    // Bau dieser Regel war genau die vollstaendige Liste der Befund.
    List<String> erwartet = new ArrayList<>();
    List<String> gemessen = new ArrayList<>();
    ERWARTET.forEach(
        (mandant, zahlen) -> {
          erwartet.add(alsZeile(mandant, zahlen));
          gemessen.add(alsZeile(mandant, zaehle(mandant)));
        });

    assertThat(gemessen)
        .as(
            "Entweder ist eine Regel geaendert worden, oder die Testkopie ist neu befuellt."
                + " Die Zahlen stammen aus dem Lauf vom 20.08.2026; wo sie von der Projektion in"
                + " docs/prozess-katalog.md §3.5 abweichen, steht das dort im Nachtrag und in"
                + " messungen-schritt9.md M80.")
        .containsExactlyElementsOf(erwartet);
  }

  @Test
  @DisplayName("Die Summen: 887 aus Regel A, 281 aus Regel B, 322 ohne Vorschlag")
  void die_summen_stimmen() {
    int regelA = 0;
    int regelB = 0;
    int keine = 0;
    for (String mandant : mandantRepository.findeAlle().stream().map(m -> m.id()).toList()) {
      int[] zahlen = zaehle(mandant);
      regelA += zahlen[0];
      regelB += zahlen[1];
      keine += zahlen[2];
    }

    assertThat(regelA).as("Regel A ueber alle Mandanten — wie in §3.5 projiziert").isEqualTo(887);
    assertThat(regelB)
        .as(
            "Regel B ueber alle Mandanten. §3.5 projiziert 281; gemessen sind es 280 — sechs"
                + " Prozesse von IBIS/IBISGUS tragen den Anker gar nicht, dafuer trifft Regel B"
                + " bei EDITIONLINGERI fuenfmal, wo die Projektion sie nicht erwartet hat.")
        .isEqualTo(280);
    assertThat(keine)
        .as("Ohne Partnervorschlag. §3.5 projiziert 322; netto ist es eine Zeile mehr.")
        .isEqualTo(323);
    assertThat(regelA + regelB + keine)
        .as(
            "1.490 erreichbare Prozesse. Die uebrigen 13 der 1.503 haengen an sechs Projekten ohne"
                + " Mandantenzeile und erscheinen in keiner Pflegeliste (M74a) — dieselbe Lage wie"
                + " in Annahme A8, aber ueber Prozesse statt ueber Projekte gezaehlt.")
        .isEqualTo(1490);
    assertThat(regelA + regelB)
        .as(
            "Vorschlag insgesamt: 1.167 von 1.490 statt der projizierten 1.168. Die Projektion"
                + " trifft die Gesamtzahl auf EINS genau — aber aus zwei Fehlern, die einander"
                + " fast aufheben: sechs verlorene bei IBIS/IBISGUS gegen fuenf gewonnene bei"
                + " EDITIONLINGERI.")
        .isEqualTo(1167);
  }
}
