package de.kraftwerkone.overlord.monitor.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.kraftwerkone.overlord.monitor.common.Baumfenster.Segment;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Die Zerlegung eines freien Fensters in Rollup-Segmente — <b>ohne Datenbank</b>.
 *
 * <p>Geprueft wird die Arithmetik, an der die Kennzahlen des freien Fensters haengen: <b>kein Eimer
 * doppelt, keiner ausgelassen</b>, ueber alle Uebergaenge zwischen den drei Ebenen. M151 hat das
 * fuer <b>ein</b> Fenster gegen den Bestand gemessen (null abweichende Zeilen); fuer jedes andere
 * Fenster ist es die Rechnung hier ({@code docs/process-view.md} §34, Belegvermerk).
 *
 * <p><b>Alle Pruefwerte sind erfunden</b> (Regel T2) — Zeitpunkte, an denen die Zerlegung ihre
 * Uebergaenge hat, keine Zahl aus der Testkopie. Der Boesfall traegt die Grenzen aus M149, weil er
 * dort gemessen ist; auch das ist Kalenderarithmetik und kein Bestandswert.
 *
 * <p><b>Keine Wanduhrzeit</b> (Regel T1): Alle Zeitpunkte sind Konstanten, keine Uhr wird gelesen.
 */
class BaumfensterTest {

  private static LocalDateTime t(String iso) {
    return LocalDateTime.parse(iso);
  }

  private static Segment stunde(String von, String bis) {
    return new Segment(Rollupebene.STUNDE, t(von), t(bis));
  }

  private static Segment tag(String von, String bis) {
    return new Segment(Rollupebene.TAG, t(von), t(bis));
  }

  private static Segment monat(String von, String bis) {
    return new Segment(Rollupebene.MONAT, t(von), t(bis));
  }

  /**
   * Die beiden Eigenschaften, die jedes Fenster halten muss: lueckenlos, ueberschneidungsfrei, und
   * die Vereinigung ist genau {@code [von, bis)} — Zeichen fuer Zeichen, nicht ungefaehr.
   */
  private static void lueckenlosUndUeberschneidungsfrei(
      List<Segment> segmente, LocalDateTime von, LocalDateTime bis) {
    assertThat(segmente).as("Ein Fenster ergibt ein bis fuenf Segmente").hasSizeBetween(1, 5);
    assertThat(segmente.getFirst().von()).as("Die Vereinigung beginnt bei von").isEqualTo(von);
    assertThat(segmente.getLast().bis()).as("Die Vereinigung endet bei bis").isEqualTo(bis);
    for (int i = 1; i < segmente.size(); i++) {
      assertThat(segmente.get(i).von())
          .as("Segment %d beginnt genau dort, wo Segment %d endet", i + 1, i)
          .isEqualTo(segmente.get(i - 1).bis());
      assertThat(segmente.get(i).ebene())
          .as("Zwei benachbarte Segmente derselben Ebene waeren eines")
          .isNotEqualTo(segmente.get(i - 1).ebene());
    }
    for (Segment segment : segmente) {
      assertThat(segment.von()).isBefore(segment.bis());
      switch (segment.ebene()) {
        case STUNDE -> {
          assertThat(Baumfenster.istVolleStunde(segment.von())).isTrue();
          assertThat(Baumfenster.istVolleStunde(segment.bis())).isTrue();
        }
        case TAG -> {
          assertThat(segment.von().toLocalTime().toSecondOfDay()).isZero();
          assertThat(segment.bis().toLocalTime().toSecondOfDay()).isZero();
        }
        case MONAT -> {
          assertThat(segment.von().getDayOfMonth()).isEqualTo(1);
          assertThat(segment.bis().getDayOfMonth()).isEqualTo(1);
          assertThat(segment.von().toLocalTime().toSecondOfDay()).isZero();
          assertThat(segment.bis().toLocalTime().toSecondOfDay()).isZero();
        }
      }
    }
    // Die Stundeneimer aller Segmente zusammen sind genau die des Fensters — in Wanduhrzeit
    // gerechnet, wie die Eimer selbst.
    long stundenDerSegmente =
        segmente.stream().mapToLong(s -> Duration.between(s.von(), s.bis()).toHours()).sum();
    assertThat(stundenDerSegmente).isEqualTo(Duration.between(von, bis).toHours());
  }

  @Nested
  @DisplayName("Die sechs Grenzfaelle")
  class Grenzfaelle {

    @Test
    @DisplayName("Innerhalb eines Tages: ein Stundensegment")
    void innerhalb_eines_tages() {
      assertThat(Baumfenster.zerlegung(t("2025-03-10T14:00"), t("2025-03-10T20:00")))
          .containsExactly(stunde("2025-03-10T14:00", "2025-03-10T20:00"));
    }

    /**
     * Ueber Mitternacht, aber ohne ganzen Tag: weiterhin <b>ein</b> Stundensegment. Zwei Bereiche
     * auf derselben Ebene, die aneinanderstossen, sind einer.
     */
    @Test
    @DisplayName("Ueber Mitternacht ohne ganzen Tag: immer noch ein Stundensegment")
    void ueber_mitternacht_ohne_ganzen_tag() {
      assertThat(Baumfenster.zerlegung(t("2025-03-10T23:00"), t("2025-03-11T01:00")))
          .containsExactly(stunde("2025-03-10T23:00", "2025-03-11T01:00"));
    }

    @Test
    @DisplayName("Genau ein ganzer Tag: ein Tagessegment")
    void genau_ein_ganzer_tag() {
      assertThat(Baumfenster.zerlegung(t("2025-03-10T00:00"), t("2025-03-11T00:00")))
          .containsExactly(tag("2025-03-10T00:00", "2025-03-11T00:00"));
      // Auch am Monatsanfang und am Monatsende — dort liegt die Grenze zur Monatsebene.
      assertThat(Baumfenster.zerlegung(t("2025-03-01T00:00"), t("2025-03-02T00:00")))
          .containsExactly(tag("2025-03-01T00:00", "2025-03-02T00:00"));
      assertThat(Baumfenster.zerlegung(t("2025-03-31T00:00"), t("2025-04-01T00:00")))
          .containsExactly(tag("2025-03-31T00:00", "2025-04-01T00:00"));
    }

    @Test
    @DisplayName("Genau ganze Monate: ein Monatssegment")
    void genau_ganze_monate() {
      assertThat(Baumfenster.zerlegung(t("2025-01-01T00:00"), t("2025-12-01T00:00")))
          .containsExactly(monat("2025-01-01T00:00", "2025-12-01T00:00"));
      // Ein einzelner Monat ist ebenfalls ein Monatssegment und keine 31 Tage.
      assertThat(Baumfenster.zerlegung(t("2025-03-01T00:00"), t("2025-04-01T00:00")))
          .containsExactly(monat("2025-03-01T00:00", "2025-04-01T00:00"));
    }

    /**
     * <b>Der Fall, an dem sich „ein Segment" entscheidet.</b> Dreissig ganze Tage ueber eine
     * Monatsgrenze hinweg — ohne ganzen Monat dazwischen. Zwei Tagesbereiche (bis zur Monatsgrenze,
     * ab der Monatsgrenze) waeren zwei Zweige fuer dieselbe Lesung.
     */
    @Test
    @DisplayName("30 ganze Tage, nicht monatsbuendig: ein Tagessegment")
    void dreissig_ganze_tage() {
      assertThat(Baumfenster.zerlegung(t("2025-11-30T00:00"), t("2025-12-30T00:00")))
          .containsExactly(tag("2025-11-30T00:00", "2025-12-30T00:00"));
    }

    /**
     * Der Boesfall aus M149: beide Enden krumm, alle drei Ebenen beteiligt, fuenf Abschnitte. Die
     * Grenzen sind die, die dort gemessen sind — Kalenderarithmetik, kein Bestandswert.
     */
    @Test
    @DisplayName("Der Boesfall aus M149: fuenf Segmente ueber alle drei Ebenen")
    void boesfall() {
      List<Segment> segmente = Baumfenster.zerlegung(t("2024-12-29T14:00"), t("2025-12-30T03:00"));

      assertThat(segmente)
          .containsExactly(
              stunde("2024-12-29T14:00", "2024-12-30T00:00"),
              tag("2024-12-30T00:00", "2025-01-01T00:00"),
              monat("2025-01-01T00:00", "2025-12-01T00:00"),
              tag("2025-12-01T00:00", "2025-12-30T00:00"),
              stunde("2025-12-30T00:00", "2025-12-30T03:00"));
      // 10 + 48 + 8.016 + 696 + 3 Stundeneimer — die 8.773 aus Abweichung A3 in §30.
      lueckenlosUndUeberschneidungsfrei(segmente, t("2024-12-29T14:00"), t("2025-12-30T03:00"));
      assertThat(Duration.between(t("2024-12-29T14:00"), t("2025-12-30T03:00")).toHours())
          .isEqualTo(8773);
    }

    @Test
    @DisplayName("Ganze Monate mit Tagen davor und danach, ohne Stunden: drei Segmente")
    void monate_mit_tagen_ohne_stunden() {
      assertThat(Baumfenster.zerlegung(t("2025-01-30T00:00"), t("2025-04-03T00:00")))
          .containsExactly(
              tag("2025-01-30T00:00", "2025-02-01T00:00"),
              monat("2025-02-01T00:00", "2025-04-01T00:00"),
              tag("2025-04-01T00:00", "2025-04-03T00:00"));
    }

    @Test
    @DisplayName("Stunden und Tage ohne Monat: drei Segmente")
    void stunden_und_tage_ohne_monat() {
      assertThat(Baumfenster.zerlegung(t("2025-03-10T14:00"), t("2025-03-12T03:00")))
          .containsExactly(
              stunde("2025-03-10T14:00", "2025-03-11T00:00"),
              tag("2025-03-11T00:00", "2025-03-12T00:00"),
              stunde("2025-03-12T00:00", "2025-03-12T03:00"));
    }
  }

  @Nested
  @DisplayName("Lueckenlos und ueberschneidungsfrei — ueber eine Menge erfundener Fenster")
  class Eigenschaften {

    /**
     * Die Grenzpunkte, an denen die Zerlegung ihre Uebergaenge hat: krumme Stunden, Mitternacht,
     * Monatsanfang, Monatsende, Jahreswechsel, Schaltjahr. Jedes Paar {@code von < bis} daraus ist
     * ein Fenster — 22 Punkte, 231 Fenster.
     */
    private static final List<LocalDateTime> PUNKTE =
        List.of(
            t("2024-02-28T23:00"),
            t("2024-02-29T00:00"),
            t("2024-02-29T13:00"),
            t("2024-03-01T00:00"),
            t("2024-03-01T01:00"),
            t("2024-06-15T09:00"),
            t("2024-12-29T14:00"),
            t("2024-12-30T00:00"),
            t("2024-12-31T23:00"),
            t("2025-01-01T00:00"),
            t("2025-01-01T01:00"),
            t("2025-01-02T00:00"),
            t("2025-01-31T23:00"),
            t("2025-02-01T00:00"),
            t("2025-03-30T02:00"),
            t("2025-04-01T00:00"),
            t("2025-10-26T03:00"),
            t("2025-11-30T00:00"),
            t("2025-12-01T00:00"),
            t("2025-12-30T00:00"),
            t("2025-12-30T03:00"),
            t("2026-01-01T00:00"));

    @Test
    @DisplayName("Jedes Fenster aus 22 Grenzpunkten ist lueckenlos, ueberschneidungsfrei und exakt")
    void alle_fenster() {
      int gezaehlt = 0;
      for (LocalDateTime von : PUNKTE) {
        for (LocalDateTime bis : PUNKTE) {
          if (!von.isBefore(bis)) {
            continue;
          }
          gezaehlt++;
          lueckenlosUndUeberschneidungsfrei(Baumfenster.zerlegung(von, bis), von, bis);
        }
      }
      assertThat(gezaehlt).as("22 Punkte ergeben 231 Fenster").isEqualTo(231);
    }

    /**
     * Ein Fenster von hoechstens einem Jahr zerfaellt in hoechstens 23 + 23 Stundeneimer, 30 + 30
     * Tageseimer und 12 Monatseimer — die Schranke aus der Bauform ({@code docs/process-view.md}
     * §33 nennt 11 Monate fuer krumme Enden; ein monatsbuendiges Jahr sind zwoelf). Hier fuer jedes
     * Fenster bis ein Jahr je Ebene nachgezaehlt.
     */
    @Test
    @DisplayName("Keine Ebene liest mehr Eimer, als die Bauform verspricht")
    void schranke_der_bauform() {
      for (LocalDateTime von : PUNKTE) {
        for (LocalDateTime bis : PUNKTE) {
          if (!von.isBefore(bis) || von.isBefore(bis.minusYears(1))) {
            continue;
          }
          long stunden = 0;
          long tage = 0;
          long monate = 0;
          for (Segment segment : Baumfenster.zerlegung(von, bis)) {
            switch (segment.ebene()) {
              case STUNDE -> stunden += Duration.between(segment.von(), segment.bis()).toHours();
              case TAG -> tage += Duration.between(segment.von(), segment.bis()).toDays();
              case MONAT -> monate += ChronoUnit.MONTHS.between(segment.von(), segment.bis());
            }
          }
          assertThat(stunden).as("Stunden von %s bis %s", von, bis).isLessThanOrEqualTo(46);
          assertThat(tage).as("Tage von %s bis %s", von, bis).isLessThanOrEqualTo(60);
          assertThat(monate).as("Monate von %s bis %s", von, bis).isLessThanOrEqualTo(12);
        }
      }
    }
  }

  /**
   * Die Eimer sind Wanduhrzeit des Quellservers. Am Umstellungstag hat ein Tag dort 23 oder 25
   * Stunden — und die Zerlegung darf davon nichts merken, weil sie ueber den Kalender rechnet und
   * nicht ueber Dauern.
   */
  @Nested
  @DisplayName("Die beiden Umstellungstage")
  class Umstellungstage {

    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

    @Test
    @DisplayName("Der 23-Stunden-Tag ist ein Tagessegment, und plusHours(24) traefe daneben")
    void kurzer_tag() {
      LocalDateTime von = t("2025-03-30T00:00");
      LocalDateTime bis = t("2025-03-31T00:00");

      assertThat(Baumfenster.zerlegung(von, bis))
          .containsExactly(tag("2025-03-30T00:00", "2025-03-31T00:00"));

      // Der Beleg, dass der Tag in dieser Zone 23 Stunden hat — und warum ueber Dauern gerechnet
      // die Grenze um eine Stunde daneben laege.
      ZonedDateTime anfang = von.atZone(BERLIN);
      assertThat(Duration.between(anfang, bis.atZone(BERLIN)).toHours()).isEqualTo(23);
      assertThat(anfang.plusHours(24).toLocalDateTime()).isEqualTo(t("2025-03-31T01:00"));
    }

    @Test
    @DisplayName("Der 25-Stunden-Tag ist ein Tagessegment, und plusHours(24) traefe daneben")
    void langer_tag() {
      LocalDateTime von = t("2025-10-26T00:00");
      LocalDateTime bis = t("2025-10-27T00:00");

      assertThat(Baumfenster.zerlegung(von, bis))
          .containsExactly(tag("2025-10-26T00:00", "2025-10-27T00:00"));

      ZonedDateTime anfang = von.atZone(BERLIN);
      assertThat(Duration.between(anfang, bis.atZone(BERLIN)).toHours()).isEqualTo(25);
      assertThat(anfang.plusHours(24).toLocalDateTime()).isEqualTo(t("2025-10-26T23:00"));
    }

    @Test
    @DisplayName("Stundensegmente ueber den Umstellungstag hinweg bleiben lueckenlos")
    void stunden_um_den_umstellungstag() {
      for (String[] fenster :
          new String[][] {
            {"2025-03-29T22:00", "2025-03-31T02:00"},
            {"2025-10-25T22:00", "2025-10-27T02:00"},
            {"2025-03-30T00:00", "2025-03-30T05:00"},
            {"2025-10-26T01:00", "2025-10-26T04:00"}
          }) {
        LocalDateTime von = t(fenster[0]);
        LocalDateTime bis = t(fenster[1]);
        lueckenlosUndUeberschneidungsfrei(Baumfenster.zerlegung(von, bis), von, bis);
      }
      assertThat(Baumfenster.zerlegung(t("2025-03-29T22:00"), t("2025-03-31T02:00")))
          .containsExactly(
              stunde("2025-03-29T22:00", "2025-03-30T00:00"),
              tag("2025-03-30T00:00", "2025-03-31T00:00"),
              stunde("2025-03-31T00:00", "2025-03-31T02:00"));
    }
  }

  @Nested
  @DisplayName("Die drei Paare bleiben unzerlegt")
  class Paare {

    /** Der Anker der Dev-Uhr, derselbe Bezug wie in {@code RollupzeitraumTest}. */
    private static final LocalDateTime ANKER = t("2025-12-30T04:09:47");

    @Test
    @DisplayName("Ein Paar ergibt genau ein Segment auf seiner Ebene mit seinem heutigen Fenster")
    void ein_paar_ein_segment() {
      for (Rollupzeitraum paar : Rollupzeitraum.values()) {
        Zeitfenster heute = paar.fenster(ANKER);
        Baumfenster fenster = Baumfenster.paar(paar);

        assertThat(fenster.istFrei()).isFalse();
        assertThat(fenster.code()).isEqualTo(paar.code());
        assertThat(fenster.fenster(ANKER)).isEqualTo(heute);
        assertThat(fenster.segmente(ANKER))
            .as("Paar %s", paar.code())
            .containsExactly(new Segment(paar.ebene(), heute.von(), heute.bis()));
      }
    }

    /**
     * <b>Die bewusste Ungleichbehandlung.</b> Das 48-Stunden-Fenster reicht ueber zwei Tage; ein
     * freies Fenster derselben Grenzen wuerde in drei Segmente zerlegt. Das Paar bleibt eines.
     */
    @Test
    @DisplayName("48H wird nicht zerlegt, dasselbe freie Fenster schon")
    void achtundvierzig_stunden_bleiben_ungeteilt() {
      Zeitfenster heute = Rollupzeitraum.STUNDEN_48.fenster(ANKER);

      assertThat(Baumfenster.paar(Rollupzeitraum.STUNDEN_48).segmente(ANKER)).hasSize(1);
      assertThat(Baumfenster.frei(heute.von(), heute.bis()).segmente(ANKER))
          .containsExactly(
              stunde("2025-12-28T05:00", "2025-12-29T00:00"),
              tag("2025-12-29T00:00", "2025-12-30T00:00"),
              stunde("2025-12-30T00:00", "2025-12-30T05:00"));
    }

    @Test
    @DisplayName("Jedes Paar nennt seine Ebene")
    void ebenen() {
      assertThat(Rollupzeitraum.STUNDEN_48.ebene()).isEqualTo(Rollupebene.STUNDE);
      assertThat(Rollupzeitraum.TAGE_30.ebene()).isEqualTo(Rollupebene.TAG);
      assertThat(Rollupzeitraum.MONATE_12.ebene()).isEqualTo(Rollupebene.MONAT);
    }
  }

  @Nested
  @DisplayName("Das freie Fenster")
  class Frei {

    @Test
    @DisplayName("Es haengt an keiner Uhr: derselbe Ausgang fuer jedes jetzt")
    void unabhaengig_von_der_uhr() {
      Baumfenster fenster = Baumfenster.frei(t("2025-03-10T14:00"), t("2025-03-12T03:00"));

      assertThat(fenster.istFrei()).isTrue();
      assertThat(fenster.code()).isEqualTo("FREI");
      assertThat(fenster.paar()).isNull();
      List<Segment> a = fenster.segmente(t("2025-12-30T04:09:47"));
      List<Segment> b = fenster.segmente(t("2031-06-01T00:00:00"));
      assertThat(a).isEqualTo(b);
      assertThat(fenster.fenster(t("2031-06-01T00:00:00")))
          .isEqualTo(new Zeitfenster(t("2025-03-10T14:00"), t("2025-03-12T03:00")));
    }

    @Test
    @DisplayName("Krumme Grenzen sind ein Programmierfehler, kein gerundetes Fenster")
    void krumme_grenzen() {
      assertThatThrownBy(() -> Baumfenster.frei(t("2025-03-10T14:30"), t("2025-03-12T03:00")))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> Baumfenster.frei(t("2025-03-10T14:00"), t("2025-03-12T03:00:01")))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> Baumfenster.frei(t("2025-03-12T03:00"), t("2025-03-12T03:00")))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Die Eimerzahlen der Ebenen sind fuer den Test nachzaehlbar")
    void eimerzahlen() {
      List<Segment> segmente =
          new ArrayList<>(Baumfenster.zerlegung(t("2025-01-30T00:00"), t("2025-04-03T00:00")));
      assertThat(segmente).hasSize(3);
      assertThat(Duration.between(segmente.get(0).von(), segmente.get(0).bis()).toDays())
          .isEqualTo(2);
      assertThat(ChronoUnit.MONTHS.between(segmente.get(1).von(), segmente.get(1).bis()))
          .isEqualTo(2);
      assertThat(Duration.between(segmente.get(2).von(), segmente.get(2).bis()).toDays())
          .isEqualTo(2);
    }
  }
}
