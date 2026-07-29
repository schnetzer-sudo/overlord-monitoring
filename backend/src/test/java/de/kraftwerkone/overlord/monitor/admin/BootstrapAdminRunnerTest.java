package de.kraftwerkone.overlord.monitor.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.kraftwerkone.overlord.monitor.audit.AuditEvent;
import de.kraftwerkone.overlord.monitor.audit.AuditEventType;
import de.kraftwerkone.overlord.monitor.audit.AuditLogWriter;
import de.kraftwerkone.overlord.monitor.security.AppUserRepository;
import de.kraftwerkone.overlord.monitor.security.Rolle;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Die Verzweigungen des Bootstraps — ohne Datenbank. */
class BootstrapAdminRunnerTest {

  private static final String EINMALPASSWORT = "einLangesPasswort1";

  private AppUserRepository appUserRepository;
  private PasswordEncoder passwortKodierer;
  private AuditLogWriter auditLogWriter;

  @BeforeEach
  void aufsetzen() {
    appUserRepository = mock(AppUserRepository.class);
    passwortKodierer = mock(PasswordEncoder.class);
    auditLogWriter = mock(AuditLogWriter.class);
    when(passwortKodierer.encode(anyString())).thenReturn("$2a$12$hash");
  }

  private BootstrapAdminRunner runner(String benutzer, String passwort) {
    return new BootstrapAdminRunner(
        appUserRepository,
        passwortKodierer,
        auditLogWriter,
        Clock.fixed(Instant.parse("2026-07-29T08:00:00Z"), ZoneOffset.UTC),
        benutzer,
        passwort);
  }

  @Test
  @DisplayName("Existiert bereits ein ADMIN, wird nichts angelegt und nichts geaendert")
  void vorhandener_admin_bleibt_unangetastet() {
    when(appUserRepository.existiertAdmin()).thenReturn(true);

    runner("admin", EINMALPASSWORT).run(null);

    verify(appUserRepository, never())
        .legeAn(anyString(), anyString(), any(), anyBoolean(), anyBoolean(), any());
    verify(auditLogWriter, never()).schreibe(any());
  }

  @Test
  @DisplayName("Ohne ADMIN entsteht ein aktives Konto mit Aenderungszwang")
  void ohne_admin_entsteht_ein_konto() {
    when(appUserRepository.existiertAdmin()).thenReturn(false);
    when(appUserRepository.legeAn(
            anyString(), anyString(), any(), anyBoolean(), anyBoolean(), any()))
        .thenReturn(42L);

    runner("admin", EINMALPASSWORT).run(null);

    verify(appUserRepository)
        .legeAn(eq("admin"), eq("$2a$12$hash"), eq(Rolle.ADMIN), eq(true), eq(true), any());
    ArgumentCaptor<AuditEvent> ereignis = ArgumentCaptor.forClass(AuditEvent.class);
    verify(auditLogWriter).schreibe(ereignis.capture());
    assertThat(ereignis.getValue().typ()).isEqualTo(AuditEventType.NUTZER_BOOTSTRAP);
    // Nirgends das Passwort — auch nicht im Protokolleintrag.
    assertThat(ereignis.getValue().detail()).doesNotContain(EINMALPASSWORT);
  }

  @Test
  @DisplayName("Fehlende Umgebungsvariablen brechen den Start ab, ohne Werte zu nennen")
  void fehlende_variablen_brechen_ab() {
    when(appUserRepository.existiertAdmin()).thenReturn(false);

    assertThatThrownBy(() -> runner("", EINMALPASSWORT).run(null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("OVERLORD_BOOTSTRAP_ADMIN_USER")
        .hasMessageNotContaining(EINMALPASSWORT);
  }

  @Test
  @DisplayName("Ein zu kurzes Einmalpasswort bricht den Start ab")
  void zu_kurzes_passwort_bricht_ab() {
    when(appUserRepository.existiertAdmin()).thenReturn(false);

    assertThatThrownBy(() -> runner("admin", "abc123").run(null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("OVERLORD_BOOTSTRAP_ADMIN_PASSWORD")
        .hasMessageNotContaining("abc123");
  }
}
