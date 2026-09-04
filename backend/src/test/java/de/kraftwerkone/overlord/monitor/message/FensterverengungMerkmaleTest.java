package de.kraftwerkone.overlord.monitor.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.verifyNoInteractions;

import de.kraftwerkone.overlord.monitor.common.MessageStatusKind;
import de.kraftwerkone.overlord.monitor.common.Seitenposition;
import de.kraftwerkone.overlord.monitor.common.Zeitfenster;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.lang.reflect.RecordComponent;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Der Filterschutz der {@link Fensterverengung} — <b>der wichtigste Test dieses Baus</b>.
 *
 * <h2>Wogegen er schuetzt</h2>
 *
 * <p>Gemessen am 27.08.2026 (offener Punkt 72 in {@code docs/messungen-liste-verengung.md}): Eine
 * Verengung, die einen gesetzten Filter nicht mitrechnet, verliert Zeilen — <b>lautlos</b>. Mit
 * {@code status=FEHLER} lieferte sie fuer {@code SUTTONS} null statt fuenf Zeilen. In einem
 * Ueberwachungswerkzeug ist „keine Fehler" die schlimmste falsche Antwort, die es gibt.
 *
 * <h2>Der Riegel, den nur dieser Test halten kann</h2>
 *
 * <p>{@link Fensterverengung} entscheidet ueber {@link Abfragemerkmal} in zwei vollstaendigen
 * {@code switch}-Ausdruecken ohne {@code default}. Ein neuer <b>Enum-Wert</b> bricht damit den Bau.
 * Was der Compiler <b>nicht</b> merkt: dass jemand dem Record {@link Nachrichtenabfrage} ein neues
 * <b>Feld</b> gibt und vergisst, dafuer ein Merkmal anzulegen. Dann liefe die Verengung weiter, als
 * gaebe es das Feld nicht — und genau so verliert man Zeilen.
 *
 * <p>{@link #jedes_feld_der_abfrage_hat_ein_merkmal()} schliesst diese Luecke: Er geht ueber {@code
 * Nachrichtenabfrage.class.getRecordComponents()} und verlangt fuer jeden Bestandteil einen
 * ausdruecklich zugeordneten Wert. <b>Wer ein Feld ergaenzt, muss hier eine Zeile ergaenzen</b> —
 * und dabei entscheiden, ob der Rollup es mittraegt. Ein Kommentar taete das nicht.
 */
@ExtendWith(MockitoExtension.class)
class FensterverengungMerkmaleTest {

  private static final MandantContext MANDANT = new MandantContext("NEXANS");
  private static final LocalDateTime JETZT = LocalDateTime.parse("2025-12-30T04:09:47");
  private static final Zeitfenster FENSTER =
      new Zeitfenster(LocalDateTime.parse("2025-11-30T04:09:47"), JETZT);

  /**
   * Die ausdrueckliche Zuordnung Feld → Merkmal. <b>Sie ist der Kern des Tests</b>: Jede Zeile ist
   * eine Entscheidung, die jemand getroffen hat, und ein fehlender Eintrag laesst den Test fallen.
   */
  private static final Map<String, Abfragemerkmal> ZUORDNUNG =
      Map.of(
          "fenster", Abfragemerkmal.ZEITFENSTER,
          "status", Abfragemerkmal.STATUS,
          "prozessIds", Abfragemerkmal.PROZESSE,
          "suchtreffer", Abfragemerkmal.SUCHBEGRIFF,
          "absteigend", Abfragemerkmal.SORTIERUNG,
          "cursor", Abfragemerkmal.CURSOR,
          "limit", Abfragemerkmal.LIMIT);

  /**
   * Die Attrappe wird in den Rueckfallfaellen <b>gar nicht angefasst</b> — und das wird geprueft.
   * Ein Rueckfall, der die Vorabfrage trotzdem stellt und ihr Ergebnis verwirft, kostete Laufzeit
   * fuer nichts.
   */
  @Mock private VerengungRepository repository;

  private Fensterverengung verengung(boolean schalter) {
    return new Fensterverengung(repository, new NachrichtenlisteEigenschaften(schalter));
  }

  private static Nachrichtenabfrage abfrage(
      Set<MessageStatusKind> status,
      List<String> prozessIds,
      Suchtreffer suchtreffer,
      boolean absteigend,
      Seitenposition cursor) {
    return new Nachrichtenabfrage(FENSTER, status, prozessIds, suchtreffer, absteigend, cursor, 50);
  }

  private static Nachrichtenabfrage schlicht() {
    return abfrage(Set.of(), List.of(), null, true, null);
  }

  // ── Der Riegel gegen ein neues Feld ───────────────────────────────────────

  @Test
  @DisplayName("Jedes Feld von Nachrichtenabfrage hat ein ausdruecklich zugeordnetes Merkmal")
  void jedes_feld_der_abfrage_hat_ein_merkmal() {
    Map<Abfragemerkmal, String> rueckwaerts = new HashMap<>();
    for (RecordComponent bestandteil : Nachrichtenabfrage.class.getRecordComponents()) {
      Abfragemerkmal merkmal = ZUORDNUNG.get(bestandteil.getName());
      if (merkmal == null) {
        fail(
            """
            Das Feld "%s" von Nachrichtenabfrage hat kein Abfragemerkmal.

            Wer der Liste ein Feld hinzufuegt, fuegt zwei Dinge hinzu: das Feld und die
            Entscheidung, ob die Fensterverengung es mittragen kann. Fehlt die Entscheidung,
            verengt sie weiter, als gaebe es das Feld nicht -- und verliert Zeilen, ohne dass
            jemand einen Fehler sieht (offener Punkt 72).

            Zu tun: einen Wert in Abfragemerkmal ergaenzen, ihn hier zuordnen und in
            Fensterverengung.gesetzt(...) und .traegtDerRollup(...) beantworten. Die sichere
            Antwort auf die zweite Frage ist false.
            """
                .formatted(bestandteil.getName()));
      }
      String schonBelegt = rueckwaerts.put(merkmal, bestandteil.getName());
      assertThat(schonBelegt).as("Merkmal %s ist zwei Feldern zugeordnet", merkmal).isNull();
    }
    assertThat(ZUORDNUNG.values())
        .as("Jedes Abfragemerkmal gehoert zu genau einem Feld der Abfrage")
        .containsExactlyInAnyOrder(Abfragemerkmal.values());
  }

  // ── Was der Rollup nicht traegt, faellt zurueck ───────────────────────────

  /*
   * Hier stand bis zum 03.09.2026 der Fall `ueberfaellig: die Verengung wird gar nicht erst
   * betreten`. Er ist mit E-71 entfallen -- zusammen mit dem Merkmal selbst.
   *
   * WAS DAMIT VERLORENGEHT, GEHOERT BENANNT: `UEBERFAELLIG` war das einzige Merkmal, das der
   * Rollup aus einem Grund nicht tragen konnte, der am BESTAND lag (MessageTimeout steht dort
   * nicht). Uebrig bleibt `SUCHBEGRIFF`, und dessen Grund liegt am SCHEMA (message_rollup hat
   * keine sos_id). Der Riegel prueft damit weiterhin beide Richtungen -- tragbar und nicht tragbar
   * --, aber nur noch an einer Sorte von Grund.
   */

  @Test
  @DisplayName("Suchbegriff: der Rollup hat keine sos_id, also faellt sie zurueck")
  void suchbegriff_faellt_zurueck() {
    Suchtreffer treffer = new Suchtreffer(List.of("p-1"), List.of("s-1"), false);

    Fensterverengung.Ergebnis ergebnis =
        verengung(true).verenge(MANDANT, abfrage(Set.of(), List.of(), treffer, true, null));

    assertThat(ergebnis.grund()).isEqualTo(Verengungsgrund.MERKMAL_NICHT_TRAGBAR);
    assertThat(ergebnis.abfrage().fenster()).isEqualTo(FENSTER);
    verifyNoInteractions(repository);
  }

  @Test
  @DisplayName("Sortierung AELTESTE: eine Untergrenze verschoebe dort die erste Seite")
  void aelteste_faellt_zurueck() {
    Fensterverengung.Ergebnis ergebnis =
        verengung(true).verenge(MANDANT, abfrage(Set.of(), List.of(), null, false, null));

    assertThat(ergebnis.grund()).isEqualTo(Verengungsgrund.MERKMAL_NICHT_TRAGBAR);
    assertThat(ergebnis.abfrage().fenster()).isEqualTo(FENSTER);
    verifyNoInteractions(repository);
  }

  @Test
  @DisplayName("Der Schalter aus: kein Blick in den Rollup, die Abfrage kommt unveraendert zurueck")
  void schalter_aus() {
    Nachrichtenabfrage hinein = schlicht();

    Fensterverengung.Ergebnis ergebnis = verengung(false).verenge(MANDANT, hinein);

    assertThat(ergebnis.grund()).isEqualTo(Verengungsgrund.ABGESCHALTET);
    assertThat(ergebnis.abfrage()).isSameAs(hinein);
    assertThat(ergebnis.sicherLeer()).isFalse();
    verifyNoInteractions(repository);
  }

  // ── Was er traegt, kommt bis zur Vorabfrage ───────────────────────────────

  @Test
  @DisplayName("Status, Prozessfilter und Cursor halten die Verengung nicht auf")
  void tragbare_merkmale_gehen_bis_zur_vorabfrage() {
    // Der Wasserstand bleibt null -- die Verengung bricht danach ab. Entscheidend ist, dass der Weg
    // ueberhaupt bis zum Repository geht: Bei einem nicht tragbaren Merkmal waere schon vorher
    // Schluss gewesen, und niemand haette den Wasserstand gelesen.
    Fensterverengung.Ergebnis ergebnis =
        verengung(true)
            .verenge(
                MANDANT,
                abfrage(
                    Set.of(MessageStatusKind.FEHLER),
                    List.of("p-1"),
                    null,
                    true,
                    new Seitenposition(JETZT.minusHours(2), "m-1")));

    assertThat(ergebnis.grund()).isEqualTo(Verengungsgrund.KEINE_UNTERGRENZE);
  }
}
