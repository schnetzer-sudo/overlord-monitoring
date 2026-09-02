package de.kraftwerkone.overlord.monitor.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.kraftwerkone.overlord.monitor.common.MessageStatusClassifier;
import de.kraftwerkone.overlord.monitor.common.Rollupzeitraum;
import de.kraftwerkone.overlord.monitor.common.Zeitfenster;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.time.LocalDateTime;
import java.util.OptionalLong;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockDataProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * <b>Was passiert, wenn die Live-Abfrage der Kachel <i>Ueberfaellig</i> stirbt</b> — geprueft an
 * der Ursache und nicht am Ergebnis.
 *
 * <p>Der Lese-Pool setzt {@code SET SESSION max_statement_time=10}. Genau diese eine Abfrage liest
 * auf der Produktion <b>live</b> ueber {@code Message}; der Rest des Dashboards kommt aus unserer
 * eigenen Tabelle. <b>Stirbt sie, darf nicht die ganze Seite sterben.</b>
 *
 * <p><b>Und die Gegenprobe gehoert dazu.</b> Ein pauschales {@code catch} machte aus jedem Bruch —
 * Syntaxfehler, abgerissene Verbindung, fehlendes Recht — ein „nicht ermittelbar" und damit aus
 * einem Befund eine Beruhigung. Der zweite Testfall haelt fest, dass genau das nicht passiert.
 *
 * <p>Hergestellt wird der Fall ueber eine jOOQ-Attrappe, die die Ausnahme wirft, die MariaDB und
 * der Treiber in diesem Fall liefern: Fehler {@code 1969}, SQLState {@code 70100}, als {@link
 * SQLTimeoutException}. <b>Deterministisch und ohne Wanduhr</b> (Regel T1).
 */
class DashboardZeitgrenzeTest {

  private static final MandantContext MANDANT = new MandantContext("ERFUNDEN");
  private static final LocalDateTime JETZT = LocalDateTime.parse("2025-12-30T04:09:47");
  private static final Zeitfenster FENSTER = Rollupzeitraum.STUNDEN_48.fenster(JETZT);

  private static DashboardRepository repositoryDas(MockDataProvider attrappe) {
    DSLContext kontext = DSL.using(new MockConnection(attrappe), SQLDialect.MARIADB);
    return new DashboardRepository(kontext, new MessageStatusClassifier());
  }

  /** Genau die Ausnahme, die MariaDB an der Zeitgrenze liefert. */
  private static MockDataProvider anDerZeitgrenze() {
    return ausfuehrung -> {
      throw new SQLTimeoutException("Query execution was interrupted", "70100", 1969);
    };
  }

  @Test
  @DisplayName("An der Zeitgrenze liefern beide Zahlen leer statt einer Ausnahme")
  void zeitgrenze_ergibt_leer() {
    DashboardRepository repository = repositoryDas(anDerZeitgrenze());

    assertThat(repository.ueberfaelligImFenster(MANDANT, FENSTER, JETZT))
        .isEqualTo(OptionalLong.empty());
    assertThat(repository.ueberfaelligInsgesamt(MANDANT, JETZT)).isEqualTo(OptionalLong.empty());
  }

  /**
   * <b>Die Gegenprobe.</b> Ein Syntaxfehler kommt als dieselbe {@link DataAccessException} an — und
   * bleibt, was er ist: ein technischer Fehler mit {@code 500}. Ohne diesen Fall bewiese der Test
   * oben nur, dass irgendein {@code catch} dasteht.
   */
  @Test
  @DisplayName("Jeder andere Datenbankfehler bleibt ein Fehler und wird nicht verschluckt")
  void anderer_fehler_faellt_durch() {
    DashboardRepository repository =
        repositoryDas(
            ausfuehrung -> {
              throw new SQLException("You have an error in your SQL syntax", "42000", 1064);
            });

    assertThatThrownBy(() -> repository.ueberfaelligInsgesamt(MANDANT, JETZT))
        .as("Ein pauschales catch machte aus einem Befund eine Beruhigung")
        .isInstanceOf(DataAccessException.class);
  }

  /**
   * Der Rest des Dashboards liest aus {@code message_rollup} und bekommt <b>keinen</b>
   * Teilerfolg-Mechanismus. Faellt dort etwas, faellt die Anfrage — und das ist richtig: Ein
   * Dashboard, das jeden Block einzeln scheitern lassen kann, zeigt irgendwann eine Seite voller
   * Luecken und nennt das eine Antwort.
   */
  @Test
  @DisplayName("Kein anderer Block faengt die Zeitgrenze ab — genau diese zwei Felder")
  void kein_allgemeiner_teilerfolg() {
    DashboardRepository repository = repositoryDas(anDerZeitgrenze());

    assertThatThrownBy(() -> repository.verlauf(MANDANT, Rollupzeitraum.STUNDEN_48, FENSTER))
        .isInstanceOf(DataAccessException.class);
    assertThatThrownBy(
            () ->
                repository.verteilung(
                    MANDANT, Rollupzeitraum.STUNDEN_48, FENSTER, Verteilungssicht.PARTNER))
        .isInstanceOf(DataAccessException.class);
    assertThatThrownBy(() -> repository.zuletztAufgefallen(MANDANT, FENSTER, JETZT, 10))
        .isInstanceOf(DataAccessException.class);
    assertThatThrownBy(() -> repository.belegung(MANDANT, Rollupzeitraum.STUNDEN_48, FENSTER))
        .isInstanceOf(DataAccessException.class);
    assertThatThrownBy(repository::letzterLauf).isInstanceOf(DataAccessException.class);
  }
}
