package de.kraftwerkone.overlord.monitor.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Die Zusammenfassung der Ablagenkachel — <b>jeder Zustand und jeder Grund</b>.
 *
 * <p><b>Feste Uhr, erfundene Kennungen, keine Datenbank und keine Ablage.</b> Der Test haengt an
 * keiner Zahl aus dem Bestand (Regel T2) und sichert nichts ueber Wanduhrzeit zu (Regel T1): Das
 * Alter entsteht aus zwei gesetzten Zeitpunkten.
 */
class AblagenkachelTest {

  private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");
  private static final LocalDateTime JETZT = LocalDateTime.parse("2025-12-30T04:09:47");
  private static final Duration TAKT = Duration.ofSeconds(60);

  /** Zwei erfundene Kennungen — der Test prueft Regeln und nicht den Pflegestand. */
  private static final String ZIEL_A = "ABLAGE_ERFUNDEN_A";

  private static final String ZIEL_B = "ABLAGE_ERFUNDEN_B";

  private static AblagenResponse kachel(LocalDateTime geprueftAm, Zielstand... ziele) {
    return Ablagenkachel.aus(new Ablagenstand(geprueftAm, List.of(ziele)), TAKT, JETZT, ZONE);
  }

  private static Zielstand ziel(String serviceId, Ablagenzustand zustand) {
    return new Zielstand(serviceId, zustand);
  }

  // ─── Die Zusammenfassung ueber die Ziele ──────────────────────────────────────

  @Nested
  @DisplayName("Mit frischem Stand entscheiden die Ziele")
  class MitFrischemStand {

    @Test
    @DisplayName("Alle Ziele erreichbar → erreichbar, und ohne Grund")
    void alle_erreichbar() {
      AblagenResponse kachel =
          kachel(
              JETZT,
              ziel(ZIEL_A, Ablagenzustand.ERREICHBAR),
              ziel(ZIEL_B, Ablagenzustand.ERREICHBAR));

      assertThat(kachel.zustand()).isEqualTo(Ablagenzustand.ERREICHBAR);
      assertThat(kachel.grund()).as("Ein erreichbarer Zustand braucht keine Erklaerung").isNull();
      assertThat(kachel.ziele())
          .containsExactly(
              new AblagenzielResponse(ZIEL_A, Ablagenzustand.ERREICHBAR),
              new AblagenzielResponse(ZIEL_B, Ablagenzustand.ERREICHBAR));
    }

    @Test
    @DisplayName("Ein Ziel nicht erreichbar schlaegt durch — auch neben einem erreichbaren")
    void eines_nicht_erreichbar() {
      AblagenResponse kachel =
          kachel(
              JETZT,
              ziel(ZIEL_A, Ablagenzustand.ERREICHBAR),
              ziel(ZIEL_B, Ablagenzustand.NICHT_ERREICHBAR));

      assertThat(kachel.zustand()).isEqualTo(Ablagenzustand.NICHT_ERREICHBAR);
      assertThat(kachel.grund()).isNull();
    }

    @Test
    @DisplayName("Nicht erreichbar schlaegt auch ein ungeklaertes Ziel")
    void nicht_erreichbar_vor_ungeklaert() {
      // Die Reihenfolge ist Absicht: "eine Ablage antwortet nicht" ist die schaerfere Auskunft als
      // "eine Ablage antwortet unerwartet", und die schaerfere gehoert auf die Kachel.
      AblagenResponse kachel =
          kachel(
              JETZT,
              ziel(ZIEL_A, Ablagenzustand.UNGEKLAERT),
              ziel(ZIEL_B, Ablagenzustand.NICHT_ERREICHBAR));

      assertThat(kachel.zustand()).isEqualTo(Ablagenzustand.NICHT_ERREICHBAR);
    }

    @Test
    @DisplayName("Ein ungeklaertes Ziel → ungeklaert mit dem Grund ZIEL_UNGEKLAERT")
    void eines_ungeklaert() {
      AblagenResponse kachel =
          kachel(
              JETZT,
              ziel(ZIEL_A, Ablagenzustand.ERREICHBAR),
              ziel(ZIEL_B, Ablagenzustand.UNGEKLAERT));

      assertThat(kachel.zustand()).isEqualTo(Ablagenzustand.UNGEKLAERT);
      assertThat(kachel.grund()).isEqualTo(Ablagengrund.ZIEL_UNGEKLAERT);
    }

    @Test
    @DisplayName("Der Pruefzeitpunkt steht in UTC, sein Alter gegen die Anwendungsuhr")
    void zeitpunkt_und_alter() {
      AblagenResponse kachel =
          kachel(
              LocalDateTime.parse("2025-12-30T04:09:17"), ziel(ZIEL_A, Ablagenzustand.ERREICHBAR));

      // 04:09:17 Ortszeit im Dezember ist 03:09:17Z -- dieselbe Bruchstelle wie ueberall:
      // Zeitstempel der Quelle sind Wanduhrzeit und werden nicht konvertiert.
      assertThat(kachel.geprueftAm()).isEqualTo(Instant.parse("2025-12-30T03:09:17Z"));
      assertThat(kachel.alterSekunden()).isEqualTo(30L);
    }
  }

  // ─── Die vier Gruende, bei denen der Beleg fehlt ──────────────────────────────

  @Nested
  @DisplayName("Ohne Beleg ist die Kachel ungeklaert — mit benanntem Grund (E-125)")
  class OhneBeleg {

    @Test
    @DisplayName("Abgeschaltet: es gibt die Pruefung nicht")
    void abgeschaltet() {
      AblagenResponse kachel = Ablagenkachel.abgeschaltet();

      assertThat(kachel.zustand()).isEqualTo(Ablagenzustand.UNGEKLAERT);
      assertThat(kachel.grund()).isEqualTo(Ablagengrund.ABGESCHALTET);
      assertThat(kachel.ziele()).isEmpty();
      assertThat(kachel.geprueftAm()).isNull();
      assertThat(kachel.alterSekunden()).isNull();
    }

    @Test
    @DisplayName("Noch kein Durchgang: die Pruefung laeuft, hat aber nichts geliefert")
    void noch_kein_durchgang() {
      AblagenResponse kachel = Ablagenkachel.aus(null, TAKT, JETZT, ZONE);

      assertThat(kachel.zustand()).isEqualTo(Ablagenzustand.UNGEKLAERT);
      assertThat(kachel.grund()).isEqualTo(Ablagengrund.NOCH_KEIN_DURCHGANG);
      assertThat(kachel.geprueftAm())
          .as("Ohne Durchgang gibt es keinen Pruefzeitpunkt — und keine Null als Ersatz")
          .isNull();
    }

    @Test
    @DisplayName("Kein Ziel eingetragen: eine Kachel ohne Ziel ist nicht gruen")
    void kein_ziel() {
      AblagenResponse kachel = kachel(JETZT);

      assertThat(kachel.zustand()).isEqualTo(Ablagenzustand.UNGEKLAERT);
      assertThat(kachel.grund()).isEqualTo(Ablagengrund.KEIN_ZIEL_EINGETRAGEN);
      assertThat(kachel.ziele()).isEmpty();
      assertThat(kachel.geprueftAm())
          .as("Der Durchgang hat stattgefunden — er hat nur nichts zu pruefen gefunden")
          .isNotNull();
    }

    @Test
    @DisplayName("Gruen darf seinen Beleg nicht ueberleben: zwei Takte alt reicht nicht mehr")
    void veraltet_schlaegt_gruen() {
      LocalDateTime zuAlt = JETZT.minus(TAKT.multipliedBy(2)).minusSeconds(1);

      AblagenResponse kachel = kachel(zuAlt, ziel(ZIEL_A, Ablagenzustand.ERREICHBAR));

      assertThat(kachel.zustand()).isEqualTo(Ablagenzustand.UNGEKLAERT);
      assertThat(kachel.grund()).isEqualTo(Ablagengrund.STAND_VERALTET);
      assertThat(kachel.ziele())
          .as("Der alte Stand bleibt sichtbar — er ist nur nicht mehr die Auskunft der Kachel")
          .hasSize(1);
      assertThat(kachel.alterSekunden()).isEqualTo(TAKT.multipliedBy(2).toSeconds() + 1);
    }

    @Test
    @DisplayName("Und er ueberlebt auch ein Rot nicht — in jede Richtung dieselbe Regel")
    void veraltet_schlaegt_auch_rot() {
      LocalDateTime zuAlt = JETZT.minus(TAKT.multipliedBy(2)).minusSeconds(1);

      AblagenResponse kachel = kachel(zuAlt, ziel(ZIEL_A, Ablagenzustand.NICHT_ERREICHBAR));

      assertThat(kachel.zustand())
          .as("Ein alter roter Stand behauptete eine Stoerung, die niemand mehr geprueft hat")
          .isEqualTo(Ablagenzustand.UNGEKLAERT);
      assertThat(kachel.grund()).isEqualTo(Ablagengrund.STAND_VERALTET);
    }

    @Test
    @DisplayName("Genau zwei Takte alt ist noch frisch — ein ausgefallener Durchgang ist erlaubt")
    void genau_zwei_takte_ist_frisch() {
      AblagenResponse kachel =
          kachel(JETZT.minus(TAKT.multipliedBy(2)), ziel(ZIEL_A, Ablagenzustand.ERREICHBAR));

      assertThat(kachel.zustand()).isEqualTo(Ablagenzustand.ERREICHBAR);
      assertThat(kachel.grund()).isNull();
    }

    @Test
    @DisplayName("Ein Stand aus der Zukunft gilt als frisch, und sein Alter ist null")
    void stand_aus_der_zukunft() {
      // Er entsteht nicht im Betrieb -- Pruefzeitpunkt und jetzt kommen aus derselben Uhr. Wenn
      // doch, ist er kein Grund, eine gerade erhobene Auskunft wegzuwerfen (E-75).
      AblagenResponse kachel =
          kachel(JETZT.plusSeconds(5), ziel(ZIEL_A, Ablagenzustand.ERREICHBAR));

      assertThat(kachel.zustand()).isEqualTo(Ablagenzustand.ERREICHBAR);
      assertThat(kachel.alterSekunden()).isNull();
    }
  }

  @Test
  @DisplayName("Die Kachel ist unveraenderlich: ihre Zielliste laesst sich nicht erweitern")
  void zielliste_ist_unveraenderlich() {
    AblagenResponse kachel = kachel(JETZT, ziel(ZIEL_A, Ablagenzustand.ERREICHBAR));

    assertThat(kachel.ziele().getClass().getName())
        .as("List.copyOf liefert eine unveraenderliche Liste")
        .doesNotContain("ArrayList");
  }
}
