package de.kraftwerkone.overlord.monitor.admin;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.audit.AuditLogWriter;
import de.kraftwerkone.overlord.monitor.security.Rolle;
import de.kraftwerkone.overlord.monitor.security.SicherheitsTestbasis;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Der Bootstrap gegen die echte Tabelle: <b>Existiert bereits ein ADMIN, passiert nichts.</b>
 *
 * <p>Der umgekehrte Fall — das erste Konto entsteht — laesst sich hier nicht sauber pruefen, weil
 * die Bedingung „es gibt noch keinen ADMIN" gegen eine gemeinsam genutzte Datenbank nicht
 * herstellbar ist, ohne fremde Konten anzufassen. Diese Verzweigung deckt {@link
 * BootstrapAdminRunnerTest} ohne Datenbank ab.
 */
class BootstrapAdminDbIT extends SicherheitsTestbasis {

  private static final String VORHANDENER_ADMIN = PRAEFIX + "bootstrap-admin";
  private static final String NEUER_NAME = PRAEFIX + "bootstrap-zweiter";

  @Autowired private AuditLogWriter auditLogWriter;

  @Test
  @DisplayName("Bei vorhandenem ADMIN legt der Bootstrap nichts an")
  void bei_vorhandenem_admin_passiert_nichts() {
    legeNutzerAn(VORHANDENER_ADMIN, "einLangesPasswort1", Rolle.ADMIN);
    assertThat(appUserRepository.existiertAdmin()).isTrue();

    new BootstrapAdminRunner(
            appUserRepository,
            passwortKodierer,
            auditLogWriter,
            systemClock,
            NEUER_NAME,
            "einLangesPasswort2")
        .run(null);

    assertThat(appUserRepository.findeNachBenutzername(NEUER_NAME))
        .as("Ein zweiter Lauf darf niemals ein Konto anlegen oder ueberschreiben")
        .isEmpty();
  }
}
