package de.kraftwerkone.overlord.monitor.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;
import org.jooq.SQLDialect;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockDataProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * <b>Fehler live, der Baustein</b> — ohne Datenbank ({@code docs/fehler-live.md} §4, §9).
 *
 * <p>Der Ersatz ist eine reine Funktion; geprueft wird er an erfundenen Zeilen einer eigenen,
 * kleinen Gestalt ({@link Zeile}: Stundeneimer, Rohwert, Anzahl) — derselben, die die Uebersicht
 * aus Rollup und Live-Rest bekommt. <b>Kein Wert aus dem Bestand</b> (Regel T2), keine Uhr (T1).
 */
class FehlerLiveTest {

  private static final MessageStatusClassifier KLASSIFIZIERER = new MessageStatusClassifier();
  private static final MandantContext MANDANT = new MandantContext("ERFUNDEN");
  private static final LocalDateTime VON = LocalDateTime.parse("2025-12-28T15:00:00");
  private static final LocalDateTime BIS = LocalDateTime.parse("2025-12-30T15:00:00");

  /** Eine Grundzeile, wie die Uebersicht sie hat: ein Eimer, ein Rohwert, eine Anzahl. */
  private record Zeile(LocalDateTime eimer, String rohwert, long anzahl) {}

  private static Zeile zeile(String eimer, String rohwert, long anzahl) {
    return new Zeile(LocalDateTime.parse(eimer), rohwert, anzahl);
  }

  private static List<Zeile> ersetzt(List<Zeile> grund, List<Zeile> live) {
    return FehlerLiveErsatz.ersetze(
        FehlerLiveZustand.ANGEWANDT, grund, live, Zeile::rohwert, KLASSIFIZIERER);
  }

  /** Die Kachel <i>Fehler</i>, wie die Uebersicht sie aus den Zeilen bildet. */
  private static long fehler(List<Zeile> zeilen) {
    return zeilen.stream()
        .filter(z -> KLASSIFIZIERER.einordnung(z.rohwert()) == MessageStatusKind.FEHLER)
        .mapToLong(Zeile::anzahl)
        .sum();
  }

  /** Die Kachel <i>Nachrichten</i>. */
  private static long nachrichten(List<Zeile> zeilen) {
    return zeilen.stream().mapToLong(Zeile::anzahl).sum();
  }

  // ─── Der Ersatz ──────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("Der Ersatz — die Fehler aus der Lesung, alles andere bleibt")
  class Ersatz {

    /**
     * <b>Der Fall aus der Produktion (18.09.2026).</b> Der Rollup haelt einen Fehler im Eimer
     * 09:00; die Nachricht ist nachverarbeitet, steht auf {@code RUNNING} und ist in die Stunde
     * 14:00 gewandert. Die Lesung findet um 09:00 keinen Fehler mehr.
     */
    @Test
    @DisplayName("Der Fall aus der Produktion: Fehler 0, Nachrichten 1, kein Fehleranteil um 09:00")
    void der_fall_aus_der_produktion() {
      List<Zeile> grund =
          List.of(
              zeile("2025-12-30T09:00", "ERROR_TIMEOUT", 1),
              zeile("2025-12-30T14:00", "RUNNING", 1));

      List<Zeile> ergebnis = ersetzt(grund, List.of());

      assertThat(fehler(ergebnis)).isZero();
      assertThat(nachrichten(ergebnis)).isEqualTo(1);
      assertThat(ergebnis)
          .as("Im Eimer 09:00 steht nichts mehr, in 14:00 die laufende Nachricht")
          .containsExactly(zeile("2025-12-30T14:00", "RUNNING", 1));
    }

    /**
     * Nach der Nachverarbeitung faellt die Nachricht erneut in einen Fehler — jetzt in der Stunde
     * 14:00. Der Rollup haelt noch den alten, der Live-Rest den neuen: Sie zaehlt <b>einmal</b>, im
     * Eimer, in dem sie jetzt steht.
     */
    @Test
    @DisplayName("Ein erneuter Fehler nach der Nachverarbeitung zaehlt einmal")
    void erneuter_fehler_zaehlt_einmal() {
      List<Zeile> grund =
          List.of(
              zeile("2025-12-30T09:00", "ERROR_TIMEOUT", 1),
              zeile("2025-12-30T14:00", "ERROR_TIMEOUT", 1));
      List<Zeile> live = List.of(zeile("2025-12-30T14:00", "ERROR_TIMEOUT", 1));

      List<Zeile> ergebnis = ersetzt(grund, live);

      assertThat(fehler(ergebnis)).isEqualTo(1);
      assertThat(nachrichten(ergebnis)).isEqualTo(1);
      assertThat(ergebnis).containsExactly(zeile("2025-12-30T14:00", "ERROR_TIMEOUT", 1));
    }

    @Test
    @DisplayName("Ein Fehler im Live-Bereich zaehlt einmal, obwohl Live-Rest und Lesung ihn kennen")
    void fehler_im_live_bereich_zaehlt_einmal() {
      List<Zeile> grund =
          List.of(
              zeile("2025-12-30T14:00", "FINISHED", 5),
              zeile("2025-12-30T14:00", "ERROR_DUPLICATE", 1));
      List<Zeile> live = List.of(zeile("2025-12-30T14:00", "ERROR_DUPLICATE", 1));

      List<Zeile> ergebnis = ersetzt(grund, live);

      assertThat(fehler(ergebnis)).isEqualTo(1);
      assertThat(nachrichten(ergebnis)).isEqualTo(6);
    }

    /**
     * Die Einordnung beim Lesen, nicht nachgebaut: Ein unbekanntes {@code ERROR_} und {@code
     * COMMIT_REJECTED} sind Fehler — sie fallen aus den Grundzeilen heraus, auch wenn die Lesung
     * sie nicht mehr kennt, und zaehlen als Fehler, wenn sie aus der Lesung kommen.
     */
    @Test
    @DisplayName("Ein unbekanntes ERROR_ und COMMIT_REJECTED werden als FEHLER eingeordnet")
    void unbekanntes_error_und_commit_rejected() {
      List<Zeile> grund =
          List.of(
              zeile("2025-12-30T10:00", "ERROR_NEUARTIG", 2),
              zeile("2025-12-30T10:00", "COMMIT_REJECTED", 3),
              zeile("2025-12-30T10:00", "error_klein", 4),
              zeile("2025-12-30T10:00", "COMMIT_RECEIVED", 7));
      List<Zeile> live =
          List.of(
              zeile("2025-12-30T11:00", "ERROR_NEUARTIG", 1),
              zeile("2025-12-30T11:00", "COMMIT_REJECTED", 1));

      List<Zeile> ergebnis = ersetzt(grund, live);

      assertThat(ergebnis)
          .extracting(Zeile::eimer, Zeile::rohwert, Zeile::anzahl)
          .as("Die drei Fehler um 10:00 sind ersetzt, die Quittung bleibt")
          .containsExactly(
              tuple(LocalDateTime.parse("2025-12-30T10:00"), "COMMIT_RECEIVED", 7L),
              tuple(LocalDateTime.parse("2025-12-30T11:00"), "ERROR_NEUARTIG", 1L),
              tuple(LocalDateTime.parse("2025-12-30T11:00"), "COMMIT_REJECTED", 1L));
      assertThat(fehler(ergebnis)).isEqualTo(2);
    }

    @Test
    @DisplayName("Leere Live-Zeilen entfernen alle Fehler — und nur die Fehler")
    void leere_live_zeilen_entfernen_alle_fehler() {
      List<Zeile> grund =
          List.of(
              zeile("2025-12-30T09:00", "ERROR_TIMEOUT", 1),
              zeile("2025-12-30T10:00", "ERROR_DUPLICATE", 2),
              zeile("2025-12-30T10:00", "SUSPENDED", 3),
              zeile("2025-12-30T10:00", "CKECKED", 1));

      List<Zeile> ergebnis = ersetzt(grund, List.of());

      assertThat(fehler(ergebnis)).isZero();
      assertThat(ergebnis)
          .as("Wartend und Ungeklaert bleiben, wie sie sind — ein Abgang aus ihnen ist nicht Teil")
          .containsExactly(
              zeile("2025-12-30T10:00", "SUSPENDED", 3), zeile("2025-12-30T10:00", "CKECKED", 1));
    }

    @Test
    @DisplayName("AUSGESETZT laesst die Grundzeilen unveraendert — samt ihrer Fehler")
    void ausgesetzt_laesst_die_grundzeilen() {
      List<Zeile> grund =
          List.of(
              zeile("2025-12-30T09:00", "ERROR_TIMEOUT", 1),
              zeile("2025-12-30T14:00", "RUNNING", 1));

      List<Zeile> ergebnis =
          FehlerLiveErsatz.ersetze(
              FehlerLiveZustand.AUSGESETZT, grund, List.of(), Zeile::rohwert, KLASSIFIZIERER);

      assertThat(ergebnis).isEqualTo(grund);
      assertThat(fehler(ergebnis)).isEqualTo(1);
    }

    @Test
    @DisplayName("AUSGESETZT mit Live-Zeilen ist ein Widerspruch und faellt laut")
    void ausgesetzt_mit_zeilen_faellt() {
      assertThatThrownBy(
              () ->
                  FehlerLiveErsatz.ersetze(
                      FehlerLiveZustand.AUSGESETZT,
                      List.<Zeile>of(),
                      List.of(zeile("2025-12-30T09:00", "ERROR_TIMEOUT", 1)),
                      Zeile::rohwert,
                      KLASSIFIZIERER))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  // ─── Ergebnis, Zeile, Block ─────────────────────────────────────────────────

  @Nested
  @DisplayName("Ergebnis, Zeile und Antwortblock")
  class Werte {

    @Test
    @DisplayName("Nur ANGEWANDT traegt Zeilen; leer und angewandt heisst: kein Fehler")
    void ergebnis() {
      FehlerLiveZeile zeile =
          new FehlerLiveZeile(LocalDateTime.parse("2025-12-30T09:00"), "P-1", "ERROR_TIMEOUT", 1);

      assertThat(FehlerLiveErgebnis.angewandt(List.of(zeile)).angewandt()).isTrue();
      assertThat(FehlerLiveErgebnis.angewandt(List.of()).zeilen()).isEmpty();
      assertThat(FehlerLiveErgebnis.ausgesetzt().angewandt()).isFalse();
      assertThatThrownBy(() -> new FehlerLiveErgebnis(FehlerLiveZustand.AUSGESETZT, List.of(zeile)))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Eine Zeile braucht Stunde und Prozess und zaehlt mindestens eine Nachricht")
    void zeile() {
      LocalDateTime stunde = LocalDateTime.parse("2025-12-30T09:00");
      assertThatThrownBy(() -> new FehlerLiveZeile(null, "P-1", "ERROR_TIMEOUT", 1))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> new FehlerLiveZeile(stunde, null, "ERROR_TIMEOUT", 1))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> new FehlerLiveZeile(stunde, "P-1", "ERROR_TIMEOUT", 0))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Der Block fehlerLive traegt den Zustand und nichts sonst")
    void block() {
      assertThat(FehlerLiveResponse.aus(FehlerLiveErgebnis.angewandt(List.of())))
          .isEqualTo(new FehlerLiveResponse(FehlerLiveZustand.ANGEWANDT));
      assertThat(FehlerLiveResponse.aus(FehlerLiveErgebnis.ausgesetzt()))
          .isEqualTo(new FehlerLiveResponse(FehlerLiveZustand.AUSGESETZT));
      assertThat(FehlerLiveResponse.class.getRecordComponents()).hasSize(1);
    }
  }

  // ─── Der Ausfall ─────────────────────────────────────────────────────────────

  /**
   * <b>Der Ausfall, wie beim Live-Rest (E-185 sinngemaess):</b> Jede {@link DataAccessException}
   * ergibt {@code AUSGESETZT} und ein {@code WARN} — die Zeitgrenze des Lese-Pools ebenso wie ein
   * Syntaxfehler, denn {@code LiveRestService} unterscheidet dort nicht, und dieser Baustein faengt
   * nicht weiter und nicht enger. <b>Was keine {@code DataAccessException} ist, laeuft durch.</b>
   */
  @Nested
  @DisplayName("Der Ausfall — ausgesetzt mit WARN, nicht weiter und nicht enger als der Live-Rest")
  class Ausfall {

    private FehlerLiveService dienstAn(MockDataProvider attrappe) {
      return new FehlerLiveService(
          new FehlerLiveRepository(
              DSL.using(new MockConnection(attrappe), SQLDialect.MARIADB), KLASSIFIZIERER));
    }

    /** Die Protokollzeilen des Dienstes waehrend eines Aufrufs — deterministisch, ohne Uhr. */
    private <T> T mitMitschrift(List<ILoggingEvent> ziel, Supplier<T> aufgabe) {
      Logger protokoll = (Logger) LoggerFactory.getLogger(FehlerLiveService.class);
      ListAppender<ILoggingEvent> anhang = new ListAppender<>();
      anhang.start();
      protokoll.addAppender(anhang);
      try {
        return aufgabe.get();
      } finally {
        protokoll.detachAppender(anhang);
        anhang.stop();
        ziel.addAll(anhang.list);
      }
    }

    @Test
    @DisplayName("An der Zeitgrenze des Lese-Pools: ausgesetzt, und ein WARN steht im Protokoll")
    void zeitgrenze() {
      List<ILoggingEvent> mitschrift = new java.util.ArrayList<>();
      FehlerLiveService dienst =
          dienstAn(
              ausfuehrung -> {
                throw new SQLTimeoutException("Query execution was interrupted", "70100", 1969);
              });

      FehlerLiveErgebnis ergebnis =
          mitMitschrift(mitschrift, () -> dienst.ermittle(MANDANT, VON, BIS));

      assertThat(ergebnis.zustand()).isEqualTo(FehlerLiveZustand.AUSGESETZT);
      assertThat(ergebnis.zeilen()).isEmpty();
      assertThat(mitschrift)
          .extracting(ILoggingEvent::getLevel)
          .as("Rueckfall statt Ausfall — aber sichtbar")
          .containsExactly(Level.WARN);
    }

    @Test
    @DisplayName("Auch ein Syntaxfehler setzt aus — der Live-Rest unterscheidet dort nicht")
    void jeder_datenbankfehler() {
      FehlerLiveService dienst =
          dienstAn(
              ausfuehrung -> {
                throw new SQLException("You have an error in your SQL syntax", "42000", 1064);
              });

      assertThat(dienst.ermittle(MANDANT, VON, BIS).zustand())
          .isEqualTo(FehlerLiveZustand.AUSGESETZT);
    }

    @Test
    @DisplayName("Was keine DataAccessException ist, laeuft durch — nicht weiter als der Live-Rest")
    void anderes_laeuft_durch() {
      FehlerLiveRepository repository = mock(FehlerLiveRepository.class);
      when(repository.ausDerQuelle(any(), any(), any()))
          .thenThrow(new IllegalStateException("Zeilenabbildung"));

      assertThatThrownBy(() -> new FehlerLiveService(repository).ermittle(MANDANT, VON, BIS))
          .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Ohne Ausfall: angewandt, mit den Zeilen der Lesung")
    void angewandt() {
      FehlerLiveRepository repository = mock(FehlerLiveRepository.class);
      FehlerLiveZeile zeile =
          new FehlerLiveZeile(LocalDateTime.parse("2025-12-30T09:00"), "P-1", "ERROR_TIMEOUT", 2);
      when(repository.ausDerQuelle(MANDANT, VON, BIS)).thenReturn(List.of(zeile));

      FehlerLiveErgebnis ergebnis = new FehlerLiveService(repository).ermittle(MANDANT, VON, BIS);

      assertThat(ergebnis.zustand()).isEqualTo(FehlerLiveZustand.ANGEWANDT);
      assertThat(ergebnis.zeilen()).containsExactly(zeile);
    }
  }
}
