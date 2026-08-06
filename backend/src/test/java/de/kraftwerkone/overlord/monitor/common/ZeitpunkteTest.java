package de.kraftwerkone.overlord.monitor.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.kraftwerkone.overlord.monitor.common.error.FachlicheAusnahme;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Die erste Haelfte der Zeitkette: <b>Wanduhrzeit der Quelle → UTC der API</b>.
 *
 * <p>Die zweite Haelfte — UTC der API → dieselbe Wanduhrzeit in der Anzeige — steht im Frontend (
 * <code>frontend/tests/format.test.ts</code>). Beide Tests halten <b>dieselben konkreten Werte</b>
 * fest; laeuft eine Seite weg, wird die andere rot. Ohne diese Verklammerung koennte jede Haelfte
 * fuer sich richtig sein und die Kette trotzdem um den Zonenversatz verschoben — genau der Zustand
 * bis zum 06.08.2026.
 */
class ZeitpunkteTest {

  /**
   * Die Zone, in der Anwendungs- und Datenbankserver laufen. Fest im Test, damit der konkrete Fall
   * unabhaengig davon prueft, wie der Rechner eingestellt ist, auf dem er laeuft.
   */
  private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

  /**
   * Der Fall, um den es geht — der letzte Zeitstempel des 29.12.2025 aus Messung M9.
   *
   * <p>In der Datenbank steht <b>23:53:50</b>, und genau das zeigt auch das Altwerkzeug. Auf der
   * Leitung ist es 22:53:50Z. Zeigte die Oberflaeche den UTC-Wert in der Zone des Browsers, staende
   * dort 22:53 — und der Nutzer faende den Beleg nicht wieder, den er im Altwerkzeug um 23:53
   * gesehen hat.
   */
  @Test
  @DisplayName("Winter: 23:53:50 in der Datenbank wird 22:53:50Z auf der Leitung")
  void wanduhrzeit_wird_utc_im_winter() {
    LocalDateTime inDerDatenbank = LocalDateTime.parse("2025-12-29T23:53:50");

    Instant aufDerLeitung = Zeitpunkte.nachUtc(inDerDatenbank, BERLIN);

    assertThat(aufDerLeitung).isEqualTo(Instant.parse("2025-12-29T22:53:50Z"));
    assertThat(aufDerLeitung.toString())
        .as("Genau dieser Wert steht im Frontend-Test als Eingabe")
        .isEqualTo("2025-12-29T22:53:50Z");
  }

  /** Im Sommer sind es zwei Stunden statt einer — die Zone rechnet, nicht ein fester Versatz. */
  @Test
  @DisplayName("Sommer: derselbe Weg mit zwei Stunden Versatz")
  void wanduhrzeit_wird_utc_im_sommer() {
    LocalDateTime inDerDatenbank = LocalDateTime.parse("2026-07-08T17:21:10");

    assertThat(Zeitpunkte.nachUtc(inDerDatenbank, BERLIN))
        .isEqualTo(Instant.parse("2026-07-08T15:21:10Z"));
  }

  /**
   * Die Gegenrichtung, die {@code von} und {@code bis} nehmen. Beide Wege benutzen dieselbe Zone —
   * sonst waere das Fenster gegen die Daten verschoben, und zwar nur im Sommer.
   */
  @Test
  @DisplayName("Hin und zurueck ergibt wieder dieselbe Wanduhrzeit")
  void hin_und_zurueck() {
    for (String wert : new String[] {"2025-12-29T23:53:50", "2026-07-08T17:21:10"}) {
      LocalDateTime wanduhrzeit = LocalDateTime.parse(wert);
      Instant utc = Zeitpunkte.nachUtc(wanduhrzeit, BERLIN);

      assertThat(Zeitpunkte.ausIso(utc.toString(), BERLIN, "von"))
          .as("Wert %s", wert)
          .isEqualTo(wanduhrzeit);
    }
  }

  /**
   * Ein Aufrufer darf den Versatz auch ausschreiben. Entscheidend ist der bezeichnete Zeitpunkt,
   * nicht seine Schreibweise — sonst haengt das Zeitfenster daran, wie der Aufrufer formatiert.
   */
  @Test
  @DisplayName("Ein ausgeschriebener Versatz bezeichnet denselben Zeitpunkt wie das Z")
  void versatz_und_z_sind_derselbe_zeitpunkt() {
    assertThat(Zeitpunkte.ausIso("2025-12-29T23:53:50+01:00", BERLIN, "von"))
        .isEqualTo(Zeitpunkte.ausIso("2025-12-29T22:53:50Z", BERLIN, "von"));
  }

  @Test
  @DisplayName("Ein leerer Zeitpunkt bleibt leer, ein unlesbarer ist 400")
  void unlesbares_ist_400() {
    assertThat(Zeitpunkte.nachUtc(null, BERLIN)).isNull();

    assertThatThrownBy(() -> Zeitpunkte.ausIso("29.12.2025", BERLIN, "bis"))
        .isInstanceOf(FachlicheAusnahme.class)
        .hasMessageContaining("bis");
  }
}
