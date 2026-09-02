package de.kraftwerkone.overlord.monitor.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.kraftwerkone.overlord.monitor.common.error.FachlicheAusnahme;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Die drei Zeitraumpaare, <b>ohne Datenbank</b>.
 *
 * <p>Geprueft wird die Eigenschaft, an der sonst ein Diagramm still falsch wuerde: <b>Beide Grenzen
 * liegen auf einer Eimergrenze, und die obere ist der Anfang des naechsten Eimers.</b> Faellt das,
 * sind der erste und der letzte Balken angebrochen und werden trotzdem so hoch gezeichnet wie ein
 * ganzer — ein Fehler, den niemand sieht, weil das Ergebnis plausibel aussieht.
 *
 * <p>Als Bezugszeitpunkt dient der <b>Anker der Anwendungsuhr im Profil {@code dev}</b>. Die
 * erwarteten Fenster sind damit Zeichen fuer Zeichen die drei Paare, die M94 gemessen hat — und
 * genau deshalb duerfen sie hier stehen: Sie sind keine Zahl aus dem Bestand, sondern Arithmetik
 * ueber einem festen Zeitpunkt (Regel T2).
 */
class RollupzeitraumTest {

  /** Der Anker aus {@code datenzugriff.md} §6, dieselbe Zahl wie in M94. */
  private static final LocalDateTime ANKER = LocalDateTime.parse("2025-12-30T04:09:47");

  @Nested
  @DisplayName("Die Fenster, gegen den Anker der Dev-Uhr")
  class Fenster {

    @Test
    @DisplayName("48H: 48 Stundeneimer, der angebrochene gehoert dazu")
    void achtundvierzig_stunden() {
      assertThat(Rollupzeitraum.STUNDEN_48.fenster(ANKER))
          .as("P1 aus M94, Zeichen fuer Zeichen")
          .isEqualTo(
              new Zeitfenster(
                  LocalDateTime.parse("2025-12-28T05:00:00"),
                  LocalDateTime.parse("2025-12-30T05:00:00")));
    }

    @Test
    @DisplayName("30T: 30 Tageseimer, der angebrochene gehoert dazu")
    void dreissig_tage() {
      assertThat(Rollupzeitraum.TAGE_30.fenster(ANKER))
          .as("P2 aus M94")
          .isEqualTo(
              new Zeitfenster(
                  LocalDateTime.parse("2025-12-01T00:00:00"),
                  LocalDateTime.parse("2025-12-31T00:00:00")));
    }

    @Test
    @DisplayName("12M: 12 Monatseimer, der angebrochene gehoert dazu")
    void zwoelf_monate() {
      assertThat(Rollupzeitraum.MONATE_12.fenster(ANKER))
          .as("P3 aus M94")
          .isEqualTo(
              new Zeitfenster(
                  LocalDateTime.parse("2025-01-01T00:00:00"),
                  LocalDateTime.parse("2026-01-01T00:00:00")));
    }

    /**
     * <b>Der Fall, an dem sich „ausschliessend" entscheidet.</b> Liegt {@code jetzt} genau auf
     * einer Eimergrenze, ist der Eimer, der dort <i>beginnt</i>, der letzte — und die obere Grenze
     * liegt eine Einheit darueber. Waere sie gleich {@code jetzt}, verschwaende genau dieser Eimer,
     * und das Diagramm zeigte 47 Balken statt 48.
     */
    @Test
    @DisplayName("Genau auf der Eimergrenze faellt kein Eimer weg")
    void auf_der_grenze() {
      LocalDateTime punkt = LocalDateTime.parse("2025-12-30T05:00:00");

      assertThat(Rollupzeitraum.STUNDEN_48.fenster(punkt))
          .isEqualTo(
              new Zeitfenster(
                  LocalDateTime.parse("2025-12-28T06:00:00"),
                  LocalDateTime.parse("2025-12-30T06:00:00")));
    }

    /** Jedes Fenster umfasst genau so viele Eimer, wie das Paar sagt. */
    @Test
    @DisplayName("Die Zahl der Eimer stimmt mit der Fensterbreite ueberein")
    void eimerzahl_passt_zur_breite() {
      Zeitfenster stunden = Rollupzeitraum.STUNDEN_48.fenster(ANKER);
      Zeitfenster tage = Rollupzeitraum.TAGE_30.fenster(ANKER);
      Zeitfenster monate = Rollupzeitraum.MONATE_12.fenster(ANKER);

      assertThat(stunden.spanne().toHours()).isEqualTo(Rollupzeitraum.STUNDEN_48.eimer());
      assertThat(tage.spanne().toDays()).isEqualTo(Rollupzeitraum.TAGE_30.eimer());
      assertThat(monate.von().plusMonths(Rollupzeitraum.MONATE_12.eimer()))
          .as("Monate haben verschiedene Laengen — gezaehlt wird in Monaten und nicht in Tagen")
          .isEqualTo(monate.bis());
    }

    /**
     * Der Monatswechsel ist der Fall, in dem eine Rechnung „minus 365 Tage" danebenlaege: Vom
     * 01.01. aus reicht das Zwoelf-Monats-Fenster bis zum 01.02. des Vorjahres.
     */
    @Test
    @DisplayName("12M rechnet in Kalendermonaten und nicht in Tagen")
    void monate_sind_kalendermonate() {
      assertThat(Rollupzeitraum.MONATE_12.fenster(LocalDateTime.parse("2026-01-15T13:37:00")))
          .isEqualTo(
              new Zeitfenster(
                  LocalDateTime.parse("2025-02-01T00:00:00"),
                  LocalDateTime.parse("2026-02-01T00:00:00")));
    }
  }

  @Nested
  @DisplayName("Der Code aus der URL")
  class Codes {

    @Test
    @DisplayName("Die drei Codes sind 48H, 30T und 12M")
    void codes() {
      assertThat(Rollupzeitraum.reihe().stream().map(Rollupzeitraum::code))
          .containsExactly("48H", "30T", "12M");
    }

    @Test
    @DisplayName("Die Reihe ist die Suchreihenfolge des Standardfensters: eng vor weit")
    void reihenfolge() {
      assertThat(Rollupzeitraum.reihe())
          .as("Erst das engste Paar — sonst bekaeme jeder Mandant zwoelf Monate")
          .containsExactly(
              Rollupzeitraum.STUNDEN_48, Rollupzeitraum.TAGE_30, Rollupzeitraum.MONATE_12);
    }

    @Test
    @DisplayName("Gross- und Kleinschreibung sind gleichgueltig, Leerraum wird abgeschnitten")
    void schreibweise() {
      assertThat(Rollupzeitraum.ausCode(" 48h ")).isEqualTo(Rollupzeitraum.STUNDEN_48);
      assertThat(Rollupzeitraum.ausCode("30t")).isEqualTo(Rollupzeitraum.TAGE_30);
    }

    @Test
    @DisplayName("Ohne Angabe waehlt der Endpunkt selbst — null ist kein Fehler")
    void ohne_angabe() {
      assertThat(Rollupzeitraum.ausCode(null)).isNull();
      assertThat(Rollupzeitraum.ausCode("  ")).isNull();
    }

    @Test
    @DisplayName("Ein unbekannter Code ist 400 und nennt die erlaubten Werte")
    void unbekannter_code() {
      assertThatThrownBy(() -> Rollupzeitraum.ausCode("7d"))
          .isInstanceOfSatisfying(
              FachlicheAusnahme.class,
              ausnahme -> {
                assertThat(ausnahme.status().value()).isEqualTo(400);
                assertThat(ausnahme.problemTyp()).isEqualTo("zeitraum-unbekannt");
                assertThat(ausnahme.detail())
                    .as("Der Text sagt, was erlaubt ist — 7d gehoert der Liste und nicht hierher")
                    .contains("48H", "30T", "12M");
              });
    }

    /**
     * Ein stiller Fehler, der sonst niemandem auffiele: Zwei Paare mit demselben Code liessen
     * {@link Rollupzeitraum#ausCode} immer dasselbe finden.
     */
    @Test
    @DisplayName("Kein Code kommt zweimal vor")
    void codes_sind_eindeutig() {
      List<String> codes = Rollupzeitraum.reihe().stream().map(Rollupzeitraum::code).toList();

      assertThat(codes).doesNotHaveDuplicates();
    }
  }
}
