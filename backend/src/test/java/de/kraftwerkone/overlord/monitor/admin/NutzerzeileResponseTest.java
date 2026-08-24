package de.kraftwerkone.overlord.monitor.admin;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.security.KontoZeile;
import de.kraftwerkone.overlord.monitor.security.Rolle;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@code lockedUntil} in der Antwortzeile — <b>nur, wenn die Sperre noch laeuft</b> (E20).
 *
 * <p><b>Warum das ein Einheitstest ist und kein {@code DbIT}.</b> Die Aussage haengt an einem
 * Vergleich zweier Zeitpunkte und an keiner Zeile in einer Datenbank. Ein Integrationstest muesste
 * eine Sperrfrist in ein echtes Konto schreiben, um dieselbe Verzweigung zu treffen — und der
 * interessanteste Fall, die <i>abgelaufene</i> Sperre, waere dort nur ueber eine verstellte Uhr
 * oder ueber Warten erreichbar. Hier ist er eine Zeile.
 *
 * <p><b>Die Gegenprobe traegt den Test.</b> Ohne den Fall „Sperre laeuft noch" bewiese er nur, dass
 * immer {@code null} herauskommt; ohne den Fall „abgelaufen" nur, dass der Wert durchgereicht wird.
 * Beide stehen deshalb nebeneinander, mit demselben Vergleichszeitpunkt.
 */
class NutzerzeileResponseTest {

  private static final LocalDateTime JETZT = LocalDateTime.of(2026, 8, 24, 12, 0);

  private static KontoZeile konto(boolean adminGesperrt, LocalDateTime gesperrtBisUtc) {
    return new KontoZeile(
        7L,
        "it-nutzer",
        Rolle.MANDANT,
        List.of("VOTG"),
        adminGesperrt,
        gesperrtBisUtc,
        true,
        false,
        null);
  }

  @Test
  @DisplayName("Eine noch laufende Zeitsperre steht in der Antwort")
  void laufende_zeitsperre_steht_drin() {
    LocalDateTime bis = JETZT.plusMinutes(9);

    NutzerzeileResponse zeile = NutzerzeileResponse.fuer(konto(false, bis), JETZT);

    assertThat(zeile.lockedUntil()).isEqualTo(bis.toInstant(ZoneOffset.UTC));
  }

  @Test
  @DisplayName("Eine abgelaufene Zeitsperre wird gar nicht erst uebertragen")
  void abgelaufene_zeitsperre_faellt_weg() {
    NutzerzeileResponse zeile =
        NutzerzeileResponse.fuer(konto(false, JETZT.minusMinutes(1)), JETZT);

    assertThat(zeile.lockedUntil()).isNull();
  }

  /**
   * Der Grenzfall, und er ist bewusst entschieden: Ein Wert, der <i>genau jetzt</i> ablaeuft, gilt
   * als abgelaufen. „Nur wenn es in der Zukunft liegt" woertlich genommen.
   */
  @Test
  @DisplayName("Genau jetzt ablaufend gilt als abgelaufen")
  void genau_jetzt_gilt_als_abgelaufen() {
    NutzerzeileResponse zeile = NutzerzeileResponse.fuer(konto(false, JETZT), JETZT);

    assertThat(zeile.lockedUntil()).isNull();
  }

  @Test
  @DisplayName("Ohne jede Zeitsperre bleibt das Feld leer")
  void ohne_zeitsperre_leer() {
    NutzerzeileResponse zeile = NutzerzeileResponse.fuer(konto(false, null), JETZT);

    assertThat(zeile.lockedUntil()).isNull();
    assertThat(zeile.locked()).isFalse();
  }

  /**
   * <b>Der Kern von E14 auf Antwortebene.</b> Die beiden Sperren stehen nebeneinander und werden
   * nicht verrechnet — weder wird {@code locked} wahr, weil eine Zeitsperre laeuft, noch faellt
   * {@code lockedUntil} weg, weil administrativ gesperrt ist.
   */
  @Test
  @DisplayName("Beide Sperren stehen unabhaengig nebeneinander")
  void beide_sperren_getrennt() {
    LocalDateTime bis = JETZT.plusMinutes(15);

    NutzerzeileResponse beides = NutzerzeileResponse.fuer(konto(true, bis), JETZT);
    NutzerzeileResponse nurZeit = NutzerzeileResponse.fuer(konto(false, bis), JETZT);
    NutzerzeileResponse nurAdmin = NutzerzeileResponse.fuer(konto(true, null), JETZT);

    assertThat(beides.locked()).isTrue();
    assertThat(beides.lockedUntil()).isEqualTo(bis.toInstant(ZoneOffset.UTC));

    assertThat(nurZeit.locked()).isFalse();
    assertThat(nurZeit.lockedUntil()).isEqualTo(bis.toInstant(ZoneOffset.UTC));

    assertThat(nurAdmin.locked()).isTrue();
    assertThat(nurAdmin.lockedUntil()).isNull();
  }

  /**
   * Der Wert wird als UTC gelesen und nicht durch die Zone der Anwendungsuhr geschickt — dieselbe
   * Regel wie bei {@code lastLogin}, und aus demselben Grund: {@code AnmeldeService} schreibt
   * {@code locked_until} aus der Systemuhr in UTC.
   */
  @Test
  @DisplayName("Umgerechnet wird ueber UTC, nicht ueber die Anwendungszone")
  void umrechnung_ueber_utc() {
    LocalDateTime bis = LocalDateTime.of(2026, 8, 24, 12, 14, 30);

    NutzerzeileResponse zeile = NutzerzeileResponse.fuer(konto(false, bis), JETZT);

    assertThat(zeile.lockedUntil()).hasToString("2026-08-24T12:14:30Z");
  }

  @Test
  @DisplayName("Ein nie angemeldetes Konto traegt weiterhin lastLogin = null")
  void nie_angemeldet_bleibt_null() {
    NutzerzeileResponse zeile = NutzerzeileResponse.fuer(konto(false, null), JETZT);

    assertThat(zeile.lastLogin()).isNull();
  }
}
