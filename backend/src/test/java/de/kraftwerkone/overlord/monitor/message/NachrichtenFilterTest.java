package de.kraftwerkone.overlord.monitor.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.kraftwerkone.overlord.monitor.common.MessageStatusClassifier;
import de.kraftwerkone.overlord.monitor.common.MessageStatusKind;
import de.kraftwerkone.overlord.monitor.common.Seitenposition;
import de.kraftwerkone.overlord.monitor.common.Sortierrichtung;
import de.kraftwerkone.overlord.monitor.common.error.FachlicheAusnahme;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Die Pruefung der Anfrageparameter — ohne Datenbank, ohne Server. */
class NachrichtenFilterTest {

  private static final Clock UHR =
      Clock.fixed(Instant.parse("2025-12-30T04:09:47Z"), ZoneOffset.UTC);

  private static NachrichtenFilter filter(String zeitraum, String cursor, Integer limit) {
    return NachrichtenFilter.aus(
        zeitraum, null, null, null, null, null, null, null, null, cursor, limit, UHR);
  }

  /** Ein absolutes Fenster von {@code tage} Tagen, endend am Bezugspunkt der Uhr. */
  private static NachrichtenFilter mitFenster(long tage, String suche, Boolean langeSuche) {
    LocalDateTime bis = LocalDateTime.parse("2025-12-30T04:09:47");
    return NachrichtenFilter.aus(
        null,
        bis.minusDays(tage) + "Z",
        bis + "Z",
        null,
        null,
        null,
        suche,
        langeSuche,
        null,
        null,
        null,
        UHR);
  }

  private static String problemTyp(ThrowingCallable aufruf) {
    try {
      aufruf.call();
    } catch (FachlicheAusnahme fehler) {
      return fehler.problemTyp();
    }
    throw new AssertionError("Erwartet wurde eine FachlicheAusnahme");
  }

  private interface ThrowingCallable {
    void call();
  }

  @Test
  @DisplayName("Die Vorgaben: 24 Stunden, 50 Zeilen, neueste zuerst, kein Statusfilter")
  void vorgaben() {
    NachrichtenFilter filter = filter(null, null, null);

    assertThat(filter.limit()).isEqualTo(NachrichtenFilter.LIMIT_VORGABE);
    assertThat(filter.sortierung()).isEqualTo(Sortierrichtung.NEUESTE);
    assertThat(filter.cursor()).isNull();
    assertThat(filter.status())
        .as("Die Liste filtert nicht nach Status, ausser der Nutzer sagt es ausdruecklich")
        .isEmpty();
    assertThat(filter.fenster().von())
        .isEqualTo(LocalDateTime.parse("2025-12-30T04:09:47").minusHours(24));
  }

  @Test
  @DisplayName("Der Statusfilter arbeitet ueber MessageStatusKind, nicht ueber Rohwerte")
  void statusfilter_ueber_einordnungen() {
    NachrichtenFilter filter =
        NachrichtenFilter.aus(
            null,
            null,
            null,
            List.of("FEHLER", "wartend"),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            UHR);

    assertThat(filter.status())
        .containsExactlyInAnyOrder(MessageStatusKind.FEHLER, MessageStatusKind.WARTEND);

    assertThat(
            problemTyp(
                () ->
                    NachrichtenFilter.aus(
                        null,
                        null,
                        null,
                        List.of("ERROR_DUPLICATE"),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        UHR)))
        .as("Ein Rohwert ist kein gueltiger Filterwert")
        .isEqualTo("status-unbekannt");
  }

  /**
   * Die beiden Werte, die am 11.08.2026 an die Stelle von {@code ZWISCHENSCHRITT} getreten sind.
   * Fuer den Nutzer bedeuten sie Gegenteiliges — <i>aus eins wurde viel</i> gegen <i>aus viel wurde
   * eins</i> —, und der Statusfilter bietet sie deshalb einzeln an.
   */
  @Test
  @DisplayName("Der Statusfilter kennt AUFGETEILT und ZUSAMMENGEFUEHRT einzeln")
  void statusfilter_kennt_beide_neuen_werte() {
    NachrichtenFilter beide =
        NachrichtenFilter.aus(
            null,
            null,
            null,
            List.of("AUFGETEILT", "ZUSAMMENGEFUEHRT"),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            UHR);

    assertThat(beide.status())
        .containsExactlyInAnyOrder(
            MessageStatusKind.AUFGETEILT, MessageStatusKind.ZUSAMMENGEFUEHRT);

    assertThat(
            problemTyp(
                () ->
                    NachrichtenFilter.aus(
                        null,
                        null,
                        null,
                        List.of("ZWISCHENSCHRITT"),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        UHR)))
        .as("Den alten Sammelwert gibt es nicht mehr — er wird abgewiesen, nicht uebersetzt")
        .isEqualTo("status-unbekannt");
  }

  @Test
  @DisplayName("Die Seitengroesse hat ein hartes Maximum")
  void seitengroesse_hat_ein_maximum() {
    assertThat(filter(null, null, NachrichtenFilter.LIMIT_MAXIMUM).limit())
        .isEqualTo(NachrichtenFilter.LIMIT_MAXIMUM);
    assertThat(problemTyp(() -> filter(null, null, NachrichtenFilter.LIMIT_MAXIMUM + 1)))
        .isEqualTo("limit-ungueltig");
    assertThat(problemTyp(() -> filter(null, null, 0))).isEqualTo("limit-ungueltig");
  }

  @Test
  @DisplayName("Ein zu kurzer Suchbegriff ist 400")
  void suchbegriff_hat_eine_mindestlaenge() {
    assertThat(
            problemTyp(
                () ->
                    NachrichtenFilter.aus(
                        null, null, null, null, null, null, "ab", null, null, null, null, UHR)))
        .isEqualTo("suchbegriff-zu-kurz");

    assertThat(
            NachrichtenFilter.aus(
                    null, null, null, null, null, null, "  AMG  ", null, null, null, null, UHR)
                .suche())
        .as("Der Begriff wird getrimmt, bevor die Laenge zaehlt")
        .isEqualTo("AMG");

    assertThat(
            NachrichtenFilter.aus(
                    null, null, null, null, null, null, "   ", null, null, null, null, UHR)
                .suche())
        .as("Ein leerer Parameter ist kein Filter")
        .isNull();
  }

  @Test
  @DisplayName("Ein Cursor ausserhalb des Zeitfensters ist 400")
  void cursor_wird_gegen_das_fenster_geprueft() {
    String ausserhalb =
        new Seitenposition(LocalDateTime.parse("2024-01-01T00:00:00"), "irgendeine-id").kodiere();
    assertThat(problemTyp(() -> filter(null, ausserhalb, null))).isEqualTo("cursor-ungueltig");

    String innerhalb =
        new Seitenposition(LocalDateTime.parse("2025-12-30T00:00:00"), "irgendeine-id").kodiere();
    assertThat(filter(null, innerhalb, null).cursor()).isNotNull();
  }

  @Test
  @DisplayName("Unbekannte Sortierung und unbekannter Zeitraum sind 400")
  void unbekannte_codes_sind_400() {
    assertThat(problemTyp(() -> filter("48h", null, null))).isEqualTo("zeitraum-unbekannt");
    assertThat(
            problemTyp(
                () ->
                    NachrichtenFilter.aus(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "groesste",
                        null,
                        null,
                        UHR)))
        .isEqualTo("sortierung-unbekannt");
  }

  /**
   * Die Fenstergrenze der Suche (Messung L13). Drei Faelle, und der dritte ist der eigentliche
   * Punkt: {@code langeSuche} hebt die Grenze an, es hebt sie nicht auf.
   */
  @Test
  @DisplayName("Ein Suchfenster ueber 30 Tagen ist 400 — mit langeSuche bis 90 Tage nicht")
  void suchfenster_hat_eine_grenze() {
    assertThat(mitFenster(30, "LAB", null).suche())
        .as("Genau 30 Tage sind noch drin — die Grenze ist einschliessend")
        .isEqualTo("LAB");

    assertThat(problemTyp(() -> mitFenster(31, "LAB", null)))
        .as("31 Tage ohne langeSuche")
        .isEqualTo("suche-fenster-zu-gross");

    assertThat(mitFenster(90, "LAB", true).langeSuche())
        .as("90 Tage mit langeSuche gehen durch")
        .isTrue();

    assertThat(problemTyp(() -> mitFenster(91, "LAB", true)))
        .as("Auch mit langeSuche ist bei 90 Tagen Schluss")
        .isEqualTo("suche-fenster-zu-gross");
  }

  @Test
  @DisplayName("Ohne Suchbegriff greift die Fenstergrenze der Suche nicht")
  void ohne_suchbegriff_keine_fenstergrenze() {
    assertThat(mitFenster(365, null, null).fenster().spanne().toDays())
        .as("Ein Jahresfenster ohne Suche kostet dieselben 2,7 ms wie ein Tagesfenster (L1 bis L3)")
        .isEqualTo(365);
  }

  /**
   * Das Maximum aus Regel L1 bleibt die aeussere Grenze und wird <b>vor</b> der Suchgrenze
   * geprueft: Wer zwei Jahre anfragt, bekommt keinen Hinweis auf die Suche, sondern die Auskunft,
   * dass dieses Fenster ueberhaupt nicht gelesen wird.
   */
  @Test
  @DisplayName("Ueber einem Jahr bleibt es bei zeitfenster-zu-gross, auch mit langeSuche")
  void das_jahresmaximum_bleibt_die_aeussere_grenze() {
    assertThat(problemTyp(() -> mitFenster(400, "LAB", true))).isEqualTo("zeitfenster-zu-gross");
    assertThat(problemTyp(() -> mitFenster(400, null, null))).isEqualTo("zeitfenster-zu-gross");
  }

  @Test
  @DisplayName("Die Fehlerantwort nennt die geltende Grenze und die angefragte Spanne")
  void die_antwort_nennt_beide_zahlen() {
    try {
      mitFenster(45, "LAB", null);
      throw new AssertionError("Erwartet wurde eine FachlicheAusnahme");
    } catch (FachlicheAusnahme fehler) {
      assertThat(fehler.zusatz())
          .as("Ohne diese Zahlen muesste die Oberflaeche die Grenze ein zweites Mal kennen")
          .containsEntry("grenzeTage", 30L)
          .containsEntry("angefragtTage", 45L);
    }
  }

  @Test
  @DisplayName("Leere Mehrfachwerte fallen weg, statt auf den leeren Namen zu filtern")
  void leere_mehrfachwerte_fallen_weg() {
    NachrichtenFilter filter =
        NachrichtenFilter.aus(
            null,
            null,
            null,
            List.of(),
            List.of("", "  ", "40000_AMG"),
            null,
            null,
            null,
            null,
            null,
            null,
            UHR);

    assertThat(filter.prozessIds()).containsExactly("40000_AMG");
    assertThatThrownBy(() -> filter.prozessIds().add("x"))
        .as("Der Filter ist unveraenderlich")
        .isInstanceOf(UnsupportedOperationException.class);
  }

  /** Ein Filter mit {@code ueberfaellig} und einem Statusfilter — die Kombination aus E-j. */
  private static NachrichtenFilter mitUeberfaellig(Boolean ueberfaellig, List<String> status) {
    return NachrichtenFilter.aus(
        null, null, null, status, null, ueberfaellig, null, null, null, null, null, UHR);
  }

  @Test
  @DisplayName("Die Vorgabe von ueberfaellig ist aus")
  void ueberfaellig_vorgabe_ist_aus() {
    assertThat(filter(null, null, null).ueberfaellig())
        .isEqualTo(NachrichtenFilter.UEBERFAELLIG_VORGABE)
        .isFalse();
    assertThat(mitUeberfaellig(true, null).ueberfaellig()).isTrue();
    assertThat(mitUeberfaellig(false, null).ueberfaellig()).isFalse();
  }

  /**
   * <b>Die eine unvereinbare Kombination.</b> Ueberfaellig sein kann nur, was noch laeuft oder
   * wartet; jeder andere Statusfilter macht die Antwort <b>ohne Ruecksicht auf die Daten</b> leer.
   * Eine leere Liste waere dann eine Auskunft ueber den Bestand — und die waere falsch.
   */
  @Test
  @DisplayName("ueberfaellig mit einem Statusfilter ohne offenen Status ist 400")
  void ueberfaellig_mit_endstatus_wird_abgewiesen() {
    assertThat(problemTyp(() -> mitUeberfaellig(true, List.of("ABGESCHLOSSEN"))))
        .isEqualTo("ueberfaellig-und-status-unvereinbar");
    assertThat(problemTyp(() -> mitUeberfaellig(true, List.of("FEHLER", "QUITTIERT"))))
        .isEqualTo("ueberfaellig-und-status-unvereinbar");
    // UNGEKLAERT ist Endstatus — die Weigerung, etwas zu behaupten, ist keine Offenheit.
    assertThat(problemTyp(() -> mitUeberfaellig(true, List.of("UNGEKLAERT"))))
        .isEqualTo("ueberfaellig-und-status-unvereinbar");
  }

  @Test
  @DisplayName("ueberfaellig mit einem offenen Status oder ganz ohne Statusfilter geht durch")
  void ueberfaellig_mit_offenem_status_geht_durch() {
    assertThat(mitUeberfaellig(true, List.of("WARTEND")).ueberfaellig()).isTrue();
    assertThat(mitUeberfaellig(true, List.of("LAEUFT")).ueberfaellig()).isTrue();
    // Eine leere Auswahl heisst „alle" und enthaelt damit auch die offenen Status.
    assertThat(mitUeberfaellig(true, List.of()).ueberfaellig()).isTrue();
    assertThat(mitUeberfaellig(true, null).ueberfaellig()).isTrue();
    // Eine gemischte Auswahl bleibt zulaessig: Sie hat einen nichtleeren Schnitt.
    assertThat(mitUeberfaellig(true, List.of("ABGESCHLOSSEN", "WARTEND")).status())
        .containsExactlyInAnyOrder(MessageStatusKind.ABGESCHLOSSEN, MessageStatusKind.WARTEND);
  }

  @Test
  @DisplayName("Ohne ueberfaellig greift die Pruefung nicht")
  void ohne_ueberfaellig_bleibt_jeder_statusfilter_zulaessig() {
    assertThat(mitUeberfaellig(false, List.of("ABGESCHLOSSEN")).status())
        .containsExactly(MessageStatusKind.ABGESCHLOSSEN);
    assertThat(mitUeberfaellig(null, List.of("ABGESCHLOSSEN")).status())
        .containsExactly(MessageStatusKind.ABGESCHLOSSEN);
  }

  /**
   * Die Menge steht hier aufgezaehlt und im {@code MessageStatusClassifier} aus {@code
   * istEndstatus} gezogen. Dieser Test ist die Klammer, die beide zusammenhaelt: Kaeme ein neuer
   * offener Statuswert dazu, wuerde er hier rot und nicht erst im Betrieb still falsch.
   */
  @Test
  @DisplayName("UEBERFAELLIG_MOEGLICH ist genau das, was istEndstatus offen laesst")
  void ueberfaellig_moeglich_deckt_sich_mit_istEndstatus() {
    MessageStatusClassifier classifier = new MessageStatusClassifier();
    for (MessageStatusKind einordnung : MessageStatusKind.values()) {
      assertThat(NachrichtenFilter.UEBERFAELLIG_MOEGLICH.contains(einordnung))
          .as("Einordnung %s", einordnung)
          .isEqualTo(!classifier.istEndstatus(einordnung));
    }
  }
}
