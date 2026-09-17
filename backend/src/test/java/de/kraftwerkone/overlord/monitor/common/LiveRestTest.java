package de.kraftwerkone.overlord.monitor.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Der Live-Rest ohne Datenbank: die <b>Entscheidung</b> ({@link LiveRest}) und die
 * <b>Verrechnung</b> ({@link LiveRestKorrektur}).
 *
 * <p>Alle Zeitpunkte und Zahlen sind erfunden (Regel T2), keine Uhr wird gelesen (Regel T1). Die
 * Grenzfaelle sind die aus {@code docs/live-rest.md}: kein Lauf, G nach und G gleich dem Anfang der
 * aktuellen Stunde, G genau drei Stunden vor jetzt und knapp darueber, beide Umstellungstage.
 */
class LiveRestTest {

  private static LocalDateTime t(String iso) {
    return LocalDateTime.parse(iso);
  }

  private static LiveRestEntscheidung entscheide(String wasserstand, String jetzt) {
    return LiveRest.entscheide(
        wasserstand == null ? Optional.empty() : Optional.of(t(wasserstand)), t(jetzt));
  }

  @Nested
  @DisplayName("Die Entscheidung")
  class Entscheidung {

    @Test
    @DisplayName("Ohne abgeschlossenen fehlerfreien Lauf: ausgesetzt, ohne Zeitangabe")
    void kein_lauf() {
      LiveRestEntscheidung e = entscheide(null, "2025-12-30T04:09:47");

      assertThat(e.zustand()).isEqualTo(LiveRestZustand.AUSGESETZT);
      assertThat(e.vollstaendigBis()).isNull();
      assertThat(e.liveVon()).isNull();
      assertThat(e.liveBis()).isNull();
      assertThat(e.angewandt()).isFalse();
    }

    /**
     * Der Normalfall im Betrieb: Der Lauf um 14:05 hat bis 15:00 gerechnet (W = 15:00, G = 14:00),
     * und um 14:37 fehlt im Eimer 14:00 eine gute halbe Stunde.
     */
    @Test
    @DisplayName("Der Normalfall: der Eimer des Laufs ist die laufende Stunde")
    void normalfall() {
      LiveRestEntscheidung e = entscheide("2025-12-30T15:00:00", "2025-12-30T14:37:12");

      assertThat(e.zustand()).isEqualTo(LiveRestZustand.ANGEWANDT);
      assertThat(e.liveVon()).isEqualTo(t("2025-12-30T14:00:00"));
      assertThat(e.liveBis()).isEqualTo(t("2025-12-30T15:00:00"));
      assertThat(e.vollstaendigBis()).isNull();
    }

    @Test
    @DisplayName(
        "G nach dem Anfang der aktuellen Stunde: nicht noetig — der Rollup reicht ueber die Uhr hinaus")
    void g_nach_der_aktuellen_stunde() {
      // Im Profil dev: ein Lauf gegen die Systemuhr (August 2026), die Anwendungsuhr im Dezember
      // 2025.
      LiveRestEntscheidung e = entscheide("2026-08-27T15:00:00", "2025-12-30T04:09:47");

      assertThat(e.zustand()).isEqualTo(LiveRestZustand.NICHT_NOETIG);
      assertThat(e.vollstaendigBis()).isNull();
      assertThat(e.liveVon()).isNull();
      assertThat(e.liveBis()).isNull();
    }

    /** Auch eine Stunde voraus ist „nach": G = 15:00, jetzt in der Stunde 14:00. */
    @Test
    @DisplayName("G eine Stunde nach der aktuellen Stunde: nicht noetig")
    void g_eine_stunde_voraus() {
      assertThat(entscheide("2025-12-30T16:00:00", "2025-12-30T14:59:59").zustand())
          .isEqualTo(LiveRestZustand.NICHT_NOETIG);
    }

    /**
     * G <b>gleich</b> dem Anfang der aktuellen Stunde: Der Lauf lief in dieser Stunde, der Eimer
     * ist angebrochen — gerechnet wird, der Live-Bereich ist genau diese eine Stunde.
     */
    @Test
    @DisplayName("G gleich dem Anfang der aktuellen Stunde: angewandt, ein Eimer")
    void g_gleich_der_aktuellen_stunde() {
      LiveRestEntscheidung e = entscheide("2025-12-30T15:00:00", "2025-12-30T14:00:00");

      assertThat(e.zustand()).isEqualTo(LiveRestZustand.ANGEWANDT);
      assertThat(e.liveVon()).isEqualTo(t("2025-12-30T14:00:00"));
      assertThat(e.liveBis()).isEqualTo(t("2025-12-30T15:00:00"));
    }

    /** Ein uebersprungener Lauf: G = 14:00, jetzt 16:04 — gut zwei Stunden, drei Eimer. */
    @Test
    @DisplayName("Ein uebersprungener Lauf: angewandt ueber drei Eimer")
    void uebersprungener_lauf() {
      LiveRestEntscheidung e = entscheide("2025-12-30T15:00:00", "2025-12-30T16:04:00");

      assertThat(e.zustand()).isEqualTo(LiveRestZustand.ANGEWANDT);
      assertThat(e.liveVon()).isEqualTo(t("2025-12-30T14:00:00"));
      assertThat(e.liveBis()).isEqualTo(t("2025-12-30T17:00:00"));
    }

    /** Genau drei Stunden sind nicht „mehr als drei": angewandt, vier Eimer. */
    @Test
    @DisplayName("G genau drei Stunden vor jetzt: noch angewandt, vier Eimer")
    void genau_drei_stunden() {
      LiveRestEntscheidung e = entscheide("2025-12-30T15:00:00", "2025-12-30T17:00:00");

      assertThat(LiveRest.OBERGRENZE_STUNDEN).isEqualTo(3);
      assertThat(e.zustand()).isEqualTo(LiveRestZustand.ANGEWANDT);
      assertThat(e.liveVon()).isEqualTo(t("2025-12-30T14:00:00"));
      assertThat(e.liveBis()).isEqualTo(t("2025-12-30T18:00:00"));
    }

    @Test
    @DisplayName("Knapp ueber drei Stunden: ausgesetzt, vollstaendig bis G")
    void knapp_ueber_drei_stunden() {
      LiveRestEntscheidung e = entscheide("2025-12-30T15:00:00", "2025-12-30T17:00:01");

      assertThat(e.zustand()).isEqualTo(LiveRestZustand.AUSGESETZT);
      assertThat(e.vollstaendigBis()).isEqualTo(t("2025-12-30T14:00:00"));
      assertThat(e.liveVon()).isNull();
      assertThat(e.liveBis()).isNull();
    }

    /**
     * Die Umstellung auf Sommerzeit (2026-03-29, 02:00 → 03:00): Gerechnet wird auf den
     * Stundenlabels der Wanduhr, wie {@code message_rollup.stunde} sie fuehrt. Zwischen dem Label
     * 02:00 und 03:30 liegen anderthalb Label-Stunden, obwohl die Sonne nur eine halbe gesehen hat.
     */
    @Test
    @DisplayName("Sommerzeitbeginn: die Arithmetik laeuft auf den Labels der Wanduhr")
    void sommerzeitbeginn() {
      LiveRestEntscheidung e = entscheide("2026-03-29T03:00:00", "2026-03-29T03:30:00");

      assertThat(e.zustand()).isEqualTo(LiveRestZustand.ANGEWANDT);
      assertThat(e.liveVon()).isEqualTo(t("2026-03-29T02:00:00"));
      assertThat(e.liveBis()).isEqualTo(t("2026-03-29T04:00:00"));
    }

    /**
     * Das Ende der Sommerzeit (2026-10-25, 03:00 → 02:00): Das Label 02:00 kommt zweimal vor, der
     * Eimer 02:00 traegt beide. Ein Lauf mit W = 03:00 lief im Label 02:00; um 02:30 (welches auch
     * immer) ist der Eimer 02:00 angebrochen, und die Schranke rechnet in Labeln: 30 Minuten.
     */
    @Test
    @DisplayName("Sommerzeitende: das doppelte Label ist ein Eimer")
    void sommerzeitende() {
      LiveRestEntscheidung e = entscheide("2026-10-25T03:00:00", "2026-10-25T02:30:00");

      assertThat(e.zustand()).isEqualTo(LiveRestZustand.ANGEWANDT);
      assertThat(e.liveVon()).isEqualTo(t("2026-10-25T02:00:00"));
      assertThat(e.liveBis()).isEqualTo(t("2026-10-25T03:00:00"));
    }

    /**
     * W ist per Konstruktion ein Stundenanfang. Waere er es nicht, rundet die Entscheidung ab und
     * deckt einen Eimer <b>mehr</b> ab — nie einen weniger: minus und plus ueber einen ganzen Eimer
     * heben sich auf, wo nichts fehlt.
     */
    @Test
    @DisplayName("Ein Wasserstand abseits der vollen Stunde wird abgerundet, nie aufgerundet")
    void wasserstand_abseits_der_vollen_stunde() {
      LiveRestEntscheidung e = entscheide("2025-12-30T15:30:00", "2025-12-30T15:40:00");

      assertThat(e.zustand()).isEqualTo(LiveRestZustand.ANGEWANDT);
      assertThat(e.liveVon()).isEqualTo(t("2025-12-30T14:00:00"));
      assertThat(e.liveBis()).isEqualTo(t("2025-12-30T16:00:00"));
    }

    /**
     * jetzt genau auf der vollen Stunde: der Live-Bereich endet an der naechsten, wie
     * RollupFenster.
     */
    @Test
    @DisplayName("jetzt genau auf der vollen Stunde: der Bereich endet an der naechsten Stunde")
    void jetzt_auf_der_vollen_stunde() {
      LiveRestEntscheidung e = entscheide("2025-12-30T15:00:00", "2025-12-30T15:00:00");

      assertThat(e.liveVon()).isEqualTo(t("2025-12-30T14:00:00"));
      assertThat(e.liveBis()).isEqualTo(t("2025-12-30T16:00:00"));
    }

    @Test
    @DisplayName("Die Felder haengen am Zustand — ein Widerspruch faellt im Konstruktor")
    void widerspruch_faellt() {
      assertThatThrownBy(
              () ->
                  new LiveRestEntscheidung(
                      LiveRestZustand.ANGEWANDT, null, t("2025-12-30T15:00:00"), null))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(
              () ->
                  new LiveRestEntscheidung(
                      LiveRestZustand.NICHT_NOETIG, t("2025-12-30T14:00:00"), null, null))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(
              () ->
                  new LiveRestEntscheidung(
                      LiveRestZustand.AUSGESETZT,
                      null,
                      t("2025-12-30T14:00:00"),
                      t("2025-12-30T15:00:00")))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  @DisplayName("Die Verrechnung")
  class Verrechnung {

    private static final LocalDateTime G = t("2025-12-30T14:00:00");

    private static LiveRestZeile zeile(
        LocalDateTime stunde, String prozess, String status, long anzahl) {
      return new LiveRestZeile(stunde, prozess, status, anzahl);
    }

    private static Map<String, Long> nachrichtenJeProzess(LiveRestKorrektur korrektur) {
      Map<String, Long> summe = new java.util.HashMap<>();
      for (LiveRestZeile zeile : korrektur.zeilen()) {
        summe.merge(zeile.processId(), zeile.anzahl(), Long::sum);
      }
      return summe;
    }

    /**
     * Der Fall, fuer den es die Korrektur gibt: Der Lauf sah die Nachricht als {@code RUNNING},
     * seither ist sie {@code ERROR_TIMEOUT}. Minus alter, plus neuer Status — die Nachrichtenzahl
     * des Prozesses bleibt gleich, und wer die Rohwerte einordnet, zaehlt einen Fehler mehr.
     */
    @Test
    @DisplayName("Ein Statuswechsel innerhalb von G: minus alter, plus neuer Status")
    void statuswechsel() {
      LiveRestKorrektur k =
          LiveRestKorrektur.aus(
              List.of(zeile(G, "p1", "RUNNING", 1), zeile(G, "p1", "FINISHED", 4)),
              List.of(zeile(G, "p1", "ERROR_TIMEOUT", 1), zeile(G, "p1", "FINISHED", 4)));

      assertThat(k.zeilen())
          .containsExactlyInAnyOrder(
              zeile(G, "p1", "RUNNING", -1), zeile(G, "p1", "ERROR_TIMEOUT", 1));
      assertThat(nachrichtenJeProzess(k)).containsEntry("p1", 0L);
      assertThat(k.juengsteStundeJeProzess()).containsEntry("p1", G);
    }

    @Test
    @DisplayName("Was sich zu null aufhebt, faellt weg — die juengste Stunde bleibt trotzdem")
    void aufgehoben() {
      LiveRestKorrektur k =
          LiveRestKorrektur.aus(
              List.of(zeile(G, "p1", "FINISHED", 5)), List.of(zeile(G, "p1", "FINISHED", 5)));

      assertThat(k.zeilen()).isEmpty();
      assertThat(k.juengsteStundeJeProzess()).containsExactly(Map.entry("p1", G));
    }

    /** Neuer Verkehr seit dem Lauf: nur plus, und die Stunde ist die juengste Bewegung. */
    @Test
    @DisplayName("Nur Live-Verkehr, keine Rollupzeile: plus, mit juengster Stunde")
    void nur_live() {
      LocalDateTime naechste = G.plusHours(1);
      LiveRestKorrektur k =
          LiveRestKorrektur.aus(
              List.of(),
              List.of(zeile(G, "p2", "FINISHED", 3), zeile(naechste, "p2", "FINISHED", 2)));

      assertThat(nachrichtenJeProzess(k)).containsEntry("p2", 5L);
      assertThat(k.juengsteStundeJeProzess()).containsExactly(Map.entry("p2", naechste));
    }

    /** Die juengste Stunde kommt allein aus der Quelle — eine Rollupzeile hebt sie nicht. */
    @Test
    @DisplayName("Die juengste Stunde kommt aus der Quelle, nicht aus dem Rollup")
    void juengste_stunde_aus_der_quelle() {
      LiveRestKorrektur k =
          LiveRestKorrektur.aus(
              List.of(zeile(G.plusHours(2), "p1", "FINISHED", 1)),
              List.of(zeile(G, "p1", "FINISHED", 1)));

      assertThat(k.juengsteStundeJeProzess()).containsExactly(Map.entry("p1", G));
      assertThat(k.zeilen())
          .containsExactlyInAnyOrder(
              zeile(G.plusHours(2), "p1", "FINISHED", -1), zeile(G, "p1", "FINISHED", 1));
    }

    @Test
    @DisplayName("Ein Prozess ohne Live-Verkehr hat keine juengste Stunde")
    void ohne_live_verkehr_keine_stunde() {
      LiveRestKorrektur k =
          LiveRestKorrektur.aus(List.of(zeile(G, "p1", "FINISHED", 2)), List.of());

      assertThat(k.zeilen()).containsExactly(zeile(G, "p1", "FINISHED", -2));
      assertThat(k.juengsteStundeJeProzess()).isEmpty();
    }

    @Test
    @DisplayName("Leer bleibt leer")
    void leer() {
      assertThat(LiveRestKorrektur.aus(List.of(), List.of())).isEqualTo(LiveRestKorrektur.KEINE);
    }

    /** Zwei Schreibweisen bleiben zwei Zeilen — und heben sich je Prozess trotzdem auf. */
    @Test
    @DisplayName("Zwei Schreibweisen desselben Status: zwei Zeilen, dieselbe Summe je Prozess")
    void schreibweisen() {
      LiveRestKorrektur k =
          LiveRestKorrektur.aus(
              List.of(zeile(G, "p1", "FINISHED", 3)), List.of(zeile(G, "p1", "finished", 3)));

      assertThat(k.zeilen()).hasSize(2);
      assertThat(nachrichtenJeProzess(k)).containsEntry("p1", 0L);
    }
  }
}
