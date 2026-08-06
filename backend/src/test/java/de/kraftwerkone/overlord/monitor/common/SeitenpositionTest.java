package de.kraftwerkone.overlord.monitor.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.kraftwerkone.overlord.monitor.common.error.FachlicheAusnahme;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Cursor-Paginierung ohne Datenbank (Regel L3). */
class SeitenpositionTest {

  private static final LocalDateTime ZEITPUNKT = LocalDateTime.parse("2025-12-29T23:22:41");
  private static final String ID = "cdb605ac-98a7-414a-a393-9965c1c0a9b7";

  private static final Zeitfenster FENSTER =
      new Zeitfenster(
          LocalDateTime.parse("2025-12-29T00:00:00"), LocalDateTime.parse("2025-12-30T00:00:00"));

  @Test
  @DisplayName("Kodieren und Dekodieren ergibt wieder dieselbe Position")
  void rundlauf() {
    String cursor = new Seitenposition(ZEITPUNKT, ID).kodiere();

    assertThat(Seitenposition.dekodiere(cursor)).isEqualTo(new Seitenposition(ZEITPUNKT, ID));
  }

  @Test
  @DisplayName("Der Cursor ist undurchsichtig und passt ohne Kodierung in eine URL")
  void cursor_ist_url_tauglich() {
    String cursor = new Seitenposition(ZEITPUNKT, ID).kodiere();

    assertThat(cursor).doesNotContain("=", "+", "/", ":");
    assertThat(cursor).doesNotContain(ID);
  }

  @Test
  @DisplayName("Ein unlesbarer Cursor ist 400 und wird nicht stillschweigend ignoriert")
  void unlesbarer_cursor_ist_400() {
    List<String> unbrauchbar =
        List.of(
            "kein-base64-!!!",
            Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString("ohne-trenner".getBytes(StandardCharsets.UTF_8)),
            Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString("kein-datum|x".getBytes(StandardCharsets.UTF_8)),
            Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString((ZEITPUNKT + "|").getBytes(StandardCharsets.UTF_8)),
            "");

    for (String cursor : unbrauchbar) {
      assertThatThrownBy(() -> Seitenposition.dekodiere(cursor))
          .as("Cursor %s", cursor)
          .isInstanceOf(FachlicheAusnahme.class)
          .extracting(fehler -> ((FachlicheAusnahme) fehler).problemTyp())
          .isEqualTo("cursor-ungueltig");
    }
  }

  @Test
  @DisplayName("Ein Cursor ausserhalb des Zeitfensters ist 400")
  void cursor_ausserhalb_des_fensters_ist_400() {
    Seitenposition davor = new Seitenposition(LocalDateTime.parse("2025-12-28T23:59:59"), ID);

    assertThatThrownBy(() -> davor.imFenster(FENSTER))
        .isInstanceOf(FachlicheAusnahme.class)
        .extracting(fehler -> ((FachlicheAusnahme) fehler).problemTyp())
        .isEqualTo("cursor-ungueltig");

    assertThat(new Seitenposition(ZEITPUNKT, ID).imFenster(FENSTER)).isNotNull();
  }

  @Test
  @DisplayName("Die Seite leitet hasMore aus der Zusatzzeile ab, nicht aus einem COUNT")
  void seite_leitet_hasmore_aus_der_zusatzzeile_ab() {
    List<String> dreiGelesen = List.of("a", "b", "c");

    Seite<String> volle = Seite.aus(dreiGelesen, 2, wert -> new Seitenposition(ZEITPUNKT, wert));
    assertThat(volle.items()).containsExactly("a", "b");
    assertThat(volle.hasMore()).isTrue();
    assertThat(Seitenposition.dekodiere(volle.nextCursor()).id())
        .as("Der Cursor zeigt auf die letzte gelieferte Zeile, nicht auf die gelesene Zusatzzeile")
        .isEqualTo("b");

    Seite<String> letzte = Seite.aus(dreiGelesen, 3, wert -> new Seitenposition(ZEITPUNKT, wert));
    assertThat(letzte.items()).containsExactly("a", "b", "c");
    assertThat(letzte.hasMore()).isFalse();
    assertThat(letzte.nextCursor()).isNull();
  }
}
