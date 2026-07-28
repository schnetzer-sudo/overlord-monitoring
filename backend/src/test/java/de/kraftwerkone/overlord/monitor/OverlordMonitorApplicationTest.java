package de.kraftwerkone.overlord.monitor;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * Rauchtest: Der Anwendungskontext startet.
 *
 * <p>Ab Schritt 2 haengt der Start an den beiden Datenbankverbindungen (DataSources, Flyway,
 * Dev-Clock lesen beim Hochfahren die Testkopie). Deshalb {@code @Tag("db")}: in der CI ohne
 * Datenbankzugriff wird der Test ueber {@code -DexcludedGroups=db} ausgeschlossen.
 */
@SpringBootTest
@ActiveProfiles("dev")
@Tag("db")
class OverlordMonitorApplicationTest {

  @Autowired private ApplicationContext kontext;

  @Test
  @DisplayName("Der Anwendungskontext startet")
  void kontext_startet() {
    assertThat(kontext).isNotNull();
    assertThat(kontext.getBean(OverlordMonitorApplication.class)).isNotNull();
  }
}
