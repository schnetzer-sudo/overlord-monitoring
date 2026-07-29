package de.kraftwerkone.overlord.monitor.security;

import de.kraftwerkone.overlord.monitor.audit.AuditEvent;
import de.kraftwerkone.overlord.monitor.audit.AuditEventType;
import de.kraftwerkone.overlord.monitor.audit.AuditLogWriter;
import de.kraftwerkone.overlord.monitor.common.error.FachlicheAusnahme;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Passwortaenderung durch den Nutzer selbst.
 *
 * <p>Ohne diesen Pfad waere <b>kein einziges Konto nutzbar</b>: Jedes Konto — auch das
 * Bootstrap-Konto — startet mit Aenderungszwang, und ein Zuruecksetzen per E-Mail gibt es nicht
 * (Annahme A3, kein SMTP-Relay).
 *
 * <p><b>Mindestlaenge zwoelf Zeichen, sonst keine Zusammensetzungsregeln.</b> Erzwungene
 * Sonderzeichen erhoehen die Entropie kaum und erzeugen vorhersagbare Muster; Laenge wirkt.
 */
@Service
public class PasswortService {

  static final int MINDESTLAENGE = 12;

  private final AppUserRepository appUserRepository;
  private final PasswordEncoder passwortKodierer;
  private final AuditLogWriter auditLogWriter;
  private final Clock systemClock;

  PasswortService(
      AppUserRepository appUserRepository,
      PasswordEncoder passwortKodierer,
      AuditLogWriter auditLogWriter,
      @Qualifier("systemClock") Clock systemClock) {
    this.appUserRepository = appUserRepository;
    this.passwortKodierer = passwortKodierer;
    this.auditLogWriter = auditLogWriter;
    this.systemClock = systemClock;
  }

  /**
   * Setzt ein neues Passwort und hebt den Aenderungszwang auf.
   *
   * @return derselbe Nutzer ohne Aenderungszwang — das Principal der Sitzung wird damit ersetzt
   */
  public AngemeldeterNutzer aendere(
      AngemeldeterNutzer nutzer, String altesPasswort, String neuesPasswort, String ip) {
    AppUserZeile zeile =
        appUserRepository
            .findeNachId(nutzer.id())
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Angemeldeter Nutzer ohne Zeile in app_user: " + nutzer.id()));

    String hash = zeile.passwortHash();
    if (hash == null || !passwortKodierer.matches(altesPasswort, hash)) {
      // Der Nutzer ist bereits angemeldet — hier laesst sich nichts ueber fremde Konten
      // herausfinden, deshalb darf der Grund benannt werden.
      throw new FachlicheAusnahme(
          HttpStatus.BAD_REQUEST,
          "altes-passwort-falsch",
          "Altes Passwort falsch",
          "Das bisherige Passwort stimmt nicht.",
          "Passwortaenderung mit falschem alten Passwort, Nutzer " + nutzer.id());
    }
    if (neuesPasswort == null || neuesPasswort.length() < MINDESTLAENGE) {
      throw new FachlicheAusnahme(
          HttpStatus.BAD_REQUEST,
          "passwort-zu-kurz",
          "Passwort zu kurz",
          "Das neue Passwort braucht mindestens " + MINDESTLAENGE + " Zeichen.",
          "Neues Passwort unter der Mindestlaenge, Nutzer " + nutzer.id());
    }
    if (neuesPasswort.equals(altesPasswort)) {
      throw new FachlicheAusnahme(
          HttpStatus.BAD_REQUEST,
          "passwort-unveraendert",
          "Passwort unveraendert",
          "Das neue Passwort muss sich vom bisherigen unterscheiden.",
          "Neues Passwort gleich dem alten, Nutzer " + nutzer.id());
    }

    LocalDateTime jetztUtc = LocalDateTime.ofInstant(systemClock.instant(), ZoneOffset.UTC);
    appUserRepository.setzePasswort(nutzer.id(), passwortKodierer.encode(neuesPasswort), jetztUtc);
    auditLogWriter.schreibe(
        new AuditEvent(
            AuditEventType.PASSWORT_GEAENDERT,
            nutzer.id(),
            nutzer.username(),
            null,
            "app_user",
            String.valueOf(nutzer.id()),
            ip,
            null));
    return nutzer.ohneAenderungszwang();
  }
}
