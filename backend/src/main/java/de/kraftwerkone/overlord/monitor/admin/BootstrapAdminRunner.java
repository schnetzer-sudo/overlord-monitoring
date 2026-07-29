package de.kraftwerkone.overlord.monitor.admin;

import de.kraftwerkone.overlord.monitor.audit.AuditEvent;
import de.kraftwerkone.overlord.monitor.audit.AuditEventType;
import de.kraftwerkone.overlord.monitor.audit.AuditLogWriter;
import de.kraftwerkone.overlord.monitor.security.AppUserRepository;
import de.kraftwerkone.overlord.monitor.security.Rolle;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Legt das erste Admin-Konto an — <b>nur im Profil {@code bootstrap}</b> und <b>nur, wenn noch kein
 * ADMIN existiert</b>.
 *
 * <p>Ein bestehendes Konto wird niemals ueberschrieben und kein Passwort zurueckgesetzt. Waere es
 * anders, waere ein versehentlich mitgegebenes Profil ein Weg, sich Zugang zu verschaffen.
 *
 * <p>Das Konto startet mit Aenderungszwang. Die Umgebungsvariable transportiert damit
 * ausschliesslich ein <b>Einmalpasswort</b>; nach der ersten Anmeldung ist ihr Inhalt wertlos.
 * <b>Das Passwort erscheint nirgends im Protokoll</b> — weder im Klartext noch als Hash, weder bei
 * Erfolg noch im Fehlerfall.
 */
@Component
@Profile("bootstrap")
public class BootstrapAdminRunner implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(BootstrapAdminRunner.class);

  private final AppUserRepository appUserRepository;
  private final PasswordEncoder passwortKodierer;
  private final AuditLogWriter auditLogWriter;
  private final Clock systemClock;
  private final String benutzername;
  private final String einmalpasswort;

  BootstrapAdminRunner(
      AppUserRepository appUserRepository,
      PasswordEncoder passwortKodierer,
      AuditLogWriter auditLogWriter,
      @Qualifier("systemClock") Clock systemClock,
      @Value("${OVERLORD_BOOTSTRAP_ADMIN_USER:}") String benutzername,
      @Value("${OVERLORD_BOOTSTRAP_ADMIN_PASSWORD:}") String einmalpasswort) {
    this.appUserRepository = appUserRepository;
    this.passwortKodierer = passwortKodierer;
    this.auditLogWriter = auditLogWriter;
    this.systemClock = systemClock;
    this.benutzername = benutzername;
    this.einmalpasswort = einmalpasswort;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (appUserRepository.existiertAdmin()) {
      log.info(
          "Bootstrap uebersprungen: es existiert bereits ein Konto mit Rolle ADMIN."
              + " Es wird nichts angelegt und nichts geaendert.");
      return;
    }
    if (benutzername.isBlank() || einmalpasswort.isBlank()) {
      // Namen der Variablen, niemals Werte.
      throw new IllegalStateException(
          "Profil bootstrap aktiv, aber OVERLORD_BOOTSTRAP_ADMIN_USER oder"
              + " OVERLORD_BOOTSTRAP_ADMIN_PASSWORD ist nicht gesetzt.");
    }
    if (einmalpasswort.length() < AdminUserService.MINDESTLAENGE) {
      throw new IllegalStateException(
          "OVERLORD_BOOTSTRAP_ADMIN_PASSWORD ist kuerzer als "
              + AdminUserService.MINDESTLAENGE
              + " Zeichen.");
    }
    // Dieselbe Grenze wie beim Anlegen ueber den Endpunkt: ein zu langer Name ergaebe ein Konto,
    // das sich nie anmelden kann.
    AdminUserService.pruefeBenutzername(benutzername);

    LocalDateTime jetztUtc = LocalDateTime.ofInstant(systemClock.instant(), ZoneOffset.UTC);
    long id =
        appUserRepository.legeAn(
            benutzername,
            passwortKodierer.encode(einmalpasswort),
            Rolle.ADMIN,
            true,
            true,
            jetztUtc);
    auditLogWriter.schreibe(
        new AuditEvent(
            AuditEventType.NUTZER_BOOTSTRAP,
            null,
            benutzername,
            null,
            "app_user",
            String.valueOf(id),
            null,
            "Erstes Admin-Konto im Profil bootstrap angelegt, mit Passwortwechselzwang"));
    log.warn(
        "Bootstrap: Admin-Konto '{}' angelegt (ID {}). Es muss das Passwort bei der ersten"
            + " Anmeldung wechseln. Starte die Anwendung danach ohne das Profil bootstrap.",
        benutzername,
        id);
  }
}
