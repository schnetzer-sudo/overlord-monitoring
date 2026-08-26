package de.kraftwerkone.overlord.monitor.rollup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Der Zeitschnitt beider Laeufe, ohne Datenbank.
 *
 * <p>Er ist die Stelle, an der bei diesem Schritt am ehesten etwas schiefgeht, und der Fehler waere
 * still: Ein Fenster, das eine Viertelstunde zu frueh beginnt, zaehlt einen Teilbereich doppelt —
 * aber nur, wenn dazugezaehlt statt ersetzt wird. Ein Fenster, das mitten in einer Stunde beginnt,
 * loescht einen Eimer ganz und schreibt ihn zum Teil neu. Beides faellt in keiner Summenprobe auf,
 * die nur den Gesamtbestand prueft.
 */
class RollupFensterTest {

  /** Der geltende Anker des Profils dev, auf die Sekunde (docs/datenzugriff.md §6). */
  private static final LocalDateTime ANKER = LocalDateTime.parse("2025-12-30T04:09:47");

  private static LocalDateTime zeit(String iso) {
    return LocalDateTime.parse(iso);
  }

  @Nested
  @DisplayName("Delta-Lauf")
  class Delta {

    @Test
    @DisplayName("Rueckgriff um 15 Minuten, dann Abrundung auf den Stundenanfang")
    void rueckgriff_und_abrundung() {
      RollupFenster fenster = RollupFenster.delta(zeit("2025-12-30T04:00"), ANKER);

      assertThat(fenster.von())
          .as("04:00 minus 15 min ist 03:45, abgerundet also 03:00")
          .isEqualTo(zeit("2025-12-30T03:00"));
      assertThat(fenster.bis())
          .as("04:09:47 liegt in der Stunde 04, das Ende ist der Anfang der naechsten")
          .isEqualTo(zeit("2025-12-30T05:00"));
    }

    @Test
    @DisplayName("Das Ende ist der Anfang der NAECHSTEN Stunde, auch genau auf der Stundengrenze")
    void ende_ist_immer_die_naechste_stunde() {
      RollupFenster aufDerGrenze =
          RollupFenster.delta(zeit("2025-12-30T04:00"), zeit("2025-12-30T05:00:00"));

      assertThat(aufDerGrenze.bis())
          .as(
              "Sonst haette der Lauf genau auf der Stundengrenze ein leeres Fenster — ein"
                  + " Verhalten, das an der Sekunde des Weckers haengt, ist kein Verhalten")
          .isEqualTo(zeit("2025-12-30T06:00"));
      assertThat(aufDerGrenze.istLeer()).isFalse();
    }

    /**
     * <b>Die Eigenschaft, auf der der ganze Ablauf ruht.</b> Weil der Wasserstand selbst immer ein
     * Stundenanfang ist, faellt „Wasserstand minus Nachlauf" stets in die vorige Stunde — der
     * zuletzt geschriebene Eimer wird also bei jedem Lauf noch einmal vollstaendig gerechnet. Nur
     * deshalb ist es unbedenklich, dass der obere Rand eine angebrochene Stunde einschliesst.
     */
    @ParameterizedTest(name = "Lauf um {0}")
    @DisplayName("Der zuletzt geschriebene Eimer wird immer noch einmal gerechnet")
    @ValueSource(
        strings = {
          "2025-12-30T05:00:01",
          "2025-12-30T05:05:00",
          "2025-12-30T05:30:00",
          "2025-12-30T05:59:59"
        })
    void letzter_eimer_wird_wiederholt(String jetzt) {
      // Der vorige Lauf hat bis 05:00 gerechnet, sein letzter Eimer war also 04:00.
      RollupFenster fenster = RollupFenster.delta(zeit("2025-12-30T05:00"), zeit(jetzt));

      assertThat(fenster.von())
          .as("Der Eimer 04:00 muss noch einmal in das Fenster fallen, sonst bliebe er halb")
          .isEqualTo(zeit("2025-12-30T04:00"));
    }

    @Test
    @DisplayName("Zwei aufeinanderfolgende Laeufe ueberlappen um genau einen Eimer, ohne Luecke")
    void zwei_laeufe_ueberlappen_um_genau_einen_eimer() {
      RollupFenster erster =
          RollupFenster.delta(zeit("2025-12-30T03:00"), zeit("2025-12-30T04:05"));
      RollupFenster zweiter = RollupFenster.delta(erster.bis(), zeit("2025-12-30T05:05"));

      assertThat(erster.bis()).isEqualTo(zeit("2025-12-30T05:00"));
      assertThat(zweiter.von())
          .as("Genau ein Eimer Ueberlapp — mehr waere Verschwendung, weniger eine Luecke")
          .isEqualTo(erster.bis().minusHours(1));
      assertThat(zweiter.von())
          .as("Und keine Luecke: der zweite Lauf beginnt nicht nach dem Ende des ersten")
          .isBeforeOrEqualTo(erster.bis());
    }

    @Test
    @DisplayName("Leerer Wasserstand: der Rueckgriff greift auch auf den fruehesten Zeitstempel an")
    void leerer_wasserstand_faellt_auf_den_bestandsanfang_zurueck() {
      // Ist rollup_lauf leer, setzt der Aufrufer W = MIN(Message.MessageLastUpdate) ein.
      LocalDateTime fruehesteAenderung = zeit("2024-10-01T02:00:28");

      RollupFenster fenster = RollupFenster.delta(fruehesteAenderung, ANKER);

      assertThat(fenster.von())
          .as("02:00:28 minus 15 min ist 01:45:28, abgerundet 01:00 — eine leere Stunde davor")
          .isEqualTo(zeit("2024-10-01T01:00"));
      assertThat(fenster.bis()).isEqualTo(zeit("2025-12-30T05:00"));
    }

    @Test
    @DisplayName("Gleich nach einem Volllauf: genau ein Eimer, nicht leer")
    void gleich_nach_einem_volllauf_bleibt_ein_eimer() {
      // Der Volllauf hat eben bis 06:00 gerechnet, der Delta-Lauf faellt noch in dieselbe Stunde.
      // Der Rueckgriff zieht von in die Stunde davor — die angebrochene Stunde 05 wird ersetzt.
      RollupFenster fenster =
          RollupFenster.delta(zeit("2025-12-30T06:00"), zeit("2025-12-30T05:10"));

      assertThat(fenster.istLeer())
          .as("Ein unmittelbar folgender Lauf ergibt KEIN leeres Fenster — das ist der Regelfall")
          .isFalse();
      assertThat(fenster.von()).isEqualTo(zeit("2025-12-30T05:00"));
      assertThat(fenster.bis()).isEqualTo(zeit("2025-12-30T06:00"));
      assertThat(fenster.stundeneimer()).isEqualTo(1);
    }

    @Test
    @DisplayName("Springt die Uhr zurueck, wird das Fenster leer statt zur Ausnahme")
    void zurueckspringende_uhr_ergibt_ein_leeres_fenster() {
      // Der praktische Fall im Profil dev: Nach einer Neubefuellung der Testkopie liegt der
      // ermittelte Anker frueher als zuvor, und der Wasserstand aus den alten Laeufen steht in der
      // Zukunft. Bis dorthin IST gerechnet — es gibt nichts zu tun, und das ist kein Fehler.
      RollupFenster fenster =
          RollupFenster.delta(zeit("2025-12-30T08:00"), zeit("2025-12-30T05:10"));

      assertThat(fenster.istLeer()).isTrue();
      assertThat(fenster.von()).isEqualTo(fenster.bis()).isEqualTo(zeit("2025-12-30T06:00"));
      assertThat(fenster.stundeneimer()).isZero();
    }
  }

  @Nested
  @DisplayName("Volllauf")
  class Voll {

    @Test
    @DisplayName("Kein Nachlauf — er begaenne ohnehin vor der ersten Zeile")
    void kein_nachlauf() {
      RollupFenster fenster = RollupFenster.voll(zeit("2024-10-01T02:00:28"), ANKER);

      assertThat(fenster.von())
          .as("Nur abgerundet, nicht zurueckgegriffen")
          .isEqualTo(zeit("2024-10-01T02:00"));
      assertThat(fenster.bis()).isEqualTo(zeit("2025-12-30T05:00"));
    }

    @Test
    @DisplayName("Der Volllauf deckt den Gesamtbestand der Testkopie ab")
    void deckt_den_gesamtbestand() {
      RollupFenster fenster = RollupFenster.voll(zeit("2024-10-01T02:00:28"), ANKER);

      assertThat(fenster.von())
          .as("Fruehester Zeitstempel des Bestands (M92, Scheibe 2024-10)")
          .isBeforeOrEqualTo(zeit("2024-10-01T02:00:28"));
      assertThat(fenster.bis())
          .as("Und der Anker liegt darin — die fuenf leeren Monate ebenfalls, als leere Eimer")
          .isAfter(ANKER);
      assertThat(fenster.stundeneimer())
          .as("Rund 22 Monate in Stunden, ohne dass eine Scheibe uebersprungen wuerde")
          .isGreaterThan(10_000);
    }
  }

  @Nested
  @DisplayName("Die Invarianten des Fensters")
  class Invarianten {

    @Test
    @DisplayName("Beide Grenzen liegen immer auf einem vollen Stundenanfang")
    void grenzen_liegen_auf_der_stunde() {
      RollupFenster fenster =
          RollupFenster.delta(zeit("2025-12-30T04:37:12"), zeit("2025-12-30T05:21:44"));

      assertThat(fenster.von().getMinute()).isZero();
      assertThat(fenster.von().getSecond()).isZero();
      assertThat(fenster.bis().getMinute()).isZero();
      assertThat(fenster.bis().getSecond()).isZero();
    }

    @Test
    @DisplayName("Eine Grenze mitten in der Stunde wird abgewiesen")
    void grenze_mitten_in_der_stunde_wird_abgewiesen() {
      assertThatThrownBy(
              () -> new RollupFenster(zeit("2025-12-30T03:30"), zeit("2025-12-30T05:00")))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Stundenanfang");
    }

    @Test
    @DisplayName("Ein rueckwaerts laufendes Fenster wird abgewiesen")
    void rueckwaerts_laufendes_fenster_wird_abgewiesen() {
      assertThatThrownBy(
              () -> new RollupFenster(zeit("2025-12-30T06:00"), zeit("2025-12-30T05:00")))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("rueckwaerts");
    }

    @Test
    @DisplayName("Ein Fenster ohne Grenzen gibt es nicht")
    void fenster_ohne_grenzen_gibt_es_nicht() {
      assertThatThrownBy(() -> new RollupFenster(null, zeit("2025-12-30T05:00")))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> new RollupFenster(zeit("2025-12-30T05:00"), null))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("stundeneimer zaehlt die vollen Eimer zwischen den Grenzen")
    void stundeneimer_zaehlt_richtig() {
      assertThat(
              new RollupFenster(zeit("2025-12-30T03:00"), zeit("2025-12-30T05:00")).stundeneimer())
          .isEqualTo(2);
      assertThat(
              new RollupFenster(zeit("2025-12-30T05:00"), zeit("2025-12-30T05:00")).stundeneimer())
          .isZero();
    }
  }
}
