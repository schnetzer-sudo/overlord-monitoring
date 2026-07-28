package de.kraftwerkone.overlord.monitor.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

import de.kraftwerkone.overlord.monitor.common.ReadOnlyViolationException;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultDSLContext;
import org.jooq.impl.DefaultExecuteListenerProvider;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockDataProvider;
import org.jooq.tools.jdbc.MockResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Prueft die dritte Schicht des Schreibschutzes ohne Datenbank: ueber eine jOOQ-Mock-Verbindung.
 * Der {@link ReadOnlyExecuteListener} muss einen Schreibversuch abweisen, bevor die Verbindung
 * ueberhaupt angefasst wird, und Lesen zulassen.
 */
class ReadOnlyExecuteListenerTest {

  private DSLContext leseKontext;

  @BeforeEach
  void aufsetzen() {
    // Der Provider liefert nie Daten — bei Schreibzugriffen greift der Listener davor.
    MockDataProvider provider =
        ctx -> new MockResult[] {new MockResult(0, DSL.using(SQLDialect.MARIADB).newResult())};
    DefaultConfiguration cfg = new DefaultConfiguration();
    cfg.set(SQLDialect.MARIADB);
    cfg.set(new MockConnection(provider));
    cfg.set(new DefaultExecuteListenerProvider(new ReadOnlyExecuteListener()));
    leseKontext = new DefaultDSLContext(cfg);
  }

  @Test
  @DisplayName("Ein Schreibversuch auf dem Lese-Kontext wird abgewiesen")
  void schreibversuch_wird_abgewiesen() {
    assertThatThrownBy(
            () -> leseKontext.insertInto(table("Message")).columns(field("x")).values(1).execute())
        .matches(
            ReadOnlyExecuteListenerTest::enthaeltReadOnlyVerletzung,
            "enthaelt eine ReadOnlyViolationException in der Ursachenkette");
  }

  @Test
  @DisplayName("Ein Lesezugriff auf dem Lese-Kontext ist erlaubt")
  void lesezugriff_ist_erlaubt() {
    assertThatCode(() -> leseKontext.selectOne().fetch()).doesNotThrowAnyException();
  }

  /** jOOQ darf die Ausnahme umhuellen — deshalb die gesamte Ursachenkette pruefen. */
  private static boolean enthaeltReadOnlyVerletzung(Throwable t) {
    for (Throwable aktuell = t; aktuell != null; aktuell = aktuell.getCause()) {
      if (aktuell instanceof ReadOnlyViolationException) {
        return true;
      }
    }
    return false;
  }
}
