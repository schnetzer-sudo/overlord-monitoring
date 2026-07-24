package de.kraftwerkone.overlord.monitor;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * Rauchtest: Der Anwendungskontext startet.
 *
 * <p>Es gibt in Schritt 1 noch keine Datenbankverbindung. Dieser Test stellt lediglich sicher, dass
 * die Konfiguration in sich stimmig ist und die Anwendung ueberhaupt hochkommt.
 */
@SpringBootTest
@ActiveProfiles("dev")
class OverlordMonitorApplicationTest {

  @Autowired private ApplicationContext kontext;

  @Test
  @DisplayName("Der Anwendungskontext startet")
  void kontext_startet() {
    assertThat(kontext).isNotNull();
    assertThat(kontext.getBean(OverlordMonitorApplication.class)).isNotNull();
  }
}
